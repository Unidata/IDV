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


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;





import java.io.ByteArrayInputStream;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;




/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class MetadataHandler extends RepositoryManager {


    /** _more_ */
    public static String ATTR_FORUSER = "foruser";

    /** _more_ */
    public static String ARG_TYPE = "type";

    /** _more_ */
    public static String ARG_METADATAID = "metadataid";

    /** _more_ */
    public static String ARG_ENTRYID = "entryid";

    /** _more_ */
    public static String ARG_ATTR1 = "attr1";

    /** _more_ */
    public static String ARG_ATTR2 = "attr2";

    /** _more_ */
    public static String ARG_ATTR3 = "attr3";

    /** _more_ */
    public static String ARG_ATTR4 = "attr4";


    /** _more_ */
    protected Hashtable<String, MetadataType> typeMap = new Hashtable<String,
                                                            MetadataType>();


    /** _more_ */
    private List<MetadataType> metadataTypes = new ArrayList<MetadataType>();

    /** _more_ */
    boolean forUser = true;


    /**
     * _more_
     *
     * @param repository _more_
     */
    public MetadataHandler(Repository repository) {
        super(repository);
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public MetadataHandler(Repository repository, Element node)
            throws Exception {
        super(repository);
    }


    /**
     * _more_
     *
     * @param type _more_
     */
    public void addMetadataType(MetadataType type) {
        type.setHandler(this);
        metadataTypes.add(type);
        typeMap.put(type.getType(), type);
        getMetadataManager().addMetadataType(type);
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param node _more_
     * @param fileMap _more_
     * @param internal _more_
     *
     * @throws Exception _more_
     */
    public void processMetadataXml(Entry entry, Element node,
                                   Hashtable fileMap, boolean internal)
            throws Exception {
        forUser = XmlUtil.getAttribute(node, ATTR_FORUSER, true);
        String type = XmlUtil.getAttribute(node, ATTR_TYPE);
        //TODO: Handle the extra attributes
        String extra = XmlUtil.getGrandChildText(node,Metadata.TAG_EXTRA,"");
        Metadata metadata =
            new Metadata(getRepository().getGUID(), entry.getId(), type,
                         XmlUtil.getAttribute(node, ATTR_INHERITED,
                             DFLT_INHERITED), XmlUtil.getAttribute(node,
                                 ATTR_ATTR1, ""), XmlUtil.getAttribute(node,
                                     ATTR_ATTR2,
                                     ""), XmlUtil.getAttribute(node,
                                         ATTR_ATTR3,
                                         ""), XmlUtil.getAttribute(node,
                                             ATTR_ATTR4, ""),extra);

        MetadataType metadataType = findType(type);
        if ( !metadataType.processMetadataXml(entry, node, metadata, fileMap,
                internal)) {
            return;
        }
        entry.addMetadata(metadata);
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
        MetadataType type = getType(metadata.getType());
        type.newEntry(metadata, entry);
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
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return;
        }
        type.decorateEntry(request, entry, sb, metadata, forLink);
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
    public Result xxxprocessView(Request request, Entry entry,
                                 Metadata metadata)
            throws Exception {
        return new Result("", "Cannot process view");
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
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return null;
        }
        return type.processView(request, entry, metadata);
    }




    /**
     * _more_
     *
     * @param cols _more_
     *
     * @return _more_
     */
    protected String formEntry(String[] cols) {
        if (cols.length == 2) {
            //            return HtmlUtil.rowTop(HtmlUtil.cols(cols[0])+"<td colspan=2>" + cols[1] +"</td>");
            //            return HtmlUtil.rowTop(HtmlUtil.cols(cols[0])
            //                                   + "<td xxcolspan=2>" + cols[1] + "</td>");
        }
        StringBuffer sb = new StringBuffer();

        sb.append(HtmlUtil.rowTop("<td colspan=2>" + cols[0] + "</td>"));
        for (int i = 1; i < cols.length; i += 2) {
            if (false && (i == 1)) {
                sb.append(
                    HtmlUtil.rowTop(
                        HtmlUtil.cols(cols[0])
                        + "<td class=\"formlabel\" align=right>" + cols[i]
                        + "</td>" + "<td>" + cols[i + 1]));
            } else {
                //                sb.append(HtmlUtil.rowTop("<td></td><td class=\"formlabel\" align=right>" + cols[i] +"</td>" +
                //                                          "<td>" + cols[i+1]));
                sb.append(
                    HtmlUtil.rowTop(
                        "<td class=\"formlabel\" align=right>" + cols[i]
                        + "</td>" + "<td>" + cols[i + 1]));
            }
        }
        return sb.toString();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected String getHandlerGroupName() {
        return "Metadata";
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
     *
     * @return _more_
     */
    public Metadata makeMetadata(String id, String entryId, String type,
                                 boolean inherited, String attr1,
                                 String attr2, String attr3, String attr4,String extra) {
        return new Metadata(id, entryId, type, inherited, attr1, attr2,
                            attr3, attr4,extra);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param id _more_
     * @param entry _more_
     * @param type _more_
     * @param inherited _more_
     * @param attr1 _more_
     * @param attr2 _more_
     * @param attr3 _more_
     * @param attr4 _more_
     * @param newMetadata _more_
     *
     * @return _more_
     */
    protected final Metadata makeMetadata(Request request, String id,
                                          Entry entry, String type,
                                          boolean inherited, String attr1,
                                          String attr2, String attr3,
                                          String attr4, String extra, boolean newMetadata) {
        return new Metadata(id, entry.getId(), type, inherited, attr1, attr2,
                            attr3, attr4,extra);
    }



    /**
     * _more_
     *
     * @param stringType _more_
     *
     * @return _more_
     */
    public MetadataType findType(String stringType) {
        return typeMap.get(stringType);
    }


    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public MetadataType getType(String type) {
        return typeMap.get(type);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadataList _more_
     * @param extra _more_
     * @param shortForm _more_
     */
    public void getInitialMetadata(Request request, Entry entry,
                                   List<Metadata> metadataList,
                                   Hashtable extra, boolean shortForm) {}


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
        MetadataType type = getType(metadata.getType());
        if (type == null) {
            return;
        }
        type.addMetadataToCatalog(request, entry, metadata, doc, datasetNode);
    }


    /**
     * _more_
     *
     * @param child _more_
     *
     * @return _more_
     */
    public Metadata makeMetadataFromCatalogNode(Element child) {
        return null;
    }


    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @return _more_
     */
    public boolean canHandle(Metadata metadata) {
        return canHandle(metadata.getType());
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public boolean canHandle(String type) {
        return typeMap.get(type) != null;
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public String getLabel(String s) {
        if (s.length() == 0) {
            return "No label";
        }
        s = s.replace("_", " ");
        s = s.replace(".", " ");
        s = s.substring(0, 1).toUpperCase() + s.substring(1);
        return s;
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
    public String[] getHtml(Request request, Entry entry, Metadata metadata) throws Exception {
        MetadataType type = getType(metadata.getType());
        if ((type == null) || !type.hasElements()) {
            return null;
        }
        return type.getHtml(request, entry, metadata);
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
        MetadataType type = getType(metadata.getType());
        if ((type == null) || !type.hasElements()) {
            return null;
        }
        String suffix = "";
        if (metadata.getId().length() > 0) {
            suffix = "." + metadata.getId();
        }
        return type.getForm(this, request, entry, metadata, suffix, forEdit);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void makeAddForm(Request request, Entry entry, StringBuffer sb)
            throws Exception {
        for (MetadataType type : metadataTypes) {
            makeAddForm(request, entry, type, sb);
        }
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
        MetadataType type = findType(metadata.getType());
        if (type == null) {
            return null;
        }
        return type.getSearchUrl(request, metadata);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param type _more_
     * @param value _more_
     *
     * @return _more_
     */
    public String getSearchUrl(Request request, MetadataType type,
                               String value) {
        List args = new ArrayList();
        args.add(ARG_METADATA_TYPE + "." + type.getType());
        args.add(type.toString());
        args.add(ARG_METADATA_ATTR1 + "." + type.getType());
        args.add(value);
        return HtmlUtil.url(request.url(getRepository().URL_ENTRY_SEARCH),
                            args);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param metadata _more_
     *
     * @return _more_
     */
    public String getSearchLink(Request request, Metadata metadata) {
        return HtmlUtil.href(
            getSearchUrl(request, metadata),
            HtmlUtil.img(
                getRepository().iconUrl(ICON_SEARCH),
                "Search for entries with this metadata", " border=0 "));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param type _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, StringBuffer sb,
                                MetadataType type)
            throws Exception {
        boolean doSelect = true;
        sb.append(HtmlUtil.hidden(ARG_METADATA_TYPE + "." + type,
                                  type.toString()));
        String inheritedCbx = HtmlUtil.checkbox(ARG_METADATA_INHERITED + "."
                                  + type, "true", false) + HtmlUtil.space(1)
                                      + "inherited";
        inheritedCbx = "";

        if (doSelect) {
            String[] values = getMetadataManager().getDistinctValues(request,
                                  this, type);
            if ((values == null) || (values.length == 0)) {
                return;
            }
            List l = trimValues((List<String>) Misc.toList(values));
            l.add(0, new TwoFacedObject(msg("-all-"), ""));
            String argName = ARG_METADATA_ATTR1 + "." + type;
            String value   = request.getString(argName, "");
            sb.append(HtmlUtil.formEntry(msgLabel(type.getLabel()),
                                         HtmlUtil.select(argName, l, value,
                                             100) + inheritedCbx));
        } else {
            sb.append(
                HtmlUtil.formEntry(
                    msgLabel(type.getLabel()),
                    HtmlUtil.input(ARG_METADATA_ATTR1 + "." + type, "")
                    + inheritedCbx));
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param type _more_
     * @param doSelect _more_
     *
     * @throws Exception _more_
     */
    public void addToBrowseSearchForm(Request request, StringBuffer sb,
                                      MetadataType type)
            throws Exception {

        boolean doSelect = true;
        String cloudLink =
            HtmlUtil.href(
                request.url(
                    getRepository().getMetadataManager().URL_METADATA_LIST,
                    ARG_METADATA_TYPE, type.toString()), HtmlUtil.img(
                        getRepository().iconUrl(ICON_LIST), "View Listing"));
        String url = request.url(getRepository().URL_ENTRY_SEARCH);
        String[] values = getMetadataManager().getDistinctValues(request,
                              this, type);
        if ((values == null) || (values.length == 0)) {
            return;
        }
        StringBuffer content = new StringBuffer();
        content.append("<div class=\"browseblock\">");
        int rowNum = 1;
        for (int i = 0; i < values.length; i++) {
            String browseUrl = HtmlUtil.url(url,
                                            ARG_METADATA_TYPE + "."
                                            + type.getType(), type.getType(),
                                                ARG_METADATA_ATTR1 + "."
                                                + type.getType(), values[i]);
            String value = values[i].trim();
            if (value.length() == 0) {
                value = "-blank-";
            }
            content.append(HtmlUtil.div(HtmlUtil.href(browseUrl, value),
                                        HtmlUtil.cssClass("listrow"
                                            + rowNum)));
            rowNum++;
            if (rowNum > 2) {
                rowNum = 1;
            }
        }
        content.append("</div>");

        sb.append(
            HtmlUtil.makeShowHideBlock(
                cloudLink + HtmlUtil.space(1) + type.getLabel(),
                content.toString(), false));


    }

    /**
     * _more_
     *
     * @param l _more_
     *
     * @return _more_
     */
    protected List trimValues(List<String> l) {
        List values = new ArrayList();
        for (String s : l) {
            String label = s;
            if (label.length() > 50) {
                label = label.substring(0, 49) + "...";
            }
            values.add(new TwoFacedObject(label, s));
        }
        return values;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param type _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void makeAddForm(Request request, Entry entry, MetadataType type,
                            StringBuffer sb)
            throws Exception {
        if (type == null) {
            return;
        }
        Metadata metadata = new Metadata(type);
        metadata.setEntry(entry);
        String[] html = getForm(request, entry, metadata, false);
        if (html == null) {
            return;
        }
        sb.append(request.uploadForm(getMetadataManager().URL_METADATA_ADD,
                                     ""));
        sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
        sb.append(html[1]);
        sb.append(HtmlUtil.formClose());
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void makeSearchForm(Request request, StringBuffer sb)
            throws Exception {
        for (MetadataType type : metadataTypes) {
            //            makeAddForm(entry, types.get(i).toString(), sb);
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadataList _more_
     *
     * @throws Exception _more_
     */
    public void handleAddSubmit(Request request, Entry entry,
                                List<Metadata> metadataList)
            throws Exception {
        String id = getRepository().getGUID();
        handleForm(request, entry, id, "", null, metadataList, true);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param existingMetadata _more_
     * @param metadataList _more_
     *
     * @throws Exception _more_
     */
    public void handleFormSubmit(
            Request request, Entry entry,
            Hashtable<String, Metadata> existingMetadata,
            List<Metadata> metadataList)
            throws Exception {
        Hashtable args = request.getArgs();
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if ( !arg.startsWith(ARG_METADATAID + ".")) {
                continue;
            }
            String id     = request.getString(arg, "");
            String suffix = "." + id;
            handleForm(request, entry, id, suffix, existingMetadata,
                       metadataList, false);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param id _more_
     * @param suffix _more_
     * @param existingMetadata _more_
     * @param metadataList _more_
     * @param newMetadata _more_
     *
     * @throws Exception _more_
     */
    public void handleForm(Request request, Entry entry, String id,
                           String suffix,
                           Hashtable<String, Metadata> existingMetadata,
                           List<Metadata> metadataList, boolean newMetadata)
            throws Exception {

        String type = request.getString(ARG_TYPE + suffix, "");
        if ( !canHandle(type)) {
            return;
        }
        MetadataType metadataType = getType(type);
        if (metadataType == null) {
            return;
        }

        Metadata metadata = metadataType.handleForm(request, entry, id,
                                suffix, ((existingMetadata == null)
                                         ? null
                                         : existingMetadata.get(
                                             id)), newMetadata);
        if (metadata != null) {
            metadataList.add(metadata);
        }
    }




    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public String getFormHtml(String type) {
        return null;
    }


    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @return _more_
     */
    public String getCatalogXml(Metadata metadata) {
        return "";
    }





    /**
     *  Set the ForUser property.
     *
     *  @param value The new value for ForUser
     */
    public void setForUser(boolean value) {
        forUser = value;
    }

    /**
     *  Get the ForUser property.
     *
     *  @return The ForUser
     */
    public boolean getForUser() {
        return forUser;
    }



}

