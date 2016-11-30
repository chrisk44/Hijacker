package com.hijacker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class CustomAPDialog extends DialogFragment {
    TextView ap[] = {null, null, null, null, null, null, null, null, null, null, null, null};
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view = getActivity().getLayoutInflater().inflate(R.layout.custom_ap_dialog, null);

        builder.setView(view);
        builder.setTitle(R.string.custom_ap_title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String mac = ((EditText)view.findViewById(R.id.custom_mac)).getText().toString();
                ReaverFragment.custom_mac = mac;
                ReaverFragment.select_button.setText(mac);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        return builder.create();
    }
}
