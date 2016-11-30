package com.hijacker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

public class StatsDialog extends DialogFragment {
    TextView wpa_count, wpa2_count, wep_count, opn_count;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.ap_stats, null);

        if(wpa_count==null) {
            wpa_count = (TextView)view.findViewById(R.id.wpa_count);
            wpa2_count = (TextView)view.findViewById(R.id.wpa2_count);
            wep_count = (TextView)view.findViewById(R.id.wep_count);
            opn_count = (TextView)view.findViewById(R.id.opn_count);
        }

        wpa_count.setText(Integer.toString(AP.wpa));
        wpa2_count.setText(Integer.toString(AP.wpa2));
        wep_count.setText(Integer.toString(AP.wep));
        opn_count.setText(Integer.toString(AP.opn));

        builder.setView(view);
        builder.setTitle(R.string.ap_stats);
        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        return builder.create();
    }
}
