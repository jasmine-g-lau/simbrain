package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.AbstractNeuronCollection
import org.simbrain.network.core.Connector
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.ImageBox
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.WeightMatrixArrow
import org.simbrain.network.gui.createCouplingMenu
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.SimbrainTablePanel
import org.simbrain.util.table.addSimpleDefaults
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.*
import java.util.function.Consumer
import javax.swing.*

/**
 * A visual representation of a weight matrix
 */
class WeightMatrixNode(networkPanel: NetworkPanel, val weightMatrix: Connector) : ScreenElement(networkPanel), PropertyChangeListener {
    /**
     * Width of the [imageBox]
     */
    private val imageWidth = 90

    /**
     * Height of the [imageBox]
     */
    private val imageHeight = 90

    /**
     * A box around the [imageBox]
     */
    val imageBox = ImageBox(imageWidth, imageHeight, 4f)

    private val arrow = WeightMatrixArrow(this)

    private val interactionBox = WeightMatrixInteractionBox()

    init {
        updateShowWeights()
        pickable = true
        val events = weightMatrix.events
        events.updated.on { events.updateGraphics.fire() }
        events.updateGraphics.on(Dispatchers.Swing) { renderMatrixToImage() }
        events.labelChanged.on(Dispatchers.Swing) { _, newLabel -> interactionBox.setText(newLabel) }
        weightMatrix.source.events.locationChanged.on(Dispatchers.Swing) {
            arrow.invalidateFullBounds()
            updateInteractionBoxLocation()
        }
        weightMatrix.target.events.locationChanged.on(Dispatchers.Swing) {
            arrow.invalidateFullBounds()
            updateInteractionBoxLocation()
        }
        invalidateFullBounds()
        weightMatrix.events.showWeightsChanged.on { updateShowWeights() }
        interactionBox.setText(weightMatrix.displayName)
        addPropertyChangeListener(PROPERTY_FULL_BOUNDS, this)
    }

    private fun updateInteractionBoxLocation() {
        val (x, y) = ((weightMatrix.target.location - weightMatrix.source.location) / 2) + weightMatrix.source.location
        interactionBox.centerFullBoundsOnPoint(x, y)
    }

    /**
     * Render the weight matrix to the [.imageBox].
     */
    private fun renderMatrixToImage() {
        val pixelArray = (weightMatrix as WeightMatrix).weights
        val img = pixelArray.toSimbrainColorImage(
            weightMatrix.weightMatrix.ncol(),
            weightMatrix.weightMatrix.nrow()
        )
        imageBox.image = img
    }

    private fun updateShowWeights() {
        networkPanel.selectionManager.remove(this)
        if (weightMatrix.isShowWeights) {
            arrow.invalidateFullBounds()
            removeChild(interactionBox)
            addChild(arrow)
            addChild(imageBox)
            renderMatrixToImage()
            setBounds(imageBox.bounds)
        } else {
            updateInteractionBoxLocation()
            interactionBox.invalidateFullBounds()
            removeChild(arrow)
            removeChild(imageBox)
            addChild(interactionBox)
            setBounds(interactionBox.bounds)
        }
    }

