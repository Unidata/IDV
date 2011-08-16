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

package ucar.unidata.idv.control;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataTimeRange;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.point.PointDataInstance;
import ucar.unidata.data.point.PointDataSource;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.geoloc.Bearing;
import ucar.unidata.gis.SpatialGrid;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.control.chart.LineState;
import ucar.unidata.idv.control.chart.PointParam;
import ucar.unidata.idv.control.chart.TimeSeriesChart;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.PropertyFilter;
import ucar.unidata.ui.symbol.CloudCoverageSymbol;
import ucar.unidata.ui.symbol.MetSymbol;
import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.ui.symbol.StationModelManager;
import ucar.unidata.ui.symbol.TextSymbol;
import ucar.unidata.ui.symbol.ValueSymbol;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LayoutUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.GlobeDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.StationModelDisplayable;
import visad.CommonUnit;
import visad.Data;
import visad.DisplayEvent;
import visad.FieldImpl;
import visad.FunctionType;
import visad.Integer1DSet;
import visad.LinearLatLonSet;
import visad.MathType;
import visad.Real;
import visad.RealTupleType;
import visad.RealType;
import visad.SI;
import visad.Set;
import visad.SetType;
import visad.TextType;
import visad.Tuple;
import visad.TupleType;
import visad.Unit;
import visad.VisADException;
import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;
import visad.georef.MapProjection;





/**
 * A DisplayControl for station models
 *
 * @author MetApps Development Team
 * @version $Revision: 1.228 $
 */

public class StationModelControl extends ObsDisplayControl {




    /** The icon used to show locked legend components */
    protected static ImageIcon lockIcon;

    /** The icon used to show unlocked legend components */
    protected static ImageIcon unlockIcon;


    /** Do we use altitude if we have it */
    private boolean shouldUseAltitude = true;

    /** are we in the globe */
    private boolean inGlobe = false;

    /** holds the z position slider */
    protected JPanel zPositionPanel;

    /** time series chart */
    private TimeSeriesChart timeSeries;

    /** This holds the chart */
    private JComponent plotPanel;

    /** dummy data */
    private static final Data DUMMY_DATA = new Real(0);

    /** The table that shows the drill-down info */
    private JTable table;

    /** table scroller */
    private JScrollPane tableScroller;

    /** The last time range we used */
    private Range lastRange;

    /** The range of all the data */
    private Range timeRange;

    /** The table model that holds the drill-down info */
    private MyTableModel tableModel;

    /** Holds the observation data for the selected station */
    private List tableRows = new ArrayList();


    /** list of chart parameters */
    private List chartParams = new ArrayList();


    /** Holds the EarthLocation of the last point clicked */
    private EarthLocation lastEarthLocation = null;



    /** The ob that is currently being displayed in the table */
    private PointOb currentTableOb;

    /** list of selected observations */
    private List<PointOb> selectedObs = new ArrayList<PointOb>();

    /** the selected observation */
    private PointOb selectedOb;

    /** id of selected ob */
    private String selectedObId = null;

    /** field name of ob id */
    private String idFieldName;

    /** lat/lon point of selected ob */
    private LatLonPoint selectedObLocation;


    /** label for the selected ob */
    private JLabel selectedObLbl;

    /** id index */
    private int idIndex = -1;

    /** widget */
    private JButton changeButton;

    /** The widget to show the layout model in the gui */
    protected LayoutModelWidget layoutModelWidget;

    static {
        lockIcon = GuiUtils.getImageIcon(
            "/ucar/unidata/idv/control/images/lock.png");
        unlockIcon = GuiUtils.getImageIcon(
            "/ucar/unidata/idv/control/images/lock_open.png");
    }


    /** displayable for dat */
    StationModelDisplayable myDisplay;

    /** time holder */
    private DisplayableData timesHolder = null;

    /** station model to use */
    StationModel stationModel;

    /** Put the color tables into the legend */
    private JPanel sideLegendExtra;

    /** name of the station model */
    String modelName;


    /** grid for decluttering */
    private SpatialGrid stationGrid;

    /** flag for decluttering */
    private boolean declutter = true;

    /** Do we just show the search results */
    private boolean onlyShowFiltered = true;



    /** Do we use and logic or or logic for the filters */
    private boolean matchAll = true;

    /** Are the filters enabled */
    private boolean filtersEnabled = true;

    /** Do we not declutter and just use the last set of decluttered stations */
    protected boolean stationsLocked = false;

    /** filters for showing data */
    protected List filters = new ArrayList();

    /** The GUI used to show and edit the filters */
    private PropertyFilter.FilterGui filterGui;



    /** Flag when we only want to use the last time */
    private boolean useLastTime = false;


    /** Widget used to lock the decluttering */
    private JButton lockBtn;

    /** Widget used to show the density filter value */
    private JSlider densitySlider;


    /** List of components to disable when not delcuttering */
    protected List densityComps = new ArrayList();

    /** List of components to disable when not delcuttering */
    protected List timeDeclutterComps = new ArrayList();

    /** Keep around the last set of decluttered data */
    protected FieldImpl lastDeclutteredData;

    /** The currently displayed data */
    protected FieldImpl currentStationData;

    /** bounds of the display */
    private Rectangle2D lastViewBounds = null;

    /** last rotation */
    private double[] lastRotation;

    /** Have we gotten the initial display scale */
    private boolean haveSetInitialScale = false;

    /** scale factor for the shapes */
    private float lastViewScale = -1.0f;

    /** The scale the user can enter */
    private float displayableScale = 1.0f;

    /** decluttering filter factor */
    private float declutterFilter = 0.8f;


    /** label for which station model is being shown */
    private JLabel stationLabel;

    /** temporary name */
    private String tmpStationModelName;

    /** flag for showing all at once */
    private boolean showAllTimes;

    /** flag for waiting to load */
    private boolean waitingToLoad = false;

    /** last load event time */
    private long lastTimeLoadDataWasCalled;

    /** For loadDataInAWhile */
    private Object LOADDATA_MUTEX = new Object();

    /** loadData timestamp */
    private int loadDataTimestamp = 0;

    /** locking object */
    private Object MUTEX = new Object();


    /** Holder to dynamically put in the time declutter widgets */
    private JComponent timeDeclutterLeft;

    /** Holder to dynamically put in the time declutter widgets */
    private JComponent timeDeclutterRight;

    /** Have we asked the user to declutter time */
    private boolean askedUserToDeclutterTime = false;


    /** The range from the symbol in the layout model */
    private Range stationModelRange;

    /** The color table from the symbol in the layout model */
    private ColorTable stationModelColorTable;

    /** Time strings */
    private final static String[] TIMES_TO_USE = { "Individual", "Multiple" };

    /** flag for using data times */
    private boolean useDataTimes = true;

    /** the range color preview for the legend */
    private RangeColorPreview rangeColorPreview = null;

    /**
     * Default constructor.
     */
    public StationModelControl() {}


    /**
     * Initailize after we have been unpersisted.
     *
     * @param vc The context
     * @param properties properties
     */
    public void initAfterUnPersistence(ControlContext vc,
                                       Hashtable properties) {
        super.initAfterUnPersistence(vc, properties);
        if ( !useZPosition()) {
            shouldUseAltitude = false;
        }
        try {
            myDisplay.setShouldUseAltitude(shouldUseAltitude);
        } catch (Exception excep) {}

        //If we have a station model then try to see if we already have 
        //one with the same name. If so then use  it. Else add the 
        //new one to the SMM
        if (stationModel != null) {
            StationModelManager smm =
                getControlContext().getStationModelManager();
            StationModel currentOne =
                smm.getStationModel(stationModel.getName());
            if (currentOne != null) {
                stationModel = currentOne;
            } else {
                smm.addStationModel(stationModel);
            }
        }
    }


    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws VisADException  some problem creating a VisAD object
     * @throws RemoteException  some problem creating a remote VisAD object
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        if ((tmpStationModelName == null) && (dataChoice != null)) {
            tmpStationModelName =
                dataChoice.getProperty(PointDataSource.PROP_STATIONMODELNAME,
                                       (String) null);

            if (getDataSelection() != null) {
                String sm = (String) getDataSelection().getProperty(
                                PointDataSource.PROP_STATIONMODELNAME);
                if (sm != null) {
                    tmpStationModelName = sm;
                }
            }

            //Try the defaults
            if (tmpStationModelName == null) {
                if (getControlContext().getStationModelManager()
                        .getStationModel("Default") != null) {
                    tmpStationModelName = "Default";
                } else if (getControlContext().getStationModelManager()
                        .getStationModel("Location") != null) {
                    tmpStationModelName = "Location";
                }
            }
        }


        myDisplay = createStationModelDisplayable();
        if ( !useZPosition()) {
            shouldUseAltitude = false;
        }
        inGlobe = (getNavigatedDisplay() instanceof GlobeDisplay);
        initDisplayable(myDisplay);

        timesHolder = new LineDrawing("ob_time" + dataChoice);
        timesHolder.setManipulable(false);
        timesHolder.setVisible(false);
        addDisplayable(timesHolder);
        lastViewBounds = null;

        setScaleOnDisplayable();

        getControlContext().getStationModelManager()
            .addPropertyChangeListener(this);

        boolean ok = setData(dataChoice);

