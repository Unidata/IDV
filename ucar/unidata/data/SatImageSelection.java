package ucar.unidata.data;

//~--- non-JDK imports --------------------------------------------------------

import edu.wisc.ssec.mcidas.AreaDirectory;

import ucar.unidata.data.imagery.AddeImageDataSource;
import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.data.imagery.AddeImageInfo;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The Class SatImageSelection.
 */
public class SatImageSelection extends DataSelectionComponent {

    /** This is the list of labels used for the advanced gui. */
    private static final String[] ADVANCED_LABELS = {
        "Data Type:", "Channel:", "Placement:", "Location:", "Image Size:", "Magnification:", "Navigation Type:"
    };

    /** flag for center. */
    private static final String PLACE_CENTER = "CENTER";

    /** flag for lower left. */
    private static final String PLACE_LLEFT = "LLEFT";

    /** flag for lower right. */
    private static final String PLACE_LRIGHT = "LRIGHT";

    /** flag for upper left. */
    private static final String PLACE_ULEFT = "ULEFT";

    /** flag for upper right. */
    private static final String PLACE_URIGHT = "URIGHT";

    /** Property for image default value band. */
    private static final String PROP_BAND = "BAND";

    /** Property for image default value loc. */
    private static final String PROP_LOC = "LOC";

    /** Property for image default value mag. */
    private static final String PROP_MAG = "MAG";

    /** Property for image default value unit. */
    private static final String PROP_NAV = "NAV";

    /** Property for image default value place. */
    private static final String PROP_PLACE = "PLACE";

    /** Property for image default value size. */
    private static final String PROP_SIZE = "SIZE";

    /** Property for image default value unit. */
    private static final String PROP_UNIT = "UNIT";

    /** limit of slider. */
    private static final int SLIDER_MAX = 29;

    /** This is the list of properties that are used in the advanced gui. */
    private static final String[] ADVANCED_PROPS = {
        PROP_UNIT, PROP_BAND, PROP_PLACE, PROP_LOC, PROP_SIZE, PROP_MAG, PROP_NAV
    };

    /** base number of lines. */
    private double baseNumElements = 0.0;

    /** base number of lines. */
    private double baseNumLines = 0.0;

    /** Keep track of the lines to element ratio. */
    private double linesToElements = 1.0;

    /** Maps the PROP_ property name to the gui component. */
    private Hashtable propToComps = new Hashtable();

    /** flag for setting properties. */
    private boolean amSettingProperties = false;

    /** The adde image data source. */
    private AddeImageDataSource addeImageDataSource;

    /** Widget for the element center point in the advanced section. */
    protected JTextField centerElementFld;

    /** Label used for the element center. */
    private JLabel centerElementLbl;

    /** Label used for the center latitude. */
    private JLabel centerLatLbl;

    /** Widget for the line center point in the advanced section. */
    protected JTextField centerLineFld;

    /** Label used for the line center. */
    private JLabel centerLineLbl;

    /** Label used for the center longitude. */
    private JLabel centerLonLbl;

    /** Label for the element mag. in the advanced section */
    JLabel elementMagLbl;

    /** Widget for the element magnfication in the advanced section. */
    JSlider elementMagSlider;

    /** full resolution button. */
    private JButton fullResBtn;

    /** Input for lat/lon center point. */
    protected LatLonWidget latLonWidget;

    /** Label for the line mag. in the advanced section */
    JLabel lineMagLbl;

    /** Widget for the line magnfication in the advanced section. */
    JSlider lineMagSlider;

    /** location panel. */
    private GuiUtils.CardLayoutPanel locationPanel;

    /** lock button. */
    private JToggleButton lockBtn;

    /** Widget for selecting image nav type. */
    protected JComboBox navComboBox;

    /** Widget to hold the number of elements in the advanced. */
    JTextField numElementsFld;

    /** Widget to hold the number of lines in the advanced. */
    JTextField numLinesFld;

    /** the place string. */
    private String place;

    /** place label. */
    private JLabel placeLbl;

    /** Holds the properties. */
    private JPanel propPanel;

    /** The current AreaDirectory used for properties. */
    AreaDirectory propertiesAD;

    /** size label. */
    JLabel sizeLbl;

