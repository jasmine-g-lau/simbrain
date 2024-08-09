package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.util.BiasedMatrixData
import org.simbrain.util.rowVectorTransposed
import org.simbrain.util.toDoubleArray
import org.simbrain.util.toMatrix
import smile.math.matrix.Matrix

class TrainingUtilsTest {

    val net = Network()
    val na1 = NeuronArray(2)
    val na2 = NeuronArray(3)
    val na3 = NeuronArray(2)
    var na1DataHolder = (na1.dataHolder as BiasedMatrixData)
    val wm1 = WeightMatrix(na1, na2)
    val wm2 = WeightMatrix(na2, na3)

    init {
        listOf(na1, na2, na3).forEach {
            it.clear()
        }
        net.addNetworkModels(na1, na2, na3, wm1, wm2)
    }

    @Test
    fun `test neuron array error`() {
        na1.setActivations(doubleArrayOf(-1.0, 1.0))
        val error = na1.getError(doubleArrayOf(1.0, 1.0).toMatrix())
        assertArrayEquals(doubleArrayOf(2.0, 0.0), error.toDoubleArray())
    }

    @Test
    fun `test bias update`() {
        na1DataHolder.biases = doubleArrayOf(1.0, 1.0).toMatrix()
        val error = na1.getError(doubleArrayOf(0.0, 1.0).toMatrix())
        // Change to bias is 0,1, so biases should become 1,2
        na1.updateBiases(error, 1.0)
        assertArrayEquals(doubleArrayOf(1.0, 2.0 ), na1DataHolder.biases.toDoubleArray())
        na1.updateBiases(error, 1.0)
        assertArrayEquals(doubleArrayOf(1.0, 3.0 ), na1DataHolder.biases.toDoubleArray())
        na1.updateBiases(error, .1)
        assertArrayEquals(doubleArrayOf(1.0, 3.1 ), na1DataHolder.biases.toDoubleArray())
        error.mul(-1.0)
        na1.updateBiases(error, 1.0)
        assertArrayEquals(doubleArrayOf(1.0, 2.1 ), na1DataHolder.biases.toDoubleArray())
    }

    @Test
    fun `test forward pass`() {
        val inputs = Matrix.column(doubleArrayOf(-1.0, 1.0))
        with(net) {
            listOf(wm1, wm2).forwardPass(inputs)
            listOf(wm1, wm2).printActivationsAndWeights(true)
        }
        assertArrayEquals(inputs.toDoubleArray(), wm2.target.activations.toDoubleArray())
    }

    @Test
    fun `test connector chain`() {
        // Should return [wm1, wm2]
        val chain = getConnectorChain(na1, na3)
        assertEquals(2, chain.size)
        assertEquals(wm1, chain[0])
        assertEquals(wm2, chain[1])
    }

    @Test
    fun `test weight matrix tree on a simple chain`() {
        // na1 - wm1 - na2 - wm2 - na3
        // This is represented by [[wm1],[wm2]]
        val wmTree = WeightMatrixTree(listOf(na1), na3)
        assertEquals(2, wmTree.tree.size)
        assertEquals(1, wmTree.tree[0].size)
        assertEquals(wm1, wmTree.tree[0].first())
        assertEquals(1, wmTree.tree[1].size)
        assertEquals(wm2, wmTree.tree[1].first())
    }

    @Test
    fun `test weight matrix tree with a branch`() {
        // [[wm1, wm1_2],[wm2]]
        val na1_2 = NeuronArray(3)
        val wm1_2 = WeightMatrix(na1_2, na2)
        net.addNetworkModels(na1_2, wm1_2)
        val wmTree = WeightMatrixTree(listOf(na1, na1_2), na3)
        assertEquals(2, wmTree.tree.size)
        assertEquals(2, wmTree.tree[0].size)
        assertEquals(1, wmTree.tree[1].size)
        assertTrue(wmTree.tree[0].contains(wm1))
        assertTrue(wmTree.tree[0].contains(wm1_2))
        assertTrue(wmTree.tree[1].first() == wm2)
    }

}