/**
 * $Id: DerivedDataChoice.java,v 1.118 2006/12/01 20:41:22 jeffmc Exp $
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


import org.python.core.*;

import org.python.util.*;

import ucar.unidata.idv.JythonManager;

import ucar.unidata.util.CacheManager;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.NamedList;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;



import visad.*;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;



/**
 * A subclass of DataChoice for derived quantities.
 *
 * @author IDV Development Team
 * @version $Revision: 1.118 $
 */
public class ListDataChoice extends DataChoice {


    /** The DataChangeListener_ */
    private List listeners = new ArrayList();



    /**
     * A list of the child DataChoice-s that this composite holds.
     * Each one represents an operand to the formula, java method,
     * or jython code,
     */
    List childrenChoices;


    /**
     *  Dummy param-less constructor so we can be recreated thru reflection.
     */
    public ListDataChoice() {}

    /**
     * The cloning ctor.
     *
     * @param other  The object to instantiate from.
     */
    public ListDataChoice(ListDataChoice other) {
        super(other);
        this.childrenChoices = cloneDataChoices(other.childrenChoices);
    }


    /**
     * The regular constructor from a {@link DerivedDataDescriptor}
     * TODO: Right now the children of a ListDataChoice do not refererence
     * their parent. If they do we will need to clone the children here
     * so a DataChoice only has one parent.
     *
     * @param id The id
     * @param name name
     * @param description  description
     * @param categories  categories
     */
    public ListDataChoice(Object id, String name, String description,
                          List categories) {
        super(id, name, description, categories);
        this.childrenChoices = new ArrayList();
        //        setDescription(convertLabel(getDescription(), childrenChoices));
        //        checkCategories();
    }


    /**
     * ctor
     *
     * @param name name
     * @param children The children data choices
     */
    public ListDataChoice(String name, List children) {
        super(name, name, name, null);
        this.childrenChoices = children;
    }


    /**
     * set the children
     *
     * @param dataChoices the children
     */
    public void setChildrenDataChoices(List dataChoices) {
        this.childrenChoices = dataChoices;
        setDescription(convertLabel(getDescription(), childrenChoices));
    }


    /**
     * Get the data
     *
     * @param category category
     * @param dataSelection selection
     * @param requestProperties properties
     *
     * @return This just returns null.
     *
     * @throws DataCancelException On badness
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Data getData(DataCategory category,
                           DataSelection dataSelection,
                           Hashtable requestProperties)
            throws VisADException, RemoteException, DataCancelException {
        return null;
    }


    /**
     * Get the array of data from the children data choices
     *
     * @param category category
     * @param dataSelection selection
     * @param requestProperties properties
     *
     * @return data from children
     *
     * @throws DataCancelException On badness
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Object[] getDataList(DataCategory category,
                                   DataSelection dataSelection,
                                   Hashtable requestProperties)
            throws VisADException, RemoteException, DataCancelException {
        List data = new ArrayList();
        for (int i = 0; i < childrenChoices.size(); i++) {
            DataChoice child = (DataChoice) childrenChoices.get(i);
            data.add(child.getData(category, dataSelection,
                                   requestProperties));
        }
        return data.toArray(new Object[data.size()]);
    }


    /**
     * Create a new instance of this object.
     *
     * @return The new instance.
     */
    public DataChoice cloneMe() {
        return new ListDataChoice(this);
    }


    /**
     * Return the name of the index'th  child DataChoice.
     *
     * @param index       The (0 based) index.
     * @return The name of the child DataChoice.
     */
    public String getIndexedName(int index) {
        if ((childrenChoices != null) && (index < childrenChoices.size())) {
            return ((DataChoice) childrenChoices.get(index)).getName();
        }
        return super.getName();
    }


