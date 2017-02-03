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
/*
    Class to manage files as root. Similar functionality with java.io.File but
    with access to files that the app is not allowed to touch.

    EXTREME CARE is required as the methods createNewFile(), delete(), mkdir() and rmdir()
    act on files as root. If a wrong file is selected, or due to a bug (it's not perfect)
    the variables absolutePath and name have something wrong, the results could be catastrophic.
 */

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IllegalFormatFlagsException;
import java.util.List;

import static com.hijacker.MainActivity.debug;
import static com.hijacker.MainActivity.getLastLine;
import static com.hijacker.Shell.getFreeShell;

public class RootFile{
    static Shell shell;
    static BufferedReader out;
    private int length = -1;
    private String absolutePath = null, name = null;
    private boolean exists = false, isFile = false, isDirectory = false, isUnknownType = false;
    RootFile(String path) throws IllegalArgumentException{
        Log.d("TEST/RootFile", "Called new RootFile with path: " + path);
        if(path==null) throw new IllegalArgumentException("File path can't be null");
        if(path.length()==0) throw new IllegalArgumentException("File path has zero length");
        if(path.charAt(0)!='/') throw new IllegalArgumentException("File path must start with /");

        if(path.charAt(path.length()-1)=='/' && path.length()>1) path = path.substring(0, path.length()-1);

        shell.run("busybox ls \"" + path + "\" -d -l; echo ENDOFLS");
        String buffer = getLastLine(out, "ENDOFLS");

        if(buffer.equals("ENDOFLS")){
            if(debug) Log.e("HIJACKER/RootFile", "Couldn't read shell output");
            return;
        }

        //Isolate absolute path and name
        this.name = path.substring(path.lastIndexOf('/') + 1);
        this.absolutePath = isFile() ? path.substring(0, path.lastIndexOf('/')) : path;
        Log.d("TEST", "Absolute path: " + absolutePath);

        if(buffer.contains("No such")){
            this.absolutePath = path.substring(0, path.lastIndexOf('/'));
            this.name = path.substring(path.lastIndexOf('/') + 1);
            Log.d("TEST", "Absolute path: " + absolutePath);
            return;
        }

        exists = true;

        //Eliminate multiple spaces
        String before = "";
        while(!before.equals(buffer)){
            before = buffer;
            buffer = buffer.replace("  ", " ");
        }

        String temp[] = buffer.split(" ");
        //0: type & permissions, 4: size, 5,6,7: some date, rest is name
        if(temp[0].length()!=10){
            throw new IllegalFormatFlagsException(temp[0] + " is not how it should be");
        }
        if(temp[0].charAt(0)=='d'){
            this.isDirectory = true;
        }else if(temp[0].charAt(0)=='-'){
            this.isFile = true;
        }else{
            this.isUnknownType = true;
        }

        this.length = Integer.parseInt(temp[4]);
    }
    String getAbsolutePath(){ return absolutePath; }
    String getName(){ return name; }
    String getParentPath(){
        if(absolutePath.equals("/")) return "";
        else return absolutePath.substring(0, absolutePath.lastIndexOf('/') + 1);
    }
    boolean exists(){ return exists; }
    boolean isFile(){ return isFile; }
    boolean isDirectory(){ return isDirectory; }
    boolean isUnknownType(){ return isUnknownType; }
    boolean canRead(){ return true; }           //We are root
    boolean canWrite(){ return true; }          //We are root
    boolean canExecute(){ return true; }        //We are root
    int length(){ return length; }
    void createNewFile(){
        if(!exists()){
            if(absolutePath==null || name==null) throw new IllegalStateException("path or name is null");
            shell.run("busybox touch " + absolutePath + "/" + name);
            exists = true;
            isFile = true;
        }else throw new IllegalStateException("Already exists" + (isDirectory() ? " and it's a directory" : ""));
    }
    void delete(){
        if(exists() && isFile()){
            if(absolutePath==null || name==null) throw new IllegalStateException("path or name is null");
            shell.run("busybox rm " + absolutePath + "/" + name);
            exists = false;
            isFile = false;
            length = -1;
        }else{
            if(!exists()) throw new IllegalStateException("Doesn't exist");
            else throw new IllegalStateException("Not a file");
        }
    }
    void mkdir(){
        if(!exists){
            if(absolutePath==null || name==null) throw new IllegalStateException("path or name is null");
            shell.run("busybox mkdir " + absolutePath);
            exists = true;
            isDirectory = true;
        }else throw new IllegalStateException("Already exists" + (isFile() ? " and it's a file" : ""));
    }
    void rmdir(){
        if(exists() && isDirectory()){
            if(absolutePath==null || name==null) throw new IllegalStateException("path or name is null");
            shell.run("rm -rf " + absolutePath);
            exists = false;
            isDirectory = false;
            length = -1;
        }else{
            if(!exists()) throw new IllegalStateException("Doesn't exist");
            else throw new IllegalStateException("Not a directory");
        }
    }
    List<RootFile> listFiles(){
        List<RootFile> result = new ArrayList<>();
        if(this.isFile()){
            result.add(this);
            return result;
        }
        Shell shell2 = getFreeShell();
        shell2.run("busybox ls -l \"" + absolutePath + "\"; echo ENDOFLS");
        BufferedReader out2 = shell2.getShell_out();
        try{
            String buffer = null;
            while(buffer==null) buffer = out2.readLine();
            while(!buffer.equals("ENDOFLS")){
                if(buffer.startsWith("total")){
                    buffer = out2.readLine();
                    continue;
                }

                //Eliminate multiple spaces
                String before = "";
                while(!before.equals(buffer)){
                    before = buffer;
                    buffer = buffer.replace("  ", " ");
                }
                //Separate by ' ' to get the name
                String temp[] = buffer.split(" ");
                //Reconstruct the full_name (it may contain spaces, so it's many arguments)
                String full_name = "";
                for(int i=8;i<temp.length;i++){
                    full_name += temp[i] + ' ';
                }
                if(full_name.charAt(full_name.length()-1)==' '){
                    full_name = full_name.substring(0, full_name.length()-1);
                }
                if(!full_name.contains("->")){
                    if(debug) Log.d("HIJACKER/RootFile", "Found " + full_name);
                    result.add(new RootFile(absolutePath + (absolutePath.length()==1 ? "" : '/') + full_name));
                }

                buffer = out2.readLine();
            }

        }catch(IOException e){
            Log.e("HIJACKER/RootFile", e.toString());
            return null;
        }finally{
            shell2.done();
        }

        return result;
    }
    static void init(){
        shell = getFreeShell();
        out = shell.getShell_out();
    }
    static void finish(){
        shell.done();
    }
}
