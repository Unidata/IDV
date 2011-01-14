/*
 * $Id: DemDataSource.java,v 1.16 2007/04/16 20:34:52 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.data.gis;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.visad.data.MapSet;

import visad.*;
import visad.data.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


/**
 *
 * @author IDV development team
 * @version $Revision: 1.16 $
 */
public class MapMaker {

    List<MapSet> mapSets = new ArrayList<MapSet>();

    public MapMaker() {
    }

    public void addMap(double[][]latLons) throws VisADException {
        addMap(Misc.toFloat(latLons));
    }

    public void addMap(double[]latLons) throws VisADException {
        addMap(Misc.toFloat(latLons));
    }

    public void addMap(float[]latLons) throws VisADException {
        float[][] tmp = new float[2][latLons.length/2];
        int cnt = 0;
        for(int i=0;i<latLons.length;i+=2) {
            tmp[0][cnt] = latLons[i];
            tmp[1][cnt] = latLons[i+1];
            cnt++;
        }
        addMap(tmp);
    }


    public void addMap(float[][]latLons) throws VisADException {
        float[][] lonLats = new float[][]{latLons[1],latLons[0]};
        RealTupleType coordMathType =    new RealTupleType(RealType.Longitude,RealType.Latitude);
        mapSets.add(new MapSet(coordMathType, lonLats, lonLats[0].length,
                               (CoordinateSystem) null, (Unit[]) null,
                               (ErrorEstimate[]) null, false));  

    }

    public UnionSet getMaps() throws VisADException {
        Gridded2DSet[] latLonLines = new Gridded2DSet[mapSets.size()];
        int            cnt         = 0;
        for (MapSet mapSet: mapSets) {
            latLonLines[cnt++] = mapSet;
        }
        RealTupleType coordMathType = new RealTupleType(RealType.Longitude,
                                          RealType.Latitude);
        UnionSet mapLines = new UnionSet(coordMathType, latLonLines,
                                         (CoordinateSystem) null,
                                         (Unit[]) null,
                                         (ErrorEstimate[]) null, false);
        return mapLines;
    }



}

