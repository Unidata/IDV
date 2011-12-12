/*
 * $Id: LatLonWidget.java,v 1.22 2007/07/06 20:45:31 jeffmc Exp $
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


package ucar.unidata.ui;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;


/**
 * A widget that provides a Lat/Lon entry box.
 */

public class LatLonWidget extends JPanel {

    private boolean doFormat = true;

    /** widget */
    private JTextField latFld;

    /** widget */
    private JTextField lonFld;

    /** widget */
    private JTextField altFld;



    /**
     *  Create a widget with blank values for latitude and longitude and with
     *  the default field labels. Add the given ActionListener to the text fields.
     *
     * @param actionListener
     */
    public LatLonWidget(ActionListener actionListener) {
        this("Latitude: ", "Longitude: ", actionListener);
    }


    /**
     *  Create a widget with the blank values for latitude and longitude and with
     *  the default field labels.
     */
    public LatLonWidget() {
        this("Latitude: ", "Longitude: ", null);
    }


    /**
     *  Create a widget with the blank values for latitude and longitude and with
     *  the given Strings for the field labels. If the given ActionListener is non-null
     *  then add it to the text fields.
     *
     * @param latLabel
     * @param lonLabel
     * @param actionListener
     */
    public LatLonWidget(String latLabel, String lonLabel,
                        ActionListener actionListener) {

        this(latLabel, lonLabel, null, actionListener);
    }

    /**
     *  Create a widget with the blank values for latitude and longitude and with
     *  the given Strings for the field labels. If the given ActionListener is non-null
     *  then add it to the text fields.
     *
     * @param latLabel
     * @param lonLabel
     * @param altLabel If non-null then add an alt field
     * @param actionListener
     */
    public LatLonWidget(String latLabel, String lonLabel, String altLabel,
                        ActionListener actionListener) {

        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        latFld = new JTextField(" ", 5);
        lonFld = new JTextField(" ", 5);
        latFld.setToolTipText("<html> ddd, ddd:mm:ss, etc.</html>");
        lonFld.setToolTipText("<html> ddd, ddd:mm:ss, etc.</html>");
        latFld.setHorizontalAlignment(JTextField.LEFT);
        lonFld.setHorizontalAlignment(JTextField.LEFT);

        if (altLabel != null) {
            altFld = new JTextField(" ", 5);
            altFld.setHorizontalAlignment(JTextField.LEFT);
        }

        if (actionListener != null) {
            latFld.addActionListener(actionListener);
            lonFld.addActionListener(actionListener);
            if (altFld != null) {
                altFld.addActionListener(actionListener);
            }
        }
        add(GuiUtils.rLabel(latLabel));
        add(latFld);
        add(GuiUtils.rLabel("  " + lonLabel));
        add(lonFld);
        if (altLabel != null) {
            add(GuiUtils.rLabel("  " + altLabel));
            add(altFld);
        }
    }

    /**
     *  Create a widget with the given initial values for latitude and longitude.
     *
     * @param lat
     * @param lon
     */
    public LatLonWidget(double lat, double lon) {
        this();
        setLatLon(lat, lon);
    }

    /**
     *  Set the value of the lat and lon fields.
     *
     * @param lat
     * @param lon
     */
    public void setLatLon(double lat, double lon) {
        setLat(lat);
        setLon(lon);
    }

    /**
     *  Set the value of the lat and lon fields.
     *
     * @param lat
     * @param lon
     */
    public void setLatLon(String lat, String lon) {
        setLat(lat);
        setLon(lon);
    }

    /**
     *  Set the value of the lat field with the given String value.
     *
     * @param latString
     */
    public void setLat(String latString) {
        latString = formatLatLonString(latString);
        latFld.setText(latString);
        if ((latString != null) && (latString.length() > 0)) {
            latFld.setCaretPosition(0);
        }
    }


    /**
     *  Set the value of the lon field with the given String value.
     *
     * @param lonString
     */
    public void setLon(String lonString) {
        lonString = formatLatLonString(lonString);
        lonFld.setText(lonString);
        if ((lonString != null) && (lonString.length() > 0)) {
            lonFld.setCaretPosition(0);
        }
    }


    /**
     *  Set the value of the alt field with the given String value.
     *
     * @param altString
     */
    public void setAlt(String altString) {
        altFld.setText(altString);
        if ((altString != null) && (altString.length() > 0)) {
            altFld.setCaretPosition(0);
        }
    }


    /**
     *  Set the value of the latitude field.
     *
     * @param lat
     */
    public void setLat(double lat) {
        setLat("" + lat);
    }

    /**
     *  Set the value of the longitude field.
     *
     * @param lon
     */
    public void setLon(double lon) {
        setLon("" + lon);
    }




    /**
     *  Set the value of the alt field.
     *
     * @param alt
     */
    public void setAlt(double alt) {
        setAlt("" + alt);
    }



