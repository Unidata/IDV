/*
 * $Id: StateManager.java,v 1.85 2007/08/17 10:50:39 jeffmc Exp $
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

package ucar.unidata.idv.mac;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.idv.*;
import com.apple.eawt.*;

import java.awt.*;

public class MacBridge extends IdvManager implements ApplicationListener  {

    /**
     * Create this manager
     *
     * @param idv The IDV
     *
     */
    public MacBridge(IntegratedDataViewer idv) {
        super(idv);
	init();
    }



    private void init() {
	Application application = Application.getApplication();
	application.addApplicationListener(this);
	Image logo  = 
	    GuiUtils.getImage(getIdv().getProperty("idv.ui.logo",
						   "/ucar/unidata/idv/images/logo.gif"),getClass());

	application.setDockIconImage(logo);
	application.setEnabledPreferencesMenu(true);
    }

    
    public void handleAbout(ApplicationEvent event) {
	getIdv().getIdvUIManager().about ();
	event.setHandled(true);
    }
    public void handleOpenApplication(ApplicationEvent event) { }
    public void handleOpenFile(ApplicationEvent event) { }
    public void handlePreferences(ApplicationEvent event) {
	getIdv().showPreferenceManager();
	event.setHandled(true);
    }
    public void handlePrintFile(ApplicationEvent event) { }

    public void handleQuit(ApplicationEvent event) {
	getIdv().quit();
	event.setHandled(true);
    }

    public void handleReOpenApplication(ApplicationEvent event) { } 


}


