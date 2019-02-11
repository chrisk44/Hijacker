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

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import static android.widget.Toast.LENGTH_SHORT;
import static com.hijacker.AP.OPN;
import static com.hijacker.AP.UNKNOWN;
import static com.hijacker.MainActivity.CHROOT_BIN_MISSING;
import static com.hijacker.MainActivity.CHROOT_DIR_MISSING;
import static com.hijacker.MainActivity.CHROOT_FOUND;
import static com.hijacker.MainActivity.FRAGMENT_REAVER;
import static com.hijacker.MainActivity.NETHUNTER_BOOTKALI_BASH;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_REAVER;
import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.bootkali_init_bin;
import static com.hijacker.MainActivity.checkChroot;
import static com.hijacker.MainActivity.cont_on_fail;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.custom_chroot_cmd;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.last_action;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.monstart;
import static com.hijacker.MainActivity.notification;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.progress;
import static com.hijacker.MainActivity.reaver_dir;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.stop;

public class ReaverFragment extends Fragment{
    static ReaverTask task;

    View fragmentView, optionsContainer;
    Button start_button, select_button;
    TextView consoleView;
    EditText pinDelayView, lockedDelayView;
    CheckBox pixie_dust_cb, ignored_locked_cb, eap_fail_cb, small_dh_cb, no_nack_cb;
    ScrollView consoleScrollView;
    boolean autostart = false;

    //Dimensions to restore animated views
    int normalOptHeight = -1;
    //User options
    static String console_text = "", pin_delay="1", locked_delay="60", custom_mac=null;       //delays are always used as strings
    static boolean pixie_dust_enabled = true, pixie_dust, ignore_locked, eap_fail, small_dh, no_nack;
    static AP ap = null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        fragmentView = inflater.inflate(R.layout.reaver_fragment, container, false);
        setRetainInstance(true);

        optionsContainer = fragmentView.findViewById(R.id.options_container);
        consoleView = fragmentView.findViewById(R.id.console);
        consoleScrollView = fragmentView.findViewById(R.id.console_scroll_view);
        pinDelayView = fragmentView.findViewById(R.id.pin_delay);
        lockedDelayView = fragmentView.findViewById(R.id.locked_delay);
        pixie_dust_cb = fragmentView.findViewById(R.id.pixie_dust);
        ignored_locked_cb = fragmentView.findViewById(R.id.ignore_locked);
        eap_fail_cb = fragmentView.findViewById(R.id.eap_fail);
        small_dh_cb = fragmentView.findViewById(R.id.small_dh);
        no_nack_cb = fragmentView.findViewById(R.id.no_nack);
        select_button = fragmentView.findViewById(R.id.select_ap);
        start_button = fragmentView.findViewById(R.id.start_button);

