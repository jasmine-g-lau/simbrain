/*
 * Copyright (C) 2005,2007 The Authors. See http://www.simbrain.net/credits This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.connections

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.UserParameter
import org.simbrain.util.cartesianProduct
import kotlin.random.Random

/**
 * Connect every source neuron to every target neuron.
 *
 * @author Zoë Tosi
 * @author Jeff Yoshimi
 */
class AllToAll @JvmOverloads constructor(

    /**
     * Whether or not connections where the source and target are the same
     * neuron are allowed. Only applicable if the source and target neuron sets
     * are the same.
     */
    @UserParameter(
        label = "Self-Connections Allowed ",
        description = "Can there exist synapses whose source and target are the same?",
        order = 1
    )
    var allowSelfConnection: Boolean = NetworkPreferences.selfConnectionAllowed,

    seed: Long = Random.nextLong()

) : ConnectionStrategy(seed) {

    override val name: String = "All to All"

    override fun toString(): String {
        return name
    }

    override fun copy(): AllToAll {
        return AllToAll(allowSelfConnection).also {
            commonCopy(it)
        }
    }

    override fun connectNeurons(
        source: List<Neuron>,
        target: List<Neuron>
    ): List<Synapse> {
        val syns = createAllToAllSynapses(source, target, allowSelfConnection)
        polarizeSynapses(syns, percentExcitatory, random)
        return syns
    }

}

/**
 * Connects every source neuron to every target neuron.
 */
fun createAllToAllSynapses(
    sourceNeurons: List<Neuron>,
    targetNeurons: List<Neuron>,
    allowSelfConnection: Boolean = false
): List<Synapse> {
    return (sourceNeurons cartesianProduct targetNeurons)
        .filter { (src, tar) ->
            allowSelfConnection || src !== tar
        }.map { (src, tar) ->
            Synapse(src, tar).apply { strength = 1.0 }
        }
}
