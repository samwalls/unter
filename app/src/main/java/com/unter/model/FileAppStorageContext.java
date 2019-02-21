package com.unter.model;

import android.net.Uri;

import java.io.*;

public class FileAppStorageContext<T extends AppDataModel> extends AppStorageContext<T> {

    private static final String fileUrl = "unter_cache";

    private File file;

    private final String storeDirectory;

    public FileAppStorageContext(String storeDirectory, T defaultModel) {
        super(defaultModel);
        this.storeDirectory = storeDirectory;
    }

    @Override
    public void initStorage() throws IOException, ClassNotFoundException {
        String name = Uri.parse(fileUrl).getLastPathSegment();
        file = new File(storeDirectory, name);

        // create the file if it doesn't exist
        if (file.createNewFile()) {
            file.setReadable(true);
            file.setWritable(true);
            file.setExecutable(false);
        }

        if (file.length() <= 0) {
            // write initialised structures to the file
            writeAppModelToFile();
        }

        // initialise the app data
        loadStorage();
    }

    @Override
    public void loadStorage() throws IOException, ClassNotFoundException {
        data = readAppDataModelFromFile();
    }

    @Override
    public void saveStorage() throws IOException {
        writeAppModelToFile();
    }

    @Override
    public void deleteStorage() throws IOException {
        if (!file.delete())
            throw new IOException("could not delete storage at local app file: " + fileUrl);
    }

    @Override
    public void closeStorage() throws IOException {
        writeAppModelToFile();
    }

    private T readAppDataModelFromFile() throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);

        //noinspection unchecked
        return (T) ois.readObject();
    }

    private void writeAppModelToFile() throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(data);
    }
}
