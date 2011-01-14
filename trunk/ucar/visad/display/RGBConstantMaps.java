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



import java.awt.Color;

import java.rmi.RemoteException;


/**
 * Provides support for a modifiable composite of adapted red, green, and blue,
 * VisAD ConstantMap-s.
 *
 * <p>Instances of this class have the following, bound, JavaBean
 * properties:<br>
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
 * <td>{@link java.awt.Color}</td>
 * <td>set/get</td>
 * <td>white</td>
 * <td align=left>The color of this instance</td>
 * </tr>
 *
 * </table>
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $
 */
public class RGBConstantMaps extends TupleOfConstantMaps {

    /**
     * The name of the color property.
     */
    public static final String COLOR = "color";

    /** constant for red */
    private ConstantMapAdapter red;

    /** constant for green */
    private ConstantMapAdapter green;

    /** constant for blue */
    private ConstantMapAdapter blue;

    /** color */
    private Color color;

    /**
     * Constructs.  The initial color is white.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RGBConstantMaps() throws VisADException, RemoteException {
        this(Color.white);
    }

    /**
     * Constructs.
     *
     * @param color             The initial color for this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RGBConstantMaps(Color color)
            throws VisADException, RemoteException {

        try {
            add(red = new ConstantMapAdapter(Display.Red));
            add(green = new ConstantMapAdapter(Display.Green));
            add(blue = new ConstantMapAdapter(Display.Blue));
        } catch (BadMappingException e) {}  // can't happen

        setColor(color);
    }

    /**
     * Sets the color of this instance.
     *
     * @param color             Color for this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void setColor(Color color)
            throws VisADException, RemoteException {

        if ( !color.equals(this.color)) {
            red.setValue(color.getRed() / 255.);
            green.setValue(color.getRed() / 255.);
            blue.setValue(color.getRed() / 255.);

            Color oldColor = color;

            this.color = color;

            firePropertyChange(COLOR, oldColor, this.color);
        }
    }

    /**
     * Returns the color of this instance.  This implementation just returns its
     * argument.
     *
     * @param color             The color of this instance.
     * @return                  The color of this instance.
     */
    public synchronized Color getColor(Color color) {
        return color;
    }
}
