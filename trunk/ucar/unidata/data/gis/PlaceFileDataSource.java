/*
 * $Id: FrontDataSource.java,v 1.15 2007/04/17 22:22:52 jeffmc Exp $
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



package ucar.unidata.data.gis;

import ucar.unidata.data.*;

import visad.Text;
import visad.Data;
import visad.VisADException;
import java.awt.Color;
import java.awt.Font;
import java.rmi.RemoteException;


import ucar.unidata.idv.control.DrawingControl;
import ucar.unidata.idv.control.drawing.DrawingGlyph;
import ucar.unidata.idv.control.drawing.TextGlyph;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;

import org.w3c.dom.*;


/**
 * This reads placefile and generates a xgrf file
 * Placefile syntax is defined here: http://www.grlevelx.com/downloads/places.txt
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */
public class PlaceFileDataSource extends FilesDataSource {

    /**
     * Default bean constructor; does nothing.
     *
     */
    public PlaceFileDataSource() {}

    /**
     * Create a new FrontDataSource
     *
     * @param descriptor    descriptor for this DataSource
     * @param filename      name of the file (or URL)
     * @param properties    extra data source properties
     */
    public PlaceFileDataSource(DataSourceDescriptor descriptor, String filename,
                           Hashtable properties) {
        this(descriptor, Misc.newList(filename), properties);
    }


    /**
     * Create a new FrontDataSource
     *
     * @param descriptor    Descriptor for this DataSource
     * @param files         List of files or urls
     * @param properties    Extra data source properties
     */
    public PlaceFileDataSource(DataSourceDescriptor descriptor, List files,
                           Hashtable properties) {
        super(descriptor, files, (String) files.get(0), "Place files data source",
              properties);
    }

    /**
     * Make the data choices associated with this source
     */
    protected void doMakeDataChoices() {
        String category = "xgrf";
        String docName  = getName();
        addDataChoice(
            new DirectDataChoice(
                this, docName, docName, docName,
                DataCategory.parseCategories(category, false)));
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
     * @return The visad.Data object
     *
     * @throws RemoteException    Java RMI problem
     * p     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        try {
            String xml = processContents();
            if (xml == null) {
                return null;
            }
            return new visad.Text(xml);
        } catch (Exception exc) {
            logException("Could not process front file contents: " + sources,
                         exc);
        }
        return null;
    }



    private String processContents() throws Exception {
        Document doc = XmlUtil.makeDocument();
        Element root = doc.createElement("shapes");
        Element editorNode = XmlUtil.create(DrawingControl.TAG_EDITOR, root);
        editorNode.setAttribute("editable", "false");
        List    contentsList = new ArrayList();
        boolean anyOk        = false;
        for (int i = 0; i < sources.size(); i++) {
            String filename = (String) sources.get(i);
            LogUtil.message("Reading place file: " + filename);
            String contents =  IOUtil.readContents(filename);
            processContents(contents,root,editorNode);
        }
        String xgrf  =  XmlUtil.toString(root);
        System.err.println (xgrf);
        return xgrf;
    }

    private static final String SHAPE_TITLE = "Title";
    private static final String SHAPE_LINE = "Line";
    private static final String SHAPE_PLACE = "Place";
    private static final String SHAPE_END = "End";
    private static final String SHAPE_TEXT = "Text";
    private static final String SHAPE_COLOR = "Color";
    private static final String SHAPE_FONT = "Font";
    private static final String SHAPE_OBJECT = "Object";

    private void processContents(String contents,Element root,Element editorNode) throws Exception {
        Color color = Color.magenta;
        Font font = GuiUtils.buttonFont;
        List lines = StringUtil.split(contents,"\n",true,true);
        Hashtable fonts = new Hashtable();
        for(int lineIdx=0;lineIdx<lines.size();lineIdx++) {
            String  line = (String)lines.get(lineIdx);
            if(line.length()==0) continue;
            if(line.startsWith(";")) continue;
            String[]toks = StringUtil.split(line,":",2);
            if(toks==null || toks.length!=2) continue;
            if(toks[0].equals(SHAPE_COLOR)) {
                color = GuiUtils.decodeColor(toks[1],color);
            } else if(toks[0].equals(SHAPE_FONT)) {
                String[]subtoks = StringUtil.split(toks[1],",",3);
                if(subtoks!=null) {
                    int fontStyle = 0;
                    int flags =  new Integer(toks[2]).intValue();
                    if((flags&1)!=0) fontStyle|=Font.BOLD;
                    if((flags&2)!=0) fontStyle|=Font.ITALIC;
                    Font f =new Font(toks[3], fontStyle, new Integer(toks[1]).intValue());
                    fonts.put(toks[0],f);
                }
            } else if(toks[0].equals(SHAPE_TITLE)) {
                editorNode.setAttribute("title", toks[1]);
            } else if(toks[0].equals(SHAPE_PLACE)) {
                String[]subtoks = StringUtil.split(toks[1],",",3);
                if(subtoks!=null) {
                    Element child = create(DrawingGlyph.TAG_TEXT, root,color);
                    child.setAttribute(DrawingGlyph.ATTR_POINTS,subtoks[0]+","+subtoks[1]);
                    child.setAttribute(DrawingGlyph.ATTR_TEXT,subtoks[2]);
                }
            } else if(toks[0].equals(SHAPE_TEXT)) {
                List<String> subtoks = (List<String>)StringUtil.split(toks[1],",",true,true);
                Element child = create(DrawingGlyph.TAG_TEXT, root,color);
                child.setAttribute(DrawingGlyph.ATTR_POINTS,subtoks.get(0)+","+subtoks.get(1));
                child.setAttribute(DrawingGlyph.ATTR_TEXT,subtoks.get(2));
            } else if(toks[0].equals(SHAPE_LINE)) {
                List<String> subtoks = (List<String>)StringUtil.split(toks[1],",",true,true);
                Element child = create(DrawingGlyph.TAG_POLYGON, root,color);
                child.setAttribute(DrawingGlyph.ATTR_LINEWIDTH,subtoks.get(0));
                if(subtoks.size()>=3) {
                    child.setAttribute(DrawingGlyph.ATTR_NAME,(String)subtoks.get(2));
                }
                StringBuffer points = null;
                lineIdx++;
                for(;lineIdx<lines.size();lineIdx++) {
                    line = (String)lines.get(lineIdx);
                    if(line.startsWith(";")) continue;
                    if(line.length()==0) continue;
                    if(line.equals(SHAPE_END+":")) break;
                    if(points == null) {
                        points = new StringBuffer();
                    } else {
                        points.append(",");
                    }
                    points.append(line);
                }
                if(points!=null) {
                    child.setAttribute(DrawingGlyph.ATTR_POINTS,points.toString());

                }
            } else {
                System.err.println ("Unknown  placefile tag:" + toks[0]);
            }
        }
    }


    private void setFont(Element node, Hashtable fonts, String id) {
        Font f = (Font) fonts.get(id);
        if(f!=null) {
            node.setAttribute(TextGlyph.ATTR_FONTFACE,f.getFamily());
            node.setAttribute(TextGlyph.ATTR_FONTSIZE,""+f.getSize());
            
        }
    }

    private Element create(String tag, Element root, Color color) throws Exception {
        Element child = XmlUtil.create(tag, root);
        XmlUtil.setAttribute(child,DrawingGlyph.ATTR_COLOR,color);
        child.setAttribute(DrawingGlyph.ATTR_COORDTYPE,"LATLON");
        return child;
    }


    /**
     * test main
     *
     * @param args args
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 20; i++) {
            //            convertFilename("foo%DAY-" + i + "%");
        }
    }
}

