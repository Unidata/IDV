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

import visad.FlatField;
import visad.SampledSet;
import visad.RealTuple;
import visad.SetType;
import visad.RealType;
import visad.RealTupleType;
import visad.VisADException;
import visad.CoordinateSystem;
import visad.FunctionType;
import visad.Real;
import visad.Set;
import visad.Linear1DSet;
import visad.Linear2DSet;
import visad.Gridded1DSet;
import visad.Gridded2DSet;
import visad.QuickSort;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.ArrayList;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import visad.georef.MapProjection;
import visad.CachingCoordinateSystem;


public class MultiSpectralAggr extends MultiSpectralData {

  Gridded1DSet aggrDomain = null;

  MultiSpectralData[] adapters = null;

  int[] sort_indexes = null;

  float[] aggrValues = null;

  float[] aggrSamples = null;

  int numAdapters;

  int numBands;

  int[] offset;

  public MultiSpectralAggr(MultiSpectralData[] adapters)
          throws Exception {
    this(adapters, null);
  }

  public MultiSpectralAggr(MultiSpectralData[] adapters, String name)
          throws Exception {
    super(adapters[0].swathAdapter, null);
    this.adapters = adapters;
    paramName = adapters[0].getParameter();
    sensorName = adapters[0].getSensorName();
    if (name != null) {
      this.name = name;
    }

    numAdapters = adapters.length;
    int[] numBandsAdapter = new int[numAdapters];
    offset = new int[numAdapters];
    SampledSet[] spectrumDomains = new SampledSet[numAdapters];

    if (adapters[0].spectrumAdapter.hasBandNames()) {
      hasBandNames = true;
      bandNameList = new ArrayList<String>();
      bandNameMap = new HashMap<String, Float>();
      for (int k = 0; k < numAdapters; k++) {
        bandNameList.addAll(adapters[k].spectrumAdapter.getBandNames());
        bandNameMap.putAll(adapters[k].spectrumAdapter.getBandNameMap());
      }
    }

    numBands = 0;
    for (int k = 0; k < numAdapters; k++) {
      SampledSet set = adapters[k].spectrumAdapter.getDomainSet();
      spectrumDomains[k] = set;
      numBandsAdapter[k] = set.getLength();
      offset[k] = numBands;
      numBands += numBandsAdapter[k];
    }

    aggrSamples = new float[numBands];
    aggrValues = new float[numBands];

    for (int k = 0; k < numAdapters; k++) {
      float[][] samples = spectrumDomains[k].getSamples(false);
      System.arraycopy(samples[0], 0, aggrSamples, offset[k], samples[0].length);
    }

    sort_indexes = QuickSort.sort(aggrSamples);
    SpectrumAdapter specAdapt = adapters[0].spectrumAdapter;
    aggrDomain = new Gridded1DSet(specAdapt.getDomainSet().getType(),
            new float[][]{aggrSamples}, aggrSamples.length);
  }

  public FlatField getSpectrum(int[] coords) throws Exception {
    FlatField spectrum = null;
    for (int k = 0; k < numAdapters; k++) {
      spectrum = adapters[k].getSpectrum(coords);
      if (spectrum == null) {
        return null;
      }
      float[][] values = spectrum.getFloats(false);
      System.arraycopy(values[0], 0, aggrValues, offset[k], values[0].length);
    }

    float[] sortVals = new float[numBands];
    for (int t = 0; t < numBands; t++) {
      sortVals[t] = aggrValues[sort_indexes[t]];
    }

    spectrum = new FlatField((FunctionType) spectrum.getType(), aggrDomain);
    spectrum.setSamples(new float[][]{sortVals});

    return spectrum;
  }

  public FlatField getSpectrum(HashMap subset) throws Exception {
    // Analyze the hashmap: only a spectrum at a single FOV is allowed
    double[] x_extent = ((double[]) subset.get(SpectrumAdapter.x_dim_name));
    double[] y_extent = ((double[]) subset.get(SpectrumAdapter.y_dim_name));
    if ((x_extent[0] != x_extent[1]) && (y_extent[0] != y_extent[1]) && (x_extent[2] != 1.0) && (y_extent[2] != 1.0)) {
      throw new Exception("Can only ask for a spectrum of a single FOV. Stride must be 1");
    }
    return getSpectrum(new int[]{(int) x_extent[0], (int) y_extent[0]});
  }

