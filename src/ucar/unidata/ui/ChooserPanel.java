/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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


import java.awt.Color;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * Common class for the chooser panels.
 *
 * @author Unidata Metapps development team
 */
public class ChooserPanel extends JPanel implements ActionListener {

    /** The spacing used in the grid layout. */
    protected static final int GRID_SPACING = 5;

    /** The Constant GRID_SPACING_V. */
    protected static final int GRID_SPACING_V = 3;

    /** The Constant GRID_SPACING_H. */
    protected static final int GRID_SPACING_H = 5;

    /** Used by derived classes when they do a GuiUtils.doLayout */
    protected static final Insets GRID_INSETS = new Insets(GRID_SPACING_V,
                                                    GRID_SPACING_H,
                                                    GRID_SPACING_V,
                                                    GRID_SPACING_H);




    /** The color for station maps. */
    public static final Color MAP_COLOR = Color.lightGray;


    /** Text for the load button. */
    public static final String CMD_LOAD = "Add Source";

    /**
     *  Keep track of how many cursor wait calls we have outstanding.
     */
    private int cursorCnt = 0;


    /** Where can we find help for this panel. */
    protected String helpPath = "";

    /** Panel holding the contents. */
    protected JComponent contents;

    /** Load button. */
    protected JButton loadButton;

    /** Cancel button. */
    protected JButton cancelButton;

    /** Flag for whether data has been chosen or not. */
    protected boolean haveData = false;

    /**
     * The PropertyChangeListener-s.
     */
    private PropertyChangeSupport changeListeners;

    /** Shows the status. */
    protected JLabel statusLabel;

    /** _more_. */
    private JComponent statusComp;

    /** _more_. */
    private Hashtable statusComps = new Hashtable();

    /** _more_. */
    protected boolean simpleMode = false;

    /** The message template. */
    private String messageTemplate;

    /**
     * Construct an object for selecting a data source from
     * the current directory and from a default ADDE server.
     */
    public ChooserPanel() {
        changeListeners = new PropertyChangeSupport(this);
    }



    /**
     * Sets the message template.
     *
     * @param template the new message template
     */
    public void setMessageTemplate(String template) {
        this.messageTemplate = template;
    }

    /**
     * Gets the message template.
     *
     * @return the message template
     */
    protected String getMessageTemplate() {
        return messageTemplate;
    }


    /**
     * Adds a PropertyChangeListener.
     *
     * @param listener          The PropertyChangeListener to add.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeListeners != null) {
            changeListeners.addPropertyChangeListener(listener);
        }
    }

    /**
     * Fires a PropertyChangeEvent.
     *
     * @param propertyName              The name of the property.
     * @param oldValue                  The old value of the property.
     * @param newValue                  The new Value of the property.
     */
    protected void firePropertyChange(String propertyName, Object oldValue,
                                      Object newValue) {

        if (changeListeners != null) {
            changeListeners.firePropertyChange(propertyName, oldValue,
                    newValue);
        }
    }



    /**
     * {@inheritDoc}
     */
    public void revalidate() {
        //        getContents().revalidate();
    }

    /**
     * Register status comp.
     *
     * @param name the name
     * @param comp the comp
     * @return the JComponent
     */
    protected JComponent registerStatusComp(String name, JComponent comp) {
        comp = GuiUtils.inset(comp, 1);
        comp = GuiUtils.inset(comp, 2);
        statusComps.put(name, comp);
        return comp;
    }


    /**
     * Set the status message.
     *
     * @param msg The status message
     */
    public void setStatus(String msg) {
        setStatus(msg, "nocomp");
    }

    /**
     * Gets the simple mode.
     *
     * @return the simple mode
     */
    public boolean getSimpleMode() {
        return simpleMode;
    }

    /**
     * Sets the status.
     *
     * @param msg the msg
     * @param compId the comp id
     */
    public void setStatus(String msg, String compId) {
        String template = getMessageTemplate();
        if (template != null) {
            msg = template.replace("${message}", msg);
        }
        getStatusLabel().setText(msg);
        if ( !getSimpleMode()) {
            return;
        }
        for (Enumeration keys =
                statusComps.keys(); keys.hasMoreElements(); ) {
            String     key  = (String) keys.nextElement();
            JComponent comp = (JComponent) statusComps.get(key);
            if (compId.equals(key)) {
                comp.setBackground(Color.cyan);
                //                comp.setBackground(Color.black);
                //                comp.setBackground(new Color(255,255, 204));
                //                comp.setBackground(Color.BLUE.brighter());
            } else {
                comp.setBackground(null);
            }

        }

    }

