/*
 * $Id: IDV-Style.xjs,v 1.1 2006/05/03 21:43:47 dmurray Exp $
 *
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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


package ucar.unidata.data.storm;


import ucar.unidata.data.DataSourceDescriptor;

import ucar.unidata.data.DataSourceImpl;

import java.util.Hashtable;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 9, 2008
 * Time: 4:57:58 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class StormDataSource extends DataSourceImpl {

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public StormDataSource() throws Exception {}

    /**
     * _more_
     *
     * @param descriptor _more_
     * @param name _more_
     * @param description _more_
     * @param properties _more_
     */
    public StormDataSource(DataSourceDescriptor descriptor, String name,
                           String description, Hashtable properties) {
        super(descriptor, name, description, properties);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public abstract List<StormInfo> getStormInfos();

    /**
     * _more_
     *
     * @param stormInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public abstract StormTrackCollection getTrackCollection(StormInfo stormInfo)
     throws Exception;



}

