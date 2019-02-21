package com.unter.model;

import java.util.List;

public interface AppDataModel {

    List<String> getUserIds();

    List<String> getDriverIds();

    UserInfo getUser(String id);

    void addUser(UserInfo user);

    void deleteUser(String id);

    DriverInfo getDriver(String id);

    void addDriver(DriverInfo driver);

    void deleteDriver(String id);
}
