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
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

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

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_DENIED){
            filename = extractLogToFile();
            if(filename==null) Log.d("SendLogActivity", "filename is null");
            else{
                File log = new File(filename);
                try{
                    BufferedReader br = new BufferedReader(new FileReader(log));
                    String buffer;
                    TextView console = (TextView)findViewById(R.id.console);
                    console.setMovementMethod(ScrollingMovementMethod.getInstance());
                    int i=0;
                    while((buffer = br.readLine())!=null && i<400){
                        console.append(buffer + '\n');
                        i++;
                    }
                    if(i==400) console.append("...more logcat not displayed here");
                }catch(IOException ignored){}
            }
        }else{
            Log.e("SendLogActivity", "WRITE_EXTERNAL_STORAGE permission denied");
        }

        stopAll();
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
        }catch(PackageManager.NameNotFoundException ignored){}
        String model = Build.MODEL;
        if (!model.startsWith(Build.MANUFACTURER)) model = Build.MANUFACTURER + " " + model;

        // Make file name - file must be saved to external storage or it wont be readable by the email app.
        String fullName = Environment.getExternalStorageDirectory() + "/report.txt";

        // Extract to file.
        File file = new File (fullName);
        FileWriter writer = null;
        try{
            // write output stream
            writer = new FileWriter(file);
            writer.write("Android version: " +  Build.VERSION.SDK_INT + "\n");
            writer.write("Device: " + model + "\n");
            writer.write("App version: " + (info == null ? "(null)" : info.versionCode) + "\n");

            // get input stream
            String cmd = "echo pref_file--------------------------------------; su -c cat /data/user/0/com.hijacker/shared_prefs/com.hijacker_preferences.xml;";
            cmd += " echo ls_system-xbin---------------------------------; su -c ls /system/xbin -1| busybox grep -e air -e mdk -e reaver -e nexutil -e iw -e libfakeioctl.so -e busybox -e toolbox;";
            cmd += " echo ls_su-xbin-------------------------------------; su -c ls /su/xbin -1| busybox grep -e air -e mdk -e reaver -e nexutil -e iw -e libfakeioctl.so -e busybox -e toolbox;";
            cmd += " echo ls_vendor-lib----------------------------------; su -c ls /vendor/lib -1| busybox grep -e air -e mdk -e reaver -e nexutil -e iw -e libfakeioctl.so -e busybox -e toolbox;";
            cmd += " echo ls_su-lib--------------------------------------; su -c ls /su/lib -1| busybox grep -e air -e mdk -e reaver -e nexutil -e iw -e libfakeioctl.so -e busybox -e toolbox;";
            cmd += " echo ls_system-lib----------------------------------; su -c ls /system/lib -1| busybox grep -e air -e mdk -e reaver -e nexutil -e iw -e libfakeioctl.so -e busybox -e toolbox;";
            cmd += " echo fw_bcmdhd--------------------------------------; su -c strings /vendor/firmware/fw_bcmdhd.bin | grep \"FWID:\";";
            cmd += " echo ps---------------------------------------------; su -c ps | busybox grep -e air -e mdk -e reaver;";
            cmd += " echo busybox----------------------------------------; busybox;";
            cmd += " echo toolbox----------------------------------------; toolbox;";
            cmd += " echo logcat-----------------------------------------; logcat -d -v time;";
            cmd += " echo ENDOFLOG\n";
            Log.d("cmd", cmd);
            shell_in.print(cmd);                //Runtime.getRuntime().exec(cmd) just echos the cmd...
            shell_in.flush();
            String buffer = shell_out.readLine();
            while(!buffer.equals("ENDOFLOG")){
                writer.write(buffer + '\n');
                buffer = shell_out.readLine();
            }

            writer.close();
        }catch(IOException e){
            if (writer != null)
                try {
                    writer.close();
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
    public void stopAll(){
        ArrayList<Integer> pids = new ArrayList<>();
        try{
            int pid;
            String s = null;
            shell_in.print("toolbox ps | busybox grep -e air -e mdk -e reaver; echo ENDOFPS\n");
            shell_in.flush();
            while(s==null){ s = shell_out.readLine(); } //for some reason sometimes s remains null
            while(!s.equals("ENDOFPS")){
                pid = ps(s);
                if(pid!=0){
                    pids.add(pid);
                }
                s = shell_out.readLine();
            }
        }catch(IOException e){ Log.e("Exception", "Caught Exception in getPIDs(pr): " + e.toString()); }
        if(pids.isEmpty()) Log.d("stopAll", "Nothing found");
        else{
            for(int i = 0; i<pids.size(); i++){
                Log.d("Killing...", Integer.toString(pids.get(i)));
                shell_in.print("kill " + pids.get(i) + "\n");
                shell_in.flush();
            }
        }
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        System.exit(1);
    }
}
