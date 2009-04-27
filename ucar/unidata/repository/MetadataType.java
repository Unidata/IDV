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
public class MetadataType implements Constants {

    /** _more_          */
    public static final String TAG_TYPE = "type";

    /** _more_          */
    public static final String TAG_ELEMENT = "element";

    /** _more_          */
    public static final String TAG_HTMLTEMPLATE = "htmltemplate";

    /** _more_          */
    public static final String TAG_CATALOGTEMPLATE = "catalogtemplate";

    /** _more_          */
    public static final String TAG_HANDLER = "handler";

    /** _more_          */
    public static final String ATTR_ROWS = "rows";

    /** _more_          */
    public static final String ATTR_CLASS = "class";

    /** _more_          */
    public static final String ATTR_COLUMNS = "columns";

    /** _more_          */
    public static final String ATTR_SHOWINHTML = "showinhtml";

    /** _more_          */
    public static final String ATTR_HANDLER = "handler";

    /** _more_          */
    public static final String ATTR_TYPE = "type";

    /** _more_          */
    public static final String ATTR_ADMINONLY = "adminonly";

    /** _more_          */
    public static final String ATTR_FORUSER = "foruser";

    /** _more_          */
    public static final String ATTR_LABEL = "label";

    /** _more_          */
    public static final String ATTR_DEFAULT = "default";

    /** _more_          */
    public static final String ATTR_VALUES = "values";

    /** _more_          */
    public static final String ATTR_NAME = "name";

    /** _more_          */
    public static final String ATTR_DISPLAYCATEGORY = "displaycategory";

    /** _more_          */
    public static final String ATTR_CATEGORY = "category";

    /** _more_          */
    public static final String ATTR_SEARCHABLE = "searchable";

    /** _more_          */
    public static final String ATTR_BROWSABLE = "browsable";

    /** _more_          */
    public static final String ATTR_THUMBNAIL = "thumbnail";

    /** _more_          */
    public static final String ATTR_ = "";



    /** _more_ */
    public static String ARG_TYPE = "type";

    /** _more_          */
    public static String ARG_ATTR = "attr";


    /** _more_ */
    public static String ARG_ATTR1 = "attr1";

    /** _more_ */
    public static String ARG_ATTR2 = "attr2";

    /** _more_ */
    public static String ARG_ATTR3 = "attr3";

    /** _more_ */
    public static String ARG_ATTR4 = "attr4";


    /** _more_ */
    public static String ARG_METADATAID = "metadataid";

    /** _more_ */
    private String type;

    /** _more_ */
    private String name;

    /** _more_          */
    private String displayCategory = "Metadata";

    /** _more_          */
    private String category = "Metadata";

    /** _more_          */
    private boolean showInHtml = true;

    /** _more_          */
    private boolean adminOnly = false;

    /** _more_          */
    private boolean searchable = false;

    /** _more_          */
    private boolean browsable = false;

    /** _more_          */
    private boolean forUser = true;

    /** _more_ */
    private List<MetadataElement> elements = new ArrayList<MetadataElement>();

    /** _more_          */
    private MetadataHandler handler;

    /** _more_          */
    private String htmlTemplate;

