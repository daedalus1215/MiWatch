package com.miproducts.miwatch.Weather.openweather;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.miproducts.miwatch.Database.WeatherLocationDbHelper;
import com.miproducts.miwatch.MiDigitalWatchFaceCompanionConfigActivity;
import com.miproducts.miwatch.utilities.Consts;
import com.miproducts.miwatch.utilities.SendToDataLayerThread;
import com.miproducts.miwatch.utilities.SettingsManager;
import com.miproducts.miwatch.Container.WeatherLocation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//TODO check over the work when requested by the watch
//TODO update weather if we do currently have the zipcode in the database.

/**
 * Created by ladam_000 on 8/1/2015.
 */
public class JSONWeatherTask extends AsyncTask<String,Void,String> {
    private static final String TAG = "JSONWeatherTask";

    // max length of a integer to check against
    private static final int MAX_INTEGER_LENGTH = 11;
    // error indicate it is not a proper zipcode
    private static final String ERROR_ZIP = "ZIP_ISSUE";
    // error indicate we are not online
    private static final String ERROR_ONLINE = "ONLINE_ISSUE";
    // error we use if we didnt get a result back from trying to reach out to the weather online.
    private static final int ERROR_URL = -2000;

    private WeatherHttpClient httpClient;
    private SettingsManager mSettingsManager;
    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private WeatherLocation locationToFill;
    private WeatherLocationDbHelper dbHelper;



    private boolean fromActivity = false;
    // keep track if we should change what the last Selected Zipcode is.
    private boolean changeSelected = true;



    /**
     * From ConfigServiceListener
     * @param mContext
     * @param mSettingsManager
     * @param mGoogleApiClient
     */
    public JSONWeatherTask(Context mContext, SettingsManager mSettingsManager, GoogleApiClient mGoogleApiClient){
        Log.d(TAG, "Made");
        this.mSettingsManager = mSettingsManager;
        this.mContext = mContext;
        this.mGoogleApiClient = mGoogleApiClient;
        this.httpClient = new WeatherHttpClient();

        this.locationToFill = new WeatherLocation();
        locationToFill.setZipcode(mSettingsManager.getZipCode());
        locationToFill.setState(mSettingsManager.getState());
        locationToFill.setCity(mSettingsManager.getTown());

        this.dbHelper = new WeatherLocationDbHelper(mContext);
        // indicate we are from a Service and not an Activity (our app is not present on the screen.)
        this.fromActivity = false;
        changeSelected = false;
    }


    /**
     * /**
     * The Weather Location indicates this came fAddWeatherLocation.Activity or MiDigitalWatchFaceComapnaionConfigActivity - not the ConfigListenerService and
     * that the Activity will eventually need it's UI updated - because it will have a new item in it's
     * listview.
     * @param mContext
     * @param mSettingsManager
     * @param mGoogleApiClient
     * @param locationToFill - WeatherLocation
     * @param changeSelected - Should we change what is saved as the default zipcode? true if we want to refresh and just clicked on an item in the ListView, false if we just onCreated and want to update all of the weathers.
     */
    //TODO really need to polish this boolean, we now have 3 booleans determining how we want to handle the search - not good.!
    public JSONWeatherTask(Context mContext, SettingsManager mSettingsManager,
                           GoogleApiClient mGoogleApiClient, WeatherLocation locationToFill,
                           boolean changeSelected){
        Log.d(TAG, "Made");
        this.mSettingsManager = mSettingsManager;
        this.mContext = mContext;
        this.mGoogleApiClient = mGoogleApiClient;
        this.locationToFill = locationToFill;
        this.dbHelper = new WeatherLocationDbHelper(mContext);
        this.httpClient = new WeatherHttpClient();

        // indicate we are from Activity not Service - our app is showing. But this is only generated in addLocation so we are not fromActivity
        this.fromActivity = true;

        this.changeSelected = changeSelected;

    }


    @Override
    protected String doInBackground(String... params) {
        Log.d(TAG, "DO_IN_BACKGROUND BEGINS");

        // check if we are even online
        if(!isOnline())
            return ERROR_ONLINE;

        // check zipcode's integrity
        if(!isZipCodeValid(locationToFill.getZipcode())) return ERROR_ZIP;
        //TODO assume all checks are done on zipcode and states/cities
        sendHttpRequest(locationToFill);

        log("DO_IN_BACKGROUND_ENDS");
        return null;
    }

