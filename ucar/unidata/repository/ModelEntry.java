/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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



package ucar.unidata.repository;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.util.Date;
import java.util.List;




/**
 * Class ModelEntry _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ModelEntry extends Entry {


    /** _more_ */
    private String modelGroup;

    private String modelRun;



    /**
     * _more_
     *
     *
     *
     * @param name _more_
     * @param description _more_
     * @param group _more_
     * @param file _more_
     * @param model _more_
     * @param date _more_
     */
    public ModelEntry(String id, String type,
                      String name, String description, Group group,
                      User user,
                      String file,  String modelGroup,String modelRun, long date) {
        super(id, type, name, description, group, user, file,
              new Date().getTime(), date,date);
        this.modelGroup = modelGroup;
        this.modelRun = modelRun;
    }



    /**
     * Set the ModelGroupl property.
     *
     * @param value The new value for ModelGroupl
     */
    public void setModelGroup(String value) {
        modelGroup = value;
    }

    /**
     * Get the ModelGroup property.
     *
     * @return The ModelGroup
     */
    public String getModelGroup() {
        return modelGroup;
    }



    /**
     * Set the ModelGroupl property.
     *
     * @param value The new value for Model
     */
    public void setModelRun(String value) {
        modelRun = value;
    }

    /**
     * Get the ModelRun property.
     *
     * @return The ModelRun
     */
    public String getModelRun() {
        return modelRun;
    }


}

