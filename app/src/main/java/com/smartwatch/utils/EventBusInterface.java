package com.smartwatch.utils;

import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by steve on 6/3/18.
 */

public class EventBusInterface {
    GoogleApiClient mLocationClient;

    public EventBusInterface(GoogleApiClient mLocationClient) {
        this.mLocationClient = mLocationClient;
    }

    public GoogleApiClient getGoogleClient() {
        return mLocationClient;
    }
}
