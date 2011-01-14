/*
 * $Id: DataOperand.java,v 1.16 2007/06/08 21:24:48 jeffmc Exp $
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
import ucar.unidata.util.StringUtil;



import visad.Data;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * This class holds a name/value pair that represents an operand for the
 * DerivedDataChoice
 *
 * @author IDV development team
 * @version $Revision: 1.16 $
 */
public class DataOperand {

    /** the categories property */
    private static final String PROP_CATEGORIES = "categories";

    /** Can this be multiple data choices */
    private static final String PROP_MULTIPLE = "multiple";

    /** pattern property in operand attributes */
    private static final String PROP_PATTERN = "pattern";

    /** description property in operand attributes */
    private static final String PROP_DESCRIPTION = "description";

    /** The level property */
    public static final String PROP_LEVEL = "level";


    /** the isuser property */
    private static final String PROP_ISUSER = "isuser";

    /** the label property */
    private static final String PROP_LABEL = "label";

    /** the data source property */
    private static final String PROP_DATASOURCE = "datasource";

    /** default value property */
    private static final String PROP_DEFAULT = "default";

    /** the times property */
    private static final String PROP_TIMES = "times";


    /** name of this operand */
    private String name;

    /** Data categories */
    private List categories;


    /** associated data */
    private Object data;


    /** Holds the properties that can be defined in the operand with: op[prop=value] */
    Hashtable properties = new Hashtable();

    /**
     * Create a new DataOperand with null data
     *
     * @param name  name for this object
     *
     */
    public DataOperand(String name) {
        this(name, null);
    }

    /**
     * Create a new DataOperand
     *
     * @param name    name for this object
     * @param data    associated data
     *
     */
    public DataOperand(String name, Object data) {
        this.name = name;
        this.data = data;
        int idx1 = name.indexOf("[");
        if (idx1 >= 0) {
            int idx2 = name.indexOf("]");
            if (idx2 > idx1) {
                String props = name.substring(idx1 + 1, idx2);
                //                System.err.println("PROPS:" + Misc.parseProperties(props,","));
                properties.putAll(Misc.parseProperties(props, ","));
            }
        }
    }


    /**
     * ctor
     *
     * @param name name
     * @param description  description
     * @param categories  categories
     * @param multiple supports multiples
     */
    public DataOperand(String name, String description, List categories,
                       boolean multiple) {
        this.name = name;
        putCategories(categories);
        if (description != null) {
            putDescription(description);
        }
        putMultiple(multiple);
    }




    /**
     * Put the description in the properties
     *
     * @param desc description
     */
    public void putDescription(String desc) {
        properties.put(PROP_DESCRIPTION, desc);
    }


    /**
     * Get the description from the properties
     *
     * @return description
     */
    public String getDescription() {
        String desc = (String) properties.get(PROP_DESCRIPTION);
        if (desc == null) {
            desc = (String) properties.get(PROP_LABEL);
        }
        if (desc == null) {
            return getParamName();
        }
        return desc;
    }


    /**
     * Put the pattern into the properties
     *
     * @param pattern the pattern
     */
    public void putPattern(String pattern) {
        properties.put(PROP_PATTERN, pattern);
    }


    /**
     * Get the pattern from the properties
     *
     * @return the pattern
     */
    public String getPattern() {
        return (String) properties.get(PROP_PATTERN);
    }


    /**
     * Check to see if the object in question is equal to this.
     *
     * @param o     Object in question
     * @return  true if they are equal
     */
    public boolean equals(Object o) {
        if ( !(o instanceof DataOperand)) {
            return false;
        }
        return Misc.equals(name, ((DataOperand) o).name)
               && Misc.equals(data, ((DataOperand) o).data);
    }


    /**
     * Return the hashcode for this DataOperand.
     *
     * @return the hashcode
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Return a string representation of this DataOperand.
     *
     * @return a string representation of this DataOperand
     */
    public String toString() {
        return name;
        //        return name +  " (" + (isBound()?"bound":"unbound")+")";
    }



    /**
     * Set the categories
     *
     * @param categories catgegories
     */
    public void putCategories(List categories) {
        this.categories = categories;
    }


    /**
     * Get the list of {@link DataCategory}s that are defined by the categories property
     *
     * @return List of data categories or null if none defined
     */
    public List getCategories() {
        if (categories != null) {
            return categories;
        }
        String catString = (String) properties.get(PROP_CATEGORIES);
        if (catString == null) {
            return null;
        }
        return DataCategory.parseCategories(catString, false);
    }

