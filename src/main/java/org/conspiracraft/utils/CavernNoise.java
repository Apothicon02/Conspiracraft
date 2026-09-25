package org.conspiracraft.utils;

import static java.lang.Math.*;

public class CavernNoise {
    public static double sample(double x, double y, double z) {
        double offX = x+(y*5);
        double offZ = z+(y*5);
        return layer3d(offX, y, offZ);
    }
    private static double layer3d(double x, double y, double z) {
        double height = 8+(32*cos(x/128.f));
        double verticality = (cos((y+(height*cos(x/256.f))+(height*cos(z/256.f)))/15.f)+1)*3.75f;//*((cos((y+(height*cos(x/256.f))+(height*cos(z/256.f)))/6.f)+1)*0.5f);
        double result = layer(x, z);
        return (verticality)+result;
    }
    private static double layer(double x, double y) {
        double offX = x+cos((y+50000)/25.f)*25;
        double offY = y+cos((x+25000)/10.f)*10;
        double r = abs((cos(offX/100.f)+cos((offY+10000)/100.f))*0.5f);
        double g = abs((cos(offY/100.f)+cos((offX+10000)/100.f))*0.5f);
        return max(r, g);
    }
}
