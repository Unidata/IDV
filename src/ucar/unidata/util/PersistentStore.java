/*
 * $Id: PersistentStore.java,v 1.9 2006/05/05 19:19:36 jeffmc Exp $
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
 * Abstraction for storing persistent objects.
 *  HashMap-like interface. objects must be persistent across JVM invocations
 *
 * @author John Caron
 * @version $Id: PersistentStore.java,v 1.9 2006/05/05 19:19:36 jeffmc Exp $
 */

public interface PersistentStore {

    /**
     * get the value specified by this key
     *
     * @param key
     * @return _more_
     */
    public Object get(Object key);

    /* store this key, value pair */

    /**
     * _more_
     *
     * @param key
     * @param value
     */
    public void put(Object key, Object value);

    /** save the current state of the PersistentStore to disk */
    public void save();
}

/*
 *  Change History:
 *  $Log: PersistentStore.java,v $
 *  Revision 1.9  2006/05/05 19:19:36  jeffmc
 *  Refactor some of the tabbedpane border methods.
 *  Also, since I ran jindent on everything to test may as well caheck it all in
 *
 *  Revision 1.8  2005/05/13 18:32:43  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.7  2004/08/19 21:34:46  jeffmc
 *  Scratch log4j
 *
 *  Revision 1.6  2004/02/27 21:18:53  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.5  2004/01/29 17:37:41  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.4  2000/08/18 04:16:12  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.3  1999/06/03 01:44:20  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:27:22  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:50  caron
 *  startAgain
 *
 * # Revision 1.5  1999/03/26  19:58:48  caron
 * # add SpatialSet; update javadocs
 * #
 * # Revision 1.4  1999/03/16  17:01:28  caron
 * # fix StationModel editing; add TopLevel
 * #
 * # Revision 1.3  1999/03/08  19:46:28  caron
 * # world coord now Point2D
 * #
 */

