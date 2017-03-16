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
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import static com.hijacker.MainActivity.background;

public class EditTextDialog extends DialogFragment {
    String title = null, hint = null, result = null;
    boolean allowEmpty = false;
    View dialogView;
    EditText field;
    Runnable runnable = null;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        dialogView = getActivity().getLayoutInflater().inflate(R.layout.edit_text_dialog, null);
        field = (EditText)dialogView.findViewById(R.id.edit_text);

        if(title!=null) builder.setTitle(title);
        if(hint!=null) field.setHint(hint);

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){}
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which){}
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
        AlertDialog d = (AlertDialog)getDialog();
        if(d != null){
            d.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    result = field.getText().toString();
                    if(result.equals("") && !allowEmpty){
                        Snackbar.make(dialogView, getString(R.string.field_empty), Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    if(runnable!=null) runnable.run();
                    dismissAllowingStateLoss();
                }
            });
        }
    }
    void setRunnable(Runnable runnable){
        this.runnable = runnable;
    }
    void setTitle(String title){
        this.title = title;
    }
    void setHint(String hint){
        this.hint = hint;
    }
    void setAllowEmpty(boolean allowEmpty){
        this.allowEmpty = allowEmpty;
    }
}
