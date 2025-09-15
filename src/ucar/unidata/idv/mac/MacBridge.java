/*
 * Copyright 1997-2025 Unidata Program Center/University Corporation for
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

import java.awt.Desktop;
import java.awt.Image;
import java.awt.Taskbar;

import ucar.unidata.idv.IdvManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.GuiUtils;


/**
 * The MacBridge class for OS X specific stuff.
 */
public class MacBridge extends IdvManager {

    /**
     * Create this manager.
     *
     * @param idv The IDV
     */
    public MacBridge(IntegratedDataViewer idv) {
        super(idv);
        init();
    }


    // =================================================================================
    // Modern Approach (Java 9+) using java.awt.Desktop and java.awt.Taskbar
    // =================================================================================

    private void init() {
        // --- Part 1: Handle Desktop events (About, Prefs, Quit) ---
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();

            if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                desktop.setAboutHandler(e -> doHandleAbout());
            }

            if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
                desktop.setPreferencesHandler(e -> doHandlePreferences());
            }

            if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                desktop.setQuitHandler((e, response) -> doHandleQuit());
            }
        } else {
            System.err.println("MacBridge: java.awt.Desktop is not supported.");
        }

        // --- Part 2: Handle Taskbar features (Dock Icon) ---
        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();

            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                String iconPath = getIdv().getProperty("idv.ui.logo", "/ucar/unidata/idv/images/logo.gif");
                Image image = (Image) GuiUtils.getImage(iconPath, getClass());
                taskbar.setIconImage(image);
            }
        } else {
            System.err.println("MacBridge: java.awt.Taskbar is not supported.");
        }
    }

    // =================================================================================
    // Centralized Handler Logic (called by both modern and legacy paths)
    // =================================================================================

    private void doHandleAbout() {
        getIdv().getIdvUIManager().about();
    }

    private void doHandlePreferences() {
        getIdv().showPreferenceManager();
    }

    private void doHandleQuit() {
        getIdv().quit();
    }

}