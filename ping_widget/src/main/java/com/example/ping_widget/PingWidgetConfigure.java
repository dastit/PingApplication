package com.example.ping_widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.example.pingapplication.TextInputAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

public class PingWidgetConfigure extends Activity {
    private static final String                        TAG = "PingWidgetConfigure";
    private              TextInputLayout               hostNameWrapper;
    private              TextInputAutoCompleteTextView hostName;
    private              TextInputLayout               shortHostNameWrapper;
    private              TextInputAutoCompleteTextView shortHostName;
    private              TextInputLayout               updateRateWrapper;
    private              TextInputEditText             updateRate;
    private              Spinner                       updateRateDim;
    private              Button                        autoStartButton;

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private String mHostName;
    private String mShortHostName;
    private long   mUpdateRate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(com.example.pingapplication.R.layout.widget_configure);

        hostNameWrapper = findViewById(com.example.pingapplication.R.id.wconfig_hostname_wrapper);
        hostName = findViewById(com.example.pingapplication.R.id.wconfig_hostName);
        hostName.requestFocus();

        shortHostNameWrapper = findViewById(
                com.example.pingapplication.R.id.wconfig_short_hostname_wrapper);
        shortHostName = findViewById(com.example.pingapplication.R.id.wconfig_short_hostName);

        updateRateWrapper = findViewById(
                com.example.pingapplication.R.id.wconfig_update_rate_wrapper);
        updateRate = findViewById(com.example.pingapplication.R.id.wconfig_update_rate);
        updateRateDim = findViewById(
                com.example.pingapplication.R.id.wconfig_update_rate_dimension);
        updateRateDim.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0)//not an hour
                {
                    updateRateWrapper.setError(getString(R.string.frequent_update_warning));
                } else {
                    updateRateWrapper.setError("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        autoStartButton = findViewById(com.example.pingapplication.R.id.autostart_button);
        autoStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAutoStartup();
            }
        });

        Button saveButton = findViewById(com.example.pingapplication.R.id.wconfig_save_button);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                                                                             com.example.pingapplication.R.array.update_rate_dims,
                                                                             android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        updateRateDim.setAdapter(adapter);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isCorrect()) {
                    return;
                }

                Context           context          = PingWidgetConfigure.this;
                AppWidgetManager  appWidgetManager = AppWidgetManager.getInstance(context);
                ArrayList<String> prefs            = new ArrayList<>();
                prefs.add(mHostName);
                prefs.add(mShortHostName);
                prefs.add(String.valueOf(mUpdateRate));

                savePrefs(PingWidgetConfigure.this, prefs, mAppWidgetId);
                PingWidgetProvider.updateWidget(context, appWidgetManager, mAppWidgetId, prefs);

//                Intent intent = new Intent(PingWidgetConfigure.this, PingWidgetProvider.class);
////                intent.setAction(MY_WIDGET_UPDATE_ACTION);
////                intent.putExtra(EXTRA_WIDGET_IDS_FOR_UPDATE, mAppWidgetId);
//                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
//                int [] appWidgetIds = new int []{mAppWidgetId};
//                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
//                PendingIntent pendingIntent = PendingIntent.getBroadcast(PingWidgetConfigure.this,
//                                                                         0, intent, 0);
//                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//                Calendar     calendar     = Calendar.getInstance();
//                // calendar.setTimeInMillis(System.currentTimeMillis());
//                if (alarmManager != null) {
//                    alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                                              SystemClock.elapsedRealtime() + mUpdateRate,
//                                              mUpdateRate, pendingIntent);
//                }
//
//                PingWidgetProvider.saveAlarmManager(alarmManager, pendingIntent);

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
    }

    private boolean isCorrect() {
        mHostName = hostName.getText().toString();
        if (mHostName.isEmpty()) {
            hostNameWrapper.setError(
                    getString(com.example.pingapplication.R.string.host_name_error));
            return false;
        }
        mShortHostName = shortHostName.getText().toString();
        if (mShortHostName.isEmpty()) {
            mShortHostName = mHostName;
            shortHostName.setText(mHostName);
        }
        if (mShortHostName.length() > 10) {
            shortHostNameWrapper.setError(getString(R.string.host_name_too_long_widget_error));
            return false;
        }
        try {
            mUpdateRate = Long.valueOf(updateRate.getText().toString()) * 1000;
            if (updateRateDim.getSelectedItemPosition() == 0) //hour
            {
                mUpdateRate = mUpdateRate * 60 * 60;
            } else if (updateRateDim.getSelectedItemPosition() == 1) //minutes
            {
                mUpdateRate = mUpdateRate * 60;
            }
        } catch (NumberFormatException | NullPointerException e) {
            updateRateWrapper.setError(getString(R.string.wrong_update_rate_error));
            return false;
        }
        return true;
    }

    static void savePrefs(Context context, ArrayList<String> prefs, int widgetId) {
        SharedPreferences.Editor editor = context.getSharedPreferences(
                context.getString(com.example.pingapplication.R.string.preferences_name),
                MODE_PRIVATE).edit();
        editor.putString(context.getString(com.example.pingapplication.R.string.sp_name) + widgetId,
                         prefs.get(0));
        editor.putString(
                context.getString(com.example.pingapplication.R.string.sp_short_name) + widgetId,
                prefs.get(1));
        editor.putLong(context.getString(com.example.pingapplication.R.string.sp_rate) + widgetId,
                       Long.parseLong(prefs.get(2)));
        editor.apply();
    }

    static String loadHostName(Context context, int widgetId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(com.example.pingapplication.R.string.preferences_name),
                MODE_PRIVATE);
        return sharedPreferences.getString(
                context.getString(com.example.pingapplication.R.string.sp_name) + widgetId, "");
    }

    static long loadUpdateRate(Context context, int widgetId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(com.example.pingapplication.R.string.preferences_name),
                MODE_PRIVATE);
        return sharedPreferences.getLong(
                context.getString(com.example.pingapplication.R.string.sp_rate) + widgetId,
                86400000);
    }

    static String loadShortHostName(Context context, int widgetId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(com.example.pingapplication.R.string.preferences_name),
                MODE_PRIVATE);
        return sharedPreferences.getString(
                context.getString(com.example.pingapplication.R.string.sp_short_name) + widgetId,
                "N/A");
    }

    static void deletePrefs(Context context, int widgetId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(com.example.pingapplication.R.string.preferences_name),
                MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(context.getString(com.example.pingapplication.R.string.sp_short_name) +
                              widgetId);
        editor.remove(context.getString(com.example.pingapplication.R.string.sp_rate) + widgetId);
        editor.remove(context.getString(com.example.pingapplication.R.string.sp_name) + widgetId);
        editor.apply();
    }

    private void addAutoStartup() {

        try {
            Intent intent       = new Intent();
            String manufacturer = android.os.Build.MANUFACTURER;
            if ("xiaomi".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.miui.securitycenter",
                                                      "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            } else if ("oppo".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.coloros.safecenter",
                                                      "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if ("vivo".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.vivo.permissionmanager",
                                                      "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            } else if ("Letv".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.letv.android.letvsafe",
                                                      "com.letv.android.letvsafe.AutobootManageActivity"));
            } else if ("Honor".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.huawei.systemmanager",
                                                      "com.huawei.systemmanager.optimize.process.ProtectActivity"));
            }

            List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
                                                                               PackageManager.MATCH_DEFAULT_ONLY);
            if (list.size() > 0) {
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e(TAG, String.valueOf(e));
        }
    }
}
