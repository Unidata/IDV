/*
 * $Id: DerivedNeed.java,v 1.22 2007/06/14 20:37:12 jeffmc Exp $
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


import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.NamedList;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import visad.Data;
import visad.VisADException;

import java.io.InputStream;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;





/**
 * This class holds the specification of the "<needs>" tag in the
 * derived.xml file.
 *
 * @author IDV Development Team
 * @version $Revision: 1.22 $
 */
public class DerivedNeed {

    /**
     *  A list of lists of parameter names (String)
     */
    private List paramSets = new ArrayList();

    /** hashtable of used needs */
    private Hashtable usedThemBefore;

    /** the categories */
    protected String categories;

    /** My categories */
    private List categoryList;


    /** the group name */
    private String groupName;

    /** the descriptor I am part of */
    private DerivedDataDescriptor ddd;



    /**
     * ctor
     *
     * @param ddd my ddd
     * @param categories categories string
     */
    public DerivedNeed(DerivedDataDescriptor ddd, String categories) {
        this.ddd        = ddd;
        this.categories = categories;
    }

    /**
     * This is the constructor that takes a parameter group  name.
     *
     *
     * @param ddd my ddd
     * @param groupName         the {@link DataGroup} name
     * @param categories        other categories
     */
    public DerivedNeed(DerivedDataDescriptor ddd, String groupName,
                       String categories) {
        this(ddd, categories);
        this.groupName = groupName;
        reInitialize();
    }



    /**
     * This is the constructor that takes a set of parameters.
     *
     *
     * @param ddd my ddd
     * @param params            parameters
     * @param categories        other categories
     */
    public DerivedNeed(DerivedDataDescriptor ddd, List params,
                       String categories) {
        this(ddd, categories);
        paramSets = Misc.newList(params);
    }


    /**
     * If we use DataGroups then reinitialize the paramSets list
     */
    protected void reInitialize() {
        if (groupName != null) {
            DataGroup group = DataGroup.getDataGroup(groupName);
            if (group != null) {
                paramSets = group.getParamSets();
            } else {
                paramSets = new ArrayList();
            }
        }
    }



    /**
     * Get the group name for this.
     *
     * @return   the group name (or null)
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Get the parameter sets
     *
     * @return  parameter sets
     */
    public List getParamSets() {
        return paramSets;
    }

    /**
     * See if the Object in question is equal to this.
     *
     * @param other  Object in question
     * @return  true if they are equal
     */
    public boolean equals(Object other) {
        if ( !(other instanceof DerivedNeed)) {
            return false;
        }
        DerivedNeed that = (DerivedNeed) other;
        return paramSets.equals(that.paramSets)
               && Misc.equals(groupName, that.groupName)
               && Misc.equals(categories, that.categories);
    }

    /**
     * Get the list of DataChoices that are needed for this.
     *
     * @param choicesSoFar   choices seen so far
     * @return  List of needed DataChoices
     */
    public List getDataChoices(Hashtable choicesSoFar) {

        //        System.err.println ("getDataChoices");
        for (int i = 0; i < paramSets.size(); i++) {
            List[] result = getDataChoicesFromParams(choicesSoFar,
                                (List) paramSets.get(i));
            if (result != null) {
                List dataChoices = result[0];
                List paramNames  = result[1];
                for (int dataChoiceIdx = 0;
                        dataChoiceIdx < dataChoices.size(); dataChoiceIdx++) {
                    DataChoice dataChoice =
                        (DataChoice) dataChoices.get(dataChoiceIdx);
                    Hashtable usedChoices =
                        (Hashtable) usedThemBefore.get(dataChoiceIdx + "");
                    if (usedChoices == null) {
                        usedChoices = new Hashtable();
                        usedThemBefore.put(dataChoiceIdx + "", usedChoices);
                    }
                    usedChoices.put(dataChoice, dataChoice);
                }
                return dataChoices;
            }
        }
        return null;
    }

    /**
     * Initialize for searching.  Clear out the seen params hashtable.
     */
    public void initForSearch() {
        usedThemBefore = new Hashtable();
    }

    /**
     * Clear out the hashtable of used parameters after searching
     */
    public void clearAfterSearch() {
        usedThemBefore = null;
    }


