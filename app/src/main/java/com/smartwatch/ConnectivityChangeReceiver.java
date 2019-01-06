package com.smartwatch;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.usage.NetworkStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.smartwatch.utils.Constants;
import com.smartwatch.utils.NetworkStatsHelper;
import com.smartwatch.utils.NotifiationUtils;
import com.smartwatch.utils.StopAppService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * <p>
 * A broadcast receiver (receiver) is an Android component which allows you to register for system
 * or application events. All registered receivers for an event are notified by the Android runtime
 * once this event happens.
 */

public class ConnectivityChangeReceiver extends BroadcastReceiver {
    private static boolean firstConnect = true;
    SharedPreferences sharedPreferences;
    // Tag used to cancel the request
    String tag_string_req = "string_req", dataUsed = "";
    Context context;
    NetworkStatsHelper networkStatsHelper;
    long startTimeInMillis;
    // Create the Handler
    private Handler handler = new Handler();
    // Define the code block to be executed
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //check for data usage threshold
            long runningDataStats = networkStatsHelper.getAllTxBytesMobile(context,
                    startTimeInMillis)
                    + networkStatsHelper.getAllTxBytesWifi(context, startTimeInMillis);
            Double runningDataStatsInMBs = ((double) runningDataStats / 1000000);
            Float setDataLimit = sharedPreferences.getFloat("data_limit", 0);

