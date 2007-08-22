/*
 * $Id: Renderer.java,v 1.12 2005/05/13 18:33:06 jeffmc Exp $
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

package ucar.unidata.view;



/**
 * A Renderer does the actual work of drawing objects.
 *
 * @author John Caron
 * @version $Id: Renderer.java,v 1.12 2005/05/13 18:33:06 jeffmc Exp $
 */
public interface Renderer {

    /**
     * Tell the renderer to draw itself.
     *  The Graphics2D object has its AffineTransform set to transform World coordinates to display coordinates.
     *  Typically the Renderer does its drawing in World coordinates, and does not modify the AffineTransform.
     *  If the Renderer wants to draw in constant-pixel coordinates, so that its objects do not change as
     *   the user zooms in and out, use the pixelAT transform, which transforms "Normalized Device"
     *   coordinates (screen pixels) to Device coordinates.
     *  The Graphics2D object also has its clipping rectangle set (in World coordinates), which the Renderer may
     *    use for optimization.
     *  The Graphics2D object has default color and line width set; the Renderer should restore any changes it makes.
     * @param g         the Graphics context
     * @param pixelAT   transforms "Normalized Device" to Device coordinates.  When drawing to the screen,
     *   this will be the identity transform. For other devices like printers, it is not the Identity transform.
     *   Renderers should use "Normalized Device" coordinates if they want to render non-scalable objects.
     *   Basically, you pretend you are working in screen pixels.
     * @see ucar.unidata.view.station.StationLocationRenderer for an example using pixelAT.
     */
    public void draw(java.awt.Graphics2D g,
                     java.awt.geom.AffineTransform pixelAT);

    /**
     * Tell the Renderer to use the given projection from now on.
     *  @param project the projection to use.
     */
    public void setProjection(ucar.unidata.geoloc.ProjectionImpl project);

    /**
     * Tell the Renderer to use the given color.
     *  @param color the Color to use.
     */
    public void setColor(java.awt.Color color);

    /**
     * Get the color
     * @return color
     */
    public java.awt.Color getColor();

    /**
     * This allows application to automatically switch to some special area defined by the Renderer
     *  @return LatLonRect or null.
     */
    public ucar.unidata.geoloc.LatLonRect getPreferredArea();
}

