/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.ui;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ucar.unidata.idv.IdvPersistenceManager;


import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.ImageGenerator;
import ucar.unidata.idv.ui.LoadBundleDialog;
import ucar.unidata.idv.ui.WindowInfo;
import ucar.unidata.ui.RovingProgress;


import ucar.unidata.util.ColorTable;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.Prototypable;
import ucar.unidata.util.PrototypeManager;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;

import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.*;

import ucar.unidata.xml.XmlUtil;


import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.swing.*;



/**
 * Handles getting properties from the user when writing out an isl file
 */

public class IslDialog {

    /** _more_          */
    private JTabbedPane tabs;


    /** Widget for the isl dialog */
    private JCheckBox offscreenCbx;

    /** Widget for the isl dialog */
    private JCheckBox inlineCbx;

    /** Widget for the isl dialog */
    private JCheckBox imageCbx;

    /** Widget for the isl dialog */
    private JCheckBox debugCbx;

    /** Widget for the isl dialog */
    private JTextField imageFld;

    /** _more_          */
    private List datasetFlds = new ArrayList();

    /** Widget for the isl dialog */
    private JTextField sleepFld;

    /** Widget for the isl dialog */
    private JTextField loopFld;

    /** _more_          */
    private JTextField widthFld;

    /** _more_          */
    private JTextField heightFld;

    /** Widget for the isl dialog */
    private JCheckBox movieCbx;

    /** Widget for the isl dialog */
    private JTextField movieFld;

    /** Widget for the isl dialog */
    private JTextField importFld;

    /** Widget for the isl dialog */
    private JComponent islContents;

    /** the manager */
    private IdvPersistenceManager persistenceManager;


    /**
     * ctor
     *
     * @param persistenceManager the manager
     */
    public IslDialog(IdvPersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }


