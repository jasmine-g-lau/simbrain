package org.simbrain.world.odorworld.sensors

import org.simbrain.util.UserParameter
import org.simbrain.world.odorworld.entities.PeripheralAttribute
import org.simbrain.world.odorworld.events.SensorEffectorEvents
import java.util.*

/**
 * Interface for 2d world sensors.  Sensors have a position given in polar
 * coordinates.
 */
abstract class Sensor : PeripheralAttribute {
    /**
     * The id of this smell sensor.
     */
    // @UserParameter(label = "Sensor ID", description = "A unique id for this sensor", order = 0, displayOnly = true)
    override var id: String? = null

    /**
     * Public label of this sensor.
     */
    @UserParameter(
        label = "Label",
        description = "Optional string description associated with this sensor",
        order = 1
    )
    private var label = ""

    /**
     * Handle events.
     */
    @Transient
    private var events = SensorEffectorEvents()

    /**
     * Construct a sensor.
     *
     * @param label  a label for this sensor
     */
    constructor(label: String) : super() {
        this.label = label
    }

    /**
     * Construct a copy of a sensor.
     *
     * @param sensor the sensor to copy
     */
    constructor(sensor: Sensor) {
        label = sensor.label
    }

    /**
     * Default no-arg constructor for [org.simbrain.util.propertyeditor.AnnotatedPropertyEditor].
     */
    constructor() : super()

    override fun getLabel(): String {
        return label
    }

    override fun setLabel(label: String) {
        this.label = label
        getEvents().propertyChanged.fireAndForget()
    }

    abstract override fun copy(): Sensor
    override fun getEvents(): SensorEffectorEvents {
        return events
    }

    fun readResolve(): Any {
        events = SensorEffectorEvents()
        return this
    }

    companion object {
        var types = Arrays.asList<Class<*>>(
            SmellSensor::class.java,
            Hearing::class.java,
            GridSensor::class.java,
            ObjectSensor::class.java,
            BumpSensor::class.java,
            TileSensor::class.java
        )
    }
}