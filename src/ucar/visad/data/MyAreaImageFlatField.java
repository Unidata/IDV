/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.visad.data;


import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.AreaFileFactory;


import ucar.ma2.Array;
import ucar.ma2.Index;

//import ucar.nc2.dataset.grid.*;
import ucar.nc2.dt.grid.*;

import ucar.unidata.data.DataUtil;
import ucar.unidata.data.imagery.AddeImageDescriptor;


import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.util.IOUtil;



import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;


import visad.*;

import visad.data.*;
import visad.data.CachedFlatField;

import visad.data.mcidas.AREACoordinateSystem;


import visad.data.mcidas.AreaAdapter;


import visad.meteorology.SingleBandedImage;

import visad.util.DataUtility;


import java.io.*;

import java.rmi.RemoteException;



/**
 * This is a FloatField that caches to disk its float array.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.7 $ $Date: 2007/08/08 17:14:56 $
 */
public class MyAreaImageFlatField extends AreaImageFlatField {






    /**
     * ctor
     *
     *
     * @param aid _more_
     * @param floats The values
     * @param type Function type
     * @param domainSet Domain
     * @param rangeCoordSys  range CoordSystem
     * @param rangeSets range sets
     * @param units units
     * @param readLabel _more_
     *
     * @throws VisADException On badness
     */
    public MyAreaImageFlatField(AddeImageDescriptor aid, FunctionType type,
                                Set domainSet,
                                CoordinateSystem rangeCoordSys,
                                Set[] rangeSets, Unit[] units,
                                float[][] floats, String readLabel)
            throws VisADException {
        super(aid, type, domainSet, rangeCoordSys, rangeSets, units, floats,
              readLabel);
    }





}
