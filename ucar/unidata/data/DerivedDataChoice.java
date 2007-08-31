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
public class DerivedDataChoice extends DataChoice {


    /**
     *  Where we might have come from.
     */
    private DerivedDataDescriptor descriptor;


    /**
     *  Initially null. Will be filled out the first time in the getData call
     */
    private String constructedCode;

    /**
     * Contains a mapping from the operand name (String) to the DataChoice
     * that the user has selected. At first this is null. It is filled out
     * during the getData call.
     */
    private Hashtable userSelectedChoices;


    /**
     * A list of the child DataChoice-s that this composite holds.
     * Each one represents an operand to the formula, java method,
     * or jython code,
     */
    List childrenChoices;

    /**
     *  The java method name. May be null.
     */
    String methodName;

    /**
     *  The jython code. May be null.
     */
    String code;

    /**
     *  The visad formula. May be null.
     */
    String formula;


    /**
     *  This is the context in which this DDC exists within. This interface
     *  (typically instantiated by the IntegratedDataViewer) allows this DDC to
     *  ask for the user to select DataChoice-s, etc.
     */
    DataContext dataContext;

    /**
     * Property indicating this is a derived quantity
     */
    public static String PROP_FROMDERIVED = "fromderived";

    /**
     *  Dummy param-less constructor so we can be recreated thru reflection.
     */
    public DerivedDataChoice() {}

    /**
     * The cloning ctor.
     *
     * @param other  The object to instantiate from.
     */
    public DerivedDataChoice(DerivedDataChoice other) {
        super(other);
        this.dataContext     = other.dataContext;
        this.childrenChoices = cloneDataChoices(other.childrenChoices);
        this.methodName      = other.methodName;
        this.code            = other.code;
        this.formula         = other.formula;
        if (other.userSelectedChoices != null) {
            userSelectedChoices =
                (Hashtable) other.userSelectedChoices.clone();
        }
    }


    /**
     * The regular constructor from a {@link DerivedDataDescriptor}
     * TODO: Right now the children of a DerivedDataChoice do not refererence
     * their parent. If they do we will need to clone the children here
     * so a DataChoice only has one parent.
     *
     * @param dataContext        The context in which this DataChoice exists
     *                           (typically the
     *                           {@link ucar.unidata.idv.IntegratedDataViewer}).
     * @param dataChoices        The list of initial children data choices
     *                           (the operands).
     * @param desc               The long description of this choice.
     * @param ddd                The {@link DerivedDataDescriptor} that holds
     *                           the information to instantiate this choice.
     */
    public DerivedDataChoice(DataContext dataContext, List dataChoices,
                             String desc, DerivedDataDescriptor ddd) {
        super(ddd.id, ddd.getId(), desc, ddd.categories);
        this.dataContext     = dataContext;
        this.properties      = ddd.properties;
        this.childrenChoices = dataChoices;
        if (childrenChoices != null) {
            //Puts the D1,D2,... into the userSelectedChoices
            userSelectedChoices = new Hashtable();
            for (int i = 0; i < childrenChoices.size(); i++) {
                DataChoice dc    = (DataChoice) childrenChoices.get(i);
                String     alias = "D" + (i + 1);
                userSelectedChoices.put(alias, dc);
            }
        }
        this.methodName = ddd.method;
        this.code       = ddd.code;
        this.formula    = ddd.formula;
        setDescription(convertLabel(getDescription(), childrenChoices));
        checkCategories();
    }

    /**
     *  This is the constructor used when creating a DDC as an end user formula.
     *
     *  @param dataContext       The context in which this DataChoice exists
     *                           (typically the
     *                           {@link ucar.unidata.idv.IntegratedDataViewer}).
     *  @param ddd               The {@link DerivedDataDescriptor} that holds
     *                           the information to instantiate this choice.
     */
    public DerivedDataChoice(DataContext dataContext,
                             DerivedDataDescriptor ddd) {
        super(ddd.id, ddd.getId(), ddd.getDescription(), ddd.categories);
        descriptor           = ddd;
        this.dataContext     = dataContext;
        this.properties      = ddd.properties;
        this.childrenChoices = new ArrayList();
        this.methodName      = ddd.method;
        this.code            = ddd.code;
        this.formula         = ddd.formula;
    }

