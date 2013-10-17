package ucar.unidata.data.imagery;

import edu.wisc.ssec.mcidas.AREAnav;
import ucar.unidata.data.*;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.view.geoloc.NavigatedMapPanel;
import visad.VisADException;
import visad.data.mcidas.AreaAdapter;

import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: opendap
 * Date: 10/17/13
 * Time: 4:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddeImagePreviewPanel extends DataSelectionComponent {

    /** _more_ */
    protected NavigatedMapPanel display;

    /** _more_ */
    private AddeImagePreview imagePreview;

    /** _more_ */
    GeoSelection geoSelection;

    /** _more_ */
    private JPanel MasterPanel;

    /** _more_ */
    private String source;

    /** _more_ */
    private AreaAdapter aAdapter;

    /** _more_ */
    AddeImageDescriptor descriptor;

    /** _more_ */
    private JCheckBox chkUseFull;
    //final AddeImageDataSource this;

    /** _more_ */
    public String USE_DEFAULTREGION = "Use Default";

    /** _more_ */
    public String USE_SELECTEDREGION = "Use Selected";

    /** _more_ */
    public String USE_DISPLAYREGION = "Use Display Area";

    /** _more_ */
    private String[] regionSubsetOptionLabels = new String[] {
            USE_DEFAULTREGION,
            USE_SELECTEDREGION, USE_DISPLAYREGION };

    /** _more_ */
    private JComponent regionsListInfo;

    /** _more_ */
    private String regionOption = USE_DEFAULTREGION;


    /** _more_ */
    AddeImageDataSource imageDataSource;


    /** _more_ */
    JComboBox regionOptionLabelBox;

    /** _more_ */
    AddeImageSelectionPanel advancedSelection;

    /** _more_ */
    AREAnav baseAnav;

    /** _more_ */
    int eMag;

    /** _more_ */
    int lMag;
    /**
     * Construct a AddeImagePreviewPanel
     *
     *
     *
     * @param imageDataSource _more_
     * @param adapter _more_
     * @param source _more_
     * @param descriptor _more_
     *
     * @throws java.io.IOException _more_
     * @throws java.text.ParseException _more_
     * @throws visad.VisADException _more_
     */
    public AddeImagePreviewPanel(AddeImageDataSource imageDataSource,
                                 AreaAdapter adapter, String source,
                                 AddeImageDescriptor descriptor,
                                 AREAnav baseAnav,
                                 AddeImageSelectionPanel advancedSelection)
            throws IOException, ParseException, VisADException {
        super("Region");
        this.imageDataSource = imageDataSource;
        this.aAdapter        = adapter;
        this.source          = source;
        this.descriptor      = descriptor;
        this.baseAnav = baseAnav;
        this.advancedSelection = advancedSelection;

        this.imagePreview    = createImagePreview(source);
        display = new NavigatedMapPanel(null, true, true,
                imagePreview.getPreviewImage(),
                this.aAdapter.getAreaFile());
        this.eMag = imageDataSource.getEMag();
        this.lMag = imageDataSource.getLMag();

        chkUseFull = new JCheckBox("Use Default");

        chkUseFull.setSelected(true);
        getRegionsList();
        JScrollPane jsp = new JScrollPane();
        jsp.getViewport().setView(display);
        //  jsp.add(GuiUtils.topCenter(regionsListInfo, display));
        JPanel labelsPanel = null;
        labelsPanel = new JPanel();
        labelsPanel.setLayout(new BoxLayout(labelsPanel, 1));
        labelsPanel.add(regionsListInfo);

        MasterPanel = new JPanel(new java.awt.BorderLayout());
        MasterPanel.add(labelsPanel, "North");
        MasterPanel.add(jsp, "Center");

        display.getNavigatedPanel().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent focusEvent) {
                System.err.println("Gain");
            }

            @Override
            public void focusLost(FocusEvent focusEvent) {
                update();
            }
        });


    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent getRegionsList() {
        return getRegionsList(USE_DEFAULTREGION);

    }

    public NavigatedMapPanel getNavigatedMapPanel(){
        return this.display;
    }
    /**
     * _more_
     *
     * @param cbxLabel _more_
     *
     * @return _more_
     */
    public JComponent getRegionsList(String cbxLabel) {
        if (regionsListInfo == null) {
            regionsListInfo = makeRegionsListAndPanel(cbxLabel, null);
        }

        return regionsListInfo;
    }

    /**
     * _more_
     *
     * @param cbxLabel _more_
     * @param extra _more_
     *
     * @return _more_
     */
    private JComponent makeRegionsListAndPanel(String cbxLabel,
                                               JComponent extra) {
        regionOptionLabelBox = new JComboBox();

        //added
        regionOptionLabelBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                String selectedObj =
                        (String) regionOptionLabelBox.getSelectedItem();
                setRegionOptions(selectedObj);
                setAdvancedPanel(selectedObj);

            }

        });

        //timeDeclutterFld = new JTextField("" + getTimeDeclutterMinutes(), 5);
        GuiUtils.enableTree(regionOptionLabelBox, true);

        List regionOptionNames = Misc.toList(regionSubsetOptionLabels);

        GuiUtils.setListData(regionOptionLabelBox, regionOptionNames);
        //        JComponent top = GuiUtils.leftRight(new JLabel("Times"),
        //                                            allTimesButton);
        JComponent top;


        if (extra != null) {
            top = GuiUtils.leftRight(extra, regionOptionLabelBox);
        } else {
            top = GuiUtils.right(regionOptionLabelBox);
        }


        return top;

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getRegionOptions() {

        return (String) regionOptionLabelBox.getSelectedItem();
    }

    /**
     * _more_
     *
     * @param selectedObject _more_
     */
    public void setRegionOptions(String selectedObject) {

        regionOption = selectedObject.toString();
        if (selectedObject.equals(USE_DEFAULTREGION)) {
            display.getNavigatedPanel().setSelectedRegion(
                    (LatLonRect) null);
            GeoSelection gs = this.imageDataSource.getDataSelection().getGeoSelection();
            if (gs != null) {
                gs.setBoundingBox(null);
            }
            display.getNavigatedPanel().setSelectRegionMode(false);
            display.getNavigatedPanel().repaint();
        } else if (selectedObject.equals(USE_SELECTEDREGION)) {
            display.getNavigatedPanel().setSelectRegionMode(true);
        } else if (selectedObject.equals(USE_DISPLAYREGION)) {
            display.getNavigatedPanel().setSelectedRegion(
                    (LatLonRect) null);
            display.getNavigatedPanel().setSelectRegionMode(false);
            display.getNavigatedPanel().repaint();
        }
    }

    /**
     * _more_
     *
     * @param selectedObject _more_
     */
    public void setAdvancedPanel(String selectedObject) {
        if (advancedSelection == null) {
            return;
        }

        regionOption = selectedObject.toString();
        if (selectedObject.equals(USE_SELECTEDREGION)) {
            // only progressiveResolution and mag can be changed
            advancedSelection.enablePanelAll(true);
            advancedSelection.prograssiveCbx.doClick();
        } else {
            advancedSelection.enablePanelAll(false);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getRegionOption() {
        return regionOption;
    }

    /**
     * _more_
     *
     * @param option _more_
     */
    public void setRegionOption(String option) {
        regionOption = option;
    }

    /**
     * _more_
     *
     * @param dataChoice _more_
     */
    public void setDataChoice(DataChoice dataChoice) {


        // display.updateImage(image_preview.getPreviewImage());
    }

    /**
     * _more_
     *
     * @param source _more_
     *
     *
     * @return _more_
     * @throws IOException _more_
     */
    private AddeImagePreview createImagePreview(String source)
            throws IOException {

        int selIndex = -1;

        //LastBandNames = SelectedBandNames;
        //LastCalInfo = CalString;
        this.imageDataSource.getIdv().showWaitCursor();
        AddeImagePreview image = new AddeImagePreview(this.aAdapter,
                this.descriptor);
        this.imageDataSource.getDataContext().getIdv().showNormalCursor();
        //String bandInfo = "test";
        // lblBandInfo = new JLabel(bandInfo);

        return image;
    }





    /**
     * _more_
     *
     * @return _more_
     */
    public String getFileName() {
        return this.source;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public AddeImagePreview getAddeImagePreview() {
        return imagePreview;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent doMakeContents() {
        return MasterPanel;
    }

    /**
     * _more_
     *
     * @param dataSelection _more_
     */
    public void applyToDataSelection(DataSelection dataSelection) {
        ProjectionRect rect =
                display.getNavigatedPanel().getSelectedRegion();
        if (rect == null) {
            // no region subset, full image
        } else {
            rect.getBounds();
            GeoLocationInfo bbox = GeoSelection.getDefaultBoundingBox();
            if (bbox != null) {
                this.geoSelection = new GeoSelection(bbox);
            }
        }


    }

    /**
     * _more_
     */
    public void update() {

        ProjectionRect rect =
                display.getNavigatedPanel().getSelectedRegion();
        boolean hasCorner = false;
        if (rect == null) {
            // no region subset, full image
        } else {
            ProjectionImpl projectionImpl = display.getProjectionImpl();
            LatLonRect latLonRect =
                    projectionImpl.getLatLonBoundingBox(rect);
            GeoLocationInfo gInfo;
            if (latLonRect.getHeight() != latLonRect.getHeight()) {
                //corner point outside the earth

                LatLonPointImpl cImpl =
                        projectionImpl.projToLatLon(rect.x
                                + rect.getWidth() / 2, rect.y
                                + rect.getHeight() / 2);
                LatLonPointImpl urImpl =
                        projectionImpl.projToLatLon(rect.x + rect.getWidth(),
                                rect.y + rect.getHeight());
                LatLonPointImpl ulImpl =
                        projectionImpl.projToLatLon(rect.x,
                                rect.y + rect.getHeight());
                LatLonPointImpl lrImpl =
                        projectionImpl.projToLatLon(rect.x + rect.getWidth(),
                                rect.y);
                LatLonPointImpl llImpl =
                        projectionImpl.projToLatLon(rect.x, rect.y);

                double maxLat = Double.NaN;
                double minLat = Double.NaN;
                double maxLon = Double.NaN;
                double minLon = Double.NaN;
                if (cImpl.getLatitude() != cImpl.getLatitude()) {
                    //do nothing
                } else if (ulImpl.getLatitude() != ulImpl.getLatitude()) {
                    //upper left conner
                    maxLat = cImpl.getLatitude()
                            + (cImpl.getLatitude()
                            - lrImpl.getLatitude());
                    minLat = lrImpl.getLatitude();
                    maxLon = lrImpl.getLongitude();
                    minLon = cImpl.getLongitude()
                            - (lrImpl.getLongitude()
                            - cImpl.getLongitude());
                } else if (urImpl.getLatitude() != urImpl.getLatitude()) {
                    //upper right conner
                    maxLat = cImpl.getLatitude()
                            + (cImpl.getLatitude()
                            - llImpl.getLatitude());
                    minLat = llImpl.getLatitude();
                    maxLon = cImpl.getLongitude()
                            + (cImpl.getLongitude()
                            - lrImpl.getLongitude());
                    minLon = lrImpl.getLongitude();
                } else if (llImpl.getLatitude() != llImpl.getLatitude()) {
                    // lower left conner
                    maxLat = urImpl.getLatitude();
                    minLat = cImpl.getLatitude()
                            - (urImpl.getLatitude()
                            - cImpl.getLatitude());
                    maxLon = urImpl.getLongitude();
                    minLon = cImpl.getLongitude()
                            - (urImpl.getLongitude()
                            - cImpl.getLongitude());
                } else if (lrImpl.getLatitude() != lrImpl.getLatitude()) {
                    // lower right conner
                    maxLat = ulImpl.getLatitude();
                    minLat = cImpl.getLatitude()
                            - (ulImpl.getLatitude()
                            - cImpl.getLatitude());
                    maxLon = cImpl.getLongitude()
                            + (cImpl.getLongitude()
                            - ulImpl.getLongitude());
                    minLon = ulImpl.getLongitude();
                }
                hasCorner = true;
                gInfo = new GeoLocationInfo(maxLat,
                        LatLonPointImpl.lonNormal(minLon), minLat,
                        LatLonPointImpl.lonNormal(maxLon));

            } else {
                gInfo = new GeoLocationInfo(latLonRect);
            }
            // update the advanced
            float[][] latlon = new float[2][1];
            latlon[1][0] = (float) gInfo.getMinLon();
            latlon[0][0] = (float) gInfo.getMaxLat();
            float[][] ulLinEle = baseAnav.toLinEle(latlon);

            latlon[1][0] = (float) gInfo.getMaxLon();
            latlon[0][0] = (float) gInfo.getMinLat();
            float[][] lrLinEle   = baseAnav.toLinEle(latlon);
            int       displayNum = (int) rect.getWidth();
            int       lines      = (int) (lrLinEle[1][0]
                    - ulLinEle[1][0]);
            //* Math.abs(lMag);
            int elems = (int) (lrLinEle[0][0] - ulLinEle[0][0]);
            //* Math.abs(eMag);
            // set latlon coord
            imageDataSource.advancedSelection.setIsFromRegionUpdate(true);
            imageDataSource.advancedSelection.coordinateTypeComboBox
                    .setSelectedIndex(0);
            // set lat lon values   locateValue = Misc.format(maxLat) + " " + Misc.format(minLon);
            if ( !hasCorner) {
                imageDataSource.advancedSelection.setPlace("ULEFT");
                imageDataSource.advancedSelection.setLatitude(
                        gInfo.getMaxLat());
                imageDataSource.advancedSelection.setLongitude(
                        gInfo.getMinLon());
                imageDataSource.advancedSelection.convertToLineEle();
            } else {
                imageDataSource.advancedSelection.setPlace("CENTER");
                double centerLat = (gInfo.getMaxLat()
                        + gInfo.getMinLat()) / 2;
                double centerLon = (gInfo.getMaxLon()
                        + gInfo.getMinLon()) / 2;
                imageDataSource.advancedSelection.setLatitude(centerLat);
                imageDataSource.advancedSelection.setLongitude(centerLon);
                imageDataSource.advancedSelection.convertToLineEle();
            }
            // update the size
            imageDataSource.advancedSelection.setNumLines(lines);
            imageDataSource.advancedSelection.setNumEles(elems);
            imageDataSource.advancedSelection.setIsFromRegionUpdate(
                    false);

            // update the mag slider
            imageDataSource.advancedSelection.setElementMagSlider(
                    -Math.abs(eMag));
            imageDataSource.advancedSelection.setLineMagSlider(
                    -Math.abs(lMag));
            imageDataSource.advancedSelection.setBaseNumElements(elems
                    * Math.abs(eMag));
            imageDataSource.advancedSelection.setBaseNumLines(lines
                    * Math.abs(lMag));

        }

    }


}