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



package ucar.unidata.repository.metadata;
import ucar.unidata.repository.*;


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
public class MetadataType extends MetadataTypeBase {

    /** _more_ */
    public static final String TAG_TYPE = "type";


    /** _more_ */
    public static final String TAG_HANDLER = "handler";


    /** _more_ */
    public static final String ATTR_CLASS = "class";



    /** _more_ */
    public static final String ATTR_HANDLER = "handler";


    /** _more_ */
    public static final String ATTR_ADMINONLY = "adminonly";

    /** _more_ */
    public static final String ATTR_FORUSER = "foruser";

    /** _more_ */
    public static final String ATTR_DISPLAYCATEGORY = "displaycategory";

    /** _more_ */
    public static final String ATTR_CATEGORY = "category";


    /** _more_ */
    public static final String ATTR_BROWSABLE = "browsable";



    /** _more_ */
    public static final String ATTR_ = "";



    /** _more_ */
    public static String ARG_TYPE = "type";


    /** _more_ */
    public static String ARG_METADATAID = "metadataid";

    /** _more_ */
    private String type;


    /** _more_ */
    private String displayCategory = "Metadata";

    /** _more_ */
    private String category = "Metadata";


    /** _more_ */
    private boolean adminOnly = false;


    /** _more_ */
    private boolean browsable = false;

    /** _more_ */
    private boolean forUser = true;