    /**
     * Instantiates a new sat image selection.
     *
     * @param addeImageDataSource the adde image data source
     */
    public SatImageSelection(AddeImageDataSource addeImageDataSource) {
        super("Advanced");
        this.addeImageDataSource = addeImageDataSource;
    }

    /*
     *  (non-Javadoc)
     * @see ucar.unidata.data.DataSelectionComponent#doMakeContents()
     */
    @Override
    protected JComponent doMakeContents() {
        List bottomComps = new ArrayList();

        getBottomComponents(bottomComps);

        return propPanel;
    }

    /*
     *  (non-Javadoc)
     * @see ucar.unidata.data.DataSelectionComponent#applyToDataSelection(ucar.unidata.data.DataSelection)
     */
    @Override
    public void applyToDataSelection(DataSelection dataSelection) {
        addeImageDataSource.getImageList();

        for (Object o : addeImageDataSource.getImageList()) {
            AddeImageDescriptor d         = (AddeImageDescriptor) o;
            AddeImageInfo       imageInfo = d.getImageInfo();
        }
    }

    /**
     * Get the list of advanced property names.
     *
     * @return array of advanced property names
     */
    protected String[] getAdvancedProps() {
        return ADVANCED_PROPS;
    }

    /**
     * Get the list of advanced property labels.
     *
     * @return list of advanced property labels
     */
    protected String[] getAdvancedLabels() {
        return ADVANCED_LABELS;
    }

    /**
     * Check if we are using the lat/lon widget.
     *
     * @return true if we are using the lat/lon widget
     */
    private boolean useLatLon() {
        return locationPanel.getVisibleIndex() == 0;
    }

    /**
     * Translate a place name into a human readable form.
     *
     * @param place raw name
     * @return human readable name
     */
    private String translatePlace(String place) {
        place = place.toUpperCase();

        if (place.equals(PLACE_ULEFT)) {
            return "Upper left";
        }

        if (place.equals(PLACE_LLEFT)) {
            return "Lower left";
        }

        if (place.equals(PLACE_URIGHT)) {
            return "Upper right";
        }

        if (place.equals(PLACE_LRIGHT)) {
            return "Lower right";
        }

        if (place.equals(PLACE_CENTER)) {
            return "Center";
        }

        return place;
    }

    /**
     * Change the place.
     *
     * @param newPlace new place
     */
    public void changePlace(String newPlace) {
        this.place = newPlace;

        String s = translatePlace(place) + "=";

        placeLbl.setText(StringUtil.padRight(s, 12));
    }

    /**
     * Associates the goven JComponent with the PROP_ property identified by the
     * given propId.
     *
     * @param propId The property
     * @param comp The gui component that allows the user to set the property
     * @return Just returns the given comp
     */
    protected JComponent addPropComp(String propId, JComponent comp) {
        Object oldComp = propToComps.get(propId);

        if (oldComp != null) {
            throw new IllegalStateException("Already have a component defined:" + propId);
        }

        propToComps.put(propId, comp);

        return comp;
    }

    /**
     * Get the "lock" button.
     *
     * @return the lock button
     */
    private JToggleButton getLockButton() {
        if (lockBtn == null) {
            lockBtn = GuiUtils.getToggleImageButton("/auxdata/ui/icons/link.png", "/auxdata/ui/icons/link_break.png",
                    0, 0, true);
            lockBtn.setContentAreaFilled(false);
            lockBtn.setSelected(true);
            lockBtn.setToolTipText("Unlock to automatically change size when changing magnification");
        }

        return lockBtn;
    }

    /**
     * Get the value of the given magnification slider.
     *
     * @param slider
     *            The slider to get the value from
     * @return The magnification value
     */
    private int getMagValue(JSlider slider) {

        // Value is [-SLIDER_MAX,SLIDER_MAX]. We change 0 and -1 to 1
        int value = slider.getValue();

        if (value >= 0) {
            return value + 1;
        }

        return value - 1;
    }

    /**
     * Get the value of the line magnification slider.
     *
     * @return The magnification value for the line
     */
    private int getLineMagValue() {
        return getMagValue(lineMagSlider);
    }

    /**
     * Get the value of the element magnification slider.
     *
     * @return The magnification value for the element
     */
    private int getElementMagValue() {
        return getMagValue(elementMagSlider);
    }

