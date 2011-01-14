/*
 * $Id: GarpImageSelector.java,v 1.8 2005/05/13 18:32:20 jeffmc Exp $
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

package ucar.unidata.ui.imagery;



import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.File;
import java.io.FileFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import ucar.unidata.ui.RadioButtonFileSelector;


/**
 * Simulates the NSAT/GARP image selection widget.
 * @author Don Murray
 */
public class GarpImageSelector extends JPanel
        implements ItemListener, ImageSelector {

    /** data sources */
    private RadioButtonFileSelector dataSources;

    /** data scales */
    private RadioButtonFileSelector dataScales;

    /** product types */
    private RadioButtonFileSelector productTypes;

    /** list of times */
    private JList timesList;

    /** directory filter */
    private DirFilter dirFilter;

    /** top level directory */
    private String topDir = null;

    /** list of images */
    private ArrayList imageList;

    /** data source names */
    private String dataSourcesName = null;

    /** data scale names */
    private String dataScalesName = null;

    /** product type names */
    private String productTypesName = null;

    /** file separator */
    private String slash = System.getProperty("file.separator", "/");

    /**
     * Construct an image selection widget.
     *
     * @param   topLevelDirectoryPath   top level directory path name
     */
    public GarpImageSelector(String topLevelDirectoryPath) {
        this(new File(topLevelDirectoryPath), 4);
    }

    /**
     * Construct an image selection widget.  Each section will have
     * the specified number of columns.
     *
     * @param topLevelDirectoryPath   top level directory path name
     * @param columns   number of columns for layout
     */
    public GarpImageSelector(String topLevelDirectoryPath, int columns) {
        this(new File(topLevelDirectoryPath), columns);
    }

    /**
     * Construct an image selection widget.
     *
     * @param   topLevelDirectory   top level directory
     */
    public GarpImageSelector(File topLevelDirectory) {
        this(topLevelDirectory, 4);
    }

    /**
     * Construct an image selection widget.  Each section will have
     * the specified number of columns.
     *
     * @param topLevelDirectory   top level directory
     * @param columns   number of columns for layout
     */
    public GarpImageSelector(File topLevelDirectory, int columns) {
        topDir = topLevelDirectory.getPath();

        // get the main components
        this.setLayout(new GridLayout(0, 1));

        // Set up top level directory listing
        dirFilter = new DirFilter();

        // Data Sources;
        dataSources =
            new RadioButtonFileSelector("Data Sources",
                                        getDirList(topLevelDirectory),
                                        columns);
        dataSources.addItemListener(this);
        this.add(dataSources);

        // Data Scales
        dataScales = new RadioButtonFileSelector("Data Scale", columns);
        dataScales.addItemListener(this);
        this.add(dataScales);

        // Product Types
        productTypes = new RadioButtonFileSelector("Product Types", columns);
        productTypes.addItemListener(this);
        this.add(productTypes);

        // Available times
        timesList = new JList();
        timesList.setVisibleRowCount(10);
        JScrollPane timeScroll = new JScrollPane(timesList);
        this.add(timeScroll);

        setPreferredSize(new java.awt.Dimension(300, 600));
    }

    /**
     * Get the list of files in the directory
     *
     * @param directoryName   name of the directory
     * @return  array of filenames
     */
    private String[] getFileList(File directoryName) {
        String[] files = directoryName.list();
        Arrays.sort(files);
        return files;
    }

    /**
     * Get the list of directories in a directory
     *
     * @param directoryName  directory name
     * @return  array of directory names
     */
    private String[] getDirList(File directoryName) {
        File[] files = directoryName.listFiles(dirFilter);
        if (files == null) {
            return new String[0];  // catch no such directory
        }
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            names[i] = files[i].getName();
        }
        Arrays.sort(names);
        return names;
    }

    /**
     * Method to handle the events when an item is selected.
     *
     * @param e  ItemEvent to handle
     */
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() instanceof RadioButtonFileSelector) {
            String file = ((JRadioButton) e.getItem()).getName();
            if ((RadioButtonFileSelector) e.getSource() == dataSources) {
                productTypes.clearEntries();
                clearTimesList();
                dataSourcesName = file;
                dataScales.setButtonList(getDirList(new File(topDir + slash
                        + file)));
            } else if ((RadioButtonFileSelector) e.getSource()
                       == dataScales) {
                clearTimesList();
                dataScalesName = file;
                productTypes.setButtonList(getDirList(new File(topDir + slash
                        + dataSourcesName + slash + file)));
            } else if ((RadioButtonFileSelector) e.getSource()
                       == productTypes) {
                productTypesName = file;
                String[] names = getFileList(new File(topDir + slash
                                                      + dataSourcesName
                                                      + slash
                                                      + dataScalesName
                                                      + slash + file));
                ArrayList times = new ArrayList();
                for (int i = 0; i < names.length; i++) {
                    if (names[i].indexOf("_") != names[i].lastIndexOf("_")) {
                        times.add(
                            new String(
                                names[i].substring(names[i].indexOf("_") + 1, names[i].lastIndexOf("_")).trim()
                                + slash
                                + names[i].substring(
                                    names[i].lastIndexOf("_") + 1).trim()));
                    }
                }
                if (times.size() > 1) {
                    Collections.sort(times);
                }
                timesList.setListData(times.toArray());
                timesList.ensureIndexIsVisible(times.size() - 1);
            }
        }
    }

    /**
     * Clear the list of times
     */
    private void clearTimesList() {
        timesList.setListData(new String[]{ "" });
    }

    /**
     * Returns a list of the images to load or null if none have been
     * selected.
     *
     * @return  list   list of selected images
     */
    public List getImageList() {
        int[] indices = timesList.getSelectedIndices();
        imageList = new ArrayList();
        if (indices.length > 0) {
            Object[] files = timesList.getSelectedValues();
            String path = topDir + slash + dataSourcesName + slash
                          + dataScalesName + slash + productTypesName;
            for (int i = 0; i < indices.length; i++) {
                String file = (String) files[i];
                if ((file != null) && (file.indexOf(slash) > 0)) {
                    imageList.add(new String(path + slash
                                             + decodeName(file)));
                }
            }
        } else {
            System.out.println("No image times selected");
        }
        return imageList;
    }

    /**
     * Decode the GEMPAK file name
     *
     * @param listName   list name
     * @return  decoded name.
     */
    private String decodeName(String listName) {
        return new String(productTypesName + "_"
                          + listName.substring(0, listName.indexOf(slash))
                          + "_"
                          + listName.substring(listName.indexOf(slash) + 1));
    }

    /**
     * Class DirFilter
     */
    class DirFilter implements FileFilter {

        /**
         * Check whether we should accept a particular file
         *
         * @param file  file to check
         * @return  true if it is a directory
         */
        public boolean accept(File file) {
            String name = file.getName();
            return file.isDirectory();
        }
    }

    /**
     *  Set the top level directory for the widget
     *
     * @param  directory   top level directory
     */
    public void setTopLevelDirectory(String directory) {
        setTopLevelDirectory(new File(directory));
    }

    /**
     *  Set the top level directory for the widget
     *
     * @param  topLevelDirectory   top level directory
     */
    public void setTopLevelDirectory(File topLevelDirectory) {
        topDir = topLevelDirectory.getPath();
        dataSources.clearEntries();
        dataScales.clearEntries();
        productTypes.clearEntries();
        clearTimesList();
        dataSources.setButtonList(getDirList(new File(topDir)));
    }

    /**
     * The main.
     * Test by running:
     *  <pre>
     *    java ucar.unidata.ui.imagery.GarpImageSelector top_level_image_dir
     *  </pre>
     *
     * @param args  top level directory
     */
    public static void main(String[] args) {
        String satDir;
        satDir = (args.length == 0)
                 ? "/data/ldm/gempak/images/sat"
                 : args[0];

        final GarpImageSelector selector = new GarpImageSelector(satDir);
        JFrame frame = new JFrame("GARP-like Image Selection Widget");
        frame.setTitle("GARP-like Image Selection Widget");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(selector, BorderLayout.CENTER);
        JButton display = new JButton("Display Image List");
        display.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                List list = selector.getImageList();
                for (int i = 0; i < list.size(); i++) {
                    System.out.println(list.get(i));
                }
            }
        });
        JButton close = new JButton("Close");
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        JPanel p = new JPanel();
        p.add(display);
        p.add(close);
        contentPane.add(p, BorderLayout.SOUTH);
        frame.pack();
        frame.setSize(500, 640);
        frame.setVisible(true);
        /*
        */
        try {
            Thread.sleep(10000);
            selector.setTopLevelDirectory(
                System.getProperty(
                    "GarpImageSelector.TopLevelDirectory", "/home/dmurray"));
        } catch (Exception e) {
            ;
        }
    }
}





