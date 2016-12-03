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
    Class to handle su shells

    It starts a shell when needed, and when it's no longer needed (done() has been called for it)
    its output is reset and it's saved to be used later, when a new one is needed
    without starting a new one.

    To create a new one just do Shell shell = new Shell();
    run commands with shell.run(command);
    and when you're done with it, call shell.done() for it to be registered as free
    so it can be used later.
 */

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.hijacker.MainActivity.debug;

class Shell{
    private Process shell;
    private PrintWriter shell_in;
    private BufferedReader shell_out, shell_out_error;
    private static List<Shell> free = new ArrayList<>();
    Shell(){
        try{
            shell = Runtime.getRuntime().exec("su");
            shell_in = new PrintWriter(shell.getOutputStream());
            shell_out = new BufferedReader(new InputStreamReader(shell.getInputStream()));
            shell_out_error = new BufferedReader(new InputStreamReader(shell.getErrorStream()));
        }catch(IOException e){
            Log.e("Shell", "Error opening shell");
        }
    }
    BufferedReader getShell_out(){ return this.shell_out; }
    BufferedReader getShell_out_error(){ return this.shell_out_error; }
    void run(String cmd){
        this.shell_in.print(cmd + '\n');
        this.shell_in.flush();
    }
    void done(){
        if(debug) Log.d("Shell", "Freeing shell");
        try{
            run("echo");
            run("echo ENDOFCLEAR");
            MainActivity.getLastLine(shell_out, "ENDOFCLEAR");      //This will read up to the last line and stop, effectively clearing shell_out
            shell_out.reset();                                      //<--- since this doesn't seem to do the job
            shell_out_error.reset();
        }catch(IOException ignored){}
        free.add(this);
    }
    static Shell getFreeShell(){
        if(free.isEmpty()) return new Shell();
        else{
            Shell temp = free.get(0);
            free.remove(0);
            return temp;
        }
    }
    static void runOne(String cmd){
        Shell shell = getFreeShell();
        shell.run(cmd);
        shell.done();
    }
    static void exitAll(){
        for(int i=0;i<free.size();i++){
            free.get(i).run("exit");
        }
    }
}
