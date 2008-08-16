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


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;





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
    private Hashtable typeMap = new Hashtable();

    /** _more_ */
    private List<Metadata.Type> types = new ArrayList<Metadata.Type>();

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
     * @param attr1 _more_
     * @param attr2 _more_
     * @param attr3 _more_
     * @param attr4 _more_
     *
     * @return _more_
     */
    public Metadata makeMetadata(String id, String entryId, String type,
                                 boolean inherited,
                                 String attr1, String attr2, String attr3,
                                 String attr4) {
        return new Metadata(id, entryId, type, inherited, attr1, attr2, attr3, attr4);
    }


    /**
     * _more_
     *
     * @param stringType _more_
     *
     * @return _more_
     */
    public Metadata.Type findType(String stringType) {
        for (Metadata.Type type : types) {
            if (type.getType().equals(stringType)) {
                return type;
            }
        }
        return null;
    }


    /**
     * _more_
     *
     * @param type _more_
     */
    public void addType(Metadata.Type type) {
        types.add(type);
        typeMap.put(type.getType(), type);
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public Metadata.Type getType(String type) {
        return (Metadata.Type) typeMap.get(type);
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
            throws Exception {}


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
        s = s.replace("_", " ");
        s = s.replace(".", " ");
        s = s.substring(0, 1).toUpperCase() + s.substring(1);
        return s;
    }


    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @return _more_
     */
    public String[] getHtml(Metadata metadata) {
        return null;
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param metadata _more_
     * @param forEdit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getForm(Request request, Metadata metadata,
                            boolean forEdit)
            throws Exception {
        return null;
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
        for (Metadata.Type type : types) {
            makeAddForm(request, entry, type, sb);
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, StringBuffer sb)
            throws Exception {}


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
    public void makeAddForm(Request request, Entry entry, Metadata.Type type,
                            StringBuffer sb)
            throws Exception {
        if (type == null) {
            return;
        }
        Metadata metadata =new Metadata(type);
        metadata.setEntry(entry);
        String[] html = getForm(request, metadata, false);
        if (html == null) {
            return;
        }
        sb.append(request.form(getMetadataManager().URL_METADATA_ADD));
        sb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
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
        for (Metadata.Type type : types) {
            //            makeAddForm(entry, types.get(i).toString(), sb);
        }
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @return _more_
     */
    public List<Metadata.Type> getTypes(Request request, Entry entry) {
        return types;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadataList _more_
     */
    public void handleAddSubmit(Request request, Entry entry,
                                List<Metadata> metadataList) {
        String type = request.getString(ARG_TYPE, "");
        if ( !canHandle(type)) {
            return;
        }
        String attr1 = request.getString(ARG_ATTR1, "");
        if (request.defined(ARG_ATTR1 + ".select")) {
            attr1 = request.getString(ARG_ATTR1 + ".select", "");
        }
        String attr2 = request.getString(ARG_ATTR2, "");
        if (request.defined(ARG_ATTR2 + ".select")) {
            attr1 = request.getString(ARG_ATTR2 + ".select", "");
        }
        String attr3 = request.getString(ARG_ATTR3, "");
        if (request.defined(ARG_ATTR3 + ".select")) {
            attr1 = request.getString(ARG_ATTR3 + ".select", "");
        }
        String attr4 = request.getString(ARG_ATTR4, "");
        if (request.defined(ARG_ATTR4 + ".select")) {
            attr1 = request.getString(ARG_ATTR4 + ".select", "");
        }
        if ((attr1.length() == 0) && (attr2.length() == 0)
                && (attr3.length() == 0) && (attr4.length() == 0)) {
            return;
        }
        metadataList.add(new Metadata(getRepository().getGUID(),
                                      entry.getId(), type, DFLT_INHERITED, attr1, attr2,
                                      attr3, attr4));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadataList _more_
     */
    public void handleFormSubmit(Request request, Entry entry,
                                 List<Metadata> metadataList) {
        Hashtable args = request.getArgs();
        for (Enumeration keys = args.keys(); keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            if ( !arg.startsWith(ARG_METADATAID + ".")) {
                continue;
            }
            String id   = request.getString(arg, "");
            String type = request.getString(ARG_TYPE + "." + id, "");
            if ( !canHandle(type)) {
                continue;
            }
            String suffix = "." + id;
            String attr1  = request.getString(ARG_ATTR1 + suffix, "");
            if (request.defined(ARG_ATTR1 + suffix + ".select")) {
                attr1 = request.getString(ARG_ATTR1 + suffix + ".select", "");
            }

            String attr2 = request.getString(ARG_ATTR2 + suffix, "");
            if (request.defined(ARG_ATTR2 + suffix + ".select")) {
                attr1 = request.getString(ARG_ATTR2 + suffix + ".select", "");
            }

            String attr3 = request.getString(ARG_ATTR3 + suffix, "");
            if (request.defined(ARG_ATTR3 + suffix + ".select")) {
                attr1 = request.getString(ARG_ATTR3 + suffix + ".select", "");
            }

            String attr4 = request.getString(ARG_ATTR4 + suffix, "");
            if (request.defined(ARG_ATTR4 + suffix + ".select")) {
                attr1 = request.getString(ARG_ATTR4 + suffix + ".select", "");
            }

            metadataList.add(new Metadata(id, entry.getId(), type, DFLT_INHERITED, attr1,
                                          attr2, attr3, attr4));
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




}

