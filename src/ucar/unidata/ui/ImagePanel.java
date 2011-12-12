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
public class ImagePanel extends JPanel implements ImageObserver, MouseListener,MouseMotionListener, KeyListener { 

    /** The file index we are currently looking at */
    private int currentIndex = 0;

    private List files= new ArrayList();

    /** The one we are drawing */
    private Image currentImage;

    /** The one we are drawing */
    private Image loadingImage;

    private double scaleFactor = 1.0;

    private int translateX = 0;
    private int translateY = 0;

    private int mouseX;
    private int mouseY;


    /**
     * NOOP ctor
     */
    public ImagePanel() {
        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent event) {
                int notches = event.getWheelRotation();
                if (notches < 0) {
                    doZoomIn();
                } else {
                    doZoomOut();
                }
            }
        });
    }


    public void keyPressed(KeyEvent e) {
        if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_R) {
            scaleFactor = 1;
            translateX = 0;
            translateY = 0;
            currentTransX = 0;
            currentTransY = 0;
            repaint();
        } else  if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_UP) {
            doZoomIn();
        } else  if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_DOWN) {
            doZoomOut();
        } else   if(e.getKeyCode() == KeyEvent.VK_DOWN) {
            translateY+=20;
            repaint();
        } else   if(e.getKeyCode() == KeyEvent.VK_UP) {
            translateY-=20;
            repaint();
        } else   if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
            translateX+=20;
            repaint();
        } else   if(e.getKeyCode() == KeyEvent.VK_LEFT) {
            translateX-=20;
            repaint();
        }
    }


    /**
     * Noop
     *
     * @param e The event
     */
    public void keyReleased(KeyEvent e) {}

    /**
     * Noop
     *
     * @param e The event
     */
    public void keyTyped(KeyEvent e) {}

    /**
     * Noop
     *
     * @param e The event
     */
    public void mouseMoved(MouseEvent e) {}

    public void mouseClicked(MouseEvent e) {}

    /**
     * Noop
     *
     * @param e The event
     */
    public void mouseEntered(MouseEvent e) {
        requestFocus();
    }

    /**
     * Noop
     *
     * @param e The event
     */
    public void mouseExited(MouseEvent e) {}


    int baseTransX=0;
    int baseTransY=0;
    int currentTransX =0;
    int currentTransY =0;

    /**
     * Mouse was pressed
     *
     * @param event The event
     */
    public void mousePressed(MouseEvent event) {
        requestFocus();
        mouseX = event.getX();        
        mouseY = event.getY();
        currentTransX = 0;
        currentTransY = 0;
    }

    public void mouseReleased(MouseEvent event) {
        translateX+=currentTransX;
        translateY+=currentTransY;
        currentTransX = 0;
        currentTransY = 0;
    }

    public void mouseDragged(MouseEvent event) {
        currentTransX = (int)((event.getX()-mouseX)/scaleFactor);
        currentTransY = (int)((event.getY()-mouseY)/scaleFactor);
        repaint();
    }


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
            loadFile(theFile);
        }
    }


    /**
     * Load the file into the preview. This uses the local loadingImage image object and listens
     * for changes on it. When its ready to load we then load it into the panel
     *
     * @param theFile The image file (or url) to load
     */
    public void loadFile(String theFile) {
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
        if (all && (img == loadingImage)) {
            loadingImageDone(loadingImage);
            return true;
        }
        return true;
    }


    public void loadingImageDone(Image image) {
        setImage(image);
    }


    protected void doZoomIn() {
        scaleFactor = scaleFactor + 0.1;
        repaint();
    }

    /**
     * zoom out
     */
    protected void doZoomOut() {
        scaleFactor = scaleFactor - 0.1;
        if (scaleFactor < 0.1) {
            scaleFactor = 0.1;
        }
        repaint();
    }


    /**
     * Draw the image
     *
     * @param g graphics_
     * @param imagePanel Where do we paint into
     * @param currentImage The image to paint
     */
    private void paintImage(Graphics2D g) {
        
        g.scale(scaleFactor, scaleFactor);
        g.translate(translateX+currentTransX, translateY+currentTransY);
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
        paintImage((Graphics2D)g);
    }

    public void setImage(Image image) {
        this.currentImage = image;
        repaint();
    }

    public Image getImage() {
        return this.currentImage;
    }


}

