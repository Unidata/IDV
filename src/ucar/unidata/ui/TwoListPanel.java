/*
 * $Id: TwoListPanel.java,v 1.12 2007/07/06 20:45:34 jeffmc Exp $
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


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.TwoFacedObject;

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import javax.swing.*;
import javax.swing.event.*;




/**
 * This supports exchanging entries between  two jlists
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.12 $
 */
public class TwoListPanel extends JPanel implements ActionListener {

    /** Action command for moving the selected entry up */
    private static final String CMD_UP = "Up";

    /** Action command for moving the selected entry down */
    private static final String CMD_DOWN = "Down";

    /** Action command for inserting the action into the toolbar list */
    private static final String CMD_INSERT = "Insert";

    /** Action command for removing the selected toolbar entry */
    private static final String CMD_REMOVE = "Remove";

    /** Action command for reloading the toolbar list with the original items */
    private static final String CMD_RELOAD = "Reload Original";

    /** Keeps track if there were any changes */
    boolean changed = false;

    /** Keep track of the actions that are used in the toolbar list */
    Hashtable seenEntries = new Hashtable();

    /** Shows the toolbar entries */
    private JList toList = new JList();

    /** List of strings of actions in the toolbar */
    private Vector toEntries;

    /** Show the available actions */
    private JList fromList = new JList();

    /** List of strings of sourceEntries */
    private Vector fromEntries = new Vector();

    /** _more_ */
    private List originalFromEntries;

    /** _more_ */
    private List originalToEntries;

    /** _more_ */
    private boolean showUpDownButtons = true;

    /**
     * _more_
     *
     * @param fromEntries All available entries
     * @param fromLabel _more_
     * @param toEntries Currently use entries
     * @param toLabel _more_
     * @param extraButtons _more_
     */
    public TwoListPanel(List fromEntries, String fromLabel, List toEntries,
                        String toLabel, JComponent extraButtons) {
        this(fromEntries, fromLabel, toEntries, toLabel, extraButtons, true);
    }

    /**
     * _more_
     *
     * @param fromEntries _more_
     * @param fromLabel _more_
     * @param toEntries _more_
     * @param toLabel _more_
     * @param extraButtons _more_
     * @param showUpDownButtons _more_
     */
    public TwoListPanel(List fromEntries, String fromLabel, List toEntries,
                        String toLabel, JComponent extraButtons,
                        boolean showUpDownButtons) {


        this.fromEntries         = new Vector(fromEntries);
        this.toEntries           = new Vector(toEntries);
        this.originalFromEntries = new ArrayList(fromEntries);
        this.originalToEntries   = new ArrayList(toEntries);
        this.showUpDownButtons   = showUpDownButtons;
        init(fromLabel, toLabel, extraButtons);
    }


    /**
     * _more_
     *
     * @param title _more_
     * @param windowLabel _more_
     * @param labels _more_
     * @param ids _more_
     * @param current _more_
     *
     * @return _more_
     */
    public boolean showDialog(String title, String windowLabel, List labels,
                              List ids, Hashtable current) {
        JComponent contents = getContents(windowLabel, labels, ids, current);
        return GuiUtils.showOkCancelDialog(null, title, contents, null);
    }




    /**
     * _more_
     *
     * @param windowLabel _more_
     * @param labels _more_
     * @param ids _more_
     * @param current _more_
     *
     * @return _more_
     */
    public JComponent getContents(String windowLabel, List labels, List ids,
                                  Hashtable current) {
        JPanel contents   = GuiUtils.topCenter(new JLabel(windowLabel), this);
        List   currentTfo = new ArrayList();
        List   allTfo     = new ArrayList();
        for (int i = 0; i < labels.size(); i++) {
            String label = (String) labels.get(i);
            Object id    = ids.get(i);
            allTfo.add(new TwoFacedObject(label, id));
            if (current.get(id) != null) {
                currentTfo.add(new TwoFacedObject(label, id));
            }
        }
        if (currentTfo.size() == 0) {
            currentTfo = new ArrayList(allTfo);
        }
        reinitialize(allTfo, currentTfo);
        return contents;
    }






    /**
     * _more_
     *
     * @param fromEntries _more_
     * @param toEntries _more_
     */
    public void reinitialize(List fromEntries, List toEntries) {
        this.fromEntries         = new Vector(fromEntries);
        this.toEntries           = new Vector(toEntries);
        this.originalFromEntries = new ArrayList(fromEntries);
        this.originalToEntries   = new ArrayList(toEntries);
        reload();

    }






