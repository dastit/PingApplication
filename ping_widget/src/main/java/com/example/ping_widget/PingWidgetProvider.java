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

public class PingWidgetProvider extends AppWidgetProvider {

    public static final  String MY_WIDGET_UPDATE = "update";
    private static final String TAG              = "PingWidgetProvider";

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
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork
                .isConnected();

        RemoteViews views = new RemoteViews(context.getPackageName(),
                                            com.example.pingapplication.R.layout.ping_widget);
        if (isConnected) {
            String hostName      = prefs.get(0);
            String shortHostName = prefs.get(1);

            //Sets opening Main Activity on click on widget
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.putExtra(EXTRA_HOST_NAME, hostName);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                                                                    PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(com.example.pingapplication.R.id.widget_view,
                                          pendingIntent);
            Calendar calendar = Calendar.getInstance();

            views.setTextViewText(com.example.pingapplication.R.id.widget_short_host_name,
                                  shortHostName + " (" + calendar.get(
                                          Calendar.HOUR) + ":" + calendar
                                          .get(Calendar.MINUTE) + ")");

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
                                      pingTask.get());
            } catch (ExecutionException e) {
                views.setImageViewResource(com.example.pingapplication.R.id.widget_icon,
                                           com.example.pingapplication.R.drawable.rect_error);
                views.setTextViewText(com.example.pingapplication.R.id.widget_responce_time,
                                      e.getLocalizedMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                views.setImageViewResource(com.example.pingapplication.R.id.widget_icon,
                                           com.example.pingapplication.R.drawable.rect_error);
                views.setTextViewText(com.example.pingapplication.R.id.widget_responce_time,
                                      e.getLocalizedMessage());
                e.printStackTrace();
            }
        } else {
            views.setImageViewResource(com.example.pingapplication.R.id.widget_icon,
                                       com.example.pingapplication.R.drawable.rect_no_connection);
            views.setTextViewText(com.example.pingapplication.R.id.widget_responce_time,
                                  "n/a");
        }
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        if (myPendingIntent != null) {
            myAlarmManager.cancel(myPendingIntent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (MY_WIDGET_UPDATE.equals(intent.getAction())) {
            Log.d(TAG, "onReceive: " + System.currentTimeMillis());
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
