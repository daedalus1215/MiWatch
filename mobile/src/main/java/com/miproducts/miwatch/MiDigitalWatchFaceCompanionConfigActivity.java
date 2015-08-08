package com.miproducts.miwatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.miproducts.miwatch.Database.WeatherLocationDbHelper;
import com.miproducts.miwatch.Weather.openweather.JSONWeatherTask;
import com.miproducts.miwatch.Container.WeatherLocation;
import com.miproducts.miwatch.utilities.SendToDataLayerThread;
import com.google.android.gms.wearable.Wearable;
import com.miproducts.miwatch.utilities.Consts;
import com.miproducts.miwatch.utilities.SettingsManager;

import java.util.List;
/**
 * 1. We will allow user to input the zipcode of the area he wants weather.
 * 2. If we get back a result, else tell him not valid address, we will Build a View and put it into the ListView. - maybe refresh
 * 3. Display City, State, Zipcode, and last updated weather. - save in MySQL
 * 4. allow user to select one of those views in listview and update the weather/set it as default for
 * wearable.
 * 5.
 * Created by larry on 7/2/15.
 */
public class MiDigitalWatchFaceCompanionConfigActivity extends Activity {
    private static final String TAG = "ConfigActivity";

    private GoogleApiClient mGoogleApiClient;
    private BroadcastReceiver brDegree;
    private SettingsManager mSettingsManager;

    private ListView lvLocations;
    private ImageButton bAddWeatherLocation;
    private EditText etZipCode;

    private WeatherLocationAdapter mWeatherLocationAdapter;

    private WeatherLocationDbHelper dbHelper;
    private ImageButton FAB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        mSettingsManager = new SettingsManager(this);
        dbHelper = new WeatherLocationDbHelper(this);

       mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        // notified by ConfigListener that temp is requested. - not used anymore - was for degrees on watch to instigate temop change
        brDegree = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                log("brDegree - Temperature is = " + intent.getIntExtra(Consts.KEY_BROADCAST_DEGREE, 0));
                int temp = intent.getIntExtra(Consts.KEY_BROADCAST_DEGREE, 0); //TODO constants these
                DataMap dataMap = new DataMap();
                // going to continue using the broadcast KEY, it is unique after in DataApi.
                dataMap.putInt(Consts.KEY_BROADCAST_DEGREE, temp);
                // send off to wearable - listener over there will be listening.
                //svMenu.sendOutDataToWearable(dataMap);
                new SendToDataLayerThread(Consts.PHONE_TO_WEARABLE_PATH, dataMap, mGoogleApiClient).start();
            }
        };

        etZipCode = (EditText) findViewById(R.id.etZipcode);

        if(!mSettingsManager.getZipCode().equals(SettingsManager.NOTHING_SAVED))
            etZipCode.setText(mSettingsManager.getZipCode());
/*
        //TODO handle done according, close keyboard - handle bAddWeather to close keyboard as well.
        bAddWeatherLocation = (ImageButton) findViewById(R.id.bAddWeatherLocation);
        bAddWeatherLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save this zipcode as the one we want to fetch for for the phone - the most active one.

                //TODO have preferences save whether or not this view is currently selected.

                WeatherLocation weatherLocation = new WeatherLocation();
                weatherLocation.setZipcode(etZipCode.getText().toString());
                getTemp(weatherLocation);
            }
        });
        */

        FAB = (ImageButton) findViewById(R.id.imageButton);
        FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Toast.makeText(MiDigitalWatchFaceCompanionConfigActivity.this, "Hello Worl", Toast.LENGTH_SHORT).show();
                WeatherLocation weatherLocation = new WeatherLocation();
                weatherLocation.setZipcode(etZipCode.getText().toString());
                getTemp(weatherLocation);


            }
        });

        lvLocations = (ListView) findViewById(R.id.lvLocations);
        updateUI();
        lvLocations.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getTemp(((WeatherLocationAdapter) lvLocations.getAdapter()).getItem(position));
            }
        });
        //TODO make sure no issue if user doesnt already have weatherLocations saved/
        /*
        // check if we got any in the DB if so update them
        List<WeatherLocation> weatherLocs = dbHelper.getAllWeatherLocations();
        for(WeatherLocation weatherLoc : weatherLocs){
            new JSONWeatherTask(this, mSettingsManager, mGoogleApiClient, weatherLoc,false).execute();
        }*/
    }

    private void addToDatabase(WeatherLocation weatherLocation) {
        dbHelper.addLocation(weatherLocation);
        updateUI();

    }


    public void updateUI() {
        List<WeatherLocation> savedList = dbHelper.getAllWeatherLocations();
        //savedList.add(new WeatherLocation(72, "04005", "Biddeford"));
        if(savedList.size() > 0){
            mWeatherLocationAdapter = new WeatherLocationAdapter(savedList, this, R.layout.view_weather_location);
            lvLocations.setAdapter(mWeatherLocationAdapter);
        }
    }





    private void getTemp(WeatherLocation weatherLocation) {
        JSONWeatherTask task = new JSONWeatherTask(this,mSettingsManager,mGoogleApiClient, weatherLocation,true);
        task.execute();

    }

    @Override
    protected void onStart() {
        super.onStart();
        initReceivers();
    }


    private void initReceivers() {
        registerReceiver(brDegree, new IntentFilter(Consts.BROADCAST_DEGREE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(brDegree);
    }

    private void log(String s) {
        Log.d(TAG, s);

    }
}
