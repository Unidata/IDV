/*
 * $Id: YahooLocationControl.java,v 1.3 2006/12/01 20:16:39 jeffmc Exp $
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




package ucar.unidata.idv.control;



import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;

import ucar.unidata.data.text.Product;
import ucar.unidata.data.text.ProductGroup;
import ucar.unidata.data.text.ProductType;
import ucar.unidata.data.text.TextProductDataSource;

import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.ui.TextSearcher;

import ucar.unidata.util.DateSelection;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import visad.*;

import java.awt.*;
import java.awt.event.*;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;




/**
 * Class to display a set of locations
 *
 * @author MetApps Development Team
 * @version $Revision: 1.3 $ $Date: 2006/12/01 20:16:39 $
 */


public class TextProductControl extends StationLocationControl implements HyperlinkListener {


    /** hours */
    private int hours = -1;

    /** date selection combo */
    private JComboBox dateSelectionCbx;


    /** show glossary checkbox */
    private JCheckBox showGlossaryCbx;

    /** flag for showing glossary */
    private boolean showGlossary = false;

    /** current text */
    private String currentText = "";

    /** data srouce */
    private TextProductDataSource dataSource;

    /** list of product groups */
    private List<ProductGroup> productGroups;

    /** product tree */
    JTree productTree;

    /** ignore time changes flag */
    private boolean ignoreTimeChanges = false;

    /** selected product group */
    private ProductGroup productGroup;

    /** selected product type */
    private ProductType productType;

    /** list of products */
    private List<Product> products;

    /** text component */
    private JTextComponent textComp;

    /** html editor */
    private JEditorPane htmlComp;

    /** station label */
    private JLabel stationLabel;

    /** current table */
    private NamedStationTable stationTable;

    /** selected station */
    private NamedStationImpl selectedStation;

    /** selected station id (for persistence) */
    private String selectedStationId;

    /** the patthen */
    private Pattern allPattern = null;

    /** current table */
    private NamedStationTable currentTable;

    /** station list */
    private List stationList = new ArrayList();

    /**
     * Default cstr;
     */
    public TextProductControl() {}


