package com.groqtap.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groqtap.data.ChatMessage
import com.groqtap.data.GroqModel
import com.groqtap.data.Role
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = GroqColors.Bg,
        topBar = {
            ChatTopBar(
                model = state.currentModel,
                isStreaming = state.isStreaming,
                onClearChat = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.clearChat()
                },
                onSettings = onNavigateToSettings,
            )
        },
        bottomBar = {
            InputBar(
                text = state.inputText,
                isStreaming = state.isStreaming,
                onTextChange = viewModel::onInputChange,
                onSend = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.onSend()
                },
                onStop = viewModel::stopStreaming,
            )
        },
        modifier = modifier,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.messages.isEmpty()) {
                EmptyState(model = state.currentModel, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = state.messages,
                        key = { it.id },
                    ) { message ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 4 },
                        ) {
                            MessageRow(
                                message = message,
                                isStreaming = state.streamingMessageId == message.id,
                            )
                        }
                    }
                }
            }

            // Error snackbar
            AnimatedVisibility(
                visible = state.error != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            ) {
                state.error?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1414)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF4A2020)),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp, 10.dp),
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(err, color = Color(0xFFFFAAAA), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = viewModel::dismissError, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = GroqColors.TextSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────── Top bar ───────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    model: GroqModel,
    isStreaming: Boolean,
    onClearChat: () -> Unit,
    onSettings: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = GroqColors.Bg,
            titleContentColor = GroqColors.TextPrimary,
        ),
        navigationIcon = {
            // GroqTap logo pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GroqColors.Orange),
                ) {
                    Text(
                        "G",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "GroqTap",
                    color = GroqColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }
        },
        title = {
            // Streaming indicator / model badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                AnimatedContent(targetState = isStreaming, label = "streaming") { streaming ->
                    if (streaming) {
                        PulsingDot()
                    } else {
                        ModelBadge(model)
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onClearChat) {
                Icon(Icons.Default.Add, "New chat", tint = GroqColors.TextSecondary)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "Settings", tint = GroqColors.TextSecondary)
            }
        },
    )
}

@Composable
private fun ModelBadge(model: GroqModel) {
    Surface(
        color = GroqColors.Surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GroqColors.Border),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(GroqColors.Orange),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                model.displayName,
                color = GroqColors.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "alpha",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .alpha(alpha)
                .size(7.dp)
                .clip(CircleShape)
                .background(GroqColors.Orange),
        )
        Spacer(Modifier.width(6.dp))
        Text("Generating…", color = GroqColors.Orange, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────── Message row ───────────────

@Composable
private fun MessageRow(message: ChatMessage, isStreaming: Boolean) {
    val isUser = message.role == Role.USER

    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        if (!isUser) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(18.dp).clip(CircleShape).background(GroqColors.Orange),
                ) {
                    Text("G", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.width(5.dp))
                Text("GroqTap", color = GroqColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isUser) 18.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp,
                    )
                )
                .background(if (isUser) GroqColors.UserBubble else GroqColors.AiBubble)
                .border(
                    1.dp,
                    if (isUser) GroqColors.UserBubbleBorder else GroqColors.AiBubbleBorder,
                    RoundedCornerShape(
                        topStart = if (isUser) 18.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp,
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (message.content.isEmpty() && isStreaming) {
                TypingIndicator()
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        message.content,
                        color = if (isUser) GroqColors.OrangeLight else GroqColors.TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isStreaming) {
                        Spacer(Modifier.width(2.dp))
                        StreamingCursor()
                    }
                }
            }
        }

        Text(
            formatTime(message.timestamp),
            color = GroqColors.TextTertiary,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun StreamingCursor() {
    val inf = rememberInfiniteTransition(label = "cursor")
    val alpha by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(530), RepeatMode.Reverse), "cursor_a")
    Box(
        modifier = Modifier
            .alpha(alpha)
            .width(2.dp)
            .height(16.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(GroqColors.Orange),
    )
}

@Composable
private fun TypingIndicator() {
    val inf = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val alpha by inf.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(600, delayMillis = index * 150),
                    RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .alpha(alpha)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(GroqColors.TextSecondary),
            )
        }
    }
}

// ─────────────── Input bar ───────────────

@Composable
private fun InputBar(
    text: String,
    isStreaming: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(
        color = GroqColors.Bg,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Divider(color = GroqColors.BorderFaint, thickness = 1.dp)
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                // Text field container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GroqColors.Surface)
                        .border(1.dp, GroqColors.Border, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    if (text.isEmpty()) {
                        Text(
                            "Message GroqTap…",
                            color = GroqColors.TextTertiary,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = GroqColors.TextPrimary),
                        cursorBrush = SolidColor(GroqColors.Orange),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { if (!isStreaming) onSend() }),
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Send / Stop button
                AnimatedContent(targetState = isStreaming, label = "send_btn") { streaming ->
                    if (streaming) {
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GroqColors.Surface),
                        ) {
                            Icon(Icons.Default.Stop, "Stop", tint = GroqColors.Orange, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        IconButton(
                            onClick = onSend,
                            enabled = text.isNotBlank(),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (text.isNotBlank()) GroqColors.Orange else GroqColors.Surface),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                "Send",
                                tint = if (text.isNotBlank()) Color.White else GroqColors.TextTertiary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────── Empty state ───────────────

@Composable
private fun EmptyState(model: GroqModel, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(GroqColors.OrangeDim, GroqColors.Bg))
                )
                .border(1.dp, GroqColors.OrangeDim, CircleShape),
        ) {
            Text(
                "G",
                color = GroqColors.Orange,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
            )
        }

        Text(
            "GroqTap",
            color = GroqColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
        )
        Text(
            "Fast AI — powered by ${model.displayName}",
            color = GroqColors.TextSecondary,
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(4.dp))

        val suggestions = listOf(
            "⚡  Explain quantum entanglement",
            "💡  Write a Python script to sort files",
            "🔍  Compare REST vs GraphQL",
        )
        suggestions.forEach { suggestion ->
            Surface(
                color = GroqColors.Surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GroqColors.Border),
            ) {
                Text(
                    suggestion,
                    color = GroqColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
    }
}

// ─────────────── Helpers ───────────────

private fun formatTime(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

@Composable
private fun Divider(color: Color, thickness: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.fillMaxWidth().height(thickness).background(color))
}
