package com.hijacker;

/*
    Copyright (C) 2017  Christos Kyriakopoylos

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
/*
    This class is used to provide a more general object than AP and ST.
    This makes the implementation of an AVL tree easier (the AVL class has not been made public (yet)),
    as well as the creation of a new class like AP and ST.
 */

import android.app.Activity;
import android.app.FragmentManager;
import android.view.View;
import android.widget.PopupMenu;

import static com.hijacker.MainActivity.SORT_NOSORT;
import static com.hijacker.MainActivity.aliases;
import static com.hijacker.MainActivity.getManuf;
import static com.hijacker.MainActivity.sort;
import static com.hijacker.MainActivity.toSort;

abstract class Device{
    final static AVLTree<Device> avl = new AVLTree<>();
    String mac, manuf, alias;
    int pwr;
    long macID, lastseen = 0;    //macID is the mac as Long
    boolean isMarked = false;
    Tile tile;
    Device(String mac){
        this.mac = mac;
        macID = toLong(mac);
        this.manuf = getManuf(this.mac);
        this.alias = aliases.get(this.mac);
        if(sort!=SORT_NOSORT) toSort = true;

        avl.add(this, macID);
    }
    public abstract String toString();
    abstract String getExported();
    abstract void showInfo(FragmentManager fragmentManager);
    abstract void saveData();
    abstract void mark();
    abstract void unmark();
    abstract PopupMenu getPopupMenu(final Activity activity, final View v);

    static Device getByMac(String mac){
        return mac==null ? null : avl.findById(toLong(mac));
    }
    static void clear(){
        avl.clear();
    }
    static long toLong(String mac){
        return Long.decode("0x" + mac.replace(":", "").toLowerCase());
    }
}
