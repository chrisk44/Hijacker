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

import static com.hijacker.MainActivity.PROCESS_AIREPLAY;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.PROCESS_MDK;
import static com.hijacker.MainActivity.PROCESS_REAVER;
import static com.hijacker.MainActivity.aireplay_dir;
import static com.hijacker.MainActivity.airodump_dir;
import static com.hijacker.MainActivity.disable_monMode;
import static com.hijacker.MainActivity.enable_monMode;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.mdk3_dir;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.reaver_dir;

public class CustomCMD{
    static final int TYPE_AP=0, TYPE_ST=1;
    private String title, start_cmd, stop_cmd;
    private int type;
    private boolean kill_airodump=false, kill_aireplay=false, kill_mdk=false, kill_reaver=false,
            enable_mm=false, disable_mm=false, requires_clients=false, requires_connected=false;
    CustomCMD(String title, String start_cmd, String stop_cmd, int type){
        this.title = title;
        this.start_cmd = start_cmd;
        this.stop_cmd = stop_cmd;
        this.type = type;
    }

    String getTitle(){ return title; }
    String getStart_cmd(){ return start_cmd; }
    String getStop_cmd(){ return stop_cmd; }
    boolean requires_clients(){ return requires_clients; }
    boolean requires_connected(){ return requires_connected; }
    int getType(){ return type; }
    void run(){
        if(kill_airodump) MainActivity.stop(PROCESS_AIRODUMP);
        if(kill_aireplay) MainActivity.stop(PROCESS_AIREPLAY);
        if(kill_mdk) MainActivity.stop(PROCESS_MDK);
        if(kill_reaver) MainActivity.stop(PROCESS_REAVER);
        Shell shell = CustomCMDFragment.shell;
        if(enable_mm) shell.run(enable_monMode);
        shell.run("export IFACE=\"" + iface + '\"');
        shell.run("export PREFIX=\"" + prefix + '\"');
        shell.run("export AIRODUMP_DIR=\"" + airodump_dir + '\"');
        shell.run("export AIREPLAY_DIR=\"" + aireplay_dir + '\"');
        shell.run("export MDK3_DIR=\"" + mdk3_dir + '\"');
        shell.run("export REAVER_DIR=\"" + reaver_dir + '\"');
        if(type==TYPE_AP){
            shell.run("export MAC=\"" + CustomCMDFragment.ap.mac + '\"');
            shell.run("export ESSID=\"" + CustomCMDFragment.ap.essid + '\"');
            shell.run("export ENC=\"" + CustomCMDFragment.ap.enc + '\"');
            shell.run("export CIPHER=\"" + CustomCMDFragment.ap.cipher + '\"');
            shell.run("export AUTH=\"" + CustomCMDFragment.ap.auth + '\"');
            shell.run("export CH=\"" + CustomCMDFragment.ap.ch + '\"');
        }else{
            shell.run("export MAC=\"" + CustomCMDFragment.st.mac + '\"');
            shell.run("export BSSID=\"" + CustomCMDFragment.st.bssid + '\"');
        }
        shell.run(start_cmd);
        shell.run("echo ENDOFCUSTOM");
        CustomCMDFragment.thread.start();
    }
    void stop(){
        Shell shell = CustomCMDFragment.shell;
        shell.run(stop_cmd);
        if(disable_mm) shell.run(disable_monMode);
    }
}
