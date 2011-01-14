/*
 * $Id: PropertiesDialog.java,v 1.43 2007/07/06 17:55:28 jeffmc Exp $
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

package ucar.unidata.ui.symbol;


import org.w3c.dom.Element;

import ucar.unidata.data.DataAlias;
import ucar.unidata.data.point.PointOb;

import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.ui.ParamField;
import ucar.unidata.ui.colortable.ColorTableCanvas;
import ucar.unidata.ui.colortable.ColorTableManager;

import ucar.unidata.ui.drawing.*;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.Range;
import ucar.unidata.util.TwoFacedObject;

import visad.Data;


import visad.Unit;

import visad.VisADGeometryArray;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;


import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Manages the properties dialog for MetSymbol-s
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.43 $
 */
public class PropertiesDialog implements ActionListener {

    /** Has this dialog been initialized */
    boolean initialized = false;


    /** The symbol we represent */
    private MetSymbol symbol;

    private List<MetSymbol> selected;


    /** The canvas */
    private StationModelCanvas canvas;

    /** The dialog */
    JDialog dialog;

    /** Holds the color table from the menu command */
    private ColorTable tmpColorTable;

    /** Maps parameters */
    private Hashtable paramMap;

    /** Holds the scale value */
    private JTextField scaleFld;

    /** Shows the color */
    private GuiUtils.ColorSwatch bgColorSwatch;

    /** Shows the color */
    private GuiUtils.ColorSwatch fgColorSwatch;

    /** Holds the scale by scale min */
    private JTextField scaleMinFld;

    /** Holds the scale by scale max */
    private JTextField scaleMaxFld;

    /** Holds the scale by data min */
    private JTextField scaleDataMinFld;

    /** Holds the scale by data min */
    private JTextField scaleDataMaxFld;

    /** The parameter name for scaling */
    private ParamField scaleParamFld;

    /** The parameter name for scaling */
    private ParamField colorTableParamFld;

    /** List of ColorMaps */
    private List colorMappings;

    /**
     * Panel we put the color maps into. We keep this around
     *   because the number of color maps shown in the gui is dynamic
     */
    private JPanel mappingHolder;


    /** The unit for scaling */
    private JComboBox scaleUnitFld = null;


    /** Popup button for the color table */
    private JButton colorPopupBtn;

    /** Text label for color table */
    private JLabel colorLbl;

    /** Do we show the symbol */
    private JCheckBox shownCheckbox;

    /** Color bar label for color table */
    private JLabel colorBarLbl;

    /** Param for color by */
    private ParamField colorParamFld;

    /** Min data value for color by */
    private JTextField colorMinFld;

    /** Max data value for color by */
    private JTextField colorMaxFld;

    /** Unit for color by */
    private JComboBox colorUnitFld = null;

    /** holds dialog widgets */
    private List paramFields = new ArrayList();

    /** holds dialog widgets */
    private List valueFields = new ArrayList();

    /** holds dialog widgets */
    private List unitFields = new ArrayList();

    /** holds dialog widgets */
    private List formatFields = new ArrayList();

    /** holds dialog widgets */
    private List fontFields = new ArrayList();

    /** holds dialog widgets */
    private List fontSizeFields = new ArrayList();

    /** Rotation GUI */
    RotateGui rotateZGui;

    /** Rotation GUI */
    RotateGui rotateXGui;

    /** Rotation GUI */
    RotateGui rotateYGui;

    /** Listener that reformats the example label */
    private ActionListener formatListener;

    /** Any errors when applying properties */
    private boolean inError = false;

    /**
     * Create a new PropertiesDialog
     *
     * @param symbol The symbol
     * @param canvas The canvas
     */
    public PropertiesDialog(MetSymbol symbol, StationModelCanvas canvas) {
        this.symbol = symbol;
        this.canvas = canvas;
    }


    public PropertiesDialog(List<MetSymbol> selected, StationModelCanvas canvas) {
	this.selected = selected;
        this.symbol = selected.get(0);
        this.canvas = canvas;
    }


    public boolean doMultiple() {
	return selected !=null;
    }

    /**
     * Close the dialog
     */
    public void close() {
        if (dialog != null) {
            dialog.dispose();
        }
    }

    /**
     * Show the dialog
     */
    public void show() {
        if (dialog == null) {
            doMakeContents();
            initialized = true;
        }
        dialog.setVisible(true);
    }

