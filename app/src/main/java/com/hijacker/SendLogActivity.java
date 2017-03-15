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
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
    String stackTrace, user_email = null;
    EditText user_email_et, extra_et;
    File report;
    Process shell;
    PrintWriter shell_in;
    BufferedReader shell_out;
    SharedPreferences pref;
    SharedPreferences.Editor pref_edit;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // make a dialog without a titlebar
        setFinishOnTouchOutside(false); // prevent users from dismissing the dialog by tapping outside
        setContentView(R.layout.activity_send_log);
        user_email_et = (EditText)findViewById(R.id.email_et);
        extra_et = (EditText)findViewById(R.id.extra_et);

        busybox = getFilesDir().getAbsolutePath() + "/bin/busybox";
        stackTrace = getIntent().getStringExtra("exception");
        Log.e("HIJACKER/SendLog", stackTrace);

        try{
            shell = Runtime.getRuntime().exec("su");
        }catch(IOException e){
            Log.e("HIJACKER/onCreate", "Caught Exception in shell start: " + e.toString());
            Snackbar.make(getCurrentFocus(), "Couldn't start su shell to stop any remaining processes", Snackbar.LENGTH_LONG).show();
            return;
        }
        shell_in = new PrintWriter(shell.getOutputStream());
        shell_out = new BufferedReader(new InputStreamReader(shell.getInputStream()));

        stopAll();

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET
        }, 0);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref_edit = pref.edit();

        PackageManager manager = this.getPackageManager();
        PackageInfo info;
        try{
            info = manager.getPackageInfo(this.getPackageName(), 0);
            versionName = info.versionName.replace(" ", "_");
            versionCode = info.versionCode;
        }catch(PackageManager.NameNotFoundException e){
            Log.e("HIJACKER/SendLog", e.toString());
        }
        deviceModel = Build.MODEL;
        if(!deviceModel.startsWith(Build.MANUFACTURER)) deviceModel = Build.MANUFACTURER + " " + deviceModel;
        deviceModel = deviceModel.replace(" ", "_");

        deviceID = pref.getLong("deviceID", -1);

        user_email_et.setText(pref.getString("user_email", ""));

        report = new File(Environment.getExternalStorageDirectory() + "/report.txt");
        createReport(report, getFilesDir().getAbsolutePath(), stackTrace, shell);

        try{
            BufferedReader br = new BufferedReader(new FileReader(report));
            TextView console = (TextView)findViewById(R.id.console);
            console.setMovementMethod(ScrollingMovementMethod.getInstance());
            int i=0, limit=100;
            String buffer;
            while((buffer = br.readLine())!=null && i<limit){
                console.append(buffer + '\n');
                i++;
            }
            if(i==limit) console.append("...");
        }catch(IOException ignored){}
    }
    public void onUseEmail(View v){
        Intent intent = new Intent (Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"kiriakopoulos44@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Hijacker bug report");
        Uri attachment = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", report);
        intent.putExtra(Intent.EXTRA_STREAM, attachment);
        intent.putExtra(Intent.EXTRA_TEXT, extra_et.getText().toString());
        startActivity(intent);
    }
    public void onSend(final View v){
        if(!report.exists()){
            Log.d("HIJACKER/SendLog", "filename is null");
            Snackbar.make(getCurrentFocus(), "Report was not created", Snackbar.LENGTH_LONG).show();
            return;
        }
        user_email = user_email_et.getText().toString();
        pref_edit.putString("user_email", user_email);
        pref_edit.commit();

        if(!internetAvailable(this)){
            Log.d("HIJACKER/SendLog", "No internet connection");
            Snackbar.make(getCurrentFocus(), getString(R.string.no_internet), Snackbar.LENGTH_SHORT).show();
            return;
        }

        final ProgressBar progress = (ProgressBar)findViewById(R.id.progress);
        final ImageView completed = (ImageView)findViewById(R.id.completed);
        v.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);

        new Thread(new Runnable(){
            @Override
            public void run(){
                Looper.prepare();
                Runnable runnable = new Runnable(){
                    @Override
                    public void run(){
                        progress.setVisibility(View.GONE);
                        v.setVisibility(View.VISIBLE);
                    }
                };
                Socket socket = connect();
                if(socket==null){
                    handler.post(runnable);
                    Snackbar.make(getCurrentFocus(), getString(R.string.server_error) , Snackbar.LENGTH_SHORT).show();
                    return;
                }
                try{
                    BufferedReader socketOut = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter socketIn = new PrintWriter(socket.getOutputStream());
                    socketIn.print(REQ_REPORT + '\n');
                    socketIn.flush();

                    String temp = socketOut.readLine();
                    if(temp!=null){
                        if(!temp.equals(ANS_POSITIVE)){
                            Snackbar.make(getCurrentFocus(), getString(R.string.server_denied), Snackbar.LENGTH_SHORT).show();
                            handler.post(runnable);
                            return;
                        }
                    }else{
                        Snackbar.make(getCurrentFocus(), getString(R.string.connection_closed), Snackbar.LENGTH_SHORT).show();
                        handler.post(runnable);
                        return;
                    }

                    BufferedReader fileReader = new BufferedReader(new FileReader(report));

                    socketIn.print("User email: " + user_email + '\n');
                    socketIn.print("Extra details: " + extra_et.getText().toString());
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
                    handler.post(new Runnable(){
                        @Override
                        public void run(){
                            progress.setVisibility(View.GONE);
                            completed.setVisibility(View.VISIBLE);
                        }
                    });
                }catch(IOException e){
                    Log.e("HIJACKER/onSend", e.toString());
                    Snackbar.make(getCurrentFocus(), getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
                    handler.post(runnable);
                }
            }
        }).start();
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
