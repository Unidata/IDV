/*
 * $Id: ValueSliderWidget.java,v 1.8 2007/08/21 14:30:37 dmurray Exp $
 *
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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


import ucar.unidata.idv.ui.ValueSliderComponent;


/**
 * A class for holding a text field and a slider for setting the
 * some integer value
 *
 * @author IDV Development Team
 * @version $Revision: 1.8 $
 */
public class ValueSliderWidget extends ValueSliderComponent {

    /**
     * Create a ValueSliderWidget
     *
     * @param dc  the display control to use
     * @param min minimum slider value
     * @param max maximum slider value
     * @param property DisplayControl property to set
     * @param label  label for the widget
     */
    public ValueSliderWidget(DisplayControlImpl dc, int min, int max,
                             String property, String label) {
        this(dc, min, max, property, label, 1.0f);
    }

    /**
     * Create a ValueSliderWidget
     *
     * @param dc  the display control to use
     * @param min minimum slider value
     * @param max maximum slider value
     * @param property DisplayControl property to set
     * @param label  label for the widget
     * @param scale  scale factor for the values
     */
    public ValueSliderWidget(DisplayControlImpl dc, int min, int max,
                             String property, String label, float scale) {
        super(dc, min, max, property, label, scale, true);
    }

    /**
     * Has the calling object been initialized?  Subclasses should
     * override if necessary.
     * @return true
     */
    public boolean getObjectHasInitialized() {
        return ((DisplayControlImpl) callingObject).getHaveInitialized();
    }

}

