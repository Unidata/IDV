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


import ucar.unidata.data.SqlUtil;
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

    /** _more_          */
    public static String ARG_TYPE = "type";

    /** _more_          */
    public static String ARG_METADATAID = "metadataid";

    /** _more_          */
    public static String ARG_ENTRYID = "entryid";

    /** _more_          */
    public static String ARG_ATTR1 = "attr1";

    /** _more_          */
    public static String ARG_ATTR2 = "attr2";

    /** _more_          */
    public static String ARG_ATTR3 = "attr3";

    /** _more_          */
    public static String ARG_ATTR4 = "attr4";




    /** _more_ */
    private Hashtable canHandle = new Hashtable();

    /** _more_ */
    private List types = new ArrayList();

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
    public void setCanHandle(String type) {
        types.add(type);
        canHandle.put(type, type);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List getTypes() {
        return types;
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
        return canHandle.get(metadata.getType()) != null;
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public boolean canHandle(String type) {
        return canHandle.get(type) != null;
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
     * @param metadata _more_
     *
     * @return _more_
     */
    public String[] getEditForm(Metadata metadata) {
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
        for (int i = 0; i < types.size(); i++) {
            makeAddForm(entry, types.get(i).toString(), sb);
        }
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param type _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    private void makeAddForm(Entry entry, String type, StringBuffer sb)
            throws Exception {
        String[] html = getEditForm(new Metadata(type));
        if (html == null) {
            return;
        }
        sb.append("<p>&nbsp;<p>");
        sb.append(HtmlUtil.form(getRepository().URL_METADATA_ADD));
        sb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
        sb.append(HtmlUtil.submit("Add " + html[0]));
        sb.append(HtmlUtil.space(2));
        sb.append(html[1]);
        sb.append(HtmlUtil.formClose());
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
        String attr2 = request.getString(ARG_ATTR2, "");
        String attr3 = request.getString(ARG_ATTR3, "");
        String attr4 = request.getString(ARG_ATTR4, "");
        if ((attr1.length() == 0) && (attr2.length() == 0)
                && (attr3.length() == 0) && (attr4.length() == 0)) {
            return;
        }
        metadataList.add(new Metadata(getRepository().getGUID(),
                                      entry.getId(), type, attr1, attr2,
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
            metadataList.add(
                new Metadata(
                    id, entry.getId(), type,
                    request.getString(ARG_ATTR1 + "." + id, ""),
                    request.getString(ARG_ATTR2 + "." + id, ""),
                    request.getString(ARG_ATTR3 + "." + id, ""),
                    request.getString(ARG_ATTR4 + "." + id, "")));

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

