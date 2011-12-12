/*
 * $Id: PLAF.java,v 1.9 2007/07/06 20:45:32 jeffmc Exp $
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

import javax.swing.*;


/**
 *  Pluggable Look and Feel management.
 * @author John Caron
 * @version $Id: PLAF.java,v 1.9 2007/07/06 20:45:32 jeffmc Exp $
 */

public class PLAF {

    /** _more_ */
    private static UIManager.LookAndFeelInfo[] plafInfo =
        UIManager.getInstalledLookAndFeels();

    /** _more_ */
    private JComponent tree;

    /** _more_ */
    private boolean debug = false;

    /* Constructor.
      * @param tree the top-level JComponent tree: everything in this tree will get switched to
      *   the new L&F. Everything not in the tree (eg Dialogs) should listen for changes like:
      *<pre>
      UIManager.addPropertyChangeListener( new PropertyChangeListener() {
        public void propertyChange( PropertyChangeEvent e) {
          if (e.getPropertyName().equals("lookAndFeel"))
            SwingUtilities.updateComponentTreeUI( <myDialogObject>);
        }
      });
      </pre>
      */

    /**
     * _more_
     *
     * @param tree
     *
     */
    public PLAF(JComponent tree) {
        this.tree = tree;
    }

    /**
     * Add a set of MenuItems to the given JMenu, one for each possible L&F.
     *  if this platform doesnt support the L&F, disable the MenuItem.
     *
     * @param menu
     */
    public void addToMenu(JMenu menu) {
        if (debug) {
            System.out.println("PLAF LookAndFeelInfo  ");
        }
        for (int i = 0; i < plafInfo.length; i++) {
            if (debug) {
                System.out.println("   " + plafInfo[i]);
            }
            boolean isSupported = true;
            try {
                String      className = plafInfo[i].getClassName();
                Class       cl        = Class.forName(className);
                LookAndFeel lf        = (LookAndFeel) cl.newInstance();
                if ( !lf.isSupportedLookAndFeel()) {
                    isSupported = false;
                }
            } catch (Throwable t) {
                isSupported = false;
            }

            AbstractAction act = new PLAFAction(plafInfo[i].getName(),
                                     plafInfo[i].getClassName());
            JMenuItem mi = menu.add(act);
            if ( !isSupported) {
                mi.setEnabled(false);
            }
        }
    }


    /**
     * Class PLAFAction
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    private class PLAFAction extends AbstractAction {

        /** _more_ */
        String plafClassName;

        /**
         * _more_
         *
         * @param name
         * @param plafClassName
         *
         */
        PLAFAction(String name, String plafClassName) {
            this.plafClassName = plafClassName;
            putValue(Action.NAME, name);
        }

        /**
         * _more_
         *
         * @param evt
         */
        public void actionPerformed(ActionEvent evt) {
            try {
                UIManager.setLookAndFeel(plafClassName);
            } catch (Exception ex) {
                System.out.println(ex);
                return;
            }

            //this sets L&F for top level and its children only
            // Dialog boxes must listen fo L&F PropertyChangeEvents
            SwingUtilities.updateComponentTreeUI(tree);
        }
    }
}

/*
 *  Change History:
 *  $Log: PLAF.java,v $
 *  Revision 1.9  2007/07/06 20:45:32  jeffmc
 *  A big J&J
 *
 *  Revision 1.8  2005/05/13 18:31:49  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.7  2004/09/07 18:36:24  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.6  2004/02/27 21:19:19  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.5  2004/01/29 17:37:12  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.4  2000/08/18 04:15:55  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.3  1999/06/03 01:44:12  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:27:07  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:47  caron
 *  startAgain
 *
 * # Revision 1.6  1999/03/26  19:58:29  caron
 * # add SpatialSet; update javadocs
 * #
 * # Revision 1.5  1999/03/18  18:21:23  caron
 * # bug fixes
 * #
 * # Revision 1.4  1999/03/16  17:00:46  caron
 * # fix StationModel editing; add TopLevel
 * #
 * # Revision 1.3  1999/03/08  19:45:37  caron
 * # world coord now Point2D
 * #
 */






