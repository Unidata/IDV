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

package ucar.unidata.idv;


import ucar.unidata.data.DataSourceResults;


import ucar.unidata.util.Misc;



/**
 * Holds the information needed to recreate a
 * {@link ucar.unidata.data.DataSource}
 *
 * @author IDV development team
 */


public class DataSourceHistory extends History {

    /**
     *  This is the xml encoded representation of the
     * {@link ucar.unidata.data.DataSource} object.
     */
    private String dataSourceXml;

    /**
     *  This (hopefully) uniquely identifies the data source since the xml cannot.
     */
    private String dataSourceIdentifier;

    /**
     * Parameterless ctor for xml encoding/decoding
     *
     */
    public DataSourceHistory() {}


    /**
     * Create this object with the gicen name and data source xml
     *
     * @param name The name
     * @param dataSourceXml  xml encoded representation of the  data source
     * @param identifier Some unique identifier
     */
    public DataSourceHistory(String name, String dataSourceXml,
                             String identifier) {
        super(name);
        this.dataSourceXml        = dataSourceXml;
        this.dataSourceIdentifier = identifier;
    }

    /**
     * Override the hashcode method to hash on the dataSourceXml
     * @return The hashcode
     */
    public int hashCode() {
        return super.hashCode() ^ Misc.hashcode(dataSourceXml)
               ^ Misc.hashcode(dataSourceIdentifier);
    }

    /**
     * Override the equals method. Compate the data source identifier
     *
     * @param o The other object
     * @return Is equals
     */
    public boolean equals(Object o) {
        if ( !(o instanceof DataSourceHistory)) {
            return false;
        }
        if ( !super.equals(o)) {
            return false;
        } else {
            DataSourceHistory that = (DataSourceHistory) o;
            return Misc.equals(this.dataSourceIdentifier,
                               that.dataSourceIdentifier);
        }
        //        return Misc.equals (dataSourceXml, that.dataSourceXml);
    }

    /**
     * Create the data source from the xml
     *
     * @param idv The IDV
     * @return Was the creation successful
     */
    public boolean process(IntegratedDataViewer idv) {
        DataSourceResults results = idv.makeDataSourceFromXml(dataSourceXml);
        idv.moveHistoryToFront(this);
        boolean ok = !results.anyFailed();
        if (ok) {
            idv.getIdvUIManager().dataSelectorToFront();
        }
        return ok;
    }



    /**
     *  Set the DataSourceXml property.
     *
     *  @param value The new value for DataSourceXml
     */
    public void setDataSourceXml(String value) {
        dataSourceXml = value;
    }

    /**
     *  Get the DataSourceXml property.
     *
     *  @return The DataSourceXml
     */
    public String getDataSourceXml() {
        return dataSourceXml;
    }


    /**
     *  Set the DataSourceIdentifier property.
     *
     *  @param value The new value for DataSourceIdentifier
     */
    public void setDataSourceIdentifier(String value) {
        dataSourceIdentifier = value;
    }

    /**
     *  Get the DataSourceIdentifier property.
     *
     *  @return The DataSourceIdentifier
     */
    public String getDataSourceIdentifier() {
        return dataSourceIdentifier;
    }





}