    /**
     * Handle the action
     *
     * @param ae The action
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_SAVE) || cmd.equals(GuiUtils.CMD_APPLY)
                || cmd.equals(GuiUtils.CMD_OK)) {
            try {
                doApply();
                setDialogTitle();
            } catch (NumberFormatException nfe) {
                LogUtil.userErrorMessage("Bad format for " + what);
                return;
            } catch (Exception exc) {
                LogUtil.logException("Error processing  " + what, exc);
                return;
            }
            canvas.repaint();
        }
        if (cmd.equals(GuiUtils.CMD_APPLY)) {
            canvas.doSave();
        }
        if (cmd.equals(GuiUtils.CMD_CANCEL) || cmd.equals(GuiUtils.CMD_OK)) {
            dialog.setVisible(false);
        }
    }

    /** Used for loggin errors */
    private String what = "";


    private String getParam(ParamField paramField) {
        String t = paramField.getText().trim();
        if(t.length()==0) return null;
        return t;
    }


    /**
     * Apply the properties
     *
     * @throws Exception when bad things happen
     */
    protected void doApply() throws Exception {

        if (formatListener != null) {
            inError = false;
            formatListener.actionPerformed(null);
            if (inError) {
                return;
            }
        }

	if(!doMultiple()) {
	    doApply(symbol);
	}  else {
	    for(MetSymbol selectedSymbol: selected) {
		doApply(selectedSymbol);
	    }
	}
    }

    protected void doApplyColorSettings(MetSymbol symbol) throws Exception {
        if (symbol.shouldShowColorTableGui()) {
	    if(!doMultiple()) {
		symbol.setColorTableParam(getParam(colorTableParamFld));
	    }
            what = "Color table properties";
            symbol.setColorTable(tmpColorTable);
            what = "color table minimum range";
            symbol.getColorTableRange().setMin(
                Misc.parseNumber(colorMinFld.getText()));
            what = "color table maximum range";
            symbol.getColorTableRange().setMax(
                Misc.parseNumber(colorMaxFld.getText()));
            String tmpUnitName =
                TwoFacedObject.getIdString(colorUnitFld.getSelectedItem());

            what = "color table unit";
            if (tmpUnitName != null) {
                Unit realUnit = ucar.visad.Util.parseUnit(tmpUnitName);
            }
            symbol.setColorTableUnitName(tmpUnitName);
        }


	if(doMultiple()) {
	    return;
	}

        if (colorParamFld != null) {
            what = "Color by properties";
            symbol.setColorParam(getParam(colorParamFld));
            symbol.setColorMappings(ColorMap.applyProperties(colorMappings));
        }



    }

    protected void doApply(MetSymbol symbol) throws Exception {


	doApplyColorSettings(symbol);

	if(doMultiple()) {
	    return;
	}

        String[] descrs  = symbol.getParamDescs();
        String[] ids     = symbol.getParamIds();
        boolean  doUnits = symbol.showDisplayUnitInProperties();
        boolean  doFont  = (symbol instanceof TextSymbol);



	if(!doMultiple()) {
	    symbol.applyProperties();
	    symbol.setActive(shownCheckbox.isSelected());
	}

        what = "scale field";
        symbol.setScale(Misc.parseNumber(scaleFld.getText()));


        if (fgColorSwatch.getSwatchColor() == null) {
            symbol.setForeground(null);
        } else {
            symbol.setForeground(fgColorSwatch.getSwatchColor());
        }
        symbol.setBackground(bgColorSwatch.getSwatchColor());

        if (symbol.shouldShowScaleGui()) {
            symbol.setScaleParam(getParam(scaleParamFld));
            String tmpUnitName =
                TwoFacedObject.getIdString(scaleUnitFld.getSelectedItem());
            what = "scale";
            symbol.getScaleDataRange().setMin(
                Misc.parseNumber(scaleDataMinFld.getText()));
            symbol.getScaleDataRange().setMax(
                Misc.parseNumber(scaleDataMaxFld.getText()));
            symbol.getScaleRange().setMin(
                Misc.parseNumber(scaleMinFld.getText()));
            symbol.getScaleRange().setMax(
                Misc.parseNumber(scaleMaxFld.getText()));
            what = "scale unit";
            if (tmpUnitName != null) {
                Unit realUnit = ucar.visad.Util.parseUnit(tmpUnitName);
            }
            symbol.setScaleUnitName(tmpUnitName);
        }


        if (symbol.shouldShowRotateGui()) {
            rotateZGui.apply();
            rotateXGui.apply();
            rotateYGui.apply();
        }


        for (int i = 0; i < paramFields.size(); i++) {
            ParamField paramBox = (ParamField) paramFields.get(i);
            ids[i] = getParam(paramBox);
            if (doUnits) {
                String unitName = TwoFacedObject.getIdString(
                                      ((JComboBox) unitFields.get(
                                          i)).getSelectedItem());
                what = "display unit";
                if (unitName != null) {
                    Unit realUnit = ucar.visad.Util.parseUnit(unitName);
                }
                symbol.setDisplayUnitName(unitName);
                if (symbol instanceof ValueSymbol) {
                    String formatString =
                        ((JComboBox) formatFields.get(
                            i)).getSelectedItem().toString();
                    formatString = formatString.trim();
                    ((ValueSymbol) symbol).setNumberFormatString(
                        formatString);
                }
            }

            if ( !(symbol instanceof WeatherSymbol)) {
                Object fld = valueFields.get(i);
                if (fld instanceof JTextField) {
                    symbol.setParamValue(
                        i, ((JTextField) fld).getText().trim());
                }
            }
        }

        if (doFont) {
            // Create a new font from the name and size.
            // There can only be one font size per symbol, so just use get(0)
            TwoFacedObject fontTFO =
                (TwoFacedObject) ((JComboBox) fontFields.get(
                    0)).getSelectedItem();
            Font f = (Font) fontTFO.getId();
            String fontSize = (String) ((JComboBox) fontSizeFields.get(
                                  0)).getSelectedItem();
            int size = (f == null)
                       ? ((TextSymbol) symbol).getFontSize()
                       : f.getSize();

            what = "font size";
            size = Integer.parseInt(fontSize);
            ((TextSymbol) symbol).setFontSize(size);
            Font newF = (f == null)
                        ? null
                        : f.deriveFont((float) size);
            ((TextSymbol) symbol).setFont(newF);
        }

        // now we have to reset these in case they changed
        symbol.setParamIds(ids);
        symbol.setParamDescs(descrs);
        canvas.repaint();
        updateMappings();
    }


