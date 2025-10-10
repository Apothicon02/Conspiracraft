package org.conspiracraft.game.gameplay;

import org.joml.Vector2i;

import static org.conspiracraft.Main.player;

public class StackManager {
    public static void dropStackToGround() {
        // add block entities and use one to make a bundle containing the stack
    }
    public static void cycleStackForward() {
        int ogType = player.stack[0];
        int ogSubtype = player.stack[1];
        for (int i = 0; i < player.stack.length-2; i+=2) {
            player.stack[i] = player.stack[i+2];
            player.stack[i+1] = player.stack[i+3];
        }
        player.stack[player.stack.length-1] = ogSubtype;
        player.stack[player.stack.length-2] = ogType;
    }
    public static void cycleStackBackward() {
        int ogType = player.stack[player.stack.length-2];
        int ogSubtype = player.stack[player.stack.length-1];
        for (int i = player.stack.length-1; i >= 2; i-=2) {
            player.stack[i-1] = player.stack[i-3];
            player.stack[i] = player.stack[i-2];
        }
        player.stack[0] = ogType;
        player.stack[1] = ogSubtype;
    }
    public static void removeFirstEntryInStack() {
        player.stack[0] = 0;
        player.stack[1] = 0;
        for (int i = 0; i < player.stack.length-2; i+=2) {
            player.stack[i] = player.stack[i+2];
            player.stack[i+1] = player.stack[i+3];
        }
        player.stack[player.stack.length-1] = 0;
        player.stack[player.stack.length-2] = 0;
    }
    public static boolean addToStack(Vector2i block) {
        for (int i = 0; i < player.stack.length; i+=2) {
            if (player.stack[i] == 0) {
                player.stack[i] = block.x;
                player.stack[i + 1] = block.y;
                return true;
            }
        }
        return false;
    }
    public static void setFirstEntryInStack(Vector2i block) {
        player.stack[0] = block.x;
        player.stack[1] = block.y;
    }
    public static void cycleToEntryInStack(Vector2i block) {
        for (int i = 0; i < player.stack.length; i+=2) {
            if (player.stack[0] == block.x && player.stack[1] == block.y) {
                return;
            } else {
                cycleStackForward();
            }
        }
        //if no exact match, search for a partial match
        for (int i = 0; i < player.stack.length; i+=2) {
            if (player.stack[0] == block.x) {
                return;
            } else {
                cycleStackForward();
            }
        }
    }
    public static void setWholeStack(Vector2i block) {
        for (int i = 0; i < player.stack.length; i+=2) {
            player.stack[i] = block.x;
            player.stack[i+1] = block.y;
        }
    }
}
