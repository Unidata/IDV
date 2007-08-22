/*
 * $Id: NavigatedDisplayCursorReadout.java,v 1.23 2006/04/04 21:41:18 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.view.geoloc;



import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.text.DecimalFormat;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import visad.*;

import java.beans.*;

import java.awt.*;


/**
 * A readout of the lat/lon/alt position of the cursor in a NavigatedDisplay.
 *
 * @author Unidata development team
 * @version $Revision
 */
public class NavigatedDisplayCursorReadout extends JPanel {

    /** navigated display */
    private NavigatedDisplay navDisplay;

    /** latitude readout */
    private Real latitude;

    /** longitude readout */
    private Real longitude;

    /** altitude readout */
    private Real altitude;

    /** listener for latitude changes */
    private PropertyChangeListener latitudeChangeListener;

    /** listener for longitude changes */
    private PropertyChangeListener longitudeChangeListener;

    /** listener for altitude changes */
    private PropertyChangeListener altitudeChangeListener;

    /** value display */
    private JLabel valueDisplay;

    /** flag for whether the label is from this object or passed in */
    private boolean myOwnLabel = true;

    /** active flag */
    private boolean active = true;

    /** Decimal format */
    private DecimalFormat formatter = new DecimalFormat("##0.0");

    /** decimal format pattern */
    private String formatPattern = "##0.0";

    /**
     * Default bean constructor
     */
    protected NavigatedDisplayCursorReadout() {}

    /**
     * Create a new readout for the specified display
     *
     * @param navDisplay  display to listen to
     *
     */
    public NavigatedDisplayCursorReadout(NavigatedDisplay navDisplay) {
        this(navDisplay, new JLabel());
        myOwnLabel = true;
        add(valueDisplay);
    }


    /**
     * Create a new readout and update the specified label.
     *
     * @param navDisplay   display to listen to
     * @param label        label to update (may be null)
     *
     */
    public NavigatedDisplayCursorReadout(NavigatedDisplay navDisplay,
                                         JLabel label) {
        this.navDisplay = navDisplay;
        setLayout(new FlowLayout(FlowLayout.LEFT));
        if (label != null) {
            myOwnLabel = true;
        } else {
            myOwnLabel = false;
        }
        valueDisplay           = label;
        latitudeChangeListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                latitude = (Real) event.getNewValue();
                setLabelText();
            }
        };

        longitudeChangeListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                // fudge to -180/180
                Real lon = (Real) event.getNewValue();
                try {
                    lon = new Real(
                        RealType.Longitude,
                        Misc.normalizeLongitude(
                            lon.getValue(CommonUnit.degree)));
                } catch (Exception e) {}
                longitude = lon;
                setLabelText();
            }
        };

        altitudeChangeListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                Real alt = (Real) event.getNewValue();
                try {
                    Unit u =
                        NavigatedDisplayCursorReadout.this.navDisplay
                            .getVerticalRangeUnit();
                    alt = new Real(RealType.Altitude, alt.getValue(u), u);
                } catch (Exception e) {}
                altitude = alt;
                setLabelText();
            }
        };

        setNavigatedDisplay(navDisplay);
    }

    /**
     * We're not used anymore. Clean up, remove listeners
     */
    public void destroy() {
        removeListeners();
        latitudeChangeListener  = null;
        longitudeChangeListener = null;
        altitudeChangeListener  = null;
    }



    /**
     * Remove the listeners
     */
    private void removeListeners() {
        if (navDisplay != null) {
            navDisplay.removePropertyChangeListener(latitudeChangeListener);
            navDisplay.removePropertyChangeListener(longitudeChangeListener);
            navDisplay.removePropertyChangeListener(altitudeChangeListener);
        }
    }


    /**
     * Set the navigated display that this panel listens to
     *
     * @param navDisplay   navigated display
     */
    public void setNavigatedDisplay(NavigatedDisplay navDisplay) {
        NavigatedDisplay oldNavDisplay = this.navDisplay;
        removeListeners();

        this.navDisplay = navDisplay;
        this.navDisplay.addPropertyChangeListener(
            NavigatedDisplay.CURSOR_LATITUDE, latitudeChangeListener);
        this.navDisplay.addPropertyChangeListener(
            NavigatedDisplay.CURSOR_LONGITUDE, longitudeChangeListener);
        this.navDisplay.addPropertyChangeListener(
            NavigatedDisplay.CURSOR_ALTITUDE, altitudeChangeListener);

    }

    /**
     * Use the label to show the readout
     *
     * @param label Label to use
     */
    public void setValueDisplay(JLabel label) {
        valueDisplay = label;
    }


    /**
     * get the readout label
     *
     * @return The readout label
     */
    protected JLabel getValueDisplay() {
        return valueDisplay;
    }


    /**
     * Set that readout active/inactive
     *
     * @param value  true to set active
     */
    public void setActive(boolean value) {
        active = value;
        JLabel label = getValueDisplay();
        if ( !active && (label != null)) {
            label.setText(" ");
        }
    }

    /**
     * Get whether this panel is active or not.
     * @return active state (true if active)
     */
    public boolean getActive() {
        return active;
    }


    /**
     * Set the format pattern for this readout
     *
     * @param pattern decimal format pattern
     */
    public void setFormatPattern(String pattern) {
        formatter.applyPattern(pattern);
        formatPattern = pattern;
        setLabelText();
    }

    /**
     * Set the label text
     */
    private void setLabelText() {
        if ( !active) {
            return;
        }
        StringBuffer buf = new StringBuffer();
        buf.append(" Latitude: ");
        if (latitude != null) {
            if (Double.isNaN(latitude.getValue())) {
                buf.append("NA");
            } else {
                try {
                    buf.append(
                        StringUtil.padLeft(
                            formatter.format(
                                latitude.getValue(CommonUnit.degree)), 6));
                } catch (Exception e) {}
            }
        }
        buf.append(" Longitude: ");
        if (longitude != null) {
            if (Double.isNaN(longitude.getValue())) {
                buf.append("NA");
            } else {
                try {
                    buf.append(
                        StringUtil.padLeft(
                            formatter.format(
                                longitude.getValue(CommonUnit.degree)), 6));
                } catch (Exception e) {}
            }
        }
        if (navDisplay.getDisplayMode() == NavigatedDisplay.MODE_3D) {
            buf.append(" Altitude: ");
            if (altitude != null) {
                if (Double.isNaN(altitude.getValue())) {
                    buf.append("NA");
                } else {
                    try {
                        buf.append(
                            StringUtil.padLeft(
                                formatter.format(altitude.getValue()), 6));
                        buf.append(" ");
                        buf.append(altitude.getUnit().toString());
                    } catch (Exception e) {}
                }
            }
        }
        String text  = buf.toString();
        JLabel label = getValueDisplay();
        if (label != null) {
            label.setText(text);
            if (myOwnLabel) {
                FontMetrics fm = label.getFontMetrics(label.getFont());
                label.setPreferredSize(new Dimension(fm.stringWidth(text),
                                                     fm.getHeight()));
            }
        }
    }
}

