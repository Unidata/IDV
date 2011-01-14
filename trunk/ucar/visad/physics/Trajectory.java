/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.visad.physics;



import visad.*;


/**
 * Provides support for trajectories. That is a set of points in some vector space
 * that are ordered on a 1-D manifold.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $ $Date: 2006/03/17 17:08:53 $
 */
public interface Trajectory {

    /**
     * Returns the trajectory.
     *
     * @return                  The trajectory.  It will be an ordered set of
     *                          points on a 1-D manifold.
     */
    public Gridded1DSetIface getTrajectory();
}
