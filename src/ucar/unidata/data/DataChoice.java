/*
 * $Id: DataChoice.java,v 1.86 2007/06/21 14:44:58 jeffmc Exp $
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


import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlUtil;


import visad.*;

import visad.georef.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


/**
 * An abstract base class that represents some selection of data. A DataChoice
 * is created with an identifying Object (e.g., a date/time, a field name, etc.)
 * a String name, a String description and a set of {@link DataCategory}s that
 * represent the flavor or flavors of data provided by this DataChoice.
 *
 * @author IDV development team
 * @version $Revision: 1.86 $
 */
public abstract class DataChoice {

    /** Holds all of the data choice names. We use this so different guis can show these names */
    private static Hashtable currentNamesMap = new Hashtable();

    /** Holds all of the data choice names. We use this so different guis can show these names */
    private static List currentNames = new ArrayList();


    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(DataChoice.class.getName());

    /** A <code>null</code> {@link DataSelection} */
    public static final DataSelection NULL_DATASELECTION = null;

    /** A <code>null</code> properties intrinsic */
    public static final Hashtable NULL_PROPERTIES = null;


    /** The requester property */
    public static final String PROP_REQUESTER = "prop.requester";

    /** The icon property */
    public static final String PROP_ICON = "prop.icon";


    /** The <code>null</code> request properties intrinsic */
    public static final Hashtable NULL_REQUESTPROPERTIES = null;


    /**
     *  A general properties table.
     */
    protected Hashtable properties;


    /** The request properties that this data choice holds */
    private Hashtable fixedRequestProperties;


    /**
     * DataChoice-s can follow a composite pattern. The parent member
     * is a (possibly null) reference to the parent DataChoice.
     */
    protected DataChoice parent;

    /**
     *  The identifying object.
     */
    protected Object id;

    /**
     *  Short descriptive name (e.g., T, U, etc.).
     */
    protected String name;

    /**
     * Long descriptive name (e.g, "isobaric temperature", "u component
     * of wind", etc.)
     */
    protected String description;

    /**
     *  List of {@link DataCategory}s.
     */
    List categories;

    /**
     *  Allows us to subset a data choice, e.g., picking a subset of times.
     */
    DataSelection myDataSelection;


    /**
     *  The bean constructor. We need this for xml decoding.
     */
    public DataChoice() {}


    /**
     * Create a new DataChoice, using the state of the given DataChoice to
     * initialize the new object.
     *
     * @param other      The other data choice.
     */
    public DataChoice(DataChoice other) {
        this.id          = other.id;
        if(other.properties!=null) {
            this.properties  = new Hashtable();
            this.properties.putAll(other.properties);
        }
        this.name        = other.name;
        this.description = other.description;
        this.categories  = new ArrayList(other.categories);
        if (other.myDataSelection != null) {
            this.myDataSelection = other.myDataSelection.cloneMe();
        }
        if (other.fixedRequestProperties != null) {
            this.fixedRequestProperties =
                (Hashtable) other.fixedRequestProperties.clone();
        }
    }


    /**
     *  Create a new DataChoice.
     *
     *  @param id The identifying object.
     *  @param name The short name of this choice.
     *  @param description The long description of this choice.
     *  @param categories List of  {@link DataCategory}s.
     */
    public DataChoice(Object id, String name, String description,
                      List categories) {
        this(id, name, description, categories, NULL_PROPERTIES);
    }


    /**
     * Create a new DataChoice.
     *
     * @param id             The identifying object.
     * @param name           The short name of this choice.
     * @param description    The long description of this choice.
     * @param categories     List of  {@link DataCategory}s.
     * @param properties     The properties for this data choice (may be null).
     */
    public DataChoice(Object id, String name, String description,
                      List categories, Hashtable properties) {
        this.id          = id;
        this.name        = name;
        this.description = description;
        if (this.description == null) {
            this.description = this.name;
        }
        if (this.description == null) {
            this.name        = "Data";
            this.description = "Data";
        }

        this.properties = properties;
        if (categories != null) {
            this.categories = new ArrayList(categories);
        } else {
            this.categories = new ArrayList();
        }
        addParamNameToCategories();
    }


    /**
     * Create a new DataChoice. Use a null list of categories.
     *
     * @param id             The identifying object.
     * @param description    The long description of this choice.
     */
    public DataChoice(Object id, String description) {
        this(id, description, (List) null);
    }

