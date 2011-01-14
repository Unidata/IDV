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
import ucar.unidata.util.Misc;

import ucar.visad.quantities.CommonUnits;

import visad.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import java.util.HashSet;

import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;


/**
 *
 * @author IDV development team
 */

public class ImageDecorator extends FlythroughDecorator {

    /** _more_ */
    private Image backgroundImage;

    /** _more_ */
    private String imageUrl;

    /** _more_ */
    private ReadoutInfo imageReadout;


    /** _more_ */
    private HashSet fetchedImages = new HashSet();



    /**
     * _more_
     */
    public ImageDecorator() {}

    /**
     * _more_
     *
     * @param flythrough _more_
     */
    public ImageDecorator(Flythrough flythrough) {
        super(flythrough);
    }


    /**
     * _more_
     *
     * @param samples _more_
     *
     * @throws Exception _more_
     */
    public void handleReadout(FlythroughPoint pt, List<ReadoutInfo> samples) throws Exception {
        String newImageUrl = null;
        imageReadout = null;
        for (ReadoutInfo info : samples) {
            if (info.getImageUrl() != null) {
                newImageUrl  = info.getImageUrl();
                imageReadout = info;
            }
        }

        if ( !Misc.equals(newImageUrl, imageUrl)) {
            imageUrl = newImageUrl;
            if (imageUrl != null) {
                if ( !fetchedImages.contains(imageUrl)) {
                    fetchedImages.add(imageUrl);
                    Misc.run(this, "fetchBackgroundImage", imageUrl);
                }
            } else {
                backgroundImage = null;
            }
        }


    }


    /**
     * _more_
     *
     * @param url _more_
     */
    public void fetchBackgroundImage(String url) {
        Image image = GuiUtils.getImage(imageUrl, getClass());
        if (image != null) {
            image.getWidth(flythrough);
            if (Misc.equals(url, imageUrl)) {
                backgroundImage = image;
                flythrough.updateDashboard();
            }
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        return "background images";
    }


    /**
     * _more_
     *
     * @param g2 _more_
     * @param comp _more_
     *
     * @return _more_
     */
    public boolean paintDashboard(Graphics2D g2, JComponent comp) {
        Image image = backgroundImage;
        if (image != null) {
            Rectangle b           = comp.getBounds();
            int       imageHeight = image.getHeight(null);
            int       imageWidth  = image.getWidth(flythrough);
            if (imageHeight > 0) {
                double          scale        = b.width / (double) imageWidth;
                AffineTransform oldTransform = g2.getTransform();
                g2.scale(scale, scale);
                g2.drawImage(image, 0, flythrough.dashboardImageOffset.y,
                             null);
                g2.setTransform(oldTransform);
            } else {
                g2.drawImage(image, 0, 0, null);
            }

            g2.setColor(Color.black);
            if (imageReadout.getImageName() != null) {
                g2.drawString(imageReadout.getImageName(), 10, 20);
            }
        }
        return false;
    }

}

