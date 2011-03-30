/**
 * $Id: ImageUtils.java,v 1.29 2007/08/13 18:34:39 jeffmc Exp $
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


import ucar.unidata.ui.drawing.Glyph;


import ucar.unidata.util.ColorTable;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WrapperException;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import java.io.*;

import java.io.InputStream;

import java.lang.reflect.*;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

import java.util.Iterator;
import java.util.List;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Provides a set of image manipulation utilities
 *
 *
 * @author IDV development team
 */
public class ImageUtils {


    /**
     * Add a matte border around the image
     *
     * @param image The image
     * @param  top top space
     * @param  bottom bottom space
     * @param  left left space
     * @param  right right space
     * @param bg Background color
     *
     * @return The matted image
     */
    public static BufferedImage matte(BufferedImage image, int top,
                                      int bottom, int left, int right,
                                      Color bg) {
        int imageWidth  = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        BufferedImage newImage = new BufferedImage(imageWidth + left + right,
                                     imageHeight + top + bottom,
                                     getImageType(image));
        Graphics newG = newImage.getGraphics();
        newG.setColor(bg);
        newG.fillRect(0, 0, newImage.getWidth(null),
                      newImage.getHeight(null));
        newG.drawImage(image, left, top, null);
        return newImage;
    }

    /**
     * Get the image type
     *
     * @param image  the image to check
     *
     * @return  the type (ARGB or RGB)
     */
    private static int getImageType(Image image) {
        if (hasAlpha(image)) {
            return BufferedImage.TYPE_INT_ARGB;
        } else {
            return BufferedImage.TYPE_INT_RGB;
        }
    }

    /**
     * Clip  the image
     *
     * @param image The image
     * @param ul upper left
     * @param lr lower right
     *
     * @return The clipped image
     */
    public static BufferedImage clip(BufferedImage image, int[] ul,
                                     int[] lr) {
        int imageWidth  = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        int w           = lr[0] - ul[0];
        int h           = lr[1] - ul[1];
        if ((ul[0] + w <= imageWidth) && (ul[1] + h <= imageHeight)
                && (w > 0) && (h > 0)) {
            return image.getSubimage(ul[0], ul[1], w, h);
        }
        System.err.println("Specified clip width/height:" + w + "/" + h
                           + " outside of image width/height:" + imageWidth
                           + "/" + imageHeight);
        return image;
    }

    /**
     * Read and image
     *
     * @param imagePath  the path to the image
     *
     * @return  the Image
     */
    public static Image readImage(String imagePath) {
        return readImage(imagePath, true);
    }

    /**
     * Read and image
     *
     * @param imagePath  the path to the image
     * @param cache Cache the image
     *
     * @return  the Image
     */

    public static Image readImage(String imagePath, boolean cache) {
    	return readImage(imagePath, cache, false);
    }

    /**
     * Read and image
     *
     * @param imagePath  the path to the image
     * @param cache Cache the image
     * @param returnNullIfNotFound  if true, return null if the image does not exist
     *
     * @return  the Image
     */
    public static Image readImage(String imagePath, boolean cache, boolean returnNullIfNotFound) {
        //System.err.println ("getImage");
        Image image = GuiUtils.getImage(imagePath, ImageUtils.class, cache, returnNullIfNotFound);
        //System.err.println ("waiting");
        image = waitOnImage(image);
        //System.err.println ("done waiting");
        return image;
    }



    /**
     * Class MyImageObserver observes and image and keeps track of if its in error
     * and/or when it is loaded
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.29 $
     */
    private static class MyImageObserver implements ImageObserver {


        /** had an error */
        boolean badImage = false;

        /** Seen allbits flag */
        boolean allBits = false;

        /** got width? */
        boolean gotWidth = false;

        /** got height? */
        boolean gotHeight = false;

        /** flag for updates */
        boolean receivedUpdate = false;

        /**
         * Ctor
         */
        public MyImageObserver() {}

        /**
         * Set the image to observe
         *
         * @param i  the image
         */
        public void setImage(Image i) {
            //Humm, is this good enough?
            if ((i.getWidth(null) > 0) && (i.getHeight(null) > 0)) {
                //                i.getWidth(this);
                allBits = true;
            } else {
                i.getWidth(this);
            }
        }