    /**
     * Create a new DataChoice. Use the description as the name of the
     * DataChoice.
     *
     * @param id             The identifying object.
     * @param description    The long description of this choice.
     * @param categories     List of  {@link DataCategory}s.
     */
    public DataChoice(Object id, String description, List categories) {
        this(id, description, description, categories);
    }

    /**
     *  Create a new DataChoice.
     *
     *  @param id            The identifying object.
     *  @param description   The long description of this choice.
     *  @param category      The DataCategory.
     */
    public DataChoice(Object id, String description, DataCategory category) {
        this(id, description, Misc.newList(category));
    }


    /**
     * This adds into a globallist the given param name
     *
     * @param name param name to add
     */
    public static void addCurrentName(Object name) {
        if ((name == null) || (currentNamesMap.get(name) != null)) {
            return;
        }
        currentNamesMap.put(name, name);
        currentNames.add(name);
    }

    /**
     * Get the list of all data choice names there ever was during the run
     *
     * @return data choice names
     */
    public static List getCurrentNames() {
        return new ArrayList(currentNames);
    }


    /**
     * This gets called  after the data choice has been unpersisted
     *
     * @param properties Properties
     */
    public void initAfterUnPersistence(Hashtable properties) {}


    /**
     * Add to the given list all final data choices (i.e.,
     * the leafs of the datachoice tree)
     *
     * @param dataChoices List to put data choices into
     */
    public void getFinalDataChoices(List dataChoices) {}

    /**
     * Add to the given list all the data sources
     *
     * @param dataSources List to put data sources into
     */
    public void getDataSources(List dataSources) {}



    /**
     * Add the DataCategory "param:<param name>" and
     * "param:<canonical name>" to the list
     * of data categories.
     */
    protected void addParamNameToCategories() {
        //Only add the param: category if there is at least one non display data category present
        boolean haveNonDisplay = false;
        for (int i = 0; (i < categories.size()) && !haveNonDisplay; i++) {
            haveNonDisplay =
                !((DataCategory) categories.get(i)).getForDisplay();
        }
        if ( !haveNonDisplay) {
            return;
        }

        categories.add(new DataCategory("param:" + name, false));
        String canonicalName = DataAlias.aliasToCanonical(name);
        if ((canonicalName != null) && !canonicalName.equals(name)) {
            //      System.err.println ("adding param:" + name + " canon:" + canonicalName);
            categories.add(new DataCategory("param:" + canonicalName, false));
        } else {
            //      System.err.println ("adding param:" + name);
        }
    }


    /**
     * Set the data time selection for this choice
     *
     * @param dataSelection The data Selection
     */
    public void setDataSelection(DataSelection dataSelection) {
        myDataSelection = dataSelection;
    }



    /**
     * Get the data selection for this choice
     *
     *
     * @return the dataselection
     */
    public DataSelection getDataSelection() {
        return myDataSelection;
    }

    /**
     * Set the time selection for this choice
     *
     * @param times  List of times
     */
    public void setTimeSelection(List times) {
        if (myDataSelection == null) {
            myDataSelection = new DataSelection(times);
        } else {
            myDataSelection.setTimes(times);
        }
    }


    /**
     * Set the level
     *
     * @param level The level
     */
    public void setLevelSelection(Real level) {
        if (myDataSelection == null) {
            myDataSelection = new DataSelection();
        }
        myDataSelection.setLevel(level);

    }

    /**
     * Get all the levels associated with this choice
     *
     * @return  List of levels
     */
    public List getAllLevels() {
        return getAllLevels(null);
    }


    /**
     * Get all the levels associated with this choice
     *
     *
     * @param dataSelection data selection
     * @return  List of levels
     */
    public List getAllLevels(DataSelection dataSelection) {
        return null;
    }


    /**
     * Get all the times associated with this choice
     *
     * @return  List of times
     */
    public List getAllDateTimes() {
        if (myDataSelection != null) {
            return myDataSelection.getTimes();
        }
        return null;
    }


    /**
     * Get the List of selected times (sub selection of all times).
     *
     * @return  selected times
     */
    public List getSelectedDateTimes() {
        if (myDataSelection != null) {
            return myDataSelection.getTimes();
        }
        return null;
    }

    /**
     * Utility method to clone and return a list of DataChoice-s.
     *
     * @param listOfChoices The source list of choices.
     *
     * @return The list of cloned choices.
     */
    public static List cloneDataChoices(List listOfChoices) {
        if (listOfChoices != null) {
            ArrayList dataChoices = new ArrayList();
            for (int i = 0; i < listOfChoices.size(); i++) {
                DataChoice dc = (DataChoice) listOfChoices.get(i);
                if (dc == null) {  //?
                    dataChoices.add(null);
                } else {
                    dataChoices.add(dc.createClone());
                }
            }
            return dataChoices;
        }
        return null;

    }


