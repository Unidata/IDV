/*
 * $Id: NewRendererEvent.java,v 1.7 2005/05/13 18:33:04 jeffmc Exp $
 *
 * Copyright  1997-2025 Unidata Program Center/University Corporation for
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
 * New Renderer event.
 * @author John Caron
 * @version $Id: NewRendererEvent.java,v 1.7 2005/05/13 18:33:04 jeffmc Exp $
 */
public class NewRendererEvent extends java.util.EventObject {

    /** renderer */
    private ucar.unidata.view.Renderer rend;

    /**
     * Constructor.
     *
     * @param source
     * @param rend
     */
    public NewRendererEvent(Object source, ucar.unidata.view.Renderer rend) {
        super(source);
        this.rend = rend;
    }

    /**
     * get the new renderer
     * @return renderer
     */
    public ucar.unidata.view.Renderer getRenderer() {
        return rend;
    }
}
