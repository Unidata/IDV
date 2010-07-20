/*
 * Copyright 2010 UNAVCO, 6350 Nautilus Drive, Boulder, CO 80301
 * http://www.unavco.org
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
 * 
 */

package ucar.unidata.repository;


import ucar.unidata.repository.*;


/**
 * An example of a page decorator. Change the above package to your package structure.
 * Compile this class and make a jar file, e.g.,:
 * jar -cvf testdecorator.jar TestPageDecorator.class
 *
 * Put the jar file in the ramadda plugins directory, e.g.:
 * ~/.unidata/repository/plugins
 *
 * Now when ramadda runs decorate page will be called with the page html from the templates.
 * Update the html as needed and return it.
 *
 * @author Jeff McWhirter
 */
public class TestPageDecorator implements PageDecorator {

    /**
     * ctor
     */
    public TestPageDecorator() {}

    /**
     * Decorate the html
     *
     * @param repository the repository
     * @param request the request
     * @param html The html page template
     * @param entry This is the last entry the user has seen. Note: this may be null.
     *
     * @return The html
     */
    public String decoratePage(Repository repository, Request request,
                               String html,  Entry entry) {
        Entry secondToTopMostEntry = null;
        if(entry!=null) {
            secondToTopMostEntry = repository.getEntryManager(). getSecondToTopEntry(entry);
        }
        if(secondToTopMostEntry !=null) {
            //Use this to change the template
        }
        //Just add on XXXXXX so we cna see this working
        return html + "XXXXXX";
    }

}
