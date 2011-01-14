/*
 * $Id: StationModelCanvas.java,v 1.69 2007/08/08 18:55:26 jeffmc Exp $
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


package ucar.unidata.ui.symbol;


import org.w3c.dom.Element;

import ucar.unidata.ui.FineLineBorder;

import ucar.unidata.ui.drawing.*;





import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.*;

import visad.Unit;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;




/**
 * Class StationModelCanvas Manages the station model gui
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.69 $
 */
public class StationModelCanvas extends EditCanvas {


    /** Set background to black */
    private static final String CMD_BLACKBG = "cmd.blackbg";

    /** Set background to white */
    private static final String CMD_WHITEBG = "cmd.whitebg";

    /** Xml tag name for the shape */
    private static final String TAG_SYMBOL = "symbol";

    /** Xml attribute name for the shape java class */
    private static final String ATTR_CLASS = "class";

    /** Xml attribute name for   the shape name */
    private static final String ATTR_NAME = "name";

    /** Xml attribute name for  the shape icon */
    private static final String ATTR_ICON = "icon";

    /** Xml attribute name for the shape attrs */
    private static final String ATTR_ATTRS = "attrs";


    /** The Station model manager */
    private StationModelManager smm;

    /** List of ShapeDescriptors from the xml */
    private List shapeDescriptors;


    /** Menu item for showing alignment points */
    private JCheckBoxMenuItem showAlignmentPointsMI;

    /** The view menu */
    private JMenu stationModelMenu;

    /** File menu */
    private JMenu fileMenu;

    /** The currently edited station model */
    private StationModel stationModel;

    /** The window frame */
    private JFrame frame;

    /** The label that displays the station model name */
    private JLabel nameLabel;

    /** Holds the label */
    private JComponent labelComponent;

    /** Shows the label of the currently highlighted symbol */
    private JLabel highlightLabel;

    /**
     * Make me
     *
     * @param smm The manager
     * @param frame The frame to put me in
     *
     */
    public StationModelCanvas(StationModelManager smm, JFrame frame) {
        this(smm, true, frame);
    }

