/*
 * $Id: DescriptorDataSource.java,v 1.23 2007/06/14 20:37:12 jeffmc Exp $
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


import ucar.unidata.util.LogUtil;

import visad.Data;
import visad.DataReference;

import visad.VisADException;

import java.rmi.RemoteException;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;




/**
 * This simply holds a list of DerivedDataDescriptors
 * It is used, for example, to represent a collection
 * of end-user defined formulas (i.e., DerivedDataDescriptor)
 *
 * @author Metapps development team
 * @version $Revision: 1.23 $
 */
public class DescriptorDataSource extends DataSourceImpl {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            DescriptorDataSource.class.getName());

    /** list of descriptors */
    List descriptors;

    /**
     * Default constructor.
     */
    public DescriptorDataSource() {}

    /**
     * Construct a DatasourceDescriptor with the given name and
     * description.
     *
     * @param name         name of this
     * @param description  description of what this is.
     */
    public DescriptorDataSource(String name, String description) {
        this(name, description, null);
    }


    /**
     * Construct a DatasourceDescriptor with the given name and
     * description and populate it with the initial list of descriptors.
     *
     * @param name         name of this
     * @param description  description of what this is.
     * @param descriptors  initial list of DerivedDataDescriptors.
     */
    public DescriptorDataSource(String name, String description,
                                List descriptors) {
        super(null, name, description, (Hashtable) null);
        /*
        if (descriptors != null)
            this.descriptors = new ArrayList (descriptors);
        */
        this.descriptors = (descriptors != null)
                           ? new ArrayList(descriptors)
                           : new ArrayList();
    }


    /**
     * Add a new descriptor to the list.
     *
     * @param descriptor  object to add to the list
     */
    public void addDescriptor(DerivedDataDescriptor descriptor) {
        descriptors.add(descriptor);
    }

    /**
     * Remove a new descriptor to the list.
     *
     * @param descriptor  object to remove from the list
     */
    public void removeDescriptor(DerivedDataDescriptor descriptor) {
        descriptors.remove(descriptor);
    }

    /**
     * Get the descriptors held by this object.
     *
     * @return the list of descriptors
     */
    public List getDescriptors() {
        return descriptors;
    }

    /**
     * Set the list of descriptors that this object holds.
     *
     * @param d  list of descriptors.  No check is made on whether this
     *           is a list of descriptors or not.
     */
    public void setDescriptors(List d) {
        descriptors = new ArrayList(d);
    }

    /**
     * Override the superclass method to return null, since this does not
     * have any data.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return   null
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

    /**
     * Get the list of <code>DataChoice-s</code> held by the descriptors
     * of this object.
     *
     * @return list of <code>DataChoice-s</code>
     */
    public List getDataChoices() {
        List      choices = new ArrayList();
        Hashtable seen    = new Hashtable();
        for (int i = 0; i < descriptors.size(); i++) {
            DerivedDataDescriptor ddd =
                (DerivedDataDescriptor) descriptors.get(i);
            if ( !ddd.getIsEndUser()) {
                continue;
            }
            //Be unique
            String key = ddd.getId() + "-" + ddd.getDescription();
            if ( !ddd.getIsLocalUsers() && (seen.get(key) != null)) {
                continue;
            }
            seen.put(key, key);
            //      System.err.println ("ddd:" + ddd + " " + ddd.getIsLocalUsers());
            choices.add(ddd.getDataChoice());
        }
        return choices;
    }


    /**
     * See if the descriptor is already in the list.
     *
     * @param descriptor  descriptor in question.
     *
     * @return  true if it exists in the list.
     */
    public boolean contains(DerivedDataDescriptor descriptor) {
        return descriptors.contains(descriptor);
    }


}

