/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

package ucar.visad.display;


import java.awt.Font;


/**
 * AxisScaleInfo JavaBean
 */
public class AxisScaleInfo {

    /** The axis label. */
    private String label;

    /** Is axis visible. */
    private boolean visible;

    /** The axis font. */
    private Font font;

    /**
     * Instantiates a new lat lon scale info.
     */
    public AxisScaleInfo() {}


    /**
     * Gets the label.
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the label.
     *
     * @param label the new label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the font.
     *
     * @return the font
     */
    public Font getFont() {
        return font;
    }

    /**
     * Sets the font.
     *
     * @param font the new font
     */
    public void setFont(Font font) {
        this.font = font;
    }

    /**
     * Checks if is visible.
     *
     * @return true, if is visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets the visible.
     *
     * @param visible the new visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime  = 31;
        int       result = 1;
        result = prime * result + ((font == null)
                                   ? 0
                                   : font.hashCode());
        result = prime * result + ((label == null)
                                   ? 0
                                   : label.hashCode());
        result = prime * result + (visible
                                   ? 1231
                                   : 1237);
        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AxisScaleInfo other = (AxisScaleInfo) obj;
        if (font == null) {
            if (other.font != null) {
                return false;
            }
        } else if ( !font.equals(other.font)) {
            return false;
        }
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        } else if ( !label.equals(other.label)) {
            return false;
        }
        if (visible != other.visible) {
            return false;
        }
        return true;
    }


}
