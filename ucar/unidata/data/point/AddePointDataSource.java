/*
 * $Id: AddePointDataSource.java,v 1.76 2007/07/05 18:46:10 jeffmc Exp $
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

package ucar.unidata.data.point;

import edu.wisc.ssec.mcidas.adde.AddePointURL;

import ucar.unidata.data.*;
import ucar.unidata.geoloc.LatLonPoint;

import ucar.unidata.geoloc.LatLonRect;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.Trace;

import visad.*;

import visad.data.mcidas.PointDataAdapter;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;

import java.io.File;


import java.io.IOException;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

import javax.swing.*;


/**
 * A data source for ADDE point data
 *
 * @author Don Murray
 * @version $Revision: 1.76 $ $Date: 2007/07/05 18:46:10 $
 */
public class AddePointDataSource extends PointDataSource {


    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(AddePointDataSource.class.getName());


    /**
     * Default contstructor.
     *
     * @throws VisADException
     */
    public AddePointDataSource() throws VisADException {
        init();
    }

    /**
     * Create a new <code>AddePointDataSource</code> from the parameters
     * supplied.
     *
     * @param descriptor  <code>DataSourceDescriptor</code> for this.
     * @param source      Source URL
     * @param properties  <code>Hashtable</code> of properties for the source.
     *
     * @throws VisADException  couldn't create the VisAD data
     */
    public AddePointDataSource(DataSourceDescriptor descriptor,
                               String source, Hashtable properties)
            throws VisADException {
        super(descriptor, source, (properties != null)
                                  ? (String) properties
                                      .get(ucar.unidata.idv.chooser.adde.AddeChooser
                                          .DATASET_NAME_KEY)
                                  : "Adde Point Data", properties);
        setName(getDescription());
    }





    /**
     * Get the name of this data. Override superclass method.
     * @return name of data
     */
    public String getDataName() {

        String dataName =
            (String) getProperty(ucar.unidata.idv.chooser.adde.AddeChooser.DATA_NAME_KEY,
                                 STATION_DATA);
        if (dataName.equals("")) {
            dataName = super.getDataName();
        }
        return dataName;

    }


    /**
     * Is this data source capable of saving its data to local disk
     *
     * @return Can save to local disk
     */
    public boolean canSaveDataToLocalDisk() {
        return !isFileBased();
    }


    /**
     * Save data to local disk
     *
     * @param prefix Fir dir and prefix
     * @param loadId For JobManager
     * @param changeLinks Change internal file references
     *
     * @return The files we made
     *
     * @throws Exception On badness
     */
    protected List saveDataToLocalDisk(String prefix, Object loadId,
                                       boolean changeLinks)
            throws Exception {
        String source = sources.get(0).toString();
        source = processUrl(source, null, null);
        List addeUrls = AddeUtil.generateTimeUrls(this, source);
        List urls     = new ArrayList();
        for (int i = 0; i < addeUrls.size(); i++) {
            String sourceUrl = (String) addeUrls.get(i);
            urls.add(sourceUrl + "&rawstream=true");
        }
        List newFiles = IOUtil.writeTo(urls, prefix, "apt", loadId);
        if (newFiles == null) {
            return null;
        }
        if (changeLinks) {
            sources = newFiles;
        }
        return newFiles;
    }

    /**
     * Process the url. Adding in the level and latlon if needed
     *
     * @param source Original url
     * @param subset For subsetting
     * @param bbox bbox
     *
     * @return processed url
     */
    private String processUrl(String source, DataSelection subset,
                              LatLonRect bbox) {
        return processUrl(source,subset,bbox, false);
    }

