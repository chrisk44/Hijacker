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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import static com.hijacker.MainActivity.getLastSeen;
import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.runInHandler;

public class APDialog extends DeviceDialog {
    AP ap;
    TextView[] views = {null, null, null, null, null, null, null, null, null, null, null, null, null};
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.ap_info, null);

        views[0] = view.findViewById(R.id.essid);
        views[1] = view.findViewById(R.id.mac);
        views[2] = view.findViewById(R.id.channel);
        views[3] = view.findViewById(R.id.pwr);
        views[4] = view.findViewById(R.id.enc);
        views[5] = view.findViewById(R.id.auth);
        views[6] = view.findViewById(R.id.cipher);
        views[7] = view.findViewById(R.id.beacons);
        views[8] = view.findViewById(R.id.data);
        views[9] = view.findViewById(R.id.ivs);
        views[10] = view.findViewById(R.id.clients);
        views[11] = view.findViewById(R.id.manuf);
        views[12] = view.findViewById(R.id.lastseen);

        builder.setView(view);
        builder.setTitle(ap.essid);
        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        return builder.create();
    }
    APDialog setAP(AP ap){
        this.ap = ap;
        return this;
    }
    void onRefresh(){
        views[0].setText(ap.essid);
        views[1].setText(ap.mac);
        views[2].setText(Integer.toString(ap.ch));
        views[3].setText(Integer.toString(ap.pwr));
        views[4].setText(ap.enc);
        views[5].setText(ap.auth);
        views[6].setText(ap.cipher);
        views[7].setText(Integer.toString(ap.getBeacons()));
        views[8].setText(Integer.toString(ap.getData()));
        views[9].setText(Integer.toString(ap.getIvs()));
        views[10].setText(Integer.toString(ap.clients.size()));
        views[11].setText(ap.manuf);
        views[12].setText(getLastSeen(ap.lastseen));
    }
}
