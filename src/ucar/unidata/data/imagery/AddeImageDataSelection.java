/*
 * Copyright 1997-2014 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.imagery;


import edu.wisc.ssec.mcidas.AREAnav;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.adde.AddeImageURL;
import edu.wisc.ssec.mcidas.adde.AddeTextReader;

import ucar.unidata.data.*;
import ucar.unidata.geoloc.*;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.NavigatedViewManager;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.unidata.view.geoloc.NavigatedMapPanel;
import ucar.unidata.view.geoloc.NavigatedPanel;

import visad.VisADException;

import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.AreaAdapter;

import visad.georef.*;

import java.awt.*;
import java.awt.event.*;

import java.awt.geom.Rectangle2D;
import java.io.IOException;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Created with IntelliJ IDEA.
 * User: yuanho
 * Date: 11/23/13
 * Time: 10:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddeImageDataSelection {

    /** _more_ */
    public AddeImageDataSource dataSource;

    /** _more_ */
    public String source;

    /** _more_ */
    public AddeImageDescriptor descriptor;

    /** _more_ */
    public AREAnav baseAnav;

    /** _more_ */
    public DataChoice dataChoice;

    /** _more_ */
    public MapProjection sampleProjection;

    /** _more_ */
    public AddeImageAdvancedPanel advancedPanel;

    /** _more_ */
    public AddeImagePreviewPanel regionPanel;

    /** _more_ */
    public AreaAdapter aAdapter;

    /** _more_ */
    //public JCheckBox prograssiveCbx;

    /** _more_ */
    //public JCheckBox prograssiveCbx1;

    /** _more_ */
    public JPanel leMagPanel;

    /**
     * Construct a AddeImageDataSelection
     *
     * @param dataSource _more_
     * @param dc _more_
     * @param source _more_
     * @param baseAnav _more_
     * @param descriptor _more_
     * @param sample _more_
     * @param aAdapter _more_
     */
    public AddeImageDataSelection(AddeImageDataSource dataSource,
                                  DataChoice dc, String source,
                                  AREAnav baseAnav,
                                  AddeImageDescriptor descriptor,
                                  MapProjection sample,
                                  AreaAdapter aAdapter) {
        this.dataSource       = dataSource;
        this.source           = source;
        this.descriptor       = descriptor;
        this.baseAnav         = baseAnav;
        this.dataChoice       = dc;
        this.sampleProjection = sample;
        this.aAdapter         = aAdapter;


        //prograssiveCbx  = new JCheckBox("", usePR);

        try {
            if(this.aAdapter == null && this.source != null){
                this.aAdapter = new AreaAdapter(this.source, false);
            }
            this.regionPanel = new AddeImagePreviewPanel(this);
        } catch (Exception e) {}


        try {
            this.advancedPanel = new AddeImageAdvancedPanel(this);
        } catch (Exception e) {}

        //regionPanel.display.setUseProgressiveResolution(usePR);
        /*prograssiveCbx1 = regionPanel.display.getPrograssiveCbx();
        prograssiveCbx1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean showMagSection =
                    !((JCheckBox) e.getSource()).isSelected();
                GuiUtils.enablePanel(leMagPanel, showMagSection);
            }
        });   */
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsProgressiveResolution() {
        boolean usePR = false;
        if (dataSource.getIdv().getViewManager() instanceof MapViewManager) {
            MapViewManager mvm =
                    (MapViewManager) dataSource.getIdv().getViewManager();
            usePR = mvm.getUseProgressiveResolution();
        }
        return usePR;
        //return prograssiveCbx1.isSelected();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public AddeImageAdvancedPanel getAdvancedPanel() {
        return advancedPanel;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public AddeImagePreviewPanel getRegionPanel() {
        return regionPanel;
    }

    /**
     * _more_
     *
     * @param choice _more_
     */
    public void setDataChoice(DataChoice choice) {
        this.dataChoice = choice;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public DataChoice getDataChoice() {
        return this.dataChoice;
    }


    /**
     * Class description
     *
     *
     * @version        Enter version here..., Sun, Nov 24, '13
     * @author         Enter your name here...
     */
    public class AddeImageAdvancedPanel extends DataSelectionComponent {

        /** _more_ */
        public JPanel advance;

        /** _more_ */
        private JPanel MasterPanel;

        /** _more_ */
        private JCheckBox chkUseFull;

        /** _more_ */
        private Hashtable propToComps = new Hashtable();

        /** The spacing used in the grid layout */
        protected int GRID_SPACING = 3;

        /** Used by derived classes when they do a GuiUtils.doLayout */
        protected Insets GRID_INSETS = new Insets(GRID_SPACING, GRID_SPACING,
                                           GRID_SPACING, GRID_SPACING);

        /** _more_ */
        protected JSlider lineMagSlider;

        /** _more_ */
        protected JSlider elementMagSlider;

        /** _more_ */
        JLabel lineMagLbl = new JLabel();

        /** _more_ */
        private static final int SLIDER_MAX = 29;


        /** _more_ */
        boolean amSettingProperties = false;

        /** _more_ */
        private int lineMag1;

        /** _more_ */
        private int elementMag1;

        /** _more_ */
        private String kmLbl = " km";

        /** _more_ */
        JLabel elementMagLbl = new JLabel();



        /** earth coordinates */
        protected static final String TYPE_LATLON = "Latitude/Longitude";

        /** area */
        protected static final String TYPE_AREA = "Area Coordinates";

        /** _more_ */
        String[] coordinateTypes = { TYPE_LATLON, TYPE_AREA };

        /** _more_ */
        String[] locations = { "Center", "Upper Left" };

        /** _more_ */
        JComboBox coordinateTypeComboBox;

        /** _more_ */
        JComboBox locationComboBox;

        /** Input for lat/lon center point */
        protected LatLonWidget latLonWidget = new LatLonWidget();

        /** Widget to hold the number of elements in the advanced */
        JTextField numElementsFld = new JTextField();

        /** Widget to hold  the number of lines   in the advanced */
        JTextField numLinesFld = new JTextField();

        /** Widget for the line  center point in the advanced section */
        JTextField centerLineFld = new JTextField();

        /** Widget for the element  center point in the advanced section */
        JTextField centerElementFld = new JTextField();

        /** _more_ */
        private int defaultNumLines = 1000;

        /** _more_ */
        private int defaultNumEles = 1000;

        /** _more_ */
        private int numLines = defaultNumLines;

        /** _more_ */
        private int numEles = defaultNumEles;

        /** _more_ */
        private double latitude1;

        /** _more_ */
        private double defaultLat = Double.NaN;

        /** _more_ */
        private double longitude1;

        /** _more_ */
        private double defaultLon = Double.NaN;


        /** _more_ */
        private int imageLine;

        /** _more_ */
        private int areaLine1;

        /** _more_ */
        private int areaElement1;

        /** _more_ */
        private int imageElement;

        /** _more_ */
        private String defaultType = TYPE_LATLON;

        /** _more_ */
        private JPanel lockPanel;

        /** _more_ */
        private JPanel latLonPanel;

        /** _more_ */
        private JPanel lineElementPanel;

        /** _more_ */
        private JLabel centerLatLbl = new JLabel();

        /** _more_ */
        private JLabel centerLonLbl = new JLabel();

        /** _more_ */
        private JLabel centerLineLbl = new JLabel();

        /** _more_ */
        private JLabel centerElementLbl = new JLabel();

        /** _more_ */
        protected GuiUtils.CardLayoutPanel locationPanel;

        /** _more_ */


        /** _more_ */
        private boolean isLineEle = false;

        /** _more_ */
        JPanel sizePanel;

        /** _more_ */
        boolean isFromRegionUpdate = false;

        /** _more_ */
        private double linesToElements = 1.0;

        /** base number of lines of whole image or selected region */
        private double baseNumLines = 0.0;

        /** base number of lines of whole image or selected region */
        private double baseNumElements = 0.0;

        /** _more_ */
        protected boolean amUpdating = false;

        /** _more_ */
        private String place1;

        /** _more_ */
        private int maxLines = 0;

        /** _more_ */
        private int maxEles = 0;

        /** _more_ */
        private AreaDirectory previewDir;

        /** _more_ */
        protected static final String PLACE_CENTER = "CENTER";

        /** _more_ */
        protected static final String PLACE_ULEFT = "ULEFT";

        /** _more_ */
        private String defaultPlace = PLACE_CENTER;

        /** _more_ */
        JLabel sizeLbl;

        /** _more_ */
        private JToggleButton linkBtn;

        /** _more_ */
        private JButton fullResBtn;

        /** _more_ */
        JLabel rawSizeLbl = new JLabel();

        /** _more_ */
        AddeImageDataSelection addeImageDataSelection;

        /** _more_ */
        String previousPlace;

        /** _more_ */
        ImageDataSelectionInfo urlInfo;

        /** _more_ */
        String coordinateType;

        /** _more_ */
        String navType;

        /** Widget for selecting image nav type */
        protected JComboBox navComboBox;

        /**
         * Construct a AddeImageAdvancedPanel
         *
         *
         *
         * @param addeImageDataSelection _more_
         * @throws IOException _more_
         * @throws ParseException _more_
         * @throws VisADException _more_
         */
        public AddeImageAdvancedPanel(
                AddeImageDataSelection addeImageDataSelection)
                throws IOException, ParseException, VisADException {

            super("Advanced");

            this.addeImageDataSelection = addeImageDataSelection;
            urlInfo = new ImageDataSelectionInfo(source);

            /*String magVal = AddeImageDataSource.getKey(source,
                                AddeImageURL.KEY_MAG);
            String[] magVals = magVal.split(" ");
            this.elementMag = new Integer(magVals[1]).intValue();
            this.lineMag    = new Integer(magVals[0]).intValue();
            */

            // init information for the location and the default is LATLON
            AreaDirectory aDir = descriptor.getDirectory();

            this.isLineEle = true;
            //this.place = urlInfo.getPlaceValue();
            this.coordinateType = urlInfo.getLocateKey();
            this.navType = urlInfo.getNavType();
            if(navType.equals("LALO")){
                this.coordinateType = AddeImageURL.KEY_LINEELE;
                centerLineFld.setText(Integer.toString(0));
                centerElementFld.setText(Integer.toString(0));
                setLine(0);
                setElement(0);
                urlInfo.setPlaceValue("ULEFT");
                urlInfo.setLocationLine(0);
                urlInfo.setLocationElem(0);
                convertToLatLon();
                urlInfo.setLocationLat(getLatitude());
                urlInfo.setLocationLon(getLongitude());
            } else if(coordinateType.equals(AddeImageURL.KEY_LATLON)){
                //this.latitude = urlInfo.getLocationLat();
                //this.longitude = urlInfo.getLocationLon();
                latLonWidget.setLat(urlInfo.getLocationLat());
                latLonWidget.setLon(urlInfo.getLocationLon());
                convertToLineEle();
                urlInfo.setLocationElem(getElement());
                urlInfo.setLocationLine(getLine());
            } else {
                //this.areaLine = urlInfo.getLocationLine();
                //this.areaElement = urlInfo.getLocationElem();
                centerLineFld.setText(Integer.toString(urlInfo.getLocationLine()));
                centerElementFld.setText(Integer.toString(urlInfo.getLocationElem()));
                convertToLatLon();
                urlInfo.setLocationLat(getLatitude());
                urlInfo.setLocationLon(getLongitude());
            }
         /*   double cLat = aDir.getCenterLatitude();
            double cLon = aDir.getCenterLongitude();
            setLatitude(cLat);
            setLongitude(cLon);
            convertToLineEle();    */

            //
            this.previewDir      = aDir;
            this.baseNumLines    = aDir.getLines();
            this.baseNumElements = aDir.getElements();

            //this.place = AddeImageDataSource.getKey(source,
            //        AddeImageURL.KEY_PLACE);

        }

        /**
         * _more_
         */
        public void reset() {
            // init information for the magnification
            urlInfo = new ImageDataSelectionInfo(source);
            String magVal = AddeImageDataSource.getKey(source,
                                AddeImageURL.KEY_MAG);
            String[] magVals = magVal.split(" ");
            /*this.elementMag = new Integer(magVals[1]).intValue();
            this.lineMag    = new Integer(magVals[0]).intValue();  */
            setLineMagSlider(urlInfo.getLineMag());
            setElementMagSlider(urlInfo.getElementMag());

            // init information for the location and the default is LATLON
            AreaDirectory aDir = descriptor.getDirectory();
            this.coordinateType = urlInfo.getLocateKey();
            this.navType = urlInfo.getNavType();

            if(coordinateType.equals(AddeImageURL.KEY_LATLON)){
                //this.latitude = urlInfo.getLocationLat();
                //this.longitude = urlInfo.getLocationLon();
                latLonWidget.setLat(urlInfo.getLocationLat());
                latLonWidget.setLon(urlInfo.getLocationLon());
                convertToLineEle();
                urlInfo.setLocationElem(getElement());
                urlInfo.setLocationLine(getLine());
            } else {
                //this.areaLine = urlInfo.getLocationLine();
                //this.areaElement = urlInfo.getLocationElem();
                centerLineFld.setText(Integer.toString(urlInfo.getLocationLine()));
                centerElementFld.setText(Integer.toString(urlInfo.getLocationElem()));
                convertToLatLon();
                urlInfo.setLocationLat(getLatitude());
                urlInfo.setLocationLon(getLongitude());
            }

            //
            this.previewDir      = aDir;
            this.baseNumLines    = aDir.getLines();
            this.baseNumElements = aDir.getElements();
            int lines = (int) (baseNumLines / Math.abs(urlInfo.getLineMag()));
            setNumLines(lines);
            int elems = (int) (baseNumElements / Math.abs(urlInfo.getElementMag()));
            setNumEles(elems);

            //this.place = AddeImageDataSource.getKey(source,
            //        AddeImageURL.KEY_PLACE);
            setPlace(urlInfo.getPlaceValue());

        }

        /**
         * _more_
         *
         * @param lines _more_
         */
        public void setBaseNumLines(int lines) {
            this.baseNumLines = lines;
        }

        /**
         * _more_
         *
         * @param eles _more_
         */
        public void setBaseNumElements(int eles) {
            this.baseNumElements = eles;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getIsFromRegionUpdate() {
            return isFromRegionUpdate;
        }

        /**
         * _more_
         *
         * @param isRegion _more_
         */
        public void setIsFromRegionUpdate(boolean isRegion) {
            isFromRegionUpdate = isRegion;
        }

        /**
         * this updates the previewSelection selected region, only lat/lon
         * or line/ele location changes and the place change being counted in
         * this update api
         */
        public void updatePlace() {

            LatLonRect llr      = null;
            String     newPlace = getPlace();


            NavigatedPanel navigatedPanel =
                regionPanel.display.getNavigatedPanel();
            ProjectionRect rect = navigatedPanel.getSelectedRegion();
            if (rect == null) {
                return;
            }
            ProjectionImpl projectionImpl =
                regionPanel.display.getProjectionImpl();
            ProjectionRect newRect = new ProjectionRect();
            newRect.setHeight(rect.getHeight());
            newRect.setWidth(rect.getWidth());
            LatLonRect latLonRectOld =
                projectionImpl.getLatLonBoundingBox(rect);

            if (newPlace.equals("CENTER")) {
                //move llr from ULEFT to CENTER
                double          lat = getLatitude();
                double          lon = getLongitude();
                LatLonPointImpl ll0 = new LatLonPointImpl(lat, lon);
                ProjectionPoint pp0 = projectionImpl.latLonToProj(ll0);

                double          x   = pp0.getX() - rect.getWidth() / 2;
                double          y   = pp0.getY() - rect.getHeight() / 2;

                newRect.setX(x);
                newRect.setY(y);

            } else {  //move llr from CENTER to ULEFT
                double          lat = getLatitude();
                double          lon = getLongitude();
                LatLonPointImpl ll0 = new LatLonPointImpl(lat, lon);
                ProjectionPoint pp0 = projectionImpl.latLonToProj(ll0);

                double          x   = pp0.getX();
                double          y   = pp0.getY() - rect.getHeight();

                newRect.setX(x);
                newRect.setY(y);
            }
            navigatedPanel.setSelectedRegion(newRect);

            //System.out.println("here");
        }


        /**
         * this updates the previewSelection selected region, only the image
         * size changes being counted in this update api
         */

        public void updateImageWidthSize() {
            NavigatedPanel navigatedPanel =
                regionPanel.display.getNavigatedPanel();
            ProjectionRect rect = navigatedPanel.getSelectedRegion();
            if (rect == null) {
                return;
            }
            ProjectionRect newRect = new ProjectionRect();
            newRect.setX(rect.getX());
            newRect.setY(rect.getY());
            newRect.setHeight(rect.getHeight());

            // calc the new width and height using the value from image size widget
            int newNumEles = getNumEles();
            int mag0       = getElementMagValue();
            newNumEles *= Math.abs(mag0);
            double newWidth = rect.getWidth()
                              * (newNumEles * 1.0 / this.baseNumElements);
            newRect.setWidth(newWidth);

            navigatedPanel.setSelectedRegion(newRect);
            //dataSource.previewSelection.setPreNumEles(newNumEles);
            updatePlace();
        }

        /**
         * _more_
         */
        public void updateImageHeightSize() {
            NavigatedPanel navigatedPanel =
                regionPanel.display.getNavigatedPanel();
            ProjectionRect rect = navigatedPanel.getSelectedRegion();
            if (rect == null) {
                return;
            }
            ProjectionRect newRect = new ProjectionRect();
            newRect.setX(rect.getX());
            newRect.setY(rect.getY());
            newRect.setWidth(rect.getWidth());

            // calc the new width and height using the value from image size widget
            int newNumLines = getNumLines();
            //int  mag = getLineMag();
            int mag0 = getLineMagValue();
            newNumLines *= Math.abs(mag0);
            double newHeight = rect.getHeight()
                               * (newNumLines * 1.0 / this.baseNumLines);
            newRect.setHeight(newHeight);

            navigatedPanel.setSelectedRegion(newRect);
            //dataSource.previewSelection.setPreNumLines(newNumLines);
            this.baseNumLines = newNumLines;
            updatePlace();
        }

        /**
         * _more_
         *
         * @param enable _more_
         */
        public void enablePanelAll(boolean enable) {
            if (coordinateTypeComboBox == null) {
                return;
            }
            coordinateTypeComboBox.setEnabled(enable);
            locationComboBox.setEnabled(enable);
            GuiUtils.enablePanel(locationPanel, enable);
            GuiUtils.enablePanel(lockPanel, enable);
            GuiUtils.enablePanel(leMagPanel, enable);
            GuiUtils.enablePanel(sizePanel, enable);
        }


        /**
         * _more_
         *
         * @return _more_
         */
        protected String getUrl() {
            String str = source;
            str = str.replaceFirst("imagedata", "text");
            int indx = str.indexOf("VERSION");
            str = str.substring(0, indx);
            str = str.concat("file=SATBAND");
            return str;
        }

        /**
         * _more_
         *
         * @param url _more_
         *
         * @return _more_
         */
        protected java.util.List readTextLines(String url) {
            AddeTextReader reader = new AddeTextReader(url);
            java.util.List lines  = null;
            if ("OK".equals(reader.getStatus())) {
                lines = reader.getLinesOfText();
            }
            return lines;
        }

        /**
         * _more_
         *
         * @param ad _more_
         *
         * @return _more_
         */
        private float[] getLineEleResolution(AreaDirectory ad) {

            float[]        res    = { (float) 1.0, (float) 1.0 };
            int            sensor = ad.getSensorID();
            java.util.List lines  = null;
            try {
                String buff = getUrl();

                lines = readTextLines(buff);
                if (lines == null) {
                    return res;
                }

                int      gotit = -1;
                String[] cards = StringUtil.listToStringArray(lines);


                for (int i = 0; i < cards.length; i++) {
                    if ( !cards[i].startsWith("Sat ")) {
                        continue;
                    }
                    StringTokenizer st = new StringTokenizer(cards[i], " ");
                    String temp        = st.nextToken();  // throw away the key
                    int             m  = st.countTokens();
                    for (int k = 0; k < m; k++) {
                        int ss = Integer.parseInt(st.nextToken().trim());
                        if (ss == sensor) {
                            gotit = i;
                            break;
                        }
                    }

                    if (gotit != -1) {
                        break;
                    }
                }

                if (gotit == -1) {
                    return res;
                }

                int gotSrc = -1;
                for (int i = gotit; i < cards.length; i++) {
                    if (cards[i].startsWith("EndSat")) {
                        return res;
                    }
                    if ( !cards[i].startsWith("B")) {
                        continue;
                    }
                    StringTokenizer tok = new StringTokenizer(cards[i]);
                    String          str = tok.nextToken();
                    str = tok.nextToken();
                    Float flt = new Float(str);
                    res[0] = flt.floatValue();
                    str    = tok.nextToken();
                    flt    = new Float(str);
                    res[1] = flt.floatValue();
                    return res;
                }
            } catch (Exception e) {}
            return res;
        }



        /**
         * _more_
         *
         * @param propId _more_
         * @param comp _more_
         *
         * @return _more_
         */
        protected JComponent addPropComp(String propId, JComponent comp) {
            Object oldComp = propToComps.get(propId);
            if (oldComp != null) {
                throw new IllegalStateException(
                    "Already have a component defined:" + propId);
            }
            propToComps.put(propId, comp);
            return comp;
        }


        /**
         * _more_
         *
         * @param autoSetSize _more_
         */
        protected void elementMagSliderChanged(boolean autoSetSize) {
            int value = getElementMagValue();
            if ((Math.abs(value) < SLIDER_MAX)) {
                int lineMag = getLineMagValue();
                if (lineMag > value) {
                    linesToElements = Math.abs(lineMag / (double) value);
                } else {
                    linesToElements = Math.abs((double) value / lineMag);
                }
            }

            elementMagLbl.setText(StringUtil.padLeft("" + value, 3));

            if (autoSetSize) {
                if (value > 0) {
                    numElementsFld.setText("" + (int) (baseNumElements
                            * value));
                } else {
                    numElementsFld.setText("" + (int) (baseNumElements
                            / (double) -value));
                }
            }

            int elems = getNumEles();
            urlInfo.setElements(elems);
            regionPanel.setElemMag(value);
        }


        /**
         * Handle the line mag slider changed event
         *
         * @param autoSetSize  the event
         */
        protected void lineMagSliderChanged(boolean autoSetSize) {
            try {
                int value = getLineMagValue();
                urlInfo.setLineMag(value);
                //setLineMag(value);
                lineMagLbl.setText(StringUtil.padLeft("" + value, 3));

                if (autoSetSize) {
                    if (value > 0) {
                        numLinesFld.setText("" + (int) (baseNumLines
                                * value));
                    } else {
                        numLinesFld.setText("" + (int) (baseNumLines
                                / (double) -value));
                    }
                }

                int lines = getNumLines();
                urlInfo.setLines(lines);
                regionPanel.setLineMag(value);
                if (value == 1) {                   // special case
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

                value               = (value > 0)
                                      ? value - 1
                                      : value + 1;  // since slider is one off
                amSettingProperties = true;
                elementMagSlider.setValue(value);
                urlInfo.setElementMag(getElementMagValue());
                amSettingProperties = false;
                elementMagSliderChanged(autoSetSize);

            } catch (Exception exc) {
                System.out.println("Setting line magnification" + exc);
            }
        }

        /**
         * _more_
         *
         * @param value _more_
         */
        public void setElementMagSlider(int value) {
            if(this.elementMagSlider == null && urlInfo != null){
                urlInfo.setElementMag(value);
                return;
            }
            this.elementMagSlider.setValue(value);
            this.elementMagLbl.setText(StringUtil.padLeft("" + value, 3));
            urlInfo.setElementMag(getElementMagValue());
        }

        /**
         * _more_
         *
         * @param value _more_
         */
        public void setLineMagSlider(int value) {
            if(this.lineMagSlider == null && urlInfo != null){
                urlInfo.setLineMag(value);
                return;
            }
            this.lineMagSlider.setValue(value);
            this.lineMagLbl.setText(StringUtil.padLeft("" + value, 3));
            urlInfo.setLineMag(getLineMagValue());
        }

        /**
         * Get the value of the line magnification slider.
         *
         * @return The magnification value for the line
         */
        protected int getLineMagValue() {
            if (lineMagSlider == null) {
                return urlInfo.getLineMag();
            }
            return getMagValue(lineMagSlider);
            //return lineMagSlider.getValue();
        }

        /**
         * Get the value of the element magnification slider.
         *
         * @return The magnification value for the element
         */
        protected int getElementMagValue() {
            if (elementMagSlider == null) {
                return urlInfo.getElementMag();
            }
            return getMagValue(elementMagSlider);
            //return elementMagSlider.getValue();
        }

        /**
         * Get the value of the given  magnification slider.
         *
         * @param slider The slider to get the value from
         * @return The magnification value
         */
        private int getMagValue(JSlider slider) {
            //Value is [-SLIDER_MAX,SLIDER_MAX]. We change 0 and -1 to 1
            int value = slider.getValue();
            if (value >= 0) {
                return value + 1;
            }
            return value -1;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public void updateMagPanel(){
            GuiUtils.enablePanel(leMagPanel,
                    !getIsProgressiveResolution());
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public String getFileName() {
            return source;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        protected JComponent doMakeContents() {

            java.util.List allComps0       = new ArrayList();
            java.util.List allComps1       = new ArrayList();
            java.util.List allComps2       = new ArrayList();
            java.util.List allComps3       = new ArrayList();
            //java.util.List allComps4       = new ArrayList();
            Insets         dfltGridSpacing = new Insets(4, 0, 4, 0);
            String         dfltLblSpacing  = " ";
            JComponent     propComp        = null;

            //allComps0.add(GuiUtils.rLabel("Progressive Resolution:"));
            //allComps0.add(GuiUtils.left(prograssiveCbx));

            // coordinate types
            //allComps1.add(new JLabel(" "));
            //allComps1.add(new JLabel(" "));

            coordinateTypeComboBox = new JComboBox(coordinateTypes);
            coordinateTypeComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    int selectedIndex =
                        coordinateTypeComboBox.getSelectedIndex();
                    flipLocationPanel(selectedIndex);
                    if(selectedIndex == 0){
                        urlInfo.setLocateKey(AddeImageURL.KEY_LATLON);
                    } else {
                        urlInfo.setLocateKey(AddeImageURL.KEY_LINEELE);
                    }
                }
            });

            allComps1.add(GuiUtils.rLabel(" Coordinates: "));
            allComps1.add(GuiUtils.left(coordinateTypeComboBox));

            // location
            allComps1.add(new JLabel(" "));
            allComps1.add(new JLabel(" "));

            ActionListener placeChange = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    if (previousPlace == null) {
                        previousPlace = getPlace();
                        return;
                    }
                    if ( !previousPlace.contains(getPlace())) {
                        previousPlace = getPlace();
                        updatePlace();
                        // System.out.print(getPlace() + "uu");
                    }
                }
            };

            locationComboBox = new JComboBox(locations);
            locationComboBox.addActionListener(placeChange);
            setPlace(urlInfo.getPlaceValue());


            allComps1.add(GuiUtils.rLabel(" Location: "));
            allComps1.add(GuiUtils.left(locationComboBox));

            ActionListener latLonChange = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    String type = getCoordinateType();
                    if (type.equals(TYPE_LATLON)) {
                        setLatitude();
                        setLongitude();
                        convertToLineEle();
                        updatePlace();
                    } else {
                        setLineElement();
                        convertToLatLon();
                        updatePlace();
                    }
                }
            };

            FocusListener linEleFocusChange = new FocusListener() {
                public void focusGained(FocusEvent fe) {}
                public void focusLost(FocusEvent fe) {
                    setLineElement();
                    convertToLatLon();
                    updatePlace();
                }
            };

            if (latLonWidget == null) {
                latLonWidget = new LatLonWidget(latLonChange);
            }

            FocusListener latLonFocusChange = new FocusListener() {
                public void focusGained(FocusEvent fe) {
                    JTextField latFld = latLonWidget.getLatField();
                    latFld.setCaretPosition(latFld.getText().length());
                    JTextField lonFld = latLonWidget.getLonField();
                    lonFld.setCaretPosition(lonFld.getText().length());
                }
                public void focusLost(FocusEvent fe) {
                    setLatitude();
                    setLongitude();
                    convertToLineEle();
                    updatePlace();
                }
            };
            if ( !this.isLineEle) {
                latLonWidget.setLatLon(urlInfo.getLocationLat(), urlInfo.getLocationLon());
            }
            String lineStr = "";
            String eleStr  = "";
            centerLineFld = new JTextField(lineStr, 3);
            centerLineFld.addActionListener(latLonChange);
            centerLineFld.addFocusListener(linEleFocusChange);

            centerElementFld = new JTextField(eleStr, 3);
            centerElementFld.addActionListener(latLonChange);
            centerElementFld.addFocusListener(linEleFocusChange);
            final JButton centerPopupBtn =
                GuiUtils.getImageButton("/auxdata/ui/icons/MapIcon16.png",
                                        getClass());
            centerPopupBtn.setToolTipText("Center on current displays");

            centerPopupBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    dataSource.getDataContext().getIdv().getIdvUIManager()
                        .popupCenterMenu(centerPopupBtn, latLonWidget);
                }
            });

            JComponent centerPopup = GuiUtils.inset(centerPopupBtn,
                                         new Insets(0, 0, 0, 4));

            JTextField latFld = latLonWidget.getLatField();
            JTextField lonFld = latLonWidget.getLonField();
            latFld.addFocusListener(latLonFocusChange);
            lonFld.addFocusListener(latLonFocusChange);
            latLonPanel = GuiUtils.hbox(new Component[] {
                centerLatLbl = GuiUtils.rLabel(" Lat:" + dfltLblSpacing),
                latFld,
                centerLonLbl = GuiUtils.rLabel(" Lon:" + dfltLblSpacing),
                lonFld, new JLabel(" "), centerPopup
            });

            lineElementPanel = GuiUtils.hbox(new Component[] {
                centerLineLbl = GuiUtils.rLabel(" Line:" + dfltLblSpacing),
                centerLineFld,
                centerElementLbl = GuiUtils.rLabel(" Element:"
                    + dfltLblSpacing),
                centerElementFld });

            locationPanel = new GuiUtils.CardLayoutPanel();
            locationPanel.addCard(latLonPanel);
            locationPanel.addCard(lineElementPanel);

            allComps1.add(GuiUtils.rLabel("  "));
            allComps1.add(GuiUtils.left(locationPanel));

            // image size and mag factor link
            allComps1.add(new JLabel(" "));
            allComps1.add(new JLabel(" "));

            lockPanel = GuiUtils.left(GuiUtils.doLayout(new Component[] {
                new JLabel(" "),
                getLinkButton() }, 2, GuiUtils.WT_N, GuiUtils.WT_N));
            allComps2.add(
                GuiUtils.rLabel("Link Image Size And Magnification:"));
            allComps2.add(GuiUtils.left(lockPanel));


            // image size
            allComps3.add(new JLabel(" "));
            allComps3.add(new JLabel(" "));
            ActionListener lSizeChange = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    int lines = getNumLines() * Math.abs(getLineMagValue());
                    if (lines > maxLines) {
                        lines = maxLines;
                    }
                    updateImageHeightSize();
                }
            };
            ActionListener eSizeChange = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    int eles = getNumEles() * Math.abs(getElementMagValue());
                    if (eles > maxEles) {
                        eles = maxEles;
                    }
                    updateImageWidthSize();
                }
            };
            FocusListener lSizeFocusChange = new FocusListener() {
                public void focusGained(FocusEvent fe) {}
                public void focusLost(FocusEvent fe) {
                    int lines = getNumLines() * Math.abs(getLineMagValue());
                    if (lines > maxLines) {
                        lines = maxLines;
                    }
                    updateImageHeightSize();
                }
            };
            FocusListener eSizeFocusChange = new FocusListener() {
                public void focusGained(FocusEvent fe) {}
                public void focusLost(FocusEvent fe) {
                    int eles = getNumEles() * Math.abs(getElementMagValue());
                    if (eles > maxEles) {
                        eles = maxEles;
                    }
                    updateImageWidthSize();
                }
            };
            this.maxLines = this.previewDir.getLines();
            this.maxEles  = this.previewDir.getElements();

            int lmag = getLineMagValue();
            int emag = getElementMagValue();
            if (lmag < 0) {
                this.numLines = this.maxLines / Math.abs(lmag);
            }
            if (emag < 0) {
                this.numEles = this.maxEles / Math.abs(emag);
            }

            setNumLines(this.numLines);
            numLinesFld = new JTextField(Integer.toString(this.numLines), 4);
            numLinesFld.addActionListener(lSizeChange);
            numLinesFld.addFocusListener(lSizeFocusChange);
            setNumEles(this.numEles);
            numElementsFld = new JTextField(Integer.toString(this.numEles),
                                            4);
            numElementsFld.addActionListener(eSizeChange);
            numElementsFld.addFocusListener(eSizeFocusChange);
            numLinesFld.setToolTipText("Number of lines");
            numElementsFld.setToolTipText("Number of elements");
            GuiUtils.tmpInsets = dfltGridSpacing;
            sizeLbl            = GuiUtils.lLabel("");

            fullResBtn =
                GuiUtils.makeImageButton("/auxdata/ui/icons/arrow_out.png",
                                         this, "setToFullResolution",
                                         new Boolean(true));
            fullResBtn.setContentAreaFilled(false);
            fullResBtn.setToolTipText("Set fields to retrieve full image");

            rawSizeLbl = new JLabel(" Raw size: " + this.maxLines + " X "
                                    + this.maxEles);
            sizePanel = GuiUtils.left(GuiUtils.doLayout(new Component[] {
                numLinesFld, new JLabel(" X "), numElementsFld, sizeLbl,
                new JLabel(" "), fullResBtn, new JLabel("  "), rawSizeLbl
            }, 8, GuiUtils.WT_N, GuiUtils.WT_N));

            allComps3.add(GuiUtils.rLabel("Image Size:"));
            allComps3.add(GuiUtils.left(sizePanel));

            // Magnification
            allComps3.add(new JLabel(" "));
            allComps3.add(new JLabel(" "));
            //line mag
            boolean oldAmSettingProperties = amSettingProperties;
            amSettingProperties = true;
            ChangeListener lineListener = new ChangeListener() {
                public void stateChanged(ChangeEvent evt) {
                    if (amSettingProperties) {
                        return;
                    }
                    lineMagSliderChanged(getLinkButton().isSelected());

                }
            };

            ChangeListener elementListener = new ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent evt) {
                    if (amSettingProperties) {
                        return;
                    }
                    elementMagSliderChanged(getLinkButton().isSelected());
                }
            };

            JComponent[] lineMagComps = GuiUtils.makeSliderPopup(-SLIDER_MAX,
                                            1, 0, lineListener);
            lineMagSlider = (JSlider) lineMagComps[1];
            lineMagSlider.setMajorTickSpacing(1);
            lineMagSlider.setSnapToTicks(true);
            lineMagSlider.setExtent(1);
            if(lmag > 0)
                lineMagSlider.setValue(lmag - 1);
            else
                lineMagSlider.setValue(lmag + 1);
            //lineMagSlider.setValue(lmag + 1);
            lineMagComps[0].setToolTipText("Change the line magnification");
            JComponent[] elementMagComps =
                GuiUtils.makeSliderPopup(-SLIDER_MAX, 1, 0, elementListener);
            elementMagSlider = (JSlider) elementMagComps[1];
            elementMagSlider.setExtent(1);
            elementMagSlider.setMajorTickSpacing(1);
            elementMagSlider.setSnapToTicks(true);
            if(emag > 0)
                elementMagSlider.setValue(emag - 1);
            else
                elementMagSlider.setValue(emag + 1);
            elementMagComps[0].setToolTipText(
                "Change the element magnification");
            lineMagSlider.setToolTipText(
                "Slide to set line magnification factor");
            lineMagLbl = GuiUtils.getFixedWidthLabel(
                StringUtil.padLeft(String.valueOf(lmag), 3));
            elementMagSlider.setToolTipText(
                "Slide to set element magnification factor");
            elementMagLbl = GuiUtils.getFixedWidthLabel(
                StringUtil.padLeft(String.valueOf(emag), 3));
            amSettingProperties = oldAmSettingProperties;


            GuiUtils.tmpInsets  = new Insets(0, 0, 0, 0);
            leMagPanel = GuiUtils.left(GuiUtils.doLayout(new Component[] {
                lineMagLbl,
                GuiUtils.inset(lineMagComps[0], new Insets(0, 4, 0, 0)),
                new JLabel("    X"), elementMagLbl,
                GuiUtils.inset(elementMagComps[0],
                               new Insets(0, 4, 0, 0)) }, 6, GuiUtils.WT_N,
                                   GuiUtils.WT_N));

            propComp = GuiUtils.hbox(new Component[] { leMagPanel }, 1);
            // allComps.add(GuiUtils.left(propComp));
            // allComps.add(GuiUtils.lLabel(" "));
            allComps3.add(GuiUtils.rLabel("Magnification:"));
            allComps3.add(GuiUtils.left(propComp));

            //Navigator Type
       /*     allComps4.add(new JLabel(" "));
            allComps4.add(new JLabel(" "));
            navComboBox = new JComboBox();
            GuiUtils.setListData(
                    navComboBox,
                    Misc.newList(
                            new TwoFacedObject("Default", "X"),
                            new TwoFacedObject("Lat/Lon", "LALO")));
            navComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    int selectedIndex =
                            navComboBox.getSelectedIndex();

                    if(selectedIndex == 0){
                        urlInfo.setNavType("X");
                    } else {
                        urlInfo.setNavType("LALO");
                    }
                }
            });
            allComps4.add(GuiUtils.rLabel("Navigation Type:"));
            allComps4.add(GuiUtils.left(navComboBox));    */

            //all
            JPanel imagePanel =
                GuiUtils.vbox(new Component []
                        {GuiUtils.doLayout(allComps0, 2, GuiUtils.WT_NY, GuiUtils.WT_N),
                         GuiUtils.doLayout(allComps1, 2, GuiUtils.WT_NY, GuiUtils.WT_N),
                         GuiUtils.doLayout(allComps2, 2, GuiUtils.WT_NY, GuiUtils.WT_N),
                         GuiUtils.doLayout(allComps3, 2, GuiUtils.WT_NY, GuiUtils.WT_N)});
                       //  GuiUtils.doLayout(allComps4, 2, GuiUtils.WT_NY, GuiUtils.WT_N)});

            advance = GuiUtils.top(imagePanel);
            boolean showMagSection = !getIsProgressiveResolution(); //prograssiveCbx1.isSelected();
            GuiUtils.enablePanel(leMagPanel, showMagSection);
            GuiUtils.enablePanel(sizePanel, showMagSection);
            String s0 = AddeImageDataSource.getKey(source,
                            AddeImageURL.KEY_LINEELE);
            if ((s0 != null) && (s0.length() > 1)) {
                coordinateTypeComboBox.setSelectedIndex(1);
            }
            chkUseFull = new JCheckBox("Use Default");

            chkUseFull.setSelected(true);

            JScrollPane jsp = new JScrollPane();
            jsp.getViewport().setView(advance);
            JPanel labelsPanel = null;
            labelsPanel = new JPanel();
            labelsPanel.setLayout(new BoxLayout(labelsPanel, 1));


            MasterPanel = new JPanel(new java.awt.BorderLayout());
            MasterPanel.add(labelsPanel, "North");
            MasterPanel.add(jsp, "Center");

            if (regionPanel != null) {
                String opStr = regionPanel.getRegionOptions();

                if (opStr.equals("Use Selected")) {
                    enablePanelAll(true);
                } else {
                    enablePanelAll(false);
                }

                /*if ( !prograssiveCbx1.isSelected()) {
                    GuiUtils.enablePanel(leMagPanel,
                                         !prograssiveCbx1.isSelected());
                }  */
                GuiUtils.enablePanel(leMagPanel,
                        !getIsProgressiveResolution());
            }


            return MasterPanel;

        }

        /**
         * Get the "lock" button
         *
         * @return  the lock button
         */
        private JToggleButton getLinkButton() {
            if (linkBtn == null) {
                linkBtn = GuiUtils.getToggleImageButton(
                    "/auxdata/ui/icons/link_break.png",
                    "/auxdata/ui/icons/link.png", 0, 0, true);
                linkBtn.setContentAreaFilled(false);
                linkBtn.setSelected(true);
                linkBtn.setToolTipText(
                    "Link changing image size with magnification factors");

            }
            return linkBtn;

        }



        /**
         * _more_
         *
         * @param locPanel _more_
         */
        protected void flipLocationPanel(int locPanel) {
            int nowPlaying = locationPanel.getVisibleIndex();
            if (locPanel > 0) {
                if (nowPlaying == 0) {
                    locationPanel.flip();
                }
                setIsLineEle(true);
                String type = getCoordinateType();
                int    ele  = this.imageElement;
                int    lin  = this.imageLine;
                if (type.equals(TYPE_AREA)) {
                    ele = urlInfo.getLocationElem();
                    lin = urlInfo.getLocationLine();
                }
                setElement(ele);
                setLine(lin);
            } else {
                if (nowPlaying > 0) {
                    locationPanel.flip();
                }
                setIsLineEle(false);
            }
        }

        /**
         * _more_
         *
         * @param val _more_
         */
        public void setIsLineEle(boolean val) {
            this.isLineEle = val;
        }





        /**
         * _more_
         *
         * @return _more_
         */
        public double getLatitude() {
            double val = latLonWidget.getLat();
            //        Double dbl = new Double(val);
            if (Double.isNaN(val)) {
                val = defaultLat;
            }
            if ((val < -90.0) || (val > 90.0)) {
                val = defaultLat;
            }
            setLatitude(val);
            return val;
        }

        /**
         * _more_
         */
        private void setLatitude() {
           // this.latitude = latLonWidget.getLat();
            urlInfo.setLocationLat(latLonWidget.getLat());
        }

        /**
         * _more_
         *
         * @param val _more_
         */
        public void setLatitude(double val) {
            if ((val < -90.0) || (val > 90.0)) {
                val = defaultLat;
            }
            latLonWidget.setLat(val);
            // this.latitude = val;
            urlInfo.setLocationLat(val);

        }

        /**
         * _more_
         */
        private void setLongitude() {
            //this.longitude = latLonWidget.getLon();
            urlInfo.setLocationLon(latLonWidget.getLon());
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public double getLongitude() {
            double val = latLonWidget.getLon();
            //        Double dbl = new Double(val);
            if (Double.isNaN(val)) {
                val = defaultLon;
            }
            if ((val < -180.0) || (val > 180.0)) {
                val = defaultLon;
            }
            setLongitude(val);
            return val;
        }

        /**
         * _more_
         *
         * @param val _more_
         */
        public void setLongitude(double val) {
            if ((val < -180.0) || (val > 180.0)) {
                val = defaultLon;
            }
            latLonWidget.setLon(val);
            //this.longitude = val;
            urlInfo.setLocationLon(val);
        }

        /**
         * _more_
         */
        protected void convertToLineEle() {
            double[][] ll = new double[2][1];
            ll[0][0] = getLatitude();
            ll[1][0] = getLongitude();
            AREACoordinateSystem macs =
                (AREACoordinateSystem) sampleProjection;
            double[][] el = baseAnav.toLinEle(ll);
            try {
                double[][] el1 = macs.fromReference(ll);
            } catch (Exception e) {}
            int elem = (int) Math.floor(el[0][0] + 0.5)
                               * Math.abs(getElementMagValue());
            int line = (int) Math.floor(el[1][0] + 0.5)
                            * Math.abs(getLineMagValue());
            urlInfo.setLocationLine(line);
            urlInfo.setLocationElem(elem);
             el                = baseAnav.areaCoordToImageCoord(el);
            this.imageElement = (int) Math.floor(el[0][0] + 0.5);
            this.imageLine    = (int) Math.floor(el[1][0] + 0.5);
        }

        /**
         * _more_
         */
        protected void convertToLatLon() {
            double[][] el1 = getLineElement();
            double[][] ll  = new double[2][1];

            double[][] el  = new double[2][1];
            el[0][0] = (double) (el1[0][0] / Math.abs(getElementMagValue())
                                 + 0.5);
            el[1][0] = (double) (el1[1][0] / Math.abs(getLineMagValue())
                                 + 0.5);
            if(baseAnav == null)
                return;
            try {
                //AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
                ll = baseAnav.toLatLon(el);
                setLatitude(ll[0][0]);
                setLongitude(ll[1][0]);

            } catch (Exception e) {
                System.out.println("convertToLatLon e=" + e);
            }
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getCoordinateType() {
            String ret = defaultType;
            try {
                ret = (String) coordinateTypeComboBox.getSelectedItem();
            } catch (Exception e) {}
            return ret;
        }



        /**
         * _more_
         *
         * @return _more_
         */
        public int getNumLines() {
            int val = -1;
            try {
                val = Integer.parseInt(numLinesFld.getText().trim());
            } catch (Exception e) {
                System.out.println("=====> exception in getNumLines: e=" + e);
            }
            setNumLines(val);
            return this.numLines;
        }

        /**
         * _more_
         *
         * @param val _more_
         */
        public void setNumLines(int val) {
            this.numLines = val;
            if (val >= 0) {
                numLinesFld.setText(Integer.toString(val));
            }
            urlInfo.setLines(val);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getNumEles() {
            int val = -1;
            try {
                val = Integer.parseInt(numElementsFld.getText().trim());
            } catch (Exception e) {
                System.out.println("=====> exception in getNumEles: e=" + e);
            }
            setNumEles(val);
            return this.numEles;
        }

        /**
         * _more_
         *
         * @param val _more_
         */
        public void setNumEles(int val) {
            val          = (int) ((double) val / 4.0 + 0.5) * 4;
            this.numEles = val;
            if (val >= 0) {
                numElementsFld.setText(Integer.toString(val));
            }
            urlInfo.setElements(val);
        }

        /**
         * _more_
         */
        protected void setLineElement() {
            double[][] el = getLineElement();

            int elem = (int) Math.floor(el[0][0] + 0.5);
            int line = (int) Math.floor(el[1][0] + 0.5);
            urlInfo.setLocationElem(elem);
            urlInfo.setLocationLine(line);
            double[][] vals = baseAnav.areaCoordToImageCoord(el);
            this.imageElement = (int) Math.floor(vals[0][0] + 0.5);
            this.imageLine    = (int) Math.floor(vals[1][0] + 0.5);

        }



        /**
         * _more_
         *
         * @param val _more_
         */
        public void setLine(int val) {
            if (val < 0) {
                centerLineFld.setText(Misc.MISSING);
            } else {
                centerLineFld.setText(Integer.toString(val));
            }
        }

        /**
         * _more_
         *
         * @param val _more_
         */
        public void setElement(int val) {
            if (val < 0) {
                centerElementFld.setText(Misc.MISSING);
            } else {
                centerElementFld.setText(Integer.toString(val));
            }
        }


        /**
         * _more_
         *
         * @param dataSelection _more_
         */
        public void applyToDataSelection(DataSelection dataSelection) {
            GeoSelection geoSelection = dataSelection.getGeoSelection();

            dataSelection.putProperty(
                DataSelection.PROP_PROGRESSIVERESOLUTION,
                getIsProgressiveResolution());

            if (geoSelection != null) {
                if ( !getIsProgressiveResolution()) {
                    geoSelection.setXStride(
                        Math.abs(elementMagSlider.getValue()));
                    geoSelection.setYStride(
                        Math.abs(lineMagSlider.getValue()));
                }
            }

            //    if (geoSelection == null) {
            String regionOption =
                dataSelection.getProperty(DataSelection.PROP_REGIONOPTION,
                                          DataSelection.PROP_USEDEFAULTAREA);
            if (regionOption.equals(DataSelection.PROP_USESELECTEDAREA)
                    || regionOption.equals(
                        DataSelection.PROP_USEDEFAULTAREA)) {

                dataSelection.putProperty("advancedURL", urlInfo.cloneMe());
                //System.out.println(urlInfo.getURLString());
            } else if(regionOption.equals(DataSelection.PROP_USEDISPLAYAREA)){
                ViewManager vm = dataSource.getIdv().getViewManager();
                NavigatedDisplay navDisplay = ((NavigatedViewManager) vm).getNavigatedDisplay();
                Rectangle2D sbox = navDisplay.getScreenBounds();
                dataSource.getIdv().getViewManager().setProjectionFromData(false);
                try{
                Rectangle2D bbox = navDisplay.getLatLonBox();
                geoSelection.setLatLonRect(bbox);
                dataSelection.setGeoSelection(geoSelection);
                visad.georef.EarthLocation el = navDisplay.screenToEarthLocation(
                        (int) (sbox.getWidth()/2), (int)(sbox.getHeight()/2));
                LatLonPointImpl llpi =
                        new LatLonPointImpl(el.getLatitude().getValue(),
                                el.getLongitude().getValue());

                dataSelection.putProperty("centerPosition", llpi);
                } catch (Exception ee){

                }
            }

            dataSelection.putProperty("navType", urlInfo.getNavType());

        }





        /**
         * _more_
         *
         * @return _more_
         */
        public String getPlace() {
            String pl = null;
            try {
                pl = translatePlace(
                    (String) locationComboBox.getSelectedItem());

            } catch (Exception e) {
                pl = defaultPlace;
            }
            urlInfo.setPlaceValue(pl);
            return pl;
        }

        /**
         * _more_
         *
         * @param thisPlace _more_
         *
         * @return _more_
         */
        protected String translatePlace(String thisPlace) {
            if (thisPlace.equals("Upper Left")) {
                return PLACE_ULEFT;
            }
            if (thisPlace.equals("Center")) {
                return PLACE_CENTER;
            }
            return thisPlace;
        }



        /**
         * _more_
         *
         * @return _more_
         */
        public int getElement() {
            int val = -1;
            try {
                val = Integer.parseInt(centerElementFld.getText().trim());
            } catch (Exception e) {}
            return val;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getLine() {
            int val = -1;
            try {
                if ( !(centerLineFld.getText().equals(Misc.MISSING))) {
                    val = Integer.parseInt(centerLineFld.getText().trim());
                }
            } catch (Exception e) {}
            return val;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        private double[][] getLineElement() {
            double[][] el = new double[2][1];
            el[0][0] = (double) getElement();
            el[1][0] = (double) getLine();
            return el;
        }



        /**
         * Set to full resolution
         *
         * @param update _more_
         */
        public void setToFullResolution(Boolean update) {
            setPlace(PLACE_CENTER);
            setLatitude(this.previewDir.getCenterLatitude());
            setLongitude(this.previewDir.getCenterLongitude());
            convertToLinEle();


            if (update) {
                setMagSliders(1, 1);
                setNumLines(this.maxLines);
                updateImageHeightSize();
                setNumEles(this.maxEles);
                updateImageWidthSize();
            } else {
                setNumLines(this.maxLines / Math.abs(getLineMagValue()));
                setNumEles(this.maxEles / Math.abs(getElementMagValue()));
            }

            amUpdating = true;
            lineMagSliderChanged(false);
            elementMagSliderChanged(false);
            amUpdating = false;

        }

        /**
         * _more_
         *
         * @param lineValue _more_
         * @param elementValue _more_
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
                urlInfo.setLineMag(getLineMagValue());
                elementMagSlider.setValue(elementValue);
                urlInfo.setElementMag(getElementMagValue());
                lineMagLbl.setText(StringUtil.padLeft("" + getLineMagValue(),
                        3));
                elementMagLbl.setText(StringUtil.padLeft(""
                        + getElementMagValue(), 3));
                linesToElements = Math.abs(lineValue / (double) elementValue);
                if (Double.isNaN(linesToElements)) {
                    linesToElements = 1.0;
                }
            }
        }

        /**
         * _more_
         *
         * @param str _more_
         */
        public void setPlace(String str) {
            if (str.equals("")) {
                str = defaultPlace;
            }
            //this.place = str;
            urlInfo.setPlaceValue(str);
            if (str.equals(PLACE_CENTER)) {
                locationComboBox.setSelectedItem("Center");
            } else {
                locationComboBox.setSelectedItem("Upper Left");
            }
        }

        /**
         * _more_
         */
        protected void convertToLinEle() {
            try {
                double[][] el = new double[2][1];
                double[][] ll = new double[2][1];
                AREACoordinateSystem macs =
                    (AREACoordinateSystem) sampleProjection;
                ll[0][0] = getLatitude();
                ll[1][0] = getLongitude();
                String coordType = getCoordinateType();
                el = baseAnav.toLinEle(ll);

                setLine((int) el[1][0]);
                setElement((int) el[0][0]);

            } catch (Exception e) {
                System.out.println("convertToLinEle e=" + e);
            }
        }


    }


    /**
     * Class description
     *
     *
     * @version        Enter version here..., Sun, Nov 24, '13
     * @author         Enter your name here...
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
        private JCheckBox chkUseFull;
        //final AddeImageDataSource this;

        /** _more_ */
        public String USE_DEFAULTREGION = DataSelection.PROP_USEDEFAULTAREA;

        /** _more_ */
        public String USE_SELECTEDREGION = DataSelection.PROP_USESELECTEDAREA;

        /** _more_ */
        public String USE_DISPLAYREGION = DataSelection.PROP_USEDISPLAYAREA;

        /** _more_ */
        private String[] regionSubsetOptionLabels = new String[] {
                                                        USE_DEFAULTREGION,
                USE_SELECTEDREGION, USE_DISPLAYREGION };

        /** the regions selection options */
        private TwoFacedObject[] regionSubsetOptions =
            new TwoFacedObject[] {
                new TwoFacedObject("Use Default Region",
                                   DataSelection.PROP_USEDEFAULTAREA),
                new TwoFacedObject("Select A Region",
                                   DataSelection.PROP_USESELECTEDAREA),
                new TwoFacedObject("Match Display Region",
                                   DataSelection.PROP_USEDISPLAYAREA) };


        /** _more_ */
        private JComponent regionsListInfo;

        /** _more_ */
        private String regionOption = USE_DEFAULTREGION;

        /** _more_ */
        JComboBox regionOptionLabelBox;

        /** _more_ */
        int eMag;

        /** _more_ */
        int lMag;

        /** _more_ */
        int eMag0;

        /** _more_ */
        int lMag0;

        /** _more_ */
        AddeImageDataSelection addeImageDataSelection;

        /**
         * Construct a AddeImagePreviewPanel
         * @param addeImageDataSelection _more_
         *
         * @throws IOException _more_
         * @throws ParseException _more_
         * @throws VisADException _more_
         */
        public AddeImagePreviewPanel(
                AddeImageDataSelection addeImageDataSelection)
                throws IOException, ParseException, VisADException {
            super("Region");

            this.addeImageDataSelection = addeImageDataSelection;
            this.imagePreview           = createImagePreview(source);
            display = new NavigatedMapPanel(null, true, false,
                                            imagePreview.getPreviewImage(),
                                            aAdapter.getAreaFile());
            this.eMag  = dataSource.getEMag();
            this.lMag  = dataSource.getLMag();
            this.eMag0  = dataSource.getEMag();
            this.lMag0  = dataSource.getLMag();
            chkUseFull = new JCheckBox(DataSelection.PROP_USEDEFAULTAREA);

            chkUseFull.setSelected(true);
            getRegionsList();
            JScrollPane jsp = new JScrollPane();
            jsp.getViewport().setView(display);
            //  jsp.add(GuiUtils.topCenter(regionsListInfo, display));
            JPanel labelsPanel = null;
            labelsPanel = new JPanel();
            labelsPanel.setLayout(new BoxLayout(labelsPanel, 2));
            labelsPanel.add(getRegionsList());

            MasterPanel = new JPanel(new java.awt.BorderLayout());
            MasterPanel.add(labelsPanel, "North");
            MasterPanel.add(jsp, "Center");

            display.getNavigatedPanel().addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent focusEvent) {
                    // System.err.println("Gain");
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

        /**
         * _more_
         *
         * @return _more_
         */
        public NavigatedMapPanel getNavigatedMapPanel() {
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
                regionsListInfo = makeRegionsListAndPanel(cbxLabel,
                        null);
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
                        (String) ((TwoFacedObject) regionOptionLabelBox
                            .getSelectedItem()).getId();
                    setRegionOptions(selectedObj);
                    setAdvancedPanel(selectedObj);
                    if (selectedObj.equals(DataSelection.PROP_USEDEFAULTAREA)
                            && (advancedPanel != null)) {
                        advancedPanel.reset();
                    }


                }

            });

            //timeDeclutterFld = new JTextField("" + getTimeDeclutterMinutes(), 5);
            GuiUtils.enableTree(regionOptionLabelBox, true);

            List regionOptionNames = Misc.toList(regionSubsetOptions);

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

            return (String) ((TwoFacedObject) regionOptionLabelBox
                .getSelectedItem()).getId();
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
                GeoSelection gs =
                    dataSource.getDataSelection().getGeoSelection();
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
            if (advancedPanel == null) {
                return;
            }
            regionOption = selectedObject.toString();
            //boolean isPR = prograssiveCbx1.isSelected();
            if (selectedObject.equals(USE_SELECTEDREGION)) {
                // only progressiveResolution and mag can be changed
                advancedPanel.enablePanelAll(true);
                //prograssiveCbx.doClick();
            } else {
                advancedPanel.enablePanelAll(false);
            }
            GuiUtils.enablePanel(leMagPanel,
                    !getIsProgressiveResolution());
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
            dataSource.getIdv().showWaitCursor();
            AddeImagePreview image = new AddeImagePreview(aAdapter,
                                         descriptor);
            dataSource.getDataContext().getIdv().showNormalCursor();
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
            return source;
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

        public void setElemMag(int value){
            eMag = Math.abs(value);
            //baseAnav.setMag(lMag,eMag);
        }

        public void setLineMag(int value){
            lMag = Math.abs(value);
            //baseAnav.setMag(lMag,eMag);
        }
        /**
         * _more_
         *
         * @param dataSelection _more_
         */
        public void applyToDataSelection(DataSelection dataSelection) {

            //boolean hasCorner = false;
            boolean isFull = false;
            regionOption = getRegionOption();
            GeoLocationInfo gInfo = null;


            geoSelection = new GeoSelection(gInfo);
            if (isFull) {
                geoSelection.setUseFullBounds(true);
            }
            dataSelection.putProperty(DataSelection.PROP_REGIONOPTION,
                                      regionOption);
            //dataSelection.putProperty(DataSelection.PROP_HASCORNER,
            //                          hasCorner);
            dataSelection.setGeoSelection(geoSelection);


        }


        /**
         * _more_
         */
        public void update() {

            ProjectionRect rect =
                display.getNavigatedPanel().getSelectedRegion();
            boolean hasCorner = false;
            boolean isFull    = false;
            if (rect != null) {
                // rect == null do nothing
                // no region subset, full image
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
                    } else if ((ulImpl.getLatitude() != ulImpl.getLatitude())
                               && (urImpl.getLatitude()
                                   != urImpl.getLatitude()) && (llImpl
                                       .getLatitude() != llImpl
                                       .getLatitude()) && (lrImpl
                                       .getLatitude() != lrImpl
                                       .getLatitude())) {

                        isFull = true;
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
                //int       displayNum = (int) rect.getWidth();
                int lines;
                int elems;
                if(ulLinEle[0][0] == ulLinEle[0][0] && lrLinEle[0][0] == lrLinEle[0][0]) {
                    lines  = (int) (lrLinEle[1][0] - ulLinEle[1][0])
                            * Math.abs(lMag0)/Math.abs(lMag);
                    elems = (int) (lrLinEle[0][0] - ulLinEle[0][0])
                            * Math.abs(eMag0)/Math.abs(eMag);
                } else if(ulLinEle[0][0] == ulLinEle[0][0]) {
                    lines  = (advancedPanel.maxLines - (int) (ulLinEle[1][0])
                            * Math.abs(lMag0))/Math.abs(lMag);
                    elems = (advancedPanel.maxEles - (int) (ulLinEle[0][0])
                            * Math.abs(eMag0))/Math.abs(eMag);
                } else if(lrLinEle[0][0] == lrLinEle[0][0]) {
                    lines  = (int) (lrLinEle[1][0])
                            * Math.abs(lMag0)/Math.abs(lMag);
                    elems = (int) (lrLinEle[0][0])
                            * Math.abs(eMag0)/Math.abs(eMag);
                } else {
                    lines = advancedPanel.maxLines;
                    elems = advancedPanel.maxEles;
                }

                lines = Math.abs(lines);
                elems = Math.abs(elems);
                advancedPanel.setIsFromRegionUpdate(true);

                // set lat lon values   locateValue = Misc.format(maxLat) + " " + Misc.format(minLon);
                if (isFull) {
                    advancedPanel.setToFullResolution(new Boolean(false));
                } else if ( !hasCorner) {
                    advancedPanel.setLatitude(gInfo.getMaxLat());
                    advancedPanel.setLongitude(gInfo.getMinLon());
                    advancedPanel.convertToLineEle();
                    advancedPanel.setPlace("ULEFT");
                } else {
                    double centerLat = (gInfo.getMaxLat()
                                        + gInfo.getMinLat()) / 2;
                    double centerLon = (gInfo.getMaxLon()
                                        + gInfo.getMinLon()) / 2;
                    advancedPanel.setLatitude(centerLat);
                    advancedPanel.setLongitude(centerLon);
                    advancedPanel.convertToLineEle();
                    advancedPanel.setPlace("CENTER");
                }
                // set latlon coord
                if(baseAnav.toString().equals("LALO")) {
                    advancedPanel.coordinateTypeComboBox.setSelectedIndex(1);
                } else {
                    advancedPanel.coordinateTypeComboBox.setSelectedIndex(0);
                }
                // update the size
                if ( !isFull) {
                    advancedPanel.setNumLines(lines);
                    advancedPanel.setNumEles(elems);
                    advancedPanel.setIsFromRegionUpdate(false);

                    // update the mag slider
                    // advancedPanel.setElementMagSlider(-Math.abs(eMag));
                    //advancedPanel.setLineMagSlider(-Math.abs(lMag));
                    advancedPanel.setBaseNumElements(elems * Math.abs(eMag));
                    advancedPanel.setBaseNumLines(lines * Math.abs(lMag));
                }
            }

        }
    }
}
