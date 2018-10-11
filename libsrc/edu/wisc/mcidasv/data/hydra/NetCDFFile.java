/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2018
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger logger = LoggerFactory.getLogger(NetCDFFile.class);
   private final Map<String, Variable> varMap = new HashMap<>();
   private final Map<String, String[]> varDimNames = new HashMap<>();
   private final Map<String, int[]> varDimLengths = new HashMap<>();
   private final Map<String, Class> varDataType = new HashMap<>();
   private final Map<String, String> varUnits = new HashMap<>();

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

     org.jdom2.Attribute attr1 = (((Element)list.get(0)).getAttributes()).get(0);
     attr1.setValue(filename);

     org.jdom2.Attribute attr2 = (((Element)list.get(1)).getAttributes()).get(0);
     attr2.setValue(other);

     XMLOutputter xmlOut = new XMLOutputter();
     String newStr = xmlOut.outputString(doc);
       logger.trace("union string:\n{}", newStr);
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
     return (dim != null) ? dim.getLength() : -1;
   }

   public float[] getFloatArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     return (float[]) readArray(array_name, start, count, stride);
   }

   public int[] getIntArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     return (int[]) readArray(array_name, start, count, stride);
   }

   public double[] getDoubleArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     return (double[]) readArray(array_name, start, count, stride);
   }

   public short[] getShortArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     return (short[]) readArray(array_name, start, count, stride);
   }

   public byte[] getByteArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     return (byte[]) readArray(array_name, start, count, stride);
   }

   public Object getArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     return readArray(array_name, start, count, stride);
   }

   protected synchronized Object readArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
     Variable var = varMap.get(array_name);
     if (var instanceof Structure) {
       Array array = Array.factory(getArrayType(array_name), count);
       Index2D idx = new Index2D(count);
       for (int i=0; i<count[0]; i++) {
         StructureData sData = ((Structure)var).readStructure(start[0]+i);
         StructureMembers sMembers = sData.getStructureMembers();
         for (int j=0; j<count[1]; j++) {
           Object obj = sData.getScalarObject(sMembers.getMember(start[1]+j));
           idx.set(i,j);
           array.setObject(idx, obj);
         }
       }
       return array.copyTo1DJavaArray();
     }
     else {
       List<Range> rangeList = new ArrayList<>(start.length);
       for (int i=0;i<start.length;i++) {
         Range rng = new Range(start[i], start[i]+(count[i]-1)*stride[i], stride[i]);
         rangeList.add(i, rng);
       }
       Array array = var.read(rangeList);
       return array.copyTo1DJavaArray();
     }
   }

   public HDFArray getGlobalAttribute(String attr_name) throws Exception {
     throw new Exception("NetCDFFile.getGlobalAttributes: Unimplemented");
   }

   public HDFArray getArrayAttribute(String array_name, String attr_name) throws Exception {
     Object array = null;
     DataType dataType = null;

     Variable var = varMap.get(array_name);
     if (var != null) {
        Attribute attr = var.findAttribute(attr_name);
        if (attr != null) {
           Array attrVals = attr.getValues();
           dataType = attr.getDataType();
           array = attrVals.copyTo1DJavaArray();
        }
     }

     if (array == null) {
        return null;
     }
     
     HDFArray harray = null;

     if (dataType.getPrimitiveClassType() == Float.TYPE) {
       harray = HDFArray.make((float[])array);
     }
     else if (dataType.getPrimitiveClassType() == Double.TYPE) {
       harray = HDFArray.make((double[])array);
     }
     else if (dataType == DataType.STRING) {
       harray = HDFArray.make((String[])array);
     }
     else if (dataType.getPrimitiveClassType() == Short.TYPE) {
       harray = HDFArray.make((short[])array);
     }
     else if (dataType.getPrimitiveClassType() == Integer.TYPE) {
       harray = HDFArray.make((int[])array);
     }
     return harray;
   }

   public void close() throws Exception {
     ncfile.close();
   }

   public Map<String, Variable> getVarMap() {
     return varMap;
   }

   public boolean hasArray(String name) {
       return varMap.get(name) != null;
   }

   public boolean hasDimension(String name) {
       return ncfile.findDimension(name) != null;
   }

   public NetcdfFile getNetCDFFile() {
	   return ncfile;
   }
   
   public static void main(String[] args) throws Exception {
     NetCDFFile ncfile = new NetCDFFile(args[0]);
     ncfile.close();
   }
}
