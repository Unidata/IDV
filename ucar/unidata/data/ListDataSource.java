/*
 * $Id: ListDataSource.java,v 1.30 2006/12/01 20:41:23 jeffmc Exp $
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
 * This simply holds a list of DataChoices
 * It is used, for example, to represent a collection
 * of end-user defined formulas (i.e., DerivedDataChoice)
 *
 * @author Metapps development team
 * @version $Revision: 1.30 $
 */


public class ListDataSource extends DataSourceImpl {

    /** list of data choices */
    private List choices = new ArrayList();

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            ListDataSource.class.getName());

    /**
     * Default bean constructor; does nothing
     */
    public ListDataSource() {}

    /**
     * Create a ListDataSource
     *
     * @param name          name of this DataSource
     * @param description   long name
     */
    public ListDataSource(String name, String description) {
        super(null, name, description, (Hashtable) null);
    }

    /**
     * This should never get called.  We need to overwrite this to return null.
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

    /**
     * Get the DataChoices.
     *
     * @return  List of DataChoices.
     */
    public List getDataChoices() {
        return choices;
    }

    /**
     * Set the list of DataChoices
     *
     * @param l  list of DataChoices
     */
    public void setDataChoices(List l) {
        choices = l;
    }

    /**
     * Add a DataChoice.
     *
     * @param choice  choice to add
     */
    public void addDataChoice(DataChoice choice) {
        choices.add(choice);
    }

    /**
     * Remove a DataChoice from the list.
     *
     * @param choice  Choice to remove
     */
    public void removeDataChoice(DataChoice choice) {
        choices.remove(choice);
    }

    /**
     * See if this contains the choice in question.
     *
     * @param choice   choice to look for
     * @return  true if choice is in here
     */
    public boolean contains(DataChoice choice) {
        return choices.contains(choice);
    }

    /**
     * Get the DataChoice at index i
     *
     * @param i   index
     * @return  the choice at <code>i</code>
     */
    public DataChoice get(int i) {
        return (DataChoice) choices.get(i);
    }


    /**
     * Return the number of choices in this data source
     *
     * @return  number of choices
     */
    public int size() {
        return choices.size();
    }


}

