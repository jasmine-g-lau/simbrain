package org.simbrain.network.core

import kotlinx.coroutines.*
import org.simbrain.network.events.NetworkEvents
import org.simbrain.network.gui.PlacementManager
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.util.SimpleIdManager
import org.simbrain.util.UserParameter
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.updater.PerformanceMonitor
import org.simbrain.workspace.updater.UpdateAction
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.random.Random

/**
 * Constant value for Math.log(10); used to approximate log 10.
 */
private val LOG_10 = ln(10.0)

/**
 * <b>Network</b> provides core neural network functionality and is the main neural network model object. The core
 * data structure is a [NetworkModelList] that associates classes of [NetworkModel] with linked hash sets of
 * instances of those types.
 *
 * To add models, use [Network.addNetworkModel] and friends.
 *
 * To remove models use [Network.getModels] and call .delete() on the resulting models. Get models can be called with
 * an argument to filter by model type, e.g. getModels(Neuron.class)
 */
class Network: CoroutineScope, EditableObject {

    @Transient
    private var job = SupervisorJob()

    @Transient
    override var coroutineContext = Dispatchers.Default + job

    /**
     * Two types of time used in simulations.
     */
    enum class TimeType {
        /**
         * Network update iterations are time-steps.
         */
        DISCRETE,

        /**
         * Simulation of real time. Each updates advances time by length.
         */
        CONTINUOUS
    }

    /**
     * Handle network events.
     */
    @Transient
    var events = NetworkEvents()
        private set

    /**
     * Main data structure containing all [NetworkModel]s: neurons, synapses, etc.
     */
    private val networkModels = NetworkModelList()

    /**
     * The update manager for this network.
     */
    val updateManager = NetworkUpdateManager(this)

    /**
     * In iterations or msec.
     */
    var time = 0.0
        private set

    /**
     * Time step.
     */
    @UserParameter("Time Step", increment = .1, minimumValue = 0.0, order = 10)
    var timeStep = NetworkPreferences.defaultTimeStep

    /**
     * Whether this is a discrete or continuous time network.
     */
    @UserParameter("Time Type", description = "Whether to display iterations or time (display property only; " +
            "does not impact logical update)", order = 20)
    var timeType = TimeType.DISCRETE

    /**
     * Whether network has been updated yet; used by thread.
     */
    @Transient
    private var updateCompleted = AtomicBoolean(false)

    /**
     * List of neurons sorted by their update priority. Used in priority based update.
     * Lower numbers updated first, as in first priority, second priority, etc.
     */
    @Transient
    var prioritySortedNeuronList: MutableList<Neuron> = ArrayList()
        private set

    /**
     * Manage ids for all network elements.
     */
    @Transient
    var idManager = SimpleIdManager({ cls -> networkModels.getRawModelSet(cls).size + 1 })
        private set

    /**
     * Manages placement of new nodes, groups, etc.
     */
    @Transient
    var placementManager = PlacementManager()

    /**
     * Returns a linked hash set of models of the specified type.
     */
    fun <T : NetworkModel> getModels(cls: Class<T>) = networkModels[cls]

    /**
     * Returns a linked hash set of models of a type specified using a generic.
     */
    inline fun <reified T : NetworkModel> getModels() = getModels(T::class.java)

    /**
     * Returns a flattened list of all network models.
     */
    val allModels get() = networkModels.all

    /**
     * Returns a list of network models in the order needed to reconstruct a network properly. Example: nodes must be
     * added before synapses which refer to them.
     */
    val modelsInReconstructionOrder get() = networkModels.allInUpdatingOrder

    var randomSeed = Random.nextLong()
        set(value) {
            field = value
            random = Random(value)
        }

    @Transient
    var random = Random(randomSeed)
        private set

    private var shouldUpdateTimeType = true

    private fun updateInternal(name: String) {
        // Main update
        updateManager.actionList.forEach {
            runBlocking {
                PerformanceMonitor.record(it, "${name}:${it.description}") {
                    it.run()
                }
            }
        }

        if (shouldUpdateTimeType) {
            updateTimeType()
            shouldUpdateTimeType = false
        }

        updateTime()
        setUpdateCompleted(true)
    }

    /**
     * The core update function of the neural network. Calls the current update function on each neuron, decays all the
     * neurons, and checks their bounds.
     */
    @JvmOverloads
    fun update(name: String = "") {
        updateInternal(name)
        events.updated.fireAndBlock()
    }

    suspend fun updateSuspend(name: String = "") {
        updateInternal(name)
        events.updated.fire().await()
    }

