package com.hijacker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import static com.hijacker.MainActivity.main;
import static com.hijacker.MainActivity.su_thread;

public class FirstRunDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.first_run);
        builder.setTitle(R.string.first_run_title);
        builder.setPositiveButton(R.string.setup, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                su_thread.start();
                MainActivity.init = true;
                new InstallFirmwareDialog().show(getFragmentManager(), "InstallFirmwareDialog");
            }
        });
        builder.setNegativeButton(R.string.home, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //return
                dismiss();
                main();
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
}
