/*
 * $Id: WmsHandler.java,v 1.53 2007/07/09 22:59:58 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.util;

/**
 *
 * @author IDV development team
 * @version $Revision: 1.53 $Date: 2007/07/09 22:59:58 $
 */


public class WmsUtil {

    /** XML tag name for the &quot;Abstract&quot; tag */
    public static final String TAG_ABSTRACT = "Abstract";

    /** XML tag name for the &quot;Dimension&quot; tag */
    public static final String TAG_DIMENSION = "Dimension";

    /** XML tag name for the &quot;Layer&quot; tag */
    public static final String TAG_LAYER = "Layer";

    /** xml tag name */
    public static final String TAG_LATLONBOUNDINGBOX = "LatLonBoundingBox";

    /** xml tag name */
    public static final String TAG_BOUNDINGBOX = "BoundingBox";

    /** xml tag name */
    public static final String TAG_SRS = "SRS";

    /** xml tag name */
    public static final String TAG_CRS = "CRS";

    /** XML tag name for the &quot;Title&quot; tag */
    public static final String TAG_TITLE = "Title";

    /** XML tag name for the &quot;Style&quot; tag */
    public static final String TAG_STYLE = "Style";

    /** XML tag name for the &quot;Capability&quot; tag */
    public static final String TAG_CAPABILITY = "Capability";

    /**
     * This is one of the root document xml tags that I have seen
     * for a WMS cababilities document
     */
    public static final String TAG_WMS1 = "WMT_MS_Capabilities";

    /**
     * This is the ther root document xml tags that I have seen
     * for a WMS cababilities document
     */
    public static final String TAG_WMS2 = "WMS_Capabilities";

    /** xml attribute name */
    public static final String ATTR_FIXEDWIDTH = "fixedWidth";

    /** xml attribute name */
    public static final String ATTR_VERSION = "version";

    /** xml attribute name */
    public static final String ATTR_FIXEDHEIGHT = "fixedHeight";

    /** xml attribute name */
    public static final String ATTR_NAME = "name";


    /** xml attribute name */
    public static final String ATTR_NOSUBSETS = "noSubsets";

    /** xml attribute name */
    public static final String ATTR_MINX = "minx";

    /** xml attribute name */
    public static final String ATTR_MAXX = "maxx";

    /** xml attribute name */
    public static final String ATTR_MINY = "miny";

    /** xml attribute name */
    public static final String ATTR_MAXY = "maxy";


    /** xml attribute value */
    public static final String VALUE_TIME = "time";



}

