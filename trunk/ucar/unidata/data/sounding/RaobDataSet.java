/*
 * $Id: RaobDataSet.java,v 1.9 2006/12/01 20:42:44 jeffmc Exp $
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

package ucar.unidata.data.sounding;



import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;



import visad.DateTime;

import java.util.ArrayList;
import java.util.List;


/**
 * Holds sounding data from the chooser
 *
 * @author IDV development team
 * @version $Revision: 1.9 $
 */
public class RaobDataSet {

    /** the sounding obs in the set */
    List soundingObs;

    /** the adapter */
    SoundingAdapter adapter;

    /**
     *  Empty ctor for unpersisting
     */
    public RaobDataSet() {}

    /**
     * Create a new RaobDataSet
     *
     * @param adapter         adapter for data
     * @param soundingObs     list of sounding observations
     *
     */
    public RaobDataSet(SoundingAdapter adapter, List soundingObs) {
        this.soundingObs = new ArrayList(soundingObs);
        this.adapter     = adapter;
    }

    public boolean equals(Object object) {
        if(!object.getClass().equals(getClass())) return false;
        RaobDataSet that = (RaobDataSet) object;
        return Misc.equals(this.soundingObs, that.soundingObs);
    }

    /**
     * Set the {@link SoundingOb}s for this dataset.  Used by XML persistence.
     *
     * @param value   list of obs
     */
    public void setSoundingObs(List value) {
        soundingObs = value;
    }

    /**
     * Get the {@link SoundingOb}s for this dataset.  Used by XML persistence.
     *
     * @return  list of obs
     */
    public List getSoundingObs() {
        return soundingObs;
    }


    /**
     * Initialize the sounding in the ob
     *
     * @param so    sounding observation to initialize
     * @return  the initialized ob
     */
    public SoundingOb initSoundingOb(SoundingOb so) {
        return getSoundingAdapter().initSoundingOb(so);
    }


    /**
     * Get the source for the adapter
     *
     * @return  the source (file, server)
     */
    public String getAdapterSource() {
        return getSoundingAdapter().getSource();
    }

    /**
     * Set the sounding adapter for this dataset
     *
     * @param a   new adapter
     */
    public void setSoundingAdapter(SoundingAdapter a) {
        adapter = a;
    }

    /**
     * Get the adapter for this dataset
     * @return   the adapter
     */
    public SoundingAdapter getSoundingAdapter() {
        return adapter;
    }


}