    /** _more_          */
    private String catalogTemplate;

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
        this.type = type;
        this.name = name;
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
     * @return _more_
     */
    public boolean hasElements() {
        return elements.size() > 0;
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
                          "ucar.unidata.repository.MetadataHandler"));

            MetadataHandler handler = manager.getHandler(c);
            String          type    = XmlUtil.getAttribute(node, ATTR_TYPE);
            MetadataType metadataType = new MetadataType(type,
                                            XmlUtil.getAttribute(node,
                                                ATTR_NAME, type));
            metadataType.htmlTemplate = XmlUtil.getGrandChildText(node,
                    TAG_HTMLTEMPLATE);
            metadataType.catalogTemplate = XmlUtil.getGrandChildText(node,
                    TAG_CATALOGTEMPLATE);


            handler.addMetadataType(metadataType);
            types.add(metadataType);
            metadataType.setAdminOnly(XmlUtil.getAttributeFromTree(node,
                    ATTR_ADMINONLY, false));
            metadataType.setForUser(XmlUtil.getAttributeFromTree(node,
                    ATTR_FORUSER, true));
            metadataType.setSearchable(XmlUtil.getAttributeFromTree(node,
                    ATTR_SEARCHABLE, false));
            metadataType.setBrowsable(XmlUtil.getAttributeFromTree(node,
                    ATTR_BROWSABLE, false));
            metadataType.setDisplayCategory(
                XmlUtil.getAttributeFromTree(
                    node, ATTR_DISPLAYCATEGORY, "Metadata"));
            metadataType.setCategory(XmlUtil.getAttributeFromTree(node,
                    ATTR_CATEGORY, handler.getHandlerGroupName()));

            List elements = XmlUtil.findChildren(node,
                                MetadataType.TAG_ELEMENT);

            for (int j = 0; j < elements.size(); j++) {
                //    <element type="string" label="Name"/>
                Element elementNode = (Element) elements.get(j);

                String elementType = XmlUtil.getAttribute(elementNode,
                                         ATTR_TYPE);
                String dflt = XmlUtil.getAttribute(elementNode, ATTR_DEFAULT,
                                  "");
                MetadataElement element =
                    new MetadataElement(
                        metadataType, j + 1, elementType,
                        XmlUtil.getAttribute(elementNode, ATTR_LABEL),
                        XmlUtil.getAttribute(elementNode, ATTR_ROWS, 1),
                        XmlUtil.getAttribute(elementNode, ATTR_COLUMNS, 60),
                        null);
                element.setThumbnail(XmlUtil.getAttribute(elementNode,
                        ATTR_THUMBNAIL, false));
                element.setDefault(dflt);
                if (elementType.equals(MetadataElement.TYPE_ENUMERATION)) {
                    String values = XmlUtil.getAttribute(elementNode,
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
                    element.setValues(enumValues);
                }
                metadataType.addElement(element);
            }
        }
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
        for (MetadataElement element : elements) {
            if (element.getType().equals(element.TYPE_FILE)) {
                String fileArg = metadata.getAttr(element.getIndex());
                if ((fileArg == null) || (fileArg.length() == 0)) {
                    continue;
                }
                if ( !entry.getIsLocalFile()) {
                    fileArg =
                        handler.getRepository().getStorageManager()
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
        for (MetadataElement element : elements) {
            if ( !element.getType().equals(element.TYPE_FILE)) {
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
                    handler.getRepository().getStorageManager()
                        .copyToEntryDir(entry, file).getName();
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
     *  @throws Exception _more_
     */
    public void handleForm(Request request, Entry entry, Metadata metadata,
                           String suffix, Metadata oldMetadata,
                           boolean newMetadata)
            throws Exception {
        for (MetadataElement element : elements) {
            if ( !element.getType().equals(element.TYPE_FILE)) {
                continue;
            }
            if (oldMetadata != null) {
                metadata.setAttr(element.getIndex(),
                                 oldMetadata.getAttr(element.getIndex()));
            }

            String url = request.getString(ARG_ATTR + element.getIndex()
                                           + suffix + ".url", "");
            String theFile = null;
            if (url.length() > 0) {
                String tail = IOUtil.getFileTail(url);
                File tmpFile =
                    handler.getStorageManager().getTmpFile(request, tail);
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
                String fileArg = request.getUploadedFile(ARG_ATTR
                                     + element.getIndex() + suffix);
                if (fileArg == null) {
                    continue;
                }
                theFile = fileArg;
            }
            theFile =
                handler.getRepository().getStorageManager().moveToEntryDir(
                    entry, new File(theFile)).getName();
            metadata.setAttr(element.getIndex(), theFile);
        }
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
        for (MetadataElement element : elements) {
            if ( !element.getType().equals(element.TYPE_FILE)) {
                continue;
            }
            File f = getFile(entry, metadata, element);
            if (f == null) {
                continue;
            }
            String tail =
                handler.getStorageManager().getFileTail(f.toString());
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



        String template = getCatalogTemplate();
        if ((template == null) || (template.length() == 0)) {
            return;
        }
        for (int attr = 1; attr <= 4; attr++) {
            template = template.replace("${attr" + attr + "}",
                                        metadata.getAttr(attr));
            template = template.replace("${attr" + attr + ".cdata}",
                                        "[CDATA[" + metadata.getAttr(attr)
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
     * @param entry _more_
     * @param metadata _more_
     * @param element _more_
     *
     * @return _more_
     */
    private File getFile(Entry entry, Metadata metadata,
                         MetadataElement element) {
        File f;
        if ( !entry.getIsLocalFile()) {
            f = new File(
                IOUtil.joinDir(
                    handler.getRepository().getStorageManager().getEntryDir(
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
        for (MetadataElement element : elements) {
            if ( !element.getType().equals(element.TYPE_FILE)) {
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
        if ((elementIndex < 0) || (elementIndex >= elements.size())) {
            return new Result("", "Cannot process view");
        }
        MetadataElement element = elements.get(elementIndex);
        if ( !element.getType().equals(element.TYPE_FILE)) {
            return new Result("", "Cannot process view");
        }
        File f = getFile(entry, metadata, element);
        if (f == null) {
            return new Result("", "File does not exist");
        }
        String mimeType = handler.getRepository().getMimeTypeFromSuffix(
                              IOUtil.getFileExtension(f.toString()));
        if (request.get(ARG_THUMBNAIL, false)) {
            File thumb = handler.getStorageManager().getTmpFile(request,
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
        String tail  = handler.getStorageManager().getFileTail(f.toString());
        String path =
            handler.getRepository().getMetadataManager().URL_METADATA_VIEW
            + "/" + tail;


        if (ImageUtils.isImage(f.toString())) {
            String img = HtmlUtil.img(HtmlUtil.url(path, ARG_ELEMENT,
                             element.getIndex() + "", ARG_ENTRYID,
                             metadata.getEntryId(), ARG_METADATA_ID,
                             metadata.getId(), ARG_THUMBNAIL,
                             "" + forLink), handler.msg("Click to enlarge"),
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
                handler.getRepository().getStorageManager().getFileTail(
                    f.getName());
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
     * @param handler _more_
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     */
    public String[] getHtml(MetadataHandler handler, Request request,
                            Entry entry, Metadata metadata) {
        if ( !showInHtml) {
            return null;
        }

        StringBuffer content = new StringBuffer();
        if (getBrowsable()) {
            content.append(handler.getSearchLink(request, metadata));
        }

        String lbl = handler.msgLabel(name);
        if (htmlTemplate != null) {
            String html = htmlTemplate;
            for (int attr = 1; attr <= 4; attr++) {
                html = html.replace("${attr" + attr + "}",
                                    metadata.getAttr(attr));
            }
            content.append(html);
        } else {
            int     cnt    = 1;
            boolean didOne = false;
            for (MetadataElement element : elements) {
                if (element.getHtml(handler, content,
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
            String widget = element.getForm(request, entry, metadata,
                                            args[cnt], values[cnt], forEdit);
            if ((widget == null) || (widget.length() == 0)) {}
            else {
                sb.append(HtmlUtil.formEntry(elementLbl, widget));
            }
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

    /**
     * Set the CatalogTemplate property.
     *
     * @param value The new value for CatalogTemplate
     */
    public void setCatalogTemplate(String value) {
        this.catalogTemplate = value;
    }

    /**
     * Get the CatalogTemplate property.
     *
     * @return The CatalogTemplate
     */
    public String getCatalogTemplate() {
        return this.catalogTemplate;
    }

    /**
     * Set the HtmlTemplate property.
     *
     * @param value The new value for HtmlTemplate
     */
    public void setHtmlTemplate(String value) {
        this.htmlTemplate = value;
    }

    /**
     * Get the HtmlTemplate property.
     *
     * @return The HtmlTemplate
     */
    public String getHtmlTemplate() {
        return this.htmlTemplate;
    }



}

