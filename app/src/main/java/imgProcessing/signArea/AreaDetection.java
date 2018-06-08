package imgProcessing.signArea;

import android.media.MediaPlayer;
import android.renderscript.ScriptIntrinsicYuvToRGB;

import imgProcessing.basic.*;

import imgProcessing.config.Config;
import imgProcessing.representation.BasicProcessing;
import imgProcessing.representation.MyImage;

import java.util.ArrayList;
import java.lang.Math.*;
import java.util.concurrent.CountDownLatch;

public class AreaDetection {

    int[] histogramY;
    int[] histogramX;
    ArrayList<Line> linesX;
    ArrayList<Line> linesY;
    ArrayList<Area> areas;

    public class HistogramThread extends Thread {

        private final CountDownLatch stopLatch;
        private final int xStart;
        private final int xStop;
        private final int yStart;
        private final int yStop;
        private final MyImage img;

        public HistogramThread(CountDownLatch stopLatch, MyImage img, int xStart, int xStop,  int yStart, int yStop) {
            this.stopLatch = stopLatch;
            this.xStart = xStart;
            this.yStart = yStart;
            this.xStop = xStop;
            this.yStop = yStop;
            this.img = img;
        }
        public void run() {
            try {
               // System.out.println("HistogramThread"+Thread.currentThread());
                for(int y = yStart; y < yStop; y++){
                    for(int x = xStart; x < xStop; x++){

                        if(img.getRed(x,y) == 0){
                            histogramY[y]++;
                            histogramX[x]++;
                        }
                    }
                }
            } finally {
                stopLatch.countDown();
            }
        }
    }
    public class LineXThread extends Thread {

        private final CountDownLatch stopLatch;
        private final MyImage img;

        public LineXThread(CountDownLatch stopLatch, MyImage img) {
            this.stopLatch = stopLatch;
            this.img = img;
        }
        public void run() {
            try {
                boolean isobjX = false;
                Line newLine = new Line();
                int x = 0;
                while(x < img.getImageWidth()){

                    if(histogramX[x]>1){
                        if(!isobjX){
                            //groupsAmountX++;
                            newLine.getBegin().setX(x);
                            isobjX=true;
                        }

                    }else{
                        if(isobjX) {
                            newLine.getEnd().setX(x);
                            linesX.add(newLine);
                            newLine = new Line();
                            isobjX = false;
                        }
                    }
                    x++;
                }
                if(isobjX){
                    newLine.getEnd().setX(x-1);
                    linesX.add(newLine);
                }
                //System.out.println("LineXThread"+Thread.currentThread());
            } finally {
                stopLatch.countDown();
            }
        }
    }
    public class LineYThread extends Thread {

        private final CountDownLatch stopLatch;
        private final MyImage img;

        public LineYThread(CountDownLatch stopLatch, MyImage img) {
            this.stopLatch = stopLatch;
            this.img = img;
        }
        public void run() {
            try {
                boolean isobjY = false;
                Line newLine = new Line();
                int y = 0;
                while( y < img.getImageHeight()){

                    if(histogramY[y]>1){
                        if(!isobjY){
                            //groupsAmountY++;
                            newLine.getBegin().setY(y);
                            isobjY=true;
                        }

                    }else{
                        if(isobjY) {
                            newLine.getEnd().setY(y);
                            linesY.add(newLine);
                            newLine = new Line();
                            isobjY = false;
                        }
                    }
                    y++;
                }
                if(isobjY){
                    newLine.getEnd().setY(y-1);
                    linesY.add(newLine);
                }
               // System.out.println("LineYThread"+Thread.currentThread());
            } finally {
                stopLatch.countDown();
            }
        }
    }


    static boolean isCenterOfCircle(int row, int col, int r, MyImage image, MediaPlayer mp) {

        int pointsOnCircle = 0;
        for(int t : Config.POINTS_ON_CIRCLE_DEGREES){
            int xp = (int)(row + r * Math.cos(t * Math.PI / 180));
            int yp = (int)(col + r * Math.sin(t * Math.PI / 180));

            if(image.getRed(xp,yp)>60){
                if((Math.sqrt(Math.pow(xp-row,2) + Math.pow(yp-col,2)) >= r - 1)&&(Math.sqrt(Math.pow(xp-row,2) + Math.pow(yp-col,2)) <= r + 1)){
                    pointsOnCircle++;
                }
            }
        }
        if(pointsOnCircle >= Config.VALID_POINTS_ON_CIRCLE_AMOUNT) {
            mp.start();
            System.out.println("Center detected! x"+row + " y" + col + " points "+ pointsOnCircle + " r "+ r);
            return true;
        }
       return false;
    }


