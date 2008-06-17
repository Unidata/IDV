/*
 * $Id: PreferenceList.java,v 1.15 2006/05/05 19:19:36 jeffmc Exp $
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



package ucar.unidata.util;


import java.awt.event.*;



import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;


/**
 * @author Metapps development team
 * @version $Revision: 1.15 $ $Date: 2006/05/05 19:19:36 $
 */



public class PreferenceList {

    /** _more_ */
    List list;

    /** _more_ */
    Object chosen;

    /** _more_ */
    String chosenId;

    /** _more_ */
    String listId;

    /** _more_ */
    PersistentStore store;

    /** _more_ */
    List boxes = new ArrayList();

    /** _more_ */
    boolean fireEventOnBoxAction = false;

    /** _more_ */
    private boolean ignoreBoxAction = false;

    /**
     * _more_
     *
     */
    public PreferenceList() {
        this(new ArrayList());
    }

    /**
     * _more_
     *
     * @param list
     *
     */
    public PreferenceList(List list) {
        this(list, null, "", "", null);
    }

    /**
     * _more_
     *
     * @param list
     * @param chosen
     * @param listId
     * @param chosenId
     * @param store
     *
     */
    public PreferenceList(List list, Object chosen, String listId,
                          String chosenId, PersistentStore store) {
        this.list     = list;
        this.chosen   = chosen;
        this.chosenId = chosenId;
        this.listId   = listId;
        this.store    = store;
        if (list == null) {
            list = new ArrayList();
        }
    }

    /**
     * _more_
     *
     * @param actionName
     * @param listener
     * @return _more_
     */
    public JComboBox createComboBox(final String actionName,
                                    final ActionListener listener) {
        return createComboBox(actionName, listener, false);
    }


    /**
     * _more_
     *
     * @param actionName
     * @param listener
     * @param fireEventOnBoxAction
     * @return _more_
     */
    public JComboBox createComboBox(final String actionName,
                                    final ActionListener listener,
                                    final boolean fireEventOnBoxAction) {
        final JComboBox box =
            new JComboBox(StringUtil.listToStringArray(list));
        boxes.add(box);
        if ((chosen != null) && list.contains(chosen)) {
            box.setSelectedItem(chosen);
        } else if (list.size() > 0) {
            box.setSelectedIndex(0);
        }
        box.setEditable(true);
        if (fireEventOnBoxAction) {
            box.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (ignoreBoxAction) {
                        return;
                    }
                    handleBoxAction(e, box, actionName, listener);
                }
            });
        }
        box.getEditor().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleBoxEdit(e, box, actionName, listener,
                              !fireEventOnBoxAction);
            }
        });
        return box;
    }

    /**
     * _more_
     *
     * @param e
     * @param box
     * @param actionName
     * @param listener
     * @param fireEvent
     */
    private synchronized void handleBoxEdit(ActionEvent e, JComboBox box,
                                            String actionName,
                                            ActionListener listener,
                                            boolean fireEvent) {
        String selection = e.getActionCommand().trim();
        if (selection.length() == 0) {
            int index = box.getSelectedIndex();
            if (index >= 0) {
                list.remove(index);
                stateChanged();
                if (list.size() > 0) {
                    ignoreBoxAction = true;
                    box.setSelectedItem(list.get(0));
                    ignoreBoxAction = false;
                }
                writeState();
            }
            return;
        } else {
            ignoreBoxAction = true;
            box.setSelectedItem(selection);
            ignoreBoxAction = false;
        }
        //      if (checkIfInList (box, selection)) {
        if (fireEvent) {
            listener.actionPerformed(new ActionEvent(box, 1, actionName));
        }
        //  }
    }

    /**
     * _more_
     *
     * @param box
     * @param selection
     * @return _more_
     */
    private boolean checkIfInList(JComboBox box, String selection) {
        List    boxList = GuiUtils.getItems(box);
        boolean inList  = boxList.contains(selection);
        if ( !inList) {
            //Add the new item  to the list
            list.add(selection);
            //Tell other boxes the list has changed
            stateChanged();
            //Now set this value as the boxes selected item.
            ignoreBoxAction = true;
            box.setSelectedItem(selection);
            ignoreBoxAction = false;
        }
        return inList;
    }


    /**
     * _more_
     *
     * @param e
     * @param box
     * @param actionName
     * @param listener
     */
    private synchronized void handleBoxAction(ActionEvent e, JComboBox box,
            String actionName, ActionListener listener) {
        chosen = box.getSelectedItem();
        listener.actionPerformed(new ActionEvent(box, 1, actionName));
    }


    /**
     *  This sets the currently chosen selection to the selected item of
     *  the given box. It then checks to see if the selected item is in the
     *  list held by the box. If not it adds it to the list. It then writes
     *  out the chosen and the list to the object store if changed.
     *
     * @param box
     */
    public void saveState(JComboBox box) {
        chosen = box.getSelectedItem();
        if(chosen != null){
            checkIfInList(box, chosen.toString());
            writeState();
        }
    }


    /**
     *  This checks if the current chosen value and/or the list of values is different
     *  than the one held by the object store. If different then it sets the store
     *  value and writes the store.
     */
    private void writeState() {
        if (store == null) {
            return;
        }
        boolean doWrite = false;
        if ( !Misc.equals(chosen, store.get(chosenId))) {
            doWrite = true;
            store.put(chosenId, chosen);
        }
        if ( !Misc.equals(list, store.get(listId))) {
            doWrite = true;
            store.put(listId, list);
        }
        if (doWrite) {
            store.save();
        }
    }


    /**
     * _more_
     */
    private void stateChanged() {
        ignoreBoxAction = true;
        for (int i = 0; i < boxes.size(); i++) {
            JComboBox box      = (JComboBox) boxes.get(i);
            Object    selected = box.getSelectedItem();
            GuiUtils.setListData(box, list);
            if (selected != null) {
                if (list.contains(selected)) {
                    box.setSelectedItem(selected);
                } else if (list.size() > 0) {
                    box.setSelectedItem(list.get(0));
                } else {
                    box.setSelectedItem("");
                }
            }
        }
        ignoreBoxAction = false;
    }


}

