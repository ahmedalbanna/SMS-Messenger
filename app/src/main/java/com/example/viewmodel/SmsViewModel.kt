package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DbMessage
import com.example.data.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MessageRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = MessageRepository(database.messageDao())
    }

    val conversations: StateFlow<List<DbMessage>> = repository.conversationsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val unreadCount: StateFlow<Int> = repository.unreadCountFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _selectedAddress = MutableStateFlow<String?>(null)
    val selectedAddress: StateFlow<String?> = _selectedAddress.asStateFlow()

    private val _selectedContactName = MutableStateFlow<String?>(null)
    val selectedContactName: StateFlow<String?> = _selectedContactName.asStateFlow()

    val activeThreadMessages: StateFlow<List<DbMessage>> = _selectedAddress
        .flatMapLatest { address ->
            if (address != null) {
                repository.getThreadFlow(address)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<DbMessage>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchMessagesFlow(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun selectConversation(address: String, name: String?) {
        _selectedAddress.value = address
        _selectedContactName.value = name
        viewModelScope.launch {
            repository.markAsRead(address)
        }
    }

    fun deselectConversation() {
        _selectedAddress.value = null
        _selectedContactName.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun syncMessages(context: Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.syncSystemSms(context)
            _isSyncing.value = false
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    fun deleteConversation(address: String) {
        viewModelScope.launch {
            repository.deleteConversation(address)
            if (_selectedAddress.value == address) {
                deselectConversation()
            }
        }
    }

    fun sendMessage(address: String, body: String, context: Context, isDefaultSmsApp: Boolean) {
        if (body.isBlank()) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val threadId = address.hashCode().toLong()
            val contactName = _selectedContactName.value ?: resolveContactName(context, address)

            // 1. Insert Sent message immediately in our local DB
            val sentMsgId = repository.insertMessage(
                DbMessage(
                    address = address,
                    contactName = contactName,
                    body = body,
                    date = now,
                    type = 2, // SENT
                    isRead = true,
                    threadId = threadId
                )
            )

            // 2. Try to send REAL SMS if permissions are available
            if (isDefaultSmsApp) {
                try {
                    val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        context.getSystemService(SmsManager::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getDefault()
                    }
                    smsManager.sendTextMessage(address, null, body, null, null)
                    Log.d("SmsViewModel", "Real SMS sent to $address")
                } catch (e: Exception) {
                    Log.e("SmsViewModel", "Failed sending real SMS: ${e.message}")
                }
            } else {
                Log.d("SmsViewModel", "Simulation Mode: Message sent locally inside Room.")
            }

            // 3. Simulated Auto-Replies for awesome emulator experience
            triggerSimulatedReply(address, contactName, body, threadId)
        }
    }

    private fun triggerSimulatedReply(
        address: String,
        contactName: String?,
        sentBody: String,
        threadId: Long
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            // Delay 1.5 seconds to simulate real communication
            delay(1500)

            val cleanBody = sentBody.trim()
            val isArabic = cleanBody.any { it in '\u0600'..'\u06FF' }

            val replyText: String = when {
                // Ahmad AlBanna
                address == "+201012345678" -> {
                    when {
                        sentBody.contains("Receiver", ignoreCase = true) || sentBody.contains("مستقبل") -> {
                            "نعم يا صديقي! الـ BroadcastReceiver يعمل بالخلفية بشكل رائع ومسجل بالـ Manifest لالتخاذ الرسائل حتى وإن كان التطبيق مغلقاً!"
                        }
                        sentBody.contains("theme", ignoreCase = true) || sentBody.contains("تصميم") -> {
                            "تم تطبيق قيم معايير Material 3 بالكامل مع دعم التوجه العربي RTL بشكل رائع، ليرضي تطلعات المطورين والجمهور العربي."
                        }
                        sentBody.contains("السلام") || sentBody.contains("مرحبا") || sentBody.contains("أهلاً") || sentBody.contains("اهلاً") -> {
                            "وعليكم السلام يا غالي! أهلاً بك ومرحباً في نظام المراسلة الذكي الخاص بنا. كيف يمكنني إثراء نقاشنا اليوم؟"
                        }
                        else -> {
                            "صحيح! هذا يدل على أن منظومة Flow والـ Room Database تعملان بتوافق مثالي لمزامنة الواجهة لحظياً!"
                        }
                    }
                }
                // Mom
                address == "+966509876543" -> {
                    "حفظك الله ورعاك يا بني، لا تتأخر عليّ وأخبرني حين تقترب من الوصول. دعواتي لك بالتوفيق والسداد."
                }
                // Aramex
                address.contains("Aramex", ignoreCase = true) -> {
                    "عزيزي العميل، لا يمكن الرد على هذه الرسالة التلقائية. للحصول على المساعدة يرجى زيارة موقعنا أو الاتصال بـ 920027447."
                }
                // SNB-AlAhli
                address.contains("AlAhli", ignoreCase = true) -> {
                    "تنبيه أمان: نود تذكيرك بأن البنك لا يطلب أبداً تحديث بياناتك المصرفية أو الرمز السري عبر الرسائل النصية."
                }
                // SmsBot
                address == "8080" -> {
                    when {
                        cleanBody.contains("مساعدة") || cleanBody.contains("help") -> {
                            "أهلاً بك في الدعم الآلي! 🤖 الأوامر المتاحة:\n- 'تنظيف': حذف كل رسائل هذه المحادثة.\n- 'معلومات': تفاصيل هندسة بنية التطبيق التقنية.\n- 'صلاحيات': معلومات عن الصلاحيات الحساسة للرسائل."
                        }
                        cleanBody.contains("تنظيف") -> {
                            repository.deleteConversation(address)
                            "تم مسح المحادثة بنجاح! أرسل أي رسالة للبدء من جديد."
                        }
                        cleanBody.contains("معلومات") -> {
                            "التطبيق مبني بنمط MVVM الموصى به برمجياً من جوجل. ويستخدم Room SQLite للتخزين المحلي كبديل سريع وآمن، ويدعم الربط المباشر مع Telephony Provider الخاص بنظام أندرويد."
                        }
                        cleanBody.contains("صلاحيات") -> {
                            "يتطلب التطبيق صلاحيات READ_SMS و SEND_SMS و RECEIVE_SMS و READ_CONTACTS لتمكينه من الاتصال الفعلي بالشبكة وإرسال واستلام الرسائل النصية."
                        }
                        else -> {
                            "عذراً، لم أفهم الأمر '$cleanBody'. أرسل كلمة 'مساعدة' لعرض قائمة الخيارات والتعليمات المتاحة."
                        }
                    }
                }
                // General Simulation Fallback
                else -> {
                    if (isArabic) {
                        "مرحباً! لقد تلقيت رسالتك: '$sentBody'. شكراً لتجربة تطبيق الرسائل SMS Messenger الرائع والمتكامل!"
                    } else {
                        "Hello! I received your message: '$sentBody'. Thank you for testing out our beautiful, high-fidelity SMS Messenger!"
                    }
                }
            }

            // Insert Simulated Reply to Room Database (With type = 1, RECEIVED)
            repository.insertMessage(
                DbMessage(
                    address = address,
                    contactName = contactName,
                    body = replyText,
                    date = System.currentTimeMillis(),
                    type = 1, // Received
                    isRead = false,
                    threadId = threadId
                )
            )

            // Trigger System Notification so user gets notified
            com.example.receiver.SmsReceiver.showNotification(
                getApplication(),
                DbMessage(
                    address = address,
                    contactName = contactName,
                    body = replyText,
                    date = System.currentTimeMillis(),
                    type = 1,
                    isRead = false,
                    threadId = threadId
                )
            )
        }
    }

    private fun resolveContactName(context: Context, address: String): String? {
        val uri = android.net.Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(address)
        )
        val projection = arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
