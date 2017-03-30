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

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupMenu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static com.hijacker.AP.WEP;
import static com.hijacker.AP.WPA;
import static com.hijacker.AP.WPA2;
import static com.hijacker.CustomAction.TYPE_AP;
import static com.hijacker.MainActivity.FRAGMENT_AIRODUMP;
import static com.hijacker.MainActivity.MDK_ADOS;
import static com.hijacker.MainActivity.aireplay_dir;
import static com.hijacker.MainActivity.airodump_dir;
import static com.hijacker.MainActivity.aliases;
import static com.hijacker.MainActivity.aliases_file;
import static com.hijacker.MainActivity.aliases_in;
import static com.hijacker.MainActivity.cap_dir;
import static com.hijacker.MainActivity.copy;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.data_path;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.refreshDrawer;
import static com.hijacker.MainActivity.startMdk;

public class MyListFragment extends ListFragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.list_fragment, container, false);
        setListAdapter(MainActivity.adapter);
        return ret;
    }
    @Override
    public void onListItemClick(ListView l, final View v, int position, long id){
        super.onListItemClick(l, v, position, id);
        final Tile clicked = Tile.tiles.get(position);
        if(clicked.getType()==TYPE_AP){
            //AP
            PopupMenu popup = new PopupMenu(getActivity(), v);
            popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

            //add(groupId, itemId, order, title)
            popup.getMenu().add(0, 0, 0, "Info");
            popup.getMenu().add(0, 1, 1, clicked.ap.isMarked ? "Unmark" : "Mark");
            popup.getMenu().add(0, 2, 2, "Copy MAC");
            popup.getMenu().add(0, 3, 3, "Watch");
            popup.getMenu().add(0, 5, 4, "Set alias");
            popup.getMenu().add(0, 4, 5, "Attack...");

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(android.view.MenuItem item) {
                    if(debug) Log.d("HIJACKER/MyListFragment", "Clicked " + item.getItemId() + " for ap");
                    switch(item.getItemId()) {
                        case 0:
                            //Info
                            clicked.ap.showInfo(mFragmentManager);
                            break;
                        case 1:
                            //mark or unmark
                            if(clicked.ap.isMarked){
                                clicked.ap.unmark();
                            }else{
                                clicked.ap.mark();
                            }
                            break;
                        case 2:
                            //copy mac to clipboard
                            copy(clicked.ap.mac, v);
                            break;
                        case 3:
                            //Watch
                            MainActivity.isolate(clicked.ap.mac);
                            Airodump.startClean(clicked.ap);
                            break;
                        case 4:
                            //attack
                            PopupMenu popup2 = new PopupMenu(getActivity(), v);
                            popup2.getMenuInflater().inflate(R.menu.popup_menu, popup2.getMenu());

                            //add(groupId, itemId, order, title)
                            popup2.getMenu().add(0, 0, 0, "Disconnect...");
                            if(clicked.ap.clients.size()>0) popup2.getMenu().add(0, 1, 1, "Disconnect Client");
                            popup2.getMenu().add(0, 2, 2, "Copy disconnect command");

                            popup2.getMenu().add(0, 3, 3, "DoS");
                            if(clicked.ap.sec==WPA || clicked.ap.sec==WPA2 || clicked.ap.sec==WEP){
                                popup2.getMenu().add(0, 4, 4, "Crack");
                                popup2.getMenu().add(0, 5, 5, "Copy crack command");
                            }
                            popup2.getMenu().add(0, 6, 6, "Crack with Reaver");

                            popup2.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
                                @Override
                                public boolean onMenuItemClick(MenuItem item){
                                    switch(item.getItemId()){
                                        case 0:
                                            //Disconnect
                                            clicked.ap.disconnectAll();
                                            break;
                                        case 1:
                                            //Disconnect client
                                            PopupMenu popup = new PopupMenu(getActivity(), v);

                                            popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
                                            for (int i = 0; i < clicked.ap.clients.size(); i++) {
                                                popup.getMenu().add(0, i, i, clicked.ap.clients.get(i).toString());
                                            }
                                            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                                public boolean onMenuItemClick(android.view.MenuItem item) {
                                                    //ItemId = i (in for())
                                                    clicked.ap.clients.get(item.getItemId()).disconnect();
                                                    return true;
                                                }
                                            });
                                            popup.show();
                                            break;
                                        case 2:
                                            //Copy disconnect command
                                            String str2 = prefix + " " + aireplay_dir + " --deauth 0 -a " + clicked.ap.mac + " " + iface;
                                            copy(str2, v);
                                            break;
                                        case 3:
                                            //DoS
                                            startMdk(MDK_ADOS, clicked.ap.mac);
                                            break;
                                        case 4:
                                            //Crack
                                            clicked.ap.crack();
                                            break;
                                        case 5:
                                            //copy crack command
                                            String str;
                                            if(clicked.ap.sec==WEP) str = prefix + " " + airodump_dir + " --channel " + clicked.ap.ch + " --bssid " + clicked.ap.mac + " --ivs -w " + cap_dir + "/wep_ivs " + iface;
                                            else str = prefix + " " + airodump_dir + " --channel " + clicked.ap.ch + " --bssid " + clicked.ap.mac + " -w " + cap_dir + "/handshake " + iface;

                                            copy(str, v);
                                            break;
                                        case 6:
                                            //crack with reaver
                                            clicked.ap.crackReaver(mFragmentManager);
                                            break;
                                    }
                                    return false;
                                }
                            });
                            popup2.show();
                            break;
                        case 5:
                            //Set alias
                            final EditTextDialog dialog = new EditTextDialog();
                            dialog.setTitle(getString(R.string.set_alias));
                            dialog.setDefaultText(clicked.ap.alias);
                            dialog.setAllowEmpty(true);
                            dialog.setRunnable(new Runnable(){
                                @Override
                                public void run(){
                                    if(dialog.result.equals("")) dialog.result = null;
                                    try{
                                        if(clicked.ap.alias==null ^ dialog.result==null){
                                            //Need to remove previous alias
                                            File temp_aliases = new File(data_path + "/temp_aliases");
                                            if(temp_aliases.exists()) temp_aliases.delete();
                                            temp_aliases.createNewFile();
                                            PrintWriter temp_in = new PrintWriter(new FileWriter(temp_aliases));

                                            BufferedReader aliases_out = new BufferedReader(new FileReader(aliases_file));

                                            //Copy current aliases to temp file, except the one we are changing
                                            String buffer = aliases_out.readLine();
                                            while(buffer!=null){
                                                //Line format: 00:11:22:33:44:55 Alias
                                                if(buffer.charAt(17)==' ' && buffer.length()>18){
                                                    String mac = buffer.substring(0, 17);
                                                    String alias = buffer.substring(18);
                                                    if(!mac.equals(clicked.ap.mac)){
                                                        temp_in.println(mac + ' ' + alias);
                                                    }
                                                }else{
                                                    Log.e("HIJACKER/setup", "Aliases file format error: " + buffer);
                                                }
                                                buffer = aliases_out.readLine();
                                            }
                                            temp_in.flush();
                                            temp_in.close();
                                            aliases_out.close();

                                            aliases_file.delete();
                                            temp_aliases.renameTo(aliases_file);
                                            aliases_in = new FileWriter(aliases_file, true);
                                        }
                                        if(dialog.result!=null){
                                            aliases_in.write(clicked.ap.mac + ' ' + dialog.result + '\n');
                                            aliases_in.flush();
                                        }
                                    }catch(IOException e){
                                        Log.e("HIJACKER/MyListFrgm", e.toString());
                                    }
                                    aliases.put(clicked.ap.mac, dialog.result);
                                    clicked.ap.alias = dialog.result;
                                    clicked.ap.update();
                                }
                            });
                            dialog.show(getFragmentManager(), "EditTextDialog");
                    }
                    return true;
                }
            });
            popup.show();
        }else{
            //ST
            PopupMenu popup = new PopupMenu(getActivity(), v);
            popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

            popup.getMenu().add(0, 0, 0, "Info");
            popup.getMenu().add(0, 4, 1, clicked.st.isMarked ? "Unmark" : "Mark");
            popup.getMenu().add(0, 1, 2, "Copy MAC");
            popup.getMenu().add(0, 5, 3, "Set alias");
            if(clicked.st.bssid!=null){
                popup.getMenu().add(0, 2, 4, "Disconnect");
                popup.getMenu().add(0, 3, 5, "Copy disconnect command");
            }

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(android.view.MenuItem item) {
                    if(debug) Log.d("HIJACKER/MyListFragment", "Clicked " + item.getItemId() + " for st");
                    switch(item.getItemId()) {
                        case 0:
                            //Info
                            clicked.st.showInfo(mFragmentManager);
                            break;
                        case 1:
                            //copy to clipboard
                            copy(clicked.st.mac, v);
                            break;
                        case 2:
                            //Disconnect this
                            clicked.st.disconnect();
                            break;
                        case 3:
                            //copy disconnect command to clipboard
                            String str = prefix + " " + aireplay_dir + " --ignore-negative-one --deauth 0 -a " + clicked.st.bssid + " -c " + clicked.st.mac + " " + iface;
                            copy(str, v);
                            break;
                        case 4:
                            //mark or unmark
                            if(clicked.st.isMarked){
                                clicked.st.unmark();
                            }else{
                                clicked.st.mark();
                            }
                            break;
                        case 5:
                            //Set alias
                            final EditTextDialog dialog = new EditTextDialog();
                            dialog.setTitle(getString(R.string.set_alias));
                            dialog.setAllowEmpty(false);
                            dialog.setDefaultText(clicked.st.alias);
                            dialog.setRunnable(new Runnable(){
                                @Override
                                public void run(){
                                    if(dialog.result.equals("")) dialog.result = null;
                                    try{
                                        if(clicked.st.alias==null ^ dialog.result==null){
                                            //Need to remove previous alias
                                            File temp_aliases = new File(data_path + "/temp_aliases");
                                            if(temp_aliases.exists()) temp_aliases.delete();
                                            temp_aliases.createNewFile();
                                            PrintWriter temp_in = new PrintWriter(new FileWriter(temp_aliases));

                                            BufferedReader aliases_out = new BufferedReader(new FileReader(aliases_file));

                                            //Copy current aliases to temp file, except the one we are changing
                                            String buffer = aliases_out.readLine();
                                            while(buffer!=null){
                                                //Line format: 00:11:22:33:44:55 Alias
                                                if(buffer.charAt(17)==' ' && buffer.length()>18){
                                                    String mac = buffer.substring(0, 17);
                                                    String alias = buffer.substring(18);
                                                    if(!mac.equals(clicked.st.mac)){
                                                        temp_in.println(mac + ' ' + alias);
                                                    }
                                                }else{
                                                    Log.e("HIJACKER/setup", "Aliases file format error: " + buffer);
                                                }
                                                buffer = aliases_out.readLine();
                                            }
                                            temp_in.flush();
                                            temp_in.close();
                                            aliases_out.close();

                                            aliases_file.delete();
                                            temp_aliases.renameTo(aliases_file);
                                            aliases_in = new FileWriter(aliases_file, true);
                                        }
                                        if(dialog.result!=null){
                                            aliases_in.write(clicked.st.mac + ' ' + dialog.result + '\n');
                                            aliases_in.flush();
                                        }
                                    }catch(IOException e){
                                        Log.e("HIJACKER/MyListFrgm", e.toString());
                                    }
                                    aliases.put(clicked.st.mac, dialog.result);
                                    clicked.st.alias = dialog.result;
                                    clicked.st.update();
                                }
                            });
                            dialog.show(getFragmentManager(), "EditTextDialog");
                    }
                    return true;
                }
            });
            popup.show();
        }
    }
    @Override
    public void onResume(){
        super.onResume();
        currentFragment = FRAGMENT_AIRODUMP;
        refreshDrawer();
    }
}
