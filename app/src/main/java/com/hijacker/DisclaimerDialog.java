package com.hijacker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import static com.hijacker.MainActivity.pref_edit;

public class DisclaimerDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.disclaimer);
        builder.setTitle(R.string.disclaimer_title);
        builder.setPositiveButton(R.string.agree, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                pref_edit.putBoolean("disclaimer", true);
                pref_edit.commit();
                new FirstRunDialog().show(getFragmentManager(), "FirstRunDialog");
                dismiss();
            }
        });
        builder.setNegativeButton(R.string.not_agree, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //return
                getActivity().finish();
            }
        });
        return builder.create();
    }
}
