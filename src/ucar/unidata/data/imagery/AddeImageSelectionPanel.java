package ucar.unidata.data.imagery;

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

import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaFile;
import edu.wisc.ssec.mcidas.adde.AddeTextReader;
import ucar.unidata.data.*;
import ucar.unidata.geoloc.*;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.StringUtil;
import ucar.unidata.view.geoloc.NavigatedMapPanel;
import ucar.visad.MapProjectionProjection;
import ucar.visad.display.RubberBandBox;
import visad.*;
import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.AreaAdapter;
import visad.data.mcidas.BaseMapAdapter;
import visad.georef.MapProjection;
import visad.georef.NavigatedCoordinateSystem;

/**
 * Created with IntelliJ IDEA.
 * User: opendap
 * Date: 5/28/13
 * Time: 10:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddeImageSelectionPanel extends DataSelectionComponent
{
    public JPanel advance;
    GeoSelection geoSelection;

    private java.awt.geom.Rectangle2D.Float new_bb;
    private JPanel MasterPanel;

    private String source;
    AddeImageDescriptor descriptor;
    private JCheckBox chkUseFull;

    private Hashtable propToComps = new Hashtable();
    /** The spacing used in the grid layout */
    protected  int GRID_SPACING = 3;

    /** Used by derived classes when they do a GuiUtils.doLayout */
    protected  Insets GRID_INSETS = new Insets(GRID_SPACING,
            GRID_SPACING,
            GRID_SPACING,
            GRID_SPACING);
    protected JSlider lineMagSlider;
    protected JSlider elementMagSlider;
    JTextField lineMagFld = new JTextField();
    JTextField eleMagFld = new JTextField();
    JLabel lineMagLbl = new JLabel();
    JLabel lineResLbl = new JLabel();
    private JPanel lMagPanel;
    private JPanel eMagPanel;
    private JPanel pRessPanel;
    private static final int SLIDER_MAX = 1;
    private static final int SLIDER_MIN = -29;
    private static final int SLIDER_WIDTH = 150;
    private static final int SLIDER_HEIGHT = 16;
    boolean amSettingProperties = false;
    private int lineMag;
    private int elementMag;
    private double lRes;
    protected double baseLRes = 0.0;
    private double eRes;
    protected double baseERes = 0.0;
    private String kmLbl = " km";
    JLabel elementMagLbl = new JLabel();
    JLabel elementResLbl = new JLabel();
    private int lineResolution;
    private int elementResolution;

    JCheckBox prograssiveCbx;

    AddeImageSelectionPanel(String source, AddeImageDescriptor descriptor)
            throws IOException, ParseException, VisADException
    {

        super("Advanced");
        this.source =  source;
        this.descriptor = descriptor;
        JComponent propComp = null;
        java.util.List allComps = new ArrayList();
        Insets  dfltGridSpacing = new Insets(4, 0, 4, 0);
        AddeImageInfo aInfo = descriptor.getImageInfo();
        this.elementMag = aInfo.getElementMag();
        this.lineMag = aInfo.getLineMag();
        AreaDirectory aDir = descriptor.getDirectory();
        try{
            this.lineResolution = aDir.getValue(11);
            this.elementResolution = aDir.getValue(12);
        } catch (Exception e){

        }
        float[] res = getLineEleResolution(aDir);
        float resol = res[0];
        if (this.lineMag < 0)
            resol *= Math.abs(this.lineMag);

        this.lRes = resol;
        this.baseLRes =  (double)(this.lineResolution);

        resol = res[1];
        if (this.elementMag < 0)
            resol *= Math.abs(this.elementMag);
        this.eRes = resol;
        this.baseERes =  (double)(this.elementResolution);
        // progressive checkbx
        prograssiveCbx = new JCheckBox("", true);

        prograssiveCbx.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean showMagSection =  !((JCheckBox) e.getSource()).isSelected();
                enablePanel(lMagPanel, showMagSection);
                enablePanel(eMagPanel, showMagSection);
            }
        });

        pRessPanel = GuiUtils.doLayout(new Component[] { prograssiveCbx
                  }, 1,
                GuiUtils.WT_N, GuiUtils.WT_N);
        propComp = GuiUtils.hbox(new Component[] { GuiUtils.lLabel("ProgressiveResolution:"), pRessPanel }, 2);
       // allComps.add(GuiUtils.lLabel("ProgressiveResolution:"));
        allComps.add(GuiUtils.left(propComp)) ;
        allComps.add(GuiUtils.hbox(new Component[]{new JLabel("")}, 1)) ;
        // Magnification
        propComp = GuiUtils.hbox(new Component[]{new JLabel("")}, 1);
        addPropComp("Magnification: ", propComp);
        allComps.add(GuiUtils.lLabel("Magnification:"));
        allComps.add(GuiUtils.right(propComp));
        GuiUtils.tmpInsets = GRID_INSETS;

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

        ActionListener lineMagChange =new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (amSettingProperties) {
                    return;
                }
                setLineMag();
                changeLineMagSlider(true);
            }
        };
        FocusListener lineMagFocusChange = new FocusListener() {
            public void focusGained(FocusEvent fe) {
            }
            public void focusLost(FocusEvent fe) {
                if (amSettingProperties) {
                    return;
                }
                setLineMag();
                changeLineMagSlider(true);
            }
        };
        JComponent[] lineMagComps =
                GuiUtils.makeSliderPopup(SLIDER_MIN, SLIDER_MAX, -3,
                        lineListener);
        lineMagSlider = (JSlider) lineMagComps[1];
        lineMagSlider.setPreferredSize(new Dimension(SLIDER_WIDTH,SLIDER_HEIGHT));
        lineMagSlider.setMajorTickSpacing(1);
        lineMagSlider.setSnapToTicks(true);
        lineMagSlider.setExtent(1);
        int mag = getLineMag();
        setLineMagSlider(mag);
        lineMagComps[0].setToolTipText(
                "Change the line magnification");
        lineMagSlider.setToolTipText(
                "Slide to set line magnification factor");
        String str = "Line Mag=";
        lineMagFld = new JTextField(Integer.toString(mag),3);
        lineMagFld.addFocusListener(lineMagFocusChange);
        lineMagFld.addActionListener(lineMagChange);
        lineMagLbl =
                GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
        str = truncateNumericString(Double.toString(this.baseLRes*Math.abs(getLineMag())), 1);
        str = " Res=" + str + kmLbl;
        lineResLbl =
                GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
        amSettingProperties = oldAmSettingProperties;

        GuiUtils.tmpInsets  = dfltGridSpacing;
        lMagPanel = GuiUtils.doLayout(new Component[] {
                lineMagLbl, lineMagFld,
                GuiUtils.inset(lineMagComps[1],
                        new Insets(0, 4, 0, 0)), lineResLbl, }, 5,
                GuiUtils.WT_N, GuiUtils.WT_N);
        propComp = GuiUtils.hbox(new Component[] {  lMagPanel }, 1);
        allComps.add(GuiUtils.left(propComp));
        allComps.add(GuiUtils.lLabel(""));
        //  allComps.add(GuiUtils.left(propComp));

        //element mag
        amSettingProperties = true;
        ChangeListener elementListener = new ChangeListener() {
            public void stateChanged(
                    javax.swing.event.ChangeEvent evt) {
                if (amSettingProperties) {
                    return;
                }
                int val = getMagValue(elementMagSlider);
                setElementMag(val);
                elementMagSliderChanged(true);
            }
        };
        ActionListener eleMagChange =new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (amSettingProperties) {
                    return;
                }
                setElementMag();
                changeEleMagSlider(true);
            }
        };
        FocusListener eleMagFocusChange = new FocusListener() {
            public void focusGained(FocusEvent fe) {
            }
            public void focusLost(FocusEvent fe) {
                if (amSettingProperties) {
                    return;
                }
                setElementMag();
                changeEleMagSlider(true);
            }
        };
        JComponent[] elementMagComps =
                GuiUtils.makeSliderPopup(SLIDER_MIN, SLIDER_MAX, 0,
                        elementListener);
        elementMagSlider = (JSlider) elementMagComps[1];
        elementMagSlider.setPreferredSize(new Dimension(SLIDER_WIDTH,SLIDER_HEIGHT));
        elementMagSlider.setExtent(1);
        elementMagSlider.setMajorTickSpacing(1);
        elementMagSlider.setSnapToTicks(true);
        mag = getElementMag();
        setElementMagSlider(mag);
        elementMagComps[0].setToolTipText(
                "Change the element magnification");
        elementMagSlider.setToolTipText(
                "Slide to set element magnification factor");
        eleMagFld = new JTextField(Integer.toString(mag),3);
        eleMagFld.addFocusListener(eleMagFocusChange);
        eleMagFld.addActionListener(eleMagChange);
        str = "Ele  Mag=";
        elementMagLbl =
                GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
        str = truncateNumericString(Double.toString(this.baseERes*Math.abs(getElementMag())), 1);
        str = " Res=" + str + kmLbl;
        elementResLbl =
                GuiUtils.getFixedWidthLabel(StringUtil.padLeft(str, 4));
        amSettingProperties = oldAmSettingProperties;

        GuiUtils.tmpInsets  = dfltGridSpacing;
        eMagPanel = GuiUtils.doLayout(new Component[] {
                elementMagLbl, eleMagFld,
                GuiUtils.inset(elementMagComps[1],
                        new Insets(0, 4, 0, 0)), elementResLbl, }, 5,
                GuiUtils.WT_N, GuiUtils.WT_N);
        propComp = GuiUtils.hbox(new Component[] {  eMagPanel }, 1);
        allComps.add(GuiUtils.left(propComp));
        allComps.add(GuiUtils.lLabel(""));
        //all
        JPanel imagePanel = GuiUtils.doLayout(allComps, 2, GuiUtils.WT_NY,
                GuiUtils.WT_N);
        advance = GuiUtils.top(imagePanel);

        enablePanel(lMagPanel, false);
        enablePanel(eMagPanel, false);

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
    }

    public void enablePanel(JPanel panel, boolean enable){

        java.util.List cList = new ArrayList();
        Component [] ac = panel.getComponents();
        for(int i = 0; i< ac.length; i++){
            Component a = ac[i];
            cList.add(a);
        }

        GuiUtils.enableComponents(cList, enable);
    }

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

    protected String getUrl() {
        String str = source;
        str = str.replaceFirst("imagedata", "text");
        int indx = str.indexOf("VERSION");
        str = str.substring(0, indx);
        str = str.concat("file=SATBAND");
        return str;
    }

    protected java.util.List readTextLines(String url) {
        AddeTextReader reader = new AddeTextReader(url);
        java.util.List lines = null;
        if ("OK".equals(reader.getStatus())) {
            lines = reader.getLinesOfText();
        }
        return lines;
    }

    private float[] getLineEleResolution(AreaDirectory ad) {

        float[] res = {(float)1.0, (float)1.0};
        int sensor = ad.getSensorID();
        java.util.List lines = null;
        try {
            String buff = getUrl();

            lines = readTextLines(buff);
            if (lines == null) {
                return res;
            }

            int gotit = -1;
            String[] cards = StringUtil.listToStringArray(lines);


            for (int i=0; i<cards.length; i++) {
                if ( ! cards[i].startsWith("Sat ")) continue;
                StringTokenizer st = new StringTokenizer(cards[i]," ");
                String temp = st.nextToken();  // throw away the key
                int m = st.countTokens();
                for (int k=0; k<m; k++) {
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
            for (int i=gotit; i<cards.length; i++) {
                if (cards[i].startsWith("EndSat")) {
                    return res;
                }
                if (!cards[i].startsWith("B") ) {
                    continue;
                }
                StringTokenizer tok = new StringTokenizer(cards[i]);
                String str = tok.nextToken();
                str = tok.nextToken();
                Float flt = new Float(str);
                res[0] = flt.floatValue();
                str = tok.nextToken();
                flt = new Float(str);
                res[1] = flt.floatValue();
                return res;
            }
        } catch (Exception e) {

        }
        return res;
    }

    public void setDataChoice(DataChoice dataChoice)
    {


        // display.updateImage(image_preview.getPreviewImage());
    }
    protected JComponent addPropComp(String propId, JComponent comp) {
        Object oldComp = propToComps.get(propId);
        if (oldComp != null) {
            throw new IllegalStateException(
                    "Already have a component defined:" + propId);
        }
        propToComps.put(propId, comp);
        return comp;
    }

    private String truncateNumericString(String str, int numDec) {
        int indx = str.indexOf(".") + numDec + 1;
        if (indx >= str.length()) indx = str.length();
        return str.substring(0,indx);
    }
    public int getLineMag() {
        return this.lineMag;
    }
    public void setLineMag(int val) {
        if (val > SLIDER_MAX) val = SLIDER_MAX;
        if (val < SLIDER_MIN-1) val = SLIDER_MIN-1;
        if (val == -1) val = 1;
        this.lineMag = val;
    }

    private void setLineMagSlider(int val) {
        if (val == 1) val = -1;
        if (val > SLIDER_MAX) val = -1;
        if (val < SLIDER_MIN) val = SLIDER_MIN-1;
        lineMagSlider.setValue(val + 1);
    }


    public int getElementMag() {
        return this.elementMag;
    }

    private void setLineMag() {
        int val = 1;
        try {
            val = Integer.parseInt(lineMagFld.getText().trim());
        } catch (Exception e) {
        }
        setLineMag(val);
    }
    public void setElementMag(int val) {
        if (val > SLIDER_MAX) val = SLIDER_MAX;
        if (val < SLIDER_MIN-1) val = SLIDER_MIN-1;
        if (val == -1) val = 1;
        this.elementMag = val;

    }

    protected void elementMagSliderChanged(boolean recomputeLineEleRatio) {
        int value = getElementMag();

        value = getElementMagValue();
        setElementMag(value);

        elementMagLbl.setText("Ele  Mag=");
        eleMagFld.setText(new Integer(value).toString());
        String str = " Res=" +
                truncateNumericString(Double.toString(this.baseERes*Math.abs(value)), 1);
        elementResLbl.setText(StringUtil.padLeft(str, 4) + kmLbl);


    }

    private void changeLineMagSlider(boolean autoSetSize) {
        int value = getLineMag();
        setLineMagSlider(value);
    }

    private void changeEleMagSlider(boolean autoSetSize) {
        int value = getElementMag();
        setElementMagSlider(value);
    }
    private void setElementMagSlider(int val) {
        if (val == 1) val = -1;
        if (val > SLIDER_MAX) val = -1;
        if (val < SLIDER_MIN) val = SLIDER_MIN-1;
        elementMagSlider.setValue(val + 1);
    }
    /**
     * Handle the line mag slider changed event
     *
     * @param autoSetSize  the event
     */
    protected void lineMagSliderChanged(boolean autoSetSize) {
        try {
            int value = getLineMag();

            value = getLineMagValue();
            setLineMag(value);

            lineMagLbl.setText("Line Mag=");
            lineMagFld.setText(new Integer(value).toString());
            String str = " Res=" +
                    truncateNumericString(Double.toString(this.baseLRes*Math.abs(value)), 1);
            lineResLbl.setText(StringUtil.padLeft(str, 4) + kmLbl);

            //amSettingProperties = true;
            //setElementMag(value);
            //setElementMagSlider(value);
            //amSettingProperties = false;
            // elementMagSliderChanged(false);
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




    public String getFileName()
    {
        return this.source;
    }


    protected JComponent doMakeContents()
    {
        return MasterPanel;
    }

    public void applyToDataSelection(DataSelection dataSelection)
    {
        ProjectionRect rect = null;
        if(rect == null)
        {
            // no region subset, full image
        } else {
            rect.getBounds() ;
            GeoLocationInfo bbox = GeoSelection.getDefaultBoundingBox();
            if (bbox != null) {
                this.geoSelection = new GeoSelection(bbox);
            }
        }


    }

    public boolean getIsProgressiveResolution(){
        return this.prograssiveCbx.isSelected();
    }
}