/*
 * Copyright 1997-2016 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.control;


import ucar.nc2.time.Calendar;

import ucar.unidata.collab.Sharable;
import ucar.unidata.collab.SharableImpl;
import ucar.unidata.data.DataCancelException;
import ucar.unidata.data.DataChangeListener;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataOperand;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataTimeRange;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.data.GeoSelectionPanel;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.DisplayInfo;
import ucar.unidata.idv.IdvConstants;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.NavigatedViewManager;
import ucar.unidata.idv.TransectViewManager;
import ucar.unidata.idv.ViewContext;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ui.DataSelectionWidget;
import ucar.unidata.idv.ui.DataSelector;
import ucar.unidata.idv.ui.DataTreeDialog;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.ui.DndImageButton;
import ucar.unidata.ui.FontSelector;
import ucar.unidata.ui.Help;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.PropertyValue;
import ucar.unidata.util.Prototypable;
import ucar.unidata.util.Range;
import ucar.unidata.util.Removable;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.GlobeDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.unidata.xml.XmlObjectStore;

import ucar.visad.UtcDate;
import ucar.visad.Util;
import ucar.visad.data.CalendarDateTime;
import ucar.visad.data.CalendarDateTimeSet;
import ucar.visad.display.Animation;
import ucar.visad.display.AnimationInfo;
import ucar.visad.display.AnimationWidget;
import ucar.visad.display.ColorScale;
import ucar.visad.display.ColorScaleInfo;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.Displayable;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.TextDisplayable;

import visad.CommonUnit;
import visad.ControlEvent;
import visad.ControlListener;
import visad.Data;
import visad.DateTime;
import visad.DisplayEvent;
import visad.DisplayListener;
import visad.DisplayRealType;
import visad.FieldImpl;
import visad.FunctionType;
import visad.GriddedSet;
import visad.LocalDisplay;
import visad.ProjectionControl;
import visad.Real;
import visad.RealTuple;
import visad.RealType;
import visad.Set;
import visad.SetType;
import visad.Text;
import visad.TextType;
import visad.Unit;
import visad.VisADException;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;
import visad.georef.MapProjection;

import visad.util.DataUtility;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;

import java.lang.reflect.Method;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.JTextComponent;


/**
 * This is the main base class for all DisplayControls.
 * @author IDV development team
 */
