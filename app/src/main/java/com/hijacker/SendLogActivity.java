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

import static com.hijacker.MainActivity.PROCESS_AIRCRACK;
import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_MDK;
import static com.hijacker.MainActivity.PROCESS_REAVER;
import static com.hijacker.MainActivity.airOnStartup;
import static com.hijacker.MainActivity.aircrack_dir;
import static com.hijacker.MainActivity.aireplay_dir;
import static com.hijacker.MainActivity.airodump_dir;
import static com.hijacker.MainActivity.cap_dir;
import static com.hijacker.MainActivity.chroot_dir;
import static com.hijacker.MainActivity.confirm_exit;
import static com.hijacker.MainActivity.deauthWait;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.delete_extra;
import static com.hijacker.MainActivity.disable_monMode;
import static com.hijacker.MainActivity.enable_monMode;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.manuf_while_ados;
import static com.hijacker.MainActivity.mdk3_dir;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.ps;
import static com.hijacker.MainActivity.reaver_dir;
import static com.hijacker.MainActivity.shell2;
import static com.hijacker.MainActivity.shell2_in;
import static com.hijacker.MainActivity.shell2_out;
import static com.hijacker.MainActivity.shell3;
import static com.hijacker.MainActivity.shell3_in;
import static com.hijacker.MainActivity.shell3_out;
import static com.hijacker.MainActivity.shell4;
import static com.hijacker.MainActivity.shell4_in;
import static com.hijacker.MainActivity.shell4_out;
import static com.hijacker.MainActivity.showLog;
import static com.hijacker.MainActivity.show_details;
import static com.hijacker.MainActivity.show_notif;

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
                    while((buffer = br.readLine())!=null && i<200){
                        console.append(buffer + '\n');
                        i++;
                    }
                    if(i==200) console.append("...more logcat not displayed here");
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
            writer.write("Airodump: " + Integer.toString(MainActivity.airodump_running) + "\n");
            writer.write("Aireplay: " + Integer.toString(MainActivity.aireplay_running) + "\n");
            writer.write("Mdk BF: " + Integer.toString(MDKFragment.bf_pid) + "\n");
            writer.write("Mdk ADoS: " + Integer.toString(MDKFragment.ados_pid) + "\n");
            writer.write("Reaver: " + Boolean.toString(ReaverFragment.cont) + "\n");
            writer.write("shell: " + (shell!=null ? "ok" : "null") + ", " + (shell2!=null ? "ok" : "null") + ", ");
            writer.write((shell3!=null ? "ok" : "null") + ", " + (shell4!=null ? "ok" : "null") + "\n");
            writer.write("shell_in: " + (shell_in!=null ? "ok" : "null") + ", " + (shell2_in!=null ? "ok" : "null") + ", ");
            writer.write((shell3_in!=null ? "ok" : "null") + ", " + (shell4_in!=null ? "ok" : "null") + "\n");
            writer.write("shell_out: " + (shell_out!=null ? "ok" : "null") + ", " + (shell2_out!=null ? "ok" : "null") + ", ");
            writer.write((shell3_out!=null ? "ok" : "null") + ", " + (shell4_out!=null ? "ok" : "null") + "\n");
            writer.write("path: " + path + '\n');
            writer.write("prefs:\n");
            writer.write("\tiface: " + iface + '\n');
            writer.write("\tprefix: " + prefix + '\n');
            writer.write("\tairodump_dir: " + airodump_dir + '\n');
            writer.write("\taireplay_dir: " + aireplay_dir + '\n');
            writer.write("\taircrack_dir: " + aircrack_dir + '\n');
            writer.write("\tmdk3_dir: " + mdk3_dir + '\n');
            writer.write("\treaver_dir: " + reaver_dir + '\n');
            writer.write("\tcap_dir: " + cap_dir + '\n');
            writer.write("\tchroot_dir: " + chroot_dir + '\n');
            writer.write("\tenable_monMode: " + enable_monMode + '\n');
            writer.write("\tdisable_monMode: " + disable_monMode + '\n');
            writer.write("\tdeauthWait: " + Integer.toString(deauthWait) + '\n');
            writer.write("\tshowLog: " + Boolean.toString(showLog) + '\n');
            writer.write("\tshow_notif: " + Boolean.toString(show_notif) + '\n');
            writer.write("\tshow_details: " + Boolean.toString(show_details) + '\n');
            writer.write("\tairOnStartup: " + Boolean.toString(airOnStartup) + '\n');
            writer.write("\tdebug: " + Boolean.toString(debug) + '\n');
            writer.write("\tconfirm_exit: " + Boolean.toString(confirm_exit) + '\n');
            writer.write("\tdelete_extra: " + Boolean.toString(delete_extra) + '\n');
            writer.write("\tmanuf_while_ados: " + Boolean.toString(manuf_while_ados) + '\n');

            // get input stream
            String cmd = " echo pref_file--------------------------------------; su -c cat " + path + "/../shared_prefs/com.hijacker_preferences.xml;";
            cmd += " echo ls_system-xbin---------------------------------; su -c ls /system/xbin -1| busybox grep -e air -e mdk -e reaver -e nexutil -e iw -e libfakeioctl.so;";
            cmd += " echo ls_su-xbin-------------------------------------; su -c ls /su/xbin -1| busybox grep -e air -e mdk -e reaver -e nexutil -e iw -e libfakeioctl.so;";
            cmd += " echo ls_vendor-lib----------------------------------; su -c ls /vendor/lib -1| busybox grep -e air -e mdk -e reaver -e nexutil -e iw -e libfakeioctl.so;";
            cmd += " echo ls_su-lib--------------------------------------; su -c ls /su/lib -1| busybox grep -e air -e mdk -e reaver -e nexutil -e iw -e libfakeioctl.so;";
            cmd += " echo ls_system-lib----------------------------------; su -c ls /system/lib -1| busybox grep -e air -e mdk -e reaver -e nexutil -e iw -e libfakeioctl.so;";
            cmd += " echo ls_capdir--------------------------------------; su -c ls " + cap_dir + " -1| busybox grep -e air -e mdk -e reaver -e nexutil -e iw -e libfakeioctl.so;";
            cmd += " echo fw_bcmdhd--------------------------------------; su -c strings /vendor/firmware/fw_bcmdhd.bin | grep \"FWID:\";";
            cmd += " echo ps---------------------------------------------; su -c ps | busybox grep -e air -e mdk -e reaver;";
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
    @Override
    public void onDestroy(){
        super.onDestroy();
        System.exit(1);
    }
}
