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

import visad.CoordinateSystem;
import visad.GridCoordinateSystem;
import visad.VisADException;
import visad.RealTupleType;
import visad.Linear2DSet;
import visad.Gridded2DSet;
import visad.Linear1DSet;
import visad.Unit;
import visad.Set;
import visad.georef.MapProjection;
import java.awt.geom.Rectangle2D;


public class LongitudeLatitudeCoordinateSystem extends CoordinateSystem {
//public class LongitudeLatitudeCoordinateSystem extends MapProjection {

   Linear2DSet domainSet;
   Linear2DSet subSet;
   Gridded2DSet gset;

   //- assumes incoming GriddedSet is (longitude,latitude) with range (-180,+180)
   boolean neg180pos180 = true;  //false: longitude range (0,+360)

   public LongitudeLatitudeCoordinateSystem(Linear2DSet domainSet, Gridded2DSet gset) throws VisADException {
     this(domainSet, gset, false);
   }

   public LongitudeLatitudeCoordinateSystem(Linear2DSet domainSet, Gridded2DSet gset, boolean lonFlag) throws VisADException {
     super(RealTupleType.SpatialEarth2DTuple, null);
     this.gset = gset;
     this.domainSet = domainSet;
     this.neg180pos180 = lonFlag;
     int[] lengths = domainSet.getLengths();
     int[] gset_lengths = gset.getLengths();
     subSet = new Linear2DSet(0.0, gset_lengths[0]-1, lengths[0],
                              0.0, gset_lengths[1]-1, lengths[1]);
   }

   public float[][] toReference(float[][] values) throws VisADException {
     float[][] coords = domainSet.valueToGrid(values);
     coords = subSet.gridToValue(coords);
     coords = gset.gridToValue(coords);
     return coords;
   }

   public float[][] fromReference(float[][] values) throws VisADException {
     if (!neg180pos180) { // force to longitude range (0,360)
       for (int t=0; t<values[0].length; t++) {
         if (values[0][t] > 180f) {
           values[0][t] -= 360f;
         }
       }
     }
     float[][] grid_vals = gset.valueToGrid(values);
     float[][] coords = subSet.valueToGrid(grid_vals);
     coords = domainSet.gridToValue(coords);
     return coords;
   }

   public double[][] toReference(double[][] values) throws VisADException {
     float[][] coords = domainSet.valueToGrid(Set.doubleToFloat(values));
     coords = subSet.gridToValue(coords);
     coords = gset.gridToValue(coords);
     return Set.floatToDouble(coords);
   }

   public double[][] fromReference(double[][] values) throws VisADException {
     if (!neg180pos180) { // force to longitude range (0,360)
       for (int t=0; t<values[0].length; t++) {
         if (values[0][t] > 180.0) {
           values[0][t] -= 360.0;
         }
       }
     }
     float[][] grid_vals = gset.valueToGrid(Set.doubleToFloat(values));
     float[][] coords = subSet.valueToGrid(grid_vals);
     coords = domainSet.gridToValue(coords);
     return Set.floatToDouble(coords);
   }

   public Rectangle2D getDefaultMapArea() {
     float[] lo = domainSet.getLow();
     float[] hi = domainSet.getHi();
     return new Rectangle2D.Float(lo[0], lo[1], hi[0] - lo[0], hi[1] - lo[1]);
   }

   public boolean equals(Object cs) {
     return (cs instanceof LongitudeLatitudeCoordinateSystem);
   }
}