    /**
     * Create a new instance of this DataChoice.
     *
     * @return The new instance.
     */
    public final DataChoice createClone() {
        return cloneMe();
    }




    /**
     * Create a new instance of this DataChoice.
     *
     * @return The new instance.
     */
    public abstract DataChoice cloneMe();


    /**
     * Return the parent of this DataChoice (may be, and usually is,  null).
     * This is used for the DerivedDataChoice-s somewhat composite pattern.
     *
     * @return The parent of this data choice.
     */
    public DataChoice getParent() {
        return parent;
    }

    /**
     * Set the parent of this DataChoice.  This is used by the
     * CompositeDataChoice
     *
     * @param parent The new parent of this data choice.
     */
    public void setParent(DataChoice parent) {
        this.parent = parent;
    }


    /**
     * A DataChoice can be not intended to be shown to the user within a UI.
     * For example,  it may be a DerivedDataChoice that is used to calculate
     * an intermediate value but where it doesn't make sense to show it to
     * the user.
     *
     * @return Is this DataChoice intended for the user to see.
     */
    public boolean getForUser() {
        return getProperty("forUser", true);
    }


    /**
     * Return the {@link DataCategory} that is meant to be used for displaying
     * this DataChoice within a UI. For now just return the first category
     * this is "forDisplay".
     *
     * @return The DataCategory used for display.
     */
    public DataCategory getDisplayCategory() {
        if (categories == null) {
            return null;
        }
        for (int i = 0; i < categories.size(); i++) {
            DataCategory cat = (DataCategory) categories.get(i);
            if (cat.getForDisplay()) {
                return cat;
            }
        }
        return null;
    }


    /**
     * Add the given {@link DataCategory} into the list of data categories.
     *
     * @param newCategory    The new {@link DataCategory}
     */
    public void addCategory(DataCategory newCategory) {
        categories.add(newCategory);
    }


    /**
     * Get the list of {@link DataCategory}s that this DataChoice represents.
     *
     * @return The list of data categories.
     */
    public List getCategories() {
        return categories;
    }

    /**
     * Get the list of DataCategory-s. Note: this is now deprecated. The problem is
     * that we had the getDataCategories and getCategories methods. These would
     * get used in wrting to a bundle and we'd end up clobbering any display categorie.
     *
     * @deprecated
     * @return data categories
     */
    public List getDataCategories() {
        return categories;
    }

    /**
     * Get the list of DataCategory-s but exclude any that are used for display
     * if excludeDisplayCategories is true
     *
     * @param excludeDisplayCategories if true then only return the data categories that are not
     * display categories
     * @return data categories
     */
    public List getDataCategories(boolean excludeDisplayCategories) {
        if ( !excludeDisplayCategories) {
            return categories;
        }
        if (categories == null) {
            return null;
        }
        List l = new ArrayList();
        for (int i = 0; i < categories.size(); i++) {
            DataCategory cat = (DataCategory) categories.get(i);
            if ( !cat.getForDisplay()) {
                l.add(cat);
            }
        }
        return l;


    }


    /**
     * Note: This does not do anything anymore. It still needs to be around
     * for old bundles but the list of categories excluded any display categories
     * and would clobber the main categories list
     *
     * @deprecated  Does nothing  now.
     * @param categories        The list of data categories.
     */
    public void setDataCategories(List categories) {}


    /**
     * Set the list of {@link DataCategory}s that this DataChoice represents.
     *
     * @param categories The list of data categories.
     */
    public void setCategories(List categories) {
        this.categories = ((categories == null)
                           ? new ArrayList()
                           : categories);
    }


    /**
     * Return the {@link visad.Data} object that this DataChoice represents.
     * Merge this object's {@link DataSelection} with the argument
     * DataSelection (which has higher priority).
     *
     * @param incomingDataSelection Allows one to subset the request.
     * @return The data.
     *
     * @throws DataCancelException   if the request to get data is canceled
     * @throws RemoteException       problem accessing remote data
     * @throws VisADException        problem creating the Data object
     */
    public final Data getData(DataSelection incomingDataSelection)
            throws VisADException, RemoteException, DataCancelException {
        return getData(
            (DataCategory) null,
            DataSelection.merge(incomingDataSelection, myDataSelection),
            (Hashtable) null);
    }


