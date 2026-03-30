package org.conspiracraft;

public class Main {
    public static String mainFolder = System.getenv("APPDATA")+"/Conspiracraft/";
    public static String resourcesPath = mainFolder+"resources/";

    public static Window window;

    public static boolean isClosing = false;

    static void main(String[] args) throws Exception {
        window = new Window();
        while (!isClosing) {
            window.update();
        }
    }
}