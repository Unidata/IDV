/*
 * $Id: PersistentDataDialog.java,v 1.20 2007/07/06 20:45:32 jeffmc Exp $
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



import ucar.unidata.ui.IndependentDialog;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;

import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


/**
 *  Provides common L&F for managing persistent data.
 *  Calling routine must implement PersistentDataManager interface that provides
 *  the services required.
 *
 *  @see ucar.unidata.ui.PersistentDataManager
 *  @author John Caron
 *  @version $Id: PersistentDataDialog.java,v 1.20 2007/07/06 20:45:32 jeffmc Exp $
 */
public class PersistentDataDialog implements ActionListener {

    /** dialog */
    private JDialog dialog;

    /** contents */
    private Component contents;

    /** manager */
    private PersistentDataManager manage;

    /** identifier for this */
    private String objectName;

    /** identifier for the help */
    private String helpId;

    // GUI stuff that needs class scope

    /** border */
    private TitledBorder viewBorder;


    // misc

    /** debug flag */
    private boolean debug = false;

    /**
     * Create a new PersistentDataDialog
     *
     * @param name        name of managed components (eg "Projections").
     * @param view        shows a view of the selected object
     * @param list        manages the list of objects
     * @param manager     abstraction of the services required
     */
    public PersistentDataDialog(String name, JComponent view,
                                JComponent list,
                                PersistentDataManager manager) {
        init(name, view, list, manager, false);
    }


    /**
     * Constructor.
     * @param parent      JFrame (application) or JApplet (applet)
     * @param modal       true for a modal dialog
     * @param name        name of managed components (eg "Projections").
     * @param view        shows a view of the selected object
     * @param list        manages the list of objects
     * @param manager     abstraction of the services required
     */
    public PersistentDataDialog(RootPaneContainer parent, boolean modal,
                                String name, JComponent view,
                                JComponent list,
                                PersistentDataManager manager) {
        this(parent, modal, name, view, list, manager, name);
    }

    /**
     * Constructor.
     * @param parent      JFrame (application) or JApplet (applet)
     * @param modal       true for a modal dialog
     * @param name        name of managed components (eg "Projections").
     * @param view        shows a view of the selected object
     * @param list        manages the list of objects
     * @param manager     abstraction of the services required
     * @param helpId      id for help page when help button is clicked
     */
    public PersistentDataDialog(RootPaneContainer parent, boolean modal,
                                String name, JComponent view,
                                JComponent list,
                                PersistentDataManager manager,
                                String helpId) {

        init(name, view, list, manager, true, helpId);
        if ((parent == null) && modal) {
            parent = (RootPaneContainer) LogUtil.getCurrentWindow();
        }
        if (parent != null) {
            if (parent instanceof JFrame) {
                dialog = new IndependentDialog((JFrame) parent, modal,
                        name + " Manager");
            } else if (parent instanceof JDialog) {
                dialog = new IndependentDialog((JDialog) parent, modal,
                        name + " Manager");
            }
        }
        if (dialog == null) {
            dialog = new IndependentDialog(parent, modal, name + " Manager");
        }
        dialog.setLocation(100, 100);
        GuiUtils.packDialog(dialog, contents);
        dialog.pack();
    }

    /**
     * Get the GUI contents
     * @return the GUI contents
     */
    public Component getContents() {
        return contents;
    }

    /**
     * Initialize the class
     *
     * @param name        name of managed components (eg "Projections").
     * @param view        shows a view of the selected object
     * @param list        manages the list of objects
     * @param manager     abstraction of the services required
     * @param doWindow    true to show in a separate window
     */
    protected void init(String name, JComponent view, JComponent list,
                        PersistentDataManager manager, boolean doWindow) {
        init(name, view, list, manager, doWindow, name);
    }

