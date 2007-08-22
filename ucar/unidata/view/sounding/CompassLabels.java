/*
 * $Id: CompassLabels.java,v 1.21 2005/05/13 18:33:26 jeffmc Exp $
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

import ucar.visad.display.*;

import visad.*;


/**
 * Provides support for compass labels.
 *
 * @author Steven R. Emmerson
 * @version $Id: CompassLabels.java,v 1.21 2005/05/13 18:33:26 jeffmc Exp $
 */
public class CompassLabels extends LineDrawing {

    /**
     * The object to be displayed.
     */
    private FieldImpl field;

    /**
     * The type of the object to be displayed.
     */
    private FunctionType functionType;

    /**
     * The label-size property.
     */
    private float size = 1;

    /**
     * The label control.
     */
    private volatile TextControl textControl;

    /**
     * The name of this displayable.
     */
    private static String uniqueName;

    /**
     * The type of the labels.
     */
    private static TextType textType;

    /**
     * The type of the X axis.
     */
    private static RealType xAxisType;

    /**
     * The type of the Y Axis.
     */
    private static RealType yAxisType;

    /**
     * Constructs from nothing.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public CompassLabels() throws VisADException, RemoteException {

        super("CompassLabels");

        functionType = new FunctionType(
            new RealTupleType(getXAxisType(), getYAxisType()), getTextType());

        ScalarMap textMap = newTextMap();

        addTextMapListener(textMap);
        addScalarMap(textMap);

        ScalarMap xMap = new ScalarMap(getXAxisType(), Display.XAxis);

        xMap.setScaleEnable(false);
        xMap.setRange(-1, 1);
        addScalarMap(xMap);

        ScalarMap yMap = new ScalarMap(getYAxisType(), Display.YAxis);

        yMap.setScaleEnable(false);
        yMap.setRange(-1, 1);
        addScalarMap(yMap);
        setLabels();
    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected CompassLabels(CompassLabels that)
            throws RemoteException, VisADException {

        super(that);

        field        = (FieldImpl) getData();  // from superclass
        functionType = that.functionType;      // immutable object
        size         = that.size;

        ScalarMap textMap = getScalarMap(newTextMap());

        addTextMapListener(textMap);

        /*
         * Because this class needs no control for the X & Y ScalarMap-s, we
         * can simply use the clones created by the superclass "as is".
         */
    }

    /**
     * Create a new ScalarMap for text
     *
     * @return new map
     *
     * @throws VisADException  couldn't create map
     */
    private ScalarMap newTextMap() throws VisADException {
        return new ScalarMap(getTextType(), Display.Text);
    }

    /**
     * Add a listener to the Text ScalarMap
     *
     * @param textMap   map to listen to
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
                }
            }

            public void mapChanged(ScalarMapEvent event) {}  // ignore
        });
    }

    /**
     * Returns a name unique to this class.
     * @return                  A name unique to this class.
     * @throws VisADException   VisAD failure.
     */
    private static String getUniqueName() throws VisADException {

        if (uniqueName == null) {
            synchronized (CompassLabels.class) {
                if (uniqueName == null) {
                    uniqueName = CompassLabels.class.getName().replace('.',
                            '_');
                }
            }
        }

        return uniqueName;
    }

    /**
     * Returns the type of the labels.
     * @return                  The type of the labels.
     * @throws VisADException   VisAD failure.
     */
    private static TextType getTextType() throws VisADException {

        if (textType == null) {
            synchronized (CompassLabels.class) {
                if (textType == null) {
                    textType = TextType.getTextType(getUniqueName()
                                                    + "_CompassLabel");
                }
            }
        }

        return textType;
    }

    /**
     * Returns the type of the X axis.
     * @return                  The type of the X axis.
     * @throws VisADException   VisAD failure.
     */
    private static RealType getXAxisType() throws VisADException {

        if (xAxisType == null) {
            synchronized (CompassLabels.class) {
                if (xAxisType == null) {
                    xAxisType = RealType.getRealType(getUniqueName()
                                                     + "_XAxis");
                }
            }
        }

        return xAxisType;
    }

    /**
     * Returns the type of the Y axis.
     * @return                  The type of the Y axis.
     * @throws VisADException   VisAD failure.
     */
    private static RealType getYAxisType() throws VisADException {

        if (yAxisType == null) {
            synchronized (CompassLabels.class) {
                if (yAxisType == null) {
                    yAxisType = RealType.getRealType(getUniqueName()
                                                     + "_YAxis");
                }
            }
        }

        return yAxisType;
    }

    /**
     * Sets the label-size property.
     * @param size              The new value (1 is normal).
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setSize(double size) throws VisADException, RemoteException {

        if (textControl != null) {
            textControl.setSize(size);
        }

        this.size = (float) size;

        setLabels();
    }

    /**
     * Indicates if this instance is semantically identical to another object
     * @param obj               The othere object.
     * @return                  <code>true</code> if and only if this instance
     *                          is semantically identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof CompassLabels)) {
            equals = false;
        } else {
            CompassLabels that = (CompassLabels) obj;

            equals = (that == this)
                     || ((size == that.size) && field.equals(that.field)
                         && super.equals(that));
        }

        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {
        return new Float(size).hashCode() ^ field.hashCode()
               ^ super.hashCode();
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
        return new CompassLabels(this);
    }

    /**
     * Sets the labels.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setLabels() throws VisADException, RemoteException {

        field = new FieldImpl(functionType,
                              new Gridded2DSet(functionType.getDomain(),
                                               new float[][] {
            { -.015f * size, -.015f * size, 1 + .08f * size,
              -1 - .12f * size },
            { 1 + .06f * size, -1 - .12f * size, -.025f * size,
              -.025f * size }
        }, 4));

        TextType textType = getTextType();

        field.setSamples(new Text[]{ new Text(textType, "N"),
                                     new Text(textType, "S"),
                                     new Text(textType, "E"),
                                     new Text(textType, "W") }, false);
        setData(field);
    }
}