    /**
     * Used when not creating it from a DerivedDataDescriptor.
     *
     * @param dataContext        The context in which this DataChoice exists
     *                           (typically the
     *                           {@link ucar.unidata.idv.IntegratedDataViewer}).
     *  @param dataChoices       The list of initial children data choices
     *                           (the operands).
     *  @param name              The name of this DataChoice.
     *  @param description       The description of this DataChoice.
     *  @param categories        The list if {@link DataCategory}s.
     *  @param method            The method name used as the operator
     *                           (may be null).
     *  @param formula           The formula body used as the operator
     *                           (may be null).
     *  @param code              The code body used as the operator (may be
     *                           null).  (Note: having different code and
     *                           formula attributes is just a hangover from the
     *                           initial development. They are acted upon in
     *                           the same way.
     */
    public DerivedDataChoice(DataContext dataContext, List dataChoices,
                             String name, String description,
                             String categories, String method,
                             String formula, String code) {
        super(name, name, description,
              DataCategory.parseCategories(categories));
        this.dataContext     = dataContext;
        this.childrenChoices = dataChoices;
        this.properties      = new Properties();
        this.methodName      = method;
        this.code            = code;
        this.formula         = formula;
        //If no categories defined then grab the union of the children
        if (this.categories.size() == 0) {
            findDataCategories();
        }

    }