    /**
     * Create (if needed) and return the JLabel that shows the status messages.
     *
     * @return The status label
     */
    protected JLabel getStatusLabel() {
        if (statusLabel == null) {
            statusLabel = new JLabel();
            statusLabel.setOpaque(true);
            statusLabel.setForeground(getStatusLabelForeground());
            statusLabel.setBackground(getStatusLabelBackground());
        }
        return statusLabel;
    }

    /**
     * Gets the status label background.
     *
     * @return the status label background
     */
    public Color getStatusLabelBackground() {
        return new Color(255, 255, 204);
    }


    /**
     * Gets the status label foreground.
     *
     * @return the status label foreground
     */
    public Color getStatusLabelForeground() {
        return Color.BLACK;
    }

    /**
     * Gets the status component.
     *
     * @return the status component
     */
    protected JComponent getStatusComponent() {
        if (statusComp == null) {
            JLabel statusLabel = getStatusLabel();
            statusComp = GuiUtils.inset(statusLabel, new Insets(3, 2, 1, 0));
            statusComp.setBackground(getStatusLabelBackground());
            statusComp = GuiUtils.inset(statusComp, new Insets(2, 2, 2, 2));
        }
        return statusComp;
    }

    /**
     * Receive the update,cancel, load commands and call:
     * doUpdate, doCancel or doLoad.
     *
     * @param ae    ActionEvent to process
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_UPDATE)) {
            doUpdate();
        } else if (cmd.equals(GuiUtils.CMD_CANCEL)) {
            doCancel();
        } else if (cmd.equals(getLoadCommandName())) {
            doLoad();
        } else if (cmd.equals(GuiUtils.CMD_HELP)) {
            doHelp();
        }

    }

    /**
     * This allows for derived classes to define their own name for the
     * "Add source" button.
     *
     * @return custom name
     */
    protected String getLoadCommandName() {
        return CMD_LOAD;
    }

    /**
     * Get the default buttons for this chooser panel.
     *
     * @return panel of buttons
     */
    public JComponent getDefaultButtons() {
        return getDefaultButtons(this);
    }


    /**
     * Gets the default buttons.
     *
     * @param listener the listener
     * @return the default buttons
     */
    public JComponent getDefaultButtons(ActionListener listener) {
        Hashtable buttonMap   = new Hashtable();
        String[]  commands    = getButtonLabels();
        String[]  labels      = new String[commands.length];
        String    loadCommand = getLoadCommandName();
        String[]  tooltips    = new String[commands.length];
        for (int i = 0; i < commands.length; i++) {
            labels[i] = commands[i];
            if (commands[i].equals(loadCommand)) {
                tooltips[i] = getLoadToolTip();
            } else if (commands[i].equals(GuiUtils.CMD_UPDATE)) {
                tooltips[i] = getUpdateToolTip();
                labels[i]   = "icon:/auxdata/ui/icons/view-refresh22.png";
            } else if (commands[i].equals(GuiUtils.CMD_HELP)) {
                tooltips[i] = "Show help for this chooser";
                labels[i]   = "icon:/auxdata/ui/icons/show-help22.png";
            } else if (commands[i].equals(GuiUtils.CMD_CANCEL)) {
                tooltips[i] = "Cancel choosing and close the window";
            }
        }
        JPanel comp = GuiUtils.makeButtons(listener, labels, commands,
                                           tooltips, buttonMap);
        loadButton = (JButton) buttonMap.get(getLoadCommandName());
        JButton tmpButton = (JButton) buttonMap.get(GuiUtils.CMD_CANCEL);
        if (tmpButton != null) {
            cancelButton = tmpButton;
            cancelButton.setEnabled(false);
        }
        setHaveData(haveData);
        return registerStatusComp("buttons", comp);
        //        return comp;
    }

    /**
     * Can do update.
     *
     * @return true, if successful
     */
    public boolean canDoUpdate() {
        return true;
    }

