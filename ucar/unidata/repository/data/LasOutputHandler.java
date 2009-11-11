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

package ucar.unidata.repository.data;


import org.w3c.dom.*;

import ucar.unidata.repository.*;
import ucar.unidata.repository.output.*;

import ucar.unidata.xml.XmlUtil;


import java.util.ArrayList;
import java.util.List;

import ucar.nc2.Variable;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;




/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class LasOutputHandler extends OutputHandler {

    public static final String NCATTR_STANDARD_NAME = "standard_name";
public static final String TAG_LASDATA = "lasdata";
public static final String TAG_INSTITUTION = "institution";
public static final String TAG_OPERATIONS = "operations";
public static final String TAG_SHADE = "shade";
public static final String TAG_ARG = "arg";
public static final String TAG_DATASETS = "datasets";
public static final String TAG_VARIABLES = "variables";
public static final String TAG_LINK = "link";
public static final String TAG_COMPOSITE = "composite";
public static final String TAG_GRIDS = "grids";
public static final String TAG_AXES = "axes";

public static final String ATTR_NAME = "name";
public static final String ATTR_URL = "url";
public static final String ATTR_CLASS = "class";
public static final String ATTR_METHOD = "method";
public static final String ATTR_TYPE = "type";
public static final String ATTR_DOC = "doc";
public static final String ATTR_UNITS = "units";
public static final String ATTR_MATCH = "match";
public static final String ATTR_JS = "js";
public static final String ATTR_SIZE = "size";
public static final String ATTR_START = "start";
public static final String ATTR_STEP = "step";




    /** The output type */
    public static final OutputType OUTPUT_LAS_XML =
        new OutputType("LAS-XML", "las.xml",
                       OutputType.TYPE_NONHTML,
                       "", null);

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public LasOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_LAS_XML);
    }


    /**
     * This method gets called to determine if the given entry or entries can be displays as las xml
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {


	DataOutputHandler dataOutputHandler = getDataOutputHandler();
	if (state.group != null) {
	    for (Entry child : state.getAllEntries()) {
		if (dataOutputHandler.canLoadAsGrid(child)) {
		    links.add(makeLink(request, state.group, OUTPUT_LAS_XML));
		    break;
		}
	    }
	} else if (state.entry != null) {
	    if (dataOutputHandler.canLoadAsGrid(state.entry)) {
		    links.add(makeLink(request, state.entry, OUTPUT_LAS_XML));
	    }
	}
    }


    private String getTagName(String s) {
	s = s.replace(" ", "_");
	s = s.replace("-", "_");
	return s;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, Group group,
                              List<Group> subGroups, List<Entry> entries)
            throws Exception {

	return outputLas(request, entries);
    }


    public Result outputEntry(final Request request, Entry entry)
            throws Exception {
	List<Entry> entries = new ArrayList<Entry>();
	entries.add(entry);
	return outputLas(request, entries);
    }

    public Result outputLas(Request request, List<Entry> entries)
            throws Exception {

	DataOutputHandler dataOutputHandler = getDataOutputHandler();
        Document doc = XmlUtil.makeDocument();
        Element root = XmlUtil.create(doc, TAG_LASDATA, null,
                                      new String[] {});


        XmlUtil.create(TAG_INSTITUTION, root, new String[]{
		ATTR_NAME,
		getRepository().getRepositoryName(),
		ATTR_URL,
		getRepository().absoluteUrl("")
	    });
        Element datasetsNode = XmlUtil.create(TAG_DATASETS, root);

        for (Entry entry : entries) {
	    if (!dataOutputHandler.canLoadAsGrid(entry)) {
		continue;
	    }
	    //<coads_climatology_cdf name="COADS Climatology" url="file:coads_climatology" doc="doc/coads_climatology.html">
	    String id = entry.getId();

	    //for now use the entry id as the tag name
	    String tagName = "data_" +getTagName(id);

	    Element entryNode = XmlUtil.create(tagName, datasetsNode, new String[]{
		    ATTR_NAME, entry.getName(),
		    ATTR_URL,
		    getRepository().absoluteUrl(
						getRepository().URL_ENTRY_SHOW + dataOutputHandler.getOpendapUrl(entry)),
		    ATTR_DOC,
		    getRepository().absoluteUrl(request.url(getRepository().URL_ENTRY_SHOW,
							    ARG_ENTRYID,
							    entry.getId()))
		}); 

	    Element variablesNode = XmlUtil.create(TAG_VARIABLES, entryNode);
	    String path = dataOutputHandler.getPath(entry);
	    NetcdfDataset dataset =
		dataOutputHandler.getNetcdfDataset(entry,
						 path);
	    try {
		//TODO: determine which variables are the actual data variables
		for (Variable var : dataset.getVariables()) {
		    if (var instanceof CoordinateAxis) {
			CoordinateAxis ca       = (CoordinateAxis) var;
			AxisType       axisType = ca.getAxisType();
			//TODO: put in the axes
			if (axisType == null) {
			    continue;
			}
			if (axisType.equals(AxisType.Lat)) {
			} else if (axisType.equals(AxisType.Lon)) {
			} else if (axisType.equals(AxisType.Time)) {
			} else {
			    // System.err.println("unknown axis:" + axisType + " for var:" + var.getName());
			}
			continue;
		    }
		    //<variables>     <airt name="Air Temperature" units="DEG C">

                    String varName = var.getShortName();
                    ucar.nc2.Attribute att =
                        var.findAttribute(NCATTR_STANDARD_NAME);
                    if (att != null) {
                        varName = att.getStringValue();
                    }
		    XmlUtil.create(getTagName(varName), variablesNode,
				   new String[]{
				       ATTR_NAME,
				       var.getName(),
				       ATTR_UNITS,
				       var.getUnitsString()
				   });
		}
	    } finally {
		dataOutputHandler.returnNetcdfDataset(path, dataset);
	    }
	}

	//Create the xml and return the result
        StringBuffer sb = new StringBuffer();
	sb.append(XmlUtil.XML_HEADER);
	sb.append(XmlUtil.toString(root));
	return new Result("dif", sb, "text/xml");
    }

}

