/*
 * $Id: HtmlPanel.java,v 1.6 2007/07/06 20:45:30 jeffmc Exp $
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


import java.awt.*;
import java.awt.event.*;

import java.io.IOException;

import java.net.MalformedURLException;

import java.net.URL;

import java.util.Vector;



import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;


/**
 * View Html files.
 * @version 1.5 12/17/97
 * @author Jeff Dinkins
 * @author Tim Prinzing
 * @author Don Murray modified for McGUI
 * @author John Caron modified for jmet
 */
public class HtmlPanel extends JPanel implements HyperlinkListener {

    /** _more_ */
    JEditorPane html;

    /** _more_ */
    static JLabel urlLabel;

    /** _more_ */
    Vector urlList = new Vector();

    /** _more_ */
    int urlListIndex;

    /** _more_ */
    JDialog parent;

    /**
     * _more_
     *
     * @param parental
     * @param url
     *
     */
    public HtmlPanel(JDialog parental, URL url) {
        this.parent = parental;

        // setBackground(Color.white);
        setLayout(new BorderLayout());
        JPanel p = new JPanel();
        add(p, BorderLayout.NORTH);

        // Add some back/forward buttons
        AbstractButton backB = BAMutil.makeButtcon(BAMutil.getIcon("Left",
                                   true), null, "Previous Page", false);
        backB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showPreviousURL();
            }
        });
        p.add(backB);

        // add a button to display the next URL
        AbstractButton foreB = BAMutil.makeButtcon(BAMutil.getIcon("Right",
                                   true), null, "Next Page", false);
        foreB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showNextURL();
            }
        });
        p.add(foreB);

        // add a close button
        AbstractButton exitB = BAMutil.makeButtcon(BAMutil.getIcon("Exit",
                                   true), null, "Close Window", false);
        exitB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parent.setVisible(false);
            }
        });
        p.add(exitB);

        // add a label indicating the URL
        urlLabel = new JLabel();
        add(urlLabel, BorderLayout.SOUTH);

        try {
            html = new JEditorPane(url);
            html.setEditable(false);
            html.addHyperlinkListener(this);
            JScrollPane scroller = new JScrollPane();
            JViewport   vp       = scroller.getViewport();
            vp.add(html);
            vp.setBackingStoreEnabled(true);
            add(scroller, BorderLayout.CENTER);
            setLabel(url.toString());
            updateUrlList(url);
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + e);
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }

    }

    /**
     * Notification of a change relative to a
     * hyperlink.
     *
     * @param e
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            linkActivated(e.getURL());
        }
    }

    /**
     * Follows the reference in an
     * link.  The given url is the requested reference.
     * By default this calls <a href="#setPage">setPage</a>,
     * and if an exception is thrown the original previous
     * document is restored and a beep sounded.  If an
     * attempt was made to follow a link, but it represented
     * a malformed url, this method will be called with a
     * null argument.
     *
     * @param u the URL to follow
     */
    protected void linkActivated(URL u) {
        Cursor c          = html.getCursor();
        Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        html.setCursor(waitCursor);
        SwingUtilities.invokeLater(new PageLoader(u, c));
    }

    /**
     * temporary class that loads synchronously (although
     * later than the request so that a cursor change
     * can be done).
     */
    class PageLoader implements Runnable {

        /**
         * _more_
         *
         * @param u
         * @param c
         *
         */
        PageLoader(URL u, Cursor c) {
            url    = u;
            cursor = c;
        }

        /**
         * _more_
         */
        public void run() {
            if (url == null) {
                // restore the original cursor
                html.setCursor(cursor);

                // PENDING(prinz) remove this hack when 
                // automatic validation is activated.
                Container parent = html.getParent();
                parent.repaint();
            } else {
                Document doc = html.getDocument();
                try {
                    html.setPage(url);
                } catch (IOException ioe) {
                    html.setDocument(doc);
                    getToolkit().beep();
                } finally {
                    // schedule the cursor to revert after
                    // the paint has happended.
                    setLabel(html.getPage().toString());
                    updateUrlList(url);
                    url = null;
                    SwingUtilities.invokeLater(this);
                }
            }
        }

        /** _more_ */
        URL url;

        /** _more_ */
        Cursor cursor;
    }

    /**
     *  Display the previous URL
     */
    public void showPreviousURL() {
        urlListIndex--;
        if (urlListIndex < 0) {
            urlListIndex = 0;
        }
        linkActivated((URL) urlList.elementAt(urlListIndex));
    }

    /**
     *  Display the next URL
     */
    public void showNextURL() {
        urlListIndex++;
        if (urlListIndex >= urlList.size()) {
            urlListIndex = urlList.size() - 1;
        }
        linkActivated((URL) urlList.elementAt(urlListIndex));
    }

    /**
     *  Update vector of URLs
     *
     * @param u
     */
    public void updateUrlList(URL u) {
        if (urlList.indexOf(u) == -1) {
            urlListIndex++;
            if (urlListIndex >= urlList.size()) {
                urlList.addElement(u);
            } else {
                urlList.setElementAt(u, urlListIndex);
                urlList.setSize(urlListIndex + 1);
            }
        }
    }

    /**
     *  Update the text on the label
     *
     * @param label
     */
    public static void setLabel(String label) {
        urlLabel.setText("Document: " + label);
    }

}

