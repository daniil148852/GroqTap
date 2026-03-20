package com.groqtap.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.groqtap.data.GroqModel
import com.groqtap.data.Prefs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: Prefs,
    onNavigateBack: () -> Unit,
    onWidgetToggle: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val apiKey by prefs.apiKey.collectAsStateWithLifecycle(initialValue = "")
    val modelId by prefs.modelId.collectAsStateWithLifecycle(initialValue = GroqModel.LLAMA_3_3_70B.id)
    val widgetEnabled by prefs.widgetEnabled.collectAsStateWithLifecycle(initialValue = false)
    val temperature by prefs.temperature.collectAsStateWithLifecycle(initialValue = 0.7f)
    val systemPrompt by prefs.systemPrompt.collectAsStateWithLifecycle(initialValue = "")

    var tempApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var keySaved by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = GroqColors.Bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GroqColors.Bg),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = GroqColors.TextSecondary)
                    }
                },
                title = { Text("Settings", color = GroqColors.TextPrimary, fontWeight = FontWeight.SemiBold) },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── API Key ──
            SettingsSection(title = "API KEY") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GroqColors.SurfaceHigh)
                                .border(1.dp, GroqColors.Border, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            if (tempApiKey.isEmpty()) {
                                Text("gsk_••••••••••••••", color = GroqColors.TextTertiary, fontSize = 14.sp)
                            }
                            BasicTextField(
                                value = tempApiKey,
                                onValueChange = { tempApiKey = it; keySaved = false },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = GroqColors.TextPrimary),
                                cursorBrush = SolidColor(GroqColors.Orange),
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        IconButton(onClick = { showApiKey = !showApiKey }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null,
                                tint = GroqColors.TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    prefs.setApiKey(tempApiKey.trim())
                                    keySaved = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GroqColors.Orange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(if (keySaved) Icons.Default.Check else Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (keySaved) "Saved!" else "Save key", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys")))
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GroqColors.TextSecondary),
                            border = BorderStroke(1.dp, GroqColors.Border),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Get key", fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── Model ──
            SettingsSection(title = "MODEL") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GroqModel.entries.forEach { model ->
                        ModelOption(
                            model = model,
                            selected = model.id == modelId,
                            onSelect = { scope.launch { prefs.setModelId(model.id) } },
                        )
                    }
                }
            }

            // ── Temperature ──
            SettingsSection(title = "TEMPERATURE") {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Creativity / randomness", color = GroqColors.TextSecondary, fontSize = 13.sp)
                        Text(
                            "%.1f".format(temperature),
                            color = GroqColors.Orange,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Slider(
                        value = temperature,
                        onValueChange = { scope.launch { prefs.setTemperature(it) } },
                        valueRange = 0f..2f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = GroqColors.Orange,
                            activeTrackColor = GroqColors.Orange,
                            inactiveTrackColor = GroqColors.Surface,
                        ),
                    )
                }
            }

            // ── Floating Widget ──
            SettingsSection(title = "FLOATING WIDGET") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show floating button", color = GroqColors.TextPrimary, fontSize = 14.sp)
                        Text("Quick access from any app", color = GroqColors.TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = widgetEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !Settings.canDrawOverlays(context)) {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}"),
                                    )
                                )
                            } else {
                                scope.launch { prefs.setWidgetEnabled(enabled) }
                                onWidgetToggle(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GroqColors.Orange,
                            uncheckedTrackColor = GroqColors.Surface,
                        ),
                    )
                }
            }

            // ── Info ──
            SettingsSection(title = "ABOUT") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoRow(Icons.Default.Bolt, "Powered by Groq", "World's fastest inference")
                    InfoRow(Icons.Default.Lock, "BYOK privacy", "Your key goes directly to Groq API")
                    InfoRow(Icons.Default.Code, "Open source", "github.com/groqtap")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            color = GroqColors.TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
        Surface(
            color = GroqColors.BgElevated,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, GroqColors.Border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                content()
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ModelOption(model: GroqModel, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect)
            .background(if (selected) GroqColors.OrangeDim else Color.Transparent)
            .border(
                1.dp,
                if (selected) GroqColors.Orange.copy(alpha = 0.4f) else GroqColors.Border,
                RoundedCornerShape(10.dp),
            )
            .padding(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(model.displayName, color = if (selected) GroqColors.OrangeLight else GroqColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚡ ${model.speed}", color = GroqColors.TextSecondary, fontSize = 11.sp)
                Text("• ${model.contextWindow} ctx", color = GroqColors.TextSecondary, fontSize = 11.sp)
            }
        }
        if (selected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(20.dp).clip(CircleShape).background(GroqColors.Orange),
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = GroqColors.Orange, modifier = Modifier.size(18.dp))
        Column {
            Text(title, color = GroqColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = GroqColors.TextSecondary, fontSize = 11.sp)
        }
    }
}
