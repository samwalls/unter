package com.unter

import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.unter.activity.MainActivity
import com.unter.model.UnterApp
import com.unter.model.UserInfo

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.unter", appContext.packageName)
    }

    @Rule @JvmField
    var mainActivityTestRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)

    @Test
    fun defaultFileStorage() {
        val app: UnterApp = mainActivityTestRule.activity.getAppModel()
        assertTrue("expected list of user IDs to be empty before start", app.data().userIds.isEmpty())
        assertTrue("expected list of driver IDs to be empty before start", app.data().driverIds.isEmpty())
    }

    @Test
    fun saveAndLoadFromStorage() {
        val app: UnterApp = mainActivityTestRule.activity.getAppModel()

        // add to the storage context
        app.data().addUser(UserInfo("qwertyuiop", "test@test.com"))
        assertEquals("expected list of user IDs to be non-empty after adding a user", 1, app.data().userIds.size)

        // save storage context to file
        app.storage().saveStorage()

        // modify the storage context
        app.data().deleteUser("qwertyuiop")
        assertTrue("expected list of user IDs to be empty after deleting its only user", app.data().userIds.isEmpty())

        // reload the storage context from the file
        app.storage().loadStorage()

        // check that the values were persisted
        assertEquals("", 1, app.data().userIds.size)
        val user: UserInfo = app.data().getUser("qwertyuiop")
        assertEquals("", "qwertyuiop", user.id)
        assertEquals("", "test@test.com", user.email)
    }
}
