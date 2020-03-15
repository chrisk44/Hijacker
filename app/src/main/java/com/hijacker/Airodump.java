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

import android.os.FileObserver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.hijacker.AP.getAPByMac;
import static com.hijacker.MainActivity.BAND_2;
import static com.hijacker.MainActivity.BAND_5;
import static com.hijacker.MainActivity.BAND_BOTH;
import static com.hijacker.MainActivity.MAX_READLINE_SIZE;
import static com.hijacker.MainActivity.airodump_dir;
import static com.hijacker.MainActivity.always_cap;
import static com.hijacker.MainActivity.band;
import static com.hijacker.MainActivity.busybox;
import static com.hijacker.MainActivity.cap_path;
import static com.hijacker.MainActivity.cap_tmp_path;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.enable_monMode;
import static com.hijacker.MainActivity.enable_on_airodump;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.last_action;
import static com.hijacker.MainActivity.last_airodump;
import static com.hijacker.MainActivity.notification;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.refreshState;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.menu;
import static com.hijacker.MainActivity.stopWPA;
import static com.hijacker.ST.getSTByMac;
import static com.hijacker.Shell.getFreeShell;
import static com.hijacker.Shell.runOne;

class Airodump{
    static final String TAG = "HIJACKER/Airodump";
    private static int channel = 0;
    private static boolean forWPA = false, forWEP = false, running = false;
    private static String mac = null;
    private static String capFile = null;
    static CapFileObserver capFileObserver = null;