    /**
     * _more_
     *
     * @param type _more_
     * @param name _more_
     */
    public MetadataType(String type,MetadataHandler handler) {
        super(handler);
        this.type = type;
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
     * _more_
     *
     * @param root _more_
     * @param repository _more_
     * @param manager _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<MetadataType> parse(Element root,
                                           MetadataManager manager)
            throws Exception {
        List<MetadataType> types = new ArrayList<MetadataType>();
        parse(root, manager, types);
        return types;
    }

    /**
     * _more_
     *
     * @param root _more_
     * @param manager _more_
     * @param types _more_
     *
     * @throws Exception _more_
     */
    private static void parse(Element root, MetadataManager manager,
                              List<MetadataType> types)
            throws Exception {

        NodeList children = XmlUtil.getElements(root);
        for (int i = 0; i < children.getLength(); i++) {
            Element node = (Element) children.item(i);
            if (node.getTagName().equals(TAG_HANDLER)) {
                parse(node, manager, types);
                continue;
            }

            Class c = Misc.findClass(XmlUtil.getAttributeFromTree(node,
                          ATTR_CLASS,
                          "ucar.unidata.repository.metadata.MetadataHandler"));

            MetadataHandler handler = manager.getHandler(c);
            String          type    = XmlUtil.getAttribute(node, ATTR_TYPE);
            MetadataType metadataType = new MetadataType(type,handler);
            metadataType.init(node);
            handler.addMetadataType(metadataType);
            types.add(metadataType);
        }
    }


    public void init(Element node) throws Exception {
        super.init(node);
        setAdminOnly(XmlUtil.getAttributeFromTree(node,
                                                  ATTR_ADMINONLY, false));
        setForUser(XmlUtil.getAttributeFromTree(node,
                                                ATTR_FORUSER, true));

        setBrowsable(XmlUtil.getAttributeFromTree(node,
                                                  ATTR_BROWSABLE, false));
        setDisplayCategory(
                           XmlUtil.getAttributeFromTree(
                                                        node, ATTR_DISPLAYCATEGORY, "Metadata"));
        setCategory(XmlUtil.getAttributeFromTree(node,
                                                 ATTR_CATEGORY, handler.getHandlerGroupName()));
    }





    /**
     * _more_
     *
     * @param metadata _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void newEntry(Metadata metadata, Entry entry) throws Exception {
        for (MetadataElement element : getChildren()) {
            if (element.getDataType().equals(element.TYPE_FILE)) {
                String fileArg = metadata.getAttr(element.getIndex());
                if ((fileArg == null) || (fileArg.length() == 0)) {
                    continue;
                }
                if ( !entry.getIsLocalFile()) {
                    fileArg =
                        getStorageManager()
                            .copyToEntryDir(entry, new File(fileArg))
                            .getName();
                }
                metadata.setAttr(element.getIndex(), fileArg);
            }
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param node _more_
     * @param metadata _more_
     * @param fileMap _more_
     * @param internal _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean processMetadataXml(Entry entry, Element node,
                                      Metadata metadata, Hashtable fileMap,
                                      boolean internal)
            throws Exception {
        for (MetadataElement element : getChildren()) {
            if ( !element.getDataType().equals(element.TYPE_FILE)) {
                continue;
            }
            String fileArg = XmlUtil.getAttribute(node,
                                 ATTR_ATTR + element.getIndex(), "");
            String fileName = null;
            if (internal) {
                fileName = fileArg;
            } else {
                String tmpFile = (String) fileMap.get(fileArg);
                if (tmpFile == null) {
                    handler.getRepository().getLogManager().logError(
                        "No attachment uploaded file:" + fileArg);
                    handler.getRepository().getLogManager().logError(
                        "available files: " + fileMap);
                    return false;
                }
                File file = new File(tmpFile);
                fileName =
                    getStorageManager().copyToEntryDir(entry, file).getName();
            }

            metadata.setAttr(element.getIndex(), fileName);
        }
        return true;

    }



    /**
     *  _more_
     *
     *  @param request _more_
     *  @param entry _more_
     *  @param id _more_
     * @param metadata _more_
     *  @param suffix _more_
     *  @param metadataList _more_
     * @param oldMetadata _more_
     *  @param newMetadata _more_
     *
     *
     * @return _more_
     *  @throws Exception _more_
     */
    public Metadata handleForm(Request request, Entry entry, String id,
                               String suffix, Metadata oldMetadata,
                               boolean newMetadata)
            throws Exception {
        boolean inherited = request.get(ARG_METADATA_INHERITED + suffix,
                                        false);
        Metadata metadata = new Metadata(id, entry.getId(), getType(),
                                         inherited);
        for (MetadataElement element : getChildren()) {
            String value =  element.handleForm(request,  entry, metadata, oldMetadata,
                                               suffix);
            metadata.setAttr(element.getIndex(), value);
        }
        return metadata;
    }






    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param doc _more_
     * @param datasetNode _more_
     *
     * @throws Exception _more_
     */
    public void addMetadataToCatalog(Request request, Entry entry,
                                     Metadata metadata, Document doc,
                                     Element datasetNode)
            throws Exception {
        for (MetadataElement element : getChildren()) {
            if ( !element.getDataType().equals(element.TYPE_FILE)) {
                continue;
            }
            File f = getFile(entry, metadata, element);
            if (f == null) {
                continue;
            }
            String tail =
                getStorageManager().getFileTail(f.toString());
            String path =
                handler.getRepository().getMetadataManager().URL_METADATA_VIEW
                    .getFullUrl("/" + tail);
            String url = HtmlUtil.url(path, ARG_ELEMENT,
                                      element.getIndex() + "", ARG_ENTRYID,
                                      metadata.getEntryId(), ARG_METADATA_ID,
                                      metadata.getId());
            XmlUtil.create(
                doc,
                ThreddsMetadataHandler.getTag(
                    ThreddsMetadataHandler.TYPE_PROPERTY), datasetNode,
                        new String[] { ThreddsMetadataHandler.ATTR_NAME,
                                       (element.getThumbnail()
                                        ? "thumbnail"
                                        : "attachment"), ThreddsMetadataHandler
                                            .ATTR_VALUE, url });

        }



        String template = getTemplate(TEMPLATETYPE_THREDDSCATALOG);
        if ((template == null) || (template.length() == 0)) {
            return;
        }
        template = template.replace("${root}",
                                    handler.getRepository().getUrlBase());

        for (MetadataElement element : getChildren()) {
            template = template.replace("${attr" + element.getIndex() + "}",
                                        metadata.getAttr(element.getIndex()));
            template = template.replace("${attr" + element.getIndex() + ".cdata}",
                                        "[CDATA[" + metadata.getAttr(element.getIndex())
                                        + "]]");
        }
        template = "<tmp>" + template + "</tmp>";
        Element root =
            XmlUtil.getRoot(new ByteArrayInputStream(template.getBytes()));
        NodeList children = XmlUtil.getElements(root);
        for (int i = 0; i < children.getLength(); i++) {
            Element node = (Element) children.item(i);
            node = (Element) doc.importNode(node, true);
            datasetNode.appendChild(node);
        }
    }







    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param metadata _more_
     * @param forLink _more_
     *
     * @throws Exception _more_
     */
    public void decorateEntry(Request request, Entry entry, StringBuffer sb,
                              Metadata metadata, boolean forLink)
            throws Exception {
        for (MetadataElement element : getChildren()) {
            if ( !element.getDataType().equals(element.TYPE_FILE)) {
                continue;
            }
            if (element.getThumbnail()) {
                String html = getFileHtml(request, entry, metadata, element,
                                          forLink);
                if (html != null) {
                    sb.append(HtmlUtil.space(1));
                    sb.append(html);
                    sb.append(HtmlUtil.space(1));
                }
                continue;
            }
            if ( !forLink) {
                String html = getFileHtml(request, entry, metadata, element,
                                          false);
                if (html != null) {
                    sb.append(HtmlUtil.space(1));
                    sb.append(html);
                    sb.append(HtmlUtil.space(1));
                }
            }
        }
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processView(Request request, Entry entry, Metadata metadata)
            throws Exception {
        int elementIndex = request.get(ARG_ELEMENT, 0) - 1;
        if ((elementIndex < 0) || (elementIndex >= getChildren().size())) {
            return new Result("", "Cannot process view");
        }
        MetadataElement element = getChildren().get(elementIndex);
        if ( !element.getDataType().equals(element.TYPE_FILE)) {
            return new Result("", "Cannot process view");
        }
        File f = getFile(entry, metadata, element);
        if (f == null) {
            return new Result("", "File does not exist");
        }
        String mimeType = handler.getRepository().getMimeTypeFromSuffix(
                              IOUtil.getFileExtension(f.toString()));
        if (request.get(ARG_THUMBNAIL, false)) {
            File thumb = getStorageManager().getTmpFile(request,
                             IOUtil.getFileTail(f.toString()));
            if ( !thumb.exists()) {
                Image image = ImageUtils.readImage(f.toString());
                image = ImageUtils.resize(image, 100, -1);
                ImageUtils.waitOnImage(image);
                ImageUtils.writeImageToFile(image, thumb.toString());
            }
            f = thumb;
        }

        Result result = new Result("thumbnail",
                                   IOUtil.readBytes(new FileInputStream(f),
                                       null, true), mimeType);
        result.setShouldDecorate(false);
        return result;
    }






    /**
     * _more_
     *
     * @param request _more_
     * @param metadata _more_
     *
     * @return _more_
     */
    public String getSearchUrl(Request request, Metadata metadata) {
        if ( !getSearchable()) {
            return null;
        }

        List args = new ArrayList();
        args.add(ARG_METADATA_TYPE + "." + getType());
        args.add(this.toString());


        for (MetadataElement element : getChildren()) {
            if ( !element.getSearchable()) {
                continue;
            }
            args.add(ARG_METADATA_ATTR + element.getIndex() + "."
                     + getType());
            args.add(metadata.getAttr(element.getIndex()));
        }

        //by default search on attr1 if none are set above
        if (args.size() == 2) {
            args.add(ARG_METADATA_ATTR1 + "." + getType());
            args.add(metadata.getAttr1());
        }

        return HtmlUtil.url(
            request.url(handler.getRepository().URL_ENTRY_SEARCH), args);
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
    public String[] getHtml(Request request,
                            Entry entry, Metadata metadata) throws Exception {
        if ( !getShowInHtml()) {
            return null;
        }

        StringBuffer content = new StringBuffer();
        if (getSearchable()) {
            content.append(handler.getSearchLink(request, metadata));
        }

        String nameString = getName();

        for (MetadataElement element : getChildren()) {
            String value = metadata.getAttr(element.getIndex());
            if (value == null) {
                value = "";
            }
            nameString = nameString.replace("${attr" + element.getIndex() + "}", value);
        }


        String lbl          = msgLabel(nameString);
        String htmlTemplate = getTemplate(TEMPLATETYPE_HTML);
        if (htmlTemplate != null) {
            String html = htmlTemplate;
            for (MetadataElement element : getChildren()) {
                String value = metadata.getAttr(element.getIndex());
                if (value == null) {
                    value = "null";
                }
                html = html.replace("${attr" + element.getIndex() + "}", value);
            }
            content.append(html);
        } else {
            int     cnt    = 1;
            boolean didOne = false;
            for (MetadataElement element : getChildren()) {
                if (element.getHtml(content,
                                    metadata.getAttr(cnt))) {
                    didOne = true;
                }
                cnt++;
            }
            if ( !didOne) {
                return null;
            }
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
                            Entry entry, Metadata metadata, String suffix,boolean forEdit)
            throws Exception {

        String lbl    = msgLabel(getName());
        String submit = HtmlUtil.submit(msg("Add")
                                        + HtmlUtil.space(1) + getName());
        String cancel = HtmlUtil.submit(msg("Cancel"), ARG_CANCEL);


        StringBuffer sb  = new StringBuffer();

        if(!forEdit) 
            sb.append(header("Add: "+ getName()));
        sb.append(HtmlUtil.br());
        String lastGroup = null;
        for (MetadataElement element : getChildren()) {
            if(element.getGroup()!=null && !Misc.equals(element.getGroup(),lastGroup)) {
                lastGroup = element.getGroup();
                sb.append(HtmlUtil.row(HtmlUtil.colspan(header(lastGroup),2)));
            }
            String elementLbl = msgLabel(element.getLabel());
            String widget = element.getForm(request, entry,  metadata,
                                            suffix, 
                                            metadata.getAttr(element.getIndex()), forEdit);
            if ((widget == null) || (widget.length() == 0)) {}
            else {
                sb.append(HtmlUtil.formEntryTop(elementLbl, widget));
            }
        }

        sb.append(HtmlUtil.formEntry(msgLabel("Inherited"),
                                     HtmlUtil.checkbox(ARG_METADATA_INHERITED
                                         + suffix, "true",
                                             metadata.getInherited())));



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
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public boolean isType(String type) {
        return Misc.equals(this.type, type);
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
     *  Set the Category property.
     *
     *  @param value The new value for Category
     */
    public void setCategory(String value) {
        category = value;
    }

    /**
     *  Get the Category property.
     *
     *  @return The Category
     */
    public String getCategory() {
        return category;
    }





    /**
     *  Set the DisplayCategory property.
     *
     *  @param value The new value for DisplayCategory
     */
    public void setDisplayCategory(String value) {
        this.displayCategory = value;
    }

    /**
     *  Get the DisplayCategory property.
     *
     *  @return The DisplayCategory
     */
    public String getDisplayCategory() {
        return this.displayCategory;
    }

    /**
     *  Set the AdminOnly property.
     *
     *  @param value The new value for AdminOnly
     */
    public void setAdminOnly(boolean value) {
        this.adminOnly = value;
    }

    /**
     *  Get the AdminOnly property.
     *
     *  @return The AdminOnly
     */
    public boolean getAdminOnly() {
        return this.adminOnly;
    }



    /**
     * Set the Browsable property.
     *
     * @param value The new value for Browsable
     */
    public void setBrowsable(boolean value) {
        this.browsable = value;
    }

    /**
     * Get the Browsable property.
     *
     * @return The Browsable
     */
    public boolean getBrowsable() {
        return this.browsable;
    }

    /**
     * Set the ForUser property.
     *
     * @param value The new value for ForUser
     */
    public void setForUser(boolean value) {
        this.forUser = value;
    }

    /**
     * Get the ForUser property.
     *
     * @return The ForUser
     */
    public boolean getForUser() {
        return this.forUser;
    }



}