    /**
     * Process the url. Adding in the level and latlon if needed
     *
     * @param source Original url
     * @param subset For subsetting
     * @param bbox bbox
     * @param sampleIt  just get a sample
     *
     * @return processed url
     */
    private String processUrl(String source, DataSelection subset,
                              LatLonRect bbox, boolean sampleIt) {
        AddePointURL temp = AddePointURL.decodeURL(source);
        source = temp.getSelectClause();
        System.out.println("origina select clause = " + source);
        if (source.indexOf(AddeUtil.LATLON_BOX) >= 0) {
            String llb = "";
            if (bbox != null) {
                LatLonPoint ll = bbox.getLowerLeftPoint();
                LatLonPoint ur = bbox.getUpperRightPoint();
                double latMin  = Math.min(ll.getLatitude(), ur.getLatitude());
                double latMax  = Math.max(ll.getLatitude(), ur.getLatitude());
                double lonMin = Math.min(-1 * ll.getLongitude(),
                                         -1 * ur.getLongitude());
                double lonMax = Math.max(-1 * ll.getLongitude(),
                                         -1 * ur.getLongitude());
                llb = "LAT " + latMin + " " + latMax + ";LON " + lonMin + " "
                      + lonMax;
            }
            log_.debug("lat/lon box = " + llb);
            source = source.replaceAll(AddeUtil.LATLON_BOX, llb);
        }
        if (source.indexOf(AddeUtil.LEVEL) >= 0) {
            String level         = "";
            Object selectedLevel = null;
            if (subset != null) {
                selectedLevel = subset.getFromLevel();
            }
            if (selectedLevel == null) {
                List   levels = getLevels();
                Object defLev = ((levels == null) || levels.isEmpty())
                                ? selectedLevel
                                : levels.get(0);
                selectedLevel =
                    getProperty(ucar.unidata.idv.chooser.adde.AddePointDataChooser
                        .SELECTED_LEVEL, defLev);
            }
            if (selectedLevel != null) {
                level = "LEV " + selectedLevel.toString();
            }
            log_.debug("level = " + level);
            source = source.replaceAll(AddeUtil.LEVEL, level);
        }
        if (sampleIt) {
            if (!(source.indexOf("COL") >= 0 || source.indexOf("col") >= 0)) {
                source = source+";COL 1 ROW 1;";
            }
        }
        System.out.println("new select clause = " + source);
        temp.setSelectClause(source);
        source = temp.getURLString();
        return source;
    }

    /**
     * Get a sample observation
     *
     * @param dataChoice  choice
     *
     * @return the sample
     *
     * @throws Exception problem getting the sample
     */
    protected FieldImpl getSample(DataChoice dataChoice) throws Exception {
        return (FieldImpl) makeObs(dataChoice, null, null, true);

    }

    /**
     * Get the data from the ADDE URL and make the FieldImpl of
     * PointObs from it.
     *
     * @param dataChoice    data choice
     * @param subset        subsetting selection
     * @param bbox bounding box. may be null
     * @return   data corresponding to the choice and subset
     *
     * @throws Exception   problem creating the data
     */
    protected FieldImpl makeObs(DataChoice dataChoice, DataSelection subset,
                                LatLonRect bbox)
            throws Exception {
        return makeObs(dataChoice, subset, bbox, false);
    }

    /**
     * 
     *
     * @param dataChoice    data choice
     * @param subset        subsetting selection
     * @param bbox bounding box. may be null
     * @param sampleIt      flag to just get a sample ob
     *
     * @return  the data
     *
     * @throws Exception  problem reading data
     */
    protected FieldImpl makeObs(DataChoice dataChoice, DataSelection subset,
                                LatLonRect bbox, boolean sampleIt)
            throws Exception {

        List realUrls;
        Trace.call1("AddePointDataSource.makeObs");
        String source =         getSource(dataChoice);
        if (canSaveDataToLocalDisk()) {
            //Pointing to an adde server
            source   = processUrl(source, subset, bbox, sampleIt);
            realUrls = AddeUtil.generateTimeUrls(this, source);
        } else {
            //Pointing to a file
            realUrls = new ArrayList();
            for (int i = 0; i < sources.size(); i++) {
                String sourceUrl = (String) sources.get(i);
                realUrls.add("file:" + sourceUrl);
            }
        }

        //(String) dataChoice.getId();

        FieldImpl obs = null;
        //  FieldImpl obs = (FieldImpl) getCache (source);
        List datas = new ArrayList();
        if (obs == null) {
            for (int i = 0; i < realUrls.size(); i++) {
                String sourceUrl = (String) realUrls.get(i);
                if (sampleIt && i > 0) break;
                log_.debug("sourceUrl = " + sourceUrl);
                try {
                    Trace.call1("AddePointDataSource.pda ctor");
                    System.err.println("Source:" + sourceUrl);
                    PointDataAdapter pda = new PointDataAdapter(sourceUrl,
                                               false);
                    Trace.call1("AddePointDataSource.pda ctor");
                    Data data = pda.getData();
                    Trace.call2("AddePointDataSource.pda ctor");

                    Trace.call1("AddePointDataSource.makePointObsFromField");
                    obs = PointObFactory.makePointObsFromField(
                        (FieldImpl) data, getBinRoundTo(), getBinWidth());
                    Trace.call2("AddePointDataSource.makePointObsFromField");

                    datas.add(obs);
                    //putCache (source, obs);
                    //TODO: check to see that this is because of no data
                } catch (VisADException ve) {
                    logException("reading point data", ve);
                }  // no data available;
            }
        }
        Trace.call1("AddePointDataSource.mergeData");
        obs = PointObFactory.mergeData(datas);
        Trace.call2("AddePointDataSource.mergeData");

        Trace.call2("AddePointDataSource.makeObs");
        return obs;
    }


