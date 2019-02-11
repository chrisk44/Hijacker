package com.hijacker;

/*
    Copyright (C) 2019  Christos Kyriakopoulos

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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import static com.hijacker.MainActivity.createReport;
import static com.hijacker.MainActivity.deviceModel;
import static com.hijacker.MainActivity.versionCode;
import static com.hijacker.MainActivity.versionName;

public class SendLogActivity extends AppCompatActivity{
    static String busybox;
    View rootView;
    View sendEmailBtn, progressBar;
    TextView console;

    File report;
    String stackTrace;
    Process shell;
    PrintWriter shell_in;
    BufferedReader shell_out;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setFinishOnTouchOutside(false);
        setContentView(R.layout.activity_send_log);

        rootView = findViewById(R.id.activity_send_log);
        progressBar = findViewById(R.id.reportProgressBar);
        console = findViewById(R.id.console);
        sendEmailBtn = findViewById(R.id.sendEmailBtn);

        busybox = getFilesDir().getAbsolutePath() + "/bin/busybox";
        stackTrace = getIntent().getStringExtra("exception");
        Log.e("HIJACKER/SendLog", stackTrace);

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

        new SetupTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults){
        boolean writeGranted = grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED;
        if(writeGranted){
            new ReportTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        String bugReport = "";
        @Override
        protected Boolean doInBackground(Void... params){
            report = new File(Environment.getExternalStorageDirectory() + "/report.txt");
            boolean result = createReport(report, getFilesDir().getAbsolutePath(), stackTrace, shell);
            if(result){
                try{
                    BufferedReader br = new BufferedReader(new FileReader(report));
                    String buffer;
                    while((buffer = br.readLine())!=null){
                        bugReport += buffer + '\n';
                    }
                }catch(IOException ignored){
                    return false;
                }
            }
            return result;
        }
        @Override
        protected void onPostExecute(final Boolean success){
            progressBar.setVisibility(View.GONE);

            if(success){
                //Show bug report
                console.setMovementMethod(ScrollingMovementMethod.getInstance());
                console.setText(bugReport);

                sendEmailBtn.setEnabled(true);
            }else{
                console.setText(getString(R.string.report_not_created));
            }
        }
    }
    public void onUseEmail(View v){
        Intent intent = new Intent (Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"droid.hijacker@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Hijacker bug report");
        Uri attachment = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", report);
        intent.putExtra(Intent.EXTRA_STREAM, attachment);
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.prompt_additional_details));
        startActivity(intent);
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
}
