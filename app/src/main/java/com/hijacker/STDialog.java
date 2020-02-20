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
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

import static com.hijacker.MainActivity.getLastSeen;

public class STDialog extends DialogFragment {
    ST st;
    TextView[] views = {null, null, null, null, null, null, null, null};
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.st_info, null);

        views[0] = view.findViewById(R.id.mac_st);
        views[1] = view.findViewById(R.id.bssid_st);
        views[2] = view.findViewById(R.id.pwr_st);
        views[3] = view.findViewById(R.id.frames_st);
        views[4] = view.findViewById(R.id.lost_st);
        views[5] = view.findViewById(R.id.manuf_st);
        views[6] = view.findViewById(R.id.lastseen_st);
        views[7] = view.findViewById(R.id.probes_st);

        builder.setView(view);
        builder.setTitle(st.mac);
        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        return builder.create();
    }
    STDialog setST(ST st){
        this.st = st;
        return this;
    }
    void onRefresh(){
        views[0].setText(st.mac);

        if(st.connectedTo==null) views[1].setText(R.string.not_connected);
        else views[1].setText(st.connectedTo.mac + " (" + st.connectedTo.essid + ")");

        views[2].setText(String.format(Locale.getDefault(), "%d", st.pwr));
        views[3].setText(String.format(Locale.getDefault(), "%d", st.getFrames()));
        views[4].setText(String.format(Locale.getDefault(), "%d", st.getLost()));
        views[5].setText(st.manuf);
        views[6].setText(getLastSeen(st.lastseen));
        views[7].setText(st.probes);
    }
}
