/*
 * $Id: RaobDataSource.java,v 1.27 2007/04/16 20:34:57 jeffmc Exp $
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


import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;

import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlEncoder;

import visad.Data;
import visad.DateTime;
import visad.Field;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;



import visad.MathType;
import visad.RealType;
import visad.Tuple;
import visad.TupleType;
import visad.VisADException;

import visad.georef.EarthLocationTuple;

import java.io.IOException;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


/**
 * {@link ucar.unidata.data.DataSource} for RAOB soundings.
 *
 * @author MetApps Development Team
 * @version $Revision: 1.27 $ $Date: 2007/04/16 20:34:57 $
 */
public final class RaobDataSource extends DataSourceImpl {

    /** the associated raob data set */
    private RaobDataSet raobDataSet;

    /** the file or url   */
    private String file;


    /**
     * Constructs from nothing.  This is necessary for use of this class as a
     * JavaBean.
     */
    public RaobDataSource() {}

    /**
     * Constructs from a specification of the data-source.
     *
     * @param descriptor          A description of the data-source.
     * @param rds                 Radar datasource
     * @param properties          A map of associated attributes.
     * @throws VisADException     if a VisAD failure occurs.
     */
    public RaobDataSource(DataSourceDescriptor descriptor, RaobDataSet rds,
                          Hashtable properties)
            throws VisADException {
        super(descriptor, "RAOB data: " + rds.getAdapterSource(),
              "RAOB data source", properties);
        this.raobDataSet = rds;
    }



    /**
     * Constructs from a specification of the data-source.
     *
     * @param descriptor          A description of the data-source.
     * @param file                File or url
     * @param properties          A map of associated attributes.
     * @throws VisADException     if a VisAD failure occurs.
     */
    public RaobDataSource(DataSourceDescriptor descriptor, String file,
                          Hashtable properties)
            throws VisADException {
        super(descriptor, "RAOB data: " + file, "RAOB data source",
              properties);
        this.file        = file;
        this.raobDataSet = doMakeRaobDataSet();
    }



    /**
     * Make a dataset with the file
     *
     * @return dataset
     */
    private RaobDataSet doMakeRaobDataSet() {
        SoundingAdapter adapter = null;
        try {
            adapter = new NetcdfSoundingAdapter(file);
        } catch (Exception ill) {
        }

        if(adapter == null) {
            try {
                adapter = new CMASoundingAdapter(file);
            } catch (Exception exc) {
                LogUtil.logException("Reading sounding:" + file, exc);
                return null;
            }
        }

        if(adapter == null) {
            throw new IllegalArgumentException("Could not open sounding file:" + file);
        }



        SoundingOb[] obs = adapter.getSoundingObs();
        return new RaobDataSet(adapter, Misc.toList(obs));


    }


    /**
     * Check for equality
     *
     * @param object the object in question
     *
     * @return  true if they are equal
     */
    public boolean equals(Object object) {
        if ( !object.getClass().equals(getClass())) {
            return false;
        }
        RaobDataSource that = (RaobDataSource) object;
        if (file != null) {
            return Misc.equals(this.file, that.file);
        }
        return Misc.equals(raobDataSet, that.raobDataSet);
    }


    /**
     * Get the list of urls from the remote server
     *
     * @return List of urls
     */
    public List getDataPaths() {
        List paths = new ArrayList();
        AddeSoundingAdapter asa =
            (AddeSoundingAdapter) getRDS().getSoundingAdapter();
        List obs = getRDS().getSoundingObs();
        for (int i = 0; i < obs.size(); i++) {
            SoundingOb ob = (SoundingOb) obs.get(i);
            if (ob.getMandatoryFile() != null) {
                //file based
                paths.add(ob.getMandatoryFile());
                paths.add(ob.getSigFile());
            } else {
                paths.add(asa.getMandatoryURL(ob));
                paths.add(asa.getSigURL(ob));
            }
        }
        return paths;
    }


