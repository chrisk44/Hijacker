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

public class CustomCMD{
    static final int TYPE_AP=0, TYPE_ST=1;
    private String title, start_cmd, stop_cmd;
    private int type;
    private boolean running=false;
    CustomCMD(String title, String start_cmd, String stop_cmd, int type){
        this.title = title;
        this.start_cmd = start_cmd + "; echo ENDOFCUSTOM";
        this.stop_cmd = stop_cmd;
        this.type = type;
    }

    String getTitle(){ return title; }
    String getStart_cmd(){ return start_cmd; }
    String getStop_cmd(){ return stop_cmd; }
    int getType(){ return type; }
    boolean isRunning(){ return running; }
    void setRunning(boolean running){ this.running = running; }
    void run(){
        Shell shell = CustomCMDFragment.shell;
        if(type==TYPE_AP){
            shell.run("export MAC=" + CustomCMDFragment.ap.mac);
            shell.run("export ESSID=" + CustomCMDFragment.ap.essid);
            shell.run("export ENC=" + CustomCMDFragment.ap.enc);
            shell.run("export CIPHER=" + CustomCMDFragment.ap.cipher);
            shell.run("export AUTH=" + CustomCMDFragment.ap.auth);
            shell.run("export CH=" + CustomCMDFragment.ap.ch);
        }else{
            shell.run("export MAC=" + CustomCMDFragment.st.mac);
            shell.run("export BSSID=" + CustomCMDFragment.st.bssid);
        }
        shell.run(start_cmd);
        CustomCMDFragment.thread.start();
    }
    void stop(){
        CustomCMDFragment.shell.run(stop_cmd);
    }
}
