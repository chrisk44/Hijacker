package com.hijacker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import static com.hijacker.MainActivity.pref_edit;

public class DisclaimerDialog extends DialogFragment {
    static long openTime;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        openTime = System.currentTimeMillis();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.disclaimer);
        builder.setTitle("Disclaimer");
        builder.setPositiveButton("I agree", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                pref_edit.putBoolean("disclaimer", true);
                pref_edit.commit();
                new FirstRunDialog().show(getFragmentManager(), "FirstRunDialog");
                dismiss();
            }
        });
        builder.setNegativeButton("I don't agree", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //return
                getActivity().finish();
            }
        });
        return builder.create();
    }
}
