/**
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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
 * along with this library; if not, write to the Free Software Foundastion,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */


package ucar.unidata.repository.listener;


import ucar.unidata.repository.*;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.File;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class Filter implements Constants {

    /** _more_          */
    private String field;

    /** _more_          */
    private Object value;

    /** _more_          */
    private boolean doNot = false;

    /**
     * _more_
     */
    public Filter() {}

    /**
     * _more_
     *
     * @param field _more_
     * @param value _more_
     */
    public Filter(String field, Object value) {
        this(field, value, false);
    }

    /**
     * _more_
     *
     * @param field _more_
     * @param value _more_
     * @param doNot _more_
     */
    public Filter(String field, Object value, boolean doNot) {
        this.field = field;
        this.value = value;
    }



    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     *
     * @return _more_
     */
    public boolean nameMatch(String s1, String s2) {
        //TODO: We need to have a StringMatcher object
        if (s1.endsWith("%")) {
            s1 = s1.substring(0, s1.length() - 1);
            return s2.startsWith(s1);
        }
        if (s1.startsWith("%")) {
            s1 = s1.substring(1);
            return s2.endsWith(s1);
        }
        return s2.equals(s1);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    public boolean checkEntry(Entry entry) {
        boolean ok = false;
        if (field.equals(ARG_TYPE)) {
            ok = value.equals(entry.getTypeHandler().getType());
        } else if (field.equals(ARG_NAME)) {
            ok = nameMatch(value.toString(), entry.getName());
        } else if (field.equals(ARG_DESCRIPTION)) {
            ok = nameMatch(value.toString(), entry.getDescription());
        } else if (field.equals(ARG_TEXT)) {
            ok = nameMatch(value.toString(), entry.getDescription())
                 || nameMatch(value.toString(), entry.getName());
        } else if (field.equals(ARG_USER)) {
            ok = Misc.equals(entry.getUser().getId(), value.toString());
        } else if (field.equals(ARG_WAIT)) {
            ok = true;
        } else if (field.equals(ARG_GROUP)) {
            //TODO: check for subgroups
            //                ok = (value.equals(entry.getParentGroup().getFullName())
            //                      || value.equals(entry.getParentGroup().getId()));
        } else {
            int match = entry.getTypeHandler().matchValue(field, value,
                            entry);
            if (match == TypeHandler.MATCH_FALSE) {
                ok = false;
            } else if (match == TypeHandler.MATCH_TRUE) {
                ok = true;
            } else {
                System.err.println("unknown field:" + field);
                return true;
            }
        }
        if (doNot) {
            return !ok;
        }
        return ok;
    }

    /**
     * Set the Field property.
     *
     * @param value The new value for Field
     */
    public void setField(String value) {
        field = value;
    }

    /**
     * Get the Field property.
     *
     * @return The Field
     */
    public String getField() {
        return field;
    }

    /**
     * Set the Value property.
     *
     * @param value The new value for Value
     */
    public void setValue(Object value) {
        value = value;
    }

    /**
     * Get the Value property.
     *
     * @return The Value
     */
    public Object getValue() {
        return value;
    }


    /**
     * Set the DoNot property.
     *
     * @param value The new value for DoNot
     */
    public void setDoNot(boolean value) {
        doNot = value;
    }

    /**
     * Get the DoNot property.
     *
     * @return The DoNot
     */
    public boolean getDoNot() {
        return doNot;
    }



}

