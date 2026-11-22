package com.ekhtiari.smsorganizer.ui.compose

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.SmsOrganizerApp
import com.ekhtiari.smsorganizer.domain.SimInfo
import com.ekhtiari.smsorganizer.ui.theme.SoftMotion
import kotlinx.coroutines.launch

class ComposeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SmsOrganizerApp
    var sims by mutableStateOf(emptyList<SimInfo>())
        private set
    var selectedSimId by mutableStateOf<Int?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var sent by mutableStateOf(false)
        private set

    fun refresh() {
        sims = app.simManager.getActiveSims()
        selectedSimId = sims.firstOrNull { it.isDefaultSms }?.subscriptionId
            ?: sims.firstOrNull()?.subscriptionId
    }

    fun selectSim(id: Int) {
        selectedSimId = id
    }

    fun send(address: String, body: String) {
        viewModelScope.launch {
            val ok = app.smsSender.sendText(address.trim(), body.trim(), selectedSimId)
            if (ok) sent = true
            else error = "Failed to send. Set this app as default SMS and grant permissions."
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    initialAddress: String = "",
    onBack: () -> Unit,
    onSent: () -> Unit,
    viewModel: ComposeViewModel = viewModel(),
) {
    var address by remember { mutableStateOf(initialAddress) }
    var body by remember { mutableStateOf("") }
    val canSend = address.isNotBlank() && body.isNotBlank()
    val sendScale by animateFloatAsState(
        targetValue = if (canSend) 1f else 0.9f,
        animationSpec = SoftMotion.tweenSoft(SoftMotion.FastMs),
        label = "composeSendScale",
    )

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(viewModel.sent) {
        if (viewModel.sent) onSent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_message)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.send(address, body) },
                        enabled = canSend,
                        modifier = Modifier.scale(sendScale),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send),
                            tint = if (canSend) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp)
                .animateContentSize(animationSpec = SoftMotion.tweenSoft()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("To") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(16.dp),
            )

            AnimatedVisibility(
                visible = viewModel.sims.size > 1,
                enter = SoftMotion.fadeSlideUp(),
                exit = SoftMotion.fadeSlideDown(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.choose_sim),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        viewModel.sims.forEach { sim ->
                            FilterChip(
                                selected = viewModel.selectedSimId == sim.subscriptionId,
                                onClick = { viewModel.selectSim(sim.subscriptionId) },
                                label = { Text(sim.label) },
                            )
                        }
                    }
                }
            }

            TextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text(stringResource(R.string.type_message)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(16.dp),
            )

            AnimatedVisibility(
                visible = viewModel.error != null,
                enter = SoftMotion.fadeSlideUp(),
                exit = SoftMotion.fadeSlideDown(),
            ) {
                Text(
                    text = viewModel.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
