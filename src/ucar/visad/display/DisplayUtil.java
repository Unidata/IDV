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

package ucar.visad.display;


import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;

import ucar.unidata.util.DatedObject;
import ucar.unidata.util.FileManager;

import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;


import ucar.visad.data.*;

import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.Length;

import visad.*;

import visad.browser.Convert;

import visad.data.netcdf.Plain;

import visad.data.units.NoSuchUnitException;

import visad.georef.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;

import visad.java2d.*;

import visad.java3d.*;

import visad.jmet.MetUnits;

import visad.util.*;

import visad.util.DataUtility;

import java.awt.GraphicsConfigTemplate;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;


import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;

import java.io.IOException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;



import java.rmi.RemoteException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.media.j3d.*;

import javax.vecmath.*;



/**
 * Provides support for utility functions.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.81 $ $Date: 2007/08/19 15:55:31 $
 */
public final class DisplayUtil {

    /**
     * Get graphics configuration for the screen
     *
     * @param d  the GraphicsDevice
     * @param is3D  true for a Java 3D display
     * @param useStereo true if a stereo display (is3D must also be true)
     *
     * @return the perferred config
     */
    public static GraphicsConfiguration getPreferredConfig(GraphicsDevice d,
            boolean is3D, boolean useStereo) {
        try {
            if (d == null) {
                GraphicsEnvironment e =
                    GraphicsEnvironment.getLocalGraphicsEnvironment();
                d = e.getDefaultScreenDevice();
            }
            GraphicsConfigTemplate template = null;
            if (is3D) {
                template = new GraphicsConfigTemplate3D();

                if (useStereo) {
                    ((GraphicsConfigTemplate3D) template).setStereo(
                        GraphicsConfigTemplate3D.PREFERRED);
                }
            }
            if (template == null) {
                return d.getDefaultConfiguration();
            } else {
                return d.getBestConfiguration(template);
            }
        } catch (HeadlessException he) {
            return null;
        }
    }

}
