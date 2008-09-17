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


import org.w3c.dom.Element;


import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;

import ucar.unidata.data.text.TextProductDataSource;
import ucar.unidata.data.text.Product;
import ucar.unidata.data.text.ProductType;
import ucar.unidata.data.text.ProductGroup;

import ucar.unidata.geoloc.Bearing;

import ucar.unidata.gis.SpatialGrid;
import ucar.unidata.gis.WorldWindReader;


import ucar.unidata.idv.DisplayConventions;


import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.ui.symbol.*;
import ucar.unidata.ui.TextSearcher;

import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.display.*;

import ucar.visad.display.CompositeDisplayable;
import ucar.visad.display.StationLocationDisplayable;
import ucar.visad.display.StationModelDisplayable;


import visad.*;


import visad.georef.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;
import visad.georef.NamedLocation;
import visad.georef.NamedLocationTuple;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;


import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.border.*;




/**
 * Class to display a set of locations
 *
 * @author MetApps Development Team
 * @version $Revision: 1.3 $ $Date: 2006/12/01 20:16:39 $
 */


public class TextProductControl extends StationLocationControl {

    /** _more_          */
    private TextProductDataSource dataSource;

    /** _more_          */
    private List<ProductGroup> productGroups;

    JTree productTree;


    private boolean ignoreTimeChanges = false;

    /** _more_          */
    private ProductGroup productGroup;

    /** _more_          */
    private ProductType productType;

    private List<Product> products;

    /** _more_          */
    private JTextArea textArea;

    /** _more_          */
    private JLabel stationLabel;

    /** _more_          */
    private NamedStationTable stationTable;

    /**
     * Default cstr;
     */
    public TextProductControl() {}


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
        JTabbedPane tabs = doMakeTabs(false, false);

        setCenterOnClick(false);
        //        setDeclutter(false);

        textArea        = new JTextArea("", 30, 60);
        TextSearcher textSearcher = new TextSearcher(textArea);


        DefaultMutableTreeNode treeRoot  = new DefaultMutableTreeNode("Product Groups");
        for(ProductGroup productGroup: productGroups) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(productGroup);
            treeRoot.add(groupNode);
            for(ProductType productType: productGroup.getProductTypes()) {
                DefaultMutableTreeNode typeNode = new DefaultMutableTreeNode(productType);
                groupNode.add(typeNode);
            }
        }

        DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);
        productTree = new JTree(treeModel);
        productTree.setRootVisible(false);
        productTree.setShowsRootHandles(true);
        productTree.addTreeSelectionListener(new TreeSelectionListener() {
                public void 	valueChanged(TreeSelectionEvent e) {
                    productType = getSelectedProductType();
                    updateText();
                }
            }); 



        JScrollPane treeScroller =  GuiUtils.makeScrollPane(productTree, 200,100);
        JComponent treeComp = GuiUtils.topCenter(new JLabel("Products:"), treeScroller);

        stationLabel = new JLabel("LBL");
        JComponent topComp = GuiUtils.leftRight(GuiUtils.bottom(stationLabel), getAnimationWidget().getContents());
        JScrollPane textScroller  = new JScrollPane(textArea);
        textScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        GuiUtils.tmpInsets = GuiUtils.INSETS_2;
        JComponent contents = GuiUtils.doLayout(new Component[]{
                GuiUtils.bottom(new JLabel("Products")),
                topComp,
                treeScroller,
                GuiUtils.centerBottom(textScroller,textSearcher)},
            2,
            new double[]{0.25,0.75},
            GuiUtils.WT_NY);




        updateText();
        tabs.insertTab("Products", null, contents, "", 0);
        tabs.setSelectedIndex(0);
        return tabs;
    }



    /** _more_          */
    private NamedStationTable currentTable;

    /** _more_          */
    private List stationList = new ArrayList();


    private ProductType getSelectedProductType() {
        TreePath[] paths = productTree.getSelectionModel().getSelectionPaths();
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
            DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) last;
            Object userObject = dmtn.getUserObject();
            if(userObject instanceof ProductType) {
                return (ProductType) userObject;
            }
        }
        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected List getStationList() {
        return stationList;
    }


    /** _more_          */
    NamedStationImpl selectedStation;

    /**
     * _more_
     *
     * @param selectionList _more_
     */
    protected void selectedStationsChanged(List selectionList) {
        NamedStationImpl newStation=null;
        if (selectionList.size() == 0) {
            selectedStation = null;
        } else {
            newStation= (NamedStationImpl) selectionList.get(0);
        }
        if(Misc.equals(newStation, selectedStation)) {
            updateStationLabel();
            return;
        }
        selectedStation = newStation;
        updateStationLabel();
        updateText();
    }

    private  void updateStationLabel() {
        if (selectedStation != null) {
            String state= (String)selectedStation.getProperty("ST","");
            stationLabel.setText(selectedStation.getName()+" " + state);
        } else {
            stationLabel.setText(" ");
        }
        
    }

    /**
     * _more_
     */
    public void updateText() {
        try {
            String            text     = "";
            NamedStationTable newTable = dataSource.getStations(productType);
            if (newTable != currentTable) {
                if (newTable != null) {
                    stationList = new ArrayList(newTable.values());
                } else {
                    stationList = new ArrayList();
                }
                if(selectedStation!=null && !stationList.contains(selectedStation)) {
                    if(productType!=null) {
                        selectedStation = null;
                        updateStationLabel();
                    }
                }
                loadData();
                currentTable = newTable;
            }

            if (productType != null && selectedStation!=null) {
                products =  dataSource.readProducts(productType,selectedStation);
            } else {
                products =  new ArrayList<Product>();
            }
            products = (List<Product>)Misc.sort(products);

            List dateTimes = new ArrayList();
            for(Product product: products) {
                if(product.getDate()!=null) {
                    dateTimes.add(new DateTime(product.getDate()));
                }
            }


            ignoreTimeChanges = true;
            if(dateTimes.size()>0) {
                getAnimationWidget().setBaseTimes(ucar.visad.Util.makeTimeSet(dateTimes));
                getAnimationWidget().gotoEnd();
            } else {
                getAnimationWidget().setBaseTimes(null);
            }
            ignoreTimeChanges = false;

            if(products.size()==0)  {
                setText("No products found");
            } else {
                setText(products.get(products.size()-1).getContent());
            }
        } catch(Exception exc) {
            logException("Error updating product text", exc);
        }
    }



    protected void setText(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if(productType==null) {
                        textArea.setText("Please select a product");
                    } else if(selectedStation==null){
                        textArea.setText("Please select a station");
                    } else {
                        textArea.setText(text);
                    }
                    textArea.setCaretPosition(0);
                    textArea.scrollRectToVisible(new Rectangle(0,0,1,1));
                }
            });

    }

    protected void timeChanged(Real time) {
        try {
            if(ignoreTimeChanges) return;
            int idx = getAnimation().getCurrent();
            if(idx>=0 && idx<products.size()) {
                setText(products.get(idx).getContent());
            } else {
                setText("");
            }
        } catch(Exception exc) {
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


}

