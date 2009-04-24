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

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.List;



/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class MetadataType implements Constants {

    /** _more_ */
    public static String ARG_TYPE = "type";

    /** _more_ */
    public static String ARG_ATTR1 = "attr1";

    /** _more_ */
    public static String ARG_ATTR2 = "attr2";

    /** _more_ */
    public static String ARG_ATTR3 = "attr3";

    /** _more_ */
    public static String ARG_ATTR4 = "attr4";


    /** _more_          */
    public static String ARG_METADATAID = "metadataid";

    /** _more_ */
    private String type;

    /** _more_          */
    private String name;

    private String displayCategory = "Metadata";

    private boolean showInHtml = true;

    private boolean adminOnly = false;

    /** _more_ */
    private List<MetadataElement> elements = new ArrayList<MetadataElement>();

    private MetadataHandler handler;


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



    /**
     * _more_
     *
     * @param type _more_
     * @param name _more_
     */
    public MetadataType(String type, String name) {
        this.type   = type;
        this.name = name;
    }


    public String toString() {
        return type;
    }

    /**
     * _more_
     *
     * @param root _more_
     * @param repository _more_
     *
     * @return _more_
     */
    public static List<MetadataType> parse(Element root,
                                           Repository repository) {
        List<MetadataType> types = new ArrayList<MetadataType>();
        return types;
    }



    /**
     * _more_
     *
     * @param handler _more_
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     */
    public String[] getHtml(MetadataHandler handler, Request request,
                            Entry entry, Metadata metadata) {
        if(!showInHtml) return null;
        String       lbl     = handler.msgLabel(name);
        StringBuffer content = new StringBuffer();
        for (MetadataElement element : elements) {
            content.append(element.getLabel() + ":" + metadata.getAttr1()
                           + HtmlUtil.space(3));
        }
        return new String[] { lbl, content.toString() };
    }


    /**
     * _more_
     *
     * @param handler _more_
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param forEdit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getForm(MetadataHandler handler, Request request,
                            Entry entry, Metadata metadata, boolean forEdit)
            throws Exception {

        String lbl    = handler.msgLabel(name);
        String suffix = "";
        if (metadata.getId().length() > 0) {
            suffix = "." + metadata.getId();
        }

        String submit = HtmlUtil.submit(handler.msg("Add")
                                        + HtmlUtil.space(1) + name);
        String cancel = HtmlUtil.submit(handler.msg("Cancel"), ARG_CANCEL);

        String[] args = { ARG_ATTR1 + suffix, ARG_ATTR2 + suffix,
                          ARG_ATTR3 + suffix, ARG_ATTR4 + suffix };

        String[] values = { metadata.getAttr1(), metadata.getAttr2(),
                            metadata.getAttr3(), metadata.getAttr4() };


        StringBuffer sb  = new StringBuffer();

        int          cnt = 0;
        for (MetadataElement element : elements) {
            String elementLbl = handler.msgLabel(element.getLabel());
            String widget     = element.getForm(args[cnt], values[cnt]);
            sb.append(HtmlUtil.formEntry(elementLbl, widget));
            cnt++;
        }





        String argtype = ARG_TYPE + suffix;
        String argid   = ARG_METADATAID + suffix;
        sb.append(HtmlUtil.hidden(argtype, type)
                  + HtmlUtil.hidden(argid, metadata.getId()));

        if ( !forEdit) {
            sb.append(HtmlUtil.formEntry("", submit + cancel));
        }


        return new String[] { lbl, sb.toString() };
    }



    /**
     * _more_
     *
     * @param element _more_
     */
    public void addElement(MetadataElement element) {
        elements.add(element);
    }


    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    public boolean isType(String type) {
        return Misc.equals(this.type, type);
    }

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
     *  Get the Type property.
     *
     *  @return The Type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    public String getLabel() {
        return name;
    }

/**
Set the ShowInHtml property.

@param value The new value for ShowInHtml
**/
public void setShowInHtml (boolean value) {
	this.showInHtml = value;
}

/**
Get the ShowInHtml property.

@return The ShowInHtml
**/
public boolean getShowInHtml () {
	return this.showInHtml;
}

/**
Set the DisplayCategory property.

@param value The new value for DisplayCategory
**/
public void setDisplayCategory (String value) {
	this.displayCategory = value;
}

/**
Get the DisplayCategory property.

@return The DisplayCategory
**/
public String getDisplayCategory () {
	return this.displayCategory;
}

/**
Set the AdminOnly property.

@param value The new value for AdminOnly
**/
public void setAdminOnly (boolean value) {
	this.adminOnly = value;
}

/**
Get the AdminOnly property.

@return The AdminOnly
**/
public boolean getAdminOnly () {
	return this.adminOnly;
}


/**
Set the Handler property.

@param value The new value for Handler
**/
public void setHandler (MetadataHandler value) {
	this.handler = value;
}

/**
Get the Handler property.

@return The Handler
**/
public MetadataHandler getHandler () {
	return this.handler;
}



}

