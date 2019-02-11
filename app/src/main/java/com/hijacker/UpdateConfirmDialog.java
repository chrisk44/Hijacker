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
import android.app.DownloadManager;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.versionName;

public class UpdateConfirmDialog extends DialogFragment{
    String newVersionName = null, link = null, message = null;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String text = getString(R.string.update_text) + "\n\n";
        text += getString(R.string.latest_version) + " " + newVersionName + "\n";
        text += getString(R.string.current_version) + " " + versionName + "\n";
        if(message!=null){
            text += "\nExtra Information: " + message;
        }

        builder.setTitle(R.string.update_title);
        builder.setMessage(text);
        builder.setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String filename = link.substring(link.lastIndexOf('/') + 1);

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(link));
                request.setTitle(filename);
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

                DownloadManager manager = (DownloadManager) UpdateConfirmDialog.this.getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                if(manager!=null) manager.enqueue(request);
                dismissAllowingStateLoss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });
        return builder.create();
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
}
