package org.conspiracraft.items;

import org.conspiracraft.Main;
import org.conspiracraft.blocks.types.BlockTypes;
import org.conspiracraft.world.World;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class Item implements Cloneable {
    public int dataLength = 9; //excludes this int
    public ItemType type = ItemTypes.AIR;
    public Vector3f pos = new Vector3f();
    public int amount = 1;
    public float rot = 0.f;
    public float hover = 0.f;
    public boolean hoverMeridiem = false;
    public int timeExisted = 0;
    public long prevTickTime = 0;

    public static Item load(int[] data, int offset) {
        return new Item().type(ItemTypes.itemTypeMap.get(data[offset++])).moveTo(new Vector3f(data[offset++]/1000f, data[offset++]/1000f, data[offset++]/1000f)).rot(data[offset++]/1000f).hover(data[offset++]/1000f, data[offset++]>0).amount(data[offset++]).timeExisted(data[offset++]);
    }
    public int[] getData() {
        return new int[]{dataLength, ItemTypes.getId(type), (int)(pos.x()*1000), (int)(pos.y()*1000), (int)(pos.z()*1000), (int)(rot*1000), (int)(hover*1000), hoverMeridiem ? 1 : 0, amount, timeExisted};
    }

    public void tick() {
        long time = Main.timeMsLong;
        if (prevTickTime != 0) {
            long dif = time - prevTickTime;
            Vector2i block = World.getBlock(pos.x(), pos.y()-0.125f, pos.z());
            if (block != null && !BlockTypes.blockTypeMap.get(block.x()).blockProperties.isSolid) {
                this.pos.y -= 0.125f;
            }
//            int start = (int)(Math.random()*Math.max(1, World.items.size()-10));
//            int end = Math.min(start+10, World.items.size());
            for (Item randomItem : World.items) {
                //Item randomItem = World.items.get(i);
                if (randomItem.type == type && randomItem.amount < randomItem.type.maxStackSize && Math.abs(randomItem.pos.x() - pos.x()) < 1.f && Math.abs(randomItem.pos.y() - pos.y()) < 1.f && Math.abs(randomItem.pos.z() - pos.z()) < 1.f) {
                    int flow = Math.min(amount, randomItem.type.maxStackSize - randomItem.amount);
                    randomItem.amount += flow;
                    amount -= flow;
                    if (amount <= 0) {
                        World.items.remove(this);
                    }
                    break;
                }
            }
            timeExisted += dif;
            rot += (dif / 50f) * Math.random();
            if (rot >= 360) {
                rot = 0;
            }
            double hoverInc = (dif / 1750f) * Math.min(Math.max(0.01f, 0.1f - hover) * 10, Math.max(0.01f, 0.1f - Math.abs(hover - 0.1f)) * 10) * Math.random();
            if (hoverMeridiem) {
                hover += hoverInc;
                if (hover >= 0.1) {
                    hover = 0.1f;
                    hoverMeridiem = false;
                }
            } else {
                hover -= hoverInc;
                if (hover < 0.f) {
                    hover = 0.f;
                    hoverMeridiem = true;
                }
            }
        }
        prevTickTime = time;
    }
    public Item moveTo(Vector3i pos) {
        this.pos = new Vector3f(pos.x, pos.y, pos.z);
        return this;
    }
    public Item moveTo(Vector3f pos) {
        this.pos = new Vector3f(pos.x, pos.y, pos.z);
        return this;
    }
    public Item type(ItemType type) {
        this.type = type;
        return this;
    }
    public Item amount(int amount) {
        this.amount = amount;
        return this;
    }
    public Item timeExisted(int newTime) {
        this.timeExisted = newTime;
        return this;
    }
    public Item rot(float rot) {
        this.rot = rot;
        return this;
    }
    public Item hover(float hover, boolean hoverMeridiem) {
        this.hover = hover;
        this.hoverMeridiem = hoverMeridiem;
        return this;
    }
    public Item prevTickTime(long time) {
        this.prevTickTime = time;
        return this;
    }

    @Override
    public Item clone() {
        try {
            return (Item) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public void playSound(Vector3f pos) {
        type.sound.placed(pos);
    }

    public Vector2i place() {
        return type.blockToPlace;
    }
}
