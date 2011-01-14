/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.ui;


import org.w3c.dom.*;

import ucar.unidata.data.DataManager;



import ucar.unidata.idv.*;
import ucar.unidata.idv.ui.IdvUIManager;


import ucar.unidata.ui.ButtonTabbedPane;
import ucar.unidata.ui.ChooserPanel;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.lang.reflect.*;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;







/**
 * Class MyEditorPane shows the html listing
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.8 $
 */
public abstract class QuicklinkPanel extends JEditorPane implements HyperlinkListener {

    /**
     *   A global list of the panels that have been created. We keep this around to do updates
     */
    private static final List editors = new ArrayList();

    /** The idv */
    private IntegratedDataViewer idv;

    /** status label */
    JLabel label = new JLabel(" ");

    /** maps id to some object for the links */
    private Hashtable map = new Hashtable();

    /** am I currently loading */
    private boolean amLoading = false;

    /** The name */
    private String name;

    /** unique counter */
    private int objectCnt = 0;


    /**
     * ctor
     *
     *
     * @param idv the idv
     * @param name name
     */
    public QuicklinkPanel(IntegratedDataViewer idv, String name) {
        this.idv  = idv;
        this.name = name;
        setEditable(false);
        setContentType("text/html");
        addHyperlinkListener(this);
        editors.add(this);
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        Font f = GuiUtils.getDefaultFont();
        if (f != null) {
            this.setFont(f);
        }
    }


    /**
     * Update all of the quicklinks
     */
    public static void updateQuicklinks() {
        updateQuicklinks(editors);
    }

    /**
     * Update all of the quicklink panels in the list
     *
     * @param panels panels
     */
    public static void updateQuicklinks(List panels) {
        for (int i = 0; i < panels.size(); i++) {
            ((QuicklinkPanel) panels.get(i)).doUpdate();
        }
    }


    /**
     * Update the history html display
     */
    public static void updateHistoryLinks() {
        for (int i = 0; i < editors.size(); i++) {
            QuicklinkPanel editor = (QuicklinkPanel) editors.get(i);
            if (editor instanceof FileHistory) {
                editor.doUpdate();
            }
        }
    }


    /**
     * Create the quicklinks panel from the resource definitions
     *
     * @param idv the idv
     *
     * @return the panel
     */
    public static JComponent createQuicklinksFromResources(
            IntegratedDataViewer idv) {
        List myEditors = new ArrayList();

        //        tab = GuiUtils.getNestedTabbedPane(JTabbedPane.LEFT);
        ButtonTabbedPane tab = new ButtonTabbedPane(-1);
        String tabPrefix =
            "<html><body style=\"margin-top:5;margin-bottom:2;\">";
        String tabSuffix = "</body></html>";
        tabPrefix = "";
        tabSuffix = "";


        ResourceCollection rc = idv.getResourceManager().getResources(
                                    idv.getResourceManager().RSC_QUICKLINKS);


        for (int i = 0; i < rc.size(); i++) {
            String resourcePath = (String) rc.get(i);
            if (resourcePath.endsWith(".class")) {
                Component comp = loadClass(resourcePath, idv);
                if (comp != null) {
                    tab.add(comp.toString(), comp);
                }
                continue;
            }

            String contents = (String) rc.read(i);
            if (contents == null) {
                continue;
            }
            String name = StringUtil.findPattern(contents,
                              "<tabtitle>(.*)</tabtitle>");

            if (name == null) {
                name = IOUtil.stripExtension(
                    IOUtil.getFileTail((String) rc.get(i)));
            } else {
                //Not sure why we are getting the tabtitle tags back from the findPattern
                name = StringUtil.stripTags(name);
            }
            myEditors.add(new QuicklinkPanel.Html(idv, name, "", contents));
        }


        if (idv.getProperty("idv.quicklinks.show", true)) {
            if (idv.getProperty("idv.quicklinks.favorites.show", true)) {
                myEditors.add(new QuicklinkPanel.Bundle(idv,
                        "Favorite Bundles",
                        IdvPersistenceManager.BUNDLES_FAVORITES));
            }

            if (idv.getProperty("idv.quicklinks.datasources.show", true)) {
                myEditors.add(new QuicklinkPanel.Bundle(idv,
                        "Data Favorites",
                        IdvPersistenceManager.BUNDLES_DATA));
            }

            if (idv.getProperty("idv.quicklinks.displaytemplates.show",
                                true)) {
                myEditors.add(new QuicklinkPanel.Bundle(idv,
                        "Display Templates",
                        IdvPersistenceManager.BUNDLES_DISPLAY));
            }


            if (idv.getProperty("idv.quicklinks.history.show", true)) {
                myEditors.add(new QuicklinkPanel.FileHistory(idv, "History"));
            }


            if (idv.getProperty("idv.quicklinks.special.show", true)) {
                myEditors.add(new QuicklinkPanel.Control(idv,
                        "Special Displays"));
            }

            if (idv.getProperty("idv.quicklinks.windows.show", true)) {
                myEditors.add(new QuicklinkPanel.Html(idv, "New Window",
                        "Create New Window",
                        idv.getIdvUIManager().getSkinHtml()));
            }
        }


        for (int i = 0; i < myEditors.size(); i++) {
            QuicklinkPanel editor = (QuicklinkPanel) myEditors.get(i);
            tab.addTab(editor.getName(), editor.getContents());
        }
        updateQuicklinks(myEditors);
        if (myEditors.size() > 0) {
            tab.setSelectedIndex(0);
        }


        return tab;
    }



