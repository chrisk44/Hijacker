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
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
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
    static String AUTH_KEY = "key-that-will-be-changed-in-the-release-build_this-is-for-testing";
    static String SERVER = "192.168.1.4";         //This will be a DNS resolvable name
    static int PORT = 1025;
    static final String REQ_VERSION = "version", REQ_INFO = "info", REQ_EXIT = "exit", REQ_REPORT = "report", REQ_FEEDBACK = "feedback", REQ_NEW_ID = "newid";
    static final String ANS_POSITIVE = "OK", ANS_NEGATIVE = "NO";
    static final int BUFFER_SIZE = 1048576;
    static final int AIREPLAY_DEAUTH = 1, AIREPLAY_WEP = 2;
    static final int FRAGMENT_AIRODUMP = 0, FRAGMENT_MDK = 1, FRAGMENT_CRACK = 2,
            FRAGMENT_REAVER = 3, FRAGMENT_CUSTOM=4, FRAGMENT_SETTINGS = 5;                     //These need to correspond to the items in the drawer
    static final int PROCESS_AIRODUMP=0, PROCESS_AIREPLAY=1, PROCESS_MDK=2, PROCESS_AIRCRACK=3, PROCESS_REAVER=4;
    static final int MDK_BF=0, MDK_ADOS=1;
    static final int SORT_NOSORT = 0, SORT_ESSID = 1, SORT_BEACONS_FRAMES = 2, SORT_DATA_FRAMES = 3, SORT_PWR = 4;
    static final int CHROOT_FOUND = 0, CHROOT_BIN_MISSING = 1, CHROOT_DIR_MISSING = 2, CHROOT_BOTH_MISSING = 3;
    //State variables
    static boolean wpacheckcont = false, completed = true, clearing = false;
    static boolean notif_on = false, background = false;    //notif_on: notification should be shown, background: the app is running in the background
    static int aireplay_running = 0, currentFragment=FRAGMENT_AIRODUMP;         //Set currentFragment in onResume of each Fragment
    //Filters
    static boolean show_ap = true, show_st = true, show_na_st = true, wpa = true, wep = true, opn = true;
    static boolean show_ch[] = {true, false, false, false, false, false, false, false, false, false, false, false, false, false, false};
    static int pwr_filter = 120;
    static int sort = SORT_NOSORT;
    static boolean sort_reverse = false;
    static boolean toSort = false;     //Variable to mark that the list must be sorted, so Tile.sort() must be called
    //Views that need to be accessible globally
    static TextView ap_count, st_count;                               //AP and ST count textviews in toolbar
    static ProgressBar progress;
    static Toolbar toolbar;
    static View rootView;
    static Drawable overflow[] = {null, null, null, null, null, null, null, null};      //Drawables to use for overflow button icon
    static ImageView[] status = {null, null, null, null, null};                         //Icons in TestDialog, set in TestDialog class
    static int progress_int;
    static long last_action;                        //Timestamp for the last action. Used in watchdog to avoid false positives
    static Thread wpa_thread, watchdog_thread;
    static Runnable wpa_runnable, watchdog_runnable;
    static Menu menu;
    static MyListAdapter adapter;
    static CustomActionAdapter custom_action_adapter;
    static FileExplorerAdapter file_explorer_adapter;
    static SharedPreferences pref;
    static SharedPreferences.Editor pref_edit;
    static ClipboardManager clipboard;
    static NotificationCompat.Builder notif, error_notif, handshake_notif;
    static NotificationManager mNotificationManager;
    static FragmentManager mFragmentManager;
    static String path, data_path, actions_path, firm_backup_file, arch, busybox;             //path: App files path (ends with .../files)
    //App and device info
    static String versionName, deviceModel;
    static int versionCode;
    static long deviceID;
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
    static boolean show_notif, show_details, airOnStartup, debug, delete_extra,
            monstart, always_cap, cont_on_fail, watchdog, target_deauth, enable_on_airodump, update_on_startup;

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
        file_explorer_adapter = new FileExplorerAdapter();
        file_explorer_adapter.setNotifyOnChange(true);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.my_toolbar));
        setup();

        if(debug) Log.d("HIJACKER/Main", "path is " + path);

        wpa_runnable = new Runnable(){
            @Override
            public void run(){
                if(debug) Log.d("HIJACKER/wpa_thread", "Started wpa_thread");

                Thread counter_thread = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        if(debug) Log.d("HIJACKER/wpa_subthread", "wpa_subthread started");
                        try{
                            progress_int = 0;
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
                            if(wpacheckcont){
                                runInHandler(new Runnable(){
                                    @Override
                                    public void run(){
                                        if(!background) Snackbar.make(findViewById(R.id.fragment1), getString(R.string.stopped_to_capture), Snackbar.LENGTH_SHORT).show();
                                        else Toast.makeText(MainActivity.this, getString(R.string.stopped_to_capture), Toast.LENGTH_SHORT).show();
                                        progress.setProgress(deauthWait);
                                        progress.setIndeterminate(true);
                                    }
                                });
                            }
                        }catch(InterruptedException e){
                            Log.e("HIJACKER/Exception", "Caught Exception in wpa_subthread: " + e.toString());
                            runInHandler(new Runnable(){
                                @Override
                                public void run(){
                                    progress.setIndeterminate(false);
                                    progress.setProgress(deauthWait);
                                }
                            });
                        }finally{
                            stop(PROCESS_AIREPLAY);
                        }
                        if(debug) Log.d("HIJACKER/wpa_subthread", "wpa_subthread finished");
                    }
                });

                boolean handshake_captured = false;
                final String capfile = Airodump.getCapFile();
                Shell shell = getFreeShell();
                try{
                    if(capfile==null){
                        if(debug) Log.d("HIJACKER/wpa_thread", "cap file not found, airodump is probably not running...");
                    }else{
                        if(debug) Log.d("HIJACKER/wpa_thread", capfile);
                        wpacheckcont = true;
                        counter_thread.start();

                        BufferedReader out = shell.getShell_out();
                        String buffer;
                        while(!handshake_captured && wpacheckcont){
                            //Check loop
                            if(debug) Log.d("HIJACKER/wpa_thread", "Checking cap file...");
                            shell.run(aircrack_dir + " " + capfile + "; echo ENDOFAIR");
                            buffer = out.readLine();
                            if(buffer==null) break;
                            else{
                                while(!buffer.equals("ENDOFAIR")){
                                    if(buffer.length()>=56){
                                        if(buffer.charAt(56)=='1' || buffer.charAt(56)=='2' || buffer.charAt(56)=='3'){
                                            handshake_captured = true;
                                            break;
                                        }
                                    }
                                    buffer = out.readLine();
                                }
                                Thread.sleep(700);
                            }
                        }
                    }
                }catch(IOException | InterruptedException e){
                    Log.e("HIJACKER/Exception", "Caught Exception in wpa_thread: " + e.toString());
                }finally{
                    wpacheckcont = false;
                    counter_thread.interrupt();
                    shell.done();
                    final boolean found = handshake_captured;
                    if(found) Airodump.startClean(is_ap);
                    runInHandler(new Runnable(){
                        @Override
                        public void run(){
                            Button crack_btn = (Button) findViewById(R.id.crack);
                            if(crack_btn!=null){
                                //We are in IsolatedFragment
                                crack_btn.setText(getString(R.string.crack));
                            }

                            if(found){
                                if(!background) Snackbar.make(findViewById(R.id.fragment1), getString(R.string.handshake_captured) + ' ' + capfile, Snackbar.LENGTH_LONG).show();
                                else{
                                    handshake_notif.setContentText(getString(R.string.saved_in_file) + ' ' + capfile);
                                    mNotificationManager.notify(2, handshake_notif.build());
                                }
                                progress.setIndeterminate(false);
                            }
                            if(debug) Log.d("HIJACKER/wpa_thread", "wpa_thread finished");
                        }
                    });
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
                        if(Airodump.isRunning() && list.size()==0){          //airodump not running
                            msg = new Message();
                            msg.obj = getString(R.string.airodump_not_running);
                            watchdog_handler.sendMessage(msg);
                            flag = false;
                            stop(PROCESS_AIRODUMP);
                        }else if(!Airodump.isRunning() && list.size()>0){     //airodump still running
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

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET
        }, 0);

        //Extract and verify oui.txt for Manufacturer look-up
        extract("oui.txt", path, false);

        //Thread to wait for the drawer to be initialized, so that the first option (airodump) can be highlighted
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

        //First start
        if(!pref.getBoolean("disclaimer", false)){
            new DisclaimerDialog().show(mFragmentManager, "Disclaimer");
            //Check for SuperSU
            if(!new File("/su").exists()){
                ErrorDialog dialog = new ErrorDialog();
                dialog.setTitle(getString(R.string.su_notfound_title));
                dialog.setMessage(getString(R.string.su_notfound));
                dialog.show(mFragmentManager, "ErrorDialog");
            }
        }else main();

        //Start background service so the app won't get killed if it goes to the background
        startService(new Intent(this, PersistenceService.class));

        if(update_on_startup){
            //Spawn new thread to wait for internet connection
            //This should be changed to a broadcast receiver
            new Thread(new Runnable(){
                @Override
                public void run(){
                    try{
                        while(!internetAvailable(MainActivity.this)){
                            Thread.sleep(1000);
                        }
                        checkForUpdate(MainActivity.this, false);
                    }catch(InterruptedException ignored){}
                }
            }).start();
        }

        //Delete old report, it's not needed if no exception is thrown up to this point
        File report = new File(Environment.getExternalStorageDirectory() + "/report.txt");
        if(report.exists()) report.delete();
    }
    void extract(String filename, String out_dir, boolean chmod){
        File f = new File(out_dir, filename);
        if(f.exists()) f.delete();          //Delete file in case it's outdated
        try{
            InputStream in = getResources().getAssets().open(filename);
            FileOutputStream out = new FileOutputStream(f);

            byte[] buf = new byte[BUFFER_SIZE];
            int len;
            while((len = in.read(buf))>0){
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            if(chmod){
                runOne("chmod 755 " + out_dir + "/" + filename);
            }
        }catch(IOException e){
            Log.e("HIJACKER/FileProvider", "Exception copying from assets", e);
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
        if(airOnStartup) Airodump.startClean();
    }
    void installTools(){
        File bin = new File(path + "/bin");
        File lib = new File(path + "/lib");
        if(!bin.exists()){
            if(!bin.mkdir()){
                ErrorDialog dialog = new ErrorDialog();
                dialog.setMessage(getString(R.string.bin_not_created));
                dialog.show(mFragmentManager, "ErrorDialog");
                return;
            }
        }
        if(!lib.exists()){
            if(!lib.mkdir()){
                ErrorDialog dialog = new ErrorDialog();
                dialog.setMessage(getString(R.string.lib_not_created));
                dialog.show(mFragmentManager, "ErrorDialog");
                return;
            }
        }
        PackageInfo info = null;
        try{
            info = getPackageManager().getPackageInfo(this.getPackageName(), 0);
        }catch(PackageManager.NameNotFoundException ignored){}
        if(bin.list().length==20 && lib.list().length==1 && info!=null){
            if(info.versionCode<=pref.getInt("tools_version", 0)){
                if(debug) Log.d("HIJACKER/installTools", "Tools already installed");
                return;
            }
        }
        String tools_location = path + "/bin/";
        String lib_location = path + "/lib/";
        extract("airbase-ng", tools_location, true);
        extract("aircrack-ng", tools_location, true);
        extract("aireplay-ng", tools_location, true);
        extract("airodump-ng", tools_location, true);
        extract("besside-ng", tools_location, true);
        extract("busybox", tools_location, true);
        extract("ivstools", tools_location, true);
        extract("iw", tools_location, true);
        extract("iwconfig", tools_location, true);
        extract("iwlist", tools_location, true);
        extract("iwpriv", tools_location, true);
        extract("kstats", tools_location, true);
        extract("makeivs-ng", tools_location, true);
        extract("mdk3", tools_location, true);
        extract("nc", tools_location, true);
        extract("packetforge-ng", tools_location, true);
        extract("reaver", tools_location, true);
        extract("reaver-wash", tools_location, true);
        extract("wesside-ng", tools_location, true);
        extract("wpaclean", tools_location, true);
        extract("libfakeioctl.so", lib_location, true);

        if(info!=null){
            pref_edit.putInt("tools_version", info.versionCode);
            pref_edit.commit();
        }
    }

    public static void _startAireplay(final String str){
        try{
            String cmd = "su -c " + prefix + " " + aireplay_dir + " -D --ignore-negative-one " + str + " " + iface;
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
    public static void startAireplay(String target, String client){
        //Disconnect client client from ap target
        aireplay_running = AIREPLAY_DEAUTH;
        _startAireplay("--deauth 0 -a " + target + " -c " + client);
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
        shell.run(busybox + " pidof " + process_name + "; echo ENDOFPIDOF");
        BufferedReader out = shell.getShell_out();
        String buffer = null;
        try{
            while(buffer==null) buffer = out.readLine();
            while(!buffer.equals("ENDOFPIDOF")){
                String[] temp = buffer.split(" ");
                try{
                    for(String tmp : temp){
                        list.add(Integer.parseInt(tmp));
                    }
                }catch(NumberFormatException e){
                    Log.e("HIJACKER/getPIDs", "Exception: " + e.toString());
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
                throw new UnsupportedOperationException("getPIDs() called with invalid pr code");
            }
    }
    public static void stop(int pr){
        //0 for airodump-ng, 1 for aireplay-ng, 2 for mdk, 3 for aircrack, 4 for reaver, everything else is considered pid and we kill it
        if(debug) Log.d("HIJACKER/stop", "stop(" + pr + ") called");
        last_action = System.currentTimeMillis();
        switch(pr){
            case PROCESS_AIRODUMP:
                Airodump.stop();
                return;
            case PROCESS_AIREPLAY:
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        if(menu!=null) menu.getItem(3).setEnabled(false);
                    }
                });
                progress_int = deauthWait;
                runOne(busybox + " kill $(" + busybox + " pidof aireplay-ng)");
                if(is_ap==null && aireplay_running==AIREPLAY_DEAUTH){
                    //Aireplay was just deauthenticating so airodump has locked a channel, no more needed
                    Airodump.startClean();
                }
                AP.currentTargetDeauth.clear();
                aireplay_running = 0;
                break;
            case PROCESS_MDK:
                if(currentFragment==FRAGMENT_MDK){
                    runInHandler(new Runnable(){
                        @Override
                        public void run(){
                            MDKFragment.bf_switch.setChecked(false);
                            MDKFragment.ados_switch.setChecked(false);
                        }
                    });
                }
                ados = false;
                bf = false;
                runOne(busybox + " kill $(" + busybox + " pidof mdk3)");
                break;
            case PROCESS_AIRCRACK:
                CrackFragment.cont = false;
                runOne(busybox + " kill $(" + busybox + " pidof aircrack-ng)");
                break;
            case PROCESS_REAVER:
                ReaverFragment.cont = false;
                runOne(busybox + " kill $(" + busybox + " pidof reaver)");
                break;
            default:
                runOne(busybox + " kill " + pr);
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
    public static void stopWPA(){
        wpacheckcont = false;
        wpa_thread.interrupt();
    }

    //Handlers used for tasks that require the Main thread to update the view, but need to be run by other threads
    public Handler watchdog_handler = new Handler(){
        public void handleMessage(Message msg){
            if(debug) Log.d("HIJACKER/watchdog", "Message is " + msg.obj);
            ErrorDialog dialog = new ErrorDialog();
            dialog.setTitle((String)msg.obj);
            dialog.setMessage(getString(R.string.watchdog_message));
            dialog.setWatchdog(true);
            dialog.show(mFragmentManager, "ErrorDialog");
        }
    };
    public static Handler handler = new Handler();
    public static void runInHandler(Runnable runnable){
        handler.post(runnable);
    }

    void setup(){
        PackageManager manager = this.getPackageManager();
        PackageInfo info;
        try{
            info = manager.getPackageInfo(this.getPackageName(), 0);
            versionName = info.versionName.replace(" ", "_");
            versionCode = info.versionCode;
        }catch(PackageManager.NameNotFoundException e){
            Log.e("HIJACKER/setup", e.toString());
        }
        deviceModel = Build.MODEL;
        if(!deviceModel.startsWith(Build.MANUFACTURER)) deviceModel = Build.MANUFACTURER + " " + deviceModel;
        deviceModel = deviceModel.replace(" ", "_");

        arch = System.getProperty("os.arch");
        ap_count = (TextView) findViewById(R.id.ap_count);
        st_count = (TextView) findViewById(R.id.st_count);
        progress = (ProgressBar) findViewById(R.id.progressBar);
        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        rootView = findViewById(R.id.fragment1);
        overflow[0] = getDrawable(R.drawable.overflow0);
        overflow[1] = getDrawable(R.drawable.overflow1);
        overflow[2] = getDrawable(R.drawable.overflow2);
        overflow[3] = getDrawable(R.drawable.overflow3);
        overflow[4] = getDrawable(R.drawable.overflow4);
        overflow[5] = getDrawable(R.drawable.overflow5);
        overflow[6] = getDrawable(R.drawable.overflow6);
        overflow[7] = getDrawable(R.drawable.overflow7);
        toolbar.setOverflowIcon(overflow[0]);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref_edit = pref.edit();
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mFragmentManager = getFragmentManager();
        actionBar = getSupportActionBar();
        path = getFilesDir().getAbsolutePath();
        data_path = Environment.getExternalStorageDirectory() + "/Hijacker";
        actions_path = data_path + "/actions";
        firm_backup_file = data_path + "/fw_bcmdhd.orig.bin";
        File data_dir = new File(data_path);
        if(!data_dir.exists()){
            //Create directory, subdirectories and files
            data_dir.mkdir();

            //Move app files from other directories in /Hijacker
            File firm_backup_old = new File(Environment.getExternalStorageDirectory() + "/fw_bcmdhd.orig.bin");
            if(firm_backup_old.exists()){
                firm_backup_old.renameTo(new File(data_path + "/fw_bcmdhd.orig.bin"));
            }

            File actions_dir_old = new File(Environment.getExternalStorageDirectory() + "/Hijacker-actions");
            if(actions_dir_old.exists()){
                actions_dir_old.renameTo(new File(actions_path));
            }else{
                new File(data_dir + "/actions").mkdir();
            }
        }

        deviceID = pref.getLong("deviceID", -1);

        //Load defaults
        iface = getString(R.string.iface);
        prefix = getString(R.string.prefix);
        cap_dir = getString(R.string.cap_dir);
        enable_monMode = getString(R.string.enable_monMode);
        disable_monMode = getString(R.string.disable_monMode);
        enable_on_airodump = Boolean.parseBoolean(getString(R.string.enable_on_airodump));
        deauthWait = Integer.parseInt(getString(R.string.deauthWait));
        show_notif = Boolean.parseBoolean(getString(R.string.show_notif));
        show_details = Boolean.parseBoolean(getString(R.string.show_details));
        airOnStartup = Boolean.parseBoolean(getString(R.string.airOnStartup));
        debug = Boolean.parseBoolean(getString(R.string.debug));
        delete_extra = Boolean.parseBoolean(getString(R.string.delete_extra));
        always_cap = Boolean.parseBoolean(getString(R.string.always_cap));
        chroot_dir = getString(R.string.chroot_dir);
        monstart = Boolean.parseBoolean(getString(R.string.monstart));
        custom_chroot_cmd = "";
        cont_on_fail = Boolean.parseBoolean(getString(R.string.cont_on_fail));
        watchdog = Boolean.parseBoolean(getString(R.string.watchdog));
        target_deauth = Boolean.parseBoolean(getString(R.string.target_deauth));
        update_on_startup = Boolean.parseBoolean(getString(R.string.auto_update));

        //Initialize notifications
            //Create intents
        Intent cancel_intent = new Intent(this, DismissReceiver.class);
        Intent stop_intent = new Intent(this, StopReceiver.class);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent click_intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            //Create 'running' notification
        notif = new NotificationCompat.Builder(this);
        notif.setContentTitle(getString(R.string.notification_title));
        notif.setContentText(" ");
        notif.setSmallIcon(R.drawable.ic_notification);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            notif.setColor(getColor(R.color.colorAccent));
        }
        notif.setDeleteIntent(PendingIntent.getBroadcast(this.getApplicationContext(), 0, cancel_intent, 0));
        notif.addAction(R.drawable.stop_drawable, getString(R.string.stop_attacks), PendingIntent.getBroadcast(this.getApplicationContext(), 0, stop_intent, 0));
        notif.setContentIntent(click_intent);

            //Crate 'error' notification (used by watchdog)
        error_notif = new NotificationCompat.Builder(this);
        error_notif.setContentTitle(getString(R.string.notification2_title));
        error_notif.setContentText("");
        error_notif.setSmallIcon(R.drawable.ic_notification);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            error_notif.setColor(getColor(android.R.color.holo_red_dark));
        }
        error_notif.setContentIntent(click_intent);
        error_notif.setVibrate(new long[]{500, 500});

            //Create 'handshake captured' notification (used by wpa_thread)
        handshake_notif = new NotificationCompat.Builder(this);
        handshake_notif.setContentTitle(getString(R.string.handshake_captured));
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            handshake_notif.setColor(getColor(android.R.color.holo_green_dark));
        }
        handshake_notif.setSmallIcon(R.drawable.ic_notification);
        handshake_notif.setContentIntent(click_intent);
        handshake_notif.setVibrate(new long[]{500, 500});

        //Load strings for when they cannot be retrived with getString or R.string...
        ST.not_connected = getString(R.string.not_connected);
        ST.paired = getString(R.string.paired) + ' ';
        ErrorDialog.notification2_title =  getString(R.string.notification2_title);

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

        //Load default fragment (airodump)
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.replace(R.id.fragment1, new MyListFragment());
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();

        //Google AppIndex
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        load();                     //Load preferences
        CustomAction.load();        //Load custom actions

        if(!(new File(path).exists())){
            Log.e("HIJACKER/onCreate", "App file directory doesn't exist");
            ErrorDialog dialog = new ErrorDialog();
            dialog.setMessage(getString(R.string.app_dir_notfound1) + path + getString(R.string.app_dir_notfound2));
            dialog.show(mFragmentManager, "ErrorDialog");
        }

        if(arch.equals("armv7l") || arch.equals("aarch64")){
            installTools();
            busybox = path + "/bin/busybox";

            prefix = "LD_PRELOAD=" + path + "/lib/libfakeioctl.so";
            airodump_dir = path + "/bin/airodump-ng";
            aireplay_dir = path + "/bin/aireplay-ng";
            aircrack_dir = path + "/bin/aircrack-ng";
            mdk3_dir = path + "/bin/mdk3";
            reaver_dir = path + "/bin/reaver";
        }else{
            Log.e("HIJACKER/onCreate", "Device not armv7l or aarch64, can't install tools");
            busybox = "busybox";
            ErrorDialog dialog = new ErrorDialog();
            dialog.setMessage(getString(R.string.not_armv7l));
            dialog.show(getFragmentManager(), "ErrorDialog");

            prefix = pref.getString("prefix", prefix);
            airodump_dir = "airodump-ng";
            aireplay_dir = "aireplay-ng";
            aircrack_dir = "aircrack-ng";
            mdk3_dir = "mdk3";
            reaver_dir = "reaver";
        }

        RootFile.init();
    }
    static void load(){
        //Load Preferences
        if(debug) Log.d("HIJACKER/load", "Loading preferences...");

        iface = pref.getString("iface", iface);
        if(!(arch.equals("armv7l") || arch.equals("aarch64"))){
            prefix = pref.getString("prefix", prefix);
        }
        deauthWait = Integer.parseInt(pref.getString("deauthWait", Integer.toString(deauthWait)));
        chroot_dir = pref.getString("chroot_dir", chroot_dir);
        monstart = pref.getBoolean("monstart", monstart);
        cap_dir = pref.getString("cap_dir", cap_dir);
        enable_monMode = pref.getString("enable_monMode", enable_monMode);
        disable_monMode = pref.getString("disable_monMode", disable_monMode);
        enable_on_airodump = pref.getBoolean("enable_on_airodump", enable_on_airodump);
        show_notif = pref.getBoolean("show_notif", show_notif);
        show_details = pref.getBoolean("show_details", show_details);
        airOnStartup = pref.getBoolean("airOnStartup", airOnStartup);
        debug = pref.getBoolean("debug", debug);
        watchdog = pref.getBoolean("watchdog", watchdog);
        target_deauth = pref.getBoolean("target_deauth", target_deauth);
        delete_extra = pref.getBoolean("delete_extra", delete_extra);
        try{
            always_cap = pref.getBoolean("always_cap", always_cap);
        }catch(ClassCastException e){
            pref_edit.putBoolean("always_cap", false);
            pref_edit.commit();
        }
        custom_chroot_cmd = pref.getString("custom_chroot_cmd", custom_chroot_cmd);
        cont_on_fail = pref.getBoolean("cont_on_fail", cont_on_fail);
        update_on_startup = pref.getBoolean("update_on_startup", update_on_startup);
        progress.setMax(deauthWait);
        progress.setProgress(deauthWait);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.reset:
                clearing = true;
                stop(PROCESS_AIRODUMP);
                Tile.clear();
                ap_count.setText("0");
                st_count.setText("0");
                Airodump.startClean();
                clearing = false;
                return true;

            case R.id.stop_run:
                if(Airodump.isRunning()){
                    stop(PROCESS_AIRODUMP);
                }else{
                    Airodump.start();
                }
                return true;

            case R.id.stop_aireplay:
                stop(PROCESS_AIREPLAY);
                return true;

            case R.id.filter:
                new FiltersDialog().show(mFragmentManager, "FiltersDialog");
                return true;

            case R.id.settings:
                if(currentFragment!=FRAGMENT_SETTINGS){
                    FragmentTransaction ft = mFragmentManager.beginTransaction();
                    ft.replace(R.id.fragment1, new SettingsFragment());
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.addToBackStack(null);
                    ft.commitAllowingStateLoss();
                }
                return true;

            case R.id.export:
                new ExportDialog().show(mFragmentManager, "ExportDialog");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onDestroy(){
        notif_on = false;
        mNotificationManager.cancelAll();
        CustomAction.save();
        watchdog_thread.interrupt();
        stop(PROCESS_AIRODUMP);
        stop(PROCESS_AIREPLAY);
        stop(PROCESS_MDK);
        stop(PROCESS_AIRCRACK);
        stop(PROCESS_REAVER);
        runOne(disable_monMode);
        RootFile.finish();
        Shell.exitAll();
        stopService(new Intent(this, PersistenceService.class));
        super.onDestroy();
        System.exit(0);
    }
    @Override
    protected void onResume(){
        super.onResume();
        notif_on = false;
        background = false;
        if(mNotificationManager!=null) mNotificationManager.cancelAll();
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
            }else if(mFragmentManager.getBackStackEntryCount()>1){
                mFragmentManager.popBackStackImmediate();
            }else{
                new ExitDialog().show(mFragmentManager, "ExitDialog");
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MainActivity.menu = menu;
        getMenuInflater().inflate(R.menu.toolbar, menu);
        if(airOnStartup){
            menu.getItem(1).setIcon(R.drawable.stop_drawable);
            menu.getItem(1).setTitle(R.string.stop);
        }
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
                FragmentTransaction ft = mFragmentManager.beginTransaction();
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
                mFragmentManager.executePendingTransactions();
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
                firstText.setTextColor(ContextCompat.getColor(getContext(), current.st.isMarked ? R.color.colorAccent : android.R.color.white));
            }else{
                if(current.ap.isHidden) iv.setImageResource(R.drawable.ap_hidden);
                else iv.setImageResource(R.drawable.ap2);
                firstText.setTextColor(ContextCompat.getColor(getContext(), current.ap.isMarked ? R.color.colorAccent : android.R.color.white));
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
            secondText.setText(currentItem.getStartCmd());

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
    class FileExplorerAdapter extends ArrayAdapter<RootFile>{
        FileExplorerAdapter(){
            super(MainActivity.this, R.layout.explorer_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            // get a view to work with
            View itemview = convertView;
            if(itemview==null){
                itemview = getLayoutInflater().inflate(R.layout.explorer_item, parent, false);
            }

            // find the item to work with
            RootFile currentItem = FileExplorerDialog.list.get(position);

            TextView firstText = (TextView) itemview.findViewById(R.id.explorer_item_tv);
            firstText.setText(currentItem.getName());

            //Image
            ImageView iv = (ImageView) itemview.findViewById(R.id.explorer_iv);
            if(currentItem.isFile()){
                iv.setImageResource(R.drawable.file);
            }else{
                iv.setImageResource(R.drawable.folder);
            }

            return itemview;
        }

        @Override
        public int getCount(){
            return FileExplorerDialog.list.size();
        }
    }
    public void onAPStats(View v){ new StatsDialog().show(mFragmentManager, "StatsDialog"); }
    public void onCrack(View v){
        //Clicked crack with isolated ap
        if(wpa_thread.isAlive()){
            //Clicked stop
            stopWPA();
            Airodump.startClean(is_ap);
        }else{
            //Clicked crack
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
        if(notif_on && show_notif && !(!Airodump.isRunning() && aireplay_running==0 &&
                !bf && !ados && !CrackFragment.cont && !ReaverFragment.cont)){
            if(show_details){
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
            }else notif.setContentText(null);
            mNotificationManager.notify(0, notif.build());
        }else{
            mNotificationManager.cancel(0);
        }
    }
    static void isolate(String mac){
        is_ap = AP.getAPByMac(mac);
        if(is_ap!=null){
            IsolatedFragment.exit_on = mFragmentManager.getBackStackEntryCount();
            FragmentTransaction ft = mFragmentManager.beginTransaction();
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
        int state = Airodump.isRunning() ? 1 : 0;
        if(aireplay_running!=0) state += 2;
        if(bf || ados) state += 4;
        toolbar.setOverflowIcon(overflow[state]);

        if(!(ReaverFragment.cont || CrackFragment.cont || wpa_thread.isAlive())){
            progress.setIndeterminate(false);
            progress.setProgress(deauthWait);
        }
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
    static String getManuf(String mac){
        String temp = mac.subSequence(0, 2).toString() + mac.subSequence(3, 5).toString() + mac.subSequence(6, 8).toString();
        Shell shell = getFreeShell();
        shell.run(busybox + " grep -m 1 -i " + temp + " " + path + "/oui.txt; echo ENDOFGREP");
        String manuf = getLastLine(shell.getShell_out(), "ENDOFGREP");
        shell.done();

        if(manuf.equals("ENDOFGREP") || manuf.length()<23) manuf = "Unknown Manufacturer";
        else manuf = manuf.substring(22);
        return manuf;
    }
    static String getLastLine(BufferedReader out, String end){
        //Returns the last line printed in out BEFORE end. If no other line is present, end is returned.
        String lastline=null, buffer = null;
        try{
            while(buffer==null) buffer = out.readLine();
            lastline = buffer;
            while(!end.equals(buffer) && buffer!=null){
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
    static String getDirectory(String str){
        //Returns a directory that ends with /
        if(str==null) return null;
        if(str.length()==0) return str;

        if(str.charAt(str.length()-1)=='/'){
            return str;
        }else return str + '/';
    }
    static String getFixed(String text, int size){
        /*Returns a string of fixed length (size) that contains spaces followed by a text
           <--  size  -->
          |     ...  text|
        */
        if(text==null) return null;
        if(text.length() > size){
            text = text.substring(0, size);
        }
        String str = "";
        for(int i=0;i < size-text.length();i++){
            str += " ";
        }
        return str + text;
    }
    static int checkChroot(){
        boolean bin = false, dir;

        Shell shell = getFreeShell();
        shell.run("echo $PATH; echo ENDOFPATH");
        String path = getLastLine(shell.getShell_out(), "ENDOFPATH");
        shell.done();
        String paths[] = path.split(":");
        for(String temp : paths){
            if(new RootFile(temp + "/bootkali_init").exists()){
                bin = true;
                break;
            }
        }

        dir = new RootFile(chroot_dir).exists() && new RootFile(chroot_dir + "/bin/bash").exists();

        if(bin && dir) return CHROOT_FOUND;
        else if(!bin && !dir) return CHROOT_BOTH_MISSING;
        else if(dir) return CHROOT_BIN_MISSING;
        else return CHROOT_DIR_MISSING;
    }
    static Socket connect(){
        //Don't call this in the main thread
        Socket socket;
        try{
            InetAddress ip = Inet4Address.getByName(SERVER);
            socket = new Socket(ip, PORT);

            PrintWriter in = new PrintWriter(socket.getOutputStream());
            BufferedReader out = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //Authenticate (receive a string generated by the server, combine it with a stored key and send back the hashcode)
            if(debug) Log.d("HIJACKER/connect", "Authenticating with server...");
            String temp = out.readLine() + AUTH_KEY;
            in.print(Integer.toString(temp.hashCode()) + '\n');
            in.flush();

            temp = out.readLine();
            if(temp!=null){
                if(!temp.equals(ANS_POSITIVE)) return null;     //Not authenticated, socket will be closed by the server
            }else return null;                                  //Connection closed, probably not authenticated
            if(debug) Log.d("HIJACKER/connect", "Authenticated");

            if(deviceID==-1){
                if(debug) Log.d("HIJACKER/connect", "Getting new deviceID...");
                in.print(REQ_NEW_ID + '\n');
                in.flush();

                try{
                    deviceID = Long.parseLong(out.readLine());
                    pref_edit.putLong("deviceID", deviceID);
                    pref_edit.commit();
                    if(debug) Log.d("HIJACKER/connect", "New deviceID is " + deviceID);
                }catch(NumberFormatException ignored){
                    if(debug) Log.d("HIJACKER/connect", "deviceID caused NumberFormatException, still -1");
                }
            }
            //String should be: info APP_VERSION_NAME APP_VERSION_CODE ANDROID_VERSION DEVICE_MODEL DEVICE_ID
            in.print(REQ_INFO + " " + versionName + " " + versionCode + " " + Build.VERSION.SDK_INT + " " + deviceModel + " " + deviceID + '\n');
            in.flush();
        }catch(IOException e){
            Log.e("HIJACKER/connect", e.toString());
            return null;
        }
        return socket;
    }
    static void checkForUpdate(final Activity activity, final boolean showMessages){
        if(showMessages) progress.setIndeterminate(true);
        new Thread(new Runnable(){
            @Override
            public void run(){
                if(showMessages) Looper.prepare();
                Runnable runnable = new Runnable(){
                    @Override
                    public void run(){
                        progress.setIndeterminate(false);
                    }
                };
                Socket socket = connect();
                if(socket==null){
                    if(showMessages){
                        handler.post(runnable);
                        Snackbar.make(activity.getCurrentFocus(), activity.getString(R.string.server_error), Snackbar.LENGTH_SHORT).show();
                    }
                    return;
                }

                try{
                    PrintWriter in = new PrintWriter(socket.getOutputStream());
                    BufferedReader out = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    in.print(REQ_VERSION + '\n');
                    in.flush();

                    int latestCode = Integer.parseInt(out.readLine());
                    String latestName = out.readLine();
                    String latestLink = out.readLine();

                    in.print(REQ_EXIT + '\n');
                    in.flush();
                    in.close();
                    out.close();
                    socket.close();

                    if(latestCode > versionCode){
                        UpdateConfirmDialog dialog = new UpdateConfirmDialog();
                        dialog.newVersionCode = latestCode;
                        dialog.newVersionName = latestName;
                        dialog.link = latestLink;
                        dialog.show(activity.getFragmentManager(), "UpdateConfirmDialog");
                    }else{
                        if(showMessages) Snackbar.make(activity.getCurrentFocus(), activity.getString(R.string.already_on_latest), Snackbar.LENGTH_SHORT).show();
                    }
                }catch(IOException | NumberFormatException e){
                    Log.e("HIJACKER/update", e.toString());
                    if(showMessages) Snackbar.make(activity.getCurrentFocus(), activity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
                }finally{
                    if(showMessages) handler.post(runnable);
                }
            }
        }).start();
    }
    static boolean internetAvailable(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getNetworkInfo(1).getState()==NetworkInfo.State.CONNECTED || connectivityManager.getNetworkInfo(0).getState()==NetworkInfo.State.CONNECTED;
    }
    static boolean createReport(File out, String filesDir, String stackTrace, Process shell){
        if(!out.exists()){
            try{
                if(!out.createNewFile()){
                    return false;
                }
            }catch(IOException e){
                Log.e("HIJACKER/createReport", e.toString());
                return false;
            }
        }
        String busybox_tmp = filesDir + "/bin/busybox";
        PrintWriter shell_in = new PrintWriter(shell.getOutputStream());
        BufferedReader shell_out = new BufferedReader(new InputStreamReader(shell.getInputStream()));

        FileWriter writer = null;
        try{
            writer = new FileWriter(out, true);
            writer.write("\n--------------------------------------------------------------------------------\n");
            writer.write("Hijacker report - " + new Date().toString() + "\n\n");
            writer.write("Android version: " +  Build.VERSION.SDK_INT + '\n');
            writer.write("Device: " + deviceModel + '\n');
            writer.write("App version: " + versionName + " (" + versionCode + ")\n");
            writer.write("App data path: " + filesDir + '\n');
            if(stackTrace!=null) writer.write("\nStack trace:\n" + stackTrace + '\n');

            String cmd = "echo pref_file--------------------------------------; cat /data/data/com.hijacker/shared_prefs/com.hijacker_preferences.xml;";
            cmd += " echo app directory----------------------------------; " + busybox_tmp + " ls -lR " + filesDir + ';';
            cmd += " echo fw_bcmdhd--------------------------------------; strings /vendor/firmware/fw_bcmdhd.bin | grep \"FWID:\";";
            cmd += " echo ps---------------------------------------------; ps | " + busybox_tmp + " grep -e air -e mdk -e reaver;";
            cmd += " echo busybox----------------------------------------; " + busybox_tmp + ";";
            cmd += " echo logcat-----------------------------------------; logcat -d -v time | " + busybox_tmp + " grep HIJACKER;";
            cmd += " exit\n";
            Log.d("HIJACKER/SendLog", cmd);
            shell_in.print(cmd);
            shell_in.flush();

            String buffer = shell_out.readLine();
            while(buffer!=null){
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
            return false;
        }
        return true;
    }

    static{
        System.loadLibrary("native-lib");
    }
}