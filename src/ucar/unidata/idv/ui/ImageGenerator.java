/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.ui;


import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import org.python.util.PythonInterpreter;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.grid.GridDataSource;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.IdvManager;
import ucar.unidata.idv.IdvPersistenceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.VectorGraphicsRenderer;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.colortable.ColorTableCanvas;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.unidata.view.geoloc.ViewpointInfo;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.GeoUtils;
import ucar.visad.display.Animation;
import ucar.visad.display.AnimationWidget;

import visad.CommonUnit;
import visad.MouseBehavior;
import visad.Real;
import visad.RealType;
import visad.Unit;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.MapProjection;

import visad.util.BaseRGBMap;
import visad.util.ColorPreview;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;


/**
 * Manages the user interface for the IDV
 *
 *
 * @author IDV development team
 */
public class ImageGenerator extends IdvManager {


    /** attr value */
    public static final String VALUE_TOP = "top";

    /** attr value */
    public static final String VALUE_BOTTOM = "bottom";

    /** attr value */
    public static final String VALUE_RIGHT = "right";

    /** attr value */
    public static final String VALUE_LEFT = "left";

    /** attr value */
    public static final String VALUE_ALL = "all";

    /** attr value */
    public static final String VALUE_NONE = "none";

    /** attr value */
    public static final String VALUE_FIRST = "first";

    /** attr value */
    public static final String VALUE_LAST = "last";

    /** macro property */
    public static final String PROP_LOOPINDEX = "loopindex";

    /** padded loop index */
    public static final String PROP_LOOPINDEX_PAD2 = "loopindex_pad2";

    /** padded loop index */
    public static final String PROP_LOOPINDEX_PAD3 = "loopindex_pad3";

    /** padded loop index */
    public static final String PROP_LOOPINDEX_PAD4 = "loopindex_pad4";




    /** macro property */
    public static final String PROP_VIEWINDEX = "viewindex";


    /** macro property */
    public static final String PROP_VIEWNAME = "viewname";

    /** macro property */
    public static final String PROP_IMAGEINDEX = "imageindex";


    /** macro property */
    public static final String PROP_IMAGEFILE = "imagefile";

    /** macro property */
    public static final String PROP_IMAGEPATH = "imagepath";


    /** file property */
    public static final String PROP_FILE = "file";

    /** filenosuffix property */
    public static final String PROP_FILENOSUFFIX = "filenosuffix";

    /** filetail property */
    public static final String PROP_FILETAIL = "filetail";

    /** filetailnosuffix property */
    public static final String PROP_FILETAILNOSUFFIX = "filetailnosuffix";

    /** fileprefix property */
    public static final String PROP_FILEPREFIX = "fileprefix";

    /** macro property */
    public static final String PROP_CONTENTS = "contents";

    /** macro property */
    public static final String PROP_ANIMATIONTIME = "animationtime";


    /** macro property */
    public static final String PROP_OFFSCREEN = "offscreen";



    /** Macro name */
    private static final String[] DATE_PROPS = {
        "G", "yy", "yyyy", "MM", "M", "MMM", "MMMMM", "HH", "H", "k", "kk",
        "D", "d", "dd", "K", "KK", "a", "mm", "ss", "s", "S", "EEE", "Z"
    };

    /** List of SimpleDateFormat objects. One for each DATE_PROPS. */
    private static List DATE_FORMATS;


    /** isl tag */
    public static final String TAG_FILESET = "fileset";


    /** view tag */
    public static final String TAG_VIEW = "view";

    /** isl tag */
    public static final String TAG_TEMPLATE = "template";


    /** isl tag */
    public static final String TAG_APPEND = "append";

    /** isl tag */
    public static final String TAG_SETFILES = "setfiles";


    /** isl tag */
    public static final String TAG_ISL = "isl";

    /** viewpoint tag */
    public static final String TAG_VIEWPOINT = "viewpoint";

    /** isl tag */
    public static final String TAG_PROPERTY = "property";

    /** isl tag */
    public static final String TAG_IMPORT = "import";

    /** isl tag */
    public static final String TAG_IMAGE = "image";

    /** isl tag */
    public static final String TAG_GROUP = "group";

    /** isl tag */
    public static final String TAG_PAUSE = "pause";

    /** isl tag */
    public static final String TAG_MOVIE = "movie";

    /** isl tag */
    public static final String TAG_BUNDLE = "bundle";

    /** isl tag */
    public static final String TAG_ELSE = "else";

    /** isl tag */
    public static final String TAG_THEN = "then";


    /** isl tag */
    public static final String TAG_COLORBAR = "colorbar";

    /** isl tag */
    public static final String TAG_CLIP = "clip";



    /** publish tag */
    public static final String TAG_PUBLISH = "publish";

    /** isl tag */
    public static final String TAG_DISPLAY = "display";

    /** datasource tag */
    public static final String TAG_DATASOURCE = "datasource";

    /** isl tag */
    public static final String TAG_MATTE = "matte";

    /** show tag */
    public static final String TAG_SHOW = "show";

    /** displaylist tag */
    public static final String TAG_DISPLAYLIST = "displaylist";

    /** isl tag */
    public static final String TAG_OUTPUT = "output";


    /** isl tag */
    public static final String TAG_OVERLAY = "overlay";


    /** isl tag */
    public static final String TAG_KML = "kml";

    /** kml colorbar tag */
    public static final String TAG_KML_COLORBAR = "kmlcolorbar";

    /** isl tag */
    public static final String TAG_KMZFILE = "kmzfile";

    /** isl tag */
    public static final String TAG_SPLIT = "split";

    /** isl tag */
    public static final String TAG_RESIZE = "resize";

    /** isl tag */
    public static final String TAG_THUMBNAIL = "thumbnail";

    /** isl tag */
    public static final String TAG_TRANSPARENT = "transparent";

    /** background transparent tag */
    public static final String TAG_BGTRANSPARENT = "backgroundtransparent";


    /** index attribute */
    public static final String ATTR_INDEX = "index";


    /** stride attribute */
    public static final String ATTR_STRIDE = "stride";

    /** x stride attribute */
    public static final String ATTR_STRIDEX = "stridex";

    /** y stride attribute */
    public static final String ATTR_STRIDEY = "stridey";

    /** z stride attribute */
    public static final String ATTR_STRIDEZ = "stridez";

    /** bounding box attribute */
    public static final String ATTR_BBOX = "bbox";

    /** from level attribute */
    public static final String ATTR_LEVEL_FROM = "levelfrom";

    /** to level attribute */
    public static final String ATTR_LEVEL_TO = "levelto";

    /** azimuth attribute */
    public static final String ATTR_AZIMUTH = "azimuth";

    /** tilt attribute */
    public static final String ATTR_TILT = "tilt";

    /** x aspect attribute */
    public static final String ATTR_ASPECTX = "aspectx";

    /** y aspect attribute */
    public static final String ATTR_ASPECTY = "aspecty";

    /** z aspect attribute */
    public static final String ATTR_ASPECTZ = "aspectz";


    /** x rotation attribute */
    public static final String ATTR_ROTX = "rotx";

    /** y rotation attribute */
    public static final String ATTR_ROTY = "roty";

    /** z rotation attribute */
    public static final String ATTR_ROTZ = "rotz";

    /** scale attribute */
    public static final String ATTR_SCALE = "scale";

    /** x translation attribute */
    public static final String ATTR_TRANSX = "transx";

    /** y translation attribute */
    public static final String ATTR_TRANSY = "transy";

    /** z translation attribute */
    public static final String ATTR_TRANSZ = "transz";


    /** suffix attribute */
    public static final String ATTR_SUFFIX = "suffix";

    /** showunit atttribute */
    public static final String ATTR_SHOWUNIT = "showunit";

    /** transparency attribute */
    public static final String ATTR_TRANSPARENCY = "transparency";

    /** top attribute */
    public static final String ATTR_TOP = "top";


    /** left space attribute */
    public static final String ATTR_SPACE_LEFT = "space_left";

    /** right space attribute */
    public static final String ATTR_SPACE_RIGHT = "space_right";

    /** top space attribute */
    public static final String ATTR_SPACE_TOP = "space_top";

    /** bottom space attribute */
    public static final String ATTR_SPACE_BOTTOM = "space_bottom";




    /** isl tag */
    public static final String TAG_WRITE = "write";



    /** isl tag */
    public static final String ATTR_ANCHOR = "anchor";


    /** isl attr */
    public static final String ATTR_FROM = "from";

    /** isl attr */
    public static final String ATTR_TO = "to";


    /** isl attribute */
    public static final String ATTR_GLOBAL = "global";

    /** isl attribute */
    public static final String ATTR_ONERROR = "onerror";

    /** isl attribute */
    public static final String ATTR_SORT = "sort";

    /** isl attribute */
    public static final String ATTR_SORTDIR = "sortdir";

    /** isl attribute */
    public static final String VALUE_TIME = "time";

    /** isl attribute */
    public static final String VALUE_ASCENDING = "ascending";

    /** isl attribute */
    public static final String VALUE_DESCENDING = "descending";


    /** isl attribute */
    public static final String ATTR_FIRST = "first";

    /** isl attribute */
    public static final String ATTR_LAST = "last";



    /** isl tag */
    public static final String ATTR_USEPROJECTION = "useprojection";

    /** isl tag */
    public static final String ATTR_EXPR = "expr";

    /** isl tag */
    public static final String ATTR_COPY = "copy";

    /** the count tag */
    public static final String ATTR_COUNT = "count";

    /** isl tag */
    public static final String ATTR_COLUMNS = "columns";

    /** isl attribute */
    public static final String ATTR_DATASOURCE = "datasource";


    /** isl attribute */
    public static final String ATTR_DESTINATION = "destination";

    /** isl attribute */
    public static final String ATTR_SERVER = "server";

    /** isl attribute */
    public static final String ATTR_PASSWORD = "password";

    /** isl attribute */
    public static final String ATTR_USER = "user";

    /** isl tag */
    public static final String ATTR_ROWS = "rows";

    /** isl tag */
    public static final String ATTR_CLASS = "class";

    /** isl tag */
    public static final String ATTR_ANGLE = "angle";

    /** isl tag */
    public static final String ATTR_WHERE = "where";

    /** isl tag */
    public static final String ATTR_BACKGROUND = "background";

    /** isl attribute */
    public static final String ATTR_BUNDLE = "bundle";

    /** isl tag */
    public static final String ATTR_SHOWLINES = "showlines";

    /** isl tag */
    public static final String ATTR_LINECOLOR = "linecolor";

    /** isl tag */
    public static final String ATTR_COLOR = "color";

    /** isl tag */
    public static final String ATTR_COMMAND = "command";

    /** isl tag */
    public static final String ATTR_FONTFACE = "fontface";

    /** isl attr */
    public static final String ATTR_FORMAT = "format";

    /** that latlonlabels tag */
    public static final String TAG_LATLONLABELS = "latlonlabels";

    /** that latvalues tag */
    public static final String ATTR_LAT_VALUES = "latvalues";

    /** that latlabels tag */
    public static final String ATTR_LAT_LABELS = "latlabels";

    /** that lonvalues tag */
    public static final String ATTR_LON_VALUES = "lonvalues";


    /** that lonlabels tag */
    public static final String ATTR_LON_LABELS = "lonlabels";

    /** the draw lon lines tag */
    public static final String ATTR_DRAWLONLINES = "drawlonlines";

    /** the draw lat lines tag */
    public static final String ATTR_DRAWLATLINES = "drawlatlines";

    /** dashes tag */
    public static final String ATTR_DASHES = "dashes";

    /** linewidth tag */
    public static final String ATTR_LINEWIDTH = "linewidth";

    /** _more_ */
    public static final String ATTR_LINEOFFSET_RIGHT = "lineoffsetright";

    /** _more_ */
    public static final String ATTR_LINEOFFSET_LEFT = "lineoffsetleft";

    /** _more_ */
    public static final String ATTR_LINEOFFSET_TOP = "lineoffsettop";

    /** _more_ */
    public static final String ATTR_LINEOFFSET_BOTTOM = "lineoffsetbottom";

    /** _more_ */
    public static final String ATTR_LABELBACKGROUND = "labelbackground";

    /** _more_ */
    public static final String ATTR_SHOWTOP = "showtop";

    /** _more_ */
    public static final String ATTR_SHOWBOTTOM = "showbottom";

    /** _more_ */
    public static final String ATTR_SHOWLEFT = "showleft";

    /** _more_ */
    public static final String ATTR_SHOWRIGHT = "showright";

    /** isl tag */
    public static final String ATTR_FONTSIZE = "fontsize";

    /** isl tag */
    public static final String ATTR_FRAMERATE = "framerate";

    /** isl tag for ending frame pause for animated gifs */
    public static final String ATTR_ENDFRAMEPAUSE = "endframepause";

    /** isl tag */
    public static final String ATTR_CAPTION = "caption";

    /** isl tag */
    public static final String ATTR_DEBUG = "debug";

    /** isl tag */
    public static final String ATTR_DEFAULT = "default";

    /** isl tag */
    public static final String ATTR_DISPLAY = "display";

    /** isl tag */
    public static final String ATTR_OFFSCREEN = "offscreen";

    /** isl tag */
    public static final String ATTR_TIMES = "times";

    /** isl tag */
    public static final String ATTR_ENSEMBLES = "ensembles";

    /** isl tag */
    public static final String ATTR_DIR = "dir";

    /** isl tag */
    public static final String ATTR_PATTERN = "pattern";


    /** isl attribute */
    public static final String ATTR_WAIT = "wait";

    /** isl tag */
    public static final String ATTR_PROPERTY = "property";

    /** isl tag */
    public static final String ATTR_QUALITY = "quality";

    /** isl tag */
    public static final String ATTR_LOOP = "loop";

    /** isl tag */
    public static final String ATTR_ENTRY = "entry";

    /** isl tag */
    public static final String ATTR_ID = "id";

    /** isl tag */
    public static final String ATTR_IMAGE = "image";

    /** isl tag */
    public static final String ATTR_INTERVAL = "interval";

    /** isl tag */
    public static final String ATTR_LEFT = "left";

    /** isl tag */
    public static final String ATTR_MESSAGE = "message";

    /** _more_ */
    public static final String ATTR_MATTEBG = "mattebg";

    /** isl tag */
    public static final String ATTR_NAME = "name";

    /** isl tag */
    public static final String ATTR_RIGHT = "right";

    /** isl tag */
    public static final String ATTR_TICKMARKS = "tickmarks";

    /** isl tag */
    public static final String ATTR_SPACE = "space";

    /** isl tag */
    public static final String ATTR_HSPACE = "hspace";

    /** isl tag */
    public static final String ATTR_VSPACE = "vspace";

    /** isl tag */
    public static final String ATTR_BOTTOM = "bottom";

    /** the valign attribute */
    public static final String ATTR_VALIGN = "valign";

    /** isl tag */
    public static final String ATTR_TEXT = "text";

    /** isl tag */
    public static final String ATTR_TEMPLATE = "template";

    /** isl tag */
    public static final String ATTR_TYPE = "type";

    /** isl tag */
    public static final String ATTR_EVERY = "every";

    /** isl tag */
    public static final String ATTR_VALUE = "value";

    /** isl tag */
    public static final String ATTR_VALUES = "values";

    /** isl tag */
    public static final String ATTR_ORIENTATION = "orientation";


    /** isl tag */
    public static final String ATTR_PARAM = "param";

    /** isl tag */
    public static final String ATTR_PLACE = "place";

    /** isl tag */
    public static final String ATTR_VIEW = "view";

    /** the view dir attribute */
    public static final String ATTR_VIEWDIR = "viewdir";

    /** isl tag */
    public static final String ATTR_URL = "url";

    /** isl tag */
    public static final String ATTR_FILE = "file";

    /** isl tag */
    public static final String ATTR_FROMFILE = "fromfile";

    /** isl tag */
    public static final String ATTR_NORTH = "north";

    /** isl tag */
    public static final String ATTR_SOUTH = "south";

    /** isl tag */
    public static final String ATTR_EAST = "east";

    /** isl tag */
    public static final String ATTR_WEST = "west";

    /** isl tag */
    public static final String ATTR_WIDTH = "width";

    /** isl tag */
    public static final String ATTR_HEIGHT = "height";

    /** isl tag */
    public static final String ATTR_SLEEP = "sleep";

    /** isl tag */
    public static final String ATTR_SECONDS = "seconds";

    /** isl tag */
    public static final String ATTR_MINUTES = "minutes";

    /** isl tag */
    public static final String ATTR_HOURS = "hours";

    /** isl tag */
    public static final String ATTR_CLEAR = "clear";

    /** isl tag */
    public static final String ATTR_CODE = "code";

    /** isl tag */
    public static final String ATTR_LAT = "lat";

    /** isl tag */
    public static final String ATTR_LON = "lon";

    /** isl attribute */
    public static final String ATTR_WHAT = "what";

    /** isl attribute */
    private static final String ATTR_SUFFIXFREQUENCY = "suffixfrequency";

    /** Show debug messages */
    private boolean debug = false;

    /** Stack of properties hashtables */
    private List propertiesStack = new ArrayList();

    /** Maps id to data source */
    private Hashtable idToDataSource = new Hashtable();

    /** The interpreter */
    private PythonInterpreter interpreter;

    /** Stack of active OutputInfo objects */
    private List outputStack = new ArrayList();

    /** When we are looping this gets set so we can use in as a macro value */
    private int currentLoopIndex = 0;

    /** Holds the procedure elements. Maps procedure name to element. */
    private Hashtable procs;

    /** Holds the tag name to method */
    private Hashtable methods = new Hashtable();

    /** current xml node we are processing */
    private Element currentNode;


    /** Keep around the last image captured */
    private Image lastImage;

    /** an object map */
    private Hashtable objectMap;



    /**
     * Create me with the IDV
     *
     * @param idv The IDV
     */
    public ImageGenerator(IntegratedDataViewer idv) {
        super(idv);
    }

    /**
     * Create me with the IDV and start processing files
     *
     * @param idv The IDV
     * @param scriptFiles List of isl files
     */
    public ImageGenerator(IntegratedDataViewer idv, List scriptFiles) {
        super(idv);
        processScriptFiles(scriptFiles);
    }


    /**
     * Process the list of isl files
     *
     * @param scriptFiles isl files
     */
    public void processScriptFiles(List scriptFiles) {
        for (int fileIdx = 0; fileIdx < scriptFiles.size(); fileIdx++) {
            String filename = (String) scriptFiles.get(fileIdx);
            if ( !processScriptFile(filename)) {
                return;
            }
        }

    }


    /**
     * Process the  isl files
     *
     * @param islFile file
     *
     * @return Was it successful
     */
    public synchronized boolean processScriptFile(String islFile) {
        return processScriptFile(islFile, new Hashtable());
    }


    /**
     * Process the script file
     *
     * @param islFile  the ISL file
     * @param properties optional properties
     *
     * @return true if successful
     */
    public synchronized boolean processScriptFile(String islFile,
            Hashtable properties) {
        procs           = new Hashtable();
        idToDataSource  = new Hashtable();
        propertiesStack = new ArrayList();
        pushProperties();

        if (islFile.endsWith(".jy") || islFile.endsWith(".py")) {
            try {
                String islPath = IOUtil.getFileRoot(islFile);
                putProperty("islpath", islPath);
                String            jythonCode = IOUtil.readContents(islFile);
                PythonInterpreter interp     = getInterpreter();
                if (getIdv().getJythonManager().getInError()) {
                    return false;
                }

                interp.exec(jythonCode);
                popProperties();
                return true;
            } catch (Exception exc) {
                exc.printStackTrace();
                return error("Error running jython script:" + islFile + "\n"
                             + exc);
            }

        }


        Element root = null;
        try {
            InputStream is = null;
            try {
                if (islFile.startsWith("xml:")) {
                    String xml = islFile.substring(4);
                    is      = new ByteArrayInputStream(xml.getBytes());
                    islFile = "Inline isl";
                } else if (islFile.startsWith("b64:")) {
                    is = new ByteArrayInputStream(
                        XmlUtil.decodeBase64(islFile.substring(4)));
                    islFile = "Inline base 64 encoded isl";
                } else {
                    is = IOUtil.getInputStream(islFile, getClass());
                }
            } catch (FileNotFoundException fnfe) {}
            catch (IOException ioe) {}
            if (is == null) {
                return error(
                    "Given script file does not exist or could not be read: "
                    + islFile);
            }
            root = XmlUtil.getRoot(is);
        } catch (Exception exc) {
            exc.printStackTrace();
            return error("Could not load script file:" + islFile + "\n"
                         + exc);
        }
        if (root == null) {
            return error("Could not load script file:" + islFile);
        }
        try {
            String islPath = IOUtil.getFileRoot(islFile);
            putProperty("islpath", islPath);
            objectMap = properties;
            processNode(root);
            popProperties();
        } catch (InvocationTargetException ite) {
            Throwable inner = ite.getTargetException();
            while (inner instanceof InvocationTargetException) {
                inner =
                    ((InvocationTargetException) inner).getTargetException();
            }
            return handleError(inner);
        } catch (Throwable exc) {
            return handleError(exc);
        } finally {
            objectMap = null;
        }
        return true;
    }

