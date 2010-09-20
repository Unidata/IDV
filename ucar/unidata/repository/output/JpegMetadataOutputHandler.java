/*
 * Copyright 2010 ramadda.org
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

package ucar.unidata.repository.output;


import org.w3c.dom.*;

import ucar.unidata.repository.*;


import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;

import java.awt.Image;
import java.awt.image.*;


import java.io.*;

import java.io.File;


import java.net.*;
import java.util.List;
import java.util.Iterator;



import com.drew.metadata.*;
import com.drew.metadata.exif.*;
import com.drew.imaging.jpeg.*;
import com.drew.lang.*;



/**
 *
 *
 * @author RAMADDA Development Team
 */
public class JpegMetadataOutputHandler extends OutputHandler {


    /** _more_ */
    public static final OutputType OUTPUT_JPEG_METADATA =
        new OutputType("JPEG Metadata", "jpeg.metadata",
                       OutputType.TYPE_HTML | OutputType.TYPE_FORSEARCH, "",
                       ICON_IMAGES);

    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public JpegMetadataOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_JPEG_METADATA);
    }


    /**
     * _more_
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
        if (state.entry != null) {
            String path = state.entry.getResource().getPath();
            if(!(path.toLowerCase().endsWith(".jpg") || path.toLowerCase().endsWith(".jpeg"))) return;
            links.add(makeLink(request, state.getEntry(), OUTPUT_JPEG_METADATA));
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param outputType _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, OutputType outputType,
                              Entry entry)
            throws Exception {
        StringBuffer sb          = new StringBuffer();
        File jpegFile = new File(entry.getResource().getPath()); 
        com.drew.metadata.Metadata metadata = JpegMetadataReader.readMetadata(jpegFile);

        sb.append("<ul>");
        Iterator directories = metadata.getDirectoryIterator(); 
        while (directories.hasNext()) { 
            Directory directory = (Directory)directories.next(); 
            sb.append("<li> ");
            sb.append(directory.getName());
            sb.append("<ul>");
            Iterator tags = directory.getTagIterator(); 
            while (tags.hasNext()) { 
                Tag tag = (Tag)tags.next(); 
                if(tag.getTagName().indexOf("Unknown")>=0) continue;
                sb.append("<li> ");
                sb.append(tag.getTagName());            
                sb.append(":");
                sb.append(tag.getDescription());            
            }
            sb.append("</ul>");
        }
        sb.append("</ul>");
        return new Result("JPEG Metadata", sb);
    }

}
