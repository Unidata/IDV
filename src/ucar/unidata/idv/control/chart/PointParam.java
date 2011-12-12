/*
 * $Id: PointParam.java,v 1.5 2007/04/16 21:32:11 jeffmc Exp $
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

package ucar.unidata.idv.control.chart;


import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;


import visad.Unit;

import java.awt.*;


/**
 * Class PointParam holds information for showing point data
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.5 $
 */
public class PointParam {


    /** line state */
    private LineState lineState = new LineState(null, 1.0f,
                                      LineState.STROKE_SOLID);



    /**
     * ctor
     */
    public PointParam() {}

    /**
     * ctor
     *
     * @param name my name
     */
    public PointParam(String name) {
        lineState.setName(name);
    }

    /**
     * get name
     *
     * @return name
     */
    public String getName() {
        return lineState.getName();
    }

    /**
     *  Set the LineState property.
     *
     *  @param value The new value for LineState
     */
    public void setLineState(LineState value) {
        lineState = value;
    }

    /**
     *  Get the LineState property.
     *
     *  @return The LineState
     */
    public LineState getLineState() {
        return lineState;
    }

    /**
     * tostring
     *
     * @return tostring
     */
    public String toString() {
        return getName();
    }
}


