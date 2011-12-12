/*
 * $Id: ExampleArgsManager.java,v 1.1 2007/05/29 20:13:19 jeffmc Exp $
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


import ucar.unidata.idv.*;



/**
 * Derive our own ui manager to do some  example specific things.
 */

public class ExampleArgsManager extends ArgsManager {


    /**
     * constructor for the example args manager
     *
     * @param idv the idv
     * @param args the args
     */
    public ExampleArgsManager(IntegratedDataViewer idv, String[] args) {
        super(idv, args);
    }

    /**
     *  Check the argument given by the arg parameter. The idx parameter
     *  points to the next unprocessed entry in the args array. If the argument
     *  requires  one or more values in the args array then increment idx accordingly.
     *  Return idx.
     *
     * @param arg The current argument we are looking at
     * @param args The full args array
     * @param idx The index into args that we are looking at
     * @return The idx of the last value in the args array we look at.
     *         i.e., if the flag arg does not require any further values
     *         in the args array then don't increment idx.  If arg requires
     *         one more value then increment idx by one. etc.
     *
     * @throws Exception When something untoward happens
     */
    protected int parseArg(String arg, String[] args, int idx)
            throws Exception {
        if (arg.equals("-examplearg")) {
            System.err.println("got example arg");
            return idx;
        } else {
            return super.parseArg(arg, args, idx);
        }
    }


    /**
     * get the usage message
     *
     * @return usage message
     */
    protected String getUsageMessage() {
        return msg("-examplearg", "the example argument")
               + super.getUsageMessage();
    }


}

