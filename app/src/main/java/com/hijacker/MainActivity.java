package com.hijacker;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.hijacker.IsolatedFragment.is_ap;

public class MainActivity extends AppCompatActivity{
    static final int AIREPLAY_DEAUTH = 1, AIREPLAY_WEP = 2;
    static final int FRAGMENT_AIRODUMP = 0, FRAGMENT_MDK = 1, FRAGMENT_CRACK = 2,
            FRAGMENT_REAVER = 3, FRAGMENT_SETTINGS = 4;
    static final int PROCESS_AIRODUMP=0, PROCESS_AIREPLAY=1, PROCESS_MDK=2, PROCESS_AIRCRACK=3;
    //State variables
    static boolean cont = false, wpacheckcont = false, test_wait, maincalled = false, done = true, notif_on = false;  //done: for calling refreshHandler only when it has stopped
    static int airodump_running = 0, aireplay_running = 0, currentFragment=FRAGMENT_AIRODUMP;         //Set currentFragment in onResume of each Fragment
    //Filters
    static boolean show_ap = true, show_st = true, show_na_st = true, wpa = true, wep = true, opn = true;
    static boolean show_ch[] = {true, false, false, false, false, false, false, false, false, false, false, false, false, false, false};
    static int pwr_filter = 120;
    static TextView tv, ap_count, st_count, test_cur_cmd;                               //Log textview, AP and ST count textviews in toolbar
    static ProgressBar progress, test_progress;
    static Toolbar toolbar;
    //static AP is_ap;                                                                    //isolated AP
    static Drawable overflow[] = {null, null, null, null, null, null, null, null};      //Drawables to use for overflow button icon
    static ImageView[] status = {null, null, null};                                     //Icons in TestDialog, set in TestDialog class
    static int progress_int;
    static Thread refresh_thread, wpa_thread, wpa_subthread, test_thread, su_thread;
    static Menu menu;
    static String temp_string;
    static int temp_int;              //set in startAirodump, to be used in c++ main() to know if there is an extra column in Airodump output
    static Process shell, shell2, shell3, shell4;
    static PrintWriter shell_in, shell2_in, shell3_in, shell4_in;
    static BufferedReader shell_out, shell2_out, shell3_out, shell4_out;
    static List<Item2> fifo;                    //List used as FIFO for handling calls to addAP/addST in an order
    static MyListAdapter adapter;
    static SharedPreferences pref;
    static SharedPreferences.Editor pref_edit;
    static ClipboardManager clipboard;
    static NotificationCompat.Builder notif;
    static NotificationManager nm;
    static Locale locale;
    static FragmentManager fm;
    static String path;             //Path for oui.txt
    static boolean init=false;      //True on first run to swap the dialogs for initialization
    private GoogleApiClient client;
    private String[] mPlanetTitles;
    private DrawerLayout mDrawerLayout;
    protected ListView mDrawerList;
    //Preferences - Defaults are in strings.xml
    static String iface, prefix, airodump_dir, aireplay_dir, aircrack_dir, mdk3_dir, cap_dir, enable_monMode, disable_monMode;
    static int deauthWait, aireplay_sleep;
    static boolean showLog, show_notif, show_details, airOnStartup, debug, confirm_exit, delete_extra;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        adapter = new MyListAdapter();              //ALWAYS BEFORE setContentView AND setup(), can't stress it enough...
        adapter.setNotifyOnChange(true);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.my_toolbar));
        setup();

        refresh_thread = new Thread(new Runnable(){
            @Override
            public void run(){
                if(debug) Log.d("refresh_thread", "refresh_thread running");
                while(cont){
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        Log.e("Exception", "Caught Exception in main() refresh_thread block: " + e.toString());
                    }
                    if(done){
                        refreshHandler.obtainMessage().sendToTarget();
                    }
                }
            }
        });
        wpa_thread = new Thread(new Runnable(){
            @Override
            public void run(){
                if(debug) Log.d("wpa_thread", "Started wpa_thread");
                String capfile, buffer = null;
                int result = 0;
                try{
                    Thread.sleep(1000);
                    shell2_in.print("ls -1 " + cap_dir + "/wpa.cap-*.cap; echo ENDOFLS\n");
                    shell2_in.flush();
                    capfile = getLastLine(shell2_out, "ENDOFLS");

                    if(debug) Log.d("wpa_thread", capfile);
                    if(capfile.equals("ENDOFLS")){
                        if(debug){
                            Log.d("wpa_thread", "cap file not found, airodump is probably not running...");
                            Log.d("wpa_thread", "Returning...");
                        }
                        tv.append(getString(R.string.cap_notfound));
                    }else{
                        if(!showLog) Snackbar.make(getCurrentFocus(), getString(R.string.cap_is) + capfile, Snackbar.LENGTH_LONG).show();
                        progress_int = 0;
                        wpacheckcont = true;
                        wpa_subthread.start();
                        while(result!=1 && wpacheckcont){
                            if(debug) Log.d("wpa_thread", "Checking cap file...");
                            if(progress_int>deauthWait) appendDot.obtainMessage().sendToTarget();
                            shell2_in.print(aircrack_dir + " " + capfile + " && echo ENDOFAIR\n");
                            shell2_in.flush();
                            buffer = shell2_out.readLine();
                            if(buffer==null) wpacheckcont = false;
                            else{
                                while(!buffer.equals("ENDOFAIR")){
                                    if(result!=1) result = checkwpa(buffer);
                                    //if(result==0) Log.d("wpa_check", buffer);
                                    buffer = shell2_out.readLine();
                                }
                                Thread.sleep(700);
                            }
                        }
                        wpacheckcont = false;
                    }
                    if(result==1){
                        Message msg = new Message();
                        msg.obj = capfile;
                        MainActivity.this.handshakeCaptured.sendMessage(msg);
                    }else stopIndeterminate.obtainMessage().sendToTarget();
                }catch(IOException | InterruptedException e){
                    Log.e("Exception", "Caught Exception in wpa_thread: " + e.toString());
                    stop1.obtainMessage().sendToTarget();
                }
                wpa_subthread.interrupt();
                if(delete_extra){
                    shell2_in.print("rm -rf " + cap_dir + "/wpa.cap-*.csv\n");
                    shell2_in.print("rm -rf " + cap_dir + "/wpa.cap-*.netxml\n");
                    shell2_in.flush();
                }
                if(debug) Log.d("wpa_thread", "wpa_thread finished");
            }
        });
        wpa_subthread = new Thread(new Runnable(){
            @Override
            public void run(){
                if(debug) Log.d("wpa_subthread", "wpa_subthread started");
                try{
                    while(progress_int<=deauthWait && wpacheckcont){
                        Thread.sleep(1000);
                        progress_int++;
                        progressHandler.obtainMessage().sendToTarget();
                    }
                    if(wpacheckcont) MainActivity.this.stopAireplayForHandshake.obtainMessage().sendToTarget();
                    else stopIndeterminate.obtainMessage().sendToTarget();
                }catch(InterruptedException e){ Log.e("Exception", "Caught Exception in wpa_subthread: " + e.toString()); }
                if(debug) Log.d("wpa_subthread", "wpa_subthread finished");
            }
        });
        test_thread = new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    Thread.sleep(1000);
                    //Separate calls so the UI can be refreshed, otherwise it gets blocked.
                    Message msg = new Message();
                    msg.arg1 = 3;
                    test_wait = true;
                    runTest.sendMessage(msg);
                    while(test_wait){
                        Thread.sleep(100);
                    }

                    msg = new Message();
                    msg.arg1 = 0;
                    test_wait = true;
                    runTest.sendMessage(msg);
                    while(test_wait){
                        Thread.sleep(100);
                    }

                    msg = new Message();
                    msg.arg1 = 1;
                    test_wait = true;
                    runTest.sendMessage(msg);
                    while(test_wait){
                        Thread.sleep(100);
                    }

                    msg = new Message();
                    msg.arg1 = 2;
                    test_wait = true;
                    runTest.sendMessage(msg);
                    while(test_wait){
                        Thread.sleep(100);
                    }
                }catch(InterruptedException e){
                    Log.d("test_thread", "Interrupted");
                }
            }
        });
        su_thread = new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    shell = Runtime.getRuntime().exec("su");
                    shell2 = Runtime.getRuntime().exec("su");
                    shell3 = Runtime.getRuntime().exec("su");
                    shell4 = Runtime.getRuntime().exec("su");
                }catch(IOException e){
                    Log.e("onCreate", "Caught Exception in shell start: " + e.toString());
                }

                try{
                    shell.exitValue();
                    Log.e("onCreate", "Error opening su shell");
                    ErrorDialog dialog = new ErrorDialog();
                    dialog.setMessage(getString(R.string.error_opening_shell));
                    dialog.show(fm, "ErrorDialog");
                    return;
                }catch(IllegalThreadStateException ignored){
                }
                try{
                    shell2.exitValue();
                    Log.e("onCreate", "Error opening su shell");
                    ErrorDialog dialog = new ErrorDialog();
                    dialog.setMessage(getString(R.string.error_opening_shell));
                    dialog.show(fm, "ErrorDialog");
                    return;
                }catch(IllegalThreadStateException ignored){
                }
                try{
                    shell3.exitValue();
                    Log.e("onCreate", "Error opening su shell");
                    ErrorDialog dialog = new ErrorDialog();
                    dialog.setMessage(getString(R.string.error_opening_shell));
                    dialog.show(fm, "ErrorDialog");
                    return;
                }catch(IllegalThreadStateException ignored){
                }
                try{
                    shell4.exitValue();
                    Log.e("onCreate", "Error opening su shell");
                    ErrorDialog dialog = new ErrorDialog();
                    dialog.setMessage(getString(R.string.error_opening_shell));
                    dialog.show(fm, "ErrorDialog");
                    return;
                }catch(IllegalThreadStateException ignored){
                }

                shell_in = new PrintWriter(shell.getOutputStream());
                shell2_in = new PrintWriter(shell2.getOutputStream());
                shell3_in = new PrintWriter(shell3.getOutputStream());
                shell4_in = new PrintWriter(shell4.getOutputStream());
                shell_out = new BufferedReader(new InputStreamReader(shell.getInputStream()));
                shell2_out = new BufferedReader(new InputStreamReader(shell2.getInputStream()));
                shell3_out = new BufferedReader(new InputStreamReader(shell3.getInputStream()));
                shell4_out = new BufferedReader(new InputStreamReader(shell4.getInputStream()));
                if(shell_in==null || shell_out==null || shell2_in==null || shell2_out==null ||
                        shell3_in==null || shell3_out==null || shell4_in==null || shell4_out==null){
                    if(debug) Log.e("onCreate", "Error opening shell_in/shell_out");
                    ErrorDialog dialog = new ErrorDialog();
                    dialog.setMessage(getString(R.string.error_opening_shell));
                    dialog.show(fm, "ErrorDialog");
                }
            }
        });

        path = getFilesDir().getAbsolutePath();
        if(debug) Log.d("Main", "path is " + path);
        if(!(new File(path).exists())){
            Log.e("onCreate", "App file directory doesn't exist");
            ErrorDialog dialog = new ErrorDialog();
            dialog.setMessage(getString(R.string.app_dir_notfound1) + path + getString(R.string.app_dir_notfound2));
            dialog.show(fm, "ErrorDialog");
        }

        extract("oui.txt", false);

        if(!pref.getBoolean("disclaimer", false)) new DisclaimerDialog().show(fm, "Disclaimer");
        else main();
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
                    Runtime.getRuntime().exec("run-as com.hijacker chmod 755 ./files/" + filename); //TODO: change this
                }
            }catch(IOException e){
                Log.e("FileProvider", "Exception copying from assets", e);
            }
        }
    }
    public static void main(){
        maincalled = true;
        if(shell==null){
            su_thread.start();
            try{
                //Wait for su shells to spawn
                su_thread.join();
            }catch(InterruptedException ignored){
            }
        }

        shell_in.print("mkdir " + cap_dir + "\n");
        shell_in.flush();

        stop(PROCESS_AIRODUMP);
        stop(PROCESS_AIREPLAY);
        stop(PROCESS_MDK);
        stop(PROCESS_AIRCRACK);
        if(airOnStartup) startAirodump(null);
        else if(menu!=null) menu.getItem(1).setIcon(R.drawable.run);
    }

    public static void startAirodump(String params){
        if(params==null){
            temp_string = "";
            temp_int = 0;
        }else{
            temp_string = params;
            temp_int = 1;
        }
        shell_in.print(enable_monMode + "\n");
        shell_in.flush();
        stop(PROCESS_AIRODUMP);
        cont = true;
        new Thread(new Runnable(){
            @Override
            public void run(){
                Process airodump = null;
                try{
                    String cmd = "su -c " + prefix + " " + airodump_dir + " --update 1 " + temp_string + " " + iface;
                    if(debug) Log.d("startAirodump", cmd);
                    airodump = Runtime.getRuntime().exec(cmd);
                }catch(IOException e){
                    Log.e("Exception", "Caught Exception in startAirodump() start block: " + e.toString());
                }
                BufferedReader in = new BufferedReader(new InputStreamReader(airodump.getErrorStream()));
                String buffer;
                try{
                    while(cont){
                        if((buffer = in.readLine())!=null){
                            main(buffer, temp_int);
                        }
                    }
                }catch(IOException e){
                    Log.e("Exception", "Caught Exception in startAirodump() read block: " + e.toString());
                }
            }
        }).start();
        refresh_thread.start();
        tv.append("Airodump: " + temp_string + "\n");
        airodump_running = 1;
        refreshState();
        if(menu!=null) menu.getItem(1).setIcon(R.drawable.stop);
}

    public static void _startAireplay(String str){
        try{
            //just to make sure that airodump started and locked the channel
            //if not, aireplay will exit if not started with -D
            Thread.sleep(aireplay_sleep);
        }catch(InterruptedException e){
            Log.e("Exception", "Caught Exception in _startAireplay() sleep block: " + e.toString());
        }
        temp_string = str;
        new Thread(new Runnable(){
            @Override
            public void run(){
                //Process dc = null;
                try{
                    String cmd = "su -c " + prefix + " " + aireplay_dir + " --ignore-negative-one " + temp_string + " " + iface;
                    if(debug) Log.d("_startAireplay", cmd);
                    //dc = Runtime.getRuntime().exec(cmd);
                    Runtime.getRuntime().exec(cmd);
                }catch(IOException e){
                    Log.e("Exception", "Caught Exception in _startAireplay() start block: " + e.toString());
                }
                /*BufferedReader in = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                String buffer;
                try{  while (cont){ if ((buffer = in.readLine()) != null) {
                            if(debug) Log.d("_startAireplay", buffer);
                            //if(aireplay(buffer)) Log.d("Aireplay", "Broadcasting...");
                            //else Log.d("_startAireplay", "Waiting for beacon...");
                        }}} catch (IOException e){ Log.e("Exception", "Caught Exception in _startAireplay() read block: " + e.toString()); }*/
            }
        }).start();
        tv.append("Aireplay: " + str + "\n");
        menu.getItem(3).setEnabled(true);
        refreshState();
        notification();
    }
    public static void startAireplay(String str){
        aireplay_running = AIREPLAY_DEAUTH;
        _startAireplay("--deauth 0 -a " + str);
    }
    public static void startAireplay(String str1, String str2){
        aireplay_running = AIREPLAY_DEAUTH;
        _startAireplay("--deauth 0 -a " + str1 + " -c " + str2);
    }
    public static void startAireplayWEP(String mac){
        aireplay_running = AIREPLAY_WEP;
        int temp = aireplay_sleep;
        _startAireplay("--fakeauth 0 -a " + mac);
        aireplay_sleep = 0;
        _startAireplay("--arpreplay -b " + mac);
        _startAireplay("--caffe-latte -b " + mac);
        aireplay_sleep = temp;
    }

    public static void startMdk(int mode, String ap){
        switch(mode){
            case 0:
                //beacon flood mode
                tv.append("Beacon Flood\n");
                new Thread(new Runnable(){
                    @Override
                    public void run(){
                        try{
                            String cmd = "su -c " + prefix + " " + mdk3_dir + " " + iface + " b -m";
                            if(debug) Log.d("MDK3", cmd);
                            Runtime.getRuntime().exec(cmd);
                        }catch(IOException e){
                            Log.e("Exception", "Caught Exception in startMdk(0) start block: " + e.toString());
                        }
                    }
                }).start();
                break;
            case 1:
                //Authentication DoS mode
                temp_string = ap==null ? " " : "-i " + ap;
                tv.append("Authentication DoS" + temp_string + "\n");
                new Thread(new Runnable(){
                    @Override
                    public void run(){
                        try{
                            String cmd = "su -c " + prefix + " " + mdk3_dir + " " + iface + " a -m " + temp_string;
                            if(debug) Log.d("MDK3", cmd);
                            Runtime.getRuntime().exec(cmd);
                        }catch(IOException e){
                            Log.e("Exception", "Caught Exception in startMdk(1) start block: " + e.toString());
                        }
                    }
                }).start();
                break;
        }
        refreshState();
        notification();
    }

    public static ArrayList<Integer> getPIDs(int pr){
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
    public static void stop(int pr){
        //0 for airodump-ng, 1 for aireplay-ng, 2 for mdk, 3 for aircrack, everything else is considered pid and we kill it
        ArrayList<Integer> pids = new ArrayList<>();
        if(pr<=3) pids = getPIDs(pr);
        switch(pr){
            case PROCESS_AIRODUMP:
                progress.setIndeterminate(false);
                progress.setProgress(deauthWait);
                if(menu!=null) menu.getItem(1).setIcon(R.drawable.run);
                cont = false;
                if(wpa_thread.isAlive()){
                    IsolatedFragment.cont = false;
                    wpacheckcont = false;
                    wpa_thread.interrupt();
                }
                tv.append("Stopping airodump\n");
                airodump_running = 0;
                Item.filter();
                break;
            case PROCESS_AIREPLAY:
                tv.append("Stopping aireplay\n");
                if(menu!=null) menu.getItem(3).setEnabled(false);
                aireplay_running = 0;
                progress_int = deauthWait;
                break;
            case PROCESS_MDK:
                tv.append("Stopping mdk3\n");
                break;
            case PROCESS_AIRCRACK:
                tv.append("Stopping aircrack\n");
                break;
            default:
                pids.add(pr);
                break;
        }
        if(pids.isEmpty()){
            if(debug) Log.d("stop", "Nothing found for " + pr);
        }else{
            for(int i = 0; i<pids.size(); i++){
                if(debug) Log.d("Killing...", Integer.toString(pids.get(i)));
                shell_in.print("kill " + pids.get(i) + "\n");
                shell_in.flush();
            }
        }
        refreshState();
        notification();
    }

    //Handlers used for tasks that require the Main thread to update the view, but need to be run by other threads
    public Handler handshakeCaptured = new Handler(){
        public void handleMessage(Message msg){
            stop(PROCESS_AIRODUMP);
            stop(PROCESS_AIREPLAY);
            tv.setText(getString(R.string.handshake_captured) + msg.obj + '\n');
            if(!showLog){
                Snackbar snackbar = Snackbar.make(getCurrentFocus(), getString(R.string.handshake_captured) + msg.obj + '\n', Snackbar.LENGTH_LONG);
                snackbar.show();
            }
            progress.setIndeterminate(false);
        }
    };
    public Handler stopAireplayForHandshake = new Handler(){
        public void handleMessage(Message msg){
            stop(PROCESS_AIREPLAY);
            tv.append(getString(R.string.stopped_to_capture) + '\n');
            tv.append(getString(R.string.checking));
            if(!showLog){
                Snackbar snackbar = Snackbar.make(getCurrentFocus(), getString(R.string.stopped_to_capture), Snackbar.LENGTH_LONG);
                snackbar.show();
            }
            progress.setIndeterminate(true);
        }
    };
    public static Handler stopIndeterminate = new Handler(){
        public void handleMessage(Message msg){
            progress.setIndeterminate(false);
            progress.setProgress(deauthWait);
        }
    };
    public static Handler appendDot = new Handler(){
        public void handleMessage(Message msg){
            tv.append(".");
        }
    };
    public static Handler refreshHandler = new Handler(){
        public void handleMessage(Message msg){
            done = false;
            while(fifo.size()>0){
                fifo.get(0).add();
                adapter.notifyDataSetChanged();             //for when data is changed, no new data added
                fifo.remove(0);
            }
            ap_count.setText(String.format(locale, "%d", Item.i));
            st_count.setText(String.format(locale, "%d", Item.items.size() - Item.i));
            notification();
            done = true;
        }
    };
    public static Handler progressHandler = new Handler(){
        public void handleMessage(Message msg){
            progress.setProgress(progress_int);
        }
    };
    public static Handler runTest = new Handler(){
        public void handleMessage(Message msg){
            try{
                String cmd;
                switch(msg.arg1){
                    case 3:
                        stop(PROCESS_AIRODUMP);
                        stop(PROCESS_AIREPLAY);
                        stop(PROCESS_MDK);
                        cmd = enable_monMode + '\n';
                        Log.d("test_thread", cmd);
                        shell3_in.print(cmd);
                        shell3_in.flush();
                        Thread.sleep(1000);
                        status[0].setImageResource(R.drawable.testing);
                        test_cur_cmd.setText(prefix + " " + airodump_dir + " " + iface);
                        test_wait = false;
                        break;

                    case 0:
                        cmd = prefix + " " + airodump_dir + " " + iface + '\n';
                        Log.d("test_thread", cmd);
                        shell3_in.print(cmd);
                        shell3_in.flush();
                        Thread.sleep(2000);
                        if(getPIDs(0).size()==0) status[0].setImageResource(R.drawable.failed);
                        else{
                            stop(PROCESS_AIRODUMP);
                            status[0].setImageResource(R.drawable.passed);
                        }
                        test_progress.setProgress(1);
                        status[1].setImageResource(R.drawable.testing);
                        test_cur_cmd.setText(prefix + " " + aireplay_dir + " --deauth 0 -a 11:22:33:44:55:66 " + iface);
                        test_wait = false;
                        break;

                    case 1:
                        cmd = prefix + " " + aireplay_dir + " --deauth 0 -a 11:22:33:44:55:66 " + iface + '\n';
                        Log.d("test_thread", cmd);
                        shell3_in.print(cmd);
                        shell3_in.flush();
                        Thread.sleep(2000);
                        if(getPIDs(1).size()==0) status[1].setImageResource(R.drawable.failed);
                        else{
                            stop(PROCESS_AIREPLAY);
                            status[1].setImageResource(R.drawable.passed);
                        }
                        test_progress.setProgress(2);
                        status[2].setImageResource(R.drawable.testing);
                        test_cur_cmd.setText(prefix + " " + mdk3_dir + " " + iface + " b -m");
                        test_wait = false;
                        break;

                    case 2:
                        cmd = prefix + " " + mdk3_dir + " " + iface + " b -m\n";
                        Log.d("test_thread", cmd);
                        shell3_in.print(cmd);
                        shell3_in.flush();
                        Thread.sleep(2000);
                        if(getPIDs(2).size()==0) status[2].setImageResource(R.drawable.failed);
                        else{
                            stop(PROCESS_MDK);
                            status[2].setImageResource(R.drawable.passed);
                        }
                        test_progress.setProgress(3);
                        test_cur_cmd.setText("");

                        stop(PROCESS_AIRODUMP);
                        stop(PROCESS_AIREPLAY);
                        stop(PROCESS_MDK);
                        test_progress.setProgress(4);
                        test_wait = false;
                        break;
                }
            }catch(InterruptedException e){
                Log.e("Exception", "Caught Exception in runTest Handler: " + e.toString());
            }
        }
    };
    public static Handler stop1 = new Handler(){
        public void handleMessage(Message msg){
            stop(PROCESS_AIREPLAY);
        }
    };

    void setup(){
        tv = (TextView) findViewById(R.id.tv);
        ap_count = (TextView) findViewById(R.id.ap_count);
        st_count = (TextView) findViewById(R.id.st_count);
        fifo = new ArrayList<>();
        progress = (ProgressBar) findViewById(R.id.progressBar);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref_edit = pref.edit();
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        locale = getResources().getConfiguration().locale;
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

        iface = getString(R.string.iface);
        prefix = getString(R.string.prefix);
        aircrack_dir = getString(R.string.aircrack_dir);
        airodump_dir = getString(R.string.airodump_dir);
        aireplay_dir = getString(R.string.aireplay_dir);
        mdk3_dir = getString(R.string.mdk3_dir);
        cap_dir = getString(R.string.cap_dir);
        enable_monMode = getString(R.string.enable_monMode);
        disable_monMode = getString(R.string.disable_monMode);
        deauthWait = Integer.parseInt(getString(R.string.deauthWait));
        aireplay_sleep = Integer.parseInt(getString(R.string.aireplay_sleep));
        showLog = Boolean.parseBoolean(getString(R.string.showLog));
        show_notif = Boolean.parseBoolean(getString(R.string.show_notif));
        show_details = Boolean.parseBoolean(getString(R.string.show_details));
        airOnStartup = Boolean.parseBoolean(getString(R.string.airOnStartup));
        debug = Boolean.parseBoolean(getString(R.string.debug));
        confirm_exit = Boolean.parseBoolean(getString(R.string.confirm_exit));
        delete_extra = Boolean.parseBoolean(getString(R.string.delete_extra));

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
        progress.setProgress(deauthWait);
        progress.setMax(deauthWait);

        ST.not_connected = getString(R.string.not_connected);
        ST.paired = getString(R.string.paired);
        CrackFragment.cap_notfound = getString(R.string.cap_notfound);
        CrackFragment.wordlist_notfound = getString(R.string.wordlist_notfound);
        CrackFragment.select_wpa_wep = getString(R.string.select_wpa_wep);

        mPlanetTitles = getResources().getStringArray(R.array.planets_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, R.id.navDrawerTv, mPlanetTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment1, new MyListFragment());
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.addToBackStack(null);
        ft.commit();
        getSupportActionBar().setTitle(mPlanetTitles[currentFragment]);

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }
    static void load(){
        //Load Preferences
        if(debug) Log.d("Main", "Loading preferences...");
        showLog = pref.getBoolean("showLog", showLog);
        if(showLog && currentFragment!=FRAGMENT_SETTINGS) tv.setMaxHeight(999999);
        else tv.setMaxHeight(0);

        iface = pref.getString("iface", iface);
        prefix = pref.getString("prefix", prefix);
        deauthWait = Integer.parseInt(pref.getString("deauthWait", Integer.toString(deauthWait)));
        aireplay_sleep = Integer.parseInt(pref.getString("aireplay_sleep", Integer.toString(aireplay_sleep)));
        airodump_dir = pref.getString("airodump_dir", airodump_dir);
        aireplay_dir = pref.getString("aireplay_dir", aireplay_dir);
        aircrack_dir = pref.getString("aircrack_dir", aircrack_dir);
        mdk3_dir = pref.getString("mdk3_dir", mdk3_dir);
        cap_dir = pref.getString("cap_dir", cap_dir);
        enable_monMode = pref.getString("enable_monMode", enable_monMode);
        disable_monMode = pref.getString("disable_monMode", disable_monMode);
        show_notif = pref.getBoolean("show_notif", show_notif);
        show_details = pref.getBoolean("show_details", show_details);
        airOnStartup = pref.getBoolean("airOnStartup", airOnStartup);
        debug = pref.getBoolean("debug", debug);
        confirm_exit = pref.getBoolean("confirm_exit", confirm_exit);
        delete_extra = pref.getBoolean("delete_extra", delete_extra);
        progress.setMax(deauthWait);
        progress.setProgress(deauthWait);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.reset:
                Item.clear();
                tv.setText("");
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
                new FiltersDialog().show(fm, "asdTAG");
                return true;

            case R.id.settings:                             //To be removed
                if(currentFragment!=FRAGMENT_SETTINGS){
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.replace(R.id.fragment1, new SettingsFragment());
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.addToBackStack(null);
                    ft.commit();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onDestroy(){
        notif_on = false;
        nm.cancelAll();
        if(shell!=null){
            stop(PROCESS_AIRODUMP);
            stop(PROCESS_AIREPLAY);
            stop(PROCESS_MDK);
            stop(PROCESS_AIRCRACK);
            shell_in.print(disable_monMode + "\n");
            shell_in.print("exit\n");
            shell_in.flush();
            shell2_in.print("exit\n");
            shell2_in.flush();
            shell3_in.print("exit\n");
            shell3_in.flush();
            shell4_in.print("exit\n");
            shell4_in.flush();
        }
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
                return true;
            }else if(getFragmentManager().getBackStackEntryCount()>1){
                mDrawerList.getChildAt(currentFragment).setBackgroundResource(R.color.colorPrimary);
                getFragmentManager().popBackStack();
                getFragmentManager().executePendingTransactions();                  //need to wait for currentFragment to update
                getSupportActionBar().setTitle(mPlanetTitles[currentFragment]);
                mDrawerList.getChildAt(currentFragment).setBackgroundResource(R.color.colorAccent);
                return true;
            }else if(is_ap!=null){
                //Use back button to return from isolated ap
                stop(PROCESS_AIRODUMP);
                if(wpa_thread.isAlive()) stop(PROCESS_AIREPLAY);
                isolate(null);
                startAirodump(null);
                return true;
            }else if(confirm_exit){
                new ExitDialog().show(fm, "ExitDialog");
                return true;
            }
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
                .setUrl(Uri.parse("http://forum.xda-developers.com/android/apps-games/app-hijacker-gui-aircrack-ng-suite-t3499599")).build();
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
                        //Airodump
                        ft.replace(R.id.fragment1, is_ap==null ? new MyListFragment() : new IsolatedFragment());
                        break;
                    case FRAGMENT_MDK:
                        //MDK3
                        ft.replace(R.id.fragment1, new MDKFragment());
                        break;
                    case FRAGMENT_CRACK:
                        //Crack WPA
                        ft.replace(R.id.fragment1, new CrackFragment());
                        break;
                    case FRAGMENT_REAVER:
                        //Reaver
                        break;
                    case FRAGMENT_SETTINGS:
                        //Settings
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
    public static void addAP(String essid, String mac, String enc, String cipher, String auth, int pwr, int beacons, int data, int ivs, int ch){
        fifo.add(new Item2(essid, mac, enc, cipher, auth, pwr, beacons, data, ivs, ch));
    }
    public static void addST(String mac, String bssid, int pwr, int lost, int frames){
        fifo.add(new Item2(mac, bssid, pwr, lost, frames));
    }
    public void onAPStats(View v){ new StatsDialog().show(fm, "asdTAG4"); }
    public void onClearLog(View v){ tv.setText(""); }
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
        if(MDKFragment.ados) Toast.makeText(this, R.string.ados_running, Toast.LENGTH_SHORT).show();
        else{
            if(IsolatedFragment.ados_pid==0){
                startMdk(1, is_ap.mac);
                IsolatedFragment.ados_pid = getPIDs(2).get(0);
                MDKFragment.ados_pid = IsolatedFragment.ados_pid;
                MDKFragment.ados = true;
                refreshState();
                notification();
            }
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
        if(notif_on && show_notif && !(airodump_running==0 && aireplay_running==0 && !MDKFragment.bf && !MDKFragment.ados)){
            Log.d("notification", "in notification()");
            String str = "APs: " + Item.i + " | STs: " + (Item.items.size() - Item.i);

            if(show_details){
                if(aireplay_running==AIREPLAY_DEAUTH) str += " | Aireplay deauthenticating...";
                else if(aireplay_running==AIREPLAY_WEP) str += " | Aireplay replaying for wep...";
                if(wpa_thread.isAlive()) str += " | WPA cracking...";
                if(MDKFragment.bf) str += " | MDK3 Beacon Flooding...";
                if(MDKFragment.ados) str += " | MDK3 Authentication DoS...";
            }

            notif.setContentText(str);
            nm.notify(0, notif.build());
        }
    }
    static void isolate(String mac){
        is_ap = AP.getAPByMac(mac);
        if(is_ap!=null){
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
        if(MDKFragment.bf || MDKFragment.ados) state += 4;
        toolbar.setOverflowIcon(overflow[state]);
    }
    static String getManuf(String mac){
        String temp = mac.subSequence(0, 2).toString() + mac.subSequence(3, 5).toString() + mac.subSequence(6, 8).toString();
        shell4_in.print("grep -i " + temp + " " + path + "/oui.txt; echo ENDOFGREP\n");
        shell4_in.flush();
        String manuf = getLastLine(shell4_out, "ENDOFGREP");

        if(manuf=="ENDOFGREP" || manuf.length()<23) manuf = "Unknown Manufacturer";
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
            Log.d("getLastLine-out", buffer);
            while(!end.equals(buffer)){
                Log.d("getLastLine", buffer);
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