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
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.getLastLine;
import static com.hijacker.MainActivity.path;

public class InstallFirmwareDialog extends DialogFragment {
    View view;
    Shell shell;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        view = getActivity().getLayoutInflater().inflate(R.layout.install_firmware, null);

        if(!(new File("/su").exists())){
            ((EditText)view.findViewById(R.id.util_location)).setText("/system/xbin");
        }
        if(new File(path + "/fw_bcmdhd.orig.bin").exists()){
            ((CheckBox)view.findViewById(R.id.backup)).setChecked(false);
        }
        shell = Shell.getFreeShell();

        builder.setView(view);
        builder.setTitle(R.string.install_nexmon_title);
        builder.setMessage(R.string.install_message);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.setNeutralButton(R.string.find_firmware, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i){}
        });
        return builder.create();
    }
    @Override
    public void onStart() {
        super.onStart();
        //Override positiveButton action to dismiss the fragment only when the directories exist, not on error
        AlertDialog d = (AlertDialog)getDialog();
        if(d != null) {
            final Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            Button neutralButton = d.getButton(Dialog.BUTTON_NEUTRAL);
            positiveButton.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v){
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    String firm_location = ((EditText)view.findViewById(R.id.firm_location)).getText().toString();
                    String util_location = ((EditText)view.findViewById(R.id.util_location)).getText().toString();
                    extract("fw_bcmdhd.bin", firm_location);
                    extract("nexutil", util_location);
                    shell.run("busybox mount -o ro,remount,ro /system");
                    Toast.makeText(getActivity(), R.string.installed_firm_util, Toast.LENGTH_SHORT).show();
                    dismiss();
                    return false;
                }
            });
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String firm_location = ((EditText)view.findViewById(R.id.firm_location)).getText().toString();
                    String util_location = ((EditText)view.findViewById(R.id.util_location)).getText().toString();

                    File firm = new File(firm_location);
                    File util = new File(util_location);
                    if(!firm.exists()){
                        Toast.makeText(getActivity(), R.string.dir_notfound_firm, Toast.LENGTH_SHORT).show();
                    }else if(!(new File(firm_location + "/fw_bcmdhd.bin").exists())){
                        Toast.makeText(getActivity(), R.string.firm_notfound, Toast.LENGTH_SHORT).show();
                    }else if(!util.exists()){
                        Toast.makeText(getActivity(), R.string.dir_notfound_util, Toast.LENGTH_SHORT).show();
                    }else{
                        if(debug){
                            Log.d("HIJACKER/InstFirmware", "Installing firmware in " + firm_location);
                            Log.d("HIJACKER/InstFirmware", "Installing utility in " + util_location);
                        }
                        shell.run("busybox mount -o rw,remount,rw /system");
                        if(((CheckBox)view.findViewById(R.id.backup)).isChecked()){
                            if(new File(path + "/fw_bcmdhd.orig.bin").exists()){
                                Toast.makeText(getActivity(), R.string.backup_exists, Toast.LENGTH_SHORT).show();
                            }else{
                                shell.run("cp -n " + firm_location + "/fw_bcmdhd.bin " + path + "/fw_bcmdhd.orig.bin");
                                Toast.makeText(getActivity(), R.string.backup_created, Toast.LENGTH_SHORT).show();
                            }
                        }
                        shell.done();                   //clear any existing output
                        shell = Shell.getFreeShell();
                        shell.run("strings " + firm_location + "/fw_bcmdhd.bin | busybox grep \"FWID:\"; echo ENDOFSTRINGS");
                        String result = getLastLine(shell.getShell_out(), "ENDOFSTRINGS");
                        result = result.substring(0, 4);

                        if(result.equals("4339")){
                            WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                            wifiManager.setWifiEnabled(false);
                            extract("fw_bcmdhd.bin", firm_location);
                            extract("nexutil", util_location);
                            shell.run("busybox mount -o ro,remount,ro /system");
                            Toast.makeText(getActivity(), R.string.installed_firm_util, Toast.LENGTH_SHORT).show();
                            wifiManager.setWifiEnabled(true);
                            dismiss();
                        }else{
                            Toast.makeText(getActivity(), R.string.fw_not_compatible, Toast.LENGTH_LONG).show();
                            if(debug) Log.d("HIJACKER/InstFirmware", "Firmware verification is: " + result);
                        }
                    }
                }
            });
            neutralButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    positiveButton.setActivated(false);
                    ProgressBar progress = (ProgressBar)view.findViewById(R.id.install_firm_progress);
                    progress.setIndeterminate(true);
                    shell.run("busybox find /system/ -type f -name \"fw_bcmdhd.bin\"; echo ENDOFFIND");
                    BufferedReader out = shell.getShell_out();
                    try{
                        String buffer = null, lastline;
                        while(buffer==null){
                            buffer = out.readLine();
                        }
                        lastline = buffer;
                        while(!buffer.equals("ENDOFFIND")){
                            lastline = buffer;
                            buffer = out.readLine();
                        }
                        if(lastline.equals("ENDOFFIND")){
                            Toast.makeText(getActivity(), R.string.firm_notfound_bcm, Toast.LENGTH_LONG).show();
                        }else{
                            lastline = lastline.substring(0, lastline.length()-14);
                            ((EditText)view.findViewById(R.id.firm_location)).setText(lastline);
                        }
                    }catch(IOException ignored){}

                    progress.setIndeterminate(false);
                    positiveButton.setActivated(true);
                }
            });
        }
    }
    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        shell.done();
        if(MainActivity.init) new InstallToolsDialog().show(getFragmentManager(), "InstallToolsDialog");
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
            }catch(IOException e){
                Log.e("HIJACKER/FileProvider", "Exception copying from assets", e);
            }
        }
    }
}