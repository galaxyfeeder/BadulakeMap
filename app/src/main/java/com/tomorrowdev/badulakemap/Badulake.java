package com.tomorrowdev.badulakemap;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * gabriel.esteban.gullon@gmail.com, May 2015
 */
public class Badulake extends RealmObject{
    @PrimaryKey
    private long id;
    private String name;
    private double longitude;
    private double latitude;
    private boolean alwaysopened;

    public boolean isAlwaysopened() {
        return alwaysopened;
    }

    public void setAlwaysopened(boolean alwaysopened) {
        this.alwaysopened = alwaysopened;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
