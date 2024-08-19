package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addDocViewer
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.createControlPanel
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.activations
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.util.place

/**
 * Demo for studying Hopfield networks,
 */

val hopfieldSim = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    // Hopfield network
    val hopfield = Hopfield(64)
    network.addNetworkModel(hopfield)

    val docViewer = addDocViewer(
        "Information",
        """ 
            # Hopfield Simulation Overview:
            
            The Hopfield simulation is a recurrent neural network with a synaptic connection pattern for pattern recognition and memory retrieval. 

            # What to do:
            
            Select an input pattern and click the train button on the Control panel to train the network on the selected pattern.
            The model learns the pattern and “remembers” it. When randomizing the network (by clicking “N” [Neuron], “R” [Randomize], and “Space” [Iterate], or using “I” [Wand Mode] over the nodes), the network adjusts the nodes on each iteration to reconfigure the inputted pattern. 
            The Network remembers the pattern and the antipattern, and when iterating (“Space”), it iterates to recreate the pattern with the most similar nodes. 
            You can get the pattern to memorize all the different patterns and antipatterns by training each one, randomizing and iterating to see if it is remembered, and training that pattern again if it needs to be learned. 

        
        """.trimIndent()
    )

    withGui {
        place(docViewer, 0, 0, 450, 483)
        place(networkComponent, 604, 0, 509, 619)
        createControlPanel("Control Panel", 470, 0) {

            addButton("Pattern 1") {
                hopfield.neuronGroup.neuronList.activations =
                    listOf(1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0)

            }

            addButton("Pattern 2") {
                hopfield.neuronGroup.neuronList.activations =
                    listOf(1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0)

            }

            addButton("Pattern 3") {
                hopfield.neuronGroup.neuronList.activations =
                    listOf(1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0)

            }

            addButton("Pattern 4") {
                hopfield.neuronGroup.neuronList.activations =
                    listOf(0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0)

            }

            addButton("Pattern 5") {
                hopfield.neuronGroup.neuronList.activations =
                    listOf(1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0)

            }

            addButton("Pattern 6") {
                hopfield.neuronGroup.neuronList.activations =
                    listOf(0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

            }

            addButton("Train") {
                hopfield.trainOnCurrentPattern()

            }

        }
    }

    // // Location of the projection in the desktop
    // val projectionPlot = addProjectionPlot2("Activations")
    // withGui {
    //     place(projectionPlot) {
    //         location = point(667, 10)
    //         width = 400
    //         height = 400
    //     }
    // }
    //
    // // Couple the network to the projection plot
    // with(couplingManager) {
    //     hopfield.neuronGroup couple projectionPlot
    // }

}