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




package ucar.unidata.repository.metadata;
import ucar.unidata.repository.*;


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
public class MetadataElement extends MetadataTypeBase {

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

    public static final String ATTR_MAX = "max";

    public static final String ATTR_ROWS = "rows";
    /** _more_ */
    public static final String ATTR_COLUMNS = "columns";

    public static final String ATTR_DEPENDS = "depends";

    /** _more_ */
    public static final String ATTR_DATATYPE = "datatype";
    public static final String ATTR_GROUP = "group";


    public static final String ATTR_SUBNAME = "subname";

    /** _more_ */
    public static final String ATTR_DEFAULT = "default";

    /** _more_ */
    public static final String ATTR_VALUES = "values";


    public static final String ATTR_SEARCHABLE = "searchable";

    /** _more_ */
    public static final String ATTR_THUMBNAIL = "thumbnail";

    /** _more_ */
    public static final String ATTR_INDEX = "index";

    /** _more_ */
    private String dataType = TYPE_STRING;


    private String subName = "";

    private int max = -1;

    /** _more_ */
    private int rows = 1;

    /** _more_ */
    private int columns = 60;

    /** _more_ */
    private List values;

    private Hashtable<String,String> valueMap = new Hashtable<String,String>();

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
    private MetadataTypeBase parent;

    private String group;

    public MetadataElement(MetadataHandler handler, MetadataTypeBase parent, int index, Element node) throws Exception {
        super(handler);
        this.parent = parent;
        this.index        = index;
        init(node);
    }


