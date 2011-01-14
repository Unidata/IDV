/*
 * $Id: TopLevel.java,v 1.9 2006/05/05 19:19:38 jeffmc Exp $
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



package ucar.unidata.util;



/** common toplevel for applets (JApplet) and applications (JFrames) */

public interface TopLevel {

    /**
     * get the getRootPaneContainer
     * @return _more_
     */
    public javax.swing.RootPaneContainer getRootPaneContainer();

    /** close and exit the progem */
    public void close();

    /** save any persistant data */
    public void save();

    /**
     * return true if this is an Applet
     * @return _more_
     */
    public boolean isApplet();
}

