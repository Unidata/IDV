/*
 * $Id: Drawable.java,v 1.9 2007/07/06 20:45:30 jeffmc Exp $
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

package ucar.unidata.ui;



import java.awt.*;
import java.awt.geom.Point2D;


/**
 * These are the objects that the DrawingPanel manipulates.
 * @see ucar.unidata.ui.DrawingPanel
 * @author John Caron
 * @version $Id: Drawable.java,v 1.9 2007/07/06 20:45:30 jeffmc Exp $
 */
public interface Drawable {

    /**
     * draw offset from the specified location
     *
     * @param g
     * @param location
     */
    public void draw(Graphics2D g, Point2D location);

    /**
     * get the bounding box
     * @return _more_
     */
    public Rectangle getBounds();

    /**
     * get the name
     * @return _more_
     */
    public String getName();

    /**
     * return true if selected
     * @return _more_
     */
    public boolean isSelected();

    /**
     * return true if Active
     * @return _more_
     */
    public boolean isActive();

    /**
     * set the offset position
     *
     * @param x
     * @param y
     */
    public void setPosition(int x, int y);

    /**
     * set whether selected
     *
     * @param select
     */
    public void setSelected(boolean select);

    /**
     * set the bounding box
     *
     * @param width
     * @param height
     */
    public void setSize(int width, int height);


}

/*
 *  Change History:
 *  $Log: Drawable.java,v $
 *  Revision 1.9  2007/07/06 20:45:30  jeffmc
 *  A big J&J
 *
 *  Revision 1.8  2005/05/13 18:31:45  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.7  2004/09/07 18:36:21  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.6  2004/02/27 21:19:17  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.5  2004/01/29 17:37:09  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.4  2000/08/18 04:15:53  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.3  1999/06/03 01:44:09  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:27:05  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:47  caron
 *  startAgain
 *
 * # Revision 1.5  1999/03/26  19:58:15  caron
 * # add SpatialSet; update javadocs
 * #
 * # Revision 1.4  1999/03/16  17:00:24  caron
 * # fix StationModel editing; add TopLevel
 * #
 * # Revision 1.3  1999/02/15  23:06:37  caron
 * # upgrade to java2D, new ProjectionManager
 * #
 * # Revision 1.2  1998/12/14  17:11:57  russ
 * # Add comment for accumulating change histories.
 * #
 */