    public void init(Element node) throws Exception {
        super.init(node);
        subName = XmlUtil.getAttribute(node, ATTR_SUBNAME,"");
        max = XmlUtil.getAttribute(node, ATTR_MAX,max);
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
                                          ATTR_REQUIRED, false);
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
                    valueMap.put(tok,tok);
                    enumValues.add(tok);
                    continue;
                }
                String[] toks = StringUtil.split(tok, ":", 2);
                if (toks == null) {
                    valueMap.put(tok,tok);
                    enumValues.add(tok);
                    continue;
                }
                valueMap.put(toks[0],toks[1]);
                enumValues.add(new TwoFacedObject(toks[1], toks[0]));
            }
            enumValues.add(0,"");
            setValues(enumValues);
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
    public boolean getHtml(StringBuffer sb,
                           String value) throws Exception {
        if (dataType.equals(TYPE_SKIP)) {
            return false;
        }
        if (dataType.equals(TYPE_FILE)) {
            return false;
        }
        String html =null;
        if (getDataType().equals(TYPE_GROUP)) {
            StringBuffer entriesSB =  new StringBuffer();
            entriesSB.append("<table border=0 width=100% cellpadding=2 cellspacing=2>");
            List<Hashtable<Integer,String>> entries =(List<Hashtable<Integer,String>>)
                (value!=null && value.length()>0?XmlEncoder.decodeXml(value):null);
            if(entries==null) return false;
            boolean justOne = getChildren().size()==1;
            for(Hashtable<Integer,String> map: entries) {
                if(subName.length()>0) {
                    entriesSB.append("<tr valign=\"top\"><td align=center colspan=2><b>" + subName+"</td></tr>");
                }
                for(MetadataElement element: getChildren()) {
                    String subValue = map.get(new Integer(element.getIndex()));
                    if(subValue==null) continue;
                    entriesSB.append("<tr valign=\"top\"><td></td><td>\n");
                    //                    entriesSB.append("<table width=100% cellpadding=0 cellspacing=0>");
                    element.getHtml(entriesSB, subValue);
                    //                    entriesSB.append("</table>");
                    entriesSB.append("</td></tr>\n");
                }
                if(!justOne) {
                    entriesSB.append("<tr><td colspan=2><hr></td></tr>\n");
                }
            }
            entriesSB.append("</table>");
            html = HtmlUtil.makeToggleInline("",
                                             HtmlUtil.div(entriesSB.toString(),HtmlUtil.cssClass("metadatagroup")),true);

        } else if(dataType.equals(TYPE_ENUMERATION) ||
                  dataType.equals(TYPE_ENUMERATIONPLUS)) {
            String label = valueMap.get(value);
            if(label==null) {
                label = value;
            }
            html = label;
        } else if (dataType.equals(TYPE_EMAIL)) {
            html =HtmlUtil.href("mailto:" + value, value);
        } else if (dataType.equals(TYPE_URL)) {
            html = HtmlUtil.href(value, value);
        } else {
            html=value;
        }
        if(html!=null) {
            String name = getName();
            if(name.length()>0) 
                name = msgLabel(name);
            else 
                name=HtmlUtil.space(1);
            sb.append(HtmlUtil.formEntryTop(name,html));
            return true;
        }
        return false;
    }

    public boolean isGroup() {
        return getDataType().equals(TYPE_GROUP);
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
    public String handleForm(Request request,  Entry entry,
                           Metadata newMetadata, Metadata oldMetadata,
                           String suffix)
            throws Exception {

        String arg = ARG_METADATA_ATTR + getIndex() + suffix;

        if (getDataType().equals(TYPE_BOOLEAN)) {
            boolean value = request.get(arg, false);
            return ""+value;
        }


        if (getDataType().equals(TYPE_GROUP)) {
            List<Hashtable<Integer,String>> entries = new ArrayList<Hashtable<Integer,String>>();
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
                    for(MetadataElement element: getChildren()) {
                        String subValue = element.handleForm(request, entry, newMetadata, oldMetadata, subArg);
                        map.put(new Integer(element.getIndex()), subValue);
                    }
                    entries.add(map);
                }
                groupCnt++;
            }
            return XmlEncoder.encodeObject(entries);
        }


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



        String oldValue = (oldMetadata==null?"":oldMetadata.getAttr(getIndex()));


        String url     = request.getString(arg + ".url", "");
        String theFile = null;
        if (url.length() > 0) {
            String tail = IOUtil.getFileTail(url);
            File tmpFile =
                getStorageManager().getTmpFile(request,
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
        theFile =   getStorageManager().moveToEntryDir(entry, new File(theFile)).getName();
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
                HtmlUtil.space(2) +
                msgLabel("Or") +
                HtmlUtil.input(arg+".input", (contains?"":value),HtmlUtil.SIZE_30);
        } else if (dataType.equals(TYPE_FILE)) {
            String image = (forEdit
                            ? getFileHtml(request, entry,
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
            List<Hashtable<Integer,String>> entries =(List<Hashtable<Integer,String>>)
                (value!=null && value.length()>0?XmlEncoder.decodeXml(value):null);
            if(entries==null) {
                entries = new ArrayList<Hashtable<Integer,String>>();
            }
            entries.add(new Hashtable<Integer,String>());

            StringBuffer sb  = new StringBuffer();
            String lastGroup = null;

            int groupCnt = 0;
            StringBuffer entriesSB = new StringBuffer();
            for(Hashtable<Integer,String> map: entries) {
                StringBuffer groupSB = new StringBuffer();
                groupSB.append(HtmlUtil.formTable());
                String subArg = arg+".group" +groupCnt+".";
                for(MetadataElement element: getChildren()) {
                    if(element.getGroup()!=null && !Misc.equals(element.getGroup(),lastGroup)) {
                        lastGroup = element.getGroup();
                        groupSB.append(HtmlUtil.row(HtmlUtil.colspan(header(lastGroup),2)));
                    }

                    String elementLbl = element.getName();
                    if(elementLbl.length()>0) {
                        elementLbl = msgLabel(elementLbl);
                    }
                    String subValue  = map.get(new Integer(element.getIndex()));
                    if(subValue == null) subValue = "";
                    String widget = element.getForm(request, entry,   metadata,
                                                    subArg, 
                                                    subValue, forEdit);
                    if ((widget == null) || (widget.length() == 0)) {continue;}
                    groupSB.append(HtmlUtil.formEntryTop(elementLbl, widget));
                    groupSB.append(HtmlUtil.hidden(subArg+".group","true"));
                }
                
                if(entries.size()>1 && groupCnt==entries.size()-1) {
                    String newCbx =HtmlUtil.checkbox(subArg+".new","true",false)+" " + msg("Create New")+
                        HtmlUtil.hidden(subArg+".lastone","true");
                    groupSB.append(HtmlUtil.formEntry("",newCbx));
                } else if(entries.size()>1) {
                    groupSB.append(HtmlUtil.formEntry("",HtmlUtil.checkbox(subArg+".delete","true",false)+" " + msg("Delete")));
                } 
                groupSB.append(HtmlUtil.formTableClose());

                if(entries.size()>1 && groupCnt==entries.size()-1) {
                    entriesSB.append(HtmlUtil.makeShowHideBlock("New " + subName,groupSB.toString(),false));
                } else {
                    if(entries.size()>1) {
                    } 
                    entriesSB.append(HtmlUtil.makeShowHideBlock(subName,groupSB.toString(),true));
                }

                groupCnt++;
            }
            sb.append(HtmlUtil.makeToggleInline("", HtmlUtil.div(entriesSB.toString(),HtmlUtil.cssClass("metadatagroup")),true));
            return sb.toString();
        } else {
            System.err.println("Unknown data type:" + dataType);
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

