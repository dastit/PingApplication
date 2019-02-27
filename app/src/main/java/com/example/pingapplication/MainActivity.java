package com.example.pingapplication;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.play.core.splitinstall.SplitInstallException;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.Task;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.example.pingapplication.PingFragment.EXTRA_WIDGET_ID;

public class MainActivity extends AppCompatActivity {
    public static final  String       PING_WIDGET = "ping_widget";
    private static final String       TAG         = "MainActivity";
    public               PingFragment pingFragment;

    private SplitInstallManager splitInstallManager;
    private int                 sessionId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FragmentManager fm = getSupportFragmentManager();
        pingFragment = PingFragment.newInstance();
        fm.beginTransaction().add(R.id.fragment_container, pingFragment).commit();

        splitInstallManager = SplitInstallManagerFactory.create(getApplicationContext());

        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();

        if (appLinkIntent.getData() != null) {
            pingFragment.startFromIntent(appLinkIntent);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_new_module, menu);
        Set<String> installedModules = splitInstallManager.getInstalledModules();
        if (installedModules.isEmpty()) {
            return true;
        }
        if (installedModules.contains(PING_WIDGET)) {
            menu.getItem(0).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_add_widget_item:
                installWidgetModule(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void installWidgetModule(final MenuItem item) {
        final SplitInstallRequest request = SplitInstallRequest
                .newBuilder()
                .addModule(PING_WIDGET)
                .build();

        sessionId = splitInstallManager.startInstall(request)
                                       .addOnCompleteListener(
                                               new OnCompleteListener<Integer>() {
                                                   @Override
                                                   public void onComplete(Task<Integer> task) {
                                                       Toast.makeText(MainActivity.this,
                                                                      R.string.module_installation_started_success,
                                                                      Toast
                                                                              .LENGTH_LONG).show();
                                                       item.setVisible(false);
                                                   }
                                               })
                                       .addOnFailureListener(new OnFailureListener() {
                                                                 @Override
                                                                 public void onFailure(Exception e) {
                                                                     switch (((SplitInstallException) e).getErrorCode()) {
                                                                         case SplitInstallErrorCode.NETWORK_ERROR:
                                                                             Toast.makeText(MainActivity
                                                                                                    .this,
                                                                                            getString(
                                                                                                    R.string.no_internet_error),
                                                                                            Toast.LENGTH_LONG).show();
                                                                             break;
                                                                         case SplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED:
                                                                         case SplitInstallErrorCode.INCOMPATIBLE_WITH_EXISTING_SESSION:
                                                                             checkForActiveDownloads(item);
                                                                             break;
                                                                         case SplitInstallErrorCode.MODULE_UNAVAILABLE:
                                                                             Toast.makeText
                                                                                     (MainActivity.this, getString(
                                                                                             R.string.no_module_found_error),
                                                                                      Toast.LENGTH_LONG).show();
                                                                             break;
                                                                         case SplitInstallErrorCode.ACCESS_DENIED:
                                                                             splitInstallManager
                                                                                     .deferredInstall(
                                                                                             Arrays.asList(PING_WIDGET));
                                                                             break;
                                                                         default:
                                                                             Toast.makeText
                                                                                     (MainActivity.this,
                                                                                      e.getLocalizedMessage(),
                                                                                      Toast.LENGTH_LONG).show();
                                                                             break;
                                                                     }
                                                                 }
                                                             }
                                       ).getResult();
    }


    private void checkForActiveDownloads(MenuItem item) {
        List<SplitInstallSessionState> states = splitInstallManager.getSessionStates().getResult();
        for (SplitInstallSessionState state : states) {
            if (state.status() == SplitInstallSessionStatus.DOWNLOADING) {
                splitInstallManager.cancelInstall(sessionId);
                Toast.makeText(this, R.string.has_active_sessions_error, Toast.LENGTH_LONG).show();
            } else {
                onOptionsItemSelected(item);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent != null) {
            pingFragment.startFromIntent(intent);

            final int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1);
            if (widgetId != -1) {
                //update widget
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        Intent updateIntent = new Intent();
                        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                        int[] appWidgetIds = new int[1];
                        appWidgetIds[0] = widgetId;
                        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                        updateIntent.setPackage("com.example.ping_widget");

                        sendBroadcast(updateIntent);

                        return null;
                    }
                }.execute();
            }
        }
    }
}
