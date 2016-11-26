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
    /*@Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog)getDialog();
        if(d != null) {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(System.currentTimeMillis() - openTime >= 3000) {
                        pref_edit.putBoolean("disclaimer", true);
                        pref_edit.commit();
                        dismiss();
                        new FirstRunDialog().show(getFragmentManager(), "FirstRunDialog");
                        dismiss();
                    }else{
                        ErrorDialog dialog1 = new ErrorDialog();
                        dialog1.setTitle("WOW!!");
                        dialog1.setMessage("You read that in less that 3 seconds?!\nGo back and read it...");
                        dialog1.setShowExit(false);
                        dialog1.show(getFragmentManager(), "TL;DR");
                    }
                }
            });
        }
    }*/
}
