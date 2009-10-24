//
// Gridded3DSet.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2009 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

package ucar.visad.data;

import visad.*;



/**
 * Gridded3DSet represents a finite set of samples of R^3.
 * <P>
 */
public class CachedGridded2DSet extends Gridded2DSet {
    private Object cacheId;

    public CachedGridded2DSet(MathType type, float[][] samples, int lengthX,
                              CoordinateSystem coord_sys, Unit[] units,
                              ErrorEstimate[] errors, boolean copy)
        throws VisADException {
        super(type, samples, lengthX, coord_sys, units, errors, copy);
        initCache(samples);
    }

    public CachedGridded2DSet(MathType type, float[][] samples, int lengthX, int lengthY,
                              CoordinateSystem coord_sys, Unit[] units,
                              ErrorEstimate[] errors, boolean copy, boolean test)
        throws VisADException {
        super(type, samples, lengthX, lengthY, coord_sys, units, errors, copy, test);
        initCache(samples);
    }


    private void initCache(float[][]samples) {
        if(cacheId!=null) return;
        cacheId = DataCacheManager.getCacheManager().addToCache(samples);
        super.setMySamples(null);
    }

    public void finalize()  throws Throwable {
        DataCacheManager.getCacheManager().removeFromCache(cacheId);
        super.finalize();
    }

    protected void setMySamples(float[][]samples) {
        if(cacheId==null) {
            cacheId = DataCacheManager.getCacheManager().addToCache(samples);
        } else {
            DataCacheManager.getCacheManager().updateData(cacheId, samples);
        }
        super.setMySamples(null);
    }


    protected float[][] getMySamples() {
        return DataCacheManager.getCacheManager().getFloatArray2D(cacheId);
    }


}
