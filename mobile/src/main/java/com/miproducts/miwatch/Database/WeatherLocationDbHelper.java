package com.miproducts.miwatch.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.miproducts.miwatch.Container.WeatherLocation;
import com.miproducts.miwatch.utilities.SettingsManager;

import java.security.Policy;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ladam_000 on 8/2/2015.
 */
public class WeatherLocationDbHelper  extends SQLiteOpenHelper{
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "weather_location";
    private static final String TAG = "DbHelper";


    public WeatherLocationDbHelper(Context context){
       super(context, DATABASE_NAME , null, DATABASE_VERSION);
   }

//TODO lets try nothing, i mean we are already setting to nothing up further in the chain, lets just save it such.
   public void addLocation(WeatherLocation wLocation){
        log("ADD_LOCATION_START");
        // grab a copy of the database.
        SQLiteDatabase database = this.getWritableDatabase();

        // lets stuff values for the row
        ContentValues insertableRow = new ContentValues();
        // ZIPCODE
        insertableRow.put(DbContractor.WeatherLoc.COLUMN_ZIPCODE, String.valueOf(wLocation.getZipcode()));
        // CITY
        insertableRow.put(DbContractor.WeatherLoc.COLUMN_CITY, wLocation.getCity());
        // TEMP
        insertableRow.put(DbContractor.WeatherLoc.COLUMN_TEMPERATURE, String.valueOf(wLocation.getTemperature()));
        // DESC
        insertableRow.put(DbContractor.WeatherLoc.COLUMN_DESC, wLocation.getDesc());
        // TIME_STAMP
        insertableRow.put(DbContractor.WeatherLoc.COLUMN_TIME, wLocation.getTime_stamp());
        // STATE
        insertableRow.put(DbContractor.WeatherLoc.COLUMN_STATE, wLocation.getState());
        log("addLocation state = " + wLocation.getState());

        float id = database.insert(DbContractor.WeatherLoc.TABLE_NAME, null, insertableRow);
        Log.d("DBHelper", "id of added item = " + id);
        database.close();
        log("ADD_LOCATION_DONE");
    }

    // Getting All Contacts
    //TODO lets grab desc and timestamp

    public List<WeatherLocation> getAllWeatherLocations() {
        log("GET_ALL_WEATHER_LOCATION_BEGINS:");
        List<WeatherLocation> weatherLocations = new ArrayList<WeatherLocation>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + DbContractor.WeatherLoc.TABLE_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {

                WeatherLocation weatherLocation = new WeatherLocation();
                // weather
                weatherLocation.setTemperature(Integer.parseInt(cursor.getString(0)));
                log("temp " + cursor.getString(0));
                // city
                weatherLocation.setCity(cursor.getString(1));
                log("city " + cursor.getString(1));
                // zipcode
                weatherLocation.setZipcode(cursor.getString(2));
                log("zipcode " + cursor.getString(2));
                // desc
                weatherLocation.setDesc(cursor.getString(3));
                log("desc " + cursor.getString(3));
                // time_stamp
                weatherLocation.setTime_stamp(cursor.getLong(4));
                log("milliseconds " + cursor.getLong(4));

                // state
                weatherLocation.setState(cursor.getString(5));
                log("state2 " + cursor.getString(5));


                // Adding Weather Location to list
                weatherLocations.add(weatherLocation);
            } while (cursor.moveToNext());
        }
        log("GET_ALL_WEATHER_LOCATIONS_DONE:");
        if(weatherLocations.size() <= 0){
            weatherLocations = null;
        }
        // return contact list
        return weatherLocations;
    }


    public boolean doesLocationExist(WeatherLocation wLocation){
        log("DOES_LOCATION_EXIST");
        List<WeatherLocation> allLocations = getAllWeatherLocations();
        if(allLocations != null)
            for(WeatherLocation loc : allLocations){
                log("compareWeatherLocation in DoesLocationExist");
                if(wLocation.equals(loc)){
                    log("doesLocationExist : TRUE");
                    return true;

                }
            }
        log("doesLocationExist : FALSE");

        return false;
    }

    private void log(String msg) {
        Log.d(TAG, msg);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DbContractor.SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + DbContractor.WeatherLoc.TABLE_NAME);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }


    //TODO letsm ake sure we update the time stamp and description

    /**
     *
     * @param locationToFill - location should be stuffed with what it needs.
     */
    public void updateTemperatureAndTime(WeatherLocation locationToFill) {
        log("updateTemperature");

        SQLiteDatabase db = this.getWritableDatabase();

        // Select All Query
        String selectQuery = "SELECT  * FROM " + DbContractor.WeatherLoc.TABLE_NAME;

        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                WeatherLocation weatherLocation = new WeatherLocation();

                // weather
                weatherLocation.setTemperature(Integer.parseInt(cursor.getString(0)));
                log("temp " + cursor.getString(0));
                // city
                weatherLocation.setCity(cursor.getString(1));
                log("city " + cursor.getString(1));
                // zipcode
                weatherLocation.setZipcode(cursor.getString(2));
                log("zipcode " + cursor.getString(2));
                // desc
                weatherLocation.setDesc(cursor.getString(3));
                // time_stamp
                weatherLocation.setTime_stamp(cursor.getLong(4));
                //state
                weatherLocation.setState(cursor.getString(5));
                log("state3 " + cursor.getString(5));

                if(weatherLocation.equals(locationToFill)){
                    ContentValues cv = new ContentValues();
                    // save temp
                    cv.put(DbContractor.WeatherLoc.COLUMN_TEMPERATURE,
                            locationToFill.getTemperature());
                    // save time
                    cv.put(DbContractor.WeatherLoc.COLUMN_TIME, locationToFill.getTime_stamp());
                    // save desc
                    cv.put(DbContractor.WeatherLoc.COLUMN_DESC, locationToFill.getDesc());

                    int result;


                    // no zipcode it must be a state/town
                    if(locationToFill.getZipcode() == SettingsManager.NOTHING_SAVED){
                        result = db.update(DbContractor.WeatherLoc.TABLE_NAME,
                                cv,
                                DbContractor.WeatherLoc.COLUMN_STATE + " = ? AND " + DbContractor.WeatherLoc.COLUMN_CITY + " = ?",
                                new String[]{locationToFill.getState(), locationToFill.getCity()});
                    }else {
                        result = db.update(DbContractor.WeatherLoc.TABLE_NAME,
                                cv,
                                DbContractor.WeatherLoc.COLUMN_ZIPCODE + " = ?",
                                new String[]{locationToFill.getZipcode()});
                    }

                    log("updated db, # rows effected = " + result);
                }

            } while (cursor.moveToNext());
        }
        db.close();
        cursor.close();

    }




}
