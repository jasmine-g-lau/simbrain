package org.simbrain.util.stats

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.mapper.Mapper
import org.apache.commons.math3.random.JDKRandomGenerator
import org.simbrain.util.createConstructorCallingConverter
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.stats.distributions.*
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaType

/**
 * A probability distribution. Most wrap apache commons math classes. Some are real and some integer valued. When
 * calling sampleDouble on an integer distribution it is cast to double, and similarly from integer to double.
 *
 * Note that these classes are deserialized by calling the default constructor with all parameters. See
 * [ProbabilityDistributionConverter]. So subclasses must be initialized using the primary constructor to deserialize
 * properly. Just follow the template of existing sublcasses.
 */
abstract class ProbabilityDistribution() : CopyableObject {

    /**
     * Random generator for pseudo-random sequences on which a seed can be set.
     */
    @Transient
    val randomGenerator = JDKRandomGenerator()

    /**
     * Use this to ensure two probability distributions return the same pseudo-random sequence of numbers.
     * See ProbabilityDistributionTest.kt for examples.
     *
     * If null, then seed is random and so copies of this object produce different samples.
     *
     */
    var randomSeed: Long? = null
        set(value) {
            field = value
            if (value != null) {
                randomGenerator.setSeed(value)
            }
        }

    abstract fun sampleDouble(): Double

    abstract fun sampleDouble(n: Int): DoubleArray

    abstract fun sampleInt(): Int

    abstract fun sampleInt(n: Int): IntArray

    abstract override fun copy(): ProbabilityDistribution

    abstract override val name: String

    override fun toString() = name

    override fun getTypeList() = probDistTypes

    companion object {

        fun getXStream(): XStream {
            val xstream = getSimbrainXStream()
            xstream.registerConverter(createConstructorCallingConverter(listOf(ProbabilityDistribution::class.java), xstream.mapper, xstream.reflectionProvider))
            return xstream
        }
    }

}

val probDistTypes = listOf(
    ExponentialDistribution::class.java,
    GammaDistribution::class.java, LogNormalDistribution::class.java,
    NormalDistribution::class.java, ParetoDistribution::class.java,
    UniformRealDistribution::class.java,
    PoissonDistribution::class.java,
    UniformIntegerDistribution::class.java
)

/**
 * De-serialize by explicitly the calling the primary constructor.
 */
class ProbabilityDistributionConverter(mapper: Mapper, reflectionProvider: ReflectionProvider) :
    ReflectionConverter(mapper, reflectionProvider) {

    override fun canConvert(type: Class<*>?): Boolean {
        return super.canConvert(type) && type?.superclass == ProbabilityDistribution::class.java
    }

    override fun unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): Any {
        val cls = try {
            Class.forName(reader.nodeName).kotlin
        } catch (e: ClassNotFoundException) {
            Class.forName(reader.getAttribute("class")).kotlin
        }
        val vars = sequence {
            while (reader.hasMoreChildren()) {
                reader.moveDown()
                yield(reader.nodeName to reader.value)
                reader.moveUp()
            }
        }.toMap()
        val constructor = cls.constructors.first()
        val params = constructor.parameters
        val paramValueMapping = params.map { param -> param to vars[param.name] }
        fun typeMapping(string: String?, param: KParameter): Any? {
            return when (param.type.javaType) {
                Double::class.java -> string?.toDouble()
                Int::class.java -> string?.toInt()
                else -> string
            }
        }

        val paramMap = paramValueMapping.associate { (param, value) -> param to typeMapping(value, param) }
        val probDist = constructor.callBy(paramMap) as ProbabilityDistribution
        vars["randomSeed"]?.toLong()?.let{
            probDist.randomSeed = it
        }
        return probDist
    }
}
