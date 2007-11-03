/*
 * $Id: ImageMovieControl.java,v 1.71 2007/08/09 17:22:25 dmurray Exp $
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

import java.awt.Color;
import java.awt.event.*;
import java.awt.image.*;
import java.beans.*;
import java.io.File;
import java.net.URL;

import ucar.unidata.util.LogUtil;
import java.util.ArrayList;
import java.util.List;




import javax.swing.*;




/**
 * Class for controlling the display of color images.
 * @author Jeff McWhirter
 * @version $Revision: 1.71 $
 */
public class ImagePanel extends JPanel implements ImageObserver { 

    /** The file index we are currently looking at */
    private int currentIndex = 0;

    private List files= new ArrayList();

    /** The one we are drawing */
    private Image currentImage;

    /** The one we are drawing */
    private Image loadingImage;


    /**
     * NOOP ctor
     */
    public ImagePanel() {}


    /**
     * Set the selected file. Will change index if it is invalid
     *
     * @param theIndex Index of the file.
     */
    public void setSelectedFile(int theIndex) {
        if (theIndex < 0) {
            theIndex = 0;
        } else if (theIndex >= files.size()) {
            theIndex = files.size() - 1;
        }
        currentIndex = theIndex;

        String theFile = null;
        if ((theIndex >= 0) && (theIndex < files.size())) {
            theFile = (String) files.get(theIndex);
        }

        if (theFile == null) {
            loadingImage = null;
            setImage(null);
        } else {
            long t1 = System.currentTimeMillis();
            try {
                //fileList.setSelectedIndex(theIndex);
                loadingImage = ImageUtils.getImageFile(theFile);
                loadingImage.getWidth(this);
            } catch (Exception exc) {
                LogUtil.logException("Error loading image", exc);
            }
            long t2 = System.currentTimeMillis();
            //            System.err.println ("time:" + (t2-t1));
        }

    }


    public void setFiles(List list) {
        files = new ArrayList(list);
    }

    public List getFiles() {
        return files;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }


    /**
     * Handle the image update
     *
     * @param img img
    * @param flags flags
     * @param x x
     * @param y y
     * @param width width
     * @param height height
     *
     * @return keep going
     */
    public boolean imageUpdate(Image img, int flags, int x, int y, int width,
                               int height) {
        if (img == currentImage) {
            repaint();
            return true;
        }
        boolean all  = (flags & ImageObserver.ALLBITS) != 0;
        boolean some = (flags & ImageObserver.SOMEBITS) != 0;
        if (all && (img == loadingImage)) {
            loadingImageDone(loadingImage);
            return true;
        }
        return true;
    }


    public void loadingImageDone(Image loadingImage) {
        setImage(loadingImage);
    }


    /**
     * Draw the image
     *
     * @param g graphics_
     * @param imagePanel Where do we paint into
     * @param currentImage The image to paint
     */
    private void paintImage(Graphics g) {
        if (currentImage == null) {
            return;
        }
        Rectangle bounds = getBounds();
        if ((bounds.width == 0) || (bounds.height == 0)) {
            return;
        }
        int imageWidth = currentImage.getWidth(this);
        if (imageWidth <= 0) {
            return;
        }
        int imageHeight = currentImage.getHeight(this);
        if (imageHeight <= 0) {
            return;
        }
        if ((imageWidth < bounds.width) && (imageHeight < bounds.height)) {
            g.drawImage(currentImage, 0, 0, null);
            return;
        }


        if (imageWidth / (double) bounds.width
                > imageHeight / (double) bounds.height) {
            imageHeight = (int) (imageHeight
                                 * (bounds.width / (double) imageWidth));
            imageWidth = bounds.width;
        } else {
            imageWidth = (int) (imageWidth
                                * (bounds.height / (double) imageHeight));

            imageHeight = bounds.height;
        }
        g.drawImage(currentImage, 0, 0, imageWidth, imageHeight, null);
    }

    public void paint(Graphics g) {
        super.paint(g);
        paintImage(g);
    }

    public void setImage(Image image) {
        this.currentImage = image;
        repaint();
    }

    public Image getImage() {
        return this.currentImage;
    }


}