public abstract class DisplayControlImpl extends DisplayControlBase implements DisplayControl,
        ActionListener, ItemListener, DataChangeListener, HyperlinkListener,
        DisplayListener, PropertyChangeListener, ControlListener,
        Prototypable {

    /** current version */
    private static final double CURRENT_VERSION = 2.2;

    /** version */
    protected double version = 0;

    /** fudge factor for the Z position */
    protected static final double ZFUDGE = 0.005;

    /** all categories for this control */
    private static Hashtable allCategories = new Hashtable();


    /** Should we use the times in this display control as part of the animation set */
    private boolean useTimesInAnimation = true;

    /** should we do the cursor readout */
    private boolean doCursorReadout = true;

    /** Are we expanded in the main tabs */
    private boolean expandedInTabs = false;

    /** Do we show in the main tabs */
    private boolean showInTabs = true;

    /** version was set */
    private boolean versionWasSet = false;

    /** time labels */
    private Hashtable timeLabels;

    /** current time label */
    private DateTime currentTime;

    /** first time */
    private DateTime firstTime;

    /** listening for times flag */
    private boolean listeningForTimes = false;

    /** time label animation */
    private Animation viewAnimation;

    /** The animation held solely by this display control */
    private Animation internalAnimation;

    /** Animation info */
    private AnimationInfo animationInfo;

    /** The anim widget */
    private AnimationWidget animationWidget;

    /** component holder */
    private IdvComponentHolder componentHolder;

    /**
     * the String
     *
     *
     * Holds the set of attributeFlags (e.g., FLAG_CONTOUR, FLAG_COLORTABLE, ...)
     * that define what kinds of gui components are created for this control.
     */
    private int attributeFlags = 0;


    /**
     *  A flag to prevent a loop around event notification for when the Data is changed
     */
    private boolean inDataChangeCall = false;


    /** A flag to prevent loop back event firing */
    private boolean okToFireEvent = true;

    /**
     *  A flag to check if this control is in the middle of toggling its
     *  visibility checkboxes. Used to keep us from having an infinite
     *  event loop. Infinity is a pretty cool concept but it doesn't
     *  work well in practice.
     */
    protected boolean settingVisibility = false;


    /**
     * The name for the text type for the display list.
     */
    protected final String DISPLAY_LIST_NAME = "Display_List_Text";

    /**
     * Is this control active.  This is set when the control has
     * been removed.
     */
    private boolean hasBeenRemoved = false;


    /** Has this control's init method been called */
    private volatile boolean haveInitialized = false;


    /** Has the initialization been finished */
    private boolean initializationDone = false;


    /**
     *  Is this display locked for when the user toggles through the visibility.
     */
    private boolean lockVisibiltyToggle = false;

    /** Should we show ourselves in the ViewManager list of displays */
    private boolean showInDisplayList = true;

    /**
     *  Is this display control insulated from the removeAllDisplays call
     */
    private boolean canDoRemoveAll = true;


    /**
     *  Created by the doMakeBottomLegendComponent method, used for
     * displaying this  control in its ViewManager.
     */
    private JComponent bottomLegendComponent;

    /** The main JLabel used in the bottom legend */
    private JButton bottomLegendButton;

    /** Color to be used for the bottom legend foreground */
    private Color legendForeground;

    /** Color to be used for the bottom legend background */
    private Color legendBackground;

    /** lock buttons */
    private List lockButtons = new ArrayList();

    /**
     * Created by the doMakeSideLegendComponent method, used for
     * displaying this  control in its ViewManager.
     */
    private JComponent sideLegendComponent;

    /** The main JLabel used in the side legend */
    private JLabel sideLegendLabel;

    /** list of legend labels to update */
    private List labelsToUpdate = new ArrayList();

    /** This displays the extra legend labels for the side legend */
    private JTextArea legendTextArea;

    /** This holds the legendTextArea */
    private JPanel legendTextPanel;

    /** The line width widget */
    private ValueSliderWidget lww;

    /** The smoothing factor widget */
    private ValueSliderWidget sww;


    /**
     *  We maintain a list of the checkboxes use for toggling the visibility
     *  so that when the user toggles visibility in one we can run through
     *  the others, setting their state appropriately
     */
    private List visibilityCbs = new ArrayList();


    /** is raster property */
    private boolean isRaster = false;


    /**
     *  This is the main panel held in the window. We keep this around
     *  so we can add/remove the noteTextArea
     */
    private JPanel mainPanel;

    /**
     *  This is the GUI contents created with a call to doMakeContents.
     */
    private Container contents;


    /**
     *  This is the gui panel that is the full wrapper
     */
    private JComponent outerContents = null;


    /** This control's window. May be null. */
    private IdvWindow myWindow;

    /** details frame */
    private Window detailsFrame;

    /** details editor */
    private JEditorPane detailsEditor;

    /** Keep the listener around so we can remove it from the window */
    private WindowAdapter windowListener;


    /**
     * Tracks whether the window is visible or not.
     * We keep this information so when the control is persisted
     * and then unpersisted we can show the window accordingly.
     */
    private boolean myWindowVisible = false;


    /**
     * The X location of the window. Used  when this control has
     * been re-created through the persistence mechanism.
     */
    private int windowX = 50;

    /**
     * The Y location of the window. Used  when this control has
     * been re-created through the persistence mechanism.
     */
    private int windowY = 100;


    /**
     * The size of the window. Used  when this control has
     * been re-created through the persistence mechanism.
     */
    private Dimension windowSize;

    /** main panel size */
    private Dimension mainPanelSize;


    /**
     * A flag that determines whether this control should create its own
     * window. For example, if the control is embedded in an
     * {@link TextDisplayControl}
     * then this  flag is set to false.
     */
    private boolean makeWindow = true;


    /** flag for showing in the legend */
    private boolean showInLegend = true;


    /**
     * The Unit (may be null) that is used to display data.
     * We keep it at this level because this class handles
     * the widget that is used to change the unit, etc.
     */
    protected Unit displayUnit;

    /**
     * The Unit (may be null) that is used to display data
     * for color displays. We keep it at this level because
     * this class handles the widget that is used to change
     * the unit, etc.
     */
    private Unit colorUnit;


    /**
     *  This is the initial text note text. We have this here so when
     * this control is instantiated via persistence this value gets set.
     * We then use it  when we create the note TextArea
     */
    protected String initNoteText;


    /** The text area widget used  for the control text notes */
    protected JTextArea noteTextArea;

    /**
     *  The Container that holds the noteTextArea. We keep this around
     *  so we can add/remove the TextArea
     */
    protected JComponent noteWrapper;

    /** Do we show the note text? */
    protected boolean showNoteText = false;


    /** Keep around the side legend button panel */
    private JPanel sideLegendButtonPanel;

    /** Keep around the bottom legend button panel */
    private JPanel bottomLegendButtonPanel;

    /** The label to show the resolution readout in the side legend */
    protected String resolutionReadout = null;



    /**
     * The {@link ucar.unidata.data.DataSelection} that holds any
     * data subsetting specifications (e.g., time). This is typically
     * created by the IDV and passed into this DisplayControl.
     */
    protected DataSelection dataSelection;

    /**
     * A hashtable that is used to hold extra properties that
     * are passed to the {@link ucar.unidata.data.DataChoice} through
     * the getData called.
     */
    protected Hashtable requestProperties;


    /** Properties that other components  add into. Is not saved. */
    Hashtable transientProperties = new Hashtable();



    /** Set by the controls.xml and used in the legend guis to organize displays */
    private String displayCategory;


    /**
     * Contains a list of all {@link ucar.unidata.collab.Sharable}
     * objects (e.g., AnimationWidget) that are created by this control.
     * We keep this around so when the display is removed we can tell
     * each Sharable to remove itself.
     */
    protected List sharables;


    /**
     * Holds the {@link ucar.visad.display.DisplayMaster}-s that this control
     * wholly owns  (e.g., profile display master).
     */
    protected List displayMasters;


    /** This can be used to temporarily set a ViewManager that is to be used. TBD. */
    protected ViewManager defaultViewManager;


    /**
     *  Holds the {@link ucar.unidata.idv.ViewManager}-s that
     * this control has created.
     */
    protected List viewManagers;

    /** The name of the ViewManager that any displays should be added to */
    protected String defaultView = null;

    /** Defines what view managers this display can go in */
    String viewManagerClassNames = null;

    /**
     *  Used to hold contour information if this control is enabled for it.
     */
    protected ContourInfo contourInfo;

    /** Used by the isl to override selective parameters in the default contourInfo */
    protected String contourInfoParams;


    /**
     *  Used to hold the color table if this control is enabled for it.
     */
    private ColorTable colorTable;


    /**
     * This holds the name of the color table for displays  that
     * are created from old bundles (i.e., pre color table persistence)
     */
    private String colorTableName = null;



    /**
     *  Used to hold the color if this control is enabled for it
     */
    private Color color;


    /**
     *  Used to hold the color of the display list displayable
     */
    private Color displayListColor;

    /**
     * A boolean to see if the display list got it's color from the display.
     */
    protected boolean displayListUsesColor = false;

    /**
     * The color widget
     */
    private JComboBox colorComboBox;

    /**
     * Used to hold the numeric range if this control is enabled
     * for color tables.
     */
    private Range colorRange;

    /**
     * Used to hold the numeric range if this control is enabled
     * for select range.
     */
    private Range selectRange;

    /** flag for select range enabled */
    private boolean selectRangeEnabled = false;

    /** The z position for displays with the FLAG_ZPOSITION set */
    private double zPosition = Double.MIN_VALUE;

    /** The line width for displays with the FLAG_LINEWIDTH set */
    private int lineWidth = 1;

    /**
     *  This is the color table gui widget for controls enabled for color tables
     */
    protected ColorTableWidget ctw;

    /**
     *  This is the color scale widget for a display
     */
    protected List colorScales;

    /**
     *  This is the table of the view manager display list displayables
     */
    protected Hashtable displayListTable = new Hashtable();

    /**
     *  This is the contour info gui widget for controls enabled for contours
     */
    protected ContourWidget contourWidget;

    /**
     *  This is the contour info gui widget for controls enabled for contours
     */
    protected SelectRangeWidget selectRangeWidget;


    /**
     * Holds the list of JComponents that are used to show the current color
     * of this control. TYpically there is one for the bottom legend and one for
     * the side legend.
     */
    private List colorSwatches = new ArrayList();



    /** The current sampling mode used by this control */
    private String defaultSamplingMode = DEFAULT_SAMPLING_MODE;


    /** The name of this control. Typically set through controls.xml */
    private String displayName = "Display";


    /** The url to show help for this control. Typically set through controls.xml */
    protected String helpUrl = null;


    /**
     * The name of the parameter (if there is one)
     */
    protected String paramName;

    /** The template for the display list label */
    private String displayListTemplate;

    /** The template for the legend label */
    private String legendLabelTemplate;

    /** extra label template */
    private String extraLabelTemplate = null;

    /** property widget */
    private JTextField legendLabelTemplateFld;

    /** property widget */
    private JTextField displayListTemplateFld;


    /** extra label template entry field */
    private JTextArea extraLabelTemplateFld;

    /** The name defined by the user */
    private String id;

    /**
     * The list of {@link ucar.unidata.data.DataChoice}s   currently
     * used by this display.
     */
    private List myDataChoices;


    /**
     * The list of {@link ucar.unidata.data.DataChoice}s
     * that this display was initialized with.
     */
    private List initDataChoices;


    /**
     * This is used during persistence/unpersistence to track
     * whether this display originally had data choices
     */
    private boolean hadDataChoices = false;

    /** This is the name of the data choice we originally had before we were saved off without data */
    private String originalDataChoicesLabel = "";


    /** Keeps around the name of the display template if we were created from one */
    private String templateName;


    /**
     * This gets set to track when we have been re-instantiated from a
     * bundle without any data.
     */
    protected boolean instantiatedWithNoData = false;


    /**
     *  The DataChoice that this control was intialized with.
     *  Really, once we have controls that make use of the list
     *  of DataChoice-s we are going to have to rethink some of this code.
     */
    //    private DataChoice xxxdataChoice;

    /**
     *  DataInstance serves as a wrapper around a DataChoice and provides
     *  various common utilities.
     */
    private List dataInstances = new ArrayList();


    /**
     *  This is the display id  defined for this DisplayControl
     *  by the idv/controls.properties and ControlDescriptor code.
     *  We keep it around for when the control is persisted.
     */
    private String displayId;


    /**
     *  This is the list of DataCategory-s that were defined for this DisplayControl
     *  by the idv/controls.properties and ControlDescriptor code.
     */
    protected List categories;


    /**
     *  Tracks whether this control's Displayable-s are visible or not.
     */
    private boolean isVisible = true;

    /**
     *  Holds the color scale information
     */
    protected ColorScaleInfo colorScaleInfo = null;



    /**
     *  This is the context in whcih this display control exists (typically
     *  an instance of the IntegratedDataViewer but we use the ControlContext
     *  interface to keep us honest and not sloppy).
     */
    protected ControlContext controlContext;



    /**
     *  This is a list of DisplayInfo objects, one for each
     *  Displayable create by this control.
     */
    private List displays = new ArrayList();



    /**
     *  Holds the collection of all {@link ucar.visad.display.Displayable}s
     *  that have atttributes set by the gui components.
     */
    List displayables;


    /**
     * Flag for collapsed legend.
     */
    private boolean collapseLegend = false;




    /** locking object */
    private Object MUTEX_CONTROLCHANGE = new Object();

    /** locking object */
    private Object LEGEND_MUTEX = new Object();

    /**  */
    private boolean controlChangePending = false;

    /** last check load time */
    private long lastCheckControlChangeTime;

    /** last load event time */
    private long lastControlChangeTime;

    /** Keep track of the last bounds we saw on a control changed event */
    private Rectangle2D lastBounds;

    /** list of propertyChangeListeners */
    private List propertyChangeListeners;

    /** Should the displayable's renderers call adjust seam */
    private boolean useFastRendering = false;

    /** A flag to note whether this object was unpersisted */
    private boolean wasUnPersisted = false;



    /** is this a time driver? */
    private boolean isTimeDriver = false;

    /** does this use the time driver? */
    private boolean usesTimeDriver = false;

    /** color dimness flag */
    private float colorDimness = 1.0f;

    /** property widget */
    private JTextField categoryFld;



    /** property widget */
    private JTextField idFld;



    /** geoselection panel */
    private GeoSelectionPanel geoSelectionPanel;

    /** data selection widget */
    private DataSelectionWidget dataSelectionWidget;

    /** The color scale dialog used in the properties dialog */
    private ColorScaleDialog csd;

    /** The date time range */
    private DataTimeRange dataTimeRange;

    /** hashtable for methods to settings */
    private Hashtable methodNameToSettingsMap = new Hashtable();

    /** slider for setting skip values */
    protected JSlider skipSlider;

    /** z position slider */
    private ZSlider zPositionSlider;

    /** the skip value */
    private int skipValue = 0;

    /** initial settings */
    private List initialSettings;

    /** Data selection components from the data source for the properties dialog */
    //private List<DataSelectionComponent> dataSelectionComponents;

    /**
     * the texture quality  (1= best, 10= moderate)
     */
    private int textureQuality = 10;

    /**
     * quality slider
     */
    private JSlider textureSlider = null;


    /** point size */
    private float pointSize = 1.0f;


    /** the projection control we're listening to */
    private ProjectionControl projectionControlListeningTo;

    /** the local display we are listening to */
    private LocalDisplay displayControlListeningTo;


    /** list of removeable things */
    private List<Removable> removables = new ArrayList<Removable>();


    /** visibility animation pause (secs) */
    private int visbilityAnimationPause = -1;

    /** visibility animation pause field for properties gui */
    private JTextField visbilityAnimationPauseFld;

    /** labels for smoothing functions */
    private final static String[] smootherLabels = new String[] {
        LABEL_NONE, "5 point", "9 point", "Gaussian Weighted",
        "Cressman Weighted", "Circular Aperture", "Rectangular Aperture"
    };

    /** types of smoothing functions */
    private final static String[] smoothers = new String[] {
        LABEL_NONE, GridUtil.SMOOTH_5POINT, GridUtil.SMOOTH_9POINT,
        GridUtil.SMOOTH_GAUSSIAN, GridUtil.SMOOTH_CRESSMAN,
        GridUtil.SMOOTH_CIRCULAR, GridUtil.SMOOTH_RECTANGULAR
    };

    /** smoothing factor for Gaussian smoother */
    private int smoothingFactor = 6;

    /** default type */
    private String smoothingType = LABEL_NONE;

    /**
     * Flag for progressive resolution.
     */
    public boolean isProgressiveResolution = false;

    /**
     * Flag for reloading from bounds
     */
    public boolean reloadFromBounds = false;

    /**
     * Flag for matching the display region
     */
    public boolean matchDisplayRegion = false;

    /**
     * Default constructor. This is called when the control is
     * unpersisted through the {@link ucar.unidata.xml.XmlEncoder}
     * mechanism.
     */
    public DisplayControlImpl() {
        setSharing(false);
        setShareGroup("DisplayControl");
    }

    /**
     * Create a DisplayControlImple from a ControlContext
     *
     * @param controlContext  context to use
     */
    public DisplayControlImpl(ControlContext controlContext) {
        this();
        this.controlContext = controlContext;
    }


    /**
     * This init method is used to just initialize some basic properties
     * of this display control. It is intended that this control has not
     * been created for normal display purposes but has been created for other
     * purposes (e.g., Testing valid help ids).
     *
     *
     * @param displayId The identifier of this control. Taken from controls.xml
     * @param categories The list of {@link ucar.unidata.data.DataCategory}ies for this control
     * @param properties Any properties (usually defined in controls.xml)
     *
     */
    public final void initBasic(String displayId, List categories,
                                Hashtable properties) {
        this.displayId  = displayId;
        this.categories = categories;
        if (properties != null) {
            applyProperties(properties);
        }
    }



    /**
     *  This init method is the one actually called by the IDV.
     *  The default is to turn around and call init with the first
     *  element in the dataChoice array.
     *
     * @param displayId The identifier of this control. Taken from controls.xml
     * @param categories The list of {@link ucar.unidata.data.DataCategory}ies for this control
     * @param choices  The list of {@link DataChoice}-s (usually only one) for this control
     * @param controlContext The context in which this control is in (usually a reference to the
     *                      {@link ucar.unidata.idv.IntegratedDataViewer}
     * @param properties Any properties (usually defined in controls.xml)
     * @param dataSelection Holds any specifications of subsets of the data (e.g., times)
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     * @deprecated use init that takes a properties Hashtable
     */
    public final void init(String displayId, List categories, List choices,
                           ControlContext controlContext, String properties,
                           DataSelection dataSelection)
            throws VisADException, RemoteException {
        init(displayId, categories, choices, controlContext,
             StringUtil.parsePropertiesString(properties), dataSelection);
    }


    /**
     *  This init method is the one actually called by the IDV.
     *  The default is to turn around and call init with the first
     *  element in the dataChoice array.
     *
     * @param displayId The identifier of this control. Taken from controls.xml
     * @param categories The list of {@link ucar.unidata.data.DataCategory}ies for this control
     * @param choices  The list of {@link DataChoice}-s (usually only one) for this control
     * @param controlContext The context in which this control is in (usually a reference to the
     *                      {@link ucar.unidata.idv.IntegratedDataViewer}
     * @param properties Any properties (usually defined in controls.xml)
     * @param dataSelection Holds any specifications of subsets of the data (e.g., times)
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    public final void init(String displayId, List categories, List choices,
                           ControlContext controlContext,
                           Hashtable properties, DataSelection dataSelection)
            throws VisADException, RemoteException {

        if (haveInitialized) {
            return;
        }


        initSharable();
        this.displayId      = displayId;
        this.categories     = categories;
        this.controlContext = controlContext;
        this.dataSelection  = dataSelection;
        if (this.dataSelection == null) {
            this.dataSelection = new DataSelection();
        }
        setMatchDisplayRegion(
            this.dataSelection.getGeoSelection(true).getUseViewBounds());

        //Initialize the adjust flags if we have not been unpersisted
        if ( !wasUnPersisted) {
            useFastRendering = getInitialFastRendering();
        }

        //Set the myDataChoices member and add this object as a DataChangeListener
        setDataChoices(choices);
        this.usesTimeDriver =
            this.dataSelection.getProperty(DataSelection.PROP_USESTIMEDRIVER,
                                           false);
        if ( !this.isTimeDriver) {
            this.isTimeDriver = this.dataSelection.getProperty(
                DataSelection.PROP_ASTIMEDRIVER, false);
            if (this.isTimeDriver) {  // make sure only one driver per view manager
                ViewManager vm = getViewManager();
                vm.ensureOnlyOneTimeDriver(this);
            }
        }


        if (properties != null) {
            applyProperties(properties);
        }

        if (checkFlag(FLAG_COLOR) && (color == null)) {
            color = getDisplayConventions().getColor();
        }

        // set the sampling mode
        defaultSamplingMode = getObjectStore().get(PREF_SAMPLING_MODE,
                DEFAULT_SAMPLING_MODE);

        //Check if we have been removed
        if (hasBeenRemoved) {
            return;
        }

        if (initialSettings != null) {
            try {
                for (int i = 0; i < initialSettings.size(); i++) {
                    applyDisplaySetting(
                        (DisplaySetting) initialSettings.get(i));
                }
                initialSettings = null;
            } catch (Exception exc) {
                logException("Initializing with settings", exc);
            }
        }



        // we do this here because time driver properties 
        // might be changed by the display settings.
        updateDataSelection(this.dataSelection);


        //Call the derived class init method.
        //        System.err.println("CALLING INIT " + mycnt);


        if ( !init(myDataChoices)) {
            displayControlFailed();
            if (getProperty("control.ignoreerrors", false)) {
                addToControlContext();
            }
            return;
        }

        if (myDataChoices != null) {
            if ((myDataChoices.size() > 0)
                    && (myDataChoices.get(0) instanceof DerivedDataChoice)) {
                List cdcs =
                    ((DerivedDataChoice) myDataChoices.get(0)).getChoices();
                if (cdcs.size() > 0) {
                    DataChoice    dc = (DataChoice) cdcs.get(0);
                    DataSelection ds = dc.getDataSelection();
                    if (ds != null) {
                        List dtimes = ds.getTimeDriverTimes();
                        this.usesTimeDriver =
                            ds.getProperty(DataSelection.PROP_USESTIMEDRIVER,
                                           false);
                        setMatchDisplayRegion(
                            ds.getGeoSelection(true).getUseViewBounds());
                    }
                }
            }
        }

        //Check if we have been removed
        if (hasBeenRemoved) {
            return;
        }


        if (getIdv().getInteractiveMode()) {
            Trace.call1("DisplayControlImpl.init doMakeWindow");
            //Now create the gui
            doMakeWindow();
            Trace.call2("DisplayControlImpl.init doMakeWindow");
        }


        addToControlContext();



        //Get the color table, range, etc.
        instantiateAttributes();



        haveInitialized = true;

        if ( !isVisible) {
            setDisplayVisibility(false);
        }



        applyAttributesToDisplayables();



        //Check if we have been removed
        if (hasBeenRemoved) {
            return;
        }

        Trace.call1("DisplayControlImpl.init insertDisplayables");
        //Add the Displayables to their ViewManagers
        if ( !insertDisplayables()) {
            return;
        }

        Trace.call2("DisplayControlImpl.init insertDisplayables");



        //Check if we have been removed
        if (hasBeenRemoved) {
            return;
        }

        //Now create the actual window
        if (makeWindow) {
            myWindow = createIdvWindow();
            if (myWindow != null) {
                initWindow(myWindow);
            }
        }




        if (shouldAddDisplayListener()) {
            NavigatedDisplay navDisplay = getNavigatedDisplay();
            if ((navDisplay != null) && (displayControlListeningTo != null)) {
                displayControlListeningTo = navDisplay.getDisplay();
                displayControlListeningTo.addDisplayListener(this);
            }
        }



        if (shouldAddControlListener()) {
            NavigatedDisplay navDisplay = getNavigatedDisplay();
            if (navDisplay != null) {
                try {
                    //Calculate the bounds (if we can) so we don't fire an extra viewpointChanged event later
                    lastBounds = calculateRectangle();
                } catch (Exception exc) {
                    //noop here 
                }
                projectionControlListeningTo =
                    navDisplay.getDisplay().getProjectionControl();
                projectionControlListeningTo.addControlListener(this);
                if (displayControlListeningTo == null) {
                    displayControlListeningTo = navDisplay.getDisplay();
                    displayControlListeningTo.addDisplayListener(this);
                }
            }
        }


        //Force the creation of the animation widget
        if (shouldAddAnimationListener()) {
            getSomeAnimation();
        }

        initDone();


        if (componentHolder != null) {
            componentHolder.displayControlHasInitialized();
        }


        initializationDone = true;
        if (animationWidget != null) {
            animationWidget.setSharing(animationInfo.getShared());
        }

        doInitialUpdateLegendAndList();


    }


    /**
     * Do the initial update legend and list
     */
    protected void doInitialUpdateLegendAndList() {
        updateLegendAndList();
    }

    /**
     * Add to the control context
     */
    protected void addToControlContext() {
        //Add this control to the main controlContext
        controlContext.addDisplayControl(this);
    }

    /**
     * Initialize as a template
     */
    public void initAsTemplate() {
        defaultView = null;
    }

    /**
     * Initialize as a prototype
     */
    public void initAsPrototype() {
        //TODO: What all should we clear here?
        defaultView    = null;
        colorTable     = null;
        colorTableName = null;
        displayUnit    = null;
    }


    /**
     * Called by the {@link ucar.unidata.idv.IntegratedDataViewer} to
     * initialize after this control has been unpersisted
     *
     * @param vc The context in which this control exists
     * @param properties Properties that may hold things
     */
    public void initAfterUnPersistence(ControlContext vc,
                                       Hashtable properties) {

        initAfterUnPersistence(vc, properties, null);
    }

    /**
     * Called by the {@link ucar.unidata.idv.IntegratedDataViewer} to
     * initialize after this control has been unpersisted
     *
     * @param vc The context in which this control exists
     * @param properties Properties that may hold things
     * @param preSelectedDataChoices set of preselected data choices
     */
    public void initAfterUnPersistence(ControlContext vc,
                                       Hashtable properties,
                                       List preSelectedDataChoices) {

        //If version was not set then this came from a really old bundle
        if ( !versionWasSet) {
            //Don't embed old display control windows in the tabs
            showInTabs = !myWindowVisible;
        }

        wasUnPersisted = true;
        if (haveInitialized) {
            return;
        }
        try {
            initSharable();
            controlContext = vc;
            List dataSources = getControlContext().getAllDataSources();
            if ((initDataChoices == null) && hadDataChoices) {
                if (preSelectedDataChoices != null) {
                    initDataChoices = preSelectedDataChoices;
                } else {
                    if ((originalDataChoicesLabel != null)
                            && getObjectStore().get(
                                IdvConstants.PREF_AUTOSELECTDATA, false)) {
                        //                    System.err.println("Looking for:"
                        //                                       + originalDataChoicesLabel);
                        for (int i = 0; i < dataSources.size(); i++) {
                            DataSource dataSource =
                                (DataSource) dataSources.get(i);
                            DataChoice dataChoice =
                                dataSource.findDataChoice(
                                    originalDataChoicesLabel);
                            if (dataChoice != null) {
                                initDataChoices = Misc.newList(dataChoice);
                                //                            System.err.println("GOT:" + dataChoice);
                                break;
                            }
                        }
                    }
                }

                if (initDataChoices == null) {
                    String label = "<html>" + "Please select data for: <i>"
                                   + getDisplayName() + "</i>";
                    if ((originalDataChoicesLabel != null)
                            && (originalDataChoicesLabel.length() > 0)) {
                        label = label + "<br>Original field: <i>"
                                + originalDataChoicesLabel + "</i>";
                    }

                    DataOperand operand =
                        new DataOperand(originalDataChoicesLabel, label,
                                        categories, false);
                    DataTreeDialog dataDialog = new DataTreeDialog(getIdv(),
                                                    null,
                                                    Misc.newList(operand),
                                                    dataSources,
                                                    myDataChoices);
                    List choices = dataDialog.getSelected();
                    if ((choices != null) && (choices.size() > 0)) {
                        initDataChoices = (List) choices.get(0);
                    }
                }
                if (initDataChoices == null) {
                    displayControlFailed();
                    return;
                }
                instantiatedWithNoData = true;
                initializeWithNewData();
            }

            if (initDataChoices != null) {
                for (int i = 0; i < initDataChoices.size(); i++) {
                    ((DataChoice) initDataChoices.get(
                        i)).initAfterUnPersistence(properties);
                }
            }
            // for bundles before 2.1, nameFromUser was used for display label
            if ((displayListTemplate == null)
                    && (legendLabelTemplate != null)) {
                displayListTemplate = legendLabelTemplate + " "
                                      + UtcDate.MACRO_TIMESTAMP;
            }

            init(getDisplayId(), getCategories(), initDataChoices,
                 getControlContext(), properties, getDataSelection());
            initDataChoices = null;
        } catch (ucar.unidata.data.DataCancelException dce) {
            displayControlFailed();
        } catch (Exception exc) {
            logException("Initializing after unpersistence", exc);
        }
    }




    /**
     * Initialize this instance according to the first
     * {@link ucar.unidata.data.DataChoice} in a {@link java.util.List}.
     * This implementation invokes {@link #init(DataChoice)} to
     * perform the initialization.  If the list is <code>null</code> or empty,
     * then the argument to {@link #init(DataChoice)} is <code>null</code>.
     *
     * @param choices          A list of data choices or <code>null</code>.
     * @return                 <code>true</code> if and only if this instance
     *                         was correctly initialized by the data choice.
     * @throws VisADException  if a VisAD Failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public boolean init(List choices) throws VisADException, RemoteException {
        if ((choices != null) && (choices.size() > 0)) {
            //xxxdataChoice
            return init((DataChoice) choices.get(0));
        } else {
            return init((DataChoice) null);
        }
    }


    /**
     * <p>Initialize this instance according to a data choice. Subclasses should
     * override this method.  Overridding methods should probably invoke {@link
     * #setData(DataChoice)} as part of their initialization procedure --
     * although it is unclear when this is actually necessary.</p>
     *
     * <p>This implementation simply returns <code>true</code>.</p>
     *
     * @param choice           A data choice or <code>null</code>.
     * @return                 <code>true</code> if and only if this instance
     *                         was correctly initialized by the data choice.
     * @throws VisADException  if a VisAD Failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public boolean init(DataChoice choice)
            throws VisADException, RemoteException {
        return true;
    }


    /**
     *  Called after all initialization has been done. A hook
     * that allows derived classes to do any further initialization.
     */
    public void initDone() {}




    /**
     * Add a property change listener.
     *
     * @param listener the listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeListeners == null) {
            propertyChangeListeners = new ArrayList();
        }
        propertyChangeListeners.add(listener);
    }


    /**
     * Remove property change listener.
     *
     * @param listener   listener to remove
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        if (propertyChangeListeners == null) {
            return;
        }
        propertyChangeListeners.remove(listener);
    }

    /**
     * Fire a property change event.
     *
     * @param event  event to propagate
     */
    protected void firePropertyChangeEvent(PropertyChangeEvent event) {
        if (propertyChangeListeners == null) {
            return;
        }
        for (int i = 0; i < propertyChangeListeners.size(); i++) {
            PropertyChangeListener listener =
                (PropertyChangeListener) propertyChangeListeners.get(i);
            listener.propertyChange(event);
        }
    }




    /**
     * Does this have a time macro string?
     *
     * @param t  the string to check
     *
     * @return true if there is a time macro
     */
    protected boolean hasTimeMacro(String t) {
        if (t == null) {
            return false;
        }
        return UtcDate.containsTimeMacro(t) || hasForecastHourMacro(t);

    }

    /**
     * A hook to allow derived classes to tell us to add this
     * as an animation listener
     *
     * @return Add as animation listener
     */
    protected boolean shouldAddAnimationListener() {
        return hasTimeMacro(getLegendLabelTemplate())
               || hasTimeMacro(getExtraLabelTemplate())
               || hasTimeMacro(getDisplayListTemplate());
    }






    /**
     * A hook to allow derived classes to tell us to add this
     * as a display listener
     *
     * @return Add as display listener
     */
    protected boolean shouldAddDisplayListener() {
        return false;
    }

    /**
     * A hook to allow derived classes to tell us to add this
     * as a control listener
     *
     * @return Add as control listener
     */

    protected boolean shouldAddControlListener() {
        return false;
    }

    /**
     * Has this control been initialized
     *
     * @return Is this control initialized
     */
    public boolean getHaveInitialized() {
        return haveInitialized;
    }

    /**
     * Get the graphics configuration
     *
     * @param is3D   use Java 3D
     * @param useStereo  use stereo (id3D must be true)
     *
     * @return  the GraphicsConfiguration for DisplayImpl's
     */
    protected GraphicsConfiguration getGraphicsConfiguration(boolean is3D,
            boolean useStereo) {
        Point p = null;
        if ( !getShowInTabs() && (myWindow != null)) {
            p = myWindow.getLocation();
        } else {
            //            p = getIdv().getIdvUIManager().getDashboardLocation();
        }
        GraphicsDevice d = getIdv().getIdvUIManager().getScreen(p);

        return ucar.visad.display.DisplayUtil.getPreferredConfig(d, is3D,
                useStereo);
    }

    /**
     * Used to apply all of the display attributes taht are active
     * to the {@link ucar.visad.display.Displayable}-s
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void applyAttributesToDisplayables()
            throws VisADException, RemoteException {
        //        Trace.call1("DisplayControlImpl.applyAttributes");
        deactivateDisplays();

        //        Trace.msg("DisplayControlImpl.applyColor");
        applyColor();
        applyDisplayUnit();
        if ( !isDisplayUnitAlsoColorUnit()) {
            applyColorUnit();
        }
        applyContourInfo();
        applyRange();
        applySelectRange();
        applyColorTable();
        //        Trace.msg("DisplayControlImpl.applyZ");
        applyZPosition();
        applyUseFastRendering();
        applyLineWidth();
        applySkipFactor();
        applyTextureQuality();
        applySmoothing();
        activateDisplays();
        //        Trace.call2("DisplayControlImpl.applyAttributes");
    }



    /**
     * Add the given attribute flag (e.g., FLAG_COLORTABLE)
     * to the attrbiute  flag map.
     *
     * @param f The flag (e.g., FLAG_COLORTABLE)
     */
    protected void addAttributeFlags(int f) {
        attributeFlags = attributeFlags | f;
    }


    /**
     * Set the attribute flag map to the given value. Note: this really should just set the flags
     * but instead it OR's the attributeFlags with the given set of flags in the f parameter.
     * Lets keep this logic because changing it now breaks lotsof things.
     * If you want to actually set the flags call reallySetAttributeFlags
     *
     * @param f The value of the attribute flag map
     */
    protected void setAttributeFlags(int f) {
        attributeFlags = attributeFlags | f;
    }


    /**
     * Set the attribute flag map to the given value.
     *
     * @param f THe value of the attribute flag map
     */
    protected void reallySetAttributeFlags(int f) {
        attributeFlags = f;
    }

    /**
     * Test if the given flag is set in the attrbiuteFlags
     *
     * @param f The flag to check
     * @return Is the given flag set
     */
    protected boolean checkFlag(int f) {
        return ((attributeFlags & f) != 0);
    }

    /**
     * Return the attribute flag map
     *
     * @return The attributeFlags
     */
    protected int getAttributeFlags() {
        return attributeFlags;
    }

    /**
     * Add the given {@link ucar.visad.display.Displayable} to the list of attribute
     * displayables.  Use this object's attributeFlags  data member as the attribute
     * attributeFlags to use.
     *
     * @param d The {@link ucar.visad.display.Displayable} to add
     */
    protected void addAttributedDisplayable(Displayable d) {
        addAttributedDisplayable(d, attributeFlags);
    }

    /**
     * Add the given {@link ucar.visad.display.Displayable} to the list
     * of displayables. This is simply a
     * wrapper that passes in notGlobalFlags="0"
     *
     * @param d The {@link ucar.visad.display.Displayable} to add
     * @param attributeFlags The set of attributes for this displayable
     */
    protected void addAttributedDisplayable(Displayable d,
                                            int attributeFlags) {
        addAttributedDisplayable(d, attributeFlags, 0);
    }


    /**
     *  Add the given Displayable into the list of attributed displayables
     *  managed by this DisplayControl. An "attributed displayable" is one which
     *  there is one (or more) gui widgets created for defining the graphic
     *  attributes of the displayable (e.g., color table, param range, color, etc)
     *  defined by the bitmask attributeFlags.
     *  The notGlobalFlags are used to define the attribute for this displayable but not
     *  used for creating the gui, etc.
     *
     * @param d The {@link ucar.visad.display.Displayable} to add
     * @param attributeFlags The set of attributes for this displayable
     * @param notGlobalFlags Attribute flags for this displayable but don't let
     *        them effect the gui.
     */
    protected void addAttributedDisplayable(Displayable d,
                                            int attributeFlags,
                                            int notGlobalFlags) {
        if (displayables == null) {
            displayables = new ArrayList();
        }
        displayables.add(new FlaggedDisplayable(d,
                attributeFlags | notGlobalFlags));
        setAttributeFlags(attributeFlags);
        if (getHaveInitialized()) {
            try {
                //                Trace.call1(
                //                    "DisplayControlImpl:applyAttributesToDisplayables");
                applyAttributesToDisplayables();
                //                Trace.call2(
                //                    "DisplayControlImpl:applyAttributesToDisplayables");
            } catch (Exception exc) {
                logException(
                    "Applying graphics attributes to displayables for display:"
                    + toString(), exc);
            }
        }
    }

    /**
     * If the contourInfo is non-null then apply it to the
     * {@link ucar.visad.display.Displayable}s in the displayables
     * list that are flagged with the FLAG_CONTOUR
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void applyContourInfo() throws VisADException, RemoteException {
        ContourInfo contourInfo = getContourInfo();
        if ((contourInfo == null) || (displayables == null)) {
            return;
        }
        for (int i = 0, n = displayables.size(); i < n; i++) {
            FlaggedDisplayable fd = (FlaggedDisplayable) displayables.get(i);
            if ( !fd.ok(FLAG_CONTOUR)) {
                continue;
            }
            fd.displayable.setContourInfo(contourInfo);
        }
    }

    /**
     * If the colorScaleInfo is non-null then apply it to the
     * {@link ucar.visad.display.Displayable}s in the displayables
     * list that are flagged with the FLAG_COLORTABLE
     *
     * @throws RemoteException  problem with remote display
     * @throws VisADException   problem with local display
     */
    protected void applyColorScaleInfo()
            throws VisADException, RemoteException {
        if (colorScaleInfo == null) {
            return;
        }
        ColorScaleInfo tmpColorScaleInfo = new ColorScaleInfo(colorScaleInfo);
        tmpColorScaleInfo.setIsVisible(tmpColorScaleInfo.getIsVisible()
                                       && getDisplayVisibility());
        if (colorScales == null) {
            if (tmpColorScaleInfo.getIsVisible() && getHaveInitialized()) {
                doMakeColorScales();
            }
            return;
        }
        for (int i = 0, n = colorScales.size(); i < n; i++) {
            ColorScale scale = (ColorScale) colorScales.get(i);
            scale.setColorScaleInfo(tmpColorScaleInfo);
        }
    }


    /**
     * A hook that is called when the color unit is changed. Allows
     * derived classes to act accordingly.
     *
     * @param oldUnit The old color unit
     * @param newUnit The new color unit
     */
    protected void colorUnitChanged(Unit oldUnit, Unit newUnit) {}



    /**
     * If the color unit (gotten from a call to getUnitForColor)
     * is non-null then apply it to the
     * {@link ucar.visad.display.Displayable}s in the displayables
     * list that are flagged with the FLAG_COLORUNIT
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void applyColorUnit() throws VisADException, RemoteException {
        Unit unitForColor = getUnitForColor();
        if ((unitForColor == null) || (displayables == null)) {
            return;
        }
        for (int i = 0, n = displayables.size(); i < n; i++) {
            FlaggedDisplayable fd = (FlaggedDisplayable) displayables.get(i);
            if ( !fd.ok(FLAG_COLORUNIT)) {
                continue;
            }
            try {
                fd.displayable.setColorUnit(unitForColor);
            } catch (Exception excp) {}  // bad unit
        }
    }


    /**
     * A hook that is called when the display unit is changed. Allows
     * derived classes to act accordingly.
     *
     * @param oldUnit The old color unit
     * @param newUnit The new color unit
     */
    protected void displayUnitChanged(Unit oldUnit, Unit newUnit) {}


    /**
     * Get the raw data unit.
     * @return  null
     */
    public Unit getRawDataUnit() {
        return null;
    }

    /**
     * If the color unit (gotten from a call to getUnitForColor)
     * is non-null then apply it to the
     * {@link ucar.visad.display.Displayable}s in the displayables
     * list that are flagged with the FLAG_DISPLAYUNIT
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void applyDisplayUnit() throws VisADException, RemoteException {
        applyDisplayUnit(true);
    }



    /**
     * If the color unit (gotten from a call to getUnitForColor)
     * is non-null then apply it to the
     * {@link ucar.visad.display.Displayable}s in the displayables
     * list that are flagged with the FLAG_DISPLAYUNIT
     *
     * @param firstTime Is this the first time this method has been called
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    private void applyDisplayUnit(boolean firstTime)
            throws VisADException, RemoteException {

        if ((displayUnit == null) || (displayables == null)) {
            return;
        }
        boolean unitIsBad = false;
        for (int i = 0, n = displayables.size(); i < n; i++) {
            FlaggedDisplayable fd = (FlaggedDisplayable) displayables.get(i);
            if ( !fd.ok(FLAG_DISPLAYUNIT)) {
                continue;
            }
            try {
                fd.displayable.setDisplayUnit(displayUnit);
            } catch (Exception excp) {
                unitIsBad = true;
                break;
            }
        }
        if (unitIsBad) {
            displayUnit = null;
            if (firstTime) {
                displayUnit = getDisplayUnit(getRawDataUnit());
                applyDisplayUnit(false);
            }
        }
    }


    /**
     * Get the color table to use when applying to displayables
     *
     * @return The color table
     */
    protected ColorTable getColorTableToApply() {
        if (colorTable == null) {
            colorTable = getOldColorTableOrInitialColorTable();
        }
        return colorTable;
    }

    /**
     * If the color table is non-null then apply it to the
     * {@link ucar.visad.display.Displayable}s in the displayables
     * list that are flagged with the FLAG_COLORTABLE
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void applyColorTable() throws VisADException, RemoteException {
        if (displayables == null) {
            return;
        }
        ColorTable colorTableToUse = getColorTableToApply();
        if (colorTableToUse == null) {
            if (colorScaleInfo != null) {
                colorScaleInfo.setColorPalette(null);
                applyColorScaleInfo();
            }
            return;
        }
        float[][] table = null;
        for (int i = 0, n = displayables.size(); i < n; i++) {
            FlaggedDisplayable fd = (FlaggedDisplayable) displayables.get(i);
            if ( !fd.ok(FLAG_COLORTABLE)) {
                continue;
            }

            //Only create this once
            if (table == null) {
                table = getColorTableForDisplayable(colorTableToUse);
                if (colorDimness < 0.1f) {
                    colorDimness = 0.1f;
                }
                if (colorDimness < 1.0f) {
                    int       len      = (table[0]).length;
                    int       size     = table.length;
                    float[][] newTable = new float[size][len];
                    for (int rgbIdx = 0; rgbIdx < size; rgbIdx++) {
                        for (int m = 0; m < len; m++) {
                            if (rgbIdx >= 3) {
                                newTable[rgbIdx][m] = table[rgbIdx][m];
                            } else {
                                newTable[rgbIdx][m] = colorDimness
                                        * table[rgbIdx][m];
                            }
                        }
                    }
                    table = newTable;
                }
            }

            //            System.err.println("setColorPalette: " +  fd.displayable);
            fd.displayable.setColorPalette(table);
        }


        //If we had table then set it on the widget and apply the color scale info
        if (table != null) {
            if (ctw != null) {
                if (table.length == 3) {
                    ctw.setColorPalette(new float[][] {
                        table[0], table[1], table[2]
                    });
                } else {
                    ctw.setColorPalette(new float[][] {
                        table[0], table[1], table[2], table[3]
                    });
                }
            }
            if (colorScaleInfo != null) {
                colorScaleInfo.setColorPalette(table);
                applyColorScaleInfo();
            }
        }


    }


    /**
     * Set the color table dimness to the default
     */
    public void resetDimness() {
        colorDimness = 1.0f;
    }


    /**
     * Set the color dimmer
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setColorDimmer() throws VisADException, RemoteException {
        colorDimness -= 0.2f;
        applyColorTable();
    }

    /**
     * Set the color brighter
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setColorBrighter() throws VisADException, RemoteException {
        colorDimness += 0.2f;
        if (colorDimness > 1.0f) {
            colorDimness = 1.0f;
        }
        applyColorTable();
    }


    /**
     * If the color  is non-null then apply it to the
     * {@link ucar.visad.display.Displayable}s in the displayables
     * list that are flagged with the FLAG_COLOR
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void applyColor() throws VisADException, RemoteException {
        if (displayables == null) {
            return;
        }
        deactivateDisplays();
        for (int i = 0, n = displayables.size(); i < n; i++) {
            FlaggedDisplayable fd = (FlaggedDisplayable) displayables.get(i);
            if ( !fd.ok(FLAG_COLOR)) {
                continue;
            }
            if (color == null) {
                color = getDisplayConventions().getColor();
            }
            fd.displayable.setColor(color);
        }
        if (displayListUsesColor) {
            setDisplayListColor(color, false);
        }
        activateDisplays();
    }




    /**
     * Apply the range it to the
     * {@link ucar.visad.display.Displayable}s in the displayables
     * list that are flagged with the FLAG_COLORTABLE
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void applyRange() throws VisADException, RemoteException {
        Range range = null;
        if (displayables == null) {
            return;
        }
        for (int i = 0, n = displayables.size(); i < n; i++) {
            FlaggedDisplayable fd = (FlaggedDisplayable) displayables.get(i);
            if ( !fd.ok(FLAG_COLORTABLE)) {
                continue;
            }
            if (range == null) {
                range = getRangeToApply();
            }
            if (range == null) {
                return;
            }
            //            Trace.call1("DisplayControlImpl:setRangeForColor");
            fd.displayable.setRangeForColor(range);
            //            Trace.call2("DisplayControlImpl:setRangeForColor");
        }
    }

    /**
     * Apply the range it to the
     * {@link ucar.visad.display.Displayable}s in the displayables
     * list that are flagged with the FLAG_SELECTRANGE
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void applySelectRange() throws VisADException, RemoteException {
        Range range = null;
        if (displayables == null) {
            return;
        }
        for (int i = 0, n = displayables.size(); i < n; i++) {
            FlaggedDisplayable fd = (FlaggedDisplayable) displayables.get(i);
            if ( !fd.ok(FLAG_SELECTRANGE)) {
                continue;
            }
            if (range == null) {
                range = getSelectRange();
            }
            if (range == null) {
                return;
            }
            Range theRange = range;
            if ( !selectRangeEnabled) {
                theRange = new Range(Double.NEGATIVE_INFINITY,
                                     Double.POSITIVE_INFINITY);
            }

            //            Trace.call1("DisplayControlImpl:setSelectedRange");

            fd.displayable.setSelectedRange(theRange);
            //            Trace.call2("DisplayControlImpl:setSelectedRange");
        }
    }

    /**
     * Apply the z position to the displayables with FLAG_ZPOSITION set
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected void applyZPosition() throws VisADException, RemoteException {
        if ((displayables == null) || !useZPosition()) {
            return;
        }
        deactivateDisplays();
        for (int i = 0, n = displayables.size(); i < n; i++) {
            FlaggedDisplayable fd = (FlaggedDisplayable) displayables.get(i);
            if ( !fd.ok(FLAG_ZPOSITION)) {
                continue;
            }
            // System.err.println("new z:" +   getVerticalValue(getZPosition()));
            fd.displayable.setConstantPosition(
                getVerticalValue(getZPosition()),
                getNavigatedDisplay().getDisplayAltitudeType());
        }
        activateDisplays();
    }

    /**
     * Apply the line width to the displayables with FLAG_LINEWIDTH set
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected void applyLineWidth() throws VisADException, RemoteException {
        if (displayables == null) {
            return;
        }
        deactivateDisplays();
        for (int i = 0, n = displayables.size(); i < n; i++) {
            FlaggedDisplayable fd = (FlaggedDisplayable) displayables.get(i);
            if ( !fd.ok(FLAG_LINEWIDTH)) {
                continue;
            }
            //      System.err.println("new z:" +   getVerticalValue(getZPosition()));
            fd.displayable.setLineWidth(getLineWidth());
        }
        activateDisplays();
    }

    /**
     *  The default is to use the alpha color table. We use this so derived
     *  classes can override it before applying the color table to the Displayable-s
     *
     * @param ct The original color table
     * @return A 2D float array  that is the actual color table
     */
    public float[][] getColorTableForDisplayable(ColorTable ct) {
        return ct.getAlphaTable();
    }

    /**
     *  Return the range attribute of the colorTable  (if non-null)
     *  else return null;
     * @return The range from the color table attribute
     */
    Range getRangeFromColorTable() {
        if (colorTable != null) {
            return colorTable.getRange();
        }
        return null;
    }

    /**
     *  Return the range attribute of the colorTable  (if non-null)
     *  else return null;
     * @return The range from the color table attribute
     */
    Range getColorRangeFromData() {
        return null;
    }

    /**
     * A hook to add an entry into the range menu
     *
     * @param rw  Range widget
     * @param items List of menu items
     */
    public void addToRangeMenu(RangeWidget rw, List items) {
        //noop
    }

    /**
     * A hook to add an entry into the range menu
     *
     * @param cw the color table widget
     * @param items List of menu items
     * @deprecated use #addToRangeMenu(RangeWidget)
     */
    public void addToRangeMenu(ColorTableWidget cw, List items) {
        //noop
    }


    /**
     * A hook to add an entry into the range menu
     *
     * @param selectRangeWidget the range dialog that has the items
     * @param items List of menu items
     * @deprecated use #addToRangeMenu(RangeWidget)
     */
    public void addToRangeMenu(RangeDialog selectRangeWidget, List items) {
        //noop
    }


    /**
     *  The given properties String is a  ";" delimited list of
     *  name=value properties. This method processes this string,
     * calling setProperty  for each property.
     *
     * @param properties Specification of the name=value properties.
     */
    protected void parseProperties(String properties) {
        if (properties == null) {
            return;
        }
        applyProperties(StringUtil.parsePropertiesString(properties));

    }



    /**
     * Set the initial settings
     *
     * @param settings  the display settings
     */
    public void setInitialSettings(List settings) {
        initialSettings = settings;
    }


    /**
     *
     * @param properties Specification of the name=value properties.
     */
    public void applyProperties(Hashtable properties) {
        if (properties == null) {
            return;
        }
        for (Enumeration keys = properties.keys(); keys.hasMoreElements(); ) {
            String key   = (String) keys.nextElement();
            Object value = properties.get(key);
            setProperty(key, value);
        }
    }




    /**
     * Apply a display setting to this control
     *
     *
     * @param displaySetting  the settings to apply
     *
     * @throws Exception  problem setting settings
     */
    public void applyDisplaySetting(DisplaySetting displaySetting)
            throws Exception {
        applyPropertyValues(displaySetting.getPropertyValues());
    }

    /**
     * Apply the property values in the list
     *
     * @param props  list of property values
     *
     * @throws Exception  problem setting property values
     */
    public void applyPropertyValues(List props) throws Exception {
        for (int i = 0; i < props.size(); i++) {
            PropertyValue prop = (PropertyValue) props.get(i);
            Misc.setProperty(this, prop.getName(), prop.getValue(), false);
        }
        updateLegendAndList();
        notifyViewManagersOfChange();
    }





    /**
     * Used by the IDV to determine whether a control should be removed
     * when the user does a remove all. This is overwritten by the TextDisplayControl
     * to return false.
     *
     * @return Should this control be removed
     */
    public boolean getCanDoRemoveAll() {
        return canDoRemoveAll;
    }

    /**
     * Used by the IDV to determine whether a control should be removed
     * when the user does a remove all.
     *
     * @param v The value
     */
    public void setCanDoRemoveAll(boolean v) {
        canDoRemoveAll = v;
    }

    /**
     * Is this control currently showing the embedded note text area.
     *
     * @return Is showing notes
     */
    public boolean getShowNoteText() {
        return showNoteText;
    }

    /**
     * Used by the presistence/unpersistence to record
     * whether this control is showing its note text area
     *
     * @param n The value for the show note text flag
     */
    public void setShowNoteText(boolean n) {
        showNoteText = n;
        if (showNoteText) {
            showNoteTextArea();
        } else {
            removeNoteTextArea();
        }
    }

    /**
     * This is the value (String) of the note text area.
     *
     * @return The note text
     */
    public String getNoteText() {
        return ((noteTextArea != null)
                ? noteTextArea.getText()
                : null);
    }

    /**
     * Set the value of the note text area.
     *
     * @param n The note text
     */
    public void setNoteText(String n) {
        initNoteText = n;
        if (noteTextArea != null) {
            noteTextArea.setText(n);
        }
    }


    /**
     * Toggle the visibility of the noteTextArea.
     */
    public void toggleNoteTextArea() {
        if (showNoteText) {
            removeNoteTextArea();
        } else {
            showNoteTextArea();
        }
        showNoteText = !showNoteText;
    }


    /**
     * Used to relayout the gui mainPanel
     */
    protected void redoGuiLayout() {
        if (mainPanel == null) {
            return;
        }
        mainPanel.invalidate();
        mainPanel.validate();
        JFrame frame = GuiUtils.getFrame(mainPanel);
        if (frame != null) {
            frame.pack();
        }
    }



    /**
     * Remove the noteText TextArea from the gui.
     */
    private void removeNoteTextArea() {
        if (noteWrapper != null) {
            mainPanel.remove(noteWrapper);
            redoGuiLayout();
        }
    }

    /**
     * Create (if null) and add the note text TextArea into the gui
     */
    private void showNoteTextArea() {
        if (noteTextArea == null) {
            noteTextArea = new JTextArea(5, 30);
            if (initNoteText != null) {
                noteTextArea.setText(initNoteText);
            }
            JScrollPane sp =
                new JScrollPane(
                    noteTextArea,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            JViewport vp = sp.getViewport();
            vp.setViewSize(new Dimension(60, 30));
            noteWrapper = GuiUtils.inset(sp, 4);
        }
        if (mainPanel != null) {
            addNoteText(mainPanel, noteWrapper);
            redoGuiLayout();
        }
    }


    /**
     * Are we displayed in a transect view manager
     *
     * @return Is in transect view
     */
    public boolean isInTransectView() {
        ViewManager vm = getViewManager();
        if ((vm != null) && (vm instanceof TransectViewManager)) {
            /*   List tdcList =
                   getIdv().getVMManager().findTransectDrawingControls();
               if (tdcList.size() == 0) {
                   // ViewPanelImpl.VMInfo vmInfos = vm.get
                   DisplayControl dc =
                       getIdv().doMakeControl("transectdrawingcontrol");
                   vm.getIdv().addDisplayControl(dc);
                  // searching for the shared group map view and move the control there
                   List    vmList = vm.getVMManager().getViewManagers();
                   Boolean moved  = false;
                   for (int i = 0; i < vmList.size(); i++) {
                       ViewManager vm0 = (ViewManager) vmList.get(i);
                       if (vm0 instanceof TransectViewManager) {
                           String grp0 = (String) vm0.getShareGroup();
                           if (vm0.getShareViews() && (grp0 != null)) {
                               for (int j = 0; j < vmList.size(); j++) {
                                   ViewManager vm1 = (ViewManager) vmList.get(j);
                                   if (vm1 instanceof MapViewManager) {
                                       String grp1 =
                                           (String) vm1.getShareGroup();
                                       if (grp0.equals(grp1) && (j != i)) {
                                           dc.moveTo(vm1);
                                           moved = true;
                                           break;
                                       }
                                   }
                               }
                           }
                       }
                   }
                   if ( !moved) {
                       if (dc.getDefaultViewManager() != null) {
                           dc.moveTo(dc.getDefaultViewManager());
                       } else {
                           List vms = vm.getVMManager().getViewManagers();
                           for (int i = 0; i < vms.size(); i++) {
                               ViewManager mvm = (ViewManager) vms.get(i);
                               if (mvm instanceof MapViewManager) {
                                   dc.moveTo(mvm);
                                   break;
                               }
                           }
                       }

                   }
               }   */
            return true;
        }
        return false;
    }


    /**
     * Insert the given noteWrapper (which holds the note text TextArea)
     * into the gui
     *
     * @param mainPanel Where to add the noteWrapper
     * @param noteWrapper Holds the note text TextArea
     */
    protected void addNoteText(JPanel mainPanel, JComponent noteWrapper) {
        mainPanel.add("South", noteWrapper);
    }


    /**
     *  Wrapper around Misc.propertySet
     *
     * @param name Property name
     * @param value Property value
     */
    protected void setProperty(String name, Object value) {
        try {
            ucar.visad.Util.propertySet(this, name, value, false);
        } catch (Exception exc) {
            logException("Setting property:" + name + " value= " + value,
                         exc);
        }
    }


    /**
     *  A sub-class can register any DisplayMaster-s created and managed by them.
     *  When this DisplayControl is removed it runs through all of the
     *  {@link ucar.visad.display.DisplayMaster}-s and calls destroy on them.
     *
     * @param s The {@link ucar.visad.display.DisplayMaster} to add
     */
    protected void addDisplayMaster(DisplayMaster s) {
        if (displayMasters == null) {
            displayMasters = new ArrayList();
        }
        displayMasters.add(s);
    }

    /**
     * A sub-class can register any {@link ucar.unidata.idv.ViewManager}-s
     * created and managed by them. When this DisplayControl is removed it runs
     * through all of the ViewManager-s and calls destroy on them.
     *
     * @param s The {@link ucar.unidata.idv.ViewManager} to add
     */
    protected void addViewManager(ViewManager s) {
        if (viewManagers == null) {
            viewManagers = new ArrayList();
        }
        viewManagers.add(s);
    }


    /**
     *  Runs through the list of ViewManager-s and tells each to destroy.
     *  Creates a new viewManagers list.
     */
    protected void clearViewManagers() {
        if (viewManagers == null) {
            return;
        }
        List tmp = viewManagers;
        viewManagers = null;
        for (int i = 0; i < tmp.size(); i++) {
            ((ViewManager) tmp.get(i)).destroy();
        }
    }




    /**
     *  A sub-class can register any {@link ucar.unidata.collab.SharableImpl}-s
     *  created and managed by them.
     *  When this DisplayControl is removed it runs through all of the
     *  SharableImpl-s and calls removeSharable on them.
     *
     * @param s The {@link ucar.unidata.collab.SharableImpl} to add
     */
    public void addSharable(SharableImpl s) {
        if (sharables == null) {
            sharables = new ArrayList();
        }
        sharables.add(s);
    }


    /**
     *  This is called when this display is created from a template and does not have any data.
     * It is meant as a hook for derived classes to clear out state that may not be in sync with
     * the newly chosen data
     */
    protected void initializeWithNewData() {}



    /**
     * Noop for the DisplayListener interface
     *
     * @param event The event
     */
    public final void displayChanged(DisplayEvent event) {
        if ( !getHaveInitialized()) {
            return;
        }
        int id = event.getId();
        if (id == DisplayEvent.MAPS_CLEARED) {
            if (shouldAddControlListener()) {
                NavigatedDisplay navDisplay = getNavigatedDisplay();
                if (navDisplay != null) {
                    if (projectionControlListeningTo != null) {
                        projectionControlListeningTo.removeControlListener(
                            this);
                    }
                    projectionControlListeningTo =
                        navDisplay.getDisplay().getProjectionControl();
                    projectionControlListeningTo.addControlListener(this);
                }
            }
        }
        handleDisplayChanged(event);
    }


    /**
     * Allow sub-classes to get displayevents
     *
     * @param event The event
     */
    public void handleDisplayChanged(DisplayEvent event) {}


    /**
     * See if two values are close
     *
     * @param a  first value
     * @param b  second value
     *
     * @return  true if they are close
     */
    protected boolean boundsClose(double a, double b) {
        if (a == b) {
            return true;
        }
        if (b == 0) {
            return false;
        }
        double diff = Math.abs((a - b) / b);
        return diff < 0.01;
    }


    /**
     * Noop for the ControlListener interface
     *
     * @param event The event
     */
    public void controlChanged(ControlEvent event) {
        //        if ( !getHaveInitialized()|| !getActive()) {
        if ( !getHaveInitialized()) {
            return;
        }
        //System.out.println("control changed");
        checkBoundsChange();
    }

    /**
     * Check to see if the screen bounds have changed
     */
    protected void checkBoundsChange() {
        Rectangle2D newBounds = calculateRectangle();
        if (Misc.equals(newBounds, lastBounds)) {
            return;
        }
        //System.out.println("control/bounds changed");
        if ((lastBounds != null) && (newBounds != null)) {
            if (boundsClose(lastBounds.getX(), newBounds
                    .getX()) && boundsClose(lastBounds.getY(), newBounds
                    .getY()) && boundsClose(lastBounds.getWidth(), newBounds
                    .getWidth()) && boundsClose(lastBounds
                    .getHeight(), newBounds.getHeight())) {
                //                System.err.println("bounds are close");
                return;
            }
        }

        //        System.err.println("new:" + newBounds + "\nlast:" + lastBounds);
        lastBounds = newBounds;
        //      System.err.println  ("control changed:" + event);
        synchronized (MUTEX_CONTROLCHANGE) {
            //This is the time we last called this method
            lastControlChangeTime = System.currentTimeMillis();

            //Do we have a pending loadData already running? 
            if (controlChangePending) {
                return;
            }
            controlChangePending       = true;
            lastCheckControlChangeTime = lastControlChangeTime;


            //Start up the runnable
            Misc.runInABit(getControlChangeSleepTime(), new Runnable() {
                public void run() {
                    NavigatedDisplay navDisplay = getNavigatedDisplay();
                    while (true) {
                        // Check if we can load data 
                        if (navDisplay.getIsAnimating()
                                || (lastControlChangeTime
                                    != lastCheckControlChangeTime)) {
                            lastCheckControlChangeTime =
                                lastControlChangeTime;
                            Misc.sleep(getControlChangeSleepTime());
                            continue;
                        }
                        viewpointChanged();
                        controlChangePending = false;
                        return;
                    }
                }
            });
        }

    }


    /**
     * This returns the time to sleep, in milliseconds,  between checks for finally
     * handling control changed events.
     *
     * @return milliseconds to sleep for control change events.
     */
    protected long getControlChangeSleepTime() {
        return 500;
    }

    /**
     * This gets called when we have received a controlChanged event
     * and have not received another one in some time delta
     */
    public void viewpointChanged() {
        //System.out.println("viewpointChanged");
        if (getMatchDisplayRegion()) {
            if (reloadFromBounds) {
                loadDataFromViewBounds();
                reloadFromBounds = false;
            }
        }
    }

    /**
     * Method to calculate screen bounds to load new data
     */
    private void loadDataFromViewBounds() {

        NavigatedDisplay nd = getNavigatedDisplay();
        if (nd != null) {
            GeoSelection geoSelection =
                getDataSelection().getGeoSelection(true);
            getViewManager().setProjectionFromData(false);
            try {
                Rectangle2D bbox = nd.getLatLonBox();
                Rectangle2D sbox = nd.getScreenBounds();
                geoSelection.setScreenBound(sbox);
                geoSelection.setLatLonRect(bbox);
                geoSelection.setUseViewBounds(true);
                getDataSelection().setGeoSelection(geoSelection);

                //getDataSelection().putProperty(DataSelection.PROP_REGIONOPTION, DataSelection.PROP_USEDISPLAYAREA);
                EarthLocation el =
                    nd.screenToEarthLocation((int) (sbox.getWidth() / 2),
                                             (int) (sbox.getHeight() / 2));
                LatLonPointImpl llpi =
                    new LatLonPointImpl(el.getLatitude().getValue(),
                                        el.getLongitude().getValue());
                //System.out.print(llpi + "\n");

                getDataSelection().putProperty("centerPosition", llpi);
                dataChanged();
            } catch (Exception e) {}
            ;
        }
    }

    /**
     * Handle animation change events
     *
     * @param evt The event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (initializationDone && getActive()
                && evt.getPropertyName().equals(Animation.ANI_VALUE)) {

            Animation animation = null;
            if ((evt.getSource() != null)
                    && (evt.getSource() instanceof Animation)) {
                animation = (Animation) evt.getSource();
            }
            if (animation == null) {
                animation = ((internalAnimation != null)
                             ? internalAnimation
                             : viewAnimation);
            }

            if (animation != null) {
                timeChanged(animation.getAniValue());
            }
        }
    }


    /**
     *  This gets called by the view manager when it has rcvd its first frame_done event.
     *  We have this here for those displays (e.g., station model) that need state from the
     *  view manager that is only valid when there has been a display.
     */
    public void firstFrameDone() {
        reDisplayColorScales();
    }

    /**
     * Return the list of current {@link ucar.unidata.data.DataChoice}-s
     *
     * @return List of data choices
     */
    public List getDataChoices() {
        if (controlContext != null) {
            if ( !controlContext.getPersistenceManager()
                    .getSaveDataSources()) {
                return null;
            }
        }
        return myDataChoices;
    }


    /**
     * Return the list of current {@link ucar.unidata.data.DataChoice}-s
     *
     * @deprecated Use getDataChoices
     * @return List of data choices
     */
    public List getMyDataChoices() {
        return myDataChoices;
    }



    /**
     * Used to publicize the list of data choices for the persitence mechanism.
     * This is the list of {@link ucar.unidata.data.DataChoice}-s that this control
     * was originally create with
     *
     * @return List of original data choices
     */
    public List getInitDataChoices() {
        if (controlContext != null) {
            if ( !controlContext.getPersistenceManager()
                    .getSaveDataSources()) {
                return null;
            }
        }
        return myDataChoices;
    }


    /**
     *  Used to publicize the list of data choices for the persistence mechanism
     *
     * @param l List of original data choices
     */
    public void setInitDataChoices(List l) {
        initDataChoices = l;
    }



    /**
     * No-op for legacy bundles
     *
     * @param l    List of data
     * @deprecated
     */
    public void setPersistedVisadData(List l) {}



    /**
     * Add the given {@link ucar.unidata.data.DataChoice} to the list of
     * data choices and return true if this is a new DataChoice.
     *
     * @param c The  data choice to add
     * @return Was this data choice added. (It won't be added if it is already
     *          in the list).
     */
    public boolean addDataChoice(DataChoice c) {
        if (myDataChoices == null) {
            myDataChoices = new ArrayList();
        }
        if ( !myDataChoices.contains(c)) {
            myDataChoices.add(c);
            c.addDataChangeListener(this);
            return true;
        }
        return false;
    }

    /**
     *  Remove the given {@link ucar.unidata.data.DataChoice} from the myDataChoices
     * list. Tell the DataChoice to remove this object as a {@link ucar.unidata.data.DataChangeListener}
     *
     * @param dataChoice The data choice to remove
     */
    public void removeDataChoice(DataChoice dataChoice) {
        if (myDataChoices == null) {
            myDataChoices = new ArrayList();
        }
        myDataChoices.remove(dataChoice);
        dataChoice.removeDataChangeListener(this);
    }

    /**
     * Set the list of data choices to be the given list
     *
     * @param newList New list of data choices.
     */
    public void setDataChoices(List newList) {
        removeListenerFromDataChoices();
        myDataChoices = newList;
        addListenerToDataChoices(myDataChoices);
    }

    /**
     * Append the given list of {@link ucar.unidata.data.DataChoice}-s
     * to the myDataChoices  list
     *
     * @param newDataChoices List to append
     */
    public void appendDataChoices(List newDataChoices) {
        if (myDataChoices == null) {
            myDataChoices = new ArrayList();
        }
        myDataChoices.addAll(newDataChoices);
        addListenerToDataChoices(newDataChoices);
    }

    /**
     * Return the single {@link ucar.unidata.data.DataChoice}
     *
     * @return The data choice
     */
    public DataChoice getDataChoice() {
        List tmp = myDataChoices;
        if ((tmp == null) || (tmp.size() == 0)) {
            return null;
        }
        return (DataChoice) tmp.get(0);
    }



    /**
     *  Implementation of {@link ucar.unidata.data.DataChangeListener}.
     */
    public synchronized void dataChanged() {
        if ( !getHaveInitialized()) {
            return;
        }
        showWaitCursor();
        try {
            //Set the flag so we kinow we are being notified of a data change event
            inDataChangeCall = true;
            //Now, reset the data
            resetData();

            List infos = getDisplayInfos();
            for (int i = 0; i < infos.size(); i++) {
                DisplayInfo displayInfo = (DisplayInfo) infos.get(i);
                displayInfo.getViewManager().displayDataChanged(this);
                break;
            }

            //Clear the flag
            inDataChangeCall = false;
        } catch (Exception exc) {
            inDataChangeCall = false;
            logException("Handling new data for display: " + toString(), exc);
        }
        updateLegendAndList();
        showNormalCursor();
    }


    /**
     * Method called by other classes that share the the state.
     * @param from  other class.
     * @param dataId  type of sharing
     * @param data  Array of data being shared.  In this case, the first
     *              (and only?) object in the array is the level
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        try {
            if (dataId.equals(SHARE_COLORTABLE)) {
                setColorTable((ColorTable) data[0], false);
            } else if (dataId.equals(SHARE_COLORSCALE)) {
                colorScaleInfo = (ColorScaleInfo) data[0];
                applyColorScaleInfo();
            } else if (dataId.equals(SHARE_DISPLAYUNIT)) {
                setNewDisplayUnit((Unit) data[0], true);
            } else if (dataId.equals(SHARE_VISIBILITY)) {
                setDisplayVisibility(((Boolean) data[0]).booleanValue(),
                                     false);
            } else if (dataId.equals(SHARE_SKIPVALUE)) {
                skipValue = ((Integer) data[0]).intValue();
                applySkipFactor();
            } else {
                super.receiveShareData(from, dataId, data);
            }
        } catch (Exception exc) {
            logException("Error processing shared state: " + dataId, exc);
        }

    }



    /**
     * This gets called when the control has received notification of a
     * dataChange event. By default it turns around and calls setData with
     * the current list of data choices.
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    protected void resetData() throws VisADException, RemoteException {
        setData(myDataChoices);
    }

    /**
     * Check the attribute  flags and create any required attrbiutes
     * (e.g., Range, ColorTable).
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    private void instantiateAttributes()
            throws VisADException, RemoteException {
        //Now that we have new Data we want to see if there is any
        //contour info's color tables, etc., that need to be initialized.

        if (checkFlag(FLAG_CONTOUR)) {
            setContourInfo(getContourInfo());
        }

        if (checkFlag(FLAG_COLORTABLE)) {
            //Do the color table setting first because the getInitialRange below
            //might use the range held by the color table.
            if ( !getHaveInitialized() && (colorTable != null)) {
                setColorTable(colorTable);
            } else {
                setColorTable(getOldColorTableOrInitialColorTable());
            }

            Range newRange = null;
            if (getHaveInitialized()) {
                newRange = getInitialRange();
            } else if (getRange() == null) {
                newRange = (getRange() == null)
                           ? getInitialRange()
                           : getRange();
            }
            if (newRange != null) {
                setRange(newRange);
            }


            if (colorScales == null) {
                doMakeColorScales();
            }

        }


    }

    /**
     * Called when the user chooses new data for this display
     *
     * @param newChoices List of new {@link ucar.unidata.data.DataChoice}-s
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected void addNewData(List newChoices)
            throws VisADException, RemoteException {
        boolean needToInstantiateAttributes = true;
        if (newChoices.size() == myDataChoices.size()) {
            boolean allOk = true;
            for (int i = 0; i < newChoices.size(); i++) {
                DataChoice newDataChoice = (DataChoice) newChoices.get(i);
                DataChoice oldDataChoice = (DataChoice) myDataChoices.get(i);
                if ( !newDataChoice.basicallyEquals(oldDataChoice)
                        && !Misc.equals(newDataChoice.getName(),
                                        oldDataChoice.getName())) {
                    allOk = false;
                    break;
                } else {}
            }
            if (allOk) {
                needToInstantiateAttributes = false;
            }
        }

        setDataChoices(newChoices);
        setData(myDataChoices);

        if (needToInstantiateAttributes) {
            //If we need to reinstantiate the attributes then clear out the units, etc.
            displayUnit = null;
            colorUnit   = null;
            colorRange  = null;
            selectRange = null;
            instantiateAttributes();
        }
        updateLegendAndList();
        setProjectionInView(true);
    }


    /**
     *  Gets called when the user has selected a new DataChoice.
     *  By default this method extracts the first DataChoice from the list of choices
     *  and calls setData (DataChoice dataChoice) {return true;}
     *  <p>
     *  This returns whether the data setting was successfull or not.
     *
     * @param newChoices List of new {@link ucar.unidata.data.DataChoice}-s
     * @return Was this successful
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     problem creating VisAD object
     */
    protected boolean setData(List newChoices)
            throws VisADException, RemoteException {
        boolean ok;
        if ((myDataChoices != null) && (myDataChoices.size() > 0)) {
            ok = setData((DataChoice) myDataChoices.get(0));
        } else {
            ok = setData((DataChoice) null);
        }
        if (ok) {
            //We used to null these out in case the user selected new data with different units
            //However, this resets what the user has set and screws things up
            //            displayUnit = null;
            //            colorUnit   = null;
            //            colorRange  = null;
            //            selectRange = null;
        }
        return ok;
    }


    /**
     *  Implements the default check if the new DataInstance/DataChoice
     *  pair holds valid data. The default DataInstance.dataOk method does
     *  a getData call (which may be expensive) and returns whether the
     *  Data object is non-null.
     *  Sub-classes can override this method.
     *  For example, the ImageSequence control overridess this to do nothing
     *  because it wants to display a progress bar.
     *
     * @param di The {@link ucar.unidata.data.DataInstance} to check
     * @return Is the data held by this data instance ok (typically is it non-null)
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected boolean checkIfDataOk(DataInstance di)
            throws VisADException, RemoteException {
        try {
            return di.dataOk();
        } catch (DataCancelException dce) {
            //The DataCancelException is thrown when the user has
            //cancelled the selection of operands for formula data choices
            return false;
        }
    }


    /**
     * <p>Sets the data associated with this instance.  This method gets called
     * at initialization or when the user has selected a new
     * {@link ucar.unidata.data.DataChoice} through the data selector
     * control.</p>
     *
     * <p>In order to implement subclasses of this class that behave correctly,
     * you should know that this implementation uses overridable methods of this
     * class in the following way:
     * <ul>
     * <li>Gets a {@link ucar.unidata.data.DataInstance} object by invoking
     * {@link #doMakeDataInstance(DataChoice)} with the given data choice
     * argument.  If the object is <code>null</code>, then this implementation
     * immediately returns <code>false</code>.
     *     </li>
     * <li>Invokes {@link #setDataInstance(DataInstance)} with the {@link
     *     DataInstance} object from the previous step.</li>
     * <li>Invokes {@link #setTitle(String)} with the return
     *     value from {@link #getTitle()}.</li>
     * <li>If {@link #checkFlag(int)} with {@link #FLAG_CONTOUR} returns
     *     <code>true</code>, then {@link #setContourInfo(ContourInfo)} is
     *     invoked with the return value from {@link #getContourInfo()}.</li>
     * <li>If {@link #checkFlag(int)} with {@link #FLAG_COLORTABLE} returns
     *     <code>true</code>, then
     *     <ul>
     *     <li>If {@link #getRange()} returns <code>null</code> or {@link
     *         #getHaveInitialized()} returns <code>true</code>, then
     *         {@link #setRange(Range)} is invoked with the return value from
     *         {@link #getInitialRange()} if it is non-<code>null</code>.
     *         </li>
     *     <li>If {@link #getHaveInitialized()} returns <code>true</code> and
     *         the private field <code>colorTable</code> is
     *         non-<code>null</code>, then {@link #setColorTable(ColorTable)}
     *         is invoked with the private field (there doesn't appear to be
     *         any way to obtain the private field); otherwise,
     *         invokes {@link #setColorTable(ColorTable)} on the return value
     *         from {@link #getInitialColorTable()} when given {@link
     *         #paramName}.
     *         </li>
     *     </ul>
     * </li>
     * </ul>
     *
     * @param dataChoice The {@link ucar.unidata.data.DataChoice} to use.
     * @return Was this setData call successful
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected boolean setData(DataChoice dataChoice)
            throws VisADException, RemoteException {
        //A cheat, actually call reInitialize if we have been notified of a dataChange event.
        if (inDataChangeCall && (getDataInstance() != null)) {
            updateDataInstance(getDataInstance()).reInitialize();
            return true;
        }
        return initializeDataInstance(dataChoice);
    }


    /**
     * Initialize the DataInstance
     *
     * @param dataChoice  the choice to initialize with
     *
     * @return true if successful
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException unable to make VisAD object
     */
    private boolean initializeDataInstance(DataChoice dataChoice)
            throws RemoteException, VisADException {

        /*
        //Make the new DataInstance through the factory call
        // if(dataChoice instanceof DerivedDataChoice){
        DataSelection mySelection = dataChoice.getDataSelection();
        if (mySelection == null) {
            mySelection = new DataSelection();
        }
        GeoSelection gs = mySelection.getGeoSelection();
        if (gs == null) {
            gs = new GeoSelection();
        }
        NavigatedDisplay navDisplay =
            (NavigatedDisplay) getViewManager().getMaster();
        Rectangle screenBoundRect = navDisplay.getScreenBounds();
        gs.setScreenBound(screenBoundRect);
        //gs.setScreenLatLonRect(navDisplay.getLatLonRect());
        mySelection.setGeoSelection(gs);
        if(navDisplay instanceof MapProjectionDisplay)  {
            MapProjectionDisplay md = (MapProjectionDisplay)navDisplay;
            LatLonPointImpl llpi = md.getCenterLLP();
            System.out.print(llpi + "\n");
            mySelection.putProperty("centerPosition", llpi);
        }
        dataChoice.setDataSelection(mySelection);
        // }
         *
         */
        DataInstance di = doMakeDataInstance(dataChoice);

        /*
        if (dataChoice instanceof DerivedDataChoice) {
            DerivedDataChoice derivedDataChoice =
                (DerivedDataChoice) dataChoice;
            while (derivedDataChoice.getChoices().get(0)
                    instanceof DerivedDataChoice) {
                derivedDataChoice =
                    (DerivedDataChoice) derivedDataChoice.getChoices().get(0);
            }
            DirectDataChoice ddc =
                (DirectDataChoice) (derivedDataChoice.getChoices().get(0));
            DataSelection ds = ddc.getDataSelection();
            if (ds != null) {
                isProgressiveResolution =
                    ds.getProperty(DataSelection.PROP_PROGRESSIVERESOLUTION,
                                   false);
            }
        }
        */

        if (cachedData != null) {
            Object id   = dataChoice.getId();
            Data   data = (Data) cachedData.get(id);
            if (data == null) {
                System.err.println("null bytes");
            } else {
                di.setTheData(data);
            }
        }


        //Make sure everything is cool with the new data
        if ((di == null) || !checkIfDataOk(di)) {
            return false;
        }

        setDataInstance(di);
        setTitle(getTitle());
        return true;
    }


    /**
     * Hook method to allow derived classes to return a different
     * initial {@link ucar.unidata.util.Range}
     *
     * @return The initial range to use
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Range getInitialRange() throws RemoteException, VisADException {
        return null;
    }


    /**
     * A hook so derived classes can return a colortable.
     * This method uses the {@link ucar.unidata.idv.DisplayConventions}
     * to get the color table based on the paramName
     *
     * @return The color table to use
     */
    protected ColorTable getInitialColorTable() {
        return getDisplayConventions().getParamColorTable(paramName);
    }


    /**
     * Reset the color table to the initial color table
     */
    protected void revertToDefaultColorTable() {
        try {
            setDisplayInactive();
            setColorTable(getInitialColorTable());
            setRange(getInitialRange());
            setDisplayActive();
        } catch (Exception exc) {
            logException("Resetting color table", exc);
        }

    }

    /**
     * Reset the range to the initial range
     */
    protected void revertToDefaultRange() {
        try {
            setRange(getInitialRange());
        } catch (Exception exc) {
            logException("Resetting range", exc);
        }
    }


    /**
     * Just a utility for bundles that were created with the old
     * code that saved off the color table name, not the color table.
     *
     * @return The color table
     */
    protected final ColorTable getOldColorTableOrInitialColorTable() {
        //Check if we have a color table name from an old bundle
        if ((colorTableName != null) && (controlContext != null)) {
            try {
                ColorTable tmpCt =
                    controlContext.getColorTableManager().getColorTable(
                        colorTableName);
                colorTableName = null;
                return tmpCt;
            } catch (Exception exc) {}
        }
        return getInitialColorTable();
    }




    /**
     * A set method so the categories list will get persisted.
     *
     * @param c The list of {@link ucar.unidata.data.DataCategory}s
     */
    public void setCategories(List c) {
        categories = c;
    }

    /**
     * Returns the list of data categories.
     *
     * @return  The list of {@link ucar.unidata.data.DataCategory}s
     */
    public List getCategories() {
        return categories;
    }


    /**
     * A helper method to show the wait cursor
     */
    public void showWaitCursor() {
        getControlContext().showWaitCursor();
    }




    /**
     * A helper method to show the normal cursor
     */
    public void showNormalCursor() {
        getControlContext().showNormalCursor();
    }



    /**
     * What is the name of this control (e.g., "Plan view")
     *
     * @param displayName The name to use for display purposes
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * I forgot why there was this getter (with the "Property") here
     *
     * @return The display name
     */
    public String getPropertyDisplayName() {
        return displayName;
    }

    /**
     * Returns the name of this display.  Initially, the name of the display is
     * "Display", but it can be set by the {@link #setDisplayName(String)}
     * method.
     *
     * @return                       The name of this display.
     */
    public String getDisplayName() {
        return displayName;
    }


    /**
     * The id of this control (from controls.xml)
     * We have this here for xml persistence
     *
     * @return The id
     */
    public String getDisplayId() {
        return displayId;
    }

    /**
     * Set the id of this control (from controls.xml)
     * We have this here for xml persistence
     *
     * @param theId  The id
     */
    public void setDisplayId(String theId) {
        displayId = theId;
    }

    /**
     * Clear the DataInstance
     */
    protected void clearDataInstance() {
        dataInstances = new ArrayList();
    }


    /**
     * Set the data instance. Protected so it does not get persisted.
     *
     * @param dataInstance The data instance used by this control
     */
    protected void setDataInstance(DataInstance dataInstance) {
        this.dataInstances = Misc.newList(dataInstance);
        setParamName(getDataInstance().getDataChoice().getName());
    }

    /**
     * We have this here (in part) for xml persistence
     *
     * @return The data instance used by this control
     */
    public DataInstance getDataInstance() {
        return getDataInstance(true);
    }


    /**
     * We have this here (in part) for xml persistence
     *
     *
     * @param forceCreation if true, force it.
     * @return The data instance used by this control
     */
    public DataInstance getDataInstance(boolean forceCreation) {
        if ((dataInstances == null) || (dataInstances.size() == 0)) {
            try {
                if (forceCreation && (myDataChoices != null)
                        && (myDataChoices.size() == 1)) {
                    if ( !initializeDataInstance(
                            (DataChoice) myDataChoices.get(0))) {
                        return null;
                    }
                    if ((dataInstances == null)
                            || (dataInstances.size() == 0)) {
                        return null;
                    }
                    return (DataInstance) dataInstances.get(0);
                }
            } catch (Exception exc) {
                logException("Creating data instance for display: "
                             + toString(), exc);
            }
            return null;
        }
        try {
            DataInstance dataInstance = (DataInstance) dataInstances.get(0);
            return dataInstance;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * A wrapper around dataInstance.getData but this calls
     * updateDataInstance first
     *
     * @param dataInstance the dataInstance
     * @return dataInstance.getData();
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem
     */
    public Data getData(DataInstance dataInstance)
            throws VisADException, RemoteException {
        return updateDataInstance(dataInstance).getData();
    }

    /**
     * A wrapper around dataInstance.getGrid but this calls
     * updateDataInstance first
     *
     * @param dataInstance the dataInstance
     * @return dataInstance.getGrid();
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem
     */
    public FieldImpl getGrid(GridDataInstance dataInstance)
            throws VisADException, RemoteException {
        return getGrid(dataInstance, false);
    }


    /**
     * A wrapper around dataInstance.getGrid but this calls
     * updateDataInstance first
     *
     * @param dataInstance the dataInstance
     * @param copy make a copy of the field
     * @return dataInstance.getGrid(copy);
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem
     */
    public FieldImpl getGrid(GridDataInstance dataInstance, boolean copy)
            throws VisADException, RemoteException {
        return updateGridDataInstance(dataInstance).getGrid(copy);
    }


    /**
     * update the datainstance in preparation for a getData call
     *
     * @param dataInstance  the grid data instance
     *
     * @return the new instance
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem
     */
    protected GridDataInstance updateGridDataInstance(
            GridDataInstance dataInstance)
            throws VisADException, RemoteException {
        return (GridDataInstance) updateDataInstance(dataInstance);
    }


    /**
     * update the datainstance in preparation for a getData call.
     * This will set the timeDriverTimes if enabled
     *
     * @param dataInstance the dataInstance to update
     * @return the updated dataInstance
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem
     */
    protected DataInstance updateDataInstance(DataInstance dataInstance)
            throws VisADException, RemoteException {
        if ( !getIdv().getUseTimeDriver()) {
            return dataInstance;
        }
        dataInstance.setDataSelection(
            updateDataSelection(dataInstance.getDataSelection()));
        return dataInstance;
    }


    /**
     * update the dataselection in preparation for a getData call.
     * This will set the timeDriverTimes if enabled
     *
     * @param dataSelection the dataSelection to update
     *
     * @return the new DataSelection
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem
     */
    protected DataSelection updateDataSelection(DataSelection dataSelection)
            throws VisADException, RemoteException {

        // update the geoselection to include at least the screen bounds
        // and the screen lat/lon box if usedisplay area
        GeoSelection geoSelection = dataSelection.getGeoSelection(true);
        // always update the screen size
        NavigatedDisplay navDisplay = getNavigatedDisplay();
        Rectangle2D      sbox       = navDisplay.getScreenBounds();
        geoSelection.setScreenBound(sbox);
        boolean levelChanged = dataSelection.getProperty("levelChanged",
                                   false);
        //if (Misc.equals(dataSelection.getProperty(DataSelection.PROP_REGIONOPTION), 
        //              DataSelection.PROP_USEDISPLAYAREA) && !levelChanged) {
        if (getMatchDisplayRegion() && !levelChanged) {
            getViewManager().setProjectionFromData(false);
            Rectangle2D bbox = navDisplay.getLatLonBox();
            geoSelection.setLatLonRect(bbox);
            geoSelection.setUseViewBounds(true);
            dataSelection.setGeoSelection(geoSelection);
            EarthLocation el =
                navDisplay.screenToEarthLocation((int) (sbox.getWidth() / 2),
                    (int) (sbox.getHeight() / 2));
            LatLonPointImpl llpi =
                new LatLonPointImpl(el.getLatitude().getValue(),
                                    el.getLongitude().getValue());

            dataSelection.putProperty("centerPosition", llpi);
        }
        if (levelChanged) {
            dataSelection.removeProperty("levelChanged");
        }
        if ( !getIdv().getUseTimeDriver()) {
            return dataSelection;
        }
        if (defaultView != null) {
            dataSelection.putProperty(DataSelection.PROP_DEFAULTVIEW, defaultView);
        }
        if (getIsTimeDriver() || !getUsesTimeDriver()) {
            if ( !getUsesTimeDriver()) {
                dataSelection.setTheTimeDriverTimes(null);
                dataSelection.putProperty(DataSelection.PROP_USESTIMEDRIVER,
                                          getUsesTimeDriver());
            }
            return dataSelection;
        }
        ViewManager vm = getViewManager();
        if (vm == null) {
            return dataSelection;
        }
        List<DateTime> times = vm.getTimeDriverTimes();
        //        System.err.println("\tdriver times to use:" + times);
        dataSelection.putProperty(DataSelection.PROP_USESTIMEDRIVER, true);
        dataSelection.setTheTimeDriverTimes(times);
        return dataSelection;
    }

    /**
     * This api is called by ISL to set the spatial subset of the datasource to
     * the Match Display Region, so the user doesn't need to save all data
     * when saving the zidv bundle, if there is more than one view window it will
     * do nothing since it will be too much book keeping to find out the region to
     * subset
     *
     * @param n _more_
     */
    public void setDisplayAreaSubset(String n) {
        List dataSources = getIdv().getAllDataSources();
        List vms         = getIdv().getVMManager().getViewManagers();
        if (vms.size() > 1) {
            return;
        } else {
            for (int i = 0; i < dataSources.size(); i++) {
                DataSource    dataSource = (DataSource) dataSources.get(i);
                DataSelection ds         = dataSource.getDataSelection();
                DataSelection ds1        = getDataSelection();
                ds.setGeoSelection(ds1.getGeoSelection());
                ds.setFromLevel(null);
                ds.setToLevel(null);
                ((DataSourceImpl) dataSource).setDataSelection(ds);
            }
        }
    }

    /**
     * The name of the parameter (initially from the DataChoice) displayed
     * by this control.
     *
     * @param paramName The parameter name
     */
    public void setParamName(String paramName) {
        this.paramName = paramName;
    }


    /**
     * We have this here (in part) for xml persistence
     * This is usually  the {@link ucar.unidata.idv.IntegratedDataViewer}
     *
     * @return The context in which this control exists
     */
    public ControlContext getViewer() {
        return controlContext;
    }

    /**
     * We have this here (in part) for xml persistence
     * This is usually  the {@link ucar.unidata.idv.IntegratedDataViewer}
     *
     * @param controlContext The control context
     */
    public void setViewer(ControlContext controlContext) {
        this.controlContext = controlContext;
    }


    /**
     * A hack for now. The controlContext is really the
     * IDV  which also serves  as the ViewContext
     *
     * @return The context in which view managers exist
     */
    public ViewContext getViewContext() {
        return (ViewContext) controlContext;
    }





    /**
     * This is  the {@link ucar.unidata.idv.IntegratedDataViewer}
     *
     * @return The context in which this control exists
     */
    public ControlContext getControlContext() {
        return controlContext;
    }

    /**
     * Get the integraed data viewer that this is part of
     *
     * @return   the <code>IntegratedDataViewer</code>
     */
    public IntegratedDataViewer getIdv() {
        return getControlContext().getIdv();
    }


    /**
     * Get the {@link ucar.unidata.idv.DisplayConventions} to use.
     *
     * @return The display conventions
     */
    public DisplayConventions getDisplayConventions() {
        return getControlContext().getDisplayConventions();
    }



    /**
     * Used by the idv classes
     *
     * @return The label that describes this control
     */
    public String getLabel() {
        return getDisplayName();
    }

    /**
     * The toString method.
     *
     * @return The displayName
     */
    public String toString() {
        return getDisplayName();
    }

    /**
     * Return the control's JFrame
     *
     * @return The JFrame
     */
    public JFrame getWindow() {
        if (myWindow != null) {
            return myWindow.getFrame();
        }
        return null;
    }





    /**
     * Returns the window title.
     *
     * This implementation invokes the {@link #getDisplayName()} method.
     *
     * @return                       The window title.
     */
    protected String getTitle() {
        //Use the bottom legend text as the window title
        return StringUtil.join("; ", getLegendLabels(BOTTOM_LEGEND), true);
    }


    /**
     *  Set the title of the window if it has been created.
     *
     *  @param title The title
     */
    public void setTitle(String title) {
        if (myWindow != null) {
            myWindow.setTitle(title);
        } else if (outerContents != null) {
            getIdv().getIdvUIManager().displayControlChanged(this);
        }
    }



    /**
     * This method is called  to update the legend labels when
     * some state has changed in this control that is reflected in the labels.
     */
    protected void updateLegendLabel() {
        if ( !haveInitialized) {
            return;
        }
        if (controlContext == null) {
            return;
        }
        setTitle(getTitle());
        if (bottomLegendButton != null) {
            String bottomLegend = StringUtil.join("; ",
                                      getLegendLabels(BOTTOM_LEGEND), true);
            bottomLegendButton.setText(bottomLegend);
        }
        List labels = getLegendLabels(SIDE_LEGEND);
        if (labels.size() > 0) {
            String tmp = labels.get(0).toString();
            for (int i = 0; i < labelsToUpdate.size(); i++) {
                ((JLabel) labelsToUpdate.get(i)).setText(tmp);
            }
        }
        if (legendTextArea != null) {
            String otherText = "";
            for (int i = 1; i < labels.size(); i++) {
                String label = (String) labels.get(i);
                if ((label == null) || (label.length() == 0)) {
                    continue;
                }
                if (otherText.length() != 0) {
                    otherText = otherText + "\n";
                }
                otherText = otherText + label;
            }
            if (otherText.length() > 0) {
                legendTextArea.setVisible(true);
            } else {
                legendTextArea.setVisible(false);
            }
            legendTextArea.setText(otherText);
        }
    }


    /**
     * Update the legend labels and the display list
     */
    protected void updateLegendAndList() {
        if ( !haveInitialized) {
            return;
        }
        updateLegendLabel();
        updateDisplayList();
    }


    /**
     * Update the display list data
     */
    protected void updateDisplayList() {
        if ( !haveInitialized) {
            return;
        }
        Data d = getDisplayListData();
        if ((d != null) && (displayListTable != null)) {
            try {
                for (Enumeration e = displayListTable.elements();
                        e.hasMoreElements(); ) {
                    ((DisplayableData) e.nextElement()).setData(d);
                }
            } catch (VisADException ve) {
                logException("Setting display list data", ve);
            } catch (RemoteException re) {}
        }

    }


    /**
     * Get the data for the display list
     *
     * @return  the dat for the display list
     */
    public Data getDataForDisplayList() {
        return getDisplayListData();
    }


    /**
     * Add the data to the in display legend
     *
     * @return the data for the display list displayable
     */
    protected Data getDisplayListData() {
        Data data = null;
        try {
            String template = applyMacrosToTemplate(getDisplayListTemplate(),
                                  false);
            //if(this instanceof PlanViewControl) 
            //    System.err.println("Using:" + template);

            Set      s   = getDataTimeSet();
            Calendar cal = null;
            if (s instanceof CalendarDateTimeSet) {
                cal = ((CalendarDateTimeSet) s).getCalendar();
            }
            TextType tt = TextType.getTextType(DISPLAY_LIST_NAME);
            if (s != null) {
                FunctionType ft =
                    new FunctionType(((SetType) s.getType()).getDomain(), tt);
                FieldImpl  fi      = new FieldImpl(ft, s);
                double[][] samples = s.getDoubles();
                samples =
                    Unit.convertTuple(samples, s.getSetUnits(),
                                      new Unit[] {
                                          CommonUnit.secondsSinceTheEpoch });
                List fhour = null;
                for (int i = 0; i < s.getLength(); i++) {
                    CalendarDateTime dt = new CalendarDateTime(samples[0][i],
                                              cal);
                    String label =
                        UtcDate.applyTimeMacro(template, dt,
                            getIdv().getPreferenceManager()
                                .getDefaultTimeZone());
                    if ((i == 0) && hasForecastHourMacro(label)) {
                        String rtime =
                            (String) this.getDataChoice().getProperty(
                                "RUNTIME");
                        if ((rtime == null)
                                && (this.getDataChoice()
                                    instanceof DerivedDataChoice)) {
                            DataChoice childChoice =
                                (DataChoice) ((DerivedDataChoice) this
                                    .getDataChoice()).getChoices().get(0);
                            while (childChoice instanceof DerivedDataChoice) {
                                childChoice =
                                    (DataChoice) ((DerivedDataChoice) childChoice)
                                        .getChoices().get(0);
                            }
                            rtime =
                                (String) childChoice.getProperty("RUNTIME");
                        }
                        if ((rtime != null) && (rtime.length() > 0)) {
                            fhour = StringUtil.parseFloatListString(rtime);
                        }
                    }
                    if (hasForecastHourMacro(label) && (fhour != null)
                            && (fhour.size() == s.getLength())) {
                        String v  = "";
                        float  fh = (Float) fhour.get(i);
                        if (fh == Math.round(fh)) {
                            v = (int) fh + "";
                        } else {
                            v = fh + "";
                        }
                        label = label.replace(MACRO_FHOUR2,
                                v).replace(MACRO_FHOUR, v + "H");
                    } else {
                        label = applyForecastHourMacro(label, dt);
                    }
                    Text t = new Text(tt, label);
                    fi.setSample(i, t, false);
                }
                data = fi;
            } else {
                String label = UtcDate.applyTimeMacro(template, null);
                label = applyForecastHourMacro(label, null);
                data  = new Text(tt, label);
            }
        } catch (VisADException ve) {
            logException("Getting display list data", ve);
        } catch (RemoteException re) {}
        return data;

    }

    /**
     * Get the displayable for the Display List
     *
     * @param view the view that it will go into
     * @return the appropriate displayable
     */
    public DisplayableData getDisplayListDisplayable(ViewManager view) {

        if (hasBeenRemoved) {
            return null;
        }

        DisplayableData displayListDisplayable = null;
        if (displayListTable == null) {
            return null;  // in process of removing ?
        }
        try {
            displayListDisplayable =
                (DisplayableData) displayListTable.get(view);
            if (displayListDisplayable == null) {
                displayListDisplayable = createDisplayListDisplayable(view);
                displayListTable.put(view, displayListDisplayable);
            }
            checkTimestampLabel(null);
            updateDisplayList();
            setDisplayListProperties(displayListDisplayable, view);
        } catch (VisADException ve) {
            logException("Getting display list displayable", ve);
        } catch (RemoteException re) {}

        return displayListDisplayable;
    }

    /**
     * Set the display list properties on the displayable
     *
     * @param d   the displayable
     * @param view  the view manager
     *
     * @throws RemoteException  a Java RMI Exception occured
     * @throws VisADException  unable to set properties
     */
    protected void setDisplayListProperties(DisplayableData d,
                                            ViewManager view)
            throws VisADException, RemoteException {
        Font f    = view.getDisplayListFont();
        int  size = (f == null)
                    ? 12
                    : f.getSize();
        if ((f != null) && f.getName().equals(FontSelector.DEFAULT_NAME)) {
            f = null;
        }
        ((TextDisplayable) d).setFont(f);
        ((TextDisplayable) d).setTextSize(size / 12.f);
        if (view.getDisplayListColor() != null) {
            d.setColor(view.getDisplayListColor());
            displayListUsesColor = false;
        }
    }

    /**
     * Get the list of strings to use for the given legend type
     *
     * @param legendType The legend type
     * @return List of legend Strings for the given legend type
     */
    private final List getLegendLabels(int legendType) {
        List labels = new ArrayList();
        getLegendLabels(labels, legendType);
        return labels;
    }





    /**
     * Set the legend buttons
     *
     * @param legendType type of legend
     *
     * @return component containing buttons
     */
    public JComponent getLegendButtons(int legendType) {

        if (legendType == SIDE_LEGEND) {
            if (sideLegendButtonPanel == null) {
                //                DndImageButton dndBtn = new DndImageButton(this, "control");
                sideLegendButtonPanel = GuiUtils.hbox(  /*dndBtn,*/
                    makeLockButton(), makeRemoveButton(), 2);
                //                dndBtn.setToolTipText("Click to drag-and-drop");
                sideLegendButtonPanel.setBackground(null);
            }
            return sideLegendButtonPanel;
        }

        if (bottomLegendButtonPanel == null) {
            bottomLegendButtonPanel = GuiUtils.hbox(makeLockButton(),
                    makeRemoveButton(), 2);
            bottomLegendButtonPanel.setBackground(null);
        }
        return bottomLegendButtonPanel;
    }

    /**
     * Get the short parameter name
     * @return The String to be used for the short parameter name
     */
    protected String getShortParamName() {
        return paramName;
    }


    /**
     * Get the long parameter name
     * @return The String to be used for the long parameter name
     */
    protected String getLongParamName() {
        DataChoice dataChoice = getDataChoice();
        return ((dataChoice == null)
                ? null
                : dataChoice.getDescription());
    }

    /**
     * Get the DataSources associated with this contol
     *
     * @return list of data sources
     */
    public List getDataSources() {
        List dataSources = new ArrayList();
        if (myDataChoices != null) {
            for (int i = 0; i < myDataChoices.size(); i++) {
                DataChoice dc = (DataChoice) myDataChoices.get(i);
                dc.getDataSources(dataSources);
            }
        }
        return dataSources;
    }



    /**
     * Add any macro name/value pairs.
     *
     *
     * @param template template for the label
     * @param patterns The macro names
     * @param values The macro values
     */
    protected void addLabelMacros(String template, List patterns,
                                  List values) {
        List      dataSources    = getDataSources();
        Hashtable seen           = new Hashtable();
        String    dataSourceName = null;
        for (int i = 0; i < dataSources.size(); i++) {
            DataSource dataSource = (DataSource) dataSources.get(i);
            if (seen.get(dataSource) != null) {
                continue;
            }
            seen.put(dataSource, dataSource);
            String name = DataSelector.getNameForDataSource(dataSource);
            if (dataSourceName == null) {
                dataSourceName = name;
            } else {
                dataSourceName = ";" + name;
            }
        }
        patterns.add(MACRO_DISPLAYNAME);
        values.add(getDisplayName());
        patterns.add(MACRO_SHORTNAME);
        values.add(getShortParamName());
        patterns.add(MACRO_LONGNAME);
        values.add(getLongParamName());
        patterns.add(MACRO_DATASOURCENAME);
        values.add(dataSourceName);
        if (displayUnit != null) {
            patterns.add(MACRO_DISPLAYUNIT);
            values.add("" + displayUnit);
        }
        patterns.add(MACRO_RESOLUTION);
        if ((resolutionReadout == null) || resolutionReadout.isEmpty()) {
            values.add("");
        } else {
            values.add(resolutionReadout);
        }

    }


    /**
     * This method is used to get all of the labels (String)
     * that are shown in the side legend.
     *
     * @param labels A list that the labels are inserted into
     * @param legendType The type of legend, BOTTOM_LEGEND or SIDE_LEGEND
     */
    protected void getLegendLabels(List labels, int legendType) {
        labels.add(applyMacrosToTemplate(getLegendLabelTemplate(), true));
        if ((extraLabelTemplate != null)
                && (extraLabelTemplate.length() > 0)) {
            labels.addAll(
                StringUtil.split(
                    applyMacrosToTemplate(extraLabelTemplate, true), "\n",
                    true, true));
        }
    }

    /**
     * Utility to apply the macro values to the given template
     *
     * @param template template
     * @param timeOk should we include the time stamp
     *
     * @return label
     */
    private String applyMacrosToTemplate(String template, boolean timeOk) {
        List patterns = new ArrayList();
        List values   = new ArrayList();
        addLabelMacros(template, patterns, values);
        if (timeOk && hasTimeMacro(template)) {
            if ((firstTime == null) || (currentTime == null)) {
                checkTimestampLabel(null);
            }
            if (UtcDate.containsTimeMacro(template)) {
                template = UtcDate.applyTimeMacro(template, currentTime,
                        getIdv().getPreferenceManager().getDefaultTimeZone());
            }
            template = applyForecastHourMacro(template, currentTime);
        }
        return StringUtil.replaceList(template, patterns, values);
    }


    /**
     * Apply the forecast hour macro
     *
     * @param t label string
     * @param currentTime first time
     *
     * @return modified string
     */
    protected String applyForecastHourMacro(String t, DateTime currentTime) {
        if (hasForecastHourMacro(t)) {
            String v = "";
            if (firstTime == null) {
                checkTimestampLabel(null);
            }
            if ((firstTime != null) && (currentTime != null)) {
                try {
                    double diff =
                        currentTime.getValue(CommonUnit.secondsSinceTheEpoch)
                        - firstTime.getValue(CommonUnit.secondsSinceTheEpoch);
                    v = ((int) (diff / 60 / 60)) + "";
                } catch (Exception exc) {
                    System.err.println("Error:" + exc);
                    exc.printStackTrace();
                }
            }
            return t.replace(MACRO_FHOUR2, v).replace(MACRO_FHOUR, v + "H");
        }
        return t;
    }

    /**
     * Check if the label string has a forecast hour macro in it.
     * @param t  the string to check
     * @return true if it does
     */
    protected boolean hasForecastHourMacro(String t) {
        return t.matches(".*(" + MACRO_FHOUR + "|" + MACRO_FHOUR2 + ").*");
    }


    /**
     * Return the label used for the menues in the IDV. Implements
     * the method in the {@link DisplayControl} interface
     *
     * @return The menu label
     */
    public String getMenuLabel() {
        List labels = getLegendLabels(SIDE_LEGEND);
        if (labels.size() > 0) {
            return labels.get(0).toString();
        }
        String l1 = ((paramName == null)
                     ? ""
                     : paramName);
        if (l1.length() > 20) {
            l1 = l1.substring(0, 19) + "... ";
        }
        return l1 + " " + getDisplayName();
    }






    /**
     * Move the control's window to the front.
     */
    public void toFront() {
        if ((myWindow != null) && makeWindow) {
            if (myWindow.getState() == Frame.ICONIFIED) {
                myWindow.setState(Frame.NORMAL);
            }
            myWindow.toFront();
        }
    }


    /**
     *  Should the DisplayControl do a doRemove when the window closes.
     *
     *  @return Whether to remove this control on a window close.
     */
    protected boolean removeOnWindowClose() {
        return getObjectStore().get(PREF_REMOVEONWINDOWCLOSE, false);
    }

    /**
     * This is called to inform this display control that its gui has been imported by some
     * other idv component and it no longer is in a window.
     */
    public void guiImported() {
        IdvWindow tmp = myWindow;
        myWindow = null;
        if (tmp != null) {
            tmp.dispose();
            setMakeWindow(false);
        }
    }


    /**
     * This is called to inform this display control that its gui has been exported out
     * of some other component (ex: the MultiDisplayControl).
     */
    public void guiExported() {
        if (myWindow != null) {
            return;
        }
        setMakeWindow(true);
        myWindow = createIdvWindow();
        if (myWindow != null) {
            myWindow.setTitle(getTitle());
            myWindow.setContents(outerContents);
            myWindow.show();
        }
    }


    /**
     * Create an IDV window
     *
     * @return  the window
     */
    protected IdvWindow createIdvWindow() {
        if ( !getIdv().okToShowWindows()) {
            return null;
        }


        myWindow       = new IdvWindow(getTitle(), getIdv());

        windowListener = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (myWindow == null) {
                    return;
                }
                handleWindowClosing();
            }
        };
        myWindow.addWindowListener(windowListener);
        ControlDescriptor cd = getIdv().getControlDescriptor(displayId);
        if (cd != null) {
            String iconLoc = cd.getIcon();
            if (iconLoc != null) {
                ImageIcon icon = GuiUtils.getImageIcon(iconLoc,
                                     getIdv().getClass());
                if (icon != null) {
                    myWindow.setIconImage(icon.getImage());
                }
            }
        }
        return (IdvWindow) myWindow;
    }

    /**
     * Make the DisplayListDisplayable
     *
     * @param view the view that this will be in
     * @return The displayable data object
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   VisAD problem
     */
    protected DisplayableData createDisplayListDisplayable(ViewManager view)
            throws VisADException, RemoteException {
        DisplayableData dt = new TextDisplayable(getTitle(),
                                 TextType.getTextType(DISPLAY_LIST_NAME),
                                 true);
        dt.setUseTimesInAnimation(false);
        if (displayListColor == null) {
            if (view.getDisplayListColor() != null) {
                displayListColor = view.getDisplayListColor();
            } else if (color != null) {
                displayListColor     = color;
                displayListUsesColor = true;
            } else {
                displayListColor = getDisplayConventions().getColor();
            }
        }
        dt.setColor(displayListColor);
        dt.setVisible(getDisplayVisibility());

        if (firstTime == null) {
            checkTimestampLabel(null);
        }

        Data d = getDisplayListData();
        if (d != null) {
            dt.setData(d);
        }
        return dt;
    }


    /**
     *  Called by the derived class init method to create the gui window.
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void doMakeWindow() throws VisADException, RemoteException {
        if (outerContents != null) {
            return;
        }
        if (contents == null) {
            contents = doMakeContents();
        }


        mainPanel = GuiUtils.center(GuiUtils.inset(contents, 3));

        if (showNoteText) {
            showNoteTextArea();
        }

        //        outerContents = GuiUtils.topCenterBottom(doMakeMenuBar(), mainPanel, makeBottomButtons());
        DndImageButton dndBtn = new DndImageButton(this, "control");
        //        JComponent topPanel = GuiUtils.centerRight(doMakeMenuBar(),dndBtn);
        outerContents = GuiUtils.topCenter(doMakeMenuBar(), mainPanel);
        //        outerContents = GuiUtils.topCenter(topPanel, mainPanel);

        ucar.unidata.util.Msg.translateTree(outerContents, true);

    }

    /*
    protected JComponent makeBottomButtons() {
        JButton removeBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Remove16.gif",
                                     this, "doRemove");
        removeBtn.setToolTipText("Remove Display Control");

        JButton expandBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/DownDown.gif", this,
                                     "expandControl", this);

        expandBtn.setToolTipText("Expand in the tabs");
        JButton exportBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Export16.gif", this,
                                     "undockControl", this);
        exportBtn.setToolTipText("Undock control window");

        JButton propBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Information16.gif",
                                     this, "showProperties");
        propBtn.setToolTipText("Show Display Control Properties");


        DndImageButton dnd = new DndImageButton(this,"idv/display");
        dnd.setToolTipText("Drag and drop to a window component");
        JPanel buttonPanel =
            GuiUtils.left(GuiUtils.hbox(Misc.newList(expandBtn, exportBtn,
                propBtn, removeBtn,dnd), 4));


        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0,
                Color.lightGray.darker()));
        return buttonPanel;
    }
    */


    /**
     * Is the GUI being shown
     *
     * @return  true if it is
     */
    protected boolean isGuiShown() {
        if (mainPanel != null) {
            try {
                mainPanel.getLocationOnScreen();
            } catch (Exception exc) {
                return false;
            }
        }
        return true;
    }

    /**
     * Make the menu bar
     *
     * @return The menu bar
     */
    protected JMenuBar doMakeMenuBar() {
        DndImageButton dndBtn  = new DndImageButton(this, "control");
        List           menus   = doMakeMenuBarMenus(new ArrayList());
        JMenuBar       menuBar = new JMenuBar();
        for (int i = 0; i < menus.size(); i++) {
            menuBar.add((JMenu) menus.get(i));
        }
        //        menuBar.add(dndBtn);
        return menuBar;
    }


    /** The last image capture dir */
    private static File captureDir;

    /**
     * Screen snapshot of window
     */
    public void captureWindow() {
        String name = getClass().getName();
        int    idx  = name.lastIndexOf(".");
        name = name.substring(idx + 1);
        name = name + ".jpg";
        JTextField nameFld = new JTextField(name, 15);
        nameFld.setCaretPosition(0);
        JFileChooser chooser = new FileManager.MyFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAccessory(GuiUtils.top(GuiUtils.hbox(new JLabel("Name:"),
                nameFld, 5)));
        if (captureDir != null) {
            chooser.setCurrentDirectory(captureDir);
        }
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        captureDir = chooser.getSelectedFile();

        name       = nameFld.getText().trim();
        String path = IOUtil.joinDir(captureDir.toString(), name);
        try {
            //TODO: Use the robot iamge capture here.
            ImageUtils.writeImageToFile(outerContents, path);
            System.out.println("Write to:" + path);
        } catch (Exception exc) {
            logException("Capturing window", exc);
        }
    }


    /**
     * Set the main panel dimenstions
     *
     * @throws Exception  problem setting the dimensions
     */
    protected void setMainPanelDimensions() throws Exception {
        doMakeWindow();
        boolean needToResize = (mainPanel.getSize().width == 0)
                               || (mainPanel.getSize().height == 0);
        mainPanel.invalidate();
        //        mainPanel.validate();
        if (needToResize) {
            JFrame frame = GuiUtils.getFrame(mainPanel);
            if (frame != null) {
                if (mainPanelSize != null) {
                    mainPanel.setSize(mainPanelSize);
                }
                frame.pack();
                if (mainPanelSize != null) {
                    frame.setSize(mainPanelSize);
                }
            } else {
                JFrame f = new JFrame();
                if (mainPanelSize != null) {
                    mainPanel.setSize(mainPanelSize);
                    mainPanel.setMinimumSize(mainPanelSize);
                    mainPanel.setPreferredSize(mainPanelSize);
                }
                f.getContentPane().add(mainPanel);
                f.pack();
            }
        }
    }

    /**
     * Get the image
     *
     * @return  the image
     *
     * @throws Exception problem getting image
     */
    public Image getImage() throws Exception {
        return getImage(null);
    }


    /**
     * Allows a derived class to provide its own viewmanager wehn capturing an image of the display from isl
     *
     * @param what The specification of the viewmanager (from the isl)
     *
     * @return the viewmanager or null
     *
     * @throws Exception on badness
     */
    public ViewManager getViewManagerForCapture(String what)
            throws Exception {
        System.err.println("base for capture:" + getClass().getName());
        return null;
    }


    /**
     * Get the image of "what"
     *
     * @param what  description of what to get
     *
     * @return the image
     *
     * @throws Exception problem getting image
     */
    public Image getImage(String what) throws Exception {
        if (what != null) {
            System.err.println("Unknown image capture component:" + what);
        }
        setMainPanelDimensions();
        if ( !getIdv().getArgsManager().getIsOffScreen()) {
            GuiUtils.showComponentInTabs(mainPanel);
        }
        return ImageUtils.getImage(mainPanel);
    }


    /**
     *  Make the menus to put in the menu bar
     *
     * @param menus List to add to
     *
     * @return The menus list
     */
    protected List doMakeMenuBarMenus(List menus) {
        if (menus == null) {
            menus = new ArrayList();
        }
        final JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(GuiUtils.charToKeyCode("F"));
        final JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(GuiUtils.charToKeyCode("E"));
        final JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(GuiUtils.charToKeyCode("V"));
        final JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(GuiUtils.charToKeyCode("H"));
        ArrayList extras = new ArrayList();
        getExtraMenus(extras, true);


        menus.add(fileMenu);
        menus.add(editMenu);
        menus.add(viewMenu);
        if ( !extras.isEmpty()) {
            for (int i = 0; i < extras.size(); i++) {
                menus.add((JMenu) extras.get(i));
            }
        }
        menus.add(helpMenu);

        fileMenu.addMenuListener(new MenuListener() {
            public void menuCanceled(MenuEvent e) {}

            public void menuDeselected(MenuEvent e) {}

            public void menuSelected(MenuEvent e) {
                makeFileMenu(fileMenu);
            }
        });


        viewMenu.addMenuListener(new MenuListener() {
            public void menuCanceled(MenuEvent e) {}

            public void menuDeselected(MenuEvent e) {}

            public void menuSelected(MenuEvent e) {
                makeViewMenu(viewMenu);
            }
        });

        editMenu.addMenuListener(new MenuListener() {
            public void menuCanceled(MenuEvent e) {}

            public void menuDeselected(MenuEvent e) {}

            public void menuSelected(MenuEvent e) {
                makeEditMenu(editMenu);
            }
        });

        helpMenu.addMenuListener(new MenuListener() {
            public void menuCanceled(MenuEvent e) {}

            public void menuDeselected(MenuEvent e) {}

            public void menuSelected(MenuEvent e) {
                makeHelpMenu(helpMenu);
            }
        });

        return menus;
    }


    /**
     * Get the object store
     *
     * @return  the store
     */
    public XmlObjectStore getStore() {
        if (controlContext == null) {
            return null;
        }
        return controlContext.getObjectStore();
    }







    /**
     * Handle the window closing.
     */
    protected void handleWindowClosing() {


        boolean remove = removeOnWindowClose();
        if ( !remove && (getDisplayInfos().size() == 0)) {
            remove = getStore().get(PREF_STANDALONE_REMOVEONCLOSE, false);
            if (getStore().get(PREF_STANDALONE_REMOVEONCLOSE_ASK, false)) {
                JCheckBox askCbx =
                    new JCheckBox("Don't show this window again", false);

                JPanel contents =
                    GuiUtils
                        .vbox(new JLabel(
                            "<html><b>NOTE:</b> This display control has no other components.<p>Do you want to remove it or just close the window?</html>"), GuiUtils
                                .filler(), askCbx);

                contents = GuiUtils.inset(contents, 10, 5);
                int result = GuiUtils.makeDialog(getWindow(),
                                 "Remove Or Close?", contents, getWindow(),
                                 new String[] { "Remove the Display",
                        "Close the Window" });
                remove = result == 0;
                getStore().put(PREF_STANDALONE_REMOVEONCLOSE, remove);
                getStore().put(PREF_STANDALONE_REMOVEONCLOSE_ASK,
                               !askCbx.isSelected());
                getStore().save();
            }
        }

        if (remove) {
            Misc.run(this, "doRemove");
        } else {
            //After a window close (like from hitting the "X") we cannot show the  window again
            //So null out the window so we create a new one later
            if ((myWindow != null) && (windowListener != null)) {
                myWindow.removeWindowListener(windowListener);
                myWindow = null;
            }
        }
    }

    /**
     * The outer contents is the outermost component of the gui (i.e.,
     * it holds the menu bar and the mainPanel)
     *
     * @return Gui contents
     */
    public Component getOuterContents() {
        return outerContents;
    }

    /**
     * Get the component for the main panel of this DisplayControlImpl's
     * contents.
     *
     * @return GUI contents
     */
    public JComponent getMainPanel() {
        return mainPanel;
    }

    /**
     * Should this control make its own window.
     * This method is here (mostly) for xml unpersistence
     *
     * @param value The make window flag
     */
    public void setMakeWindow(boolean value) {
        makeWindow = value;
    }

    /**
     * Should this control make its own window.
     * This method is here (mostly) for xml unpersistence
     *
     * @return The flag
     */
    public boolean getMakeWindow() {
        return makeWindow;
    }

    /**
     * This method is here (mostly) for xml unpersistence
     *
     * @param value The size of the window
     */
    public void setWindowSize(Dimension value) {
        windowSize = value;
    }

    /**
     * This method is here (mostly) for xml unpersistence
     *
     * @return The size of the window (if non-null) else null
     */
    public Dimension getWindowSize() {
        if (myWindow != null) {
            return myWindow.getSize();
        }
        return null;
    }


    /**
     * Get the main panel size
     *
     * @return the main panel size
     */
    public Dimension getMainPanelSize() {
        if (mainPanel != null) {
            return mainPanel.getSize();
        }
        return null;
    }


    /**
     * Set the main panel size
     *
     * @param s the dimensions of the panel
     */
    public void setMainPanelSize(Dimension s) {
        mainPanelSize = s;
    }



    /**
     * This method is here (mostly) for xml unpersistence
     *
     * @param x The x location of the window
     */
    public void setWindowX(int x) {
        windowX = x;
    }

    /**
     * This method is here (mostly) for xml unpersistence
     *
     * @return The x location of the window
     */
    public int getWindowX() {
        return ((myWindow != null)
                ? myWindow.getLocation().x
                : 50);
    }

    /**
     * This method is here (mostly) for xml unpersistence
     *
     *
     * @param y The y location of the window
     */
    public void setWindowY(int y) {
        windowY = y;
    }

    /**
     * This method is here (mostly) for xml unpersistence
     *
     * @return The y location of the window
     */
    public int getWindowY() {
        return ((myWindow != null)
                ? myWindow.getLocation().y
                : 100);
    }

    /**
     * SHow the window
     */
    public void show() {
        popup(null);
        if ((myWindow != null) && makeWindow) {
            controlContext.showWindow(this, myWindow);
        }
    }


    /**
     * Hide or show the main window
     */
    public void toggleWindow() {
        if (makeWindow) {
            if (getWindowVisible()) {
                hide();
            } else {
                popup(sideLegendLabel);
            }
        } else if (outerContents != null) {
            GuiUtils.showComponentInTabs(outerContents);
        }
    }


    /**
     * Hide the window
     */
    public void hide() {
        if (myWindow != null) {
            myWindow.setVisible(false);
        }
    }

    /**
     *  A hook method to allow subclasses to initialize the dialog window,
     *  set the size of the dialog window, etc.
     *
     * @param window The control's window
     */
    public void initWindow(final IdvWindow window) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    //Was removed?
                    if (hasBeenRemoved) {
                        return;
                    }
                    if (outerContents == null) {
                        return;
                    }

                    window.setTitle(getTitle());
                    window.setContents(outerContents);
                    if (windowSize != null) {
                        window.setWindowBounds(new Rectangle(windowX,
                                windowY, windowSize.width,
                                windowSize.height));
                    } else {
                        window.setLocation(windowX, windowY);
                    }
                    controlContext.showWindow(DisplayControlImpl.this,
                            window);
                    if (myWindowVisible) {
                        //                            System.err.println("calling show");
                        //                            show();
                    } else {
                        //System.err.println("calling hide");
                        //                            hide();
                    }
                } catch (Exception exc) {
                    System.err.println("ERROR:" + hasBeenRemoved);
                    System.err.println("oops: " + exc);
                    exc.printStackTrace();
                }
            }
        });
    }


    /**
     * Is the window currently visible
     *
     * @return Window visibility
     */
    public boolean getWindowVisible() {
        return ((myWindow != null)
                ? myWindow.isShowing()
                : false);
    }


    /**
     * Does this display control popup its window on creation
     *
     * @return Should the window be visible
     */
    public boolean shouldWindowBeVisible() {
        return myWindowVisible;
    }


    /**
     *  Set the local data memeber myWindowVisible. If myWindow is
     *  non-null then set its visiblity as well. This method is mostly used
     *  in the XmlEncoder persistence mechanism.
     *
     * @param v Window visibility
     */
    public void setWindowVisible(boolean v) {
        myWindowVisible = v;
        if (myWindow != null) {
            if (myWindowVisible) {
                show();
            } else {
                hide();
            }
        }
    }



    /**
     * A hook to allow derived classes to have their own label in the menu
     * for the change data call.
     *
     * @return Menu label for the change data call.
     */
    protected String getChangeParameterLabel() {
        return "Change Parameter...";
    }


    /**
     * Implement the HyperLinkListener method to pass any link clicks
     * off to the {@link ucar.unidata.idv.ControlContext}
     *
     * @param e The event
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String url;
            if (e.getURL() == null) {
                url = e.getDescription();
            } else {
                url = e.getURL().toString();
            }
            controlContext.handleAction(url, null);
        }
    }


    /**
     * Returns the help url that should be used.
     *
     * @return Either the helpUrl member (if non-null) or
     * the displayId prepended with "idv.controls"
     */
    private String getActualHelpUrl() {
        if (helpUrl != null) {
            return helpUrl;
        }
        return "idv.controls." + displayId;
    }

    /**
     * Does this display have a map projection
     *
     * @return Has map projection
     */
    public boolean hasMapProjection() {
        return hasDisplayWithData();
    }


    /**
     * Determines whether this control has a {@link ucar.visad.display.Displayable} that has
     * non-null Data that isa FieldImpl
     *
     * @return Does this control have a visad.Data
     */
    private boolean hasDisplayWithData() {
        boolean hasDisplayWithData = false;
        try {
            List infos = getDisplayInfos();
            for (int i = 0, n = infos.size(); i < n; i++) {
                DisplayInfo info = (DisplayInfo) infos.get(i);
                Data        data = info.getDisplayable().getData();
                if ((data != null) && (data instanceof FieldImpl)) {
                    hasDisplayWithData = true;
                    break;
                }
            }
        } catch (Exception exc) {
            logException("Inserting displayables", exc);
        }
        return hasDisplayWithData;
    }



    /**
     * Create the edit menu
     *
     * @param menu The edit menu
     */
    private void makeEditMenu(JMenu menu) {
        menu.removeAll();
        List items = new ArrayList();
        getEditMenuItems(items, true);
        GuiUtils.makeMenu(menu, items);
        Msg.translateTree(menu);
    }

    /**
     * Create the view menu
     *
     * @param menu The view menu
     */
    private void makeViewMenu(JMenu menu) {
        menu.removeAll();
        List items = new ArrayList();
        getViewMenuItems(items, true);
        getIdv().getIdvUIManager().addViewMenuItems(this, items);
        if (componentHolder != null) {
            items.add(GuiUtils.makeMenuItem("Undock", componentHolder,
                                            "undockControl"));
        }
        GuiUtils.makeMenu(menu, items);
        Msg.translateTree(menu);
    }


    /**
     * Reload the data sources
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException VisAD problem
     */
    public void reloadDataSource() throws RemoteException, VisADException {
        List dataSources = Misc.makeUnique(getDataSources());
        for (int i = 0; i < dataSources.size(); i++) {
            ((DataSource) dataSources.get(i)).reloadData();
        }
    }

    /**
     * reload the data source in a thread.
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException VisAD problem
     */
    public void reloadDataSourceInThread()
            throws RemoteException, VisADException {
        Misc.run(this, "reloadDataSource");
    }

    /**
     * Get last file menu items.
     *
     * @param items the last items in the file menu.
     */
    protected void getLastFileMenuItems(List items) {
        if ((myDataChoices != null) && (myDataChoices.size() > 0)) {
            items.add(GuiUtils.MENU_SEPARATOR);
            items.add(GuiUtils.makeMenuItem("Reload Data", this,
                                            "reloadDataSourceInThread"));
        }

        if (makeWindow) {
            items.add(GuiUtils.MENU_SEPARATOR);
            items.add(GuiUtils.makeMenuItem((getWindowVisible()
                                             ? "Close Window"
                                             : "Show Window"), this,
                                             "toggleWindow"));
        }
    }

    /**
     * Create the file menu
     *
     * @param menu The file menu
     */
    private void makeFileMenu(JMenu menu) {
        menu.removeAll();
        List items = new ArrayList();
        //Uncomment this line if you want an easy way to screen capture the window
        //      items.add(GuiUtils.makeMenuItem("Capture", this,"captureWindow"));
        getFileMenuItems(items, true);
        List saveItems = new ArrayList();
        getSaveMenuItems(saveItems, true);
        items.add(GuiUtils.MENU_SEPARATOR);
        items.add(GuiUtils.makeMenu(new JMenu("Save"), saveItems));
        getLastFileMenuItems(items);
        GuiUtils.makeMenu(menu, items);
        Msg.translateTree(menu);
    }

    /**
     * Add in the menu items for the save and export display template
     *
     * @param items List of menu items to add into
     * @param forMenuBar if this is for the menubar
     */
    protected void getSaveMenuItems(List items, boolean forMenuBar) {
        if ( !items.isEmpty()) {
            items.add(GuiUtils.MENU_SEPARATOR);
        }
        if (canSaveDataInCache()) {
            items.add(GuiUtils.makeMenuItem("Save Data in Cache...", this,
                                            "saveDataChoiceInCache"));
        }
        if (canExportData()) {
            items.add(
                GuiUtils.makeMenuItem(
                    "Export Displayed Data to NetCDF...", this,
                    "exportDisplayedData", FileManager.SUFFIX_NETCDF, true));
        }
        if ((myDataChoices != null) && haveParameterDefaults()
                && (myDataChoices.size() == 1)) {
            items.add(GuiUtils.makeMenuItem("Save As Parameter Defaults",
                                            this, "saveAsParameterDefaults"));

        }
        items.addAll(GuiUtils.makeMenuItems(this, new String[][] {
            { "Save Display as Favorite...", "saveAsFavorite", null,
              "Save this display, without its data, as a favorite template" },
            { "Save Display as Bundle...", "saveAsTemplate", null,
              "Save this display, without its data, as a template" }
            /*            { "Save Display State as Default", "saveAsPrototype", null,
              "Use the state of this display as the default for new displays of this type" },
            { "Clear Default", "clearPrototype", null,
            "Clear any saved default state for displays of this type" }*/
        }));
    }


    /**
     * Create the help menu
     *
     * @param menu The help menu
     */
    private void makeHelpMenu(JMenu menu) {
        menu.removeAll();
        List items = new ArrayList();
        getHelpMenuItems(items, true);
        GuiUtils.makeMenu(menu, items);
        Msg.translateTree(menu);
    }

    /**
     * Add the  relevant edit menu items into the list
     *
     * @param items List of menu items
     * @return the list with the new items
     */
    public List getEditMenuItems(List items) {
        getEditMenuItems(items, false);
        return items;
    }

    /**
     * Add the  relevant edit menu items into the list
     *
     * @param items List of menu items
     * @return the list with the new items
     */
    public List getFileMenuItems(List items) {
        getFileMenuItems(items, false);
        return items;
    }

    /**
     * Add the  relevant view menu items into the list
     *
     * @param items List of menu items
     * @return the list with the new items
     */
    public List getViewMenuItems(List items) {
        getViewMenuItems(items, false);
        return items;
    }


    /**
     * Add the  relevant edit menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {


        boolean addedSeparator = false;
        if (ctw != null) {
            if ((items.size() > 0) && !addedSeparator) {
                items.add(GuiUtils.MENU_SEPARATOR);
            }
            addedSeparator = true;
            items.add(ctw.makeMenu());
        }


        if (checkFlag(FLAG_COLOR)) {
            if ((items.size() > 0) && !addedSeparator) {
                items.add(GuiUtils.MENU_SEPARATOR);
            }
            addedSeparator = true;
            items.add(doMakeChangeColorMenu());
        }

        /*
        if (getDisplayInfos().size() > 0) {
            //            items.add(GuiUtils.MENU_SEPARATOR);
            //            items.add(GuiUtils.makeCheckboxMenuItem("Show In Display List",
            //                    this, "showInDisplayList", null));
            //            if (getShowInDisplayList()) {
            if ((items.size() > 0) && !addedSeparator) {
                items.add(GuiUtils.MENU_SEPARATOR);
            }
            addedSeparator = true;
            items.add(
                GuiUtils.makeMenu(
                    "Display List Color",
                    makeChangeColorMenuItems(
                        "setDisplayListColor", displayListColor)));
            //            }
        }
        */


        boolean addedChangeSeparator = false;
        if (checkFlag(FLAG_DATACONTROL)) {
            if ( !addedChangeSeparator) {
                items.add(GuiUtils.MENU_SEPARATOR);
            }
            addedChangeSeparator = true;
            items.add(doMakeChangeParameterMenuItem());
        }

        if (checkFlag(FLAG_DISPLAYUNIT) && (displayUnit != null)) {
            if ( !addedChangeSeparator) {
                items.add(GuiUtils.MENU_SEPARATOR);
            }
            addedChangeSeparator = true;
            items.add(GuiUtils.makeMenuItem("Change Display Unit...", this,
                                            "changeDisplayUnit"));
        }

        if (checkFlag(FLAG_COLORUNIT) && (colorUnit != null)) {
            if ( !addedChangeSeparator) {
                items.add(GuiUtils.MENU_SEPARATOR);
            }
            addedChangeSeparator = true;
            items.add(GuiUtils.makeMenuItem("Change Color Unit...", this,
                                            "changeColorUnit"));
        }

        if (checkFlag(FLAG_CONTOUR)) {
            if ( !addedChangeSeparator) {
                items.add(GuiUtils.MENU_SEPARATOR);
            }
            addedChangeSeparator = true;
            items.add(GuiUtils.makeMenuItem("Change Contours...", this,
                                            "showContourPropertiesDialog"));
        }


        if ( !items.isEmpty()) {
            items.add(GuiUtils.MENU_SEPARATOR);
        }

        JMenu sharingMenu = new JMenu("Sharing");
        items.add(sharingMenu);
        sharingMenu.add(GuiUtils.makeCheckboxMenuItem("Sharing On", this,
                "sharing", null));

        sharingMenu.add(GuiUtils.makeMenuItem("Set Share Group", this,
                "showSharableDialog"));


        if ( !items.isEmpty()) {
            items.add(GuiUtils.MENU_SEPARATOR);
        }

        items.add(GuiUtils.setIcon(GuiUtils.makeMenuItem("Display Settings...",
                this,
                "showDisplaySettingsDialog"), "/auxdata/ui/icons/Settings16.png"));


        items.add(GuiUtils.setIcon(GuiUtils.makeMenuItem("Properties...",
                this,
                "showProperties"), "/auxdata/ui/icons/information.png"));

    }





    /**
     * Check to see if we have any display properties.
     * @return true if we have properties for parameter defaults.
     */
    protected boolean haveParameterDefaults() {
        return (ctw != null)
               || (checkFlag(FLAG_DISPLAYUNIT) && (displayUnit != null))
               || (checkFlag(FLAG_CONTOUR));
    }



    /**
     * Get the time set
     *
     * @return  the Set for times
     *
     * @throws RemoteException  RMI exception
     * @throws VisADException  Couldn't create time set
     */
    public Set getTimeSet() throws RemoteException, VisADException {
        return getDataTimeSet();
    }



    /**
     * Collect the time animation set from the displayables.
     * If none found then return null.
     *
     * @return Animation set
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Set getDataTimeSet() throws RemoteException, VisADException {
        Set  aniSet = null;
        List infos  = getDisplayInfos();
        for (int i = 0; i < infos.size(); i++) {
            DisplayInfo displayInfo = (DisplayInfo) infos.get(i);
            Animation animation = displayInfo.getViewManager().getAnimation();
            if (animation == null) {
                continue;
            }

            //Displayable.debug = (this instanceof PlanViewControl);
            RealType aniType = animation.getAnimationRealType();
            //Displayable.debug = false;

            Set set = displayInfo.getDisplayable().getAnimationSet(aniType,
                          true);

            if (set == null) {
                //if(this instanceof PlanViewControl) 
                //    System.err.println ("      no set ");
                continue;
            }
            aniSet = (aniSet == null)
                     ? set
                     : aniSet.merge1DSets(set);
        }
        return aniSet;
    }


    /**
     * Do any of our displayables have times
     *
     * @return Have times
     */
    protected boolean haveDataTimes() {
        try {
            return getDataTimeSet() != null;
        } catch (Exception exc) {
            logException("Checking for times", exc);
        }
        return false;
    }


    /**
     * Utility to make the macro popup button that adds macros into a text component
     *
     * @param field The field
     * @param pref If non-null add the "Set as preference" items
     * @param property  the property
     *
     * @return The button
     */
    private JButton makeMacroPopup(final JTextComponent field,
                                   final String pref, final String property) {
        final JButton popupBtn = new JButton("Add Macro");
        popupBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showMacroPopup(popupBtn, field, pref, property);
            }
        });
        return popupBtn;
    }




    /**
     * Show the popup for macros
     *
     * @param popupBtn   the popup button
     * @param field      The text field associated with this popup
     * @param pref       the preference to set
     * @param property   the property
     */
    private void showMacroPopup(JButton popupBtn, final JTextComponent field,
                                final String pref, String property) {
        List macroItems = new ArrayList();
        List names      = new ArrayList();
        List labels     = new ArrayList();
        getMacroNames(names, labels);
        for (int i = 0; i < names.size(); i++) {
            JMenuItem btn = new JMenuItem(labels.get(i).toString());
            btn.addActionListener(new ObjectListener(names.get(i)) {
                public void actionPerformed(ActionEvent ae) {
                    String text        = field.getText();
                    int    caret       = field.getCaretPosition();
                    int    selectStart = field.getSelectionStart();
                    int    selectEnd   = field.getSelectionEnd();
                    // System.out.println("Select start = " + selectStart + "; end = " + selectEnd);
                    if (selectStart == selectEnd) {  // no selected text
                    } else {
                        text = text.substring(0, selectStart)
                               + text.substring(selectEnd);
                        caret = Math.max(0, (caret
                                             - (selectEnd - selectStart)));
                    }
                    if (caret >= text.length()) {
                        field.setText(text + theObject);
                    } else {
                        field.setText(text.substring(0, caret) + theObject
                                      + text.substring(caret));
                    }
                }
            });
            macroItems.add(btn);
        }

        if (pref != null) {
            macroItems.add(GuiUtils.MENU_SEPARATOR);
            macroItems.add(
                GuiUtils.makeMenuItem(
                    "Use as default for this type of display", this,
                    "setLabelAsPreference", new Object[] { field,
                    pref + "." + displayId }));
            macroItems.add(
                GuiUtils.makeMenuItem(
                    "Use as default for all displays with data", this,
                    "setLabelAsPreference", new Object[] { field,
                    pref + ".data" }));
            macroItems.add(
                GuiUtils.makeMenuItem(
                    "Use as default for all displays without data", this,
                    "setLabelAsPreference", new Object[] { field,
                    pref + ".nodata" }));
        }






        final JPopupMenu macroPopup = GuiUtils.makePopupMenu(macroItems);
        macroPopup.show(popupBtn, 0, popupBtn.getBounds().height);
    }




    /**
     * Find displays based on the key
     *
     * @param key  the key
     *
     * @return  List of  appropriate display
     */
    protected List findDisplays(String key) {
        List displays = getIdv().getDisplayControls();
        return findDisplays(key, displays);
    }

    /**
     * Find displays with the particular key in the list of displays
     *
     * @param key  key to look for
     * @param displays  list of displays
     *
     * @return  the displays for that key
     */
    protected List findDisplays(String key, List displays) {
        List result = new ArrayList();
        if ((key == null) || (key.length() == 0) || (key.equals(FIND_ALL))) {
            return displays;
        }
        if (key.equals(FIND_THIS)) {
            result.add(this);
            return result;
        }
        ViewManager vm = getDefaultViewManager();
        for (int i = 0; i < displays.size(); i++) {
            DisplayControlImpl control = (DisplayControlImpl) displays.get(i);
            if (key.startsWith(FIND_CLASS)) {
                String className = key.substring(6);
                if (control.getClass().getName().equals(className)) {
                    result.add(control);
                }
            } else if (key.startsWith(FIND_WITHTHISVIEW)) {
                if (Misc.equals(vm, control.getDefaultViewManager())) {
                    result.add(control);
                }
            } else if (key.startsWith(FIND_CATEGORY)) {
                String value = key.substring(9);
                if (Misc.equals(value, control.getDisplayCategory())) {
                    result.add(control);
                }
            } else if (key.equals(FIND_WITHDATA)) {
                if (control.getShortParamName() != null) {
                    result.add(control);
                }
            } else if (key.equals(FIND_SPECIAL)) {
                if (control.getShortParamName() == null) {
                    result.add(control);
                }
            } else if (key.equals(FIND_WITHTHISDATA)) {
                if (Misc.equals(this.getDataSources(),
                                control.getDataSources())) {
                    result.add(control);
                }
            } else if (key.equals(FIND_WITHTHISFIELD)) {
                if (this.getShortParamName().equals(
                        control.getShortParamName())) {
                    result.add(control);
                }
            } else {
                if (i == 0) {
                    System.err.println("unknown key:" + key);
                }
            }
        }
        return result;
    }



    /**
     * Popup a category menu
     *
     * @param categoryFld the text field
     * @param categoryBtn the button
     */
    private void popupCategoryMenu(JTextField categoryFld,
                                   JButton categoryBtn) {
        List categoryItems = new ArrayList();
        for (Enumeration keys =
                allCategories.keys(); keys.hasMoreElements(); ) {
            String key = (String) keys.nextElement();
            categoryItems.add(GuiUtils.makeMenuItem(key, categoryFld,
                    "setText", key));
        }



        final JPopupMenu categoryPopup =
            GuiUtils.makePopupMenu(categoryItems);

        categoryPopup.show(categoryBtn, 0, categoryBtn.getBounds().height);

    }



    /**
     * Show the properties dialog
     */
    public void showProperties() {
        JTabbedPane jtp = new JTabbedPane();
        addPropertiesComponents(jtp);
        final JDialog propertiesDialog =
            GuiUtils.createDialog("Properties -- " + getTitle(), true);
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String cmd = event.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_APPLY)) {
                    if ( !applyProperties()) {
                        return;
                    }
                }
                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_CANCEL)) {
                    propertiesDialog.dispose();
                }
            }
        };
        Window     f       = GuiUtils.getWindow(contents);
        JComponent buttons = GuiUtils.makeApplyOkCancelButtons(listener);
        JComponent propContents = GuiUtils.inset(GuiUtils.centerBottom(jtp,
                                      buttons), 5);
        Msg.translateTree(jtp, true);
        propertiesDialog.getContentPane().add(propContents);
        propertiesDialog.pack();
        if (f != null) {
            GuiUtils.showDialogNearSrc(f, propertiesDialog);
        } else {
            propertiesDialog.setVisible(true);
        }
    }

    /**
     * Add tabs to the properties dialog.
     *
     * @param jtp  the JTabbedPane to add to
     */
    protected void addPropertiesComponents(JTabbedPane jtp) {

        int  width = 20;
        List comps = new ArrayList();


        categoryFld = new JTextField(displayCategory, width);

        final JTextField catFld      = categoryFld;




        final JButton    categoryBtn = new JButton("<<");
        categoryBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                popupCategoryMenu(catFld, categoryBtn);
            }
        });


        comps.add(GuiUtils.rLabel("Display Category:"));
        comps.add(categoryFld);
        comps.add(GuiUtils.left(categoryBtn));


        // legend label properties
        legendLabelTemplateFld = new JTextField(getLegendLabelTemplate(),
                width);



        legendLabelTemplateFld.setToolTipText("Enter your own label");
        JButton popupBtn = makeMacroPopup(legendLabelTemplateFld,
                                          PREF_LEGENDLABEL_TEMPLATE,
                                          "legendLabelTemplate");
        comps.add(GuiUtils.rLabel("Legend Label:"));
        comps.add(legendLabelTemplateFld);
        comps.add(popupBtn);

        extraLabelTemplateFld = new JTextArea(extraLabelTemplate, 3, 25);
        extraLabelTemplateFld.setToolTipText("Enter extra legend labels");
        popupBtn = makeMacroPopup(extraLabelTemplateFld,
                                  PREF_EXTRALABEL_TEMPLATE,
                                  "extraLabelTemplate");



        JScrollPane sp =
            new JScrollPane(
                extraLabelTemplateFld,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);


        comps.add(GuiUtils.top(GuiUtils.rLabel("Extra Legend Labels:")));
        comps.add(sp);
        comps.add(popupBtn);

        // display list properties
        displayListTemplateFld = new JTextField(displayListTemplate, width);
        // color swatch  changer
        JPanel colorSwatch = GuiUtils.wrap(new JLabel("     "));
        colorSwatch.setBackground(getDisplayListColor());
        colorSwatch.setToolTipText("Click to change display list color");
        final JComponent theColorSwatch = colorSwatch;
        colorSwatch.addMouseListener(new ObjectListener(null) {
            public void mouseClicked(MouseEvent me) {
                popupDisplayListColorMenu(theColorSwatch);
                theColorSwatch.setBackground(getDisplayListColor());
                theColorSwatch.invalidate();
                if (theColorSwatch.getParent() != null) {
                    theColorSwatch.getParent().validate();
                }
            }
        });
        displayListTemplateFld.setToolTipText(
            "Enter your own display list label");
        popupBtn = makeMacroPopup(displayListTemplateFld,
                                  PREF_DISPLAYLIST_TEMPLATE,
                                  "displayListTemplate");
        comps.add(GuiUtils.rLabel("Display Label:"));
        comps.add(GuiUtils.centerRight(displayListTemplateFld, colorSwatch));
        comps.add(popupBtn);



        idFld = new JTextField(id, width);
        comps.add(GuiUtils.rLabel("Id:"));
        comps.add(idFld);
        comps.add(new JLabel(" (for scripting)"));

        visbilityAnimationPauseFld = new JTextField(""
                + visbilityAnimationPause, 5);
        visbilityAnimationPauseFld.setToolTipText(
            "Number of seconds this display should be shown when in visibiltiy animation mode");
        /*
        comps.add(GuiUtils.rLabel("Visiblilty Pause:"));
        comps.add(GuiUtils.left(GuiUtils.hbox(visbilityAnimationPauseFld,new JLabel(" in seconds,  use -1 for default"))));
        comps.add(GuiUtils.filler());
        */



        JPanel settingsPanel = getSettingsPanel();
        if (settingsPanel != null) {
            comps.add(GuiUtils.filler());
            comps.add(GuiUtils.topLeft(settingsPanel));
            comps.add(GuiUtils.filler());
        }

        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        GuiUtils.setHFill();
        JComponent contents = GuiUtils.doLayout(comps, 3, GuiUtils.WT_NYN,
                                  GuiUtils.WT_N);


        jtp.add("Settings", GuiUtils.top(contents));



        if (checkFlag(FLAG_COLORTABLE)) {
            csd = new ColorScaleDialog(this, "Color Scale Properties",
                                       getColorScaleInfo(), false);
            jtp.add("Color Scale", GuiUtils.top(csd.getContents()));
        }


        GeoSelection geoSelection  = null;
        List         selectedTimes = null;
        if (dataSelection != null) {
            geoSelection = dataSelection.getGeoSelection(true);
            if (dataSelection.hasTimes()) {
                selectedTimes = dataSelection.getTimes();
            }
        }


        dataSelectionWidget = null;
        if ( /*selectedTimes!=null && */myDataChoices.size() == 1) {
            DataChoice dataChoice = (DataChoice) myDataChoices.get(0);
            List       allTimes   = dataChoice.getAllDateTimes();
            if ((allTimes != null) && (allTimes.size() > 0)) {
                dataSelectionWidget = new DataSelectionWidget(getIdv());
                jtp.add("Times", dataSelectionWidget.getTimesList());
                dataSelectionWidget.setTimes(allTimes, selectedTimes);
                if (selectedTimes != null) {
                    dataSelectionWidget.setUseAllTimes(false);
                }
            }
        }


        //dataSelectionComponents = null;

        List dataSources = Misc.makeUnique(getDataSources());
        geoSelectionPanel = null;
        if (dataSources.size() == 1) {
            DataSourceImpl dataSource = (DataSourceImpl) dataSources.get(0);
            if (dataSource.canDoGeoSelection()) {
                geoSelectionPanel =
                    ((DataSourceImpl) dataSource).doMakeGeoSelectionPanel(
                        true, geoSelection);
                jtp.add("Spatial Subset", geoSelectionPanel);
            }


            /*    try {
                     if (getDataChoice() != null) {
                         dataSelectionComponents =
                             dataSource.getDataSelectionComponents(
                                 getDataChoice());
                         if (dataSelectionComponents != null) {
                             for (DataSelectionComponent dsc :
                                     dataSelectionComponents) {
                                 if (dsc.getShowInControlProperties()) {
                                    // dsc.getContents(getDataSelection());
                                   //  jtp.add(dsc.getName(),
                                   //          dsc.getContents(getDataSelection()));
                                 }
                             }
                         }
                     }
                 } catch (Exception exc) {
                     logException("Initializing  properties", exc);
                 }
     */
        }





    }







    /**
     * Add property values for this DisplaySettingsDialog
     *
     * @param dsd  the display settings dialog
     */
    protected void addDisplaySettings(DisplaySettingsDialog dsd) {

        dsd.addPropertyValue(getDisplayCategory(), "displayCategory",
                             "Display Category", "Labels");
        dsd.addPropertyValue(getLegendLabelTemplate(), "legendLabelTemplate",
                             "Legend Label", "Labels");
        dsd.addPropertyValue(getExtraLabelTemplate(), "extraLabelTemplate",
                             "Extra Legend Labels", "Labels");
        dsd.addPropertyValue(getDisplayListTemplate(), "displayListTemplate",
                             "Display Label", "Labels");


        if (displayUnit != null) {
            dsd.addPropertyValue(displayUnit, "settingsDisplayUnit",
                                 "Display Unit", SETTINGS_GROUP_DISPLAY);
        }


        ColorTable ct = getColorTable();
        if (ct != null) {
            dsd.addPropertyValue(ct, "colorTable", "Color Table",
                                 SETTINGS_GROUP_DISPLAY);
        }

        if (colorRange != null) {
            dsd.addPropertyValue(colorRange, "range", "Color Range",
                                 SETTINGS_GROUP_DISPLAY);
        }

        if (selectRangeEnabled && (selectRange != null)) {
            dsd.addPropertyValue(selectRange, "selectRange", "Data Range",
                                 SETTINGS_GROUP_DISPLAY);
        }

        if (contourInfo != null) {
            dsd.addPropertyValue(contourInfo, "contourInfo",
                                 "Contour Settings", SETTINGS_GROUP_DISPLAY);
        }

        if (colorScaleInfo != null) {
            dsd.addPropertyValue(colorScaleInfo, "colorScaleInfo",
                                 "Color Scale Settings",
                                 SETTINGS_GROUP_DISPLAY);
        }

        if (checkFlag(FLAG_LINEWIDTH)) {
            dsd.addPropertyValue(new Integer(lineWidth), "lineWidth",
                                 "Line Width", SETTINGS_GROUP_DISPLAY);
        }
        if (checkFlag(FLAG_SKIPFACTOR)) {
            dsd.addPropertyValue(new Integer(skipValue), "skipValue",
                                 "Skip Value", SETTINGS_GROUP_DISPLAY);
        }
        if (checkFlag(FLAG_ZPOSITION) && useZPosition()) {
            dsd.addPropertyValue(new Double(getZPosition()), "zPosition",
                                 "Vertical Position", SETTINGS_GROUP_DISPLAY);
        }
        if (checkFlag(FLAG_COLOR) && (color != null)) {
            dsd.addPropertyValue(color, "color", getColorWidgetLabel(),
                                 SETTINGS_GROUP_DISPLAY);
        }

        if (checkFlag(FLAG_TEXTUREQUALITY)) {
            dsd.addPropertyValue(new Integer(textureQuality),
                                 "textureQuality", getTextureQualityLabel(),
                                 SETTINGS_GROUP_DISPLAY);
        }
        if (checkFlag(FLAG_SMOOTHING)) {
            dsd.addPropertyValue(getSmoothingType(), "smoothingType",
                                 "Smoothing Type", SETTINGS_GROUP_DISPLAY);
            dsd.addPropertyValue(new Integer(getSmoothingFactor()),
                                 "smoothingFactor", "Smoothing Factor",
                                 SETTINGS_GROUP_DISPLAY);
        }

        dsd.addPropertyValue(new Boolean(getDisplayVisibility()),
                             "displayVisibility", "Visibility",
                             SETTINGS_GROUP_FLAGS);
        dsd.addPropertyValue(new Boolean(getLockVisibilityToggle()),
                             "lockVisibilityToggle",
                             "Lock Visibility Toggle", SETTINGS_GROUP_FLAGS);
        dsd.addPropertyValue(new Boolean(getShowInDisplayList()),
                             "showInDisplayList", "Show In Display List",
                             SETTINGS_GROUP_FLAGS);
        dsd.addPropertyValue(new Boolean(getUseFastRendering()),
                             "useFastRendering", "Use Fast Rendering",
                             SETTINGS_GROUP_FLAGS);
        dsd.addPropertyValue(new Boolean(getUseTimesInAnimation()),
                             "useTimesInAnimation", "Use Times In Animation",
                             SETTINGS_GROUP_FLAGS);

        dsd.addPropertyValue(new Boolean(getDoCursorReadout()),
                             "doCursorReadout", "Include In Cursor Readout",
                             SETTINGS_GROUP_FLAGS);
        dsd.addPropertyValue(new Boolean(getCanDoRemoveAll()),
                             "canDoRemoveAll", "Remove on Remove All",
                             SETTINGS_GROUP_FLAGS);
        dsd.addPropertyValue(new Boolean(getShowNoteText()), "showNoteText",
                             "Show Note Text", SETTINGS_GROUP_FLAGS);
        if (getIdv().getUseTimeDriver()) {
            dsd.addPropertyValue(new Boolean(getIsTimeDriver()),
                                 "isTimeDriver",
                                 "Drive Times with this Display",
                                 SETTINGS_GROUP_FLAGS);
            dsd.addPropertyValue(new Boolean(getUsesTimeDriver()),
                                 "usesTimeDriver", "Use Time Driver Times",
                                 SETTINGS_GROUP_FLAGS);
        }



    }


    /**
     * Set the label as a preference
     *
     * @param obj  list of objects defining the label
     */
    public void setLabelAsPreference(Object[] obj) {
        JTextComponent field = (JTextComponent) obj[0];
        String         pref  = (String) obj[1];
        getStore().put(pref, field.getText().trim());
        getStore().save();
    }



    /**
     * Show the DisplaySettingsDialog
     */
    public void showDisplaySettingsDialog() {
        DisplaySettingsDialog displaySettingsDialog =
            new DisplaySettingsDialog(this);
    }




    /**
     * Get the settings panel
     *
     * @return the panel for settings
     */
    protected JPanel getSettingsPanel() {
        List comps = new ArrayList();
        addCheckBoxSettings(comps, methodNameToSettingsMap);
        if (comps.size() > 4) {
            return GuiUtils.left(GuiUtils.doLayout(comps, 2, GuiUtils.WT_N,
                    GuiUtils.WT_N));
        }
        return GuiUtils.left(GuiUtils.vbox(comps));
    }







    /**
     * Add checkbox type settings to the Properties panel
     *
     * @param comps  list of checkbox components
     * @param methodNameToSettingsMap  hashtable of methods to checkbox
     */
    protected void addCheckBoxSettings(List comps,
                                       Hashtable methodNameToSettingsMap) {
        JCheckBox cbx;


        methodNameToSettingsMap.put(
            "setLockVisibilityToggle",
            cbx = new JCheckBox(
                "Lock Visibility Toggle", getLockVisibilityToggle()));


        comps.add(cbx);

        if (getDisplayInfos().size() > 0) {
            methodNameToSettingsMap.put(
                "setShowInDisplayList",
                cbx = new JCheckBox(
                    "Show In Display List", getShowInDisplayList()));
            comps.add(cbx);
        }


        if (shouldApplyFastRendering()) {
            methodNameToSettingsMap.put("setUseFastRendering", cbx =
                new JCheckBox("Use Fast Rendering", getUseFastRendering()));
            comps.add(cbx);
        }

        if (haveDataTimes()) {
            methodNameToSettingsMap.put(
                "setUseTimesInAnimation",
                cbx = new JCheckBox(
                    "Use Times In Animation", getUseTimesInAnimation()));
            comps.add(cbx);
        }

        methodNameToSettingsMap.put("setCanDoRemoveAll", cbx =
            new JCheckBox("Remove on Remove All", getCanDoRemoveAll()));

        comps.add(cbx);

        methodNameToSettingsMap.put("setDoCursorReadout", cbx =
            new JCheckBox("Include in cursor readout", getDoCursorReadout()));
        comps.add(cbx);

        methodNameToSettingsMap.put("setShowNoteText",
                                    cbx = new JCheckBox("Show Note Text",
                                        getShowNoteText()));
        comps.add(cbx);


    }


    /**
     * Apply the properties from the dialog
     *
     * @return true if successful
     */
    public final boolean applyProperties() {
        try {
            if ( !doApplyProperties()) {
                return false;
            }
            boolean needToReloadData = false;
            if (geoSelectionPanel != null) {
                GeoSelection newGeoSelection = (geoSelectionPanel.getEnabled()
                        ? geoSelectionPanel.getGeoSelection()
                        : null);
                GeoSelection oldGeoSelection =
                    getDataSelection().getGeoSelection(true);
                if ( !Misc.equals(newGeoSelection, oldGeoSelection)) {
                    getDataSelection().setGeoSelection(newGeoSelection);
                    setMatchDisplayRegion(newGeoSelection.getUseViewBounds());
                    needToReloadData = true;
                }
            }
            /*     if (dataSelectionComponents != null) {
                     for (DataSelectionComponent dsc : dataSelectionComponents) {
                         if (dsc.getShowInControlProperties()) {
                             dsc.applyToDataSelection(getDataSelection());
                             needToReloadData = true;
                         }
                     }
                 }*/
            if (dataSelectionWidget != null) {
                List oldSelectedTimes = getDataSelection().getTimes();
                List selectedTimes =
                    dataSelectionWidget.getSelectedDateTimes();
                if ( !Misc.equals(oldSelectedTimes, selectedTimes)) {
                    getDataSelection().setTimes(selectedTimes);
                    needToReloadData = true;
                }
            }
            if (needToReloadData) {
                reloadDataSourceInThread();
            }


        } catch (Exception exc) {
            logException("Applying properties", exc);
            return false;
        }
        updateLegendAndList();
        notifyViewManagersOfChange();
        return true;
    }


    /**
     * Apply the properties
     *
     * @return true if successful
     */
    public boolean doApplyProperties() {
        if (csd != null) {
            csd.doApply();
        }
        if (idFld == null) {
            return true;
        }
        setId(idFld.getText());
        visbilityAnimationPause = new Integer(
            visbilityAnimationPauseFld.getText().trim()).intValue();

        setDisplayCategory(categoryFld.getText());
        setDisplayListTemplate(displayListTemplateFld.getText());
        setExtraLabelTemplate(extraLabelTemplateFld.getText().trim());

        setLegendLabelTemplate(legendLabelTemplateFld.getText());
        if (hasTimeMacro(legendLabelTemplate)
                || hasTimeMacro(extraLabelTemplate)
                || hasTimeMacro(getDisplayListTemplate())) {
            try {
                if ((internalAnimation == null) && (viewAnimation == null)) {
                    getSomeAnimation();
                }
                if (internalAnimation != null) {
                    timeChanged(internalAnimation.getAniValue());
                } else if (viewAnimation != null) {
                    timeChanged(viewAnimation.getAniValue());
                }
            } catch (Exception exc) {
                logException("Getting animation", exc);
            }
        }

        try {
            for (Enumeration keys = methodNameToSettingsMap.keys();
                    keys.hasMoreElements(); ) {
                String    key  = (String) keys.nextElement();
                JCheckBox cbx = (JCheckBox) methodNameToSettingsMap.get(key);
                boolean   flag = cbx.isSelected();
                Method theMethod = Misc.findMethod(getClass(), key,
                                       new Class[] { Boolean.TYPE });

                theMethod.invoke(this, new Object[] { new Boolean(flag) });
            }
        } catch (Exception exc) {
            throw new IllegalArgumentException("Error:" + exc);
        }

        return true;
    }



    /**
     * Popup the contour properties dialog
     */
    public void showContourPropertiesDialog() {
        if (contourWidget != null) {
            contourWidget.showContourPropertiesDialog();
        }
    }


    /**
     * Popup the color scale properties dialog
     */
    public void showColorScaleDialog() {
        ColorScaleDialog csd = new ColorScaleDialog(this,
                                   "Color Scale Properties",
                                   getColorScaleInfo(), true);
    }


    /**
     * Add any macro name/label pairs
     *
     * @param names List of macro names
     * @param labels List of macro labels
     */
    protected void getMacroNames(List names, List labels) {
        boolean haveData = (getShortParamName() != null);
        names.add(MACRO_DISPLAYNAME);
        labels.add("Display Name");

        if (haveData) {
            names.addAll(Misc.newList(MACRO_SHORTNAME, MACRO_LONGNAME,
                                      MACRO_DATASOURCENAME));
            labels.addAll(Misc.newList("Field Short Name", "Field Long Name",
                                       "Data Source Name"));
        }
        if (displayUnit != null) {
            names.add(MACRO_DISPLAYUNIT);
            labels.add("Display Unit");
        }
        if (haveDataTimes()) {
            names.add(MACRO_TIMESTAMP);
            labels.add("Time Stamp");
            names.add(MACRO_FHOUR);
            labels.add("Forecast Hour");
            names.add(MACRO_FHOUR2);
            labels.add("Forecast Hour (value only)");
        }
        if (canDoProgressiveResolution()) {
            names.addAll(Misc.newList(MACRO_RESOLUTION));
            labels.addAll(Misc.newList("Resolution"));
        }
    }


    /**
     * Called by ISL.
     * Write out some data defined by the what parameter to the given file.
     * This method throws an UnimplementedException. Derived classes need to
     * overwrite this to write out the appropriate data
     *
     * @param what What is to be written out
     * @param filename To what file
     *
     * @throws Exception  problem exporting
     */
    public void doExport(String what, String filename) throws Exception {
        //Override if you want your display control to export to csv, etc.
    }



    /**
     * Save the state as parameter defaults
     */
    public void saveAsParameterDefaults() {
        getIdv().getParamDefaultsEditor().saveDefaults(this);
    }

    /**
     * Save this display control as a favorite display template.
     */
    public void saveAsFavorite() {
        controlContext.getPersistenceManager().saveDisplayControlFavorite(
            this, templateName);
    }

    /**
     * Save this display control as a display template.
     */
    public void saveAsTemplate() {
        controlContext.getPersistenceManager().saveDisplayControl(this);
    }

    /**
     * Save this display as a prototype (default)
     */
    public void saveAsPrototype() {
        getIdv().getPersistenceManager().writePrototype(this);
    }

    /**
     * Clear the prototype (default) for this display control
     */
    public void clearPrototype() {
        getIdv().getPersistenceManager().clearPrototype(getClass());
    }



    /**
     * Move the displayable to the front
     */
    public void displayableToFront() {
        try {
            List infos = getDisplayInfos();
            for (int i = 0; i < infos.size(); i++) {
                ((DisplayInfo) infos.get(i)).getDisplayable().toFront();
            }
            getIdv().toFront(this);
        } catch (Exception exc) {
            logException("Moving to front", exc);
        }
    }



    /**
     * Add the  relevant view menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        items.add(GuiUtils.makeCheckboxMenuItem("Visible", this,
                "displayVisibility", null));
        items.add(GuiUtils.makeCheckboxMenuItem("Lock Visibility", this,
                "lockVisibilityToggle", null));
        if (getDisplayInfos().size() > 0) {
            items.add(GuiUtils.setIcon(GuiUtils.makeMenuItem("Bring to Front",
                    this,
                    "displayableToFront"), "/auxdata/ui/icons/shape_move_front.png"));

            ViewManager vm = getViewManager();
            if (vm != null) {
                items.add(GuiUtils.makeMenuItem("Show View Window", vm,
                        "toFront"));
            }

        }



        if (haveDataTimes()) {
            JCheckBoxMenuItem jcmi =
                GuiUtils.makeCheckboxMenuItem("Use Times In Animation", this,
                    "useTimesInAnimation", null);
            if (getIdv().getUseTimeDriver()) {
                JMenu jm = new JMenu("Times");
                jm.add(jcmi);
                jm.add(GuiUtils.makeCheckboxMenuItem(
                    "Drive Times with this Display", this, "isTimeDriver",
                    null));
                jm.add(GuiUtils.makeCheckboxMenuItem(
                    "Uses Time Driver Times", this, "usesTimeDriver",
                    usesTimeDriver, null));
                items.add(jm);
            } else {
                items.add(jcmi);
            }
        }

        if (getDisplayInfos().size() > 0) {
            JMenu dlMenu = new JMenu("Display List");
            dlMenu.add(GuiUtils.makeCheckboxMenuItem("Show In Display List",
                    this, "showInDisplayList", null));
            dlMenu.add(
                GuiUtils.makeMenu(
                    "Display List Color",
                    makeChangeColorMenuItems(
                        "setDisplayListColor", displayListColor)));
            items.add(dlMenu);
        }

        if (hasMapProjection()) {
            items.add(GuiUtils.MENU_SEPARATOR);
            JMenuItem mi = new JMenuItem(getDataProjectionLabel());
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    setProjectionInView(false);
                }
            });
            items.add(mi);

            mi = new JMenuItem("Center on Display");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    centerOnDisplay();
                }
            });
            items.add(mi);
            if (canDoProgressiveResolution()) {
                items.add(
                    GuiUtils.makeCheckboxMenuItem(
                        MapViewManager.PR_LABEL, this,
                        "isProgressiveResolution", null));
                items.add(
                    GuiUtils.makeCheckboxMenuItem(
                        "Match Display Region", this, "matchDisplayRegion",
                        null));
            }
        }

    }



    /**
     * Get the data projection label
     *
     * @return  the data projection label
     */
    protected String getDataProjectionLabel() {
        return "Use Data Projection";
    }

    /**
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getFileMenuItems(List items, boolean forMenuBar) {
        items.add(GuiUtils.setIcon(GuiUtils.makeMenuItem("Remove Display",
                this, "doRemove"), "/auxdata/ui/icons/delete.png"));
    }

    /**
     * Save the data choice into the cache data source
     */
    public void saveDataChoiceInCache() {
        try {
            if ((dataInstances == null) || (dataInstances.size() != 1)) {
                return;
            }
            DataInstance dataInstance = (DataInstance) dataInstances.get(0);
            Data         data         = dataInstance.getData();
            getIdv().saveInCache(dataInstance.getDataChoice(), data,
                                 dataSelection);
        } catch (Exception exc) {
            logException("Saving data to cache", exc);
        }
    }


    /**
     * Get any extra menus for this control.  Subclasses should override
     * to add more menus
     *
     * @param menus   list of menus to populate
     * @param forMenuBar Is this for the menu in the window's menu bar or
     *                   for a popup menu in the legend
     */
    protected void getExtraMenus(List menus, boolean forMenuBar) {}

    /**
     * Add the help menu items
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getHelpMenuItems(List items, boolean forMenuBar) {
        items.add(GuiUtils.makeMenuItem("Details", this, "showDetails"));
        items.add(GuiUtils.setIcon(GuiUtils.makeMenuItem("User's Guide",
                this, "showHelp"), "/auxdata/ui/icons/help.png"));
    }


    /**
     * Popup the details window
     */
    public void showDetails() {
        if (detailsFrame == null) {
            Component[] comps = GuiUtils.showHtmlDialog(getLegendToolTip(),
                                    "Details for " + getTitle(),
                                    DisplayControlImpl.this);


            detailsFrame  = (Window) comps[0];
            detailsEditor = (JEditorPane) comps[1];
        } else {
            detailsFrame.setVisible(true);
            detailsEditor.setText(getLegendToolTip());
        }

    }

    /**
     * Return the list of (String) java help ids
     *
     * @return List of help ids
     */
    public List getHelpIds() {
        List helpIds = new ArrayList();
        if (helpUrl != null) {
            helpIds.add(helpUrl);
        }
        if (displayId != null) {
            helpIds.add("idv.controls." + displayId);
        }
        Class myClass = getClass();
        while ( !(myClass.equals(DisplayControlImpl.class))) {
            helpIds.add("idv.controls."
                        + Misc.getClassName(myClass).toLowerCase());
            myClass = myClass.getSuperclass();
        }
        return helpIds;
    }



    /**
     * This method will attempt to  show the relevant javahelp for
     * this display. It successively tries to show the helpUrl attribute,
     * the &quot;idv.controls.displayId&quot; and then each
     * &quot;idv.controls.classname&quot;
     * <p>
     * If there is no help available it will popup a message to the user.
     */
    public void showHelp() {
        if ( !Help.getDefaultHelp().gotoTarget(getHelpIds())) {
            userMessage("No help available for this display control");
        }
    }

    /**
     *  Does this control   have displays in a view manager
     *
     * @return Does this control   have displays in a view manager
     */
    public boolean isInViewManager() {
        if (displays == null) {
            return false;
        }
        return !(displays.isEmpty());
    }




    /**
     *  Find the first ViewManager in the list of DisplayInfo-s
     *  and have the ViewManager write its image to to given
     *  filename.
     *
     * @param filename The file to write the image to
     */
    public void saveImage(String filename) {
        List v = getDisplayInfos();
        if (v.size() >= 1) {
            DisplayInfo info = (DisplayInfo) v.get(0);
            info.getViewManager().writeImage(filename);
        }
    }




    /**
     *  Write out all screen images
     *
     * @param archiveName The name of the archive we are writing to
     */
    public void writeTestArchive(String archiveName) {
        try {
            archiveName = archiveName + "_" + displayId;
            String guiImageFile = archiveName + "_gui.png";
            toFront();
            Misc.sleep(200);
            System.err.println("Writing image:" + guiImageFile);
            ImageUtils.writeImageToFile(outerContents, guiImageFile);

            int displayCnt = 1;
            if (viewManagers != null) {
                for (int i = 0; i < viewManagers.size(); i++) {
                    String displayImageFile = archiveName + "_display_"
                                              + displayCnt + ".png";
                    displayCnt++;
                    System.err.println("Writing image:" + displayImageFile);
                    ViewManager viewManager =
                        ((ViewManager) viewManagers.get(i));
                    GuiUtils.showComponentInTabs(viewManager.getComponent());
                    viewManager.writeImage(displayImageFile, true);
                }
            }
            if (displayMasters != null) {
                toFront();
                for (int i = 0, n = displayMasters.size(); i < n; i++) {
                    String displayImageFile = archiveName + "_display_"
                                              + displayCnt + ".png";
                    displayCnt++;
                    System.err.println("Writing image:" + displayImageFile);
                    ((DisplayMaster) displayMasters.get(
                        i)).saveCurrentDisplay(
                            new File(displayImageFile), false, true);
                }
            }
        } catch (Exception exc) {
            logException("Writing image", exc);
        }
    }

    /**
     * Method to call if projection changes.  Subclasses that
     * are worried about such events should implement this.
     */
    public void projectionChanged() {
        reDisplayColorScales();
        try {
            applyZPosition();
        } catch (Exception exc) {
            logException("Applying z position", exc);
        }
        //System.out.println("projection changed");
        if (getMatchDisplayRegion()) {
            reloadFromBounds = true;
            lastBounds       = null;
            checkBoundsChange();
        }
    }

    /**
     * Method called when a transect  changes.
     */
    public void transectChanged() {}


    /**
     * Method called when a transect  changes.
     *
     * @param property The property that changed
     */
    public void viewManagerChanged(String property) {
        if (property.equals(MapViewManager.SHARE_PROJECTION)) {
            projectionChanged();
        } else if (property.equals(MapViewManager.PREF_PERSPECTIVEVIEW)
                   || property.equals(
                       MapViewManager.PROP_COMPONENT_RESIZED)) {
            reDisplayColorScales();
        }
        if (property.equals(NavigatedViewManager.SHARE_RUBBERBAND)) {
            reloadFromBounds = true;
        }
    }

    /**
     * Call redisplay on any color scales
     */
    private void reDisplayColorScales() {
        if ((colorScales != null) && !colorScales.isEmpty()) {
            for (int i = 0; i < colorScales.size(); i++) {
                ((ColorScale) colorScales.get(i)).reDisplay();
            }
        }
    }

    /**
     * Get the MapProjection for this data; if have a single point data object
     * make synthetic map projection for location
     * @return MapProjection  for the data
     */
    public MapProjection getDataProjectionForMenu() {
        return getDataProjection();
    }


    /**
     * get MapProjection of data to display
     *
     * @return The native projection of the data
     */
    public MapProjection getDataProjection() {

        MapProjection mp = null;
        List          v  = getDisplayInfos();

        try {
            // look at all the Displayables;
            // get the display's data, and not selector points or such
            for (int i = 0, n = v.size(); i < n; i++) {
                DisplayInfo info = (DisplayInfo) v.get(i);

                Data        data = info.getDisplayable().getData();
                if ((data != null) && (data instanceof FieldImpl)) {
                    try {
                        mp = GridUtil.getNavigation((FieldImpl) data);
                    } catch (Exception e) {
                        mp = null;
                    }
                    if (mp != null) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logException("Getting projection from data", e);
        }

        return mp;
    }


    /**
     * Get the center of the display
     *
     * @return center point or null if not a navigated display
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public LatLonPoint getDisplayCenter()
            throws RemoteException, VisADException {
        MapProjection mapProjection = getDataProjection();
        if (mapProjection != null) {
            return mapProjection.getCenterLatLon();
        }
        return null;
    }

    /**
     * Set the projection in the map view manager.
     *
     * @param useViewPreference  if true, will let the view decide if
     *                           preference to reset data is used or not
     */
    protected void setProjectionInView(boolean useViewPreference) {
        setProjectionInView(useViewPreference, false);
    }

    /**
     * Set the projection in the map view manager.
     *
     * @param useViewPreference  if true, will let the view decide if
     *                           preference to reset data is used or not
     * @param maintainViewpoint  keep the same viewpoint
     */
    protected void setProjectionInView(boolean useViewPreference,
                                       boolean maintainViewpoint) {
        MapViewManager mvm = getMapViewManager();
        if (mvm == null) {
            return;
        }
        MapProjection mp = (useViewPreference
                            ? getDataProjection()
                            : getDataProjectionForMenu());
        if (mp == null) {
            return;
        }
        mvm.setMapProjection(
            mp, true,
            getDisplayConventions().getMapProjectionLabel(mp, this),
            useViewPreference, true, maintainViewpoint);
    }




    /**
     * If this display has a dataprojection then center the view to it
     */
    protected void centerOnDisplay() {
        MapViewManager mvm = getMapViewManager();
        if (mvm == null) {
            return;
        }
        MapProjection mp = getDataProjection();
        if (mp == null) {
            return;
        }

        try {
            mvm.center(mp);
        } catch (Exception exc) {
            logException("Centering display", exc);
        }
    }

    /**
     * Get the projection from the main display.
     * @return map projection for this display's map view manager or null
     */
    public MapProjection getMapViewProjection() {
        MapViewManager mvm = getMapViewManager();
        if (mvm == null) {
            return null;
        }
        return mvm.getMainProjection();
    }


    /**
     *  Called after everything has been initialized. Iterates across all of
     *  the DisplayInfo-s, telling them to add their Displayable to their
     *  ViewManager.
     *
     * @return Was the insertion successful
     */
    private boolean insertDisplayables() {
        try {
            List v = getDisplayInfos();
            //Tell each of my displayInfo's to add themselves to their viewManger
            boolean addOk = true;
            Hashtable<ViewManager, List<DisplayInfo>> vmMap =
                new Hashtable<ViewManager, List<DisplayInfo>>();
            List<ViewManager> vms = new ArrayList<ViewManager>();
            for (int i = 0, n = v.size(); i < n; i++) {
                DisplayInfo info = (DisplayInfo) v.get(i);
                ViewManager vm   = info.getViewManager();
                if (vm == null) {
                    continue;
                }
                List<DisplayInfo> infos = vmMap.get(vm);
                if (infos == null) {
                    vmMap.put(vm, infos = new ArrayList<DisplayInfo>());
                    vms.add(vm);
                }
                infos.add(info);
            }
            for (ViewManager vm : vms) {
                List<DisplayInfo> infos = vmMap.get(vm);
                vm.addDisplayInfos(infos);
            }

            for (int i = 0, n = v.size(); i < n; i++) {
                DisplayInfo info = (DisplayInfo) v.get(i);
                if ( !info.getDisplayableAdded()) {
                    removeDisplayInfo(info);
                    addOk = false;
                }
            }
            if ( !addOk) {
                doRemove();
                return false;
            }
            activateDisplay(v);
        } catch (Exception exc) {
            logException("Inserting displayables", exc);
        }
        return true;
    }

    /**
     * Activate each DisplayInfo  in the given list.
     *
     * @param displayList List of {@link ucar.unidata.idv.DisplayInfo}s
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    private void activateDisplay(List displayList)
            throws RemoteException, VisADException {
        for (int i = 0, n = displayList.size(); i < n; i++) {
            DisplayInfo info = (DisplayInfo) displayList.get(i);
            info.activateDisplay();
        }
    }


    /**
     * Activate each DisplayInfo
     *
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void activateDisplays() throws RemoteException, VisADException {
        //        Trace.call1("DisplayControlImpl.activateDisplays");
        activateDisplay(getDisplayInfos());
        //        Trace.call2("DisplayControlImpl.activateDisplays");
    }



    /**
     * DeActivate each DisplayInfo
     *
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void deactivateDisplays()
            throws RemoteException, VisADException {
        //        Trace.call1("DisplayControlImpl.deactivateDisplays");
        List displayList = getDisplayInfos();
        for (int i = 0, n = displayList.size(); i < n; i++) {
            DisplayInfo info = (DisplayInfo) displayList.get(i);
            info.deactivateDisplay();
        }
        //        Trace.call2("DisplayControlImpl.deactivateDisplays");
    }

    /**
     * Find the DisplayInfo for a displayable
     *
     * @param displayable  the displayable to search for
     *
     * @return the associated <code>DisplayInfo</code>
     */
    protected DisplayInfo findDisplayInfo(Displayable displayable) {
        List displayList = getDisplayInfos();
        if (displayList == null) {
            return null;
        }
        for (int i = 0; i < displayList.size(); i++) {
            DisplayInfo info = (DisplayInfo) displayList.get(i);
            if (info.getDisplayable() == displayable) {
                return info;
            }
        }
        return null;

    }


    /**
     * Remove a <code>Displayable</code>
     *
     * @param displayable   displayable to remove
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    Problem in VisAD
     */
    public void removeDisplayable(Displayable displayable)
            throws RemoteException, VisADException {
        DisplayInfo info = findDisplayInfo(displayable);
        if (info != null) {
            removeDisplayInfo(info);
        }
    }


    /**
     * Iterates across the list of {@link ucar.unidata.idv.DisplayInfo}-s, telling them to
     * removeDisplayable.
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void removeDisplayables()
            throws RemoteException, VisADException {
        removeDisplayables(false);
    }

    /**
     * Iterates across the list of {@link ucar.unidata.idv.DisplayInfo}-s, telling them to
     * removeDisplayable.
     *
     *
     * @param andDestroyThem  true to destroy them
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */

    protected void removeDisplayables(boolean andDestroyThem)
            throws RemoteException, VisADException {
        List displayList = getDisplayInfos();
        displays = new ArrayList();
        for (int i = 0, n = displayList.size(); i < n; i++) {
            DisplayInfo info        = (DisplayInfo) displayList.get(i);
            Displayable displayable = info.getDisplayable();
            if (displayable != null) {
                displayable.removePropertyChangeListener(this);
            }
            info.removeDisplayable();
            if ((displayable != null) && andDestroyThem) {
                displayable.destroyDisplayable();
            }
        }
    }


    /**
     * Remove the given display info from the list of display infos
     * and remove the Displayable it holds from the Display.
     *
     * @param info The info to remove
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    private void removeDisplayInfo(DisplayInfo info)
            throws RemoteException, VisADException {
        displays.remove(info);
        if (info.getDisplayable() != null) {
            info.getDisplayable().removePropertyChangeListener(this);
        }
        info.removeDisplayable();
    }


    /**
     * Called when a ViewManager which holds the display is destoryed
     *
     *
     * @param viewManager The view manager that was destroyed
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void viewManagerDestroyed(ViewManager viewManager)
            throws VisADException, RemoteException {
        doRemove();
    }



    /**
     * Add something to be removed on doRemove
     *
     * @param removable  the removeable
     */
    public void addRemovable(Removable removable) {
        removables.add(removable);
    }



    /**
     *  Remove this DisplayControl. Tells the {@link ucar.unidata.idv.ControlContext}
     *  to removeDisplayControl.
     *  Removes all Displayable-s from
     *  their ViewManager-s, remove this object from its  Sharable
     *  group, and sets the visibility of the dialog window to false.
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    public void doRemove() throws RemoteException, VisADException {

        if (hasBeenRemoved) {
            return;
        }
        hasBeenRemoved = true;


        if (componentHolder != null) {
            componentHolder.removeDisplayControl(this);
            componentHolder = null;
        }

        if (detailsFrame != null) {
            detailsFrame.dispose();
            detailsFrame = null;
        }

        firePropertyChangeEvent(new PropertyChangeEvent(this, PROP_REMOVED,
                this, null));
        propertyChangeListeners = null;

        if (displayControlListeningTo != null) {
            displayControlListeningTo.removeDisplayListener(this);
            displayControlListeningTo = null;
        }


        if (projectionControlListeningTo != null) {
            projectionControlListeningTo.removeControlListener(this);
            projectionControlListeningTo = null;
        }




        if ((myWindow != null) && (windowListener != null)) {
            myWindow.removeWindowListener(windowListener);
        }

        if (viewAnimation != null) {
            viewAnimation.removePropertyChangeListener(this);
            viewAnimation = null;
        }


        if (internalAnimation != null) {
            internalAnimation.removePropertyChangeListener(this);
            internalAnimation = null;
        }


        displayUnit = null;
        colorUnit   = null;
        getControlContext().removeDisplayControl(this);
        removeDisplayables(true);
        disposeOfWindow();
        removeSharable();

        if (animationWidget != null) {
            animationWidget.destroy();
            animationWidget = null;
        }


        if (sharables != null) {
            for (int i = 0, n = sharables.size(); i < n; i++) {
                ((SharableImpl) sharables.get(i)).removeSharable();
            }
            sharables = null;
        }


        for (Removable removable : removables) {
            removable.doRemove();
        }
        removables = null;


        if (displayMasters != null) {
            for (int i = 0, n = displayMasters.size(); i < n; i++) {
                ((DisplayMaster) displayMasters.get(i)).destroy();
            }
            displayMasters = null;
        }
        clearViewManagers();
        defaultViewManager = null;
        removeListenerFromDataChoices();
        if (contents != null) {
            //Don't do this for now:
            //GuiUtils.empty(contents);
            contents = null;
        }

        if (outerContents != null) {
            //Don't do this for now:
            //            GuiUtils.empty(outerContents);
            outerContents = null;
        }


        //Just to be on the safe side (for memory leaks) 
        //null out all references that we have.
        bottomLegendComponent = null;
        mainPanel             = null;
        myDataChoices         = null;
        dataInstances         = null;
        initDataChoices       = null;
        dataSelection         = null;
        displays              = null;
        displayListTable.clear();
        displayListTable  = null;
        ctw               = null;
        selectRangeWidget = null;
        contourWidget     = null;
        lww               = null;
        sww               = null;
    }


    /**
     * Add this control as a {@link ucar.unidata.data.DataChangeListener}
     * to the {@link ucar.unidata.data.DataChoice}s in the given list
     *
     * @param choices List of data choices.
     */
    private void addListenerToDataChoices(List choices) {
        if (choices == null) {
            return;
        }
        List tmp = new ArrayList(choices);
        for (int i = 0; i < tmp.size(); i++) {
            ((DataChoice) tmp.get(i)).addDataChangeListener(this);
        }
    }

    /**
     * Remove this control as a {@link ucar.unidata.data.DataChangeListener}
     * from the {@link ucar.unidata.data.DataChoice}s in the myDataChoices list.
     */
    private void removeListenerFromDataChoices() {
        if (myDataChoices == null) {
            return;
        }
        List tmp = new ArrayList(myDataChoices);
        for (int i = 0; i < tmp.size(); i++) {
            ((DataChoice) tmp.get(i)).removeDataChangeListener(this);
        }
    }


    /**
     *  Is this control active, i.e., has this control not gotten removed yet
     *
     * @return Is active
     */
    public boolean getActive() {
        return !hasBeenRemoved;
    }


    /**
     * Make Gui contents
     *
     * @return User interface contents
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        return GuiUtils.wrap(doMakeWidgetComponent());
    }  // end doMakeContents



    /**
     * Set the gui contents
     *
     * @param c The user interface contents
     */
    protected void setContents(Container c) {
        contents = c;
    }

    /**
     * Get the GUI contents
     *
     * @return the contents
     */
    protected Container getContents() {
        return contents;
    }


    /**
     * Reset the view manager
     *
     * @param oldViewId old view id
     * @param newViewId new view id
     */
    public void resetViewManager(String oldViewId, String newViewId) {
        if (Misc.equals(oldViewId, defaultView)) {
            defaultView = newViewId;
        }
    }


    /**
     * Set the name of the default view manager that displays are put into.
     *
     * @param s The default view
     */
    public void setDefaultView(String s) {
        defaultView = s;
    }

    /**
     * Get the list of items for the cursor readout
     *
     * @param el   location of cursor
     * @param animationValue  animation value
     * @param animationStep  animation step
     * @param samples The list of samples returned
     *
     * @return list of strings for readout
     *
     * @throws Exception  problem getting at the data
     */
    public final List getCursorReadout(EarthLocation el, Real animationValue,
                                       int animationStep,
                                       List<ReadoutInfo> samples)
            throws Exception {
        if ( !getDoCursorReadout()) {
            return null;
        }
        try {
            List l = getCursorReadoutInner(el, animationValue, animationStep,
                                           samples);
            return l;
        } catch (Exception exc) {
            LogUtil.consoleMessage("Error getting cursor readout");
            LogUtil.consoleMessage(LogUtil.getStackTrace(exc));
            setDoCursorReadout(false);
        }
        return null;
    }

    /**
     * Get the list of items, subclasses should override
     *
     * @param el   location of cursor
     * @param animationValue  animation value
     * @param animationStep  animation step
     * @param samples The list of samples returned
     *
     * @return list of strings for readout
     *
     * @throws Exception  problem getting at the data
     */
    protected List getCursorReadoutInner(EarthLocation el,
                                         Real animationValue,
                                         int animationStep,
                                         List<ReadoutInfo> samples)
            throws Exception {
        return null;
    }

    /**
     * The getCursorReadout method that really does the work
     *
     * @param el  the location
     * @param animationValue the animation value
     * @param animationStep  the animation step
     *
     * @return the list of readout strings
     */
    protected final List getCursorReadoutInner(EarthLocation el,
            Real animationValue, int animationStep) {
        return null;
    }


    /**
     * Format a real for the cursor readout
     *
     * @param r  the real
     *
     * @return  the formatted string
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException  VisAD error
     */
    protected String formatForCursorReadout(Real r)
            throws VisADException, RemoteException {
        Unit   displayUnit = getDisplayUnit();
        double value;
        Unit   unit;
        String result;
        if (r.isMissing()) {
            result = "missing";
        } else {
            if (displayUnit != null) {
                value = r.getValue(displayUnit);
                unit  = displayUnit;
            } else {
                value = r.getValue();
                unit  = r.getUnit();
            }
            result = Misc.format(value);
            result = result + "[" + unit + "]";
            int length = result.length();
            result = StringUtil.padLeft(result, 8 * (20 - length), "&nbsp;");
        }

        return result;
    }




    /**
     * Set the view manager for this control to use.
     * Note: This is only for use when some code is directly
     * creating a display control and wants to place it in a specific viewmanager.
     * Use this after the control has been created but before the init method has been called
     *
     * @param viewManager The viewmanager to use
     */
    public void setInitialViewManager(ViewManager viewManager) {
        defaultViewManager = viewManager;
    }


    /**
     * Return the name  of the first {@link ucar.unidata.idv.ViewManager} found
     * in the list of {@link ucar.unidata.idv.DisplayInfo}-s.
     *
     * @return The name of the default view
     */
    public String getDefaultView() {
        ViewManager vm = getDefaultViewManager();
        if ( !controlContext.getPersistenceManager().getSaveViewState()
                && !controlContext.getPersistenceManager()
                    .getSaveDataSources() && !controlContext
                    .getPersistenceManager().getSaveData() && !controlContext
                    .getPersistenceManager().getSaveJython()) {
            // this block is for the display template
            return null;
        }
        if ((vm != null) && (vm.getViewDescriptor() != null)) {
            return vm.getViewDescriptor().getName();
        }
        return null;
    }


    /**
     * Return the name  of the first {@link ucar.unidata.idv.ViewManager} found
     * in the list of {@link ucar.unidata.idv.DisplayInfo}-s.
     *
     * @return The name of the default view
     */
    public ViewManager getDefaultViewManager() {
        if (displays == null) {
            return null;
        }
        for (int i = 0, n = displays.size(); i < n; i++) {
            DisplayInfo info = (DisplayInfo) displays.get(i);
            //Grab the first view manager in the list
            ViewManager vm = info.getViewManager();
            if ((viewManagers != null) && viewManagers.contains(vm)) {
                continue;
            }
            return vm;

        }
        return null;
    }



    /**
     * Return the  {@link ucar.unidata.idv.ViewDescriptor} to use.
     * This allows the IDV to find a ViewManager identified by this descriptor
     *
     * @return The view descriptor
     */
    public ViewDescriptor getDefaultViewDescriptor() {
        ViewDescriptor vd;
        if (defaultView == null) {
            vd = new ViewDescriptor(ViewDescriptor.LASTACTIVE);
        } else {
            vd = new ViewDescriptor(defaultView);
        }

        if (viewManagerClassNames != null) {
            vd.setClassNames(StringUtil.split(viewManagerClassNames, ",",
                    true, true));
        }
        return vd;
    }

    /**
     * Add the given {@link ucar.visad.display.Displayable} into a
     * {@link ucar.unidata.idv.ViewManager}
     *
     * @param d The displayable to add
     * @return The {@link ucar.unidata.idv.ViewManager} this Displayable
     *         is added into
     */
    public ViewManager addDisplayable(Displayable d) {
        return addDisplayable(d, getDefaultViewDescriptor());
    }


    /**
     *  A wrapper around
     *  addDisplayable (Displayable theDisplay, ViewDescriptor viewDescriptor),
     *  passing in the default LASTACTIVE ViewDescriptor.
     *
     * @param theDisplay The {@link ucar.visad.display.Displayable} to add
     * @param attributeFlag The attribute flag (e.g, FLAG_COLOR|FLAG_COLORTABLE)
     * @return The {@link ucar.unidata.idv.ViewManager} this Displayable
     *         is added into
     */

    public ViewManager addDisplayable(Displayable theDisplay,
                                      int attributeFlag) {

        ViewManager vm = addDisplayable(theDisplay,
                                        getDefaultViewDescriptor());
        addAttributedDisplayable(theDisplay, attributeFlag);
        return vm;
    }


    /**
     * Add the given {@link ucar.visad.display.Displayable} into the given
     * {@link ucar.unidata.idv.ViewManager}
     *
     * @param theDisplay The displayable to add
     * @param viewManager The ViewManager
     * @param flag The attribute flag (e.g., FLAG_COLORTABLE) for this Displayable
     * @return The {@link ucar.unidata.idv.ViewManager} this Displayable is
     *          added into
     */
    public ViewManager addDisplayable(Displayable theDisplay,
                                      ViewManager viewManager, int flag) {
        addDisplayable(theDisplay, viewManager);
        addAttributedDisplayable(theDisplay, flag);
        return viewManager;
    }

    /**
     * Add the given {@link ucar.visad.display.Displayable} into the
     * {@link ucar.unidata.idv.ViewManager} identified by
     * the given {@link ucar.unidata.idv.ViewDescriptor}
     *
     * @param theDisplay The displayable to add
     * @param viewDescriptor The descriptor
     * @param flag The attribute flag (e.g., FLAG_COLORTABLE) for this Displayable
     * @return The {@link ucar.unidata.idv.ViewManager} this Displayable is added into
     */

    public ViewManager addDisplayable(Displayable theDisplay,
                                      ViewDescriptor viewDescriptor,
                                      int flag) {
        ViewManager vm = addDisplayable(theDisplay, viewDescriptor);
        addAttributedDisplayable(theDisplay, flag);
        return vm;
    }


    /**
     *  Find the {@link ucar.unidata.idv.ViewManager} defined
     *  by the given {@link ucar.unidata.idv.ViewDescriptor}.
     *  Create a new {@link ucar.unidata.idv.DisplayInfo}
     *  that holds this DisplayControl, the given
     *  {@link ucar.visad.display.Displayable} and the
     * looked up ViewManager. Add the DisplayInfo
     *  to the list of DisplayInfo-s
     *
     * @param theDisplay The displayable to add
     * @param viewDescriptor Describes the ViewManager in which to add the Displayable
     * @return The ViewManager
     */
    public ViewManager addDisplayable(Displayable theDisplay,
                                      ViewDescriptor viewDescriptor) {
        ViewManager viewManager = getViewManager(viewDescriptor);
        addDisplayable(theDisplay, viewManager);
        return viewManager;
    }



    /**
     * Add the given {@link ucar.visad.display.Displayable} into the
     * given {@link ucar.unidata.idv.ViewManager}
     *
     * @param theDisplay The Displayable to add
     * @param viewManager The ViewManager in which the display is added
     * @return  the DisplayInfo for the displayable.
     */
    public DisplayInfo addDisplayable(Displayable theDisplay,
                                      ViewManager viewManager) {

        if (haveInitialized && (defaultView == null)) {
            //            defaultView = viewManager.getName();
        }
        try {
            theDisplay.setUseFastRendering(
                shouldApplyFastRendering()
                && viewManager.getUseFastRendering(useFastRendering));
            theDisplay.setUseTimesInAnimation(useTimesInAnimation);
        } catch (Exception exc) {
            logException("Setting fast rendering:" + useFastRendering, exc);
        }

        DisplayInfo info = new DisplayInfo(this, viewManager, theDisplay);

        displays.add(info);
        //If we added this displayable after we have been initialized
        //the tell the displayInfo to add itself to the ViewManager.
        if (haveInitialized) {
            try {
                //                Trace.call1("addDisplayable");
                info.addDisplayable();
                //                Trace.call2("addDisplayable");
                if ( !info.getDisplayableAdded()) {
                    //                    Trace.msg("removeDisplayInfo");
                    removeDisplayInfo(info);
                } else {
                    //                    Trace.call1("activateDisplay");
                    info.activateDisplay();
                    //                    Trace.call2("activateDisplay");
                }
            } catch (Exception exc) {
                logException("Adding displayables", exc);
            }
        }
        return info;
    }




    /**
     *  Return the list of {@link ucar.unidata.idv.DisplayInfo}
     * objects held by this control.
     *
     * @return List of display infos
     */
    public List getDisplayInfos() {
        if (displays == null) {
            return new ArrayList();
        }
        return new ArrayList(displays);
    }




    /**
     * A helper method for constructing the ui.
     * This fills up a list of {@link ControlWidget}
     * (e.g., ColorTableWidget) and creates a gridded
     * ui  with them.
     *
     * @return The ui for the widgets
     */
    protected JComponent doMakeWidgetComponent() {
        try {
            List controlWidgets = new ArrayList();
            getControlWidgets(controlWidgets);
            List widgetComponents = ControlWidget.fillList(controlWidgets);
            GuiUtils.tmpInsets = new Insets(4, 8, 4, 8);
            GuiUtils.tmpFill   = GridBagConstraints.HORIZONTAL;
            JPanel p = GuiUtils.doLayout(widgetComponents, 2, GuiUtils.WT_NY,
                                         GuiUtils.WT_N);
            return p;
        } catch (Exception exc) {
            logException("Making the widget component", exc);
            return new JLabel("Error");
        }
    }

    /**
     * The z slider postion changed
     *
     * @param value slider value
     */
    public void zSliderChanged(double value) {
        try {
            setZPosition(value);
        } catch (Exception exc) {
            logException("Setting z position", exc);
        }

    }


    /**
     * Make a slider for the texture quality
     *
     * @return the slider
     */
    protected JSlider doMakeTextureSlider() {
        if (textureSlider == null) {
            textureSlider = GuiUtils.makeSlider(1, 21, textureQuality, this,
                    "setTextureQuality");
            Hashtable labels = new Hashtable();
            labels.put(new Integer(1), GuiUtils.lLabel("High"));
            labels.put(new Integer(10), GuiUtils.cLabel("Medium"));
            labels.put(new Integer(21), GuiUtils.rLabel("Low"));
            textureSlider.setLabelTable(labels);
            textureSlider.setPaintLabels(true);
        }
        return textureSlider;
    }



    /**
     * Create the z position slider panel
     *
     * @return The panel that shows the z position  slider
     */
    protected JComponent doMakeZPositionSlider() {
        int sliderPos = (int) (getZPosition() * 100);


        zPositionSlider = new ZSlider(getZPosition()) {
            public void valueHasBeenSet() {
                zSliderChanged(getValue());
            }
        };
        return zPositionSlider.getContents();

        /*
        int min       = -100;
        int max       = 100;
        sliderPos = Math.min(Math.max(sliderPos, min), max);
        zPositionSlider = GuiUtils.makeSlider(min, max, sliderPos, this,
                "zSliderChanged");
        JPanel labelPanel =
            GuiUtils.leftCenterRight(GuiUtils.lLabel("Bottom"),
                                     GuiUtils.cLabel("Middle"),
                                     GuiUtils.rLabel("Top"));


        return GuiUtils.vbox(zPositionSlider, labelPanel);
        */
    }

    /**
     * Get the label for the Z position slider.
     * @return  label
     */
    protected String getZPositionSliderLabel() {
        return "Vertical Position:";
    }

    /**
     * Add into the given the  widgets  for the different attributes
     *
     * @param controlWidgets List of {@link ControlWidget}s to add into
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    public void getControlWidgets(List<ControlWidget> controlWidgets)
            throws VisADException, RemoteException {
        if (checkFlag(FLAG_CONTOUR)) {
            controlWidgets.add(contourWidget = new ContourWidget(this,
                    getContourInfo()));
            addRemovable(contourWidget);
        }
        if (checkFlag(FLAG_COLORTABLE)) {
            controlWidgets.add(getColorTableWidget(getRangeForColorTable()));
        }
        if (checkFlag(FLAG_ZPOSITION) && useZPosition()) {
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel(getZPositionSliderLabel()),
                    doMakeZPositionSlider()));
        }

        if (checkFlag(FLAG_COLOR) && showColorControlWidget()) {
            if (color == null) {
                color = getDisplayConventions().getColor();
            }
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel(getColorWidgetLabel() + ":"),
                    GuiUtils.left(doMakeColorControl(color))));
        }
        if (checkFlag(FLAG_SELECTRANGE)) {
            controlWidgets.add(getSelectRangeWidget(getSelectRange()));
        }


        if (checkFlag(FLAG_TIMERANGE)) {
            addTimeModeWidget(controlWidgets);
        }

        if (checkFlag(FLAG_LINEWIDTH)) {
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel(getLineWidthWidgetLabel() + ":"),
                    getLineWidthWidget().getContents(false)));
        }

        if (checkFlag(FLAG_SKIPFACTOR)) {
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel(getSkipWidgetLabel() + ":"),
                    doMakeSkipFactorSlider()));
        }

        if (checkFlag(FLAG_TEXTUREQUALITY)) {
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel(getTextureQualityLabel() + ":"),
                    doMakeTextureSlider()));
        }

        if (checkFlag(FLAG_SMOOTHING)) {
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Smoothing:"), doMakeSmoothingWidget()));
        }

    }

    /**
     * Add in the time mode widget to the list of control widgets
     *
     * @param controlWidgets List to add to
     */
    protected void addTimeModeWidget(List controlWidgets) {
        JPanel timeModePanel =
            GuiUtils.leftCenter(
                GuiUtils.wrap(
                    GuiUtils.makeImageButton(
                        "/auxdata/ui/icons/calendar_edit.png", this,
                        "showTimeRangeDialog")), GuiUtils.inset(
                            getDataTimeRange(true).getTimeModeLabel(),
                            new Insets(0, 10, 0, 0)));
        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Time Mode:"),
                                             timeModePanel));

    }


    /**
     * Return the label that is to be used for the color widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the color widget
     */
    public String getColorWidgetLabel() {
        return "Selector Color";
    }

    /**
     * Return the label that is to be used for the line width widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the line width widget
     */
    public String getLineWidthWidgetLabel() {
        return "Line Width";
    }


    /**
     * Return the label that is to be used for the skip widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the line width widget
     */
    public String getSkipWidgetLabel() {
        return "Skip";
    }


    /**
     * Allows for derived classes to define that the display unit
     * is different then the color unit. This is used  when we are
     * setting the display and color units.
     *
     * @return Color unit same as display unit?
     */
    protected boolean isDisplayUnitAlsoColorUnit() {
        return true;
    }


    /**
     * The user has chosen a new unit for display.
     *
     * @param newUnit The new  display unit
     * @param applyToDisplayable Should we call  applyDisplayUnit();
     * @return Did this succeed
     */
    protected boolean setNewDisplayUnit(Unit newUnit,
                                        boolean applyToDisplayable) {
        if (newUnit == null) {
            return true;
        }


        //If we haven't initialized yet just set the unit and return
        if ( !getHaveInitialized()) {
            /*            if (isDisplayUnitAlsoColorUnit()) {
                if ( !setNewColorUnit(newUnit, false)) {
                    return false;
                }
                }*/
            setDisplayUnit(newUnit);
            return true;
        }

        Unit oldUnit = getDisplayUnit();
        try {
            //Do this first because it uses displayUnit as the old unit
            if (isDisplayUnitAlsoColorUnit()) {
                if ( !setNewColorUnit(newUnit, false)) {
                    return false;
                }
            }
            setDisplayUnit(newUnit);
            ContourInfo contourInfo = getContourInfo();
            if ((contourInfo != null) && (oldUnit != null)) {

                ContourInfo newContourInfo = new ContourInfo(contourInfo);

                //New interval setting code:
                //Try to preserve how many lines there were between min and max
                double oldRange = newContourInfo.getMax()
                                  - newContourInfo.getMin();
                int howMany = ((newContourInfo.getInterval() != 0.0)
                               ? (int) (oldRange
                                        / newContourInfo.getInterval())
                               : 0);
                //                newContourInfo.setInterval(
                //                    (float) newUnit.toThis(
                //                        newContourInfo.getInterval(), oldUnit));

                newContourInfo.setBase(
                    (float) newUnit.toThis(
                        newContourInfo.getBase(), oldUnit));
                newContourInfo.setMin(
                    (float) newUnit.toThis(newContourInfo.getMin(), oldUnit));
                newContourInfo.setMax(
                    (float) newUnit.toThis(newContourInfo.getMax(), oldUnit));



                //New interval setting code:
                double newRange = newContourInfo.getMax()
                                  - newContourInfo.getMin();
                if (howMany > 0) {
                    newContourInfo.setInterval((float) (newRange / howMany));
                } else {
                    newContourInfo.setInterval(
                        (float) newUnit.toThis(
                            newContourInfo.getInterval(), oldUnit));
                }



                setContourInfo(newContourInfo);
            }
            if (applyToDisplayable) {
                applyDisplayUnit();
            }
            updateListOrLegendWithMacro(MACRO_DISPLAYUNIT);

            displayUnitChanged(oldUnit, newUnit);
            if ( !applyProperties()) {
                return false;
            }
        } catch (Exception exc) {
            //logException ("Error setting unit from: " + oldUnit + " to: " + newUnit + "\n", exc);
            userMessage("Error setting unit from: " + oldUnit + " to: "
                        + newUnit + "\n" + exc);
            setDisplayUnit(oldUnit);
            return false;
        }
        return true;
    }

    /**
     * If the display list or legend templates contain <code>macro</code>
     * update the appropriate UI component
     *
     * @param macro  the macro to check for
     */
    private void updateListOrLegendWithMacro(String macro) {
        boolean listUpdate = getDisplayListTemplate().indexOf(macro) >= 0;
        boolean legendUpdate =
            ((getLegendLabelTemplate().indexOf(macro) >= 0)
             || (getExtraLabelTemplate().indexOf(macro) >= 0));
        if (legendUpdate && listUpdate) {
            updateLegendAndList();
        } else if (listUpdate) {
            updateDisplayList();
        } else if (legendUpdate) {
            updateLegendLabel();
        }
    }

    /**
     * The user has chosen a new unit for color.
     *
     * @param newUnit The new color unit
     * @param applyToDisplayable Apply this to the Displayables?
     * @return Return true if there was no error, false if there was an error
     */
    protected boolean setNewColorUnit(Unit newUnit,
                                      boolean applyToDisplayable) {
        if (newUnit == null) {
            return true;
        }
        //        System.err.println ("setNewColorUnit:" + getHaveInitialized());
        if ( !getHaveInitialized()) {
            setUnitForColor(newUnit);
            return true;
        }

        Unit oldUnit = getUnitForColor();
        try {
            setUnitForColor(newUnit);
            Range currentRange = getRange();
            if (currentRange != null) {
                Range newRange;
                if (oldUnit != null) {
                    newRange =
                        new Range(newUnit.toThis(currentRange.getMin(),
                            oldUnit), newUnit.toThis(currentRange.getMax(),
                                oldUnit));
                } else {
                    newRange = getInitialRange();
                }
                if (newRange != null) {
                    setRange(newRange);
                }
            }
            if (applyToDisplayable) {
                applyColorUnit();
            }
            colorUnitChanged(oldUnit, newUnit);
        } catch (Exception exc) {
            setUnitForColor(oldUnit);
            //      logException ("Error setting unit from: " + oldUnit + " to: " + newUnit + "\n", exc);
            userMessage("Error setting unit: " + exc);
            return false;
        }
        return true;
    }




    /**
     * Returns the default display unit to be used. The
     * {@link ucar.unidata.idv.DisplayConventions} class
     * is used to fidn out what unit to use.
     *
     * @param rawUnit The raw data unit
     * @return The default Unit to use for displays
     */
    protected Unit getDisplayUnit(Unit rawUnit) {
        if (displayUnit == null) {
            displayUnit = getDisplayConventions().getDisplayUnit(paramName,
                    rawUnit);
            /*
                   System.err.println ("getDisplayUnit:" + displayUnit + " -- " +
                   rawUnit +" -- " +
                   paramName);
            */
        }
        return displayUnit;
    }



    /**
     * Return the display unit
     *
     * @return The display unit
     */
    public Unit getDisplayUnit() {
        return displayUnit;
    }

    /**
     * Set the display unit from the settings
     *
     * @param newUnit  the new unit
     */
    public void setSettingsDisplayUnit(Unit newUnit) {
        setNewDisplayUnit(newUnit, true);
    }


    /**
     * Set the display unit. This is protected so the display unit itself is not
     * persisted.
     *
     *
     * @param newUnit The new display unit
     */
    protected void setDisplayUnit(Unit newUnit) {
        displayUnit = newUnit;
    }

    /**
     * Get the name of the display unit (if non-null)
     * This is used for xml persistence.
     *
     * @return Name of display unit
     */
    public String getDisplayUnitName() {
        return ((displayUnit == null)
                ? null
                : displayUnit.toString());
    }


    /**
     * Set the name of the display unit. Try to
     * create the actual displayUnit based on this name
     * This is used for xml persistence.
     *
     * @param name The name to use
     */
    public void setDisplayUnitName(String name) {
        if (name == null) {
            displayUnit = null;
        } else {
            try {
                displayUnit = ucar.visad.Util.parseUnit(name);
            } catch (Exception exc) {}
        }
    }

    /**
     * Get the default distance unit
     *
     * @return the default distance unit from the IDV preferences
     */
    protected Unit getDefaultDistanceUnit() {
        return controlContext.getPreferenceManager().getDefaultDistanceUnit();
    }


    /**
     * Set the unit to use for color. If the display unit is
     * also the color unit (as determined by a call to
     * isDisplayUnitAlsoColorUnit()) then also set the display unit.
     *
     * @param newUnit The new color unit
     */
    protected void setUnitForColor(Unit newUnit) {
        if (isDisplayUnitAlsoColorUnit()) {
            displayUnit = newUnit;
            colorUnit   = newUnit;
        } else {
            colorUnit = newUnit;
        }
    }


    /**
     * Get the unit used for coloring the displays. If the
     * colorUnit member is null and the color unit is the
     * same as the displayUnit then return the displayUnit.
     *
     * @return The color unit
     */
    protected Unit getUnitForColor() {
        if ((colorUnit == null) && isDisplayUnitAlsoColorUnit()) {
            return displayUnit;
        }
        return colorUnit;
    }


    /**
     * Return the colorUnit
     * The get and set methods here are protected so the XmlEncoder
     * will not try to encode them.
     *
     * @return The color unit
     */
    protected Unit getColorUnit() {
        return colorUnit;
    }

    /**
     * Set the colorUnit
     *
     * @param unit The color unit
     */
    protected void setColorUnit(Unit unit) {
        colorUnit = unit;
    }

    /**
     * This is used for xml persistence.
     *
     * @return Name of the colorUnit
     */
    public String getColorUnitName() {
        return ((colorUnit == null)
                ? null
                : colorUnit.toString());
    }

    /**
     * This is used for xml persistence.
     *
     * @param name The name of the colorUnit. Try to create the unit.
     */
    public void setColorUnitName(String name) {
        if (name == null) {
            colorUnit = null;
        } else {
            try {
                colorUnit = ucar.visad.Util.parseUnit(name);
            } catch (Exception exc) {}
        }
    }

    /**
     * Popup the time range dialog
     */
    public void showTimeRangeDialog() {
        if ( !getDataTimeRange(true).showDialog()) {
            return;
        }
        Misc.run(this, "applyTimeRange");
    }

    /**
     * Called when we have set the time range. Allows derived classes to
     * do their thang.
     */
    public void applyTimeRange() {}

    /**
     * Create and show the data choosing dialog
     *
     * @param dialogMessage The message to display
     * @param from What component clicked
     */
    protected void popupDataDialog(final String dialogMessage,
                                   Component from) {
        popupDataDialog(dialogMessage, from, false);
    }


    /**
     * Popup a DataTreeDialog
     *
     * @param dialogMessage message for the dialog
     * @param from   the component that it is on
     * @param multiples can handle multiple selections
     */
    protected void popupDataDialog(final String dialogMessage,
                                   Component from, boolean multiples) {
        popupDataDialog(dialogMessage, from, multiples, categories);
    }

    /**
     * Popup a DataTreeDialog
     *
     * @param dialogMessage message for the dialog
     * @param from   the component that it is on
     * @param multiples can handle multiple selections
     * @param categories List of data categories. If
     */
    protected void popupDataDialog(final String dialogMessage,
                                   Component from, boolean multiples,
                                   List categories) {

        List<DataChoice> choices = selectDataChoices(dialogMessage, from,
                                       multiples, categories);
        if ((choices == null) || (choices.size() == 0)) {
            return;
        }
        final List clonedList =
            DataChoice.cloneDataChoices((List) choices.get(0));
        dataSelection = ((DataChoice) clonedList.get(0)).getDataSelection();
        Misc.run(new Runnable() {
            public void run() {
                try {
                    addNewData(clonedList);
                } catch (Exception exc) {
                    logException("Selecting new data", exc);
                }
            }
        });

    }

    /**
     * Popup a DataTreeDialog
     *
     * @param dialogMessage message for the dialog
     * @param from   the component that it is on
     * @param multiples can handle multiple selections
     * @param categories List of data categories. If
     *
     * @return List of selected data choices or null if none selected
     */
    protected List<DataChoice> selectDataChoices(final String dialogMessage,
            Component from, boolean multiples, List categories) {

        if (categories == null) {
            categories = getCategories();
        }
        DataOperand dataOperand = new DataOperand(dialogMessage,
                                      dialogMessage, categories, multiples);
        DataTreeDialog dataDialog =
            new DataTreeDialog(getIdv(), from, Misc.newList(dataOperand),
                               getControlContext().getAllDataSources(),
                               myDataChoices);

        return dataDialog.getSelected();
    }



    /**
     * This is used for xml persistence.
     *
     * @return The dataSelection member
     */
    public DataSelection getDataSelection() {
        if (dataSelection == null) {
            dataSelection = new DataSelection();
        }
        return dataSelection;
    }


    /**
     * This is used for xml persistence.
     *
     *
     * @param newDataSelection  The new dataSelection member
     */
    public void setDataSelection(DataSelection newDataSelection) {
        dataSelection = newDataSelection;
    }


    /**
     * <p>Creates and returns the {@link ucar.unidata.data.DataInstance}
     * corresponding to a {@link ucar.unidata.data.DataChoice}.
     * Returns <code>null</code> if the {@link ucar.unidata.data.DataInstance}
     * was somehow invalid.</p>
     *
     * <p>This method is invoked by the overridable method {@link
     * #setData(DataChoice)}.</p>
     *
     * @param dataChoice       The {@link ucar.unidata.data.DataChoice} from
     *                         which to create a
     *                         {@link ucar.unidata.data.DataInstance}.
     * @return                 The created
     *                         {@link ucar.unidata.data.DataInstance} or
     *                         <code>null</code>.
     * @throws VisADException  if a VisAD Failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    protected DataInstance doMakeDataInstance(DataChoice dataChoice)
            throws RemoteException, VisADException {
        return new DataInstance(dataChoice, getDataSelection(),
                                getRequestProperties());
    }

    /**
     * Create (if null) and return the Hashtable that holds
     * the extra request properties used in a getData call
     * on the {@link ucar.unidata.data.DataChoice}
     *
     * @return Request properties
     */
    protected Hashtable getRequestProperties() {
        if (requestProperties == null) {
            requestProperties = Misc.newHashtable(DataChoice.PROP_REQUESTER,
                    this);
        }
        return requestProperties;
    }

    /**
     * Process request properties from a DataChoice
     */
    protected void processRequestProperties() {
        if (requestProperties == null) {
            return;
        }

        timeLabels =
            (Hashtable) requestProperties.get(DataSource.PROP_TIMELABELS);
        //      System.err.println ("DCI.timeLabels = " + timeLabels);
        if (timeLabels == null) {
            return;
        }
        if (listeningForTimes) {
            return;
        }
    }


    /**
     * Check the timestamp label for a time entry
     *
     * @param time  to use
     *
     * @return true if the time is in the label
     */
    private boolean checkTimestampLabel(Real time) {

        boolean hasTimestamp = shouldAddAnimationListener();

        if ( !hasTimestamp) {
            return false;
        }
        if (time == null) {
            try {
                Animation animation = getSomeAnimation();
                if (animation != null) {
                    time = animation.getAniValue();
                }
            } catch (Exception exc) {
                logException("Getting animation", exc);
                return false;
            }
        }


        if (time == null) {
            return false;
        }
        try {
            Set timeSet = getDataTimeSet();
            if (timeSet == null) {
                currentTime = null;
                firstTime   = null;
            } else {
                currentTime = new DateTime(time);
                Unit setUnit = timeSet.getSetUnits()[0];

                if (Unit.canConvert(time.getUnit(), setUnit)) {
                    if (timeSet.getLength() > 0) {
                        Data firstSetTime = timeSet.__getitem__(0);
                        if (firstSetTime instanceof Real) {
                            double firstTimeValue =
                                ((Real) firstSetTime).getValue(
                                    currentTime.getUnit());
                            firstTime = new DateTime(
                                time.cloneButValue(firstTimeValue));
                        }
                    }
                    double timeVal = time.getValue(setUnit);
                    int    index   = timeSet.doubleToIndex(new double[][] {
                        new double[] { timeVal }
                    })[0];
                    if (index >= 0) {
                        RealTuple rt = DataUtility.getSample(timeSet, index);
                        DateTime dataTime =
                            new DateTime((Real) rt.getComponent(0));

                        currentTime = dataTime;
                    }
                }
            }
        } catch (Exception exc) {
            logException("Setting time string", exc);

        }

        return true;
    }


    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        if ( !getHaveInitialized() || !getActive()) {
            return;
        }

        if (checkTimestampLabel(time)) {
            updateLegendLabel();
        }

        if (timeLabels == null) {
            return;
        }


        /*
       String tmpLabel = (String) timeLabels.get(time);
        //      System.err.println ("timeLabel: " + tmpLabel);
       String currentTimeLabel = (current
        if ( !Misc.equals(tmpLabel, currentTimeLabel)) {
            currentTimeLabel = tmpLabel;
            if (currentTimeLabel == null) {
                currentTimeLabel = " ";
            }
            updateLegendLabel();
            }*/
    }


    /**
     * This is the main JLabel used in the legend.
     *
     * @param  legendType  type of legend
     *
     * @return  legend label
     */
    public JComponent getLegendLabel(int legendType) {
        switch (legendType) {

          case (SIDE_LEGEND) :
              if (sideLegendLabel == null) {
                  getSideLegendComponent();
              }
              return sideLegendLabel;

          default :
              return null;
        }
    }

    /**
     *  Return the gui component used to display this DisplayControl
     *  within a ViewManager. Note: as of now we only have one legend component
     *  per DisplayControl. This will  fail when we have a DisplayControl
     *  that displays into more than one ViewManagers.
     *
     * @param  legendType  type of legend
     *
     * @return legend component
     */
    public JComponent getLegendComponent(int legendType) {
        synchronized (LEGEND_MUTEX) {
            switch (legendType) {

              case (SIDE_LEGEND) :
                  if (sideLegendComponent == null) {
                      sideLegendComponent = doMakeSideLegendComponent();
                  }
                  return sideLegendComponent;

              case (BOTTOM_LEGEND) :
                  if (bottomLegendComponent == null) {
                      bottomLegendComponent = doMakeBottomLegendComponent();
                      applyLegendForeground();
                  }
                  return bottomLegendComponent;

              default :
                  return null;
            }
        }
    }

    /**
     * Shortcut to get the bottom legend component
     *
     * @return bottom legend component
     */
    private JComponent getBottomLegendComponent() {
        return getLegendComponent(BOTTOM_LEGEND);
    }

    /**
     * Shortcut to get the side legend component
     *
     * @return side legend component
     */
    private JComponent getSideLegendComponent() {
        return getLegendComponent(SIDE_LEGEND);
    }

    /**
     *  Create and return the gui component which is used to display
     *  this DisplayControl in the "legend" area of its ViewManager.
     *  This creates a JButton to popup the DisplayControl's window
     *  and other elements.
     *
     * @return Side legend component
     */
    //    static int cnt = 0;
    protected JComponent doMakeSideLegendComponent() {

        List comps  = new ArrayList();
        List labels = getLegendLabels(SIDE_LEGEND);

        legendTextArea = new JTextArea("");
        //legendTextArea.setBackground(Color.red);
        GuiUtils.applyDefaultFont(legendTextArea);
        legendTextArea.setEditable(false);
        //Add a paint method to draw an underline
        //We can't do it with the html in the jlabel
        //because of sizing issues
        sideLegendLabel = new SideLegendLabel(this, " ");
        labelsToUpdate.add(sideLegendLabel);

        comps.add(sideLegendLabel);
        comps.add(legendTextArea);
        for (int lblIdx = 0; lblIdx < comps.size(); lblIdx++) {
            JComponent comp = (JComponent) comps.get(lblIdx);
            if (lblIdx == 0) {
                comp.setToolTipText(
                    "<html>Click to show the control window.<br>Control-Click to center display.<br>Right click to show menu.<br>Click and drag to move display.</html>");
            } else {
                comp.setToolTipText("<html>Right click to show menu.</html>");
            }

            //            comp.setFont(GuiUtils.buttonFont);
            comp.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent event) {
                    if (SwingUtilities.isRightMouseButton(event)) {
                        showLegendMenu((JComponent) event.getSource(), event);
                    }
                }
            });
        }

        legendTextPanel = new JPanel(new BorderLayout());
        legendTextArea.setBackground(legendTextPanel.getBackground());
        legendTextPanel.add(legendTextArea);

        updateLegendLabel();
        JPanel     legendPanel = legendTextPanel;



        JComponent extraLegend = getExtraLegendComponent(SIDE_LEGEND);
        String     iconPath    = null;
        if (getDataChoice() != null) {
            iconPath = getDataChoice().getProperty(PROP_LEGENDICON,
                    (String) null);
        }

        JComponent iconLbl = null;
        //        System.err.println ("path:" + iconPath);
        if (iconPath != null) {
            iconLbl =
                GuiUtils.inset(GuiUtils.left(GuiUtils.getImageLabel(iconPath,
                    getClass())), 2);
        }


        JPanel colorSwatch = null;
        if (showColorControlWidget() && checkFlag(FLAG_COLOR)) {
            colorSwatch = GuiUtils.wrap(new JLabel("     "));
            colorSwatches.add(colorSwatch);
            colorSwatch.setBackground(color);
            final JComponent theColorSwatch = colorSwatch;
            colorSwatch.setToolTipText("Click to change color");
            colorSwatch.addMouseListener(new ObjectListener(null) {

                public void mouseClicked(MouseEvent me) {
                    popupColorMenu(theColorSwatch);
                }
            });
            final JLabel colorSwatchLabel = new JLabel(getColorWidgetLabel()
                                                + ": ");
            colorSwatchLabel.setToolTipText("Click to change color");
            colorSwatchLabel.addMouseListener(new ObjectListener(null) {
                public void mouseClicked(MouseEvent me) {
                    popupColorMenu(colorSwatchLabel);
                }
            });
            colorSwatch = GuiUtils.left(GuiUtils.hbox(colorSwatchLabel,
                    colorSwatch));
        }

        if ((iconLbl != null) || (extraLegend != null)
                || (colorSwatch != null)) {
            //Only show either  the  iconLbl or the color bar.
            //This is a bit of a hack but it works for now because the
            //only display control that has an icon label is image controls
            //from a WMS datasource.
            if (iconLbl != null) {
                extraLegend = GuiUtils.vbox(iconLbl, colorSwatch);
            } else {
                extraLegend = GuiUtils.vbox(extraLegend, colorSwatch);
            }
            legendPanel = GuiUtils.vbox(legendPanel,
                                        GuiUtils.inset(extraLegend,
                                            new Insets(1, 0, 0, 0)));
            legendPanel.setBackground(Color.blue);
        }

        if (GuiUtils.getDefaultFont() == null) {
            GuiUtils.setFontOnTree(legendPanel,
                                   GuiUtils.buttonFont.deriveFont(10.0f));
        } else {
            GuiUtils.setFontOnTree(legendPanel, GuiUtils.getDefaultFont());
        }
        return legendPanel;
    }



    /**
     * make the legend label
     *
     * @return legend label
     */
    public JLabel makeLegendLabel() {
        JLabel comp = new SideLegendLabel(this, getMenuLabel());
        labelsToUpdate.add(comp);
        comp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                if (SwingUtilities.isRightMouseButton(event)) {
                    showLegendMenu((JComponent) event.getSource(), event);
                }
            }
        });
        return comp;
    }



    /**
     * Class SideLegendLabel Does the underline
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.726 $
     */
    public static class SideLegendLabel extends DndImageButton {

        /** The control */
        DisplayControlImpl myControl;

        /** Old cursor */
        Cursor oldCursor;

        /** Is mouse down */
        boolean mouseDown = false;

        /** Is mouse in */
        boolean mouseIn = false;

        /** the foreground color */
        Color color;

        /**
         * Ctor
         *
         * @param displayControl The display
         * @param text Label text
         */
        public SideLegendLabel(DisplayControlImpl displayControl,
                               String text) {
            super(text, displayControl, "control");
            this.myControl = displayControl;
            //            setForeground(Color.blue);

            color = GuiUtils.decodeColor("#005aff", Color.red);
            this.setForeground(color);
            GuiUtils.applyDefaultFont(this);
            this.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    //Don't do anything if it is the right mouse button
                    if ( !myControl.getHaveInitialized()) {
                        return;
                    }
                    if (GuiUtils.isControlKey(event)) {
                        myControl.centerOnDisplay();
                        return;
                    }
                    if (SwingUtilities.isRightMouseButton(event)) {
                        return;
                    }
                    myControl.popup(SideLegendLabel.this);
                }

                public void mousePressed(MouseEvent e) {
                    mouseDown = true;
                    repaint();
                }

                public void mouseReleased(MouseEvent e) {
                    mouseDown = false;
                    repaint();
                }

                public void mouseEntered(MouseEvent e) {
                    mouseIn = true;
                    setForeground(color);
                    oldCursor = getCursor();
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    repaint();
                }

                public void mouseExited(MouseEvent e) {
                    mouseIn = false;
                    //setForeground(Color.black);
                    if (oldCursor != null) {
                        setCursor(oldCursor);
                    }
                    repaint();
                }
            });
        }

        /**
         * Paint me
         *
         * @param g The graphics
         */
        public void paint(Graphics g) {
            Rectangle b = getBounds();
            if (mouseDown) {
                g.setColor(Color.gray);
                g.drawRect(0, 0, b.width - 1, b.height - 1);
                //                g.fillRect(0,0,b.width-1,b.height-1);
            }
            if (mouseIn) {
                //                g.setColor(Color.white);
                //                g.drawRect(0,0,b.width-1,b.height-1);
            }
            g.setColor(color);
            super.paint(g);



            String      text = getText();
            Font        font = g.getFont();
            FontMetrics fm   = g.getFontMetrics(font);
            //int width = Math.min(getWidth(), fm.stringWidth(text));
            int width = fm.stringWidth(text);  //should be good enough
            //Use height of label and height of font to calc baseline
            int y = (getHeight() + fm.getHeight()) / 2 - 2;
            g.drawLine(3, y, width + 3, y);
        }

    }



    /**
     * Popup a color setting menu wrt the given component
     *
     * @param comp The component to popup near
     */
    private void popupColorMenu(JComponent comp) {
        JPopupMenu popup = GuiUtils.makePopupMenu(makeChangeColorMenuItems());
        popup.show(comp, 0, comp.getHeight());
    }

    /**
     * Popup a color setting menu wrt the given component
     *
     * @param comp The component to popup near
     */
    private void popupDisplayListColorMenu(JComponent comp) {
        JPopupMenu popup = GuiUtils.makePopupMenu(
                               makeChangeColorMenuItems(
                                   "setDisplayListColor",
                                   getDisplayListColor()));
        popup.show(comp, 0, comp.getHeight());
    }

    /**
     * Add the list of menu items for changing the color to the
     * component (usually a menu or popup menu)
     *
     *
     * @return component with the change color menu items
     */
    protected List makeChangeColorMenuItems() {
        return makeChangeColorMenuItems("setColor", color);
    }

    /**
     * Add the list of menu items for changing the color to the
     * component (usually a menu or popup menu)
     *
     * @param methodName  method to invoke
     * @param color the default color
     *
     * @return List with the change color menu items
     */
    private List makeChangeColorMenuItems(final String methodName,
                                          final Color color) {
        List items  = new ArrayList();
        List colors = getDisplayConventions().getColorNameList();
        for (Iterator iter = colors.iterator(); iter.hasNext(); ) {
            String colorName = iter.next().toString();
            final Color menuColor =
                getDisplayConventions().getColor(colorName);
            JMenuItem mi = new JMenuItem(colorName.substring(0,
                               1).toUpperCase() + colorName.substring(1)
                                   + "  ") {
                public void paint(Graphics g) {
                    super.paint(g);
                    Rectangle b = getBounds();
                    if (Misc.equals(menuColor, color)) {
                        g.setColor(Color.white);
                        g.fillRect(b.width - b.height, 0, b.height, b.height);
                        g.setColor(Color.black);
                        g.drawRect(b.width - b.height, 0, b.height - 1,
                                   b.height - 1);
                    }
                    g.setColor(menuColor);
                    int w = b.height;
                    g.fillRect(b.width - w + 3, 3, w - 6, w - 6);
                }
            };
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        Method theMethod =
                            Misc.findMethod(
                                DisplayControlImpl.this.getClass(),
                                methodName,
                                new Class[] { menuColor.getClass() });
                        if (theMethod == null) {
                            throw new NoSuchMethodException("unknown method "
                                    + methodName);
                        } else {
                            theMethod.invoke(DisplayControlImpl.this,
                                             new Object[] { menuColor });
                        }
                    } catch (Exception exc) {
                        logException("Setting color", exc);
                    }
                }
            });
            items.add(mi);
        }
        items.add(GuiUtils.MENU_SEPARATOR);
        items.add(GuiUtils.makeMenuItem("Custom Color", this,
                                        "showColorDialog", methodName));

        return items;
    }


    /**
     * Create and show the popup menu for the user's right click on the
     * legend.
     *
     * @param comp The component the user clicked on
     * @param event The mouse event
     */
    protected void showLegendMenu(final JComponent comp, MouseEvent event) {
        List items = getControlMenus(comp);

        /*
        //If we have our own window then add the Control Window menu item
        items.add(0, GuiUtils.makeMenuItem("Control Window", this, "popup",
                                           comp));
        items.add(1, GuiUtils.MENU_SEPARATOR);
        */
        JPopupMenu menu = GuiUtils.makePopupMenu(items);
        menu.show(comp, event.getX(), event.getY());
    }



    /**
     * Get the list of menus associated with this control.
     *
     *
     * @param comp  the  component to place the menus near
     * @return  The menus
     */
    public List getControlMenus(final JComponent comp) {

        List fileItems  = new ArrayList();
        List viewItems  = new ArrayList();
        List helpItems  = new ArrayList();
        List editItems  = new ArrayList();
        List extraMenus = new ArrayList();

        getFileMenuItems(fileItems, false);
        List saveItems = new ArrayList();
        getSaveMenuItems(saveItems, true);
        fileItems.add(GuiUtils.MENU_SEPARATOR);
        fileItems.add(GuiUtils.makeMenu(new JMenu("Save"), saveItems));


        getLastFileMenuItems(fileItems);


        getViewMenuItems(viewItems, false);
        // TODO: merge this with the makeViewMenu logic
        getIdv().getIdvUIManager().addViewMenuItems(this, viewItems);
        getEditMenuItems(editItems, false);
        getExtraMenus(extraMenus, false);  // array of extra menus
        getHelpMenuItems(helpItems, false);



        JMenuItem mi;
        List      items = new ArrayList();
        items.add(GuiUtils.makeMenu("File", fileItems));
        items.add(GuiUtils.makeMenu("Edit", editItems));
        items.add(GuiUtils.makeMenu("View", viewItems));
        if ( !extraMenus.isEmpty()) {
            for (int i = 0; i < extraMenus.size(); i++) {
                items.add(extraMenus.get(i));
            }
        }
        items.add(GuiUtils.makeMenu("Help", helpItems));

        if (comp != null) {
            //If we have our own window then add the Control Window menu item
            items.add(0, GuiUtils.makeMenuItem("Control Window", this,
                    "popup", comp));
            items.add(1, GuiUtils.MENU_SEPARATOR);
        }

        return items;
    }


    /**
     * Change the display unit.
     */
    public void changeDisplayUnit() {
        Unit newUnit = null;
        while (true) {
            newUnit = getDisplayConventions().selectUnit(displayUnit, null);
            if (newUnit == null) {
                return;
            }
            if (setNewDisplayUnit(newUnit, true)) {
                break;
            }
        }
        doShareExternal(SHARE_DISPLAYUNIT, newUnit);
    }


    /**
     * Change the color unit.
     */
    public void changeColorUnit() {
        Unit newUnit = getDisplayConventions().selectUnit(colorUnit, null);
        if (newUnit != null) {
            setNewColorUnit(newUnit, true);
        }

    }



    /**
     * Utility to make the menu item for changing the color.
     *
     * @return The menu item
     */
    protected JMenu doMakeChangeColorMenu() {
        return doMakeChangeColorMenu(getColorWidgetLabel());
    }


    /**
     * Utility to make the menu item for changing the color.
     * @param name  name for the menu
     *
     * @return The menu item
     */
    protected JMenu doMakeChangeColorMenu(String name) {
        return GuiUtils.makeMenu(((name != null)
                                  ? name
                                  : getColorWidgetLabel()), makeChangeColorMenuItems());
    }



    /**
     * Utility to make the menu item for changing the data choice
     *
     * @return The menu item
     */
    protected JMenuItem doMakeChangeParameterMenuItem() {
        final JMenuItem selectChoices =
            new JMenuItem(getChangeParameterLabel());
        selectChoices.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                popupDataDialog("<html>Choose Parameter</html>",
                                selectChoices);
            }
        });
        return selectChoices;
    }



    /**
     * Return the tooltip text that is to be shown in the legend
     * This method constructs html that shows the display name, choices,
     * link to help, etc.
     *
     * @return The tooltip
     */
    protected String getLegendToolTip() {
        StringBuffer sb = new StringBuffer("<html>");
        sb.append("<b>&nbsp; Display: "
                  + StringUtil.join("; ", getLegendLabels(BOTTOM_LEGEND))
                  + "</b><p>");
        sb.append(getDetailsContents());
        return sb.toString();
    }



    /**
     * Get the contents of the details html
     *
     * @return The contents of the details
     */
    protected String getDetailsContents() {
        if (myDataChoices == null) {
            return "";
        }
        StringBuffer sb     = new StringBuffer("");
        boolean      didone = false;
        for (int i = 0; i < myDataChoices.size(); i++) {
            if (i == 0) {
                sb.append(" <br> <b>&nbsp;Data:</b><ul>");
            }
            didone = true;
            DataChoice dc = (DataChoice) myDataChoices.get(i);
            sb.append("<li> &nbsp;");
            sb.append(dc.getFullDescription());
            sb.append(" &nbsp;");
        }
        if (didone) {
            sb.append("</ul>");
        }
        if (dataSelection != null) {
            GeoSelection geoSelection = dataSelection.getGeoSelection();
            if (geoSelection != null) {
                sb.append("Geo selection:" + geoSelection);
                sb.append("<br>\n");
            }
            List times = dataSelection.getTimes();
            if ((times != null) && (times.size() > 0)) {
                sb.append("Selected times:" + StringUtil.join(" ", times));
            }
        }


        if (LogUtil.getDebugMode()) {
            getDebugDetails(sb);
        }

        return sb.toString();
    }


    /**
     * Debug
     *
     * @param msg the message
     */
    public void debug(String msg) {
        //        if(displayId.startsWith("plan")) {
        //            System.out.println(new java.util.Date() + ": " + msg);
        //        }
    }


    /**
     * When we are iun debug mode this method gets called to add details html
     *
     * @param sb Buffer to append to.
     */
    protected void getDebugDetails(StringBuffer sb) {
        if (dataInstances != null) {
            try {
                sb.append("<p>Data Types:<br>");
                for (int i = 0; i < dataInstances.size(); i++) {
                    DataInstance dataInstance =
                        (DataInstance) dataInstances.get(i);
                    Data data = dataInstance.getData();
                    sb.append("<b>" + data.getType().toString() + "<br>");
                }
            } catch (Exception exc) {
                logException("Making debug details", exc);
            }
        } else {}

    }

    /**
     * Create and return the gui component which is used to display
     * this DisplayControl in the "legend" area of its ViewManager.
     * This creates a JButton to popup the DisplayControl's window
     * and other elements.
     *
     * @return  Bottom legend component
     */
    protected JComponent doMakeBottomLegendComponent() {

        bottomLegendButton = new JButton("                    ");

        bottomLegendButton.setToolTipText(
            "Left click to toggle visiblity. Right click to show menu.");

        bottomLegendButton.setBorder(BorderFactory.createEmptyBorder());
        bottomLegendButton.setFont(GuiUtils.buttonFont);

        bottomLegendButton.setHorizontalAlignment(SwingConstants.LEFT);
        bottomLegendButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                if (SwingUtilities.isRightMouseButton(event)) {
                    showLegendMenu((JComponent) event.getSource(), event);
                }
            }
        });
        bottomLegendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setDisplayVisibility( !isVisible);
            }
        });
        bottomLegendButton.addActionListener(this);

        JPanel colorSwatch = GuiUtils.wrap(new JLabel("     "));
        colorSwatches.add(colorSwatch);
        if (color != null) {
            colorSwatch.setBackground(color);
        }
        JComponent extraLegend = getExtraLegendComponent(BOTTOM_LEGEND);

        updateLegendLabel();

        if (extraLegend != null) {
            return GuiUtils.centerRight(
                GuiUtils.centerRight(bottomLegendButton, extraLegend),
                colorSwatch);
        } else {
            return GuiUtils.centerRight(bottomLegendButton, colorSwatch);
        }


    }

    /**
     * Method to show all <code>ColorScales</code> associated with this
     * DisplayControl.
     *
     * @param show  true to show.
     * @return true if successful, false if it failed
     */
    protected boolean showColorScales(boolean show) {
        try {
            if (colorScales == null) {
                if ( !show) {
                    return false;
                }
                doMakeColorScales();
            }
            for (int i = 0; i < colorScales.size(); i++) {
                setDisplayableVisibility(((Displayable) colorScales.get(i)),
                                         show);
            }
            return true;
        } catch (Exception exc) {
            userMessage("Error showing ColorScale: " + exc);
        }
        return false;
    }

    /**
     * Actually create the color scales
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void doMakeColorScales()
            throws VisADException, RemoteException {
        if (colorScaleInfo == null) {
            colorScaleInfo = getDefaultColorScaleInfo();
        }
        if ( !colorScaleInfo.getIsVisible() || !getDisplayVisibility()) {
            return;
        }
        //Misc.printStack("adding color scales", 5);
        colorScales = new ArrayList();
        List v = getViewManagers();
        for (int i = 0; i < v.size(); i++) {
            ColorScale colorScale = new ColorScale(colorScaleInfo);
            addDisplayable(colorScale, (ViewManager) v.get(i),
                           FLAG_COLORTABLE);
            setDisplayableVisibility(colorScale,
                                     colorScaleInfo.getIsVisible());
            colorScales.add(colorScale);
        }
    }

    /**
     * Get the default color scale info
     * @return default ColorScaleInfo
     */
    protected ColorScaleInfo getDefaultColorScaleInfo() {
        ColorScaleInfo info = new ColorScaleInfo(paramName,
                                  ColorScale.VERTICAL_ORIENT,
                                  (colorTable != null)
                                  ? getColorTable().getColorTable()
                                  : null);
        info.setPlacement(ColorScaleInfo.TOP);
        info.setLabelColor(ColorScale.DEFAULT_LABEL_COLOR);
        info.setIsVisible(false);
        info.setUnit(getDisplayUnit());
        ViewManager vm = getDefaultViewManager();
        Font        f  = null;
        if (vm != null) {
            f = vm.getDisplayListFont();
        }
        info.setLabelFont(f);
        return info;
    }

    /**
     * Does this control have displays in one of the main
     * ViewManagers
     *
     * @return Shown in an external ViewManager
     */
    private boolean hasExternalView() {
        List v = getDisplayInfos();
        //TODO: Is this all we need to do?
        return (v.size() > 0);

        /**
         *        for (int i=0, n=v.size(); i < n; i++) {
         *   DisplayInfo info = (DisplayInfo) v.get (i);
         *   }
         */
    }


    /**
     * Move the displays of this control into a newly created  ViewManager.
     *
     */
    private void newViewManager() {
        moveTo(getViewManager(new ViewDescriptor()));
    }

    /**
     * An implementation of the DisplayControl  interface moveTo method.
     * This is called when the user has dragged-and-dropped the display
     * control (well, really the legend component) from one ViewManager
     * to another. This nulls out the legendComponent member and iterates
     * through all of the DisplayInfo-s, telling them to set their
     * ViewManager to the given viewManager.
     *
     * @param newViewManager  new <code>ViewManager</code> to move to
     */
    public void moveTo(ViewManager newViewManager) {
        defaultViewManager = newViewManager;
        if ((defaultViewManager != null)
                && (defaultViewManager.getViewDescriptor() != null)) {
            defaultView = defaultViewManager.getViewDescriptor().getName();
        }

        List displayList = getDisplayInfos();
        try {
            boolean didone = false;
            for (int i = 0, n = displayList.size(); i < n; i++) {
                DisplayInfo info = (DisplayInfo) displayList.get(i);
                if (info.getViewManager() == newViewManager) {
                    continue;
                }
                didone = true;
                info.moveTo(newViewManager);
            }
            if (didone) {
                activateDisplay(displayList);
            }

        } catch (Exception exc) {
            logException("Moving to a new view", exc);
        }
        newViewManager.controlMoved(this);
    }


    /**
     * Set the color to be used for the foreground in the bottom legend
     *
     * @param fg The new color
     */
    public void setLegendForeground(Color fg) {
        legendForeground = fg;
        applyLegendForeground();
    }



    /**
     * Set the color to be used for the background in the bottom legend
     *
     * @param bg The new color
     */
    public void setLegendBackground(Color bg) {
        legendBackground = bg;
        GuiUtils.setBackgroundOnTree(getBottomLegendComponent(), bg);
        if (color != null) {
            for (int i = 0; i < colorSwatches.size(); i++) {
                ((JComponent) colorSwatches.get(i)).setBackground(color);
            }
        }
    }

    /**
     * Change the visiblity of all color swatches
     *
     * @param on Visibility on or off
     */
    protected void showColorSwatches(boolean on) {
        for (int i = 0; i < colorSwatches.size(); i++) {
            JComponent swatch = (JComponent) colorSwatches.get(i);
            swatch.setVisible(on);
            swatch.invalidate();
            if (swatch.getParent() != null) {
                swatch.getParent().validate();
            }

        }

    }


    /**
     * Apply the legendForeground color to the bottom legend
     */
    private void applyLegendForeground() {
        Color fg;
        if (isVisible) {
            fg = ((legendForeground != null)
                  ? legendForeground
                  : Color.black);
        } else {
            fg = Color.gray;
        }
        if (bottomLegendComponent != null) {
            GuiUtils.setForegroundOnTree(bottomLegendComponent, fg);
        }
    }

    /**
     * notify views  of change
     */
    protected void notifyViewManagersOfChange() {
        List v = getViewManagers();
        for (int i = 0; i < v.size(); i++) {
            ((ViewManager) v.get(i)).displayControlChanged(this);
        }

    }


    /**
     * Set the ShowInDisplayList property.
     *
     * @param value The new value for ShowInDisplayList
     */
    public void setShowInDisplayList(boolean value) {
        showInDisplayList = value;
        notifyViewManagersOfChange();
    }

    /**
     * Get the ShowInDisplayList property.
     *
     * @return The ShowInDisplayList
     */
    public boolean getShowInDisplayList() {
        return showInDisplayList;
    }

    /**
     * Get the locking visibility of the display
     *
     * @return true if locking visibility on
     */
    public boolean getLockVisibilityToggle() {
        return lockVisibiltyToggle;
    }

    /**
     * Set the locking visibility of the display
     *
     * @param v true to set locking visibility on
     */
    public void setLockVisibilityToggle(boolean v) {
        lockVisibiltyToggle = v;
        for (int i = 0; i < lockButtons.size(); i++) {
            updateLockButton((JButton) lockButtons.get(i));
        }
    }


    /**
     * Get the visibility of the display
     *
     * @return true if visibility on
     */
    public boolean getDisplayVisibility() {
        return isVisible;
    }

    /**
     * Toggle the visibility for vector graphics rendering
     *
     * @param rasterMode  the toggle mode
     *
     * @throws Exception  problem toggling
     */
    public void toggleVisibilityForVectorGraphicsRendering(int rasterMode)
            throws Exception {
        if (rasterMode == RASTERMODE_SHOWRASTER) {
            setDisplayVisibility(getIsRaster());
        } else if (rasterMode == RASTERMODE_SHOWNONRASTER) {
            setDisplayVisibility( !getIsRaster());
        } else {
            setDisplayVisibility(true);
        }
    }


    /**
     * Is this a raster display?
     *
     * @return  true if raster
     */
    public boolean getIsRaster() {
        return isRaster;
    }

    /**
     * Set IsRaster property
     *
     * @param v  the value
     */
    public void setIsRaster(boolean v) {
        isRaster = v;
    }

    /**
     * Set the visibility of the display and set the state of
     * any visibility buttons we may have.
     *
     * @param on true to set visibility on
     */
    public void setDisplayVisibility(boolean on) {
        setDisplayVisibility(on, true);
    }


    /**
     * Set the visibility of the display and set the state of any
     * visibility buttons we may have.
     *
     * @param on  true if they should be shown
     * @param shouldShare  true if this property should be shared
     */
    private void setDisplayVisibility(boolean on, boolean shouldShare) {
        //Make sure we don't loop forever because we setSelected on other 
        //checkboxes which triggers the event which ends up calling this method
        if (settingVisibility) {
            return;
        }
        isVisible = on;
        if ( !getHaveInitialized()) {
            return;
        }

        settingVisibility = true;
        applyLegendForeground();

        try {
            //Now run through all of the visibility buttons and update them
            for (int i = 0, n = visibilityCbs.size(); i < n; i++) {
                ((AbstractButton) visibilityCbs.get(i)).setSelected(on);
            }
            List displayList = getDisplayInfos();
            for (int i = 0, n = displayList.size(); i < n; i++) {
                DisplayInfo info        = (DisplayInfo) displayList.get(i);
                Displayable displayable = (Displayable) info.getDisplayable();
                //Preserve if the displayable   was initially hidden
                if (isVisible) {
                    if (info.getUltimateVisible()) {
                        displayable.setVisible(true);
                    }
                } else {
                    displayable.setVisible(false);
                }
                // TODO:  If we want to toggle the times in animation, uncomment
                // the next line
                //displayable.setUseTimesInAnimation(getUseTimesInAnimation() && isVisible);
            }

            if (colorScales != null) {
                boolean shouldBeVisible = getColorScaleInfo().getIsVisible();
                for (int i = 0; i < colorScales.size(); i++) {
                    ColorScale scale = (ColorScale) colorScales.get(i);
                    if ( !on) {
                        scale.setVisible(false);
                    } else {
                        scale.setVisible(shouldBeVisible);
                    }
                }
            }

            if (displayListTable != null) {
                for (Enumeration e = displayListTable.elements();
                        e.hasMoreElements(); ) {
                    DisplayableData d = (DisplayableData) e.nextElement();
                    d.setVisible(on);
                }
            }


        } catch (Exception exc) {
            logException("Setting visibility of the display", exc);
        }
        settingVisibility = false;
        if (shouldShare) {
            doShareExternal(SHARE_VISIBILITY, new Boolean(isVisible));
        }

        List v = getViewManagers();
        for (int i = 0; i < v.size(); i++) {
            ((ViewManager) v.get(i)).displayControlVisibilityChanged(this);
        }

    }

    /**
     * Deprecated, misspelled version of setDisplayVisibility.
     * @deprecated
     *
     * @param d d
     * @param visible visible
     *
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    protected void setDisplayableVisiblity(Displayable d, boolean visible)
            throws RemoteException, VisADException {
        setDisplayableVisibility(d, visible);
    }

    /**
     * See if this can save data in cache.
     * @return true if allowable
     */
    protected boolean canSaveDataInCache() {
        return ((dataInstances != null) && (dataInstances.size() == 1));
    }

    /**
     * Set the ultimate visibility of the particular {@link ucar.visad.display.Displayable}.
     * Used for maintaining the visibility of individual displayables
     * if there are multiple.
     *
     * @param d {@link ucar.visad.display.Displayable} in question
     * @param visible  true if it should be visible when the visibility
     *                 of the entire control is true.
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the visibility in VisAD
     */
    protected void setDisplayableVisibility(Displayable d, boolean visible)
            throws RemoteException, VisADException {
        List displayList = getDisplayInfos();
        for (int i = 0, n = displayList.size(); i < n; i++) {
            DisplayInfo info        = (DisplayInfo) displayList.get(i);
            Displayable displayable = (Displayable) info.getDisplayable();
            if (displayable != d) {
                continue;
            }
            info.setUltimateVisible(visible);
            if (visible) {
                d.setVisible(isVisible);
            } else {
                d.setVisible(false);
            }
            break;
        }
    }


    /**
     * A no-op method to implement the ItemListener interface.
     *
     * @param event The event
     */
    public void itemStateChanged(ItemEvent event) {}

    /**
     * Method to do what needs to be done when the display control
     * failed.
     */
    public void displayControlFailed() {
        try {
            if ( !getProperty("control.ignoreerrors", false)) {
                doRemove();
            }
        } catch (Exception exc) {}
        //        disposeOfWindow ();
    }


    /**
     * Close the  window. If this display control is not shown
     * in any ViewManager-s then call doRemove
     */
    public void close() {
        if (myWindow != null) {
            myWindow.setVisible(false);
        }
        if ( !isInViewManager()) {
            try {
                doRemove();
            } catch (Exception exc) {}
        }

    }


    /**
     *  If the JFrame window is non-null call dispose on it
     */
    private void disposeOfWindow() {
        if (myWindow != null) {
            final IdvWindow tmpWindow = myWindow;
            myWindow = null;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    tmpWindow.setVisible(false);
                    tmpWindow.dispose();
                }
            });
        }
    }


    /**
     * Show the dialog window for this control.
     *
     * @param src Component that we popup the window near. May be null.
     */
    public void popup(Component src) {
        Window f = GuiUtils.getWindow(contents);
        if ((f != null) && !makeWindow) {
            GuiUtils.showComponentInTabs(contents);
            //            if (f != null) {
            //                GuiUtils.showDialogNearSrc(src, f);
            //            }
        } else {
            makeWindow = true;
            if (myWindow == null) {
                myWindow = createIdvWindow();
                if (myWindow != null) {
                    initWindow(myWindow);
                }
            }
            if (src != null) {
                GuiUtils.showDialogNearSrc(src, ((myWindow != null)
                        ? myWindow.getComponent()
                        : null));
            } else {
                GuiUtils.showWidget(myWindow.getComponent());
            }
        }
    }

    /**
     * Process the actions of any of the controls created with the
     * doMake...Control method calls.
     *
     * @param event The action event
     */
    public void actionPerformed(ActionEvent event) {
        if ( !okToFireEvent) {
            return;
        }

        String cmd = event.getActionCommand();
        try {
            if (cmd.equals(CMD_REMOVE)) {
                try {
                    doRemove();
                } catch (Exception exc) {
                    logException("Removing the display", exc);
                }
            }
            if (cmd.equals(CMD_POPUP)) {
                popup((Component) event.getSource());
                return;
            }

            if (cmd.equals(CMD_COLORS)) {
                JComboBox box       = (JComboBox) event.getSource();
                String    colorName = (String) box.getSelectedItem();
                if (colorName != null) {
                    Color newColor =
                        getDisplayConventions().getColor(colorName);
                    setColor(newColor);
                }
                return;
            }

            if (cmd.equals(CMD_COLORTABLE)) {
                JComboBox  box           = (JComboBox) event.getSource();
                ColorTable newColorTable = (ColorTable) box.getSelectedItem();
                if (newColorTable == null) {
                    //TODO - error handle
                } else {
                    setColorTable(newColorTable);
                }
                return;
            }


            log_.debug("Got unhandled cmd:" + cmd);
        } catch (Exception exc) {
            logException("Handling the action:" + cmd, exc);
        }
    }


    /**
     * Are we in a Globe display?
     *
     * @return  true if we're in the globe
     */
    public boolean inGlobeDisplay() {
        return (getNavigatedDisplay() instanceof GlobeDisplay);
    }



    /**
     * Just calls getNavigatedDisplay
     *
     * @return The  NavigatedDisplay that this control displays in.
     */
    public NavigatedDisplay getMapDisplay() {
        return getNavigatedDisplay();
    }


    /**
     * Utility method to determine if we are displaying in a 3d viewmanager
     *
     * @return Is the display 3d
     */
    public boolean isDisplay3D() {
        NavigatedDisplay navDisplay = getNavigatedDisplay();
        if (navDisplay != null) {
            return getNavigatedDisplay().getDisplayMode()
                   == NavigatedDisplay.MODE_3D;
        }
        return false;
    }

    /**
     * Utility method to get the altitude type of the display
     *
     * @return Altitude type of the display
     */
    public DisplayRealType getDisplayAltitudeType() {
        NavigatedDisplay navDisplay = getNavigatedDisplay();
        if (navDisplay != null) {
            return navDisplay.getDisplayAltitudeType();
        }
        //What do we return here?
        return null;
    }


    /**
     * A helper method for finding the
     * {@link ucar.unidata.view.geoloc.NavigatedDisplay} of this control's
     * {@link ucar.unidata.idv.MapViewManager}.
     * This iterates through the list of DisplayInfo-s, finding the first one
     * that holds an instance of a MapViewManager.  If not found this uses the
     * default MapViewManager
     *
     * @return The  NavigatedDisplay that this control displays in.
     */

    public NavigatedDisplay getNavigatedDisplay() {
        ViewManager vm = getViewManager();
        if ((vm == null) || !(vm instanceof NavigatedViewManager)) {
            return null;
        }
        return ((NavigatedViewManager) vm).getNavigatedDisplay();
    }


    /**
     * Set the display master to be inactive.
     * Return whether it was active at first.
     *
     */
    public void setDisplayInactive() {
        try {
            NavigatedDisplay display = getNavigatedDisplay();
            if (display != null) {
                display.setDisplayInactive();
            }
        } catch (Exception exc) {}
    }

    /**
     * If wasActive is true then set the display master to be active.
     */
    public void setDisplayActive() {
        try {
            NavigatedDisplay display = getNavigatedDisplay();
            if (display != null) {
                display.setDisplayActive();
            }
        } catch (Exception exc) {}
    }




    /**
     * Set the display master to be active/inactive.
     *
     * @param active Active or inactive
     *
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the visibility in VisAD
     */
    public void xxxsetDisplayActive(boolean active)
            throws RemoteException, VisADException {
        NavigatedDisplay display = getNavigatedDisplay();
        if (display != null) {
            //            display.setActive(active);
        }

    }


    /**
     * A helper method for finding the
     * {@link ucar.unidata.idv.MapViewManager}
     * this control displays in.  This method
     * iterates through the list of DisplayInfo-s, finding the first one
     * that holds an instance of a MapViewManager.  If not found this
     * returns the default MapViewManager
     *
     * @return A MapViewManager
     */
    public MapViewManager getMapViewManager() {
        List displayList = getDisplayInfos();
        for (int i = 0, n = displayList.size(); i < n; i++) {
            DisplayInfo info = (DisplayInfo) displayList.get(i);
            ViewManager vm   = info.getViewManager();
            if (vm instanceof MapViewManager) {
                return (MapViewManager) vm;
            }
        }
        return null;
    }


    /**
     * A helper method for finding the {@link ucar.visad.display.DisplayMaster}
     * that displays the given {@link ucar.visad.display.Displayable}
     *
     * @param displayable The displayable to look for
     * @return The DisplayMaster the displayable is in
     */
    public DisplayMaster getDisplayMaster(Displayable displayable) {
        ViewManager viewManager = getViewManager(displayable);
        return ((viewManager != null)
                ? viewManager.getMaster()
                : null);
    }

    /**
     * A helper method for finding the common {@link ucar.unidata.idv.ViewManager}
     * which is displaying the given displayable.
     *
     * @param displayable The displayable to look for
     * @return The ViewManager the displayable is in
     *
     */
    public ViewManager getViewManager(Displayable displayable) {
        List displayList = getDisplayInfos();
        for (int i = 0, n = displayList.size(); i < n; i++) {
            DisplayInfo info = (DisplayInfo) displayList.get(i);
            if (info.getDisplayable() == displayable) {
                return info.getViewManager();
            }
        }
        return null;
    }

    /**
     * Get, from the {@link ucar.unidata.idv.ControlContext},
     * the {@link ucar.unidata.idv.ViewManager} identified by the
     * {@link ucar.unidata.idv.ViewDescriptor} returned from
     * {@link #getDefaultViewDescriptor}
     *
     * @return The view manager
     */
    public ViewManager getViewManager() {
        if (defaultViewManager != null) {
            if (defaultViewManager.getIsDestroyed()) {
                defaultViewManager = null;
                defaultView        = null;
            } else {
                return defaultViewManager;
            }
        }
        return defaultViewManager =
            getViewManager(getDefaultViewDescriptor());
    }


    /**
     * Get, from the {@link ucar.unidata.idv.ControlContext},
     * the {@link ucar.unidata.idv.ViewManager} identified by the
     * given {@link ucar.unidata.idv.ViewDescriptor}
     *
     * @param viewDescriptor The view descriptor that identifies the view manager we want
     * @return The view manager
     */
    public ViewManager getViewManager(ViewDescriptor viewDescriptor) {
        if ((defaultViewManager != null)
                && defaultViewManager.getIsDestroyed()) {
            defaultViewManager = null;
        }


        if (viewDescriptor.equals(getDefaultViewDescriptor())
                && (defaultViewManager != null)) {
            return defaultViewManager;
        }
        debug("new vm-1");
        ViewManager vm = getControlContext().getViewManager(viewDescriptor,
                             true, null);
        debug("new vm-2");
        return vm;

    }



    /**
     * Get the toggle button JCheckBox that is source for item listener
     * events to toggle visibility of the display.
     *
     * @param label The label to use when creating the JCheckBox
     * @return JCheckBox the toggle button
     */
    public JCheckBox doMakeVisibilityControl(String label) {
        // make toggle button for toggling visibility of surface
        final JCheckBox cb = new JCheckBox(label, isVisible);
        visibilityCbs.add(cb);
        cb.setToolTipText("Toggle visibility on/off");
        cb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                setDisplayVisibility(cb.isSelected());
            }
        });

        return cb;
    }


    /**
     * Create the gui control (a JButton) that allows the user to
     * remove this DisplayControl. This object is added as an ActionListener
     * to the button with the action command CMD_REMOVE.
     *
     * @param tooltip  tooltip for the button
     * @return button
     */
    public JButton doMakeRemoveControl(String tooltip) {
        return GuiUtils.makeJButton("Remove display", new Object[] {
            "-listener", this, "-tooltip", tooltip, "-command", CMD_REMOVE
        });
    }


    /**
     * Show the color dialog
     */
    public void showColorDialog() {
        showColorDialog("setColor");
    }

    /**
     * Show the color dialog and invoke the named method
     * @param methodName    the method name to invoke
     */
    public void showColorDialog(String methodName) {
        Color newColor = JColorChooser.showDialog(null, "Choose Color",
                             color);
        if (newColor != null) {
            try {

                Method theMethod = Misc.findMethod(this.getClass(),
                                       methodName,
                                       new Class[] { newColor.getClass() });
                if (theMethod == null) {
                    throw new NoSuchMethodException("unknown method "
                            + methodName);
                } else {
                    theMethod.invoke(this, new Object[] { newColor });
                }
            } catch (Exception exc) {
                logException("Setting color with:" + methodName, exc);
            }
        }
    }


    /**
     * Set the color for the selector. Used by persistence.
     *
     * @param c  color to use
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the color in VisAD
     */
    public void setColor(Color c) throws RemoteException, VisADException {
        color = c;
        if (color != null) {
            for (int i = 0; i < colorSwatches.size(); i++) {
                ((JComponent) colorSwatches.get(i)).setBackground(color);
            }
            if (colorComboBox != null) {
                okToFireEvent = false;
                colorComboBox.setSelectedItem(
                    getDisplayConventions().getColorName(c));
                okToFireEvent = true;
            }
        }
        if (getHaveInitialized()) {
            applyColor();
        }
    }

    /**
     * Get the color for the selector. Used by persistence.
     *
     * @return color being used.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Get the range to use to apply to displayables
     *
     * @return the range for displayables
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Range getRangeToApply() throws RemoteException, VisADException {
        return getRange();
    }


    /**
     * Get the range for the parameter
     *
     * @return range being used
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error getting the range in VisAD
     */
    public Range getRange() throws RemoteException, VisADException {
        if (colorRange == null) {
            colorRange = getInitialRange();
        }
        return colorRange;
    }

    /**
     * Get the range for the data
     *
     * @return range of data
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error getting the range in VisAD
     */
    public Range getSelectRange() throws RemoteException, VisADException {
        if (selectRange == null) {
            selectRange = getColorRangeFromData();
        }
        return selectRange;
    }

    /**
     * Get the range for the color table.
     *
     * @return range being used
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error getting the range in VisAD
     */
    public Range getRangeForColorTable()
            throws RemoteException, VisADException {
        return getRange();
    }

    /**
     * Set the range (from the colortablewidget)
     *
     * @param whichColortable Defines which color table to use. Default is the main one.
     * @param newRange The value
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setRange(String whichColortable, Range newRange)
            throws RemoteException, VisADException {
        setRange(newRange);
    }

    /**
     * Set the color table from the colortablewidget
     *
     * @param whichColorTable Defines which color table to use. Default is the main one.
     * @param newColorTable The new colortable
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setColorTable(String whichColorTable,
                              ColorTable newColorTable)
            throws RemoteException, VisADException {
        setColorTable(newColorTable);
    }

    /**
     * Revert to the default
     *
     * @param whichColorTable Which  one. Default is the main one.
     */
    protected void revertToDefaultColorTable(String whichColorTable) {
        revertToDefaultColorTable();
    }

    /**
     * Revert to the default
     *
     * @param whichColorTable Which  one. Default is the main one.
     */
    protected void revertToDefaultRange(String whichColorTable) {
        revertToDefaultRange();
    }


    /**
     * Set the range for the color table.
     *
     * @param newRange   range to use
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the range in VisAD
     */
    public void setRange(Range newRange)
            throws RemoteException, VisADException {

        colorRange = newRange;
        if (colorRange != null) {
            if (ctw != null) {
                ctw.setRange(colorRange);
            }
            if (getHaveInitialized()) {
                applyRange();
            }
        }
    }

    /**
     * Set the range for the select data.
     *
     * @param newRange   range to use
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the range in VisAD
     */
    public void setSelectRange(Range newRange)
            throws RemoteException, VisADException {

        selectRange = newRange;
        if (selectRange != null) {
            if (selectRangeWidget != null) {
                selectRangeWidget.setRange(selectRange);
                if ( !selectRangeWidget.getSelectRangeEnabled()) {
                    return;
                }
            }
            if (getHaveInitialized()) {
                applySelectRange();
            }
        }
    }




    /**
     * Set the color table and propagate the change to any shared objects.
     *
     * @param newColorTable  new ColorTable
     * @param fromUser  true if the user is setting the color table
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the color table in VisAD
     */
    private void setColorTable(ColorTable newColorTable, boolean fromUser)
            throws RemoteException, VisADException {
        if ((colorTable != null) && (newColorTable != null)) {
            if (colorTable.equalsTable(newColorTable)) {
                return;
            }
            if ( !colorTable.getName().equals(newColorTable.getName())) {
                resetDimness();
            }

        } else {
            resetDimness();
        }
        if (newColorTable == null) {
            this.colorTable = null;
            return;
        }
        this.colorTable = new ColorTable(newColorTable);
        if ((ctw != null) && (colorTable != null)) {
            ctw.setColorTable(colorTable);
        }
        if (getHaveInitialized()) {
            deactivateDisplays();
            applyColorTable();
            if (fromUser) {
                doShare(SHARE_COLORTABLE, colorTable);
            }
            activateDisplays();
        }
    }

    /*
     *  For now we'll comment this out - we used to use it to persist off the name, not the color table
     *  itself. Now we persist off the color table.
     * public String getColorTableName () {
     *   return (colorTable!=null?colorTable.getName ():null);
     *   }
     */


    /**
     * This method is for legacy bundles that used to save off the
     * color table name instead of the color table.
     *
     * @param n The name of the color table
     */
    public void setColorTableName(String n) {
        colorTableName = n;
        if (haveInitialized) {
            ColorTable ct =
                controlContext.getColorTableManager().getColorTable(
                    colorTableName);
            try {
                setColorTable(ct);
            } catch (Exception exc) {
                logException("Setting color table", exc);
            }
        }
    }


    /**
     * Set the {@link ucar.unidata.util.ColorTable} property.
     *
     * @param newColorTable The new value for ColorTable
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the color table in VisAD
     */
    public void setColorTable(ColorTable newColorTable)
            throws RemoteException, VisADException {
        setColorTable(newColorTable, true);
    }


    /**
     * Get the {@link ucar.unidata.util.ColorTable} property.
     *
     * @return The ColorTable
     */
    public ColorTable getColorTable() {
        return colorTable;
    }


    /**
     * Set the display list color property.
     *
     * @param newColor The new color
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the color in VisAD
     */
    public void setDisplayListColor(Color newColor)
            throws RemoteException, VisADException {
        setDisplayListColor(newColor, true);
    }

    /**
     * Set the display list color property.
     *
     * @param newColor The new color
     * @param fromUser true if this is from the user
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the color in VisAD
     */
    protected void setDisplayListColor(Color newColor, boolean fromUser)
            throws RemoteException, VisADException {
        for (Enumeration e =
                displayListTable.elements(); e.hasMoreElements(); ) {
            ((DisplayableData) e.nextElement()).setColor(newColor);
        }
        displayListColor = newColor;
        if (fromUser) {
            displayListUsesColor = false;
        }
    }


    /**
     * Get the DisplayListColor property.
     *
     * @return The display list color
     */
    public Color getDisplayListColor() {
        return displayListColor;
    }


    /**
     * Assume that any display controls that have a color table widget
     * will want the color table to show up in the legend.
     *
     * @param  legendType  type of legend
     * @return The extra JComponent to use in legend
     */
    protected JComponent getExtraLegendComponent(int legendType) {
        if (ctw != null) {
            return ctw.getLegendPanel(legendType);
        }
        return null;
    }


    /**
     * Get the ColorTableWidget using the specified range.
     *
     * @param r range for the color table
     * @return widget
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the color table range in VisAD
     */
    public ColorTableWidget getColorTableWidget(Range r)
            throws VisADException, RemoteException {
        if (ctw == null) {
            if (colorTable == null) {
                colorTable = getOldColorTableOrInitialColorTable();
            }
            ctw = new ColorTableWidget(this,
                                       controlContext.getColorTableManager(),
                                       ((colorTable != null)
                                        ? colorTable
                                        : controlContext
                                            .getColorTableManager()
                                            .getDefaultColorTable()), r);
            addRemovable(ctw);
        } else if (r != null) {
            ctw.setRange(r);
        }
        return ctw;
    }

    /**
     * Get the SelectRangeWidget using the specified range.
     *
     * @param r range for the color table
     * @return widget
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the color table range in VisAD
     */
    public SelectRangeWidget getSelectRangeWidget(Range r)
            throws VisADException, RemoteException {
        if (selectRangeWidget == null) {
            selectRangeWidget = new SelectRangeWidget(this, r);
            addRemovable(selectRangeWidget);
        }
        return selectRangeWidget;
    }

    /**
     * Get the LineWidthWidget
     *
     * @return widget
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the color table range in VisAD
     */
    public ValueSliderWidget getLineWidthWidget()
            throws VisADException, RemoteException {
        if (lww == null) {
            lww = new ValueSliderWidget(this, 1, 10, "lineWidth",
                                        getLineWidthWidgetLabel());
            addRemovable(lww);
        }
        return lww;
    }

    /**
     * A wrapper around doMakeColorControl (Color color), passing
     * in null as the Color argument.
     *
     * @return color selector component
     * @see #doMakeColorControl(Color)
     */
    public Component doMakeColorControl() {
        return doMakeColorControl(null);
    }


    /**
     * This creates a gui control for selecting a Color.
     * It uses the list of Colors defined by the idv.DisplayConventions
     * class. If the color argument is non-null then the corresponding
     * String name of the color is retrieved from idv.DisplayConventions.
     * If found the name is used to set the selected item on the JComboBox.
     *
     * @param color  default
     * @return color selector component
     */
    public Component doMakeColorControl(Color color) {
        colorComboBox =
            new JComboBox(getDisplayConventions().getColorNameList());

        if (color != null) {
            String colorName = getDisplayConventions().getColorName(color);
            if (colorName != null) {
                colorComboBox.setSelectedItem(colorName);
            }
        }
        colorComboBox.addActionListener(this);
        colorComboBox.setActionCommand(CMD_COLORS);
        return colorComboBox;
    }

    /**
     * Get the vertical position for a constant map from a
     * value in the range of -1.0 to 1.0.
     *
     * @param value  value to scale to vertical coordinates
     * @return scaled value in vertical coordinates
     */
    public double getVerticalValue(double value) {
        double returnValue = value;
        returnValue = (value < -1.0)
                      ? -1.0
                      : (value > 1.0)
                        ? 1.0
                        : value;
        if (getNavigatedDisplay() != null) {
            DisplayRealType drt =
                getNavigatedDisplay().getDisplayAltitudeType();
            double[] range    = new double[2];
            boolean  hasRange = drt.getRange(range);
            if (hasRange) {
                double pcnt = (returnValue - (-1)) / 2;
                returnValue = Math.min((range[0]
                                        + (range[1] - range[0])
                                          * pcnt), range[1]);
            }
        }
        return returnValue;
    }

    /**
     * Set the help URL for this DisplayControl
     *
     * @param helpUrl  URL for the help.
     */
    public void setHelpUrl(String helpUrl) {
        this.helpUrl = helpUrl;
    }

    /**
     * Wrapper around ControlContext.getProperty method
     *
     * @param  name  property name
     * @param  dflt  default value
     * @return The boolean value of the given property or the dflt if undefined
     */
    public boolean getProperty(String name, boolean dflt) {
        return getControlContext().getProperty(name, dflt);
    }

    /**
     * Wrapper around ControlContext.getProperty method
     *
     * @param  name  property name
     * @param  dflt  default value
     * @return The String value of the given property or the dflt if undefined
     */
    public String getProperty(String name, String dflt) {
        return getControlContext().getProperty(name, dflt);
    }

    /**
     * Return the object store from the getControlContext
     *
     * @return The object store to use.
     */
    public XmlObjectStore getObjectStore() {
        return getControlContext().getObjectStore();
    }

    /**
     * Set whether it's okay to fire events to listeners.
     *
     * @param v true if okay.
     */
    public void setOkToFireEvents(boolean v) {
        okToFireEvent = v;
    }

    /**
     * Get whether it's okay to fire events to listeners.
     *
     * @return true if okay.
     */
    public boolean getOkToFireEvents() {
        return okToFireEvent;
    }

    /**
     * Used by the isl to override selective parameters in the default contourInfo
     *
     * @param s The param string.
     */
    public void setContourInfoParams(String s) {
        contourInfoParams = s;
    }

    /**
     * Set the contour parameters for any contours
     *
     * @param  newInfo  the new contour information
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the contour info in VisAD
     */
    public void setContourInfo(ContourInfo newInfo)
            throws VisADException, RemoteException {
        if (newInfo == null) {
            contourInfo = null;
            return;
        }
        contourInfo = new ContourInfo(newInfo);
        if (contourWidget != null) {
            contourWidget.setContourInfo(newInfo);
        }
        applyContourInfo();
    }


    /**
     * Get the contour information for any contours
     *
     * @return  the contour information
     */
    public ContourInfo getContourInfo() {
        return contourInfo;
    }

    /**
     * Set the shared color scale info
     *
     * @param newInfo  the new information
     *
     * @throws RemoteException    remote problem
     * @throws VisADException     VisAD problem
     */
    public void setSharedColorScaleInfo(ColorScaleInfo newInfo)
            throws VisADException, RemoteException {
        if (newInfo == null) {
            return;
        }
        //Copy everything except orientation
        newInfo = new ColorScaleInfo(newInfo);
        if (colorScaleInfo != null) {
            newInfo.setPlacement(colorScaleInfo.getPlacement());
            newInfo.setOrientation(colorScaleInfo.getOrientation());
        }
        colorScaleInfo = newInfo;
        applyColorScaleInfo();
    }


    /**
     * Set the color scale parameters for any color scale
     *
     * @param  newInfo  the new contour information
     * @throws RemoteException  some RMI exception occured
     * @throws VisADException  error setting the contour info in VisAD
     */
    public void setColorScaleInfo(ColorScaleInfo newInfo)
            throws VisADException, RemoteException {
        if (newInfo == null) {
            colorScaleInfo = null;
            return;
        }
        colorScaleInfo = new ColorScaleInfo(newInfo);
        applyColorScaleInfo();

    }




    /**
     * Get the contour information for any contours
     *
     * @return  the contour information
     */
    public ColorScaleInfo getColorScaleInfo() {
        if (colorScaleInfo == null) {
            colorScaleInfo = getDefaultColorScaleInfo();
        }
        return colorScaleInfo;
    }



    /** Used to hold Data when persisting. Not really using this now */
    private Hashtable cachedData;

    /**
     * Get the cache of data. Not being used now.
     *
     * @return Data cache
     */
    public Hashtable getCachedData() {
        //For now don't persist the cache
        if (true) {
            return null;
        }

        if ( !controlContext.getPersistenceManager().getSaveData()) {
            return null;
        }

        if ((dataInstances == null) || (dataInstances.size() == 0)) {
            return null;
        }
        Hashtable cache = new Hashtable();
        try {
            for (int i = 0; i < dataInstances.size(); i++) {
                DataInstance dataInstance =
                    (DataInstance) dataInstances.get(i);
                Data data = dataInstance.getData();
                cache.put(dataInstance.getDataChoice().getId(), data);
            }
        } catch (Exception exc) {
            logException("Making data cache", exc);
            return null;
        }
        return cache;
    }


    /**
     * Set the data cache. Not being used now.
     *
     * @param cache Data cache
     */
    public void setCachedData(Hashtable cache) {
        cachedData = cache;
    }





    /**
     * Sets the list of <code>DataInstances</code> for this
     * DisplayControl.  May be null.
     *
     * @param l  <code>List</code> of <code>DataInstances</code>
     */
    public void setDataInstances(List l) {
        dataInstances = l;
    }

    /**
     * Get the scaling factor for probes and such. The scaling is
     * the parameter that gets passed to TextControl.setSize() and
     * ShapeControl.setScale().
     *
     * @return ratio of the current matrix scale factor to the
     *         saved matrix scale factor.
     * @throws VisADException problem determining scale
     * @throws RemoteException problem determining scale for remote display
     */
    public float getDisplayScale() throws VisADException, RemoteException {
        NavigatedDisplay navDisplay = getNavigatedDisplay();
        if (navDisplay != null) {
            return navDisplay.getDisplayScale();
        }
        return 1.0f;
    }

    /**
     * Get the perferred sampling mode.
     *
     * @return sampling mode (WEIGHTED_AVERAGE, NEAREST_NEIGHBOR)
     */
    public String getDefaultSamplingMode() {
        return defaultSamplingMode;
    }

    /**
     * Set sampling mode.
     *
     * @param newMode  String name of sampling mode
     */
    public void setDefaultSamplingMode(String newMode) {
        defaultSamplingMode = newMode;
    }

    /**
     * Get the sampling mode
     *
     * @return sampling mode
     */
    public int getSamplingModeValue() {
        return getSamplingModeValue(defaultSamplingMode);
    }


    /**
     * Get the integer value for the sampling mode supplied
     *
     * @param samplingMode  String name of sampling mode
     * @return value to use in resampling
     */
    public int getSamplingModeValue(String samplingMode) {
        return Misc.equals(samplingMode, WEIGHTED_AVERAGE)
               ? Data.WEIGHTED_AVERAGE
               : Data.NEAREST_NEIGHBOR;
    }

    /**
     * Get the name of the sampling mode
     *
     * @param  mode  String name of sampling mode
     * @return value to use in resampling
     */
    public String getSamplingModeName(int mode) {
        return ((mode == Data.WEIGHTED_AVERAGE)
                ? WEIGHTED_AVERAGE
                : NEAREST_NEIGHBOR);
    }


    /**
     * Get the integer value of the default sampling mode
     *
     * @return  integer value of default sampling mode
     */
    public int getDefaultSamplingModeValue() {
        return getSamplingModeValue((defaultSamplingMode == null)
                                    ? DEFAULT_SAMPLING_MODE
                                    : defaultSamplingMode);
    }

    /**
     * This is the category that is defined for this control.
     * It is mostly used by the legend guis to organise the presentation
     * of the display controls.
     *
     * @param value The new value for the display category.
     */
    public void setDisplayCategory(String value) {
        displayCategory = value;
        if (displayCategory != null) {
            allCategories.put(displayCategory, displayCategory);
        }
    }

    /**
     * This is the category that is defined for this control.
     * It is mostly used by the legend guis to organise the presentation
     * of the display controls.
     *
     * @return The display category.
     */
    public String getDisplayCategory() {
        return displayCategory;
    }


    /**
     * Method to get a list of ViewManagers that are applicable to this
     * DisplayControl.
     *
     * @return <code>List</code> of view managers associated with this
     *         DisplayControl's Displayables.
     */
    public List getViewManagers() {
        List displayList  = getDisplayInfos();
        List viewManagers = new ArrayList();
        for (int i = 0; i < displayList.size(); i++) {
            DisplayInfo info = (DisplayInfo) displayList.get(i);
            ViewManager vm   = info.getViewManager();
            if ( !viewManagers.contains(vm)) {
                viewManagers.add(vm);
                //System.out.println("viewManager-"+i+" = "+vm);
            }
        }
        return viewManagers;
    }

    /**
     * Set the visibility of the color scale.
     *
     * @param viz true for color scales to be visible
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setColorScaleVisible(boolean viz)
            throws VisADException, RemoteException {
        getColorScaleInfo().setIsVisible(viz);
        applyColorScaleInfo();
    }

    /**
     *  Set the HadDataChoices property.
     *
     *  @param value The new value for HadDataChoices
     */
    public void setHadDataChoices(boolean value) {
        hadDataChoices = value;
    }

    /**
     *  Get the HadDataChoices property.
     *
     *  @return The HadDataChoices
     */
    public boolean getHadDataChoices() {
        return (myDataChoices != null) && (myDataChoices.size() > 0);
    }

    /**
     * Set the name of the data choice
     *
     * @param s the name
     */
    public void setOriginalDataChoicesLabel(String s) {
        this.originalDataChoicesLabel = s;
    }


    /**
     * Get the name of the data choice, if there is one, when we are saving off without data
     *
     * @return datachoice name
     */
    public String getOriginalDataChoicesLabel() {
        if (controlContext != null) {
            if ( !controlContext.getPersistenceManager()
                    .getSaveDataSources()) {
                if ((myDataChoices != null) && (myDataChoices.size() > 0)) {
                    return "" + myDataChoices.get(0);
                }
            }
        }
        return "";
    }



    /**
     *  Set the TemplateName property.
     *
     *  @param value The new value for TemplateName
     */
    public void setTemplateName(String value) {
        templateName = value;
    }

    /**
     *  Get the TemplateName property.
     *
     *  @return The TemplateName
     */
    public String getTemplateName() {
        if (controlContext != null) {
            return controlContext.getPersistenceManager()
                .getCurrentTemplateName();
        }
        return null;
    }



    /**
     * Set the NameFromUser property.
     * This is kept around for legacy bundles
     *
     * @param value The new value for NameFromUser
     */
    public void setNameFromUser(String value) {
        legendLabelTemplate = value;
    }



    /**
     * Set the Name property.
     * Keep around for legacy bundles.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        id = value;
    }



    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the collapsed legend property.
     *
     * @param value The new value for collapsedLegend
     */
    public void setCollapseLegend(boolean value) {
        collapseLegend = value;
    }

    /**
     * Get the collapsed legend property.
     *
     * @return The collapsed legend property
     */
    public boolean getCollapseLegend() {
        return collapseLegend;
    }


    /**
     * Find a property on the display control. These properties are not saved.
     *
     * @param key The key
     *
     * @return The value
     */
    public Object getTransientProperty(Object key) {
        return transientProperties.get(key);
    }

    /**
     * Put a property on the display control. These properties are not saved.
     *
     * @param key The key
     * @param value The value
     */
    public void putTransientProperty(Object key, Object value) {
        transientProperties.put(key, value);
    }

    /**
     * Apply preferences to this control.  Subclasses should override
     * if needed.
     */
    public void applyPreferences() {
        updateLegendAndList();
    }

    /**
     * Set the ZPosition property.
     *
     * @param value The new value for ZPosition
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void setZPosition(double value)
            throws RemoteException, VisADException {
        setZPosition(value, false);
    }

    /**
     * Set the Z position
     *
     * @param value  the value
     * @param fromSlider  true if from slider
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void setZPosition(double value, boolean fromSlider)
            throws RemoteException, VisADException {
        zPosition = value;
        if (getHaveInitialized()) {
            applyZPosition();
        }
        if ((zPositionSlider != null) && !fromSlider) {
            zPositionSlider.setValue(value);
        }
    }


    /**
     * Set the line width property.
     *
     * @param value The new value for line width
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void setLineWidth(int value)
            throws RemoteException, VisADException {
        lineWidth = value;
        if (getHaveInitialized()) {
            applyLineWidth();
        }
        if (lww != null) {
            lww.setValue(value);
        }
    }

    /**
     * Get the line width property.
     *
     * @return The line width
     */
    public int getLineWidth() {
        return lineWidth;
    }


    /**
     * Get the initial Z position
     *
     * @return the position in Z space
     */
    protected double getInitialZPosition() {
        double           retVal     = -1;
        NavigatedDisplay navDisplay = getNavigatedDisplay();
        if (navDisplay != null) {
            DisplayRealType drt      = navDisplay.getDisplayAltitudeType();
            double[]        range    = new double[2];
            boolean         hasRange = drt.getRange(range);
            double          defVal   = drt.getDefaultValue();
            if (hasRange) {
                double pcnt = Math.abs((defVal - range[0])
                                       / Math.abs(range[1] - range[0]));
                // find percentage along a -1 to 1 range
                retVal = -1 + 2 * pcnt;
            }
        }
        return retVal;
    }

    /**
     * Get the ZPosition property.
     *
     * @return The ZPosition
     */
    public double getZPosition() {
        if (zPosition == Double.MIN_VALUE) {
            zPosition = getInitialZPosition();
        }
        return zPosition;
    }

    /**
     * Update the animation for the view manager
     */
    protected void updateAnimation() {
        if (internalAnimation != null) {
            internalAnimation.reCalculateAnimationSet();
        }
        if (viewAnimation != null) {
            viewAnimation.reCalculateAnimationSet();
        }
    }


    /**
     * Get some animation.
     *
     * @return The animation
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    public Animation getSomeAnimation()
            throws VisADException, RemoteException {
        return getAnimation();

    }



    /**
     * Get the animation
     *
     * @return The animation
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    public Animation getAnimation() throws VisADException, RemoteException {
        return getAnimation(false, null);
    }


    /**
     * Get the Animation for this display.
     *
     * @param createOurOwn If true then we create our own. Else we
     * get it from the ViewManager we are displayed in.
     *
     * @return The Animation
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    protected Animation getAnimation(boolean createOurOwn)
            throws VisADException, RemoteException {
        return getAnimation(createOurOwn, null);
    }

    /**
     * Create, if needed, and return  the Animation to use
     *
     * @param timeType The time type
     *
     * @return The Animation
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    protected Animation getAnimation(RealType timeType)
            throws VisADException, RemoteException {
        return getAnimation(true, timeType);
    }


    /**
     * Get the Animation for this display.
     *
     * @param createOurOwn If true then we create our own. Else we
     * get it from the ViewManager we are displayed in.
     * @param timeType Time type
     *
     * @return The Animation
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    protected Animation getAnimation(boolean createOurOwn, RealType timeType)
            throws VisADException, RemoteException {

        if (createOurOwn) {
            return getInternalAnimation(timeType);
        }

        //For backwards compatability
        if (internalAnimation != null) {
            return internalAnimation;
        }

        return getViewAnimation();
    }



    /**
     * Get the Animation that is from the view manager for this control
     *
     * @return The Animation
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    public Animation getViewAnimation()
            throws VisADException, RemoteException {
        if (viewAnimation == null) {
            ViewManager viewManager = null;
            //First check for our own ViewManager
            if ((viewManagers != null) && (viewManagers.size() > 0)) {
                viewManager   = (ViewManager) viewManagers.get(0);
                viewAnimation = viewManager.getAnimation();
            }
            if (viewAnimation == null) {
                viewManager = getViewManager();
                if (viewManager == null) {
                    return null;
                }
                viewAnimation = viewManager.getAnimation();
            }

            if (viewAnimation == null) {
                return null;
            }
            viewAnimation.addPropertyChangeListener(this);
        }
        return viewAnimation;
    }







    /**
     * Create if needed and return an Animation
     *
     * @return The Animation
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    protected Animation getInternalAnimation()
            throws VisADException, RemoteException {
        return getInternalAnimation(null);
    }


    /**
     * Create if needed and return an Animation
     *
     *
     * @param timeType The realtype of the time set
     * @return The Animation
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    protected Animation getInternalAnimation(RealType timeType)
            throws VisADException, RemoteException {

        if (internalAnimation != null) {
            return internalAnimation;
        }
        if (timeType == null) {
            internalAnimation = new Animation();
        } else {
            internalAnimation = new Animation(timeType);
        }
        internalAnimation.addPropertyChangeListener(this);
        return internalAnimation;
    }




    /**
     * Create, if needed, and return the AnimationWidget
     *
     * @return The animation widget
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    public AnimationWidget getAnimationWidget()
            throws VisADException, RemoteException {
        if (animationWidget == null) {
            if (animationInfo == null) {
                animationInfo =
                    (AnimationInfo) getIdv().getPersistenceManager()
                        .getPrototype(AnimationInfo.class);
                if (animationInfo == null) {
                    animationInfo = new AnimationInfo();
                }
            }
            animationWidget = new AnimationWidget(null, null, animationInfo);
            Animation animation = getInternalAnimation();
            animation.setAnimationInfo(animationInfo);
            if ( !initializationDone) {
                animationWidget.setSharing(false);
            }
            animationWidget.setAnimation(animation);
        }
        return animationWidget;
    }


    /**
     * Check if initialization is done
     *
     * @return true if done
     */
    public boolean isInitDone() {
        return initializationDone;
    }


    /**
     * Set the ShowInLegend property.
     *
     * @param value The new value for ShowInLegend
     */
    public void setShowInLegend(boolean value) {
        showInLegend = value;
    }

    /**
     * Get the ShowInLegend property.
     *
     * @return The ShowInLegend
     */
    public boolean getShowInLegend() {
        return showInLegend;
    }

    /**
     * This method can be overwritten by the derived classes that do not want the
     * general application of the fast rendering flag.
     *
     * @return Should fast rendering logic be used.
     */
    protected boolean shouldApplyFastRendering() {
        return true;
    }

    /**
     * Get the default for fast rendering
     *
     * @return  true to use fast rendering
     */
    protected boolean getDefaultFastRendering() {
        return true;
    }

    /**
     * Get the initial fast rendering property
     *
     * @return the initial fast rendering property
     */
    protected boolean getInitialFastRendering() {
        return controlContext.getObjectStore().get(
            IdvConstants.PREF_FAST_RENDER, getDefaultFastRendering());
    }


    /**
     * call setUseFastRendering on all of the displayables
     */
    private void applyUseFastRendering() {
        if ( !shouldApplyFastRendering()) {
            return;
        }
        if (haveInitialized) {
            List displayList = getDisplayInfos();
            for (int i = 0, n = displayList.size(); i < n; i++) {
                DisplayInfo info = (DisplayInfo) displayList.get(i);
                try {
                    Displayable displayable = info.getDisplayable();
                    if (displayable != null) {
                        displayable.setUseFastRendering(
                            info.getViewManager().getUseFastRendering(
                                useFastRendering));
                    }
                } catch (Exception exc) {
                    logException("Setting fast rendering to: "
                                 + useFastRendering, exc);
                }
            }
        }
    }


    /**
     * Set the UseFastRendering property.
     *
     * @param value The new value for UseFastRendering
     */
    public void setUseFastRendering(boolean value) {
        useFastRendering = value;
        applyUseFastRendering();

    }

    /**
     * Get the UseFastRendering property.
     *
     * @return The UseFastRendering
     */
    public boolean getUseFastRendering() {
        return useFastRendering;
    }

    /**
     * A utility method to set the animation set from the given list of times
     * of the animation
     *
     * @param dateTimes List of DateTime objects
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void setAnimationSet(List dateTimes)
            throws VisADException, RemoteException {
        Set set = null;
        if ((dateTimes != null) && !dateTimes.isEmpty()) {
            DateTime[] timeArray = new DateTime[dateTimes.size()];
            for (int i = 0; i < timeArray.length; i++) {
                timeArray[i] = (DateTime) dateTimes.get(i);
            }
            set = CalendarDateTime.makeTimeSet(timeArray);
        }
        getAnimationWidget().setBaseTimes(set);
    }

    /**
     * Set the AnimationInfo property.
     *
     * @param value The new value for AnimationInfo
     */
    public void setAnimationInfo(AnimationInfo value) {
        animationInfo = value;
    }

    /**
     * Get the AnimationInfo property.
     *
     * @return The AnimationInfo
     */
    public AnimationInfo getAnimationInfo() {
        if (animationWidget != null) {
            return animationWidget.getAnimationInfo();
        }
        return animationInfo;
    }





    /**
     * Create a  lock button for the given display control.
     *
     * @return The button used to lock the toggling of visibility
     */
    protected JButton makeLockButton() {
        final JButton lockBtn = new JButton(ICON_LOCK);
        lockBtn.setContentAreaFilled(false);
        lockBtn.setBorder(BorderFactory.createEmptyBorder());
        lockButtons.add(lockBtn);
        updateLockButton(lockBtn);
        lockBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setLockVisibilityToggle( !getLockVisibilityToggle());
            }
        });
        return lockBtn;
    }


    /**
     * Change the icon in the lock button for the given display control.
     *
     * @param lockBtn The button.
     */
    protected void updateLockButton(JButton lockBtn) {
        boolean isLocked = getLockVisibilityToggle();
        lockBtn.setIcon(isLocked
                        ? ICON_LOCK
                        : ICON_UNLOCK);
        lockBtn.setToolTipText(
            Msg.msg(
                "When locked this display control is not affected by the visibility toggling"));
    }




    /**
     * Create an icon  button for removing the given display control.
     *
     * @return The button used to lock the toggling of visibility
     */
    protected JButton makeRemoveButton() {
        JButton removeBtn = GuiUtils.getImageButton(ICON_REMOVE);
        removeBtn.setToolTipText("Remove the display");
        removeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    doRemove();
                } catch (Exception exc) {
                    logException("Removing display", exc);
                }
            }
        });
        removeBtn.setBackground(null);
        return removeBtn;
    }


    /**
     * Set the ViewManagerClasses property.
     *
     * @param value The new value for ViewManagerClasses
     */
    public void setViewManagerClassNames(String value) {
        viewManagerClassNames = value;
    }

    /**
     * Get the ViewManagerClasses property.
     *
     * @return The ViewManagerClasses
     */
    public String getViewManagerClassNames() {
        return viewManagerClassNames;
    }


    /**
     * Set the ColorDimness property.
     *
     * @param value The new value for ColorDimness
     */
    public void setColorDimness(float value) {
        colorDimness = value;
    }

    /**
     * Get the ColorDimness property.
     *
     * @return The ColorDimness
     */
    public float getColorDimness() {
        return colorDimness;
    }

    /**
     * Set the DataTimeRange property.
     *
     * @param value The new value for DataTimeRange
     */
    public void setDataTimeRange(DataTimeRange value) {
        dataTimeRange = value;
    }

    /**
     * Get the DataTimeRange property.
     *
     * @return The DataTimeRange
     */
    public DataTimeRange getDataTimeRange() {
        return dataTimeRange;
    }

    /**
     * Get the DataTimeRange property.
     *
     * @param createIfNeeded If true then create it
     *
     * @return The date time range
     */
    public DataTimeRange getDataTimeRange(boolean createIfNeeded) {
        if (createIfNeeded && (dataTimeRange == null)) {
            dataTimeRange = new DataTimeRange();
        }
        return dataTimeRange;
    }


    /**
     * Set the UseTimesInAnimation property.
     *
     * @param value The new value for UseTimesInAnimation
     */
    public void setUseTimesInAnimation(boolean value) {
        useTimesInAnimation = value;
        try {
            if (haveInitialized) {
                List infos = getDisplayInfos();
                for (int i = 0, n = infos.size(); i < n; i++) {
                    DisplayInfo info = (DisplayInfo) infos.get(i);
                    info.getDisplayable().setUseTimesInAnimation(value);
                }
            }
        } catch (Exception exc) {
            logException("Setting use times in animation", exc);
        }
    }

    /**
     * Get the UseTimesInAnimation property.
     *
     * @return The UseTimesInAnimation
     */
    public boolean getUseTimesInAnimation() {
        return useTimesInAnimation;
    }

    /**
     * Utility to convert the given raw data range into the display units
     *
     * @param rawRange Raw data range
     * @param rawUnit  the raw unit for the range
     *
     * @return Converted range
     */
    public Range convertColorRange(Range rawRange, Unit rawUnit) {
        return convertRange(rawRange, rawUnit, getUnitForColor());
    }


    /**
     * Utility to convert the given raw data range into the display units
     *
     * @param rawRange Raw data range
     * @param rawUnit  the raw unit for the range
     * @param outUnit  the converted unit
     *
     * @return Converted range
     */
    public Range convertRange(Range rawRange, Unit rawUnit, Unit outUnit) {
        return Util.convertRange(rawRange, rawUnit, outUnit);
        /*
        if ( !Misc.equals(rawUnit, outUnit)) {
            if ((rawUnit != null) && (outUnit != null)) {
                try {
                    rawRange = new Range(outUnit.toThis(rawRange.getMin(),
                            rawUnit), outUnit.toThis(rawRange.getMax(),
                                rawUnit));
                } catch (Exception e) {
                    rawRange = null;
                }
            }
        }
        return rawRange;
        */
    }

    /**
     * Set the ExpandedInTabs property.
     *
     * @param value The new value for ExpandedInTabs
     */
    public void setExpandedInTabs(boolean value) {
        expandedInTabs = value;
    }

    /**
     * Get the ExpandedInTabs property.
     *
     * @return The ExpandedInTabs
     */
    public boolean getExpandedInTabs() {
        return expandedInTabs;
    }




    /**
     * Set the ShowInTabs property.
     *
     * @param value The new value for ShowInTabs
     */
    public void setShowInTabs(boolean value) {
        showInTabs = value;
    }

    /**
     * Get the ShowInTabs property.
     *
     * @return The ShowInTabs
     */
    public boolean getShowInTabs() {
        return showInTabs;
    }

    /**
     * Should this be docked
     *
     * @return  true if should be docked
     */
    public boolean shouldBeDocked() {
        return getShowInTabs() && (componentHolder == null);
    }

    /**
     * Can this be docked
     *
     * @return true if can be docked
     */
    public boolean canBeDocked() {
        return componentHolder == null;
    }


    /**
     *  Set the Version property.
     *
     *  @param value The new value for Version
     */
    public void setVersion(double value) {
        versionWasSet = true;
        version       = value;
    }

    /**
     *  Get the Version property.
     *
     *  @return The Version
     */
    public double getVersion() {
        //We hard code this so we can disambiguate old versions from new when unbundling
        return CURRENT_VERSION;
    }

    /**
     * See if we were unpersisted
     *
     * @return true if we were
     */
    protected boolean getWasUnPersisted() {
        return wasUnPersisted;
    }


    /**
     * Set the DisplayListTemplate property.
     *
     * @param value The new value for DisplayListTemplate
     */
    public void setDisplayListTemplate(String value) {
        //System.err.println("display list template: " + value);
        displayListTemplate = value;
    }

    /**
     * Get the DisplayListTemplate property.
     *
     * @return The DisplayListTemplate
     */
    public String getDisplayListTemplate() {
        if (displayListTemplate == null) {
            boolean haveData = (getShortParamName() != null);
            displayListTemplate = getStore().get(PREF_DISPLAYLIST_TEMPLATE
                    + "." + displayId, (String) null);
            if (displayListTemplate == null) {
                String pref = PREF_DISPLAYLIST_TEMPLATE + (haveData
                        ? ".data"
                        : ".nodata");
                displayListTemplate = getStore().get(pref,
                        getDefaultDisplayListTemplate());
            }
        }
        return displayListTemplate;
    }


    /**
     * Get the default display list template for this control.  Subclasses can override
     * @return the default template
     */
    protected String getDefaultDisplayListTemplate() {

        return (getShortParamName() != null)  // haveData
               ? MACRO_SHORTNAME + " - " + MACRO_DISPLAYNAME + " "
                 + MACRO_TIMESTAMP
               : MACRO_DISPLAYNAME;
    }


    /**
     * Set the LegendLabel property.
     *
     * @param value The new value for LegendLabel
     */
    public void setLegendLabelTemplate(String value) {
        legendLabelTemplate = value;
    }

    /**
     * Get the LegendLabel property.
     *
     * @return The LegendLabel
     */
    public String getLegendLabelTemplate() {
        if (legendLabelTemplate == null) {
            boolean haveData = (getShortParamName() != null);
            legendLabelTemplate = getStore().get(PREF_LEGENDLABEL_TEMPLATE
                    + "." + displayId, (String) null);
            if (legendLabelTemplate == null) {
                String pref = PREF_LEGENDLABEL_TEMPLATE + (haveData
                        ? ".data"
                        : ".nodata");
                legendLabelTemplate = getStore().get(pref, (haveData
                        ? MACRO_SHORTNAME + " - " + MACRO_DISPLAYNAME
                        : MACRO_DISPLAYNAME));
            }
        }

        return legendLabelTemplate;
    }







    /**
     * A no-op so unpersisting old bundles won't flag a warning.
     *
     * @param label The label
     */
    public void setLegendLabel(String label) {}



    /**
     * Set the ExtraLabelTemplate property.
     *
     * @param value The new value for ExtraLabelTemplate
     */
    public void setExtraLabelTemplate(String value) {
        extraLabelTemplate = value;
    }

    /**
     * Get the ExtraLabelTemplate property.
     *
     * @return The ExtraLabelTemplate
     */
    public String getExtraLabelTemplate() {
        // If it's null, it's never been set
        //if ((extraLabelTemplate == null) || extraLabelTemplate.isEmpty()) {
        if (extraLabelTemplate == null) {
            boolean haveData = (getShortParamName() != null);
            extraLabelTemplate = getStore().get(PREF_EXTRALABEL_TEMPLATE
                    + "." + displayId, (String) null);
            if (extraLabelTemplate == null) {
                String pref = PREF_EXTRALABEL_TEMPLATE + (haveData
                        ? ".data"
                        : ".nodata");
                extraLabelTemplate = getStore().get(pref, (String) null);
            }
            if ((extraLabelTemplate == null)
                    && canDoProgressiveResolution()) {
                extraLabelTemplate = MACRO_RESOLUTION;
            }
        }
        if (extraLabelTemplate == null) {
            extraLabelTemplate = "";
        }

        return extraLabelTemplate;
    }


    /**
     * Set the SelectRangeEnabled property.
     *
     * @param value The new value for SelectRangeEnabled
     */
    public void setSelectRangeEnabled(boolean value) {
        if (value == selectRangeEnabled) {
            return;
        }
        selectRangeEnabled = value;
        if (getHaveInitialized()) {
            try {
                applySelectRange();
            } catch (Exception exc) {
                logException("Applying selection range", exc);
            }
        }
    }

    /**
     * Get the SelectRangeEnabled property.
     *
     * @return The SelectRangeEnabled
     */
    public boolean getSelectRangeEnabled() {
        return selectRangeEnabled;
    }

    /**
     * Can this display control write out data.
     * @return true if it can
     */
    public boolean canExportData() {
        return false;
    }

    /**
     * Get the DisplayedData
     * @return the data or null
     *
     * @throws RemoteException   problem getting remote data
     * @throws VisADException    problem getting local data
     */
    protected Data getDisplayedData() throws VisADException, RemoteException {
        return null;
    }


    /**
     * Export displayed data to file
     * @param type  type of data
     */
    public void exportDisplayedData(String type) {
        try {
            Data d = getDisplayedData();
            if (d == null) {
                return;
            }
            if (Util.exportAsNetcdf(d)) {
                userMessage(
                    "<html>The displayed data has been exported.<p>Note: this facility is experimental. The exported NetCDF file is not CF compliant and cannot be used within the IDV</html>");
            }
        } catch (Exception e) {
            logException("Unable to export the data", e);
        }
    }

    /**
     * See if the display supports Z positioning.
     * @return true if z positioning is supported
     */
    protected boolean useZPosition() {
        return isDisplay3D() || isInTransectView();
    }

    /**
     *  Use the value of the skip factor to subset the data.
     */
    protected void applySkipFactor() {}


    /**
     * Set the skip value property
     * @param value  new skip value
     */
    public void setSkipValue(int value) {
        skipValue = value;
        if (getHaveInitialized()) {
            applySkipFactor();
            doShare(SHARE_SKIPVALUE, new Integer(skipValue));
        }
    }

    /**
     * Get the skip value property
     * @return the current skip value
     */
    public int getSkipValue() {
        return ((skipSlider == null)
                ? skipValue
                : skipSlider.getValue());
    }

    /**
     * Make the skip factor slider.
     * @return  slider for setting skip factor
     */
    protected Component doMakeSkipFactorSlider() {
        skipSlider = new JSlider(0, 10, skipValue);
        skipSlider.setPaintTicks(true);
        skipSlider.setPaintLabels(true);
        skipSlider.setToolTipText("Change sampling factor");
        skipSlider.setMajorTickSpacing(5);
        skipSlider.setMinorTickSpacing(1);
        skipSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (skipSlider.getValueIsAdjusting()) {
                    return;
                }
                Misc.run(new Runnable() {
                    public void run() {
                        applySkipFactor();
                    }
                });
            }
        });
        return GuiUtils.hgrid(skipSlider, GuiUtils.filler());
    }

    /**
     * If image is bigger than threshold then set the skip value
     *
     * @param image The image
     */
    protected void checkImageSize(FieldImpl image) {
        try {
            int maxSize = getStore().get(IdvConstants.PREF_MAXIMAGESIZE, -1);
            if (maxSize > 0) {
                int[] lengths = ((GriddedSet) GridUtil.getSpatialDomain(
                                    image)).getLengths();
                int sizeX = sizeX = lengths[0];
                int sizeY = lengths[1];
                //            System.err.println("max size:" + maxSize + " image size:" + sizeX
                //                               + "/" + sizeY);
                int skipValue = getSkipValue();
                if ((skipValue == 0) && ( !getWasUnPersisted() || true)) {
                    while (sizeX * sizeY > maxSize) {
                        skipValue++;
                        sizeX = sizeX / (skipValue + 1);
                        sizeY = sizeY / (skipValue + 1);
                        //                          System.err.println("\tskip:" + skipValue +  " " +sizeX+"/"+sizeY+ " " + (sizeX*sizeY));
                    }
                    if (skipSlider != null) {
                        skipSlider.setValue(skipValue);
                    }
                    this.skipValue = skipValue;
                }
            }
        } catch (Exception e) {
            logException("checkImageSize: ", e);
        }
    }

    /**
     * Set the ComponentHolder property.
     *
     * @param value The new value for ComponentHolder
     */
    public void setComponentHolder(IdvComponentHolder value) {
        componentHolder = value;
        if (componentHolder != null) {}
    }

    /**
     * Get the ComponentHolder property.
     *
     * @return The ComponentHolder
     */
    public IdvComponentHolder getComponentHolder() {
        return componentHolder;
    }

    /**
     * Show the color control widget in the widgets if FLAG_COLOR is set.
     * @return  false  subclasses should override
     */
    public boolean showColorControlWidget() {
        return false;
    }

    /**
     * Set the DoCursorReadout property.
     *
     * @param value The new value for DoCursorReadout
     */
    public void setDoCursorReadout(boolean value) {
        doCursorReadout = value;
    }

    /**
     * Get the DoCursorReadout property.
     *
     * @return The DoCursorReadout
     */
    public boolean getDoCursorReadout() {
        return doCursorReadout;
    }

    /**
     * Set the texture quality
     *
     * @param quality  1=high, &gt; 1 lower
     */
    public void setTextureQuality(int quality) {
        textureQuality = quality;
        if (getHaveInitialized()) {
            try {
                applyTextureQuality();
            } catch (Exception exc) {
                logException("Applying z position", exc);
            }
        }
    }

    /**
     * Get the texture quality
     *
     * @return the  texture quality
     */
    public int getTextureQuality() {
        return textureQuality;
    }

    /**
     * Return the label that is to be used for the texture quality widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the color widget
     */
    public String getTextureQualityLabel() {
        return "Texture Quality";
    }

    /**
     *  Use the value of the texture quality to set the value on the display
     *
     * @throws RemoteException Java RMI error
     * @throws VisADException  VisAD error
     */
    protected void applyTextureQuality()
            throws VisADException, RemoteException {}


    /**
     *  Use the value of the smoothing properties to set the value on the display.  Subclasses
     *  need to implement.
     *
     * @throws RemoteException Java RMI error
     * @throws VisADException  VisAD error
     */
    protected void applySmoothing() throws VisADException, RemoteException {}

    /**
     * Make the smoothing widget
     *
     * @return the smoothing widget
     */
    private JComponent doMakeSmoothingWidget() {
        sww = new ValueSliderWidget(
            this, 1, 19, "smoothingFactor", "Factor", 1.0f, true,
            "Amount of smoothing or radius in grid units (larger number = greater smoothing");
        final JComponent swwContents = sww.getContents(true);
        addRemovable(sww);
        GuiUtils.enableTree(swwContents, useSmoothingFactor());

        List<TwoFacedObject> smootherList =
            TwoFacedObject.createList(smoothers, smootherLabels);
        JComboBox smootherBox = new JComboBox();
        GuiUtils.setListData(smootherBox, smootherList);
        smootherBox.setSelectedItem(TwoFacedObject.findId(getSmoothingType(),
                smootherList));
        smootherBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TwoFacedObject select =
                    (TwoFacedObject) ((JComboBox) e.getSource())
                        .getSelectedItem();
                setSmoothingType((String) select.getId());
                GuiUtils.enableTree(swwContents, useSmoothingFactor());
            }
        });
        JPanel smoothWidgets = GuiUtils.left(GuiUtils.hbox(smootherBox,
                                   GuiUtils.filler(), swwContents));
        return smoothWidgets;
    }

    /**
     * Should we use the smoothing factor?
     * @return true if it's a smoothing type that uses a factor
     */
    public boolean useSmoothingFactor() {
        String type = getSmoothingType();
        return !(type.equals(LABEL_NONE)
                 || type.equals(GridUtil.SMOOTH_5POINT)
                 || type.equals(GridUtil.SMOOTH_9POINT));
    }

    /**
     * Get the smoothing factor
     *
     * @return the smoothing factor
     */
    public int getSmoothingFactor() {
        return smoothingFactor;
    }

    /**
     * Get the smoothing type
     *
     * @return the smoothing factor
     */
    public String getSmoothingType() {
        return smoothingType;
    }

    /**
     * Set the smoothing factor
     *
     * @param val the new smoothing factor
     */
    public void setSmoothingFactor(int val) {
        smoothingFactor = val;
        if (sww != null) {
            sww.setValue(val);
        }
        if (getHaveInitialized()) {
            try {
                applySmoothing();
            } catch (Exception e) {
                logException("Error applying smoothing factor", e);
            }
        }
    }

    /**
     * Set the smoothing type
     *
     * @param type  the new smoothing type
     */
    public void setSmoothingType(String type) {
        smoothingType = type;
        // reload data if done interactively
        if (getHaveInitialized()) {
            try {
                applySmoothing();
            } catch (Exception e) {
                logException("Error applying smoothing type", e);
            }
        }
    }

    /**
     *  Set the PointSize property.
     *
     *  @param value The new value for PointSize
     */
    public void setPointSize(float value) {
        pointSize = value;
    }

    /**
     *  Get the PointSize property.
     *
     *  @return The PointSize
     */
    public float getPointSize() {
        return pointSize;
    }

    /**
     * Make the point size widget
     *
     * @return  the point size widget
     */
    public JComponent doMakePointSizeWidget() {
        final JTextField pointSizeFld = new JTextField("" + getPointSize(),
                                            5);
        pointSizeFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    setPointSize(
                        new Float(
                            pointSizeFld.getText().trim()).floatValue());
                } catch (Exception exc) {
                    logException("Error parsing size:"
                                 + pointSizeFld.getText(), exc);
                }
            }
        });

        return pointSizeFld;
    }

    /**
     *  Set the VisbilityAnimationPause property.
     *
     *  @param value The new value for VisbilityAnimationPause
     */
    public void setVisbilityAnimationPause(int value) {
        this.visbilityAnimationPause = value;
    }

    /**
     *  Get the VisbilityAnimationPause property.
     *
     *  @return The VisbilityAnimationPause
     */
    public int getVisbilityAnimationPause() {
        return this.visbilityAnimationPause;
    }

    /**
     *  Set the IsTimeDriver property.
     *
     *  @param value The new value for IsTimeDriver
     */
    public void setIsTimeDriver(boolean value) {
        this.isTimeDriver = value;
        if (haveInitialized && value) {
            ViewManager vm = getViewManager();
            vm.ensureOnlyOneTimeDriver(this);
        }
    }

    /**
     *  Get the IsTimeDriver property.
     *
     *  @return The IsTimeDriver
     */
    public boolean getIsTimeDriver() {
        return this.isTimeDriver;
    }

    /**
     *  Set the UsesTimeDriver property.
     *
     *  @param value The new value for UsesTimeDriver
     */
    public void setUsesTimeDriver(boolean value) {
        this.usesTimeDriver = value;
    }

    /**
     *  Get the UsesTimeDriver property.
     *
     *  @return The UsesTimeDriver
     */
    public boolean getUsesTimeDriver() {
        return this.usesTimeDriver;
    }

    /**
     * _more_
     *
     * @param xyPoints _more_
     *
     * @return _more_
     */
    public ucar.unidata.geoloc.LatLonPoint[] getLatLonPoints(
            double[][] xyPoints) {
        ucar.unidata.geoloc.LatLonPoint[] latlonPoints =
            new ucar.unidata.geoloc.LatLonPoint[xyPoints[0].length];

        if (inGlobeDisplay()) {
            for (int i = 0; i < xyPoints.length; i++) {
                latlonPoints[i] = new LatLonPointImpl(xyPoints[0][i],
                        xyPoints[1][i]);
            }
            return latlonPoints;
        }

        NavigatedDisplay navDisplay = getMapDisplay();

        for (int i = 0; i < xyPoints.length; i++) {
            EarthLocation llpoint =
                navDisplay.getEarthLocation(xyPoints[0][i], xyPoints[1][i],
                                            0);
            latlonPoints[i] =
                new LatLonPointImpl(llpoint.getLatitude().getValue(),
                                    llpoint.getLongitude().getValue());
        }

        return latlonPoints;
    }

    /**
     * Can we do progresive resolution from this display
     *
     * @return  true if display and view supports it
     */
    public boolean getShoulDoProgressiveResolution() {
        boolean shouldDo = canDoProgressiveResolution()
                           && getIsProgressiveResolution();
        MapViewManager mvm = getMapViewManager();
        if (mvm != null) {
            shouldDo = shouldDo && mvm.getUseProgressiveResolution();
        }
        return shouldDo;
    }

    /**
     * Does this control support progressive resolution?  Subclasses should
     * override.
     * @return false
     */
    protected boolean canDoProgressiveResolution() {
        return false;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsProgressiveResolution() {
        return this.isProgressiveResolution;
    }

    /**
     * _more_
     *
     * @param isPG _more_
     */
    public void setIsProgressiveResolution(boolean isPG) {
        this.isProgressiveResolution = isPG;
        if (dataSelection != null) {
            dataSelection.putProperty(
                DataSelection.PROP_PROGRESSIVERESOLUTION,
                this.isProgressiveResolution);
        }
    }

    /**
     * Should we match the display region for spatial bounds
     * @return true if match display region
     */
    public boolean getMatchDisplayRegion() {
        return matchDisplayRegion;
    }

    /**
     * Set whether we should match the display region for spatial bounds
     * @param useDR  true if match display region
     */
    public void setMatchDisplayRegion(boolean useDR) {
        this.matchDisplayRegion = useDR;
        if (dataSelection != null) {
            dataSelection.getGeoSelection(true).setUseViewBounds(useDR);
        }
    }

}
