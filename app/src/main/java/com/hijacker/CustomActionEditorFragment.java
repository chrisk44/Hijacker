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

import android.app.Fragment;
import android.os.Bundle;
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
import static com.hijacker.MainActivity.actions_path;
import static com.hijacker.MainActivity.currentFragment;
import static com.hijacker.MainActivity.mFragmentManager;
import static com.hijacker.MainActivity.refreshDrawer;

public class CustomActionEditorFragment extends Fragment{
    View fragmentView;
    EditText titleView, startCmdView, stopCmdView, processNameView;
    CheckBox requirement_cb, has_process_name_cb;
    Button save_btn;
    CustomAction action;
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){
        fragmentView = inflater.inflate(R.layout.custom_action_editor, container, false);

        titleView = fragmentView.findViewById(R.id.title);
        startCmdView = fragmentView.findViewById(R.id.start_cmd);
        stopCmdView = fragmentView.findViewById(R.id.stop_cmd);
        processNameView = fragmentView.findViewById(R.id.process_name);
        requirement_cb = fragmentView.findViewById(R.id.requirement);
        has_process_name_cb = fragmentView.findViewById(R.id.has_process_name);
        save_btn = fragmentView.findViewById(R.id.save_button);

        ((RadioGroup)fragmentView.findViewById(R.id.radio_group)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i){
                titleView.setEnabled(true);
                startCmdView.setEnabled(true);
                stopCmdView.setEnabled(true);
                requirement_cb.setEnabled(true);
                has_process_name_cb.setEnabled(true);
                save_btn.setEnabled(true);
                requirement_cb.setText(i==R.id.st_rb ? R.string.requires_associated : R.string.requires_clients);
            }
        });

        has_process_name_cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                processNameView.setEnabled(isChecked);
            }
        });

        save_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                titleView.setError(null);
                startCmdView.setError(null);
                stopCmdView.setError(null);
                processNameView.setError(null);

                String title = titleView.getText().toString();
                String start_cmd = startCmdView.getText().toString();
                String stop_cmd = stopCmdView.getText().toString();
                String process_name = processNameView.getText().toString();
                if(title.equals("")){
                    titleView.setError(getString(R.string.title_empty));
                    titleView.requestFocus();
                }else if(title.contains("\n")){
                    titleView.setError(getString(R.string.title_newline));
                    titleView.requestFocus();
                }else if(start_cmd.equals("")){
                    startCmdView.setError(getString(R.string.start_cmd_empty));
                    startCmdView.requestFocus();
                }else if(start_cmd.contains("\n")){
                    startCmdView.setError(getString(R.string.start_cmd_newline));
                    startCmdView.requestFocus();
                }else if(stop_cmd.contains("\n")){
                    stopCmdView.setError(getString(R.string.stop_cmd_newline));
                    stopCmdView.requestFocus();
                }else if(process_name.contains("\n")){
                    processNameView.setError(getString(R.string.process_name_newline));
                    processNameView.requestFocus();
                }else if(process_name.equals("") && has_process_name_cb.isChecked()){
                    processNameView.setError(getString(R.string.process_name_empty));
                    processNameView.requestFocus();
                }else if(action!=null){
                    //update existing action
                    if(!action.getTitle().equals(title)){
                        String filename_before = actions_path + "/" + action.getTitle() + ".action";
                        String filename_after = actions_path + "/" + title + ".action";
                        new File(filename_before).renameTo(new File(filename_after));
                    }
                    action.setTitle(title);
                    action.setStartCmd(start_cmd);
                    action.setStopCmd(stop_cmd);
                    if(action.getType()==TYPE_AP){
                        action.setRequiresClients(requirement_cb.isChecked());
                    }else{
                        action.setRequiresConnected(requirement_cb.isChecked());
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
                        titleView.setError(getString(R.string.action_exists));
                        titleView.requestFocus();
                    }else{
                        //create new action
                        if(((RadioGroup) fragmentView.findViewById(R.id.radio_group)).getCheckedRadioButtonId()==R.id.ap_rb){
                            //this action is for ap
                            action = new CustomAction(title, start_cmd, stop_cmd, process_name, TYPE_AP);
                            action.setRequiresClients(requirement_cb.isChecked());
                        }else{
                            //this action is for st
                            action = new CustomAction(title, start_cmd, stop_cmd, process_name, TYPE_ST);
                            action.setRequiresConnected(requirement_cb.isChecked());
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
            titleView.setText(action.getTitle());
            startCmdView.setText(action.getStartCmd());
            stopCmdView.setText(action.getStopCmd());
            if(action.getType()==TYPE_AP){
                ((RadioButton)fragmentView.findViewById(R.id.ap_rb)).setChecked(true);
                fragmentView.findViewById(R.id.st_rb).setEnabled(false);
                requirement_cb.setChecked(action.requiresClients());
            }else{
                ((RadioButton) fragmentView.findViewById(R.id.st_rb)).setChecked(true);
                fragmentView.findViewById(R.id.ap_rb).setEnabled(false);
                requirement_cb.setChecked(action.requiresConnected());
                requirement_cb.setText(R.string.requires_associated);
            }
            if(action.hasProcessName()){
                has_process_name_cb.setChecked(true);
                processNameView.setText(action.getProcessName());
            }
            titleView.setEnabled(true);
            startCmdView.setEnabled(true);
            stopCmdView.setEnabled(true);
            requirement_cb.setEnabled(true);
            has_process_name_cb.setEnabled(true);
            processNameView.setEnabled(action.hasProcessName());
            save_btn.setEnabled(true);
        }

        refreshDrawer();
    }
}
