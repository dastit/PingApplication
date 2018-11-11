package com.example.pingapplication;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Calendar;

public class PingWidgetConfigure extends Activity {
    private TextInputLayout               hostNameWrapper;
    private TextInputAutoCompleteTextView hostName;
    private TextInputLayout               shortHostNameWrapper;
    private TextInputAutoCompleteTextView shortHostName;
    private TextInputLayout               updateRateWrapper;
    private TextInputEditText             updateRate;
    private Spinner                       updateRateDim;
    private Button                        saveButton;

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private String mHostName;
    private String mShortHostName;
    private int    mUpdateRate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.widget_configure);

        hostNameWrapper = findViewById(R.id.wconfig_hostname_wrapper);
        hostName = findViewById(R.id.wconfig_hostName);
        shortHostNameWrapper = findViewById(R.id.wconfig_short_hostname_wrapper);
        shortHostName = findViewById(R.id.wconfig_short_hostName);

        updateRateWrapper = findViewById(R.id.wconfig_update_rate_wrapper);
        updateRate = findViewById(R.id.wconfig_update_rate);
        updateRateDim = findViewById(R.id.wconfig_update_rate_dimension);
        saveButton = findViewById(R.id.wconfig_save_button);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                                                                             R.array.update_rate_dims,
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

                Intent intent = new Intent(PingWidgetConfigure.this, PingWidgetProvider.class);
                intent.setAction(PingWidgetProvider.MY_WIDGET_UPDATE);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(PingWidgetConfigure.this,
                                                                         0, intent, 0);
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                Calendar     calendar     = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                if (alarmManager != null) {
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), mUpdateRate, pendingIntent);
                }

                PingWidgetProvider.saveAlarmManager(alarmManager, pendingIntent);

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
            hostNameWrapper.setError(getString(R.string.host_name_error));
            return false;
        }
        mShortHostName = shortHostName.getText().toString();
        if (mShortHostName.isEmpty()) {
            shortHostNameWrapper.setError("Укажите краткий заголовок для виджета");
            return false;
        }
        if (mShortHostName.length() > 10) {
            shortHostNameWrapper.setError("Слишком длинный заголовок, влезает не более " +
                                                  "10 символов");
            return false;
        }
        try {
            mUpdateRate = Integer.valueOf(updateRate.getText().toString()) * 1000;
            if (updateRateDim.getSelectedItemPosition() == 0) {
                mUpdateRate = mUpdateRate * 60 * 60;
            } else if (updateRateDim.getSelectedItemPosition() == 1) {
                mUpdateRate = mUpdateRate * 60;
            }
        } catch (NumberFormatException | NullPointerException e) {
            updateRateWrapper.setError("Поле должно содержать числовое значение");
            return false;
        }
        return true;
    }

    static void savePrefs(Context context, ArrayList<String> prefs, int widgetId) {
        SharedPreferences.Editor editor = context.getSharedPreferences(
                context.getString(R.string.preferences_name),
                MODE_PRIVATE).edit();
        editor.putString(context.getString(R.string.sp_name) + widgetId, prefs.get(0));
        editor.putString(context.getString(R.string.sp_short_name) + widgetId, prefs.get(1));
        editor.putInt(context.getString(R.string.sp_rate) + widgetId,
                      Integer.parseInt(prefs.get(2)));
        editor.apply();
    }

    static String loadHostName(Context context, int widgetId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.preferences_name),
                MODE_PRIVATE);
        return sharedPreferences.getString(
                context.getString(R.string.sp_name) + widgetId, "");
    }

    static int loadUpdateRate(Context context, int widgetId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.preferences_name),
                MODE_PRIVATE);
        return sharedPreferences.getInt(
                context.getString(R.string.sp_rate) + widgetId, 86400000);
    }

    static String loadShortHostName(Context context, int widgetId) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.preferences_name),
                MODE_PRIVATE);
        return sharedPreferences.getString(
                context.getString(R.string.sp_short_name) + widgetId, "N/A");
    }
}
