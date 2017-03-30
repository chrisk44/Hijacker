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
import android.os.Environment;
import android.support.design.widget.Snackbar;
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

import static com.hijacker.MainActivity.background;

public class ExportDialog extends DialogFragment{
    View dialogView;
    EditText output_file;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        dialogView = getActivity().getLayoutInflater().inflate(R.layout.export, null);

        output_file = (EditText)dialogView.findViewById(R.id.output_file);
        dialogView.findViewById(R.id.export_fe_btn).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                final FileExplorerDialog dialog = new FileExplorerDialog();
                dialog.setStartingDir(new RootFile(Environment.getExternalStorageDirectory().toString()));
                dialog.setToSelect(FileExplorerDialog.SELECT_DIR);
                dialog.setOnSelect(new Runnable(){
                    @Override
                    public void run(){
                        output_file.setText(dialog.result.getAbsolutePath() + "/output.txt");
                    }
                });
                dialog.show(getFragmentManager(), "FileExplorerDialog");
            }
        });

        builder.setView(dialogView);
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
                    if(output_file.getText().toString().equals("")){
                        Snackbar.make(dialogView, getString(R.string.filename_empty), Snackbar.LENGTH_SHORT).show();
                        return false;
                    }
                    export(new File(output_file.getText().toString()));
                    return false;
                }
            });
            positiveButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if(output_file.getText().toString().equals("")){
                        Snackbar.make(dialogView, getString(R.string.filename_empty), Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    File out_file = new File(output_file.getText().toString());

                    if(out_file.exists()){
                        Snackbar.make(dialogView, R.string.output_file_exists, Snackbar.LENGTH_LONG).show();
                    }else{
                        export(out_file);
                    }
                }
            });
        }
    }
    void export(File out_file){
        try{
            out_file.createNewFile();
            if(!out_file.canWrite()){
                Snackbar.make(dialogView, R.string.output_file_cant_write, Snackbar.LENGTH_SHORT).show();
                return;
            }
            FileWriter out = new FileWriter(out_file);
            String ap_str = "Access Points:\nMAC                 PWR  CH  Beacons    Data      #s   ENC  AUTH  CIPHER  Hidden  ESSID - Manufacturer";
            String st_str = "Stations:\nMAC                BSSID               PWR  Frames    Lost  Manufacturer - Probes";
            out.write("Hijacker - " + new Date().toString() + "\n\n");
            if(((RadioGroup) dialogView.findViewById(R.id.radio_group)).getCheckedRadioButtonId()==R.id.all_rb){
                //export all
                out.write(ap_str + '\n');
                for(int i=0;i<AP.APs.size();i++){
                    out.write(AP.APs.get(i).getExported() + '\n');
                }
                out.write('\n' + st_str + '\n');
                for(int i=0;i<ST.STs.size();i++){
                    out.write(ST.STs.get(i).getExported() + '\n');
                }
            }else{
                //export visible
                out.write(ap_str + '\n');
                int i;
                for(i=0;i<Tile.i;i++){
                    out.write(Tile.tiles.get(i).ap.getExported() + '\n');
                }
                out.write('\n' + st_str + '\n');
                for(;i<Tile.tiles.size();i++){
                    out.write(Tile.tiles.get(i).st.getExported() + '\n');
                }
            }
            out.close();
            dismissAllowingStateLoss();
            Toast.makeText(getActivity(), R.string.output_file_exported, Toast.LENGTH_SHORT).show();
        }catch(IOException e){
            Log.e("HIJACKER/ExportDialog", "Exception: " + e.toString());
            Snackbar.make(dialogView, getString(R.string.file_not_created), Snackbar.LENGTH_SHORT).show();
        }
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
}