    /**
     * Create a field description from the field
     *
     * @param fi  field to use
     */
    protected void makeFieldDescription(FieldImpl fi) {
        super.makeFieldDescription(fi);
        fieldsDescription = getSources().get(0) + "<p>" + fieldsDescription;
    }

    /**
     * Get the data choice to use for the description
     *
     * @return  the data choice
     */
    protected DataChoice getDescriptionDataChoice() {
        DirectDataChoice retDC =
            new DirectDataChoice((DirectDataChoice) getDataChoices().get(0));
        String source = (String) retDC.getId();
        if (source.indexOf("num=all") >= 0) {
            source = source.replaceAll("num=all", "num=1");
        }
        if (source.indexOf(AddeUtil.RELATIVE_TIME) >= 0) {
            source = source.replaceAll(AddeUtil.RELATIVE_TIME, "");
        }
        retDC.setId(source);
        return retDC;
    }


    /**
     * Can this datasource do the geoselection subsetting and decimation
     *
     * @return _can do geo subsetting
     */
    public boolean canDoGeoSelection() {
        return true;
    }

    /**
     * Used for the geo subsetting property gui as to whether to
     * show the stride or not
     *
     * @return default is true
     */
    protected boolean canDoGeoSelectionStride() {
        return false;
    }

    /**
     * Used for the geo subsetting property gui as to whether to
     * show the map selection or not
     *
     * @return default is true
     */
    protected boolean canDoGeoSelectionMap() {
        return true;
    }

    /**
     * Check to see if this AddePointDataSource is equal to the object
     * in question.
     *
     * @param o  object in question
     *
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof AddePointDataSource)) {
            return false;
        }
        AddePointDataSource that = (AddePointDataSource) o;
        return (this == that);
    }

    /**
     * HashCode implementation.
     *
     * @return  hash code
     */
    public int hashCode() {
        int hashCode = getName().hashCode();
        return hashCode;
    }


    /**
     * Set the ADDE URL used for this object.
     *
     * @param url  ADDE URL
     *
     * @deprecated  Use setSource() instead.
     */
    public void setUrl(String url) {
        setSource(url);
    }


    /**
     * main
     *
     * @param args args
     *
     * @throws Exception On badness
     */
    public static void mainxxx(String[] args) throws Exception {
        //        Trace.startTrace();
        if (args.length == 0) {
            args = new String[] {
                "adde://adde.ucar.edu/pointdata?compress=true&port=112&debug=false&version=1&user=idv&proj=0&group=RTPTSRC&descr=SFCHOURLY&select='day 2006241 2006241;time 12 16;type 0;'&num=all&pos=ALL" };
        }

        boolean newWay = true;
        long    oldSum = 0;
        long    newSum = 0;
        int     oldCnt = 0;
        int     newCnt = 0;
        for (int j = 0; j < 20; j++) {
            Misc.gc();
            for (int i = 0; i < args.length; i++) {
                Trace.call1("AddePointDataSource.pda ctor");
                long             t1  = System.currentTimeMillis();
                PointDataAdapter pda = new PointDataAdapter(args[i], false);
                Trace.call2("AddePointDataSource.pda ctor", (newWay
                        ? " new way"
                        : " old way"));
                newWay = !newWay;
                Data data = pda.getData();

                Trace.call1("AddePointDataSource.makePointObsFromField");
                PointObFactory.makePointObsFromField((FieldImpl) data, 0, 0);
                Trace.call2("AddePointDataSource.makePointObsFromField");
                long t2 = System.currentTimeMillis();
                if (newWay) {
                    newCnt++;
                    newSum += (t2 - t1);
                } else {
                    oldCnt++;
                    oldSum += (t2 - t1);
                }
                if ((newCnt != 0) && (oldCnt != 0)) {
                    System.err.println("avg old:" + (oldSum / oldCnt)
                                       + " new: " + (newSum / newCnt));
                }
            }
        }
        Trace.stopTrace();

    }

    /**
     * Get the list of all levels available from this DataSource
     *
     *
     * @param dataChoice The data choice we are getting levels for
     * @return  List of all available levels
     */
    public List getAllLevels(DataChoice dataChoice, DataSelection dataSelection) {
        return getLevels();
    }

    /**
     * Get the levels property
     * @return levels;
     */
    private List getLevels() {
        return (List) getProperty(
            ucar.unidata.idv.chooser.adde.AddePointDataChooser.LEVELS,
            new ArrayList());
    }

}

