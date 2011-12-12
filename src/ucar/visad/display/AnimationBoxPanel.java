/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.visad.display;


import ucar.unidata.collab.*;

import ucar.unidata.ui.XmlUi;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.Misc;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Resource;
import ucar.unidata.xml.XmlUtil;



import ucar.visad.display.*;

import visad.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

import java.beans.*;

import java.rmi.RemoteException;


import java.util.ArrayList;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;




/**
 * A widget to graphically display animation times
 *
 * @author IDV Development Team
 * @version $Revision: 1.18 $
 */
public class AnimationBoxPanel extends JPanel implements MouseListener,
        MouseMotionListener, KeyListener {


    /** box color when ok */
    private static final Color COLOR_OK = Color.green;

    /** box color when turned off */
    private static final Color COLOR_NOTOK = Color.red;

    /** box color for current anim step */
    private static final Color COLOR_ON = Color.blue;

    /** Box height */
    private static final int BOX_HEIGHT = 6;

    /** Box height */
    private int myHeight = BOX_HEIGHT;

    /** Vertical space */
    private static final int SPACE_V = 2;

    /** Box width */
    private static final int BOX_WIDTH = 10;

    /** Hor space */
    private static final int SPACE_H = 3;

    /** The widget */
    private AnimationWidget widget;


    /** _more_ */
    private Object BOXES_MUTEX = new Object();

    /** List of boxes to draw */
    private List myBoxes = new ArrayList();



    /** Index of box we are currently over. Used to draw the highlight */
    private int overBoxIndex = -1;

    /** used when dragging */
    private boolean[] copyForDrag;

    /** used when dragging */
    private int dragStartIndex;

    /** used when dragging */
    private boolean dragStartOk;

    /** used when dragging */
    private int dragCurrentIndex;

    /** Is the number key currently pressed */
    private boolean numberKeyDown = false;

    /** The number key */
    private int numberKey = 0;


    /**
     * Default Constructor
     *
     * @param widget The widget
     */
    public AnimationBoxPanel(AnimationWidget widget) {
        this(widget, null);
    }

    /**
     * Default Constructor
     *
     * @param widget The widget
     * @param okArray initial array of what is
     */
    public AnimationBoxPanel(AnimationWidget widget, boolean[] okArray) {
        this.widget = widget;
        if (widget == null) {
            myHeight = BOX_HEIGHT * 2;
        }
        setPreferredSize(new Dimension(100, BOX_HEIGHT + SPACE_V * 2));
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        setToolTipText(
            "<html>Click: toggle<br>Control-click:toggle left<br>Shift-click: toggle right</html>");
        if (okArray != null) {
            setNumTimes(okArray.length, okArray);
        }
    }

    /**
     * handle event
     *
     * @param e  event
     */
    public void keyPressed(KeyEvent e) {
        numberKeyDown = false;
        char c = e.getKeyChar();
        try {
            numberKey = Integer.parseInt("" + c);
            if (numberKey > 0) {
                numberKeyDown = true;
            }
        } catch (Exception exc) {}

    }

    /**
     * handle event
     *
     * @param e event
     */
    public void keyReleased(KeyEvent e) {
        numberKeyDown = false;
    }

    /**
     * handle event
     *
     * @param e  event
     */
    public void keyTyped(KeyEvent e) {}


    /**
     * Get the tooltip
     *
     * @param event event
     *
     * @return The tooltip
     */
    public String getToolTipText(MouseEvent event) {
        if (widget == null) {
            return super.getToolTipText(event);
        }
        return getToolTipText();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getToolTipText() {
        if (widget == null) {
            return "";
        }
        String text = "";
        if ((overBoxIndex >= 0) && (widget != null)) {
            DateTime dttm = widget.getTimeAtIndex(overBoxIndex);
            if (dttm != null) {
                text = "Time:" + dttm + " (" + (overBoxIndex + 1) + " of "
                       + myBoxes.size() + ")";
            }
        } else {
            text = text + "Number of times:" + myBoxes.size();
        }
        DateTime startTime = widget.getTimeAtIndex(0);
        DateTime endTime   = widget.getTimeAtIndex(myBoxes.size() - 1);
        if ((startTime != null) && (endTime != null)) {
            text = text + "<br>" + "Range:" + startTime + " - " + endTime;
        }

        return "<html>" + text + "</html>";

    }


    /**
     * Paint the boxes
     *
     * @param graphics graphics
     */
    public void paint(Graphics graphics) {
        super.paint(graphics);
        Graphics2D g      = (Graphics2D) graphics;
        Rectangle  bounds = getBounds();
        double     width  = BOX_WIDTH;
        double     space  = SPACE_H;
        List       boxes  = getBoxes();
        int        size   = boxes.size();
        if (size == 0) {
            return;
        }
        while ((space >= 1.0) && (bounds.width < (size * (width + space)))) {
            space--;
        }

        while ((width >= 4.0) && (bounds.width < (size * (width + space)))) {
            width--;
        }
        boolean discrete = true;
        if (bounds.width < (size * (width + space))) {
            width    = bounds.width / (double) size;
            discrete = false;
        }
        double x      = 0;
        int    iWidth = (int) width;
        if (iWidth <= 0) {
            iWidth = 1;
        }
        Box onBox = null;
        if ( !discrete) {
            g.setColor(COLOR_OK);
            g.fillRect(1, SPACE_V, bounds.width, myHeight);
        }
        for (int i = 0; i < boxes.size(); i++) {
            Box box = (Box) boxes.get(i);
            if (box.on) {
                onBox = box;
                g.setColor(COLOR_ON);
            } else if (box.ok) {
                g.setColor(COLOR_OK);
            } else {
                g.setColor(COLOR_NOTOK);
            }
            if (discrete || !box.ok) {
                g.fillRect((int) x, SPACE_V, iWidth, myHeight);
            }
            //Only draw the outline if the boxes are somewhat wide
            if (width >= 4.0) {
                if (i == overBoxIndex) {
                    g.setColor(Color.darkGray);
                } else {
                    g.setColor(Color.gray);
                }
                g.drawRect((int) x, SPACE_V, iWidth, myHeight);
            }
            box.setRect(x, (double) SPACE_V, iWidth, (double) myHeight);
            x = x + width + space;
        }

        if (onBox != null) {
            g.setColor(COLOR_ON);
            g.fillRect((int) onBox.getX(), SPACE_V, iWidth, myHeight);
        }
    }

    /**
     * set the current index to on
     *
     * @param index current index
     */
    public void setOnIndex(int index) {
        List boxes = getBoxes();
        for (int i = 0; i < boxes.size(); i++) {
            Box box = (Box) boxes.get(i);
            box.on = (index == i);
        }
        repaint();
    }


    /**
     * Set the number of boxes
     *
     * @param numTimes number of times
     */
    public void setNumTimes(int numTimes) {
        setNumTimes(numTimes, null);
    }

    /**
     * Set the number of boxes. If okArray non-null then use its values.
     *
     * @param numTimes Number of times
     * @param okArray Values for boxes
     */
    public void setNumTimes(int numTimes, boolean[] okArray) {
        synchronized (BOXES_MUTEX) {
            if (numTimes <= 0) {
                myBoxes = new ArrayList();
                repaint();
                return;
            }

            boolean changed = false;
            while (myBoxes.size() < numTimes) {
                myBoxes.add(new Box());
                changed = true;
            }
            while (myBoxes.size() > numTimes) {
                myBoxes.remove(myBoxes.size() - 1);
                changed = true;
            }
            if (okArray != null) {
                for (int onIdx = 0;
                        (onIdx < okArray.length) && (onIdx < myBoxes.size());
                        onIdx++) {
                    ((Box) myBoxes.get(onIdx)).ok = okArray[onIdx];
                }
            }


            if (changed) {
                checkStepsOk();
                repaint();
            }
        }
    }


    /**
     * Get the list of boxes
     *
     * @return List of Box-es
     */
    public List getBoxes() {
        synchronized (BOXES_MUTEX) {
            List tmp = new ArrayList(myBoxes);
            return tmp;
        }
    }

    /**
     * Apply the properties from the that
     *
     * @param that Object to get properties from
     */
    protected void applyProperties(AnimationBoxPanel that) {
        boolean[] stepsOk = that.getStepsOk();
        setNumTimes(stepsOk.length, stepsOk);
        checkStepsOk();
        repaint();
    }

    /**
     * Class Box represents a time step
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.18 $
     */
    public static class Box extends Rectangle2D.Double {

        /** Ok */
        public boolean ok = true;

        /** Is the current one */
        public boolean on = false;

        /**
         * ctor
         */
        public Box() {}

    }


    /**
     * Find the box at the given location
     *
     * @param x x
     * @param y y
     *
     * @return The box that contains x,y or null if none found
     */
    private int findBox(int x, int y) {
        List boxes = getBoxes();
        for (int i = 0; i < boxes.size(); i++) {
            Box box = (Box) boxes.get(i);
            if ((x >= box.getX()) && (x < box.getX() + box.getWidth())) {
                return i;
            }
        }

        int closestIndex = -1;
        int minDistance  = Integer.MAX_VALUE;
        for (int i = 0; i < boxes.size(); i++) {
            Box box = (Box) boxes.get(i);
            int tmp = (int) Math.abs(x - box.getX());
            if ((tmp < minDistance) && (tmp < 4)) {
                closestIndex = i;
                minDistance  = tmp;
            }
        }
        return closestIndex;
    }


    /**
     * Handle event
     *
     * @param event event
     */
    public void mousePressed(MouseEvent event) {
        requestFocus();
        int theBoxIndex = findBox(event.getX(), event.getY());
        if (theBoxIndex < 0) {
            return;
        }


        if ( !event.isControlDown() && !event.isShiftDown()
                && !SwingUtilities.isRightMouseButton(event)) {
            if (widget != null) {
                widget.gotoIndex(theBoxIndex);
            }
            return;
        }

        List boxes = getBoxes();
        Box  box   = (Box) boxes.get(theBoxIndex);
        box.ok = !box.ok;
        boolean ok = box.ok;
        if (event.isControlDown()) {
            for (int i = theBoxIndex - 1; i >= 0; i--) {
                box    = (Box) boxes.get(i);
                box.ok = ok;
            }
        } else if (event.isShiftDown()) {
            for (int i = theBoxIndex + 1; i < boxes.size(); i++) {
                box    = (Box) boxes.get(i);
                box.ok = ok;
            }
        }
        checkStepsOk();
        repaint();

        if (copyForDrag == null) {
            dragStartIndex = theBoxIndex;
            if (dragStartIndex < 0) {
                return;
            }
            copyForDrag = getStepsOk();
            dragStartOk = copyForDrag[dragStartIndex];
        }


        if (widget != null) {
            widget.boxPanelChanged(this);
        }



    }


    /**
     * Get the array of boolean that shows what steps are ok
     *
     * @return Array of the steps ok from the boxes
     */
    public boolean[] getStepsOk() {
        List      boxes   = getBoxes();
        boolean[] stepsOk = new boolean[boxes.size()];
        for (int i = 0; i < boxes.size(); i++) {
            stepsOk[i] = ((Box) boxes.get(i)).ok;
        }
        return stepsOk;
    }




    /**
     * Tell the widget we changed.
     */
    private void checkStepsOk() {
        if (widget != null) {
            widget.stepsOkChanged(getStepsOk());
        }
    }


    /**
     * Handle event
     *
     * @param e event
     */
    public void mouseMoved(MouseEvent e) {
        int index = findBox(e.getX(), e.getY());
        if (overBoxIndex != index) {
            overBoxIndex = index;
            repaint();
        }
    }

    /**
     * noop
     *
     * @param e event
     */
    public void mouseEntered(MouseEvent e) {}

    /**
     * Noop
     *
     * @param e The event
     */
    public void mouseExited(MouseEvent e) {
        overBoxIndex = -1;
    }

    /**
     * Noop
     *
     * @param event event
     */
    public void mouseClicked(MouseEvent event) {}



    /**
     * Noop
     *
     * @param event event
     */
    public void mouseDragged(MouseEvent event) {
        int index = findBox(event.getX(), myHeight / 2);
        if (index < 0) {
            return;
        }
        if (copyForDrag == null) {
            return;
        } else if (index == dragCurrentIndex) {
            return;
        }
        dragCurrentIndex = index;
        List boxes = getBoxes();
        for (int i = 0; (i < copyForDrag.length) && (i < boxes.size()); i++) {
            ((Box) boxes.get(i)).ok = copyForDrag[i];
        }
        int count = 0;
        if (dragStartIndex < index) {
            for (int i = dragStartIndex; (i <= index) && (i < boxes.size());
                    i++) {
                if (numberKeyDown && (count % numberKey) != 0) {
                    ((Box) boxes.get(i)).ok = !dragStartOk;

                } else {
                    ((Box) boxes.get(i)).ok = dragStartOk;
                }
                count++;
            }
        } else {
            for (int i = dragStartIndex;
                    (i >= index) && (i >= 0) && (i < boxes.size()); i--) {
                if (numberKeyDown && (count % numberKey) != 0) {
                    ((Box) boxes.get(i)).ok = !dragStartOk;

                } else {
                    ((Box) boxes.get(i)).ok = dragStartOk;
                }
                count++;
            }
        }
        checkStepsOk();
        if (widget != null) {
            widget.boxPanelChanged(this);
        }
        repaint();
    }

    /**
     * Noop
     *
     * @param event event
     */
    public void mouseReleased(MouseEvent event) {
        copyForDrag = null;
    }


}
