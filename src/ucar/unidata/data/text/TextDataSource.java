/*
 * $Id: TextDataSource.java,v 1.28 2007/04/16 20:34:59 jeffmc Exp $
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

import ucar.unidata.util.IOUtil;
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
import java.util.List;




/**
 * A class for handling text (and HTML) classes
 *
 * @author IDV development team
 * @version $Revision: 1.28 $
 */
public class TextDataSource extends FilesDataSource {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            TextDataSource.class.getName());


    /**
     * Default bean constructor; does nothing.
     *
     */
    public TextDataSource() {}

    /**
     * Create a new TextDataSource
     *
     * @param descriptor    descriptor for this DataSource
     * @param filename      name of the file (or URL)
     * @param properties    extra data source properties
     *
     */
    public TextDataSource(DataSourceDescriptor descriptor, String filename,
                          Hashtable properties) {
        super(descriptor, filename, "Text data source", properties);
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
     * Override base class method to not make any derived data choices
     *
     * @param dataChoices Initial data choices
     */
    protected void makeDerivedDataChoices(List dataChoices) {}




    /**
     * Make the data choices associated with this source
     */
    protected void doMakeDataChoices() {
        String category = getDescriptor().getProperty("categories");
        String docName  = getSource();
        if (category == null) {
            if (IOUtil.isHtmlFile(docName)) {
                category = "html";
            } else if (IOUtil.isTextFile(docName)) {
                category = "text";
            } else {
                category = "unknown";
            }
        }
        addDataChoice(
            new DirectDataChoice(
                this, docName, docName, docName,
                DataCategory.parseCategories(category, false)));
    }

    /**
     * Actually get the data identified by the given DataChoce. The default is
     * to call the getDataInner that does not take the requestProperties. This
     * allows other, non unidata.data DataSource-s (that follow the old API)
     * to work.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The visad.Data object
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        String filename = dataChoice.getStringId();
        try {
            return new visad.Text(filename);
            //            return new visad.Text(IOUtil.readContents(filename));
        } catch (Exception fnfe) {
            LogUtil.printException(log_, "getData", fnfe);
            //        } catch (java.io.IOException ioe) {
            //            LogUtil.printException(log_, "getData", ioe);
        }
        return null;
    }

}

