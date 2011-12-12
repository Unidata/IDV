/*
 * $Id: RadioButtonFileSelector.java,v 1.9 2007/07/06 20:45:33 jeffmc Exp $
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



import java.awt.GridLayout;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;


/**
 *  A ScrollPane that presents a list of file names as a series of
 *  JRadioButtons.  When a button is selected, an ItemEvent.SELECTED
 *  event is thrown.  The default layout is 4 columns, but this
 *  can be changed in the constructor or by using the setColumns method.
 */
public class RadioButtonFileSelector extends JScrollPane implements ActionListener,
        ItemSelectable {

    /** _more_ */
    private ButtonGroup group = null;

    /** _more_ */
    private JPanel groupPanel;

    /** _more_ */
    private JRadioButton selected = null;

    /** _more_ */
    private Vector listeners = new Vector();

    /** _more_ */
    private int columns;


    /**
     * Construct an empty RadioButtonFileSelector with the specified
     * Border title and the default number of columns
     *
     * @param title   selector title
     *
     */
    public RadioButtonFileSelector(String title) {
        this(title, new String[0], 4);
    }

    /**
     * Construct an empty RadioButtonFileSelector with the specified
     * Border title and the specified number of columns
     *
     * @param title   selector title
     * @param  columns      number of columns per row.
     *
     */
    public RadioButtonFileSelector(String title, int columns) {
        this(title, new String[0], columns);
    }

    /**
     * Construct a new RadioButtonFileSelector from a list of names of
     * buttons with the specified title and default number of columns.
     *
     * @param  buttonNames  list of the names of the buttons
     *
     */
    public RadioButtonFileSelector(String[] buttonNames) {
        this("", buttonNames, 4);
    }

    /**
     * Construct a new RadioButtonFileSelector from a list of names of
     * buttons with the specified title and the specified number of columns.
     *
     * @param  buttonNames  list of the names of the buttons
     * @param  columns      number of columns per row.
     *
     */
    public RadioButtonFileSelector(String[] buttonNames, int columns) {
        this("", buttonNames, columns);
    }

    /**
     * Construct a new RadioButtonFileSelector from a list of names of
     * buttons with the specified title and default number of rows.
     *
     * @param  title        title for the border around the panel
     * @param  buttonNames  list of the names of the buttons
     *
     */
    public RadioButtonFileSelector(String title, String[] buttonNames) {
        this(title, buttonNames, 4);
    }

    /**
     * Construct a new RadioButtonFileSelector from a list of names of
     * buttons with the specified title and set the number of columns
     * of buttons per row.
     *
     * @param  title        title for the border around the panel
     * @param  buttonNames  list of the names of the buttons
     * @param  columns      number of columns per row.
     *
     */
    public RadioButtonFileSelector(String title, String[] buttonNames,
                                   int columns) {

        groupPanel = new JPanel();
        this.setBorder(title.equals("")
                       ? BorderFactory.createEtchedBorder()
                       : BorderFactory.createTitledBorder(new EtchedBorder(),
                       title, TitledBorder.LEFT, TitledBorder.TOP));
        this.columns = (columns <= 0)
                       ? 4
                       : columns;
        groupPanel.setLayout(new GridLayout(0, this.columns));
        getViewport().setView(groupPanel);
        setViewportBorder(
            BorderFactory.createBevelBorder(BevelBorder.LOWERED));

        group = new ButtonGroup();
        if (buttonNames.length > 0) {
            setButtonList(buttonNames);
        }
    }

    /**
     * Set the border title
     *
     * @param   title   title to put on the border.  Titles are left
     *                  justified.
     */
    public void setTitle(String title) {
        this.setBorder(BorderFactory.createTitledBorder(new EtchedBorder(),
                title, TitledBorder.LEFT, TitledBorder.TOP));
    }

    /**
     * Set the list of buttons
     *
     * @param  buttonNames  list of button names
     */
    public void setButtonList(String[] buttonNames) {
        // clear out the existing buttons (if any)
        clearEntries();

        // now add in the new ones
        for (int i = 0; i < buttonNames.length; i++) {
            String       name = buttonNames[i];
            JRadioButton rb   = new JRadioButton(name);
            rb.setName(name);
            rb.addActionListener(this);
            group.add(rb);
            groupPanel.add(rb);
        }
        revalidate();
    }

    /**
     * Set the number of columns in this panel
     *
     * @param  columns  number of columns of buttons
     */
    public void setColumns(int columns) {
        if (columns <= 0) {
            return;
        }
        this.columns = columns;
        groupPanel.setLayout(new GridLayout(0, this.columns));
        groupPanel.validate();
    }

    /**
     * Clear the entries in the panel
     */
    public void clearEntries() {
        // clear out the existing buttons (if any)
        groupPanel.removeAll();
        Enumeration e = group.getElements();
        while (e.hasMoreElements()) {
            AbstractButton ab = (AbstractButton) e.nextElement();
            group.remove(ab);
            ab.removeActionListener(this);
        }
        revalidate();
        repaint();
    }

    /**
     * Run "java ucar.unidata.ui.RadioButtonFileSelector" to test
     *
     * @param args
     */
    public static void main(String[] args) {
        String[] names = new String[6];
        names[0] = "GOES-E";
        names[1] = "GOES-W";
        names[2] = "Meteosat";
        names[3] = "GMS";
        names[4] = "NIDS";
        names[5] = "NOWrad";

        RadioButtonFileSelector rbp = (args.length == 0)
                                      ? new RadioButtonFileSelector(names)
                                      : new RadioButtonFileSelector(args[0],
                                          names);
        JFrame frame = new JFrame();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.getContentPane().add(rbp);
        frame.setSize(400, 200);
        frame.pack();
        frame.setVisible(true);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            ;
        }

        System.out.println("Now changing parameters");
        rbp.setTitle("Now changing the buttons");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            ;
        }

        names    = new String[6];
        names[0] = "Tom";
        names[1] = "Don";
        names[2] = "Chiz";
        names[3] = "Russ";
        names[4] = "Steve";
        names[5] = "Robb";

        rbp.setButtonList(names);
        rbp.setTitle("Software Engineers");

        System.out.println("Now changing column number");
        rbp.setTitle("Now changing column number");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            ;
        }
        rbp.setColumns(2);
    }

    /**
     * ActionListener method
     *
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        selected = (JRadioButton) e.getSource();
        fireItemStateChanged(new ItemEvent(this,
                                           ItemEvent.ITEM_STATE_CHANGED,
                                           e.getSource(),
                                           ItemEvent.SELECTED));
    }

    /**
     *  Adds the specified item listener to receive item events from this
     *  ojbect.
     *
     * @param l
     */
    public void addItemListener(ItemListener l) {
        listeners.add(l);
    }

    /**
     *  Removes the specified item listener so that the item listener
     *  no longer receives item events from this object.
     *
     * @param l
     */
    public void removeItemListener(ItemListener l) {
        listeners.remove(l);

    }

    /**
     * Returns an array (length 1) containing the selected radio button
     * or null if no radio buttons are selected.
     * @return _more_
     */
    public Object[] getSelectedObjects() {
        return new Object[] { selected };
    }

    /**
     * Notify ItemListeners when a radio button is selected
     *
     * @param e
     */
    protected void fireItemStateChanged(ItemEvent e) {
        for (int i = 0; i < listeners.size(); i++) {
            ((ItemListener) listeners.get(i)).itemStateChanged(e);
        }
    }
}