    /**
     * Handle changes to the element/line mag sliders.
     *
     * @param recomputeLineEleRatio true to recompute the ratio
     */
    private void elementMagSliderChanged(boolean recomputeLineEleRatio) {
        int value = getElementMagValue();

        if ((Math.abs(value) < SLIDER_MAX)) {
            int lineMag = getLineMagValue();

            if (lineMag > value) {
                linesToElements = Math.abs(lineMag / (double) value);
            } else {
                linesToElements = Math.abs((double) value / lineMag);
            }
        }

        // System.out.println(" changelistener: linesToElements = " +
        // linesToElements);
        elementMagLbl.setText(StringUtil.padLeft("" + value, 3));

        if (!getLockButton().isSelected()) {
            if (value > 0) {
                numElementsFld.setText("" + (int) (baseNumElements * value));
            } else {
                numElementsFld.setText("" + (int) (baseNumElements / (double) -value));
            }
        }
    }

    /**
     * Handle the line mag slider changed event.
     *
     * @param autoSetSize true to automatically set the size
     */
    private void lineMagSliderChanged(boolean autoSetSize) {
        try {
            int value = getLineMagValue();

            lineMagLbl.setText(StringUtil.padLeft("" + value, 3));

            if (autoSetSize) {
                if (value > 0) {
                    numLinesFld.setText("" + (int) (baseNumLines * value));
                } else {
                    numLinesFld.setText("" + (int) (baseNumLines / (double) -value));
                }
            }

            if (value == 1) {       // special case
                if (linesToElements < 1.0) {
                    value = (int) (-value / linesToElements);
                } else {
                    value = (int) (value * linesToElements);
                }
            } else if (value > 1) {
                value = (int) (value * linesToElements);
            } else {
                value = (int) (value / linesToElements);
            }

            value = (value > 0)
                    ? value - 1
                    : value + 1;    // since slider is one

            // off
            amSettingProperties = true;
            elementMagSlider.setValue(value);
            amSettingProperties = false;
            elementMagSliderChanged(false);
        } catch (Exception exc) {

            // logException("Setting line magnification", exc); //TODO: Fix me
            exc.printStackTrace();
        }

        // amSettingProperties = false;
    }

    /**
     * Set the mag slider values.
     *
     * @param lineValue the line value
     * @param elementValue the element value
     */
    private void setMagSliders(int lineValue, int elementValue) {
        if (lineMagSlider != null) {
            if (lineValue > 0) {
                lineValue--;
            } else if (lineValue < 0) {
                lineValue++;
            }

            if (elementValue > 0) {
                elementValue--;
            } else if (elementValue < 0) {
                elementValue++;
            }

            lineMagSlider.setValue(lineValue);
            elementMagSlider.setValue(elementValue);
            lineMagLbl.setText(StringUtil.padLeft("" + getLineMagValue(), 3));
            elementMagLbl.setText(StringUtil.padLeft("" + getElementMagValue(), 3));
            linesToElements = Math.abs(lineValue / (double) elementValue);

            if (Double.isNaN(linesToElements)) {
                linesToElements = 1.0;
            }
        }
    }

    /**
     * Cycle the place.
     */
    public void cyclePlace() {
        if (place.equals(PLACE_CENTER)) {
            changePlace(PLACE_ULEFT);
        } else {
            changePlace(PLACE_CENTER);
        }
    }

    /**
     * Set to full resolution.
     */
    public void setToFullResolution() {
        if (propertiesAD == null) {
            return;
        }

        amSettingProperties = true;
        numLinesFld.setText("" + propertiesAD.getLines());
        numElementsFld.setText("" + propertiesAD.getElements());
        changePlace(PLACE_CENTER);

        if (useLatLon()) {
            locationPanel.flip();
        }

        centerLineFld.setText("" + (propertiesAD.getLines() / 2 - 1));
        centerElementFld.setText("" + (propertiesAD.getElements() / 2 - 1));
        setMagSliders(1, 1);
        amSettingProperties = false;
    }

