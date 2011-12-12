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

package ucar.unidata.data.point;


import org.w3c.dom.Element;


import ucar.unidata.data.*;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.Util;

import visad.*;

import visad.georef.*;



import java.rmi.RemoteException;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;


/**
 *
 * @author IDV Development Team
 * @version $Revision: 1.90 $ $Date: 2007/08/06 17:02:27 $
 */
public class WaterMLDataSource extends PointDataSource {

    /** calender */
    public static final GregorianCalendar calendar =
        new GregorianCalendar(DateUtil.TIMEZONE_GMT);




    /**
     * Default contstructor.
     *
     * @throws VisADException
     */
    public WaterMLDataSource() throws VisADException {
        //        init();
    }

    /**
     * Create a new <code>AddePointDataSource</code> from the parameters
     * supplied.
     *
     * @param descriptor  <code>DataSourceDescriptor</code> for this.
     * @param source      Source URL
     * @param properties  <code>Hashtable</code> of properties for the source.
     *
     *
     * @throws Exception On badness
     */
    public WaterMLDataSource(DataSourceDescriptor descriptor, String source,
                             Hashtable properties)
            throws Exception {
        super(descriptor, source, "WaterML Point Data", properties);
    }



    /**
     * make the obs
     *
     * @param dataChoice the data choice
     * @param subset the data selection
     * @param bbox the bbox
     *
     * @return the obs
     *
     * @throws Exception On badness
     */
    protected FieldImpl makeObs(DataChoice dataChoice, DataSelection subset,
                                LatLonRect bbox)
            throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);

        Element       root = XmlUtil.getRoot(getFilePath(), getClass());

        List<PointOb> obs          = new ArrayList<PointOb>();
        TupleType     allTupleType = null;
        TupleType     finalTT      = null;
        List<Element> timeSeriesNodes =
            (List<Element>) XmlUtil.findChildren(root,
                WaterMLUtil.TAG_TIMESERIES);

        for (Element timeSeriesNode : timeSeriesNodes) {
            /*
    <sourceInfo xsi:type="SiteInfoType">
      <siteName>Little Bear River at McMurdy Hollow near Paradise, Utah</siteName>
      <siteCode network="LittleBearRiver" siteID="2">USU-LBR-Paradise</siteCode>
      <geoLocation>
        <geogLocation srs="EPSG:4269" xsi:type="LatLonPointType">
          <latitude>41.575552</latitude>
          <longitude>-111.855217</longitude>
        </geogLocation>
            */

            double latitude  = 0;
            double longitude = 0;
            double altitude  = 0;


            Element sourceNode = XmlUtil.findChild(timeSeriesNode,
                                     WaterMLUtil.TAG_SOURCEINFO);
            Element geoNode = XmlUtil.findChild(sourceNode,
                                  WaterMLUtil.TAG_GEOLOCATION);
            Element dataSetLocationNode =
                XmlUtil.findChild(sourceNode,
                                  WaterMLUtil.TAG_DATASETLOCATION);
            if (geoNode != null) {
                Element geogNode = XmlUtil.findChild(geoNode,
                                       WaterMLUtil.TAG_GEOGLOCATION);
                latitude = XmlUtil.getGrandChildValue(geogNode,
                        WaterMLUtil.TAG_LATITUDE, latitude);
                longitude = XmlUtil.getGrandChildValue(geogNode,
                        WaterMLUtil.TAG_LONGITUDE, longitude);
            } else if (dataSetLocationNode != null) {
                double south =
                    XmlUtil.getGrandChildValue(dataSetLocationNode,
                        WaterMLUtil.TAG_SOUTH, 0);
                double north =
                    XmlUtil.getGrandChildValue(dataSetLocationNode,
                        WaterMLUtil.TAG_NORTH, 0);
                double east = XmlUtil.getGrandChildValue(dataSetLocationNode,
                                  WaterMLUtil.TAG_EAST, 0);
                double west = XmlUtil.getGrandChildValue(dataSetLocationNode,
                                  WaterMLUtil.TAG_WEST, 0);
                latitude  = south + (north - south) / 2;
                longitude = east + (west - east) / 2;
            }

            String station = "stn";
            Element variableNode = XmlUtil.findChild(timeSeriesNode,
                                       WaterMLUtil.TAG_VARIABLE);
            List numericTypes = new ArrayList();
            List numericUnits = new ArrayList();

            List stringTypes  = new ArrayList();
            stringTypes.add(TextType.getTextType("Station"));


            /**
             * <variable>
             * <variableCode vocabulary="MODIS">9</variableCode>
             * <variableName>Cloud Optical Thickness Combined Phase (QA WT)</variableName>
             * <units
             * unitsAbbreviation="um"
             * unitsCode="55"
             * unitsType="Length">micron</units>
             * </variable>
             */

            String name = XmlUtil.getGrandChildText(variableNode,
                              WaterMLUtil.TAG_VARIABLENAME);
            String unitName = XmlUtil.getGrandChildText(variableNode,
                                  WaterMLUtil.TAG_UNITS);
            Unit unit = DataUtil.parseUnit(unitName);
            numericTypes.add(DataUtil.makeRealType(Util.cleanName(name),
                    unit));
            numericUnits.add(unit);
            Unit[] allUnits =
                (Unit[]) numericUnits.toArray(new Unit[numericUnits.size()]);

            allTupleType = DoubleStringTuple.makeTupleType(numericTypes,
                    stringTypes);


            Element valuesNode = XmlUtil.findChild(timeSeriesNode,
                                     WaterMLUtil.TAG_VALUES);
            List<Element> valueNodes =
                (List<Element>) XmlUtil.findChildren(valuesNode,
                    WaterMLUtil.TAG_VALUE);
            int cnt = 0;
            for (Element valueNode : valueNodes) {
                cnt++;
                ///xxxx
                //            <value dateTime="2001-01-01T00:00:00">11.2673</value>
                Date date = sdf.parse(XmlUtil.getAttribute(valueNode,
                                WaterMLUtil.ATTR_DATETIME));
                EarthLocation elt =
                    new EarthLocationLite(new Real(RealType.Latitude,
                        latitude), new Real(RealType.Longitude, longitude),
                                   new Real(RealType.Altitude, altitude));

                DateTime dttm = new DateTime(date);


                double value = new Double(
                                   XmlUtil.getChildText(
                                       valueNode).trim()).doubleValue();
                double[] realArray   = new double[] { value };
                String[] stringArray = new String[] { station };
                Tuple tuple = new DoubleStringTuple(allTupleType, realArray,
                                  stringArray, allUnits);

                PointObTuple pot;
                if (finalTT == null) {
                    pot     = new PointObTuple(elt, dttm, tuple);
                    finalTT = Tuple.buildTupleType(pot.getComponents());
                } else {
                    pot = new PointObTuple(elt, dttm, tuple, finalTT, false);

                }
                obs.add(pot);
            }
        }


        Integer1DSet indexSet =
            new Integer1DSet(RealType.getRealType("index"), obs.size());
        FieldImpl retField =
            new FieldImpl(
                new FunctionType(
                    ((SetType) indexSet.getType()).getDomain(),
                    ((PointObTuple) obs.get(0)).getType()), indexSet);
        Data[] obsArray = (Data[]) obs.toArray(new Data[obs.size()]);

        retField.setSamples(obsArray, false, false);





        return retField;
    }


    /**
     * add to properties. The comps list contains pairs of label/widget.
     *
     *
     * @param comps comps
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);
    }


    /**
     * apply the properties
     *
     * @return success
     */
    public boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        return true;
    }



}