        if ( !ok) {
            return false;
        }

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (getHaveInitialized()) {
                    applyFilters();
                }
            }
        };
        filterGui = new PropertyFilter.FilterGui(filters, getFilterNames(),
                filtersEnabled, matchAll, listener);

        return ok;
    }


    /**
     */
    protected void doInitialUpdateLegendAndList() {
        //We don't do this
    }


    /**
     * Initialize the displayable with another
     *
     * @param myDisplay  the display
     *
     * @throws VisADException  some problem creating a VisAD object
     * @throws RemoteException  some problem creating a remote VisAD object
     */
    protected void initDisplayable(StationModelDisplayable myDisplay)
            throws VisADException, RemoteException {
        if (inGlobe) {
            myDisplay.setRotateShapes(true);
        }
        myDisplay.setShouldUseAltitude(shouldUseAltitude);
    }

    /**
     * Init is done
     */
    public void initDone() {
        super.initDone();
        if ( !getWasUnPersisted()) {
            if (chartParams == null) {
                chartParams = new ArrayList();
            }
        }
        loadDataInAWhile();
    }


    /**
     * Are we fully initialized
     *
     * @return is init done
     */
    public boolean isInitDone() {
        if ( !super.isInitDone()) {
            return false;
        }
        return !waitingToLoad;
    }


    /**
     * Get the image of "what".
     *
     * @param what  the thing to get
     *
     * @return the image
     *
     * @throws Exception On badness
     */
    public Image getImage(String what) throws Exception {
        if ((what != null) && what.equals("chart")) {
            setMainPanelDimensions();
            if ( !getIdv().getArgsManager().getIsOffScreen()) {
                GuiUtils.showComponentInTabs(getChart().getContents());
            }
            return ImageUtils.getImage(getChart().getContents());
        }
        return super.getImage(what);
    }



    /**
     * Signal base class to add this as a display listener
     *
     * @return Add as display listener
     */
    protected boolean shouldAddDisplayListener() {
        return true;
    }

    /**
     * Signal base class to add this as a control listener
     *
     * @return Add as control listener
     */
    protected boolean shouldAddControlListener() {
        return true;
    }



    /**
     * Make the side legend component
     *
     * @return side legend component
     */
    protected JComponent doMakeSideLegendComponent() {
        JComponent legendComp = super.doMakeSideLegendComponent();
        sideLegendExtra = new JPanel();
        sideLegendExtra.setLayout(new BoxLayout(sideLegendExtra,
                BoxLayout.Y_AXIS));
        fillSideLegend();
        return GuiUtils.centerBottom(legendComp, sideLegendExtra);
    }

    /**
     * Fill the side legend
     */
    private void fillSideLegend() {
        if (sideLegendExtra == null) {
            return;
        }
        Range      newRange      = null;
        ColorTable newColorTable = null;
        try {
            sideLegendExtra.removeAll();
            if (stationModel == null) {
                stationModelRange      = null;
                stationModelColorTable = null;
                return;
            }
            sideLegendExtra.add(GuiUtils.left(new JLabel("Layout model:"
                    + stationModel.getDisplayName())));
            List<Object[]> ctComps = new ArrayList<Object[]>();
            for (Iterator iter = stationModel.iterator(); iter.hasNext(); ) {
                MetSymbol  metSymbol = (MetSymbol) iter.next();
                ColorTable ct        = metSymbol.getColorTable();
                Range      range     = metSymbol.getColorTableRange();
                if ((ct != null) && (stationModelColorTable == null)
                        && (range != null)) {
                    newColorTable = ct;
                    newRange      = range;

                }
                String param = metSymbol.getColorTableParam();
                if ((ct == null) || (param == null)) {
                    continue;
                }

                rangeColorPreview = new RangeColorPreview(ct.getColorList(),
                        getDisplayConventions(),
                        param.equalsIgnoreCase(PointOb.PARAM_TIME));

                //                rangeColorPreview.setPreferred
                rangeColorPreview.setRange(range);
                Rectangle b = metSymbol.getBounds();
                ctComps.add(new Object[] { new Integer(b.y),
                                           GuiUtils
                                           .topCenter(GuiUtils
                                               .left(new JLabel(param
                                                   + ":")), rangeColorPreview
                                                       .doMakeContents()) });
            }

            //Sort the components on height
            ctComps = (List<Object[]>) Misc.sortTuples(ctComps, true);
            for (Object[] pair : ctComps) {
                sideLegendExtra.add((Component) pair[1]);
            }

            GuiUtils.setFontOnTree(sideLegendExtra,
                                   GuiUtils.buttonFont.deriveFont(10.0f));
            sideLegendExtra.validate();
            sideLegendExtra.repaint();

            if ( !Misc.equals(newRange, stationModelRange)
                    || !Misc.equals(newColorTable, stationModelColorTable)) {
                stationModelRange      = newRange;
                stationModelColorTable = newColorTable;
                applyColorTable();
                applyRange();
            }
        } catch (Exception exc) {
            logException("Creating color table previews", exc);
        }
    }


    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        try {
            getChart().timeChanged();
            updateTimes();
            applyTimeRange();
        } catch (Exception exc) {
            logException("Property change", exc);
        }
        super.timeChanged(time);
    }



    /**
     * Property change method.
     *
     * @param evt   event to act on
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(
                StationModelManager.PROP_RESOURCECHANGE)) {

            StationModel changedModel = (StationModel) evt.getNewValue();
            handleChangedStationModel(changedModel);
        } else if (evt.getPropertyName().equals(
                StationModelManager.PROP_RESOURCEREMOVE)) {
            StationModel changedModel = (StationModel) evt.getOldValue();
            if (stationModel.getName().equals(changedModel.getName())) {
                handleStationModelChange();
            }
        }
        super.propertyChange(evt);
    }


    /**
     * The station model changed
     *
     * @param changedModel The changed station model_
     */
    protected void handleChangedStationModel(StationModel changedModel) {
        if (stationModel.getName().equals(changedModel.getName())) {
            setStationModel(changedModel);
        }

    }


    /**
     *  The station model changed
     */
    private void handleStationModelChange() {
        StationModel changedModel = null;
        if (stationModel != null) {
            changedModel =
                getControlContext().getStationModelManager().getStationModel(
                    stationModel.getName());
        }
        if (changedModel != null) {
            if (changedModel != stationModel) {
                setStationModel(changedModel);
            }
            return;
        }
        setStationModel(getControlContext().getStationModelManager()
            .getDefaultStationModel());
    }


    /**
     * The animation has been stepped in the main display. Update the table
     */
    private void updateTimes() {
        try {

            FieldImpl theField = getCurrentTimeStep(false);
            enableTable();
            if ((theField == null) || (selectedOb == null)) {
                disableTable();
                //                updateTable(null);
                return;
            }

            Set          domainSet = theField.getDomainSet();
            int          numObs    = domainSet.getLength();
            PointOb      theOb     = null;
            StringBuffer sb        = new StringBuffer();
            for (int i = 0; i < numObs; i++) {
                Object tmp = theField.getSample(i);
                if ( !(tmp instanceof PointOb)) {
                    continue;
                }
                PointOb ob = (PointOb) tmp;
                if (isSelected(ob)) {
                    theOb = ob;
                    break;
                }
            }
            if (theOb == null) {
                disableTable();
                return;
                //                System.err.println(selectedObId+":"+ sb);
            } else {
                //                System.err.println("got one");
            }
            updateTable(theOb);
        } catch (Exception e) {
            logException("Updating times", e);
        }

    }

    /** color of the disabled table */
    private static Color tableDisableColor = new Color(230, 230, 230);


    /**
     * disable the table
     */
    private void disableTable() {
        if (table != null) {
            table.setBackground(tableDisableColor);
            table.repaint();
        }
    }

    /**
     * enable the table
     */
    private void enableTable() {
        if (table != null) {
            table.setBackground(Color.white);
        }
    }

    /**
     * What label should be used for the data projection
     *
     * @return data projection label
     */
    protected String getDataProjectionLabel() {
        return "Use Projection From Observations";
    }


    /**
     * Find the obs
     *
     * @param theField The field to look in
     * @param obs List of obs to add to
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void findSelectedObs(FieldImpl theField, List<PointOb> obs)
            throws VisADException, RemoteException {
        Set domainSet = theField.getDomainSet();
        int numObs    = domainSet.getLength();
        int cnt       = 0;
        //        System.err.println ("id:" + stationIdx);
        for (int i = 0; i < numObs; i++) {
            Object tmp = theField.getSample(i);
            if ( !(tmp instanceof PointOb)) {
                continue;
            }
            PointOb ob = (PointOb) tmp;
            if (isSelected(ob)) {
                obs.add(ob);
            }
        }
    }


    /**
     * Check to see if the PointOb is selected
     *
     * @param ob  the ob to check
     *
     * @return true if selected
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private boolean isSelected(PointOb ob)
            throws VisADException, RemoteException {
        if (selectedObId != null) {
            if (idIndex < 0) {
                getId(ob);
            }
        }

        if ((selectedObId != null) && (idIndex >= 0)) {
            Tuple      tuple     = (Tuple) ob.getData();
            TupleType  tupleType = (TupleType) tuple.getType();
            MathType[] types     = tupleType.getComponents();
            String     obId      = tuple.getComponent(idIndex).toString();
            return selectedObId.equals(obId);
        } else if (selectedObLocation != null) {
            LatLonPoint llp = ob.getEarthLocation().getLatLonPoint();
            return llp.equals(selectedObLocation);
        }
        return false;
    }


    /**
     * Get the ID of the PointOb
     *
     * @param ob  the ob
     *
     * @return the ID
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private String getId(PointOb ob) throws VisADException, RemoteException {
        idFieldName = null;
        idIndex     = -1;
        if (ob == null) {
            return null;
        }
        Tuple      tuple     = (Tuple) ob.getData();
        TupleType  tupleType = (TupleType) tuple.getType();
        MathType[] types     = tupleType.getComponents();
        String     id        = null;
        int        typeIdx   = 0;
        //First look at the text types
        for (typeIdx = 0; typeIdx < types.length; typeIdx++) {
            MathType type = types[typeIdx];
            if (type instanceof TextType) {
                String name = type.toString();
                if (isIdParam(name)) {
                    idFieldName = StringUtil.replace(name, "(Text)", "");
                    idIndex     = typeIdx;
                    String putativeId = tuple.getComponent(typeIdx).toString().trim();
                    if (!putativeId.isEmpty())
                      return tuple.getComponent(typeIdx).toString();
                }
            }
        }
        typeIdx = 0;
        //Now look at the numeric types
        for (typeIdx = 0; typeIdx < types.length; typeIdx++) {
            MathType type = types[typeIdx];
            if ( !(type instanceof TextType)) {
                String name = type.toString();
                if (isIdParam(name)) {
                    idIndex     = typeIdx;
                    idFieldName = name;
                    return tuple.getComponent(typeIdx).toString();
                }
            }
        }
        return null;
    }

    /**
     * Find the selected observations
     *
     *
     * @return the list of selected obs
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private List<PointOb> findSelectedObs()
            throws VisADException, RemoteException {
        List<PointOb>     obs = new ArrayList<PointOb>();
        PointDataInstance pdi = (PointDataInstance) getDataInstance();
        if (pdi == null) {
            return obs;
        }
        FieldImpl data = pdi.getTimeSequence();
        //pdi.getPointObs();
        if (data == null) {
            return obs;
        }
        boolean isTimeSequence = GridUtil.isTimeSequence(data);
        //        System.err.println("is time:"+ isTimeSequence);
        if (isTimeSequence) {
            Set timeSet  = data.getDomainSet();
            int numTimes = timeSet.getLength();
            //            System.err.println("num times:"+ numTimes);
            for (int i = 0; i < numTimes; i++) {
                FieldImpl oneTime = (FieldImpl) data.getSample(i);
                findSelectedObs(oneTime, obs);
            }
        } else {
            findSelectedObs(data, obs);
        }
        return obs;
    }


    /**
     * Get the MapProjection for this data; if have a single point data object
     * make synthetic map projection for location
     * @return MapProjection  for the data
     */
    public MapProjection getDataProjection() {
        try {

            PointDataInstance pdi = (PointDataInstance) getDataInstance();
            if (pdi == null) {
                return null;
            }
            FieldImpl data = pdi.getPointObs();
            if (data == null) {
                return null;
            }
            double  minX           = Double.POSITIVE_INFINITY;
            double  maxX           = Double.NEGATIVE_INFINITY;
            double  minY           = Double.POSITIVE_INFINITY;
            double  maxY           = Double.NEGATIVE_INFINITY;
            int     cnt            = 0;
            boolean isTimeSequence = GridUtil.isTimeSequence(data);
            List    boxes          = new ArrayList();
            if (isTimeSequence) {
                Set timeSet  = data.getDomainSet();
                int numTimes = timeSet.getLength();
                for (int i = 0; i < numTimes; i++) {
                    FieldImpl oneTime = (FieldImpl) data.getSample(i);
                    boxes.add(PointObFactory.getBoundingBoxOneTime(oneTime));
                }
            } else {
                boxes.add(PointObFactory.getBoundingBoxOneTime(data));
            }

            for (int i = 0; i < boxes.size(); i++) {
                double[] bbox = (double[]) boxes.get(i);
                minY = Math.min(bbox[0], minY);
                maxY = Math.max(bbox[0], maxY);
                minY = Math.min(bbox[2], minY);
                maxY = Math.max(bbox[2], maxY);

                minX = Math.min(bbox[1], minX);
                maxX = Math.max(bbox[1], maxX);
                minX = Math.min(bbox[3], minX);
                maxX = Math.max(bbox[3], maxX);
            }
            return ucar.visad.Util.makeMapProjection(minY, minX, maxY, maxX);
        } catch (Exception exc) {
            logException("Error reading xml", exc);
            return null;
        }
    }




    /**
     * Listen for DisplayEvents
     *
     * @param evt The event
     */
    public void handleDisplayChanged(DisplayEvent evt) {
        try {
            int        id         = evt.getId();
            InputEvent inputEvent = evt.getInputEvent();
            if (id == DisplayEvent.MAPS_CLEARED) {
                setScaleOnDisplayable();
            } else if ((id == DisplayEvent.MOUSE_PRESSED)
                       && !inputEvent.isShiftDown()) {
                double[] coords = screenToBox(evt.getX(), evt.getY(),
                                      getZPosition());
                lastEarthLocation = boxToEarth(coords);
                handleMousePressed(lastEarthLocation, evt);
            }
        } catch (Exception e) {
            logException("Handling display event changed", e);
        }
    }


    /**
     * Get the data for the current time step
     *
     * @param justVisible If true then only look at the obs we are displaying (e.g.,
     * the decluttered ones)
     * @return Data for current time.
     *
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    private FieldImpl getCurrentTimeStep(boolean justVisible)
            throws VisADException, RemoteException {
        PointDataInstance pdi = (PointDataInstance) getDataInstance();
        if (pdi == null) {
            return null;
        }
        FieldImpl data = (justVisible
                          ? currentStationData
                          : pdi.getTimeSequence());
        if (data == null) {
            return null;
        }
        boolean isTimeSequence = GridUtil.isTimeSequence(data);
        if (isTimeSequence) {
            return (FieldImpl) data.evaluate(
                getViewAnimation().getAniValue(), Data.NEAREST_NEIGHBOR,
                Data.NO_ERRORS);
        }
        return data;
    }

    /**
     * Set the selected observation
     *
     * @param ob   the observation
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void setSelectedOb(PointOb ob)
            throws VisADException, RemoteException {
        selectedOb = ob;
        if (ob == null) {
            selectedObs        = new ArrayList<PointOb>();
            selectedObId       = null;
            selectedObLocation = null;
            idIndex            = -1;
        } else {
            selectedObId       = getId(ob);
            selectedObLocation = ob.getEarthLocation().getLatLonPoint();
            selectedObs        = findSelectedObs();
            //            System.err.println ("ob:" + selectedObId + " " + selectedObLocation);
        }

        setXYPlot(selectedObs);
        if (selectedObLbl == null) {
            return;
        }

        if (selectedOb == null) {
            selectedObLbl.setText("  ");
            getChart().setEmptyChartLabel(
                "Select a station in the main display");
        } else {
            String label;
            if (selectedObId != null) {
                label = "Station: " + selectedObId;
                if (idFieldName != null) {
                    label = label + " (" + idFieldName + ")";
                }
            } else {
                label = "Location: " + selectedObLocation;
            }
            selectedObLbl.setText(label);
            getChart().setEmptyChartLabel(
                "Right click on observation in table to add to chart");
        }
        updateTable(ob);
    }


    /**
     * Handle the mouse pressed event in the main display
     *
     * @param el Where the mouse is clicked
     * @param evt The event
     */
    protected void handleMousePressed(EarthLocation el, DisplayEvent evt) {
        try {
            if ( !isGuiShown() || !getDisplayVisibility()) {
                return;
            }
            PointOb   ob       = null;
            FieldImpl theField = getCurrentTimeStep(true);
            if (theField != null) {
                ob = findClosestOb(el, theField);
            }
            setSelectedOb(ob);
            if (ob != null) {
                //                GuiUtils.showComponentInTabs(getChart().getContents());
            }
        } catch (Exception e) {
            logException("Handling mouse move event", e);
        }
    }


    /**
     * Find the closest ob in the field to the particular EarthLocation
     *
     * @param el  the EarthLocation
     * @param theField  the data to search
     *
     * @return the closest ob (may be null);
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    protected PointOb findClosestOb(EarthLocation el, FieldImpl theField)
            throws VisADException, RemoteException {

        if ((el == null) || (theField == null)) {
            return null;
        }
        Set         domainSet   = theField.getDomainSet();

        int         numObs      = domainSet.getLength();
        PointOb     closestOb   = null;
        LatLonPoint llp         = el.getLatLonPoint();
        Bearing     bearing     = null;

        Component   comp = myDisplay.getDisplayMaster().getDisplayComponent();
        Rectangle   bounds      = comp.getBounds();
        int[]       clickPt     = boxToScreen(earthToBox(el));
        double      minDistance = 20;
        //        System.err.println ("click:" + clickPt[0]+"/"+clickPt[1] + " " +minDistance);

        for (int i = 0; i < numObs; i++) {
            Object tmp = theField.getSample(i);
            if ( !(tmp instanceof PointOb)) {
                continue;
            }
            PointOb       ob       = (PointOb) tmp;
            EarthLocation obEl     = ob.getEarthLocation();
            int[]         obScreen = boxToScreen(earthToBox(obEl));
            double        distance = GuiUtils.distance(obScreen, clickPt);
            //            System.err.println ("\t" + obScreen[0]+"/"+obScreen[1] + " d:" + distance);
            if (distance < minDistance) {
                closestOb   = ob;
                minDistance = distance;
            }
        }
        return closestOb;
    }


    /**
     * Extract the observation values from the given ob and update the
     * table.
     *
     *
     * @param ob The ob
     */
    protected void updateTable(PointOb ob) {
        updateTable(ob, false);
    }

    /**
     * Extract the observation values from the given ob and update the
     * table.
     *
     *
     * @param ob The ob
     * @param force  force the update
     */
    private void updateTable(PointOb ob, boolean force) {
        if ( !force && Misc.equals(ob, currentTableOb)) {
            return;
        }
        enableTable();


        currentTableOb = ob;
        tableRows      = new ArrayList();
        if (ob == null) {
            if (tableModel != null) {
                tableModel.fireTableStructureChanged();
            }
            return;
        }

        try {
            Tuple      tuple     = (Tuple) ob.getData();
            TupleType  tupleType = (TupleType) tuple.getType();
            MathType[] types     = tupleType.getComponents();
            int[]      indices   = getIndicesToShow(tupleType);
            List       cols      = getFieldsToShow(tupleType);
            for (int colIdx = 0; colIdx < indices.length; colIdx++) {
                String name    = null;
                String value   = null;
                int    typeIdx = indices[colIdx];
                if (typeIdx != PointOb.BAD_INDEX) {
                    name = Util.cleanTypeName(types[typeIdx]);
                    Data dataElement = tuple.getComponent(typeIdx);
                    value = getColValue(dataElement, types[typeIdx],
                                        true).toString();
                    if (value.equals("NaN")) {
                        value = "--";
                    }
                } else {
                    name = cols.get(colIdx).toString();
                    EarthLocation el = ob.getEarthLocation();
                    if (cols.get(colIdx).equals(PointOb.PARAM_LAT)) {
                        //value = Util.formatReal(el.getLatitude());
                        value = getColValue(el.getLatitude(),
                                            RealType.Latitude,
                                            true).toString();
                    } else if (cols.get(colIdx).equals(PointOb.PARAM_LON)) {
                        //value = Util.formatReal(el.getLongitude());
                        value = getColValue(el.getLongitude(),
                                            RealType.Longitude,
                                            true).toString();
                    } else if (cols.get(colIdx).equals(PointOb.PARAM_ALT)) {
                        //value = Util.formatReal(el.getAltitude());
                        value = getColValue(el.getAltitude(),
                                            RealType.Altitude,
                                            true).toString();
                    } else if (cols.get(colIdx).equals(PointOb.PARAM_TIME)) {
                        if (getShowDataRaw()) {
                            value = Util.formatReal(ob.getDateTime());
                        } else {
                            value = ob.getDateTime().toString();
                        }
                    }
                }

                if (value == null) {
                    continue;
                }

                List rowVector = new ArrayList();
                rowVector.add(getParamLabel(name));
                rowVector.add(value);
                if (isIdParam(name)) {
                    tableRows.add(0, rowVector);
                } else {
                    tableRows.add(rowVector);
                }
            }
            if (tableModel != null) {
                table.repaint();
            }
        } catch (Exception e) {
            logException("Updating table", e);
        }
    }


    /**
     * Get the point parameter from the name
     *
     * @param paramName  the parameter name
     *
     * @return the PointParam
     */
    private PointParam getPointParam(String paramName) {
        for (int i = 0; i < chartParams.size(); i++) {
            PointParam pointParam = (PointParam) chartParams.get(i);
            if (pointParam.getName().equals(paramName)) {
                return pointParam;
            }
        }
        return null;
    }

    /**
     * Add a chart parametere
     *
     * @param paramName  the name of the parameter
     */
    public void addChartParam(String paramName) {
        PointParam pointParam = getPointParam(paramName);
        if (pointParam == null) {
            chartParams.add(new PointParam(paramName));
        } else {
            pointParam.getLineState().setVisible(true);
        }
        chartChanged();
    }

    /**
     * Handle a chart changed event
     */
    public void chartChanged() {
        try {
            setXYPlot(findSelectedObs());
            table.repaint();
        } catch (Exception exc) {
            logException("Removing chart parameter", exc);
        }

    }

    /**
     * Remove a parameters from the chart
     *
     * @param pointParam  the description of the parameter to remove
     */
    public void removeChartParam(PointParam pointParam) {
        pointParam.getLineState().setVisible(false);
        chartChanged();
    }

    /**
     * Bring the chart parameter to the front
     *
     * @param pointParam   the parameter to frontten.
     */
    public void toFront(PointParam pointParam) {
        chartParams.remove(pointParam);
        chartParams.add(0, pointParam);
        chartChanged();
    }

    /**
     * Set the XYPlot used for the obs
     *
     * @param obs  list of obs to use
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void setXYPlot(List<PointOb> obs)
            throws VisADException, RemoteException {
        getChart().setPointObs(obs, chartParams);
    }


    /**
     * Have we been unpersisted into a view manager that has not
     * been displayed yet?
     */
    public void firstFrameDone() {
        super.firstFrameDone();
        loadDataInAWhile();
    }


    /**
     * Create the {@link StationModelDisplayable} used by this
     * <code>DisplayControl</code>.  This implementation also adds it
     * to the display.  Called during init.
     *
     * @return <code>StationModelDisplayable</code> used by this instance.
     *
     * @throws VisADException  some problem creating a VisAD object
     * @throws RemoteException  some problem creating a remote VisAD object
     */
    protected StationModelDisplayable createStationModelDisplayable()
            throws VisADException, RemoteException {
        myDisplay = new StationModelDisplayable(getStationModel(),
                getControlContext().getJythonManager());
        addDisplayable(myDisplay,
                       FLAG_ZPOSITION | FLAG_LINEWIDTH | FLAG_COLORTABLE);
        return myDisplay;
    }



    /**
     * Overwrite base class method to return the color range to use when applying to displayables.
     * This is  the range from the station model for any symbol that is colored by
     *
     * @return Range
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Range getRangeToApply() throws RemoteException, VisADException {
        return stationModelRange;
    }

    /**
     * Overwrite base class method so we don't have any color table.
     * This gets called because we turn on the FLAG_COLORTABLE to enable the
     * color scales but we use the color table from the station model
     *
     * @return color table
     */
    protected ColorTable getInitialColorTable() {
        return null;
    }




    /**
     * Get the color table to use when applying to displayables
     *
     * @return color table from the layout model. May be null.
     */
    protected ColorTable getColorTableToApply() {
        return stationModelColorTable;
    }


    /**
     * Set the data for this instance from the choice supplied.
     * @param choice  <code>DataChoice</code> that describes the data to
     *                be loaded.
     * @return true if load was successful.
     *
     * @throws VisADException  some problem creating a VisAD object
     * @throws RemoteException  some problem creating a remote VisAD object
     *
     * @see DisplayControlImpl#setData(DataChoice)
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {
        if ( !super.setData(choice)) {
            return false;
        }
        if (getHaveInitialized()) {
            loadData();
        }
        return true;
    }


    /**
     * Handle some sort of time change.  Either the subsetting interval
     * changes or there is a new timestep.
     */
    public void applyTimeRange() {
        try {
            DataTimeRange dataTimeRange = getDataTimeRange(true);
            if (timeRange == null) {
                return;
            }
            /* TODO: figure out the time stuff
            double sinceEpoch = timeRange.getMax();
            int    years      = (int) (sinceEpoch / (365 * 24 * 3600));
            int    year       = 1970 + (years);
            Unit dataTimeUnit = Util.parseUnit("seconds since " + year
                                    + "-1-1 0:00:00 0:00");
            RealType dataTimeRealType = Util.getRealType(dataTimeUnit);
            Real startReal = new Real(dataTimeRealType, timeRange.getMin(),
                                      dataTimeUnit);
            Real endReal = new Real(dataTimeRealType, timeRange.getMax(),
                                    dataTimeUnit);
            */

            Real      startReal = new Real(RealType.Time, timeRange.getMin());
            Real      endReal   = new Real(RealType.Time, timeRange.getMax());

            Animation anime     = getViewAnimation();
            Real      aniValue  = ((anime != null)
                                   ? anime.getAniValue()
                                   : null);

            Real[] startEnd = getDataTimeRange().getTimeRange(startReal,
                                  endReal, aniValue);
            //System.out.println("start = " + startEnd[0]);
            //System.out.println("end = " + startEnd[1]);


            double startDate =
                startEnd[0].getValue(CommonUnit.secondsSinceTheEpoch);
            double endDate =
                startEnd[1].getValue(CommonUnit.secondsSinceTheEpoch);
            if ( !Misc.equals(lastRange, new Range(startDate, endDate))) {
                lastRange = new Range(startDate, endDate);
                if (myDisplay != null) {
                    myDisplay.setSelectedRange(startDate, endDate);
                }
            }
            if ((rangeColorPreview != null)
                    && rangeColorPreview.getIsTime()) {
                rangeColorPreview.setRange(new Range(startDate, endDate));
            }
        } catch (Exception e) {
            logException("applyTimeRange", e);
        }
    }

    /**
     * Get the range for time selection.
     *
     * @return the Range
     *
     * @throws RemoteException remote data error
     * @throws VisADException  VisAD error
     */
    private Range getRangeForTimeSelect()
            throws VisADException, RemoteException {
        Range range = null;
        if ((timesHolder != null)
                && !timesHolder.getData().equals(DUMMY_DATA)) {
            Set        timeSet = (Set) timesHolder.getData();
            double[][] doubles = timeSet.getDoubles(false);
            double     start   = doubles[0][0];
            double     end     = doubles[0][timeSet.getLength() - 1];
            range = new Range(start, end);
        }
        return range;
    }

    /**
     * A utility method that sets the wait cursor and calls loadData in a separate thread .
     */
    protected void loadDataInThread() {
        Misc.run(new Runnable() {
            public void run() {
                showWaitCursor();
                try {
                    loadData();
                } catch (Exception exc) {
                    logException("Loading data", exc);
                }
                showNormalCursor();

            }
        });
    }



    /**
     * Synchronize around the setStations
     */
    private final Object DISPLAYABLE_MUTEX = new Object();


    /**
     * Load data into the <code>Displayable</code>.  This is called from
     * {@link #setData(DataChoice)} and whenever the projection changes.
     * Subclasses should override this to do whatever they need to.
     * This implementation uses a
     * {@link ucar.unidata.data.point.PointDataInstance PointDataInstance}
     * to manage the data.
     *
     * @see #doMakeDataInstance(DataChoice)
     */
    protected void loadData() {

        int myTimestamp = ++loadDataTimestamp;

        try {
            if ( !getActive()) {
                return;
            }
            //            Trace.startTrace();

            if ( !haveSetInitialScale) {
                setScaleOnDisplayable();
            }
            haveSetInitialScale = true;

            lastViewScale       = getScaleFromDisplayable();
            lastViewBounds      = calculateRectangle();
            LinearLatLonSet llBounds = calculateLatLonBounds(lastViewBounds);

            LogUtil.message("Observation display: loading data");
            Trace.call1("getDataInstance");
            PointDataInstance pdi = (PointDataInstance) getDataInstance();
            if ((pdi == null) || (myDisplay == null)) {
                return;
            }
            Trace.call2("getDataInstance");
            Trace.call1("StationModelControl.loadData");
            FieldImpl data = null;

            if (isInTransectView() || (llBounds == null)) {
                Trace.call1("getObs-1");
                data = pdi.getTimeSequence();
                Trace.call2("getObs-1");
            } else {
                Trace.call1("getObs-2");

                data = pdi.getTimeSequence(llBounds);
                Trace.call2("getObs-2");
            }
            if (data == null) {
                //Not sure if we want to do a doRemove here.
                //                doRemove();
                return;
            }


            //In case the user did a remove displayable while we were waiting for the data
            if ( !getActive()) {
                return;
            }

            //            Trace.call2("getting data");
            FieldImpl theData = data;
            if (filtersEnabled && (filters.size() > 0)) {
                lastDeclutteredData = null;
                try {
                    LogUtil.message("Observation display: filtering data");
                    Trace.call1("filterData");
                    theData = filterData(theData);
                    Trace.call2("filterData");
                } catch (Exception exc) {
                    logException("Processing filters", exc);
                }
            }


            if ( !getTimeDeclutterEnabled()) {
                boolean isTimeSequence = GridUtil.isTimeSequence(theData);
                if (isTimeSequence) {
                    Set timeSet = theData.getDomainSet();
                    if ( !getAskedUserToDeclutterTime()
                            && (timeSet.getLength() > 1000)) {
                        setAskedUserToDeclutterTime(true);
                        if ( !GuiUtils.askYesNo("Time Declutter", new JLabel("<html>There are "
                                + timeSet.getLength()
                                + " time steps in the data.<br>Do you want to show them all?"))) {
                            setTimeDeclutterEnabled(true);
                        }
                    }
                }
            }


            if (getTimeDeclutterEnabled()) {
                lastDeclutteredData = null;
                boolean isTimeSequence = GridUtil.isTimeSequence(theData);
                LogUtil.message("Observation display: subsetting times");
                Trace.call1("doDeclutterTime");
                theData = doDeclutterTime(theData);
                Trace.call2("doDeclutterTime");
            }

            // remove the data times if we are in accumulate mode
            if ( !getUseDataTimes()) {
                setDataTimes(theData);
                timeRange = getRangeForTimeSelect();
                theData   = PointObFactory.removeTimeDimension(theData);
            }

            if (declutter) {
                if ( !stationsLocked || (lastDeclutteredData == null)) {
                    Trace.call1("doDeclutter");
                    FieldImpl tmp = doDeclutter(theData, myTimestamp);
                    if (tmp == null) {
                        return;
                    }
                    lastDeclutteredData = tmp;
                    Trace.call2("doDeclutter");
                }
                theData = lastDeclutteredData;
            }
            synchronized (DISPLAYABLE_MUTEX) {
                //                Trace.call1("setStationData");
                boolean okToSet = true;
                if ((currentStationData != null) && (theData != null)) {
                    //              okToSet = hasDataChanged((FieldImpl)currentStationData, (FieldImpl)theData);
                }
                currentStationData = theData;
                if (okToSet) {
                    LogUtil.message("Observation display: creating display");
                    Trace.call1("setStationData");
                    myDisplay.setStationData(theData);
                    Trace.call2("setStationData");
                    LogUtil.message("");
                }
                //                Trace.call2("setStationData");
            }
            Trace.call2("StationModelControl.loadData");

            //Force adding a listener to the animation
            getViewAnimation();

            //Anything selected from before
            if ((selectedObId != null) || (selectedObLocation != null)) {
                List<PointOb> obs = findSelectedObs();
                if (obs.size() > 0) {
                    setSelectedOb((PointOb) obs.get(0));
                }
            }
            applyTimeRange();

        } catch (Exception excp) {
            logException("loading data ", excp);
        }
        updateLegendAndList();

        //      Trace.stopTrace();
        //If we went to showAllTimes=true then we need to update the animation set


    }


    /**
     * Make the time option widget
     *
     * @return  the time option widget
     */
    private Component doMakeTimeOptionWidget() {
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JRadioButton b = (JRadioButton) ae.getSource();
                setUseDataTimes(b.getText().equals(TIMES_TO_USE[0]));
            }
            ;
        };
        ButtonGroup bg = new ButtonGroup();
        JRadioButton single = new JRadioButton(TIMES_TO_USE[0],
                                  getUseDataTimes());
        bg.add(single);
        single.addActionListener(listener);
        JRadioButton range = new JRadioButton(TIMES_TO_USE[1],
                                 !getUseDataTimes());
        bg.add(range);
        range.addActionListener(listener);
        return GuiUtils.hbox(single, range);
    }

    /**
     * Make the time option widget
     *
     * @return  the time option widget
     */
    private Component doMakeTimeDeclutterWidget() {
        JComponent[] timeDeclutterComps = getTimeDeclutterComps();
        JPanel timeDeclutter =
            GuiUtils.left(GuiUtils.hflow(Misc.newList(new Component[] {
                addTimeDeclutterComp(new JLabel("Use every:  ")),
                addTimeDeclutterComp(timeDeclutterComps[1]),
                addTimeDeclutterComp(new JLabel(" minutes ")),
                timeDeclutterComps[0], new JLabel("enabled") }), 2, 1));
        return timeDeclutter;
    }


    /**
     * Has this data changed
     *
     * @param newData new data
     * @param oldData old data
     *
     * @return Has changed
     *
     * @throws RemoteException bad'un
     * @throws VisADException bad'un
     */
    private boolean hasDataChanged(FieldImpl newData, FieldImpl oldData)
            throws RemoteException, VisADException {
        boolean isTimeSequence = GridUtil.isTimeSequence(newData);
        if (isTimeSequence != GridUtil.isTimeSequence(oldData)) {
            return true;
        }
        if (isTimeSequence) {
            Set timeSet  = newData.getDomainSet();
            int numTimes = timeSet.getLength();

            if (numTimes != oldData.getDomainSet().getLength()) {
                return true;
            }
            for (int i = 0; i < timeSet.getLength(); i++) {
                FieldImpl timeFieldNew = (FieldImpl) newData.getSample(i);
                FieldImpl timeFieldOld = (FieldImpl) oldData.getSample(i);
                if (hasTimeFieldChanged(timeFieldNew, timeFieldOld)) {
                    return true;
                }
            }
        } else {
            if (hasTimeFieldChanged((FieldImpl) newData,
                                    (FieldImpl) oldData)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Has data changed
     *
     * @param newData new
     * @param oldData old
     *
     * @return Is different
     *
     * @throws RemoteException bad'un
     * @throws VisADException bad'un
     */
    private boolean hasTimeFieldChanged(FieldImpl newData, FieldImpl oldData)
            throws RemoteException, VisADException {

        Set newDomainSet = newData.getDomainSet();
        int numObs       = newDomainSet.getLength();
        if (numObs != oldData.getDomainSet().getLength()) {
            return true;
        }

        for (int i = 0; i < numObs; i++) {
            if (newData.getSample(i) != oldData.getSample(i)) {
                //              System.err.println ("!=:" + i + " #:" + numObs);
                //System.err.println ("ob1:" + newData.getSample(i));
                //System.err.println ("ob2:" + oldData.getSample(i));
                return true;
            }
        }
        return false;
    }


    /**
     * Provide to the base class the type of the obs
     *
     * @return The tuple type
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected TupleType getTupleType()
            throws RemoteException, VisADException {
        PointDataInstance pdi = (PointDataInstance) getDataInstance();
        if (pdi == null) {
            return null;
        }
        FieldImpl obs = pdi.getPointObs();
        /*
          boolean   isTimeSequence = GridUtil.isTimeSequence(obs);
          if (isTimeSequence) {
          obs = (FieldImpl) obs.getSample(0);
          }
          Set domainSet = obs.getDomainSet();
          if (domainSet.getLength() == 0) {
          return null;
          }
        */
        PointOb ob = (PointOb) obs.getSample(0);
        return (TupleType) ((Tuple) ob.getData()).getType();
    }




    /**
     *  A utility to get the scale from the dislayable
     *
     * @return The scale
     */
    protected float getScaleFromDisplayable() {
        if (myDisplay != null) {
            return myDisplay.getScale();
        }
        return 0.0f;
    }

    /**
     *  A utility to set the scale on the dislayable
     *
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void setScaleOnDisplayable()
            throws RemoteException, VisADException {
        setScaleOnDisplayable(getDisplayScale() * displayableScale);
    }


    /**
     *  A utility to set the scale on the dislayable
     *
     * @param f The new scale value
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected void setScaleOnDisplayable(float f)
            throws RemoteException, VisADException {
        if (myDisplay != null) {
            myDisplay.setScale(f);
        }
    }


    /**
     * This gets called when the control has received notification of a
     * dataChange event.
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    protected void resetData() throws VisADException, RemoteException {
        clearDataInstance();
        super.resetData();
    }

    /**
     * Method to call if projection changes. This will reset the
     * viewScape, bounds and llBounds held by this object and then
     * load data
     */
    public void projectionChanged() {
        super.projectionChanged();
        //Handle this in a thread
        Misc.run(new Runnable() {
            public void run() {
                try {
                    setScaleOnDisplayable();
                    loadData();
                } catch (Exception exc) {
                    logException("handling projection change", exc);
                }
            }
        });

    }


    /**
     * Method called when a transect  changes.
     */
    public void transectChanged() {
        super.transectChanged();
        loadDataInThread();
    }


    /**
     * Handle when the value in the column field has changed.
     */
    protected void fieldSelectorChanged() {
        updateTable(currentTableOb, true);
    }


    /**
     * Add a few things to the View Menu specific to this control
     *
     * @param items the list of items for the menu
     * @param forMenuBar  true if for the menubar
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        super.getViewMenuItems(items, forMenuBar);
        if (timeSeries != null) {
            List chartItems = new ArrayList();
            chartItems.add(
                GuiUtils.makeCheckboxMenuItem(
                    "Show Chart Thumbnail", getChart(), "showThumb", null));
            getChart().addViewMenuItems(chartItems);
            chartItems.add(GuiUtils.MENU_SEPARATOR);
            chartItems.addAll(getChartMenuItems());
            JMenu chartMenu = GuiUtils.makeMenu("Chart", chartItems);
            items.add(GuiUtils.MENU_SEPARATOR);
            items.add(chartMenu);
            items.add(GuiUtils.MENU_SEPARATOR);
            items.add(GuiUtils.makeMenuItem("Show Field Selector", this,
                                            "showFieldSelector"));
            items.add(GuiUtils.makeCheckboxMenuItem("Show Raw Data", this,
                    "showDataRaw", null));
        }
    }



    /**
     * Apply the filters to the given data
     *
     * @param obs The data
     *
     * @return The filtered data.
     *
     * @throws Exception When bad things happen.
     */
    protected FieldImpl filterData(FieldImpl obs) throws Exception {
        boolean   isTimeSequence = GridUtil.isTimeSequence(obs);
        FieldImpl filteredField  = null;
        if (isTimeSequence) {
            Set timeSet = obs.getDomainSet();
            filteredField = new FieldImpl((FunctionType) obs.getType(),
                                          timeSet);
            int numTimes = timeSet.getLength();
            for (int i = 0; i < numTimes; i++) {
                FieldImpl oneTime = (FieldImpl) obs.getSample(i);
                FieldImpl subTime = doTheActualFiltering(oneTime);
                if (subTime != null) {
                    filteredField.setSample(i, subTime, false);
                }
            }
        } else {
            filteredField = doTheActualFiltering(obs);
        }
        return filteredField;
    }


    /**
     * Apply the filters to the data
     *
     * @param pointObs The data
     *
     * @return The filtered data
     *
     * @throws Exception When bad things happen
     */
    private FieldImpl doTheActualFiltering(FieldImpl pointObs)
            throws Exception {

        if ((pointObs == null) || pointObs.isMissing()) {
            return pointObs;
        }
        FieldImpl retField  = null;
        Set       domainSet = pointObs.getDomainSet();
        int       numObs    = domainSet.getLength();
        Vector    v         = new Vector();
        Object[]  tmpValues = new Object[filters.size()];

        for (int i = 0; i < numObs; i++) {
            Object tmp = pointObs.getSample(i);
            if ( !(tmp instanceof PointOb)) {
                continue;
            }
            PointOb    ob        = (PointOb) tmp;
            Tuple      tuple     = (Tuple) ob.getData();
            TupleType  tupleType = (TupleType) tuple.getType();
            MathType[] types     = tupleType.getComponents();
            String[]   typeNames = new String[types.length];
            for (int typeIdx = 0; typeIdx < types.length; typeIdx++) {
                typeNames[typeIdx] = types[typeIdx].toString();
            }
            boolean ok          = true;
            boolean matchedSome = false;
            for (int filterIdx = 0; ok && (filterIdx < filters.size());
                    filterIdx++) {
                PropertyFilter filter =
                    (PropertyFilter) filters.get(filterIdx);
                String paramName   = filter.getName();

                Data   dataElement = null;

                if (paramName.equals(PointOb.PARAM_LAT)) {
                    dataElement = ob.getEarthLocation().getLatitude();
                } else if (paramName.equals(PointOb.PARAM_LON)) {
                    dataElement = ob.getEarthLocation().getLongitude();
                } else if (paramName.equals(PointOb.PARAM_ALT)) {
                    dataElement = ob.getEarthLocation().getAltitude();
                } else {
                    int dataIndex = -1;
                    for (int typeIdx = 0;
                            (dataIndex == -1) && (typeIdx < typeNames.length);
                            typeIdx++) {
                        if (paramName.equals(typeNames[typeIdx])) {
                            dataIndex = typeIdx;
                        }
                    }

                    if (dataIndex < 0) {
                        continue;
                    }
                    dataElement = tuple.getComponent(dataIndex);
                }

                if (dataElement == null) {
                    continue;
                }
                if (dataElement.isMissing()) {
                    if (matchAll) {
                        ok = false;
                    }
                    continue;
                }
                boolean filterOk = false;
                if ( !(dataElement instanceof Real)
                        || !filter.isNumericOperator()) {
                    filterOk = filter.ok(dataElement.toString().trim());
                } else {
                    Real obsReal = (Real) dataElement;
                    if (tmpValues[filterIdx] == null) {
                        String filterValue = filter.getValue().trim();
                        tmpValues[filterIdx] = filterValue;
                        Real filterReal = ucar.visad.Util.toReal(filterValue);
                        if (filterReal != null) {
                            //                            System.err.println("filterReal:" + filterReal.getUnit() + " " +
                            //                                         obsReal.getUnit());
                            if (obsReal.getUnit() == null) {
                                tmpValues[filterIdx] =
                                    new Double(filterReal.getValue());
                            } else {
                                tmpValues[filterIdx] = new Double(
                                    filterReal.getValue(obsReal.getUnit()));
                            }
                            //System.err.println("value:" + tmpValues[filterIdx]);
                        }
                    }
                    filterOk = filter.ok(dataElement, tmpValues[filterIdx]);
                }
                if (filterOk) {
                    matchedSome = true;
                    if ( !matchAll) {
                        break;
                    }
                } else {
                    if (matchAll) {
                        ok = false;
                    }
                }
            }

            if (ok && matchedSome) {
                v.add(ob);
            }
        }

        //System.out.println("found " + v.size() + " decluttered obs in region");
        if (v.isEmpty()) {
            retField = new FieldImpl(
                (FunctionType) pointObs.getType(),
                new Integer1DSet(
                    ((SetType) domainSet.getType()).getDomain(), 1));
        } else if (v.size() == numObs) {
            retField = pointObs;  // all were in domain, just return input
        } else {
            retField = new FieldImpl(
                (FunctionType) pointObs.getType(),
                new Integer1DSet(
                    ((SetType) domainSet.getType()).getDomain(), v.size()));
            retField.setSamples((PointOb[]) v.toArray(new PointOb[v.size()]),
                                false);
        }
        return retField;
    }




    /**
     *  Set the StationModel property.
     *
     *  @param value The new value for StationModel
     */
    public void setTheStationModel(StationModel value) {
        stationModel = value;
    }

    /**
     *  Get the StationModel property.
     *
     *  @return The StationModel
     */
    public StationModel getTheStationModel() {
        return stationModel;
    }



    /**
     * Get the current station model view.
     *
     * @return station model layout
     */
    public StationModel getStationModel() {
        if (stationModel == null) {
            stationModel = createStationModel();
        }
        return stationModel;
    }



    /**
     *  This is private so the XmlEncoding won't try to save the station model
     *  as a property
     *
     * @param sm   station model to use
     */
    private void setStationModel(StationModel sm) {
        try {
            stationModel = sm;
            if (layoutModelWidget != null) {
                layoutModelWidget.setLayoutModel(sm);
            }
            myDisplay.setStationModel(sm, false);
            loadData();
            fillSideLegend();
            //Misc.runInABit (1,new Runnable () {public void run() {loadData();}});
        } catch (Exception exc) {
            logException("setting station model", exc);
        }
    }

    /**
     * Set the station model
     *
     * @param sm station model
     */
    public void setStationModelFromWidget(StationModel sm) {
        setStationModel(sm);
    }


    /**
     * Set layout model
     *
     * @param id id
     * @param stationModel station model
     */
    protected void setLayoutModel(
            String id, ucar.unidata.ui.symbol.StationModel stationModel) {
        setStationModel(stationModel);
    }


    /**
     * Assume that any display controls that have a color table widget
     * will want the color table to show up in the legend.
     *
     * @param  legendType  type of legend
     * @return The extra JComponent to use in legend
     */
    protected JComponent getExtraLegendComponent(int legendType) {
        JComponent parentComp = super.getExtraLegendComponent(legendType);
        if (legendType == BOTTOM_LEGEND) {
            return parentComp;
        }
        return GuiUtils.vbox(parentComp, getChart().getThumb());
    }



    /**
     * Gets the name of the <code>StationModel</code>.  Used by XML encoding.
     * @return name of the <code>StationModel</code>
     */
    public String getStationModelName() {
        return getStationModel().getName();
    }

    /**
     * Sets the name of the <code>StationModel</code>.
     * @param n  name of the <code>StationModel</code>
     */
    public void setStationModelName(String n) {
        tmpStationModelName = n;
    }

    /**
     * Creates the <code>StationModel</code> based on the name set by
     * {@link #setStationModelName(String)} or if that is null,
     * gets the default station from the <code>StationModelManager</code>.
     * If there is no default, it makes one up.
     *
     * @return <code>StationModel</code> to use with this instance.
     */
    private StationModel createStationModel() {
        StationModel stationModel = null;
        if (tmpStationModelName != null) {
            stationModel =
                getControlContext().getStationModelManager().getStationModel(
                    tmpStationModelName);
            if (stationModel == null) {
                LogUtil.userErrorMessage("Unable to find layout model: "
                                         + tmpStationModelName
                                         + ". Using default");
            }
            tmpStationModelName = null;
        }

        if (stationModel == null) {
            stationModel =
                getControlContext().getStationModelManager()
                    .getDefaultStationModel();
        }
        if (stationModel == null) {
            stationModel = makeDefaultStationModel();
        }
        return stationModel;
    }


    /**
     * Makes a default station model for this to use.
     *
     * @return default <code>StationModel</code>
     */
    private StationModel makeDefaultStationModel() {
        List        l    = new ArrayList();

        ValueSymbol tSym = new ValueSymbol(-20, -10, "T", "Temperature");
        tSym.setForeground(Color.red);
        Unit u = SI.kelvin;
        try {
            u = visad.data.units.Parser.parse("degF");
        } catch (Exception excp) {}
        tSym.setTheDisplayUnit(u);
        l.add(tSym);

        ValueSymbol tdSym = new ValueSymbol(-20, 10, "TD", "DewPoint", u);
        tdSym.setForeground(Color.green);
        l.add(tdSym);

        ValueSymbol pslSym = new ValueSymbol(20, -20 / 2, "PSL",
                                             "Sea Level Pressure");
        pslSym.setForeground(Color.magenta);
        pslSym.setNumberFormatString("####.0");
        l.add(pslSym);

        TextSymbol idSym = new TextSymbol(0, 20, "ID", "Station ID");
        idSym.setForeground(Color.gray);
        l.add(idSym);

        CloudCoverageSymbol cc = new CloudCoverageSymbol(0, 0, "CC1", "CC1");
        cc.setForeground(Color.white);
        l.add(cc);


        return new StationModel("Layout model", l);
    }



    /**
     * Create and return the lcok button
     *
     * @return The lock button
     */
    public JButton getLockButton() {
        if (lockBtn == null) {
            lockBtn = new JButton(lockIcon);
            lockBtn.setContentAreaFilled(false);
            lockBtn.setBorder(BorderFactory.createEmptyBorder());
            lockBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    setStationsLocked( !stationsLocked);
                }
            });
            updateLockButton();
        }
        return lockBtn;
    }






    /**
     * Get any control widgets special to this control and add them to
     * the list.
     *
     * @param controlWidgets  default list based on any attributes set
     *                        for this DisplayControl.
     * @throws VisADException  some problem creating a VisAD object
     * @throws RemoteException  some problem creating a remote VisAD object
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {

        //We don't need to call this
        //        super.getControlWidgets(controlWidgets);


        JCheckBox toggle = new JCheckBox("", declutter);
        toggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setDeclutter(((JCheckBox) e.getSource()).isSelected());
                loadDataInThread();
            }
        });


        JComponent testButton = GuiUtils.makeButton("Test", this, "test");
        //        controlWidgets.add(new WrapperWidget(this, GuiUtils.rLabel(""),
        //                                             testButton));

        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Declutter:"),
                GuiUtils.hbox(Misc.newList(new Object[] { toggle,
                getLockButton(), GuiUtils.filler(),
                addDensityComp(GuiUtils.rLabel(" Density: ")),
                getDensityControl() }))));




        GuiUtils.enableComponents(densityComps, declutter);


        final JTextField scaleField =
            new JTextField(Misc.format(displayableScale), 5);
        ActionListener scaleListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    //System.err.println("display scale = "
                    //                   + getDisplayScale());
                    setDisplayableScale(
                        (float) Misc.parseNumber(scaleField.getText()));
                } catch (Exception nfe) {
                    userErrorMessage("Bad scale format");
                }
            }
        };
        scaleField.addActionListener(scaleListener);
        JButton scaleBtn = new JButton("Apply");
        scaleBtn.addActionListener(scaleListener);


        JPanel stationModelPanel =
            GuiUtils.hbox(
                GuiUtils.left(
                    layoutModelWidget =
                        new LayoutModelWidget(
                            this, this, "setStationModelFromWidget",
                            getStationModel())), GuiUtils.rLabel(
                                "   Scale:"), GuiUtils.hflow(
                                Misc.newList(scaleField, scaleBtn), 4, 0));

        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Layout Model:"),
                GuiUtils.left(stationModelPanel)));


        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel(getLineWidthWidgetLabel() + ": "),
                getLineWidthWidget().getContents(false)));

        if (useZPosition()) {
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.top(GuiUtils.rLabel("Vertical Position: ")),
                    doMakeVerticalPositionPanel()));
        }

    }

    /**
     * test method
     */
    public void test() {
        loadDataInThread();
    }


    /**
     * Popup the station model editor
     */
    public void editStationTable() {
        getControlContext().getStationModelManager().show(stationModel);
    }

    /**
     * Get the list of obs to show in the table
     *
     * @return List of obs
     */
    private List getTableRows() {
        if (tableRows == null) {
            tableRows = new ArrayList();
        }
        return tableRows;
    }


    /**
     * Make the gui panel for vertical position
     *
     * @return gui
     */
    protected JPanel doMakeVerticalPositionPanel() {
        zPositionPanel = GuiUtils.hgrid(doMakeZPositionSlider(),
                                        GuiUtils.filler());
        GuiUtils.enableTree(zPositionPanel, !shouldUseAltitude);
        JRadioButton[] jrbs =
            GuiUtils.makeRadioButtons(Misc.newList("Altitude (if available)",
                "Fixed position:"), (shouldUseAltitude
                                     ? 0
                                     : 1), this, "setShouldUseAltitudeIndex");

        return GuiUtils.doLayout(new Component[] {
            GuiUtils.left(GuiUtils.hbox(jrbs[0], jrbs[1])),
            zPositionPanel }, 1, GuiUtils.WT_Y, GuiUtils.WT_N);

    }


    /**
     * Make Gui contents
     *
     * @return User interface contents
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {

        JComponent widgets = doMakeWidgetComponent();
        tableModel = new MyTableModel();
        int width = 300;
        if (timeSeries != null) {
            timeSeries.setControl(this);
        }
        table = new JTable(tableModel);
        table.setToolTipText(
            "Double click to add to or remove from chart. Right click change settings");
        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if ( !SwingUtilities.isRightMouseButton(e)
                        && (e.getClickCount() <= 1)) {
                    return;
                }
                final int row = table.rowAtPoint(e.getPoint());
                if ((row < 0) || (row >= getTableRows().size())) {
                    return;
                }
                PointParam pointParam = getPointParam(paramName);
                JPopupMenu popupMenu  = new JPopupMenu();
                String paramName = (String) tableModel.getValueAt(row, 0,
                                       false);
                boolean isShowing =
                    ((pointParam != null)
                     && pointParam.getLineState().getVisible());
                if ( !SwingUtilities.isRightMouseButton(e)
                        && (e.getClickCount() > 1)) {
                    //                    System.err.println("click " + isShowing);
                    if ( !isShowing) {
                        addChartParam(paramName);
                    } else {
                        //                        System.err.println("Removing:" + pointParam);
                        removeChartParam(pointParam);
                    }
                    return;
                }

                JMenuItem jmi;
                if ( !isShowing) {
                    popupMenu.add(GuiUtils.makeMenuItem("Add To Chart",
                            StationModelControl.this, "addChartParam",
                            paramName));
                    if (chartParams.size() > 0) {
                        popupMenu.addSeparator();
                    }
                }
                GuiUtils.makePopupMenu(popupMenu, getChartMenuItems());
                popupMenu.show(table, e.getX(), e.getY());
            }
        });
        table.setPreferredScrollableViewportSize(new Dimension(width, 100));
        tableScroller = new JScrollPane(table);
        tableScroller.setPreferredSize(new Dimension(width, 100));
        getChart().getContents().setPreferredSize(new Dimension(width, 200));

        selectedObLbl = new JLabel(" ");

        getChart().setEmptyChartLabel("Select a station in the main display");


        /*
                JComponent plotComp = GuiUtils.doLayout(new Component[] {
                                          getChart().getContents(),
                                          tableScroller }, 1, GuiUtils.WT_Y,
                                          new double[] {0, 2, 1 });*/


        //        JSplitPane split = GuiUtils.vsplit(getChart().getContents(),
        //                                           tableScroller, 0.75);
        //        split.setOneTouchExpandable(true);

        plotPanel = GuiUtils.centerBottom(getChart().getContents(),
                                          tableScroller);

        List timeWidgets = new ArrayList();
        timeWidgets.add(GuiUtils.rLabel("Show:"));
        timeWidgets.add(doMakeTimeOptionWidget());
        getDataTimeRange(true).setOneLineLabel(true);
        JPanel timeModePanel =
            GuiUtils.leftCenter(
                GuiUtils.wrap(
                    GuiUtils.makeImageButton(
                        "/auxdata/ui/icons/calendar_edit.png", this,
                        "showTimeRangeDialog")), GuiUtils.inset(
                            getDataTimeRange(true).getTimeModeLabel(),
                            new Insets(0, 10, 0, 0)));

        timeWidgets.add(GuiUtils.rLabel("Range:"));
        timeWidgets.add(timeModePanel);
        timeWidgets.add(GuiUtils.rLabel("Declutter:"));
        timeWidgets.add(doMakeTimeDeclutterWidget());

        GuiUtils.enableComponents(timeDeclutterComps,
                                  getTimeDeclutterEnabled());


        JComponent plotComp =
            GuiUtils.topCenter(GuiUtils.inset(selectedObLbl, 3), plotPanel);
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Layout", GuiUtils.top(widgets));
        LayoutUtil.tmpInsets = LayoutUtil.INSETS_5;
        JComponent timeComp = LayoutUtil.doLayout(timeWidgets, 2,
                                  GuiUtils.WT_N, GuiUtils.WT_N);
        tabbedPane.add("Times", GuiUtils.topLeft(timeComp));
        tabbedPane.add("Plot", plotComp);
        tabbedPane.add("Filters", doMakeFilterGui(false));
        return tabbedPane;

    }

    /**
     * Get the Chart menu items
     *
     * @return  the list of chart menu items
     */
    private List getChartMenuItems() {
        List menuItems = new ArrayList();
        for (int i = 0; i < chartParams.size(); i++) {
            PointParam pointParam = (PointParam) chartParams.get(i);
            LineState  lineState  = pointParam.getLineState();
            if ( !lineState.getValid() || !lineState.getVisible()) {
                continue;
            }
            JMenu paramMenu = new JMenu(pointParam.getName());
            menuItems.add(paramMenu);
            paramMenu.add(GuiUtils.makeMenuItem("Remove From Chart",
                    StationModelControl.this, "removeChartParam",
                    pointParam));
            paramMenu.add(GuiUtils.makeMenuItem("To Front",
                    StationModelControl.this, "toFront", pointParam));
            paramMenu.add(GuiUtils.makeMenuItem("Chart Properties",
                    StationModelControl.this, "showLineProperties",
                    pointParam));
        }
        return menuItems;
    }

    /**
     * Show the line properties for the PointParam
     *
     * @param pointParam  the point parameter description
     */
    public void showLineProperties(PointParam pointParam) {
        LineState              lineState = pointParam.getLineState();
        PropertyChangeListener listener  = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                chartChanged();
            }
        };
        if ( !lineState.showPropertiesDialog(listener,
                                             getChart().getPlotNames(),
                                             getChart().getCurrentRanges())) {
            return;
        }
    }


    /**
     * Handle when the time decluttering state has changed
     */
    protected void timeDeclutterChanged() {
        lastDeclutteredData = null;
        GuiUtils.enableComponents(timeDeclutterComps,
                                  getTimeDeclutterEnabled());
        loadDataInThread();
    }



    /**
     * Add the given component into the list of density components that are to
     * be enabled/disabled when the declutter checkbox is toggled.
     *
     * @param comp The component to add into the lsit
     *
     * @return The same component. We return it as a convenience.
     */
    protected JComponent addDensityComp(JComponent comp) {
        densityComps.add(comp);
        return comp;
    }

    /**
     * Add the given component into the list of time declutter components
     * that are to be enabled/disabled when the declutter checkbox is toggled.
     *
     * @param comp The component to add into the lsit
     *
     * @return The same component. We return it as a convenience.
     */
    protected JComponent addTimeDeclutterComp(JComponent comp) {
        timeDeclutterComps.add(comp);
        return comp;
    }

    /**
     * Create the 'Low' slider 'High' jpanel for the density slider.
     *
     * @return The panel that holds the density slider.
     */
    protected JPanel getDensityControl() {
        densitySlider = new JSlider(0, 10, 0);
        addDensityComp(densitySlider);
        GuiUtils.setSliderPercent(densitySlider,
                                  (double) 1.0f - declutterFilter);
        densitySlider.setToolTipText(
            "Control the density of the plot displays");
        densitySlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (densitySlider.getValueIsAdjusting()) {
                    return;
                }
                float newValue =
                    1.0f - (float) GuiUtils.getSliderPercent(densitySlider);
                if (newValue == declutterFilter) {
                    return;
                }
                setDeclutterFilter(newValue);
                if (getDeclutter()) {
                    loadDataInThread();
                }
            }
        });


        JComponent comp = GuiUtils.doLayout(new Component[] {
                              addDensityComp(GuiUtils.rLabel("Low ")),
                              densitySlider,
                              addDensityComp(GuiUtils.lLabel(" High")) }, 3,
                                  GuiUtils.WT_NYN, GuiUtils.WT_N);

        return GuiUtils.hgrid(comp, GuiUtils.filler());
    }

    /**
     * Get edit menu items
     *
     * @param items      list of menu items
     * @param forMenuBar  true if for the menu bar
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        makeStationModelEditMenuItems(items, forMenuBar);
        super.getEditMenuItems(items, forMenuBar);
    }


    /**
     * Get edit menu items
     *
     * @param items      list of menu items
     * @param forMenuBar  true if for the menu bar
     */
    protected void makeStationModelEditMenuItems(List items,
            boolean forMenuBar) {
        items.add(GuiUtils.makeMenuItem("Layout Model...", this,
                                        "editStationTable"));
    }



    /**
     * Add properties to the display settings dialog
     *
     * @param dsd display settings dialog
     */
    protected void addDisplaySettings(DisplaySettingsDialog dsd) {
        super.addDisplaySettings(dsd);
        if (getDataTimeRange() != null) {
            dsd.addPropertyValue(getDataTimeRange(), "dataTimeRange",
                                 "Accumulation Times", "Display");
        }
        dsd.addPropertyValue(new Boolean(getUseDataTimes()), "useDataTimes",
                             "Use Data Times", SETTINGS_GROUP_DISPLAY);

        dsd.addPropertyValue(getStationModelName(), "stationModelName",
                             "Layout Model", SETTINGS_GROUP_DISPLAY);



        if ( !isChartEnabled()) {
            return;
        }
        try {
            TimeSeriesChart clonedTimeSeries =
                (TimeSeriesChart) getIdv().decodeObject(
                    getIdv().encodeObject(timeSeries, false));
            clonedTimeSeries.setControl(null);
            List tmp = new ArrayList();
            for (int i = 0; i < chartParams.size(); i++) {
                PointParam cp = (PointParam) chartParams.get(i);
                if (cp.getLineState().getVisible()) {
                    tmp.add(cp);
                }
            }
            String label = StringUtil.join(",", tmp);
            TwoFacedObject tfo = new TwoFacedObject(label,
                                     new Object[] { clonedTimeSeries,
                    tmp });
            dsd.addPropertyValue(tfo, "chartSettings", "Chart Settings",
                                 "Display");

        } catch (Exception exc) {
            logException("Applying chart settings", exc);
        }
    }

    /**
     * recieve the chart display settings
     *
     * @param tfo Holds the chart to apply
     */
    public void setChartSettings(TwoFacedObject tfo) {
        try {
            Object[] a = (Object[]) tfo.getId();
            timeSeries = (TimeSeriesChart) getIdv().decodeObject(
                getIdv().encodeObject(a[0], false));
            timeSeries.setControl(this);
            chartParams = new ArrayList((List) a[1]);
            if (plotPanel != null) {
                plotPanel.removeAll();
                plotPanel.add(BorderLayout.CENTER, getChart().getContents());
                plotPanel.add(BorderLayout.SOUTH, tableScroller);
                plotPanel.invalidate();
                plotPanel.validate();
                plotPanel.repaint();
                getChart().getContents().repaint(5);
                chartChanged();
            }
        } catch (Exception exc) {
            logException("Applying chart settings", exc);
        }
    }


    /**
     * Is the chart enabled?
     *
     * @return  true if it is enabled
     */
    protected boolean isChartEnabled() {
        return true;
    }


    /**
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getSaveMenuItems(List items, boolean forMenuBar) {

        super.getSaveMenuItems(items, forMenuBar);
        if (isChartEnabled()) {
            items.add(GuiUtils.makeMenuItem("Save Chart Image...",
                                            getChart(), "saveImage"));
        }

        PointDataInstance pdi = (PointDataInstance) getDataInstance();
        if (pdi != null) {
            items.add(GuiUtils.makeMenuItem("Export to NetCDF...", this,
                                            "exportAsNetcdf", null, true));

            items.add(GuiUtils.makeMenuItem("Export to KMZ...", this,
                                            "exportAsKmz", null, true));
        }

        if ((table != null) && (table.getModel().getRowCount() > 0)) {
            items.add(GuiUtils.makeMenuItem("Export Selected Observation...",
                                            this, "exportAsCsv"));
        }
    }


    /** _more_ */
    JTextField kmzWidthFld;

    /** _more_ */
    JTextField kmzHeightFld;

    /** _more_ */
    JTextField kmzNameFld;

    /** _more_ */
    GuiUtils.ColorSwatch kmzColorSwatch;

    /**
     * _more_
     */
    public void exportAsKmz() {
        try {
            if (kmzWidthFld == null) {
                kmzWidthFld  = new JTextField("80", 5);
                kmzHeightFld = new JTextField("80", 5);
                kmzNameFld   = new JTextField("Point Observations");
                kmzColorSwatch = new GuiUtils.ColorSwatch(Color.white,
                        "KMZ Icon Color", true);
            }

            JComponent widgets = GuiUtils.formLayout(new Component[] {
                GuiUtils.rLabel("Name:"), kmzNameFld,
                GuiUtils.rLabel("Icon Size:"),
                GuiUtils.left(GuiUtils.hbox(kmzWidthFld, new JLabel(" X "),
                                            kmzHeightFld)),
                GuiUtils.rLabel("BG Color:"), kmzColorSwatch.getPanel()
            });
            JComboBox publishCbx =
                getIdv().getPublishManager().getSelector("kmz.export");
            JComponent accessory = ((publishCbx != null)
                                    ? GuiUtils.topBottom(widgets, publishCbx)
                                    : widgets);
            String filename =
                FileManager.getWriteFile(FileManager.FILTER_KMZ,
                                         FileManager.SUFFIX_KMZ,
                                         GuiUtils.top(accessory));


            if (filename == null) {
                return;
            }



            if ( !myDisplay.writeKmzFile(
                    new File(filename), currentStationData,
                    kmzNameFld.getText(),
                    new Integer(kmzWidthFld.getText().trim()).intValue(),
                    new Integer(kmzHeightFld.getText().trim()).intValue(),
                    kmzColorSwatch.getColor())) {
                return;
            }
            getIdv().getPublishManager().publishContent(filename, null,
                    publishCbx);
        } catch (Exception exc) {
            logException("Exporting point data to kmz", exc);
        }

    }



    /**
     * Export the table as csv
     */
    public void exportAsCsv() {
        GuiUtils.exportAsCsv(table.getModel());
    }


    /**
     * Do we have any filters
     *
     * @return have filters
     */
    protected boolean haveFilters() {
        return filtersEnabled && (filters.size() > 0);
    }

    /**
     * Init the vis filters
     */
    public void initFilters() {
        if (filterGui != null) {
            filters        = filterGui.getFilters();
            matchAll       = filterGui.getMatchAll();
            filtersEnabled = filterGui.getEnabled();
        }
    }


    /**
     * Apply the vis filters
     */
    public void applyFilters() {
        lastDeclutteredData = null;
        initFilters();
        loadData();
    }


    /**
     * Make the filter GUI
     *
     *
     * @param includeAll Include the radio buttons
     * @return The filter gui
     */
    protected JComponent doMakeFilterGui(boolean includeAll) {
        JComponent buttons = GuiUtils.makeButton("Apply Filters", this,
                                 "applyFilters");
        if (includeAll) {
            JRadioButton[] rbs =
                GuiUtils.makeRadioButtons(Misc.newList("Only show these",
                    "Always show these"), (onlyShowFiltered
                                           ? 0
                                           : 1), this, "handleOnlyShow");

            buttons = GuiUtils.hbox(buttons, rbs[0], rbs[1]);
        }


        JComponent gui =
            GuiUtils.topCenter(GuiUtils.left(GuiUtils.inset(buttons, 5)),
                               filterGui.getContents());
        return gui;

    }


    /**
     * call back from radio buttons in gui for filters
     *
     * @param index Which radio button
     */
    public void handleOnlyShow(int index) {
        setOnlyShowFiltered(index == 0);

    }



    /**
     * Return the list of names that shows up in the filter gui names combob box.
     *
     * @return List of filter names
     */
    protected List getFilterNames() {
        try {
            PointDataInstance pdi = (PointDataInstance) getDataInstance();
            if (pdi == null) {
                return null;
            }
            FieldImpl data = pdi.getPointObs();
            /*
              if (GridUtil.isTimeSequence(data)) {
              Set timeSet  = data.getDomainSet();
              int numTimes = timeSet.getLength();
              if (numTimes == 0) {
              return null;
              }
              data = (FieldImpl) data.getSample(0);
              }
            */
            Set domainSet = data.getDomainSet();
            int numObs    = domainSet.getLength();
            if (numObs == 0) {
                return null;
            }
            PointOb    ob        = (PointOb) data.getSample(0);
            Data       tuple     = ob.getData();
            TupleType  tupleType = (TupleType) tuple.getType();
            List       names     = new ArrayList();
            MathType[] types     = tupleType.getComponents();
            names.add(PropertyFilter.NULL_NAME);
            for (int i = 0; i < types.length; i++) {
                String typeId   = types[i].toString();
                String typeName = Util.cleanTypeName(typeId);
                names.add(new TwoFacedObject(typeName, typeId));
            }
            names.add(new TwoFacedObject("Latitude", PointOb.PARAM_LAT));
            names.add(new TwoFacedObject("Longitude", PointOb.PARAM_LON));
            names.add(new TwoFacedObject("Altitude", PointOb.PARAM_ALT));
            return names;
        } catch (Exception exc) {
            logException("Getting filter names", exc);
        }
        return null;
    }



    /**
     * Set the icon and the tooltip on the lock button
     */
    protected void updateLockButton() {
        lockBtn.setIcon(stationsLocked
                        ? lockIcon
                        : unlockIcon);
        lockBtn.setToolTipText(stationsLocked
                               ? "Unlock station display"
                               : "Lock station display");
    }


    /**
     * Remove this DisplayControl from the system.  Nulls out any
     * objects for garbage collection, removes any
     * ProjectionControlListeners.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doRemove() throws VisADException, RemoteException {
        getControlContext().getStationModelManager()
            .removePropertyChangeListener(this);
        rangeColorPreview = null;
        super.doRemove();
    }

    /**
     * Set the times in the times holder
     *
     * @param timeData  the data with times
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    private void setDataTimes(FieldImpl timeData)
            throws VisADException, RemoteException {
        if (timesHolder == null) {
            return;
        }
        if (getUseDataTimes()) {
            timesHolder.setData(DUMMY_DATA);
            return;
        }
        if (GridUtil.isTimeSequence(timeData)) {
            Set timeSet = timeData.getDomainSet();
            timesHolder.setData(timeSet);
        }
    }


    /**
     * Declutters the observations.  This is just a wrapper around
     * the real decluttering in {@link #doTheActualDecluttering(FieldImpl)}
     * to handle the case where there is a time sequence of observations.
     *
     * @param  obs initial field of observations.
     * @param timestamp _more_
     *
     * @return a decluttered version of obs
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private FieldImpl doDeclutter(FieldImpl obs, int timestamp)
            throws VisADException, RemoteException {

        long      millis           = System.currentTimeMillis();
        boolean   isTimeSequence   = GridUtil.isTimeSequence(obs);
        FieldImpl declutteredField = null;
        if (isTimeSequence) {
            Set timeSet = obs.getDomainSet();
            declutteredField = new FieldImpl((FunctionType) obs.getType(),
                                             timeSet);
            int numTimes = timeSet.getLength();
            for (int i = 0; i < numTimes; i++) {
                FieldImpl oneTime = (FieldImpl) obs.getSample(i);
                FieldImpl subTime = doTheActualDecluttering(oneTime,
                                        timestamp);
                if (timestamp != loadDataTimestamp) {
                    return null;
                }
                if (subTime != null) {
                    declutteredField.setSample(i, subTime, false);
                }
            }
        } else {
            declutteredField = doTheActualDecluttering(obs, timestamp);
        }
        //System.out.println("Subsetting took : " +
        //    (System.currentTimeMillis() - millis) + " ms");
        return declutteredField;
    }

    /**
     * a     * Declutters a single timestep of observations.
     *
     * @param pointObs  point observations for one timestep.
     * @param timestamp _more_
     *
     * @return a decluttered version of pointObs
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private FieldImpl doTheActualDecluttering(FieldImpl pointObs,
            int timestamp)
            throws VisADException, RemoteException {
        if ((pointObs == null) || pointObs.isMissing()) {
            return pointObs;
        }
        FieldImpl retField    = null;
        Set       domainSet   = pointObs.getDomainSet();
        int       numObs      = domainSet.getLength();
        Vector    v           = new Vector();

        long      t1          = System.currentTimeMillis();
        Rectangle glyphBounds = getStationModel().getBounds();

        float     myScale = getScale() * .0025f * getDeclutterFilter();

        Rectangle2D scaledGlyphBounds =
            new Rectangle2D.Double(glyphBounds.getX() * myScale,
                                   glyphBounds.getY() * myScale,
                                   glyphBounds.getWidth() * myScale,
                                   glyphBounds.getHeight() * myScale);
        NavigatedDisplay   navDisplay = getNavigatedDisplay();

        Rectangle2D.Double obBounds   = new Rectangle2D.Double();
        obBounds.width  = scaledGlyphBounds.getWidth();
        obBounds.height = scaledGlyphBounds.getHeight();

        Rectangle2D bounds = getBounds();
        //        System.out.println("my bounds: x:" + bounds.getX()+"-" +(bounds.getX()+bounds.getWidth())+" y:" +
        //                           bounds.getY()+"-" +(bounds.getY()+bounds.getHeight()));


        if (stationGrid == null) {
            stationGrid = new SpatialGrid(200, 200);
        }
        stationGrid.clear();
        stationGrid.setGrid(bounds, scaledGlyphBounds);
        if (getDeclutterFilter() < 0.3f) {
            //      stationGrid.setOverlap((int)((1.0-getDeclutterFilter())*100));
            //      stationGrid.setOverlap(          (int)((.5f-getDeclutterFilter())*100));
        } else {
            //      stationGrid.setOverlap(0);
        }

        double[] xyz = new double[3];
        //TODO: The repeated getSpatialCoords is a bit expensive
        for (int i = 0; i < numObs; i++) {
            if (timestamp != loadDataTimestamp) {
                return null;
            }
            PointOb ob = (PointOb) pointObs.getSample(i);
            xyz = navDisplay.getSpatialCoordinates(ob.getEarthLocation(),
                    xyz, 0);
            obBounds.x = xyz[0];
            obBounds.y = xyz[1];
            //            if(i<30)
            //                System.err.println("\tel:" + ob.getEarthLocation() +" obBounds:" + obBounds.x +"/" + obBounds.y);
            if (stationGrid.markIfClear(obBounds, "") || isSelected(ob)) {
                v.add(ob);  // is in the bounds
            }
        }
        //      stationGrid.print();
        long t2 = System.currentTimeMillis();


        if (v.isEmpty()) {
            retField = new FieldImpl(
                (FunctionType) pointObs.getType(),
                new Integer1DSet(
                    ((SetType) domainSet.getType()).getDomain(), 1));
            retField.setSample(0, pointObs.getSample(0), false);
        } else if (v.size() == numObs) {
            retField = pointObs;  // all were in domain, just return input
        } else {
            retField = new FieldImpl(
                (FunctionType) pointObs.getType(),
                new Integer1DSet(
                    ((SetType) domainSet.getType()).getDomain(), v.size()));
            retField.setSamples((PointOb[]) v.toArray(new PointOb[v.size()]),
                                false, false);
        }

        long t3 = System.currentTimeMillis();
        //System.err.println("size:" + v.size() +" declutter:" + (t2-t1) + " " + (t3-t2));


        return retField;
    }


    /**
     * Set whether this DisplayControl should be decluttered or not.
     * Used by XML persistence.
     *
     * @param v true to declutter
     */
    public void setDeclutter(boolean v) {
        declutter = v;
        GuiUtils.enableComponents(densityComps, declutter);
    }

    /**
     * Get whether this DisplayControl should be decluttered or not.
     *
     * @return true if set to declutter
     */
    public boolean getDeclutter() {
        return declutter;
    }





    /**
     * Set the locking of the stations parameter
     *
     * @param v true to stationsLocked
     */
    public void setStationsLocked(boolean v) {
        stationsLocked = v;
        if (lockBtn != null) {
            updateLockButton();
        }
    }

    /**
     * Get the locking of the stations parameter
     *
     * @return true if set to stationsLocked
     */
    public boolean getStationsLocked() {
        return stationsLocked;
    }

    /**
     * Set whether the filtering for decluttering.
     * Used by XML persistence.
     *
     * @param filter value of 1 (default) for no overlap (default).
     *               0 &lt; filter &lt; 1 allows some data overlap.
     *               filter &gt; 1 causes data to be more widely spaced.
     */
    public void setDeclutterFilter(float filter) {
        declutterFilter = filter;
    }

    /**
     * Get whether this DisplayControl should be decluttered or not.
     *
     * @return weighting for decluttering.
     */
    public float getDeclutterFilter() {
        return declutterFilter;
    }


    /**
     * Get the scaling used for this object to control the size of
     * the shapes.
     *
     * @return scaling used for sizing/decluttering shapes.
     */
    protected float getScale() {
        return getScaleFromDisplayable();
    }

    /**
     * Get the bounds for the visible portion of the screen.
     *
     * @return bounds in VisAD screen coordinates.
     */
    protected Rectangle2D getBounds() {
        return calculateRectangle();
    }

    /**
     * Calculate the LatLonBounds based on the VisAD screen bound.  This
     * uses the projection for the navigated display and the screen bounds
     *
     * @param screenBounds  VisAD screen bounds.
     *
     * @return  LinearLatLonSet of screen bounds in lat/lon coordinates.
     */
    protected LinearLatLonSet calculateLatLonBounds(
            Rectangle2D screenBounds) {
        if ((screenBounds.getWidth() == 0)
                || (screenBounds.getHeight() == 0)) {
            return null;
        }

        LinearLatLonSet bounds = null;
        try {

            Rectangle2D.Double rect = getNavigatedDisplay().getLatLonBox();


            //            System.err.println("llb:" + rect);

            bounds =
                new LinearLatLonSet(RealTupleType.LatitudeLongitudeTuple,
                                    rect.y, rect.y + rect.height, 11, rect.x,
                                    rect.x + rect.width, 11);

        } catch (Exception e) {
            try {
                bounds =
                    new LinearLatLonSet(RealTupleType.LatitudeLongitudeTuple,
                                        -90, 90, 19, -180, 180, 37);
            } catch (Exception ne) {
                logException("calculating LLLSet ", ne);
            }
        }
        return bounds;
    }


    /**
     * Decode the selected filter string and return the corresponding
     * filter.  This method allows someone to type in YES and NO for
     * values of 1 and 0 respectively, as in GEMPAK.
     *
     * @param filter   filter as a string
     * @return scaling factor for filtering.
     */
    protected float decodeFilterString(String filter) {
        if (filter.toLowerCase().startsWith("y")) {
            return 1.0f;
        } else if (filter.toLowerCase().startsWith("n")) {
            return 0.01f;
        }
        float value = getDeclutterFilter();
        try {
            value = (float) Misc.parseNumber(filter);
            if (value == 0.f) {
                value = 0.001f;
            }
        } catch (NumberFormatException nfe) {}
        return value;
    }


    /**
     * Return the label that is to be used for the color widget
     *
     * @return Label used for the color widget
     */
    public String getColorWidgetLabel() {
        return "Color";
    }



    /**
     *  Set the Filters property.
     *
     *  @param value The new value for Filters
     */
    public void setFilters(List value) {
        filters = value;
    }

    /**
     *  Get the Filters property.
     *
     *  @return The Filters
     */
    public List getFilters() {
        return filters;
    }


    /**
     * Set the MatchAll property.
     *
     * @param value The new value for MatchAll
     */
    public void setMatchAll(boolean value) {
        matchAll = value;
    }

    /**
     * Get the MatchAll property.
     *
     * @return The MatchAll
     */
    public boolean getMatchAll() {
        return matchAll;
    }

    /**
     * Set the FiltersEnabled property.
     *
     * @param value The new value for FiltersEnabled
     */
    public void setFiltersEnabled(boolean value) {
        filtersEnabled = value;
    }

    /**
     * Get the FiltersEnabled property.
     *
     * @return The FiltersEnabled
     */
    public boolean getFiltersEnabled() {
        return filtersEnabled;
    }

    /**
     * Set the show all times property.
     *
     * @param value The new value for showAllTimes
     */
    public void setShowAllTimes(boolean value) {
        setUseDataTimes( !value);
    }

    /**
     * Set the use data times times property.
     *
     * @param value The new value for use data times
     */
    public void setUseDataTimes(boolean value) {
        useDataTimes = value;
        if (getHaveInitialized()) {
            try {
                loadDataInThread();
            } catch (Exception e) {
                logException("setUseDataTimes", e);
            }
        }
    }

    /**
     * Get the use data times property.
     *
     * @return The use data times property
     */
    public boolean getUseDataTimes() {
        return useDataTimes;
    }


    /**
     * Get the scale the user can enter
     *
     * @return The scale
     */
    public float getDisplayableScale() {
        return displayableScale;
    }

    /**
     * Set the scale the user can enter
     *
     * @param f The scale
     */
    public void setDisplayableScale(float f) {
        displayableScale = f;
        if (myDisplay != null) {
            try {
                setScaleOnDisplayable();
            } catch (Exception exc) {
                logException("Setting scale ", exc);
            }
        }
    }

    /**
     * Set the UseLastTime property.
     *
     * @param value The new value for UseLastTime
     */
    public void setUseLastTime(boolean value) {
        useLastTime = value;
    }

    /**
     * Get the UseLastTime property.
     *
     * @return The UseLastTime
     */
    public boolean getUseLastTime() {
        return useLastTime;
    }


    /**
     * Respond to changes in the control.
     */
    public void viewpointChanged() {
        super.viewpointChanged();
        synchronized (MUTEX) {
            if ( !getHaveInitialized() || !getActive()) {
                return;
            }
            Rectangle2D newBounds    = calculateRectangle();
            boolean     shouldReload = false;

            if (inGlobe) {
                double[] rotation = getNavigatedDisplay().getRotation();
                if (lastRotation == null) {
                    shouldReload = true;
                } else {
                    if ( !java.util.Arrays.equals(rotation, lastRotation)) {
                        //TODO: Check if the rotation changed considerably
                        shouldReload = true;
                    }
                }
                lastRotation = rotation;
            }

            if ( !shouldReload) {
                if ((lastViewBounds == null)
                        || (lastViewBounds.getWidth() == 0)
                        || (lastViewBounds.getHeight() == 0)) {
                    shouldReload = true;
                } else if ( !(newBounds.equals(lastViewBounds))) {
                    double widthratio = newBounds.getWidth()
                                        / lastViewBounds.getWidth();
                    double heightratio = newBounds.getHeight()
                                         / lastViewBounds.getHeight();
                    double xdiff = Math.abs(newBounds.getX()
                                            - lastViewBounds.getX());
                    double ydiff = Math.abs(newBounds.getY()
                                            - lastViewBounds.getY());
                    // See if this is 20% greater or smaller than before.
                    if ((((widthratio < .80) || (widthratio > 1.20))
                            && ((heightratio < .80)
                                || (heightratio > 1.20))) || ((xdiff
                                   > .2 * lastViewBounds
                                       .getWidth()) || (ydiff
                                           > .2 * lastViewBounds
                                               .getHeight()))) {
                        shouldReload = true;
                    }
                }
                float newScale = getScaleFromDisplayable();
                if (Float.floatToIntBits(lastViewScale)
                        != Float.floatToIntBits(newScale)) {
                    shouldReload = true;
                }
            }

            if (shouldReload) {
                if ( !stationsLocked) {
                    loadDataInAWhile();
                } else if (inGlobe) {
                    loadDataInAWhile();
                }
            }

        }

    }





    /**
     * This is how long (1/5 second) we wait from the time we get
     * the controlChanged event to the time we actually do the loadData.
     * It is also the time we sleep if another controlChanged event
     * has come in.
     */
    private static final long SLEEPTIME_MS = 200;





    /**
     * This checks to see if we have a pending loadData call
     * (from a prior event). If so then -  return. Else, create a
     * runnable that will (in SLEEPTIME_MS time) check if there has been
     * no calls to this method since it started. If so then it loads data.
     * Else it sleeps and keeps checking. <p>
     * Note: This method does not need to be synchronized because it
     * is called from within a synchronized block above.
     */
    protected void loadDataInAWhile() {

        //This is the time we last called this method
        lastTimeLoadDataWasCalled = System.currentTimeMillis();

        DataLoader dataLoader;
        synchronized (LOADDATA_MUTEX) {
            //Do we have a pending loadData already running? 
            if (waitingToLoad) {
                return;
            }
            waitingToLoad = true;
            dataLoader    = new DataLoader();
        }
        Misc.runInABit(SLEEPTIME_MS, dataLoader);
    }

    /**
     * Class DataLoader handles the delayed loadDataInAWhile calls
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private class DataLoader implements Runnable {

        /** Last time we checked */
        long lastCheckLoadTime;

        /**
         * ctor
         */
        public DataLoader() {
            lastCheckLoadTime = lastTimeLoadDataWasCalled;
        }

        /**
         * run
         */
        public void run() {
            while (true) {
                // Check if we can load data 
                boolean sleepAndContinue = false;
                sleepAndContinue = (lastTimeLoadDataWasCalled
                                    != lastCheckLoadTime);
                if (sleepAndContinue) {
                    lastCheckLoadTime = lastTimeLoadDataWasCalled;
                    Misc.sleep(SLEEPTIME_MS);
                    continue;
                }
                loadData();
                synchronized (LOADDATA_MUTEX) {
                    waitingToLoad = false;
                }
                return;
            }

        }
    }

    ;




    /**
     * Set the OnlyShowFiltered property.
     *
     * @param value The new value for OnlyShowFiltered
     */
    public void setOnlyShowFiltered(boolean value) {
        onlyShowFiltered = value;
        if (getHaveInitialized()) {
            loadData();
        }
    }

    /**
     * Get the OnlyShowFiltered property.
     *
     * @return The OnlyShowFiltered
     */
    public boolean getOnlyShowFiltered() {
        return onlyShowFiltered;
    }


    /**
     * Set the ShouldUseAltitude property.
     *
     * @param value The new value for ShouldUseAltitude
     */
    public void setShouldUseAltitude(boolean value) {
        shouldUseAltitude = value;
        if (myDisplay != null) {
            try {
                myDisplay.setShouldUseAltitude(shouldUseAltitude);
                if ( !shouldUseAltitude) {
                    applyZPosition();
                }
            } catch (Exception exc) {
                logException("Setting shouldUseAltitude", exc);
            }
        }
        if (zPositionPanel != null) {
            GuiUtils.enableTree(zPositionPanel, !shouldUseAltitude);
        }
    }

    /**
     * Get the ShouldUseAltitude property.
     *
     * @return The ShouldUseAltitude property
     */
    public boolean getShouldUseAltitude() {
        return shouldUseAltitude;
    }



    /**
     * Set the ShouldUseAltitude property.
     *
     * @param index The index
     */
    public void setShouldUseAltitudeIndex(int index) {
        setShouldUseAltitude(index == 0);
    }

    /**
     *  Set the AskedUserToDeclutterTime property.
     *
     *  @param value The new value for AskedUserToDeclutterTime
     */
    public void setAskedUserToDeclutterTime(boolean value) {
        askedUserToDeclutterTime = value;
    }

    /**
     *  Get the AskedUserToDeclutterTime property.
     *
     *  @return The AskedUserToDeclutterTime
     */
    public boolean getAskedUserToDeclutterTime() {
        return askedUserToDeclutterTime;
    }


    /**
     *  Set the PlotVars property.
     *
     *  @param value The new value for PlotVars
     */
    public void setChartParams(List value) {
        chartParams = value;
    }

    /**
     *  Get the ChartParams property.
     *
     *  @return The ChartParams
     */
    public List getChartParams() {
        return chartParams;
    }


    /**
     * Customized class for the table
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.228 $
     */
    private class MyTableModel extends AbstractTableModel {

        /**
         * IS the cell editable?
         *
         * @param rowIndex  cell row index
         * @param columnIndex  cell column index
         *
         * @return false
         */
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        /**
         * Set the value at the row/column index
         *
         * @param aValue  the value
         * @param rowIndex  row index
         * @param columnIndex  column index
         */
        public void setValueAt(Object aValue, int rowIndex,
                               int columnIndex) {}

        /**
         * Get the number of rows
         *
         * @return  the number of rows
         */
        public int getRowCount() {
            return Math.max(getTableRows().size(), 30);
        }

        /**
         * Get the number of colunms
         *
         * @return the number of colunms
         */
        public int getColumnCount() {
            return 2;
        }

        /**
         * Get the value at the row/column
         *
         * @param row   row index
         * @param column   column index
         *
         * @return the object at that cell
         */
        public Object getValueAt(int row, int column) {
            return getValueAt(row, column, true);
        }

        /**
         * Get the value at the row/column
         *
         * @param row     row index
         * @param column  column index
         * @param forTable  true if for the table
         *
         * @return  the Object
         */
        public Object getValueAt(int row, int column, boolean forTable) {
            List tableRows = getTableRows();
            if (row >= tableRows.size()) {
                return "";
            }
            List rowData = (List) tableRows.get(row);
            if (forTable && (column == 0)) {
                String     name       = (String) rowData.get(column);
                PointParam pointParam = getPointParam(name);
                if ((pointParam != null)
                        && pointParam.getLineState().getVisible()) {
                    name = name + " (in chart)";
                }
                return name;
            }
            if (column < rowData.size()) {
                return rowData.get(column);
            }
            return "";
        }

        /**
         * Get the column name
         *
         * @param column  the column
         *
         * @return  the name
         */
        public String getColumnName(int column) {
            if (column == 0) {
                return "Field";
            }
            return "Value";
        }
    }


    /**
     *  Set the SelectedObId property.
     *
     *  @param value The new value for SelectedObId
     */
    public void setSelectedObId(String value) {
        selectedObId = value;
    }

    /**
     *  Get the SelectedObId property.
     *
     *  @return The SelectedObId
     */
    public String getSelectedObId() {
        return selectedObId;
    }

    /**
     *  Set the TimeSeries property.
     *
     *  @param value The new value for TimeSeries
     */
    public void setTimeSeries(TimeSeriesChart value) {
        timeSeries = value;
    }

    /**
     *  Get the TimeSeries property.
     *
     *  @return The TimeSeries
     */
    public TimeSeriesChart getTimeSeries() {
        return timeSeries;
    }

    /**
     * Get the chart
     *
     * @return  the chart
     */
    public TimeSeriesChart getChart() {
        if (timeSeries == null) {
            timeSeries = new TimeSeriesChart(this, "Plot");
            timeSeries.setEmptyChartLabel(
                "Select a station in the main display");
            timeSeries.showAnimationTime(true);
        }
        return timeSeries;
    }


    /**
     * Set the ShowThumbNail property.
     *
     * @param value The new value for ShowThumbNail
     */
    public void setShowThumbNail(boolean value) {
        getChart().setShowThumb(value);
    }


    /**
     * Set the SelectedObLocation property.
     *
     * @param value The new value for SelectedObLocation
     */
    public void setSelectedObLocation(LatLonPoint value) {
        selectedObLocation = value;
    }

    /**
     * Get the SelectedObLocation property.
     *
     * @return The SelectedObLocation
     */
    public LatLonPoint getSelectedObLocation() {
        return selectedObLocation;
    }



}
