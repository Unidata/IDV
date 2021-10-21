/*
 * $Id: Test3.java,v 1.5 2005/05/13 18:33:58 jeffmc Exp $
 * 
 * Copyright  1997-2022 Unidata Program Center/University Corporation for
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


/**
 * Class Test3
 *
 *
 * @author IDV development team
 */
public class Test3 extends Test {

    /** Field larray */
    List[] larray;

    /** Field array */
    int[] array = { 1, 2, 3 };

    /**
     *ctor
     *
     */
    public Test3() {

        List l1 = new ArrayList();
        l1.add("Hi there");

        List l2 = new ArrayList();
        l2.add("Hi there");
        l2.add("Jeff");

        larray = new List[]{ l1, l2 };
    }

    /**
     * int array
     * @return int array
     *
     */
    public int[] getArray() {

        return array;
    }

    /**
     * int array
     *
     * @param a int array
     */
    public void setArray(int[] a) {

        array = a;
    }

    /**
     * List array
     * @return List array
     */
    public List[] getLArray() {

        return larray;
    }

    /**
     * List array
     *
     * @param a List array
     */
    public void setLArray(List[] a) {

        larray = a;
    }
}

























