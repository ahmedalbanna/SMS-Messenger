package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DbMessage
import com.example.viewmodel.SmsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: SmsViewModel,
    isDefaultSms: Boolean,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val address by viewModel.selectedAddress.collectAsState()
    val contactName by viewModel.selectedContactName.collectAsState()
    val messages by viewModel.activeThreadMessages.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to latest message when a new one arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val displayName = contactName ?: address ?: "Unknown"

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding() // Keep it safe from system gesture bars
            .testTag("chat_screen"),
        topBar = {
            // Elegant Clean Minimalism Top App Bar with a subtle divider
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1C1E)
                                ),
                                maxLines = 1
                            )
                            if (address != null && displayName != address) {
                                Text(
                                    text = address!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF74777F)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to conversation list",
                                tint = Color(0xFF1A1C1E)
                            )
                        }
                    },
                    actions = {
                        // Call contact button (Direct Intent)
                        if (address != null && address!!.any { it.isDigit() }) {
                            IconButton(
                                onClick = {
                                    try {
                                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${address}"))
                                        context.startActivity(callIntent)
                                    } catch (e: Exception) {
                                        // Fallback
                                    }
                                },
                                modifier = Modifier.testTag("call_contact_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Call,
                                    contentDescription = "Call Contact",
                                    tint = Color(0xFF1A1C1E)
                                )
                            }
                        }

                        // Delete conversation button
                        IconButton(
                            onClick = {
                                if (address != null) {
                                    viewModel.deleteConversation(address!!)
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.testTag("delete_whole_thread_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Clear Entire Thread",
                                tint = Color(0xFFBA1A1A) // Clean Google Red
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFFDFBFF)
                    )
                )
                // Divider under TopAppBar to match minimalism
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE1E2E9))
                )
            }
        },
        bottomBar = {
            // Typing Input Bar Panel in F3F4F9 with borders and white input container from design HTML
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(), // Automatically adjusts when keyboard opens
                color = Color(0xFFF3F4F9),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2E9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { 
                            Text(
                                "اكتب رسالتك النصية هنا...",
                                color = Color(0xFF74777F)
                            ) 
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input_field"),
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFDFBFF),
                            unfocusedContainerColor = Color(0xFFFDFBFF),
                            focusedBorderColor = Color(0xFF0B57D0),
                            unfocusedBorderColor = Color(0xFFE1E2E9),
                            focusedTextColor = Color(0xFF1A1C1E),
                            unfocusedTextColor = Color(0xFF1A1C1E)
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FloatingActionButton(
                        onClick = {
                            if (textInput.isNotBlank() && address != null) {
                                viewModel.sendMessage(
                                    address = address!!,
                                    body = textInput,
                                    context = context,
                                    isDefaultSmsApp = isDefaultSms
                                )
                                textInput = ""
                            }
                        },
                        containerColor = Color(0xFFD3E3FD),
                        contentColor = Color(0xFF041E49),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send SMS",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFDFBFF))
        ) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sms,
                        contentDescription = "No Messages",
                        tint = Color(0xFF0B57D0).copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "بداية هذا الخيط",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1A1C1E)
                    )
                    Text(
                        text = "لا توجد أي رسائل سابقة مع $displayName. اكتب رسالة في الأسفل للتواصل.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF74777F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubbleItem(
                            message = msg,
                            onDelete = {
                                viewModel.deleteMessage(msg.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: DbMessage,
    onDelete: () -> Unit
) {
    val isSent = message.type == 2
    var showMenu by remember { mutableStateOf(false) }

    val formattedTime = remember(message.date) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.date))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_bubble_${message.id}"),
        horizontalAlignment = if (isSent) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isSent) 16.dp else 4.dp,
                        bottomEnd = if (isSent) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isSent) {
                        Color(0xFF0B57D0) // Primary Google Blue
                    } else {
                        Color(0xFFE8F0FE) // Light Blue background container
                    }
                )
                .clickable { showMenu = true }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSent) {
                        Color.White
                    } else {
                        Color(0xFF1A1C1E)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = if (isSent) {
                            Color.White.copy(alpha = 0.7f)
                        } else {
                            Color(0xFF74777F)
                        }
                    )
                    if (isSent) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Sent indicator",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // Expanded Options dropdown on Bubble Click
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("حذف الرسالة") },
                onClick = {
                    onDelete()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Message",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}
