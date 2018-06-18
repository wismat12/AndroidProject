package imgProcessing.representation;

import android.graphics.Bitmap;

import java.util.concurrent.CountDownLatch;

import imgProcessing.config.Config;

public class BasicProcessing {

    Bitmap bi;

    public class HSV_redBinarizationThread extends Thread {

        private final CountDownLatch stopLatch;
        private final int xStart;
        private final int xStop;
        private final int yStart;
        private final int yStop;
        private MyImage img;


        public HSV_redBinarizationThread(CountDownLatch stopLatch, MyImage img, int xStart, int xStop,  int yStart, int yStop) {
            this.stopLatch = stopLatch;
            this.xStart = xStart;
            this.yStart = yStart;
            this.xStop = xStop;
            this.yStop = yStop;
            this.img = img;
        }
        public void run() {
            try {
               // System.out.println("HSV RED BIN THREAD "+Thread.currentThread());
                for(int y = yStart; y < yStop; y++){
                    for(int x = xStart; x < xStop; x++){

                        int a = img.getAlpha(x, y);
                        int h = (int)(img.HSV_getHue(x, y));
                        int s = (int)((img.HSV_getSaturation(x, y)));
                        int v = (int)((img.HSV_getValue(x, y)));

                        boolean isRed = (((h >= 0) && (h <= 20)) || ((h >= 320) && (h <= 360))) &&
                                ((s >= 39) && (s <= 100)) &&
                                ((v >= 39) && (v <= 100));
                        if (isRed) {
                            img.setPixel(x, y, a, 0, 0, 0);
                        }else {
                            img.setPixel(x, y, a, 255, 255, 255);
                        }
                    }
                }
            } finally {
                stopLatch.countDown();
            }
        }
    }

    public class CropThread extends Thread {

        private final CountDownLatch stopLatch;
        private final int xStart;
        private final int xStop;
        private final int yStart;
        private final int yStop;
        private final int xBi;
        private final int yBi;
        private  MyImage img;


        public CropThread(CountDownLatch stopLatch, MyImage img, int xBi, int yBi, int xStart, int xStop,  int yStart, int yStop) {
            this.stopLatch = stopLatch;
            this.xStart = xStart;
            this.yStart = yStart;
            this.xStop = xStop;
            this.yStop = yStop;
            this.xBi = xBi;
            this.yBi = yBi;
            this.img = img;
        }
        public void run() {
            try {
                //System.out.println("CROP HREAD "+Thread.currentThread().getId());
                for(int sy = yStart, j = yBi; sy < yStop; sy++, j++){
                    for(int sx = xStart, i = xBi; sx < xStop; sx++, i++){
                    //  System.out.println(j);
                      bi.setPixel(i, j, img.getPixel(sx, sy));
                    }
                }
            } finally {
                stopLatch.countDown();
            }
        }
    }

    public void HSV_createHSVImage(MyImage img){
        for(int y = 0; y < img.getImageHeight(); y++){
            for(int x = 0; x < img.getImageWidth(); x++){

                int a = img.getAlpha(x, y);
                int h = (int)(img.HSV_getHue(x, y));
                int s = (int)((img.HSV_getSaturation(x, y)));
                int v = (int)((img.HSV_getValue(x, y)));

                img.setPixel(x, y, a, h, s, v);
            }
        }
    }

