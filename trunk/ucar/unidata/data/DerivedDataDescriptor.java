/*
 * $Id: DerivedDataDescriptor.java,v 1.77 2007/06/15 13:03:16 jeffmc Exp $
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


import org.w3c.dom.Document;



import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.NamedList;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlResourceCollection;
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
 * This class is used to hold the descriptions of possible
 * {@link DerivedDataChoice}-s that are specified in the derived.xml
 * resource file.
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.77 $
 */

public class DerivedDataDescriptor {

    /** NULL string identifier */
    public static final String NULL_STRING = null;

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            DerivedDataDescriptor.class.getName());


    /**
     * A list of String names of full class names taken from the derived.xml
     * &lt;class&gt; tags. Used to init the jython interpreter
     */
    public static List classes = new ArrayList();


    /** The XML tag for categories */
    private static final String TAG_CATEGORIES = "categories";

    /** The XML tag for categories */
    private static final String TAG_CATEGORY = "category";

    /** The XML tag for class */
    private static final String TAG_CLASS = "class";

    /** The XML tag for derived */
    private static final String TAG_DERIVED = "derived";

    /** The XML tag for derived parameters */
    private static final String TAG_DERIVEDTOP = "derived_parameters";

    /** The XML tag for operands */
    private static final String TAG_OPERANDS = "operands";

    /** The XML tag for parameters */
    private static final String TAG_PARAMS = "params";

    /** The XML tag for a paramgroup */
    private static final String TAG_PARAMGROUP = "paramgroup";

    /** The XML tag for properties */
    private static final String TAG_PROPERTIES = "properties";

    /** The XML attribute for append */
    private static final String ATTR_APPEND = "append";

    /** The XML attribute for replace */
    private static final String ATTR_REPLACE = "replace";

    /** The XML attribute for category index */
    private static final String ATTR_CATEGORYIDX = "categoryidx";

    /** The XML attribute for categories */
    private static final String ATTR_CATEGORIES = "categories";

    /** The XML attribute for child index */
    private static final String ATTR_CHILDIDX = "childidx";

    /** The XML attribute for code */
    private static final String ATTR_CODE = "code";

    /** The XML attribute for description */
    private static final String ATTR_DESCRIPTION = "description";

    /** The XML attribute for formula */
    private static final String ATTR_FORMULA = "formula";

    /** The XML attribute for id */
    private static final String ATTR_ID = "id";

    /** The XML attribute for inherit */
    private static final String ATTR_INHERIT = "inherit";

    /** The XML attribute for a default formula */
    private static final String ATTR_ISDEFAULT = "isdefault";

    /** The XML attribute for end user formula identification */
    private static final String ATTR_ISENDUSER = "isenduser";

    /** The XML attribute for label */
    private static final String ATTR_LABEL = "label";

    /** The XML attribute for method */
    private static final String ATTR_METHOD = "method";

    /** The XML attribute for name */
    private static final String ATTR_NAME = "name";

    /** The XML attribute for parameter prefix */
    private static final String ATTR_PARAMPREFIX = "p";

    /** The XML attribute for value */
    private static final String ATTR_VALUE = "value";

    /** The XML attribute for display */
    private static final String ATTR_DISPLAY = "display";



    /**
     *  A list of {@link DerivedNeed} objects, that define what
     *  kinds of parameters are needed for the {@link DerivedDataChoice}
     *  described by this descriptor to be created.
     */
    private List myNeeds = new ArrayList();

    /**
     *  This is the category for the operands
     */
    private String operandsCategories;

    /** categories for operands */
    private List operandsCategoryList;

    /**
     *  This is the  identifier used for the created {@link DerivedDataChoice}
     *  (e.g., skewt, windvector, etc.)
     */
    String id;

    /**
     *  Human readable description
     */
    String description;

    /** A label for widgets */
    String label;

    /**
     *  One of the 3 ways of specifying an operation.
     *  The java  method specified (really ClassName.methodName).
     *  My be null.
     */
    String method;

    /**
     *  One of the 3 ways of specifying an operation.
     *  The visad formula. May be null.
     */
    String formula;

    /**
     *  One of the 3 ways of specifying an operation.
     *  jython code. May be null.
     */
    String code;

    /**
     *  The list of {@link DataCategory}-s that the {@link DerivedDataChoice}
     *  is created with.
     */

    ArrayList categories;

    /**
     *  Doesn't seem to be used for now
     */

    Properties properties;

    /** Is this DDD one of the ones te user has edited themselves */
    private boolean isLocalUsers = false;

    /**
     *  Is this a descriptor for an end-user formula
     */
    private boolean isEndUser = false;

    /**
     *  Is this descriptor used for creating default derived quantities
     */
    private boolean isDefault = false;


    /**
     *  We have this around for when we create a  DerivedDataChoice from this
     *  descriptor.
     */
    private DataContext dataContext;


    /**
     *  Dummy constructor for now
     */
    public DerivedDataDescriptor() {}


    /**
     * ctor
     *
     * @param dataContext   context for this descriptor
     * @param id The id
     * @param desc description
     * @param formula formula
     * @param categories List of data categories
     */
    public DerivedDataDescriptor(DataContext dataContext, String id,
                                 String desc, String formula,
                                 List categories) {
        this.dataContext = dataContext;
        this.categories  = ((categories != null)
                            ? new ArrayList(categories)
                            : new ArrayList());
        this.id          = id;
        this.description = desc;
        this.label       = desc;
        this.formula     = formula;
        isEndUser        = true;
    }


    /**
     * Constructor used for when we create an end-user formula
     *
     * @param dataContext   context for this descriptor
     */
    public DerivedDataDescriptor(DataContext dataContext) {
        this.dataContext = dataContext;
        this.categories  = new ArrayList();
    }

    /**
     * This is a private constructor used internally when creating a DDD
     * from the derived.xml node.
     *
     * @param dataContext     The DataContext that this DDD exists in
     *                        (Really, just the idv)
     * @param derivedNode     The "<derived>" tag xml node
     */
    private DerivedDataDescriptor(DataContext dataContext,
                                  Element derivedNode) {

        //Set some basic attributes.
        this.dataContext = dataContext;
        id               = XmlUtil.getAttribute(derivedNode, ATTR_ID);
        description = XmlUtil.getAttribute(derivedNode, ATTR_DESCRIPTION, "");
        label = XmlUtil.getAttribute(derivedNode, ATTR_LABEL, NULL_STRING);
        method = XmlUtil.getAttribute(derivedNode, ATTR_METHOD, NULL_STRING);
        code = XmlUtil.getAttribute(derivedNode, ATTR_CODE, NULL_STRING);
        formula = XmlUtil.getAttribute(derivedNode, ATTR_FORMULA,
                                       NULL_STRING);



        Element operandsNode = XmlUtil.findChild(derivedNode, TAG_OPERANDS);
        if (operandsNode != null) {
            processOperands(operandsNode);
        }


        categories = new ArrayList();
        Element categoriesNode = XmlUtil.findChild(derivedNode,
                                     TAG_CATEGORIES);
        if (categoriesNode != null) {
            List categoryNodes = XmlUtil.findChildren(categoriesNode,
                                     TAG_CATEGORY);
            for (int i = 0; i < categoryNodes.size(); i++) {
                Element child = (Element) categoryNodes.get(i);
                DataCategory dataCategory =
                    DataCategory.parseCategory(XmlUtil.getAttribute(child,
                        ATTR_VALUE), XmlUtil.getAttribute(child,
                            ATTR_DISPLAY, false));
                int childIdx = XmlUtil.getAttribute(child, ATTR_CHILDIDX, -1);
                int categoryIdx = XmlUtil.getAttribute(child,
                                      ATTR_CATEGORYIDX, -1);
                String append = XmlUtil.getAttribute(child, ATTR_APPEND,
                                    NULL_STRING);
                String replace = XmlUtil.getAttribute(child, ATTR_REPLACE,
                                    NULL_STRING);
                dataCategory.setChildIndex(childIdx);
                dataCategory.setCategoryIndex(categoryIdx);
                dataCategory.setAppend(append);
                dataCategory.setReplace(replace);
                categories.add(dataCategory);
            }
        }

        //Right now we don't use properties but we may sometime
        properties = Misc.parseProperties(XmlUtil.getAttribute(derivedNode,
                TAG_PROPERTIES, NULL_STRING));

        //We must have either a method, formula or code
        if ((method == null) && (formula == null) && (code == null)) {
            log_.error(
                "processDerivedDataChoice: Unable to find method, formula or code for:"
                + id);
        }

        //And a few more properties

        //the endUser property defines whether this is an end-user formula
        setIsEndUser(XmlUtil.getAttribute(derivedNode, ATTR_ISENDUSER,
                                          false));

        //Is default says whether this DDD is used to create default derived quantities
        //for data sources.
        setIsDefault(XmlUtil.getAttribute(derivedNode, ATTR_ISDEFAULT, true));
    }



    /**
     * Constructor for the descriptor. Pass in the operands, id, etc.
     *
     * @param needs         list of operands needed
     * @param id              identifier
     * @param description     long name
     * @param categories      list of categories
     * @param method          method (may be null)
     * @param formula         formula (may be null)
     * @param code            jython code (may be null)
     * @param properties      extra properties
     */

    public DerivedDataDescriptor(ArrayList needs, String id,
                                 String description, ArrayList categories,
                                 String method, String formula, String code,
                                 Properties properties) {
        setNeeds(needs);
        this.id          = id;
        this.description = description;
        this.categories  = categories;
        this.method      = method;
        this.formula     = formula;
        this.code        = code;
        this.properties  = properties;
    }

    /** some buffer */
    StringBuffer sb = new StringBuffer();

    /**
     * Copy constructor.  Effectively clones this.
     *
     * @param other  other instance to copy.
     */
    public DerivedDataDescriptor(DerivedDataDescriptor other) {
        this.isEndUser    = other.isEndUser;
        this.isLocalUsers = other.isLocalUsers;
        this.isDefault    = other.isDefault;
        this.dataContext  = other.dataContext;
        setNeeds(other.myNeeds);
        this.id          = other.id;
        this.description = other.description;
        this.categories  = (ArrayList) other.categories.clone();
        this.method      = other.method;
        this.formula     = other.formula;
        this.code        = other.code;
        this.properties  = (other.properties == null)
                           ? null
                           : (Properties) other.properties.clone();
    }

    /**
     * get categories for operands
     *
     * @return categories for operands
     */
    protected List getOperandsCategoryList() {
        return operandsCategoryList;
    }

    /**
     * This method runs through the children of the given "&lt;operands&gt;"
     * node  and adds to the myNeeds list  a set of DerivedNeed objects,
     * one for "&lt;paramgroup&gt;" or "&lt;params&gt;" node.
     *
     * @param operandsNode   node specifying operands
     */
    private void processOperands(Element operandsNode) {
        NodeList children = operandsNode.getChildNodes();

        //The operands node can have a data category that is used
        //to determine what data choices are applicable for the derived needs
        operandsCategories = XmlUtil.getAttribute(operandsNode,
                ATTR_CATEGORIES, NULL_STRING);


        if (operandsCategories != null) {
            if (operandsCategories.trim().length() == 0) {
                operandsCategories = null;
            } else {
                operandsCategoryList =
                    DataCategory.parseCategories(operandsCategories);
            }
        }

        //Run through the children of the operands node
        for (int operandsIdx = 0; operandsIdx < children.getLength();
                operandsIdx++) {
            Node child = children.item(operandsIdx);

            //Likewise, the child node may also have data categories associate with it
            //to refine what data choices can match this need.
            String needCategory = XmlUtil.getAttribute(child,
                                      ATTR_CATEGORIES, "");
            if (needCategory.trim().length() == 0) {
                needCategory = null;
            }

            //The child nodes are either a "<params>" or a "<paramgroup>" node
            //Create the appropriate DerivedNeed object and add it into the myNeeds list
            DerivedNeed derivedNeed = null;
            if (child.getNodeName().equals(TAG_PARAMS)) {
                //If it is a params node then collect the p1 ... pn attribute 
                //strings into the needList
                int    paramIdx = 1;
                List   needList = new ArrayList();
                String param;
                while ((param = XmlUtil.getAttribute(child,
                        ATTR_PARAMPREFIX + (paramIdx++),
                        NULL_STRING)) != null) {
                    needList.add(param);
                }
                derivedNeed = new DerivedNeed(this, needList, needCategory);
            } else if (child.getNodeName().equals(TAG_PARAMGROUP)) {
                String paramGroupName = XmlUtil.getAttribute(child,
                                            ATTR_NAME);
                derivedNeed = new DerivedNeed(this, paramGroupName,
                        needCategory);

            } else {
                //Humm, unknown tag
            }
            if (derivedNeed != null) {
                myNeeds.add(derivedNeed);
            }
        }
        checkNeeds();
    }


    /**
     * Get list of DerivedNeed-s
     *
     * @return derived needs
     */
    public List getNeeds() {
        if (myNeeds != null) {
            return new ArrayList(myNeeds);
        }
        return null;
    }


    /**
     * Set the needs list
     *
     * @param needs The needs
     */
    public void setNeeds(List<DerivedNeed> needs) {
        if (needs != null) {
            myNeeds = new ArrayList<DerivedNeed>(needs);
        } else {
            myNeeds = null;
        }
        checkNeeds();
    }




    /**
     * check the myNeeds list
     */
    private void checkNeeds() {
        if (myNeeds != null) {
            List tmp = new ArrayList();
            for (int i = 0; i < myNeeds.size(); i++) {
                Object obj = myNeeds.get(i);
                if (obj instanceof DerivedNeed) {
                    tmp.add(obj);
                }
            }
            myNeeds = tmp;
        }
    }


    /**
     * Mostly used by the IntegratedDataViewer FormulaDialog when creating an
     * end-user formula. Sets the dataCategories that characterize the result
     * of the DerivedDataChoice-s defined by the DDD.
     *
     * @param categories   list of categories
     */
    public void setDataCategories(List categories) {
        this.categories = new ArrayList(categories);
    }


    /**
     * Return the list of {@link DataCategory}s
     * @return The list of {@link DataCategory}s defined for this descriptor.
     */
    public List getDataCategories() {
        return categories;
    }


    /**
     *  Called by the DerivedDataChoice.getDerivedDataChoices method
     *  before the search for the set of possible DerivedDataChoice-s
     *  are created. This simply  turns around and calls initForSearch
     *  on the set of {@link DerivedNeed}-s
     */
    public void initForSearch() {
        if (myNeeds == null) {
            return;
        }
        for (int i = 0; i < myNeeds.size(); i++) {
            DerivedNeed need = (DerivedNeed) myNeeds.get(i);
            need.initForSearch();
        }
    }

    /**
     *  Called by the DerivedDataChoice.getDerivedDataChoices method
     *  after the search for the set of possible DerivedDataChoice-s
     *  are created. This simply  turns around and calls clearAfterSearch
     *  on the set of {@link DerivedNeed}-s
     */
    public void clearAfterSearch() {
        for (int i = 0; i < myNeeds.size(); i++) {
            DerivedNeed need = (DerivedNeed) myNeeds.get(i);
            need.clearAfterSearch();
        }
    }


    /**
     * This creates a new DerivedDataChoice whose state is defined
     * by this DerivedDataDescriptor
     *
     * @return  new DDC
     */
    public DerivedDataChoice getDataChoice() {
        DerivedDataChoice ddc = new DerivedDataChoice(dataContext, this);
        if (properties != null) {
            ddc.setProperties(properties);
        }
        return ddc;
    }


    /**
     * If the choicesSoFar map contains each parameter in any of the parameter
     * groups return a NamedList holding the label to be used and the
     * collection of dataChoices that correspond to the required parameters.
     * If no match return null.
     *
     * @param choicesSoFar  table of choices to check
     * @return  NamedList containing the collection of DataChoices or null
     */
    public NamedList getDataChoices(Hashtable choicesSoFar) {
        //Find the first DerivedNeed that is satisfied with the
        //DataChoice-s held in choicesSoFar
        for (int i = 0; i < myNeeds.size(); i++) {
            DerivedNeed need        = (DerivedNeed) myNeeds.get(i);
            List        dataChoices = need.getDataChoices(choicesSoFar);
            if (dataChoices != null) {
                return new NamedList(getDescription(), dataChoices);
            }
        }
        return null;
    }



    /**
     * Return the XML Element that represents this DerivedDataDescriptor
     *
     * @param doc     document
     * @return  XML Element representing this.
     */
    public Element toXml(Document doc) {
        Element node = doc.createElement(TAG_DERIVED);
        node.setAttribute(ATTR_ID, id);
        node.setAttribute(ATTR_DESCRIPTION, description);
        //      node.setAttribute (ATTR_LABEL, label);

        if (method != null) {
            node.setAttribute(ATTR_METHOD, method);
        }
        if (formula != null) {
            node.setAttribute(ATTR_FORMULA, formula);
        }
        if (code != null) {
            node.setAttribute(ATTR_CODE, code);
        }
        node.setAttribute(ATTR_ISENDUSER, "" + getIsEndUser());
        node.setAttribute(ATTR_ISDEFAULT, "" + getIsDefault());
        Element categoriesNode = doc.createElement(TAG_CATEGORIES);
        node.appendChild(categoriesNode);
        for (int i = 0; i < categories.size(); i++) {
            DataCategory cat          = (DataCategory) categories.get(i);
            Element      categoryNode = doc.createElement(TAG_CATEGORY);
            categoriesNode.appendChild(categoryNode);
            categoryNode.setAttribute(ATTR_VALUE, cat.toString());
            if (cat.getForDisplay()) {
                categoryNode.setAttribute(ATTR_DISPLAY, "true");
            }

            if (cat.getAppend() != null) {
                categoryNode.setAttribute(ATTR_APPEND, cat.getAppend());
            }
            if (cat.getReplace() != null) {
                categoryNode.setAttribute(ATTR_REPLACE, cat.getReplace());
            }
            if (cat.getChildIndex() >= 0) {
                categoryNode.setAttribute(ATTR_CHILDIDX,
                                          "" + cat.getChildIndex());
            }
            if (cat.getCategoryIndex() >= 0) {
                categoryNode.setAttribute(ATTR_CATEGORYIDX,
                                          "" + cat.getCategoryIndex());
            }
        }

        Element opNode = doc.createElement(TAG_OPERANDS);
        if (operandsCategories != null) {
            opNode.setAttribute(ATTR_CATEGORIES, operandsCategories);
        }
        node.appendChild(opNode);
        for (int needIdx = 0; needIdx < myNeeds.size(); needIdx++) {
            DerivedNeed need      = (DerivedNeed) myNeeds.get(needIdx);
            String      groupName = need.getGroupName();
            Element     needNode;
            if (groupName != null) {
                needNode = doc.createElement(TAG_PARAMGROUP);
                needNode.setAttribute(ATTR_NAME, groupName);
            } else {
                needNode = doc.createElement(TAG_PARAMS);
                List params = (List) (need.getParamSets().get(0));
                for (int i = 0; i < params.size(); i++) {
                    needNode.setAttribute(ATTR_PARAMPREFIX + (i + 1),
                                          params.get(i).toString());
                }
            }

            if (need.categories != null) {
                needNode.setAttribute(ATTR_CATEGORIES, need.categories);
            }
            opNode.appendChild(needNode);
        }

        return node;
    }


    /**
     * Return a string representation of this DerivedDataDescriptor.
     *
     * @return  string representation of this DerivedDataDescriptor
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        //buf.append("\n\t");
        //buf.append("id: ");
        for (int i = 0; i < categories.size(); i++) {
            DataCategory cat = (DataCategory) categories.get(i);
            if (cat.getForDisplay()) {
                buf.append(cat.toString());
                buf.append(">");
                break;
            }
        }

        if ((description != null) && (description.trim().length() > 0)) {
            buf.append(description);
        } else if ((label != null) && (label.trim().length() > 0)) {
            buf.append(label);
        } else {
            buf.append(id);
        }
        /*
        buf.append("\n\t");
        buf.append("formula: ");
        buf.append(formula);
        buf.append("\n\t");
        buf.append("method: ");
        buf.append(method);
        buf.append("\n\t");
        buf.append("code: ");
        buf.append(code);
        buf.append("\n\t");
        buf.append("myNeeds: ");
        buf.append(myNeeds);
        buf.append("\n\t");
        buf.append("categories: ");
        buf.append(categories);
        buf.append("\n\t");
        buf.append("id: ");
        buf.append(id);
        buf.append("\n\t");
        buf.append("isDefault: ");
        buf.append(isDefault);
        buf.append("\n\t");
        buf.append("isEndUser: ");
        buf.append(isEndUser);
        */
        return buf.toString();
    }


    /**
     * Check to see if the Object is equal to this one.
     *
     * @param other  Object in question
     * @return  true if they are equal
     */
    public boolean equals(Object other) {
        if ( !(other instanceof DerivedDataDescriptor)) {
            return false;
        }
        DerivedDataDescriptor that = (DerivedDataDescriptor) other;
        return (this == that)
               || (Misc.equals(formula, that.formula)
                   && Misc.equals(method, that.method)
                   && Misc.equals(code, that.code)
                   && Misc.equals(description, that.description)
                   && Misc.equals(myNeeds, that.myNeeds)
                   && Misc.equals(categories, that.categories)
                   && Misc.equals(id, that.id)
                   && (isDefault == that.isDefault)
                   && (isEndUser == that.isEndUser));
    }


    /**
     * Return the first {@link DataCategory} in the list of categories
     * that is for display purposes.
     *
     * @return The display category
     */
    public DataCategory getDisplayCategory() {
        if ((categories == null) || (categories.size() == 0)) {
            return null;
        }
        for (int i = 0; i < categories.size(); i++) {
            DataCategory dataCategory = (DataCategory) categories.get(i);
            if (dataCategory.getForDisplay()) {
                return dataCategory;
            }
        }
        return null;
    }


    /**
     * Get the Jython procedure for this derived data
     *
     * @return  the procedure or <code>null</code>
     */
    public String getJythonProcedure() {
        String def = getJythonProcedure(getId());
        return def;
    }

    /**
     * Get the Jython procedure for the specified procedure name
     *
     * @param procname   procedure name
     *
     * @return  the Jython procedure
     */
    public String getJythonProcedure(String procname) {
        //Misc.printStack ("  getJythonProcedure for "+procname); 
        String code = getFormula();
        if (code == null) {
            return null;
        }
        List operands = DerivedDataChoice.parseOperands(code);
        return "def " + procname + " (" + StringUtil.join(", ", operands)
               + "):\n" + "    return " + code + "\n\n";
    }


    /**
     * Set whether this is an end user derived quantity (ie, formula).
     *
     * @param value   true if end user
     */
    public void setIsEndUser(boolean value) {
        isEndUser = value;
    }

    /**
     * Get whether this is an end user derived quantity
     *
     * @return  true if end user
     */
    public boolean getIsEndUser() {
        return isEndUser;
    }


    /**
     * Set whether this is a formula the user has edited
     *
     * @param value   true if end user edited
     */
    public void setIsLocalUsers(boolean value) {
        isLocalUsers = value;
    }

    /**
     * Get whether this is a formula the user has edited
     *
     * @return  true if end user editable
     */
    public boolean getIsLocalUsers() {
        return isLocalUsers;
    }








    /**
     * Set whether this is a default derived quantity.
     *
     * @param value  true if default
     */
    public void setIsDefault(boolean value) {
        isDefault = value;
    }

    /**
     * Get whether this is a default derived quantity.
     *
     * @return  true if default
     */
    public boolean getIsDefault() {
        return isDefault;
    }

    /**
     * Set the formula for this derived quantity
     *
     * @param value  formula
     */
    public void setFormula(String value) {
        formula = value;
    }

    /**
     * Get the formula for this derived quantity
     *
     * @return  the formula
     */
    public String getFormula() {
        return formula;
    }

    /**
     * Set the id for this descriptor.
     *
     * @param value  the id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Get the id for this descriptor.
     *
     * @return  the id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the description for this descriptor
     *
     * @param value  the description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the description for this descriptor
     *
     * @return  the description
     */
    public String getDescription() {
        return description;
    }


    /**
     * Set the DataContext for this descriptor
     *
     * @param value  the data context
     */
    public void setDataContext(DataContext value) {
        dataContext = value;
    }

    /**
     * Get the DataContext for this descriptor
     *
     * @return  the data context
     */
    public DataContext getDataContext() {
        return dataContext;
    }



    /**
     * Create and return a list of the  DDDs that are defined by the given
     * xml root Element.
     *
     * @param dataContext    data context
     * @param root           root element
     * @param isLocal Is this one of the users local formulas
     * @return  list of DerivedDataDescriptors
     */
    public static List readDescriptors(DataContext dataContext, Element root,
                                       boolean isLocal) {
        List descriptors = new ArrayList();
        if ( !root.getTagName().equals(TAG_DERIVEDTOP)) {
            //Try it as a list of DDDs
            Object obj =
                dataContext.getIdv().getEncoderForRead().toObject(root);
            if (obj instanceof List) {
                List l = (List) obj;
                for (int i = 0; i < l.size(); i++) {
                    DerivedDataDescriptor ddd =
                        (DerivedDataDescriptor) l.get(i);
                    ddd.setIsLocalUsers(isLocal);
                    ddd.dataContext = dataContext;
                    descriptors.add(ddd);
                }
            } else if (obj instanceof DerivedDataDescriptor) {
                DerivedDataDescriptor ddd = (DerivedDataDescriptor) obj;
                ddd.setIsLocalUsers(isLocal);
                ddd.dataContext = dataContext;
                descriptors.add(ddd);
            }
        } else {
            List children = XmlUtil.findChildren(root, TAG_DERIVED);
            for (int i = 0; i < children.size(); i++) {
                DerivedDataDescriptor ddd =
                    new DerivedDataDescriptor(dataContext,
                        (Element) children.get(i));
                ddd.setIsLocalUsers(isLocal);
                descriptors.add(ddd);
            }
        }
        return descriptors;
    }

    /**
     * Read in the java classes defined in any  class  tags
     * under the root. We use this to preload the jython interpreter with
     * the full class names so we can later (in the derived.xml) refer to
     * them with just the class name
     *
     * @param root    root element
     * @return  List of classes
     */
    public static List readClasses(Element root) {
        List classes  = new ArrayList();
        List children = XmlUtil.findChildren(root, TAG_CLASS);
        for (int i = 0; i < children.size(); i++) {
            Element child = (Element) children.get(i);
            classes.add(XmlUtil.getAttribute(child, ATTR_NAME));
        }
        return classes;
    }


    /**
     * Create the xml representation (the derived.xml) of the given
     * list of DerivedDataDescriptors.
     *
     * @param descriptors    list of descriptors to write out
     * @return  XML version of this list
     */
    public static String toXml(List descriptors) {
        Document doc = XmlUtil.makeDocument();
        Element  top = doc.createElement(TAG_DERIVEDTOP);
        for (int i = 0; i < descriptors.size(); i++) {
            DerivedDataDescriptor ddd =
                (DerivedDataDescriptor) descriptors.get(i);
            top.appendChild(ddd.toXml(doc));
        }
        return XmlUtil.toString(top);

    }

    /**
     * Update derived needs when the DataGroups change
     */
    public void updateDataGroups() {
        for (int i = 0; i < myNeeds.size(); i++) {
            DerivedNeed need = (DerivedNeed) myNeeds.get(i);
            need.reInitialize();
        }
    }



    /**
     * Go through each xml file defined by the given XmlResourceCollection
     * and create the DerivedDataDescriptor-s defined in the xml.
     *
     * @param dataContext   the data context
     * @param xrc           the resource collection of DDDs
     *
     * @return List of DerivedDataDescriptors
     */
    public static List init(DataContext dataContext,
                            XmlResourceCollection xrc) {
        List descriptors = new ArrayList();
        try {
            for (int i = 0; i < xrc.size(); i++) {
                Element root = xrc.getRoot(i);
                if (root == null) {
                    continue;
                }
                descriptors.addAll(
                    DerivedDataDescriptor.readDescriptors(
                        dataContext, root, xrc.isWritableResource(i)));
                classes.addAll(DerivedDataDescriptor.readClasses(root));
            }
        } catch (Exception exc) {
            LogUtil.printException(log_, "Initializing DerivedDataChoices",
                                   exc);
        }
        return descriptors;
    }



    /**
     *  Add the given data choice into the Hashtable of choicesSoFar.
     *  We really map the id of the data choice to a list of data choices
     *  (one of which is the given dataChoice)
     *
     * @param choicesSoFar   existing choices
     * @param dataChoice     new choice
     */
    public static void addToChoicesSoFar(Hashtable choicesSoFar,
                                         DataChoice dataChoice) {
        Object key = dataChoice.getName();
        List   l   = (List) choicesSoFar.get(key);
        if (l == null) {
            l = new ArrayList();
            choicesSoFar.put(key, l);
        }
        l.add(dataChoice);
    }


    /**
     * Search through and return a list of DerivedDataChoices that can be
     * created from the given list of sourceChoices. We use the
     * DerivedDataDescriptor created from derived.xml to find out what new
     * DDCs can be created. This method serves as a wrapper around the
     * getDerivedDataChoicesInner method, synchronizing on the list
     *
     * @param context        context (the IDV)
     * @param sourceChoices  choices to search
     * @param descriptors The descriptors
     * @return  list of DDCs in sourceChoices
     */
    public static List getDerivedDataChoices(DataContext context,
                                             List sourceChoices,
                                             List descriptors) {
        if (descriptors == null) {
            return null;
        }
        //Synchronize around the descriptors list 
        synchronized (descriptors) {
            return getDerivedDataChoicesInner(context, sourceChoices,
                    descriptors);
        }
    }

    /**
     * Inner private method called from getDerivedDataChoices
     *
     * @param context           the context (the IDV)
     * @param sourceChoices     list of choices
     * @param descriptors The descriptors
     * @return
     */
    private static List getDerivedDataChoicesInner(DataContext context,
            List sourceChoices, List descriptors) {
        List      newChoices   = new ArrayList();
        Hashtable choicesSoFar = new Hashtable();

        for (int dddIdx = 0; dddIdx < descriptors.size(); dddIdx++) {
            DerivedDataDescriptor ddd =
                (DerivedDataDescriptor) descriptors.get(dddIdx);
            //Some are not meant for default derived quantities
            if ( !ddd.getIsDefault()) {
                continue;
            }
            ddd.initForSearch();
        }


        //
        //Fill in the map of data choice ids to data choice objects
        //
        for (int i = 0; i < sourceChoices.size(); i++) {
            addToChoicesSoFar(choicesSoFar,
                              (DataChoice) sourceChoices.get(i));
        }

        //
        //Go thru each descriptor and check to see if it matches any of the dataChoices 
        //we have so far. If it does then construct  the DerivedDataChoice and add that to the
        //list of dataChoices. We use a tmp list because we remove descriptors that match
        //

        DerivedDataChoice ddc;
        boolean           addedNewChoice = true;

        //
        //Keep going until there is no more
        //
        while (addedNewChoice) {
            addedNewChoice = false;
            for (int dddIdx = 0; dddIdx < descriptors.size(); dddIdx++) {
                DerivedDataDescriptor ddd =
                    (DerivedDataDescriptor) descriptors.get(dddIdx);

                //Some DDDs are set to not be default derived quantities 
                if ( !ddd.getIsDefault()) {
                    continue;
                }

                //Ask the DDD for any new derived quantities
                //The result is a data structure that holds a name
                //and a list of DataChoices (i.e., the operands)
                NamedList result = ddd.getDataChoices(choicesSoFar);
                if (result == null) {
                    continue;
                }
                try {
                    //Create the new DerivedDataChoice and add it into the list of new choices so far
                    //                        System.err.println ("ddd:" + ddd);
                    newChoices.add(ddc = new DerivedDataChoice(context,
                            result.getList(), result.getName(), ddd));
                    addToChoicesSoFar(choicesSoFar, ddc);
                    addedNewChoice = true;
                } catch (Exception exc) {
                    LogUtil.logException("Error making derived data choice",
                                         exc);
                }
            }
        }

        //Now make sure we clear the DerivedNeed-s so we don't have any dangling pointers
        for (int dddIdx = 0; dddIdx < descriptors.size(); dddIdx++) {
            DerivedDataDescriptor ddd =
                (DerivedDataDescriptor) descriptors.get(dddIdx);
            if ( !ddd.getIsDefault()) {
                continue;
            }
            ddd.clearAfterSearch();
        }
        return newChoices;
    }




    /**
     * Method to test
     *
     * @param args   arguments (not used)
     */
    public static void main(String[] args) {
        try {
            Element root = XmlUtil.getRoot(IOUtil.readContents("derived.xml",
                               DerivedDataDescriptor.class));
            System.out.println(XmlUtil.toString(root));
        } catch (Exception exc) {
            exc.printStackTrace();
        }

    }

    /**
     * Set the OperandsCategories property.
     *
     * @param value The new value for OperandsCategories
     */
    public void setOperandsCategories(String value) {
        operandsCategories = value;
        if ((operandsCategories != null)
                && (operandsCategories.trim().length() > 0)) {
            operandsCategoryList =
                DataCategory.parseCategories(operandsCategories);
        } else {
            operandsCategoryList = null;
        }
    }

    /**
     * Get the OperandsCategories property.
     *
     * @return The OperandsCategories
     */
    public String getOperandsCategories() {
        return operandsCategories;
    }



}

