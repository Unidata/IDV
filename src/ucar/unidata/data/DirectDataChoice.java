/*
 * $Id: DirectDataChoice.java,v 1.56 2007/08/08 19:12:51 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of th`397e GNU Lesser General Public License as published by
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


import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.XmlEncoder;



import visad.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;


/**
 * A subclass of DataChoice for supporting a leaf choice.  This choice
 * maps directly to data.
 *
 * @author IDV Development Team
 * @version $Revision: 1.56 $
 */
public class DirectDataChoice extends DataChoice {

    /**
     * This is the XmlEncoder encoding property we use to
     * determine if the datasource should be written out
     */
    public static final String PROP_WRITEDATASOURCE = "prop.writedatasource";


    /** The data source */
    protected DataSource dataSource;

    /**
     * Default bean constructor; does nothing.
     */
    public DirectDataChoice() {}

    /**
     * Create a new DirectDataChoice.
     *
     * @param dataSource     The source of the data
     * @param id             The identifying object.
     * @param name           The short name of this choice.
     * @param description    The long description of this choice.
     * @param categories     List of  {@link DataCategory}s.
     * @param properties     The properties for this data choice (may be null).
     */
    public DirectDataChoice(DataSource dataSource, Object id, String name,
                            String description, List categories,
                            Hashtable properties) {
        this(dataSource, id, name, description, categories,
             NULL_DATASELECTION, properties);
    }


    /**
     * Create a new DirectDataChoice.
     *
     * @param dataSource     The source of the data
     * @param id             The identifying object.
     * @param name           The short name of this choice.
     * @param description    The long description of this choice.
     * @param categories     List of  {@link DataCategory}s.
     */
    public DirectDataChoice(DataSource dataSource, Object id, String name,
                            String description, List categories) {
        this(dataSource, id, name, description, categories,
             NULL_DATASELECTION, NULL_PROPERTIES);
    }


    /**
     * Create a new DirectDataChoice.
     *
     * @param dataSource     The source of the data
     * @param id             The identifying object.
     * @param name           The short name of this choice.
     * @param description    The long description of this choice.
     * @param categories     List of  {@link DataCategory}s.
     * @param dataSelection  The subsetting criteria
     */
    public DirectDataChoice(DataSource dataSource, Object id, String name,
                            String description, List categories,
                            DataSelection dataSelection) {
        this(dataSource, id, name, description, categories, dataSelection,
             NULL_PROPERTIES);
    }




    /**
     * Create a new DirectDataChoice.
     *
     * @param dataSource     The source of the data
     * @param id             The identifying object.
     * @param name           The short name of this choice.
     * @param description    The long description of this choice.
     * @param categories     List of  {@link DataCategory}s.
     * @param dataSelection  The subsetting criteria
     * @param properties     The properties for this data choice (may be null).
     */
    public DirectDataChoice(DataSource dataSource, Object id, String name,
                            String description, List categories,
                            DataSelection dataSelection,
                            Hashtable properties) {
        super(id, name, description, categories, properties);
        //        addCurrentName(name);
        if (dataSource.canAddCurrentName(this)) {
            addCurrentName(
                new TwoFacedObject(
                    DataSourceImpl.getNameForDataSource(dataSource, 30, true)
                    + ">" + name, name));
        }
        this.myDataSelection = dataSelection;
        this.dataSource      = dataSource;
    }

    /**
     * Copy constructor.
     *
     * @param other  other DirectDataChoice to get state from
     *
     */
    public DirectDataChoice(DirectDataChoice other) {
        super(other);
        this.dataSource = other.dataSource;
    }

    /**
     * Method for cloning.  Calls copy constructor.
     *
     * @return  copy of this
     */
    public DataChoice cloneMe() {
        return new DirectDataChoice(this);
    }


    /**
     * Add to the given list all final data choices (i.e.,
     * the leafs of the datachoice tree)
     *
     * @param finalDataChoices The list to add to
     */
    public void getFinalDataChoices(List finalDataChoices) {
        super.getFinalDataChoices(finalDataChoices);
        finalDataChoices.add(this);
    }

    /**
     * Get the full description for this DataChoice.  Overrides the
     * superclass method.
     *
     * @return    full description
     */
    public String getFullDescription() {
        return super.getFullDescription() + " from: <br>"
               + dataSource.getPartialDescription();
    }


    /**
     * Add to the given list all the data sources
     *
     * @param dataSources List to put data sources into
     */
    public void getDataSources(List dataSources) {
        dataSources.add(dataSource);
    }

    /**
     * Get all the levels associated with this choice
     *
     *
     * @param dataSelection data selection
     * @return  List of levels
     */
    public List getAllLevels(DataSelection dataSelection) {
        return dataSource.getAllLevels(this,
                                       DataSelection.merge(dataSelection,
                                           myDataSelection));
    }


