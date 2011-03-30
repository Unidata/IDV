/*
 * $Id: ImageGlyph.java,v 1.31 2007/04/16 20:53:47 jeffmc Exp $
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

package ucar.unidata.idv.control.drawing;


import org.w3c.dom.Element;

import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.gis.KmlDataSource;


import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.data.imagery.ImageXmlDataSource;


import ucar.unidata.idv.control.DrawingControl;
import ucar.unidata.ui.ImageUtils;

import ucar.unidata.util.ColorTable;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.xml.XmlUtil;


import ucar.visad.display.*;


import visad.*;

import visad.data.gif.GIFForm;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;

import visad.util.DataUtility;
import visad.util.ImageHelper;

import java.awt.*;
import java.awt.image.*;

import java.net.MalformedURLException;
import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;



/**
 * Class ImageGlyph. Displays images.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.31 $
 */
public class ImageGlyph extends DrawingGlyph {


    /** Xml attribute name */
    public static final String ATTR_IMAGE = "image";

    /** Xml attribute name */
    public static final String ATTR_UPDATE = "update";

    /** Used to load in images */
    //    private GIFForm form = new GIFForm();


    /** Holds the image data */
    private FlatField imageData;

    /** Displays the image */
    private ImageRGBDisplayable imageDisplay;

    /** Displays the image outline */
    private LineDrawing outline;


    /** Update frequency */
    int update = -1;

    /** Image url */
    private String image;

    /** Aspect ratio */
    private double aspectRatio = 1.0;

    /** Scale down the image */
    private double scale = 0.5;


    /**
     * Ctor
     */
    public ImageGlyph() {}


    /**
     * Ctor
     *
     * @param control The control I'm in.
     * @param event The display event.
     * @param image The image
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public ImageGlyph(DrawingControl control, DisplayEvent event,
                      String image)
            throws VisADException, RemoteException {
        super(control, event);
        this.image = image;
    }

    /**
     * is this glyph a raster
     *
     * @return is raster (true)
     */
    public boolean getIsRaster() {
        return true;
    }


    /**
     * Initialize after the user created me
     *
     *
     * @param control The control I'm in.
     * @param event The display event.
     *
     * @return ok
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public boolean initFromUser(DrawingControl control, DisplayEvent event)
            throws VisADException, RemoteException {
        //Make sure we set the coordtype up front so the getPoint call below gets a point
        setCoordType(control.getCoordType());

        image = FileManager.getReadFileOrURL(
            "Image Selection", Misc.newList(FileManager.FILTER_IMAGE), null);
        if (image == null) {
            return false;
        }
        points.add(getPoint(event));
        return super.initFromUser(control, event);
    }




    /**
     * Initialize from xml
     *
     *
     * @param control The control I'm in.
     * @param node The xml node
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void initFromXml(DrawingControl control, Element node)
            throws VisADException, RemoteException {
        super.initFromXml(control, node);
        image  = XmlUtil.getAttribute(node, ATTR_IMAGE);
        update = XmlUtil.getAttribute(node, ATTR_UPDATE, -1);
    }




    /**
     * Get extra description for the table listing
     *
     * @return extra description
     */
    public String getExtraDescription() {
        return image;
    }



