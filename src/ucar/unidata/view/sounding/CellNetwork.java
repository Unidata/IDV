/*
 * $Id: CellNetwork.java,v 1.7 2005/05/13 18:33:25 jeffmc Exp $
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



import java.rmi.RemoteException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import visad.DataReference;

import visad.VisADException;


/**
 * <p>A network of {@link ComputeCell}s.  This class provides support for
 * computational networks of {@link ComputeCell}s that perform their work more
 * efficiently than a naive network.</p>
 *
 * <p>This class is thread-compatible but not thread-safe.  Clients should
 * synchronize access to an instance of this class when appropriate.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $ $Date: 2005/05/13 18:33:25 $
 */
public final class CellNetwork {

    /** dirty flag */
    private boolean isDirty;

    /** Map of references to cells */
    private final Map refToCell;

    /**
     * Constructs from nothing.
     */
    public CellNetwork() {
        refToCell = new HashMap();
        isDirty   = true;
    }

    /**
     * Adds a cell.
     *
     * @param cell              The cell to be added to this network.
     */
    public void add(ComputeCell cell) {

        DataReference ref = cell.getOutputRef();

        if (refToCell.put(ref, cell) == null) {
            isDirty = true;
        }
    }

    /**
     * Removes a cell.
     *
     * @param cell              The cell to be removed from this network.
     */
    public void remove(ComputeCell cell) {

        DataReference refs = cell.getOutputRef();

        if (refToCell.remove(refs) != null) {
            isDirty = true;
        }
    }

    /**
     * Configures the network.  The network is analyzed for dependencies and
     * the input data references of the individual cells are appropriately
     * adjusted.
     *
     * @throws CellCycleException if the directed graph of dependencies of the
     *                            cells contains a cycle.
     * @throws VisADException     if a VisAD failure occurs.
     * @throws RemoteException    if a Java RMI failure occurs.
     */
    public void configure() throws VisADException, RemoteException {

        if (isDirty) {
            Set                visited = new HashSet(refToCell.size());
            CellCycleException ex      = new CellCycleException();

            for (Iterator iter = refToCell.values().iterator();
                    iter.hasNext(); ) {
                ComputeCell     cell      = (ComputeCell) iter.next();
                DataReference[] inputRefs = cell.getInputRefs();

                cell.enableAllInputRefs();

                for (int i = 0; i < inputRefs.length; i++) {
                    ex.setAncestor(inputRefs[i]);

                    for (int j = 0; j < inputRefs.length; j++) {
                        if (j == i) {
                            continue;
                        }

                        ex.setChild(inputRefs[j]);

                        try {
                            if (dependsOn(inputRefs[j], inputRefs[i],
                                          visited, ex)) {
                                cell.disableRef(inputRefs[i]);

                                /*
                                System.out.println("CellNetwork: Disabled " +
                                    "cell " + cell + ", reference " +
                                    inputRefs[i]);
                                 */
                                break;
                            }
                        } finally {
                            visited.clear();
                        }
                    }
                }
            }

            isDirty = false;
        }
    }

    /**
     * Check for dependency.
     *
     * @param r1    first ref
     * @param r2    second ref
     * @param visited  set of visited refs
     * @param ex       exception
     * @return true if dependent?
     * @throws CellCycleException if the directed graph of dependencies of the
     *                            cells contains a cycle.
     */
    private boolean dependsOn(DataReference r1, DataReference r2,
                              Set visited, CellCycleException ex) {

        boolean     dep;
        ComputeCell cell = (ComputeCell) refToCell.get(r1);

        if (cell == null) {
            dep = false;
        } else {
            if (visited.contains(cell)) {
                ex.setFound(cell);

                throw ex;
            }

            DataReference[] refs = cell.getInputRefs();

            dep = false;

            for (int i = 0; i < refs.length; i++) {
                if (r2 == refs[i]) {
                    dep = true;

                    break;
                }
            }

            if ( !dep) {
                visited.add(cell);

                for (int i = 0; i < refs.length; i++) {
                    if (dependsOn(refs[i], r2, visited, ex)) {
                        dep = true;

                        break;
                    }
                }

                visited.remove(cell);
            }
        }

        return dep;
    }
}







