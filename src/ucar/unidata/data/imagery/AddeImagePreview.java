package ucar.unidata.data.imagery;

import java.awt.Point;
import java.awt.image.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import edu.wisc.ssec.mcidas.AreaFileException;
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

/**
 * Created with IntelliJ IDEA.
 * User: opendap
 * Date: 5/29/13
 * Time: 9:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddeImagePreview {
    AddeImageDescriptor aid;
    private String BandNames[];
    private String source;
    private String CalInfo;
    private String SensorName;
    private String ProcessingLevel;

    private int subSampledPixels;
    private int subSampledScans;
    private BufferedImage preview_image;
    private ProjectionImpl proj;
    public AddeImagePreview( String source)
            throws IOException
    {

        this.source = source;
        String saveBand = AddeImageDataSource.getKey(source, AddeImageDataSource.BAND_KEY);


        //AddeImageDescriptor aid = null;
        while (aid == null) {
            try {
                aid = new AddeImageDescriptor(source);
            } catch (Exception excp) {
            }
        }


        try{
            init();
        } catch (Exception e) {

        }
    }



    private void createBufferedImage(float image_data[][])
    {
        WritableRaster raster = null;
        int num_bands = 0;
        if(null != preview_image)
            preview_image.getSampleModel().getNumBands();
        if(null == preview_image || image_data.length != num_bands)
        {
            if(image_data.length == 1)
                preview_image = new BufferedImage(subSampledPixels, subSampledScans, 10);
            else
                preview_image = new BufferedImage(subSampledPixels, subSampledScans, 5);
            DataBufferFloat dbuf = new DataBufferFloat(image_data, subSampledPixels * subSampledScans * image_data.length);
            SampleModel sampleModel = new BandedSampleModel(0, subSampledPixels, subSampledScans, image_data.length);
            raster = Raster.createWritableRaster(sampleModel, dbuf, new Point(0, 0));
            preview_image.setData(raster);
        } else
        if(1 == num_bands)
            preview_image.getRaster().setDataElements(0, 0, preview_image.getWidth(), preview_image.getHeight(), image_data[0]);
        else
        if(3 == num_bands)
            preview_image.getRaster().setDataElements(0, 0, preview_image.getWidth(), preview_image.getHeight(), image_data);
    }

    private void init()
            throws IOException, VisADException, AreaFileException
    {
        AreaAdapter aa = new AreaAdapter(this.source, false);
        visad.meteorology.SingleBandedImageImpl ff =
                (visad.meteorology.SingleBandedImageImpl) aa.getImage();
        AREACoordinateSystem acs = null;
        acs = new AREACoordinateSystem(aa.getAreaFile());
        this.proj = new MapProjectionProjection(acs);
        String sizeStr = AddeImageDataSource.getKey(source, AddeImageDataSource.SIZE_KEY);
        StringTokenizer strTok = new StringTokenizer(sizeStr, " ") ;
        String lineStr = strTok.nextToken();
        String elemStr = strTok.nextToken();
        subSampledPixels = Integer.parseInt(elemStr);
        subSampledScans = Integer.parseInt(lineStr);
        //Uggh, make a copy for now
        float[][]        image_data       = ff.unpackFloats();

        createBufferedImage(image_data);


    }


    public ProjectionImpl getSampledProjection()
    {
        return proj;
    }



    public BufferedImage getPreviewImage()
    {
        return preview_image;
    }

    public int getActualScans()
    {
        return subSampledScans;
    }

    public int getActualPixels()
    {
        return subSampledPixels;
    }

}
