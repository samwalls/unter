package com.unter.model;

public class DriverInfo extends Data {

    private String name;
    private String telephoneNo;
    private String vehicle;
    private float rating;
    private float price;

    public DriverInfo(String id, String name, String telephoneNo, String vehicle, float rating, float price) {
        super(id);
        this.name = name;
        this.telephoneNo = telephoneNo;
        this.vehicle = vehicle;
        this.rating = rating;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public String getTelephoneNo() {
        return telephoneNo;
    }

    public String getVehicle() {
        return vehicle;
    }

    public float getRating() {
        return rating;
    }

    public float getPrice() {
        return price;
    }
}
