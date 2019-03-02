package com.unter.model;

import java.io.Serializable;
import java.util.*;

public class SimpleAppData implements AppDataModel, Serializable {

    private Map<String, UserInfo> users;

    private Map<String, DriverInfo> drivers;

    private Map<String, JourneyInfo> journeys;

    private Map<String, JourneyRequestInfo> journeyRequests;

    private String loggedInUser = null;

    public SimpleAppData() {
        users = new HashMap<>();
        drivers = new HashMap<>();
        journeys = new HashMap<>();
        journeyRequests = new HashMap<>();

        // add a test user
        addUser(new UserInfo("0", "test@test.com", "password"));

        // add a set of test requests
        addJourneyRequest(new JourneyRequestInfo("0", 50.0, -4.0, 50.1, -4.1, Calendar.getInstance().getTime().getTime() / 1000));
        addJourneyRequest(new JourneyRequestInfo("1", 51.0, -3.0, 51.1, -3.1, Calendar.getInstance().getTime().getTime() / 1000));

        // add a set of test drivers
        addDriver(new DriverInfo("0", "Alice Bloggs", "447700900000", "Honda Civic", 4.5f, 0.6f));
        addDriver(new DriverInfo("1", "Bob Crichton", "447700912345", "Ford Fiesta", 4.4f, 0.4f));
        addDriver(new DriverInfo("2", "Charlie Davidson", "447700900123", "Jaguar XJ", 4.8f, 0.95f));
        addDriver(new DriverInfo("3", "Donna Edwards", "447700911111", "Mini Countryman", 4.1f, 0.5f));
        addDriver(new DriverInfo("4", "Ellie Fraser", "447700911110", "Vauxhall Vectra", 3.5f, 0.6f));
        addDriver(new DriverInfo("5", "Frank Gray", "447700901010", "Subaru Impreza", 2.8f, 0.55f));

        // add a set of test histories
        addJourney(new JourneyInfo("0", getUser("0"), getJourneyRequest("0"), getDriver("0")));
        addJourney(new JourneyInfo("1", getUser("0"), getJourneyRequest("1"), getDriver("4")));

        getJourney("0").setCompletion(1.0f);
        getJourney("0").setRating(8.0f);
        getJourney("1").cancel();
        getJourney("1").setRating(1.0f);
    }

    @Override
    public List<String> getUserIds() {
        return new ArrayList<>(users.keySet());
    }

    @Override
    public List<String> getDriverIds() {
        return new ArrayList<>(drivers.keySet());
    }

    public List<String> getJourneyIds() {
        return new ArrayList<>(journeys.keySet());
    }

    public List<String> getJourneyRequestIds() {
        return new ArrayList<>(journeyRequests.keySet());
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
        if (user == null)
            loggedInUser = null;
        else
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

    public void addJourneyRequest(JourneyRequestInfo request) {
        journeyRequests.put(request.id, request);
    }

    public JourneyRequestInfo getJourneyRequest(String id) {
        if (!journeyRequests.containsKey(id))
            return null;
        return journeyRequests.get(id);
    }

    public void addJourney(JourneyInfo journey) {
        journeys.put(journey.id, journey);
    }

    public JourneyInfo getJourney(String id) {
        if (!journeys.containsKey(id))
            return null;
        return journeys.get(id);
    }
}
