package com.hijacker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.shell;
import static com.hijacker.MainActivity.shell3_in;
import static com.hijacker.MainActivity.shell3_out;
import static com.hijacker.MainActivity.su_thread;

public class RestoreFirmwareDialog extends DialogFragment {
    View view;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.restore_firmware, null);

        builder.setView(view);
        builder.setTitle(R.string.restore_firmware);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        builder.setPositiveButton("Restore", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.setNeutralButton("Find firmware", new DialogInterface.OnClickListener(){
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
                    if(shell==null){
                        su_thread.start();
                        try{
                            //Wait for su shells to spawn
                            su_thread.join();
                        }catch(InterruptedException ignored){}
                    }
                    String firm_location = ((EditText)view.findViewById(R.id.firm_location)).getText().toString();

                    File firm = new File(firm_location);
                    if(!firm.exists()){
                        Toast.makeText(getActivity().getApplicationContext(), "Directory for Firmware doesn't exist", Toast.LENGTH_SHORT).show();
                    }else if(!(new File(firm_location + "/fw_bcmdhd.bin").exists())){
                        Toast.makeText(getActivity().getApplicationContext(), "There is no fw_bcmdhd.bin in there", Toast.LENGTH_SHORT).show();
                    }else{
                        if(debug){
                            Log.d("RestoreFirmwareDialog", "Restoring firmware in " + firm_location);
                        }
                        shell3_in.print("busybox mount -o rw,remount,rw /system\n");
                        shell3_in.flush();

                        File origFirm = new File(path + "/fw_bcmdhd.orig.bin");
                        if(!origFirm.exists()){
                            Toast.makeText(getActivity().getApplicationContext(), "There is no backup", Toast.LENGTH_SHORT).show();
                        }else{
                            shell3_in.print("cp " + path + "/fw_bcmdhd.orig.bin " + firm_location + "/fw_bcmdhd.bin\n");
                            shell3_in.flush();
                            Toast.makeText(getActivity().getApplicationContext(), "Backup restored", Toast.LENGTH_SHORT).show();
                        }

                        shell3_in.print("busybox mount -o ro,remount,ro /system\n");
                        shell3_in.flush();
                        dismiss();
                    }
                }
            });
            neutralButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    positiveButton.setActivated(false);
                    if(shell==null){
                        su_thread.start();
                        try{
                            //Wait for su shells to spawn
                            su_thread.join();
                        }catch(InterruptedException ignored){}
                    }
                    ProgressBar progress = (ProgressBar)view.findViewById(R.id.install_firm_progress);
                    progress.setIndeterminate(true);
                    shell3_in.print("find /system/ -type f -name \"fw_bcmdhd.bin\"; echo ENDOFFIND\n");
                    shell3_in.flush();
                    try{
                        String buffer = null, lastline;
                        while(buffer==null){
                            buffer = shell3_out.readLine();
                        }
                        lastline = buffer;
                        while(!buffer.equals("ENDOFFIND")){
                            lastline = buffer;
                            buffer = shell3_out.readLine();
                        }
                        if(lastline.equals("ENDOFFIND")){
                            Toast.makeText(getActivity().getApplicationContext(), "Firmware not found. Are you sure you have BCM4339?", Toast.LENGTH_LONG).show();
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
}