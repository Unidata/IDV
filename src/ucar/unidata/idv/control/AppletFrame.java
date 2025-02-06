/*
 * Copyright 1997-2024 Unidata Program Center/University Corporation for
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


import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.sound.sampled.Clip;
import javax.swing.*;




public class AppletFrame extends JPanel{

    /** the applet for the control */
    JPanel theApplet;

    /** base directory for the applet */
    private File baseDir;

    /** table of applet parameters */
    private Hashtable params;

    /**
     * Construct an AppletFrame.
     *
     * @param a              the applet
     * @param w              width of the frame
     * @param h              height of the frame
     * @param baseDirPath    base directory path
     * @param params         applet parameters
     *
     */
    public AppletFrame(JPanel a, int w, int h, String baseDirPath,
                       Hashtable params) {
        theApplet   = a;
        this.params = params;
        if (baseDirPath != null) {
            this.baseDir = new File(baseDirPath);
        }
        params = null;
        setSize(w, h);
        setPreferredSize(new Dimension(w,h));
        setLayout(new BorderLayout());
        add("Center", a);
        a.setSize(w,h);
        a.setPreferredSize(new Dimension(w,h));
        a.setVisible(true);
    }



    /**
     * Get the base directory
     */
    private void getBase() {
        if (baseDir == null) {
            File f = new File(new File(".").getAbsolutePath());
            baseDir = new File(f.getParent());
        }
    }

    // AppletStub methods

    /**
     * Resize the applet to the width and height
     *
     * @param width    new width
     * @param height   new height
     */
    public void appletResize(int width, int height) {
        setSize(width, height);
        validate();
    }


    /**
     * Get the code base
     * @return code base
     */
    public URL getCodeBase() {
        getBase();
        try {
            return new URL("file:" + baseDir);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Get the document base
     * @return  document base
     */
    public URL getDocumentBase() {
        getBase();
        try {
            return new URL("file:" + baseDir + "/foo.html");
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Get a parameter from the list of parameters
     *
     * @param name   name of the parameter
     * @return  get the parameters for this name
     */
    public String getParameter(String name) {
        if (params == null) {
            return null;
        }
        return (String) params.get(name);
    }


    // AppletContext methods

    /**
     * Get the image at the url
     *
     * @param url  location of image
     * @return  the image
     */
    public Image getImage(URL url) {
        String base = getCodeBase().toString();
        String name = url.toString();
        if (name.startsWith(base)) {
            name = name.substring(base.length());
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
        }

        return Toolkit.getDefaultToolkit().getImage(name);
    }

    /**
     * Show the status of this applet
     *
     * @param status  applet status
     */
    public void showStatus(String status) {
        System.err.println(status);
    }

    // AppletContext stubs

    /**
     * Get the applet of said name
     *
     * @param name  name of the applet
     * @return null
     */
    public JPanel getApplet(String name) {
        return null;
    }

    /**
     * Get an enumeration of the applets.
     * @return null in this implementation
     */
    public Enumeration getApplets() {
        return null;
    }

    /**
     * Get the audio clip at the location specified.
     *
     * @param url   location of clip
     * @return  the clip
     */
    public Clip getAudioClip(URL url) {
        return null;
    }

    /**
     * Show the document at the location specified
     *
     * @param url  document location
     */
    public void showDocument(URL url) {}

    /**
     * Show the document at the url with the target
     *
     * @param url     document URL
     * @param target  target
     */
    public void showDocument(URL url, String target) {}

    /**
     * Get the InputStream for the given key
     *
     * @param key   key for stream
     * @return null
     */
    public InputStream getStream(String key) {
        return null;
    }

    /**
     * Get the keys for a given stream
     * @return null
     */
    public Iterator getStreamKeys() {
        return null;
    }

    /**
     * Set the input stream for the given key
     *
     * @param key    key to use
     * @param stream stream for key
     */
    public void setStream(String key, InputStream stream) {}

}
