package com.example.ping_widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.pingapplication.MainActivity;
import com.example.pingapplication.PingAsyncTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import static com.example.pingapplication.PingFragment.EXTRA_HOST_NAME;
import static com.example.pingapplication.PingFragment.EXTRA_WIDGET_ID;
import static com.example.pingapplication.PingFragment.MY_WIDGET_UPDATE_ACTION;
import static com.example.pingapplication.PingFragment.EXTRA_WIDGET_IDS_FOR_UPDATE;

public class PingWidgetProvider extends AppWidgetProvider {

    private static final String TAG                   = "PingWidgetProvider";

    static AlarmManager  myAlarmManager;
    static PendingIntent myPendingIntent;

    public static void saveAlarmManager(AlarmManager alarmManager, PendingIntent pendingIntent) {
        myAlarmManager = alarmManager;
        myPendingIntent = pendingIntent;
    }


    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            String hostName = PingWidgetConfigure.loadHostName(context,
                                                               appWidgetId);
            int updateRate = PingWidgetConfigure.loadUpdateRate(context,
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
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = null;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
        }
        boolean isConnected = activeNetwork != null && activeNetwork
                .isConnected();

        RemoteViews views = new RemoteViews(context.getPackageName(),
                                            com.example.pingapplication.R.layout.ping_widget);
        if (isConnected) {
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

            String finalCommand;
            if (hostName.contains(":")) {
                finalCommand = "/system/bin/ping6 -c 1 ";
            } else {
                finalCommand = "/system/bin/ping -c 1 ";
            }

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
                                      String.format("%s (%s:%s)", pingTask.get(),
                                                    addZero(calendar.get(Calendar.HOUR_OF_DAY)),
                                                    addZero(calendar.get(Calendar.MINUTE))));
            } catch (ExecutionException e) {
                views.setImageViewResource(com.example.pingapplication.R.id.widget_icon,
                                           com.example.pingapplication.R.drawable.rect_error);
                views.setTextViewText(com.example.pingapplication.R.id.widget_responce_time,
                                      e.getLocalizedMessage());
                Log.e(TAG, "updateWidget: ExecutionException", e);;
            } catch (InterruptedException e) {
                views.setImageViewResource(com.example.pingapplication.R.id.widget_icon,
                                           com.example.pingapplication.R.drawable.rect_error);
                views.setTextViewText(com.example.pingapplication.R.id.widget_responce_time,
                                      e.getLocalizedMessage());
                Log.e(TAG, "updateWidget: InterruptedException", e);;
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        if (myPendingIntent != null) {
            myAlarmManager.cancel(myPendingIntent);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        //delete update alarm
        if (myPendingIntent != null) {
            myAlarmManager.cancel(myPendingIntent);
        }

        //delete saved information from SharedPreferences
        for (int appWidgetId : appWidgetIds) {
            PingWidgetConfigure.deletePrefs(context, appWidgetId);
        }
    }

    //TODO: add saving and deleting ids in shared prefs, delete old ids from phon

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (MY_WIDGET_UPDATE_ACTION.equals(intent.getAction())) {
            Log.d(TAG, "onReceive: " + System.currentTimeMillis());
            Bundle extras = intent.getExtras();
            if (extras != null) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
                                                                PingWidgetProvider.class.getName());
                //int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
                int[] appWidgetIds = new int[1];
                appWidgetIds[0] = extras.getInt(EXTRA_WIDGET_IDS_FOR_UPDATE);
                onUpdate(context, appWidgetManager, appWidgetIds);
            }
        }
    }

    private static String addZero(int value) {
        String result = String.valueOf(value);
        if (value < 10) {
            result = "0" + result;
        }
        return result;
    }
}
