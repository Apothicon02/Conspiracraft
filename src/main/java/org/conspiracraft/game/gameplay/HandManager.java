package org.conspiracraft.game.gameplay;

import org.conspiracraft.Main;
import org.conspiracraft.engine.MouseInput;
import org.conspiracraft.game.audio.BlockSFX;
import org.conspiracraft.game.blocks.Fluids;
import org.conspiracraft.game.blocks.Tags;
import org.conspiracraft.game.blocks.types.BlockProperties;
import org.conspiracraft.game.blocks.types.BlockType;
import org.conspiracraft.game.blocks.types.BlockTypes;
import org.conspiracraft.game.blocks.types.FullBucketBlockType;
import org.conspiracraft.game.world.World;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4i;

import static org.conspiracraft.Main.player;

public class HandManager {
    public static long lastBlockBrokenOrPlaced = 0L;
    public static long lastBlockPlaced = 0L;
    public static long lastBlockBreakCheck = 0;
    public static Vector4i blockStartedBreaking = new Vector4i();

    public static void useHands(long timeMillis, MouseInput mouseInput) {
        if (mouseInput.scroll.y > 0) {
            for (int i = 0; i < mouseInput.scroll.y; i++) {
                StackManager.cycleStackForward();
            }
        } else if (mouseInput.scroll.y < 0) {
            for (int i = 0; i < -1*mouseInput.scroll.y; i++) {
                StackManager.cycleStackBackward();
            }
        }
        boolean lmbDown = mouseInput.isLeftButtonPressed();
        boolean mmbDown = mouseInput.isMiddleButtonPressed();
        boolean rmbDown = mouseInput.isRightButtonPressed();
        if (!lmbDown) {
            player.breakingSource.stop();
            blockStartedBreaking.set(0, 0, 0, 0);
        }
        if ((!player.creative || (timeMillis - lastBlockBrokenOrPlaced >= 200)) && (!rmbDown || timeMillis - lastBlockPlaced >= 200)) { //two tenth second minimum delay between breaking blocks in creative or when placing blocks
            if (lmbDown || mmbDown || rmbDown) {
                Vector3f pos = World.raycast(new Matrix4f(player.getCameraMatrix()), lmbDown || mmbDown, player.reach, (Tags.buckets.tagged.contains(player.stack[0]) && lmbDown) || (mmbDown && player.crouching), player.reachAccuracy);
                if (pos != null) {
                    if (mmbDown) {
                        Vector2i block = World.getBlock(pos.x, pos.y, pos.z);
                        if (block != null) {
                            if (BlockTypes.blockTypeMap.get(block.x).blockProperties.isFluid) {
                                block.x = Fluids.fluidBucketMap.get(block.x);
                            }
                            if (player.creative) {
                                StackManager.setFirstEntryInStack(block);
                            } else {
                                StackManager.cycleToEntryInStack(block);
                            }
                        }
                    } else if (BlockTypes.blockTypeMap.get(player.stack[0]) != null) {
                        lastBlockBrokenOrPlaced = timeMillis;
                        int blockTypeId = player.stack[0];
                        int blockSubtypeId = player.stack[1];
                        int cornerData = World.getCorner((int) pos.x, (int) pos.y, (int) pos.z);
                        int cornerIndex = (pos.y < (int)(pos.y)+0.5 ? 0 : 4) + (pos.z < (int)(pos.z)+0.5 ? 0 : 2) + (pos.x < (int)(pos.x)+0.5 ? 0 : 1);
                        if (lmbDown) {
                            cornerData |= (1 << (cornerIndex - 1));
                            Vector2i blockBreaking = World.getBlock(pos.x, pos.y, pos.z);
                            if (cornerData == -2147483521 || !player.crouching) {
                                boolean canBreak = true;
                                if (BlockTypes.blockTypeMap.get(blockBreaking.x).blockProperties.isFluid) {
                                    if (BlockTypes.blockTypeMap.get(blockTypeId) == BlockTypes.BUCKET) {
                                        StackManager.removeFirstEntryInStack();
                                        blockBreaking.x = Fluids.fluidBucketMap.get(blockBreaking.x);
                                    } else if (Fluids.fluidBucketMap.get(blockBreaking.x) == blockTypeId) {
                                        int room = 15-blockSubtypeId;
                                        int flow = Math.min(room, blockBreaking.y);
                                        player.stack[1] += flow;
                                        blockBreaking.y -= flow;
                                        if (blockBreaking.y < 1) {
                                            World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, 0, 0, true, false, 1, false);
                                        } else {
                                            World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockBreaking.x, blockBreaking.y, true, false, 1, false);
                                        }
                                        canBreak = false;
                                    } else {
                                        canBreak = false;
                                    }
                                }
                                if (canBreak) {
                                    if (!player.creative) {
                                        boolean sameBlock = blockStartedBreaking.x == (int)(pos.x) && blockStartedBreaking.y == (int)(pos.y) && blockStartedBreaking.z == (int)(pos.z);
                                        if (sameBlock) {
                                            if (blockStartedBreaking.w > 0) {
                                                canBreak = false;
                                                blockStartedBreaking.sub(0, 0, 0, (int) (System.currentTimeMillis()-lastBlockBreakCheck));
                                                lastBlockBreakCheck = System.currentTimeMillis();
                                            }
                                        } else {
                                            canBreak = false;
                                            lastBlockBreakCheck = System.currentTimeMillis();
                                            blockStartedBreaking.set((int) pos.x, (int) pos.y, (int) pos.z, BlockTypes.blockTypeMap.get(blockBreaking.x).getTTB());
                                            BlockSFX sfx = BlockTypes.blockTypeMap.get(blockBreaking.x).blockProperties.blockSFX;
                                            player.breakingSource.setPos(pos);
                                            player.breakingSource.setGain(sfx.placeGain);
                                            player.breakingSource.setPitch(sfx.placePitch, 0);
                                            player.breakingSource.play(sfx.placeIds[(int) (Math.random() * sfx.placeIds.length)], true);
                                        }
                                    }
                                    if (canBreak) {
                                        if (player.creative ? true : StackManager.addToStack(blockBreaking)) {
                                            blockStartedBreaking.set(0, 0, 0, 0);
                                            player.breakingSource.stop();
                                            World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, 0);
                                            World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, 0, 0, true, false, 1, false);
                                        }
                                    }
                                }
                            } else if (BlockTypes.blockTypeMap.get(blockBreaking.x).blockProperties.isSolid) {
                                World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, cornerData);
                            }
                        } else if (rmbDown) {
                            lastBlockPlaced = System.currentTimeMillis();
                            if (cornerData != 0) {
                                cornerData &= (~(1 << (cornerIndex - 1)));
                                World.setCorner((int) pos.x, (int) pos.y, (int) pos.z, cornerData);
                            } else if (player.stack[0] > 0) {
                                Vector2i oldBlock = World.getBlock((int) pos.x, (int) pos.y, (int) pos.z);
                                BlockProperties oldType = BlockTypes.blockTypeMap.get(oldBlock.x).blockProperties;
                                BlockType blockType = BlockTypes.blockTypeMap.get(blockTypeId);
                                if (blockType instanceof FullBucketBlockType && !player.crouching) {
                                    blockTypeId = Fluids.fluidBucketMap.get(blockTypeId);
                                    blockType = BlockTypes.blockTypeMap.get(blockTypeId);
                                }
                                if (oldType.isFluidReplaceable || (oldType.isFluid && !Tags.buckets.tagged.contains(blockTypeId) && !blockType.blockProperties.isFluidReplaceable && !blockType.blockProperties.isFluid)) {
                                    World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockTypeId, blockSubtypeId, true, false, 1, false);
                                    if (!player.creative) {
                                        if (blockType.blockProperties.isFluid) {
                                            blockTypeId = BlockTypes.getId(BlockTypes.BUCKET);
                                            blockSubtypeId = 0;
                                            StackManager.setFirstEntryInStack(new Vector2i(blockTypeId, blockSubtypeId));
                                        } else {
                                            StackManager.removeFirstEntryInStack();
                                        }
                                    }
                                } else if (oldType.isFluid && blockTypeId == oldBlock.x) { //merge liquid
                                    int room = 15-oldBlock.y;
                                    int flow = Math.min(room, blockSubtypeId);
                                    World.setBlock((int) pos.x, (int) pos.y, (int) pos.z, blockTypeId, oldBlock.y+flow, true, false, 1, false);
                                    if (!player.creative) {
                                        blockSubtypeId -= flow;
                                        if (blockSubtypeId < 1) {
                                            blockTypeId = BlockTypes.getId(BlockTypes.BUCKET);
                                            blockSubtypeId = 0;
                                        }
                                        StackManager.setFirstEntryInStack(new Vector2i(blockTypeId, blockSubtypeId));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