        /**
         * Implemenation of imageUpdate
         *
         * @param img    The image
         * @param flags  flags
         * @param x      x position
         * @param y      y position
         * @param width  width
         * @param height height
         *
         * @return  true if updated
         */
        public boolean imageUpdate(Image img, int flags, int x, int y,
                                   int width, int height) {
            boolean debug = false;
            if (debug) {
                System.err.println("imageUpdate " + flags + " " + width + "X"
                                   + height);
            }
            receivedUpdate = true;
            if ((flags & ImageObserver.WIDTH) != 0) {
                if (debug) {
                    System.err.println("got width");
                }
                gotWidth = true;
            }
            if ((flags & ImageObserver.HEIGHT) != 0) {
                if (debug) {
                    System.err.println("got height");
                }
                gotHeight = true;
            }
            if ( !allBits) {
                //                allBits = gotWidth && gotHeight;
            }
            if (flags == 0) {
                allBits = true;
            }
            if ((flags & ImageObserver.FRAMEBITS) != 0) {
                if (debug) {
                    System.err.println("got FRAMEBITS");
                }
                allBits = true;
            }

            if ((flags & ImageObserver.ALLBITS) != 0) {
                if (debug) {
                    System.err.println("got ALLBITS");
                }
                allBits = true;
            }
            if ((flags & ImageObserver.SOMEBITS) != 0) {
                if (debug) {
                    System.err.println("got SOMEBITS");
                }
                //                System.err.println (id + "\timageUpdate-SOMEBITS");
            }
            if ((flags & ImageObserver.ERROR) != 0) {
                if (debug) {
                    System.err.println("got ERROR");
                }
                //                System.err.println (id + "\timageUpdate-ERROR");
                badImage = true;
            }
            if ((flags & ImageObserver.ABORT) != 0) {
                if (debug) {
                    System.err.println("got ABORT");
                }
                //                System.err.println (id + "\timageUpdate-ABORT");
                badImage = true;
            }
            if (debug) {
                //                    System.err.println("all bits:" + allBits + " badImage:" + badImage);
            }

            return !(allBits || badImage);
        }
    }


    /**
     * Wait until it is loaded in.
     *
     * @param image  the image
     * @return The image
     */
    public static Image waitOnImage(Image image) {
        if (image == null) {
            return null;
        }
        //      System.err.println ("waitOnImage");
        MyImageObserver mio = new MyImageObserver();
        mio.setImage(image);
        int heightOkCnt = 0;
        //Wait at most 2 seconds
        while ( !mio.badImage && !mio.allBits && (heightOkCnt < 20)) {
            Misc.sleep(5);
            if ( !mio.receivedUpdate) {
                if ((image.getWidth(null) > 0)
                        && (image.getHeight(null) > 0)) {
                    heightOkCnt++;
                }
            }
        }
        if (mio.badImage) {
            return null;
        }
        return image;
    }



    /**
     * Make a color in the image transparent
     *
     * @param im  image
     * @param c  the color to make transparent
     *
     * @return  a new image with the color transparent.
     */
    public static BufferedImage makeColorTransparent(Image im, Color c) {
        int[] redRange   = { 0, 0 };
        int[] greenRange = { 0, 0 };
        int[] blueRange  = { 0, 0 };
        if (c != null) {
            redRange[0]   = redRange[1] = c.getRed();
            greenRange[0] = greenRange[1] = c.getGreen();
            blueRange[0]  = blueRange[1] = c.getBlue();
        }
        return makeColorTransparent(im, redRange, greenRange, blueRange);
    }



    /**
     * Set the colors taht are within the given red, green and blue ranges to be transparent.
     *
     * @param im The image
     * @param redRange   red range
     * @param greenRange green range
     * @param blueRange blue range
     *
     * @return munged image
     */
    public static BufferedImage makeColorTransparent(Image im,
            final int[] redRange, final int[] greenRange,
            final int[] blueRange) {
        //      GuiUtils.showDialog("writing image", new JLabel(new ImageIcon(im)));
        //      System.err.println ("rgb:" + redRange[0]+"/"+ greenRange[0]+"/"+ blueRange[0]);
        ImageFilter filter = new RGBImageFilter() {
            public final int filterRGB(int x, int y, int rgb) {
                int red   = (rgb >> 16) & 0xff;
                int green = (rgb >> 8) & 0xff;
                int blue  = (rgb) & 0xff;
                if ((red >= redRange[0]) && (red <= redRange[1])
                        && (green >= greenRange[0])
                        && (green <= greenRange[1]) && (blue >= blueRange[0])
                        && (blue <= blueRange[1])) {
                    return 0x00FFFFFF & rgb;
                } else {
                    return rgb;
                }
            }
        };
        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        im = Toolkit.getDefaultToolkit().createImage(ip);
        //      GuiUtils.showDialog("writing image", new JLabel(new ImageIcon(im)));
        BufferedImage bim = toBufferedImage(im);
        return bim;
    }



