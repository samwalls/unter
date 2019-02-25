package com.unter.model

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModel
import android.content.SharedPreferences
import android.util.Log.d
import com.unter.model.exception.LoginException

@SuppressLint("LogNotTimber")
class UnterAppModel : ViewModel() {

    private val TAG: String = UnterAppModel::class.java.canonicalName!!

    private lateinit var app: UnterApp

    private var _currentRequest: JourneyRequestInfo? = null

    private var _currentUser: UserInfo? = null

    var currentRequest: JourneyRequestInfo?
        get() = _currentRequest
        set(value) {_currentRequest = value }

    var currentUser: UserInfo? = _currentUser
        get() = _currentUser

    fun initStorage(sharedPreferences: SharedPreferences) {
        app = UnterApp(sharedPreferences)
        app.init()
    }

    override fun onCleared() {
        super.onCleared()
        app.exit()
    }

    fun save() {
        app.storage.saveStorage()
    }

    fun load() {
        app.storage.loadStorage()
    }

    fun login(email: String, password: String): UserInfo {
        val userId = app.login(email, password)

        if (userId == null || app.getUser(userId) == null)
            throw LoginException("no user ID found for email '$email'")

        d(TAG, "user with ID $userId found for email '$email'")

        _currentUser = app.getUser(userId)
        return currentUser!!
    }

    fun register(email: String, password: String) = app.register(email, password)
}