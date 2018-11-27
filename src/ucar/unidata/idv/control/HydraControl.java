/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

//import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.SelectRangeWidget;
import ucar.visad.display.ColorScale;
import ucar.unidata.util.Range;
import ucar.unidata.util.LogUtil;

public abstract class HydraControl extends DisplayControlImpl {

//    public abstract boolean init(DataChoice dataChoice) throws VisADException, RemoteException;
//
//    public abstract void initDone();
//
//    public abstract MapProjection getDataProjection();

    public void updateRange(Range range) {
        if (ctw != null)
            ctw.setRange(range);

        try {
            SelectRangeWidget srw = getSelectRangeWidget(range);
            if (srw != null) {
                srw.setRange(range);
            }
        } catch (Exception e) {
            LogUtil.logException("Error updating select range widget", e);
        }

        if (colorScales != null) {
            ColorScale scale = (ColorScale) colorScales.get(0);
            try {
                scale.setRangeForColor(range.getMin(), range.getMax());
            }
            catch (Exception exc) {
                LogUtil.logException("Error updating display ColorScale range", exc);
            }
        }
    }

    public void handleChannelChange(final float newChan) {
        return;
    }

    //protected abstract MultiSpectralDisplay getMultiSpectralDisplay();
}