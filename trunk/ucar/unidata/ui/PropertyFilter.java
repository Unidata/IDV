/*
 * $Id: PropertyFilter.java,v 1.6 2007/07/06 20:45:32 jeffmc Exp $
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


import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;



/**
 * Misc. methods - a whole grab bag.
 * @author Metapps development group.
 * @version $Revision: 1.6 $ $Date: 2007/07/06 20:45:32 $
 */


import visad.Real;

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;

import javax.swing.*;





/**
 *  Is used to define an operator/value and to evaluate the filter on a given object.
 */


public class PropertyFilter {

    /** _more_ */
    public static final String NULL_NAME = "--";

    /** _more_ */
    public static final int OP_GREATERTHAN = 0;

    /** _more_ */
    public static final int OP_LESSTHAN = 1;

    /** _more_ */
    public static final int OP_EQUALS = 2;

    /** _more_ */
    public static final int OP_NOTEQUALS = 3;

    /** _more_ */
    public static final int OP_STREQUALS = 4;

    /** _more_ */
    public static final int OP_STRNOTEQUALS = 5;

    /** _more_ */
    public static final int OP_STRCONTAINS = 6;

    /** _more_ */
    public static final int OP_STRNOTCONTAINS = 7;


    /** _more_ */
    public static final int OP_STRMATCH = 8;

    /** _more_ */
    public static final int OP_STRNOTMATCH = 9;


    /** _more_ */
    public static final int OP_STRCONTAINS_NOCASE = 10;

    /** _more_ */
    public static final int OP_STRNOTCONTAINS_NOCASE = 11;

    /** _more_ */
    public static final int OP_STRCONTAINEDIN = 12;

    /** _more_ */
    public static final int OP_STRCONTAINEDIN_NOCASE = 13;

    /** _more_          */
    public static final int[] OPERATORS = {
        OP_GREATERTHAN, OP_LESSTHAN, OP_EQUALS, OP_NOTEQUALS, OP_STREQUALS,
        OP_STRNOTEQUALS, OP_STRCONTAINS, OP_STRCONTAINS_NOCASE,
        OP_STRNOTCONTAINS, OP_STRNOTCONTAINS_NOCASE, OP_STRMATCH,
        OP_STRNOTMATCH, OP_STRCONTAINEDIN, OP_STRCONTAINEDIN_NOCASE
    };



    /** _more_          */
    public static final String[] OP_LABELS = {
        ">", "<", "=", "!=", "String equals", "String not equals",
        "Contains string", "Contains string (no case)", "Not contains",
        "Not contains (no case)", "String matches", "Not string matches",
        "Is contained in", "Is contained in (no case)"
    };






    /** _more_ */
    private String name;

    /** _more_ */
    private int operator;

    /** _more_ */
    private String value;


    /**
     *  Create a filter.
     *
     *  @param name the property name
     *  @param operator The operator
     *  @param value The value to match
     */
    public PropertyFilter(String name, String operator, String value) {
        this(name, getOperator(operator), value);
    }



    /**
     *  Create a filter.
     *
     *  @param name the property name
     *  @param operator The operator
     *  @param value The value to match
     */
    public PropertyFilter(String name, int operator, String value) {
        this.name     = name;
        this.operator = operator;
        this.value    = value;
    }


    /**
     *  A parameterless ctor for encoding/decoding
     */
    public PropertyFilter() {}




