package org.simbrain.world.odorworld.sensors;

import org.simbrain.util.UserParameter;
import org.simbrain.util.decayfunctions.DecayFunction;
import org.simbrain.util.decayfunctions.LinearDecayFunction;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Sensor that reacts when an object of a given type is near it.
 * <br>
 * While the smell framework involves objects emitting smells, object type
 * sensors have a sensitivity, and are more "sensor" or "subject" based than
 * object based.
 * <br>
 * The sensor itself is currently fixed at the center of the agent. We may
 * make the location editable at some point, if use-cases emerge.
 */
public class ObjectSensor extends Sensor implements VisualizableEntityAttribute {

    /**
     * Current value of the sensor.
     */
    private double value = 0;

    @UserParameter(
            description = "Maximum value of the sensor when agent is right on top of the associated object type",
            label = "Max Value",
            order = 10)
    private double baseValue = 1;

    /**
     * Decay function
     */
    @UserParameter(label = "Decay Function", isObjectType = true,
        showDetails = false, order = 15)
    DecayFunction decayFunction = new LinearDecayFunction(70);

    /**
     * The type of the object represented, e.g. Swiss.gif.
     */
    @UserParameter(label = "Object Type",
        description = "What type of object this sensor responds to",
        order = 3)
    private EntityType objectType = EntityType.SWISS;

    /**
     * Should the sensor node show a label on top.
     */
    @UserParameter(label = "Show Label",
            description = "Show label on top of the sensor node",
            order = 4)
    private boolean showLabel = false;

    /**
     * Instantiate an object sensor.
     *
     * @param objectType the type (e.g. Swiss.gif)
     */
    public ObjectSensor(EntityType objectType) {
        this.objectType = objectType;
    }

    /**
     * Construct a copy of a object sensor.
     *
     * @param objectSensor the object sensor to copy
     */
    public ObjectSensor(ObjectSensor objectSensor) {
        super(objectSensor);
        this.setId(objectSensor.getId());
        this.baseValue = objectSensor.baseValue;
        this.decayFunction = (DecayFunction) objectSensor.decayFunction.copy();
        this.objectType = objectSensor.objectType;
        this.showLabel = objectSensor.showLabel;
    }

    /**
     * Default constructor for {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     */
    public ObjectSensor() {
        super();
    }

    public DecayFunction getDecayFunction() {
        return decayFunction;
    }

    public void setDecayFunction(DecayFunction decayFunction) {
        this.decayFunction = decayFunction;
    }

    public ObjectSensor(EntityType type, double radius, double angle) {
        objectType = type;
        this.radius = radius;
        this.theta = angle;
    }

    @Override
    public void update(OdorWorldEntity parent) {
        value = 0;
        for (OdorWorldEntity otherEntity : parent.getEntitiesInRadius(decayFunction.getDispersion())) {
            if (otherEntity.getEntityType() == objectType) {
                double scaleFactor = decayFunction.getScalingFactor(
                    SimbrainMath.distance(computeLocationFrom(parent), otherEntity.getLocation()));
                value += baseValue * scaleFactor;
            }
        }
    }

    @Producible(customDescriptionMethod = "getAttributeDescription")
    public double getCurrentValue() {
        return value;
    }

    @Override
    public ObjectSensor copy() {
        return new ObjectSensor(this);
    }

    @Override
    public String getName() {
        return "Object Sensor";
    }

    public double getBaseValue() {
        return baseValue;
    }

    public boolean isShowLabel() {
        return showLabel;
    }

    public void setRange(double value) {
        decayFunction.setDispersion(value);
    }

    public void setObjectType(EntityType objectType) {
        this.objectType = objectType;
    }

    @Override
    public String getLabel() {
        if (super.getLabel().isEmpty()) {
            return getDirectionString() + objectType.getDescription() + " Detector";
        } else {
            return super.getLabel();
        }
    }
}