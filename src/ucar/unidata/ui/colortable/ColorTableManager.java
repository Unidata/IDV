/*
 * $Id: ColorTableManager.java,v 1.25 2007/06/15 19:34:38 jeffmc Exp $
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


import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.util.ColorTable;
import ucar.unidata.ui.ImageUtils;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.NamedObject;


import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.ResourceCollection;


import ucar.unidata.util.ResourceManager;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;



import visad.*;

import java.awt.*;
import java.awt.image.*;

import java.awt.event.*;

import java.beans.*;

import java.io.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


import javax.swing.*;



/**
 * A class to manage a collection of system and user color tables
 *
 * @author Metapps Development Team
 * @version $Id: ColorTableManager.java,v 1.25 2007/06/15 19:34:38 jeffmc Exp $
 */
public class ColorTableManager extends ResourceManager {


    /** The singleton */
    private static ColorTableManager manager;


    /** THe name of the default color table */
    public static final String NAME_DEFAULT = ColorTableDefaults.NAME_DEFAULT;

    /** The color table property for PropertyChangeListeners */
    public static final String PROP_COLORTABLE =
        ColorTableCanvas.PROP_COLORTABLE;

    /** The range property for PropertyChangeListeners */
    public static final String PROP_RANGE = ColorTableCanvas.PROP_RANGE;

    /** The cancel property for PropertyChangeListeners */
    public static final String PROP_CANCEL = ColorTableCanvas.PROP_CANCEL;

    /** The close property for PropertyChangeListeners */
    public static final String PROP_CLOSE = ColorTableCanvas.PROP_CLOSE;


    /** Xml tag name */
    public String TAG_COLORTABLES = "colortables";

    /** Xml tag name */
    public String TAG_COLORTABLE = "colortable";

    /** Xml tag name */
    public String TAG_NAME = "name";

    /** The encoder for writing/reading color table xml */
    private XmlEncoder encoder;



    /** File filter used for IDV color tables */
    public static final PatternFileFilter FILTER_IDV =
        new PatternFileFilter(".+\\.xml", "IDV color table (*.xml)", ".xml");

    /** File filter used for Gempak color tables */
    public static final PatternFileFilter FILTER_GEM =
        new PatternFileFilter(".+\\.tbl", "GEMPAK color table (*.tbl)",
                              ".tbl");

    /** File filter used for McIdas color tables */
    public static final PatternFileFilter FILTER_MCI =
        new PatternFileFilter(".+\\.et", "McIDAS color table (*.et)", ".et");


    /** File filter used for PAL color tables */
    public static final PatternFileFilter FILTER_PAL =
        new PatternFileFilter("(.+\\.pal|.+\\.pa1|.+\\.pa2)",
                              "PAL color table (*.pal,*.pa1,*.pa2)");


    /** File filter used for ACT color tables */
    public static final PatternFileFilter FILTER_ACT =
        new PatternFileFilter(".+\\.act", "ACT color table (*.act)");



    //   /** File filter used for CWC color tables */
    //    public static final PatternFileFilter FILTER_CWC =
    //        new PatternFileFilter(".+\\.cwc", "CWC color table (*.cwc)");




    /**
     * Create me
     *
     */
    public ColorTableManager() {}


    public void doExport(NamedObject object, String file) {
	if(!ImageUtils.isImage(file)) {
	    super.doExport(object, file);
            return;
	}
	try {
	    Image image =ColorTableCanvas.getImage((ColorTable) object, 200,30);
	    ImageUtils.writeImageToFile(image,  new File(file));
	} catch(Exception exc) {
	    throw new RuntimeException(exc);
	}
	
    }

    /**
     * set the singleton manager
     *
     * @param theManager The singleton manager
     */
    public static void setManager(ColorTableManager theManager) {
        manager = theManager;
    }

    /**
     * Get the singleton
     *
     * @return The color table manager
     */
    public static ColorTableManager getManager() {
        return manager;
    }


    /**
     * Return the file filters used for writing a file on an export
     *
     * @return Write file filters
     */
    public List getWriteFileFilters() {
        return Misc.newList(FILTER_IDV, FILTER_GEM);
    }

