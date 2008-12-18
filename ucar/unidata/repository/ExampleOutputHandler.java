/**
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


import org.w3c.dom.*;


import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;



/**
 * This is an example of an outputhandler. To show this in RAMADDA uncomment the
 * line in resources/outputhandlers.xml
 *
 * It defines 3 different outputs (example1, example2, example3).
 * example1 is applicable to groups
 * example2 is applicable to a single entry
 * example3 is applicable to a single entry but only shows up in the links and actions tab
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ExampleOutputHandler extends OutputHandler {

    /** example1 */
    public static final OutputType OUTPUT_EXAMPLE1 =
        new OutputType("Example 1", "example.example1", OutputType.TYPE_HTML);

    /** example2 */
    public static final OutputType OUTPUT_EXAMPLE2 =
        new OutputType("Example 2", "example.example2", OutputType.TYPE_HTML);

    /** example3 */
    public static final OutputType OUTPUT_EXAMPLE3 =
        new OutputType("Example 3", "example.example3", OutputType.TYPE_HTML);


    /**
     * Constructor
     *
     * @param repository The repository
     * @param element The xml element from outputhandlers.xml
     * @throws Exception On badness
     */
    public ExampleOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        //add in the output types
        addType(OUTPUT_EXAMPLE1);
        addType(OUTPUT_EXAMPLE2);
        addType(OUTPUT_EXAMPLE3);
    }



    /*
      getEntryLinks is the boiler plate code that is called
      by the repository and is used to determine what if any outputtypes are applicable
      to the given content.
    */



    /**
     * This method gets called to add in to the types list the OutputTypes that are applicable
     * to the given State.  The State can be viewing a single Entry (state.entry non-null),
     * viewing a Group (state.group non-null). These would show up along the top navigation bar.
     *
     * The Request holds all information about the request
     *
     * @param request The request
     * @param state The state
     * @param types The list to add to
     * @param links _more_
     * @param forHeader _more_
     *
     *
     * @throws Exception On badness
     */
    protected void getEntryLinks(Request request, State state,
                                 List<Link> links, boolean forHeader)
            throws Exception {
        //We'll add example1 when we are viewing a group and example2 when viewing a single entry
        if (state.group != null) {
            links.add(makeLink(request, state.group, OUTPUT_EXAMPLE1));
        }
        if (state.entry != null) {
            /*
              If you wanted to look at the file you could do:

              //Check if the resource is a file that exists
              if ( !entry.getResource().isFile()) {
                  File file = entry.getResource().getFile();
              }
            */


            links.add(makeLink(request, state.entry, OUTPUT_EXAMPLE2));
            links.add(makeLink(request, state.entry, OUTPUT_EXAMPLE3));
        }




    }


    /**
     * Output a group. We break apart the children groups from the children entries
     *
     * @param request The request
     * @param group The group
     * @param subGroups Children groups
     * @param entries Children entries
     *
     * @return A Result object that holds the content
     *
     * @throws Exception On badness
     */
    public Result outputGroup(Request request, Group group,
                              List<Group> subGroups, List<Entry> entries)
            throws Exception {

        //Here output should be example1
        OutputType output = request.getOutput();

        //The stringbuffer holds the content we are creating
        StringBuffer sb = new StringBuffer();

        //Lets just make a list of links to the children
        //All access urls are defined using the RequestUrl class
        //URL_ENTRY_SHOW is used for showing all content through the output handlers
        if (subGroups.size() > 0) {
            sb.append("Sub-groups:<br>");
            for (Group childGroup : subGroups) {
                sb.append(
                    HtmlUtil.href(
                        request.entryUrl(
                            getRepository().URL_ENTRY_SHOW,
                            childGroup), childGroup.getName()));
                sb.append(HtmlUtil.br());
            }
        }

        if (entries.size() > 0) {
            sb.append("<p>Sub-entries:<br>");
            for (Entry entry : entries) {
                sb.append(
                    HtmlUtil.href(
                        request.entryUrl(
                            getRepository().URL_ENTRY_SHOW,
                            entry), entry.getName()));
                sb.append(HtmlUtil.br());
            }
        }

        //All content gets passed back through a Result object.
        //Here we make one with the "Example 1" title and the stringbuffer which assumes it is html
        Result result = new Result("Example 1", sb);

        //This adds the navigation links to other output handlers that are applicable to this content
        addLinks(request, result, new State(group, subGroups, entries));
        return result;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, Entry entry) throws Exception {
        OutputType output = request.getOutput();

        //Check if we are doing example3
        if (output.equals(OUTPUT_EXAMPLE3)) {
            return outputExample3(request, entry);
        }

        //Here output should be example2
        StringBuffer sb = new StringBuffer();

        /*
           Accessing arguments.

           The request allows you to access url arguments:

           request.defined("argname") Returns true if argname exists and is a non-0 length string
           request.getString("argname")  Returns the string value or "" if not exists
           request.getString("argname")  Returns the string value or "" if not exists
           Returns an int, double, Date or boolean:
           int intValue = request.getString("argname", int dflt)
           double doubleValue = request.getString("argname", double dflt)
           Date dateValue = request.getString("argname", Date dflt)
           boolean booleanValue = request.getString("argname", boolean dflt)
         */


        //Now, we just append html to the sb
        sb.append("Example 2 content");


        /*
          Returning a result

          If you wanted to return something (e.g., xml) that is not html you could do:
          Result result = new Result("", sb,"some mime type");
          or:
          Result result = new Result("", sb,repository.getMimeTypeFromSuffix(".xml"));
          or:
          Result result = new Result(String title, byte[] content, String mimeType);
          or:
          Result result = new Result(String title, InputStream inputStream, String mimeType);

          If you wanted to redirect to another URL do:
          Result result = new Result(redirectUrl);

        */


        Result result = new Result("Example 2", sb);

        //This adds the navigation links to other output handlers that are applicable to this content
        addLinks(request, result, new State(entry));
        return result;

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputExample3(Request request, Entry entry)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("Here is the content for example3");
        sb.append(HtmlUtil.p());
        sb.append("Name:");
        sb.append(entry.getName());
        sb.append(HtmlUtil.p());
        sb.append("Description:");
        sb.append(entry.getDescription());

        Result result = new Result("Example 3", sb);

        //This adds the navigation links to other output handlers that are applicable to this content
        addLinks(request, result, new State(entry));
        return result;

    }



}