    /**
     * Get the lists of DataChoices from the parameter names
     *
     * @param choicesSoFar   choices already selected
     * @param params         list of parameters
     * @return  Lists of DataChoices
     */
    private List[] getDataChoicesFromParams(Hashtable choicesSoFar,
                                            List params) {
        boolean allMatched  = true;
        List    dataChoices = null;
        List    paramNames  = null;
        //        System.err.println ("\tfrom Params:" + params);
        for (int paramIdx = 0; paramIdx < params.size(); paramIdx++) {
            String param = (String) params.get(paramIdx);
            DataChoice dataChoice = getDataChoice(param, choicesSoFar,
                                        paramIdx);
            if (dataChoice == null) {
                allMatched = false;
                break;
            } else {
                if (dataChoices == null) {
                    dataChoices = new ArrayList();
                    paramNames  = new ArrayList();
                }
                //System.err.println ("\tGot choice:" + dataChoice);
                dataChoices.add(dataChoice);
                paramNames.add(param);
            }
        }
        if ( !allMatched) {
            return null;
        }
        return new List[] { dataChoices, paramNames };
    }

    /**
     * Get the DataChoice for the parameter
     *
     * @param paramName      name of parameter
     * @param choicesSoFar   choices already seend
     * @param paramIdx       parameter index
     * @return  the associated DataChoice
     */
    private DataChoice getDataChoice(String paramName,
                                     Hashtable choicesSoFar, int paramIdx) {
        if (paramName.startsWith("*")) {
            //      System.err.println ("Unbound:" + paramName);
            return new UnboundDataChoice(paramName.substring(1));
        } else if (paramName.startsWith("user:")) {
            //      System.err.println ("User data choice:" + paramName);
            return new UserDataChoice(paramName.substring(5).trim());
        } else {
            DataChoice choice = findDataChoice(paramName, choicesSoFar,
                                    paramIdx);
            if (choice != null) {
                return choice;
            }
            List aliasList = DataAlias.getAliasesOf(paramName);
            if (aliasList == null) {
                return null;
            }
            for (int i = 0; i < aliasList.size(); i++) {
                choice = findDataChoice((String) aliasList.get(i),
                                        choicesSoFar, paramIdx);
                if (choice != null) {
                    //              System.err.println ("Regular:" + paramName);
                    return choice;
                }
            }
        }
        return null;
    }


    /**
     * Find and return the DataChoice (possibly) contained in the given
     * Hashtable choicesSoFar. Make sure that we have not used this DataChoice
     * before for this particular paramName/paramIndex pair.
     *
     * @param paramName      parameter name
     * @param choicesSoFar   choices seen so far
     * @param paramIdx       parameter index
     * @return  the DataChoice or null
     */
    private DataChoice findDataChoice(String paramName,
                                      Hashtable choicesSoFar, int paramIdx) {
        DataChoice dataChoice = null;
        String     key        = paramIdx + "";

        //Look at all of the data choices created so far with the given  parameter name
        List choiceList = (List) choicesSoFar.get(paramName);


        //        System.err.println ("\t\tfindDataChoice " + paramName);
        if (choiceList == null) {
            if (StringUtil.containsRegExp(paramName)) {
                //                System.err.println ("\t\tisregexp");
                for (Enumeration keys = choicesSoFar.keys();
                        keys.hasMoreElements(); ) {
                    String tmp = (String) keys.nextElement();
                    //                    System.err.println ("\t\t\t" + tmp);
                    if (StringUtil.regexpMatch(tmp, paramName)) {
                        choiceList = (List) choicesSoFar.get(tmp);
                        //                        System.err.println ("Got it from a regexp:" + paramName + " " + choiceList);
                        break;
                    }
                }

            }
        }


        //Don't have any, return null
        if (choiceList == null) {
            return null;
        }

        for (int i = 0; i < choiceList.size(); i++) {
            dataChoice = (DataChoice) choiceList.get(i);
            //These are the ones we have seen before
            Hashtable usedChoices = (Hashtable) usedThemBefore.get(key);
            if ((usedChoices != null)
                    && (usedChoices.get(dataChoice) != null)) {
                //We've used it before (for this parameter). Let's skip it.
                continue;
            }
            List cats = getCategoryList();
            if ((cats != null) && (cats.size() > 0)) {
                if ( !DataCategory.applicableTo(cats,
                        dataChoice.getCategories())) {
                    continue;
                }
            }
            return dataChoice;
        }
        return null;
    }


    /**
     * Set the Categories property.
     *
     * @param value The new value for Categories
     */
    public void setCategories(String value) {
        categories   = value;
        categoryList = null;
    }

    /**
     * Get the Categories property.
     *
     * @return The Categories
     */
    public String getCategories() {
        return categories;
    }





    /**
     * Get the list of data categories to use
     *
     * @return list of data categories
     */
    private List getCategoryList() {
        if ((categoryList == null) && (categories != null)) {
            categoryList = DataCategory.parseCategories(categories);
        }
        if (categoryList != null) {
            return categoryList;
        }
        return ddd.getOperandsCategoryList();
    }


}

