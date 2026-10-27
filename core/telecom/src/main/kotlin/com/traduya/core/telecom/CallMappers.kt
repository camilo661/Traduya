package com.traduya.core.telecom

import android.os.Build
import android.telecom.Call
import com.traduya.core.model.ActiveCall
import com.traduya.core.model.CallDirection
import com.traduya.core.model.CallStatus

internal val Call.currentState: Int
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        details.state
    } else {
        @Suppress("DEPRECATION")
        state
    }

internal fun Call.stableId(): String = "${details.creationTimeMillis}-${System.identityHashCode(this)}"

internal fun Call.toActiveCall(isMuted: Boolean, isSpeakerOn: Boolean): ActiveCall {
    val details = details
    val displayName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        details.contactDisplayName ?: details.callerDisplayName
    } else {
        details.callerDisplayName
    }
    return ActiveCall(
        id = stableId(),
        number = details.handle?.schemeSpecificPart.orEmpty(),
        displayName = displayName,
        direction = if (details.callDirection == Call.Details.DIRECTION_INCOMING) {
            CallDirection.INCOMING
        } else {
            CallDirection.OUTGOING
        },
        status = currentState.toCallStatus(),
        connectedAtMillis = details.connectTimeMillis.takeIf { it > 0 },
        isMuted = isMuted,
        isSpeakerOn = isSpeakerOn,
    )
}

internal fun Int.toCallStatus(): CallStatus = when (this) {
    Call.STATE_RINGING -> CallStatus.RINGING
    Call.STATE_ACTIVE -> CallStatus.ACTIVE
    Call.STATE_HOLDING -> CallStatus.HOLDING
    Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> CallStatus.DISCONNECTED
    else -> CallStatus.DIALING
}
