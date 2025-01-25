package org.conspiracraft.engine;

public class BitBuffer {
    private int[] bits;
    public final int bitsPerValue;
    private final int valueMask;
    private final int valuesPerInt;

    public BitBuffer(int numValues, int bitsPerValue) {
        this.bitsPerValue = bitsPerValue;
        this.valueMask = (1 << bitsPerValue) - 1;
        this.valuesPerInt = 32 / bitsPerValue;
        this.bits = new int[Math.ceilDiv(numValues, this.valuesPerInt)];
    }

    public void setData(int[] data) {
        bits = data;
    }
    public void setValue(int index, int value) {
        // Each int fits `valuesPerInt` values of `bitsPerValue` each.
        int intIndex = index / valuesPerInt;
        int bitIndex = (index % valuesPerInt) * bitsPerValue;
        bits[intIndex] &= ~(valueMask << bitIndex);
        bits[intIndex] |= value << bitIndex;
    }

    public int[] getData() {
        return bits;
    }
    public int getValue(int index) {
        int intIndex = index / valuesPerInt;
        int bitIndex = (index % valuesPerInt) * bitsPerValue;
        return (bits[intIndex] >> bitIndex ) & valueMask;
    }
}