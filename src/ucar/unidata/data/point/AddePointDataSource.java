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

package ucar.unidata.data.point;


import edu.wisc.ssec.mcidas.adde.AddePointURL;

import ucar.unidata.data.AddeUtil;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import visad.Data;
import visad.FieldImpl;
import visad.Real;
import visad.RealType;
import visad.Tuple;
import visad.VisADException;

import visad.data.mcidas.PointDataAdapter;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * A data source for ADDE point data
 *
 * @author IDV Development Team
 */
public class AddePointDataSource extends PointDataSource {


    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(AddePointDataSource.class.getName());

    /** does this have the Altitude parameter? */
    private boolean hasAltitude = true;

    /** urls with time in them */
    private DataSelection timeDriverSelection;

    static double  increment = 0.0;

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
        super(descriptor, source, makeDatasetName(properties), properties);
        setName(getDescription());
    }

    /**
     * Get the name from the hashtable
     * @param properties  Hashtable of properties (may be null)
     * @return a name
     */
    private static String makeDatasetName(Hashtable properties) {
        String name = "Adde Point Data";
        if (properties != null) {
            String possible =
                (String) properties
                    .get(ucar.unidata.idv.chooser.adde.AddeChooser
                        .DATASET_NAME_KEY);
            if (possible != null) {
                name = possible;
            }
            String checkglm =
                    (String) properties
                            .get(ucar.unidata.idv.chooser.adde.AddeChooser
                                    .DATA_NAME_KEY);
            if(checkglm != null && checkglm.contains("GLM Lightning Data")){
                Double relT =  (Double) properties
                        .get("relative time increment");
                properties.put(AddePointDataSource.PROP_STATIONMODELSCALE, "0.5");
                if(relT != null ){
                    double reltime = (double)(relT * 60.0);
                    increment = reltime;
                }
            }
        }
        return name;
    }

    /** list of excluded params for objective analysis */
    private static final String[] excludes = {
        "COL", "ROW", "NREC", "IDN", "MOD", "TYPE", "HMS"
    };

    /**
     * Is it ok to create a grid field for the parameter with the
     * given real type
     *
     * @param type the type
     *
     * @return ok to create grid
     */
    protected boolean canCreateGrid(RealType type) {
        for (int i = 0; i < excludes.length; i++) {
            if (type.getName().equals(excludes[i])) {
                return false;
            }
        }
        return true;
    }


    /**
     * Get the name of this data. Override superclass method.
     * @return name of data
     */
    public String getDataName() {

        String dataName =
            (String) getProperty(
                ucar.unidata.idv.chooser.adde.AddeChooser.DATA_NAME_KEY,
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
        // See if there is a data subset
        LatLonRect bbox = null;
        DataSelection ds = getDataSelection();
        if (ds != null) {
            GeoSelection gs = ds.getGeoSelection();
            if (gs != null) {
                bbox = gs.getLatLonRect();
            }
        }
        source = processUrl(source, null, null, bbox, false);
        List addeUrls = AddeUtil.generateTimeUrls(this, source,
                            timeDriverSelection);
        List<String> urls = new ArrayList<String>();
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
     * @param choice for param subsetting
     * @param subset For subsetting
     * @param bbox bbox
     * @param sampleIt  just get a sample
     *
     * @return processed url
     */
    private String processUrl(String source, DataChoice choice,
                              DataSelection subset, LatLonRect bbox,
                              boolean sampleIt) {
        AddePointURL temp = AddePointURL.decodeURL(source);
        if (temp == null) {
            return source;
        }
        source = temp.getSelectClause();
        if (sampleIt) {
            if (source.indexOf(AddeUtil.LEVEL) >= 0) {
                String level = makeLevelString(subset);
                if (level.length() > 0) {
                    level = "'" + level + "'";
                }
                temp.setSelectClause(level);
            } else {
                temp.setSelectClause("");
            }
            temp.setMaxNumber(1);
            if(!choice.getDescription().contains("Lightning"))
                temp.setPosition("0");  // might have to change this to ALL
        } else {
            //System.out.println("original select clause = " + source);
            if (source.indexOf(AddeUtil.LATLON_BOX) >= 0) {
                String llb = "";
                if (bbox != null) {
                    LatLonPoint ll = bbox.getLowerLeftPoint();
                    LatLonPoint ur = bbox.getUpperRightPoint();
                    double lonMin;
                    double lonMax;
                                             
                    double latMin = Math.min(ll.getLatitude(),
                                             ur.getLatitude());
                    double latMax = Math.max(ll.getLatitude(),
                                             ur.getLatitude());
                    if(ll.getLongitude() > 0 && ur.getLongitude() < 0) {
                        lonMax = -1 * ll.getLongitude();
                        lonMin = -1 * ur.getLongitude();
                    } else {
                        lonMin = Math.min(-1 * ll.getLongitude(),
                                -1 * ur.getLongitude());
                        lonMax = Math.max(-1 * ll.getLongitude(),
                                -1 * ur.getLongitude());
                    }
                    llb = "LAT " + latMin + " " + latMax + ";LON " + lonMin
                          + " " + lonMax;
                }
                log_.debug("lat/lon box = " + llb);
                source = source.replaceAll(AddeUtil.LATLON_BOX, llb);
            }
            if (source.indexOf(AddeUtil.LEVEL) >= 0) {
                String level = makeLevelString(subset);
                source = source.replaceAll(AddeUtil.LEVEL, level);
            }
            if ((choice != null)
                    && (choice.getProperty(PROP_GRID_PARAM) != null)) {
                String param = ((RealType) choice.getProperty(
                                   PROP_GRID_PARAM)).getName();
                if (hasAltitude) {
                    temp.setParams("DAY TIME LAT LON ZS " + param);
                } else {
                    temp.setParams("DAY TIME LAT LON " + param);
                }
            }
            temp.setSelectClause(source);
            //System.out.println("new select clause = " + source);
        }
        //source = temp.getURLString();
        //return source;
        return temp.getURLString();

    }

    /**
     * Make the level string
     *
     * @param subset  the data selection for the level
     *
     * @return  the level string
     */
    private String makeLevelString(DataSelection subset) {
        String level         = "";
        Object selectedLevel = null;
        if (subset != null) {
            selectedLevel = subset.getFromLevel();
            //if (selectedLevel != null) System.out.println("from subset " + selectedLevel + " " + selectedLevel.getClass().getName());
        }
        if (selectedLevel == null) {
            List   levels = getLevels();
            Object defLev = ((levels == null) || levels.isEmpty())
                            ? selectedLevel
                            : levels.get(0);
            //System.out.println("devLev = " + defLev);
            selectedLevel =
                getProperty(ucar.unidata.idv.chooser.adde.AddePointDataChooser
                    .SELECTED_LEVEL, defLev);
            //System.out.println("selectedLevel from property = " + selectedLevel);
        }
        if (selectedLevel != null) {
            String levelString = selectedLevel.toString();
            // HACK for upper air levels
            if (selectedLevel instanceof Real) {
                int value = (int) ((Real) selectedLevel).getValue();
                switch (value) {

                  case 1001 :
                      levelString = "SFC";
                      break;

                  case 0 :
                      levelString = "TRO";
                      break;

                  case 1013 :
                      levelString = "MSL";
                      break;

                  default :
                      levelString = "" + value;
                      break;
                }
            }
            level = "LEV " + levelString;
        }
        log_.debug("level = " + level);
        return level;
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
        String source = getSource(dataChoice);
        boolean usingTD = false;
        if (canSaveDataToLocalDisk()) {  // Pointing to an adde server
            source   = processUrl(source, dataChoice, subset, bbox, sampleIt);
            realUrls = AddeUtil.generateTimeUrls(this, source, subset);
            if ((subset != null) && (subset.getTimeDriverTimes() != null)) {
                usingTD = true;
                timeDriverSelection = subset;
            } else {
                timeDriverSelection = null;
            }
        } else {                         // Pointing to a file
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
                if (sampleIt && (i > 0)) {
                    break;
                }
                log_.debug("sourceUrl = " + sourceUrl);
                try {
                    Trace.call1("AddePointDataSource.pda ctor");
                    PointDataAdapter pda = new PointDataAdapter(sourceUrl,
                                               false, true);
                    Trace.call1("AddePointDataSource.pda ctor");
                    Data data = pda.getData();
                    String message = "size of ll data " + ((FieldImpl) data).getLength();
                    LogUtil.consoleMessage(message);
                    //System.out.println(message);
                    Trace.call2("AddePointDataSource.pda ctor");
                    if (sampleIt) {
                        checkForNeededParams((FieldImpl) data);
                    }

                    Trace.call1("AddePointDataSource.makePointObsFromField");
                    if(increment != 0.0 && getBinWidth() == 0.0){
                        setBinWidth(increment);
                    }
                    obs = PointObFactory.makePointObsFromField(
                        (FieldImpl) data, getBinRoundTo(), getBinWidth());
                    Trace.call2("AddePointDataSource.makePointObsFromField");
                    if (!canSaveDataToLocalDisk() && bbox != null) {  
                        obs = PointObFactory.subSet(obs, bbox);
                    }

                    datas.add(obs);
                    //putCache (source, obs);
                    //TODO: check to see that this is because of no data
                } catch (VisADException ve) {
                   // only log exceptions when not using time driver.
                   if (!usingTD) {
                       logException("reading point data", ve);
                   }
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
     * Check for required params
     * @param rawSample  sample data (index)-&gt;Tuple
     */
    private void checkForNeededParams(FieldImpl rawSample) {
        try {
            Tuple  ob    = (Tuple) rawSample.getSample(0);
            Real[] reals = ob.getRealComponents();
            if (reals != null) {
                for (int i = 0; i < reals.length; i++) {
                    RealType rt = (RealType) reals[i].getType();
                    if (rt.equals(RealType.Altitude)) {
                        hasAltitude = true;
                        return;
                    }
                }
            }
            hasAltitude = false;
        } catch (Exception ve) {}
    }

    /**
     * Can this datasource do the geoselection subsetting and decimation
     *
     * @return can do geo subsetting
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
                long t1 = System.currentTimeMillis();
                PointDataAdapter pda = new PointDataAdapter(args[i], false,
                                           true);
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
     * @param dataSelection   data selection
     * @return  List of all available levels
     */
    public List getAllLevels(DataChoice dataChoice,
                             DataSelection dataSelection) {
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

    /**
     * Get a list of selected levels.  Subclasses should override
     * if they have levels.
     * @return list of levels (may be empty)
     */
    protected List getDefaultLevels() {
        List retList = new ArrayList();
        Object level =
            getProperty(ucar.unidata.idv.chooser.adde.AddePointDataChooser
                .SELECTED_LEVEL);
        if (level != null) {
            retList.add(level);
        }
        return retList;
    }

    /**
     * Get the list of times for this datasource
     *
     * @return  empty list from this class
     */
    protected List doMakeDateTimes() {
        List   timesList = new ArrayList();
        String source    = (String) sources.get(0);
        if (getProperty(AddeUtil.ABSOLUTE_TIMES, (Object) null) != null) {
            timesList.addAll((List) getProperty(AddeUtil.ABSOLUTE_TIMES));
        } else if (source.indexOf(AddeUtil.RELATIVE_TIME) >= 0) {
            Object tmp = getProperty(AddeUtil.NUM_RELATIVE_TIMES,
                                     new Integer(0));
            int[] timeIndices;
            if (tmp instanceof Integer) {
                int numTimes = ((Integer) tmp).intValue();
                timeIndices = new int[numTimes];
                for (int i = 0; i < numTimes; i++) {
                    timeIndices[i] = i;
                }
            } else {
                timeIndices = (int[]) tmp;
            }
            for (int i = 0; i < timeIndices.length; i++) {
                String name = timeIndices[i] + "th most recent";
                if (i == 0) {
                    name = "Most recent";
                }
                if ((i > 0) && (i < DataSource.ordinalNames.length)) {
                    name = DataSource.ordinalNames[timeIndices[i]]
                           + " most recent";
                }
                timesList.add(new TwoFacedObject(name, i));
            }
        }
        return timesList;
    }

}
