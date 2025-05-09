package edu.wisc.ssec.mcidasv.data.hydra;

import visad.FlatField;
import visad.VisADException;
import visad.RealType;
import visad.SetType;
import visad.FunctionType;
import visad.Set;
import visad.Unit;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class MultiDimensionAdapter {

    public static String array_name = "array_name";
    public static String range_name = "range_name";
    public static String scale_name = "scale_name";
    public static String offset_name = "offset_name";
    public static String fill_value_name = "fill_value_name";
    public static String product_name = "product_name";


    MultiDimensionReader reader = null;
    HashMap metadata = null;
    String arrayName = null;
    String[] array_dim_names = null;
    int[] array_dim_lengths  = null;
    int array_rank;
    Class arrayType;

    HashMap<String, String> dimNameMap = new HashMap<String, String>();

    RealType rangeType;

    RangeProcessor rangeProcessor = null;

    Unit[] rangeUnits = new Unit[]{null};

    String rangeName = null;

    public MultiDimensionAdapter() {
    }

    public MultiDimensionAdapter(MultiDimensionReader reader, HashMap metadata) {
        this.reader = reader;
        this.metadata = metadata;
        this.init();
    }

    public abstract HashMap getDefaultSubset();

    public abstract Set makeDomain(Object subset) throws Exception;

    private void init() {
        this.arrayName = (String) metadata.get("array_name");

        String[] suppliedDimNames = (String[]) metadata.get("array_dimension_names");
        if (suppliedDimNames != null) {
            array_dim_names = suppliedDimNames;
        }
        else {
            array_dim_names = reader.getDimensionNames(arrayName);
        }

        array_dim_lengths = reader.getDimensionLengths(arrayName);
        array_rank = array_dim_lengths.length;
        arrayType = reader.getArrayType(arrayName);

        for (int i=0; i<array_rank; i++) {
            dimNameMap.put(array_dim_names[i], array_dim_names[i]);
        }

        Iterator iter = metadata.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            Object val = metadata.get(key);
            if (!(val instanceof String)) continue;
            String name = (String) val;
            for (int kk=0; kk<array_rank; kk++) {
                if (array_dim_names[kk].equals(name)) {
                    dimNameMap.put(array_dim_names[kk], key);
                }
            }
        }

        if (metadata.get(range_name) != null) {
            rangeName = (String) metadata.get(range_name);
        } else {
            rangeName = (String) metadata.get(array_name);
    }

        rangeType = RealType.getRealType(rangeName, rangeUnits[0]);

        /** TODO could be a mis-match between supplied unit, and default
         unit of an existing RealType with same name. */
        if (rangeType == null) {
            rangeType = RealType.getRealType(rangeName);
        }

        /** Note: RangeProcessor must be set by extending class if needed */
    }

    public Subset getIndexes(HashMap select) {
        Subset subset = new Subset(array_rank);
        int[] start = subset.getStart();
        int[] count = subset.getCount();
        int[] stride = subset.getStride();

        Iterator iter = select.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String name = (String) metadata.get(key);

            if (name == null) name = key;

            for (int kk=0; kk<array_rank; kk++) {
                if (array_dim_names[kk].equals(name)) {
                    double[] coords = (double[]) select.get(key);

                    if (array_dim_lengths[kk] == 1) {
                        start[kk] = 0;
                        count[kk] = 1;
                        stride[kk] = 1;
                    }
                    else {
                        start[kk] = (int) coords[0];
                        count[kk] = (int) ((coords[1] - coords[0])/coords[2] + 1f);
                        stride[kk] = (int) coords[2];
                    }

                }
            }
        }
        return subset;
    }

    public int getDimensionLengthFromIndex(int idx) {
        return array_dim_lengths[idx];
    }

    public int getIndexOfDimensionName(String name) {
        int idx = -1;
        for (int k = 0; k < array_rank; k++) {
            if (name.equals(array_dim_names[k])) {
                idx = k;
            }
        }
        return idx;
    }

    public FlatField getData(Object subset) throws Exception {
        Set domainSet = makeDomain(subset);
        return makeFlatField(domainSet, subset);
    }

    public FlatField makeFlatField(Set domainSet, float[][] range) throws VisADException, RemoteException {
        FlatField f_field = makeFlatField(domainSet);
        f_field.setSamples(range, false);
        return f_field;
    }

    public FlatField makeFlatField(Set domainSet, double[][] range) throws VisADException, RemoteException {
        FlatField f_field = makeFlatField(domainSet);
        f_field.setSamples(range, false);
        return f_field;
    }

    public FlatField makeFlatField(Set domainSet) throws VisADException, RemoteException {
        FlatField f_field = new FlatField(new FunctionType(((SetType)domainSet.getType()).getDomain(), rangeType), domainSet);
        return f_field;
    }

    public FlatField makeFlatField(Set domainSet, Object subset) throws Exception {
        FlatField f_field = null;

        Object range = readArray(subset);

        if (range instanceof float[]) {
            float[] new_range = processRange((float[]) range, subset);
            f_field = makeFlatField(domainSet, new float[][] {new_range});
        }
        else if (range instanceof double[]) {
            double[] new_range = processRange((double[]) range, subset);
            f_field = makeFlatField(domainSet, new double[][] {new_range});
        } else if (range instanceof int[]) {
            float[] float_range = processRange((int[]) range, subset);
            f_field = makeFlatField(domainSet, new float[][]{float_range});
        } else if (range instanceof short[]) {
            float[] float_range = processRange((short[])range, subset);
            f_field = makeFlatField(domainSet, new float[][] {float_range});
        }
        else if (range instanceof byte[]) {
            float[] float_range = processRange((byte[])range, subset);
            f_field = makeFlatField(domainSet, new float[][] {float_range});
        }

        return f_field;
    }

    public RangeProcessor getRangeProcessor() {
        return rangeProcessor;
    }

    public void setRangeProcessor(RangeProcessor rangeProcessor) {
        this.rangeProcessor = rangeProcessor;
    }

    public float[] processRange(int[] range, Object subset) {
        if (rangeProcessor == null) {
            float[] f_range = new float[range.length];
            for (int i = 0; i < range.length; i++) f_range[i] = (float) range[i];
            return f_range;
        } else {
            return rangeProcessor.processRange(range, (HashMap) subset);
        }
    }

    public float[] processRange(short[] range, Object subset) {
        if (rangeProcessor == null) {
            float[] f_range = new float[range.length];
            for (int i=0; i<range.length;i++) f_range[i] = (float) range[i];
            return f_range;
        }
        else {
            return rangeProcessor.processRange(range, (HashMap) subset);
        }
    }

    public float[] processRange(byte[] range, Object subset) {
        if (rangeProcessor == null) {
            float[] f_range = new float[range.length];
            for (int i=0; i<range.length;i++) f_range[i] = (float) range[i];
            return f_range;
        }
        else {
            return rangeProcessor.processRange(range, (HashMap) subset);
        }
    }

    public float[] processRange(float[] range, Object subset) {
        if (rangeProcessor == null) {
            return range;
        }
        else {
            return rangeProcessor.processRange(range, (HashMap) subset);
        }
    }

    public double[] processRange(double[] range, Object subset) {
        if (rangeProcessor == null) {
            return range;
        }
        else {
            return rangeProcessor.processRange(range, (HashMap) subset);
        }
    }


    public Object readArray(Object subset) throws Exception {
        Subset select = getIndexes((HashMap) subset);
        int[] start = select.getStart();
        int[] count = select.getCount();
        int[] stride = select.getStride();

        return reader.getArray(arrayName, start, count, stride, subset);
    }

    public MultiDimensionReader getReader() {
        return reader;
    }

    public Object getMetadata() {
        return metadata;
    }

    public String getArrayName() {
        return arrayName;
    }

    public RealType getRangeType() {
        return rangeType;
    }

    public HashMap getSubsetFromLonLatRect(HashMap subset, double minLat, double maxLat,
                                                         double minLon, double maxLon) {
        throw new UnsupportedOperationException("Must be implemented by extending class");
    }

    public HashMap getSubsetFromLonLatRect(double minLat, double maxLat,
                                                         double minLon, double maxLon) {
        throw new UnsupportedOperationException("Must be implemented by extending class");
    }

    public HashMap getSubsetFromLonLatRect(double minLat, double maxLat,
                                                         double minLon, double maxLon,
                                                         int xStride, int yStride, int zStride) {
        throw new UnsupportedOperationException("Must be implemented by extending class");
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("{\n");
        for (
                Object o : metadata.keySet()) {
            String key = (String) o;
            Object value = metadata.get(key);
            if (value instanceof String) {
                result.append("\t").append(key).append(": ").append(value).append(",\n");
            }
        }
        result.append("}");
        return result.toString();
    }
}