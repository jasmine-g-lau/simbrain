package org.simbrain.network.core

import org.simbrain.network.events.LocationEvents
import org.simbrain.util.UserParameter
import org.simbrain.util.toDoubleArray
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Producible
import smile.math.matrix.Matrix
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D

/**
 * Superclass for network models involved in, roughly speaking, array based computations. Simbrain layers are connected
 * to each other by [Connector]s. Subclassses include neuron arrays, collections of neurons, and deep networks,
 * backed by different data structures, including java arrays, Smile Matrices, and Tensor Flow tensors.
 * <br></br>
 * This class maintains connectors, id, and events, etc.
 * <br></br>
 * Input and output functions must be provided in Smile matrix format to support communication between different types
 * of layer. Smile matrices are the "lingua franca" of layers.
 * <br></br>
 * However, specific subclasses can communicate in other ways to support fast or custom communication. For example,
 * tensor-based layers and connectors could communicate using tensor flow operations, or specialized layers with
 * multiple arrays could be joined by special connectors making use of those arrays.
 *
 * @author Jeff Yoshimi
 * @author Yulin Li
 */
abstract class Layer : LocatableModel(), AttributeContainer {
    // TODO: Currently Smile Matrices are the "lingua Franca" for different layers.
    //  Keep an eye on Kotlin's Multik as a possible alternative
    /**
     * "Fan-in" of incoming connectors.
     */
    @Transient
    var incomingConnectors: MutableList<Connector> = ArrayList()

    /**
     * "Fan-out" of outgoing connectors.
     */
    @Transient
    var outgoingConnectors: MutableList<Connector> = ArrayList()

    /**
     * Collects inputs from other network models using arrays. Cannot be set directly. Use [addInputs] instead.
     */
    abstract val inputs: Matrix

    private var _inputData: Matrix? = null

    /**
     * This data can be used to update the layer’s state. It will often be empty. Currently used by [createTestInputPanel]
     */
    var inputData: Matrix
        get() {
            if (_inputData == null) {
                _inputData = Matrix(10, this.size)
            }
            return _inputData!!
        }
        set(inputData) {
            _inputData = inputData
        }

    /**
     * Add inputs to input vector. Performed in first pass of [org.simbrain.network.update_actions.BufferedUpdate]
     * Asynchronous buffered update assumes that inputs are aggregated in one pass then updated in a second pass.
     * Thus setting inputs directly is a dangerous operation and so is not allowed.
     */
    abstract fun addInputs(inputs: Matrix)

    context(Network) override fun accumulateInputs() {
        incomingConnectors.forEach { it.updatePSR() }
    }

    open fun setActivations(activations: DoubleArray) {
        throw RuntimeException("applyActivations not implemented")
    }

    /**
     * In subclasses under [NeuronArray] the matrices are basic and computations are performed on those.
     * In subclasses under [AbstractNeuronCollection], [activationArray] is basic and computations are performed on those.
     */
    abstract val activations: Matrix

    /**
     * See [activations].
     */
    abstract val activationArray: DoubleArray

    abstract val biases: Matrix

    abstract val biasArray: DoubleArray

    @get:Producible
    val outputArray: DoubleArray
        get() = activations.toDoubleArray()

    @get:Producible
    open val spikes: DoubleArray
        get() = DoubleArray(this.size)

    /**
     * Width of layer. Mainly used by graphica arrows drawn to represent [Connector]s.
     */
    open var width = 0.0
        set(width) {
            field = width
            events.locationChanged.fire()
        }

    /**
     * Height of layer
     */
    open var height = 0.0
        set(height) {
            field = height
            events.locationChanged.fire()
        }

    /**
     * Event support.
     */
    @Transient
    override val events: LocationEvents = LocationEvents()

    /**
     * Returns the size of the activation array / input array for this layer.
     */
    abstract val size: Int

    /**
     * Needed so arrow can be set correctly
     */
    abstract val bound: Rectangle2D

    fun addIncomingConnector(connector: Connector) {
        incomingConnectors.add(connector)
    }

    fun removeIncomingConnector(connector: Connector) {
        incomingConnectors.remove(connector)
    }

    fun addOutgoingConnector(connector: Connector) {
        outgoingConnectors.add(connector)
    }

    fun removeOutgoingConnector(connector: Connector) {
        outgoingConnectors.remove(connector)
    }

    override fun delete() {
        events.deleted.fireAndBlock(this)
    }

    /**
     * See [org.simbrain.workspace.serialization.WorkspaceComponentDeserializer]
     */
    open fun readResolve(): Any? {
        return this
    }

    override var location: Point2D = Point2D.Double()
        set(location) {
            field.setLocation(location)
            events.locationChanged.fire()
        }
}
