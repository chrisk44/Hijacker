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

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.hijacker.MainActivity.FRAGMENT_CRACK;
import static com.hijacker.MainActivity.PROCESS_AIRCRACK;
import static com.hijacker.MainActivity.aircrack_dir;
import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.busybox;
import static com.hijacker.MainActivity.cap_dir;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.getLastLine;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.progress;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.stop;

public class CrackFragment extends Fragment{
    static final int WPA=2, WEP=1;
    View fragmentView;
    static View optionsContainer;
    TextView consoleView;
    EditText capfileView, wordlistView;
    RadioGroup wepRG, securityRG;
    RadioButton wepRB, wpaRB;
    Button startBtn, capFeBtn, wordlistFeBtn;
    ScrollView consoleScrollView;
    static CrackTask task;
    static String capfile, wordlist, console_text, capfile_text=null, wordlist_text=null;
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){
        fragmentView = inflater.inflate(R.layout.crack_fragment, container, false);

        consoleView = (TextView)fragmentView.findViewById(R.id.console);
        consoleScrollView = (ScrollView)fragmentView.findViewById(R.id.console_scroll_view);
        capfileView = (EditText)fragmentView.findViewById(R.id.capfile);
        wordlistView = (EditText)fragmentView.findViewById(R.id.wordlist);
        capFeBtn = (Button)fragmentView.findViewById(R.id.cap_fe_btn);
        wordlistFeBtn = (Button)fragmentView.findViewById(R.id.wordlist_fe_btn);
        wepRG = (RadioGroup)fragmentView.findViewById(R.id.wep_rg);
        securityRG = (RadioGroup)fragmentView.findViewById(R.id.radio_group);
        wepRB = (RadioButton)fragmentView.findViewById(R.id.wep_rb);
        wpaRB = (RadioButton)fragmentView.findViewById(R.id.wpa_rb);
        startBtn = (Button)fragmentView.findViewById(R.id.start);

        consoleView.setText("");

        capfileView.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
                if(actionId == EditorInfo.IME_ACTION_NEXT){
                    wordlistView.requestFocus();
                    return true;
                }
                return false;
            }
        });

        for (int i = 0; i < wepRG.getChildCount(); i++) {
            //Disable all the WEP options
            wepRG.getChildAt(i).setEnabled(false);
        }

        if(task==null) task = new CrackTask();

        wepRB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b){
                for (int i = 0; i < wepRG.getChildCount(); i++) {
                    //If wep is now checked, enable the wep options, otherwise disable them
                    wepRG.getChildAt(i).setEnabled(b);
                }
            }
        });
        startBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(!isRunning()){
                    attemptStart();
                }else{
                    task.cancel(true);
                }
            }
        });
        capFeBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                final FileExplorerDialog dialog = new FileExplorerDialog();
                dialog.setStartingDir(new RootFile(cap_dir));
                dialog.setToSelect(FileExplorerDialog.SELECT_EXISTING_FILE);
                dialog.setOnSelect(new Runnable(){
                    @Override
                    public void run(){
                        capfileView.setText(dialog.result.getAbsolutePath());
                        capfileView.setError(null);
                    }
                });
                dialog.show(getFragmentManager(), "FileExplorerDialog");
            }
        });
        wordlistFeBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                final FileExplorerDialog dialog = new FileExplorerDialog();
                dialog.setStartingDir(new RootFile(Environment.getExternalStorageDirectory().toString()));
                dialog.setToSelect(FileExplorerDialog.SELECT_EXISTING_FILE);
                dialog.setOnSelect(new Runnable(){
                    @Override
                    public void run(){
                        wordlistView.setText(dialog.result.getAbsolutePath());
                        wordlistView.setError(null);
                    }
                });
                dialog.show(getFragmentManager(), "FileExplorerDialog");
            }
        });

        //Restore view
        if(capfile_text!=null){
            capfileView.setText(capfile_text);
        }else{
            Shell shell = Shell.getFreeShell();
            shell.run(busybox + " ls -1 " + cap_dir + "/handshake-*.cap; echo ENDOFLS");
            capfile = getLastLine(shell.getShell_out(), "ENDOFLS");
            if(!capfile.equals("ENDOFLS") && capfile.charAt(0)!='l'){
                capfileView.setText(capfile);
            }
            shell.done();
        }
        if(wordlist_text!=null) wordlistView.setText(wordlist_text);
        consoleView.setText(console_text);
        consoleView.post(new Runnable() {
            @Override
            public void run() {
                consoleScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });

        return fragmentView;
    }
    @Override
    public void onResume() {
        super.onResume();
        currentFragment = FRAGMENT_CRACK;
        refreshDrawer();
    }
    @Override
    public void onPause(){
        super.onPause();
        console_text = consoleView.getText().toString();
        capfile_text = capfileView.getText().toString();
        wordlist_text = wordlistView.getText().toString();
    }
    @Override
    public void onStart(){
        super.onStart();
        optionsContainer = fragmentView.findViewById(R.id.options_container);
        if(task.getStatus()==AsyncTask.Status.RUNNING){
            ViewGroup.LayoutParams layoutParams = optionsContainer.getLayoutParams();
            layoutParams.height = 0;
            optionsContainer.setLayoutParams(layoutParams);
        }
    }
    @Override
    public void onStop(){
        if(task.getStatus()!= AsyncTask.Status.RUNNING){
            //Avoid memory leak
            optionsContainer = null;
        }
        super.onStop();
    }
    static boolean isRunning(){
        if(task==null) return false;
        return task.getStatus()==AsyncTask.Status.RUNNING;
    }
    static void stopCracking(){
        //Does NOT completely stop the cracking process, only the app's task
        //MainActivity.stop(PROCESS_AIRCRACK) should be also called
        if(task!=null){
            task.cancel(true);
        }
    }
    void attemptStart(){
        capfileView.setError(null);
        wordlistView.setError(null);
        capfile = capfileView.getText().toString();
        wordlist = wordlistView.getText().toString();

        if(!capfile.startsWith("/")){
            capfileView.setError(getString(R.string.capfile_invalid));
            capfileView.requestFocus();
            return;
        }
        if(!wordlist.startsWith("/")){
            wordlistView.setError(getString(R.string.wordlist_invalid));
            wordlistView.requestFocus();
            return;
        }
        RootFile cap = new RootFile(capfile);
        RootFile word = new RootFile(wordlist);
        if(!cap.exists() || !cap.isFile()){
            capfileView.setError(getString(R.string.cap_notfound));
            capfileView.requestFocus();
            return;
        }
        if(!word.exists() || !word.isFile()){
            wordlistView.setError(getString(R.string.wordlist_notfound));
            wordlistView.requestFocus();
            return;
        }

        switch(securityRG.getCheckedRadioButtonId()){
            case -1:
                //Mode not selected
                Snackbar.make(fragmentView, getString(R.string.select_wpa_wep), Snackbar.LENGTH_SHORT).show();
                return;
            case R.id.wep_rb:
                if(wepRG.getCheckedRadioButtonId()==-1){
                    //WEP is selected but we need to have a wep bit length selection
                    Snackbar.make(fragmentView, getString(R.string.select_wep_bits), Snackbar.LENGTH_SHORT).show();
                    return;
                }
        }

        task = new CrackTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    class CrackTask extends AsyncTask<Void, String, Boolean>{
        int mode;
        String cmd, key;
        int prevOptContainerHeight = -1;
        @Override
        protected void onPreExecute(){
            progress.setIndeterminate(true);
            startBtn.setText(R.string.stop);
            consoleView.append("\nRunning...\n");
            consoleScrollView.fullScroll(View.FOCUS_DOWN);

            switch(securityRG.getCheckedRadioButtonId()){
                case R.id.wpa_rb:
                    //WPA
                    mode = WPA;
                    break;
                case R.id.wep_rb:
                    //WEP
                    mode = WEP;
                    break;
            }
            //Create command
            cmd = "su -c " + aircrack_dir + " " + capfile + " -l " + path + "/aircrack-out.txt -a " + mode;
            if(wordlist!=null) cmd += " -w " + wordlist;
            if(mode==WEP){
                cmd += " -n ";
                switch(wepRG.getCheckedRadioButtonId()){
                    case R.id.wep_64:
                        cmd += "64";
                        break;
                    case R.id.wep_128:
                        cmd += "128";
                        break;
                    case R.id.wep_152:
                        cmd += "152";
                        break;
                    case R.id.wep_256:
                        cmd += "256";
                        break;
                    case R.id.wep_512:
                        cmd += "512";
                        break;
                }
            }
            if(debug) Log.d("HIJACKER/CrackTask", cmd);

            prevOptContainerHeight = optionsContainer.getHeight();

            ValueAnimator sizeAnimator = ValueAnimator.ofInt(optionsContainer.getHeight(), 0);
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
            try{
                //Run aircrack and wait to either finish or be cancelled
                Process dc = Runtime.getRuntime().exec(cmd);
                BufferedReader out = new BufferedReader(new InputStreamReader(dc.getInputStream()));
                while(!isCancelled() && out.readLine()!=null){
                    Thread.sleep(100);
                }
            }catch(IOException | InterruptedException e){
                Log.e("HIJACKER/CrackTask", e.toString());
                return false;
            }

            if(new File(path + "/aircrack-out.txt").exists()){
                Shell shell = Shell.getFreeShell();     //Using root shell because "new File(path + "/aircrack-out.txt");" throws FileNotFoundException (permission denied)
                BufferedReader out = shell.getShell_out();
                shell.run("cat " + path + "/aircrack-out.txt; echo ");              //No newline at the end of the file, readLine will hang
                try{
                    key = out.readLine();
                }catch(IOException ignored){}
                shell.run("rm " + path + "/aircrack-out.txt");
                shell.done();
                return true;
            }

            return false;
        }
        @Override
        protected void onPostExecute(final Boolean success){
            done();
            String str;
            if(success){
                str = "Key found: " + key + '\n';
            }else{
                str = "Key not found\n";
                if(mode==WEP) str += "Try with different wep bit selection or more IVs\n";
            }
            if(currentFragment==FRAGMENT_CRACK && !background){
                consoleScrollView.fullScroll(View.FOCUS_DOWN);
                consoleView.append(str);
            }else{
                console_text += str;
            }
        }
        @Override
        protected void onCancelled(){
            done();
            consoleView.append("Interrupted\n");
            consoleScrollView.fullScroll(View.FOCUS_DOWN);
            stop(PROCESS_AIRCRACK);
        }
        void done(){
            startBtn.setText(R.string.start);
            progress.setIndeterminate(false);

            ValueAnimator sizeAnimator = ValueAnimator.ofInt(0, prevOptContainerHeight);
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
                public void onAnimationStart(Animator animation) {}
                @Override
                public void onAnimationEnd(Animator animation) {
                    consoleScrollView.fullScroll(View.FOCUS_DOWN);
                }
                @Override
                public void onAnimationCancel(Animator animation) {}
                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
            sizeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

            sizeAnimator.start();
        }
    }
}

