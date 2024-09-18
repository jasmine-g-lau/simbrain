package org.simbrain.network.gui

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PImage
import org.piccolo2d.nodes.PPath
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.scale
import java.awt.BasicStroke
import java.awt.Color
import java.awt.image.BufferedImage

class ImageBox(val width: Int, val height: Int, thickness: Float) : PNode() {

    var image: BufferedImage? = null
        set(image) {
            field = image
            pImage.image = image!!.scale(width, height)
            setBounds(0.0, 0.0, width.toDouble(), height.toDouble())
        }

    val box = PPath.createRectangle(0.0, 0.0, width.toDouble(), height.toDouble())!!
        .apply {
            stroke = BasicStroke(thickness).also { strokePaint = NetworkPreferences.weightMatrixBoundaryColor }
        }
        .also { addChild(it) }

    private val pImage = PImage().also { addChild(it) }


}