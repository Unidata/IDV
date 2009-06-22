/*
 * $Id: MovieGlyph.java,v 1.13 2007/07/30 17:19:02 jeffmc Exp $
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

import ucar.unidata.data.grid.GridUtil;


import ucar.unidata.idv.control.DrawingControl;

import ucar.unidata.ui.ImageUtils;

import ucar.unidata.util.ColorTable;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.xml.XmlUtil;


import ucar.visad.display.*;


import visad.*;

import visad.data.gif.GIFForm;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;

import visad.util.DataUtility;

import com.sun.media.parser.video.QuicktimeParser;
import com.sun.media.renderer.video.JPEGRenderer;

import java.awt.*;
import java.awt.image.*;

import java.io.File;

import java.net.MalformedURLException;
import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;

import java.util.List;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;


import javax.media.protocol.*;

import javax.media.protocol.URLDataSource;
import javax.media.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;



/**
 * Class MovieGlyph. Displays movies. Not done.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.13 $
 */
public class MovieGlyph extends DrawingGlyph {



    /** Xml attribute name */
    public static final String ATTR_MOVIE = "movie";

    /** The displayable */
    private ImageRGBDisplayable movieDisplay;


    /** The player */
    private Player player;

    /** movie stuff */
    FramePositioningControl fpc;

    /** movie stuff */
    FrameGrabbingControl fgc;


    /** The data */
    private FlatField movieData;


    /** In error */
    private boolean inError = false;

    /** Size */
    private Dimension movieSize;

    /** Where is movie */
    private String movieFile = "/home/jeffmc/test.mov";

    /** Current frame number */
    private int frame = 0;

    /** are we playing the movie */
    private boolean runningMovie = false;

    /** run time */
    private double seconds = 0;

    /** how fast */
    private double frameRate = 2;

    /** Show movie in real time */
    private boolean realTime = false;

    /** loop movie */
    private boolean loop = true;

    /** Show movie in real time */
    private JCheckBox realTimeCbx;

    /** loop movie */
    private JCheckBox loopCbx;

    /** for gui_ */
    private JCheckBox runningCbx;

    /** virtual timestamp */
    private int timestamp = 0;


    /**
     * Ctor
     */
    public MovieGlyph() {}

    /**
     * Ctor
     *
     * @param control The control I'm in.
     * @param event The display event.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public MovieGlyph(DrawingControl control, DisplayEvent event)
            throws VisADException, RemoteException {
        super(control, event);
    }


    /**
     * is this glyph a raster
     *
     * @return is raster
     */
    public boolean getIsRaster() {
        return true;
    }


    /**
     * Init
     *
     * @param control The control I'm in.
     * @param event The display event.
     *
     *
     * @return OK
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public boolean initFromUser(DrawingControl control, DisplayEvent event)
            throws VisADException, RemoteException {
        setCoordType(control.getCoordType());
        movieFile = FileManager.getReadFileOrURL("Movie Selection",
                Misc.newList(FileManager.FILTER_MOV), null);
        if (movieFile == null) {
            return false;
        }
        points.add(getPoint(event));
        return super.initFromUser(control, event);
    }



    /**
     * Init
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
        movieFile = XmlUtil.getAttribute(node, ATTR_MOVIE);
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

    public DrawingGlyph handleCreation(DisplayEvent event)
            throws VisADException, RemoteException {
        super.handleCreation(event);
        points = Misc.newList(getPoint(event));
        updateLocation();
        return null;
    }




    /**
     * Stretch
     *
     * @param event The display event.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void doStretch(DisplayEvent event)
            throws VisADException, RemoteException {
        return;
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
        e.setAttribute(ATTR_MOVIE, movieFile);

    }



    /**
     * Get xml tag name to use
     *
     * @return Xml tag name
     */
    public String getTagName() {
        return TAG_MOVIE;
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
        return null;
    }