    /**
     * Handle the error
     *
     * @param exc The error
     *
     * @return Just return false
     */
    private boolean handleError(Throwable exc) {
        if ( !(exc instanceof IllegalStateException)
                && !(exc instanceof IllegalArgumentException)) {
            exc.printStackTrace();
        } else {
            exc.printStackTrace();
        }
        return error("An error occurred:" + exc);
    }

    /**
     * Find the inner most non InvocationTargetException exception
     *
     * @param ite The exception
     *
     * @return First non InvocationTargetException exception
     */
    private Throwable getInnerException(InvocationTargetException ite) {
        Throwable inner = ite.getTargetException();
        while (inner instanceof InvocationTargetException) {
            inner = ((InvocationTargetException) inner).getTargetException();
        }
        return inner;
    }


    /**
     * Process the node
     *
     * @param node The node
     * @return ok
     * @throws Throwable On badness
     */
    private boolean processNode(Element node) throws Throwable {
        String tagName = node.getTagName();
        //        System.err.println("tag:" + tagName);

        String methodName = "processTag"
                            + tagName.substring(0, 1).toUpperCase()
                            + tagName.substring(1).toLowerCase();

        //Look to see if this is a isl procedure
        Element procNode = (Element) procs.get(tagName);
        if (procNode != null) {
            return processTagCall(node, procNode);
        }


        Object tmp = methods.get(methodName);
        if (tmp == null) {
            try {
                tmp = getClass().getDeclaredMethod(methodName,
                        new Class[] { Element.class });
            } catch (Exception exc) {}
            if (tmp == null) {
                tmp = "no method";
            }
            methods.put(methodName, tmp);
        }

        if (tmp instanceof Method) {
            try {
                currentNode = node;
                Object result = ((Method) tmp).invoke(this,
                                    new Object[] { node });
                if (result.equals(Boolean.TRUE)) {
                    return true;
                }
            } catch (InvocationTargetException ite) {
                Throwable inner = getInnerException(ite);
                if (inner instanceof BadIslException) {
                    return error("Error handling ISL node:"
                                 + XmlUtil.toStringNoChildren(currentNode)
                                 + "\t" + inner.toString());
                }
                throw inner;
            }
            return false;
        }
        return error("Unknown tag:" + tagName);
    }


    /**
     * Recursively call processNode on each of the children elements
     *
     * @param node The parent node.
     *
     * @return Success.
     *
     * @throws Throwable On badness
     */
    private boolean processChildren(Element node) throws Throwable {
        NodeList elements = XmlUtil.getElements(node);
        for (int childIdx = 0; childIdx < elements.getLength(); childIdx++) {
            Element child = (Element) elements.item(childIdx);
            try {
                if ( !processNode(child)) {
                    return false;
                }
            } catch (Throwable thr) {
                String onerror = applyMacros(node, ATTR_ONERROR,
                                             (String) null);
                if (onerror == null) {
                    throw thr;
                }
                if (onerror.equals("ignore")) {}
                else {
                    System.err.println("Error occured");
                    thr.printStackTrace();
                }
            }
        }
        return true;
    }





    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagFtp(Element node) throws Throwable {
        String file        = applyMacros(node, ATTR_FILE);
        String server      = applyMacros(node, ATTR_SERVER);
        String destination = applyMacros(node, ATTR_DESTINATION);
        String user        = applyMacros(node, ATTR_USER, "anonymous");
        String password = applyMacros(node, ATTR_PASSWORD,
                                      "idvuser@unidata.ucar.edu");
        byte[] bytes = IOUtil.readBytes(IOUtil.getInputStream(file));
        ftpPut(server, user, password, destination, bytes);
        return true;
    }


    /**
     * Process the export tag. This allows one to export data from a display. It requires
     * a display identifier
     *
     * @param node isl xml node
     *
     * @return everything is cool
     *
     * @throws Throwable On badness
     */
    protected boolean processTagExport(Element node) throws Throwable {
        DisplayControlImpl display = findDisplayControl(node);
        if (display == null) {
            throw new IllegalArgumentException("Could not find display:"
                    + XmlUtil.toString(node));
        }
        String what     = applyMacros(node, ATTR_WHAT, (String) null);

        String filename = XmlUtil.getAttribute(node, ATTR_FILE);
        display.doExport(what, filename);
        return true;
    }


