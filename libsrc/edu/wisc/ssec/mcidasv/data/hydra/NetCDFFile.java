package edu.wisc.ssec.mcidasv.data.hydra;


import ucar.nc2.*;
import ucar.nc2.ncml.NcMLReader;
import ucar.ma2.*;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Map;

import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jdom2.Document;
import org.jdom2.Element;


public class NetCDFFile implements MultiDimensionReader {

    HashMap<String, Variable> varMap = new HashMap<>();
    HashMap<String, String[]> varDimNames = new HashMap<>();
    HashMap<String, int[]> varDimLengths = new HashMap<>();
    HashMap<String, Class> varDataType = new HashMap<>();
    HashMap<String, String> varUnits = new HashMap<>();

    NetcdfFile ncfile = null;

    public static NetCDFFile makeUnion(String filename, String other) throws Exception {
        Object obj = new Object();
        URL url = obj.getClass().getResource("/edu/wisc/ssec/mcidasv/data/hydra/resources/union.ncml");
        SAXBuilder builder = new SAXBuilder(false);
        Document doc = null;

        try {
            doc = builder.build(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();

        List list = root.getChildren();

        list = ((Element)list.get(1)).getChildren();

        org.jdom2.Attribute attr1 = (org.jdom2.Attribute) (((Element) list.get(0)).getAttributes()).get(0);
        attr1.setValue(filename);

        org.jdom2.Attribute attr2 = (org.jdom2.Attribute) (((Element) list.get(1)).getAttributes()).get(0);
        attr2.setValue(other);

        XMLOutputter xmlOut = new XMLOutputter();
        String newStr = xmlOut.outputString(doc);
        ByteArrayInputStream is = new ByteArrayInputStream(newStr.getBytes());
        return new NetCDFFile(is);
    }

    public NetCDFFile(InputStream is) throws Exception {
        ncfile = NcMLReader.readNcML(is, null);
        init();
    }

    public NetCDFFile(String filename) throws Exception {
        if (filename.endsWith(".ncml")) {
            java.io.FileReader rdr = new java.io.FileReader(filename);
            ncfile = NcMLReader.readNcML(rdr, null);
        }
        else {
            ncfile = NetcdfFile.open(filename);
        }
        init();
    }

    public NetCDFFile(String filename, org.jdom2.Element root) throws Exception {
        ncfile = NcMLReader.readNcML(filename, root, null);
        init();
    }

    private void init() throws Exception {
        Iterator varIter = ncfile.getVariables().iterator();
        while(varIter.hasNext()) {
            Variable var = (Variable) varIter.next();

            if (var instanceof Structure) {
                analyzeStructure((Structure) var);
                continue;
            }

            int rank = var.getRank();
            String varName = var.getFullName();
            varMap.put(varName, var);
            Iterator dimIter = var.getDimensions().iterator();
            String[] dimNames = new String[rank];
            int[] dimLengths = new int[rank];
            int cnt = 0;
            while(dimIter.hasNext()) {
                Dimension dim = (Dimension) dimIter.next();
                String dim_name = dim.getShortName();
                if (dim_name == null) dim_name = "dim"+cnt;
                dimNames[cnt] = dim_name;
                dimLengths[cnt] = dim.getLength();
                cnt++;
            }
            varDimNames.put(varName, dimNames);
            varDimLengths.put(varName, dimLengths);
            varDataType.put(varName, var.getDataType().getPrimitiveClassType());

            Attribute attr = var.findAttribute("units");
            if (attr != null) {
                String unitStr = attr.getStringValue();
                varUnits.put(varName, unitStr);
            }
        }
    }

    void analyzeStructure(Structure var) throws Exception {
        if ((var.getShape()).length == 0) {
            return;
        }
        String varName = var.getFullName();
        String[] dimNames = new String[2];
        int[] dimLengths = new int[2];
        int cnt = 0;
        dimLengths[0] = (var.getShape())[0];
        dimNames[0] = "dim" + cnt;

        cnt++;
        StructureData sData = var.readStructure(0);
        List memList = sData.getMembers();
        dimLengths[1] = memList.size();
        dimNames[1] = "dim" + cnt;

        varDimNames.put(varName, dimNames);
        varDimLengths.put(varName, dimLengths);
        varMap.put(varName, var);

        StructureMembers sMembers = sData.getStructureMembers();
        Object obj = sData.getScalarObject(sMembers.getMember(0));
        varDataType.put(varName, obj.getClass());
    }

    public DataType getArrayDataType(String array_name, int idx) throws Exception {
        Structure var = (Structure) this.varMap.get(array_name);
        StructureData sData = var.readStructure(0);
        StructureMembers sMembers = sData.getStructureMembers();
        sData.getScalarObject(sMembers.getMember(idx));
        return var.getDataType();
    }

    public Class getArrayType(String array_name, int idx) throws Exception {
        Structure var = (Structure) varMap.get(array_name);
        StructureData sData = var.readStructure(0);
        StructureMembers sMembers = sData.getStructureMembers();
        Object obj = sData.getScalarObject(sMembers.getMember(idx));
        return obj.getClass();
    }

    public Class getArrayType(String array_name) {
        return varDataType.get(array_name);
    }

    public String[] getDimensionNames(String array_name) {
        return varDimNames.get(array_name);
    }

    public int[] getDimensionLengths(String array_name) {
        return varDimLengths.get(array_name);
    }

    public String getArrayUnitString(String array_name) {
        return varUnits.get(array_name);
    }

    public int getDimensionLength(String dimName) {
        Dimension dim = ncfile.findDimension(dimName);
        if (dim != null) {
            return dim.getLength();
        } else {
            return -1;
        }
    }

    public float[] getFloatArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
       Variable var = varMap.get(array_name);
       if (var instanceof Structure) {
            Array array = Array.factory(getArrayDataType(array_name, start[1]), count);
           Index2D idx = new Index2D(count);

            for (int i = 0; i < count[0]; ++i) {
                StructureData sData = ((Structure) var).readStructure(start[0] + i * stride[0]);
               StructureMembers sMembers = sData.getStructureMembers();

                for (int j = 0; j < count[1]; ++j) {
                    StructureMembers.Member mem = sMembers.getMember(start[1] + j * stride[1]);
                    int size = mem.getSize();
                    if (size == 1) {
                        Object obj = sData.getScalarObject(mem);
                   idx.set(i,j);
                   array.setObject(idx, obj);
                    } else {
                        array = sData.getArray(mem);
               }
           }
            }

           return (float[]) array.get1DJavaArray(DataType.FLOAT);
       }
       else {
            ArrayList rangeList = new ArrayList();

            for (int i = 0; i < start.length; ++i) {
               Range rng = new Range(start[i], start[i]+(count[i]-1)*stride[i], stride[i]);
               rangeList.add(i, rng);
           }
           Array array = var.read(rangeList);
           return (float[]) array.get1DJavaArray(DataType.FLOAT);
       }
    }

    public int[] getIntArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
       Variable var = varMap.get(array_name);
       if (var instanceof Structure) {
            Array array = Array.factory(getArrayDataType(array_name, start[1]), count);
           Index2D idx = new Index2D(count);

            for (int i = 0; i < count[0]; ++i) {
                StructureData sData = ((Structure) var).readStructure(start[0] + i * stride[0]);
               StructureMembers sMembers = sData.getStructureMembers();

                for (int j = 0; j < count[1]; ++j) {
                    StructureMembers.Member mem = sMembers.getMember(start[1] + j * stride[1]);
                    int size = mem.getSize();
                    if (size == 1) {
                        Object obj = sData.getScalarObject(mem);
                   idx.set(i,j);
                   array.setObject(idx, obj);
                    } else {
                        array = sData.getArray(mem);
               }
           }
            }

           return (int[]) array.get1DJavaArray(DataType.INT);
       }
       else {
            ArrayList rangeList = new ArrayList();

            for (int i = 0; i < start.length; ++i) {
               Range rng = new Range(start[i], start[i]+(count[i]-1)*stride[i], stride[i]);
               rangeList.add(i, rng);
           }
           Array array = var.read(rangeList);
           return (int[]) array.get1DJavaArray(DataType.INT);
       }
    }