    /**
     * Initialize the class
     *
     * @param name        name of managed components (eg "Projections").
     * @param view        shows a view of the selected object
     * @param list        manages the list of objects
     * @param manager     abstraction of the services required
     * @param doWindow    true to show in a separate window
     * @param helpId      id for help page
     */
    protected void init(String name, JComponent view, JComponent list,
                        PersistentDataManager manager, boolean doWindow,
                        String helpId) {
        this.manage     = manager;
        this.objectName = name;
        this.helpId     = helpId;

        // construct the UI
        JPanel mainPanel = new JPanel(new BorderLayout());
        //      mainPanel.setBorder(new LineBorder(Color.blue));
        Border standardBorder = new EtchedBorder();

        // the view component
        JPanel viewSide = new JPanel();
        viewSide.setLayout(new BorderLayout());
        viewBorder = new TitledBorder(standardBorder, "Current " + name,
                                      TitledBorder.ABOVE_TOP,
                                      TitledBorder.CENTER);
        viewSide.setBorder(viewBorder);
        viewSide.add(view, BorderLayout.CENTER);
        mainPanel.add(viewSide, BorderLayout.WEST);

        // list toolbar
        JToolBar listTools = new JToolBar();
        listTools.setFloatable(false);
        JButton editButton = new JButton("Edit");
        editButton.setToolTipText("edit selected " + name);
        JButton newButton = new JButton("New");
        newButton.setToolTipText("define new " + name);
        JButton deleteButton = new JButton("Delete");
        deleteButton.setToolTipText("delete selected " + name);
        listTools.add(editButton);
        listTools.add(newButton);
        listTools.add(deleteButton);

        // the list component
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(new TitledBorder(standardBorder,
                                             "Saved " + name + "s",
                                             TitledBorder.ABOVE_TOP,
                                             TitledBorder.CENTER));
        listPanel.add(listTools, BorderLayout.NORTH);
        //listPanel.add(new JScrollPane(list), BorderLayout.CENTER);
        listPanel.add(list, BorderLayout.CENTER);
        mainPanel.add(listPanel, BorderLayout.CENTER);


        // the bottom button panel
        if (doWindow) {
            Container buttPanel = GuiUtils.makeApplyOkHelpCancelButtons(this);
            contents = GuiUtils.centerBottom(mainPanel, buttPanel);
        } else {
            contents = mainPanel;
        }


        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                manage.edit(false);
            }
        });
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                manage.edit(true);
            }
        });
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                manage.deleteSelected();
            }
        });
    }

    /**
     * Called to cleanup.
     */
    public void destroy() {
        if (dialog != null) {
            dialog.dispose();
        }
        dialog = null;
    }


    /**
     * Show the dialog
     */
    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }

    /**
     * Get the JDialog for this
     * @return the dialog
     */
    public JDialog getDialog() {
        return dialog;
    }

    /**
     * Close this widget
     */
    public void close() {
        if (dialog != null) {
            dialog.setVisible(false);
        }
    }

    /**
     * Handle action events
     *
     * @param event  event to handle
     */
    public void actionPerformed(ActionEvent event) {
        String cmd = event.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_APPLY)) {
            manage.accept();
        }
        if (cmd.equals(GuiUtils.CMD_OK) || cmd.equals(GuiUtils.CMD_CANCEL)) {
            close();
        }
        if (cmd.equals(GuiUtils.CMD_HELP)) {
            ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(helpId);
        }
    }

    /**
     * Set the help id for this window.  If not called, the name
     * supplied at construction is used.
     * @param newId  new help ID
     */
    public void setHelpId(String newId) {
        helpId = newId;
    }

    /**
     * Set this widget enabled
     *
     * @param enabled  true to enable
     */
    public void setEnabled(boolean enabled) {
        //TODO: 
        //    acceptButton.setEnabled( enabled);
        //    cancelButton.setEnabled( enabled);
        //    helpButton.setEnabled( enabled);
    }

    /**
     * Set the name
     *
     * @param name  new name
     */
    public void setCurrent(String name) {
        viewBorder.setTitle(name);
        contents.invalidate();
        contents.repaint();
    }

    /**
     * Utility to provide validation of objects the user wants to save
     *   @param startingId         the starting name of the edited object
     *   @param id                 the current name  of the edited object
     * @return true if okay to save
     */
    public boolean checkSaveOK(String startingId, String id) {

        // cant be blank
        id = id.trim();
        if (id.length() == 0) {
            JOptionPane.showMessageDialog(null,
                                          "You must enter a name for this "
                                          + objectName);
            return false;
        }

        // see if already exists
        if ( !manage.contains(id)) {
            return true;  // new is ok
        }

        if (startingId.equals(id)) {
            return true;  // name was not changed - ok
        }

        // otherwise get confirmation
        return (JOptionPane.YES_OPTION
                == JOptionPane.showConfirmDialog(
                    null, "You are about to replace an existing "
                    + objectName + " named '" + id + "'", "Confirm "
                        + objectName
                        + " Replacement", JOptionPane.OK_CANCEL_OPTION));
    }


}  // end PersistentDataDialog

/*
 *  Change History:
 *  $Log: PersistentDataDialog.java,v $
 *  Revision 1.20  2007/07/06 20:45:32  jeffmc
 *  A big J&J
 *
 *  Revision 1.19  2005/11/17 20:59:03  jeffmc
 *  Better handling of parent dialogs/windows
 *
 *  Revision 1.18  2005/05/13 18:31:50  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.17  2004/10/22 14:58:35  dmurray
 *  fix the projection manager help.  Really this whole ProjectionManger
 *  needs a rework due to the issues brought up in the IDV workshop
 *
 *  Revision 1.16  2004/09/07 18:36:25  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.15  2004/02/27 21:19:19  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.14  2004/01/29 17:37:12  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.13  2003/06/19 22:21:38  jeffmc
 *  A bevy of changes - most to do with adding in support
 *  for creating and running test archives of images
 *
 *  Revision 1.12  2002/08/16 22:17:37  jeffmc
 *  A variety of memory leak cleanup
 *
 *  Revision 1.11  2002/07/01 21:45:38  jeffmc
 *  Use the GuiUtils.CMD_OK, etc., instead of hard coded strings
 *
 *  Revision 1.10  2002/03/09 17:17:06  jeffmc
 *  Lots of changes to support preferences
 *
 *  Revision 1.9  2002/02/21 22:31:28  jeffmc
 *  some changes...
 *
 *  Revision 1.8  2001/11/21 01:07:42  jeffmc
 *  Changed the rather divergent use if Apply/Ok/Cancel/etc. buttons
 *  to use a new facility in GuiUtils that provides a constistent set of named
 *  buttons.
 *
 *  Revision 1.7  2000/08/18 04:15:56  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.6  2000/05/26 21:19:33  caron
 *  new GDV release
 *
 *  Revision 1.5  1999/12/22 21:18:06  caron
 *  javahelp support
 *
 *  Revision 1.4  1999/12/16 22:58:13  caron
 *  gridded data viewer checkin
 *
 *  Revision 1.3  1999/06/03 01:44:12  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:27:07  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:47  caron
 *  startAgain
 *
 *  # Revision 1.2  1999/03/26  19:58:32  caron
 *  # add SpatialSet; update javadocs
 *  #
 *  # Revision 1.1  1999/03/16  17:00:48  caron
 *  # fix StationModeol editing; add TopLevel
 *  #
 */






