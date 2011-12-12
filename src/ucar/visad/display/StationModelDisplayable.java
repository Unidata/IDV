/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.visad.display;


import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import org.w3c.dom.Element;

import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.gis.KmlUtil;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.idv.JythonManager;
import ucar.unidata.ui.drawing.Glyph;
import ucar.unidata.ui.symbol.ColorMap;
import ucar.unidata.ui.symbol.LabelSymbol;
import ucar.unidata.ui.symbol.MetSymbol;
import ucar.unidata.ui.symbol.RotateInfo;
import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.ui.symbol.TextSymbol;
import ucar.unidata.ui.symbol.ValueSymbol;
import ucar.unidata.ui.symbol.WeatherSymbol;
import ucar.unidata.ui.symbol.WindBarbSymbol;
import ucar.unidata.ui.symbol.WindVectorSymbol;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Range;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.WrapperException;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.ShapeUtility;
import ucar.visad.Util;
import ucar.visad.WindBarb;

import visad.CommonUnit;
import visad.Data;
import visad.DateTime;
import visad.Display;
import visad.FieldImpl;
import visad.FunctionType;
import visad.Integer1DSet;
import visad.MathType;
import visad.RangeControl;
import visad.Real;
import visad.RealTupleType;
import visad.RealType;
import visad.Scalar;
import visad.ScalarMap;
import visad.ScalarMapControlEvent;
import visad.ScalarMapEvent;
import visad.ScalarMapListener;
import visad.Set;
import visad.ShadowType;
import visad.ShapeControl;
import visad.Text;
import visad.Tuple;
import visad.TupleType;
import visad.Unit;
import visad.VisADException;
import visad.VisADGeometryArray;
import visad.VisADLineArray;
import visad.VisADQuadArray;
import visad.VisADTriangleArray;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;
import visad.georef.NamedLocationTuple;

import visad.meteorology.WeatherSymbols;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.media.j3d.Transform3D;

import javax.swing.ImageIcon;
import javax.swing.JLabel;




/**
 * Class for displaying a station (layout) model plot
 *
 * @author IDV Development Team
 */
public class StationModelDisplayable extends DisplayableData {


    /** Special index value */
    private static int INDEX_LAT = -1000;

    /** Special index value */
    private static int INDEX_LON = -1001;

    /** Special index value */
    private static int INDEX_ALT = -1002;

    /** Special index value */
    private static int INDEX_TIME = -1003;

    /** Scale for offset */
    public static final float OFFSET_SCALE = 20.f;

    /** Mapping of param name to the index */
    private Hashtable<String, Integer> nameToIndex;

    /** Should we use altitude */
    private boolean shouldUseAltitude = true;

    /** flag for rotating shapes */
    private boolean rotateShapes = false;

    /** working  transform */
    private Transform3D transform = new Transform3D();

    /** the current rotation */
    private double[] currentRotation;



    /** Keep around for makeShapes */
    private Point2D workPoint = new Point2D.Double();

    /** Keep around for makeShapes */
    private Rectangle2D workRect = new Rectangle2D.Float();

    /** Work object */
    private Rectangle2D workShapeBounds = new Rectangle2D.Float();





    /** Work object */
    private float[] workOffsetArray = { 0.0f, 0.0f, 0.0f };

    /** Work object */
    private Data[] workDataArray = { null, null };

    /** Work object */
    private float[] workUV = { 0.0f, 0.0f };

    /** Work object */
    float[][] workFlowValues = {
        { 0.0f }, { 0.0f }, { 0.0f }
    };

    /** Work object */
    float[][] workSpatialValues = {
        { 0.0f }, { 0.0f }, { 0.0f }
    };

    /** range select variable */
    private boolean[][] workRangeSelect = {
        { true }, { true }, { true }
    };


    /** Mapping between comma separated param names and the parsed list */
    private Hashtable<String, List> namesToList = new Hashtable<String,
                                                      List>();

    /** Keeps trakc of when we have  printed out a missing param message */
    private Hashtable<String, String> haveNotified = null;

    /**
     * Should we try to merge the shapes. This gets set to false
     *   if we have an error in merging
     */
    private boolean tryMerge = true;

    /** Hashtable for jython codes to operands */
    private Hashtable codeToOperands = new Hashtable();

    /** code for conversion */
    private String convertCode = null;

    /** code for formatting */
    private String fmtCode = null;

    /** Our interperter */
    PythonInterpreter interp;

    /** A cache of shapes */
    private Hashtable shapeCache = new Hashtable();

    /** my color */
    private Color myColor;


    /**
     * The {@link ucar.unidata.idv.JythonManager} to use for accessing
     * jython  interpreters
     */
    private JythonManager jythonManager;

    /** ScalarMap for the weather symbol shapes */
    ScalarMap wxMap = null;

    /** RealType used for the weather symbols */
    RealType wxType = null;

    /** ShapeControl for the weather symbol shapes */
    ShapeControl shapeControl = null;

    /** ScalarMap for the time selection */
    ScalarMap timeSelectMap = null;

    /** RealType used for the time selection */
    RealType timeSelectType = null;

    /** Control for select range */
    private RangeControl timeSelectControl;

    /** low range for select */
    private double lowSelectedRange = Double.NaN;  // low range for scalarmap

    /** high range for select */
    private double highSelectedRange = Double.NaN;  // high range for scalarmap

    /** low range for select map */
    private double minSelect = Double.NaN;  // low range for scalarmap

    /** high range for select map */
    private double maxSelect = Double.NaN;  // high range for scalarmap

    /** The weather symbol shapes */
    VisADGeometryArray[] shapes = null;


    /** static count for incrementing the RealTypes */
    private static int count = 0;

    /** StationModel for laying out the shapes */
    private StationModel stationModel;

    /** The data to use to make the shapes */
    private FieldImpl stationData = null;

    /** index for the shape */
    private int shapeIndex;

    /** Vector of shapes */
    private List shapeList;

    /** scaling factor */
    private float scale = 1.0f;

    /** This is the scale factor to unsquash squashed aspect ratios from the display */
    private double[] displayScaleFactor;

    /** variable indices */
    private int[] varIndices;

    /** instance */
    private static int instance = 0;

    /** mutex for locking */
    private static Object INSTANCE_MUTEX = new Object();

    /** mutex for locking when creating shapes */
    private Object DATA_MUTEX = new Object();

    /** flag for a time sequence */
    private boolean isTimeSequence = false;

    /** the missing alt value */
    private Real missingAlt;

    /**
     * Default constructor;
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable() throws VisADException, RemoteException {
        this("Station Model");
    }


    /**
     * Construct a StationModelDisplayable with the specified name
     * @param name  name of displayable and StationModel
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable(String name)
            throws VisADException, RemoteException {
        this(new StationModel(name));
    }


    /**
     * Construct a StationModelDisplayable with the specified name
     * @param name  name of displayable and StationModel
     * @param jythonManager The JythonManager for evaluating embedded expressions
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable(String name, JythonManager jythonManager)
            throws VisADException, RemoteException {
        this(name, new StationModel(name), jythonManager);
    }



    /**
     * Construct a StationModelDisplayable using the specified model.
     * @param model  StationModel to use for data depiction
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable(StationModel model)
            throws VisADException, RemoteException {
        this(model, null);
    }


    /**
     * Construct a StationModelDisplayable using the specified model.
     * @param model  StationModel to use for data depiction
     * @param jythonManager The JythonManager for evaluating embedded expressions
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable(StationModel model,
                                   JythonManager jythonManager)
            throws VisADException, RemoteException {
        this(model.getName(), model, jythonManager);
    }


    /**
     * Construct a StationModelDisplayable using the specified model and
     * name.
     * @param name  name of for this displayable data reference
     * @param stationModel  StationModel to use for data depiction
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable(String name, StationModel stationModel)
            throws VisADException, RemoteException {
        this(name, stationModel, null);
    }


    /**
     * Construct a StationModelDisplayable using the specified model and
     * name.
     * @param name  name of for this displayable data reference
     * @param stationModel  StationModel to use for data depiction
     * @param jythonManager The JythonManager for evaluating embedded expressions
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable(String name, StationModel stationModel,
                                   JythonManager jythonManager)
            throws VisADException, RemoteException {
        super(name);
        missingAlt         = new Real(RealType.Altitude, 0);
        this.jythonManager = jythonManager;
        this.stationModel  = stationModel;
        setUpScalarMaps();
    }

    /**
     * Clone constructor to create another instance.
     * @param that  object to clone from.
     *
     * @throws RemoteException  a remote error
     * @throws VisADException   a VisAD error
     */
    protected StationModelDisplayable(StationModelDisplayable that)
            throws VisADException, RemoteException {
        super(that);
        this.stationModel = that.stationModel;
    }