    /**
     * Get the names for the buttons.
     *
     * @return array of button names
     */
    protected String[] getButtonLabels() {
        if (canDoUpdate()) {
            return new String[] { getLoadCommandName(), GuiUtils.CMD_UPDATE,
                                  GuiUtils.CMD_HELP };
        } else {
            return new String[] { getLoadCommandName(), GuiUtils.CMD_HELP };
        }
    }

    /**
     * Get the tooltip for the load button.
     *
     * @return The tooltip for the load button
     */
    protected String getLoadToolTip() {
        return "Load the selected data";
    }

    /**
     * Get the tooltip for the update button.
     *
     * @return The tooltip for the update button
     */
    protected String getUpdateToolTip() {
        return "Refresh content shown in this Chooser";
    }

    /**
     * Set whether the user has made a selection that contains data.
     *
     * @param have   true to set the haveData property.  Enables the
     *               loading button
     */
    public void setHaveData(boolean have) {
        if (loadButton != null) {
            loadButton.setEnabled(have);
        }
        haveData = have;
        //        updateStatus();
    }

    /**
     * Gets the have data.
     *
     * @return the have data
     */
    public boolean getHaveData() {
        return haveData;
    }

    /**
     * Clear any outstanding cursor waits.
     */
    public void clearWaitCursor() {
        cursorCnt = 0;
        GuiUtils.setCursor(getContents(),
                           Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }


    /**
     * Set the wait cursor over this panel.
     */
    public void showWaitCursor() {
        showWaitCursor(getContents());
    }



    /**
     * Set the normal cursor over this panel.
     */
    public void showNormalCursor() {
        showNormalCursor(getContents());
    }


    /**
     * Set the wait cursor over this panel.
     *
     * @param comp the comp
     */
    public void showWaitCursor(JComponent comp) {
        cursorCnt++;
        GuiUtils.setCursor(comp,
                           Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }



    /**
     * Set the normal cursor over this panel.
     *
     * @param comp the comp
     */
    public void showNormalCursor(JComponent comp) {
        cursorCnt--;
        if (cursorCnt < 0) {
            cursorCnt = 0;
        }
        if (cursorCnt == 0) {
            GuiUtils.setCursor(
                comp, Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }


    /**
     * Pad label.
     *
     * @param s the string
     * @return the JComponent
     */
    public JComponent padLabel(String s) {
        return GuiUtils.inset(new JLabel(s), new Insets(0, 5, 0, 5));
    }

    /**
     * Hides the fact that this is really a JPanel.
     *
     * @return the contents
     */
    public JComponent getContents() {
        if (contents == null) {
            contents = doMakeContents();
            updateStatus();
        }
        return contents;
    }



    /**
     * Do make contents.
     *
     * @return the JComponent
     */
    protected JComponent doMakeContents() {
        return new JPanel();
    }

    /**
     * Update status.
     */
    protected void updateStatus() {}



    /**
     *  Gets called when the user presses {@link #CMD_LOAD} button.
     *  This can get overwritten by a derived class to do something.
     * By default this calls doLoadDataInThread in a separate thread
     */
    public void doLoad() {
        Misc.run(this, "doLoadInThread");
    }


    /**
     * Gets called by doLoad in a thread when the user presses the
     * load button. Should be overwritten by a derived class.
     */
    public void doLoadInThread() {}


    /**
     *  Gets called when the user presses the Update button.
     *  This can get overwritten by a derived class to do something.
     */
    public void doUpdate() {}


    /**
     * Gets called when the user presses the Help button.
     */
    public void doHelp() {
        Help.getDefaultHelp().gotoTarget(helpPath);
    }

    /**
     *  Gets called when the user presses Cancel in multipleSelect mode
     *  This can get overwritten by a derived class to do something.
     */
    public void doCancel() {
        //For now don't do anything
        //        doClose();
    }


    /**
     * Gets called to close the panel.
     */
    protected void doClose() {}


    /**
     * Convenience method to {@link LogUtil#logException(String, Throwable)}.
     *
     * @param msg   message to log
     * @param exc   Exception to log
     */
    public void logException(String msg, Exception exc) {
        LogUtil.logException(msg, exc);
    }

    /**
     * Set the help path used for this chooser.
     *
     * @param path the new help path
     */
    public void setHelpPath(String path) {
        this.helpPath = path;
    }
}
