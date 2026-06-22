package org.conspiracraft.elements;

import org.conspiracraft.items.types.ItemType;

public class Element {
    public double specificHeat;
    public int freezingTemp;
    public ItemType iceItemType = null;
    public String name;

    public Element(String name, double specificHeat, int freezingTemp) {
        this.name = name;
        this.specificHeat = specificHeat;
        this.freezingTemp = freezingTemp;
    }
}