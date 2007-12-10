package org.simbrain.world.threedee.gui;

import org.simbrain.world.threedee.Moveable;

import com.jme.math.Vector3f;

/**
 * A view that can be moved around freely in three dimensions.
 * 
 * @author Matt Watson
 */
public class FreeBirdView extends Moveable {
    /** The height at which the free-bird starts. */
    private static final float START_HEIGHT = 25f;
    
    /** The current direction of the view. */
    private Vector3f direction;

    /** The current location of the view. */
    private Vector3f location;

    /**
     * {@inheritDoc}
     */
    @Override
    protected Vector3f getDirection() {
        return direction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Vector3f getLocation() {
        return location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final Vector3f direction, final Vector3f location) {
        this.direction = direction;
        this.location = location.add(0, START_HEIGHT, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateDirection(final Vector3f direction) {
        this.direction = direction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateLocation(final Vector3f location) {
        this.location = location;
    }
}
