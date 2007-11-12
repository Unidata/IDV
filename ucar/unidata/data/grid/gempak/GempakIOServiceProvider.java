/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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




package ucar.unidata.data.grid.gempak;


import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

import java.util.List;


/**
 * Abstract class for reading GEMPAK files
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public abstract class GempakIOServiceProvider implements IOServiceProvider {

    /** The netCDF file */
    protected NetcdfFile ncfile;

    /** the file we are reading */
    protected RandomAccessFile raf;

    /** Gempak file reader */
    protected GempakFileReader gemreader;

    /**
     * Open the service provider for reading.
     * @param raf  file to read from
     * @param ncfile  netCDF file we are writing to (memory)
     * @param cancelTask  task for cancelling
     *
     * @throws IOException  problem reading file
     */
    public void open(RandomAccessFile raf, NetcdfFile ncfile,
                     CancelTask cancelTask)
            throws IOException {
        this.raf    = raf;
        this.ncfile = ncfile;
    }

    /**
     * Close this IOSP
     *
     * @throws IOException problem closing file
     */
    public void close() throws IOException {
        ncfile.close();
        raf.close();
    }

    /**
     * Sync and extend
     *
     * @return false
     */
    public boolean syncExtend() {
        return false;
    }

    /**
     * Sync this file to any additions
     *
     * @return true if sync successful
     *
     * @throws IOException problem reading file
     */
    public boolean sync() throws IOException {
        return gemreader.init();
    }

    /**
     * Get the detail information (glorified toString())
     * @return detail information
     */
    public String getDetailInfo() {
        return (gemreader == null)
               ? "No info"
               : gemreader.toString();
    }

    /**
     * Debug string info (why is this in the interface?)
     *
     * @param o  object to debug
     *
     * @return  string rep of the object in question
     */
    public String toStringDebug(Object o) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Read nested data
     *
     * @param v2  Variable to read
     * @param section  section info
     *
     * @return Array of data
     *
     * @throws IOException  problem reading file
     * @throws InvalidRangeException  invalid range
     */
    public Array readNestedData(Variable v2, List section)
            throws IOException, InvalidRangeException {
        throw new UnsupportedOperationException(
            "GEMPAK IOSP does not support nested variables");
    }
}

