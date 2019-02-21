package com.unter.model;

public class UserInfo extends Data {

    public String email;

    public UserInfo(String id, String email) {
        super(id);
        this.email = email;
    }
}
