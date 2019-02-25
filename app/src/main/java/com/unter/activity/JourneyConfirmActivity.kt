package com.unter.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log.d
import android.util.Log.e
import android.widget.Toast
import com.unter.R

@SuppressLint("LogNotTimber")
class JourneyConfirmActivity : Activity() {

    private val TAG: String = JourneyConfirmActivity::class.java.canonicalName!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey_confirm)

        if (intent?.action == Intent.ACTION_VIEW) {
            d(TAG, "received ACTION_VIEW intent for: '${intent.dataString}'")

            // query parameters in the URI
            val userId: String? = intent.data!!.getQueryParameter("userId")
            val originLong: String? = intent.data!!.getQueryParameter("originLong")
            val originLat: String? = intent.data!!.getQueryParameter("originLat")
            val destinationLong: String? = intent.data!!.getQueryParameter("destinationLong")
            val destinationLat: String? = intent.data!!.getQueryParameter("destinationLat")

            if (userId == null)
                error("could not handle ride request", "no user ID provided in ride request")
            if (originLong == null)
                error("could not handle ride request", "no origin longitude provided in ride request")
            if (originLat == null)
                error("could not handle ride request", "no origin latitude provided in ride request")
            if (destinationLong == null)
                error("could not handle ride request", "no destination longitude provided in ride request")
            if (destinationLat == null)
                error("could not handle ride request", "no destination latitude provided in ride request")


        }
    }

    private fun error(message: String, error: String) {
        e(TAG, error)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}