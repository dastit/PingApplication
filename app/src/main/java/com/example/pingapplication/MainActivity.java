package com.example.pingapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

public class MainActivity extends AppCompatActivity {
    private static final String TAG         = "MainActivity";
    public static final  String PING_WIDGET = "ping_widget";

    public PingFragment pingFragment;

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
        pingFragment.startFromIntent(intent);
    }
}
