package org.simbrain.network.core

import org.simbrain.network.*
import org.simbrain.network.core.*
import org.simbrain.network.events.NeuronCollectionEvents
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.Layout
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.util.SpikingScalarData
import org.simbrain.util.*
import org.simbrain.util.SimbrainConstants.Polarity
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.workspace.Consumable
import org.simbrain.workspace.Producible
import smile.math.matrix.Matrix
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.min

/**
 * Superclass for neuron collections (which are loose assemblages of neurons) and neuron groups (which enforce consistent
 * neuron update rules and track synapse polarity).
 * <br></br>
 * Subclasses maintain lists of neurons and can copy their activations to matrices. To communicate with other
 * [Layer]s it can create output matrices and accept input matrices, but it wil only create and cache these if
 * relevant methods are called. Matrix based layers should subclass [ArrayLayer]
 */
abstract class AbstractNeuronCollection : Layer(), CopyableObject {

    @Transient
    var incomingSgs: HashSet<SynapseGroup> = HashSet()
        private set

    @Transient
    var outgoingSg: HashSet<SynapseGroup> = HashSet()
        private set

    @Transient
    override val events: NeuronCollectionEvents = NeuronCollectionEvents()

    @get:Producible(arrayDescriptionMethod = "getLabelArray")
    @set:Consumable
    @UserParameter("Activation Array", "Activations", order = 10)
    override var activationArray: DoubleArray
        get() = neuronList
            .map { it.activation }
            .toDoubleArray()
        set(activations) {
            val size = min(activations.size, neuronList.size)
            for (i in 0 until size) {
                neuronList[i].activation = activations[i]
            }
        }

    @UserParameter("Bias Array", "Biases", order = 20)
    override var biasArray: DoubleArray
        get() = neuronList
            .map { it.bias }
            .toDoubleArray()
        set(biases) {
            val size = min(biases.size, neuronList.size)
            for (i in 0 until size) {
                neuronList[i].bias = biases[i]
            }
        }

    override val biases: Matrix
        get() = Matrix.column(biasArray)

    /**
     * Returns an array of binary values that represents the neurons in the neuron list.
     * The value is 1 for spiking neurons that are spiking, and 0 otherwise (non-spiking neurons are always associated with 0s)
     */
    @get:Producible
    override val spikes: DoubleArray
        get() = neuronList.map {
            if ((it.dataHolder as? SpikingScalarData)?.spiked == true) 1.0 else 0.0
        }.toDoubleArray()

    override val inputs: Matrix get() = Matrix.column(inputArray)

    /**
     * References to neurons in this collection
     */
    val neuronList: CopyOnWriteArrayList<Neuron> = CopyOnWriteArrayList()

    /**
     * Space between neurons within a layer.
     */
    var betweenNeuronInterval: Int = 50

    /**
     * In method setLayoutBasedOnSize, this is used as the threshold number of neurons in the group, above which to use
     * grid layout instead of line layout.
     */
    var gridThreshold: Int = 9

    /**
     * The layout for the neurons in this group.
     */
    var layout: Layout = GridLayout()

    override val activations: Matrix
        get() = // TODO: Performance drain? Consider caching this.
            Matrix.column(this.activationArray)

    override fun addInputs(inputs: Matrix) {
        addInputs(inputs.col(0))
    }

    @Consumable
    override fun setActivations(activations: DoubleArray) {
        this.activationArray = activations
    }

    /**
     * Set input values of neurons using an array of doubles. Assumes the order
     * of the items in the array matches the order of items in the neuronlist.
     *
     * Does not throw an exception if the provided input array and neuron list
     * do not match in size.
     */
    @Consumable
    fun addInputs(inputs: DoubleArray) {
        val size = min(inputs.size.toDouble(), neuronList.size.toDouble()).toInt()
        for (i in 0 until size) {
            neuronList[i].addInputValue(inputs[i])
        }
        invalidateCachedInputs()
    }

    private var cachedInputsDirty = true
    private var _cachedInputs = DoubleArray(0)

    /**
     * Return inputs as a double array. Either create the array or return a cache of it.
     */
    @get:Producible
    val inputArray: DoubleArray
        get() {
            if (cachedInputsDirty) {
                _cachedInputs = neuronList
                    .map { it.input }
                    .toDoubleArray()
            }
            return _cachedInputs
        }

    override val size: Int get() = activationArray.size

    /**
     * Get the central x coordinate of this group, based on the positions of the neurons that comprise it.
     */
    val centerX: Double
        get() = neuronList.centerLocation.x

    /**
     * Get the central y coordinate of this group, based on the positions of the neurons that comprise it.
     */
    val centerY: Double
        get() = neuronList.centerLocation.y

    override var location: Point2D
        get() = neuronList.centerLocation
        set(newLocation) {
            val delta = newLocation - location
            neuronList.forEach { it.location += delta }
            events.locationChanged.fire()
        }

    override val bound: Rectangle2D
        get() = neuronList.bound

    val sides: RectangleSides
        get() = neuronList.sides

