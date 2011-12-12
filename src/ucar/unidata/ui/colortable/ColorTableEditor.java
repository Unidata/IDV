/*
 * $Id: ColorTableEditor.java,v 1.20 2007/08/10 14:27:30 jeffmc Exp $
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

package ucar.unidata.ui.colortable;




import ucar.unidata.ui.WindowHolder;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Range;




import java.awt.*;

import java.awt.event.*;

import java.beans.*;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;

import javax.swing.event.*;



/**
 * Class ColorTableEditor handles some of the editing of colortables.
 * It wraps a {@link ColorTableCanvas} that does most of  the actual work
 * but this class  serves as the intermediary between the canvas and calling
 * code
 *
 *
 * @author IDV development team
 */
public class ColorTableEditor extends WindowHolder {

    /** For logging errors */
    public static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            ColorTableEditor.class.getName());


    /** The name of the color table */
    private String originalName;

    /** The canvas */
    private ColorTableCanvas canvas;


    /** Was OK pressed */
    private boolean ok = false;

    /** The current color table */
    private ColorTable theColorTable;

    /**
     * Is this editor meant to edit all of the colortables or was it brought up
     * to edit a specific table.
     */
    private boolean standalone = false;

    /** Menu item to delete the color table */
    private JMenuItem deleteMenuItem;

    /** The view menu */
    private JMenu viewMenu;

    private int colorTableListVersion=0;

    /** The ColorTableManager. Does the work of saving, importing, etc. */
    private ColorTableManager ctm;

    /** Has init been called */
    private boolean haveInited = false;

    /** Listener to route events to (may be null) */
    private PropertyChangeListener listener;

    /**
     * Create a stand alone color table editor
     *
     * @param colorTableManager The manager
     */

    public ColorTableEditor(ColorTableManager colorTableManager) {
        this(colorTableManager, null);
        standalone = true;
    }

    /**
     * Create an editor for the given color table
     *
     * @param colorTableManager The manager
     * @param table The color table (may be null)
     *
     */
    public ColorTableEditor(ColorTableManager colorTableManager,
                            ColorTable table) {
        this(colorTableManager, table, null);
    }



    /**
     * Create an editor for the given color table
     *
     * @param colorTableManager The manager
     * @param table The color table (may be null)
     * @param listener The property change listener to route events to. May be null
     */
    public ColorTableEditor(ColorTableManager colorTableManager,
                            ColorTable table,
                            PropertyChangeListener listener) {
        this.ctm           = colorTableManager;
        this.theColorTable = table;
        this.listener      = listener;
    }


    /**
     * Show the window
     */
    public void show() {
        if ( !haveInited) {
            init(theColorTable, listener);
        }
        super.show();
    }


    /**
     * Get the manager
     *
     * @return The manager
     */
    public ColorTableManager getColorTableManager() {
        return ctm;
    }


    /**
     * Get the name of the currently edited color table
     *
     * @return  The color table name
     */
    public String getColorTableName() {
        if (canvas != null) {
            ColorTable current = canvas.getCurrentColorTable();
            if (current != null) {
                return current.getName().trim();
            }
        }
        return ((theColorTable != null)
                ? theColorTable.getName().trim()
                : "");
    }

    /**
     * Initialize the editor
     *
     * @param table The current color table
     * @param listener The listener to route  events to
     */
    private void init(ColorTable table, PropertyChangeListener listener) {
        haveInited         = true;
        this.theColorTable = table;
        if (theColorTable == null) {
            theColorTable = ctm.getDefaultColorTable();
        }
        if (theColorTable == null) {
            theColorTable =
                ColorTableDefaults.createColorTable(new ArrayList(),
                    "Default", ColorTable.CATEGORY_BASIC,
                    ColorTableDefaults.grayTable(256, true));
        }
        canvas       = new ColorTableCanvas(this, theColorTable);
        originalName = getColorTableName();
        if (listener != null) {
            canvas.addPropertyChangeListener(listener);
        }

        JPanel buttons;
        if (standalone) {
            buttons = GuiUtils.makeButtons(this,
                                           new String[] {
                                               GuiUtils.CMD_CLOSE });

        } else {
            buttons = GuiUtils.makeButtons(this,
                                           new String[] { GuiUtils.CMD_APPLY,
                    GuiUtils.CMD_OK, GuiUtils.CMD_CANCEL });
        }

        final JCheckBox propagateChanges = new JCheckBox("Auto update", true);
        propagateChanges.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                canvas.setPropagateChanges(propagateChanges.isSelected());
            }
        });
        JPanel bottom = GuiUtils.hbox(buttons, ((listener != null)
                ? (Component) propagateChanges
                : (Component) new JLabel("")));
        JMenuBar menuBar = doMakeMenuBar();
        contents = GuiUtils.topCenterBottom(menuBar, canvas.getContents(),
                                            bottom);


	setMenuBar(menuBar);
        setDialogTitle();
        show();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getWindowTitle() {
        return GuiUtils.getApplicationTitle() +"Color Table Editor";
    }




    public void updateViewMenu(JMenu m) {
        if(colorTableListVersion != ctm.getResourceTimestamp()) {
            doMakeViewMenu();
        }

    }


    /**
     * Make the menu bar
     * @return The menu bar
     */
    protected JMenuBar doMakeMenuBar() {
        JMenuBar menuBar  = new JMenuBar();

        JMenu    fileMenu = new JMenu("File");
        doMakeFileMenu(fileMenu);
        viewMenu = GuiUtils.makeDynamicMenu("Color Tables", this, "updateViewMenu",false);
        doMakeViewMenu();
        Msg.addDontRecurseComponent(viewMenu);
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);


        JMenu     helpMenu     = new JMenu("Help");
        JMenuItem helpMenuItem = new JMenuItem("User's Guide");
        helpMenu.add(helpMenuItem);
        helpMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showHelp();
            }
        });

        menuBar.add(helpMenu);

        return menuBar;
    }

    /**
     * Get the canvas
     *
     * @return The canvas
     */
    public ColorTableCanvas getCanvas() {
        return canvas;
    }

    /**
     * Add the file menu items
     *
     * @param fileMenu The  file menu
     */
    protected void doMakeFileMenu(JMenu fileMenu) {
        JMenuItem mi;

        fileMenu.add(mi = new JMenuItem("New"));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doNew();
            }
        });

        fileMenu.addSeparator();


        fileMenu.add(mi = new JMenuItem("Save"));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSave();
            }
        });


        fileMenu.add(mi = new JMenuItem("Save As..."));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSaveAs();
            }
        });



        deleteMenuItem = new JMenuItem("Remove");
        deleteMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String originalName = getColorTableName();
                if ( !GuiUtils.showYesNoDialog(dialog,
                        "Are you sure you want to remove the " + originalName
                        + " color table?", "Confirm")) {
                    return;
                }
                ctm.removeUsers(theColorTable);
                ColorTable newTable = ctm.getColorTable(originalName);
                if (newTable == null) {
                    newTable = getColorTableManager().getDefaultColorTable();
                }
                setColorTable(newTable);
                doMakeViewMenu();
            }
        });


        fileMenu.addSeparator();
        fileMenu.add(deleteMenuItem);
        fileMenu.addSeparator();

        fileMenu.add(mi = new JMenuItem("Import..."));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doImport();
            }
        });


        fileMenu.add(mi = new JMenuItem("Export..."));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doExport();
            }
        });

        fileMenu.addSeparator();
        fileMenu.add(mi = new JMenuItem("Close"));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doClose();
            }
        });
    }



    /**
     * Make the View menu.
     */
    public void doMakeViewMenu() {
        colorTableListVersion = ctm.getResourceTimestamp();
        //See if we need to dis/enable the "Remove color table"
        checkDeleteMenu();
        viewMenu.removeAll();
        ArrayList menus = new ArrayList();
        ctm.makeColorTableMenu(new ObjectListener(null) {
            public void actionPerformed(ActionEvent ae, Object data) {
                setColorTable((ColorTable) data);
            }
        }, menus, true);

        for (int i = 0; i < menus.size(); i++) {
            viewMenu.add((JMenuItem) menus.get(i));
        }

    }




    /**
     * Import a color table
     */
    private void doImport() {
        ColorTable ct = (ColorTable) ctm.doImport();
        if (ct == null) {
            return;
        }
        doMakeViewMenu();
        setColorTable(ct);
    }

    /**
     * Export the current color table
     */
    private void doExport() {
        ctm.doExport(canvas.getCurrentColorTable());
    }




    /**
     * Find the color table with the given name and use it
     *
     * @param ctName The color table name to lookup
     */
    public void setColorTable(String ctName) {
        try {
            if ( !haveInited) {
                init(theColorTable, listener);
            }
            ColorTable ct = ctm.getColorTable(ctName);

            if (ct != null) {
                setColorTable(ct);
            }
        } catch (Exception exc) {
            LogUtil.printException(log_, "Setting color table", exc);
        }
    }


    /**
     * Set the current color table
     *
     * @param ct The new color table to use
     */
    public void setColorTable(ColorTable ct) {
        try {
            if ( !haveInited) {
                init(theColorTable, listener);
            }
            theColorTable = ct;
            originalName  = getColorTableName();
            canvas.setColorTable(ct);
            checkDeleteMenu();
        } catch (Exception exc) {
            LogUtil.printException(log_, "Setting color table", exc);
        }
    }

    /**
     * Set the range used by the color table canvas
     *
     * @param range The new range
     */
    public void setRange(Range range) {
        canvas.setRange(range.getMin(), range.getMax());
    }




    /**
     * Enable or disable the delete color table menu item
     */
    private void checkDeleteMenu() {
        if (deleteMenuItem != null) {
            if (theColorTable != null) {
                deleteMenuItem.setEnabled(ctm.isUsers(theColorTable));
            } else {
                deleteMenuItem.setEnabled(false);
            }
        }
    }

    /**
     * Apply the colortable and the range. This triggers an event being sent to
     * the property change listener (if there is one).
     */
    private void doApply() {
        canvas.doApply();
        //Make sure the range gets propagated
        canvas.setMinMax();
    }


    /**
     * Save the current color table, asking for a new name.
     */
    private void doSaveAs() {
        theColorTable = canvas.getCurrentColorTable();
        String newName = ctm.doSaveAs(theColorTable, canvas);
        if (newName == null) {
            return;
        }
        originalName = newName;
        canvas.setName(newName);
        doSave();
    }

    /**
     * Save the current color table with the current name
     */
    private void doSave() {
        ctm.addUsers(theColorTable = canvas.getCurrentColorTable());
        doMakeViewMenu();
    }




    /**
     * Create a new color table. By default use a size 32 gray scale.
     */
    private void doNew() {
        String newName = ctm.doNew(canvas);
        if (newName == null) {
            return;
        }
        float[][] table = ColorTableDefaults.grayTable1(32);
        ColorTable newCt = new ColorTable(newName, ColorTable.CATEGORY_BASIC,
                                          table);
        ctm.addUsers(newCt);
        setColorTable(newCt);
        doMakeViewMenu();
    }



    /**
     * Add the given property change listener
     *
     * @param listener The listener to route events to.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        canvas.addPropertyChangeListener(listener);
    }


    /**
     * Set the visiblity of the window
     *
     * @param v On or off.
     */
    public void setVisible(boolean v) {
        if (dialog != null) {
            dialog.setVisible(false);
        }
    }

    /**
     * Set the title of the window to the default
     */
    protected void setDialogTitle() {
        setWindowTitle(GuiUtils.getApplicationTitle() +"Color Table Editor -- " + getColorTableName());
    }



    /**
     * Close the window
     */
    public void doClose() {
        ok = true;
        super.close();
        canvas.doClose();
    }

    /**
     * Handle the action (eg: CLOSE, APPLY, CANCEL, etc).
     *
     * @param e The action
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_CLOSE)) {
            doClose();
        } else if (cmd.equals(GuiUtils.CMD_APPLY)) {
            doApply();
        } else if (cmd.equals(GuiUtils.CMD_CANCEL)) {
            ok = false;
            super.close();
            canvas.doCancel();
        } else if (cmd.equals(GuiUtils.CMD_OK)) {
            ok = true;
            doApply();
            canvas.doClose();
            super.close();
        } else if (cmd.equals(GuiUtils.CMD_SAVEAS)) {
            doSaveAs();
        } else if (cmd.equals(GuiUtils.CMD_SAVE)) {
            doSave();
        } else if (cmd.equals(GuiUtils.CMD_HELP)) {
            showHelp();
        }
    }

    private static String HELP_TOP_DIR = "/auxdata/docs/userguide";
    public static void setHelpTopDir(String topDir) {
        HELP_TOP_DIR = topDir;
    }

    /**
     * Popup the help window
     */
    private void showHelp() {
        ucar.unidata.ui.Help.setTopDir(HELP_TOP_DIR);
        ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(
            "idv.tools.colortableeditor");
    }

    /**
     * Was OK pressed
     *
     * @return Are the changes ok
     */
    public boolean getOk() {
        return ok;
    }

    /**
     * Test main
     *
     * @param args cmd line args
     */
    public static void main(String[] args) {
        //        ColorTableEditor cte = new ColorTableEditor();
    }

}

