package com.unter.model

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.util.Log.d
import com.unter.model.exception.LoginException

@SuppressLint("LogNotTimber")
class UnterAppModel : ViewModel() {

    private val TAG: String = UnterAppModel::class.java.canonicalName!!

    private lateinit var app: UnterApp

    private var initialised: Boolean = false

    private var _lastUserLogin: String? = null

    var lastUserLogin: String? = _lastUserLogin
        get() = _lastUserLogin

    fun initStorage(context: Context) {
        app = UnterApp(context.getSharedPreferences("unter", Context.MODE_PRIVATE))
        app.init()
        initialised = true
    }

    fun isInitialised(): Boolean {
        return initialised
    }

    override fun onCleared() {
        super.onCleared()
        app.exit()
        initialised = false
    }

    fun getDriverList(): List<DriverInfo> {
        // return list of current drivers
        return app.storage.data.driverIds.map { id ->
            app.storage.data.getDriver(id)
        }
    }

    fun getJourneys(user: UserInfo): List<JourneyInfo> {
        // return all journeys this user took part in
        return app.storage.data.journeyIds
            .map { id -> getJourney(id) }
            .filter { journey -> journey.userId == user.id }
    }

    fun getUser(id: String): UserInfo {
        return app.storage.data.getUser(id)
    }

    fun getDriver(id: String): DriverInfo {
        return app.storage.data.getDriver(id)
    }

    fun getJourneyRequest(id: String): JourneyRequestInfo {
        return app.storage.data.getJourneyRequest(id)
    }

    fun getJourney(id: String): JourneyInfo {
        return app.storage.data.getJourney(id)
    }

    fun addJourneyRequest(request: JourneyRequestInfo): String {
        return app.addJourneyRequest(request)
    }

    fun confirmJourney(user: UserInfo, request: JourneyRequestInfo, driver: DriverInfo): JourneyInfo? {
        return app.confirmJourney(user, request, driver)
    }

    fun save() {
        app.storage.saveStorage()
    }

    fun load() {
        app.storage.loadStorage()
    }

    fun login(email: String, password: String): UserInfo {
        val userId = app.login(email, password)

        if (userId == null || app.storage.data.getUser(userId) == null)
            throw LoginException("no user ID found for email '$email'")

        d(TAG, "user with ID $userId found for email '$email'")
        _lastUserLogin = userId
        return app.storage.data.getUser(userId)
    }

    fun logout() {
        _lastUserLogin = null
    }

    fun register(email: String, password: String) = app.register(email, password)
}