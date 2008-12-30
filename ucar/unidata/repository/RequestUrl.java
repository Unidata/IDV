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


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class RequestUrl {

    /** _more_ */
    private RepositorySource repositorySource;

    /** _more_ */
    private String path = "foo";

    /** _more_ */
    private String label = null;

    /**
     * _more_
     *
     *
     * @param repositorySource _more_
     * @param path _more_
     */
    public RequestUrl(RepositorySource repositorySource, String path) {
        this.repositorySource = repositorySource;
        this.path             = path;
    }

    /**
     * _more_
     *
     *
     * @param repositorySource _more_
     * @param path _more_
     * @param label _more_
     */
    public RequestUrl(RepositorySource repositorySource, String path,
                      String label) {
        this(repositorySource, path);
        this.label = label;
    }

    /**
     * _more_
     *
     * @param suffix _more_
     *
     * @return _more_
     */
    public String getFullUrl(String suffix) {
        return repositorySource.getRepositoryBase().absoluteUrl(
            repositorySource.getRepositoryBase().getUrlBase()
            + path) + suffix;
    }

    /**
     * _more_
     *
     * @param suffix _more_
     *
     * @return _more_
     */
    public String getHttpsUrl(String suffix) {
        return repositorySource.getRepositoryBase().httpsUrl(
            repositorySource.getRepositoryBase().getUrlBase()
            + path) + suffix;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getFullUrl() {
        return repositorySource.getRepositoryBase().absoluteUrl(
            repositorySource.getRepositoryBase().getUrlBase() + path);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return repositorySource.getRepositoryBase().getUrlBase() + path;
    }


    /**
     * _more_
     *
     * @param collectionPath _more_
     *
     * @return _more_
     */
    public String getUrl(String collectionPath) {
        return repositorySource.getRepositoryBase().getUrlBase() + "/"
               + collectionPath + path;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String getLabel() {
        return label;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getPath() {
        return path;
    }

    public boolean equals(Object o) {
        if(!(o instanceof RequestUrl)) return false;
        RequestUrl that = (RequestUrl) o;
        return this.path.equals(that.path);
    }


}

