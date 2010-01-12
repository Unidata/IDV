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


import org.python.core.*;
import org.python.util.*;


import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataGroup;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.DerivedDataDescriptor;
import ucar.unidata.data.DerivedNeed;

import ucar.unidata.idv.*;

import ucar.unidata.ui.CheckboxCategoryPanel;
import ucar.unidata.ui.ParamField;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.Resource;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import visad.VisADException;

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.BoxLayout;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;




/**
 * This provides a formula editing dialog so the user can edit the
 * end-user formulas.
 * <p>
 * End user formulas are described with a
 * {@link DerivedDataDescriptor} (DDD) class. The FormulaDialog
 * either creates a new DDD or is instantied with an existing one.
 * The dialog uses the  {@link ucar.unidata.idv.JythonManager}
 * to add and notify changes to the DDD. The JythonManager is responsible
 * for updating any guis, writing out the formulas to disk, etc.
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.64 $
 */


public class FormulaDialog extends JFrame implements ActionListener {


    /** This is the divider we use for displaying categories */
    private static final String MYCATDIVIDER = ">";

    /** Icon used to show/hide the advanced gui */
    public static ImageIcon ICON_UPUP;

    /** Icon used to show/hide the advanced gui */
    public static ImageIcon ICON_DOWNDOWN;

    /** Action command for changing formula */
    public static final String CMD_CHANGE = "cmd.change";

    /** Action command for adding a  formula */
    public static final String CMD_ADD = "cmd.add";

    /** Help path */
    public static final String HELP_FORMULAS = "idv.tools.formulas";


    /** Preference for having the advanced panel open */
    public static final String PREF_ADVANCEDOPEN =
        "FormulaDialog.AdvancedOpen";

    /** Preference of the window size */
    public static final String PREF_WINDOWSIZE = "FormulaDialog.WindowSize";


    static {
        ICON_UPUP =
            new ImageIcon(Resource.getImage("/auxdata/ui/icons/UpUp.gif"));

        ICON_DOWNDOWN = new ImageIcon(
            Resource.getImage("/auxdata/ui/icons/DownDown.gif"));
    }



    /** Is this a user's default derived formula */
    private JCheckBox isDefaultCbx;

    /** Is this formula for an end user */
    private JCheckBox isEndUserCbx;

    /** Text field that holds the formula string */
    private JTextField formulaField;

    /** Text field that holds the name string */
    private JTextField nameField;

    /** Text field that holds the description string */
    private JTextField descField;

    /** gui widget */
    private JTextField operandsCategoriesFld;


    /** Holds all of the display categories */
    private List categories;

    /** Holds the group string */
    private JComboBox categoryBox;

    /** gui widget */
    private JComponent catComp;

    /** Is the advanced panel open */
    private boolean advancedOpen = false;

    /**
     *  Should we ignore the entries  in the controls list
     */
    private JRadioButton useAllBtn;

    /** for default derived settings */
    private List needCompList = new ArrayList();

    /** for default derived settings */
    private List catCompList = new ArrayList();


    /**
     *  A mapping from JCheckBox to ControlDescriptor
     */
    private Hashtable cbxToCdMap;

    /** Holds the list of CategoryPanels for the displays list */
    private List catPanels = new ArrayList();

    /** Formula string */
    private String formula = "";

    /** The name */
    private String name = "";

    /** The description */
    private String description = "";

    /** The group the formula is in */
    private String category = "";

    /** Are we making a new formula */
    private boolean makingNewOne = false;

    /** The ddd we work on */
    private DerivedDataDescriptor ddd;

    /** Reference to the IDV */
    private IntegratedDataViewer idv;

    /** gui comps */
    List paramGroupComps = new ArrayList();

    /**
     * Create the dialog.
     *
     * @param idv Reference to the IDV
     * @param src The component (e.g., JButton) we popup nearby
     *
     */
    public FormulaDialog(IntegratedDataViewer idv, Component src) {
        this(idv, null, src, null);
    }

    /**
     * Create the dialog.
     *
     * @param idv Reference to the IDV
     * @param ddd Represents the formula we are editing (or null if we
     * are adding a new formula)
     * @param src The component (e.g., JButton) we popup nearby
     * @param categories List of display categories
     *
     */
    public FormulaDialog(IntegratedDataViewer idv, DerivedDataDescriptor ddd,
                         Component src, List categories) {

        this(idv, ddd, src, categories, ddd == null);
    }