    /**
     * This method runs through the list of {@link  DataCategory}-s. If any
     * of them are "inherited" categories then we replace the inherited
     * category with the data categories of the children DataChoice-s.
     */
    protected void checkCategories() {
        //We use a tmp list because the calls to inherit in the loop
        //Can result in the addition of new elements into the categories list
        List tmpCategories = (List) new ArrayList(categories);
        for (int i = 0; i < tmpCategories.size(); i++) {
            DataCategory dataCategory = (DataCategory) tmpCategories.get(i);
            if ( !dataCategory.isInherited()) {
                continue;
            }
            inherit(dataCategory);
        }
    }

    /**
     * This gets called  after the data choice has been unpersisted
     *
     * @param properties Properties
     */
    public void initAfterUnPersistence(Hashtable properties) {
        super.initAfterUnPersistence(properties);
        for (int i = 0; i < childrenChoices.size(); i++) {
            ((DataChoice) childrenChoices.get(i)).initAfterUnPersistence(
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
        for (int i = 0; i < childrenChoices.size(); i++) {
            ((DataChoice) childrenChoices.get(i)).getFinalDataChoices(
                finalDataChoices);
        }
    }


    /**
     * Add to the given list all the data sources
     *
     * @param dataSources List to put data sources into
     */
    public void getDataSources(List dataSources) {
        for (int i = 0; i < childrenChoices.size(); i++) {
            ((DataChoice) childrenChoices.get(i)).getDataSources(dataSources);
        }

    }



    /**
     * This method determines the type of inheritance defined by the
     * given DataCategory. It collects the set of categories from the children
     * DataChoice-s and replaces the given src category with this new set.
     * The different flavors of of inheritance are:
     * <ul>
     *  <li> all -  take the (set) union of all of the categories of all
     *       of the children
     *  <li> child index - take all of the categories form a particular child
     *       DataChoice (e.g., the second child).
     *  <li> category index - take the index'th data category form each child.
     *  <li> child & category index - take the index'th category form a
     *       particular child.
     * </ul>
     *
     *  @param src The {@link DataCategory} to look at.
     */
    protected void inherit(DataCategory src) {
        if (src == null) {
            return;
        }
        List children = childrenChoices;
        //Find the child data choices we are to use (either all of them or the index'th one
        if (src.getChildIndex() >= 0) {
            children = Misc.newList(childrenChoices.get(src.getChildIndex()));
        }
        int categoryIndex = src.getCategoryIndex();

        //Run through each child and grab the appropriate list of categories
        List newCategories = new ArrayList();
        for (int childIdx = 0; childIdx < children.size(); childIdx++) {
            DataChoice child           = (DataChoice) children.get(childIdx);
            List       childCategories = child.getCategories();
            //Do we just select the index'th one?
            if (categoryIndex >= 0) {
                childCategories =
                    Misc.newList(childCategories.get(categoryIndex));
            }
            //Or do we get all of them
            for (int i = 0; i < childCategories.size(); i++) {
                DataCategory childsDataCategory =
                    (DataCategory) childCategories.get(i);
                //Make sure to not insert duplicates
                if ( !newCategories.contains(childsDataCategory)) {
                    newCategories.add(childsDataCategory);
                }
            }
        }

        //Now insert the new categories into our category list
        //at the place held by the src category.
        int srcIndex = categories.indexOf(src);
        for (int i = 0; i < newCategories.size(); i++) {
            //If this is the first new category then replace the src
            DataCategory newCategory = (DataCategory) newCategories.get(i);
            if (src.getAppend() != null) {
                String newCategoryString = newCategory.toString();
                String newSuffix =  DataCategory.DIVIDER + src.getAppend();
                //Don't append the same category onto the end
                //This fixes the 3DGrid->Derived->Derived problem
                if(!newCategoryString.endsWith(newSuffix)) {
                    newCategory =
                        DataCategory.parseCategory(newCategoryString+ newSuffix, 
                                                   newCategory.getForDisplay());
                } 
            }
            if (src.getReplace() != null) {
                String catString = newCategory.toString()
                    + DataCategory.DIVIDER
                    + src.getAppend();
                for(String pair:  StringUtil.split(src.getReplace(),";",true,true)) {
                    List<String>tuple = StringUtil.split(pair,":",true,true);
                    catString = catString.replaceAll(tuple.get(0), tuple.get(1));
                }
                newCategory =
                    DataCategory.parseCategory(catString,
                                               newCategory.getForDisplay());
                //System.err.println ("replace:" + newCategory);
            }

            if (i == 0) {
                categories.set(srcIndex, newCategory);
                //Else add it into the list
            } else {
                categories.add(srcIndex + 1, newCategory);
            }
        }
    }


    /**
     * Iterate through the children DataChoice-s. For each child n replace
     * within the given label the macro "%Nn%" with the name of the
     * child choice. Replace %Dn" with the child's description.
     *
     * @param label         The inital (possibly macro containing)  label.
     * @param dataChoices   The list of child {@link DataChoice}s.
     *
     * @return The instantiated label.
     */
    protected String convertLabel(String label, List dataChoices) {
        for (int i = 0; i < dataChoices.size(); i++) {
            DataChoice choice = (DataChoice) dataChoices.get(i);
            label = StringUtil.replace(label, "%N" + (i + 1) + "%",
                                       choice.getName());
            label = StringUtil.replace(label, "%D" + (i + 1) + "%",
                                       choice.getDescription());
        }
        return label;
    }





    /**
     * Get the full Description for this DataChoice.
     *
     * @return  full description
     */
    public String getFullDescription() {
        StringBuffer sb = new StringBuffer(super.getFullDescription()
                                           + "<br>from: <ul>");
        for (int i = 0; i < childrenChoices.size(); i++) {
            DataChoice child = (DataChoice) childrenChoices.get(i);
            sb.append("<li>" + child.getFullDescription());
        }
        sb.append("</ul>");
        return sb.toString();
    }



    /**
     * Get the union of all of the children {@link DataChoice}-s levels.
     *
     *
     * @param dataSelection data selection
     * @return The union of all of the children {@link DataChoice}-s levels.
     */
    public List getAllLevels(DataSelection dataSelection) {
        String levelsProp = (String) getProperty("levels");
        if (Misc.equals(levelsProp, "false")) {
            return new ArrayList();
        }
        //        System.err.println ("ListDataChoice.getAllLevels");
        List      mine = new ArrayList();
        Hashtable seen = new Hashtable();
        dataSelection = DataSelection.merge(dataSelection, myDataSelection);
        for (int i = 0; i < childrenChoices.size(); i++) {
            DataChoice child = (DataChoice) childrenChoices.get(i);
            if (child != null) {
                Misc.addUnique(mine, child.getAllLevels(dataSelection), seen);
            }
        }
        return mine;
    }





    /**
     * Get the union of all of the children {@link DataChoice}-s times.
     *
     * @return The union of all of the children {@link DataChoice}-s times.
     */
    public List getAllDateTimes() {
        List      mine = new ArrayList();
        Hashtable seen = new Hashtable();
        for (int i = 0; i < childrenChoices.size(); i++) {
            DataChoice child = (DataChoice) childrenChoices.get(i);
            if (child != null) {
                Misc.addUnique(mine, child.getAllDateTimes(), seen);
            }
        }
        return mine;
    }





    /**
     * Get the union of all of the children {@link DataChoice}-s selected times
     *
     * @return The union of all of the children {@link DataChoice}-s
     *         selected times
     */
    public List getSelectedDateTimes() {
        List      mine = new ArrayList();
        Hashtable seen = new Hashtable();
        for (int i = 0; i < childrenChoices.size(); i++) {
            DataChoice child = (DataChoice) childrenChoices.get(i);
            if (child != null) {
                Misc.addUnique(mine, child.getSelectedDateTimes(), seen);
            }
        }
        return mine;
    }




    /**
     *  For now set this object's data categories to be the union
     *  of the  data categories of its sub-data choices.
     */
    protected void findDataCategories() {
        List      dataCategories = new ArrayList();
        Hashtable seenCategories = new Hashtable();
        for (int i = 0; i < childrenChoices.size(); i++) {
            List subCategories =
                ((DataChoice) childrenChoices.get(i)).categories;
            for (int j = 0; j < subCategories.size(); j++) {
                DataCategory subCategory =
                    (DataCategory) subCategories.get(i);
                if (seenCategories.get(subCategory) == null) {
                    seenCategories.put(subCategory, subCategory);
                    dataCategories.add(subCategory);
                }
            }
        }
        setCategories(dataCategories);
    }




    /**
     * Print a list of the child data choices
     */
    void printChildren() {
        for (int i = 0; i < childrenChoices.size(); i++) {
            DataChoice dc = (DataChoice) childrenChoices.get(i);
            System.err.println("child:" + dc.getClass().getName() + " " + dc);
        }
    }


    /**
     * Set the child DataChoices. Mostly used for  xml encoding.
     *
     * @param l   The list of choices
     */
    public void setChoices(List l) {
        childrenChoices = l;
    }

    /**
     * Get the children choices. Mostly used for  xml encoding.
     *
     * @return the list of children
     */
    public List getChoices() {
        return childrenChoices;
    }


    /**
     *  Remove any objects created by this choice from the cache.
     */
    protected void flushCache() {
        CacheManager.remove(this);
    }





    /**
     * Add the data change listeners to the data choice
     *
     * @param dataChoice data choice
     */
    protected void addDataChangeListeners(DataChoice dataChoice) {
        for (int i = 0; i < listeners.size(); i++) {
            dataChoice.addDataChangeListener(
                (DataChangeListener) listeners.get(i));
        }
    }


    /**
     *  Add the data change listener. This overwrites the base class method
     *  to tell each of the children to addDataChangeListener
     *  (i.e., the composite pattern).
     *
     *  @param listener The listener to add.
     */
    public void addDataChangeListener(DataChangeListener listener) {
        listeners.add(listener);
        for (int i = 0; i < childrenChoices.size(); i++) {
            DataChoice child = (DataChoice) childrenChoices.get(i);
            if (child != null) {
                child.addDataChangeListener(listener);
            }
        }
    }


    /**
     *  Remove the data change listener. This overwrites the base class method
     *  to tell each of the children to removeDataChangeListener.
     *  (i.e., the composite pattern).
     *
     *  @param listener The listener to remove.
     */
    public void removeDataChangeListener(DataChangeListener listener) {
        flushCache();
        listeners.remove(listener);
        for (int i = 0; i < childrenChoices.size(); i++) {
            DataChoice child = (DataChoice) childrenChoices.get(i);
            if (child != null) {
                child.removeDataChangeListener(listener);
            }
        }
    }


    /**
     *  Override the hasCode method.
     *
     *  @return The object's hashcode.
     */
    public int hashCode() {
        return super.hashCode() ^ Misc.hashcode(childrenChoices)
               ^ Misc.hashcode(properties);
    }

    /**
     * This just checks for basic equality. Things like id, datasource, etc.
     *
     * @param that The object ot check for equality
     *
     * @return Is basically equals
     */
    public boolean basicallyEquals(DataChoice that) {
        return equals(that);
    }


    /**
     * Override the equals method.
     *
     * @param o  Obejct to compare to.
     *
     * @return Is equals?
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if ( !super.equals(o)) {
            return false;
        }
        if ( !(o instanceof ListDataChoice)) {
            return false;
        }
        ListDataChoice that = (ListDataChoice) o;
        return Misc.equals(properties, that.properties)
               && Misc.equals(childrenChoices, that.childrenChoices);
    }



}

