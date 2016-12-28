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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.init;
import static com.hijacker.MainActivity.load;
import static com.hijacker.MainActivity.main;
import static com.hijacker.MainActivity.notif_on;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.pref_edit;

public class InstallToolsDialog extends DialogFragment {
    View view;
    Shell shell;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        view = getActivity().getLayoutInflater().inflate(R.layout.install_tools, null);

        if(!(new File("/su").exists())){
            ((EditText)view.findViewById(R.id.tools_location)).setText("/system/xbin");
        }
        if(!(new File("/vendor").exists())){
            if(new File("/su").exists()){
                ((EditText) view.findViewById(R.id.lib_location)).setText("/su/lib");
            }else{
                ((EditText)view.findViewById(R.id.util_location)).setText("/system/lib");
            }
        }

        builder.setView(view);
        builder.setTitle(R.string.install_tools_title);
        builder.setMessage(R.string.install_message);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        builder.setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        return builder.create();
    }
    @Override
    public void onStart() {
        super.onStart();
        //Override positiveButton action to dismiss the fragment only when the directories exist, not on error
        AlertDialog d = (AlertDialog)getDialog();
        if(d != null) {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String tools_location = ((EditText)view.findViewById(R.id.tools_location)).getText().toString();
                    String lib_location = ((EditText)view.findViewById(R.id.lib_location)).getText().toString();
                    File tools = new File(tools_location);
                    File lib = new File(lib_location);
                    if(!tools.exists()){
                        Toast.makeText(getActivity(), R.string.dir_notfound_tools, Toast.LENGTH_SHORT).show();
                    }else if(!lib.exists()){
                        Toast.makeText(getActivity(), R.string.dir_notfound_lib, Toast.LENGTH_SHORT).show();
                    }else{
                        if(debug){
                            Log.d("HIJACKER/InstTools", "Installing Tools in " + tools_location);
                            Log.d("HIJACKER/InstTools", "Installing Library in " + lib_location);
                        }
                        shell = Shell.getFreeShell();
                        shell.run("busybox mount -o rw,remount,rw /system");
                        shell.run("cd " + path);
                        shell.run("rm !(oui.txt)");
                        extract("airbase-ng", tools_location);
                        extract("aircrack-ng", tools_location);
                        extract("aireplay-ng", tools_location);
                        extract("airodump-ng", tools_location);
                        extract("besside-ng", tools_location);
                        extract("ivstools", tools_location);
                        extract("iw", tools_location);
                        extract("iwconfig", tools_location);
                        extract("iwlist", tools_location);
                        extract("iwpriv", tools_location);
                        extract("kstats", tools_location);
                        extract("makeivs-ng", tools_location);
                        extract("mdk3", tools_location);
                        extract("nc", tools_location);
                        extract("packetforge-ng", tools_location);
                        extract("wesside-ng", tools_location);
                        extract("wpaclean", tools_location);
                        extract("reaver", tools_location);
                        extract("reaver-wash", tools_location);
                        extract("libfakeioctl.so", lib_location);
                        extract("toolbox", tools_location);
                        shell.run("busybox mount -o ro,remount,ro /system");
                        shell.done();
                        Toast.makeText(getActivity(), R.string.installed_tools_lib, Toast.LENGTH_SHORT).show();
                        pref_edit.putString("prefix", "LD_PRELOAD=" + lib_location + "/libfakeioctl.so");
                        pref_edit.commit();
                        load();
                        Toast.makeText(getActivity(), R.string.prefix_adjusted, Toast.LENGTH_LONG).show();
                        dismissAllowingStateLoss();
                    }
                }
            });
        }
    }
    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if(init){
            init = false;
            main();
        }
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!notif_on) super.show(fragmentManager, tag);
    }
    void extract(String filename, String dest){
        File f = new File(path, filename);      //no permissions to write at dest so extract at local directory and then move to target
        dest = dest + '/' + filename;
        if(!f.exists()){
            try{
                InputStream in = getResources().getAssets().open(filename);
                FileOutputStream out = new FileOutputStream(f);
                byte[] buf = new byte[1024];
                int len;
                while((len = in.read(buf))>0){
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                shell.run("mv " + path + '/' + filename + " " + dest);
                shell.run("chmod 755 " + dest);
                shell.run("chown root " + dest);
                shell.run("chgrp shell " + dest);
            }catch(IOException e){
                Log.e("HIJACKER/FileProvider", "Exception copying from assets", e);
            }
        }
    }
}
