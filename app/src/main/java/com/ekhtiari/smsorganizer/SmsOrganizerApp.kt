package com.ekhtiari.smsorganizer

import android.app.Application
import com.ekhtiari.smsorganizer.data.repository.SmsRepository
import com.ekhtiari.smsorganizer.sms.NotificationHelper
import com.ekhtiari.smsorganizer.sms.SimManager
import com.ekhtiari.smsorganizer.sms.SmsSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsOrganizerApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var smsRepository: SmsRepository
        private set
    lateinit var simManager: SimManager
        private set
    lateinit var smsSender: SmsSender
        private set
    lateinit var notificationHelper: NotificationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        smsRepository = SmsRepository(this)
        simManager = SimManager(this)
        smsSender = SmsSender(this, simManager)
        notificationHelper = NotificationHelper(this)
        notificationHelper.ensureChannels()

        appScope.launch(Dispatchers.IO) {
            runCatching { smsRepository.classifyMissingMessages() }
        }
    }
}
