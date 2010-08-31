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

package ucar.unidata.repository;


import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public abstract class AdminHandlerImpl extends RepositoryManager implements AdminHandler {


    /**
     * _more_
     */
    public AdminHandlerImpl() {
        super(null);
    }

    /**
     * _more_
     *
     * @param repository _more_
     *
     * @throws Exception _more_
     */
    public void setRepository(Repository repository) throws Exception {
        this.repository = repository;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Repository getRepository() {
        return repository;
    }


    /**
     * _more_
     *
     * @param blockId _more_
     * @param sb _more_
     */
    public void addToSettingsForm(String blockId, StringBuffer sb) {}

    public void applySettingsForm(Request request) throws Exception {}

    /**
     * _more_
     *
     * @return _more_
     */
    public List<RequestUrl> getUrls() {
        return null;
    }


}
