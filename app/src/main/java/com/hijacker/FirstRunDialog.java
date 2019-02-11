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
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AlertDialog;

import static com.hijacker.MainActivity.arch;
import static com.hijacker.MainActivity.mDrawerLayout;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.background;

public class FirstRunDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.first_run);
        builder.setTitle(R.string.first_run_title);
        builder.setPositiveButton(R.string.install_firmware, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(arch.equals("armv7l")){
                    MainActivity.init = true;
                    new InstallFirmwareDialog().show(mFragmentManager, "InstallFirmwareDialog");
                }else{
                    mDrawerLayout.openDrawer(GravityCompat.START);
                    ErrorDialog errdialog = new ErrorDialog();
                    errdialog.setMessage(getString(R.string.not_armv7l_firm));
                    errdialog.show(mFragmentManager, "ErrorDialog");
                }
            }
        });
        builder.setNegativeButton(R.string.home, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //return
                dismissAllowingStateLoss();
                mDrawerLayout.openDrawer(GravityCompat.START);
                ((MainActivity)getActivity()).main();
            }
        });
        builder.setNeutralButton(R.string.exit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getActivity().finish();
            }
        });
        return builder.create();
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
}
