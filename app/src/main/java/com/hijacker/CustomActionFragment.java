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

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;

import static com.hijacker.CustomAction.TYPE_AP;
import static com.hijacker.CustomAction.TYPE_ST;
import static com.hijacker.CustomAction.cmds;
import static com.hijacker.MainActivity.FRAGMENT_CUSTOM;
import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.progress;
import static com.hijacker.MainActivity.refreshDrawer;

public class CustomActionFragment extends Fragment{
    static CustomAction selected_action=null;
    static AP ap=null;
    static ST st=null;
    static Shell shell=null;
    static Thread thread;
    static Runnable runnable;
    static boolean cont=false;
    Button start_button, select_target, select_action;
    TextView console;
    static String console_text = null;
    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.custom_action_fragment, container, false);

        runnable = new Runnable(){
            @Override
            public void run(){
                if(debug) Log.d("HIJACKER/CustomCMDFrag", "thread running");
                BufferedReader out = shell.getShell_out();
                try{
                    Message msg;
                    cont = true;
                    String end = "ENDOFCUSTOM";
                    String buffer = out.readLine();
                    while(!end.equals(buffer) && cont){
                        msg = new Message();
                        msg.obj = buffer;
                        refresh.sendMessage(msg);
                        buffer = out.readLine();
                    }
                    stop.obtainMessage().sendToTarget();
                    if(debug) Log.d("HIJACKER/CustomCMDFrag", "thread done");
                }catch(IOException ignored){}
            }
        };
        thread = new Thread(runnable);

        console = (TextView)v.findViewById(R.id.console);
        console.setMovementMethod(new ScrollingMovementMethod());
        start_button = (Button)v.findViewById(R.id.start_button);
        select_target = (Button)v.findViewById(R.id.select_target);
        select_action = (Button)v.findViewById(R.id.select_action);

        select_action.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                PopupMenu popup = new PopupMenu(getActivity(), view);
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
                            int prev_type = -1;
                            if(selected_action!=null){
                                prev_type = selected_action.getType();
                            }
                            selected_action = cmds.get(item.getItemId());
                            select_action.setText(selected_action.getTitle());
                            if(prev_type==-1){
                                ap = null;
                                st = null;
                            }else{
                                if(selected_action.getType()==TYPE_AP && prev_type==TYPE_ST){
                                    st = null;
                                }else if(selected_action.getType()==TYPE_ST && prev_type==TYPE_AP){
                                    ap = null;
                                }
                            }
                            select_target.setEnabled(true);
                            select_target.setText(R.string.select_target);
                            start_button.setEnabled(false);
                            setTarget();
                            stop.obtainMessage().sendToTarget();
                        }
                        return true;
                    }
                });
                popup.show();
            }
        });

        select_target.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                PopupMenu popup = new PopupMenu(getActivity(), view);
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

                //add(groupId, itemId, order, title)
                int i;
                if(selected_action.getType()==TYPE_AP){
                    AP temp;
                    for(i = 0; i<AP.APs.size(); i++){
                        temp = AP.APs.get(i);
                        popup.getMenu().add(TYPE_AP, i, i, temp.essid + " (" + temp.mac + ")");
                        if(selected_action.requires_clients() && temp.clients.size()==0){
                            popup.getMenu().findItem(i).setEnabled(false);
                        }
                    }
                }else{
                    ST temp;
                    for(i = 0; i<ST.STs.size(); i++){
                        temp = ST.STs.get(i);
                        popup.getMenu().add(TYPE_ST, i, i, temp.mac + ((temp.bssid==null) ? "" : " (" + AP.getAPByMac(temp.bssid).essid + ")"));
                        if(selected_action.requires_connected() && temp.bssid==null){
                            popup.getMenu().findItem(i).setEnabled(false);
                        }
                    }
                }

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
                    public boolean onMenuItemClick(android.view.MenuItem item){
                        switch(item.getGroupId()){
                            case TYPE_AP:
                                //ap
                                ap = AP.APs.get(item.getItemId());
                                st = null;
                                break;
                            case TYPE_ST:
                                //st
                                st = ST.STs.get(item.getItemId());
                                ap = null;
                                break;
                        }
                        select_target.setText(item.getTitle());
                        stop.obtainMessage().sendToTarget();
                        start_button.setEnabled(true);
                        return true;
                    }
                });
                if(popup.getMenu().size()>0) popup.show();

            }
        });

        start_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(!thread.isAlive()){
                    //not started
                    shell = Shell.getFreeShell();
                    console.append("Running: " + selected_action.getStart_cmd() + '\n');
                    if(debug) Log.d("HIJACKER/CustomCMDFrag", "Running: " + selected_action.getStart_cmd());
                    selected_action.run();
                    start_button.setText(R.string.stop);
                    progress.setIndeterminate(true);
                }else{
                    //started
                    console.append("Running: " + selected_action.getStop_cmd() + '\n');
                    if(selected_action.hasProcessName()){
                        console.append("Killing process named " + selected_action.getProcess_name() + '\n');
                        if(debug) Log.d("HIJACKER/CustomCMDFrag", "Killing process named " + selected_action.getProcess_name());
                    }
                    if(debug) Log.d("HIJACKER/CustomCMDFrag", "Running: " + selected_action.getStop_cmd());
                    cont = false;
                    selected_action.stop();
                    stop.obtainMessage().sendToTarget();
                }
            }
        });

        return v;
    }
    @Override
    public void onResume(){
        super.onResume();
        currentFragment = FRAGMENT_CUSTOM;
        console.setText(console_text);
        if(selected_action!=null){
            select_action.setText(selected_action.getTitle());
            select_target.setEnabled(true);
            setTarget();
        }
        start_button.setText(thread.isAlive() ? R.string.stop : R.string.start);
        refreshDrawer();
    }
    @Override
    public void onPause(){
        super.onPause();
        console_text = console.getText().toString();
    }
    void setTarget(){
        if(ap!=null){
            start_button.setEnabled(true);
            select_target.setText(ap.essid + " (" + ap.mac + ")");
            return;
        }
        if(st!=null){
            start_button.setEnabled(true);
            select_target.setText(st.mac + ((st.bssid==null) ? "" : " (" + AP.getAPByMac(st.bssid).essid + ")"));
            return;
        }
        if(selected_action.getType()==TYPE_AP && !AP.marked.isEmpty()){
            start_button.setEnabled(true);

            ap = AP.marked.get(AP.marked.size()-1);
            select_target.setText(ap.essid + " (" + ap.mac + ")");

        }else if(selected_action.getType()==TYPE_ST && !ST.marked.isEmpty()){
            start_button.setEnabled(true);

            st = ST.marked.get(ST.marked.size()-1);
            select_target.setText(st.mac + ((st.bssid==null) ? "" : " (" + AP.getAPByMac(st.bssid).essid + ")"));
        }
    }

    public Handler stop = new Handler(){
        public void handleMessage(Message msg){
            start_button.setText(R.string.start);
            cont = false;
            progress.setIndeterminate(false);
            if(shell!=null) shell.done();
            shell = null;
        }
    };
    public Handler refresh = new Handler(){
        public void handleMessage(Message msg){
            if(currentFragment==FRAGMENT_CUSTOM && !background){
                console.append((String)msg.obj + '\n');
            }else{
                console_text += (String)msg.obj + '\n';
            }
        }
    };
}
