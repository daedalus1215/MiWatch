package com.miproducts.miwatch;

import android.app.Activity;
import android.media.audiofx.BassBoost;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.miproducts.miwatch.AutoComplete.CustomAutoCompleteTextView;
import com.miproducts.miwatch.Container.WeatherLocation;
import com.miproducts.miwatch.Database.WeatherLocationDbHelper;
import com.miproducts.miwatch.Weather.openweather.JSONWeatherTask;
import com.miproducts.miwatch.utilities.CSVReader;
import com.miproducts.miwatch.utilities.SettingsManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by larry on 8/10/15.
 */
public class AddWeatherLocation extends Activity{

    private static final String TAG = "AddWeatherLocation";
    /* Link to the View objects */
    private ImageButton ibBack;
    private ImageButton ibCurrentLocation;
    private AutoCompleteTextView etSearchPlaces;
    private SettingsManager mSettingsManager;
    private GoogleApiClient mGoogleApiClient;
   // List<WeatherLocation> list;
    //WeatherLocationDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_weather_location);

        mSettingsManager = new SettingsManager(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        initViewObjects();
        //dbHelper = new WeatherLocationDbHelper(this);
        //list = dbHelper.getAllWeatherLocations();
       // parseCityAndStateCSV();
    }
/**
    private void parseCityAndStateCSV() {
        String next[] = {};
       // list = new ArrayList<WeatherLocation>();



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

            for(int i = 0; i < list.size(); i++){
                Log.d(TAG, list.get(i)[2].toString());
            }

            List<HashMap<String, String>> aList = new ArrayList<HashMap<String, String>>();

            for(int i = 0; i < list.size(); i++){
                HashMap<String, String> townState = new HashMap<String, String>();
                townState.put(list.get(i)[2],list.get(i)[1]);
                aList.add(townState);
            }

            CustomAutoCompleteTextView customTextView = (CustomAutoCompleteTextView) findViewById(R.id.atSearchPlaces);

            String[] from = {"town", "state"};
            int[] to = {R.id.tvTown, R.id.tvState};

            SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.auto_complete_text_view, from, to);

            //etSearchPlaces.addTextChangedListener(new );
            customTextView.setAdapter(adapter);

        }

    }
*/

    /* Initialize View Objects */
    private void initViewObjects() {
        ibBack = (ImageButton) findViewById(R.id.ibBack);
        ibCurrentLocation = (ImageButton) findViewById(R.id.ibCurrentLocation);
        etSearchPlaces = (AutoCompleteTextView) findViewById(R.id.atSearchPlaces);
        attachClickHandlers();
    }
    /* Attach Handlers to View Objects */
    private void attachClickHandlers() {

        ibBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // end This Activity
                finish();
            }
        });

        ibCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO need a better check for whether this is an Integer or String.
                // CHECK IF IT HAS DIGITS
                if (etSearchPlaces.getText().toString().contains("0")) {
                    WeatherLocation weatherLocation = new WeatherLocation();
                    weatherLocation.setZipcode(etSearchPlaces.getText().toString());

                    // Nothing for these 2, because we are fetching with zipcode.
                    weatherLocation.setState(SettingsManager.NOTHING_SAVED);
                    weatherLocation.setCity((SettingsManager.NOTHING_SAVED));

                    JSONWeatherTask task = new JSONWeatherTask(getApplicationContext(), mSettingsManager, mGoogleApiClient,weatherLocation,false);
                    task.execute();

                }
                //TODO key no matter what is we are saving the NOTHING_SAVED if we dont have a value to put in there.
                // came from town and state
                else {
                    WeatherLocation weatherLocation = new WeatherLocation();
                    int comma = etSearchPlaces.getText().toString().indexOf(",");
                    // make sure user actually inputs town,state format
                    if(comma == -1)
                    {
                        Toast.makeText(getApplication(), "Please 'town,state' format", Toast.LENGTH_LONG).show();
                        return;
                    }
                    // isolate the town and state.
                    String town = etSearchPlaces.getText().toString().substring(0, comma);
                    town = town.replace(" ", "+");
                    String state = etSearchPlaces.getText().toString().substring(comma + 1, etSearchPlaces.length());
                    // remove the space.
                    state = state.replace(" ","");

                    // northing saved for zipcode
                    weatherLocation.setZipcode(SettingsManager.NOTHING_SAVED);


                    weatherLocation.setCity(town);
                    weatherLocation.setState(state);

                    log("town = " + town);
                    log("state = " + state);
                    JSONWeatherTask task = new JSONWeatherTask(getApplicationContext(), mSettingsManager, mGoogleApiClient, weatherLocation, true);
                    task.execute();
                }
            }
        });
    }

    private void log(String s) {
        Log.d(TAG, s);
    }


}
/*
        etSearchPlaces.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "onKey");

                //TODO this is probably too much work and will kill app!
                // dont want to fetch it if we are doing something else here.
                if (event.getAction() != KeyEvent.KEYCODE_DEL || event.getAction() != KeyEvent.KEYCODE_ENTER) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i)[2].contains(etSearchPlaces.getText().toString())){
                            etSearchPlaces.setText(list.get(i)[2].toString() + ", " + list.get(i)[1]);
                        }
                    }
                }

                return false;
            }
        });
 */
