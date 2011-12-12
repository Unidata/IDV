/*
 * $Id: GisFeature.java,v 1.11 2005/05/13 18:29:32 jeffmc Exp $
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

package ucar.unidata.gis;



/**
 * An interface for GIS features, (analogous to ESRI Shapefile shapes).
 *
 * Created: Sat Feb 20 16:44:29 1999
 *
 * @author Russ Rew
 * @version $Id: GisFeature.java,v 1.11 2005/05/13 18:29:32 jeffmc Exp $
 */

public interface GisFeature {

    /**
     * Get the bounding box for this feature.
     *
     * @return rectangle bounding this feature
     */
    public java.awt.geom.Rectangle2D getBounds2D();

    /**
     * Get total number of points in all parts of this feature.
     *
     * @return total number of points in all parts of this feature.
     */
    public int getNumPoints();

    /**
     * Get number of parts comprising this feature.
     *
     * @return number of parts comprising this feature.
     */
    public int getNumParts();

    /**
     * Get the parts of this feature, in the form of an iterator.
     *
     * @return the iterator over the parts of this feature.  Each part
     * is a GisPart.
     */
    public java.util.Iterator getGisParts();

}  // GisFeature

/* Change History:
   $Log: GisFeature.java,v $
   Revision 1.11  2005/05/13 18:29:32  jeffmc
   Clean up the odd copyright symbols

   Revision 1.10  2005/03/10 18:38:28  jeffmc
   jindent and javadoc

   Revision 1.9  2004/01/29 17:35:19  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.8  2000/08/18 04:15:24  russ
   Licensed under GNU LGPL.

   Revision 1.7  2000/02/10 17:45:10  caron
   add GisFeatureRenderer,GisFeatureAdapter

*/







