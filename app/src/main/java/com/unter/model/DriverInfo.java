package com.unter.model;

public class DriverInfo extends Data {

    String name;
    float rating;

    public DriverInfo(String id, String name, float rating) {
        super(id);
        this.name = name;
        this.rating = rating;
    }
}