    /**
     * the longest dimensions upon which neurons are laid out.
     */
    val maxDim: Double
        get() = if (width > height) {
            width
        } else {
            height
        }

    /**
     * Translate all neurons (the only objects with position information).
     *
     * @param offsetX x offset for translation.
     * @param offsetY y offset for translation.
     */
    open fun offset(offsetX: Double, offsetY: Double) {
        for (neuron in neuronList) {
            neuron.offset(offsetX, offsetY, false)
        }
        events.locationChanged.fire()
    }

    /**
     * Returns an neuron using a provided index
     *
     * @param i index of the neuron in the neuron list
     */
    fun getNeuron(i: Int): Neuron {
        return neuronList[i]
    }

    /**
     * Add a neuron to the collection. Exposed in [NeuronCollection] but not in [NeuronGroup]
     */
    protected open fun addNeuron(neuron: Neuron) {
        neuronList.add(neuron)
        addListener(neuron)
    }

    /**
     * Add a collection of neurons.
     */
    protected fun addNeurons(neurons: Collection<Neuron>) {
        neurons.forEach { this.addNeuron(it) }
    }

    /**
     * Add listener to indicated neuron.
     */
    protected fun addListener(n: Neuron) {
        n.events.locationChanged.on { events.locationChanged.fire() }
        n.events.deleted.on(wait = true) { neuron ->
            neuronList.remove(neuron)
            if (isEmpty) {
                delete()
            }
        }
    }

    fun removeNeuron(neuron: Neuron?) {
        neuronList.remove(neuron)
    }

    fun removeAllNeurons() {
        neuronList.clear()
    }

    /**
     * True if the group contains the specified neuron.
     *
     * @param n neuron to check for.
     * @return true if the group contains this neuron, false otherwise
     */
    fun containsNeuron(n: Neuron?): Boolean {
        return neuronList.contains(n)
    }

    /**
     * Set clamping on all neurons in this group.
     *
     * @param clamp true to clamp them, false otherwise
     */
    @Deprecated("Use isAllClamped instead")
    fun setClamped(clamp: Boolean) {
        for (neuron in neuronList) {
            neuron.clamped = clamp
        }
    }

    /**
     * Set all activations to a specified value.
     *
     * @param value the value to set the neurons to
     */
    @Consumable
    fun setActivationLevels(value: Double) {
        for (n in neuronList) {
            n.activation = value
        }
    }

    override fun randomize(randomizer: ProbabilityDistribution?) {
        neuronList.forEach { it.randomize(randomizer) }
    }

    /**
     * Randomize bias for all neurons in group.
     */
    context(Network)
    fun randomizeBiases() {
        for (neuron in neuronList) {
            neuron.randomizeBias()
        }
    }

    /**
     * Return flat list of fanins for all neurons in group.
     */
    val incomingWeights: List<Synapse>
        get() = neuronList.flatMap { it.fanIn }

    /**
     * Return flat list of fanouts for all neurons in group.
     */
    val outgoingWeights: List<Synapse>
        get() = neuronList.flatMap { it.fanOut.values }

    /**
     * Randomize fan-in for all neurons in group.
     */
    open fun randomizeIncomingWeights(randomizer: ProbabilityDistribution? = null) {
        for (neuron in neuronList) {
            neuron.randomizeFanIn(randomizer)
        }
    }

    /**
     * Randomize fan-out for all neurons in group.
     */
    context(Network)
    fun randomizeOutgoingWeights() {
        for (neuron in neuronList) {
            neuron.randomizeFanOut()
        }
    }

    fun removeIncomingSg(sg: SynapseGroup): Boolean {
        return incomingSgs.remove(sg)
    }

    fun removeOutgoingSg(sg: SynapseGroup): Boolean {
        return outgoingSg.remove(sg)
    }

    override suspend fun delete() {
        outgoingSg.forEach { it.delete() }
        incomingSgs.forEach { it.delete() }
        val customInfo = customInfo
        customInfo?.events?.deleted?.fire(customInfo)?.await()
        super.delete()
    }

    context(Network)
    override fun accumulateInputs() {
        // if (inputManager.getData() == null) {
        //     throw new NullPointerException("Test data variable is null," + " but neuron group " + getLabel() + " is in input" + " mode.");
        // }
        // inputManager.applyCurrentRow(); // TODO
        super.accumulateInputs()
        val wtdInputs = DoubleArray(size)
        for (c in incomingConnectors) {
            wtdInputs.addi(c.getSummedPSRs())
        }
        addInputs(wtdInputs)
        addInputs(biasArray)
    }

    var isAllClamped: Boolean
        get() = neuronList.none { !it.clamped }
        set(value) {
            neuronList.forEach { it.clamped = value }
        }

    /**
     * Here to support APE where the conditional can only support one single expression.
     */
    val isAllUnclamped: Boolean
        get() = neuronList.none { it.clamped }

    /**
     * Set the lower bound on all neurons in this group.
     *
     * @param lb the lower bound to set.
     */
    fun setLowerBound(lb: Double) {
        for (neuron in neuronList) {
            neuron.lowerBound = lb
        }
    }