    static void reset(){
        stop();
        channel = 0;
        forWPA = false;
        forWEP = false;
        mac = null;
        capFile = null;
    }
    static void setChannel(int ch){
        if(isRunning()){
            Log.e(TAG, "Can't change settings while airodump is running");
            throw new IllegalStateException("Airodump is still running");
        }
        channel = ch;
    }
    static void setMac(String new_mac){
        if(isRunning()){
            Log.e(TAG, "Can't change settings while airodump is running");
            throw new IllegalStateException("Airodump is still running");
        }
        mac = new_mac;
    }
    static void setForWPA(boolean bool){
        if(isRunning()){
            Log.e(TAG, "Can't change settings while airodump is running");
            throw new IllegalStateException("Airodump is still running");
        }
        if(forWEP){
            Log.e(TAG, "Can't set forWPA when forWEP is enabled");
            throw new IllegalStateException("Tried to set forWPA when forWEP is enabled");
        }
        forWPA = bool;
    }
    static void setForWEP(boolean bool){
        if(isRunning()){
            Log.e(TAG, "Can't change setting while airodump is running");
            throw new IllegalStateException("Airodump is still running");
        }
        if(forWPA){
            Log.e(TAG, "Can't set forWEP when forWPA is enabled");
            throw new IllegalStateException("Tried to set forWEP when forWPA is enabled");
        }
        forWEP = bool;
    }
    static void setAP(AP ap){
        if(isRunning()){
            Log.e(TAG, "Can't change setting while airodump is running");
            throw new IllegalStateException("Airodump is still running");
        }
        mac = ap.mac;
        channel = ap.ch;
    }
    static int getChannel(){ return channel; }
    static String getMac(){ return mac; }
    static String getCapFile(){
        while(!capFileObserver.found_cap_file() && writingToFile()){}
        return capFile;
    }
    static boolean writingToFile(){ return (forWEP || forWPA || always_cap) && isRunning(); }
    static void startClean(){
        reset();
        start();
    }
    static void startClean(AP ap){
        reset();
        setAP(ap);
        start();
    }
    static void startClean(int ch){
        reset();
        setChannel(ch);
        start();
    }
    static void start(){
        // Construct the command
        String cmd = "su -c " + prefix + " " + airodump_dir + " --update 9999999 --write-interval 1 --band ";

        if(band==BAND_5 || band==BAND_BOTH || channel>20) cmd += "a";
        if((band==BAND_2 || band==BAND_BOTH) && channel<=20) cmd += "bg";

        cmd += " -w " + cap_tmp_path;

        if(forWPA) cmd += "/handshake --output-format pcap,csv ";
        else if(forWEP) cmd += "/wep_ivs  --output-format pcap,csv ";
        else if(always_cap) cmd += "/cap  --output-format pcap,csv ";
        else cmd += "/cap  --output-format csv ";

        // If we are starting for WEP capture, capture only IVs
        if(forWEP) cmd += "--ivs ";

        // If we have a valid channel, select it (airodump does not recognize 5ghz channels here)
        if(channel>0 && channel<20) cmd += "--channel " + channel + " ";

        // If we have a specific MAC, listen for it
        if(mac!=null) cmd += "--bssid " + mac + " ";

        cmd += iface;

        // Enable monitor mode
        if(enable_on_airodump) runOne(enable_monMode);

        // Stop any airodump instances
        stop();

        capFile = null;
        running = true;
        capFileObserver.startWatching();

        if(debug) Log.d("HIJACKER/Airodump.start", cmd);
        try{
            Runtime.getRuntime().exec(cmd);
            last_action = System.currentTimeMillis();
            last_airodump = cmd;
        }catch(IOException e){
            e.printStackTrace();
            Log.e("HIJACKER/Exception", "Caught Exception in Airodump.start() read thread: " + e.toString());
        }

        runInHandler(new Runnable(){
            @Override
            public void run(){
                if(menu!=null){
                    menu.getItem(1).setIcon(R.drawable.stop_drawable);
                    menu.getItem(1).setTitle(R.string.stop);
                }
                refreshState();
                notification();
            }
        });
    }
    static void stop(){
        last_action = System.currentTimeMillis();
        running = false;
        capFileObserver.stopWatching();
        runInHandler(new Runnable(){
            @Override
            public void run(){
                if(menu!=null){
                    menu.getItem(1).setIcon(R.drawable.start_drawable);
                    menu.getItem(1).setTitle(R.string.start);
                }
            }
        });
        stopWPA();
        runOne(busybox + " kill $(" + busybox + " pidof airodump-ng)");
        AP.saveAll();
        ST.saveAll();

        runInHandler(new Runnable(){
            @Override
            public void run(){
                refreshState();
                notification();
            }
        });
    }
    static boolean isRunning(){
        return running;
    }
    public static void addAP(String essid, String mac, String enc, String cipher, String auth,
                             int pwr, int beacons, int data, int ivs, int ch){
        AP temp = getAPByMac(mac);

        if(temp==null) new AP(essid, mac, enc, cipher, auth, pwr, beacons, data, ivs, ch);
        else temp.update(essid, enc, cipher, auth, pwr, beacons, data, ivs, ch);
    }
    public static void addST(String mac, String bssid, String probes, int pwr, int lost, int frames){
        ST temp = getSTByMac(mac);

        if (temp == null) new ST(mac, bssid, pwr, lost, frames, probes);
        else temp.update(bssid, pwr, lost, frames, probes);
    }
    static void analyzeAirodumpString(String buffer, int mode){
        int i, j;

        // Remove trailing spaces
        while(buffer.endsWith(" "))
            buffer = buffer.substring(0, buffer.length()-1);

        if(buffer.length()<3) return;
        if( buffer.charAt(3)==':' || buffer.charAt(3)=='o' ){
            //logd("Found ':' or 'o' @ 3");
            while(buffer.charAt(buffer.length()-1)=='\n'){
                buffer = buffer.substring(0, buffer.length()-1);
            }

            //Clear spaces
            for(i=123; i<buffer.length(); i++){
                if(buffer.charAt(i)==' ' && buffer.charAt(i+1)==' '){
                    for(j=i;j<buffer.length();j++){
                        buffer = buffer.substring(0, 123) + buffer.substring(124, buffer.length());
                    }
                    i--;
                }
            }
            if(buffer.charAt(22)==':'){
                //logd("0         1         2         3         4         5         6");
                //logd("0123456789012345678901234567890123456789012345678901234567890");
                //logd(buffer);
                //st
                String st_mac, bssid, probes;
                int pwr, lost, frames;

                st_mac = buffer.substring(20, 37);

                if(buffer.charAt(1)=='(') bssid = "na";
                else bssid = buffer.substring(1, 18);

                pwr = Integer.parseInt(buffer.substring(37, 43).replace(" ", ""));
                lost = Integer.parseInt(buffer.substring(52, 58).replace(" ", ""));
                frames = Integer.parseInt(buffer.substring(58, 67).replace(" ", ""));
                if(buffer.length()>=69) probes = buffer.substring(69);
                else probes = "";

                addST(st_mac, bssid, probes, pwr, lost, frames);
            }else{
                //ap
                String bssid, enc, cipher, auth, essid;
                int pwr, beacons, data, ivs, ch;

                bssid = buffer.substring(1, 17);

                pwr = Integer.parseInt(buffer.substring(18, 23).replace(" ", ""));

                //if mode is not 0 then airodump-ng is running for a specific channel
                //so we need to bypass 4 characters after pwr to get the correct results because there is one extra column
                int offset = mode==0 ? 0 : 4;
                buffer = buffer.substring(offset);

                beacons = Integer.parseInt(buffer.substring(23, 32).replace(" ", ""));
                data = Integer.parseInt(buffer.substring(32, 41).replace(" ", ""));
                ivs = Integer.parseInt(buffer.substring(41, 46).replace(" ", ""));
                ch = Integer.parseInt(buffer.substring(48, 50).replace(" ", ""));
                enc = buffer.substring(57, 61).replace(" ", "");
                cipher = buffer.substring(62, 66);
                auth = buffer.substring(69, 73).replace(" ", "");

                if(buffer.charAt(74)!='<') essid = buffer.substring(74);
                else essid = "<hidden>";

                addAP(essid, bssid, enc, cipher, auth, pwr, beacons, data, ivs, ch);
            }
        }
    }