    /**
     * Update the priority list used for priority based update.
     */
    private fun updatePriorityList() {
        prioritySortedNeuronList = flatNeuronList.sortedBy { it.updatePriority }.toMutableList()
    }

    /**
     * This function is used to update the neuron and sub-network activation values if the user chooses to set different
     * priority values for a subset of neurons and sub-networks. The priority value determines the order in which the
     * neurons and sub-networks get updated - smaller priority value elements will be updated before larger priority
     * value elements.
     */
    fun updateNeuronsByPriority() {
        for (neuron in prioritySortedNeuronList) {
            neuron.accumulateInputs()
            neuron.update()
        }
    }

    /**
     * Update all network models except for neurons.
     * Obviously a temporary method!
     */
    fun updateAllButNeurons() {
        // TODO: Temporary function until we create a generalized priority based update
        listOf(
            NeuronGroup::class.java,
            NeuronCollection::class.java,
            NeuronArray::class.java,
            Connector::class.java,
            SynapseGroup::class.java,
            Subnetwork::class.java,
            Synapse::class.java
        )
            .flatMap { networkModels[it] }
            .forEach {nm ->
                nm.accumulateInputs()
                nm.update()
            }
    }

    /**
     * Default asynchronous update method called by [org.simbrain.network.update_actions.BufferedUpdate].
     */
    suspend fun bufferedUpdate()  = coroutineScope {
        networkModels.all.forEach { it.accumulateInputs() }
        networkModels.all.forEach { it.update() }
    }

    /**
     * Set the activation level of all neurons to zero.
     */
    fun clearActivations() {
        flatNeuronList.forEach(Neuron::clear)
    }

    val freeNeurons get() = networkModels.get<Neuron>()

    val freeSynapses get() = networkModels.get<Synapse>()

    /**
     * Create "flat" list of neurons, which includes the top-level neurons plus all group neurons.
     *
     * @return the flat list
     */
    val flatNeuronList: List<Neuron>
        get() = sequence {
            yieldAll(networkModels.get<Neuron>())
            for (neuronGroup in networkModels.get<NeuronGroup>()) {
                yieldAll(neuronGroup.neuronList)
            }
            for (subnetwork in networkModels.get<Subnetwork>()) {
                yieldAll(subnetwork.modelList.get<NeuronGroup>().flatMap { it.neuronList })
            }
        }.toList()

    /**
     * Create "flat" list of synapses, which includes the top-level synapses plus all subnet synapses.
     *
     * @return the flat list
     */
    val flatSynapseList: List<Synapse>
        get() = sequence {
            yieldAll(networkModels.get<Synapse>())
            yieldAll(networkModels.get<SynapseGroup>().flatMap { sg -> sg.synapses })
            yieldAll(networkModels.get<Subnetwork>().flatMap { subnetwork ->
                subnetwork.modelList.get<SynapseGroup>().flatMap { it.synapses }
            })
        }.toList()

    /**
     * Returns a list of all neuron groups including those in subnetworks.
     */
    val flatNeuronGroupList: List<NeuronGroup>
        get() = sequence {
            yieldAll(networkModels.get<NeuronGroup>())
            yieldAll(networkModels.get<Subnetwork>().flatMap { it.modelList.get() })
        }.toList()

    /**
     * Returns a list of all synapse groups including those in subnetworks.
     */
    val flatSynapseGroupList: List<SynapseGroup>
        get() = sequence {
            yieldAll(networkModels.get<SynapseGroup>())
            yieldAll(networkModels.get<Subnetwork>().flatMap { it.modelList.get() })
        }.toList()

    /**
     * Returns a list of all weight matrices including those in subnetworks.
     */
    val flatWeightMatrixList: List<WeightMatrix>
        get() = sequence {
            yieldAll(networkModels.get<WeightMatrix>())
            yieldAll(networkModels.get<Subnetwork>().flatMap { it.modelList.get() })
        }.toList()

    private fun assignId(model: NetworkModel) {
        model.id = idManager.getAndIncrementId(model.javaClass)
        when (model) {
            is NeuronGroup -> model.neuronList.forEach { assignId(it) }
            is SynapseGroup -> model.synapses.forEach { assignId(it) }
            is Subnetwork -> model.modelList.all.forEach { assignId(it) }
        }
    }

