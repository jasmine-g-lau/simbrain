package org.simbrain.util.propertyeditor

import org.simbrain.util.LabelledItemPanel
import org.simbrain.util.UserParameter
import org.simbrain.util.invokeLegacySetter
import smile.math.matrix.Matrix
import java.awt.Color
import javax.swing.JPanel
import javax.swing.JTabbedPane
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

/**
 * A panel that takes a set of objects either annotated with [UserParameter] or set to [GuiEditable] objects, and
 * produces a set of appropriate widgets for editing each object in a provided list. Each annotated field is
 * represented by an appropriate [ParameterWidget] class.
 *
 * When edited objects have different values a null value "..." is shown.  Null values are ignored when the panel is
 * closed, and any values in the panel are written to it.
 *
 * Note that annotated interfaces must be in Kotlin.
 *
 * Object types are created by copying a prototype object. Thus object types must implement [CopyableObject].
 *
 * @param O the type of the edited objects. Example: Neuron when editing a list of neurons.
 *
 * @author Yulin Li
 * @author Jeff Yoshimi
 * @author Oliver Coleman
 */
class AnnotatedPropertyEditor<O : EditableObject>(val editingObjects: List<O>) : JPanel() {

    constructor(vararg editingObjects: O) : this(editingObjects.toList())

    val parameterWidgetMap = editingObjects.first().let { obj ->


        val delegatedPropertyNames = HashSet<String>()

        /**
         * Properties using [GuiEditable].
         *
         * Ex:
         * ```
         * var testString by GuiEditable(initValue = "test")
         * ````
         */
        val delegated = (obj::class.allSuperclasses + obj::class)
            .map { it.declaredMemberProperties }
            .flatten()
            .filterIsInstance<KMutableProperty1<O, *>>()
            .onEach { it.isAccessible = true }
            .mapNotNull { property ->
                property.getDelegate(obj)?.also { property.get(obj) }
            }
            .filterIsInstance<GuiEditable<O, *>>()
            .onEach { delegatedPropertyNames.add(it.property.name) }

        /**
         * Properties using [UserParameter] annotations
         *
         * Ex:
         * ```
         * @UserParameter()
         * var testString = initValue
         * ```
         */
        val annotated = (obj::class.allSuperclasses + obj::class)
            .map { it.declaredMemberProperties }
            .flatten()
            .filterNot {
                // If a property has both a UserParameter annotation and a GuiEditable delegation, only keep the delegation
                delegatedPropertyNames.contains(it.name)
            }
            .mapNotNull {
                (it.annotations
                    .filterIsInstance<UserParameter>()
                    .firstOrNull()
                    ?.toGuiEditable(obj, it)
                    ?.also { up -> up.getValue(obj, it) })
            }

        (delegated + annotated)
            .associateWith { parameter ->
                makeWidget(
                    parameter,
                    isConsistent = if (parameter.value is CopyableObject) {
                        editingObjects.map { eo -> parameter.property.getter.call(eo)!!::class }.toSet().size == 1
                    } else {
                        editingObjects
                            .map { eo -> parameter.property.getter.call(eo) }
                            .map { value ->
                                when (value) {
                                    is IntArray -> value.contentHashCode()
                                    is DoubleArray -> value.contentHashCode()
                                    else -> value.hashCode()
                                }
                            }.toSet().size == 1
                    }
                )
            }

    }

    val propertyNameWidgetMap = parameterWidgetMap.map { (parameter, widget) ->
        parameter.property.name to widget
    }.toMap()

    val labelledItemPanelsByTab = LinkedHashMap<String?, LabelledItemPanel>()

    val defaultLabelledItemPanel: LabelledItemPanel
        get() = getLabelledItemPanel(null)

    private fun getLabelledItemPanel(tab: String?): LabelledItemPanel {
        return labelledItemPanelsByTab.getOrPut(if (tab.isNullOrEmpty()) "Main" else tab) {
            LabelledItemPanel()
        }
    }

    val parameterJLabels = parameterWidgetMap.entries
        .sortedBy { (parameter) -> parameter.order }
        .mapNotNull { (parameter, widget) ->
            // object widgets span the dialog and don’t use labels
            if (widget is ObjectWidget<*, *>) {
                getLabelledItemPanel(parameter.tab).addItem(widget.component)
                widget.events.valueChanged.on {
                    parameterWidgetMap.forEach { (_, w) -> w.refresh(widget.parameter.property) }
                }
                null
            } else {
                val label = getLabelledItemPanel(parameter.tab).addItem(parameter.label, widget.component)
                label.toolTipText = parameter.description
                widget.events.valueChanged.on {
                    parameterWidgetMap.forEach { (_, w) -> w.refresh(widget.parameter.property) }
                }
                parameter to label
            }
        }.toMap()

    val mainPanel = if (labelledItemPanelsByTab.size == 1) {
        labelledItemPanelsByTab.values.first()
    } else {
        JTabbedPane().also {

            val tabs = editingObjects.first().let {
                it::class.allSuperclasses + it::class
            }.firstNotNullOfOrNull { it.findAnnotation<APETabOder>() }?.tabs?.toList() ?: listOf()

            tabs.forEach { tab ->
                labelledItemPanelsByTab[tab]?.let { panel ->
                    it.addTab(tab, panel)
                }
            }

            labelledItemPanelsByTab.forEach { (tab, panel) ->
                if (tab !in tabs) {
                    it.addTab(tab, panel)
                }
            }
        }
    }.also { add(it) }

