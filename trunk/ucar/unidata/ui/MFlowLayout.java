/*
 * $Id: MFlowLayout.java,v 1.9 2007/07/06 20:45:31 jeffmc Exp $
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


/**
 * Extends java.awt.FlowLayout, which has a bug where it cant deal with multiple lines.
 * @author John Caron
 * @version $Id: MFlowLayout.java,v 1.9 2007/07/06 20:45:31 jeffmc Exp $
 */

public class MFlowLayout extends FlowLayout {

    /**
     * _more_
     *
     * @param align
     * @param hgap
     * @param vgap
     *
     */
    public MFlowLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    // deal with having components on more than one line

    /**
     * _more_
     *
     * @param target
     * @return _more_
     */
    public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);

            for (int i = 0; i < target.getComponentCount(); i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = m.getPreferredSize();

                    // original
                    // dim.height = Math.max(dim.height, d.height);
                    //if (i > 0) { dim.width += hgap; }
                    // dim.width += d.width;

                    // new  way
                    Point p = m.getLocation();
                    dim.width  = Math.max(dim.width, p.x + d.width);
                    dim.height = Math.max(dim.height, p.y + d.height);
                }
            }
            Insets insets = target.getInsets();
            dim.width  += insets.left + insets.right + getHgap() * 2;
            dim.height += insets.top + insets.bottom + getVgap() * 2;
            return dim;
        }
    }
}

/*
 *  Change History:
 *  $Log: MFlowLayout.java,v $
 *  Revision 1.9  2007/07/06 20:45:31  jeffmc
 *  A big J&J
 *
 *  Revision 1.8  2005/05/13 18:31:48  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.7  2004/09/07 18:36:23  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.6  2004/02/27 21:19:18  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.5  2004/01/29 17:37:11  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.4  2000/08/18 04:15:55  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.3  1999/06/03 01:44:11  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:27:06  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:47  caron
 *  startAgain
 *
 * # Revision 1.3  1999/03/26  19:58:27  caron
 * # add SpatialSet; update javadocs
 * #
 */