    /**
     * Glyph moved. Update the Displayable location.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void updateLocation() throws VisADException, RemoteException {
        super.updateLocation();
    }


    /**
     * Handle event
     *
     * @param event The display event.
     *
     * @return This or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMousePressed(DisplayEvent event)
            throws VisADException, RemoteException {
        return this;
    }


    /**
     * Init final
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
        //Make it a url format
        if ((new File(movieFile)).exists()) {
            movieFile = "file:" + movieFile;
        }

        MediaLocator mrl = null;
        if ((mrl = new MediaLocator(movieFile)) == null) {
            LogUtil.userMessage("Can't build URL for " + movieFile);
            return false;
        }


        try {
            Manager.setHint(Manager.PLUGIN_PLAYER, new Boolean(true));
            player = Manager.createRealizedPlayer(new URL(movieFile));

            Misc.run(new Runnable() {
                public void run() {
                    initPlayer();
                }
            });
        } catch (javax.media.CannotRealizeException cre) {
            if (movieFile.indexOf(".mov") >= 0) {
                LogUtil.userErrorMessage(
                    "The IDV cannot process the given Quicktime movie.\n Not all Quicktime formats are supported.");
            } else {
                LogUtil.userErrorMessage(
                    "The IDV cannot process the given movie.");
            }
            return false;
        } catch (Exception e) {
            LogUtil.logException("Could not create player for " + mrl, e);
            return false;
        }
        //        player.addControllerListener(this);
        //        player.start();


        movieDisplay = new ImageRGBDisplayable("ImageGlyph." + (typeCnt++));
        ColorTable colorTable = control.getRGBColorTable();
        movieDisplay.setRangeForColor(0.0, 255.0);
        movieDisplay.setColorPalette(colorTable.getAlphaTable());

        if ( !getFullLatLon() || isInXYSpace()) {
            movieDisplay.addConstantMap(new ConstantMap(getZPosition(),
                    Display.ZAxis));
        } else {
            movieDisplay.addConstantMap(new ConstantMap(getZPosition(),
                    Display.ZAxis));
        }
        addDisplayable(movieDisplay);
        return true;
    }



    /**
     * SHould show color selector in properties
     *
     * @return false
     */
    protected boolean shouldShowColorSelector() {
        return false;
    }


    /**
     * get properties gui components
     *
     * @param comps comps
     * @param compMap can hold comps
     */
    protected void getPropertiesComponents(List comps, Hashtable compMap) {
        super.getPropertiesComponents(comps, compMap);
        runningCbx  = new JCheckBox("Running", runningMovie);
        realTimeCbx = new JCheckBox("Real Time", runningMovie);
        loopCbx     = new JCheckBox("Loop", loop);
        comps.add(GuiUtils.rLabel("Movie Playback:"));
        JComponent comp = GuiUtils.hbox(runningCbx, realTimeCbx, loopCbx);

        comps.add(GuiUtils.left(comp));
        comps.add(GuiUtils.rLabel("Runtime:"));
        comps.add(new JLabel("" + ((int) seconds) + " seconds"));


    }




    /**
     * apply props
     *
     * @param compMap holds comps
     *
     * @return ok
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    protected boolean applyProperties(Hashtable compMap)
            throws VisADException, RemoteException {
        if ( !super.applyProperties(compMap)) {
            return false;
        }


        realTime = realTimeCbx.isSelected();
        loop     = loopCbx.isSelected();

        if (runningMovie != runningCbx.isSelected()) {
            runningMovie = runningCbx.isSelected();
            if (runningMovie) {
                Misc.run(new Runnable() {
                    public void run() {
                        System.err.println("running ");
                        runMovie(++timestamp);
                    }
                });
            }
        }


        return true;

    }



    /**
     * init qt player
     */
    private void initPlayer() {
        player.prefetch();
        int cnt = 0;
        while (((fpc == null) || (fgc == null)) && (cnt < 100)) {
            fpc = (FramePositioningControl) player.getControl(
                "javax.media.control.FramePositioningControl");

            fgc = (FrameGrabbingControl) player.getControl(
                "javax.media.control.FrameGrabbingControl");
            if ((fpc != null) && (fgc != null)) {
                break;
            }
            Misc.sleep(100);
            cnt++;
        }

        if (fpc == null) {
            LogUtil.userErrorMessage("Failed to load movie");
            return;
        }

        seconds = player.getDuration().getSeconds();
        frame   = 0;
        runMovie(++timestamp);
    }




