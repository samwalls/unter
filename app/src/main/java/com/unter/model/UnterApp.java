package com.unter.model;

import com.unter.model.exception.LoginException;
import com.unter.model.exception.RegisterException;

import java.util.List;

public class UnterApp extends App<SimpleAppData> {

    public UnterApp(String storeUrl) {
        storage = new FileAppStorageContext<>(storeUrl, new SimpleAppData());
    }

    @Override
    public void onInit() throws Exception {
        storage.initStorage();
    }

    @Override
    public void onExit() throws Exception {
        storage.closeStorage();
    }

    @Override
    public String login(String email, String password) throws LoginException {
        List<String> ids = storage.data.getUserIds();

        for (String id : ids) {
            UserInfo user = storage.data.getUser(id);
            if (user.email.equals(email))
                storage.data.setLoggedInUser(user);
        }

        if (!isLoggedIn()) {
            onLoginFailure();
            throw new LoginException("no user with email \'" + email + "\' found");
        }

        onLoginSuccess();
        return storage.data.getLoggedInUser();
    }

    @Override
    public boolean isLoggedIn() {
        return storage.data.getLoggedInUser() != null;
    }

    public void register(String email, String password) throws RegisterException {
        // first check existing users
        List<String> ids = storage.data.getUserIds();
        for (String id : ids) {
            UserInfo user = storage.data.getUser(id);
            if (user != null) {
                onRegisterFailure();
                throw new RegisterException("user with email \'" + email + "\' is already registered");
            }
        }

        // add a new user
        // TODO: sequential IDs is bad, implement something proper
        // TODO: do something with the password
        String nextId = Integer.toString(ids.size());
        storage.data.addUser(new UserInfo(nextId, email));

        onRegisterSuccess();
    }

    @Override
    public UserInfo getUser(String userId) {
        return storage.data.getUser(userId);
    }

    @Override
    public DriverInfo requestDriver(JourneyRequestInfo request) {
        // TODO
        return null;
    }
}
