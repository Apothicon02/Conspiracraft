package org.conspiracraft.engine;

import java.util.List;

public class ConspiracraftMath {
    public static double gradient(int y, int fromY, int toY, float fromValue, float toValue) {
        return clampedLerp(toValue, fromValue, inverseLerp(y, fromY, toY));
    }

    public static double inverseLerp(double y, double fromY, double toY) {
        return (y - fromY) / (toY - fromY);
    }

    public static double clampedLerp(double toValue, double fromValue, double invLerpValue) {
        if (invLerpValue < 0.0D) {
            return toValue;
        } else {
            return invLerpValue > 1.0D ? fromValue : lerp(invLerpValue, toValue, fromValue);
        }
    }

    public static double lerp(double invLerpValue, double toValue, double fromValue) {
        return toValue + invLerpValue * (fromValue - toValue);
    }

    public static float mix(float min, float max, float factor) {
        return min * (1 - factor) + max * factor;
    }

    public static double averageLongs(List<Long> numbers) {
        double sum = 0.0;
        for (double num : numbers) {
            sum += num;
        }

        return sum / numbers.size();
    }

    public static double averageInts(List<Integer> numbers) {
        double sum = 0.0;
        for (int num : numbers) {
            sum += num;
        }

        return sum / numbers.size();
    }
}
