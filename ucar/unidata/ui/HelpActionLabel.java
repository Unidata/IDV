/**
 * $Id: HelpActionLabel.java,v 1.2 2006/12/27 19:56:55 jeffmc Exp $
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

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.*;


/**
 * Class HelpActionLabel is used as a lightweight component in the java help.
 * It allows for actions to be routed to a global ActionListener
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.2 $
 */
public class HelpActionLabel extends JButton implements ActionListener {


    /** The global action listener */
    private static ActionListener actionListener;


    /** The action */
    String helpAction = null;

    /** The text */
    String helpText = "";


    /** The cursor to use when mouse is over us */
    private final static Cursor handCursor =
        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    /** default cursor */
    private Cursor origCursor;

    /**
     * ctor
     */
    public HelpActionLabel() {
        addActionListener(this);
        origCursor = getCursor();
        addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {}

            public void mouseEntered(MouseEvent e) {
                setCursor(handCursor);
            }

            public void mouseExited(MouseEvent e) {
                setCursor(origCursor);
            }
            public void mousePressed(MouseEvent e) {}

            public void mouseReleased(MouseEvent e) {}
        });


    }


    /**
     * set the global action listener
     *
     * @param actionListener the action listener
     */
    public static void setActionListener(ActionListener actionListener) {
        HelpActionLabel.actionListener = actionListener;
    }


    /**
     * Handle the action
     *
     * @param e event
     */
    public void actionPerformed(ActionEvent e) {
        if ((actionListener != null) && (helpAction != null)) {
            actionListener.actionPerformed(new ActionEvent(this, 0,
                    helpAction));
        }
    }


    /**
     * Sets the style of the button
     */
    private void setStyle() {
        setBorder(new EmptyBorder(1, 1, 1, 1));
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBackground(UIManager.getColor("EditorPane.background"));
        setForeground(Color.blue);
        invalidate();
    }


    /**
     * Set the style for the icon mode
     */
    private void setIconStyle() {
        setAlignmentY(getPreferredLabelAlignment());
        setStyle();
    }


    /**
     * Sets the style of the button
     */
    private void setTextStyle() {
        setAlignmentY(0.8f);
        setForeground(Color.blue);
        setStyle();
    }




    /**
     * Determine the alignment offset so the text is aligned with other views
     * correctly.
     *
     * @return alignment
     */
    private float getPreferredLabelAlignment() {
        Icon        icon  = (Icon) getIcon();
        String      text  = getText();

        Font        font  = getFont();
        FontMetrics fm    = getToolkit().getFontMetrics(font);

        Rectangle   iconR = new Rectangle();
        Rectangle   textR = new Rectangle();
        Rectangle   viewR = new Rectangle(Short.MAX_VALUE, Short.MAX_VALUE);

        SwingUtilities.layoutCompoundLabel(this, fm, text, icon,
                                           getVerticalAlignment(),
                                           getHorizontalAlignment(),
                                           getVerticalTextPosition(),
                                           getHorizontalTextPosition(),
                                           viewR, iconR, textR,
                                           ((text == null)
                                            ? 0
                                            : ((BasicButtonUI) ui)
                                            .getDefaultTextIconGap(this)));

        /* The preferred size of the button is the size of
         * the text and icon rectangles plus the buttons insets.
         */
        Rectangle r      = iconR.union(textR);

        Insets    insets = getInsets();
        r.height += insets.top + insets.bottom;

        /* Ensure that the height of the button is odd,
         * to allow for the focus line.
         */
        if (r.height % 2 == 0) {
            r.height += 1;
        }

        float offAmt = fm.getMaxAscent() + insets.top;
        return offAmt / (float) r.height;
    }


    /**
     * set the tool tip
     *
     * @param s tooltip
     */
    public void setHelpTooltip(String s) {
        setToolTipText(s);
    }


    /**
     * Set the help text
     *
     * @param s text
     */
    public void setHelpText(String s) {
        helpText = s;
        super.setText(s);
        setTextStyle();
    }


    /**
     * Set the icon
     *
     * @param s icon
     */
    public void setHelpIcon(String s) {
        setIconStyle();
        setIcon(ucar.unidata.util.GuiUtils.getImageIcon(s, getClass()));
    }



    /**
     * Set the HelpAction property.
     *
     * @param value The new value for HelpAction
     */
    public void setHelpAction(String value) {
        helpAction = value;
    }


}

