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

public class WeatherDecorator extends FlythroughDecorator {

    /** _more_ */
    private double precipLevel = 0;

    /** _more_ */
    private double temperature = Double.NaN;

    /** _more_ */
    private Image rainIcon;

    /** _more_ */
    private Image snowIcon;


    /**
     * _more_
     */
    public WeatherDecorator() {}


    /**
     * _more_
     *
     * @param flythrough _more_
     */
    public WeatherDecorator(Flythrough flythrough) {
        super(flythrough);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        return "raindrops and weather";
    }


    /**
     * _more_
     *
     * @param samples _more_
     *
     * @throws Exception _more_
     */
    public void handleReadout(FlythroughPoint pt, List<ReadoutInfo> samples) throws Exception {
        precipLevel = 0;
        temperature = Double.NaN;
        for (ReadoutInfo info : samples) {
            Real r = info.getReal();
            if (r == null) {
                continue;
            }
            Unit unit = info.getUnit();
            if (unit == null) {
                unit = r.getUnit();
            }
            String name = ucar.visad.Util.cleanTypeName(r.getType());
            if (((name.toLowerCase().indexOf("precipitation") >= 0)
                    || (name.toLowerCase().indexOf("rain")
                        >= 0)) && Unit.canConvert(unit, CommonUnits.MILLIMETER)) {
                precipLevel = r.getValue(CommonUnits.MILLIMETER);
            } else if (Unit.canConvert(unit, CommonUnits.CELSIUS)) {
                temperature = r.getValue(CommonUnits.CELSIUS);
            }
        }
    }



    /**
     * _more_
     *
     * @param g _more_
     * @param comp _more_
     *
     * @return _more_
     */
    public boolean paintDashboard(Graphics2D g, JComponent comp) {
        if (precipLevel > 0) {
            Rectangle b = comp.getBounds();
            int cv = 255 - (int) (255 * (Math.min(precipLevel, 100) / 100));
            Color     c = new Color(cv, cv, cv);
            g.setColor(c);
            //            if (backgroundImage == null) {
            //                g.fillRect(0, 0, b.width, b.height);
            //            }
            if (rainIcon == null) {
                snowIcon =
                    GuiUtils.getImage("/auxdata/ui/icons/snowflake.gif",
                                      getClass());
                rainIcon = GuiUtils.getImage("/auxdata/ui/icons/drops.gif",
                                             getClass());
            }
            Image icon = (((temperature == temperature) && (temperature < 0))
                          ? snowIcon
                          : rainIcon);
            for (int i = 0; i < precipLevel * 10; i++) {
                int x = (int) (Math.random() * b.width) - icon.getWidth(null);
                int y = (int) (Math.random() * b.height)
                        - icon.getHeight(null);
                g.drawImage(icon, x, y, null);
            }
            return true;
        }
        return false;
    }

}

