/*
 * $Id: UIChangeEvent.java,v 1.8 2005/05/13 18:32:16 jeffmc Exp $
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

package ucar.unidata.ui.event;



import java.awt.Rectangle;


/**
 * Change events for UI objects.
 * @author John Caron
 * @version $Id: UIChangeEvent.java,v 1.8 2005/05/13 18:32:16 jeffmc Exp $
 */
public class UIChangeEvent extends java.util.EventObject {

    /** _more_ */
    private String property;

    /** _more_ */
    private Object objectChanged;

    /** _more_ */
    private Object newValue;

    /**
     * _more_
     *
     * @param source
     * @param property
     * @param changed
     * @param newValue
     *
     */
    public UIChangeEvent(Object source, String property, Object changed,
                         Object newValue) {
        super(source);
        this.property      = property;
        this.objectChanged = changed;
        this.newValue      = newValue;
    }

    /**
     * _more_
     * @return _more_
     */
    public String getChangedProperty() {
        return property;
    }

    /**
     * _more_
     * @return _more_
     */
    public Object getChangedObject() {
        return objectChanged;
    }

    /**
     * _more_
     * @return _more_
     */
    public Object getNewValue() {
        return newValue;
    }

    /**
     * _more_
     * @return _more_
     */
    public String toString() {
        return "UIChangeEvent: " + property + " objectChanged: "
               + objectChanged + "  newValue: " + newValue;
    }
}

/* Change History:
   $Log: UIChangeEvent.java,v $
   Revision 1.8  2005/05/13 18:32:16  jeffmc
   Clean up the odd copyright symbols

   Revision 1.7  2005/03/10 18:39:50  jeffmc
   jindent and javadoc

   Revision 1.6  2004/02/27 21:19:32  jeffmc
   Lots of javadoc warning fixes

   Revision 1.5  2004/01/29 17:37:22  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.4  2000/08/18 04:16:03  russ
   Licensed under GNU LGPL.

   Revision 1.3  1999/06/03 01:44:15  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:27:09  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:49  caron
   startAgain

# Revision 1.3  1999/03/16  17:00:59  caron
# fix StationModel editing; add TopLevel
#
# Revision 1.2  1998/12/14  17:12:03  russ
# Add comment for accumulating change histories.
#
*/





