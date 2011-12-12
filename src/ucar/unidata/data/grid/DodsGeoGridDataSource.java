/*
 * $Id: DodsGeoGridDataSource.java,v 1.26 2006/12/01 20:41:34 jeffmc Exp $
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

package ucar.unidata.data.grid;


import ucar.nc2.dods.DODSNetcdfFile;



import ucar.unidata.data.DataSourceDescriptor;

import java.util.Hashtable;



/**
 * A helper derived class to instantiate a DODS netcdf
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.26 $
 */

public class DodsGeoGridDataSource extends GeoGridDataSource {

    /**
     * Default constuctor; does nothing
     */
    public DodsGeoGridDataSource() {}

    /**
     * Construct a new DodsGeoGridDataSource from the supplied parameter
     *
     * @param descriptor    descriptor for the datasource
     * @param filename      filename or URL
     * @param properties    extra properties
     *
     * @throws java.io.IOException   unable to open file
     */
    public DodsGeoGridDataSource(DataSourceDescriptor descriptor,
                                 String filename, Hashtable properties)
            throws java.io.IOException {
        super(descriptor, DODSNetcdfFile.canonicalURL(filename), properties);
    }



    /**
     * Clean up the url
     *
     * @return the url
     */
    protected String getFilePath() {
        String path = super.getFilePath();
        if (path != null) {
            return DODSNetcdfFile.canonicalURL(path);
        }
        return null;
    }


}

