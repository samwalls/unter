package com.unter.model;

import java.io.Serializable;

public abstract class Data implements Serializable {

    public Data(String id) {
        this.id = id;
    }

    public String id;
}
