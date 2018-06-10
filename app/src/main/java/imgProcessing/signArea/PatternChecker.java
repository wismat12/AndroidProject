package imgProcessing.signArea;

import imgProcessing.config.Config;
import imgProcessing.representation.MyImage;

public class PatternChecker {

    static public String recognisePattern(MyImage img){

        for(int i = 0; i < Config.PATTERN_BASE.length ; i++){
            if(img.isEqual(Config.PATTERN_BASE[i], Config.INEQUALITY_PERCENTAGE))
                return Integer.toString(i);
        }
        return "e";
    }
}
