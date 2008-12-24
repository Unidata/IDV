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
import ucar.unidata.ui.ImageUtils;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.File;
import java.io.FileInputStream;


import java.sql.Statement;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ContentMetadataHandler extends MetadataHandler {


    /** _more_ */
    public static Metadata.Type TYPE_THUMBNAIL =
        new Metadata.Type("content.thumbnail", "Thumbnail Image");

    /** _more_ */
    public static Metadata.Type TYPE_ATTACHMENT =
        new Metadata.Type("content.attachment", "Attachment");


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public ContentMetadataHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
        addType(TYPE_THUMBNAIL);
        addType(TYPE_ATTACHMENT);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected String getHandlerGroupName() {
        return "Content";
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public void processMetadataXml(Entry entry, Element node, Hashtable fileMap,boolean internal)
            throws Exception {
        String type = XmlUtil.getAttribute(node, ATTR_TYPE);
        if (getType(type).equals(TYPE_THUMBNAIL)
                || getType(type).equals(TYPE_ATTACHMENT)) {
            String fileArg = XmlUtil.getAttribute(node, ATTR_ATTR1, "");

            String fileName =null;

            if(internal) {
                fileName = fileArg;
            } else {
                String tmpFile = (String)fileMap.get(fileArg);
                if(tmpFile==null) {
                    System.err.println ("No uploaded file:" + fileArg);
                    System.err.println ("files: " + fileMap);
                    return;
                }
                File file = new File(tmpFile);
                fileName =
                    getRepository().getStorageManager().copyToEntryDir(entry,
                                                                       file).getName();
            }

            System.err.println ("adding attachment " + fileName );
            Metadata metadata =
                new Metadata(getRepository().getGUID(), entry.getId(), type,
                             XmlUtil.getAttribute(node, ATTR_INHERITED,
                                 DFLT_INHERITED), fileName,
                             "","","");
            entry.addMetadata(metadata);
        } else {
            super.processMetadataXml(entry, node,fileMap,internal);
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
        Metadata.Type type = getType(metadata.getType());
        if (type.equals(TYPE_THUMBNAIL) || type.equals(TYPE_ATTACHMENT)) {
            String fileArg = metadata.getAttr1();
            if ( !entry.getIsLocalFile()) {
                fileArg =
                    getRepository().getStorageManager().copyToEntryDir(entry,
                        new File(fileArg)).getName();
            }
            metadata.setAttr1(fileArg);
        }
        super.newEntry(metadata, entry);
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
        Metadata.Type type = getType(metadata.getType());
        if (type.equals(TYPE_THUMBNAIL)) {
            String html = getHtml(request, entry, metadata, forLink);
            if (html != null) {
                sb.append(HtmlUtil.space(1));
                sb.append(html);
                sb.append(HtmlUtil.space(1));
            }
        }

        if ( !forLink && type.equals(TYPE_ATTACHMENT)) {
            String html = getHtml(request, entry, metadata, false);
            if (html != null) {
                sb.append(HtmlUtil.space(1));
                sb.append(html);
                sb.append(HtmlUtil.space(1));
            }
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param forLink _more_
     *
     * @return _more_
     */
    public String getHtml(Request request, Entry entry, Metadata metadata,
                          boolean forLink) {
        File f = getImageFile(entry, metadata);
        if (f == null) {
            return null;
        }

        Metadata.Type type  = getType(metadata.getType());
        String        extra = (forLink
                               ? " width=\"100\" "
                               : "");
        if (ImageUtils.isImage(f.toString())) {
            return HtmlUtil.img(
                request.url(
                    getRepository().getMetadataManager().URL_METADATA_VIEW,
                    ARG_ENTRYID, metadata.getEntryId(), ARG_METADATA_ID,
                    metadata.getId()), "thumbnail", extra);
        } else if (f.exists()) {
            String name =
                getRepository().getStorageManager().getFileTail(f.getName());
            return HtmlUtil.href(
                request.url(
                    getRepository().getMetadataManager().URL_METADATA_VIEW,
                    ARG_ENTRYID, metadata.getEntryId(), ARG_METADATA_ID,
                    metadata.getId()), name);
        }
        return "";
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     */
    public String[] getHtml(Request request, Entry entry, Metadata metadata) {
        if (true) {
            return null;
        }
        Metadata.Type type    = getType(metadata.getType());
        String        lbl     = msgLabel(type.getLabel());
        String        content = null;
        if (type.equals(TYPE_THUMBNAIL) || type.equals(TYPE_ATTACHMENT)) {
            content = getHtml(request, entry, metadata, false);
        }
        if (content == null) {
            return null;
        }
        return new String[] { lbl, content };
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     */
    private File getImageFile(Entry entry, Metadata metadata) {
        File f;
        if ( !entry.getIsLocalFile()) {
            f = new File(
                IOUtil.joinDir(
                    getRepository().getStorageManager().getEntryDir(
                        metadata.getEntryId(), false), metadata.getAttr1()));
        } else {
            f = new File(metadata.getAttr1());
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
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processView(Request request, Entry entry, Metadata metadata)
            throws Exception {
        Metadata.Type type = getType(metadata.getType());
        if (type.equals(TYPE_THUMBNAIL) || type.equals(TYPE_ATTACHMENT)) {
            File f = getImageFile(entry, metadata);
            if (f == null) {
                return new Result("", "Thumbnail does not exist");
            }
            String mimeType = getRepository().getMimeTypeFromSuffix(
                                  IOUtil.getFileExtension(f.toString()));
            Result result =
                new Result("thumbnail",
                           IOUtil.readBytes(new FileInputStream(f), null,
                                            true), mimeType);
            result.setShouldDecorate(false);
            return result;
        }
        return new Result("", "Cannot process view");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param id _more_
     * @param suffix _more_
     * @param metadataList _more_
     * @param newMetadata _more_
     *
     * @throws Exception _more_
     */
    public void handleForm(Request request, Entry entry, String id,
                           String suffix, List<Metadata> metadataList,
                           boolean newMetadata)
            throws Exception {
        Metadata.Type type = getType(request.getString(ARG_TYPE + suffix,
                                 ""));
        if (type == null) {
            return;
        }
        if (type.equals(TYPE_THUMBNAIL) || type.equals(TYPE_ATTACHMENT)) {
            if ( !newMetadata) {
                //TODO: delete the old thumbs file
            }
            String fileArg = request.getUploadedFile(ARG_ATTR1 + suffix);
            if (fileArg == null) {
                return;
            }
            String fileName =
                getRepository().getStorageManager().moveToEntryDir(entry,
                    new File(fileArg)).getName();
            metadataList.add(new Metadata(id, entry.getId(), type, false,
                                          fileName, "", "", ""));
            return;
        }
        super.handleForm(request, entry, id, suffix, metadataList,
                         newMetadata);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param forEdit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getForm(Request request, Entry entry, Metadata metadata,
                            boolean forEdit)
            throws Exception {
        Metadata.Type type    = getType(metadata.getType());
        String        lbl     = msgLabel(type.getLabel());
        String        content = null;
        String        id      = metadata.getId();
        String        suffix  = "";
        if (id.length() > 0) {
            suffix = "." + id;
        }


        String submit = HtmlUtil.submit(msg("Add") + HtmlUtil.space(1)
                                        + type.getLabel());
        String cancel = HtmlUtil.submit(msg("Cancel"), ARG_CANCEL);
        if (forEdit) {
            submit = "";
            cancel = "";
        }
        String arg1 = ARG_ATTR1 + suffix;
        String arg2 = ARG_ATTR2 + suffix;
        String size = HtmlUtil.SIZE_70;
        if (type.equals(TYPE_THUMBNAIL) || type.equals(TYPE_ATTACHMENT)) {
            String image = getHtml(request, entry, metadata, false);
            if (image == null) {
                image = "";
            } else {
                image = "<br>" + image;
            }
            content = formEntry(new String[] { submit,
                    msgLabel((type.equals(TYPE_THUMBNAIL)
                              ? "Thumbnail"
                              : "Attachment")), HtmlUtil.fileInput(arg1,
                              size) + image });
        }

        if (content == null) {
            return null;
        }
        String argtype = ARG_TYPE + suffix;
        String argid   = ARG_METADATAID + suffix;
        content = content + HtmlUtil.hidden(argtype, type.getType())
                  + HtmlUtil.hidden(argid, metadata.getId());
        if (cancel.length() > 0) {
            content = content + HtmlUtil.row(HtmlUtil.colspan(cancel, 2));
        }
        return new String[] { lbl, content };
    }






}

