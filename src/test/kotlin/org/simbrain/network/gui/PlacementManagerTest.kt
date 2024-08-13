package org.simbrain.network.gui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.util.point

class PlacementManagerTest {

    val net = Network()
    val pm = PlacementManager()

    @Test
    fun `test default placements`() {

        // Place two neurons. They should be offset by default amount
        val n1 = Neuron()
        net.addNetworkModel(n1)
        pm.placeObject(n1)
        val n2 = Neuron()
        net.addNetworkModel(n2)
        pm.placeObject(n2)
        val neuronOffset = pm.offsetMap.get(n2::class)
        assertEquals(neuronOffset!!.x, n2.x, .01)
    }

    @Test
    fun `test initial location change`() {

        pm.lastClickedLocation = point(100.0, 0.0)
        val n1 = Neuron()
        net.addNetworkModel(n1)
        pm.placeObject(n1)
        assertEquals(100.0, n1.x, .01 )

        // Subsequent should be offset from there by default amount
        val n2 = Neuron()
        net.addNetworkModel(n2)
        pm.placeObject(n2)
        val neuronOffset =  pm.offsetMap.get(n2::class)
        assertEquals(100 + neuronOffset!!.x, n2.x, .01)
    }

    @Test
    fun `test neuron array`() {
        val na1 = NeuronArray(20)
        net.addNetworkModel(na1)
        pm.placeObject(na1)
        val na2 = NeuronArray(20)
        net.addNetworkModel(na2)
        pm.placeObject(na2)
        val offset = pm.offsetMap.get(na2::class)
        assertEquals(offset!!.y, na2.location.y, .01 )
    }

}