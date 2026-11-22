package com.ekhtiari.smsorganizer.ui.compose

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ekhtiari.smsorganizer.R
import com.ekhtiari.smsorganizer.SmsOrganizerApp
import com.ekhtiari.smsorganizer.domain.SimInfo
import com.ekhtiari.smsorganizer.ui.components.AvatarCircle
import com.ekhtiari.smsorganizer.ui.components.rememberUiHaptics
import com.ekhtiari.smsorganizer.ui.components.filledFieldColors
import com.ekhtiari.smsorganizer.ui.theme.LocalReducedMotion
import com.ekhtiari.smsorganizer.ui.theme.SoftMotion
import com.ekhtiari.smsorganizer.ui.theme.withMessageFont
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

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(viewModel.sent) {
        if (viewModel.sent) onSent()
    }

    ComposeContent(
        address = address,
        onAddressChange = { address = it },
        body = body,
        onBodyChange = { body = it },
        sims = viewModel.sims,
        selectedSimId = viewModel.selectedSimId,
        onSelectSim = viewModel::selectSim,
        error = viewModel.error,
        onBack = onBack,
        onSend = { viewModel.send(address, body) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ComposeContent(
    address: String,
    onAddressChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    sims: List<SimInfo>,
    selectedSimId: Int?,
    onSelectSim: (Int) -> Unit,
    error: String?,
    onBack: () -> Unit,
    onSend: () -> Unit,
) {
    val haptics = rememberUiHaptics()
    val reduce = LocalReducedMotion.current
    val canSend = address.isNotBlank() && body.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.new_message),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        shapes = IconButtonShapes(
                            shape = MaterialTheme.shapes.extraLarge,
                            pressedShape = MaterialTheme.shapes.small,
                        ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .animateContentSize(animationSpec = SoftMotion.spatial(reduce)),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AvatarCircle(
                        name = address.ifBlank { "?" },
                        size = 52.dp,
                    )
                    TextField(
                        value = address,
                        onValueChange = onAddressChange,
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.to_recipient)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Person, contentDescription = null)
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Ltr,
                        ),
                        colors = filledFieldColors(),
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                }

                AnimatedVisibility(
                    visible = sims.size > 1,
                    enter = SoftMotion.fadeSlideUp(reduce),
                    exit = SoftMotion.fadeSlideDown(reduce),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.choose_sim),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                        )
                        ButtonGroup(
                            overflowIndicator = { menuState ->
                                ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                            },
                        ) {
                            sims.forEach { sim ->
                                toggleableItem(
                                    checked = selectedSimId == sim.subscriptionId,
                                    label = sim.label,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            haptics.tick()
                                            onSelectSim(sim.subscriptionId)
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Outlined.SimCard,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                TextField(
                    value = body,
                    onValueChange = onBodyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text(stringResource(R.string.type_message)) },
                    textStyle = MaterialTheme.typography.bodyLarge.withMessageFont().copy(
                        textDirection = TextDirection.Content,
                    ),
                    colors = filledFieldColors(),
                    shape = MaterialTheme.shapes.extraLargeIncreased,
                )

                AnimatedVisibility(
                    visible = error != null,
                    enter = SoftMotion.fadeSlideUp(reduce),
                    exit = SoftMotion.fadeSlideDown(reduce),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Surface(color = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = {
                        haptics.confirm()
                        onSend()
                    },
                    shapes = ButtonDefaults.shapesFor(ButtonDefaults.LargeContainerHeight),
                    enabled = canSend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(min = ButtonDefaults.LargeContainerHeight),
                    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.LargeContainerHeight),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.send),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                    )
                }
            }
        }
    }
}
