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
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;

import static com.hijacker.MainActivity.busybox;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.firm_backup_file;
import static com.hijacker.MainActivity.getDirectory;
import static com.hijacker.MainActivity.getLastLine;

public class RestoreFirmwareDialog extends DialogFragment {
    View dialogView;
    Shell shell;
    EditText firm_et;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        shell = Shell.getFreeShell();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        dialogView = getActivity().getLayoutInflater().inflate(R.layout.restore_firmware, null);

        firm_et = (EditText)dialogView.findViewById(R.id.firm_location);

        dialogView.findViewById(R.id.firm_fe_btn).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                final FileExplorerDialog dialog = new FileExplorerDialog();
                dialog.setToSelect(FileExplorerDialog.SELECT_DIR);
                dialog.setOnSelect(new Runnable(){
                    @Override
                    public void run(){
                        firm_et.setText(dialog.result.getAbsolutePath());
                    }
                });
                dialog.show(getFragmentManager(), "FileExplorerDialog");
            }
        });

        builder.setView(dialogView);
        builder.setTitle(R.string.restore_firmware);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        builder.setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
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
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String firm_location = firm_et.getText().toString();
                    firm_location = getDirectory(firm_location);

                    File firm = new File(firm_location);
                    if(!firm.exists()){
                        Snackbar.make(dialogView, R.string.dir_notfound_firm, Snackbar.LENGTH_SHORT).show();
                    }else if(!(new File(firm_location + "fw_bcmdhd.bin").exists())){
                        Snackbar.make(dialogView, R.string.firm_notfound, Snackbar.LENGTH_SHORT).show();
                    }else{
                        if(debug) Log.d("HIJACKER/RestoreFirm", "Restoring firmware in " + firm_location);
                        WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                        wifiManager.setWifiEnabled(false);

                        shell.run(busybox + " mount -o rw,remount,rw /system");
                        shell.run("cp " + firm_backup_file + " " + firm_location + "fw_bcmdhd.bin");
                        shell.run(busybox + " mount -o ro,remount,ro /system");

                        Toast.makeText(getActivity(), R.string.restored, Toast.LENGTH_SHORT).show();
                        wifiManager.setWifiEnabled(true);
                        dismissAllowingStateLoss();
                    }
                }
            });
            neutralButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    positiveButton.setActivated(false);
                    ProgressBar progress = (ProgressBar)dialogView.findViewById(R.id.install_firm_progress);
                    progress.setIndeterminate(true);
                    shell.run("find /system/ -type f -name \"fw_bcmdhd.bin\"; echo ENDOFFIND");
                    String lastline = getLastLine(shell.getShell_out(), "ENDOFFIND");
                    if(lastline.equals("ENDOFFIND")){
                        Snackbar.make(dialogView, R.string.firm_notfound_bcm, Snackbar.LENGTH_LONG).show();
                    }else{
                        lastline = lastline.substring(0, lastline.length()-14);
                        firm_et.setText(lastline);
                    }
                    progress.setIndeterminate(false);

                    positiveButton.setActivated(true);
                }
            });
        }
    }
    @Override
    public void onDismiss(final DialogInterface dialog){
        super.onDismiss(dialog);
        shell.done();
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
}