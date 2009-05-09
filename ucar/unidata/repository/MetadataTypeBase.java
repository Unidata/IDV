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

import ucar.unidata.repository.data.ThreddsMetadataHandler;


import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import java.awt.Image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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
public class MetadataTypeBase extends RepositoryManager {

    /** _more_ */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String TAG_ELEMENT = "element";

    /** _more_ */
    public static final String TAG_TEMPLATE = "template";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_SEARCHABLE = "searchable";



    /** _more_ */
    public static final String ATTR_SHOWINHTML = "showinhtml";


    /** _more_ */
    public static final String TEMPLATETYPE_THREDDSCATALOG = "threddscatalog";

    /** _more_          */
    public static final String TEMPLATETYPE_HTML = "html";

    /** _more_ */
    private String name;

    /** _more_ */
    private boolean showInHtml = true;


    /** _more_ */
    List<MetadataElement> children= new ArrayList<MetadataElement>();


    /** _more_          */
    private Hashtable<String, String> templates = new Hashtable<String,
                                                      String>();


    MetadataHandler handler;

    /** _more_ */
    private boolean searchable = false;

    /**
     * _more_
     *
     * @param type _more_
     * @param name _more_
     */
    public MetadataTypeBase(MetadataHandler handler) {
        super(handler.getRepository());
        this.handler = handler;
    }


    /**
     *  Set the Handler property.
     *
     *  @param value The new value for Handler
     */
    public void setHandler(MetadataHandler value) {
        this.handler = value;
    }

    /**
     *  Get the Handler property.
     *
     *  @return The Handler
     */
    public MetadataHandler getHandler() {
        return this.handler;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasElements() {
        return getChildren().size() > 0;
    }


    public void init(Element node) throws Exception {
        setName(XmlUtil.getAttribute(node,
                                     ATTR_NAME, ""));
        setShowInHtml(XmlUtil.getAttribute(node,
                                           ATTR_SHOWINHTML, true));
        setSearchable(XmlUtil.getAttributeFromTree(node,
                                                   ATTR_SEARCHABLE, false));
        List templateElements = XmlUtil.findChildren(node, TAG_TEMPLATE);
        for (int j = 0; j < templateElements.size(); j++) {
            Element templateNode = (Element) templateElements.get(j);
            templates.put(XmlUtil.getAttribute(templateNode,
                                               ATTR_TYPE), XmlUtil.getChildText(templateNode));
        }

        List childrenElements = XmlUtil.findChildren(node, TAG_ELEMENT);
        int lastIndex = 0;
        for (int j = 0; j < childrenElements.size(); j++) {
            Element elementNode = (Element) childrenElements.get(j);
            int index = lastIndex+1;
            if(XmlUtil.hasAttribute(elementNode, MetadataElement.ATTR_INDEX)) {
                index = XmlUtil.getAttribute(elementNode, MetadataElement.ATTR_INDEX, index);
            }
            lastIndex = index;
            MetadataElement element =
                new MetadataElement(getHandler(), this, lastIndex, elementNode);
            addElement(element);
        }


    }


    public List<MetadataElement> getChildren() {
        return children;
    }

    /**
     * _more_
     *
     * @param element _more_
     */
    public void addElement(MetadataElement element) {
        getChildren().add(element);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param element _more_
     * @param forLink _more_
     *
     * @return _more_
     */
    public String getFileHtml(Request request, Entry entry,
                              Metadata metadata, MetadataElement element,
                              boolean forLink) {
        File f = getFile(entry, metadata, element);
        if (f == null) {
            return null;
        }

        String extra = (forLink
                        ? " "
                        : "");
        String tail  = getStorageManager().getFileTail(f.toString());
        String path =
            handler.getRepository().getMetadataManager().URL_METADATA_VIEW
            + "/" + tail;


        if (ImageUtils.isImage(f.toString())) {
            String img = HtmlUtil.img(HtmlUtil.url(path, ARG_ELEMENT,
                             element.getIndex() + "", ARG_ENTRYID,
                             metadata.getEntryId(), ARG_METADATA_ID,
                             metadata.getId(), ARG_THUMBNAIL,
                             "" + forLink), msg("Click to enlarge"),
                                            extra);

            if (forLink) {
                String bigimg = HtmlUtil.img(HtmlUtil.url(path, ARG_ELEMENT,
                                    element.getIndex() + "", ARG_ENTRYID,
                                    metadata.getEntryId(), ARG_METADATA_ID,
                                    metadata.getId()), "thumbnail", "");


                String imgUrl = HtmlUtil.url(path, ARG_ELEMENT,
                                             element.getIndex() + "",
                                             ARG_ENTRYID,
                                             metadata.getEntryId(),
                                             ARG_METADATA_ID,
                                             metadata.getId());


                //                System.err.println(imgUrl);
                //img =  HtmlUtil.href(imgUrl,img," dojoType=\"dojox.image.Lightbox\" ");
                img = handler.getRepository().makePopupLink(img, bigimg,
                        true, false);
            }
            return img;
        } else if (f.exists()) {
            String name =
                getStorageManager().getFileTail(f.getName());
            return HtmlUtil.href(HtmlUtil.url(path, ARG_ELEMENT,
                    element.getIndex() + "", ARG_ENTRYID,
                    metadata.getEntryId(), ARG_METADATA_ID,
                    metadata.getId()), name);
        }
        return "";
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param metadata _more_
     * @param element _more_
     *
     * @return _more_
     */
    public File getFile(Entry entry, Metadata metadata,
                         MetadataElement element) {
        File f;
        if ( !entry.getIsLocalFile()) {
            f = new File(
                IOUtil.joinDir(
                    getStorageManager().getEntryDir(
                        metadata.getEntryId(), false), metadata.getAttr(
                        element.getIndex())));
        } else {
            f = new File(metadata.getAttr(element.getIndex()));
        }
        if ( !f.exists()) {
            return null;
        }
        return f;
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

    /**
     * _more_
     *
     * @return _more_
     */
    public String getLabel() {
        return name;
    }

    /**
     *  Set the ShowInHtml property.
     *
     *  @param value The new value for ShowInHtml
     */
    public void setShowInHtml(boolean value) {
        this.showInHtml = value;
    }

    /**
     *  Get the ShowInHtml property.
     *
     *  @return The ShowInHtml
     */
    public boolean getShowInHtml() {
        return this.showInHtml;
    }

    /**
     * Get the CatalogTemplate property.
     *
     *
     * @param type _more_
     * @return The CatalogTemplate
     */
    public String getTemplate(String type) {
        return templates.get(type);
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


}

