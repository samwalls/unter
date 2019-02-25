package com.unter.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleAppData implements AppDataModel, Serializable {

    private Map<String, UserInfo> users;

    private Map<String, DriverInfo> drivers;

    private String loggedInUser = null;

    public SimpleAppData() {
        users = new HashMap<>();
        drivers = new HashMap<>();
    }

    @Override
    public List<String> getUserIds() {
        return new ArrayList<>(users.keySet());
    }

    @Override
    public List<String> getDriverIds() {
        return new ArrayList<>(drivers.keySet());
    }

    @Override
    public UserInfo getUser(String id) {
        if (!users.containsKey(id))
            return null;
        return users.get(id);
    }

    @Override
    public void addUser(UserInfo user) {
        users.put(user.id, user);
    }

    @Override
    public void deleteUser(String id) {
        users.remove(id);
    }

    @Override
    public void setLoggedInUser(UserInfo user) {
        loggedInUser = user.id;
    }

    @Override
    public String getLoggedInUser() {
        return loggedInUser;
    }

    @Override
    public DriverInfo getDriver(String id) {
        if (!drivers.containsKey(id))
            return null;
        return drivers.get(id);
    }

    @Override
    public void addDriver(DriverInfo driver) {
        drivers.put(driver.id, driver);
    }

    @Override
    public void deleteDriver(String id) {
        drivers.remove(id);
    }
}
