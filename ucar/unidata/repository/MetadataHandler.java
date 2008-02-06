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
public class MetadataHandler extends RepositoryManager {


    /** _more_          */
    private Hashtable canHandle = new Hashtable();

    /** _more_          */
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
     * @param type _more_
     *
     * @return _more_
     */
    public String getFormHtml(String type) {
        return "";
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

