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

package ucar.unidata.repository.type;




import org.w3c.dom.*;

import ucar.unidata.repository.*;

import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;


import java.util.Hashtable;





/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class BlobTypeHandler extends GenericTypeHandler {


    /** _more_ */
    private Hashtable<String, Hashtable> propertiesCache =
        new Hashtable<String, Hashtable>();

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public BlobTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Hashtable getProperties(Entry entry) throws Exception {
        Hashtable properties = propertiesCache.get(entry.getId());
        if (properties == null) {
            Object[]   values     = entry.getValues();
            XmlEncoder xmlEncoder = new XmlEncoder();
            if ((values != null) && (values.length > 0)
                    && (values[0] != null)) {
                properties =
                    (Hashtable) xmlEncoder.decodeXml((String) values[0]);
            } else {
                properties = new Hashtable();
            }
            propertiesCache.put(entry.getId(), properties);
        }
        return properties;
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param properties _more_
     *
     * @throws Exception _more_
     */
    protected void setProperties(Entry entry, Hashtable properties)
            throws Exception {
        XmlEncoder xmlEncoder = new XmlEncoder();
        entry.setValues(new Object[] { xmlEncoder.encodeObject(properties) });
    }



}

