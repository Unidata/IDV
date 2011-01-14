/*
 * $Id: CompositeDataChoice.java,v 1.36 2006/12/01 20:41:20 jeffmc Exp $
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


import ucar.unidata.util.Misc;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;



/**
 * A class for compositing data choices.  This is useful for having
 * a {@link DataChoice} with associated categories and child choices
 * for the individual components.
 *
 * @author IDV Development Team
 * @version $Revision: 1.36 $
 */

public class CompositeDataChoice extends DirectDataChoice {

    /** Should we ask the data source for our times */
    private boolean useDataSourceToFindTimes = false;

    /** List of composited data choices */
    List dataChoices;

    /**
     * Create a CompositeDataChoices with no associated child choices
     */
    public CompositeDataChoice() {
        dataChoices = new ArrayList();
    }

    /**
     * Create a CompositeDataChoices from another composite.  Clones
     * the set of {@link DataChoice}s from <code>other</code>.
     *
     * @param other   other composite
     */
    public CompositeDataChoice(CompositeDataChoice other) {
        super(other);
        this.dataChoices = cloneDataChoices(other.dataChoices);
    }

    /**
     * Create a composite data choice for the associated
     * {@link DataSource} given the other parameters.
     *
     * @param dataSource   source of the data
     * @param id           id of this choice
     * @param description  description for this choice
     * @param categories   choice categories
     */
    public CompositeDataChoice(DataSource dataSource, Object id,
                               String description, List categories) {
        this(dataSource, id, id.toString(), description, categories);
    }

    /**
     * Create a composite data choice for the associated
     * {@link DataSource} given the other parameters.
     *
     * @param dataSource   source of the data
     * @param id           id of this choice
     * @param name         name of this choice
     * @param description  description for this choice
     * @param categories   choice categories
     */
    public CompositeDataChoice(DataSource dataSource, Object id, String name,
                               String description, List categories) {
        this(dataSource, id, name, description, categories, null);
    }


    /**
     * Create a composite data choice for the associated
     * {@link DataSource} given the other parameters.
     *
     * @param dataSource   source of the data
     * @param id           id of this choice
     * @param name         name of this choice
     * @param description  description for this choice
     * @param categories   choice categories
     * @param properties   miscellaneous properties for this choice
     */
    public CompositeDataChoice(DataSource dataSource, Object id, String name,
                               String description, List categories,
                               Hashtable properties) {
        super(dataSource, id, name, description, categories, properties);
        dataChoices = new ArrayList();
    }


    /**
     * Gets called by the GUI to incrementally add children if needed
     */
    public void expandIfNeeded() {
        dataSource.expandIfNeeded(this);
    }


    /**
     * Method for cloning this choice.
     *
     * @return  new DataChoice with same properties as this.
     */
    public DataChoice cloneMe() {
        return new CompositeDataChoice(this);
    }


    /**
     * This gets called  after the data choice has been unpersisted
     *
     * @param properties Properties
     */
    public void initAfterUnPersistence(Hashtable properties) {
        super.initAfterUnPersistence(properties);
        for (int i = 0; i < dataChoices.size(); i++) {
            ((DataChoice) dataChoices.get(i)).initAfterUnPersistence(
                properties);
        }
    }



    /**
     * Add to the given list all final data choices (i.e.,
     * the leafs of the datachoice tree)
     *
     * @param finalDataChoices The list to add to
     */
    public void getFinalDataChoices(List finalDataChoices) {
        super.getFinalDataChoices(finalDataChoices);
        for (int i = 0; i < dataChoices.size(); i++) {
            ((DataChoice) dataChoices.get(i)).getFinalDataChoices(
                finalDataChoices);
        }
    }



    /**
     * Get the number of child {@link DataChoice}s
     *
     * @return   number of child choices
     */
    public int getNumChildren() {
        return dataChoices.size();
    }