    /**
     * Add a new [NetworkModel]. All network models must be added using this method.
     * For best results call with `?.await()` when possible.
     */
    @JvmOverloads
    fun addNetworkModel(model: NetworkModel, usePlacementManager: Boolean = true): Deferred<Boolean>? {
        if (model.shouldAdd()) {
            assignId(model)
            networkModels.add(model)
            if (usePlacementManager && model is LocatableModel && model.shouldBePlaced) {
                placementManager.placeObject(model)
            }
            model.events.deleted.on(wait = true) {
                networkModels.remove(it)
                events.modelRemoved.fire(it).join()
                updatePriorityList()
            }
            val deferred = events.modelAdded.fire(model)
            if (model is Neuron) {
                model.events.priorityChanged.on { _, _ ->
                    updatePriorityList()
                }
                model.events.updateRuleChanged.on { _, _ -> shouldUpdateTimeType = true }
                updatePriorityList()
            }
            return deferred
        }
        return null
    }

    /**
     * Returns the precision of the current time step.
     *
     * @return the precision of the current time step.
     */
    private fun getTimeStepPrecision(): Int = ceil(ln(timeStep) / LOG_10).toInt().let {
        if (it < 0) {
            abs(it) + 1
        } else {
            0
        }
    }

    /**
     * Returns a copy of this network based on its xml rep.
     *
     * @return the copied network.
     */
    fun copy(): Network {
        val xmlRepresentation = getNetworkXStream().toXML(this)
        return getNetworkXStream().fromXML(xmlRepresentation) as Network
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    private fun readResolve(): Any {

        job = SupervisorJob()

        coroutineContext = Dispatchers.Default + job

        placementManager = PlacementManager()

        events = NetworkEvents()
        updateCompleted = AtomicBoolean(false)
        updatePriorityList();

        // Initialize update manager
        networkModels.allInUpdatingOrder.forEach { model ->
            model.events.deleted.on(wait = true) {
                networkModels.remove(it)
                events.modelRemoved.fire(it)
            }
        }
        idManager = SimpleIdManager ({ cls -> networkModels.getRawModelSet(cls).size + 1 })
        return this
    }

    /**
     * Returns the current number of iterations.
     *
     * @return the number of update iterations which have been run since the network was created.
     */
    val iterations: Long get() = (time / timeStep).toLong()

    /**
     * string version of time, with units.
     */
    val timeLabel: String
        get() = if (timeType == TimeType.DISCRETE) {
            "$iterations iterations"
        } else {
            "${SimbrainMath.roundDouble(time, getTimeStepPrecision() + 1)} msec"
        }

    /**
     * If there is a single continuous neuron in the network, consider this a continuous network.
     */
    private fun updateTimeType() {
        timeType = TimeType.DISCRETE
        if (flatNeuronList.any { it.timeType == TimeType.CONTINUOUS }) {
            timeType = TimeType.CONTINUOUS
        }
    }

    /**
     * Increment the time counter, using a different method depending on whether this is a continuous or discrete.
     * network.
     */
    fun updateTime() {
        time += timeStep
    }

    /**
     * Reset time. Note that last spike time computations for spiking networks will be incorrect.
     */
    fun resetTime() {
        time = 0.0
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before updating again.
     *
     * @return whether the network has been updated or not
     */
    fun isUpdateCompleted(): Boolean {
        return updateCompleted.get()
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before updating again.
     *
     * @param b whether the network has been updated or not.
     */
    fun setUpdateCompleted(b: Boolean) {
        updateCompleted.set(b)
    }

    override fun toString(): String =
        "------Network------\n" + networkModels

    /**
     * Forward to [NetworkUpdateManager.addAction]
     */
    fun addUpdateAction(action: UpdateAction) {
        updateManager.addAction(action)
    }

    /**
     * Forward to [NetworkUpdateManager.removeAction]
     */
    fun removeUpdateAction(action: UpdateAction) {
        updateManager.removeAction(action)
    }


    /**
     * Adds a list of network elements to this network. Used in copy / paste.
     *
     * @param toAdd list of objects to add.
     */
    @JvmOverloads
    fun addNetworkModels(toAdd: List<NetworkModel>, usePlacementManager: Boolean = true) = toAdd.mapNotNull { addNetworkModel(it, usePlacementManager) }

    /**
     * Var arg version of addNetworkModels.
     *
     * Ex: addNetworkModels(synapse1, synapse2, neuron1, neuron2, ...)
     */
    fun addNetworkModels(vararg toAdd: NetworkModel, usePlacementManager: Boolean = true) = toAdd.mapNotNull { addNetworkModel(it, usePlacementManager) }

    fun selectModels(models: List<NetworkModel>) {
        events.selected.fire(models)
    }

}