package com.hijacker;

/*
    Copyright (C) 2016  Christos Kyriakopoylos

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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import static com.hijacker.MainActivity.enable_monMode;
import static com.hijacker.MainActivity.load;
import static com.hijacker.MainActivity.shell;
import static com.hijacker.MainActivity.status;
import static com.hijacker.MainActivity.su_thread;
import static com.hijacker.MainActivity.test_cur_cmd;
import static com.hijacker.MainActivity.test_progress;
import static com.hijacker.MainActivity.test_thread;

public class TestDialog extends DialogFragment {
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        load();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.test, null);

        test_progress = (ProgressBar)view.findViewById(R.id.test_progress);
        test_progress.setProgress(0);
        status[0] = (ImageView)view.findViewById(R.id.imageView1);
        status[1] = (ImageView)view.findViewById(R.id.imageView2);
        status[2] = (ImageView)view.findViewById(R.id.imageView3);
        status[3] = (ImageView)view.findViewById(R.id.imageView4);
        status[0].setImageResource(android.R.color.transparent);
        status[1].setImageResource(android.R.color.transparent);
        status[2].setImageResource(android.R.color.transparent);
        status[3].setImageResource(android.R.color.transparent);
        test_cur_cmd = (TextView)view.findViewById(R.id.current_cmd);
        test_cur_cmd.setText(enable_monMode);
        if(shell==null){
            su_thread.start();
            try{
                //Wait for su shells to spawn
                su_thread.join();
            }catch(InterruptedException ignored){}
        }
        test_thread.start();

        builder.setView(view);
        builder.setTitle(R.string.testing);
        builder.setMessage(R.string.make_sure_wifi);
        builder.setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close
                test_thread.interrupt();
            }
        });
        return builder.create();
    }
}
