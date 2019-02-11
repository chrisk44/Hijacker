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
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.hijacker.MainActivity.background;
import static com.hijacker.MainActivity.file_explorer_adapter;

public class FileExplorerDialog extends DialogFragment{
    static final int SELECT_EXISTING_FILE=1, SELECT_DIR=2;
    static List<RootFile> list = new ArrayList<>();
    RootFile result = null;
    ListView listView;
    ImageButton backButton, newFolderButton;
    TextView currentDir;
    Runnable onSelect = null, onCancel = null;
    RootFile start = null, current = null;
    int toSelect = 0;
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.file_explorer, null);

        currentDir = view.findViewById(R.id.currentDir);
        backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                goToDirectory(new RootFile(current.getParentPath()));
            }
        });
        newFolderButton = view.findViewById(R.id.newFolderButton);
        newFolderButton.setVisibility(toSelect==SELECT_DIR ? View.VISIBLE : View.INVISIBLE);
        newFolderButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                final EditTextDialog dialog = new EditTextDialog();
                dialog.setTitle(getString(R.string.folder_name));
                dialog.setRunnable(new Runnable(){
                    @Override
                    public void run(){
                        RootFile newFolder = new RootFile(current.getAbsolutePath() + "/" + dialog.result);
                        if(newFolder.exists()){
                            Toast.makeText(getActivity(), getString(R.string.folder_exists), Toast.LENGTH_SHORT).show();
                        }else{
                            newFolder.mkdir();
                            goToDirectory(newFolder);
                        }
                    }
                });
                dialog.show(getFragmentManager(), "EditTextDialog");
            }
        });

        listView = view.findViewById(R.id.explorer_listview);
        listView.setAdapter(file_explorer_adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                RootFile clicked = list.get(position);
                if(clicked.isDirectory()){
                    goToDirectory(list.get(position));
                }else{
                    onSelect(clicked);
                }
            }
        });

        builder.setView(view);
        if(toSelect==SELECT_DIR){
            builder.setPositiveButton(R.string.select, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int id){
                    onSelect(current);
                }
            });
        }
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(onCancel!=null){
                    onCancel.run();
                }
            }
        });

        goToDirectory(start==null ? new RootFile("/") : start);
        return builder.create();
    }
    @Override
    public void show(FragmentManager fragmentManager, String tag){
        if(!background) super.show(fragmentManager, tag);
    }
    void goToDirectory(RootFile file){
        while(!file.exists()){
            file = new RootFile(file.getParentPath());
        }
        current = file;
        list = file.listFiles();
        for(int i=0;i<list.size();i++){
            if((toSelect==SELECT_DIR && !list.get(i).isDirectory()) || list.get(i).isUnknownType()){
                list.remove(i);
                i--;
            }
        }
        Collections.sort(list, new Comparator<RootFile>(){
            @Override
            public int compare(RootFile o1, RootFile o2){
                if(o1.isFile() && o2.isDirectory()) return 1;
                else if(o1.isDirectory() && o2.isFile()) return -1;
                else return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        file_explorer_adapter.notifyDataSetChanged();
        backButton.setEnabled(!current.getAbsolutePath().equals("/"));
        currentDir.setText(current.getAbsolutePath());
    }
    void onSelect(RootFile file){
        result = file;
        if(onSelect!=null){
            onSelect.run();
        }
        this.dismissAllowingStateLoss();
    }
    void setStartingDir(RootFile file){
        start = file;
    }
    void setOnSelect(Runnable runnable){
        onSelect = runnable;
    }
    void setOnCancel(Runnable runnable){
        onCancel = runnable;
    }
    void setToSelect(int selection){
        toSelect = selection;
    }
}
