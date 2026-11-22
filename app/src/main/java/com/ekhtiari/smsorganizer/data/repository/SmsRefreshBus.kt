package com.ekhtiari.smsorganizer.data.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Manual reload signal for the SMS list.
 *
 * Some devices (Samsung in particular) delete rows from the telephony provider
 * without notifying a [android.database.ContentObserver]. Collectors merge this
 * bus with the observer so a successful delete still re-queries.
 */
class SmsRefreshBus {
    private val signalsInternal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val signals: SharedFlow<Unit> = signalsInternal.asSharedFlow()

    fun notifyChanged() {
        signalsInternal.tryEmit(Unit)
    }
}