            Log.e("monitoring____", runningDataStatsInMBs + "/" + setDataLimit);
            // Repeat every 1 seconds
            handler.postDelayed(runnable, 1000);
            if (MainActivity.getInstance() != null) {//only if view is visible
                MainActivity.getInstance().setStats(sharedPreferences.getFloat
                        ("data_limit", 0) + " MBs", String.format("%.2f",
                        runningDataStatsInMBs));
            }
            if (runningDataStatsInMBs > setDataLimit) {

                if (Constants.isWorkPlace) {

                    dataUsed = runningDataStatsInMBs.toString();

                    NotifiationUtils.showNotification(context, "Data Limit Warning!",
                            "You have reached your data limit of " + sharedPreferences.getFloat
                                    ("data_limit", 0) + " MBs", 1);
                    //start service to stop running applications
                    Intent i = new Intent(context, StopAppService.class);
                    context.startService(i);

                    initiateNetworkRequest();
                    handler.removeCallbacks(runnable);
                }
            }

        }
    };

    /**
     * This method is called when the BroadcastReceiver is receiving an Intent broadcast.
     **/
    @SuppressLint("DefaultLocale")
    @Override
    public void onReceive(final Context context, final Intent intent) {
        this.context = context;
        //initialize SharedPreferences for persistence of data
        sharedPreferences = context.getSharedPreferences("ACCOUNT", context.MODE_PRIVATE);

        //Start location sharing service to app server.........
        Intent startServiceIntent = new Intent(context, LocationMonitoringService.class);
        context.startService(startServiceIntent);

        //check for network connectivity
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        final android.net.NetworkInfo wifi = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        final android.net.NetworkInfo mobile = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        //check for permissions to for tracking application operation
        //if MODE is default (MODE_DEFAULT), extra permission checking is needed
        boolean granted = false;
        AppOpsManager appOps = (AppOpsManager) context
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            granted = (context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }
        long dataInBytes = 0;
        double dataInMBs = 0.0;
        startTimeInMillis = getStartTimeInMillis();
        if (granted) {
            NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService
                    (Context.NETWORK_STATS_SERVICE);
            networkStatsHelper = new NetworkStatsHelper(networkStatsManager);

        } else {
            context.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }

        if (networkStatsHelper != null) {
            if (wifi.isConnected()) {
                Constants.connectionType = "Wifi";
                if (firstConnect) {
                    firstConnect = false;
                    updateViewOnConnectivityStatus(true);
                    Log.e("network_______", "connected");
                    dataInBytes = networkStatsHelper.getAllTxBytesWifi(context,
                            startTimeInMillis);

                }
            } else if (mobile.isConnected()) {
                Constants.connectionType = "Mobile data";
                if (firstConnect) {
                    // do subroutines here
                    firstConnect = false;
                    updateViewOnConnectivityStatus(true);
                    Log.e("network_______", "connected");
                    dataInBytes = networkStatsHelper.getAllTxBytesMobile(context,
                            startTimeInMillis);
                }
            } else {
                Constants.connectionType = "";
                firstConnect = true;
                Log.e("network_______", "disconnected");
                updateViewOnConnectivityStatus(false);
            }
            Log.e("WORKPLACE", String.valueOf(Constants.isWorkPlace) + "/" + Constants.connectionType);
            startNetworkRequestCommands();
            if (Constants.isWorkPlace) {
                dataInMBs = ((double) dataInBytes / 1000000);
                dataUsed = String.format("%.2f",
                        dataInMBs);
                Log.e("track____", "long: " + dataInBytes + ", dec:" + String.format("%.2f",
                        dataInMBs));
                startNetworkRequestCommands();
            }
        }

    }

    private long getStartTimeInMillis() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault
                ());//dd/MM/yyyy
        long timeInMilliseconds = 0;
        try {
            Date mDate = sdfDate.parse(getCurrentTime());
            timeInMilliseconds = mDate.getTime();
            System.out.println("Date in milli :: " + timeInMilliseconds);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return timeInMilliseconds;
    }

    private void initiateNetworkRequest() {
        if (Constants.connectionType.equalsIgnoreCase("Wifi")) {
            updateViewOnConnectivityStatus(true);
            Log.e("network_______1", "connected");
            networkRequest();
        } else if (Constants.connectionType.equalsIgnoreCase("Mobile data")) {
            updateViewOnConnectivityStatus(true);
            networkRequest();
            Log.e("network_______2", "connected");
        } else {
            Log.e("network_______3", "disconnected");
            updateViewOnConnectivityStatus(false);
            saveOfflineDateToPreferences();
        }

    }

    public void startNetworkRequestCommands() {
        fetchDataLimitNetworkRequest();
        fetchMaximumLocationLimitNetworkRequest();
    }

    /**
     * Update UI when network changes
     **/
    private void updateViewOnConnectivityStatus(final boolean isConnected) {
        if (MainActivity.getInstance() != null) {//only if view is visible
            MainActivity.getInstance().updateView(isConnected);
        }

    }

    private void fetchDataLimitNetworkRequest() {
        final String url = context.getResources().getString(R.string.base_url) + "data_limit.php";
        StringRequest strReq = new StringRequest(Request.Method.GET,
                url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("data_limit_", response);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putFloat("data_limit", Float.parseFloat(response));
                editor.apply();
                // Start the Runnable immediately
                handler.post(runnable);
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("sync__", error.toString());
                fetchDataLimitNetworkRequest();
                fetchMaximumLocationLimitNetworkRequest();
            }
        });
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void fetchMaximumLocationLimitNetworkRequest() {
        final String url = context.getResources().getString(R.string.base_url) + "location_limit.php";
        StringRequest strReq = new StringRequest(Request.Method.GET,
                url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("location_limit_", response);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("location_limit", Integer.valueOf(response));
                editor.apply();
                // Start the Runnable immediately
                handler.post(runnable);
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("sync__", error.toString());
                fetchDataLimitNetworkRequest();
                fetchMaximumLocationLimitNetworkRequest();
            }
        });
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Making the request using volley library. Volley library uses a singleton
     * to make network requests.
     * Requires @params: imei_code, active_time, last_active_time, type
     **/

    private void networkRequest() {
        final String url = context.getResources().getString(R.string.base_url) + "connected.php";
        StringRequest strReq = new StringRequest(Request.Method.POST,
                url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e("sync__", response);
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("sync__", error.toString());
                //networkRequest();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                if (getDeviceIMEI(context) != null) {
                    params.put("imei_code", getDeviceIMEI(context));
                } else {
                    params.put("imei_code", "0000000000000000");
                }
                params.put("active_time", getCurrentTime());
                params.put("last_active_time", getLastActiveTime());
                params.put("type", Constants.connectionType);
                params.put("data_used", dataUsed);

                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    /**
     * Using SharedPreferences to save last active time
     **/
    private void saveOfflineDateToPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("last_active_time", getCurrentTime());
        editor.apply();
    }

    /**
     * Fetch last active time from SharedPreferences
     **/
    private String getLastActiveTime() {
        return sharedPreferences.getString("last_active_time", getCurrentTime());
    }

    /**
     * Method to return device IMEI. This requires explicit permissions that need to be granted from
     * Android 6.0 and greater.
     **/
    @SuppressLint({"MissingPermission", "HardwareIds"})
    public String getDeviceIMEI(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context
                .TELEPHONY_SERVICE);
        assert telephonyManager != null;
        return telephonyManager.getDeviceId();
    }

    /**
     * Method to return the current date time using SimpleDateFormat
     **/
    public String getCurrentTime() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault
                ());//dd/MM/yyyy
        Date now = new Date();
        return sdfDate.format(now);
    }
}
