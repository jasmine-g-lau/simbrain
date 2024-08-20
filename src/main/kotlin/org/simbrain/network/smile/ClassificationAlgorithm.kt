package org.simbrain.network.smile

import org.simbrain.network.smile.classifiers.KNNClassifier
import org.simbrain.network.smile.classifiers.LogisticRegClassifier
import org.simbrain.network.smile.classifiers.SVMClassifier
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.util.Utils
import org.simbrain.util.getOneHotArray
import org.simbrain.util.propertyeditor.CopyableObject
import smile.classification.Classifier

/**
 * Superclass for wrappers of Smile classifier objects.
 */
abstract class ClassificationAlgorithm(
    val inputSize: Int,
    val outputSize: Int
): CopyableObject {

    /**
     * Main training data.
     */
    var trainingData = ClassificationDataset(inputSize, outputSize, 4)

    /**
     * Fit a model to the training data.
     */
    abstract fun fit(get2DDoubleArray: Array<DoubleArray>, intColumn: IntArray)

    /**
     * Statistics to display after training
     */
    var stats = ""

    /**
     * The main model used for classification.
     */
    abstract var model: Classifier<DoubleArray>?

    /**
     * Use the model to generate a predicted output from inputs.
     */
    abstract fun predict(input: DoubleArray): Int

    fun assertValidWinnerIndex(winner: Int) {
        if (winner > outputSize) {
            throw IllegalArgumentException("Prediction of ${winner} > output size of ${outputSize}")
        }
    }

    /**
     * Convert this model's integer prediction to an output vector.
     */
    open fun getOutputArray(winner: Int): DoubleArray {
        assertValidWinnerIndex(winner)
        return getOneHotArray(winner, outputSize)
    }

    fun setAccuracyLabel(accuracy: Double) {
        stats = "Accuracy: ${Utils.round(accuracy, 3)}"
    }

    /**
     * For use with object type editor.
     */
    abstract override fun copy(): ClassificationAlgorithm

    override fun getTypeList() = classifierTypes

}

val classifierTypes = listOf(
    LogisticRegClassifier::class.java, SVMClassifier::class.java, KNNClassifier::class.java)