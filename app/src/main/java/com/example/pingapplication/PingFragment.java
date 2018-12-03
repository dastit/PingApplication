package com.example.pingapplication;


import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;

import static android.os.AsyncTask.Status.RUNNING;


public class PingFragment extends Fragment implements PingAsyncTask.TaskDelegate,
                                                      RadioGroup.OnCheckedChangeListener {
    public static final String TAG             = "PingFragment";
    public static final String EXTRA_HOST_NAME = "host_name";

    private TextInputLayout               addressWrapper;
    private TextInputAutoCompleteTextView address;
    private Button                        pingButton;
    private TextView                      connectionResult;
    private ImageButton                   showCommandButton;
    private LinearLayout                  commandContainer;
    private TextView                      commandPingType;
    private EditText                      commandText;

    private String           command;
    private String           params;
    private PingAsyncTask    pingTask;
    private FragmentActivity parentActivity;

    public PingFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.ping_fragment_layout, container, false);

        //Creating adapter for inserted values
        parentActivity = getActivity();

        final HostNameDatabase database = HostNameDatabase.getInstance(
                parentActivity.getApplicationContext());

        ArrayList<String> savedNames = new ArrayList<>();
        for (HostName name : database.hostNameDao().getAll()) {
            savedNames.add(name.getName());
        }
        final RemovableItemArrayAdapter<String> adapter = new RemovableItemArrayAdapter<>(
                parentActivity, R.layout.removable_dropdown_item, R.id.adapter_text, savedNames);

        addressWrapper = v.findViewById(R.id.hostNameWrapper);
        address = v.findViewById(R.id.hostName);
        address.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        address.setKeyListener(DigitsKeyListener.getInstance("0123456789.:"));
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
        address.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                addressWrapper.setErrorEnabled(false);
                setIPV(s.toString());
                commandPingType.setText(command.trim());
            }
        });
        address.setThreshold(1);
        address.setAdapter(adapter);
        pingButton = v.findViewById(R.id.pingButton);
        Button stopButton = v.findViewById(R.id.stopPingButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pingTask != null && pingTask.getStatus().equals(RUNNING)) {
                    pingTask.cancel(false);
                    address.setClickable(true);
                }
            }
        });
        connectionResult = v.findViewById(R.id.connectionResult);
        connectionResult.setMovementMethod(new ScrollingMovementMethod());
        showCommandButton = v.findViewById(R.id.showCommandButton);
        commandContainer = v.findViewById(R.id.pingCommandContainer);
        commandPingType = v.findViewById(R.id.pingType);
        commandPingType.setText(R.string.ping_command_for_ipv4);
        commandText = v.findViewById(R.id.commandText);
        commandText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                params = s.toString();
            }
        });
        showCommandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (commandContainer.getVisibility() == View.GONE) {
                    commandContainer.setVisibility(View.VISIBLE);
                    commandText.setTextColor(commandPingType.getTextColors());
                    showCommandButton.setImageResource(R.drawable.sharp_expand_less_24);
                } else {
                    commandContainer.setVisibility(View.GONE);
                    showCommandButton.setImageResource(R.drawable.sharp_expand_more_24);
                }
            }
        });

        params = "";
        pingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (pingTask != null && pingTask.getStatus().equals(RUNNING)) {
                    return;
                }
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
                    connectionResult.setText(parentActivity.getString(R.string.no_internet_error));
                    return;
                }
                connectionResult.setText("");
                String hostName = address.getText().toString();
                if (hostName.isEmpty()) {
                    addressWrapper.setErrorEnabled(true);
                    addressWrapper.setError(getString(R.string.host_name_error));
                    address.requestFocus();
                    return;
                }

                if (adapter.getPosition(hostName.trim()) == -1) {
                    database.hostNameDao().insertAll(new HostName(hostName));
                    adapter.insert(hostName, adapter.getCount());
                    address.setAdapter(adapter);
                }

                address.clearFocus();
                address.setClickable(false);
                connectionResult.requestFocus();
                commandContainer.setVisibility(View.GONE);
                String finalCommand = command + " " + params + " ";
                pingTask = new PingAsyncTask(PingFragment.this, finalCommand, hostName);
                pingTask.execute();
            }
        });

        RadioGroup  radioGroup      = v.findViewById(R.id.radio_group_name_or_ip);
        RadioButton ipRadioButton   = v.findViewById(R.id.radio_ip);
        RadioButton nameRadioButton = v.findViewById(R.id.radio_name);
        radioGroup.setOnCheckedChangeListener(this);

        //if fragment (Activity) started from widget
        Intent intent   = parentActivity.getIntent();
        String hostName = intent.getStringExtra(EXTRA_HOST_NAME);
        if (hostName != null) {
            //TODO:update widget
            Log.d(TAG, "startWithIntent: hostname=" + hostName);
            setIPV(hostName);
            params = "-c 3 ";
            if (hostName.matches("[0-9:.]+")) {
                ipRadioButton.setChecked(true);
            } else {
                nameRadioButton.setChecked(true);
            }
            address.setText(hostName);
            pingButton.performClick();
            address.clearFocus();
        }
        return v;
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
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.radio_name:
                address.setInputType(InputType.TYPE_TEXT_VARIATION_URI
                                             | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                break;
            case R.id.radio_ip:
                address.setInputType(
                        InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
                address.setKeyListener(DigitsKeyListener.getInstance("0123456789.:"));
                break;
        }
        address.setText("");
        address.requestFocus();
    }
}
