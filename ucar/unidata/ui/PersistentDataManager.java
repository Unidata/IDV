/*
 * $Id: PersistentDataManager.java,v 1.9 2007/07/06 20:45:32 jeffmc Exp $
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

package ucar.unidata.ui;



/**
 * Abstracts the services needed for common L&F for managing persistent data.
 * @see ucar.unidata.ui.PersistentDataDialog
 */
public interface PersistentDataManager {

    /** accept/use the current selection */
    public void accept();

    /**
     * does the list contain an Object with this id ?
     *
     * @param id
     * @return _more_
     */
    public boolean contains(String id);

    /** delete the selected object */
    public void deleteSelected();

    /**
     * go into edit mode.
     * @param isNew      true = start new one; false = edit current one
     */
    public void edit(boolean isNew);

}

