/*
 * Copyright 1997-2023 Unidata Program Center/University Corporation for
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
package ucar.unidata.idv.control;

import static ucar.unidata.util.CollectionHelpers.list;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.LogUtil;


/**
 * A simple utility class for opening a web browser to a given link.
 */
public final class WebBrowser {

    /** Logging object. */
    private static final Logger logger =
            LoggerFactory.getLogger(ucar.unidata.idv.control.WebBrowser.class);

    /** Probe Unix-like systems for these browsers, in this order. */
    private static final List<String> unixBrowsers =
            list("firefox", "chromium-browser", "google-chrome", "konqueror",
                    "opera", "mozilla", "netscape");

    /**
     * {@code IOException} formatting string used when all browsing methods
     * have failed.
     */
    private static final String ALL_METHODS_ERRMSG = "Could not open '%s'";

    /**
     * {@code IOException} formatting string used by
     * {@link #openOldStyle(String)} when no browsers could be identified on
     * the system.
     */
    private static final String NO_BROWSER_ERRMSG =
            "Could not find a web browser to launch (tried %s)";

    /** Message displayed to the user when all browsing methods have failed. */
    private static final String THINGS_DUN_BROKE_ERRMSG =
            "All three approaches for opening a browser " +
                    "failed!\nPlease consider sending a support request via\n" +
                    "the button below.\n";

    /** Do not create instances of {@code WebBrowser}. */
    private WebBrowser() { }

    /**
     * Attempts to use the system default browser to visit {@code url}. Tries
     * looking for and executing any browser specified by the IDV property
     * {@literal "idv.browser.path"}.
     *
     * <p>If the property wasn't given or there
     * was an error, try the new (as of Java 1.6) way of opening a browser.
     *
     * <p>If the previous attempts failed (or we're in 1.5), we finally try
     * some more primitive measures.
     *
     * <p>Note: if you are trying to use this method with a
     * {@link javax.swing.JTextPane} you may need to turn off editing via
     * {@link javax.swing.JTextPane#setEditable(boolean)}.
     *
     * @param url URL to visit.
     *
     * @see #tryUserSpecifiedBrowser(String)
     * @see #openNewStyle(String)
     * @see #openOldStyle(String)
     */
    public static void browse(final String url) {
        // if the user has taken the trouble to explicitly provide the path to
        // a web browser, we should probably prefer it.
        //if (tryUserSpecifiedBrowser(url)) {
         //   return;
        //}

        // try using the JDK-supported approach
        if (openNewStyle(url)) {
            return;
        }

        // if not, use the hacky stuff.
        try {
            openOldStyle(url);
        } catch (Exception e) {
            logger.warn(String.format(ALL_METHODS_ERRMSG, url), e);
            IOException uhoh =
                    new IOException(String.format(ALL_METHODS_ERRMSG, url));
            LogUtil.logException(THINGS_DUN_BROKE_ERRMSG, uhoh);
        }
    }

    /**
     * Test whether or not a given URL should be opened in a web browser.
     *
     * @param url URL to test. Cannot be {@code null}.
     *
     * @return {@code true} if {@code url} begins with either
     * {@literal "http:"} or {@literal "https:"}.
     */
    public static boolean useBrowserForUrl(final String url) {
        String lowercase = url.toLowerCase();
        return lowercase.startsWith("http:") || lowercase.startsWith("https:");
    }

    /**
     * Use the functionality within {@link java.awt.Desktop} to try opening
     * the user's preferred web browser.
     *
     * @param url URL to visit.
     *
     * @return Either {@code true} if things look ok, {@code false} if there
     * were problems.
     */
    private static boolean openNewStyle(final String url) {
        boolean retVal = false;
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(url));
                    // well... the assumption is that there was not a problem
                    retVal = true;
                } catch (URISyntaxException e) {
                    logger.warn("Bad syntax in URI: "+url, e);
                } catch (IOException e) {
                    logger.warn("Problem accessing URI: "+url, e);
                }
            }
        }
        return retVal;
    }

    /**
     * Uses {@link Runtime#exec(String)} to launch the user's preferred web
     * browser. This method isn't really recommended unless you're stuck with
     * Java 1.5.
     *
     * <p>Note that the browsers need to be somewhere in the PATH, as this
     * method uses the {@code which} command (also needs to be in the PATH!).
     *
     * @param url URL to visit.
     */
    private static void openOldStyle(final String url) {
        try {
            if (isWindows()) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (isMac()) {
                Runtime.getRuntime().exec("/usr/bin/open "+url);
            } else {
                for (String browser : unixBrowsers) {
                    if (Runtime.getRuntime().exec("which "+browser).waitFor() == 0) {
                        Runtime.getRuntime().exec(browser+' '+url);
                        return;
                    }
                }
                String msg = String.format(NO_BROWSER_ERRMSG, unixBrowsers);
                throw new IOException(msg);
            }
        } catch (Exception e) {
            logger.warn("Could not open URL '"+url+'\'', e);
        }
    }

    /**
     * Attempts to launch the browser pointed at by
     * the {@literal "idv.browser.path"} IDV property, if it has been set.
     *
     * @param url URL to open.
     *
     * @return Either {@code true} if the command-line was executed,
     * {@code false} if either the command-line wasn't launched or
     * {@literal "idv.browser.path"} was not set.
     */
    private static boolean tryUserSpecifiedBrowser(final String url) {
        IntegratedDataViewer idv = null; //getIdv();
        boolean retVal = false;
        if (idv != null) {
            String browserPath = idv.getProperty("idv.browser.path", null);
            if ((browserPath != null) && !browserPath.trim().isEmpty()) {
                try {
                    Runtime.getRuntime().exec(browserPath+' '+url);
                    retVal = true;
                } catch (Exception e) {
                    logger.warn("Could not execute '"+browserPath+'\'', e);
                }
            }
        }
        return retVal;
    }

    /**
     * Test for whether or not the current platform is Mac OS X.
     *
     * @return Are we shiny, happy OS X users?
     */
    private static boolean isMac() {
        return System.getProperty("os.name", "").startsWith("Mac OS");
    }

    /**
     * Test for whether or not the current platform is some form of
     * {@literal "unix"} (but not OS X!).
     *
     * @return Do we perhaps think that beards and suspenders are the height
     * of fashion?
     */
    private static boolean isUnix() {
        return !isMac() && !isWindows();
    }

    /**
     * Test for whether or not the current platform is Windows.
     *
     * @return Are we running Windows??
     */
    private static boolean isWindows() {
        return System.getProperty("os.name", "").startsWith("Windows");
    }

    public static void main(String[] args) {
        browse("http://www.rust-lang.org/"); // sassy!
    }
}