    /**
     * _more_
     *
     * @param idv _more_
     * @param ddd _more_
     * @param src _more_
     * @param categories _more_
     * @param newFormula _more_
     */
    public FormulaDialog(IntegratedDataViewer idv, DerivedDataDescriptor ddd,
                         Component src, List categories, boolean newFormula) {
        super("Formula Editor");
        LogUtil.registerWindow(this);
        //      super (null, "Formula editor", false);
        //      super (idv.getFrame (), "Formula editor", false);
        this.idv     = idv;
        makingNewOne = newFormula;
        if (ddd == null) {
            ddd = new DerivedDataDescriptor(idv);
            ddd.setIsEndUser(true);
        }
        this.ddd        = ddd;
        this.categories = categories;
        //        if (ddd != null) {
        formula     = ddd.getFormula();
        name        = ddd.getId();
        description = ddd.getDescription();
        DataCategory cat = ddd.getDisplayCategory();
        if (cat != null) {
            category = cat.toString();
        }
        //        }
        doMakeWindow(src);
    }


    /**
     * Create the window and place it near the given src component
     *
     * @param src The component to locate the window near
     */
    private void doMakeWindow(Component src) {
        Component contents = doMakeContents();
        Container cpane    = getContentPane();
        cpane.setLayout(new BorderLayout());
        cpane.add(contents, BorderLayout.CENTER);
        try {
            Point     loc  = src.getLocationOnScreen();
            Dimension size = src.getSize();
            setLocation(loc.x + size.width, loc.y);
        } catch (Exception exc) {
            setLocation(50, 50);
        }
        Dimension dim = (Dimension) idv.getStore().get(PREF_WINDOWSIZE);
        pack();
        if (dim != null) {
            setSize(dim.width, getSize().height);
        }
        Msg.translateTree(this);
        show();
    }


