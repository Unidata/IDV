/*
 * $Id: SuffixFileFilter.java,v 1.11 2006/05/05 19:19:38 jeffmc Exp $
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



package ucar.unidata.util;


import java.io.File;




import java.util.Hashtable;

import javax.swing.filechooser.*;


/**
 * Class SuffixFileFilter
 *
 *
 * @author IDV development team
 */
public class SuffixFileFilter extends FileFilter {

    /** _more_ */
    Hashtable fileSuffixes;

    /** _more_ */
    String desc;

    /**
     * _more_
     *
     * @param fileSuffixes
     * @param desc
     *
     */
    public SuffixFileFilter(Hashtable fileSuffixes, String desc) {
        this.fileSuffixes = fileSuffixes;
        this.desc         = desc;
    }

    /**
     * _more_
     *
     * @param file
     * @return _more_
     */
    public boolean accept(File file) {
        String name = file.getName();
        String ext  = IOUtil.getFileExtension(name).toLowerCase();
        return file.isDirectory() || (fileSuffixes.get(ext) != null);
    }

    /**
     * _more_
     * @return _more_
     */
    public String getDescription() {
        return desc;
    }

}

