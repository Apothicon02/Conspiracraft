package org.conspiracraft.player;

import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.entities.CracksEntity;
import org.conspiracraft.entities.Entity;
import org.conspiracraft.entities.EntityType;
import org.conspiracraft.entities.EntityTypes;
import org.conspiracraft.gui.GUI;
import org.conspiracraft.items.types.ItemTypes;
import org.conspiracraft.physics.DDAResult;
import org.conspiracraft.physics.PhysicsHelper;
import org.conspiracraft.utils.Utils;
import org.conspiracraft.world.World;
import org.joml.*;
import org.conspiracraft.Main;
import org.conspiracraft.Window;
import org.conspiracraft.items.Item;

import java.lang.Math;

import static org.conspiracraft.Main.player;

public class HandManager {
    public static long lastBlockBrokenOrPlaced = 0L;
    public static long lastBlockPlaced = 0L;
    public static long lastBlockBreakCheck = 0;
    public static int hotbarSlot = 0;
    public static float prevTilt = 0;
    public static float tilt = 0;
    public static float tiltTarget = 0;
    public static int tiltDelay = 0;
    public static Vector4i blockStartedBreaking = new Vector4i();
    public static long delayStart = 0;
    public static int delay = 0;

    public static boolean lmbDown = false, mmbDown = false, rmbDown = false;
    public static void input() {
        if (player.inputHandler.scroll.y > 0) {
            hotbarSlot++;
            if (hotbarSlot >= Inventory.invWidth) {
                hotbarSlot = 0;
            }
        } else if (player.inputHandler.scroll.y < 0) {
            hotbarSlot--;
            if (hotbarSlot < 0) {
                hotbarSlot = Inventory.invWidth-1;
            }
        }
        lmbDown = lmbDown || player.inputHandler.leftButtonPressed;
        mmbDown = mmbDown || player.inputHandler.middleButtonPressed;
        rmbDown = rmbDown || player.inputHandler.rightButtonPressed;
    }
    public static void useHands(Window window) {
        Item selectedItem = player.inv.getItem(player.inv.selectedSlot);
        Vector2i blockToPlace = selectedItem == null ? new Vector2i(0) : selectedItem.place();
        if (!lmbDown) {
            player.breakingSource.stop();
            blockStartedBreaking.set(0, 0, 0, 0);
            tiltTarget = 0;
        }
        DDAResult ddaResult = PhysicsHelper.dda(player.getCameraTranslation(), player.camera.getForward(), 1000);
        if (ddaResult != null && ddaResult.hitAnything) {
            player.selectedBlock.set(ddaResult.hit.x(), ddaResult.hit.y(), ddaResult.hit.z());
            player.prevSelectedBlock.set(ddaResult.prevHit.x(), ddaResult.prevHit.y(), ddaResult.prevHit.z());
        } else {
            player.selectedBlock.set(-1);
            player.prevSelectedBlock.set(-1);
        }
        if (Main.timeMsLong - delayStart >= delay) {
            delayStart = Main.timeMsLong;
            delay = (selectedItem == null || selectedItem.amount <= 0) ? 0 : selectedItem.use(ddaResult);
            if (delay == 0) { //if item did no interaction
                if (lmbDown && World.inBounds(player.selectedBlock)) {
                    mine(4);
                }
            }
        }
        if (lmbDown) {
            if (tiltTarget == 0) {
                tiltTarget = 30;
            }
        } else {
            tiltTarget = 0;
        }
        lmbDown = false;
        mmbDown = false;
        rmbDown = false;
    }

    public static void mine(int damage) {
        CracksEntity entity = null;
        for (Entity maybeEntity : World.entities) {
            if (maybeEntity instanceof CracksEntity cracksEntity && (int)maybeEntity.prevPos.x() == (int)player.selectedBlock.x()
                    && (int)maybeEntity.prevPos.y() == (int)player.selectedBlock.y()  && (int)maybeEntity.prevPos.z() == (int)player.selectedBlock.z()) {
                entity = cracksEntity;
                break;
            }
        }
        Vector3i selectedBlockI = new Vector3i((int)player.selectedBlock.x(), (int)player.selectedBlock.y(), (int)player.selectedBlock.z());
        Vector2i block = World.getBlock(selectedBlockI);
        if (entity == null) {
            entity = new CracksEntity(EntityTypes.SLIGHTLY_CRACKED, new Matrix4f().setTranslation(player.selectedBlock).translate(0.5f, 0.5f, 0.5f).scale(1.01f), 0);
            World.entities.add(entity);
        } else if (entity.mine(damage, BlockTypes.blockTypes[block.x()].blockProperties.blockSFX)) {
            World.setBlock((int)player.selectedBlock.x(), (int)player.selectedBlock.y(), (int)player.selectedBlock.z(), 0, 0);
            delay = 300;
        }
    }

    public static void tick() {
        if (tiltDelay >= 0) {
            tiltDelay--;
        }
        prevTilt = tilt;
        if (tiltDelay <= 0) {
            if (tiltTarget == 0 && Math.abs(tilt - tiltTarget) < 10f) {
                tilt = 0;
            } else {
                if (tilt < tiltTarget) {
                    tilt += 10f;
                } else if (tilt > tiltTarget) {
                    tilt -= 10f;
                }
                if (tilt >= 30) {
                    tiltTarget = -30;
                } else if (tilt <= -30) {
                    tiltTarget = 30;
                }
            }
        }
    }

    public static float getTilt() {
        return Utils.getInterpolatedFloat(prevTilt, tilt);
    }
}
