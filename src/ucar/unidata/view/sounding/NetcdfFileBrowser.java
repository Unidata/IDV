/*
 * $Id: NetcdfFileBrowser.java,v 1.22 2007/04/02 21:59:35 dmurray Exp $
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
import ucar.unidata.data.sounding.NetcdfSoundingAdapter;
import ucar.unidata.data.sounding.SoundingAdapter;

import ucar.unidata.util.LogUtil;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.beans.*;

import java.io.File;

import javax.swing.JFrame;


/**
 * A browser for finding netCDF upper air files.
 *
 * @author Unidata development team
 * @version $Revision: 1.22 $
 */
public class NetcdfFileBrowser extends SoundingFileBrowser {


    /** flag for file changes */
    private boolean ignoreChangingFile = false;

    /**
     * Construct an object for selecting sounding files starting at
     * the current directory
     */
    NetcdfFileBrowser() {
        this(".");
    }

    /**
     * Construct an object for selecting sounding files starting at
     * the specified directory.
     *
     * @param  directoryName   starting directory to search for files.
     */
    NetcdfFileBrowser(String directoryName) {
        super(directoryName);
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
        SoundingAdapter nc = null;
        try {
            nc = new NetcdfSoundingAdapter(selectedFile);
        } catch (Exception exc) {
            LogUtil.logException("Creating netcdf sounding:" + selectedFile,
                                 exc);
            return;
        }
        if (nc.getSoundingTimes() != null) {
            try {
                getSoundingAdapterProperty().setValueAndNotifyListeners(nc);
                ignoreChangingFile = true;
                selectedFileDisplay.setText(selectedFile.getPath());
                ignoreChangingFile = false;
            } catch (PropertyVetoException excpt) {
                LogUtil.logException("New netCDF dataset was vetoed: ",
                                     excpt);
            }
        } else {
            LogUtil.userMessage("Unable to read data from file "
                                + selectedFile);
        }
    }


    /**
     * Test routine.
     *
     * @param args  name of file or directory if supplied
     */
    public static void main(String[] args) {

        JFrame frame = new JFrame("Netcdf Sounding Browser Test");

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        NetcdfFileBrowser ncfb = new NetcdfFileBrowser((args.length > 0)
                ? args[0]
                : "/var/data/ldm/decoded");

        frame.getContentPane().add(ncfb.getContents());
        frame.pack();
        frame.show();
    }
}

