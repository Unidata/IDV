/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 * 
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.grid.gempak;


import ucar.unidata.util.IOUtil;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Class to hold a lookup of gempak parameters
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GempakParameterTable {

    /** table to hold the  values */
    private HashMap paramMap = new HashMap(256);

    /** indices of breakpoints in the table */
    private static int[] indices = {
        0, 4, 37, 58, 71, 88, 96
    };

    /** lengths */
    private static int[] lengths = {
        4, 33, 21, 13, 16, 12, 1
    };

    /**
     * Create a new table.
     */
    public GempakParameterTable() {}

    /*
    ID# NAME                             UNITS                GNAM         SCALE   MISSING  HZREMAP DIRECTION
    */

    /**
     * Add parameters from the table
     *
     * @param tbl   table location
     *
     * @throws IOException   problem reading table.
     */
    public void addParameters(String tbl) throws IOException {
        String content = IOUtil.readContents(tbl, GempakParameterTable.class);
        List   lines   = StringUtil.split(content, "\n", false);
        List   result  = new ArrayList();
        for (int i = 0; i < lines.size(); i++) {
            String line  = (String) lines.get(i);
            String tline = line.trim();
            if (tline.length() == 0) {
                continue;
            }
            if (tline.startsWith("!")) {
                continue;
            }
            String[] words = new String[indices.length];
            for (int idx = 0; idx < indices.length; idx++) {
                if (indices[idx] >= tline.length()) {
                    continue;
                }
                if (indices[idx] + lengths[idx] > tline.length()) {
                    words[idx] = line.substring(indices[idx]);
                } else {
                    words[idx] = line.substring(indices[idx],
                            indices[idx] + lengths[idx]);
                }
                //if (trimWords) {
                words[idx] = words[idx].trim();
                //}
            }
            result.add(words);
        }
        for (int i = 0; i < result.size(); i++) {
            GridParameter p = makeParameter((String[]) result.get(i));
            if (p != null) {
                paramMap.put(p.getName(), p);
            }
        }

    }

    /**
     * Make a parameter from the tokens
     *
     * @param words   the tokens
     *
     * @return  a grid parameter (may be null)
     */
    private GridParameter makeParameter(String[] words) {
        int    num = 0;
        String description;
        if (words[0] != null) {
            num = (int) Misc.parseDouble(words[0]);
        }
        if ((words[3] == null) || words[3].equals("")) {  // no param name
            return null;
        }
        if ((words[1] == null) || words[1].equals("")) {
            description = words[3];
        } else {
            description = words[1];
        }
        String unit = words[2];
        if (unit != null) {
            unit = unit.replaceAll("\\*\\*", "\\^");
            if (unit.equals("-")) {
                unit = "";
            }
        }
        return new GridParameter(num, words[3], description, unit);
    }

    /**
     * Get the parameter for the given name
     *
     * @param name   name of the parameter (eg:, TMPK);
     *
     * @return  corresponding parameter or null if not found in table
     */
    public GridParameter getParameter(String name) {
        return (GridParameter) paramMap.get(name);
    }

    /**
     * Test
     *
     * @param args ignored
     *
     * @throws IOException  problem reading the table.
     */
    public static void main(String[] args) throws IOException {
        GempakParameterTable pt = new GempakParameterTable();
        pt.addParameters("wmogrib3.tbl");
    }
}

