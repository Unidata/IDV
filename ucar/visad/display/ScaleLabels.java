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

package ucar.visad.display;


import ucar.visad.display.LineDrawing;

import visad.*;



import java.awt.Component;

import java.beans.PropertyVetoException;

import java.rmi.RemoteException;

import java.text.NumberFormat;


/**
 * Provides support for labeling the scale of an axis.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.9 $
 */
public class ScaleLabels extends LineDrawing {

    /**
     * The X alignment of the labels.
     */
    private float xAlignment = Component.LEFT_ALIGNMENT;

    /**
     * The label values.
     */
    private float[] values;

    /**
     * The size of the labels.
     */
    private float size = 1;

    /**
     * The RealType of the axis.
     */
    private RealType realType;

    /**
     * The labels.
     */
    private Text[] labels;

    /**
     * The position of the labels.
     */
    private float[] positionValues;

    /**
     * The type of the label text.
     */
    private TextType textType;

    /**
     * The template, text ScalarMap.
     */
    private ScalarMap templateTextMap;

    /**
     * The format for the labels.
     */
    private NumberFormat format = NumberFormat.getInstance();

    /**
     * Constructs from a name and a axis RealType.
     * @param name              The name for this instance.
     * @param realType          The type of the axis.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public ScaleLabels(String name, RealType realType)
            throws RemoteException, VisADException {

        super(name);

        this.realType = realType;
        textType      = new TextType(name + "ScaleLabel");

        addScalarMap(new ScalarMap(textType, Display.Text));
    }

    /**
     * Constructs from another instance.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected ScaleLabels(ScaleLabels that)
            throws RemoteException, VisADException {

        super(that);

        xAlignment     = that.xAlignment;
        values         = (float[]) that.values.clone();
        size           = that.size;
        realType       = that.realType;                 // immutable object
        labels         = (Text[]) that.labels.clone();  // immutable objects
        positionValues = (float[]) that.positionValues.clone();
        textType       = that.textType;                 // immutable object

        addScalarMap(new ScalarMap(textType, Display.Text));

        format = (NumberFormat) that.format.clone();
    }

    /**
     * Sets the size of the labels.
     * method.
     * @param size              The size of the labels (1 = normal).
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #setSize(TextControl)
     */
    public void setSize(double size) throws VisADException, RemoteException {
        this.size = (float) size;
    }

    /**
     * Sets the size of text in a TextControl according to the size property.
     * @param control           The TextControl.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setSize(TextControl control)
            throws VisADException, RemoteException {
        control.setSize(size);
    }

    /**
     * Sets the X-alignment of the labels.
     * @param alignment         The X-alignment of the labels.  One of
     *                          Component.CENTER_ALIGNMENT,
     *                          Component.RIGHT_ALIGNMENT, or
     *                          Component.LEFT_ALIGNMENT.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #setAlignment(TextControl)
     */
    public void setXAlignment(float alignment)
            throws VisADException, RemoteException {

        xAlignment = alignment;

        setLabels();
    }

    /**
     * Sets the alignment of text in a TextControl according to the alignment
     * property.
     * @param control           The TextControl.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setAlignment(TextControl control)
            throws VisADException, RemoteException {

        if (xAlignment == Component.CENTER_ALIGNMENT) {
            control.setCenter(true);
        }
    }

    /**
     * Sets the format of the labels.
     * @param format            The format of the labels.
     * @see java.text.NumberFormat
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setFormat(NumberFormat format)
            throws RemoteException, VisADException {

        this.format = format;

        setLabels();
    }

    /**
     * Sets the labels from a set of values.
     * @param set               The set of values.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void set(Gridded1DSet set) throws RemoteException, VisADException {

        values         = null;
        labels         = null;
        positionValues = null;

        float[] vals = set.getSamples(false)[0];

        setPositionValues(realType.getDefaultUnit().toThis(vals,
                set.getSetUnits()[0]));
        setLabelValues(vals);
    }

    /**
     * Sets the labels from an array of values.
     * @param values            The array of values.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setLabelValues(float[] values)
            throws VisADException, RemoteException {

        this.values = (float[]) values.clone();

        setLabels();
    }

    /**
     * Sets the labels from internal information.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setLabels() throws VisADException, RemoteException {

        if (values != null) {
            int      maxWidth = 0;
            String[] strings  = new String[values.length];

            for (int i = values.length; --i >= 0; ) {
                String label = format.format(values[i]).trim();
                int    width = label.length();

                if (width > maxWidth) {
                    maxWidth = width;
                }

                strings[i] = label;
            }

            if ((labels == null) || (labels.length != values.length)) {
                labels = new Text[values.length];
            }

            StringBuffer labelBuf = new StringBuffer(maxWidth);

            for (int i = values.length; --i >= 0; ) {
                String label;

                if (xAlignment != Component.RIGHT_ALIGNMENT) {
                    label = strings[i];
                } else {
                    for (int j = strings[i].length(); j < maxWidth; ++j) {
                        labelBuf.append(" ");
                    }

                    labelBuf.append(strings[i]);

                    label = labelBuf.toString();

                    labelBuf.setLength(0);
                }

                labels[i] = new Text(textType, label);
            }

            setData(computeData());
        }
    }

    /**
     * Sets the position of the labels from an array of positions.
     * @param positionValues    The position for the labels along the axis.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setPositionValues(float[] positionValues)
            throws VisADException, RemoteException {

        this.positionValues = (float[]) positionValues.clone();

        setData(computeData());
    }

    /**
     * Creates the VisAD Data object corresponding to the labels.
     * @return                  The VisAD Data object corresponding to the
     *                          labels.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private Data computeData() throws VisADException, RemoteException {

        FieldImpl field;

        if ((realType == null) || (positionValues == null)
                || (labels == null)) {
            field = null;
        } else {
            field = new FieldImpl(new FunctionType(realType, textType),
                                  new Gridded1DSet(realType, new float[][] {
                positionValues
            }, positionValues.length));

            field.setSamples(labels, false);
        }

        return field;
    }

    /**
     * Adds the non-spatial ScalarMap-s of this instance to a VisAD display.
     * @param display           The VisAD display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void addScalarMaps(LocalDisplay display)
            throws VisADException, RemoteException {

        ScalarMap textMap = new ScalarMap(textType, Display.Text);

        display.addMap(textMap);

        TextControl control = (TextControl) textMap.getControl();

        setSize(control);
        setAlignment(control);
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new ScaleLabels(this);
    }
}
