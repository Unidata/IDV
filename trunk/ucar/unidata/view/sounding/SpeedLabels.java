/*
 * $Id: SpeedLabels.java,v 1.21 2005/05/13 18:33:39 jeffmc Exp $
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

import java.util.Arrays;

import ucar.visad.display.*;
import ucar.visad.quantities.*;

import visad.*;


/**
 * Provides support for speed labels for a 3D wind hodograph.
 *
 * @author Steven R. Emmerson
 * @version $Id: SpeedLabels.java,v 1.21 2005/05/13 18:33:39 jeffmc Exp $
 */
public class SpeedLabels extends PolarLineDrawing {

    /**
     * The data object to be displayed.
     */
    private FieldImpl field;

    /**
     * The type of the function.
     */
    private FunctionType functionType;

    /**
     * The default speed unit.
     */
    private Unit defaultSpeedUnit;

    /**
     * The size of the labels.
     */
    private float size = 1;

    /**
     * The format for the labels.
     */
    private NumberFormat format = NumberFormat.getInstance();

    /**
     * The type of the labels.
     */
    private TextType textType;

    /**
     * The label control.
     */
    private volatile TextControl textControl;

    /**
     * Constructs from nothing.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public SpeedLabels() throws RemoteException, VisADException {

        super("SpeedLabels", PolarHorizontalWind.getRealTupleType());

        textType = TextType.getTextType("SpeedLabel");
        functionType =
            new FunctionType(PolarHorizontalWind.getRealTupleType(),
                             textType);
        defaultSpeedUnit = Speed.getRealType().getDefaultUnit();

        ScalarMap textMap = newTextMap();

        addTextMapListener(textMap);
        addScalarMap(textMap);
    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected SpeedLabels(SpeedLabels that)
            throws RemoteException, VisADException {

        super(that);

        field            = (FieldImpl) that.getData();
        functionType     = that.functionType;      // immutable object
        defaultSpeedUnit = that.defaultSpeedUnit;  // immutable object
        size             = that.size;
        format           = (NumberFormat) that.format.clone();
        textType         = that.textType;          // immutable object

        addTextMapListener(getScalarMap(newTextMap()));
    }

    /**
     * Create a new ScalarMap for text.
     *
     * @return new Text ScalarMap
     *
     * @throws VisADException   problem creating ScalarMap
     */
    private ScalarMap newTextMap() throws VisADException {
        return new ScalarMap(textType, Display.Text);
    }

    /**
     * Add a listener to the text ScalarMap
     *
     * @param textMap  map to add listener to
     */
    private void addTextMapListener(final ScalarMap textMap) {

        textMap.addScalarMapListener(new ScalarMapListener() {

            public void controlChanged(ScalarMapControlEvent event)
                    throws VisADException, RemoteException {

                int id = event.getId();

                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    textControl = (TextControl) textMap.getControl();

                    textControl.setSize(size);
                }
            }

            public void mapChanged(ScalarMapEvent event) {}  // ignore
        });
    }

    /**
     * Sets the size of the labels.
     * @param size              The size of the labels.  1 is normal.
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
     * Sets the values for the labels.
     * @param speedSet          The set of speeds to be labeled.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setLabels(Gridded1DSet speedSet)
            throws VisADException, RemoteException {

        float[] spds   = speedSet.getSamples()[0];
        float[] speeds = new float[spds.length - 1];

        System.arraycopy(spds, 0, speeds, 0, speeds.length);

        float[] directions = new float[speeds.length];

        Arrays.fill(directions, 270f);

        float[][] domainSamples = {
            defaultSpeedUnit.toThis(speeds, speedSet.getSetUnits()[0]),
            directions
        };

        field = new FieldImpl(functionType,
                              new Gridded2DSet(functionType.getDomain(),
                                               domainSamples,
                                               domainSamples[0].length));

        Text[]   labels   = new Text[speeds.length];
        int      maxWidth = 0;
        String[] strings  = new String[speeds.length];

        for (int i = speeds.length; --i >= 0; ) {
            String label = format.format(speeds[i]).trim();
            int    width = label.length();

            if (width > maxWidth) {
                maxWidth = width;
            }

            strings[i] = label;
        }

        if ((labels == null) || (labels.length != speeds.length)) {
            labels = new Text[speeds.length];
        }

        StringBuffer labelBuf = new StringBuffer(maxWidth);

        for (int i = speeds.length; --i >= 0; ) {
            String label;

            for (int j = strings[i].length(); j < maxWidth; ++j) {
                labelBuf.append(" ");
            }

            labelBuf.append(strings[i]);

            label = labelBuf.toString();

            labelBuf.setLength(0);

            labels[i] = new Text(textType, label);
        }

        field.setSamples(labels, false);
        setData(field);
    }

    /**
     * Indicates if this instance is semantically identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is semantically identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof SpeedLabels)) {
            equals = false;
        } else {
            SpeedLabels that = (SpeedLabels) obj;

            equals = (that == this)
                     || ((size == that.size) && format.equals(that.format)
                         && field.equals(that.field) && super.equals(that));
        }

        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {
        return new Float(size).hashCode() ^ format.hashCode()
               ^ field.hashCode() ^ super.hashCode();
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws VisADException, RemoteException {
        return new SpeedLabels(this);
    }
}







