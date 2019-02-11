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

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import static com.hijacker.MainActivity.background;

public class StatsDialog extends DialogFragment {
    static boolean isResumed = false;
    TextView wpa_count, wpa2_count, wep_count, opn_count, hidden_count, connected_count;
    static Runnable runnable;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.ap_stats, null);

        if(wpa_count==null) {
            wpa_count = view.findViewById(R.id.wpa_count);
            wpa2_count = view.findViewById(R.id.wpa2_count);
            wep_count = view.findViewById(R.id.wep_count);
            opn_count = view.findViewById(R.id.opn_count);
            hidden_count = view.findViewById(R.id.hidden_count);
            connected_count = view.findViewById(R.id.connected_count);
        }

        runnable = new Runnable(){
            @Override
            public void run(){
                wpa_count.setText(Integer.toString(AP.wpa));
                wpa2_count.setText(Integer.toString(AP.wpa2));
                wep_count.setText(Integer.toString(AP.wep));
                opn_count.setText(Integer.toString(AP.opn));
                hidden_count.setText(Integer.toString(AP.hidden));
                connected_count.setText(Integer.toString(ST.connected) + '/' + Integer.toString(ST.STs.size()));
            }
        };
        runnable.run();

        builder.setView(view);
        builder.setTitle(R.string.ap_stats);
        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        return builder.create();
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
    @Override
    public void onResume(){
        super.onResume();
        isResumed = true;
    }
    @Override
    public void onPause(){
        super.onPause();
        isResumed = false;
    }
}
