/*
 * $Id: ScaleLabels.java,v 1.22 2005/05/13 18:33:37 jeffmc Exp $
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

package ucar.unidata.view.sounding;



import java.awt.Component;

import java.rmi.RemoteException;

import java.text.NumberFormat;

import ucar.visad.display.*;

import visad.*;


/**
 * Provides support for displaying scale-labels along an axis.
 *
 * @author Steven R. Emmerson
 * @version $Id: ScaleLabels.java,v 1.22 2005/05/13 18:33:37 jeffmc Exp $
 */
public abstract class ScaleLabels extends LineDrawing {

    /**
     * The X-alignment.
     */
    private float xAlignment = Component.LEFT_ALIGNMENT;

    /**
     * The label values.
     */
    private float[] values;

    /**
     * The label size.
     */
    private float size = 1;

    /**
     * The type of the axis.
     */
    private RealType realType;

    /**
     * The labels.
     */
    private Text[] labels;

    /**
     * The positions of the labels.
     */
    private float[] positionValues;

    /**
     * The type of the labels.
     */
    private TextType textType;

    /**
     * The label control.
     */
    private TextControl textControl;

    /**
     * The format for the numeric values.
     */
    private NumberFormat format = NumberFormat.getInstance();

    /**
     * Constructs from a name for the displayable and the type of the axis.
     * @param name              The name for the displayable.
     * @param axisType          The type of the axis.  May be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected ScaleLabels(String name, RealType axisType)
            throws RemoteException, VisADException {

        super(name);

        textType = TextType.getTextType(name + "ScaleLabel");
        realType = axisType;

        ScalarMap textMap = newTextMap(textType);

        addTextMapListener(textMap);
        addScalarMap(textMap);
    }

    /**
     * Constructs from another instance.
     *
     * @param that              The other instance.
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    protected ScaleLabels(ScaleLabels that)
            throws RemoteException, VisADException {

        super(that);

        xAlignment     = that.xAlignment;
        values         = (float[]) that.values.clone();
        size           = that.size;
        realType       = that.realType;  // immutable
        labels         = (Text[]) that.labels.clone();
        positionValues = (float[]) that.positionValues.clone();
        textType       = that.textType;  // immutable

        addTextMapListener(getScalarMap(newTextMap(that.textType)));

        format = (NumberFormat) that.format.clone();
    }

    /**
     * Create a new ScalarMap for text
     *
     * @param textType  type for text
     * @return new ScalarMap
     *
     * @throws VisADException   problem creating ScalarMap
     */
    private static ScalarMap newTextMap(TextType textType)
            throws VisADException {
        return new ScalarMap(textType, Display.Text);
    }

    /**
     * Add a new listener for the ScalarMap
     *
     * @param textMap  ScalarMap for text
     */
    private void addTextMapListener(final ScalarMap textMap) {

        textMap.addScalarMapListener(new ScalarMapListener() {

            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {

                int id = event.getId();

                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    textControl = (TextControl) textMap.getControl();

                    textControl.setSize(size);

                    if (xAlignment == Component.CENTER_ALIGNMENT) {
                        textControl.setCenter(true);
                    }
                }
            }

            public void mapChanged(ScalarMapEvent event) {}  // ignore
        });
    }

    /**
     * Sets the size of the labels.
     * @param size              The size of the labels (1 is normal).
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setSize(double size) throws VisADException, RemoteException {

        this.size = (float) size;

        if (textControl != null) {
            textControl.setSize(size);
        }
    }

    /**
     * Sets the X-alignment of the labels.
     * @param alignment         The X-alignment of the labels.  One of
     *                          Component.LEFT_ALIGNMENT,
     *                          Component.RIGHT_ALIGNMENT.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setXAlignment(float alignment)
            throws VisADException, RemoteException {

        xAlignment = alignment;

        if ((alignment == Component.CENTER_ALIGNMENT)
                && (textControl != null)) {
            textControl.setCenter(true);
        } else {
            setLabels();
        }
    }

    /**
     * Sets the type of the axis.
     * @param realType          The type of the axis.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRealType(RealType realType)
            throws VisADException, RemoteException {
        this.realType = realType;
    }

    /**
     * Sets the format for the labels.
     * @param format            The format for the labels.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setFormat(NumberFormat format)
            throws RemoteException, VisADException {

        this.format = format;

        setLabels();
    }

    /**
     * Sets the values for the labels.
     * @param values            The values for the labels.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setLabelValues(float[] values)
            throws VisADException, RemoteException {

        this.values = (float[]) values.clone();

        setLabels();
    }

    /**
     * Sets the labels.
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

            setData();
        }
    }

    /**
     * Sets the position of the labels.
     * @param values            The position of the labels.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setPositionValues(float[] values)
            throws VisADException, RemoteException {

        positionValues = (float[]) values.clone();

        setData();
    }

    /**
     * Set the data for the labels
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private void setData() throws VisADException, RemoteException {

        Data data = computeData();

        if (data != null) {
            setData(data);
        }
    }

    /**
     * Computes the label data.
     * @return                  The label data.  Will be <code>null</code> if it
     *                          can't yet be computed.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private Data computeData() throws VisADException, RemoteException {

        FieldImpl field;

        if ((realType == null) || (positionValues == null)
                || (labels == null)
                || (positionValues.length != labels.length)) {
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
}







