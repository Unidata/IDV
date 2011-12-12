/*
 * $Id: MovieDataSource.java,v 1.15 2007/04/16 20:34:59 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.text;



import ucar.unidata.data.*;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import visad.Data;

import visad.DataReference;
import visad.VisADException;
import visad.VisADException;

import java.io.FileInputStream;

import java.rmi.RemoteException;



import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;




/**
 * A DataSource for movie (QuickTime, AVI) data.
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */
public class MovieDataSource extends FilesDataSource {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            MovieDataSource.class.getName());

    /**
     * Default bean constructor; does nothing.
     */
    public MovieDataSource() {}

    /**
     * Create a new  MovieDataSource
     *
     * @param descriptor    descriptor for this data source
     * @param filename      filename (or URL)
     * @param properties    extra properties for the source
     *
     */
    public MovieDataSource(DataSourceDescriptor descriptor, String filename,
                           Hashtable properties) {
        super(descriptor, filename, "Movie data source", properties);
    }


    /**
     * Is this data source capable of saving its data to local disk
     *
     * @return Can save to local disk
     */
    public boolean canSaveDataToLocalDisk() {
        return !isFileBased();
    }


    /**
     * Initialize if being unpersisted.
     */
    public void initAfterUnpersistence() {
        //From a legacy bundle
        if (sources == null) {
            sources = Misc.newList(getName());
        }
        super.initAfterUnpersistence();
    }



    /**
     * Make the data choices associated with this source.
     */
    protected void doMakeDataChoices() {
        String category = "movie";
        String docName  = getSource();
        addDataChoice(
            new DirectDataChoice(
                this, docName, docName, docName,
                DataCategory.parseCategories(category, false)));
    }

    /**
     * Override the base class to do nothing.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return null
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        return null;
    }

}

