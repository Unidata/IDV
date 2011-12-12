/*
 * $Id: SoundingDataModel.java,v 1.9 2005/05/13 18:33:38 jeffmc Exp $
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



import visad.Field;


/**
 * Defines the API for a database of soundings.  The database comprises a
 * mutable list of soundings and selected soundings.  A sounding comprises a
 * temperature profile and a dew-point profile.
 *
 * @author Steven R. Emmerson
 * @version $Id: SoundingDataModel.java,v 1.9 2005/05/13 18:33:38 jeffmc Exp $
 */
interface SoundingDataModel extends DataModel {

    /**
     * Gets the sounding at the given index.
     * @param index             The index of the sounding.
     * @return                  The sounding at the given index.  The returned
     *                          array has 2 elements; the first element is the
     *                          temperature profile and the second element is
     *                          the dew-point profile.
     */
    Field[] getSounding(int index);

    /**
     * Returns the temperature profile at the given index.
     * @param index             The index of the temperature profile.
     * @return                  The temperature profile at the given index.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     */
    Field getTemperatureProfile(int index) throws IndexOutOfBoundsException;

    /**
     * Returns the dew-point profile at the given index.
     * @param index             The index of the dew-point profile.
     * @return                  The dew-point profile at the given index.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     */
    Field getDewPointProfile(int index) throws IndexOutOfBoundsException;
}







