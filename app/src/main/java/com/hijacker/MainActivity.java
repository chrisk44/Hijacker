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

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import static com.hijacker.AP.getAPByMac;
import static com.hijacker.CustomAction.TYPE_ST;
import static com.hijacker.Device.trimMac;
import static com.hijacker.IsolatedFragment.is_ap;
import static com.hijacker.MDKFragment.ados;
import static com.hijacker.MDKFragment.bf;
import static com.hijacker.Shell.getFreeShell;
import static com.hijacker.Shell.runOne;

public class MainActivity extends AppCompatActivity{
    static final String NETHUNTER_BOOTKALI_BASH = "/data/data/com.offsec.nethunter/files/scripts/bootkali_bash";
    static final String RELEASES_LINK = "https://api.github.com/repos/chrisk44/Hijacker/releases";
    static final String WORDLISTS_LINK = "https://api.github.com/repos/chrisk44/Hijacker/contents/wordlists";
    static final int BUFFER_SIZE = 1048576;
    static final int MAX_READLINE_SIZE = 10000;
    static final int AIREPLAY_DEAUTH = 1, AIREPLAY_WEP = 2;
    static final int BAND_2 = 1, BAND_5 = 2, BAND_BOTH = 3;
    static final int FRAGMENT_AIRODUMP = R.id.nav_airodump, FRAGMENT_MDK = R.id.nav_mdk3, FRAGMENT_CRACK = R.id.nav_crack,
            FRAGMENT_REAVER = R.id.nav_reaver, FRAGMENT_CUSTOM = R.id.nav_custom_actions, FRAGMENT_SETTINGS = R.id.nav_settings;
    static final int PROCESS_AIRODUMP=0, PROCESS_AIREPLAY=1, PROCESS_MDK_BF=2, PROCESS_MDK_DOS=3, PROCESS_AIRCRACK=4, PROCESS_REAVER=5;
    static final int SORT_NOSORT = 0, SORT_ESSID = 1, SORT_BEACONS_FRAMES = 2, SORT_DATA_FRAMES = 3, SORT_PWR = 4;
    static final int CHROOT_FOUND = 0, CHROOT_BIN_MISSING = 1, CHROOT_DIR_MISSING = 2, CHROOT_BOTH_MISSING = 3;
    //State variables
    static boolean wpacheckcont = false;
    static boolean notif_on = false, background = false;    //notif_on: notification should be shown, background: the app is running in the background
    static int aireplay_running = 0, currentFragment = FRAGMENT_AIRODUMP;         //Set currentFragment in onResume of each Fragment
    //Filters
    static boolean show_ap = true, show_st = true, show_na_st = true, wpa = true, wep = true, opn = true;
    static boolean show_ch[] = {true, false, false, false, false, false, false, false, false, false, false, false, false, false, false};
    static int pwr_filter = 120;
    static String manuf_filter = "";
    //Airodump list sort 
    static int sort = SORT_NOSORT;
    static boolean sort_reverse = false;
    static boolean toSort = false;     //Variable to mark that the list must be sorted, so Tile.sort() must be called
    //Views that need to be accessible globally
    static TextView ap_count, st_count;                               //AP and ST count textviews in toolbar
    static ProgressBar progress;
    static Toolbar toolbar;
    static View rootView;
    static DrawerLayout mDrawerLayout;
    static NavigationView navigationView;
    static SparseArray<String> navTitlesMap = new SparseArray<>();             //SparseArray to map fragment IDs to their respective navigation titles
    static Drawable overflow[] = {null, null, null, null, null, null, null, null};      //Drawables to use for overflow button icon
    static ImageView status[] = {null, null, null, null, null};                         //Icons in TestDialog, set in TestDialog class
    static int progress_int;
    static long last_action;                        //Timestamp for the last action. Used in watchdog to avoid false positives
    static Thread wpa_thread;
    static Runnable wpa_runnable;
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
    static String path, data_path, actions_path, wl_path, cap_path, firm_backup_file, manufDBFile, arch, busybox;             //path: App files path (ends with .../files)
    static File aliases_file;
    static FileWriter aliases_in;
    static final HashMap<String, String> aliases = new HashMap<>();
    static HashMap<String, String> manufHashMap;
    //App and device info
    static String versionName, deviceModel;
    static int versionCode;
    static String devChipset = "";
    static boolean init = false;      //True on first run to swap the dialogs for initialization
    static ActionBar actionBar;
    static String bootkali_init_bin = "bootkali_init";
    //Preferences - Defaults are in strings.xml
    static String iface, prefix, airodump_dir, aireplay_dir, aircrack_dir, mdk3bf_dir, mdk3dos_dir, reaver_dir, chroot_dir,
            enable_monMode, disable_monMode, custom_chroot_cmd;
    static int deauthWait, band;
    static boolean show_notif, show_details, airOnStartup, debug, delete_extra, show_client_count,
            monstart, always_cap, cont_on_fail, watchdog, target_deauth, enable_on_airodump, update_on_startup;

    private GoogleApiClient client;
    WatchdogTask watchdogTask;

    ReaverFragment reaverFragment = new ReaverFragment();
    CrackFragment crackFragment = new CrackFragment();
    CustomActionFragment customActionFragment = new CustomActionFragment();
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

        //Google AppIndex
        client = new GoogleApiClient.Builder(MainActivity.this).addApi(AppIndex.API).build();