    /**
     * Run movie
     *
     * @param myTime virtual timestamp
     */
    private void runMovie(int myTime) {
        runningMovie = true;
        while (runningMovie && (myTime == timestamp)) {
            if (getBeenRemoved()) {
                break;
            }
            if (isVisible()) {
                fpc.seek(frame);
                Buffer buf = fgc.grabFrame();
                if (buf == null) {
                    Misc.sleep(250);
                    continue;
                }
                VideoFormat vf = (VideoFormat) buf.getFormat();
                if (vf == null) {
                    Misc.sleep(250);
                    continue;
                }
                if (realTime) {
                    frame += vf.getFrameRate() / frameRate;
                } else {
                    frame++;
                }
                //            System.err.println(vf.getFrameRate()+" frame:" + frame);
                if (frame / vf.getFrameRate() > seconds) {
                    //                System.err.println("reset");
                    frame = 0;
                    if ( !loop) {
                        break;
                    }
                }

                BufferToImage bufferToImage = new BufferToImage(vf);
                Image         im            = bufferToImage.createImage(buf);
                if (im != null) {
                    gotImage(im);
                }
            }
            if (frameRate == 0) {
                break;
            }
            long ms = (long) (1000 / frameRate);
            Misc.sleep(ms);
        }
        runningMovie = false;
    }


    /**
     * Got the image
     *
     * @param img The image
     */
    private void gotImage(Image img) {
        try {
            FlatField   imageData   = DataUtility.makeField(img);
            Linear2DSet imageDomain = (Linear2DSet) imageData.getDomainSet();
            int[]       pt          = control.boxToScreen(getBoxPoint(0));
            int         x           = pt[IDX_X];
            int         y           = pt[IDX_Y];
            int         width       = imageDomain.getX().getLength();
            int         height      = imageDomain.getY().getLength();
            int         left, right, top, bottom;


            left   = x;
            right  = x + width;
            top    = y;
            bottom = y + height;
            double[] origin = control.screenToBox(left, top);
            double[] lr     = control.screenToBox(right, bottom);
            lr[2] = origin[2] = getZPosition();
            Linear2DSet domain =
                new Linear2DSet(RealTupleType.SpatialCartesian2DTuple,
                                (float) origin[IDX_X], (float) lr[IDX_X],
                                imageDomain.getX().getLength(),
                                (float) origin[IDX_Y], (float) lr[IDX_Y],
                                imageDomain.getY().getLength());

            FlatField newImageData =
                (FlatField) GridUtil.setSpatialDomain(imageData, domain);
            movieDisplay.loadData((FieldImpl) getTimeField(newImageData));
            actualPoints = getBoundingBox(Misc.newList(origin, lr));
        } catch (Exception exc) {
            LogUtil.logException("Rendering image", exc);
            player = null;
            return;
        }


    }

    /**
     * Name to use for this glyph
     *
     * @return The glyph type name
     */
    public String getTypeName() {
        return "Movie";
    }



    /**
     *  Set the MovieFile property.
     *
     *  @param value The new value for MovieFile
     */
    public void setMovieFile(String value) {
        movieFile = value;
    }

    /**
     *  Get the MovieFile property.
     *
     *  @return The MovieFile
     */
    public String getMovieFile() {
        return movieFile;
    }


    /**
     * Set the Loop property.
     *
     * @param value The new value for Loop
     */
    public void setLoop(boolean value) {
        loop = value;
    }

    /**
     * Get the Loop property.
     *
     * @return The Loop
     */
    public boolean getLoop() {
        return loop;
    }

    /**
     * Set the FrameRate property.
     *
     * @param value The new value for FrameRate
     */
    public void setFrameRate(double value) {
        frameRate = value;
    }

    /**
     * Get the FrameRate property.
     *
     * @return The FrameRate
     */
    public double getFrameRate() {
        return frameRate;
    }

    /**
     * Set the RealTime property.
     *
     * @param value The new value for RealTime
     */
    public void setRealTime(boolean value) {
        realTime = value;
    }

    /**
     * Get the RealTime property.
     *
     * @return The RealTime
     */
    public boolean getRealTime() {
        return realTime;
    }

    /**
     * Set the RunningMovie property.
     *
     * @param value The new value for RunningMovie
     */
    public void setRunningMovie(boolean value) {
        runningMovie = value;
    }

    /**
     * Get the RunningMovie property.
     *
     * @return The RunningMovie
     */
    public boolean getRunningMovie() {
        return runningMovie;
    }



}


