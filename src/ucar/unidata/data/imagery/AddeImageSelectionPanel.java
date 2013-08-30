/*
 * Copyright 1997-2013 Unidata Program Center/University Corporation for
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
import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.adde.AddeImageURL;
import edu.wisc.ssec.mcidas.adde.AddeTextReader;

import ucar.unidata.data.*;
import ucar.unidata.geoloc.*;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.view.geoloc.NavigatedMapPanel;

import ucar.unidata.view.geoloc.NavigatedPanel;

import ucar.visad.MapProjectionProjection;
import ucar.visad.display.RubberBandBox;

import visad.*;

import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.AreaAdapter;
import visad.data.mcidas.BaseMapAdapter;

import visad.georef.MapProjection;
import visad.georef.NavigatedCoordinateSystem;

import java.awt.*;
import java.awt.List;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.io.IOException;

import java.rmi.RemoteException;

import java.text.ParseException;

import java.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Created with IntelliJ IDEA.
 * User: opendap
 * Date: 5/28/13
 * Time: 10:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddeImageSelectionPanel extends DataSelectionComponent {

    /** _more_ */
    public JPanel advance;

    /** _more_ */
    GeoSelection geoSelection;

    /** _more_ */
    private java.awt.geom.Rectangle2D.Float new_bb;

    /** _more_ */
    private JPanel MasterPanel;

    /** _more_ */
    private String source;

    /** _more_ */
    AddeImageDescriptor descriptor;

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
    JTextField lineMagFld = new JTextField();

    /** _more_ */
    JTextField eleMagFld = new JTextField();

    /** _more_ */
    JLabel lineMagLbl = new JLabel();


    /** _more_ */
    private JPanel lMagPanel;

    /** _more_ */
    private JPanel eMagPanel;

    /** _more_ */
    private JPanel pRessPanel;

    /** _more_ */
    private JPanel cCoorPanel;


    /** _more_ */
    private static final int SLIDER_MAX = 1;

    /** _more_ */
    private static final int SLIDER_MIN = -29;

    /** _more_ */
    private static final int SLIDER_WIDTH = 150;

    /** _more_ */
    private static final int SLIDER_HEIGHT = 16;

    /** _more_ */
    boolean amSettingProperties = false;

    /** _more_ */
    private int lineMag;

    /** _more_ */
    private int elementMag;

    /** _more_ */
    private String kmLbl = " km";

    /** _more_ */
    JLabel elementMagLbl = new JLabel();

    /** _more_ */
    JCheckBox prograssiveCbx;

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
    private double latitude;

    /** _more_ */
    private double defaultLat = Double.NaN;

    /** _more_ */
    private double longitude;

    /** _more_ */
    private double defaultLon = Double.NaN;

    /** _more_ */
    private boolean resetLatLon = true;

    /** _more_ */
    private int imageLine;

    /** _more_ */
    private int areaLine;

    /** _more_ */
    private int areaElement;

    /** _more_ */
    private int imageElement;

    /** _more_ */
    private String defaultType = TYPE_LATLON;

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
    private AddeImageDataSource dataSource;

    /** _more_ */
    private boolean isLineEle = false;

    /** _more_ */
    AREAnav baseAnav;

    /** _more_ */
    JPanel sizePanel;

    /** _more_ */
    AddeImageDataSource.ImagePreviewSelection region;

    /** _more_ */
    boolean isFromRegionUpdate = false;

    /**
     * Construct a AddeImageSelectionPanel
     *
     * @param dataSource _more_
     * @param dc _more_
     * @param source _more_
     * @param baseAnav _more_
     * @param descriptor _more_
     * @param sample _more_
     * @param region _more_
     *
     * @throws IOException _more_
     * @throws ParseException _more_
     * @throws VisADException _more_
     */
    AddeImageSelectionPanel(AddeImageDataSource dataSource, DataChoice dc,
                            String source, AREAnav baseAnav,
                            AddeImageDescriptor descriptor,
                            MapProjection sample,
                            AddeImageDataSource.ImagePreviewSelection region)
            throws IOException, ParseException, VisADException {

        super("Advanced");
        this.dataSource       = dataSource;
        this.source           = source;
        this.descriptor       = descriptor;
        this.baseAnav         = baseAnav;
        this.previewNav       = baseAnav;
        this.dataChoice       = dc;
        this.sampleProjection = sample;
        this.region           = region;

        // init information for the magnification
        String magVal = AddeImageDataSource.getKey(source,
                            AddeImageURL.KEY_MAG);
        String[] magVals = magVal.split(" ");
        this.elementMag = new Integer(magVals[0]).intValue();
        this.lineMag    = new Integer(magVals[1]).intValue();


        // init information for the location and the default is LATLON
        AreaDirectory aDir = descriptor.getDirectory();
        this.isLineEle = true;
        double cLat = aDir.getCenterLatitude();
        double cLon = aDir.getCenterLongitude();
        setLatitude(cLat);
        setLongitude(cLon);
        convertToLineEle();
        /*String locVal = AddeImageDataSource.getKey(source, "LINELE");
        String[] locVals = locVal.split(" ");
        int li = new Integer(locVals[0]).intValue();
        int el = new Integer(locVals[1]).intValue();
        this.areaElement = el;
        this.areaLine = li;
        setElement(el);
        setLine(li);
        setLineElement();
        convertToLatLon();     */
        //
        this.previewDir       = aDir;
        this.sampleProjection = sample;
        this.place = AddeImageDataSource.getKey(source,
                AddeImageURL.KEY_PLACE);

        previewLineRes = Math.abs(lineMag);
        previewEleRes  = Math.abs(elementMag);
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
            dataSource.previewSelection.display.getNavigatedPanel();
        ProjectionRect rect = navigatedPanel.getSelectedRegion();
        ProjectionImpl projectionImpl =
            dataSource.previewSelection.display.getProjectionImpl();
        ProjectionRect newRect = new ProjectionRect();
        newRect.setHeight(rect.getHeight());
        newRect.setWidth(rect.getWidth());
        LatLonRect latLonRectOld = projectionImpl.getLatLonBoundingBox(rect);

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
            dataSource.previewSelection.display.getNavigatedPanel();
        ProjectionRect rect    = navigatedPanel.getSelectedRegion();

        ProjectionRect newRect = new ProjectionRect();
        newRect.setX(rect.getX());
        newRect.setY(rect.getY());
        newRect.setHeight(rect.getHeight());

        // calc the new width and height using the value from image size widget
        int newNumEles = getNumEles();
        double newWidth = rect.getWidth()
                          * (newNumEles * 1.0
                             / dataSource.previewSelection.getPreNumEles());
        newRect.setWidth(newWidth);

        navigatedPanel.setSelectedRegion(newRect);
        dataSource.previewSelection.setPreNumEles(newNumEles);
        updatePlace();
    }

    /**
     * _more_
     */
    public void updateImageHeightSize() {
        NavigatedPanel navigatedPanel =
            dataSource.previewSelection.display.getNavigatedPanel();
        ProjectionRect rect    = navigatedPanel.getSelectedRegion();

        ProjectionRect newRect = new ProjectionRect();
        newRect.setX(rect.getX());
        newRect.setY(rect.getY());
        newRect.setWidth(rect.getWidth());

        // calc the new width and height using the value from image size widget
        int newNumLines = getNumLines();
        double newHeight = rect.getHeight()
                           * (newNumLines * 1.0
                              / dataSource.previewSelection.getPreNumLines());
        newRect.setHeight(newHeight);

        navigatedPanel.setSelectedRegion(newRect);
        dataSource.previewSelection.setPreNumLines(newNumLines);
        updatePlace();
    }

    /**
     * _more_
     *
     * @param panel _more_
     * @param enable _more_
     */
    public void enablePanel(JPanel panel, boolean enable) {

        java.util.List cList = new ArrayList();
        Component[]    ac    = panel.getComponents();
        for (int i = 0; i < ac.length; i++) {
            Component a = ac[i];
            cList.add(a);
        }

        GuiUtils.enableComponents(cList, enable);
    }

    /**
     * _more_
     */
    private void setElementMag() {
        int val = 1;
        try {
            val = Integer.parseInt(eleMagFld.getText().trim());
        } catch (Exception e) {
            System.out.println(" setElementMag e=" + e);
            return;
        }
        setElementMag(val);
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
                StringTokenizer st   = new StringTokenizer(cards[i], " ");
                String          temp = st.nextToken();  // throw away the key
                int             m    = st.countTokens();
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
     * @param str _more_
     * @param numDec _more_
     *
     * @return _more_
     */
    private String truncateNumericString(String str, int numDec) {
        int indx = str.indexOf(".") + numDec + 1;
        if (indx >= str.length()) {
            indx = str.length();
        }
        return str.substring(0, indx);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getLineMag() {
        return this.lineMag;
    }

    /**
     * _more_
     *
     * @param val _more_
     */
    public void setLineMag(int val) {
        if (val > SLIDER_MAX) {
            val = SLIDER_MAX;
        }
        if (val < SLIDER_MIN - 1) {
            val = SLIDER_MIN - 1;
        }
        if (val == -1) {
            val = 1;
        }
        this.lineMag = val;
    }

    /**
     * _more_
     *
     * @param val _more_
     */
    private void setLineMagSlider(int val) {
        if (val == 1) {
            val = -1;
        }
        if (val > SLIDER_MAX) {
            val = -1;
        }
        if (val < SLIDER_MIN) {
            val = SLIDER_MIN - 1;
        }
        lineMagSlider.setValue(val + 1);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getElementMag() {
        return this.elementMag;
    }

    /**
     * _more_
     */
    private void setLineMag() {
        int val = 1;
        try {
            val = Integer.parseInt(lineMagFld.getText().trim());
        } catch (Exception e) {}
        setLineMag(val);
    }

    /**
     * _more_
     *
     * @param val _more_
     */
    public void setElementMag(int val) {
        if (val > SLIDER_MAX) {
            val = SLIDER_MAX;
        }
        if (val < SLIDER_MIN - 1) {
            val = SLIDER_MIN - 1;
        }
        if (val == -1) {
            val = 1;
        }
        this.elementMag = val;

    }

    /**
     * _more_
     *
     * @param recomputeLineEleRatio _more_
     */
    protected void elementMagSliderChanged(boolean recomputeLineEleRatio) {
        int value = getElementMagValue();
        setElementMag(value);

        elementMagLbl.setText("Ele  Mag=");
        eleMagFld.setText(new Integer(value).toString());
    }

    /**
     * _more_
     *
     * @param autoSetSize _more_
     */
    private void changeLineMagSlider(boolean autoSetSize) {
        int value = getLineMag();
        setLineMagSlider(value);
    }

    /**
     * _more_
     *
     * @param autoSetSize _more_
     */
    private void changeEleMagSlider(boolean autoSetSize) {
        int value = getElementMag();
        setElementMagSlider(value);
    }

    /**
     * _more_
     *
     * @param val _more_
     */
    private void setElementMagSlider(int val) {
        if (val == 1) {
            val = -1;
        }
        if (val > SLIDER_MAX) {
            val = -1;
        }
        if (val < SLIDER_MIN) {
            val = SLIDER_MIN - 1;
        }
        elementMagSlider.setValue(val + 1);
    }

    /**
     * Handle the line mag slider changed event
     *
     * @param autoSetSize  the event
     */
    protected void lineMagSliderChanged(boolean autoSetSize) {
        try {
            int value = getLineMagValue();
            setLineMag(value);

            lineMagLbl.setText("Line Mag=");
            lineMagFld.setText(new Integer(value).toString());

        } catch (Exception exc) {
            System.out.println("Setting line magnification" + exc);
        }
    }

    /**
     * Get the value of the line magnification slider.
     *
     * @return The magnification value for the line
     */
    protected int getLineMagValue() {
        int val = getMagValue(lineMagSlider);
        return val;
    }

    /**
     * Get the value of the element magnification slider.
     *
     * @return The magnification value for the element
     */
    protected int getElementMagValue() {
        int val = getMagValue(elementMagSlider);
        //   setElementMag(val);
        return val;
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
        if (value == 0) {
            value = SLIDER_MAX;
            return value;
        } else if (value < SLIDER_MIN) {
            value = SLIDER_MIN;
        }
        return value - 1;
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
    protected JComponent doMakeContents() {

        java.util.List allComps        = new ArrayList();
        Insets         dfltGridSpacing = new Insets(4, 0, 4, 0);
        String         dfltLblSpacing  = " ";
        JComponent     propComp        = null;

        // progressive checkbx
        prograssiveCbx = new JCheckBox("", true);

        prograssiveCbx.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean showMagSection =
                    !((JCheckBox) e.getSource()).isSelected();
                enablePanel(lMagPanel, showMagSection);
                enablePanel(eMagPanel, showMagSection);
                enablePanel(sizePanel, showMagSection);
            }
        });

        pRessPanel = GuiUtils.doLayout(new Component[] { prograssiveCbx }, 1,
                                       GuiUtils.WT_N, GuiUtils.WT_N);
        propComp = GuiUtils.hbox(new Component[] {
            GuiUtils.lLabel("ProgressiveResolution:"),
            pRessPanel }, 2);

        allComps.add(GuiUtils.rLabel("ProgressiveResolution:"));
        allComps.add(GuiUtils.left(prograssiveCbx));

        // coordinate types
        allComps.add(new JLabel(" "));
        allComps.add(new JLabel(" "));

        coordinateTypeComboBox = new JComboBox(coordinateTypes);
        coordinateTypeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int selectedIndex = coordinateTypeComboBox.getSelectedIndex();
                flipLocationPanel(selectedIndex);
            }
        });

        allComps.add(GuiUtils.rLabel("Coordinate Type:"));
        allComps.add(GuiUtils.left(coordinateTypeComboBox));

        // location
        allComps.add(new JLabel(" "));
        allComps.add(new JLabel(" "));
        locationComboBox = new JComboBox(locations);
        setPlace(this.place);


        allComps.add(GuiUtils.rLabel("Location:"));
        allComps.add(GuiUtils.left(locationComboBox));

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
            latLonWidget.setLatLon(this.latitude, this.longitude);
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
            centerLatLbl = GuiUtils.rLabel(" Lat:" + dfltLblSpacing), latFld,
            centerLonLbl = GuiUtils.rLabel(" Lon:" + dfltLblSpacing), lonFld,
            new JLabel(" "), centerPopup
        });

        lineElementPanel = GuiUtils.hbox(new Component[] {
            centerLineLbl = GuiUtils.rLabel(" Line:" + dfltLblSpacing),
            centerLineFld,
            centerElementLbl = GuiUtils.rLabel(" Element:" + dfltLblSpacing),
            centerElementFld });

        locationPanel = new GuiUtils.CardLayoutPanel();
        locationPanel.addCard(latLonPanel);
        locationPanel.addCard(lineElementPanel);

        allComps.add(GuiUtils.rLabel("  "));
        allComps.add(GuiUtils.left(locationPanel));

        // image size
        allComps.add(new JLabel(" "));
        allComps.add(new JLabel(" "));
        ActionListener sizeChange = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int lines = getNumLines() * Math.abs(getLineMag());
                if (lines > maxLines) {
                    lines = maxLines;
                }

                int eles = getNumEles() * Math.abs(getElementMag());
                if (eles > maxEles) {
                    eles = maxEles;
                }
                if (isNumLinesChanges()) {
                    updateImageHeightSize();
                }

                if (isNumElemsChanges()) {
                    updateImageWidthSize();
                }
            }
        };
        FocusListener sizeFocusChange = new FocusListener() {
            public void focusGained(FocusEvent fe) {}
            public void focusLost(FocusEvent fe) {
                int lines = getNumLines() * Math.abs(getLineMag());
                if (lines > maxLines) {
                    lines = maxLines;
                }

                int eles = getNumEles() * Math.abs(getElementMag());
                if (eles > maxEles) {
                    eles = maxEles;
                }

                if (isNumLinesChanges()) {
                    updateImageHeightSize();
                }

                if (isNumElemsChanges()) {
                    updateImageWidthSize();
                }
            }
        };

        this.maxLines = this.previewDir.getLines();
        this.maxEles  = this.previewDir.getElements();

        int lmag = getLineMag();
        int emag = getElementMag();
        if (lmag < 0) {
            this.numLines = this.maxLines / Math.abs(lmag);
        }
        if (emag < 0) {
            this.numEles = this.maxEles / Math.abs(emag);
        }

        setNumLines(this.numLines);
        numLinesFld = new JTextField(Integer.toString(this.numLines), 4);
        numLinesFld.addActionListener(sizeChange);
        numLinesFld.addFocusListener(sizeFocusChange);
        setNumEles(this.numEles);
        numElementsFld = new JTextField(Integer.toString(this.numEles), 4);
        numElementsFld.addActionListener(sizeChange);
        numElementsFld.addFocusListener(sizeFocusChange);
        numLinesFld.setToolTipText("Number of lines");
        numElementsFld.setToolTipText("Number of elements");
        GuiUtils.tmpInsets = dfltGridSpacing;
        sizeLbl            = GuiUtils.lLabel("");

        fullResBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/arrow_out.png", this,
                                     "setToFullResolution");
        fullResBtn.setContentAreaFilled(false);
        fullResBtn.setToolTipText("Set fields to retrieve full image");

        lockBtn = GuiUtils.getToggleImageButton(IdvUIManager.ICON_UNLOCK,
                IdvUIManager.ICON_LOCK, 0, 0, true);
        lockBtn.setContentAreaFilled(false);
        lockBtn.setSelected(true);
        lockBtn.setToolTipText(
            "Unlock to automatically change size when changing magnification");

        rawSizeLbl = new JLabel(" Raw size: " + this.maxLines + " X "
                                + this.maxEles);
        sizePanel = GuiUtils.left(GuiUtils.doLayout(new Component[] {
            numLinesFld, new JLabel(" X "), numElementsFld, sizeLbl,
            new JLabel(" "), fullResBtn, new JLabel("  "), lockBtn, rawSizeLbl
        }, 9, GuiUtils.WT_N, GuiUtils.WT_N));

        allComps.add(GuiUtils.rLabel("Image Size:"));
        allComps.add(GuiUtils.left(sizePanel));

        // Magnification
        propComp = GuiUtils.hbox(new Component[] { new JLabel("") }, 1);
        addPropComp("Magnification: ", propComp);
        allComps.add(GuiUtils.rLabel("Magnification:"));
        allComps.add(GuiUtils.left(propComp));
        //GuiUtils.tmpInsets = GRID_INSETS;

        //line mag
        boolean oldAmSettingProperties = amSettingProperties;
        amSettingProperties = true;
        ChangeListener lineListener = new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                if (amSettingProperties) {
                    return;
                }
                int val = getMagValue(lineMagSlider);
                setLineMag(val);
                lineMagSliderChanged(true);

            }
        };

        ActionListener lineMagChange = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (amSettingProperties) {
                    return;
                }
                setLineMag();
                changeLineMagSlider(true);
            }
        };
        FocusListener lineMagFocusChange = new FocusListener() {
            public void focusGained(FocusEvent fe) {}
            public void focusLost(FocusEvent fe) {
                if (amSettingProperties) {
                    return;
                }
                setLineMag();
                changeLineMagSlider(true);
            }
        };
        JComponent[] lineMagComps = GuiUtils.makeSliderPopup(SLIDER_MIN,
                                        SLIDER_MAX, -3, lineListener);
        lineMagSlider = (JSlider) lineMagComps[1];
        lineMagSlider.setPreferredSize(new Dimension(SLIDER_WIDTH,
                SLIDER_HEIGHT));
        lineMagSlider.setMajorTickSpacing(1);
        lineMagSlider.setSnapToTicks(true);
        lineMagSlider.setExtent(1);
        int mag = getLineMag();
        setLineMagSlider(mag);
        lineMagComps[0].setToolTipText("Change the line magnification");
        lineMagSlider.setToolTipText(
            "Slide to set line magnification factor");
        String str = "Line Mag=";
        lineMagFld = new JTextField(Integer.toString(mag), 3);
        lineMagFld.addFocusListener(lineMagFocusChange);
        lineMagFld.addActionListener(lineMagChange);
        lineMagLbl = GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));

        amSettingProperties = oldAmSettingProperties;

        GuiUtils.tmpInsets  = dfltGridSpacing;
        lMagPanel = GuiUtils.doLayout(new Component[] { lineMagLbl,
                lineMagFld,
                GuiUtils.inset(lineMagComps[1], new Insets(0, 4, 0, 0)) }, 5,
                    GuiUtils.WT_N, GuiUtils.WT_N);
        propComp = GuiUtils.hbox(new Component[] { lMagPanel }, 1);
        // allComps.add(GuiUtils.left(propComp));
        // allComps.add(GuiUtils.lLabel(""));
        allComps.add(GuiUtils.rLabel(""));
        allComps.add(GuiUtils.left(propComp));

        //element mag
        amSettingProperties = true;
        ChangeListener elementListener = new ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                if (amSettingProperties) {
                    return;
                }
                int val = getMagValue(elementMagSlider);
                setElementMag(val);
                elementMagSliderChanged(true);
            }
        };
        ActionListener eleMagChange = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (amSettingProperties) {
                    return;
                }
                setElementMag();
                changeEleMagSlider(true);
            }
        };
        FocusListener eleMagFocusChange = new FocusListener() {
            public void focusGained(FocusEvent fe) {}
            public void focusLost(FocusEvent fe) {
                if (amSettingProperties) {
                    return;
                }
                setElementMag();
                changeEleMagSlider(true);
            }
        };
        JComponent[] elementMagComps = GuiUtils.makeSliderPopup(SLIDER_MIN,
                                           SLIDER_MAX, 0, elementListener);
        elementMagSlider = (JSlider) elementMagComps[1];
        elementMagSlider.setPreferredSize(new Dimension(SLIDER_WIDTH,
                SLIDER_HEIGHT));
        elementMagSlider.setExtent(1);
        elementMagSlider.setMajorTickSpacing(1);
        elementMagSlider.setSnapToTicks(true);
        mag = getElementMag();
        setElementMagSlider(mag);
        elementMagComps[0].setToolTipText("Change the element magnification");
        elementMagSlider.setToolTipText(
            "Slide to set element magnification factor");
        eleMagFld = new JTextField(Integer.toString(mag), 3);
        eleMagFld.addFocusListener(eleMagFocusChange);
        eleMagFld.addActionListener(eleMagChange);
        str = "Ele  Mag=";
        elementMagLbl = GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str,
                4));

        amSettingProperties = oldAmSettingProperties;

        GuiUtils.tmpInsets  = dfltGridSpacing;
        eMagPanel = GuiUtils.doLayout(new Component[] { elementMagLbl,
                eleMagFld,
                GuiUtils.inset(elementMagComps[1],
                               new Insets(0, 4, 0, 0)) }, 5, GuiUtils.WT_N,
                                   GuiUtils.WT_N);
        propComp = GuiUtils.hbox(new Component[] { eMagPanel }, 1);
        // allComps.add(GuiUtils.left(propComp));
        // allComps.add(GuiUtils.lLabel(" "));
        allComps.add(GuiUtils.rLabel(""));
        allComps.add(GuiUtils.left(propComp));


        //all
        JPanel imagePanel = GuiUtils.doLayout(allComps, 2, GuiUtils.WT_NY,
                                GuiUtils.WT_N);
        advance = GuiUtils.top(imagePanel);
        boolean showMagSection = !prograssiveCbx.isSelected();
        enablePanel(lMagPanel, showMagSection);
        enablePanel(eMagPanel, showMagSection);
        enablePanel(sizePanel, showMagSection);
        String s0 = AddeImageDataSource.getKey(this.source,
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


        return MasterPanel;

    }

    /**
     * _more_
     *
     * @return _more_
     */
    boolean isNumLinesChanges() {
        if (dataSource.previewSelection.getPreNumLines() == 0) {
            return false;
        }
        return dataSource.previewSelection.getPreNumLines() != getNumLines();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    boolean isNumElemsChanges() {
        if (dataSource.previewSelection.getPreNumEles() == 0) {
            return false;
        }
        return dataSource.previewSelection.getPreNumEles() != getNumEles();
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
                ele = this.areaElement;
                lin = this.areaLine;
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
    public boolean getIsProgressiveResolution() {
        return this.prograssiveCbx.isSelected();
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
        return this.latitude;
    }

    /**
     * _more_
     */
    private void setLatitude() {
        this.latitude = latLonWidget.getLat();
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
        this.latitude    = val;
        this.resetLatLon = false;
    }

    /**
     * _more_
     */
    private void setLongitude() {
        this.longitude = latLonWidget.getLon();
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
        return this.longitude;
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
        this.longitude   = val;
        this.resetLatLon = false;
    }

    /**
     * _more_
     */
    protected void convertToLineEle() {
        double[][] ll = new double[2][1];
        ll[0][0] = getLatitude();
        ll[1][0] = getLongitude();
        AREACoordinateSystem macs = (AREACoordinateSystem) sampleProjection;
        double[][]           el   = this.baseAnav.toLinEle(ll);
        try {
            double[][] el1 = macs.fromReference(ll);
        } catch (Exception e) {}
        this.areaElement = (int) Math.floor(el[0][0] + 0.5)
                           * Math.abs(this.elementMag);
        this.areaLine = (int) Math.floor(el[1][0] + 0.5)
                        * Math.abs(this.lineMag);
        el                = this.baseAnav.areaCoordToImageCoord(el);
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
        el[0][0] = (double) (el1[0][0] / Math.abs(this.elementMag) + 0.5);
        el[1][0] = (double) (el1[1][0] / Math.abs(this.lineMag) + 0.5);
        try {
            //AREACoordinateSystem macs = (AREACoordinateSystem)sampleProjection;
            ll = this.baseAnav.toLatLon(el);
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
    private void setNumberOfLines(int val) {
        numLinesFld.setText(Integer.toString(val));
    }

    /**
     * _more_
     *
     * @param val _more_
     */
    public void setNumLines(int val) {
        this.numLines = val;
        if (val >= 0) {
            setNumberOfLines(val);
        }
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
            setNumberOfElements(val);
        }
    }

    /**
     * _more_
     *
     * @param val _more_
     */
    private void setNumberOfElements(int val) {
        numElementsFld.setText(Integer.toString(val));
    }

    /**
     * _more_
     */
    protected void setLineElement() {
        double[][] el = getLineElement();

        this.areaElement = (int) Math.floor(el[0][0] + 0.5);
        this.areaLine    = (int) Math.floor(el[1][0] + 0.5);
        double[][] vals = this.baseAnav.areaCoordToImageCoord(el);
        this.imageElement = (int) Math.floor(vals[0][0] + 0.5);
        this.imageLine    = (int) Math.floor(vals[1][0] + 0.5);

    }

    /**
     * _more_
     *
     * @param lin _more_
     * @param ele _more_
     *
     * @return _more_
     */
    protected GeoLocationInfo getGeoLocationInfo(int lin, int ele) {
        int nLin = getNumLines();
        if (nLin > 0) {
            int nEle = getNumEles();
            if (nEle > 0) {
                int lMag = getLineMag();
                if (lMag > 1) {
                    return geoLocInfo;
                }
                int eMag = getElementMag();
                if (eMag > 1) {
                    return geoLocInfo;
                }
                geoLocInfo = makeGeoLocationInfo(lin, ele, nLin, nEle, lMag,
                        eMag);
                return geoLocInfo;
            }
        }
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
                latLon[i][j]    = Double.NaN;
                imageEL[i][j]   = Double.NaN;
                areaEL[i][j]    = Double.NaN;
                displayEL[i][j] = Double.NaN;
            }
        }

        setLine(-1);
        setElement(-1);
        return null;
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
     * @param lin _more_
     * @param ele _more_
     * @param nlins _more_
     * @param neles _more_
     * @param linMag _more_
     * @param eleMag _more_
     *
     * @return _more_
     */
    private GeoLocationInfo makeGeoLocationInfo(int lin, int ele, int nlins,
            int neles, int linMag, int eleMag) {

        geoLocInfo = null;

        String               plc         = getPlace();
        String               type        = getCoordinateType();

        AREACoordinateSystem macs = (AREACoordinateSystem) sampleProjection;
        Rectangle2D          mapArea     = macs.getDefaultMapArea();
        double               previewXDim = mapArea.getWidth();
        double               previewYDim = mapArea.getHeight();

        double dLine = (double) nlins / (2.0 * this.previewLineRes)
                       * Math.abs(linMag);
        double dEle = (double) neles / (2.0 * this.previewEleRes)
                      * Math.abs(eleMag);

        if (plc.equals(PLACE_CENTER)) {
            displayEL[0][0] = ele;
            displayEL[1][0] = lin;
            displayEL[0][1] = ele - dEle;
            if (displayEL[0][1] < 0) {
                displayEL[0][1] = 0.0;
            }
            displayEL[1][1] = lin + dLine;
            if (displayEL[1][1] > previewYDim) {
                displayEL[1][1] = previewYDim;
            }
        } else if (plc.equals(PLACE_ULEFT)) {
            displayEL[0][0] = ele + dEle;
            if (displayEL[0][0] > previewXDim) {
                displayEL[0][0] = previewXDim;
            }
            displayEL[1][0] = lin - dLine;
            if (displayEL[1][0] < 0) {
                displayEL[1][0] = 0.0;
            }
            displayEL[0][1] = ele;
            displayEL[1][1] = lin;
        }
        int cEle = (int) Math.ceil(displayEL[0][0]);
        int cLin = (int) Math.ceil(displayEL[1][0]);
        displayEL[0][2] = cEle + dEle;
        if (displayEL[0][2] > previewXDim) {
            displayEL[0][2] = previewXDim;
        }
        displayEL[1][2] = cLin + dLine;
        if (displayEL[1][2] > previewYDim) {
            displayEL[1][2] = previewYDim;
        }
        displayEL[0][3] = cEle - dEle;
        if (displayEL[0][3] < 0) {
            displayEL[0][3] = 0.0;
        }
        displayEL[1][3] = cLin - dLine;
        if (displayEL[1][3] < 0) {
            displayEL[1][3] = 0.0;
        }
        displayEL[0][4] = cEle + dEle;
        if (displayEL[0][4] > previewXDim) {
            displayEL[0][4] = previewXDim;
        }
        displayEL[1][4] = cLin - dLine;
        if (displayEL[1][4] < 0) {
            displayEL[1][4] = 0.0;
        }

        areaEL = displayCoordToAreaCoord(displayEL);

        for (int i = 0; i < 5; i++) {
            if (areaEL[0][i] < 0.0) {
                areaEL[0][i] = 0.0;
            }
            if (areaEL[0][i] > this.maxEles) {
                areaEL[0][i] = (double) this.maxEles;
            }
            if (areaEL[1][i] < 0.0) {
                areaEL[1][i] = 0.0;
            }
            if (areaEL[1][i] > this.maxLines) {
                areaEL[1][i] = (double) this.maxLines;
            }
        }

        try {
            latLon = macs.toReference(displayEL);
        } catch (Exception e) {
            System.out.println("Error converting input lat/lon e=" + e);
        }

        double maxLat = latLon[0][1];
        if (latLon[0][2] > maxLat) {
            maxLat = latLon[0][2];
        }
        double minLat = latLon[0][3];
        if (latLon[0][4] < minLat) {
            minLat = latLon[0][4];
        }
        double maxLon = latLon[1][4];
        if (latLon[1][2] > maxLon) {
            maxLon = latLon[1][2];
        }
        double minLon = latLon[1][1];
        if (latLon[1][3] < minLon) {
            minLon = latLon[1][3];
        }

        imageEL    = this.previewNav.areaCoordToImageCoord(areaEL);

        geoLocInfo = new GeoLocationInfo(maxLat, minLon, minLat, maxLon);

        return geoLocInfo;

    }

    /**
     * _more_
     *
     * @param dataSelection _more_
     */
    public void applyToDataSelection(DataSelection dataSelection) {

        //do something

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
     * _more_
     *
     * @return _more_
     */
    public int getPreviewLineRes() {
        return this.previewLineRes;
    }

    /**
     * _more_
     *
     * @param val _more_
     */
    public void setPreviewLineRes(int val) {
        this.previewLineRes = val;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getPreviewEleRes() {
        return this.previewEleRes;
    }

    /**
     * _more_
     *
     * @param val _more_
     */
    public void setPreviewEleRes(int val) {
        this.previewEleRes = val;
    }

    /**
     * _more_
     *
     * @param disp _more_
     *
     * @return _more_
     */
    private double[][] displayCoordToAreaCoord(double[][] disp) {
        double[][] area = new double[2][disp[0].length];
        try {
            if (sampleProjection != null) {
                AREACoordinateSystem macs =
                    (AREACoordinateSystem) sampleProjection;
                double[][] ll     = macs.toReference(disp);
                double[][] el     = this.baseAnav.toLinEle(ll);
                int        midEle = (int) Math.floor(el[0][0] + 0.5);
                int        midLin = (int) Math.floor(el[1][0] + 0.5);

                int width = (int) Math.floor(Math.abs(disp[0][2]
                                - disp[0][1]) * getPreviewEleRes() + 0.5);

                int height = (int) Math.floor(Math.abs(disp[1][3]
                                 - disp[1][1]) * getPreviewLineRes() + 0.5);
                int deltaEle = width / 2;
                int deltaLin = height / 2;

                area[0][0] = midEle;
                area[1][0] = midLin;
                area[0][1] = midEle - deltaEle;
                area[1][1] = midLin - deltaLin;
                area[0][2] = midEle + deltaEle;
                area[1][2] = midLin - deltaLin;
                area[0][3] = midEle - deltaEle;
                area[1][3] = midLin + deltaLin;
                area[0][4] = midEle + deltaEle;
                area[1][4] = midLin + deltaLin;

            }
        } catch (Exception e) {
            System.out.println("displayCoordToAreaCoord e=" + e);
        }
        return area;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getPlace() {
        try {
            this.place =
                translatePlace((String) locationComboBox.getSelectedItem());
        } catch (Exception e) {
            this.place = defaultPlace;
        }
        return this.place;
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
    protected double[][] convertToDisplayCoords() {
        double[][] el = getLineElement();
        try {
            double[][] ll = new double[2][1];
            AREACoordinateSystem macs =
                (AREACoordinateSystem) sampleProjection;
            String type = getCoordinateType();
            if (type.equals(TYPE_LATLON)) {
                ll[0][0] = getLatitude();
                ll[1][0] = getLongitude();
                el       = macs.fromReference(ll);
            } else {
                int[]       dirB           = macs.getDirBlock();
                int         previewLineMag = dirB[11];
                int         previewEleMag  = dirB[12];
                int         dirLMag        = this.previewDir.getValue(11);
                int         dirEMag        = this.previewDir.getValue(12);

                Rectangle2D mapArea        = macs.getDefaultMapArea();
                int previewXDim =
                    new Long(
                        new Double(
                            mapArea.getMaxX()
                            - mapArea.getMinX()).longValue()).intValue();
                int previewYDim =
                    new Long(
                        new Double(
                            mapArea.getMaxY()
                            - mapArea.getMinY()).longValue()).intValue();
                el[0][0] = el[0][0] * dirEMag / previewEleMag;
                el[1][0] = previewYDim - 1
                           - el[1][0] * dirLMag / previewLineMag;;
            }
        } catch (Exception e) {
            System.out.println("convertToDisplayCoords e=" + e);
        }
        return el;
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
     */
    public void setToFullResolution() {
        setPlace(PLACE_CENTER);
        setLatitude(this.previewDir.getCenterLatitude());
        setLongitude(this.previewDir.getCenterLongitude());
        convertToLinEle();
        setNumLines(this.maxLines);
        setNumEles(this.maxEles);

        setLineMag(1);
        setElementMag(1);
        setLineMagSlider(1);

        setElementMagSlider(1);

        amUpdating = true;
        lineMagSliderChanged(false);
        elementMagSliderChanged(false);
        amUpdating = false;

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
        this.place = str;
        if (str.equals(PLACE_CENTER)) {
            locationComboBox.setSelectedItem("Center");
        } else {
            locationComboBox.setSelectedItem("Upper Left");
        }
    }

    /**
     * _more_
     *
     * @param str _more_
     */
    public void setPlace0(String str) {
        this.place = str;
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
            el = this.previewNav.toLinEle(ll);

            setLine((int) el[1][0]);
            setElement((int) el[0][0]);

        } catch (Exception e) {
            System.out.println("convertToLinEle e=" + e);
        }
    }

    /** _more_ */
    protected boolean amUpdating = false;

    /** _more_ */
    private GeoLocationInfo geoLocInfo;

    /** _more_ */
    double[][] imageEL = new double[2][5];

    /** _more_ */
    double[][] areaEL = new double[2][5];

    /** _more_ */
    double[][] displayEL = new double[2][5];

    /** _more_ */
    double[][] latLon = new double[2][5];

    /** _more_ */
    private String place;

    /** _more_ */
    MapProjection sampleProjection;

    /** _more_ */
    private int previewLineRes = 1;

    /** _more_ */
    private int previewEleRes = 1;

    /** _more_ */
    private int maxLines = 0;

    /** _more_ */
    private int maxEles = 0;

    /** _more_ */
    private AREAnav previewNav;

    /** _more_ */
    private AreaDirectory previewDir;

    /** _more_ */
    protected static final String PLACE_CENTER = "CENTER";

    /** _more_ */
    protected static final String PLACE_ULEFT = "ULEFT";

    /** _more_ */
    private String defaultPlace = PLACE_CENTER;

    /** Property for image default value lat/lon */
    protected static final String PROP_LATLON = "LATLON";

    /** Property for image default value line/ele */
    protected static final String PROP_LINEELE = "LINELE";


    /** _more_ */
    JLabel sizeLbl;

    /** _more_ */
    private JToggleButton lockBtn;

    /** _more_ */
    private JButton fullResBtn;

    /** _more_ */
    JLabel rawSizeLbl = new JLabel();

    /** _more_ */
    DataChoice dataChoice;
}
