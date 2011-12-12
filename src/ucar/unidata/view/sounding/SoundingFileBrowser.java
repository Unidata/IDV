/*
 * $Id: SoundingFileBrowser.java,v 1.1 2007/04/02 21:59:35 dmurray Exp $
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




package ucar.unidata.view.sounding;


import ucar.unidata.beans.NonVetoableProperty;
import ucar.unidata.beans.Property;
import ucar.unidata.beans.PropertySet;
import ucar.unidata.data.sounding.CMASoundingAdapter;
import ucar.unidata.data.sounding.NetcdfSoundingAdapter;
import ucar.unidata.data.sounding.SoundingAdapter;


import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;



import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.beans.*;

import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;


/**
 * A browser for finding netCDF upper air files.
 *
 * @author Unidata development team
 * @version $Revision: 1.1 $
 */
public class SoundingFileBrowser {

    /** PatternFileFilter for upper air netCDF files */
    public static final PatternFileFilter FILTER_NC =
        new PatternFileFilter(".*ua\\.nc$,Upperair.*\\.nc$",
                              "netCDF Upper Air files (*ua.nc)");

    /** PatternFileFilter for CMA upper air files */
    public static final PatternFileFilter FILTER_CMA_UA =
        new PatternFileFilter(".*\\.ta$", "CMA Upper Air files (*.ta)");

    /** property for the sounding adapter */
    private Property soundingAdapterProperty;

    /** property set */
    private PropertySet propertySet;

    /** selected file input */
    protected JTextField selectedFileDisplay;

    /** flag for file changes */
    private boolean ignoreChangingFile = false;

    /** frame for the browse */
    private static JFrame frame = null;

    /** frame contents */
    private JPanel contents;

    /**
     * Construct an object for selecting sounding files starting at
     * the current directory
     */
    SoundingFileBrowser() {
        this(".");
    }

    /**
     * Construct an object for selecting sounding files starting at
     * the specified directory.
     *
     * @param  directoryName   starting directory to search for files.
     */
    SoundingFileBrowser(String directoryName) {

        // set up the properties
        propertySet = new PropertySet();

        propertySet.addProperty(soundingAdapterProperty =
            new NonVetoableProperty(this, "soundingAdapter"));

        File selectedFile = new File((directoryName != null)
                                     ? directoryName
                                     : ".");


        selectedFileDisplay = new JTextField(30);
        selectedFileDisplay.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                checkNewFile(new File(selectedFileDisplay.getText().trim()));
            }
        });
        GuiUtils.setNoFill();
        GuiUtils.tmpInsets = new Insets(0, 2, 0, 2);
        contents = GuiUtils.doLayout(new Component[] { selectedFileDisplay,
                fileSelectionButton() }, 2, GuiUtils.WT_N, GuiUtils.WT_N);
    }


    /**
     * Create a file selection button
     * @return the file selection button
     */
    private JButton fileSelectionButton() {
        JButton fileSelectButton = new JButton("Select File...");
        fileSelectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //Read the file - don't include the "include all" file filter.
                String file =
                    FileManager.getReadFile("Select Upper Air File",
                                            Misc.newList(FILTER_NC,
                                                FILTER_CMA_UA));
                if (file == null) {
                    return;
                }
                checkNewFile(new File(file));
            }
        });

        return fileSelectButton;
    }

    /**
     * Check the status of the file.
     *
     * @param selectedFile   file to use for checking
     */
    protected void checkNewFile(File selectedFile) {
        if (ignoreChangingFile) {
            return;
        }
        if ( !selectedFile.exists()) {
            LogUtil.userMessage("File does not exist:" + selectedFile);
            return;
        }
        SoundingAdapter adapter = null;
        try {
            adapter = new NetcdfSoundingAdapter(selectedFile);
        } catch (IllegalArgumentException ill) {
            System.out.println(ill.getMessage());
            try {
                adapter = new CMASoundingAdapter(selectedFile);
            } catch (Exception exc) {
                LogUtil.logException("Reading sounding:" + selectedFile, exc);
                return;
            }
        } catch (Exception exc) {
            LogUtil.logException("Reading sounding:" + selectedFile, exc);
            return;
        }
        if (adapter.getSoundingTimes() != null) {
            try {
                soundingAdapterProperty.setValueAndNotifyListeners(adapter);
                ignoreChangingFile = true;
                selectedFileDisplay.setText(selectedFile.getPath());
                ignoreChangingFile = false;
            } catch (PropertyVetoException excpt) {
                LogUtil.logException("New sounding dataset was vetoed: ",
                                     excpt);
            }
        } else {
            LogUtil.userMessage("Unable to read data from file "
                                + selectedFile);
        }
    }


    /**
     * Get the contents of this browser.
     *
     * @return browser contents
     */
    public JPanel getContents() {
        return contents;
    }

    /**
     * Get the SoundingAdapter property
     *
     * @return the SoundingAdapter property
     */
    protected Property getSoundingAdapterProperty() {
        return soundingAdapterProperty;
    }

    /**
     * Get the SoundingAdapter associated with this browser
     * @return the associated SoundingAdapter
     */
    public SoundingAdapter getSoundingAdapter() {
        return (SoundingAdapter) soundingAdapterProperty.getValue();
    }

    /**
     * Adds a property change listener.
     *
     * @param listener          The property change listener.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySet.addPropertyChangeListener(listener);
    }

    /**
     * Removes a property change listener.
     *
     * @param listener          The property change listener.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        propertySet.removePropertyChangeListener(listener);
    }

    /**
     * Adds a property change listener for a named property.
     *
     * @param name              The name of the property.
     * @param listener          The property change listener.
     */
    public void addPropertyChangeListener(String name,
                                          PropertyChangeListener listener) {
        propertySet.addPropertyChangeListener(name, listener);
    }

    /**
     * Removes a property change listener for a named property.
     *
     * @param name              The name of the property.
     * @param listener          The property change listener.
     */
    public void removePropertyChangeListener(
            String name, PropertyChangeListener listener) {
        propertySet.removePropertyChangeListener(name, listener);
    }

    /**
     * Test routine.
     *
     * @param args  name of file or directory if supplied
     */
    public static void main(String[] args) {

        frame = new JFrame("Sounding Browser Test");

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        SoundingFileBrowser ncfb = new SoundingFileBrowser((args.length > 0)
                ? args[0]
                : "/var/data/ldm/decoded");

        frame.getContentPane().add(ncfb.getContents());
        frame.pack();
        frame.show();
    }
}

