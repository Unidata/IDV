/*
 * $Id: ExampleDataSource.java,v 1.16 2007/06/01 14:36:48 dmurray Exp $
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


package ucar.unidata.apps.example;



import visad.VisADException;
import visad.Data;

import java.rmi.RemoteException;


import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;

import ucar.unidata.data.DirectDataChoice;

import ucar.unidata.util.Misc;
import ucar.unidata.util.IOUtil;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *  This is an example implementation of a
 *  {@link ucar.unidata.data.DataSource}.
 *  See the file <a href=docs/datasource.html>docs/datasource.html</a>
 *  Normally, new DataSources need to be registered with the
 *  IDV using the datasource.xml file. However, for this
 *  example application we have done that already.
 *  This DataSource is applicable to .txt and .html files (defined
 *  as patterns in datasource.xml).
 *  When you run the ExampleIdv (java ucar.unidata.apps.example.ExampleIdv)
 *  you can open up the data chooser, select any .txt file or .html file.
 *  This DataSource will automagically get created. When you view the DataTree
 *  (JTree) you should see the DataSource and a set of 3 example data choices
 *  (described below).
 */


public class ExampleDataSource extends DataSourceImpl {

    /** local holder for filename */
    String myFilename;

    /**
     * Default bean constructor for persistence; does nothing.
     */
    public ExampleDataSource() {}

    /**
     *  Just pass through to the base class the ctor arguments.
     *  @param descriptor Describes this data source, has a label etc.
     *  @param filename This is the filename (or url) that points to
     *  the actual data source.
     *  @param properties General properties used in the base class.
     */

    public ExampleDataSource(DataSourceDescriptor descriptor,
                             String filename, Hashtable properties) {
        //Pass the filename up to the base class as the name of this DataSource
        super(descriptor, "File:" + filename,
              "Description of example data source", properties);
        myFilename = filename;
    }



    /**
     *  This method is called at initialization time and  should
     *  create a set of  {@link ucar.unidata.data.DirectDataChoice}-s
     *  and add them into the base class managed list of DataChoice-s
     *  with the method addDataChoice.
     */
    protected void doMakeDataChoices() {

        //Now let's create a data choice for the text file 

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
        DataCategory displayCategory = new DataCategory(myFilename, true);
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

        if (myFilename.endsWith(".html")) {
            categories.add(new DataCategory(DataCategory.CATEGORY_HTML,
                                            false));
        } else {
            categories.add(new DataCategory(DataCategory.CATEGORY_TEXT,
                                            false));
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
         *  DataCategory.parseCategories (myFilename +";" +  DataCategory.CATEGORY_TEXT);
         */


        /**
         *  Now, we create the data choice. Use the filename
         *  for both the  id and the name of the data choice.
         */

        DataChoice dc;
        String     description = "File: " + myFilename;
        dc = new DirectDataChoice(this,  //me - DataSource
                                  myFilename,  //identifier - can be any Object
                                  myFilename,  //name - String
                                  description, //description 
                                  categories,  //list of categories
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
                                  myFilename,  //identifier - can be any Object
                                  "Test choice",  //name - String
                                  "Test choice desc.",          //description 
                                  newCatList,  //list of categories
                                  DataChoice.NULL_PROPERTIES);  //no properties
        addDataChoice(dc);



        /**
         *  And finally lets add one that has no display category.
         */
        DataCategory noDisplayCategory =
            new DataCategory(DataCategory.CATEGORY_TEXT, false);
        List noDisplayCatList = new ArrayList();
        noDisplayCatList.add(noDisplayCategory);
        dc = new DirectDataChoice(this,  //me - DataSource
                                  myFilename,  //identifier - can be any Object
                                  "No display cat. choice",  //name - String
                                  "No display cat.  choice desc.",  //description 
                                  noDisplayCatList,  //list of categories
                                  DataChoice.NULL_PROPERTIES);  //no properties
        addDataChoice(dc);

    }




    /**
     *  This method should create and return the visad.Data that
     *  is identified by the given {@link ucar.unidata.data.DataChoice}.
     *
     *  @param dataChoice This is one of the DataChoice-s that was created
     *  in the doMakeDataChoices call above.
     * @param requestProperties
     *
     *  @param category The specific {@link ucar.unidata.data.DataCategory}
     *  which the {@link ucar.unidata.idv.DisplayControl} was instantiated with.
     *  Usually can be ignored.
     *
     *  @param dataSelection This may contain a list of times which subsets the request.
     *                       Of course for this example we have no times.
     *  @return The {@link visad.Data} object represented by the given dataChoice
     *
     * @throws RemoteException
     * @throws VisADException
     */

    protected Data getDataInner(DataChoice dataChoice, DataCategory category, DataSelection dataSelection, Hashtable requestProperties)
            throws VisADException, RemoteException {

        String filename = dataChoice.getId().toString();
        //For this example the data choice represents a text file name.
        try {
            //Use the Misc method to read in the contents of the file
            String fileContents = IOUtil.readContents(filename);
            return new visad.Text(fileContents);
        } catch (Exception exc) {
            logException("getData", exc);
        }
        return null;
    }


    /**
     *  Create and return the list of DateTime-s associated with
     *  this DataSource. You can also override the base class method:
     *  getAllDateTimes () to return the list  of all date/times that
     *  this DataSource holds.  For this example  there are no times.
     *  @return This should be a List of {@link visad.DateTime} objects.
     */
    protected List doMakeDateTimes() {
        return new ArrayList();
    }

}








