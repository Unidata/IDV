/*
 * Copyright 1997-2022 Unidata Program Center/University Corporation for
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

import static ucar.unidata.util.GuiUtils.hbox;
import static ucar.unidata.util.GuiUtils.filler;
import static ucar.unidata.util.GuiUtils.left;
import static ucar.unidata.util.GuiUtils.topLeft;
import static ucar.unidata.util.CollectionHelpers.arr;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import ucar.unidata.data.adt.Data;
import ucar.unidata.data.adt.Env;
import ucar.unidata.data.adt.Functions;
import ucar.unidata.data.adt.History;
import ucar.unidata.data.adt.Main;
import ucar.unidata.data.adt.ReadIRImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.imagery.AddeImageDataSource;
import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.ImageDataSource;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.idv.DisplayInfo;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.unidata.xml.XmlObjectStore;
import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.display.PointProbe;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.quantities.AirTemperature;
import visad.CommonUnit;
import visad.DateTime;
import visad.DisplayEvent;
import visad.FieldImpl;
import visad.FlatField;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.Set;
import visad.VisADException;
import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.util.DataUtility;
import edu.wisc.ssec.mcidas.AreaDirectory;

/**
 * Advanced Dvorak Technique Display Control
 * Algorithm developed at UW Madison/CIMSS to objectively determine tropical
 * cyclone intensity from geostationary satellite infrared imagery.
 *
 * @author Tim Olander
 */

public class ADTControl extends DisplayControlImpl {

    public static final String[] SCENE_TYPES = {
            "Eye", "Pinhole Eye", "Large Eye", "CDO", "Embedded Center",
            "Irregular CDO", "Curved Band", "Shear"
    };
    private static final Logger logger = LoggerFactory.getLogger(ADTControl.class);
    // Tooltip strings for the various UI buttons and inputs
    private static final String TOOLTIP_LAND_FLAG_ON = "Apply ADT Land Interaction Rule";
    private static final String TOOLTIP_LAND_FLAG_OFF = "Do Not Apply ADT Land Interaction Rule";
    private static final String TOOLTIP_MANUAL = "Manually Select Storm Center In Image";
    private static final String TOOLTIP_AUTOMATIC = "Select Forecast File For First Guess Below";
    private static final String TOOLTIP_HISTORY = "Choose a File to Retain and Store ADT Output Data";
    private static final String TOOLTIP_PMW = "Supplement Analysis With Passive Microwave Eye Score";
    private static final String TOOLTIP_PENV = "Environmental Mean Sea Level Pressure";
    private static final String TOOLTIP_34KT = "34 Knot Wind/Gale Radius";
    private static final String TOOLTIP_MSLP_FROM_DVORAK = "Utilize Dvorak Technique to Derive MSLP";
    private static final String TOOLTIP_MSLP_FROM_CKZ = "Utilize Coutney/Knaff/Zehr Wind Speed/Presssure Technique";
    private static final String TOOLTIP_RMW = "Manually Input Radius of Maximum Wind";
    private static final String TOOLTIP_RAW_T = "Manually Define Initial Value for New Storm";
    private static final String TOOLTIP_STORM_ID = "Three Character WMO Storm Identfier";
    private static final String TOOLTIP_SITE_ID = "Four Character Site Analysis Identifier";
    private static final String[] FORECAST_TYPES = {
            "ATCF", "DISC", "PACWARN", "GENERIC", "RMSC ICAO", "RMSC WTIO",
            "TCWC AXAU", "BEST", "HURDAT"
    };
    private static final String DEFAULT_PENV = "1012";
    private static final String DEFAULT_RADIUS = "300";
    private static final String SCENE_TYPE_PREFIX = "Current Scene Type: ";
    /** _more_ */
    private static boolean running = false;
    private static boolean runFullADTAnalysis = false;
    private static boolean GUIFileOverrideTF = false;
    private static boolean GUIOverrideSceneTF;
    private static boolean GUIRunAutoTF;
    private static boolean GUIOverrideTF;
    private static boolean GUIATCFOutputTF;
    private static boolean GUIInitStrengthTF;
    private static boolean GUILandFlagTF;
    private static boolean GUIVmax1or10TF = true;
    private static boolean GUICommentAddTF;
    private static boolean GUIDeleteTF;
    private static boolean GUIATCFRecordOutputTF;
    private static boolean GUIPMWActivateTF;
    // need to determine or provide option
    private static int GUIDomainID;
    // need to initialize pulldown menu
    private static int GUIForecastType = 0;
    private static int GUIMWJulianDate;
    private static int GUIMWHHMMSSTime;
    private static int GUIStartDate;
    private static int GUIStartTime;
    private static int GUIEndDate;
    private static int GUIEndTime;
    private static int GUIHistoryListFormat;
    private static double GUIRawTValue;
    private static double GUIMWScore;
    private static double GUICKZGaleRadius;
    private static double GUICKZPenv;
    private static double GUIRMWSize;
    private static double GUIUserLatitude;
    private static double GUIUserLongitude;
    private static String GUIForecastFileName;
    private static String GUIHistoryFileName;
    private static String GUIHistoryFileListingName;
    private static String GUICommentString;
    private static String HistoryListOutput;
    // CKZ params will need to be validated before running
    JTextField ckzPenvTextField = null;
    JTextField ckz34radiusTextField = null;
    JTextField ATCFEntryStormTextField = null;
    JTextField ATCFEntrySiteTextField = null;
    /** _more_ */
    private LatLonWidget latLonWidget;
    /** the probe */
    private PointProbe probe;
    /** _more_ */
    private LatLonPoint probeLocation;
    /** _more_ */
    private DataChoice choice;
    // Default Java boolean value is false - need to initialize if we want true
    private boolean GUIUseCKZTF = false;
    private String GUIATCFStormID = null;
    private String GUIATCFSiteID = null;
    /** _more_ */
    private JButton adtBtn;
    private JButton forecastBtn;
    private JButton PMWFileBtn;
    private JRadioButton manButton;
    // Button to relocate probe
    private JButton moveProbeButton;
    /** _more_ */
    private JComboBox<String> forecastTypeBox;
    private JFrame resultFrame;
    private JTextArea resultArea;
    private JFrame historyFrame;
    private JTextArea historyArea;
    private JLabel selectedHistoryFile;
    private JFileChooser historyFileSaveChooser;
    private JFrame overrideSceneFrame;
    private JLabel overrideSceneCurrentValueLabel;
    private JComboBox<String> overrideSceneTypeBox;
    private JLabel historyLabel;

    /**
     *
     */
    public ADTControl()  {
        super();
    }

    @Override public boolean init(DataChoice choice) throws VisADException,
            RemoteException {
        logger.info("ADTControl constructor begin...");

        if (!super.init(choice)) {
            return false;
        }
        this.choice = choice;

        probe = new PointProbe(new RealTuple(RealTupleType.SpatialEarth3DTuple,
                new double[] { 0.0, 0.0, 0.0 }));

        probe.setVisible(false);
        probe.setAutoSize(true);
        probe.addPropertyChangeListener(this);

        probe.setPointSize(getDisplayScale());
        addDisplayable(probe, FLAG_COLOR);

        // obtain initial ADT environmental parameters
        getADTenvParameters();

        // setup window contents in Controls Window
        setContents(setupMainWindow());

        // TJJ Jun 2017
        // We want to initialize probe to display center if in Manual mode
        NavigatedDisplay d = getNavigatedDisplay();
        if (manButton.isSelected()) {
            if (d != null) {
                EarthLocation el = d.getCenterPoint();
                logger.debug("Initializing probe location to: {}, {}", el.getLatitude(), el.getLongitude());
                probeLocation = el.getLatLonPoint();
                probe.setVisible(true);
            }
        }
        updateProbeLocation();
        return true;
    }

