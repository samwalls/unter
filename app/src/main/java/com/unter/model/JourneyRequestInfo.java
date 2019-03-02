package com.unter.model;

public class JourneyRequestInfo extends Data {

    private Long pickupTime = null;

    private Double originLong = null, originLat = null;
    private Double destinationLong = null, destinationLat = null;

    public JourneyRequestInfo() {
        super(null);
    }

    public JourneyRequestInfo(String id) {
        super(id);
    }

    public JourneyRequestInfo(String id, double oLng, double oLat, double dLng, double dLat, long pickupTime) {
        super(id);
        this.originLong = oLng;
        this.originLat = oLat;
        this.destinationLong = dLng;
        this.destinationLat = dLat;
        this.pickupTime = pickupTime;
    }

    public Double getOriginLong() {
        return originLong;
    }

    public void setOriginLong(Double originLong) {
        this.originLong = originLong;
    }

    public Double getOriginLat() {
        return originLat;
    }

    public void setOriginLat(Double originLat) {
        this.originLat = originLat;
    }

    public Double getDestinationLong() {
        return destinationLong;
    }

    public void setDestinationLong(Double destinationLong) {
        this.destinationLong = destinationLong;
    }

    public Double getDestinationLat() {
        return destinationLat;
    }

    public void setDestinationLat(Double destinationLat) {
        this.destinationLat = destinationLat;
    }

    public void setPickupTime(Long timestamp) {
        this.pickupTime = timestamp;
    }

    public Long getPickupTime() {
        return this.pickupTime;
    }
}
