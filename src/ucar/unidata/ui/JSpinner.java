/*
 * $Id: JSpinner.java,v 1.7 2007/07/06 20:45:31 jeffmc Exp $
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


/**
 * Spinner class
 *
 * @author MetApps Development Team
 * @version $Revision: 1.7 $ $Date: 2007/07/06 20:45:31 $
 */
public class JSpinner extends JPanel {

    /** _more_ */
    private JTextField tf;

    /** _more_ */
    private int value = 0;

    /** _more_ */
    private int minValue;

    /** _more_ */
    private int maxValue;

    /**
     * _more_
     *
     * @param initValue
     *
     */
    public JSpinner(int initValue) {
        this(initValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * _more_
     *
     * @param initValue
     * @param min
     * @param max
     *
     */
    public JSpinner(int initValue, int min, int max) {
        value         = initValue;
        this.minValue = min;
        this.maxValue = max;

        // the value field
        tf = new JTextField(1);
        tf.setEditable(false);
        javax.swing.border.Border border = tf.getBorder();
        tf.setBorder(BorderFactory.createEmptyBorder());
        setBorder(javax.swing.border.LineBorder.createBlackLineBorder());

        setValue(initValue);

        // create up & down buttons
        JPanel  udPanel = new JPanel(new GridLayout(2, 1));
        JButton up      = createButton(SpinIcon.TypeUp);
        udPanel.add(up);

        JButton down = createButton(SpinIcon.TypeDown);
        udPanel.add(down);

        add(tf);
        add(udPanel);

        // listeners
        up.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (value < maxValue) {
                    value++;
                }
                setValue(value);
            }
        });
        down.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (value > minValue) {
                    value--;
                }
                setValue(value);
            }
        });
    }

    /**
     * _more_
     * @return _more_
     */
    public int getValue() {
        return value;
    }

    /**
     * _more_
     *
     * @param newValue
     */
    public void setValue(int newValue) {
        value = newValue;
        tf.setText(Integer.toString(value));
        tf.invalidate();
    }

    /**
     * _more_
     *
     * @param type
     * @return _more_
     */
    private JButton createButton(SpinIcon.Type type) {
        SpinIcon icon = new SpinIcon(type);
        JButton  butt = new JButton(icon);
        Insets   i    = new Insets(0, 0, 0, 0);
        butt.setMargin(i);
        butt.setBorderPainted(false);
        butt.setFocusPainted(false);
        butt.setPreferredSize(new Dimension(icon.getIconWidth() + 2,
                                            icon.getIconHeight() + 2));
        return butt;
    }
}