    /**
     * Return the file filters used for writing a file on an import
     *
     * @return Read file  filters
     */
    public List getReadFileFilters() {
        List filters = Misc.newList(FILTER_IDV, FILTER_GEM, FILTER_MCI);
        filters.addAll(Misc.newList(FILTER_PAL, FILTER_ACT));
        return filters;
    }


    /**
     * Overwrite  the base class (ResourceManager) method to return the name
     * of the resource we deal with.
     *
     * @return The resource title
     */
    public String getTitle() {
        return "Color table";
    }

    /**
     * Return a list of all ColorTable objects
     *
     * @return List of color tables
     */
    public List getColorTables() {
        return getResources();
    }

    /**
     * Return a list (String) of the categories defined by the color tables
     *
     * @return List of color table categories
     */
    public List getCategories() {
        Hashtable map         = new Hashtable();
        List      colorTables = getColorTables();
        for (int i = 0; i < colorTables.size(); i++) {
            ColorTable ct = (ColorTable) colorTables.get(i);
            if (ct.getCategory() != null) {
                map.put(ct.getCategory(), "");
            }
        }

        List categories = new ArrayList();
        for (Enumeration e = map.keys(); e.hasMoreElements(); ) {
            categories.add(e.nextElement());
        }
        return categories;
    }


    /**
     * Find and return the default color table
     *
     * @return The default color table
     */
    public ColorTable getDefaultColorTable() {
        return getColorTable(NAME_DEFAULT);
    }


    /**
     * Lookup and return the color table identified by the given name
     *
     * @param name The color table name
     * @return The color table or null if none found with the given name
     */
    public ColorTable getColorTable(String name) {
        return (ColorTable) getObject(name);
    }



    /**
     * Create a ColorTableEditor for editing the given color table
     *
     * @param colorTable The color table to edit
     * @param listener Who should get changes sent to (may be null)
     * @return The new color table editor
     */
    public ColorTableEditor edit(ColorTable colorTable,
                                 PropertyChangeListener listener) {
        ColorTableEditor cte = new ColorTableEditor(this,
                                   new ColorTable(colorTable), listener);
        cte.show();
        return cte;
    }


    /**
     *  Make the set of color table menus. On an action event we will call the listeners actionPerformed \
     *  method, passing in the chosen color table.
     *
     *  @param listener The listener to pass events to.
     *  @param l The list to add the JMenus to
     */
    public void makeColorTableMenu(final ObjectListener listener, List l) {
        makeColorTableMenu(listener, l, false);
    }


    public JLabel getLabel(String colorTableName) {
        ColorTable ct   = getColorTable(colorTableName);
        if(ct==null) return null;
        Icon       icon = ColorTableCanvas.getIcon(ct);
        //        return new JLabel(new ImageIcon(icon));
        return new JLabel(icon);
    }




    boolean didit = false;

    private void doit() {
        didit = true;
        List      tables     = getColorTables();
        try {
            for (int i = 0; i < tables.size(); i++) {
                ColorTable ct   = (ColorTable) tables.get(i);
                Image image = ColorTableCanvas.getImage(ct, 100, 20);
                String name = IOUtil.cleanFileName(ct.getName());
                name  = name.replace(" ","_");
                ImageUtils.writeImageToFile(image, name+".png");
                System.out.println("colortable." + ct.getName() +".icon"  +"=" + name+".png");
                System.out.println("colortable." + ct.getName() +".category"  +"=" + ct.getCategory());
            }
        } catch(Exception exc) {
            exc.printStackTrace();
        }
    }



