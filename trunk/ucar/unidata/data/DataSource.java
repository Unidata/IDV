/*
 * $Id: DataSource.java,v 1.94 2007/08/06 14:11:34 jeffmc Exp $
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


import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlPersistable;

import visad.Data;
import visad.DataReference;
import visad.VisADException;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.Action;


/**
 * Interface for a source of Data
 *
 * @author IDV Development Team
 * @version $Revision: 1.94 $
 */
public interface DataSource {


    /** properties from the catalogs */
    public static final String PROP_SERVICE_HTTP = "prop.service.http";

    /** property identifier */
    public static final String PROP_SUBPROPERTIES = "prop.subproperties";

    /** Property id for if this data source is cacheable */
    public static final String PROP_CACHEABLE = "DataSource.Cacheable";


    /** icon property name */
    public static final String PROP_ICON = "icon";


    /** property id */
    public static final String PROP_AUTOCREATEDISPLAY =
        "idv.data.autocreatedisplay";

    /** The document links property */
    public final static String PROP_DOCUMENTLINKS = "documentlinks";

    /** The file pattern property */
    public final static String PROP_FILEPATTERN = "prop.filepattern";

    /** The resolver URL property */
    public final static String PROP_RESOLVERURL = "RESOLVERURL";

    /** The polling interval property */
    public final static String PROP_POLLINFO = "prop.pollinfo";

    /** The base directory property */
    public final static String PROP_BASEDIRECTORY = "prop.basedirectory";

    /** The title property */
    public final static String PROP_TITLE = "TITLE";

    /** The name property */
    public static final String PROP_NAME = "name";

    /** The DataChoice name property */
    public static final String PROP_DATACHOICENAME = "datachoicename";

    /** The geolocation property */
    public static final String PROP_GEOLOCATION = "geolocation";

    /** The property for display properties */
    public static final String PROP_DISPLAYPROPERTIES = "displayproperties";


    /** Used in request properties for the time labels */
    public final static String PROP_TIMELABELS = "prop.timelabels";


    /** Ordinal names for images */
    public static final String[] ordinalNames = StringUtil.ordinalNames;


    /** Identifier for most recent properties */
    public static final String MOST_RECENT = "datasource.mostrecent";


    /** ??? */
    public static final String DATAPATH_DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss_z";

    /** ??? */
    public static final String DATAPATH_DATE_PATTERN =
        "\\d\\d\\d\\d_\\d\\d_\\d\\d_\\d\\d_\\d\\d_\\d\\d_[^._]+";

    /**
     * Return the List of {@link DataChoice} objects
     *
     * @return   List of DataChoices
     */
    public List getDataChoices();

    /**
     * Return the visad.Data object represented by the given
     * dataChoice argument
     *
     * @param dataChoice           choice for the data
     * @param category             The data category
     * @param dataSelection        sub selection (i.e. times) criteria
     * @param requestProperties    extra request properties
     * @return  the Data object for the request
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public Data getData(DataChoice dataChoice, DataCategory category,
                        DataSelection dataSelection,
                        Hashtable requestProperties)
     throws VisADException, RemoteException;

    /**
     * Return the visad.Data object represented by the given dataChoice argument
     *
     * @param dataChoice           choice for the data
     * @param category             The data category
     * @param requestProperties    extra request properties
     * @return  the Data object for the request
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public Data getData(DataChoice dataChoice, DataCategory category,
                        Hashtable requestProperties)
     throws VisADException, RemoteException;


    /**
     * Get the property
     *
     * @param name prop name
     *
     * @return prop value
     */
    public Object getProperty(String name);


    /**
     * Human readable name of this DataSource
     *
     * @return  a human readable name
     */
    public String getName();

    /**
     * Used to change what files this data source uses
     *
     * @param files List of files
     */
    public void setNewFiles(List files);

    /**
     * Human readable description of this DataSource
     *
     * @return  a human readable description
     */
    public String getDescription();


