/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.neurongroups

import org.simbrain.network.core.InfoText
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.gui.dialogs.NetworkPreferences.weightRandomizer
import org.simbrain.network.layouts.HexagonalGridLayout
import org.simbrain.network.util.SimnetUtils
import org.simbrain.util.UserParameter
import org.simbrain.util.Utils
import org.simbrain.util.propertyeditor.CustomTypeName
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.stats.ProbabilityDistribution
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Implements a Self-Organizing Map
 *
 * @author William B. St. Clair
 * @author Jeff Yoshimi
 */
class SOMGroup @JvmOverloads constructor(
    neurons: List<Neuron>,
    params: SOMParams = SOMParams()
) : NeuronGroup() {

    var params by GuiEditable(
        label = "SOM Parameters",
        description = "Parameters for the SOM",
        initValue = params.apply { creationMode = false },
        order = 50
    )

    constructor(numNeurons: Int) : this(List(numNeurons) { Neuron() })

    @XStreamConstructor
    private constructor() : this(listOf())

    init {
        addNeurons(neurons)
        neurons.forEach{n ->
            n.upperBound = 1.0
            n.lowerBound = -1.0
        }
        layout = HexagonalGridLayout(50.0, 50.0, sqrt(neurons.size.toDouble()).toInt())
    }

    override fun copy() = SOMGroup(neuronList.map { it.copy() }, params.copy())

    var neighborhoodSize = params.initNeighborhoodSize
    var learningRate = params.initialLearningRate
    var winDistance = 0.0
    var distance = 0.0
    var value = 0.0
    var winner: Neuron? = null

    override val customInfo = InfoText(getStateInfoText())

    /**
     * Randomize all weights coming in to this network. The weights will be
     * between 0 and the upper bound of each synapse.
     */
    override fun randomizeIncomingWeights(randomizer: ProbabilityDistribution?) {
        for (n in neuronList) {
            for (s in n.fanIn) {
                s.lowerBound = 0.0
                s.strength = (randomizer ?: weightRandomizer).sampleDouble()
            }
        }
    }

    /**
     * Pushes the weight values of an SOM neuron onto the input neurons.
     */
    fun recall() {
        val maxActivation = Double.MIN_VALUE
        var mostActivatedNeuron: Neuron? = null
        for (neuron in this.neuronList) {
            if (neuron.activation > maxActivation) {
                mostActivatedNeuron = neuron
            }
        }
        if (mostActivatedNeuron != null) {
            val incomingNeurons: MutableList<Neuron> = ArrayList()
            for (incoming in mostActivatedNeuron.fanIn) {
                incoming.source.activation = incoming.strength
                incomingNeurons.add(incoming.source)
            }
        }
    }

    /**
     * Resets SOM Network to initial values.
     */
    fun reset() {
        learningRate = params.initialLearningRate
        neighborhoodSize = params.initNeighborhoodSize
        updateStateInfoText()
        events.customInfoUpdated.fire()
    }

    /**
     * Update the network. This method has the following structure: If all
     * weights are clamped, return. Determine the winner by finding which of the
     * SOM neurons is closest to the input vector. Update the winning neuron and
     * its neighborhood. The update algorithm accounts for all possible
     * arrangements of the SOM network. - When the neuron is outside of the
     * neighborhood. - When the neuron is within the the neighborhood. Including
     * the current vector, if the total number of vectors analyzed during the
     * current iteration is equal to the total number of vectors to be analyzed,
     * update the network parameters and count one full iteration. Else the
     * network must be in recallMode. If all neurons are clamped, return. Find
     * the SOM neuron with highest activation. Set the activations of input
     * neurons according to the SOM weights.
     */
    override fun update() {
        winDistance = Double.POSITIVE_INFINITY
        // winner = 0;
        var physicalDistance: Double

        // Determine Winner and update neurons: The SOM Neuron with the lowest
        // distance between  its weight vector and the input neurons's weight
        // vector.
        winner = calculateWinner()
        for (i in neuronList.indices) {
            val n = neuronList[i]
            if (n === winner) {
                n.activation = 1.0
            } else {
                n.activation = 0.0
            }
        }

        if (winner == null) {
            return
        }

        // Update Synapses of the neurons within the radius of the winning
        // neuron.
        for (i in neuronList.indices) {
            val neuron = neuronList[i]
            physicalDistance = SimnetUtils.getEuclideanDist(neuron, winner)
            // The center of the neuron is within the update region.
            if (physicalDistance <= neighborhoodSize) {
                for (incoming in neuron.fanIn) {
                    value = incoming.strength + learningRate * (incoming.source.activation - incoming.strength)
                    incoming.strength = value
                }
            }
        }

        // Update alpha and neighborhood size
        learningRate -= learningRate * params.initialLearningRate
        if (neighborhoodSize - params.neighborhoodDecayAmount > 0.0) {
            neighborhoodSize -= params.neighborhoodDecayAmount
        } else {
            neighborhoodSize = 0.0
        }

        // For box
        customInfo.text = getStateInfoText()
        events.customInfoUpdated.fire()
    }

    fun getStateInfoText() = """
        Learning rate (${Utils.round(learningRate, 2)})
        N-size (${Utils.round(neighborhoodSize, 2)})
    """.trimIndent()

    fun updateStateInfoText() {
        customInfo.text = getStateInfoText()
        events.customInfoUpdated.fire()
    }

    /**
     * Find the SOM neuron which is closest to the input vector.
     *
     * @return winner
     */
    private fun calculateWinner(): Neuron? {
        var winner: Neuron? = null
        for (i in neuronList.indices) {
            val n = neuronList[i]
            distance = findDistance(n)
            if (distance < winDistance) {
                winDistance = distance
                winner = n
            }
        }
        return winner
    }

// TODO: Reuse our functions?
    /**
     * Calculates the Euclidian distance between the SOM neuron's weight vector
     * and the input vector.
     *
     * @param n The SOM neuron one wishes to find the for.
     * @return distance.
     */
    private fun findDistance(n: Neuron): Double {
        var ret = 0.0
        for (incoming in n.fanIn) {
            ret += (incoming.strength - incoming.source.activation).pow(2.0)
        }
        return ret
    }

}

@CustomTypeName("SOM Group")
class SOMParams : NeuronGroupParams() {

    var initialLearningRate: Double by GuiEditable(
        label = "Initial learning rate",
        description = "Initiate Learning rate, which then decays",
        initValue = 0.06,
        order = 60
    )

    var initNeighborhoodSize by GuiEditable(
        label = "Initial Neighborhood size",
        description = "Initial radius around each neuron within which learning takes place",
        initValue = 100.0,
        order = 70
    )

    @UserParameter(label = "Learning decay rate", )
    var learningDecayRate: Double by GuiEditable(
        label = "Learning decay rate",
        initValue = 0.002,
        description = "The rate at which the learning rate decays.",
        order = 90
    )

    var neighborhoodDecayAmount: Double by GuiEditable(
        label = "Neighborhood decay rate",
        initValue = .05,
        description = "The amount that the neighborhood decrements at each iteration",
        order = 100
    )

    override fun create(): SOMGroup {
        return SOMGroup(List(numNeurons) { Neuron() }, this.copy())
    }

    override fun copy(): SOMParams {
        return SOMParams().also {
            commonCopy(it)
            it.initNeighborhoodSize = initNeighborhoodSize
            it.initialLearningRate = initialLearningRate
            it.learningDecayRate = learningDecayRate
            it.neighborhoodDecayAmount = neighborhoodDecayAmount
        }
    }

}

