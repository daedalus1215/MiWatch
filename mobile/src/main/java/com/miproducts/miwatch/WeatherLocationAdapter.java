package com.miproducts.miwatch;

import android.content.Context;
import android.util.TimeUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.miproducts.miwatch.Container.TimeKeeper;
import com.miproducts.miwatch.Container.WeatherLocation;
import com.miproducts.miwatch.Weather.openweather.ConverterUtil;
import com.miproducts.miwatch.utilities.SettingsManager;
import com.miproducts.miwatch.utilities.TimerFormat;

import java.sql.Time;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for the list view
 * Created by ladam_000 on 8/1/2015.
 */
public class WeatherLocationAdapter extends BaseAdapter{
    private List <WeatherLocation> mLocations;
    private Context mContext;
    private int rowForLocationToInflate;
    private LayoutInflater inflater;
    private SettingsManager mSettingsMananger;

    public WeatherLocationAdapter(List<WeatherLocation> mLocations, Context mContext, int rowForLocationToInflate) {
        this.mLocations = mLocations;
        this.mContext = mContext;
        this.rowForLocationToInflate = rowForLocationToInflate;
        inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // to grab the saved zipcode (selected zipcode)
        mSettingsMananger = new SettingsManager(mContext);
    }

    private void addLocation(WeatherLocation newLocation){
        mLocations.add(newLocation);
    }

    @Override
    public int getCount() {
        return mLocations.size();
    }

    @Override
    public WeatherLocation getItem(int position) {
        return mLocations.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
            //TODO build a viewholder

            // Inflate up
            View rowView = inflater.inflate(rowForLocationToInflate, parent, false);

            // Link up
            TextView tvZipcode = (TextView) rowView.findViewById(R.id.tvZipCodelv);
            TextView tvCity = (TextView) rowView.findViewById(R.id.tvCitylv);
            TextView tvTemp = (TextView) rowView.findViewById(R.id.tvDegreelv);

            // we will do logic here to pick the image
            TextView tvDesc = (TextView) rowView.findViewById(R.id.tvDesclv);
            TextView tvTime = (TextView) rowView.findViewById(R.id.tvTimeStamplv);
            ImageView ivWeather  =  (ImageView) rowView.findViewById(R.id.ivWeatherlv);
            TextView tvSelected = (TextView) rowView.findViewById(R.id.tvCurrentlySelectedlv);

            tvZipcode.setText(mLocations.get(position).getZipcode());
            tvCity.setText(mLocations.get(position).getCity());
            tvTemp.setText(String.valueOf(mLocations.get(position).getTemperature()));
            tvDesc.setText(mLocations.get(position).getDesc());

            // whats currently selected.
        if((mSettingsMananger.getTown().equals(mLocations.get(position).getCity())&& mSettingsMananger.getState().equals(mLocations.get(position).getState()))){
            if(mSettingsMananger.getZipCode().equals(mLocations.get(position).getZipcode())){
                tvSelected.setText("Currently Selected");
            }
        }


            // Format the saved time.
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(mLocations.get(position).getTime_stamp());
            String time = checkForMilitaryTime(cal.getTime().getHours(), cal.getTime().getMinutes());
            tvTime.setText(time);

            // Decide what image to display - based off the description we got back.
            String desc = mLocations.get(position).getDesc();
        //TODO issues here now that I have added in townAndState in the JSON fetching - prob cuz im not doing something there right.
        if(desc != null){
            if(desc.equals("sky is clear")){
                ivWeather.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_sun));
            }
            else if(desc.equals("few clouds")){
                ivWeather.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_cloudy));
            }
            else if(desc.equals("broken clouds")){
                ivWeather.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_cloudy));
            }
            else if (desc.equals("scattered clouds")){
                ivWeather.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_cloudy));
            }
            else if (desc.equals("overcast clouds")){
                ivWeather.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_cloudy));
            }
            else if(desc.equals("light intensity shower rain")){
                ivWeather.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_rain));
            }

        }



            return rowView;
    }

    private String checkForMilitaryTime(int hours, int mins) {
        String AM = "AM";
        String nMin = String.valueOf(mins);

        if(hours >= 12){
            AM = "PM";
        }

        if(hours > 12 ){
            hours = hours - 12;
        }

        if(mins <= 9){
            nMin = "0"+mins;
        }

        return hours + ":" + nMin + AM;
    }
}
