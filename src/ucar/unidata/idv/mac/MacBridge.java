/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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


import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;

import ucar.unidata.idv.IdvManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.GuiUtils;

import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.UIManager;


/**
 * The MacBridge class for OS X specific stuff.
 */
public class MacBridge extends IdvManager implements ApplicationListener {

    /**
     * Create this manager.
     *
     * @param idv The IDV
     */
    public MacBridge(IntegratedDataViewer idv) {
        super(idv);
        init();
    }

    /**
     * Init
     */
    private void init() {
        String icon =
            getIdv().getProperty("idv.ui.logo",
                                 "/ucar/unidata/idv/images/logo.gif");

        Application application = Application.getApplication();
        application.addApplicationListener(this);
        Image logo = GuiUtils.getImage(icon, getClass());

        application.setDockIconImage(logo);
        application.setEnabledPreferencesMenu(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleAbout(ApplicationEvent event) {
        getIdv().getIdvUIManager().about();
        event.setHandled(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleOpenApplication(ApplicationEvent event) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleOpenFile(ApplicationEvent event) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void handlePreferences(ApplicationEvent event) {
        getIdv().showPreferenceManager();
        event.setHandled(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handlePrintFile(ApplicationEvent event) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleQuit(ApplicationEvent event) {
        getIdv().quit();
        event.setHandled(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleReOpenApplication(ApplicationEvent event) {}
}
