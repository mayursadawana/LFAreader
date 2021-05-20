package com.example.lfareader;


import android.os.Build;

import androidx.annotation.RequiresApi;


public class Pixel {
    private static final String TAG = "Pixel:";
    int x, y;
    double r,g,b,h,s,v,l,A,B;

    public Pixel(){
    }
    public void setPoint(int x,int y){
        this.x = x;
        this.y = y;
    }
    public void setRGB(int r, int g, int b){
        this.r = r;
        this.g = g;
        this.b = b;
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void setRGB(double[] rgb){
        this.r = rgb[0];
        this.g = rgb[1];
        this.b = rgb[2];
//        double[] RGB = new double[]{(double)this.r/255, (double)this.g/255, (double)this.b/255};
////        Log.d(TAG, "setRGB: RGB_inverse:"+Arrays.toString(RGB));
//        double Cmax =0;
//        for (int i=0;i<RGB.length;i++){
//            if(Cmax < RGB[i]){
//                Cmax = RGB[i];
//            }
//        }
//        double Cmin = 100;
//        for (int i=0;i<RGB.length;i++){
//            if(Cmin > RGB[i]){
//                Cmin = RGB[i];
//            }
//        }
//        this.v = (int)Cmax*100;
//        if(Cmax==0){
//            this.s = 0;
//        }
//        else{
//            this.s = (int)((Cmax-Cmin)*100/Cmax);
////            Log.d(TAG, "setRGB: "+this.s);
//        }
//        if((long)Cmax-(long)Cmin == 0){
//            this.h = 0;
//        }
//        else{
//            if(Cmax == RGB[0]){
//                double h = 60*(Math.floorMod(((long)RGB[1]-(long)RGB[0])/((long)Cmax-(long)Cmin),(long)6.0));
//                this.h = (int)h;
//            }
//            if(Cmax == RGB[1]){
//                double h = 60*(2+((RGB[2]-RGB[0])/(Cmax-Cmin)));
//                this.h = (int)h;
//            }
//            if(Cmax == RGB[2]){
//                double h = 60*(4+((RGB[0]-RGB[1])/(Cmax-Cmin)));
//                this.h = (int)h;
//            }
//        }
//        Log.d(TAG, "setRGB: hsv"+this.h+":"+this.s+":"+this.v);
    }
    public void setHSV(double[] hsv){
        this.h = hsv[0];
        this.s = hsv[1];
        this.v = hsv[2];
    }
    public String getDetails(){
        return "x:"+this.x+" y:"+this.y+" r:"+this.r+" g:"+this.g+" b:"+this.b+" h:"+this.h+" s:"+this.s+" v:"+this.v;
    }

    public void setLAB(int l, int A, int B){
        this.l = l;
        this.A = A;
        this.B = B;
    }
}
