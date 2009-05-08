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
 * WITHOUT ANY WARRANTYP; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */





package ucar.unidata.repository;


import org.w3c.dom.*;

import ucar.unidata.xml.XmlUtil;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.io.File;
import java.io.FileOutputStream;

import java.io.InputStream;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Hashtable;
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

    /** _more_ */
    public static final String TYPE_STRING = "string";

    /** _more_ */
    public static final String TYPE_URL = "url";

    /** _more_ */
    public static final String TYPE_EMAIL = "email";

    /** _more_ */
    public static final String TYPE_FILE = "file";

    /** _more_ */
    public static final String TYPE_BOOLEAN = "boolean";

    /** _more_ */
    public static final String TYPE_ENUMERATION = "enumeration";

    public static final String TYPE_ENUMERATIONPLUS = "enumerationplus";

    public static final String TYPE_GROUP = "group";

    public static final String TYPE_DEPENDENTENUMERATION = "dependentenumeration";


    public static final String ATTR_REQUIRED = "required";

    public static final String ATTR_ROWS = "rows";
    /** _more_ */
    public static final String ATTR_COLUMNS = "columns";

    public static final String ATTR_DEPENDS = "depends";

    /** _more_ */
    public static final String ATTR_DATATYPE = "datatype";
    public static final String ATTR_GROUP = "group";

    /** _more_ */
    public static final String ATTR_LABEL = "label";

    /** _more_ */
    public static final String ATTR_DEFAULT = "default";

    /** _more_ */
    public static final String ATTR_VALUES = "values";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    public static final String ATTR_SEARCHABLE = "searchable";

    /** _more_ */
    public static final String ATTR_THUMBNAIL = "thumbnail";

    /** _more_ */
    public static final String ATTR_INDEX = "index";

    /** _more_ */
    private String dataType = TYPE_STRING;

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

    /** _more_ */
    private boolean thumbnail = false;

    private boolean required = false;

    /** _more_          */
    private boolean searchable = false;

    /** _more_ */
    private int index;

    /** _more_ */
    private MetadataType metadataType;

    private String group;

    private List<MetadataElement> children;

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
    public MetadataElement(MetadataType metadataType, int index, Element node) throws Exception {
        this.metadataType = metadataType;
        this.index        = index;
        init(node);
    }


    private void init(Element node) throws Exception {
        setLabel(XmlUtil.getAttribute(node, ATTR_LABEL));
        setRows(XmlUtil.getAttribute(node, ATTR_ROWS, 1));
        setColumns(XmlUtil.getAttribute(node, ATTR_COLUMNS, 60));
        setDataType(XmlUtil.getAttribute(node,
                                     ATTR_DATATYPE,MetadataElement.TYPE_STRING));
        setDefault(XmlUtil.getAttribute(node, ATTR_DEFAULT,
                                        ""));

        setGroup(XmlUtil.getAttribute(node, ATTR_GROUP,(String) null));
        setSearchable(XmlUtil.getAttribute(node,
                                           ATTR_SEARCHABLE, false));
        required = XmlUtil.getAttribute(node,
                                          ATTR_REQUIRED, false));
        setThumbnail(XmlUtil.getAttribute(node,
                                          ATTR_THUMBNAIL, false));
        if (dataType.equals(MetadataElement.TYPE_ENUMERATION)||
            dataType.equals(MetadataElement.TYPE_ENUMERATIONPLUS)) {
            String values = XmlUtil.getAttribute(node,
                                                 ATTR_VALUES);
            List<String> tmpValues = null;
            if (values.startsWith("file:")) {
                String tagValues =
                    IOUtil.readContents(values.substring(5),
                                        MetadataType.class);
                tmpValues =
                    (List<String>) StringUtil.split(tagValues, "\n",
                                                    true, true);
            } else {
                tmpValues = (List<String>) StringUtil.split(values,
                                                            ",", true, true);
            }

            List enumValues = new ArrayList();
            for (String tok : tmpValues) {
                int idx = tok.indexOf(":");
                if (idx < 0) {
                    enumValues.add(tok);
                    continue;
                }
                String[] toks = StringUtil.split(tok, ":", 2);
                if (toks == null) {
                    enumValues.add(tok);
                    continue;
                }
                enumValues.add(new TwoFacedObject(toks[1], toks[0]));
            }
            enumValues.add(0,"");
            setValues(enumValues);
        }


        if(dataType.equals(TYPE_GROUP)) {
            List elements = XmlUtil.findChildren(node,
                                MetadataType.TAG_ELEMENT);
            children = new ArrayList<MetadataElement>();
            int lastIndex = 0;
            for (int j = 0; j < elements.size(); j++) {
                Element elementNode = (Element) elements.get(j);
                int index = lastIndex+1;
                if(XmlUtil.hasAttribute(elementNode, ATTR_INDEX)) {
                    index = XmlUtil.getAttribute(elementNode, ATTR_INDEX, index);
                }
                lastIndex = index;
                MetadataElement element =
                    new MetadataElement(metadataType, lastIndex, elementNode);
                children.add(element);
            }
        }
    }



    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    private boolean isString(String type) {
        return dataType.equals(TYPE_STRING) || dataType.equals(TYPE_EMAIL)
               || dataType.equals(TYPE_URL);
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
        if (dataType.equals(TYPE_SKIP)) {
            return false;
        }
        if (dataType.equals(TYPE_FILE)) {
            return false;
        }
        if (dataType.equals(TYPE_EMAIL)) {
            sb.append(HtmlUtil.href("mailto:" + value, value));
        } else if (dataType.equals(TYPE_URL)) {
            sb.append(HtmlUtil.href(value, value));
        } else {
            sb.append(value);
        }
        sb.append(HtmlUtil.br());
        return true;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param type _more_
     * @param entry _more_
     * @param newMetadata _more_
     * @param oldMetadata _more_
     * @param suffix _more_
     *
     * @throws Exception _more_
     */
    public String handleForm(Request request, MetadataType type, Entry entry,
                           Metadata newMetadata, Metadata oldMetadata,
                           String suffix)
            throws Exception {

        String arg = ARG_METADATA_ATTR + getIndex() + suffix;

        if (getDataType().equals(TYPE_BOOLEAN)) {
            boolean value = request.get(arg, false);
            return ""+value;
        }


        if (getDataType().equals(TYPE_GROUP)) {
            List<Hashtable<Integer,String>> entries =   entries = new ArrayList<Hashtable<Integer,String>>();
            int groupCnt=0;
            while(true) {
                String subArg = arg+".group" +groupCnt+".";
                if(!request.exists(subArg+".group")) break;
                if(!request.get(subArg+".delete",false)) {
                    if(request.get(subArg+".lastone",false)) {
                        if(!request.get(subArg+".new",false)) {
                            groupCnt++;
                            continue;
                        }
                    }
                    Hashtable<Integer,String> map = new Hashtable<Integer,String>();
                    for(MetadataElement element: children) {
                        String subValue = element.handleForm(request, type, entry, newMetadata, oldMetadata, subArg);
                        map.put(new Integer(element.getIndex()), subValue);
                    }
                    entries.add(map);
                }
                groupCnt++;
            }
            return XmlEncoder.encodeObject(entries);
        }


        System.err.println("handle form:" + arg);

        String attr = request.getString(arg, "");
        if (request.defined(arg + ".select")) {
            attr = request.getString(arg + ".select", "");
        }
        if (request.defined(arg + ".input")) {
            attr = request.getString(arg + ".input", "");
        }

        //        newMetadata.setAttr(getIndex(), attr);

        if ( !getDataType().equals(TYPE_FILE)) {
            return attr;
        }



        String oldValue = oldMetadata.getAttr(getIndex());


        String url     = request.getString(arg + ".url", "");
        String theFile = null;
        if (url.length() > 0) {
            String tail = IOUtil.getFileTail(url);
            File tmpFile =
                type.getHandler().getStorageManager().getTmpFile(request,
                    tail);
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
                throw new IllegalArgumentException("Could not download url:"
                        + url);
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
                return oldValue;
            }
            theFile = fileArg;
        }
        theFile =
            type.getHandler().getRepository().getStorageManager()
                .moveToEntryDir(entry, new File(theFile)).getName();
        return  theFile;
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
    public String getForm(Request request, Entry entry, 
                          MetadataType metadataType,
                          Metadata metadata,
                          String suffix, String value, boolean forEdit) throws Exception {
        if (dataType.equals(TYPE_SKIP)) {
            return "";
        }
        String arg = ARG_METADATA_ATTR + getIndex() + suffix;

        value = (((value == null) || (value.length() == 0))
                 ? dflt
                 : value);
        if (isString(dataType)) {
            if (rows > 1) {
                return HtmlUtil.textArea(arg, value, rows, columns);
            }
            return HtmlUtil.input(arg, value,
                                  HtmlUtil.attr(HtmlUtil.ATTR_SIZE,
                                      "" + columns));
        } else if (dataType.equals(TYPE_BOOLEAN)) {
            return HtmlUtil.checkbox(arg, "true", Misc.equals(value, "true"));
        } else if (dataType.equals(TYPE_ENUMERATION)) {
            return HtmlUtil.select(arg, values, value);

        } else if (dataType.equals(TYPE_ENUMERATIONPLUS)) {
            boolean contains = values.contains(value);
            return HtmlUtil.select(arg, values, value) +
                HtmlUtil.br() +
                metadataType.getHandler().msgLabel("Or") +
                HtmlUtil.input(arg+".input", (contains?"":value),HtmlUtil.SIZE_30);
        } else if (dataType.equals(TYPE_FILE)) {
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
        } else if (dataType.equals(TYPE_GROUP)) {
            /**
               <collection>
                  <entry index="1">
                  blobcdata
                  </entry>
               </collection>
             **/
            List<Hashtable<Integer,String>> entries =(List<Hashtable<Integer,String>>)
                (value!=null && value.length()>0?XmlEncoder.decodeXml(value):null);
            if(entries==null) {
                entries = new ArrayList<Hashtable<Integer,String>>();
            }
            entries.add(new Hashtable<Integer,String>());

            StringBuffer sb  = new StringBuffer();
            String lastGroup = null;

            int groupCnt = 0;
            for(Hashtable<Integer,String> map: entries) {
                String subArg = arg+".group" +groupCnt+".";
                StringBuffer groupSB = new StringBuffer();
                groupSB.append(HtmlUtil.formTable());
                for(MetadataElement element: children) {
                    if(element.getGroup()!=null && !Misc.equals(element.getGroup(),lastGroup)) {
                        lastGroup = element.getGroup();
                        groupSB.append(HtmlUtil.row(HtmlUtil.colspan(metadataType.getHandler().header(lastGroup),2)));
                    }
                    String elementLbl = metadataType.getHandler().msgLabel(element.getLabel());
                    String subValue  = map.get(new Integer(element.getIndex()));
                    if(subValue == null) subValue = "";
                    String widget = element.getForm(request, entry,  metadataType, metadata,
                                                    subArg, 
                                                    subValue, forEdit);
                    if ((widget == null) || (widget.length() == 0)) {continue;}
                    groupSB.append(HtmlUtil.formEntry(elementLbl, widget));
                    groupSB.append(HtmlUtil.hidden(subArg+".group","true"));
                }
                groupSB.append(HtmlUtil.formTableClose());
                if(entries.size()>1 && groupCnt==entries.size()-1) {
                    String newCbx =HtmlUtil.checkbox(subArg+".new","true",false)+" " + metadataType.getHandler().msg("Add new")+
                        HtmlUtil.hidden(subArg+".lastone","true");

                    sb.append(HtmlUtil.formEntry("",HtmlUtil.checkbox(subArg+".new","true",false)));
                    sb.append(HtmlUtil.formEntry("",HtmlUtil.makeShowHideBlock("New",
                                                                               newCbx+HtmlUtil.br()+
                                                                               groupSB.toString(),false)));
                } else {
                    sb.append(HtmlUtil.formEntry("",HtmlUtil.checkbox(subArg+".delete","true",false)+" " + metadataType.getHandler().msg("Delete")));
                    sb.append(HtmlUtil.formEntry("",groupSB.toString()));
                }
                groupCnt++;
            }
            return sb.toString();
        } else {
            return null;
        }
    }


    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setDataType(String value) {
        dataType = value;
    }

    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public String getDataType() {
        return dataType;
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

    /**
     * Set the Searchable property.
     *
     * @param value The new value for Searchable
     */
    public void setSearchable(boolean value) {
        this.searchable = value;
    }

    /**
     * Get the Searchable property.
     *
     * @return The Searchable
     */
    public boolean getSearchable() {
        return this.searchable;
    }


    /**
       Set the Group property.

       @param value The new value for Group
    **/
    public void setGroup (String value) {
	this.group = value;
    }

    /**
       Get the Group property.

       @return The Group
    **/
    public String getGroup () {
	return this.group;
    }

}

