/*
 * $Id: DataSourceResults.java,v 1.17 2006/12/01 20:41:22 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.data;



import java.util.ArrayList;
import java.util.List;



/**
 * Holds the results from the createDataSource call.
 *
 * @author IDV Development Team
 * @version $Revision: 1.17 $
 */
public class DataSourceResults {

    /** List of sucessfully created DataSources */
    private List successDataSources = new ArrayList();

    /** List of the defining objects for successDataSources */
    private List successDefiningObjects = new ArrayList();


    /** List of exceptions for failed creations */
    private List failedExceptions = new ArrayList();

    /** List of defining objects for failed creations */
    private List failedDefiningObjects = new ArrayList();


    /**
     * Default bean constructor; does nothing
     */
    public DataSourceResults() {}

    /**
     * Create a DataSourceResults for the given parameters
     *
     * @param dataSource          the DataSource
     * @param definingObject      the defining object for that source
     */
    public DataSourceResults(DataSource dataSource, Object definingObject) {
        addSuccess(dataSource, definingObject);
    }

    /**
     * Create a DataSourceResults from the failed parameters.
     *
     * @param failedDefiningObject   defining object for failure
     * @param exc                    failed exception
     */
    public DataSourceResults(Object failedDefiningObject, Throwable exc) {
        addFailed(failedDefiningObject, exc);
    }

    /**
     * Merge the results of another DataSourceResults to this one
     *
     * @param results    other results.
     */
    public void merge(DataSourceResults results) {
        successDataSources.addAll(results.successDataSources);
        successDefiningObjects.addAll(results.successDefiningObjects);
        failedExceptions.addAll(results.failedExceptions);
        failedDefiningObjects.addAll(results.failedDefiningObjects);
    }

    /**
     * Get the list of successfully created DataSources
     *
     * @return   list of DataSources
     */
    public List getDataSources() {
        return successDataSources;
    }

    /**
     * Get the list of successful defining objects.
     *
     * @return  list of successes
     */
    public List getSuccessData() {
        return successDefiningObjects;
    }

    /**
     * Get the list of failure exceptions.
     *
     * @return  list of failed exceptions
     */
    public List getExceptions() {
        return failedExceptions;
    }

    /**
     * Get the list of failed data.
     *
     * @return  List of failed defining objects
     */
    public List getFailedData() {
        return failedDefiningObjects;
    }

    /**
     * See if there were any failures.
     *
     * @return  true if there were any failures
     */
    public boolean anyFailed() {
        return failedDefiningObjects.size() > 0;
    }

    /**
     * See if there were any successes.
     *
     * @return  true if there were any successes
     */
    public boolean anyOk() {
        return successDefiningObjects.size() > 0;
    }


    /**
     * See if all the results were failures.
     *
     * @return  true if no successes.
     */
    public boolean allFailed() {
        return (failedDefiningObjects.size() > 0)
               && (successDataSources.size() == 0);
    }

    /**
     * See if all the results were okay.
     *
     * @return  true if no failures
     */
    public boolean allOk() {
        return failedDefiningObjects.size() == 0;
    }


    /**
     * Add a successful result to the lists
     *
     * @param dataSource       successfully created DataSource
     * @param definingObject   the defining object for <code>dataSource</code>.
     */
    public void addSuccess(DataSource dataSource, Object definingObject) {
        successDataSources.add(dataSource);
        successDefiningObjects.add(definingObject);
    }


    /**
     * Add a failed defining object and the failure exception to the lists.
     *
     * @param definingObject     defining object for the DataSource
     * @param exception          failure exception
     */
    public void addFailed(Object definingObject, Throwable exception) {
        failedDefiningObjects.add(definingObject);
        failedExceptions.add(exception);
    }


    /**
     * Get a String representation of this DataSourceResults.
     * @return  a string represenation of this
     */
    public String toString() {
        return "Failed: " + failedDefiningObjects + "\nSuccess:"
               + successDataSources;
    }

}

