package com.hijacker;

/*
    Copyright (C) 2017  Christos Kyriakopoylos

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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

import static com.hijacker.MainActivity.ANS_POSITIVE;
import static com.hijacker.MainActivity.REQ_EXIT;
import static com.hijacker.MainActivity.REQ_FEEDBACK;
import static com.hijacker.MainActivity.REQ_REPORT;
import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.connect;
import static com.hijacker.MainActivity.createReport;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.deviceModel;
import static com.hijacker.MainActivity.internetAvailable;
import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.pref;
import static com.hijacker.MainActivity.pref_edit;
import static com.hijacker.MainActivity.runInHandler;
import static com.hijacker.MainActivity.versionCode;
import static com.hijacker.MainActivity.versionName;

public class FeedbackDialog extends DialogFragment{
    EditText email_et, feedback_et;
    ProgressBar progress;
    CheckBox include_report;
    File report;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.feedback_dialog, null);
        email_et = (EditText)view.findViewById(R.id.email_et);
        include_report = (CheckBox)view.findViewById(R.id.include_report);
        feedback_et = (EditText)view.findViewById(R.id.feedback_et);
        progress = (ProgressBar)view.findViewById(R.id.progress);

        email_et.setText(pref.getString("user_email", ""));

        report = null;
        include_report.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                if(isChecked && report==null){
                    progress.setIndeterminate(true);
                    new Thread(new Runnable(){
                        @Override
                        public void run(){
                            report = new File(Environment.getExternalStorageDirectory() + "/report.txt");
                            if(!createReport(report, path, null, Shell.getFreeShell().getShell())){
                                if(debug) Log.e("HIJACKER/feedbackDialog", "Report not generated");
                                report = null;
                            }
                            runInHandler(new Runnable(){
                                @Override
                                public void run(){
                                    progress.setIndeterminate(false);
                                }
                            });
                        }
                    }).start();
                }
            }
        });

        builder.setView(view);
        builder.setTitle(getString(R.string.feedback));
        builder.setPositiveButton(R.string.send, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){}
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.setNeutralButton(R.string.send_email, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                Intent intent = new Intent (Intent.ACTION_SEND);
                intent.setType("plain/text");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"kiriakopoulos44@gmail.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Hijacker feedback");
                if(report!=null){
                    Uri attachment = FileProvider.getUriForFile(FeedbackDialog.this.getActivity().getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", report);
                    intent.putExtra(Intent.EXTRA_STREAM, attachment);
                }
                intent.putExtra(Intent.EXTRA_TEXT, feedback_et.getText().toString());
                startActivity(intent);
            }
        });
        return builder.create();
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog d = (AlertDialog)getDialog();
        if(d != null) {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if(!internetAvailable(FeedbackDialog.this.getActivity())){
                        Log.d("HIJACKER/SendLog", "No internet connection");
                        Snackbar.make(v, getString(R.string.no_internet), Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    final String feedback = feedback_et.getText().toString();
                    if(feedback.equals("")){
                        Snackbar.make(v, getString(R.string.feedback_empty), Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    final String email = email_et.getText().toString();
                    if(!email.equals("")){
                        pref_edit.putString("user_email", email);
                        pref_edit.commit();
                    }

                    progress.setIndeterminate(true);

                    new Thread(new Runnable(){
                        @Override
                        public void run(){
                            Looper.prepare();
                            FeedbackDialog.this.setCancelable(false);
                            Runnable runnable = new Runnable(){
                                @Override
                                public void run(){
                                    progress.setIndeterminate(false);
                                }
                            };
                            Socket socket = connect();
                            if(socket==null){
                                handler.post(runnable);
                                Snackbar.make(v, getString(R.string.server_error), Snackbar.LENGTH_SHORT).show();
                                return;
                            }

                            try{
                                PrintWriter in = new PrintWriter(socket.getOutputStream());
                                BufferedReader out = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                                in.print(REQ_FEEDBACK + '\n');
                                in.flush();

                                String buffer;
                                buffer = out.readLine();
                                if(buffer!=null){
                                    if(!buffer.equals(ANS_POSITIVE)){
                                        handler.post(runnable);
                                        Snackbar.make(v, getString(R.string.server_denied), Snackbar.LENGTH_SHORT).show();
                                        return;
                                    }
                                }else{
                                    handler.post(runnable);
                                    Snackbar.make(v, getString(R.string.connection_closed), Snackbar.LENGTH_SHORT).show();
                                    return;
                                }

                                in.print("Hijacker feedback - " + new Date().toString() + "\n");
                                in.print("User email: " + email + '\n');
                                in.print("App version: " + versionName + " (" + versionCode + ")\n");
                                in.print("Android version: " +  Build.VERSION.SDK_INT + '\n');
                                in.print("Device model: " + deviceModel + '\n');
                                in.print("\nFeedback:\n");
                                in.print(feedback + '\n');
                                in.print("EOF\n");
                                in.flush();

                                if(report!=null && include_report.isChecked()){
                                    in.print(REQ_REPORT + '\n');
                                    in.flush();

                                    buffer = out.readLine();
                                    if(buffer!=null){
                                        if(!buffer.equals(ANS_POSITIVE)){
                                            Snackbar.make(v, getString(R.string.server_denied), Toast.LENGTH_SHORT).show();
                                            handler.post(runnable);
                                            return;
                                        }
                                    }else{
                                        Snackbar.make(v, getString(R.string.connection_closed), Toast.LENGTH_SHORT).show();
                                        handler.post(runnable);
                                        return;
                                    }

                                    BufferedReader fileReader = new BufferedReader(new FileReader(report));

                                    in.print("User email: " + email + '\n');
                                    in.flush();
                                    buffer = fileReader.readLine();
                                    while(buffer!=null){
                                        in.print(buffer + '\n');
                                        in.flush();
                                        buffer = fileReader.readLine();
                                    }
                                    in.print("EOF\n");
                                    in.flush();
                                }
                                in.print(REQ_EXIT + '\n');
                                in.flush();

                                in.close();
                                out.close();
                                socket.close();
                                dismissAllowingStateLoss();
                            }catch(IOException e){
                                Log.e("HIJACKER/FeedbackOnSend", e.toString());
                                Snackbar.make(v, getString(R.string.unknown_error), Snackbar.LENGTH_SHORT).show();
                            }finally{
                                handler.post(runnable);
                                FeedbackDialog.this.setCancelable(true);
                            }
                        }
                    }).start();

                }
            });
        }
    }
    static Handler handler = new Handler();
}
