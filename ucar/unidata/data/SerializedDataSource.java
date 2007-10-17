/*
 * $Id: SerializedDataSource.java,v 1.2 2007/08/19 15:54:51 jeffmc Exp $
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

package ucar.unidata.data;


import org.w3c.dom.*;



import ucar.unidata.util.CacheManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlUtil;



import visad.*;

import java.io.*;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import java.util.zip.*;


/**
 * This data source knows how to read in a zip file that contains an index xml file and a set of
 * serialized visad.Data objects
 *
 * @author IDV development team
 * @version $Revision: 1.2 $ $Date: 2007/08/19 15:54:51 $
 */
public class SerializedDataSource extends FilesDataSource {


    /** xml thing */
    public static final String TAG_SERIALIZEDDATA = "serializeddata";

    /** xml thing */
    public static final String TAG_DATA = "data";

    /** xml thing */
    public static final String ATTR_NAME = "name";

    /** xml thing */
    public static final String ATTR_FILE = "file";

    /** xml thing */
    public static final String ATTR_ICON = "icon";

    /** xml thing */
    public static final String ATTR_CATEGORIES = "categories";

    /**
     * Dummy constructor so this object can get unpersisted.
     */
    public SerializedDataSource() {}


    /** Holds the data choices we ake when we first parse the xml */
    private List dataChoices = new ArrayList();


    /**
     * Create a SerializedDataSource from the specification given.
     *
     * @param descriptor          descriptor for the data source
     * @param source of file      file name (or directory)
     * @param properties          extra properties
     *
     * @throws VisADException     some problem occurred creating data
     */
    public SerializedDataSource(DataSourceDescriptor descriptor,
                                String source, Hashtable properties)
            throws VisADException {
        super(descriptor, source, "Serialized data source", properties);
    }





    /**
     * init me
     */
    public void initAfterCreation() {
        loadFile();
    }

    /**
     * Load in the zip file
     */
    private void loadFile() {
        try {
            BufferedInputStream bin =
                new BufferedInputStream(IOUtil.getInputStream(getFilePath()));
            ZipInputStream zin = new ZipInputStream(bin);
            ZipEntry       ze  = null;
            while ((ze = zin.getNextEntry()) != null) {
                String name = ze.getName().toLowerCase();
                if (name.endsWith(".xser")) {
                    String xml = new String(IOUtil.readBytes(zin, null,
                                     false));
                    processToc(XmlUtil.getRoot(xml));
                }
            }
            zin.close();
        } catch (Exception iexc) {
            LogUtil.logException(
                "There was an error processing the serialized data file:\n "
                + getFilePath(), iexc);
            setInError(true);
        }
    }


    /**
     * Process the xml toc
     *
     * @param root xml root
     */
    private void processToc(Element root) {
        List children = XmlUtil.findChildren(root, TAG_DATA);
        for (int i = 0; i < children.size(); i++) {
            Element   child = (Element) children.get(i);
            Hashtable props = new Hashtable();
            if (XmlUtil.hasAttribute(child, ATTR_ICON)) {
                props.put(DataChoice.PROP_ICON,
                          XmlUtil.getAttribute(child, ATTR_ICON));
            }


            List categories =
                DataCategory.parseCategories(XmlUtil.getAttribute(child,
                    "categories"), false);
            dataChoices.add(new DirectDataChoice(this,
                    XmlUtil.getAttribute(child, ATTR_FILE),
                    XmlUtil.getAttribute(child, ATTR_NAME),
                    XmlUtil.getAttribute(child, ATTR_NAME), categories,
                    props));
        }
    }





    /**
     * Create the data choices associated with this source.
     */
    protected void doMakeDataChoices() {
        if (dataChoices.size() == 0) {
            loadFile();
        }
        for (int i = 0; i < dataChoices.size(); i++) {
            addDataChoice((DataChoice) dataChoices.get(i));
        }
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

        String file = dataChoice.getId().toString();
        try {
            BufferedInputStream bin =
                new BufferedInputStream(IOUtil.getInputStream(getFilePath()));
            ZipInputStream zin = new ZipInputStream(bin);
            ZipEntry       ze  = null;
            while ((ze = zin.getNextEntry()) != null) {
                String name = ze.getName().toLowerCase();
                if (name.equals(file)) {
                    byte[] bytes = IOUtil.readBytes(zin, null, false);
                    zin.close();
                    return (Data) Misc.deserialize(bytes);
                }
            }
            zin.close();
            LogUtil.userErrorMessage("Could not find the serialized data:"
                                     + file);
        } catch (Exception iexc) {
            LogUtil.logException(
                "There was an error reading the data for:\n " + file, iexc);
        }
        return null;
    }




    /**
     * Create a list of times for this data source.  Since shapefiles
     * don't have any times, return an empty List.
     *
     * @return  an empty List
     */
    protected List doMakeDateTimes() {
        return new ArrayList();
    }



}

