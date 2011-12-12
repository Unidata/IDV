/*
 * $Id: SondeDataSource.java,v 1.25 2007/04/16 20:34:57 jeffmc Exp $
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


import ucar.ma2.Range;

import ucar.nc2.dt.TrajectoryObsDataset;
import ucar.nc2.dt.TrajectoryObsDatatype;

import ucar.unidata.data.*;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlUtil;


import visad.*;

import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 * A data source for balloon and aircraft tracks
 *
 * @author IDV Development Team
 * @version $Revision: 1.25 $ $Date: 2007/04/16 20:34:57 $
 */
public class SondeDataSource extends TrackDataSource {

    /**
     * sonde locations
     * @deprecated use ID_SONDESTARTLOCATIONS
     */
    public static final String ID_SONDELOCATIONS = "id.sondelocations";

    /** sounding trace id */
    public static final String ID_SONDESTARTLOCATIONS =
        "id.sondestartlocations";

    /** sounding trace id */
    private static final String ID_SONDEENDLOCATIONS = "id.sondeendlocations";

    /** Default Constructor */
    public SondeDataSource() {}

    /**
     * Create a SondeDataSource from the specification given.
     *
     * @param descriptor    descriptor for the DataSource
     * @param source        file location or URL
     * @param properties    extra properties
     *
     * @throws VisADException
     *
     */
    public SondeDataSource(DataSourceDescriptor descriptor, String source,
                           Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(source), properties);
    }

    /**
     * Create a SondeDataSource from the specification given.
     *
     * @param descriptor    descriptor for the DataSource
     * @param sources        file location or URL
     * @param properties    extra properties
     *
     * @throws VisADException
     *
     */
    public SondeDataSource(DataSourceDescriptor descriptor, List sources,
                           Hashtable properties)
            throws VisADException {
        super(descriptor, sources, properties);
        setDescription("Sonde Data Source");
    }


    /**
     * Get the base time for the sondes
     *
     * @return base time
     */
    protected DateTime getBaseTime() {
        DateTime[] minMax = getMinMaxTime();
        if (minMax == null) {
            return null;
        }
        return minMax[0];
    }


    /**
     * Aggregate the tracks
     *
     * @param tracks List of sonde tracks
     *
     * @return FieldImpl of aggregated tracks
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException  problem in VisAD
     */
    protected FieldImpl HIDEaggregateTracks(List tracks)
            throws VisADException, RemoteException {
        List         adapters = getAdapters();
        FunctionType fiType   = null;
        DateTime[]   times    = new DateTime[tracks.size()];
        for (int i = 0; i < tracks.size(); i++) {
            TrackAdapter adapter = (TrackAdapter) adapters.get(i);
            DateTime     time    = adapter.getEndTime();
            if (fiType == null) {
                FieldImpl data = (FieldImpl) tracks.get(i);
                fiType = new FunctionType(RealType.Time, data.getType());

            }
            times[i] = adapter.getStartTime();
        }

        FieldImpl fi = new FieldImpl(fiType, DateTime.makeTimeSet(times));
        for (int i = 0; i < tracks.size(); i++) {
            FieldImpl data = (FieldImpl) tracks.get(i);
            fi.setSample(i, data, false);
        }
        return fi;
    }


    /**
     * Make the {@link DataChoice}s associated with this dataset
     */
    protected void doMakeDataChoices() {
        List sources = getSources();
        if (sources.size() == 0) {
            return;
        }
        String source = sources.get(0).toString();
        File   f      = new File(source);
        if (f.isDirectory()) {
            return;
        }
        super.doMakeDataChoices();
        /*
        List         adapters = getAdapters();
        DataChoice soundingChoice = null;
        if (adapters != null && adapters.size() > 1) {
            soundingChoice = new CompositeDataChoice(
                this, ID_SOUNDINGOB, getDataChoiceLabel(ID_SOUNDINGOB),
                   getDataChoiceLabel(ID_SOUNDINGOB),
                   getRaobCategories());
            for (int i = 0;i<adapters.size();i++) {
                TrackAdapter ta = (TrackAdapter) adapters.get(i);
                TrackInfo trackInfo =
                     (TrackInfo) ta.getTrackInfos().get(0);
                String trackName           = trackInfo.getTrackName();
                System.err.println("track name = " + trackName);
                ((CompositeDataChoice)soundingChoice).addDataChoice(
                    new DirectDataChoice(
                        this, ID_SOUNDINGOB, trackName,
                        ("Sonde " + i), getRaobCategories()));
            }
        } else {
            soundingChoice =
            new DirectDataChoice(
                this, ID_SOUNDINGOB, getDataChoiceLabel(ID_SOUNDINGOB),
                   getDataChoiceLabel(ID_SOUNDINGOB),
                   getRaobCategories());
        }
        addDataChoice(soundingChoice);
        */

        addDataChoice(
            new DirectDataChoice(
                this, ID_SOUNDINGTRACE, getDataChoiceLabel(ID_SOUNDINGTRACE),
                getDataChoiceLabel(ID_SOUNDINGTRACE),
                getSoundingCategories()));


        List locCats = DataCategory.parseCategories("locations", false);
        addDataChoice(new DirectDataChoice(this, ID_SONDESTARTLOCATIONS,
                                           "Sonde Start Locations",
                                           "Sonde Start Locations", locCats));
        /*  End locations don't work now because lat/lon/alt values are NaN
        addDataChoice(
            new DirectDataChoice(
                this, ID_SONDEENDLOCATIONS, "Sonde End Locations",
                "Sonde End Locations", locCats));
        */
    }



    /**
     * Get the VisAD Data object that corresponds to the dataChoice
     * category and dataSelection criteria.
     *
     * @param dataChoice         choice for data
     * @param category           specific category of data (not used currently)
     * @param dataSelection      additional selection criteria
     * @param requestProperties  extra request properties
     *
     * @return corresponding Data object
     *
     * @throws VisADException  unable to create Data object
     * @throws RemoteException (some kind of remote error.
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        Object id = getChoiceId(dataChoice);
        if (id.equals(ID_SONDELOCATIONS) || id.equals(ID_SONDESTARTLOCATIONS)
                || id.equals(ID_SONDEENDLOCATIONS)) {
            List   adapters = getAdapters();
            String name     = id.equals(ID_SONDEENDLOCATIONS)
                              ? "Sonde End Locations"
                              : "Sonde Start Locations";
            StringBuffer xml = new StringBuffer("<stationtable name=\""
                                   + name + "\">\n");
            if (adapters != null) {
                for (int i = 0; i < adapters.size(); i++) {
                    TrackAdapter adapter = (TrackAdapter) adapters.get(i);
                    List         infos   = adapter.getTrackInfos();
                    if (infos.size() == 0) {
                        continue;
                    }
                    TrackInfo trackInfo = ((TrackInfo) infos.get(0));
                    //TODO:
                    TrajectoryObsDatatype todt =
                        ((CdmTrackInfo) trackInfo).getTodt();
                    int numObs = 0;
                    try {
                        numObs = trackInfo.getNumberPoints();
                        double[] times =
                            trackInfo.getTimeVals(todt.getFullRange());
                        boolean ascendingTimes = (times[0]
                                                  < times[times.length - 1]);
                        int rVal;
                        if (id.equals(ID_SONDEENDLOCATIONS)) {
                            rVal = (ascendingTimes)
                                   ? times.length - 1
                                   : 0;
                        } else {
                            rVal = (ascendingTimes)
                                   ? 0
                                   : times.length - 1;
                        }
                        Range r = new Range(rVal, rVal);
                        Date trueStartDate = (todt.getStartDate().getTime()
                                              > todt.getEndDate().getTime())
                                             ? todt.getEndDate()
                                             : todt.getStartDate();
                        String time = new DateTime(
                                          trueStartDate).formattedString(
                                          "HH:mm", DateTime.DEFAULT_TIMEZONE);
                        xml.append("<station " + XmlUtil.attr("id", time)
                                + XmlUtil.attr("name",
                                    IOUtil.getFileTail(adapter.getFilename())) + XmlUtil.attr("lat",
                                        "" + todt.getLatitude(r)) + XmlUtil.attr("lon",
                                            "" + todt.getLongitude(r)) + XmlUtil.attr("elev",
                                                "" + todt.getElevation(r)) + XmlUtil.attr("time",
                                                    time) + "/>");
                    } catch (Exception exc) {
                        throw new IllegalStateException(
                            "Got error creating sonde positions " + exc);

                    }
                }
            }

            xml.append("</stationtable>");
            return new visad.Text(xml.toString());
        } else {
            return super.getDataInner(dataChoice, category, dataSelection,
                                      requestProperties);
        }
    }

}

