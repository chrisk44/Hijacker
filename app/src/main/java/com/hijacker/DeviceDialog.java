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

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.AsyncTask;

import static com.hijacker.MainActivity.background;

abstract public class DeviceDialog extends DialogFragment{
    abstract void onRefresh();

    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
    @Override
    public void onResume(){
        super.onResume();
        new DialogRefreshTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}

