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

import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import visad.CoordinateSystem;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded2DSet;
import visad.Linear1DSet;
import visad.Linear2DSet;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Set;
import visad.SetType;
import visad.VisADException;

public class MultiSpectralData extends MultiDimensionAdapter {

  SwathAdapter swathAdapter = null;
  SpectrumAdapter spectrumAdapter = null;
  CoordinateSystem cs = null;

  Map<String, double[]> spectrumSelect = null;
  Map swathSelect = null;

  String sensorName = null;
  String platformName = null;
  String paramName = null;
  String inputParamName = null;
  String name = null;

  public float init_wavenumber = 919.50f;
  public String init_bandName = null;

  float[] dataRange = new float[] {180f, 320f};

  boolean hasBandNames = false;
  List<String> bandNameList = null;
  Map<String, Float> bandNameMap = null;

  
  public MultiSpectralData(SwathAdapter swathAdapter, SpectrumAdapter spectrumAdapter,
                           String inputParamName, String paramName, String sensorName, String platformName) {
    this.swathAdapter = swathAdapter;
    this.spectrumAdapter = spectrumAdapter;
    this.paramName = paramName;
    this.inputParamName = inputParamName;
    this.name = swathAdapter.getArrayName();

    if (spectrumAdapter != null) {
      this.spectrumSelect = spectrumAdapter.getDefaultSubset();
      if (spectrumAdapter.hasBandNames()) {
        hasBandNames = true;
        bandNameList = spectrumAdapter.getBandNames();
        bandNameMap = spectrumAdapter.getBandNameMap();
      }
      try {
        setInitialWavenumber(getWavenumberFromChannelIndex(0));
      } 
      catch (Exception e) {
        e.printStackTrace();
        System.out.println("could not initialize initial wavenumber");
      }
    }

    setSpectrumAdapterProcessor();

    this.sensorName = sensorName;
    this.platformName = platformName;
  }

  public MultiSpectralData(SwathAdapter swathAdapter, SpectrumAdapter spectrumAdapter,
                           String sensorName, String platformName) {
    this(swathAdapter, spectrumAdapter, "Radiance", "BrightnessTemp", sensorName, platformName);
  }

  public MultiSpectralData(SwathAdapter swathAdapter, SpectrumAdapter spectrumAdapter) {
    this(swathAdapter, spectrumAdapter, null, null);
  }

  public MultiSpectralData() {
    this(null, null, null, null);
  }

  void setSpectrumAdapterProcessor() {
     if (swathAdapter != null) {
        if (spectrumAdapter != null) {
          spectrumAdapter.setRangeProcessor(swathAdapter.getRangeProcessor());
        }
     }
  }

  public FlatField getSpectrum(int[] coords) 
      throws Exception, VisADException, RemoteException {
    if (coords == null) return null;
    if (spectrumAdapter == null) return null;
    spectrumSelect.put(SpectrumAdapter.x_dim_name, new double[] {(double)coords[0], (double)coords[0], 1.0});
    spectrumSelect.put(SpectrumAdapter.y_dim_name, new double[] {(double)coords[1], (double)coords[1], 1.0});

    FlatField spectrum = spectrumAdapter.getData(spectrumSelect);
    return convertSpectrum(spectrum, paramName);
  }

  public FlatField getSpectrum(RealTuple location) 
      throws Exception, VisADException, RemoteException {
    if (spectrumAdapter == null) return null;
    int[] coords = getSwathCoordinates(location, cs);
    if (coords == null) return null;
    spectrumSelect.put(SpectrumAdapter.x_dim_name, new double[] {(double)coords[0], (double)coords[0], 1.0});
    spectrumSelect.put(SpectrumAdapter.y_dim_name, new double[] {(double)coords[1], (double)coords[1], 1.0});

    FlatField spectrum = spectrumAdapter.getData(spectrumSelect);
    return convertSpectrum(spectrum, paramName);
  }

  public FlatField getImage(Map<String, double[]> subset)
    throws Exception, VisADException, RemoteException {
    FlatField image = swathAdapter.getData(subset);
    cs = ((RealTupleType) ((FunctionType)image.getType()).getDomain()).getCoordinateSystem();

    int channelIndex = (int) ((double[])subset.get(SpectrumAdapter.channelIndex_name))[0];
    float channel = spectrumAdapter.getWavenumberFromChannelIndex(channelIndex);

    return convertImage(image, channel, paramName);
  }

