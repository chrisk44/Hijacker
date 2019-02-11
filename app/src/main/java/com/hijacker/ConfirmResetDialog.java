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

import static com.hijacker.MainActivity.load;
import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.pref_edit;

public class ConfirmResetDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.reset_dialog_title);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                pref_edit.putString("iface", getString(R.string.iface));
                pref_edit.putString("prefix", getString(R.string.prefix));
                pref_edit.putString("enable_monMode", getString(R.string.enable_monMode));
                pref_edit.putString("disable_monMode", getString(R.string.disable_monMode));
                pref_edit.putBoolean("enable_on_airodump", Boolean.parseBoolean(getString(R.string.enable_on_airodump)));
                pref_edit.putString("deauthWait", getString(R.string.deauthWait));
                pref_edit.putBoolean("show_notif", Boolean.parseBoolean(getString(R.string.show_notif)));
                pref_edit.putBoolean("show_details", Boolean.parseBoolean(getString(R.string.show_details)));
                pref_edit.putBoolean("airOnStartup", Boolean.parseBoolean(getString(R.string.airOnStartup)));
                pref_edit.putBoolean("debug", Boolean.parseBoolean(getString(R.string.debug)));
                pref_edit.putBoolean("delete_extra", Boolean.parseBoolean(getString(R.string.delete_extra)));
                pref_edit.putBoolean("always_cap", Boolean.parseBoolean(getString(R.string.always_cap)));
                pref_edit.putString("chroot_dir", getString(R.string.chroot_dir));
                pref_edit.putBoolean("monstart", Boolean.parseBoolean(getString(R.string.monstart)));
                pref_edit.putString("custom_chroot_cmd", "");
                pref_edit.putBoolean("cont_on_fail", Boolean.parseBoolean(getString(R.string.cont_on_fail)));
                pref_edit.putBoolean("watchdog", Boolean.parseBoolean(getString(R.string.watchdog)));
                pref_edit.putBoolean("target_deauth", Boolean.parseBoolean(getString(R.string.target_deauth)));
                pref_edit.putBoolean("update_on_startup", Boolean.parseBoolean(getString(R.string.auto_update)));
                pref_edit.commit();
                load();
                dismissAllowingStateLoss();
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });
        return builder.create();
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
}
