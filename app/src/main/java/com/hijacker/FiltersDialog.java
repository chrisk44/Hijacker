package com.hijacker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import static com.hijacker.MainActivity.opn;
import static com.hijacker.MainActivity.pwr_filter;
import static com.hijacker.MainActivity.show_ap;
import static com.hijacker.MainActivity.show_ch;
import static com.hijacker.MainActivity.show_na_st;
import static com.hijacker.MainActivity.show_st;
import static com.hijacker.MainActivity.wep;
import static com.hijacker.MainActivity.wpa;

public class FiltersDialog extends DialogFragment {
    View view;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.filters, null);

        ((CheckBox)view.findViewById(R.id.ap_cb)).setChecked(show_ap);
        ((CheckBox)view.findViewById(R.id.st_cb)).setChecked(show_st);
        ((CheckBox)view.findViewById(R.id.st_na_cb)).setChecked(show_na_st);
        ((CheckBox)view.findViewById(R.id.cb_all)).setChecked(show_ch[0]);
        ((CheckBox)view.findViewById(R.id.cb_1)).setChecked(show_ch[1]);
        ((CheckBox)view.findViewById(R.id.cb_2)).setChecked(show_ch[2]);
        ((CheckBox)view.findViewById(R.id.cb_3)).setChecked(show_ch[3]);
        ((CheckBox)view.findViewById(R.id.cb_4)).setChecked(show_ch[4]);
        ((CheckBox)view.findViewById(R.id.cb_5)).setChecked(show_ch[5]);
        ((CheckBox)view.findViewById(R.id.cb_6)).setChecked(show_ch[6]);
        ((CheckBox)view.findViewById(R.id.cb_7)).setChecked(show_ch[7]);
        ((CheckBox)view.findViewById(R.id.cb_8)).setChecked(show_ch[8]);
        ((CheckBox)view.findViewById(R.id.cb_9)).setChecked(show_ch[9]);
        ((CheckBox)view.findViewById(R.id.cb_10)).setChecked(show_ch[10]);
        ((CheckBox)view.findViewById(R.id.cb_11)).setChecked(show_ch[11]);
        ((CheckBox)view.findViewById(R.id.cb_12)).setChecked(show_ch[12]);
        ((CheckBox)view.findViewById(R.id.cb_13)).setChecked(show_ch[13]);
        ((CheckBox)view.findViewById(R.id.cb_14)).setChecked(show_ch[14]);

        ((CheckBox)view.findViewById(R.id.cb_wpa)).setChecked(wpa);
        ((CheckBox)view.findViewById(R.id.cb_wep)).setChecked(wep);
        ((CheckBox)view.findViewById(R.id.cb_opn)).setChecked(opn);

        SeekBar seek = (SeekBar)view.findViewById(R.id.seekBar);
        seek.setProgress(pwr_filter);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ((TextView)view.findViewById(R.id.pwr)).setText("-" + progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        ((TextView)view.findViewById(R.id.pwr)).setText("-" + pwr_filter);

        builder.setView(view);
        builder.setTitle("Filters");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        builder.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                show_ap = ((CheckBox)view.findViewById(R.id.ap_cb)).isChecked();
                show_st = ((CheckBox)view.findViewById(R.id.st_cb)).isChecked();
                show_na_st =  ((CheckBox)view.findViewById(R.id.st_na_cb)).isChecked();
                show_ch[0] =  ((CheckBox)view.findViewById(R.id.cb_all)).isChecked();
                show_ch[1] =  ((CheckBox)view.findViewById(R.id.cb_1)).isChecked();
                show_ch[2] =  ((CheckBox)view.findViewById(R.id.cb_2)).isChecked();
                show_ch[3] =  ((CheckBox)view.findViewById(R.id.cb_3)).isChecked();
                show_ch[4] =  ((CheckBox)view.findViewById(R.id.cb_4)).isChecked();
                show_ch[5] =  ((CheckBox)view.findViewById(R.id.cb_5)).isChecked();
                show_ch[6] =  ((CheckBox)view.findViewById(R.id.cb_6)).isChecked();
                show_ch[7] =  ((CheckBox)view.findViewById(R.id.cb_7)).isChecked();
                show_ch[8] =  ((CheckBox)view.findViewById(R.id.cb_8)).isChecked();
                show_ch[9] =  ((CheckBox)view.findViewById(R.id.cb_9)).isChecked();
                show_ch[10] = ((CheckBox)view.findViewById(R.id.cb_10)).isChecked();
                show_ch[11] = ((CheckBox)view.findViewById(R.id.cb_11)).isChecked();
                show_ch[12] = ((CheckBox)view.findViewById(R.id.cb_12)).isChecked();
                show_ch[13] = ((CheckBox)view.findViewById(R.id.cb_13)).isChecked();
                show_ch[14] = ((CheckBox)view.findViewById(R.id.cb_14)).isChecked();

                wpa = ((CheckBox)view.findViewById(R.id.cb_wpa)).isChecked();
                wep = ((CheckBox)view.findViewById(R.id.cb_wep)).isChecked();
                opn = ((CheckBox)view.findViewById(R.id.cb_opn)).isChecked();

                pwr_filter = ((SeekBar)view.findViewById(R.id.seekBar)).getProgress();

                Item.filter();
            }
        });
        return builder.create();
    }
}
