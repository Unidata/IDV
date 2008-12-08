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
    private List predefinedValues;



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
            predefinedValues = StringUtil.split(tagValues, "\n", true, true);
        }

        TYPE_ENUM = new Metadata.Type(XmlUtil.getAttribute(node, ATTR_TYPE),
                                      XmlUtil.getAttribute(node, ATTR_NAME));
        addType(TYPE_ENUM);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected String getHandlerGroupName() {
        return "Tags";
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
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     */
    public String[] getHtml(Request request, Entry entry, Metadata metadata) {
        String lbl = msgLabel(TYPE_ENUM.getLabel());
        String content = getSearchLink(request, metadata)
                         + metadata.getAttr1();
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
        List<String> l = (List<String>) getValues(request, true);
        if ((l == null) || (l.size() == 0)) {
            return;
        }
        List values = trimValues(l);
        values.add(0, new TwoFacedObject(msg("-all-"), ""));

        String inheritedCbx = HtmlUtil.checkbox(ARG_METADATA_INHERITED + "."
                                  + TYPE_ENUM, "true",
                                      false) + HtmlUtil.space(1)
                                             + "inherited";
        inheritedCbx = "";
        sb.append(HtmlUtil.hidden(ARG_METADATA_TYPE + "." + TYPE_ENUM,
                                  TYPE_ENUM.toString()));
        String argName = ARG_METADATA_ATTR1 + "." + TYPE_ENUM.toString();
        String value   = request.getString(argName, "");
        sb.append(HtmlUtil.formEntry(msgLabel(TYPE_ENUM.getLabel()),
                                     HtmlUtil.select(argName, values, value,
                                         100) + inheritedCbx));

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void addToBrowseSearchForm(Request request, StringBuffer sb)
            throws Exception {
        addToBrowseSearchForm(request, sb, TYPE_ENUM, true);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param justTheOnesInTheDatabase _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List getValues(Request request, boolean justTheOnesInTheDatabase)
            throws Exception {
        if ( !justTheOnesInTheDatabase && (predefinedValues != null)) {
            return predefinedValues;
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
        Metadata.Type type   = TYPE_ENUM;
        String        lbl    = msgLabel(type.getLabel());

        String        id     = metadata.getId();
        String        suffix = "";
        if (id.length() > 0) {
            suffix = "." + id;
        }
        String submit = HtmlUtil.submit(msg("Add") + HtmlUtil.space(1) + lbl);
        if (forEdit) {
            submit = "";
        }
        String cancel = HtmlUtil.submit(msg("Cancel"), ARG_CANCEL);
        if (forEdit) {
            submit = "";
            cancel = "";
        }

        String arg1 = ARG_ATTR1 + suffix;
        String content;
        if (predefinedValues != null) {
            content = HtmlUtil.row(HtmlUtil.colspan(submit, 2))
                      + HtmlUtil.row(HtmlUtil.colspan(HtmlUtil.select(arg1,
                          predefinedValues, metadata.getAttr1(), 100), 2));
            //            content = HtmlUtil.formEntry(submit,
            //                                         HtmlUtil.select(arg1,
            //                                             predefinedValues,
            //                                             metadata.getAttr1(), 100));

        } else {
            List values = getValues(request, false);
            if (values != null) {
                values.add(0, new TwoFacedObject("", ""));
                content = formEntry(new String[] { submit, lbl,
                        HtmlUtil.input(arg1, metadata.getAttr1(),
                                       HtmlUtil.SIZE_40),
                        msgLabel("Or Use"),
                        HtmlUtil.select(arg1 + ".select", values) });
            } else {
                content = HtmlUtil.formEntry(submit,
                                             HtmlUtil.input(arg1,
                                                 metadata.getAttr1(),
                                                     HtmlUtil.SIZE_40));
            }
        }

        if ( !forEdit) {
            content = content + HtmlUtil.colspan(cancel, 2);
        }
        String argtype = ARG_TYPE + suffix;
        String argid   = ARG_METADATAID + suffix;
        content = content + HtmlUtil.hidden(argtype, type.getType())
                  + HtmlUtil.hidden(argid, metadata.getId());
        return new String[] { lbl, content };
    }






}

