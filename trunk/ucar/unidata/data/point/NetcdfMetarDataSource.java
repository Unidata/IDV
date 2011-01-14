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

package ucar.unidata.data.point;


import ucar.unidata.data.DataSourceDescriptor;

import ucar.unidata.util.LogUtil;

import visad.VisADException;

import java.rmi.RemoteException;


import java.util.Hashtable;


/**
 * A data source for netCDF METAR data
 *
 * @author Don Murray
 * @deprecated  use NetcdfPointDataSource
 */
public class NetcdfMetarDataSource extends NetcdfPointDataSource {


    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(NetcdfMetarDataSource.class.getName());

    /**
     * Default constructor
     *
     * @throws VisADException  problem creating VisAD data object
     *
     */
    public NetcdfMetarDataSource() throws VisADException {
        super();
    }

    /**
     * Create a NetcdfMetarDataSource
     *
     * @param descriptor    descriptor for the DataSource
     * @param source        file location
     * @param properties    extra properties
     *
     * @throws VisADException
     *
     */
    public NetcdfMetarDataSource(DataSourceDescriptor descriptor,
                                 String source, Hashtable properties)
            throws VisADException {
        super(descriptor, source, properties);
    }

}