        pinDelayView.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
                if(actionId == EditorInfo.IME_ACTION_NEXT){
                    lockedDelayView.requestFocus();
                    return true;
                }
                return false;
            }
        });

        if(task==null) task = new ReaverTask();

        int chroot_check = checkChroot();
        if(chroot_check!=CHROOT_FOUND){
            pixie_dust_cb.setEnabled(false);
            pixie_dust_enabled = false;
            if(chroot_check==CHROOT_DIR_MISSING) Toast.makeText(getActivity(), getString(R.string.chroot_notfound), LENGTH_SHORT).show();
            else if(chroot_check==CHROOT_BIN_MISSING) Toast.makeText(getActivity(), getString(R.string.kali_notfound), LENGTH_SHORT).show();
            else Toast.makeText(getActivity(), getString(R.string.chroot_both_notfound), LENGTH_SHORT).show();
        }else{
            pixie_dust_cb.setEnabled(true);
            pixie_dust_enabled = true;
        }

        select_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                PopupMenu popup = new PopupMenu(getActivity(), view);

                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
                int i = 0;
                for(AP ap : AP.APs){
                    popup.getMenu().add(0, i, i, ap.toString());
                    if(ap.sec==UNKNOWN  || ap.sec==OPN){
                        popup.getMenu().getItem(i).setEnabled(false);
                    }
                    i++;
                }
                popup.getMenu().add(1, i, i, "Custom");
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(android.view.MenuItem item) {
                        //ItemId = i in for()
                        if(item.getGroupId()==0){
                            custom_mac = null;
                            AP temp = AP.APs.get(item.getItemId());
                            if(ap!=temp){
                                ap = temp;
                            }
                            select_button.setText(ap.toString());
                        }else{
                            //Clcked custom
                            final EditTextDialog dialog = new EditTextDialog();
                            dialog.setTitle(getString(R.string.custom_ap_title));
                            dialog.setHint(getString(R.string.mac_address));
                            dialog.setRunnable(new Runnable(){
                                @Override
                                public void run(){
                                    ap = null;
                                    custom_mac = dialog.result;
                                    select_button.setText(dialog.result);
                                }
                            });
                            dialog.show(mFragmentManager, "EditTextDialog");
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });
        start_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(task.getStatus()!=AsyncTask.Status.RUNNING){
                    attemptStart();
                }else{
                    stop(PROCESS_REAVER);
                    task.cancel(true);
                }
            }
        });

        return fragmentView;
    }
    void attemptStart(){
        pinDelayView.setError(null);
        lockedDelayView.setError(null);

        if(ap==null && custom_mac==null){
            Snackbar.make(fragmentView, getString(R.string.select_ap), Snackbar.LENGTH_LONG).show();
        }else{
            if(pinDelayView.getText().toString().equals("")){
                pinDelayView.setError(getString(R.string.field_required));
                pinDelayView.requestFocus();
                return;
            }
            if(lockedDelayView.getText().toString().equals("")){
                lockedDelayView.setError(getString(R.string.field_required));
                lockedDelayView.requestFocus();
                return;
            }

            task = new ReaverTask();
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
    ReaverFragment setAutostart(boolean autostart){
        this.autostart = autostart;
        return this;
    }
    static boolean isRunning(){
        if(task==null) return false;
        return task.getStatus()==AsyncTask.Status.RUNNING;
    }
    static void stopReaver(){
        //Does NOT completely stop reaver, only the app's task
        //MainActivity.stop(PROCESS_REAVER) should be also called
        if(task!=null){
            task.cancel(true);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_REAVER;
        refreshDrawer();

        //Console text is saved/restored on pause/resume
        consoleView.setText(console_text);
        consoleView.post(new Runnable() {
            @Override
            public void run() {
                consoleScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
    @Override
    public void onPause(){
        super.onPause();

        //Console text is saved/restored on pause/resume
        console_text = consoleView.getText().toString();
    }
    @Override
    public void onStart(){
        super.onStart();

        //Restore options
        pinDelayView.setText(pin_delay);
        lockedDelayView.setText(locked_delay);
        pixie_dust_cb.setChecked(pixie_dust);
        pixie_dust_cb.setEnabled(pixie_dust_enabled);
        ignored_locked_cb.setChecked(ignore_locked);
        eap_fail_cb.setChecked(eap_fail);
        small_dh_cb.setChecked(small_dh);
        no_nack_cb.setChecked(no_nack);
        if(custom_mac!=null) select_button.setText(custom_mac);
        else if(ap!=null) select_button.setText(ap.toString());
        else if(!AP.marked.isEmpty()){
            ap = AP.marked.get(AP.marked.size()-1);
            select_button.setText(ap.toString());
        }
        start_button.setText(isRunning() ? R.string.stop : R.string.start);

        //Restore animated views
        if(task.getStatus()==AsyncTask.Status.RUNNING){
            ViewGroup.LayoutParams layoutParams = optionsContainer.getLayoutParams();
            layoutParams.height = 0;
            optionsContainer.setLayoutParams(layoutParams);
        }else if(normalOptHeight!=-1){
            ViewGroup.LayoutParams params = optionsContainer.getLayoutParams();
            params.height = normalOptHeight;
            optionsContainer.setLayoutParams(params);

            consoleScrollView.fullScroll(View.FOCUS_DOWN);
        }

        if(autostart){
            optionsContainer.post(new Runnable(){
                @Override
                public void run(){
                    attemptStart();
                }
            });
            autostart = false;
        }
    }
    @Override
    public void onStop(){
        if(task!=null){
            if(task.sizeAnimator!=null){
                task.sizeAnimator.cancel();
            }
        }

        //Backup options
        pin_delay = pinDelayView.getText().toString();
        locked_delay = lockedDelayView.getText().toString();
        pixie_dust = pixie_dust_cb.isChecked();
        pixie_dust_enabled = pixie_dust_cb.isEnabled();
        ignore_locked = ignored_locked_cb.isChecked();
        eap_fail = eap_fail_cb.isChecked();
        small_dh = small_dh_cb.isChecked();
        no_nack = no_nack_cb.isChecked();

        super.onStop();
    }
    static String get_chroot_env(final Activity activity){
        // add strings here , they will be in the kali env
        String[] ENV = {
                "USER=root",
                "SHELL=/bin/bash",
                "MAIL=/var/mail/root",
                "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                "TERM=linux",
                "HOME=/root",
                "LOGNAME=root",
                "SHLVL=1",
                "YOU_KNOW_WHAT=THIS_IS_KALI_LINUX_NETHUNER_FROM_JAVA_BINKY"
        };
        String ENV_OUT = "";
        for (String aENV : ENV) {
            ENV_OUT = ENV_OUT + "export " + aENV + " && ";
        }
        if(monstart){
            ENV_OUT += "source monstart-nh";
            ENV_OUT += cont_on_fail ? "; " : " && ";
        }
        if(!custom_chroot_cmd.equals("")){
            if(custom_chroot_cmd.contains("'") && activity!=null){
                runInHandler(new Runnable(){
                    @Override
                    public void run(){
                        Toast.makeText(activity, activity.getString(R.string.custom_chroot_cmd_illegal), Toast.LENGTH_SHORT).show();
                    }
                });
            }else{
                ENV_OUT += custom_chroot_cmd;
                ENV_OUT += cont_on_fail ? "; " : " && ";
            }
        }
        return ENV_OUT;
    }
    class ReaverTask extends AsyncTask<Void, String, Boolean>{
        String pinDelay, lockedDelay;
        boolean ignoreLocked, eapFail, smallDH, pixieDust, noNack;
        ValueAnimator sizeAnimator;
        @Override
        protected void onPreExecute(){
            pinDelay = pinDelayView.getText().toString();
            lockedDelay = lockedDelayView.getText().toString();
            ignoreLocked = ignored_locked_cb.isChecked();
            eapFail = eap_fail_cb.isChecked();
            smallDH = small_dh_cb.isChecked();
            pixieDust = pixie_dust_cb.isChecked();
            noNack = no_nack_cb.isChecked();

            start_button.setText(R.string.stop);
            progress.setIndeterminate(true);

            normalOptHeight = optionsContainer.getHeight();

            sizeAnimator = ValueAnimator.ofInt(optionsContainer.getHeight(), 0);
            sizeAnimator.setTarget(optionsContainer);
            sizeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
                @Override
                public void onAnimationUpdate(ValueAnimator animation){
                    ViewGroup.LayoutParams layoutParams = optionsContainer.getLayoutParams();
                    layoutParams.height = (int)animation.getAnimatedValue();
                    optionsContainer.setLayoutParams(layoutParams);
                }
            });
            sizeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            sizeAnimator.start();
        }
        @Override
        protected Boolean doInBackground(Void... params){
            last_action = System.currentTimeMillis();
            stop(PROCESS_AIRODUMP);            //Can't have channels changing from anywhere else
            try{
                BufferedReader out;
                String args = "-i " + iface + " -vv";
                args += ap==null ? " -b " + custom_mac : " -b " + ap.mac + " --channel " + ap.ch;
                args += " -d " + pinDelay;
                args += " -l " + lockedDelay;
                if(ignoreLocked) args += " -L";
                if(eapFail) args += " -E";
                if(smallDH) args += " -S";
                if(noNack) args += " -N";
                String cmd;
                if(pixieDust){
                    publishProgress(getString(R.string.chroot_warning));
                    if(bootkali_init_bin.equals(NETHUNTER_BOOTKALI_BASH)){
                        //Not in nethunter, need to initialize the chroot environment
                        Runtime.getRuntime().exec("su -c " + bootkali_init_bin);       //Make sure kali has booted
                    }
                    args += " -K 1";
                    cmd = "chroot " + MainActivity.chroot_dir + " /bin/bash -c \'" + get_chroot_env(getActivity()) + "reaver " + args + "\'";
                    publishProgress("\nRunning: " + cmd);
                    ProcessBuilder pb = new ProcessBuilder("su");
                    pb.redirectErrorStream(true);
                    Process dc = pb.start();
                    out = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                    PrintWriter in = new PrintWriter(dc.getOutputStream());
                    in.print(cmd + "\nexit\n");
                    in.flush();
                }else{
                    cmd = "su -c " + prefix + " " + reaver_dir + " " + args;
                    publishProgress("\nRunning: " + cmd);
                    Process dc = Runtime.getRuntime().exec(cmd);
                    out = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                }
                if(debug) Log.d("HIJACKER/ReaverFragment", cmd);

                String buffer;
                while(!isCancelled() && (buffer = out.readLine())!=null){
                    publishProgress(buffer);
                }
                publishProgress("Done");
            }catch(IOException e){
                Log.e("HIJACKER/Exception", "Caught Exception in ReaverFragment: " + e.toString());
            }

            return true;
        }
        @Override
        protected void onProgressUpdate(String... text){
            text[0] += '\n';
            if(currentFragment==FRAGMENT_REAVER && !background){
                consoleView.append(text[0]);
                consoleScrollView.fullScroll(View.FOCUS_DOWN);
            }else{
                console_text += text[0];
            }
        }
        @Override
        protected void onPostExecute(final Boolean success){
            done();
        }
        @Override
        protected void onCancelled(){
            done();
        }
        void done(){
            start_button.setText(R.string.start);
            progress.setIndeterminate(false);

            sizeAnimator = ValueAnimator.ofInt(0, normalOptHeight);
            sizeAnimator.setTarget(optionsContainer);
            sizeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
                @Override
                public void onAnimationUpdate(ValueAnimator animation){
                    ViewGroup.LayoutParams layoutparams = optionsContainer.getLayoutParams();
                    layoutparams.height = (int)animation.getAnimatedValue();
                    optionsContainer.setLayoutParams(layoutparams);
                }
            });
            sizeAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation){}
                @Override
                public void onAnimationEnd(Animator animation){
                    consoleScrollView.fullScroll(View.FOCUS_DOWN);
                }
                @Override
                public void onAnimationCancel(Animator animation){}
                @Override
                public void onAnimationRepeat(Animator animation){}
            });
            sizeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            sizeAnimator.start();

            notification();
        }
    }
}

