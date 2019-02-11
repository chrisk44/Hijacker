package com.hijacker;

/*
    Copyright (C) 2019  Christos Kyriakopoulos

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import static com.hijacker.MainActivity.SORT_BEACONS_FRAMES;
import static com.hijacker.MainActivity.SORT_DATA_FRAMES;
import static com.hijacker.MainActivity.SORT_ESSID;
import static com.hijacker.MainActivity.SORT_NOSORT;
import static com.hijacker.MainActivity.SORT_PWR;
import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.manuf_filter;
import static com.hijacker.MainActivity.opn;
import static com.hijacker.MainActivity.pwr_filter;
import static com.hijacker.MainActivity.show_ap;
import static com.hijacker.MainActivity.show_ch;
import static com.hijacker.MainActivity.show_na_st;
import static com.hijacker.MainActivity.show_st;
import static com.hijacker.MainActivity.sort;
import static com.hijacker.MainActivity.sort_reverse;
import static com.hijacker.MainActivity.wep;
import static com.hijacker.MainActivity.wpa;

public class FiltersDialog extends DialogFragment {
    String sort_texts[];
    View view;
    EditText manufView;
    TextView pwrTv;
    Button sortSelectBtn;
    CheckBox apCb, stCb, stNaCb, wpaCb, wepCb, opnCb, sortReverseCb;
    CheckBox[] channelCb = new CheckBox[15];
    SeekBar seek;
    int temp_sort;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        view = getActivity().getLayoutInflater().inflate(R.layout.filters, null);

        sort_texts = new String[]{
                getString(R.string.sort_nosort),
                getString(R.string.sort_essid),
                getString(R.string.sort_beacons_frames),
                getString(R.string.sort_data_frames),
                getString(R.string.sort_pwr)
        };

        apCb = view.findViewById(R.id.ap_cb);
        stCb = view.findViewById(R.id.st_cb);
        stNaCb = view.findViewById(R.id.st_na_cb);
        channelCb[0] = view.findViewById(R.id.cb_all);
        channelCb[1] = view.findViewById(R.id.cb_1);
        channelCb[2] = view.findViewById(R.id.cb_2);
        channelCb[3] = view.findViewById(R.id.cb_3);
        channelCb[4] = view.findViewById(R.id.cb_4);
        channelCb[5] = view.findViewById(R.id.cb_5);
        channelCb[6] = view.findViewById(R.id.cb_6);
        channelCb[7] = view.findViewById(R.id.cb_7);
        channelCb[8] = view.findViewById(R.id.cb_8);
        channelCb[9] = view.findViewById(R.id.cb_9);
        channelCb[10] = view.findViewById(R.id.cb_10);
        channelCb[11] = view.findViewById(R.id.cb_11);
        channelCb[12] = view.findViewById(R.id.cb_12);
        channelCb[13] = view.findViewById(R.id.cb_13);
        channelCb[14] = view.findViewById(R.id.cb_14);
        wpaCb = view.findViewById(R.id.cb_wpa);
        wepCb = view.findViewById(R.id.cb_wep);
        opnCb = view.findViewById(R.id.cb_opn);
        seek = view.findViewById(R.id.seekBar);
        manufView = view.findViewById(R.id.manuf_filter_et);
        sortSelectBtn = view.findViewById(R.id.select_sort);
        sortReverseCb = view.findViewById(R.id.sort_reverse);
        pwrTv = view.findViewById(R.id.pwr);

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                pwrTv.setText("-" + progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        manufView.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    apply();
                    dismissAllowingStateLoss();
                    return true;
                }
                return false;
            }
        });

        sortSelectBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                showSortingPopup(v);
            }
        });

        builder.setView(view);
        builder.setTitle(R.string.filters);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
            }
        });
        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                apply();
            }
        });

        return builder.create();
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
    @Override
    public void onStart(){
        super.onStart();

        apCb.setChecked(show_ap);
        stCb.setChecked(show_st);
        stNaCb.setChecked(show_na_st);
        for(int i=0;i<channelCb.length;i++){
            channelCb[i].setChecked(show_ch[i]);
        }
        wpaCb.setChecked(wpa);
        wepCb.setChecked(wep);
        opnCb.setChecked(opn);
        seek.setProgress(pwr_filter);
        pwrTv.setText("-" + pwr_filter);
        manufView.setText(manuf_filter);
        sortSelectBtn.setText(sort_texts[sort]);
        sortReverseCb.setChecked(sort_reverse);
        temp_sort = sort;
    }
    void apply(){
        show_ap = apCb.isChecked();
        show_st = stCb.isChecked();
        show_na_st =  stNaCb.isChecked();
        for(int i=0;i<channelCb.length;i++){
            show_ch[i] = channelCb[i].isChecked();
        }

        wpa = wpaCb.isChecked();
        wep = wepCb.isChecked();
        opn = opnCb.isChecked();

        pwr_filter = seek.getProgress();

        sort = temp_sort;
        sort_reverse = sortReverseCb.isChecked();

        manuf_filter = manufView.getText().toString().replace("\n", "");

        Tile.filter();
    }
    void showSortingPopup(View v){
        PopupMenu popup = new PopupMenu(getActivity(), v);

        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
        popup.getMenu().add(0, SORT_NOSORT, 0, sort_texts[SORT_NOSORT]);
        popup.getMenu().add(0, SORT_ESSID, 1, sort_texts[SORT_ESSID]);
        popup.getMenu().add(0, SORT_BEACONS_FRAMES, 2, sort_texts[SORT_BEACONS_FRAMES]);
        popup.getMenu().add(0, SORT_DATA_FRAMES, 3, sort_texts[SORT_DATA_FRAMES]);
        popup.getMenu().add(0, SORT_PWR, 4, sort_texts[SORT_PWR]);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(android.view.MenuItem item) {
                temp_sort = item.getItemId();
                sortSelectBtn.setText(sort_texts[temp_sort]);
                return true;
            }
        });
        popup.show();
    }
}
