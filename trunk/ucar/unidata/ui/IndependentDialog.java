/*
 * $Id: IndependentDialog.java,v 1.9 2007/07/06 20:45:31 jeffmc Exp $
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



import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


/**
 *  Provides common L&F for managing independent dialogs
 *  Takes RootPaneContainer as parent, to work with both applet and app
 *  Will reset L&F
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
 * @version $Id: IndependentDialog.java,v 1.9 2007/07/06 20:45:31 jeffmc Exp $
 */
public class IndependentDialog extends JDialog {

    /**
     * Constructor
     *
     * @param parent    JFrame (application) or JApplet (applet)
     * @param modal     is modal
     * @param title     Window title
     */
    public IndependentDialog(RootPaneContainer parent, boolean modal,
                             String title) {
        // having a parent JFrame is better. But what to do about applets?
        super((parent != null) && (parent instanceof JFrame)
              ? (JFrame) parent
              : null);
        setModal(modal);
        if (title != null) {
            setTitle(title);
        }
        init();
    }


    /**
     * Constructor
     *
     * @param parent    JFrame (application) or JApplet (applet)
     * @param modal     is modal
     * @param title     Window title
     */
    public IndependentDialog(JFrame parent, boolean modal, String title) {
        // having a parent JFrame is better. But what to do about applets?
        super(parent);
        setModal(modal);
        if (title != null) {
            setTitle(title);
        }
        init();
    }



    /**
     * Constructor
     *
     * @param parent    JFrame (application) or JApplet (applet)
     * @param modal     is modal
     * @param title     Window title
     */
    public IndependentDialog(JDialog parent, boolean modal, String title) {
        // having a parent JFrame is better. But what to do about applets?
        super(parent);
        setModal(modal);
        if (title != null) {
            setTitle(title);
        }
        init();
    }


    /**
     * _more_
     */
    private void init() {
        // L&F may change
        UIManager.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals("lookAndFeel")) {
                    SwingUtilities.updateComponentTreeUI(
                        IndependentDialog.this);
                }
            }
        });
    }


    /**
     * _more_
     *
     * @param parent
     * @param modal
     * @param title
     * @param comp
     *
     */
    public IndependentDialog(RootPaneContainer parent, boolean modal,
                             String title, Component comp) {
        this(parent, modal, title);

        Container cp = getContentPane();
        cp.add(comp, BorderLayout.CENTER);
        pack();
    }






}

/*
 *  Change History:
 *  $Log: IndependentDialog.java,v $
 *  Revision 1.9  2007/07/06 20:45:31  jeffmc
 *  A big J&J
 *
 *  Revision 1.8  2005/11/17 20:59:02  jeffmc
 *  Better handling of parent dialogs/windows
 *
 *  Revision 1.7  2005/05/13 18:31:47  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.6  2004/09/07 18:36:23  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.5  2004/08/23 17:27:48  dmurray
 *  silence some javadoc warnings now that we are at 1.4
 *
 *  Revision 1.4  2004/01/29 17:37:10  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.3  2000/08/18 04:15:54  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.2  2000/02/07 17:57:40  caron
 *  sorted JTable, better ComboBox
 *
 *  Revision 1.1  1999/12/16 22:58:11  caron
 *  gridded data viewer checkin
 *
 */






