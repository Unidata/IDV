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
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.net.URL;
import java.net.URLConnection;

import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class MetadataElement implements Constants {

    /** _more_ */
    public static final String TYPE_SKIP = "skip";

    /** _more_          */
    public static final String TYPE_STRING = "string";

    /** _more_          */
    public static final String TYPE_URL = "url";

    /** _more_          */
    public static final String TYPE_EMAIL = "email";

    /** _more_          */
    public static final String TYPE_FILE = "file";

    /** _more_ */
    public static final String TYPE_BOOLEAN = "boolean";

    /** _more_ */
    public static final String TYPE_ENUMERATION = "enumeration";

    /** _more_ */
    private String type = TYPE_STRING;

    /** _more_ */
    private String label = "";

    /** _more_ */
    private int rows = 1;

    /** _more_ */
    private int columns = 60;

    /** _more_ */
    private List values;

    /** _more_ */
    private String dflt = "";

    /** _more_          */
    private boolean thumbnail = false;

    /** _more_          */
    private int index;

    /** _more_          */
    private MetadataType metadataType;


    /**
     * _more_
     *
     *
     * @param metadataType _more_
     * @param index _more_
     * @param type _more_
     * @param label _more_
     * @param rows _more_
     * @param columns _more_
     * @param values _more_
     */
    public MetadataElement(MetadataType metadataType, int index, String type,
                           String label, int rows, int columns,
                           List<Object> values) {
        this.metadataType = metadataType;
        this.index        = index;
        this.type         = type;
        this.label        = label;
        this.rows         = rows;
        this.columns      = columns;
        this.values       = values;
    }


    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    private boolean isString(String type) {
        return type.equals(TYPE_STRING) || type.equals(TYPE_EMAIL)
               || type.equals(TYPE_URL);
    }


    /**
     * _more_
     *
     * @param handler _more_
     * @param sb _more_
     * @param value _more_
     *
     * @return _more_
     */
    public boolean getHtml(MetadataHandler handler, StringBuffer sb,
                           String value) {
        if (type.equals(TYPE_SKIP)) {
            return false;
        }
        if (type.equals(TYPE_FILE)) {
            return false;
        }
        if (type.equals(TYPE_EMAIL)) {
            sb.append(HtmlUtil.href("mailto:" + value, value));
        } else if (type.equals(TYPE_URL)) {
            sb.append(HtmlUtil.href(value, value));
        } else {
            sb.append(value);
        }
        sb.append(HtmlUtil.br());
        return true;
    }

    public void handleForm(Request request, MetadataType type, Entry entry,
                           Metadata newMetadata, 
                           Metadata oldMetadata,                           
                           String suffix)
            throws Exception {

        String arg = MetadataType.ARG_ATTR + getIndex() + suffix;

        if (getType().equals(TYPE_BOOLEAN)) {
            boolean value  = request.get(arg, false);
            newMetadata.setAttr(getIndex(), ""+value);
            return;
        }


        String attr = request.getString(arg, "");
        if (request.defined(arg + ".select")) {
            attr = request.getString(arg + ".select", "");
        }
        newMetadata.setAttr(getIndex(), attr);

        if ( !getType().equals(TYPE_FILE)) {
            return;
        }


        if (oldMetadata != null) {
            newMetadata.setAttr(getIndex(),
                             oldMetadata.getAttr(getIndex()));
        }

        String url = request.getString(arg + ".url", "");
        String theFile = null;
        if (url.length() > 0) {
            String tail = IOUtil.getFileTail(url);
            File tmpFile =
                type.getHandler().getStorageManager().getTmpFile(request, tail);
                RepositoryUtil.checkFilePath(tmpFile.toString());
                URL              fromUrl    = new URL(url);
                URLConnection    connection = fromUrl.openConnection();
                InputStream      fromStream = connection.getInputStream();
                FileOutputStream toStream   = new FileOutputStream(tmpFile);
                try {
                    int bytes = IOUtil.writeTo(fromStream, toStream);
                    if (bytes < 0) {
                        throw new IllegalArgumentException(
                            "Could not download url:" + url);
                    }
                } catch (Exception ioe) {
                    throw new IllegalArgumentException(
                        "Could not download url:" + url);
                } finally {
                    try {
                        toStream.close();
                        fromStream.close();
                    } catch (Exception exc) {}
                }
                theFile = tmpFile.toString();
            } else {
                String fileArg = request.getUploadedFile(arg);
                if (fileArg == null) {
                    return;
                }
                theFile = fileArg;
            }
            theFile =
                type.getHandler().getRepository().getStorageManager().moveToEntryDir(
                    entry, new File(theFile)).getName();
            newMetadata.setAttr(getIndex(), theFile);


    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param arg _more_
     * @param value _more_
     * @param forEdit _more_
     *
     * @return _more_
     */
    public String getForm(Request request, Entry entry, Metadata metadata,
                          String arg, String value, boolean forEdit) {
        if (type.equals(TYPE_SKIP)) {
            return "";
        }

        value = (((value == null) || (value.length() == 0))
                 ? dflt
                 : value);
        if (isString(type)) {
            if (rows > 1) {
                return HtmlUtil.textArea(arg, value, rows, columns);
            }

            return HtmlUtil.input(arg, value,
                                  HtmlUtil.attr(HtmlUtil.ATTR_SIZE,
                                      "" + columns));
        } else if (type.equals(TYPE_BOOLEAN)) {
            return HtmlUtil.checkbox(arg, "true", Misc.equals(value, "true"));
        } else if (type.equals(TYPE_ENUMERATION)) {
            return HtmlUtil.select(arg, values, value);
        } else if (type.equals(TYPE_FILE)) {
            String image = (forEdit
                            ? metadataType.getFileHtml(request, entry,
                                metadata, this, false)
                            : "");
            if (image == null) {
                image = "";
            } else {
                image = "<br>" + image;
            }
            return HtmlUtil.fileInput(arg, HtmlUtil.SIZE_70) + image + "<br>"
                   + "Or download URL:"
                   + HtmlUtil.input(arg + ".url", "", HtmlUtil.SIZE_70);
        } else {
            return null;
        }
    }


    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
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
     *  Set the Label property.
     *
     *  @param value The new value for Label
     */
    public void setLabel(String value) {
        label = value;
    }

    /**
     *  Get the Label property.
     *
     *  @return The Label
     */
    public String getLabel() {
        return label;
    }

    /**
     *  Set the Rows property.
     *
     *  @param value The new value for Rows
     */
    public void setRows(int value) {
        rows = value;
    }

    /**
     *  Get the Rows property.
     *
     *  @return The Rows
     */
    public int getRows() {
        return rows;
    }

    /**
     *  Set the Columns property.
     *
     *  @param value The new value for Columns
     */
    public void setColumns(int value) {
        columns = value;
    }

    /**
     *  Get the Columns property.
     *
     *  @return The Columns
     */
    public int getColumns() {
        return columns;
    }


    /**
     * Set the Values property.
     *
     * @param value The new value for Values
     */
    public void setValues(List value) {
        values = value;
    }

    /**
     * Get the Values property.
     *
     * @return The Values
     */
    public List getValues() {
        return values;
    }


    /**
     * Set the Dflt property.
     *
     * @param value The new value for Dflt
     */
    public void setDefault(String value) {
        dflt = value;
    }

    /**
     * Get the Dflt property.
     *
     * @return The Dflt
     */
    public String getDefault() {
        return dflt;
    }

    /**
     * Set the Thumbnail property.
     *
     * @param value The new value for Thumbnail
     */
    public void setThumbnail(boolean value) {
        this.thumbnail = value;
    }

    /**
     * Get the Thumbnail property.
     *
     * @return The Thumbnail
     */
    public boolean getThumbnail() {
        return this.thumbnail;
    }

    /**
     * Set the Index property.
     *
     * @param value The new value for Index
     */
    public void setIndex(int value) {
        this.index = value;
    }

    /**
     * Get the Index property.
     *
     * @return The Index
     */
    public int getIndex() {
        return this.index;
    }


}