    /**
     * Handle a hyperlink update
     *
     * @param e  Hyperlink event.
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        URL    tmp = e.getURL();
        String url = ((tmp != null)
                      ? tmp.toString()
                      : e.getDescription());
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                String content =
                    IOUtil.readContents(
                        "http://www.crh.noaa.gov/glossary.php?word=" + url,
                        getClass());
                int idx = content.indexOf("<html");
                if (idx >= 0) {
                    content = content.substring(idx);
                }
                idx = content.indexOf("<hr");
                if (idx >= 0) {
                    content = content.substring(0, idx);
                }
                content = content.replaceAll("<!--.*-->", "");
                content = content.replaceAll("<dt>", "");
                content = content.replaceAll("</dt><dd>", "<br>");
                JEditorPane pane = new JEditorPane();
                pane.setEditable(false);
                pane.setContentType("text/html");
                pane.setText(content);
                pane.setPreferredSize(new Dimension(250, 150));
                JLabel lbl = new JLabel(content);
                GuiUtils.showOkDialog(null, "Definition:" + url, pane, null);

            } catch (Exception exc) {
                logException("Could not fetch definition", exc);
            }
        } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            //            System.err.println ("entered:" + url);
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            //            System.err.println ("exited:" + url);
        }
    }



    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {

        if (productGroups == null) {
            return new JLabel("Could not load product data");
        }
        JTabbedPane tabs = doMakeTabs(false, false);

        setCenterOnClick(false);
        //        setDeclutter(false);

        htmlComp = new JEditorPane();
        htmlComp.addHyperlinkListener(this);
        htmlComp.setEditable(false);
        htmlComp.setContentType("text/html");
        textComp = new JTextArea("", 30, 60);
        textComp.setEditable(false);
        TextSearcher textSearcher = new TextSearcher(textComp);


        DefaultMutableTreeNode treeRoot =
            new DefaultMutableTreeNode("Product Groups");
        DefaultMutableTreeNode selectedNode = null;
        for (ProductGroup productGroup : productGroups) {
            DefaultMutableTreeNode groupNode =
                new DefaultMutableTreeNode(productGroup);
            treeRoot.add(groupNode);
            for (ProductType type : productGroup.getProductTypes()) {
                DefaultMutableTreeNode typeNode =
                    new DefaultMutableTreeNode(type);
                groupNode.add(typeNode);
                if (Misc.equals(type, productType)) {
                    selectedNode = typeNode;
                }
            }
        }

        DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);
        productTree = new JTree(treeModel);
        productTree.setRootVisible(false);
        productTree.setShowsRootHandles(true);
        productTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                productType = getSelectedProductType();
                updateText();
            }
        });

        if (selectedNode != null) {
            TreeNode[] path = treeModel.getPathToRoot(selectedNode);
            productTree.setSelectionPath(new TreePath(path));
            productTree.expandPath(new TreePath(path));
        }


        Object[] dateSelectionItems = new Object[] {
            new TwoFacedObject("Latest Product", -1),
            new TwoFacedObject("Last 3 Hours", 3),
            new TwoFacedObject("Last 6 Hours", 6),
            new TwoFacedObject("Last 12 Hours", 12),
            new TwoFacedObject("Last 24 Hours", 24),
            new TwoFacedObject("Last 36 Hours", 36),
            new TwoFacedObject("Last 48 Hours", 48),
            new TwoFacedObject("All", 0)
        };
        TwoFacedObject selectedTfo =
            TwoFacedObject.findId(new Integer(hours),
                                  Misc.toList(dateSelectionItems));
        dateSelectionCbx = new JComboBox(dateSelectionItems);
        if (selectedTfo != null) {
            dateSelectionCbx.setSelectedItem(selectedTfo);
        }
        dateSelectionCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                updateText();
            }
        });

        JScrollPane treeScroller = GuiUtils.makeScrollPane(productTree, 200,
                                       100);
        JComponent treeComp = GuiUtils.centerBottom(
                                  treeScroller,
                                  GuiUtils.inset(
                                      GuiUtils.vbox(
                                          new JLabel("Date Range:"),
                                          dateSelectionCbx), 5));

        stationLabel = new JLabel(" ");
        JComponent topComp =
            GuiUtils.leftRight(GuiUtils.bottom(stationLabel),
                               getAnimationWidget().getContents());
        JScrollPane textScroller = new JScrollPane(textComp);
        JScrollPane htmlScroller = new JScrollPane(htmlComp);
        textScroller.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        htmlScroller.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JComponent textHolder = GuiUtils.centerBottom(textScroller,
                                    textSearcher);
        JTabbedPane textTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);

        showGlossaryCbx = new JCheckBox("Show Glossary", showGlossary);
        showGlossaryCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setText(currentText);
            }
        });
        textTabbedPane.addTab("HTML",
                              GuiUtils.centerBottom(htmlScroller,
                                  GuiUtils.right(showGlossaryCbx)));
        textTabbedPane.addTab("Text", textHolder);
        GuiUtils.tmpInsets = GuiUtils.INSETS_2;
        JComponent contents = GuiUtils.doLayout(new Component[] {
                                  GuiUtils.bottom(new JLabel("Products")),
                                  topComp, treeComp, textTabbedPane }, 2,
                                      new double[] { 0.25,
                0.75 }, GuiUtils.WT_NY);

        updateText();
        tabs.insertTab("Products", null, contents, "", 0);
        tabs.setSelectedIndex(0);
        return tabs;

    }




    /**
     * Get the selected product type
     *
     * @return  the type or null
     */
    private ProductType getSelectedProductType() {
        TreePath[] paths =
            productTree.getSelectionModel().getSelectionPaths();
        if (paths == null) {
            return null;
        }
        for (int i = 0; i < paths.length; i++) {
            Object last = paths[i].getLastPathComponent();
            if (last == null) {
                continue;
            }
            if ( !(last instanceof DefaultMutableTreeNode)) {
                continue;
            }
            DefaultMutableTreeNode dmtn       = (DefaultMutableTreeNode) last;
            Object                 userObject = dmtn.getUserObject();
            if (userObject instanceof ProductType) {
                return (ProductType) userObject;
            }
        }
        return null;
    }


