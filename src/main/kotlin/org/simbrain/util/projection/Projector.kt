package org.simbrain.util.projection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.simbrain.util.UserParameter
import org.simbrain.util.createEditorDialog
import org.simbrain.util.display
import org.simbrain.util.propertyeditor.EditableObject
import java.awt.Color

class Projector(initialDimension: Int = 25) : EditableObject, CoroutineScope {

    @Transient
    private var job = SupervisorJob()

    @Transient
    override var coroutineContext = Dispatchers.Default + job

    @Transient
    var events = ProjectorEvents()

    var dimension: Int = initialDimension
        set(value) {
            dataset = Dataset(value)
            field = value
        }

    /**
     * The main data structure for a projection. A set of [DataPoint]s, each of which has two double arrays, one of
     * which ("upstairs") represents the high dimensional data, and the other of which ("downstairs") represents the
     * low dimensional data.
     */
    var dataset = Dataset(dimension)

    /**
     * The method used to project from high dimensional data upstairs to low dimensional data downstairs.
     */
    @UserParameter(label = "Projection Method", order = 100)
    var projectionMethod: ProjectionMethod = PCAProjection()
        set(value) {
            val oldMethod = field
            field = value
            initProjector()
            events.methodChanged.fire(oldMethod, value)
        }

    @UserParameter(label = "Tolerance", description = "Only add new points if they are more than this distance from any existing point", minimumValue = 0.0, increment = .1, order =  1)
    var tolerance: Double = 0.1

    @UserParameter(label = "Connect points", description = "Draw lines between points in plot", order = 10)
    var connectPoints = false
        set(value) {
            field = value
            events.settingsChanged.fire()
        }

    @UserParameter(label = "Hot color", order = 20)
    var hotColor = Color.red

    @UserParameter(label = "Base color", order = 30)
    var baseColor = Color.DARK_GRAY

    @UserParameter(label = "Show labels", description = "Show text labels sometimes associated with points", order = 40)
    var showLabels = true

    @UserParameter(label = "Use hot point", description = "If true, current point is rendered using the hotpoint color", order = 50)
    var useHotColor = true

    @UserParameter(label = "Coloring Manager", order = 110)
    var coloringManager: ColoringManager = NoOpColoringManager()

    fun addDataPoint(newPoint: DataPoint) {
        // If a different size point is added simply reset the dataset to match
        if (newPoint.upstairsPoint.size != dimension) {
            dimension = newPoint.upstairsPoint.size
        }
        synchronized(dataset) {
            val closestPoint = dataset.kdTree.findClosestPoint(newPoint)
            if (closestPoint != null && closestPoint.euclideanDistance(newPoint) < tolerance) {
                dataset.currentPoint = closestPoint
            } else {
                dataset.kdTree.insert(newPoint)
                dataset.currentPoint = newPoint
                projectionMethod.addPoint(dataset, newPoint)
                events.datasetChanged.fire()
            }
            events.pointUpdated.fire(newPoint)
        }
    }

    fun initProjector() {
        projectionMethod.init(dataset)
    }

    fun addDataPoint(array: DoubleArray) = addDataPoint(DataPoint(array))

    private fun readResolve(): Any {
        job = SupervisorJob()
        coroutineContext = Dispatchers.Default + job
        events = ProjectorEvents()
        return this
    }

    override val name = "Projector"
}

fun main() {
    val projector = Projector(4)
    projector.initProjector()
    println(projector.dataset)
    projector.addDataPoint(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0))
    projector.addDataPoint(doubleArrayOf(2.0, 3.0, 4.0, 5.0, 6.0))
    projector.addDataPoint(doubleArrayOf(3.0, 4.0, 5.0, 6.0, 7.0))
    projector.addDataPoint(doubleArrayOf(4.0, 5.0, 6.0, 7.0, 8.0))
    projector.addDataPoint(doubleArrayOf(5.0, 6.0, 7.0, 8.0, 9.0))
    println(projector.dataset)
    projector.initProjector()
    println(projector.dataset)
    projector.createEditorDialog {

    }.display()
}