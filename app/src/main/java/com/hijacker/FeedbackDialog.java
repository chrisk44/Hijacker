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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.io.File;

import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.createReport;
import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.path;

public class FeedbackDialog extends DialogFragment{
    View dialogView;
    EditText feedbackView;
    ProgressBar progress;
    CheckBox include_report;
    File report;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        dialogView = getActivity().getLayoutInflater().inflate(R.layout.feedback_dialog, null);

        include_report = dialogView.findViewById(R.id.include_report);
        feedbackView = dialogView.findViewById(R.id.feedback_et);
        progress = dialogView.findViewById(R.id.progress);

        report = null;
        include_report.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                if(isChecked && report==null){
                    new ReportTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        });

        builder.setView(dialogView);
        builder.setTitle(getString(R.string.feedback));
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.setPositiveButton(R.string.send_email, new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                Intent intent = new Intent (Intent.ACTION_SEND);
                intent.setType("plain/text");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"kiriakopoulos44@gmail.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "Hijacker feedback");
                intent.putExtra(Intent.EXTRA_TEXT, feedbackView.getText().toString());
                if(report!=null){
                    Uri attachment = FileProvider.getUriForFile(FeedbackDialog.this.getActivity().getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider", report);
                    intent.putExtra(Intent.EXTRA_STREAM, attachment);
                }
                startActivity(intent);
            }
        });
        return builder.create();
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
    private class ReportTask extends AsyncTask<Void, String, Boolean>{
        @Override
        protected void onPreExecute(){
            progress.setIndeterminate(true);
        }
        @Override
        protected Boolean doInBackground(Void... params){
            report = new File(Environment.getExternalStorageDirectory() + "/report.txt");
            return createReport(report, path, null, Shell.getFreeShell().getShell());
        }
        @Override
        protected void onPostExecute(final Boolean success){
            progress.setIndeterminate(false);
            if(!success){
                if(debug) Log.e("HIJACKER/feedbackDialog", "Report not generated");
                report = null;
            }
        }
    }
}
