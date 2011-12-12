/*
 * $Id: TwoFacedObject.java,v 1.20 2006/06/23 20:17:32 dmurray Exp $
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


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 * @author Metapps development team
 * @version $Revision: 1.20 $ $Date: 2006/06/23 20:17:32 $
 */
public class StringBufferCollection extends ObjectCollection {

    /**
     * _more_
     */
    public StringBufferCollection() {}

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public StringBuffer getBuffer(Object key) {
        StringBuffer sb = (StringBuffer) map.get(key);
        if (sb == null) {
            keys.add(key);
            sb = new StringBuffer();
            map.put(key, sb);
        }
        return sb;
    }



}

