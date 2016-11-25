package com.hijacker;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.hijacker.MainActivity.path;
import static com.hijacker.MainActivity.shell;
import static com.hijacker.MainActivity.shell3_in;
import static com.hijacker.MainActivity.su_thread;

public class InstallToolsDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.install_tools_title);
        builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //return
            }
        });
        builder.setItems(R.array.install_locations, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(shell==null){
                    su_thread.start();
                    try{
                        //Wait for su shells to spawn
                        su_thread.join();
                    }catch(InterruptedException ignored){}
                }
                String dest = "/su/xbin";
                switch(which){
                    case 0:
                        dest = "/system/bin";
                        break;
                    case 1:
                        dest = "/system/xbin";
                        break;
                    case 2:
                        dest = "/su/bin";
                        break;
                    case 3:
                        dest = "/su/xbin";
                        break;
                }
                extract("airbase-ng", dest);
                extract("aircrack-ng", dest);
                extract("aireplay-ng", dest);
                extract("airodump-ng", dest);
                extract("besside-ng", dest);
                extract("ivstools", dest);
                extract("iw", dest);
                extract("iwconfig", dest);
                extract("iwlist", dest);
                extract("iwpriv", dest);
                extract("kstats", dest);
                extract("makeivs-ng", dest);
                extract("mdk3", dest);
                extract("nc", dest);
                extract("packetforge-ng", dest);
                extract("wesside-ng", dest);
                extract("wpaclean", dest);
                extract("libfakeioctl.so", "/vendor/lib");
                dismiss();
                Toast.makeText(getActivity().getApplicationContext(), "Installed tools at " + dest, Toast.LENGTH_LONG).show();
            }
        });
        return builder.create();
    }
    void extract(String filename, String dest){
        File f = new File(path, filename);      //no permissions to write at dest
        dest = dest + '/' + filename;
        if(!f.exists()){
            try{
                InputStream in = getResources().getAssets().open(filename);
                FileOutputStream out = new FileOutputStream(f);
                byte[] buf = new byte[1024];
                int len;
                while((len = in.read(buf))>0){
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                shell3_in.print("mv " + path + '/' + filename + " " + dest + '\n');
                shell3_in.print("chmod 755 " + dest + '\n');
                shell3_in.flush();
            }catch(IOException e){
                Log.e("FileProvider", "Exception copying from assets", e);
            }
        }
    }
}
