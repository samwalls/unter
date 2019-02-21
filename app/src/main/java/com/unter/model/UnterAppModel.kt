package com.unter.model

import android.arch.lifecycle.ViewModel

class UnterAppModel : ViewModel() {

    private lateinit var app: UnterApp

    fun initStorage(storageDir: String) {
        app = UnterApp(storageDir)
        app.init()
    }

    override fun onCleared() {
        super.onCleared()
        app.exit()
    }

    fun login(email: String, password: String) = app.login(email, password)

    fun register(email: String, password: String) = app.register(email, password)
}