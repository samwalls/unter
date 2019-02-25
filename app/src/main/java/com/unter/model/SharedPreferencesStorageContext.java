package com.unter.model;

import android.content.SharedPreferences;
import com.google.gson.Gson;

public class SharedPreferencesStorageContext extends AppStorageContext<SimpleAppData> {

    private static final String SHARED_PREF_KEY = "AppDataModel";

    private SharedPreferences sharedPreferences;

    public SharedPreferencesStorageContext(SimpleAppData defaultModel, SharedPreferences sharedPreferences) {
        super(defaultModel);
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public void initStorage() throws Exception {

    }

    @Override
    public void loadStorage() throws Exception {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(SHARED_PREF_KEY, "");
        data = gson.fromJson(json, SimpleAppData.class);
    }

    @Override
    public void saveStorage() throws Exception {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(data);
        prefsEditor.putString(SHARED_PREF_KEY, json);
        // TODO: consider apply() vs commit()
        prefsEditor.commit();
    }

    @Override
    public void deleteStorage() throws Exception {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putString(SHARED_PREF_KEY, "");
        // TODO: consider apply() vs commit()
        prefsEditor.commit();
    }

    @Override
    public void closeStorage() throws Exception { }
}
