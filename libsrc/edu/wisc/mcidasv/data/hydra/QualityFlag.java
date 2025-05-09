package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.HashMap;

/**
 * @author tommyj
 *
 * Holds info to extract a Suomi NPP Quality Flag from a packed byte.
 * Info is read from the appropriate XML Product Profile
 *
 */

public class QualityFlag {

    private int bitOffset = -1;
    private int numBits = -1;
    private String name = null;
    private String packedName = null;
    private HashMap<String, String> hm = null;

    /**
     * @param bitOffset
     * @param numBits
     * @param name
     */

    public QualityFlag(int bitOffset, int numBits, String name, HashMap<String, String> hm) {
        this.bitOffset = bitOffset;
        this.numBits = numBits;
        this.name = name;
        this.hm = hm;
    }

    /**
     * @return the bitOffset
     */
    public int getBitOffset() {
        return bitOffset;
    }

    /**
     * @param bitOffset the bitOffset to set
     */
    public void setBitOffset(int bitOffset) {
        this.bitOffset = bitOffset;
    }

    /**
     * @return the numBits
     */
    public int getNumBits() {
        return numBits;
    }

    /**
     * @param numBits the numBits to set
     */
    public void setNumBits(int numBits) {
        this.numBits = numBits;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the packedName
     */
    public String getPackedName() {
        return packedName;
    }

    /**
     * @param packedName the packedName to set
     */
    public void setPackedName(String packedName) {
        this.packedName = packedName;
    }

    /**
     * @return the hm
     */
    public HashMap<String, String> getHm() {
        return hm;
    }

    /**
     * @param hm the hm to set
     */
    public void setHm(HashMap<String, String> hm) {
        this.hm = hm;
    }

    /**
     * @return the name for a discreet value for this flag
     */
    public String getNameForValue(String valueStr) {
        if (hm != null) {
            if (hm.containsKey(valueStr)) {
                return hm.get(valueStr);
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "QualityFlag [bitOffset=" + bitOffset + ", numBits=" + numBits
                + ", name=" + name + ", packedName=" + packedName + "]";
    }

}