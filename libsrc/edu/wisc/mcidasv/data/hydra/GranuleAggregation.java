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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.data.QualityFlag;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/**
 * Provides a view and operations on a set of contiguous data granules as if they 
 * were a single granule.
 * 
 * This file needs to implement the same signatures NetCDFFile does,
 * but for aggregations of consecutive granules.
 * 
 * @author tommyj
 *
 */

public class GranuleAggregation implements MultiDimensionReader {
 
	private static final Logger logger = LoggerFactory.getLogger(GranuleAggregation.class);
	
	// this structure holds the NcML readers that get passed in 
   List<NetcdfFile> nclist = new ArrayList<>();

   // this holds the MultiDimensionReaders, here NetCDFFile
   List<NetCDFFile> ncdfal = null;
   
   // need an ArrayList for each variable hashmap structure
   List<Map<String, Variable>> varMapList = new ArrayList<>();
   List<Map<String, String[]>> varDimNamesList = new ArrayList<>();
   List<Map<String, Class>> varDataTypeList = new ArrayList<>();

   // map of granule index and granule in-track length for each variable
   Map<String, Map<Integer, Integer>> varGranInTrackLengths = new HashMap<>();
   Map<String, int[]> varAggrDimLengths = new HashMap<>();
   
   // this object is used to handle granules like VIIRS Imagery EDRs, where scan
   // gaps of varying sizes and locations in the granule must be removed.  If 
   // present, an initial read with these "cut" ranges will be done before subsetting
   Map<Integer, List<Range>> granCutRanges = new HashMap<>();
   Map<Integer, Integer> granCutScans = new HashMap<>();
   
   // except quality flags - only need one hashmap per aggregation
   // it maps the broken out variable name back to the original packed variable name
   Map<String, QualityFlag> qfMap = null;
   
   // For those variables which are assembled from other variables based on LUTs
   Map<String, float[]> lutMap = null;

   // variable can have bulk array processor set by the application
   Map<String, RangeProcessor> varToRangeProcessor = new HashMap<>();
   
   private int granuleCount = -1;
   private String inTrackDimensionName = null;
   private String inTrackGeoDimensionName = null;
   private String crossTrackDimensionName = null;
   private Set<String> products;
   private String origName = "";
   // assume we are working with VIIRS, will toggle if not
   private boolean isVIIRS = true;

   public GranuleAggregation(List<NetCDFFile> ncdfal, Set<String> products,
		   String inTrackDimensionName, String inTrackGeoDimensionName, 
		   String crossTrackDimensionName, boolean isVIIRS) throws Exception {
	   if (ncdfal == null) throw new Exception("No data: empty Suomi NPP aggregation object");
	   this.inTrackDimensionName = inTrackDimensionName;
	   this.crossTrackDimensionName = crossTrackDimensionName;
	   this.inTrackGeoDimensionName = inTrackGeoDimensionName;
       this.ncdfal = ncdfal;
       this.products = products;
       this.isVIIRS = isVIIRS;
	   init(ncdfal);
   }
   
   public GranuleAggregation(List<NetCDFFile> ncdfal, Set<String> products,
		   String inTrackDimensionName, String inTrackGeoDimensionName, 
		   String crossTrackDimensionName) throws Exception {
	   this(ncdfal, products, inTrackDimensionName, inTrackGeoDimensionName, crossTrackDimensionName, false);
   }
   
   public GranuleAggregation(List<NetCDFFile> ncdfal, Set<String> products,
		   String inTrackDimensionName, String crossTrackDimensionName) throws Exception {
       this(ncdfal, products, inTrackDimensionName, inTrackDimensionName, crossTrackDimensionName, false);
   }
   
   public GranuleAggregation(List<NetCDFFile> ncdfal, Set<String> products,
		   String inTrackDimensionName, String crossTrackDimensionName, boolean isEDR) throws Exception {
        this(ncdfal, products, inTrackDimensionName, inTrackDimensionName, crossTrackDimensionName, isEDR);
   }

