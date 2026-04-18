package org.conspiracraft.player;

import org.conspiracraft.utils.Utils;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4i;
import org.conspiracraft.Main;
import org.conspiracraft.Window;
import org.conspiracraft.audio.BlockSFX;
import org.conspiracraft.blocks.BlockTags;
import org.conspiracraft.blocks.drops.BlockDrops;
import org.conspiracraft.blocks.types.BlockProperties;
import org.conspiracraft.blocks.types.BlockType;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.items.Item;
import org.conspiracraft.world.World;

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

    public static void useHands(Window window) {
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
        Item selectedItem = player.inv.getItem(player.inv.selectedSlot);
        Vector2i blockToPlace = selectedItem == null ? new Vector2i(0) : selectedItem.place();
        if (BlockTags.smallBlock.tagged.contains(blockToPlace.x())) {
            player.chiselMode = true;
        }
        boolean lmbDown = player.inputHandler.leftButtonPressed;
        boolean mmbDown = player.inputHandler.middleButtonPressed;
        boolean rmbDown = player.inputHandler.rightButtonPressed;
        if (!lmbDown) {
            player.breakingSource.stop();
            blockStartedBreaking.set(0, 0, 0, 0);
            tiltTarget = 0;
        }
        if ((!player.creative || (Main.timeMsLong - lastBlockBrokenOrPlaced >= 200)) && (!rmbDown || Main.timeMsLong - lastBlockPlaced >= 200)) { //two tenth second minimum delay between breaking blocks in creative or when placing blocks
            if (lmbDown || mmbDown || rmbDown) {
                Vector2i handBlock = new Vector2i(blockToPlace.x, blockToPlace.y);
                BlockType handType = BlockTypes.blockTypeMap.get(blockToPlace.x);
                Vector3f pos = lmbDown || mmbDown ? player.selectedBlock : player.prevSelectedBlock;
                if (pos != null && World.inBounds((int) pos.x, (int) pos.y, (int) pos.z)) {
                    if (mmbDown) {
                        Vector2i block = World.getBlock(pos.x, pos.y, pos.z);
                        if (block != null) {
                            if (player.creative) {
                                //StackManager.setFirstEntryInStack(block);
                            } else {
                                //StackManager.cycleToEntryInStack(block);
                            }
                        }
                    } else {
                        lastBlockBrokenOrPlaced = Main.timeMsLong;
                        if (lmbDown) {
                            Vector2i blockBreaking = World.getBlock(pos.x, pos.y, pos.z);
                            BlockType breakingType = BlockTypes.blockTypeMap.get(blockBreaking.x);
                            if (!player.crouching) {
                                Vector3i intBreakingPos = new Vector3i((int) pos.x, (int) pos.y, (int) pos.z);
                                boolean canBreak = breakingType.whilePlayerBreaking(intBreakingPos, blockBreaking, handBlock);
                                if (canBreak && !BlockTags.cantBreakBlocks.tagged.contains(blockToPlace.x)) {
                                    if (!player.creative) {
                                        boolean sameBlock = blockStartedBreaking.x == (int)(pos.x) && blockStartedBreaking.y == (int)(pos.y) && blockStartedBreaking.z == (int)(pos.z);
                                        if (sameBlock) {
                                            if (blockStartedBreaking.w > 0) {
                                                canBreak = false;
                                                blockStartedBreaking.sub(0, 0, 0, (int) ((Main.timeMsLong-lastBlockBreakCheck)*breakingType.getTTBSpeed(blockToPlace.x)));
                                                lastBlockBreakCheck = Main.timeMsLong;
                                            }
                                        } else {
                                            canBreak = false;
                                            lastBlockBreakCheck = Main.timeMsLong;
                                            blockStartedBreaking.set((int) pos.x, (int) pos.y, (int) pos.z, breakingType.getTTB());
                                            BlockSFX sfx = breakingType.blockProperties.blockSFX;
                                            player.breakingSource.setPos(pos);
                                            player.breakingSource.setGain(sfx.placeGain);
                                            player.breakingSource.setPitch(sfx.placePitch*(1+(Math.abs(1-breakingType.getTTBSpeed(blockToPlace.x))*0.8f)), 0);
                                            player.breakingSource.play(sfx.placeIds[(int) (Math.random() * sfx.placeIds.length)], true);
                                        }
                                    }
                                    if (canBreak) {
                                        if (!player.creative) {
                                            for (Item item : BlockDrops.getDrops(blockBreaking)) {
                                                World.items.add(item.moveTo(new Vector3f((float)(Math.floor(pos.x())+(Math.clamp(pos.x-Math.floor(pos.x), 0.2, 0.8)/2)),
                                                        (float)(Math.floor(pos.y())+(Math.clamp(pos.y-Math.floor(pos.y), 0.2, 0.8)/2)),
                                                        (float)(Math.floor(pos.z())+(Math.clamp(pos.z-Math.floor(pos.z), 0.2, 0.8)/2)))));
                                            }
                                        }
                                        blockStartedBreaking.set(0, 0, 0, 0);
                                        player.breakingSource.stop();
                                        //World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, 0);
                                        if (player.chiselMode) {
                                            World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, 0, 0, true, false, 1, false);
                                        } else {
                                            Vector3i roundedPos = new Vector3i((int)(pos.x()/2), (int)(pos.y()/2), (int)(pos.z()/2)).mul(2);
                                            World.setBlock(roundedPos.x(), roundedPos.y(), roundedPos.z(), 0, 0, true, false, 1, false);
                                            World.setBlock(roundedPos.x()+1, roundedPos.y(), roundedPos.z(), 0, 0, true, false, 1, false);
                                            World.setBlock(roundedPos.x(), roundedPos.y()+1, roundedPos.z(), 0, 0, true, false, 1, false);
                                            World.setBlock(roundedPos.x(), roundedPos.y(), roundedPos.z()+1, 0, 0, true, false, 1, false);
                                            World.setBlock(roundedPos.x()+1, roundedPos.y(), roundedPos.z()+1, 0, 0, true, false, 1, false);
                                            World.setBlock(roundedPos.x()+1, roundedPos.y()+1, roundedPos.z(), 0, 0, true, false, 1, false);
                                            World.setBlock(roundedPos.x(), roundedPos.y()+1, roundedPos.z()+1, 0, 0, true, false, 1, false);
                                            World.setBlock(roundedPos.x()+1, roundedPos.y()+1, roundedPos.z()+1, 0, 0, true, false, 1, false);
                                        }
                                        //BlockBreaking.blockBroken(blockBreaking, handBlock);
                                    }
                                }
                            } else if (breakingType.blockProperties.isSolid) {
                                //World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, cornerData);
                            }
                        } else if (rmbDown) {
                            lastBlockPlaced = Main.timeMsLong;
                            if (blockToPlace.x > 0) {//player.stack[0] > 0) {
                                Vector2i oldBlock = World.getBlock((int) pos.x, (int) pos.y, (int) pos.z);
                                BlockProperties oldType = BlockTypes.blockTypeMap.get(oldBlock.x).blockProperties;
                                if (oldType.isFluidReplaceable) {
                                    if (player.chiselMode) {
                                        World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockToPlace.x, blockToPlace.y, true, false, 1, false);
                                    } else {
                                        Vector3i roundedPos = new Vector3i((int)(pos.x()/2), (int)(pos.y()/2), (int)(pos.z()/2)).mul(2);
                                        World.setBlock(roundedPos.x(), roundedPos.y(), roundedPos.z(), blockToPlace.x, blockToPlace.y, true, false, 1, false);
                                        World.setBlock(roundedPos.x()+1, roundedPos.y(), roundedPos.z(), blockToPlace.x, blockToPlace.y, true, false, 1, false);
                                        World.setBlock(roundedPos.x(), roundedPos.y()+1, roundedPos.z(), blockToPlace.x, blockToPlace.y, true, false, 1, false);
                                        World.setBlock(roundedPos.x(), roundedPos.y(), roundedPos.z()+1, blockToPlace.x, blockToPlace.y, true, false, 1, false);
                                        World.setBlock(roundedPos.x()+1, roundedPos.y(), roundedPos.z()+1, blockToPlace.x, blockToPlace.y, true, false, 1, false);
                                        World.setBlock(roundedPos.x()+1, roundedPos.y()+1, roundedPos.z(), blockToPlace.x, blockToPlace.y, true, false, 1, false);
                                        World.setBlock(roundedPos.x(), roundedPos.y()+1, roundedPos.z()+1, blockToPlace.x, blockToPlace.y, true, false, 1, false);
                                        World.setBlock(roundedPos.x()+1, roundedPos.y()+1, roundedPos.z()+1, blockToPlace.x, blockToPlace.y, true, false, 1, false);
                                    }
                                    if (!player.creative) {
                                        //StackManager.removeFirstEntryInStack();
                                    }
                                }
                            }
                        }
                    }
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
