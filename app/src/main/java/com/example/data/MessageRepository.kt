package com.example.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MessageRepository(private val messageDao: MessageDao) {

    val conversationsFlow: Flow<List<DbMessage>> = messageDao.getConversationsFlow()
    val unreadCountFlow: Flow<Int> = messageDao.getUnreadCountFlow()

    fun getThreadFlow(address: String): Flow<List<DbMessage>> {
        return messageDao.getMessageThreadFlow(address)
    }

    fun searchMessagesFlow(query: String): Flow<List<DbMessage>> {
        val formattedQuery = "%$query%"
        return messageDao.searchMessages(formattedQuery)
    }

    suspend fun insertMessage(message: DbMessage): Long = withContext(Dispatchers.IO) {
        messageDao.insertMessage(message)
    }

    suspend fun deleteMessage(id: Long) = withContext(Dispatchers.IO) {
        messageDao.deleteMessage(id)
    }

    suspend fun deleteConversation(address: String) = withContext(Dispatchers.IO) {
        messageDao.deleteConversation(address)
    }

    suspend fun markAsRead(address: String) = withContext(Dispatchers.IO) {
        messageDao.markAsRead(address)
    }

    /**
     * Synchronize system SMS threads into our local Room Cache if permission is granted.
     */
    suspend fun syncSystemSms(context: Context) = withContext(Dispatchers.IO) {
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val uri: Uri = Uri.parse("content://sms")
            val projection = arrayOf("_id", "thread_id", "address", "body", "date", "type", "read")
            
            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC LIMIT 200"
            )

            val systemMessages = mutableListOf<DbMessage>()
            cursor?.use { c ->
                val idIdx = c.getColumnIndexOrThrow("_id")
                val threadIdIdx = c.getColumnIndexOrThrow("thread_id")
                val addressIdx = c.getColumnIndexOrThrow("address")
                val bodyIdx = c.getColumnIndexOrThrow("body")
                val dateIdx = c.getColumnIndexOrThrow("date")
                val typeIdx = c.getColumnIndexOrThrow("type")
                val readIdx = c.getColumnIndexOrThrow("read")

                while (c.moveToNext()) {
                    val smsId = c.getLong(idIdx)
                    val threadId = c.getLong(threadIdIdx)
                    val address = c.getString(addressIdx) ?: "Unknown"
                    val body = c.getString(bodyIdx) ?: ""
                    val date = c.getLong(dateIdx)
                    val type = c.getInt(typeIdx) // 1 = inbox, 2 = sent
                    val read = c.getInt(readIdx) == 1

                    // Resolve contact name from address if possible
                    val contactName = resolveContactName(context, address)

                    systemMessages.add(
                        DbMessage(
                            systemSmsId = smsId,
                            address = address,
                            contactName = contactName,
                            body = body,
                            date = date,
                            type = type,
                            isRead = read,
                            threadId = threadId
                        )
                    )
                }
            }

            if (systemMessages.isNotEmpty()) {
                // If we fetched actual messages, insert them to Room
                messageDao.insertMessages(systemMessages)
            } else {
                // If system messages has 0 messages (e.g. inside our emulator),
                // we check if Room has any messages, if completely empty, pre-populate with mock conversations.
                prepopulateIfEmpty()
            }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Failed to sync system SMS, falling back to mock: ${e.message}")
            prepopulateIfEmpty()
        }
    }

    suspend fun prepopulateIfEmpty() = withContext(Dispatchers.IO) {
        val current = conversationsFlow.first()
        if (current.isEmpty()) {
            val now = System.currentTimeMillis()
            val hourMs = 3600000L
            val dayMs = 24 * hourMs

            val mockMessages = listOf(
                // Conversation 1: Ahmad Al-Banna (Bilingual / Arabic Friendly developer colleague)
                DbMessage(
                    address = "+201012345678",
                    contactName = "أاحمد البنا 👨‍💻",
                    body = "أهلاً بك يا صديقي! هل قمنا بتجهيز الـ BroadcastReceiver والمزامنة التلقائية؟ للتجربة أرسل لي أي شيء وسأقوم بالرد عليك تلقائياً فوراً!",
                    date = now - 5 * 60 * 1000, // 5 mins ago
                    type = 1, // Received
                    isRead = false,
                    threadId = 1L
                ),
                DbMessage(
                    address = "+201012345678",
                    contactName = "أحمد البنا 👨‍💻",
                    body = "نعم، جاري العمل حالياً على تصميم وتطوير التطبيق وربطه بقواعد البيانات والمستقبلات الخاصة بالنظام.",
                    date = now - 10 * 60 * 1000,
                    type = 2, // Sent
                    isRead = true,
                    threadId = 1L
                ),
                DbMessage(
                    address = "+201012345678",
                    contactName = "أحمد البنا 👨‍💻",
                    body = "يسعدني جداً البدء في هذا المشروع الرائد!",
                    date = now - 15 * 60 * 1000,
                    type = 1, // Received
                    isRead = true,
                    threadId = 1L
                ),

                // Conversation 2: Mom (الوالدة ❤️)
                DbMessage(
                    address = "+966509876543",
                    contactName = "الوالدة ❤️",
                    body = "يا بني، لا تنسَ المرور بالمنزل لشراء بعض احتياجات البيت اليوم، طمأني عليك عندما تصل بالسلامة.",
                    date = now - 2 * hourMs, // 2 hours ago
                    type = 1,
                    isRead = true,
                    threadId = 2L
                ),
                DbMessage(
                    address = "+966509876543",
                    contactName = "الوالدة ❤️",
                    body = "حاضر يا أمي العزيزة، سأقوم بشرائها فوراً وتجهيزها لك قبل المغرب إن شاء الله.",
                    date = now - 1 * hourMs - 50 * 60000,
                    type = 2,
                    isRead = true,
                    threadId = 2L
                ),

                // Conversation 3: Aramex Delivery
                DbMessage(
                    address = "Aramex",
                    contactName = "Aramex 📦",
                    body = "عزيزي العميل، شحنتك رقم 298471927 من الرياض قيد التوصيل اليوم مع المندوب. يرجى تجهيز مبلغ 45 ريال أو الدفع عبر مدى. لتأكيد الموقع: https://aramex.com/track/298471927",
                    date = now - 1 * dayMs, // Yesterday
                    type = 1,
                    isRead = true,
                    threadId = 3L
                ),

                // Conversation 4: AlAhli Bank (البنك الأهلي OTP)
                DbMessage(
                    address = "SNB-AlAhli",
                    contactName = "SNB-AlAhli 🏦",
                    body = "البنك الأهلي السعودي: رمز الأمان لمرة واحدة للعملية الشرائية بمبلغ 125.00 ريال هو 4819. الرجاء عدم مشاركة الرمز مع أي شخص آخر.",
                    date = now - 2 * dayMs,
                    type = 1,
                    isRead = true,
                    threadId = 4L
                ),

                // Conversation 5: Automated Bot / Chat companion
                DbMessage(
                    address = "8080",
                    contactName = "SmsBot 🤖",
                    body = "مرحباً بك في تطبيق الرسائل SMS Messenger المطور! أنا الآلي الخاص بالتطبيق. هل تعجبك هندسة الواجهة وطريقة تصفح الخيوط والمحادثات؟ أرسل 'مساعدة' أو 'المزيد' لاستعراض الأوامر المتاحة.",
                    date = now - 3 * dayMs,
                    type = 1,
                    isRead = true,
                    threadId = 5L
                )
            )

            messageDao.insertMessages(mockMessages)
        }
    }

    private fun resolveContactName(context: Context, address: String): String? {
        val uri = Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(address)
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
