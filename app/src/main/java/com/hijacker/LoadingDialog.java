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
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import static com.hijacker.MainActivity.background;

public class LoadingDialog extends DialogFragment {
    String title = null;
    View dialogView;
    TextView loadingDescription;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        setCancelable(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        dialogView = getActivity().getLayoutInflater().inflate(R.layout.loading_dialog, null);
        loadingDescription = dialogView.findViewById(R.id.loadingDescription);

        if(title!=null) loadingDescription.setText(title);

        builder.setView(dialogView);
        return builder.create();
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
    void setInitText(String str){
        title = str;
    }
    void setText(String str){
        loadingDescription.setText(str);
    }
}
