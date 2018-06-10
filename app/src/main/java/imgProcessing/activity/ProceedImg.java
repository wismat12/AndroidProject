package imgProcessing.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;

import imgProcessing.basic.Circle;
import imgProcessing.basic.Area;
import imgProcessing.config.Config;
import imgProcessing.detection.Detect;
import imgProcessing.representation.BasicProcessing;
import imgProcessing.representation.MyImage;
import imgProcessing.signArea.AreaDetection;
import imgProcessing.signArea.PatternChecker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ProceedImg {

    /* Briefly algorithm description
    1 Scaling (optional)
    2 HSV_redBinarization performed on 8 threads (countdownLatch)
    3 Computing areas - red objects on the image and theirs positions - if red color is detected, app'll play particular sound
      3.1 Preparing histograms by 8 threads
      3.2 Making Lines - by 2 threads
      3.2 and rectangles - there is no need to involve more than 1 thread
    4 croping source imgs after binariaztion and rgb to previously detected areas - bitmap crop operation is performed on 8 threads
    5 edges detecting on red objects - white/black edges as output - preformed on 8 threads
    6 looking for circles - if point belong to circle - So far programmed on 1 thread, - if circle is detected, app'll play particular sound
    7 marking circles on output img/frame
    8 croping every circle to inner one - we need only interior - preparing areas with black numbers
    9 croping rgb imgs to the same coordinates
    10 HSV_blackBinarization on rgb interior circles
    11 cleaning black pixels on the edges - cleaning edges
    12 detecting areas with only one number - we need only one digit per area
    13 comparing found digit with PatternBase
    14 if passed returned sign constraint in String
    15 getting GPS coordinates
    16 getting geocode request to google api in order to obtain formatted address (BlockingQueue)
    17 saving png file by different thread with marked sign, detected its speed limit and address from geocode api response.
     */

    public static class BitmapSaverThread extends Thread {

        private final MyImage img;
        private final Context context;
        private final File folder;
        private final String prefix;
        private final boolean requestLocation;
        private final  ArrayList<String> detectedl;

        public BitmapSaverThread(MyImage myImage, Context context, File mImageFolder, String prefix, boolean requestLocation,  ArrayList<String> detected) {
            this.img = myImage;
            this.context = context;
            this.folder = mImageFolder;
            this.prefix = prefix;
            this.requestLocation = requestLocation;
            this.detectedl = detected;
        }
        public void run() {

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            img.getImage().compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            String geocodeAddress = prefix;
            //String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            try {
                if(requestLocation){
                    Config.locationRequest = true;
                    String tmp = Config.GPS_GEOCODDED_LOCATION_RESPONSES.take();   //Blocking queue
                    geocodeAddress = "Found_";
                    for(String s: detectedl){
                        geocodeAddress+=s+"_";
                    }
                    geocodeAddress += tmp.replaceAll(" ", "_").replaceAll(",", "_").replaceAll("-", "_");
                }
                File imageFile = null;
                String mImageFileName;

                try {
                    imageFile = File.createTempFile(geocodeAddress,".png",folder);
                    //Toast.makeText(context,"Saving shot in proceed!", Toast.LENGTH_SHORT).show();
                    mImageFileName = imageFile.getAbsolutePath();

                    FileOutputStream fileOutputStream = null;
                    try {
                        fileOutputStream = new FileOutputStream(mImageFileName);

                        fileOutputStream.write(byteArray);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                        if(fileOutputStream != null){
                            try {
                                fileOutputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static ArrayList<String> detectSigns(byte[] bytes, Context context, File imageFolder, TextToSpeech tts, MediaPlayer circleSound, MediaPlayer redAreaSound){

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes , 0, bytes.length).copy(Bitmap.Config.RGB_565,true);

        int indexDigit = 0;
        ArrayList<String> detected = new ArrayList<>();

        MyImage baseImg = new MyImage(bitmap);

        //Scaling picture if needed
        if((baseImg.getImageWidth() > Config.MAX_WIDTH_IMG)||(baseImg.getImageHeight() > Config.MAX_HEIGHT_IMG)){
            baseImg.resizeImageObject(baseImg.getImageWidth()/2,baseImg.getImageHeight()/2);
        }

        MyImage rgbImgOut = new MyImage(baseImg);  //needed to create output

        BasicProcessing basic = new BasicProcessing();
        AreaDetection areaDetection = new AreaDetection();

        basic.HSV_redBinarization(baseImg);

        if(Config.SAVE_BITMAP_STEPS)
            saveBitmap(baseImg,context,imageFolder, "HSV_redBinarization",false, detected);

        ArrayList<Area> areas = areaDetection.detectAreas(baseImg, 1, Config.MIN_SIZE_AREA);

        //System.out.println("areas size: "+ areas.size());
        int index = 1;
        int indexR = 0;
        for(Area area : areas) {

            redAreaSound.start();

            MyImage newArea = new MyImage(baseImg);    // it's after red binarization - 0,255 value picture, needed to detecting edges end circles
            MyImage newAreaRGB = new MyImage(rgbImgOut);  //needed to hsv black binarization

            basic.crop(newArea, area.getLeftUpper().getX(), area.getLeftUpper().getY(),
                    area.getRigthUpper().getX() - area.getLeftUpper().getX(),
                    area.getRightLower().getY() - area.getRigthUpper().getY());

            if(Config.SAVE_BITMAP_STEPS)
                saveBitmap(newArea, context, imageFolder, "before_detect" + (index++),false, detected);


            basic.crop(newAreaRGB, area.getLeftUpper().getX(), area.getLeftUpper().getY(),
                    area.getRigthUpper().getX() - area.getLeftUpper().getX(),
                    area.getRightLower().getY() - area.getRigthUpper().getY());

            Detect detect = new Detect();
            detect.edge(newArea);

            if(Config.SAVE_BITMAP_STEPS)
                saveBitmap(newArea, context, imageFolder, "areaBlack" + (index++),false, detected);

            ArrayList<Circle> circles = AreaDetection.detectCircle(newArea,circleSound);

            //System.out.println("Centers size: " + circles.size());

           for (int i = 0; i < circles.size(); i++) {
               indexR++;
                MyImage newArea2 = new MyImage(newAreaRGB); //needed to extract black numbers

                rgbImgOut.markSignArea(newArea2, circles.get(i), 51, 255, 51, 3, tts); //placing mark around the sign on output image

               basic.crop(newArea2, circles.get(i).getS().getX() - circles.get(i).getR(),
                        circles.get(i).getS().getY() - circles.get(i).getR(),
                        circles.get(i).getR() * 2,
                        circles.get(i).getR() * 2);

               if(Config.SAVE_BITMAP_STEPS)
                   saveBitmap(newArea2, context, imageFolder, "outColor" + (indexR),false, detected);

                BasicProcessing.HSV_blackBinarization(newArea2);

                //TODO cleaning edges - operacja czyszczenia brzegow - to przyciecie ponizej pozwala na chwilowe obejscie tego czyli usuniecie czarnych pxow z rogow img - na 40% wysokości
               basic.crop(newArea2,0, (int)(newArea2.getImageHeight()*0.2), newArea2.getImageWidth(), newArea2.getImageHeight() - 2*(int)(newArea2.getImageHeight()*0.2));

                ArrayList<Area> digitsAreas = areaDetection.detectAreas(newArea2, 0, 8);   //return areas with one particular number

               if(Config.SAVE_BITMAP_STEPS)
                   saveBitmap(newArea2, context, imageFolder, "outBlackBinarization" + (indexR),false, detected);

                String tmp = "";
                //System.out.println("digitsAreas.size()" + digitsAreas.size());

                for (Area areaDigit : digitsAreas) {
                    indexDigit++;
                    tmp += String.valueOf(checkDigit(newArea2,areaDigit,context,imageFolder,indexDigit, detected));
                }
                if(!tmp.contains("e")&&!tmp.equals(""))
                    detected.add(tmp);
            }
        }
        if((detected.size()>0)&&(Config.SAVE_RESULT))
            saveBitmap(rgbImgOut, context, imageFolder, "result",true, detected);
        return detected;
    }

    private static String checkDigit(MyImage ImgToClone, Area area,  Context context, File mImageFolder, int indexDigit, ArrayList<String> detected){

        MyImage tmp = new MyImage(ImgToClone);

        BasicProcessing basic = new BasicProcessing();
        basic.crop(tmp, area.getLeftUpper().getX(), area.getLeftUpper().getY(),
                area.getRigthUpper().getX() - area.getLeftUpper().getX(),
                area.getRightLower().getY() - area.getRigthUpper().getY());

        tmp.resizeImageObject(20, 40);

        if(Config.SAVE_BITMAP_STEPS)
            saveBitmap(tmp,context,mImageFolder,"digit" + indexDigit, false, detected);

        return  String.valueOf(PatternChecker.recognisePattern(tmp));
    }

    private static void saveBitmap(MyImage myImage, Context context, File mImageFolder, String prefix, boolean requestLocation, ArrayList<String> detected){

        Thread saver = new BitmapSaverThread(myImage,context,mImageFolder,prefix,requestLocation, detected);
        saver.start();
    }
}