    /**
     * Override the init method for when this data source is unpersisted.
     * We simply check the imageList to see if this object came from a
     * legacy bundle.
     */
    public void initAfterUnpersistence() {
        super.initAfterUnpersistence();
        List tmp = getTmpPaths();
        if (tmp != null) {
            List obs = getRDS().getSoundingObs();
            AddeSoundingAdapter asa =
                (AddeSoundingAdapter) getRDS().getSoundingAdapter();
            for (int i = 0; i < tmp.size(); i += 2) {
                SoundingOb ob = (SoundingOb) obs.get(i / 2);
                ob.setMandatoryFile(tmp.get(i).toString());
                ob.setMandatoryFile(tmp.get(i + 1).toString());
            }
        }
    }

    /**
     * _Save the remote data to local disk
     *
     * @param prefix Where to write the files to
     * @param loadId For the JobManager dialog
     * @param changeLinks Should we change the internal data references
     *
     * @return List of files we wrote
     *
     * @throws Exception On badness
     */
    protected List saveDataToLocalDisk(String prefix, Object loadId,
                                       boolean changeLinks)
            throws Exception {
        List urls = new ArrayList();
        List obs  = getRDS().getSoundingObs();
        AddeSoundingAdapter asa =
            (AddeSoundingAdapter) getRDS().getSoundingAdapter();
        for (int i = 0; i < obs.size(); i++) {
            SoundingOb ob = (SoundingOb) obs.get(i);
            urls.add(asa.getMandatoryURL(ob) + "&rawstream=true");
            urls.add(asa.getSigURL(ob) + "&rawstream=true");
        }
        List newFiles = IOUtil.writeTo(urls, prefix, "raob", loadId);
        if (newFiles == null) {
            return null;
        }
        if (changeLinks) {
            for (int i = 0; i < newFiles.size(); i += 2) {
                SoundingOb ob = (SoundingOb) obs.get(i / 2);
                ob.setMandatoryFile(newFiles.get(i).toString());
                ob.setSigFile(newFiles.get(i + 1).toString());
            }
        }
        return newFiles;
    }


    /**
     * Is this data source capable of saving its data to local disk
     *
     * @return Can save to local disk
     */
    public boolean canSaveDataToLocalDisk() {
        return (getRDS().getSoundingAdapter() instanceof AddeSoundingAdapter)
               && ((SoundingOb) getRDS().getSoundingObs().get(
                   0)).getMandatoryFile() == null;

    }



    /**
     * Adds the {@link ucar.unidata.data.DataChoice}s of the current
     * input data via {@link #addDataChoice(DataChoice)}.
     */
    protected void doMakeDataChoices() {
        int  i           = 0;
        List soundingObs = getRDS().getSoundingObs();
        List categories  = new ArrayList();

        DataCategory cat =
            new DataCategory(DataCategory.CATEGORY_RAOBSOUNDING);
        cat.setForDisplay(false);
        categories.add(cat);

        List         compCategories = new ArrayList();
        DataCategory compCat        = new DataCategory("None");
        compCat.setForDisplay(false);
        compCategories.add(compCat);

        CompositeDataChoice composite = new CompositeDataChoice(this,
                                            soundingObs, getName(),
                                            "RAOB Data", categories);
        Hashtable props = Misc.newHashtable(DataChoice.PROP_ICON,
                                            "/auxdata/ui/icons/Balloon.gif");

        for (Iterator iter = soundingObs.iterator(); iter.hasNext(); ) {
            SoundingOb    ob         = (SoundingOb) iter.next();
            String        name       = ob.getLabel();
            DateTime      obTime     = ob.getTimestamp();
            DataSelection timeSelect = null;
            if (obTime != null) {
                ArrayList times = new ArrayList(1);
                times.add(obTime);
                timeSelect = new DataSelection(times);
            }
            DataChoice choice = new DirectDataChoice(this, ob,
                                    composite.getName(), name, categories,
                                    timeSelect, props);
            composite.addDataChoice(choice);
        }
        addDataChoice(composite);
    }