    /**
     * Remove the brighter red from the image
     *
     * @param im image
     * @param x1 bounds
     * @param y1 bounds
     * @param x2 bounds
     * @param y2 bounds
     *
     * @return new image
     */
    public static BufferedImage removeRedeye(Image im, final int x1,
                                             final int y1, final int x2,
                                             final int y2) {
        ImageFilter filter = new RGBImageFilter() {
            public final int filterRGB(int x, int y, int rgb) {
                if ((x < x1) || (x > x2) || (y < y1) || (y > y2)) {
                    return rgb;
                }
                int    threshold = 0;
                int    r         = (rgb >> 16) & 0xff;
                int    g         = (rgb >> 8) & 0xff;
                int    b         = (rgb) & 0xff;
                double rbrite    = r * 0.5133333;
                double gbrite    = g;
                double bbrite    = b * 0.1933333;
                if ((rbrite >= gbrite - threshold)
                        && (rbrite >= bbrite - threshold)) {
                    rbrite = (gbrite + bbrite) / 2;
                    r      = (int) (rbrite / 0.51333333);
                    return (0xFF00FFFF & rgb) | (r << 16);
                }
                return rgb;

            }
        };
        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        im = Toolkit.getDefaultToolkit().createImage(ip);
        //        GuiUtils.showDialog("writing image", new JLabel(new ImageIcon(im)));
        BufferedImage bim = toBufferedImage(im);
        return bim;
    }



    /**
     * Change the transparency percentage into an int alpha value
     *
     * @param percent the percent transparent 0-1.0
     *
     * @return the alpha value
     */
    public static int toAlpha(double percent) {
        percent = 1.0 - percent;
        return (int) (0xFF * percent);
    }


