/*
 * $Id: PointDataSource.java,v 1.33 2007/06/21 14:44:59 jeffmc Exp $
 *
 * Copyright (c) 1997-2004 Unidata Program Center/University Corporation for
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


import ucar.unidata.data.*;

import ucar.unidata.geoloc.LatLonRect;


import ucar.unidata.ui.TimeLengthField;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import visad.*;

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;


/**
 * A data source for point data
 *
 * @author Don Murray
 * @version $Revision: 1.33 $ $Date: 2007/06/21 14:44:59 $
 */
public abstract class PointDataSource extends FilesDataSource {

    /** station model name property */
    public static final String PROP_STATIONMODELNAME =
        "prop.stationmodelname";

    /** Identifier for station data */
    public static final String STATION_DATA = "Station Data";

    /** Identifier for point data */
    public static final String POINT_DATA = "Point Data";

    /** Identifier for a station plot */
    public static final String STATION_PLOT = DataCategory.CATEGORY_POINTPLOT;

    /** default categories */
    private List pointCategories = null;


    /**
     *  A cached version of the html description of the fields.
     */
    protected String fieldsDescription;

    /** bind round to factor */
    private double binRoundTo = 0;

    /** time bin width */
    private double binWidth = 0;

    /** for properties dialog */
    private TimeLengthField binWidthField;

    /** for properties dialog */
    private TimeLengthField binRoundToField;

    /** for properties dialog */
    private JComboBox roundToCbx;

    /** for properties dialog */
    private JComboBox widthCbx;


    /**
     *
     * Default constructor
     *
     * @throws VisADException  problem creating VisAD data object
     */
    public PointDataSource() throws VisADException {
        init();
    }

