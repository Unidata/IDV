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

package ucar.unidata.idv.control;



import ucar.visad.display.Displayable;



/**
 * A class to encapsulate a displayable with an attribute flags.
 *
 * @author Jeff McWhirter, et. al. really smart guys (and gals)
 * @version $Revision: 1.6 $
 */
public class FlaggedDisplayable {

    /** The displayable */
    public Displayable displayable;

    /** the flag */
    public int flag;

    /**
     * Create a flagged displayable.
     *
     * @param displayable   the displayable
     * @param flag          the flag
     */
    public FlaggedDisplayable(Displayable displayable, int flag) {
        this.displayable = displayable;
        this.flag        = flag;
    }

    /**
     * Check if a flag matches this instances flag.
     *
     * @param f   flag to check
     * @return  true if a match
     */
    public boolean ok(int f) {
        return (flag & f) != 0;
    }

}