    public static ArrayList<Circle> detectCircle(MyImage img, MediaPlayer mp) {
        /* https://en.wikipedia.org/wiki/Circle_Hough_Transform */

        ArrayList<Circle> circles = new ArrayList<>();
        //Circle lastCircle = null;
        int rmax;
        int imgHeight = img.getImageHeight();
        int imgWidth = img.getImageWidth();
        if(imgHeight > imgWidth){
            rmax = (img.getImageWidth()/2);
        }else {
            rmax = (img.getImageHeight()/2);
        }

        for(int y = rmax - 2 ; y < imgHeight - rmax; y++){
            for(int x = rmax - 2 ; x < imgWidth - rmax; x++){
                for(int r = Config.MIN_SIZE_CIRCLE_RADIUS; r < rmax ; r++){

                    if((x - r < 0)||(x + r >=imgWidth))
                        break;
                    if((y - r < 0)||(y + r >=imgHeight))
                        break;

                    if(isCenterOfCircle(x,y,r,img,mp)){
                        Circle c = new Circle(new Point(x,y),r);
                        circles.add(c);
                        return circles;
                    }
                }
            }
        }
        return circles;
    }

    @SuppressWarnings("Duplicates")
    public ArrayList<Area> detectAreas(MyImage img, int treshold, int minSize){

        histogramY = new int[img.getImageHeight()];
        histogramX = new int[img.getImageWidth()];


        boolean isobjY = false;

        linesX = new ArrayList<>();
        linesY = new ArrayList<>();
        areas = new ArrayList<>();

        CountDownLatch cdl = new CountDownLatch(8);
        Thread t1 = new HistogramThread(cdl,img,0,img.getImageWidth()/2,0,img.getImageHeight()/4);
        Thread t2 = new HistogramThread(cdl,img,img.getImageWidth()/2,img.getImageWidth(),0,img.getImageHeight()/4);
        Thread t3 = new HistogramThread(cdl,img,0,img.getImageWidth()/2,img.getImageHeight()/4,img.getImageHeight()/2);
        Thread t4 = new HistogramThread(cdl,img,img.getImageWidth()/2,img.getImageWidth(),img.getImageHeight()/4,img.getImageHeight()/2);


        Thread t5 = new HistogramThread(cdl,img,0,img.getImageWidth()/2,img.getImageHeight()/2,3*img.getImageHeight()/4);
        Thread t6 = new HistogramThread(cdl,img,img.getImageWidth()/2,img.getImageWidth(),img.getImageHeight()/2,3*img.getImageHeight()/4);
        Thread t7 = new HistogramThread(cdl,img,0,img.getImageWidth()/2,3*img.getImageHeight()/4,img.getImageHeight());
        Thread t8 = new HistogramThread(cdl,img,img.getImageWidth()/2,img.getImageWidth(),3*img.getImageHeight()/4,img.getImageHeight());

        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        t6.start();
        t7.start();
        t8.start();
        try {
            cdl.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        CountDownLatch cdl2 = new CountDownLatch(2);
        Thread lineX = new LineXThread(cdl2,img);
        Thread lineY = new LineYThread(cdl2,img);
        lineX.start();
        lineY.start();
        try {
            cdl2.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for(Line lx: linesX){
            for(Line ly: linesY){
                if((lx.getEnd().getX() - lx.getBegin().getX() >= minSize)&&(ly.getEnd().getY() - ly.getBegin().getY() >= minSize)){
                    Area area = new Area();
                    area.getLeftUpper().setX((lx.getBegin().getX()-treshold) >= 0 ? (lx.getBegin().getX()-treshold):0);
                    area.getLeftUpper().setY((ly.getBegin().getY()-treshold) >= 0 ? (ly.getBegin().getY()-treshold):0);

                    area.getLeftLower().setX((lx.getBegin().getX()-treshold) >= 0 ? (lx.getBegin().getX()-treshold):0);
                    area.getLeftLower().setY((ly.getEnd().getY()+treshold) < img.getImageHeight()-1 ? (ly.getEnd().getY()+treshold):ly.getEnd().getY());

                    area.getRigthUpper().setX((lx.getEnd().getX()+treshold) < img.getImageWidth()-1 ? (lx.getEnd().getX()+treshold):lx.getEnd().getX());
                    area.getRigthUpper().setY((ly.getBegin().getY()-treshold) >= 0 ? (ly.getBegin().getY()-treshold):0);

                    area.getRightLower().setX((lx.getEnd().getX()+treshold) < img.getImageWidth()-1 ? (lx.getEnd().getX()+treshold):lx.getEnd().getX());
                    area.getRightLower().setY((ly.getEnd().getY()+treshold) < img.getImageHeight()-1 ? (ly.getEnd().getY()+treshold):ly.getEnd().getY());

                    /*System.out.println(area.getLeftUpper());
                    System.out.println(area.getLeftLower());
                    System.out.println(area.getRigthUpper());
                    System.out.println(area.getRightLower());*/
                    areas.add(area);
                }
            }
        }
        return areas;
    }
}
