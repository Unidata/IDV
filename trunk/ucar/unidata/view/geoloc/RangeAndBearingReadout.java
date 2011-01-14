/*
 * $Id: RangeAndBearingReadout.java,v 1.8 2005/10/10 21:55:40 dmurray Exp $
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

import ucar.unidata.view.sounding.RealReadout;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import visad.*;

import java.beans.*;

import java.awt.*;


/**
 * A class for displaying a readout of range and bearing values
 *
 * @author Unidata development team
 * @version $Revision: 1.8 $
 */
public class RangeAndBearingReadout extends JPanel {

    /** map display to listen to */
    private MapProjectionDisplay mapDisplay;

    /** readout for bearing values */
    private RealReadout bearingReadout;

    /** readout for range values */
    private RealReadout rangeReadout;

    /** listener for changes in the range */
    private PropertyChangeListener rangeChangeListener;

    /** listener for changes in the bearing */
    private PropertyChangeListener bearingChangeListener;

    /** display for the value */
    private JLabel valueDisplay;

    /** whether I use my own label */
    private boolean myOwnLabel = true;

    /** flag for active */
    private boolean active = true;

    /** Default label for range values */
    private String rangeName = "Range";

    /** Default label for bearing values */
    private String bearingName = "Bearing";

    /**
     * Create a new RangeAndBearingReadout for the given display
     *
     * @param mapDisplay    display to listen to
     */
    public RangeAndBearingReadout(MapProjectionDisplay mapDisplay) {
        this(mapDisplay, null);
    }

    /**
     * Create a new RangeAndBearingReadout for the given display and use
     * the supplied label
     *
     * @param mapDisplay    display to listen to
     * @param label         label to update
     *
     */
    public RangeAndBearingReadout(MapProjectionDisplay mapDisplay,
                                  JLabel label) {
        this(mapDisplay, label, (String) null, (String) null);
    }

    /**
     * Create a new RangeAndBearingReadout for the given display and use
     * the supplied labels
     *
     * @param mapDisplay    display to listen to
     * @param rangeName     name (label) for range values
     * @param bearingName   name (label) for bearing values
     *
     */
    public RangeAndBearingReadout(MapProjectionDisplay mapDisplay,
                                  String rangeName, String bearingName) {
        this(mapDisplay, null, rangeName, bearingName);
    }

    /**
     * Create a new RangeAndBearingReadout for the given display and use
     * the supplied label to update with the names
     *
     * @param mapDisplay    display to listen to
     * @param label         label to update
     * @param rangeName     name (label) for range values
     * @param bearingName   name (label) for bearing values
     */
    public RangeAndBearingReadout(MapProjectionDisplay mapDisplay,
                                  JLabel label, String rangeName,
                                  String bearingName) {
        this.mapDisplay = mapDisplay;
        setLayout(new FlowLayout(FlowLayout.LEFT));
        if (rangeName != null) {
            this.rangeName = rangeName;
        }
        if (bearingName != null) {
            this.bearingName = bearingName;
        }
        if (label != null) {
            valueDisplay = label;
            myOwnLabel   = false;
        } else {
            valueDisplay = new JLabel(rangeName + ": " + bearingName + ": ",
                                      SwingConstants.LEFT);
            add(valueDisplay);
        }


        rangeReadout = new RealReadout();
        rangeReadout.setFormat(new DecimalFormat("###.0"));
        rangeReadout.setSpecifiedName(rangeName);
        rangeReadout.setNameUse(RealReadout.SPECIFIED_NAME);

        rangeReadout.addPropertyChangeListener(RealReadout.NUMERIC_STRING,
                                               new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                setLabelText();
            }
        });

        bearingReadout = new RealReadout();
        bearingReadout.setFormat(new DecimalFormat("###.0"));
        bearingReadout.setSpecifiedName(bearingName);
        bearingReadout.setNameUse(RealReadout.SPECIFIED_NAME);
        bearingReadout.addPropertyChangeListener(
            RealReadout.NUMERIC_STRING, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                setLabelText();
            }
        });

        rangeChangeListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                rangeReadout.setReal((Real) event.getNewValue());
            }
        };

        bearingChangeListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                bearingReadout.setReal((Real) event.getNewValue());
            }
        };

        setMapProjectionDisplay(mapDisplay);
    }

    /**
     * Set the display to listen to.
     *
     * @param mapDisplay  new map display
     */
    public void setMapProjectionDisplay(MapProjectionDisplay mapDisplay) {
        MapProjectionDisplay oldNavDisplay = this.mapDisplay;
        if (oldNavDisplay != null) {
            oldNavDisplay.removePropertyChangeListener(rangeChangeListener);
            oldNavDisplay.removePropertyChangeListener(bearingChangeListener);
        }

        this.mapDisplay = mapDisplay;
        this.mapDisplay.addPropertyChangeListener(
            MapProjectionDisplay.CURSOR_RANGE, rangeChangeListener);
        this.mapDisplay.addPropertyChangeListener(
            MapProjectionDisplay.CURSOR_BEARING, bearingChangeListener);

    }

    /**
     * Set this active
     *
     * @param value  true to be active
     */
    public void setActive(boolean value) {
        active = value;
        if ( !active && (valueDisplay != null)) {
            valueDisplay.setText(" ");
        }
    }

    /**
     * Get whether this readout is active.
     *
     * @return true if active
     */
    public boolean getActive() {
        return active;
    }


    /**
     * Get the name used for the range label.
     *
     * @return range name
     */
    public String getRangeName() {
        return rangeName;
    }

    /**
     * Set the range name (label)
     *
     * @param newName   new range name
     */
    public void setRangeName(String newName) {
        if (newName != null) {
            rangeName = newName;
        }
    }

    /**
     * Return the name used for bearing values
     *
     * @return bearing label.
     */
    public String getBearingName() {
        return bearingName;
    }

    /**
     * Set the name used for bearing values.
     *
     * @param newName   new bearing name.
     */
    public void setBearingName(String newName) {
        if (newName != null) {
            bearingName = newName;
        }
    }


    /**
     * Set the label text.
     */
    private void setLabelText() {
        if ( !active) {
            return;
        }
        StringBuffer buf = new StringBuffer();
        buf.append(" ");
        buf.append(getRangeName());
        buf.append(": ");
        buf.append(StringUtil.padLeft(rangeReadout.getNumericString(), 6));
        buf.append(" ");
        buf.append(getBearingName());
        buf.append(": ");
        buf.append(StringUtil.padLeft(bearingReadout.getNumericString(), 6));
        String text = buf.toString();
        valueDisplay.setText(text);
        if (myOwnLabel) {
            FontMetrics fm =
                valueDisplay.getFontMetrics(valueDisplay.getFont());
            valueDisplay.setPreferredSize(new Dimension(fm.stringWidth(text),
                                                        fm.getHeight()));
        }
    }
}

