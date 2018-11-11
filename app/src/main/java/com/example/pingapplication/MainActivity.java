package com.example.pingapplication;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;

import static android.os.AsyncTask.Status.RUNNING;

public class MainActivity extends AppCompatActivity
        implements PingAsyncTask.TaskDelegate {
    private static final String                        TAG = "MainActivity";
    private              TextInputLayout               addressWrapper;
    private              TextInputAutoCompleteTextView address;
    private              Button                        pingButton;
    private              Button                        stopButton;
    private              TextView                      connectionResult;
    private              ImageButton                   showCommandButton;
    private              LinearLayout                  commandContainer;
    private              TextView                      commandPingType;
    private              EditText                      commandText;

    private String        command;
    private String        params;
    private PingAsyncTask pingTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);


        final HostNameDatabase database = Room.databaseBuilder(this, HostNameDatabase.class,
                                                               "Host_name_database")
                                              .allowMainThreadQueries().build();

        params = "";
        final InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);

        ArrayList<String> savedNames = new ArrayList<>();
        for (HostName name: database.hostNameDao().getAll() ) {
            savedNames.add(name.getName());
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                                                                      android.R.layout
                                                                        .simple_dropdown_item_1line, savedNames);

        addressWrapper = findViewById(R.id.hostNameWrapper);
        address = findViewById(R.id.hostName);
        address.setInputType(
                InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_NORMAL);
        address.setKeyListener(DigitsKeyListener.getInstance("0123456789.:"));
        address.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    imm.showSoftInput(v, 0);
                }
            }
        });
        address.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    pingButton.performClick();
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
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
        pingButton = findViewById(R.id.pingButton);
        stopButton = findViewById(R.id.stopPingButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pingTask != null && pingTask.getStatus().equals(RUNNING)) {
                    pingTask.cancel(false);
                }
            }
        });
        connectionResult = findViewById(R.id.connectionResult);
        connectionResult.setMovementMethod(new ScrollingMovementMethod());
        showCommandButton = findViewById(R.id.showCommandButton);
        commandContainer = findViewById(R.id.pingCommandContainer);
        commandPingType = findViewById(R.id.pingType);
        commandPingType.setText("/system/bin/ping ");
        commandText = findViewById(R.id.commandText);
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

        pingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                connectionResult.setText("");
                String hostName = address.getText().toString();
                if (hostName.isEmpty()) {
                    addressWrapper.setErrorEnabled(true);
                    addressWrapper.setError(getString(R.string.host_name_error));
                    address.requestFocus();
                    return;
                }

                if(adapter.getPosition(hostName) == -1){
                    database.hostNameDao().insertAll(new HostName(hostName));
                    adapter.insert(hostName, adapter.getCount());
                    address.setAdapter(adapter);
                }

                commandContainer.setVisibility(View.GONE);
                String finalCommand = command + params + " ";
                pingTask = new PingAsyncTask(MainActivity.this, finalCommand, hostName);
                pingTask.execute();
            }
        });


        Intent intent = getIntent();
        String hostName = intent.getStringExtra(PingWidgetProvider.EXTRA_HOST_NAME);
        if(hostName !=null){
            address.setText(hostName);
            setIPV(hostName);
            params = "-c 3 ";
            RadioButton radioButton = findViewById(R.id.radio_name);
            radioButton.setChecked(true);
            pingButton.performClick();
            address.clearFocus();
        }
    }

    private void setIPV(String hostName) {
        if (hostName.contains(":")) {
            command = "/system/bin/ping6 ";
        } else {
            command = "/system/bin/ping ";
        }
    }


    @Override
    public void fillConnectionResult(String text) {
        connectionResult.append(text);
    }


    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.radio_name:
                if (checked) {
                    address.setInputType(InputType.TYPE_TEXT_VARIATION_URI
                                                 | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    address.requestFocus();
                }
                break;
            case R.id.radio_ip:
                if (checked) {
                    address.setInputType(
                            InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_NORMAL);
                    address.setKeyListener(DigitsKeyListener.getInstance("0123456789.:"));
                    address.requestFocus();
                }
                break;
        }
    }
}