    /**
     * Add the bottom advanced gui panel to the list.
     *
     * @param bottomComps the bottom components
     * @return the bottom components
     */
    protected void getBottomComponents(List bottomComps) {
        String[] propArray  = getAdvancedProps();
        String[] labelArray = getAdvancedLabels();

        // List bottomComps = new ArrayList();
        Insets  dfltGridSpacing = new Insets(4, 0, 4, 0);
        String  dfltLblSpacing  = " ";
        boolean haveNav         = Misc.toList(propArray).contains(PROP_NAV);

        for (int propIdx = 0; propIdx < propArray.length; propIdx++) {
            JComponent propComp = null;
            String     prop     = propArray[propIdx];

            if (prop.equals(PROP_PLACE)) {

                // Moved to PROP_LOC
            } else if (prop.equals(PROP_LOC)) {
                placeLbl = GuiUtils.getFixedWidthLabel("");
                changePlace(PLACE_CENTER);
                addPropComp(PROP_PLACE, placeLbl);
                latLonWidget     = new LatLonWidget();
                centerLineFld    = new JTextField("", 3);
                centerElementFld = new JTextField("", 3);
                fullResBtn       = GuiUtils.makeImageButton("/auxdata/ui/icons/arrow_out.png", this,
                        "setToFullResolution");
                fullResBtn.setContentAreaFilled(false);
                fullResBtn.setToolTipText("Set to full resolution");

                final JButton centerPopupBtn = GuiUtils.getImageButton("/auxdata/ui/icons/MapIcon16.png", getClass());

                centerPopupBtn.setToolTipText("Center on current displays");
                centerPopupBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        addeImageDataSource.getIdv().getIdvUIManager().popupCenterMenu(centerPopupBtn, latLonWidget);
                    }
                });

                JComponent centerPopup = GuiUtils.inset(centerPopupBtn, new Insets(0, 0, 0, 4));

                GuiUtils.tmpInsets = dfltGridSpacing;

                final JPanel latLonPanel = GuiUtils.hbox(new Component[] {
                    centerLatLbl = GuiUtils.rLabel(" Lat:" + dfltLblSpacing), latLonWidget.getLatField(),
                    centerLonLbl = GuiUtils.rLabel(" Lon:" + dfltLblSpacing), latLonWidget.getLonField(),
                    new JLabel(" "), centerPopup
                });
                final JPanel lineElementPanel = GuiUtils.hbox(new Component[] {
                                                    centerLineLbl = GuiUtils.rLabel(" Line:" + dfltLblSpacing),
                        centerLineFld, centerElementLbl = GuiUtils.rLabel(" Element:" + dfltLblSpacing),
                        centerElementFld });

                locationPanel = new GuiUtils.CardLayoutPanel();
                locationPanel.addCard(latLonPanel);
                locationPanel.addCard(lineElementPanel);

                JButton locPosButton = GuiUtils.makeImageButton("/auxdata/ui/icons/Refresh16.gif", this, "cyclePlace",
                                           null, true);

                locPosButton.setToolTipText("Change place type");

                JButton locTypeButton = GuiUtils.makeImageButton("/auxdata/ui/icons/Refresh16.gif", locationPanel,
                                            "flip", null, true);

