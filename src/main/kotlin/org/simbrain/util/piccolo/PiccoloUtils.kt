package org.simbrain.util.piccolo

import org.piccolo2d.PCamera
import org.piccolo2d.PNode
import org.piccolo2d.activities.PTransformActivity
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PAffineTransform
import org.piccolo2d.util.PBounds
import org.simbrain.network.gui.nodes.ScreenElement
import java.awt.BasicStroke
import java.awt.Color
import java.awt.event.MouseEvent
import java.awt.geom.Rectangle2D
import kotlin.math.max

val PNode.parents
    get() = generateSequence(parent) { it.parent }

val PNode.screenElements
    get() = generateSequence(this) { it.parent }.filterIsInstance<ScreenElement>()

val PNode.firstScreenElement
    get() = screenElements.firstOrNull()

val PNode?.hasScreenElement
    get() = this?.parents?.any { it is ScreenElement } ?: false

val PInputEvent.isDoubleClick
    get() = clickCount == 2 && button == MouseEvent.BUTTON1

fun Collection<PNode>.unionOfGlobalFullBounds() = map { it.globalFullBounds }.fold(PBounds()) { acc, b -> acc.add(b); acc }

/**
 * Add a black border around a PImage. Must be called after the image's bounds have been set.
 */
fun PImage.addBorder(strokeWidth: Float = 1f): PNode {
    val (x, y, w, h) = bounds
    val box = PPath.createRectangle(x, y, w, h)
    box.strokePaint = Color.BLACK
    box.stroke = BasicStroke(strokeWidth)
    box.paint = null
    addChild(box)
    return box
}

operator fun PBounds.component1() = x
operator fun PBounds.component2() = y
operator fun PBounds.component3() = width
operator fun PBounds.component4() = height

/**
 * Copied from [PCamera.animateViewToCenterBounds]. See also [PCamera.setViewBounds].
 */
fun PCamera.setViewBoundsNoOverflow(centerBounds: Rectangle2D): PTransformActivity? {
    val delta = viewBounds.deltaRequiredToCenter(centerBounds)
    val newTransform: PAffineTransform = viewTransform
    newTransform.translate(delta.width, delta.height)
    val s = max(
        viewBounds.getWidth() / centerBounds.width,
        viewBounds.getHeight() / centerBounds.height
    )
    if (s != Double.POSITIVE_INFINITY && s != 0.0) {
        newTransform.scaleAboutPoint(s, centerBounds.centerX, centerBounds.centerY)
    }
    return animateViewToTransform(newTransform, 0)
}