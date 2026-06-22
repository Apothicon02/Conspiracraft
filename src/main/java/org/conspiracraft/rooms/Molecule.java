package org.conspiracraft.rooms;

public class Molecule {
    public byte element;
    public int amount;
    public Molecule(int element, int amount) {
        this.element = (byte)(element);
        this.amount = amount;
    }
    public Molecule(Molecule molecule) {
        this.element = molecule.element;
        this.amount = molecule.amount;
    }
}