  //public FlatField getSpectrum(MultiDimensionSubset subset) throws Exception, VisADException, RemoteException {
  //  return getSpectrum(subset.getSubset());
  //}

  public FlatField getSpectrum(RealTuple location) throws Exception {
    FlatField spectrum = null;
    for (int k = 0; k < numAdapters; k++) {
      spectrum = adapters[k].getSpectrum(location);
      if (spectrum == null) {
        return null;
      }
      float[][] values = spectrum.getFloats(false);
      System.arraycopy(values[0], 0, aggrValues, offset[k], values[0].length);
    }

    float[] sortVals = new float[numBands];
    for (int t = 0; t < numBands; t++) {
      sortVals[t] = aggrValues[sort_indexes[t]];
    }

    spectrum = new FlatField((FunctionType) spectrum.getType(), aggrDomain);
    spectrum.setSamples(new float[][]{sortVals});

    return spectrum;
  }

  public FlatField getImage(HashMap subset) throws Exception {
    int channelIndex = (int) ((double[]) subset.get(SpectrumAdapter.channelIndex_name))[0];
    return getImage(channelIndex, subset);
  }

  public FlatField getImage(float channel, HashMap subset) throws Exception {
    int channelIndex = aggrDomain.valueToIndex(new float[][]{{channel}})[0];
    return getImage(channelIndex, subset);
  }

  public FlatField getImage(float channel, HashMap subset, String param) throws Exception {
    int channelIndex = aggrDomain.valueToIndex(new float[][]{{channel}})[0];
    return getImage(channelIndex, subset, param);
  }

  public FlatField getImage(int channelIndex, HashMap subset) throws Exception {
    int idx = sort_indexes[channelIndex];
    int swathAdapterIndex = numAdapters - 1;
    for (int k = 0; k < numAdapters - 1; k++) {
      if (idx >= offset[k] && idx < offset[k + 1]) swathAdapterIndex = k;
    }

    float channel = aggrSamples[channelIndex];
    FlatField image = adapters[swathAdapterIndex].getImage(channel, subset);
    Linear2DSet domSet = (Linear2DSet) image.getDomainSet();
    cs = ((RealTupleType) ((FunctionType) image.getType()).getDomain()).getCoordinateSystem();

    float[] reflCorr = adapters[swathAdapterIndex].getReflectanceCorr(domSet);
    for (int k = 0; k < numAdapters; k++) {
      adapters[k].setCoordinateSystem(cs);
      if (k != swathAdapterIndex) {
        adapters[k].setReflectanceCorr(domSet, reflCorr);
      }
    }

    return image;
  }

  public FlatField getImage(int channelIndex, HashMap subset, String param) throws Exception {
    int idx = sort_indexes[channelIndex];
    int swathAdapterIndex = numAdapters - 1;
    for (int k = 0; k < numAdapters - 1; k++) {
      if (idx >= offset[k] && idx < offset[k + 1]) swathAdapterIndex = k;
    }

    float channel = aggrSamples[channelIndex];
    FlatField image = adapters[swathAdapterIndex].getImage(channel, subset, param);
    Linear2DSet domSet = (Linear2DSet) image.getDomainSet();
    cs = ((RealTupleType) ((FunctionType) image.getType()).getDomain()).getCoordinateSystem();

    float[] reflCorr = adapters[swathAdapterIndex].getReflectanceCorr(domSet);
    for (int k = 0; k < numAdapters; k++) {
      adapters[k].setCoordinateSystem(cs);
      if (k != swathAdapterIndex) {
        adapters[k].setReflectanceCorr(domSet, reflCorr);
      }
    }

    return image;
  }

  //public FlatField makeConvolvedRadiances(edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset subset, HyperToBroadBand.Kernel kernel, float wavenumL, float cntrWavenum, float wavenumR) throws Exception {
  //  int loIdx = getChannelIndexFromWavenumber(wavenumL);
  //  int hiIdx = getChannelIndexFromWavenumber(wavenumR);

