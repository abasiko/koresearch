package com.smartwatch.utils;

import android.app.ActivityManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.loopj.android.http.AsyncHttpClient.LOG_TAG;

public class StopAppService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(LOG_TAG, "In onStartCommand");

      /*  final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningTaskInfo> recentTasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

        for (int i = 0; i < recentTasks.size(); i++) {
            Log.e("Executed app", "Application executed : " + recentTasks.get(i).baseActivity.toShortString()
                    + "\t\t ID: " + recentTasks.get(i).id + "");
        }*/

        String currentApp = "";

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
                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
        } else {
            ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();

        }
        return START_REDELIVER_INTENT;
    }

    public void amKillProcess(String process)
    {
        ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();

        for(ActivityManager.RunningAppProcessInfo runningProcess : runningProcesses)
        {
            if(runningProcess.processName.equals(process))
            {
                android.os.Process.sendSignal(runningProcess.pid, android.os.Process.SIGNAL_KILL);
            }
        }
    }
}
