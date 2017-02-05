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

import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.File;

import static com.hijacker.CustomAction.TYPE_AP;
import static com.hijacker.CustomAction.TYPE_ST;
import static com.hijacker.CustomAction.cmds;
import static com.hijacker.CustomAction.save;
import static com.hijacker.MainActivity.FRAGMENT_CUSTOM;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.refreshDrawer;

public class CustomActionEditorFragment extends Fragment{
    View fragmentView;
    EditText title_et, start_cmd_et, stop_cmd_et, process_name_et;
    CheckBox requirement_cb, has_process_name_cb;
    Button save_btn;
    CustomAction action;
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){
        fragmentView = inflater.inflate(R.layout.custom_action_editor, container, false);

        title_et = (EditText)fragmentView.findViewById(R.id.title);
        start_cmd_et = (EditText)fragmentView.findViewById(R.id.start_cmd);
        stop_cmd_et = (EditText)fragmentView.findViewById(R.id.stop_cmd);
        process_name_et = (EditText)fragmentView.findViewById(R.id.process_name);
        requirement_cb = (CheckBox)fragmentView.findViewById(R.id.requirement);
        has_process_name_cb = (CheckBox)fragmentView.findViewById(R.id.has_process_name);
        save_btn = (Button)fragmentView.findViewById(R.id.save_button);

        ((RadioGroup)fragmentView.findViewById(R.id.radio_group)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i){
                title_et.setEnabled(true);
                start_cmd_et.setEnabled(true);
                stop_cmd_et.setEnabled(true);
                requirement_cb.setEnabled(true);
                has_process_name_cb.setEnabled(true);
                save_btn.setEnabled(true);
                requirement_cb.setText(i==R.id.st_rb ? R.string.requires_associated : R.string.requires_clients);
            }
        });

        has_process_name_cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                process_name_et.setEnabled(isChecked);
            }
        });

        save_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                String title = title_et.getText().toString();
                String start_cmd = start_cmd_et.getText().toString();
                String stop_cmd = stop_cmd_et.getText().toString();
                String process_name = process_name_et.getText().toString();
                if(title.equals("")){
                    Snackbar.make(fragmentView, getString(R.string.title_empty), Snackbar.LENGTH_SHORT).show();
                }else if(start_cmd.equals("")){
                    Snackbar.make(fragmentView, getString(R.string.start_cmd_empty), Snackbar.LENGTH_SHORT).show();
                }else if(title.contains("\n")){
                    Snackbar.make(fragmentView, getString(R.string.title_newline), Snackbar.LENGTH_SHORT).show();
                }else if(start_cmd.contains("\n")){
                    Snackbar.make(fragmentView, getString(R.string.start_cmd_newline), Snackbar.LENGTH_SHORT).show();
                }else if(stop_cmd.contains("\n")){
                    Snackbar.make(fragmentView, getString(R.string.stop_cmd_newline), Snackbar.LENGTH_SHORT).show();
                }else if(process_name.contains("\n")){
                    Snackbar.make(fragmentView, getString(R.string.process_name_newline), Snackbar.LENGTH_SHORT).show();
                }else if(process_name.equals("") && ((CheckBox)fragmentView.findViewById(R.id.has_process_name)).isChecked()){
                    Snackbar.make(fragmentView, getString(R.string.process_name_empty), Snackbar.LENGTH_SHORT).show();
                }else if(action!=null){
                    //update existing action
                    if(!action.getTitle().equals(title)){
                        String filename_before = Environment.getExternalStorageDirectory() + "/Hijacker-actions/" + action.getTitle() + ".action";
                        String filename_after = Environment.getExternalStorageDirectory() + "/Hijacker-actions/" + title + ".action";
                        new File(filename_before).renameTo(new File(filename_after));
                    }
                    action.setTitle(title);
                    action.setStart_cmd(start_cmd);
                    action.setStop_cmd(stop_cmd);
                    if(action.getType()==TYPE_AP){
                        action.setRequires_clients(requirement_cb.isChecked());
                    }else{
                        action.setRequires_connected(requirement_cb.isChecked());
                    }
                    save();
                    Snackbar.make(fragmentView, getString(R.string.saved) + " " + action.getTitle(), Snackbar.LENGTH_SHORT).show();
                    mFragmentManager.popBackStackImmediate();
                }else{
                    boolean found = false;
                    for(int i=0;i<cmds.size();i++){
                        if(cmds.get(i).getTitle().equals(title)){
                            found = true;
                            break;
                        }
                    }
                    if(found){
                        Snackbar.make(fragmentView, getString(R.string.action_exists), Snackbar.LENGTH_SHORT).show();
                    }else{
                        //create new action
                        if(((RadioGroup) fragmentView.findViewById(R.id.radio_group)).getCheckedRadioButtonId()==R.id.ap_rb){
                            //this action is for ap
                            action = new CustomAction(title, start_cmd, stop_cmd, process_name, TYPE_AP);
                            action.setRequires_clients(requirement_cb.isChecked());
                        }else{
                            //this action is for st
                            action = new CustomAction(title, start_cmd, stop_cmd, process_name, TYPE_ST);
                            action.setRequires_connected(requirement_cb.isChecked());
                        }
                        save();
                        Snackbar.make(fragmentView, getString(R.string.saved) + " " + action.getTitle(), Snackbar.LENGTH_SHORT).show();
                        mFragmentManager.popBackStackImmediate();
                    }
                }
            }
        });

        return fragmentView;
    }
    @Override
    public void onResume(){
        super.onResume();
        currentFragment = FRAGMENT_CUSTOM;
        if(action!=null){
            title_et.setText(action.getTitle());
            start_cmd_et.setText(action.getStart_cmd());
            stop_cmd_et.setText(action.getStop_cmd());
            if(action.getType()==TYPE_AP){
                ((RadioButton)fragmentView.findViewById(R.id.ap_rb)).setChecked(true);
                fragmentView.findViewById(R.id.st_rb).setEnabled(false);
                requirement_cb.setChecked(action.requires_clients());
            }else{
                ((RadioButton) fragmentView.findViewById(R.id.st_rb)).setChecked(true);
                fragmentView.findViewById(R.id.ap_rb).setEnabled(false);
                requirement_cb.setChecked(action.requires_connected());
                requirement_cb.setText(R.string.requires_associated);
            }
            if(action.hasProcessName()){
                has_process_name_cb.setChecked(true);
                process_name_et.setText(action.getProcess_name());
            }
            title_et.setEnabled(true);
            start_cmd_et.setEnabled(true);
            stop_cmd_et.setEnabled(true);
            requirement_cb.setEnabled(true);
            has_process_name_cb.setEnabled(true);
            process_name_et.setEnabled(action.hasProcessName());
            save_btn.setEnabled(true);
        }

        refreshDrawer();
    }
}
