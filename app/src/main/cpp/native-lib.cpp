#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <android/log.h>

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

void logd(char* str){
    __android_log_write(ANDROID_LOG_INFO, "CPP", str);
}

extern "C" jint Java_com_hijacker_Airodump_main(JNIEnv* env, jobject obj, jstring str, jint off){
    int i, j;
    char *buffer = (char*)malloc(512);
    const char *nativeString = env->GetStringUTFChars(str, 0);
    strcpy(buffer, nativeString);
    buffer[env->GetStringLength(str)]='\0';
    i=strlen(buffer)-1;
    while(buffer[i]==' ') i--;
    buffer[i+1]='\0';

    //__android_log_print(ANDROID_LOG_INFO, "CPP", "NULL @ [%d], buffer length=%d", env->GetStringLength(str), strlen(buffer));

    env->ReleaseStringUTFChars(str, nativeString);

    jclass jclass1 = env->FindClass("com/hijacker/Airodump");
    jmethodID method_ap = env->GetStaticMethodID(jclass1, "addAP", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIIII)V");
    jmethodID method_st = env->GetStaticMethodID(jclass1, "addST", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;III)V");

    if( buffer[3]==':' || buffer[3]=='o' ){
        //logd("Found ':' or 'o' @ 3");
        while(buffer[strlen(buffer)-1]=='\n'){
            buffer[strlen(buffer)-1] = '\0';
        }
        //Clear spaces
        for(i=123;i<strlen(buffer);i++){
            if(buffer[i]==' ' && buffer[i+1]==' '){
                for(j=i;j<strlen(buffer);j++){
                    buffer[j] = buffer[j+1];
                }
                i--;
            }
        }
        if(buffer[22]==':'){
            //logd("0         1         2         3         4         5         6");
            //logd("0123456789012345678901234567890123456789012345678901234567890");
            //logd(buffer);
            //st
            char st_mac[18], bssid[18], pwr_c[6], lost_c[7], frames_c[10], probes[100];
            int pwr, lost, frames;

            strncpy(st_mac, buffer+20, 17);
            st_mac[17]='\0';

            if(buffer[1]=='(') strcpy(bssid, "na"); //not associated
            else strncpy(bssid, buffer+1, 17);
            bssid[17]='\0';

            strncpy(pwr_c, buffer+37, 6);
            pwr_c[5]='\0';
            pwr = atoi(pwr_c);

            strncpy(lost_c, buffer+52, 6);
            lost_c[6]='\0';
            lost = atoi(lost_c);

            strncpy(frames_c, buffer+58, 9);
            frames_c[9]='\0';
            frames = atoi(frames_c);

            strncpy(probes, buffer+69, 100);
            probes[99] = '\0';

            jstring s1 = env->NewStringUTF(st_mac);
            jstring s2 = env->NewStringUTF(bssid);
            jstring s3 = env->NewStringUTF(probes);
            env->CallStaticVoidMethod(jclass1, method_st, s1, s2, s3, pwr, lost, frames);
        }else{
            //ap
            char bssid[18], pwr_c[6], beacons_c[10], data_c[10], ivs_c[6], ch_c[3], enc[5], cipher[5], auth[5], essid[50];
            int pwr, beacons, data, ivs, ch;

            strncpy(bssid, buffer+1, 17);
            bssid[17]='\0';

            strncpy(pwr_c, buffer+18, 5);
            pwr_c[5]='\0';
            pwr = atoi(pwr_c);

            //if off is not 0 then airodump-ng is running for a specific channel
            //so we need to bypass 4 characters after pwr to get the correct results because there is one extra column
            if(off!=0) buffer += 4;

            strncpy(beacons_c, buffer+23, 9);
            beacons_c[9]='\0';
            beacons = atoi(beacons_c);

            strncpy(data_c, buffer+32, 9);
            data_c[9]='\0';
            data = atoi(data_c);

            strncpy(ivs_c, buffer+41, 5);
            ivs_c[5]='\0';
            ivs = atoi(ivs_c);

            strncpy(ch_c, buffer+48, 2);
            ch_c[2]='\0';
            ch = atoi(ch_c);

            strncpy(enc, buffer+57, 4);
            if(enc[3]==' ') enc[3] = '\0';
            else enc[4] = '\0';

            strncpy(cipher, buffer+62, 4);
            cipher[4]='\0';

            strncpy(auth, buffer+69, 4);
            if(auth[3]==' ') auth[3] = '\0';
            else auth[4] = '\0';

            if(buffer[74]!='<'){
                strncpy(essid, buffer+74, 49);
            }else strcpy(essid, "<hidden>\0");
            essid[49]='\0';

            jstring s1 = env->NewStringUTF(essid);
            jstring s2 = env->NewStringUTF(bssid);
            jstring s3 = env->NewStringUTF(enc);
            jstring s4 = env->NewStringUTF(cipher);
            jstring s5 = env->NewStringUTF(auth);
            env->CallStaticVoidMethod(jclass1, method_ap, s1, s2, s3, s4, s5, pwr, beacons, data, ivs, ch);
        }
    }
    return 0;
}