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

import ucar.unidata.repository.data.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Image;


import java.io.*;
import java.io.File;
import java.io.FileInputStream;

import java.net.URL;
import java.net.URLConnection;


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
public class ContentMetadataHandler extends MetadataHandler {


    /** _more_ */
    public static final String TYPE_THUMBNAIL = "content.thumbnail";

    /** _more_ */
    public static final String TYPE_ATTACHMENT = "content.attachment";

    /** _more_ */
    public static final String TYPE_CONTACT = "content.contact";

    /** _more_ */
    public static final String TYPE_SORT = "content.sort";

    public static final String TYPE_ALIAS = "content.alias";


    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public ContentMetadataHandler(Repository repository) throws Exception {
        super(repository);
    }


}

