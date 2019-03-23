package com.simpleapps.pingme;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PingAsyncTask extends AsyncTask<Void, String, String> {
    private static final String TAG           = "PingAsyncTask";
    public static final  String NO_CONNECTION = "0.00 ms";

    private String       command;
    private String       hostName;
    private TaskDelegate delegate;

    private Process mIpAddrProcess;

    private StringBuilder lineAfterCancelation;

    public interface TaskDelegate {
        void fillConnectionResult(String text);

        void changeButtonName(boolean taskIsFinished);
    }

    public PingAsyncTask(TaskDelegate context, String command, String hostName) {
        this.command = command;
        this.hostName = hostName;
        delegate = context;
        lineAfterCancelation = new StringBuilder();
    }

    protected String doInBackground(Void... voids) {
        Runtime runtime = Runtime.getRuntime();
        try {
            String fullCommand = String.format("%s %s", command, hostName);
            Log.d(TAG, "doInBackground: " + fullCommand);
            mIpAddrProcess = runtime.exec(fullCommand);
            String   pid            = "";
            String[] processDetails = mIpAddrProcess.toString().split(",");
            for (String part : processDetails) {
                if (part.contains("pid=")) {
                    pid = part.substring(part.lastIndexOf("pid=") + 4);
                    break;
                }
            }
            String line;
            String responceTime = NO_CONNECTION;

            InputStreamReader in = new InputStreamReader(
                    mIpAddrProcess.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(in);
            while ((line = bufferedReader.readLine()) != null) {
                Log.d(TAG, line + "\n");
                //getting time value for widget
                int index = line.indexOf("time=");
                if (index != -1) {
                    responceTime = line.substring(index + 5);
                }

                publishProgress(line);

                if (isCancelled()) {
                    runtime.exec("kill -INT " + pid);
                    lineAfterCancelation.append(line).append("\n");
                }
            }
            return responceTime;
        } catch (IOException e) {
            publishProgress("Wrong command, check hostname and parameters carefully.");
            return NO_CONNECTION;
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (delegate != null) {
            delegate.fillConnectionResult(values[0] + "\n");
        }
    }

    @Override
    protected void onPostExecute(String result) {

        if (delegate != null && mIpAddrProcess != null) {
            if (result.equals(NO_CONNECTION)) {
                try {
                    InputStreamReader in = new InputStreamReader(
                            mIpAddrProcess.getErrorStream());
                    BufferedReader bufferedReader = new BufferedReader(in);
                    String         line;
                    while ((line = bufferedReader.readLine()) != null) {
                        Log.d(TAG, "onPostExecute: " + line);
                        delegate.fillConnectionResult(line + "\n");
                    }
                } catch (IOException e) {
                    delegate.fillConnectionResult(e.getLocalizedMessage());
                }
            }
            delegate.changeButtonName(true);
        }
    }

    @Override
    protected void onCancelled() {
        if (delegate != null) {
            delegate.fillConnectionResult(lineAfterCancelation.toString());
            delegate.changeButtonName(true);
        }
    }
}
