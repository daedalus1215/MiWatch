package com.miproducts.miwatch;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.miproducts.miwatch.Database.WeatherLocationDbHelper;
import com.miproducts.miwatch.Weather.openweather.ConverterUtil;
import com.miproducts.miwatch.Weather.openweather.JSONWeatherTask;
import com.miproducts.miwatch.Weather.openweather.WeatherHttpClient;
import com.miproducts.miwatch.utilities.Consts;
import com.miproducts.miwatch.utilities.SettingsManager;

import java.util.concurrent.TimeUnit;

/**
 * Responsible for listening for the wearable.
 * Created by ladam_000 on 7/12/2015.
 */
public class ConfigListenerService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener {

    private static final String TAG = "DigitalListenerService";
    private GoogleApiClient mGoogleApi;
    private SettingsManager mSettingsManager;
    private WeatherLocationDbHelper dbHelper;

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected: " + bundle);

    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed: " + result);

    }
    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        log("onDataChanged");
        mSettingsManager = new SettingsManager(getApplicationContext());
        dbHelper = new WeatherLocationDbHelper(getApplicationContext());


        mGoogleApi = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        //TODO if any issues with receiving data this is why. - check and delete toDO
            try{
                getTemp();
            }finally {
                dataEventBuffer.close();
            }
    }
    private void getTemp() {

        JSONWeatherTask task = new JSONWeatherTask(getApplicationContext(), mSettingsManager, mGoogleApi);
        task.execute();
    }

    public void log(String s) {
        Log.d("Phone Service Listener", s);
    }






}