    /**
     * Returns a string describing what object or objects are being edited
     */
    val titleString: String
        get() {
            return if (editingObjects.size == 1) {
                "Edit ${editingObjects[0].name}"
            } else {
                "Edit ${editingObjects.size} ${editingObjects[0].javaClass.simpleName}s"
            }
        }

    fun <T> makeWidget(userParameter: GuiEditable<O, T>, isConsistent: Boolean): ParameterWidget<O, T> {
        if (userParameter.displayOnly) {
            return DisplayOnlyWidget(
                this@AnnotatedPropertyEditor,
                userParameter,
                isConsistent
            )
        }

        return when (userParameter.value) {

            is String, is String? -> StringWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, String?>,
                isConsistent
            ) as ParameterWidget<O, T>

            is Boolean -> BooleanWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, Boolean>,
                isConsistent
            ) as ParameterWidget<O, T>

            is Int, is Short, is Long, is Double, is Float -> NumericWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, *>,
                isConsistent
            ) as ParameterWidget<O, T>

            is Color -> ColorWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, Color>,
                isConsistent
            ) as ParameterWidget<O, T>

            is Enum<*> -> EnumWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, Enum<*>>,
                isConsistent
            ) as ParameterWidget<O, T>

            is DoubleArray -> DoubleArrayWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, DoubleArray>,
                isConsistent
            ) as ParameterWidget<O, T>

            is IntArray -> IntArrayWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, IntArray>,
                isConsistent
            ) as ParameterWidget<O, T>

            is BooleanArray -> BooleanArrayWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, BooleanArray>,
                isConsistent
            ) as ParameterWidget<O, T>

            is Array<*> -> StringArrayWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, Array<String>>,
                isConsistent
            ) as ParameterWidget<O, T>

            is Matrix -> MatrixWidget(
                this@AnnotatedPropertyEditor,
                userParameter as GuiEditable<O, Matrix>,
                isConsistent
            ) as ParameterWidget<O, T>

            is CopyableObject -> ObjectWidget(
                this@AnnotatedPropertyEditor,
                editingObjects.map { eo -> userParameter.property.get(eo) as CopyableObject },
                userParameter as GuiEditable<O, CopyableObject>,
                isConsistent
            ) as ParameterWidget<O, T>

            else -> throw IllegalArgumentException("Unsupported type: ${userParameter.property.returnType.jvmErasure.simpleName}")
        }
    }

    fun commitChanges() {
        parameterWidgetMap.forEach { (parameter, widget) ->
            editingObjects.forEach { eo ->
                if (widget.isConsistent) {
                    (parameter.property as? KMutableProperty1<O, Any>)?.let { property ->
                        if (widget is ObjectWidget<*, *>) {
                            widget.objectTypeEditor.commitChanges()
                            if (parameter.useLegacySetter) {
                                property.invokeLegacySetter(eo, widget.value.copy())
                            } else {
                                property.setter.call(eo, widget.value.copy())
                            }
                        } else {
                            if (parameter.useLegacySetter) {
                                property.invokeLegacySetter(eo, widget.value)
                            } else {
                                property.setter.call(eo, widget.value)
                            }
                        }
                        eo.onCommit()
                    }
                }

            }
        }
    }

    fun refreshValues() {
        parameterWidgetMap.map { (_, widget) -> parameterWidgetMap.forEach { (_, w) -> w.refresh(widget.parameter.property) } }
    }

    init {
        refreshValues()
    }

    private fun getWidgetByLabel(label: String): ParameterWidget<O, *> {
        return propertyNameWidgetMap.values.first { it.isConsistent && it.parameter.label == label }
    }

    fun getWidgetValueByLabel(label: String): Any? {
        return getWidgetByLabel(label).value
    }

    fun getWidgetEventsByLabel(label: String): ParameterEvents<*, *> {
        return getWidgetByLabel(label).events
    }

}

class APEObjectWrapper<O : EditableObject>(val label: String, obj: O, showLabeledBorder: Boolean = true)
    : EditableObject {
    var editingObject: O by GuiEditable(
        initValue = obj,
        label = label,
        showLabeledBorder = showLabeledBorder,
    )
    override val name: String
        get() = label
}

/**
 * Wraps an [EditableObject] so that it can be used in an [org.simbrain.util.propertyeditor.AnnotatedPropertyEditor]
 * so that we can edit the object itself with a dropdown.
 */
@JvmOverloads
fun <O : EditableObject> objectWrapper(label: String, obj: O, showLabeledBorder: Boolean = true) =
    APEObjectWrapper(label, obj, showLabeledBorder)

/**
 * Returns the widget associated with the object being edited.
 * Example: the [ObjectWidget] used to select a probability distribution.
 */
val <O : EditableObject> AnnotatedPropertyEditor<APEObjectWrapper<O>>.wrapperWidget get() = parameterWidgetMap.values.first()

/**
 * Returns the value of the widget associated with the edited object.
 * Example: the probability distribution currently selected by [ObjectWidget]
 */
val <O : EditableObject> AnnotatedPropertyEditor<APEObjectWrapper<O>>.wrapperWidgetValue get() = wrapperWidget.value as O

/**
 * Currently used with [ObjectWidget] to customize the name in the dropdown.
 *
 * Use [EditableObject.name] if object based name is possible. Use this if class based access is required.
 */
annotation class CustomTypeName(val name: String)
