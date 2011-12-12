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

package ucar.visad;


import org.w3c.dom.Element;

import ucar.nc2.iosp.mcidas.McIDASAreaProjection;

import ucar.unidata.data.point.PointOb;

import ucar.unidata.geoloc.ProjectionRect;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlDelegate;
import ucar.unidata.xml.XmlDelegateImpl;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlObjectFactory;
import ucar.unidata.xml.XmlPersistable;
import ucar.unidata.xml.XmlUtil;

import visad.*;

import visad.bom.WindPolarCoordinateSystem;

import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.GRIDCoordinateSystem;
import visad.data.vis5d.Vis5DCoordinateSystem;
import visad.data.visad.BinaryReader;

import visad.data.visad.BinaryWriter;

import visad.georef.*;

import visad.meteorology.*;

import java.io.ByteArrayInputStream;

import java.io.ByteArrayOutputStream;

import java.util.ArrayList;
import java.util.List;


import javax.media.j3d.*;

import javax.vecmath.*;


/**
 * A class for supporting XML delegates for VisAD objects.
 *
 * @author MetApps Development Team
 * @version $Revision: 1.24 $ $Date: 2007/07/19 14:06:44 $
 */


public class VisADPersistence {

    /** attribute for scaled unit amount */
    public static final String ATTR_AMOUNT = "amount";

    /** attribute for scaled unit name */
    public static final String ATTR_NAME = "name";

    /** list of delegates */
    private static List delegates;

    /** list of classes */
    private static List classes;


    /** list of delegates */
    private static List highPriorityDelegates;

    /** list of classes */
    private static List highPriorityClasses;

    /** encoder for encoding */
    XmlEncoder myEncoder;

    /**
     * Construct a new VisADPersistence
     *
     * @param encoder  encoder for encoding
     *
     */
    private VisADPersistence(XmlEncoder encoder) {
        this.myEncoder = encoder;
        encoder.registerClassName("ucar.unidata.util.PropertyFilter",
                                  ucar.unidata.ui.PropertyFilter.class);
        init();
    }

    /**
     * Add a delegate to the list
     *
     * @param c   Class for delegate
     * @param delegate   delegate to add
     */
    private static void addDelegate(Class c, XmlDelegate delegate) {
        classes.add(c);
        delegates.add(delegate);
    }



    /**
     * Add a high priority delegate to the list. These take precedence over any other delegates.
     *
     * @param c   Class for delegate
     * @param delegate   delegate to add
     */
    private static void addHighPriorityDelegate(Class c,
            XmlDelegate delegate) {
        highPriorityClasses.add(c);
        highPriorityDelegates.add(delegate);
    }






    /**
     * Initialize a new VisADPersistence with the encoder
     *
     * @param encoder   encoder to us
     */
    public static void init(XmlEncoder encoder) {
        new VisADPersistence(encoder);
    }

    /**
     * Initialize the class
     */
    private void init() {
        if (delegates == null) {
            initDelegates();
        }
        for (int i = 0; i < delegates.size(); i++) {
            myEncoder.addDelegateForClass((Class) classes.get(i),
                                          (XmlDelegate) delegates.get(i));
        }

        for (int i = 0; i < highPriorityDelegates.size(); i++) {
            myEncoder.addHighPriorityDelegateForClass(
                (Class) highPriorityClasses.get(i),
                (XmlDelegate) highPriorityDelegates.get(i));
        }
    }