    /**
     * Process the tag trace
     *
     * @param node  the node
     *
     * @return true if processed
     *
     * @throws Throwable  on badness
     */
    protected boolean processTagTrace(Element node) throws Throwable {
        String pattern = applyMacros(node, ATTR_PATTERN);
        debug("turning trace on for:" + pattern);
        Trace.startTrace();
        Trace.clearOnly();
        Trace.addOnly(pattern);
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagRemovedisplays(Element node)
            throws Throwable {

        if (XmlUtil.hasAttribute(node, ATTR_DISPLAY)) {
            debug("Removing display");
            DisplayControlImpl display = findDisplayControl(node);
            if (display == null) {
                return error("Could not find display:"
                             + XmlUtil.toString(node));
            }
            display.doRemove();
            return true;
        }
        debug("Removing all displays");
        getIdv().removeAllDisplays(false);
        return true;
    }






    /**
     * Process the print cache tag
     *
     * @param node  the XML node
     *
     * @return  true if successful
     *
     * @throws Throwable  on badness
     */
    protected boolean processTagPrintcache(Element node) throws Throwable {
        visad.data.DataCacheManager.getCacheManager().printStats();
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagRemoveall(Element node) throws Throwable {
        debug("Removing all displays and data");
        getIdv().removeAllDisplays(false);
        getIdv().removeAllDataSources();
        idToDataSource = new Hashtable();
        return true;
    }



    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagSetfiles(Element node) throws Throwable {
        DataSource dataSource = findDataSource(node);
        if (dataSource == null) {
            return error("Could not find data source");
        }
        List files = new ArrayList();
        if (XmlUtil.hasAttribute(node, ATTR_FILE)) {
            files.add(applyMacros(node, ATTR_FILE));
        } else {
            List filesetFiles = findFiles(node);
            if (filesetFiles != null) {
                files.addAll(filesetFiles);
            }
        }
        if (files.size() == 0) {
            return error("Could not find files");
        }
        dataSource.setNewFiles(files);
        return true;
    }

    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagExists(Element node) throws Throwable {
        List    files  = findFiles(node);
        boolean exists = ((files != null) && (files.size() > 0));
        putProperty(applyMacros(node, ATTR_PROPERTY), (exists
                ? "1"
                : "0"));
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagAsk(Element node) throws Throwable {
        String  property = applyMacros(node, ATTR_PROPERTY);
        boolean result;
        if (getIdv().getArgsManager().getIsOffScreen()) {
            if ( !XmlUtil.hasAttribute(node, ATTR_DEFAULT)) {
                throw new IllegalStateException(
                    "Running in offscreen mode and the 'ask' node does not have a 'default' attribute");
            }
            result = applyMacros(node, ATTR_DEFAULT, true);
        } else {
            result = GuiUtils.showYesNoDialog(null,
                    applyMacros(node, ATTR_MESSAGE), "");
        }
        putProperty(property, (result
                               ? "1"
                               : "0"));
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagEcho(Element node) throws Throwable {
        String message = applyMacros(node, ATTR_MESSAGE, (String) null);
        if (message == null) {
            message = applyMacros(XmlUtil.getChildText(node));
        }
        System.out.println(message);
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagAsktocontinue(Element node) throws Throwable {
        String message = applyMacros(node, ATTR_MESSAGE, (String) null);
        if (message == null) {
            message = applyMacros(XmlUtil.getChildText(node));
        }
        return GuiUtils.askOkCancel("ISL", message);
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagGc(Element node) throws Throwable {
        Runtime.getRuntime().gc();
        return true;
    }



    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagBreak(Element node) throws Throwable {
        throw new MyBreakException();
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagContinue(Element node) throws Throwable {
        throw new MyContinueException();
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagReturn(Element node) throws Throwable {
        throw new MyReturnException();
    }



    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagProcedure(Element node) throws Throwable {
        procs.put(applyMacros(node, ATTR_NAME), node);
        return true;
    }

    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagMkdir(Element node) throws Throwable {
        IOUtil.makeDir(applyMacros(node, ATTR_FILE));
        return true;
    }

    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagStop(Element node) throws Throwable {
        return false;
    }


    /**
     * Get the property value
     *
     * @param node the XML node
     *
     * @return  the values
     *
     * @throws Throwable on badness
     */
    protected String[] getPropertyValue(Element node) throws Throwable {
        String name  = (String) applyMacros(node, ATTR_NAME);
        String value = null;
        if (XmlUtil.hasAttribute(node, ATTR_VALUE)) {
            value = (String) applyMacros(node, ATTR_VALUE);
        } else if (XmlUtil.hasAttribute(node, ATTR_FROMFILE)) {
            String filename = applyMacros(node, ATTR_FROMFILE);
            value = applyMacros(IOUtil.readContents(filename));
        } else {
            value = XmlUtil.getChildText(node);
            if ((value == null) || (value.trim().length() == 0)) {
                throw new IllegalArgumentException(
                    "No value in property tag: " + XmlUtil.toString(node));
            }
            value = applyMacros(value);
        }
        return new String[] { name, value };
    }

    /**
     * Process IDV property tag
     *
     * @param node  the XML node
     *
     * @return  true if successful
     *
     * @throws Throwable  on badness
     */
    protected boolean processTagIdvproperty(Element node) throws Throwable {
        String[] tuple = getPropertyValue(node);
        debug("setting idv property: " + tuple[0] + " =" + tuple[1]);
        getIdv().getStateManager().putProperty(applyMacros(tuple[0]),
                applyMacros(tuple[1]));
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagProperty(Element node) throws Throwable {
        String[] tuple = getPropertyValue(node);
        putProperty(applyMacros(tuple[0]), tuple[1],
                    applyMacros(node, ATTR_GLOBAL, false));
        return true;
    }




    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagMove(Element node) throws Throwable {
        List files = findFiles(node);
        if (files != null) {
            File dir = new File(applyMacros(node, ATTR_DIR));
            debug("moving files to: " + dir + " files=" + files);
            for (int i = 0; i < files.size(); i++) {
                IOUtil.moveFile(new File(files.get(i).toString()), dir);
            }
        }
        return true;
    }

    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagRename(Element node) throws Throwable {
        String from = applyMacros(node, ATTR_FROM);
        String to   = applyMacros(node, ATTR_TO);
        IOUtil.moveFile(new File(from), new File(to));
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagDelete(Element node) throws Throwable {
        List files = findFiles(node);
        if (files != null) {
            debug("deleting files:" + files);
            for (int i = 0; i < files.size(); i++) {
                ((File) files.get(i)).delete();
            }
        }
        return true;
    }

    /**
     * Handle the clear tag
     *
     * @param node node
     *
     * @return ok
     *
     * @throws Throwable On badness
     */
    protected boolean processTagClear(Element node) throws Throwable {
        String    name = applyMacros(node, ATTR_NAME);
        Hashtable ht   = (Hashtable) propertiesStack.get(0);
        ht.remove(name);
        return true;
    }

    /**
     * Handle the append tag
     *
     * @param node node
     *
     * @return ok
     *
     * @throws Throwable On badness
     */
    protected boolean processTagAppend(Element node) throws Throwable {
        String    name  = applyMacros(node, ATTR_NAME);
        Hashtable ht    = findTableFor(name);
        String    value = (String) ht.get(name);
        if (value == null) {
            value = "";
        }
        if (XmlUtil.hasAttribute(node, ATTR_VALUE)) {
            value = value + applyMacros(node, ATTR_VALUE);
        } else if (XmlUtil.hasAttribute(node, ATTR_FROMFILE)) {
            String filename = applyMacros(node, ATTR_FROMFILE);
            value = value + applyMacros(IOUtil.readContents(filename));
        } else {
            value = value + applyMacros(XmlUtil.getChildText(node)).trim();
        }
        ht.put(name, value);
        return true;
    }


    /**
     * Handle the append tag
     *
     * @param node node
     *
     * @return ok
     *
     * @throws Throwable On badness
     */
    protected boolean processTagIncrement(Element node) throws Throwable {
        String    name  = applyMacros(node, ATTR_NAME);
        Hashtable ht    = findTableFor(name);
        String    value = (String) ht.get(name);
        if (value == null) {
            value = "0";
        }
        String by = "1";
        if (XmlUtil.hasAttribute(node, ATTR_VALUE)) {
            by = applyMacros(node, ATTR_VALUE);
        }
        double num = new Double(value).doubleValue()
                     + new Double(by).doubleValue();
        ht.put(name, "" + num);
        return true;
    }

    /**
     * Handle the append tag
     *
     * @param node node
     *
     * @return ok
     *
     * @throws Throwable On badness
     */
    protected boolean processTagReplace(Element node) throws Throwable {
        String    name = applyMacros(node, ATTR_NAME);
        Hashtable ht   = findTableFor(name);
        ht.put(name, applyMacros(node, ATTR_VALUE));
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagCopy(Element node) throws Throwable {
        List files = findFiles(node);
        if (files != null) {
            File dir = new File(applyMacros(node, ATTR_DIR));
            IOUtil.makeDir(dir);
            if ( !dir.isDirectory()) {
                return error("Specified file:" + dir + " is not a directory");
            }
            debug("copying files to: " + dir + " files=" + files);
            for (int i = 0; i < files.size(); i++) {
                IOUtil.copyFile(new File(files.get(i).toString()), dir);
            }
        }
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagReload(Element node) throws Throwable {
        List dataSources = getIdv().getDataSources();
        for (int i = 0; i < dataSources.size(); i++) {
            DataSource dataSource = (DataSource) dataSources.get(i);
            dataSource.reloadData();
        }
        return true;
    }

    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagExec(Element node) throws Throwable {
        String command = applyMacros(node, ATTR_COMMAND);
        debug("Calling exec:" + command);
        Process process = Runtime.getRuntime().exec(command);
        //This seems to hang?
        process.waitFor();
        if (process.exitValue() != 0) {
            String result = IOUtil.readContents(process.getInputStream());
            System.err.println("Exec:\n\t" + command + "\nreturned:"
                               + process.exitValue() + "\n" + result);
        }
        return true;

    }

    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagJython(Element node) throws Throwable {
        String jythonFile = applyMacros(node, ATTR_FILE, (String) null);
        if (jythonFile != null) {
            InputStream is = IOUtil.getInputStream(jythonFile, getClass());
            if (is == null) {
                return error("Could not open jython file:" + jythonFile);
            } else {
                getInterpreter().execfile(is, jythonFile);
            }
        } else {
            String jython = applyMacros(node, ATTR_CODE, (String) null);
            if (jython == null) {
                jython = XmlUtil.getChildText(node);
            }
            if (jython != null) {
                getInterpreter().exec(jython);
            }
        }
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagFileset(Element node) throws Throwable {
        List files = findFiles(Misc.newList(node));
        pushProperties();
        for (int i = 0; i < files.size(); i++) {
            try {
                String filePath = files.get(i).toString();
                String tail     = IOUtil.getFileTail(filePath);
                putProperty(PROP_FILE, filePath);
                putProperty(PROP_FILEPREFIX, IOUtil.stripExtension(filePath));
                putProperty(PROP_FILENOSUFFIX,
                            IOUtil.stripExtension(filePath));
                putProperty(PROP_FILETAIL, tail);
                putProperty(PROP_FILETAILNOSUFFIX,
                            IOUtil.stripExtension(tail));
                if ( !processChildren(node)) {
                    return false;
                }
            } catch (MyBreakException be) {
                break;
            } catch (MyContinueException ce) {}
        }
        popProperties();
        return true;

    }



    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagImport(Element node) throws Throwable {
        Element parent  = (Element) node.getParentNode();
        String  file    = applyMacros(node, ATTR_FILE);
        Element root    = XmlUtil.findRoot(node);
        Element newRoot = XmlUtil.getRoot(file, getClass());
        newRoot = (Element) root.getOwnerDocument().importNode(newRoot, true);
        parent.insertBefore(newRoot, node);
        parent.removeChild(node);
        if ( !processNode(newRoot)) {
            return false;
        }
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagDatasource(Element node) throws Throwable {
        debug("Creating data source");

        DataSource dataSource = null;
        String     id         = applyMacros(node, ATTR_ID, (String) null);
        if ((id != null) && (objectMap != null)) {
            dataSource = (DataSource) objectMap.get(id);
        }


        if (dataSource == null) {
            Object dataObject = applyMacros(node, ATTR_URL, (String) null);
            if (dataObject == null) {
                dataObject = StringUtil.toString(findFiles(node));
            }
            String bundle = applyMacros(node, ATTR_BUNDLE, (String) null);
            String type   = applyMacros(node, ATTR_TYPE, (String) null);
            if ((bundle == null) && (dataObject == null)) {
                return error(
                    "datasource tag requires either a url, fileset or a bundle");
            }

            if (dataObject != null) {
                dataSource = getIdv().makeOneDataSource(dataObject, type,
                        null);
                if (dataSource == null) {
                    return error("Failed to create data source:" + dataObject
                                 + " " + type);
                }
            } else {
                try {
                    String bundleXml = IOUtil.readContents(bundle);
                    Object obj =
                        getIdv().getEncoderForRead().toObject(bundleXml);
                    if ( !(obj instanceof DataSource)) {
                        return error("datasource bundle is not a DataSource:"
                                     + obj.getClass().getName());
                    }
                    dataSource = (DataSource) obj;
                } catch (Exception exc) {
                    return error("Error loading data source bundle: "
                                 + bundle, exc);
                }
            }
        }


        if (XmlUtil.hasAttribute(node, ATTR_TIMES)) {
            List timesList =
                StringUtil.parseIntegerListString(applyMacros(node,
                    ATTR_TIMES, (String) null));
            dataSource.setDateTimeSelection(timesList);
        }

        if (XmlUtil.hasAttribute(node, ATTR_ENSEMBLES)) {
            List ensList =
                StringUtil.parseIntegerListString(applyMacros(node,
                    ATTR_ENSEMBLES, (String) null));
            if (dataSource instanceof GridDataSource) {
                ((GridDataSource) dataSource).setEnsembleSelection(ensList);
            }
        }

        processGeoSelectionTags(node, dataSource.getDataSelection());


        Hashtable properties = getProperties(node);
        dataSource.setObjectProperties(properties);
        if (id != null) {
            idToDataSource.put(id, dataSource);
        }

        NodeList elements = XmlUtil.getElements(node);
        for (int childIdx = 0; childIdx < elements.getLength(); childIdx++) {
            Element child = (Element) elements.item(childIdx);
            if (child.getTagName().equals(TAG_DISPLAY)) {
                if ( !processDisplayNode(child, dataSource)) {
                    return false;
                }
            }
        }
        //        getIdv().getVMManager().setDisplayMastersActive();
        updateViewManagers();

        return true;
    }

    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagJoin(Element node) throws Throwable {
        List files = findFiles(node);
        if (files != null) {
            List images = new ArrayList();
            int  cols   = applyMacros(node, ATTR_COLUMNS, 0);
            int  rows   = applyMacros(node, ATTR_ROWS, 0);
            if ((cols == 0) && (rows == 0)) {
                cols = 1;
            }
            if ((cols != 0) && (rows != 0)) {
                cols = 0;
            }
            int colNum = 0;
            int rowNum = 0;
            for (int i = 0; i < files.size(); i++) {
                Image theImage =
                    ImageUtils.readImage(files.get(i).toString());
                if (theImage == null) {
                    continue;
                }
                images.add(theImage);
            }
            if (images.size() > 0) {
                if (cols == 0) {
                    cols = images.size() / rows;
                } else {
                    rows = images.size() / cols;
                }

                int maxWidth  = 0;
                int maxHeight = 0;
                int colCnt    = 0;
                for (int i = 0; i < images.size(); i++) {
                    Image theImage = (Image) images.get(i);
                    int   width    = theImage.getWidth(null);
                    int   height   = theImage.getHeight(null);
                }
            }
        }
        return true;
    }

    /**
     * Process the view tag
     *
     * @param node the element
     *
     * @return  true if it was processed
     *
     * @throws Throwable  problems
     */
    protected boolean processTagView(Element node) throws Throwable {
        List   vms    = getViewManagers(node);

        String width  = applyMacros(node, ATTR_WIDTH, (String) null);
        String height = applyMacros(node, ATTR_HEIGHT, (String) null);
        if ((width != null) && (height != null)) {
            getIdv().getStateManager().setViewSize(
                new Dimension(
                    new Integer(width).intValue(),
                    new Integer(height).intValue()));
        }

        if (vms.size() == 0) {
            StringBuffer properties = new StringBuffer();
            List         nodes      = XmlUtil.findChildren(node,
                                          TAG_PROPERTY);
            for (int childIdx = 0; childIdx < nodes.size(); childIdx++) {
                Element child = (Element) nodes.get(childIdx);
                properties.append(applyMacros(child, ATTR_NAME) + "="
                                  + applyMacros(child, ATTR_VALUE) + ";");
            }
            vms.add(getIdv().getViewManager(ViewDescriptor.LASTACTIVE, false,
                                            properties.toString()));
            return true;
        }

        for (int i = 0; i < vms.size(); i++) {
            ViewManager vm    = (ViewManager) vms.get(i);
            List        nodes = XmlUtil.findChildren(node, TAG_PROPERTY);
            for (int childIdx = 0; childIdx < nodes.size(); childIdx++) {
                Element child = (Element) nodes.get(childIdx);
                vm.setProperty(applyMacros(child, ATTR_NAME),
                               applyMacros(child, ATTR_VALUE), false);
            }
        }
        return true;
    }


    /**
     * Handle the animation tag. The index attribute can either be a number or be "end"
     *
     * @param node  the node
     *
     * @return  true if successful
     *
     * @throws Throwable  problems
     */
    protected boolean processTagAnimation(Element node) throws Throwable {
        String  indexString = applyMacros(node, ATTR_INDEX, "0");
        int     index       = -1;
        boolean end         = indexString.equals("end");
        boolean step        = indexString.equals("step");
        if ( !end && !step) {
            index = new Integer(indexString).intValue();
        }
        for (ViewManager viewManager : getViewManagers(node)) {
            AnimationWidget animationWidget =
                viewManager.getAnimationWidget();
            if (animationWidget == null) {
                continue;
            }
            if (end) {
                animationWidget.gotoEnd();
            } else if (step) {
                animationWidget.stepForward();
            } else {
                animationWidget.gotoIndex(index);
            }
        }
        return true;
    }



    /**
     * Process the viewpoint tag
     *
     * @param node  the node
     *
     * @return  true if successful
     *
     * @throws Throwable  problems
     */
    protected boolean processTagViewpoint(Element node) throws Throwable {

        List vms = getViewManagers(node);
        if (vms.size() == 0) {
            debug("Could not find view managers processing:"
                  + XmlUtil.toString(node));
        }
        ViewpointInfo viewpointInfo = null;


        for (int i = 0; i < vms.size(); i++) {
            ViewManager vm = (ViewManager) vms.get(i);
            if (XmlUtil.hasAttribute(node, ATTR_VIEWDIR)) {
                vm.setView(XmlUtil.getAttribute(node, ATTR_VIEWDIR));
            }

            if (XmlUtil.hasAttribute(node, ATTR_AZIMUTH)
                    || XmlUtil.hasAttribute(node, ATTR_TILT)) {
                viewpointInfo = new ViewpointInfo(toDouble(node,
                        ATTR_AZIMUTH, 0.0), toDouble(node, ATTR_TILT, 0.0));
                if ( !(vm instanceof MapViewManager)) {
                    continue;
                }
                MapViewManager mvm = (MapViewManager) vm;
                mvm.setViewpointInfo(viewpointInfo);
            }

            if (XmlUtil.hasAttribute(node, ATTR_ASPECTX)
                    || XmlUtil.hasAttribute(node, ATTR_ASPECTY)
                    || XmlUtil.hasAttribute(node, ATTR_ASPECTZ)) {
                double[] a = vm.getMaster().getDisplayAspect();
                a = new double[] { toDouble(node, ATTR_ASPECTX, a[0]),
                                   toDouble(node, ATTR_ASPECTY, a[1]),
                                   toDouble(node, ATTR_ASPECTZ, a[2]) };
                vm.getMaster().setDisplayAspect(a);
                vm.setAspectRatio(a);
            }

            if (XmlUtil.hasAttribute(node, ATTR_ROTX)
                    || XmlUtil.hasAttribute(node, ATTR_ROTY)
                    || XmlUtil.hasAttribute(node, ATTR_ROTZ)
                    || XmlUtil.hasAttribute(node, ATTR_TRANSX)
                    || XmlUtil.hasAttribute(node, ATTR_TRANSY)
                    || XmlUtil.hasAttribute(node, ATTR_TRANSZ)
                    || XmlUtil.hasAttribute(node, ATTR_SCALE)) {
                double[]      a = vm.getMaster().getDisplayAspect();
                double[]      currentMatrix = vm.getDisplayMatrix();
                double[]      trans         = { 0.0, 0.0, 0.0 };
                double[]      rot           = { 0.0, 0.0, 0.0 };
                double[]      scale         = { 0.0, 0.0, 0.0 };

                MouseBehavior mb = vm.getMaster().getMouseBehavior();
                mb.instance_unmake_matrix(rot, scale, trans, currentMatrix);
                double scaleValue = applyMacros(node, ATTR_SCALE, 0.0);
                if (scaleValue != 0) {
                    double scaleX = scaleValue * a[0];
                    double scaleY = scaleValue * a[1];
                    double scaleZ = scaleValue * a[2];
                    double[] scaleMatrix = mb.make_matrix(0.0, 0.0, 0.0,
                                               scaleX / scale[0],
                                               scaleY / scale[1],
                                               scaleZ / scale[2], 0.0, 0.0,
                                               0.0);
                    currentMatrix = mb.multiply_matrix(scaleMatrix,
                            currentMatrix);
                    mb.instance_unmake_matrix(rot, scale, trans,
                            currentMatrix);
                }
                double[] tmp;
                double   dummy = 0.0;
                //TODO: the rotation maybe doesn't work
                if (XmlUtil.hasAttribute(node, ATTR_ROTX)) {
                    tmp = mb.make_matrix(applyMacros(node, ATTR_ROTX, dummy),
                                         0.0, 0.0, 1.0, 0.0, 0.0, 0.0);
                    currentMatrix = mb.multiply_matrix(tmp, currentMatrix);
                    mb.instance_unmake_matrix(rot, scale, trans,
                            currentMatrix);
                }
                if (XmlUtil.hasAttribute(node, ATTR_ROTY)) {
                    tmp = mb.make_matrix(0.0,
                                         applyMacros(node, ATTR_ROTY, dummy)
                                         - rot[1], 0.0, 1.0, 0.0, 0.0, 0.0);

                    currentMatrix = mb.multiply_matrix(tmp, currentMatrix);
                    mb.instance_unmake_matrix(rot, scale, trans,
                            currentMatrix);
                }
                if (XmlUtil.hasAttribute(node, ATTR_ROTZ)) {
                    tmp = mb.make_matrix(0.0, 0.0,
                                         applyMacros(node, ATTR_ROTZ, dummy)
                                         - rot[2], 1.0, 0.0, 0.0, 0.0);

                    currentMatrix = mb.multiply_matrix(tmp, currentMatrix);
                    mb.instance_unmake_matrix(rot, scale, trans,
                            currentMatrix);
                }

                if (XmlUtil.hasAttribute(node, ATTR_TRANSX)) {
                    tmp = mb.make_translate(applyMacros(node, ATTR_TRANSX,
                            dummy) - trans[0], 0.0, 0.0);
                    currentMatrix = mb.multiply_matrix(tmp, currentMatrix);
                    mb.instance_unmake_matrix(rot, scale, trans,
                            currentMatrix);
                }
                if (XmlUtil.hasAttribute(node, ATTR_TRANSY)) {
                    tmp = mb.make_translate(0.0,
                                            applyMacros(node, ATTR_TRANSY,
                                                dummy) - trans[1], 0.0);
                    currentMatrix = mb.multiply_matrix(tmp, currentMatrix);
                    mb.instance_unmake_matrix(rot, scale, trans,
                            currentMatrix);
                }

                if (XmlUtil.hasAttribute(node, ATTR_TRANSZ)) {
                    tmp = mb.make_translate(0.0, 0.0,
                                            applyMacros(node, ATTR_TRANSZ,
                                                dummy) - trans[2]);
                    currentMatrix = mb.multiply_matrix(tmp, currentMatrix);
                    mb.instance_unmake_matrix(rot, scale, trans,
                            currentMatrix);
                }


                vm.setDisplayMatrix(currentMatrix);

            }
        }
        return true;

    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagCenter(Element node) throws Throwable {
        List vms = getViewManagers(node);
        if (XmlUtil.hasAttribute(node, ATTR_LAT)) {
            getVMManager().center(
                ucar.visad.Util.makeEarthLocation(
                    toDouble(node, ATTR_LAT), toDouble(node, ATTR_LON)), vms);
            return true;
        }


        if (XmlUtil.hasAttribute(node, ATTR_NORTH)) {
            ProjectionRect projRect = new ProjectionRect(toDouble(node,
                                          ATTR_WEST), toDouble(node,
                                              ATTR_NORTH), toDouble(node,
                                                  ATTR_EAST), toDouble(node,
                                                      ATTR_SOUTH));
            getVMManager().center(projRect, vms);
            return true;
        }

        if (XmlUtil.hasAttribute(node, ATTR_DISPLAY)) {
            DisplayControlImpl display = findDisplayControl(node);
            if (display == null) {
                throw new IllegalArgumentException("Could not find display:"
                        + XmlUtil.toString(node));
            }
            if (XmlUtil.getAttribute(node, ATTR_USEPROJECTION, false)) {
                MapProjection mp = display.getDataProjection();
                if (mp != null) {
                    getVMManager().center(mp, vms);
                }
            } else {
                LatLonPoint llp = display.getDisplayCenter();
                if (llp != null) {
                    getVMManager().center(
                        ucar.visad.Util.makeEarthLocation(llp), vms);
                }
            }
            return true;
        }

        getVMManager().center(vms);
        return true;
    }


    /**
     * Find the data source that is identified by the given xml node
     *
     * @param node node
     *
     * @return The data source or null
     */
    private DataSource findDataSource(Element node) {
        String id = XmlUtil.getAttribute(node, ATTR_DATASOURCE);
        return findDataSource(id);
    }

    /**
     * Find the data source with the given id
     *
     * @param id the id we pass to datasource.identifiedByName
     *
     * @return The data source or null if none found
     */
    private DataSource findDataSource(String id) {
        List       dataSources = getIdv().getDataSources();
        DataSource dataSource  = (DataSource) idToDataSource.get(id);
        if (dataSource != null) {
            return dataSource;
        }
        for (int i = 0; i < dataSources.size(); i++) {
            dataSource = (DataSource) dataSources.get(i);
            if (dataSource.identifiedByName(id)) {
                return dataSource;
            }
        }
        return null;
    }




    /**
     * Find the display control that is identified by the given xml node
     *
     * @param node node
     *
     * @return The display control source or null
     */
    private DisplayControlImpl findDisplayControl(Element node) {
        String id = XmlUtil.getAttribute(node, ATTR_DISPLAY);
        return findDisplayControl(id);
    }


    /**
     * Find the display control identified by the given id
     *
     * @param id The id of the display control. This can be the id or it can be a 'class:class name'
     *
     * @return The display control or null
     */
    public DisplayControlImpl findDisplayControl(String id) {
        List controls = getIdv().getDisplayControls();
        return findDisplayControl(id, controls);
    }


    /**
     * Find the display control
     *
     * @param id  the control id
     * @param controls  the list of controls
     *
     * @return  the control or null
     */
    public DisplayControlImpl findDisplayControl(String id,
            List<DisplayControlImpl> controls) {
        for (int i = 0; i < controls.size(); i++) {
            DisplayControlImpl control = (DisplayControlImpl) controls.get(i);

            if (id.startsWith("class:")) {
                if (StringUtil.stringMatch(control.getClass().getName(),
                                           id.substring(6), true, true)) {
                    return control;
                }
            }
            if (control.getId() != null) {
                if (StringUtil.stringMatch(control.getId(), id, true, true)) {
                    return control;
                }
            }
        }
        return null;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagBundle(Element node) throws Throwable {

        List timesList = null;
        List ensList   = null;
        if (XmlUtil.hasAttribute(node, ATTR_TIMES)) {
            timesList = StringUtil.parseIntegerListString(applyMacros(node,
                    ATTR_TIMES, (String) null));
        }
        if (XmlUtil.hasAttribute(node, ATTR_ENSEMBLES)) {
            ensList = StringUtil.parseIntegerListString(applyMacros(node,
                    ATTR_ENSEMBLES, (String) null));
        }


        List nodes    = XmlUtil.findChildren(node, TAG_SETFILES);
        List ids      = new ArrayList();
        List fileList = new ArrayList();
        getPersistenceManager().clearFileMapping();
        for (int i = 0; i < nodes.size(); i++) {
            Element child       = (Element) nodes.get(i);
            String dataSourceId = XmlUtil.getAttribute(child,
                                      ATTR_DATASOURCE);
            ids.add(dataSourceId);
            List files = new ArrayList();
            if (XmlUtil.hasAttribute(child, ATTR_FILE)) {
                String file = applyMacros(child, ATTR_FILE);
                debug("For data source: " + dataSourceId + " Using file: "
                      + file);
                files.add(file);
            } else {
                List filesetFiles = findFiles(child);
                if (filesetFiles != null) {
                    debug("For data source: " + dataSourceId
                          + " Using file: " + filesetFiles);
                    files.addAll(filesetFiles);
                } else {
                    debug("For data source: " + dataSourceId
                          + " Could not find any files");
                }
            }
            fileList.add(files);
            debug("Adding a file override id=" + dataSourceId + " files="
                  + files);
        }
        if (ids.size() > 0) {
            getPersistenceManager().setFileMapping(ids, fileList);
        }


        String width  = applyMacros(node, ATTR_WIDTH, (String) null);
        String height = applyMacros(node, ATTR_HEIGHT, (String) null);
        if ((width != null) && (height != null)) {
            getIdv().getStateManager().setViewSize(
                new Dimension(
                    new Integer(width).intValue(),
                    new Integer(height).intValue()));
        }
        String  bundleFile = applyMacros(node, ATTR_FILE, (String) null);
        boolean doRemove   = applyMacros(node, ATTR_CLEAR, true);
        if (doRemove) {
            //            try {
            cleanup();
            //            } catch(Exception exc) {
            //                System.err.println ("Error cleanup");
            //                System.exit(1);
            //            }
        }
        getIdv().getStateManager().setAlwaysLoadBundlesSynchronously(true);
        Hashtable bundleProperties = new Hashtable();
        if (timesList != null) {
            bundleProperties.put(IdvPersistenceManager.PROP_TIMESLIST,
                                 timesList);
        }
        if (ensList != null) {
            bundleProperties.put(IdvPersistenceManager.PROP_ENSLIST, ensList);
        }


        if (bundleFile != null) {
            debug("Loading bundle: " + bundleFile);
            if (bundleFile.endsWith(".jnlp")) {
                String xml =
                    getPersistenceManager().extractBundleFromJnlp(bundleFile);
                getPersistenceManager().decodeXml(xml, false, bundleFile,
                        null, false, true, bundleProperties, true, false);
            } else if (getArgsManager().isZidvFile(bundleFile)) {
                Hashtable properties = new Hashtable();
                boolean   ask        = getStore().get(PREF_ZIDV_ASK, true);
                getStore().put(PREF_ZIDV_ASK, false);
                getPersistenceManager().decodeXmlFile(bundleFile, "", false,
                        false, properties);
                getStore().put(PREF_ZIDV_ASK, ask);
            } else {
                String xml = IOUtil.readContents(bundleFile);
                xml = applyMacros(xml, null, false);
                getPersistenceManager().decodeXml(xml, false, bundleFile,
                        null, false, true, bundleProperties, true, false);
                //                getPersistenceManager().decodeXmlFile(bundleFile, false,
                //                        timesList);
            }
        } else {
            String b64Bundle = XmlUtil.getChildText(node).trim();
            if (b64Bundle.length() == 0) {
                return error("Could not bundle");
            }
            String xml = new String(XmlUtil.decodeBase64(b64Bundle));
            getPersistenceManager().decodeXml(xml, false, "", null, false,
                    true, bundleProperties, true, false);

        }

        if (applyMacros(node, ATTR_WAIT, true)) {
            getIdv().getIdvUIManager().waitUntilDisplaysAreDone(
                getIdv().getIdvUIManager());
        }
        getPersistenceManager().clearFileMapping();
        Color c            = applyMacros(node, ATTR_COLOR, (Color) null);
        List  viewManagers = getVMManager().getViewManagers();
        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager viewManager = (ViewManager) viewManagers.get(i);
            if (c != null) {
                viewManager.setColors(null, c);
            }
            viewManager.updateDisplayList();
        }

        //One more pause for the display lists
        updateViewManagers();
        getIdv().getIdvUIManager().waitUntilDisplaysAreDone(
            getIdv().getIdvUIManager());
        return true;
    }

    /**
     * remove data and displays, etc
     */
    private void cleanup() {
        getIdv().removeAllDisplays(false);
        getIdv().removeAllDataSources();
        idToDataSource = new Hashtable();
        ucar.unidata.util.CacheManager.clearCache();

        //        getIdv().getIdvUIManager().disposeAllWindows();
        if (getIdv().getArgsManager().getIsOffScreen()) {
            getIdv().getVMManager().removeAllViewManagers(true);
        }
        getIdv().getIdvUIManager().clearWaitCursor();

        double totalMemory   = (double) Runtime.getRuntime().maxMemory();
        double highWaterMark = (double) Runtime.getRuntime().totalMemory();
        double freeMemory    = (double) Runtime.getRuntime().freeMemory();
        double usedMemory    = (highWaterMark - freeMemory);
        totalMemory   = totalMemory / 1000000.0;
        usedMemory    = usedMemory / 1000000.0;
        highWaterMark = highWaterMark / 1000000.0;

        /*            System.err.println(
                      "MEM:" + ((int) usedMemory) + "/" + ((int) highWaterMark)
                      + " vms:" + getIdv().getVMManager().getViewManagers().size());
        */

    }

    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagCall(Element node) throws Throwable {
        String  name     = applyMacros(node, ATTR_NAME);
        Element procNode = (Element) procs.get(name);
        return processTagCall(node, procNode);
    }

    /**
     * process the given node
     *
     * @param node Node to process
     * @param procNode The procedure node
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagCall(Element node, Element procNode)
            throws Throwable {
        if (procNode == null) {
            return error("Could not find procedure node for call:"
                         + XmlUtil.toString(node));
        }

        pushProperties();
        String cdata = XmlUtil.getChildText(node);
        if ((cdata != null) && (cdata.trim().length() > 0)) {
            putProperty("paramtext", cdata);
        } else {
            putProperty("paramtext", "");
        }

        NamedNodeMap procnnm = procNode.getAttributes();
        if (procnnm != null) {
            for (int i = 0; i < procnnm.getLength(); i++) {
                Attr attr = (Attr) procnnm.item(i);
                if ( !ATTR_NAME.equals(attr.getNodeName())) {
                    putProperty(attr.getNodeName(),
                                applyMacros(attr.getNodeValue()));
                }
            }
        }


        NamedNodeMap nnm = node.getAttributes();
        if (nnm != null) {
            for (int i = 0; i < nnm.getLength(); i++) {
                Attr attr = (Attr) nnm.item(i);
                if ( !ATTR_NAME.equals(attr.getNodeName())) {
                    putProperty(attr.getNodeName(),
                                applyMacros(attr.getNodeValue()));
                }
            }
        }
        try {
            if ( !processChildren(node)) {
                return false;
            }
            try {
                if ( !processChildren(procNode)) {
                    return false;
                }
            } catch (MyReturnException mre) {
                //noop
            }
        } catch (Throwable throwable) {
            popProperties();
            throw throwable;
        }
        popProperties();
        return true;
    }

    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagIf(Element node) throws Throwable {
        String expr = applyMacros(node, ATTR_EXPR, (String) null);
        if (expr == null) {
            expr = applyMacros(XmlUtil.getChildText(node));
        }
        if ((expr == null) || (expr.trim().length() == 0)) {
            return error("Could not find if expression");
        }
        expr = expr.trim();
        boolean result = getInterpreter().eval(expr).toString().equals("1");

        Element thenNode      = XmlUtil.findChild(node, TAG_THEN);
        Element elseNode      = XmlUtil.findChild(node, TAG_ELSE);
        Element statementNode = (result
                                 ? thenNode
                                 : elseNode);
        if (statementNode == null) {
            if (result && (thenNode == null) && (elseNode == null)) {
                statementNode = node;
            } else {
                return true;
            }
        }
        if (statementNode != null) {
            //            pushProperties();
            try {
                if ( !processChildren(statementNode)) {
                    return false;
                }
            } catch (Throwable throwable) {
                //                popProperties();
                throw throwable;
            }
            //            popProperties();
        }
        return true;
    }

    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagOutput(Element node) throws Throwable {
        if ( !XmlUtil.hasAttribute(node, ATTR_FILE)) {
            for (int i = 0; i < outputStack.size(); i++) {
                OutputInfo oi = (OutputInfo) outputStack.get(i);
                oi.process(node);
            }
            return true;
        }
        OutputInfo outputInfo = new OutputInfo(node);
        outputStack.add(outputInfo);
        pushProperties();
        try {
            if ( !processChildren(node)) {
                return false;
            }
        } catch (Throwable throwable) {
            popProperties();
            throw throwable;
        }
        popProperties();
        outputStack.remove(outputStack.size() - 1);
        outputInfo.write();
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagIsl(Element node) throws Throwable {
        debug = applyMacros(node, ATTR_DEBUG, false);
        boolean offScreen = applyMacros(node, ATTR_OFFSCREEN, true);
        //        System.err.println ("offscreen:" + offScreen);
        if ( !getIdv().getArgsManager().getIslInteractive()) {
            //            System.err.println ("setting offscreen:" + offScreen);
            getIdv().getArgsManager().setIsOffScreen(offScreen);
        }
        putProperty(PROP_OFFSCREEN,
                    (getIdv().getArgsManager().getIsOffScreen()
                     ? "1"
                     : "0"));

        //        System.err.println("setting offScreen " +         getIdv().getArgsManager().getIsOffScreen());

        return processTagGroup(node);
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagGroup(Element node) throws Throwable {
        pushProperties();
        int    loopTimes   = applyMacros(node, ATTR_LOOP, 1);
        String sleepString = applyMacros(node, ATTR_SLEEP, (String) null);
        long   sleepTime   = 0;
        if (sleepString != null) {
            sleepString = sleepString.trim();
            long   multiplier = 1000;
            String unit = StringUtil.findPattern(sleepString, "[0-9.]+(.*)$");

            if ((unit != null) && (unit.trim().length() > 0)) {
                sleepString = sleepString.substring(0,
                        sleepString.length() - unit.length());
                if (unit.equals("s")) {}
                else if (unit.equals("seconds")) {}
                else if (unit.equals("minutes")) {
                    multiplier = 60 * 1000;
                } else if (unit.equals("m")) {
                    multiplier = 60 * 1000;
                } else if (unit.equals("hours")) {
                    multiplier = 60 * 60 * 1000;
                } else if (unit.equals("h")) {
                    multiplier = 60 * 60 * 1000;
                } else {
                    return error("Unknown sleep time unit:" + unit);
                }
            }
            sleepTime = (long) (multiplier
                                * new Double(sleepString).doubleValue());
        }
        for (int i = 0; i < loopTimes; i++) {
            currentLoopIndex = i;
            try {
                if ( !processChildren(node)) {
                    return false;
                }
            } catch (MyBreakException be) {
                break;
            } catch (MyContinueException ce) {}
            if ((loopTimes > 1) && (sleepTime > 0)) {
                Misc.sleep(sleepTime);
            }
        }
        popProperties();

        return true;
    }

    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagForeach(Element node) throws Throwable {
        pushProperties();
        List         allValues   = new ArrayList();
        int          numElements = 0;
        int          cnt         = 1;
        NamedNodeMap attrs       = node.getAttributes();
        if (attrs == null) {
            return error("No values in foreach tag");
        }

        for (int i = 0; i < attrs.getLength(); i++) {
            Attr   attr   = (Attr) attrs.item(i);
            String var    = attr.getNodeName();
            String values = applyMacros(attr.getNodeValue());
            List   tokens = StringUtil.split(values, ",");
            if (allValues.size() == 0) {
                numElements = tokens.size();
            } else if (numElements != tokens.size()) {
                return error("Bad number of tokens in foreach argument:\n"
                             + var + "=" + values);
            }
            allValues.add(new Object[] { var, tokens });
            cnt++;
        }
        for (int tokIdx = 0; tokIdx < numElements; tokIdx++) {
            for (int valueIdx = 0; valueIdx < allValues.size(); valueIdx++) {
                Object[] tuple = (Object[]) allValues.get(valueIdx);
                putProperty(tuple[0], ((List) tuple[1]).get(tokIdx));
            }
            try {
                if ( !processChildren(node)) {
                    return false;
                }
            } catch (MyBreakException be) {
                break;
            } catch (MyContinueException ce) {}
        }
        popProperties();
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagMovie(Element node) throws Throwable {
        pushProperties();
        captureMovie(null, node);
        popProperties();
        return true;
    }

    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagHtml(Element node) throws Throwable {

        String html = null;
        if (XmlUtil.hasAttribute(node, ATTR_FROMFILE)) {
            html = IOUtil.readContents(applyMacros(node, ATTR_FROMFILE));
        } else {
            html = XmlUtil.getChildText(node);
        }
        html = applyMacros(html);
        int   width = XmlUtil.getAttribute(node, ATTR_WIDTH, -1);
        Image image = ImageUtils.renderHtml(html, width, null, null);
        image = processImage(ImageUtils.toBufferedImage(image),
                             XmlUtil.getAttribute(node, ATTR_FILE), node,
                             getAllProperties(), null, new Hashtable());

        //        writeImageToFile(image, XmlUtil.getAttribute(node, ATTR_FILE));
        return true;
    }

    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagPanel(Element node) throws Throwable {
        pushProperties();
        captureMovie(null, node);
        popProperties();
        return true;
    }


    /**
     * Parse the xml
     *
     * @param xml the xml
     *
     * @return the root
     *
     * @throws Exception On badness
     */
    private Element makeElement(String xml) throws Exception {
        return XmlUtil.getRoot(xml);
    }


    /**
     * Capture a movie and write it out. This is typically called by the jython scripting
     *
     * @param filename Movie filename
     * @param params xml parameters of the the form:  "task arg=val arg2=val; task2 arg3=val"
     *
     * @throws Exception On badness
     */
    public void writeMovie(String filename, String params) throws Exception {
        String isl = makeXmlFromString(params);

        String xml = "<movie file=\"" + filename + "\" imagesuffix=\".png\">"
                     + isl + "</movie>";
        captureMovie(applyMacros(filename), makeElement(xml));
    }



    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagImage(Element node) throws Throwable {
        captureImage(XmlUtil.getAttribute(node, ATTR_FILE), node);
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagWait(Element node) throws Throwable {
        File f = null;
        if (XmlUtil.hasAttribute(node, ATTR_FILE)) {
            f = new File(applyMacros(node, ATTR_FILE));
        }
        double seconds = applyMacros(node, ATTR_SECONDS, 60.0);
        if ((f != null) && f.isDirectory()) {
            String patternStr = applyMacros(applyMacros(node, ATTR_PATTERN,
                                    (String) null));
            IOUtil.wait(f, patternStr, seconds);
        } else {
            if (f != null) {
                IOUtil.wait(Misc.newList(f), seconds);
            } else {
                IOUtil.wait(findFiles(node), seconds);
            }
        }
        return true;
    }



    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     *
     * @throws Throwable On badness
     */
    protected boolean processTagPause(Element node) throws Throwable {
        if (XmlUtil.hasAttribute(node, ATTR_EVERY)) {
            Misc.pauseEvery((int) (60 * toDouble(node, ATTR_EVERY)));
            return true;
        }
        if (XmlUtil.hasAttribute(node, ATTR_SECONDS)) {
            Misc.sleep((long) (1000 * toDouble(node, ATTR_SECONDS)));
        } else if (XmlUtil.hasAttribute(node, ATTR_MINUTES)) {
            Misc.sleep((long) (60 * 1000 * toDouble(node, ATTR_MINUTES)));
        } else if (XmlUtil.hasAttribute(node, ATTR_HOURS)) {
            Misc.sleep((long) (60 * 60 * 1000 * toDouble(node, ATTR_HOURS)));
        } else {
            updateViewManagers();
            getIdv().getIdvUIManager().waitUntilDisplaysAreDone(
                getIdv().getIdvUIManager());
        }
        return true;

    }

    /**
     * Update the view managers
     */
    protected void updateViewManagers() {
        try {
            List viewManagers = getVMManager().getViewManagers();
            for (int i = 0; i < viewManagers.size(); i++) {
                ViewManager viewManager = (ViewManager) viewManagers.get(i);
                viewManager.updateDisplayIfNeeded();
            }
        } catch (Exception exc) {
            logException("Updating view manager", exc);
        }
    }

    /**
     * Process tag display properties
     *
     * @param node  the node
     *
     * @return true if successful
     */
    protected boolean processTagDisplayproperties(Element node) {
        DisplayControlImpl display = findDisplayControl(node);
        if (display == null) {
            throw new IllegalArgumentException("Could not find display:"
                    + XmlUtil.toString(node));
        }
        Hashtable properties = getProperties(node);
        display.applyProperties(properties);
        return true;
    }


    /**
     * process the given node
     *
     * @param node Node to process
     *
     * @return keep going
     */

    protected boolean processTagDisplay(Element node) {
        if ( !processDisplayNode(node, null)) {
            return false;
        }
        if (applyMacros(node, ATTR_WAIT, true)) {
            pause();
        }
        updateViewManagers();
        return true;
    }



    /**
     * Process the display node. If data source is not null then use that
     * to find data choices. If null then use all loaded data sources to find
     * data choice.
     *
     * @param node Node to process
     * @param dataSource The data source. May be null.
     *
     * @return keep going
     */
    private boolean processDisplayNode(Element node, DataSource dataSource) {

        //TODO:
        String     type       = applyMacros(node, ATTR_TYPE, (String) null);
        String     param      = applyMacros(node, ATTR_PARAM, (String) null);
        DataChoice dataChoice = null;
        debug("Creating display: " + type + " param:" + param);

        if ((dataSource == null)
                && XmlUtil.hasAttribute(node, ATTR_DATASOURCE)) {
            dataSource = findDataSource(node);
            if (dataSource == null) {
                return error("Failed to to find data source for display tag:"
                             + XmlUtil.toString(node));
            }
        }

        if (param != null) {
            if (dataSource != null) {
                dataChoice = dataSource.findDataChoice(param);
            } else {
                List dataSources = getIdv().getDataSources();
                for (int i = 0;
                        (i < dataSources.size()) && (dataChoice == null);
                        i++) {
                    dataSource = (DataSource) dataSources.get(i);
                    dataChoice = dataSource.findDataChoice(param);
                }
            }
            if (dataChoice == null) {
                return error("Failed to find parameter:" + param);
            }
        }
        List dataChoices = new ArrayList();
        if (dataChoice != null) {
            dataChoice = dataChoice.createClone();
            DataSelection dataSelection =
                new DataSelection(applyMacros(node, ATTR_LEVEL_FROM,
                    (String) null), applyMacros(node, ATTR_LEVEL_TO,
                        (String) null));


            processGeoSelectionTags(node, dataSelection);

            String timeString = applyMacros(node, ATTR_TIMES, (String) null);
            if (timeString != null) {
                List times = new ArrayList();
                for (String tok :
                        StringUtil.split(timeString, ",", true, true)) {
                    times.add(new Integer(tok));
                }
                dataSelection.setTimes(times);
            }
            dataChoice.setDataSelection(dataSelection);
            dataChoices.add(dataChoice);
        }

        if (type == null) {
            String bundleXml = null;
            if (XmlUtil.hasAttribute(node, ATTR_TEMPLATE)) {
                String filename = applyMacros(node, ATTR_TEMPLATE);
                try {
                    bundleXml = IOUtil.readContents(filename);
                } catch (IOException exc) {
                    return error("Could not find file: " + filename);
                }
            } else {
                Element templateNode = XmlUtil.findChild(node, TAG_TEMPLATE);
                if (templateNode != null) {
                    bundleXml = XmlUtil.getChildText(templateNode);
                }
            }
            if (bundleXml == null) {
                return error(
                    "<display> tag does not contain type attribute or template attribute/tag");
            }
            try {
                Object obj = getIdv().getEncoderForRead().toObject(bundleXml);

                if ( !(obj instanceof DisplayControl)) {
                    return error("display template is not a DisplayControl:"
                                 + obj.getClass().getName());
                }
                DisplayControl displayControl = (DisplayControl) obj;
                displayControl.initAfterUnPersistence(getIdv(),
                        getProperties(node), dataChoices);
                getIdv().addDisplayControl(displayControl);
            } catch (Exception exc) {
                return error("Creating display", exc);
            }
        } else {
            ControlDescriptor cd = getIdv().getControlDescriptor(type);
            if (cd == null) {
                return error("Failed to find display control:" + type);
            }
            Trace.call1("ImageGenerator making display");
            getIdv().doMakeControl(dataChoices, cd, getProperties(node),
                                   null, false);
            Trace.call2("ImageGenerator making display");
        }
        return true;

    }

    /**
     * _more_
     *
     * @param node _more_
     * @param dataSelection _more_
     *
     * @return _more_
     */
    private boolean processGeoSelectionTags(Element node,
                                            DataSelection dataSelection) {
        String strideString = applyMacros(node, ATTR_STRIDE, (String) null);
        if (strideString != null) {
            dataSelection.getGeoSelection(true).setXStride(applyMacros(node,
                    ATTR_STRIDE, 1));
            dataSelection.getGeoSelection(true).setYStride(applyMacros(node,
                    ATTR_STRIDE, 1));

        }


        String strideXString = applyMacros(node, ATTR_STRIDEX, (String) null);
        if (strideXString != null) {
            dataSelection.getGeoSelection(true).setXStride(applyMacros(node,
                    ATTR_STRIDEX, 1));

        }

        String strideYString = applyMacros(node, ATTR_STRIDEY, (String) null);
        if (strideYString != null) {
            dataSelection.getGeoSelection(true).setYStride(applyMacros(node,
                    ATTR_STRIDEY, 1));

        }
        String strideZString = applyMacros(node, ATTR_STRIDEZ, (String) null);
        if (strideZString != null) {
            dataSelection.getGeoSelection(true).setZStride(applyMacros(node,
                    ATTR_STRIDEX, 1));

        }

        String bboxString = applyMacros(node, ATTR_BBOX, (String) null);
        if (bboxString != null) {
            List toks = StringUtil.split(bboxString, ",", true, true);
            if (toks.size() != 4) {
                return error("Bad idv.data.geosubset property:" + bboxString);
            } else {
                GeoLocationInfo boundingBox =
                    new GeoLocationInfo(
                        Misc.decodeLatLon((String) toks.get(0)),
                        Misc.decodeLatLon((String) toks.get(1)),
                        Misc.decodeLatLon((String) toks.get(2)),
                        Misc.decodeLatLon((String) toks.get(3)));
                dataSelection.getGeoSelection(true).setBoundingBox(
                    boundingBox);
            }
        }
        return true;
    }


    /**
     * Process the property tag children of the given node
     *
     * @param node parent node that holds property tags
     *
     * @return properties
     */
    private Hashtable getProperties(Element node) {
        Hashtable properties = new Hashtable();
        List      nodes      = XmlUtil.findChildren(node, TAG_PROPERTY);
        for (int i = 0; i < nodes.size(); i++) {
            Element child = (Element) nodes.get(i);
            properties.put(applyMacros(child, ATTR_NAME),
                           applyMacros(child, ATTR_VALUE));
        }
        return properties;
    }


    /**
     * Utility to print a message and return false.
     *
     * @param msg message
     *
     * @return false
     */
    protected boolean error(String msg) {
        if ( !getIdv().getArgsManager().getIsOffScreen()) {
            LogUtil.userErrorMessage(msg);
        } else {
            System.err.println(msg);
        }
        return false;
    }



    /**
     * Utility to print a message and return false.
     *
     * @param msg message
     * @param exc exception
     *
     * @return false
     */
    protected boolean error(String msg, Exception exc) {
        if ( !getIdv().getArgsManager().getIsOffScreen()) {
            LogUtil.logException(msg, exc);
        } else {
            exc.printStackTrace();
            System.err.println(msg);
        }
        return false;
    }


    /**
     * Find all of the files that are defined by contained fileset nodes.
     *
     * @param parentNode Node to process
     *
     * @return List of files
     */
    private List findFiles(Element parentNode) {
        List resultFiles = null;
        List filesets    = XmlUtil.findChildren(parentNode, TAG_FILESET);
        if (filesets.size() > 0) {
            if (resultFiles == null) {
                resultFiles = new ArrayList();
            }
            resultFiles.addAll(findFiles(filesets));
        }
        if (resultFiles == null) {
            return null;
        }
        return resultFiles;
    }


    /**
     * Find all of the files that are defined by any fileset nodes in the nodes list..
     *
     *
     * @param nodes List of nodes
     *
     * @return List of files
     */
    private List findFiles(List nodes) {
        List files = new ArrayList();
        for (int i = 0; i < nodes.size(); i++) {
            Element node = (Element) nodes.get(i);
            if (node.getTagName().equals(TAG_FILESET)) {
                String filename = applyMacros(node, ATTR_FILE, (String) null);
                if (filename != null) {
                    files.add(new File(filename));
                    continue;
                }
                File dir = new File(applyMacros(node, ATTR_DIR, "."));
                String pattern = applyMacros(applyMacros(node, ATTR_PATTERN,
                                     (String) null));
                File[] allFiles = ((pattern == null)
                                   ? dir.listFiles()
                                   : dir.listFiles(
                                       (java.io
                                           .FileFilter) new PatternFileFilter(
                                               pattern)));
                if (allFiles == null) {
                    continue;
                }
                List tmpFiles = new ArrayList();
                for (int fileIdx = 0; fileIdx < allFiles.length; fileIdx++) {
                    if ( !allFiles[fileIdx].isDirectory()) {
                        if ( !files.contains(allFiles[fileIdx])) {
                            tmpFiles.add(allFiles[fileIdx]);
                        }
                    }
                }

                String sort = applyMacros(node, ATTR_SORT, (String) null);
                String sortDir = applyMacros(node, ATTR_SORTDIR,
                                             VALUE_ASCENDING);
                if (sort != null) {
                    if (sort.equals(VALUE_TIME)) {
                        if (sortDir.equals(VALUE_ASCENDING)) {
                            tmpFiles = Misc.toList(
                                IOUtil.sortFilesOnAge(
                                    IOUtil.toFiles(tmpFiles), false));
                        } else if (sortDir.equals(VALUE_DESCENDING)) {
                            tmpFiles = Misc.toList(
                                IOUtil.sortFilesOnAge(
                                    IOUtil.toFiles(tmpFiles), true));
                        } else {
                            System.err.println("unknown sort direction:"
                                    + sortDir);
                        }
                    } else {
                        System.err.println("unknown sort type:" + sort);
                    }
                }


                if (XmlUtil.hasAttribute(node, ATTR_FIRST)) {
                    int first = applyMacros(node, ATTR_FIRST, 0);
                    if (first < tmpFiles.size()) {
                        List tmp = new ArrayList();
                        for (int fileIdx = 0; fileIdx < first; fileIdx++) {
                            tmp.add(tmpFiles.get(fileIdx));
                        }
                        tmpFiles = tmp;

                    }
                } else if (XmlUtil.hasAttribute(node, ATTR_LAST)) {
                    int last = applyMacros(node, ATTR_LAST, 0);
                    if (last < tmpFiles.size()) {
                        List tmp = new ArrayList();
                        for (int fileIdx = tmpFiles.size() - 1; fileIdx >= 0;
                                fileIdx--) {
                            tmp.add(0, tmpFiles.get(fileIdx));
                            if (tmp.size() >= last) {
                                break;
                            }
                        }
                        tmpFiles = tmp;
                    }
                }
                files.addAll(tmpFiles);


            }
        }
        return files;
    }


    /**
     * Put the property in the current properties hashtable
     *
     * @param key key
     * @param value value
     */
    private void putProperty(Object key, Object value) {
        putProperty(key, value, false);
    }

    /**
     * Put the property in the current properties hashtable
     *
     * @param key key
     * @param value value
     * @param global If true put it in the base stack frame
     */
    private void putProperty(Object key, Object value, boolean global) {
        Hashtable properties = (global
                                ? (Hashtable) propertiesStack.get(0)
                                : getProperties());
        properties.put(key, value);
    }


    /**
     * Find the property table for the given key
     *
     * @param key The key
     *
     * @return The properties table. If none found then it returns the top of the stack
     */
    private Hashtable findTableFor(Object key) {
        for (int i = propertiesStack.size() - 1; i >= 0; i--) {
            Hashtable properties = (Hashtable) propertiesStack.get(i);
            if (properties.get(key) != null) {
                return properties;
            }
        }
        return getProperties();
    }

    /**
     * Find the table that contains the given property and replace it with the new value
     *
     * @param key key
     * @param value new value
     */
    private void replaceProperty(Object key, Object value) {
        findTableFor(key).put(key, value);
    }


    /**
     * Get the top most hashtable in the properties stack.
     *
     * @return Current properties hashtable.
     */
    private Hashtable getProperties() {
        if (propertiesStack.size() == 0) {
            return new Hashtable();
        }
        return (Hashtable) propertiesStack.get(propertiesStack.size() - 1);
    }


    /**
     * Add a Hashtable to the properties stack.
     *
     * @return The newly created hashtable.
     */
    private Hashtable pushProperties() {
        Hashtable properties = new Hashtable();
        propertiesStack.add(properties);
        return properties;
    }


    /**
     * Remove the top most hashtable in the properties stack
     */
    private void popProperties() {
        propertiesStack.remove(propertiesStack.size() - 1);
    }


    /**
     * utility to convert a string to a double. If the string ends with '%'
     * then return the percentage of the given base value
     *
     *
     * @param node Node to process
     * @param attr The attribute to look up
     * @param baseValue Used to handle '%'
     * @return The value
     */
    private double toDouble(Element node, String attr, double baseValue) {
        String s = applyMacros(node, attr);
        return toDouble(s, baseValue);
    }


    /**
     * utility to make a double. If the string begins with '%' then we take a percentage of the baseValue
     *
     * @param s string
     * @param baseValue Used if s is a percentage
     *
     * @return double value
     */
    private double toDouble(String s, double baseValue) {
        if (s.endsWith("%")) {
            double percent = Misc.toDouble(s.substring(0, s.length() - 1));
            return (percent / 100.0) * baseValue;
        }
        return new Double(s).doubleValue();
    }


    /**
     * Convert the attribute value of the given node to a double
     *
     * @param node Node to process
     * @param attr Attribute name
     *
     * @return double value
     */
    private double toDouble(Element node, String attr) {
        return Misc.toDouble(applyMacros(node, attr));
    }


    /**
     * Find the attribute  value of the given node. Apply the macros to it.
     *
     * @param node Node to process
     * @param attr Attribute name
     *
     * @return The value
     */
    public String applyMacros(Element node, String attr) {
        return applyMacros(XmlUtil.getAttribute(node, attr));
    }


    /**
     * If the attribute does not exist return the dflt. Else return the value.
     *
     * @param node Node to process
     * @param attr Attribute name
     * @param dflt The default value to use if the attribute does not exist
     *
     * @return The value
     */
    public String applyMacros(Element node, String attr, String dflt) {
        return applyMacros(XmlUtil.getAttribute(node, attr, dflt));
    }


    /**
     * If the attribute does not exist return the dflt. Else return the value.
     *
     * @param node Node to process
     * @param attr Attribute name
     * @param dflt The default value to use if the attribute does not exist
     *
     * @return The value
     */
    public int applyMacros(Element node, String attr, int dflt) {
        String value = XmlUtil.getAttribute(node, attr, (String) null);
        if (value == null) {
            return dflt;
        }
        return (int) Misc.toDouble(applyMacros(value));
    }

    /**
     * If the attribute does not exist return the dflt. Else return the value.
     *
     * @param node Node to process
     * @param attr Attribute name
     * @param dflt The default value to use if the attribute does not exist
     *
     * @return The value
     */
    public boolean applyMacros(Element node, String attr, boolean dflt) {
        String value = XmlUtil.getAttribute(node, attr, (String) null);
        if (value == null) {
            return dflt;
        }
        return new Boolean(applyMacros(value)).booleanValue();
    }




    /**
     * If the attribute does not exist return the dflt. Else return the value.
     *
     * @param node Node to process
     * @param attr Attribute name
     * @param dflt The default value to use if the attribute does not exist
     *
     * @return The value
     */
    public Color applyMacros(Element node, String attr, Color dflt) {
        String value = XmlUtil.getAttribute(node, attr, (String) null);
        if (value == null) {
            return dflt;
        }
        String result = applyMacros(value);
        if (result.equals("none")) {
            return null;
        }
        return GuiUtils.decodeColor(result, dflt);
    }


    /**
     * If the attribute does not exist return the dflt. Else return the value.
     *
     * @param node Node to process
     * @param attr Attribute name
     * @param dflt The default value to use if the attribute does not exist
     *
     * @return The value
     */
    public double applyMacros(Element node, String attr, double dflt) {
        String value = XmlUtil.getAttribute(node, attr, (String) null);
        if (value == null) {
            return dflt;
        }
        return Misc.toDouble(applyMacros(value));
    }



    /**
     * Do the macro substitution
     *
     * @param s The string
     *
     * @return The expanded string
     */
    public String applyMacros(String s) {
        return applyMacros(s, null);
    }

    /**
     * Merge all of the proeprties together
     *
     * @return The properties
     */
    private Hashtable getAllProperties() {
        Hashtable props = new Hashtable();
        for (int i = 0; i < propertiesStack.size(); i++) {
            Hashtable properties = (Hashtable) propertiesStack.get(i);
            props.putAll(properties);
        }
        return props;
    }


    /**
     * Do the macro substitution
     *
     * @param s The string
     * @param props Properties
     *
     * @return The expanded string
     */
    private String applyMacros(String s, Hashtable props) {
        return applyMacros(s, props, true);
    }

    /**
     * Do the macro substitution
     *
     * @param s The string
     * @param props Properties
     * @param doTime  process time macros
     *
     * @return The expanded string
     */
    private String applyMacros(String s, Hashtable props, boolean doTime) {
        if (s == null) {
            return null;
        }
        if (props == null) {
            props = new Hashtable();
        } else {
            Hashtable tmp = props;
            props = new Hashtable();
            props.putAll(tmp);
        }
        props.putAll(getAllProperties());

        putIndex(props, PROP_LOOPINDEX, currentLoopIndex);
        props.put(PROP_LOOPINDEX_PAD2,
                  StringUtil.padLeft("" + currentLoopIndex, 2, "0"));
        props.put(PROP_LOOPINDEX_PAD3,
                  StringUtil.padLeft("" + currentLoopIndex, 3, "0"));
        props.put(PROP_LOOPINDEX_PAD4,
                  StringUtil.padLeft("" + currentLoopIndex, 4, "0"));

        Date now = new Date(Misc.getCurrentTime());


        if (DATE_FORMATS == null) {
            TimeZone timeZone = TimeZone.getTimeZone("GMT");
            DATE_FORMATS = new ArrayList();
            for (int i = 0; i < DATE_PROPS.length; i++) {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_PROPS[i]);
                sdf.setTimeZone(timeZone);
                DATE_FORMATS.add(sdf);
            }
        }

        for (int i = 0; i < DATE_FORMATS.size(); i++) {
            SimpleDateFormat sdf = (SimpleDateFormat) DATE_FORMATS.get(i);
            props.put(DATE_PROPS[i], sdf.format(now));
        }


        props.put("memory", "" + Misc.usedMemory());


        /*
        if (s.indexOf("${anim:") >= 0) {
            now = getAnimationTime();
            if (now != null) {
                for (int i = 0; i < DATE_FORMATS.size(); i++) {
                    SimpleDateFormat sdf =
                        (SimpleDateFormat) DATE_FORMATS.get(i);
                    props.put("anim:" + DATE_PROPS[i], sdf.format(now));
                }

            }
            }*/


        TimeZone tz = getIdv().getPreferenceManager().getDefaultTimeZone();


        s = StringUtil.replaceDate(s, "now:", now, tz);

        if ((s.indexOf("anim:") >= 0) || (s.indexOf("time:") >= 0)) {
            Date animationTime = getAnimationTime();
            if (animationTime == null) {
                animationTime = now;
            }
            if (doTime) {
                s = StringUtil.replaceDate(s, "anim:", animationTime, tz);
                s = StringUtil.replaceDate(s, "time:", animationTime, tz);
                s = StringUtil.replaceDate(s, "now:", now, tz);
            }
        }
        s = StringUtil.applyMacros(s, props, false);
        //Now use the idv properties
        s = StringUtil.applyMacros(s, getStateManager().getProperties(),
                                   false);
        if (s.indexOf("${") >= 0) {
            throw new BadIslException("Undefined macro in: " + s);
        }

        if (s.startsWith("jython:")) {
            Object result = getInterpreter().eval(s.substring(7));
            s = result.toString();
        }

        if (s.startsWith("interp.")) {
            Object result = getInterpreter().eval(s);
            s = result.toString();
        }
        if (s.startsWith("islInterpreter.")) {
            Object result = getInterpreter().eval(s);
            s = result.toString();
        }
        s = s.replace("\\n", "\n");
        return s;
    }




    /**
     * Capture an image from the first active view managers
     *
     * @param filename The image filename
     */
    public void captureImage(String filename) {
        try {
            captureImage(filename, null);
        } catch (Throwable exc) {
            logException("Capturing image", exc);
        }
    }


    /**
     * Put the index
     *
     * @param props  the properties
     * @param name the name
     * @param v  the index
     */
    public void putIndex(Hashtable props, String name, int v) {
        props.put(name, new Integer(v));
        props.put(name + "_alpha", getLetter(v).toLowerCase());
        props.put(name + "_ALPHA", getLetter(v).toUpperCase());
        props.put(name + "_ROMAN", getRoman(v).toUpperCase());
        props.put(name + "_roman", getRoman(v).toLowerCase());


    }


    /**
     * Find all view managers that are identified by the given xml node. If the node
     * does not have a "view" attribute return all view managers. Else use the view
     * attribute to find the vms. The view can be class:class name or just a name. If a name can
     * also be a regular expression
     *
     * @param node node
     *
     * @return List of view managers
     */
    private List<ViewManager> getViewManagers(Element node) {
        List<ViewManager> viewManagers =
            (List<ViewManager>) getVMManager().getViewManagers();
        if ((node == null) || !XmlUtil.hasAttribute(node, ATTR_VIEW)) {
            return viewManagers;
        }

        List   goodOnes = new ArrayList();
        String viewId   = applyMacros(node, ATTR_VIEW);
        //        System.err.println ("viewManagers:" + viewManagers);
        if (viewId.startsWith("name:")) {
            viewId = viewId.substring(5);
        }

        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager viewManager = (ViewManager) viewManagers.get(i);
            if (viewId.startsWith("#")) {
                int viewIndex = new Integer(viewId.substring(1)).intValue();
                if (viewIndex == i) {
                    goodOnes.add(viewManager);
                    //                    System.err.println("\tskipping index");
                }
                continue;
            }
            if (viewId.startsWith("class:")) {
                if (StringUtil.stringMatch(viewManager.getClass().getName(),
                                           viewId.substring(6), true, true)) {
                    goodOnes.add(viewManager);
                }
                continue;
            }
            String name = viewManager.getName();
            if (name == null) {
                name = "";
            }
            if (StringUtil.stringMatch(name, viewId, true, true)) {
                goodOnes.add(viewManager);
            }
        }
        if (goodOnes.size() == 0) {
            warning("Unable to find any views with id:" + viewId);
        } else {
            //            System.err.println(viewId + " " + goodOnes);
        }
        return goodOnes;
    }


    /**
     * Wait until all displays are built
     */
    public void pause() {
        getIdv().waitUntilDisplaysAreDone();
    }


    /**
     * Toggle debug
     *
     * @param v debug
     */
    public void setDebug(boolean v) {
        debug = v;
    }


    /**
     * Evaluate the given isl
     *
     * @param isl The isl
     *
     * @return success
     *
     * @throws Throwable On badness
     */
    public boolean evaluateIsl(String isl) throws Throwable {
        isl = XmlUtil.tag(TAG_GROUP, "", isl);
        return processNode(XmlUtil.getRoot(isl));
    }



    /**
     * Load the given bundle file
     *
     * @param bundleFile The bundle
     * @param setFiles This is a list, which may be null, of datasource patterns and file names to change
     *
     * @throws Throwable     On badness
     */
    public void loadBundle(String bundleFile, List setFiles)
            throws Throwable {
        loadBundle(bundleFile, setFiles, -1, -1, "", true);
    }

    /**
     * Load the given bundle file, list of datasets, width and height
     *
     * @param bundleFile The bundle
     * @param setFiles This is a list, which may be null, of datasource patterns and file names to change
     * @param width The width of the display area to use
     * @param height The height of the display are to use
     *
     * @throws Throwable  an exception
     */
    public void loadBundle(String bundleFile, List setFiles, int width,
                           int height)
            throws Throwable {
        loadBundle(bundleFile, setFiles, width, height, "", true);
    }


    /**
     * Load the given bundle file, list of datasets, width and height
     *
     * @param bundleFile The bundle
     * @param setFiles This is a list, which may be null, of datasource patterns and file names to change
     * @param width The width of the display area to use
     * @param height The height of the display are to use
     * @param times A string of times to use from the bundle file
     * @param clear If false then do not clear out the data sources and displays (which is otherwise the default)
     *
     * @throws Throwable  an exception
     */
    public void loadBundle(String bundleFile, List setFiles, int width,
                           int height, String times, boolean clear)
            throws Throwable {

        StringBuffer extra = new StringBuffer();
        if (setFiles != null) {
            for (int i = 0; i < setFiles.size(); i += 2) {
                String datasource = (String) setFiles.get(i);
                String files      = (String) setFiles.get(i + 1);
                if ((datasource != null) && (files != null)) {
                    extra.append(XmlUtil.tag(TAG_SETFILES,
                                             XmlUtil.attrs("datasource",
                                                 datasource, "file", files)));
                }
                extra.append("\n");
            }
        }
        StringBuffer attrs = new StringBuffer();
        attrs.append(" ");
        attrs.append(ATTR_FILE + "=" + quote(bundleFile));
        attrs.append(" ");
        if ((width > 0) && (height > 0)) {
            attrs.append(" ");
            attrs.append(ATTR_WIDTH + "=" + quote("" + width));
            attrs.append(" ");
            attrs.append(ATTR_HEIGHT + "=" + quote("" + height));
            attrs.append(" ");
        }
        if ((times != null) && !times.isEmpty()) {
            attrs.append(" ");
            attrs.append(ATTR_TIMES + "=" + quote("" + times));
        }
        if ( !clear) {
            attrs.append(" ");
            attrs.append(ATTR_CLEAR + "=" + quote("false"));
        }

        String xml = "<bundle " + attrs + ">" + extra + "</bundle>";
        System.err.println(xml);
        processTagBundle(makeElement(xml));
    }

    /**
     * Write an Image to the specified file
     *
     * @param image Image to be written
     * @param file Name of output file (may use macros)
     *
     * @throws Exception On badness
     */
    public void writeImageToFile(Image image, String file) throws Exception {
        ImageUtils.writeImageToFile(image,
                                    applyMacros(getImageFileName(file)));
    }


    /**
     * Create XML from the input String
     *
     * @param s in the form:  "task arg=val arg2=val; task2 arg3=val"
     *
     * @return <task arg="val" arg2="val"/> <task2 arg3="val" />
     *
     */
    protected static String makeXmlFromString(String s) {
        if ((s == null) || (s.length() == 0)) {
            return "";
        }
        StringTokenizer st = new StringTokenizer(s, ";");
        StringBuffer    sb = new StringBuffer();

        while (st.hasMoreTokens()) {
            String          so  = st.nextToken();
            StringTokenizer sot = new StringTokenizer(so, "=");
            int             k   = sot.countTokens();

            for (int i = 0; i < k; i++) {

                StringTokenizer sbt =
                    new StringTokenizer(sot.nextToken().trim(), " ");

                if (i == 0) {
                    sb.append("<" + sbt.nextToken().trim());
                }

                int     n      = sbt.countTokens();
                boolean gotone = false;
                while (n > 1) {
                    if (gotone) {
                        sb.append(" ");
                    }
                    sb.append(sbt.nextToken().trim());
                    n      = n - 1;
                    gotone = true;
                }

                // now deal with the last value
                if (sbt.hasMoreTokens()) {
                    if (i != k - 1) {
                        if (gotone) {
                            sb.append("\" " + sbt.nextToken().trim() + "=\"");
                        } else {
                            sb.append(" " + sbt.nextToken().trim() + "=\"");
                        }
                    } else {
                        if (gotone) {
                            sb.append(" " + sbt.nextToken().trim() + "\"");
                        } else {
                            sb.append(sbt.nextToken().trim() + "\"");
                        }
                    }
                }
            }

            sb.append(" />");
        }

        return sb.toString();
    }

    /**
     * Quote a string
     *
     * @param s  the string
     *
     * @return  the quotated string
     */
    private static String quote(String s) {
        return "\"" + s + "\"";
    }

    /**
     * Get the image of the current display and write to file. Image
     * may be modified by the params given in the form:
     *    tag1 arg=val arg2=val2; tag2 arg=val
     * where 'tag' are ISL tags.
     *
     * @param filename Output filename (may be modified by macros)
     * @param params String of parameters
     * @param qual Quality (def=1.0)
     *
     *
     * @throws Exception On badness
     * @throws Throwable On badness
     */
    public void writeImage(String filename, String params, float qual)
            throws Exception, Throwable {
        String isl = makeXmlFromString(params);
        String xml = "<image file=\"" + filename + "\" quality=\"" + qual
                     + "\">" + isl + "</image>";
        captureImage(applyMacros(filename), makeElement(xml));
    }


    /**
     * Get the Image of the current display
     *
     * @return the Image
     *
     * @throws Exception On badness
     */
    public Image getImage() throws Exception {
        //        updateViewManagers();
        List viewManagers = getVMManager().getViewManagers();
        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager viewManager = (ViewManager) viewManagers.get(i);
            if ( !getIdv().getArgsManager().getIsOffScreen()) {
                IdvWindow window = viewManager.getDisplayWindow();
                if (window != null) {
                    window.setLocation(50, 50);
                    viewManager.toFront();
                }
            }
            return viewManager.getMaster().getImage(false);
        }
        return null;
    }


    /**
     * Capture the image
     *
     * @param filename file
     * @param scriptingNode THe node from the isl. Possibly null.
     *
     * @throws Throwable On badness
     */
    private void captureImage(String filename, Element scriptingNode)
            throws Throwable {
        Hashtable imageProperties = new Hashtable();

        //See if we're in test mode
        if ((scriptingNode != null)
                && XmlUtil.hasAttribute(scriptingNode, "test")) {
            BufferedImage tmpImage =
                new BufferedImage(applyMacros(scriptingNode, ATTR_WIDTH,
                    300), applyMacros(scriptingNode, ATTR_HEIGHT, 300),
                          BufferedImage.TYPE_INT_RGB);
            String loopFilename = applyMacros(filename);
            lastImage = processImage((BufferedImage) tmpImage, loopFilename,
                                     scriptingNode, getAllProperties(), null,
                                     imageProperties);
            return;
        }


        List<ViewManager> viewManagers = null;
        if ((scriptingNode != null)
                && XmlUtil.hasAttribute(scriptingNode, ATTR_DISPLAY)) {
            DisplayControlImpl display = findDisplayControl(scriptingNode);
            if (display == null) {
                throw new IllegalArgumentException("Could not find display:"
                        + XmlUtil.toString(scriptingNode));
            }
            String loopFilename = applyMacros(filename);
            String what = applyMacros(scriptingNode, ATTR_WHAT,
                                      (String) null);

            ViewManager viewManager = display.getViewManagerForCapture(what);
            if (viewManager != null) {
                viewManager.updateDisplayIfNeeded();
                viewManagers = (List<ViewManager>) Misc.newList(viewManager);
            } else {
                lastImage = display.getImage(what);
                lastImage = processImage((BufferedImage) lastImage,
                                         loopFilename, scriptingNode,
                                         getAllProperties(), null,
                                         imageProperties);
                return;
            }
        }

        if (viewManagers == null) {
            viewManagers = (List<ViewManager>) getViewManagers(scriptingNode);
        }

        if (viewManagers.size() == 0) {
            debug("No views to capture");
        }
        pushProperties();
        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager viewManager = (ViewManager) viewManagers.get(i);
            putIndex(getProperties(), PROP_VIEWINDEX, i);
            String name = viewManager.getName();
            if (name == null) {
                name = "view" + i;
            }
            getProperties().put(PROP_VIEWNAME, name);
            if ( !getIdv().getArgsManager().getIsOffScreen()) {
                IdvWindow window = viewManager.getDisplayWindow();
                if (window != null) {
                    window.setLocation(50, 50);
                    viewManager.toFront();
                    //                    Misc.sleep(100);
                }
            }
            String loopFilename = applyMacros(filename);
            if (scriptingNode == null) {
                File imageFile = null;
                if (loopFilename != null) {
                    imageFile = new File(getImageFileName(loopFilename));
                }
                viewManager.writeImage(imageFile, true, false);
            } else if ((loopFilename != null)
                       && ViewManager.isVectorGraphicsFile(loopFilename)) {
                VectorGraphicsRenderer vectorRenderer =
                    new VectorGraphicsRenderer(viewManager);
                vectorRenderer.renderTo(loopFilename);
            } else {
                getIdv().getIdvUIManager().waitUntilDisplaysAreDone(
                    getIdv().getIdvUIManager(), 0);
                lastImage       = viewManager.getMaster().getImage(false);
                imageProperties = new Hashtable();
                lastImage = processImage((BufferedImage) lastImage,
                                         loopFilename, scriptingNode,
                                         getAllProperties(), viewManager,
                                         imageProperties);
            }
        }
        popProperties();
    }


    /**
     * Resize the image
     *
     * @param image The image
     * @param node Node to process. This may contain a width or a height attribute.
     *
     * @return The resized image
     */
    protected Image resize(Image image, Element node) {
        int imageWidth  = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        int width       = -1;
        int height      = -1;
        if (XmlUtil.hasAttribute(node, ATTR_WIDTH)) {
            width = (int) toDouble(node, ATTR_WIDTH, imageWidth);
        }
        if (XmlUtil.hasAttribute(node, ATTR_HEIGHT)) {
            height = (int) toDouble(node, ATTR_HEIGHT, imageWidth);
        }
        if ((width == -1) && (height == -1)) {
            return image;
        }

        return image.getScaledInstance(width, height,
                                       Image.SCALE_AREA_AVERAGING);
    }




    /**
     * Resize the image
     *
     * @param image The image
     * @param widthStr width of desired image (pixels)
     * @param heightStr height of desired image (pixels)
     *
     * @return The resized image
     */
    public BufferedImage resizeImage(BufferedImage image, String widthStr,
                                     String heightStr) {
        int imageWidth  = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        int width       = -1;
        int height      = -1;
        if ( !widthStr.equals("-1")) {
            width = (int) toDouble(widthStr, imageWidth);
        }
        if ( !heightStr.equals("-1")) {
            height = (int) toDouble(heightStr, imageHeight);
        }
        if ((width == -1) && (height == -1)) {
            return image;
        }

        BufferedImage resizedImage =
            ImageUtils.toBufferedImage(image.getScaledInstance(width, height,
                Image.SCALE_AREA_AVERAGING), BufferedImage.TYPE_INT_RGB);
        return resizedImage;

    }



    /**
     * Matte the image
     *
     * @param image The image
     * @param bgString color for the matte ("red", "green", etc)
     * @param top number of lines for the top (north) matte
     * @param left number of pixels for the left (west) matte
     * @param bottom number of lines for the bottom (south) matte
     * @param right number of pixels for the right (east) matte
     *
     * @return The matte'd image
     */
    public BufferedImage matteImage(BufferedImage image, String bgString,
                                    int top, int left, int bottom,
                                    int right) {
        Color bg = GuiUtils.decodeColor(bgString, (Color) null);
        return ImageUtils.matte(image, top, bottom, left, right, bg);
    }



    /**
     * Process the image
     *
     * @param image The image
     * @param filename File to write the image to
     * @param node Node to process
     * @param props Extra properties
     * @param viewManager The viewmanager this image came from
     * @param imageProps  the image properties
     *
     *
     * @return The processed image
     * @throws Throwable On badness
     */
    protected BufferedImage processImage(BufferedImage image,
                                         String filename, Element node,
                                         Hashtable props,
                                         ViewManager viewManager,
                                         Hashtable imageProps)
            throws Throwable {

        if (node == null) {
            return image;
        }

        if (props == null) {
            props = new Hashtable();
        }
        if (viewManager != null) {
            Animation animation = viewManager.getAnimation();
            props.put(PROP_ANIMATIONTIME, "");
            if (animation != null) {
                if (animation.getAniValue() != null) {
                    props.put(PROP_ANIMATIONTIME, animation.getAniValue());
                }
            }
        }
        getProperties().putAll(props);

        NodeList  elements       = XmlUtil.getElements(node);
        Hashtable seenColorTable = new Hashtable();
        for (int childIdx = 0; childIdx < elements.getLength(); childIdx++) {
            boolean       shouldIterateChildren = true;
            BufferedImage newImage              = null;
            int           imageWidth            = image.getWidth(null);
            int           imageHeight           = image.getHeight(null);
            Element       child = (Element) elements.item(childIdx);
            String        tagName               = child.getTagName();

            if (tagName.equals(TAG_RESIZE)) {
                newImage = ImageUtils.toBufferedImage(resize(image, child));
            } else if (tagName.equals(TAG_FILESET)) {
                //ignore
            } else if (tagName.equals(TAG_OUTPUT)) {
                processTagOutput(child);
            } else if (tagName.equals(TAG_DISPLAYLIST)) {
                if (viewManager != null) {
                    newImage = ImageUtils.toBufferedImage(image, true);
                    Graphics g = newImage.getGraphics();
                    String valign = applyMacros(child, ATTR_VALIGN,
                                        VALUE_BOTTOM);
                    Font font = getFont(child);
                    if (XmlUtil.hasAttribute(child, ATTR_MATTEBG)) {
                        int height =
                            viewManager.paintDisplayList((Graphics2D) g,
                                null, imageWidth, imageHeight,
                                valign.equals(VALUE_BOTTOM), null, font);

                        int top    = (valign.equals(VALUE_TOP)
                                      ? height
                                      : 0);
                        int bottom = (valign.equals(VALUE_BOTTOM)
                                      ? height
                                      : 0);
                        newImage = ImageUtils.matte(image, top, bottom, 0, 0,
                                applyMacros(child, ATTR_MATTEBG,
                                            Color.white));
                        g           = newImage.getGraphics();
                        imageHeight += height;
                    }

                    Color c = applyMacros(child, ATTR_COLOR, (Color) null);
                    viewManager.paintDisplayList((Graphics2D) g, null,
                            imageWidth, imageHeight,
                            valign.equals(VALUE_BOTTOM), c, font);
                }
            } else if (tagName.equals(TAG_COLORBAR)
                       || tagName.equals(TAG_KML_COLORBAR)) {
                // only do one colorbar if we are writing to kml
                Integer index = (Integer) props.get(PROP_IMAGEINDEX);
                if ((index != null) && (index.intValue() > 0)
                        && tagName.equals(TAG_KML_COLORBAR)) {
                    continue;
                }

                boolean showLines = applyMacros(child, ATTR_SHOWLINES, false);

                List<DisplayControlImpl> controls =
                    (List<DisplayControlImpl>) ((viewManager != null)
                        ? viewManager.getControls()
                        : new ArrayList());

                if (XmlUtil.hasAttribute(child, ATTR_DISPLAY)) {
                    DisplayControlImpl display = ((controls.size() > 0)
                            ? findDisplayControl(XmlUtil.getAttribute(child,
                                ATTR_DISPLAY), controls)
                            : findDisplayControl(child));
                    if (display == null) {
                        error("Could not find display:"
                              + XmlUtil.toString(node));
                        return null;
                    }
                    controls = Misc.newList(display);
                }

                int    width    = applyMacros(child, ATTR_WIDTH, 150);
                int    height   = applyMacros(child, ATTR_HEIGHT, 20);
                int    ticks    = applyMacros(child, ATTR_TICKMARKS, 0);
                double interval = applyMacros(child, ATTR_INTERVAL, -1.0);
                String valuesStr = applyMacros(child, ATTR_VALUES,
                                       (String) null);
                Color c = applyMacros(child, ATTR_COLOR, Color.black);

                Color lineColor = applyMacros(child, ATTR_LINECOLOR, c);

                Rectangle imageRect = new Rectangle(0, 0, imageWidth,
                                          imageHeight);

                Point pp = ImageUtils.parsePoint(applyMacros(child,
                               ATTR_PLACE, "ll,10,-10"), imageRect);
                Point ap = ImageUtils.parsePoint(applyMacros(child,
                               ATTR_ANCHOR, "ll"), new Rectangle(0, 0, width,
                                   height));

                String orientation = applyMacros(child, ATTR_ORIENTATION,
                                         VALUE_BOTTOM);
                boolean vertical = orientation.equals(VALUE_RIGHT)
                                   || orientation.equals(VALUE_LEFT);
                int     baseY       = pp.y - ap.y + (vertical
                        ? 0
                        : height);
                int     baseX       = pp.x - ap.x;

                List    colorTables = new ArrayList();
                List    ranges      = new ArrayList();
                List    units       = new ArrayList();

                boolean forKml      = tagName.equals(TAG_KML_COLORBAR);

                for (int i = 0; i < controls.size(); i++) {
                    DisplayControlImpl control =
                        (DisplayControlImpl) controls.get(i);
                    ColorTable colorTable = control.getColorTable();
                    if (colorTable == null) {
                        continue;
                    }
                    Range range = control.getRangeForColorTable();
                    //only do unique color tables
                    Object[] key = { colorTable, range };
                    if (seenColorTable.get(key) != null) {
                        continue;
                    }
                    seenColorTable.put(key, key);
                    colorTables.add(colorTable);
                    ranges.add(range);
                    units.add(control.getDisplayUnit());
                }

                for (int i = 0; i < colorTables.size(); i++) {
                    ColorTable colorTable = (ColorTable) colorTables.get(i);
                    Range      range      = (Range) ranges.get(i);
                    Unit       unit       = (Unit) units.get(i);
                    Image      imageToDrawIn;
                    if (forKml) {
                        if (vertical) {
                            baseX = 0;
                            baseY = 0;
                        } else {
                            baseX = 0;
                            baseY = height;
                        }
                        int space = applyMacros(child, ATTR_SPACE, (vertical
                                ? width
                                : height));
                        imageToDrawIn = new BufferedImage(width + (vertical
                                ? space
                                : 0), height + (vertical
                                ? 0
                                : space), BufferedImage.TYPE_INT_RGB);
                    } else {
                        imageToDrawIn = newImage =
                            ImageUtils.toBufferedImage(image);
                    }
                    Graphics g = imageToDrawIn.getGraphics();
                    if (forKml) {
                        Color bgColor = applyMacros(child, ATTR_BACKGROUND,
                                            Color.white);
                        g.setColor(bgColor);
                        g.fillRect(0, 0, imageToDrawIn.getWidth(null),
                                   imageToDrawIn.getHeight(null));
                    }
                    ColorPreview preview =
                        new ColorPreview(
                            new BaseRGBMap(colorTable.getNonAlphaTable()),
                            (vertical
                             ? width
                             : height));
                    if (vertical) {
                        preview.setSize(new Dimension(height, width));
                    } else {
                        preview.setSize(new Dimension(width, height));
                    }
                    Image previewImage = GuiUtils.getImage(preview);
                    previewImage = ColorTableCanvas.getImage(colorTable,
                            (vertical
                             ? height
                             : width), (vertical
                                        ? width
                                        : height));


                    if (vertical) {
                        BufferedImage tmpImage =
                            new BufferedImage(width, height,
                                BufferedImage.TYPE_INT_RGB);

                        BufferedImage tmpImagexxx =
                            new BufferedImage(500, 500,
                                BufferedImage.TYPE_INT_RGB);
                        Graphics2D tmpG = (Graphics2D) tmpImage.getGraphics();
                        tmpG.setColor(Color.red);
                        tmpG.fillRect(0, 0, 1000, 1000);
                        tmpG.rotate(Math.toRadians(90.0));
                        tmpG.drawImage(previewImage, 0, 0 - width, null);
                        previewImage = tmpImage;
                    }
                    if (forKml) {
                        g.drawImage(previewImage, 0, 0, null);
                    } else {
                        g.drawImage(previewImage, baseX, (vertical
                                ? baseY
                                : baseY - height), null);
                    }
                    if (showLines) {
                        g.setColor(lineColor);
                        g.drawRect(baseX, (vertical
                                           ? baseY
                                           : baseY - height), width - 1,
                                           height - (vertical
                                ? 1
                                : 0));
                    }
                    setFont(g, child);
                    FontMetrics fm     = g.getFontMetrics();
                    List        values = new ArrayList();
                    String suffixFrequency = XmlUtil.getAttribute(child,
                                                 ATTR_SUFFIXFREQUENCY,
                                                 XmlUtil.getAttribute(child,
                                                     ATTR_SHOWUNIT,
                                                     "false")).toLowerCase();
                    String unitDefault = ( !suffixFrequency.equals("false"))
                                         ? " %unit%"
                                         : "";
                    String labelSuffix = applyMacros(child, ATTR_SUFFIX,
                                             unitDefault);
                    if (unit != null) {
                        labelSuffix = labelSuffix.replace("%unit%",
                                "" + unit);
                    } else {
                        labelSuffix = labelSuffix.replace("%unit%", "");
                    }
                    if (valuesStr != null) {
                        double[] valueArray = Misc.parseDoubles(valuesStr,
                                                  ",");
                        for (int valueIdx = 0; valueIdx < valueArray.length;
                                valueIdx++) {
                            values.add(new Double(valueArray[valueIdx]));
                        }
                    } else if (ticks > 0) {
                        int spacing = ((ticks == 1)
                                       ? 0
                                       : (vertical
                                          ? height
                                          : width) / (ticks - 1));
                        for (int tickIdx = 0; tickIdx < ticks; tickIdx++) {
                            double percent = ((ticks > 1)
                                    ? (double) tickIdx / (double) (ticks - 1)
                                    : 0.0);
                            values.add(
                                new Double(range.getValueOfPercent(percent)));
                        }
                    } else if (interval > 0) {
                        double value = range.getMin();
                        double max   = range.getMax();
                        while (value <= max) {
                            values.add(new Double(value));
                            value += interval;
                        }
                    }
                    for (int valueIdx = 0; valueIdx < values.size();
                            valueIdx++) {
                        double value =
                            ((Double) values.get(valueIdx)).doubleValue();
                        int x;
                        int y;
                        if (vertical) {
                            if (orientation.equals(VALUE_RIGHT)) {
                                x = baseX + width;
                            } else {
                                x = baseX;
                            }
                            y = baseY
                                + (int) (range.getPercent(value) * height);
                            if (y > baseY + height) {
                                break;
                            }
                        } else {
                            if (orientation.equals(VALUE_BOTTOM)) {
                                y = baseY;
                            } else {
                                y = baseY - height;
                            }

                            if(range != null)
                                x = baseX
                                    + (int) (range.getPercent(value) * width);
                            else
                                x = baseX;

                            if (x > baseX + width) {
                                break;
                            }
                        }
                        String tickLabel =
                            getIdv().getDisplayConventions().format(value);
                        if (suffixFrequency.equals(VALUE_LAST)
                                && (valueIdx == values.size() - 1)) {
                            tickLabel += labelSuffix;
                        } else if (suffixFrequency.equals(VALUE_FIRST)
                                   && (valueIdx == 0)) {
                            tickLabel += labelSuffix;
                        } else if (suffixFrequency.equals(VALUE_ALL)
                                   || suffixFrequency.equals("true")) {
                            tickLabel += labelSuffix;
                        }


                        Rectangle2D rect = fm.getStringBounds(tickLabel, g);
                        g.setColor(lineColor);
                        if (orientation.equals(VALUE_RIGHT)) {
                            g.drawLine(x + 1, y, x, y);
                            if (showLines) {
                                g.drawLine(x, y, x - width, y);
                            }
                        } else if (orientation.equals(VALUE_LEFT)) {
                            g.drawLine(x - 1, y, x, y);
                            if (showLines) {
                                g.drawLine(x, y, x + width, y);
                            }
                        } else if (orientation.equals(VALUE_BOTTOM)) {
                            g.drawLine(x, y + 1, x, y);
                            if (showLines) {
                                g.drawLine(x, y, x, y - height);
                            }
                        } else {
                            g.drawLine(x, y - 1, x, y);
                            if (showLines) {
                                g.drawLine(x, y, x, y + height);
                            }
                        }
                        g.setColor(c);
                        if (orientation.equals(VALUE_RIGHT)) {
                            int yLoc = y + (int) (rect.getHeight() / 2) - 2;
                            if (forKml) {
                                if (valueIdx == 0) {
                                    yLoc = y + (int) (rect.getHeight()) - 2;
                                } else if (valueIdx == values.size() - 1) {
                                    yLoc = y - (int) (rect.getHeight()) + 6;
                                }
                            }
                            g.drawString(tickLabel, x + 2, yLoc);
                        } else if (orientation.equals(VALUE_LEFT)) {
                            int xLoc = x - 2 - (int) rect.getWidth();
                            g.drawString(tickLabel, xLoc,
                                         y + (int) (rect.getHeight() / 2)
                                         - 2);
                        } else if (orientation.equals(VALUE_BOTTOM)) {
                            int xLoc = x - (int) (rect.getWidth() / 2);
                            if (forKml) {
                                if (valueIdx == 0) {
                                    xLoc = x + 2;
                                } else if (valueIdx == values.size() - 1) {
                                    xLoc = x - (int) rect.getWidth() + 2;
                                }
                            }
                            g.drawString(tickLabel, xLoc,
                                         y + (int) rect.getHeight() + 2);
                        } else {
                            g.drawString(tickLabel,
                                         x - (int) (rect.getWidth() / 2),
                                         y - 2);
                        }
                    }
                    if (vertical) {
                        baseX += width + 30;
                    } else {
                        baseY += height + 30;
                    }
                    if (forKml) {
                        String tmpImageFile =
                            applyMacros(
                                child, ATTR_FILE,
                                getIdv().getStore().getTmpFile(
                                    "testcolorbar${viewindex}.png"));
                        String template =
                            "<ScreenOverlay><name>${kml.name}</name><Icon><href>${icon}</href></Icon>\n"
                            + "<overlayXY x=\"${kml.overlayXY.x}\" y=\"${kml.overlayXY.y}\" xunits=\"${kml.overlayXY.xunits}\" yunits=\"${kml.overlayXY.yunits}\"/>\n"
                            + "<screenXY x=\"${kml.screenXY.x}\" y=\"${kml.screenXY.y}\" xunits=\"${kml.screenXY.xunits}\" yunits=\"${kml.screenXY.yunits}\"/>\n"
                            + "<size x=\"${kml.size.x}\" y=\"${kml.size.y}\" xunits=\"${kml.size.xunits}\" yunits=\"${kml.size.yunits}\"/>\n"
                            + "</ScreenOverlay>\n";
                        String[] macros = {
                            "kml.name", "kml.overlayXY.x", "kml.overlayXY.y",
                            "kml.overlayXY.xunits", "kml.overlayXY.yunits",
                            "kml.screenXY.x", "kml.screenXY.y",
                            "kml.screenXY.xunits", "kml.screenXY.yunits",
                            "kml.size.x", "kml.size.y", "kml.size.xunits",
                            "kml.size.yunits"
                        };
                        String[] macroValues = {
                            "", "0", "1", "fraction", "fraction", "0", "1",
                            "fraction", "fraction", "-1", "-1", "pixels",
                            "pixels"
                        };

                        for (int macroIdx = 0; macroIdx < macros.length;
                                macroIdx++) {
                            template =
                                template.replace("${" + macros[macroIdx]
                                    + "}", applyMacros(child,
                                        macros[macroIdx],
                                        macroValues[macroIdx]));
                        }
                        template = template.replace("${icon}",
                                IOUtil.getFileTail(tmpImageFile));
                        imageProps.put("kml", template);
                        List kmlFiles = (List) imageProps.get("kmlfiles");
                        //TODO: Only do the first one for now
                        if (kmlFiles == null) {
                            kmlFiles = new ArrayList();
                            imageProps.put("kmlfiles", kmlFiles);
                        }
                        kmlFiles.add(tmpImageFile);

                        //                        System.out.println(template);
                        ImageUtils.writeImageToFile(imageToDrawIn,
                                tmpImageFile);
                    }
                }


            } else if (tagName.equals(TAG_TRANSPARENT)
                       || tagName.equals(TAG_BGTRANSPARENT)) {
                Color c = null;
                if (tagName.equals(TAG_BGTRANSPARENT)) {
                    c = viewManager.getBackground();
                } else {
                    c = applyMacros(child, ATTR_COLOR, (Color) null);
                }
                //                System.err.println ("c:" + c);
                int[] redRange   = { 0, 0 };
                int[] greenRange = { 0, 0 };
                int[] blueRange  = { 0, 0 };
                if (c != null) {
                    //                    System.err.println("got color");
                    redRange[0]   = redRange[1] = c.getRed();
                    greenRange[0] = greenRange[1] = c.getGreen();
                    blueRange[0]  = blueRange[1] = c.getBlue();
                } else {}
                newImage = ImageUtils.makeColorTransparent(image, redRange,
                        greenRange, blueRange);
            } else if (tagName.equals(TAG_SHOW)) {
                JComponent contents = new JLabel(new ImageIcon(image));
                String message = applyMacros(child, ATTR_MESSAGE,
                                             (String) null);
                if (message != null) {
                    contents = GuiUtils.topCenter(new JLabel(message),
                            contents);
                }
                if ( !GuiUtils.askOkCancel("Continue?", contents)) {
                    throw new MyQuitException();
                }
            } else if (tagName.equals(TAG_MATTE)) {
                newImage = doMatte(image, child, 0);
            } else if (tagName.equals(TAG_LATLONLABELS)) {
                newImage = doLatLonLabels(child, viewManager, image,
                                          imageProps);
            } else if (tagName.equals(TAG_WRITE)) {
                ImageUtils.writeImageToFile(
                    image, getImageFileName(applyMacros(child, ATTR_FILE)));

            } else if (tagName.equals(TAG_PUBLISH)) {
                getIdv().getPublishManager().publishIslImage(this, node,
                        image);
            } else if (tagName.equals(TAG_CLIP)) {
                int[] ul;
                int[] lr;
                if (XmlUtil.hasAttribute(child, ATTR_DISPLAY)) {
                    //                    System.err.println("Clipping from display");
                    DisplayControlImpl dc = findDisplayControl(child);
                    if (dc == null) {
                        throw new IllegalArgumentException(
                            "Could not find display:"
                            + XmlUtil.toString(node));
                    }
                    NavigatedDisplay display =
                        (NavigatedDisplay) viewManager.getMaster();
                    MapProjection mapProjection = dc.getDataProjection();
                    java.awt.geom.Rectangle2D rect =
                        mapProjection.getDefaultMapArea();
                    LatLonPoint llplr =
                        mapProjection.getLatLon(new double[][] {
                        { rect.getX() + rect.getWidth() },
                        { rect.getY() + rect.getHeight() }
                    });
                    LatLonPoint llpul =
                        mapProjection.getLatLon(new double[][] {
                        { rect.getX() }, { rect.getY() }
                    });
                    EarthLocation ulEl = new EarthLocationTuple(llpul,
                                             new Real(RealType.Altitude, 0));
                    EarthLocation lrEl = new EarthLocationTuple(llplr,
                                             new Real(RealType.Altitude, 0));
                    ul = display.getScreenCoordinates(
                        display.getSpatialCoordinates(ulEl, null));
                    lr = display.getScreenCoordinates(
                        display.getSpatialCoordinates(lrEl, null));
                    //System.err.println("ul:" + ulEl + " lr:" + lrEl);
                    if (ul[0] > lr[0]) {
                        int tmp = ul[0];
                        ul[0] = lr[0];
                        lr[0] = tmp;
                    }
                    if (ul[1] > lr[1]) {
                        int tmp = ul[1];
                        ul[1] = lr[1];
                        lr[1] = tmp;
                    }
                    imageProps.put(ATTR_NORTH,
                                   new Double(ulEl.getLatitude().getValue()));
                    imageProps.put(
                        ATTR_WEST,
                        new Double(ulEl.getLongitude().getValue()));
                    imageProps.put(ATTR_SOUTH,
                                   new Double(lrEl.getLatitude().getValue()));
                    imageProps.put(
                        ATTR_EAST,
                        new Double(lrEl.getLongitude().getValue()));
                } else if ((viewManager != null)
                           && XmlUtil.hasAttribute(child, ATTR_NORTH)) {
                    NavigatedDisplay display =
                        (NavigatedDisplay) viewManager.getMaster();
                    EarthLocation el1 =
                        DisplayControlImpl.makeEarthLocation(toDouble(child,
                            ATTR_NORTH), toDouble(child, ATTR_WEST), 0);
                    EarthLocation el2 =
                        DisplayControlImpl.makeEarthLocation(toDouble(child,
                            ATTR_SOUTH), toDouble(child, ATTR_EAST), 0);
                    ul = display.getScreenCoordinates(
                        display.getSpatialCoordinates(el1, null));
                    lr = display.getScreenCoordinates(
                        display.getSpatialCoordinates(el2, null));
                    imageProps.put(ATTR_NORTH,
                                   new Double(el1.getLatitude().getValue()));
                    imageProps.put(ATTR_WEST,
                                   new Double(el1.getLongitude().getValue()));
                    imageProps.put(ATTR_SOUTH,
                                   new Double(el2.getLatitude().getValue()));
                    imageProps.put(ATTR_EAST,
                                   new Double(el2.getLongitude().getValue()));
                } else if (XmlUtil.hasAttribute(child, ATTR_LEFT)) {
                    ul = new int[] {
                        (int) toDouble(child, ATTR_LEFT, imageWidth),
                        (int) toDouble(child, ATTR_TOP, imageHeight) };
                    lr = new int[] {
                        (int) toDouble(child, ATTR_RIGHT, imageWidth),
                        (int) toDouble(child, ATTR_BOTTOM, imageHeight) };
                } else if (viewManager != null) {
                    //TODO: Clip on visad coordinates
                    NavigatedDisplay display =
                        (NavigatedDisplay) viewManager.getMaster();
                    ul = display.getScreenCoordinates(new double[] { -1, 1,
                            0 });
                    lr = display.getScreenCoordinates(new double[] { 1, -1,
                            0 });
                    int space  = applyMacros(child, ATTR_SPACE, 0);
                    int hspace = applyMacros(child, ATTR_HSPACE, space);
                    int vspace = applyMacros(child, ATTR_VSPACE, space);
                    ul[0] -= applyMacros(child, ATTR_SPACE_LEFT, hspace);
                    ul[1] -= applyMacros(child, ATTR_SPACE_TOP, vspace);
                    lr[0] += applyMacros(child, ATTR_SPACE_RIGHT, hspace);
                    lr[1] += applyMacros(child, ATTR_SPACE_BOTTOM, vspace);
                } else {
                    continue;
                }


                for (String attr :
                        (List<String>) Misc.newList(ATTR_NORTH, ATTR_SOUTH,
                            ATTR_EAST, ATTR_WEST)) {
                    String kmlAttr = "kml." + attr;
                    if (XmlUtil.hasAttribute(child, kmlAttr)) {
                        imageProps.put(attr,
                                       new Double(applyMacros(child, kmlAttr,
                                           0.0)));
                    }
                }



                ul[0]    = Math.max(0, ul[0]);
                ul[1]    = Math.max(0, ul[1]);

                lr[0]    = Math.min(lr[0], imageWidth);
                lr[1]    = Math.min(lr[1], imageHeight);


                newImage = ImageUtils.clip(image, ul, lr);
            } else if (tagName.equals(TAG_SPLIT)) {
                shouldIterateChildren = false;
                int    width  = image.getWidth(null);
                int    height = image.getHeight(null);
                int    cols   = applyMacros(child, ATTR_COLUMNS, 2);
                int    rows   = applyMacros(child, ATTR_ROWS, 2);
                String file   = applyMacros(child, ATTR_FILE);
                int    cnt    = 0;
                int    hSpace = width / cols;
                int    vSpace = height / rows;
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        pushProperties();
                        Hashtable myprops = new Hashtable();
                        putProperty("row", new Integer(row));
                        putProperty("column", new Integer(col));
                        putProperty("count", new Integer(++cnt));
                        String realFile = applyMacros(file, myprops);
                        Image splitImage = image.getSubimage(hSpace * col,
                                               vSpace * row, hSpace, vSpace);
                        processImage(ImageUtils.toBufferedImage(splitImage),
                                     realFile, child, myprops, viewManager,
                                     new Hashtable());
                        popProperties();
                    }
                }
            } else if (tagName.equals(TAG_THUMBNAIL)) {
                shouldIterateChildren = false;
                BufferedImage thumbImage =
                    ImageUtils.toBufferedImage(resize(image, child));
                String thumbFile = applyMacros(child, ATTR_FILE,
                                       (String) null);
                if (thumbFile == null) {
                    thumbFile = IOUtil.stripExtension(filename) + "_thumb"
                                + IOUtil.getFileExtension(filename);
                }
                processImage(thumbImage, thumbFile, child, null, viewManager,
                             new Hashtable());
            } else if (tagName.equals(TAG_KML)) {
                //NOOP
            } else if (tagName.equals(TAG_KMZFILE)) {
                //NOOP
            } else if (tagName.equals(TAG_OVERLAY)) {
                double transparency = applyMacros(child, ATTR_TRANSPARENCY,
                                          0.0);
                Graphics2D g = (Graphics2D) image.getGraphics();
                String imagePath = applyMacros(child, ATTR_IMAGE,
                                       (String) null);

                Rectangle imageRect = new Rectangle(0, 0, imageWidth,
                                          imageHeight);
                Point pp = ImageUtils.parsePoint(applyMacros(child,
                               ATTR_PLACE, "lr,-10,-10"), imageRect);
                String text = applyMacros(child, ATTR_TEXT, (String) null);
                Color  bg = applyMacros(child, ATTR_BACKGROUND, (Color) null);
                if (text != null) {
                    double angle = Math.toRadians(applyMacros(child,
                                       ATTR_ANGLE, 0.0));
                    text = applyMacros(text);
                    Color c = applyMacros(child, ATTR_COLOR, Color.white);
                    if ((c != null) && (transparency > 0)) {
                        c = new Color(c.getRed(), c.getGreen(), c.getBlue(),
                                      ImageUtils.toAlpha(transparency));
                    }
                    //Color bg = applyMacros(child, ATTR_BACKGROUND,
                    //                       (Color) null);
                    if ((bg != null) && (transparency > 0)) {
                        bg = new Color(bg.getRed(), bg.getGreen(),
                                       bg.getBlue(),
                                       ImageUtils.toAlpha(transparency));
                    }
                    setFont(g, child);
                    FontMetrics fm     = g.getFontMetrics();
                    Rectangle2D rect   = fm.getStringBounds(text, g);
                    int         width  = (int) rect.getWidth();
                    int         height = (int) (rect.getHeight());

                    Point ap = ImageUtils.parsePoint(applyMacros(child,
                                   ATTR_ANCHOR,
                                   "lr,-10,-10"), new Rectangle(0, 0, width,
                                       height));

                    g.rotate(angle);

                    if (bg != null) {
                        g.setColor(bg);
                        g.fillRect(pp.x - ap.x - 1, pp.y - ap.y - 1,
                                   (int) width + 2, (int) height + 2);
                    }
                    g.setColor(c);
                    g.drawString(text, pp.x - ap.x, pp.y - ap.y + height);
                }

                if (imagePath != null) {
                    Image overlay = ImageUtils.readImage(imagePath);
                    if (overlay != null) {
                        if (transparency > 0) {
                            overlay = ImageUtils.setAlpha(overlay,
                                    transparency);
                        }
                        int width  = overlay.getWidth(null);
                        int height = overlay.getHeight(null);
                        Point ap = ImageUtils.parsePoint(applyMacros(child,
                                       ATTR_ANCHOR,
                                       "lr,-10,-10"), new Rectangle(0, 0,
                                           width, height));
                        g.drawImage(overlay, pp.x - ap.x, pp.y - ap.y, bg,
                                    null);
                    }
                }
            } else {
                error("Unknown tag:" + tagName);
            }
            if (newImage != null) {
                String newFileName = applyMacros(child, ATTR_FILE,
                                         (String) null);
                if (shouldIterateChildren) {
                    newImage = processImage(newImage, newFileName, child,
                                            null, viewManager,
                                            new Hashtable());
                }
                if (newFileName != null) {
                    ImageUtils.writeImageToFile(newImage,
                            getImageFileName(newFileName));
                    debug("Writing image:" + newFileName);
                }
                if ( !applyMacros(child, ATTR_COPY, false)) {
                    image = newImage;
                }
            }
        }


        if (filename != null) {
            float quality = (float) applyMacros(node, ATTR_QUALITY, 1.0);
            List<String> fileToks = (List<String>) StringUtil.split(filename,
                                        ",", true, true);
            for (String file : fileToks) {
                file = getImageFileName(file);
                debug("Writing image:" + file);
                if (file.endsWith(FileManager.SUFFIX_KMZ)) {
                    GeoLocationInfo bounds = null;
                    if (viewManager != null) {
                        bounds = viewManager.getVisibleGeoBounds();
                        ImageSequenceGrabber.subsetBounds(bounds, imageProps);
                        String tail = IOUtil.getFileTail(file);
                        String tmpImageFile =
                            getIdv().getStore().getTmpFile(tail + ".png");
                        ImageUtils.writeImageToFile(image, tmpImageFile,
                                quality);
                        ImageWrapper imageWrapper =
                            new ImageWrapper(tmpImageFile, null, bounds,
                                             null);
                        imageWrapper.setProperties(imageProps);
                        new ImageSequenceGrabber(
                            file, getIdv(), this, node,
                            (List<ImageWrapper>) Misc.newList(imageWrapper),
                            null, 1);
                    }
                } else {
                    ImageUtils.writeImageToFile(image, file, quality);
                }
            }
        }
        return image;
    }


    /**
     * Get the insets
     *
     * @param child  the element
     * @param dflt   the default value
     *
     * @return the Insets
     */
    public Insets getInsets(Element child, int dflt) {
        int space  = applyMacros(child, ATTR_SPACE, dflt);
        int hspace = applyMacros(child, ATTR_HSPACE, space);
        int vspace = applyMacros(child, ATTR_VSPACE, space);
        int top    = applyMacros(child, ATTR_TOP, vspace);
        int bottom = applyMacros(child, ATTR_BOTTOM, vspace);
        int left   = applyMacros(child, ATTR_LEFT, hspace);
        int right  = applyMacros(child, ATTR_RIGHT, hspace);
        return new Insets(top, left, bottom, right);
    }


    /**
     * Process the lat/lon labels tag
     *
     * @param child  the XML
     * @param viewManager  the associated view manager
     * @param image  the image to draw on
     * @param imageProps  the image properties
     *
     * @return  a new image
     *
     * @throws Exception  on badness
     */
    public BufferedImage doLatLonLabels(Element child,
                                        ViewManager viewManager,
                                        BufferedImage image,
                                        Hashtable imageProps)
            throws Exception {

        if (viewManager == null) {
            throw new IllegalArgumentException("Tag " + TAG_LATLONLABELS
                    + " requires a view");
        }
        if ( !(viewManager instanceof MapViewManager)) {
            throw new IllegalArgumentException("Tag " + TAG_LATLONLABELS
                    + " requires a map view");
        }
        MapViewManager   mvm     = (MapViewManager) viewManager;
        NavigatedDisplay display = (NavigatedDisplay) viewManager.getMaster();
        DecimalFormat format = new DecimalFormat(applyMacros(child,
                                   ATTR_FORMAT, "##0.0"));
        Color color     = applyMacros(child, ATTR_COLOR, Color.red);
        Color lineColor = applyMacros(child, ATTR_LINECOLOR, color);
        Color bg = applyMacros(child, ATTR_LABELBACKGROUND, (Color) null);

        double[] latValues = Misc.parseDoubles(applyMacros(child,
                                 ATTR_LAT_VALUES, ""));
        List<String> latLabels = StringUtil.split(applyMacros(child,
                                     ATTR_LAT_LABELS, ""), ",", true, true);
        double[] lonValues = Misc.parseDoubles(applyMacros(child,
                                 ATTR_LON_VALUES, ""));
        List<String> lonLabels = StringUtil.split(applyMacros(child,
                                     ATTR_LON_LABELS, ""), ",", true, true);

        boolean drawLonLines = applyMacros(child, ATTR_DRAWLONLINES, false);
        boolean drawLatLines = applyMacros(child, ATTR_DRAWLATLINES, false);
        boolean       showTop    = applyMacros(child, ATTR_SHOWTOP, false);
        boolean       showBottom = applyMacros(child, ATTR_SHOWBOTTOM, true);
        boolean       showLeft   = applyMacros(child, ATTR_SHOWLEFT, true);
        boolean       showRight  = applyMacros(child, ATTR_SHOWRIGHT, false);

        int           width      = image.getWidth(null);
        int           height     = image.getHeight(null);
        int           centerX    = width / 2;
        int           centerY    = height / 2;
        EarthLocation nw, ne, se, sw;

        //don: this  what I added
        Double north = (Double) imageProps.get(ATTR_NORTH);
        Double south = (Double) imageProps.get(ATTR_SOUTH);
        Double east  = (Double) imageProps.get(ATTR_EAST);
        Double west  = (Double) imageProps.get(ATTR_WEST);
        //Assume if we have one we have them all
        if (north != null) {
            nw = DisplayControlImpl.makeEarthLocation(north.doubleValue(),
                    west.doubleValue(), 0);

            ne = DisplayControlImpl.makeEarthLocation(north.doubleValue(),
                    east.doubleValue(), 0);
            sw = DisplayControlImpl.makeEarthLocation(south.doubleValue(),
                    west.doubleValue(), 0);
            se = DisplayControlImpl.makeEarthLocation(south.doubleValue(),
                    east.doubleValue(), 0);

        } else {
            nw = display.screenToEarthLocation(0, 0);
            ne = display.screenToEarthLocation(width, 0);
            se = display.screenToEarthLocation(0, height);
            sw = display.screenToEarthLocation(width, height);
        }


        double widthDegrees = ne.getLongitude().getValue()
                              - nw.getLongitude().getValue();
        double heightDegrees = ne.getLatitude().getValue()
                               - se.getLatitude().getValue();

        Insets insets = getInsets(child, 0);
        int    delta  = 2;
        int    bgPad  = 1;

        image = doMatte(image, child, 0);

        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setFont(getFont(child));
        FontMetrics fm            = g.getFontMetrics();

        int lineOffsetRight = applyMacros(child, ATTR_LINEOFFSET_RIGHT, 0);
        int lineOffsetLeft = applyMacros(child, ATTR_LINEOFFSET_LEFT, 0);
        int         lineOffsetTop = applyMacros(child, ATTR_LINEOFFSET_TOP,
                                        0);
        int lineOffsetBottom = applyMacros(child, ATTR_LINEOFFSET_BOTTOM, 0);



        Stroke      lineStroke;
        if (XmlUtil.hasAttribute(child, ATTR_DASHES)) {
            lineStroke = new BasicStroke((float) applyMacros(child,
                    ATTR_LINEWIDTH, 1.0), BasicStroke.CAP_BUTT,
                                          BasicStroke.JOIN_BEVEL, 1.0f,
                                          Misc.parseFloats(applyMacros(child,
                                              ATTR_DASHES, "8")), 0.0f);
        } else {
            lineStroke = new BasicStroke((float) applyMacros(child,
                    ATTR_LINEWIDTH, 1.0));
        }

        g.setStroke(lineStroke);
        double leftLon  = nw.getLongitude().getValue(CommonUnit.degree);
        double rightLon = ne.getLongitude().getValue(CommonUnit.degree);
        Range  lonRange = new Range(leftLon, rightLon);

        for (int i = 0; i < lonValues.length; i++) {
            double lon = GeoUtils.normalizeLongitude(lonRange, lonValues[i]);
            double percent = (lon - nw.getLongitude().getValue())
                             / widthDegrees;
            //            if(percent<0 || percent>1) continue;
            String label;
            if (i < lonLabels.size()) {
                label = lonLabels.get(i);
            } else {
                label = format.format(lonValues[i]);
            }
            Rectangle2D rect  = fm.getStringBounds(label, g);
            int         baseX = insets.left + (int) (percent * width);
            int         x     = baseX - (int) rect.getWidth() / 2;
            int         topY;
            if (insets.top == 0) {
                topY = (int) rect.getHeight() + delta;
            } else {
                topY = insets.top - delta;
            }
            if (drawLonLines) {
                g.setColor(lineColor);
                g.drawLine(baseX, insets.top + lineOffsetTop, baseX,
                           insets.top + height - lineOffsetBottom);
            }

            if (showTop) {
                if (bg != null) {
                    g.setColor(bg);
                    g.fillRect(x - bgPad,
                               topY - (int) rect.getHeight() - bgPad,
                               (int) rect.getWidth() + bgPad * 2,
                               (int) rect.getHeight() + bgPad * 2);
                }
                g.setColor(color);
                g.drawString(label, x, topY);
            }
            int bottomY;
            if (insets.bottom == 0) {
                bottomY = insets.top + height - delta;
            } else {
                bottomY = insets.top + height + (int) rect.getHeight()
                          + delta;
            }
            if (showBottom) {
                if (bg != null) {
                    g.setColor(bg);
                    g.fillRect(x - bgPad,
                               bottomY - (int) rect.getHeight() - bgPad,
                               (int) rect.getWidth() + bgPad * 2,
                               (int) rect.getHeight() + bgPad * 2);
                }
                g.setColor(color);
                g.drawString(label, x, bottomY);
            }
        }


        for (int i = 0; i < latValues.length; i++) {
            double lat = latValues[i];
            double percent = 1.0
                             - (lat - se.getLatitude().getValue())
                               / heightDegrees;
            int baseY = insets.top + (int) (percent * height);
            //            if(percent<0 || percent>1) continue;
            String label;
            if (i < latLabels.size()) {
                label = latLabels.get(i);
            } else {
                label = format.format(lat);
            }
            Rectangle2D rect = fm.getStringBounds(label, g);
            int         y    = baseY + (int) rect.getHeight() / 2;
            int         leftX;
            if (insets.left == 0) {
                leftX = 0 + delta;
            } else {
                leftX = insets.left - (int) rect.getWidth() - delta;
            }
            if (drawLonLines) {
                g.setColor(lineColor);
                g.drawLine(insets.left + lineOffsetRight, baseY,
                           insets.left + width - lineOffsetRight, baseY);
            }

            if (showLeft) {
                if (bg != null) {
                    g.setColor(bg);
                    g.fillRect(leftX - bgPad,
                               y - (int) rect.getHeight() - bgPad,
                               (int) rect.getWidth() + bgPad * 2,
                               (int) rect.getHeight() + bgPad * 2);
                }
                g.setColor(color);
                g.drawString(label, leftX, y);
            }

            if (insets.right == 0) {
                leftX = insets.left + width - (int) rect.getWidth() - delta;
            } else {
                leftX = insets.left + width + delta;
            }
            if (showRight) {
                if (bg != null) {
                    g.setColor(bg);
                    g.fillRect(leftX - bgPad,
                               y - (int) rect.getHeight() - bgPad,
                               (int) rect.getWidth() + bgPad * 2,
                               (int) rect.getHeight() + bgPad * 2);
                }
                g.setColor(color);
                g.drawString(label, leftX, y);
            }
        }

        return image;

    }

    /**
     * Matte the image
     *
     * @param image  the image
     * @param child  the XML defining the matting
     * @param dfltSpace  default spacing
     *
     * @return a new image
     */
    public BufferedImage doMatte(BufferedImage image, Element child,
                                 int dfltSpace) {
        return doMatte(image, child, getInsets(child, dfltSpace));
    }


    /**
     * Matte the image
     *
     * @param image  the image
     * @param child  the matte specs
     * @param insets the insets
     *
     * @return  a new image
     */
    public BufferedImage doMatte(BufferedImage image, Element child,
                                 Insets insets) {
        Color bg = applyMacros(child, ATTR_BACKGROUND, Color.white);
        return ImageUtils.matte(image, insets.top, insets.bottom,
                                insets.left, insets.right, bg);
    }



    /*
      public void setDataSourceFiles(String[] datasource, String[] filenames) {

      List dataSources = getIdv().getDataSources();
        for (int k = 0; k < datasource.size(); k++) {
          for (int i = 0; i < dataSources.size(); i++) {
            DataSource theDataSource = (DataSource) dataSources.get(i);
            if (theDataSource.identifiedByName(datasource[k])) {
              theDataSource.setNewFiles(new ArrayList().add(filenames[k]));
            }
          }
        }
      }

     */


    /**
     * Get the file name to write images to. If we are in test mode then prepend the test directory
     *
     * @param filename image file name
     *
     * @return filename to use
     */
    private String getImageFileName(String filename) {
        if (LogUtil.getTestMode()) {
            if (getIdv().getArgsManager().testDir != null) {
                filename = IOUtil.joinDir(getIdv().getArgsManager().testDir,
                                          filename);
            }
        }
        return filename;
    }


    /**
     * Set the font on the graphics from the font defined on the node.
     *
     * @param g The graphics
     * @param node Node to get font info from
     */
    private void setFont(Graphics g, Element node) {
        int fontSize = applyMacros(node, ATTR_FONTSIZE, 12);
        Font f = new Font(applyMacros(node, ATTR_FONTFACE, "dialog"),
                          Font.PLAIN, fontSize);
        g.setFont(f);
    }


    /**
     * Get the font from the XML
     *
     * @param node  the XML
     *
     * @return  the font or null
     */
    private Font getFont(Element node) {
        if (XmlUtil.hasAttribute(node, ATTR_FONTSIZE)
                || XmlUtil.hasAttribute(node, ATTR_FONTFACE)) {
            int fontSize = applyMacros(node, ATTR_FONTSIZE, 12);
            return new Font(applyMacros(node, ATTR_FONTFACE, "dialog"),
                            Font.PLAIN, fontSize);
        }
        return null;
    }



    /**
     * Called to notify this object that the movie capture is done
     */
    public synchronized void doneCapturingMovie() {
        this.notify();
    }


    /**
     * Capture a movie from the first view manager
     *
     * @param filename The movie  filename
     */
    public synchronized void captureMovie(String filename) {
        captureMovie(filename, null);
    }


    /**
     * Capture the movie
     *
     * @param filename The file
     * @param scriptingNode Node form isl.
     */
    public synchronized void captureMovie(String filename,
                                          Element scriptingNode) {
        if ((filename == null) && (scriptingNode != null)) {
            filename = XmlUtil.getAttribute(scriptingNode, ATTR_FILE);
        }

        if (scriptingNode != null) {
            List files = findFiles(scriptingNode);
            if (files != null) {
                debug("Making movie from existing images " + filename);
                filename = applyMacros(filename);
                Dimension size = new Dimension(applyMacros(scriptingNode,
                                     ATTR_WIDTH,
                                     400), applyMacros(scriptingNode,
                                         ATTR_HEIGHT, 300));
                ImageSequenceGrabber isg = new ImageSequenceGrabber(
                                               filename, getIdv(), this,
                                               scriptingNode, files, size,
                                               applyMacros(
                                                   scriptingNode,
                                                   ATTR_FRAMERATE,
                                                   2), applyMacros(
                                                       scriptingNode,
                                                       ATTR_ENDFRAMEPAUSE,
                                                       -1));
                return;
            }
        }

        List<ViewManager> viewManagers = null;
        if ((scriptingNode != null)
                && XmlUtil.hasAttribute(scriptingNode, ATTR_DISPLAY)) {
            DisplayControlImpl display = findDisplayControl(scriptingNode);
            if (display == null) {
                throw new IllegalArgumentException("Could not find display:"
                        + XmlUtil.toString(scriptingNode));
            }
            String what = applyMacros(scriptingNode, ATTR_WHAT,
                                      (String) null);

            ViewManager viewManager = null;
            try {
                viewManager = display.getViewManagerForCapture(what);
            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }

            if (viewManager != null) {
                viewManagers = (List<ViewManager>) Misc.newList(viewManager);
            } else {
                throw new IllegalArgumentException(
                    "Cannot capture a movie with display:"
                    + XmlUtil.toString(scriptingNode));
            }
        }

        if (viewManagers == null) {
            viewManagers = (List<ViewManager>) getViewManagers(scriptingNode);
        }



        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager viewManager = (ViewManager) viewManagers.get(i);

            getProperties().put(PROP_VIEWINDEX, new Integer(i));
            String name = viewManager.getName();
            if (name == null) {
                name = "view" + i;
            }
            getProperties().put(PROP_VIEWNAME, name);

            if ( !getIdv().getArgsManager().getIsOffScreen()) {
                JFrame frame = GuiUtils.getFrame(viewManager.getContents());
                if (frame != null) {
                    LogUtil.registerWindow(frame);
                    frame.show();
                    GuiUtils.toFront(frame);
                    frame.setLocation(50, 50);
                    Misc.sleep(50);
                }
            }
            String loopFilename = applyMacros(filename);
            debug("Making movie:" + loopFilename);
            ImageSequenceGrabber isg = new ImageSequenceGrabber(viewManager,
                                           loopFilename, getIdv(), this,
                                           scriptingNode);
            try {
                wait();
            } catch (Exception exc) {
                logException("Doing the captureMovie wait", exc);
            }
            debug("Done making movie:" + loopFilename);
        }
    }


    /**
     * Find the animation time of the first Animation in a view manager we find
     *
     * @return Animation time
     */
    public Date getAnimationTime() {
        List vms = getViewManagers(currentNode);
        if (vms.size() > 0) {
            ViewManager vm        = (ViewManager) vms.get(0);
            Animation   animation = vm.getAnimation();
            if (animation != null) {
                Real v = animation.getAniValue();
                if (v != null) {
                    return new Date((long) v.getValue() * 1000);
                }

            }
        }
        return new Date(Misc.getCurrentTime());
    }


    /**
     * Create and instantiate the jython interp.
     *
     * @return The interp
     */
    private PythonInterpreter getInterpreter() {
        if (interpreter == null) {
            interpreter = getIdv().getJythonManager().createInterpreter();
            interpreter.set("ig", this);
            interpreter.set("interp", this);
            interpreter.set("islInterpreter", this);
        }
        return interpreter;
    }

    /**
     * callable by jython to find the data choices that match the given pattern
     *
     * @param datasource data source
     * @param pattern pattern to match
     *
     * @return comma separated list of data choice names
     */
    public String fields(String datasource, String pattern) {
        DataSource dataSource = findDataSource(datasource);
        if (dataSource == null) {
            throw new IllegalArgumentException("Could not find data source:"
                    + datasource);
        }
        List choices;
        if ((pattern == null) || (pattern.length() == 0)) {
            choices = dataSource.getDataChoices();
        } else {
            choices = dataSource.findDataChoices(pattern);
        }

        List names = new ArrayList();
        for (int i = 0; i < choices.size(); i++) {
            DataChoice dataChoice = (DataChoice) choices.get(i);
            names.add(dataChoice.getName());
        }
        return StringUtil.join(",", names);
    }



    /**
     * Class OutputInfo is used for handling output tags
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.113 $
     */
    private class OutputInfo {

        /** The node */
        Element outputNode;

        /** mapping of where to StringBuffer */
        Hashtable buffers = new Hashtable();

        /** mapping of where to templates */
        Hashtable templates = new Hashtable();

        /**
         * ctor
         *
         * @param node The output node
         */
        public OutputInfo(Element node) {
            this.outputNode = node;
        }

        /**
         * Handle the node.
         *
         * @param node Node to process
         *
         * @throws Throwable On badness
         */
        public void process(Element node) throws Throwable {
            String       where = applyMacros(node, ATTR_TEMPLATE, "contents");
            StringBuffer sb       = (StringBuffer) buffers.get(where);
            String       template = (String) templates.get(where);
            if (sb == null) {
                sb = new StringBuffer();
                template = XmlUtil.getAttribute(outputNode,
                        ATTR_TEMPLATE + ":" + where, "${text}");
                if (template.startsWith("file:")) {
                    template = applyMacros(template);
                    template = IOUtil.readContents(template.substring(5));
                }
                buffers.put(where, sb);
                templates.put(where, template);
            }
            String text = XmlUtil.getAttribute(node, ATTR_TEXT,
                              (String) null);
            if (text == null) {
                if (XmlUtil.hasAttribute(node, ATTR_FROMFILE)) {
                    String filename = applyMacros(node, ATTR_FROMFILE);
                    text = applyMacros(IOUtil.readContents(filename));
                } else {
                    text = XmlUtil.getChildText(node);
                    if ((text != null) && (text.length() == 0)) {
                        text = null;
                    }
                }
            }
            if (text == null) {
                NamedNodeMap nnm   = node.getAttributes();
                Hashtable    props = new Hashtable();
                if (nnm != null) {
                    for (int i = 0; i < nnm.getLength(); i++) {
                        Attr attr = (Attr) nnm.item(i);
                        if ( !ATTR_TEMPLATE.equals(attr.getNodeName())) {
                            props.put(attr.getNodeName(),
                                      applyMacros(attr.getNodeValue()));
                        }
                    }
                }
                text = applyMacros(template, props);
            } else {
                text = applyMacros(text);
            }
            sb.append(text);
        }

        /**
         * Write out the output
         *
         * @throws Throwable On badness
         */
        public void write() throws Throwable {
            String outputFile = applyMacros(outputNode, ATTR_FILE);
            String template = applyMacros(outputNode, ATTR_TEMPLATE,
                                          (String) null);
            if (template == null) {
                template = "${contents}";
            }
            if (template.startsWith("file:")) {
                template = IOUtil.readContents(template.substring(5));
            }
            for (Enumeration keys =
                    buffers.keys(); keys.hasMoreElements(); ) {
                String       key  = (String) keys.nextElement();
                StringBuffer buff = (StringBuffer) buffers.get(key);
                template = applyMacros(template,
                                       Misc.newHashtable(key,
                                           buff.toString()));
            }
            IOUtil.writeFile(outputFile, template);
        }

    }


    /**
     * Print out a wanring message
     *
     * @param msg message
     */
    private void warning(String msg) {
        System.err.println(new Date() + " WARNING:" + msg);
    }



    /**
     * Print the message if in debug mode
     *
     * @param msg The message
     */
    protected void debug(String msg) {
        if (debug) {
            System.out.println(new Date() + ": " + msg);
        }
    }


    /**
     * Class MyBreakException for handling break tags
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.113 $
     */
    protected static class MyBreakException extends Exception {}

    /**
     * Class MyContinueException for handling continue tags
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.113 $
     */
    protected static class MyContinueException extends Exception {}

    /**
     * Class MyReturnException allows us to return from a isl procedure by throwing an exception.
     * Yes, I know you're not supposed to use exceptions in a non-exceptional way but it works
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.113 $
     */
    protected static class MyReturnException extends Exception {}


    /**
     * Class description
     *
     *
     * @version        Enter version here..., Tue, Jan 12, '10
     * @author         Enter your name here...
     */
    protected static class MyQuitException extends Exception {}

    /**
     * Class BadIslException is used to handle bad isl errors
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.113 $
     */
    private static class BadIslException extends RuntimeException {

        /** message */
        String msg;

        /**
         * ctor
         *
         * @param msg error message
         */
        public BadIslException(String msg) {
            this.msg = msg;
        }

        /**
         * to string
         *
         * @return error message
         */
        public String toString() {
            return msg;
        }

    }


    /**
     * IS the FtpClient in an ok state. If it isn't then disconnect it and throw and IllegalStateException
     *
     * @param f Ftp client
     * @param msg Message to use if in error
     *
     * @throws Exception On badness
     */
    private static void checkFtp(FTPClient f, String msg) throws Exception {
        int replyCode = f.getReplyCode();
        if ( !FTPReply.isPositiveCompletion(replyCode)) {
            String reply = f.getReplyString();
            f.disconnect();
            throw new IllegalStateException("Error with ftp: " + replyCode
                                            + " " + msg + "\n" + reply);
        }
    }

    /**
     * Do an FTP put of the given bytes
     *
     * @param server server
     * @param userName user name on server
     * @param password password on server
     * @param destination Where to put the bytes
     * @param bytes The bytes
     *
     * @throws Exception On badness
     */
    public static void ftpPut(String server, String userName,
                              String password, String destination,
                              byte[] bytes)
            throws Exception {
        FTPClient f = new FTPClient();

        f.connect(server);
        f.login(userName, password);
        f.setFileType(FTP.BINARY_FILE_TYPE);
        f.enterLocalPassiveMode();
        checkFtp(f, "Connecting to ftp server");
        f.storeFile(destination, new ByteArrayInputStream(bytes));
        checkFtp(f, "Storing file");
        f.logout();
        f.disconnect();
    }

    /** abcdefg... */
    private static String[] alphabet = {
        "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
        "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
    };

    /** roman numerals */
    private static String[] roman = {
        "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI",
        "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XX", "XXI",
        "XXII", "XXIII", "XXIV", "XXV", "XXVI", "XXVII", "XXVIII"
    };

    /**
     * Get the letter for the index
     *
     * @param i  the index
     *
     * @return  the letter
     */
    public String getLetter(int i) {
        if ((i >= 0) && (i < alphabet.length)) {
            return alphabet[i];
        }
        //A hack for now
        return "out of range";

    }

    /**
     * Get the roman numeral
     *
     * @param i the index
     *
     * @return  the corresponding number
     */
    public String getRoman(int i) {
        if ((i >= 0) && (i < roman.length)) {
            return roman[i];
        }
        //A hack for now
        return "out of range";
    }



}