    /**
     * Was created from user
     *
     * @param event The display event.
     *
     * @return This or null
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public DrawingGlyph handleCreation(DisplayEvent event)
            throws VisADException, RemoteException {

        super.handleCreation(event);
        //        updateLocation();
        return null;
    }



    /**
     * Handle the event
     *
     * @param event The display event.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void doMove(DisplayEvent event)
            throws VisADException, RemoteException {
        beingDragged = true;
        moveTo(event);
        updateLocation();
        //        checkAspectRatio(event, 1);
    }


    /**
     * Handle the event
     *
     * @param event The display event.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void doStretch(DisplayEvent event)
            throws VisADException, RemoteException {
        beingDragged = true;
        points.set(stretchIndex, getPoint(event));
        if (event.getInputEvent().isControlDown()) {
            updateLocation();
            return;
        }
        checkAspectRatio(event, stretchIndex);
    }


    /**
     * Check the aspect ratio
     *
     * @param event The display event.
     * @param indexToMove What point are we stretching
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void checkAspectRatio(DisplayEvent event, int indexToMove)
            throws VisADException, RemoteException {
        double[] pt1    = getBoxPoint(indexToMove);
        double[] pt2    = getBoxPoint((indexToMove == 0)
                                      ? 1
                                      : 0);
        double   height = Math.abs(pt2[IDX_Y] - pt1[IDX_Y]);
        double   width  = aspectRatio * height;
        if (pt1[IDX_X] < pt2[IDX_X]) {
            pt1[IDX_X] = pt2[IDX_X] - width;
        } else {
            pt1[IDX_X] = pt2[IDX_X] + width;
        }

        Object point;
        if (isInXYSpace()) {
            point = pt1;
        } else {
            point = control.boxToEarth(pt1);
        }
        points.set(indexToMove, point);
        updateLocation();
    }


    /**
     * Get the location of the lower right of the image
     *
     * @return Lower right
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private double[] getLowerRight() throws VisADException, RemoteException {
        Linear2DSet imageDomain = (Linear2DSet) imageData.getDomainSet();
        int         width       = imageDomain.getX().getLength();
        int         height      = imageDomain.getY().getLength();
        double[]    origin      = getBoxPoint(0);
        int[]       tmp         = control.boxToScreen(origin);
        tmp[IDX_X] += width;
        tmp[IDX_Y] += height;
        double[] lr = control.screenToBox(tmp[IDX_X], tmp[IDX_Y]);
        aspectRatio = Math.abs(origin[IDX_X] - lr[IDX_X])
                      / Math.abs(origin[IDX_Y] - lr[IDX_Y]);
        return lr;
    }


    /**
     * Is glyph constrained to 2d
     *
     * @return Constrained to 2d
     */
    protected boolean constrainedTo2D() {
        return true;
    }


    /**
     * Populate the xml node with attrs
     *
     * @param e Xml node
     */
    protected void addAttributes(Element e) {
        super.addAttributes(e);
        e.setAttribute(ATTR_IMAGE, image);
        if (update > 0) {
            e.setAttribute(ATTR_UPDATE, "" + update);
        }
    }



    /**
     * Xml tag name to use
     *
     * @return Xml tag name to use
     */
    public String getTagName() {
        return TAG_IMAGE;
    }




    /**
     * Handle event
     *
     * @param event The display event.
     *
     * @return This or null
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public DrawingGlyph handleMouseReleased(DisplayEvent event)
            throws VisADException, RemoteException {
        boolean wasBeingDragged = beingDragged;
        super.handleMouseReleased(event);
        if (outline != null) {
            removeDisplayable(outline);
            outline = null;
        }
        if (wasBeingDragged) {
            updateLocation();
        }
        return null;
    }



    /**
     * Started moving
     *
     * @param event The display event.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void initMove(DisplayEvent event)
            throws VisADException, RemoteException {
        super.initMove(event);
    }



    /**
     * Glyph moved. Update the Displayable location.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void updateLocation() throws VisADException, RemoteException {
        if (points.size() < 2) {
            return;
        }


        if ( !beingDragged) {
            Linear2DSet imageDomain = (Linear2DSet) imageData.getDomainSet();
            Linear2DSet newDomain;
            double[]    origin = getBoxPoint(0);
            double[]    lr     = getBoxPoint(1);
            boolean     swapX  = origin[IDX_X] > lr[IDX_X];
            boolean     swapY  = origin[IDX_Y] < lr[IDX_Y];
            if ( !getFullLatLon() || isInXYSpace()) {
                if (swapX) {
                    swap(origin, lr, IDX_X);
                }
                if (swapY) {
                    swap(origin, lr, IDX_Y);
                }

                actualPoints = getBoundingBox(Misc.newList(origin, lr));
                /*          System.err.println ("update width:" + Math.abs(origin[IDX_X]-lr[IDX_X])
                            + " height:" + Math.abs(origin[IDX_Y]-lr[IDX_Y])
                */

