/*
 * $Id: Coord.java,v 1.7 2006/05/05 19:19:33 jeffmc Exp $
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


package ucar.unidata.util;




/**
 * Holds a simple  Coord (x,y,z)
 *
 *
 */
public class Coord {

    /** _more_ */
    public double x, y, z;

    /**
     * _more_
     *
     */
    public Coord() {}

    /**
     * _more_
     *
     * @param x
     * @param y
     * @param z
     *
     */
    public Coord(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * _more_
     *
     * @param x
     * @param y
     * @param z
     *
     */
    public Coord(int x, int y, int z) {
        this((double) x, (double) y, (double) z);
    }

    /**
     * _more_
     *
     * @param lengths
     *
     */
    public Coord(int[] lengths) {
        this(lengths[0], lengths[1], lengths[2]);
    }

    /**
     * _more_
     *
     * @param lengths
     *
     */
    public Coord(double[] lengths) {
        this(lengths[0], lengths[1], lengths[2]);
    }

    /**
     * _more_
     *
     * @param s
     *
     */
    public Coord(Coord s) {
        this(s.x, s.y, s.z);
    }


    /**
     * _more_
     * @return _more_
     */
    public int getIntX() {
        return (int) x;
    }

    /**
     * _more_
     * @return _more_
     */
    public int getIntY() {
        return (int) y;
    }

    /**
     * _more_
     * @return _more_
     */
    public int getIntZ() {
        return (int) z;
    }


    /**
     * _more_
     * @return _more_
     */
    public double getX() {
        return x;
    }

    /**
     * _more_
     *
     * @param s
     */
    public void setX(double s) {
        x = s;
    }

    /**
     * _more_
     * @return _more_
     */
    public double getY() {
        return y;
    }

    /**
     * _more_
     *
     * @param s
     */
    public void setY(double s) {
        y = s;
    }

    /**
     * _more_
     * @return _more_
     */
    public double getZ() {
        return z;
    }

    /**
     * _more_
     *
     * @param s
     */
    public void setZ(double s) {
        z = s;
    }


    /**
     * _more_
     * @return _more_
     */
    public String toString() {
        return " x  = " + x + " y  = " + y + " z  = " + z;
    }




}

