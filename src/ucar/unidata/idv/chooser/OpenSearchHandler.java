/*
* Copyright 2008-2011 Jeff McWhirter/ramadda.org
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this 
* software and associated documentation files (the "Software"), to deal in the Software 
* without restriction, including without limitation the rights to use, copy, modify, 
* merge, publish, distribute, sublicense, and/or sell copies of the Software, and to 
* permit persons to whom the Software is furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in all copies 
* or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
* INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
* PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
* FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
* OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
* DEALINGS IN THE SOFTWARE.
*/

package ucar.unidata.idv.chooser;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSource;

import ucar.unidata.idv.*;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlNodeList;


import ucar.unidata.xml.XmlUtil;

import java.awt.*;

import java.awt.event.*;

import java.io.File;




import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;


/**
 * This handles  opensearch description documents in the
 * {@link XmlChooser}.
 *
 * @author Jeff McWhirter
 */
public class OpenSearchHandler extends XmlHandler {

    /** xml items          */
    public static final String XMLNS_XMLNS =
        "http://a9.com/-/spec/opensearch/1.1/";

    /** xml items          */
    public static final String XMLNS_XMLNS_GEO =
        "http://a9.com/-/opensearch/extensions/geo/1.0/";

    /** xml items          */
    public static final String XMLNS_XMLNS_TIME =
        "http://a9.com/-/opensearch/extensions/time/1.0/";

    /** xml items          */
    public static final String TAG_OPENSEARCHDESCRIPTION =
        "OpenSearchDescription";

    /** xml items          */
    public static final String TAG_SHORTNAME = "ShortName";

    /** xml items          */
    public static final String TAG_DESCRIPTION = "Description";

    /** xml items          */
    public static final String TAG_CONTACT = "Contact";

    /** xml items          */
    public static final String TAG_SYNDICATIONRIGHT = "SyndicationRight";

    /** xml items          */
    public static final String TAG_ADULTCONTENT = "AdultContent";

    /** xml items          */
    public static final String TAG_LANGUAGE = "Language";

    /** xml items          */
    public static final String TAG_OUTPUTENCODING = "OutputEncoding";

    /** xml items          */
    public static final String TAG_INPUTENCODING = "InputEncoding";

    /** xml items          */
    public static final String TAG_IMAGE = "Image";

    /** xml items          */
    public static final String TAG_URL = "Url";

    /** xml items          */
    public static final String ATTR_TEMPLATE = "template";

    /** xml items          */
    public static final String ATTR_TYPE = "type";


    /**
     * Create the handler
     *
     * @param chooser The chooser we are in
     * @param root The root of the xml tree
     * @param path The url path of the xml document
     *
     */
    public OpenSearchHandler(XmlChooser chooser, Element root, String path) {
        super(chooser, root, path);
    }

    /**
     * Update the status
     */
    protected void updateStatus() {
        if (chooser.getHaveData()) {
            chooser.setStatus("Press \"" + chooser.CMD_LOAD
                              + "\" to load the selected data", "buttons");
        } else {
            chooser.setStatus("Please select a dataset from the catalog");
        }
    }


    /**
     * Create the  UI
     *
     *  @return The UI component
     */
    protected JComponent doMakeContents() {

        return new JLabel("Open Search");


        /*
          JComponent ui =
          GuiUtils.inset(GuiUtils.topCenterBottom(GuiUtils.left(dsComp),
          tree.getScroller(), GuiUtils.right(showThumbsCbx)), 5);

        */
    }



    /**
     *  The user  has pressed the 'Load' button. Check if a  node is selected
     */
    public void doLoad() {}




}
