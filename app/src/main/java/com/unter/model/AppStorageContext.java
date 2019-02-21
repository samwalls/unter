package com.unter.model;

public abstract class AppStorageContext<T extends AppDataModel> {

    protected T data;

    public AppStorageContext(T defaultModel) {
        data = defaultModel;
    }

    public abstract void initStorage() throws Exception;

    public abstract void loadStorage() throws Exception;

    public abstract void saveStorage() throws Exception;

    public abstract void deleteStorage() throws Exception;

    public abstract void closeStorage() throws Exception;
}
