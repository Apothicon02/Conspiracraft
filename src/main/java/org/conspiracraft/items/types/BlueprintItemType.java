package org.conspiracraft.items.types;

import org.conspiracraft.items.BlueprintItem;

public class BlueprintItemType extends ItemType {
    public BlueprintItemType(String name) {
        super(name);
    }

    @Override
    public BlueprintItem createItem() {
        return (BlueprintItem) new BlueprintItem().type(this);
    }
}
