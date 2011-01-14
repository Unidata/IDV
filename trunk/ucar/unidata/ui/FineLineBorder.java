/*
 * $Id: FineLineBorder.java,v 1.5 2007/07/06 20:45:30 jeffmc Exp $
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
import java.awt.event.*;

import java.text.DecimalFormat;

import javax.swing.*;
import javax.swing.border.*;

import javax.swing.event.*;


/**
 *  This overrides the BevelBorder paint methods to paint a single pixel
 *  lowered or raised border
 */
public class FineLineBorder extends BevelBorder {

    /**
     * _more_
     *
     * @param type
     *
     */
    public FineLineBorder(int type) {
        super(type);
    }


    /**
     * _more_
     *
     * @param c
     * @param g
     * @param x
     * @param y
     * @param width
     * @param height
     */
    protected void paintRaisedBevel(Component c, Graphics g, int x, int y,
                                    int width, int height) {
        Color oldColor = g.getColor();
        int   h        = height;
        int   w        = width;

        g.translate(x, y);

        g.setColor(getHighlightOuterColor(c));
        g.drawLine(0, 0, 0, h - 1);
        g.drawLine(1, 0, w - 1, 0);



        g.setColor(getShadowOuterColor(c));
        g.drawLine(1, h - 1, w - 1, h - 1);
        g.drawLine(w - 1, 1, w - 1, h - 2);

        g.translate(-x, -y);
        g.setColor(oldColor);

    }


    /**
     * _more_
     *
     * @param c
     * @param g
     * @param x
     * @param y
     * @param width
     * @param height
     */
    protected void paintLoweredBevel(Component c, Graphics g, int x, int y,
                                     int width, int height) {
        Color oldColor = g.getColor();
        int   h        = height;
        int   w        = width;

        g.translate(x, y);

        g.setColor(getShadowOuterColor(c));
        g.drawLine(1, 1, 1, h - 2);
        g.drawLine(2, 1, w - 2, 1);

        g.setColor(getHighlightOuterColor(c));
        g.drawLine(1, h - 1, w - 1, h - 1);
        g.drawLine(w - 1, 1, w - 1, h - 2);

        g.translate(-x, -y);
        g.setColor(oldColor);
    }


}

