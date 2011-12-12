/**
 * $Id: ViewManager.java,v 1.401 2007/08/16 14:05:04 jeffmc Exp $
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
 * This library is distributed in the hope that it will be2 useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */






package ucar.unidata.idv.flythrough;


import ucar.unidata.idv.control.ReadoutInfo;
import ucar.unidata.util.GuiUtils;

import ucar.visad.quantities.CommonUnits;

import visad.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;


/**
 *
 * @author IDV development team
 */

public abstract class FlythroughDecorator {

    /** _more_ */
    Flythrough flythrough;

    /** _more_          */
    private boolean shown = true;




    /**
     * _more_
     */
    public FlythroughDecorator() {}

    /**
     * _more_
     *
     * @param flythrough _more_
     */
    public FlythroughDecorator(Flythrough flythrough) {
        this.flythrough = flythrough;
    }


    /**
     * _more_
     *
     * @param flythrough _more_
     */
    public void setFlythrough(Flythrough flythrough) {
        this.flythrough = flythrough;
    }

    /**
     * _more_
     *
     * @param samples _more_
     *
     * @throws Exception _more_
     */
    public void handleReadout(FlythroughPoint pt, List<ReadoutInfo> samples) throws Exception {}

    /**
     * _more_
     *
     * @param g _more_
     * @param comp _more_
     *
     * @return _more_
     */
    public boolean paintDashboard(Graphics2D g, JComponent comp) {
        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract String getName();

    /**
     * _more_
     */
    public void clearSamples() {}

    /**
     * _more_
     *
     * @param viewMenu _more_
     */
    public void initViewMenu(JMenu viewMenu) {
        viewMenu.add(GuiUtils.makeCheckboxMenuItem("Show " + getName(), this, "shown",
                                                   null));
    }


    /**
     * _more_
     *
     * @param viewMenu _more_
     */
    public void initEditMenu(JMenu viewMenu) {}

    /**
     * _more_
     *
     * @param viewMenu _more_
     */
    public void initFileMenu(JMenu viewMenu) {}


    /**
     *  Set the Shown property.
     *
     *  @param value The new value for Shown
     */
    public void setShown(boolean value) {
        shown = value;
        if (flythrough != null) {
            flythrough.updateDashboard();
        }
    }

    /**
     *  Get the Shown property.
     *
     *  @return The Shown
     */
    public boolean getShown() {
        return shown;
    }


    /**
     * _more_
     *
     * @param msg _more_
     * @param exc _more_
     */
    public void logException(String msg, Throwable exc) {
        flythrough.logException(msg, exc);
    }



}

