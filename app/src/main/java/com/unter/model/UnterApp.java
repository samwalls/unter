package com.unter.model;

import com.unter.model.exception.LoginException;
import com.unter.model.exception.RegisterException;

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

    public void login(String username, String password) throws LoginException {
        // TODO
    }

    @Override
    public void onLoginSuccess() {
        // TODO
    }

    @Override
    public void onLoginFailure() {
        // TODO
    }

    @Override
    public boolean isLoggedIn() {
        // TODO
        return false;
    }

    public void register(String username, String password) throws RegisterException {
        // TODO
    }

    @Override
    public void onRegisterSuccess() {
        // TODO
    }

    @Override
    public void onRegisterFailure() {
        // TODO
    }
}
