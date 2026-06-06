package com.example.receiver

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class SmsRespondService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        Log.d("SmsRespondService", "onBind called")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SmsRespondService", "onStartCommand called")
        stopSelf()
        return START_NOT_STICKY
    }
}
