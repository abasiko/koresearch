package com.smartwatch;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.smartwatch.utils.Constants;
import com.smartwatch.utils.EventBusInterface;
import com.smartwatch.utils.NotifiationUtils;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Kevin on 5/15/18.
 */

public class LocationMonitoringService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private static final String TAG = LocationMonitoringService.class.getSimpleName();
    GoogleApiClient mLocationClient;
    LocationRequest mLocationRequest = new LocationRequest();
    SharedPreferences sharedPreferences, locationSharedPreference;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sharedPreferences = getSharedPreferences("DATA", MODE_PRIVATE);
        locationSharedPreference = getSharedPreferences("ACCOUNT", MODE_PRIVATE);

        buildGoogleClient();

        mLocationRequest.setInterval(Constants.LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(Constants.FASTEST_LOCATION_INTERVAL);
        //minimum distance between updates.
        mLocationRequest.setSmallestDisplacement(0.1f);


        int priority = LocationRequest.PRIORITY_HIGH_ACCURACY; //by default
        //PRIORITY_BALANCED_POWER_ACCURACY, PRIORITY_LOW_POWER, PRIORITY_NO_POWER are the other
        // priority modes

        mLocationRequest.setPriority(priority);
        mLocationClient.connect();
        Log.e("service___", "started");

        //Make it stick to the notification panel so it is less prone to get cancelled by the
        // Operating System.
        return START_STICKY;
    }

    private void buildGoogleClient() {
        mLocationClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        if (!mLocationClient.isConnected()) {
            mLocationClient.connect();
            NotifiationUtils.showNotification(getApplicationContext(), "Turn On Your Location",
                    "Go to Settings > Location", 0);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
     * LOCATION CALLBACKS
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Log.d(TAG, "== Error On onConnected() Permission not granted");
            //Permission not granted by user so cancel the further execution.

            return;
        }
        if (mLocationClient.isConnected()) {
            //set event bus to register events when location client is connected
            //request updates of location
            EventBus.getDefault().post(new EventBusInterface(mLocationClient));
            LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient,
                    mLocationRequest, this);
            Log.d(TAG, "Connected to Google API");
        } else {
            buildGoogleClient();
        }

    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended");
    }

    //to get the location change
    @Override
    public void onLocationChanged(Location location) {
        Log.e(TAG, "Location changed");
        if (location != null) {
            Log.e(TAG, "== location != null" + location.getLatitude());
            Constants.location = location;
            EventBus.getDefault().post(new EventBusInterface(mLocationClient));
            calculateDistanceFromLastLocation(location);
        }
    }

    /*
     * Calculate the distance between the current location and the
     * location client set as work place
     */
    private void calculateDistanceFromLastLocation(Location location) {

        double latitude = Double.parseDouble(sharedPreferences.getString("latitude", "0"));
        double longitude = Double.parseDouble(sharedPreferences.getString("longitude", "0"));
        float distance = 0;
        //saved location
        Location savedLocation = new Location("saved_location");
        savedLocation.setLatitude(latitude);
        savedLocation.setLongitude(longitude);
        //new location the client is at
        Location newLocation = new Location("new_location");
        newLocation.setLatitude(location.getLatitude());
        newLocation.setLongitude(location.getLongitude());

        //distance in meters between the two points. Assume 50m is out of range of a work place.
        distance = (float) Math.round(savedLocation.distanceTo(newLocation));//in meters
        if (distance > 50) {

            Log.d("Location X",  String.valueOf(locationSharedPreference.getInt("location_limit", 0)));

            Constants.isWorkPlace = false;
            NotifiationUtils.showNotification(getApplicationContext(), "Location Changed",
                    "Current Distance " + distance
                            + " M from " +
                            "Work" +
                            " Place", 0);
        } else {
            Log.d("Location X",  String.valueOf(locationSharedPreference.getInt("location_limit", 0)));

            Constants.isWorkPlace = true;
            NotifiationUtils.showNotification(getApplicationContext(), "Location Changed",
                    "Currently at Work Place", 0);
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Failed to connect to Google API");

    }


}