    /**
     * Create a new instance of this object.
     *
     * @return The new instance.
     */
    public DataChoice cloneMe() {
        return new DerivedDataChoice(this);
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
    private void checkCategories() {
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
    private void inherit(DataCategory src) {
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
                newCategory =
                    DataCategory.parseCategory(newCategory.toString()
                        + DataCategory.DIVIDER
                        + src.getAppend(), newCategory.getForDisplay());
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
    private String convertLabel(String label, List dataChoices) {
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
        String extra = "";
        if (formula != null) {
            extra = extra + "<br>Formula: <i>" + formula + "</i><br>";
        }
        StringBuffer sb = new StringBuffer("Derived quantity: "
                                           + super.getFullDescription()
                                           + extra + "<br>from: <ul>");
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
     * @return The union of all of the children {@link DataChoice}-s levels.
     */
    public List getAllLevels() {
        List      mine = new ArrayList();
        Hashtable seen = new Hashtable();
        for (int i = 0; i < childrenChoices.size(); i++) {
            DataChoice child = (DataChoice) childrenChoices.get(i);
            if (child != null) {
                Misc.addUnique(mine, child.getAllLevels(), seen);
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
    private void findDataCategories() {
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
     * A utility to to add (uniquely) the given opName into the list of ops.
     *
     * @param opName        The operand name (e.g., D1)
     * @param data          The data associated  with the operand.
     * @param operands      The list of operand ({@link DataOperand}) objects.
     * @param opsSoFar      A hashtable to keep the operands unique in the list.
     *
     * @return The instance of the {@link DataOperand} that holds
     *         the opName/data pair.
     */
    private static DataOperand addOperand(String opName, Object data,
                                          List operands, Hashtable opsSoFar) {
        DataOperand op = (DataOperand) opsSoFar.get(opName);
        if (op != null) {
            return op;
        }
        op = new DataOperand(opName, data);
        opsSoFar.put(opName, op);
        operands.add(op);
        return op;
    }

    /**
     * Print a list of the child data choices
     */
    private void printChildren() {
        for (int i = 0; i < childrenChoices.size(); i++) {
            DataChoice dc = (DataChoice) childrenChoices.get(i);
            System.err.println("child:" + dc.getClass().getName() + " " + dc);
        }
    }


    /**
     * A utility to collect all of the operands (held as {@link DataOperand}s)
     *
     * @param category       We ignore this.
     * @param dataSelection  We pass this through to the children getData calls.
     * @param requestProperties  extra request properties
     *
     * @return List of {@link DataOperand}s holding the data.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private List collectOperands(DataCategory category,
                                 DataSelection dataSelection,
                                 Hashtable requestProperties)
            throws VisADException, RemoteException {

        List      operands      = new ArrayList();
        Hashtable operandsSoFar = new Hashtable();


        //First pull out the operands from the code
        if (methodName != null) {
            StringBuffer paramString = new StringBuffer();
            for (int i = 0; i < operands.size(); i++) {
                if (i != 0) {
                    paramString.append(",");
                }
                DataOperand op = (DataOperand) operands.get(i);
                paramString.append(op.getName());
            }
            constructedCode = Misc.getClassMethod(methodName) + "("
                              + paramString.toString() + ")";
        } else if (code != null) {
            constructedCode = code;
        } else if (formula != null) {
            constructedCode = formula;
        } else {
            throw new IllegalArgumentException(
                "DerivedDataChoice: no operation defined");
        }


        //Now, pull out the list of operands that are used in the jython code
        List operandsFromCode =
            DerivedDataChoice.parseOperands(constructedCode);




        //Now, split the list between user and non-user operands
        //Make sure all user operands are in the data choices list
        List nonUserOperands = new ArrayList();
        for (int i = 0; i < operandsFromCode.size(); i++) {
            DataOperand operand = (DataOperand) operandsFromCode.get(i);
            if (operand.isUser()) {
                UserDataChoice udc = new UserDataChoice(operand.getName(),
                                         operand.getUserDefault());

                if ( !childrenChoices.contains(udc)) {
                    childrenChoices.add(udc);
                }
            } else {
                nonUserOperands.add(operand);
            }
        }

        /*        System.err.println("formula:" +constructedCode);
                  System.err.println("userSelectedChoices= " + userSelectedChoices);
                  System.err.println("children:" + childrenChoices);
        */

        //First see if we have any UserDataChoice children (i.e., from 
        //the user:param_name in derived.xml)
        List unboundUserChoices  = new ArrayList();
        List unboundUserOperands = new ArrayList();
        List allUserOperands     = new ArrayList();
        for (int i = 0; i < childrenChoices.size(); i++) {
            DataChoice dc = (DataChoice) childrenChoices.get(i);
            if (dc instanceof UserDataChoice) {
                DataOperand operand = new DataOperand(dc.getName(),
                                          ((UserDataChoice) dc).getValue());
                allUserOperands.add(operand);
                if ( !operand.isBound()) {
                    unboundUserChoices.add(dc);
                    unboundUserOperands.add(operand);
                }
            }
        }


        //Find the user values
        if (unboundUserOperands.size() > 0) {
            List userValues =
                dataContext.selectUserChoices(getProperty("usermsg", ""),
                    unboundUserOperands);
            if (userValues == null) {
                return null;
            }
            for (int i = 0; i < unboundUserOperands.size(); i++) {
                DataOperand operand =
                    (DataOperand) unboundUserOperands.get(i);
                Object value = userValues.get(i);
                operand.setData(value);
                UserDataChoice udc =
                    (UserDataChoice) unboundUserChoices.get(i);
                udc.setValue(value);
                if ( !childrenChoices.contains(udc)) {
                    childrenChoices.add(udc);
                }
            }
        }


        //Create the hashtable (if needed) that will hold the name->DataChoice
        //mapping for the user selected operands
        if (userSelectedChoices == null) {
            userSelectedChoices = new Hashtable();
        }




        //Go through any pre-defined operands (the DataChoice children).
        //These can be an UnboundDataChoice (from a need=*param_name in derived.xml),
        //a UserDataChoice (from a need="user:param_name" in derived.xml) or
        //a regular DataChoice.
        for (int i = 0; i < childrenChoices.size(); i++) {
            DataChoice dc     = (DataChoice) childrenChoices.get(i);
            String     alias  = "D" + (i + 1);
            String     opName = dc.getName();
            if (dc instanceof UnboundDataChoice) {
                //                System.err.println ("unbound choice");
                addOperand(opName, null, operands, operandsSoFar);
            } else if (dc instanceof UserDataChoice) {
                //                System.err.println ("UDC");
                UserDataChoice userChoice = (UserDataChoice) dc;
                addOperand(alias, userChoice.getValue(), operands,
                           operandsSoFar);
            } else {
                //Here, put the DataChoice in as the  data (sort of as a place holder for later).
                //                System.err.println ("addOperand ");
                addOperand(opName, dc, operands, operandsSoFar);
                if (userSelectedChoices.get(alias) == null) {
                    //                    System.err.println ("addOperand-alias " + alias);
                    addOperand(alias, dc, operands, operandsSoFar);
                }
            }
        }


        Hashtable dataChoiceToData = new Hashtable();

        //Go through the list of jython operands and put any that
        //we don't have in the list of DataOperands
        for (int i = 0; i < nonUserOperands.size(); i++) {
            DataOperand operand = (DataOperand) nonUserOperands.get(i);
            //            System.err.println ("addOperand-nonUser ");
            addOperand(operand.getName(), null, operands, operandsSoFar);
        }



        //Go through the list of DataOperand-s and see if any 
        //are already defined in the userSelectedChoices (i.e., from this
        //object being un-persisted)
        List unboundOperands = new ArrayList();
        //        System.err.println("Operands:" + operands);
        for (int i = 0; i < operands.size(); i++) {
            DataOperand op = (DataOperand) operands.get(i);
            if ( !op.isBound()) {
                DataChoice boundChoice =
                    (DataChoice) userSelectedChoices.get(op.getParamName());
                //For legacy bundles the operand may have been put into the
                //hashtable with the full name (including "[....]")
                if (boundChoice == null) {
                    boundChoice =
                        (DataChoice) userSelectedChoices.get(op.getName());
                }

                if (boundChoice != null) {
                    //Got it from before
                    //                    System.err.println("getData-1:" + boundChoice + " " + op);
                    Object data = boundChoice.getData(DataCategory.NULL,
                                      dataSelection, requestProperties);
                    dataChoiceToData.put(boundChoice, data);
                    op.setData(data);
                } else {
                    //Need to get this one
                    unboundOperands.add(op);
                }
            }
        }


        //If any of the operands we need are unbound 
        //than ask the DataContext to bind them
        if (unboundOperands.size() > 0) {
            List selected = dataContext.selectDataChoices(unboundOperands);
            if (selected == null) {
                return null;
            }
            for (int i = 0; i < unboundOperands.size(); i++) {
                DataOperand op = (DataOperand) unboundOperands.get(i);
                DataChoice  selectedChoice = (DataChoice) selected.get(i);
                //Add this data choice to the list of data choices
                childrenChoices.add(selectedChoice);
                userSelectedChoices.put(op.getParamName(), selectedChoice);
                op.setData(selectedChoice.getData(DataCategory.NULL,
                        dataSelection, requestProperties));
            }
        }

        //Now, do the getData calls on any DataOperand-s that hold a DataChoice.
        for (int i = 0; i < operands.size(); i++) {
            DataOperand op   = (DataOperand) operands.get(i);
            Object      data = op.getData();
            if (data instanceof DataChoice) {
                Object realData = dataChoiceToData.get(data);
                if (realData == null) {
                    //                    System.err.println("getData-3:" + data + " op= " + op);
                    realData = ((DataChoice) data).getData(DataCategory.NULL,
                            dataSelection, requestProperties);
                    if (realData == null) {
                        throw new BadDataException("Unable to get data: "
                                + ((DataChoice) data).getName());
                    }
                    dataChoiceToData.put(data, realData);
                }
                op.setData(realData);
            }
        }

        operands.addAll(allUserOperands);
        //Done, return the list of DataOperand-s
        return operands;
    }


    /**
     * Apply the derived operation. If any of the getData of the sub-dataChoices
     * returns null or if on a user select we get null back then this returns
     * null.
     *
     * @param category           Ignore this for now.
     * @param dataSelection      Allows one to subset the getData call
     *                              (e.g., selecting times).
     * @param requestProperties  extra request properties
     *
     * @return The {@link visad.Data} object that is the result of
     *         applying the operator held by this choice to its set of operands.
     *
     * @throws DataCancelException   if the request was canelled
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getData(DataCategory category,
                           DataSelection dataSelection,
                           Hashtable requestProperties)
            throws VisADException, RemoteException, DataCancelException {

        /**
         *  For now, don't do any caching here.
         * Object cacheKey = Misc.newList (category, dataSelection);
         * Data cachedData = (Data) CacheManager.get (this, cacheKey);
         * if (cachedData != null) {
         *   return cachedData;
         * }
         */
        if (requestProperties == null) {
            requestProperties = new Hashtable();
        }
        Object tmpProperty = requestProperties.get(PROP_FROMDERIVED);
        requestProperties.put(PROP_FROMDERIVED, new Boolean(true));

        Trace.call1("DerivedData.getData");
        //First, collect the DataOperand-s. This also sets the "constructedCode" member
        List ops = collectOperands(category, dataSelection,
                                   requestProperties);

        if (tmpProperty != null) {
            requestProperties.put(PROP_FROMDERIVED, tmpProperty);
        } else {
            requestProperties.remove(PROP_FROMDERIVED);
        }




        //Did the user cancel the selection of operands
        if (ops == null) {
            throw new DataCancelException();
        }

        Data result = null;

        //Get the interpreter, synchronize so we don't have conflicts with the
        //operand setting.

        PythonInterpreter interp =
            dataContext.getJythonManager().getDerivedDataInterpreter(
                methodName);
        synchronized (interp) {
            //Bind the operand name to the appropriate values.
            for (int i = 0; i < ops.size(); i++) {
                DataOperand op               = (DataOperand) ops.get(i);
                String      cleanOperandName = op.makeLegalJython();
                constructedCode = StringUtil.replace(constructedCode,
                        op.getName(), cleanOperandName);
                /*
                if(op.getData() instanceof String)
                    System.err.println ("op:" + cleanOperandName +"="+op.getData());
                else
                    System.err.println ("op:" + cleanOperandName +"=visad");
                */
                interp.set(cleanOperandName, op.getData());
            }
            //System.err.println ("Clean Code:" + constructedCode);


            //Check here because the hashCode/equals on this object
            //that the cache manager uses to do lookups
            //is set in the code above.
            /*
              for now don't do any caching
            result = (Data) CacheManager.get (this, cacheKey);
            */

            if (result == null) {
                //Sometime we may want to do an exec here, instead of an eval.
                //If we do the exec we need to have the contructed code
                //have a "result=" in it and then we retrieve the
                //value of "result" from the interpreter


                PyObject pyResult     = interp.eval(constructedCode);
                Object   resultObject = pyResult.__tojava__(visad.Data.class);
                //Make sure we got the right kind of return value
                if ((resultObject != null)
                        && !(resultObject instanceof Data)) {
                    throw new IllegalArgumentException(
                        "Unknown return value type:"
                        + resultObject.getClass().getName() + "\n Value="
                        + resultObject + "\nCode:" + constructedCode);
                }
                result = (Data) resultObject;
                //              System.err.println ("adding to cache:" + this);
                /*
                  for now don't do any caching
                  CacheManager.put (this, cacheKey, result);
                */

            } else {
                //              System.err.println ("had data in cache:" + this);
            }

            //Now, go thru each arg that we just set and clear it so we don't leak
            for (int i = 0; i < ops.size(); i++) {
                DataOperand op = (DataOperand) ops.get(i);
                interp.set(op.getName(), null);
            }
        }



        Trace.call2("DerivedData.getData");
        return result;
    }





    /**
     * Set the formula. Mostly used for  xml encoding.
     *
     * @param formula The new value.
     */
    public void setFormula(String formula) {
        this.formula = formula;
    }

    /**
     * Get the formula. Mostly used for  xml encoding.
     *
     * @return The formula.
     */
    public String getFormula() {
        return formula;
    }

    /**
     * Get the method name. Mostly used for  xml encoding.
     *
     * @return The method name.
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Set the method name. Mostly used for  xml encoding.
     *
     * @param methodName The new value.
     */
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }



    /**
     * Stub method to keep around so persistence won't flag a warning.
     *
     * @param m   class name
     */
    public void setClassName(String m) {}

    /**
     *  Get the code. Mostly used for  xml encoding.
     *
     *  @return The code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Set the code. Mostly used for  xml encoding.
     *
     * @param m The new value.
     */
    public void setCode(String m) {
        code = m;
    }

    /**
     * Get the DataContext. Mostly used for  xml encoding.
     *
     * @return The DataContext.
     */
    public DataContext getDataContext() {
        return dataContext;
    }


    /**
     * Set the DataContext. Mostly used for  xml encoding.
     *
     * @param c The new value.
     */
    public void setDataContext(DataContext c) {
        dataContext = c;
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
     * Set the user selected choices. Mostly used for  xml encoding.
     *
     * @param value   a hashtable of choices
     */
    public void setUserSelectedChoices(Hashtable value) {
        userSelectedChoices = value;
    }


    /**
     * Get the user selected choices. Mostly used for  xml encoding.
     *
     * @return The user selected choices.
     */
    public Hashtable getUserSelectedChoices() {
        return userSelectedChoices;
    }


    /**
     * Set the descriptor. Mostly used for  xml encoding.
     *
     * @param value  the new DerivedDataDescriptor
     */
    public void setDescriptor(DerivedDataDescriptor value) {
        descriptor = value;
    }

    /**
     * Get the DerivedDataDescriptor. Mostly used for  xml encoding.
     *
     * @return The DerivedDataDescriptor.
     */
    public DerivedDataDescriptor getDataDescriptor() {
        return descriptor;
    }


    /**
     *  Remove any objects created by this choice from the cache.
     */
    protected void flushCache() {
        CacheManager.remove(this);
    }

    /**
     *  Add the data change listener. This overwrites the base class method
     *  to tell each of the children to addDataChangeListener
     *  (i.e., the composite pattern).
     *
     *  @param listener The listener to add.
     */
    public void addDataChangeListener(DataChangeListener listener) {
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
        for (int i = 0; i < childrenChoices.size(); i++) {
            DataChoice child = (DataChoice) childrenChoices.get(i);
            if (child != null) {
                child.removeDataChangeListener(listener);
            }
        }
    }


    /**
     * Dummy for persistence.
     *
     * @param foo   extra args
     */
    public void setExtraArgs(String foo) {}

    /**
     *  Override the hasCode method.
     *
     *  @return The object's hashcode.
     */
    public int hashCode() {
        return super.hashCode() ^ Misc.hashcode(childrenChoices)
               ^ Misc.hashcode(properties)
               ^ Misc.hashcode(userSelectedChoices);
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
        if ( !(o instanceof DerivedDataChoice)) {
            return false;
        }
        DerivedDataChoice that = (DerivedDataChoice) o;
        return Misc.equals(methodName, that.methodName)
               && Misc.equals(formula, that.formula)
               && Misc.equals(code, that.code)
               && Misc.equals(userSelectedChoices, that.userSelectedChoices)
               && Misc.equals(properties, that.properties)
               && Misc.equals(childrenChoices, that.childrenChoices);
    }


    /**
     * Is this an (editable) end user formula.
     *
     * @return  true if no descriptor
     */
    public boolean isEndUserFormula() {
        return (descriptor != null);
    }


    /** hashtable of jython keywords */
    private static Hashtable jythonKeywords;

    /**
     * Parse out the operands in the given Jython code. Return a list of
     * {@link DataOperand}s
     *
     * @param jythonCode The code.
     *
     * @return List of operands.
     */
    public static List parseOperands(String jythonCode) {

        //Yank out all whitespace
        //        jythonCode = StringUtil.removeWhitespace(jythonCode);

        List         operands                = new ArrayList();
        StringBuffer current                 = null;
        Hashtable    seen                    = new Hashtable();
        char[]       chars                   = jythonCode.toCharArray();

        final int    STATE_LOOKINGFORTOKEN   = 0;
        final int    STATE_HAVEIDENTIFIER    = 1;
        final int    STATE_HAVENONIDENTIFIER = 2;
        final int    STATE_HAVEBRACKETEDID   = 3;
        final int    STATE_INDOUBLEQUOTE     = 4;
        final int    STATE_INSINGLEQUOTE     = 5;

        int          state                   = STATE_LOOKINGFORTOKEN;

        char         CHAR_DQUOTE             = '\"';
        char         CHAR_SQUOTE             = '\'';
        char         CHAR_HASH               = '#';
        String       extraSpaces             = null;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            switch (state) {

              case STATE_LOOKINGFORTOKEN :
                  if (c == CHAR_DQUOTE) {
                      state = STATE_INDOUBLEQUOTE;
                  } else if (c == CHAR_SQUOTE) {
                      state = STATE_INSINGLEQUOTE;
                  } else if (Character.isJavaIdentifierStart(c)
                             || (c == CHAR_HASH)) {
                      current = new StringBuffer();
                      current.append(c);
                      state = STATE_HAVEIDENTIFIER;
                  } else {}
                  break;

              case STATE_INDOUBLEQUOTE :
                  if (c == '\"') {
                      state = STATE_LOOKINGFORTOKEN;
                  }
                  break;

              case STATE_INSINGLEQUOTE :
                  if (c == '\'') {
                      state = STATE_LOOKINGFORTOKEN;
                  }
                  break;

              case STATE_HAVEBRACKETEDID :
                  current.append(c);
                  if (c == ']') {
                      addOperand(current.toString(), seen, operands);
                      state = STATE_LOOKINGFORTOKEN;
                  }
                  break;

              case STATE_HAVEIDENTIFIER :
                  if (Character.isJavaIdentifierPart(c) || (c == '.')
                          || (c == ':') || (c == '#')) {
                      //Inside of possible operand
                      current.append(c);
                  } else if (c == '[') {
                      //Do we have an operand with the special [...] properties
                      state = STATE_HAVEBRACKETEDID;
                      if (extraSpaces != null) {
                          current.append(extraSpaces);
                      }
                      extraSpaces = null;
                      current.append(c);
                  } else if ((c == ' ') || (c == '\t')) {
                      //Eat any white space
                      if (extraSpaces == null) {
                          extraSpaces = "";
                      }
                      extraSpaces += c;
                  } else {
                      //If it is not a proc name then it is an operand
                      if (c != '(') {
                          addOperand(current.toString(), seen, operands);
                      }
                      extraSpaces = null;
                      current     = null;
                      state       = STATE_LOOKINGFORTOKEN;
                  }
                  break;


              case STATE_HAVENONIDENTIFIER :
                  //Not used.
                  break;

            }
        }

        if ((state == STATE_HAVEIDENTIFIER) && (current != null)
                && (current.length() > 0)) {
            addOperand(current.toString(), seen, operands);
        }
        return operands;
    }


    /**
     *  Add the given operand op into the list of ops if it has not been
     *  placed into the seen table and if it is not one of the jython keywords.
     *
     *  @param op      The operand name.
     *  @param seen    Keeps track of what operands are in the list.
     *  @param ops     The list of operands.
     */
    private static void addOperand(String op, Hashtable seen, List ops) {
        if (jythonKeywords == null) {
            jythonKeywords = new Hashtable();
            jythonKeywords.put("if", "");
            jythonKeywords.put("def", "");
            jythonKeywords.put("for", "");
            jythonKeywords.put("while", "");
            //      jythonKeywords.put ("", "");
        }
        if (jythonKeywords.get(op) != null) {
            return;
        }
        if ( !Misc.haveSeen(op, seen)) {
            ops.add(new DataOperand(op));
        }

    }




    /**
     * Clean up any JythonCode that the user inputs
     *
     * @param code   code to clean up
     * @return  cleaned up code
     */
    public static String cleanupJythonCode(String code) {
        List ops = parseOperands(code);
        for (int i = 0; i < ops.size(); i++) {
            DataOperand operand = (DataOperand) ops.get(i);
            code = StringUtil.replace(code, operand.getName(),
                                      operand.makeLegalJython());
        }
        return code;
    }



    /**
     * Method for testing.
     *
     * @param args   operands
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("No args");
            System.exit(0);
        }
        System.err.println("ops:" + DerivedDataChoice.parseOperands(args[0]));
        System.err.println("code:" + cleanupJythonCode(args[0]));
    }




}