    /**
     * A little more detailed description of this DataSource
     *
     * @return  a human readable description
     */
    public String getPartialDescription();

    /**
     * All the details
     *
     * @return  full descriptive name
     */
    public String getFullDescription();


    /**
     * A utility method to find a given DataChoice based on the id
     *
     * @param dataChoiceId   the ID of the DataChoice
     *
     * @return  the DataChoice or null
     */
    public DataChoice findDataChoice(Object dataChoiceId);


    /**
     * A utility method to find all data choices that match the pattern
     *
     *
     * @param id The id
     *
     * @return  List of data choices
     */
    public List findDataChoices(Object id);

    /**
     * Get the type name of this DataSource
     *
     * @return   type name
     */
    public String getTypeName();


    /**
     * Get the list of all levels available from this DataSource
     *
     *
     * @param dataChoice The data choice to get levels for
     * @return  List of all available levels
     */
    public List getAllLevels(DataChoice dataChoice);


    /**
     * Get all levels for the data choice and selection
     *
     * @param dataChoice data choice
     * @param dataSelection selection
     *
     * @return List of levels
     */
    public List getAllLevels(DataChoice dataChoice,
                             DataSelection dataSelection);

    /**
     * Get the list of all times available from this DataSource
     *
     * @return  List of all available times
     */
    public List getAllDateTimes();

    /**
     * Get the list of all times selected from this DataSource
     *
     * @return  List of selected times
     */
    public List getSelectedDateTimes();


    /**
     * Get the list of all times available from the DataChoice
     *
     * @param dataChoice    DataChoice in question
     *
     * @return  List of times
     */
    public List getAllDateTimes(DataChoice dataChoice);

    /**
     * Get the list of all times selected from the DataChoice
     *
     * @param dataChoice    DataChoice in question
     *
     * @return  List of times
     */
    public List getSelectedDateTimes(DataChoice dataChoice);



    /**
     * Set the list of datetimes that this data source should use.
     *
     * @param selectedTimes    List of times to use
     */
    public void setDateTimeSelection(List selectedTimes);

    /**
     * Get the list of datetimes that this data source should use.
     *
     * @return  List of times the data source should use.
     */
    public List getDateTimeSelection();

    /**
     * Get the DataSelection for this DataSource
     *
     * @return  the DataSelection (time sub-selection)
     */
    public DataSelection getDataSelection();


    /**
     * Add the data change listener.
     *
     * @param listener   listener to add
     */
    public void addDataChangeListener(DataChangeListener listener);

    /**
     * Gets called by the DataSelection tree gui when a CompositeDataChoice
     * is first opened. This allows us to incrementally expand these nested
     * data choices.
     *
     * @param cdc the data choice
     */
    public void expandIfNeeded(CompositeDataChoice cdc);

    /**
     * Remove the data change listener.
     *
     * @param listener   listener to remove
     */
    public void removeDataChangeListener(DataChangeListener listener);

    /**
     * Method to call notifying implementers of changes to data
     */
    public void notifyDataChange();

    /**
     * Remove this DataSource
     */
    public void doRemove();


    /**
     * See if this DataSource is in error.
     *
     * @return  true if in error
     */
    public boolean getInError();

    public void setInError(boolean b);

    /**
     * Do we need to show the error  to the user or was it already handled
     *
     * @return need to show the error  to the user
     */
    public boolean getNeedToShowErrorToUser();

    /**
     * Get the current error message.
     *
     * @return  error message
     */
    public String getErrorMessage();


    /**
     * Method to be implemented to have work done after the DataSource
     * has been unpersisted from XML
     */
    public void initAfterUnpersistence();


    /**
     * Intialization method that gets called after the DataSource has been created.
     */
    public void initAfterCreation();


    /**
     * Is this datasource identified by the given defining object.
     *
     * @param definingObject        object in question
     * @return  true if it is
     */
    public boolean identifiedBy(Object definingObject);


