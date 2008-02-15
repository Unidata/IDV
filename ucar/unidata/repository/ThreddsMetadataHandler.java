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
import java.util.Hashtable;
import java.util.List;


import java.sql.Statement;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ThreddsMetadataHandler extends MetadataHandler {

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_ROLE = "role";

    /** _more_ */
    public static final String ATTR_EMAIL = "email";

    /** _more_ */
    public static final String ATTR_URL = "url";

    /** _more_ */
    public static final String ATTR_VOCABULARY = "vocabulary";

    /** _more_ */
    public static final String ATTR_VALUE = "value";



    /** _more_ */
    public static final Metadata.Type TYPE_CREATOR = new Metadata.Type("creator");

    /** _more_ */
    public static final Metadata.Type TYPE_LINK = new Metadata.Type("link");

    /** _more_ */
    public static final Metadata.Type TYPE_DATAFORMAT = new Metadata.Type("dataFormat", "Data Format" );

    /** _more_ */
    public static final Metadata.Type TYPE_DATATYPE = new Metadata.Type("dataType", "Data Type");

    /** _more_ */
    public static final Metadata.Type TYPE_AUTHORITY = new Metadata.Type("authority");

    /** _more_ */
    public static final Metadata.Type TYPE_VARIABLE = new Metadata.Type("variable");

    /** _more_ */
    public static final Metadata.Type TYPE_VOCABULARY = new Metadata.Type("vocabulary");

    /** _more_ */
    public static final Metadata.Type TYPE_VARIABLES = new Metadata.Type("variables");

    /** _more_ */
    public static final Metadata.Type TYPE_PUBLISHER = new Metadata.Type("publisher");

    /** _more_ */
    public static final Metadata.Type TYPE_PARAMETERS = new Metadata.Type("parameters");

    /** _more_ */
    public static final Metadata.Type TYPE_PROJECT = new Metadata.Type("project");

    /** _more_ */
    public static final Metadata.Type TYPE_KEYWORD = new Metadata.Type("keyword");

    /** _more_ */
    public static final Metadata.Type TYPE_CONTRIBUTOR = new Metadata.Type("contributor");

    /** _more_ */
    public static final Metadata.Type TYPE_PROPERTY = new Metadata.Type("property");

    /** _more_ */
    public static final Metadata.Type TYPE_DOCUMENTATION = new Metadata.Type("documentation");

    /** _more_ */
    public static final Metadata.Type TYPE_ICON = new Metadata.Type("icon");



    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public ThreddsMetadataHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
        addType(TYPE_DOCUMENTATION);
        addType(TYPE_PROPERTY);
        addType(TYPE_LINK);
        addType(TYPE_KEYWORD);
        addType(TYPE_ICON);
        addType(TYPE_CONTRIBUTOR);
        addType(TYPE_CREATOR);
        addType(TYPE_PROJECT);
        addType(TYPE_AUTHORITY);
        addType(TYPE_DATATYPE);
        addType(TYPE_DATAFORMAT);
        addType(TYPE_VOCABULARY);
        addType(TYPE_PUBLISHER);

        addType(TYPE_VARIABLES);
    }




    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @return _more_
     */
    public String[] getHtml(Metadata metadata) {
        Metadata.Type type    = getType(metadata.getType());
        String lbl     = type.getLabel() + ":";
        String content = null;
        if (type.equals(TYPE_LINK)) {
            content = HtmlUtil.href(metadata.getAttr2(), metadata.getAttr1());
        } else if (type.equals(TYPE_DOCUMENTATION)) {
            if (metadata.getAttr1().length() > 0) {
                lbl = getLabel(metadata.getAttr1()) + ":";
            }
            content = metadata.getAttr2();
        } else if (type.equals(TYPE_PROPERTY)) {
            lbl     = getLabel(metadata.getAttr1()) + ":";
            content = metadata.getAttr2();
        } else if (type.equals(TYPE_ICON)) {
            lbl     = "";
            content =HtmlUtil.img(metadata.getAttr1());
        } else if (type.equals(TYPE_PUBLISHER) || type.equals(TYPE_CREATOR)) {
            content = metadata.getAttr1();
            if (metadata.getAttr3().length() > 0) {
                content += "<br>Email: " + metadata.getAttr3();
            }
            if (metadata.getAttr4().length() > 0) {
                content += "<br>Url: "
                           + HtmlUtil.href(metadata.getAttr4(),
                                           metadata.getAttr4());
            }
        } else {
            content = metadata.getAttr1();
        }
        if (content == null) {
            return null;
        }
        return new String[] { lbl, content };
    }



    public void addToSearchForm(Request request, StringBuffer sb, Metadata.Type type, boolean makeSelect)
            throws Exception {
        if(makeSelect) {
            Statement stmt = getDatabaseManager().execute(
                                                          SqlUtil.makeSelect(SqlUtil.distinct(COL_METADATA_ATTR1), TABLE_METADATA,
                                                                             SqlUtil.eq(COL_METADATA_TYPE,SqlUtil.quote(type.getType()))));
            String[] values = SqlUtil.readString(stmt, 1);
            if(values.length==0) return;
            List l = Misc.toList(values);
            l.add(0, new TwoFacedObject("None",""));
            sb.append(HtmlUtil.formEntry(type.getLabel()+":",   HtmlUtil.select(ARG_METADATA_TYPE+"."+type, l, "",100)));

        } else {
            sb.append(HtmlUtil.formEntry(type.getLabel()+":",   HtmlUtil.input(ARG_METADATA_TYPE+"."+type, "")));
        }
    }


    public void addToSearchForm(Request request, StringBuffer sb)
            throws Exception {
        addToSearchForm(request, sb, TYPE_DOCUMENTATION,false);
        addToSearchForm(request, sb, TYPE_PROJECT,true);
        addToSearchForm(request, sb, TYPE_CREATOR,true);
        addToSearchForm(request, sb, TYPE_CONTRIBUTOR,true);
        addToSearchForm(request, sb, TYPE_PUBLISHER,true);
    }


    private  String formEntry(String[]cols) {
        if(cols.length==2) {
            //            return HtmlUtil.rowTop(HtmlUtil.cols(cols[0])+"<td colspan=2>" + cols[1] +"</td>");
            return HtmlUtil.rowTop(HtmlUtil.cols(cols[0])+"<td xxcolspan=2>" + cols[1] +"</td>");
        }
        StringBuffer sb = new StringBuffer();

        sb.append(HtmlUtil.rowTop("<td colspan=2>" +cols[0]+"</td>"));
        for(int i=1;i<cols.length;i+=2) {
            if(false && i == 1) {
                sb.append(HtmlUtil.rowTop(HtmlUtil.cols(cols[0])+"<td class=\"formlabel\" align=right>" + cols[i] +"</td>" +
                                       "<td>" + cols[i+1]));
            } else {
                //                sb.append(HtmlUtil.rowTop("<td></td><td class=\"formlabel\" align=right>" + cols[i] +"</td>" +
                //                                          "<td>" + cols[i+1]));
                sb.append(HtmlUtil.rowTop("<td class=\"formlabel\" align=right>" + cols[i] +"</td>" +
                                          "<td>" + cols[i+1]));
            }
        }
        return sb.toString();
    }


    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @return _more_
     */
    public String[] getForm(Metadata metadata, boolean forEdit) {
        Metadata.Type  type    = getType(metadata.getType());
        String lbl     = type.getLabel() + ":";
        String content = null;
        String id      = metadata.getId();
        String suffix  = "";
        if (id.length() > 0) {
            suffix = "." + id;
        }


        String submit = HtmlUtil.submit("Add " + lbl);
        if(forEdit) submit = "";
        String arg1 = ARG_ATTR1 + suffix;
        String arg2 = ARG_ATTR2 + suffix;
        String arg3 = ARG_ATTR3 + suffix;
        String arg4 = ARG_ATTR4 + suffix;
        String size = HtmlUtil.SIZE_70;

        if (type.equals(TYPE_LINK)) {
            content = formEntry(new String[] {submit,"Label:",
                    HtmlUtil.input(arg1, metadata.getAttr1(), size), "Url:",
                    HtmlUtil.input(arg2, metadata.getAttr2(), size) });
        }  if (type.equals(TYPE_ICON)) {
            content = formEntry(new String[] {submit,"URL:",
                                              HtmlUtil.input(arg1, metadata.getAttr1(), size)});
        } else if (type.equals(TYPE_DOCUMENTATION)) {
            List types = Misc.newList(new TwoFacedObject("Summary","summary"),
                                      new TwoFacedObject("Funding","funding"),
                                      new TwoFacedObject("History", "history"),
                                      new TwoFacedObject("Processing Level", "processing_level"),
                                      new TwoFacedObject("Rights","rights"));

            content = formEntry(new String[] { submit, "Type:",
                    HtmlUtil.select(arg1, types, metadata.getAttr1()), "Value:",
                    HtmlUtil.textArea(arg2, metadata.getAttr2(), 5, 50) });
        } else if (type.equals(TYPE_CONTRIBUTOR)) {
            content = formEntry(new String[] { submit,"Name:",
                    HtmlUtil.input(arg1, metadata.getAttr1(), size), "Role:",
                    HtmlUtil.input(arg2, metadata.getAttr2(), size) });
        } else if (type.equals(TYPE_PROPERTY)) {
            content =formEntry(new String[] {submit, "Name:",
                    HtmlUtil.input(arg1, metadata.getAttr1(), size), "Value:",
                    HtmlUtil.input(arg2, metadata.getAttr2(), size) });

        } else if (type.equals(TYPE_KEYWORD)) {
            content = formEntry(new String[] { submit,"Value:",
                    HtmlUtil.input(arg1,
                                   metadata.getAttr1().replace("\n", ""),
                                   size),
                    "Vocabulary:",
                    HtmlUtil.input(arg2, metadata.getAttr2(), size) });

        } else if (type.equals(TYPE_PUBLISHER) || type.equals(TYPE_CREATOR)) {
            content = formEntry(new String[] {submit,
                "Organization:",
                HtmlUtil.input(arg1, metadata.getAttr1(), size), "Email:",
                HtmlUtil.input(arg3, metadata.getAttr3(), size), "Url:",
                HtmlUtil.input(arg4, metadata.getAttr4(), size)
            });
        } else {
            content = formEntry(new String[]{submit,HtmlUtil.input(arg1, metadata.getAttr1(), size)});
        }
        if (content == null) {
            return null;
        }
        String argtype = ARG_TYPE + suffix;
        String argid   = ARG_METADATAID + suffix;
        content = content + HtmlUtil.hidden(argtype, type.getType())
                  + HtmlUtil.hidden(argid, metadata.getId());
        return new String[] { lbl, content };
    }


    public boolean isTag(String tag, Metadata.Type type) {
        return tag.toLowerCase().equals(type.getType());
    }


    /**
     * _more_
     *
     * @param child _more_
     *
     * @return _more_
     */
    public Metadata makeMetadataFromCatalogNode(Element child) {
        String tag = child.getTagName();
        if (isTag(tag,TYPE_DOCUMENTATION)) {
            if (XmlUtil.hasAttribute(child, "xlink:href")) {
                String url = XmlUtil.getAttribute(child, "xlink:href");
                return new Metadata(getRepository().getGUID(), "", TYPE_LINK,
                                    XmlUtil.getAttribute(child,
                                        "xlink:title", url), url, "", "");
            } else {
                String type = XmlUtil.getAttribute(child, "type");
                String text = XmlUtil.getChildText(child).trim();
                return new Metadata(getRepository().getGUID(), "", TYPE_DOCUMENTATION, type,
                                    text, "", "");
            }
        } else if (isTag(tag,TYPE_PROJECT)) {
            String text = XmlUtil.getChildText(child).trim();
            return new Metadata(getRepository().getGUID(), "", TYPE_PROJECT, text,
                                XmlUtil.getAttribute(child, ATTR_VOCABULARY,
                                    ""), "", "");
        } else if (isTag(tag,TYPE_CONTRIBUTOR)) {
            String text = XmlUtil.getChildText(child).trim();
            return new Metadata(getRepository().getGUID(), "", TYPE_CONTRIBUTOR, text,
                                XmlUtil.getAttribute(child, ATTR_ROLE, ""),
                                "", "");
        } else if (isTag(tag,TYPE_PUBLISHER) || isTag(tag,TYPE_CREATOR)) {
            Element nameNode = XmlUtil.findChild(child, CatalogOutputHandler.TAG_NAME);
            String  name     = XmlUtil.getChildText(nameNode).trim();
            String vocabulary = XmlUtil.getAttribute(nameNode,
                                    ATTR_VOCABULARY, "");
            String  email       = "";
            String  url         = "";
            Element contactNode = XmlUtil.findChild(child, CatalogOutputHandler.TAG_CONTACT);
            if (contactNode != null) {
                email = XmlUtil.getAttribute(contactNode, ATTR_EMAIL, "");
                url   = XmlUtil.getAttribute(contactNode, ATTR_URL, "");
            }
            return new Metadata(getRepository().getGUID(), "", getType(tag), name,
                                vocabulary, email, url);
        } else if (isTag(tag,TYPE_KEYWORD)) {
            String text = XmlUtil.getChildText(child).trim();
            return new Metadata(getRepository().getGUID(), "", TYPE_KEYWORD, text,
                                XmlUtil.getAttribute(child, ATTR_VOCABULARY,
                                    ""), "", "");

        } else if (isTag(tag,TYPE_AUTHORITY)
                   || isTag(tag,TYPE_DATATYPE)
                   || isTag(tag,TYPE_DATAFORMAT)) {
            String text = XmlUtil.getChildText(child).trim();
            text = text.replace("\n", "");
            return new Metadata(getRepository().getGUID(), "", getType(tag), text, "",
                                "", "");
        } else if (isTag(tag,TYPE_VOCABULARY) || isTag(tag,TYPE_VARIABLES)) {
            String text = XmlUtil.toString(child, false);
            return new Metadata(getRepository().getGUID(), "", getType(tag), text, "",
                                "", "");
        } else if (isTag(tag,TYPE_PROPERTY)) {
            return new Metadata(getRepository().getGUID(), "", getType(tag),
                                XmlUtil.getAttribute(child, ATTR_NAME),
                                XmlUtil.getAttribute(child, ATTR_VALUE), "",
                                "");
        }
        return null;
    }




}

