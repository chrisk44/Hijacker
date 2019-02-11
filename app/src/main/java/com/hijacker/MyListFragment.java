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

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import static com.hijacker.MainActivity.FRAGMENT_AIRODUMP;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.refreshDrawer;

public class MyListFragment extends ListFragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.list_fragment, container, false);
        setListAdapter(MainActivity.adapter);
        return ret;
    }
    @Override
    public void onListItemClick(ListView l, final View v, int position, long id){
        super.onListItemClick(l, v, position, id);
        Tile.tiles.get(position).device.getPopupMenu((MainActivity)getActivity(), v).show();
    }
    @Override
    public void onResume(){
        super.onResume();
        currentFragment = FRAGMENT_AIRODUMP;
        refreshDrawer();
    }
}
