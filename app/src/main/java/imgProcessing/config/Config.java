package imgProcessing.config;

import android.content.res.Resources;
import android.net.Uri;

import imgProcessing.representation.MyImage;
import pl.agh.roadsigns.camera2detector.R;

import java.util.ArrayList;

public class Config {

    public static final boolean SAVE_BITMAP_STEPS = false;
    public static final boolean SAVE_RESULT = false;

    public static final int MAX_WIDTH_IMG = 1920;
    public static final int MAX_HEIGHT_IMG = 1080;

    /* For Circle detection */
    public static final int MIN_SIZE_CIRCLE_RADIUS = 40;

    public static final int POINTS_ON_CIRCLE_AMOUNT = 18;
    public static final int VALID_POINTS_ON_CIRCLE_AMOUNT = 10;
    public static final int[] POINTS_ON_CIRCLE_DEGREES = new int[POINTS_ON_CIRCLE_AMOUNT];

    public static final int HSV_blackBinarization_TRESHOLD = 22;  //0-100 Value from HSV 0 -black


    /* For Pattern checking 0 - 100[%] 0 - img is the same, 100 - is completely different*/
    public static final int INEQUALITY_PERCENTAGE = 25;

    public static final int MIN_SIZE_AREA = 82;

    public static final int MAX_SIZE_AREA = 200;

    public static final MyImage[] PATTERN_BASE = new MyImage[10];

    public static void prepare(Resources res){

        MyImage digit0 = new MyImage();
        MyImage digit1 = new MyImage();
        MyImage digit2 = new MyImage();
        MyImage digit3 = new MyImage();
        MyImage digit4 = new MyImage();
        MyImage digit5 = new MyImage();
        MyImage digit6 = new MyImage();
        MyImage digit7 = new MyImage();
        MyImage digit8 = new MyImage();
        MyImage digit9 = new MyImage();
        digit0.readImage(res , R.drawable.digit0);
        digit1.readImage(res , R.drawable.digit1);
        digit2.readImage(res , R.drawable.digit2);
        digit3.readImage(res , R.drawable.digit3);
        digit4.readImage(res , R.drawable.digit4);
        digit5.readImage(res , R.drawable.digit5);
        digit6.readImage(res , R.drawable.digit6);
        digit7.readImage(res , R.drawable.digit7);
        digit8.readImage(res , R.drawable.digit8);
        digit9.readImage(res , R.drawable.digit9);

        PATTERN_BASE[0] = digit0;
        PATTERN_BASE[1] = digit1;
        PATTERN_BASE[2] = digit2;
        PATTERN_BASE[3] = digit3;
        PATTERN_BASE[4] = digit4;
        PATTERN_BASE[5] = digit5;
        PATTERN_BASE[6] = digit6;
        PATTERN_BASE[7] = digit7;
        PATTERN_BASE[8] = digit8;
        PATTERN_BASE[9] = digit9;

    }

    static{


        double degreeRange = 360/POINTS_ON_CIRCLE_AMOUNT;
        for(int i = 0; i < POINTS_ON_CIRCLE_AMOUNT; i++){
            POINTS_ON_CIRCLE_DEGREES[i] =(int)( i * degreeRange);
        }

    }

}