    /**
     *  Make the set of color table menus. On an action event we will call the listeners actionPerformed \
     *  method, passing in the chosen color table.
     *
     *  @param listener The listener to pass events to.
     *  @param l The list to add the JMenus to
     *  @param showLocal If true then add  "<local>"  to the menu item names for local  color tables.
     */
    public void makeColorTableMenu(final ObjectListener listener, List l,
                                   boolean showLocal) {
        //        if(!didit) doit();
        int       listIndex  = l.size();
        Hashtable categories = new Hashtable();
        List      tables     = getColorTables();
        List      items      = null;

        for (int i = 0; i < tables.size(); i++) {
            ColorTable ct   = (ColorTable) tables.get(i);
            Icon       icon = ColorTableCanvas.getIcon(ct);
            String     name = GuiUtils.getLocalName(ct.getName(),isUsers(ct)&& showLocal);
            JMenuItem item = ((icon != null)
                              ? new JMenuItem(name, icon)
                              : new JMenuItem(name));
            item.addActionListener(new ObjectListener(ct) {
                public void actionPerformed(ActionEvent ae) {
                    listener.actionPerformed(ae, (ColorTable) theObject);
                }
            });


            String category = ct.getCategory();
            if ((category == null) || (category.trim().length() == 0)) {
                if (items == null) {
                    items = new ArrayList();
                }
                items.add(item);
                continue;
            }
            List   cats         = StringUtil.split(category, ">", true, true);
            JMenu  categoryMenu = null;
            String catSoFar     = "";
            String menuCategory = "";
            for (int catIdx = 0; catIdx < cats.size(); catIdx++) {
                String subCat = (String) cats.get(catIdx);
                catSoFar = catSoFar + "/" + subCat;
                JMenu m = (JMenu) categories.get(catSoFar);
                if (m == null) {
                    m            = new JMenu(subCat);
                    menuCategory = catSoFar;
                    categories.put(catSoFar, m);
                    if (categoryMenu != null) {
                        categoryMenu.add(m, 0);
                    } else {
                        l.add(m);
                    }
                }
                categoryMenu = m;
            }


            if (categoryMenu == null) {
                categoryMenu = new JMenu("");
                categories.put(ct.getCategory(), categoryMenu);
                l.add(categoryMenu);
                menuCategory = ct.getCategory();
            }

            if (false && (categoryMenu.getItemCount() > 20)) {
                JMenu moreMenu = new JMenu("More");
                categoryMenu.add(moreMenu);
                categoryMenu = moreMenu;
                categories.put(menuCategory, categoryMenu);
            }
            categoryMenu.add(item);
        }
        if (items != null) {
            l.addAll(listIndex, items);
        }
        GuiUtils.limitMenuSize(l, "Group", 20);

    }







    /**
     * Merge the programmaticcally created color tables with the ones given
     * on the command line. If the first command line argument is "-merge"
     * then don't load in the programmatic color tables, just merge the command line
     * ones.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        List theList  = new ArrayList();
        int  startIdx = 0;
        if ((args.length == 0) || !args[0].equals("-merge")) {
            theList.addAll(ColorTableDefaults.createColorTables());
        } else {
            startIdx = 1;
        }
        for (int i = startIdx; i < args.length; i++) {
            try {
                System.err.println("Processing:" + args[i]);
                String contents = IOUtil.readContents(args[i],
                                      ColorTableManager.class);
                if (contents == null) {
                    System.err.println("Failed to read:" + args[i]);
                    continue;
                }
                Object o = (new XmlEncoder()).toObject(contents);
                if (o == null) {
                    System.err.println("Failed to process:" + args[i]);
                    continue;
                }
                if (o instanceof Collection) {
                    theList.addAll((Collection) o);
                } else if (o instanceof ColorTable) {
                    theList.add(o);
                } else {
                    System.err.println("Unknown object:"
                                       + o.getClass().getName());
                }
            } catch (Exception exc) {
                System.err.println("Error processing:" + args[i]);
                exc.printStackTrace();
            }

        }

        String xml = (new XmlEncoder()).toXml(theList);
        System.out.println(xml);
    }


    /**
     * Is the given file a Gempak file
     *
     * @param file The file name to check
     * @return Is it a Gempak file
     */
    private boolean isGempakFile(String file) {
        return file.toLowerCase().endsWith(".tbl");
    }

    /**
     * When we do an export this method is called to create the String
     * contents that is actually written out. We implement this method here
     * so if we are saving as a Gempak file we can write  it out in that format.
     *
     * @param object The color table
     * @param file The file name
     * @return The contents to be written to the file
     */
    protected String getExportContents(NamedObject object, String file) {
        if ( !isGempakFile(file)) {
            return super.getExportContents(object, file);
        }

        StringBuffer c =
            new StringBuffer("!\n!This table was created from the IDV's "
                             + object + " color table\n");
        ColorTable ct       = (ColorTable) object;
        float[][]  table    = ct.getColorTable();
        String     template = "                       red    green    blue ";
        c.append("!");
        c.append("                       red    green  blue ");
        c.append("\n!\n");
        String[] names = { "red", "green", "blue" };

        float    v;
        for (int i = 0; i < table[0].length; i++) {
            String line = template;
            for (int ci = 0; ci < 3; ci++) {
                v = table[ci][i] * 255.0f;
                line = StringUtil.replace(line, names[ci],
                                          StringUtil.padLeft("" + (int) v,
                                              3));
            }
            c.append(line);
            c.append("\n");
        }
        return c.toString();
    }


