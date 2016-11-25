package com.hijacker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;

public class ErrorDialog extends DialogFragment {
    String message;
    String title = "Error";
    boolean showExit = true;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });
        if(showExit) {
            builder.setNeutralButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().finish();
                }
            });
        }
        if(message!=null) {
            builder.setTitle(title);
            builder.setMessage(this.message);
        }else{
            Log.d("ErrorDialog", "Message not set");
            builder.setTitle("Error on ErrorDialog");
            builder.setMessage("Yes, this is an ErrorDialog saying that there was an error displaying a previous ErrorDialog. -_-");
        }
        return builder.create();
    }
    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        DisclaimerDialog.openTime = System.currentTimeMillis();
    }
    public void setMessage(String msg){ this.message = msg; }
    public void setTitle(String title){ this.title = title; }
    public void setShowExit(boolean show){this.showExit = show; }
}
