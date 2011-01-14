/*
 * $Id: ThreeDSize.java,v 1.8 2007/03/22 11:21:29 jeffmc Exp $
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
 * Holds a simple  ThreeDSize (x,y,z)
 *
 *
 */
public class ThreeDSize {

    /** _more_ */
    public int sizeX=0;
    public int sizeY=0;
    public int sizeZ=1;

    public ThreeDSize() {
    }

    public ThreeDSize(int sizeX, int sizeY) {
        this(sizeX, sizeY, 1);
    }


    /**
     * _more_
     *
     * @param sizeX
     * @param sizeY
     * @param sizeZ
     *
     */
    public ThreeDSize(int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    /**
     * _more_
     *
     * @param lengths
     *
     */
    public ThreeDSize(int[] lengths) {
        this(lengths[0], lengths[1], lengths[2]);
    }

    /**
     * _more_
     *
     * @param s
     *
     */
    public ThreeDSize(ThreeDSize s) {
        this(s.sizeX, s.sizeY, s.sizeZ);
    }


    public int getSize() {
        return sizeX*sizeY*sizeZ;
    }

    /**
     * _more_
     * @return _more_
     */
    public int getSizeX() {
        return sizeX;
    }

    /**
     * _more_
     *
     * @param s
     */
    public void setSizeX(int s) {
        sizeX = s;
    }

    /**
     * _more_
     * @return _more_
     */
    public int getSizeY() {
        return sizeY;
    }

    /**
     * _more_
     *
     * @param s
     */
    public void setSizeY(int s) {
        sizeY = s;
    }

    /**
     * _more_
     * @return _more_
     */
    public int getSizeZ() {
        return sizeZ;
    }

    /**
     * _more_
     *
     * @param s
     */
    public void setSizeZ(int s) {
        sizeZ = s;
    }


    /**
     * _more_
     * @return _more_
     */
    public String toString() {
        return " Size X: " + sizeX + " Size Y:" + sizeY + (sizeZ<=1?"":" Size Z:"
							   + sizeZ);
    }


    /**
     * _more_
     * @return _more_
     */
    public String getLabel() {
        return sizeX + " X  " + sizeY + " X " + sizeZ;
    }




}