    /**
     * Return the {@link visad.Data} object that this DataChoice represents.
     * Merge this object's {@link DataSelection} with the argument
     * DataSelection (which has higher priority).
     *
     * @param incomingDataSelection      Allows one to subset the request.
     * @param requestProperties          The object requesting this data.
     * @return The data.
     *
     * @throws DataCancelException   if the request to get data is canceled
     * @throws RemoteException       problem accessing remote data
     * @throws VisADException        problem creating the Data object
     */
    public final Data getData(DataSelection incomingDataSelection,
                              Hashtable requestProperties)
            throws VisADException, RemoteException, DataCancelException {
        fixedRequestProperties = mergeRequestProperties(requestProperties,
                fixedRequestProperties);
        Hashtable tmp = null;
        if (fixedRequestProperties != null) {
            tmp = new Hashtable(fixedRequestProperties);
        }

        Data data = getData((DataCategory) null,
                            DataSelection.merge(incomingDataSelection,
                                myDataSelection), tmp);


        for (Enumeration keys = tmp.keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            requestProperties.put(key, tmp.get(key));
        }
        return data;
    }



    /**
     * Return the {@link visad.Data} object that this DataChoice represents.
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
    protected abstract Data getData(DataCategory category,
                                    DataSelection dataSelection,
                                    Hashtable requestProperties)
     throws VisADException, RemoteException, DataCancelException;


    /**
     * Merge the given incoming request properties with the ones held by this data choice.
     *
     * @param incoming Incoming choices
     * @param fixedRequestProperties The lower priority hashtable
     *
     * @return Merged set of choices with the incomng having priority.
     */
    public static Hashtable mergeRequestProperties(Hashtable incoming,
            Hashtable fixedRequestProperties) {

        Hashtable result = null;


        if (incoming == null) {
            result = fixedRequestProperties;
        } else if (fixedRequestProperties == null) {
            result = incoming;
        } else {
            result = new Hashtable(incoming);
            for (Enumeration keys = fixedRequestProperties.keys();
                    keys.hasMoreElements(); ) {
                Object key = keys.nextElement();
                if (result.get(key) == null) {
                    result.put(key, fixedRequestProperties.get(key));
                }
            }
        }
        if (result != null) {
            result = new Hashtable(result);
        }

        return result;
    }



    /**
     * Override the hashCodes method.
     *
     * @return Object's hash code value.
     */
    public int hashCode() {
        return Misc.hashcode(id) ^ Misc.hashcode(categories)
               ^ Misc.hashcode(myDataSelection);
    }

    /**
     * Check for equality of this object to another
     *
     * @param other Object to check equality.
     * @return true if equals to the other.
     */
    public boolean equals(Object other) {
        if ( !(other instanceof DataChoice)) {
            return false;
        }
        DataChoice that     = (DataChoice) other;
        boolean    idEquals = idEquals(that);
        return idEquals && Misc.equals(categories, that.categories)
               && Misc.equals(myDataSelection, that.myDataSelection);
    }




    /**
     * This just checks for basic equality. Things like id, datasource, etc.
     *
     * @param that The object ot check for equality
     *
     * @return Is basically equals
     */
    public boolean basicallyEquals(DataChoice that) {
        if ( !getClass().equals(that.getClass())) {
            return false;
        }
        return idEquals(that);
    }


    /**
     * Do the ids equals
     *
     * @param that The dc to check for id equality_
     *
     * @return Are the two ids equal
     */
    private boolean idEquals(DataChoice that) {
        boolean idEquals = Misc.equals(id, that.id);
        if ( !idEquals && id.getClass().equals(that.id.getClass())
                && id.getClass().isArray()) {
            idEquals = java.util.Arrays.equals((Object[]) id,
                    (Object[]) that.id);
        }
        return idEquals;
    }


    /**
     * Return the toString() value of the id.
     *
     * @return the toString() value of the id.
     */
    public String getStringId() {
        if (id == null) {
            return null;
        }
        return id.toString();
    }


    /**
     * Return the indexed name, the default is to simply return the name.
     * This method provides a hook for derived classes to return different
     * names. For example, the {@link DerivedDataChoice} uses this to return
     * the name of the index'th operand.
     *
     * @param index The (0 based) index.
     * @return By default, the name of this DataChoice.
     */
    public String getIndexedName(int index) {
        return getName();
    }

    /**
     * Return the name of this DataChoice.
     *
     * @return The name of the DataChoice.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this DataChoice. This is mostly for the xml encoding.
     *
     * @param newName The new name.
     */
    public void setName(String newName) {
        name = newName;
    }

