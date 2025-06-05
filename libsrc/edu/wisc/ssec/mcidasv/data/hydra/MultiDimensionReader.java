package edu.wisc.ssec.mcidasv.data.hydra;
import ucar.ma2.DataType;
import java.lang.Class;

public interface MultiDimensionReader {

    public float[] getFloatArray(String name, int[] start, int[] count, int[] stride) throws Exception;

    public double[] getDoubleArray(String name, int[] start, int[] count, int[] stride) throws Exception;

    public int[] getIntArray(String name, int[] start, int[] count, int[] stride) throws Exception;

    public short[] getShortArray(String name, int[] start, int[] count, int[] stride) throws Exception;

    public byte[] getByteArray(String name, int[] start, int[] count, int[] stride) throws Exception;

    public Object getArray(String name, int[] start, int[] count, int[] stride, Object obj) throws Exception;

    public Class getArrayType(String name);

    public String[] getDimensionNames(String arrayName);

    public int[] getDimensionLengths(String arrayName);

    public HDFArray getGlobalAttribute(String attrName) throws Exception;

    public HDFArray getArrayAttribute(String arrayName, String attrName) throws Exception;

    public Number getAttributeValue(String path, String attrName) throws Exception;

    public boolean hasArray(String arrayName);

    public void close() throws Exception;
}