    /**
     * Set the station data to display using the StationModel.
     * @param  stationData Field of station observations.
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public void setStationData(FieldImpl stationData)
            throws VisADException, RemoteException {

        synchronized (DATA_MUTEX) {
            setDisplayInactive();
            try {
                Data d = makeNewDataWithShapes(stationData);
                if ((d != null) && !d.isMissing()) {
                    setData(d);

                } else {
                    setData(new Real(0));
                }
            } finally {
                setDisplayActive();
            }
            this.stationData = stationData;  // hold around for posterity
        }
    }


    /**
     * Set the color
     *
     * @param c  the new color
     *
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public void setColor(Color c) throws VisADException, RemoteException {
        super.setColor(c);
        myColor = c;
        if (stationData != null) {
            setStationData(stationData);
        }
    }

    /**
     * Get the default color
     *
     * @return  the color
     */
    public Color getColor() {
        return myColor;
    }

    /**
     * Implement toFront
     *
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public void toFront() throws RemoteException, VisADException {
        super.toFront();
        DisplayMaster master = getDisplayMaster();
        if (master != null) {
            setScale(master.getDisplayScale());
        }
    }


    /**
     * Set up the ScalarMaps for this Displayable
     *
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    private void setUpScalarMaps() throws VisADException, RemoteException {
        int myInstance;
        synchronized (INSTANCE_MUTEX) {
            myInstance = instance++;
        }
        wxType = RealType.getRealType("Station_Model_" + myInstance);
        wxMap  = new ScalarMap(wxType, Display.Shape);
        wxMap.addScalarMapListener(new ScalarMapListener() {
            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {
                int id = event.getId();
                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    shapeControl = (ShapeControl) wxMap.getControl();
                    if (shapeControl != null) {
                        setShapesInControl(shapes);
                        shapeControl.setAutoScale(false);
                        shapeControl.setScale(scale);
                        shapeControl.setAutoScale(true);
                    }
                }
            }

            public void mapChanged(ScalarMapEvent event)
                    throws RemoteException, VisADException {}
        });
        addScalarMap(wxMap);
        timeSelectType = RealType.getRealType("Station_Model_Time"
                + myInstance, CommonUnit.secondsSinceTheEpoch);
        timeSelectMap = new ScalarMap(timeSelectType, Display.SelectRange);
        timeSelectMap.addScalarMapListener(new ScalarMapListener() {
            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {
                int id = event.getId();
                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    timeSelectControl =
                        (RangeControl) timeSelectMap.getControl();
                    if (hasSelectedRange() && (timeSelectControl != null)) {
                        timeSelectControl.setRange(new double[] {
                            lowSelectedRange,
                            highSelectedRange });
                    }
                }
            }

            public void mapChanged(ScalarMapEvent event)
                    throws RemoteException, VisADException {
                if ((event.getId() == event.AUTO_SCALE)
                        && hasSelectMinMax()) {
                    timeSelectMap.setRange(minSelect, maxSelect);
                }
            }
        });
        addScalarMap(timeSelectMap);
    }


    /**
     * set the shapes in the control
     *
     * @param shapes  shapes to use
     * n
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    private void setShapesInControl(VisADGeometryArray[] shapes)
            throws VisADException, RemoteException {
        if (shapeControl != null) {
            if ((shapes == null) || (shapes.length == 0)) {
                shapes = ShapeUtility.createShape(ShapeUtility.NONE);
            }
            shapeControl.setShapeSet(new Integer1DSet(shapes.length));
            shapeControl.setShapes(shapes);
        }

    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()  // revise
            throws RemoteException, VisADException {
        return new StationModelDisplayable(this);
    }

    /**
     * Set the station ob model for this object
     * @param model StationModel to use
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setStationModel(StationModel model)
            throws VisADException, RemoteException {
        setStationModel(model, true);
    }

    /**
     * Set the station ob model for this object
     * @param model StationModel to use
     * @param update update data using new model
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setStationModel(StationModel model, boolean update)
            throws VisADException, RemoteException {
        tryMerge     = true;
        stationModel = model;
        if ((stationData != null) && update) {
            makeNewDataWithShapes(stationData);
        }
    }

    /**
     * Get the station model used by this displayable.
     * @return the station model
     */
    public StationModel getStationModel() {
        return stationModel;
    }



    /** ob counter */
    private int obCounter = 0;

    /** time counter */
    private int timeCounter = 0;

    /** array counter */
    private int geometryArrayCounter = 0;

    /**
     * make the shapes and set the data
     *
     * @param data The data  to create shapes with
     * @return The new field
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private FieldImpl makeNewDataWithShapes(FieldImpl data)
            throws VisADException, RemoteException {

        if (data == null) {
            return null;
        }

        DisplayMaster master = getDisplayMaster();
        displayScaleFactor = null;
        if (master != null) {
            double[] aspect = master.getDisplayAspect();
            if ((aspect != null) && (aspect.length > 2)) {
                displayScaleFactor = new double[] { 1.0 / aspect[0],
                        1.0 / aspect[1], 1.0 / aspect[2] };
            }
        }

        FieldImpl newFI = null;
        if (rotateShapes) {
            currentRotation = master.getRotation();
            double[] m =
                master.getMouseBehavior().make_matrix(currentRotation[0],
                    currentRotation[1], currentRotation[2], 1.0, 0.0, 0.0,
                    0.0);

            transform = new Transform3D(m);
            transform.invert();
            //            System.err.println("rot:" + currentRotation[0] + "/" + currentRotation[1] +"/" +
            //                               currentRotation[2]);
        }


        haveNotified   = null;
        isTimeSequence = ucar.unidata.data.grid.GridUtil.isTimeSequence(data);
        int numTimes = 0;
        shapeIndex           = 0;
        shapeList            = new Vector();
        nameToIndex          = new Hashtable<String, Integer>();
        obCounter            = 0;
        timeCounter          = 0;
        geometryArrayCounter = 0;
        try {
            if (isTimeSequence) {
                boolean haveChecked = false;
                Set     timeSet     = data.getDomainSet();
                for (int i = 0; i < timeSet.getLength(); i++) {
                    timeCounter++;
                    FieldImpl sample  = (FieldImpl) data.getSample(i);
                    FieldImpl shapeFI = makeShapesFromPointObsField(sample);
                    if (shapeFI == null) {
                        continue;
                    }
                    if (newFI == null) {  // first time through
                        FunctionType functionType =
                            new FunctionType(
                                ((FunctionType) data.getType()).getDomain(),
                                shapeFI.getType());
                        newFI = new FieldImpl(functionType, timeSet);
                    }
                    newFI.setSample(i, shapeFI, false, !haveChecked);
                    if ( !haveChecked) {
                        haveChecked = true;
                    }
                }  // end isSequence
            } else {
                newFI = makeShapesFromPointObsField(data);
            }      // end single time 
        } catch (Exception exc) {
            logException("making shapes", exc);
            return null;
        }

        try {
            shapes = new VisADGeometryArray[shapeList.size()];
            shapes = (VisADGeometryArray[]) shapeList.toArray(shapes);
            setShapesInControl(shapes);
        } catch (Exception t) {
            throw new VisADException(
                "Unable to covert vector to VisADGeometry array");
        }
        return newFI;
    }






    /** kmz writing state */
    private boolean writingKmz = false;

    /** kmz writing state */
    private ZipOutputStream kmzZos;

    /** kmz writing state */
    private int kmzObCounter = 0;

    /** kmz writing state */
    private Element kmlRoot;

    /** kmz writing state */
    private Element kmlDoc;

    /** kmz writing state */
    private int kmzIconHeight = 40;

    /** kmz writing state */
    private int kmzIconWidth = 40;

    /** kmz writing state */
    private Color iconColor;

    /** kmz writing state */
    private File kmzFile;

    /** kmz writing state */
    private boolean kmzShowExampleDialog = true;


    /**
     * Write the station display into a kmz file
     *
     * @param f The file
     * @param data The data
     * @param name Name for kml
     * @param kmzIconWidth icon width
     * @param kmzIconHeight icon height
     * @param bgColor background color of icon
     *
     * @return success
     *
     * @throws Exception on badness
     */
    public boolean writeKmzFile(File f, FieldImpl data, String name,
                                int kmzIconWidth, int kmzIconHeight,
                                Color bgColor)
            throws Exception {
        try {

            this.kmzShowExampleDialog = true;
            this.kmzIconWidth         = kmzIconWidth;
            this.kmzIconHeight        = kmzIconHeight;
            this.iconColor            = bgColor;
            this.kmzObCounter         = 0;
            this.kmzFile              = f;
            this.writingKmz           = true;
            this.kmlRoot              = KmlUtil.kml(name);
            this.kmlDoc               = KmlUtil.document(kmlRoot, name);



            isTimeSequence =
                ucar.unidata.data.grid.GridUtil.isTimeSequence(data);



            currentRotation = null;
            if (isTimeSequence) {
                Set timeSet = data.getDomainSet();
                for (int i = 0; i < timeSet.getLength(); i++) {
                    makeShapesFromPointObsField(
                        (FieldImpl) data.getSample(i));
                }
            } else {
                makeShapesFromPointObsField(data);
            }

            if ( !writingKmz) {
                return false;
            }

            kmzZos.putNextEntry(new ZipEntry("observations.kml"));
            byte[] bytes = XmlUtil.toString(kmlRoot, false).getBytes();
            kmzZos.write(bytes, 0, bytes.length);
            kmzZos.closeEntry();
        } finally {
            writingKmz = false;
            if (kmzZos != null) {
                kmzZos.close();
            }
            kmzZos  = null;
            kmzFile = null;
        }
        return true;
    }