    /**
     * Check to see if these are any of the special resources
     *
     * @param resources resources
     * @param index which one
     *
     * @return The resource instantiated
     */
    protected Object initResource(ResourceCollection resources, int index) {
        String file = (String) resources.get(index);
        try {
            List cts = processSpecial(file,
                                      resources.getProperty("name", index),
                                      resources.getProperty("category",
                                          index));
            if (cts != null) {
                return cts;
            }
        } catch (IOException ioe) {
            return null;
        }
        return super.initResource(resources, index);
    }


    /**
     * Try to load in one of the special colortables
     *
     * @param file file
     * @param name _more_
     * @param category category
     *
     * @return the ct
     *
     * @throws IOException _more_
     */
    private List processSpecial(String file, String name, String category)
            throws IOException {
        String cat = category;
        if (name == null) {
            name = IOUtil.stripExtension(IOUtil.getFileTail(file));
        }
        if (cat == null) {
            cat = ColorTable.CATEGORY_BASIC;
        }

        String suffix = file.toLowerCase();
        List   cts    = new ArrayList();
        if (suffix.endsWith(".et")) {
            if (category == null) {
                cat = ColorTable.CATEGORY_SATELLITE;
            }
            cts.add(ColorTableDefaults.createColorTable(name, cat,
                    ColorTableDefaults.makeTableFromET(file, false)));
        } else if (suffix.endsWith(".pa1")) {
            cts.add(ColorTableDefaults.createColorTable(name, cat,
                    ColorTableDefaults.makeTableFromPal1(file)));
        } else if (suffix.endsWith(".pa2")) {
            cts.add(ColorTableDefaults.createColorTable(name, cat,
                    ColorTableDefaults.makeTableFromPal2(file)));
        } else if (suffix.endsWith(".pal")) {
            cts.add(ColorTableDefaults.createColorTable(name, cat,
                    ColorTableDefaults.makeTableFromPal(file)));
        } else if (suffix.endsWith(".act")) {
            cts.add(ColorTableDefaults.createColorTable(name, cat,
                    ColorTableDefaults.makeTableFromAct(file)));
        } else if (suffix.endsWith(".ncmap")) {
            //Treat these like gempak
            cts.addAll(ColorTableDefaults.makeGempakColorTables(name, cat,
                    file));
        } else if (suffix.endsWith(".gp")) {
            cts.addAll(ColorTableDefaults.makeNclRgbColorTables(name, cat,
                    file, null));
        } else if (isGempakFile(file)) {
            cts.addAll(ColorTableDefaults.makeGempakColorTables(name, cat,
                    file));
        } else if (suffix.endsWith(".rgb")) {
            cts.addAll(ColorTableDefaults.makeRgbColorTables(name, cat,
                    file));
        } else {
            return null;
        }
        if (cts.size() == 0) {
            return null;
        }
        return cts;

    }


    /**
     * Import a color table
     *
     * @param makeUnique If true then we change the name of the color table so it is unique
     * @return The imported color table
     */
    public NamedObject doImport(boolean makeUnique) {
        String file = FileManager.getReadFile(getTitle() + " import",
                          getReadFileFilters());
        if (file == null) {
            return null;
        }

        try {
            List cts = processSpecial(file, null, null);
            if (cts != null) {
                return doImport(cts, makeUnique);
            }
        } catch (IOException ioe) {
            LU.printException(log_, "Error reading file: " + file, ioe);

            return null;
        }
        try {
            String xml = IOUtil.readContents(file, ResourceManager.class);
            if (xml == null) {
                return null;
            }
            Object o = (new XmlEncoder()).toObject(xml);
            return doImport(o, makeUnique);
        } catch (Exception exc) {
            LU.printException(log_, "Error reading file: " + file, exc);
        }
        return null;
    }



}