    /**
     * _more_
     *
     * @return _more_
     */
    public List getCurrentEntries() {
        return toEntries;
    }

    /**
     * Set the data in the list. Try to preserve the selected index
     *
     * @param jlist _more_
     * @param entries _more_
     */
    private void setList(JList jlist, Vector entries) {
        int toIdx = jlist.getSelectedIndex();
        jlist.setListData(entries);
        if (toIdx < 0) {
            if (jlist == fromList) {
                jlist.setSelectedIndex(0);
            } else {
                jlist.setSelectedIndex(entries.size() - 1);
            }
        } else if (toIdx < entries.size()) {
            jlist.setSelectedIndex(toIdx);
        } else {
            jlist.setSelectedIndex(entries.size() - 1);
        }
    }


    /**
     * Insert the given entry into the toEntries lsit
     *
     * @param tfo The new entry
     */
    public void insertEntry(Object tfo) {
        changed = true;
        int toIdx = toList.getSelectedIndex();
        if (toIdx < 0) {
            toEntries.add(tfo);
        } else {
            toEntries.add(toIdx + 1, tfo);
        }
        setList(toList, toEntries);
        toList.setSelectedValue(tfo, true);
    }

    /**
     * Take the selected entry in the fromList and add it into the
     * the toolbar list. Remove the entry from the action list.
     */
    private void insertEntry() {
        Object[] objects = fromList.getSelectedValues();
        if ((objects == null) || (objects.length == 0)) {
            return;
        }
        for (int i = objects.length - 1; i >= 0; i--) {
            Object object = objects[i];
            seenEntries.put(object, object);
            insertEntry(object);
        }
        updateFromList();
    }

    /**
     * _more_
     *
     * @param l _more_
     *
     * @return _more_
     */
    private int getLastIndex(JList l) {
        int[] indices = l.getSelectedIndices();
        int   idx     = -1;
        for (int i = 0; i < indices.length; i++) {
            if ((i == 0) || (indices[i] > idx)) {
                idx = indices[i];
            }
        }

        return idx;
    }

    /**
     * Return the from list
     *
     * @return The from list
     */
    public JList getFromList() {
        return fromList;
    }

    /**
     * Return the to list
     *
     * @return The to list
     */
    public JList getToList() {
        return toList;
    }


    /**
     * _more_
     *
     * @param l _more_
     *
     * @return _more_
     */
    private int getFirstIndex(JList l) {
        int[] indices = l.getSelectedIndices();
        int   idx     = -1;
        for (int i = 0; i < indices.length; i++) {
            if ((i == 0) || (indices[i] < idx)) {
                idx = indices[i];
            }
        }

        return idx;
    }

    /**
     * Move the selected item in the icon list up (well, up visually, actually down in the list).
     *
     * @param resetSelected If true then set the selected index in the icon list
     */
    private void moveUp(boolean resetSelected) {
        changed = true;
        int toIdx = getFirstIndex(toList);
        if (toIdx <= 0) {
            return;
        }
        Object tmp = toEntries.remove(toIdx);
        toEntries.add(toIdx - 1, tmp);
        setList(toList, toEntries);
        if (resetSelected) {
            toList.setSelectedValue(toEntries.get(toIdx - 1), true);
        }

    }

    /**
     * Move the selected item in the icon list down (well, up visually, actually up in the list).
     *
     * @param resetSelected If true then set the selected index in the icon list
     */
    private void moveDown(boolean resetSelected) {
        changed = true;
        int toIdx = getLastIndex(toList);
        if (toIdx < 0) {
            return;
        }
        if (toIdx >= toEntries.size() - 1) {
            return;
        }
        Object tmp = toEntries.remove(toIdx);
        toEntries.add(toIdx + 1, tmp);
        setList(toList, toEntries);
        if (resetSelected) {
            toList.setSelectedValue(toEntries.get(toIdx + 1), true);
            //            toList.setSelectedIndex(toIdx + 1);
        }
    }

    /**
     * Remove the selected entry in the icon list
     */
    private void removeEntry() {
        changed = true;
        Object[] objects = toList.getSelectedValues();
        if ((objects == null) || (objects.length == 0)) {
            return;
        }
        for (int i = 0; i < objects.length; i++) {
            seenEntries.remove(objects[i]);
            toEntries.remove(objects[i]);
        }
        setList(toList, toEntries);
        updateFromList();
    }

