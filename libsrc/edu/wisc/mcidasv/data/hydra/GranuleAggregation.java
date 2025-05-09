package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import ucar.ma2.Array;
import ucar.ma2.DataType;
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

	// this structure holds the NcML readers that get passed in
	ArrayList<NetcdfFile> nclist = new ArrayList<NetcdfFile>();

	// this holds the MultiDimensionReaders, here NetCDFFile
	ArrayList<NetCDFFile> ncdfal = null;

	// need an ArrayList for each variable hashmap structure
	ArrayList<HashMap<String, Variable>> varMapList = new ArrayList<HashMap<String, Variable>>();
	ArrayList<HashMap<String, String[]>> varDimNamesList = new ArrayList<HashMap<String, String[]>>();
	HashMap<String, String[]> varDimNames = new HashMap<String, String[]>();
	ArrayList<HashMap<String, int[]>> varDimLengthsList = new ArrayList<HashMap<String, int[]>>();
	ArrayList<HashMap<String, Class>> varDataTypeList = new ArrayList<HashMap<String, Class>>();

	HashMap<String, HashMap<Integer, Integer>> varGranInTrackLengths = new HashMap<String, HashMap<Integer, Integer>>();
	HashMap<String, Integer> varToAggrInTrackLength = new HashMap<String, Integer>();
	HashMap<Integer, Integer> varToAggrInTrackIndex;
	HashMap<String, int[]> varAggrDimLengths = new HashMap<String, int[]>();

	// variable can have bulk array processor set by the application
	HashMap<String, RangeProcessor> varToRangeProcessor = new HashMap<String, RangeProcessor>();

	private int granuleCount = -1;
	private String inTrackDimensionName = null;
	private TreeSet<String> products;
	private String origName = null;

	public GranuleAggregation(ArrayList<NetCDFFile> ncdfal, String inTrackDimensionName) throws Exception {
		if (ncdfal == null) throw new Exception("No data: empty Suomi NPP aggregation object");
		this.inTrackDimensionName = inTrackDimensionName;
		this.ncdfal = ncdfal;
		init(ncdfal);
	}

	public GranuleAggregation(ArrayList<NetCDFFile> ncdfal, HashMap<Integer, Integer> varToInTrackIndex) throws Exception {
		if (ncdfal == null) throw new Exception("No data: empty Suomi NPP aggregation object");
		this.varToAggrInTrackIndex = varToInTrackIndex;
		this.ncdfal = ncdfal;
		init(ncdfal);
	}

	public Class getArrayType(String array_name) {
		return varDataTypeList.get(0).get(array_name);
	}

	public String[] getDimensionNames(String array_name) {
		return varDimNames.get(array_name);
	}

	public int[] getDimensionLengths(String array_name) {
		return varAggrDimLengths.get(array_name);
	}

	public float[] getFloatArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
		return (float[]) readArray(array_name, start, count, stride, null);
	}

	public int[] getIntArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
		return (int[]) readArray(array_name, start, count, stride, null);
	}

	public double[] getDoubleArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
		return (double[]) readArray(array_name, start, count, stride, null);
	}

	public short[] getShortArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
		return (short[]) readArray(array_name, start, count, stride, null);
	}

	public byte[] getByteArray(String array_name, int[] start, int[] count, int[] stride) throws Exception {
		return (byte[]) readArray(array_name, start, count, stride, null);
	}

	public Object getArray(String array_name, int[] start, int[] count, int[] stride, Object obj) throws Exception {
		return readArray(array_name, start, count, stride, obj);
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
			harray = HDFArray.make((float[]) array);
		} else if (dataType.getPrimitiveClassType() == Double.TYPE) {
			harray = HDFArray.make((double[]) array);
		} else if (dataType == DataType.STRING) {
			harray = HDFArray.make((String[]) array);
		} else if (dataType.getPrimitiveClassType() == Short.TYPE) {
			harray = HDFArray.make((short[]) array);
		} else if (dataType.getPrimitiveClassType() == Integer.TYPE) {
			harray = HDFArray.make((int[]) array);
		}
		return harray;
	}

	public Number getAttributeValue(String path, String attrName) throws Exception {
		return null;
	}

	public void close() throws Exception {
		// close each NetCDF file
		for (NetcdfFile n : nclist) {
			n.close();
		}
	}

	private void init(ArrayList<NetCDFFile> ncdfal) throws Exception {

		// make a NetCDFFile object from the NcML for each granule
		for (NetCDFFile n : ncdfal) {
			NetcdfFile ncfile = n.getNetCDFFile();
			nclist.add(ncfile);
		}

		granuleCount = nclist.size();

		NetcdfFile ncfile = nclist.get(0); //All files have to have same structure
		Iterator<Variable> varIter = ncfile.getVariables().iterator();
		while (varIter.hasNext()) {
			Variable var = varIter.next();
			varAggrDimLengths.put(var.getFullName(), new int[var.getRank()]);
			varGranInTrackLengths.put(var.getFullName(), new HashMap<Integer, Integer>());
		}


		for (int ncIdx = 0; ncIdx < nclist.size(); ncIdx++) {

			ncfile = nclist.get(ncIdx);

			HashMap<String, Variable> varMap = new HashMap<String, Variable>();
			HashMap<String, int[]> varDimLengths = new HashMap<String, int[]>();
			HashMap<String, Class> varDataType = new HashMap<String, Class>();

			varIter = ncfile.getVariables().iterator();
			int varInTrackIndex = -1;
			while (varIter.hasNext()) {
				Variable var = (Variable) varIter.next();


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
				varInTrackIndex = getInTrackIndex(var);

				while (dimIter.hasNext()) {
					Dimension dim = dimIter.next();
					String s = dim.getShortName();
					dimNames[cnt] = s;
					dimLengths[cnt] = dim.getLength();

					if ((s != null) && (!s.isEmpty())) {
					} else {
						String dimName = "dim" + cnt;
						dimNames[cnt] = dimName;
					}
					cnt++;
				}

				varDimNames.put(varName, dimNames);
				varDataType.put(varName, var.getDataType().getPrimitiveClassType());


				int[] aggrDimLengths = varAggrDimLengths.get(varName);
				for (int i = 0; i < rank; i++) {
					if (i == varInTrackIndex) {
						aggrDimLengths[i] += dimLengths[i];
					} else {
						aggrDimLengths[i] = dimLengths[i];
					}
				}

				if (varInTrackIndex < 0) {
					continue;
				}

				HashMap<Integer, Integer> granIdxToInTrackLen = varGranInTrackLengths.get(varName);
				granIdxToInTrackLen.put(ncIdx, Integer.valueOf(dimLengths[varInTrackIndex]));

				dimLengths[varInTrackIndex] = dimLengths[varInTrackIndex] * granuleCount;
				varDataType.put(varName, var.getDataType().getPrimitiveClassType());
			}

			// add the new hashmaps to our enclosing lists
			varMapList.add(varMap);
			varDimNamesList.add(varDimNames);
			varDataTypeList.add(varDataType);

			varDimLengthsList.add(varDimLengths);
		}
	}

	/**
	 * Based on the names of the variable dimensions, determine the in-track index
	 *
	 * @param v the Variable
	 * @return correct index (0 or greater), or -1 if error
	 */

	private int getInTrackIndex(Variable v) {

		int index = -1;
		boolean is2D = false;
		boolean is3D = false;

		String inTrackName = inTrackDimensionName;

		// typical sanity check
		if (v == null) return index;

		// pull out the dimensions
		List<Dimension> dList = v.getDimensions();

		// right now, we only handle 2D and 3D variables.
		// TJJ XXX it does get trickier, and we will have to expand this
		// to deal with for example CrIS data...
		int numDimensions = dList.size();

		if (inTrackDimensionName == null) {
			Integer dims = Integer.valueOf(numDimensions);
			if (varToAggrInTrackIndex.containsKey(dims)) {
				int inTrackIndex = varToAggrInTrackIndex.get(dims);
				return inTrackIndex;
			} else {
				return 0;
			}
		}

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

	private synchronized Object readArray(String array_name, int[] start, int[] count, int[] stride, Object obj) throws Exception {

		// how many dimensions are we dealing with
		int dimensionCount = start.length;

		// pull out a representative variable so we can determine which index is in-track
		Variable vTmp = varMapList.get(0).get(array_name);
		int vInTrackIndex = getInTrackIndex(vTmp);

		int loGranuleId = 0;
		int hiGranuleId = 0;

		HashMap<Integer, Integer> granIdxToInTrackLen = varGranInTrackLengths.get(array_name);
		int numGrans = granIdxToInTrackLen.size();

		int[] vGranuleLengths = new int[numGrans];
		for (int k = 0; k < numGrans; k++) {
			vGranuleLengths[k] = granIdxToInTrackLen.get(k);
		}

		// calculate variable read ranges for individual granules in this aggregation
		Granule aggrGran = new Granule();
		for (int k = 0; k < vGranuleLengths.length; k++) {
			aggrGran.add(new Granule(vGranuleLengths[k]));
		}
		ArrayList<SegmentRange> ranges = aggrGran.getGranulesRanges(start[vInTrackIndex], count[vInTrackIndex], stride[vInTrackIndex]);
		int granuleSpan = ranges.size();
		loGranuleId = ranges.get(0).granIdx;
		hiGranuleId = ranges.get(granuleSpan - 1).granIdx;

		Range[][] rangeSet = new Range[granuleSpan][dimensionCount];

		for (int i = 0; i < granuleSpan; i++) {
			for (int j = 0; j < dimensionCount; j++) {
				if (j != vInTrackIndex) {
					rangeSet[i][j] = new Range(start[j], start[j] + (count[j] - 1) * stride[j], stride[j]);
				} else {
					SegmentRange segRng = ranges.get(i);
					Range rng = segRng.range;
					rangeSet[i][j] = segRng.range;
				}
			}
		}


		int totalLength = 0;
		int rangeListCount = 0;
		ArrayList<Array> arrayList = new ArrayList<Array>();
		for (int j = 0; j < granuleSpan; j++) {
			int granIdx = ranges.get(j).granIdx;
			Variable var = varMapList.get(granIdx).get(array_name);

			if (var instanceof Structure) {
				// what to do here?
			} else {
				ArrayList<Range> rangeList = new ArrayList<Range>();
				for (int dimensionIdx = 0; dimensionIdx < dimensionCount; dimensionIdx++) {
					rangeList.add(dimensionIdx, rangeSet[rangeListCount][dimensionIdx]);
				}
				rangeListCount++;
				Array subarray = var.read(rangeList);
				totalLength += subarray.getSize();
				arrayList.add(subarray);
			}
		}


		// last, concatenate the individual NetCDF arrays pulled out

		Class outType;
		Class arrayType = getArrayType(array_name);
		RangeProcessor rngProcessor = varToRangeProcessor.get(array_name);
		if (rngProcessor == null) {
			outType = getArrayType(array_name);
		} else {
			outType = java.lang.Float.TYPE;
		}
		Object o = java.lang.reflect.Array.newInstance(outType, totalLength);

		int destPos = 0;
		int granIdx = loGranuleId;


		for (Array a : arrayList) {
			if (a != null) {
				Object primArray = a.copyTo1DJavaArray();
				primArray = processArray(arrayType, granIdx, primArray, rngProcessor, obj);
				System.arraycopy(primArray, 0, o, destPos, (int) a.getSize());
				destPos += a.getSize();
			}
			granIdx++;
		}

		return o;
	}

	public HashMap getVarMap() {
		return varMapList.get(0);
	}

	public boolean hasArray(String name) {
		if (varMapList.get(0).get(name) == null) {
			return false;
		} else {
			return true;
		}
	}

	public ArrayList<NetCDFFile> getReaders() {
		return this.ncdfal;
	}

	/* pass individual granule pieces just read from dataset through the RangeProcessor */
	private Object processArray(Class arrayType, int granIdx, Object values, RangeProcessor rngProcessor, Object obj) {

		if (rngProcessor == null) {
			return values;
		} else {
			((AggregationRangeProcessor) rngProcessor).setWhichRangeProcessor(granIdx);

			Object outArray = null;

			if (arrayType == Short.TYPE) {
				outArray = rngProcessor.processRange((short[]) values, (HashMap) obj);
			} else if (arrayType == Byte.TYPE) {
				outArray = rngProcessor.processRange((byte[]) values, (HashMap) obj);
			} else if (arrayType == Integer.TYPE) {
				outArray = rngProcessor.processRange((int[]) values, (HashMap) obj);
			} else if (arrayType == Float.TYPE) {
				outArray = rngProcessor.processRange((float[]) values, (HashMap) obj);
			} else if (arrayType == Double.TYPE) {
				outArray = rngProcessor.processRange((double[]) values, (HashMap) obj);
			}

			return outArray;
		}
	}

	/* Application can supply a RangeProcessor for a variable 'arrayName'.
	 *  This effectively applies rangeProcessor to individual granule segments *prior* to assembling the
	 *  aggregated array. This is necessary, for example, when individual granules may have difference calibration
	 *  parameter values.
	 */
	public void addPreProcessor(String arrayName, RangeProcessor rangeProcessor) {
		varToRangeProcessor.put(arrayName, rangeProcessor);
	}

	public RangeProcessor getPreProcessor(String arrayName) {
		return varToRangeProcessor.get(arrayName);
	}

}