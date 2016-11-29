package com.hijacker;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupMenu;

import static com.hijacker.MainActivity.FRAGMENT_AIRODUMP;
import static com.hijacker.MainActivity.aireplay_dir;
import static com.hijacker.MainActivity.copy;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.iface;
import static com.hijacker.MainActivity.prefix;

public class MyListFragment2 extends ListFragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.list_fragment, container, false);
        setListAdapter(MainActivity.adapter);
        return ret;
    }
    @Override
    public void onListItemClick(ListView l, final View v, int position, long id){
        super.onListItemClick(l, v, position, id);
        final Item clicked = Item.items.get(position);

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
    @Override
    public void onResume(){
        super.onResume();
        currentFragment = FRAGMENT_AIRODUMP;
    }
}
