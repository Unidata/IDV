/*
 * $Id: CommandManager.java,v 1.15 2007/07/06 20:45:29 jeffmc Exp $
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

import java.awt.*;
import java.awt.event.*;


import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


/**
 * Class CommandManager
 *
 *
 * @author Unidata development team
 * @version %I%, %G%
 */
public class CommandManager {

    /** _more_ */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            CommandManager.class.getName());

    private Object MUTEX = new Object();
    /** _more_ */
    ArrayList commands = new ArrayList();

    /** _more_ */
    int currentIdx = -1;

    /** _more_ */
    int historySize = 30;

    /** _more_ */
    JButton bBtn;

    /** _more_ */
    JButton fBtn;

    /** _more_ */
    private boolean applyingCommand = false;

    /**
     * _more_
     *
     */
    public CommandManager() {}

    /**
     * _more_
     *
     *
     * @param historySize _more_
     */
    public CommandManager(int historySize) {
        this.historySize = historySize;
    }

    /**
     * _more_
     * @return _more_
     */
    public JPanel getContents() {
        return getContents(true);
    }

    public JPanel getContents(boolean includeHistory) {
        if(includeHistory)
            return GuiUtils.hbox(new JLabel("History: "), getBackButton(),
                                 getForwardButton());
        else
            return GuiUtils.hbox(getBackButton(),
                                 getForwardButton());
    }

    /**
     * _more_
     * @return _more_
     */
    public JButton getBackButton() {
        if (bBtn == null) {
            bBtn = GuiUtils.getImageButton(
                GuiUtils.getImageIcon("/auxdata/ui/icons/arrow_undo.png", getClass()));
            bBtn.setToolTipText(
                "Click to go back one step. Shift click to go back 10 steps");
            bBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int cnt = (isShift(e)
                               ? 10
                               : 1);
                    while (bBtn.isEnabled() && (--cnt) >= 0) {
                        move(-1);
                    }
                }
            });
            checkGui();
        }
        return bBtn;
    }


    /**
     * _more_
     * @return _more_
     */
    public JButton getForwardButton() {
        if (fBtn == null) {
            fBtn = GuiUtils.getImageButton(
                GuiUtils.getImageIcon("/auxdata/ui/icons/arrow_redo.png", getClass()));
            fBtn.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            fBtn.setToolTipText(
                "Click to go forward one step. Shift click to go forward 10 steps");
            fBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int cnt = (isShift(e)
                               ? 10
                               : 1);
                    while (fBtn.isEnabled() && (--cnt) >= 0) {
                        move(1);
                    }
                }
            });
            checkGui();
        }
        return fBtn;
    }

    /**
     * _more_
     */
    public void undo() {
        move(-1);
    }

    /**
     * _more_
     */
    public void redo() {
        move(1);
    }

    /**
     * _more_
     *
     * @param e
     * @return _more_
     */
    private boolean isShift(ActionEvent e) {
        return ((e.getModifiers() & e.SHIFT_MASK) != 0);
    }

    /**
     * _more_
     *
     * @param delta
     */
    public void move(int delta) {
        int newIdx = currentIdx + delta;
        if (newIdx < 0) {
            newIdx = -1;
        } else if (newIdx >= commands.size()) {
            newIdx = commands.size() - 1;
        }
        //      System.err.println ("Size:" + commands.size () + " Current:" + currentIdx + " newIdx:" + newIdx);

        applyingCommand = true;
        if (newIdx > currentIdx) {
            Command newCommand = (Command) commands.get(newIdx);
            newCommand.redoCommand();
        } else if (newIdx < currentIdx) {
            Command oldCommand = (Command) commands.get(currentIdx);
            oldCommand.undoCommand();
        }
        applyingCommand = false;
        currentIdx      = newIdx;
        checkGui();

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getApplyingCommand() {
        return applyingCommand;
    }

    /**
     * _more_
     */
    public void checkGui() {
        if (bBtn != null) {
            bBtn.setEnabled(currentIdx >= 0);
        }
        if (fBtn != null) {
            fBtn.setEnabled(currentIdx < commands.size() - 1);
        }
    }


    /**
     * _more_
     *
     * @param command
     */
    public void add(Command command) {
        add(command, true);
    }

    /**
     * _more_
     *
     * @param command
     * @param andCallDoCommand _more_
     */
    public void add(Command command, boolean andCallDoCommand) {
        synchronized(MUTEX) {
            int i = currentIdx + 1;
            while (i < commands.size()) {
                commands.remove(i);
            }
            //Purge the list. 
            while (commands.size() > historySize) {
                commands.remove(0);
            }
            commands.add(command);
            currentIdx = commands.size() - 1;
        }
        if (andCallDoCommand) {
            command.doCommand();
        }
        checkGui();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int size() {
        return commands.size();
    }

    /**
     * _more_
     * @return _more_
     */
    public String toString() {
        String s = "Commands:";
        synchronized(MUTEX) {
            for (int i = 0; i < commands.size(); i++) {
                s = s + "\n\t" + commands.get(i);
            }
        }
        return s;

    }

    /**
     * _more_
     * @return _more_
     */
    public boolean canGoForward() {
        return (currentIdx < commands.size() - 1);
    }


    /**
     * _more_
     * @return _more_
     */
    public boolean canGoBack() {
        return (currentIdx > 0);
    }



}

