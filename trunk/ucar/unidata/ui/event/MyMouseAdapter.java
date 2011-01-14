/*
 * $Id: MyMouseAdapter.java,v 1.5 2005/05/13 18:32:16 jeffmc Exp $
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

package ucar.unidata.ui.event;



import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


/**
 * Mouse adapter
 *
 * @author MetApps Development Team
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:32:16 $
 */
public class MyMouseAdapter extends java.awt.event.MouseAdapter {

    /** _more_ */
    private int startx, starty;

    /** _more_ */
    private int minMove = 4;

    /** _more_ */
    private boolean debugEvent = false;

    /**
     * _more_
     *
     * @param e
     */
    public void mousePressed(MouseEvent e) {
        startx = e.getX();
        starty = e.getY();

        if (debugEvent) {
            System.out.println("mousePressed " + startx + " " + starty);
        }
    }

    /**
     * _more_
     *
     * @param e
     */
    public void mouseReleased(MouseEvent e) {
        int deltax = e.getX() - startx;
        int deltay = e.getY() - starty;
        int total  = Math.abs(deltax) + Math.abs(deltay);
        if (total <= minMove) {
            click(e);
        } else {
            drag(e, deltax, deltay);
        }

        if (debugEvent) {
            System.out.println("mouseReleased " + e.getX() + " " + e.getY());
            if ((deltax > 0) || (deltay > 0)) {
                System.out.println("  MOVED " + deltax + " " + deltay);
            }
        }
    }

    /**
     * _more_
     *
     * @param minMove
     */
    public void setMinMove(int minMove) {
        this.minMove = minMove;
    }

    /// subclasses should override

    /**
     * _more_
     *
     * @param e
     */
    public void click(MouseEvent e) {}  // System.out.println( "click"); }

    /**
     * _more_
     *
     * @param e
     * @param deltax
     * @param deltay
     */
    public void drag(MouseEvent e, int deltax, int deltay) {}

    // System.out.println( "drag: "+deltax+" "+deltay);}

    /**
     * _more_
     *
     * @param args
     */
    public static void main(String args[]) {

        JFrame frame = new JFrame("Test MyMouseAdapter");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        JLabel comp = new JLabel("test  sdfk sdf ks;dflk ;sdlkf ldsk lk");
        comp.setOpaque(true);
        comp.setBackground(Color.white);
        comp.setForeground(Color.black);

        comp.addMouseListener(new MyMouseAdapter());

        JPanel main = new JPanel(new FlowLayout());
        frame.getContentPane().add(main);
        main.setPreferredSize(new Dimension(200, 200));
        main.add(comp);

        frame.pack();
        frame.setLocation(300, 300);
        frame.setVisible(true);
    }


}





