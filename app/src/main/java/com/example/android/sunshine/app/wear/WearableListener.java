package com.example.android.sunshine.app.wear;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.util.Log;

import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

public class WearableListener extends WearableListenerService {
    public WearableListener() {
    }

    private final String TAG = this.getClass().getSimpleName();
    private String mPeerId;
    private String mShortDesc;
    private String mDate;
    private int mMaxTemp;
    private int mMinTemp;
    private int mWeatherId;


    private static final String DATA_WEATHER_REQUEST= "/data-weather-request";
    GoogleApiClient mGoogleApiClient;
    private static final String[] PROJECTION = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            // This works because the WeatherProvider returns location data joined with
            // weather data, even though they're stored in two different tables.
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
    };

    // These indices are tied to DETAIL_COLUMNS.  If DETAIL_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_WEATHER_CONDITION_ID = 5;



    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
            ConnectionResult connectionResult = mGoogleApiClient
                    .blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "DataLayerListenerService failed to connect to GoogleApiClient, "
                        + "error code: " + connectionResult.getErrorCode());
                return;
            }
        }
        // Loop through the events and send a message back to the node that created the data item.
        for (DataEvent event : dataEvents) {
            Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();
            if (DATA_WEATHER_REQUEST.equals(path)) {
                // Get the node id of the node that created the data item from the host portion of
                // the uri.
                mPeerId = uri.getHost();
                // Set the data of the message to be the bytes of the Uri.
                byte[] payload = uri.toString().getBytes();

                Log.d(TAG, "Node request " + mPeerId);
                setWeatherInfo();
                sendWeatherToWear();
            }
        }



    }

    public void sendWeatherToWear(){

        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(DATA_WEATHER_REQUEST);
        putDataMapReq.getDataMap().putString("date", mDate);
        putDataMapReq.getDataMap().putString("short_desc", mShortDesc);
        putDataMapReq.getDataMap().putInt("weather_id", mWeatherId);
        putDataMapReq.getDataMap().putLong("max_temp", mMaxTemp);
        putDataMapReq.getDataMap().putInt("min_temp", mMinTemp);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.d(TAG,"SaveConfig: " + dataItemResult.getStatus() + ", " + dataItemResult.getDataItem().getUri());
                    }
                });;

    }


    private void setWeatherInfo(){

        String locationQuery = Utility.getPreferredLocation(this);
        long date = System.currentTimeMillis();
        Cursor cursor = getContentResolver().query(
                WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery,date),
                PROJECTION,
                null,
                null,
                WeatherContract.WeatherEntry.COLUMN_DATE + " DESC"
                );

        if (cursor.moveToFirst()){
            do{
                mDate = cursor.getString(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE));
                mShortDesc = cursor.getString(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC));
                mWeatherId = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
                mMaxTemp = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP));
                mMinTemp = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP));
                Log.d(TAG, "Cursor on setWeatherInfo: " + DatabaseUtils.dumpCursorToString(cursor));
            }while(cursor.moveToNext());
        }
        cursor.close();


    }


    @Override
    public void onDestroy() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        super.onDestroy();
    }
}
