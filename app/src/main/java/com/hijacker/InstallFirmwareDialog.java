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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.hijacker.MainActivity.BUFFER_SIZE;
import static com.hijacker.MainActivity.PROCESS_AIRODUMP;
import static com.hijacker.MainActivity.busybox;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.devChipset;
import static com.hijacker.MainActivity.firm_backup_file;
import static com.hijacker.MainActivity.getLastLine;
import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.init;
import static com.hijacker.MainActivity.mDrawerLayout;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.stop;

public class InstallFirmwareDialog extends DialogFragment {
    View dialogView;
    Shell shell;
    Button positiveButton, neutralButton;
    ProgressBar progressBar;
    TextView firmwareView, devChipsetView;
    EditText utilView;
    CheckBox backup_cb;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        dialogView = getActivity().getLayoutInflater().inflate(R.layout.install_firmware, null);

        progressBar = dialogView.findViewById(R.id.install_firm_progress);
        firmwareView = dialogView.findViewById(R.id.firmware_location);
        devChipsetView = dialogView.findViewById(R.id.device_chipset);
        utilView = dialogView.findViewById(R.id.util_location);
        backup_cb = dialogView.findViewById(R.id.backup);

        devChipsetView.setText(devChipset);

        utilView.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    attemptInstall();
                    return true;
                }
                return false;
            }
        });

        //Adjust directories
        if(!(new File("/su").exists())){
            utilView.setText("/system/xbin");
        }
        backup_cb.setChecked(!(new File(firm_backup_file).exists()));

        dialogView.findViewById(R.id.util_fe_btn).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                final FileExplorerDialog dialog = new FileExplorerDialog();
                dialog.setToSelect(FileExplorerDialog.SELECT_DIR);
                dialog.setOnSelect(new Runnable(){
                    @Override
                    public void run(){
                        utilView.setText(dialog.result.getAbsolutePath());
                    }
                });
                dialog.show(getFragmentManager(), "FileExplorerDialog");
            }
        });

        builder.setView(dialogView);
        builder.setTitle(R.string.install_nexmon_title);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.setNeutralButton(R.string.restore, new DialogInterface.OnClickListener() {
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
            positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    attemptInstall();
                }
            });

            neutralButton = d.getButton(Dialog.BUTTON_NEUTRAL);
            neutralButton.setEnabled(false);
            neutralButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    attemptRestore();
                }
            });

            new FindFirmwareTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if(shell!=null) shell.done();
        if(init){
            init = false;
            mDrawerLayout.openDrawer(GravityCompat.START);
            ((MainActivity)getActivity()).main();
        }
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }

    void attemptInstall(){
        String firm_location = firmwareView.getText().toString();
        String util_location = utilView.getText().toString();

        utilView.setError(null);
        if(util_location.equals("")){
            utilView.setError(getString(R.string.field_required));
            utilView.requestFocus();
            return;
        }
        File util = new File(util_location);
        if(!util.exists()){
            utilView.setError(getString(R.string.dir_notfound));
            utilView.requestFocus();
            return;
        }

        install(firm_location, util_location);
    }
    void attemptRestore(){
        String firm_location = firmwareView.getText().toString();
        restore(firm_location);
    }

    void install(String firm_location, String util_location){
        boolean start_airodump = false;
        if(Airodump.isRunning()){
            start_airodump = true;
            stop(PROCESS_AIRODUMP);
        }

        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager!=null) wifiManager.setWifiEnabled(false);
        if(debug) Log.d("HIJACKER/InstFirmware", "Backing up firmware from " + firm_location);
        if(backup_cb.isChecked()){
            if(new File(firm_backup_file).exists()){
                Toast.makeText(getActivity(), R.string.backup_exists, Toast.LENGTH_SHORT).show();
            }else{
                shell.run("cp -n " + firm_location + " " + firm_backup_file);
                Toast.makeText(getActivity(), R.string.backup_created, Toast.LENGTH_SHORT).show();
            }
        }
        shell.done();                   //clear any existing output
        shell = Shell.getFreeShell();

        if(debug){
            Log.d("HIJACKER/InstFirmware", "Installing firmware in " + firm_location);
            Log.d("HIJACKER/InstFirmware", "Installing utility in " + util_location);
        }
        shell.run(busybox + " mount -o rw,remount,rw /system");

        String fw_filename;
        if(devChipset.startsWith("4339")) {
            fw_filename = "fw_bcmdhd_4339.bin";
        }else if(devChipset.startsWith("4358")){
            fw_filename = "fw_bcmdhd_4358.bin";
        }else{
            Log.e("HIJACKER/InstFirmware", "devChipset is " + devChipset + ", shouldn't be here");
            return;
        }
        if(!extract(fw_filename, firm_location)){
            Snackbar.make(dialogView, R.string.error_extracting_files, Snackbar.LENGTH_LONG).show();
        }
        if(!extract("nexutil", util_location + "/nexutil")){
            Snackbar.make(dialogView, R.string.error_extracting_files, Snackbar.LENGTH_LONG).show();
        }
        shell.run(busybox + " mount -o ro,remount,ro /system");

        Toast.makeText(getActivity(), R.string.installed_firm_util, Toast.LENGTH_SHORT).show();
        if(wifiManager!=null) wifiManager.setWifiEnabled(true);

        if(start_airodump) Airodump.startClean();

        dismissAllowingStateLoss();
    }
    void restore(String firm_location){
        if(debug) Log.d("HIJACKER/InstFirm", "Restoring firmware in " + firm_location);
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(wifiManager!=null) wifiManager.setWifiEnabled(false);

        shell.run(busybox + " mount -o rw,remount,rw /system");
        shell.run("cp " + firm_backup_file + " " + firm_location);
        shell.run(busybox + " mount -o ro,remount,ro /system");

        Toast.makeText(getActivity(), R.string.restored, Toast.LENGTH_SHORT).show();
        if(wifiManager!=null) wifiManager.setWifiEnabled(true);
        dismissAllowingStateLoss();
    }

    boolean extract(String filename, String dest){
        File f = new File(path, filename);      //no permissions to write at dest so extract at local directory and then move to target
        if(!f.exists()){
            try{
                InputStream in = getResources().getAssets().open(filename);
                FileOutputStream out = new FileOutputStream(f);
                byte[] buf = new byte[BUFFER_SIZE];
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
                return false;
            }
        }
        return true;
    }

    class FindFirmwareTask extends AsyncTask<Void, String, String>{
        @Override
        protected void onPreExecute() {
            progressBar.setIndeterminate(true);
        }
        @Override
        protected String doInBackground(Void... voids){
            shell = Shell.getFreeShell();
            shell.run(busybox + " find /system/ -type f -name \"fw_bcmdhd.bin\"; echo ENDOFFIND");
            String result = getLastLine(shell.getShell_out(), "ENDOFFIND");

            if(result.equals("ENDOFFIND")) return null;
            else return result;
        }

        @Override
        protected void onPostExecute(final String result){
            progressBar.setIndeterminate(false);

            if(result==null){
                //Firmware not found
                firmwareView.setText(getString(R.string.firmware_not_found));
            }else {
                //Firmware is at 'result' (absolute path)
                firmwareView.setText(result);

                //Enable 'install' button only if we have a firmware to install (BCM4339 or BCM4358)
                if(devChipset.startsWith("4339") || devChipset.startsWith("4358")) {
                    positiveButton.setEnabled(true);
                }else{
                    Snackbar.make(dialogView, R.string.fw_not_compatible, Snackbar.LENGTH_LONG).show();
                }

                //Enable 'restore' button only if we have a backup firmware to restore
                if(new File(firm_backup_file).exists()){
                    neutralButton.setEnabled(true);
                }
            }
        }
    }
}