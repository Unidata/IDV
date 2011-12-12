/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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


import visad.*;

import visad.java2d.*;

import java.awt.Color;



import java.rmi.RemoteException;


/**
 * Provides support for line drawings.  The color and width of the lines can be
 * controlled using the methods of this class.
 *
 * <p>
 * Instances of this class have the following bound properties:<br>
 * <table border align=center>
 *
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Access</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 *
 * <tr align=center>
 * <td>color</td>
 * <td>java.awt.Color</td>
 * <td>set/get</td>
 * <td>Color.white</td>
 * <td align=left>The color of rendered lines or points.</td>
 * </tr>
 *
 * <tr align=center>
 * <td>lineWidth</td>
 * <td>float</td>
 * <td>set/get</td>
 * <td>1.0f</td>
 * <td align=left>The width of rendered lines.</td>
 * </tr>
 *
 * <tr align=center>
 * <td>lineStyle</td>
 * <td>int</td>
 * <td>set/get</td>
 * <td>0</td>
 * <td align=left>The style of rendered lines.</td>
 * </tr>
 *
 * <tr align=center>
 * <td>pointSize</td>
 * <td>float</td>
 * <td>set/get</td>
 * <td>1.0f</td>
 * <td align=left>The size of rendered points.</td>
 * </tr>
 *
 * </table>
 *
 * @author Steven R. Emmerson
 * @author Don Murray
 * @version $Revision: 1.21 $
 */
public class LineDrawing extends DisplayableData {

    /**
     * The name of the color property.
     */
    public static String COLOR = "color";

    /**
     * The name of the line-width property.
     */
    public static String LINE_WIDTH = "lineWidth";

    /**
     * The name of the line-style property.
     */
    public static String LINE_STYLE = "lineStyle";

    /**
     * The name of the point-size property.
     */
    public static String POINT_SIZE = "pointSize";

    /** color for linedrawing */
    private Color myColor;

    /** line widthe for linedrawing */
    private float myLineWidth;

    /** point size for linedrawing */
    private float myPointSize;

    /** style for linedrawing */
    private int myLineStyle;

    /**
     * Constructs an instance with the specified name
     *
     * @param  name  name for the instance
     * @throws VisADException     Can't create the necessary VisAD object
     * @throws RemoteException    Can't create the necessary remote object
     */
    public LineDrawing(String name) throws VisADException, RemoteException {

        super(name);

        myColor     = Color.white;
        myLineWidth = 1.0f;
        myPointSize = 1.0f;
        myLineStyle = 0;
    }

    /**
     * Constructs from another instance.  The following attributes are copied from
     * the other instance: color, line width, and point size.
     * @param that                The other instance.
     * @throws VisADException     Can't create the necessary VisAD object
     * @throws RemoteException    Can't create the necessary remote object
     */
    public LineDrawing(LineDrawing that)
            throws VisADException, RemoteException {

        super(that);

        myColor     = that.myColor;
        myLineWidth = that.myLineWidth;
        myPointSize = that.myPointSize;
        myLineStyle = that.myLineStyle;
    }

