/*
 * Copyright 1997-2014 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.imagery;


import edu.wisc.ssec.mcidas.AreaFileException;
import edu.wisc.ssec.mcidas.adde.AddeImageURL;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.geoloc.ProjectionImpl;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.MapProjectionProjection;

import visad.VisADException;

import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.AreaAdapter;

import java.awt.Point;
import java.awt.image.*;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 * Created with IntelliJ IDEA.
 * User: yuanho
 * Date: 5/29/13
 * Time: 9:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddeImagePreview {

    /** _more_ */
    AddeImageDescriptor aid;

    /** _more_ */
    private String BandNames[];

    /** _more_ */
    private AreaAdapter adapter;


    /** _more_ */
    private int subSampledPixels;

    /** _more_ */
    private int subSampledScans;

    /** _more_ */
    private BufferedImage preview_image;

    /** _more_ */
    private ProjectionImpl proj;

    /**
     * Construct a AddeImagePreview
     *
     * @param adapter _more_
     * @param aid _more_
     *
     * @throws IOException _more_
     */
    public AddeImagePreview(AreaAdapter adapter, AddeImageDescriptor aid)
            throws IOException {

        this.adapter = adapter;
        this.aid     = aid;

        try {
            init();
        } catch (Exception e) {}
    }



    /**
     * _more_
     *
     * @param image_data _more_
     */
    private void createBufferedImage(float image_data[][]) {
        WritableRaster raster    = null;
        int            num_bands = 0;
        if (null != preview_image) {
            preview_image.getSampleModel().getNumBands();
        }
        if ((null == preview_image) || (image_data.length != num_bands)) {
            if (image_data.length == 1) {
                preview_image = new BufferedImage(subSampledPixels,
                        subSampledScans, BufferedImage.TYPE_BYTE_GRAY);
            } else {
                preview_image = new BufferedImage(subSampledPixels,
                        subSampledScans, BufferedImage.TYPE_3BYTE_BGR);
            }
            DataBufferFloat dbuf = new DataBufferFloat(image_data,
                                       subSampledPixels * subSampledScans
                                       * image_data.length);
            SampleModel sampleModel = new BandedSampleModel(0,
                                          subSampledPixels, subSampledScans,
                                          image_data.length);
            raster = Raster.createWritableRaster(sampleModel, dbuf,
                    new Point(0, 0));
            preview_image.setData(raster);
        } else if (1 == num_bands) {
            preview_image.getRaster().setDataElements(0, 0,
                    preview_image.getWidth(), preview_image.getHeight(),
                    image_data[0]);
        } else if (3 == num_bands) {
            preview_image.getRaster().setDataElements(0, 0,
                    preview_image.getWidth(), preview_image.getHeight(),
                    image_data);
        }
    }

    /**
     * _more_
     *
     * @throws AreaFileException _more_
     * @throws IOException _more_
     * @throws VisADException _more_
     */
    private void init()
            throws IOException, VisADException, AreaFileException {

        visad.meteorology.SingleBandedImageImpl ff =
            (visad.meteorology.SingleBandedImageImpl) adapter.getImage();
        AREACoordinateSystem acs = null;
        acs       = new AREACoordinateSystem(adapter.getAreaFile());
        this.proj = new MapProjectionProjection(acs);

        int[] ldir = adapter.getAreaDirectory().getDirectoryBlock();

        subSampledPixels = ldir[9];
        subSampledScans  = ldir[8];

        float[][] image_data = ff.unpackFloats();

        createBufferedImage(image_data);


    }


    /**
     * _more_
     *
     * @return _more_
     */
    public ProjectionImpl getSampledProjection() {
        return proj;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public BufferedImage getPreviewImage() {
        return preview_image;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getActualScans() {
        return subSampledScans;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getActualPixels() {
        return subSampledPixels;
    }

}
