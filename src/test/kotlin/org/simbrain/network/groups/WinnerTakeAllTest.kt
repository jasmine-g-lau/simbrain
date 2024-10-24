package org.simbrain.network.groups

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.connect
import org.simbrain.network.neurongroups.WinnerTakeAll

class WinnerTakeAllTest {

    var net = Network()
    val wta = WinnerTakeAll(net, 2)
    val n1 = Neuron()
    val n2 = Neuron()

    init {
        with(net) {
            net.addNetworkModels(wta, n1, n2)
            n1.clamped = true
            n2.clamped = true
            connect(n1, wta.getNeuron(0), 1.0)
            connect(n2, wta.getNeuron(1), 1.0)
        }
    }

    @Test
    fun `Check that at any time there is just one winner` () {
        net.update()
        assertEquals(1, wta.neuronList.count { it.activation > 0.0 })
    }

    @Test
    fun `Check that node with most input wins` () {
        n1.activation = 1.0
        n2.activation = .9
        net.update()
        assertEquals(1.0, wta.getNeuron(0).activation)
        assertEquals(0.0, wta.getNeuron(1).activation)
        n1.activation = -1.0
        n2.activation = 0.2
        net.update()
        assertEquals(0.0, wta.getNeuron(0).activation)
        assertEquals(1.0, wta.getNeuron(1).activation)
    }

    @Test
    fun `Check that if equal input a random node wins`() {
        n1.activation = 1.0
        n2.activation = 1.0
        val result = (0..100).map {
            net.update()
            wta.getWinner()
        }.toSet().size
        assertEquals(2,result)
    }

    @Test
    fun `Check that winning and losing value works` () {
        wta.params.winValue = 2.0
        wta.params.loseValue = -.5
        n1.activation = 1.0
        n2.activation = .9
        net.update()
        assertEquals(2.0, wta.getNeuron(0).activation)
        assertEquals(-0.5, wta.getNeuron(1).activation)
    }

}