    /**
     * HSV red Binarization with default configuration
     *
     * @param img The image to proceed binarization.
     */
    public void HSV_redBinarization(MyImage img){

        CountDownLatch cdl = new CountDownLatch(8);

        Thread t1 = new HSV_redBinarizationThread(cdl,img,0,img.getImageWidth()/2,0,img.getImageHeight()/4);
        Thread t2 = new HSV_redBinarizationThread(cdl,img,img.getImageWidth()/2,img.getImageWidth(),0,img.getImageHeight()/4);
        Thread t3 = new HSV_redBinarizationThread(cdl,img,0,img.getImageWidth()/2,img.getImageHeight()/4,img.getImageHeight()/2);
        Thread t4 = new HSV_redBinarizationThread(cdl,img,img.getImageWidth()/2,img.getImageWidth(),img.getImageHeight()/4,img.getImageHeight()/2);


        Thread t5 = new HSV_redBinarizationThread(cdl,img,0,img.getImageWidth()/2,img.getImageHeight()/2,3*img.getImageHeight()/4);
        Thread t6 = new HSV_redBinarizationThread(cdl,img,img.getImageWidth()/2,img.getImageWidth(),img.getImageHeight()/2,3*img.getImageHeight()/4);
        Thread t7 = new HSV_redBinarizationThread(cdl,img,0,img.getImageWidth()/2,3*img.getImageHeight()/4,img.getImageHeight());
        Thread t8 = new HSV_redBinarizationThread(cdl,img,img.getImageWidth()/2,img.getImageWidth(),3*img.getImageHeight()/4,img.getImageHeight());

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
    }
    /**
     * HSV black Binarization with default threshold
     *
     * @param img The image to proceed binarization.
     */
    public static void HSV_blackBinarization(MyImage img){
        HSV_blackBinarization(img, Config.HSV_blackBinarization_TRESHOLD);
    }
    /**
     * HSV black Binarization with fixed threshold
     *
     * @param img The image to proceed binarization.
     * @param threshold The image to proceed binarization.
     */
    public static void HSV_blackBinarization(MyImage img, int threshold){

        for(int y = 0; y < img.getImageHeight(); y++){
            for(int x = 0; x < img.getImageWidth(); x++){

                int a = img.getAlpha(x, y);
                int v = (int)((img.HSV_getValue(x, y)));


                boolean isblack = ((v >= 0) && (v <= threshold));

                if (isblack) {
                    img.setPixel(x, y, a, 0, 0, 0);
                }else {
                    img.setPixel(x, y, a, 255, 255, 255);
                }
            }
        }
    }
    /**
     * Cropping img
     *
     * @param img The image to crop.
     * @param x The x coordinate from where cropping will start.
     * @param y The y coordinate from where cropping will start.
     * @param width The width of the new cropped image.
     * @param height The height of the new cropped image.
     */
    public void crop(MyImage img, int x, int y, int width, int height){
        img.setOldX(x);
        img.setOldY(y);
        bi = Bitmap.createBitmap(width, height, img.getImage().getConfig());


        CountDownLatch cdl = new CountDownLatch(8);
        /*
        CountDownLatch cdl = new CountDownLatch(1);
        Thread t1 = new CropThread(cdl,img,0,0,x,x+width,y,y+height);
        t1.start();*/

        Thread t1 = new CropThread(cdl,img,0,0,x,x+width/2,y,y+height/4);
        Thread t2 = new CropThread(cdl,img,width/2,0,x+width/2,x+width,y,y+height/4);
        Thread t3 = new CropThread(cdl,img,0,height/4, x,x+width/2,y+height/4,y+height/2);
        Thread t4 = new CropThread(cdl,img,width/2, height/4,x+width/2,x+width,y+height/4,y+height/2);


        Thread t5 = new CropThread(cdl,img,0,height/2,x,x+width/2,y+height/2,y+(3*height)/4);
        Thread t6 = new CropThread(cdl,img,width/2,height/2,x+width/2,x+width,y+height/2,y+(3*height)/4);
        Thread t7 = new CropThread(cdl,img,0,(3*height)/4,x,x+width/2,y+(3*height)/4,y+height);
        Thread t8 = new CropThread(cdl,img,width/2,(3*height)/4,x+width/2,x+width,y+(3*height)/4,y+height);

        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        t6.start();
        t7.start();
        t8.start();

        try {
           // System.out.println();
            cdl.await();
          //  System.out.println("after crop");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        img.modifyImageObject(width, height, bi);
    }

}