    /**
     * Return the human readable description. This is typically longer than
     * the name.
     *
     * @return The description of this DataChoice.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the human readable description.  Usually used for xml decoding.
     *
     * @param desc The new description.
     */
    public void setDescription(String desc) {
        description = desc;
    }

    /**
     * This method is used to get a lengthy description of this DataChoice.
     * This is used by the DataTree to show a tooltip. By default it just
     * returns the name concatenated with the description but can be
     * overwritten.
     *
     * @return The name concatenated with the description.
     */
    public String getFullDescription() {
        return "<b>Field:</b> " + getName() + " <b>(" + getDescription() + ")</b>";
    }



    /**
     * DataChoices typically have some identifying Object associated with
     * them (e.g., a time, a field name, etc.)
     *
     * @param  theId The identifying Object.
     */
    public void setId(Object theId) {
        id = theId;
    }

    /**
     * Return the identifying object.
     *
     * @return The identifying object.
     */
    public Object getId() {
        return id;
    }

    /**
     * Return the description.
     *
     * @return The description.
     */
    public String toString() {
        return description;
    }


    /**
     * Set the boolean property.
     *
     * @param name           Property name.
     * @param propertyValue  boolean property value
     */
    public void setProperty(String name, boolean propertyValue) {
        setProperty(name, "" + propertyValue);
    }

    /**
     * Set the String property.
     *
     * @param name           Property name.
     * @param propertyValue  String property value
     */
    public void setObjectProperty(String name, Object propertyValue) {
        if (properties == null) {
            properties = new Hashtable();
        }
        properties.put(name, propertyValue);
    }


    /**
     * Set the property.
     *
     * @param name           Property name.
     * @param propertyValue  String property value
     */
    public void setProperty(String name, String propertyValue) {
        setObjectProperty(name, propertyValue);
    }

    /**
     *  Return the boolean property, if not found return the dflt.
     *
     *  @param name     Property name.
     *  @param dflt     The default value.
     *  @return The boolean property value or the dflt argument if not found.
     */
    public boolean getProperty(String name, boolean dflt) {
        return Misc.getProperty(properties, name, dflt);
    }

    /**
     *  Return the String property, if not found return the dflt.
     *
     *  @param name     Property name.
     *  @param dflt     The default value.
     *  @return The String property value or the dflt argument if not found.
     */
    public String getProperty(String name, String dflt) {
        String v = Misc.getProperty(properties, name, dflt);
        if (v != null) {
            v = v.trim();
        }
        return v;
    }

    /**
     *  Return the property, if not found return the null.
     *
     *  @param name     Property name.
     *  @return The property value
     */
    public Object getProperty(String name) {
        if (properties != null) {
            return properties.get(name);
        }
        return null;
    }



    /**
     * Getter method for the properties hashtable. We have this here for
     * the XmlEncoder.
     *
     * @return The properties table.
     */
    public Hashtable getProperties() {
        return properties;
    }


    /**
     * Setter method for the properties hashtable. We have this here for
     * the XmlEncoder.
     *
     * @param newProperties The new Properties table.
     */
    public void setProperties(Hashtable newProperties) {
        if (newProperties == null) {
            return;
        }
        if (properties == null) {
            properties = new Hashtable();
        }
        properties.putAll(newProperties);
    }



    /**
     * Add the {@link DataChangeListener}. This is the object that is notify
     * when the data that this DataChoice represents has changed. This is an
     * abstract method, the derived classes override it. e.g., the
     * {@link DerivedDataChoice} tells simply passes the listener on to
     * its set of children DataChoices.
     *
     * @param listener The {@link DataChangeListener} to add.
     */
    public abstract void addDataChangeListener(DataChangeListener listener);


    /**
     * Remove the {@link DataChangeListener}.
     *
     * @param listener The {@link DataChangeListener} to remove.
     */
    public abstract void removeDataChangeListener(
            DataChangeListener listener);


    /**
     * Top-level method to determine if this object is an end-user formula.
     * Default is false.
     *
     * @return Is this object an end user formula (i.e., really means is this
     *         object a {@link DerivedDataChoice} created through the formulas
     *         framework.
     *
     */
    public boolean isEndUserFormula() {
        return false;
    }


    /**
     * Set the FixedRequestProperties property.
     *
     * @param value The new value for FixedRequestProperties
     */
    public void setFixedRequestProperties(Hashtable value) {
        fixedRequestProperties = value;
    }

    /**
     * Get the FixedRequestProperties property.
     *
     * @return The FixedRequestProperties
     */
    public Hashtable getFixedRequestProperties() {
        return fixedRequestProperties;
    }




}

