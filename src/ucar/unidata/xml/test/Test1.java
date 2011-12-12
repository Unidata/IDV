/*
 * $Id: Test1.java,v 1.5 2005/05/13 18:33:58 jeffmc Exp $
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

package ucar.unidata.xml.test;



import ucar.unidata.xml.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;

import java.lang.reflect.*;

import org.w3c.dom.Element;


/**
 * Class Test1
 *
 *
 * @author IDV development team
 */
public class Test1 extends Test {

    /** Field cnt */
    static int cnt = 0;

    /**
     * getx
     * @return x
     */
    public String getX() {

        return "FOO";
    }

    /**
     * set x
     *
     * @param v x
     */
    public void setX(String v) {}

    /** Field value_byte */
    public byte value_byte;

    /**
     * get byte
     * @return byte
     */
    public byte get_byte() {
        return value_byte;
    }

    /**
     * set byte
     *
     * @param v byte
     */
    public void set_byte(byte v) {

        value_byte = v;

        // System.err.println ("set byte" + v);
    }

    /** Field value_char */
    public char value_char;

    /**
     * get_char
     *
     * @return char
     */
    public char get_char() {

        return value_char;
    }

    /**
     * set_char
     *
     * @param v chat
     */
    public void set_char(char v) {

        value_char = v;

        // System.err.println ("set char" + v);
    }

    /** Field value_short */
    public short value_short;

    /**
     * short
     * @return short
     */
    public short get_short() {

        return value_short;
    }

    /**
     * short
     *
     * @param v short
     */
    public void set_short(short v) {

        value_short = v;

        // System.err.println ("set short" + v);
    }

    /** Field value_int */
    public int value_int;

    /**
     * int
     * @return int
     */
    public int get_int() {

        return value_int;
    }

    /**
     * int
     *
     * @param v int
     */
    public void set_int(int v) {

        value_int = v;

        // System.err.println ("set int" + v);
    }

    /** Field value_long */
    public long value_long;

    /**
     * long
     * @return long
     */
    public long get_long() {
        return value_long;
    }

    /**
     * long
     *
     * @param v long
     */
    public void set_long(long v) {

        value_long = v;

        // System.err.println ("set long" + v);
    }

    /** Field value_float */
    public float value_float;

    /**
     * float
     * @return float
     */
    public float get_float() {

        return value_float;
    }

    /**
     * float
     *
     * @param v float
     */
    public void set_float(float v) {

        value_float = v;

        // System.err.println ("set float" + v);
    }

    /** Field value_double */
    public double value_double;

    /**
     * double
     * @return double
     */
    public double get_double() {

        return value_double;
    }

    /**
     * double
     *
     * @param v double
    */
    public void set_double(double v) {

        value_double = v;

        // System.err.println ("set double" + v);
    }

    /**
     * dummy ctor
     *
     */
    public Test1() {

        value_byte   = (byte) (cnt++);
        value_char   = 'a';
        value_short  = (short) (cnt++);
        value_int    = (int) (cnt++);
        value_long   = (long) (cnt++);
        value_float  = (float) (cnt++);
        value_double = (double) (cnt++);
    }
}

























