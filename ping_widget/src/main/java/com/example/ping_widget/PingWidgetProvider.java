package com.example.ping_widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.pingapplication.MainActivity;
import com.example.pingapplication.PingAsyncTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static android.content.Context.ALARM_SERVICE;
import static com.example.pingapplication.PingFragment.EXTRA_HOST_NAME;
import static com.example.pingapplication.PingFragment.EXTRA_WIDGET_ID;

public class PingWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "PingWidgetProvider";

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            String hostName = PingWidgetConfigure.loadHostName(context,
                                                               appWidgetId);
            long updateRate = PingWidgetConfigure.loadUpdateRate(context,
                                                                 appWidgetId);
            String shortHostName = PingWidgetConfigure.loadShortHostName(context,
                                                                         appWidgetId);

            if (!hostName.equals("") && !shortHostName.equals("")) {
                ArrayList<String> prefs = new ArrayList<>();
                prefs.add(hostName);
                prefs.add(shortHostName);
                prefs.add(String.valueOf(updateRate));
                updateWidget(context, appWidgetManager, appWidgetId, prefs);
            }
        }
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int
            appWidgetId, ArrayList<String> prefs) {
        long interval = Long.parseLong(prefs.get(2));

        setNextUpdateAlarm(context, appWidgetId, interval);

        RemoteViews views = new RemoteViews(context.getPackageName(),
                                            com.example.pingapplication.R.layout.ping_widget);
        if (isConnected(context)) {
            String hostName      = prefs.get(0);
            String shortHostName = prefs.get(1);

            //Sets opening Main Activity on click on widget
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(EXTRA_HOST_NAME, hostName);
            intent.putExtra(EXTRA_WIDGET_ID, appWidgetId);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent,
                                                                    PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(com.example.pingapplication.R.id.widget_view,
                                          pendingIntent);

            Calendar calendar = Calendar.getInstance();
            views.setTextViewText(com.example.pingapplication.R.id.widget_short_host_name,
                                  shortHostName);
            String finalCommand = getFinalCommandString(hostName);

            String time = addZero(calendar.get(Calendar.HOUR_OF_DAY))
                    + ":"
                    + addZero(calendar.get(Calendar.MINUTE));
            String date = addZero(calendar.get(Calendar.DATE))
                    + "."
                    + addZero(calendar.get(Calendar.MONTH))
                    + "."
                    + addZero(calendar.get(Calendar.YEAR));

            //TODO: delete log writing before release
            long intervalInMinutes = interval / 1000 / 60;
            appendLog(
                    String.format(Locale.getDefault(), "Widget '%s' #%d updated at %s %s " +
                                          "(interval = %d minutes)",
                                  shortHostName, appWidgetId,
                                  time, date, intervalInMinutes), context);
            //

            PingAsyncTask pingTask = new PingAsyncTask(null, finalCommand, hostName);
            pingTask.execute();
            try {
                if (pingTask.get().equals(PingAsyncTask.NO_CONNECTION)) {
                    views.setImageViewResource(com.example.pingapplication.R.id.widget_icon,
                                               com.example.pingapplication.R.drawable.rect_error);
                } else {
                    views.setImageViewResource(com.example.pingapplication.R.id.widget_icon,
                                               com.example.pingapplication.R.drawable.rect_ok);
                }
                views.setTextViewText(com.example.pingapplication.R.id.widget_responce_time,
                                      String.format("%s", pingTask.get()));
            } catch (ExecutionException e) {
                views.setImageViewResource(com.example.pingapplication.R.id.widget_icon,
                                           com.example.pingapplication.R.drawable.rect_error);
                views.setTextViewText(com.example.pingapplication.R.id.widget_responce_time,
                                      e.getLocalizedMessage());
                Log.e(TAG, "updateWidget: ExecutionException", e);

            } catch (InterruptedException e) {
                views.setImageViewResource(com.example.pingapplication.R.id.widget_icon,
                                           com.example.pingapplication.R.drawable.rect_error);
                views.setTextViewText(com.example.pingapplication.R.id.widget_responce_time,
                                      e.getLocalizedMessage());
                Log.e(TAG, "updateWidget: InterruptedException", e);
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @NonNull
    private static String getFinalCommandString(String hostName) {
        String finalCommand;
        if (hostName.contains(":")) {
            finalCommand = "/system/bin/ping6 -c 1 ";
        } else {
            finalCommand = "/system/bin/ping -c 1 ";
        }
        return finalCommand;
    }

    private static boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = null;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
        }
        return activeNetwork != null && activeNetwork
                .isConnected();
    }

    private static void setNextUpdateAlarm(Context context, int appWidgetId, long interval) {
        PendingIntent pendingIntent = getAlarmPendingIntent(context, appWidgetId);
        AlarmManager  alarmManager  = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, SystemClock
                        .elapsedRealtime() + interval, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                                   SystemClock.elapsedRealtime() + interval,
                                   pendingIntent);
            }
        }
    }

    private static PendingIntent getAlarmPendingIntent(Context context, int appWidgetId) {
        Intent updateIntent = new Intent(context, PingWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] appWidgetIds = new int[]{appWidgetId};
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        return PendingIntent.getBroadcast(context, appWidgetId,
                                                     updateIntent,
                                                     PendingIntent.FLAG_UPDATE_CURRENT);
    }


    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        PendingIntent pendingIntent = getAlarmPendingIntent(context, appWidgetIds[0]);
        if (pendingIntent != null && alarmManager!=null) {
            alarmManager.cancel(pendingIntent);
        }

        //delete saved information from SharedPreferences
        for (int appWidgetId : appWidgetIds) {
            PingWidgetConfigure.deletePrefs(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);

    }

    private static String addZero(int value) {
        String result = String.valueOf(value);
        if (value < 10) {
            result = "0" + result;
        }
        return result;
    }

    public static void appendLog(String text, Context context) {
        File logFile = new File(context.getApplicationContext().getFilesDir(), "log.file");

        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