    override fun paint(paintContext: PPaintContext) {
        paintContext.graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )
        super.paint(paintContext)
    }

    override val isDraggable: Boolean = false

    override val toolTipText: String
        get() = weightMatrix.toString()

    override val contextMenu: JPopupMenu
        get() {
            val contextMenu = JPopupMenu()
            contextMenu.add(networkPanel.networkActions.cutAction)
            contextMenu.add(networkPanel.networkActions.copyAction)
            contextMenu.add(networkPanel.networkActions.pasteAction)
            contextMenu.addSeparator()

            // Edit Submenu
            val editArray: Action = object : AbstractAction("Edit...") {
                override fun actionPerformed(event: ActionEvent) {
                    val dialog: StandardDialog = matrixDialog
                    dialog.setVisible(true)
                }
            }
            contextMenu.add(editArray)
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()
            val randomizeAction: Action = networkPanel.networkActions.randomizeObjectsAction
            contextMenu.add(randomizeAction)
            val diagAction: Action = object : AbstractAction("Diagonalize") {
                init {
                    // putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/"));
                    putValue(SHORT_DESCRIPTION, "Diagonalize array")
                }

                override fun actionPerformed(event: ActionEvent) {
                    networkPanel.selectionManager
                        .filterSelectedModels(WeightMatrix::class.java)
                        .forEach(Consumer { obj: WeightMatrix -> obj.diagonalize() })
                }
            }
            contextMenu.add(diagAction)
            contextMenu.addSeparator()
            if (weightMatrix is WeightMatrix) {
                contextMenu.add(
                    actionManager
                        .createCoupledPlotMenu(
                            (weightMatrix).getProducer(WeightMatrix::weights),
                            Objects.requireNonNull<String>(weightMatrix.id),
                            "Plot Weight Matrix"
                        )
                )
                contextMenu.add(
                    networkPanel.createAction(
                        name = "Set Spectral Radius...",
                        description = "Rescale matrix so that max eigenvalue is the specified value. < .9 decays; .9" +
                                " churns; > 1 explodes."
                    ) {
                        val radius =
                            showNumericInputDialog("Set spectral Radius:", weightMatrix.weightMatrix.maxEigenvalue())
                        if (radius != null) {
                            weightMatrix.weightMatrix.setSpectralRadius(radius)
                            weightMatrix.events.updated.fire()
                        }
                    }
                )
            }

            if (model.source is AbstractNeuronCollection) {
                contextMenu.addSeparator()

                contextMenu.add(networkPanel.createAction(name = "Toggle Show Weights") {
                    weightMatrix.isShowWeights = !weightMatrix.isShowWeights
                })
            }

            // Coupling menu
            contextMenu.addSeparator()
            val couplingMenu: JMenu = networkPanel.networkComponent.createCouplingMenu(weightMatrix)
            contextMenu.add(couplingMenu)

            return contextMenu
        }

    /**
     * Returns the dialog for editing this weight matrix
     */
    private val matrixDialog: StandardDialog
        get() {
            val dialog = StandardDialog()
            dialog.setTitle("Edit Weight Matrix")
            val tabs = JTabbedPane()

            // Property Editor
            val ape: AnnotatedPropertyEditor<*> = AnnotatedPropertyEditor(weightMatrix)
            tabs.addTab("Properties", ape)
            dialog.addCommitTask { ape.commitChanges() }

            // Weight matrix
            if (weightMatrix is WeightMatrix) {
                val wm = MatrixDataFrame(weightMatrix.weightMatrix)
                val wmViewer = SimbrainTablePanel(wm, false)
                wmViewer.addSimpleDefaults()
                tabs.addTab("Weight Matrix", wmViewer)
                weightMatrix.events.updated.on { wmViewer.model.fireTableDataChanged() }
                dialog.addCommitTask {
                    weightMatrix.setWeights(wm.get2DDoubleArray())
                    weightMatrix.events.updated.fire()
                }
            }
            dialog.setContentPane(tabs)
            dialog.pack()
            dialog.setLocationRelativeTo(null)
            return dialog
        }

    /**
     * Without this the node can't be selected.
     */
    override fun propertyChange(arg0: PropertyChangeEvent) {
        if (model.isShowWeights) {
            setBounds(imageBox.fullBounds)
        } else {
            setBounds(interactionBox.fullBounds)
        }
        invalidateFullBounds()
    }

    override val propertyDialog: StandardDialog
        get() = matrixDialog

    override val model: Connector
        get() = weightMatrix

    inner class WeightMatrixInteractionBox : InteractionBox(networkPanel) {

        override val propertyDialog: StandardDialog = this@WeightMatrixNode.propertyDialog

        override val model: Connector
            get() = weightMatrix

        override val isDraggable: Boolean = false

        override val contextMenu: JPopupMenu
            get() = this@WeightMatrixNode.contextMenu

        override val toolTipText: String
            get() = this@WeightMatrixNode.toolTipText

    }
}
