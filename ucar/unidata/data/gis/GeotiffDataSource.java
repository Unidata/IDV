/*
 * $Id: GeotiffDataSource.java,v 1.14 2007/04/16 20:34:52 jeffmc Exp $
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


package ucar.unidata.data.gis;


import ucar.visad.Util;

import ucar.unidata.data.*;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import visad.*;

import visad.data.DefaultFamily;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;

/**
 * This is an implementation that will read in a generic data file
 * and return a single Data choice that is a VisAD Data object.
 *
 * @author IDV Development team
 * @version $Revision: 1.14 $
 */
public class GeotiffDataSource extends FilesDataSource {

    /** the adapter */
    private GeotiffAdapter adapter;

    private String paramName="";

    private String unit="";


    /**
     *  Parameterless constructor for XML encoding.
     */
    public GeotiffDataSource() {}


    /**
     * Just pass through to the base class the ctor arguments.
     *
     * @param descriptor    Describes this data source, has a label etc.
     * @param filename      This is the filename (or url) that
     *                      points to the actual data source.
     * @param properties General properties used in the base class
     *
     * @throws VisADException
     */
    public GeotiffDataSource(DataSourceDescriptor descriptor,
                             String filename, Hashtable properties)
            throws VisADException {
        super(descriptor, filename, "Tiff data source", properties);
        openData();
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
     * Initialize if being unpersisted.
     */
    public void initAfterUnpersistence() {
        //From a legacy bundle
        if (sources == null) {
            sources = Misc.newList(getName());
        }
        super.initAfterUnpersistence();
        openData();
    }


    private JTextField nameFld;
    private JTextField unitFld;

    public boolean applyProperties() {
	paramName = nameFld.getText().trim();
	unit = unitFld.getText().trim();
	return super.applyProperties();
    }

    public void getPropertiesComponents(List comps) {
	super.getPropertiesComponents(comps);
	nameFld = new JTextField(paramName, 20);
	unitFld = new JTextField(unit, 10);

	comps.add(GuiUtils.filler());
	comps.add(getPropertiesHeader("Parameter"));
	comps.add(GuiUtils.rLabel("Name:"));
	comps.add(GuiUtils.left(nameFld));
	comps.add(GuiUtils.rLabel("Unit:"));
	comps.add(GuiUtils.left(unitFld));

    }

    /**
     * Open the data.
     */
    private void openData() {
        try {
            if(adapter==null) {
                adapter = new GeotiffAdapter(getSource());
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            setInError(true,
                       "Failed to open " + getSource() + " "
                       + exc.getMessage());
        }
    }


    /**
     * This method is called at initialization time and  should create
     * a set of {@link ucar.unidata.data.DirectDataChoice}-s  and add them
     * into the base class managed list of DataChoice-s with the method
     * addDataChoice.
     */
    protected void doMakeDataChoices() {
        //Now let's create a data choice for the file 
        String description = getSource();
        String desc = IOUtil.getFileTail(getSource());
        List categories;
        openData();
        if (adapter == null) {
            return;
        }
        try {
            if (adapter.getHasProjection()) {
                categories = DataCategory.parseCategories("RGBIMAGE",
                        false);
                addDataChoice(new DirectDataChoice(this, new Object[]{"image"}, desc + " -  3 Color RGB",
                                                   desc + " -  3 Color RGB", categories,
                                                   DataChoice.NULL_PROPERTIES));
                categories = DataCategory.parseCategories("IMAGE-2D;GRID-2D", false);
		String name = paramName;
		if(name==null || name.length()==0) 
		    name = desc+" -  Grid data";
		addDataChoice(new DirectDataChoice(this, new Object[]{"grid"}, name,
                                                   desc+" -  Grid data", categories,
                                                   DataChoice.NULL_PROPERTIES));

            } else {
               categories = DataCategory.parseCategories("TIFFIMAGE;"
                        + desc, false);
                addDataChoice(new DirectDataChoice(this, "Image", desc,
                                                   desc, categories,
                                                   DataChoice.NULL_PROPERTIES));
            }

        } catch (Exception exc) {
            exc.printStackTrace();
            setInError(true,
                       "Failed to make data choices " + getSource() + " "
                       + exc.getMessage());
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getFullDescription() {
        String desc = super.getFullDescription();
        if (adapter != null) {
            desc = desc + "<p><b>Keys:</b> <pre>" + adapter.getKeyString()
                   + "</pre>";
        }
        return desc;

    }


    /**
     * This method should create and return the visad.Data that is
     * identified by the given {@link ucar.unidata.data.DataChoice}.
     *
     * @param dataChoice This is one of the DataChoice-s that was created
     *                   in the doMakeDataChoices call above.
     * @param category   The specific {@link ucar.unidata.data.DataCategory}
     *                   which the {@link ucar.unidata.idv.DisplayControl} was
     *                   instantiated with. Usually can be ignored.
     * @param dataSelection This may contain a list of times which
     *                      subsets the request.
     * @param requestProperties  extra request properties
     * @return The {@link visad.Data} object represented by the given dataChoice
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        try {
            openData();
            if (adapter == null) {
                return null;
            }
            Object id = dataChoice.getId();

            if(id instanceof String) {
                //old way
                return adapter.getData();
            }
            Object[]idArray = (Object[]) id;
            if(idArray[0].equals("grid")) {
		FieldImpl field = (FieldImpl) adapter.getDataAsGrid();
		if(unit!=null && unit.length()>0) {
		    Unit newunit = Util.parseUnit(unit);
		    RealType newType = Util.makeRealType(paramName, newunit);
		    field= GridUtil.setParamType(field, newType);
		}
		return field;
            } else {
                return adapter.getDataAsRgb();
            }




        } catch (java.io.IOException ioe) {
            throw new VisADException("Failed to get tiff data:" + ioe);
        }
    }


    /**
     * You can also override the base class method to return the list
     * of all date/times that this DataSource holds.
     * @return This should be an List of {@link visad.DateTime} objects.
     */
    protected List doMakeDateTimes() {
        // don't know for this, so return empty ArrayList
        return new ArrayList();
    }


    /**
       Set the ParamName property.

       @param value The new value for ParamName
    **/
    public void setParamName (String value) {
	this.paramName = value;
    }

    /**
       Get the ParamName property.

       @return The ParamName
    **/
    public String getParamName () {
	return this.paramName;
    }

    /**
       Set the Unit property.

       @param value The new value for Unit
    **/
    public void setUnit (String value) {
	this.unit = value;
    }

    /**
       Get the Unit property.

       @return The Unit
    **/
    public String getUnit () {
	return this.unit;
    }




}

