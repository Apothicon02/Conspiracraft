package org.terraflat.engine;

public class TerraflatMath {
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
}
