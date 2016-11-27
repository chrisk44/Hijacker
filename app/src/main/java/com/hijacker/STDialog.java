package com.hijacker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class STDialog extends DialogFragment {
    ST info_st;
    TextView st[] = {null, null, null, null, null, null};
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.st_info, null);

        if(st[0]==null) {
            st[0] = (TextView)view.findViewById(R.id.mac_st);
            st[1] = (TextView)view.findViewById(R.id.bssid_st);
            st[2] = (TextView)view.findViewById(R.id.pwr_st);
            st[3] = (TextView)view.findViewById(R.id.frames_st);
            st[4] = (TextView)view.findViewById(R.id.lost_st);
            st[5] = (TextView)view.findViewById(R.id.manuf_st);
        }

        st[0].setText(info_st.mac);

        if(info_st.bssid==null) st[1].setText(R.string.not_connected);
        else if(AP.getAPByMac(info_st.bssid)!=null) st[1].setText(info_st.bssid + " (" + AP.getAPByMac(info_st.bssid).essid + ")");
        else st[1].setText(info_st.bssid);

        st[2].setText(Integer.toString(info_st.pwr));
        st[3].setText(Integer.toString(info_st.frames));
        st[4].setText(Integer.toString(info_st.lost));
        st[5].setText(info_st.manuf);

        builder.setView(view);
        builder.setTitle(info_st.mac);
        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        builder.setNeutralButton(R.string.refresh, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });
        return builder.create();
    }
    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog)getDialog();
        if(d != null) {
            Button neutralButton = d.getButton(Dialog.BUTTON_NEUTRAL);
            neutralButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    st[0].setText(info_st.mac);

                    if(info_st.bssid==null) st[1].setText(R.string.not_connected);
                    else if(AP.getAPByMac(info_st.bssid)!=null) st[1].setText(info_st.bssid + " (" + AP.getAPByMac(info_st.bssid).essid + ")");
                    else st[1].setText(info_st.bssid);

                    st[2].setText(Integer.toString(info_st.pwr));
                    st[3].setText(Integer.toString(info_st.frames));
                    st[4].setText(Integer.toString(info_st.lost));
                    st[5].setText(info_st.manuf);
                }
            });
        }
    }
}