    /**
     * Get the station list
     *
     * @return  the list
     */
    protected List getStationList() {
        return stationList;
    }


    /**
     * Handle a change to the selected stations
     *
     * @param selectionList  list of stations
     */
    protected void selectedStationsChanged(List selectionList) {
        NamedStationImpl newStation = null;
        if (selectionList.size() == 0) {
            selectedStation = null;
        } else {
            newStation = (NamedStationImpl) selectionList.get(0);
        }
        if (Misc.equals(newStation, selectedStation)) {
            updateStationLabel();
            return;
        }
        selectedStation = newStation;
        updateStationLabel();
        updateText();
    }

    /**
     * Update the station label
     */
    private void updateStationLabel() {
        if (stationLabel != null) {
            NamedStationImpl station = selectedStation;
            if (station != null) {
                String state =
                    (String) station.getProperty(NamedStationTable.KEY_STATE,
                        "");
                stationLabel.setText(station.getName() + (state.equals("")
                        ? " "
                        : ", ") + state);
            } else {
                stationLabel.setText(" ");
            }
        }

    }

    /**
     * Update the text.
     */
    public void updateText() {
        Misc.run(new Runnable() {
            public void run() {
                showWaitCursor();
                try {
                    setText("Loading...");
                    updateTextInner();
                } catch (Exception exc) {
                    setText("Error:" + exc);
                } finally {
                    showNormalCursor();
                }
            }
        });
    }

    /**
     * Add selected to list
     *
     * @param listOfStations list of stations
     */
    protected void addSelectedToList(List listOfStations) {
        //NOOP
    }

    /**
     * Update the text for real
     */
    private void updateTextInner() {
        try {
            NamedStationTable newTable = dataSource.getStations(productType,
                                             getDateSelection());
            if (newTable != currentTable) {
                if (newTable != null) {
                    stationList = new ArrayList(newTable.values());
                } else {
                    stationList = new ArrayList();
                }
                // in case we are unpersisting
                if (selectedStationId != null) {
                    for (NamedStationImpl station : (List<NamedStationImpl>) stationList) {
                        if (selectedStationId.equals(station.getID())) {
                            selectedStation = station;
                            break;
                        }
                    }
                    selectedStationId = null;
                    updateStationLabel();
                }


                if (selectedStation != null) {
                    if ( !stationList.contains(selectedStation)) {
                        if (productType != null) {
                            selectedStation = null;
                            updateStationLabel();
                        }
                    } else {
                        selectedStation = (NamedStationImpl) stationList.get(
                            stationList.indexOf(selectedStation));
                    }
                }
                loadData();
                currentTable = newTable;
            }

            if ((productType != null) && (selectedStation != null)) {
                products = dataSource.readProducts(productType,
                        selectedStation, getDateSelection());
            } else {
                products = new ArrayList<Product>();
            }
            products = (List<Product>) Misc.sort(products);

            List dateTimes = new ArrayList();
            for (Product product : products) {
                if (product.getDate() != null) {
                    dateTimes.add(new DateTime(product.getDate()));
                }
            }


            ignoreTimeChanges = true;
            if (dateTimes.size() > 0) {
                getAnimationWidget().setBaseTimes(
                    ucar.visad.Util.makeTimeSet(dateTimes));
                getAnimationWidget().gotoEnd();
            } else {
                getAnimationWidget().setBaseTimes(null);
            }
            ignoreTimeChanges = false;

            if (products.size() == 0) {
                setText("No products found");
            } else {
                setText(products.get(products.size() - 1).getContent());
            }
        } catch (Exception exc) {
            logException("Error updating product text", exc);
        }
    }


