package com.smartwatch;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.smartwatch.utils.Constants;
import com.smartwatch.utils.EventBusInterface;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {
    /**
     * Code used in requesting runtime permissions.
     **/
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final String TAG = "MainActivity";
    private static MainActivity mainActivityRunningInstance;
    protected GoogleApiClient googleApiClient;
    RecyclerView recyclerView;
    TextView tvStatus, tvVersion, tvDataLimit, tvDataStats;
    ImageView imgStatus;
    LinearLayout rootLayout;
    ProgressDialog dialog;
    Button btnSetLocation;

    public static MainActivity getInstance() {
        return mainActivityRunningInstance;
    }

    public static int getPackageUid(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        int uid = -1;
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager
                    .GET_META_DATA);
            uid = packageInfo.applicationInfo.uid;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return uid;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get activity's running instance
        mainActivityRunningInstance = this;

        // Hiding Title bar of this activity screen
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        //Making this activity, full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager
                .LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        dialog = ProgressDialog.show(MainActivity.this, "",
                "Fetching location. Please wait...", true, false);
        initViews();

        // At activity startup we manually check the internet status and change
        // the connectivity status
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context
                .CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            updateView(true);
        } else {
            updateView(false);
        }

        //Request user for required permissions
        if (!isPermissionGranted()) {
            requestPermissions();
        }



        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usm = (UsageStatsManager) this.getSystemService(getApplicationContext().USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(1, time - 100 * 1000, time);
            if (appList != null && appList.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                ActivityManager mActivityManager = (ActivityManager) getApplication().getSystemService(Context.ACTIVITY_SERVICE);
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                    Log.e("Executed app", "Application executed : " + usageStats.getPackageName());
                    try {
                        ApplicationInfo app = this.getPackageManager().getApplicationInfo(usageStats.getPackageName(), 0);
                        if (app.packageName.equals("com.google.android.gm")) {
                            mActivityManager.killBackgroundProcesses(usageStats.getPackageName());
                            Log.e("GOOGLE Executed app", app.packageName + "");
                        }
                        if (app.packageName.equals("com.google.android.apps.docs")) {
                            mActivityManager.killBackgroundProcesses(usageStats.getPackageName());
                            Log.e("GOOGLE Executed app", app.packageName + "");

                        }
                        if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 1) continue;
                        if (usageStats.getPackageName().equals("com.smartwatch")) continue;
                        //Log.e("Executed app", "Application executed : " + usageStats.getPackageName());
                        mActivityManager.killBackgroundProcesses(usageStats.getPackageName());
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    //currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
        } else {
            ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();

        }
    }

    /**
     * Initialize views
     **/
    private void initViews() {
        tvStatus = findViewById(R.id.status);
        tvVersion = findViewById(R.id.version);
        imgStatus = findViewById(R.id.img_status);
        rootLayout = findViewById(R.id.root_layout);
        btnSetLocation = findViewById(R.id.location_btn);
        tvDataLimit = findViewById(R.id.data_limit);
        tvDataStats = findViewById(R.id.data_usage);
        tvVersion.setText("V.3");

        btnSetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Constants.location != null) {
                    Log.e("LOCATION__", Constants.location.getLatitude() + ", " + Constants.location
                            .getLongitude());
                    //set current work location
                    SharedPreferences sharedPreferences = getSharedPreferences("DATA", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("latitude", String.valueOf(Constants.location.getLatitude()));
                    editor.putString("longitude", String.valueOf(Constants.location.getLongitude()));
                    editor.apply();
                }
            }
        });
    }

    public void setStats(String data_limit, String data_stats) {
        tvDataLimit.setText("Data Limit: " + data_limit);
        tvDataStats.setText("Data Usage: " + data_stats + "MBs");
    }

    /**
     * Allows read only access to phone state, including the phone number of the device, IMEI,
     * current
     * cellular network information, the status of any ongoing calls, and a list of any
     * PhoneAccounts
     * registered on the device.
     * Protection level: dangerous
     **/
    public boolean isPermissionGranted() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.e("TAG", "State:" + checkSelfPermission(android.Manifest.permission
                    .READ_PHONE_STATE) + " = " +
                    PackageManager.PERMISSION_GRANTED);

            if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) ==
                    PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest
                    .permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                Log.e("TAG", "Permission is granted");
                return true;
            } else {
                Log.e("TAG", "Permission is revoked");
                requestPermissions();
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.e("TAG", "Permission is granted");
            if (!isGooglePlayServicesAvailable()) {
                Toast.makeText(getApplicationContext(), "No Google Services installed",
                        Toast.LENGTH_LONG).show();
            }
            return true;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Intent serviceIntent = new Intent(this, LocationMonitoringService.class);
        //startService(serviceIntent);

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (!gps_enabled && !network_enabled) {
            //showSettingDialog();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mainActivityRunningInstance = this;
        //mGoogleApiClient.disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mainActivityRunningInstance = this;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getGoogleClient(EventBusInterface googleClient) {
        googleApiClient = googleClient.getGoogleClient();
        dialog.dismiss();
    }

    /**
     * Update UI when network changes
     **/
    public void updateView(boolean isConnected) {
        if (isConnected) {
            tvStatus.setText("Internet Connected");
            rootLayout.setBackgroundColor(getResources().getColor(R.color.connectedColor));
            imgStatus.setImageDrawable(getResources().getDrawable(R.drawable.ic_connected));
        } else {
            tvStatus.setText("Internet Disconnected");
            rootLayout.setBackgroundColor(getResources().getColor(R.color.disconnectedColor));
            imgStatus.setImageDrawable(getResources().getDrawable(R.drawable.ic_disconnected));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //Check whether this user has installed Google play service which is being used by
        // Location updates.
        if (!isGooglePlayServicesAvailable()) {
            Toast.makeText(getApplicationContext(), "No Google Services installed",
                    Toast.LENGTH_LONG).show();
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    /**
     * Return the availability of GooglePlayServices
     */
    public boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(this, status, 2404).show();
            }
            return false;
        }
        return true;
    }

    /**
     * Start permissions requests.
     */
    private void requestPermissions() {

        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        boolean shouldProvideRationale2 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);

        boolean shouldProvideRationale3 =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_PHONE_STATE);


        // Provide an additional rationale to the img_user. This would happen if the img_user
        // denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale || shouldProvideRationale2 || shouldProvideRationale3) {
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{android.Manifest.permission
                                            .ACCESS_FINE_LOCATION, Manifest.permission
                                            .ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the img_user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest
                            .permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.e("TAG", "onRequestPermissionResult");
        if (grantResults.length <= 0) {
            // If img_user interaction was interrupted, the permission request is cancelled
            // and you
            // receive empty arrays.
            Log.e("TAG", "User interaction was cancelled.");
        } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            Log.e("TAG", "Permission granted, updates requested, starting location updates");

            //Start location sharing service to app server.........
            Intent startServiceIntent = new Intent(MainActivity.this, LocationMonitoringService
                    .class);
            startService(startServiceIntent);

        } else {
            // Permission denied.

            // Notify the img_user via a SnackBar that they have rejected a core permission
            // for the
            // app, which makes the Activity useless. In a real app, core permissions would
            // typically be best requested during a welcome-screen flow.

            // Additionally, it is important to remember that a permission might have been
            // rejected without asking the img_user for permission (device policy or "Never ask
            // again" prompts). Therefore, a img_user interface affordance is typically
            // implemented
            // when permissions are denied. Otherwise, your app could appear unresponsive to
            // touches or interactions which have required permissions.
            showSnackbar(R.string.permission_denied_explanation,
                    R.string.settings, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Build intent that displays the App settings screen.
                            Intent intent = new Intent();
                            intent.setAction(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package",
                                    BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    });
        }
    }

}
