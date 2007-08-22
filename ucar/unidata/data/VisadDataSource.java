/*
 * $Id: VisadDataSource.java,v 1.18 2007/04/16 17:04:43 jeffmc Exp $
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

package ucar.unidata.data;



import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;

import ucar.unidata.data.DirectDataChoice;

import ucar.unidata.util.Misc;



import visad.*;

import visad.data.DefaultFamily;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * This is an implementation that will read in a generic data file
 * and return a single Data choice that is a VisAD Data object.
 */
public class VisadDataSource extends FilesDataSource {

    /** default family for reading data */
    DefaultFamily family = new DefaultFamily("VisADDataSource");

    /** the data */
    Data myData = null;

    /**
     *  Parameterless ctro for xml encoding.
     */
    public VisadDataSource() {}


    /**
     * Just pass through to the base class the ctor arguments.
     * @param descriptor    Describes this data source, has a label etc.
     * @param filename      This is the filename (or url) that
     *                      points to the actual data source. Can be
     *                      referenced with DataSourceImpl.getName
     * @param properties General properties used in the base class
     *
     * @throws VisADException   problem getting the data
     */
    public VisadDataSource(DataSourceDescriptor descriptor, String filename,
                           Hashtable properties)
            throws VisADException {
        super(descriptor, Misc.newList(filename), filename,
              "Visad data source", properties);
        openData();
    }

    /**
     * Can this data source save its file to local disk
     *
     * @return can save to local disk
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
        openData();
    }

    /**
     * Open the data.  Wrapper for family.open()
     */
    private void openData() {
        try {
            myData = family.open(sources.get(0).toString());
        } catch (Exception exc) {
            setInError(true,
                       "Failed to open " + getName() + " "
                       + exc.getMessage());
        }
    }


    /**
     * This method is called at initialization time and  should create
     * a set of {@link ucar.unidata.data.DirectDataChoice}-s  and add them
     * into the base class managed list of DataChoice-s with the method
     * addDataChoice.
     */
    protected void doMakeDataChoices() {
        //Now let's create a data choice for the file 
        String name        = "VisAD Data";
        String description = (String) getProperty(PROP_DATACHOICENAME);

        if (description != null) {
            name = description;
        } else {
            description = "Generic VisAD Data Object";
            try {
                description = myData.getType().toString();
            } catch (Exception excp) {
                ;
            }
        }
        List categories =
            DataCategory.parseCategories(DataCategory.CATEGORY_VISAD + ";"
                                         + description, false);
        DataChoice main = new CompositeDataChoice(this, description, name,
                              description, categories);
        if (myData instanceof Tuple) {
            try {
                TupleType tt = (TupleType) myData.getType();
                for (int i = 0; i < tt.getDimension(); i++) {
                    MathType mt = tt.getComponent(i);
                    DataChoice cc = new DirectDataChoice(this, mt,
                                        "Tuple Component " + i,
                                        mt.toString(), categories,
                                        DataChoice.NULL_PROPERTIES);
                    ((CompositeDataChoice) main).addDataChoice(cc);
                }
            } catch (Exception excp) {
                ;
            }
        }
        addDataChoice(main);
    }


    /**
     * This method should create and return the visad.Data that is
     * identified by the given {@link ucar.unidata.data.DataChoice}.
     *
     * @param dataChoice     This is one of the DataChoice-s that was created
     *                       in the doMakeDataChoices call above.
     * @param category       The specific {@link ucar.unidata.data.DataCategory}
     *                       which the {@link ucar.unidata.idv.DisplayControl}
     *                       was instantiated with. Usually can be ignored.
     * @param dataSelection  This may contain a list of times which
     *                       subsets the request.
     * @param requestProperties  extra request properties
     * @return The {@link visad.Data} object represented by the given dataChoice
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        Data data = (dataChoice instanceof CompositeDataChoice)
                    ? myData
                    : ((Tuple) myData).getComponent(
                        ((TupleType) myData.getType()).getIndex(
                            (MathType) dataChoice.getId()));

        return data;
    }


    /**
     * You can also override the base class method to return the list
     * of all date/times that this DataSource holds.
     * @return This should be an List of {@link visad.DateTime} objects.
     */
    protected List doMakeDateTimes() {
        // don't know for this, so return empty ArrayList
        return new ArrayList();
    }

}