    /**
     *  Clear the contents of the lat/lon text fields.
     */
    public void clear() {
        latFld.setText("");
        lonFld.setText("");
        if (altFld != null) {
            altFld.setText("");
        }
    }

    /**
     * See if the value is defined
     *
     * @param s  the  value
     * @return true if it is defined
     */
    public boolean isDefined(String s) {
        s = s.trim();
        if (s.length() == 0) {
            return false;
        }
        return !Double.isNaN(Misc.decodeLatLon(s));
    }


    /**
     *  Is there any text entered in the lat field and is it enabled.
     * @return do we have a lat
     */
    public boolean isLatDefined() {
        return isDefined(latFld.getText()) && latFld.isEnabled();

    }

    /**
     *  Is there any text entered in the lon field  and is it enabled.
     * @return do we have a lon
     */
    public boolean isLonDefined() {
        return isDefined(lonFld.getText()) && lonFld.isEnabled();
    }

    /**
     *  Is there any text entered in the alt field and is it enabled.
     * @return do we have alt
     */
    public boolean isAltDefined() {
        return (altFld != null) && isDefined(altFld.getText())
               && altFld.isEnabled();
    }

    /**
     *  Is there any text entered in both the lat and the lon field.
     * @return true if defined
     */
    public boolean isLatLonDefined() {
        return isLatDefined() && isLonDefined();
    }


    /**
     *  Check if the current field values are valid. If invalid then return
     *  a String error message. If the values are valid then return null.
     *  The fields are invalid if there is non-whitespace text entered
     *  in both fields and the text does not convert to a double
     *  or the values are outside the range (for lat) -90,90 and (for lon) -180,180.
     * @return error message
     */
    public String isValidValues() {
        String lat = latFld.getText().trim();
        String lon = lonFld.getText().trim();
        String msg = "";
        if ((lat.length() == 0) && (lon.length() == 0)) {
            return "";
        }

        if ((lat.length() != 0) && (lon.length() == 0)) {
            return "You must enter a longitude";
        }

        if ((lon.length() != 0) && (lat.length() == 0)) {
            return "You must enter a latitude";
        }

        try {
            double latV = Misc.decodeLatLon(lat);
            if (Double.isNaN(latV)) {
                msg += "Bad latitude value:" + lat + "\n";
            } else if ((latV < -90.0) || (latV > 90.0)) {
                msg += "Latitude not within -90,90\n";
            }
        } catch (NumberFormatException nfe) {
            msg += "Bad latitude value:" + lat + "\n";
        }

        try {
            double lonV = Misc.decodeLatLon(lon);
            if (Double.isNaN(lonV)) {
                msg += "Bad longitude value:" + lon + "\n";
            } else if ((lonV < -180.0) || (lonV > 180.0)) {
                msg += "Longitude not within -180,180\n";
            }
        } catch (NumberFormatException nfe) {
            msg += "Bad longitude value:" + lon + "\n";
        }
        if (msg.length() > 0) {
            return msg;
        }
        return null;
    }

    /**
     * Get a latitude
     * @return the latitude
     *
     * @throws NumberFormatException  illegal number
     */
    public double getLat() throws NumberFormatException {
        return Misc.decodeLatLon(latFld.getText().trim());
    }

    /**
     * Get a longitude
     * @return the longitude
     *
     * @throws NumberFormatException  illegal number
     */
    public double getLon() throws NumberFormatException {
        return Misc.decodeLatLon(lonFld.getText().trim());
    }



    /**
     * Get the altitude
     * @return  the altitude
     *
     * @throws NumberFormatException  illegal number
     */
    public double getAlt() throws NumberFormatException {
        return Misc.parseNumber(altFld.getText().trim());
    }


    /**
     * Get the text field for the latitude.
     *
     * @return The  Lat field.
     */

    public JTextField getLatField() {
        return latFld;
    }

    /**
     * Get the text field for the longitude.
     *
     * @return The  Lon field.
     */
    public JTextField getLonField() {
        return lonFld;
    }

    /**
     * Get the text field for the alt.
     *
     * @return The  alt field.
     */
    public JTextField getAltField() {
        return altFld;
    }

    /**
     * Format the lat or lon string
     *
     * @param latOrLon  lat or lon value as a string in decimal notation
     *
     * @return value in regional formatting
     */
    protected String formatLatLonString(String latOrLon) {
        if(!doFormat) return latOrLon;
        if ((latOrLon == null) || (latOrLon.length() == 0)) {
            return latOrLon;
        }
        String formatted = latOrLon.trim();
        try {
            formatted = Misc.format(Misc.parseDouble(formatted));
        } catch (Exception e) {
            return latOrLon;
        }
        return formatted;
    }

    /**
       Set the DoFormat property.

       @param value The new value for DoFormat
    **/
    public void setDoFormat (boolean value) {
	this.doFormat = value;
    }

    /**
       Get the DoFormat property.

       @return The DoFormat
    **/
    public boolean getDoFormat () {
	return this.doFormat;
    }




}

