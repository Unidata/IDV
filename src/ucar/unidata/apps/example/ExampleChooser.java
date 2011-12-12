/*
 * $Id: ExampleChooser.java,v 1.3 2006/03/16 23:29:32 jeffmc Exp $
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

package ucar.unidata.apps.example;


import ucar.unidata.idv.*;
import ucar.unidata.idv.ui.*;
import ucar.unidata.data.*;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.idv.ui.*;


import visad.VisADException;

import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import org.w3c.dom.Element;




/**
 * Class ExampleChooser _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ExampleChooser extends JPanel {



    /** _more_ */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String TAG_DATA = "data";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    IntegratedDataViewer idv;

    /** _more_ */
    JList typeList;

    /** _more_ */
    JList dataList;

    /** _more_ */
    JList fieldList;

    /**
     * _more_
     *
     * @param idv _more_
     */
    public ExampleChooser(IntegratedDataViewer idv) {
        this.idv = idv;
        init();
    }


    /**
     * _more_
     */
    private void init() {
        setLayout(new BorderLayout());
        typeList  = new JList();
        dataList  = new JList();
        fieldList = new JList();

        JScrollPane typeScroller = GuiUtils.makeScrollPane(typeList, 300,
                                       200);
        JScrollPane dataScroller = GuiUtils.makeScrollPane(dataList, 300,
                                       200);
        JScrollPane fieldScroller = GuiUtils.makeScrollPane(fieldList, 300,
                                        200);
        JPanel typePanel = GuiUtils.topCenter(GuiUtils.cLabel("Type"),
                                              typeScroller);
        JPanel dataPanel = GuiUtils.topCenter(GuiUtils.cLabel("Data"),
                                              dataScroller);

        JPanel fieldPanel = GuiUtils.topCenter(GuiUtils.cLabel("Field"),
                                               fieldScroller);

        JPanel listPanel =
            GuiUtils.hgrid(Misc.newList(typePanel, dataPanel, fieldPanel), 4);
        this.add(listPanel, BorderLayout.CENTER);

        XmlResourceCollection catalogs =
            idv.getResourceManager().getXmlResources(
                ExampleIdv.RSC_EXAMPLECATALOGS);
        Vector typeEntries = new Vector();
        for (int resourceIdx = 0; resourceIdx < catalogs.size();
                resourceIdx++) {
            Element root = catalogs.getRoot(resourceIdx);
            if (root == null) {
                continue;
            }
            //XmlUtil has a variety of facilities  for accessing xml
            //e.g.: find all child
            List typeElements = XmlUtil.getElements(root, TAG_TYPE);
            for (int typeIdx = 0; typeIdx < typeElements.size(); typeIdx++) {
                Element typeElement = (Element) typeElements.get(typeIdx);
                //The getAttribute method throws an exception if the attr is not found
                String name = XmlUtil.getAttribute(typeElement, ATTR_NAME);
                //You can also call:
                //String name = XmlUtil.getAttribute(typeElement, ATTR_NAME, defaultValue);
                //Which will return the defaultValue if the attr is not found


                //The TwoFacedObject holds a String label and an Object id
                //It is used  so the name is displayed in the JList but
                //you can still access the Element from it
                TwoFacedObject tfo = new TwoFacedObject(name, typeElement);
                typeEntries.add(tfo);
            }

        }
        typeList.setListData(typeEntries);

        typeList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                TwoFacedObject tfo =
                    (TwoFacedObject) typeList.getSelectedValue();
                fillDataList((Element) tfo.getId());
            }
        });


    }


    /**
     * _more_
     *
     * @param typeNode _more_
     */
    private void fillDataList(Element typeNode) {
        Vector dataEntries  = new Vector();
        List   dataElements = XmlUtil.getElements(typeNode, TAG_DATA);
        for (int dataIdx = 0; dataIdx < dataElements.size(); dataIdx++) {
            Element        dataElement = (Element) dataElements.get(dataIdx);
            String name = XmlUtil.getAttribute(dataElement, ATTR_NAME);
            TwoFacedObject tfo = new TwoFacedObject(name, dataElement);
            dataEntries.add(tfo);
        }
        dataList.setListData(dataEntries);
    }



    /**
     * _more_
     */
    protected void dataSelected() {
        String example = "/home/jeffmc/test.nc";
        DataSourceResults results = idv.createDataSource(example, null, null,
                                        false);

        //This shows any errors
        idv.getIdvUIManager().showResults(results);
        if ( !results.anyOk()) {
            return;
        }
        DataSource dataSource = (DataSource) results.getDataSources().get(0);
        List       choices    = dataSource.getDataChoices();
        for (int i = 0; i < choices.size(); i++) {
            DataChoice dataChoice = (DataChoice) choices.get(i);
            //dataChoice.toString();
        }

    }


    /**
     * _more_
     *
     * @param dataChoice _more_
     */
    protected void fieldSelected(DataChoice dataChoice) {
        List l = ControlDescriptor.getApplicableControlDescriptors(
                     dataChoice.getCategories(), idv.getControlDescriptors());
        for (int i = 0; i < l.size(); i++) {
            ControlDescriptor dd = (ControlDescriptor) l.get(i);
            dd.getLabel();
        }

    }


    /**
     * _more_
     *
     * @param isLeft _more_
     * @param dataChoice _more_
     * @param descriptor _more_
     *
     * @return _more_
     */
    public DisplayControl createDisplay(boolean isLeft,
                                        DataChoice dataChoice,
                                        ControlDescriptor descriptor) {
        //Find the left ViewManager or the right ViewManager
        //And configure something to have the new display control us it.
        IdvWindow   window         = (IdvWindow) IdvWindow.findWindow(this);
        if(window!=null) {
            List        viewManagers   = window.getViewManagers();
            ViewManager theViewManager = (ViewManager) (isLeft
                                                        ? viewManagers.get(0)
                                                        : viewManagers.get(1));
            //Now,  make the display
            String properties = "makeWindow=false;" + "defaultView="
                + theViewManager.getViewDescriptor();

            //The data selection can hold a subset of times
            DataSelection dataSelection = null;

            return idv.doMakeControl(dataChoice, descriptor, properties,
                                     dataSelection);
        } 
        return null;

    }





}







