package com.ekhtiari.smsorganizer

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ekhtiari.smsorganizer.ui.compose.ComposeScreen
import com.ekhtiari.smsorganizer.ui.conversation.ConversationScreen
import com.ekhtiari.smsorganizer.ui.conversation.ConversationViewModel
import com.ekhtiari.smsorganizer.ui.inbox.InboxScreen
import com.ekhtiari.smsorganizer.ui.inbox.InboxViewModel
import com.ekhtiari.smsorganizer.ui.settings.SettingsScreen
import com.ekhtiari.smsorganizer.ui.theme.SmsOrganizerTheme
import com.ekhtiari.smsorganizer.ui.theme.SoftMotion
import com.ekhtiari.smsorganizer.ui.theme.rememberReducedMotion
import com.ekhtiari.smsorganizer.ui.update.UpdatePrompt
import com.ekhtiari.smsorganizer.util.SmsPermissions

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_OTP = "otp"
        const val ACTION_COPY_OTP = "com.ekhtiari.smsorganizer.COPY_OTP"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        handleOtpIntent(intent)

        setContent {
            SmsOrganizerTheme {
                UpdatePrompt()
                val reduceMotion = rememberReducedMotion()
                val navController = rememberNavController()
                val inboxViewModel: InboxViewModel = viewModel()
                val conversationViewModel: ConversationViewModel = viewModel()
                val context = LocalContext.current

                var pendingAddress by remember { mutableStateOf(resolveSendToAddress(intent)) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) {
                    inboxViewModel.refreshStatus()
                }

                val defaultSmsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { result ->
                    inboxViewModel.refreshStatus()
                    if (result.resultCode != Activity.RESULT_OK &&
                        !SmsPermissions.isDefaultSmsApp(this@MainActivity)
                    ) {
                        inboxViewModel.markDefaultSmsRequestDenied()
                    }
                }

                fun requestPermissions() {
                    permissionLauncher.launch(SmsPermissions.requiredRuntime)
                }

                fun requestDefaultSms() {
                    // Grant runtime SMS permissions first — some OEMs deny ROLE_SMS otherwise.
                    if (!SmsPermissions.hasAll(this@MainActivity)) {
                        requestPermissions()
                        return
                    }
                    defaultSmsLauncher.launch(SmsPermissions.createDefaultSmsIntent(this@MainActivity))
                }

                fun openAppInfo() {
                    startActivity(SmsPermissions.createAppDetailsIntent(this@MainActivity))
                }

                fun openDefaultApps() {
                    startActivity(SmsPermissions.createFallbackDefaultSmsSettingsIntent(this@MainActivity))
                }

                LaunchedEffect(Unit) {
                    if (!SmsPermissions.hasAll(context)) {
                        requestPermissions()
                    }
                }

                LaunchedEffect(pendingAddress) {
                    val address = pendingAddress
                    if (!address.isNullOrBlank()) {
                        navController.navigate("compose?address=${Uri.encode(address)}")
                        pendingAddress = null
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "inbox",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    enterTransition = { SoftMotion.sharedAxisZEnter(reduceMotion) },
                    exitTransition = { SoftMotion.sharedAxisZExit(reduceMotion) },
                    popEnterTransition = { SoftMotion.sharedAxisZPopEnter(reduceMotion) },
                    popExitTransition = { SoftMotion.sharedAxisZPopExit(reduceMotion) },
                ) {
                    composable(
                        route = "inbox",
                        exitTransition = {
                            when {
                                targetState.destination.route.orEmpty().startsWith("settings") ->
                                    SoftMotion.fadeThroughExit(reduceMotion)
                                targetState.destination.route.orEmpty().startsWith("compose") ->
                                    SoftMotion.sharedAxisYExit(forward = true, reduce = reduceMotion)
                                else -> SoftMotion.sharedAxisZExit(reduceMotion)
                            }
                        },
                        popEnterTransition = {
                            when {
                                initialState.destination.route.orEmpty().startsWith("settings") ->
                                    SoftMotion.fadeThroughEnter(reduceMotion)
                                initialState.destination.route.orEmpty().startsWith("compose") ->
                                    SoftMotion.sharedAxisYEnter(forward = false, reduce = reduceMotion)
                                else -> SoftMotion.sharedAxisZPopEnter(reduceMotion)
                            }
                        },
                    ) {
                        InboxScreen(
                            viewModel = inboxViewModel,
                            onOpenThread = { threadId, address ->
                                navController.navigate(
                                    "thread/$threadId?address=${Uri.encode(address)}",
                                )
                            },
                            onCompose = { navController.navigate("compose") },
                            onSettings = { navController.navigate("settings") },
                            onRequestDefaultSms = ::requestDefaultSms,
                            onRequestPermissions = ::requestPermissions,
                            onOpenAppInfo = ::openAppInfo,
                            onOpenDefaultApps = ::openDefaultApps,
                            sharedScope = this,
                        )
                    }
                    composable(
                        route = "thread/{threadId}?address={address}",
                        arguments = listOf(
                            navArgument("threadId") { type = NavType.LongType },
                            navArgument("address") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                        ),
                        enterTransition = { SoftMotion.sharedAxisZEnter(reduceMotion) },
                        popExitTransition = { SoftMotion.sharedAxisZPopExit(reduceMotion) },
                    ) { entry ->
                        val threadId = entry.arguments?.getLong("threadId") ?: 0L
                        val address = entry.arguments?.getString("address").orEmpty()
                        ConversationScreen(
                            viewModel = conversationViewModel,
                            threadId = threadId,
                            address = Uri.decode(address),
                            onBack = { navController.popBackStack() },
                            onRequestDefaultSms = ::requestDefaultSms,
                            sharedScope = this,
                        )
                    }
                    composable(
                        route = "compose?address={address}",
                        arguments = listOf(
                            navArgument("address") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                        ),
                        enterTransition = { SoftMotion.sharedAxisYEnter(forward = true, reduce = reduceMotion) },
                        exitTransition = { SoftMotion.sharedAxisYExit(forward = true, reduce = reduceMotion) },
                        popEnterTransition = { SoftMotion.sharedAxisYEnter(forward = false, reduce = reduceMotion) },
                        popExitTransition = { SoftMotion.sharedAxisYExit(forward = false, reduce = reduceMotion) },
                    ) { entry ->
                        val address = Uri.decode(entry.arguments?.getString("address").orEmpty())
                        ComposeScreen(
                            initialAddress = address,
                            onBack = { navController.popBackStack() },
                            onSent = {
                                navController.popBackStack()
                                inboxViewModel.refreshStatus()
                            },
                        )
                    }
                    composable(
                        route = "settings",
                        enterTransition = { SoftMotion.fadeThroughEnter(reduceMotion) },
                        exitTransition = { SoftMotion.fadeThroughExit(reduceMotion) },
                        popEnterTransition = { SoftMotion.fadeThroughEnter(reduceMotion) },
                        popExitTransition = { SoftMotion.fadeThroughExit(reduceMotion) },
                    ) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onRequestDefaultSms = ::requestDefaultSms,
                            onRequestPermissions = ::requestPermissions,
                            onOpenAppInfo = ::openAppInfo,
                            onOpenDefaultApps = ::openDefaultApps,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOtpIntent(intent)
    }

    private fun handleOtpIntent(intent: Intent?) {
        if (intent?.action == ACTION_COPY_OTP) {
            val otp = intent.getStringExtra(EXTRA_OTP) ?: return
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("OTP", otp))
            Toast.makeText(this, R.string.otp_copied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolveSendToAddress(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.action == Intent.ACTION_SENDTO || intent.action == Intent.ACTION_VIEW) {
            return intent.data?.schemeSpecificPart?.substringBefore('?')
        }
        return intent.getStringExtra(EXTRA_ADDRESS)
    }
}
