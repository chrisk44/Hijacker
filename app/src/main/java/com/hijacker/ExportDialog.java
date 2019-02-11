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
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import static com.hijacker.MainActivity.background;

public class ExportDialog extends DialogFragment{
    View dialogView;
    EditText filenameView;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        dialogView = getActivity().getLayoutInflater().inflate(R.layout.export, null);

        filenameView = dialogView.findViewById(R.id.output_file);
        filenameView.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    attemptExport(false);
                    return true;
                }
                return false;
            }
        });
        dialogView.findViewById(R.id.export_fe_btn).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                final FileExplorerDialog dialog = new FileExplorerDialog();
                dialog.setStartingDir(new RootFile(Environment.getExternalStorageDirectory().toString()));
                dialog.setToSelect(FileExplorerDialog.SELECT_DIR);
                dialog.setOnSelect(new Runnable(){
                    @Override
                    public void run(){
                        filenameView.setText(dialog.result.getAbsolutePath() + "/output.txt");
                        filenameView.setError(null);
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
                    attemptExport(true);
                    return false;
                }
            });
            positiveButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    attemptExport(false);
                }
            });
        }
    }
    void attemptExport(boolean override){
        filenameView.setError(null);
        String filename = filenameView.getText().toString();
        if(filename.equals("")){
            filenameView.setError(getString(R.string.field_required));
            filenameView.requestFocus();
            return;
        }
        if(override){
            export(new File(filename));
        }else{
            File out_file = new File(filename);

            if(out_file.exists()){
                filenameView.setError(getString(R.string.output_file_exists));
                filenameView.requestFocus();
            }else{
                export(out_file);
            }
        }
    }
    void export(File out_file){
        try{
            out_file.createNewFile();
            if(!out_file.canWrite()){
                filenameView.setError(getString(R.string.output_file_cant_write));
                filenameView.requestFocus();
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
                for(i=0;i<Tile.tiles.size();i++){
                    out.write(Tile.tiles.get(i).device.getExported() + '\n');
                    if(i==Tile.i-1) out.write('\n' + st_str + '\n');
                }
            }
            out.close();
            dismissAllowingStateLoss();
            Toast.makeText(getActivity(), R.string.output_file_exported, Toast.LENGTH_SHORT).show();
        }catch(IOException e){
            Log.e("HIJACKER/ExportDialog", "Exception: " + e.toString());
            filenameView.setError(getString(R.string.file_not_created));
            filenameView.requestFocus();
        }
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
}
