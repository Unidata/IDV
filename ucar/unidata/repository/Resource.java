/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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


import ucar.unidata.ui.ImageUtils;

import java.io.File;

import java.net.URL;



/**
 * Class Entry _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Resource {

    /** _more_ */
    public static final String TYPE_FILE = "file";

    public static final String TYPE_LOCALFILE = "localfile";

    /** _more_ */
    public static final String TYPE_URL = "url";

    /** _more_ */
    public static final String TYPE_UNKNOWN = "unknown";

    /** _more_ */
    private String path;

    /** _more_ */
    private String type = TYPE_UNKNOWN;

    /** _more_ */
    private File file;

    /**
     * _more_
     */
    public Resource() {
        path = "";
    }


    /**
     * _more_
     *
     * @param file _more_
     */
    public Resource(File file) {
        this(file, TYPE_FILE);
    }

    public Resource(File file, String type) {
        this.file = file;
        this.path      = file.toString();
        this.type      = type;
    }


    /**
     * _more_
     *
     * @param path _more_
     */
    public Resource(String path) {
        this.path = path;
        if (new File(path).exists()) {
            type = TYPE_FILE;
        } else {
            try {
                new URL(path);
                type = TYPE_URL;
            } catch (Exception exc) {}
        }
    }


    /**
     * _more_
     *
     * @param path _more_
     * @param type _more_
     */
    public Resource(String path, String type) {
        this.path = path;
        this.type = type;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isImage() {
        return ImageUtils.isImage(path);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public File getFile() {
        if (file == null) {
            file = new File(path);
        }
        return file;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isFile() {
        if (type.equals(TYPE_FILE) || type.equals(TYPE_LOCALFILE)) {
            return getFile().exists();
        }
        return false;
    }

    public boolean isLocalFile() {
        return type.equals(TYPE_LOCALFILE);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isUrl() {
        return type.equals(TYPE_URL);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return path;
    }

    /**
     * Set the Path property.
     *
     * @param value The new value for Path
     */
    public void setPath(String value) {
        path = value;
    }

    /**
     * Get the Path property.
     *
     * @return The Path
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public String getType() {
        return type;
    }


}

