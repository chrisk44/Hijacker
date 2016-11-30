package com.hijacker;

import static com.hijacker.IsolatedFragment.is_ap;

class Item2 {
    int i1, i2, i3, i4 ,i5;
    boolean type;
    String str1, str2, str3, str4, str5;
    Item2(String str1, String str2, String str3, String str4, String str5, int i1, int i2, int i3, int i4, int i5){
        //AP
        this.str1 = str1;
        this.str2 = str2;
        this.str3 = str3;
        this.str4 = str4;
        this.str5 = str5;
        this.i1 = i1;
        this.i2 = i2;
        this.i3 = i3;
        this.i4 = i4;
        this.i5 = i5;
        this.type = true;
    }
    Item2(String str1, String str2, int i1, int i2, int i3){
        //ST
        this.str1 = str1;
        this.str2 = str2;
        this.i1 = i1;
        this.i2 = i2;
        this.i3 = i3;
        this.type = false;
    }
    void add(){
        if(this.type){
            //AP
            AP temp = AP.getAPByMac(str2);
            if(is_ap==null || temp==is_ap){
                if(temp==null) new AP(str1, str2, str3, str4, str5, i1, i2, i3, i4, i5);
                else temp.update(str1, str3, str4, str5, i1, i2, i3, i4, i5);
            }
        }else{
            //ST
            ST temp = ST.getSTByMac(str1);
            if (str2.equals("na")) str2=null;
            if (temp == null) new ST(str1, str2, i1, i2, i3);
            else temp.update(str2, i1, i2, i3);
        }
    }
}
