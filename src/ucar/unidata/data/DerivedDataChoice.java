/**
 * Copyright 1997-2009 Unidata Program Center/University Corporation for
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
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;


import visad.*;



import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 * A subclass of DataChoice for derived quantities.
 *
 * @author IDV Development Team
 * @version $Revision: 1.118 $
 */
public class DerivedDataChoice extends ListDataChoice {

    /**
     * Property indicating this is a derived quantity
     */
    public static String PROP_FROMDERIVED = "fromderived";


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
    private Hashtable userSelectedChoices = new Hashtable();


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
     *  Dummy param-less constructor so we can be recreated thru reflection.
     */
    public DerivedDataChoice() {}


    /**
     * The copy ctor.
     *
     * @param that  The object to instantiate from.
     */
    public DerivedDataChoice(DerivedDataChoice that) {
        super(that);
        this.dataContext = that.dataContext;
        this.methodName  = that.methodName;
        this.code        = that.code;
        this.formula     = that.formula;
        if (that.userSelectedChoices != null) {
            Enumeration keys = that.userSelectedChoices.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                DataChoice dataChoice =
                    (DataChoice) that.userSelectedChoices.get(key);
                int idx = childrenChoices.indexOf(dataChoice);
                if (idx >= 0) {
                    //                    System.err.println ("Found find the cloned data choice");
                    userSelectedChoices.put(key, childrenChoices.get(idx));
                } else {
                    System.err.println(
                        "Couldn't find the cloned data choice");
                    userSelectedChoices.put(key, dataChoice);
                }
            }

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
        descriptor       = ddd;
        this.dataContext = dataContext;
        this.properties  = ddd.properties;
        this.methodName  = ddd.method;
        this.code        = ddd.code;
        this.formula     = ddd.formula;
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
     * Used when not creating it from a DerivedDataDescriptor.
     *
     * @param dataContext        The context in which this DataChoice exists
     *                           (typically the
     *                           {@link ucar.unidata.idv.IntegratedDataViewer}).
     *  @param formula           The formula body used as the operator
     */
    public DerivedDataChoice(DataContext dataContext, String formula) {
        super("", "", "", new ArrayList());
        this.dataContext = dataContext;
        this.properties  = new Properties();
        this.formula     = formula;
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
     * Get the full Description for this DataChoice.
     *
     * @return  full description
     */
    public String getFullDescription() {
        String extra = "";
        if (formula != null) {
            extra = extra + "<br>Formula: <i>" + formula + "</i><br>";
        }
        StringBuffer sb = new StringBuffer("Derived quantity: " + extra
                                           + super.getFullDescription());
        return sb.toString();
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
        DataOperand dataOperand = (DataOperand) opsSoFar.get(opName);
        if (dataOperand != null) {
            if ( !dataOperand.isBound()) {
                dataOperand.setData(data);
            }
            return dataOperand;
        }
        dataOperand = new DataOperand(opName, data);
        opsSoFar.put(opName, dataOperand);
        operands.add(dataOperand);
        return dataOperand;
    }



    /**
     *  Add the given operand op into the list of ops if it has not been
     *  placed into the seen table and if it is not one of the jython keywords.
     *
     *  @param operand      The operand name.
     *  @param seen    Keeps track of what operands are in the list.
     *  @param ops     The list of operands.
     */
    private static void addOperand(String operand, Hashtable seen, List ops) {
        if (jythonKeywords == null) {
            jythonKeywords = new Hashtable();
            jythonKeywords.put("if", "");
            jythonKeywords.put("def", "");
            jythonKeywords.put("for", "");
            jythonKeywords.put("while", "");
            //      jythonKeywords.put ("", "");
        }
        if (jythonKeywords.get(operand) != null) {
            return;
        }
        DataOperand dataOperand = (DataOperand) seen.get(operand);
        if (dataOperand != null) {
            return;
        }
        //        Misc.printStack("addOperand - 1:" + operand + ":", 5, null);
        dataOperand = new DataOperand(operand);
        seen.put(operand, dataOperand);
        ops.add(dataOperand);
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
        DerivedDataChoice.parseOperands(constructedCode, operands,
                                        operandsSoFar);


        //Now, split the list between user and non-user operands
        //Make sure all user operands are in the data choices list
        List nonUserOperands = new ArrayList();
        List tmpList         = new ArrayList(operands);
        for (int i = 0; i < tmpList.size(); i++) {
            DataOperand operand = (DataOperand) tmpList.get(i);
            if (operand.isUser()) {
                UserDataChoice udc = new UserDataChoice(operand.getName(),
                                         operand.getUserDefault());

                if ( !childrenChoices.contains(udc)) {
                    childrenChoices.add(udc);
                }
                operands.remove(operand);
            } else {
                nonUserOperands.add(operand);
            }
        }

        /*
           System.err.println("formula:" +constructedCode);
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
                UserOperandValue userOperandValue =
                    (UserOperandValue) userValues.get(i);
                Object value = userOperandValue.getValue();
                operand.setData(value);
                UserDataChoice udc =
                    (UserDataChoice) unboundUserChoices.get(i);
                udc.persistent = userOperandValue.getPersistent();
                udc.setValue(value);
                if ( !childrenChoices.contains(udc)) {
                    childrenChoices.add(udc);
                }
            }
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
                if ( !userChoice.persistent) {
                    userChoice.setValue(null);
                }
            } else {
                //Here, put the DataChoice in as the  data (sort of as a place holder for later).
                //                System.err.println ("addOperand " + opName + " alias = " + alias );
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
            if (op.isBound()) {
                continue;
            }
            DataChoice boundChoice =
                (DataChoice) userSelectedChoices.get(op.getParamName());
            //For legacy bundles the operand may have been put into the
            //hashtable with the full name (including "[....]")
            if (boundChoice == null) {
                boundChoice =
                    (DataChoice) userSelectedChoices.get(op.getName());
            }
            if (boundChoice == null) {
                unboundOperands.add(op);
                continue;
            }
            setData(boundChoice, op, dataChoiceToData, dataSelection,
                    requestProperties);
        }


        //        System.err.println("operands:" + operands);

        //If any of the operands we need are unbound 
        //than ask the DataContext to bind them
        if (unboundOperands.size() > 0) {
            //            System.err.println("unbound operands:" + unboundOperands);
            List selected = dataContext.selectDataChoices(unboundOperands);
            if (selected == null) {
                return null;
            } else {  //yuanho added time
                DataChoice dc0 = (DataChoice)selected.get(0);
                DataSelection ds0 = dc0.getDataSelection();
                if(ds0 != null) {
                    Object ud = ds0.getProperty(DataSelection.PROP_USESTIMEDRIVER);
                    if(ud != null && ((Boolean)ud).booleanValue()){
                        dataSelection.putProperty(DataSelection.PROP_USESTIMEDRIVER, true);
                    }
                }
            }
            for (int i = 0; i < unboundOperands.size(); i++) {
                DataOperand op = (DataOperand) unboundOperands.get(i);
                DataChoice  selectedChoice = (DataChoice) selected.get(i);
                //Add this data choice to the list of data choices
                childrenChoices.add(selectedChoice);
                addDataChangeListeners(selectedChoice);

                userSelectedChoices.put(op.getParamName(), selectedChoice);
                //Do an .equals instead of an instanceof because DerivedDataChoice 
                //derived from ListDataChoice
                setData(selectedChoice, op, dataChoiceToData, dataSelection,
                        requestProperties);
            }
        }

        //Now, do the getData calls on any DataOperand-s that hold a DataChoice.
        for (int i = 0; i < operands.size(); i++) {
            DataOperand op   = (DataOperand) operands.get(i);
            Object      data = op.getData();
            if (data instanceof DataChoice) {
                DataChoice dataChoice = (DataChoice) data;
                setData(dataChoice, op, dataChoiceToData, dataSelection,
                        requestProperties);
            }
        }

        operands.addAll(allUserOperands);
        //Done, return the list of DataOperand-s
        return operands;
    }


    /**
     * get the data from the dataChoice and set it on the operand
     *
     * @param dataChoice the data choice
     * @param dataOperand the operand
     * @param dataChoiceToData map of previously created data
     * @param dataSelection properties
     * @param requestProperties properties
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void setData(DataChoice dataChoice, DataOperand dataOperand,
                         Hashtable dataChoiceToData,
                         DataSelection dataSelection,
                         Hashtable requestProperties)
            throws VisADException, RemoteException {
        //System.out.println("getting data for " + dataChoice.getName() + "," + dataOperand.getName());
        Object data = dataChoiceToData.get(dataOperand.getName());
        if (data == null) {
            if (dataChoice.getClass().equals(ListDataChoice.class)) {
                ListDataChoice ldc = (ListDataChoice) dataChoice;
                data = ldc.getDataList(DataCategory.NULL, dataSelection,
                                       requestProperties);
            } else {
                //                System.err.println("Calling checkLevel");
                checkLevel(dataChoice, dataOperand, dataSelection);
                data = dataChoice.getData(DataCategory.NULL, dataSelection,
                                          requestProperties);
            }
            dataChoiceToData.put(dataOperand.getName(), data);
        }
        dataOperand.setData(data);
        //        return data;
    }



    /**
     * if the operand has a level property then set the level on the
     * uber DataSelection
     *
     * @param dataChoice the data choice
     * @param op the operand
     * @param incomingSelection the incoming data selection
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void checkLevel(DataChoice dataChoice, DataOperand op,
                            DataSelection incomingSelection)
            throws VisADException, RemoteException {
        DataSelection dataSelection = dataChoice.getDataSelection();
        // System.err.println (op +" LEVEL:" + op.getProperty("level"));

        // System.err.println("checkLevel : " + dataChoice + " " + dataSelection + "  prop:" + op + " " + op.getProperty("level"));

        if (op.getProperty("level") != null) {
            //&& ((dataSelection == null)
            //    || (dataSelection.getFromLevel() == null))) {
            List levels = StringUtil.split(op.getProperty("level"), ":",
                                           true, true);
            Object fromLevel = null,
                   toLevel   = null;
            if (levels.size() == 0) {
                throw new IllegalArgumentException("Incorrect levels format:"
                        + op.getProperty("level") + "  for operand:" + op);
            }
            try {
                fromLevel = ucar.visad.Util.toReal(levels.get(0).toString(),
                        "(", ")");
                if (levels.size() >= 2) {
                    toLevel =
                        ucar.visad.Util.toReal(levels.get(1).toString(), "(",
                            ")");
                } else {
                    toLevel = fromLevel;
                }
                // System.err.println ("setting levels data selection " + fromLevel+ " to " + toLevel);
                incomingSelection.setLevelRange(toLevel, fromLevel);
                //dataChoice.setDataSelection(new DataSelection(fromLevel,
                //        toLevel));
            } catch (Exception exc) {
                throw new VisADException("Error parsing levels:" + exc);
            }
        }
    }

    /**
     * Apply the derived operation. If any of the getData of the sub-dataChoices
     * returns null or if on a user select we get null back then this returns
     * null.
     *
     * @param category           Ignore this for now.
     * @param incomingDataSelection      Allows one to subset the getData call
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
                           DataSelection incomingDataSelection,
                           Hashtable requestProperties)
            throws VisADException, RemoteException, DataCancelException {

        DataSelection dataSelection =
            DataSelection.merge(incomingDataSelection, myDataSelection);


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
            List<String> setVariables = new ArrayList<String>();
            try {
                for (int i = 0; i < ops.size(); i++) {
                    DataOperand op               = (DataOperand) ops.get(i);
                    String      cleanOperandName = op.makeLegalJython();
                    constructedCode = StringUtil.replace(constructedCode,
                            op.getName(), cleanOperandName);
                    setVariables.add(cleanOperandName);
                    interp.set(cleanOperandName, op.getData());
                }

                //Check here because the hashCode/equals on this object
                //that the cache manager uses to do lookups
                //is set in the code above.
                /*
                  for now don't do any caching
                  result = (Data) CacheManager.get (this, cacheKey);
                */

                interp.set("derivedDataChoice", this);
                if (result == null) {
                    //Sometime we may want to do an exec here, instead of an eval.
                    //If we do the exec we need to have the contructed code
                    //have a "result=" in it and then we retrieve the
                    //value of "result" from the interpreter

                    PyObject pyResult = interp.eval(constructedCode);

                    Object resultObject =
                        pyResult.__tojava__(visad.Data.class);
                    //Make sure we got the right kind of return value
                    if ((resultObject != null)
                            && !(resultObject instanceof Data)) {
                        resultObject = pyResult.__tojava__(DataChoice.class);
                        //If we get back a data choice then we reset our expression to be
                        //"bounddatachoice" and we add the data choice to our selves
                        if (resultObject instanceof DataChoice) {
                            DataChoice dataChoice = (DataChoice) resultObject;
                            result =
                                dataChoice.getData(incomingDataSelection);
                            code                = "bounddatachoice";
                            userSelectedChoices = new Hashtable();
                            userSelectedChoices.put(code, dataChoice);
                            childrenChoices = new ArrayList();
                            childrenChoices.add(dataChoice);
                        } else {
                            throw new IllegalArgumentException(
                                "Unknown return value type:"
                                + resultObject.getClass().getName()
                                + "\n Value=" + resultObject + "\nCode:"
                                + constructedCode);
                        }
                    } else {
                        result = (Data) resultObject;
                    }
                    //              System.err.println ("adding to cache:" + this);
                    /*
                      for now don't do any caching
                      CacheManager.put (this, cacheKey, result);
                    */

                } else {
                    //              System.err.println ("had data in cache:" + this);
                }

            } finally {
                //Now, go thru each arg that we just set and clear it so we don't leak
                try {
                    interp.set("derivedDataChoice", null);
                } catch (Exception ignore) {}
                try {
                    for (String varName : setVariables) {
                        interp.set(varName, null);
                    }
                } catch (Exception ignore) {}
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
        return super.hashCode() ^ Misc.hashcode(userSelectedChoices);
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
               && Misc.equals(userSelectedChoices, that.userSelectedChoices);
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
        Hashtable seen     = new Hashtable();
        List      operands = new ArrayList();
        return parseOperands(jythonCode, operands, seen);
    }



    /**
     * parse the operands in the given jython code
     *
     * @param jythonCode jython code to parse
     * @param operands ???
     * @param seen operands we have seen so far
     *
     * @return the operands
     */
    public static List parseOperands(String jythonCode, List operands,
                                     Hashtable seen) {

        //Yank out all whitespace
        //        jythonCode = StringUtil.removeWhitespace(jythonCode);

        StringBuffer current                 = null;
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
                  } else if (c == '=') {
                      //This is a jython parameter value
                      //Eat up the value of the parameter
                      for (; i < chars.length; i++) {
                          c = chars[i];
                          if ((c == ',') || (c == ' ') || (c == ')')) {
                              break;
                          }
                      }
                      extraSpaces = null;
                      current     = null;
                      state       = STATE_LOOKINGFORTOKEN;
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

