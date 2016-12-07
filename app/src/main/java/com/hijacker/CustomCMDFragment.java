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
import java.util.ArrayList;
import java.util.List;

import static com.hijacker.CustomCMD.TYPE_AP;
import static com.hijacker.CustomCMD.TYPE_ST;
import static com.hijacker.MainActivity.FRAGMENT_CUSTOM;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.progress;

public class CustomCMDFragment extends Fragment{
    static List<CustomCMD> st_cmds = new ArrayList<>();
    static List<CustomCMD> ap_cmds = new ArrayList<>();
    static CustomCMD selected_cmd=null;
    static AP ap=null;
    static ST st=null;
    static Shell shell=null;
    static Thread thread;
    static boolean cont;
    Button start_button, select_target, select_cmd;
    TextView console;
    static String console_text = null;
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.custom_fragment, container, false);

        thread = new Thread(new Runnable(){
            @Override
            public void run(){
                if(debug) Log.d("CustomCMDFragment", "thread running");
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
                    if(debug) Log.d("CustomCMDFragment", "thread done");
                }catch(IOException ignored){}
            }
        });

        console = (TextView)v.findViewById(R.id.console);
        console.setMovementMethod(new ScrollingMovementMethod());
        start_button = (Button)v.findViewById(R.id.start_button);
        select_target = (Button)v.findViewById(R.id.select_target);
        select_cmd = (Button)v.findViewById(R.id.select_cmd);

        if(selected_cmd!=null){
            select_cmd.setText(selected_cmd.getTitle());
            select_target.setEnabled(true);
        }
        if(ap!=null){
            start_button.setEnabled(true);
            select_target.setText(ap.essid + " (" + ap.mac + ")");
        }
        if(st!=null){
            start_button.setEnabled(true);
            select_target.setText(st.mac + " (" + st.bssid + ")");
        }
        if(thread.isAlive()){
            start_button.setText(R.string.stop);
        }

        select_cmd.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                PopupMenu popup = new PopupMenu(getActivity(), view);
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

                //add(groupId, itemId, order, title)
                int i, j;
                for(i=0;i<ap_cmds.size();i++){
                    popup.getMenu().add(TYPE_AP, i, i, ap_cmds.get(i).getTitle());
                }
                for(j=0;j<st_cmds.size();j++){
                    popup.getMenu().add(TYPE_ST, j, i+j, st_cmds.get(j).getTitle());
                }
                popup.getMenu().add(100, 0, i+j+1, "Manage commands...");

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(android.view.MenuItem item){
                        switch(item.getGroupId()){
                            case 100:
                                //Open commands manager
                                FragmentTransaction ft = getFragmentManager().beginTransaction();
                                ft.replace(R.id.fragment1, new CustomCMDManagerFragment());
                                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                                ft.addToBackStack(null);
                                ft.commit();
                                return true;
                            case TYPE_AP:
                                //ap commands
                                selected_cmd = ap_cmds.get(item.getItemId());
                                break;
                            case TYPE_ST:
                                //st commands
                                selected_cmd = st_cmds.get(item.getItemId());
                                break;
                        }
                        stop.obtainMessage().sendToTarget();
                        select_cmd.setText(selected_cmd.getTitle());
                        select_target.setEnabled(true);
                        select_target.setText(R.string.select_target);
                        start_button.setEnabled(false);
                        ap = null;
                        st = null;
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
                int i, added=0;
                if(selected_cmd.getType()==TYPE_AP){
                    AP temp;
                    for(i = 0; i<AP.APs.size(); i++){
                        temp = AP.APs.get(i);
                        if(!selected_cmd.requires_clients() || temp.clients.size()>0){
                            popup.getMenu().add(TYPE_AP, i, i, temp.essid + " (" + temp.mac + ")");
                            added++;
                        }
                    }
                }else{
                    ST temp;
                    for(i = 0; i<ST.STs.size(); i++){
                        temp = ST.STs.get(i);
                        if(!selected_cmd.requires_connected() || temp.bssid!=null){
                            popup.getMenu().add(TYPE_ST, i, i, temp.mac + " (" + temp.bssid + ")");
                            added++;
                        }
                    }
                }

                if(added>0){    //If we call show() but there are not items to show, there will be an empty popup, so the next touch will be "ignored"
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
                        public boolean onMenuItemClick(android.view.MenuItem item){
                            switch(item.getGroupId()){
                                case TYPE_AP:
                                    //ap
                                    ap = AP.APs.get(item.getItemId());
                                    st = null;
                                    select_target.setText(ap.essid + " (" + ap.mac + ")");
                                    break;
                                case TYPE_ST:
                                    //st
                                    st = ST.STs.get(item.getItemId());
                                    ap = null;
                                    select_target.setText(st.mac + " (" + st.bssid + ")");
                                    break;
                            }
                            stop.obtainMessage().sendToTarget();
                            start_button.setEnabled(true);
                            return true;
                        }
                    });
                    popup.show();
                }
            }
        });

        start_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(!thread.isAlive()){        //same effect with !thread.isAlive()
                    //not started
                    shell = Shell.getFreeShell();
                    console.append("Running: " + selected_cmd.getStart_cmd() + '\n');
                    if(debug) Log.d("CustomCMDFragment", "Running: " + selected_cmd.getStart_cmd());
                    selected_cmd.run();
                    start_button.setText(R.string.stop);
                    progress.setIndeterminate(true);
                }else{
                    //started
                    console.append("Running: " + selected_cmd.getStop_cmd() + '\n');
                    if(debug) Log.d("CustomCMDFragment", "Running: " + selected_cmd.getStop_cmd());
                    selected_cmd.stop();
                    stop.obtainMessage().sendToTarget();
                }
            }
        });

        return v;
    }
    @Override
    public void onResume(){
        super.onResume();
        console.setText(console_text);
        currentFragment = FRAGMENT_CUSTOM;
    }
    @Override
    public void onPause(){
        super.onPause();
        console_text = console.getText().toString();
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
            console.append((String)msg.obj + '\n');
        }
    };
}