    static class CapFileObserver extends FileObserver{
        static String TAG = "HIJACKER/CapFileObs";
        String master_path;
        Shell shell = null;
        boolean found_cap_file = false;
        public CapFileObserver(String path, int mask) {
            super(path, mask);
            master_path = path;
        }
        @Override
        public void onEvent(int event, @Nullable String path){
            if(path==null){
                Log.e(TAG, "Received event " + event + " for null path");
                return;
            }
            boolean isPcap = path.endsWith(".pcap");

            switch(event){
                case FileObserver.CREATE:
                    // Airodump started, pcap or csv file was just created

                    if(isPcap){
                        capFile = master_path + '/' + path;
                        found_cap_file = true;
                    }
                    break;

                case FileObserver.MODIFY:
                    // Airodump just updated pcap or csv
                    if(!isPcap){
                        readCsv(master_path + '/' + path, shell);
                    }
                    break;

                default:
                    // Unknown event received (should never happen)
                    Log.e(TAG, "Unknown event received: " + event);
                    Log.e(TAG, "for file " + path);
                    break;
            }
        }
        @Override
        public void startWatching(){
            super.startWatching();
            shell = getFreeShell();

            found_cap_file = false;
        }
        @Override
        public void stopWatching(){
            super.stopWatching();

            if(shell!=null) {
                if (writingToFile()) {
                    shell.run(busybox + " mv " + capFile + " " + cap_path + '/');
                }
                shell.run(busybox + " rm " + cap_tmp_path + "/*");
                shell.done();
                shell = null;
            }
        }
        boolean found_cap_file(){
            return found_cap_file;
        }
        void readCsv(String csv_path, @NonNull Shell shell){
            shell.clearOutput();
            shell.run(busybox + " cat " + csv_path + "; echo ENDOFCAT");
            BufferedReader out = shell.getShell_out();
            try {

                int type = 0;           // 0 = AP, 1 = ST
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                while(true){
                    String line = out.readLine();
                    Log.d(TAG, line);
                    if(line.equals("ENDOFCAT"))
                        break;

                    if(line.equals(""))
                        continue;
                    if(line.startsWith("BSSID")) {
                        type = 0;
                        continue;
                    }else if(line.startsWith("Station")) {
                        type = 1;
                        continue;
                    }

                    line = line.replace(", ", ",");
                    String[] fields = line.split(",");
                    Log.i(TAG, line);
                    if(type == 0){
                        // Parse AP
                        // BSSID, First time seen, Last time seen, channel, Speed, Privacy,Cipher,
                        // Authentication, Power, # beacons, # IVs (or data??), LAN IP, ID-length, ESSID, Key

                        String bssid = fields[0];
                        try {
                            Date first_seen = sdf.parse(fields[1]);
                            Date last_seen = sdf.parse(fields[2]);
                        }catch(ParseException e){
                            e.printStackTrace();
                            Log.e(TAG, e.toString());
                        }
                        int ch = Integer.parseInt(fields[3].replace(" ", ""));
                        int speed = Integer.parseInt(fields[4].replace(" ", ""));
                        String enc = fields[5];
                        String cipher = fields[6];
                        String auth = fields[7];
                        int pwr = Integer.parseInt(fields[8].replace(" ", ""));
                        int beacons = Integer.parseInt(fields[9].replace(" ", ""));
                        int data = Integer.parseInt(fields[10].replace(" ", ""));
                        String lan_ip = fields[11].replace(" ", "");
                        int id_length = Integer.parseInt(fields[12].replace(" ", ""));
                        String essid = id_length > 0 ? fields[13] : null;

                        String key = fields.length>14 ? fields[14] : null;

                        addAP(essid, bssid, enc, cipher, auth, pwr, beacons, data, 0, ch);
                    }else{
                        // Parse ST
                        //Station MAC, First time seen, Last time seen, Power, # packets, BSSID, Probed ESSIDs

                        String mac = fields[0];
                        try {
                            Date first_seen = sdf.parse(fields[1]);
                            Date last_seen = sdf.parse(fields[2]);
                        }catch(ParseException e){
                            e.printStackTrace();
                            Log.e(TAG, e.toString());
                        }
                        int pwr = Integer.parseInt(fields[3].replace(" ", ""));
                        int packets = Integer.parseInt(fields[4].replace(" ", ""));
                        String bssid = fields[5];
                        if(bssid.charAt(0)=='(') bssid = null;

                        String probes = "";
                        if(fields.length==7) {
                            probes = fields[6];
                        }else if(fields.length>7){
                            // Multiple probes are separated by comma, so concatenate them
                            probes = "";
                            for(int i=6; i<fields.length; i++){
                                probes += fields[i] + ", ";
                            }
                            probes = probes.substring(0, probes.length()-2);
                        }

                        addST(mac, bssid, probes, pwr, 0, packets);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        }
    }
}
