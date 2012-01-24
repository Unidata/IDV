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

package ucar.unidata.idv;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.collab.SharableImpl;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.gis.KmlDataSource;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.publish.PublishManager;
import ucar.unidata.idv.ui.BottomLegend;
import ucar.unidata.idv.ui.IdvLegend;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.ImageSequenceGrabber;
import ucar.unidata.idv.ui.SideLegend;
import ucar.unidata.java3d.LightInfo;
import ucar.unidata.ui.Command;
import ucar.unidata.ui.CommandManager;
import ucar.unidata.ui.DropPanel;
import ucar.unidata.ui.FontSelector;
import ucar.unidata.ui.ImagePanel;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.Timeline;
import ucar.unidata.util.BooleanProperty;
import ucar.unidata.util.DatedObject;
import ucar.unidata.util.DatedThing;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.Removable;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.display.AnimationInfo;
import ucar.visad.display.AnimationWidget;
import ucar.visad.display.CompositeDisplayable;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.Displayable;
import ucar.visad.display.TextDisplayable;

import visad.ConstantMap;
import visad.ControlEvent;
import visad.ControlListener;
import visad.CoordinateSystem;
import visad.Data;
import visad.DateTime;
import visad.Display;
import visad.DisplayEvent;
import visad.DisplayImpl;
import visad.DisplayListener;
import visad.DisplayRenderer;
import visad.FieldImpl;
import visad.GraphicsModeControl;
import visad.KeyboardBehavior;
import visad.ProjectionControl;
import visad.Real;
import visad.Set;
import visad.VisADException;

import visad.bom.annotations.ImageJ3D;
import visad.bom.annotations.ScreenAnnotatorJ3D;

import visad.java3d.DisplayImplJ3D;
import visad.java3d.DisplayRendererJ3D;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.rmi.RemoteException;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;


//import org.apache.batik.svggen.SVGGraphics2D;



/**
 * A wrapper around a {@link ucar.visad.display.DisplayMaster}.
 * Provides an interface for managing user interactions, gui creation, etc.
 *
 * @author IDV development team
 */

