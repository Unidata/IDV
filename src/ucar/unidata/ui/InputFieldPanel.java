/*
 * $Id: InputFieldPanel.java,v 1.16 2007/07/06 20:45:31 jeffmc Exp $
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

import javax.swing.*;
import javax.swing.border.*;


/**
 * Class InputFieldPanel
 *
 *
 * @author Unidata
 */

public class InputFieldPanel extends JPanel {

    /** _more_ */
    static public final int CENTER = 0;

    /** _more_ */
    static public final int LEFT = 0;

    /** _more_ */
    private GridBagConstraints gbcon, gbcSep;

    /** _more_ */
    private GridBagConstraints gbcLabel = new GridBagConstraints();

    /** _more_ */
    private GridBagConstraints gbcValue = new GridBagConstraints();

    // default is to center

    /**
     * _more_
     *
     */
    public InputFieldPanel() {
        this(InputFieldPanel.CENTER);
    }

    /**
     * _more_
     *
     * @param mode
     *
     */
    public InputFieldPanel(int mode) {
        super();
        setLayout(new GridBagLayout());
        //setBorder(new EtchedBorder());

        if (mode == CENTER) {
            gbcLabel.anchor    = GridBagConstraints.EAST;
            gbcLabel.gridwidth = 1;
            gbcValue.anchor    = GridBagConstraints.WEST;
            gbcValue.gridwidth = GridBagConstraints.REMAINDER;
        } else {
            gbcLabel.anchor    = GridBagConstraints.EAST;
            gbcLabel.gridwidth = 1;
            gbcValue.anchor    = GridBagConstraints.EAST;
            gbcValue.gridwidth = GridBagConstraints.REMAINDER;
        }

        // used by addHeading
        gbcon           = new GridBagConstraints();
        gbcon.fill      = GridBagConstraints.NONE;
        gbcon.anchor    = GridBagConstraints.NORTHWEST;
        gbcon.gridwidth = GridBagConstraints.REMAINDER;

        // used by addSeperator
        gbcSep           = new GridBagConstraints();
        gbcSep.fill      = GridBagConstraints.HORIZONTAL;
        gbcSep.insets    = new Insets(10, 0, 10, 0);
        gbcSep.gridwidth = GridBagConstraints.REMAINDER;
    }

    /**
     * _more_
     */
    public void addSeperator() {
        add(new JSeparator(), gbcSep);
    }

    /**
     * _more_
     *
     * @param head
     */
    public void addHeading(String head) {
        add(new JLabel(head), gbcon);
    }


    /**
     * Class InputField
     *
     *
     * @author Unidata
     */

    public abstract class InputField {

        /** _more_ */
        protected GridBagConstraints gbc = new GridBagConstraints();

        /** _more_ */
        protected JComponent editComp;

        /** _more_ */
        protected JLabel lab;

        /**
         * _more_
         *
         * @param label
         */
        protected void layout(String label) {
            JPanel panel = InputFieldPanel.this;
            lab = new JLabel(label + ": ");

            panel.add(Box.createHorizontalStrut(15), gbcLabel);
            panel.add(lab, gbcLabel);
            panel.add(Box.createHorizontalStrut(10));
            panel.add(editComp, gbcValue);
        }

        /**
         * _more_
         *
         * @param enable
         */
        public void setEnabled(boolean enable) {
            lab.setEnabled(enable);
            editComp.setEnabled(enable);
        }

        /**
         * _more_
         *
         * @param tip
         */
        public void setToolTipText(String tip) {
            lab.setToolTipText(tip);
            editComp.setToolTipText(tip);
        }

        /**
         * _more_
         * @return _more_
         */
        public int getSize() {
            return 0;
        }

        /**
         * _more_
         *
         * @param size
         */
        public void setSize(int size) {}
    }

    /**
     * Class TextField
     *
     *
     * @author Unidata
     */
    public class TextField extends InputField {

        /** _more_ */
        protected JTextField tf;

        /**
         * _more_
         *
         */
        protected TextField() {}  // needed for subclass

        /**
         * _more_
         *
         * @param label
         * @param defValue
         *
         */
        TextField(String label, String defValue) {
            int size = Math.max(5, defValue.length() + 3);
            tf = new JTextField(defValue, size);
            tf.setColumns(size);
            editComp = (JComponent) tf;
            layout(label);
        }

        /**
         * _more_
         * @return _more_
         */
        public String getText() {
            return tf.getText();
        }

        /**
         * _more_
         * @return _more_
         */
        public int getInt() {
            return Integer.parseInt(tf.getText());
        }

        /**
         * _more_
         *
         * @param enable
         */
        public void setEditable(boolean enable) {
            tf.setEditable(enable);
        }

        /**
         * _more_
         *
         * @param val
         */
        public void setInt(int val) {
            tf.setText(Integer.toString(val));
        }

        /**
         * _more_
         *
         * @param val
         */
        public void setText(String val) {
            tf.setText(val);
            int size = tf.getColumns();
            tf.setColumns(Math.max(size, val.length()));
        }

        /**
         * _more_
         * @return _more_
         */
        public int getSize() {
            return tf.getColumns();
        }

        /**
         * _more_
         *
         * @param size
         */
        public void setSize(int size) {
            tf.setColumns(size);
        }
    }

    /**
     * Class PasswordField
     *
     *
     * @author Unidata
     */
    public class PasswordField extends TextField {

