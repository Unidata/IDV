/*
 * $Id: CellCycleException.java,v 1.5 2005/05/13 18:33:25 jeffmc Exp $
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

package ucar.unidata.view.sounding;



import visad.DataReference;


/**
 * The exception thrown when a cycle is dectected in directed graph of {@link
 * ComputeCell}s.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:33:25 $
 */
public final class CellCycleException extends RuntimeException {

    /** ancestor data reference */
    private DataReference ancestor;

    /** child ref */
    private DataReference child;

    /** found cell */
    private ComputeCell found;

    /**
     * Constructs from nothing.
     */
    public CellCycleException() {}

    /**
     * Sets the ancestor data cell.
     *
     * @param cell              The ancestor cell.
     */
    public void setAncestor(DataReference cell) {
        ancestor = cell;
    }

    /**
     * Sets the child data reference.
     *
     * @param cell                  The child cell.
     */
    public void setChild(DataReference cell) {
        child = cell;
    }

    /**
     * Sets the output cell that caused the cycle.
     *
     * @param cell                  The output cell that caused the cycle.
     */
    public void setFound(ComputeCell cell) {
        found = cell;
    }

    /**
     * Returns the message associated with this instance.
     *
     * @return                      This instances message.
     */
    public String getMessage() {
        return "ancestor=" + ancestor + ", child=" + child + ", found="
               + found;
    }

    /**
     * Returns the {@link ComputeCell} at which a cycle was detected.
     *
     * @return                      The {@link ComputeCell} iat which a cycle was
     *                              detected.
     */
    public ComputeCell getComputeCell() {
        return found;
    }
}







