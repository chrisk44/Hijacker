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
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import static com.hijacker.MainActivity.notif_on;

public class CustomAPDialog extends DialogFragment {
    static final int FOR_MDK=0, FOR_REAVER=1;
    int mode;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view = getActivity().getLayoutInflater().inflate(R.layout.custom_ap_dialog, null);

        builder.setView(view);
        builder.setTitle(R.string.custom_ap_title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String mac = ((EditText) view.findViewById(R.id.custom_mac)).getText().toString();
                if(mode==FOR_REAVER){
                    ReaverFragment.ap = null;
                    ReaverFragment.custom_mac = mac;
                    ReaverFragment.select_button.setText(mac);
                }else if(mode==FOR_MDK){
                    MDKFragment.ados_ap = null;
                    MDKFragment.custom_mac = mac;
                    MDKFragment.select_button.setText(mac);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        return builder.create();
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!notif_on) super.show(fragmentManager, tag);
    }
}
