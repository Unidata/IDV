/*
 * $Id: PropertyDialog.java,v 1.14 2007/07/06 20:45:32 jeffmc Exp $
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



import ucar.unidata.util.PersistentStore;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import javax.swing.*;


/**
 *  An independent dialog window for managing Properties files
 *
 *  example of use:
 *    infoWindow = new IndependentDialog(topLevel.getRootPaneContainer(), false, "Dataset Information");
 *    datasetInfoTA = new TextHistoryPane(500, 100, true);
 *    Container cp = infoWindow.getContentPane();
 *    cp.add(datasetInfoTA, BorderLayout.CENTER);
 *    infoWindow.pack();
 *    infoWindow.setSize(700,700);
 *    infoWindow.setLocation(100,100);
 *
 * @author John Caron
 * @version $Id: PropertyDialog.java,v 1.14 2007/07/06 20:45:32 jeffmc Exp $
 * @deprecated
 */
public class PropertyDialog extends IndependentDialog {

    /** _more_ */
    private String propertiesName;

    /** _more_ */
    private PersistentStore store;

    /** _more_ */
    private Properties prop;

    /** _more_ */
    private InputFieldPanel panel;

    /** _more_ */
    private InputFieldPanel.TextField tf;

    /** _more_ */
    private InputFieldPanel.BooleanField useDiscrete;

    /** _more_ */
    private InputFieldPanel.DoubleField numPixels;

    /** _more_ */
    private InputFieldPanel.DoubleField windLength;

    /**
     * constructor
     *  @param parent      JFrame (application) or JApplet (applet)
     *  @param modal       is modal
     *  @param title       Window title
     *  @param store       persistent stor
     *  @param propertiesName  name of property
     */
    public PropertyDialog(RootPaneContainer parent, boolean modal,
                          String title, PersistentStore store,
                          String propertiesName) {
        super(parent, false, title);

        this.propertiesName = propertiesName;
        this.store          = store;

        /*prop = (Properties) store.get( propertiesName);
        if (prop == null) {                   // LOOK KLUDGE
          prop = new Properties();
          prop.setProperty("javahelp.toplevel.directory", "/help/GDV/");
        }

        Enumeration enum = prop.propertyNames();
        while (enum.hasMoreElements()) {
          String key = (String) enum.nextElement();
          String value = prop.getProperty(key);
          panel.addTextField( key, value);
        } */

        String name = (String) store.get(propertiesName);
        if (name == null) {
            name = "/help/GDV";
        }
        panel = new InputFieldPanel();
        windLength =
            panel.addDoubleField("length of wind vectors (m/s per deg)",
                                 10.0);
        useDiscrete = panel.addBooleanField("use map Discretization", false);
        numPixels   = panel.addDoubleField("discretization Pixels", 2.0);
        tf          = panel.addTextField("javahelp.toplevel.directory", name);

        // button panel
        JPanel  buttPanel    = new JPanel();
        JButton acceptButton = new JButton("Apply");
        JButton cancelButton = new JButton("Dismiss");
        JButton helpButton   = new JButton("Help");
        buttPanel.add(acceptButton, null);
        buttPanel.add(cancelButton, null);
        buttPanel.add(helpButton, null);

        // button listeners
        acceptButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ucar.unidata.ui.Help.setTopDir(tf.getText());
                //ucar.unidata.view.grid.WindRenderer.setWindLength(windLength.getDoubleValue());
                ucar.unidata.gis.GisFeatureRendererMulti.setDiscretization(
                    useDiscrete.getBoolean());
                ucar.unidata.gis.GisFeatureRendererMulti.setPixelMatch(
                    numPixels.getDoubleValue());
                PropertyDialog.this.setVisible(false);
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                PropertyDialog.this.setVisible(false);
            }
        });
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ucar.unidata.ui.Help.getDefaultHelp().gotoTarget(
                    "system.config");
            }
        });

        Container cp = getContentPane();
        cp.add(panel, BorderLayout.CENTER);
        cp.add(buttPanel, BorderLayout.SOUTH);
    }

    /**
     * _more_
     */
    public void storePersistentData() {
        store.put(propertiesName, tf.getText());
    }

}

/*
 *  Change History:
 *  $Log: PropertyDialog.java,v $
 *  Revision 1.14  2007/07/06 20:45:32  jeffmc
 *  A big J&J
 *
 *  Revision 1.13  2005/05/13 18:31:50  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.12  2004/09/07 18:36:25  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.11  2004/08/23 17:27:48  dmurray
 *  silence some javadoc warnings now that we are at 1.4
 *
 *  Revision 1.10  2004/01/29 17:37:12  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.9  2001/05/10 18:53:12  caron
 *  improve SuperComboBox; mv to ui.table
 *
 *  Revision 1.8  2000/10/05 20:17:43  caron
 *  GDV release
 *
 *  Revision 1.7  2000/08/18 04:15:57  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.6  2000/05/26 21:19:33  caron
 *  new GDV release
 *
 *  Revision 1.5  2000/05/16 23:10:07  caron
 *  mostly /r cleanup
 *
 *  Revision 1.4  2000/04/26 22:12:39  caron
 *  fix minor bugs for clean compile
 *
 *  Revision 1.3  2000/04/26 21:14:02  caron
 *  latest version of GDV
 *
 *  Revision 1.2  2000/02/17 20:28:36  caron
 *  add map resolution config
 *
 *  Revision 1.1  2000/02/07 18:09:01  caron
 *  checkin
 *
 *  Revision 1.1  1999/12/16 22:58:11  caron
 *  gridded data viewer checkin
 *
 */






