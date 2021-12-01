package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.initializer.HeNormal
import org.jetbrains.kotlinx.dl.api.core.initializer.HeUniform
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.Conv2D
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.ConvPadding
import org.simbrain.util.UserParameter
import org.simbrain.util.toLongArray

/**
 * Wrapper for kotlin dl convolutional 2d layer
 */
class TFConv2DLayer : TFLayer<Conv2D>() {

    @UserParameter(label = "Number of filters",  conditionalEnablingMethod = "creationMode", order = 10)
    var nfilters = 5

    @UserParameter(label = "Kernel size",  conditionalEnablingMethod = "creationMode", order = 20)
    var kernelSize = intArrayOf(3,3)

    @UserParameter(label = "Strides",  conditionalEnablingMethod = "creationMode", order = 30)
    var strides = intArrayOf(1,1,1,1)

    @UserParameter(label = "Dilations",  conditionalEnablingMethod = "creationMode", order = 40)
    var dilations = intArrayOf(1,1,1,1)

    @UserParameter(label = "Activation function",  conditionalEnablingMethod = "creationMode", order = 50)
    var activations = Activations.Relu

    @UserParameter(label = "Padding",  conditionalEnablingMethod = "creationMode", order = 80)
    var padding = ConvPadding.SAME

    @UserParameter(label = "Use bias",  conditionalEnablingMethod = "creationMode", order = 90)
    var useBias = true

    override var layer: Conv2D? = null

    override fun create() : Conv2D {
        return Conv2D(nfilters.toLong(),
            kernelSize = kernelSize.toLongArray(),
            strides = strides.toLongArray(),
            dilations = dilations.toLongArray(),
            activation = activations,
            kernelInitializer = HeNormal(),
            biasInitializer = HeUniform(),
            padding = ConvPadding.SAME,
            useBias = useBias
        ).also {
            layer = it
        }
    }

    override fun getName(): String {
        return "Convolutional 2d"
    }

    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return TFLayer.getTypes()
        }
    }

}