  public FlatField getImage(float channel, Map<String, double[]> subset)
      throws Exception, VisADException, RemoteException {
    if (spectrumAdapter == null) return getImage(subset);
    int channelIndex = spectrumAdapter.getChannelIndexFromWavenumber(channel);
    subset.put(SpectrumAdapter.channelIndex_name, new double[] {(double)channelIndex, (double)channelIndex, 1.0});
    FlatField image = swathAdapter.getData(subset);
    cs = ((RealTupleType) ((FunctionType)image.getType()).getDomain()).getCoordinateSystem();

    return convertImage(image, channel, paramName);
  }

  public FlatField getData(Map<String, double[]> subset) throws Exception {
    return getImage(subset);
  }

  public Set makeDomain(Map<String, double[]> subset) throws Exception {
    throw new Exception("makeDomain unimplented");
  } 


  FlatField convertImage(FlatField image, float channel, String param) 
            throws Exception {
    FlatField new_image = null;
    FunctionType f_type = (FunctionType)image.getType();
    if (param.equals("BrightnessTemp")) { //- convert radiance to BrightnessTemp
      FunctionType new_type = new FunctionType(f_type.getDomain(), RealType.getRealType("BrightnessTemp"));
      new_image = new FlatField(new_type, image.getDomainSet());
      float[][] values = image.getFloats(false);
      float[] bt_values = values[0];
      if (Objects.equals(inputParamName, "Radiance")) {
        bt_values = radianceToBrightnessTemp(values[0], channel, platformName, sensorName);
      }
      new_image.setSamples(new float[][] {bt_values}, false);
    }
    else if (param.equals("Reflectance")) {
      FunctionType new_type = new FunctionType(f_type.getDomain(), RealType.getRealType("Reflectance"));
      new_image = new FlatField(new_type, image.getDomainSet());
      new_image.setSamples(image.getFloats(false), false);
    }
    else {
      new_image = image;
    }
    return new_image;
  }


  FlatField convertSpectrum(FlatField spectrum, String param) throws Exception {
    FlatField new_spectrum = null;
    FunctionType f_type = (FunctionType) spectrum.getType();

    if (param.equals("BrightnessTemp")) {
      FunctionType new_type = new FunctionType(f_type.getDomain(), RealType.getRealType("BrightnessTemp"));
      float[][] channels = ((SampledSet)spectrum.getDomainSet()).getSamples(false);
      float[][] values = spectrum.getFloats(false);
      float[] bt_values = values[0];
      if (Objects.equals(inputParamName, "Radiance")) {
        bt_values = radianceToBrightnessTempSpectrum(values[0], channels[0], platformName, sensorName);
      }
      new_spectrum = new FlatField(new_type, spectrum.getDomainSet());
      new_spectrum.setSamples(new float[][] {bt_values}, true);
    }
    else if (param.equals("Reflectance")) {
      FunctionType new_type = new FunctionType(f_type.getDomain(), RealType.getRealType("Reflectance"));
      new_spectrum = new FlatField(new_type, spectrum.getDomainSet());
      new_spectrum.setSamples(spectrum.getFloats(false), false);
    }
    else {
      new_spectrum = spectrum;
    }
    return new_spectrum;
  }

  protected void setDataRange(float[] range) {
    dataRange = range;
  }

  public float[] getDataRange() {
    return dataRange;
  }

  public String getParameter() {
    return paramName;
  }

  /**
   * @return the paramName
   */
  public String getParamName() {
	  return paramName;
  }

  /**
   * @param paramName the paramName to set
   */
  public void setParamName(String paramName) {
	  this.paramName = paramName;
  }

  public String getName() {
	  return name;
  }

  public CoordinateSystem getCoordinateSystem() {
    return cs;
  }

  public void setCoordinateSystem(CoordinateSystem cs) {
    this.cs = cs;
  }

  public boolean hasBandNames() {
    return hasBandNames;
  }

  public List<String> getBandNames() {
    return bandNameList;
  }

  public Map<String, Float> getBandNameMap() {
    return bandNameMap;
  }

  public String getBandNameFromWaveNumber(float channel) {
    String bandName = null;
    Iterator iter = bandNameMap.keySet().iterator();
    while (iter.hasNext()) {
       String key = (String) iter.next();
       float mapVal = ((Float)bandNameMap.get(key)).floatValue();
       if (channel == mapVal) {
         bandName = key;
         break;
       }
    }
    return bandName;
  }

  public void setInitialWavenumber(float val) {
    init_wavenumber = val;
    if (hasBandNames) {
      init_bandName = getBandNameFromWaveNumber(init_wavenumber);
    }
  }

