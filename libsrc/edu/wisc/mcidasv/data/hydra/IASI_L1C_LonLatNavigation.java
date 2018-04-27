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

import visad.Gridded2DSet;
import visad.RealTupleType;

public class IASI_L1C_LonLatNavigation extends SwathNavigation  {

  public IASI_L1C_LonLatNavigation(SwathAdapter swathAdapter) throws Exception {
    super(swathAdapter);
  }

  Gridded2DSet createInterpSet() throws Exception {
    float scale = 1E-06f;

    int[] new_geo_start = new int[2]; 
    int[] new_geo_count = new int[2]; 
    int[] new_geo_stride = new int[2]; 

    new_geo_start[geo_xtrack_idx] = 0;
    new_geo_count[geo_xtrack_idx] = 60*2;
    new_geo_stride[geo_xtrack_idx] = 1;
    new_geo_start[geo_track_idx] = 0;
    new_geo_count[geo_track_idx] = geo_count[geo_track_idx]/2;
    new_geo_stride[geo_track_idx] = 1;

    int[] lonValues = reader.getIntArray(lon_array_name, new_geo_start, new_geo_count, new_geo_stride);
    int[] latValues = reader.getIntArray(lat_array_name, new_geo_start, new_geo_count, new_geo_stride);

    float[] lons = new float[lonValues.length];
    float[] lats = new float[latValues.length];

    for (int k=0; k<lons.length; k++) {
      lons[k] = ((float)lonValues[k])*scale;
      lats[k] = ((float)latValues[k])*scale;
    }

    lons = IASI_L1C_Utility.psuedoScanReorder2(lons, 60, geo_count[0]);
    lats = IASI_L1C_Utility.psuedoScanReorder2(lats, 60, geo_count[0]);
                                                                                                                                             
    Gridded2DSet gset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
                   new float[][] {lons, lats},
                        geo_count[idx_order[0]], geo_count[idx_order[1]],
                            null, null, null, false, false);
    return gset;
  }
}
