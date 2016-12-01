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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import static com.hijacker.MainActivity.PROCESS_AIRCRACK;
import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_MDK;
import static com.hijacker.MainActivity.PROCESS_REAVER;
import static com.hijacker.MainActivity.ps;

public class SendLogActivity extends AppCompatActivity{
    String filename;
    Process shell;
    PrintWriter shell_in;
    BufferedReader shell_out;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // make a dialog without a titlebar
        setFinishOnTouchOutside(false); // prevent users from dismissing the dialog by tapping outside
        setContentView(R.layout.activity_send_log);

        stopAll();

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_DENIED){
            filename = extractLogToFile();
            if(filename==null) Log.d("SendLogActivity", "filename is null");
        }else{
            Log.e("SendLogActivity", "WRITE_EXTERNAL_STORAGE permission denied");
        }
    }
    private void sendLogFile(String fullName){
        Intent intent = new Intent (Intent.ACTION_SEND);
        intent.setType("plain/text");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"kiriakopoulos44@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Hijacker bug report");
        Uri attachment = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", new File(fullName));
        intent.putExtra(Intent.EXTRA_STREAM, attachment);
        intent.putExtra(Intent.EXTRA_TEXT, "Log file attached.\n\nAdd additional details here, like what exactly you were doing when the crash occurred."); // do this so some email clients don't complain about empty body.
        startActivity(intent);
    }
    private String extractLogToFile(){
        PackageManager manager = this.getPackageManager();
        PackageInfo info = null;
        try{
            info = manager.getPackageInfo (this.getPackageName(), 0);
        }catch(PackageManager.NameNotFoundException e2){}
        String model = Build.MODEL;
        if (!model.startsWith(Build.MANUFACTURER))
            model = Build.MANUFACTURER + " " + model;

        // Make file name - file must be saved to external storage or it wont be readable by the email app.
        String fullName = Environment.getExternalStorageDirectory() + "/report.txt";

        // Extract to file.
        File file = new File (fullName);
        InputStreamReader reader = null;
        FileWriter writer = null;
        try{
            String cmd = "logcat -d -v time";

            // get input stream
            Process process = Runtime.getRuntime().exec(cmd);
            reader = new InputStreamReader(process.getInputStream());

            // write output stream
            writer = new FileWriter(file);
            writer.write("Android version: " +  Build.VERSION.SDK_INT + "\n");
            writer.write("Device: " + model + "\n");
            writer.write("App version: " + (info == null ? "(null)" : info.versionCode) + "\n");

            char[] buffer = new char[10000];
            while(true){
                int n = reader.read (buffer, 0, buffer.length);
                if (n == -1) break;
                writer.write (buffer, 0, n);
            }

            reader.close();
            writer.close();
        }catch (IOException e){
            if (writer != null)
                try {
                    writer.close();
                }catch(IOException ignored) {}
            if (reader != null)
                try {
                    reader.close();
                }catch(IOException ignored) {}

            return null;
        }

        return fullName;
    }
    public void onSend(View v){
        if(filename==null){
            Log.d("SendLogActivity", "filename is null");
            Toast.makeText(this, "Report was not created", Toast.LENGTH_LONG).show();
        }else sendLogFile(filename);
    }
    public ArrayList<Integer> getPIDs(int pr){
        ArrayList<Integer> list = new ArrayList<>();
        try{
            int pid;
            String s = null;
            switch(pr){
                case PROCESS_AIRODUMP:
                    shell_in.print("ps | grep airo; echo ENDOFPS\n");
                    break;
                case PROCESS_AIREPLAY:
                    shell_in.print("ps | grep aire; echo ENDOFPS\n");
                    break;
                case PROCESS_MDK:
                    shell_in.print("ps | grep mdk3; echo ENDOFPS\n");
                    break;
                case PROCESS_AIRCRACK:
                    shell_in.print("ps | grep airc; echo ENDOFPS\n");
                    break;
                case PROCESS_REAVER:
                    shell_in.print("ps | grep reav; echo ENDOFPS\n");
                    break;
            }
            shell_in.flush();
            while(s==null){ s = shell_out.readLine(); } //for some reason sometimes s remains null
            while(!s.equals("ENDOFPS")){
                pid = ps(s);
                if(pid!=0){
                    list.add(pid);
                }
                s = shell_out.readLine();
            }
        }catch(IOException e){ Log.e("Exception", "Caught Exception in getPIDs(pr): " + e.toString()); }
        return list;
    }
    public void stopAll(){
        try{
            shell = Runtime.getRuntime().exec("su");
        }catch(IOException e){
            Log.e("onCreate", "Caught Exception in shell start: " + e.toString());
            Toast.makeText(this, "Couldn't start su shell to stop any remaining processes", Toast.LENGTH_LONG).show();
            return;
        }

        shell_in = new PrintWriter(shell.getOutputStream());
        shell_out = new BufferedReader(new InputStreamReader(shell.getInputStream()));
        if(shell_in==null || shell_out==null){
            Log.e("onCreate", "Error opening shell_in/shell_out");
            Toast.makeText(this, "Couldn't start su shell to stop any remaining processes", Toast.LENGTH_LONG).show();
            return;
        }

        ArrayList<Integer> pids = new ArrayList<>();
        pids.addAll(getPIDs(PROCESS_AIRCRACK));
        pids.addAll(getPIDs(PROCESS_AIRODUMP));
        pids.addAll(getPIDs(PROCESS_AIREPLAY));
        pids.addAll(getPIDs(PROCESS_MDK));
        pids.addAll(getPIDs(PROCESS_REAVER));
        if(pids.isEmpty()) Log.d("stopAll", "Nothing found");
        else{
            for(int i = 0; i<pids.size(); i++){
                Log.d("Killing...", Integer.toString(pids.get(i)));
                shell_in.print("kill " + pids.get(i) + "\n");
                shell_in.flush();
            }
        }
    }
}
