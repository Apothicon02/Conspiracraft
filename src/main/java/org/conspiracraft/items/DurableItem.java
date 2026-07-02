package org.conspiracraft.items;

import org.conspiracraft.items.types.ItemTypes;
import org.joml.Vector3f;

import java.nio.IntBuffer;

public class DurableItem extends Item implements Cloneable {
    public static int dataLength = 10; //excludes this int
    public int durability = 1;

    @Override
    public Item load(IntBuffer data) {
        return ((DurableItem)new DurableItem().type(ItemTypes.itemTypeMap.get(data.get())).moveTo(new Vector3f(data.get()/1000f, data.get()/1000f, data.get()/1000f)).rot(data.get()/1000f).hover(data.get()/1000f, data.get()>0).amount(data.get()).timeExisted(data.get())).durability(data.get());
    }
    @Override
    public int[] getData() {
        return new int[]{dataLength, ItemTypes.getId(type), (int)(pos.x()*1000), (int)(pos.y()*1000), (int)(pos.z()*1000), (int)(rot*1000), (int)(hover*1000), hoverMeridiem ? 1 : 0, amount, timeExisted, durability};
    }

    public DurableItem durability(int durability) {
        this.durability = durability;
        return this.durability <= 0 ? null : this;
    }
    public DurableItem damage(int damage) {
        this.durability -= damage;
        return this.durability <= 0 ? null : this;
    }
}
