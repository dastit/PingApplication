package com.example.pingapplication;


import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import static android.os.AsyncTask.Status.RUNNING;


public class PingFragment extends Fragment implements PingAsyncTask.TaskDelegate {
    public static final String TAG             = "PingFragment";
    public static final String EXTRA_HOST_NAME = "host_name";
    public static final String EXTRA_WIDGET_ID = "widget_id";


    private SpecialFilterAutoCompleteTextView address;
    private Button                            pingButton;
    private TextView                          connectionResult;


    private String           command;
    private String           params;
    private PingAsyncTask    pingTask;
    private FragmentActivity parentActivity;

    public PingFragment() {
    }

    public static PingFragment newInstance() {
        return new PingFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        params = " -c 3 ";
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.ping_fragment_layout, container, false);

        //Creating adapter for inserted values
        parentActivity = getActivity();

        final HostNameDatabase database;
        if (parentActivity != null) {
            database = HostNameDatabase.getInstance(
                    parentActivity.getApplicationContext());

            ArrayList<String> savedNames = new ArrayList<>();
            for (HostName name : database.hostNameDao().getAll()) {
                savedNames.add(name.getName());
            }

            final RemovableItemArrayAdapter<String> adapter = new RemovableItemArrayAdapter<String>(
                    parentActivity, R.layout.removable_dropdown_item, R.id.adapter_text,
                    savedNames) {
                @Override
                public void onClick(View v) {
                    CharSequence chosenAddress        = ((TextView) v).getText();
                    String       stringInAddressField = address.getText().toString();
                    int indexOfSuggestion = stringInAddressField.lastIndexOf(
                            " ");
                    String stringInAddressFieldWithoutSuggestion = stringInAddressField;
                    if (indexOfSuggestion >= 0) {
                        stringInAddressFieldWithoutSuggestion = stringInAddressField.substring(
                                0, indexOfSuggestion);
                    }

                    address.setTextKeepState(
                            String.format("%s %s", stringInAddressFieldWithoutSuggestion,
                                          chosenAddress), TextView.BufferType.EDITABLE);
                    address.setSelection(address.getText().length());
                    address.dismissDropDown();
                }
            };

            address = v.findViewById(R.id.hostname);
            address.setText(params, TextView.BufferType.EDITABLE);
            address.setImeOptions(EditorInfo.IME_ACTION_DONE);
            address.setMaxLines(2);

            final InputMethodManager imm = (InputMethodManager) parentActivity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            address.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (imm != null) {
                        if (hasFocus) {
                            imm.showSoftInput(v, 0);
                        }
                    }
                }
            });
            address.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        pingButton.performClick();
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        }
                        return true;
                    }
                    return false;
                }
            });

            address.setAdapter(adapter);

            connectionResult = v.findViewById(R.id.connectionResult);
            connectionResult.setMovementMethod(new ScrollingMovementMethod());

            pingButton = v.findViewById(R.id.pingButton);
            pingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    //if ping task is still running, stop running
                    if (pingTask != null && pingTask.getStatus().equals(RUNNING)) {
                        pingTask.cancel(false);
                        address.setClickable(true);
                        return;
                    }

                    //check if internet is connected
                    ConnectivityManager cm =
                            (ConnectivityManager) parentActivity.getSystemService(
                                    Context.CONNECTIVITY_SERVICE);

                    NetworkInfo activeNetwork = null;
                    if (cm != null) {
                        activeNetwork = cm.getActiveNetworkInfo();
                    }
                    boolean isConnected = activeNetwork != null && activeNetwork
                            .isConnected();
                    if (!isConnected) {
                        connectionResult.setText(
                                parentActivity.getString(R.string.no_internet_error));
                        return;
                    }

                    //clear previous ping task results
                    connectionResult.setText("");

                    //add new value to host names suggestion list
                    String stringInAddressField = address.getText().toString();
                    String hostname             = stringInAddressField;
                    int    indexOf_             = stringInAddressField.lastIndexOf(" ");
                    if (indexOf_ != -1) {
                        hostname = stringInAddressField.substring(indexOf_).trim();
                    }

                    if (!hostname.equals("") && adapter.getPosition(hostname) == -1
                            && database.hostNameDao().get(hostname) == null) {
                        database.hostNameDao().insertAll(new HostName(hostname));
                        adapter.insert(hostname, adapter.getCount());
                        adapter.notifyDataSetChanged();
                        address.setAdapter(adapter);
                    }

                    //start pinging
                    address.clearFocus();
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    connectionResult.requestFocus();
                    setIPV(hostname);
                    String finalCommand = String.format("%s ", command);
                    pingTask = new PingAsyncTask(PingFragment.this, finalCommand,
                                                 stringInAddressField);
                    pingTask.execute();

                    changeButtonName(false);
                }
            });

            Intent intent = getActivity().getIntent();
            if (intent != null && intent.getExtras() != null) {
                startFromIntent(intent);
            }
        }
        return v;
    }

    public void startFromIntent(Intent intent) {
        String hostName = "";
        if (intent.getAction() != null && Intent.ACTION_VIEW.equals(
                intent.getAction())) {
            if (intent.getData() != null && intent.getData().getLastPathSegment() != null) {
                hostName = intent.getData().getLastPathSegment();
            }
        } else {
            hostName = intent.getStringExtra(EXTRA_HOST_NAME);
        }
        if (hostName!=null && !hostName.equals("")) {
            Log.d(TAG, "startWithIntent: hostname=" + hostName);
            setIPV(hostName);
            params = "-c 1 ";
            address.setText(String.format("%s%s", params, hostName));
            pingButton.performClick();
            address.clearFocus();
        }
    }


    private void setIPV(String hostName) {
        if (hostName.contains(":")) {
            command = getString(R.string.ping_command_for_ipv6);
        } else {
            command = getString(R.string.ping_command_for_ipv4);
        }
    }

    @Override
    public void fillConnectionResult(String text) {
        connectionResult.append(text);
    }

    @Override
    public void changeButtonName(boolean taskIsFinished) {
        if (taskIsFinished) {
            pingButton.setText(R.string.ping_button_start_name);
        } else {
            pingButton.setText(R.string.ping_button_stop_name);
        }
    }
}
