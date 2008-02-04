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




/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ThreddsMetadataHandler extends MetadataHandler {

    /** _more_ */
    public static final String ATTR_NAME = "name";

    public static final String ATTR_ROLE = "role";

    public static final String ATTR_EMAIL = "email";

    public static final String ATTR_URL = "url";

    public static final String ATTR_VOCABULARY = "vocabulary";

    /** _more_ */
    public static final String ATTR_VALUE = "value";

    /** _more_ */
    public static final String TAG_CREATOR = "creator";
    public static final String TAG_NAME = "name";
    public static final String TAG_CONTACT = "contact";

    public static final String TAG_LINK = "link";

    /** _more_ */
    public static final String TAG_DATAFORMAT = "dataFormat";

    /** _more_ */
    public static final String TAG_DATATYPE = "dataType";

    /** _more_ */
    public static final String TAG_AUTHORITY = "authority";

    /** _more_ */
    public static final String TAG_VARIABLE = "variable";

    /** _more_ */
    public static final String TAG_VOCABULARY = "vocabulary";

    /** _more_ */
    public static final String TAG_VARIABLES = "variables";

    /** _more_ */
    public static final String TAG_PUBLISHER = "publisher";

    /** _more_ */
    public static final String TAG_PARAMETERS = "Parameters";

    /** _more_ */
    public static final String TAG_PROJECT = "project";

    /** _more_ */
    public static final String TAG_METADATA = "metadata";

    /** _more_ */
    public static final String TAG_ACCESS = "access";

    /** _more_ */
    public static final String TAG_KEYWORD = "keyword";

    /** _more_ */
    public static final String TAG_CONTRIBUTOR = "contributor";

    /** _more_ */
    public static final String TAG_PROPERTY = "property";

    /** _more_ */
    public static final String TAG_GEOSPATIALCOVERAGE = "geospatialCoverage";

    /** _more_ */
    public static final String TAG_TIMECOVERAGE = "timeCoverage";

    /** _more_ */
    public static final String TAG_START = "start";

    /** _more_ */
    public static final String TAG_END = "end";

    /** _more_ */
    public static final String TAG_DATE = "date";

    /** _more_ */
    public static final String TAG_DOCUMENTATION = "documentation";

    /** _more_ */
    public static final String TAG_DATASIZE = "dataSize";


    /** _more_ */
    public static final String TYPE_HTML = "html";

    /** _more_ */
    public static final String TYPE_URL = "html";

    /** _more_ */
    public static final String TYPE_LINK = "link";


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
        setCanHandle(TAG_DOCUMENTATION);
        setCanHandle(TAG_CONTRIBUTOR);
        setCanHandle(TAG_PROJECT);
        setCanHandle(TAG_KEYWORD);
        setCanHandle(TAG_AUTHORITY);
        setCanHandle(TAG_DATATYPE);
        setCanHandle(TAG_DATAFORMAT);
        setCanHandle(TAG_VOCABULARY);
        setCanHandle(TAG_PUBLISHER);
        setCanHandle(TAG_CREATOR);
        setCanHandle(TAG_VARIABLES);
        setCanHandle(TAG_PROPERTY);
        setCanHandle(TAG_LINK);
    }

    


    public String[] getHtml(Metadata metadata) {
        String type = metadata.getType();
        String lbl = getLabel(type) + ":";
        String content = null;
        if(type.equals(TAG_LINK)) {
            content = HtmlUtil.href(metadata.getAttr2(),
                                    metadata.getAttr1());
        }  else  if(type.equals(TAG_DOCUMENTATION)) {
            if(metadata.getAttr1().length()>0) {
                lbl = getLabel(metadata.getAttr1())+":";
            }
            content =  metadata.getAttr2();
        }  else  if(type.equals(TAG_PROPERTY)) {
            lbl = getLabel(metadata.getAttr1())+":";
            content = metadata.getAttr2();
        }  else  if(type.equals(TAG_PUBLISHER)) {
            content = metadata.getAttr1();
            if(metadata.getAttr3().length()>0) {
                content += "<br>Email: " + metadata.getAttr3();
            }
            if(metadata.getAttr4().length()>0) {
                content += "<br>Url: " + HtmlUtil.href(metadata.getAttr4(),metadata.getAttr4());
            }
        } else {
            content =  metadata.getAttr1();
        }
        if(content == null) return null;
        return new String[]{lbl, content};
    }


    public Metadata makeMetadataFromCatalogNode(Element child) {
        String  tag   = child.getTagName();
        if (tag.equals(TAG_DOCUMENTATION)) {
            if (XmlUtil.hasAttribute(child, "xlink:href")) {
                String url = XmlUtil.getAttribute(child, "xlink:href");
                return new Metadata(getRepository().getGUID(),"", TAG_LINK,
                                    XmlUtil.getAttribute(child, "xlink:title", url),
                                    url,"","");
            } else {
                String type = XmlUtil.getAttribute(child, "type");
                String text = XmlUtil.getChildText(child).trim();
                return new Metadata(getRepository().getGUID(),"",tag, type, text,"","");
            }
        } else if (tag.equals(TAG_PROJECT)) {
            String text = XmlUtil.getChildText(child).trim();
            return new Metadata(getRepository().getGUID(),"",tag, text,
                                XmlUtil.getAttribute(child, ATTR_VOCABULARY,""),"","");
        } else if (tag.equals(TAG_CONTRIBUTOR)) {
            String text = XmlUtil.getChildText(child).trim();
            return new Metadata(getRepository().getGUID(),"",tag, text,
                                XmlUtil.getAttribute(child, ATTR_ROLE,""),"","");
        } else if (tag.equals(TAG_PUBLISHER) || tag.equals(TAG_CREATOR)) {
            Element nameNode  = XmlUtil.findChild(child, TAG_NAME);
            String name = XmlUtil.getChildText(nameNode).trim();
            String vocabulary = XmlUtil.getAttribute(nameNode, ATTR_VOCABULARY,"");
            String email ="";
            String url = "";
            Element contactNode  = XmlUtil.findChild(child, TAG_CONTACT);
            if(contactNode!=null) {
                email = XmlUtil.getAttribute(contactNode, ATTR_EMAIL,"");
                url = XmlUtil.getAttribute(contactNode, ATTR_URL,"");
            }
            return new Metadata(getRepository().getGUID(),"",tag, name,
                                vocabulary,
                                email, url);
        } else if (tag.equals(TAG_KEYWORD)
                   || tag.equals(TAG_AUTHORITY)
                   || tag.equals(TAG_DATATYPE)
                   || tag.equals(TAG_DATAFORMAT)) {
            String text = XmlUtil.getChildText(child).trim();
            return new Metadata(getRepository().getGUID(),"",tag, text,"","","");
        } else if (tag.equals(TAG_VOCABULARY)
                   || tag.equals(TAG_VARIABLES)) {
            String text = XmlUtil.toString(child, false);
            return new Metadata(getRepository().getGUID(),"",tag, text,"","","");
        } else if (tag.equals(TAG_PROPERTY)) {
            return  new Metadata(getRepository().getGUID(),"",tag,
                                 XmlUtil.getAttribute(child, ATTR_NAME),
                                 XmlUtil.getAttribute(child, ATTR_VALUE),"","");
        }  
        return null;
    }




}