                locTypeButton.setToolTipText("Toggle between Latitude/Longitude and Line/Element");
                propComp = GuiUtils.hbox(new Component[] { locPosButton, placeLbl, locTypeButton, locationPanel }, 5);
                addPropComp(PROP_LOC, propComp);
            } else if (prop.equals(PROP_MAG)) {
                boolean oldAmSettingProperties = amSettingProperties;

                amSettingProperties = true;

                ChangeListener lineListener = new javax.swing.event.ChangeListener() {
                    public void stateChanged(ChangeEvent evt) {
                        if (amSettingProperties) {
                            return;
                        }

                        lineMagSliderChanged(!getLockButton().isSelected());
                    }
                };
                ChangeListener elementListener = new ChangeListener() {
                    public void stateChanged(javax.swing.event.ChangeEvent evt) {
                        if (amSettingProperties) {
                            return;
                        }

                        elementMagSliderChanged(true);
                    }
                };
                JComponent[] lineMagComps = GuiUtils.makeSliderPopup(-SLIDER_MAX, SLIDER_MAX, 0, lineListener);

                lineMagSlider = (JSlider) lineMagComps[1];
                lineMagSlider.setMajorTickSpacing(1);
                lineMagSlider.setSnapToTicks(true);
                lineMagSlider.setExtent(1);
                lineMagComps[0].setToolTipText("Change the line magnification");

                JComponent[] elementMagComps = GuiUtils.makeSliderPopup(-SLIDER_MAX, SLIDER_MAX, 0, elementListener);

                elementMagSlider = (JSlider) elementMagComps[1];
                elementMagSlider.setExtent(1);
                elementMagSlider.setMajorTickSpacing(1);
                elementMagSlider.setSnapToTicks(true);
                elementMagComps[0].setToolTipText("Change the element magnification");
                lineMagSlider.setToolTipText("Slide to set line magnification factor");
                lineMagLbl = GuiUtils.getFixedWidthLabel(StringUtil.padLeft("1", 3));
                elementMagSlider.setToolTipText("Slide to set element magnification factor");
                elementMagLbl       = GuiUtils.getFixedWidthLabel(StringUtil.padLeft("1", 3));
                amSettingProperties = oldAmSettingProperties;
                GuiUtils.tmpInsets  = new Insets(0, 0, 0, 0);

                JPanel magPanel = GuiUtils.doLayout(new Component[] {
                    lineMagLbl, GuiUtils.inset(lineMagComps[0], new Insets(0, 4, 0, 0)), new JLabel("    X"),
                    elementMagLbl, GuiUtils.inset(elementMagComps[0], new Insets(0, 4, 0, 0)),
                    GuiUtils.inset(getLockButton(), new Insets(0, 10, 0, 0))
                }, 7, GuiUtils.WT_N, GuiUtils.WT_N);

                addPropComp(PROP_MAG, propComp = magPanel);

                if (haveNav) {
                    navComboBox = new JComboBox();
                    GuiUtils.setListData(navComboBox,
                                         Misc.newList(new TwoFacedObject("Default", "X"),
                                                      new TwoFacedObject("Lat/Lon", "LALO")));
                    addPropComp(PROP_NAV, navComboBox);

                    boolean showNav = false;

                    // showNav = getProperty("includeNavComp", false);
                    showNav = false;

                    if (showNav) {
                        propComp = GuiUtils.hbox(propComp,
                                                 GuiUtils.inset(new JLabel("Navigation Type:"),
                                                     new Insets(0, 10, 0, 5)), navComboBox, 5);
                    }
                }
            } else if (prop.equals(PROP_SIZE)) {
                numLinesFld    = new JTextField("", 4);
                numElementsFld = new JTextField("", 4);
                numLinesFld.setToolTipText("Number of lines");
                numElementsFld.setToolTipText("Number of elements");
                GuiUtils.tmpInsets = dfltGridSpacing;
                sizeLbl            = GuiUtils.lLabel("");

                /*
                 * JPanel sizePanel = GuiUtils.left(GuiUtils.doLayout(new
                 * Component[] { GuiUtils.rLabel("Lines:" + dfltLblSpacing),
                 * numLinesFld, GuiUtils.rLabel(" Elements:" + dfltLblSpacing),
                 * numElementsFld, new JLabel(" "), sizeLbl }, 6, GuiUtils.WT_N,
                 * GuiUtils.WT_N));
                 */
                JPanel sizePanel = GuiUtils.left(GuiUtils.doLayout(new Component[] {
                    numLinesFld, new JLabel(" X "), numElementsFld /* , lockBtn */, GuiUtils.filler(10, 1), fullResBtn,    /*
                                                                                                                            *                                     new
                                                                                                                            *                                     JLabel
                                                                                                                            *                                     (" "),
                                                                                                                            */
                    sizeLbl
                }, 7, GuiUtils.WT_N, GuiUtils.WT_N));

                addPropComp(PROP_SIZE, propComp = sizePanel);
            }

            if (propComp != null) {
                bottomComps.add(GuiUtils.rLabel(labelArray[propIdx]));
                bottomComps.add(GuiUtils.left(propComp));
            }
        }

        GuiUtils.tmpInsets = new Insets(3, 4, 0, 4);
        propPanel          = GuiUtils.doLayout(bottomComps, 2, GuiUtils.WT_N, GuiUtils.WT_N);
    }
}