    /**
     * Sets the RGB values to control the color of this Displayable.
     *
     * @param   red     red value (0 - 1)
     * @param   green   green value (0 - 1)
     * @param   blue    blue value (0 - 1)
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setRGB(double red, double green, double blue)
            throws VisADException, RemoteException {
        setRGBA(red, green, blue, 1.0);
    }

    /**
     * Sets the RGBA values to control the color of this Displayable.
     *
     * @param   red     red value (0 - 1)
     * @param   green   green value (0 - 1)
     * @param   blue    blue value (0 - 1)
     * @param   alpha   alpha value (0 - 1)
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setRGBA(double red, double green, double blue, double alpha)
            throws VisADException, RemoteException {
        setColor(new Color((float) red, (float) green, (float) blue,
                           (float) alpha));
    }

    /**
     * Sets the RGB values to control the color of this Displayable.
     *
     * @param   rgb     array of red, green, blue intesities (all 0 - 1).
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setRGB(float[] rgb) throws VisADException, RemoteException {
        setRGB(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Sets the RGBA values to control the color of this Displayable.
     *
     * @param   rgba     array of red, green, blue, alpha intesities (all 0 - 1).
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setRGBA(float[] rgba) throws VisADException, RemoteException {
        setRGBA(rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    /**
     * Sets the HSV values to control the color of this Displayable.
     *
     * @param   hue          hue value (red=0, green=120, blue=240).
     * @param   saturation   saturation value (0 - 1).
     * @param   value        brightness value (0 - 1).
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setHSV(double hue, double saturation, double value)
            throws VisADException, RemoteException {

        float[][] rgb = Display.DisplayHSVCoordSys.toReference(new float[][] {
            { (float) hue }, { (float) saturation }, { (float) value }
        });

        setRGB(new float[] { rgb[0][0], rgb[1][0], rgb[2][0] });
    }

    /**
     * Sets the HSV values to control the color of this Displayable.
     *
     * @param hsv         array of hue (red=0, green=120, blue=240),
     *                    saturation (0-1), and brightness (0-1) values.
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setHSV(float[] hsv) throws VisADException, RemoteException {
        setHSV(hsv[0], hsv[1], hsv[2]);
    }

    /**
     * Sets the color of the lines for this Displayable.
     *
     * @param   color     color for the line.
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setColor(Color color) throws VisADException, RemoteException {

        Color oldValue;

        synchronized (this) {
            oldValue = myColor;

            addConstantMaps(new ConstantMap[] {
                new ConstantMap(color.getRed() / 255., Display.Red),
                new ConstantMap(color.getGreen() / 255., Display.Green),
                new ConstantMap(color.getBlue() / 255., Display.Blue),
                new ConstantMap(color.getAlpha() / 255., Display.Alpha) });

            myColor = color;
        }

        firePropertyChange(COLOR, oldValue, myColor);
    }

    /**
     * Sets the width of lines in this Displayable.
     *
     * @param   lineWidth     Width of lines (1 = normal)
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setLineWidth(float lineWidth)
            throws VisADException, RemoteException {

        float oldValue;

        synchronized (this) {
            oldValue = myLineWidth;
            addConstantMap(new ConstantMap(lineWidth, Display.LineWidth));
            myLineWidth = lineWidth;
        }

        firePropertyChange(LINE_WIDTH, new Float(oldValue),
                           new Float(myLineWidth));
    }

    /**
     * Sets the style of lines in this Displayable.
     *
     * @param   lineStyle     style of line
     * @see visad.GraphicsModeControl
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setLineStyle(int lineStyle)
            throws VisADException, RemoteException {

        int oldValue;

        synchronized (this) {
            oldValue = myLineStyle;

            addConstantMap(new ConstantMap(lineStyle, Display.LineStyle));

            myLineStyle = lineStyle;
        }

        firePropertyChange(LINE_STYLE, new Integer(oldValue),
                           new Integer(myLineStyle));
    }

    /**
     * Sets the size of points in this Displayable.
     *
     * @param   pointSize     Size of points (2 = normal)
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setPointSize(float pointSize)
            throws VisADException, RemoteException {

        float oldValue;

        synchronized (this) {
            oldValue = myPointSize;

            addConstantMap(new ConstantMap(pointSize, Display.PointSize));

            myPointSize = pointSize;
        }

        firePropertyChange(POINT_SIZE, new Float(oldValue),
                           new Float(myPointSize));
    }

    /**
     * Gets the current java.awt.Color associated with this LineDrawing
     *
     * @return  color value
     */
    public Color getColor() {
        return myColor;
    }

    /**
     * Gets the current line width associated with this LineDrawing
     *
     * @return  line width
     */
    public float getLineWidth() {
        return myLineWidth;
    }

    /**
     * Gets the current line style associated with this LineDrawing
     *
     * @return  line style
     * @see visad.GraphicsModeControl
     */
    public int getLineStyle() {
        return myLineStyle;
    }

    /**
     * Gets the point size associated with this LineDrawing
     *
     * @return  point size
     */
    public float getPointSize() {
        return myPointSize;
    }


}