    /**
     * Get all the times associated with this choice.
     *
     * @return  all times from the superclass or all times from the datasource
     */
    public List getAllDateTimes() {
        if (true) {
            return dataSource.getAllDateTimes(this);
        }
        List mine           = super.getAllDateTimes();
        List theDataSources = dataSource.getAllDateTimes(this);
        if (mine == null) {
            return theDataSources;
        } else if (theDataSources == null) {
            return mine;
        }
        mine = DataSourceImpl.getDateTimes(mine, theDataSources);
        return mine;
    }


    /**
     * Get all the selected times associated with this choice.
     *
     * @return  the selected times
     */
    public List getSelectedDateTimes() {
        List mine           = super.getSelectedDateTimes();
        List theDataSources = dataSource.getSelectedDateTimes();

        if (theDataSources == null) {
            return mine;
        }

        if (mine == null) {
            mine = getAllDateTimes();
        }
        //Make sure we have absolute times.
        mine = DataSourceImpl.getDateTimes(mine, getAllDateTimes());
        theDataSources = DataSourceImpl.getDateTimes(theDataSources,
                dataSource.getAllDateTimes());


        //Now, make sure we only  use the selected ones from the data source that
        //are in our list
        SortedSet uniqueTimes =
            Collections.synchronizedSortedSet(new TreeSet());
        List finalResult = new ArrayList();
        for (int i = 0; i < theDataSources.size(); i++) {
            Object tmp = theDataSources.get(i);
            if (mine.contains(tmp)) {
                uniqueTimes.add(tmp);
            }
        }

        return new ArrayList(uniqueTimes);
    }


    /**
     * This gets called  after the data choice has been unpersisted
     *
     * @param properties Properties
     */
    public void initAfterUnPersistence(Hashtable properties) {
        super.initAfterUnPersistence(properties);
    }




    /**
     * Get the associated {@link DataSource}.  Mostly used by XML persistence.
     *
     * @return  the DataSource.
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Set the associated {@link DataSource}.  Mostly used by XML persistence.
     *
     * @param s  the DataSource.
     */
    public void setDataSource(DataSource s) {
        dataSource = s;
    }


    /**
     * Get the data that this choice represents.
     *
     * @param category          The {@link DataCategory} used to subset this
     *                          call (usually not used but  placed in here
     *                          just in case it is needed.)
     * @param dataSelection     Allows one to subset the data request (e.g.,
     *                          asking for a smaller set of times, etc.)
     * @param requestProperties Extra selection properties
     *
     * @return The data.
     *
     * @throws DataCancelException   if the request to get data is canceled
     * @throws RemoteException       problem accessing remote data
     * @throws VisADException        problem creating the Data object
     */
    protected Data getData(DataCategory category,
                           DataSelection dataSelection,
                           Hashtable requestProperties)
            throws VisADException, RemoteException, DataCancelException {
        //Get  and keep around the DataSelection that will be used for this data choice
        //      if (dataSelection == null) {
        //          dataSelection = dataSource.getDataSelection ();
        //      }
        Data data =
            dataSource.getData(this, category,
                               DataSelection.merge(dataSelection,
                                   myDataSelection), requestProperties);
        return data;
    }



    /**
     * Return the hashcode for this DirectDataChoice
     *
     * @return  the hashcode
     */
    public int hashCode() {
        Object myDataSource = dataSource;
        if(myDataSource==null) return super.hashCode();
        //TODO - is this a good way to combine the hash codes?
        return (myDataSource.hashCode() ^ (super.hashCode()));
    }

    /**
     * Check for equality.
     *
     * @param o   Object to check.
     * @return  true if <code>o</code> is equivalent
     */
    public boolean equals(Object o) {
        if ( !(o instanceof DirectDataChoice)) {
            return false;
        }
        return super.equals(o)
               && ((DirectDataChoice) o).dataSource.equals(dataSource);
    }


    /**
     * This just checks for basic equality. Things like id, datasource, etc.
     *
     * @param that The object ot check for equality
     *
     * @return Is basically equals
     */
    public boolean basicallyEquals(DataChoice that) {
        if ( !super.basicallyEquals(that)) {
            return false;
        }
        return ((DirectDataChoice) that).dataSource.equals(dataSource);

    }


    /**
     * Add the data change listener
     *
     * @param listener   listener to add
     */
    public void addDataChangeListener(DataChangeListener listener) {
        DataSource tmpDataSource = dataSource;
        if(tmpDataSource!=null)
            tmpDataSource.addDataChangeListener(listener);
    }


    /**
     * Remove the data change listener
     *
     * @param listener   listener to remove
     */
    public void removeDataChangeListener(DataChangeListener listener) {
        DataSource tmpDataSource = dataSource;
        if(tmpDataSource!=null)
            tmpDataSource.removeDataChangeListener(listener);
    }



}

