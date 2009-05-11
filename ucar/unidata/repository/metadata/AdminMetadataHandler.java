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
 * WITHOUT ANY WARRANTYP; without even the implied warranty of
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
public class AdminMetadataHandler extends MetadataHandler {


    /** _more_ */
    public static final String TYPE_TEMPLATE = "admin.template";

    /** _more_ */
    public static final String TYPE_CONTENTTEMPLATE = "admin.contenttemplate";

    /** _more_ */
    public static final String TYPE_LOCALFILE_PATTERN =
        "admin.localfile.pattern";

    /** _more_ */
    public static final String TYPE_ANONYMOUS_UPLOAD =
        "admin.anonymousupload";



    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public AdminMetadataHandler(Repository repository) throws Exception {
        super(repository);
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
        String[] result = super.getHtml(request, entry, metadata);
        if (result != null) {
            return result;
        }
        MetadataType type = findType(metadata.getType());
        if (type == null) {
            return null;
        }
        String lbl = msgLabel(type.getLabel());
        if (type.isType(TYPE_TEMPLATE) || type.isType(TYPE_CONTENTTEMPLATE)) {
            return new String[] { lbl, "Has template" };
        }


        if (type.isType(TYPE_LOCALFILE_PATTERN)) {
            return new String[] { lbl, "Local File Pattern" };
        }

        String content = metadata.getAttr1();
        //        return new String[] { lbl, content };
        return null;
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
        MetadataType type   = findType(metadata.getType());
        String       lbl    = msgLabel(type.getLabel());
        String       id     = metadata.getId();
        String       suffix = "";
        if (id.length() > 0) {
            suffix = "." + id;
        }
        String submit = (forEdit
                         ? ""
                         : HtmlUtil.submit(msg("Add") + HtmlUtil.space(1)
                                           + lbl));
        String cancel  = (forEdit
                          ? ""
                          : HtmlUtil.submit(msg("Cancel"), ARG_CANCEL));
        String arg1    = ARG_ATTR1 + suffix;
        String content = "";
        if (type.isType(TYPE_TEMPLATE)) {
            String value = metadata.getAttr1();
            if ( !forEdit || (value == null)) {
                value = getRepository().getResource(PROP_HTML_TEMPLATE);
            }
            if (value == null) {
                value = "";
            }
            value = value.replace("<", "&lt;");
            value = value.replace(">", "&gt;");
            value = value.replace("$", "&#36;");
            String textarea = HtmlUtil.textArea(arg1, value, 20, 80);
            content =
                HtmlUtil.row(HtmlUtil.colspan(submit, 2))
                + HtmlUtil.formEntry(lbl,
                                     "Note: must contain macro ${content}"
                                     + "<br>" + textarea);
        }
        if (type.isType(TYPE_LOCALFILE_PATTERN)) {
            if ((metadata.getEntry() == null)
                    || !metadata.getEntry().getIsLocalFile()) {
                return null;
            }
            String value = metadata.getAttr1();
            String input = HtmlUtil.input(arg1, value);
            content = HtmlUtil.row(HtmlUtil.colspan(submit, 2))
                      + HtmlUtil.formEntry(lbl, input);
        }
        if (type.isType(TYPE_ANONYMOUS_UPLOAD)) {
            content = "From:" + metadata.getAttr1() + " IP: "
                      + metadata.getAttr2();
        }


        if ( !forEdit) {
            content = content + HtmlUtil.row(HtmlUtil.colspan(cancel, 2));
        }
        String argtype = ARG_TYPE + suffix;
        String argid   = ARG_METADATAID + suffix;
        content = content + HtmlUtil.hidden(argtype, type.getId())
                  + HtmlUtil.hidden(argid, metadata.getId());
        return new String[] { lbl, content };
    }






}

