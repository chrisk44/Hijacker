package com.hijacker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.opn;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.pwr_filter;
import static com.hijacker.MainActivity.shell;
import static com.hijacker.MainActivity.shell3_in;
import static com.hijacker.MainActivity.show_ap;
import static com.hijacker.MainActivity.show_ch;
import static com.hijacker.MainActivity.show_na_st;
import static com.hijacker.MainActivity.show_st;
import static com.hijacker.MainActivity.su_thread;
import static com.hijacker.MainActivity.wep;
import static com.hijacker.MainActivity.wpa;

public class InstallFirmwareDialog extends DialogFragment {
    View view;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.install_firmware, null);

        builder.setView(view);
        builder.setTitle(R.string.install_nexmon_title);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        builder.setPositiveButton("Install", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(shell==null){
                    su_thread.start();
                    try{
                        //Wait for su shells to spawn
                        su_thread.join();
                    }catch(InterruptedException ignored){}
                }
                String firm_location = ((EditText)view.findViewById(R.id.firm_location)).getText().toString();
                String util_location = ((EditText)view.findViewById(R.id.util_location)).getText().toString();
                if(debug){
                    Log.d("InstallToolsDialog", "Installing Firmware in " + firm_location);
                    Log.d("InstallToolsDialog", "Installing Utility in " + util_location);
                }
                shell3_in.print("mount -o rw,remount,rw /system\n");
                shell3_in.flush();
                extract("fw_bcmdhd.bin", firm_location);
                extract("nexutil", util_location);
                shell3_in.print("mount -o ro,remount,ro /system\n");
                shell3_in.flush();
                Toast.makeText(getActivity().getApplicationContext(), "Installed firmware and utility", Toast.LENGTH_LONG).show();
            }
        });
        return builder.create();
    }
    void extract(String filename, String dest){
        File f = new File(path, filename);      //no permissions to write at dest
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
                shell3_in.print("mv " + path + '/' + filename + " " + dest + '\n');
                shell3_in.print("chmod 755 " + dest + '\n');
                shell3_in.flush();
            }catch(IOException e){
                Log.e("FileProvider", "Exception copying from assets", e);
            }
        }
    }
}