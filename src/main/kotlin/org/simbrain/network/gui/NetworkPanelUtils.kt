package org.simbrain.network.gui

import org.simbrain.network.gui.actions.ConditionallyEnabledAction
import org.simbrain.network.gui.actions.checkEnablingFunction
import org.simbrain.util.KeyCombination
import org.simbrain.util.createAction
import java.awt.event.ActionEvent

/**
 * [createAction] that is conditionally enabled based on the state of the network, using [ConditionallyEnabledAction.EnablingCondition]
 * with a list of keyboard shortcuts.
 */
fun NetworkPanel.createConditionallyEnabledAction(
    iconPath: String? = null,
    name: String,
    enablingCondition: ConditionallyEnabledAction.EnablingCondition,
    description: String = name,
    keyCombos: List<KeyCombination>,
    block: suspend NetworkPanel.(e: ActionEvent) -> Unit
) = this.createAction(
    iconPath = iconPath,
    name = name,
    description = description,
    keyCombos = keyCombos,
    initBlock = {
        fun updateAction() {
            isEnabled = selectionManager.checkEnablingFunction(enablingCondition)
        }
        updateAction()
        selectionManager.events.selection.on { _, _ -> updateAction() }
    },
    coroutineScope = null,
    block
)

/**
 * [createConditionallyEnabledAction] with one or no keyboard shortcut.
 */
fun NetworkPanel.createConditionallyEnabledAction(
    iconPath: String? = null,
    name: String,
    enablingCondition: ConditionallyEnabledAction.EnablingCondition,
    description: String = name,
    keyCombo: KeyCombination? = null,
    block: suspend NetworkPanel.(e: ActionEvent) -> Unit
) = this.createConditionallyEnabledAction(
    iconPath,
    name,
    enablingCondition,
    description,
    keyCombo?.let { listOf(it) } ?: listOf(),
    block
)