    /**
     * Load a class
     *
     * @param path the class's path
     * @param idv the idv
     *
     * @return The gui component
     */
    private static Component loadClass(String path,
                                       IntegratedDataViewer idv) {
        try {
            path = StringUtil.replace(path, ".class", "");
            Class c = Misc.findClass(path);
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { idv.getClass() });
            Object o;
            if (ctor != null) {
                o = ctor.newInstance(new Object[] { idv });
            } else {
                o = c.newInstance();
            }

            if (o instanceof Component) {
                return (Component) o;
            }
        } catch (Exception exc) {
            LogUtil.logException("Loading class:" + path, exc);
        }
        return null;
    }




    /**
     * get the name
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * get idv
     *
     * @return idv
     */
    public IntegratedDataViewer getIdv() {
        return idv;
    }

    /**
     * Get the gui contents
     *
     * @return gui
     */
    public JComponent getContents() {
        return GuiUtils.centerBottom(getScroller(), label);
    }

    /**
     * log error
     *
     * @param msg message
     * @param exc exception
     */
    public void logException(String msg, Exception exc) {
        LogUtil.logException(msg, exc);
    }

    /**
     * show wait cursor
     */
    public void showWaitCursor() {
        idv.getIdvUIManager().showWaitCursor();
    }

    /**
     * show regular cursor
     */
    public void showNormalCursor() {
        idv.getIdvUIManager().showNormalCursor();
    }


    /**
     * make a scroller
     *
     * @return scroller
     */
    protected JScrollPane getScroller() {
        int width  = 400;
        int height = 400;
        setMinimumSize(new Dimension(width, height));
        setPreferredSize(new Dimension(width, height));
        JScrollPane scroller = GuiUtils.makeScrollPane(this, width, height);
        scroller.setPreferredSize(new Dimension(width, height));
        scroller.setMinimumSize(new Dimension(width, height));
        return scroller;
    }


    /**
     * update the html
     */
    public void doUpdate() {
        map = new Hashtable();
        setText(getHtml());
    }



    /**
     * Register the object
     *
     * @param object object_
     * @param command what to do_
     *
     * @return string to put into html
     */
    protected String registerObject(Object object, String command) {
        String id = "qobject:" + command + ":" + (objectCnt++);
        map.put(id, object);
        return id;
    }



    /**
     * Get the label to be used when mousing over a link
     *
     * @param id link id
     *
     * @return label
     */
    protected String getMouseOverString(String id) {
        return "Load";
    }



    /** _more_          */
    protected Hashtable showMap = new Hashtable();


    /**
     * handle event
     *
     * @param e event
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        final String id = e.getDescription();
        if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.setText(getMouseOverString(id));
        }

        if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            setCursor(Cursor.getDefaultCursor());
            label.setText(" ");
        }

        if (amLoading
                || (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED)) {
            return;
        }
        Misc.run(new Runnable() {
            public void run() {
                amLoading = true;
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                setEnabled(false);
                try {
                    handleHyperLink(id);
                } catch (Exception exc) {
                    logException("Handling link:" + id, exc);
                }
                setCursor(Cursor.getDefaultCursor());
                setEnabled(true);
                amLoading = false;
            }
        });

    }

    /**
     * _more_
     *
     * @param header _more_
     *
     * @return _more_
     */
    protected String getHeader(String header) {
        return "<b>" + header + "</b><hr>";
    }

    /**
     * _more_
     *
     * @param cat _more_
     *
     * @return _more_
     */
    protected boolean isOpen(String cat) {
        cat = cat.replace(">", "&gt;");
        return Misc.equals(showMap.get(cat), "open");

    }


    /**
     * Handle a hyperlinke
     *
     * @param id  the hyperlink ID
     */
    protected void handleHyperLink(String id) {
        if (id.startsWith("toggle:")) {
            String category = id.substring(7);
            category = category.replace(">", "&gt;");
            if (isOpen(category)) {
                showMap.put(category, "closed");
            } else {
                showMap.put(category, "open");
            }
            doUpdate();
            return;
        }


        if (id.startsWith("qobject:")) {
            List tokens = StringUtil.split(id, ":");
            if (tokens.size() != 3) {
                return;
            }
            final Object object  = map.get(id);
            final String command = (String) tokens.get(1);
            label.setText(" ");
            Misc.run(new Runnable() {
                public void run() {
                    objectClicked(command, object);
                    label.setText(" ");
                }
            });
            return;
        }



        if (getIdv().handleAction(id, null)) {
            return;
        }

        if (id.startsWith("jython:")) {
            getIdv().handleAction(id, null);
            return;
        }
        if (id.startsWith("http:")) {
            try {
                String html = IOUtil.readContents(id, getClass());
                //                System.err.println("html:" + html);
                //                System.err.println("id:" + id);
                GuiUtils.showHtmlDialog(html, null);
            } catch (Exception exc) {
                logException("Error loading html:" + id, exc);
            }
            return;
        }


    }

    /**
     * Create the html to display
     *
     * @return the html
     */
    protected abstract String getHtml();

    /**
     * user clicked on something
     *
     * @param command command
     * @param object object
     */
    public abstract void objectClicked(String command, Object object);

    /**
     * Shows one of the three favorites bundle groups
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.8 $
     */
    public static class Bundle extends QuicklinkPanel {

        /** which bundle type */
        int type;

        /** type name */
        String typeName;


        /**
         * ctor
         *
         *
         * @param idv idv
         * @param name name
         * @param type type
         */
        public Bundle(IntegratedDataViewer idv, String name, int type) {
            super(idv, name);
            this.type = type;
            typeName  = name.toLowerCase();
            if (typeName.endsWith("s")) {
                typeName = typeName.substring(0, typeName.length() - 1);
            }
        }


        /**
         * Show the manage dialog
         */
        public void showManageDialog() {
            getIdv().getIdvUIManager().showBundleDialog(type);
        }


        /**
         * Get the contents
         *
         * @return the contents
         */
        public JComponent getContents() {
            List buttons = new ArrayList();

            buttons.add(GuiUtils.makeButton("Manage...", this,
                                            "showManageDialog", null,
                                            "Show the saved bundle manager"));

            JComponent buttonPanel = GuiUtils.hbox(buttons, 5);
            JPanel     top         = null;
            JComponent bottom      = GuiUtils.leftCenter(buttonPanel, label);
            return GuiUtils.topCenterBottom(top, getScroller(), bottom);
        }


        /**
         * get label when mousing over link
         *
         * @param id link id
         *
         * @return label
         */
        protected String getMouseOverString(String id) {
            if (id.equals("manage")) {
                return "Manage the "
                       + getIdv().getPersistenceManager().getBundleTitle(
                           type).toLowerCase();
            }

            if (id.equals("save")) {
                return "Save the current state as a favorite";
            }
            return "Load in " + typeName.toLowerCase();
        }



        /**
         * Create the html
         *
         * @return html
         */
        protected String getHtml() {
            List         cats = new ArrayList();
            Hashtable    ht   = new Hashtable();
            List bundles = getIdv().getPersistenceManager().getBundles(type);
            StringBuffer html = new StringBuffer("<html><body>");
            html.append(
                getHeader(
                    getIdv().getPersistenceManager().getBundleTitle(type)));
            if (bundles.size() == 0) {
                html.append(
                    "<h2> No Saved "
                    + getIdv().getPersistenceManager().getBundleTitle(type)
                    + "</h2>");
            } else {
                //                html.append("<ul style=\"margin-top:0;\">\n");
            }

            int catCnt = 0;
            for (int bundleIdx = 0; bundleIdx < bundles.size(); bundleIdx++) {
                SavedBundle bundle     = (SavedBundle) bundles.get(bundleIdx);
                List        categories = bundle.getCategories();
                String      id         = registerObject(bundle, "bundle");
                if (categories.size() > 0) {
                    String catString = StringUtil.join(" &gt; ", categories);
                    StringBuffer catBuffer = (StringBuffer) ht.get(catString);
                    boolean      open      = isOpen(catString);
                    if (catBuffer == null) {
                        String img = (open
                                      ? "<img src=\"idvresource:/auxdata/ui/icons/CategoryOpen.gif\" border=\"0\">"
                                      : "<img src=\"idvresource:/auxdata/ui/icons/CategoryClosed.gif\" border=\"0\">");
                        catBuffer = new StringBuffer(((catCnt > 0)
                                ? "<br>"
                                : "") + "&nbsp;&nbsp;" + "<a href=\"toggle:"
                                      + catString + "\">" + img + "</a> "
                                      + catString + (open
                                ? "\n<ul style=\"margin-top:0;margin-bottom:0;\">\n"
                                : ""));
                        catCnt++;
                        ht.put(catString, catBuffer);
                        cats.add(catString);
                    }
                    if (open) {
                        catBuffer.append(
                            "<li> <a href=\"" + id + "\"> "
                            + GuiUtils.getLocalName(
                                bundle.toString(), bundle.getLocal(),
                                false) + "</a>\n");
                    }
                } else {
                    html.append("<li> <a href=\"" + id + "\"> " + bundle
                                + "</a>\n");
                }
            }
            for (int i = 0; i < cats.size(); i++) {
                String       catString = (String) cats.get(i);
                StringBuffer sb        = (StringBuffer) ht.get(catString);
                html.append(sb.toString());
                boolean open = isOpen(catString);
                if (open) {
                    html.append("</ul>");
                }
            }

            html.append("</body></html>");
            //            html.append("</ul></body></html>");
            return html.toString();
        }


        /**
         * handle event
         *
         * @param command command
         * @param object the object
         */
        public void objectClicked(String command, Object object) {

            if (command.startsWith("toggle:")) {
                return;
            }

            if (command.equals("manage")) {
                getIdv().getIdvUIManager().showBundleDialog(type);
                return;
            }

            if (command.equals("save")) {
                getIdv().getPersistenceManager().doSaveAsFavorite();
                return;
            }

            if ( !getIdv().getPersistenceManager().open((SavedBundle) object,
                    type == IdvPersistenceManager.BUNDLES_FAVORITES)) {
                return;
            }
            if (type != IdvPersistenceManager.BUNDLES_DISPLAY) {
                getIdv().getIdvUIManager().showDataSelector();
            }
        }


    }




    /**
     * Shows the history
     *
     * @author IDV Development Team
     * @version $Revision: 1.8 $
     */
    public static class FileHistory extends QuicklinkPanel {


        /**
         *  ctor
         *
         *
         * @param idv idv
         * @param name name
         */
        public FileHistory(IntegratedDataViewer idv, String name) {
            super(idv, name);
        }



        /**
         * create the html
         *
         * @return html
         */
        protected String getHtml() {
            StringBuffer html      = new StringBuffer("<html><body>");
            List         histories = getIdv().getHistory();
            if (histories.size() == 0) {
                html.append(getHeader("No History"));
            } else {
                html.append(getHeader("History"));
            }

            StringBuffer dataSourceHtml = new StringBuffer();
            StringBuffer bundleHtml     = new StringBuffer();
            for (int i = 0; i < histories.size(); i++) {



                History history = (History) histories.get(i);
                String  id      = registerObject(history, "history");
                if (history instanceof ucar.unidata.idv.FileHistory) {
                    bundleHtml.append(
                        "<li style=\"margin-top:5;\"> <a href=\"" + id
                        + "\"> " + history.getName() + "</a>\n");
                } else if (history instanceof DataSourceHistory) {
                    dataSourceHtml.append(
                        "<li style=\"margin-top:5;\"> <a href=\"" + id
                        + "\"> " + history.getName() + "</a>\n");
                }
            }

            if (dataSourceHtml.length() > 0) {
                html.append(
                    "<b>Data Sources:</b><ul style=\"margin-top:0;\">");
                html.append(dataSourceHtml.toString());
                html.append("</ul>");
            }
            if (bundleHtml.length() > 0) {
                html.append("<p><b>Bundles:</b><ul style=\"margin-top:0;\">");
                html.append(bundleHtml.toString());
                html.append("</ul>");
            }
            html.append("</body></html>");
            return html.toString();
        }


        /**
         * handle event
         *
         * @param command command
         * @param object object
         */
        public void objectClicked(String command, Object object) {
            History history = (History) object;
            showWaitCursor();
            LogUtil.message("Loading: " + history);
            if (history.process(getIdv())) {
                getIdv().getIdvUIManager().showDataSelector();
            }
            LogUtil.message(" ");
            showNormalCursor();
        }


    }




    /**
     * Shows the history
     *
     * @author IDV Development Team
     * @version $Revision: 1.8 $
     */
    public static class Control extends QuicklinkPanel {

        /**
         *  ctor
         *
         *
         * @param idv idv
         * @param name name
         */
        public Control(IntegratedDataViewer idv, String name) {
            super(idv, name);
        }


        /**
         * create the html
         *
         * @return html
         */
        protected String getHtml() {
            StringBuffer html = new StringBuffer("<html><body>"
                                    + getHeader("Special Displays"));
            List controlDescriptors =
                getIdv().getIdvUIManager().getStandAloneControlDescriptors();
            List      cats   = new ArrayList();
            Hashtable catMap = new Hashtable();
            for (int i = 0; i < controlDescriptors.size(); i++) {
                ControlDescriptor cd =
                    (ControlDescriptor) controlDescriptors.get(i);
                String       cat = cd.getDisplayCategory();
                StringBuffer sb  = (StringBuffer) catMap.get(cat);
                if (sb == null) {
                    sb = new StringBuffer();
                    catMap.put(cat, sb);
                    cats.add(cat);
                }
                String id   = registerObject(cd, "control");
                String name = cd.getLabel().trim();
                sb.append(
                    "<li style=\"margin-top:5;margin-left:0\"> <a href=\""
                    + id + "\"> " + cd.getLabel() + "</a>");
            }

            int colCnt = 0;
            for (int i = 0; i < cats.size(); i++) {
                String       cat = (String) cats.get(i);
                StringBuffer sb  = (StringBuffer) catMap.get(cat);
                if (colCnt == 0) {
                    //                    html.append("<tr valign=\"top\">");
                } else if (colCnt == 2) {
                    //                    html.append("</tr>");
                }
                colCnt++;
                html.append("<b>&nbsp;&nbsp;&nbsp;&nbsp;" + cat + "</b>"
                            + sb);
                //                html.append("<td><b>&nbsp;&nbsp;&nbsp;&nbsp;" + cat + "</b>"
                //                            + sb + "</td><td> &nbsp; &nbsp;&nbsp;</td>");
            }

            html.append("</body></html>");
            //            html.append("</table></body></html>");
            return html.toString();
        }


        /**
         * handle event
         *
         * @param command command
         * @param object object
         */
        public void objectClicked(String command, Object object) {


            getIdv().doMakeControl(new ArrayList(),
                                   (ControlDescriptor) object);

        }


    }



    /**
     * Shows the list of windows
     *
     * @author IDV Development Team
     * @version $Revision: 1.8 $
     */
    public static class Html extends QuicklinkPanel {

        /** the HTML */
        String html;

        /** the string for mouseover */
        String mouseOverString;

        /**
         *  ctor
         *
         *
         * @param idv idv
         * @param name name
         * @param mouseOverString the string for mouseover
         * @param html the html
         */
        public Html(IntegratedDataViewer idv, String name,
                    String mouseOverString, String html) {
            super(idv, name);
            this.html            = html;
            this.mouseOverString = mouseOverString;
        }


        /**
         * create the html
         *
         * @return html
         */
        protected String getHtml() {
            return html;
        }


        /**
         * Handle when an object has been clicked
         *
         * @param command the command
         * @param object  the object
         */
        public void objectClicked(String command, Object object) {
            //noop
        }


        /**
         * Get the text to display on mouseover
         *
         * @param id the id
         *
         * @return the text
         */
        protected String getMouseOverString(String id) {
            return mouseOverString;
        }

    }



}
