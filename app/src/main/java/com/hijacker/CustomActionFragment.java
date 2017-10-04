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
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import static com.hijacker.CustomAction.TYPE_AP;
import static com.hijacker.CustomAction.TYPE_ST;
import static com.hijacker.CustomAction.cmds;
import static com.hijacker.MainActivity.FRAGMENT_CUSTOM;
import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.busybox;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.getPIDs;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.progress;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.Shell.runOne;

public class CustomActionFragment extends Fragment{
    static CustomActionTask task;
    static CustomAction selectedAction = null;
    static Device targetDevice;
    static String console_text = null;

    View fragmentView;
    static View optionsContainer;
    Button startBtn, targetBtn, actionBtn;
    TextView consoleView;
    ScrollView consoleScrollView;
    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){
        setRetainInstance(true);
        fragmentView = inflater.inflate(R.layout.custom_action_fragment, container, false);

        consoleView = (TextView)fragmentView.findViewById(R.id.console);
        consoleScrollView = (ScrollView)fragmentView.findViewById(R.id.console_scroll_view);
        optionsContainer = fragmentView.findViewById(R.id.options_container);
        startBtn = (Button)fragmentView.findViewById(R.id.start_button);
        targetBtn = (Button)fragmentView.findViewById(R.id.select_target);
        actionBtn = (Button)fragmentView.findViewById(R.id.select_action);

        if(task==null) task = new CustomActionTask();

        actionBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                showActionSelector();
            }
        });
        targetBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                showTargetSelector();
            }
        });
        startBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(isRunning()){
                    //Stop
                    startBtn.setEnabled(false);
                    task.cancel(true);
                }else{
                    task = new CustomActionTask();
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });

        //Restore view
        consoleView.setText(console_text);
        consoleView.post(new Runnable() {
            @Override
            public void run() {
                consoleScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
        if(selectedAction!=null){
            actionBtn.setText(selectedAction.getTitle());
            targetBtn.setEnabled(true);
            if(targetDevice!=null){
                targetBtn.setText(targetDevice.toString());
                startBtn.setEnabled(true);
            }
        }
        startBtn.setText(isRunning() ? R.string.stop : R.string.start);

        if(task.getStatus()==AsyncTask.Status.RUNNING){
            ViewGroup.LayoutParams layoutParams = optionsContainer.getLayoutParams();
            layoutParams.height = 0;
            optionsContainer.setLayoutParams(layoutParams);
        }

        return fragmentView;
    }
    @Override
    public void onResume(){
        super.onResume();
        currentFragment = FRAGMENT_CUSTOM;
        refreshDrawer();
    }
    @Override
    public void onPause(){
        super.onPause();
        console_text = consoleView.getText().toString();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if(task.getStatus()!= AsyncTask.Status.RUNNING){
            //Avoid memory leak
            optionsContainer = null;
        }
    }
    static boolean isRunning(){
        if(task==null) return false;
        return task.getStatus()==AsyncTask.Status.RUNNING;
    }

    void showActionSelector(){
        PopupMenu popup = new PopupMenu(getActivity(), actionBtn);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

        //add(groupId, itemId, order, title)
        int i;
        for(i=0;i<cmds.size();i++){
            popup.getMenu().add(cmds.get(i).getType(), i, i, cmds.get(i).getTitle());
        }
        popup.getMenu().add(-1, 0, i+1, getString(R.string.manage_actions));

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(android.view.MenuItem item){
                if(item.getGroupId()==-1){
                    //Open actions manager
                    FragmentTransaction ft = mFragmentManager.beginTransaction();
                    ft.replace(R.id.fragment1, new CustomActionManagerFragment());
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    ft.addToBackStack(null);
                    ft.commitAllowingStateLoss();
                }else{
                    onActionSelected(cmds.get(item.getItemId()));
                }
                return true;
            }
        });
        popup.show();
    }
    void showTargetSelector(){
        PopupMenu popup = new PopupMenu(getActivity(), targetBtn);
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

        //add(groupId, itemId, order, title)
        int i;
        if(selectedAction.getType()==TYPE_AP){
            i = 0;
            for(AP ap : AP.APs){
                popup.getMenu().add(TYPE_AP, i, i, ap.toString());
                if(selectedAction.requiresClients() && ap.clients.size()==0){
                    popup.getMenu().findItem(i).setEnabled(false);
                }
                i++;
            }
        }else{
            i = 0;
            for(ST st : ST.STs){
                popup.getMenu().add(TYPE_ST, i, i, st.toString());
                if(selectedAction.requiresConnected() && st.bssid==null){
                    popup.getMenu().findItem(i).setEnabled(false);
                }
                i++;
            }
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
            public boolean onMenuItemClick(android.view.MenuItem item){
                switch(item.getGroupId()){
                    case TYPE_AP:
                        //ap
                        onTargetSelected(AP.APs.get(item.getItemId()));
                        break;
                    case TYPE_ST:
                        //st
                        onTargetSelected(ST.STs.get(item.getItemId()));
                        break;
                }
                return true;
            }
        });
        if(popup.getMenu().size()>0) popup.show();
    }

    void onActionSelected(CustomAction newAction){
        targetBtn.setEnabled(true);

        if(selectedAction!=null){
            if(newAction.getType()!=selectedAction.getType()){
                //Different types
                targetDevice = null;
                targetBtn.setText(getString(R.string.select_target));

                startBtn.setEnabled(false);
            }
        }

        selectedAction = newAction;
        actionBtn.setText(selectedAction.getTitle());
    }
    void onTargetSelected(Device newDevice){
        targetDevice = newDevice;

        targetBtn.setText(targetDevice.toString());
        startBtn.setEnabled(true);
    }

    class CustomActionTask extends AsyncTask<Void, String, Boolean>{
        Shell shell;
        int prevOptContainerHeight = -1;
        @Override
        protected void onPreExecute(){
            startBtn.setText(R.string.stop);
            progress.setIndeterminate(true);

            consoleView.append("Running: " + selectedAction.getStartCmd() + '\n');
            consoleScrollView.fullScroll(View.FOCUS_DOWN);
            if(debug) Log.d("HIJACKER/CustomCMDFrag", "Running: " + selectedAction.getStartCmd());

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
            shell = Shell.getFreeShell();

            //Start the action
            selectedAction.run(shell, targetDevice);

            //Read output until it's finished or cancelled
            BufferedReader out = shell.getShell_out();
            try{
                String end = "ENDOFCUSTOM";
                String buffer = out.readLine();
                while(!end.equals(buffer) && !isCancelled()){
                    if(currentFragment==FRAGMENT_CUSTOM && !background){
                        publishProgress(buffer + '\n');
                    }else{
                        console_text += buffer + '\n';
                    }
                    buffer = out.readLine();
                }
                if(debug) Log.d("HIJACKER/CustomCMDFrag", "thread done");
            }catch(IOException ignored){
                return false;
            }

            if(isCancelled()){
                if(selectedAction.hasProcessName()){
                    if(debug) Log.d("HIJACKER/CustomCMDFrag", "Killing process named " + selectedAction.getProcessName());
                    publishProgress("Killing process named " + selectedAction.getProcessName() + '\n');

                    ArrayList<Integer> list = getPIDs(selectedAction.getProcessName());
                    for(int i=0;i<list.size();i++){
                        runOne(busybox + " kill " + list.get(i));
                    }
                }

                if(selectedAction.hasStopCmd()){
                    if(debug) Log.d("HIJACKER/CustomCMDFrag", "Running: " + selectedAction.getStopCmd());
                    publishProgress("Running: " + selectedAction.getStopCmd() + '\n');

                    runOne(selectedAction.getStopCmd());
                }
                publishProgress("Interrupted\n");
            }else{
                publishProgress("Done\n");
            }

            if(shell!=null) shell.done();

            return true;
        }
        @Override
        protected void onProgressUpdate(String... text){
            if(currentFragment==FRAGMENT_CUSTOM && !background){
                consoleView.append(text[0] + '\n');
                consoleScrollView.fullScroll(View.FOCUS_DOWN);
            }else{
                console_text += text[0] + '\n';
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
            startBtn.setEnabled(true);
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
