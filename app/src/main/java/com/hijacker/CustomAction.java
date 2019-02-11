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

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.hijacker.MainActivity.actions_path;
import static com.hijacker.MainActivity.aireplay_dir;
import static com.hijacker.MainActivity.airodump_dir;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.mdk3bf_dir;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.reaver_dir;

class CustomAction{
    static final int TYPE_AP=0, TYPE_ST=1;
    static final List<CustomAction> cmds = new ArrayList<>();
    private String title, start_cmd, stop_cmd, process_name;
    private int type;
    private boolean requires_clients=false, requires_connected=false, has_process_name=false;
    CustomAction(String title, String start_cmd, String stop_cmd, String process_name, int type){
        this.title = title;
        this.start_cmd = start_cmd;
        this.stop_cmd = stop_cmd;
        this.process_name = process_name;
        if(!process_name.equals("")) has_process_name = true;
        this.type = type;
        cmds.add(this);
    }

    String getTitle(){ return title; }
    String getStartCmd(){ return start_cmd; }
    String getStopCmd(){ return stop_cmd; }
    String getProcessName(){ return process_name; }
    boolean requiresClients(){ return requires_clients; }
    boolean requiresConnected(){ return requires_connected; }
    boolean hasProcessName(){ return has_process_name; }
    boolean hasStopCmd(){ return !stop_cmd.equals(""); }
    int getType(){ return type; }
    void setTitle(String title){ this.title = title; }
    void setStartCmd(String start_cmd){ this.start_cmd = start_cmd; }
    void setStopCmd(String stop_cmd){ this.stop_cmd = stop_cmd; }
    void setRequiresClients(boolean requires_clients){ this.requires_clients = requires_clients; }
    void setRequiresConnected(boolean requires_connected){ this.requires_connected = requires_connected; }

    void run(Shell shell, @NonNull Device dev){
        shell.run("export IFACE=\"" + iface + '\"');
        shell.run("export PREFIX=\"" + prefix + '\"');
        shell.run("export AIRODUMP_DIR=\"" + airodump_dir + '\"');
        shell.run("export AIREPLAY_DIR=\"" + aireplay_dir + '\"');
        shell.run("export MDK3_DIR=\"" + mdk3bf_dir + '\"');
        shell.run("export REAVER_DIR=\"" + reaver_dir + '\"');
        if(type==TYPE_AP){
            AP ap = (AP)dev;
            shell.run("export MAC=\"" + ap.mac + '\"');
            shell.run("export ESSID=\"" + ap.essid + '\"');
            shell.run("export ENC=\"" + ap.enc + '\"');
            shell.run("export CIPHER=\"" + ap.cipher + '\"');
            shell.run("export AUTH=\"" + ap.auth + '\"');
            shell.run("export CH=\"" + ap.ch + '\"');
        }else{
            ST st = (ST)dev;
            shell.run("export MAC=\"" + st.mac + '\"');
            shell.run("export BSSID=\"" + st.bssid + '\"');
        }
        shell.run(start_cmd);
        shell.run("echo ENDOFCUSTOM");
    }
    static void save(){
        //Save current cmds list to permanent storage
        File file;
        FileWriter writer;
        CustomAction action;
        for(int i=0;i<cmds.size();i++){
            action = cmds.get(i);
            file = new File(actions_path + "/" + action.getTitle() + ".action");
            try{
                writer = new FileWriter(file);
                writer.write(action.title + '\n');
                writer.write(action.start_cmd + '\n');
                writer.write(action.stop_cmd + '\n');
                writer.write(Integer.toString(action.type) + '\n');
                writer.write(Boolean.toString(action.requires_clients || action.requires_connected) + '\n');
                writer.write(action.process_name + '\n');
                writer.close();
            }catch(IOException e){
                Log.e("HIJACKER/CustomAction", "In save(): " + e.toString());
                ErrorDialog dialog = new ErrorDialog();
                dialog.setMessage("Error while saving " + action.title);
                dialog.show(mFragmentManager, "ErrorDialog");
            }
        }
    }
    static void load(){
        //load custom actions from storage to cmds
        cmds.clear();
        File folder = new File(actions_path);
        if(!folder.exists()){
            folder.mkdir();
        }
        File actions[] = folder.listFiles();
        if(actions!=null){
            if(debug) Log.d("HIJACKER/CustomAction", "Reading custom actions...");
            for(File file : actions){
                try{
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String title = reader.readLine();
                    String start_cmd = reader.readLine();
                    String stop_cmd = reader.readLine();
                    int type = Integer.parseInt(reader.readLine());
                    boolean requirement = Boolean.parseBoolean(reader.readLine());
                    String process_name = reader.readLine();
                    if(process_name==null) process_name = "";   //For files that were created before process_name was implemented
                    CustomAction action = new CustomAction(title, start_cmd, stop_cmd, process_name, type);
                    if(type==TYPE_AP){
                        action.setRequiresClients(requirement);
                    }else{
                        action.setRequiresConnected(requirement);
                    }
                    reader.close();
                }catch(Exception e){
                    Log.e("HIJACKER/CustomAction", "In load(): " + e.toString());
                }
            }
        }
    }
}
