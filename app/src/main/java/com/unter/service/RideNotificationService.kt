package com.unter.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast

@SuppressLint("LogNotTimber")
class RideNotificationService : Service() {

    private val TAG: String = RideNotificationService::class.java.canonicalName!!

    override fun onBind(intent: Intent?): IBinder? {
        if (intent != null && intent.data!!.path == "ride/notify" && intent.action == Intent.ACTION_RUN) {
            val userId: String? = intent.data!!.getQueryParameter("userId")
            val journeyId: String? = intent.data!!.getQueryParameter("journeyId")

            if (userId == null)
                error("could not start ride notification service", "no user ID provided to ride notification service")
            if (journeyId == null)
                error("could not start ride notification service", "no journey ID provided to ride notification service")
        }
        // TODO TODO TODO
        return null
    }

    private fun error(message: String, error: String) {
        Log.e(TAG, error)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}