public class ViewManager extends SharableImpl implements ActionListener,
        ItemListener, ControlListener, DisplayListener {

    /** Used to log errors */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(ViewManager.class.getName());


    /** Xml tag for the color pair */
    public static final String TAG_COLORPAIR = "colorpair";

    /** Xml tag for the color pairs xml */
    public static final String TAG_COLORPAIRS = "colorpairs";


    /** Prefix used for preference String ids */
    public static final String PREF_PREFIX = "View.";

    /** Preference for clipping at 3d box_ */
    public static final String PREF_3DCLIP = "View.3Dclip";

    /** Preference for  showing the wireframe box */
    public static final String PREF_SHOWSCALES = "View.ShowScales";

    /** Preference for  showing the wireframe box */
    public static final String PREF_SHOWTRANSECTSCALES =
        "View.ShowTransectScales";


    /** Preference for showing cursor readout */
    public static final String PREF_SHOWCURSOR = "View.ShowCursor";

    /** Preference for showing cursor readout */
    public static final String PREF_SHOWDISPLAYLIST = "View.ShowDisplayList";


    /** Preference for sharing view points */
    public static final String PREF_SHAREVIEWS = "View.ShareViews";

    /** are the toolbars floatable */
    public static final String PREF_TOOLBARSFLOATABLE =
        "idv.viewmanager.toolbars.floatable";



    /** For the bg color */
    public static final String PREF_BGCOLOR = "View.BackgroundColor";


    /** For the foreground color */
    public static final String PREF_FGCOLOR = "View.ForegroundColor";



    /** For the display list font */
    public static final String PREF_DISPLAYLISTFONT = "View.DisplayListFont";

    /** For the display list color */
    public static final String PREF_DISPLAYLISTCOLOR =
        "View.DisplayListColor";

    /** For hiding/showing components of the toolbar */
    public static final String PREF_SHOWTOOLBAR = "View.ShowToolBar";

    /** Show the side legend */
    public static final String PREF_SHOWSIDELEGEND = "View.ShowSideLegend";

    /** Show the bottom legend */
    public static final String PREF_SHOWBOTTOMLEGEND =
        "View.ShowBottomLegend";


    /** Show the bottom legend */
    public static final String PREF_SHOWANIMATIONBOXES =
        "View.ShowAnimationBoxes";


    /** Preference for  showing the wireframe box */
    public static final String PREF_WIREFRAME = "View.Wireframe";

    /** Preference for showing the time string in the display */
    public static final String PREF_ANIREADOUT = "View.AniReadout";


    /** Preference for  3d or 2d _ */
    public static final String PREF_DIMENSION = "View.Dimension";

    /** For the visibility of the please wait message */
    public static final String PREF_WAITMSG = "View.WaitVisible";

    /** For the visibility of the top compenent */
    public static final String PREF_TOPBAR_VISIBLE = "View.TopBarVisible";

    /** for the contour label size */
    public static final String PREF_CONTOUR_LABELSIZE =
        "idv.contour.labelsize";

    /** for the contour label Font */
    public static final String PREF_CONTOUR_LABELFONT =
        "idv.contour.labelfont";

    /** for the contour label Font */
    public static final String PREF_CONTOUR_LABELALIGN =
        "idv.contour.alignlabel";

    /** for the logo */
    public static final String PREF_LOGO = "idv.viewmanager.logo";

    /** for the logo position */
    public static final String PREF_LOGO_POSITION_OFFSET =
        "idv.viewmanager.logo.position";

    /** for the logo visibility */
    public static final String PREF_LOGO_VISIBILITY =
        "idv.viewmanager.logo.visible";

    /** for the logo scale */
    public static final String PREF_LOGO_SCALE = "idv.viewmanager.logo.scale";


    /** border width */
    public static final int borderWidth = 3;

    /** border hightlight color */
    //public static Color borderHighlightColor = new Color(255, 255, 204);
    //    public static Color borderHighlightColor = Color.magenta;
    public static Color borderHighlightColor = Color.blue;

    /** For the currently selected panel. */
    public static final String PREF_BORDERCOLOR = "View.BorderHighlightColor";

    /** line border */
    private static final Border lineBorder =
        BorderFactory.createLineBorder(Color.gray);

    /** highlight border */
    protected static Border highlightBorder;

    /** normal border */
    protected static Border normalBorder;


    /** is shared flag */
    private boolean isShared = true;

    /** Do we focus on this one only when the user clicks */
    protected boolean clickToFocus = true;

    /** Has this view manager been destroyed. */
    private boolean isDestroyed = false;

    /** Is the the currently active VM */
    protected boolean lastActive = false;

    /** Keeps track of the last time this VM was set as the active VM */
    protected long lastTimeActivated = 0;

    /** Holds the location of the side legend divider */
    private int sideDividerLocation = -1;

    /** name label */
    protected JLabel nameLabel;

    /** Keep around the split pane location */
    private double initialSplitPaneLocation = 0.75;


    /** A flag to turn off all toolbars */
    private boolean showToolbars = true;


    /** Acts as the timestamp of the wait key call */
    int currentWaitKey = 0;

    /** Tracks if the mouse is down */
    boolean mouseDown = false;

    /** The projection matrix when the mouse is first pressed */
    private double[] mousePressedMatrix;

    /** the name of the view manager */
    private String name;

    /** Has the init() method been called on this ViewManager? */
    private boolean haveInitialized = false;

    /** flag for init done */
    private boolean initDone = false;

    /** Handles history */
    private CommandManager commandManager;

    /** The window for full screen */
    protected JFrame fullScreenWindow;

    /** We can set the full screen to a specific size for image capture */
    private int fullScreenWidth = 0;

    /** We can set the full screen to a specific size for image capture */
    private int fullScreenHeight = 0;

    /** For full screen properties */
    private JTextField fullScreenWidthFld;

    /** For full screen properties */
    private JTextField fullScreenHeightFld;

    /** For logo properties */
    private JCheckBox logoVisCbx;

    /** For logo properties */
    private JTextField logoFileField;

    /** For logo properties */
    private String logoFile;

    /** For logo properties */
    private ScreenAnnotatorJ3D annotator;

    /** For logo properties */
    //private boolean logoVisibility = false;

    /** For logo properties */
    private String logoPosition = null;

    /** For logo properties */
    private float logoScale = -1f;

    /**
     * We keep the window bounds around for persisting/unpersisting
     * this ViewManager.
     */

    /** flag for light changes */
    private boolean ignoreLightChanges = false;

    /** lock lights button */
    private JToggleButton lockLightsBtn;

    /** The inner gui */
    protected JComponent innerContents;

    /** The outer most GUI Component */
    JComponent fullContents;

    /**
     * We keep the window bounds around for persisting/unpersisting
     * this ViewManager.
     */
    protected Rectangle windowBounds;

    /**
     * This is used when persisting/unpersisting to record whether
     * this ViewManager had been in its own window.
     */
    private boolean hasWindow = false;

    /** List of {@link IdvLegend}s */
    private List<IdvLegend> legends = new ArrayList();

    /** The side legend */
    private SideLegend sideLegend;

    /** Holds the side legend */
    private JComponent sideLegendContainer;



    /** This holds the side legend */
    private JComponent sideLegendComponent;

    /** gui */
    JPanel contentsWrapper;

    /** gui */
    JComponent centerPanel;

    /** gui */
    JComponent centerPanelWrapper;

    /** tracks wether the legend is shown, hidden or is floating */
    private String legendState = IdvLegend.STATE_DOCKED;


    /** flag for where the legend is */
    private boolean legendOnLeft = false;

    /** The current foreground color */
    private Color foreground = null;

    /** The current background color */
    private Color background = null;

    /** This holds the visibility toggle animation commands. */
    protected JMenu animationMenu;

    /** The menu bar */
    private JMenuBar menuBar;

    /** The menu bar */
    private Component topBar;

    /** List of Components that are shown in the left side, vertical tool bar */
    protected List toolbars = new ArrayList();


    /**
     * List of the ids (String), one for each component in the toolbars list
     * We keep this around so the user could have a preference to show or not
     * show toolbar components.
     */
    protected List toolbarIds = new ArrayList();


    /**
     * List of the names (String), one for each component in the toolbars list
     * We keep this around so the user could have a preference to show or not
     * show toolbar components.
     */
    protected List toolbarNames = new ArrayList();



    /** The default size for this ViewManager. */
    private Dimension defaultSize;


    /** for making timelines component */
    private Object TIMELINES_MUTEX = new Object();

    /** timelines */
    private List timelines = new ArrayList();

    /** the timeline that shows the animation set */
    private Timeline animationTimeline;

    /** for timelines */
    private JComponent timelineHolder;

    /** for timelines */
    JDialog timelineDialog;

    /** The animation widget */
    private AnimationWidget animationWidget;

    /** This allows us to have an animation from an external source for doing movie captures */
    private AnimationWidget externalAnimationWidget;

    /** The panel that holds the animation widget */
    protected JComponent animationHolder;

    /** We create this Animation and add it into the DisplayMaster */
    private Animation animation;

    /** This allows us to have an animation from an external source for doing movie captures */
    private Animation externalAnimation;

    /** Holds the animation state from the AnimationWidget */
    private AnimationInfo animationInfo;


    /** This is the split pane that holds the side legend */
    private JSplitPane mainSplitPane;

    /** The last split pane divider location when we do the float/embed */
    private int lastDividerLoc = -1;

    /** split pane location */
    private int splitPaneLocation = -1;


    /**
     *  This holds the list of DisplayInfos being displayed in this ViewManager.
     *  The DisplayInfo holds the Displayable and the DisplayControl.
     */
    private List<DisplayInfo> displayInfos = new ArrayList<DisplayInfo>();


    /** Do we show the control menu */
    protected boolean showControlMenu = true;

    /** Do we show the control legends */
    protected boolean showControlLegend = true;



    /** The {@link ucar.visad.display.DisplayMaster} */
    private DisplayMaster master;

    /** The bounds of the display_ */
    protected Rectangle displayBounds;

    /**
     * The keyboard behavior we add to the display that routes key type
     *  event to this ViewManager.
     */
    private IdvKeyboardBehavior keyboardBehavior;


    //    ViewDescriptor viewDescriptor;

    /** Describes this ViewManager (mostly has a name) */
    List aliases = new ArrayList();



    /** The ProjectionControl from the DisplayMaster */
    private ProjectionControl projectionControl;


    /** The IDV_ */
    private IntegratedDataViewer idv;


    /**
     * When toggling the visibility of display controls this is the
     * index of the currently shown display control
     */
    private int currentVisibilityIdx = -1;

    /** Flag when we are running the display control visibility animation */
    private boolean runVisibilityAnimation = false;

    /** How fast do we automatically toggle  visibility */
    private int animationSpeed = 1000;

    /** The GUI component to show the visiblity toggle animation */
    private JCheckBoxMenuItem animationCB;


    /** Keeps track of how many wait cursor calls have been made */
    private int outstandingWaits = 0;

    /** Have we seen the first frame done event from the display */
    private boolean receivedFirstFrameDone = false;

    /** For making movies */
    private ImageSequenceGrabber isg;


    /**
     *  A mapping from (String) id to BooleanProperty.
     */
    private Hashtable booleanPropertyMap = new Hashtable();

    /** List of BooleanProperty-s */
    private List booleanProperties = new ArrayList();

    /** Holds the values to force them to be persisted */
    private Hashtable booleanPropertiesForPersistence;


    /** init properties */
    private String initProperties;

    /** aspect ratio */
    private double[] aspectRatio;


    /** Keep the aspect ratio values around when we show the properties dialog */
    private double[] originalAspectSliderValues = { 0, 0, 0 };

    /** aspect sliders */
    private JSlider[] aspectSliders = { null, null, null };


    /** aspect slider labels */
    private JLabel[] aspectLbls = { null, null, null };

    /** aspect slider labels values */
    private String[] aspectText = { "X", "Y", "Z" };

    /**
     *  The viewpoint matrix when we are (un)persisted
     */
    private double[] initMatrix;

    /** skin properties */
    private Hashtable skinProperties;

    /** name field */
    private JTextField nameFld;

    /** properties map */
    private Hashtable propertiesMap;

    /** properties dialog */
    JDialog propertiesDialog;

    /** is the properties dialog shown */
    boolean propertiesDialogShown = false;

    /** the matrix label */
    JLabel matrixLabel;

    /** decimal format */
    DecimalFormat fmt = new DecimalFormat("####0.0###");

    /** the view menu */
    JMenu viewMenu;

    /** foreground color swatch */
    private GuiUtils.ColorSwatch fgPropertiesSwatch;

    /** background color swatch */
    private GuiUtils.ColorSwatch bgPropertiesSwatch;

    /** background color swatch */
    private GuiUtils.ColorSwatch dlPropertiesSwatch;

    /** Holds the display list displayables */
    private CompositeDisplayable displayListDisplayables;

    /** mutext for the display list */
    private Object MUTEX_DISPLAYLIST = new Object();

    /** default display list font */
    private static final Font defaultFont = FontSelector.DEFAULT_FONT;

    /**
     *  Used to hold the font of the display list displayable
     */
    private Font displayListFont = null;

    /**
     *  Used to hold the font of the display list displayable
     */
    private Color displayListColor = null;

    /**
     * DisplayList font selector
     */
    private FontSelector fontSelector;

    /** Just in case this synchs the legend filling */
    private Object LEGENDMUTEX = new Object();

    /** are we currently about to fill the legends */
    private boolean fillLegendsPending = false;

    /** The last time we tried to fill the legends */
    private long fillLegendsTime = 0;


    /** hi res button */
    private static JRadioButton hiBtn;

    /** publish checkbox */
    private JComboBox publishCbx;

    /** medium res button */
    private static JRadioButton medBtn;

    /** low res button */
    private static JRadioButton lowBtn;

    /** main display button */
    private static JRadioButton mainDisplayBtn;

    /** For capturing images */
    private static JCheckBox backgroundTransparentBtn;

    /** contents button */
    private static JRadioButton contentsBtn;

    /** full window button */
    private static JRadioButton fullWindowBtn;


    /** Last time we saw a FRAME_DONE event */
    private long lastFrameDoneTime = 0;


    /** general properties */
    private Hashtable properties = new Hashtable();


    /** counter */
    static int cnt = 0;

    /** instance counter */
    int mycnt = cnt++;

    /** Keeps track of when we update display list when the component resizes_ */
    private int componentResizeCnt = 0;

    /** are we dirty ;-) */
    private boolean dirty = false;


    /** Vector Graphics renderer */
    private VectorGraphicsRenderer vectorRenderer;

    /** init view state name */
    private String initViewStateName;


    /** removeables */
    private List<Removable> removables = new ArrayList<Removable>();

    /** logo position box */
    private JComboBox logoPositionBox;

    /** logo sizer control */
    private JSlider logoSizer;

    /** the logo size label */
    private JLabel logoSizeLabel;

    /** the logo offset field */
    private JTextField logoOffsetTextField;

    /** logo positions */
    protected static TwoFacedObject[] logoPoses = { new TwoFacedObject(
                                                      "Lower Left", "ll"),
            new TwoFacedObject("Upper Left", "ul"),
            new TwoFacedObject("Upper Right", "ur"),
            new TwoFacedObject("Lower Right", "lr"),
            new TwoFacedObject("Center", "mm"), };


    /**
     *  A parameter-less ctor for the XmlEncoder based decoding.
     */
    public ViewManager() {}


    /**
     * Create  this ViewManager
     *
     * @param viewContext As  a hack this really better be an IntegratedDataViewer
     */
    public ViewManager(ViewContext viewContext) {
        this.idv = (IntegratedDataViewer) viewContext;
        setHighlightBorder(getStore().get(PREF_BORDERCOLOR, Color.blue));
    }


    /**
     *  Instantiate this ViewManager with the given AnimationInfo
     *
     * @param viewContext As  a hack this really better be an IntegratedDataViewer
     * @param info The initial animation info
     */
    public ViewManager(ViewContext viewContext, AnimationInfo info) {
        this(viewContext, null, null, info);
    }

    /**
     *  Instantiate this ViewManager with the given AnimationInfo
     *
     * @param viewContext As  a hack this really better be an IntegratedDataViewer
     * @param viewDescriptor  the view descriptor
     * @param properties a list of semi-colon separated properties
     */
    public ViewManager(ViewContext viewContext,
                       ViewDescriptor viewDescriptor, String properties) {
        this(viewContext, viewDescriptor, properties, null);
    }


    /**
     *  Instantiate this ViewManager with the given AnimationInfo
     *
     * @param viewContext As  a hack this really better be an IntegratedDataViewer
     * @param viewDescriptor  the view descriptor
     * @param properties a list of semi-colon separated properties
     * @param info The initial animation info
     */
    public ViewManager(ViewContext viewContext,
                       ViewDescriptor viewDescriptor, String properties,
                       AnimationInfo info) {
        this(viewContext);
        //        if(this instanceof MapViewManager)
        this.animationInfo  = info;
        this.initProperties = properties;
        if ((viewDescriptor == null)
                || viewDescriptor.nameEquals(ViewDescriptor.LASTACTIVE)) {
            viewDescriptor = new ViewDescriptor();
        }
        addViewDescriptor(viewDescriptor);
        setHighlightBorder(getStore().get(PREF_BORDERCOLOR, Color.blue));
    }


    /**
     * Create  this ViewManager
     *
     * @param master The DisplayMaster to use
     * @param viewContext As  a hack this really better be an IntegratedDataViewer
     * @param viewDescriptor This describes this ViewManager. Mostly just a name.
     * @param properties semi-colon separated list of name=value properties.
     * We apply these properties to this object using reflection- looking for
     * public set methods with the given name. We use reflection on the argument
     * to the set method to coerce the String value in the properties to the
     * correct type.
     *
     * @throws RemoteException
     * @throws VisADException
     *
     */
    public ViewManager(ViewContext viewContext, DisplayMaster master,
                       ViewDescriptor viewDescriptor, String properties)
            throws VisADException, RemoteException {
        this(viewContext, viewDescriptor, properties, null);
        setDisplayMaster(master);
    }


    /**
     * Initialize.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void init() throws VisADException, RemoteException {
        if (haveInitialized) {
            return;
        }
        Trace.call1("ViewManager.init");

        haveInitialized = true;
        initSharable();
        Trace.call1("ViewManager.init getMaster()");
        getMaster();
        Trace.call2("ViewManager.init getMaster()");

        getMaster().addKeyboardBehavior(keyboardBehavior =
            new IdvKeyboardBehavior(this));
        addRemovable(keyboardBehavior);

        Trace.call1("ViewManager.init setBooleanProperties()");
        setBooleanProperties(this);
        Trace.call2("ViewManager.init setBooleanProperties()");



        Animation animation = getAnimation();
        if (animation != null) {
            getMaster().setAnimation(animation, animationWidget);
            //            getMaster().addDisplayable(animation);
        }


        Trace.call1("ViewManager.init initWith");
        initWith(this);
        Trace.call2("ViewManager.init initWith");


        Trace.call2("ViewManager.init");

        initDone = true;
    }


    /**
     * Get the array of animation times
     *
     * @return times
     */
    public DateTime[] getAnimationTimes() {
        if (animationWidget != null) {
            return animationWidget.getTimes();
        }
        return null;
    }

    /**
     * Get the list of times from the control that is flagged as the time driver
     *
     * @return list of times from the time driver control or null
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   VisAD problem
     */
    public List<DateTime> getTimeDriverTimes()
            throws VisADException, RemoteException {
        for (DisplayControl control : (List<DisplayControl>) getControls()) {
            if (control.getIsTimeDriver()) {
                Set        timeSet = control.getTimeSet();
                DateTime[] times   = Animation.getDateTimeArray(timeSet);
                return (List<DateTime>) Misc.toList(times);
            }
        }
        return null;
    }


    /**
     * have we initialized
     *
     * @return Has init been called
     */
    protected boolean getHaveInitialized() {
        return haveInitialized;
    }

    /**
     * Have we finished initializing
     *
     * @return finished initializing
     */
    protected boolean getInitDone() {
        return initDone;
    }

    /**
     * Initialize from a skin
     *
     * @param skinNode the XML node
     */
    public void initFromSkin(Element skinNode) {
        NodeList elements = XmlUtil.getElements(skinNode, "property");
        for (int childIdx = 0; childIdx < elements.getLength(); childIdx++) {
            Element child = (Element) elements.item(childIdx);
            String  name  = XmlUtil.getAttribute(child, "name");
            String  value = XmlUtil.getAttribute(child, "value");
            if ( !setProperty(name, value, true)) {
                if (skinProperties == null) {
                    skinProperties = new Hashtable();
                }
                skinProperties.put(name, value);
            }
        }
    }




    /**
     * Handle the drop action
     *
     * @param object object being dropped
     */
    public void doDrop(Object object) {
        DisplayControl control = (DisplayControl) object;
        ViewManager    vm      = control.getViewManager();
        if ((vm == null) || (vm == this)) {
            return;
        }
        control.moveTo(this);
    }


    /**
     * Create the ui
     */
    protected void initUI() {

        try {
            init();
            if (initProperties != null) {
                //We set initProperties to null so we don't infinite loop
                String tmp = initProperties;
                initProperties = null;
                parseProperties(tmp);

            }
        } catch (Exception exp) {
            logException("Initializing UI", exp);
        }
        if (fullContents != null) {
            return;
        }

        JComponent baseContents = (JComponent) doMakeContents();
        baseContents    = makeDropPanel(baseContents, false);
        innerContents   = GuiUtils.center(baseContents);
        contentsWrapper = GuiUtils.center(innerContents);
        menuBar         = doMakeMenuBar();
        if (menuBar != null) {
            menuBar.setBorderPainted(false);
            menuBar.setMargin(new Insets(0, 0, 0, 0));
            menuBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            menuBar.setOpaque(false);
        }
        nameLabel = GuiUtils.cLabel(" ");
        updateNameLabel();
        if (showToolbars) {
            initToolBars();
        }

        checkToolBarVisibility();
        Component topRight;
        if (animationWidget == null) {
            topRight = GuiUtils.filler();
        } else {
            topRight = animationHolder =
                GuiUtils.inset(animationWidget.getContents(),
                               new Insets(0, 0, 0, 4));
        }
        JPanel leftNav = GuiUtils.topCenter(GuiUtils.doLayout(toolbars, 1,
                             GuiUtils.WT_N, GuiUtils.WT_N), null);


        //Component topBar = GuiUtils.leftCenterRight(GuiUtils.bottom(menuBar),
        topBar = GuiUtils.leftCenterRight(GuiUtils.bottom(menuBar),
                                          GuiUtils.bottom(nameLabel),
                                          topRight);
        centerPanel = GuiUtils.topCenter(topBar, contentsWrapper);
        topBar.setVisible(getTopBarVisible());
        if (getShowBottomLegend()) {
            IdvLegend bottomLegend = new BottomLegend(this);
            addRemovable(bottomLegend);
            synchronized (legends) {
                legends.add(bottomLegend);
            }
            JComponent contents = bottomLegend.getContents();
            if (showControlLegend) {
                centerPanel = GuiUtils.vsplit(centerPanel, contents, 1.0);
                bottomLegend.setTheContainer(centerPanel);
                ((JSplitPane) centerPanel).setDividerSize(2);
            }
        }

        //Create it if we need to
        if (sideLegend == null) {
            sideLegend = new SideLegend(this);
            addRemovable(sideLegend);
        }
        synchronized (legends) {
            if (getShowSideLegend()) {
                legends.add(sideLegend);
            }
        }
        sideLegendComponent = getSideComponent(sideLegend.getContents());
        sideLegendContainer = new JPanel(new BorderLayout());
        sideLegendContainer.add(BorderLayout.CENTER, sideLegendComponent);
        //Set the contents from the side legend in case the sideLegendComponent is not just the 
        //contents from the legend
        sideLegend.setContentsToUse(sideLegendComponent);


        JComponent leftComp  = (legendOnLeft
                                ? sideLegendContainer
                                : centerPanel);
        JComponent rightComp = (legendOnLeft
                                ? centerPanel
                                : sideLegendContainer);
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftComp,
                                       rightComp);

        if (legendOnLeft) {
            mainSplitPane.setResizeWeight(0.20);
        } else {
            mainSplitPane.setResizeWeight(0.80);
        }
        mainSplitPane.setOneTouchExpandable(true);
        if (splitPaneLocation >= 0) {
            //SPLIT       mainSplitPane.setDividerLocation(splitPaneLocation);
        }


        //        centerPanelWrapper = new JPanel(new BorderLayout());
        centerPanelWrapper = (showControlLegend)
                             ? GuiUtils.center(mainSplitPane)
                             : GuiUtils.center(centerPanel);
        fullContents       = GuiUtils.leftCenter(leftNav, centerPanelWrapper);
        fullContents.setBorder(getContentsBorder());

        insertSideLegend();
        fillLegends();

    }


    /**
     * Set the position state of the side legend
     *
     * @param state float, hidden, etc
     */
    public void setSideLegendPosition(String state) {
        legendState = state;
        insertSideLegend();
    }



    /**
     * Place the side legend where it belongs
     */
    protected void insertSideLegend() {
        if (legendState.equals(IdvLegend.STATE_DOCKED)) {
            sideLegend.unFloatLegend();
            sideLegendContainer.removeAll();
            sideLegendContainer.add(BorderLayout.CENTER, sideLegendComponent);
            sideLegendContainer.repaint();
            if (lastDividerLoc >= 0) {
                mainSplitPane.setDividerLocation(lastDividerLoc);
            } else {
                mainSplitPane.resetToPreferredSizes();
            }
        } else if (legendState.equals(IdvLegend.STATE_FLOAT)) {
            lastDividerLoc = mainSplitPane.getDividerLocation();
            sideLegendContainer.removeAll();
            sideLegendContainer.repaint();
            sideLegend.floatLegend();
            if (legendOnLeft) {
                mainSplitPane.setDividerLocation(0);
            } else {
                mainSplitPane.setDividerLocation(
                    mainSplitPane.getBounds().width);

            }
        } else if (legendState.equals(IdvLegend.STATE_HIDDEN)) {
            lastDividerLoc = mainSplitPane.getDividerLocation();
            sideLegendContainer.removeAll();
            sideLegendContainer.repaint();
            sideLegend.unFloatLegend();
            sideLegendContainer.add(BorderLayout.CENTER, sideLegendComponent);
            if (legendOnLeft) {
                mainSplitPane.setDividerLocation(0);
            } else {
                mainSplitPane.setDividerLocation(
                    mainSplitPane.getBounds().width);
            }
        }
    }







    /**
     * Make a dnd panel
     *
     * @param contents contents to show
     * @param doBorder draw  a border
     *
     * @return the drop panel
     */
    public DropPanel makeDropPanel(JComponent contents, boolean doBorder) {
        DropPanel dropPanel = new DropPanel(contents, doBorder) {
            public void handleDrop(Object object) {
                Misc.run(ViewManager.this, "doDrop", object);
            }
            public boolean okToDrop(Object object) {
                if ( !(object instanceof DisplayControl)) {
                    return false;
                }
                return okToImportDisplay((DisplayControl) object);
            }
        };

        return dropPanel;
    }


    /**
     * Can this view manager import the given display control. e.g., from a drag-and-drop
     *
     * @param control control
     *
     * @return can import
     */
    public boolean okToImportDisplay(DisplayControl control) {
        ViewManager vm = control.getViewManager();
        if ((vm == null) || (vm == ViewManager.this)
                || !vm.getClass().equals(ViewManager.this.getClass())) {
            return false;
        }
        return true;
    }


    /**
     * Set the contents boreder
     *
     * @param b  the border
     */
    public void setContentsBorder(Border b) {
        if (fullContents != null) {
            fullContents.setBorder(b);
        }
    }


    /**
     * Get the border for the contents
     *
     * @return  the border
     */
    protected Border getContentsBorder() {
        return BorderFactory.createMatteBorder(1, 1, 0, 0, Color.gray);
    }


    /**
     * Show the properties dialog
     */
    public void showPropertiesDialog() {
        //        propertiesDialog = GuiUtils.createDialog("Properties", true);
        //Make the dialog non-modal
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String cmd = event.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_APPLY)) {
                    if ( !applyProperties()) {
                        return;
                    }
                    getIdvUIManager().viewManagerChanged(ViewManager.this);
                }
                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_CANCEL)) {
                    propertiesDialog.dispose();
                    propertiesDialogShown = false;
                    //                    propertiesDialog = null;
                }
            }
        };


        JComponent buttons = GuiUtils.makeApplyOkCancelButtons(listener);
        JComponent comp =
            GuiUtils.inset(GuiUtils.centerBottom(getPropertiesComponent(),
                buttons), 5);
        boolean newOne = false;
        if (propertiesDialog == null) {
            IdvWindow myWindow = getDisplayWindow();
            Window    window   = null;
            if (myWindow != null) {
                window = myWindow.getWindow();
            }
            propertiesDialog = GuiUtils.createDialog(window, "Properties",
                    false);
            newOne = true;
        }
        propertiesDialog.getContentPane().removeAll();
        propertiesDialog.getContentPane().add(comp);
        propertiesDialog.pack();
        setMatrixLabel(true);
        if (newOne) {
            GuiUtils.showDialogNearSrc(viewMenu, propertiesDialog);
        } else {
            propertiesDialog.show();
        }
        propertiesDialogShown = true;
        //        propertiesDialog.show();
    }

    /**
     * Get  the component for setting properties on the  display.
     *
     * @return the component
     */
    public JComponent getPropertiesComponent() {
        JTabbedPane tabbedPane = new JTabbedPane();
        addPropertiesComponents(tabbedPane);
        return tabbedPane;
    }

    /**
     * Get main properties component
     *
     * @return the component
     */
    protected JComponent getMainPropertiesComponent() {

        nameFld       = new JTextField(((name != null)
                                        ? name
                                        : ""), 20);
        propertiesMap = new Hashtable();
        List props = new ArrayList();
        for (int i = 0; i < booleanProperties.size(); i++) {
            BooleanProperty bp  = (BooleanProperty) booleanProperties.get(i);
            JCheckBox       cbx = new JCheckBox(bp.getName(), bp.getValue());
            propertiesMap.put(cbx, bp);
            props.add(GuiUtils.left(cbx));
        }
        GuiUtils.tmpInsets = new Insets(0, 5, 0, 5);
        JComponent propsComp = GuiUtils.doLayout(props, 2, GuiUtils.WT_N,
                                   GuiUtils.WT_N);

        bgPropertiesSwatch = new GuiUtils.ColorSwatch(getBackground(),
                "Set Background Color");
        fgPropertiesSwatch = new GuiUtils.ColorSwatch(getForeground(),
                "Set Foreground Color");

        List colorProps = new ArrayList();
        colorProps.add(GuiUtils.rLabel("Foreground:"));
        colorProps.add(fgPropertiesSwatch);
        colorProps.add(GuiUtils.rLabel("Background:"));
        colorProps.add(bgPropertiesSwatch);
        GuiUtils.tmpInsets = new Insets(2, 2, 2, 2);
        JComponent colorPanel = GuiUtils.doLayout(colorProps, 4,
                                    GuiUtils.WT_N, GuiUtils.WT_N);
        colorPanel.setBorder(BorderFactory.createTitledBorder("Colors"));


        fontSelector = new FontSelector(FontSelector.COMBOBOX_UI, false,
                                        false);
        fontSelector.setFont(getDisplayListFont());
        dlPropertiesSwatch = new GuiUtils.ColorSwatch(getDisplayListColor(),
                "Set Display List Color");

        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent fontPanel = GuiUtils.doLayout(new Component[] {
                                   GuiUtils.rLabel("   Font: "),
                                   GuiUtils
                                       .left(fontSelector
                                           .getComponent()), GuiUtils
                                               .rLabel("  Color: "),
                                   GuiUtils.left(
                                       GuiUtils.hbox(
                                           dlPropertiesSwatch,
                                           dlPropertiesSwatch.getClearButton(),
                                           5)) }, 2, GuiUtils.WT_N,
                                               GuiUtils.WT_N);

        fontPanel.setBorder(BorderFactory.createTitledBorder("Display List"));

        fullScreenWidthFld  = new JTextField(((fullScreenWidth > 0)
                ? "" + fullScreenWidth
                : ""), 5);
        fullScreenHeightFld = new JTextField(((fullScreenHeight > 0)
                ? "" + fullScreenHeight
                : ""), 5);
        fullScreenWidthFld.setToolTipText("Leave blank or 0 for full screen");
        fullScreenHeightFld.setToolTipText(
            "Leave blank or 0 for full screen");
        JPanel fullScreenPanel =
            GuiUtils.left(GuiUtils.hbox(new JLabel("Width: "),
                                        fullScreenWidthFld,
                                        new JLabel("  Height: "),
                                        fullScreenHeightFld));

        fullScreenPanel.setBorder(
            BorderFactory.createTitledBorder("Full Screen Dimensions"));

        //logoVisCbx = new JCheckBox("", getLogoVisibility());
        logoFileField = new JTextField(getLogoFile());
        logoFileField.setToolTipText("Enter a file or URL");
        // top panel
        JButton browseButton = new JButton("Browse..");
        browseButton.setToolTipText("Choose a logo from disk");
        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String filename =
                    FileManager.getReadFile(FileManager.FILTER_IMAGE);
                if (filename == null) {
                    return;
                }
                logoFileField.setText(filename);
            }
        });

        String[] logos = parseLogoPosition(getLogoPosition());
        logoPositionBox = new JComboBox(logoPoses);
        logoPositionBox.setToolTipText("Set the logo position on the screen");
        logoPositionBox.setSelectedItem(findLoc(logos[0]));

        logoOffsetTextField = new JTextField(logos[1]);
        logoOffsetTextField.setToolTipText(
            "Set an offset from the position (x,y)");

        logoSizeLabel = new JLabel("" + getLogoScale());
        ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                logoSizeLabel.setText("" + logoSizer.getValue() / 10.f);
            }
        };
        JComponent[] sliderComps = GuiUtils.makeSliderPopup(0, 20,
                                       (int) (getLogoScale() * 10), listener);
        logoSizer = (JSlider) sliderComps[1];
        sliderComps[0].setToolTipText("Change Logo Scale Value");

        JPanel logoPanel = GuiUtils.vbox(
        //GuiUtils.leftCenter(GuiUtils.rLabel("Visible: "), GuiUtils.leftCenter(logoVisCbx, GuiUtils.filler())),
        GuiUtils.centerRight(logoFileField, browseButton), GuiUtils.hbox(
            GuiUtils.leftCenter(
                GuiUtils.rLabel("Screen Position: "),
                logoPositionBox), GuiUtils.leftCenter(
                    GuiUtils.rLabel("Offset: "),
                    logoOffsetTextField), GuiUtils.leftCenter(
                        GuiUtils.rLabel("Scale: "),
                        GuiUtils.leftRight(logoSizeLabel, sliderComps[0]))));
        logoPanel.setBorder(BorderFactory.createTitledBorder("Logo"));

        propsComp = GuiUtils.vbox(new Component[] { propsComp,
                GuiUtils.inset(colorPanel, new Insets(10, 5, 5, 5)),
                GuiUtils.inset(fontPanel, new Insets(10, 5, 5, 5)),
                GuiUtils.inset(fullScreenPanel, new Insets(10, 5, 5, 5)),
                GuiUtils.inset(logoPanel, new Insets(10, 5, 5, 5)) });

        return GuiUtils.vbox(GuiUtils.left(GuiUtils.label("Name:  ",
                nameFld)), propsComp);

    }

    /**
     * Find the appropriate combobox item from the string
     *
     * @param loc  the string location
     *
     * @return  the corresponding TFO
     */
    protected TwoFacedObject findLoc(String loc) {
        if (loc.equalsIgnoreCase("ll")) {
            return logoPoses[0];
        } else if (loc.equalsIgnoreCase("ul")) {
            return logoPoses[1];
        } else if (loc.equalsIgnoreCase("ur")) {
            return logoPoses[2];
        } else if (loc.equalsIgnoreCase("lr")) {
            return logoPoses[3];
        } else if (loc.equalsIgnoreCase("mm")) {
            return logoPoses[4];
        }
        return logoPoses[0];
    }

    /**
     * Parse the logo position
     *
     * @param position  the string representation
     *
     * @return  the position and offset
     */
    protected String[] parseLogoPosition(String position) {
        String logoP, logoO;
        if ((position == null) || position.isEmpty()) {
            logoP = "ll";
            logoO = "0,0";
        } else {
            int firstComma = position.indexOf(",");
            if (firstComma > 0) {
                logoP = position.substring(0, firstComma - 1);
                logoO = position.substring(firstComma + 1);
            } else {
                logoP = position;
                logoO = "0,0";
            }
        }
        return new String[] { logoP, logoO };
    }

    /**
     * Make a logo position from the position and offset
     *
     * @param pos  the position
     * @param offset  the offset
     *
     * @return  the logo position string
     */
    protected String makeLogoPosition(String pos, String offset) {
        if ((pos == null) || pos.isEmpty()) {
            return "";
        }
        int firstComma = pos.indexOf(",");
        if (firstComma > 0) {
            return pos;
        } else {
            if ((offset == null) || offset.isEmpty()) {
                offset = "0,0";
            }
        }
        return pos + "," + offset;
    }




    /**
     * Add a JTabbedPane to the properties component
     *
     * @param tabbedPane  the pane to add
     */
    protected void addPropertiesComponents(JTabbedPane tabbedPane) {
        tabbedPane.add(
            "Main",
            GuiUtils.inset(GuiUtils.top(getMainPropertiesComponent()), 5));
        tabbedPane.add(
            "Aspect Ratio",
            GuiUtils.inset(GuiUtils.top(getAspectPropertiesComponent()), 5));
        if (lights != null) {
            try {


                if (lockLightsBtn == null) {
                    lockLightsBtn = GuiUtils.getToggleButton(
                        "/auxdata/ui/icons/link_break.png", 0, 0);
                    lockLightsBtn.setContentAreaFilled(false);
                    lockLightsBtn.setSelectedIcon(
                        GuiUtils.getImageIcon(
                            "/auxdata/ui/icons/link.png", getClass()));
                    lockLightsBtn.setSelected(false);
                    lockLightsBtn.setBorder(
                        BorderFactory.createEmptyBorder(0, 0, 0, 0));
                    lockLightsBtn.setToolTipText(
                        "Link stride changes between x & y");
                }

                List comps = new ArrayList();
                for (LightInfo lightInfo : lights) {
                    ObjectListener listener = new ObjectListener(lightInfo) {
                        public void actionPerformed(ActionEvent ae) {

                            LightInfo theLight = (LightInfo) theObject;
                            if (ignoreLightChanges) {
                                return;
                            }
                            ignoreLightChanges = true;
                            theLight.applyProperties();
                            if (lockLightsBtn.isSelected()) {
                                for (LightInfo light : lights) {
                                    if (light == theLight) {
                                        continue;
                                    }
                                    light.setVisible(theLight.getVisible(),
                                            true);
                                    light.updateLight();
                                }
                            }

                            ignoreLightChanges = false;
                        }
                        public void stateChanged(ChangeEvent e) {
                            JSlider slider = (JSlider) e.getSource();
                            if (ignoreLightChanges
                                    || slider.getValueIsAdjusting()) {
                                return;
                            }
                            LightInfo theLight = (LightInfo) theObject;
                            ignoreLightChanges = true;
                            theLight.applyProperties();
                            if (lockLightsBtn.isSelected()) {
                                for (LightInfo light : lights) {
                                    if (light == theLight) {
                                        continue;
                                    }
                                    light.setColor(theLight.getColor(), true);
                                    light.updateLight();
                                }

                            }
                            ignoreLightChanges = false;
                        }

                    };
                    lightInfo.getPropertyComponents(comps, listener);
                }

                JComponent lightsComp = GuiUtils.formLayout(comps);
                tabbedPane.add(
                    "Lighting",
                    GuiUtils.inset(
                        GuiUtils.top(
                            GuiUtils.vbox(
                                lightsComp,
                                GuiUtils.left(lockLightsBtn))), 5));
            } catch (Exception exc) {
                logException("", exc);
            }
        }
    }



    /**
     * Handle an aspect slider change.
     *
     * @param value  the new value
     */
    public void aspectSliderChanged(int value) {
        for (int i = 0; i < 3; i++) {
            aspectLbls[i].setText("" + aspectSliders[i].getValue() / 10.0);
        }
    }


    /**
     * Reset the aspect sliders.
     */
    public void resetAspectSliders() {
        if (aspectRatio == null) {
            aspectRatio = master.getDisplayAspect();
        }
        for (int i = 0; i < 3; i++) {
            aspectSliders[i].setValue((int) (aspectRatio[i] * 10));
            aspectLbls[i].setText("" + aspectRatio[i]);
        }
    }


    /** list of lights */
    private List<LightInfo> lights;

    /**
     * Initialize the lights
     *
     * @param renderer  the renderer
     */
    private void initLights(DisplayRendererJ3D renderer) {
        if (lights == null) {
            lights = new ArrayList<LightInfo>();
            createInitialLights();
        }
        BranchGroup lightsNode = new BranchGroup();
        lightsNode.setCapability(BranchGroup.ALLOW_DETACH);
        lightsNode.setCapability(Group.ALLOW_CHILDREN_READ);
        lightsNode.setCapability(Group.ALLOW_CHILDREN_WRITE);
        lightsNode.setCapability(Group.ALLOW_CHILDREN_EXTEND);


        for (LightInfo lightInfo : lights) {
            lightsNode.addChild(lightInfo.getLight());
        }

        renderer.getTrans().addChild(lightsNode);
        //        renderer.getRoot().addChild(lightsNode);

    }


    /**
     * Make the lights brighter
     */
    public void brighter() {
        if (lights == null) {
            return;
        }
        for (LightInfo lightInfo : lights) {
            lightInfo.brighter();
        }
    }


    /**
     * Create the initial lights
     */
    private void createInitialLights() {
        Point3d[] locations = new Point3d[] {
            new Point3d(0.0, 0.0, 0.0), new Point3d(0.0, 0.0, 0.0),
            new Point3d(0.0, 0.0, 0.0), new Point3d(0.0, 0.0, 0.0),
            new Point3d(0.0, 0.0, 0.0), new Point3d(0.0, 0.0, 0.0)
        };

        Vector3f[] directions = new Vector3f[] {
            new Vector3f(0.0f, 0.0f, 1.0f), new Vector3f(0.0f, 0.0f, -1.0f),
            new Vector3f(0.0f, -1.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f),
            new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(-1.0f, 0.0f, 0.0f)
        };


        String[] names = {
            "Over", "Under", "Top", "Bottom", "Right", "Left"
        };
        for (int i = 0; i < directions.length; i++) {
            lights.add(new LightInfo(names[i], locations[i], directions[i]));
        }
    }





    /**
     * Get the aspect properties component
     *
     * @return  the component
     */
    protected JComponent getAspectPropertiesComponent() {
        try {
            DisplayMaster master = getMaster();
            JButton resetBtn = GuiUtils.makeButton("Reset", this,
                                   "resetAspectSliders");
            List comps = new ArrayList();
            comps.add(resetBtn);
            comps.add(GuiUtils.filler());

            for (int i = 0; i < 3; i++) {
                aspectSliders[i] = GuiUtils.makeSlider(0, 100, 0, this,
                        "aspectSliderChanged", true);
                aspectLbls[i] = new JLabel(" ");
                comps.add(GuiUtils.rLabel(aspectText[i]));
                comps.add(GuiUtils.centerRight(aspectSliders[i],
                        aspectLbls[i]));
            }
            resetAspectSliders();
            originalAspectSliderValues = new double[] {
                aspectSliders[0].getValue() / 10.0,
                aspectSliders[1].getValue() / 10.0,
                aspectSliders[2].getValue() / 10.0 };
            GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
            JComponent contents = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                      GuiUtils.WT_N);


            matrixLabel = new JLabel("<html></html>");
            matrixLabel.setBorder(
                BorderFactory.createTitledBorder("Display Matrix Settings"));
            setMatrixLabel(true);
            contents = GuiUtils.centerBottom(contents,
                                             GuiUtils.inset(matrixLabel,
                                                 new Insets(20, 0, 0, 0)));
            return contents;
        } catch (Exception exc) {
            logException("Creating aspect dialog", exc);
        }
        return null;
    }


    /** last matrix */
    private double[] lastMatrix;

    /** last coordinates */
    private List<TwoFacedObject> lastCoords;

    /**
     * Set the matrix label
     *
     * @param force  true to force this
     */
    private void setMatrixLabel(boolean force) {
        try {
            if (matrixLabel == null) {
                return;
            }
            if ( !force && !propertiesDialogShown) {
                return;
            }
            if ( !force) {
                try {
                    matrixLabel.getLocationOnScreen();
                } catch (Exception exc) {
                    return;
                }
            }
            double[]             currentMatrix = getDisplayMatrix();
            boolean changed = !Misc.equals(lastMatrix, currentMatrix);
            List<TwoFacedObject> coords        = null;
            if (getMaster() instanceof NavigatedDisplay) {
                coords =
                    ((NavigatedDisplay) getMaster()).getScreenCoordinates();
                if ( !Misc.equals(coords, lastCoords)) {
                    changed = true;
                }
            }
            if ( !changed) {
                return;
            }

            lastMatrix = currentMatrix;
            double[] trans = { 0.0, 0.0, 0.0 };
            double[] rot   = { 0.0, 0.0, 0.0 };
            double[] scale = { 0.0, 0.0, 0.0 };
            getMaster().getMouseBehavior().instance_unmake_matrix(rot, scale,
                    trans, currentMatrix);



            StringBuffer sb = new StringBuffer();
            sb.append(
                "<html><table width=100%><tr><td width=33%></td><td width=33%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;X&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td width=33%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Y&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td><td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Z&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td></tr>" + "<tr><td align=right>Rotation:</td><td align=right>" + fmt(rot[0]) + "</td><td align=right>" + fmt(rot[1]) + "</td><td align=right>" + fmt(rot[2]) + "</td></tr>" + "<tr><td align=right>Translation:</td><td align=right>" + fmt(trans[0]) + "</td><td align=right>" + fmt(trans[1]) + "</td><td align=right>" + fmt(trans[2]) + "</td></tr>" + "<tr><td align=right>Scale:</td><td align=right>" + fmt(scale[0]) + "</td><td>" + fmt(scale[1]) + "</td><td>" + fmt(scale[2]) + "</td></tr>");

            if (getMaster() instanceof NavigatedDisplay) {
                NavigatedDisplay navDisplay = (NavigatedDisplay) getMaster();
                sb.append(
                    "<tr><td></td><td colspan=3 align=center>Box</td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td><td colspan=2 align=center>Screen</td></tr>");
                for (TwoFacedObject tfo : coords) {
                    double[] xyz     = (double[]) tfo.getId();
                    int[]    scoords = navDisplay.getScreenCoordinates(xyz);
                    sb.append("<tr align=right><td align=right>");
                    sb.append(tfo.toString() + ":");
                    sb.append("</td><td>");
                    sb.append(fmt(xyz[0]));
                    sb.append("</td><td>");
                    sb.append(fmt(xyz[1]));
                    sb.append("</td><td>");
                    sb.append(fmt(xyz[2]));
                    sb.append("</td><td>");
                    sb.append("</td><td>");
                    sb.append(scoords[0]);
                    sb.append("</td><td>");
                    sb.append(scoords[1]);
                    sb.append("</td></tr>");
                }
            }
            sb.append("</table>");

            matrixLabel.setText(sb.toString());
        } catch (Exception exc) {
            LogUtil.consoleMessage("Error:" + exc);
        }

    }

    /**
     * Format a double
     *
     * @param d  the double
     *
     * @return the formatted double
     */
    private String fmt(double d) {
        if (d == -0.0) {
            d = 0.0;
        }
        return fmt.format(d);
    }


    /**
     * Apply properties
     *
     * @return true if successful
     */
    public boolean applyProperties() {
        if (lights != null) {
            for (LightInfo lightInfo : lights) {
                lightInfo.applyProperties();
            }
        }

        int width  = 0;
        int height = 0;
        if (fullScreenWidthFld.getText().trim().equals("")
                || fullScreenWidthFld.getText().trim().equals("0")) {
            width = 0;
        } else {
            width =
                new Integer(fullScreenWidthFld.getText().trim()).intValue();
        }


        if (fullScreenHeightFld.getText().trim().equals("")
                || fullScreenHeightFld.getText().trim().equals("0")) {
            height = 0;
        } else {
            height =
                new Integer(fullScreenHeightFld.getText().trim()).intValue();
        }

        if ((width != fullScreenWidth) || (height != fullScreenHeight)) {
            fullScreenWidth  = width;
            fullScreenHeight = height;
            //Reset the window size if we are in full screen
            if ((fullScreenWindow != null) && (width != 0) && (height != 0)) {
                Dimension  theSize      = new Dimension(width, height);
                JComponent navComponent = getComponent();
                navComponent.setMinimumSize(theSize);
                navComponent.setPreferredSize(theSize);
                GuiUtils.getWindow(navComponent).pack();
            }
        }






        try {
            setName(nameFld.getText());
            for (Enumeration keys = propertiesMap.keys();
                    keys.hasMoreElements(); ) {
                JCheckBox       cbx = (JCheckBox) keys.nextElement();
                BooleanProperty bp  =
                    (BooleanProperty) propertiesMap.get(cbx);
                bp.setValue(cbx.isSelected());
            }
            setColors(fgPropertiesSwatch.getSwatchColor(),
                      bgPropertiesSwatch.getSwatchColor());

            Font f = fontSelector.getFont();
            setDisplayListFont(f);
            setDisplayListColor(dlPropertiesSwatch.getSwatchColor());
            updateDisplayList();

            double[] newAspectRatio = new double[] {
                                          aspectSliders[0].getValue() / 10.0,
                                          aspectSliders[1].getValue() / 10.0,
                                          aspectSliders[2].getValue()
                                          / 10.0 };
            if ( !Arrays.equals(newAspectRatio, originalAspectSliderValues)) {
                //              System.err.println("aspect ratios differ:");
                //              Misc.printArray("old:" , originalAspectSliderValues);
                //              Misc.printArray("new:", newAspectRatio);
                aspectRatio = newAspectRatio;
                getMaster().setDisplayAspect(aspectRatio);
            }

            //setLogoVisibility(logoVisCbx.isSelected());
            logoFile = logoFileField.getText().trim();
            //setLogoAnchor(((TwoFacedObject) logoAnchorBox.getSelectedItem()).getId().toString());
            String logoPos =
                ((TwoFacedObject) logoPositionBox.getSelectedItem()).getId()
                    .toString();
            String logoOff = logoOffsetTextField.getText().trim();
            setLogoPosition(makeLogoPosition(logoPos, logoOff));
            setLogoScale(logoSizer.getValue() / 10.f);
            updateAnnotations();
            return true;
        } catch (Exception exp) {
            logException("Applying properties", exp);
            return false;
        }

    }

    /**
     * Update the annotations
     */
    protected void updateAnnotations() {

        try {
            DisplayImpl display = (DisplayImpl) getMaster().getDisplay();
            if (display instanceof DisplayImplJ3D) {
                if (annotator == null) {
                    annotator = new ScreenAnnotatorJ3D(display);
                }
                annotator.clear();
                Component comp = display.getComponent();
                if (comp == null) {
                    return;
                }
                Rectangle bounds = comp.getBounds();
                //System.out.println("window bounds: " + bounds);
                if ((bounds.width == 0) || (bounds.height == 0)
                        || (logoFile == null) || logoFile.isEmpty()
                        || !getLogoVisibility()) {
                    annotator.draw();
                    return;
                }

                Image logo = ImageUtils.readImage(logoFile, true, true);
                if (logo == null) {
                    //throw new VisADException("Logo file: " + logoFile + " does not exist.");
                    System.err.println("Logo file: " + logoFile
                                       + " does not exist.");
                    return;
                }

                Point ap = ImageUtils.parsePoint(getLogoPosition(), bounds);
                //System.out.println("screen point = " + ap);

                int   baseX = ap.x;
                int   baseY = ap.y;
                float zval  = getPerspectiveView()
                              ? 1
                              : display.getDisplayRenderer().getMode2D()
                                ? 1.5f
                                : 2;

                ImageJ3D logoJ3D = new ImageJ3D(logo,
                                       getImageAnchor(getLogoPosition()),
                                       baseX, baseY, zval, getLogoScale());
                annotator.add(logoJ3D);
                annotator.draw();
            }
        } catch (Exception exp) {
            logException("updating annotations", exp);
        }
    }

    /**
     * Get the image location from the screen location
     *
     * @param loc  the screen location
     *
     * @return  the anchor point
     */
    private int getImageAnchor(String loc) {
        int index = loc.indexOf(",");
        if (index > 0) {
            loc = loc.substring(0, index);
        }
        if (loc.equalsIgnoreCase("ll")) {
            return ImageJ3D.BOTTOM_LEFT;
        } else if (loc.equalsIgnoreCase("ul")) {
            return ImageJ3D.TOP_LEFT;
        } else if (loc.equalsIgnoreCase("ur")) {
            return ImageJ3D.TOP_RIGHT;
        } else if (loc.equalsIgnoreCase("lr")) {
            return ImageJ3D.BOTTOM_RIGHT;
        } else if (loc.equalsIgnoreCase("mm")) {
            return ImageJ3D.CENTER;
        }
        return ImageJ3D.BOTTOM_LEFT;
    }


    /**
     * Called after this object has been unpersisted from xml.
     *
     * @param idv the IDV associated with this.
     *
     * @throws RemoteException  Java RMI exception
     * @throws VisADException   Couldn't create the VisAD display
     */
    public void initAfterUnPersistence(IntegratedDataViewer idv)
            throws VisADException, RemoteException {
        setIdv(idv);
        setHighlightBorder(getStore().get(PREF_BORDERCOLOR, Color.blue));
        initBooleanProperties();
    }


    /**
     * Init with a view state
     *
     * @param viewState  the view state
     *
     * @throws Exception  problem initializing
     */
    public void initWith(ViewState viewState) throws Exception {
        double[] matrix = (double[]) viewState.get(ViewState.PROP_MATRIX);
        if (matrix != null) {
            setDisplayMatrix(matrix);
        }
    }


    /**
     * Initialize this ViewManager with the state in the that ViewManager
     *
     * @param that The other view manager
     */
    public final void initWith(ViewManager that) {
        initWith(that, false);
    }


    /** flag for sharing */
    private boolean wasSharing;

    /**
     * Initialize this ViewManager with the state in the that ViewManager.
     * This is just a wrapper around initWithInner so we can handle
     * exceptions cohesively.
     *
     * @param that The other view manager
     * @param ignoreWindow If false then we set the window bounds
     * of this ViewManager to the bounds held in that ViewManager.
     */
    public final void initWith(ViewManager that, boolean ignoreWindow) {
        try {
            wasSharing = getSharing();
            setSharing(false);
            initWithInner(that, ignoreWindow);
            setSharing(wasSharing);
        } catch (Exception exp) {
            logException("initializing ViewManager", exp);
        }
    }

    /**
     * Initialize this ViewManager with the state in the that ViewManager.
     *
     * @param that The other view manager
     * @param ignoreWindow If false then we set the window bounds
     * of this ViewManager to the bounds held in that ViewManager.
     *
     * @throws RemoteException  Java RMI exception
     * @throws VisADException   Couldn't create the VisAD display
     */
    protected void initWithInner(ViewManager that, boolean ignoreWindow)
            throws VisADException, RemoteException {
        if (that != this) {
            this.aspectRatio = that.aspectRatio;
            List newAliases = that.aliases;
            for (int aliasIdx = 0; aliasIdx < newAliases.size(); aliasIdx++) {
                ViewDescriptor vd = (ViewDescriptor) newAliases.get(aliasIdx);
                addViewDescriptor(vd);
            }
            this.properties.putAll(that.properties);


            if (that.lights != null) {
                this.lights = new ArrayList<LightInfo>();
                for (LightInfo lightInfo : that.lights) {
                    this.lights.add(new LightInfo(lightInfo));
                }
            }
        }


        //        if(true) return;

        if ((that.name != null) && (that.name.trim().length() > 0)) {
            setName(that.name);
        }

        if ( !ignoreWindow) {
            Rectangle bounds = that.windowBounds;
            if (bounds != null) {
                //                System.err.println ("  vm.initWith-2" );
                //                Misc.sleep(10000);
                setWindowBounds(bounds);
                //                System.err.println ("  vm.initWith-2 after" );
                //                Misc.sleep(10000);
            }
        }

        if ((that.animationInfo != null) && (this.animationWidget != null)) {
            this.animationWidget.setProperties(that.animationInfo);
        }

        if (initViewStateName == null) {
            if (that.initMatrix != null) {
                setDisplayMatrix(that.initMatrix);
            }
        }


        if (this != that) {
            this.setSharing(wasSharing = that.getSharing());
        }
        setBooleanProperties(that);
        setColors(that.getForeground(), that.getBackground());
        setDisplayListFont(that.getDisplayListFont());
        setLogoFile(that.getLogoFile());
        setLogoScale(that.getLogoScale());
        setLogoVisibility(that.getLogoVisibility());
        setLogoPosition(that.getLogoPosition());
        updateAnnotations();


        if ((sideLegend != null) && (that.sideLegend != null)) {
            sideLegend.initWith(that.sideLegend);
        }

        if (that.sideDividerLocation > 0) {
            sideDividerLocation = that.sideDividerLocation;
            if (mainSplitPane != null) {
                // mainSplitPane.setDividerLocation(sideDividerLocation);
            }
        }
    }





    /**
     * This gets called when the initial guis (windows, etc) have been created and shown
     */
    public void guiInitializationDone() {
        sideDividerLocation = -1;
        toFront();
    }




    /**
     * This is here to set the IDV when this object has been unpersisted.
     * @param idv The IDV
     */
    public void setIdv(IntegratedDataViewer idv) {
        this.idv = idv;
    }


    /**
     * Get the IDV
     *
     * @return The IDV
     */
    public IntegratedDataViewer getIdv() {
        return this.idv;
    }



    /**
     * Set the different boolean flags from thise held by the
     * given  view manager.
     *
     * @param vm The ViewManager to get state from
     *
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void setBooleanProperties(ViewManager vm)
            throws VisADException, RemoteException {
        if (booleanPropertyMap.size() == 0) {
            initBooleanProperties();
        }
        for (int i = 0; i < booleanProperties.size(); i++) {
            BooleanProperty myProperty =
                (BooleanProperty) booleanProperties.get(i);
            BooleanProperty hisProperty =
                vm.getBooleanProperty(myProperty.getId());
            if (hisProperty != null) {
                myProperty.setValue(hisProperty.getValue());
            }
        }
    }


    /**
     * The BooleanProperty identified byt he given id has changed.
     * Apply the change to the display.
     *
     * @param id Id of the changed BooleanProperty
     * @param value Its new value
     *
     * @throws Exception problem handeling the change
     */
    protected void handleBooleanPropertyChange(String id, boolean value)
            throws Exception {
        if (id.equals(PREF_SHAREVIEWS)) {
            setSharing(value);
        } else if (id.equals(PREF_WIREFRAME)) {
            DisplayRenderer renderer = getDisplayRenderer();
            if (renderer != null) {
                renderer.setBoxOn(value);
            }
        } else if (id.equals(PREF_ANIREADOUT)) {
            if (master != null) {
                master.setAnimationStringVisible(value);
            }
        } else if (id.equals(PREF_TOPBAR_VISIBLE)) {
            if (topBar != null) {
                topBar.setVisible(value);
            }
        } else if (id.equals(PREF_WAITMSG)) {
            DisplayMaster master = getMaster();
            if (master != null) {
                master.setWaitMessageVisible(value);
            }
        } else if (id.equals(PREF_SHOWDISPLAYLIST)) {
            updateDisplayList();
        } else if (id.equals(PREF_LOGO_VISIBILITY)) {
            updateAnnotations();
        }
    }

    /**
     * show the timeline window
     */
    public void showTimeline() {
        if (timelineDialog == null) {
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    String cmd = ae.getActionCommand();
                    if (cmd.equals(GuiUtils.CMD_OK)
                            || cmd.equals(GuiUtils.CMD_CANCEL)) {
                        timelineDialog.dispose();
                        //timelineDialog = null;
                    }
                    if (cmd.equals(GuiUtils.CMD_UPDATE)) {
                        updateTimelines(true);
                    }
                }
            };

            timelineDialog = GuiUtils.createDialog("Animation Timeline",
                    false);
            timelineDialog.getContentPane().add(
                GuiUtils.centerBottom(
                    getTimelineComponent(),
                    GuiUtils.makeButtons(
                        listener, new String[] { GuiUtils.CMD_UPDATE,
                    GuiUtils.CMD_CANCEL })));
            updateTimelines(true);
            timelineDialog.pack();
            timelineDialog.setLocation(200, 200);
        }
        timelineDialog.setVisible(true);
        GuiUtils.toFront(timelineDialog);
    }


    /**
     * set the animation time
     *
     * @param date date
     */
    private void setAnimationTime(Date date) {
        if (animation != null) {
            try {
                animation.setAniValue(new DateTime(date));
            } catch (Exception exp) {
                logException("Setting animation time", exp);
            }
        }
    }

    /**
     * The time changed
     */
    protected void animationTimeChanged() {}


    /**
     * update the timelines display
     *
     * @param force remake the timelines
     */
    private void updateTimelines(boolean force) {
        if (timelineDialog != null) {
            if (force) {
                JComponent comp = getTimelineComponent();
            }
            timelineDialog.invalidate();
            timelineDialog.validate();
            timelineDialog.repaint();
            Real value = animation.getCurrentAnimationValue();
            try {
                for (int i = 0; i < timelines.size(); i++) {
                    MyTimeline timeline = (MyTimeline) timelines.get(i);
                    timeline.update(value);
                }
            } catch (Exception exp) {
                logException("Updating timeline", exp);
            }
        }
    }


    /**
     * Class MyTimeline for the animation timeline
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.401 $
     */
    private class MyTimeline extends Timeline {

        /** the times */
        Set timeSet;

        /** member */
        JLabel label;

        /** the right component */
        JLabel rightComp;

        /** member */
        Real animationValue;

        /** member */
        DisplayControl control;

        /**
         * ctor
         *
         *
         * @param timeSet the times
         * @param dates dates
         * @param w width
         * @param control the display control
         */
        public MyTimeline(Set timeSet, List dates, int w,
                          DisplayControl control) {
            super(dates, w);
            this.timeSet = timeSet;
            this.control = control;
            this.label   = control.makeLegendLabel();
            if (idv.getUseTimeDriver()) {
                this.rightComp = GuiUtils.rLabel(control.getIsTimeDriver()
                        ? "Time Driver"
                        : "");
            } else {
                this.rightComp = GuiUtils.rLabel("");
            }
            setIsCapableOfSelection(false);
        }

        /**
         * ctor
         *
         * @param dates dates
         * @param w width
         * @param h height
         */
        public MyTimeline(List dates, int w, int h) {
            super(dates, w, h);
            setIsCapableOfSelection(false);
        }


        /**
         * These are a list of earth locations used to draw sunrise/sunset
         *
         * @return locations for sunrise/sunset
         */
        public List getSunriseLocations() {
            return getIdv().getIdvUIManager().getMapLocations();
        }

        /**
         * override base class method to use different color
         *
         * @return color
         */
        public Color getColorTimeUnselected() {
            return Color.black;
        }



        /**
         * handle double click
         *
         * @param e event
         */
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() < 2) {
                super.mouseClicked(e);
                return;
            }
            setAnimationTime(toDate(e.getX()));
        }


        /**
         * time changed
         *
         * @param value new time
         *
         * @throws RemoteException on badness
         * @throws VisADException on badness
         */
        public void update(Real value)
                throws VisADException, RemoteException {
            if (control != null) {
                timeSet = control.getTimeSet();
                if (timeSet != null) {
                    DateTime[] times = Animation.getDateTimeArray(timeSet);
                    setDatedThings(
                        DatedObject.wrap(ucar.visad.Util.makeDates(times)));
                }
            }
            setAnimationValue(value);
        }

        /**
         * set animation value and repaint
         *
         * @param value value
         *
         * @throws RemoteException On badness
         * @throws VisADException On badness
         */
        public void setAnimationValue(Real value)
                throws VisADException, RemoteException {
            this.animationValue = value;
            if ((timeSet != null) && (value != null)) {
                int  index       = ucar.visad.Util.findIndex(timeSet, value);
                List datedThings = getDatedThings();
                if ((index >= 0) && (index < datedThings.size())) {
                    setSelected(Misc.newList(datedThings.get(index)));
                    return;
                }
                if (getSelected().size() > 0) {
                    setSelected(new ArrayList());
                }
            }
            repaint();
        }

        /**
         *  paint animation time
         *
         * @param g graphics
         */
        public void paintBackgroundDecoration(Graphics2D g) {
            super.paintBackgroundDecoration(g);
            Dimension d = getSize();
            if ((control != null) && !control.getDisplayVisibility()) {
                g.setColor(Color.lightGray);
                g.fillRect(0, 0, d.width, d.height);
            }

            if (animationValue instanceof DateTime) {
                try {
                    Date date =
                        ucar.visad.Util.makeDate((DateTime) animationValue);
                    int x = toLocation(date);
                    g.setColor(Color.gray);
                    g.fillRect(x - 1, 0, 2, d.height);
                } catch (Exception exp) {
                    logException("Painting timeline", exp);
                }
            }
        }
    }



    /**
     * make the timeline gui
     *
     * @return timeline
     */
    public JComponent getTimelineComponent() {

        try {
            synchronized (TIMELINES_MUTEX) {

                Insets lblInsets    = new Insets(5, 0, 0, 0);
                List   controls     = getControls();
                List   allTimes     = new ArrayList();
                List   comps        = new ArrayList();
                List   oldTimelines = new ArrayList(timelines);
                timelines = new ArrayList();

                if (animationWidget != null) {
                    List datedThings = DatedObject.wrap(
                                           Util.makeDates(
                                               animationWidget.getTimes()));
                    if (animationTimeline == null) {
                        animationTimeline = new MyTimeline(datedThings, 300,
                                100);
                    }
                    if (datedThings.size() > 0) {
                        allTimes.addAll(datedThings);
                        timelines.add(animationTimeline);
                        comps.add(
                            GuiUtils.inset(
                                GuiUtils.lLabel("Animation Times"),
                                lblInsets));
                        comps.add(animationTimeline.getContents(false));
                    }
                }

                for (int i = controls.size() - 1; i >= 0; i--) {
                    DisplayControl control = (DisplayControl) controls.get(i);
                    Set            timeSet = control.getTimeSet();
                    if (timeSet == null) {
                        continue;
                    }
                    DateTime[] times = Animation.getDateTimeArray(timeSet);
                    List datedObjects =
                        DatedObject.wrap(ucar.visad.Util.makeDates(times));
                    if (datedObjects.size() == 0) {
                        continue;
                    }
                    allTimes.addAll(datedObjects);

                    MyTimeline timeline = null;
                    for (int j = 0; j < oldTimelines.size(); j++) {
                        MyTimeline oldTimeline =
                            (MyTimeline) oldTimelines.get(j);
                        if (oldTimeline.control == control) {
                            timeline = oldTimeline;
                            oldTimelines.remove(oldTimeline);
                            break;
                        }
                    }


                    if (timeline == null) {
                        //                    System.err.println ("new timeline");
                        timeline = new MyTimeline(timeSet, datedObjects, 300,
                                control);
                    }
                    if (timelines.size() > 0) {
                        timeline.setShortDisplay(true);
                    }
                    timelines.add(timeline);
                    comps.add(
                        GuiUtils.inset(
                            GuiUtils.leftRight(
                                timeline.label,
                                timeline.rightComp), lblInsets));
                    comps.add(timeline.getContents(false));
                }
                allTimes = DatedObject.sort(allTimes, true);
                if (allTimes.size() > 1) {
                    Date startDate = ((DatedThing) allTimes.get(0)).getDate();
                    Date endDate = ((DatedThing) allTimes.get(allTimes.size()
                                       - 1)).getDate();
                    for (int i = 0; i < timelines.size(); i++) {
                        MyTimeline timeline = (MyTimeline) timelines.get(i);
                        timeline.setStartDate(startDate);
                        timeline.setEndDate(endDate);
                        timeline.expandByPercent(1.1, false);
                    }
                }
                for (int i = 0; i < timelines.size(); i++) {
                    MyTimeline timeline = (MyTimeline) timelines.get(i);
                    timeline.setGroup(timelines);
                }

                GuiUtils.tmpInsets = new Insets(1, 1, 1, 1);
                JComponent comp = GuiUtils.doLayout(comps, 1, GuiUtils.WT_Y,
                                      GuiUtils.WT_N);
                JScrollPane sp =
                    new JScrollPane(
                        GuiUtils.top(GuiUtils.inset(comp, 5)),
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                sp.setPreferredSize(new Dimension(300, 400));
                if (timelineHolder == null) {
                    timelineHolder = new JPanel(new BorderLayout());
                }
                timelineHolder.removeAll();
                //            timelineHolder.add(BorderLayout.CENTER, GuiUtils.top(sp));
                timelineHolder.add(BorderLayout.CENTER, sp);
                timelineHolder.invalidate();
                timelineHolder.repaint();
                return timelineHolder;
            }
        } catch (Exception exp) {
            logException("Creating timeline", exp);
        }
        return new JPanel();


    }


    /**
     * Populate the display list
     */
    public void updateDisplayList() {
        try {
            synchronized (MUTEX_DISPLAYLIST) {
                if ( !hasDisplayMaster()) {
                    return;
                }
                if (displayListDisplayables == null) {
                    displayListDisplayables = new CompositeDisplayable();
                    displayListDisplayables.setUseTimesInAnimation(false);
                } else {
                    getMaster().removeDisplayable(displayListDisplayables);
                    displayListDisplayables.clearDisplayables();
                }
                if ( !getShowDisplayList()) {
                    return;
                }

                List controls = getControls();

                Hashtable<String, List<TextDisplayable>> catMap =
                    new Hashtable<String, List<TextDisplayable>>();
                List<String> cats = new ArrayList<String>();
                List<TextDisplayable> textDisplayables =
                    new ArrayList<TextDisplayable>();


                //Seed the list first so we get the right order
                if (sideLegend != null) {
                    for (String cat : sideLegend.getDisplayCategories()) {
                        cats.add(cat);
                        catMap.put(cat, new ArrayList<TextDisplayable>());
                    }
                }

                for (int i = controls.size() - 1; i >= 0; i--) {
                    DisplayControl control = (DisplayControl) controls.get(i);
                    if ( !control.getShowInDisplayList()
                            || !control.getDisplayVisibility()) {
                        continue;
                    }
                    TextDisplayable d =
                        (TextDisplayable) control.getDisplayListDisplayable(
                            this);
                    if (d == null) {
                        continue;
                    }

                    String cat = control.getDisplayCategory();
                    if (cat == null) {
                        cat = "Displays";
                    }
                    List<TextDisplayable> l = catMap.get(cat);
                    if (l == null) {
                        l = new ArrayList<TextDisplayable>();
                        catMap.put(cat, l);
                        cats.add(cat);
                    }
                    l.add(d);
                    textDisplayables.add(d);
                }


                boolean grouped = getStateManager().getPreferenceOrProperty(
                                      IdvConstants.PROP_DISPLAYLIST_GROUP,
                                      false);
                if (grouped) {
                    int count = 0;
                    for (int i = cats.size() - 1; i >= 0; i--) {
                        String                cat          = cats.get(i);
                        List<TextDisplayable> displayables = catMap.get(cat);
                        for (int j = displayables.size() - 1; j >= 0; j--) {
                            TextDisplayable d = displayables.get(j);
                            count++;
                            if (setDisplayablePosition(d, count)) {
                                displayListDisplayables.addDisplayable(d);
                            }
                        }
                    }
                } else {
                    for (int count = 0; count < textDisplayables.size();
                            count++) {
                        TextDisplayable d = textDisplayables.get(count);
                        if (setDisplayablePosition(d, count + 1)) {
                            displayListDisplayables.addDisplayable(d);
                        }
                    }
                }

                getMaster().addDisplayable(displayListDisplayables);
                //displayListDisplayables.setVisible(true);
            }
        } catch (Exception exp) {
            logException("Setting display list", exp);
        }
    }

    /**
     * Set the position of the displayable
     *
     * @param d  the displayable
     * @param number  the number in the list.
     *
     * @return true if successful
     */
    private boolean setDisplayablePosition(Displayable d, int number) {

        try {
            DisplayMaster master  = getMaster();
            DisplayImpl   display = (DisplayImpl) master.getDisplay();
            Rectangle     r       = master.getScreenBounds();
            double[]      xyz     = new double[3];
            if ( !r.isEmpty()) {
                // System.out.println("screen bounds = " + r);
                float scale = getDisplayListFont().getSize() / 12.f;
                int   x     = r.x + (int) (.5f * r.width);
                int y = r.y
                        + (int) ((1.f - ((.025 * scale) * number))
                                 * r.height);
                xyz = Util.getVWorldCoords(display, x, y, xyz);
                double zval = getPerspectiveView()
                              ? xyz[2]
                              : 2.0;

                d.addConstantMaps(new ConstantMap[] {
                    new ConstantMap(xyz[0], Display.XAxis),
                    new ConstantMap(xyz[1], Display.YAxis),
                // new ConstantMap(2.0, Display.ZAxis) });  // set at top of box
                new ConstantMap(zval, Display.ZAxis) });  // set at top of box
                return true;
            }
        } catch (VisADException ve) {
            ve.printStackTrace();
        } catch (RemoteException re) {}
        return false;
    }

    /**
     * Get whether this is in a perspective view or not
     *
     * @return false, subclasses should override if necessary
     */
    public boolean getPerspectiveView() {
        return false;
    }



    /**
     * Get the intial BooleanProperties
     *
     * @param props  the properties
     */
    protected void getInitialBooleanProperties(List props) {
        props.add(new BooleanProperty(PREF_WIREFRAME, "Show Wireframe Box",
                                      "", true));
        props.add(new BooleanProperty(PREF_SHAREVIEWS, "Share Views", "",
                                      getSharing()));
        props.add(new BooleanProperty(PREF_ANIREADOUT, "Show Times In View",
                                      "Toggle animation readout in display",
                                      false));
        props.add(
            new BooleanProperty(
                PREF_WAITMSG, "Show \"Please Wait\" Message",
                "Toggle \"Please Wait\" message in display", true));
        props.add(new BooleanProperty(PREF_SHOWDISPLAYLIST,
                                      "Show Display List",
                                      "Show display labels in the display",
                                      true));
        props.add(new BooleanProperty(PREF_LOGO_VISIBILITY, "Show Logo",
                                      "Toggle logo in display", false));
        props.add(new BooleanProperty(PREF_TOPBAR_VISIBLE, "Show Top Bar",
                                      "Toggle top bar", true));

    }



    /**
     * Debug
     *
     * @param msg message to print
     */
    private void debug(String msg) {
        // System.err.println (msg);
    }

    /**
     * Create the set of {@link ucar.unidata.util.BooleanProperty}s.
     * These hold all of the different flag based display state.
     */
    protected void initBooleanProperties() {
        // debug("initBooleanProperties");
        if (booleanPropertiesForPersistence == null) {
            // debug("\tno bpforpersistence");
        }
        List props = new ArrayList();
        getInitialBooleanProperties(props);
        StateManager   stateManager = getStateManager();
        XmlObjectStore store        = getStore();
        if (booleanPropertiesForPersistence != null) {
            booleanPropertiesForPersistence =
                stateManager.processPropertyTable(
                    booleanPropertiesForPersistence);
        }
        for (int i = 0; i < props.size(); i++) {
            BooleanProperty bp = (BooleanProperty) props.get(i);
            initializeBooleanProperty(bp);
        }
    }


    /**
     * Initialize a BooleanProperty
     *
     * @param bp  the property
     */
    protected void initializeBooleanProperty(BooleanProperty bp) {
        StateManager   stateManager = getStateManager();
        XmlObjectStore store        = getStore();
        boolean        defaultValue = bp.getDefault();
        if (stateManager != null) {
            defaultValue = getStateManager().getProperty(bp.getId(),
                    defaultValue);
        }

        if (store != null) {
            defaultValue = store.get(bp.getId(), defaultValue);
        }
        if (booleanPropertiesForPersistence != null) {
            Boolean b =
                (Boolean) booleanPropertiesForPersistence.get(bp.getId());
            //                System.err.println("has for persistence: " + b);
            if (b != null) {
                bp.setValue(b.booleanValue());
            }
        }
        // debug("\tbp: " + bp);
        BooleanProperty existingBp =
            (BooleanProperty) booleanPropertyMap.get(bp.getId());
        if ((existingBp != null)
                && (booleanPropertiesForPersistence == null)) {
            // debug("\thave existing " + existingBp);
            bp.setValue(existingBp.getValue());
        }


        BooleanProperty newBp = new BooleanProperty(bp) {
            public void setValueInner(boolean value) throws Exception {
                super.setValueInner(value);
                if (getHaveInitialized()) {
                    handleBooleanPropertyChange(getId(), value);
                }
            }
        };
        newBp.setDefault(defaultValue);
        addBooleanProperty(newBp);

    }



    /**
     * Check to see if this view manager has the property
     *
     * @param propertyId  property id
     *
     * @return true if it has this property
     */
    protected boolean hasBooleanProperty(String propertyId) {
        return booleanPropertyMap.get(propertyId) != null;
    }


    /**
     * Find, or create,  the BooleanProperty identified with the  given id
     *
     * @param propertyId Identifies the BooleanProperty
     * @return The BooleanProperty
     */
    protected BooleanProperty getBooleanProperty(String propertyId) {
        return getBooleanProperty(propertyId, true);
    }

    /**
     * Get a BooleanProperty
     *
     * @param propertyId  the property
     * @param dflt  the default value
     *
     * @return the property
     */
    protected BooleanProperty getBooleanProperty(String propertyId,
            boolean dflt) {
        if (booleanPropertyMap.size() == 0) {
            initBooleanProperties();
        }
        BooleanProperty bp =
            (BooleanProperty) booleanPropertyMap.get(propertyId);
        if (bp == null) {
            bp = new BooleanProperty(propertyId, propertyId, propertyId,
                                     dflt);
            booleanPropertyMap.put(bp.getId(), bp);
            initializeBooleanProperty(bp);
        }
        return bp;
    }


    /**
     * Helper to set the value of a BooleanProperty
     *
     * @param propertyId Identifies the BooleanProperty
     * @param value The value to set
     */
    protected void setBp(String propertyId, boolean value) {
        getBooleanProperty(propertyId).setValue(value);
    }


    /**
     * Get the value of the BooleanProperty
     *
     * @param propertyId Identifies the BooleanProperty
     * @return The value
     */
    protected boolean getBp(String propertyId) {
        return getBp(propertyId, true);
    }


    /**
     * Get a BooleanProperty (local implementation)
     *
     * @param propertyId  the id
     * @param dflt        the default value
     *
     * @return  the value
     */
    protected boolean getBp(String propertyId, boolean dflt) {
        return getBooleanProperty(propertyId, dflt).getValue();
    }


    /**
     * Set the  share view state flag
     *
     * @param value The value
     */
    public void setShareViews(boolean value) {
        setBp(PREF_SHAREVIEWS, value);
    }

    /**
     *  Get  the share view state flag
     * @return The flag value
     */
    public boolean getShareViews() {
        return getBp(PREF_SHAREVIEWS);
    }

    /**
     *  Set  the show display list flag
     * @param value The flag value
     */
    public void setShowDisplayList(boolean value) {
        setBp(PREF_SHOWDISPLAYLIST, value);
    }


    /**
     *  Get  the show display list flag
     * @return The flag value
     */
    public boolean getShowDisplayList() {
        return getBp(PREF_SHOWDISPLAYLIST);
    }

    /**
     * Set the please wait visible state flag
     *
     * @param value The value
     */
    public void setWaitMessageVisible(boolean value) {
        setBp(PREF_WAITMSG, value);
    }

    /**
     * Get the please wait visible state flag
     * @return The flag value
     */
    public boolean getWaitMessageVisible() {
        return getBp(PREF_WAITMSG);
    }


    /**
     * Add in the BooleanProperty into the list of properties.
     *
     * @param bp The new BooleanProperty
     */
    protected void addBooleanProperty(BooleanProperty bp) {
        synchronized (booleanPropertyMap) {
            if (booleanPropertyMap.get(bp.getId()) != null) {
                for (int i = 0; i < booleanProperties.size(); i++) {
                    BooleanProperty other =
                        (BooleanProperty) booleanProperties.get(i);
                    if (other.getId().equals(bp.getId())) {
                        booleanProperties.remove(other);
                        break;
                    }
                }
            }
            booleanPropertyMap.put(bp.getId(), bp);
            booleanProperties.add(bp);
        }
    }



    /**
     * Set the BooleanPropertiesForPersistence property.
     *
     * @param value The new value for BooleanPropertiesForPersistence
     */
    public void setBooleanPropertiesForPersistence(Hashtable value) {
        booleanPropertiesForPersistence = value;
    }

    /**
     * Get the BooleanPropertiesForPersistence property.
     *
     * @return The BooleanPropertiesForPersistence
     */
    public Hashtable getBooleanPropertiesForPersistence() {
        Hashtable tmp = new Hashtable();
        for (Iterator iter = booleanPropertyMap.values().iterator();
                iter.hasNext(); ) {
            BooleanProperty bp = (BooleanProperty) iter.next();
            if (bp.hasValue()) {
                tmp.put(bp.getId(), new Boolean(bp.getValue()));
            }
        }
        return tmp;
    }



    /**
     * A helper to create a check box menu item from the
     * id for a BooleanProperty.
     *
     * @param menu The menu to add the checkbox menu item into
     * @param id Identifies the boolean property that this cbmi controls
     * @return The new check box menu item
     */
    protected JCheckBoxMenuItem createCBMI(JMenu menu, final String id) {
        final JCheckBoxMenuItem mi = getBooleanProperty(id).createCBMI();
        String                  s  = mi.getText();
        if (s.startsWith("Show ")) {
            s = s.substring(5);
            mi.setText(s);
        }
        if (menu != null) {
            menu.add(mi);
        }
        return mi;
    }


    /**
     * Set the  show wireframe flag
     *
     * @param value The value
     */
    public void setWireframe(boolean value) {
        setBp(PREF_WIREFRAME, value);
    }

    /**
     * Get  the show wireframe box  flag
     * @return The flag value
     */
    public boolean getWireframe() {
        return getBp(PREF_WIREFRAME);
    }


    /**
     * Set the show display scale flag
     *
     * @param value The value
     */
    public void setShowScales(boolean value) {
        setBp(PREF_SHOWSCALES, value);
    }

    /**
     * Get  the show display scale flag
     * @return The flag value
     */
    public boolean getShowScales() {
        return getBp(PREF_SHOWSCALES);
    }




    /**
     * Get the component that holds the side legend
     *
     * @return side legend container
     * @deprecated No longer called sideLegendContainer
     */
    public JSplitPane getSideLegendContainer() {
        return mainSplitPane;
    }

    /**
     *  Get the component that might hold the sideLegend
     *  By default this just returns the side legend component.
     *  This allows derived classes to add their own stuff to the side.
     *
     * @param sideLegend The side legend
     *
     * @return The side legend.
     */
    protected JComponent getSideComponent(JComponent sideLegend) {
        return sideLegend;
    }


    /**
     * Set the InitialSplitPaneLocation property.
     *
     * @param value The new value for InitialSplitPaneLocation
     */
    public void setInitialSplitPaneLocation(double value) {
        initialSplitPaneLocation = value;
    }


    /**
     * Set the state of the legend
     *
     * @param legend The legend
     * @param state its state, e.g., hidden, shown, floating
     */
    public void setLegendState(IdvLegend legend, String state) {
        legendState = state;
        insertSideLegend();
    }



    /**
     * Some user preferences have changed.
     */
    public void applyPreferences() {
        for (Iterator iter = booleanPropertyMap.values().iterator();
                iter.hasNext(); ) {
            BooleanProperty bp = (BooleanProperty) iter.next();
            bp.setValue(getStore().get(bp.getId(), bp.getValue()));
        }

        checkToolBarVisibility();
        setColors(getStore().get(PREF_FGCOLOR, Color.white),
                  getStore().get(PREF_BGCOLOR, Color.black));
        setDisplayListFont(getStore().get(PREF_DISPLAYLISTFONT, defaultFont));
        setDisplayListColor(getStore().get(PREF_DISPLAYLISTCOLOR,
                                           (Color) null));
        if (animationOk()) {
            animationInfo.setBoxesVisible(getShowAnimationBoxes());
            animationWidget.setProperties(animationInfo);
        }
        setLogoFile(getStore().get(PREF_LOGO, ""));
        setLogoVisibility(getStore().get(PREF_LOGO_VISIBILITY, false));
        setLogoScale(getStore().get(PREF_LOGO_SCALE, 1.0f));
        setLogoPosition(getStore().get(PREF_LOGO_POSITION_OFFSET,
                                       "ll,10,-10"));
        updateDisplayList();
        updateAnnotations();
    }

    /**
     * Turn on/off the toolbar components based on the user preferences.
     */
    public void checkToolBarVisibility() {
        for (int i = 0; i < toolbars.size(); i++) {
            ((Component) toolbars.get(i)).setVisible(
                getStore().get(PREF_SHOWTOOLBAR + toolbarIds.get(i), true));
        }
    }



    /**
     * Setter method for xml persistence. Does this
     * ViewManager have a window.
     *
     * @param value Does it have a window
     */
    public void setHasWindow(boolean value) {
        hasWindow = value;
    }

    /**
     * Getter method for xml persistence. Does this
     * ViewManager have a window.
     *
     * @return Is the myWindow member non-null.
     */
    public boolean getHasWindow() {
        return (getDisplayWindow() != null);
    }



    /**
     * This returns the value of the hasWindow member which
     * was set when unpersisting.
     *
     * @return hasWindow
     */
    public boolean getReallyHasWindow() {
        return hasWindow;
    }


    /*
     * Getter method for xml persistence
     *
     * @return The bounds of the window
    public Rectangle getWindowBounds() {
        IdvWindow myWindow = getDisplayWindow();
        if (myWindow != null) {
            return myWindow.getBounds();
        }
        return windowBounds;
    }
     */


    /**
     * Setter method for xml persistence
     *
     * @param r The bounds of the window
     */
    public void setWindowBounds(Rectangle r) {
        windowBounds = r;
        IdvWindow myWindow = getDisplayWindow();
        if ((myWindow != null) && (r != null)) {
            boolean wasVisible = myWindow.isVisible();
            //            myWindow.setVisible(false);
            myWindow.setBounds(r);
            //            myWindow.setVisible(wasVisible);
        }
    }



    /**
     * Set the window that this ViewManager is shown in.
     * This adds this object as a <code>WindowListener</code>
     * and sets the bounds of the window if the windowBounds
     * is non-null.
     *
     * @param w The window
     */
    public void setWindow(final IdvWindow w) {
        if (w == null) {
            return;
        }

        if (windowBounds != null) {
            w.setWindowBounds(windowBounds);
            windowBounds = null;
        }
        final WindowAdapter[] wa = { null };

        w.addWindowListener(wa[0] = new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                destroy();
                w.removeWindowListener(wa[0]);
            }
            public void windowClosing(WindowEvent e) {
                doClose();
            }
        });
    }

    /**
     * Get the legends
     *
     * @return List of legends
     */
    protected List<IdvLegend> getLegends() {
        if (legends == null) {
            return new ArrayList<IdvLegend>();
        }
        synchronized (legends) {
            return new ArrayList<IdvLegend>(legends);
        }
    }


    /**
     * Called when the window is closed. This method closes any open legends.
     */
    protected void doClose() {
        if (legends == null) {
            return;
        }
        for (IdvLegend legend : getLegends()) {
            legend.doClose();
        }
    }


    /**
     * Show (float) all legends
     */
    public void showLegend() {
        for (IdvLegend legend : getLegends()) {
            legend.showLegend();
        }
    }


    /**
     * We call this getDisplayWindow instead of getWindow so the
     * window is not treated as a persistable property.
     *
     * @return The window
     */
    public IdvWindow getDisplayWindow() {
        if (fullContents == null) {
            return null;
        }
        return IdvWindow.findWindow(fullContents);
    }


    /**
     * Set the default size of the DisplayMaster held by this view.
     *
     * @param d The default size
     */
    public void setSize(Dimension d) {
        defaultSize = d;
    }

    /**
     * Get the default size of the DisplayMaster held by this view.
     *
     * @return The default size
     */
    public Dimension getMySize() {
        if (defaultSize == null) {
            defaultSize = new Dimension(600, 400);
        }
        return defaultSize;
    }


    /**
     * A hook so derived classes can add in their own preference widgets.
     *
     * @param preferenceManager The preference manager
     */
    public void initPreferences(IdvPreferenceManager preferenceManager) {}


    /**
     * Wrapper around {@link IntegratedDataViewer#getStore()}
     *
     * @return The XmlObjectStore to use for preferences.
     */
    public XmlObjectStore getStore() {
        return ((idv == null)
                ? null
                : idv.getStore());
    }



    /**
     * Get the list of  Projections available.
     *
     * @return The list of projections
     */
    public List getProjectionList() {
        return getIdv().getIdvProjectionManager().getProjections();
    }


    /**
     * Get the {@link IdvResourceManager} to use.
     *
     * @return The resource manager from the IDV.
     */
    public IdvResourceManager getResourceManager() {
        return ((idv == null)
                ? null
                : idv.getResourceManager());
    }


    /**
     * Get the {@link StateManager} to use.
     *
     * @return The state manager from the IDV.
     */
    public StateManager getStateManager() {
        return ((idv == null)
                ? null
                : idv.getStateManager());
    }

    /**
     * Get the {@link ucar.unidata.idv.publish.PublishManager} to use.
     *
     * @return The publish manager from the IDV.
     */
    public PublishManager getPublishManager() {
        return ((idv == null)
                ? null
                : idv.getPublishManager());
    }


    /**
     * Get the {@link ucar.unidata.idv.ui.IdvUIManager} to use.
     *
     * @return The UI manager from the IDV.
     */
    public IdvUIManager getIdvUIManager() {
        return ((idv == null)
                ? null
                : idv.getIdvUIManager());
    }


    /**
     *  Get the {@link VMManager} to use.
     *
     *  @return The view manager manager from the IDV.
     */
    public VMManager getVMManager() {
        return ((idv == null)
                ? null
                : idv.getVMManager());
    }



    /**
     * Parse and apply the properties in the semi-colon delimited
     * list of name=value pairs.
     *
     * @param properties The list of  properties
     */
    public void parseProperties(String properties) {
        if (properties == null) {
            return;
        }
        List props = StringUtil.split(properties, ";");
        for (int i = 0; i < props.size(); i++) {
            String nameValue = (String) props.get(i);
            int    idx       = nameValue.indexOf("=");
            if (idx < 0) {
                continue;
            }
            String propName  = nameValue.substring(0, idx).trim();
            String propValue = nameValue.substring(idx + 1);
            setProperty(propName, propValue, false);
        }
    }

    /**
     * Apply the given named property. This uses
     * {@link ucar.unidata.util.Misc#propertySet (Object, String, String)}
     * to set the property on this object via reflection.
     *
     * @param name Name of property
     * @param value Its value
     * @param ignoreError  true to ignore error
     *
     * @return true if successful
     */
    public boolean setProperty(String name, String value,
                               boolean ignoreError) {
        try {
            return Misc.propertySet(this, name, value, ignoreError);
        } catch (Exception exc) {
            logException("setProperty:" + name + " value= " + value, exc);
        }
        return false;
    }






    /**
     * Is the DisplayMaster currently active.
     *
     * @return Is display master active
     */
    public boolean getIsActive() {
        if (master == null) {
            return false;
        }
        return master.isActive();
    }



    /**
     * Set the {@link ucar.visad.display.DisplayMaster} inactive.
     */
    public void setMasterInactive() {
        //Make sure we have an initialized DisplayMaster
        if (master == null) {
            return;
        }
        try {
            master.setDisplayInactive();
        } catch (Exception exp) {
            logException("setMasterInactive", exp);
        }
    }



    /**
     * Set the {@link ucar.visad.display.DisplayMaster} active
     * if there are no more outstanind mast active calls.
     */
    public void setMasterActive() {
        setMasterActive(false);
    }

    /**
     * Set the {@link ucar.visad.display.DisplayMaster} active
     * if there are no more outstanind mast active calls.
     *
     * @param force  if true, force it active
     */
    public void setMasterActive(boolean force) {
        //Make sure we have an initialized DisplayMaster
        if (master == null) {
            return;
        }
        try {
            if (force) {
                master.setActive(true);
            } else {
                master.setDisplayActive();
            }
        } catch (Exception exp) {
            logException("setMasterActive", exp);
        }
    }



    /**
     * Get the location of the side legend or -1 if there is no side legend
     *
     * @return Side legend location
     */
    public int getSideDividerLocation() {
        JSplitPane tmp = mainSplitPane;
        if (tmp != null) {
            return tmp.getDividerLocation();
        }
        return -1;
    }


    /**
     * Set the location  of the side legend. This really just saves it
     * as an attribute which is used later.
     *
     * @param l Side legend location
     */
    public void setSideDividerLocation(int l) {
        sideDividerLocation = l;
    }



    /**
     * Class MatrixCommand manages changes to the viewpoint matrix
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.401 $
     */
    public static class MatrixCommand extends Command {

        /** The viewmanager */
        ViewManager viewManager;

        /** The old matrix */
        double[] oldMatrix;

        /** The new matrix */
        double[] newMatrix;

        /**
         * ctor
         *
         * @param viewManager The vm
         * @param oldMatrix  The old matrix
         * @param newMatrix  The new matrix
         */
        public MatrixCommand(ViewManager viewManager, double[] oldMatrix,
                             double[] newMatrix) {
            this.viewManager = viewManager;
            this.oldMatrix   = oldMatrix;
            this.newMatrix   = newMatrix;
        }

        /**
         * Redo
         */
        public void redoCommand() {
            try {
                viewManager.getMaster().setProjectionMatrix(newMatrix);
            } catch (Exception exp) {
                viewManager.logException("setProjectionMatrix", exp);
            }
        }

        /**
         * Undo
         */
        public void undoCommand() {
            try {
                viewManager.getMaster().setProjectionMatrix(oldMatrix);
            } catch (Exception exp) {
                viewManager.logException("setProjectionMatrix", exp);
            }
        }
    }



    /**
     * Add the command to the command manager. Use this for undo/redo commands.
     *
     * @param cmd The command
     */
    protected void addCommand(Command cmd) {
        getCommandManager().add(cmd, false);
    }

    /**
     * Process sthe key event
     *
     * @param keyEvent The key event
     */
    public void keyWasTyped(KeyEvent keyEvent) {
        char c    = keyEvent.getKeyChar();
        int  code = keyEvent.getKeyCode();
        int  id   = keyEvent.getID();
        if (id == KeyEvent.KEY_PRESSED) {
            if (GuiUtils.isControlKey(keyEvent)) {
                if (code == KeyEvent.VK_Z) {
                    //                    getCommandManager().move(-1);
                } else if (code == KeyEvent.VK_Y) {
                    //                    getCommandManager().move(1);
                }
                return;
            }
        }


        if (id != KeyEvent.KEY_RELEASED) {
            return;
        }



        if (code == KeyEvent.VK_F1) {
            runVisibilityAnimation = false;
            stepVisibilityToggle();
        } else if (code == KeyEvent.VK_F2) {
            turnOnOffAllDisplays(true);
        } else if (code == KeyEvent.VK_F3) {
            turnOnOffAllDisplays(false);
        } else if (GuiUtils.isControlKey(keyEvent, KeyEvent.VK_I)) {
            doSaveImageInThread();
            return;
        } else if (GuiUtils.isControlKey(keyEvent, KeyEvent.VK_M)) {
            startImageCapture();
            return;
        } else if (GuiUtils.isControlKey(keyEvent, KeyEvent.VK_B)) {
            getIdv().doSaveAs();
            return;
        } else if ( !Character.isDigit(c)) {
            return;
        } else {

            /**
             *  Lets not use numeric keys to toggle specific displays anymore
             * stepVisibilityToggle(new Integer("" + c).intValue());
             * runVisibilityAnimation = false;
             * if (animationCB != null) {
             *   animationCB.setSelected(false);
             * }
             */
        }
    }


    /**
     * Turn on/off the display control visibility toggling animation
     *
     * @param state Turn on or off.
     */
    public void setAnimatedVisibility(boolean state) {
        if (state == runVisibilityAnimation) {
            return;
        }
        runVisibilityAnimation = state;
        if (runVisibilityAnimation) {
            Thread t = new Thread() {
                public void run() {
                    if (getIsDestroyed()) {
                        return;
                    }
                    while (runVisibilityAnimation && !getIsDestroyed()
                            && !getDisplayInfos().isEmpty()) {
                        int sleepTime = stepVisibilityToggle();
                        try {
                            Thread.sleep((sleepTime < 0)
                                         ? animationSpeed
                                         : sleepTime * 1000);
                        } catch (InterruptedException ie) {
                            return;
                        }
                    }
                    if (getIsDestroyed()) {
                        return;
                    }
                    if (getDisplayInfos().isEmpty()) {
                        runVisibilityAnimation = false;
                    }
                }
            };
            t.start();

        }
    }

    /**
     * This turns off the visiblity toggle animation and sets the
     * visibility of all unlocked display controls to the given value.
     *
     * @param on Visiblity on or off
     */
    public void turnOnOffAllDisplays(boolean on) {
        runVisibilityAnimation = false;
        List controls = getControls();
        for (int i = controls.size() - 1; i >= 0; i--) {
            DisplayControl control = (DisplayControl) controls.get(i);
            if ( !control.getLockVisibilityToggle()) {
                control.setDisplayVisibility(on);
            }
        }

    }


    /**
     * Tell the display controls that the projection has changed.
     *
     * @param property Identifes the change type.
     */
    protected void notifyDisplayControls(String property) {
        List controls = getControls();
        for (int i = 0; i < controls.size(); i++) {
            DisplayControl control = (DisplayControl) controls.get(i);
            control.viewManagerChanged(property);
        }
    }


    /**
     * This turns on the visbility of the next  display control
     * (that is &quot;unlocked&quot;) and turns off the others
     *
     * @return index
     */
    private int stepVisibilityToggle() {
        if (currentVisibilityIdx == -1) {
            currentVisibilityIdx = 0;
        } else {
            currentVisibilityIdx++;
        }
        List controls                = getControls();

        int  visbilityAnimationPause = -1;

        int  cnt                     = 0;
        //Find the next one in the list that is not locked
        while (cnt < controls.size()) {
            if (currentVisibilityIdx >= controls.size()) {
                currentVisibilityIdx = 0;
            }
            DisplayControl control =
                (DisplayControl) controls.get(currentVisibilityIdx);
            if ( !control.getLockVisibilityToggle()) {
                visbilityAnimationPause =
                    control.getVisbilityAnimationPause();
                break;

            }
            currentVisibilityIdx++;
            cnt++;
        }

        List unlockedControls = new ArrayList();
        for (int i = 0; i < controls.size(); i++) {
            DisplayControl control = (DisplayControl) controls.get(i);
            if ( !control.getLockVisibilityToggle()) {
                unlockedControls.add(control);
            }
        }


        for (int i = 0; i < controls.size(); i++) {
            DisplayControl control = (DisplayControl) controls.get(i);
            if (control.getLockVisibilityToggle()) {
                continue;
            }
            //Special case for when we only have we. We just flip-flop its visibility
            if (unlockedControls.size() == 1) {
                control.setDisplayVisibility(
                     !control.getDisplayVisibility());
            } else {
                control.setDisplayVisibility(i == currentVisibilityIdx);
            }

        }

        return visbilityAnimationPause;
    }


    /**
     *  Show the idx'th DisplayControl. Turn off the visiblity of the others (except
     *  for the ones that are "locked").
     *
     * @param idx The index into the list of display controls to turn on
     */
    private void stepVisibilityToggle(int idx) {
        idx--;
        List controls = getControls();
        if (idx >= controls.size()) {
            return;
        }
        int cnt = 0;
        for (int i = controls.size() - 1; i >= 0; i--) {
            DisplayControl control = (DisplayControl) controls.get(i);
            if (cnt == idx) {
                control.setDisplayVisibility(true);
            } else if ( !control.getLockVisibilityToggle()) {
                control.setDisplayVisibility(false);
            }
            cnt++;
        }
    }



    /**
     * Add the given component into the list of toolbar
     * components. The id and name are used for the user preferences
     * for shoing/not showing toolbar components.
     *
     * @param component The GUI component
     * @param id Its id
     * @param name Its name
     */
    protected void addToolBar(Component component, String id, String name) {
        toolbars.add(component);
        toolbarIds.add(id);
        toolbarNames.add(name);
    }

    /**
     * A hook so derived  classes can add in their toolbar components
     */
    protected void initToolBars() {}

    /**
     * Should we show the bottom legend that holds the list of
     * display controls.
     *
     * @return Should show bottom legend
     */
    public boolean getShowBottomLegend() {
        return getStore().get(PREF_SHOWBOTTOMLEGEND, false);
    }


    /**
     * Should we show the side legend that holds the list of
     * display controls.
     *
     * @return Should show side legend
     */
    public boolean getShowSideLegend() {
        return getStore().get(PREF_SHOWSIDELEGEND, true);
    }



    /**
     * Should we show the animation widget boxes
     *
     * @return Should show anime boxes
     */
    public boolean getShowAnimationBoxes() {
        return getStore().get(PREF_SHOWANIMATIONBOXES, true);
    }



    /**
     * Set whether we should show any of the display control legends
     *
     * @param b Should show legend
     */
    public void setShowControlLegend(boolean b) {
        showControlLegend = b;
        if (showControlLegend) {
            legendState = IdvLegend.STATE_DOCKED;
        } else {
            legendState = IdvLegend.STATE_HIDDEN;
        }
    }

    /**
     * Get whether we should show any of the display control legends.
     *
     * @return b Should show legend
     */
    public boolean getShowControlLegend() {
        return showControlLegend;
    }

    /**
     * Set the LegendState property.
     *
     * @param value The new value for LegendState
     */
    public void setLegendState(String value) {
        legendState = value;
    }

    /**
     * Get the LegendState property.
     *
     * @return The LegendState
     */
    public String getLegendState() {
        return legendState;
    }



    /**
     * Create and return the menu bar.
     *
     * @return The menu bar
     */
    protected JMenuBar doMakeMenuBar() {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        List     items   = doMakeMenuList();
        JMenuBar menuBar = GuiUtils.makeMenuBar(items);
        return menuBar;
    }

    /**
     * Return a list of components that are used to create the menubar.
     * The default here is to just return an empty list.
     *
     * @return List of menu bar components
     */
    public ArrayList doMakeMenuList() {
        return new ArrayList();
    }

    /**
     * Return the  list of {@link DisplayControl}s displayed in this ViewManager.
     *
     * @return List of display controls
     */
    public List getControls() {
        ArrayList controls          = new ArrayList();
        List      localDisplayInfos = getDisplayInfos();
        Hashtable seen              = new Hashtable();
        for (int i = 0; i < localDisplayInfos.size(); i++) {
            DisplayControl control =
                ((DisplayInfo) localDisplayInfos.get(i)).getDisplayControl();
            if (seen.get(control) == null) {
                seen.put(control, control);
                controls.add(control);
            }
        }
        return controls;
    }


    /**
     * Handle the display data changing
     *
     * @param displayControl  the control
     */
    public void displayDataChanged(DisplayControl displayControl) {
        if (idv.getUseTimeDriver() && displayControl.getIsTimeDriver()) {
            for (DisplayControl control :
                    (List<DisplayControl>) getControls()) {
                if ( !control.equals(displayControl)
                        && ((DisplayControlImpl) control)
                            .getUsesTimeDriver()) {
                    try {
                        ((DisplayControlImpl) control)
                            .reloadDataSourceInThread();
                    } catch (Exception e) {
                        logException("Error reloading data source for "
                                     + displayControl.getLabel(), e);
                    }
                }
            }
        }
    }

    /**
     * Return the  list of {@link DisplayControl}s displayed in this ViewManager
     * that are meant to be shown in a legend
     *
     * @return List of display controls for the legend
     */
    public List getControlsForLegend() {
        List legendControls = new ArrayList();
        List controls       = getControls();
        for (int i = 0; i < controls.size(); i++) {
            DisplayControl control = (DisplayControl) controls.get(i);
            if (control.getShowInLegend()) {
                legendControls.add(control);
            }
        }
        return legendControls;
    }



    /**
     * Set the foreground and background colors in the display.
     * This method just sets the color members and passes them through
     * to the legends. Derived classes do the work of setting the colors
     * in their displays.
     *
     * @param foreground The  foreground color
     * @param background The background color
     */
    public void setColors(Color foreground, Color background) {
        if (foreground == null) {
            foreground = this.foreground;
        }
        this.foreground = foreground;
        if (background == null) {
            background = this.background;
        }
        this.background = background;

        if ( !hasDisplayMaster()) {
            return;
        }

        getMaster().setBackground(background);
        getMaster().setForeground(foreground);
        if (legends == null) {
            return;
        }
        for (IdvLegend legend : getLegends()) {
            legend.setColors(foreground, background);
        }
    }



    /**
     * This goes through the list of display controls and, if
     * their &quot; display category&quot; is equals to the given
     * category, then their visiblity is set to the given on value.
     * This is called from the {@link ucar.unidata.idv.ui.SideLegend}
     * class to turn on/off  groups of display controls.
     *
     * @param category Display category to match
     * @param on The visibility value
     */
    private void setDisplayCategoryVisiblity(String category, boolean on) {
        List controls = getControls();
        for (int i = controls.size() - 1; i >= 0; i--) {
            DisplayControl control = (DisplayControl) controls.get(i);
            if (Misc.equals(control.getDisplayCategory(), category)) {
                control.setDisplayVisibility(on);
            }
        }
    }


    /**
     * Called when the dispay category or other state of the display control has changed.
     * Triggers a redisplay of the legends.
     *
     * @param displayControl The display control that changed
     */
    public void displayControlChanged(DisplayControl displayControl) {
        fillLegends();
        if (timelineDialog != null) {
            timelineDialog.repaint();
        }
    }

    /**
     * handle when the display control has changed
     *
     * @param displayControl display control
     */
    public void displayControlVisibilityChanged(
            DisplayControl displayControl) {
        if (timelineDialog != null) {
            timelineDialog.repaint();
        }
        updateDisplayList();
    }


    /**
     * This is called when the list of display controls has changed.
     * (e.g., a new one has been added, etc.). It tells the list of
     * legends to fill themselves  and updates the display menu.
     */
    protected void fillLegends() {
        if (shouldDoThingsRightAway()) {
            reallyFillLegends();
        } else {
            dirty = true;
        }

    }


    /**
     * tell the legends to fill themselves
     */
    protected void reallyFillLegends() {
        //If we are not loading a bundle then fill the legends right now
        if ( !getStateManager().isLoadingXml()
                || !shouldDoThingsRightAway()) {
            fillLegendsInner();
        } else {
            //else fill them in about 1 second
            fillLegendsTime = System.currentTimeMillis() + 1000;
            if ( !fillLegendsPending) {
                //If none pending then start the thread
                fillLegendsPending = true;
                Misc.run(this, "fillLegendsLater");
            }
        }
    }

    /**
     * Wait until there have been no calls to fillLegends for a little bit
     * and then fill the legends
     */
    public void fillLegendsLater() {
        //We keep looping until there hasn't been a recent addition to the legends
        //or until something else has filled the legends
        while (fillLegendsPending) {
            long diff = fillLegendsTime - System.currentTimeMillis();
            if (diff > 0) {
                Misc.sleep(diff);
            } else {
                if (fillLegendsPending) {
                    fillLegendsInner();
                }
                break;
            }
        }
    }



    /**
     * actually fill the legends and update the display list
     */
    protected void fillLegendsInner() {
        fillLegendsPending = false;
        updateDisplayList();
        synchronized (LEGENDMUTEX) {
            for (IdvLegend legend : getLegends()) {
                legend.fillLegend();
            }
        }
    }



    /**
     * This is called when the list of display controls has changed
     * to update the display menu.
     *
     * @param displayMenu The menu to fill
     */
    public void initDisplayMenu(JMenu displayMenu) {
        List      controls = getControls();
        JMenuItem item     = null;
        boolean   didone   = false;
        int       cnt      = 0;
        if (getShowSideLegend()) {
            if ( !legendState.equals(IdvLegend.STATE_HIDDEN)) {
                displayMenu.add(GuiUtils.makeMenuItem("Hide Legend", this,
                        "setSideLegendPosition", IdvLegend.STATE_HIDDEN));
            }
            if ( !legendState.equals(IdvLegend.STATE_DOCKED)) {
                displayMenu.add(GuiUtils.makeMenuItem("Embed Legend", this,
                        "setSideLegendPosition", IdvLegend.STATE_DOCKED));
            }
            if ( !legendState.equals(IdvLegend.STATE_FLOAT)) {
                displayMenu.add(GuiUtils.makeMenuItem("Float Legend", this,
                        "setSideLegendPosition", IdvLegend.STATE_FLOAT));
            }
            displayMenu.addSeparator();
        }

        for (int i = controls.size() - 1; i >= 0; i--) {
            DisplayControl control = (DisplayControl) controls.get(i);
            if ( !didone) {
                didone = true;
                displayMenu.add(GuiUtils.makeMenuItem("Remove All Displays",
                        this, "clearDisplays"));
                if (animationMenu == null) {
                    animationMenu = new JMenu("Visibility Animation");
                    animationCB   = new JCheckBoxMenuItem("On");
                    animationCB.addActionListener(new ObjectListener(null) {
                        public void actionPerformed(ActionEvent event) {
                            setAnimatedVisibility(((JCheckBoxMenuItem) event
                                .getSource()).isSelected());
                        }
                    });
                    animationMenu.add(animationCB);
                    item = new JMenuItem("Faster");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            if (animationSpeed > 300) {
                                animationSpeed -= 200;
                            }
                        }
                    });
                    animationMenu.add(item);
                    item = new JMenuItem("Slower");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent event) {
                            animationSpeed += 200;
                        }
                    });
                    animationMenu.add(item);
                }
                displayMenu.add(animationMenu);
                displayMenu.addSeparator();
            }
            String legendOrder = " #" + (++cnt) + "  ";
            String label       = legendOrder + " " + control.getMenuLabel();
            displayMenu.add(GuiUtils.makeMenu(label,
                    control.getControlMenus(displayMenu)));
        }

        if ( !didone) {
            displayMenu.add(new JLabel("No displays"));
        }
    }


    /**
     * Adds the given display listener to the
     * {@link ucar.visad.display.DisplayMaster}
     *
     * @param listener The listener to add
     */
    public void addDisplayListener(DisplayListener listener) {
        getMaster().addDisplayListener(listener);
    }





    /**
     * A wrapper  around {@link IdvBase#getDisplayConventions()}
     *
     * @return The {@link DisplayConventions} to use.
     */
    public DisplayConventions getDisplayConventions() {
        return idv.getDisplayConventions();
    }



    /**
     * This removes the given {@link DisplayInfo}.
     * The DisplayInfo is what really holds the
     * {@link DisplayControl}.
     *
     * @param displayInfo The display info to remove
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void removeDisplayInfo(DisplayInfo displayInfo)
            throws RemoteException, VisADException {
        if (getIsDestroyed()) {
            return;
        }
        synchronized (displayInfos) {
            displayInfos.remove(displayInfo);
        }
        if (master != null) {
            Displayable displayable = displayInfo.getDisplayable();
            if (displayable != null) {
                master.removeDisplayable(displayable);
            }
        }
        fillLegends();
        updateTimelines(true);
    }


    /**
     * This is called by display controls and allows us to force fast rendering
     *
     * @param b The displays fast rendering flag
     *
     * @return true
     */
    public boolean getUseFastRendering(boolean b) {
        return b;
    }



    /**
     * Add the display infos
     *
     * @param displayInfos the display infos
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   VisAD Exception
     */
    public void addDisplayInfos(List<DisplayInfo> displayInfos)
            throws RemoteException, VisADException {
        if (getIsDestroyed()) {
            return;
        }


        setMasterInactive();
        try {
            if (master != null) {
                for (DisplayInfo info : displayInfos) {
                    info.setDisplayableAdded(addDisplayInfo(info));
                }
            }


            if (shouldDoThingsRightAway()) {
                fillLegends();
                updateTimelines(true);
                if ( !getStateManager().isLoadingXml()) {
                    toFront();
                }
            } else {
                dirty = true;
            }

        } finally {
            setMasterActive();
        }
    }


    /**
     *  Add the DisplayInfo to the list of DisplayInfo-s
     *  If I have a {@link ucar.visad.display.DisplayMaster}
     *  then set the master to be inactive
     *  and add the displayable held  by the displayInfo into the displayMaster.
     *  This returns  true if the add was successful, false otherwise.
     *
     * @param displayInfo The display info to add.
     * @return Was the addition successful
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public boolean addDisplayInfo(DisplayInfo displayInfo)
            throws RemoteException, VisADException {
        if (getIsDestroyed()) {
            return false;
        }

        synchronized (displayInfos) {
            if (displayInfos.contains(displayInfo)) {
                return true;
            }
            displayInfos.add(displayInfo);
        }
        if (master != null) {
            if (shouldDoThingsRightAway()) {
                master.addDisplayable(displayInfo.getDisplayable());
            } else {
                dirty = true;
            }
        }

        return true;
    }




    /**
     * Update display if needed
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void updateDisplayIfNeeded()
            throws VisADException, RemoteException {
        if ( !dirty || (master == null)) {
            return;
        }
        dirty = false;
        List tmpList = getDisplayInfos();
        master.setDisplayInactive();
        for (int i = 0; i < tmpList.size(); i++) {
            DisplayInfo displayInfo = (DisplayInfo) tmpList.get(i);
            master.addDisplayable(displayInfo.getDisplayable());
        }
        reallyFillLegends();
        //This sets the master active
        master.draw();
    }


    /**
     * Bring the window that contains this ViewManager to the front
     */
    public void toFront() {
        Window window = GuiUtils.getWindow(getComponent());
        //        IdvWindow idvWindow = getDisplayWindow();
        if (window != null) {
            GuiUtils.toFront(window);
            //            indow.show();
            //            idvWindow.toFront();
        }
    }

    /**
     * This removes all of the display controls
     */
    public void clearDisplays() {
        if (getIsDestroyed()) {
            return;
        }
        try {
            //            setMasterInactive();
            List tmpList = getDisplayInfos();
            for (int i = 0; i < tmpList.size(); i++) {
                DisplayControl dc =
                    ((DisplayInfo) tmpList.get(i)).getDisplayControl();
                if (dc.getCanDoRemoveAll()) {
                    dc.doRemove();
                }
            }
            //            setMasterActive();
            fillLegends();
            updateTimelines(true);
        } catch (Exception exp) {
            logException("clearDisplays", exp);
        }
    }


    /**
     * This gets called when a display control has been moved to this viewmanager
     *
     * @param control The display control that has been moved
     */
    public void controlMoved(DisplayControl control) {
        fillLegends();
        getIdv().getIdvUIManager().getViewPanel().controlMoved(control);
    }

    /**
     * Create and return the show menu.
     *
     * @return The Show menu
     */
    protected JMenu makeShowMenu() {
        JMenu showMenu = new JMenu("Show");
        createCBMI(showMenu, PREF_WIREFRAME);
        createCBMI(showMenu, PREF_WAITMSG);
        createCBMI(showMenu, PREF_SHOWDISPLAYLIST);
        createCBMI(showMenu, PREF_TOPBAR_VISIBLE);
        return showMenu;
    }



    /**
     * Create and return the view menu.
     *
     * @return The View menu
     */
    public JMenu makeViewMenu() {
        JMenu viewMenu = GuiUtils.makeDynamicMenu("View", this,
                             "firstInitializeViewMenu");
        viewMenu.setMnemonic(GuiUtils.charToKeyCode("V"));
        if (this.viewMenu == null) {
            this.viewMenu = viewMenu;
        }
        return viewMenu;
    }

    /**
     * Add items to the context menu. This is the  one we popup on a right-click in the controls tab.
     *
     * @param menuItems List to add to
     */
    public void addContextMenuItems(List menuItems) {
        menuItems.add(GuiUtils.makeMenuItem("Show Window", this, "toFront"));
        menuItems.add(GuiUtils.MENU_SEPARATOR);
        menuItems.addAll(doMakeMenuList());
    }


    /**
     * Initialize the view menu.
     * This gets called first and allows us to translate the component tree
     *
     * @param viewMenu the view menu
     */
    public void firstInitializeViewMenu(JMenu viewMenu) {
        initializeViewMenu(viewMenu);
        Msg.translateTree(viewMenu);
    }



    /**
     * Dynamically initialize the view menu
     *
     * @param viewMenu The menu
     */
    public void initializeViewMenu(JMenu viewMenu) {
        if (showControlMenu) {
            JMenu displayMenu = GuiUtils.makeDynamicMenu("Displays", this,
                                    "initDisplayMenu");
            viewMenu.add(displayMenu);
            viewMenu.addSeparator();
        }



        List renderItems = new ArrayList();
        renderItems.add(GuiUtils.makeMenuItem("Make Frames", this,
                "makeFrames"));
        if (usingImagePanel) {
            renderItems.add(GuiUtils.makeMenuItem("Reset to display", this,
                    "useDisplay"));
        } else if (imagePanel != null) {
            renderItems.add(GuiUtils.makeMenuItem("Reset to images", this,
                    "useImages"));
        }

        //        viewMenu.add(GuiUtils.makeMenu("Rendering", renderItems));

        JMenu captureMenu = new JMenu("Capture");
        viewMenu.add(captureMenu);
        JMenuItem mi;
        captureMenu.add(
            mi = (JMenuItem) GuiUtils.setIcon(
                GuiUtils.makeMenuItem(
                    "Image...  Ctrl+I", this,
                    "doSaveImageInThread"), "/auxdata/ui/icons/camera.png"));
        //        mi.setMnemonic(GuiUtils.charToKeyCode("C"));
        captureMenu.add(
            mi = (JMenuItem) GuiUtils.setIcon(
                GuiUtils.makeMenuItem(
                    "Movie...  Ctrl+M", this,
                    "startImageCapture"), "/auxdata/ui/icons/film.png"));
        //        mi.setMnemonic(GuiUtils.charToKeyCode("M"));


        captureMenu.add(GuiUtils.setIcon(GuiUtils.makeMenuItem("Print...",
                this, "doPrintImage", null,
                true), "/auxdata/ui/icons/printer.png"));
        viewMenu.add(makeShowMenu());

        if (this.viewMenu == null) {
            this.viewMenu = viewMenu;
        }
    }



    /**
     * Make the saved views menu
     *
     * @return saved views menu
     */
    protected JMenu makeSavedViewsMenu() {
        return GuiUtils.makeDynamicMenu("Viewpoints", this,
                                        "initViewStateMenu");
    }


    /**
     * Initialize the View State menu
     *
     * @param viewStateMenu the menu to init
     */
    public void initViewStateMenu(JMenu viewStateMenu) {
        viewStateMenu.add(GuiUtils.makeMenuItem("Save Current Viewpoint",
                getViewManager(), "doSaveState"));
        viewStateMenu.addSeparator();
        makeViewStateMenu(viewStateMenu);
    }

    /**
     * Is this compatible with the other ViewManager
     *
     * @param vm  the other VM
     *
     * @return  true if compatible
     */
    public boolean isCompatibleWith(ViewManager vm) {
        IdvWindow thisWindow = getDisplayWindow();
        if (thisWindow != null) {
            //If this is being called it means the given vm is from an old bundle
            //And we only want to be compatible with it if we are in the default window
            String skin = thisWindow.getSkinPath();
            if ((skin != null)
                    && !Misc.equals(
                        "/ucar/unidata/idv/resources/skins/skin.xml", skin)) {
                return false;
            }
        }
        return getClass().equals(vm.getClass());
    }


    /**
     * Is this ViewState appropriate for me?
     *
     * @param viewState the ViewState
     *
     * @return  true if compatible
     */
    public boolean isCompatibleWith(ViewState viewState) {
        return getClass().getName().toString().equals(
            viewState.getViewClassName().trim());
    }


    /**
     * Make the view state menu
     *
     * @param viewStateMenu The menu to fill in
     */
    protected void makeViewStateMenu(JMenu viewStateMenu) {
        getIdvUIManager().makeViewStateMenu(viewStateMenu, this);
    }

    /**
     * Return the {@link ucar.visad.display.AnimationWidget}
     * that is used. May be null.
     *
     * @return The animation widget.
     */
    public AnimationWidget getAnimationWidget() {
        return animationWidget;
    }

    /**
     * Set the ExternalAnimation property.
     *
     * @param value The new value for ExternalAnimation
     * @param widget the animation widget to update
     */
    public void setExternalAnimation(Animation value,
                                     AnimationWidget widget) {
        externalAnimation       = value;
        externalAnimationWidget = widget;
    }

    /**
     * Get the ExternalAnimation property.
     *
     * @return The ExternalAnimation
     */
    public Animation getExternalAnimation() {
        return externalAnimation;
    }






    /**
     * Get the ExternalAnimationWidget property.
     *
     * @return The ExternalAnimationWidget
     */
    public AnimationWidget getExternalAnimationWidget() {
        return externalAnimationWidget;
    }



    /**
     * Return the {@link ucar.visad.display.Animation}
     * that is used. May be null. This is the
     * {@link ucar.visad.display.Displayable} that is
     * added into the  {@link ucar.visad.display.DisplayMaster}
     * to control time animation.
     *
     * @return The animation.
     *
     */
    public Animation getAnimation() {
        if ((animation == null) && animationOk()) {
            try {
                if (animationInfo == null) {
                    animationInfo =
                        (AnimationInfo) getIdv().getPersistenceManager()
                            .getPrototype(AnimationInfo.class);
                    if (animationInfo == null) {
                        animationInfo = new AnimationInfo();
                        animationInfo.setBoxesVisible(
                            getShowAnimationBoxes());
                    }

                }
                animationWidget = new AnimationWidget(animationInfo);
                animationWidget.setUniqueId(getUniqueId() + "_anim");
                animation = new Animation();
                animationWidget.setAnimation(animation);
                animation.addPropertyChangeListener(
                    new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        try {
                            if (evt.getPropertyName().equals(
                                    Animation.ANI_VALUE)) {
                                animationTimeChanged();
                                updateTimelines(false);
                                if (imagePanel != null) {
                                    imagePanel.setSelectedFile(
                                        animation.getCurrent());
                                }
                            } else if (evt.getPropertyName().equals(
                                    Animation.ANI_SET) && (animationTimeline
                                        != null)) {
                                if (animationTimeline != null) {
                                    animationTimeline.setDatedThings(
                                        DatedObject.wrap(
                                            Util.makeDates(
                                                animationWidget.getTimes())));
                                }
                            }
                        } catch (Exception exp) {
                            logException("updating timeline", exp);
                        }
                    }
                });

            } catch (Exception exp) {
                logException("getAnimation", exp);
            }
        }

        return animation;
    }

    /**
     * Should this ViewManager create an animation object/widget.
     * The default is true.
     *
     * @return Is animation ok
     */
    public boolean animationOk() {
        return true;
    }

    /**
     *  Set the {@link ucar.visad.display.AnimationInfo} property.
     * This holds the state of the animation (e.g, rocking, rate, etc.)
     *
     *  @param value The new value for AnimationInfo
     */
    public void setAnimationInfo(AnimationInfo value) {
        animationInfo = value;
    }

    /**
     *  Get the AnimationInfo property.
     *
     *  @return The AnimationInfo
     */
    public AnimationInfo getAnimationInfo() {
        if (animationWidget != null) {
            return animationWidget.getAnimationInfo();
        }
        return null;
    }



    /**
     * Has this ViewManager been destroyed. A destroyed  ViewManager
     * is one whose window has been closed. We have this around so the
     * ViewManager does no more operations.
     *
     * @return Is destroyed
     */
    public boolean getIsDestroyed() {
        return isDestroyed;
    }


    /**
     * Add a removeable object
     *
     * @param removable  the removeable
     */
    public void addRemovable(Removable removable) {
        removables.add(removable);
    }


    /**
     * Destroy this view manager. Turn off all pending wait cursor
     * calls,  tell the display master and the animation widget
     * to destory themselves, null out references to other objects
     * (so we don't get dangling reference/memory leaks).
     */
    public void destroy() {

        if (isDestroyed) {
            return;
        }
        isDestroyed = true;
        getIdvUIManager().viewManagerDestroyed(this);

        for (Removable removable : removables) {
            removable.doRemove();
        }
        removables = null;


        //First get rid of the  displays
        //Do this here so we don't get into any infinite loops with
        //the transectdrawingcontrol
        setMasterInactive();
        if (displayInfos != null) {
            List tmpList = getDisplayInfos();
            for (int i = 0; i < tmpList.size(); i++) {
                try {
                    ((DisplayInfo) tmpList.get(
                        i)).getDisplayControl().viewManagerDestroyed(this);
                } catch (Exception exc) {}
            }
        }


        if (getVMManager() != null) {
            getVMManager().removeViewManager(this);
        }

        //Make sure we clear out any outstanding wait cursor calls
        while (outstandingWaits-- >= 0) {
            //            getIdvUIManager().showNormalCursor();
        }





        removeSharable();

        if (timelineDialog != null) {
            timelineDialog.dispose();

        }

        if (propertiesDialog != null) {
            propertiesDialog.dispose();

        }

        if (animationWidget != null) {
            animationWidget.destroy();
        }

        if (master != null) {
            try {
                if (displayListDisplayables != null) {
                    master.removeDisplayable(displayListDisplayables);
                }
                master.destroy();
            } catch (Throwable exp) {
                //Ignore  any errors when we destroy the DisplayMaster
                Throwable wrappedExc = LogUtil.getInnerException(exp);
                LogUtil.consoleMessage(
                    "Had an error destroying the DisplayMaster:"
                    + wrappedExc);
                //logException("Destroying the View Manager", exp);
            } finally {}
            master = null;
        }


        if (animationMenu != null) {
            GuiUtils.empty(animationMenu, true);
        }

        if (viewMenu != null) {
            GuiUtils.empty(viewMenu, true);
        }

        if (menuBar != null) {
            GuiUtils.empty(menuBar, true);
        }

        if (fullContents != null) {
            GuiUtils.empty(fullContents, true);
        }

        //Be somewhat overly agressive about nulling out references, etc.
        timelineDialog          = null;
        propertiesDialog        = null;
        animationWidget         = null;
        keyboardBehavior        = null;
        displayListDisplayables = null;
        animation               = null;
        legends                 = null;
        sideLegend              = null;
        sideLegendComponent     = null;
        mainSplitPane           = null;
        animationMenu           = null;
        viewMenu                = null;
        menuBar                 = null;
        fullContents            = null;
        idv                     = null;

        displayInfos.clear();

    }

    /**
     * Get a copy of the list of {@link DisplayInfo}-s
     *
     * @return A copy of the the display infos
     */
    protected List<DisplayInfo> getDisplayInfos() {
        synchronized (displayInfos) {
            List<DisplayInfo> tmp = new ArrayList<DisplayInfo>(displayInfos);
            return tmp;
        }
    }


    /**
     * Return the string representation of this object
     *
     * @return The  String representation
     */
    public String toString() {
        return "View:" + getViewDescriptor();
        //        return "View:" + cnt;
    }

    /**
     * What type of view is this
     *
     * @return The type of view
     */
    public String getTypeName() {
        return "View";
    }


    /**
     * Get the {@link ViewDescriptor}. The ViewDescriptor
     * is what describes this ViewManager. For now it just has
     * a name. We have this here for xml persistence.
     *
     * @return The ViewDescriptor
     */
    public ViewDescriptor getViewDescriptor() {
        if (aliases.size() > 0) {
            return (ViewDescriptor) aliases.get(0);
        }
        return null;
    }

    /**
     * Set the {@link ViewDescriptor}. We have this here for xml
     * persistence.
     *
     * @param vd The new ViewDescriptor.
     * @deprecated Use addViewDescriptor
     */
    public void setViewDescriptor(ViewDescriptor vd) {
        addViewDescriptor(vd);
    }

    /**
     * Add the view descriptor
     *
     * @param vd the view descriptor
     */
    public void addViewDescriptor(ViewDescriptor vd) {
        if (vd != null) {
            if ( !vd.nameEquals(ViewDescriptor.LASTACTIVE)) {
                if ( !aliases.contains(vd)) {
                    aliases.add(vd);
                }
            }
        }
    }

    /**
     * Does this view manager have the given  view descriptor
     *
     * @param vd the view descriptor
     *
     * @return defined by the view descriptor
     */
    public boolean isDefinedBy(ViewDescriptor vd) {
        if ( !isClassOk(vd)) {
            return false;
        }
        for (int i = 0; i < aliases.size(); i++) {
            ViewDescriptor viewDescriptor = (ViewDescriptor) aliases.get(i);
            if (viewDescriptor.nameEquals(vd)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Can this view manager be used in exchange for the given view manager
     *
     * @param that The other view manager to check
     * @return Can this be used in place of that
     */
    public boolean canBe(ViewManager that) {
        return that.getClass().equals(this.getClass());
    }


    /**
     * If the given view descriptor has one or more class names is the class name
     * of this ViewManager in the list
     *
     * @param vd view descriptor
     *
     * @return class ok
     */
    public boolean isClassOk(ViewDescriptor vd) {
        if (vd == null) {
            return false;
        }
        List classNames = vd.getClassNames();
        if ((classNames == null) || (classNames.size() == 0)) {
            return true;
        }
        String className = getClass().getName();
        return classNames.contains(className);
    }

    /**
     * Get the view manager
     *
     * @return this
     */
    public ViewManager getViewManager() {
        return this;
    }


    /**
     * Override the base class method to return a (hopefully)
     * unique id.
     *
     * @return The String that uniquely identified this object.
     */
    public String getUniqueId() {
        ViewDescriptor viewDescriptor = getViewDescriptor();
        if (viewDescriptor != null) {
            return viewDescriptor.getName();
        }
        return Misc.getUniqueId();
    }


    /**
     * Return the GUI component from the
     * {@link ucar.visad.display.DisplayMaster}
     *
     * @return The component from  the display master.
     */
    public JComponent getComponent() {
        return (JComponent) getMaster().getComponent();
    }

    /**
     * Return the  full GUI contents.
     *
     * @return The GUI contents.
     */
    public Container getContents() {
        if (fullContents == null) {
            initUI();
        }
        return fullContents;
    }

    /**
     * This is meant to be overrode by a derived class
     * to return the contents (typically the display master's
     * component) that is the &quot;abbreviated&quot; gui.
     * It is used by the MultiPaneIdv.
     *
     * @return The inner contents. This method just returns null.
     */
    public JComponent getInnerContents() {
        return null;
    }

    /**
     * Create the main GUI contents.
     *
     * @return The GUI contents
     */
    protected Container doMakeContents() {
        if (master == null) {
            return new JPanel();
        }
        Component mc = master.getComponent();
        return GuiUtils.topCenter(null, mc);
    }




    /**
     * Set the cursor in the component of the main display
     *
     * @param c The cursor
     */
    public void setCursorInDisplay(Cursor c) {
        JComponent comp = getComponent();
        if (comp != null) {
            comp.setCursor(c);
        }
    }


    /**
     * See if this has a display master
     *
     * @return true if the DisplayMaster is not null
     */
    public boolean hasDisplayMaster() {
        return master != null;
    }


    /** a mutex for accessing the display master */
    private Object MASTER_MUTEX = new Object();

    /**
     * Get the {@link ucar.visad.display.DisplayMaster}.
     *
     * @return The display master
     */
    public DisplayMaster getMaster() {
        if (master == null) {
            try {
                //we want to synchronize here so we don't make 2 (or more) versions of the displaymaster
                synchronized (MASTER_MUTEX) {
                    //If we had been blocked by another thread creating the DisplayMaste
                    //check if we have a master
                    if (master != null) {
                        return master;
                    }
                    // might need these for the display initialization
                    if (initProperties != null) {
                        String tmp = initProperties;
                        initProperties = null;
                        parseProperties(tmp);
                    }
                    //Use a local tmpMaster variable so we don't set the master member before we're ready to
                    DisplayMaster tmpMaster = doMakeDisplayMaster();
                    if (tmpMaster == null) {
                        return null;
                    }
                    DisplayRenderer renderer =
                        tmpMaster.getDisplay().getDisplayRenderer();
                    if (renderer instanceof DisplayRendererJ3D) {
                        initLights((DisplayRendererJ3D) renderer);
                    }

                    if ( !shouldDoThingsRightAway()) {
                        tmpMaster.setDisplayInactive();
                    } else {
                        tmpMaster.setMouseFunctions(
                            getIdv().getPreferenceManager().getMouseMap());
                        tmpMaster.setKeyboardEventMap(
                            getIdv().getPreferenceManager().getKeyboardMap());
                        tmpMaster.setWheelEventMap(
                            getIdv().getPreferenceManager().getWheelMap());
                    }
                    GraphicsModeControl gmc =
                        tmpMaster.getDisplay().getGraphicsModeControl();
                    gmc.setCacheAppearances(true);
                    gmc.setMergeGeometries(true);
                    setDisplayMaster(tmpMaster);

                    if (shouldDoThingsRightAway()) {
                        Trace.call1("ViewManager.getMaster master.draw");
                        tmpMaster.draw();
                        Trace.call2("ViewManager.getMaster master.draw");
                        Trace.call1(
                            "ViewManager.getMaster updateDisplayList");
                        updateDisplayList();
                        Trace.call2(
                            "ViewManager.getMaster updateDisplayList");
                    }
                    master = tmpMaster;
                }
            } catch (Exception exc) {
                logException("Creating display master", exc);
            }
        }
        return master;
    }



    /**
     * A hack for when we are running isl and don't want to update the display incrementally
     * This just returns true for now but is a hook for later
     *
     * @return true
     */
    public boolean shouldDoThingsRightAway() {
        return true;
    }



    /**
     * Is this an interactive session
     *
     * @return true if interactive
     */
    public boolean isInteractive() {
        return getIdv().getInteractiveMode();
    }

    /**
     * Make the DisplayMaster for this ViewManger. Subclasses should
     * override.
     *
     * @return  the appropriate display
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   VisAD problem
     */
    protected DisplayMaster doMakeDisplayMaster()
            throws VisADException, RemoteException {
        return null;
    }



    /**
     * get the display renderer
     *
     * @return display renderer
     */
    public DisplayRenderer getDisplayRenderer() {
        if (master != null) {
            DisplayImpl vdisplay = (DisplayImpl) master.getDisplay();
            return vdisplay.getDisplayRenderer();
        }
        return null;
    }


    /**
     * Set the {@link ucar.visad.display.DisplayMaster}
     *
     * @param master The display master
     */
    protected void setDisplayMaster(DisplayMaster master) {
        if (this.master != null) {
            //TODO: How should we handle this?
            //throw new IllegalArgumentException ("Already have one display master set");
        }
        this.master = master;
        DisplayImpl display = (DisplayImpl) master.getDisplay();
        display.addDisplayListener(this);
        display.enableEvent(DisplayEvent.MOUSE_PRESSED);
        display.enableEvent(DisplayEvent.COMPONENT_RESIZED);

        resetProjectionControl();
    }


    /**
     * Reset the projection control
     */
    protected void resetProjectionControl() {
        DisplayImpl display = (DisplayImpl) master.getDisplay();
        projectionControl = display.getProjectionControl();
        projectionControl.addControlListener(this);
    }

    /**
     * Get the projection control
     *
     * @return The projection control
     */
    public ProjectionControl getProjectionControl() {
        return projectionControl;
    }


    /**
     * Create, if needed, and return the command manager
     *
     * @return The command manager
     */
    public CommandManager getCommandManager() {
        if (commandManager == null) {
            commandManager = new CommandManager(100);
        }
        return commandManager;
    }



    /**
     * This is the default method (no-op) for
     * the ControlListener interface
     *
     * @param e The <code>ControlEvent</code>
     */
    public final void controlChanged(ControlEvent e) {
        if ( !initDone || getIsDestroyed()) {
            return;
        }
        handleControlChanged(e);
    }

    /**
     * Handle a control changed.  A no-op here.
     *
     * @param e the event.
     */
    protected void handleControlChanged(ControlEvent e) {
        //Set the matrix label in the properties dialog
        setMatrixLabel(false);
    }


    /**
     *  An implementation of the the DisplayListener interface.
     * This method turns on/off the wait cursor when it gets a
     * WAIT_ON or WAIT_OFF event. It also, when it receives a
     * FRAME_DONE event for the first time,  calls <code>firstFrameDone</code>
     * on the {@link DisplayControl}s
     *
     * @param de The <code>DisplayEvent</code>
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public void displayChanged(DisplayEvent de)
            throws VisADException, RemoteException {
        int        eventId    = de.getId();
        InputEvent inputEvent = de.getInputEvent();
        if (getIsDestroyed()) {
            return;
        }

        if (eventId == DisplayEvent.MOUSE_PRESSED) {
            //            System.err.println (" mouse pressed");
        }


        if (getIsShared() && !lastActive) {
            if (eventId == DisplayEvent.MOUSE_PRESSED) {
                getVMManager().setLastActiveViewManager(this);
            } else if ( !clickToFocus
                        && (eventId == DisplayEvent.MOUSE_MOVED)) {
                getVMManager().setLastActiveViewManager(this);
            }
        }

        if (eventId == DisplayEvent.KEY_PRESSED) {
            if ((inputEvent instanceof KeyEvent)) {
                KeyEvent keyEvent = (KeyEvent) inputEvent;
                if ((keyEvent.getKeyCode() == KeyEvent.VK_ESCAPE)) {
                    Misc.runInABit(100, this, "resetFullScreen", null);
                } else if ((keyEvent.getKeyCode() == KeyEvent.VK_F5)) {
                    Misc.runInABit(100, this, "toggleFullScreen", null);
                }
            }
        } else if (eventId == DisplayEvent.WAIT_ON) {
            if (LogUtil.getTestMode()) {
                outstandingWaits++;
                if (outstandingWaits == 1) {
                    getIdvUIManager().showWaitCursor();
                    //                    System.err.println ("waiton:" + outstandingWaits + " " + getIdvUIManager().getWaitCursorCount());
                }
            } else {
                waitCursorInABit(++currentWaitKey);
            }
        } else if (eventId == DisplayEvent.WAIT_OFF) {
            //            Trace.msg("waitOff");
            currentWaitKey++;
            if (outstandingWaits == 0) {
                return;
            }
            outstandingWaits--;
            if (outstandingWaits <= 0) {
                getIdvUIManager().showNormalCursor();
                //                System.err.println ("waitoff:" + outstandingWaits + " " + getIdvUIManager().getWaitCursorCount());
                outstandingWaits = 0;
            }
        } else if (eventId == DisplayEvent.MOUSE_PRESSED) {
            mouseDown          = true;
            mousePressedMatrix = getMaster().getProjectionMatrix();
        } else if (eventId == DisplayEvent.MOUSE_RELEASED) {
            mouseDown = false;
            double[] currentMatrix = getMaster().getProjectionMatrix();
            if ((mousePressedMatrix != null)
                    && !Arrays.equals(currentMatrix, mousePressedMatrix)) {
                addCommand(new MatrixCommand(this, mousePressedMatrix,
                                             currentMatrix));
            }
            mousePressedMatrix = null;

        } else if (eventId == DisplayEvent.FRAME_DONE) {
            if ( !receivedFirstFrameDone) {
                doneFirstFrame();
            }
            lastFrameDoneTime = System.currentTimeMillis();
            //            System.err.println(lastFrameDoneTime+ " FRAME DONE");
        } else if (eventId == DisplayEvent.COMPONENT_RESIZED) {
            final int myComponentResizeCnt = ++componentResizeCnt;
            Misc.runInABit(200, new Runnable() {
                public void run() {
                    if (isDestroyed) {
                        return;
                    }
                    if (myComponentResizeCnt == componentResizeCnt) {
                        updateAnnotations();
                        updateDisplayList();
                    }
                }
            });

        } else {
            //      System.err.println ("??? id:" + eventId);
        }
    }




    /**
     * Get the last time we've seen a FRAME_DONE event
     *
     * @return tiem of last frame done event
     */
    public long getLastFrameDoneTime() {
        return lastFrameDoneTime;
    }

    /**
     *  We have received the very first framedone event. Tell the controls
     */
    protected void doneFirstFrame() {
        //call setLastActive to set the active border
        if (getIsShared()) {
            setLastActive(lastActive);
        }

        receivedFirstFrameDone = true;
        List controls = getControls();
        for (int i = controls.size() - 1; i >= 0; i--) {
            DisplayControl control = (DisplayControl) controls.get(i);
            control.firstFrameDone();
        }
        updateDisplayList();
    }

    /**
     * Show the wait cursor in a bit
     *
     * @param timeStamp THe virtual timestamp that keeps track if we have had
     * any firther wait cursor calls since this call. If so then don't do
     * anything
     */
    private void waitCursorInABit(final int timeStamp) {
        Misc.runInABit(500, new Runnable() {
            public void run() {
                if (timeStamp != currentWaitKey) {
                    return;
                }
                outstandingWaits++;
                if (outstandingWaits == 1) {
                    getIdvUIManager().showWaitCursor();
                }
            }
        });
    }

    /**
     * Responds to ItemEvents handled by an ItemListener; in this
     * class, from JCheckBoxes.
     * This method needed to instantiate the 3d viewContext as an ItemListener.
     *
     * @param  e ItemEvent whose state has changed.
     */

    public void itemStateChanged(ItemEvent e) {
        Object source = e.getItemSelectable();
    }



    /**
     * Required interface for ActionEvents, to implement ActionListener
     * for the UI objects such as JButton-s and MenuItem-s
     *
     * @param event an ActionEvent
     */
    public void actionPerformed(ActionEvent event) {}


    /** image panel */
    private ImagePanel imagePanel;

    /** flag for using the image panel */
    boolean usingImagePanel = false;

    /**
     * Make frames
     */
    public void makeFrames() {
        ImageSequenceGrabber isg = new ImageSequenceGrabber(this, null, true);
    }

    /**
     * Do we use the display
     *
     * @return true if we do
     */
    public boolean useDisplay() {
        if ( !usingImagePanel) {
            return false;
        }
        usingImagePanel = false;
        contentsWrapper.removeAll();
        contentsWrapper.add(BorderLayout.CENTER, innerContents);
        contentsWrapper.revalidate();
        //        animation.setEnabled(true);
        return true;

    }

    /**
     * Use the images
     */
    public void useImages() {
        if (true) {
            return;
        }
        if (usingImagePanel) {
            return;
        }
        if (imagePanel == null) {
            imagePanel = new ImagePanel();
        }
        //        animation.setEnabled(false);
        contentsWrapper.removeAll();
        contentsWrapper.add(BorderLayout.CENTER, imagePanel);
        contentsWrapper.revalidate();
        usingImagePanel = true;
    }

    /**
     * Use the images
     *
     * @param images list of images
     * @param andShow true to show
     */
    public void useImages(List images, boolean andShow) {
        if (true) {
            return;
        }
        if (imagePanel == null) {
            imagePanel = new ImagePanel();
        }
        imagePanel.setFiles(images);
        if (animation != null) {
            imagePanel.setSelectedFile(animation.getCurrent());
        }
        if (andShow) {
            useImages();
        }
    }


    /**
     * Start the image capture if we are not currently doing that.
     * Creates the {@link ucar.unidata.idv.ui.ImageSequenceGrabber}.
     */
    public synchronized void startImageCapture() {
        if (isg != null) {
            LogUtil.userMessage("The Movie Capture dialog window is open");
            isg.show();
            return;
        }
        isg = new ImageSequenceGrabber(this);
    }


    /**
     *  The given grabber is done. Null out the <code>isg</code>
     * member if it == the given grabber.
     *
     * @param grabber The ImageSequenceGrabber to clear
     */
    public void clearImageGrabber(ImageSequenceGrabber grabber) {
        if (isg == grabber) {
            isg = null;
        }
    }


    /**
     * This shows and brings to the front the display window, sleeps for
     * a bit and then makes a screen snapshot and writes it out
     * using the given archive path (which is a directory with a file root)
     *
     * @param archivePath The  path to write to
     */
    public void writeTestArchive(String archivePath) {
        try {
            String imageFile = archivePath + ".png";
            toFront();
            Misc.sleepSeconds(1);
            System.err.println("Writing image:" + imageFile);
            writeImage(imageFile, true);
        } catch (Exception exc) {
            logException("writeTestArchive", exc);
        }

    }


    /**
     * show the window I am in
     */
    public void showWindow() {
        toFront();
    }


    /**
     * Create a screen image and write it to the given file
     *
     * @param fileName File to write image to
     */
    public void writeImage(String fileName) {
        writeImage(fileName, false);
    }

    /**
     * Create a screen image and write it to the given file
     *
     * @param fileName File to write image to
     * @param block If true then do the write in this thread
     */
    public void writeImage(String fileName, boolean block) {
        writeImage(new File(fileName), block);
    }

    /**
     * Create a screen image and write it to the given file
     *
     * @param fileName File to write image to
     * @param block If true then do the write in this thread
     * @param quality jpeg quality
     */
    public void writeImage(String fileName, boolean block, float quality) {
        writeImage(new File(fileName), block, quality);
    }

    /**
     * Create a screen image and write it to the given file
     *
     * @param file File to write image to
     */
    public void writeImage(File file) {
        writeImage(file, false);
    }

    /**
     * Create a screen image and write it to the given file
     *
     * @param file File to write image to
     * @param block If true then do the write in this thread
     */
    public void writeImage(File file, boolean block) {
        writeImage(file, block, false);
    }


    /**
     * Create a screen image and write it to the given file
     *
     * @param file File to write image to
     * @param block If true then do the write in this thread
     * @param sync Synchronize before capturing
     */
    public void writeImage(File file, boolean block, boolean sync) {
        toFront();
        Misc.sleep(100);
        master.saveCurrentDisplay(file, sync, block);
    }

    /**
     * Create a screen image and write it to the given file
     *
     * @param file File to write image to
     * @param block If true then do the write in this thread
     * @param quality jpeg quality
     */
    public void writeImage(File file, boolean block, float quality) {
        toFront();
        Misc.sleep(100);
        master.saveCurrentDisplay(file, false, block, quality);
    }

    /**
     * Save this state of this view manager. This simply passes
     * through to {@link VMManager#saveViewManagerState(ViewManager)}
     */
    public void doSaveState() {
        getVMManager().saveViewManagerState(this);
    }


    /**
     * User has requested saving display as an image. Prompt
     * for a filename and save the image to it.
     */
    public void doSaveImage() {
        doSaveImage(false);
    }


    /**
     * Print an image
     */
    public void doPrintImage() {
        try {
            toFront();
            PrinterJob printJob = PrinterJob.getPrinterJob();
            printJob.setPrintable(
                ((DisplayImpl) getMaster().getDisplay()).getPrintable());
            if ( !printJob.printDialog()) {
                return;
            }
            printJob.print();
        } catch (Exception exc) {
            logException("There was an error printing the image", exc);
        }
    }

    /**
     * Set the view
     *
     * @param view  the view description
     */
    public void setView(String view) {
        if (getMaster() instanceof NavigatedDisplay) {
            NavigatedDisplay navDisplay = (NavigatedDisplay) getMaster();
            navDisplay.setView(view);
        }
    }

    /**
     * User has requested saving display as an image. Prompt
     * for a filename and save the image to it.
     */
    public void doSaveImageInThread() {
        Misc.run(this, "doSaveImage");
    }





    /**
     * Does this viewmanager have any bounds that are visible.
     * This is overwritten by the MapViewManager
     *
     * @return bounds
     */
    public GeoLocationInfo getVisibleGeoBounds() {
        return null;
    }

    /**
     * Check to see if the file is a vector graphics file
     *
     * @param filename  name of the file
     *
     * @return  true if it has the right extension
     */
    public static boolean isVectorGraphicsFile(String filename) {
        return filename.toLowerCase().endsWith(".pdf")
               || filename.toLowerCase().endsWith(".ps")
               || filename.toLowerCase().endsWith(".eps")
               || filename.toLowerCase().endsWith(".svg");
    }

    /**
     * Save the image (and the bundle that does with it);
     *
     * @param andSaveBundle true to save the bundle also
     */
    public void doSaveImage(boolean andSaveBundle) {

        SecurityManager backup = System.getSecurityManager();
        System.setSecurityManager(null);
        try {
            if (hiBtn == null) {
                hiBtn  = new JRadioButton("High", true);
                medBtn = new JRadioButton("Medium", false);
                lowBtn = new JRadioButton("Low", false);
                GuiUtils.buttonGroup(hiBtn, medBtn).add(lowBtn);
                backgroundTransparentBtn = new JCheckBox("BG Transparent");
                backgroundTransparentBtn.setToolTipText(
                    "Set the background color to be transparent");
                mainDisplayBtn = new JRadioButton("View", true);
                contentsBtn    = new JRadioButton("View & Legend", false);
                fullWindowBtn  = new JRadioButton("Full Window", false);
                GuiUtils.buttonGroup(mainDisplayBtn,
                                     fullWindowBtn).add(contentsBtn);
            }

            if (publishCbx == null) {
                publishCbx = getIdv().getPublishManager().makeSelector();
            }
            hiBtn.setBorder(null);
            medBtn.setBorder(null);
            lowBtn.setBorder(null);
            mainDisplayBtn.setBorder(null);
            contentsBtn.setBorder(null);
            fullWindowBtn.setBorder(null);
            List comps = new ArrayList();
            comps.add(new JLabel("Quality:"));
            comps.add(GuiUtils.inset(GuiUtils.vbox(hiBtn, medBtn, lowBtn),
                                     new Insets(2, 8, 10, 0)));

            comps.add(new JLabel("Capture What:"));
            comps.add(GuiUtils.inset(GuiUtils.vbox(mainDisplayBtn,
                    contentsBtn, fullWindowBtn), new Insets(2, 8, 10, 0)));

            comps.add(backgroundTransparentBtn);
            if (publishCbx != null) {
                comps.add(GuiUtils.inset(publishCbx,
                                         new Insets(10, 0, 0, 0)));
            }
            JComponent accessory = GuiUtils.vbox(comps);


            PatternFileFilter captureFilter =
                new PatternFileFilter(
                    ".+\\.jpg|.+\\.gif|.+\\.jpeg|.+\\.png|.+\\.pdf|.+\\.ps|.+\\.svg",
                    "Image files (*.jpg,*.gif,*.png,*.pdf,*.ps,*.svg)");
            List            filters = Misc.newList(captureFilter);
            GeoLocationInfo bounds  = getVisibleGeoBounds();
            if (bounds != null) {
                filters.add(KmlDataSource.FILTER_KML);
            }

            String filename = FileManager.getWriteFile(filters,
                                  FileManager.SUFFIX_JPG,
                                  GuiUtils.top(GuiUtils.inset(accessory, 5)));



            if (filename != null) {
                if (isVectorGraphicsFile(filename)) {
                    if (vectorRenderer == null) {
                        vectorRenderer = new VectorGraphicsRenderer(this);
                    }
                    if (vectorRenderer.showConfigDialog()) {
                        vectorRenderer.renderTo(filename);
                        getIdv().getPublishManager().publishContent(filename,
                                this, publishCbx);
                    }
                    System.setSecurityManager(backup);
                    return;
                }

                float quality = 1.0f;
                if (medBtn.isSelected()) {
                    quality = 0.6f;
                } else if (lowBtn.isSelected()) {
                    quality = 0.2f;
                }
                if (false && mainDisplayBtn.isSelected()) {
                    //For now we'll try the robot based capture for everything.
                    writeImage(new File(filename), false, quality);
                } else {
                    Component comp;
                    String    whichComponent;
                    if (fullWindowBtn.isSelected()) {
                        comp           = getDisplayWindow().getComponent();
                        whichComponent = "full window";
                    } else if (mainDisplayBtn.isSelected()) {
                        comp           = getMaster().getComponent();
                        whichComponent = "main display";
                    } else {
                        comp           = getContents();
                        whichComponent = "contents";
                    }
                    Dimension             dim = comp.getSize();
                    Point                 loc = comp.getLocationOnScreen();
                    GraphicsConfiguration gc =
                        comp.getGraphicsConfiguration();
                    Robot robot = new Robot(gc.getDevice());

                    if ((gc.getBounds().x > 0) || (gc.getBounds().y > 0)) {
                        System.err.println("Offsetting location:" + loc
                                           + " by gc bounds: "
                                           + gc.getBounds().x + " "
                                           + gc.getBounds().y);
                        loc.x -= gc.getBounds().x;
                        loc.y -= gc.getBounds().y;
                        System.err.println("new location:" + loc);
                    }

                    if ((dim.width <= 0) || (dim.height <= 0)) {
                        throw new IllegalStateException("Bad component size:"
                                + dim.width + " X " + dim.height
                                + " for component:" + whichComponent);
                    }

                    toFront();
                    Misc.sleep(250);
                    BufferedImage image;

                    try {
                        image =
                            robot.createScreenCapture(new Rectangle(loc.x,
                                loc.y, dim.width, dim.height));
                    } catch (Exception exc) {
                        logException("Error capturing image for component:"
                                     + whichComponent + " location:" + loc.x
                                     + "x" + loc.y + " dimension:"
                                     + dim.width + "x" + dim.height, exc);
                        return;
                    }


                    if (backgroundTransparentBtn.isSelected()) {
                        image = ImageUtils.makeColorTransparent(image,
                                getBackground());
                    }

                    if (KmlDataSource.isKmlFile(filename)) {
                        if ( !checkForKmlImageCapture()) {
                            return;
                        }
                        String kmlFilename = filename;
                        String suffix      = ".png";
                        filename = IOUtil.stripExtension(filename) + suffix;
                        if (kmlFilename.endsWith(".kml")) {
                            KmlDataSource.writeToFile(kmlFilename, bounds,
                                    filename);
                        } else {
                            String tail = IOUtil.stripExtension(
                                              IOUtil.getFileTail(
                                                  kmlFilename));
                            ZipOutputStream zos = new ZipOutputStream(
                                                      new FileOutputStream(
                                                          kmlFilename));
                            zos.putNextEntry(new ZipEntry(tail + ".kml"));
                            byte[] kmlBytes = KmlDataSource.createKml(bounds,
                                                  tail + suffix).getBytes();
                            zos.write(kmlBytes, 0, kmlBytes.length);
                            String tmpFile =
                                IOUtil.joinDir(getIdv().getObjectStore()
                                    .getUserTmpDirectory(), Math.random()
                                        + "" + System.currentTimeMillis()
                                        + suffix);
                            ImageUtils.writeImageToFile(image, tmpFile,
                                    quality);
                            byte[] imageBytes = IOUtil.readBytes(
                                                    new FileInputStream(
                                                        tmpFile));
                            zos.putNextEntry(new ZipEntry(tail + suffix));
                            zos.write(imageBytes, 0, imageBytes.length);
                            zos.close();
                            getIdv().getPublishManager().publishContent(
                                kmlFilename, this, publishCbx);
                            return;
                        }
                    }
                    ImageUtils.writeImageToFile(image, filename, quality);
                    getIdv().getPublishManager().publishContent(filename,
                            this, publishCbx);
                }
                if (andSaveBundle) {
                    filename = IOUtil.stripExtension(filename) + ".jnlp";
                    getIdv().getPersistenceManager().doSave(filename);
                }
            }
        } catch (Exception exp) {
            logException("doSaveImage", exp);
        }
        // for webstart
        System.setSecurityManager(backup);

    }


    /**
     * Check if its ok to capture a kmz file
     *
     * @return ok to capture kmz
     */
    protected boolean checkForKmlImageCapture() {
        return false;
    }


    /**
     * Paint the display list in a 2D graphics
     *
     * @param graphics  the graphics to paint on
     * @param displayControls  the list of controls
     * @param width  width
     * @param height height
     * @param bottom bottom position
     * @param color  label color
     * @param font   label font
     *
     * @return the height of the top label
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   problem with VisAD data
     */
    public int paintDisplayList(Graphics2D graphics,
                                List<DisplayControl> displayControls,
                                int width, int height, boolean bottom,
                                Color color, Font font)
            throws VisADException, RemoteException {
        if (displayControls == null) {
            displayControls = getControls();
        }

        int  cnt = 0;
        Font f   = ((font != null)
                    ? font
                    : getDisplayListFont());
        graphics.setFont(f);
        FontMetrics fm         = graphics.getFontMetrics();
        int         lineHeight = fm.getAscent() + fm.getDescent();
        int         startY;
        int         offsetY;
        int         totalHeight = 0;
        if (bottom) {
            startY      = height - 4;
            offsetY     = -(lineHeight + 1);
            totalHeight = 4;
        } else {
            startY      = 2 + lineHeight;
            offsetY     = (lineHeight + 1);
            totalHeight = lineHeight;
        }

        for (DisplayControl control : displayControls) {
            if ( !control.getShowInDisplayList()) {
                continue;
            }
            Data data = control.getDataForDisplayList();
            if (data == null) {
                continue;
            }
            String text = null;
            if (data instanceof visad.Text) {
                text = ((visad.Text) data).getValue();
            } else if (data instanceof FieldImpl) {
                Animation anime = getAnimation();
                if (anime != null) {
                    Real now = anime.getCurrentAnimationValue();
                    if (now != null) {
                        FieldImpl fi = (FieldImpl) data;
                        Data rangeValue = fi.evaluate(now,
                                              Data.NEAREST_NEIGHBOR,
                                              Data.NO_ERRORS);
                        if ((rangeValue != null)
                                && (rangeValue instanceof visad.Text)) {
                            text = ((visad.Text) rangeValue).getValue();
                        }
                    }
                }
            }
            if ((text == null) || (text.length() == 0)) {
                continue;
            }
            Color c = ((color != null)
                       ? color
                       : getDisplayListColor());
            if (c == null) {
                c = ((ucar.unidata.idv.control.DisplayControlImpl) control)
                    .getDisplayListColor();
            }
            graphics.setColor(c);
            int lineWidth = fm.stringWidth(text);
            graphics.drawString(text, width / 2 - lineWidth / 2,
                                startY + offsetY * cnt);
            totalHeight += Math.abs(offsetY);
            cnt++;
        }
        return totalHeight;
    }




    /**
     * Hook into the publishing interface to &quot;publish&quot;
     * the screen image.
     */
    public void doPublishImage() {
        doPublishImage(null);
    }



    /**
     * Hook into the publishing interface to &quot;publish&quot;
     * the screen image.
     *
     * @param props Properties to pass through to the publish manager.
     */
    public void doPublishImage(final String props) {
        // user has requested saving display as an image
        SecurityManager backup = System.getSecurityManager();
        System.setSecurityManager(null);
        try {
            Misc.run(new Runnable() {
                public void run() {
                    String uid  = Misc.getUniqueId();
                    String tail = uid + ".png";
                    String file =
                        IOUtil.joinDir(getStore().getUserTmpDirectory(),
                                       tail);
                    writeImage(file, true);
                    getPublishManager().doPublish("Publish JPEG file", file,
                            props);
                }
            });
        } catch (Exception exp) {
            logException("doSaveImage", exp);
        }
        // for webstart
        System.setSecurityManager(backup);
    }



    /**
     * Helper to log errors
     *
     * @param msg The error message
     * @param exc The exception
     */
    public void logException(String msg, Throwable exc) {
        LogUtil.printException(log_, msg, exc);
    }




    /**
     * Class IdvKeyboardBehavior is used to route keyboard events
     * from the display to this ViewManager.
     *
     *
     * @author IDV development team
     */
    private static class IdvKeyboardBehavior implements KeyboardBehavior,
            Removable {

        /** The ViewManager */
        ViewManager viewManager;

        /**
         * Create me
         *
         * @param viewManager The ViewManager
         */
        public IdvKeyboardBehavior(ViewManager viewManager) {
            this.viewManager = viewManager;
        }

        /**
         * Remove me
         */
        public void doRemove() {
            viewManager = null;
        }


        /**
         * Needed for the interface
         *
         * @param function function
         * @param keycode keycode
         * @param modifiers modifiers
         */
        public void mapKeyToFunction(int function, int keycode,
                                     int modifiers) {}

        /**
         * Route the key pressed event to the ViewManager
         *
         * @param keyEvent The event
         */
        public void processKeyEvent(KeyEvent keyEvent) {
            viewManager.keyWasTyped(keyEvent);
        }

        /**
         * Needed for the interface
         *
         * @param function function
         */
        public void execFunction(int function) {}

    }

    ;

    /**
     * make the color set menu
     *
     * @return color menu
     */
    public JMenu makeColorMenu() {
        return GuiUtils.makeDynamicMenu("Color", this, "initColorMenu");
    }

    /**
     * Dynamically add the menu items into the color menu
     *
     * @param colorMenu The Color menu to initialize
     */
    public void initColorMenu(JMenu colorMenu) {
        colorMenu.add(GuiUtils.setIcon(GuiUtils.makeMenuItem("Set Colors...",
                this,
                "showColorPairDialog"), "/auxdata/ui/icons/color_swatch.png"));

        JMenu deleteMenu = new JMenu("Delete");
        colorMenu.add(deleteMenu);
        colorMenu.addSeparator();

        boolean addedToDelete = false;

        XmlResourceCollection colors = getResourceManager().getXmlResources(
                                           IdvResourceManager.RSC_COLORPAIRS);

        boolean didone = false;
        for (int colorResourceIdx = 0; colorResourceIdx < colors.size();
                colorResourceIdx++) {
            Element root = colors.getRoot(colorResourceIdx, false);
            if (root == null) {
                continue;
            }
            List nodes = XmlUtil.findChildren(root, TAG_COLORPAIR);
            for (int colorIdx = 0; colorIdx < nodes.size(); colorIdx++) {
                Element node = (Element) nodes.get(colorIdx);
                final Color fg = XmlUtil.getAttribute(node, "foreground",
                                     (Color) Color.black);
                final Color bg = XmlUtil.getAttribute(node, "background",
                                     (Color) Color.white);
                final String label = XmlUtil.getAttribute(node, "label");
                JMenuItem mi = new JMenuItem(GuiUtils.getLocalName(label,
                                   (colorResourceIdx == 0)));
                try {
                    JLabel lbl = new JLabel("  T  ");
                    lbl.setSize(new Dimension(30, 20));
                    lbl.setForeground(fg);
                    lbl.setBackground(bg);
                    lbl.setOpaque(true);
                    Image image = ImageUtils.getImage(lbl);
                    mi.setIcon(new ImageIcon(image));
                    //                    if(!didone)
                    //                        didone = !GuiUtils.showOkCancelDialog(null,null,lbl,null);
                } catch (Exception exc) {
                    exc.printStackTrace();
                }

                colorMenu.add(mi);
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        setColors(fg, bg);
                    }
                });

                if (colorResourceIdx == 0) {
                    addedToDelete = true;
                    deleteMenu.add(GuiUtils.makeMenuItem(label, this,
                            "removeColorPair", label));
                }
            }
        }

        if ( !addedToDelete) {
            deleteMenu.setEnabled(false);
            deleteMenu.setToolTipText("No user defined colors");
        }
        Msg.translateTree(colorMenu);
    }



    /**
     * Remove the named color pair from the users xml
     *
     * @param name The name of the color pair to remove
     */
    public void removeColorPair(String name) {
        XmlResourceCollection colors = getResourceManager().getXmlResources(
                                           IdvResourceManager.RSC_COLORPAIRS);
        Element root = colors.getWritableRoot("<colorpairs></colorpairs>");
        Element colorNode = XmlUtil.findElement(root, TAG_COLORPAIR, "label",
                                name);
        try {
            if (colorNode != null) {
                root.removeChild(colorNode);
                colors.writeWritable();
            }
        } catch (Exception exc) {
            LogUtil.printException(log_, "writing chooser xml", exc);
        }

    }


    /**
     * Show the dialog that allows the user to change the foreground/background colors
     */
    public void showColorPairDialog() {
        //TODO
        final JDialog dialog = new JDialog((JFrame) null,
                                           "Background/Foreground Color",
                                           true);
        JLabel bgLbl = GuiUtils.rLabel("Background:");
        JLabel fgLbl = GuiUtils.rLabel("Foreground:");
        final JComponent[] bgComps =
            GuiUtils.makeColorSwatchWidget(getBackground(),
                                           "Set Background Color");
        final JComponent[] fgComps =
            GuiUtils.makeColorSwatchWidget(getForeground(),
                                           "Set Foreground Color");

        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JPanel         top      = GuiUtils.doLayout(new Component[] {
            bgLbl, bgComps[0], bgComps[1], fgLbl, fgComps[0], fgComps[1]
        }, 3, GuiUtils.WT_NYN, GuiUtils.WT_N);


        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_SAVEAS)) {
                    saveColors(fgComps[0].getBackground(),
                               bgComps[0].getBackground());
                    return;
                }

                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_APPLY)) {
                    setColors(fgComps[0].getBackground(),
                              bgComps[0].getBackground());
                }
                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_CANCEL)) {
                    dialog.dispose();
                }
            }
        };


        JPanel contents = GuiUtils.vbox(top,
                                        GuiUtils.makeButtons(listener,
                                            new String[] {
                                                GuiUtils.CMD_SAVEAS,
                GuiUtils.CMD_APPLY, GuiUtils.CMD_OK, GuiUtils.CMD_CANCEL }));
        dialog.getContentPane().add(GuiUtils.inset(contents, 5));
        dialog.pack();
        dialog.setLocation(GuiUtils.getLocation(null));
        dialog.setVisible(true);
    }


    /**
     * Prompt the user for a color name, make sure it is unique, and write
     * out the given color pair.
     *
     * @param fg Foreground color
     * @param bg Background color
     */
    private void saveColors(Color fg, Color bg) {
        String name = "";
        XmlResourceCollection colors = getResourceManager().getXmlResources(
                                           IdvResourceManager.RSC_COLORPAIRS);


        Document colorDoc =
            colors.getWritableDocument("<colorpairs></colorpairs>");
        Element root = colors.getWritableRoot("<colorpairs></colorpairs>");

        while (true) {
            name = GuiUtils.getInput(
                "Please provide a name to save the colors as",
                "Color name: ", name);
            if (name == null) {
                return;
            }
            name = name.trim();
            if (name.length() == 0) {
                LogUtil.userMessage(
                    "Please enter a name for the new color pair");
                continue;
            }
            List    nodes     = XmlUtil.findChildren(root, TAG_COLORPAIR);
            boolean nameOk    = true;
            Element colorPair = null;
            for (int colorIdx = 0; colorIdx < nodes.size(); colorIdx++) {
                Element node = (Element) nodes.get(colorIdx);
                if (XmlUtil.getAttribute(node, "label", "").equals(name)) {
                    int result =
                        GuiUtils.showYesNoCancelDialog(
                            GuiUtils.getFrame(fullContents),
                            "A color pair with the name: " + name
                            + " already exists."
                            + "Do you want to overwrite it?", "Color name exists");
                    if (result == 2) {
                        return;
                    }
                    if (result == 1) {
                        nameOk = false;
                    }
                    colorPair = node;
                    break;
                }
            }
            if ( !nameOk) {
                continue;
            }
            if (colorPair == null) {
                colorPair = colorDoc.createElement(TAG_COLORPAIR);
            }
            colorPair.setAttribute("label", name);
            XmlUtil.setAttribute(colorPair, "foreground", fg);
            XmlUtil.setAttribute(colorPair, "background", bg);
            root.appendChild(colorPair);
            try {
                colors.writeWritable();
            } catch (Exception exc) {
                LogUtil.printException(log_, "writing chooser xml", exc);
            }
            return;
        }
    }

    /**
     * Get the foreground color or white if it is null
     *
     * @return The foreground color
     */
    public Color getForeground() {
        if (foreground == null) {
            foreground = getDefaultForeground();
        }
        return foreground;
    }


    /**
     * Get the default foreground color
     *
     * @return the color
     */
    protected Color getDefaultForeground() {
        return getStore().get(PREF_FGCOLOR, Color.white);
    }


    /**
     * Get the default background color
     *
     * @return the color
     */
    protected Color getDefaultBackground() {
        return getStore().get(PREF_BGCOLOR, Color.black);
    }

    /**
     * Set the foreground color
     *
     * @param c The new foreground color
     */
    public void setForeground(Color c) {
        foreground = c;
    }




    /**
     * Get the background color or black if it is null
     *
     * @return The background color
     */
    public Color getBackground() {
        if (background == null) {
            background = getDefaultBackground();
        }
        return background;
    }


    /**
     * Set the background color
     *
     * @param c The new background color
     */
    public void setBackground(Color c) {
        background = c;
    }

    /**
     *  Set the SideLegend property.
     *
     *  @param value The new value for SideLegend
     */
    public void setSideLegend(SideLegend value) {
        sideLegend = value;
        if (sideLegend != null) {
            sideLegend.setViewManager(this);
        }
    }

    /**
     *  Get the SideLegend property.
     *
     *  @return The SideLegend
     */
    public SideLegend getSideLegend() {
        return sideLegend;
    }

    /**
     * are the toolbars floatable
     *
     * @return toolbars floatable
     */
    public boolean getToolbarsFloatable() {
        String prop = getSkinProperty(PREF_TOOLBARSFLOATABLE);
        if (prop == null) {
            return false;
        }
        return new Boolean(prop).booleanValue();
    }


    /**
     * Turn  on/off  the showing of the toolbars
     *
     * @param v Should show
     */
    public void setShowToolBars(boolean v) {
        showToolbars = v;
    }


    /**
     * Set whether this view manager is the last one active. That is,
     * is the considered to be the current default view manager that
     * display controls add themselves into.
     *
     * @param b Is active
     */
    public void setLastActive(boolean b) {
        if ( !getIsShared()) {
            return;
        }

        lastActive = b;
        if (lastActive) {
            lastTimeActivated = System.currentTimeMillis();
        }
        VMManager  vmManager = getVMManager();
        JComponent contents  = innerContents;
        if ((contents != null) && (vmManager != null)) {
            boolean haveMany = vmManager.haveMoreThanOneMainViewManager();
            if (getIsShared()) {
                if (haveMany && b) {
                    contents.setBorder(getHighlightBorder());
                } else {
                    contents.setBorder(getNormalBorder());
                }
            }
        }
        IdvUIManager idvUIManager = getIdvUIManager();
        if (idvUIManager != null) {
            idvUIManager.viewManagerActiveChanged(this);
        }
    }


    /**
     * Get the normal border
     *
     * @return normal border
     */
    public static Border getNormalBorder() {
        if (normalBorder == null) {
            int    bw    = borderWidth;
            Border outer = BorderFactory.createEmptyBorder(bw, bw, bw, bw);
            normalBorder = BorderFactory.createCompoundBorder(outer,
                    lineBorder);
        }
        return normalBorder;
    }

    /**
     * Get the border to use when this ViewManager is the currently selected ViewManager
     *
     * @return highlight border
     */
    public static Border getHighlightBorder() {
        if (highlightBorder == null) {
            int bw = borderWidth;
            Border outer = new MatteBorder(new Insets(bw, bw, bw, bw),
                                           borderHighlightColor);
            highlightBorder = BorderFactory.createCompoundBorder(outer,
                    lineBorder);
        }
        return highlightBorder;
    }

    /**
     * Sets the color used to denote the currently selected panel.
     *
     * @param c The new color for the currently selected panel's border.
     */
    public static void setHighlightBorder(Color c) {
        borderHighlightColor = c;
        highlightBorder      = null;
    }

    /**
     * Should we show the highlight border
     *
     * @return should show the highlight border
     */
    public boolean showHighlight() {
        boolean haveMany = getVMManager().haveMoreThanOneMainViewManager();
        return haveMany && lastActive;
    }



    /**
     * Get the time that this was last the active VM
     *
     * @return The time of last activation
     */
    public long getLastTimeActivated() {
        return lastTimeActivated;
    }



    /**
     * Set the DisplayBounds property.
     *
     * @param value The new value for DisplayBounds
     */
    public void setDisplayBounds(Rectangle value) {
        displayBounds = value;
    }

    /**
     * Get the DisplayBounds property.
     *
     * @return The DisplayBounds
     */
    public Rectangle getDisplayBounds() {
        if (master != null) {
            return master.getScreenBounds();
        }
        return displayBounds;
    }



    /**
     * Are we in full screen mode.
     *
     * @return full screen mode.
     */
    protected boolean isFullScreen() {
        return fullScreenWindow != null;
    }

    /**
     * Toggle full screen
     */
    public void toggleFullScreen() {
        if (isFullScreen()) {
            resetFullScreen();
        } else {
            setFullScreen();
        }
    }

    /**
     * Go back to normal screen
     */
    public void resetFullScreen() {
        if ((fullScreenWindow == null) || (innerContents == null)) {
            return;
        }

        Runnable runnable = new Runnable() {
            public void run() {
                //                    System.err.println("reset full screen:" + Thread.currentThread());
                JComponent navComponent = getComponent();
                innerContents.add(BorderLayout.CENTER, navComponent);
                fullScreenWindow.setVisible(false);
                fullScreenWindow.dispose();
                fullScreenWindow = null;
                AnimationWidget animationWidget = getAnimationWidget();
                if ((animationWidget != null) && (animationHolder != null)) {
                    //            animationHolder.add(BorderLayout.CENTER,
                    //                                animationWidget.getContents());
                    animationHolder.add(animationWidget.getContents());
                }
            }
        };
        //        GuiUtils.invokeInSwingThread(runnable);
        Misc.run(runnable);
    }

    /**
     * Go to full screen mode
     */
    public void setFullScreen() {

        Dimension fixedSize = null;
        if ((fullScreenWidth > 0) && (fullScreenHeight > 0)) {
            fixedSize = new Dimension(fullScreenWidth, fullScreenHeight);
        }
        if (innerContents == null) {
            return;
        }

        if (fullScreenWindow != null) {
            resetFullScreen();
            return;
        }
        JComponent navComponent = getComponent();
        innerContents.remove(navComponent);


        Dimension theSize = fixedSize;
        if (theSize == null) {
            theSize        = Toolkit.getDefaultToolkit().getScreenSize();
            theSize.height -= 50;
        }
        navComponent.setMinimumSize(theSize);
        navComponent.setPreferredSize(theSize);
        AnimationWidget animationWidget = getAnimationWidget();
        JComponent      rightPanel      = (JComponent) GuiUtils.filler();
        if (animationWidget != null) {
            rightPanel = animationWidget.getContents();
        }

        JComponent menuBar = doMakeMenuBar();
        JComponent cancelBtn1 =
            GuiUtils.makeImageButton("/auxdata/ui/icons/cancel.gif", this,
                                     "resetFullScreen");
        JComponent cancelBtn2 =
            GuiUtils.makeImageButton("/auxdata/ui/icons/cancel.gif", this,
                                     "resetFullScreen");
        //If they set the width to be smallish then don't include the
        //animation and the menu bar
        if (theSize.width < 300) {
            rightPanel = new JPanel();
            menuBar    = new JPanel();
        }

        JPanel top =
            GuiUtils.leftRight(GuiUtils.hbox(GuiUtils.bottom(cancelBtn1),
                                             menuBar), rightPanel);
        JPanel bottom = GuiUtils.left(GuiUtils.hbox(cancelBtn2,
                            new JLabel(" ")));
        JPanel contents = GuiUtils.topCenterBottom(top, navComponent, bottom);

        fullScreenWindow = new JFrame();
        if (fixedSize == null) {
            fullScreenWindow.setUndecorated(true);
        }
        fullScreenWindow.getContentPane().add(contents);


        if (fixedSize == null) {
            fullScreenWindow.setSize(
                Toolkit.getDefaultToolkit().getScreenSize());
        } else {}
        fullScreenWindow.pack();
        int yOffset = 0;
        if (GuiUtils.isMac()) {
            yOffset = 23;
        }
        if (fixedSize == null) {
            fullScreenWindow.setLocation(0, 0 + yOffset);
        } else {
            fullScreenWindow.setLocation(20, 20 + yOffset);
        }
        fullScreenWindow.setVisible(true);
        navComponent.requestFocus();
    }







    /**
     * Set the click to focus property. When true this VM only gets focus on a mouse click
     *
     * @param value The value
     */
    public void setClickToFocus(boolean value) {
        clickToFocus = value;
    }

    /**
     * Get the click to focus property. When true this VM only gets focus on a mouse click
     *
     * @return The click to focus property
     */
    public boolean getClickToFocus() {
        return clickToFocus;
    }

    /**
     * Set the Aliases property.
     *
     * @param value The new value for Aliases
     */
    public void setAliases(List value) {
        aliases = value;
        if ((aliases != null) && (aliases.size() > 0)) {
            aliases = Misc.newList(aliases.get(0));
        }
    }

    /**
     * Get the Aliases property.
     *
     * @return The Aliases
     */
    public List getAliases() {
        //Only return the first one
        if (aliases.size() > 0) {
            return Misc.newList(aliases.get(0));
        }
        return aliases;
    }


    /**
     * Set the display matrix array
     *
     * @param newMatrix  display matrix
     * @throws VisADException  problem in VisAD display
     * @throws RemoteException  problem in remote VisAD display
     */
    public void setDisplayMatrix(double[] newMatrix)
            throws VisADException, RemoteException {
        initMatrix = newMatrix;
        if ((newMatrix == null) || !hasDisplayMaster()) {
            return;
        }
        double[] displayMatrix = getProjectionControl().getMatrix();
        if (displayMatrix.length != newMatrix.length) {
            initMatrix = ProjectionControl.matrixDConvert(newMatrix);
        }
        if (getIdv().getArgsManager().getIsOffScreen()) {
            //            System.err.println("Setting projection matrix");
        }
        getMaster().setProjectionMatrix(initMatrix);
    }

    /**
     * Get the display matrix array
     *
     * @return display matrix
     */
    public double[] getDisplayMatrix() {
        if (getProjectionControl() == null) {
            return initMatrix;
        }
        return getProjectionControl().getMatrix();
    }



    /**
     * Toggle the animation string visibility.
     * @param visible  true to make it visible
     * @deprecated Use setAniReadout now
     */
    public void setAnimationStringVisible(boolean visible) {
        setAniReadout(visible);
    }

    /**
     * Get the animation string visibility.
     * @return visible  true to make it visible
     * @deprecated Use getAniReadout now
     */
    public boolean getAnimationStringVisible() {
        return getAniReadout();
    }

    /**
     * Toggle the top component visibility.
     * @param visible  true to make it visible
     */
    public void setTopBarVisible(boolean visible) {
        setBp(PREF_TOPBAR_VISIBLE, visible);
    }

    /**
     * Get the top component visibility.
     * @return visible  true to make it visible
     */
    public boolean getTopBarVisible() {
        return getBp(PREF_TOPBAR_VISIBLE);
    }


    /**
     * Set the  show animation readout flag
     *
     * @param value The value
     */
    public void setAniReadout(boolean value) {
        setBp(PREF_ANIREADOUT, value);
    }


    /**
     * Get  the animation readout  flag
     * @return The flag value
     */
    public boolean getAniReadout() {
        return getBp(PREF_ANIREADOUT);
    }

    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
        updateNameLabel();
    }

    /**
     * Update the name label
     */
    protected void updateNameLabel() {
        if (nameLabel == null) {
            return;
        }
        //        nameLabel.setText("View:" + cnt);
        if (name != null) {
            nameLabel.setText(name);
        }
    }


    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     * Set the AspectRatio property.
     *
     * @param value The new value for AspectRatio
     */
    public void setAspectRatio(double[] value) {
        aspectRatio = value;
    }

    /** xxx */
    static int xxx = 0;

    /** my cnt */
    int xmycnt = xxx++;

    /**
     * Print the aspect
     *
     * @param msg  a prefix
     */
    public void printAspect(String msg) {
        if (aspectRatio == null) {
            System.err.println(xmycnt + " " + msg + " aspect is null");
        } else {
            System.err.println(xmycnt + " " + msg + " aspect: "
                               + aspectRatio[0] + " " + aspectRatio[1] + " "
                               + aspectRatio[2]);
        }
    }


    /**
     * Get the AspectRatio property.
     *
     * @return The AspectRatio
     */
    public double[] getAspectRatio() {
        return aspectRatio;
    }


    /**
     * Set the SkinProperties property.
     *
     * @param value The new value for SkinProperties
     */
    public void setSkinProperties(Hashtable value) {
        skinProperties = value;
    }

    /**
     * Get the SkinProperties property.
     *
     * @return The SkinProperties
     */
    public Hashtable getSkinProperties() {
        return skinProperties;
    }

    /**
     * Get the named property for the skin
     *
     * @param name  the name of the property
     *
     * @return the value
     */
    public String getSkinProperty(String name) {
        String prop = null;
        if (skinProperties != null) {
            prop = (String) skinProperties.get(name);
        }
        if (prop == null) {
            prop = (String) getStateManager().getProperty(name);
        }
        return prop;
    }

    /**
     * Set the LegendOnLeft property.
     *
     * @param value The new value for LegendOnLeft
     */
    public void setLegendOnLeft(boolean value) {
        legendOnLeft = value;
    }

    /**
     * Get the LegendOnLeft property.
     *
     * @return The LegendOnLeft
     */
    public boolean getLegendOnLeft() {
        return legendOnLeft;
    }

    /**
     * Set the IsShared property.
     *
     * @param value The new value for IsShared
     */
    public void setIsShared(boolean value) {
        isShared = value;
    }

    /**
     * Get the IsShared property.
     *
     * @return The IsShared
     */
    public boolean getIsShared() {
        return isShared;
    }

    /**
     * Set the display list font property.
     *
     * @param value The new value for displayListFont
     */
    public void setDisplayListFont(Font value) {
        displayListFont = value;
    }

    /**
     * Get the display list font property.
     *
     * @return The value for displayListFont
     */
    public Font getDisplayListFont() {
        if (displayListFont == null) {
            displayListFont = getStore().get(PREF_DISPLAYLISTFONT,
                                             defaultFont);
        }
        return displayListFont;
    }

    /**
     * Set the display list color property.
     *
     * @param value The new value for displayListColor
     */
    public void setDisplayListColor(Color value) {
        displayListColor = value;
    }

    /**
     * Get the display list color property.
     *
     * @return The value for displayListColor
     */
    public Color getDisplayListColor() {
        if (displayListColor == null) {
            displayListColor = getStore().get(PREF_DISPLAYLISTCOLOR,
                    (Color) null);
        }
        return displayListColor;
    }

    /**
     * Set the FullScreenWidth property.
     *
     * @param value The new value for FullScreenWidth
     */
    public void setFullScreenWidth(int value) {
        fullScreenWidth = value;
    }

    /**
     * Get the FullScreenWidth property.
     *
     * @return The FullScreenWidth
     */
    public int getFullScreenWidth() {
        return fullScreenWidth;
    }

    /**
     * Set the FullScreenHeight property.
     *
     * @param value The new value for FullScreenHeight
     */
    public void setFullScreenHeight(int value) {
        fullScreenHeight = value;
    }

    /**
     * Get the FullScreenHeight property.
     *
     * @return The FullScreenHeight
     */
    public int getFullScreenHeight() {
        return fullScreenHeight;
    }


    /**
     * Set the Properties property.
     *
     * @param value The new value for Properties
     */
    public void setProperties(Hashtable value) {
        properties = value;
    }

    /**
     * Get the Properties property.
     *
     * @return The Properties
     */
    public Hashtable getProperties() {
        return properties;
    }

    /**
     * get a property.
     *
     * @param key key
     *
     * @return property
     */
    public Object getProperty(Object key) {
        return properties.get(key);
    }

    /**
     * Set a property. Anything placed here is persisted off with the ViewManager
     *
     * @param key key
     * @param value value
     */
    public void putProperty(Object key, Object value) {
        properties.put(key, value);
    }

    /**
     * Get the display side coordinate system, subclasses should implement
     * if there is one.
     * @return  CoordinateSystem or null
     */
    public CoordinateSystem getDisplayCoordinateSystem() {
        return null;
    }

    /**
     *  Set the Lights property.
     *
     *  @param value The new value for Lights
     */
    public void setLights(List<LightInfo> value) {
        lights = value;
    }

    /**
     *  Get the Lights property.
     *
     *  @return The Lights
     */
    public List<LightInfo> getLights() {
        return lights;
    }


    /**
     *  Set the SplitPaneLocation property.
     *
     *  @param value The new value for SplitPaneLocation
     */
    public void setSplitPaneLocation(int value) {
        splitPaneLocation = value;
    }

    /**
     *  Get the SplitPaneLocation property.
     *
     *  @return The SplitPaneLocation
     */
    public int getSplitPaneLocation() {
        JSplitPane tmp = mainSplitPane;
        if (tmp != null) {
            return tmp.getDividerLocation();
        }
        return -1;
    }


    /**
     * Initialize the ViewState
     *
     * @param viewState  the view state
     */
    public void initViewState(ViewState viewState) {
        viewState.put(ViewState.PROP_MATRIX, getDisplayMatrix());
    }


    /**
     * Make a ViewState
     *
     * @return  the ViewState
     */
    public ViewState doMakeViewState() {
        ViewState viewState = new ViewState(getClass().getName());
        initViewState(viewState);
        return viewState;
    }



    /**
     * Set the initial ViewState name
     *
     * @param s  the name
     */
    public void setInitViewStateName(String s) {
        initViewStateName = s;
    }

    /**
     * Get the intial ViewState name
     *
     * @return  the intial ViewState name
     */
    public String getInitViewStateName() {
        return initViewStateName;
    }

    /**
     * Get the logo
     *
     * @return  the logo file or URL
     */
    public String getLogoFile() {
        if (logoFile == null) {
            logoFile =
                getIdv().getStateManager().getPreferenceOrProperty(PREF_LOGO,
                    "");
        }
        return logoFile;
    }

    /**
     * Set the logo file or URL
     *
     * @param logo the logo file or URL
     */
    public void setLogoFile(String logo) {
        logoFile = logo;
    }

    /**
     * Get the logo position on the screen
     *
     * @return the logo position on the screen
     */
    public String getLogoPosition() {
        if (logoPosition == null) {
            logoPosition = getIdv().getStateManager().getPreferenceOrProperty(
                PREF_LOGO_POSITION_OFFSET, "");
        }
        return logoPosition;
    }

    /**
     * Set the logo position on the screen
     *
     * @param logop the logo position on the screen
     */
    public void setLogoPosition(String logop) {
        logoPosition = logop;
    }

    /**
     * Get the logo scale
     *
     * @return the logo scale
     */
    public float getLogoScale() {
        if (logoScale < 0) {
            logoScale =
                (float) getIdv().getStateManager().getPreferenceOrProperty(
                    PREF_LOGO_SCALE, 1f);
        }
        return logoScale;
    }

    /**
     * Set the logo scale
     *
     * @param scale  the new scale
     */
    public void setLogoScale(float scale) {
        logoScale = scale;
    }

    /**
     * Set the logo visibility
     *
     * @param on  true to show
     */
    public void setLogoVisibility(boolean on) {
        setBp(PREF_LOGO_VISIBILITY, on);
    }

    /**
     * Get the logo visibility
     *
     * @return the logo visibility
     */
    public boolean getLogoVisibility() {
        return getBp(PREF_LOGO_VISIBILITY);
    }


    /**
     * Ensure that there is only one time driver for this view
     *
     * THERE CAN BE ONLY ONE! - Highlander
     *
     * @param displayControl  the time driver control
     */
    public void ensureOnlyOneTimeDriver(DisplayControl displayControl) {
        for (DisplayControl control : (List<DisplayControl>) getControls()) {
            if ( !control.equals(displayControl)
                    && control.getIsTimeDriver()) {
                ((DisplayControlImpl) control).setIsTimeDriver(false);
            }
        }

    }
}
