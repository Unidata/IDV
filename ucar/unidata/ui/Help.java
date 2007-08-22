/*
 * $Id: Help.java,v 1.21 2007/07/20 22:20:09 dmurray Exp $
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



import java.util.List;

import javax.help.*;

import javax.swing.*;


/**
 * Convenience routines for accessing JavaHelp.
 * These are static routines, so that they can be accessed from deeeply nested
 * methods without having to pass the HekpSet object all over the place. The
 * assumption is that there is only one helpset per application.
 *
 * The application should call setTopDir() to set the root directory, before any
 * other calls are made into the help system. The helpset is then found at:
 * <pre> /<topDir>/HelpSet.hs </pre>
 *
 * Inside the application, Help is accessed by
 *   ucar.unidata.ui.Help.getDefaultHelp().gotoTarget("top");
 * The actual target names must be valid names in the Map.xml file.
 * I use "top" as the convention for the general Help page.
 *
 * @author caron
 * @version $Revision: 1.21 $ $Date: 2007/07/20 22:20:09 $
 */

public class Help {

    /** _more_ */
    static private String defaultTopDir = null;  // bogus, get rid of this

    /** _more_ */
    static private Help defaultHelp = null;

    /** _more_ */
    static private boolean debug = false;

    /**
     * _more_
     * @return _more_
     */
    static public Help getDefaultHelp() {
        if (defaultHelp == null) {
            defaultHelp = new Help();
        }
        return defaultHelp;
    }

    /**
     * _more_
     *
     * @param topDir
     */
    static public void setTopDir(String topDir) {
        if ( !topDir.equals(defaultTopDir)) {
            defaultTopDir = topDir;
            defaultHelp   = null;
            if (debug) {
                System.out.println("Help topDir changed to =" + topDir);
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /** _more_ */
    private String helpsetName;

    /** _more_ */
    private javax.help.HelpSet helpSet = null;

    /** _more_ */
    private javax.help.HelpBroker helpBroker = null;

    /** _more_          */
    private boolean makeBroker = true;

    /** _more_          */
    JHelp jHelp;


    /**
     * _more_
     *
     */
    public Help() {
        this(defaultTopDir + "/HelpSet.hs", true);
    }


    /**
     * _more_
     *
     * @param helpSetName _more_
     * @param makeBroker _more_
     */
    public Help(String helpSetName, boolean makeBroker) {
        this.helpsetName = helpSetName;
        this.makeBroker  = makeBroker;
    }


    /**
     * _more_
     */
    private void makeHelp() {
        java.net.URL url = null;
        Class        cl  = this.getClass();
        url = HelpSet.findHelpSet(cl.getClassLoader(), helpsetName);  // straight file lookup first
        if (null == url) {
            try {
                url = cl.getResource(helpsetName);  // now look in the jar file
            } catch (Exception ee) {
                System.out.println("HelpSet exception" + helpsetName
                                   + " not found");
                return;
            }
        }
        if (url == null) {
            System.out.println("HelpSet URL " + helpsetName + " not found");
            return;
        }
        if (debug) {
            System.out.println("URL ok " + url);
        }

        try {
            //System.err.println("url: " + url);
            helpSet = new HelpSet(cl.getClassLoader(), url);
        } catch (HelpSetException ee) {
            System.out.println("API Help Set not found");
            return;
        }

        if (makeBroker) {
            helpBroker = helpSet.createHelpBroker();
        } else {}
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JHelp getJHelp() {
        if (jHelp == null) {
            jHelp = new JHelp(helpSet);
        }
        return jHelp;
    }


    /**
     * Go to the first target in the array that is valid.
     * If none are valid then return false
     *
     * @param ids Array of   target help ids
     *
     * @return _more_
     */
    public boolean gotoTarget(List ids) {
        for (int i = 0; i < ids.size(); i++) {
            String id = (String) ids.get(i);
            if (id == null) {
                continue;
            }
            if (isValidID(id)) {
                gotoTarget(id, true);
                return true;
            }
        }
        return false;
    }


    /**
     * Go to the specified target in the <code>HelpSet</code>.  If
     * the display window for the help is not visible, show it and
     * bring it to the front.
     * @param id  target help id
     */
    public void gotoTarget(String id) {
        gotoTarget(id, true);
    }

    /**
     * Is the given help id valid
     *
     * @param id The help id to check for validity
     * @return Is the id valid, i.e., is it defined in the helpset
     */
    public boolean isValidID(String id) {
        if (null == helpSet) {  // lazy instantiation
            makeHelp();
        }
        if (null == helpSet) {
            return false;
        }
        return helpSet.getCombinedMap().isValidID(id, helpSet);
    }


    /**
     * Go to the specified target in the <code>HelpSet</code>.
     * @param id  target help id
     * @param setVisible  if true, create the help display window (if needed)
     *                    the display window for the help is not visible,
     *                    show it and bring it to the front.
     *                    and bring it to the front.
     */
    public void gotoTarget(String id, boolean setVisible) {

        if (null == helpSet) {  // lazy instantiation
            makeHelp();
        }
        if (null == helpSet) {
            return;
        }

        try {
            if (helpBroker != null) {
                helpBroker.setCurrentID(id);  // go to this topic
            }
            if (jHelp != null) {
                jHelp.setCurrentID(id);       // go to this topic
            }
            if (debug) {
                System.out.println("Help Id set " + id);
            }
        } catch (BadIDException e) {
            System.out.println("Bad Help ID = " + id);
        }

        if (setVisible && (helpBroker != null)) {
            try {
                helpBroker.setDisplayed(true);
            } catch (javax.help.UnsupportedOperationException e) {
                System.out.println("UnsupportedOperationException = " + e);
            }
        }
    }

}