    /**
     * Get the date selection
     *
     * @return the DateSelection (or null)
     */
    private DateSelection getDateSelection() {
        long hours = (long) getHours();
        if (hours == 0) {
            return null;
        }
        int count = Integer.MAX_VALUE;
        if (hours == -1) {
            count = 1;
            hours = 24 * 365 * 20;
        }

        Date now = new Date(System.currentTimeMillis());
        //        Date now = DateUtil.roundByDay(new Date(System.currentTimeMillis()), 1);
        Date then = new Date(System.currentTimeMillis()
                             - DateUtil.hoursToMillis(hours));
        DateSelection dateSelection = new DateSelection(then, now);
        dateSelection.setCount(count);
        return dateSelection;
    }

    /**
     * Convert the text to HTML
     *
     * @param text  the text
     *
     * @return the text converted to HTML
     */
    private String convertToHtml(String text) {
        if (allPattern == null) {
            try {
                List<String> tmp =
                    (List<String>) StringUtil.split(
                        IOUtil.readContents(
                            "/ucar/unidata/idv/control/nwsglossary.txt",
                            getClass()), "\n", true, true);
                StringBuffer pattern = new StringBuffer("([ ]+)(");
                for (String word : tmp) {
                    if (word.length() <= 3) {
                        continue;
                    }
                    word = word.toUpperCase();
                    word = word.replace("(", "\\(");
                    word = word.replace(")", "\\)");
                    word = word.replace("+", "\\+");
                    word = word.replace(".", "\\.");
                    word = word.replace("*", "\\*");
                    pattern.append(word);
                    pattern.append("|");
                }
                pattern.append(")([\\. ]+)");
                allPattern = Pattern.compile(pattern.toString());
            } catch (Exception exc) {
                logException("Reading glossary", exc);
            }
        }
        if (selectedStation != null) {
            text = text.replace(" " + selectedStation.getID() + " ",
                                " <b>" + selectedStation.getID() + "</b> ");
        }

        text = text.replaceAll("\\.\\.\\.\n++", "...\n");
        text = text.trim();
        StringBuffer sb = new StringBuffer();
        List<String> lines = (List<String>) StringUtil.split(text, "\n",
                                 false, false);
        int lineCnt = 0;
        for (String line : lines) {
            lineCnt++;
            line = line.trim();
            if (line.startsWith(".")) {
                int idx = line.indexOf("...");
                if (idx > 1) {
                    String header = line.substring(1, idx);
                    line = "<div style=\"background-color:#c3d9ff; font-weight: bold; padding-left:2px; padding-top:2px;margin-top:7px;\">"
                           + header + "</div>" + line.substring(idx + 3);
                } else if (line.equals(".")) {
                    line = "\n";
                }
            } else if (line.equals("=")) {
                line = "<hr>";
            } else if (line.equals("$$")) {
                continue;
            } else {
                line = line.replaceAll("^([0-9]+ (AM|PM).*[0-9]+)$",
                                       "<i>$1</i>");
            }
            //            if(lineCnt<5) line = line+"<br>";
            sb.append(line);
            sb.append("\n");
        }
        text = sb.toString();
        String[] icons = { "partlycloudy.png", "cloudy.png",
                           "partlysunny.png", "sunny.png", "rainy.png" };
        String[] patterns = { "PARTLY CLOUDY", "MOSTLY CLOUDY",
                              "PARTLY SUNNY", "SUNNY", "RAIN SHOWERS" };
        for (int i = 0; i < icons.length; i++) {
            text = text.replace(
                patterns[i],
                "PATTERN" + i
                + "<img src=idvresource:/ucar/unidata/idv/control/images/"
                + icons[i] + ">");
        }
        for (int i = 0; i < icons.length; i++) {
            text = text.replace("PATTERN" + i, patterns[i]);
        }


        if (showGlossaryCbx.isSelected()) {
            text = allPattern.matcher(text).replaceAll(
                "$1<a href=\"$2\">$2</a>$3");
        }
        text = text.replace("\n\n", "<p>");
        text = text.replace("\n", "<br>");
        return "<html>" + text + "</html>";
    }