    /**
     * Is this datasource identified by the given label.
     * The name may be of the form "class:classpattern" or just a pattern
     * to match the name by
     *
     * @param name the name.
     * @return  true if it is
     */
    public boolean identifiedByName(String name);


    /**
     * Return the alias for this DataSource.
     *
     * @return   alias (i.e., alternate name) for this DataSource
     */
    public String getAlias();

    /**
     * Set the alias
     *
     * @param alias   alias to use
     */
    public void setAlias(String alias);


    /**
     * Return the unique id of this datasource.
     *
     * @return  unique id
     */
    public String getUniqueId();


    /**
     * Get any {@link Action}-s associated with this DataSource.  The actions
     * can be used to create menus, buttons, etc.
     *
     * @return a list of Actions associated with this DataSource
     */
    public List getActions();


    /**
     * Show the properties dialog
     *
     * @return Was ok pressed
     */
    public boolean showPropertiesDialog();


    /**
     * Reload the data and notify any listeners
     */
    public void reloadData();


    /**
     * Utility to update the children data choices
     *
     * @param compositeDataChoice composite data choice
     * @param dataChoices Its children
     *
     * @return The actual children to use
     */
    public List getCompositeDataChoices(
            CompositeDataChoice compositeDataChoice, List dataChoices);


    /**
     * Can this datasource do the geoselection subsetting and decimation
     *
     * @return can do geo subsetting
     */
    public boolean canDoGeoSelection();


    /**
     * Returns a list of DataSelectionComponents that are shown in the field selector
     *
     * @param dataChoice The data choice in the field selector
     *
     * @return The list of components
     */
    public List<DataSelectionComponent> getDataSelectionComponents(
            DataChoice dataChoice);


    /**
     * set the properties
     *
     * @param properties properties
     */
    public void setObjectProperties(Hashtable properties);





    /**
     *  Set the DataIsEditable property.
     *
     *  @param value The new value for DataIsEditable
     */
    public void setDataIsEditable(boolean value);

    /**
     * Has this data source been marked to change it file on a bundle load
     *
     * @return is editable
     */
    public boolean getDataIsEditable();

    /**
     * for changing paths
     *
     * @return paths to change
     */
    public List getTmpPaths();

    /**
     * Update the state
     *
     * @param newObject new object
     * @param newProperties the properties
     */
    public void updateState(Object newObject, Hashtable newProperties);

    /**
     * for changing paths
     *
     * @param strings Changed paths
     */
    public void setTmpPaths(List strings);

    /**
     * This gets called after we have been saved in a zidv bundle to reset us back to any original state
     */
    public void resetTmpState();


    /**
     * Get the list of file or url paths this data source has
     *
     * @return List of files or urls
     */
    public List getDataPaths();


    /**
     * Can this data source saves its files to local disk
     *
     * @return can save to local disk
     */
    public boolean canSaveDataToLocalDisk();


    /**
     * Save the files to local disk
     *
     * @param changeLinks And change the internal file references
     * @param uniqueFilePath Prefix to save files to
     *
     * @return List of files saved
     *
     * @throws java.io.IOException On badness
     */
    public List saveDataToLocalDisk(boolean changeLinks,
                                    String uniqueFilePath)
     throws java.io.IOException;


    /**
     * This is called when the CacheManager detects the need ot clear memory.
     */
    public void clearCachedData();


    /**
     * Automatically create the given display on initialization. This used to be in the IDV
     * but we moved it here to allow different data sources to do different things.
     *
     * @param displayType The display control type id
     * @param dataContext Really, the IDV
     */
    public void createAutoDisplay(String displayType,
                                  DataContext dataContext);

    /**
     * can we add the data choice to the global list of parameter names
     *
     * @param dataChoice the data choice
     *
     * @return can add
     */
    public boolean canAddCurrentName(DataChoice dataChoice);


    public void reloadData(Object object, Hashtable properties);

}