        new SetupTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    private class SetupTask extends AsyncTask<Void, String, Boolean>{
        LoadingDialog loadingDialog;
        @Override
        protected void onPreExecute(){
            pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            pref_edit = pref.edit();
            if(!pref.getBoolean("disclaimerAccepted", false)){
                //First start
                new DisclaimerDialog().show(getFragmentManager(), "Disclaimer");
                //Check for SuperSU
                if(!new File("/su").exists()){
                    ErrorDialog dialog = new ErrorDialog();
                    dialog.setTitle(getString(R.string.su_notfound_title));
                    dialog.setMessage(getString(R.string.su_notfound));
                    dialog.show(getFragmentManager(), "ErrorDialog");
                }
            }

            loadingDialog = new LoadingDialog();
            loadingDialog.setInitText(getString(R.string.starting_hijacker));
            loadingDialog.show(getFragmentManager(), "LoadingDialog");

            //Initialize the drawer
            mDrawerLayout = findViewById(R.id.drawer_layout);
            navigationView = findViewById(R.id.nav_view);
            navigationView.getMenu().getItem(0).setChecked(true);
            navigationView.setNavigationItemSelectedListener(
                    new NavigationView.OnNavigationItemSelectedListener() {
                        @Override
                        public boolean onNavigationItemSelected(MenuItem menuItem) {
                            // set item as selected to persist highlight
                            menuItem.setChecked(true);
                            // close drawer when item is tapped
                            mDrawerLayout.closeDrawers();

                            if(currentFragment!=menuItem.getItemId()){
                                FragmentTransaction ft = mFragmentManager.beginTransaction();
                                switch(menuItem.getItemId()){
                                    case FRAGMENT_AIRODUMP:
                                        ft.replace(R.id.fragment1, is_ap==null ? new MyListFragment() : new IsolatedFragment());
                                        break;
                                    case FRAGMENT_MDK:
                                        ft.replace(R.id.fragment1, new MDKFragment());
                                        break;
                                    case FRAGMENT_REAVER:
                                        ft.replace(R.id.fragment1, reaverFragment);
                                        break;
                                    case FRAGMENT_CRACK:
                                        ft.replace(R.id.fragment1, crackFragment);
                                        break;
                                    case FRAGMENT_CUSTOM:
                                        ft.replace(R.id.fragment1, customActionFragment);
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

                            actionBar.setTitle(navTitlesMap.get(currentFragment));

                            return true;
                        }
                    });

            //Initialize toolbar
            toolbar = findViewById(R.id.my_toolbar);
            setSupportActionBar(toolbar);
            toolbar.setOverflowIcon(overflow[0]);

            ActionBar actionbar = getSupportActionBar();
            if(actionbar!=null){
                actionbar.setDisplayHomeAsUpEnabled(true);
                actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
            }else{
                Log.e("HIJACKER/SetupPreEx", "actionbar is null");
            }
        }
        @Override
        protected Boolean doInBackground(Void... params){
            Looper.prepare();
            //Initialize managers
            publishProgress(getString(R.string.init_managers));
            clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            mFragmentManager = getFragmentManager();

            //Load device information
            publishProgress(getString(R.string.loading_device_information));
            PackageManager manager = MainActivity.this.getPackageManager();
            PackageInfo info = null;
            try{
                info = manager.getPackageInfo(MainActivity.this.getPackageName(), 0);
                versionName = info.versionName.replace(" ", "_");
                versionCode = info.versionCode;
            }catch(PackageManager.NameNotFoundException e){
                Log.e("HIJACKER/SetupTask", e.toString());
            }
            deviceModel = Build.MODEL;
            if(!deviceModel.startsWith(Build.MANUFACTURER)) deviceModel = Build.MANUFACTURER + " " + deviceModel;
            deviceModel = deviceModel.replace(" ", "_");
            //devChipset is set later because busybox needs to be extracted
            arch = System.getProperty("os.arch");

            //Find views
            publishProgress(getString(R.string.init_views));
            ap_count = findViewById(R.id.ap_count);
            st_count = findViewById(R.id.st_count);
            progress = findViewById(R.id.progressBar);
            rootView = findViewById(R.id.fragment1);
            overflow[0] = getDrawable(R.drawable.overflow0);
            overflow[1] = getDrawable(R.drawable.overflow1);
            overflow[2] = getDrawable(R.drawable.overflow2);
            overflow[3] = getDrawable(R.drawable.overflow3);
            overflow[4] = getDrawable(R.drawable.overflow4);
            overflow[5] = getDrawable(R.drawable.overflow5);
            overflow[6] = getDrawable(R.drawable.overflow6);
            overflow[7] = getDrawable(R.drawable.overflow7);
            actionBar = getSupportActionBar();

            //Load defaults
            publishProgress(getString(R.string.loading_defaults));
            iface = getString(R.string.iface);
            prefix = getString(R.string.prefix);
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
            band = Integer.parseInt(getString(R.string.band));
            show_client_count = Boolean.parseBoolean(getString(R.string.show_client_count));

            //Load preferences
            publishProgress(getString(R.string.loading_preferences));
            load();

            //Initialize paths
            publishProgress(getString(R.string.init_files));
            path = getFilesDir().getAbsolutePath();
            data_path = Environment.getExternalStorageDirectory() + "/Hijacker";
            actions_path = data_path + "/actions";
            wl_path = data_path + "/wordlists";
            cap_path = data_path + "/capture_files";
            firm_backup_file = data_path + "/fw_bcmdhd.orig.bin";
            manufDBFile = path + "/manuf.db";
            ArrayList<File> dirs = new ArrayList<>();
            dirs.add(new File(data_path));
            dirs.add(new File(actions_path));
            dirs.add(new File(wl_path));
            dirs.add(new File(cap_path));
            for(File dir : dirs){
                if(!dir.exists()){
                    dir.mkdir();
                }
            }
            //cap file directory used to be set by the user, so move everything to the new location
            if(pref.contains("cap_dir")){
                //Move capture files to new directory
                String old_dir = pref.getString("cap_dir", null);
                Toast.makeText(MainActivity.this, "Moving cap files from " + old_dir + " to " + cap_path, Toast.LENGTH_SHORT).show();
                runOne(" mv " + old_dir + "/* " + cap_path + "/ && rmdir " + old_dir);

                pref_edit.remove("cap_dir");
                pref_edit.apply();
            }else{
                //cap directory was never changed so there may be files in /sdcard/cap/
                File old_dir = new File("/sdcard/cap");
                if(old_dir.exists() && old_dir.isDirectory()){
                    File files[] = old_dir.listFiles();
                    if(files!=null){
                        Toast.makeText(MainActivity.this, "Moving cap files from " + old_dir.getAbsolutePath() + " to " + cap_path, Toast.LENGTH_LONG).show();
                        for(File f : old_dir.listFiles()){
                            //Move all the files to the new directory
                            f.renameTo(new File(cap_path, f.getName()));
                        }
                    }
                    old_dir.delete();
                }
            }

            //Initialize notifications
            publishProgress(getString(R.string.init_notifications));
            //Create intents
            Intent cancel_intent = new Intent(MainActivity.this, DismissReceiver.class);
            Intent stop_intent = new Intent(MainActivity.this, StopReceiver.class);
            Intent notificationIntent = new Intent(MainActivity.this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            PendingIntent click_intent = PendingIntent.getActivity(MainActivity.this, 0, notificationIntent, 0);

            //Create 'running' notification
            notif = new NotificationCompat.Builder(MainActivity.this);
            notif.setContentTitle(getString(R.string.notification_title));
            notif.setContentText(" ");
            notif.setSmallIcon(R.drawable.ic_notification);
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                notif.setColor(getColor(R.color.colorAccent));
            }
            notif.setDeleteIntent(PendingIntent.getBroadcast(MainActivity.this.getApplicationContext(), 0, cancel_intent, 0));
            notif.addAction(R.drawable.stop_drawable, getString(R.string.stop_attacks), PendingIntent.getBroadcast(MainActivity.this.getApplicationContext(), 0, stop_intent, 0));
            notif.setContentIntent(click_intent);

            //Create 'error' notification (used by watchdog)
            error_notif = new NotificationCompat.Builder(MainActivity.this);
            error_notif.setSmallIcon(R.drawable.ic_notification);
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                error_notif.setColor(getColor(android.R.color.holo_red_dark));
            }
            error_notif.setContentIntent(click_intent);
            error_notif.setVibrate(new long[]{500, 500});

            //Create 'handshake captured' notification (used by wpa_thread)
            handshake_notif = new NotificationCompat.Builder(MainActivity.this);
            handshake_notif.setContentTitle(getString(R.string.handshake_captured));
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                handshake_notif.setColor(getColor(android.R.color.holo_green_dark));
            }
            handshake_notif.setSmallIcon(R.drawable.ic_notification);
            handshake_notif.setContentIntent(click_intent);
            handshake_notif.setVibrate(new long[]{500, 500});

            //Load strings for when they cannot be retrieved with getString or R.string
            publishProgress(getString(R.string.loading_strings));
            ST.not_connected = getString(R.string.not_connected);
            ST.paired = getString(R.string.paired) + ' ';

            //Setup tools
            publishProgress(getString(R.string.setting_up_tools));
            if(arch.equals("armv7l") || arch.equals("aarch64")) {
                String tools_location = path + "/bin/";
                String lib_location = path + "/lib/";

                //Create directories
                File bin = new File(path + "/bin");
                File lib = new File(path + "/lib");
                if(!bin.exists()){
                    if(!bin.mkdir()){
                        publishProgress(null, getString(R.string.bin_not_created));
                        return false;
                    }
                }
                if(!lib.exists()){
                    if(!lib.mkdir()){
                        publishProgress(null, getString(R.string.lib_not_created));
                        return false;
                    }
                }

                //Extract busybox
                extract("busybox", tools_location, true);
                busybox = path + "/bin/busybox";

                //Extract tools
                boolean install = true;
                if(bin.list().length==21 && lib.list().length==2 && info!=null){
                    if(info.versionCode==pref.getInt("tools_version", 0)){
                        if(debug) Log.d("HIJACKER/SetupTask", "Tools already installed");
                        install = false;
                    }else{
                        File manufDB = new File(manufDBFile);
                        if(manufDB.exists()) manufDB.delete();
                    }
                }
                if(install){
                    extract("airbase-ng", tools_location, true);
                    extract("aircrack-ng", tools_location, true);
                    extract("aireplay-ng", tools_location, true);
                    extract("airodump-ng", tools_location, true);
                    extract("besside-ng", tools_location, true);
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
                    extract("libnexmon.so", lib_location, true);

                    runOne("cd " + path + "/bin; mv mdk3 mdk3bf; cp mdk3bf mdk3dos");

                    if(info!=null){
                        pref_edit.putInt("tools_version", info.versionCode);
                        pref_edit.apply();
                    }
                }

                //Detect device chipset
                publishProgress(getString(R.string.detecting_device_chipset));
                Shell shell = getFreeShell();

                String firmwarePath = findFirmwarePath(shell);
                if(firmwarePath!=null){
                    //Get chipset from firmware file
                    shell.run("strings " + firmwarePath + " | " + busybox + " grep \"FWID:\"; echo ENDOFSTRINGS");
                    devChipset = getLastLine(shell.getShell_out(), "ENDOFSTRINGS");
                    int index = devChipset.indexOf('-');
                    if(index != -1){
                        devChipset = devChipset.substring(0, index);
                    }
                }
                Log.i("HIJACKER/DetectDev", "devChipset is " + devChipset);
                shell.done();

                //Set directories
                prefix = "LD_PRELOAD=" + path + "/lib/";
                if(devChipset.startsWith("4339")) {
                    //BCM4339
                    prefix += "libfakeioctl.so";
                }else if(devChipset.startsWith("4358")){
                    //BCM4358
                    prefix += "libnexmon.so";
                }else{
                    //Default (detected but not included)
                    SettingsFragment.allow_prefix = true;       //Allow user to change the prefix
                    prefix = pref.getString("prefix", null);    //Use user-set prefix

                    if(prefix==null){
                        //No user-set prefix, use default
                        prefix = "LD_PRELOAD=" + path + "/lib/libfakeioctl.so";
                    }
                }

                airodump_dir = path + "/bin/airodump-ng";
                aireplay_dir = path + "/bin/aireplay-ng";
                aircrack_dir = path + "/bin/aircrack-ng";
                mdk3bf_dir = path + "/bin/mdk3bf";
                mdk3dos_dir = path + "/bin/mdk3dos";
                reaver_dir = path + "/bin/reaver";
            }else{
                Log.e("HIJACKER/onCreate", "Device not armv7l or aarch64, can't install tools");
                busybox = "busybox";
                publishProgress(null, getString(R.string.not_armv7l));

                prefix = pref.getString("prefix", prefix);
                airodump_dir = "airodump-ng";
                aireplay_dir = "aireplay-ng";
                aircrack_dir = "aircrack-ng";
                mdk3bf_dir = "mdk3";
                mdk3dos_dir = "mdk3";
                reaver_dir = "reaver";
            }

            //Initialize RootFile
            publishProgress(getString(R.string.init_rootFile));
            RootFile.init();

            //Initialize threads
            publishProgress(getString(R.string.init_threads));
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
                                Button crack_btn = findViewById(R.id.crack);
                                if(crack_btn!=null){
                                    //We are in IsolatedFragment
                                    crack_btn.setText(getString(R.string.crack));
                                }

                                if(found){
                                    if(!background){
                                        Snackbar s = Snackbar.make(findViewById(R.id.fragment1), getString(R.string.handshake_captured) + ' ' + capfile, Snackbar.LENGTH_LONG);
                                        s.setAction(R.string.crack, new View.OnClickListener(){
                                            @Override
                                            public void onClick(View v){
                                                CrackFragment.capfile_text = capfile;
                                                FragmentTransaction ft = mFragmentManager.beginTransaction();
                                                ft.replace(R.id.fragment1, MainActivity.this.crackFragment);
                                                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                                                ft.addToBackStack(null);
                                                ft.commitAllowingStateLoss();
                                            }
                                        });
                                        s.show();
                                    }else{
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
            watchdogTask = new WatchdogTask(MainActivity.this);

            //Start background service so the app won't get killed if it goes to the background
            publishProgress(getString(R.string.starting_pers_service));
            startService(new Intent(MainActivity.this, PersistenceService.class));

            //Load manufacturer HashMap
            publishProgress(getString(R.string.loading_manuf_db));
            manufHashMap = new HashMap<>();
            File db = new File(manufDBFile);
            if(!db.exists()){
                //Database has not been created
                try{
                    //Create the database
                    if(!db.createNewFile()){
                        Log.e("HIJACKER/SetupTask", "Error creating database file");
                    }else{
                        if(debug) Log.d("HIJACKER/SetupTask", "Creating manufacturer database...");
                        publishProgress(getString(R.string.building_manuf_db));
                        BufferedReader out = new BufferedReader(new InputStreamReader(getResources().getAssets().open("oui.txt")));
                        FileWriter in = new FileWriter(db);

                        //Load data from oui.txt and write to the new database file
                        String buffer = out.readLine();
                        while(buffer!=null){
                            if(buffer.length()<18 || !buffer.contains("(base 16)")){
                                buffer = out.readLine();
                                continue;
                            }

                            String mac = buffer.substring(0, 6);
                            String manuf = buffer.substring(22);
                            if(manufHashMap.get(mac)==null){
                                //Write to file only if it was added to the HashMap (it's unique)
                                manufHashMap.put(mac, manuf);
                                in.write(mac + ";" + manuf + '\n');
                            }

                            buffer = out.readLine();
                        }
                        in.close();
                        out.close();
                        if(debug) Log.d("HIJACKER/SetupTask", "Manufacturer database built");
                    }
                }catch(IOException e){
                    Log.e("HIJACKER/SetupTask", e.toString());
                    manufHashMap = null;
                }
            }else{
                //Load database on the HashMap
                try{
                    //Database format:
                    //01B256;Manufacturer co.\n
                    BufferedReader out = new BufferedReader(new FileReader(db));

                    String buffer = out.readLine();
                    while(buffer!=null){
                        String mac = buffer.substring(0, buffer.indexOf(';'));
                        String manuf = buffer.substring(buffer.indexOf(';')+1);
                        manufHashMap.put(mac, manuf);
                        buffer = out.readLine();
                    }
                }catch(IOException e){
                    Log.e("HIJACKER/SetupTask", e.toString());
                    manufHashMap = null;
                }
            }

            //Load navigation titles to HashMap
            navTitlesMap.put(R.id.nav_airodump, getString(R.string.nav_airodump));
            navTitlesMap.put(R.id.nav_mdk3, getString(R.string.nav_mdk3));
            navTitlesMap.put(R.id.nav_reaver, getString(R.string.nav_reaver));
            navTitlesMap.put(R.id.nav_crack, getString(R.string.nav_crack));
            navTitlesMap.put(R.id.nav_custom_actions, getString(R.string.nav_custom_actions));
            navTitlesMap.put(R.id.nav_settings, getString(R.string.nav_settings));

            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
                //Load custom actions
                publishProgress(getString(R.string.loading_custom_actions));
                CustomAction.load();

                //Create or read aliases file
                publishProgress(getString(R.string.loading_aliases));
                loadAliases();
            }

            //Checking permissions
            publishProgress(getString(R.string.checking_permissions));
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.INTERNET
            }, 0);

            //Check for updates
            if(update_on_startup){
                if(internetAvailable(MainActivity.this)){
                    publishProgress(getString(R.string.checking_for_updates));
                    checkForUpdate(MainActivity.this, false);
                }else{
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
            }

            //Delete old report, it's not needed if no exception is thrown up to this point
            publishProgress(getString(R.string.deleting_bug_report));
            File report = new File(Environment.getExternalStorageDirectory() + "/report.txt");
            if(report.exists()) report.delete();

            return true;
        }
        @Override
        protected void onProgressUpdate(String... progress){
            if(progress[0]!=null){
                //Update loading dialog with progress[0]
                loadingDialog.setText(progress[0]);
            }else if(progress[1]!=null){
                //Show error message with progress[1]
                ErrorDialog dialog = new ErrorDialog();
                dialog.setMessage(progress[1]);
                dialog.show(getFragmentManager(), "ErrorDialog");
            }
        }
        @Override
        protected void onPostExecute(final Boolean success){
            if(!success) return;
            loadingDialog.setText(getString(R.string.starting_hijacker));

            if(watchdog){
                watchdogTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            //Load default fragment (airodump)
            if(mFragmentManager.getBackStackEntryCount()==0){
                FragmentTransaction ft = mFragmentManager.beginTransaction();
                ft.replace(R.id.fragment1, new MyListFragment());
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
            }

            loadingDialog.dismissAllowingStateLoss();

            //Start
            if(pref.getBoolean("disclaimerAccepted", false)) main();
        }
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
    public void main(){
        runOne(enable_monMode);

        stop(PROCESS_AIRODUMP);
        stop(PROCESS_AIREPLAY);
        stop(PROCESS_MDK_BF);
        stop(PROCESS_MDK_DOS);
        stop(PROCESS_AIRCRACK);
        stop(PROCESS_REAVER);
        if(airOnStartup) Airodump.startClean();
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

    //There are 2 mdk3 binaries with different names, so the app can easily stop each one separately
    public static void startBeaconFlooding(String str){
        try{
            String cmd = "su -c " + prefix + " " + mdk3bf_dir + " " + iface + " b -m ";
            if(str!=null) cmd += str;
            if(debug) Log.d("HIJACKER/MDK3", cmd);
            Runtime.getRuntime().exec(cmd);
        }catch(IOException e){ Log.e("HIJACKER/startBF", e.toString()); }
        last_action = System.currentTimeMillis();
        bf = true;

        runInHandler(new Runnable(){
            @Override
            public void run(){
                refreshState();
                notification();
            }
        });
    }
    public static void startAdos(String str){
        try{
            String cmd = "su -c " + prefix + " " + mdk3dos_dir + " " + iface + " a -m";
            cmd += str==null ? "" : " -i " + str;
            if(debug) Log.d("HIJACKER/MDK3", cmd);
            Runtime.getRuntime().exec(cmd);
        }catch(IOException e){ Log.e("HIJACKER/startAdos", e.toString()); }
        last_action = System.currentTimeMillis();
        ados = true;

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
            list = null;
        }
        shell.done();
        return list;
    }
    public static ArrayList<Integer> getPIDs(int pr){
        switch(pr){
            case PROCESS_AIRODUMP:
                return getPIDs("airodump-ng");
            case PROCESS_AIREPLAY:
                return getPIDs("aireplay-ng");
            case PROCESS_MDK_BF:
                return getPIDs("mdk3bf");
            case PROCESS_MDK_DOS:
                return getPIDs("mdk3dos");
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
                if(is_ap==null && aireplay_running==AIREPLAY_DEAUTH && Airodump.isRunning()){
                    //Aireplay was just deauthenticating so airodump has locked a channel, no more needed
                    Airodump.startClean();
                }
                AP.currentTargetDeauth.clear();
                aireplay_running = 0;
                break;
            case PROCESS_MDK_BF:
                if(currentFragment==FRAGMENT_MDK){
                    runInHandler(new Runnable(){
                        @Override
                        public void run(){
                            MDKFragment.bf_switch.setChecked(false);
                        }
                    });
                }

                bf = false;
                runOne(busybox + " kill $(" + busybox + " pidof mdk3bf)");
                break;
            case PROCESS_MDK_DOS:
                if(currentFragment==FRAGMENT_MDK){
                    runInHandler(new Runnable(){
                        @Override
                        public void run(){
                            MDKFragment.ados_switch.setChecked(false);
                        }
                    });
                }

                ados = false;
                runOne(busybox + " kill $(" + busybox + " pidof mdk3dos)");
            case PROCESS_AIRCRACK:
                CrackFragment.stopCracking();
                runOne(busybox + " kill $(" + busybox + " pidof aircrack-ng)");
                break;
            case PROCESS_REAVER:
                ReaverFragment.stopReaver();
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

    public static Handler handler = new Handler();
    public static void runInHandler(Runnable runnable){
        handler.post(runnable);
    }

    static void load(){
        //Load Preferences
        Log.d("HIJACKER/load", "Loading preferences...");

        iface = pref.getString("iface", iface);
        if(!(arch.equals("armv7l") || arch.equals("aarch64"))){
            prefix = pref.getString("prefix", prefix);
        }
        deauthWait = Integer.parseInt(pref.getString("deauthWait", Integer.toString(deauthWait)));
        chroot_dir = pref.getString("chroot_dir", chroot_dir);
        monstart = pref.getBoolean("monstart", monstart);
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
        band = Integer.parseInt(pref.getString("band", Integer.toString(band)));
        show_client_count = pref.getBoolean("show_client_count", show_client_count);

        progress.setMax(deauthWait);
        progress.setProgress(deauthWait);
    }
    static void loadAliases(){
        aliases_file = new File(data_path + "/aliases.txt");
        try{
            if(!aliases_file.exists()){
                aliases_file.createNewFile();
            }else{
                if(debug) Log.d("HIJACKER/loadAliases", "Reading aliases file...");
                try{
                    BufferedReader aliases_out = new BufferedReader(new FileReader(aliases_file));
                    String buffer = aliases_out.readLine();
                    while(buffer!=null){
                        //Line format: 00:11:22:33:44:55 Alias
                        if(buffer.charAt(17)==' ' && buffer.length()>18){
                            String mac = buffer.substring(0, 17);
                            String alias = buffer.substring(18);
                            aliases.put(mac, alias);
                        }else{
                            Log.e("HIJACKER/loadAliases", "Aliases file format error: " + buffer);
                        }
                        buffer = aliases_out.readLine();
                    }
                    aliases_out.close();
                }catch(IOException e){
                    Log.e("HIJACKER/loadAliases1", e.toString());
                }
            }
            aliases_in = new FileWriter(aliases_file, true);
        }catch(Exception e){
            Log.e("HIJACKER/loadAliases2", e.toString());
            aliases_in = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;

            case R.id.reset:
                stop(PROCESS_AIRODUMP);
                Tile.clear();
                Tile.onCountsChanged();
                Airodump.startClean();
                return true;

            case R.id.stop_run:
                if(Airodump.isRunning()) stop(PROCESS_AIRODUMP);
                else Airodump.start();
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
    // See https://g.co/AppIndexing/AndroidStudio for more information.
    @Override
    public void onStart(){
        super.onStart();
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }
    @Override
    protected void onResume(){
        super.onResume();
        notif_on = false;
        background = false;
        if(mNotificationManager!=null) mNotificationManager.cancelAll();
    }
    @Override
    protected void onPause(){
        super.onPause();
        background = true;
    }
    @Override
    protected void onStop(){
        super.onStop();
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
        if(show_notif){
            notif_on = true;
            notification();
        }
    }
    @Override
    protected void onDestroy(){
        notif_on = false;
        mNotificationManager.cancelAll();
        CustomAction.save();
        watchdogTask.cancel(true);
        stop(PROCESS_AIRODUMP);
        stop(PROCESS_AIREPLAY);
        stop(PROCESS_MDK_BF);
        stop(PROCESS_MDK_DOS);
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
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode==KeyEvent.KEYCODE_BACK){
            if(mDrawerLayout.isDrawerOpen(Gravity.START)){
                mDrawerLayout.closeDrawers();
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
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if(requestCode==0){
            //The one and only request this app sends
            if (grantResults.length > 0 && grantResults[2]==PackageManager.PERMISSION_GRANTED) {
                CustomAction.load();
                loadAliases();
            }
        }
    }
    @SuppressLint("MissingSuperCall")
    @Override
    protected void onSaveInstanceState(Bundle outState){
        //No call for super(), avoid IllegalStateException on FragmentManagerImpl.checkStateLoss.
    }

    public Action getIndexApiAction(){
        Thing object = new Thing.Builder().setName("Hijacker")
                .setUrl(Uri.parse("https://github.com/chrisk44/Hijacker")).build();
        return new Action.Builder(Action.TYPE_VIEW).setObject(object).setActionStatus(Action.STATUS_TYPE_COMPLETED).build();
    }

    class MyListAdapter extends ArrayAdapter<Tile>{
        MyListAdapter(){
            super(MainActivity.this, R.layout.listitem);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent){
            // get a view to work with
            View itemview = convertView;
            if(itemview==null){
                itemview = getLayoutInflater().inflate(R.layout.listitem, parent, false);
            }

            // find the item to work with
            Tile current = Tile.tiles.get(position);

            TextView upperLeft = itemview.findViewById(R.id.upperLeft);
            upperLeft.setText(current.device.upperLeft);
            upperLeft.setTextColor(ContextCompat.getColor(getContext(), current.device.isMarked ? R.color.colorAccent : android.R.color.white));

            TextView lowerLeft = itemview.findViewById(R.id.lowerLeft);
            lowerLeft.setText(current.device.lowerLeft);

            TextView lowerRight = itemview.findViewById(R.id.lowerRight);
            lowerRight.setText(current.device.lowerRight);

            TextView upperRight = itemview.findViewById(R.id.upperRight);
            upperRight.setText(current.device.upperRight);

            //Image and count views
            ImageView iv = itemview.findViewById(R.id.iv);
            TextView icon_count_view = itemview.findViewById(R.id.icon_count_view);
            if(current.device instanceof AP){
                if(((AP)current.device).isHidden) iv.setImageResource(R.drawable.ap_hidden);
                else iv.setImageResource(R.drawable.ap2);

                if(show_client_count){
                    icon_count_view.setText(Integer.toString(((AP) (current.device)).clients.size()));
                    icon_count_view.setVisibility(View.VISIBLE);
                }else{
                    icon_count_view.setVisibility(View.GONE);
                }
            }else{
                iv.setImageResource(R.drawable.st2);

                icon_count_view.setVisibility(View.GONE);
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

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent){
            // get a view to work with
            View itemview = convertView;
            if(itemview==null){
                itemview = getLayoutInflater().inflate(R.layout.listitem, parent, false);
            }

            // find the item to work with
            CustomAction currentItem = CustomAction.cmds.get(position);

            TextView upperLeft = itemview.findViewById(R.id.upperLeft);
            upperLeft.setText(currentItem.getTitle());

            TextView lowerLeft = itemview.findViewById(R.id.lowerLeft);
            lowerLeft.setText(currentItem.getStartCmd());

            TextView lowerRight = itemview.findViewById(R.id.lowerRight);
            lowerRight.setText("");

            TextView upperRight = itemview.findViewById(R.id.upperRight);
            upperRight.setText("");

            //Image
            ImageView iv = itemview.findViewById(R.id.iv);
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

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent){
            // get a view to work with
            View itemview = convertView;
            if(itemview==null){
                itemview = getLayoutInflater().inflate(R.layout.explorer_item, parent, false);
            }

            // find the item to work with
            RootFile currentItem = FileExplorerDialog.list.get(position);

            TextView firstText = itemview.findViewById(R.id.explorer_item_tv);
            firstText.setText(currentItem.getName());

            //Image
            ImageView iv = itemview.findViewById(R.id.explorer_iv);
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
            stop(PROCESS_MDK_DOS);
            ados = false;
        }else{
            ((TextView)v).setText(R.string.stop);
            startAdos(is_ap.mac);
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
        if(notif_on && show_notif){
            if(show_details){
                String str;
                if(is_ap==null) str = "APs: " + Tile.i + " | STs: " + (Tile.tiles.size() - Tile.i);
                else str = is_ap.essid + " | STs: " + (Tile.tiles.size() - Tile.i);

                if(aireplay_running==AIREPLAY_DEAUTH) str += " | Aireplay deauthenticating...";
                else if(aireplay_running==AIREPLAY_WEP) str += " | Aireplay replaying for wep...";
                if(wpa_thread!=null){
                    if(wpa_thread.isAlive()) str += " | WPA cracking...";
                }
                if(bf) str += " | MDK3 Beacon Flooding...";
                if(ados) str += " | MDK3 Authentication DoS...";
                if(ReaverFragment.isRunning()) str += " | Reaver running...";
                if(CrackFragment.isRunning()) str += " | Cracking .cap file...";
                if(CustomActionFragment.isRunning()) str += " | Running action " + CustomActionFragment.selectedAction.getTitle() + "...";

                notif.setContentText(str);
            }else notif.setContentText(null);
            mNotificationManager.notify(0, notif.build());
        }else{
            mNotificationManager.cancel(0);
        }
    }
    static void isolate(String mac){
        is_ap = getAPByMac(mac);
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

        if(!(ReaverFragment.isRunning() || CrackFragment.isRunning() || wpa_thread.isAlive())){
            progress.setIndeterminate(false);
            progress.setProgress(deauthWait);
        }
    }
    static void refreshDrawer(){
        navigationView.getMenu().findItem(currentFragment).setChecked(true);
        actionBar.setTitle(navTitlesMap.get(currentFragment));
    }
    static String getManuf(String mac){
        mac = trimMac(mac);
        if(manufHashMap==null) return "Unknown Manufacturer";
        String manuf = manufHashMap.get(mac);
        if(manuf==null) return "Unknown Manufacturer";
        else return manuf;
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
                bootkali_init_bin = temp + "/bootkali_init";
                break;
            }
        }
        if(!bin){
            if(new RootFile(NETHUNTER_BOOTKALI_BASH).exists()){
                bin = true;
                bootkali_init_bin = NETHUNTER_BOOTKALI_BASH;
            }
        }

        dir = new RootFile(chroot_dir).exists() && new RootFile(chroot_dir + "/bin/bash").exists();

        if(bin && dir) return CHROOT_FOUND;
        else if(!bin && !dir) return CHROOT_BOTH_MISSING;
        else if(dir) return CHROOT_BIN_MISSING;
        else return CHROOT_DIR_MISSING;
    }
    static void checkForUpdate(final Activity activity, final boolean showMessages){
        //Can be called from any thread, blocks until the job is finished
        if(showMessages){
            runInHandler(new Runnable(){
                @Override
                public void run(){
                    progress.setIndeterminate(true);
                }
            });
        }

        try{
            HttpsURLConnection connection = (HttpsURLConnection) (new URL(RELEASES_LINK).openConnection());
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            JsonReader reader = new JsonReader(new InputStreamReader(connection.getInputStream()));
            reader.beginArray();
            if(!reader.hasNext()){
                //No releases
                Log.e("HIJACKER/UpdateCheck", "No releases found");
                throw new Exception();
            }

            String latestName = null, latestLink = null, latestBody = null;

            reader.beginObject();
            //Run through all the names in the release array
            while(reader.hasNext()){
                String field = reader.nextName();
                if(field.equals("tag_name")){
                    latestName = reader.nextString();
                }else if(field.equals("body")){
                    latestBody = reader.nextString();
                    if(latestBody.equals("")) latestBody = null;
                }else if(field.equals("assets")){
                    //assets is an array
                    reader.beginArray();
                    reader.beginObject();
                    //Run through all the names in the 'assets' array
                    while(reader.hasNext()){
                        field = reader.nextName();
                        if(field.equals("browser_download_url")){
                            latestLink = reader.nextString();
                        }else{
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                    reader.endArray();
                }else{
                    reader.skipValue();
                }
            }
            reader.close();

            if(!versionName.equals(latestName)){
                final UpdateConfirmDialog dialog = new UpdateConfirmDialog();
                dialog.newVersionName = latestName;
                dialog.link = latestLink;
                dialog.message = latestBody;
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        dialog.show(activity.getFragmentManager(), "UpdateConfirmDialog");
                    }
                });
            }else{
                if(showMessages) Snackbar.make(rootView, activity.getString(R.string.already_on_latest), Snackbar.LENGTH_SHORT).show();
            }
        }catch(Exception e){
            Log.e("HIJACKER/update", e.toString());
            if(showMessages) Snackbar.make(rootView, activity.getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
        }finally{
            if(showMessages) runInHandler(new Runnable(){
                @Override
                public void run(){
                    progress.setIndeterminate(false);
                }
            });
        }
    }
    static boolean internetAvailable(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getNetworkInfo(1).getState()==NetworkInfo.State.CONNECTED || connectivityManager.getNetworkInfo(0).getState()==NetworkInfo.State.CONNECTED;
    }
    static boolean createReport(File out, String filesDir, String stackTrace, Process shell){
        if(!out.exists()){
            try{
                if(!out.createNewFile()) return false;
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
            cmd += " echo aliases file-----------------------------------; " + busybox_tmp + " cat " + Environment.getExternalStorageDirectory() + "/Hijacker/aliases.txt;";
            cmd += " echo app directory----------------------------------; " + busybox_tmp + " ls -lR " + filesDir + ';';
            cmd += " echo fw_bcmdhd--------------------------------------; strings /vendor/firmware/fw_bcmdhd.bin | grep \"FWID:\";";
            cmd += " echo ps---------------------------------------------; ps | " + busybox_tmp + " grep -e air -e mdk -e reaver;";
            cmd += " echo busybox----------------------------------------; " + busybox_tmp + ";";
            cmd += " echo logcat-----------------------------------------; logcat -d -v time | " + busybox_tmp + " grep HIJACKER;";
            cmd += " exit\n";
            Log.d("HIJACKER/createReport", cmd);
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

    static String findFirmwarePath(Shell shell){
        //Blocking function, don't run on main thread
        boolean flag = false;
        if(shell==null){
            flag = true;
            shell = getFreeShell();
        }

        String dirs[] = {
                "/system",
                "/vendor"
        };
        String firmware = null;
        int i = 0;
        while(firmware==null && i<dirs.length){
            firmware = checkDirectoryForFirmware(shell, dirs[i]);
            i++;
        }

        if(flag){
            //Release the shell only if it was obtained by this function
            shell.done();
        }

        return firmware;
    }
    static String checkDirectoryForFirmware(Shell shell, String directory){
        String firmware = null;
        shell.run(busybox + " find " + directory + " -type f -name \"fw_bcmdhd.bin\"; echo ENDOFFIND");
        BufferedReader out = shell.getShell_out();
        try{
            String result = out.readLine();
            while(result!=null){
                if(result.equals("ENDOFFIND")) break;
                if(!result.contains("/bac/") && !result.contains("backup")) firmware = result;

                result = out.readLine();
            }
        }catch(IOException e){
            e.printStackTrace();
        }

        return firmware;
    }

    static{
        System.loadLibrary("native-lib");
    }
}