    /**
     * Set the alpha channel to the given transparency percent
     *
     * @param im image
     * @param percent Percent transparent 0-1.0
     *
     * @return munged image
     */
    public static BufferedImage setAlpha(Image im, double percent) {
        int       t    = toAlpha(percent);
        final int mask = 0x00FFFFFF | (t << 24);
        if (im == null) {
            return null;
        }
        BufferedImage image = toBufferedImage(im,
                                  BufferedImage.TYPE_INT_ARGB);
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int rgb = image.getRGB(x, y);
                rgb &= mask;
                image.setRGB(x, y, rgb);
            }
        }
        if (true) {
            return image;
        }

        /*
        ImageFilter filter = new RGBImageFilter() {
            public final int filterRGB(int x, int y, int rgb) {
                return mask & rgb;
            }
        };
        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        im = Toolkit.getDefaultToolkit().createImage(ip);
        BufferedImage bim = toBufferedImage(im);
        return bim;*/
        return null;
    }



    /**
     * convenience to convert to a Point
     *
     * @param p point
     *
     * @return point
     */
    public static Point toPoint(Point2D p) {
        return new Point((int) p.getX(), (int) p.getY());
    }

    /**
     * Parse the string specification of a point with respect to the rectangle.
     * The spec can look like: &quot;rectpoint,offsetx,offsety&quot;
     * where rectpoint can be: <pre>
     * ul    um    ur
     * ml    mm    mr
     * ll    lm    lr</pre>
     * Where u=upper,m=middle,l=lower
     * r=right,l=left
     *
     *
     *
     * @param s Stirng spec
     * @param r Reference rect
     *
     * @return The point
     */

    public static Point parsePoint(String s, Rectangle r) {
        s = s.toLowerCase();
        List places = StringUtil.split(s, ",");
        int  dx     = 0;
        int  dy     = 0;
        if (places.size() == 0) {
            places.add("ll");
        }
        String place = (String) places.get(0);
        if (places.size() > 1) {
            dx = new Integer(places.get(1).toString()).intValue();
        }
        if (places.size() > 2) {
            dy = new Integer(places.get(2).toString()).intValue();
        }
        Point2D placePoint = Glyph.getPointOnRect(place, r);
        placePoint.setLocation(placePoint.getX() + dx,
                               placePoint.getY() + dy);
        return toPoint(placePoint);
    }




    /**
     * This method returns a buffered image with the contents of an image
     *
     * @param image  the image
     *
     * @return  a buffered image
     */
    public static BufferedImage toBufferedImage(Image image) {
        return toBufferedImage(image, false);
    }

    /**
     * This method returns a buffered image with the contents of an image
     *
     * @param image  the image
     * @param force If false then just return the image argument if its a BufferedImage
     *
     * @return  a buffered image
     */
    public static BufferedImage toBufferedImage(Image image, boolean force) {
        if ( !force && (image instanceof BufferedImage)) {
            return (BufferedImage) image;
        }

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels; for this method's
        // implementation, see e661 Determining If an Image Has Transparent Pixels
        boolean hasAlpha = hasAlpha(image);
        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }

            // Create the buffered image
            GraphicsDevice        gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(image.getWidth(null),
                    image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null),
                                       image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return bimage;
    }

    /**
     * Merge images
     *
     * @param images list of images
     * @param space space between images
     * @param bg background color
     *
     * @return  merged image
     */
    public static Image mergeImages(List images, int space, Color bg) {
        return gridImages(images, space, bg, 1);
    }

    /**
     * Merge images
     *
     * @param images list of images
     * @param space space between images
     * @param bg background color
     * @param columns number of columns
     *
     * @return  merged image
     */
    public static Image gridImages(List images, int space, Color bg,
                                   int columns) {
        if (images.size() == 1) {
            return (Image) images.get(0);
        }

        int maxHeight = 0;
        int maxWidth  = 0;
        int rows      = (int) (images.size() / (double) columns + 1);
        if (rows == 0) {
            rows = 1;
        }
        for (int i = 0; i < images.size(); i++) {
            Image image       = (Image) images.get(i);
            int   imageWidth  = image.getWidth(null);
            int   imageHeight = image.getHeight(null);
            maxHeight = Math.max(maxHeight, imageHeight);
            maxWidth  = Math.max(maxWidth, imageWidth);
        }

        if (columns > images.size()) {
            columns = images.size();
        }

        BufferedImage bImage = new BufferedImage(maxWidth * columns
                                   + (columns - 1) * space, maxHeight * rows
                                       + (rows - 1)
                                         * space, BufferedImage.TYPE_INT_RGB);

        Graphics g = bImage.getGraphics();

        if (bg != null) {
            g.setColor(bg);
            g.fillRect(0, 0, bImage.getWidth(null), bImage.getHeight(null));
        }


        int colCnt = 0;
        int rowCnt = 0;
        for (int i = 0; i < images.size(); i++) {
            Image image = (Image) images.get(i);
            g.drawImage(image, colCnt * (maxWidth + space),
                        rowCnt * (maxHeight + space), null);
            colCnt++;
            if (colCnt >= columns) {
                colCnt = 0;
                rowCnt++;
            }
        }
        return bImage;
    }


    /**
     * Create a BufferedImage from the given image
     *
     * @param image The image
     * @param type BufferedImage type
     *
     * @return The BufferedImage
     */
    public static BufferedImage toBufferedImage(Image image, int type) {
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        //        System.err.println("tobuffered:" + w +"/" +h);
        BufferedImage bImage = new BufferedImage(w, h, type);
        bImage.getGraphics().drawImage(image, 0, 0, null);
        return bImage;
    }


    /**
     * Check to see if the image has alpha
     *
     * @param image  the image
     *
     * @return true if has alpha
     */
    public static boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            BufferedImage bimage = (BufferedImage) image;
            return bimage.getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {}

        // Get the image's color model
        ColorModel cm = pg.getColorModel();
        return cm.hasAlpha();
    }


    /**
     * Get the screen image from the component
     *
     * @param component The component.
     * @return Its image
     *
     * @throws Exception
     */
    public static Image getImage(Component component) throws Exception {
        RepaintManager repaintManager =
            RepaintManager.currentManager(component);
        double w = component.getWidth();
        double h = component.getHeight();
        if ((w == 0) || (h == 0)) {
            return null;
        }
        BufferedImage image = new BufferedImage((int) w, (int) h,
                                  BufferedImage.TYPE_INT_ARGB);
        repaintManager.setDoubleBufferingEnabled(false);
        Graphics2D g = (Graphics2D) image.getGraphics();
        component.paint(g);
        repaintManager.setDoubleBufferingEnabled(true);
        //        component.repaint();
        //        RepaintManager.setCurrentManager(manager);


        return image;
    }



    /**
     * Write a Buffered image to a file
     *
     * @param image   image to write
     * @param saveFile   file to write to
     *
     * @throws Exception  problem writing file
     */
    public static void writeImageToFile(Image image, File saveFile)
            throws Exception {
        writeImageToFile(image, saveFile.toString());
    }

    /**
     * Write a Buffered image to a file
     *
     * @param image   image to write
     * @param saveFile   file to write to
     *
     * @throws Exception  problem writing file
     */
    public static void writeImageToFile(Image image, String saveFile)
            throws Exception {
        writeImageToFile(image, saveFile, 1.0f);
    }


    /**
     * Flip the image horizontally
     * From: Josiah Hester - http://www.javalobby.org/articles/ultimate-image
     *
     * @param img image
     *
     * @return flipped image
     */
    public static BufferedImage horizontalflip(BufferedImage img) {
        int           w    = img.getWidth();
        int           h    = img.getHeight();
        BufferedImage dimg = new BufferedImage(w, h, img.getType());
        Graphics2D    g    = dimg.createGraphics();
        g.drawImage(img, 0, 0, w, h, w, 0, 0, h, null);
        g.dispose();
        return dimg;
    }


    /**
     * Flip the image vertically
     * From: Josiah Hester - http://www.javalobby.org/articles/ultimate-image
     *
     * @param img image
     *
     * @return flipped image
     */
    public static BufferedImage verticalflip(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage dimg = dimg = new BufferedImage(w, h,
                                 img.getColorModel().getTransparency());
        Graphics2D g = dimg.createGraphics();
        g.drawImage(img, 0, 0, w, h, 0, h, w, 0, null);
        g.dispose();
        return dimg;
    }



    /**
     * Rotate the image 90 degrees
     *
     * @param img image
     * @param left rotate counter clockwise
     *
     * @return rotated image
     */
    public static BufferedImage rotate90(BufferedImage img, boolean left) {
        int    w     = img.getWidth(null);
        int    h     = img.getHeight(null);
        double angle = (left
                        ? -90
                        : 90);
        BufferedImage rotatedImage = new BufferedImage(h, w,
                                         BufferedImage.TYPE_INT_RGB);
        AffineTransform trans = new AffineTransform();
        trans.rotate(Math.toRadians(angle));
        if (left) {
            trans.translate(-w, 0);
        } else {
            trans.translate(0, -h);
        }
        Graphics2D g = rotatedImage.createGraphics();
        //        g.rotate(Math.toRadians(angle),w/2,h/2);
        //        g.drawImage(img, null, 0,0);
        g.drawImage(img, trans, null);
        return rotatedImage;
    }


    /**
     * Convert an image to a new type
     *
     * @param file  image file
     * @param newType  new image type
     *
     * @return name of the new file
     */
    public static String convertImageTo(String file, String newType) {
        try {
            String newFile = IOUtil.stripExtension(file) + "." + newType;
            Image  image   = readImage(file);
            writeImageToFile(image, newFile);
            return newFile;
        } catch (Exception exc) {
            throw new WrapperException(exc);
        }
    }



    /**
     * Write a Buffered image to a file at a particular quality
     *
     * @param image      image to write
     * @param saveFile   file to write to
     * @param quality    image quality (if supported)
     *
     * @throws Exception  problem writing file
     */
    public static void writeImageToFile(Image image, String saveFile,
                                        float quality)
            throws Exception {
        writeImageToFile(image, saveFile, null, quality);
    }

    /**
     * Write a Buffered image to a file at a particular quality
     *
     * @param image      image to write
     * @param os output stream
     * @param quality    image quality (if supported)
     *
     * @throws Exception  problem writing file
     */
    public static void writeImageToFile(Image image, String saveFile,
                                        OutputStream os, float quality)
            throws Exception {
        RenderedImage renderedImage = null;
        File          file          = new File(saveFile);
        // From Heiko Klein, Norwegian Meteorological Institute (www.met.no)
        ImageWriter writer     = null;
        String      fileSuffix = file.getName();
        fileSuffix = fileSuffix.replaceFirst(".*\\.(\\w*)$", "$1");
        Iterator iter = ImageIO.getImageWritersBySuffix(fileSuffix);
        if (iter.hasNext()) {
            writer = (ImageWriter) iter.next();
        } else {
            //The ImageIO  does not seem to handle gif so we'll use
            //the AnimatedGifEncoder. We do this by reflection because 
            //it is in another package and we are trying to keep util 
            //clean
            if (fileSuffix.equals("gif")) {
                AnimatedGifEncoder.createGif(saveFile, image);
                return;
            }
            throw new Exception("unknown suffix: " + fileSuffix);
        }


        // Prepare output file
        //        ImageOutputStream ios = ImageIO.createImageOutputStream(file);
        ImageOutputStream ios;
        if (os != null) {
            ios = ImageIO.createImageOutputStream(os);
        } else {
            ios = ImageIO.createImageOutputStream(file);
        }
        writer.setOutput(ios);

        // Set the compression quality
        ImageWriteParam iwparam = writer.getDefaultWriteParam();
        if (iwparam.canWriteCompressed()) {
            String[] types = iwparam.getCompressionTypes();
            iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwparam.setCompressionType(types[0]);  // pick the first type
            if ( !iwparam.isCompressionLossless()) {
                iwparam.setCompressionQuality(quality);
            }
        }

        //A hack to make sure we aren't writing out an ARGB image to a jpg
        if (saveFile.toLowerCase().endsWith(".jpg")
                || saveFile.toLowerCase().endsWith(".jpeg")) {
            if (ImageUtils.hasAlpha(image)) {
                renderedImage = ImageUtils.toBufferedImage(image,
                        BufferedImage.TYPE_INT_RGB);
                image = (Image) renderedImage;
            }
        }

        if (renderedImage == null) {
            if (image instanceof RenderedImage) {
                renderedImage = (RenderedImage) image;
            } else {
                renderedImage = ImageUtils.toBufferedImage(image);
            }
        }


        // Write the image
        writer.write(null, new IIOImage(renderedImage, null, null), iwparam);

        // Cleanup 
        ios.flush();
        writer.dispose();
        ios.close();

    }


    /**
     * Is the file name an image
     *
     * @param file file
     *
     * @return is image
     */
    public static boolean isImage(String file) {
        file = file.toLowerCase();
        return file.endsWith(".jpg") || file.endsWith(".jpeg")
               || file.endsWith(".gif") || file.endsWith(".png");
    }

    /**
     * Make a screen capture of the window. Write it to the file.
     *
     * @param window The window
     * @param file The file
     * @return Successful
     *
     * @throws Exception
     */
    public static boolean writeImage(JDialog window, String file)
            throws Exception {
        if ((window == null) || !window.isShowing()) {
            return false;
        }
        //        System.err.println("Writing image:" + file);
        writeImageToFile(window.getContentPane(), file);
        return true;
    }


    /**
     * Make a screen capture of the window. Write it to the file.
     *
     * @param window The window
     * @param file The file
     * @return Successful
     *
     * @throws Exception
     */
    public static boolean writeImage(JFrame window, String file)
            throws Exception {
        if ((window == null) || !window.isShowing()) {
            return false;
        }
        writeImageToFile(window.getContentPane(), file);
        return true;
    }


    /** debug flag */
    public static boolean debug = false;



    /**
     * Take a screen snapshot of the component. Write it to the file.
     *
     * @param component The component.
     * @param saveFile The file.
     *
     * @throws Exception
     */
    public static void writeImageToFile(Component component, String saveFile)
            throws Exception {

        if (saveFile.endsWith(".pdf")) {
            OutputStream fos = new FileOutputStream(saveFile);
            writePDF(fos, (JComponent) component);
            fos.close();
            return;
        }


        RepaintManager manager = RepaintManager.currentManager(component);
        //        Image image = manager.getOffscreenBuffer(component, component.getWidth (), component.getHeight ());
        double w = component.getWidth();
        double h = component.getHeight();
        BufferedImage image = new BufferedImage((int) w, (int) h,
                                  BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        component.paint(g);

        //        System.err.println("Writing image:" + saveFile);
        writeImageToFile(image, saveFile);
    }

    /**
     * Write an AVI file
     *
     * @param imageFiles   list of files
     * @param frameRateInFPS frame rate
     * @param outFile  output file
     *
     * @throws IOException problem writing AVI
     */
    public static void writeAvi(java.util.List imageFiles,
                                double frameRateInFPS, File outFile)
            throws IOException {
        gov.noaa.ncdc.nexradiv.AVIWriter aviWriter = null;
        for (int n = 0; n < imageFiles.size(); n++) {
            BufferedImage image = ImageUtils.toBufferedImage(
                                      ImageUtils.readImage(
                                          imageFiles.get(n).toString()));
            ImageUtils.waitOnImage(image);
            if (aviWriter == null) {
                aviWriter = new gov.noaa.ncdc.nexradiv.AVIWriter();
                int width  = image.getWidth(null);
                int height = image.getHeight(null);
                aviWriter.init(outFile, width, height, imageFiles.size(),
                               frameRateInFPS);
            }
            aviWriter.addFrame(image);
        }
        if (aviWriter != null) {
            aviWriter.close();
        }
    }






    /**
     * Resize an image
     *
     * @param image  the image
     * @param width  new width
     * @param height new height
     *
     * @return  resized image
     */
    public static Image resize(Image image, int width, int height) {
        return image.getScaledInstance(width, height,
                                       Image.SCALE_AREA_AVERAGING);
        //                                       Image.SCALE_SMOOTH);

    }


    /**
     * Get an image from the component
     *
     * @param editor component
     * @param transparentColor if non null then set this color to be transparent
     *
     * @return image
     *
     * @throws Exception on badness
     */
    public static Image getImage(JEditorPane editor, Color transparentColor)
            throws Exception {
        editor.setBackground(transparentColor);
        Image i = getImage(editor);
        if (transparentColor != null) {
            i = makeColorTransparent(i, transparentColor);
        }
        return i;
    }


    /**
     * Render the given html and return an image
     *
     * @param html html to render
     * @param width image width
     * @param transparentColor if non null set this color in the image to be transparent
     * @param font font to render with
     *

     * @return image
     *
     * @throws Exception on badness
     */
    public static Image renderHtml(String html, int width,
                                   Color transparentColor, Font font)
            throws Exception {
        JEditorPane editor = getEditor(html, width, transparentColor, font);
        editor.updateUI();
        editor.invalidate();
        editor.validate();
        return getImage(editor, transparentColor);
    }


    /**
     * Make a editor pane from the html
     *
     * @param html html
     * @param width width
     * @param transparentColor what color to set as transparent
     * @param font font
     *
     * @return editor pane
     *
     * @throws Exception on badness
     */
    public static JEditorPane getEditor(String html, int width,
                                        Color transparentColor, Font font)
            throws Exception {
        return getEditor(null, html, width, transparentColor, font);
    }


    /**
     * Make a editor pane from the html
     *
     * @param editor Initial editor
     * @param html html
     * @param width width
     * @param transparentColor what color to set as transparent
     * @param font font
     *
     * @return editor pane
     *
     * @throws Exception on badness
     */
    public static JEditorPane getEditor(JEditorPane editor, String html,
                                        int width, Color transparentColor,
                                        Font font)
            throws Exception {

        if (editor == null) {
            editor = new JEditorPane();
            editor.setContentType("text/html");
        }

        final JEditorPane theEditor = editor;

        theEditor.setBackground(transparentColor);
        theEditor.setEditable(false);
        if (font != null) {
            theEditor.setFont(font);
        }
        theEditor.setText(html);
        Dimension dim = theEditor.getPreferredSize();
        if ((width > 0) && (width < dim.width)) {
            theEditor.setSize(new Dimension(width, 100));
            dim.width = width;
        }
        //Do this a couple of times so we get the height right
        theEditor.setSize(dim);
        theEditor.setSize(theEditor.getSize().width,
                          theEditor.getPreferredSize().height);
        //        GuiUtils.showOkCancelDialog(null,"",theEditor,null);
        return theEditor;
    }

    /**
     * test code
     *
     * @param out test
     * @param comp test
     *
     * @throws IOException test
     */
    public static void writePDF(OutputStream out, JComponent comp)
            throws IOException {

        /*
        debug = true;
        int width = comp.getWidth();
        int height = comp.getHeight();
        try {
            com.lowagie.text.Rectangle pagesize =  new com.lowagie.text.Rectangle(width, height);
            com.lowagie.text.Document document = new com.lowagie.text.Document(pagesize);

            com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.addAuthor("IDV");
            document.open();
            com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContent();
            com.lowagie.text.pdf.PdfTemplate tp = cb.createTemplate(width, height);
            Graphics2D g2 = tp.createGraphics(width, height, new com.lowagie.text.pdf.DefaultFontMapper() );
            //            Graphics2D  g2 = cb.createGraphics(width, height, new com.lowagie.text.pdf.DefaultFontMapper() );

            Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
            RepaintManager repaintManager =
                RepaintManager.currentManager(comp);
            repaintManager.setDoubleBufferingEnabled(false);
            comp.paint(g2);
            repaintManager.setDoubleBufferingEnabled(true);
            g2.dispose();
            cb.addTemplate(tp, 0, 0);
            document.close();
            debug=false;
        }
        catch (com.lowagie.text.DocumentException de) {
            System.err.println(de.getMessage());
        }
        */
    }


    /**
     * Read in the image. Wait util it is loaded in.
     *
     * @param args args
     *
     * @throws Exception  problem with this
     */
    public static void main(String[] args) throws Exception {
        int width = 400;
        System.err.println("reading");
        Image image = readImage(args[0]);


        removeRedeye(image, 10, 10, 1000, 1000);
        if (true) {
            return;
        }


        System.err.println("cvrting");
        BufferedImage bimage = toBufferedImage(image,
                                   BufferedImage.TYPE_INT_ARGB);
        System.err.println("resizing");
        //        BufferedImage resizedImage =
        //            ImageUtils.toBufferedImage(bimage.getScaledInstance(width, -1,
        //                Image.SCALE_AREA_AVERAGING), BufferedImage.TYPE_INT_RGB);

        BufferedImage resizedImage = toBufferedImage(resize(bimage, width,
                                         -1));
        waitOnImage(resizedImage);

        Image  newImage = resizedImage;


        JLabel lbl1;
        JLabel lbl2;



        newImage = ImageUtils.rotate90(toBufferedImage(resizedImage), false);
        lbl1     = new JLabel(new ImageIcon(resizedImage));
        lbl2     = new JLabel(new ImageIcon(newImage));
        GuiUtils.showOkCancelDialog(null, "", GuiUtils.hbox(lbl1, lbl2),
                                    null);
        newImage = ImageUtils.rotate90(toBufferedImage(newImage), false);
        lbl1     = new JLabel(new ImageIcon(resizedImage));
        lbl2     = new JLabel(new ImageIcon(newImage));
        GuiUtils.showOkCancelDialog(null, "", GuiUtils.hbox(lbl1, lbl2),
                                    null);


        newImage = ImageUtils.rotate90(toBufferedImage(resizedImage), true);
        lbl1     = new JLabel(new ImageIcon(resizedImage));
        lbl2     = new JLabel(new ImageIcon(newImage));
        GuiUtils.showOkCancelDialog(null, "", GuiUtils.hbox(lbl1, lbl2),
                                    null);


        newImage = ImageUtils.rotate90(toBufferedImage(newImage), true);
        lbl1     = new JLabel(new ImageIcon(resizedImage));
        lbl2     = new JLabel(new ImageIcon(newImage));
        GuiUtils.showOkCancelDialog(null, "", GuiUtils.hbox(lbl1, lbl2),
                                    null);


        /*
        String ext  = IOUtil.getFileExtension(args[0]);
        String tail = IOUtil.stripExtension(args[0]);
        System.err.println ("writing");
        writeImageToFile(resizedImage, tail + "_thumb" + width + ".png");
        */
        System.exit(0);
    }



    /**
     * Read in the image from the given filename or url
     *
     * @param file File or url
     *
     * @return The image
     *
     * @throws Exception On badness
     */
    public static Image getImageFile(String file) throws Exception {
        if (IOUtil.isHttpProtocol("http:")) {
            byte[] imageBytes = IOUtil.readBytesAndCache(file,
                                    "ImageMovieControl");
            if (imageBytes == null) {
                return null;
            }
            return Toolkit.getDefaultToolkit().createImage(imageBytes);
        } else {
            return Toolkit.getDefaultToolkit().createImage(file);
        }
    }



}

