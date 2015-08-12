package com.miproducts.miwatch.Container;

import android.util.Log;

/**
 * This will store
 * 1. zipcode of a location the user wanted to find the weather of.
 * 2. State and city of a location the user wanted to find the weather of.
 * 3. last temperature the user retrieved from a weather fetch.
 * Created by ladam_000 on 8/1/2015.
 */
public class WeatherLocation {
    private static final String TAG = "WeatherLocation";
    private int temperature;
    private String zipcode;
    private String city;
    private String desc;



    private String state;
    private long time_stamp;




    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setTime_stamp(long time_stamp) {
        this.time_stamp = time_stamp;
    }



    public long getTime_stamp() {
        return time_stamp;
    }

    public String getDesc() {
        return desc;
    }



    public WeatherLocation() {

    }

    public int getTemperature() {
        return temperature;
    }

    public String getZipcode() {
        return zipcode;
    }


    public String getCity() {
        return city;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }


    public void setCity(String city) {
        this.city = city;
    }

    public WeatherLocation(int temperature, String zipcode, String city) {
        this.temperature = temperature;
        this.zipcode = zipcode;
        this.city = city;
    }

    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        WeatherLocation wObject = (WeatherLocation) o;
        // city
        if(!getCity().equals(wObject.getCity())){
            log("WeatherLocations dont equal");
            return false;
        }
        // state
        if(!getState().equals(wObject.getState()))
        {
            log("WeatherLocations dont equal");
            return false;
        }
        // zipcode
        if(!getZipcode().equals(wObject.getZipcode()))
        {
            log("WeatherLocations dont equal");
            return false;
        }

        Log.d(TAG, "they equal");
        log("first Object state " +getState() );
        log("first Object town " +getCity() );
        log("first Object zip " + getZipcode() );

        log("2nd Object state " +wObject.getState() );
        log("2nd Object town " +wObject.getCity() );
        log("2nd Object zip " + wObject.getZipcode() );
        return true;
    }

    private void log(String s) {
       Log.d(TAG, s);
    }
}
