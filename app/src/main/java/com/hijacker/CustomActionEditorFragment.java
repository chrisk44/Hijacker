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
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import static com.hijacker.CustomAction.TYPE_AP;
import static com.hijacker.CustomAction.TYPE_ST;
import static com.hijacker.CustomAction.cmds;
import static com.hijacker.CustomAction.save;
import static com.hijacker.MainActivity.FRAGMENT_CUSTOM;
import static com.hijacker.MainActivity.currentFragment;

public class CustomActionEditorFragment extends Fragment{
    CustomAction action;
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState){
        final View v = inflater.inflate(R.layout.custom_action_editor, container, false);

        if(action!=null){
            ((EditText)v.findViewById(R.id.title)).setText(action.getTitle());
            ((EditText)v.findViewById(R.id.start_cmd)).setText(action.getStart_cmd());
            ((EditText)v.findViewById(R.id.stop_cmd)).setText(action.getStop_cmd());
            if(action.getType()==TYPE_AP){
                ((RadioButton)v.findViewById(R.id.ap_rb)).setChecked(true);
                v.findViewById(R.id.st_rb).setEnabled(false);
                ((CheckBox)v.findViewById(R.id.requirement)).setChecked(action.requires_clients());
            }else{
                ((RadioButton) v.findViewById(R.id.st_rb)).setChecked(true);
                v.findViewById(R.id.ap_rb).setEnabled(false);
                ((CheckBox)v.findViewById(R.id.requirement)).setChecked(action.requires_connected());
                ((CheckBox)v.findViewById(R.id.requirement)).setText(R.string.requires_associated);
            }
            v.findViewById(R.id.title).setEnabled(true);
            v.findViewById(R.id.start_cmd).setEnabled(true);
            v.findViewById(R.id.stop_cmd).setEnabled(true);
            v.findViewById(R.id.requirement).setEnabled(true);
            v.findViewById(R.id.save_button).setEnabled(true);
        }

        ((RadioGroup)v.findViewById(R.id.radio_group)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i){
                v.findViewById(R.id.title).setEnabled(true);
                v.findViewById(R.id.start_cmd).setEnabled(true);
                v.findViewById(R.id.stop_cmd).setEnabled(true);
                v.findViewById(R.id.requirement).setEnabled(true);
                v.findViewById(R.id.save_button).setEnabled(true);
                ((CheckBox)v.findViewById(R.id.requirement)).setText(i==R.id.st_rb ? R.string.requires_associated : R.string.requires_clients);
            }
        });

        (v.findViewById(R.id.save_button)).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                String title = ((EditText)v.findViewById(R.id.title)).getText().toString();
                String start_cmd = ((EditText)v.findViewById(R.id.start_cmd)).getText().toString();
                String stop_cmd = ((EditText)v.findViewById(R.id.stop_cmd)).getText().toString();
                if(title.equals("")){
                    Snackbar.make(v, getString(R.string.title_empty), Snackbar.LENGTH_SHORT).show();
                }else if(start_cmd.equals("")){
                    Snackbar.make(v, getString(R.string.start_cmd_empty), Snackbar.LENGTH_SHORT).show();
                }else if(action!=null){
                    //update existing action
                    action.setTitle(title);
                    action.setStart_cmd(start_cmd);
                    action.setStop_cmd(stop_cmd);
                    if(action.getType()==TYPE_AP){
                        action.setRequires_clients(((CheckBox) v.findViewById(R.id.requirement)).isChecked());
                    }else{
                        action.setRequires_connected(((CheckBox) v.findViewById(R.id.requirement)).isChecked());
                    }
                    save();
                    Snackbar.make(v, getString(R.string.saved) + " " + action.getTitle(), Snackbar.LENGTH_SHORT).show();
                }else{
                    boolean found = false;
                    for(int i=0;i<cmds.size();i++){
                        if(cmds.get(i).getTitle().equals(title)){
                            found = true;
                            break;
                        }
                    }
                    if(found){
                        Snackbar.make(v, getString(R.string.action_exists), Snackbar.LENGTH_SHORT).show();
                    }else{
                        //create new action
                        if(((RadioGroup) v.findViewById(R.id.radio_group)).getCheckedRadioButtonId()==R.id.ap_rb){
                            //this action is for ap
                            action = new CustomAction(title, start_cmd, stop_cmd, TYPE_AP);
                            action.setRequires_clients(((CheckBox) v.findViewById(R.id.requirement)).isChecked());
                        }else{
                            //this action is for st
                            action = new CustomAction(title, start_cmd, stop_cmd, TYPE_ST);
                            action.setRequires_connected(((CheckBox) v.findViewById(R.id.requirement)).isChecked());
                        }
                        save();
                        Snackbar.make(v, getString(R.string.saved) + " " + action.getTitle(), Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
        });

        return v;
    }
    @Override
    public void onResume(){
        super.onResume();
        currentFragment = FRAGMENT_CUSTOM;
    }
}