    /**
     * Get the VisAD {@link visad.Data} object orresponding to
     * {@link ucar.unidata.data.DataChoice},
     * {@link ucar.unidata.data.DataCategory},
     * and {@link ucar.unidata.data.DataSelection} criteria.
     *
     * @param dataChoice       choice of data.
     * @param category         specific category of data (currently ignored).
     * @param dataSelection    additional selection criteria.
     * @param requestProperties  extra request properties
     *
     * @return corresponding   Data object.  extra request properties
     *
     * @throws VisADException  if unable to create Data object.
     * @throws RemoteException (some kind of remote error.
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        return getSoundingObs(dataChoice, dataSelection);
    }

    /**
     * Make the list of times associated with this DataSource for
     * DataSelection.
     *
     * @return DateTimes as a list.
     */
    protected List doMakeDateTimes() {
        return null;  // Arrays.asList (
        //  new DateTime[] { traceAdapter.getBaseTime() });
    }

    /**
     * Gets the SoundingOb associated with this DataChoice.  The VisAD {@link
     * visad.MathType} of the return value is the {@link visad.Tuple}
     * ({@link visad.DateTime}, {@link visad.georef.EarthLocationTuple},
     * {@link ucar.visad.functiontypes.InSituAirTemperatureProfile},
     * {@link ucar.visad.functiontypes.DewPointProfile}).
     *
     *
     * @param dc                DataChoice for data
     * @param dataSelection     subselection criteria
     * @return                  The sounding observation.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private Data makeSoundingOb(DataChoice dc, DataSelection dataSelection)
            throws VisADException, RemoteException {
        SoundingOb so = getRDS().initSoundingOb((SoundingOb) dc.getId());
        if (so == null) {
            return null;
        }
        RAOB            raob = so.getRAOB();
        SoundingStation ss   = (SoundingStation) so.getStation();
        // sanity check
        FlatField t = raob.getTemperatureProfile();
        if (t.getDomainSet().getLength() == 1) {
            return null;
        }
        return new Tuple(new Data[] { so.getTimestamp(),
                                      ((SoundingStation) so.getStation())
                                          .getNamedLocation(),
                                      raob.getTemperatureProfile(),
                                      raob.getDewPointProfile(),
                                      raob.getWindProfile() });
    }

    /**
     * Get a set of sounding obs based on the choice
     *
     * @param dataChoice        DataChoice for data
     * @param subset            subselection criteria
     * @return  corresponding sounding observations
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private Data getSoundingObs(DataChoice dataChoice, DataSelection subset)
            throws VisADException, RemoteException {
        Vector v       = new Vector();
        List   choices = (dataChoice instanceof CompositeDataChoice)
                         ? ((CompositeDataChoice) dataChoice).getDataChoices()
                         : Arrays.asList(new DataChoice[] { dataChoice });
        for (Iterator iter = choices.iterator(); iter.hasNext(); ) {
            Data ob = makeSoundingOb((DataChoice) iter.next(), subset);
            if (ob != null) {
                v.add(ob);
            }
        }
        return (v.isEmpty())
               ? null
               : new Tuple((Data[]) v.toArray(new Data[v.size()]), false);
    }





    /**
     * Dynamically create the rds if needed and return it
     *
     * @return the rds
     */
    public RaobDataSet getRDS() {
        if ((raobDataSet == null) && (file != null)) {
            this.raobDataSet = doMakeRaobDataSet();
        }
        return raobDataSet;
    }

    /**
     * Get the RaobDataSet for this data source
     *
     * @return  the RaobDataSet for this data source
     */
    public RaobDataSet getRaobDataSet() {
        //Only write out the  dataset if we don't have a file
        if (file == null) {
            return raobDataSet;
        }
        return null;
    }

    /**
     * Set the RaobDataSet for this data source
     *
     * @param newRds  the RaobDataSet for this data source
     */
    public void setRaobDataSet(RaobDataSet newRds) {
        raobDataSet = newRds;
    }

    /**
     *  Set the File property.
     *
     *  @param value The new value for File
     */
    public void setFile(String value) {
        file = value;
    }

    /**
     *  Get the File property.
     *
     *  @return The File
     */
    public String getFile() {
        return file;
    }


}

