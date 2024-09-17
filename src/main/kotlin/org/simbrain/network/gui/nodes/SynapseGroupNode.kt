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
package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.*


class SynapseGroupNode(networkPanel: NetworkPanel, val synapseGroup: SynapseGroup) :
    ScreenElement(networkPanel) {

    /**
     * Reference to the currently used PNode type.
     */
    private var currentNode: Arrow? = null

    /**
     * PNode that represents an aggregate of visible "loose" [SynapseNode]s.
     * when [SynapseGroup.isDisplaySynapses] is true.
     */
    var expandedNode: SynapseGroupNodeExpanded? = null
        private set

    /**
     * PNode that represents a single one-directional green arrow from
     * one neuron group to another.
     */
    private var directedNode: SynapseGroupNodeDirected? = null

    /**
     * The interaction box for this neuron group.
     */
    var interactionBox: SynapseGroupInteractionBox

    init {
        // Note the children pnodes to outlined objects are created in
        // networkpanel and added externally to outlined objects
        interactionBox = SynapseGroupInteractionBox(networkPanel, synapseGroup, this)
        interactionBox.setText(synapseGroup.displayName)
        addChild(interactionBox)
        fun invalidateArrow() {
            when (currentNode) {
                directedNode -> directedNode?.invalidateFullBounds()
                expandedNode -> expandedNode?.invalidateFullBounds()
            }
        }
        synapseGroup.source.events.locationChanged.on(Dispatchers.Swing) {
            invalidateArrow()
            updateInteractionBoxLocation()
        }
        synapseGroup.target.events.locationChanged.on(Dispatchers.Swing) {
            invalidateArrow()
            updateInteractionBoxLocation()
        }
        updateInteractionBoxLocation()

        // Handle events
        val events = synapseGroup.events
        events.labelChanged.on { _, _ -> updateText() }
        events.visibilityChanged.on(Dispatchers.Swing) { setVisibility() }
        events.synapseAdded.on(dispatcher = Dispatchers.Swing) {
            this@SynapseGroupNode.networkPanel.createNode(it)
            refreshVisible()
        }
        events.synapseRemoved.on(wait = true) { s ->
            s.delete()
        }
        events.synapseRemoved.on(dispatcher = Dispatchers.Swing) {
            refreshVisible()
        }
        events.synapseListChanged.on(dispatcher = Dispatchers.Swing) {
            setVisibility()
            refreshVisible()
        }
        setVisibility()
        interactionBox.invalidateFullBounds()
    }

    private fun removeEverythingButInteractionBox() {
        removeChild(directedNode)
        removeChild(expandedNode)
    }

    private fun refreshVisible() {
        removeChild(expandedNode)
        expandedNode = null
        setVisibility()
    }

    private fun updateInteractionBoxLocation() {
        val (x, y) = ((synapseGroup.target.location - synapseGroup.source.location) / 2) + synapseGroup.source.location
        interactionBox.centerFullBoundsOnPoint(x, y)
    }

    private fun setVisibility() {
        if (synapseGroup.displaySynapses) {
            removeEverythingButInteractionBox()
            if (expandedNode == null) {
                expandedNode = SynapseGroupNodeExpanded(networkPanel, this)
            }
            addChild(expandedNode)
            interactionBox.raiseAbove(expandedNode)
            currentNode = expandedNode
        } else {
            removeEverythingButInteractionBox()
            if (!synapseGroup.isRecurrent()) {
                if (directedNode == null) {
                    directedNode = SynapseGroupNodeDirected(this)
                }
                addChild(directedNode)
                interactionBox.raiseAbove(directedNode)
                currentNode = directedNode
            }
        }
    }

    /**
     * Update the text in the interaction box.
     */
    fun updateText() {
        interactionBox.setText(synapseGroup.displayName)
    }

    override val isDraggable: Boolean = false

    override val model: SynapseGroup
        get() = synapseGroup

    /**
     * Interface for all PNodes used in as the main representation for a synapse group.
     */
    interface Arrow {
        fun layoutChildren()
    }

}