   public Class getArrayType(String array_name) {
	   array_name = mapNameIfQualityFlag(array_name);
	   array_name = mapNameIfLUTVar(array_name);
	   return varDataTypeList.get(0).get(array_name);
   }

   public String[] getDimensionNames(String array_name) {
	   array_name = mapNameIfQualityFlag(array_name);
	   array_name = mapNameIfLUTVar(array_name);
	   return varDimNamesList.get(0).get(array_name);
   }

   public int[] getDimensionLengths(String array_name) {
	   array_name = mapNameIfQualityFlag(array_name);
	   array_name = mapNameIfLUTVar(array_name);
	   return varAggrDimLengths.get(array_name);
   }

   private String mapNameIfQualityFlag(String array_name) {
	   // only applies if name is from a packed quality flag
	   // we pull data from the "mapped" variable name, a packed byte
	   if (qfMap != null) {
		   if (qfMap.containsKey(array_name)) {
			   origName = array_name;
			   QualityFlag qf = qfMap.get(array_name);
			   String mappedName = qf.getPackedName();
			   logger.debug("Key: " + array_name + " mapped to: " + mappedName);
			   return mappedName;
		   }
	   }
	   return array_name;
   }
   
   private String mapNameIfLUTVar(String array_name) {
	   // only applies if name is from a LUT pseudo variable
	   // we pull data from a "mapped" variable name, and apply a LUT to that variable

	   if (lutMap != null) {
		   if (lutMap.containsKey(array_name)) {
			   origName = array_name;
			   String mappedName = array_name.substring(0, array_name.length() - 3);
			   logger.debug("Key: " + array_name + " mapped to: " + mappedName);
			   return mappedName;
		   }
	   }
	   return array_name;
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

   public HDFArray getGlobalAttribute(String attr_name) throws Exception {
     throw new Exception("GranuleAggregation.getGlobalAttributes: Unimplemented");
   }

   public HDFArray getArrayAttribute(String array_name, String attr_name) throws Exception {
	   Variable var = varMapList.get(0).get(array_name);
	   if (var == null) return null;
	   
	   Attribute attr = var.findAttribute(attr_name);
	   if (attr == null) return null;
	   
	   Array attrVals = attr.getValues();
	   DataType dataType = attr.getDataType();
	   Object array = attrVals.copyTo1DJavaArray();

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
	   // close each NetCDF file
	   for (NetcdfFile n : nclist) {
		   n.close();
	   }
   }

   private void init(List<NetCDFFile> ncdfal) throws Exception {
	   
	   logger.debug("init in...");
	   // make a NetCDFFile object from the NcML for each granule
	   for (NetCDFFile n : ncdfal) {
		   logger.debug("loading another NetCDF file from NcML...");
		   NetcdfFile ncfile = n.getNetCDFFile();
		   nclist.add(ncfile);
	   }
	   
	   granuleCount = nclist.size();
	   logger.debug("Granule count: " + granuleCount);

       // All files do NOT have the same structure, so need to look at each ncfile
	   // For ex, some MODIS granules have slightly different in-track and along-track 
	   // lengths
	   
	   NetcdfFile ncfile = null;
	   for (int ncIdx = 0; ncIdx < nclist.size(); ncIdx++) {
		   
		   // good place to initialize the cut Range ArrayList for each granule
		   Integer granuleIndex = new Integer(ncIdx);
		   List<Range> al = new ArrayList<>();

		   int cutScanCount = 0;
		   
		   ncfile = nclist.get(ncIdx); 
		   
		   Iterator<Variable> varIter = ncfile.getVariables().iterator();
		   while (varIter.hasNext()) {
			   Variable var = varIter.next();
			   logger.trace("Variable " + var.getShortName() + ", Rank: " + var.getRank());
			   varAggrDimLengths.put(var.getFullName(), new int[var.getRank()]);
			   varGranInTrackLengths.put(var.getFullName(), new HashMap<>());
			   
			   // Here, let's try to check the data for EDR fill lines
			   // and if found, try to handle it by simply adjusting the dimensions
			   // for this granule.  Sound like a plan?  We'll see...
			   
			   // TJJ May 2016 
			   // "simply adjusting the dimensions" he says
			   // Anyway, we now do this check for EDRs and SDRs, it can manifest for both
			   
			   if (isVIIRS) {
				   
				   // look through lat grid, look for missing scans
				   String varName = var.getShortName();
				   if (varName.endsWith("Latitude")) {
					   // iterate through the scan lines, looking for fill lines
					   // NOTE: we only need to check the first column! so set
					   // up an appropriate Range to cut the read down significantly
					   int[] shape = var.getShape();
					   List<Range> alr = new ArrayList<>();
					   alr.add(new Range(0, shape[0] - 1, 1));
					   alr.add(new Range(0, 1, 1));
					   Array a = var.read(alr);
					   int granLength = shape[0];
					   int scanLength = shape[1];
					   Index index = a.getIndex();
					   float fVal = 0.0f;

					   int rangeOffset = 0;
					   boolean prvScanWasCut = false;
					   boolean needClosingRange = false;
					   boolean hadCutRanges = false;
					   boolean someMissing = false;

					   for (int i = 0; i < shape[0]; i++) {

						   someMissing = false;
						   fVal = a.getFloat(index.set(i, 0));
						   if (fVal < -90.0f) {
							   someMissing = true;
						   }

						   if (someMissing) {
							   hadCutRanges = true;
							   cutScanCount++;
							   logger.trace("Found a cut scan " + i + ", last val: " + fVal);
							   if ((prvScanWasCut) || (i == 0)) {
								   if (i == 0) {
									   rangeOffset = 0;
								   } else {
									   rangeOffset = i + 1;
								   }
							   } else {
								   try {
									   // We are using 2D ranges
									   logger.trace("Adding Range: " + rangeOffset
											   + ", " + (i - 1) + ", 1");
									   al.add(new Range(rangeOffset, i - 1, 1));
									   logger.trace("Adding Range: " + 0 + ", "
											   + (scanLength - 1) + ", 1");
									   al.add(new Range(0, scanLength - 1, 1));
								   } catch (Exception e) {
									   e.printStackTrace();
								   }
								   rangeOffset = i;
							   }
							   prvScanWasCut = true;
						   } else {
							   prvScanWasCut = false;
						   }

						   // check to see if closing Range needed, good data at end
						   if ((! prvScanWasCut) && (i == (granLength - 1))) {
						       if (hadCutRanges) {
							      needClosingRange = true;
						       }
						   }
					   }

					   if (needClosingRange) {
						   // We are using 2D ranges
                           logger.trace("Adding closing cut range: " + rangeOffset + ", " + (shape[0] - 1) + ", 1");
						   al.add(new Range(rangeOffset, shape[0] - 1, 1));
						   al.add(new Range(0, scanLength - 1, 1));
					   }

					   // if only one contiguous range, process as a normal clean granule
					   if (! hadCutRanges) {
						   al.clear();
					   }

					   logger.debug("Total scans cut this granule: " + cutScanCount);

				   }
			   }
		   }
		   granCutScans.put(granuleIndex, new Integer(cutScanCount));
	       granCutRanges.put(granuleIndex, al);
	   }
	   
	   for (int ncIdx = 0; ncIdx < nclist.size(); ncIdx++) {
		   
		   ncfile = nclist.get(ncIdx);
		   
		   Map<String, Variable> varMap = new HashMap<>();
		   Map<String, String[]> varDimNames = new HashMap<>();
		   Map<String, Class> varDataType = new HashMap<>();
		   
		   Iterator<Variable> varIter = ncfile.getVariables().iterator();
		   int varInTrackIndex = -1;
		   while (varIter.hasNext()) {
			   Variable var = varIter.next();
			   
			   boolean foundProduct = false;
			   for (String s : products) {
				   if (s.contains(var.getFullName())) {
					   logger.trace("Valid product: " + var.getFullName());
					   foundProduct = true;
					   break;
				   }
			   }
			   
			   if (! foundProduct) {
				   logger.trace("Skipping variable: " + var.getFullName());
				   continue;
			   }
			   
			   if (var instanceof Structure) {
				   	// simply skip these, applicable only to IASI far as I know
				   	continue;
			   }

			   int rank = var.getRank();
			   
			   // bypass any less-than-2D variables for now...
			   if (rank < 2) {
				   continue;
			   }
			   
			   String varName = var.getFullName();
			   varMap.put(varName, var);
			   Iterator<Dimension> dimIter = var.getDimensions().iterator();
			   String[] dimNames = new String[rank];
			   int[] dimLengths = new int[rank];
			   int cnt = 0;
			   boolean notDisplayable = false;
			   varInTrackIndex = getInTrackIndex(var);

			   while (dimIter.hasNext()) {
				   Dimension dim = dimIter.next();
				   String s = dim.getShortName();
				   if ((s != null) && (!s.isEmpty())) {
					   if ((! s.equals(inTrackDimensionName)) && 
							   ((! s.startsWith("Band")) && (cnt == 0)) &&
							   (! varName.endsWith("Latitude")) &&
							   (! varName.endsWith("latitude")) &&
							   (! varName.endsWith("Latitude_TC")) &&
							   (! varName.endsWith("Longitude")) &&
							   (! varName.endsWith("longitude")) &&
							   (! varName.endsWith("Longitude_TC")) &&
							   (! s.equals(crossTrackDimensionName))) {
						   notDisplayable = true;
						   break;
					   }
				   }
				   String dimName = dim.getShortName();
				   logger.debug("GranuleAggregation init, variable: " + varName + ", dimension name: " + dimName + ", length: " + dim.getLength());
				   if (dimName == null)  dimName = "dim" + cnt;
				   if (dimName.isEmpty()) {
					   dimName = "dim" + cnt;
				   }
				   dimNames[cnt] = dimName;
				   dimLengths[cnt] = dim.getLength();
				   cnt++;
			   }
			   
			   // skip to next variable if it's not displayable data
			   if (notDisplayable) continue;
			   
			   // adjust in-track dimension if needed (scans were cut)
			   int cutScans = 0;
			   if (! granCutScans.isEmpty() && granCutScans.containsKey(ncIdx)) {
			       cutScans = granCutScans.get(new Integer(ncIdx));
			   } else {
			       granCutScans.put(new Integer(ncIdx), new Integer(0));
			   }
			   dimLengths[varInTrackIndex] = dimLengths[varInTrackIndex] - cutScans;
			   
			   // XXX TJJ - can below block go away?  Think so...
			   int[] aggrDimLengths = varAggrDimLengths.get(varName);
			   for (int i = 0; i < rank; i++) {
				   if (i == varInTrackIndex) {
					   aggrDimLengths[i] += dimLengths[i];
				   } else {
					   aggrDimLengths[i] = dimLengths[i];
				   }
			   }
			   
			   varDimNames.put(varName, dimNames);
			   varDataType.put(varName, var.getDataType().getPrimitiveClassType());

			   if (varInTrackIndex < 0) {
				   logger.debug("Skipping variable with unknown dimension: " + var.getFullName());
				   continue;
			   }

			   Map<Integer, Integer> granIdxToInTrackLen = varGranInTrackLengths.get(varName);
			   granIdxToInTrackLen.put(ncIdx, new Integer(dimLengths[varInTrackIndex]));
			   
			   dimLengths[varInTrackIndex] = dimLengths[varInTrackIndex] * granuleCount;
			   varDataType.put(varName, var.getDataType().getPrimitiveClassType());
		   }
		   
		   // add the new hashmaps to our enclosing lists
		   varMapList.add(varMap);
		   varDimNamesList.add(varDimNames);
		   varDataTypeList.add(varDataType);
		   
	   }
   }
   
   /**
    * Based on the names of the variable dimensions, determine the in-track index.
    *
    * @param v {@code Variable} that {@literal "contains"} dimension names that
    * allow for inference of the in-track index. {@code null} is allowed.
    *
    * @return correct index (0 or greater), or -1 if error.
    */
   
   private int getInTrackIndex(Variable v) {
	   
	   int index = -1;
	   boolean is2D = false;
	   boolean is3D = false;
	   
	   String inTrackName = null;
	    
	   // typical sanity check
	   if (v == null) return index;
	   
	   // lat/lon vars have different dimension names
	   if ((v.getFullName().endsWith("Latitude")) || 
			   (v.getFullName().endsWith("Latitude_TC")) ||
			   (v.getFullName().endsWith("Longitude")) ||
			   (v.getFullName().endsWith("latitude")) ||
			   (v.getFullName().endsWith("longitude")) ||
			   (v.getFullName().endsWith("Longitude_TC"))) {
		   if ((v.getFullName().startsWith("All_Data")) || 
			   (v.getFullName().startsWith("observation_data"))) {
			   inTrackName = inTrackDimensionName;
		   } else {
			   inTrackName = inTrackGeoDimensionName;
		   }
	   } else {
		   inTrackName = inTrackDimensionName;
	   }
	   // pull out the dimensions
	   List<Dimension> dList = v.getDimensions();
	   
	   // right now, we only handle 2D and 3D variables.
	   // TJJ XXX it does get trickier, and we will have to expand this
	   // to deal with for example CrIS data...
	   int numDimensions = dList.size();
	   
	   // the only 4D data right now is CrIS, return 0
	   if (numDimensions == 4) return 0;
	   
	   if ((numDimensions == 2) || (numDimensions == 3)) {
		   if (numDimensions == 2) is2D = true;
		   if (numDimensions == 3) is3D = true;
	   } else {
		   return index;
	   }
	   
	   // if the data is 2D, we use the SwathAdapter class,
	   // if 3D, we use the SpectrumAdapter class
	   for (int i = 0; i < numDimensions; i++) {
		   if (is2D) {
			   // XXX TJJ - if empty name, in-track index is 0
			   if ((dList.get(i).getShortName() == null) || (dList.get(i).getShortName().isEmpty())) {
				   logger.trace("Empty dimension name!, assuming in-track dim is 0");
				   return 0;
			   }
			   if (dList.get(i).getShortName().equals(inTrackName)) {
				   index = i;
				   break;
			   }
		   }
		   if (is3D) {
			   // XXX TJJ - if empty name, in-track index is 0
			   if ((dList.get(i).getShortName() == null) || (dList.get(i).getShortName().isEmpty())) {
				   logger.debug("Empty dimension name!, assuming in-track dim is 0");
				   return 0;
			   }
			   if (dList.get(i).getShortName().equals(inTrackName)) {
				   index = i;
				   break;
			   }
		   }
	   }
	   
	   // hopefully we found the right one
	   return index;
   }
   
   private synchronized Object readArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
	   
	   String mapName = array_name;
	   array_name = mapNameIfQualityFlag(array_name);
	   array_name = mapNameIfLUTVar(array_name);
	   // how many dimensions are we dealing with
	   int dimensionCount = start.length;
	   
	   // pull out a representative variable so we can determine which index is in-track
	   Variable vTmp = varMapList.get(0).get(array_name);
	   logger.trace("");
	   logger.trace("Working on var: " + array_name);
	   int vInTrackIndex = getInTrackIndex(vTmp);
	   
	   int loGranuleId = 0;
	   int hiGranuleId = 0;

	   Map<Integer, Integer> granIdxToInTrackLen = varGranInTrackLengths.get(array_name);
	   int numGrans = granIdxToInTrackLen.size();

	   int[] vGranuleLengths = new int[numGrans];
	   for (int k = 0; k < numGrans; k++) {
		   vGranuleLengths[k] = granIdxToInTrackLen.get(k);
		   logger.debug("readArray, gran len: " + vGranuleLengths[k] + ", scans cut: " + granCutScans.get(k));
	   }

	   int strt = start[vInTrackIndex];
	   int stp = strt + (count[vInTrackIndex] - 1) * stride[vInTrackIndex];
	   int cnt = 0;
	   for (int k = 0; k < numGrans; k++) {
		   int granLen = granIdxToInTrackLen.get(k);
		   cnt += granLen;
		   if (strt < cnt) {
			   loGranuleId = k;
			   break;
		   }
	   }

	   cnt = 0;
	   for (int k = 0; k < numGrans; k++) {
		   int granLen = granIdxToInTrackLen.get(k);
		   cnt += granLen;
		   if (stp < cnt) {
			   hiGranuleId = k;
			   break;
		   }
	   }
       
       int totalScansCut = 0;
       for (int k = loGranuleId; k <= hiGranuleId; k++) {
           totalScansCut += granCutScans.get(k); 
       }
       logger.debug("Scans cut for this selection: " + totalScansCut);
       
	   // next, we break out the offsets, counts, and strides for each granule
	   int granuleSpan = hiGranuleId - loGranuleId + 1;
	   
	   logger.debug("readArray req, loGran: " + loGranuleId + ", hiGran: " + 
			   hiGranuleId + ", granule span: " + granuleSpan + ", dimCount: " + dimensionCount);
	   
	   for (int i = 0; i < dimensionCount; i++) {
		   logger.debug("start[" + i + "]: " + start[i]);
		   logger.debug("count[" + i + "]: " + count[i]);
		   logger.debug("stride[" + i + "]: " + stride[i]);
	   }

	   int [][] startSet = new int [granuleSpan][dimensionCount];
	   int [][] countSet = new int [granuleSpan][dimensionCount];
	   int [][] strideSet = new int [granuleSpan][dimensionCount];
	   int countSubtotal = 0;

       int inTrackTotal = 0;
       for (int i = 0; i < loGranuleId; i++) {
           inTrackTotal += vGranuleLengths[i];
       }
	   
	   // this part is a little tricky - set the values for each granule we need to access for this read
	   for (int i = 0; i < granuleSpan; i++) {
	       
           inTrackTotal += vGranuleLengths[loGranuleId + i];
           
		   for (int j = 0; j < dimensionCount; j++) {
			   // for all indeces other than the in-track index, the numbers match what was passed in
			   if (j != vInTrackIndex) {
				   startSet[i][j] = start[j];
				   countSet[i][j] = count[j] * stride[j];
				   strideSet[i][j] = stride[j];  
			   } else {
				   // for the in-track index, it's not so easy...
				   // for first granule, start is what's passed in
				   if (i == 0) {
				       if (inTrackTotal > vGranuleLengths[loGranuleId + i]) {
					       startSet[i][j] = 
					          (start[j] % (inTrackTotal - vGranuleLengths[loGranuleId + i]));
				       } else {
				           startSet[i][j] = start[j];
				       }
				   } else {  
				       startSet[i][j] = 
                          ((inTrackTotal - (vGranuleLengths[loGranuleId + i])) % stride[j]);
				       // if there is a remainder, need to offset stride - remainder into next gran
				       if (startSet[i][j] != 0) startSet[i][j] = stride[j] - startSet[i][j];
				   }
                   
				   // counts may be different for start, end, and middle granules
				   if (i == 0) {
					   // is this the first and only granule?
					   if (granuleSpan == 1) {
						   countSet[i][j] = count[j] * stride[j];
		                   // TJJ May 2016
		                   // This condition manifests because there are times when 
		                   // "fill" scans are cut from otherwise fine granules.
	                       // e.g., to the chooser it may look like there are 3072 valid lines,
                           // but by the time we get here we realize there are really 3056
		                   // This typically shortens the granule by one scan (16 lines)
		                   if (countSet[i][j] > (vGranuleLengths[loGranuleId+i])) 
		                       countSet[i][j] = vGranuleLengths[loGranuleId+i];

					   // or is this the first of multiple granules...
					   } else {
						   if ((inTrackTotal - start[j]) < (count[j] * stride[j])) {	
                               countSet[i][j] = inTrackTotal - start[j];
						   } else {
							   countSet[i][j] = count[j] * stride[j];
						   }
						   countSubtotal += countSet[i][j];
					   }
				   } else {
					   // middle granules
					   if (i < (granuleSpan - 1)) {
						   countSet[i][j] = vGranuleLengths[loGranuleId+i] - startSet[i][j];
						   countSubtotal += countSet[i][j];
					   } else {
						   // the end granule
						   countSet[i][j] = (count[j] * stride[j]) - countSubtotal - startSet[i][j];
		                   // TJJ May 2016
		                   // This condition manifests because there are times when 
		                   // "fill" scans are cut from otherwise fine granules.
						   // e.g., to the chooser it may look like there are 3072 valid lines,
						   // but by the time we get here we realize there are really 3056
		                   // This typically shortens the granule by one scan (16 lines)
		                   if (countSet[i][j] > (vGranuleLengths[loGranuleId+i] - startSet[i][j])) 
		                       countSet[i][j] = vGranuleLengths[loGranuleId+i] - startSet[i][j];
					   }
				   }
				   // luckily, stride never changes
				   strideSet[i][j] = stride[j];
			   }
		   }
	   }
	   
	   int totalLength = 0;
	   int rangeListCount = 0;
	   List<Array> arrayList = new ArrayList<>();
	   for (int granuleIdx = 0; granuleIdx < granuleCount; granuleIdx++) {
		   if ((granuleIdx >= loGranuleId) && (granuleIdx <= hiGranuleId)) {
			   Variable var = varMapList.get(loGranuleId + (granuleIdx-loGranuleId)).get(array_name);

			   if (var instanceof Structure) {
				   // what to do here?
			   } else {
				   List<Range> rangeList = new ArrayList<>();
				   for (int dimensionIdx = 0; dimensionIdx < dimensionCount; dimensionIdx++) {
					   logger.debug("Creating new Range: " + startSet[rangeListCount][dimensionIdx] +
							   ", " + (startSet[rangeListCount][dimensionIdx] + countSet[rangeListCount][dimensionIdx] - 1) + ", " + strideSet[rangeListCount][dimensionIdx]);
					   Range range = new Range(
							   startSet[rangeListCount][dimensionIdx], 
							   startSet[rangeListCount][dimensionIdx] + countSet[rangeListCount][dimensionIdx] - 1,
							   strideSet[rangeListCount][dimensionIdx]
					   );
					   rangeList.add(dimensionIdx, range);
				   }
				   rangeListCount++;
				   
				   // If there were chunks of fill data to remove...
				   List<Range> al = granCutRanges.get(new Integer(granuleIdx));
				   if (! al.isEmpty()) {
					   List<Variable> varChunks = new ArrayList<>();
					   for (int rangeCount = 0; rangeCount < al.size(); rangeCount+=2) {
						   List<Range> rl = new ArrayList<>();
						   rl.add(al.get(rangeCount));
						   rl.add(al.get(rangeCount + 1));
						   varChunks.add(var.section(rl));
					   }

					   int [] newShape = var.getShape();
					   int cutScans = granCutScans.get(granuleIdx);
					   newShape[0] = newShape[0] - cutScans;
					   logger.trace("New Shape: " + newShape[0] + ", " + newShape[1]);
					   Array single = Array.factory(var.getDataType(), newShape);

					   // now read variable chunk data into single contiguous array
					   int idx = 0;
					   for (Variable v : varChunks) {
						   Array data = v.read();
						   int [] tmpShape = v.getShape();
						   for (int tIdx = 0; tIdx < tmpShape.length; tIdx++) {
							   logger.trace("Shape[" + tIdx + "]: " + tmpShape[tIdx]);
						   }
						   IndexIterator ii = data.getIndexIterator();
						   while (ii.hasNext()) {
							   single.setFloat(idx, ii.getFloatNext());
							   idx++;
						   }
					   }

					   // finally, apply subset ranges
					   logger.debug("Size of cut src array: " + single.getSize());
					   Array subarray = single.section(rangeList);
					   totalLength += subarray.getSize();
					   arrayList.add(subarray);
					   logger.debug("Size of cut sub array: " + subarray.getSize());

				   } else {
					   Array subarray = var.read(rangeList);
					   totalLength += subarray.getSize();
					   logger.debug("Size of reg sub array: " + subarray.getSize());
					   arrayList.add(subarray);
				   }
				   
			   }
			   // put in an empty ArrayList placeholder to retain a slot for each granule
		   } else {
			   Array emptyArray = null;
			   arrayList.add(emptyArray);
		   }
	   }
	   
	   // last, concatenate the individual NetCDF arrays pulled out 

	   Class arrayType = getArrayType(array_name);
	   RangeProcessor rngProcessor = varToRangeProcessor.get(array_name);

	   logger.debug("Creating aggregated array, totalLength: " + totalLength);
	   
	   // TJJ May 2016
	   // I'm starting to think there may be a bug in the Java NetCDF section/subarray
	   // code - I have chased this quite a bit with no solution, the total length 
	   // sectioned out sometimes exceeds the total requested. It's almost as if a
	   // previous section length is retained. Anyway, as a hack for now I will just
	   // truncate if this occurs. We also need to watch for overflow in the arraycopy
	   // calls below
	   
	   if (count.length < 3) {
	       if (totalLength > (count[0] * count[1])) {
	           totalLength = count[0] * count[1];
	       }
	   }
	   
	   float[] finalArray = new float[totalLength];
           
	   int destPos = 0;
	   int granIdx = 0;

	   int remaining = totalLength;
	   for (Array a : arrayList) {
		   if (a != null) {
			   Object primArray = a.copyTo1DJavaArray();
			   primArray = processArray(
			      mapName, array_name, arrayType, granIdx, primArray, rngProcessor, start, count
			   );
			   if (a.getSize() > remaining) {
			       System.arraycopy(primArray, 0, finalArray, destPos, remaining);
			   } else {
			       System.arraycopy(primArray, 0, finalArray, destPos, (int) a.getSize());
			   }
			   destPos += a.getSize();
			   remaining -= (int) a.getSize();
		   }
		   granIdx++;
	   }
       
	   return finalArray;
   }
   
