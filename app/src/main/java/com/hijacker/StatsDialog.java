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

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import static com.hijacker.MainActivity.runInHandler;

public class StatsDialog extends DialogFragment {
    TextView wpa_count, wpa2_count, wep_count, opn_count, hidden_count, connected_count;
    Runnable runnable;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.ap_stats, null);

        if(wpa_count==null) {
            wpa_count = (TextView)view.findViewById(R.id.wpa_count);
            wpa2_count = (TextView)view.findViewById(R.id.wpa2_count);
            wep_count = (TextView)view.findViewById(R.id.wep_count);
            opn_count = (TextView)view.findViewById(R.id.opn_count);
            hidden_count = (TextView)view.findViewById(R.id.hidden_count);
            connected_count = (TextView)view.findViewById(R.id.connected_count);
        }

        wpa_count.setText(Integer.toString(AP.wpa));
        wpa2_count.setText(Integer.toString(AP.wpa2));
        wep_count.setText(Integer.toString(AP.wep));
        opn_count.setText(Integer.toString(AP.opn));
        hidden_count.setText(Integer.toString(AP.hidden));
        connected_count.setText(Integer.toString(ST.connected) + '/' + Integer.toString(ST.STs.size()));

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
        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    Thread.sleep(1000);
                    while(StatsDialog.this.isResumed()){
                        runInHandler(runnable);
                        Thread.sleep(1000);
                    }
                }catch(InterruptedException ignored){}
            }
        }).start();

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
}