    /**
     * Set the name. (Used by XML persistence)
     *
     * @param value   name for this object
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the name. (Used by XML persistence)
     *
     * @return   this object's name
     */
    public String getName() {
        return name;
    }


    /**
     * Set the data. (Used by XML persistence)
     *
     * @param value  the data
     */
    public void setData(Object value) {
        data = value;
    }

    /**
     * Get the data. (Used by XML persistence)
     *
     * @return  the data
     */
    public Object getData() {
        return data;
    }

    /**
     * See if this a bound operand or not (has data)
     *
     * @return  true if data is not null
     */
    public boolean isBound() {
        return (data != null);
    }

    /**
     * Put the multiple property
     *
     * @param b multiple
     */
    public void putMultiple(boolean b) {
        properties.put(PROP_MULTIPLE, "" + b);
    }

    /**
     * Get the multiple property
     *
     * @return multiple
     */
    public boolean getMultiple() {
        String isMultiple = (String) properties.get(PROP_MULTIPLE);
        if (isMultiple == null) {
            return false;
        }
        return isMultiple.trim().equals("true");
    }

    /**
     * Is this operand persistent
     *
     * @return is persistent
     */
    public boolean isPersistent() {
        if (Misc.equals(getProperty("persistent"), "false")) {
            return false;
        }
        return true;
    }


    /**
     * Is this operand an end user defined value
     *
     * @return Is define dby the user
     */
    public boolean isUser() {
        if (name.startsWith("user_")) {
            return true;
        }
        String isUser = (String) properties.get(PROP_ISUSER);
        if (isUser == null) {
            return false;
        }
        return isUser.trim().equals("true");
    }


    /**
     * DOes this operand have a datasource property
     *
     * @return The data source property
     */
    public String getDataSourceName() {
        return (String) properties.get(PROP_DATASOURCE);
    }

    /**
     * Get the default user value
     *
     * @return The default for user specified values
     */
    public String getUserDefault() {
        return (String) properties.get(PROP_DEFAULT);
    }



    /**
     * Get the named property
     *
     * @param name property name
     *
     * @return The property
     */
    public String getProperty(String name) {
        return (String) properties.get(name);
    }


    /**
     * Get just the param name, strip off the categories
     *
     * @return The param name
     */
    public String getParamName() {
        int bracketIndex = name.lastIndexOf("[");
        if (bracketIndex >= 0) {
            return name.substring(0, bracketIndex);
        }
        return name;
    }


    /**
     * Get the name used for the label in the gui
     *
     * @return The label.
     */
    public String getLabel() {
        String label = (String) properties.get(PROP_LABEL);
        if (label == null) {
            label = getParamName();
            if (label.startsWith("user_")) {
                label = label.substring(5);
            }
        }
        return label;
    }



    /**
     * Remove all of the special IDV characters in the given jython operand.
     *
     * @return  cleaned up operand
     */
    public String makeLegalJython() {
        String op   = name;
        int    idx1 = op.indexOf("[");
        int    idx2 = op.indexOf("]");
        if ((idx1 >= 0) && (idx2 > idx1)) {
            String       internals = op.substring(idx1 + 1, idx2);
            StringBuffer good      = new StringBuffer("_");
            char[]       chars     = internals.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if (Character.isJavaIdentifierPart(chars[i])) {
                    good.append(chars[i]);
                } else if (chars[i] != ' ') {
                    good.append('_');
                }
            }
            good.append("_");
            op = op.substring(0, idx1).trim() + good
                 + op.substring(idx2 + 1).trim();
        }
        op = StringUtil.replace(op, ":", "_");
        return op;
    }

    /**
     *  Get the list of time (Integer) indices defined with the times property
     *
     * @return Listof time indices  or null
     */
    public List getTimeIndices() {
        String timeString = (String) properties.get(PROP_TIMES);
        if (timeString == null) {
            return null;
        }
        List timeStrings = StringUtil.split(timeString, ";", true, true);
        List timeIndices = new ArrayList();
        for (int timeIdx = 0; timeIdx < timeStrings.size(); timeIdx++) {
            timeIndices.add(new Integer(timeStrings.get(timeIdx).toString()));
        }
        return timeIndices;
    }


    /**
     * Test
     *
     * @param argv Cmd line args
     */
    public static void main(String[] argv) {
        for (int i = 0; i < argv.length; i++) {
            new DataOperand(argv[i]);
        }
    }


}

