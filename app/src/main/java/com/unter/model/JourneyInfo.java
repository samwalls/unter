package com.unter.model;

import java.util.Calendar;

public class JourneyInfo extends Data {

    private String userId = null;

    private String requestId = null;

    private String driverId = null;

    private float completion = 0.0f;

    private boolean isCancelled = false;

    private float rating = 0.0f;

    private long timeFinish = 0;

    public JourneyInfo(String id, UserInfo user, JourneyRequestInfo request, DriverInfo driver) {
        super(id);
        userId = user.id;
        requestId = request.id;
        driverId = driver.id;
    }

    public String getUserId() {
        return userId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getDriverId() {
        return driverId;
    }

    public Float getCompletion() {
        return completion;
    }

    public void setCompletion(float completion) {
        // clamp completion value to [0, 1]
        this.completion = completion < 0.0f ? 0.0f : (completion > 1.0f ? 1.0f : completion);
    }

    public boolean isComplete() {
        return completion >= 1.0f;
    }

    public boolean isInProgress() {
        return completion > 0 && !isComplete();
    }

    public void cancel() {
        this.isCancelled = true;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public long getTimeFinish() {
        return timeFinish;
    }

    public void finish() {
        timeFinish = Calendar.getInstance().getTime().getTime() / 1000;
        setCompletion(1.0f);
    }
}
