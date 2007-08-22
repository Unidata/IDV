/*
 * $Id: ImageGlyph.java,v 1.9 2005/05/13 18:32:10 jeffmc Exp $
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






package ucar.unidata.ui.drawing;



import ucar.unidata.util.GuiUtils;



import java.awt.*;
import java.awt.image.ImageObserver;

import java.net.URL;


/**
 * Class ImageGlyph
 *
 *
 * @author IDV development team
 */
public class ImageGlyph extends RectangleGlyph implements ImageObserver {

    /** _more_ */
    String url = "UNKNOWN";

    /** _more_ */
    Image image = null;

    /** _more_ */
    DisplayCanvas canvas;

    /**
     * _more_
     *
     * @param x
     * @param y
     * @param url
     *
     */
    public ImageGlyph(int x, int y, String url) {
        super("IMAGE", x, y, 0, 0);
        setFilled(true);
        this.url = url;
    }

    /**
     * _more_
     *
     * @param x
     * @param y
     *
     */
    public ImageGlyph(int x, int y) {
        this(x, y, (String) null);
    }

    /**
     * _more_
     */
    public void initDone() {
        super.initDone();
        if ((image == null) && (url != null)) {
            setImage(url);
        }
    }


    /**
     * _more_
     *
     * @param url
     */
    public void setImage(String url) {
        this.url = url;
        if (url == null) {
            return;
        }
        try {
            this.image = GuiUtils.getImage(url, getClass());
            if (image != null) {
                setImage(image);
            } else {
                System.err.println("ImageGlyph.setImage " + url + "\n"
                                   + "Null image");
            }
        } catch (Exception exc) {
            System.err.println("ImageGlyph.setImage " + url + "\n" + exc);
        }
    }


    /**
     * _more_
     *
     * @param image
     */
    public void setImage(Image image) {
        this.image = image;
        if (image != null) {
            bounds.width  = image.getWidth(this);
            bounds.height = image.getHeight(this);
        }
    }

    /**
     * _more_
     *
     * @param img
     * @param flags
     * @param x
     * @param y
     * @param width
     * @param height
     * @return _more_
     */
    public boolean imageUpdate(Image img, int flags, int x, int y, int width,
                               int height) {

        if (image == null) {
            return false;
        }
        if ((flags & ImageObserver.ERROR) != 0) {
            image = null;
            System.err.println("An error occured loading the image:" + url);
            return false;
        }

        if (bounds.width < 0) {
            bounds.width = width;
        }
        if (bounds.height < 0) {
            bounds.height = height;
        }

        if ((bounds.width > 0) && (bounds.height > 0)) {
            if (canvas != null) {
                canvas.repaint(bounds);
            }
        }
        return true;
    }


    /**
     * _more_
     *
     * @param g
     * @param c
     */
    public void paint(Graphics g, DisplayCanvas c) {
        canvas = c;
        if (image == null) {
            return;
        }

        if (bounds.width < 0) {
            bounds.width = image.getWidth(this);
        }
        if (bounds.height < 0) {
            bounds.height = image.getHeight(this);
        }

        if ((bounds.width < 0) || (bounds.height < 0)) {
            return;
        }

        Rectangle r = transformOutput(c, bounds);
        g.drawImage(image, r.x, r.y, r.width, r.height, null, this);
        if (underline) {
            if (getForeground() != null) {
                g.setColor(getForeground());
            }
            g.drawRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
        }
    }

    /**
     * _more_
     *
     * @param x
     * @param y
     * @return _more_
     */
    public String getStretchPoint(int x, int y) {
        return PT_CENTER;
    }


    /**
     * _more_
     * @return _more_
     */
    public String getAttrs() {
        return super.getAttrs() + makeAttr(ATTR_IMAGE, url);
    }


    /**
     * _more_
     *
     * @param name
     * @param value
     */
    public void setAttr(String name, String value) {
        if (ATTR_IMAGE.equals(name)) {
            setImage(value);
        } else {
            super.setAttr(name, value);
        }
    }

}