    /**
     * Initialize the delegates
     */
    private static void initDelegates() {

        delegates             = new ArrayList();
        classes               = new ArrayList();
        highPriorityDelegates = new ArrayList();
        highPriorityClasses   = new ArrayList();

        //This needs to be before the Rectangle2D
        addHighPriorityDelegate(ProjectionRect.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                ProjectionRect r = (ProjectionRect) o;

                List args = Misc.newList(new Double(r.getMinX()),
                                         new Double(r.getMinY()),
                                         new Double(r.getMaxX()),
                                         new Double(r.getMaxY()));
                List types = Misc.newList(Double.TYPE, Double.TYPE,
                                          Double.TYPE, Double.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });




        addDelegate(Real.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Real r = (Real) o;
                List args = Misc.newList(r.getType(),
                                         new Double(r.getValue()),
                                         r.getUnit(), r.getError());
                List types = Misc.newList(RealType.class, Double.TYPE,
                                          getUnitClass(r.getUnit()),
                                          ErrorEstimate.class);
                return e.createObjectConstructorElement(o, args, types);
            }
        });


        addDelegate(SetType.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                SetType s    = (SetType) o;
                List    args = Misc.newList(s.getDomain());
                return e.createObjectConstructorElement(o, args);
            }
        });

        addDelegate(ErrorEstimate.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                ErrorEstimate r    = (ErrorEstimate) o;
                Unit          unit = r.getUnit();
                List args = Misc.newList(new Double(r.getErrorValue()),
                                         new Double(r.getMean()),
                                         new Long(r.getNumberNotMissing()),
                                         unit);
                List types = Misc.newList(Double.TYPE, Double.TYPE,
                                          Long.TYPE,
                                          getUnitClass(r.getUnit()));
                return e.createObjectConstructorElement(o, args, types);
            }
        });

        addDelegate(DisplayRealType.class, new XmlDelegateImpl() {
            //The DisplayRealType delegate ends up writing out an object
            //tag for the RealType that contains
            //an ArrayList object. This ArrayList  
            //holds name, range, default value, whether is single,
            //unit, set and attrmask
            public Element createElement(XmlEncoder e, Object o) {
                DisplayRealType d = (DisplayRealType) o;
                double[] range = new double[] { Double.NEGATIVE_INFINITY,
                        Double.POSITIVE_INFINITY };
                boolean isRange = d.getRange(range);
                List    args    = Misc.newList(new Object[] {
                    d.getName(), new Boolean(d.isSingle()),
                    new Double(range[0]), new Double(range[1]),
                    new Double(d.getDefaultValue()), d.getDefaultUnit()
                });
                Element objectElement = e.createObjectElement(o.getClass());
                Element listElement   = e.createElement(args);
                objectElement.appendChild(listElement);
                return objectElement;
            }

            public Object createObject(XmlEncoder e, Element o) {
                Element   listElement = XmlUtil.getFirstChild(o);
                ArrayList args = (ArrayList) e.createObject(listElement);
                String    name        = (String) args.get(0);
                // in case no display has been instantiated, load the
                // Display class so intrinsics will show up.
                DisplayRealType bogus = Display.Animation;
                DisplayRealType drt =
                    (DisplayRealType) DisplayRealType.getRealTypeByName(name);
                if (drt == null) {
                    if (name.startsWith("Display")) {
                        for (int i = 0; i < Display.DisplayRealArray.length;
                                i++) {
                            if (Display.DisplayRealArray[i].getName().equals(
                                    name)) {
                                drt = Display.DisplayRealArray[i];
                            }
                        }
                    } else {
                        boolean single =
                            ((Boolean) args.get(1)).booleanValue();
                        double lo  = ((Double) args.get(2)).doubleValue();
                        double hi  = ((Double) args.get(3)).doubleValue();
                        double def = ((Double) args.get(3)).doubleValue();
                        try {
                            drt = new DisplayRealType((String) args.get(0),
                                    single, lo, hi, def, (Unit) args.get(4));
                        } catch (VisADException excp) {
                            drt = null;
                        }

                    }
                }
                return drt;
            }
        });

        addDelegate(RealType.class, new XmlDelegateImpl() {

            //The RealType delegate ends up writing out an object
            //tag for the RealType that contains
            //an ArrayList object. This ArrayList  holds name, unit, set and attrmask
            public Element createElement(XmlEncoder e, Object o) {
                RealType r = (RealType) o;
                List args = Misc.newList(r.getOriginalName(),
                                         r.getDefaultUnit(),
                                         r.getDefaultSet(),
                                         new Integer(r.getAttributeMask()));
                Element objectElement = e.createObjectElement(o.getClass());
                Element listElement   = e.createElement(args);
                objectElement.appendChild(listElement);
                return objectElement;
            }

            public Object createObject(XmlEncoder e, Element o) {

                Element   listElement = XmlUtil.getFirstChild(o);
                ArrayList args = (ArrayList) e.createObject(listElement);
                // in case no display has been instantiated, load the
                // RealType class so intrinsics will show up.
                RealType bogus = RealType.Time;
                if (args.size() == 3) {
                    XmlEncoder.debug = true;
                    args             =
                        (ArrayList) e.createObject(listElement);
                    XmlEncoder.debug = false;
                }
                if (args.size() != 4) {
                    throw new IllegalArgumentException(
                        "Creating RealType. Incorrect number of arguments in list: "
                        + args.size() + "\n"
                        + ucar.unidata.xml.XmlUtil.toString(o));
                }
                int attrMask = ((Integer) args.get(3)).intValue();;
                return RealType.getRealType((String) args.get(0),
                                            (Unit) args.get(1),
                                            (Set) args.get(2), attrMask);
            }
        });

        addDelegate(ErrorEstimate.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                ErrorEstimate r = (ErrorEstimate) o;
                List args = Misc.newList(new Double(r.getErrorValue()),
                                         new Double(r.getMean()),
                                         new Long(r.getNumberNotMissing()),
                                         r.getUnit());
                List types = Misc.newList(Double.TYPE, Double.TYPE,
                                          Long.TYPE,
                                          getUnitClass(r.getUnit()));
                return e.createObjectConstructorElement(o, args, types);
            }
        });

        addDelegate(Text.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Text t    = (Text) o;
                List args = Misc.newList(t.getType(), t.getValue());
                return e.createObjectConstructorElement(o, args);
            }
        });


        addDelegate(TextType.class, new XmlDelegateImpl() {

            //The TextType delegate ends up writing out an object
            //tag for the TextType that contains
            //an ArrayList object. This ArrayList  holds name, unit, set and attrmask
            public Element createElement(XmlEncoder e, Object o) {
                TextType t             = (TextType) o;
                Element  objectElement = e.createObjectElement(o.getClass());
                objectElement.appendChild(e.createElement(t.getName()));
                return objectElement;
            }

            public Object createObject(XmlEncoder e, Element o) {
                String name =
                    (String) e.createObject(XmlUtil.getFirstChild(o));
                return TextType.getTextType(name);
            }
        });


        addDelegate(ScaledUnit.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                ScaledUnit r             = (ScaledUnit) o;
                Element    objectElement =
                    e.createObjectElement(o.getClass());
                objectElement.setAttribute(ATTR_AMOUNT, r.getAmount() + "");
                objectElement.setAttribute(ATTR_NAME, r.toString());
                Element childElement = e.createElement(r.getUnit());
                objectElement.appendChild(childElement);
                return objectElement;
            }

            public Object createObject(XmlEncoder e, Element o) {
                try {
                    Object object = e.createObject(XmlUtil.getFirstChild(o));
                    Unit   scaledUnit = null;
                    if (object instanceof String) {
                        //Handle old bundles that have a string "scale unit"
                        String s = (String) object;
                        try {
                            String[] toks = StringUtil.split(s, " ", 2);
                            if ((toks == null) || (toks.length == 1)) {
                                Unit unit = createUnit(e, o);
                                return unit;
                            }
                            String identifier = toks[1];
                            double amount = new Double(toks[0]).doubleValue();
                            Unit   theUnit    = Util.parseUnit(identifier);
                            scaledUnit = ScaledUnit.create(amount, theUnit);
                        } catch (Exception exc) {
                            System.err.println("error creating scaled unit:"
                                    + exc);
                            Unit unit = createUnit(e, o);
                            return unit;
                        }
                    } else {
                        Unit subUnit = (Unit) object;
                        double amount = XmlUtil.getAttribute(o, ATTR_AMOUNT,
                                            (double) 1.0);
                        scaledUnit = ScaledUnit.create(amount, subUnit);
                    }
                    if (scaledUnit == null) {
                        return scaledUnit;
                    }
                    String name = o.getAttribute(ATTR_NAME);
                    if ((name != null) && (name.length() > 0)) {
                        scaledUnit = scaledUnit.clone(name);
                    }
                    return scaledUnit;
                } catch (Exception exc) {
                    System.err.println("Error creating unit:"
                                       + XmlUtil.toString(o));
                    exc.printStackTrace();
                    return null;
                }
            }
        });




        addDelegate(Unit.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Unit    r             = (Unit) o;
                Element objectElement = e.createObjectElement(o.getClass());
                String  unitString    = null;
                String  unitName      = null;
                if (r == CommonUnit.dimensionless) {
                    unitString = "dimensionless";
                } else {
                    unitString = r.getDefinition();
                    unitName   = r.toString();
                }
                Element childElement = e.createElement(unitString);
                if (unitName != null) {
                    objectElement.setAttribute(ATTR_NAME, unitName);
                }
                objectElement.appendChild(childElement);
                return objectElement;
            }


            public Object createObject(XmlEncoder e, Element o) {
                try {
                    return createUnit(e, o);
                } catch (Exception exc) {
                    System.err.println("Error creating unit " + exc);
                    exc.printStackTrace();
                    return null;
                }
            }
        });

        addDelegate(DateTime.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                DateTime r     = (DateTime) o;
                List     args  = Misc.newList(new Double(r.getValue()));
                List     types = Misc.newList(Double.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });

        addDelegate(McIDASAreaProjection.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                McIDASAreaProjection ac  = (McIDASAreaProjection) o;
                int[]                dir = ac.getDirBlock();
                List args = Misc.newList(dir, ac.getNavBlock(),
                                         ac.getAuxBlock());
                List types = Misc.newList(null, null, dir.getClass());
                return e.createObjectConstructorElement(o, args, types);
            }
        });

        addDelegate(AREACoordinateSystem.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                AREACoordinateSystem ac  = (AREACoordinateSystem) o;
                int[]                dir = ac.getDirBlock();
                List args = Misc.newList(dir, ac.getNavBlock(),
                                         ac.getAuxBlock(),
                                         new Boolean(ac.getUseSpline()));
                List types = Misc.newList(null, null, dir.getClass(),
                                          Boolean.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });

        addDelegate(ScalarMap.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                ScalarMap s = (ScalarMap) o;
                List args = Misc.newList(s.getScalar(), s.getDisplayScalar());
                return e.createObjectConstructorElement(o, args);
            }
        });

        addDelegate(EarthLocationLite.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                try {
                    EarthLocationLite nlt  = (EarthLocationLite) o;
                    Real              lat  = nlt.getLatitude();
                    Real              lon  = nlt.getLongitude();
                    Real              alt  = nlt.getAltitude();
                    List              args = Misc.newList(lat, lon, alt);
                    return e.createObjectConstructorElement(o, args);
                } catch (Exception exc) {
                    System.err.println("Error persisting NamedLocationTuple:"
                                       + exc);
                    return null;

                }
            }
        });



        //TODO: Do we need the other tuple delegates to be before the RealTuple delegate
        addDelegate(RealTuple.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                try {
                    RealTuple rt = (RealTuple) o;
                    List args = Misc.newList(rt.getType(),
                                             rt.getRealComponents(),
                                             rt.getCoordinateSystem());
                    return e.createObjectConstructorElement(o, args);
                } catch (Exception exc) {
                    System.err.println("Error persisting RealTuple:" + exc);
                    return null;

                }
            }
        });

        addDelegate(NamedLocationTuple.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                try {
                    NamedLocationTuple nlt = (NamedLocationTuple) o;
                    List args = Misc.newList(nlt.getIdentifier(),
                                             nlt.getLatitude(),
                                             nlt.getLongitude(),
                                             nlt.getAltitude());
                    return e.createObjectConstructorElement(o, args);
                } catch (Exception exc) {
                    System.err.println("Error persisting NamedLocationTuple:"
                                       + exc);
                    return null;

                }
            }
        });

        addDelegate(LatLonTuple.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                try {
                    LatLonTuple nlt = (LatLonTuple) o;
                    List args = Misc.newList(nlt.getLatitude(),
                                             nlt.getLongitude());
                    return e.createObjectConstructorElement(o, args);
                } catch (Exception exc) {
                    System.err.println("Error persisting LatLonTuple:" + exc);
                    return null;

                }
            }
        });

        addDelegate(EarthLocationTuple.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                try {
                    EarthLocationTuple nlt = (EarthLocationTuple) o;
                    List args = Misc.newList(nlt.getLatitude(),
                                             nlt.getLongitude(),
                                             nlt.getAltitude());
                    return e.createObjectConstructorElement(o, args);
                } catch (Exception exc) {
                    System.err.println("Error persisting NamedLocationTuple:"
                                       + exc);
                    return null;

                }
            }
        });



        addDelegate(UnionSet.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                UnionSet           unionSet = (UnionSet) o;
                visad.SampledSet[] sets     = unionSet.getSets();
                List args = Misc.newList(unionSet.getType(), sets);
                return e.createObjectConstructorElement(o, args);
            }

        });


        addDelegate(RealTupleType.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                try {
                    RealTupleType rt  = (RealTupleType) o;
                    MathType[]    mt  = rt.getComponents();
                    RealType[]    rta = new RealType[mt.length];
                    for (int i = 0; i < mt.length; i++) {
                        rta[i] = (RealType) mt[i];
                    }

                    List args = Misc.newList(rta, rt.getCoordinateSystem(),
                                             null);
                    List types = Misc.newList(null, CoordinateSystem.class,
                                     visad.Set.class);
                    return e.createObjectConstructorElement(o, args, types);
                } catch (Exception exc) {
                    System.err.println("Error persisting RealTuple:" + exc);
                    return null;

                }
            }
        });

        addDelegate(TrivialMapProjection.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                TrivialMapProjection tmp = (TrivialMapProjection) o;
                List args = Misc.newList(tmp.getReference(),
                                         tmp.getDefaultMapArea());
                return e.createObjectConstructorElement(o, args);
            }

        });

        addDelegate(CachingCoordinateSystem.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                CachingCoordinateSystem c = (CachingCoordinateSystem) o;
                List args = Misc.newList(c.getCachedCoordinateSystem());
                return e.createObjectConstructorElement(o, args);
            }

        });

        addDelegate(CartesianProductCoordinateSystem.class,
                    new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                CartesianProductCoordinateSystem c =
                    (CartesianProductCoordinateSystem) o;
                List args = Misc.newList(c.getCoordinateSystems());
                return e.createObjectConstructorElement(o, args);
            }

        });

        addDelegate(Vis5DCoordinateSystem.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Vis5DCoordinateSystem vcs = (Vis5DCoordinateSystem) o;
                List args = Misc.newList(new Integer(vcs.getProjection()),
                                         vcs.getProjectionParams(),
                                         new Double(vcs.getRows()),
                                         new Double(vcs.getColumns()));
                List types = Misc.newList(Integer.TYPE, null, Double.TYPE,
                                          Double.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }

        });

        XmlDelegateImpl refCSDelegate = new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                CoordinateSystem c    = (CoordinateSystem) o;
                List             args = Misc.newList(c.getReference());
                return e.createObjectConstructorElement(o, args);
            }

        };

        addDelegate(CMYCoordinateSystem.class, refCSDelegate);
        addDelegate(CylindricalCoordinateSystem.class, refCSDelegate);
        addDelegate(FlowSphericalCoordinateSystem.class, refCSDelegate);
        addDelegate(HSVCoordinateSystem.class, refCSDelegate);
        addDelegate(IdentityCoordinateSystem.class, refCSDelegate);
        addDelegate(PolarCoordinateSystem.class, refCSDelegate);
        addDelegate(SphericalCoordinateSystem.class, refCSDelegate);
        addDelegate(TrivialNavigation.class, refCSDelegate);

        XmlDelegateImpl genCSDelegate = new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                CoordinateSystem c = (CoordinateSystem) o;
                List args = Misc.newList(c.getReference(),
                                         c.getCoordinateSystemUnits());
                return e.createObjectConstructorElement(o, args);
            }

        };
        addDelegate(WindPolarCoordinateSystem.class, genCSDelegate);

        /**
         *
         * addDelegate(DataImpl.class, new XmlDelegate() {
         *       public Object createObject(XmlEncoder encoder, Element element) {
         *           try {
         *               System.err.println("createObject:" );
         *               byte[]bytes = XmlUtil.decodeBase64(encoder.getTextFromChild(element));
         *               System.err.println("read bytes:" + bytes.length);
         *               BinaryReader reader = new BinaryReader(new ByteArrayInputStream(bytes));
         *               Data data = reader.getData();
         *               System.err.println("got data:" + (data!=null));
         *               return data;
         *           } catch(Exception exc) {
         *               System.err.println("Error instantiating DataImpl:"
         *                                  + exc);
         *               exc.printStackTrace();
         *               return null;
         *           }
         *       }
         *
         *       public Element createElement(XmlEncoder e, Object o) {
         *           try {
         *               System.err.println("createElement:" );
         *               ByteArrayOutputStream baos = new ByteArrayOutputStream();
         *               BinaryWriter writer = new BinaryWriter(baos);
         *               writer.save((DataImpl) o);
         *               baos.close();
         *               byte[]bytes = baos.toByteArray();
         *               System.err.println("createElement: bytes: " +  bytes.length);
         *               Element element = e.createObjectElement(DataImpl.class);
         *               element.appendChild(e.createTextNode(XmlUtil.encodeBase64(bytes)));
         *               return element;
         *           } catch (Exception exc) {
         *               System.err.println("Error persisting DataImpl:"
         *                                  + exc);
         *               exc.printStackTrace();
         *               return null;
         *
         *           }
         *       }
         *   });
         *
         */


        addDelegate(GriddedSet.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                GriddedSet obj   = (GriddedSet) o;
                List       args  = Misc.newList("foo");
                List       types = Misc.newList(String.class);
                return e.createObjectConstructorElement(o, args, types);
            }

        });

        addDelegate(PointOb.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                PointOb obj   = (PointOb) o;
                List    args  = Misc.newList("foo");
                List    types = Misc.newList(String.class);
                return e.createObjectConstructorElement(o, args, types);
            }

        });





        addDelegate(ImageSequenceImpl.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                ImageSequenceImpl obj   = (ImageSequenceImpl) o;
                List              args  = Misc.newList("foo");
                List              types = Misc.newList(String.class);
                return e.createObjectConstructorElement(o, args, types);
            }

        });





        addDelegate(FlatField.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                FlatField obj   = (FlatField) o;
                List      args  = Misc.newList("foo");
                List      types = Misc.newList(String.class);
                return e.createObjectConstructorElement(o, args, types);
            }

        });



        addDelegate(FieldImpl.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                FieldImpl obj   = (FieldImpl) o;
                List      args  = Misc.newList(obj.getType());
                List      types = Misc.newList(FunctionType.class);
                return e.createObjectConstructorElement(o, args, types);
            }

        });


        addDelegate(Color3f.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Color3f c   = (Color3f) o;
                float[] rgb = c.get().getRGBComponents(null);
                List args = Misc.newList(new Float(rgb[0]),
                                         new Float(rgb[1]),
                                         new Float(rgb[2]));
                List types = Misc.newList(Float.TYPE, Float.TYPE, Float.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });

        addDelegate(Vector3f.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Vector3f v   = (Vector3f) o;
                float[]  xyz = new float[3];
                v.get(xyz);
                List args = Misc.newList(new Float(xyz[0]),
                                         new Float(xyz[1]),
                                         new Float(xyz[2]));
                List types = Misc.newList(Float.TYPE, Float.TYPE, Float.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });


        addDelegate(Point3d.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Point3d  p   = (Point3d) o;
                double[] xyz = new double[3];
                p.get(xyz);
                List args = Misc.newList(new Double(xyz[0]),
                                         new Double(xyz[1]),
                                         new Double(xyz[2]));
                List types = Misc.newList(Double.TYPE, Double.TYPE,
                                          Double.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });



    }

    /**
     * Get the Unit class
     *
     * @param unit unit
     *
     * @return the class
     */
    public static Class getUnitClass(Unit unit) {
        if (unit == null) {
            return Unit.class;
        }
        return unit.getClass();
    }

    /**
     * Create a unit from the XML
     *
     * @param e the encoder
     * @param o the XML element
     *
     * @return the unit
     *
     * @throws VisADException problem creating the unit
     */
    public static Unit createUnit(XmlEncoder e, Element o)
            throws VisADException {
        String identifier = (String) e.createObject(XmlUtil.getFirstChild(o));
        if (identifier == null) {
            return null;
        }
        if (identifier.equals("promiscuous")
                || identifier.equals(CommonUnit.promiscuous.toString())) {
            return CommonUnit.promiscuous;
        }
        if (identifier.equals("dimensionless")) {
            return CommonUnit.dimensionless;
        }
        //return  visad.data.units.Parser.instance().parse (identifier);
        String name = o.getAttribute(ATTR_NAME);
        if ((name == null) || (name.length() == 0)) {
            name = identifier;
        }
        Unit theUnit = Util.parseUnit(identifier, name);
        // System.err.println ("Created unit: " + theUnit.getDefinition() + " id=" + theUnit.getIdentifier());
        return theUnit;
    }



    /**
     * Test by running java ucar.visad.VisADPersistence
     *
     * @param args  arguments
     */
    public static void main(String[] args) {
        try {
            String name    = "s since 1970-01-01 00:00:00.000 UTC";
            Unit   theUnit = Util.parseUnit(name);
            System.err.println("Created unit" + theUnit + " id="
                               + theUnit.getIdentifier());
            if (true) {
                return;
            }

            XmlEncoder encoder = new XmlEncoder();
            VisADPersistence.init(encoder);
            //      String s = encoder.toXml (new Real (RealType.XAxis, 5.5));
            String s = encoder.toXml(new Real(5.5));
            System.err.println(s);

            Real r = (Real) encoder.toObject(s);
            System.err.println("Got:" + r);
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
            exc.printStackTrace();
        }
    }



}
