package com.ekhtiari.smsorganizer

import android.app.Application
import com.ekhtiari.smsorganizer.data.repository.SmsRepository
import com.ekhtiari.smsorganizer.sms.NotificationHelper
import com.ekhtiari.smsorganizer.sms.SimManager
import com.ekhtiari.smsorganizer.sms.SmsSender
import com.ekhtiari.smsorganizer.update.AppUpdateNotifier
import com.ekhtiari.smsorganizer.update.GithubReleaseClient
import com.ekhtiari.smsorganizer.update.SharedPrefsUpdateStore
import com.ekhtiari.smsorganizer.update.UpdateChecker
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
    lateinit var updateChecker: UpdateChecker
        private set

    override fun onCreate() {
        super.onCreate()
        smsRepository = SmsRepository(this)
        simManager = SimManager(this)
        smsSender = SmsSender(this, simManager)
        notificationHelper = NotificationHelper(this)
        notificationHelper.ensureChannels()
        updateChecker = UpdateChecker(
            store = SharedPrefsUpdateStore(this),
            releases = GithubReleaseClient(),
            notifier = AppUpdateNotifier(this),
            installedVersionName = BuildConfig.VERSION_NAME,
        )

        appScope.launch(Dispatchers.IO) {
            runCatching { smsRepository.classifyMissingMessages() }
        }
        // Never block startup. GitHub may be slow or unreachable; the check fails closed.
        appScope.launch(Dispatchers.IO) {
            runCatching { updateChecker.check(force = false) }
        }
    }
}