    /**
     * Create the GUI
     *
     * @return The GUI
     */
    private Component doMakeContents() {

        ArrayList fieldLabelList = new ArrayList();
        fieldLabelList.add(new JLabel("Field name"));
        fieldLabelList.add(new JLabel("Identifier"));

        JButton jythonBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/EditJython16.png",
                                     idv.getJythonManager(),
                                     "showJythonEditor");
        GuiUtils.makeMouseOverBorder(jythonBtn);
        jythonBtn.setToolTipText("Edit Jython Library");
        JButton evalBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Evaluate16.png",
                                     this, "evaluate");
        GuiUtils.makeMouseOverBorder(evalBtn);
        evalBtn.setToolTipText("Save and Evaluate Formula");

        formulaField = new JTextField(formula, 25);
        formulaField.setToolTipText(
            "<html>Right-click to add procedures from library</html>");
        formulaField.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    List items = new ArrayList();
                    items.add(
                        GuiUtils.makeMenu(
                            "Insert Procedure Call",
                            idv.getJythonManager().makeProcedureMenu(
                                FormulaDialog.this, "insertText", null)));


                    JPopupMenu popup = GuiUtils.makePopupMenu(items);
                    popup.show(formulaField, e.getX(),
                               (int) formulaField.getBounds().getHeight());
                }
            }
        });

        nameField   = new JTextField(name, 25);
        descField   = new JTextField(description, 25);
        categoryBox = new JComboBox();
        categoryBox.setEditable(true);
        categoryBox.addItem("");
        categoryBox.setToolTipText(
            "<html>The group can be entered manually<br>Use '>' as the group delimiter. e.g.:<br>System &gt; Sub group</html>");
        //        categoryField = new JTextField(category, 25);
        if (categories != null) {
            for (int i = 0; i < categories.size(); i++) {
                String catString = categories.get(i).toString();
                catString = convertToMyCategory(catString);
                categoryBox.addItem(catString);
            }
        }
        if (category != null) {
            categoryBox.setSelectedItem(convertToMyCategory(category));
        }

        Hashtable controlsMap = new Hashtable();
        if ((ddd != null) && (ddd.getDataCategories() != null)) {
            List categories = ddd.getDataCategories();
            for (int i = 0; i < categories.size(); i++) {
                DataCategory dc   = (DataCategory) categories.get(i);
                String       name = dc.toString();
                if (name.startsWith("display:")) {
                    controlsMap.put(name.substring(8).trim(), name);
                }
            }
        }

        List      controlDescriptors = idv.getControlDescriptors();
        Vector    listData           = new Vector();

        boolean   allUnchecked       = true;
        List      cdList             = new ArrayList();
        Hashtable catMap             = new Hashtable();
        cbxToCdMap = new Hashtable();

        final JButton allOn = new JButton("All on");
        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                toggleAll(true);
            }
        });
        final JButton allOff = new JButton("All off");
        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                toggleAll(false);
            }
        });
        for (int i = 0; i < controlDescriptors.size(); i++) {
            ControlDescriptor cd =
                (ControlDescriptor) controlDescriptors.get(i);
            if (cd.canStandAlone()
                    || !idv.getPreferenceManager().shouldShowControl(cd)) {
                continue;
            }
            String displayCategory = cd.getDisplayCategory();
            CheckboxCategoryPanel catPanel =
                (CheckboxCategoryPanel) catMap.get(displayCategory);
            if (catPanel == null) {
                catPanel = new CheckboxCategoryPanel(displayCategory, false);
                catPanels.add(catPanel);
                catMap.put(displayCategory, catPanel);
                cdList.add(catPanel.getTopPanel());
                cdList.add(catPanel);
            }
            boolean isSelected = ((ddd == null)
                                  ? true
                                  : controlsMap.containsKey(
                                      cd.getControlId()));
            if (isSelected) {
                allUnchecked = false;
            }
            JCheckBox cbx = new JCheckBox(cd.getLabel(), isSelected);
            cbx.setBorder(BorderFactory.createEmptyBorder());
            catPanel.addItem(cbx);
            cbxToCdMap.put(cbx, cd);
            catPanel.add(GuiUtils.inset(cbx, new Insets(0, 20, 0, 0)));
        }


        final JPanel cdPanel = GuiUtils.vbox(cdList);
        JScrollPane cdScroll = GuiUtils.makeScrollPane(GuiUtils.top(cdPanel),
                                   100, 150);

        cdScroll.getVerticalScrollBar().setUnitIncrement(10);
        cdScroll.setPreferredSize(new Dimension(100, 150));
        //If we are creating then set to 'Use all'
        if (ddd == null) {
            allUnchecked = true;
        }
        useAllBtn = new JRadioButton("Use all ", allUnchecked);
        JRadioButton useTheseBtn = new JRadioButton("Use selected: ",
                                       !allUnchecked);
        GuiUtils.buttonGroup(useAllBtn, useTheseBtn);


        ActionListener radioBtnListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(cdPanel, !useAllBtn.isSelected());
                allOn.setEnabled( !useAllBtn.isSelected());
                allOff.setEnabled( !useAllBtn.isSelected());
            }
        };


        useAllBtn.addActionListener(radioBtnListener);
        useTheseBtn.addActionListener(radioBtnListener);


        GuiUtils.enableTree(cdPanel, !useAllBtn.isSelected());
        allOn.setEnabled( !useAllBtn.isSelected());
        allOff.setEnabled( !useAllBtn.isSelected());


        JPanel allOnOffPanel = GuiUtils.vbox(allOn, allOff);
        JPanel radioBtnPanel = GuiUtils.hbox(Misc.newList(useAllBtn,
                                   useTheseBtn));
        JPanel displaysPanel = GuiUtils.topCenter(radioBtnPanel, cdScroll);


        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        double[] stretchyY = new double[] { 0, 0, 0, 1 };
        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        JPanel bottomPanel = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Description:"), descField,
            GuiUtils.rLabel("Group:"), categoryBox,
            GuiUtils.rLabel("Displays:"), radioBtnPanel,
            GuiUtils.top(allOnOffPanel), cdScroll,
        }, 2, GuiUtils.WT_NY, stretchyY);

        //        bottomPanel.setBorder(BorderFactory.createEtchedBorder());


        isDefaultCbx = new JCheckBox(
            "Create derived quantities (Note: Use D1, D2, ..., DN as operands in formula) ",
            ddd.getIsDefault());
        isEndUserCbx = new JCheckBox("For end user", ddd.getIsEndUser());
        isDefaultCbx.setToolTipText(
            "Automatically create derived  quantities for data sources that have fields that match the following");



        List needs      = ddd.getNeeds();
        List needsComps = new ArrayList();
        operandsCategoriesFld = doMakeCategoriesField();

        if (ddd.getOperandsCategories() != null) {
            operandsCategoriesFld.setText(ddd.getOperandsCategories());
        }

        needsComps.add(new JLabel("Parameter Groups"));
        needsComps.add(new JLabel("Categories"));


        ParamGroupComponent pgc;
        ParamComponent      pc;
        List                pcs = new ArrayList();
        pcs.add(new JLabel("Parameters"));
        pcs.add(new JLabel("Categories"));
        for (int needIdx = 0; needIdx < needs.size(); needIdx++) {
            DerivedNeed need      = (DerivedNeed) needs.get(needIdx);
            String      groupName = need.getGroupName();
            if (groupName != null) {
                DataGroup group = DataGroup.getDataGroup(groupName);
                if (group == null) {
                    continue;
                }
                needCompList.add(pgc = new ParamGroupComponent(need, group));
                needsComps.add(pgc.cbx);
                needsComps.add(pgc.catFld);
            } else {
                needCompList.add(pc = new ParamComponent(need));
                pcs.add(pc.paramsFld);
                pcs.add(pc.catFld);
            }
        }


        //Add in extra
        for (int i = 0; i < 1; i++) {
            needCompList.add(pgc =
                new ParamGroupComponent(new DerivedNeed(ddd, null), null));
            needsComps.add(pgc.cbx);
            needsComps.add(pgc.catFld);
        }

        //Add in extra
        for (int i = 0; i < 1; i++) {
            needCompList.add(pc = new ParamComponent(new DerivedNeed(ddd,
                    null)));
            pcs.add(pc.paramsFld);
            pcs.add(pc.catFld);
        }
        needsComps.addAll(pcs);


        List catComps = new ArrayList();
        List cats     = ddd.getDataCategories();
        int  catCols  = 0;
        for (int i = 0; i < cats.size(); i++) {
            DataCategory dataCategory = (DataCategory) cats.get(i);
            catCompList.add(new CatComponent(dataCategory, catComps));
            if (catCols == 0) {
                catCols = catComps.size() / 2;
            }
        }

        catCompList.add(new CatComponent(new DataCategory(false), catComps));
        if (catCols == 0) {
            catCols = catComps.size() / 2;
        }
        catCompList.add(new CatComponent(new DataCategory(false), catComps));
        catCompList.add(new CatComponent(new DataCategory(false), catComps));

        GuiUtils.setHFill();
        catComp = GuiUtils.doLayout(catComps, catCols, GuiUtils.WT_YN,
                                    GuiUtils.WT_N);

        GuiUtils.tmpInsets = new Insets(2, 2, 2, 2);
        GuiUtils.setHFill();
        JComponent needsComp = GuiUtils.doLayout(needsComps, 2,
                                   GuiUtils.WT_NY, GuiUtils.WT_N);
        needsComp = GuiUtils.vbox(GuiUtils.label("Categories: ",
                operandsCategoriesFld), needsComp);
        needsComp = GuiUtils.inset(needsComp, new Insets(5, 0, 0, 0));
        JComponent cbxPanel     = GuiUtils.vbox(isEndUserCbx, isDefaultCbx);

        JComponent derivedPanel = needsComp;

        catComp = GuiUtils.vbox(new JLabel("Categorize the new data with:"),
                                catComp);
        JButton popupBtn = GuiUtils.makeButton("Define Output Categories >>",
                               this, "popupCatComp");
        derivedPanel = GuiUtils.inset(
            GuiUtils.vbox(
                new JLabel("Match on fields:"), derivedPanel,
                GuiUtils.left(popupBtn)), new Insets(0, 30, 0, 0));

        derivedPanel = GuiUtils.top(GuiUtils.vbox(cbxPanel, derivedPanel));



        JTabbedPane advancedTab = new JTabbedPane();
        advancedTab.add("Settings", bottomPanel);
        advancedTab.add("Derived", derivedPanel);
        final JPanel theBottomPanel = GuiUtils.inset(advancedTab, 4);

        advancedOpen = idv.getStore().get(PREF_ADVANCEDOPEN, advancedOpen);
        theBottomPanel.setVisible(advancedOpen);



        final JButton advancedBtn     = new JButton("Advanced  ");
        final JButton advancedIconBtn = new JButton(ICON_DOWNDOWN);
        advancedIconBtn.setContentAreaFilled(false);
        GuiUtils.makeMouseOverBorder(advancedIconBtn);
        //        advancedBtn.setBorder(BorderFactory.createEmptyBorder());
        //        advancedIconBtn.setBorder(BorderFactory.createEmptyBorder());


        ActionListener advancedListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                advancedOpen = !theBottomPanel.isVisible();
                theBottomPanel.setVisible(advancedOpen);
                checkAdvancedState(advancedBtn, advancedIconBtn);
                int oldWidth = getSize().width;
                pack();
                setSize(oldWidth, getSize().height);
                doLayout();
            }
        };
        checkAdvancedState(advancedBtn, advancedIconBtn);
        advancedBtn.addActionListener(advancedListener);
        advancedIconBtn.addActionListener(advancedListener);
        GuiUtils.tmpInsets = new Insets(4, 4, 0, 4);
        Container topPanel = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Name:"), nameField,
            GuiUtils.rLabel("       Formula:"),
            GuiUtils.centerRight(formulaField,
                                 GuiUtils.hbox(evalBtn, jythonBtn)),
            GuiUtils.rLabel("Advanced"), GuiUtils.left(advancedIconBtn)
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);

        JPanel innerPanel = GuiUtils.doLayout(Misc.newList(topPanel,
                                theBottomPanel), 1, GuiUtils.WT_Y,
                                    GuiUtils.WT_NY);
        //GuiUtils.top(inputPanel);
        //        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        //        innerPanel.add(inputPanel);

        Component buttons;
        if ( !makingNewOne) {
            if ( !ddd.getIsLocalUsers()) {
                buttons = GuiUtils.makeButtons(this,
                        new String[] { "Change Formula",
                                       "Cancel", "Help" }, new String[] {
                                           CMD_CHANGE,
                                           GuiUtils.CMD_CANCEL,
                                           GuiUtils.CMD_HELP });
            } else {
                buttons = GuiUtils.makeButtons(this,
                        new String[] { "Change Formula",
                                       "Remove Formula", "Cancel",
                                       "Help" }, new String[] { CMD_CHANGE,
                        GuiUtils.CMD_REMOVE, GuiUtils.CMD_CANCEL,
                        GuiUtils.CMD_HELP });
            }
        } else {
            buttons = GuiUtils.makeButtons(this, new String[] { "Add Formula",
                    "Cancel", "Help" }, new String[] { CMD_ADD,
                    GuiUtils.CMD_CANCEL, GuiUtils.CMD_HELP });
        }
        return GuiUtils.centerBottom(GuiUtils.inset(innerPanel, 2), buttons);
    }





    /**
     * Insert text into the formula field
     *
     * @param t text
     */
    public void insertText(String t) {
        GuiUtils.insertText(formulaField, t);
    }

    /**
     * Popup the output categories dialog
     */
    public void popupCatComp() {
        GuiUtils.makeDialog(this, "Define Output Categories",
                            GuiUtils.inset(catComp, 5), null,
                            new String[] { GuiUtils.CMD_OK });
    }

    /**
     * Class NeedComponent is used for the param needs
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.64 $
     */
    private abstract class NeedComponent {

        /** widget */
        JTextField catFld;

        /** need */
        DerivedNeed need;

        /**
         * ctor
         *
         * @param need need
         */
        public NeedComponent(DerivedNeed need) {
            this.need = need;
            catFld    = doMakeCategoriesField();
            if (need.getCategories() != null) {
                catFld.setText(need.getCategories());
            }
        }

        /**
         * get the need
         *
         * @return the need
         */
        public abstract DerivedNeed getNeed();
    }






    /**
     * show menu
     *
     * @param fld field to set
     * @param e event
     */
    private void showCategoriesPopup(final JTextField fld, MouseEvent e) {
        List cats = DataCategory.getCurrentCategories();
        cats = Misc.sort(cats);
        List items = new ArrayList();
        for (int i = 0; i < cats.size(); i++) {
            final String cat = (String) cats.get(i);
            JMenuItem    mi  = new JMenuItem(cat);
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    GuiUtils.appendText(fld, cat, ";");
                }
            });
            items.add(mi);
        }
        JMenu dummy = GuiUtils.makeMenu("", items);
        items = new ArrayList();
        GuiUtils.limitMenuSize(dummy, "Data Categories Group #", 20);
        for (int i = 0; i < dummy.getItemCount(); i++) {
            items.add(dummy.getMenuComponent(i));
        }

        JPopupMenu popup = GuiUtils.makePopupMenu(items);
        popup.show(fld, e.getX(), (int) fld.getBounds().getHeight());

    }



    /**
     * Make the categories text field
     *
     * @return new text field
     */
    private JTextField doMakeCategoriesField() {
        final JTextField fld = new JTextField("", 15);
        fld.setToolTipText(
            "<html>Semi-colon separated list of data categories<br>Right mouse to add categories</html>");
        fld.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showCategoriesPopup(fld, e);
                }
            }
        });
        return fld;
    }



    /**
     * Class ParamGroupComponent
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.64 $
     */
    private class ParamGroupComponent extends NeedComponent {

        /** widget */
        JComboBox cbx;

        /**
         * ctor
         *
         * @param need need
         * @param group          group
         */
        public ParamGroupComponent(DerivedNeed need, DataGroup group) {
            super(need);
            Vector groups = new Vector(DataGroup.getGroups());
            groups.add(0, "");
            cbx = new JComboBox(groups);
            if (group != null) {
                cbx.setSelectedItem(group);
            }
        }


        /**
         * get the need
         *
         * @return the need
         */
        public DerivedNeed getNeed() {
            Object o = cbx.getSelectedItem();
            if ((o == null) || !(o instanceof DataGroup)) {
                return null;
            }
            DataGroup dataGroup = (DataGroup) o;
            return new DerivedNeed(ddd, dataGroup.getName(),
                                   catFld.getText().trim());
        }


    }



    /**
     * Class ParamComponent is used for derived quantities
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.64 $
     */
    private class ParamComponent extends NeedComponent {

        /** widget */
        ParamField paramsFld;

        /**
         * ctor
         *
         * @param need need
         */
        public ParamComponent(DerivedNeed need) {
            super(need);
            paramsFld = new ParamField(",", true);
            if (need.getParamSets().size() > 0) {
                paramsFld.setText(StringUtil.join(", ",
                        (List) need.getParamSets().get(0)));
            }
        }

        /**
         * get the need
         *
         * @return the need
         */
        public DerivedNeed getNeed() {
            String params = paramsFld.getText().trim();
            if (params.length() == 0) {
                return null;
            }

            return new DerivedNeed(ddd,
                                   StringUtil.split(params, ",", true, true),
                                   catFld.getText().trim());
        }

    }

    /**
     * Used to hold categories
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.64 $
     */
    private class CatComponent {

        /** attribute */
        DataCategory cat;

        /** attribute */
        JTextField appendFld;

        /** attribute */
        JTextField catFld;

        /** attribute */
        JRadioButton catButton;

        /** attribute */
        JRadioButton inheritButton;

        /** attribute */
        JComboBox childIndexCbx;

        /** attribute */
        JComboBox catIndexCbx;

        /**
         * ctor
         *
         * @param cat category
         * @param comps components
         */
        public CatComponent(DataCategory cat, List comps) {
            Vector indices = new Vector(Misc.toList(new Object[] {
                new TwoFacedObject("All", -1), new TwoFacedObject("1st", 0),
                new TwoFacedObject("2nd", 1), new TwoFacedObject("3rd", 2),
                new TwoFacedObject("4th", 3), new TwoFacedObject("5th", 4)
            }));


            childIndexCbx = new JComboBox(indices);
            TwoFacedObject tfo =
                TwoFacedObject.findId(new Integer(cat.getChildIndex()),
                                      indices);
            if (tfo != null) {
                childIndexCbx.setSelectedItem(tfo);
            }
            catIndexCbx = new JComboBox(indices);
            tfo = TwoFacedObject.findId(new Integer(cat.getCategoryIndex()),
                                        indices);
            if (tfo != null) {
                catIndexCbx.setSelectedItem(tfo);
            }

            String catString = cat.toString();
            if (catString.startsWith("display:") || cat.getForDisplay()) {
                return;
            }
            if (comps.size() == 0) {
                comps.add(GuiUtils.cLabel("Data Category"));
                comps.add(GuiUtils.cLabel("  Use Data  "));
                comps.add(GuiUtils.cLabel("  Operand  "));
                comps.add(GuiUtils.cLabel("  Category  "));
                comps.add(GuiUtils.cLabel("  Append  "));
            }
            this.cat      = cat;
            inheritButton = new JRadioButton("", false);
            catButton     = new JRadioButton("", true);
            GuiUtils.buttonGroup(inheritButton, catButton);

            ActionListener listener;
            inheritButton.addActionListener(listener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    checkEnabled();
                }
            });
            catButton.addActionListener(listener);
            if (cat == null) {
                cat = new DataCategory();
            }
            appendFld = new JTextField(cat.getAppend(), 10);
            catFld    = doMakeCategoriesField();
            if (catString.equals("inherit")) {
                catString = "";
                inheritButton.setSelected(true);
            }
            catFld.setText(catString);
            comps.add(GuiUtils.leftCenter(catButton, catFld));
            comps.add(GuiUtils.right(inheritButton));
            comps.add(childIndexCbx);
            comps.add(catIndexCbx);
            comps.add(appendFld);
            checkEnabled();
        }

        /**
         * Get the data category
         *
         * @return data category
         */
        public DataCategory getDataCategory() {
            if (catFld == null) {
                return null;
            }
            if (catButton.isSelected()) {
                String catString = catFld.getText().trim();
                if (catString.length() == 0) {
                    return null;
                }
                return DataCategory.parseCategory(catString, false);
            } else {
                DataCategory cat = DataCategory.parseCategory("inherit",
                                       false);
                TwoFacedObject tfo =
                    (TwoFacedObject) childIndexCbx.getSelectedItem();
                cat.setChildIndex(((Integer) tfo.getId()).intValue());
                tfo = (TwoFacedObject) catIndexCbx.getSelectedItem();
                cat.setCategoryIndex(((Integer) tfo.getId()).intValue());
                cat.setAppend(appendFld.getText().trim());
                return cat;
            }
        }

        /**
         * enable/disable things
         */
        private void checkEnabled() {
            boolean enabled = inheritButton.isSelected();
            appendFld.setEnabled(enabled);
            childIndexCbx.setEnabled(enabled);
            catIndexCbx.setEnabled(enabled);
            catFld.setEnabled( !enabled);
        }

    }


    /**
     * Convert the DataCategory string, which is delimited by "-",
     * to the string that is displayed to the user, which is delimited
     * by ">"
     * @param cat The data category string
     * @return The category to display
     */
    private String convertToMyCategory(String cat) {
        return StringUtil.join(MYCATDIVIDER,
                               StringUtil.split(cat, DataCategory.DIVIDER,
                                   true, true));
    }

    /**
     * Configure the advanced open buttons - setting their icon, tooltip, etc.
     *
     * @param advancedBtn The text advanced button
     * @param advancedIconBtn The icon advanced button
     */
    private void checkAdvancedState(JButton advancedBtn,
                                    JButton advancedIconBtn) {
        if (advancedOpen) {
            advancedIconBtn.setIcon(ICON_UPUP);
            advancedIconBtn.setToolTipText("Click to hide advanced options");
            advancedBtn.setToolTipText("Click to hide advanced options");
        } else {
            advancedIconBtn.setIcon(ICON_DOWNDOWN);
            advancedIconBtn.setToolTipText("Click to show advanced options");
            advancedBtn.setToolTipText("Click to show advanced options");
        }
    }



    /**
     * Turn on/off all of the checkboxes
     *
     * @param to What do we turn the checkboxes to
     */
    private void toggleAll(boolean to) {
        for (int i = 0; i < catPanels.size(); i++) {
            ((CheckboxCategoryPanel) catPanels.get(i)).toggleAll(to);
        }
    }


    /**
     * Evaluate the formula
     */
    public void evaluate() {
        Misc.run(this, "evaluateInThread");
    }

    /**
     * evalue the formula
     */
    public void evaluateInThread() {
        if ( !addOrChange()) {
            return;
        }
        idv.getJythonManager().evaluateDataChoice(ddd.getDataChoice());
    }

    /**
     * Add or change the formula
     *
     * @return success
     */
    private boolean addOrChange() {
        boolean wasNew = false;
        if (ddd == null) {
            ddd = new DerivedDataDescriptor(idv);
            ddd.setIsEndUser(true);
            wasNew = true;
        }
        if ( !setValues(ddd)) {
            return false;
        }
        ddd.setIsLocalUsers(true);
        if (wasNew) {
            idv.getJythonManager().addFormula(ddd);
        } else {
            idv.getJythonManager().descriptorChanged(ddd);
        }
        return true;
    }



    /**
     * Handle the ADD, CHANGE, CANCEL  and HELP commands
     *
     * @param event The event
     */
    public void actionPerformed(ActionEvent event) {
        String cmd = event.getActionCommand();
        if (cmd.equals(CMD_ADD)) {
            if (addOrChange()) {
                closeFormulaDialog();
            }
            return;
        }

        if (cmd.equals(CMD_CHANGE)) {
            if (addOrChange()) {
                closeFormulaDialog();
            }
            return;
        }

        if (cmd.equals(GuiUtils.CMD_REMOVE)) {
            if (GuiUtils.askOkCancel(
                    "Remove Formula",
                    "Are you sure you want to remove the formula?")) {
                idv.getJythonManager().removeFormula(ddd);
                closeFormulaDialog();
            }
        }

        if (cmd.equals(GuiUtils.CMD_HELP)) {
            idv.getIdvUIManager().showHelp(HELP_FORMULAS);
        }
        if (cmd.equals(GuiUtils.CMD_CANCEL)) {
            closeFormulaDialog();
        }
    }


    /**
     * Store the windowsize and advanced open preferences and close the window
     */
    private void closeFormulaDialog() {
        idv.getStore().put(PREF_ADVANCEDOPEN, advancedOpen);
        idv.getStore().put(PREF_WINDOWSIZE, getSize());
        idv.getStore().saveIfNeeded();
        hide();
    }


    /**
     * Set the formula values from the UI
     *
     * @param ddd The ddd to set the values on
     * @return Was there a syntax error in the fomrula
     */
    private boolean setValues(DerivedDataDescriptor ddd) {
        String name = nameField.getText().trim();

        //Keep the old name around in case the new name is bad
        String oldName = ddd.getId();

        // set local var "name" to DerivedDataDescriptor's local var "id"
        ddd.setId(name);
        String formula = formulaField.getText();
        ddd.setFormula(formula);
        ddd.setIsEndUser(isEndUserCbx.isSelected());
        ddd.setIsDefault(isDefaultCbx.isSelected());
        ddd.setOperandsCategories(operandsCategoriesFld.getText().trim());

        List derivedNeeds = new ArrayList();
        for (int i = 0; i < needCompList.size(); i++) {
            NeedComponent needComponent = (NeedComponent) needCompList.get(i);
            DerivedNeed   derivedNeed   = needComponent.getNeed();
            if (derivedNeed != null) {
                derivedNeeds.add(derivedNeed);
            }
        }
        ddd.setNeeds(derivedNeeds);




        // Try to evaluate the procedure to see if the proc formula is bad.
        // make Jython procdure with dummy name testproc
        String proc = ddd.getJythonProcedure("testproc");

        if (proc != null) {
            try {
                String cleanProc = DerivedDataChoice.cleanupJythonCode(proc);
                idv.getJythonManager().getDerivedDataInterpreter(null).exec(
                    cleanProc);
            } catch (org.python.core.PySyntaxError pse) {
                LogUtil.userErrorMessage("The formula: " + formula
                                         + " has a Jython error: " + pse);
                return false;
            } catch (Exception e) {
                LogUtil.userErrorMessage("The formula: " + formula
                                         + " has a Jython error: " + e);
                return false;
            }
        }

        String desc = descField.getText().trim();
        if (desc.length() == 0) {
            desc = name;
        }
        ddd.setFormula(formulaField.getText());  // why duplicate?
        ddd.setDescription(desc);
        setCategory(ddd);
        return true;
    }


    /**
     * Set the group  and what displays to use on the ddd.
     * This takes the string from the categoryBox and if it
     * is non-zero length will create a
     * {@link ucar.unidata.data.DataCategory} for display.
     * It will then look at the list of display types
     * and if any are selected will add in DataCategory-s
     * with the "display:displaytype" format
     *
     * @param ddd The ddd to set the category on
     */
    private void setCategory(DerivedDataDescriptor ddd) {
        ArrayList categories = new ArrayList();
        //        String    category   = categoryField.getText().trim();
        String category = categoryBox.getSelectedItem().toString().trim();
        if (category.length() > 0) {
            category = StringUtil.join(DataCategory.DIVIDER,
                                       StringUtil.split(category,
                                           MYCATDIVIDER, true, true));

            DataCategory dataCategory = DataCategory.parseCategory(category,
                                            true);
            dataCategory.setForDisplay(true);
            categories.add(dataCategory);
        }

        if ( !useAllBtn.isSelected()) {
            int numNotSelected = 0;
            for (Enumeration keys =
                    cbxToCdMap.keys(); keys.hasMoreElements(); ) {
                JCheckBox cbx = (JCheckBox) keys.nextElement();
                if ( !cbx.isSelected()) {
                    numNotSelected++;
                    continue;
                }
                ControlDescriptor cd =
                    (ControlDescriptor) cbxToCdMap.get(cbx);
                categories.add(new DataCategory("display:"
                        + cd.getControlId(), false));
            }
            if (numNotSelected == 0) {
                categories = new ArrayList();
            }
        }


        for (int i = 0; i < catCompList.size(); i++) {
            CatComponent catComponent = (CatComponent) catCompList.get(i);
            DataCategory cat          = catComponent.getDataCategory();
            if (cat != null) {
                categories.add(cat);
            }
        }

        ddd.setDataCategories(categories);
    }




}
