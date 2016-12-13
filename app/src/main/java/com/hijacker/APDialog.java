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
import android.widget.Button;
import android.widget.TextView;

import static com.hijacker.MainActivity.runInHandler;

public class APDialog extends DialogFragment {
    AP info_ap;
    TextView ap[] = {null, null, null, null, null, null, null, null, null, null, null, null};
    Runnable runnable;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.ap_info, null);

        if(ap[0]==null) {
            ap[0] = (TextView) view.findViewById(R.id.essid);
            ap[1] = (TextView) view.findViewById(R.id.mac);
            ap[2] = (TextView) view.findViewById(R.id.channel);
            ap[3] = (TextView) view.findViewById(R.id.pwr);
            ap[4] = (TextView) view.findViewById(R.id.enc);
            ap[5] = (TextView) view.findViewById(R.id.auth);
            ap[6] = (TextView) view.findViewById(R.id.cipher);
            ap[7] = (TextView) view.findViewById(R.id.beacons);
            ap[8] = (TextView) view.findViewById(R.id.data);
            ap[9] = (TextView) view.findViewById(R.id.ivs);
            ap[10] = (TextView) view.findViewById(R.id.clients);
            ap[11] = (TextView) view.findViewById(R.id.manuf);
        }

        ap[0].setText(info_ap.essid);
        ap[1].setText(info_ap.mac);
        ap[2].setText(Integer.toString(info_ap.ch));
        ap[3].setText(Integer.toString(info_ap.pwr));
        ap[4].setText(info_ap.enc);
        ap[5].setText(info_ap.auth);
        ap[6].setText(info_ap.cipher);
        ap[7].setText(Integer.toString(info_ap.beacons));
        ap[8].setText(Integer.toString(info_ap.data));
        ap[9].setText(Integer.toString(info_ap.ivs));
        ap[10].setText(Integer.toString(info_ap.clients.size()));
        ap[11].setText(info_ap.manuf);

        runnable = new Runnable(){
            @Override
            public void run(){
                ap[0].setText(info_ap.essid);
                ap[1].setText(info_ap.mac);
                ap[2].setText(Integer.toString(info_ap.ch));
                ap[3].setText(Integer.toString(info_ap.pwr));
                ap[4].setText(info_ap.enc);
                ap[5].setText(info_ap.auth);
                ap[6].setText(info_ap.cipher);
                ap[7].setText(Integer.toString(info_ap.beacons));
                ap[8].setText(Integer.toString(info_ap.data));
                ap[9].setText(Integer.toString(info_ap.ivs));
                ap[10].setText(Integer.toString(info_ap.clients.size()));
                ap[11].setText(info_ap.manuf);
            }
        };
        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    Thread.sleep(1000);
                    while(APDialog.this.isResumed()){
                        runInHandler(runnable);
                        Thread.sleep(1000);
                    }
                }catch(InterruptedException ignored){}
            }
        }).start();

        builder.setView(view);
        builder.setTitle(info_ap.essid);
        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        return builder.create();
    }
}