  public int[] getSwathCoordinates(RealTuple location, CoordinateSystem cs) 
      throws VisADException, RemoteException {
    if (location == null) return null;
    if (cs == null) return null;
    Real[] comps = location.getRealComponents();
    //- trusted: latitude:0, longitude:1
    float lon = (float) comps[1].getValue();
    float lat = (float) comps[0].getValue();
    if (lon < -180) lon += 360f;
    if (lon > 180) lon -= 360f;
    float[][] xy = cs.fromReference(new float[][] {{lon}, {lat}});
    if ((Float.isNaN(xy[0][0])) || Float.isNaN(xy[1][0])) return null;
    Set domain = swathAdapter.getSwathDomain();
    int[] idx = domain.valueToIndex(xy);
    xy = domain.indexToValue(idx);
    int[] coords = new int[2];
    coords[0] = (int) xy[0][0];
    coords[1] = (int) xy[1][0];
    if ((coords[0] < 0)||(coords[1] < 0)) return null;
    return coords;
  }

  public RealTuple getEarthCoordinates(float[] xy)
      throws VisADException, RemoteException {
    float[][] tup = cs.toReference(new float[][] {{xy[0]}, {xy[1]}});
    return new RealTuple(RealTupleType.SpatialEarth2DTuple, new double[] {(double)tup[0][0], (double)tup[1][0]});
  }

  public int getChannelIndexFromWavenumber(float channel) throws Exception {
    return spectrumAdapter.getChannelIndexFromWavenumber(channel);
  }

  public float getWavenumberFromChannelIndex(int index) throws Exception {
    return spectrumAdapter.getWavenumberFromChannelIndex(index);
  }

  public Rectangle2D getLonLatBoundingBox(CoordinateSystem cs) {
    return null;
  }

  public Rectangle2D getLonLatBoundingBox(Map<String, double[]> subset)
      throws Exception {
    Set domainSet = swathAdapter.makeDomain(subset);
    return getLonLatBoundingBox(domainSet);
  }

  public static Rectangle2D getLonLatBoundingBox(FlatField field) {
    Set domainSet = field.getDomainSet();
    return getLonLatBoundingBox(domainSet);
  }