    /**
     * create shapes for an individual time step.
     *
     * @param data
     * @return
     *
     *
     * @throws Exception on badness
     */
    private FieldImpl makeShapesFromPointObsField(FieldImpl data)
            throws Exception {

        Set  set = data.getDomainSet();
        Data tmp = data.getSample(0);
        if (tmp.isMissing()) {
            return null;
        }

        PointOb       firstOb   = (PointOb) data.getSample(0);
        FunctionType  fieldType = (FunctionType) data.getType();
        RealTupleType domain    = fieldType.getDomain();
        MathType      llType;
        boolean       useAltitude = getShouldUseAltitude();
        if (useAltitude) {
            llType = firstOb.getEarthLocation().getType();
        } else {
            llType = RealTupleType.LatitudeLongitudeTuple;
        }
        //      System.err.println(" usealt:" + useAltitude +" llType:" + llType);

        Real indexReal = new Real(wxType, 0);
        Real timeReal  = new Real(timeSelectType, 0);

        TupleType tt = new TupleType(new MathType[] { wxType, llType,
                timeSelectType });
        FunctionType    retType          = new FunctionType(domain, tt);
        List            dataList         = new ArrayList();
        List<MetSymbol> symbols          = new ArrayList<MetSymbol>();
        List<Point2D>   pointOnSymbols   = new ArrayList<Point2D>();
        List            offsetFlipPoints = new ArrayList();

        for (Iterator iter = stationModel.iterator(); iter.hasNext(); ) {
            MetSymbol metSymbol = (MetSymbol) iter.next();
            if ( !metSymbol.getActive()) {
                continue;
            }
            symbols.add(metSymbol);
            Rectangle symbolBounds = metSymbol.getBounds();
            String    symbolPoint  = metSymbol.getRectPoint();
            Point2D pointOnSymbol = Glyph.getPointOnRect(symbolPoint,
                                        symbolBounds);
            pointOnSymbol.setLocation(pointOnSymbol.getX() / OFFSET_SCALE,
                                      pointOnSymbol.getY() / OFFSET_SCALE);

            pointOnSymbols.add(pointOnSymbol);
            offsetFlipPoints.add(Glyph.flipY(symbolPoint));
        }
        int  total = 0;
        long t1    = System.currentTimeMillis();
        Trace.call1("SMD.makeShapes loop",
                    " #obs:" + set.getLength() + " #vars:"
                    + ((Tuple) firstOb.getData()).getLength());
        String[]  typeNames     = null;
        int       length        = set.getLength();
        TupleType dataTupleType = null;


        for (int obIdx = 0; obIdx < length; obIdx++) {
            PointOb ob = (PointOb) data.getSample(obIdx);

            obCounter++;
            if (typeNames == null) {
                Tuple     obData = (Tuple) ob.getData();
                TupleType tType  = (TupleType) obData.getType();
                typeNames = getTypeNames(tType);
            }

            List<VisADGeometryArray> obShapes = makeShapes(ob, typeNames,
                                                    symbols, pointOnSymbols,
                                                    offsetFlipPoints);
            if (obShapes == null) {
                continue;
            }
            geometryArrayCounter += obShapes.size();
            if (writingKmz) {
                processKmz(ob, obShapes);
            }

            Data location = (useAltitude
                             ? ob.getEarthLocation()
                             : ob.getEarthLocation().getLatLonPoint());
            // check for missing altitude
            if (useAltitude) {
                EarthLocation oldOne = (EarthLocation) location;
                Real          alt    = oldOne.getAltitude();
                if (alt.isMissing()) {
                    location = new EarthLocationLite(oldOne.getLatitude(),
                            oldOne.getLongitude(), missingAlt);

                }
            }
            DateTime obTime = ob.getDateTime();
            Real time = timeReal.cloneButValue(
                            obTime.getValue(timeSelectType.getDefaultUnit()));

            for (int j = 0; j < obShapes.size(); j++) {
                Data[] dataArray = new Data[] {
                                       indexReal.cloneButValue(shapeIndex++),
                                       location, time };
                if (dataTupleType == null) {
                    dataTupleType = Tuple.buildTupleType(dataArray);
                }
                Tuple t = new Tuple(dataTupleType, dataArray, false, false);
                dataList.add(t);
                shapeList.add(obShapes.get(j));
            }
        }
        Trace.call2("SMD.makeShapes loop");

        long t2 = System.currentTimeMillis();
        //      System.err.println ("ob shape time:" +(t2-t1));

        FieldImpl fi = null;
        if ( !dataList.isEmpty()) {
            Data[] dArray = new Data[dataList.size()];
            try {
                dArray = (Data[]) dataList.toArray(dArray);
            } catch (Exception t) {
                throw new VisADException(
                    "Unable to convert vector to data array");
            }
            Integer1DSet index = new Integer1DSet(domain, dArray.length);
            fi = new FieldImpl(retType, index);
            fi.setSamples(dArray, false, false);
        } else {  // return a missing object
            fi = new FieldImpl(retType, new Integer1DSet(domain, 1));
            fi.setSample(0, new Tuple(tt), false);
        }

        return fi;
    }


