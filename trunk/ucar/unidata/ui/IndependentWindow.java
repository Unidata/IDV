/*
 * $Id: IndependentWindow.java,v 1.14 2007/07/06 20:45:31 jeffmc Exp $
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
 *  Provides common L&F for managing independent windows.
 *  Will reset L&F.
 *
 *  example: <pre>
 *    infoWindow = new IndependentWindow("Dataset Information");
 *    datasetInfoTA = new TextHistoryPane(500, 100, true);
 *    Container cp = infoWindow.getContentPane();
 *    cp.add(datasetInfoTA, BorderLayout.CENTER);
 *    infoWindow.pack();
 *    infoWindow.setSize(700,700);
 *    infoWindow.setLocation(100,100);
 *   </pre>
 *
 * @author John Caron
 * @version $Id: IndependentWindow.java,v 1.14 2007/07/06 20:45:31 jeffmc Exp $
 */
public class IndependentWindow extends JFrame {

    /**
     * constructor
     *  @param title       Window title
     */
    public IndependentWindow(String title) {
        super(title);

        // L&F may change

        /**
         * UIManager.addPropertyChangeListener( new PropertyChangeListener() {
         * public void propertyChange( PropertyChangeEvent e) {
         *   if (e.getPropertyName().equals("lookAndFeel"))
         *     SwingUtilities.updateComponentTreeUI( IndependentWindow.this);
         * }
         * });
         */
    }

    /**
     * _more_
     *
     * @param title
     * @param comp
     *
     */
    public IndependentWindow(String title, Component comp) {
        this(title);
        Container cp = getContentPane();
        cp.add(comp, BorderLayout.CENTER);
        pack();
    }


    /**
     * _more_
     */
    public void show() {
        setState(Frame.NORMAL);  // deiconify if needed
        super.show();
    }

    /**
     * _more_
     */
    public void showWaitCursor() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    /**
     * _more_
     */
    public void showNormalCursor() {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

}

/*
 *  Change History:
 *  $Log: IndependentWindow.java,v $
 *  Revision 1.14  2007/07/06 20:45:31  jeffmc
 *  A big J&J
 *
 *  Revision 1.13  2006/04/07 16:43:30  jeffmc
 *  jindent
 *
 *  Revision 1.12  2005/09/12 03:08:55  jeffmc
 *  Some cleanup, etc.
 *
 *  Revision 1.11  2005/05/13 18:31:47  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.10  2005/02/18 14:36:50  jeffmc
 *  Add 'One instance of the IDV' capability
 *
 *  Revision 1.9  2004/09/07 18:36:23  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.8  2004/08/23 17:27:48  dmurray
 *  silence some javadoc warnings now that we are at 1.4
 *
 *  Revision 1.7  2004/02/27 19:12:41  jeffmc
 *  Snapshot before I screw things up too bad
 *
 *  Revision 1.6  2004/01/29 17:37:10  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.5  2002/08/26 22:27:24  jeffmc
 *  Remove the addPropertyChange listener for L&F changes. THis was causing memory leaks
 *
 *  Revision 1.4  2001/11/29 20:12:45  jeffmc
 *  Added setWaitCursor, setNormalCursor methods.
 *
 *  Revision 1.3  2000/08/18 04:15:54  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.2  2000/05/26 21:19:32  caron
 *  new GDV release
 *
 *  Revision 1.1  1999/12/16 22:58:12  caron
 *  gridded data viewer checkin
 *
 */






