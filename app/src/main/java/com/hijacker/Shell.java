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
/*
    Class to handle su shells

    It starts a shell when needed, and when it's no longer needed (done() has been called for it)
    its output is reset and it's saved to be used later, when a new one is needed
    without starting a new one.

    To get a Shell call getFreeShell();
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
    private BufferedReader shell_out;
    private static final List<Shell> free = new ArrayList<>();
    private static int total=0;
    private boolean valid = true;
    Shell(){
        total++;
        try{
            ProcessBuilder temp = new ProcessBuilder("su");
            temp.redirectErrorStream(true);
            shell = temp.start();
            shell_in = new PrintWriter(shell.getOutputStream());
            shell_out = new BufferedReader(new InputStreamReader(shell.getInputStream()));
        }catch(IOException e){
            Log.e("HIJACKER/Shell", "Error opening shell");
        }
        if(debug) Log.d("HIJACKER/Shell", "New shell: total=" + total + " free:" + free.size());
        if(free.size()>5) exitAll();
    }
    BufferedReader getShell_out(){ return this.shell_out; }
    Process getShell(){ return this.shell; }
    void run(String cmd){
        if(!valid){
            throw new IllegalStateException("Shell has been registered as free");
        }
        this.shell_in.print(cmd + '\n');
        this.shell_in.flush();
    }
    void done(){
        if(!valid){
            throw new IllegalStateException("Shell has already been registered as free");
        }
        String term_str = "ENDOFCLEAR" + System.currentTimeMillis();    //Use unique string
        run("echo; echo " + term_str);
        MainActivity.getLastLine(shell_out, term_str);      //This will read up to the last line and stop, effectively clearing shell_out
        synchronized(free){
            if(!free.contains(this)) free.add(this);
            valid = false;
        }
    }
    static synchronized Shell getFreeShell(){
        if(free.isEmpty()) return new Shell();
        else{
            Shell temp = free.get(0);
            free.remove(0);
            temp.valid = true;
            return temp;
        }
    }
    static void runOne(String cmd){
        Shell shell = getFreeShell();
        shell.run(cmd);
        shell.done();
    }
    static void exitAll(){
        total -= free.size();
        for(int i=0;i<free.size();i++){
            free.get(i).valid = true;
            free.get(i).run("exit");
            free.get(i).shell.destroy();
        }
    }
}