        /**
         * _more_
         *
         * @param label
         * @param defValue
         *
         */
        PasswordField(String label, String defValue) {
            int size = Math.max(5, defValue.length() + 3);
            tf = new JPasswordField(defValue, size);
            tf.setColumns(size);
            editComp = (JComponent) tf;
            layout(label);
        }
    }

    /**
     * Class BooleanField
     *
     *
     * @author Unidata
     */
    public class BooleanField extends TextField {

        /**
         * _more_
         *
         * @param label
         * @param defValue
         *
         */
        BooleanField(String label, boolean defValue) {
            tf       = new JTextField(defValue
                                      ? "true"
                                      : "false", 5);
            editComp = (JComponent) tf;
            layout(label);
        }

        /**
         * _more_
         *
         * @param value
         */
        public void setValue(boolean value) {
            tf.setText(value
                       ? "true"
                       : "false");
            tf.invalidate();
            //System.out.println("  BooleanField "+ lab.getText()+" set to "+value);
        }

        /**
         * _more_
         * @return _more_
         */
        public boolean getBoolean() {
            return tf.getText().equals("true");
        }
    }

    /**
     * Class DoubleField
     *
     *
     * @author Unidata
     */
    public class DoubleField extends TextField {

        /**
         * _more_
         *
         * @param label
         * @param defValue
         *
         */
        DoubleField(String label, double defValue) {
            tf       = new JTextField(Double.toString(defValue), 8);
            editComp = (JComponent) tf;
            layout(label);
        }

        /**
         * _more_
         *
         * @param value
         */
        public void setValue(double value) {
            tf.setText(Double.toString(value));
            tf.invalidate();
            //System.out.println("  BooleanField "+ lab.getText()+" set to "+value);
        }

        /**
         * _more_
         * @return _more_
         */
        public double getDoubleValue() {
            return Double.parseDouble(tf.getText());
        }
    }

    /**
     * Class IntUnitField
     *
     *
     * @author Unidata
     */
    public class IntUnitField extends TextField {

        /**
         * _more_
         *
         * @param label
         * @param value
         * @param units
         *
         */
        IntUnitField(String label, int value, String units) {
            String defValue = Integer.toString(value);
            int    size     = Math.max(5, defValue.length() + 3);
            tf       = new JTextField(defValue, size);
            editComp = (JComponent) tf;

            // different layout
            JPanel panel = InputFieldPanel.this;
            panel.add(Box.createHorizontalStrut(15), gbcLabel);
            lab = new JLabel(label + ": ");
            panel.add(lab, gbcLabel);
            panel.add(Box.createHorizontalStrut(10));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = 1;
            gbc.anchor    = GridBagConstraints.WEST;
            panel.add(editComp, gbc);
            panel.add(Box.createHorizontalStrut(5), gbc);

            gbc.gridwidth = GridBagConstraints.REMAINDER;
            panel.add(new JLabel(units), gbc);
        }
    }

    /**
     * Class ComboField
     *
     *
     * @author Unidata
     */
    public class ComboField extends InputField {

        /** _more_ */
        private JComboBox cbox;

        /**
         * _more_
         *
         * @param label
         *
         */
        ComboField(String label) {
            cbox     = new JComboBox();
            editComp = (JComponent) cbox;
            layout(label);
        }

        /**
         * _more_
         *
         * @param s
         */
        public void add(String s) {
            cbox.addItem(s);
        }

        /**
         * _more_
         * @return _more_
         */
        public String getText() {
            return (String) cbox.getSelectedItem();
        }
    }

    /**
     * Class CheckBoxField
     *
     *
     * @author Unidata
     */
    public class CheckBoxField extends InputField {

        /** _more_ */
        private JCheckBox check;

        /**
         * _more_
         *
         * @param label
         * @param initValue
         *
         */
        CheckBoxField(String label, boolean initValue) {
            check = new JCheckBox();
            check.setSelected(initValue);
            editComp = (JComponent) check;
            layout(label);
        }

        /**
         * _more_
         * @return _more_
         */
        public boolean isSelected() {
            return check.isSelected();
        }

        /**
         * _more_
         *
         * @param v
         */
        public void setSelected(boolean v) {
            check.setSelected(v);
        }
    }

    /**
     * _more_
     *
     * @param label
     * @param val
     * @return _more_
     */
    public InputFieldPanel.BooleanField addBooleanField(String label,
            boolean val) {
        return new BooleanField(label, val);
    }

    /**
     * _more_
     *
     * @param label
     * @param val
     * @return _more_
     */
    public InputFieldPanel.DoubleField addDoubleField(String label,
            double val) {
        return new DoubleField(label, val);
    }

    /**
     * _more_
     *
     * @param label
     * @param val
     * @param unit
     * @return _more_
     */
    public InputFieldPanel.IntUnitField addIntUnitField(String label,
            int val, String unit) {
        return new IntUnitField(label, val, unit);
    }

    /**
     * _more_
     *
     * @param label
     * @param val
     * @return _more_
     */
    public InputFieldPanel.PasswordField addPasswordField(String label,
            String val) {
        return new PasswordField(label, val);
    }

    /**
     * _more_
     *
     * @param label
     * @param val
     * @return _more_
     */
    public InputFieldPanel.TextField addTextField(String label, String val) {
        return new TextField(label, val);
    }

    /**
     * _more_
     *
     * @param label
     * @param val
     * @return _more_
     */
    public InputFieldPanel.CheckBoxField addCheckBoxField(String label,
            boolean val) {
        return new CheckBoxField(label, val);
    }

}

