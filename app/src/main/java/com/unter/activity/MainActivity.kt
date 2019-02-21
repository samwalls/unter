package com.unter.activity

import android.arch.lifecycle.ViewModelProviders
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
        model.initStorage(filesDir.absolutePath)
    }
}
