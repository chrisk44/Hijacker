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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import static com.hijacker.MainActivity.background;

public class CustomDialog extends DialogFragment {
    String title, message;
    String positiveText, neutralText, negativeText;
    boolean cancelable = true;
    Runnable onPositiveClick, onNeutralClick, onNegativeClick;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(cancelable);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if(title!=null) builder.setTitle(title);
        if(message!=null) builder.setMessage(message);
        if(positiveText!=null){
            builder.setPositiveButton(positiveText, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int id){
                    if(onPositiveClick!=null) onPositiveClick.run();
                    synchronized(CustomDialog.this){
                        CustomDialog.this.notify();
                    }
                }
            });
        }
        if(neutralText!=null){
            builder.setNeutralButton(neutralText, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which){
                    if(onNeutralClick!=null) onNeutralClick.run();
                    synchronized(CustomDialog.this){
                        CustomDialog.this.notify();
                    }
                }
            });
        }
        if(negativeText!=null){
            builder.setNegativeButton(negativeText, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int id){
                    if(onNegativeClick!=null) onNegativeClick.run();
                    synchronized(CustomDialog.this){
                        CustomDialog.this.notify();
                    }
                }
            });
        }
        return builder.create();
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
    @Override
    public void onDismiss(DialogInterface dialogInterface){
        super.onDismiss(dialogInterface);

        synchronized(this){
            notify();
        }
    }

    public void setTitle(String title){ this.title = title; }
    public void setMessage(String message){ this.message = message; }
    public void setPositiveButton(@NonNull String text, Runnable runnable){
        this.positiveText = text;
        this.onPositiveClick = runnable;
    }
    public void setNeutralButton(@NonNull String text, Runnable runnable){
        this.neutralText = text;
        this.onNeutralClick = runnable;
    }
    public void setNegativeButton(@NonNull String text, Runnable runnable){
        this.negativeText = text;
        this.onNegativeClick = runnable;
    }
    public void setCancelable(boolean cancelable){ this.cancelable = cancelable; }
    public void _wait(){
        try{
            synchronized(this){
                wait();
            }
        }catch(InterruptedException ignored){}
    }
}
