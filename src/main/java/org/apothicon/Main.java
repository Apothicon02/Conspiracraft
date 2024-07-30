package org.apothicon;

import org.apothicon.core.Engine;
import org.apothicon.core.Terraflat;
import org.apothicon.core.WindowManager;
import org.apothicon.core.utilities.Constants;

public class Main {
    private static WindowManager window;
    private static Terraflat terraflat;

    public static void main(String[] args) {
        window = new WindowManager(Constants.NAME_OF_THE_GAME, 0, 0, false);
        terraflat = new Terraflat();
        Engine engine = new Engine();

        try {
            engine.start();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static WindowManager getWindow() {
        return window;
    }

    public static Terraflat getTerraflat() {
        return terraflat;
    }
}