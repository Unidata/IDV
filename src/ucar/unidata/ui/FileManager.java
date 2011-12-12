/*
 * $Id: FileManager.java,v 1.59 2007/07/06 20:45:30 jeffmc Exp $
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

package ucar.unidata.ui;




import ucar.unidata.util.PatternFileFilter;

import java.awt.*;

import java.util.ArrayList;
import java.util.List;



import java.util.Vector;

import javax.swing.*;
import javax.swing.filechooser.*;


/**
 * Wrapper cover for JFileChooser.
 * @deprecated Use ucar.unidata.util.FileManager
 * @author Unidata development staff
 * @version $Id: FileManager.java,v 1.59 2007/07/06 20:45:30 jeffmc Exp $
 */
public class FileManager extends ucar.unidata.util.FileManager {

    /**
     * Create a FileManager and use <code>parent</code> as the
     * parent for the dialog.
     * @param parent  parent component for the dialog.
     */
    public FileManager(Component parent) {
        super(parent);
    }

    /**
     * Create a FileManager and use the specified params to configure its
     * behavior.
     * @param parent  parent component for the dialog.
     * @param defDir  default directory to open up
     * @param file_extension  file_extention to use for a filter
     * @param desc  description of files of type <code>file_extension</code>
     */
    public FileManager(Component parent, String defDir,
                       String file_extension, String desc) {
        super(parent, defDir, file_extension, desc);
    }

    /**
     * Create a FileManager and use the specified params to configure its
     * behavior.
     * @param parent  parent component for the dialog.
     * @param defDir  default directory to open up
     * @param filter  default <code>FileFilter</code>
     */
    public FileManager(Component parent, String defDir, FileFilter filter) {
        super(parent, defDir, filter);
    }

    /**
     * Create a FileManager and use the specified params to configure its
     * behavior.
     * @param parent  parent component for the dialog.
     * @param defDir  default directory to open up
     * @param filter  default <code>FileFilter</code>
     * @param title  title for the dialog window
     */
    public FileManager(Component parent, String defDir, FileFilter filter,
                       String title) {
        super(parent, defDir, filter, title);
    }



    /**
     * Create a FileManager and use the specified params to configure its
     * behavior.
     * @param parent  parent component for the dialog.
     * @param defDir  default directory to open up
     * @param filters  <code>List</code> of default <code>FileFilter</code>'s
     */
    public FileManager(Component parent, String defDir, List filters) {
        super(parent, defDir, filters);
    }


    /**
     * Create a FileManager and use the specified params to configure its
     * behavior.
     * @param parent  parent component for the dialog.
     * @param defDir  default directory to open up
     * @param filters  <code>List</code> of default <code>FileFilter</code>'s
     * @param title  title for the dialog window
     */
    public FileManager(Component parent, String defDir, List filters,
                       String title) {
        super(parent, defDir, filters, title);
    }


    /**
     * Create a FileManager and use the specified params to configure its
     * behavior.
     * @param parent  parent component for the dialog.
     * @param defDir  default directory to open up
     * @param filters  <code>List</code> of default <code>FileFilter</code>'s
     * @param title  title for the dialog window
     * @param includeAllFilter  true to include the "All files" filter.
     */
    public FileManager(Component parent, String defDir, List filters,
                       String title, boolean includeAllFilter) {
        super(parent, defDir, filters, title, includeAllFilter);
    }

}

