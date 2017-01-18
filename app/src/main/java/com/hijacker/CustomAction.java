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

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.hijacker.MainActivity.aireplay_dir;
import static com.hijacker.MainActivity.airodump_dir;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.getPIDs;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.mdk3_dir;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.reaver_dir;

class CustomAction{
    static final int TYPE_AP=0, TYPE_ST=1;
    static List<CustomAction> cmds = new ArrayList<>();
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
    String getStart_cmd(){ return start_cmd; }
    String getStop_cmd(){ return stop_cmd; }
    String getProcess_name(){ return process_name; }
    boolean requires_clients(){ return requires_clients; }
    boolean requires_connected(){ return requires_connected; }
    boolean hasProcessName(){ return has_process_name; }
    int getType(){ return type; }
    void setTitle(String title){ this.title = title; }
    void setStart_cmd(String start_cmd){ this.start_cmd = start_cmd; }
    void setStop_cmd(String stop_cmd){ this.stop_cmd = stop_cmd; }
    void setRequires_clients(boolean requires_clients){ this.requires_clients = requires_clients; }
    void setRequires_connected(boolean requires_connected){ this.requires_connected = requires_connected; }
    void run(){
        Shell shell = CustomActionFragment.shell;
        shell.run("export IFACE=\"" + iface + '\"');
        shell.run("export PREFIX=\"" + prefix + '\"');
        shell.run("export AIRODUMP_DIR=\"" + airodump_dir + '\"');
        shell.run("export AIREPLAY_DIR=\"" + aireplay_dir + '\"');
        shell.run("export MDK3_DIR=\"" + mdk3_dir + '\"');
        shell.run("export REAVER_DIR=\"" + reaver_dir + '\"');
        if(type==TYPE_AP){
            shell.run("export MAC=\"" + CustomActionFragment.ap.mac + '\"');
            shell.run("export ESSID=\"" + CustomActionFragment.ap.essid + '\"');
            shell.run("export ENC=\"" + CustomActionFragment.ap.enc + '\"');
            shell.run("export CIPHER=\"" + CustomActionFragment.ap.cipher + '\"');
            shell.run("export AUTH=\"" + CustomActionFragment.ap.auth + '\"');
            shell.run("export CH=\"" + CustomActionFragment.ap.ch + '\"');
        }else{
            shell.run("export MAC=\"" + CustomActionFragment.st.mac + '\"');
            shell.run("export BSSID=\"" + CustomActionFragment.st.bssid + '\"');
        }
        shell.run(start_cmd);
        shell.run("echo ENDOFCUSTOM");
        CustomActionFragment.thread = new Thread(CustomActionFragment.runnable);
        CustomActionFragment.thread.start();
    }
    void stop(){
        Shell shell = Shell.getFreeShell();     //Can't use the CustomActionFragment.shell as it is used by the action
        shell.run(stop_cmd);
        if(has_process_name){
            ArrayList<Integer> list = getPIDs(process_name);
            for(int i=0;i<list.size();i++){
                shell.run("busybox kill " + list.get(i));
            }
            try{
                Thread.sleep(100);  //Make sure that the processes have been killed
            }catch(InterruptedException ignored){
            }finally{
                shell.done();
            }
        }
        shell.done();
    }
    static void save(){
        //Save current cmds list to permanent storage
        File file;
        FileWriter writer;
        String folder = Environment.getExternalStorageDirectory() + "/Hijacker-actions/";
        CustomAction action;
        for(int i=0;i<cmds.size();i++){
            action = cmds.get(i);
            file = new File(folder + action.getTitle() + ".action");
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
        File folder = new File(Environment.getExternalStorageDirectory() + "/Hijacker-actions");
        if(!folder.exists()){
            folder.mkdir();
        }
        File actions[] = folder.listFiles();
        if(actions!=null){
            if(debug) Log.d("HIJACKER/CustomAction", "Reading files...");
            FileReader reader0;
            BufferedReader reader;
            for(File file : actions){
                try{
                    reader0 = new FileReader(file);
                    reader = new BufferedReader(reader0);
                    String title = reader.readLine();
                    String start_cmd = reader.readLine();
                    String stop_cmd = reader.readLine();
                    int type = Integer.parseInt(reader.readLine());
                    boolean requirement = Boolean.parseBoolean(reader.readLine());
                    String process_name = reader.readLine();
                    if(process_name==null) process_name = "";   //For files that were created before process_name was implemented
                    if(debug) Log.d("HIJACKER/CustomAction", "Read from file " + file.getName() + ": " + title + ", " +
                            start_cmd + ", " + stop_cmd + ", " + Integer.toString(type) + ", " + Boolean.toString(requirement) + ", " + process_name);
                    CustomAction action = new CustomAction(title, start_cmd, stop_cmd, process_name, type);
                    if(type==TYPE_AP){
                        action.setRequires_clients(requirement);
                    }else{
                        action.setRequires_connected(requirement);
                    }
                    reader.close();
                    reader0.close();
                }catch(IOException e){
                    Log.e("HIJACKER/CustomAction", "In load(): " + e.toString());
                }
            }
        }
    }
}
