package com.example.pingapplication;


import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
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
import android.widget.ArrayAdapter;
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
    private Button                        stopButton;
    private TextView                      connectionResult;
    private ImageButton                   showCommandButton;
    private LinearLayout                  commandContainer;
    private TextView                      commandPingType;
    private EditText                      commandText;
    private RadioGroup                    radioGroup;
    private RadioButton                   ipRadioButton;
    private RadioButton                   nameRadioButton;

    private String        command;
    private String        params;
    private PingAsyncTask pingTask;

    public PingFragment() {
    }

    public static PingFragment newInstance() {
        PingFragment fragment = new PingFragment();
        Bundle       args     = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.ping_fragment_layout, container, false);

        //Creating adapter for inserted values
        final HostNameDatabase database = Room.databaseBuilder(getActivity(),
                                                               HostNameDatabase.class,
                                                               "Host_name_database")
                                              .allowMainThreadQueries().build();
        ArrayList<String> savedNames = new ArrayList<>();
        for (HostName name : database.hostNameDao().getAll()) {
            savedNames.add(name.getName());
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                                                                android.R.layout
                                                                        .simple_dropdown_item_1line,
                                                                savedNames);

        addressWrapper = v.findViewById(R.id.hostNameWrapper);
        address = v.findViewById(R.id.hostName);
        address.setInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        address.setKeyListener(DigitsKeyListener.getInstance("0123456789.:"));
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
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
        stopButton = v.findViewById(R.id.stopPingButton);
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
                        (ConnectivityManager) getActivity().getSystemService(
                                Context.CONNECTIVITY_SERVICE);

                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null && activeNetwork
                        .isConnected();
                if (!isConnected) {
                    connectionResult.setText(getActivity().getString(R.string.no_internet_error));
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

        radioGroup = v.findViewById(R.id.radio_group_name_or_ip);
        ipRadioButton = v.findViewById(R.id.radio_ip);
        nameRadioButton = v.findViewById(R.id.radio_name);
        radioGroup.setOnCheckedChangeListener(this);

        Intent intent   = getActivity().getIntent();
        String hostName = intent.getStringExtra(EXTRA_HOST_NAME);
        if (hostName != null) {

            //TODO: hostname is empty
            Log.d(TAG, "startWithIntent: hostname=" + hostName);
            setIPV(hostName);
            params = "-c 3 ";
            nameRadioButton.setChecked(true);
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