    /**
     * Set the text
     *
     * @param theText the text
     */
    protected void setText(final String theText) {
        currentText = theText;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String html = "";
                String text = "";
                if (productType == null) {
                    html = text = "Please select a product";
                } else if (selectedStation == null) {
                    html = text = "Please select a station";
                } else {
                    text = theText;
                    long t1 = System.currentTimeMillis();
                    html = convertToHtml(theText);
                    long t2 = System.currentTimeMillis();
                    //                        System.err.println ("Time:" + (t2-t1));

                }
                textComp.setText(text);
                htmlComp.setText(html);
                textComp.setCaretPosition(0);
                textComp.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
                htmlComp.setCaretPosition(0);
                htmlComp.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
            }
        });

    }

    /**
     * Handle the time changed
     *
     * @param time  the time to set
     */
    protected void timeChanged(Real time) {
        try {
            if (ignoreTimeChanges) {
                return;
            }
            int idx = getAnimation().getCurrent();
            if ((idx >= 0) && (idx < products.size())) {
                setText(products.get(idx).getContent());
            } else {
                setText("");
            }
        } catch (Exception exc) {
            logException("Error setting time", exc);
        }
    }



    /**
     * @param dataChoice    the DataChoice of the moment -
     *
     * @return  true if successful
     *
     * @throws  VisADException  there was a VisAD error
     * @throws  RemoteException  there was a remote error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        List dataSources = new ArrayList();
        dataChoice.getDataSources(dataSources);

        if (dataSources.size() != 1) {
            userMessage("Could not find Text Product  Data Source");
            return false;
        }

        if ( !(dataSources.get(0) instanceof TextProductDataSource)) {
            userMessage("Could not find Text Product  Data Source");
            return false;
        }

        dataSource    = (TextProductDataSource) dataSources.get(0);
        productGroups = dataSource.getProductGroups();
        if (productGroup != null) {
            int idx = productGroups.indexOf(productGroup);
            if (idx >= 0) {
                productGroup = productGroups.get(idx);
            } else {
                productGroup = null;
            }
        }
        return super.init(dataChoice);
    }



    /**
     *  Set the ProductGroup property.
     *
     *  @param value The new value for ProductGroup
     */
    public void setProductGroup(ProductGroup value) {
        productGroup = value;
    }

    /**
     *  Get the ProductGroup property.
     *
     *  @return The ProductGroup
     */
    public ProductGroup getProductGroup() {
        return productGroup;
    }

    /**
     *  Set the Product property.
     *
     *  @param value The new value for Product
     */
    public void setProductType(ProductType value) {
        productType = value;
    }

    /**
     *  Get the Product property.
     *
     *  @return The Product
     */
    public ProductType getProductType() {
        return productType;
    }

    /**
     * Set the SelectedStationId property.
     *
     * @param value The new value for SelectedStationId
     */
    public void setSelectedStationId(String value) {
        selectedStationId = value;
    }

    /**
     * Get the SelectedStationId property.
     *
     * @return The SelectedStationId
     */
    public String getSelectedStationId() {
        if (selectedStation != null) {
            return selectedStation.getID();
        }
        return null;
    }


    /**
     * Set the Hours property.
     *
     * @param value The new value for Hours
     */
    public void setHours(int value) {
        hours = value;
    }

    /**
     * Get the Hours property.
     *
     * @return The Hours
     */
    public int getHours() {
        if (dateSelectionCbx != null) {
            TwoFacedObject tfo =
                (TwoFacedObject) dateSelectionCbx.getSelectedItem();
            if (tfo != null) {
                hours = new Integer(tfo.getId().toString()).intValue();
            }
        }
        return hours;
    }


    /**
     * Set the ShowGlossary property.
     *
     * @param value The new value for ShowGlossary
     */
    public void setShowGlossary(boolean value) {
        showGlossary = value;
    }

    /**
     * Get the ShowGlossary property.
     *
     * @return The ShowGlossary
     */
    public boolean getShowGlossary() {
        if (showGlossaryCbx != null) {
            showGlossary = showGlossaryCbx.isSelected();
        }
        return showGlossary;
    }


}

