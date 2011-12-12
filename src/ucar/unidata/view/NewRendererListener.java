/*
 * $Id: NewRendererListener.java,v 1.6 2005/05/13 18:33:05 jeffmc Exp $
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

package ucar.unidata.view;



/**
 * Listeners for New Renderer events.
 * @author John Caron
 * @version $Id: NewRendererListener.java,v 1.6 2005/05/13 18:33:05 jeffmc Exp $
 */
public interface NewRendererListener extends java.util.EventListener {

    /**
     * handle action
     *
     * @param e param
     */
    public void actionPerformed(NewRendererEvent e);
}

