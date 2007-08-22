/*
 * $Id: ClockedBeanCycleException.java,v 1.5 2005/05/13 18:33:26 jeffmc Exp $
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



/**
 * The exception thrown when a cycle is dectected in directed graph of {@link
 * ClockedBean} listeners.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.5 $ $Date: 2005/05/13 18:33:26 $
 */
public final class ClockedBeanCycleException extends RuntimeException {

    /** bean */
    private final ClockedBean bean;

    /**
     * Constructs from the clocked JavaBean that is part of a cycle.
     *
     * @param bean                  The clocked JavaBean that is part of a
     *                              cycle.
     * @throws NullPointerException if the argument is <code>null</code>.
     */
    public ClockedBeanCycleException(ClockedBean bean) {

        super(bean.toString());

        this.bean = bean;
    }

    /**
     * Returns the clocked JavaBean at which a cycle was detected.
     *
     * @return                      The JavaBean at which a cycle was detected.
     */
    public ClockedBean getClockedBean() {
        return bean;
    }
}