   /**
    * @param qfMap the qfMap to set
    */
   
   public void setQfMap(Map<String, QualityFlag> qfMap) {
	   this.qfMap = qfMap;
   }
   
   /**
    * @param lutMap the lutMap to set
    */
   
   public void setLUTMap(Map<String, float[]> lutMap) {
	   this.lutMap = lutMap;
   }   

   public Map<String, Variable> getVarMap() {
	   return varMapList.get(0);
   }

   public List<NetCDFFile> getReaders() {
     return this.ncdfal;
   }

   /* pass individual granule pieces just read from dataset through the RangeProcessor */
   private Object processArray(String mapName, String array_name, Class arrayType, int granIdx, Object values, RangeProcessor rngProcessor, int[] start, int[] count) {

	   if (rngProcessor == null) {
		   return values;
	   }
	   else {
		   ((AggregationRangeProcessor) rngProcessor).setWhichRangeProcessor(granIdx);

		   Object outArray = null;

		   if (arrayType == Short.TYPE) {
			   // if variable is a LUT var, apply LUT
			   if ((lutMap != null) && (lutMap.containsKey(mapName))) {
				   float lut[] = lutMap.get(mapName);
				   outArray = rngProcessor.processRangeApplyLUT((short []) values, lut);
			   } else {
				   outArray = rngProcessor.processRange((short[]) values, null);
			   }
		   } else if (arrayType == Byte.TYPE) {
			   // if variable is a bit-field quality flag, apply mask
			   if ((qfMap != null) && (qfMap.containsKey(origName))) {
				   QualityFlag qf = qfMap.get(origName);
				   outArray = rngProcessor.processRangeQualityFlag((byte[]) values, null, qf);
			   } else {
				   outArray = rngProcessor.processRange((byte[]) values, null);
			   }
		   } else if (arrayType == Float.TYPE) {
			   outArray = rngProcessor.processRange((float[]) values, null);
		   } else if (arrayType == Double.TYPE) {
			   outArray = rngProcessor.processRange((double[]) values, null);
		   }

		   return outArray;
	   }
   }

   /* Application can supply a RangeProcessor for a variable 'arrayName' */
   public void addRangeProcessor(String arrayName, RangeProcessor rangeProcessor) {
	   varToRangeProcessor.put(arrayName, rangeProcessor);
   }
   
}
