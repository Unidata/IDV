/*
 * $Id: DataContext.java,v 1.29 2006/12/01 20:41:21 jeffmc Exp $
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


import ucar.unidata.idv.IdvContext;
import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.JythonManager;

import ucar.unidata.xml.XmlObjectStore;


import java.util.List;


/**
 *
 * This  interface defines the context in which
 * the collection of data  classes (e.g., {@link DataChoice},
 * {@link DirectDataChoice}, {@link DerivedDataDescriptor},
 * {@link DataSource} etc.) exist. For now the DataContext is
 * the {@link ucar.unidata.idv.IntegratedDataViewer}
 * (IDV). It is mostly used by the {@link DerivedDataChoice}
 * to have the user select the {@link DataChoice} operands  needed
 * when evaluating the DerivedDataChoice.
 *
 * @author IDV development team
 * @version $Revision: 1.29 $Date: 2006/12/01 20:41:21 $
 */
public interface DataContext extends IdvContext {


    /**
     * Get the reference to the idv
     *
     * @return The idv
     */
    public IntegratedDataViewer getIdv();


    /**
     * Return the list of {@link DataSource} objects currently held
     * @return  List of DataSources
     */
    public List getDataSources();


    /**
     * Return the list of {@link DataSource} objects currently held, including
     * the formula data source
     * @return  List of DataSources
     */
    public List getAllDataSources();

    /**
     * Select a set of {@link DataChoice}-s, one for each label (String)
     * contained within the  labels List. This may return null
     * to denote a user cancellation.
     *
     * @param labels   labels to associate with choices
     * @return  List of DataChoices
     */
    public List selectDataChoices(List labels);

    /**
     * Get the given user preference
     *
     * @param pref The name of the preference
     *
     * @return The value
     */
    public Object getPreference(String pref);

    /**
     * Select a set of Strings, one for each label (String)
     * contained within the  userChoices  List. This may return null
     * to denote a user cancellation. Typically a user interface is
     * generated based on the userChoices that is made of a set of
     * JTextFields in which the user enters the required values.
     *
     * @param msg            message prompt
     * @param userChoices    list of user choices
     * @return  List of user choices  (may be null)
     */
    public List selectUserChoices(String msg, List userChoices);


    /**
     * This method gets called when something changed in the data source.
     *
     * @param source The data source that changed
     */
    public void dataSourceChanged(DataSource source);


    /**
     * Add the given {@link DataSource}. For the IDV this entails
     * displaying the DataSource in the jtrees and menus, etc.
     *
     * @param dataSource   the DataSource to load
     * @return  true if the DataSource was loaded
     */
    public boolean loadDataSource(DataSource dataSource);


    /**
     * Return the {@link ucar.unidata.idv.JythonManager}.
     * This is the class that manages the
     * set of jython interpreters, etc.,
     *
     * @return The JythonManager
     */
    public JythonManager getJythonManager();


    /**
     * Return the {@link ucar.unidata.idv.IdvResourceManager}.
     * This is the class that manages the
     * set of idv resources etc.,
     *
     * @return The IdvResourcemanager
     */
    public IdvResourceManager getResourceManager();


    /**
     * Return the {@link ucar.unidata.xml.XmlObjectStore} that is  used
     * to get and store  persistent user state.
     *
     * @return The object store
     */
    public XmlObjectStore getObjectStore();


    /**
     * Ask the user what data type to use
     *
     * @param definingObject defines the data source to be created
     *
     * @return The data type or null
     */
    public String selectDataType(Object definingObject);




    /**
     * Ask the user what data type to use
     *
     * @param definingObject defines the data source to be created
     * @param label the label
     *
     * @return The data type or null
     */
    public String selectDataType(Object definingObject, String label);

}