    /**
     * Write out an isl file
     *
     * @param islFile File name
     * @param bundleXml The bundle
     *
     * @throws IOException On badness
     */
    public void writeIsl(String islFile, String bundleXml)
            throws IOException {

        String tail     = IOUtil.getFileTail(islFile);
        String xidvFile = IOUtil.stripExtension(islFile) + ".xidv";
        boolean hasMultipleViews =
            persistenceManager.getIdv().getVMManager().getViewManagers()
                .size() > 1;



        String extra = (hasMultipleViews
                        ? "_${viewindex}"
                        : "");
        String pngFile = "${islpath}/" + IOUtil.stripExtension(tail) + extra
                         + ".png";
        String movFile = "${islpath}/" + IOUtil.stripExtension(tail) + extra
                         + ".mov";

        if (islContents == null) {


            List comps1 = new ArrayList();
            List comps2 = new ArrayList();

            offscreenCbx = new JCheckBox("Offscreen", true);
            debugCbx     = new JCheckBox("Debug", true);
            inlineCbx    = new JCheckBox("Include Bundle Inline", false);

            imageCbx     = new JCheckBox("", true);
            imageFld     = new JTextField(pngFile, 30);
            imageFld.setToolTipText(
                "<html><b>.png,.gif,.jpg</b> for images<br><b>.kml/kmz</b> for Google Earth<br>Use ${viewindex} for each view<br>Use ${viewname} for view name<br>Use ${loopindex} for each loop iteration");

            comps1.add(GuiUtils.rLabel("Generate an Image:"));
            comps1.add(GuiUtils.leftCenterRight(imageCbx, imageFld,
                    GuiUtils.makeFileBrowseButton(imageFld)));
            movieCbx = new JCheckBox("", false);
            movieFld = new JTextField(movFile, 30);
            movieFld.setToolTipText(
                "<html><b>.mov</b> for Quicktime<br><b>.gif</b> for animated GIF<br><b>.kml/kmz</b> for Google Earth<br><b>.html</b> for AniS Applet<br>Use ${viewindex} for each view<br>Use ${viewname} for view name<br>Use ${loopindex} for each loop iteration");
            comps1.add(GuiUtils.rLabel("Generate a Movie:"));
            comps1.add(GuiUtils.leftCenterRight(movieCbx, movieFld,
                    GuiUtils.makeFileBrowseButton(movieFld)));


            comps2.add(GuiUtils.rLabel("Flags:"));
            List lineComps = Misc.newList(offscreenCbx, debugCbx, inlineCbx);
            comps2.add(GuiUtils.left(GuiUtils.hbox(lineComps, 5)));

            loopFld   = new JTextField("1", 5);
            sleepFld  = new JTextField("60", 5);
            widthFld  = new JTextField("", 4);
            heightFld = new JTextField("", 4);

            comps2.add(GuiUtils.rLabel("#Iterations:"));
            comps2.add(GuiUtils.left(GuiUtils.hbox(loopFld,
                    GuiUtils.rLabel("   Sleep Time:  "), sleepFld,
                    new JLabel(" (minutes)"))));

            widthFld.setToolTipText("View width");
            heightFld.setToolTipText("View height");
            comps2.add(GuiUtils.rLabel("Dimensions:"));
            comps2.add(GuiUtils.left(GuiUtils.hbox(widthFld,
                    new JLabel(" X "), heightFld,
                    new JLabel(" Optional View Dimensions"))));

            importFld = new JTextField("", 30);
            comps2.add(GuiUtils.rLabel("Import ISL File:"));
            comps2.add(
                GuiUtils.centerRight(
                    importFld, GuiUtils.makeFileBrowseButton(importFld)));


            GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
            tabs               = new JTabbedPane();
            JComponent tab1 = GuiUtils.doLayout(comps1, 2, GuiUtils.WT_NY,
                                  GuiUtils.WT_N);
            GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
            JComponent tab2 = GuiUtils.doLayout(comps2, 2, GuiUtils.WT_NY,
                                  GuiUtils.WT_N);

            tabs.addTab("Product", GuiUtils.top(tab1));
            tabs.addTab("Advanced", GuiUtils.top(tab2));
            islContents = tabs;
        }
        if ( !GuiUtils.showOkCancelDialog(null, "ISL Properties",
                                          islContents, null)) {
            return;
        }


        IOUtil.writeFile(xidvFile, bundleXml);
        Document doc  = XmlUtil.makeDocument();
        Element  root = doc.createElement(ImageGenerator.TAG_ISL);
        root.setAttribute(ImageGenerator.ATTR_LOOP, loopFld.getText().trim());
        double sleepMinutes =
            new Double(sleepFld.getText().trim()).doubleValue();
        root.setAttribute(ImageGenerator.ATTR_SLEEP,
                          "" + (sleepMinutes) + "minutes");
        root.setAttribute(ImageGenerator.ATTR_OFFSCREEN,
                          "" + offscreenCbx.isSelected());

        root.setAttribute(ImageGenerator.ATTR_DEBUG,
                          "" + debugCbx.isSelected());

        Element bundleNode = doc.createElement(ImageGenerator.TAG_BUNDLE);
        root.appendChild(bundleNode);

        if ((widthFld.getText().trim().length() > 0)
                && (heightFld.getText().trim().length() > 0)) {
            bundleNode.setAttribute(ImageGenerator.ATTR_WIDTH,
                                    widthFld.getText().trim());
            bundleNode.setAttribute(ImageGenerator.ATTR_HEIGHT,
                                    heightFld.getText().trim());
        }

        if (inlineCbx.isSelected()) {
            bundleNode.appendChild(
                doc.createTextNode(
                    XmlUtil.encodeBase64(bundleXml.getBytes())));
        } else {
            bundleNode.setAttribute(ImageGenerator.ATTR_FILE,
                                    "${islpath}/"
                                    + IOUtil.getFileTail(xidvFile));

            bundleNode.setAttribute(ImageGenerator.ATTR_WAIT, "true");

            bundleNode.setAttribute(ImageGenerator.ATTR_CLEAR, "true");
        }

        //        root.appendChild(doc.createElement(ImageGenerator.TAG_PAUSE));

        if (importFld.getText().trim().length() > 0) {
            Element importNode = doc.createElement(ImageGenerator.TAG_IMPORT);
            importNode.setAttribute(ImageGenerator.ATTR_FILE,
                                    importFld.getText().trim());
            root.appendChild(importNode);
        }
        if (imageCbx.isSelected()) {
            Element imageNode = doc.createElement(ImageGenerator.TAG_IMAGE);
            root.appendChild(imageNode);
            imageNode.setAttribute(ImageGenerator.ATTR_FILE,
                                   imageFld.getText().trim());
        }
        if (movieCbx.isSelected()) {
            Element imageNode = doc.createElement(ImageGenerator.TAG_MOVIE);
            root.appendChild(imageNode);
            imageNode.setAttribute(ImageGenerator.ATTR_FILE,
                                   movieFld.getText().trim());
        }
        IOUtil.writeFile(islFile, XmlUtil.toStringWithHeader(root));

    }

}