    /**
     * Make me
     *
     * @param smm The manager
     * @param editable Is this editable
     * @param frame The frame to put me in
     */
    public StationModelCanvas(StationModelManager smm, boolean editable,
                              JFrame frame) {
        this.smm   = smm;
        this.frame = frame;
        initFromDefault();
        this.addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent event) {
                int notches = event.getWheelRotation();
                if (notches < 0) {
                    doZoomIn();
                } else {
                    doZoomOut();
                }
            }
        });

    }

    /**
     * Ovedrride the method so we can set the highlight label
     *
     * @param g Highlighted glyph
     */
    public void setHighlight(Glyph g) {
        super.setHighlight(g);
        if ((g == null) || !(g instanceof MetSymbol)) {
            highlightLabel.setText("   ");
            return;
        }
        highlightLabel.setText(" " + ((MetSymbol) g).getLabel());
    }


    /**
     * Create the name label if needed.
     *
     * @return The component that holds the name label.
     */
    protected JComponent getLabelComponent() {
        if (nameLabel == null) {
            nameLabel = new JLabel();
            Font font = nameLabel.getFont();
            nameLabel.setFont(font.deriveFont(Font.ITALIC | Font.BOLD));
            labelComponent =
                GuiUtils.hflow(Misc.newList(new JLabel("Layout Model: "),
                                            nameLabel));
        }
        return labelComponent;
    }


    /**
     * Set the label
     *
     * @param n The name of the station model
     */
    public void setName(String n) {
        //Make sure we got the label
        getLabelComponent();
        if (nameLabel != null) {
            nameLabel.setText(n);
        }
    }




    /**
     * Use the default station model
     */
    private void initFromDefault() {
        stationModel = smm.getDefaultStationModel();
        if (stationModel == null) {
            stationModel = new StationModel("Default model", new ArrayList());
        }
        setStationModel(stationModel, true);
    }


    /**
     * If there has been a change to the station model then ask the user if they
     * want to save them
     *
     * @return Should the cancel proceed
     */
    boolean okToChange() {
        if (getHaveChanged()) {
            int result =
                GuiUtils.showYesNoCancelDialog(null,
                    "Save changes to station model: "
                    + stationModel.getName(), "Save station model changes");
            if (result == JOptionPane.CANCEL_OPTION) {
                return false;
            }
            if (result == JOptionPane.YES_OPTION) {
                doSave();
            }
        }
        return true;
    }


    /**
     * Set the current station model_
     *
     * @param l The station model
     */
    public void setStationModel(StationModel l) {
        setStationModel(l, true);
    }


    /**
     * Set the current station model
     *
     * @param newModel The station model
     * @param closeDialogs  If true then close any dialog windows
     */
    public void setStationModel(StationModel newModel, boolean closeDialogs) {
        if (closeDialogs) {
            closeDialogs();
        }
        if ( !smm.isUsers(newModel)) {
            List newGlyphs = cloneGlyphs(newModel.getList());
            newModel = new StationModel(newModel.getName(), newGlyphs);
            //new ArrayList(newModel.getList()));
        }
        stationModel = newModel;
        setGlyphs(cloneGlyphs(newModel.getList()));
        setName(stationModel.getDisplayName());
        setHaveChanged(false);
        frame.setTitle(GuiUtils.getApplicationTitle() +"Layout Model Editor -- " + stationModel.getName());
    }

    /**
     * Does this canvas support grouping of glyphs.
     *
     * @return Support grouping of glyphs.
     */
    public boolean doGroup() {
        return false;
    }

    /**
     * Get the current station model
     *
     * @return The current station model
     */
    public StationModel getStationModel() {
        stationModel.setList(getGlyphs());
        return stationModel;
    }

    /**
     * Return the station model name
     *
     * @return The station model name
     */
    public String toString() {
        return stationModel.toString();
    }



    /**
     * Add the menus into the menu bar
     *
     * @param menuBar The menu bar
     */
    public void initMenuBar(JMenuBar menuBar) {
        stationModelMenu = new JMenu("Layout Models");
        stationModelMenu.addMenuListener(new MenuListener() {
            public void menuCanceled(MenuEvent e) {}

            public void menuDeselected(MenuEvent e) {}

            public void menuSelected(MenuEvent e) {
                makeStationModelMenu();
            }
        });

        fileMenu = new JMenu("File");
        fileMenu.addMenuListener(new MenuListener() {
            public void menuCanceled(MenuEvent e) {}

            public void menuDeselected(MenuEvent e) {}

            public void menuSelected(MenuEvent e) {
                makeFileMenu();
            }
        });


        menuBar.add(fileMenu);
        super.initMenuBar(menuBar);

        menuBar.add(stationModelMenu);


        JMenu     helpMenu     = new JMenu("Help");
        JMenuItem helpMenuItem = new JMenuItem("Layout Models");
        helpMenu.add(helpMenuItem);
        helpMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showHelp();
            }
        });

        menuBar.add(helpMenu);
    }


    /**
     * Make the edit menu
     *
     * @param editMenu edit menu
     *
     * @return edit menu
     */
    public JMenu makeEditMenu(JMenu editMenu) {
        editMenu.add(GuiUtils.makeDynamicMenu("Symbols", this,
                "initSymbolsMenu"));
	editMenu.add(GuiUtils.makeMenuItem("Set properties on selected", this, "setPropertiesOnSelected"));
        editMenu.addSeparator();
        return super.makeEditMenu(editMenu);
    }

    public void setPropertiesOnSelected() {
	List selected = getSelection();
	if(selected == null || selected.size()==0) {
	    GuiUtils.showOkDialog(null,"Set range on selected", new JLabel("No shapes are selected"),null);
	    return;
	}
	PropertiesDialog propertiesDialog = new PropertiesDialog(selected, this);
        propertiesDialog.show();
    }



    private MetSymbol highlightedMetSymbol;

    /**
     * initialize the symbols menu
     *
     * @param m menu
     */
    public void initSymbolsMenu(JMenu m) {
        m.removeAll();
        for (int i = 0; i < glyphs.size(); i++) {
            final MetSymbol metSymbol = (MetSymbol) glyphs.get(i);
            JMenuItem mi = GuiUtils.makeMenuItem(metSymbol.getLabel(), this,
                                        "showProperties", metSymbol);
            mi.addMouseListener(new MouseAdapter() {
                    public void  mousePressed(MouseEvent e) {
                        highlightedMetSymbol = null;
                        StationModelCanvas.this.repaint();
                    }

                    public void  mouseReleased(MouseEvent e) {
                        highlightedMetSymbol = null;
                        StationModelCanvas.this.repaint();
                    }

                    public void  mouseEntered(MouseEvent e) {
                        highlightedMetSymbol = metSymbol;
                        StationModelCanvas.this.repaint();
                    }
                    public void  mouseExited(MouseEvent e) {
                        highlightedMetSymbol = null;
                        StationModelCanvas.this.repaint();
                    }
                });
            m.add(mi);
        }
    }

    /**
     * Should we draw the alignment points in the canvas
     *
     * @return Draw the alignment points in the canvas
     */
    public boolean shouldShowAlignmentPoints() {
        if (showAlignmentPointsMI != null) {
            return showAlignmentPointsMI.isSelected();
        }
        return true;
    }

    /**
     * Make the file menu
     */
    private void makeFileMenu() {
        fileMenu.removeAll();
        fileMenu.add(makeMenuItem("New", 'n', GuiUtils.CMD_NEW));
        fileMenu.addSeparator();
        fileMenu.add(makeMenuItem("Save", 's', GuiUtils.CMD_SAVE));
        fileMenu.add(makeMenuItem("Save As...", GuiUtils.CMD_SAVEAS));
        fileMenu.add(makeMenuItem("Rename...", GuiUtils.CMD_RENAME));
        fileMenu.addSeparator();
        JMenuItem removeMenuItem = makeMenuItem("Remove",
                                       GuiUtils.CMD_REMOVE);
        removeMenuItem.setEnabled(smm.isUsers(stationModel));
        fileMenu.add(removeMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(makeMenuItem("Import...", GuiUtils.CMD_IMPORT));
        fileMenu.add(makeMenuItem("Export...", GuiUtils.CMD_EXPORT));
        fileMenu.addSeparator();
        fileMenu.add(makeMenuItem("Close", GuiUtils.CMD_CLOSE));
    }


    /**
     * Make the view menu
     *
     * @param viewMenu view menu
     *
     * @return The view menu
     */
    public JMenu makeViewMenu(JMenu viewMenu) {
        showAlignmentPointsMI =
            new JCheckBoxMenuItem("Show Alignment Points", true);
        viewMenu.add(showAlignmentPointsMI);
        showAlignmentPointsMI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                repaint();
            }
        });

        viewMenu.addSeparator();
        super.makeViewMenu(viewMenu);
        viewMenu.addSeparator();
        viewMenu.add(makeMenuItem("Black background", CMD_BLACKBG));
        viewMenu.add(makeMenuItem("White background", CMD_WHITEBG));
        return viewMenu;
    }

    private static String HELP_TOP_DIR = "/auxdata/docs/userguide";
    public static void setHelpTopDir(String topDir) {
        HELP_TOP_DIR = topDir;
    }


    /**
     * Show the help
     */
    private void showHelp() {
        ucar.unidata.ui.Help.setTopDir(HELP_TOP_DIR);
        ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(
            "idv.tools.stationmodeleditor");
    }


    /**
     * Make the view menu
     */
    private void makeStationModelMenu() {
        stationModelMenu.removeAll();
        List           symbols  = smm.getResources();
        List           items    = new ArrayList();
        ObjectListener listener = new ObjectListener(null) {
            public void actionPerformed(ActionEvent ae) {
                if ( !okToChange()) {
                    return;
                }
                setStationModel((StationModel) theObject, true);
            }
        };
        GuiUtils.makeMenu(stationModelMenu,
                          makeStationModelMenuItems(symbols, listener, smm));
    }


    /**
     * _more_
     *
     * @param symbols _more_
     * @param listener _more_
     * @param smm _more_
     *
     * @return _more_
     */
    public static List makeStationModelMenuItems(List symbols,
            final ObjectListener listener, StationModelManager smm) {
        List      items      = new ArrayList();
        List      subMenus   = new ArrayList();
        Hashtable categories = new Hashtable();
        for (int i = 0; i < symbols.size(); i++) {
            StationModel sm      = (StationModel) symbols.get(i);
            boolean      isUsers = smm.isUsers(sm);
            String       name    = sm.getName();
            if (name.equals("")) continue;
            List         toks    = StringUtil.split(name, ">", true, true);
            if (toks.size() > 0) {
                name = (String) toks.get(toks.size() - 1);
            }
            JMenuItem item = new JMenuItem(GuiUtils.getLocalName(name,
                                 isUsers));
            item.addActionListener(new ObjectListener(sm) {
                public void actionPerformed(ActionEvent ae) {
                    listener.setObject(this.theObject);
                    listener.actionPerformed(ae);
                }
            });

            toks.remove(toks.size() - 1);
            if (toks.size() == 0) {
                items.add(item);
                continue;
            }
            JMenu  categoryMenu = null;
            String catSoFar     = "";
            String menuCategory = "";
            for (int catIdx = 0; catIdx < toks.size(); catIdx++) {
                String subCat = (String) toks.get(catIdx);
                catSoFar = catSoFar + "/" + subCat;
                JMenu m = (JMenu) categories.get(catSoFar);
                if (m == null) {
                    m            = new JMenu(subCat);
                    menuCategory = catSoFar;
                    categories.put(catSoFar, m);
                    if (categoryMenu != null) {
                        categoryMenu.add(m, 0);
                    } else {
                        subMenus.add(m);
                    }
                }
                categoryMenu = m;
            }
            if (categoryMenu == null) {
                categoryMenu = new JMenu("");
                categories.put(toks.toString(), categoryMenu);
                subMenus.add(categoryMenu);
                menuCategory = toks.toString();
            }
            categoryMenu.add(item);
        }
        items.addAll(subMenus);
        return items;
    }



    /**
     * Handle ui actions.
     *
     * @param event The action event
     */
    public void actionPerformed(ActionEvent event) {
        String action = event.getActionCommand();
        if (action.equals(GuiUtils.CMD_SAVE)) {
            doSave();
        } else if (action.equals(GuiUtils.CMD_SAVEAS)) {
            doSaveAs();
        } else if (action.equals(GuiUtils.CMD_RENAME)) {
            doRename();
        } else if (action.equals(GuiUtils.CMD_NEW)) {
            doNew();
        } else if (action.equals(GuiUtils.CMD_IMPORT)) {
            doImport();
        } else if (action.equals(GuiUtils.CMD_EXPORT)) {
            doExport();
        } else if (action.equals(GuiUtils.CMD_REMOVE)) {
            doRemove();
        } else if (action.equals(GuiUtils.CMD_CLOSE)) {
            doClose();
        } else if (action.equals(CMD_BLACKBG)) {
            setCanvasBackground(Color.BLACK);
        } else if (action.equals(CMD_WHITEBG)) {
            setCanvasBackground(Color.WHITE);
        } else {
            super.actionPerformed(event);
        }
    }




    /**
     * Close any open property dialogs
     */
    protected void closeDialogs() {
        for (int i = 0; i < glyphs.size(); i++) {
            MetSymbol metSymbol = (MetSymbol) glyphs.get(i);
            metSymbol.closePropertiesDialog();
        }
    }

    /**
     * Close the gui
     */
    protected void doClose() {}

    /**
     * Save the current station model under a  new  name
     */
    private void doSaveAs() {
        StationModel nl      = getStationModel();
        String       newName = smm.doSaveAs(nl, this);
        if (newName == null) {
            return;
        }
        nl = new StationModel(newName, new ArrayList(nl.getList()));
        setStationModel(nl, false);
        smm.addUsers(nl);
    }

    /**
     * Save the current station model
     */
    public void doSave() {
        doSave(true);
    }

    /**
     * Save the current station model
     *
     * @param fireChangeEvent Do we also fire the change event
     */
    private void doSave(boolean fireChangeEvent) {
        StationModel sm = getStationModel();
        smm.addUsers(sm);
        if (fireChangeEvent) {
            //            sm.firePropertyChangeEvent();
        }
        setHaveChanged(false);
    }

    /**
     * Remove the current station model
     */
    protected void doRemove() {
        if ( !GuiUtils.showYesNoDialog(
                null,
                "Are you sure you want to remove the station model: "
                + stationModel.getName() + "?", "Confirm")) {
            return;
        }
        smm.removeUsers(stationModel);
        initFromDefault();
    }

    /**
     * Rename the current station model
     */
    protected void doRename() {
        if ( !okToChange()) {
            return;
        }
        String newName = smm.doNew(this, "Rename", stationModel.getName(),
                                   "Use '>' to add categories");
        if (newName == null) {
            return;
        }
        doSave(false);
        StationModel sm = getStationModel();
        sm.setName(newName);
        setStationModel(sm, false);
        doSave(true);
    }


    /**
     * Create a new station model
     */
    protected void doNew() {
        if ( !okToChange()) {
            return;
        }
        String newName = smm.doNew(this, "New", "model", "Provide a name for the model");
        if (newName == null) {
            return;
        }
        setStationModel(new StationModel(newName, new ArrayList()), true);
        smm.addUsers(getStationModel());
    }

    /**
     * Import a station model
     */
    private void doImport() {
        if ( !okToChange()) {
            return;
        }
        StationModel nl = (StationModel) smm.doImport(true);
        if (nl == null) {
            return;
        }
        setStationModel(nl, true);
    }

    /**
     * Export the current station model
     */
    private void doExport() {
        smm.doExport(getStationModel());
    }



    /**
     * Create the list of shape descriptors form the xml
     *
     * @return List of shape descriptors
     */
    public List getShapeDescriptors() {
        if (shapeDescriptors == null) {
            shapeDescriptors = new ArrayList();
            List                  sds         = shapeDescriptors;
            XmlResourceCollection symbolTypes = smm.symbolTypes;
            for (int i = 0; i < symbolTypes.size(); i++) {
                Element root = symbolTypes.getRoot(i);
                if (root == null) {
                    continue;
                }
                XmlNodeList symbols = XmlUtil.getElements(root, TAG_SYMBOL);
                for (int j = 0; j < symbols.size(); j++) {
                    final Element symbolNode = (Element) symbols.item(j);
                    ShapeDescriptor sd =
                        new ShapeDescriptor(
                            XmlUtil.getAttribute(symbolNode, ATTR_CLASS),
                            XmlUtil.getAttribute(symbolNode, ATTR_NAME),
                            XmlUtil.getAttribute(
                                symbolNode, ATTR_ICON,
                                "/ucar/unidata/ui/symbol/images/default.gif"), XmlUtil
                                    .getAttribute(
                                        symbolNode, ATTR_ATTRS, "")) {
                        public void initializeGlyph(Glyph g) {
                            super.initializeGlyph(g);
                            initializeGlyphFromXml(g, symbolNode);
                        }
                    };
                    sds.add(sd);
                }
            }
        }
        return shapeDescriptors;
    }



    /**
     * Initialize the new glyph from the given xml node. Only do this if
     * the glyph is a MetSymbol
     *
     * @param g The new glyph
     * @param symbolNode The xml it was created from
     */
    protected void initializeGlyphFromXml(Glyph g, Element symbolNode) {
        if ( !(g instanceof MetSymbol)) {
            return;
        }
        ((MetSymbol) g).initialize(symbolNode);
    }

    /**
     * Make the gui
     *
     * @return _gui
     */
    protected Component doMakeContents() {
        JComponent comp = (JComponent) super.doMakeContents();
        highlightLabel = new JLabel(" ");
        highlightLabel.setBorder(new FineLineBorder(BevelBorder.LOWERED));
        return GuiUtils.centerBottom(comp,
                                     GuiUtils.inset(highlightLabel,
                                         new Insets(2, 0, 2, 0)));
    }



    /**
     * Override the pain method do draw the axis lines
     *
     * @param g The graphics to paint to
     */
    public void paint(Graphics g) {
        Rectangle b = getBounds();
        g.setColor(canvasBg);
        g.fillRect(0, 0, b.width, b.height);
        paintGrid(g);

        Point center = getCenter();
        if (g instanceof Graphics2D) {
            ((Graphics2D) g).translate(center.x, center.y);
        }
        super.paint(g);
        if (g instanceof Graphics2D) {
            ((Graphics2D) g).scale(1.0, 1.0);
        }
        g.setColor(Color.gray);
        g.drawLine(0, -10 * b.height, 0, 10 * b.height);
        g.drawLine(-10 * b.width, 0, 10 * b.width, 0);


        MetSymbol tmp = highlightedMetSymbol;
        if(tmp != null) {
            Rectangle tb = tmp.getBounds();
            g.setColor(Color.red);
            g.drawRect(tb.x-2,tb.y-2,tb.width+4,tb.height+4);
        }
    }



    /**
     * handle event
     *
     * @param e event
     */
    public void keyReleased(KeyEvent e) {
        if (e.isControlDown() && (e.getKeyCode() == KeyEvent.VK_P)) {
            List selectionSet = getSelection();
            if (selectionSet.size() == 1) {
                showProperties((MetSymbol) selectionSet.get(0));
            }
            return;
        }
        super.keyReleased(e);
    }

    /**
     * Make popup menu items for the given glyph
     *
     * @param g The glyph
     * @param l List to put menu items into
     * @return The list
     */
    public List doMakeMenuItems(final Glyph g, List l) {
        JMenuItem mi;
        l = super.doMakeMenuItems(g, l);
        if ( !(g instanceof MetSymbol)) {
            return l;
        }
        return doMakeMetSymbolMenu((MetSymbol) g, l, true);

    }

    /**
     * Make  menu items for the given glyph
     *
     * @param metSymbol The glyph
     * @param l List ot put items in
     * @param forPopup Is this for a popup menu
     * @return The list
     */
    protected List doMakeMetSymbolMenu(final MetSymbol metSymbol, List l,
                                       boolean forPopup) {
        if (l == null) {
            l = new ArrayList();
        }
        JMenuItem mi;
        if ( !forPopup) {
            l = super.doMakeMenuItems(metSymbol, l);
        }

        l.add(mi = new JMenuItem(metSymbol.getActive()
                                 ? "Hide"
                                 : "Show"));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                metSymbol.setActive( !metSymbol.getActive());
            }
        });




        if (metSymbol.doAlignmentMenu()) {
            JMenu rectPtMenu =
                new JMenu("Alignment Point ("
                          + Glyph.getRectPointName(metSymbol.getRectPoint())
                          + ")");
            l.add(rectPtMenu);
            for (int i = 0; i < Glyph.RECTPOINTNAMES.length; i++) {
                rectPtMenu.add(mi = new JMenuItem(Glyph.RECTPOINTNAMES[i]));
                mi.addActionListener(new ObjectListener(Glyph.RECTPOINTS[i]) {
                    public void actionPerformed(ActionEvent ae) {
                        metSymbol.setRectPoint(theObject.toString());
                        setHaveChanged(true);
                        repaint();
                    }
                });
            }
        }

        JMenu alignMenu = new JMenu("Center");
        l.add(alignMenu);

        alignMenu.add(mi = new JMenuItem("Center"));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Rectangle b = metSymbol.getBounds();
                Point2D rp = Glyph.getPointOnRect(metSymbol.getRectPoint(),
                                 b);
                metSymbol.moveBy((int) -(rp.getX()), (int) -(rp.getY()));
                repaint();
                setHaveChanged(true);
            }
        });


        alignMenu.add(mi = new JMenuItem("Center Vertically"));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Rectangle b = metSymbol.getBounds();
                Point2D rp = Glyph.getPointOnRect(metSymbol.getRectPoint(),
                                 b);
                metSymbol.moveBy(0, (int) -(rp.getY()));
                repaint();
                setHaveChanged(true);
            }
        });
        alignMenu.add(mi = new JMenuItem("Center Horizontally"));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Rectangle b = metSymbol.getBounds();
                Point2D rp = Glyph.getPointOnRect(metSymbol.getRectPoint(),
                                 b);
                metSymbol.moveBy((int) -(rp.getX()), 0);
                repaint();
                setHaveChanged(true);
            }
        });



        if (forPopup) {
            l.add(GuiUtils.MENU_SEPARATOR);
            l.add(mi = new JMenuItem("Properties..."));
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    showProperties(metSymbol);
                }
            });
        }

        return l;
    }

    /**
     * Determine whether this canvas should show text in a palette
     * or not.
     *
     * @return Should text be shown
     */
    public boolean showTextInPalette() {
        return true;
    }

    /**
     * User double clicked on the glyph. Show the properties dialog.
     *
     * @param glyph Glyph that was clicked.
     */
    protected void doDoubleClick(Glyph glyph) {
        if (glyph instanceof MetSymbol) {
            showProperties((MetSymbol) glyph);
        }
    }



    /**
     * Determine whether the editor should show parameters or not.
     *
     * @return  true to show parameter selection.
     */
    public boolean getShowParams() {
        return false;
        //return true;
    }

    /**
     *  Gets called when a new glyph has bee created.
     *
     * @param newGlyph
     * @param diddleSelection
     * @param fromPaste
     */
    public void notifyGlyphCreateComplete(Glyph newGlyph,
                                          boolean diddleSelection,
                                          boolean fromPaste) {
        super.notifyGlyphCreateComplete(newGlyph, diddleSelection, fromPaste);
        if (fromPaste) {
            return;
        }
        if (newGlyph instanceof MetSymbol) {
            showProperties((MetSymbol) newGlyph);
        }
    }


    /**
     * Show the properties dialog for the given symbol.
     *
     * @param metSymbol The symbol.
     */
    public void showProperties(MetSymbol metSymbol) {
        metSymbol.showPropertiesDialog(this);
        repaint();
    }


    /**
     * Get the TFO to put in  a menu
     *
     * @param value The value
     * @param formatString The format
     * @return The TFO
     */
    private static TwoFacedObject getFormat(double value,
                                            String formatString) {
        DecimalFormat df = new DecimalFormat(formatString);
        return new TwoFacedObject(df.format(value) + " (" + formatString
                                  + ")", formatString);
    }

    /**
     * Get list of format string
     *
     * @param value The value
     * @return The format strings
     */
    public static List getDefaultFormatList(double value) {
        List formatList = new ArrayList();
        String[] formats = { "###0.#", "###0.0#", "###,###.###", "###.##",
                             "000000.000" };
        for (int i = 0; i < formats.length; i++) {
            formatList.add(formats[i]);
        }
        return formatList;
    }

    /**
     * Get list of default units TFO for the gui
     *
     * @return List of TFOS for choosing units
     */
    public static List getDefaultUnitList() {
        return Misc.newList(new Object[] {
            new TwoFacedObject("Default", null), "Celsius", "Fahrenheit",
            "Kelvin", "millibars", "hectoPascals", "meters", "kilometers",
            "feet", "knots", "meters/second", "miles/hour", "kilometers/hour",
            "furlongs/fortnight"
        });
    }

    /**
     * Return the center point of the canvas.
     * @return center point in canvas coordinates.
     */
    public Point getCenter() {
        Rectangle b = getBounds();
        return new Point(b.width / 2, b.height / 2);
    }

    /*
      The transform... methods are called by the base canvas
       class and the glyphs to transform the coordinate system.
     */

    /**
     * Transform an X coordinate from Java coordinates to canvas
     * coordinates.
     *
     * @param x
     * @return x in canvas coordinates
     */
    public int transformInputX(int x) {
        Point center = getCenter();
        x = x - center.x;
        x = (int) (x / scaleFactor);
        return x;
    }

    /**
     * Transform a Y coordinate from Java coordinates to canvas
     * coordinates.
     *
     * @param y
     * @return y in canvas coordinates
     */
    public int transformInputY(int y) {
        Point center = getCenter();
        y = y - center.y;
        y = (int) (y / scaleFactor);
        return y;
    }

    /**
     * Transform an X coordinate from canvas coordinates to Java
     * (screen) coordinates.
     *
     * @param x
     * @return x in screen coordinates
     */
    public int xxxtransformOutputX(int x) {
        Point center = getCenter();
        return center.x + x;
    }

    /**
     * Transform a Y coordinate from canvas coordinates to Java
     * (screen) coordinates.
     *
     * @param y
     * @return x in screen coordinates
     */
    public int xxxtransformOutputY(int y) {
        Point center = getCenter();
        return center.y + y;
    }

    /**
     * Transform a <code>Rectangle</code> from canvas coordinates to Java
     * (screen) coordinates.
     *
     * @param r
     * @return rectangle in screen coordinates
     */
    public Rectangle xxxtransformOutput(Rectangle r) {
        return new Rectangle(transformOutputX(r.x), transformOutputY(r.y),
                             r.width, r.height);
    }

    /**
     * Transform a <code>Rectangle<code> from Java (screen) coordinates to
     * canvas coordinates.
     *
     * @param r
     * @return rectangle in canvas coordinates
     */
    public Rectangle transformInput(Rectangle r) {
        return new Rectangle(transformInputX(r.x), transformInputY(r.y),
                             r.width, r.height);
    }

    /**
     * Transform a <code>Point</code> from canvas coordinates to Java
     * (screen) coordinates.
     *
     * @param r
     * @return point in screen coordinates
     */
    public Point xxxtransformOutput(Point r) {
        return new Point(transformOutputX(r.x), transformOutputY(r.y));

    }

    /**
     * Transform a <code>Point</code> from Java (screen) coordinates to canvas
     * coordinates.
     *
     * @param r
     * @return rectangle in canvas coordinates
     */
    public Point transformInput(Point r) {
        return new Point(transformInputX(r.x), transformInputY(r.y));

    }


}

