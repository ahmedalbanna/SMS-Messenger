package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DbMessage
import com.example.viewmodel.SmsViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    viewModel: SmsViewModel,
    isDefaultSms: Boolean,
    onRequestDefaultSms: () -> Unit,
    onNavigateToChat: (address: String, name: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val conversations by viewModel.conversations.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var showNewChatDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("الكل") }

    Scaffold(
        modifier = modifier.testTag("conversation_list_screen"),
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color(0xFFFDFBFF))
                    .statusBarsPadding()
            ) {
                // Clean Minimalism App Bar with rounded border and shape from HTML design
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color(0xFFF3F4F9), RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .clickable { /* Tapping the header activates search */ }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD3E3FD)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sms,
                                    contentDescription = "Search Icon",
                                    tint = Color(0xFF041E49),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "ابحث في الرسائل",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 16.sp,
                                    color = Color(0xFF44474E)
                                )
                            )
                        }

                        // Avatar / Profile icon mimicking the design's gradient avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.syncMessages(context) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("sync_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF0B57D0)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Sync,
                                        contentDescription = "Sync Messages",
                                        tint = Color(0xFF1C1B1F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF60A5FA), Color(0xFF6366F1))
                                        )
                                    )
                            )
                        }
                    }
                }

                // Default SMS App Warning Banner
                if (!isDefaultSms) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تطبيق الرسائل الافتراضي",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "عينه كافترضي لإرسال رسائل حقيقية، أو استمر بوضع المحاكاة التفاعلية.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onRequestDefaultSms,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("set_default_sms_button")
                            ) {
                                Text(
                                    text = "تعيين",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // Interactive Category Filter Pills (Material 3 Filter Chips)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filterCategories = listOf("الكل", "شخصي", "معاملات", "عروض")
                    filterCategories.forEach { category ->
                        val isSelected = selectedCategory == category
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = category }
                                .testTag("category_pill_$category"),
                            color = if (isSelected) Color(0xFFD3E3FD) else Color.Transparent,
                            border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF74777F)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) Color(0xFF041E49) else Color(0xFF44474E),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Real-time Search Text Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_text_input"),
                    placeholder = { Text("بحث عن رقم، جهة اتصال أو محتوى...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search Icon") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear Search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3F4F9),
                        unfocusedContainerColor = Color(0xFFF3F4F9),
                        focusedBorderColor = Color(0xFF0B57D0),
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }
        },
        bottomBar = {
            // Elegant Clean Minimalism bottom nav bar directly from design HTML
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                color = Color(0xFFF3F4F9),
                border = BorderStroke(0.5.dp, Color(0xFFE1E2E9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item 1: Messages (Active)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedCategory = "الكل" }
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFD3E3FD))
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forum,
                                contentDescription = "الرسائل",
                                tint = Color(0xFF041E49),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "الرسائل",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF041E49)
                        )
                    }

                    // Item 2: Contacts
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                android.widget.Toast.makeText(context, "سجل جهات الاتصال سيتوفر قريباً!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = "جهات الاتصال",
                            tint = Color(0xFF1A1C1E).copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "جهات الاتصال",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1A1C1E).copy(alpha = 0.6f)
                        )
                    }

                    // Item 3: Settings
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                android.widget.Toast.makeText(context, "الإعدادات ستتوفر قريباً للتخصيص الكامل!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "الإعدادات",
                            tint = Color(0xFF1A1C1E).copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "الإعدادات",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1A1C1E).copy(alpha = 0.6f)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = Color(0xFFD3E3FD),
                contentColor = Color(0xFF041E49),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 8.dp, start = 8.dp)
                    .testTag("new_chat_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New Conversation",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFDFBFF))
                .padding(paddingValues)
        ) {
            val isSearching = searchQuery.isNotEmpty()
            val baseList = if (isSearching) searchResults else conversations

            // Filter baseList according to selectedCategory
            val listToShow = remember(baseList, selectedCategory) {
                when (selectedCategory) {
                    "الكل" -> baseList
                    "شخصي" -> baseList.filter { msg ->
                        val addr = msg.address.lowercase()
                        addr.startsWith("+") || (addr.all { it.isDigit() } && !addr.contains("8080") && !addr.contains("9200")) || msg.contactName?.contains("أحمد") == true || msg.contactName?.contains("الوالدة") == true
                    }
                    "معاملات" -> baseList.filter { msg ->
                        val addr = msg.address.lowercase()
                        addr.contains("aramex") || addr.contains("ahli") || msg.body.contains("البنك") || msg.body.contains("رمز") || msg.body.contains("أمان") || msg.body.contains("شحنتك") || msg.body.contains("OTP")
                    }
                    "عروض" -> baseList.filter { msg ->
                        val addr = msg.address.lowercase()
                        addr.contains("vodafone") || msg.body.contains("باقة") || msg.body.contains("عروض") || msg.body.contains("خصم") || msg.body.contains("إنترنت")
                    }
                    else -> baseList
                }
            }

            if (listToShow.isEmpty()) {
                // Empty State View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                Color(0xFFD3E3FD).copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSearching) Icons.Filled.SearchOff else Icons.Filled.Sms,
                            contentDescription = "Empty State",
                            tint = Color(0xFF0B57D0),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = if (isSearching) "لم يتم العثور على أي رسائل" else "لا توجد رسائل في هذا القسم",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1A1C1E)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isSearching) "تأكد من كتابة الكلمات بشكل صحيح أو جرب رقم هاتف آخر." else "يرجى الضغط على زر المزامنة بالأعلى لعرض رسائل النظام، أو ابدأ محادثة وهمية جديدة للتجربة التفاعلية.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF44474E),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                // Messaging List View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listToShow, key = { it.id }) { msg ->
                        ConversationItem(
                            message = msg,
                            onClick = {
                                viewModel.selectConversation(msg.address, msg.contactName)
                                onNavigateToChat(msg.address, msg.contactName)
                            },
                            onDelete = {
                                viewModel.deleteConversation(msg.address)
                            }
                        )
                    }
                }
            }
        }
    }


    // New Contact Chat Dialog
    if (showNewChatDialog) {
        var inputAddress by remember { mutableStateOf("") }
        var inputName by remember { mutableStateOf("") }
        var inputBody by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = {
                Text(
                    text = "بدء محادثة جديدة 💬",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = inputAddress,
                        onValueChange = { inputAddress = it },
                        label = { Text("رقم الهاتف أو العنوان") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_address_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("الاسم (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = inputBody,
                        onValueChange = { inputBody = it },
                        label = { Text("نص الرسالة الأولى") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("dialog_message_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputAddress.isNotBlank() && inputBody.isNotBlank()) {
                            viewModel.selectConversation(inputAddress, if (inputName.isBlank()) null else inputName)
                            viewModel.sendMessage(
                                address = inputAddress,
                                body = inputBody,
                                context = context,
                                isDefaultSmsApp = isDefaultSms
                            )
                            onNavigateToChat(inputAddress, if (inputName.isBlank()) null else inputName)
                            showNewChatDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_confirm_button")
                ) {
                    Text("إرسال وركوب الخيط")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ConversationItem(
    message: DbMessage,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val displayName = message.contactName ?: message.address
    val initial = if (displayName.isNotEmpty()) displayName.take(1).uppercase(Locale.getDefault()) else "?"
    val isUnread = !message.isRead && message.type == 1

    // Generate deterministic background and text colors based on address for visual diversity
    val (avatarBg, avatarText) = remember(message.address, isUnread) {
        if (isUnread) {
            Color(0xFF0B57D0) to Color.White
        } else {
            val palettes = listOf(
                Color(0xFFFFDBCB) to Color(0xFF3B0900),
                Color(0xFFC2EFD3) to Color(0xFF00210C),
                Color(0xFFD1E4FF) to Color(0xFF001D35),
                Color(0xFFE2D6FE) to Color(0xFF21005D),
                Color(0xFFFDE047) to Color(0xFF713F12),
                Color(0xFFF472B6) to Color(0xFF500724)
            )
            val index = Math.abs(message.address.hashCode()) % palettes.size
            palettes[index]
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("conversation_card_${message.address}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) Color(0xFFE8F0FE) else Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Circular Badge with custom Clean Minimalism theme colors
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = avatarText
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Details Block
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = Color(0xFF1A1C1E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = formatTime(message.date),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isUnread) {
                            Color(0xFF0B57D0)
                        } else {
                            Color(0xFF74777F)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Outgoing icon inside conversation preview
                    if (message.type == 2) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Sent message indication",
                            tint = Color(0xFF0B57D0),
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 2.dp)
                        )
                    }

                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isUnread) {
                            Color(0xFF1A1C1E)
                        } else {
                            Color(0xFF74777F)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF0B57D0), CircleShape)
                        )
                    } else {
                        // Action buttons
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("delete_conversation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Conversation",
                                tint = Color(0xFF74777F).copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val smsDate = Calendar.getInstance().apply { timeInMillis = timeMs }
    val now = Calendar.getInstance()

    return if (smsDate.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
        if (smsDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timeMs))
        } else if (now.get(Calendar.DAY_OF_YEAR) - smsDate.get(Calendar.DAY_OF_YEAR) == 1) {
            "أمس"
        } else {
            SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timeMs))
        }
    } else {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timeMs))
    }
}
