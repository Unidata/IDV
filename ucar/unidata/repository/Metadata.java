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
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.repository;


import org.w3c.dom.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Metadata implements Constants, Tables {


    /** _more_ */
    private Entry entry;

    /** _more_ */
    private String id;

    /** _more_ */
    private String entryId;

    /** _more_ */
    private String type;


    /** _more_ */
    private String attr1;

    /** _more_ */
    private String attr2;

    /** _more_ */
    private String attr3;

    /** _more_ */
    private String attr4;

    /** _more_ */
    private boolean inherited = false;

    /**
     * _more_
     *
     * @param type _more_
     */
    public Metadata(String type) {
        this("", "", type, false, "", "", "", "");
    }


    /**
     * _more_
     *
     * @param type _more_
     * @param attr1 _more_
     * @param attr2 _more_
     * @param attr3 _more_
     * @param attr4 _more_
     */
    public Metadata(String type, String attr1, String attr2, String attr3,
                    String attr4) {
        this("", "", type, false, attr1, attr2, attr3, attr4);
    }


    /**
     * _more_
     *
     * @param type _more_
     */
    public Metadata(Type type) {
        this("", "", type, false, "", "", "", "");
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param entryId _more_
     * @param type _more_
     * @param inherited _more_
     * @param attr1 _more_
     * @param attr2 _more_
     * @param attr3 _more_
     * @param attr4 _more_
     */
    public Metadata(String id, String entryId, Type type, boolean inherited,
                    String attr1, String attr2, String attr3, String attr4) {
        this(id, entryId, type.getType(), inherited, attr1, attr2, attr3,
             attr4);
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param entryId _more_
     * @param type _more_
     * @param inherited _more_
     * @param attr1 _more_
     * @param attr2 _more_
     * @param attr3 _more_
     * @param attr4 _more_
     */
    public Metadata(String id, String entryId, String type,
                    boolean inherited, String attr1, String attr2,
                    String attr3, String attr4) {
        this.id        = id;
        this.entryId   = entryId;
        this.type      = type;
        this.inherited = inherited;
        this.attr1     = attr1;
        this.attr2     = attr2;
        this.attr3     = attr3;
        this.attr4     = attr4;
    }

    /**
     * Set the Id property.
     *
     * @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getId() {
        return id;
    }


    /**
     * Set the EntryId property.
     *
     * @param value The new value for EntryId
     */
    public void setEntryId(String value) {
        entryId = value;
    }

    /**
     * Get the EntryId property.
     *
     * @return The EntryId
     */
    public String getEntryId() {
        return entryId;
    }




    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public String getType() {
        return type;
    }




    /**
     * Set the Attr1 property.
     *
     * @param value The new value for Attr1
     */
    public void setAttr1(String value) {
        attr1 = value;
    }

    /**
     * Get the Attr1 property.
     *
     * @return The Attr1
     */
    public String getAttr1() {
        return attr1;
    }

    /**
     * Set the Attr2 property.
     *
     * @param value The new value for Attr2
     */
    public void setAttr2(String value) {
        attr2 = value;
    }

    /**
     * Get the Attr2 property.
     *
     * @return The Attr2
     */
    public String getAttr2() {
        return attr2;
    }

    /**
     * Set the Attr3 property.
     *
     * @param value The new value for Attr3
     */
    public void setAttr3(String value) {
        attr3 = value;
    }

    /**
     * Get the Attr3 property.
     *
     * @return The Attr3
     */
    public String getAttr3() {
        return attr3;
    }

    /**
     * Set the Attr4 property.
     *
     * @param value The new value for Attr4
     */
    public void setAttr4(String value) {
        attr4 = value;
    }

    /**
     * Get the Attr4 property.
     *
     * @return The Attr4
     */
    public String getAttr4() {
        return attr4;
    }


    /**
     *  Set the Inherited property.
     *
     *  @param value The new value for Inherited
     */
    public void setInherited(boolean value) {
        inherited = value;
    }

    /**
     *  Get the Inherited property.
     *
     *  @return The Inherited
     */
    public boolean getInherited() {
        return inherited;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return "entry:" + entryId + " type:" + type + " attr1:" + attr1
               + " attr2:" + attr2 + " attr3:" + attr3 + " attr4:" + attr4;
    }


    /**
     * _more_
     *
     * @param idx _more_
     *
     * @return _more_
     */
    public String getAttr(int idx) {
        if (idx == 1) {
            return attr1;
        }
        if (idx == 2) {
            return attr2;
        }
        if (idx == 3) {
            return attr3;
        }
        if (idx == 4) {
            return attr4;
        }
        throw new IllegalArgumentException("Bad attr idx:" + idx);
    }




    /**
     * Class Type _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class Type {

        /** _more_ */
        public static final String DEFAULT_CATEGORY = "Metadata";

        /** _more_ */
        public static final int SEARCHABLE_ATTR1 = 1 << 0;

        /** _more_ */
        public static final int SEARCHABLE_ATTR2 = 1 << 1;

        /** _more_ */
        public static final int SEARCHABLE_ATTR3 = 1 << 3;

        /** _more_ */
        public static final int SEARCHABLE_ATTR4 = 1 << 4;

        /** _more_ */
        public int searchableMask = 0;

        /** _more_ */
        private String type;

        /** _more_ */
        private String category = DEFAULT_CATEGORY;


        /** _more_ */
        private String label;

        /** _more_ */
        private boolean isEnumerated = false;

        /**
         * _more_
         *
         * @param type _more_
         */
        public Type(String type) {
            this.type = type;
            label     = type.replace("_", " ");
            label     = label.replace(".", " ");
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
        }

        /**
         * _more_
         *
         * @param type _more_
         * @param label _more_
         */
        public Type(String type, String label) {
            this(type, label, DEFAULT_CATEGORY, false);
        }

        /**
         * _more_
         *
         * @param type _more_
         * @param label _more_
         * @param category _more_
         */
        public Type(String type, String label, String category) {
            this(type, label, category, false);
        }

        /**
         * _more_
         *
         * @param type _more_
         * @param label _more_
         * @param enumerated _more_
         */
        public Type(String type, String label, boolean enumerated) {
            this(type, label, DEFAULT_CATEGORY, enumerated);
        }

        /**
         * _more_
         *
         * @param type _more_
         * @param label _more_
         * @param category _more_
         * @param enumerated _more_
         */
        public Type(String type, String label, String category,
                    boolean enumerated) {
            this.type         = type;
            this.label        = label;
            this.category     = category;
            this.isEnumerated = enumerated;
        }

        /**
         * _more_
         *
         * @param mask _more_
         */
        public void setSearchableMask(int mask) {
            searchableMask = mask;
        }


        /**
         * _more_
         *
         * @param mask _more_
         *
         * @return _more_
         */
        public boolean isSearchable(int mask) {
            return (searchableMask & mask) != 0;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isAttr1Searchable() {
            return isSearchable(SEARCHABLE_ATTR1);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isAttr2Searchable() {
            return isSearchable(SEARCHABLE_ATTR2);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isAttr3Searchable() {
            return isSearchable(SEARCHABLE_ATTR3);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isAttr4Searchable() {
            return isSearchable(SEARCHABLE_ATTR4);
        }



        /**
         * _more_
         *
         * @param type _more_
         *
         * @return _more_
         */
        public boolean isType(String type) {
            return this.type.equals(type);
        }

        /**
         * _more_
         *
         * @param o _more_
         *
         * @return _more_
         */
        public boolean equals(Object o) {
            if ( !getClass().equals(o.getClass())) {
                return false;
            }
            Type that = (Type) o;
            return type.equals(that.type);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return type;
        }


        /**
         * Set the Type property.
         *
         * @param value The new value for Type
         */
        public void setType(String value) {
            type = value;
        }

        /**
         * Get the Type property.
         *
         * @return The Type
         */
        public String getType() {
            return type;
        }

        /**
         * Set the Label property.
         *
         * @param value The new value for Label
         */
        public void setLabel(String value) {
            label = value;
        }

        /**
         * Get the Label property.
         *
         * @return The Label
         */
        public String getLabel() {
            return label;
        }

        /**
         *  Set the IsEnumerated property.
         *
         *  @param value The new value for IsEnumerated
         */
        public void setIsEnumerated(boolean value) {
            isEnumerated = value;
        }

        /**
         *  Get the IsEnumerated property.
         *
         *  @return The IsEnumerated
         */
        public boolean getIsEnumerated() {
            return isEnumerated;
        }


        /**
         * Set the Category property.
         *
         * @param value The new value for Category
         */
        public void setCategory(String value) {
            category = value;
        }

        /**
         * Get the Category property.
         *
         * @return The Category
         */
        public String getCategory() {
            return category;
        }



    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !getClass().equals(o.getClass())) {
            return false;
        }
        Metadata that = (Metadata) o;
        /*
        System.err.println(Misc.equals(this.type,  that.type) + " " +
            Misc.equals(this.attr1, that.attr1) + " " +
            Misc.equals(this.attr2, that.attr2) + " " +
            Misc.equals(this.attr3, that.attr3) + " " +
            Misc.equals(this.attr4, that.attr4) + " " +
            Misc.equals(this.entryId, that.entryId));*/

        return Misc.equals(this.type, that.type)
               && Misc.equals(this.attr1, that.attr1)
               && Misc.equals(this.attr2, that.attr2)
               && Misc.equals(this.attr3, that.attr3)
               && Misc.equals(this.attr4, that.attr4)
               && Misc.equals(this.entryId, that.entryId);
    }


    /**
     * Set the Entry property.
     *
     * @param value The new value for Entry
     */
    public void setEntry(Entry value) {
        entry = value;
        if (value != null) {
            entryId = value.getId();
        }
    }

    /**
     * Get the Entry property.
     *
     * @return The Entry
     */
    public Entry getEntry() {
        return entry;
    }



}

