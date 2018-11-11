package com.example.pingapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class PingWidgetProvider extends AppWidgetProvider {

    public static final  String MY_WIDGET_UPDATE = "update";
    private static final String TAG              = "PingWidgetProvider";
    public static final  String EXTRA_HOST_NAME  = "host_name";

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
            ArrayList<String> prefs = new ArrayList<>();
            prefs.add(hostName);
            prefs.add(shortHostName);
            prefs.add(String.valueOf(updateRate));
            updateWidget(context, appWidgetManager, appWidgetId, prefs);
        }
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int
            appWidgetId, ArrayList<String> prefs) {

        String hostName      = prefs.get(0);
        String shortHostName = prefs.get(1);

        RemoteViews views = new RemoteViews(context.getPackageName(),
                                            R.layout.ping_widget);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_HOST_NAME, hostName);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                                                                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_view, pendingIntent);

        views.setTextViewText(R.id.widget_short_host_name, shortHostName);

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
                views.setImageViewResource(R.id.widget_icon, R.drawable.rect_error);
            } else {
                views.setImageViewResource(R.id.widget_icon, R.drawable.rect_ok);
            }
            views.setTextViewText(R.id.widget_responce_time, pingTask.get());
        } catch (ExecutionException e) {
            views.setImageViewResource(R.id.widget_icon, R.drawable.rect_error);
            views.setTextViewText(R.id.widget_responce_time, e.getLocalizedMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            views.setImageViewResource(R.id.widget_icon, R.drawable.rect_error);
            views.setTextViewText(R.id.widget_responce_time, e.getLocalizedMessage());
            e.printStackTrace();
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);

        myAlarmManager.cancel(myPendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (MY_WIDGET_UPDATE.equals(intent.getAction())) {
            Log.d(TAG, "onReceive: "+System.currentTimeMillis());
            Bundle extras = intent.getExtras();
            if (extras != null) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
                                                                PingWidgetProvider.class.getName());
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

                onUpdate(context, appWidgetManager, appWidgetIds);
            }
        }
    }
}
