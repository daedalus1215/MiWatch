package com.miproducts.miwatch;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.miproducts.miwatch.AutoComplete.TestAutoCompleteActivity;
import com.miproducts.miwatch.utilities.CSVReader;
import com.miproducts.miwatch.Database.WeatherLocationDbHelper;
import com.miproducts.miwatch.Weather.openweather.JSONWeatherTask;
import com.miproducts.miwatch.Container.WeatherLocation;
import com.miproducts.miwatch.utilities.SendToDataLayerThread;
import com.google.android.gms.wearable.Wearable;
import com.miproducts.miwatch.utilities.Consts;
import com.miproducts.miwatch.utilities.SettingsManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
public class MiDigitalWatchFaceCompanionConfigActivity extends Activity{
    private static final String TAG = "ConfigActivity";

    private GoogleApiClient mGoogleApiClient;
    private BroadcastReceiver brDegree;
    private SettingsManager mSettingsManager;

    private ListView lvLocations;
    //private ImageButton bAddWeatherLocation;
    //private EditText etZipCode;

    private WeatherLocationAdapter mWeatherLocationAdapter;

    private WeatherLocationDbHelper dbHelper;
    private ImageButton FAB;
    //toolbar stuff
    EditText etSearchPlaces;
    ImageButton ibSearch;

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
/*

        FAB = (ImageButton) findViewById(R.id.imageButton);
        FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // lets go to the new Activity.
                //Intent intentToAddWeatherLocation = new Intent(MiDigitalWatchFaceCompanionConfigActivity.this,AddWeatherLocation.class);
                //TETS
                Intent intentToAddWeatherLocation = new Intent(MiDigitalWatchFaceCompanionConfigActivity.this,TestAutoCompleteActivity.class);

                startActivity(intentToAddWeatherLocation);


            }
        });
*/
        lvLocations = (ListView) findViewById(R.id.lvLocations);
        lvLocations.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getTemp(((WeatherLocationAdapter) lvLocations.getAdapter()).getItem(position), true);
            }
        });
 // TODO CLEAN UP

       updateUI();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        etSearchPlaces = (EditText) toolbar.findViewById(R.id.etWeatherLocation);

        ibSearch = (ImageButton) toolbar.findViewById(R.id.ibSearch);
        ibSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearchPlaces.setVisibility(View.VISIBLE);
                v.setVisibility(View.GONE);
                etSearchPlaces.setShowSoftInputOnFocus(true);
            }
        });


        etSearchPlaces.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKey");

                //TODO this is probably too much work and will kill app!
                // dont want to fetch it if we are doing something else here.
                if (event.getAction() == KeyEvent.KEYCODE_ENTER) {

                    InputMethodManager inputManager = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);

                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);


                    etSearchPlaces.setVisibility(View.GONE);
                    ibSearch.setVisibility(View.VISIBLE);
                    return true;
                }

                return false;
            }
        });

            setActionBar(toolbar);
            //checkIfFirstTimeRunning();
        }

    @Override
    public void onBackPressed() {
        etSearchPlaces.setVisibility(View.GONE);
        ibSearch.setVisibility(View.VISIBLE);
        super.onBackPressed();
    }

    private void checkIfFirstTimeRunning() {
        boolean firstTime = mSettingsManager.getIsUsersFirstTimeRunningApp();

        // user has logged on already
        if(!firstTime) return;

        //TODO Do this in a ASyncTask
        log("firstTime checking if We should load up db");
        String next[] = {};
        List<String[]> list = new ArrayList<String[]>();

        try {
            CSVReader reader = new CSVReader(new InputStreamReader(getAssets().open("citiesstateszipcodes.csv")));
            while(true) {
                next = reader.readNext();
                if(next != null) {

                    list.add(next);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            for(String[] line : list){
                WeatherLocation location = new WeatherLocation();
                // zipcode
                location.setZipcode(line[0]);
                location.setState(line[1]);
                location.setCity(line[2]);
                Log.d(TAG, location.getCity());
                dbHelper.addLocation(location);

            }
            // only do this once.
            mSettingsManager.setIsUsersFirstTimeRunningApp(true);

        }



    }

    public void updateUI() {
        // check if we got any in the DB if so update them
        List<WeatherLocation> weatherLocs = dbHelper.getAllWeatherLocations();
        if(weatherLocs != null){
            for(WeatherLocation weatherLoc : weatherLocs){
                getTemp(weatherLoc, false);
            }
            mWeatherLocationAdapter = new WeatherLocationAdapter(weatherLocs, this, R.layout.view_weather_location);
            lvLocations.setAdapter(mWeatherLocationAdapter);
            invalidateAdapter();
        }
    }


    /**
     * Get the temperature with a JSONWeatherTask
     *
     * @param weatherLocation - built up weather location to get the temperature.
     * @param changeCurrentSelection - whether or not we are changing the current selection - we dont want to change current selection if its the activity firing up.
     */
    private void getTemp(WeatherLocation weatherLocation, boolean changeCurrentSelection) {
        JSONWeatherTask task;
        log("town we saved that we need for url is " + weatherLocation.getCity());

        task = new JSONWeatherTask(this,mSettingsManager,mGoogleApiClient, weatherLocation,changeCurrentSelection);

        task.execute();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initReceivers();
        //updateUI();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateUI();
        invalidateAdapter();
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

    public void invalidateAdapter() {
        mWeatherLocationAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu_layout, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                Toast.makeText(this, "update items", Toast.LENGTH_LONG).show();
                return true;
            case R.id.toolbar:
                Toast.makeText(this, "update items", Toast.LENGTH_LONG).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
