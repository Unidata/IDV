/*
 * $Id: MapBean.java,v 1.8 2005/05/13 18:29:33 jeffmc Exp $
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

package ucar.unidata.gis;



/**
 * Wrap map Renderers as beans.
 *  This interface allows us to do a cute demo of dropping a bean into an application
 *  and having it show up on the toolbar and menu.
 *
 *  @author John Caron
 *  @version $Id: MapBean.java,v 1.8 2005/05/13 18:29:33 jeffmc Exp $
 */


public interface MapBean {

    /**
     * Construct the Action that is called when this bean's menu item/buttcon is selected.
     *  Typically this routine is only called once when the bean is added.
     *  The Action itself is called whenever the menu/buttcon is selected.
     *
     *  The action should have NAME, SMALL_ICON and SHORT_DESC properties set.
     *  The applications uses these to put up a buttcon and menu item.
     *  The actionPerformed() method may do various things, but it must
     *  send a NewRendererEvent to any listeners, if a new renderer (map) is chosen.
     *  @return the Action to be called.
     */
    public javax.swing.Action getAction();

    /**
     * each bean has one Renderer, made current when Action is called
     * @return _more_
     */
    public ucar.unidata.view.Renderer getRenderer();

    /**
     * this bean is a source of NewRendererEvent events
     *
     * @param l
     */
    public void addNewRendererListener(
        ucar.unidata.view.NewRendererListener l);

    /**
     * _more_
     *
     * @param l
     */
    public void removeNewRendererListener(
        ucar.unidata.view.NewRendererListener l);
}

/* Change History:
   $Log: MapBean.java,v $
   Revision 1.8  2005/05/13 18:29:33  jeffmc
   Clean up the odd copyright symbols

   Revision 1.7  2005/03/10 18:38:29  jeffmc
   jindent and javadoc

   Revision 1.6  2004/02/27 21:21:52  jeffmc
   Lots of javadoc warning fixes

   Revision 1.5  2004/01/29 17:35:22  jeffmc
   A big sweeping checkin after a big sweeping reformatting
   using the new jindent.

   jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
   templates there is a '_more_' to remind us to fill these in.

   Revision 1.4  2000/08/18 04:15:25  russ
   Licensed under GNU LGPL.

   Revision 1.3  1999/06/03 01:43:54  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:26:21  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:42  caron
   startAgain

# Revision 1.4  1999/03/16  16:56:59  caron
# fix StationModel editing; add TopLevel
#
# Revision 1.3  1999/03/03  19:58:13  caron
# more java2D changes
#
# Revision 1.2  1999/02/24  21:10:21  caron
# corrections for Solaris
#
*/