    /**
     *  Apply this filter to the given value.
     *
     *  @param inValue The value to check
     *  @return  Does this filter match the given value.
     */
    public boolean ok(Object inValue) {
        return ok(inValue, value);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isNumericOperator() {
        return ((operator == OP_GREATERTHAN) || (operator == OP_LESSTHAN)
                || (operator == OP_EQUALS) || (operator == OP_NOTEQUALS));
    }


    /**
     * _more_
     *
     * @param inValue _more_
     * @param myValue _more_
     *
     * @return _more_
     */
    public boolean ok(Object inValue, Object myValue) {
        if (name.length() == 0) {
            return true;
        }
        if (operator == OP_STREQUALS) {
            return myValue.toString().equals(inValue.toString());
        } else if (operator == OP_STRNOTEQUALS) {
            return !myValue.toString().equals(inValue.toString());
        } else if (operator == OP_STRCONTAINS) {
            return inValue.toString().trim().indexOf(myValue.toString()) >= 0;
        } else if (operator == OP_STRCONTAINS_NOCASE) {
            return inValue.toString().toLowerCase().trim().indexOf(
                myValue.toString().toLowerCase()) >= 0;
        } else if (operator == OP_STRNOTCONTAINS) {
            return !(inValue.toString().indexOf(myValue.toString()) >= 0);
        } else if (operator == OP_STRNOTCONTAINS_NOCASE) {
            return !(inValue.toString().toLowerCase().indexOf(
                myValue.toString().toLowerCase()) >= 0);
        } else if (operator == OP_STRMATCH) {
            return StringUtil.stringMatch(inValue.toString().trim(),
                                          myValue.toString());
        } else if (operator == OP_STRNOTMATCH) {
            return !StringUtil.stringMatch(inValue.toString().trim(),
                                           myValue.toString());
        } else if (operator == OP_STRCONTAINEDIN) {
            return myValue.toString().indexOf(inValue.toString().trim()) >= 0;

        } else if (operator == OP_STRCONTAINEDIN_NOCASE) {
            return myValue.toString().toLowerCase().
                        indexOf(inValue.toString().toLowerCase().trim()) >= 0;
        }

        try {
            Real myReal;
            if (myValue instanceof Real) {
                myReal = (Real) myValue;
            } else {
                myReal = ucar.visad.Util.toReal(myValue.toString());
            }
            Real inReal;
            if (inValue instanceof Real) {
                inReal = (Real) inValue;
            } else {
                inReal = ucar.visad.Util.toReal(inValue.toString());
            }


            if (operator == OP_GREATERTHAN) {
                return inReal.__gt__(myReal) == 1;
            } else if (operator == OP_LESSTHAN) {
                return inReal.__lt__(myReal) == 1;
            } else if (operator == OP_EQUALS) {
                return inReal.__eq__(myReal) == 1;
            } else if (operator == OP_NOTEQUALS) {
                return inReal.__ne__(myReal) == 1;
            }
        } catch (Exception exc) {
            System.err.println("Exception: " + exc);
        }
        return false;
    }

    /**
     * _more_
     *
     * @param opLabel _more_
     *
     * @return _more_
     */
    public static int getOperator(String opLabel) {
        for (int i = 0; i < OPERATORS.length; i++) {
            if (opLabel.equals(OP_LABELS[i])) {
                return OPERATORS[i];
            }
        }
        return OPERATORS[0];
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getOperatorLabel() {
        for (int i = 0; i < OPERATORS.length; i++) {
            if (operator == OPERATORS[i]) {
                return OP_LABELS[i];
            }
        }
        return OP_LABELS[0];
    }


    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * _more_
     * @return _more_
     */
    public String getName() {
        return name;
    }

    /**
     *  Set the Operator property. This is here for legacy bundles
     *
     *  @param value The new value for Operator
     */
    public void setOperator(String value) {
        operator = getOperator(value);
    }



    /**
     *  Set the Operator property.
     *
     *  @param value The new value for Operator
     */
    public void setOperator(int value) {
        operator = value;
    }

    /**
     *  Get the Operator property.
     *
     *  @return The Operator
     */
    public int getOperator() {
        return operator;
    }

    /**
     *  Set the Value property.
     *
     *  @param value The new value for Value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     *  Get the Value property.
     *
     *  @return The Value
     */
    public String getValue() {
        return value;
    }



    /**
     * Class FilterGui _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.6 $
     */
    public static class FilterGui {

        /** filter names */
        private List filterNameFields = new ArrayList();

        /** filter values */
        private List filterValues = new ArrayList();

        /** filter operations */
        private List filterOps = new ArrayList();

        /** _more_ */
        protected boolean matchAll;

        /** _more_ */
        protected boolean enabled;


        /** The action listener */
        private ActionListener listener;

        /** _more_ */
        JPanel contents;

        /**
         * _more_
         *
         * @param filters _more_
         * @param filterNames _more_
         * @param enabled _more_
         * @param matchAll _more_
         */
        public FilterGui(List filters, List filterNames, boolean enabled,
                         boolean matchAll) {

            this(filters, filterNames, enabled, matchAll, null);
        }


        /**
         * _more_
         *
         * @param filters _more_
         * @param filterNames _more_
         * @param enabled _more_
         * @param matchAll _more_
         * @param listener the listener
         */
        public FilterGui(List filters, List filterNames, boolean enabled,
                         boolean matchAll, ActionListener listener) {

            this.listener = listener;
            this.enabled  = enabled;
            this.matchAll = matchAll;
            makeGui(filters, filterNames);
        }

        /**
         * _more_
         *
         * @param filters _more_
         * @param filterNames _more_
         */
        private void makeGui(List filters, List filterNames) {

            if (filterNames == null) {
                filterNames = new ArrayList();
            }
            List filterComps = new ArrayList();
            filterComps.add(new JLabel("Property"));
            filterComps.add(new JLabel(""));
            filterComps.add(new JLabel("Value"));
            PropertyFilter filter;
            PropertyFilter defaultFilter = new PropertyFilter(NULL_NAME,
                                               PropertyFilter.OP_GREATERTHAN,
                                               "");

            filterNames = new ArrayList(filterNames);
            if ( !filterNames.contains(NULL_NAME)) {
                filterNames.add(0, NULL_NAME);
            }
            int cnt = filters.size() + 1;
            if (cnt < 5) {
                cnt = 5;
            }
            for (int i = 0; i < cnt; i++) {
                if (i < filters.size()) {
                    filter = (PropertyFilter) filters.get(i);
                } else {
                    filter = defaultFilter;
                }
                JComboBox nameFld = new JComboBox();
                if ( !filterNames.contains(filter.getName())) {
                    List tmp = Misc.newList(filter.getName());
                    tmp.addAll(filterNames);
                    GuiUtils.setListData(nameFld, tmp);
                } else {
                    GuiUtils.setListData(nameFld, filterNames);
                }
                if (filterNames.get(0) instanceof TwoFacedObject) {
                    nameFld.setSelectedItem(
                        new TwoFacedObject(
                            filter.getName(), filter.getName()));
                } else {
                    nameFld.setSelectedItem(filter.getName());
                }
                JComboBox opFld = new JComboBox(PropertyFilter.OP_LABELS);
                opFld.setSelectedItem(filter.getOperatorLabel());
                JTextField valueFld = new JTextField(filter.getValue(), 20);
                valueFld.setToolTipText(
                    "<html>Enter a value to compare.<br>Use <i>value[unit]</i> to specify a unit.</html>");
                if (listener != null) {
                    valueFld.addActionListener(listener);
                }

                filterNameFields.add(nameFld);
                filterOps.add(opFld);
                filterValues.add(valueFld);
                filterComps.add(nameFld);
                filterComps.add(opFld);
                filterComps.add(valueFld);
            }


            final JCheckBox enabledCbx = new JCheckBox("Filters Enabled",
                                             enabled);
            enabledCbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    enabled = enabledCbx.isSelected();
                    if (listener != null) {
                        listener.actionPerformed(ae);
                    }
                }
            });


            final JRadioButton allRB =
                new JRadioButton("Match all of the filters", matchAll);
            final JRadioButton anyRB =
                new JRadioButton("Match any of the filters", !matchAll);
            allRB.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    matchAll = allRB.isSelected();
                    if (listener != null) {
                        listener.actionPerformed(ae);
                    }
                }
            });
            anyRB.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    matchAll = !anyRB.isSelected();
                    if (listener != null) {
                        listener.actionPerformed(ae);
                    }
                }
            });

            GuiUtils.buttonGroup(allRB, anyRB);
            JPanel allPanel =
                GuiUtils.inset(GuiUtils.hbox(Misc.newList(enabledCbx,
                    new JLabel("      "), allRB, anyRB), 2), new Insets(0,
                        10, 0, 10));

            GuiUtils.tmpInsets = new Insets(2, 2, 2, 2);
            JPanel lines = GuiUtils.doLayout(filterComps, 3, GuiUtils.WT_Y,
                                             GuiUtils.WT_N);


            contents = GuiUtils.topCenter(allPanel, GuiUtils.top(lines));


        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getMatchAll() {
            return matchAll;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getEnabled() {
            return enabled;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public JComponent getContents() {
            return contents;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public List getFilters() {
            List filters = new ArrayList();
            for (int i = 0; i < filterNameFields.size(); i++) {
                JComboBox  nameFld  = (JComboBox) filterNameFields.get(i);
                JTextField valueFld = (JTextField) filterValues.get(i);
                JComboBox  opFld    = (JComboBox) filterOps.get(i);
                String name =
                    TwoFacedObject.getIdString(nameFld.getSelectedItem());
                if (name.equals(NULL_NAME) || (name.length() == 0)) {
                    continue;
                }
                filters.add(new PropertyFilter(name,
                        opFld.getSelectedItem().toString(),
                        valueFld.getText().trim()));
            }
            return filters;

        }

    }


    /**
     * @deprecated Keep around for bundles
     *
     * @param v _more_
     */
    public void setIsNot(boolean v) {}



}

