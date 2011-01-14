/*
 * $Id: AddeTextDataSource.java,v 1.21 2006/12/01 20:42:49 jeffmc Exp $
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


import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import visad.Data;
import visad.DataReference;
import visad.Text;
import visad.VisADException;
import visad.VisADException;

import visad.data.mcidas.AddeTextAdapter;

import java.io.FileInputStream;

import java.rmi.RemoteException;



import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;




/**
 * Class for data sources of ADDE text data.  These may be generic
 * files or weather bulletins
 *
 * @author IDV development team
 * @version $Revision: 1.21 $
 */

public class AddeTextDataSource extends DataSourceImpl {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            AddeTextDataSource.class.getName());

    /**
     * Default bean constructor for persistence; does nothing.
     */
    public AddeTextDataSource() {}

    /**
     * Create a new AddeTextDataSource
     *
     * @param descriptor    descriptor for this source
     * @param filename      ADDE URL
     * @param properties    extra properties for this source
     *
     */
    public AddeTextDataSource(DataSourceDescriptor descriptor,
                              String filename, Hashtable properties) {
        super(descriptor, filename, "Text data source", properties);
    }



    /**
     * Make the data choices assoicated with this source.
     */
    protected void doMakeDataChoices() {
        String category = "unknown";
        String docName  = getName().toLowerCase().trim();
        if (docName.startsWith("adde") && (docName.indexOf("text") > 0)) {
            category = "html";
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
     * @return The visad.Text object
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        String          filename = dataChoice.getStringId();
        AddeTextAdapter ata      = new AddeTextAdapter(filename);
        return ata.getData();
    }

}

