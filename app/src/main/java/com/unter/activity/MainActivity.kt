package com.unter.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.unter.*
import com.unter.model.UnterAppModel

class MainActivity : AppCompatActivity() {

    private lateinit var model: UnterAppModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        model = ViewModelProviders.of(this).get(UnterAppModel::class.java)
        model.initStorage(getPreferences(Context.MODE_PRIVATE))
    }

    override fun onDestroy() {
        super.onDestroy()
        model.save()
    }

    override fun onPause() {
        super.onPause()
        model.save()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        model.save()
    }

    override fun onResume() {
        super.onResume()
        model.load()
    }
}