    /**
     * Set the upper bound on all neurons in this group.
     *
     * @param ub the upper bound to set.
     */
    fun setUpperBound(ub: Double) {
        for (neuron in neuronList) {
            neuron.upperBound = ub
        }
    }

    /**
     * Set the increment on all neurons in this group.
     *
     * @param increment the increment to set.
     */
    fun setIncrement(increment: Double) {
        for (neuron in neuronList) {
            neuron.increment = increment
        }
    }

    /**
     * Utility to method (used in couplings) to get a string showing the labels of all "active" neurons (neurons with
     * activation above a threshold).
     *
     * @param threshold threshold above which to consider a neuron "active"
     * @return the "active labels"
     */
    fun getLabelsOfActiveNeurons(threshold: Double): String {
        val strBuilder = StringBuilder("")
        for (neuron in neuronList) {
            if ((neuron.activation > threshold) && (!neuron.label.isNullOrBlank())) {
                strBuilder.append(neuron.label + " ")
            }
        }
        return strBuilder.toString()
    }

    val mostActiveNeuron: String
        /**
         * Returns the label of the most active neuron.
         *
         * @return the label of the most active neuron
         */
        get() {
            return (neuronList.maxBy { it.activation }.label ?: "") + " "
        }

    /**
     * Sets the polarities of every neuron in the group.
     */
    fun setPolarity(p: Polarity) {
        neuronList.forEach { it.polarity = p }
    }

    /**
     * Get the neuron with the specified label, or null if none found.
     *
     * @param label label to search for
     * @return the associated neuron
     */
    fun getNeuronByLabel(label: String?) = neuronList.firstOrNull { it.label.equals(label, ignoreCase = true) }

    protected fun invalidateCachedInputs() {
        cachedInputsDirty = true
    }

    /**
     * Returns an array of labels, one for each neuron this group.
     * Called by reflection for some coupling related events.
     */
    val labelArray: Array<String?>
        get() = neuronList
            .map { if (it.label.isNullOrEmpty()) it.id else it.label }
            .toTypedArray()

    val isEmpty: Boolean
        get() = neuronList.isEmpty()

    val minX: Double
        get() = neuronList.minX

    val maxX: Double
        get() = neuronList.maxX

    val minY: Double
        get() = neuronList.minY

    val maxY: Double
        get() = neuronList.maxY

    override val name = "AbstractNeuronCollection"

    override fun onCommit() {}

    override fun toString(): String {
        return "$id with ${this.activationArray.size} activations: ${Utils.getTruncatedArrayString(
            this.activationArray, 10)}"
    }

    fun clearInputs() {
        neuronList.forEach { it.clearInput() }
    }

    override fun clear() {
        neuronList.forEach { it.clear() }
    }

    override fun increment() {
        neuronList.forEach { it.increment() }
    }

    override fun decrement() {
        neuronList.forEach { it.decrement() }
    }

    override fun toggleClamping() {
        neuronList.forEach { it.toggleClamping() }
    }

    /**
     * If more than gridThreshold neurons use a grid layout, else a horizontal line layout.
     *
     * @param initialPosition the initial Position for the layout
     */
    @JvmOverloads
    fun setLayoutBasedOnSize(initialPosition: Point2D = point(0, 0)) {
        val lineLayout = LineLayout(betweenNeuronInterval.toDouble(), LineLayout.LineOrientation.HORIZONTAL)
        val gridLayout = GridLayout(betweenNeuronInterval.toDouble(), betweenNeuronInterval.toDouble())
        if (neuronList.size < gridThreshold) {
            lineLayout.setInitialLocation(initialPosition)
            layout = lineLayout
        } else {
            gridLayout.setInitialLocation(initialPosition)
            layout = gridLayout
        }
        // Used rather than apply layout to make sure initial position is used.
        layout.layoutNeurons(neuronList)
    }

    open val topLeftLocation: Point2D.Double
        get() = neuronList.topLeftLocation

    /**
     * Apply this group's layout to its neurons.
     */
    fun applyLayout() {
        layout.setInitialLocation(this.topLeftLocation)
        layout.layoutNeurons(neuronList)
    }

    /**
     * Apply this group's layout to its neurons based on a specified top-left initial position.
     *
     * @param initialPosition the position from which to begin the layout.
     */
    fun applyLayout(initialPosition: Point2D?) {
        layout.setInitialLocation(initialPosition)
        layout.layoutNeurons(neuronList)
    }

    /**
     * Forwards to [.applyLayout]
     */
    fun applyLayout(x: Int, y: Int) {
        applyLayout(Point2D.Double(x.toDouble(), y.toDouble()))
    }

    /**
     * Sets a new layout and applies it, using the groups' current location.
     */
    fun applyLayout(newLayout: Layout) {
        layout = newLayout
        applyLayout(location)
    }

    /**
     * Optional information about the current state of the group. For display in
     * GUI.
     */
    open val customInfo: NetworkModel?
        get() = null
}
