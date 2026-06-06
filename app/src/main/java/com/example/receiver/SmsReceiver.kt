package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.DbMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("SmsReceiver", "onReceive action: $action")
        
        if (action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION || action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNotEmpty()) {
                val pendingResult = goAsync()
                receiverScope.launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val dao = db.messageDao()

                        // Group message bodies from same address
                        val address = messages[0].originatingAddress ?: "Unknown"
                        val body = messages.joinToString(separator = "") { it.messageBody }
                        val timestamp = messages[0].timestampMillis

                        val isRead = false
                        val threadId = SmsThreadIdGenerator.generateId(address)

                        // Insert message to database
                        val dbMessage = DbMessage(
                            address = address,
                            contactName = resolveContactName(context, address),
                            body = body,
                            date = timestamp,
                            type = 1, // RECEIVED
                            isRead = isRead,
                            threadId = threadId
                        )
                        dao.insertMessage(dbMessage)

                        // Show system notification
                        showNotification(context, dbMessage)

                    } catch (e: Exception) {
                        Log.e("SmsReceiver", "Error saving received SMS: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
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

    companion object {
        private const val CHANNEL_ID = "sms_notifications_channel"
        private const val NOTIFICATION_ID = 23982

        fun showNotification(context: Context, sms: DbMessage) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "SMS Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for incoming SMS notifications"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("chat_address", sms.address)
                putExtra("chat_name", sms.contactName ?: sms.address)
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

            val displayName = sms.contactName ?: sms.address
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(displayName)
                .setContentText(sms.body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
}

/**
 * A helper to consistently generate thread IDs based on address
 */
object SmsThreadIdGenerator {
    fun generateId(address: String): Long {
        return address.hashCode().toLong()
    }
}
