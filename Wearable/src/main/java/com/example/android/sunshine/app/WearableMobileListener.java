package com.example.android.sunshine.app;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

public class WearableMobileListener extends WearableListenerService {
    public WearableMobileListener() {
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


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        // Loop through the events and send a message back to the node that created the data item.
        for (DataEvent event : dataEvents) {
            DataItem item = event.getDataItem();
            Uri uri = item.getUri();
            String path = uri.getPath();
            if (DATA_WEATHER_REQUEST.equals(path)) {
                // Get the node id of the node that created the data item from the host portion of
                // the uri.
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                mPeerId = uri.getHost();
                // Set the data of the message to be the bytes of the Uri.
                byte[] payload = uri.toString().getBytes();

                Log.d(TAG, "Node request wear " + mPeerId);
                Log.d(TAG, "TempMax: " + dataMap.getLong("max_temp") );

            }
        }

    }


}