    /**
     * Create a PointDataSource
     *
     * @param descriptor    descriptor for the DataSource
     * @param source        file location or URL
     * @param description   description of data
     * @param properties    extra properties
     *
     * @throws VisADException
     *
     */
    public PointDataSource(DataSourceDescriptor descriptor, String source,
                           String description, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.toList(new String[] { source }), description,
             properties);
    }


    /**
     * Create a new PointDataSource
     *
     * @param descriptor    data source descriptor
     * @param sources       List of sources of data (filename/URL)
     * @param name          The name to use
     * @param properties    extra properties for initialization
     *
     * @throws VisADException   problem creating the data
     *
     */
    public PointDataSource(DataSourceDescriptor descriptor, List sources,
                           String name, Hashtable properties)
            throws VisADException {
        super(descriptor, sources, (sources.size() > 1)
                                   ? "Point Data"
                                   : (String) sources.get(0), name,
                                   properties);
        try {
            init();
        } catch (VisADException exc) {
            setInError(true);
            throw exc;
        }
    }

    /**
     * Initialize this object
     *
     * @throws VisADException    problem during initialization
     */
    protected void init() throws VisADException {}



    public boolean canAddCurrentName(DataChoice dataChoice){
        return false;
    }


    /**
     * add to properties
     *
     * @param comps comps
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);
        binWidthField   = new TimeLengthField("Bin Width", true);
        binRoundToField = new TimeLengthField("Bin Round To", true);
        binWidthField.setTime(binWidth);
        binRoundToField.setTime(binRoundTo);
        List roundToItems = Misc.toList(new Object[] {
            new TwoFacedObject("Change", new Double(0)),
            new TwoFacedObject("On the hour", new Double(60)),
            new TwoFacedObject("5 after", new Double(5)),
            new TwoFacedObject("10 after", new Double(10)),
            new TwoFacedObject("15 after", new Double(15)),
            new TwoFacedObject("20 after", new Double(20)),
            new TwoFacedObject("30 after", new Double(30)),
            new TwoFacedObject("45 after", new Double(45)),
            new TwoFacedObject("10 to", new Double(50)),
            new TwoFacedObject("5 to", new Double(55))
        });

        roundToCbx = GuiUtils.makeComboBox(roundToItems, roundToItems.get(0),
                                           false, this,
                                           "setRoundToFromComboBox");

        List widthItems = Misc.toList(new Object[] {
            new TwoFacedObject("Change", new Double(0)),
            new TwoFacedObject("5 minutes", new Double(5)),
            new TwoFacedObject("10 minutes", new Double(10)),
            new TwoFacedObject("15 minutes", new Double(15)),
            new TwoFacedObject("20 minutes", new Double(20)),
            new TwoFacedObject("30 minutes", new Double(30)),
            new TwoFacedObject("45 minutes", new Double(45)),
            new TwoFacedObject("1 hour", new Double(60)),
            new TwoFacedObject("6 hours", new Double(60 * 6)),
            new TwoFacedObject("12 hours", new Double(60 * 12)),
            new TwoFacedObject("1 day", new Double(60 * 24))
        });


        widthCbx = GuiUtils.makeComboBox(widthItems, widthItems.get(0),
                                         false, this, "setWidthFromComboBox");



        comps.add(GuiUtils.filler());
        comps.add(getPropertiesHeader("Time Binning"));

        comps.add(GuiUtils.rLabel("Bin Size:"));
        comps.add(GuiUtils.left(GuiUtils.hbox(binWidthField.getContents(),
                widthCbx, 5)));
        comps.add(GuiUtils.rLabel("Round To:"));
        comps.add(GuiUtils.left(GuiUtils.hbox(binRoundToField.getContents(),
                roundToCbx, 5)));


    }


    /**
     * Set the property
     *
     * @param tfo value from combo box_
     */
    public void setRoundToFromComboBox(TwoFacedObject tfo) {
        double value = ((Double) tfo.getId()).doubleValue();
        if (value == 0.0) {
            return;
        }
        binRoundToField.setTime(value);
        roundToCbx.setSelectedIndex(0);
    }

    /**
     * set the property
     *
     * @param tfo value_
     */
    public void setWidthFromComboBox(TwoFacedObject tfo) {
        double value = ((Double) tfo.getId()).doubleValue();
        if (value == 0.0) {
            return;
        }
        binWidthField.setTime(value);
        widthCbx.setSelectedIndex(0);
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
        try {
            boolean changed = (binRoundToField.getTime() != binRoundTo)
                              || (binWidth != binWidthField.getTime());
            binRoundTo = binRoundToField.getTime();
            binWidth   = binWidthField.getTime();
            if (changed) {
                flushCache();
            }
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Bad bin value");
            return false;
        }

        return true;
    }


    /**
     * Make the <code>DataChoices</code> for this <code>DataSource</code>.
     */
    public void doMakeDataChoices() {
        if (sources == null) {
            return;
        }
        String stationModelName = (String) getProperty(PROP_STATIONMODELNAME);
        Hashtable properties = Misc.newHashtable(DataChoice.PROP_ICON,
                                   "/auxdata/ui/icons/Placemark16.gif");
        if (stationModelName != null) {
            properties.put(PROP_STATIONMODELNAME, stationModelName);
        }
        DataChoice uberChoice = null;
        /*  Might want to do this someday
        uberChoice = new DirectDataChoice(this,
                                            sources, getName(),
                                            getDataName(),
                                            getPointCategories(),
                                            properties);
        */
        if (sources.size() > 1) {
            uberChoice = new CompositeDataChoice(this, sources, getName(),
                    getDataName(), getPointCategories(), properties);
            addDataChoice(uberChoice);
        }

        for (int i = 0; i < sources.size(); i++) {
            DataChoice choice = new DirectDataChoice(this, new Integer(i),
                                    getDescription(), getDataName(),
                                    getPointCategories(), properties);



            /*
              We'd like to create sub choices for each parameter but we don't really
              know the parameters until we read the data and that can be expensive
                          DirectDataChoice subChoice = new DirectDataChoice(this,
                                    (String) sources.get(i),
                                    getDescription(), getDataName(),
                                    getPointCategories(), properties);
                                    choice.addDataChoice(subChoice);*/

            if (sources.size() > 1) {
                ((CompositeDataChoice) uberChoice).addDataChoice(choice);
            } else {
                addDataChoice(choice);
            }
        }

    }


    /**
     * Get the file or url source path from the given data choice.
     * The new version uses an Integer index into the sources list
     * as the id of the data choice. However, this method does handle
     * 
     *
     * @param dataChoice The data choice
     *
     * @return The file or url the data choice refers to
     */
    protected String getSource(DataChoice dataChoice) {
        Object id = dataChoice.getId();
        if (id instanceof String) {
            return (String) id;
        } else if (id instanceof Integer) {
            int idx = ((Integer) id).intValue();
            return (String) sources.get(idx);
        }
        return null;
    }


    /**
     * Get the default categories for data from PointDataSource-s
     *
     * @return list of categories
     */
    protected List getPointCategories() {
        if (pointCategories == null) {
            pointCategories =
                DataCategory.parseCategories(DataCategory.CATEGORY_POINT
                                             + ";" + STATION_PLOT, false);
        }
        return pointCategories;
    }

    /**
     * Get the name of this data.
     *
     * @return name of data
     */
    public String getDataName() {
        return POINT_DATA;
    }


    /**
     * Get the data represented by this class.  Calls makeObs, real work
     * needs to be implemented there.
     *
     * @param dataChoice         choice for data
     * @param category           category of data
     * @param dataSelection      subselection properties
     * @param requestProperties  additional selection properties (not used here)
     * @return  Data object representative of the choice
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {


        GeoSelection    geoSelection = ((dataSelection != null)
                                        ? dataSelection.getGeoSelection()
                                        : null);
        GeoLocationInfo bbox         = ((geoSelection == null)
                                        ? null
                                        : geoSelection.getBoundingBox());


        LatLonRect      llr          = ((bbox != null)
                                        ? bbox.getLatLonRect()
                                        : null);


        FieldImpl       retField     = null;
        try {
            //List choices = (List) dataChoice.getId();
            List choices = (dataChoice instanceof CompositeDataChoice)
                           ? ((CompositeDataChoice) dataChoice)
                               .getDataChoices()
                           : Misc.toList(new DataChoice[] { dataChoice });
            List datas = new ArrayList(choices.size());
            for (int i = 0; i < choices.size(); i++) {
                FieldImpl obs = makeObs((DataChoice) choices.get(i),
                                        dataSelection, llr);
                if (obs == null) {
                    return null;
                }
                if(true) return obs;
                datas.add(obs);
                if ((fieldsDescription == null) && (obs != null)) {
                    makeFieldDescription(obs);
                }
            }
            retField = PointObFactory.mergeData(datas);
        } catch (Exception exc) {
            logException("Creating obs", exc);
        }
        return retField;
    }

    /**
     * Override the base class method to add on the listing of the
     * param names in the point tuple.
     *
     * @return   full description of this datasource for help
     */
    public String getFullDescription() {
        String parentDescription = super.getFullDescription();
        if (fieldsDescription == null) {
            /*
              Don't do this can this can cost us
            try {
                FieldImpl fi =
                    (FieldImpl) getData(getDescriptionDataChoice(), null,
                                        null);
            } catch (Exception exc) {
                logException("getting description", exc);
                return "";
            }
            */
        }
        return parentDescription + "<p>" + ((fieldsDescription != null)
                                            ? fieldsDescription
                                            : "");
    }

    /**
     * Get the data choice to use for the description
     *
     * @return  the data choice
     */
    protected DataChoice getDescriptionDataChoice() {
        return (DataChoice) getDataChoices().get(0);
    }

    /**
     * Create e field description from the field
     *
     * @param fi  field to use
     */
    protected void makeFieldDescription(FieldImpl fi) {
        if (fi == null) {
            fieldsDescription = "Bad data: null";
            return;
        }
        try {
            if (ucar.unidata.data.grid.GridUtil.isTimeSequence(fi)) {
                fi = (FieldImpl) fi.getSample(0);
            }
            PointOb    ob    = (PointOb) fi.getSample(0);
            Tuple      tuple = (Tuple) ob.getData();
            MathType[] comps = ((TupleType) tuple.getType()).getComponents();
            Trace.msg("PointDataSource #vars=" + comps.length);
            StringBuffer params = new StringBuffer(comps.length
                                      + " Fields:<ul>");
            String dataSourceName = getName();
            for (int i = 0; i < comps.length; i++) {
                params.append("<li>");
                String paramName =
                    ucar.visad.Util.cleanTypeName(comps[i].toString());
                DataAlias alias = DataAlias.findAlias(paramName);
                params.append(paramName);
                if (alias != null) {
                    params.append(" --  " + alias.getLabel());
                    DataChoice.addCurrentName(new TwoFacedObject(dataSourceName+">" + alias.getLabel()+" -- " + paramName,paramName)); 
                } else {
                    DataChoice.addCurrentName(new TwoFacedObject(dataSourceName+">" + paramName,paramName)); 
                }
                Data data = tuple.getComponent(i);
                if (data instanceof Real) {
                    Unit unit = ((Real) data).getUnit();
                    if (unit != null) {
                        params.append("  [" + unit.toString() + "]");
                    }
                }
            }
            fieldsDescription = params.toString();
        } catch (Exception exc) {
            logException("getting description", exc);
        }
    }



    /**
     * Make the observation data
     *
     * @param dataChoice  choice describing the data
     * @param subset subselection (not used)
     * @param bbox The bounding box
     *
     * @return FieldImpl of PointObs
     *
     * @throws Exception problem (VisAD or IO)
     */
    protected abstract FieldImpl makeObs(DataChoice dataChoice,
                                         DataSelection subset,
                                         LatLonRect bbox)
     throws Exception;




    /**
     * Set the source property (filename or URL).  Used by persistence
     *
     * @param value  data source
     */
    public void setSource(String value) {
        setSources(Misc.toList(new String[] { value }));
    }


    /**
     * Set the BinWidth property.
     *
     * @param value The new value for BinWidth
     */
    public void setBinWidth(double value) {
        binWidth = value;
    }

    /**
     * Get the BinWidth property.
     *
     * @return The BinWidth
     */
    public double getBinWidth() {
        return binWidth;
    }

    /**
     * Set the BinRoundTo property.
     *
     * @param value The new value for BinRoundTo
     */
    public void setBinRoundTo(double value) {
        binRoundTo = value;
    }

    /**
     * Get the BinRoundTo property.
     *
     * @return The BinRoundTo
     */
    public double getBinRoundTo() {
        return binRoundTo;
    }


}