    /**
     * Get all the levels associated with this choice
     *
     *
     * @param dataSelection data selection
     * @return  List of levels
     */
    public List getAllLevels(DataSelection dataSelection) {
        Hashtable seen = new Hashtable();
        List      mine = new ArrayList();
        dataSelection = DataSelection.merge(dataSelection, myDataSelection);
        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice child = (DataChoice) dataChoices.get(i);
            if (child != null) {
                Misc.addUnique(mine, child.getAllLevels(dataSelection), seen);
            }
        }
        return mine;
    }





    /**
     * Override the superclass method for this.
     *
     * @return a unique composite of the times for all child choices.
     */
    public List getAllDateTimes() {
        Hashtable seen = new Hashtable();
        List      mine = new ArrayList();
        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice child = (DataChoice) dataChoices.get(i);
            if (child != null) {
                Misc.addUnique(mine, child.getAllDateTimes(), seen);
            }
        }
        return mine;
    }


    /**
     * Override the superclass method for this.
     *
     * @return a unique set of the selected times of all child choices
     */
    public List getSelectedDateTimes() {
        if (useDataSourceToFindTimes) {
            return dataSource.getSelectedDateTimes();
        }
        List      mine = new ArrayList();
        Hashtable seen = new Hashtable();
        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice child = (DataChoice) dataChoices.get(i);
            if (child != null) {
                Misc.addUnique(mine, child.getSelectedDateTimes(), seen);
            }
        }
        return mine;
    }





    /**
     * Add a {@link DataChoice} to the composite
     *
     * @param c   choice to add
     */
    public void addDataChoice(DataChoice c) {
        dataChoices.add(c);
        c.setParent(this);
    }


    /**
     * Replace the given child with the given chold
     *
     *
     * @param oldDataChoice old one
     * @param newDataChoice new one
     */
    public void replaceDataChoice(DataChoice oldDataChoice,
                                  DataChoice newDataChoice) {
        int idx = dataChoices.indexOf(oldDataChoice);
        newDataChoice.setParent(this);
        oldDataChoice.setParent(null);
        if (idx >= 0) {
            dataChoices.set(idx, newDataChoice);
        } else {
            addDataChoice(newDataChoice);
        }
    }


    /**
     * Remove the current children and add the new ones
     *
     * @param newChoices New children data choices
     */
    public void replaceDataChoices(List newChoices) {
        removeAllDataChoices();
        for (int i = 0; i < newChoices.size(); i++) {
            addDataChoice((DataChoice) newChoices.get(i));
        }
    }


    /**
     * Remove the current children
     *
     */
    public void removeAllDataChoices() {
        for (int i = 0; i < dataChoices.size(); i++) {
            ((DataChoice) dataChoices.get(i)).setParent(null);
        }
        dataChoices = new ArrayList();
    }

    /**
     * Remove a {@link DataChoice} from the composite
     *
     * @param c  choice to remove
     */
    public void removeDataChoice(DataChoice c) {
        dataChoices.remove(c);
        c.setParent(null);
    }

    /**
     * Get the list of child data choices.
     *
     * @return  list of children
     */
    public List getDataChoices() {
        DataSource dataSource = getDataSource();
        dataChoices = dataSource.getCompositeDataChoices(this, dataChoices);
        return dataChoices;
    }

    /**
     * Set the list of child data choices for this composite
     *
     * @param l   list of choices
     */
    public void setDataChoices(List l) {
        dataChoices = l;
    }


    /**
     * Set the UseDataSourceToFindTimes property.
     *
     * @param value The new value for UseDataSourceToFindTimes
     */
    public void setUseDataSourceToFindTimes(boolean value) {
        useDataSourceToFindTimes = value;
    }

    /**
     * Get the UseDataSourceToFindTimes property.
     *
     * @return The UseDataSourceToFindTimes
     */
    public boolean getUseDataSourceToFindTimes() {
        return useDataSourceToFindTimes;
    }


}

