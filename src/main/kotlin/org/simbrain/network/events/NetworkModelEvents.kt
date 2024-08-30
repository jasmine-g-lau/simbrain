package org.simbrain.network.events

import org.simbrain.network.core.NetworkModel
import org.simbrain.util.Events

/**
 * See [Events].
 */
open class NetworkModelEvents: Events() {

    val selected = OneArgEvent<NetworkModel>()
    var deleted = OneArgEvent<NetworkModel>()
    var labelChanged = ChangedEvent<String?>()
    var clampChanged = NoArgEvent()
    var updated = NoArgEvent()
    val updateGraphics = NoArgEvent(interval = 10, timingMode = TimingMode.Throttle)

}