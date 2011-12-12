/*
 * $Id: TransectDrawingControl.java,v 1.41 2006/12/28 19:50:59 jeffmc Exp $
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

package ucar.unidata.idv.control.editor;




/**
 * Class Action some class
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Action {

    /** _more_ */
    private String name;

    /** _more_ */
    private String function;


    /** _more_ */
    private String jython;

    /** _more_ */
    private Selector selector;


    /**
     * _more_
     */
    public Action() {}

    /**
     * _more_
     *
     * @param name _more_
     * @param function _more_
     * @param selector _more_
     */
    public Action(String name, String function, Selector selector) {
        this(name, function, null, selector);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param function _more_
     * @param jython _more_
     * @param selector _more_
     */
    public Action(String name, String function, String jython,
                  Selector selector) {
        this.name     = name;
        this.function = function;
        this.jython   = jython;
        this.selector = selector;
    }

    /**
     *  Set the Selector property.
     *
     *  @param value The new value for Selector
     */
    public void setSelector(Selector value) {
        selector = value;
    }

    /**
     *  Get the Selector property.
     *
     *  @return The Selector
     */
    public Selector getSelector() {
        return selector;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        String region = selector.toString();
        return name + " applied to " + region;
    }



    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     *  Set the Function property.
     *
     *  @param value The new value for Function
     */
    public void setFunction(String value) {
        function = value;
    }

    /**
     *  Get the Function property.
     *
     *  @return The Function
     */
    public String getFunction() {
        return function;
    }

    /**
     *  Set the Jython property.
     *
     *  @param value The new value for Jython
     */
    public void setJython(String value) {
        jython = value;
    }

    /**
     *  Get the Jython property.
     *
     *  @return The Jython
     */
    public String getJython() {
        return jython;
    }





}


