package com.example.lfareader;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ImageProcessing {
    private static final String TAG = "IP class";
    private Bitmap rawImage;
    private Bitmap finalImage;
    private Mat rawMat;
    public List<Point> edges;
    public List<Point> midpoints;
    public List<Pixel> midline;
    public List<Pixel> padLine;
    public List<Pixel> grayPad;
    public List<Pixel> whitePad;

    public int colorspace;
    public int satCutoff;
    public int hueCutoffUpper;
    public int hueCutoffLower;
    private double sd = 10;

    public ImageProcessing(){
        this.edges = new ArrayList<>();
        this.midpoints = new ArrayList<>();
        this.midline = new ArrayList<>();
        this.padLine = new ArrayList<>();
        this.whitePad = new ArrayList<>();
        this.grayPad = new ArrayList<>();
        this.colorspace = 0;
        this.satCutoff = 25;
        this.hueCutoffUpper = 140;
        this.hueCutoffLower = 140;
    }

    public Mat imageToThresholdBW(Mat rawMat, int threshold){
        Mat mat = new Mat();
        Imgproc.cvtColor(rawMat, mat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(mat,mat,threshold,255,Imgproc.THRESH_BINARY);
        return mat;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public Mat setColorPads(Mat rawMat, Mat contMat){
        Mat hsvRaw = new Mat();
        Imgproc.cvtColor(rawMat,hsvRaw,Imgproc.COLOR_BGR2HSV);
        Point northwest = this.edges.get(0);
        Point northeast = this.edges.get(2);
        northwest.y = northwest.y - 30;
        northeast.y = northeast.y - 30;
        int expanse = (int)northeast.x -(int)northwest.x;
        int span = (int)((northeast.x-northwest.x)/ Math.abs(northeast.y- northwest.y));
        int tilt = (int)Math.abs(northeast.y- northwest.y);
        int slope = 0;
        if (northeast.y<northwest.y){
            slope = 1;
        }
        int startingX = (int)northwest.x;
        int yShift = 0;
        for(int i=0;i<tilt;i++){
            for (int j=0;j<span;j++){
                if(slope==1){
                    double[] rgb = rawMat.get((int)northwest.y-i,startingX+j);
                    Pixel pixel = new Pixel();
                    pixel.x = (int)northwest.y-i;
                    pixel.y = startingX+j;
                    pixel.setRGB(rgb);
                    pixel.setHSV(hsvRaw.get(pixel.x, pixel.y));
                    this.padLine.add(pixel);
                }
                else{
                    double[] rgb = rawMat.get((int)northwest.y+i,startingX+j);
                    Pixel pixel = new Pixel();
                    pixel.x = (int)northwest.y-i;
                    pixel.y = startingX+j;
                    pixel.setRGB(rgb);
                    pixel.setHSV(hsvRaw.get(pixel.x, pixel.y));
                    this.padLine.add(pixel);
                }
            }
            startingX = startingX+span;
        }

        double[] v = this.padLine.stream().mapToDouble(pixel->pixel.v).sorted().toArray();
        List<Point> peaks = findKDF(v);
        for(int i=0;i<this.padLine.size();i++){
            if(this.padLine.get(i).v > peaks.get(0).x-this.sd && this.padLine.get(i).v < peaks.get(0).x+this.sd){
//                contMat.put(this.padLine.get(i).x,this.padLine.get(i).y,255,0,255);
                this.whitePad.add(this.padLine.get(i));
            }
            if(this.padLine.get(i).v > peaks.get(1).x-this.sd && this.padLine.get(i).v < peaks.get(1).x+this.sd){
//                contMat.put(this.padLine.get(i).x,this.padLine.get(i).y,0,0,255);
                this.grayPad.add(this.padLine.get(i));
            }
        }
        this.grayPad.sort(Comparator.comparing(pixel -> pixel.y));
        this.whitePad.sort(Comparator.comparing(pixel -> pixel.y));
        Point grayMidPoint = new Point();
        grayMidPoint.x = this.grayPad.get((int)this.grayPad.size()/2).y;
        grayMidPoint.y = this.grayPad.get((int)this.grayPad.size()/2).x;
        Point whiteMidPoint = new Point();
        whiteMidPoint.x = this.whitePad.get((int)this.whitePad.size()/2).y;
        whiteMidPoint.y = this.whitePad.get((int)this.whitePad.size()/2).x;
        this.whitePad.clear();
        this.grayPad.clear();
        for (int i=-5;i<5;i++){
            for (int j=-5;j<5;j++){
                Pixel grayPixel = new Pixel();
                grayPixel.x = (int)grayMidPoint.x+i;
                grayPixel.y = (int)grayMidPoint.y+j;
                grayPixel.setRGB(rawMat.get(grayPixel.y, grayPixel.x));
                grayPixel.setHSV(hsvRaw.get(grayPixel.y, grayPixel.x));
                this.grayPad.add(grayPixel);
                Pixel whitePixel = new Pixel();
                whitePixel.x = (int)whiteMidPoint.x+i;
                whitePixel.y = (int)whiteMidPoint.y+j;
                whitePixel.setRGB(rawMat.get(whitePixel.y, whitePixel.x));
                whitePixel.setHSV(hsvRaw.get(whitePixel.y, whitePixel.x));
                this.whitePad.add(whitePixel);
            }
        }
        double averageVGray = this.grayPad.stream().mapToDouble(pixel-> pixel.v).average().getAsDouble();
        double averageVWhite = this.whitePad.stream().mapToDouble(pixel-> pixel.v).average().getAsDouble();
        Log.d(TAG, "getPadLine: v average: gray:"+averageVGray+" white:"+averageVWhite+" size of pads:"+this.whitePad.size());
        for (int i=0;i<this.whitePad.size();i++){
            contMat.put(this.whitePad.get(i).y,this.whitePad.get(i).x,255,0,0);
            contMat.put(this.grayPad.get(i).y,this.grayPad.get(i).x,0,0,255);
        }
        return contMat;
    }

    public List<Pixel> getGrayPad(){
        return this.grayPad;
    }

    public List<Pixel> getWhitePadPad(){
        return this.whitePad;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public double[] getGrayAverages(){
        return new double[]{this.grayPad.stream().mapToDouble(pixel->pixel.h).average().getAsDouble(),this.grayPad.stream().mapToDouble(pixel->pixel.s).average().getAsDouble(),this.grayPad.stream().mapToDouble(pixel->pixel.v).average().getAsDouble() };
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public double[] getWhiteAverages(){
        return new double[]{this.whitePad.stream().mapToDouble(pixel->pixel.h).average().getAsDouble(),this.whitePad.stream().mapToDouble(pixel->pixel.s).average().getAsDouble(),this.whitePad.stream().mapToDouble(pixel->pixel.v).average().getAsDouble() };
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<Point> findKDF(double[] values){
        List<Point> allV= new ArrayList<>();
        double constant1 = 1/(2*this.sd*Math.sqrt(2*Math.PI));
        double constant2 = 2*Math.pow(this.sd,2);
        for(int i=0;i<255;i++){
            Point point = new Point(i,0);
            allV.add(i,point);
        }
        for(int i=0;i<values.length;i++){
            for (int j=0;j<255;j++){
                double a = Math.pow(values[i]-j,2);
                double b = -1*a/constant2;
                double c = Math.exp(b);
                double d = c*constant1;
                allV.get(j).y = d+allV.get(j).y;
            }
        }
        List<Point> peaks = new ArrayList<>();
        double peak1 = allV.stream().mapToDouble(point->point.y).max().getAsDouble();
        allV.sort(Comparator.comparing(point -> point.y));
        double localMaxIndex = allV.get(allV.size()-1).x;
        double localMaxValue = allV.get(allV.size()-1).y;
        peaks.add(new Point(localMaxIndex,localMaxValue));
        for(int i = allV.size()-1;i>=0;i--){
                int finalI = i;
                if(peaks.stream().noneMatch(point -> allV.get(finalI).x > point.x - 2*this.sd && allV.get(finalI).x < point.x + 2*this.sd )){
                    if(allV.get(finalI).x>80-this.sd) {
                        peaks.add(new Point(allV.get(finalI).x, allV.get(finalI).y));
                    }
                }
        }
        peaks.sort(Comparator.comparing(point -> point.y));
        Collections.reverse(peaks);
        peaks = peaks.subList(0,3);
        peaks.sort(Comparator.comparing(point -> point.x));
        Collections.reverse(peaks);

        Log.d(TAG, "findKDF: "+Arrays.toString(peaks.toArray()));
        return peaks;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    public Mat applyContourDetection(Mat rawMat, Mat colorMat){
        int indexOfLargest=0;
        List<MatOfPoint> contours = new ArrayList<>();
        List<Double> cAreas = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(rawMat,contours,hierarchy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for(int i=0;i<contours.size();i++){
            cAreas.add(i,Imgproc.contourArea(contours.get(i)));
            Imgproc.drawContours(rawMat, contours, i, new Scalar(170,0,0), 2, 0, hierarchy, 0, new Point());
        }
        double c = Collections.max(cAreas);
        indexOfLargest = cAreas.indexOf(c);
        Log.d(TAG, "applyContourDetection: largest index:"+indexOfLargest + " with area:"+c);
        return markContourOnColoredMat(contours.get(indexOfLargest).toList(),colorMat);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    protected Mat markContourOnColoredMat(List<Point> points, Mat drawOn){

        markPoints(points,drawOn,2,255,255,255);

        Point bottomMostPoint = new Point();
        bottomMostPoint.y = points.stream().mapToDouble(point -> point.y).max().getAsDouble();

        Point bottomMostleft = new Point();
        bottomMostleft.y = bottomMostPoint.y;
        bottomMostleft.x = points.stream().filter(point-> point.y == bottomMostleft.y).mapToDouble(point-> point.x).min().getAsDouble();
        markPoint(bottomMostleft,drawOn,6,255,0,0);
        Point bottomMostright = new Point();
        bottomMostright.y = bottomMostPoint.y;
        bottomMostright.x = points.stream().filter(point-> point.y == bottomMostright.y).mapToDouble(point-> point.x).max().getAsDouble();
        markPoint(bottomMostright,drawOn,6,255,255,0);
        if(bottomMostleft.x>(int)drawOn.cols()/2 && bottomMostright.x>(int)drawOn.cols()/2){
            Log.d(TAG, "markContourOnColoredMat: bottom most right point is bottom most point and is the southeast point");
            bottomMostPoint.x = points.stream().filter(point -> point.y>bottomMostPoint.y-5).mapToDouble(point-> point.x).max().getAsDouble();
//            bottomMostPoint.x = bottomMostright.x;
        }
        if(bottomMostright.x<(int)drawOn.cols()/2 && bottomMostleft.x<(int)drawOn.cols()/2){
            Log.d(TAG, "markContourOnColoredMat: bottom most left point is the bottommost point and is the southwest point");
            bottomMostPoint.x = points.stream().filter(point -> point.y>bottomMostPoint.y-5).mapToDouble(point-> point.x).min().getAsDouble();
//            bottomMostPoint.x = bottomMostleft.x;
        }
        if(bottomMostright.x>(int)drawOn.cols()/2 && bottomMostleft.x<(int)drawOn.cols()/2){
            //dilemma..
            double leftmostx = points.stream().mapToDouble(point-> point.x).min().getAsDouble();
            double rightmostx = points.stream().mapToDouble(point-> point.x).max().getAsDouble();
            Log.d(TAG, "markContourOnColoredMat: "+leftmostx/bottomMostleft.x+" : "+rightmostx/bottomMostright.x);
            if(leftmostx/bottomMostleft.x<1){
                bottomMostPoint.x = bottomMostleft.x;
            }
            else{
                bottomMostPoint.x = bottomMostright.x;
            }
        }

//        bottomMostPoint.x = bottomMostleft.x;
        markPoint(bottomMostPoint,drawOn,8,0,255,255);



        Point southeast = new Point();
        Point southwest = new Point();
        Point northeast = new Point();
        Point northwest = new Point();


        if(bottomMostPoint.x > (int)drawOn.cols()/2){
            Log.d(TAG, "markContourOnColoredMat: downward slope bottommost point is southeast");
//            bottomMostPoint.y = points.stream().mapToDouble(point->point.y).max().getAsDouble();
//            bottomMostPoint.x = points.stream().filter(point->point.y==bottomMostPoint.y).mapToDouble(point->point.x).max().getAsDouble();

            southeast.x = bottomMostPoint.x;
            southeast.y = bottomMostPoint.y;
            Log.d(TAG, "markContourOnColoredMat: southeast:"+southeast.toString());
            markPoint(southeast,drawOn,4,255,0,0);
            // get smallest x from contour as that is the southwest point..
            southwest.x = points.stream().mapToDouble(point-> point.x).min().getAsDouble();
            southwest.y = points.stream().filter(point-> point.x < southwest.x+5).mapToDouble(point-> point.y).max().getAsDouble();
            Log.d(TAG, "markContourOnColoredMat: southwest:"+southwest.toString());
            markPoint(southwest,drawOn,4,255,255,0);
            // get largest x from contour as that is the northeast point..
            northeast.x = points.stream().mapToDouble(point-> point.x).max().getAsDouble();
            northeast.y = points.stream().filter(point-> point.x > northeast.x-5).mapToDouble(point-> point.y).min().getAsDouble();
            Log.d(TAG, "markContourOnColoredMat: northeast:"+northeast.toString());
            markPoint(northeast,drawOn,4,255,255,0);
            // get northwest from calculations..
            northwest.x = northeast.x-(southeast.x-southwest.x);
            northwest.y = northeast.y-(southeast.y-southwest.y);
            Log.d(TAG, "markContourOnColoredMat: northwest:"+northwest.toString());
            markPoint(northwest,drawOn,6,255,0,255);
        }
        else{
            Log.d(TAG, "markContourOnColoredMat: upward slope bottommost point is southwest");
            // reiterating from the bottommost point to get the left most point for southwest. might occur due to very else slope..
//            bottomMostPoint.y = points.stream().mapToDouble(point -> point.y).max().getAsDouble();
//            bottomMostPoint.x = points.stream().filter(point-> point.y == bottomMostPoint.y).mapToDouble(point-> point.x).min().getAsDouble();
//            markPoint(bottomMostPoint,drawOn,4,0,255,255);

            southwest.x = bottomMostPoint.x;
            southwest.y = bottomMostPoint.y;
            Log.d(TAG, "markContourOnColoredMat: southwest:"+southwest.toString());
            markPoint(southwest,drawOn,4,255,0,0);

            southeast.x = points.stream().mapToDouble(point-> point.x).max().getAsDouble();
            southeast.y = points.stream().filter(point-> point.x > southeast.x-5).mapToDouble(point-> point.y).max().getAsDouble();
            Log.d(TAG, "markContourOnColoredMat: southeast:"+southeast.toString());
            markPoint(southeast,drawOn,4,255,0,0);

            northwest.x = points.stream().mapToDouble(point-> point.x).min().getAsDouble();
            northwest.y = points.stream().filter(point-> point.x < northwest.x+5).mapToDouble(point-> point.y).min().getAsDouble();
            Log.d(TAG, "markContourOnColoredMat: northwest:"+northwest.toString());
            markPoint(northwest,drawOn,4,255,0,0);

            northeast.x = southeast.x- southwest.x + northwest.x;
            northeast.y = northwest.y - (southwest.y - southeast.y);
            Log.d(TAG, "markContourOnColoredMat: northeast:"+northeast.toString());
            markPoint(northeast,drawOn,4,255,0,0);

        }
        this.edges.add(northwest);
        this.edges.add(southwest);
        this.edges.add(northeast);
        this.edges.add(southeast);
        this.midpoints.add(new Point(Math.round(northwest.x/2+southwest.x/2),Math.round(northwest.y/2+ southwest.y/2)));
        this.midpoints.add(new Point(Math.round(southeast.x/2+northeast.x/2),Math.round(southeast.y/2+ northeast.y/2)));
        markPoints(this.midpoints,drawOn,4,127,127,127);
        return drawOn;
    }

    /**
     * Draws midline for the casette from the contour detected of the casette
     * @param rawMat input image matrix that the line will be drawn on
     * @return mat with the output image with the midline on the casette
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    protected Mat drawMidLineForCasette(Mat rawMat){
        Mat hsvRaw = new Mat();
        Imgproc.cvtColor(rawMat,hsvRaw,Imgproc.COLOR_RGB2HSV);
//        Log.d(TAG, "line length on x:"+(this.midpoints.get(1).x - this.midpoints.get(0).x));
//        Log.d(TAG, "line height on y:"+(Math.abs(this.midpoints.get(1).y - this.midpoints.get(0).y)));
        int expanse = (int)this.midpoints.get(1).x - (int)this.midpoints.get(0).x;
        int tilt = (int)Math.abs(this.midpoints.get(1).y - this.midpoints.get(0).y);
        int slope = 0;
        if(this.midpoints.get(1).y<this.midpoints.get(0).y){
            slope = 1;
        }
        int span = expanse/tilt;
        int startingX = (int)this.midpoints.get(0).x;
        int a=0;
        for (int j=0;j<tilt;j++){
            for(int i=0;i<span;i++){
                if(slope==1){
                    Pixel pixel = new Pixel();
                    pixel.setPoint((int)this.midpoints.get(0).y-j,startingX+i);
//                    double[] hsv = hsvRaw.get(pixel.x,pixel.y);
                    pixel.setHSV(hsvRaw.get(pixel.x,pixel.y));
                    double[] rgb = rawMat.get(pixel.x, pixel.y);
                    pixel.setRGB((int)rgb[0],(int)rgb[1],(int)rgb[2]);
                    this.midline.add(pixel);
                    rawMat.put(pixel.x,pixel.y, 0,255,255);
                }
                else{
                    Pixel pixel = new Pixel();
                    pixel.setPoint((int)this.midpoints.get(0).y+j,startingX+i);
//                    double[] hsv = hsvRaw.get(pixel.x,pixel.y);
                    pixel.setHSV(hsvRaw.get(pixel.x,pixel.y));
//                    double[] rgb = rawMat.get(pixel.x, pixel.y);
                    pixel.setRGB(rawMat.get(pixel.x, pixel.y));
                    this.midline.add(pixel);
                    rawMat.put(pixel.x, pixel.y,0,255,255);
                }
            }
            startingX=startingX+span;
        }
        for (int i =0;i<this.midline.size();i++){
            if(this.midline.get(i).s>this.satCutoff && this.midline.get(i).h>this.hueCutoffUpper){
//                Log.d(TAG, "drawMidLineForCasette: "+this.midline.get(i).getDetails());
                rawMat.put(this.midline.get(i).x,this.midline.get(i).y, 255,0,0);
            }
        }

        return rawMat;
    }

    private double getDistance(Pixel of, Pixel with, int usingRGB){
//        double distance=9999;
//            double rD = Math.pow(of.r- with.r,2);
//            double gD = Math.pow(of.g- with.g,2);
//            double bD = Math.pow(of.b- with.b,2);
//            return Math.sqrt(rD+gD+bD);
//            double hD = Math.pow(of.h- with.h,2);
            double sD = Math.pow(of.s- with.s,2);
            double vD = Math.pow(of.v- with.v,2);
            return   Math.sqrt(sD+vD);

    }

    public Mat loadImageToMat(String path){
        Mat rawMat = new Mat();
        Imgproc.cvtColor(Imgcodecs.imread(path,Imgcodecs.IMREAD_COLOR),rawMat,Imgproc.COLOR_BGR2RGB);
        return rawMat;
    }

    public Bitmap loadMatToBitmap(Mat mat){
        Bitmap bitmap = Bitmap.createBitmap(mat.width(),mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat,bitmap);
        return bitmap;
    }

    public Mat resizeMat(Mat rawMat, double resizeTo){
        Mat dst = new Mat();
//        int actualWidth = rawMat.width();
//        int actualHeight = rawMat.height();
//        int resizeWidth = (int) Math.round(actualWidth * resizeTo);
//        int resizeHeight = (int) Math.round(actualHeight * resizeTo);
        Imgproc.resize(rawMat,dst,new Size(),resizeTo,resizeTo);
        return dst;
    }

    private double minDistanceWithGround(List<Pixel> ground, Pixel point){
        double minDistance = 999999;
        for(int i=0;i<ground.size();i++){
            double distance = getDistance(ground.get(i),point,1);
            if(distance<minDistance){
                minDistance = distance;
            }
        }
        return minDistance;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public ArrayList<List<Pixel>> getForegrounds(List<Pixel> bg, ArrayList<List<Pixel>> lines, Mat rawMat){
        Mat hsvMat = new Mat();
        Imgproc.cvtColor(rawMat,hsvMat,Imgproc.COLOR_RGB2HSV);
        ArrayList<List<Pixel>> foreground = new ArrayList<>();
        for(int i=0;i<lines.size();i++){
            foreground.add(i,new ArrayList<>());
            for(int j=0;j<lines.get(i).size();j++){
                foreground.get(i).add(j,lines.get(i).get(j));
            }
        }

        for(int i=0;i<foreground.size();i++){
            for(int j=0;j<foreground.get(i).size();j++){
                // search for up, down, right, left pixels whether they belong to bg or fg..
                double minDistWithBG;
                double minDistWithFG;
                Pixel topPixel = new Pixel();
                topPixel.x = foreground.get(i).get(j).x;
                topPixel.y = foreground.get(i).get(j).y-1;
                topPixel.setRGB(rawMat.get(topPixel.x, topPixel.y));
                topPixel.setHSV(hsvMat.get(topPixel.x, topPixel.y));
                if(topPixel.s>this.satCutoff) {
                    minDistWithBG = minDistanceWithGround(bg, topPixel);
                    minDistWithFG = minDistanceWithGround(foreground.get(i), topPixel);
                    if (minDistWithFG < minDistWithBG) {
                        foreground.get(i).add(topPixel);
                        rawMat.put(topPixel.x, topPixel.y, 255, 255, 255);
                        hsvMat.put(topPixel.x, topPixel.y, 0, 0, 255);
                    }
                }
                Pixel bottomPixel = new Pixel();
                bottomPixel.x = foreground.get(i).get(j).x;
                bottomPixel.y = foreground.get(i).get(j).y+1;
                bottomPixel.setRGB(rawMat.get(bottomPixel.x, bottomPixel.y));
                bottomPixel.setHSV(hsvMat.get(bottomPixel.x, bottomPixel.y));
                if(bottomPixel.s>this.satCutoff) {
                    minDistWithBG = minDistanceWithGround(bg, bottomPixel);
                    minDistWithFG = minDistanceWithGround(foreground.get(i), bottomPixel);
//                    Log.d(TAG, "bottomPixel distances:bg: "+minDistWithBG+" fg: "+minDistWithFG);
                    if (minDistWithFG < minDistWithBG) {
                        foreground.get(i).add(bottomPixel);
                        rawMat.put(bottomPixel.x, bottomPixel.y, 255, 255, 255);
                        hsvMat.put(bottomPixel.x, bottomPixel.y, 0, 0, 255);
                    }
                }
                Pixel leftPixel = new Pixel();
                leftPixel.x = foreground.get(i).get(j).x-1;
                leftPixel.y = foreground.get(i).get(j).y;
                leftPixel.setRGB(rawMat.get(leftPixel.x, leftPixel.y));
                leftPixel.setHSV(hsvMat.get(leftPixel.x, leftPixel.y));
                if(leftPixel.s>this.satCutoff) {
                    minDistWithBG = minDistanceWithGround(bg, leftPixel);
                    minDistWithFG = minDistanceWithGround(foreground.get(i), leftPixel);
//                Log.d(TAG, "leftPixel distances:bg: "+minDistWithBG+" fg: "+minDistWithFG);
                    if (minDistWithFG < minDistWithBG) {
                        foreground.get(i).add(leftPixel);
                        rawMat.put(leftPixel.x, leftPixel.y, 255, 255, 255);
                        hsvMat.put(leftPixel.x, leftPixel.y, 0, 0, 255);
                    }
                }
                Pixel rightPixel = new Pixel();
                rightPixel.x = foreground.get(i).get(j).x+1;
                rightPixel.y = foreground.get(i).get(j).y;
                rightPixel.setRGB(rawMat.get(rightPixel.x, rightPixel.y));
                rightPixel.setHSV(hsvMat.get(rightPixel.x, rightPixel.y));
                if(rightPixel.s>this.satCutoff) {
                    minDistWithBG = minDistanceWithGround(bg,rightPixel);
                    minDistWithFG = minDistanceWithGround(foreground.get(i), rightPixel);
//                  Log.d(TAG, "rightPixel distances:bg: "+minDistWithBG+" fg: "+minDistWithFG);
                    if(minDistWithFG<minDistWithBG){
                        foreground.get(i).add(rightPixel);
                        rawMat.put(rightPixel.x, rightPixel.y,255,255,255);
                        hsvMat.put(rightPixel.x, rightPixel.y,0,0,255);
                    }
                }
            }
        }
        return foreground;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List<Pixel> setBackground(Mat rawMat){
        List<Pixel> bg = new ArrayList<>();
        for (int i =0;i<this.midline.size();i++){
            if(this.midline.get(i).s<=this.satCutoff && this.midline.get(i).h<=this.hueCutoffUpper){
                bg.add(this.midline.get(i));
            }
        }
        return bg;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public ArrayList<List<Pixel>> getAllLines(Mat rawMat){
        ArrayList<List<Pixel>> lines = new ArrayList<>();
        List<Pixel> line = new ArrayList<>();
        int newLine = 0;
        for(int i = 0;i<this.midline.size();i++){
//            Log.d(TAG, "getAllLines: Point in question: "+this.midline.get(i).getDetails());
            if(line.size()>0){
                if(this.midline.get(i).y - line.get(line.size()-1).y>1){
                    newLine=1;
                }
            }
            if(this.midline.get(i).s>this.satCutoff && this.midline.get(i).h>this.hueCutoffUpper && newLine==0){
                line.add(this.midline.get(i));
            }
            if(newLine==1){
                lines.add(new ArrayList<>(line));
                line.clear();
                newLine=0;
            }
        }
        return lines;
    }

    public Mat applyGB(int size, int sigma, Mat rawMat){
            Mat mat = new Mat();
            Imgproc.GaussianBlur(rawMat,mat,new Size(size,size),sigma);
            return mat;
    }

    public Mat erode(Mat mat, int iterations){
        Imgproc.erode(mat, mat, new Mat(),new Point(0,0),iterations);
        return mat;
    }

    public Mat dilate(Mat mat,int iterations){
        Imgproc.dilate(mat,mat,new Mat(),new Point(0,0),iterations);
        return mat;
    }

    protected void markPoints(List<Point> points,Mat drawOn,int size, double red, double green, double blue){
//        Log.d(TAG, "markPoints with color:"+red+":"+green+":"+blue);
        for(int i=0;i<points.size();i++){
//            Log.d(TAG, "markPoints: x:"+points.get(i).x+"y:"+points.get(i).y);
            for (int a=-size;a<size;a++){
                for (int b=-size;b<size;b++){
                    drawOn.put((int)points.get(i).y+a,(int)points.get(i).x+b, red, green, blue);
                }
            }
        }
    }

    protected void markPoint(Point point,Mat drawOn,int size, double red, double green, double blue){
//        Log.d(TAG, "markPoint with color:"+red+":"+green+":"+blue);
        for (int a=-size;a<size;a++){
            for (int b=-size;b<size;b++){
                drawOn.put((int)point.y+a,(int)point.x+b, red, green, blue);
            }
        }

    }

    public int[] getAverageOfLine(List<Pixel> points){
        double r=0,g=0,b=0,h=0,s=0,v=0;
       for(int i=0;i<points.size();i++){
           r = r+ points.get(i).r;
           b = b+ points.get(i).b;
           g = g+ points.get(i).g;
           h = h+ points.get(i).h;
           s = s+ points.get(i).s;
           v = v+ points.get(i).v;
       }
        int R = (int)(r/points.size());
        int G = (int)(g/points.size());
        int B = (int)(b/points.size());
        int H = (int)(h/points.size());
        int S = (int)(s/points.size());
        int V = (int)(v/points.size());
        return new int[]{R,G,B,H,S,V};
    }

    public double[] getAverageAndStdOfLine(List<Pixel> points){
        double r=0,g=0,b=0,h=0,s=0,v=0,sdR=0,sdG=0,sdB=0,sdH=0,sdS=0,sdV=0;
        for(int i=0;i<points.size();i++){
            r = r+ points.get(i).r;
            b = b+ points.get(i).b;
            g = g+ points.get(i).g;
            h = h+ points.get(i).h;
            s = s+ points.get(i).s;
            v = v+ points.get(i).v;
        }
        r=r/points.size();g=g/points.size();b=b/points.size();
        h=h/points.size();s=s/points.size();v=v/points.size();
        double sdSumR=0,sdSumG=0,sdSumB=0,sdSumH=0,sdSumS=0,sdSumV=0;
        for(int i=0;i<points.size();i++){
            sdR = sdR + Math.pow((points.get(i).r-r),2);
            sdG = sdG + Math.pow((points.get(i).g-g),2);
            sdB = sdB + Math.pow((points.get(i).b-b),2);
            sdH = sdH + Math.pow((points.get(i).h-h),2);
            sdS = sdS + Math.pow((points.get(i).s-s),2);
            sdV = sdV + Math.pow((points.get(i).v-v),2);
        }
        sdR = Math.sqrt(sdR);
        sdG = Math.sqrt(sdG);
        sdB = Math.sqrt(sdB);
        sdH = Math.sqrt(sdH);
        sdS = Math.sqrt(sdS);
        sdV = Math.sqrt(sdV);

        return new double[]{r,g,b,sdR,sdG,sdB,h,s,v,sdH,sdS,sdV};
    }

}
