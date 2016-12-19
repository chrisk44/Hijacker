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
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class ExportDialog extends DialogFragment{
    View view;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        view = getActivity().getLayoutInflater().inflate(R.layout.export, null);

        builder.setView(view);
        builder.setTitle(R.string.export);
        builder.setPositiveButton(R.string.export, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {}
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { }
        });
        return builder.create();
    }
    @Override
    public void onStart(){
        super.onStart();
        AlertDialog d = (AlertDialog)getDialog();
        if(d != null){
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v){
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    export(new File(((EditText)view.findViewById(R.id.output_file)).getText().toString()));
                    return false;
                }
            });
            positiveButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    File out_file = new File(((EditText) view.findViewById(R.id.output_file)).getText().toString());

                    if(out_file.exists()){
                        Toast.makeText(getActivity(), R.string.output_file_exists, Toast.LENGTH_LONG).show();
                    }else export(out_file);
                }
            });
        }
    }
    void export(File out_file){
        try{
            out_file.createNewFile();
            if(!out_file.canWrite()){
                Toast.makeText(getActivity(), R.string.output_file_cant_write, Toast.LENGTH_SHORT).show();
                return;
            }
            FileWriter out = new FileWriter(out_file);
            String ap_str = "Access Points:\nMAC\t\t\tPWR\tChannel\tBeacons\tData\t#s\tEnc\tAuth\tCipher\tHidden\tESSID\tManufacturer\n";
            String st_str = "\nStations:\nMAC\t\t\tConnected To\t\tPWR\tFrames\tLost\tManufacturer\n";
            out.write("Hijacker - " + new Date().toString() + "\n\n");
            if(((RadioGroup) view.findViewById(R.id.radio_group)).getCheckedRadioButtonId()==R.id.all_rb){
                //export all
                out.write(ap_str);
                for(int i=0;i<AP.APs.size();i++){
                    out.write(AP.APs.get(i).toString());
                }
                out.write(st_str);
                for(int i=0;i<ST.STs.size();i++){
                    out.write(ST.STs.get(i).toString());
                }
            }else{
                //export visible
                out.write(ap_str);
                int i;
                for(i=0;i<Item.i;i++){
                    out.write(Item.items.get(i).ap.toString());
                }
                out.write(st_str);
                for(;i<Item.items.size();i++){
                    out.write(Item.items.get(i).st.toString());
                }
            }
            out.close();
            Toast.makeText(getActivity(), R.string.output_file_exported, Toast.LENGTH_SHORT).show();
        }catch(IOException e){ Log.e("ExportDialog", "Exception: " + e.toString()); }
        dismiss();
    }
}
