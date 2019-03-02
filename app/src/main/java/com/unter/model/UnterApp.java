package com.unter.model;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import com.unter.model.exception.JourneyConfirmationException;
import com.unter.model.exception.LoginException;
import com.unter.model.exception.RegisterException;

import java.util.List;

import static android.util.Log.d;

@SuppressLint("LogNotTimber")
public class UnterApp extends App<SimpleAppData> {

    private static final String TAG = UnterApp.class.getCanonicalName();

    public UnterApp(SharedPreferences sharedPreferences) {
        storage = new SharedPreferencesStorageContext(new SimpleAppData(), sharedPreferences);
    }

    @Override
    public void onInit() throws Exception {
        storage.initStorage();
    }

    @Override
    public void onExit() throws Exception {
        storage.saveStorage();
        storage.closeStorage();
    }

    @Override
    public String login(String email, String password) throws LoginException {
        storage.data.setLoggedInUser(null);
        List<String> ids = storage.data.getUserIds();

        for (String id : ids) {
            UserInfo user = storage.data.getUser(id);
            if (user.email.equals(email)) {
                // TODO: work based on encrypted or salt/hashed password
                if (user.password.equals(password))
                    storage.data.setLoggedInUser(user);
                else
                    throw new LoginException("incorrect password");
            }
        }

        if (!isLoggedIn()) {
            onLoginFailure();
            throw new LoginException("no user with email \'" + email + "\' found");
        }

        onLoginSuccess();
        return storage.data.getLoggedInUser();
    }

    public boolean isLoggedIn() {
        return storage.data.getLoggedInUser() != null;
    }

    public void register(String email, String password) throws RegisterException {

        if (email == null || email.isEmpty())
            throw new RegisterException("email cannot be empty");

        if (password == null || password.isEmpty())
            throw new RegisterException("password cannot be empty");

        // first check existing users
        List<String> ids = storage.data.getUserIds();
        for (String id : ids) {
            UserInfo user = storage.data.getUser(id);
            if (user != null && user.email.equals(email)) {
                onRegisterFailure();
                throw new RegisterException("user with email \'" + email + "\' is already registered");
            }
        }

        // add a new user
        // TODO: sequential IDs is bad, implement something proper
        // TODO: encrypt/hash the password
        String nextId = Integer.toString(ids.size());
        storage.data.addUser(new UserInfo(nextId, email, password));

        onRegisterSuccess();
    }

    public String addJourneyRequest(JourneyRequestInfo request) {
        request.id = Integer.toString(storage.data.getJourneyRequestIds().size());
        storage.data.addJourneyRequest(request);
        return request.id;
    }

    public JourneyInfo confirmJourney(UserInfo user, JourneyRequestInfo request, DriverInfo driver) throws JourneyConfirmationException {

        if (user.getJourneyId() != null)
            throw new JourneyConfirmationException("cannot make a new journey when the user is already in one");

        // get a driver
        String nextId = Integer.toString(storage.data.getJourneyIds().size());
        JourneyInfo journey = new JourneyInfo(nextId, user, request, driver);
        // commit a new journey object to the storage context
        storage.data.addJourney(journey);

        // set the user's current journey
        user.setJourneyId(journey.id);

        return journey;
    }
}
