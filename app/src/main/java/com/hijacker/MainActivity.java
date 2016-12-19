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
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
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
    //State variables
    static boolean cont = false, wpacheckcont = false, done = true, notif_on = false;  //done: for calling refreshHandler only when it has stopped
    static int airodump_running = 0, aireplay_running = 0, currentFragment=FRAGMENT_AIRODUMP;         //Set currentFragment in onResume of each Fragment
    //Filters
    static boolean show_ap = true, show_st = true, show_na_st = true, wpa = true, wep = true, opn = true;
    static boolean show_ch[] = {true, false, false, false, false, false, false, false, false, false, false, false, false, false, false};
    static int pwr_filter = 120;
    static TextView ap_count, st_count;                               //AP and ST count textviews in toolbar
    static ProgressBar progress;
    static Toolbar toolbar;
    static Drawable overflow[] = {null, null, null, null, null, null, null, null};      //Drawables to use for overflow button icon
    static ImageView[] status = {null, null, null, null, null};                         //Icons in TestDialog, set in TestDialog class
    static int progress_int;
    static Thread refresh_thread, wpa_thread, watchdog_thread;
    static Runnable refresh_runnable, wpa_runnable, watchdog_runnable;
    static Menu menu;
    static List<Item2> fifo;                    //List used as FIFO for handling calls to addAP/addST in an order
    static MyListAdapter adapter;
    static CustomActionAdapter custom_action_adapter;
    static SharedPreferences pref;
    static SharedPreferences.Editor pref_edit;
    static ClipboardManager clipboard;
    static NotificationCompat.Builder notif;
    static NotificationManager nm;
    static FragmentManager fm;
    static String path;             //App files path (ends with .../files)
    static boolean init=false;      //True on first run to swap the dialogs for initialization
    private GoogleApiClient client;
    private String[] mPlanetTitles;
    private DrawerLayout mDrawerLayout;
    protected ListView mDrawerList;
    //Preferences - Defaults are in strings.xml
    static String iface, prefix, airodump_dir, aireplay_dir, aircrack_dir, mdk3_dir, reaver_dir, cap_dir, chroot_dir,
            enable_monMode, disable_monMode, custom_chroot_cmd;
    static int deauthWait;
    static boolean show_notif, show_details, airOnStartup, debug, confirm_exit, delete_extra, manuf_while_ados,
            monstart, always_cap, cont_on_fail, watchdog;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){
            @Override
            public void uncaughtException(Thread thread, Throwable throwable){
                throwable.printStackTrace();

                Intent intent = new Intent();
                intent.setAction("com.hijacker.SendLogActivity");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

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
                if(debug) Log.d("refresh_thread", "refresh_thread running");
                try{
                    while(cont){
                        if(done) refreshHandler.obtainMessage().sendToTarget();
                        Thread.sleep(1000);
                    }
                }catch(InterruptedException e){ Log.e("Exception", "Caught Exception in main() refresh_thread block: " + e.toString()); }
                if(debug) Log.d("refresh_thread", "refresh_thread done");
            }
        };
        refresh_thread = new Thread(refresh_runnable);

        wpa_runnable = new Runnable(){
            @Override
            public void run(){
                if(debug) Log.d("wpa_thread", "Started wpa_thread");

                Thread wpa_subthread = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        if(debug) Log.d("wpa_subthread", "wpa_subthread started");
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
                                        Snackbar snackbar = Snackbar.make(getCurrentFocus(), getString(R.string.stopped_to_capture), Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                        progress.setIndeterminate(true);
                                    }else{
                                        progress.setIndeterminate(false);
                                        progress.setProgress(deauthWait);
                                    }
                                }
                            });
                        }catch(InterruptedException e){ Log.e("Exception", "Caught Exception in wpa_subthread: " + e.toString()); }
                        if(debug) Log.d("wpa_subthread", "wpa_subthread finished");
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

                    if(debug) Log.d("wpa_thread", capfile);
                    if(capfile.equals("ENDOFLS")){
                        if(debug){
                            Log.d("wpa_thread", "cap file not found, airodump is probably not running...");
                            Log.d("wpa_thread", "Returning...");
                        }
                    }else{
                        Snackbar.make(getCurrentFocus(), getString(R.string.cap_is) + capfile, Snackbar.LENGTH_LONG).show();
                        progress_int = 0;
                        wpacheckcont = true;
                        wpa_subthread.start();
                        while(result!=1 && wpacheckcont){
                            if(debug) Log.d("wpa_thread", "Checking cap file...");
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
                                Snackbar.make(getCurrentFocus(), getString(R.string.handshake_captured) + ' ' + capfile, Snackbar.LENGTH_LONG).show();
                            }
                            progress.setIndeterminate(false);
                            progress.setProgress(deauthWait);
                        }
                    });
                }catch(IOException | InterruptedException e){
                    Log.e("Exception", "Caught Exception in wpa_thread: " + e.toString());
                    stop(PROCESS_AIREPLAY);
                }finally{
                    wpa_subthread.interrupt();
                    if(delete_extra){
                        shell.run("rm -rf " + cap_dir + "/handshake-*.csv");
                        shell.run("rm -rf " + cap_dir + "/handshake-*.netxml");
                    }
                    if(debug) Log.d("wpa_thread", "wpa_thread finished");
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
                        if(debug) Log.d("watchdog_handler", "Watchdog watching...");
                        List<Integer> list;
                        Message msg;
                        list = getPIDs(PROCESS_AIRODUMP);
                        if(airodump_running!=0 && list.size()==0){          //airodump not running
                            msg = new Message();
                            msg.obj = getString(R.string.airodump_not_running);
                            watchdog_handler.sendMessage(msg);
                            flag = false;
                            airodump_running = 0;
                        }else if(airodump_running==0 && list.size()>0){     //airodump still running
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
                            aireplay_running = 0;
                        }else if(aireplay_running==0 && list.size()>0){ //aireplay still running
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
                            bf = false;
                            ados = false;
                        }else if(!(bf || ados) && list.size()>0){   //mdk still running
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
                            ReaverFragment.cont = false;
                        }else if(!ReaverFragment.cont && list.size()>0){   //reaver still running
                            stop(PROCESS_REAVER);
                            if(getPIDs(PROCESS_REAVER).size()>0){
                                msg = new Message();
                                msg.obj = getString(R.string.reaver_still_running);
                                watchdog_handler.sendMessage(msg);
                                flag = false;
                            }
                        }
                    }
                }catch(InterruptedException e){ Log.e("watchdog thread", "Exception: " + e.toString()); }
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

        extract("busybox", false);
        Shell shell = getFreeShell();
        String dest = "/su/xbin/busybox";
        boolean inSystem = false;
        if(!(new File("/su").exists())){
            dest = "/system/xbin/busybox";
            inSystem = true;
            shell.run("chmod 755 " + path + "/busybox");
            shell.run(path + "/busybox mount -o rw,remount,rw /system");
        }
        shell.run("cp " + path + "/busybox " + dest);
        shell.run("chmod 755 " + dest);
        if(inSystem) shell.run("busybox mount -o ro,remount,ro /system");
        shell.done();
        if(debug) Log.d("onCreate", "Installed " + dest);

        if(!pref.getBoolean("disclaimer", false)){
            mDrawerLayout.openDrawer(GravityCompat.START);      //Can't open it later
            new DisclaimerDialog().show(fm, "Disclaimer");
        }else main();
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
                Log.e("FileProvider", "Exception copying from assets", e);
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
        if(watchdog){
            watchdog_thread = new Thread(watchdog_runnable);
            watchdog_thread.start();
        }
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
                if(debug) Log.d("startAirodump", cmd);
                try{
                    Process process = Runtime.getRuntime().exec(cmd);
                    BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String buffer;
                    while(cont && (buffer = in.readLine())!=null){
                        main(buffer, mode);
                    }
                }catch(IOException e){ Log.e("Exception", "Caught Exception in startAirodump() read block: " + e.toString()); }
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

    public static void _startAireplay(final String str){
        try{
            String cmd = "su -c " + prefix + " " + aireplay_dir + " --ignore-negative-one " + str + " " + iface;
            if(debug) Log.d("_startAireplay", cmd);
            Runtime.getRuntime().exec(cmd);
        }catch(IOException e){ Log.e("Exception", "Caught Exception in _startAireplay() start block: " + e.toString()); }
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
    public static void startAireplayWEP(String mac){
        //Increase IV generation from ap mac to crack a wep network
        aireplay_running = AIREPLAY_WEP;
        _startAireplay("--fakeauth 0 -a " + mac);
        _startAireplay("--arpreplay -b " + mac);
        _startAireplay("--caffe-latte -b " + mac);
    }

    public static void startMdk(int mode, String str){
        ArrayList<Integer> ps_before = getPIDs(PROCESS_MDK);
        switch(mode){
            case MDK_BF:
                //beacon flood mode
                try{
                    String cmd = "su -c " + prefix + " " + mdk3_dir + " " + iface + " b -m ";
                    if(str!=null) cmd += str;
                    if(debug) Log.d("MDK3", cmd);
                    Runtime.getRuntime().exec(cmd);
                    Thread.sleep(500);
                }catch(IOException | InterruptedException e){ Log.e("Exception", "Caught Exception in startMdk(MDK_BF) start block: " + e.toString()); }
                bf = true;
                break;
            case MDK_ADOS:
                //Authentication DoS mode
                try{
                    String cmd = "su -c " + prefix + " " + mdk3_dir + " " + iface + " a -m";
                    cmd += str==null ? "" : " -i " + str;
                    if(debug) Log.d("MDK3", cmd);
                    Runtime.getRuntime().exec(cmd);
                    Thread.sleep(500);
                }catch(IOException | InterruptedException e){ Log.e("Exception", "Caught Exception in startMdk(MDK_ADOS) start block: " + e.toString()); }
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

    public static ArrayList<Integer> getPIDs(int pr){
        ArrayList<Integer> list = new ArrayList<>();
        try{
            Shell shell = getFreeShell();
            int pid;
            String s = null;
            switch(pr){
                case PROCESS_AIRODUMP:
                    shell.run("toolbox ps | busybox grep airo; echo ENDOFPS");
                    break;
                case PROCESS_AIREPLAY:
                    shell.run("toolbox ps | busybox grep aire; echo ENDOFPS");
                    break;
                case PROCESS_MDK:
                    shell.run("toolbox ps | busybox grep mdk3; echo ENDOFPS");
                    break;
                case PROCESS_AIRCRACK:
                    shell.run("toolbox ps | busybox grep airc; echo ENDOFPS");
                    break;
                case PROCESS_REAVER:
                    shell.run("toolbox ps | busybox grep reav; echo ENDOFPS");
                    break;
                default:
                    Log.e("getPIDs", "Method called with invalid pr code");
                    return null;
            }
            BufferedReader shell_out = shell.getShell_out();
            while(s==null){ s = shell_out.readLine(); } //for some reason sometimes s remains null
            while(!s.equals("ENDOFPS")){
                pid = ps(s);
                if(pid!=0){
                    list.add(pid);
                }
                s = shell_out.readLine();
            }
            shell.done();
        }catch(IOException e){ Log.e("Exception", "Caught Exception in getPIDs(pr): " + e.toString()); }
        return list;
    }
    public static void stop(int pr){
        //0 for airodump-ng, 1 for aireplay-ng, 2 for mdk, 3 for aircrack, 4 for reaver, everything else is considered pid and we kill it
        ArrayList<Integer> pids = new ArrayList<>();
        if(pr<=4) pids = getPIDs(pr);
        switch(pr){
            case PROCESS_AIRODUMP:
                cont = false;
                refresh_thread.interrupt();
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        if(menu!=null) menu.getItem(1).setIcon(R.drawable.run);
                        Item.filter();
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
                break;
            case PROCESS_AIREPLAY:
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        if(menu!=null) menu.getItem(3).setEnabled(false);
                    }
                });
                if(delete_extra && aireplay_running==AIREPLAY_WEP){
                    runOne("busybox rm -rf " + cap_dir + "/wep_ivs-*.csv");
                    runOne("busybox rm -rf " + cap_dir + "/wep_ivs-*.netxml");
                }
                aireplay_running = 0;
                progress_int = deauthWait;
                break;
            case PROCESS_MDK:
                ados = false;
                bf = false;
                break;
            case PROCESS_AIRCRACK:
                CrackFragment.cont = false;
                break;
            case PROCESS_REAVER:
                ReaverFragment.cont = false;
                break;
            default:
                pids.add(pr);
                break;
        }
        if(pids.isEmpty()){
            if(debug) Log.d("stop", "Nothing found for " + pr);
        }else{
            Shell shell = getFreeShell();
            for(int i = 0; i<pids.size(); i++){
                if(debug) Log.d("Killing...", Integer.toString(pids.get(i)));
                shell.run("busybox kill " + pids.get(i));
            }
            shell.done();
        }
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
            ap_count.setText(Integer.toString(is_ap==null ? Item.i : 1));
            st_count.setText(Integer.toString(Item.items.size() - Item.i));
            notification();
            done = true;
        }
    };
    public Handler watchdog_handler = new Handler(){
        public void handleMessage(Message msg){
            if(debug) Log.d("watchdog_handler", "Message is " + msg.obj);
            ErrorDialog dialog = new ErrorDialog();
            dialog.setTitle((String)msg.obj);
            dialog.setMessage(getString(R.string.watchdog_message));
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
        ap_count = (TextView) findViewById(R.id.ap_count);
        st_count = (TextView) findViewById(R.id.st_count);
        fifo = new ArrayList<>();
        progress = (ProgressBar) findViewById(R.id.progressBar);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref_edit = pref.edit();
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        fm = getFragmentManager();
        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
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
        confirm_exit = Boolean.parseBoolean(getString(R.string.confirm_exit));
        delete_extra = Boolean.parseBoolean(getString(R.string.delete_extra));
        manuf_while_ados = Boolean.parseBoolean(getString(R.string.manuf_while_ados));
        always_cap = Boolean.parseBoolean(getString(R.string.always_cap));
        chroot_dir = getString(R.string.chroot_dir);
        monstart = Boolean.parseBoolean(getString(R.string.monstart));
        custom_chroot_cmd = "";
        cont_on_fail = Boolean.parseBoolean(getString(R.string.cont_on_fail));
        watchdog = Boolean.parseBoolean(getString(R.string.watchdog));

        //Initialize notification
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

        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        load();

        //Load strings for when they cannot be retrived with getString or R.string...
        ST.not_connected = getString(R.string.not_connected);
        ST.paired = getString(R.string.paired);

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
        ft.commit();
        getSupportActionBar().setTitle(mPlanetTitles[currentFragment]);

        //Google AppIndex
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        path = getFilesDir().getAbsolutePath();
        CustomAction.load();
        if(debug) Log.d("Main", "path is " + path);
        if(!(new File(path).exists())){
            Log.e("onCreate", "App file directory doesn't exist");
            ErrorDialog dialog = new ErrorDialog();
            dialog.setMessage(getString(R.string.app_dir_notfound1) + path + getString(R.string.app_dir_notfound2));
            dialog.show(fm, "ErrorDialog");
        }
    }
    static void load(){
        //Load Preferences
        if(debug) Log.d("Main", "Loading preferences...");

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
        confirm_exit = pref.getBoolean("confirm_exit", confirm_exit);
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
                Item.clear();
                ap_count.setText("0");
                st_count.setText("0");
                stop(PROCESS_AIRODUMP);
                startAirodump(null);
                return true;

            case R.id.stop_run:
                if(cont){
                    //Running
                    stop(PROCESS_AIRODUMP);
                    stop(PROCESS_AIREPLAY);
                    stop(PROCESS_MDK);
                }else{
                    if(is_ap==null) startAirodump(null);
                    else startAirodump("--channel " + is_ap.ch + " --bssid " + is_ap.mac);
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
                    ft.commit();
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
        if(nm!=null) nm.cancelAll();
    }
    @Override
    protected void onStop(){
        super.onStop();
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
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
                mDrawerList.getChildAt(currentFragment).setBackgroundResource(R.color.colorPrimary);
                getFragmentManager().popBackStack();
                getFragmentManager().executePendingTransactions();                  //need to wait for currentFragment to update
                getSupportActionBar().setTitle(mPlanetTitles[currentFragment]);
                mDrawerList.getChildAt(currentFragment).setBackgroundResource(R.color.colorAccent);
            }else if(confirm_exit){
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
        Thing object = new Thing.Builder().setName("Home Screen")
                .setUrl(Uri.parse("https://github.com/chrisk44/Hijacker")).build();
        return new Action.Builder(Action.TYPE_VIEW).setObject(object).setActionStatus(Action.STATUS_TYPE_COMPLETED).build();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(currentFragment!=position){
                mDrawerList.getChildAt(currentFragment).setBackgroundResource(R.color.colorPrimary);
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
                ft.commit();
                fm.executePendingTransactions();

                getSupportActionBar().setTitle(mPlanetTitles[position]);
                mDrawerList.getChildAt(currentFragment).setBackgroundResource(R.color.colorAccent);
            }
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }
    class MyListAdapter extends ArrayAdapter<Item>{
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
            Item currentItem = Item.items.get(position);

            TextView firstText = (TextView) itemview.findViewById(R.id.top_left);
            firstText.setText(currentItem.s1);

            TextView secondText = (TextView) itemview.findViewById(R.id.bottom_left);
            secondText.setText(currentItem.s2);

            TextView thirdText = (TextView) itemview.findViewById(R.id.bottom_right);
            thirdText.setText(currentItem.s3);

            TextView text4 = (TextView) itemview.findViewById(R.id.top_right);
            text4.setText(currentItem.s4);

            //Image
            ImageView iv = (ImageView) itemview.findViewById(R.id.iv);
            if(!currentItem.type){
                iv.setImageResource(R.drawable.st2);
            }else{
                if(currentItem.ap.isHidden) iv.setImageResource(R.drawable.ap_hidden);
                else iv.setImageResource(R.drawable.ap2);
            }

            return itemview;
        }

        @Override
        public int getCount(){
            return Item.items.size();
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
            stop(PROCESS_AIRODUMP);
            stop(PROCESS_AIREPLAY);
            ((TextView)v).setText(R.string.crack);
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
            if(is_ap==null) str = "APs: " + Item.i + " | STs: " + (Item.items.size() - Item.i);
            else str = is_ap.essid + " | STs: " + (Item.items.size() - Item.i);

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
            ft.commit();
        }
        Item.filter();
        if(debug){
            if(is_ap==null) Log.d("Main", "No AP isolated");
            else Log.d("Main", "AP with MAC " + mac + " isolated");
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
            while(buffer==null){
                buffer = out.readLine();
            }
            lastline = buffer;
            while(!end.equals(buffer)){
                lastline = buffer;
                buffer = out.readLine();
            }
        }catch(IOException e){ Log.e("Exception", "Exception in getLastLine: " + e); }

        return lastline;
    }

    static{
        System.loadLibrary("native-lib");
    }
    public static native int ps(String str);
    public static native boolean aireplay(String buf);
    public static native int main(String str, int off);
    public static native int checkwpa(String str);
}