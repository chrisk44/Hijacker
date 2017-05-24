package com.hijacker;

/*
    Copyright (C) 2016  Christos Kyriakopoylos

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
//Code from here (modified): http://stackoverflow.com/questions/601503/how-do-i-obtain-crash-data-from-my-android-application

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import static com.hijacker.MainActivity.ANS_POSITIVE;
import static com.hijacker.MainActivity.REQ_EXIT;
import static com.hijacker.MainActivity.REQ_REPORT;
import static com.hijacker.MainActivity.connect;
import static com.hijacker.MainActivity.createReport;
import static com.hijacker.MainActivity.deviceID;
import static com.hijacker.MainActivity.deviceModel;
import static com.hijacker.MainActivity.internetAvailable;
import static com.hijacker.MainActivity.versionCode;
import static com.hijacker.MainActivity.versionName;

public class SendLogActivity extends AppCompatActivity{
    static String busybox;
    View rootView;
    View sendProgress, sendCompleted, sendBtn, sendEmailBtn, progressBar;
    TextView console;
    EditText userEmailView, extraView;

    File report;
    String stackTrace, user_email = null;
    boolean internetGranted = false;
    Process shell;
    PrintWriter shell_in;
    BufferedReader shell_out;
    SharedPreferences pref;
    SharedPreferences.Editor pref_edit;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setFinishOnTouchOutside(false);
        setContentView(R.layout.activity_send_log);

        rootView = findViewById(R.id.activity_send_log);
        userEmailView = (EditText)findViewById(R.id.email_et);
        extraView = (EditText)findViewById(R.id.extra_et);
        progressBar = findViewById(R.id.reportProgressBar);
        console = (TextView)findViewById(R.id.console);
        sendProgress = findViewById(R.id.progress);
        sendCompleted = findViewById(R.id.completed);
        sendBtn = findViewById(R.id.sendBtn);
        sendEmailBtn = findViewById(R.id.sendEmailBtn);

        userEmailView.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
                if(actionId == EditorInfo.IME_ACTION_NEXT){
                    extraView.requestFocus();
                    return true;
                }
                return false;
            }
        });

        busybox = getFilesDir().getAbsolutePath() + "/bin/busybox";
        stackTrace = getIntent().getStringExtra("exception");
        Log.e("HIJACKER/SendLog", stackTrace);

        pref = PreferenceManager.getDefaultSharedPreferences(SendLogActivity.this);
        pref_edit = pref.edit();
        userEmailView.setText(pref.getString("user_email", ""));

        //Load device info
        PackageManager manager = getPackageManager();
        PackageInfo info;
        try{
            info = manager.getPackageInfo(getPackageName(), 0);
            versionName = info.versionName.replace(" ", "_");
            versionCode = info.versionCode;
        }catch(PackageManager.NameNotFoundException e){
            Log.e("HIJACKER/SendLog", e.toString());
        }
        deviceModel = Build.MODEL;
        if(!deviceModel.startsWith(Build.MANUFACTURER)) deviceModel = Build.MANUFACTURER + " " + deviceModel;
        deviceModel = deviceModel.replace(" ", "_");

        deviceID = pref.getLong("deviceID", -1);

        new SetupTask().execute();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults){
        boolean writeGranted = grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED;
        internetGranted = grantResults.length>0 && grantResults[1]==PackageManager.PERMISSION_GRANTED;
        if(writeGranted){
            new ReportTask().execute();
        }else{
            progressBar.setVisibility(View.GONE);
            console.setText(getString(R.string.cant_create_report));
        }
    }
    private class SetupTask extends AsyncTask<Void, String, Boolean>{
        @Override
        protected Boolean doInBackground(Void... params){
            //Start su shell
            try{
                shell = Runtime.getRuntime().exec("su");
            }catch(IOException e){
                Log.e("HIJACKER/onCreate", "Caught Exception in shell start: " + e.toString());
                Snackbar.make(rootView, "Couldn't start su shell to stop any remaining processes", Snackbar.LENGTH_LONG).show();
            }
            shell_in = new PrintWriter(shell.getOutputStream());
            shell_out = new BufferedReader(new InputStreamReader(shell.getInputStream()));

            stopAll();

            ActivityCompat.requestPermissions(SendLogActivity.this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.ACCESS_NETWORK_STATE
            }, 0);

            return true;
        }
    }
    private class ReportTask extends AsyncTask<Void, String, Boolean>{
        @Override
        protected Boolean doInBackground(Void... params){
            report = new File(Environment.getExternalStorageDirectory() + "/report.txt");
            return createReport(report, getFilesDir().getAbsolutePath(), stackTrace, shell);
        }
        @Override
        protected void onPostExecute(final Boolean success){
            progressBar.setVisibility(View.GONE);

            if(success){
                //Show snippet of report
                try{
                    BufferedReader br = new BufferedReader(new FileReader(report));
                    console.setMovementMethod(ScrollingMovementMethod.getInstance());
                    int i = 0, limit = 100;
                    String buffer;
                    while((buffer = br.readLine())!=null && i<limit){
                        console.append(buffer + '\n');
                        i++;
                    }
                    if(i==limit) console.append("...");
                }catch(IOException ignored){}

                sendBtn.setEnabled(true);
                sendEmailBtn.setEnabled(true);
            }else{
                console.setText(getString(R.string.report_not_created));
            }
        }
    }
    private class SendReportTask extends AsyncTask<Void, String, Boolean>{
        String extraDetails;
        @Override
        protected void onPreExecute(){
            sendBtn.setVisibility(View.GONE);
            sendProgress.setVisibility(View.VISIBLE);

            extraDetails = extraView.getText().toString();
        }
        @Override
        protected Boolean doInBackground(Void... params){
            if(!report.exists()){
                Log.d("HIJACKER/SendReportTask", "report doesn't exist");
                publishProgress(getString(R.string.report_not_created));
                return false;
            }
            if(!internetGranted){
                Log.d("HIJACKER/SendReportTask", "No internet permission");
                publishProgress(getString(R.string.no_internet_permission));
                return false;
            }
            if(!internetAvailable(SendLogActivity.this)){
                Log.d("HIJACKER/SendReportTask", "No internet connection");
                publishProgress(getString(R.string.no_internet));
                return false;
            }

            Socket socket = connect();
            if(socket==null){
                publishProgress(getString(R.string.server_error));
                return false;
            }
            try{
                BufferedReader socketOut = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter socketIn = new PrintWriter(socket.getOutputStream());
                socketIn.print(REQ_REPORT + '\n');
                socketIn.flush();

                String temp = socketOut.readLine();
                if(temp!=null){
                    if(!temp.equals(ANS_POSITIVE)){
                        publishProgress(getString(R.string.server_denied));
                        return false;
                    }
                }else{
                    publishProgress(getString(R.string.connection_closed));
                    return false;
                }

                BufferedReader fileReader = new BufferedReader(new FileReader(report));

                socketIn.print("User email: " + user_email + '\n');
                socketIn.print("Extra details: " + extraDetails);
                socketIn.flush();
                String buffer = fileReader.readLine();
                while(buffer!=null){
                    socketIn.print(buffer + '\n');
                    socketIn.flush();
                    buffer = fileReader.readLine();
                }
                socketIn.print("EOF\n");
                socketIn.print(REQ_EXIT + '\n');
                socketIn.flush();

                socketIn.close();
                socketOut.close();
                socket.close();
            }catch(IOException e){
                Log.e("HIJACKER/SendReportTask", e.toString());
                publishProgress(getString(R.string.unknown_error));
            }
            return true;
        }
        @Override
        protected void onProgressUpdate(String... message){
            Snackbar.make(rootView, message[0], Snackbar.LENGTH_SHORT).show();
        }
        @Override
        protected void onPostExecute(final Boolean success){
            sendProgress.setVisibility(View.GONE);
            if(success){
                sendCompleted.setVisibility(View.VISIBLE);
            }else{
                sendBtn.setVisibility(View.VISIBLE);
            }
        }
    }
    public void onUseEmail(View v){
        Intent intent = new Intent (Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"kiriakopoulos44@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Hijacker bug report");
        Uri attachment = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", report);
        intent.putExtra(Intent.EXTRA_STREAM, attachment);
        intent.putExtra(Intent.EXTRA_TEXT, extraView.getText().toString());
        startActivity(intent);
    }
    public void onSend(View v){
        user_email = userEmailView.getText().toString();
        pref_edit.putString("user_email", user_email);
        pref_edit.commit();

        new SendReportTask().execute();
    }
    public void onRestart(View v){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    public void stopAll(){
        ArrayList<Integer> pids = new ArrayList<>();
        String processes[] = {
                "airodump-ng",
                "aireplay-ng",
                "aircrack-ng",
                "mdk3",
                "reaver",
                "reaver-wash"
        };
        String cmd = busybox + " pidof";
        for(String process_name : processes){
            cmd += ' ' + process_name;
        }
        cmd += "; echo ENDOFPIDOF\n";
        shell_in.print(cmd);
        shell_in.flush();
        String buffer = null;
        try{
            while(buffer==null) buffer = shell_out.readLine();
            while(!buffer.equals("ENDOFPIDOF")){
                String[] temp = buffer.split(" ");
                try{
                    for(String tmp : temp){
                        pids.add(Integer.parseInt(tmp));
                    }
                }catch(NumberFormatException e){
                    Log.e("HIJACKER/SendLog", "Exception: " + e.toString());
                }
                buffer = shell_out.readLine();
            }
        }catch(IOException e){
            Log.e("HIJACKER/SendLog", "Exception: " + e.toString());
        }
        if(pids.isEmpty()) Log.d("HIJACKER/stopAll", "Nothing found");
        else{
            for(int i = 0; i<pids.size(); i++){
                Log.d("HIJACKER/Killing...", Integer.toString(pids.get(i)));
                shell_in.print("kill " + pids.get(i) + "\n");
                shell_in.flush();
            }
        }
    }
    static Handler handler = new Handler();
}
