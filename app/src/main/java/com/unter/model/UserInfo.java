package com.unter.model;

public class UserInfo extends Data {

    public String email;

    // TODO: this is just a placeholder for a password hash or encrypted text - not intended to have plaintext passwords
    public String password;

    /**
     * The current journey the user is tied to. Null if the user is not currently in a journey.
     */
    private String journeyId = null;

    public UserInfo(String id, String email, String password) {
        super(id);
        this.email = email;
        this.password = password;
    }

    public String getJourneyId() {
        return journeyId;
    }

    public void setJourneyId(String id) {
        journeyId = id;
    }
}