    /**
     * Get the list of params to show in the gui
     *
     * @param addNull If true then add the '-none-' param
     *
     * @return List of TwoFacedObject-s that represent parameters
     */
    private List getParameterList(boolean addNull) {
        List ids = new ArrayList(DataAlias.getLabelIdList());
        ids.add(0, new TwoFacedObject("Observation time", "time"));
        ids.add(0, new TwoFacedObject("Latitude", "lat"));
        ids.add(0, new TwoFacedObject("Longitude", "lon"));
        ids.add(0, new TwoFacedObject("Altitude", "alt"));
        if (addNull) {
            ids.add(0, new TwoFacedObject("-none-", null));
        }
        return ids;
    }



    /**
     * Make the gui
     */
    public void doMakeContents() {

        JTabbedPane tabbedPane = new JTabbedPane();


        boolean     doUnits    = symbol.showDisplayUnitInProperties();
        boolean     doFont     = (symbol instanceof TextSymbol);

        List        comps      = new ArrayList();
        symbol.initPropertyComponents(comps);
        List labelIds = getParameterList(false);
        paramMap = new Hashtable();
        for (int i = 0; i < labelIds.size(); i++) {
            Object o = labelIds.get(i);
            paramMap.put(o.toString(), o);
        }
        //boolean doValueFields = !(symbol instanceof WeatherSymbol);
        boolean doValueFields = !symbol.doAllObs();
        if (symbol instanceof LabelSymbol) {
            doValueFields = false;
        }

        // get the ids and descriptors
        String[] ids    = symbol.getParamIds();
        String[] descrs = symbol.getParamDescs();
        List     rightComps;

        for (int i = 0; i < ids.length; i++) {
            JTextField tmpFld = null;
            rightComps = new ArrayList();
            if (doValueFields) {
                ParamField tfld = new ParamField(",");
                tfld.setToolTipText("<html>Comma separated list of parameters.<br>Right mouse to add aliases or current fields</html>");
                tfld.setText(ids[i]);
                paramFields.add(tfld);
                comps.add(GuiUtils.rLabel(descrs[i] + ":  "));
                rightComps.add(tfld);

                //if ( !(symbol instanceof WeatherSymbol)) {
                Object paramValue = symbol.getParamValue(i);
                tmpFld = new JTextField(((paramValue == null)
                                         ? ""
                                         : paramValue.toString()), 10);
                valueFields.add(tmpFld);
                rightComps.add(GuiUtils.rLabel(" Ex. value:  "));
                rightComps.add(tmpFld);
                comps.add(GuiUtils.left(GuiUtils.hbox(rightComps, 5)));
            }
            final JTextField vfld = tmpFld;

            if (doUnits) {
                String unitName = symbol.getDisplayUnitName();
                JComboBox ufld =
                    GuiUtils.getEditableBox(canvas.getDefaultUnitList(),
                                            unitName);

                unitFields.add(ufld);
                comps.add(GuiUtils.rLabel(" Unit:"));
                rightComps = new ArrayList();
                rightComps.add(GuiUtils.left(ufld));

                if (symbol instanceof ValueSymbol) {
                    String formatString =
                        ((ValueSymbol) symbol).getNumberFormatString();
                    double value = ((ValueSymbol) symbol).getDoubleValue();
                    final JComboBox ffld = GuiUtils.getEditableBox(
                                               canvas.getDefaultFormatList(
                                                   value), formatString);
                    what = "format string";
                    DecimalFormat df = new DecimalFormat(formatString);
                    final JLabel formatLabel = new JLabel(" ex: "
                                                   + df.format(value));

                    formatListener = new ObjectListener(vfld) {
                        public void actionPerformed(ActionEvent ae) {
                            String pattern =
                                ffld.getSelectedItem().toString();
                            try {
                                inError = false;
                                double value =
                                    Misc.parseNumber(vfld.getText().trim());
                                DecimalFormat df = new DecimalFormat(pattern);
                                formatLabel.setText(" ex: "
                                        + df.format(value));
                            } catch (NumberFormatException nfe) {
                                inError = true;
                                LogUtil.userMessage("Bad example value:"
                                        + vfld.getText());
                            } catch (IllegalArgumentException iae) {
                                LogUtil.userMessage("Bad number format:"
                                        + pattern);
                            }
                        }
                    };
                    if (vfld != null) {
                        vfld.addActionListener(formatListener);
                    }
                    ffld.addActionListener(formatListener);
                    rightComps.add(GuiUtils.rLabel(" Format:"));
                    formatFields.add(ffld);
                    rightComps.add(GuiUtils.hgrid(Misc.newList(ffld,
                            formatLabel), 0));

                }
                comps.add(GuiUtils.left(GuiUtils.hbox(rightComps, 5)));
            }

            if (doFont) {
                Font   font     = ((TextSymbol) symbol).getFont();
                Font[] fonts    = ((TextSymbol) symbol).getFontList();
                List   fontList = new Vector(fonts.length + 1);
                fontList.add(makeFontTFO(null));
                for (int f = 0; f < fonts.length; f++) {
                    fontList.add(makeFontTFO(fonts[f]));
                }

                JComboBox ffld = GuiUtils.getEditableBox(fontList,
                                     makeFontTFO(font));
                ffld.setEditable(false);

                fontFields.add(ffld);
                int fontSize = ((TextSymbol) symbol).getFontSize();
                JComboBox sfld =
                    GuiUtils.getEditableBox(
                        Misc.newList(((TextSymbol) symbol).FONT_SIZES),
                        String.valueOf(fontSize));
                fontSizeFields.add(sfld);
                comps.add(GuiUtils.rLabel("Font:"));
                rightComps = new ArrayList();
                rightComps.add(ffld);
                rightComps.add(new JLabel(" Size:"));
                rightComps.add(sfld);
                comps.add(GuiUtils.left(GuiUtils.hbox(rightComps, 5)));
            }
        }




        shownCheckbox = new JCheckBox("", symbol.getActive());
        comps.add(GuiUtils.rLabel("Shown:"));
        comps.add(GuiUtils.left(shownCheckbox));

        JComponent[] fgSwatch =
            GuiUtils.makeColorSwatchWidget(symbol.getForeground(), "");
        fgColorSwatch = (GuiUtils.ColorSwatch) fgSwatch[0];
        JPanel fgColorPanel = GuiUtils.hbox(fgSwatch[0], fgSwatch[2]);
        comps.add(GuiUtils.rLabel("Foreground Color:"));
        comps.add(GuiUtils.left(fgColorPanel));


        JComponent[] bgSwatch =
            GuiUtils.makeColorSwatchWidget(symbol.getBackground(), "");
        bgColorSwatch = (GuiUtils.ColorSwatch) bgSwatch[0];
        JPanel bgColorPanel = GuiUtils.hbox(bgSwatch[0], bgSwatch[2]);
        comps.add(GuiUtils.rLabel("Background Color:"));
        comps.add(GuiUtils.left(bgColorPanel));


        scaleFld = new JTextField("", 5);
        scaleFld.setText(Misc.format(symbol.getScale()));
        comps.add(GuiUtils.rLabel("Scale Size By:"));
        comps.add(GuiUtils.left(scaleFld));



	if(!doMultiple()) {
	    GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
	    tabbedPane.add("Display",
			   GuiUtils.topLeft(GuiUtils.doLayout(comps, 2,
							      GuiUtils.WT_N, GuiUtils.WT_N)));
	}


        if (symbol.shouldShowColorTableGui()) {
            JTabbedPane colorTabbedPane = new JTabbedPane();
            tabbedPane.add("Color By", colorTabbedPane);

            comps = new ArrayList();

            colorUnitFld =
                GuiUtils.getEditableBox(canvas.getDefaultUnitList(),
                                        symbol.getColorTableUnitName());
            if (colorPopupBtn == null) {
                colorMaxFld        = new JTextField("", 5);
                colorMinFld        = new JTextField("", 5);
                colorTableParamFld = new ParamField();
                colorTableParamFld.setToolTipText("<html>Parameter name to color by.<br>Right mouse to use aliases or current fields</html>");
                colorLbl           = new JLabel("-none-");
                colorBarLbl        = new JLabel();
                colorPopupBtn      = new JButton("Set");
                colorPopupBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        showColorTableMenu(colorPopupBtn);
                    }
                });
                colorTableParamFld.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        colorTableParamChanged();
                    }
                });

            }
            colorMinFld.setText(
                Misc.format(symbol.getColorTableRange().getMin()));
            colorMaxFld.setText(
                Misc.format(symbol.getColorTableRange().getMax()));
            setTmpColorTable(symbol.getColorTable());
            colorTableParamFld.setText(symbol.getColorTableParam());
	    if(!doMultiple()) {
		comps.add(GuiUtils.rLabel("Map Value of:"));
		comps.add(GuiUtils.left(colorTableParamFld));
		comps.add(new JLabel(""));
		comps.add(new JLabel("Into:"));
	    }
            comps.add(GuiUtils.rLabel("Data Range:"));
            rightComps = new ArrayList();
            rightComps.add(colorMinFld);
            rightComps.add(colorMaxFld);
            rightComps.add(new JLabel("  Unit:"));
            rightComps.add(colorUnitFld);
            comps.add(GuiUtils.left(GuiUtils.hbox(rightComps, 5)));

            comps.add(GuiUtils.rLabel("Color Table:"));
            comps.add(GuiUtils.left(GuiUtils.hbox(colorPopupBtn, colorBarLbl,
                    colorLbl, 5)));

            GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
            colorTabbedPane.add("Map Value Into Color Table",
                                GuiUtils.topLeft(GuiUtils.doLayout(comps, 2,
                                    GuiUtils.WT_N, GuiUtils.WT_N)));

            comps = new ArrayList();

            if (colorParamFld == null) {
                colorParamFld =new ParamField();
            }
            colorParamFld.setToolTipText("<html>Parameter name to color by.<br>Right mouse to use aliases or current fields</html>");
            colorParamFld.setText(symbol.getColorParam());

	    if(!doMultiple()) {
		comps.add(GuiUtils.rLabel("Get Color From:"));
		comps.add(GuiUtils.left(colorParamFld));
	    }


            mappingHolder = new JPanel(new BorderLayout());
            updateMappings();
            JScrollPane mappingScroller = new JScrollPane(mappingHolder);
            mappingScroller.setPreferredSize(new Dimension(400, 250));
            JViewport vp = mappingScroller.getViewport();
            vp.setViewSize(new Dimension(400, 250));

            comps.add(GuiUtils.top(GuiUtils.rLabel("Color Mapping:")));
            comps.add(mappingScroller);
	    if(!doMultiple()) {
		GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
		colorTabbedPane.add("Color From Parameter",
				    GuiUtils.topLeft(GuiUtils.doLayout(comps, 2,
								       GuiUtils.WT_N, GuiUtils.WT_N)));
	    }

        }



        if (symbol.shouldShowScaleGui()) {
            comps = new ArrayList();
            scaleUnitFld =
                GuiUtils.getEditableBox(canvas.getDefaultUnitList(),
                                        symbol.getScaleUnitName());
            if (scaleMaxFld == null) {
                scaleMaxFld     = new JTextField("", 5);
                scaleMinFld     = new JTextField("", 5);
                scaleDataMaxFld = new JTextField("", 5);
                scaleDataMinFld = new JTextField("", 5);
                scaleParamFld   = new ParamField();
                scaleParamFld.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        scaleParamChanged();
                    }
                });
            }
            scaleMinFld.setText(Misc.format(symbol.getScaleRange().getMin()));
            scaleMaxFld.setText(Misc.format(symbol.getScaleRange().getMax()));
            scaleDataMinFld.setText(
                Misc.format(symbol.getScaleDataRange().getMin()));
            scaleDataMaxFld.setText(
                Misc.format(symbol.getScaleDataRange().getMax()));
            scaleParamFld.setText(symbol.getScaleParam());
            comps.add(GuiUtils.rLabel("Scale By Parameter:"));
            comps.add(GuiUtils.left(scaleParamFld));

            comps.add(GuiUtils.rLabel("Data Range:"));
            rightComps = new ArrayList();
            rightComps.add(scaleDataMinFld);
            rightComps.add(scaleDataMaxFld);
            rightComps.add(new JLabel("  Unit:"));
            rightComps.add(scaleUnitFld);
            comps.add(GuiUtils.left(GuiUtils.hbox(rightComps, 5)));


            comps.add(GuiUtils.rLabel("Scale By Factor:"));
            rightComps = new ArrayList();
            rightComps.add(scaleMinFld);
            rightComps.add(scaleMaxFld);
            comps.add(GuiUtils.left(GuiUtils.hbox(rightComps, 5)));

            GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
	    if(!doMultiple()) {
		tabbedPane.add("Scale Size",
			       GuiUtils.topLeft(GuiUtils.doLayout(comps, 2,
								  GuiUtils.WT_N, GuiUtils.WT_N)));

	    }
        }

	
        if (!doMultiple() && symbol.shouldShowRotateGui()) {
            JTabbedPane rotateTab = new JTabbedPane();
            tabbedPane.add("Rotate", rotateTab);
            rotateZGui = new RotateGui(symbol.getRotateZInfo());
            rotateTab.add("Rotate About Z Axis",
                          GuiUtils.topLeft(rotateZGui.contents));

            rotateXGui = new RotateGui(symbol.getRotateXInfo());
            rotateTab.add("Rotate About X Axis",
                          GuiUtils.topLeft(rotateXGui.contents));

            rotateYGui = new RotateGui(symbol.getRotateYInfo());
            rotateTab.add("Rotate About Y Axis",
                          GuiUtils.topLeft(rotateYGui.contents));

        }


	if(!doMultiple()) {
	    symbol.addPropertyTabs(tabbedPane);
	}




        JPanel buttons = GuiUtils.makeButtons(this, null,
                             new String[] { GuiUtils.CMD_APPLY,
                                            GuiUtils.CMD_OK,
                                            GuiUtils.CMD_CANCEL }, new String[] {
                                                "Apply the changes and save the station model",
                "Apply the changes and close the window",
                "Close the window" }, null);

        JPanel contents = GuiUtils.centerBottom(tabbedPane, buttons);
        dialog = GuiUtils.createDialog("", false);
        dialog.setLocation(new Point(200,200));
        setDialogTitle();
        dialog.getContentPane().add(contents);
        dialog.pack();
    }


    /**
     * Se tthe title on the dialog window
     */
    private void setDialogTitle() {
	if(!doMultiple()) {
	    dialog.setTitle(GuiUtils.getApplicationTitle() +"Layout Model Editor - Properties Dialog - "
			    + symbol.getLabel());
	} else {
	    dialog.setTitle(GuiUtils.getApplicationTitle() +"Layout Model Editor - Properties Dialog - Serlected");

	}
    }

    /**
     * Apply the mappings properties. Any ones that don't have a pattern
     * remove.
     */
    private void updateMappings() {
        if (mappingHolder == null) {
            return;
        }
        List mappings     = symbol.getColorMappings();
        List mappingComps = new ArrayList();
        mappingComps.add(new JLabel("Parameter Value"));
        mappingComps.add(new JLabel("Color"));
        if (mappings == null) {
            mappings = new ArrayList();
        }
        colorMappings = new ArrayList(mappings);
        colorMappings.add(new ColorMap());
        colorMappings.add(new ColorMap());
        for (int i = 0; i < colorMappings.size(); i++) {
            ColorMap colorMap = (ColorMap) colorMappings.get(i);
            mappingComps.add(colorMap.getPatternWidget());
            mappingComps.add(colorMap.getColorWidget());
        }
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JPanel mappingPanel = GuiUtils.doLayout(mappingComps, 2,
                                  GuiUtils.WT_YN, GuiUtils.WT_N);
        mappingHolder.removeAll();
        mappingHolder.add(BorderLayout.CENTER, GuiUtils.top(mappingPanel));
        mappingHolder.validate();
    }


    /**
     * Handle when the color by param changed.
     */
    private void colorTableParamChanged() {
        String tmp   = colorTableParamFld.getText().trim();
        Range  range = null;
        Unit   unit  = null;

        try {
            ColorTable colorTable =
                DisplayConventions.getDisplayConventions().getParamColorTable(
                    tmp);
            if (colorTable != null) {
                setTmpColorTable(colorTable);
            }
            range =
                DisplayConventions.getDisplayConventions().getParamRange(tmp,
                    null);
            unit = DisplayConventions.getDisplayConventions().getDisplayUnit(
                tmp, null);
        } catch (Exception exc) {}

        if (unit != null) {
            colorUnitFld.setSelectedItem(unit.toString());
        } else {
            colorUnitFld.setSelectedItem(new TwoFacedObject("Default", null));
        }


        if (range != null) {
            colorMinFld.setText(Misc.format(range.getMin()));
            colorMaxFld.setText(Misc.format(range.getMax()));
        } else {
            colorMinFld.setText(Misc.format(0));
            colorMaxFld.setText(Misc.format(100));
        }
    }



    /**
     * Handle when the scale by param changed.
     */
    private void scaleParamChanged() {
        String tmp   = scaleParamFld.getText().trim();
        Range  range = null;
        Unit   unit  = null;
        try {
            range =
                DisplayConventions.getDisplayConventions().getParamRange(tmp,
                    null);
            unit = DisplayConventions.getDisplayConventions().getDisplayUnit(
                tmp, null);
        } catch (Exception exc) {}

        if (unit != null) {
            scaleUnitFld.setSelectedItem(unit.toString());
        } else {
            scaleUnitFld.setSelectedItem(new TwoFacedObject("Default", null));
        }


        if (range != null) {
            scaleDataMinFld.setText(Misc.format(range.getMin()));
            scaleDataMaxFld.setText(Misc.format(range.getMax()));
        } else {
            scaleDataMinFld.setText(Misc.format(0));
            scaleDataMaxFld.setText(Misc.format(100));
        }
    }






    /**
     * Utility to set the selected value in the box
     *
     * @param param The param name
     * @param params List of params
     * @param box The box
     */
    private void setSelected(String param, List params, JComboBox box) {
        GuiUtils.setListData(box, params);
        if (param == null) {
            return;
        }
        Object current = new TwoFacedObject(param);
        int    index   = params.indexOf(current);
        if (index >= 0) {
            box.setSelectedIndex(index);
        } else {
            box.setSelectedItem(current);
        }
    }

    /**
     * Convert the selected item form a combobox
     *
     * @param paramObject The item
     *
     * @return The string value
     */
    private String convertItem(Object paramObject) {
        if (paramObject instanceof TwoFacedObject) {
            Object tmp = ((TwoFacedObject) paramObject).getId();
            if (tmp == null) {
                return null;
            }
            return tmp.toString();
        } else {
            Object tmp = paramMap.get(paramObject);
            if ((tmp != null) && (tmp instanceof TwoFacedObject)) {
                return ((TwoFacedObject) tmp).getId().toString();
            } else {
                return paramObject.toString().trim();
            }
        }
    }

    /**
     * Set the temp colortable
     *
     * @param ct The new tmp ct
     */
    private void setTmpColorTable(ColorTable ct) {
        tmpColorTable = ct;
        if (tmpColorTable == null) {
            colorLbl.setText("-none-");
            colorBarLbl.setIcon(null);
        } else {
            colorLbl.setText(tmpColorTable.getName());
            colorBarLbl.setIcon(ColorTableCanvas.getIcon(tmpColorTable));
        }
    }

    /**
     * Popup color table menu
     *
     * @param comp Near comp
     */
    private void showColorTableMenu(JComponent comp) {
        ArrayList items    = new ArrayList();
        JMenuItem noneItem = new JMenuItem("-none-");
        noneItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setTmpColorTable(null);
            }
        });
        items.add(noneItem);
        ColorTableManager.getManager().makeColorTableMenu(
            new ObjectListener(null) {
            public void actionPerformed(ActionEvent ae, Object data) {
                tmpColorTable = (ColorTable) data;
                colorLbl.setText(tmpColorTable.getName());
                colorBarLbl.setIcon(ColorTableCanvas.getIcon(tmpColorTable));
            }
        }, items);
        JPopupMenu popup = GuiUtils.makePopupMenu(items);
        Dimension  d     = comp.getSize();
        popup.show(comp, 0, d.height);
    }



    /**
     * Make a TwoFacedObject from a font.  The getId() method of
     * the TwoFacedObject is always guaranteed to be a Font
     * (or <code>null</code>).
     *
     * @param font
     * @return The TFO for the font
     */
    private TwoFacedObject makeFontTFO(Font font) {
        return new TwoFacedObject(((font == null)
                                   ? "Default"
                                   : font.getName()), font);
    }


    /**
     * Holds the gui components for the rotateinfo state. We have 3 of these,
     * for the X, Y and Z axis
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.43 $
     */
    private class RotateGui {

        /** The info */
        RotateInfo info;

        /** Param name for rotate by_ */
        private ParamField paramFld;

        /** min rotate by angle */
        private JTextField minFld;

        /** max rotate by angle */
        private JTextField maxFld;

        /** Min rotate by data */
        private JTextField dataMinFld;

        /** Max rotate by data */
        private JTextField dataMaxFld;

        /** Unit for rotate by */
        private JComboBox unitFld = null;

        /** The gui */
        JPanel contents;

        /**
         * ctor
         *
         * @param info info
         */
        public RotateGui(RotateInfo info) {
            this.info = info;
            List comps = new ArrayList();
            List rightComps;
            unitFld = GuiUtils.getEditableBox(canvas.getDefaultUnitList(),
                    info.getUnitName());
            if (maxFld == null) {
                maxFld     = new JTextField("", 5);
                minFld     = new JTextField("", 5);
                dataMaxFld = new JTextField("", 5);
                dataMinFld = new JTextField("", 5);
                paramFld   =  new ParamField();
                paramFld.setToolTipText(
                                        "<html>Enter a parameter name to rotate by or \"angle:some_angle\" for a fixed rotation<br>Right mouse to add aliases or current fields</html>");
                paramFld.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        paramChanged();
                    }
                });
            }
            minFld.setText(Misc.format(info.getRange().getMin()));
            maxFld.setText(Misc.format(info.getRange().getMax()));
            dataMinFld.setText(Misc.format(info.getDataRange().getMin()));
            dataMaxFld.setText(Misc.format(info.getDataRange().getMax()));
            paramFld.setText(info.getParam());
            comps.add(GuiUtils.rLabel("Rotate By Parameter:"));
            comps.add(GuiUtils.left(paramFld));

            comps.add(GuiUtils.rLabel("Data Range:"));
            rightComps = new ArrayList();
            rightComps.add(dataMinFld);
            rightComps.add(dataMaxFld);
            rightComps.add(unitFld);
            comps.add(GuiUtils.left(GuiUtils.hbox(rightComps, 5)));

            comps.add(GuiUtils.rLabel("Degrees:"));
            rightComps = new ArrayList();
            rightComps.add(minFld);
            rightComps.add(maxFld);
            comps.add(GuiUtils.left(GuiUtils.hbox(rightComps, 5)));

            GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
            contents = GuiUtils.doLayout(comps, 2, GuiUtils.WT_N,
                                         GuiUtils.WT_N);
        }


        /**
         * Handle when the rotate by param changed.
         */
        private void paramChanged() {
            String tmp   = paramFld.getText().trim();
            Range  range = null;
            Unit   unit  = null;
            try {
                range =
                    DisplayConventions.getDisplayConventions().getParamRange(
                        tmp, null);
                unit = DisplayConventions.getDisplayConventions()
                    .getDisplayUnit(tmp, null);
            } catch (Exception exc) {}

            if (unit != null) {
                unitFld.setSelectedItem(unit.toString());
            } else {
                unitFld.setSelectedItem(new TwoFacedObject("Default", null));
            }


            if (range != null) {
                dataMinFld.setText(Misc.format(range.getMin()));
                dataMaxFld.setText(Misc.format(range.getMax()));
            } else {
                dataMinFld.setText(Misc.format(0));
                dataMaxFld.setText(Misc.format(360));
            }
        }


        /**
         * Apply properties
         *
         * @throws Exception On badness
         */
        public void apply() throws Exception {
            info.setParam(paramFld.getText().trim());
            String tmpUnitName =
                TwoFacedObject.getIdString(unitFld.getSelectedItem());
            what = "rotate";
            info.getRange().setMin(Misc.parseNumber(minFld.getText()));
            info.getRange().setMax(Misc.parseNumber(maxFld.getText()));
            info.getDataRange().setMin(
                Misc.parseNumber(dataMinFld.getText()));
            info.getDataRange().setMax(
                Misc.parseNumber(dataMaxFld.getText()));
            if (tmpUnitName != null) {
                Unit realUnit = ucar.visad.Util.parseUnit(tmpUnitName);
            }
            info.setUnitName(tmpUnitName);
        }


    }




}