  public static float[][] getLonLatBoundingCorners(Set domainSet) {
    CoordinateSystem cs =
      ((SetType)domainSet.getType()).getDomain().getCoordinateSystem();

    float start0, stop0, start1, stop1;
    int len0, len1;
    float minLon = Float.MAX_VALUE;
    float minLat = Float.MAX_VALUE;
    float maxLon = -Float.MAX_VALUE;
    float maxLat = -Float.MAX_VALUE;

    float[][] corners = null;

    if (domainSet instanceof Linear2DSet) {
      Linear1DSet lset = ((Linear2DSet)domainSet).getLinear1DComponent(0);
      start0 = (float) lset.getFirst();
      stop0 = (float) lset.getLast();
      len0 = lset.getLengthX();
      lset = ((Linear2DSet)domainSet).getLinear1DComponent(1);
      start1 = (float) lset.getFirst();
      stop1 = (float) lset.getLast();
      len1 = lset.getLengthX();

      float x, y, del_x, del_y;
      float lonA = Float.NaN;
      float lonB = Float.NaN;
      float lonC = Float.NaN;
      float lonD = Float.NaN;
      float latA = Float.NaN;
      float latB = Float.NaN;
      float latC = Float.NaN;
      float latD = Float.NaN;

      int nXpts = len0/1;
      int nYpts = len1/1;

      del_x = (stop0 - start0)/nXpts;
      del_y = (stop1 - start1)/nYpts;
      x = start0;
      y = start1;
      try {
        for (int j=0; j<nYpts; j++) {
          y = start1+j*del_y;
          for (int i=0; i<nXpts; i++) {
            x = start0 + i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonA = lon;
              latA = lat;
              break;
            }
          }
          for (int i=0; i<nXpts; i++) {
            x = stop0 - i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonB = lon;
              latB = lat;
              break;
            }
          }
          if (!Float.isNaN(lonA) && !Float.isNaN(lonB)) {
            break;
          }
        }

        for (int j=0; j<nYpts; j++) {
          y = stop1-j*del_y;
          for (int i=0; i<nXpts; i++) {
            x = start0 + i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonC = lon;
              latC = lat;
              break;
            }
          }
          for (int i=0; i<nXpts; i++) {
            x = stop0 - i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonD = lon;
              latD = lat;
              break;
            }
          }
          if (!Float.isNaN(lonC) && !Float.isNaN(lonD)) {
            break;
          }
         }
         // TJJ - should these be validated? See history, lost some dead code here
         corners = new float[][] {{lonA,lonB,lonC,lonD},{latA,latB,latC,latD}};
       } catch (Exception e) {
       }
    }
    else if (domainSet instanceof Gridded2DSet) {
      int[] lens = ((Gridded2DSet)domainSet).getLengths();
      start0 = 0f;
      start1 = 0f;
      stop0 = (float) lens[0];
      stop1 = (float) lens[1];

      float x, y, del_x, del_y;
      del_x = (stop0 - start0)/10;
      del_y = (stop1 - start1)/10;
      x = start0;
      y = start1;
      try {
        for (int j=0; j<11; j++) {
          y = start1+j*del_y;
          for (int i=0; i<11; i++) {
            x = start0+i*del_x;
            float[][] lonlat = ((Gridded2DSet)domainSet).gridToValue(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if ((lon > 180 || lon < -180) || (lat > 90 || lat < -90)) continue;
            if (lon < minLon) minLon = lon;
            if (lat < minLat) minLat = lat;
            if (lon > maxLon) maxLon = lon;
            if (lat > maxLat) maxLat = lat;
          }
        }
      } catch (Exception e) {
      }
    }

    return corners;
  }

  public static Rectangle2D getLonLatBoundingBox(Set domainSet) {
    CoordinateSystem cs = 
      ((SetType)domainSet.getType()).getDomain().getCoordinateSystem();

    float start0, stop0, start1, stop1;
    int len0, len1;
    float minLon = Float.MAX_VALUE;
    float minLat = Float.MAX_VALUE;
    float maxLon = -Float.MAX_VALUE;
    float maxLat = -Float.MAX_VALUE;


    if (domainSet instanceof Linear2DSet) {
      Linear1DSet lset = ((Linear2DSet)domainSet).getLinear1DComponent(0);
      start0 = (float) lset.getFirst();
      stop0 = (float) lset.getLast();
      len0 = lset.getLengthX();
      lset = ((Linear2DSet)domainSet).getLinear1DComponent(1);
      start1 = (float) lset.getFirst();
      stop1 = (float) lset.getLast();
      len1 = lset.getLengthX();

      float x, y, del_x, del_y;
      float lonA = Float.NaN;
      float lonB = Float.NaN;
      float lonC = Float.NaN;
      float lonD = Float.NaN;
      float latA = Float.NaN;
      float latB = Float.NaN;
      float latC = Float.NaN;
      float latD = Float.NaN;

      int nXpts = len0/8;
      int nYpts = len1/8;

      del_x = (stop0 - start0)/nXpts;
      del_y = (stop1 - start1)/nYpts;

      x = start0;
      y = start1;
      try {
        for (int j=0; j<nYpts; j++) {
          y = start1+j*del_y;
          for (int i=0; i<nXpts; i++) {
            x = start0 + i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonA = lon;
              latA = lat;
              break;
            }
          }
          for (int i=0; i<nXpts; i++) {
            x = stop0 - i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonB = lon;
              latB = lat;
              break;
            }
          }
          if (!Float.isNaN(lonA) && !Float.isNaN(lonB)) {
            break;
          }
        }

        for (int j=0; j<nYpts; j++) {
          y = stop1-j*del_y;
          for (int i=0; i<nXpts; i++) {
            x = start0 + i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonC = lon;
              latC = lat;
              break;
            }
          }
          for (int i=0; i<nXpts; i++) {
            x = stop0 - i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonD = lon;
              latD = lat;
              break;
            }
          }
          if (!Float.isNaN(lonC) && !Float.isNaN(lonD)) {
            break;
          }
         }
         float[][] corners = {{lonA,lonB,lonC,lonD},{latA,latB,latC,latD}};
         for (int k=0; k<corners[0].length; k++) {
            float lon = corners[0][k];
            float lat = corners[1][k];
            if (lon < minLon) minLon = lon;
            if (lat < minLat) minLat = lat;
            if (lon > maxLon) maxLon = lon;
            if (lat > maxLat) maxLat = lat;
         }
       } catch (Exception e) {
       }
    }
    else if (domainSet instanceof Gridded2DSet) {
      int[] lens = ((Gridded2DSet)domainSet).getLengths();
      start0 = 0f;
      start1 = 0f;
      stop0 = (float) lens[0];
      stop1 = (float) lens[1];

      float x, y, del_x, del_y;
      del_x = (stop0 - start0)/10;
      del_y = (stop1 - start1)/10;
      x = start0;
      y = start1;
      try {
        for (int j=0; j<11; j++) {
          y = start1+j*del_y;
          for (int i=0; i<11; i++) {
            x = start0+i*del_x;
            float[][] lonlat = ((Gridded2DSet)domainSet).gridToValue(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if ((lon > 180 || lon < -180) || (lat > 90 || lat < -90)) continue;
            if (lon < minLon) minLon = lon;
            if (lat < minLat) minLat = lat;
            if (lon > maxLon) maxLon = lon;
            if (lat > maxLat) maxLat = lat;
          }
        }
      } catch (Exception e) {
      }
    }
    

    float del_lon = maxLon - minLon;
    float del_lat = maxLat - minLat;

    return new Rectangle2D.Float(minLon, minLat, del_lon, del_lat);
  }

  public float[] radianceToBrightnessTemp(float[] values, float channelValue) {
    float c1=1.191066E-5f;           //- mW/m2/ster/cm^-4
    float c2=1.438833f;              //- K*cm
    float nu = channelValue;         //- nu: wavenumber
    float B, K, BT;

    int n_values = values.length;
    float[] new_values = new float[n_values];
    for (int i=0; i<n_values;i++) {
      B = values[i];
      K = (c1*nu*nu*nu)/B;
      if (K == 0.0) {
        BT = B;
      } 
      else {
        BT = c2*nu/((float) (Math.log((double)((c1*nu*nu*nu)/B)+1.0f)) );
      }
      if (BT < 0.01) BT = Float.NaN;
      new_values[i] = BT;
    }
    return new_values;
  }

  public float[] radianceToBrightnessTemp(float[] values, float channelValue, String platformName, String sensorName) 
     throws Exception {
    float[] new_values = null;

    if (sensorName == null) {
      new_values = radianceToBrightnessTemp(values, channelValue);
    }
    else if (Objects.equals(sensorName, "MODIS")) {
      int channelIndex = spectrumAdapter.getChannelIndexFromWavenumber(channelValue);
      int band_number = MODIS_L1B_Utility.emissive_indexToBandNumber(channelIndex);
      new_values = MODIS_L1B_Utility.modis_radiance_to_brightnessTemp(platformName, band_number, values);
    }
    return new_values;
  }

  public float[] radianceToBrightnessTempSpectrum(float[] values, float[] channelValues) {
    //- Converts radiances [mW/ster/m2/cm^-1] to BT [K]
    //-  Input: nu  array of wavenmbers [cm^-1]
    //-          B   radiances [mW/ster/m2/cm^-1]
    //-  Output: bt brightness temperature in [K]
    //-   Paolo Antonelli
    //-   Wed Feb 25 16:43:05 CST 1998

    float c1=1.191066E-5f;           //- mW/m2/ster/cm^-4
    float c2=1.438833f;              //- K*cm

    float nu;                        //- wavenumber
    float B, BT;

    int n_values = values.length;
    float[] new_values = new float[n_values];
    for (int i=0; i<n_values; i++) {
      nu = channelValues[i];
      B = values[i];
      BT = c2*nu/((float) (Math.log(((c1*nu*nu*nu)/B)+1.0f)) );
      new_values[i] = BT;
    }
    return new_values;
  }


  public float[] radianceToBrightnessTempSpectrum(float[] values, float[] channelValues,
                                 String platformName, String sensorName) 
     throws Exception
  {
    float[] new_values = null;

    if (sensorName == null) {
      new_values =  radianceToBrightnessTempSpectrum(values, channelValues);
    }
    else if (Objects.equals(sensorName, "MODIS")) {
      new_values = new float[values.length];
      for (int k=0; k<new_values.length; k++) {
        int channelIndex = spectrumAdapter.getChannelIndexFromWavenumber(channelValues[k]);
        int band_number = MODIS_L1B_Utility.emissive_indexToBandNumber(channelIndex);
        float[] tmp = new float[1];
        tmp[0] = values[k];
        new_values[k] = (MODIS_L1B_Utility.modis_radiance_to_brightnessTemp(platformName, band_number, tmp))[0];
      }
    }

    return new_values;
  }

  public Map<String, double[]> getDefaultSubset() {
    Map<String, double[]> subset = swathAdapter.getDefaultSubset();
    double chanIdx=0;

    try {
       chanIdx = spectrumAdapter.getChannelIndexFromWavenumber(init_wavenumber);
    }
    catch (Exception e) {
      System.out.println("couldn't get chanIdx, using zero");
    }
      
    subset.put(SpectrumAdapter.channelIndex_name, new double[] {chanIdx, chanIdx, 1});
    return subset;
  }
 

  public SpectrumAdapter getSpectrumAdapter() {
    return spectrumAdapter;
  }
}
