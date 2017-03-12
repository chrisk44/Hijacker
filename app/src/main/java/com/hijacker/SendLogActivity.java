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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
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
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

import static com.hijacker.MainActivity.ANS_POSITIVE;
import static com.hijacker.MainActivity.REQ_EXIT;
import static com.hijacker.MainActivity.REQ_REPORT;
import static com.hijacker.MainActivity.connect;

public class SendLogActivity extends AppCompatActivity{
    static String busybox;
    String stackTrace, user_email = null;
    EditText user_email_et;
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

        busybox = getFilesDir().getAbsolutePath() + "/busybox";
        stackTrace = getIntent().getStringExtra("exception");
        Log.e("HIJACKER/SendLog", stackTrace);

        try{
            shell = Runtime.getRuntime().exec("su");
        }catch(IOException e){
            Log.e("HIJACKER/onCreate", "Caught Exception in shell start: " + e.toString());
            Toast.makeText(this, "Couldn't start su shell to stop any remaining processes", Toast.LENGTH_LONG).show();
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

        user_email_et.setText(pref.getString("user_email", ""));

        report = new File(Environment.getExternalStorageDirectory() + "/report.txt");
        if(report.exists()) report.delete();
        try{
            report.createNewFile();
        }catch(IOException ignored){}
        createReport();

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
        intent.putExtra(Intent.EXTRA_TEXT, "Log file attached.\n\nAdd additional details here, like what exactly you were doing when the crash occurred.");
        startActivity(intent);
    }
    void createReport(){
        PackageManager manager = this.getPackageManager();
        PackageInfo info = null;
        try{
            info = manager.getPackageInfo (this.getPackageName(), 0);
        }catch(PackageManager.NameNotFoundException ignored){}
        String model = Build.MODEL;
        if (!model.startsWith(Build.MANUFACTURER)) model = Build.MANUFACTURER + " " + model;

        // Extract to file.
        FileWriter writer = null;
        try{
            writer = new FileWriter(report, true);
            writer.write("\n--------------------------------------------------------------------------------\n");
            writer.write("Hijacker bug report - " + new Date().toString() + "\n\n");
            writer.write("Android version: " +  Build.VERSION.SDK_INT + '\n');
            writer.write("Device: " + model + '\n');
            writer.write("App version: " + (info == null ? "(null)" : info.versionName) + '\n');
            writer.write("App data path: " + getFilesDir().getAbsolutePath() + '\n');
            writer.write("\nStack trace:\n" + stackTrace + '\n');

            String cmd = "echo pref_file--------------------------------------; su -c cat /data/data/com.hijacker/shared_prefs/com.hijacker_preferences.xml;";
            cmd += " echo app directory----------------------------------; " + busybox + " ls -lR " + getFilesDir().getAbsolutePath() + ';';
            cmd += " echo fw_bcmdhd--------------------------------------; su -c strings /vendor/firmware/fw_bcmdhd.bin | grep \"FWID:\";";
            cmd += " echo ps---------------------------------------------; su -c ps | " + busybox + " grep -e air -e mdk -e reaver;";
            cmd += " echo busybox----------------------------------------; " + busybox + ";";
            cmd += " echo logcat-----------------------------------------; logcat -d -v time | " + busybox + " grep HIJACKER;";
            cmd += " echo ENDOFLOG\n";
            Log.d("HIJACKER/SendLog", cmd);
            shell_in.print(cmd);                //Runtime.getRuntime().exec(cmd) just echos the cmd...
            shell_in.flush();

            String buffer = shell_out.readLine();
            while(!buffer.equals("ENDOFLOG")){
                writer.write(buffer + '\n');
                buffer = shell_out.readLine();
            }

            writer.close();
        }catch(IOException e){
            if(writer != null){
                try{
                    writer.close();
                }catch(IOException ignored){}
            }
        }
    }
    public void onSend(final View v){
        if(!report.exists()){
            Log.d("HIJACKER/SendLog", "filename is null");
            Toast.makeText(this, "Report was not created", Toast.LENGTH_LONG).show();
            return;
        }
        user_email = user_email_et.getText().toString();
        pref_edit.putString("user_email", user_email);
        pref_edit.commit();

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(1).getState()!=NetworkInfo.State.CONNECTED &&
                connectivityManager.getNetworkInfo(0).getState()!=NetworkInfo.State.CONNECTED){
            Log.d("HIJACKER/SendLog", "No internet connection");
            Toast.makeText(this, getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(SendLogActivity.this, getString(R.string.server_error) , Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(SendLogActivity.this, getString(R.string.server_denied), Toast.LENGTH_SHORT).show();
                            handler.post(runnable);
                            return;
                        }
                    }else{
                        Toast.makeText(SendLogActivity.this, getString(R.string.connection_closed), Toast.LENGTH_SHORT).show();
                        handler.post(runnable);
                        return;
                    }

                    BufferedReader fileReader = new BufferedReader(new FileReader(report));

                    socketIn.print("User email: " + user_email + '\n');
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
                    Toast.makeText(SendLogActivity.this, getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
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
