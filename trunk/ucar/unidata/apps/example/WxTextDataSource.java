/*
 * $Id: WxTextDataSource.java,v 1.4 2007/06/01 22:35:12 dmurray Exp $
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

package ucar.unidata.apps.example;


import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import visad.Data;
import visad.DataReference;
import visad.Text;
import visad.VisADException;
import visad.VisADException;

import visad.data.mcidas.AddeTextAdapter;

import java.io.FileInputStream;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;



/**
 * Class for data sources of ADDE weather text bulletins.
 *
 * @author IDV development team
 * @version $Revision: 1.4 $
 */

public class WxTextDataSource extends DataSourceImpl {

    /** my URL*/
    String myUrl;


    /**
     * Default bean constructor for persistence; does nothing.
     */
    public WxTextDataSource() {}

    /**
     * Create a new WxTextDataSource, just pass through to the base 
     * class the ctor arguments.
     *
     * @param descriptor  Describes this data source, has a label etc.
     * @param filename    This is the filename (or url) that points to
     *                    the actual data source (ADDE URL).
     * @param properties General properties used in the base class.
     */
    public WxTextDataSource(DataSourceDescriptor descriptor,
                              String filename, Hashtable properties) {
        super(descriptor, filename, "Weather Text", properties);
        myUrl = filename;
    }



    /**
     * Make the data choices assoicated with this source.
     *
     *  This method is called at initialization time and  should
     *  create a set of  {@link ucar.unidata.data.DirectDataChoice}-s
     *  and add them into the base class managed list of DataChoice-s
     *  with the method addDataChoice.
     */
    protected void doMakeDataChoices() {

        //Create a list of categories for this data choice. 
        List categories = new ArrayList();

        /**
         *  We use the filename attribute  as the
         *  first category which acts as the "display" category, i.e.,
         *  the one used to place this data choice in the JTree in the GUI
         *  What will happen in the GUI - there will be a tree node that
         *  represents the DataSource, under it will be a tree node that
         *  represents this category. Under there will be a tree node that
         *  represents the DataChoice
         */
        DataCategory displayCategory = new DataCategory(myUrl, true);
        categories.add(displayCategory);

        /**
         *  Now add in the TEXT or HTML category. This is the one that
         *  tells the DisplayControl-s what flavor of data this DataChoice
         *  represents. The false argument says that these are *not
         *  display categories. i.e., they are not used to put this
         *  DataChoice into the gui but they *are* used to do the actually
         *  matching up. If these were to be true and there are no other
         *  non-display category then *all* displays will considerd to
         *  be applicable to  this DataChoice
         */

        if (myUrl.startsWith("adde") && (myUrl.indexOf("wxtext") > 0)) {
            categories.add(new DataCategory(DataCategory.CATEGORY_HTML, false));
        } else {
            categories.add(new DataCategory(DataCategory.CATEGORY_TEXT, false));
        }

        /**
         *  The CATEGORY_HTML and CATEGORY_TEXT are simply the
         *  strings: "html" and "text".
         *
         *  What  DisplayControl-s are applicable to what categorized
         *  DataChoice's is defined in: ucar/unidataidv/resources/controls.xml
         *  (Or in any local controls.xml file that you load in here).
         *  Looking at controls.xml we see that the TextDisplayControl and
         *  the OmniViewer can take  html or text categories:
         *
         *  <control
         *       categories="text;html"
         *       class="ucar.unidata.idv.control.TextDisplayControl"
         *  .../>
         *  <control
         *       categories="*"
         *       class="ucar.unidata.idv.control.OmniControl"
         *  .../>
         *
         *  The "*" above says match anything.
         *
         *  Note: the above creation of the DataCategories
         *  could have been accomplished with the DataCategory class
         *  utility method parseCategories which takes a semi-colon
         *  delimited list of categories:
         *  DataCategory.parseCategories (myUrl +";" +  DataCategory.CATEGORY_TEXT);
         */

         /**
          *  Now let's add a new category special to this data source
          */
         categories.add(new DataCategory("wxtext", false));


        /**
         *  Now, we create the data choice. Use the filename
         *  for both the  id and the name of the data choice.
         */

        DataChoice dc;
        String description = "Weather Text Bulletin";
        dc = new DirectDataChoice(this,         //me - DataSource
                                  myUrl,        //identifier - can be any Object
                                  myUrl,        //name - String
                                  description,  //description 
                                  categories,   //list of categories
                                  DataChoice.NULL_PROPERTIES);  //no proeprties
        addDataChoice(dc);

        /**
         *   Now just to further demonstrate how you can use categories
         *   let's  add in a DataChoice with a different
         *   display category, this time a hierarchhical one:
         */
        List newCatList =
            DataCategory.parseCategories("Some-Test-Category;text");
        dc = new DirectDataChoice(this,  //me - DataSource
                                  myUrl,  //identifier - can be any Object
                                  "Test choice",  //name - String
                                  "Test choice desc.",          //description 
                                  newCatList,  //list of categories
                                  DataChoice.NULL_PROPERTIES);  //no properties
        addDataChoice(dc);

    }

    /**
     * Actually get the data identified by the given DataChoce. The default is
     * to call the getDataInner that does not take the requestProperties. This
     * allows other, non unidata.data DataSource-s (that follow the old API)
     * to work.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The visad.Text object
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        String          filename = dataChoice.getStringId();
        AddeTextAdapter ata      = new AddeTextAdapter(filename);
        return ata.getDataAsHTML();
    }

    /**
     * Set the URL for this Data Source (used by persistence)
     * @param url  the ADDE URL as a string
     */
    public void setURL(String url) {
        myUrl = url;
    }

    /**
     * Get the URL for this Data Source
     * @return the ADDE URL as a string
     */
    public String getURL() {
        return myUrl;
    }
}