                newDomain =
                    new Linear2DSet(RealTupleType.SpatialCartesian2DTuple,
                                    (float) origin[IDX_X], (float) lr[IDX_X],
                                    imageDomain.getX().getLength(),
                                    (float) origin[IDX_Y], (float) lr[IDX_Y],
                                    imageDomain.getY().getLength());
            } else {
                float[] originLLA =
                    toLatLonAlt((EarthLocation) points.get(0));
                float[] lrLLA = toLatLonAlt((EarthLocation) points.get(1));
                if (swapX) {
                    swap(originLLA, lrLLA, IDX_LON);
                }
                if (swapY) {
                    swap(originLLA, lrLLA, IDX_LAT);
                }

                actualPoints = control.boxToEarth(
                    getBoundingBox(control.earthToBox(points)));
                newDomain =
                    new Linear2DSet(RealTupleType.SpatialEarth2DTuple,
                                    originLLA[IDX_LON], lrLLA[IDX_LON],
                                    imageDomain.getX().getLength(),
                                    originLLA[IDX_LAT], lrLLA[IDX_LAT],
                                    imageDomain.getY().getLength());
                //TODO: Map Z
                double fixedAlt = getFixedAltitude();
            }

            FlatField newImageData =
                (FlatField) GridUtil.setSpatialDomain(imageData, newDomain);
            imageDisplay.loadData(newImageData);
            //Reset the selection points
            super.updateLocation();
            return;
        }



        if (outline == null) {
            control.setDisplayInactive();
            outline = new LineDrawing("DrawingControl.ImageGlyph");
            addDisplayable(outline);
            outline.setColor(Color.blue);
            control.setDisplayActive();
        }

        MathType mathType = null;
        if (isInXYSpace()) {
            mathType = RealTupleType.SpatialCartesian3DTuple;
        } else if (isInLatLonSpace()) {
            mathType = RealTupleType.LatitudeLongitudeAltitude;
        }
        float[][] lineVals = getPointValues();
        float[][] pts      = new float[3][5];
        for (int i = 0; i < 3; i++) {
            pts[i][0] = lineVals[i][0];
            pts[i][4] = lineVals[i][0];
            pts[i][2] = lineVals[i][1];
        }
        pts[IDX_X][1] = lineVals[IDX_X][1];
        pts[IDX_Y][1] = lineVals[IDX_Y][0];
        pts[IDX_Z][1] = lineVals[IDX_Z][0];
        pts[IDX_X][3] = lineVals[IDX_X][0];
        pts[IDX_Y][3] = lineVals[IDX_Y][1];
        pts[IDX_Z][3] = lineVals[IDX_Z][1];
        outline.setData(new Gridded3DSet(mathType, pts, 5));
        super.updateLocation();
    }


    /**
     * Handle event
     *
     * @param event The display event.
     *
     * @return This or null_
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMousePressed(DisplayEvent event)
            throws VisADException, RemoteException {
        super.handleMousePressed(event);
        return this;
    }


    /**
     * Do final initialization
     *
     * @return Success
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected boolean initFinalInner()
            throws VisADException, RemoteException {

        if ( !super.initFinalInner()) {
            return false;
        }


        if (image == null) {
            return false;
        }
        imageData = loadImage();
        if (imageData == null) {
            return false;
        }


	//Make a RGBA
        imageDisplay = new ImageRGBDisplayable("ImageGlyph." + (typeCnt++),true);
        ColorTable colorTable = control.getRGBColorTable();
        imageDisplay.setRangeForColor(0.0, 255.0);
        imageDisplay.setColorPalette(colorTable.getAlphaTable());

        if ( !getFullLatLon() || isInXYSpace()) {
            imageDisplay.addConstantMap(new ConstantMap(getZPosition(),
                    Display.ZAxis));
        } else {
            imageDisplay.addConstantMap(new ConstantMap(getZPosition(),
                    Display.ZAxis));
        }
        addDisplayable(imageDisplay);

        if (points.size() == 1) {
            double[] lr = getLowerRight();
            if (isInXYSpace()) {
                points.add(lr);
            } else {
                points.add(control.boxToEarth(lr));
            }
        }



        if (update > 0) {
            Misc.runInABit(update * 1000, new Runnable() {
                public void run() {
                    doUpdate();
                }
            });
        }
        return true;
    }

    /**
     * Read in the image
     *
     * @return The image data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private FlatField loadImage() throws VisADException, RemoteException {
        FlatField imageData = null;
        try {
            Image theImage = ImageUtils.readImage(image);
            if (theImage != null) {
                if (scale != 1.0) {
                    JLabel lbl;
                    //                    lbl = new JLabel(new ImageIcon(theImage));
                    //GuiUtils.showOkCancelDialog(null,"",lbl,null);
                    theImage = theImage.getScaledInstance(
                        (int) (theImage.getWidth(null) * scale),
                        (int) (theImage.getHeight(null) * scale),
                        Image.SCALE_AREA_AVERAGING);

                    //              ImageUtils.waitOnImage(theImage);
                    //              lbl = new JLabel(new ImageIcon(theImage));
                    //GuiUtils.showOkCancelDialog(null,"",lbl1,null);
                }
                imageData = (FlatField) ucar.visad.Util.makeField(theImage,
							 .0f, true, true);
		//		System.err.println ("image:" + imageData.getType());

            }
        } catch (java.io.IOException ioe) {}
        if (imageData == null) {
            LogUtil.userMessage("Unable to open image:" + image);
        }
        return imageData;
    }




    /**
     * Do the auto image update. Reload the image periodically.
     */
    private void doUpdate() {
        try {
            if ( !control.getActive() || getBeenRemoved()) {
                return;
            }
            FlatField tmp = loadImage();
            if (tmp == null) {
                return;
            }
            imageData = tmp;
            updateLocation();
            if (update > 0) {
                Misc.runInABit(update * 1000, new Runnable() {
                    public void run() {
                        doUpdate();
                    }
                });
            }
        } catch (Exception exc) {
            LogUtil.logException("Updating image:" + image, exc);
        }

    }


    /**
     * Get properties widgets
     *
     * @param comps widgets
     * @param compMap extra holder of components
     */
    protected void getPropertiesComponents(List comps, Hashtable compMap) {
        super.getPropertiesComponents(comps, compMap);
        comps.add(GuiUtils.rLabel(" "));
        comps.add(GuiUtils.left(GuiUtils.makeButton("Export", this,
                "doExport")));

    }

    /**
     * Export the image as an ximg file
     */
    public void doExport() {
        String filename =
            FileManager.getWriteFile(
                Misc.newList(
                    ImageXmlDataSource.FILTER_XIMG,
                    KmlDataSource.FILTER_KML), ImageXmlDataSource.EXT_XIMG);
        if (filename == null) {
            return;
        }
        try {
            double[]      xyz1 = getBoxPoint(0, getPoints());
            double[]      xyz2 = getBoxPoint(1, getPoints());
            EarthLocation el1  = control.boxToEarth(xyz1);
            EarthLocation el2  = control.boxToEarth(xyz2);
            EarthLocation control;
            GeoLocationInfo bounds =
                new GeoLocationInfo(
                    el1.getLatLonPoint().getLatitude().getValue(),
                    el1.getLatLonPoint().getLongitude().getValue(),
                    el2.getLatLonPoint().getLatitude().getValue(),
                    el2.getLatLonPoint().getLongitude().getValue());
            if (filename.endsWith(ImageXmlDataSource.EXT_XIMG)) {
                ImageXmlDataSource.writeToFile(filename, bounds, image);
            } else if (filename.endsWith(KmlDataSource.EXT_KML)) {
                KmlDataSource.writeToFile(filename, bounds, image);
            } else {
                //??
            }

        } catch (Exception exc) {
            LogUtil.logException("Exporting XIMG file", exc);
        }
    }



    /**
     * Name to use for this glyph
     *
     * @return The glyph type name
     */
    public String getTypeName() {
        return "Image";
    }



    /**
     * Set the Image property.
     *
     * @param value The new value for Image
     */
    public void setImage(String value) {
        image = value;
    }

    /**
     * Get the Image property.
     *
     * @return The Image
     */
    public String getImage() {
        return image;
    }




}


