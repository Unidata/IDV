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
public class EnumeratedMetadataHandler extends MetadataHandler {

    /** _more_ */
    public static final String ATTR_FILE = "file";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String TYPE_TAG = "enum_tag";

    /** _more_ */
    private Metadata.Type TYPE_ENUM;

    /** _more_ */
    private List values;



    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public EnumeratedMetadataHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
        if (XmlUtil.hasAttribute(node, ATTR_FILE)) {
            String tagValues = IOUtil.readContents(XmlUtil.getAttribute(node,
                                   ATTR_FILE), getClass());
            values = StringUtil.split(tagValues, "\n", true, true);
        }

        TYPE_ENUM = new Metadata.Type(XmlUtil.getAttribute(node, ATTR_TYPE),
                                      XmlUtil.getAttribute(node, ATTR_NAME));
        addType(TYPE_ENUM);
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
        Metadata.Type type = getType(metadata.getType());
        if (type.equals(TYPE_ENUM)) {
            XmlUtil.create(doc,
                           ThreddsMetadataHandler.TYPE_KEYWORD.toString(),
                           datasetNode, metadata.getAttr1());
        }
    }


    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @return _more_
     */
    public String[] getHtml(Metadata metadata) {
        String lbl     = TYPE_ENUM.getLabel() + ":";
        String content = metadata.getAttr1();
        return new String[] { lbl, content };
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
            throws Exception {
        List l = getValues(request);
        if (l == null) {
            return;
        }
        l = new ArrayList(l);
        l.add(0, new TwoFacedObject("None", ""));

        String inheritedCbx = HtmlUtil.checkbox(ARG_METADATA_INHERITED + "."
                                  + TYPE_ENUM, "true",
                                      false) + HtmlUtil.space(1)
                                             + "inherited";
        inheritedCbx = "";
        sb.append(HtmlUtil.hidden(ARG_METADATA_TYPE + "." + TYPE_ENUM,
                                  TYPE_ENUM.toString()));
        sb.append(HtmlUtil.formEntry(TYPE_ENUM.getLabel() + ":",
                                     HtmlUtil.select(ARG_METADATA_ATTR1 + "."
                                         + TYPE_ENUM.toString(), l, "",
                                             100) + inheritedCbx));

    }


    /**
     * _more_
     *
     * @param cols _more_
     *
     * @return _more_
     */
    private String formEntry(String[] cols) {
        if (cols.length == 2) {
            //            return HtmlUtil.rowTop(HtmlUtil.cols(cols[0])+"<td colspan=2>" + cols[1] +"</td>");
            return HtmlUtil.rowTop(HtmlUtil.cols(cols[0])
                                   + "<td xxcolspan=2>" + cols[1] + "</td>");
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
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List getValues(Request request) throws Exception {
        if (values != null) {
            return values;
        }
        String[] values = getMetadataManager().getDistinctValues(request,
                              this, TYPE_ENUM);
        if ((values == null) || (values.length == 0)) {
            return null;
        }
        return Misc.toList(values);
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
        Metadata.Type type   = TYPE_ENUM;
        String        lbl    = type.getLabel() + ":";

        String        id     = metadata.getId();
        String        suffix = "";
        if (id.length() > 0) {
            suffix = "." + id;
        }
        String submit = HtmlUtil.submit("Add " + lbl);
        if (forEdit) {
            submit = "";
        }
        String arg1 = ARG_ATTR1 + suffix;
        String content;
        if (values != null) {
            content = formEntry(new String[] { submit,
                    HtmlUtil.select(arg1, getValues(request),
                                    metadata.getAttr1()) });
        } else {
            content = formEntry(new String[] { submit,
                    HtmlUtil.input(arg1, metadata.getAttr1()) });
        }

        String argtype = ARG_TYPE + suffix;
        String argid   = ARG_METADATAID + suffix;
        content = content + HtmlUtil.hidden(argtype, type.getType())
                  + HtmlUtil.hidden(argid, metadata.getId());
        return new String[] { lbl, content };
    }






}