    private Container setupMainWindow() {

        /* add Lat/Lon position display text areas */
        latLonWidget = new LatLonWidget(GuiUtils.makeActionListener(this,
                "latLonWidgetChanged", null));
        moveProbeButton = new JButton("Move Probe");
        // TJJ add a strut and Probe button to the Lat-Lon widget panel
        latLonWidget.add(Box.createHorizontalStrut(6));
        latLonWidget.add(moveProbeButton);
        moveProbeButton.addActionListener(ae -> {
            // Validate the manual lat/lon text boxes
            String validLL = latLonWidget.isValidValues();
            if (validLL == null) {
                // User provided valid lat/lon data, see if it's within
                // our display bounds. If so, move the probe
                NavigatedDisplay d = getNavigatedDisplay();
                if (manButton.isSelected()) {
                    if (d != null) {
                        EarthLocationTuple elt = null;
                        try {
                            elt = new EarthLocationTuple(latLonWidget.getLat(), latLonWidget.getLon(), Double.NaN);
                            // Make sure the new Earth location is within the bounds of our satellite IR image
                            LatLonRect bounds = d.getLatLonRect();
                            logger.debug("Bounds min, max Lat: " + bounds.getLatMin() + ", " + bounds.getLatMax());
                            logger.debug("Bounds min, max Lon: " + bounds.getLonMin() + ", " + bounds.getLonMax());
                            logger.debug("ELT LatVal, LonVal: " + elt.getLatitude().getValue() + ", " + elt.getLongitude().getValue());
                            if (bounds.contains(elt.getLatitude().getValue(), elt.getLongitude().getValue())) {
                                probeLocation = elt.getLatLonPoint();
                                updateProbeLocation();
                            } else {
                                JOptionPane.showMessageDialog(null, "Location provided is outside image bounds");
                            }
                        } catch (VisADException | RemoteException ve) {
                            logException(ve);
                        }
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, validLL);
            }
        });

        /* add Manual or Automated storm centering buttons */

        manButton = new JRadioButton("Manual");
        manButton.setActionCommand("Manual");
        manButton.setSelected(true);
        manButton.setToolTipText(TOOLTIP_MANUAL);
        JRadioButton autoButton = new JRadioButton("Automated");
        autoButton.setActionCommand("Automated");
        autoButton.setSelected(false);
        autoButton.setToolTipText(TOOLTIP_AUTOMATIC);
        ButtonGroup automangroup = new ButtonGroup();
        automangroup.add(manButton);
        automangroup.add(autoButton);

        /* add forecast file file selector button and file type menu */
        JLabel autoStormSelectLabel = new JLabel("AUTOMATED STORM SELECTION");
        JLabel manualStormSelectLabel = new JLabel("MANUAL STORM SELECTION");
        JLabel forecastSelectLabel = new JLabel("Selected Forecast File: ");

        JLabel forecastLabel = new JLabel("No forecast file selected yet");

        manButton.addActionListener(ae -> {
            // enable the manual lat/lon text boxes
            latLonWidget.getLonField().setEnabled(true);
            latLonWidget.getLatField().setEnabled(true);
            autoStormSelectLabel.setEnabled(false);
            manualStormSelectLabel.setEnabled(true);
            forecastSelectLabel.setEnabled(false);
            moveProbeButton.setEnabled(true);
            forecastBtn.setEnabled(false);
            forecastTypeBox.setEnabled(false);
            GUIRunAutoTF = false;
        });

        autoButton.addActionListener(ae -> {
            // disable the manual lat/lon text boxes when in auto mode
            latLonWidget.getLonField().setEnabled(false);
            latLonWidget.getLatField().setEnabled(false);
            autoStormSelectLabel.setEnabled(true);
            manualStormSelectLabel.setEnabled(false);
            forecastSelectLabel.setEnabled(true);
            moveProbeButton.setEnabled(false);
            forecastBtn.setEnabled(true);
            forecastTypeBox.setEnabled(true);
            GUIRunAutoTF = true;
            System.out.println("running automated ADT!!!\n");
        });

        forecastBtn = new JButton("Select Forecast File");
        forecastBtn.setPreferredSize(new Dimension(200,30));
        forecastBtn.addActionListener(fbtn -> {
            GUIForecastFileName = selectForecastFile();
            logger.trace("forecast file name={}", GUIForecastFileName);
            forecastLabel.setText(
                    GUIForecastFileName.substring(GUIForecastFileName.lastIndexOf(File.separatorChar) + 1)
            );
        });

        forecastTypeBox = new JComboBox<>(FORECAST_TYPES);
        forecastTypeBox.setSelectedIndex(GUIForecastType);
        forecastTypeBox.setPreferredSize(new Dimension(150,20));
        forecastTypeBox.addActionListener(ame -> {
            GUIForecastType = forecastTypeBox.getSelectedIndex();
            logger.trace("forecast file type={}", GUIForecastType);
        });

        forecastTypeBox.setToolTipText("Select Forecast File Type.");
        autoStormSelectLabel.setEnabled(false);
        forecastSelectLabel.setEnabled(false);
        forecastBtn.setEnabled(false);
        forecastTypeBox.setEnabled(false);

        /* define default history file text field message */
        selectedHistoryFile = new JLabel("No history file selected yet");

        /* add history file selection button */
        JButton historyBtn = new JButton("Select History File");
        historyBtn.setToolTipText(TOOLTIP_HISTORY);
        historyBtn.setPreferredSize(new Dimension(200, 30));
        historyBtn.addActionListener(hbtn -> {
            GUIHistoryFileName = selectHistoryFile();
            logger.debug("history file name={}", GUIHistoryFileName);

            // TJJ Dec 2017
            // Do some cursory validation on History file before plowing ahead
            if (! validHistoryFile(GUIHistoryFileName)) {
                JOptionPane.showMessageDialog(null,
                        "Your selection does not appear to be a valid ADT History File.");
            } else {
                runFullADTAnalysis = true;
                selectedHistoryFile.setText(
                        GUIHistoryFileName.substring(GUIHistoryFileName.lastIndexOf(File.separatorChar) + 1)
                );
            }
        });

        /* add main ADT analysis start button */
        adtBtn = new JButton("Run ADT Analysis");
        adtBtn.setPreferredSize(new Dimension(250, 50));
        adtBtn.addActionListener(ae -> runADTmain());

        /* add history file list/write button */
        JButton listBtn = new JButton("List/Write History File");
        listBtn.setPreferredSize(new Dimension(250, 50));
        listBtn.addActionListener(ae -> {
            logger.debug("listing history file name={}", GUIHistoryFileName);
            try {
                listHistoryFile();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(null,
                        "Your selection does not appear to be a valid ADT History File.");
            }
        });

        // TJJ Jan 2017
        // We'll keep the Manual vs. Automated PMW radio button group around
        // in case code to support automated is added later. For now, only
        // manual works in this version, so we'll just set the state of the
        // buttons but not show them.

        JRadioButton PMWManButton = new JRadioButton("Manual");
        PMWManButton.setActionCommand("Man");
        PMWManButton.setSelected(true);
        PMWManButton.setEnabled(true);

        JRadioButton PMWAutoButton = new JRadioButton("Automated");
        PMWAutoButton.setActionCommand("Auto");
        PMWAutoButton.setSelected(false);
        PMWAutoButton.setEnabled(false);

        /* PMW Manual options */
        JLabel pmwManDateLabel = new JLabel("Date:");
        JLabel pmwManTimeLabel = new JLabel("Time:");
        JLabel pmwManScoreLabel = new JLabel("Score:");
        JTextField pmwManDateTextField = new JTextField("1900JAN01", 8);
        pmwManDateTextField.setToolTipText("YYYYMMMDD");
        pmwManDateTextField.addActionListener(ae -> {
            /* read PMW overpass date */
            JTextField src = (JTextField) ae.getSource();
            GUIMWJulianDate =
                    Functions.cmonth2julian(src.getText());
            GUIMWScore = -99.0;
        });
        JTextField pmwManTimeTextField = new JTextField("000000", 6);
        pmwManTimeTextField.setToolTipText("HHMMSS");
        pmwManTimeTextField.addActionListener(ae -> {
            /* read PMW overpass time */
            JTextField src = (JTextField) ae.getSource();
            GUIMWHHMMSSTime = Integer.valueOf(src.getText());
            GUIMWScore = -99.0;
        });
        JTextField pmwManScoreTextField = new JTextField("-99.0", 4);
        pmwManScoreTextField.setToolTipText("Eye Score Value");
        pmwManScoreTextField.addActionListener(ae -> {
            /* read PMW overpass score */
            JTextField src = (JTextField) ae.getSource();
            GUIMWScore = Double.valueOf(src.getText());
        });
        pmwManDateTextField.setEnabled(false);
        pmwManTimeTextField.setEnabled(false);
        pmwManScoreTextField.setEnabled(false);
        pmwManDateLabel.setEnabled(false);
        pmwManTimeLabel.setEnabled(false);
        pmwManScoreLabel.setEnabled(false);

        ButtonGroup pmwgroup = new ButtonGroup();
        pmwgroup.add(PMWAutoButton);
        pmwgroup.add(PMWManButton);
        PMWAutoButton.addActionListener(ae -> {
            /* enter file name */
            // Automated - file entry
            PMWFileBtn.setEnabled(true);
            pmwManDateTextField.setEnabled(false);
            pmwManTimeTextField.setEnabled(false);
            pmwManScoreTextField.setEnabled(false);
            pmwManDateLabel.setEnabled(false);
            pmwManTimeLabel.setEnabled(false);
            pmwManScoreLabel.setEnabled(false);
        });
        PMWManButton.addActionListener(ae -> {
            /* enter date/time and score manually */
            // Maunal entry
            PMWFileBtn.setEnabled(false);
            pmwManDateTextField.setEnabled(true);
            pmwManTimeTextField.setEnabled(true);
            pmwManScoreTextField.setEnabled(true);
            pmwManDateLabel.setEnabled(true);
            pmwManTimeLabel.setEnabled(true);
            pmwManScoreLabel.setEnabled(true);
        });

        /* Add PMW Analysis option buttons and entry fields */
        JCheckBox PMWActivateButton = new JCheckBox("Activate");
        PMWActivateButton.setActionCommand("PMW");
        PMWActivateButton.setSelected(false);
        PMWActivateButton.setEnabled(true);
        PMWActivateButton.setToolTipText(TOOLTIP_PMW);
        PMWActivateButton.addActionListener(ae -> {
            // if on, turn off and vice versa
            GUIPMWActivateTF = !GUIPMWActivateTF;
            PMWManButton.setEnabled(GUIPMWActivateTF);
            PMWManButton.setSelected(GUIPMWActivateTF);
            pmwManDateTextField.setEnabled(GUIPMWActivateTF);
            pmwManTimeTextField.setEnabled(GUIPMWActivateTF);
            pmwManScoreTextField.setEnabled(GUIPMWActivateTF);
            pmwManDateLabel.setEnabled(GUIPMWActivateTF);
            pmwManTimeLabel.setEnabled(GUIPMWActivateTF);
            pmwManScoreLabel.setEnabled(GUIPMWActivateTF);
            PMWActivateButton.setSelected(GUIPMWActivateTF);
        });

        /* add CKZ option buttons and entry fields */
        JLabel ckzPenvLabel = new JLabel("Penv:");
        ckzPenvLabel.setEnabled(false);

        JLabel ckz34radiusLabel = new JLabel("34kt Radius:");
        ckz34radiusLabel.setEnabled(false);

        ckzPenvTextField = new JTextField(DEFAULT_PENV, 5);
        ckzPenvTextField.setToolTipText(TOOLTIP_PENV);
        ckzPenvTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUICKZPenv = Integer.valueOf(src.getText());
        });
        ckz34radiusTextField = new JTextField(DEFAULT_RADIUS, 5);
        ckz34radiusTextField.setToolTipText(TOOLTIP_34KT);
        ckz34radiusTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUICKZGaleRadius = Integer.valueOf(src.getText());
        });
        ckzPenvTextField.setEnabled(false);
        ckz34radiusTextField.setEnabled(false);

        JRadioButton mslpDvorakButton = new JRadioButton("Dvorak");
        mslpDvorakButton.setActionCommand("Dvorak");
        mslpDvorakButton.setSelected(true);
        mslpDvorakButton.setToolTipText(TOOLTIP_MSLP_FROM_DVORAK);
        JRadioButton mslpCKZButton = new JRadioButton("CKZ");
        mslpCKZButton.setActionCommand("CKZ");
        mslpCKZButton.setSelected(false);
        mslpCKZButton.setToolTipText(TOOLTIP_MSLP_FROM_CKZ);
        ButtonGroup mslpgroup = new ButtonGroup();
        mslpgroup.add(mslpDvorakButton);
        mslpgroup.add(mslpCKZButton);
        mslpDvorakButton.addActionListener(ae -> {
            // Dvorak
            ckzPenvTextField.setEnabled(false);
            ckz34radiusTextField.setEnabled(false);
            ckzPenvLabel.setEnabled(false);
            ckz34radiusLabel.setEnabled(false);
            mslpDvorakButton.setSelected(true);
            mslpCKZButton.setSelected(false);
            GUIUseCKZTF = false;
        });
        mslpCKZButton.addActionListener(ae -> {
            // CKZ
            ckzPenvTextField.setEnabled(true);
            ckz34radiusTextField.setEnabled(true);
            ckzPenvLabel.setEnabled(true);
            ckz34radiusLabel.setEnabled(true);
            mslpDvorakButton.setSelected(false);
            mslpCKZButton.setSelected(true);
            GUIUseCKZTF = true;
        });

        /* various other keyword options */
        /* Initial classification entry -- RAWT */
        JLabel RawTLabel = new JLabel("Raw T:");
        JTextField RawTTextField = new JTextField("1.0", 4);
        RawTTextField.setToolTipText(TOOLTIP_RAW_T);
        RawTTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUIRawTValue = Double.valueOf(src.getText());
            GUIInitStrengthTF = GUIRawTValue >= 1.0;
        });

        /* Radius of Max Wind entry -- RMW */
        JLabel RMWLabel = new JLabel("RMW:");
        JTextField RMWTextField = new JTextField("-99", 4);
        RMWTextField.setToolTipText(TOOLTIP_RMW);
        RMWTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUIRMWSize = Double.valueOf(src.getText());
        });

        /* Override option */
        JButton sceneOverrideButton = new JButton("Override Scene Type");
        JLabel OverrideLabel = new JLabel(SCENE_TYPE_PREFIX + SCENE_TYPES[Env.OverrideSceneTypeIndex]);
        sceneOverrideButton.addActionListener(ae -> {
            overrideSceneFrame.setVisible(true);
        });

        /* ATCF Analysis Output Checkbox */

        JLabel ATCFOutputLabel = new JLabel("ATCF Output:");
        JCheckBox ATCFOutputButton = new JCheckBox("Activate");
        ATCFOutputButton.setActionCommand("ATCF");
        ATCFOutputButton.setSelected(false);
        ATCFOutputButton.setEnabled(true);

        JLabel ATCFEntryStormLabel = new JLabel("Storm ID:");
        ATCFEntryStormTextField = new JTextField("XXX", 8);
        ATCFEntryStormTextField.setToolTipText(TOOLTIP_STORM_ID);
        JLabel ATCFEntrySiteLabel = new JLabel("Site ID:");
        ATCFEntrySiteTextField = new JTextField("XXXX", 8);
        ATCFEntrySiteTextField.setToolTipText(TOOLTIP_SITE_ID);
        ATCFEntryStormLabel.setEnabled(false);
        ATCFEntryStormTextField.setEnabled(false);
        ATCFEntrySiteLabel.setEnabled(false);
        ATCFEntrySiteTextField.setEnabled(false);
        ATCFEntryStormTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUIATCFStormID = src.getText();
        });
        ATCFEntrySiteTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUIATCFSiteID = src.getText();
        });

        ATCFOutputButton.addActionListener(ae -> {
            // if on, turn off and vice versa
            GUIATCFRecordOutputTF = !GUIATCFRecordOutputTF;
            ATCFEntryStormLabel.setEnabled(GUIATCFRecordOutputTF);
            ATCFEntryStormTextField.setEnabled(GUIATCFRecordOutputTF);
            ATCFEntrySiteLabel.setEnabled(GUIATCFRecordOutputTF);
            ATCFEntrySiteTextField.setEnabled(GUIATCFRecordOutputTF);
            ATCFOutputButton.setSelected(GUIATCFRecordOutputTF);
        });

        /* Land Flag button -- LAND */
        JLabel LandFlagLabel = new JLabel("Land Flag:");
        JRadioButton LandONButton = new JRadioButton("ON");
        LandONButton.setActionCommand("On");
        LandONButton.setSelected(true);
        LandONButton.setToolTipText(TOOLTIP_LAND_FLAG_ON);
        JRadioButton LandOFFButton = new JRadioButton("OFF");
        LandOFFButton.setActionCommand("Off");
        LandOFFButton.setSelected(false);
        LandOFFButton.setToolTipText(TOOLTIP_LAND_FLAG_OFF);
        ButtonGroup landgroup = new ButtonGroup();
        landgroup.add(LandONButton);
        landgroup.add(LandOFFButton);
        LandONButton.addActionListener(ae -> {
            // LAND=YES
            LandONButton.setSelected(true);
            LandOFFButton.setSelected(false);
            GUILandFlagTF = true;
        });
        LandOFFButton.addActionListener(ae -> {
            // LAND=NO
            LandONButton.setSelected(false);
            LandOFFButton.setSelected(true);
            GUILandFlagTF = false;
        });

        /*  Wind Speed Vmax output button -- VOUT */
        JLabel VOutLabel = new JLabel("VMax:");
        JRadioButton V1MinButton = new JRadioButton("One-minute");
        V1MinButton.setActionCommand("One");
        V1MinButton.setSelected(true);
        V1MinButton.setToolTipText("Maximum Wind Speed Averaged Over");
        JRadioButton V10MinButton = new JRadioButton("Ten-minute");
        V10MinButton.setActionCommand("Ten");
        V10MinButton.setSelected(false);
        V10MinButton.setToolTipText("Maximum Wind Speed Averaged Over");
        ButtonGroup voutgroup = new ButtonGroup();
        voutgroup.add(V1MinButton);
        voutgroup.add(V10MinButton);
        V1MinButton.addActionListener(ae -> {
            // 1-minute winds
            V1MinButton.setSelected(true);
            V10MinButton.setSelected(false);
            GUIVmax1or10TF = true;
        });
        V10MinButton.addActionListener(ae -> {
            // 10-minute winds
            V1MinButton.setSelected(false);
            V10MinButton.setSelected(true);
            GUIVmax1or10TF = false;
        });

        JLabel blankfield = new JLabel("");

        // TJJ Jan 2018 - interim link to Help for McV 1.7 release
        JButton helpLinkLabel = new JButton("<html><a href=\"http://www.ssec.wisc.edu\">Help</a></html>");
        helpLinkLabel.setToolTipText("Opens ADT Help PDF in your system web browser");
        helpLinkLabel.addActionListener(e -> {
            WebBrowser.browse("http://www.ssec.wisc.edu/mcidas/software/v/resources/adt/McV_ADT_1p7.pdf");
        });

        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent widgets =
                GuiUtils.formLayout(
                        arr(left(hbox(arr(new JLabel("Storm Center Selection:"), manButton, autoButton), 5)),
                                filler(),
                                left(hbox(arr(manualStormSelectLabel), 10)),
                                filler(),
                                left(hbox(arr(filler(30, 1), latLonWidget))), filler(),
                                left(hbox(arr(autoStormSelectLabel), 10)), filler(),
                                left(hbox(arr(filler(30, 1), forecastBtn, forecastTypeBox,
                                        forecastSelectLabel, forecastLabel), 5)), filler(),
                                left(hbox(arr(blankfield))),
                                filler(1, 5),
                                left(hbox(arr(new JLabel("HISTORY FILE INFORMATION")), 10)), filler(),
                                left(hbox(arr(filler(30, 1), historyBtn, new JLabel
                                        ("Selected History File: "), selectedHistoryFile), 5)),
                                filler(),
                                left(hbox(arr(blankfield))),
                                filler(1, 5),
                                left(hbox(arr(new JLabel("PMW ANALYSIS")), 10)), filler(),
                                left(hbox(arr(filler(30, 1), PMWActivateButton,
                                        pmwManDateLabel, pmwManDateTextField, pmwManTimeLabel,
                                        pmwManTimeTextField, pmwManScoreLabel, pmwManScoreTextField), 5)), filler(),
                                left(hbox(arr(blankfield))),
                                filler(1, 5),
                                left(hbox(arr(new JLabel("MISCELLANEOUS OPTIONS")), 10)), filler(),
                                left(hbox(arr(filler(30, 1), new JLabel("MSLP Conversion Method:"), mslpDvorakButton, mslpCKZButton, ckzPenvLabel, ckzPenvTextField, ckz34radiusLabel, ckz34radiusTextField), 5)), filler(),
                                left(hbox(arr(filler(30, 1), sceneOverrideButton, OverrideLabel), 5)), filler(),
                                left(hbox(arr(filler(30, 1), LandFlagLabel, LandONButton, LandOFFButton, filler(20, 1), VOutLabel, V1MinButton, V10MinButton, filler(20, 1), RawTLabel, RawTTextField, RMWLabel, RMWTextField), 5)), filler(),
                                left(hbox(arr(filler(30, 1), ATCFOutputLabel, ATCFOutputButton, ATCFEntryStormLabel, ATCFEntryStormTextField, ATCFEntrySiteLabel, ATCFEntrySiteTextField), 5)), filler(),
                                left(hbox(arr(filler(80, 1), adtBtn, listBtn, helpLinkLabel), 20)), filler()));

        JPanel controls = topLeft(widgets);

        /* set up ADT Bulletin display area */
        resultArea = new JTextArea();
        resultArea.setEditable(false);

        Font c = new Font("Courier", Font.BOLD, 12);

        resultFrame = new JFrame("ADT Results");
        resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JScrollPane resultScroller = new JScrollPane(resultArea);
        resultScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        resultFrame.add(resultScroller, BorderLayout.CENTER);
        resultFrame.setPreferredSize(new Dimension(400, 600));
        resultFrame.setFont(c);

        /* set up ADT History File display area */
        historyFrame = new JFrame("ADT History File Listing");
        Container historyContainer = historyFrame.getContentPane();
        historyFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel historyTextPanel = new JPanel();
        FlowLayout historyListLayout = new FlowLayout();
        historyTextPanel.setLayout(historyListLayout);
        historyListLayout.setAlignment(FlowLayout.CENTER);

        historyArea = new JTextArea(50,150);
        historyArea.setEditable(false);
        JScrollPane historyScroller = new JScrollPane(historyArea);
        historyScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        historyScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        historyScroller.setPreferredSize(new Dimension(1200, 400));
        historyArea.setFont(c);

        JPanel historyLabelPanel = new JPanel();
        FlowLayout HistoryLabelLayout = new FlowLayout();
        historyLabelPanel.setLayout(HistoryLabelLayout);
        HistoryLabelLayout.setAlignment(FlowLayout.CENTER);
        historyLabel = new JLabel("No History File Selected");
        historyLabel.setPreferredSize(new Dimension(800, 20));
        historyLabel.setFont(c);

        /* history file Editing Date Selection window */
        JFrame historyDateFrame = new JFrame("History File Editor");
        Container historyDateContainer = historyDateFrame.getContentPane();
        historyDateFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel historyDatePanel = new JPanel();
        FlowLayout DateStartEndLayout = new FlowLayout();
        historyDatePanel.setLayout(DateStartEndLayout);
        DateStartEndLayout.setAlignment(FlowLayout.CENTER);
        JLabel historyDateStartLabel = new JLabel("Start:");
        JLabel historyDateStartDateLabel = new JLabel("Date");
        JTextField historyDateStartDateTextField = new JTextField("0000XXX00", 10);
        JLabel historyDateStartTimeLabel = new JLabel("Time");
        JTextField historyDateStartTimeTextField = new JTextField("-1", 8);
        JLabel historyDateEndLabel = new JLabel("End");
        JLabel historyDateEndDateLabel = new JLabel("Date");
        JTextField historyDateEndDateTextField = new JTextField("0000XXX00", 10);
        JLabel historyDateEndTimeLabel = new JLabel("Time");
        JTextField historyDateEndTimeTextField = new JTextField("-1", 8);

        JPanel historyButtonPanel = new JPanel();
        FlowLayout HistoryButtonLayout = new FlowLayout();
        historyButtonPanel.setLayout(HistoryButtonLayout);
        HistoryButtonLayout.setAlignment(FlowLayout.CENTER);

        JButton historySaveListingBtn = new JButton("Write History");
        historySaveListingBtn.setPreferredSize(new Dimension(200, 20));
        historySaveListingBtn.addActionListener(ae -> {
            GUIHistoryFileListingName = selectHistoryFileOutput();
            logger.debug("saving history listing file name={}", GUIHistoryFileListingName);
            GUIHistoryListFormat = -1;
        });
        JButton historyWriteATCFBtn = new JButton("Write ATCF");
        historyWriteATCFBtn.setPreferredSize(new Dimension(200, 20));
        historyWriteATCFBtn.addActionListener(ae -> {
            GUIATCFOutputTF = true;
            GUIHistoryListFormat = 0;
            logger.debug("calling ATCFFileOutput");
            ATCFFileOutput(0);
        });
        historyLabelPanel.add(historyLabel);
        historyLabelPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        historyTextPanel.add(historyScroller);
        historyTextPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

        historyButtonPanel.add(historySaveListingBtn);
        historyButtonPanel.add(historyWriteATCFBtn);
        historyButtonPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        historyContainer.add(historyLabelPanel,BorderLayout.NORTH);
        historyContainer.add(historyTextPanel,BorderLayout.CENTER);
        historyContainer.add(historyButtonPanel,BorderLayout.SOUTH);

        historyDateStartDateTextField.addActionListener(ae -> {
            JTextField textField = (JTextField)ae.getSource();
            GUIStartDate = Functions.cmonth2julian(textField.getText());
        });
        historyDateStartTimeTextField.addActionListener(ae -> {
            JTextField textField = (JTextField)ae.getSource();
            GUIStartTime = Integer.valueOf(textField.getText());
        });
        historyDateEndDateTextField.addActionListener(ae -> {
            JTextField textField = (JTextField)ae.getSource();
            GUIEndDate = Functions.cmonth2julian(textField.getText());
        });
        historyDateEndTimeTextField.addActionListener(ae -> {
            JTextField textField = (JTextField)ae.getSource();
            GUIEndTime = Integer.valueOf(textField.getText());
        });

        JPanel historyDateButtonPanel = new JPanel();
        FlowLayout DateButtonLayout = new FlowLayout();
        historyDateButtonPanel.setLayout(DateButtonLayout);
        DateButtonLayout.setAlignment(FlowLayout.CENTER);
        JRadioButton historyEditDeleteButton = new JRadioButton("Delete Records");
        historyEditDeleteButton.setActionCommand("Delete");
        historyEditDeleteButton.setSelected(false);
        JRadioButton historyEditAddCommentButton = new JRadioButton("Add Comment");
        historyEditAddCommentButton.setActionCommand("Comment");
        historyEditAddCommentButton.setSelected(false);
        ButtonGroup editgroup = new ButtonGroup();
        editgroup.add(historyEditDeleteButton);
        editgroup.add(historyEditAddCommentButton);
        JLabel historyEditAddCommentLabel = new JLabel("Comment:");
        JTextField historyEditAddCommentTextField = new JTextField("no comment entered", 25);
        historyEditAddCommentTextField.setEnabled(false);

        historyEditDeleteButton.addActionListener(ae -> {
            // history Edit - Delete
            historyEditDeleteButton.setSelected(true);
            historyEditAddCommentButton.setSelected(false);
            historyEditAddCommentLabel.setEnabled(false);
            historyEditAddCommentTextField.setEnabled(false);
            GUICommentAddTF = false;
            GUIDeleteTF = true;
        });

        historyEditAddCommentButton.addActionListener(ae -> {
            // history Edit - Add Comment
            historyEditDeleteButton.setSelected(false);
            historyEditAddCommentButton.setSelected(true);
            historyEditAddCommentLabel.setEnabled(true);
            historyEditAddCommentTextField.setEnabled(true);
            GUICommentAddTF = true;
            GUIDeleteTF = false;
        });
        historyEditAddCommentTextField.addActionListener(ae -> {
            JTextField src = (JTextField)ae.getSource();
            GUICommentString = src.getText();
        });
        JPanel historyEditInputPanel = new JPanel();
        FlowLayout EditInputButtonLayout = new FlowLayout();
        historyEditInputPanel.setLayout(EditInputButtonLayout);
        EditInputButtonLayout.setAlignment(FlowLayout.CENTER);
        JButton historyEditApplyButton = new JButton("Apply Edits");
        historyEditApplyButton.setPreferredSize(new Dimension(150, 20));
        historyEditApplyButton.addActionListener(ae -> modifyHistoryFile());
        JButton historyEditCancelButton = new JButton("Cancel");
        historyEditCancelButton.setPreferredSize(new Dimension(150, 20));
        historyEditCancelButton.addActionListener(ae -> historyDateFrame.dispose());
        historyDatePanel.add(historyDateStartLabel);
        historyDatePanel.add(historyDateStartDateLabel);
        historyDatePanel.add(historyDateStartDateTextField);
        historyDatePanel.add(historyDateStartTimeLabel);
        historyDatePanel.add(historyDateStartTimeTextField);
        historyDatePanel.add(historyDateEndLabel);
        historyDatePanel.add(historyDateEndDateLabel);
        historyDatePanel.add(historyDateEndDateTextField);
        historyDatePanel.add(historyDateEndTimeLabel);
        historyDatePanel.add(historyDateEndTimeTextField);
        historyDatePanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        historyDateButtonPanel.add(historyEditDeleteButton);
        historyDateButtonPanel.add(historyEditAddCommentButton);
        historyDateButtonPanel.add(historyEditAddCommentLabel);
        historyDateButtonPanel.add(historyEditAddCommentTextField);
        historyEditInputPanel.add(historyEditApplyButton);
        historyEditInputPanel.add(historyEditCancelButton);
        historyDateContainer.add(historyDatePanel, BorderLayout.NORTH);
        historyDateContainer.add(historyDateButtonPanel, BorderLayout.CENTER);
        historyDateContainer.add(historyEditInputPanel, BorderLayout.SOUTH);

        /* set up Scene Type Override Window display window */
        overrideSceneFrame = new JFrame("Override Scene Type");
        overrideSceneFrame.setSize(new Dimension(400, 300));
        Container overrideSceneContainer = overrideSceneFrame.getContentPane();
        overrideSceneFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel overrideSceneCurrentPanel = new JPanel();
        FlowLayout OverrideSceneCurrentLayout = new FlowLayout();
        overrideSceneCurrentPanel.setLayout(OverrideSceneCurrentLayout);
        OverrideSceneCurrentLayout.setAlignment(FlowLayout.CENTER);
        JLabel overrideSceneCurrentLabel = new JLabel("Current Scene Type:");
        overrideSceneCurrentValueLabel = new JLabel(SCENE_TYPES[Env.OverrideSceneTypeIndex]);
        JPanel overrideSceneSelectPanel = new JPanel();
        FlowLayout OverrideSceneSelectLayout = new FlowLayout();
        overrideSceneCurrentPanel.setLayout(OverrideSceneSelectLayout);
        OverrideSceneSelectLayout.setAlignment(FlowLayout.CENTER);
        JLabel overrideSceneSelectLabel = new JLabel("Select New Scene Type:");
        overrideSceneTypeBox = new JComboBox<>(SCENE_TYPES);
        overrideSceneTypeBox.setSelectedIndex(Env.OverrideSceneTypeIndex);
        overrideSceneTypeBox.setPreferredSize(new Dimension(150, 20));
        // overrideSceneTypeBox.addActionListener(ame -> Env.OverrideSceneTypeIndex = overrideSceneTypeBox.getSelectedIndex());
        JPanel overrideSceneButtonPanel = new JPanel();
        FlowLayout OverrideSceneButtonLayout = new FlowLayout();
        overrideSceneButtonPanel.setLayout(OverrideSceneButtonLayout);
        OverrideSceneButtonLayout.setAlignment(FlowLayout.CENTER);
        JButton overrideSceneAcceptButton = new JButton("Accept New Scene");
        overrideSceneAcceptButton.setPreferredSize(new Dimension(190, 20));
        overrideSceneAcceptButton.addActionListener(ae -> {
            // accept new scene selection
            overrideSceneFrame.setVisible(false);
            Env.OverrideSceneTypeIndex = overrideSceneTypeBox.getSelectedIndex();
            OverrideLabel.setText(SCENE_TYPE_PREFIX + SCENE_TYPES[Env.OverrideSceneTypeIndex]);
            overrideSceneCurrentValueLabel.setText(SCENE_TYPES[Env.OverrideSceneTypeIndex]);
            // runADTmain();
        });
        JButton overrideSceneCancelButton = new JButton("Keep Current Scene");
        overrideSceneCancelButton.setPreferredSize(new Dimension(190, 20));
        overrideSceneCancelButton.addActionListener(ae -> {
            overrideSceneFrame.setVisible(false);
            // runADTmain();
        });
        overrideSceneCurrentPanel.add(overrideSceneCurrentLabel);
        overrideSceneCurrentPanel.add(overrideSceneCurrentValueLabel);
        overrideSceneSelectPanel.add(overrideSceneSelectLabel);
        overrideSceneSelectPanel.add(overrideSceneTypeBox);
        overrideSceneButtonPanel.add(overrideSceneAcceptButton);
        overrideSceneButtonPanel.add(overrideSceneCancelButton);
        overrideSceneContainer.add(overrideSceneCurrentPanel, BorderLayout.NORTH);
        overrideSceneContainer.add(overrideSceneSelectPanel, BorderLayout.CENTER);
        overrideSceneContainer.add(overrideSceneButtonPanel, BorderLayout.SOUTH);

        JScrollPane scrollPane = new JScrollPane(controls);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        return scrollPane;
    }

    /**
     * Do some cursory checking on validity of selected History file
     * @param historyFileName
     * @return true is seems ok
     */

    private boolean validHistoryFile(String historyFileName) {
        boolean seemsOk = true;

        History CurrentHistory = new History();

        try {
            logger.debug("trying to read history file {}", historyFileName);
            CurrentHistory.ReadHistoryFile(historyFileName);
        } catch (IOException exception) {
            logger.warn("History file %s is not valid", historyFileName);
            seemsOk = false;
        }

        logger.debug("Number of history records: {}", History.HistoryNumberOfRecords());
        if (History.HistoryNumberOfRecords() == 0) seemsOk = false;
        return seemsOk;
    }

    private void runADTmain() {
        if (!running) {
            running = true;
            adtBtn.setEnabled(false);
            adtBtn.setText("Running");
            Misc.run(() -> {
                runADT();
                ExitADT();
            });
        }
    }

    private void runADT() {
        Main StormADT = new Main();
        String ADTRunOutput;
        String ErrorMessage;

        if (GUIFileOverrideTF) {
            String GUIOverrideFilePath = System.getenv("ODTHOME");
            if (GUIOverrideFilePath == null) {
                GUIOverrideFilePath = System.getenv("HOME");
            }
            String GUIOverrideFile = GUIOverrideFilePath + "/runadt.nogui.inputs.txt";
            /* GUIFileOverrideCheckBoxToggle();  change toggle back to OFF */
            int RetVal = ReadGUIOverrideInputFile(GUIOverrideFile);
            if (RetVal == -1) {
                ErrorMessage = String.format("Error reading GUI override file %s\n",GUIOverrideFile);
                System.out.println(ErrorMessage);
                userMessage(ErrorMessage);
                ExitADT();
                return;
            }
        }

        loadADTenvParameters();

        boolean RunAuto = Env.AutoTF;

        // In auto mode, make sure a valid forecast file was selected
        if (RunAuto) {
            if (GUIForecastFileName == null) {
                userMessage("A valid forecast file must be selected to use Automated mode.");
                ExitADT();
                return;
            }
        }

        /* set storm position either through automated storm selection or by manual choice */
        GetImageDateTime();
        int ReturnVal = StormADT.GetInitialPosition();  // should set up to throw exception instead of return value
        if (ReturnVal < 0) {
            ErrorMessage = "Error obtaining initial position... exiting ADT\n";
            System.out.println(ErrorMessage);
            userMessage(ErrorMessage);
            ExitADT();
        } else {
            if (RunAuto) {
                try {
                    float CenterLatitude = (float)Env.SelectedLatitude;
                    float CenterLongitude =  (float)Env.SelectedLongitude;
                    /* System.out.println("pre-ARCHER latitude=%f longitude=%f\n",CenterLatitude,CenterLongitude); */
                    GetImageData(CenterLatitude, CenterLongitude);
                } catch (Exception exception) {
                    ErrorMessage = "Error reading IR data pre-ARCHER\n";
                    System.out.println(ErrorMessage);
                    userMessage(ErrorMessage);
                    ExitADT();
                    return;
                }
                StormADT.GetARCHERPosition();
            } else {
                if (probeLocation == null) {
                    ErrorMessage = "Please select storm center location manually and try again";
                    System.out.println(ErrorMessage);
                    userMessage(ErrorMessage);
                    ExitADT();
                    return;
                } else {
                    Env.SelectedLatitude = probeLocation.getLatitude().getValue();
                    Env.SelectedLongitude = probeLocation.getLongitude().getValue();
                }
            }

            try {
                float CenterLatitude = (float) Env.SelectedLatitude;
                float CenterLongitude =  (float) Env.SelectedLongitude;
                /* System.out.println("latitude=%f longitude=%f domain=%d\n",CenterLatitude,CenterLongitude,DomainID); */
                GetImageData(CenterLatitude, CenterLongitude);
            } catch (Exception e) {
                ErrorMessage = "Error reading IR data in getimagedata()\n";
                logger.error(ErrorMessage.trim(), e);
                userMessage(ErrorMessage);
                ExitADT();
                return;
            }

            // TJJ Jun 2017 Just about ready, a few more validation checks and we can run
            // If CKZ chosen as MSLP Conversion Method, need to validate Penv and 34kt Radius fields
            // This may not be the best place to do this, but it's better than not doing it ;-)

            if (GUIUseCKZTF) {

                String newPenvStr = ckzPenvTextField.getText();
                boolean badPenv = false;
                try {
                    int newPenv = Integer.valueOf(newPenvStr);
                    if (newPenv > 0) {
                        GUICKZPenv = newPenv;
                        Env.CKZPenv = GUICKZPenv;
                    } else {
                        badPenv = true;
                    }
                } catch (NumberFormatException nfe) {
                    badPenv = true;
                }

                if (badPenv) {
                    // Throw up a warning and bail out
                    showBadIntWarning("Penv", newPenvStr);
                    return;
                }

                String newRadiusStr = ckz34radiusTextField.getText();
                boolean badNewRadius = false;
                try {
                    int newRadius = Integer.valueOf(newRadiusStr);
                    if (newRadius > 0) {
                        GUICKZGaleRadius = newRadius;
                        Env.CKZGaleRadius = GUICKZGaleRadius;
                    } else {
                        badNewRadius = true;
                    }
                } catch (NumberFormatException nfe) {
                    badNewRadius = true;
                }

                if (badNewRadius) {
                    // Throw up a warning and bail out
                    showBadIntWarning("Radius", newRadiusStr);
                    return;
                }

            }

            try {
                logger.debug("RUNNING ADT ANALYSIS");
                ADTRunOutput = StormADT.RunADTAnalysis(runFullADTAnalysis,GUIHistoryFileName);
            } catch (IOException exception) {
                ErrorMessage = "Error with call to StormADT.RunADT()\n";
                logger.error(ErrorMessage.trim(), exception);
                userMessage(ErrorMessage);
                ExitADT();
                return;
            }
            if (GUIOverrideSceneTF) {
                /* System.out.println("Overriding scene type!!!  Scene value=%d\n",InitialSceneTypeValue); */
                overrideSceneCurrentValueLabel.setText(SCENE_TYPES[Env.OverrideSceneTypeIndex]);
                overrideSceneFrame.pack();
                overrideSceneFrame.setVisible(true);
                ExitADT();
            } else {
                logger.debug("done running ADT");

                resultArea.setText(ADTRunOutput);
                resultFrame.pack();
                resultFrame.setVisible(true);

                // TJJ Dec 2017
                // This is in reference to Request #11, Bug #17 from
                // http://mcidas.ssec.wisc.edu/inquiry-v/?inquiry=1187
                // Since the intent here is to modify the currently active history file by appending
                // one record, and since that record insert had been previously commented out below,
                // we'll assume this was never working properly in the first place.  To prevent the
                // current History File from being clobbered, we just won't do the re-write for now,
                // since as is, a deep Exception zeros out the file, and the original file should
                // at the very least remain unmodified.

//                if (GUIHistoryFileName != null) {
//                    try {
//                        // int[] InsertRecs = History.InsertHistoryRecord(runFullADTAnalysis,GUIHistoryFileName);
//                        /* System.out.println("*** Modified=%d InsertOverwriteFlag=%d***\n",InsertRecs[0],InsertRecs[1]); */
//                        int NumRecs = History.WriteHistoryFile(GUIHistoryFileName);
//                        ErrorMessage = String.format("Number of records written to history file: %d\n", NumRecs);
//                    } catch (IOException exception) {
//                        ErrorMessage = String.format("Error writing history file %s\n", GUIHistoryFileName);
//                    } catch (Exception e) {
//                        logger.error("Exception: ", e);
//                        ErrorMessage = String.format("Error writing history file %s\n", GUIHistoryFileName);
//                    }
//                    logger.warn(ErrorMessage.trim());
//                    userMessage(ErrorMessage);
//                }

                if (GUIATCFRecordOutputTF) {
                    ATCFFileOutput(-1);
                }

                ExitADT();
            }
        }
    }

    /**
     * Show a warning about a certain parameter needing to be greater than zero.
     *
     * @param type Parameter name. Cannot be {@code null}.
     * @param badValue Erroneous value. Cannot be {@code null}.
     */
    private void showBadIntWarning(String type, String badValue) {
        String msg = "Invalid %s value: %s\nPlease provide a positive integer.";
        JOptionPane.showMessageDialog(null,
                String.format(msg, type, badValue));
        ExitADT();
    }

    private void ExitADT() {
        running = false;
        adtBtn.setEnabled(true);
        adtBtn.setText("Run Analysis");
    }

    /*
     * Override for additional local cleanup
     * (non-Javadoc)
     * @see ucar.unidata.idv.control.DisplayControlImpl#doRemove()
     */

    @Override public void doRemove() throws RemoteException, VisADException {
        super.doRemove();
        if (resultFrame != null) {
            resultFrame.dispose();
        }
        if (historyFrame != null) {
            historyFrame.dispose();
        }
    }

    private void listHistoryFile() {
        HistoryListOutput = null;

        History CurrentHistory = new History();

        // Make sure a valid History File has been selected. At startup, value will be null
        if (GUIHistoryFileName == null) {
            JOptionPane.showMessageDialog(null,
                    "Please first select a valid ADT History File.");
            return;
        }

        try {
            logger.debug("trying to read history file {}", GUIHistoryFileName);
            CurrentHistory.ReadHistoryFile(GUIHistoryFileName);
        } catch (IOException exception) {
            String ErrorMessage = String.format("History file %s is not found",GUIHistoryFileName);
            logger.warn(ErrorMessage);
            userMessage(ErrorMessage);
            return;
        }

        logger.debug("Number of history records: {}", History.HistoryNumberOfRecords());

        HistoryListOutput = History.ListHistory(0, -1, "CIMS", "99X");
        historyLabel.setText(GUIHistoryFileName);
        historyArea.setText(HistoryListOutput);
        historyFrame.pack();
        historyFrame.setVisible(true);

    }

    private void modifyHistoryFile() {

        if (GUIDeleteTF) {
            // delete records
            int DeleteReturn[] = History.DeleteHistoryRecords(runFullADTAnalysis,GUIHistoryFileName);
            logger.debug("deleted {} records... modified {} records", DeleteReturn[1],DeleteReturn[0]);
        } else if( GUICommentAddTF) {
            //
            int CommentAddReturn = History.CommentHistoryRecords(GUICommentString);
            logger.debug("added comment to {} records",CommentAddReturn);
        } else {
            // invalid selection
            logger.warn("entered invalid selection!");
        }

        try {
            int HistoryFileRecords = History.WriteHistoryFile(GUIHistoryFileName);
            if (HistoryFileRecords >= 0) {
                logger.debug("wrote {} records to '{}'", HistoryFileRecords, GUIHistoryFileName);
            }
        } catch (IOException exception) {
            String ErrorMessage = String.format("error updating history file %s",GUIHistoryFileName);
            System.out.println(ErrorMessage);
            userMessage(ErrorMessage);
        }
    }

    private String selectHistoryFile() {

        String fileNameReturn = null;

        JFrame historyFileFrame = new JFrame();
        JFileChooser historyFileChooser = new JFileChooser();
        String historyPath = System.getenv("ODTHISTORY");
        if (historyPath == null) {
            historyPath = getLastPath("mcv.adt.lasthistorypath", System.getProperty("user.home"));
        }
        historyFileChooser.setCurrentDirectory(new File(historyPath));
        historyFileChooser.setDialogTitle("Select ADT History File");
        int returnVal = historyFileChooser.showOpenDialog(historyFileFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = historyFileChooser.getSelectedFile();
            fileNameReturn = file.getAbsolutePath();
            setLastPath("mcv.adt.lasthistorypath", file.getPath());
        }

        return fileNameReturn;
    }

    /**
     * Returns the path that corresponds to the given McIDAS-V property ID.
     *
     * @param id ID used to store user's last selected path.
     * @param defaultPath Path to use if {@code id} has not been set.
     *
     * @return Either the {@code String} representation of the last selected
     * path, or {@code defaultPath}.
     */
    private String getLastPath(String id, String defaultPath) {
        IntegratedDataViewer mcv = getIdv();
        String path = defaultPath;
        if (mcv != null) {
            path = mcv.getObjectStore().get(id, defaultPath);
        }
        return path;
    }

    /**
     * Sets the value of the given McIDAS-V property ID to the specified path.
     *
     * @param id ID to store.
     * @param path Path to associate with {@code id}.
     */
    private void setLastPath(String id, String path) {
        String okayPath = (path != null) ? path : "";
        IntegratedDataViewer mcv = getIdv();
        if (mcv != null) {
            XmlObjectStore store = mcv.getObjectStore();
            store.put(id, okayPath);
            store.saveIfNeeded();
        }
    }

    /**
     * Write a new ADT History File
     * @return true if ok
     */

    private String selectHistoryFileOutput() {

        File saveFile = null;
        String ErrorMessage;

        historyFileSaveChooser = new JFileChooser();
        historyFileSaveChooser.setCurrentDirectory(null);
        historyFileSaveChooser.setDialogTitle("Save ADT History File");
        int returnVal = historyFileSaveChooser.showSaveDialog(historyFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            saveFile = historyFileSaveChooser.getSelectedFile();
            try (FileWriter outFile = new FileWriter(saveFile)) {
                outFile.write(HistoryListOutput);
                outFile.flush();
                outFile.close();
                ErrorMessage = String.format("success writing history file output file %s\n",saveFile.toString());
            } catch (IOException ex) {
                logger.error("problem writing to history file output", ex);
                ErrorMessage = String.format("error writing history file output file %s\n",saveFile.toString());
            }
            System.out.println(ErrorMessage);
            userMessage(ErrorMessage);
        }

        String saveFilePath = null;
        if (saveFile != null) {
            saveFilePath = saveFile.getAbsolutePath();
        }
        return saveFilePath;

    }

    /**
     * Write out the ATCF file
     * @param outputstyle
     * @return true if written ok
     */

    private boolean ATCFFileOutput(int outputstyle) {
        File saveFile = null;
        String ATCFOutputFileName;
        String ATCFOutputFilePath;
        String ATCFFileOutput;
        String ATCFMessage;
        boolean writefileTF = false;
        boolean returnStatus = true;

        if (outputstyle == 0) {
            // output entire history file in ATCF
            historyFileSaveChooser = new JFileChooser();
            historyFileSaveChooser.setCurrentDirectory(null);
            historyFileSaveChooser.setDialogTitle("Write ATCF File");
            int returnVal = historyFileSaveChooser.showSaveDialog(historyFrame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                saveFile = historyFileSaveChooser.getSelectedFile();
                writefileTF = true;
            } else if (returnVal == JFileChooser.CANCEL_OPTION) {
                // User has pressed cancel button
                writefileTF = false;
            }
            logger.debug("saving ATCF history listing file name={} writeTF={}", saveFile, writefileTF);
        } else {

            GUIATCFStormID = ATCFEntryStormTextField.getText();
            GUIATCFSiteID = ATCFEntrySiteTextField.getText();

            if ((GUIATCFStormID == null) || (GUIATCFSiteID == null)) {
                JOptionPane.showMessageDialog(this.getMainPanel(), "Please provide valid Storm and Site IDs for ATCF output.");
                return false;
            }

            // Validate the Storm ID and Site ID inputs
            boolean siteStormValid = true;
            // Storm must be 3-char
            if (GUIATCFStormID.length() != 3) {
                siteStormValid = false;
            } else {
                // It is 3-char, make sure it's DDC (digit-digit-char)
                if (! GUIATCFStormID.matches("\\d\\d[A-Z]")) {
                    siteStormValid = false;
                }
            }
            // Site must be 4-char
            if (GUIATCFSiteID.length() != 4) {
                siteStormValid = false;
            }

            if (! siteStormValid) {
                JOptionPane.showMessageDialog(null, "Please provide valid Storm and Site IDs for ATCF output.");
                return false;
            }

            // call routine to generate ATCF file name for single analysis record
            logger.debug("stormID={} siteID={}", GUIATCFStormID, GUIATCFSiteID);
            ATCFOutputFileName = Functions.adt_atcffilename(GUIATCFStormID,GUIATCFSiteID);
            logger.debug("atcf output name={}*", ATCFOutputFileName);
            ATCFOutputFilePath = System.getenv("ODTOUTPUT");
            if (ATCFOutputFilePath == null) {
                ATCFOutputFilePath = System.getenv("HOME");
            }
            logger.debug("atcf output path={}*", ATCFOutputFilePath);
            saveFile = new File(ATCFOutputFilePath + File.separator + ATCFOutputFileName);
            logger.debug("atcf output name={}*", saveFile.toString());
            writefileTF = true;
        }
        // call routine to output file
        logger.info("Site ID: " + GUIATCFSiteID + ", Storm ID: " + GUIATCFStormID);
        if ((GUIATCFSiteID == null) || (GUIATCFStormID == null)) {
            JOptionPane.showMessageDialog(historyFrame, "You must first activate ATCF output");
            return returnStatus;
        }
        ATCFFileOutput = History.ListHistory(outputstyle, GUIHistoryListFormat, GUIATCFSiteID, GUIATCFStormID);
        if (writefileTF) {
            try (FileWriter outFile = new FileWriter(saveFile)) {
                outFile.write(ATCFFileOutput);
                outFile.flush();
                outFile.close();
                ATCFMessage = String.format("Success writing ATCF file %s",saveFile);
            } catch (IOException ex) {
                logger.error("problem writing to ATCF file", ex);
                ATCFMessage = String.format("Error writing ATCF file %s",saveFile);
            }
            System.out.println(ATCFMessage);
            userMessage(ATCFMessage);
        }
        return returnStatus;
    }

    private String selectForecastFile() {

        String fileNameReturn = null;

        logger.debug("in selectForecastFile");
        JFrame forecastFileFrame = new JFrame();
        JFileChooser forecastFileChooser = new JFileChooser();
        String forecastPath = System.getenv("ODTAUTO");
        if (forecastPath == null) {
            forecastPath = getLastPath("mcv.adt.lastforecastpath", System.getProperty("user.home"));
        }
        logger.debug("forecast path={}", forecastPath);
        forecastFileChooser.setCurrentDirectory(new File(forecastPath));
        forecastFileChooser.setDialogTitle("Select ADT Forecast File");
        int returnVal = forecastFileChooser.showOpenDialog(forecastFileFrame);
        logger.debug("retVal={}", returnVal);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = forecastFileChooser.getSelectedFile();
            fileNameReturn = file.getAbsolutePath();
            setLastPath("mcv.adt.lastforecastpath", file.getPath());
        } else {
            logger.error("error with file chooser");
        }
        return fileNameReturn;
    }

    private void getADTenvParameters() {
        History.InitCurrent(true);
        GUIHistoryFileName = null;

        /* load initial ADT Environmental parameters */
        GUIDeleteTF = Env.DeleteTF;
        GUIRunAutoTF = Env.AutoTF;
        GUIOverrideSceneTF = Env.OverSceneTF;
        GUIOverrideTF = Env.OverTF;
        GUIATCFOutputTF = Env.ATCFOutputTF;
        GUIATCFRecordOutputTF = Env.ATCFRecordOutputTF;
        GUIInitStrengthTF = Env.InitStrengthTF;
        GUILandFlagTF = Env.LandFlagTF;
        GUIUseCKZTF = Env.UseCKZTF;
        GUIVmax1or10TF = Env.Vmax1or10TF;
        GUICommentAddTF = Env.CommentAddTF;
        GUIPMWActivateTF = Env.UsePMWTF;

        /* integer values */
        GUIDomainID = Env.DomainID;
        GUIForecastType = Env.ForecastFileType;
        GUIMWJulianDate = Env.MWJulianDate;
        GUIMWHHMMSSTime = Env.MWHHMMSSTime;
        GUIStartDate = Env.StartJulianDate;
        GUIStartTime = Env.StartHHMMSSTime;
        GUIEndDate = Env.EndJulianDate;
        GUIEndTime = Env.EndHHMMSSTime;
        GUIHistoryListFormat = Env.HistoryListFormat;
        /* double values */
        GUIRawTValue = Env.InitRawTValue;
        GUIMWScore = Env.MWScore;
        GUICKZGaleRadius = Env.CKZGaleRadius;
        GUICKZPenv = Env.CKZPenv;
        GUIRMWSize = Env.RMWSize;
        GUIUserLatitude = Env.SelectedLatitude;
        GUIUserLongitude = Env.SelectedLongitude;

        GUIForecastFileName = Env.ForecastFileName;            // needed?
        GUIHistoryFileListingName = Env.ASCIIOutputFileName;   // needed?
        GUIATCFStormID = Env.StormIDString;
        GUIATCFSiteID = Env.ATCFSourceAgcyIDString;

    }

    private void loadADTenvParameters() {
        /* Env GlobalVariables = new Env(); */

        logger.debug("setting env parameters");

        // send ADT Environmental parameters to Env prior to running ADT
        // boolean values
        Env.DeleteTF = GUIDeleteTF;
        Env.AutoTF = GUIRunAutoTF;
        Env.OverTF = GUIOverrideTF;
        Env.ATCFOutputTF = GUIATCFOutputTF;
        Env.ATCFRecordOutputTF = GUIATCFRecordOutputTF;
        Env.InitStrengthTF = GUIInitStrengthTF;
        Env.LandFlagTF = GUILandFlagTF;
        Env.UseCKZTF = GUIUseCKZTF;
        Env.Vmax1or10TF = GUIVmax1or10TF;
        Env.CommentAddTF = GUICommentAddTF;
        Env.OverSceneTF = GUIOverrideSceneTF;
        Env.UsePMWTF = GUIPMWActivateTF;

        // integer values
        Env.DomainID = GUIDomainID;
        Env.ForecastFileType = GUIForecastType;
        Env.MWJulianDate = GUIMWJulianDate;
        Env.MWHHMMSSTime = GUIMWHHMMSSTime;
        Env.StartJulianDate = GUIStartDate;
        Env.StartHHMMSSTime = GUIStartTime;
        Env.EndJulianDate = GUIEndDate;
        Env.EndHHMMSSTime = GUIEndTime;
        Env.HistoryListFormat = GUIHistoryListFormat;
        // double values
        Env.InitRawTValue = GUIRawTValue;
        Env.MWScore = GUIMWScore;
        Env.CKZGaleRadius = GUICKZGaleRadius;
        Env.CKZPenv = GUICKZPenv;
        Env.RMWSize = GUIRMWSize;
        Env.SelectedLatitude = GUIUserLatitude;
        Env.SelectedLongitude = GUIUserLongitude;

        logger.debug("load forecast file name={}", GUIForecastFileName);
        Env.ForecastFileName = GUIForecastFileName;   // needed?
        Env.ASCIIOutputFileName = GUIHistoryFileListingName;   // needed?
        Env.StormIDString = GUIATCFStormID;
        Env.ATCFSourceAgcyIDString = GUIATCFSiteID;

    }

    private int ReadGUIOverrideInputFile(String GUIOverrideFile) {

        logger.debug("opening file '{}'", GUIOverrideFile);

        File GUIDataFile = new File(GUIOverrideFile);
        String delims = "[ ]+";
        String line;
        int retval = 1;

        GUIOverrideTF = false;
        GUIOverrideSceneTF = false;
        GUICommentString = null;
        GUIRunAutoTF = true;
        GUIDeleteTF = false;
        GUICommentAddTF = false;
        GUIStartDate = 1900001;
        GUIStartTime = 000000;
        GUIEndDate = 1900001;
        GUIEndTime = 000000;
        GUIUserLatitude = -99.5;
        GUIUserLongitude = -999.5;
        GUIDomainID = 0;
        runFullADTAnalysis = true;

        try {
            Scanner GUIFile = new Scanner(GUIDataFile);
            while (GUIFile.hasNextLine()) {
                if ((line = GUIFile.nextLine()).isEmpty()){
                    break;
                } else {
                    String[] tokens = line.split(delims);
                    String IDstring = tokens[0];
                    String RecValue = tokens[1];
                    /* System.out.println("scanning IDstring=%s\n",IDstring); */
                    switch (IDstring) {
                        case "ATCFOutputTF":
                            GUIATCFOutputTF = Boolean.valueOf(RecValue);
                            break;
                        case "ATCFRecordOutputTF":
                            GUIATCFRecordOutputTF = Boolean.valueOf(RecValue);
                            break;
                        case "InitStrengthTF":
                            GUIInitStrengthTF = Boolean.valueOf(RecValue);
                            break;
                        case "LandFlagTF":
                            GUILandFlagTF = Boolean.valueOf(RecValue);
                            break;
                        case "UseCKZTF":
                            GUIUseCKZTF = Boolean.valueOf(RecValue);
                            break;
                        case "Vmax1or10TF":
                            GUIVmax1or10TF = Boolean.valueOf(RecValue);
                            break;
                        case "UsePMWTF":
                            GUIPMWActivateTF = Boolean.valueOf(RecValue);
                            break;
                        case "ForecastType":
                            GUIForecastType = Integer.valueOf(RecValue);
                            break;
                        case "MWJulianDate":
                            GUIMWJulianDate = Integer.valueOf(RecValue);
                            break;
                        case "MWHHMMSSTime":
                            GUIMWHHMMSSTime = Integer.valueOf(RecValue);
                            break;
                        case "HistoryListFormat":
                            GUIHistoryListFormat = Integer.valueOf(RecValue);
                            break;
                        case "RawTValue":
                            GUIRawTValue = Double.valueOf(RecValue);
                            break;
                        case "MWScore":
                            GUIMWScore = Double.valueOf(RecValue);
                            break;
                        case "CKZGaleRadius":
                            GUICKZGaleRadius = Double.valueOf(RecValue);
                            break;
                        case "CKZPenv":
                            GUICKZPenv = Double.valueOf(RecValue);
                            break;
                        case "RMWSize":
                            GUIRMWSize = Double.valueOf(RecValue);
                            break;
                        case "HistoryFileName":
                            GUIHistoryFileName = RecValue;
                            break;
                        case "ForecastFileName":
                            GUIForecastFileName = RecValue;
                            break;
                        case "HistoryFileListingName":
                            GUIHistoryFileListingName = RecValue;
                            break;
                        case "ATCFStormID":
                            GUIATCFStormID = RecValue;
                            break;
                        case "ATCFSiteID":
                            GUIATCFSiteID = RecValue;
                            break;
                        default:
                            break;
                    }
                }
            }
            GUIFile.close();
        } catch (IOException ex) {
            retval = -1;
        }
        return retval;
    }

    public void latLonWidgetChanged() {
        logger.debug("latlonwidgetchanged called");
        try {
            logger.debug("latlon widget changed");
            String message = latLonWidget.isValidValues();
            if (message != null) {
                userMessage(message);
                return;
            }
            probeLocation = ucar.visad.Util.makeEarthLocation(
                    latLonWidget.getLat(), latLonWidget.getLon()).getLatLonPoint();
        } catch (Exception e) {
            logException("Handling LatLonWidget changed", e);
        }
    }

    protected boolean shouldAddDisplayListener() {
        return true;
    }

    protected boolean shouldAddControlListener() {
        return true;
    }

    protected boolean canHandleEvents() {
        if (!getHaveInitialized() || (getMakeWindow() && !getWindowVisible())) {
            return false;
        }
        return isGuiShown();
    }

    public void handleDisplayChanged(DisplayEvent event) {
        super.handleDisplayChanged(event);
        if (canHandleEvents()) {
//            int id = event.getId();
//            // String idstring = event.toString();
//            // InputEvent inputEvent = event.getInputEvent();
//            // System.out.println("event ID=%d %s\n",id,idstring);
//            try {
//                if (id == DisplayEvent.MOUSE_PRESSED_LEFT) {
//                    logger.debug("Manual Position Selection");
//                    probeLocation = toEarth(event).getLatLonPoint();
//                    updateProbeLocation();
//                }
//            } catch (Exception e) {
//                logException("Error selecting position with mouse", e);
//            }
        }
    }

    /**
     * Respond to the probe being dragged.
     *
     * @param event Event to handle.
     */
    @Override public void propertyChange(PropertyChangeEvent event) {
        if (canHandleEvents() && SelectorDisplayable.PROPERTY_POSITION.equals(event.getPropertyName())) {
            try {
                RealTuple position = probe.getPosition();
                double[] loc = position.getValues();
                logger.debug("Manual Position Selection loc={}", loc);
                // note: loc[1] is apparently latitude, and loc[0] is longitude!
                probeLocation =
                        makeEarthLocation(loc[1], loc[0], loc[2]).getLatLonPoint();
                SwingUtilities.invokeLater(this::updatePositionWidget);
            } catch (VisADException | RemoteException ex) {
                logger.error("Error updating probe location", ex);
            }
        } else {
            super.propertyChange(event);
        }
    }

    /**
     * Update {@link #latLonWidget} if it exists.
     *
     * <p>Note: must be called from the event dispatch thread.</p>
     */
    private void updatePositionWidget() {
        if (latLonWidget != null) {
            try {
                logger.trace("attempting to update widget! lat={} lon={}", probeLocation.getLatitude(), probeLocation.getLongitude());
                latLonWidget.setLat(getDisplayConventions().formatLatLon(probeLocation.getLatitude().getValue(CommonUnit.degree)));
                latLonWidget.setLon(getDisplayConventions().formatLatLon(probeLocation.getLongitude().getValue(CommonUnit.degree)));
            } catch (VisADException ex) {
                logger.error("Error updating GUI with probe position", ex);
            }
        } else {
            logger.trace("no lat/lon widget to update!");
        }
    }

    private void updateProbeLocation() {
        try {
            if (probeLocation == null) {
                return;
            }
            double lon = probeLocation.getLongitude().getValue(CommonUnit.degree);
            double lat = probeLocation.getLatitude().getValue(CommonUnit.degree);
            probe.setPosition(
                    new RealTuple(RealTupleType.SpatialEarth3DTuple, new double[] { lon, lat, 0 }));
            probe.setVisible(true);

            GUIUserLatitude = lat;    // added TLO
            GUIUserLongitude = lon;    // added TLO
            logger.debug("set lat/lon from probe at lat={} lon={}", GUIUserLatitude, GUIUserLongitude);
            if (latLonWidget != null) {
                latLonWidget.setLat(getDisplayConventions().formatLatLon(
                        probeLocation.getLatitude().getValue(CommonUnit.degree)));
                latLonWidget.setLon(getDisplayConventions().formatLatLon(
                        probeLocation.getLongitude().getValue(CommonUnit.degree)));
            }
        } catch (Exception e) {
            logException("Handling probe changed", e);
        }
    }

    /**
     * Get the ProbeLocation property.
     *
     * @return The ProbeLocation
     */
    public LatLonPoint getProbeLocation() {
        return probeLocation;
    }

    /**
     * Set the ProbeLocation property.
     *
     * @param value New value for ProbeLocation.
     */
    public void setProbeLocation(LatLonPoint value) {
        probeLocation = value;
    }

    protected FlatField getFlatField(FieldImpl data)
            throws VisADException, RemoteException
    {
        FlatField ff;
        if (GridUtil.isSequence(data)) {
            ff = (FlatField)data.getSample(0);
        } else {
            ff = (FlatField)data;
        }
        return ff;
    }

    public EarthLocation toEarth(DisplayEvent event)
            throws VisADException, RemoteException
    {
        NavigatedDisplay d = getNavigatedDisplay();
        return (d == null) ? null : d.getEarthLocation(toBox(event));
    }

    private void GetImageDateTime() {

        RealTuple timeTuple;
        Real tt;
        DateTime dat;

        List infos = getDisplayInfos();
        DisplayInfo displayInfo = (DisplayInfo) infos.get(0);

        try {
            Animation anime = displayInfo.getViewManager().getAnimation();
            Set timeSet = anime.getSet();
            int pos = anime.getCurrent();

            timeTuple = DataUtility.getSample(timeSet, pos);
            tt = (Real) timeTuple.getComponent(0);
            dat = new DateTime(tt);
        } catch (VisADException e) {
            logException("Handling data", e);
            return;
        } catch (RemoteException f) {
            logger.warn("Something went wrong!", f);
            return;
        }

        double curdate = dat.getValue();
        logger.debug("curdate={}",curdate);

        Date datevalue = new Date((long)curdate*1000);

        SimpleDateFormat dateformat = new SimpleDateFormat("yyyyDDD");
        SimpleDateFormat timeformat = new SimpleDateFormat("HHmmss");
        dateformat.setTimeZone(TimeZone.getTimeZone("GMT"));
        timeformat.setTimeZone(TimeZone.getTimeZone("GMT"));

        String JulianDate = dateformat.format(datevalue);
        String HHMMSSTime = timeformat.format(datevalue);
        int ImageDateInt = Integer.valueOf(JulianDate);
        int ImageTimeInt = Integer.valueOf(HHMMSSTime);
        // System.out.println("image date = %d  image time=%d\n",ImageDateInt,ImageTimeInt); */

        Data.IRData_JulianDate = ImageDateInt;
        Data.IRData_HHMMSSTime = ImageTimeInt;

        logger.debug("IMAGE DATE={} TIME={}", Data.IRData_JulianDate, Data.IRData_HHMMSSTime);
    }

    private void GetImageData(float CenterLatitude, float CenterLongitude) {
        logger.debug("creating ReadIRImage()...");

        // ReadIRImage IRImage = new ReadIRImage();

        FlatField ffield;
        int SatelliteID;
        int channel;

        List sources = new ArrayList();

        logger.debug("entering getimagedata");
        boolean isTemp = false;
        choice.getDataSources(sources);
        try {
            List infos = getDisplayInfos();
            DataInstance de = getDataInstance();
            DisplayInfo displayInfo = (DisplayInfo) infos.get(0);

            Animation anime = displayInfo.getViewManager().getAnimation();
            // Set timeSet = anime.getSet();
            int pos = anime.getCurrent();
            ffield = DataUtil.getFlatField(de.getData());
            DataSourceImpl dsi = (DataSourceImpl) sources.get(0);

            if (dsi instanceof AddeImageDataSource) {
                ImageDataSource dds = (ImageDataSource) sources.get(0);
                List imageLists = dds.getImageList();

                AddeImageDescriptor aid = (AddeImageDescriptor) imageLists.get(pos);
                AreaDirectory ad = aid.getDirectory();
                SatelliteID = ad.getSensorID();
                int[] bands = ad.getBands();
                channel = bands[0];

                isTemp = Util.isCompatible(ffield, AirTemperature.getRealType());
            } else {
                channel = 4;
                SatelliteID = 70;
                // String name = ffield.getSample(0).getType().prettyString();
            }
        } catch (VisADException e) {
            logException("Handling data", e);
            return;
        } catch (RemoteException f) {
            logger.warn("Something went wrong!", f);
            return;
        }

        // String shortName = choice.getName();

        Env.UserDefineDomain = 0; // automated
        // String sidName = Functions.adt_sattypes(SatelliteID);

        logger.debug("SatelliteID={}", SatelliteID);

        try {
            ReadIRImage.ReadIRDataFile(ffield,
                    CenterLatitude,
                    CenterLongitude,
                    SatelliteID,
                    channel,
                    isTemp);
        }
        catch (Exception ex) {
            logger.error("ReadIRImage failed", ex);
        }
    }
}