    /**
     * Process a KMZ file
     *
     * @param ob  the ob
     * @param obShapes  the ob shaped
     *
     * @throws Exception  problem
     */
    private void processKmz(PointOb ob, List<VisADGeometryArray> obShapes)
            throws Exception {
        BufferedImage image = new BufferedImage(kmzIconWidth, kmzIconHeight,
                                  BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        if (iconColor != null) {
            g.setColor(iconColor);
            g.fillRect(0, 0, kmzIconWidth, kmzIconHeight);
        }
        g.setStroke(new BasicStroke(2.0f));
        paint(g, obShapes);
        if (kmzShowExampleDialog) {
            writingKmz = ucar.unidata.util.GuiUtils.showOkCancelDialog(null,
                    null,
                    GuiUtils.vbox(new JLabel("Example:"),
                                  new JLabel(new ImageIcon(image))), null);
            kmzShowExampleDialog = false;
            if ( !writingKmz) {
                return;
            }
        }

        if (kmzZos == null) {
            kmzZos = new ZipOutputStream(new FileOutputStream(kmzFile));

        }



        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ucar.unidata.ui.ImageUtils.writeImageToFile(image, "icon.png", bos,
                1.0f);
        String file = "kmzicon" + (kmzObCounter) + ".png";
        kmzZos.putNextEntry(new ZipEntry(file));
        byte[] bytes = bos.toByteArray();
        kmzZos.write(bytes, 0, bytes.length);
        kmzZos.closeEntry();
        String       styleId = "obicon" + kmzObCounter;
        StringBuffer descSB  = new StringBuffer();
        descSB.append("<h3>Observation</h3>");
        descSB.append("<table>");
        Tuple     tuple = (Tuple) ob.getData();
        TupleType tType = (TupleType) tuple.getType();
        String[]  names = getTypeNames(tType);
        Data[]    datum = tuple.getComponents();

        for (int i = 0; i < names.length; i++) {
            descSB.append("<tr><td align=right><b>");
            descSB.append(names[i]);

            descSB.append(":</b></td><td>");
            if (datum[i] instanceof Real) {
                Real r = (Real) datum[i];
                if (r.isMissing()) {
                    descSB.append("--");
                } else {
                    descSB.append("" + r);
                }
                Unit u = r.getUnit();
                if (u != null) {
                    String us = u.toString();
                    if (us.length() > 0) {
                        descSB.append(" [" + us + "]");
                    }
                }
            } else {
                descSB.append(datum[i] + "");
            }
            descSB.append("</td></tr>");
        }



        descSB.append("</table>");
        EarthLocation el = ob.getEarthLocation();
        Element placemark =
            KmlUtil.placemark(
                kmlDoc, "", descSB.toString(),
                el.getLatitude().getValue(visad.CommonUnit.degree),
                el.getLongitude().getValue(visad.CommonUnit.degree),
                ((el.getAltitude() != null)
                 ? el.getAltitude().getValue()
                 : 0), styleId);
        KmlUtil.timestamp(placemark,
                          ucar.visad.Util.makeDate(ob.getDateTime()));
        KmlUtil.iconstyle(kmlDoc, styleId, file, 2.0);
        kmzObCounter++;
    }


    /**
     * Make shape from an observation based on the StationModel
     *
     * @param ob  a single point ob to turn into a shape
     * @param typeNames list of type names
     * @param symbols List of the MetSymbols to use
     * @param pointOnSymbols List of the rectangle point on the symbols
     * @param offsetFlipPoints List of the flipper points
     * @return  corresponding shape
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private List<VisADGeometryArray> makeShapes(PointOb ob,
            String[] typeNames, List<MetSymbol> symbols,
            List<Point2D> pointOnSymbols, List offsetFlipPoints)
            throws VisADException, RemoteException {

        List      lineShapes     = null;
        List      triangleShapes = null;
        List      quadShapes     = null;
        Tuple     data           = (Tuple) ob.getData();
        TupleType tType          = (TupleType) data.getType();

        MetSymbol metSymbol      = null;
        workDataArray[0] = null;
        workDataArray[1] = null;
        //The workDataArray should never be more than size 2
        try {
            for (int symbolIdx = 0; symbolIdx < symbols.size(); symbolIdx++) {
                metSymbol = symbols.get(symbolIdx);
                Point2D  pointOnSymbol    = pointOnSymbols.get(symbolIdx);
                float    shapeScaleFactor = .05f;
                String[] paramIds         = metSymbol.getParamIds();
                boolean  ok               = true;
                if ( !(metSymbol instanceof LabelSymbol)
                        && !metSymbol.doAllObs()) {
                    int max = Math.min(workDataArray.length, paramIds.length);
                    for (int paramIdx = 0; paramIdx < max; paramIdx++) {
                        String paramId = paramIds[paramIdx];
                        if (paramId.startsWith("=")) {
                            //Is a jython formula
                            workDataArray[paramIdx] = evaluateCode(ob,
                                    paramId.substring(1), tType, typeNames,
                                    data, metSymbol,
                                    metSymbol.getDisplayUnit());
                        } else if (paramId.startsWith("value:")) {
                            String tok = paramId.substring(6);
                            workDataArray[paramIdx] = Util.toReal(tok);
                        } else {
                            workDataArray[paramIdx] = getComponent(ob, data,
                                    tType, typeNames, paramId);
                        }
                        if (workDataArray[paramIdx] == null) {
                            ok = false;
                            break;
                        }
                    }
                }
                if ( !ok) {
                    continue;
                }

                Rectangle            symbolBounds = metSymbol.getBounds();
                VisADGeometryArray[] shapes       = null;
                VisADGeometryArray   shape        = null;


                if (metSymbol.doAllObs()) {
                    shapes = metSymbol.makeShapes(ob);
                } else if (metSymbol instanceof TextSymbol) {
                    TextSymbol textSymbol  = (TextSymbol) metSymbol;
                    Font       font        = textSymbol.getFont();
                    String     stringValue = null;
                    Scalar     scalar      = (Scalar) workDataArray[0];
                    //Perhaps cache on value,font,number format and display unit
                    //              Object theKey = new Object[]{font,scalar};
                    //shape = (VisADGeometryArray) shapeCache.get(key);
                    if ((scalar != null) && (scalar instanceof Real)
                            && (metSymbol instanceof ValueSymbol)) {
                        ValueSymbol vs = (ValueSymbol) metSymbol;
                        try {
                            double value = ((vs.getDisplayUnit() == null)
                                            || ((Real) scalar).getUnit()
                                               == null)
                                           ? ((Real) scalar).getValue()
                                           : ((Real) scalar).getValue(
                                               vs.getDisplayUnit());
                            stringValue = Double.isNaN(value)
                                          ? null
                                          : vs.formatNumber(value);
                        } catch (Exception ue) {
                            throw new WrapperException("Incompatible units "
                                    + ((Real) scalar).getUnit() + " & "
                                    + vs.getDisplayUnit(), ue);
                        }
                    } else if (metSymbol instanceof LabelSymbol) {
                        stringValue = ((LabelSymbol) metSymbol).getText();
                    } else if (metSymbol instanceof TextSymbol) {
                        if (scalar instanceof Text) {
                            stringValue = ((Text) scalar).getValue();
                        } else {
                            stringValue = ((TextSymbol) metSymbol).format(
                                (Real) scalar);
                        }
                    }
                    if (stringValue != null) {
                        if (font != null) {
                            shapeScaleFactor = 0.1f;
                        }
                        int    fontSize = textSymbol.getFontSize();
                        Object key = font + "_" + fontSize + "_"
                                     + stringValue;
                        shape = (VisADGeometryArray) shapeCache.get(key);
                        if (shape == null) {
                            shape = (font == null)
                                    ? ShapeUtility.shapeText(stringValue,
                                    fontSize, true)
                                    : ShapeUtility.shapeFont(stringValue,
                                    font, true);
                            if (shape != null) {
                                if ((font != null) && (fontSize != 12)) {
                                    shape = ShapeUtility.setSize(shape,
                                            (float) fontSize / 12.0f);
                                }
                                shapeCache.put(key, shape.clone());
                            }
                        } else {
                            shape = (VisADGeometryArray) shape.clone();
                        }
                    }
                } else if (metSymbol instanceof WeatherSymbol) {
                    double value = Double.NaN;
                    try {
                        if (workDataArray[0] instanceof Text) {
                            value = new Double(
                                workDataArray[0].toString()).doubleValue();
                        } else {
                            value = ((Real) workDataArray[0]).getValue();
                        }
                        // ignore null data
                    } catch (Exception npe) {}
                    if ( !Double.isNaN(value)) {
                        shape =
                            ((WeatherSymbol) metSymbol).getLines((int) value);
                    }
                } else if (metSymbol instanceof WindBarbSymbol) {
                    boolean isNorth =
                        (ob.getEarthLocation().getLatitude().getValue(
                            CommonUnit.degree) < 0.0);
                    String name = metSymbol.getClass().getName();
                    Object key = "wind_" + pointOnSymbol + "_" + isNorth
                                 + "_" + workDataArray[0] + "_"
                                 + workDataArray[1]+"_"+name;
                    //shapes = (VisADGeometryArray[]) shapeCache.get(key);
                    if (shapes != null) {
                        shapes = ShapeUtility.clone(shapes);
                    } else {
                        float speed;
                        float direction;
                        try {
                            speed =
                                (float) ((Real) workDataArray[0]).getValue(
                                    CommonUnit.meterPerSecond);
                        } catch (Exception e) {
                            speed =
                                (float) ((Real) workDataArray[0]).getValue();
                        }
                        // could be speed or v component.  default to speed
                        Real vOrSpeed = (Real) workDataArray[1];
                        if (Unit.canConvert(vOrSpeed.getUnit(),
                                            CommonUnit.meterPerSecond)) {
                            workUV[0] = speed;
                            workUV[1] = (float) vOrSpeed.getValue(
                                CommonUnit.meterPerSecond);
                        } else {
                            try {
                                direction = (float) vOrSpeed.getValue(
                                    CommonUnit.degree);
                            } catch (Exception e) {
                                direction = (float) vOrSpeed.getValue();
                            }
                            // should be 360 for north
                            windVector(speed, direction, workUV);
                        }
                        workFlowValues[0][0] = workUV[0];
                        workFlowValues[1][0] = workUV[1];
                        workFlowValues[2][0] = 0;  // zero out every time
                        if (Float.isNaN(workUV[0])
                                || Float.isNaN(workUV[1])) {
                            continue;
                        }
                        DisplayMaster master = getDisplayMaster();
                        // adjust flow to earth
                        if ((master
                                instanceof ucar.unidata.view.geoloc
                                    .NavigatedDisplay) && (renderer != null)
                                        && addRefsInvoked()) {
                            ucar.unidata.view.geoloc.NavigatedDisplay navDisplay =
                                (ucar.unidata.view.geoloc.NavigatedDisplay) master;
                            double[] boxCoords =
                                navDisplay.getSpatialCoordinates(
                                    ob.getEarthLocation(), new double[3]);
                            float[][] spatial_locs = new float[][] {
                                { (float) boxCoords[0] },
                                { (float) boxCoords[1] },
                                { (float) boxCoords[2] }
                            };

                            if (renderer != null) {
                                ShadowType.adjustFlowToEarth(0,
                                        workFlowValues, spatial_locs,
                                        getScale(), renderer, true);
                            }
                        }
                        workSpatialValues[0][0] =
                            (float) (pointOnSymbol.getX());
                        workSpatialValues[1][0] =
                            (float) (-pointOnSymbol.getY());
                        //float scale = 2.5f * (float) metSymbol.getScale();
                        try {
                        	if (metSymbol instanceof WindVectorSymbol) {
                               shapes = ((WindVectorSymbol) metSymbol).makeVector(workFlowValues,
                                    .5f, workSpatialValues, (byte[][]) null,  //color_values, 
                                    workRangeSelect);
                        	} else {
                               shapes = WindBarb.staticMakeFlow(workFlowValues,
                                       2.5f, workSpatialValues, (byte[][]) null,  //color_values, 
                                       workRangeSelect, isNorth);
                        	}
                            shapeCache.put(key, ShapeUtility.clone(shapes));

                        } catch (Exception excp) {
                            //System.out.println("speed = " + speed);
                            //System.out.println("dir = " + speed);
                            //Misc.printArray("workUV", workUV);
                        }  // bad winds
                    }

                } else {
                    //Default is to ask the symbol to make the shapes
                    shapes = metSymbol.makeShapes(workDataArray, ob);
                }

                if (shape != null) {
                    shapes = new VisADGeometryArray[] { shape };
                } else if (shapes == null) {
                    continue;
                }

                String scaleParam = metSymbol.getScaleParam();
                if (scaleParam != null) {
                    Data scaleData = getComponent(ob, data, tType, typeNames,
                                         scaleParam);
                    if (scaleData != null) {
                        boolean valueOk = true;
                        double  value   = 0;
                        if (scaleData instanceof Real) {
                            Unit scaleUnit = metSymbol.getScaleUnit();
                            value = ((scaleUnit != null)
                                     ? ((Real) scaleData).getValue(scaleUnit)
                                     : ((Real) scaleData).getValue());
                        } else {
                            try {
                                value =
                                    Double.parseDouble(scaleData.toString());
                            } catch (NumberFormatException nfe) {
                                valueOk = false;
                            }
                        }
                        if (valueOk) {
                            Range  dataRange  = metSymbol.getScaleDataRange();
                            Range  scaleRange = metSymbol.getScaleRange();
                            double percent    = dataRange.getPercent(value);
                            if (percent < 0.0) {
                                percent = 0.0;
                            } else if (percent > 1.0) {
                                percent = 1.0;
                            }
                            shapeScaleFactor *=
                                scaleRange.getValueOfPercent(percent);
                        }
                    }
                }
                shapeScaleFactor *= metSymbol.getScale();

                Rectangle2D shapeBounds = null;
                for (int shapeIndex = 0; shapeIndex < shapes.length;
                        shapeIndex++) {
                    if (shapes[shapeIndex] == null) {
                        continue;

                    }
                    double lat =
                        90 - ob.getEarthLocation().getLatitude().getValue(
                            CommonUnit.degree);
                    double lon =
                        ob.getEarthLocation().getLongitude().getValue(
                            CommonUnit.degree);

                    for (int i = 0; i < RotateInfo.TYPES.length; i++) {
                        RotateInfo info =
                            metSymbol.getRotateInfo(RotateInfo.TYPES[i]);
                        String rotateParam = info.getParam();
                        if (rotateParam == null) {
                            continue;
                        }
                        double angle = 0.0;
                        if (rotateParam.startsWith("angle:")) {
                            angle = Math.toRadians(
                                Double.parseDouble(rotateParam.substring(6)));
                        } else {
                            Data rotateData = getComponent(ob, data, tType,
                                                  typeNames, rotateParam);
                            if ((rotateData == null)
                                    || !(rotateData instanceof Real)) {
                                continue;
                            }
                            Unit   rotateUnit = info.getUnit();
                            double value      = ((rotateUnit != null)
                                    ? ((Real) rotateData).getValue(rotateUnit)
                                    : ((Real) rotateData).getValue());
                            Range rotateRange     = info.getRange();
                            Range rotateDataRange = info.getDataRange();
                            double percent =
                                Math.min(
                                    1.0,
                                    Math.max(
                                        0.0,
                                        rotateDataRange.getPercent(value)));
                            angle = -Math.toRadians(
                                rotateRange.getValueOfPercent(percent));

                        }
                        if (RotateInfo.TYPES[i] == RotateInfo.TYPE_Z) {
                            ShapeUtility.rotateZ(shapes[shapeIndex],
                                    (float) angle);
                        } else if (RotateInfo.TYPES[i] == RotateInfo.TYPE_X) {
                            ShapeUtility.rotateX(shapes[shapeIndex],
                                    (float) angle);
                        } else {
                            ShapeUtility.rotateY(shapes[shapeIndex],
                                    (float) angle);
                        }
                    }



                    shapeBounds = ShapeUtility.bounds2d(shapes[shapeIndex],
                            workRect);
                    double tmpScale = shapeScaleFactor;
                    if (metSymbol.shouldScaleShape()) {
                        float size = (metSymbol instanceof TextSymbol)
                                     ? ((TextSymbol) metSymbol).getFontSize()
                                       / 12.f
                                     : Math.min((float) ((symbolBounds.width
                                         / OFFSET_SCALE) / shapeBounds
                                             .getWidth()), (float) ((symbolBounds
                                                 .height / OFFSET_SCALE) / shapeBounds
                                                     .getHeight()));
                        tmpScale *= size;
                    }

                    if (displayScaleFactor != null) {
                        ShapeUtility.reScale(shapes[shapeIndex],
                                             displayScaleFactor,
                                             shapeScaleFactor);
                    } else {
                        ShapeUtility.reScale(shapes[shapeIndex],
                                             shapeScaleFactor);
                    }
                    if (metSymbol.shouldScaleShape()) {
                        shapeBounds =
                            ShapeUtility.bounds2d(shapes[shapeIndex],
                                shapeBounds);
                    }
                    if (shapeIndex == 0) {
                        workShapeBounds.setRect(shapeBounds);
                    } else {
                        workShapeBounds =
                            shapeBounds.createUnion(workShapeBounds);
                    }
                }

                shapeBounds = workShapeBounds;
                for (int s = 0; s < shapes.length; s++) {
                    if (shapes[s] == null) {
                        continue;
                    }
                    if (metSymbol.shouldOffsetShape()) {
                        Point2D fromPoint = Glyph.getPointOnRect(
                                                (String) offsetFlipPoints.get(
                                                    symbolIdx), shapeBounds,
                                                        workPoint);
                        workOffsetArray[0] = (float) (pointOnSymbol.getX()
                                * shapeScaleFactor - fromPoint.getX());
                        workOffsetArray[1] = (float) (-pointOnSymbol.getY()
                                * shapeScaleFactor - fromPoint.getY());
                        ShapeUtility.offset(shapes[s], workOffsetArray);
                    }

                    //Bump it a bit up
                    ShapeUtility.offset(shapes[s], 0.0f, 0.0f, 0.002f);


                    ColorTable ct         = metSymbol.getColorTable();
                    String     colorParam = metSymbol.getColorParam();
                    String     ctParam    = metSymbol.getColorTableParam();
                    //                    System.err.println("colorParam:" + colorParam + ": ctParam:" + ctParam+":");
                    if ((colorParam != null) && (colorParam.length() > 0)) {
                        if (metSymbol.shouldBeColored()) {
                            Data colorData = getComponent(ob, data, tType,
                                                 typeNames, colorParam);
                            if (colorData != null) {
                                List  mappings = metSymbol.getColorMappings();
                                Color theColor = metSymbol.getForeground();
                                if ((mappings != null)
                                        && (mappings.size() > 0)) {
                                    for (int i = 0; i < mappings.size();
                                            i++) {
                                        ColorMap colorMap =
                                            (ColorMap) mappings.get(i);
                                        if (colorMap.match(colorData)) {
                                            Color color = colorMap.getColor();
                                            if (color == null) {
                                                continue;
                                            }
                                            theColor = color;
                                            break;
                                        }
                                    }
                                } else {
                                    String colorString = colorData.toString();
                                    theColor =
                                        ucar.unidata.util.GuiUtils
                                            .decodeColor(colorString, null);
                                }
                                //                                if(theColor == null) {
                                //                                    theColor = myColor;
                                //                                }
                                if (theColor != null) {
                                    ShapeUtility.setColor(shapes[s],
                                            theColor);
                                }
                            }
                        }
                    } else if ((ct == null) || (ctParam == null)
                               || (ctParam.length() == 0)) {
                        if (metSymbol.shouldBeColored()) {
                            Color theColor = metSymbol.getForeground();
                            if (theColor == null) {
                                theColor = myColor;
                            }
                            if (theColor != null) {
                                ShapeUtility.setColor(shapes[s], theColor);
                            }
                        }
                    } else {
                        Data ctData = getComponent(ob, data, tType,
                                          typeNames, ctParam);
                        if (ctData != null) {
                            Unit    ctUnit  = metSymbol.getColorTableUnit();
                            double  value   = 0.0;
                            boolean valueOk = true;
                            if (ctData instanceof Real) {
                                value = ((ctUnit != null)
                                         ? ((Real) ctData).getValue(ctUnit)
                                         : ((Real) ctData).getValue());
                            } else {
                                try {
                                    value =
                                        Double.parseDouble(ctData.toString());
                                } catch (Exception exc) {
                                    valueOk = false;
                                    System.err.println(ctData.toString());
                                }
                            }
                            if (valueOk) {
                                Range  r = metSymbol.getColorTableRange();
                                double percent = r.getPercent(value);
                                if (percent < 0.0) {
                                    percent = 0.0;
                                } else if (percent > 1.0) {
                                    percent = 1.0;
                                }
                                List   colors = ct.getColorList();
                                double dindex = (colors.size() * percent);
                                int    index  = (int) dindex;
                                //Round up
                                if (dindex - index >= 0.5) {
                                    index++;
                                }
                                index--;
                                // System.err.println ("v:" + value + " r:" + r + " %:" + percent + " dindex:" + dindex +" index:" + index + " size:" + colors.size());
                                if (index < 0) {
                                    index = 0;
                                }
                                ShapeUtility.setColor(shapes[s],
                                        (Color) colors.get(index));
                            }

                        }
                    }


                    VisADQuadArray bgshape = null;
                    if (metSymbol.getBackground() != null) {
                        Rectangle2D tmp = ShapeUtility.bounds2d(shapes[s],
                                              workRect);
                        Rectangle2D.Float bgb =
                            new Rectangle2D.Float((float) tmp.getX(),
                                (float) tmp.getY(), (float) tmp.getWidth(),
                                (float) tmp.getHeight());


                        bgb.x               = bgb.x - bgb.width * 0.05f;
                        bgb.y               = bgb.y - bgb.height * 0.05f;
                        bgb.width           += bgb.width * 0.1f;
                        bgb.height          += bgb.height * 0.1f;

                        bgshape             = new VisADQuadArray();
                        bgshape.coordinates = new float[] {
                            bgb.x, bgb.y, 0.0f, bgb.x, bgb.y + bgb.height,
                            0.0f, bgb.x + bgb.width, bgb.y + bgb.height, 0.0f,
                            bgb.x + bgb.width, bgb.y, 0.0f
                        };
                        bgshape.vertexCount = bgshape.coordinates.length / 3;
                        bgshape.normals     = new float[12];
                        for (int i = 0; i < 12; i += 3) {
                            bgshape.normals[i]     = 0.0f;
                            bgshape.normals[i + 1] = 0.0f;
                            bgshape.normals[i + 2] = 1.0f;
                        }
                        //Bump it a bit up
                        ShapeUtility.offset(bgshape, 0.0f, 0.0f, 0.001f);

                        ShapeUtility.setColor(bgshape,
                                metSymbol.getBackground());

                        quadShapes = add(quadShapes, bgshape);
                    }

                    if (rotateShapes && (currentRotation != null)
                            && metSymbol.rotateOnEarth()) {
                        for (VisADGeometryArray points : shapes) {
                            ShapeUtility.rotate(points, transform);
                        }
                        if (bgshape != null) {
                            ShapeUtility.rotate(bgshape, transform);
                        }
                    }

                    if (shapes[s] instanceof VisADLineArray) {
                        lineShapes = add(lineShapes, shapes[s]);
                    } else if (shapes[s] instanceof VisADQuadArray) {
                        quadShapes = add(quadShapes, shapes[s]);
                    } else {
                        triangleShapes = add(triangleShapes, shapes[s]);
                    }
                }
            }
        } catch (Exception e) {
            throw new WrapperException("Error generating symbol: "
                                       + ((metSymbol != null)
                                          ? metSymbol.getName()
                                          : ""), e);
        }

        List<VisADGeometryArray> allShapes =
            new ArrayList<VisADGeometryArray>();
        //Try to merge them. But, if any of the  Visad arrays have different
        //state (normals,colors, etc.) there will be an error. We track that with the 
        //tryMerge flag.
        if (lineShapes != null) {
            if ((lineShapes.size() > 1) && tryMerge) {
                try {
                    VisADLineArray[] la =
                        new VisADLineArray[lineShapes.size()];
                    la = (VisADLineArray[]) lineShapes.toArray(la);
                    allShapes.add(VisADLineArray.merge(la));
                } catch (visad.DisplayException exc) {
                    tryMerge = false;
                    allShapes.addAll(lineShapes);
                }
            } else {
                allShapes.addAll(lineShapes);
            }
        }
        if (triangleShapes != null) {
            if ((triangleShapes.size() > 1) && tryMerge) {
                try {
                    VisADTriangleArray[] ta =
                        new VisADTriangleArray[triangleShapes.size()];
                    ta = (VisADTriangleArray[]) triangleShapes.toArray(ta);
                    allShapes.add(VisADTriangleArray.merge(ta));
                } catch (visad.DisplayException exc) {
                    tryMerge = false;
                    allShapes.addAll(triangleShapes);
                }
            } else {
                allShapes.addAll(triangleShapes);
            }
        }
        if (quadShapes != null) {
            allShapes.addAll(quadShapes);
        }
        return allShapes;
    }





    /**
     * Paint the shapes into the graphics for kmz icon generation
     *
     * @param g2 graphics
     * @param shapes shapes
     */
    private void paint(Graphics2D g2, List<VisADGeometryArray> shapes) {

        for (VisADGeometryArray array : shapes) {
            int     my     = kmzIconHeight / 2;
            int     mx     = kmzIconWidth / 2;
            float[] pts    = array.coordinates;
            int     count  = array.vertexCount;
            byte[]  colors = array.colors;
            if (colors == null) {
                //                System.err.println ("colors are null");
                //                continue;
            }
            if (pts == null) {
                //                System.err.println ("pts are null");
                //                continue;
            }
            //            System.err.println ("shape:" + array.getClass().getName() + " pts:" + pts.length +" colors:" + colors.length);
            float scale = 200.0f;
            if (array instanceof VisADLineArray) {
                if (colors == null) {
                    g2.setColor(Color.black);
                }
                int jinc = ((colors == null)
                            ? 0
                            : (colors.length == pts.length)
                              ? 3
                              : 4);
                int j    = 0;
                for (int i = 0; i < 3 * count; i += 6) {
                    if (colors != null) {
                        g2.setColor(new Color((((colors[j] < 0)
                                ? (((int) colors[j]) + 256)
                                : ((int) colors[j])) + ((colors[j + jinc] < 0)
                                ? (((int) colors[j + jinc]) + 256)
                                : ((int) colors[j + jinc]))) / 2, (((colors[j + 1]
                                   < 0)
                                ? (((int) colors[j + 1]) + 256)
                                : ((int) colors[j + 1])) + ((colors[j + jinc + 1]
                                   < 0)
                                ? (((int) colors[j + jinc + 1]) + 256)
                                : ((int) colors[j + jinc + 1]))) / 2, (((colors[j + 2]
                                   < 0)
                                ? (((int) colors[j + 2]) + 256)
                                : ((int) colors[j + 2])) + ((colors[j + jinc + 2]
                                   < 0)
                                ? (((int) colors[j + jinc + 2]) + 256)
                                : ((int) colors[j + jinc + 2]))) / 2));
                    }
                    j += 2 * jinc;
                    g2.draw(new Line2D.Float(mx + scale * pts[i],
                                             my - scale * pts[i + 1],
                                             mx + scale * pts[i + 3],
                                             my - scale * pts[i + 4]));
                }
            } else if (array instanceof VisADTriangleArray) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_OFF);

                if (colors == null) {
                    for (int i = 0; i < 3 * count; i += 9) {
                        GeneralPath path =
                            new GeneralPath(GeneralPath.WIND_EVEN_ODD);
                        path.moveTo(mx + scale * pts[i],
                                    my - scale * pts[i + 1]);
                        path.lineTo(mx + scale * pts[i + 3],
                                    my - scale * pts[i + 4]);
                        path.lineTo(mx + scale * pts[i + 6],
                                    my - scale * pts[i + 7]);
                        path.closePath();
                        g2.fill(path);
                    }
                } else {  // colors != null
                    int j    = 0;
                    int jinc = (colors.length == pts.length)
                               ? 3
                               : 4;
                    for (int i = 0; i < 3 * count; i += 9) {
                        g2.setColor(new Color((((colors[j] < 0)
                                ? (((int) colors[j]) + 256)
                                : ((int) colors[j])) + ((colors[j + jinc] < 0)
                                ? (((int) colors[j + jinc]) + 256)
                                : ((int) colors[j + jinc])) + ((colors[j + 2 * jinc]
                                   < 0)
                                ? (((int) colors[j + 2 * jinc]) + 256)
                                : ((int) colors[j + 2 * jinc]))) / 3, (((colors[j + 1]
                                   < 0)
                                ? (((int) colors[j + 1]) + 256)
                                : ((int) colors[j + 1])) + ((colors[j + jinc + 1]
                                   < 0)
                                ? (((int) colors[j + jinc + 1]) + 256)
                                : ((int) colors[j + jinc + 1])) + ((colors[j + 2 * jinc + 1]
                                   < 0)
                                ? (((int) colors[j + 2 * jinc + 1]) + 256)
                                : ((int) colors[j + 2 * jinc + 1]))) / 3, (((colors[j + 2]
                                   < 0)
                                ? (((int) colors[j + 2]) + 256)
                                : ((int) colors[j + 2])) + ((colors[j + jinc + 2]
                                   < 0)
                                ? (((int) colors[j + jinc + 2]) + 256)
                                : ((int) colors[j + jinc + 2])) + ((colors[j + 2 * jinc + 2]
                                   < 0)
                                ? (((int) colors[j + 2 * jinc + 2]) + 256)
                                : ((int) colors[j + 2 * jinc + 2]))) / 3));
                        j += 3 * jinc;
                        GeneralPath path =
                            new GeneralPath(GeneralPath.WIND_EVEN_ODD);
                        path.moveTo(mx + scale * pts[i],
                                    my - scale * pts[i + 1]);
                        path.lineTo(mx + scale * pts[i + 3],
                                    my - scale * pts[i + 4]);
                        path.lineTo(mx + scale * pts[i + 6],
                                    my - scale * pts[i + 7]);
                        path.closePath();
                        g2.fill(path);
                    }
                }
            } else if (array instanceof VisADQuadArray) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_OFF);
                if (colors == null) {
                    for (int i = 0; i < 3 * count; i += 12) {
                        GeneralPath path =
                            new GeneralPath(GeneralPath.WIND_EVEN_ODD);
                        path.moveTo(mx + scale * pts[i],
                                    my - scale * pts[i + 1]);
                        path.lineTo(mx + scale * pts[i + 3],
                                    my - scale * pts[i + 4]);
                        path.lineTo(mx + scale * pts[i + 6],
                                    my - scale * pts[i + 7]);
                        path.lineTo(mx + scale * pts[i + 9],
                                    my - scale * pts[i + 10]);
                        path.closePath();
                        g2.fill(path);
                    }
                } else {  // colors != null
                    int j    = 0;
                    int jinc = (colors.length == pts.length)
                               ? 3
                               : 4;
                    for (int i = 0; i < 3 * count; i += 12) {
                        g2.setColor(new Color((((colors[j] < 0)
                                ? (((int) colors[j]) + 256)
                                : ((int) colors[j])) + ((colors[j + jinc] < 0)
                                ? (((int) colors[j + jinc]) + 256)
                                : ((int) colors[j + jinc])) + ((colors[j + 2 * jinc]
                                   < 0)
                                ? (((int) colors[j + 2 * jinc]) + 256)
                                : ((int) colors[j + 2 * jinc])) + ((colors[j + 3 * jinc]
                                   < 0)
                                ? (((int) colors[j + 3 * jinc]) + 256)
                                : ((int) colors[j + 3 * jinc]))) / 4, (((colors[j + 1]
                                   < 0)
                                ? (((int) colors[j + 1]) + 256)
                                : ((int) colors[j + 1])) + ((colors[j + jinc + 1]
                                   < 0)
                                ? (((int) colors[j + jinc + 1]) + 256)
                                : ((int) colors[j + jinc + 1])) + ((colors[j + 2 * jinc + 1]
                                   < 0)
                                ? (((int) colors[j + 2 * jinc + 1]) + 256)
                                : ((int) colors[j + 2 * jinc + 1])) + ((colors[j + 3 * jinc + 1]
                                   < 0)
                                ? (((int) colors[j + 3 * jinc + 1]) + 256)
                                : ((int) colors[j + 3 * jinc + 1]))) / 4, (((colors[j + 2]
                                   < 0)
                                ? (((int) colors[j + 2]) + 256)
                                : ((int) colors[j + 2])) + ((colors[j + jinc + 2]
                                   < 0)
                                ? (((int) colors[j + jinc + 2]) + 256)
                                : ((int) colors[j + jinc + 2])) + ((colors[j + 2 * jinc + 2]
                                   < 0)
                                ? (((int) colors[j + 2 * jinc + 2]) + 256)
                                : ((int) colors[j + 2 * jinc + 2])) + ((colors[j + 3 * jinc + 2]
                                   < 0)
                                ? (((int) colors[j + 3 * jinc + 2]) + 256)
                                : ((int) colors[j + 3 * jinc + 2]))) / 4));
                        j += 4 * jinc;
                        GeneralPath path =
                            new GeneralPath(GeneralPath.WIND_EVEN_ODD);
                        path.moveTo(mx + scale * pts[i],
                                    my - scale * pts[i + 1]);
                        path.lineTo(mx + scale * pts[i + 3],
                                    my - scale * pts[i + 4]);
                        path.lineTo(mx + scale * pts[i + 6],
                                    my - scale * pts[i + 7]);
                        path.lineTo(mx + scale * pts[i + 9],
                                    my - scale * pts[i + 10]);
                        path.closePath();
                        g2.fill(path);
                    }
                }
            }

        }

    }


    /**
     * Utility to add to the given list. If null then create it
     *
     * @param l The list
     * @param o The object to add
     *
     * @return The given list if non-null, else the newly created list.
     */
    private List add(List l, Object o) {
        if (l == null) {
            l = new ArrayList();
        }
        l.add(o);
        return l;
    }


    /*
     * Get the jython interpreter to use for evaluating embedded expressions
     *
     * @return The jython interpreter to use
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
        return jythonManager.getDerivedDataInterpreter();
        }
     */


    /**
     * Called when the displayable is removed from a display master
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void destroy() throws RemoteException, VisADException {
        if (getDestroyed()) {
            return;
        }
        if (interp != null) {
            jythonManager.removeInterpreter(interp);
            interp        = null;
            jythonManager = null;
        }
        super.destroy();
    }


    /** Track how much time is spent evaluating code */
    private long codeTime = 0;

    /**
     * Evaluate some Jython code
     *
     *
     * @param ob           The point ob
     * @param code         Jython code
     * @param tType        tuple type of all symbols
     * @param typeNames    list of type names
     * @param data         corresponding data
     * @param formatter    formatter for code
     * @param displayUnit  unit for display
     * @return
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private Data evaluateCode(PointOb ob, String code, TupleType tType,
                              String[] typeNames, Tuple data,
                              Object formatter, Unit displayUnit)
            throws VisADException, RemoteException {
        //Find the operands in the code
        List operands = (List) codeToOperands.get(code);
        if (operands == null) {
            operands = DerivedDataChoice.parseOperands(code);
            codeToOperands.put(code, operands);
        }

        if (interp == null) {
            interp      = jythonManager.createInterpreter();
            convertCode = "from ucar.visad.Util import *\n\n";
            convertCode += "def format(v):\n";
            convertCode += "    if formatter is None:\n";
            convertCode += "        return str(v);\n";
            convertCode += "    return formatter.format(v)\n";
            convertCode += "\n\n";
            convertCode += "def convert(v):\n";
            convertCode += "    if displayUnit is None:\n";
            convertCode += "        return v\n";
            convertCode += "    return v.getValue (displayUnit)\n";
            convertCode += "\n\n";
            convertCode += "def formatDate(v,format):\n";
            convertCode += "    return formatUtcDate(v,format)\n";
            convertCode += "\n\n";
            interp.exec(convertCode);
        }

        interp.set("displayUnit", displayUnit);
        interp.set("formatter", formatter);

        //Bind the operands
        for (int opIdx = 0; opIdx < operands.size(); opIdx++) {
            String op     = (String) operands.get(opIdx).toString();
            Data   opData = getComponent(ob, data, tType, typeNames, op);
            if (opData == null) {
                return null;
            }
            interp.set(op, opData);
        }

        /**
         *       if(codeTime==0) {
         *           String testCode = "def p(op):\n";
         *           testCode += "   return op\n\n\n";
         *           interp.exec(testCode);
         *           testCode = "";
         *           for(int i=0;i<1000;i++) {
         *               interp.set("o"+i, "x"+i);
         *               testCode +="r"+i+"=p(o"+i+")\n";
         *           }
         *           long testt1 = System.currentTimeMillis();
         *           interp.exec(testCode);
         *           long testt2 = System.currentTimeMillis();
         *           System.err.println ("test:" + (testt2-testt1));
         *           for(int i=0;i<1000;i++) {
         *               Object obj = interp.get("r"+i,Object.class);
         *               System.err.print(obj+",");
         *           }
         *           System.err.print("");
         *       }
         */

        long t1 = System.currentTimeMillis();
        //Evaluate the code
        PyObject pyResult = interp.eval(code);
        long     t2       = System.currentTimeMillis();
        codeTime += (t2 - t1);



        //Get the result
        Object resultObject = pyResult.__tojava__(visad.Data.class);
        if ((resultObject == null) || !(resultObject instanceof visad.Data)) {
            resultObject = pyResult.__tojava__(Object.class);
        }

        //Make sure we have the right kind of return value
        if ( !(resultObject instanceof Data)) {
            resultObject = new visad.Text(resultObject.toString());
            //            throw new IllegalArgumentException ("Unknown return value type:" + resultObject.getClass().getName () + "\n Value=" + resultObject +"\nCode:" + code);
        }

        //Now clear out the bindings
        for (int opIdx = 0; opIdx < operands.size(); opIdx++) {
            interp.set((String) operands.get(opIdx).toString(), null);
        }
        return (Data) resultObject;

    }


    /**
     * Debug to print out a message
     *
     * @param msg  message to print
     */
    void pr(String msg) {
        System.err.println(msg);
    }

    /**
     * Debug to print out a message and the bounds of a Rectangle2D
     *
     * @param msg message to print out
     * @param r   rectangle
     */
    void pr(String msg, Rectangle2D r) {
        System.err.println(msg + " y1 =" + " " + r.getY() + " cy= "
                           + (r.getY() + r.getHeight() / 2) + " y2="
                           + (r.getY() + r.getHeight()) + " height= "
                           + r.getHeight());
    }

    /**
     * Debug to print out a message and a point
     *
     * @param msg  message
     * @param r    point
     */
    void pr(String msg, Point2D r) {
        System.err.println(msg + " pt.y= " + r.getY());
    }

    /**
     * Set the scale of the ShapeControl.  Usually done to set
     * the initial scale.
     * @param newScale  scale to use.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setScale(float newScale)
            throws VisADException, RemoteException {
        if (shapeControl != null) {
            //We set auto scale false here so the shape control 
            //clears out its controllistener which was keeping around the old initial scale
            shapeControl.setAutoScale(false);
            shapeControl.setScale(newScale);
            shapeControl.setAutoScale(true);
        }
        scale = newScale;
    }

    /**
     * Get the scale of the ShapeControl.
     * @return current scale;
     */
    public float getScale() {
        return (shapeControl != null)
               ? shapeControl.getScale()
               : scale;
    }


    /**
     * Create u and v components from speed and direction.
     * @param speed  wind speed (m/s)
     * @param direction  wind direction (degrees)
     * @param uv uv work array
     */
    private void windVector(float speed, float direction, float[] uv) {
        if (((direction > 0.f) && (direction <= 360.f))
                || ((direction == 0.f) && (speed == 0.f))) {
            uv[0] = (float) (-1 * speed
                             * Math.sin(direction * Data.DEGREES_TO_RADIANS));
            uv[1] = (float) (-1 * speed
                             * Math.cos(direction * Data.DEGREES_TO_RADIANS));
        } else {
            uv[0] = Float.NaN;
            uv[1] = Float.NaN;
        }
    }



    /**
     * Get the index of any of a list of comma separated names in
     * that match the names of ScalarTypes in a TupleType.
     *
     * @param typeNames names
     * @param names List of param names
     * @return the index if found.
     */
    private int getIndex(String[] typeNames, List names) {
        for (int i = 0; i < names.size(); i++) {
            String name = (String) names.get(i);

            if (name.equalsIgnoreCase(PointOb.PARAM_LAT)
                    || name.equalsIgnoreCase("latitude")) {
                return INDEX_LAT;
            }
            if (name.equalsIgnoreCase(PointOb.PARAM_LON)
                    || name.equalsIgnoreCase("longitude")) {
                return INDEX_LON;
            }
            if (name.equalsIgnoreCase(PointOb.PARAM_ALT)
                    || name.equalsIgnoreCase("altitude")) {
                return INDEX_ALT;
            }
            if (name.equalsIgnoreCase(PointOb.PARAM_TIME)
                    || name.equalsIgnoreCase("dttm")) {
                return INDEX_TIME;
            }

            // first check to see if the name is good before aliases
            //            int index = getIndex(tType,ScalarType.getScalarTypeByName(name));
            int index = getIndex(typeNames, name);
            if (index != PointOb.BAD_INDEX) {
                return index;
            }
            List aliases = DataAlias.getAliasesOf(name);
            if ((aliases == null) || aliases.isEmpty()) {
                continue;
            }
            for (int aliasIdx = 0; aliasIdx < aliases.size(); aliasIdx++) {
                String alias = (String) aliases.get(aliasIdx);
                //                index = tType.getIndex(ScalarType.getScalarTypeByName(alias));
                index = getIndex(typeNames, alias);
                if (index != PointOb.BAD_INDEX) {
                    return index;
                }
            }
        }

        return PointOb.BAD_INDEX;
    }


    /**
     * Find the index in the TupleType of the MathType whose name matches
     * lookingFor. If the math type name ends with '[unit:...]' then strip
     * that off
     *
     * @param names   list of names
     * @param lookingFor  pattern to look for
     *
     * @return index or bad index
     */
    private int getIndex(String[] names, String lookingFor) {
        if (lookingFor.equals("*")) {
            if (names.length > 0) {
                return 0;
            }
            return PointOb.BAD_INDEX;
        }


        if (lookingFor.startsWith("#")) {
            int index = new Integer(lookingFor.substring(1)).intValue();
            if (index < names.length) {
                return index;
            }
            return PointOb.BAD_INDEX;

        }


        boolean not = false;
        if (lookingFor.startsWith("!")) {
            lookingFor = lookingFor.substring(1);
            not        = true;
        }

        if (StringUtil.containsRegExp(lookingFor)) {
            for (int i = 0; i < names.length; i++) {
                if (StringUtil.stringMatch(names[i], lookingFor)) {
                    if (not) {
                        continue;
                    }
                    return i;
                } else if (not) {
                    return i;
                }
            }
        }



        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(lookingFor)) {
                if (not) {
                    continue;
                }
                return i;
            } else if (not) {
                return i;
            }
        }
        return PointOb.BAD_INDEX;
    }


    /**
     *
     * @param tType  get the list of type names
     *
     * @return the list
     */
    private String[] getTypeNames(TupleType tType) {
        MathType[] comps = tType.getComponents();
        String[]   names = new String[comps.length];
        for (int i = 0; i < comps.length; i++) {
            String name = Util.cleanTypeName(comps[i]);
            names[i] = name;
        }
        return names;
    }




    /**
     * Find and return the Data component in the given Tuple that matches one of
     * the param names in the given argument.
     *
     *
     * @param ob  The point ob
     * @param data The tuple to look into.
     * @param tType  <code>TupleType</code> to check.
     * @param typeNames list of type names
     * @param commaSeparatedNames  a string containing a comma separated
     *                             list of names for a data alias.
     *
     * @return the Data if found.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     *
     */
    private Data getComponent(PointOb ob, Tuple data, TupleType tType,
                              String[] typeNames, String commaSeparatedNames)
            throws VisADException, RemoteException {

        List names = namesToList.get(commaSeparatedNames);
        if (names == null) {
            names = StringUtil.split(commaSeparatedNames, ",", true, true);
            namesToList.put(commaSeparatedNames, names);
        }
        int     index;
        Integer cachedIndex = (Integer) nameToIndex.get(commaSeparatedNames);
        if (cachedIndex != null) {
            index = cachedIndex.intValue();
        } else {
            index = getIndex(typeNames, names);
            nameToIndex.put(commaSeparatedNames, new Integer(index));
        }

        if (index == INDEX_LAT) {
            return ob.getEarthLocation().getLatitude();
        }
        if (index == INDEX_LON) {
            return ob.getEarthLocation().getLongitude();
        }
        if (index == INDEX_ALT) {
            return ob.getEarthLocation().getAltitude();
        }
        if (index == INDEX_TIME) {
            return ob.getDateTime();
        }

        if (index == PointOb.BAD_INDEX) {
            if (haveNotified == null) {
                haveNotified = new Hashtable<String, String>();
            }
            if (haveNotified.get(commaSeparatedNames) == null) {
                haveNotified.put(commaSeparatedNames, commaSeparatedNames);
                LogUtil.consoleMessage("Unknown field name:"
                                       + commaSeparatedNames);
            }
            return null;
        }
        return data.getComponent(index);
    }



    /**
     * Set the ShouldUseAltitude property.
     *
     * @param value The new value for ShouldUseAltitude
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setShouldUseAltitude(boolean value)
            throws VisADException, RemoteException {
        shouldUseAltitude = value;
        if (stationData != null) {
            setStationData(stationData);
        }
        //FieldImpl tmp = stationData;
        //        setDisplayInactive();
        //setStationData(null);
        //setStationData(tmp);
        //        setDisplayActive();
    }

    /**
     * Get the ShouldUseAltitude property.
     *
     * @return The ShouldUseAltitude
     */
    public boolean getShouldUseAltitude() {
        return shouldUseAltitude;
    }

    /**
     * Set selected range with the range for select
     *
     * @param low  low select value
     * @param hi   hi select value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setSelectedRange(double low, double hi)
            throws VisADException, RemoteException {

        lowSelectedRange  = low;
        highSelectedRange = hi;
        if ((timeSelectControl != null) && hasSelectedRange()) {
            setTimeColorRange(low, hi);
            timeSelectControl.setRange(new double[] { low, hi });
        }

    }

    /**
     * Set the time color range
     *
     * @param low  low value
     * @param hi   high value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void setTimeColorRange(double low, double hi)
            throws VisADException, RemoteException {
        if (stationModel != null) {
            // check for glyphs that are colored by time
            boolean haveTimeColoredShape = false;
            for (Iterator iter = stationModel.iterator(); iter.hasNext(); ) {
                MetSymbol metSymbol = (MetSymbol) iter.next();
                if ( !metSymbol.getActive() || !metSymbol.shouldBeColored()) {
                    continue;
                }
                String colorParam = metSymbol.getColorTableParam();
                if ((colorParam != null) && (colorParam.length() > 0)) {
                    if (colorParam.equalsIgnoreCase(PointOb.PARAM_TIME)) {
                        haveTimeColoredShape = true;
                        metSymbol.setColorTableRange(new Range(low, hi));
                    }
                }
            }
            if (haveTimeColoredShape) {
                // see if we can make this more efficient
                if (stationData != null) {
                    setStationData(stationData);
                }
            }
        }
    }

    /**
     * Set the upper and lower limit of the range values associated
     * with a color table.
     *
     * @param low    the minimun value
     * @param hi     the maximum value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setRangeForSelect(double low, double hi)
            throws VisADException, RemoteException {

        minSelect = low;
        maxSelect = hi;
        if ((timeSelectMap != null) && hasSelectMinMax()) {
            timeSelectMap.setRange(low, hi);
        }
    }

    /**
     * Check to see if the range has been set for the select
     *
     * @return true if it has
     */
    private boolean hasSelectMinMax() {
        return ( !Double.isNaN(minSelect) && !Double.isNaN(maxSelect));
    }

    /**
     * Returns whether this Displayable has a valid range
     * (i.e., lowSelectedRange and highSelectedRange are both not NaN's
     *
     * @return true if range has been set
     */
    public boolean hasSelectedRange() {
        return ( !Double.isNaN(lowSelectedRange)
                 && !Double.isNaN(highSelectedRange));
    }

    /**
     *  Set the RotateShapes property.
     *
     *  @param value The new value for RotateShapes
     */
    public void setRotateShapes(boolean value) {
        rotateShapes = value;
    }

    /**
     *  Get the RotateShapes property.
     *
     *  @return The RotateShapes
     */
    public boolean getRotateShapes() {
        return rotateShapes;
    }



}