    public double[] getDoubleArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
       Variable var = varMap.get(array_name);
       if (var instanceof Structure) {
            Array array = Array.factory(getArrayDataType(array_name, start[1]), count);
           Index2D idx = new Index2D(count);

            for (int i = 0; i < count[0]; ++i) {
                StructureData sData = ((Structure) var).readStructure(start[0] + i * stride[0]);
               StructureMembers sMembers = sData.getStructureMembers();

                for (int j = 0; j < count[1]; ++j) {
                    StructureMembers.Member mem = sMembers.getMember(start[1] + j * stride[1]);
                    int size = mem.getSize();
                    if (size == 1) {
                        Object obj = sData.getScalarObject(mem);
                   idx.set(i,j);
                   array.setObject(idx, obj);
                    } else {
                        array = sData.getArray(mem);
               }
           }
            }

           return (double[]) array.get1DJavaArray(DataType.DOUBLE);
       }
       else {
            ArrayList rangeList = new ArrayList();

            for (int i = 0; i < start.length; ++i) {
               Range rng = new Range(start[i], start[i]+(count[i]-1)*stride[i], stride[i]);
               rangeList.add(i, rng);
           }
           Array array = var.read(rangeList);
           return (double[]) array.get1DJavaArray(DataType.DOUBLE);
       }
    }

    public short[] getShortArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
       Variable var = varMap.get(array_name);
       if (var instanceof Structure) {
            Array array = Array.factory(getArrayDataType(array_name, start[1]), count);
           Index2D idx = new Index2D(count);

            for (int i = 0; i < count[0]; ++i) {
                StructureData sData = ((Structure) var).readStructure(start[0] + i * stride[0]);
               StructureMembers sMembers = sData.getStructureMembers();

                for (int j = 0; j < count[1]; ++j) {
                    StructureMembers.Member mem = sMembers.getMember(start[1] + j * stride[1]);
                    int size = mem.getSize();
                    if (size == 1) {
                        Object obj = sData.getScalarObject(mem);
                   idx.set(i,j);
                   array.setObject(idx, obj);
                    } else {
                        array = sData.getArray(mem);
               }
           }
            }

           return (short[]) array.get1DJavaArray(DataType.SHORT);
       }
       else {
            ArrayList rangeList = new ArrayList();

            for (int i = 0; i < start.length; ++i) {
               Range rng = new Range(start[i], start[i]+(count[i]-1)*stride[i], stride[i]);
               rangeList.add(i, rng);
           }
           Array array = var.read(rangeList);
           return (short[]) array.get1DJavaArray(DataType.SHORT);
       }
    }

    public byte[] getByteArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
       Variable var = varMap.get(array_name);
       if (var instanceof Structure) {
            Array array = Array.factory(getArrayDataType(array_name, start[1]), count);
           Index2D idx = new Index2D(count);

            for (int i = 0; i < count[0]; ++i) {
                StructureData sData = ((Structure) var).readStructure(start[0] + i * stride[0]);
               StructureMembers sMembers = sData.getStructureMembers();

                for (int j = 0; j < count[1]; ++j) {
                    StructureMembers.Member mem = sMembers.getMember(start[1] + j * stride[1]);
                    int size = mem.getSize();
                    if (size == 1) {
                        Object obj = sData.getScalarObject(mem);
                   idx.set(i,j);
                   array.setObject(idx, obj);
                    } else {
                        array = sData.getArray(mem);
               }
           }
            }

           return (byte[]) array.get1DJavaArray(DataType.BYTE);
       }
       else {
            ArrayList rangeList = new ArrayList();

            for (int i = 0; i < start.length; ++i) {
               Range rng = new Range(start[i], start[i]+(count[i]-1)*stride[i], stride[i]);
               rangeList.add(i, rng);
           }
           Array array = var.read(rangeList);
           return (byte[]) array.get1DJavaArray(DataType.BYTE);
       }
    }

    public Object getArray(String array_name, int[] start, int[] count, int[] stride, Object obj) throws Exception {
        return this.readArray(array_name, start, count, stride);
    }

    protected synchronized Object readArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
        Variable var = varMap.get(array_name);
        if (var instanceof Structure) {
            Array array = Array.factory(getArrayDataType(array_name, start[1]), count);
            Index2D idx = new Index2D(count);
            for (int i=0; i<count[0]; i++) {
                StructureData sData = ((Structure) var).readStructure(start[0] + i * stride[0]);
                StructureMembers sMembers = sData.getStructureMembers();
                for (int j=0; j<count[1]; j++) {
                    StructureMembers.Member mem = sMembers.getMember(start[1] + j * stride[1]);
                    int size = mem.getSize();
                    if (size == 1) {
                        Object obj = sData.getScalarObject(mem);
                    idx.set(i,j);
                    array.setObject(idx, obj);
                    } else { // if size > 1 assume array and just return
                        array = sData.getArray(mem);
                    }
                }
            }
            return array.copyTo1DJavaArray();
        }
        else {
            ArrayList rangeList = new ArrayList();
            for (int i=0;i<start.length;i++) {
                Range rng = new Range(start[i], start[i]+(count[i]-1)*stride[i], stride[i]);
                rangeList.add(i, rng);
            }
            Array array = var.read(rangeList);
            return array.copyTo1DJavaArray();
        }
    }

    public Number getAttributeValue(String path, String attr_name) {
        Group grp = ncfile.findGroup(path);
        Attribute attr = grp.findAttribute(attr_name);
        Number num = attr.getNumericValue();
        return num;
    }

    public HDFArray getGlobalAttribute(String attr_name) throws Exception {
        Object array = null;
        DataType dataType = null;
        Array attrVals = null;

        Attribute attr = ncfile.findGlobalAttribute(attr_name);
        if (attr != null) {
            attrVals = attr.getValues();
            dataType = attr.getDataType();
            if (dataType.isNumeric()) {
                array = attrVals.copyTo1DJavaArray();
            }
        } else {
            return null;
        }

        HDFArray harray = null;

        if (dataType.getPrimitiveClassType() == Float.TYPE) {
            harray = HDFArray.make((float[]) array);
        } else if (dataType.getPrimitiveClassType() == Double.TYPE) {
            harray = HDFArray.make((double[]) array);
        } else if (dataType.getPrimitiveClassType() == Short.TYPE) {
            harray = HDFArray.make((short[]) array);
        } else if (dataType.getPrimitiveClassType() == Integer.TYPE) {
            harray = HDFArray.make((int[]) array);
        } else if (dataType.getPrimitiveClassType() == Byte.TYPE) {
            harray = HDFArray.make((byte[]) array);
        } else if (dataType == DataType.STRING) {
            int len = (int) attrVals.getSize();
            String[] sa = new String[len];
            for (int k = 0; k < sa.length; k++) {
                sa[k] = (String) attrVals.getObject(k);
            }
            harray = HDFArray.make(sa);
        }

        return harray;
    }

    public HDFArray getArrayAttribute(String array_name, String attr_name) throws Exception {
        Object array = null;
        DataType dataType = null;
        Array attrVals = null;

        Variable var = varMap.get(array_name);
        if (var != null) {
            Attribute attr = var.findAttribute(attr_name);
            if (attr != null) {
                attrVals = attr.getValues();
                dataType = attr.getDataType();
                if (dataType.isNumeric()) {
                array = attrVals.copyTo1DJavaArray();
            }
            } else {
            return null;
        }
        }

        HDFArray harray = null;

        if (dataType.getPrimitiveClassType() == Float.TYPE) {
            harray = HDFArray.make((float[])array);
        }
        else if (dataType.getPrimitiveClassType() == Double.TYPE) {
            harray = HDFArray.make((double[])array);
        } else if (dataType.getPrimitiveClassType() == Short.TYPE) {
            harray = HDFArray.make((short[])array);
        }
        else if (dataType.getPrimitiveClassType() == Integer.TYPE) {
            harray = HDFArray.make((int[])array);
        } else if (dataType.getPrimitiveClassType() == Byte.TYPE) {
            harray = HDFArray.make((byte[]) array);
        } else if (dataType == DataType.STRING) {
            int len = (int) attrVals.getSize();
            String[] sa = new String[len];
            for (int k = 0; k < sa.length; k++) {
                sa[k] = (String) attrVals.getObject(k);
        }
            harray = HDFArray.make(sa);
        }

        return harray;
    }

    public void close() throws Exception {
        ncfile.close();
    }

    public HashMap getVarMap() {
        return varMap;
    }

    public boolean hasArray(String name) {
        if (varMap.get(name) == null) {
            return false;
        } else {
            return true;
        }
    }

    public boolean hasDimension(String name) {
        if (ncfile.findDimension(name) != null) {
            return true;
        } else {
            return false;
        }
    }

    public NetcdfFile getNetCDFFile() {
        return ncfile;
    }

    public static void main(String[] args) throws Exception {
        NetCDFFile ncfile = new NetCDFFile(args[0]);
        ncfile.close();
    }
}