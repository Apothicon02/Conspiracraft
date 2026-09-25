package org.conspiracraft.utils;

import static java.lang.Math.*;

public class NoodleNoise {
//    public static final ReturnDistanceFunction subDistance = new ReturnDistanceFunction() {
//        @Override
//        public double applyAsDouble(double[] distances) {
//            return distances[2] - distances[0];
//        }
//        @Override
//        public boolean isValidArrayLength(int depth) {
//            return depth >= 3;
//        }
//    };
//
//    public static final WorleyNoiseGenerator noisePipeline = WorleyNoiseGenerator.newBuilder().setSeed(6642).setDepth(3).setDistanceFunction(DistanceFunctionType.EUCLIDEAN).setFeaturePointAmountFunction(i -> 1).setReturnDistanceFunction(subDistance).build();

    public static double sample(double x, double y, double z) {
        //return noisePipeline.evaluateNoise(x*0.015f, y*0.025f, z*0.015f);
        return min(layer3d(x+y, z, y-z), layer3d(z+y+5123, x-32, y+8432-x));
    }
    private static double layer3d(double x, double y, double z) {
        double height = 8+(32*cos(x/128.f));
        double verticality = (cos((y+(height*cos(x/64.f))+(height*cos(z/64.f)))/15.f)+1)*3.75f;//*((cos((y+(height*cos(x/256.f))+(height*cos(z/256.f)))/6.f)+1)*0.5f);
        double result = layer(x, z);
        return (verticality)+result;
    }
    public static double sample(double x, double y) {
        double result = min(layer(x, y), layer(y, x));
        return result;
    }
    private static double layer(double x, double y) {
        double offX = x+cos((y+50000)/25.f)*25;
        double offY = y+cos((x+25000)/10.f)*10;
        double r = min(0.1f, abs((cos(offX/100.f)+cos((offY+10000)/100.f))*0.5f))*10;
        double g = min(0.1f, abs((cos(offY/100.f)+cos((offX+10000)/100.f))*0.5f))*10;
        return max(r, g);
    }
}