    private boolean isZipCodeValid(String zipcode) {
        // not a integer if it exceeds MAX_INTER_LENGTH
        if(zipcode.length() >= MAX_INTEGER_LENGTH){
            log("zipcode was not valid");
            return false;
        }
        log("zipcode was valid");
        return true;
    }






    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    protected void onPostExecute(String result) {
        // by default result should return a null - unless there was an issue.
        if(result != null){
            if(result.equals(ERROR_ZIP)){
                Toast.makeText(mContext, "Not proper zipcode!", Toast.LENGTH_LONG).show();

                return;
            }else if(result.equals(ERROR_ONLINE)){
                Toast.makeText(mContext, "Not online!", Toast.LENGTH_LONG).show();

            }

        }
        //TODO TEST!!!@
        if(changeSelected){
            // not if we are coming from AddLocation
            if(mContext instanceof  MiDigitalWatchFaceCompanionConfigActivity){
             ((MiDigitalWatchFaceCompanionConfigActivity) mContext).invalidateAdapter();
            }

        }

        Log.d(TAG, "onPostExecute");
    }

    //TODO clean up
    private int sendHttpRequest(WeatherLocation locationToFill) {

        String result = httpClient.getWeatherData(locationToFill);

        log("city = " + locationToFill.getCity() + " state= " + locationToFill.getState());

        if(resultIsValid(result)){

            int tempInFah = parseTemp(result);

            try{
                //  json result
                JSONObject resultObject = new JSONObject(result);
                JSONArray jsonArrayWeather = resultObject.getJSONArray("weather");

                // description from JSONObject
                JSONObject description = jsonArrayWeather.getJSONObject(0);
                // get Name of town from JSONObject
                String town = resultObject.getString("name");

                locationToFill.setCity(town);
                log("weather pulled out town " + town);
                locationToFill.setTemperature(tempInFah);
                log("weather pulled out temp " + tempInFah);
                locationToFill.setDesc(description.getString("description").replace("proximity", ""));
                log("weather pulled out " + description.getString("description"));
                locationToFill.setTime_stamp(System.currentTimeMillis());


                //log("name of place with jsonArray = " + town);
            }catch(JSONException e){
                log("issue with json = " + e.getMessage());
            }

            if(fromActivity){
                // make sure we don't already have a copy of this location - compare zipcodes and town/state.
                if(!dbHelper.doesLocationExist(locationToFill)){
                    log("add location of + " + locationToFill.getCity());
                    dbHelper.addLocation(locationToFill);// Store into the Database

                }
                    // update the database because we already have this zipcode
                else {
                    log("update location of + " + locationToFill.getCity());

                    dbHelper.updateTemperatureAndTime(locationToFill);}
                // save the new zipcode.
                // make sure we are saving the current selection!
                if(changeSelected){
                    mSettingsManager.saveZipcode(locationToFill.getZipcode());
                    mSettingsManager.saveState(locationToFill.getState());
                    mSettingsManager.saveTown(locationToFill.getCity());
                }
                sendHandledTemperatureToWatch(String.valueOf(locationToFill.getTemperature()));
            }
            // we caem from Config
            else {
                // send out to the dataLayer
                DataMap dataMap = new DataMap();
                // going to continue using the broadcast KEY, it is unique after in DataApi.
                dataMap.putInt(Consts.KEY_BROADCAST_DEGREE, tempInFah);
                // send off to wearable - listener over there will be listening.
                new SendToDataLayerThread(Consts.PHONE_TO_WEARABLE_PATH, dataMap, mGoogleApiClient).start();

                // update the database because we already have this zipcode
                dbHelper.updateTemperatureAndTime(locationToFill);



                // send this to the MainCompanionActivity to send it off to wearable. -
                Intent sendDegreesIntent = new Intent(Consts.BROADCAST_DEGREE);
                sendDegreesIntent.putExtra(Consts.KEY_BROADCAST_DEGREE, tempInFah);
                mContext.sendBroadcast(sendDegreesIntent);
            }

            return tempInFah;
        }else {
            return ERROR_URL;
        }

    }


    private boolean resultIsValid(String result) {
        if(result == null)
            return false;
        log("check validity " + !result.contains("Not found city"));
        //TODO check validity
        // we will get not found city if this was not a accurate zipcode.
        return !result.contains("Not found city");

    }

    private void log(String y) {
        Log.d(TAG, y);
    }


    private int parseTemp(String result) {
        int indexInt = result.indexOf("temp");
        int begOfTempValue = indexInt+6;
        int endOfTempValue = result.indexOf(",", begOfTempValue);
        // Kelvin version
        String temperatureInCelsius = result.substring(begOfTempValue, endOfTempValue);
        // Celsius version
        int tempInCels = (int) ConverterUtil.convertKelvinToCelsius(Double.parseDouble(temperatureInCelsius));
        // Fernheit
        int tempInFah = ConverterUtil.convertCelsiusToFahrenheit(tempInCels);

        return tempInFah;

    }


    private void sendHandledTemperatureToWatch(String temp) {
        // send out to the dataLayer
        DataMap dataMap = new DataMap();
        // going to continue using the broadcast KEY, it is unique after in DataApi.
        dataMap.putInt(Consts.KEY_BROADCAST_DEGREE, Integer.valueOf(temp));
        // send off to wearable - listener over there will be listening.
        new SendToDataLayerThread(Consts.PHONE_TO_WEARABLE_PATH, dataMap, mGoogleApiClient).start();
    }

}