  //  float[] wghts = HyperToBroadBand.generate_convolution_weights(kernel, this, wavenumL, cntrWavenum, wavenumR);

  //  return makeConvolvedRadiances(new int[]{loIdx, hiIdx}, subset.getSubset(), wghts);
  //}

  public FlatField makeConvolvedRadiances(int[] channelIndexRange, HashMap subset, float[] wghts)
          throws Exception {
    int idx_lo = channelIndexRange[0];
    int idx_hi = channelIndexRange[1];
    int nChans = (idx_hi - idx_lo) + 1;

    ArrayList swathIdxRanges = new ArrayList();

    int idx = sort_indexes[idx_lo];
    int swthIdxLo = numAdapters - 1;
    for (int k = 0; k < numAdapters - 1; k++) {
      if (idx >= offset[k] && idx < offset[k + 1]) swthIdxLo = k;
    }
    idx = sort_indexes[idx_hi];
    int swthIdxHi = numAdapters - 1;
    for (int k = 0; k < numAdapters - 1; k++) {
      if (idx >= offset[k] && idx < offset[k + 1]) swthIdxHi = k;
    }
    if (swthIdxLo != swthIdxHi) {
      throw new Exception("Can't convolve across adapters if more than one exists");
    }

    idx_lo = sort_indexes[idx_lo];
    idx_hi = sort_indexes[idx_hi];

    subset.put(SpectrumAdapter.channelIndex_name, new double[]{(double) (idx_lo - offset[swthIdxLo]), (double) (idx_hi - offset[swthIdxLo]), 1.0});
    return adapters[swthIdxLo].getSwathAdapter().makeConvolvedRadiances(subset, wghts);
  }

  public int getChannelIndexFromWavenumber(float channel) throws VisADException, RemoteException {
    int idx = (aggrDomain.valueToIndex(new float[][]{{channel}}))[0];
    return idx;
  }

  public float getWavenumberFromChannelIndex(int index) throws Exception {
    return (aggrDomain.indexToValue(new int[]{index}))[0][0];
  }

  public void setCoordinateSystem(CoordinateSystem cs) {
    this.cs = cs;
    for (int k = 0; k < numAdapters; k++) {
      adapters[k].setCoordinateSystem(cs);
    }
  }

  public void setSwathDomainSet(Linear2DSet dset) {
    for (int k = 0; k < numAdapters; k++) {
      adapters[k].setSwathDomainSet(dset);
    }
  }

  public int getNumChannels() {
    return numBands;
  }

  public Gridded1DSet getSpectralDomain() {
    return aggrDomain;
  }

  public boolean hasBandName(String name) {
    for (int k = 0; k < numAdapters; k++) {
      if (adapters[k].hasBandName(name)) {
        return true;
      }
    }
    return false;
  }

  public HashMap getDefaultSubset() {
    HashMap subset = adapters[0].getDefaultSubset();
    double chanIdx = 0;
    try {
      chanIdx = getChannelIndexFromWavenumber(init_wavenumber);
    } catch (Exception e) {
      System.out.println("couldn't get chanIdx, using zero");
    }
    subset.put(SpectrumAdapter.channelIndex_name, new double[]{chanIdx, chanIdx, 1});
    return subset;
  }

  public MultiDimensionSubset getDefaultSubsetSpectrum() {
    HashMap<String, double[]> subset = SpectrumAdapter.getEmptySubset();

    double[] coords = (double[]) subset.get(SpectrumAdapter.y_dim_name);
    coords[0] = 1.0;
    coords[1] = 1.0;
    coords[2] = 1.0;
    subset.put(SpectrumAdapter.y_dim_name, coords);

    coords = (double[]) subset.get(SpectrumAdapter.x_dim_name);
    coords[0] = 1.0;
    coords[1] = 1.0;
    coords[2] = 1.0;
    subset.put(SpectrumAdapter.x_dim_name, coords);

    coords = (double[]) subset.get(SpectrumAdapter.channelIndex_name);

    coords[0] = 0.0;
    coords[1] = (double) (numBands - 1);
    coords[2] = 1.0;
    subset.put(SpectrumAdapter.channelIndex_name, coords);

    return new MultiDimensionSubset(subset);
  }

}