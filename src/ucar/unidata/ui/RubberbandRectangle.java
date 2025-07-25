/*
 * $Id: RubberbandRectangle.java,v 1.8 2007/07/06 20:45:33 jeffmc Exp $
 *
 * Copyright  1997-2025 Unidata Program Center/University Corporation for
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


/**
 * Rectangle Rubberbanding.
 * @author David M. Geary
 * @author John Caron
 * @version $Id: RubberbandRectangle.java,v 1.8 2007/07/06 20:45:33 jeffmc Exp $
 */
public class RubberbandRectangle extends Rubberband {

    /**
     * _more_
     *
     */
    public RubberbandRectangle() {}

    /**
     * _more_
     *
     * @param component
     *
     */
    public RubberbandRectangle(Component component) {
        super(component);
    }

    /**
     * _more_
     *
     * @param graphics
     */
    public void drawLast(Graphics graphics) {
        Rectangle rect = lastBounds();
        graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * _more_
     *
     * @param graphics
     */
    public void drawNext(Graphics graphics) {
        Rectangle rect = getBounds();
        graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
    }
}

/*
 *  Change History:
 *  $Log: RubberbandRectangle.java,v $
 *  Revision 1.8  2007/07/06 20:45:33  jeffmc
 *  A big J&J
 *
 *  Revision 1.7  2005/05/13 18:31:52  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.6  2004/09/07 18:36:26  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.5  2004/01/29 17:37:13  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.4  2000/08/18 04:15:58  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.3  1999/06/03 01:44:14  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:27:09  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:48  caron
 *  startAgain
 *
 * # Revision 1.3  1999/03/26  19:58:38  caron
 * # add SpatialSet; update javadocs
 * #
 * # Revision 1.2  1998/12/14  17:12:01  russ
 * # Add comment for accumulating change histories.
 * #
 */