    /**
     * Reset the list of fromEntries to be shown
     */
    private void updateFromList() {
        fromEntries.clear();
        for (int i = 0; i < originalFromEntries.size(); i++) {
            Object entry = originalFromEntries.get(i);
            if (seenEntries.get(entry) != null) {
                continue;
            }
            fromEntries.add(entry);
        }
        setList(fromList, fromEntries);
    }

    /**
     * Set the initial list of toEntries
     */
    public void reload() {
        changed     = false;
        seenEntries = new Hashtable();
        toEntries   = new Vector();
        for (int i = 0; i < originalToEntries.size(); i++) {
            Object o = originalToEntries.get(i);
            seenEntries.put(o, o);
            toEntries.add(o);
        }
        updateFromList();
        setList(toList, toEntries);
    }






    /**
     * Init me.
     *
     * @param fromLabel _more_
     * @param toLabel _more_
     * @param extra _more_
     */
    private void init(String fromLabel, String toLabel, JComponent extra) {
        changed = false;
        reload();
        toList.setToolTipText(
            "<html>Delete/Left arrow to remove <br>Right arrow to add</html>");
        fromList.setToolTipText("Right arrow to add");
        toList.setSelectionMode(
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fromList.setSelectionMode(
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fromList.setVisibleRowCount(15);
        toList.setVisibleRowCount(15);

        JButton removeButton = new JButton("<html>&lt;&nbsp;&nbsp;"
                                           + Msg.msg("Remove") + "</html>");
        JButton insertButton = new JButton("<html>" + Msg.msg("Add")
                                           + "&nbsp;&nbsp;&gt;</html>");

        insertButton.setActionCommand(CMD_INSERT);
        insertButton.addActionListener(this);
        removeButton.setActionCommand(CMD_REMOVE);
        removeButton.addActionListener(this);
        JButton upButton =
            GuiUtils.getImageButton("/auxdata/ui/icons/Up16.gif", getClass());
        upButton.setActionCommand(CMD_UP);
        upButton.addActionListener(this);
        JButton downButton =
            GuiUtils.getImageButton("/auxdata/ui/icons/Down16.gif",
                                    getClass());
        downButton.setActionCommand(CMD_DOWN);
        downButton.addActionListener(this);

        fromList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_RIGHT) {
                    insertEntry();
                }
            }
        });

        toList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
		if (GuiUtils.isDeleteEvent(event) ||
		    event.getKeyCode() == KeyEvent.VK_LEFT) {
                    removeEntry();
                } else if ((event.getKeyCode() == KeyEvent.VK_UP)
                           && event.isShiftDown()) {
                    //                    moveUp(false);
                } else if ((event.getKeyCode() == KeyEvent.VK_DOWN)
                           && event.isShiftDown()) {
                    //                    moveDown(false);
                }
            }
        });





        JPanel middlePanel = GuiUtils.inset(GuiUtils.vbox(insertButton,
                                 removeButton, ((extra == null)
                ? (JComponent) new JPanel()
                : extra)), 10);
        int         width  = 200;
        int         height = 400;
        JScrollPane fromSp = GuiUtils.makeScrollPane(fromList, width, height);


        fromSp.setPreferredSize(new Dimension(width, height));
        fromSp.setMinimumSize(new Dimension(width, height));

        JScrollPane toSp = GuiUtils.makeScrollPane(toList, width, height);
        toSp.setPreferredSize(new Dimension(width, height));
        toSp.setMinimumSize(new Dimension(width, height));

        JPanel mainPanel = GuiUtils.doLayout(new Component[] {
                               GuiUtils.topCenter(GuiUtils.cLabel(fromLabel),
                                   fromSp),
                               middlePanel,
                               GuiUtils.topCenter(GuiUtils.cLabel(toLabel),
                                   toSp), GuiUtils.top(showUpDownButtons
                ? GuiUtils.vbox(upButton, downButton)
                : new JPanel()) }, 4, GuiUtils.WT_YNYN, GuiUtils.WT_Y);
        mainPanel = GuiUtils.inset(mainPanel, 5);

        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, mainPanel);
    }



    /**
     * Handle the action
     *
     * @param ae The action
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(CMD_INSERT)) {
            insertEntry();
        } else if (cmd.equals(CMD_REMOVE)) {
            removeEntry();
        } else if (cmd.equals(CMD_UP)) {
            moveUp(true);
        } else if (cmd.equals(CMD_DOWN)) {
            moveDown(true);
        }
    }


    /**
     * Keeps track if there were any changes
     *
     * @return Any changes
     */
    public boolean getChanged() {
        return changed;
    }


}

