package com.hijacker;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupMenu;

import static com.hijacker.AP.WEP;
import static com.hijacker.AP.WPA;
import static com.hijacker.AP.WPA2;
import static com.hijacker.MainActivity.FRAGMENT_AIRODUMP;
import static com.hijacker.MainActivity.aireplay_dir;
import static com.hijacker.MainActivity.airodump_dir;
import static com.hijacker.MainActivity.copy;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.prefix;
import static com.hijacker.MainActivity.startAireplay;
import static com.hijacker.MainActivity.startAirodump;
import static com.hijacker.MainActivity.stop;

public class MyListFragment extends ListFragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.list_fragment, container, false);
        setListAdapter(MainActivity.adapter);
        return ret;
    }
    @Override
    public void onListItemClick(ListView l, final View v, int position, long id){
        super.onListItemClick(l, v, position, id);
        final Item clicked = Item.items.get(position);
        if(clicked.type){
            //AP
            PopupMenu popup = new PopupMenu(getActivity(), v);
            popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

            popup.getMenu().add(0, 0, 0, "Info");
            popup.getMenu().add(0, 1, 1, "Copy MAC");
            popup.getMenu().add(0, 9, 2, "Copy disconnect command");
            popup.getMenu().add(0, 2, 3, "Disconnect...");
            if(clicked.ap == MainActivity.is_ap) popup.getMenu().add(0, 4, 5, "Stop Watching");
            else popup.getMenu().add(0, 3, 5, "Watch");
            if(clicked.ap.clients.size()>0) popup.getMenu().add(0, 5, 4 , "Disconnect Client");
            popup.getMenu().add(0, 6, 6, "DoS");
            if(clicked.ap.sec==WPA || clicked.ap.sec==WPA2 || clicked.ap.sec==WEP){
                popup.getMenu().add(0, 7, 7, "Crack");
                popup.getMenu().add(0, 8, 8, "Copy crack command");
            }

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(android.view.MenuItem item) {
                    switch(item.getItemId()) {
                        case 0:
                            //Info
                            APDialog dialog = new APDialog();
                            dialog.info_ap = clicked.ap;
                            dialog.show(getFragmentManager(), "APDialog");
                            break;
                        case 1:
                            //copy mac to clipboard
                            copy(clicked.ap.mac, v);
                            break;
                        case 2:
                            //Disconnect
                            stop(0);
                            if (debug) Log.d("MyListFragment", "Starting airodump for channel " + clicked.ap.ch);
                            startAirodump("--channel " + clicked.ap.ch);
                            if(debug) {
                                Log.d("MyListFragment", "Starting aireplay without targets...");
                                Log.d("MyListFragment", clicked.ap.mac);
                            }
                            startAireplay(clicked.ap.mac);
                            break;
                        case 3:
                            //Watch
                            MainActivity.isolate(clicked.ap.mac);
                            startAirodump("--channel " + clicked.ap.ch);
                            break;
                        case 4:
                            //Stop watching                 //Not used with IsolatedFragment
                            MainActivity.isolate(null);
                            startAirodump(null);
                            break;
                        case 5:
                            //Disconnect client
                            PopupMenu popup = new PopupMenu(getActivity(), v);

                            popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
                            for (int i = 0; i < clicked.ap.clients.size(); i++) {
                                popup.getMenu().add(0, i, i, clicked.ap.clients.get(i).mac);
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
                        case 6:
                            //DoS
                            MainActivity.startMdk(1, clicked.ap.mac);
                            break;
                        case 7:
                            //Crack
                            clicked.ap.crack();
                            break;
                        case 8:
                            //Copy command to crack to clipboard
                            String str;
                            if(clicked.ap.sec==WEP) str = prefix + " " + airodump_dir + " --channel " + clicked.ap.ch + " --bssid " + clicked.ap.mac + " --ivs -w /sdcard/cap/wep.cap " + iface;
                            else str = prefix + " " + airodump_dir + " --channel " + clicked.ap.ch + " --bssid " + clicked.ap.mac + " -w /sdcard/cap/wpa.cap " + iface;

                            copy(str, v);
                            break;
                        case 9:
                            //copy disconnect command to clipboard
                            String str2 = prefix + " " + airodump_dir + " --channel " + clicked.ap.ch + " " + iface;
                            copy(str2, v);
                            break;
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
            popup.getMenu().add(0, 1, 1, "Copy MAC");
            if(clicked.st.bssid!=null){
                popup.getMenu().add(0, 2, 2, "Disconnect");
                popup.getMenu().add(0, 3, 3, "Copy disconnect command");
            }

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(android.view.MenuItem item) {
                    switch(item.getItemId()) {
                        case 0:
                            //Info
                            STDialog dialog = new STDialog();
                            dialog.info_st = clicked.st;
                            dialog.show(getFragmentManager(), "STDialog");
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
    }
}
