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

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.hijacker.CustomAction.TYPE_ST;
import static com.hijacker.IsolatedFragment.is_ap;
import static com.hijacker.MDKFragment.ados;
import static com.hijacker.MDKFragment.ados_pid;
import static com.hijacker.MDKFragment.bf;
import static com.hijacker.MDKFragment.bf_pid;
import static com.hijacker.Shell.getFreeShell;
import static com.hijacker.Shell.runOne;

public class MainActivity extends AppCompatActivity{
    static final int AIREPLAY_DEAUTH = 1, AIREPLAY_WEP = 2;
    static final int FRAGMENT_AIRODUMP = 0, FRAGMENT_MDK = 1, FRAGMENT_CRACK = 2,
            FRAGMENT_REAVER = 3, FRAGMENT_CUSTOM=4, FRAGMENT_SETTINGS = 5;                     //These need to correspond to the items in the drawer
    static final int PROCESS_AIRODUMP=0, PROCESS_AIREPLAY=1, PROCESS_MDK=2, PROCESS_AIRCRACK=3, PROCESS_REAVER=4;
    static final int MDK_BF=0, MDK_ADOS=1;
    static final int SORT_NOSORT = 0, SORT_ESSID = 1, SORT_BEACONS_FRAMES = 2, SORT_DATA_FRAMES = 3, SORT_PWR = 4;
    //State variables
    static boolean cont = false, wpacheckcont = false, done = true;  //done: for calling refreshHandler only when it has stopped
    static boolean notif_on = false, background = false;    //notif_on: notification should be shown, background: the app is running in the background
    static int airodump_running = 0, aireplay_running = 0, currentFragment=FRAGMENT_AIRODUMP;         //Set currentFragment in onResume of each Fragment
    //Filters
    static boolean show_ap = true, show_st = true, show_na_st = true, wpa = true, wep = true, opn = true;
    static boolean show_ch[] = {true, false, false, false, false, false, false, false, false, false, false, false, false, false, false};
    static int pwr_filter = 120;
    static int sort = SORT_NOSORT;
    static boolean sort_reverse = false;
    static boolean toSort = false;     //Variable to mark that the list must be sorted, so Tile.sort() must be called
    static TextView ap_count, st_count;                               //AP and ST count textviews in toolbar
    static ProgressBar progress;
    static Toolbar toolbar;
    static Drawable overflow[] = {null, null, null, null, null, null, null, null};      //Drawables to use for overflow button icon
    static ImageView[] status = {null, null, null, null, null};                         //Icons in TestDialog, set in TestDialog class
    static int progress_int;
    static long last_action;
    static Thread refresh_thread, wpa_thread, watchdog_thread;
    static Runnable refresh_runnable, wpa_runnable, watchdog_runnable;
    static Menu menu;
    static List<Item2> fifo;                    //List used as FIFO for handling calls to addAP/addST in an order
    static MyListAdapter adapter;
    static CustomActionAdapter custom_action_adapter;
    static SharedPreferences pref;
    static SharedPreferences.Editor pref_edit;
    static ClipboardManager clipboard;
    static NotificationCompat.Builder notif, error_notif, handshake_notif;
    static NotificationManager nm;
    static FragmentManager fm;
    static String path, version, arch;             //App files path (ends with .../files)
    static boolean init=false;      //True on first run to swap the dialogs for initialization
    static ActionBar actionBar;
    private GoogleApiClient client;
    static String[] mPlanetTitles;
    static DrawerLayout mDrawerLayout;
    static ListView mDrawerList;
    //Preferences - Defaults are in strings.xml
    static String iface, prefix, airodump_dir, aireplay_dir, aircrack_dir, mdk3_dir, reaver_dir, cap_dir, chroot_dir,
            enable_monMode, disable_monMode, custom_chroot_cmd;
    static int deauthWait;
    static boolean show_notif, show_details, airOnStartup, debug, delete_extra, manuf_while_ados,
            monstart, always_cap, cont_on_fail, watchdog;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){
            @Override
            public void uncaughtException(Thread thread, Throwable throwable){
                throwable.printStackTrace();
                String stackTrace = "";
                stackTrace += throwable.getMessage() + '\n';
                for(int i=0;i<throwable.getStackTrace().length;i++){
                    stackTrace += throwable.getStackTrace()[i].toString() + '\n';
                }

                Intent intent = new Intent();
                intent.setAction("com.hijacker.SendLogActivity");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("exception", stackTrace);
                startActivity(intent);

                finish();
                System.exit(1);
            }
        });
        adapter = new MyListAdapter();              //ALWAYS BEFORE setContentView AND setup(), can't stress it enough...
        adapter.setNotifyOnChange(true);
        custom_action_adapter = new CustomActionAdapter();
        custom_action_adapter.setNotifyOnChange(true);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.my_toolbar));
        setup();

        refresh_runnable = new Runnable(){
            @Override
            public void run(){
                if(debug) Log.d("HIJACKER/refresh_thread", "refresh_thread running");
                try{
                    while(cont){
                        if(done) refreshHandler.obtainMessage().sendToTarget();
                        Thread.sleep(1000);
                    }
                }catch(InterruptedException e){ Log.e("HIJACKER/Exception", "Caught Exception in main() refresh_thread block: " + e.toString()); }
                if(debug) Log.d("HIJACKER/refresh_thread", "refresh_thread done");
            }
        };
        refresh_thread = new Thread(refresh_runnable);

        wpa_runnable = new Runnable(){
            @Override
            public void run(){
                if(debug) Log.d("HIJACKER/wpa_thread", "Started wpa_thread");

                Thread wpa_subthread = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        if(debug) Log.d("HIJACKER/wpa_subthread", "wpa_subthread started");
                        try{
                            while(progress_int<=deauthWait && wpacheckcont){
                                Thread.sleep(1000);
                                progress_int++;
                                runInHandler(new Runnable(){
                                    @Override
                                    public void run(){
                                        progress.setProgress(progress_int);
                                    }
                                });
                            }
                            runInHandler(new Runnable(){
                                @Override
                                public void run(){
                                    if(wpacheckcont){
                                        stop(PROCESS_AIREPLAY);
                                        if(!background) Snackbar.make(findViewById(R.id.fragment1), getString(R.string.stopped_to_capture), Snackbar.LENGTH_SHORT).show();
                                        else Toast.makeText(MainActivity.this, getString(R.string.stopped_to_capture), Toast.LENGTH_SHORT).show();
                                        progress.setIndeterminate(true);
                                    }else{
                                        progress.setIndeterminate(false);
                                        progress.setProgress(deauthWait);
                                    }
                                }
                            });
                        }catch(InterruptedException e){ Log.e("HIJACKER/Exception", "Caught Exception in wpa_subthread: " + e.toString()); }
                        if(debug) Log.d("HIJACKER/wpa_subthread", "wpa_subthread finished");
                    }
                });

                final String capfile;
                String buffer;
                int result = 0;
                Shell shell = getFreeShell();
                try{
                    Thread.sleep(1000);
                    shell.run("busybox ls -1 " + cap_dir + "/handshake-*.cap; echo ENDOFLS");
                    capfile = getLastLine(shell.getShell_out(), "ENDOFLS");

                    if(debug) Log.d("HIJACKER/wpa_thread", capfile);
                    if(capfile.equals("ENDOFLS")){
                        if(debug){
                            Log.d("HIJACKER/wpa_thread", "cap file not found, airodump is probably not running...");
                            Log.d("HIJACKER/wpa_thread", "Returning...");
                        }
                    }else{
                        Snackbar.make(findViewById(R.id.fragment1), getString(R.string.cap_is) + ' ' + capfile, Snackbar.LENGTH_LONG).show();
                        progress_int = 0;
                        wpacheckcont = true;
                        wpa_subthread.start();
                        while(result!=1 && wpacheckcont){
                            if(debug) Log.d("HIJACKER/wpa_thread", "Checking cap file...");
                            shell.run(aircrack_dir + " " + capfile + "; echo ENDOFAIR");
                            BufferedReader out = shell.getShell_out();
                            buffer = out.readLine();
                            if(buffer==null) wpacheckcont = false;
                            else{
                                while(!buffer.equals("ENDOFAIR")){
                                    if(result!=1) result = checkwpa(buffer);
                                    buffer = out.readLine();
                                }
                                Thread.sleep(700);
                            }
                        }
                        wpacheckcont = false;
                    }
                    final int temp = result;
                    runInHandler(new Runnable(){
                        @Override
                        public void run(){
                            if(temp==1){
                                stop(PROCESS_AIRODUMP);
                                stop(PROCESS_AIREPLAY);
                                if(!background) Snackbar.make(findViewById(R.id.fragment1), getString(R.string.handshake_captured) + ' ' + capfile, Snackbar.LENGTH_LONG).show();
                                else{
                                    handshake_notif.setContentText(getString(R.string.saved_in_file) + ' ' + capfile);
                                    nm.notify(2, handshake_notif.build());
                                }
                            }
                            progress.setIndeterminate(false);
                            progress.setProgress(deauthWait);
                            ((Button)findViewById(R.id.crack)).setText(getString(R.string.crack));
                        }
                    });
                }catch(IOException | InterruptedException e){
                    Log.e("HIJACKER/Exception", "Caught Exception in wpa_thread: " + e.toString());
                    stop(PROCESS_AIREPLAY);
                }finally{
                    wpa_subthread.interrupt();
                    if(delete_extra){
                        shell.run("rm -rf " + cap_dir + "/handshake-*.csv");
                        shell.run("rm -rf " + cap_dir + "/handshake-*.netxml");
                    }
                    if(debug) Log.d("HIJACKER/wpa_thread", "wpa_thread finished");
                    shell.done();
                }
            }
        };
        wpa_thread = new Thread(wpa_runnable);

        watchdog_runnable = new Runnable(){        //Thread to check whether the tools we think are running, are actually running
            @Override
            public void run(){
                try{
                    boolean flag = true;
                    while(flag){
                        Thread.sleep(5000);
                        while(System.currentTimeMillis()-last_action < 1000){
                            if(debug) Log.d("HIJACKER/watchdog", "Watchdog waiting for 1 sec...");
                            Thread.sleep(1000);
                        }
                        if(debug) Log.d("HIJACKER/watchdog", "Watchdog watching...");
                        List<Integer> list;
                        Message msg;
                        list = getPIDs(PROCESS_AIRODUMP);
                        if(airodump_running!=0 && list.size()==0){          //airodump not running
                            msg = new Message();
                            msg.obj = getString(R.string.airodump_not_running);
                            watchdog_handler.sendMessage(msg);
                            flag = false;
                            stop(PROCESS_AIRODUMP);
                        }else if(airodump_running==0 && list.size()>0){     //airodump still running
                            if(debug) Log.d("HIJACKER/watchdog", "Airodump is still running. Trying to kill it...");
                            stop(PROCESS_AIRODUMP);
                            if(getPIDs(PROCESS_AIRODUMP).size()>0){
                                msg = new Message();
                                msg.obj = getString(R.string.airodump_still_running);
                                watchdog_handler.sendMessage(msg);
                                flag = false;
                            }
                        }
                        list = getPIDs(PROCESS_AIREPLAY);
                        if(aireplay_running!=0 && list.size()==0){      //aireplay not running
                            msg = new Message();
                            msg.obj = getString(R.string.aireplay_not_running);
                            watchdog_handler.sendMessage(msg);
                            flag = false;
                            stop(PROCESS_AIREPLAY);
                        }else if(aireplay_running==0 && list.size()>0){ //aireplay still running
                            if(debug) Log.d("HIJACKER/watchdog", "Aireplay is still running. Trying to kill it...");
                            stop(PROCESS_AIREPLAY);
                            if(getPIDs(PROCESS_AIREPLAY).size()>0){
                                msg = new Message();
                                msg.obj = getString(R.string.aireplay_still_running);
                                watchdog_handler.sendMessage(msg);
                                flag = false;
                            }
                        }
                        list = getPIDs(PROCESS_MDK);
                        if((bf || ados) && list.size()==0){         //mdk not running
                            msg = new Message();
                            msg.obj = getString(R.string.mdk_not_running);
                            watchdog_handler.sendMessage(msg);
                            flag = false;
                            stop(PROCESS_MDK);
                        }else if(!(bf || ados) && list.size()>0){   //mdk still running
                            if(debug) Log.d("HIJACKER/watchdog", "MDK is still running. Trying to kill it...");
                            stop(PROCESS_MDK);
                            if(getPIDs(PROCESS_MDK).size()>0){
                                msg = new Message();
                                msg.obj = getString(R.string.mdk_still_running);
                                watchdog_handler.sendMessage(msg);
                                flag = false;
                            }
                        }
                        list = getPIDs(PROCESS_REAVER);
                        if(ReaverFragment.cont && list.size()==0){         //reaver not running
                            msg = new Message();
                            msg.obj = getString(R.string.reaver_not_running);
                            watchdog_handler.sendMessage(msg);
                            flag = false;
                            stop(PROCESS_REAVER);
                        }else if(!ReaverFragment.cont && list.size()>0){   //reaver still running
                            if(debug) Log.d("HIJACKER/watchdog", "Reaver is still running. Trying to kill it...");
                            stop(PROCESS_REAVER);
                            if(getPIDs(PROCESS_REAVER).size()>0){
                                msg = new Message();
                                msg.obj = getString(R.string.reaver_still_running);
                                watchdog_handler.sendMessage(msg);
                                flag = false;
                            }
                        }
                    }
                }catch(InterruptedException e){ Log.e("HIJACKER/watchdog", "Exception: " + e.toString()); }
            }
        };
        watchdog_thread = new Thread(watchdog_runnable);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)==PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 0);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)==PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CHANGE_WIFI_STATE}, 0);
        }

        extract("oui.txt", true);
        File oui = new File(path + "/oui.txt");
        if(!oui.exists()){
            ErrorDialog dialog = new ErrorDialog();
            dialog.setMessage(getString(R.string.oui_not_found));
            dialog.show(getFragmentManager(), "ErrorDialog");
        }

        if(arch.equals("armv7l")){
            if(new File("/su").exists()){
                if(debug) Log.d("HIJACKER/onCreate", "Installing busybox in /su/xbin...");
                extract("busybox", false);
                Shell shell = getFreeShell();
                shell.run("cp " + path + "/busybox /su/xbin/busybox");
                shell.run("chmod 755 /su/xbin/busybox");
                shell.done();
                if(debug) Log.d("HIJACKER/onCreate", "Installed busybox in /su/xbin");
            }else if(debug) Log.d("HIJACKER/onCreate", "No /su to install busybox");
        }else if(debug) Log.d("HIJACKER/onCreate", "Cannot install busybox, arch is " + arch);


        new Thread(new Runnable(){      //Thread to wait until the drawer is initialized and then highlight airodump
            @Override
            public void run(){
                try{
                    while(mDrawerList.getChildAt(0)==null){
                        Thread.sleep(100);
                    }
                    runInHandler(new Runnable(){
                        @Override
                        public void run(){
                            refreshDrawer();
                        }
                    });
                }catch(InterruptedException ignored){}
            }
        }).start();

        if(watchdog){
            watchdog_thread = new Thread(watchdog_runnable);
            watchdog_thread.start();
        }

        if(!pref.getBoolean("disclaimer", false)){
            new DisclaimerDialog().show(fm, "Disclaimer");
            File su = new File("/su");
            if(!su.exists()){
                ErrorDialog dialog = new ErrorDialog();
                dialog.setTitle(getString(R.string.su_notfound_title));
                dialog.setMessage(getString(R.string.su_notfound));
                dialog.show(getFragmentManager(), "ErrorDialog");
            }
        }else main();

        File report = new File(Environment.getExternalStorageDirectory() + "/report.txt");
        if(report.exists()) report.delete();    //Delete old report, it's not needed if no exception is thrown up to this point
    }
    void extract(String filename, boolean chmod){
        File f = new File(getFilesDir(), filename);
        if(!f.exists()){
            try{
                InputStream in = getResources().getAssets().open(filename);
                FileOutputStream out = new FileOutputStream(f);

                byte[] buf = new byte[1024];
                int len;
                while((len = in.read(buf))>0){
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                if(chmod){
                    runOne("chmod 744 ./files/" + filename);
                }
            }catch(IOException e){
                Log.e("HIJACKER/FileProvider", "Exception copying from assets", e);
            }
        }
    }
    public static void main(){
        runOne(enable_monMode);
        runOne("mkdir " + cap_dir);

        stop(PROCESS_AIRODUMP);
        stop(PROCESS_AIREPLAY);
        stop(PROCESS_MDK);
        stop(PROCESS_AIRCRACK);
        stop(PROCESS_REAVER);
        if(airOnStartup) startAirodump(null);
        else if(menu!=null) menu.getItem(1).setIcon(R.drawable.run);
    }

    public static void startAirodump(String params){
        final String temp;
        final int mode;
        if(params==null){
            temp = always_cap ? " -w " + cap_dir + "/cap" : "";
            mode = 0;
        }else{
            if(!(params.contains("handshake") || params.contains("wep_ivs")) && always_cap){
                temp = params + " -w " + cap_dir + "/cap";
            }else{
                temp = params;
            }
            mode = 1;
        }
        runOne(enable_monMode);
        stop(PROCESS_AIRODUMP);
        cont = true;
        new Thread(new Runnable(){
            @Override
            public void run(){
                String cmd = "su -c " + prefix + " " + airodump_dir + " --update 1 " + temp + " " + iface;
                if(debug) Log.d("HIJACKER/startAirodump", cmd);
                try{
                    Process process = Runtime.getRuntime().exec(cmd);
                    last_action = System.currentTimeMillis();
                    BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String buffer;
                    while(cont && (buffer = in.readLine())!=null){
                        main(buffer, mode);
                    }
                }catch(IOException e){ Log.e("HIJACKER/Exception", "Caught Exception in startAirodump() read block: " + e.toString()); }
            }
        }).start();
        refresh_thread = new Thread(refresh_runnable);
        refresh_thread.start();
        airodump_running = 1;
        runInHandler(new Runnable(){
            @Override
            public void run(){
                if(menu!=null) menu.getItem(1).setIcon(R.drawable.stop);
                refreshState();
                notification();
            }
        });
    }
    public static void startAirodumpForAP(AP ap, String extra){
        startAirodump("--channel " + ap.ch + " --bssid " + ap.mac + (extra==null ? "" : ' ' + extra));
    }

    public static void _startAireplay(final String str){
        try{
            String cmd = "su -c " + prefix + " " + aireplay_dir + " --ignore-negative-one " + str + " " + iface;
            if(debug) Log.d("HIJACKER/_startAireplay", cmd);
            Runtime.getRuntime().exec(cmd);
            last_action = System.currentTimeMillis();
        }catch(IOException e){ Log.e("HIJACKER/Exception", "Caught Exception in _startAireplay() start block: " + e.toString()); }
        runInHandler(new Runnable(){
            @Override
            public void run(){
                menu.getItem(3).setEnabled(true);       //Enable 'Stop aireplay' button
                refreshState();
                notification();
            }
        });
    }
    public static void startAireplay(String mac){
        //Disconnect all clients from mac
        aireplay_running = AIREPLAY_DEAUTH;
        _startAireplay("--deauth 0 -a " + mac);
    }
    public static void startAireplay(String mac1, String mac2){
        //Disconnect client mac2 from ap mac1
        aireplay_running = AIREPLAY_DEAUTH;
        _startAireplay("--deauth 0 -a " + mac1 + " -c " + mac2);
    }
    public static void startAireplayWEP(AP ap){
        //Increase IV generation from ap mac to crack a wep network
        aireplay_running = AIREPLAY_WEP;
        _startAireplay("--fakeauth 0 -a " + ap.mac + " -e " + ap.essid);
        //_startAireplay("--arpreplay -b " + ap.mac);       //Aireplay tries to open a file at a read-only system
        //_startAireplay("--caffe-latte -b " + ap.mac);     //don't know where
    }

    public static void startMdk(int mode, String str){
        ArrayList<Integer> ps_before = getPIDs(PROCESS_MDK);
        switch(mode){
            case MDK_BF:
                //beacon flood mode
                try{
                    String cmd = "su -c " + prefix + " " + mdk3_dir + " " + iface + " b -m ";
                    if(str!=null) cmd += str;
                    if(debug) Log.d("HIJACKER/MDK3", cmd);
                    Runtime.getRuntime().exec(cmd);
                    last_action = System.currentTimeMillis();
                    Thread.sleep(500);
                }catch(IOException | InterruptedException e){ Log.e("HIJACKER/Exception", "Caught Exception in startMdk(MDK_BF) start block: " + e.toString()); }
                bf = true;
                break;
            case MDK_ADOS:
                //Authentication DoS mode
                try{
                    String cmd = "su -c " + prefix + " " + mdk3_dir + " " + iface + " a -m";
                    cmd += str==null ? "" : " -i " + str;
                    if(debug) Log.d("HIJACKER/MDK3", cmd);
                    Runtime.getRuntime().exec(cmd);
                    last_action = System.currentTimeMillis();
                    Thread.sleep(500);
                }catch(IOException | InterruptedException e){ Log.e("HIJACKER/Exception", "Caught Exception in startMdk(MDK_ADOS) start block: " + e.toString()); }
                ados = true;
                break;
        }
        ArrayList<Integer> ps_after = getPIDs(PROCESS_MDK);
        int pid=0;
        if(ps_before.size()!=ps_after.size()){
            for(int i=0;i<ps_before.size();i++){
                if(!ps_before.get(i).equals(ps_after.get(i))){
                    pid = ps_after.get(i);
                }
            }
            if(pid==0) pid = ps_after.get(ps_after.size()-1);
        }
        switch(mode){
            case MDK_BF:
                bf_pid = pid;
                break;
            case MDK_ADOS:
                ados_pid = pid;
                break;
        }
        runInHandler(new Runnable(){
            @Override
            public void run(){
                refreshState();
                notification();
            }
        });
    }

    public static ArrayList<Integer> getPIDs(String process_name){
        if(process_name==null) return null;

        Shell shell = getFreeShell();
        ArrayList<Integer> list = new ArrayList<>();
        shell.run("busybox pidof " + process_name + "; echo ENDOFPIDOF");
        BufferedReader out = shell.getShell_out();
        String buffer = null;
        try{
            while(buffer==null) buffer = out.readLine();
            while(!buffer.equals("ENDOFPIDOF")){
                String[] temp = buffer.split(" ");
                for(String tmp : temp){
                    list.add(Integer.parseInt(tmp));
                }
                buffer = out.readLine();
            }
        }catch(IOException e){
            Log.e("HIJACKER/getPIDs", "Exception: " + e.toString());
            return null;
        }finally{
            shell.done();
        }
        return list;
    }
    public static ArrayList<Integer> getPIDs(int pr){
        switch(pr){
            case PROCESS_AIRODUMP:
                return getPIDs("airodump-ng");
            case PROCESS_AIREPLAY:
                return getPIDs("aireplay-ng");
            case PROCESS_MDK:
                return getPIDs("mdk3");
            case PROCESS_AIRCRACK:
                return getPIDs("aircrack-ng");
            case PROCESS_REAVER:
                return getPIDs("reaver");
            default:
                Log.e("HIJACKER/getPIDs", "Method called with invalid pr code");
                return null;
            }
    }
    public static void stop(int pr){
        //0 for airodump-ng, 1 for aireplay-ng, 2 for mdk, 3 for aircrack, 4 for reaver, everything else is considered pid and we kill it
        if(debug) Log.d("HIJACKER/stop", "stop(" + pr + ") called");
        switch(pr){
            case PROCESS_AIRODUMP:
                cont = false;
                refresh_thread.interrupt();
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        if(menu!=null) menu.getItem(1).setIcon(R.drawable.run);
                        Tile.filter();
                        if(wpa_thread.isAlive()) progress.setIndeterminate(false);
                    }
                });
                if(wpa_thread.isAlive()){
                    IsolatedFragment.cont = false;
                    wpacheckcont = false;
                    wpa_thread.interrupt();
                }
                if(delete_extra && always_cap){
                    runOne("busybox rm -rf " + cap_dir + "/cap-*.csv");
                    runOne("busybox rm -rf " + cap_dir + "/cap-*.netxml");
                }
                airodump_running = 0;
                runOne("busybox kill $(busybox pidof airodump-ng)");
                break;
            case PROCESS_AIREPLAY:
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        if(menu!=null) menu.getItem(3).setEnabled(false);
                        if(aireplay_running==AIREPLAY_WEP){
                            progress.setIndeterminate(false);
                        }
                    }
                });
                Shell shell = getFreeShell();
                if(delete_extra && aireplay_running==AIREPLAY_WEP){
                    shell.run("busybox rm -rf " + cap_dir + "/wep_ivs-*.csv");
                    shell.run("busybox rm -rf " + cap_dir + "/wep_ivs-*.netxml");
                }
                aireplay_running = 0;
                progress_int = deauthWait;
                shell.run("busybox kill $(busybox pidof aireplay-ng)");
                shell.done();
                break;
            case PROCESS_MDK:
                ados = false;
                bf = false;
                runOne("busybox kill $(busybox pidof mdk3)");
                break;
            case PROCESS_AIRCRACK:
                CrackFragment.cont = false;
                runOne("busybox kill $(busybox pidof aircrack-ng)");
                break;
            case PROCESS_REAVER:
                ReaverFragment.cont = false;
                runOne("busybox kill $(busybox pidof reaver)");
                break;
            default:
                runOne("busybox kill " + pr);
                break;
        }
        last_action = System.currentTimeMillis();
        runInHandler(new Runnable(){
            @Override
            public void run(){
                refreshState();
                notification();
            }
        });
    }

    //Handlers used for tasks that require the Main thread to update the view, but need to be run by other threads
    public static Handler refreshHandler = new Handler(){
        public void handleMessage(Message msg){
            done = false;
            while(fifo.size()>0){
                fifo.get(0).add();
                adapter.notifyDataSetChanged();             //for when data is changed, no new data added
                fifo.remove(0);
            }
            ap_count.setText(Integer.toString(is_ap==null ? Tile.i : 1));
            st_count.setText(Integer.toString(Tile.tiles.size() - Tile.i));
            notification();
            if(toSort && !background) Tile.sort();
            done = true;
        }
    };
    public Handler watchdog_handler = new Handler(){
        public void handleMessage(Message msg){
            if(debug) Log.d("HIJACKER/watchdog", "Message is " + msg.obj);
            ErrorDialog dialog = new ErrorDialog();
            dialog.setTitle((String)msg.obj);
            dialog.setMessage(getString(R.string.watchdog_message));
            dialog.setWatchdog(true);
            dialog.show(getFragmentManager(), "ErrorDialog");
        }
    };
    public static Handler handler = new Handler(){
        public void handleMessage(Message msg){
            ((Runnable)msg.obj).run();
        }
    };
    public static void runInHandler(Runnable runnable){
        Message msg = new Message();
        msg.obj = runnable;
        handler.sendMessage(msg);
    }

    void setup(){
        try{
            version = getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        }catch(PackageManager.NameNotFoundException e){ Log.e("setup()", "Exception: " + e.toString()); }
        arch = System.getProperty("os.arch");
        ap_count = (TextView) findViewById(R.id.ap_count);
        st_count = (TextView) findViewById(R.id.st_count);
        fifo = new ArrayList<>();
        progress = (ProgressBar) findViewById(R.id.progressBar);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref_edit = pref.edit();
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        fm = getFragmentManager();
        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        actionBar = getSupportActionBar();
        overflow[0] = getDrawable(R.drawable.overflow0);
        overflow[1] = getDrawable(R.drawable.overflow1);
        overflow[2] = getDrawable(R.drawable.overflow2);
        overflow[3] = getDrawable(R.drawable.overflow3);
        overflow[4] = getDrawable(R.drawable.overflow4);
        overflow[5] = getDrawable(R.drawable.overflow5);
        overflow[6] = getDrawable(R.drawable.overflow6);
        overflow[7] = getDrawable(R.drawable.overflow7);
        toolbar.setOverflowIcon(overflow[0]);

        //Load defaults
        iface = getString(R.string.iface);
        prefix = getString(R.string.prefix);
        aircrack_dir = getString(R.string.aircrack_dir);
        airodump_dir = getString(R.string.airodump_dir);
        aireplay_dir = getString(R.string.aireplay_dir);
        mdk3_dir = getString(R.string.mdk3_dir);
        reaver_dir = getString(R.string.reaver_dir);
        cap_dir = getString(R.string.cap_dir);
        enable_monMode = getString(R.string.enable_monMode);
        disable_monMode = getString(R.string.disable_monMode);
        deauthWait = Integer.parseInt(getString(R.string.deauthWait));
        show_notif = Boolean.parseBoolean(getString(R.string.show_notif));
        show_details = Boolean.parseBoolean(getString(R.string.show_details));
        airOnStartup = Boolean.parseBoolean(getString(R.string.airOnStartup));
        debug = Boolean.parseBoolean(getString(R.string.debug));
        delete_extra = Boolean.parseBoolean(getString(R.string.delete_extra));
        manuf_while_ados = Boolean.parseBoolean(getString(R.string.manuf_while_ados));
        always_cap = Boolean.parseBoolean(getString(R.string.always_cap));
        chroot_dir = getString(R.string.chroot_dir);
        monstart = Boolean.parseBoolean(getString(R.string.monstart));
        custom_chroot_cmd = "";
        cont_on_fail = Boolean.parseBoolean(getString(R.string.cont_on_fail));
        watchdog = Boolean.parseBoolean(getString(R.string.watchdog));

        //Initialize notifications
        notif = new NotificationCompat.Builder(this);
        notif.setContentTitle(getString(R.string.notification_title));
        notif.setContentText(" ");
        notif.setSmallIcon(R.drawable.ic_notification);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            notif.setColor(getColor(R.color.colorAccent));
        }

        Intent cancel_intent = new Intent(this, DismissReceiver.class);
        notif.setDeleteIntent(PendingIntent.getBroadcast(this.getApplicationContext(), 0, cancel_intent, 0));

        Intent stop_intent = new Intent(this, StopReceiver.class);
        notif.addAction(R.drawable.stop, getString(R.string.stop_attacks), PendingIntent.getBroadcast(this.getApplicationContext(), 0, stop_intent, 0));

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent click_intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notif.setContentIntent(click_intent);

        error_notif = new NotificationCompat.Builder(this);
        error_notif.setContentTitle(getString(R.string.notification2_title));
        error_notif.setContentText("");
        error_notif.setSmallIcon(R.drawable.ic_notification);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            error_notif.setColor(getColor(android.R.color.holo_red_dark));
        }
        error_notif.setContentIntent(click_intent);
        error_notif.setVibrate(new long[]{500, 500});

        handshake_notif = new NotificationCompat.Builder(this);
        handshake_notif.setContentTitle(getString(R.string.handshake_captured));
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            handshake_notif.setColor(getColor(android.R.color.holo_green_dark));
        }
        handshake_notif.setSmallIcon(R.drawable.ic_notification);
        handshake_notif.setContentIntent(click_intent);
        handshake_notif.setVibrate(new long[]{500, 500});

        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        load();

        //Load strings for when they cannot be retrived with getString or R.string...
        ST.not_connected = getString(R.string.not_connected);
        ST.paired = getString(R.string.paired);
        ErrorDialog.notification2_title = getString(R.string.notification2_title);

        //Initialize the drawer
        mPlanetTitles = getResources().getStringArray(R.array.planets_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.drawer_list_item, R.id.navDrawerTv, mPlanetTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerList.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener(){
            @Override                   //Works only for the first run
            public void onSystemUiVisibilityChange(int visibility){
                mDrawerList.getChildAt(currentFragment).setBackgroundResource(R.color.colorAccent);
            }
        });

        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment1, new MyListFragment());
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();

        //Google AppIndex
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        path = getFilesDir().getAbsolutePath();
        CustomAction.load();
        if(debug) Log.d("HIJACKER/Main", "path is " + path);
        if(!(new File(path).exists())){
            Log.e("HIJACKER/onCreate", "App file directory doesn't exist");
            ErrorDialog dialog = new ErrorDialog();
            dialog.setMessage(getString(R.string.app_dir_notfound1) + path + getString(R.string.app_dir_notfound2));
            dialog.show(fm, "ErrorDialog");
        }
    }
    static void load(){
        //Load Preferences
        if(debug) Log.d("HIJACKER/load", "Loading preferences...");

        iface = pref.getString("iface", iface);
        prefix = pref.getString("prefix", prefix);
        deauthWait = Integer.parseInt(pref.getString("deauthWait", Integer.toString(deauthWait)));
        airodump_dir = pref.getString("airodump_dir", airodump_dir);
        aireplay_dir = pref.getString("aireplay_dir", aireplay_dir);
        aircrack_dir = pref.getString("aircrack_dir", aircrack_dir);
        mdk3_dir = pref.getString("mdk3_dir", mdk3_dir);
        reaver_dir = pref.getString("reaver_dir", reaver_dir);
        chroot_dir = pref.getString("chroot_dir", chroot_dir);
        monstart = pref.getBoolean("monstart", monstart);
        cap_dir = pref.getString("cap_dir", cap_dir);
        enable_monMode = pref.getString("enable_monMode", enable_monMode);
        disable_monMode = pref.getString("disable_monMode", disable_monMode);
        show_notif = pref.getBoolean("show_notif", show_notif);
        show_details = pref.getBoolean("show_details", show_details);
        airOnStartup = pref.getBoolean("airOnStartup", airOnStartup);
        debug = pref.getBoolean("debug", debug);
        watchdog = pref.getBoolean("watchdog", watchdog);
        delete_extra = pref.getBoolean("delete_extra", delete_extra);
        manuf_while_ados = pref.getBoolean("manuf_while_ados", manuf_while_ados);
        always_cap = pref.getBoolean("always_cap", always_cap);
        custom_chroot_cmd = pref.getString("custom_chroot_cmd", custom_chroot_cmd);
        cont_on_fail = pref.getBoolean("cont_on_fail", cont_on_fail);
        progress.setMax(deauthWait);
        progress.setProgress(deauthWait);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.reset:
                Tile.clear();
                ap_count.setText("0");
                st_count.setText("0");
                stop(PROCESS_AIRODUMP);
                if(is_ap==null) startAirodump(null);
                else startAirodumpForAP(is_ap, null);
                return true;

            case R.id.stop_run:
                if(cont){
                    //Running
                    stop(PROCESS_AIRODUMP);
                }else{
                    if(is_ap==null) startAirodump(null);
                    else startAirodumpForAP(is_ap, null);
                }
                return true;

            case R.id.stop_aireplay:
                stop(PROCESS_AIREPLAY);
                return true;

            case R.id.filter:
                new FiltersDialog().show(fm, "FiltersDialog");
                return true;

            case R.id.settings:
                if(currentFragment!=FRAGMENT_SETTINGS){
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.replace(R.id.fragment1, new SettingsFragment());
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.addToBackStack(null);
                    ft.commitAllowingStateLoss();
                }
                return true;

            case R.id.export:
                new ExportDialog().show(getFragmentManager(), "ExportDialog");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onDestroy(){
        notif_on = false;
        nm.cancelAll();
        CustomAction.save();
        watchdog_thread.interrupt();
        stop(PROCESS_AIRODUMP);
        stop(PROCESS_AIREPLAY);
        stop(PROCESS_MDK);
        stop(PROCESS_AIRCRACK);
        stop(PROCESS_REAVER);
        runOne(disable_monMode);
        Shell.exitAll();
        super.onDestroy();
        System.exit(0);
    }
    @Override
    protected void onResume(){
        super.onResume();
        notif_on = false;
        background = false;
        if(nm!=null) nm.cancelAll();
    }
    @Override
    protected void onStop(){
        super.onStop();
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        background = true;
        if(show_notif){
            notif_on = true;
            notification();
        }
        client.disconnect();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode==KeyEvent.KEYCODE_BACK){
            if(mDrawerLayout.isDrawerOpen(mDrawerList)){
                mDrawerLayout.closeDrawer(mDrawerList);
            }else if(getFragmentManager().getBackStackEntryCount()>1){
                getFragmentManager().popBackStackImmediate();
            }else{
                new ExitDialog().show(fm, "ExitDialog");
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        MainActivity.menu = menu;
        getMenuInflater().inflate(R.menu.toolbar, menu);
        if(!airOnStartup) menu.getItem(1).setIcon(R.drawable.run);
        menu.getItem(3).setEnabled(false);
        return true;
    }

    // See https://g.co/AppIndexing/AndroidStudio for more information.
    @Override
    public void onStart(){
        super.onStart();
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }
    public Action getIndexApiAction(){
        Thing object = new Thing.Builder().setName("AirodumpGUI")
                .setUrl(Uri.parse("https://github.com/chrisk44/Hijacker")).build();
        return new Action.Builder(Action.TYPE_VIEW).setObject(object).setActionStatus(Action.STATUS_TYPE_COMPLETED).build();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(currentFragment!=position){
                FragmentTransaction ft = fm.beginTransaction();
                switch(position){
                    case FRAGMENT_AIRODUMP:
                        ft.replace(R.id.fragment1, is_ap==null ? new MyListFragment() : new IsolatedFragment());
                        break;
                    case FRAGMENT_MDK:
                        ft.replace(R.id.fragment1, new MDKFragment());
                        break;
                    case FRAGMENT_CRACK:
                        ft.replace(R.id.fragment1, new CrackFragment());
                        break;
                    case FRAGMENT_REAVER:
                        ft.replace(R.id.fragment1, new ReaverFragment());
                        break;
                    case FRAGMENT_CUSTOM:
                        ft.replace(R.id.fragment1, new CustomActionFragment());
                        break;
                    case FRAGMENT_SETTINGS:
                        ft.replace(R.id.fragment1, new SettingsFragment());
                        break;
                }
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
                fm.executePendingTransactions();
            }
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }
    class MyListAdapter extends ArrayAdapter<Tile>{
        MyListAdapter(){
            super(MainActivity.this, R.layout.listitem);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            // get a view to work with
            View itemview = convertView;
            if(itemview==null){
                itemview = getLayoutInflater().inflate(R.layout.listitem, parent, false);
            }

            // find the item to work with
            Tile current = Tile.tiles.get(position);

            TextView firstText = (TextView) itemview.findViewById(R.id.top_left);
            firstText.setText(current.s1);

            TextView secondText = (TextView) itemview.findViewById(R.id.bottom_left);
            secondText.setText(current.s2);

            TextView thirdText = (TextView) itemview.findViewById(R.id.bottom_right);
            thirdText.setText(current.s3);

            TextView text4 = (TextView) itemview.findViewById(R.id.top_right);
            text4.setText(current.s4);

            //Image
            ImageView iv = (ImageView) itemview.findViewById(R.id.iv);
            if(!current.type){
                iv.setImageResource(R.drawable.st2);
            }else{
                if(current.ap.isHidden) iv.setImageResource(R.drawable.ap_hidden);
                else iv.setImageResource(R.drawable.ap2);
            }

            return itemview;
        }

        @Override
        public int getCount(){
            return Tile.tiles.size();
        }
    }
    class CustomActionAdapter extends ArrayAdapter<CustomAction>{
        CustomActionAdapter(){
            super(MainActivity.this, R.layout.listitem);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            // get a view to work with
            View itemview = convertView;
            if(itemview==null){
                itemview = getLayoutInflater().inflate(R.layout.listitem, parent, false);
            }

            // find the item to work with
            CustomAction currentItem = CustomAction.cmds.get(position);

            TextView firstText = (TextView) itemview.findViewById(R.id.top_left);
            firstText.setText(currentItem.getTitle());

            TextView secondText = (TextView) itemview.findViewById(R.id.bottom_left);
            secondText.setText(currentItem.getStart_cmd());

            TextView thirdText = (TextView) itemview.findViewById(R.id.bottom_right);
            thirdText.setText("");

            TextView text4 = (TextView) itemview.findViewById(R.id.top_right);
            text4.setText("");

            //Image
            ImageView iv = (ImageView) itemview.findViewById(R.id.iv);
            if(currentItem.getType()==TYPE_ST){
                iv.setImageResource(R.drawable.st2);
            }else{
                iv.setImageResource(R.drawable.ap2);
            }

            return itemview;
        }

        @Override
        public int getCount(){
            return CustomAction.cmds.size();
        }
    }
    public static void addAP(String essid, String mac, String enc, String cipher, String auth, int pwr, int beacons, int data, int ivs, int ch){
        fifo.add(new Item2(essid, mac, enc, cipher, auth, pwr, beacons, data, ivs, ch));
    }
    public static void addST(String mac, String bssid, int pwr, int lost, int frames){
        fifo.add(new Item2(mac, bssid, pwr, lost, frames));
    }
    public void onAPStats(View v){ new StatsDialog().show(fm, "StatsDialog"); }
    public void onCrack(View v){
        //Clicked crack with isolated ap
        if(wpa_thread.isAlive()){
            wpa_thread.interrupt();
            stop(PROCESS_AIRODUMP);
            stop(PROCESS_AIREPLAY);
            ((TextView)v).setText(R.string.crack);
            progress.setIndeterminate(false);
            progress.setProgress(deauthWait);
            startAirodumpForAP(is_ap, null);
        }else{
            is_ap.crack();
            ((TextView)v).setText(R.string.stop);
        }
    }
    public void onDisconnect(View v){
        //Clicked disconnect all with isolated ap
        stop(PROCESS_AIREPLAY);
        startAireplay(is_ap.mac);
        Toast.makeText(this, R.string.disconnect_started, Toast.LENGTH_SHORT).show();
    }
    public void onDos(View v){
        //Clicked dos with isolated ap
        if(ados){
            ((TextView)v).setText(R.string.dos);
            stop(ados_pid);
            ados = false;
        }else{
            ((TextView)v).setText(R.string.stop);
            startMdk(MDK_ADOS, is_ap.mac);
        }
    }
    public void onCopy(View v){
        copy(((TextView)v).getText().toString(), v);
    }
    static void copy(String str, View view){
        clipboard.setPrimaryClip(ClipData.newPlainText("label", str));
        if(view!=null) Snackbar.make(view, "\"" + str + "\" copied to clipboard", Snackbar.LENGTH_SHORT).show();
    }
    static void notification(){
        if(notif_on && show_notif && !(airodump_running==0 && aireplay_running==0 &&
                !bf && !ados && !CrackFragment.cont && !ReaverFragment.cont)){
            String str;
            if(is_ap==null) str = "APs: " + Tile.i + " | STs: " + (Tile.tiles.size() - Tile.i);
            else str = is_ap.essid + " | STs: " + (Tile.tiles.size() - Tile.i);

            if(show_details){
                if(aireplay_running==AIREPLAY_DEAUTH) str += " | Aireplay deauthenticating...";
                else if(aireplay_running==AIREPLAY_WEP) str += " | Aireplay replaying for wep...";
                if(wpa_thread.isAlive()) str += " | WPA cracking...";
                if(bf) str += " | MDK3 Beacon Flooding...";
                if(ados) str += " | MDK3 Authentication DoS...";
                if(ReaverFragment.cont) str += " | Reaver running...";
                if(CrackFragment.cont) str += " | Cracking .cap file...";
                if(CustomActionFragment.cont) str += " | Running action " + CustomActionFragment.selected_action.getTitle() + "...";
            }

            notif.setContentText(str);
            nm.notify(0, notif.build());
        }else{
            nm.cancel(0);
        }
    }
    static void isolate(String mac){
        is_ap = AP.getAPByMac(mac);
        if(is_ap!=null){
            IsolatedFragment.exit_on = fm.getBackStackEntryCount();
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.fragment1, new IsolatedFragment());
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.addToBackStack(null);
            ft.commitAllowingStateLoss();
        }
        Tile.filter();
        if(debug){
            if(is_ap==null) Log.d("HIJACKER/Main", "No AP isolated");
            else Log.d("HIJACKER/Main", "AP with MAC " + mac + " isolated");
        }
    }
    static void refreshState(){
        //refresh overflow icon to show what is running
        int state = airodump_running;
        if(aireplay_running!=0) state += 2;
        if(bf || ados) state += 4;
        toolbar.setOverflowIcon(overflow[state]);

        if(!(ReaverFragment.cont || CrackFragment.cont || wpa_thread.isAlive())){
            progress.setIndeterminate(false);
            progress.setProgress(deauthWait);
        }
    }
    static String getManuf(String mac){
        if(ados && !manuf_while_ados) return "ADoS running";

        String temp = mac.subSequence(0, 2).toString() + mac.subSequence(3, 5).toString() + mac.subSequence(6, 8).toString();
        Shell shell = getFreeShell();
        shell.run("busybox grep -m 1 -i " + temp + " " + path + "/oui.txt; echo ENDOFGREP");
        String manuf = getLastLine(shell.getShell_out(), "ENDOFGREP");
        shell.done();

        if(manuf.equals("ENDOFGREP") || manuf.length()<23) manuf = "Unknown Manufacturer";
        else manuf = manuf.substring(22);
        return manuf;
    }
    static String getLastLine(BufferedReader out, String end){
        String lastline=null, buffer = null;
        try{
            while(buffer==null) buffer = out.readLine();
            lastline = buffer;
            while(!end.equals(buffer)){
                lastline = buffer;
                buffer = out.readLine();
            }
        }catch(IOException e){ Log.e("HIJACKER/Exception", "Exception in getLastLine: " + e); }

        return lastline;
    }
    static String getLastSeen(long lastseen){
        String str = "";
        long diff = System.currentTimeMillis() - lastseen;
        if(diff < 1000) return "Just now";
        diff = diff/1000; //diff is now seconds
        if(diff/60>0){
            //minutes = diff/60
            str += diff/60 + " minute" + (diff/60 > 1 ? "s " : " ");
            diff = diff%60;
        }
        if(diff > 0){
            str += diff + " second" + (diff > 1 ? "s " : " ");
        }
        str += "ago";
        return str;
    }
    static void refreshDrawer(){
        if(mDrawerList.getChildAt(0)!=null){        //Ensure that the Drawer is initialized
            for(int i = 0; i<6; i++){
                mDrawerList.getChildAt(i).setBackgroundResource(R.color.colorPrimary);
            }
            mDrawerList.getChildAt(currentFragment).setBackgroundResource(R.color.colorAccent);
        }
        actionBar.setTitle(mPlanetTitles[currentFragment]);
    }

    static{
        System.loadLibrary("native-lib");
    }
    public static native int ps(String str);
    public static native boolean aireplay(String buf);
    public static native int main(String str, int off);
    public static native int checkwpa(String str);
}