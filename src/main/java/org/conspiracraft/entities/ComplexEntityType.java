package org.conspiracraft.entities;

public class ComplexEntityType extends EntityType {
    public ComplexEntityType(String name) {
        this.id = ComplexEntityTypes.entityTypeMap.size();
        this.name = name;
    }
}
