package com.unter.model;

import com.unter.model.exception.*;

public abstract class App<T extends AppDataModel> {

    protected AppStorageContext<T> storage;

    public void init() throws Exception {
        onInit();
        storage.initStorage();
    }

    public void exit() throws Exception {
        onExit();
        storage.saveStorage();
        storage.closeStorage();
    }

    public T data() {
        return storage.data;
    }

    public AppStorageContext<T> storage() {
        return storage;
    }

    protected abstract void onInit() throws Exception;

    protected abstract void onExit() throws Exception;

    public abstract void login(String username, String password) throws LoginException;

    public void onLoginSuccess() {}

    public void onLoginFailure() {}

    public abstract boolean isLoggedIn();

    public abstract void register(String username, String password) throws RegisterException;

    public void onRegisterSuccess() {}

    public void onRegisterFailure() {}
}
