package org.conspiracraft.engine;

public class BitBuffer {
    private int[] bits;
    public final int bitsPerValue;
    public final int valueMask;
    public final int valuesPerInt;

    public BitBuffer(int numValues, int bitsPerValue) {
        this.bitsPerValue = bitsPerValue;
        if (bitsPerValue == 0) {
            this.valueMask = 0;
            this.valuesPerInt = 0;
            this.bits = null;
        } else {
            this.valueMask = (1 << bitsPerValue) - 1;
            this.valuesPerInt = 32 / bitsPerValue;
            this.bits = new int[Math.ceilDiv(numValues, this.valuesPerInt)];
        }
    }

    public void setData(int[] data) {
        bits = data;
    }
    public void setValue(int index, int value) {
        if (bitsPerValue > 0 && !(bits == null || bits.length == 0)) {
            // Each int fits `valuesPerInt` values of `bitsPerValue` each.
            int intIndex = index / valuesPerInt;
            int bitIndex = (index % valuesPerInt) * bitsPerValue;
            bits[intIndex] &= ~(valueMask << bitIndex);
            bits[intIndex] |= value << bitIndex;
        }
    }

    public int[] getData() {
        return bits;
    }
    public int getValue(int index) {
        if (bits == null || bits.length == 0) {
            return 0;
        }
        int intIndex = index / valuesPerInt;
        int bitIndex = (index % valuesPerInt) * bitsPerValue;
        return (bits[intIndex] >> bitIndex ) & valueMask;
    }
}