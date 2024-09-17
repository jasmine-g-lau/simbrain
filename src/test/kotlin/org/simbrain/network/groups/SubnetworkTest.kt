package org.simbrain.network.groups

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.subnetworks.FeedForward
import java.awt.geom.Point2D

class SubnetworkTest {

    var net = Network()

    @Test
    fun `ff creation` () {
        val ff = FeedForward(intArrayOf(2,2,2),  Point2D.Double(0.0,0.0))
        assertEquals(5, ff.modelList.size)
    }

    @Test
    fun `ff layer deletion` () = runBlocking {
        val ff = FeedForward(intArrayOf(2,2,2),  Point2D.Double(0.0,0.0))
        val firstLayer = ff.modelList.get<NeuronArray>().first()
        firstLayer.delete() // This should get rid of a weight matrix
        assertEquals(3, ff.modelList.size)
    }

    @Test
    fun `subnet deleted when empty` () = runBlocking {
        val ff = FeedForward(intArrayOf(2,2,2),  Point2D.Double(0.0,0.0))
        net.addNetworkModel(ff);
        ff.modelList.all.forEach { it.delete() }
        assertEquals(0, net.allModels.size)
    }
}


