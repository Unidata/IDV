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

package ucar.unidata.xml;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;





import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectArray;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.test.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;



import java.io.*;

import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.Date;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;



/**
 * See the package.html file.
 *
 * @author Metapps development team
 * @version $Revision: 1.79 $Date: 2007/02/07 15:37:48 $
 */


public class XmlEncoder extends XmlUtil {


    /** Used to synchronize the toXml/toObject methods */
    private Object MUTEX = new Object();

    /** Keeps track of the propotype objects. Maps class to object. */
    private Hashtable prototypes = new Hashtable();

    /** This is used for classes that we cannot create a prototype for. */
    private static Object NO_PROTOTYPE = new Object();


    /**
     *  Just a debug flag.
     */
    public static boolean debug = false;

    /**
     * The name of the init method that is called (if it exists)
     * after an object is deserialized
     */
    public static final String METHOD_INIT = "initAfterXml";




    /**
     * Keep around  the classes that we have checked so
     * far that are ok. So we don't have to take the hit of
     * checking everyone we encounter
     */
    private static Hashtable classCtorsOk = new Hashtable();



    /**
     * A fixed array holding the XmlEncoder Class. This is used to see if an object has
     * an initAfterXml method.
     */
    private static final Class[] ENCODER_ARRAY = new Class[] {
                                                     XmlEncoder.class };

    /**
     *  Method name for Hashtable.put
     */
    public static final String METHOD_PUT = "put";

    /**
     *  Method name for List.add
     */
    public static final String METHOD_ADD = "add";




    /**
     *  The name used in the xml for String objects.
     */
    public static final String NAME_STRING = "string";


    /**
     *  The tag name used for arrays.
     */
    public static final String TAG_ARRAY = "array";

    /**
     *  The tag name used for primitive arrays.
     */
    public static final String TAG_PARRAY = "parray";

    /**
     *  The xml tag for constructors.
     */
    public static final String TAG_CONSTRUCTOR = "constructor";

    /**
     *  The xml tag for factory objects.
     */
    public static final String TAG_FACTORY = "factory";

    /**
     *  The xml tag for  object fields.
     */
    public static final String TAG_FIELD = "field";

    /**
     *  The xml tag when we want to ignore something?
     */
    public static final String TAG_IGNORE = "ignore";

    /**
     *  The xml tag for  method calls.
     */
    public static final String TAG_METHOD = "method";

    /**
     *  The xml tag for  the null object.
     */
    public static final String TAG_NULL = "null";

    /**
     *  The xml tag for  objects.
     */
    public static final String TAG_OBJECT = "object";

    /**
     *  The xml tag for  properties.
     */
    public static final String TAG_PROPERTY = "property";


    /**
     *  The xml tag for serialized objects.
     */
    public static final String TAG_SERIAL = "serial";


    /**
     *  The name for the class attribute.
     */
    public static final String ATTR_CLASS = "class";

    /**
     *  The name for the encode attribute
     *
     */
    public static final String ATTR_ENCODING = "encode";

    /**
     *  The value for the encode by base64
     */
    public static final String VALUE_BASE64 = "base64";


    /**
     *  The name for the id attribute.
     */
    public static final String ATTR_ID = "id";

    /**
     *  The name for the idref attribute.
     */
    public static final String ATTR_IDREF = "idref";

    /**
     *  The name for the name attribute.
     */
    public static final String ATTR_NAME = "name";

    /**
     *  The name for the null attribute.
     */
    public static final String ATTR_NULL = "null";

    /**
     *  The name for the stringvalue attribute.
     */
    public static final String ATTR_STRINGVALUE = "stringvalue";

    /**
     *  The name for the length attribute.
     */
    public static final String ATTR_LENGTH = "length";


    /**
     *  The name for the value attribute.
     */
    public static final String ATTR_VALUE = "value";



    /**
     *  The Xml document we create.
     */
    protected Document document;

    /**
     *  List of classes that we have corresponding {@link XmlDelegate}s for.
     */
    protected ArrayList delegateClasses = new ArrayList();

    /**
     *  The List of {@link XmlDelegate}s that know how to handle certain classes.
     */
    protected ArrayList delegates;

    /**
     *  Holds the set of (String) method names that are ok to execute. By default we add "put" and "add"
     *  into them. We keep this list to provide a layer of security, we don't just willy-nilly invoke methods
     *  from an encoded xml document.
     */
    private Hashtable okMethods;

    /**
     *  Allows one to have a set of pre-existing objects that are .equals
     *  to newly created objects.
     */
    private Hashtable seedTable;


    /**
     *  A mapping between the Classes for primitives (e.g., int, Integer, double)
     *  and their name.
     */
    private Hashtable primitiveClassToName;

    /**
     *  A mapping between the names for primitives (e.g., int, Integer, double)
     *  and their Class.
     */
    private Hashtable nameToPrimitiveClass;

    /**
     *  A mapping between the primitive classes (e.g., int, double)
     *  and the corresponding Object classes (e.g., Integer, Double)
     */
    private Hashtable primitiveClassToCtor;

    /**
     * Holds the set of primitive classes (e.g., Integer.TYPE)
     * so we can quickly determine if a given Class is a primitive
     */
    private Hashtable primitiveClasses;


    /**
     *  A counter to generate unique (for this encoder) String ids.
     */
    private int nextObjectId;

    /**
     *  A mapping from String ids to the Object.
     */
    private Hashtable idToObject;

    /**
     *  A mapping from an object to its id.
     */
    private Hashtable objectToId;

    /**
     *  A mapping from an object to the DOM element that defined it.
     */
    private Hashtable objectToElement;

    /**
     *  A mapping from an old (perhaps no longer in existence) class name to the new Class that handles it.
     */
    private Hashtable<String, Class> classNameToClass = new Hashtable<String,
                                                            Class>();


    /** _more_          */
    private Hashtable<String, String> newClassNames = new Hashtable<String,
                                                          String>();


    /** _more_          */
    private List<String[]> patterns = new ArrayList<String[]>();

    /**
     *  Keep a list of the exceptions that get throwing during the encoding or decoding step.
     */
    private ArrayList exceptions;

    /**
     *  Keep a list of the corresponding error messages.
     */
    private ArrayList errorMessages;

    /**
     *  Create a new XmlEncoder.
     */
    public XmlEncoder() {
        addDefaultDelegates();
    }




    /**
     *  Define the set of default delegates (e.g., for Rectangle, Font, etc.) for common objects
     *  that don't encode very well.
     */
    protected void addDefaultDelegates() {
        //TODO: We don't to need to create these for every encoder.
        addDelegateForClass(Color.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Color color = (Color) o;
                List args = Misc.newList(new Integer(color.getRed()),
                                         new Integer(color.getGreen()),
                                         new Integer(color.getBlue()));
                List types = Misc.newList(Integer.TYPE, Integer.TYPE,
                                          Integer.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });

        addDelegateForClass(Rectangle.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Rectangle r = (Rectangle) o;
                List args = Misc.newList(new Integer(r.x), new Integer(r.y),
                                         new Integer(r.width),
                                         new Integer(r.height));
                List types = Misc.newList(Integer.TYPE, Integer.TYPE,
                                          Integer.TYPE, Integer.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });


        addDelegateForClass(Rectangle2D.Double.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Rectangle2D.Double r = (Rectangle2D.Double) o;
                List args = Misc.newList(new Double(r.x), new Double(r.y),
                                         new Double(r.width),
                                         new Double(r.height));
                List types = Misc.newList(Double.TYPE, Double.TYPE,
                                          Double.TYPE, Double.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });
        addDelegateForClass(Rectangle2D.Float.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Rectangle2D.Float r = (Rectangle2D.Float) o;
                List args = Misc.newList(new Float(r.x), new Float(r.y),
                                         new Float(r.width),
                                         new Float(r.height));
                List types = Misc.newList(Float.TYPE, Float.TYPE, Float.TYPE,
                                          Float.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });

        addDelegateForClass(Point.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Point p     = (Point) o;
                List  args  = Misc.newList(new Integer(p.x),
                                           new Integer(p.y));
                List  types = Misc.newList(Integer.TYPE, Integer.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });

        addDelegateForClass(Dimension.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Dimension p = (Dimension) o;
                List args = Misc.newList(new Integer(p.width),
                                         new Integer(p.height));
                List types = Misc.newList(Integer.TYPE, Integer.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });

        addDelegateForClass(Font.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Font f = (Font) o;
                List args = Misc.newList(f.getName(),
                                         new Integer(f.getStyle()),
                                         new Integer(f.getSize()));
                List types = Misc.newList(String.class, Integer.TYPE,
                                          Integer.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });

        addDelegateForClass(Date.class, new XmlDelegateImpl() {
            public Element createElement(XmlEncoder e, Object o) {
                Date  p     = (Date) o;
                List  args  = Misc.newList(new Long(p.getTime()));
                List  types = Misc.newList(Long.TYPE);
                return e.createObjectConstructorElement(o, args, types);
            }
        });


    }



    /**
     *  Convert the  given object to xml, formatting the xml (with spaces and newlines).
     *
     *  @param theObject The object to convert.
     *  @return The String xml that represents the object.
     */
    public String toXml(Object theObject) {
        return toXml(theObject, true);
    }


    /**
     *  Convert the  given object to xml.
     *
     *  @param theObject The object to convert.
     *  @param formatXml Do we format the result xml with newlines and spaces to make it readable.
     *  @return The String xml that represents the object.
     */
    public String toXml(Object theObject, boolean formatXml) {
        long   t1     = System.currentTimeMillis();
        String result = toXmlInner(theObject, formatXml);
        long   t2     = System.currentTimeMillis();
        //      System.err.println ("Time:" + (t2-t1));
        return result;
    }



    /**
     *  Convert the  given object to xml.
     *
     *
     * @param o
     *  @param formatXml Do we format the result xml with newlines and spaces to make it readable.
     *  @return The String xml that represents the object.
     */
    private String toXmlInner(Object o, boolean formatXml) {
        synchronized (MUTEX) {
            init();
            String xml = null;
            try {
                Element element = toElement(o);
                if (element != null) {
                    xml = XmlUtil.toStringWithHeader(element, (formatXml
                            ? "    "
                            : ""), (formatXml
                                    ? "\n"
                                    : ""));
                }
            } catch (Exception exc) {
                logException("Error:", exc);
            }
            LogUtil.printExceptions(errorMessages, exceptions);
            clear();
            return xml;
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List getExceptions() {
        return exceptions;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getErrorMessages() {
        return errorMessages;
    }



    /**
     *  Create the DOM  Element that represents the given object.
     *
     *  @param theObject The object to encode.
     *  @return The dom Element that represents the given Object.
     */
    public Element toElement(Object theObject) {
        return createElement(theObject);
    }


    /**
     * _more_
     *
     * @param xml _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Object decodeXml(String xml) throws Exception {
        return new XmlEncoder().toObject(xml);
    }

    /**
     * _more_
     *
     * @param object _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String encodeObject(Object object) throws Exception {
        String s = new XmlEncoder().toXml(object, false);
        return s;
    }


    /**
     *  Create an object from the given xml. This will catch any exceptions and print them out
     *  in toto, returning null.
     *
     *  @param xml The xml String that defines an object.
     *  @return The newly created object.
     *  @throws Exception When anything bad happens.
     */
    public Object toObject(String xml) throws Exception {
        return toObject(xml, true);
    }


    /**
     * _more_
     *
     * @param xml _more_
     * @param catchAndLogError _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Object toObject(String xml, boolean catchAndLogError)
            throws Exception {
        if ((xml == null) || (xml.length() == 0)) {
            return null;
        }

        try {
            Element root = XmlUtil.getRoot(xml);
            if (root == null) {
                return null;
            }
            return toObjectInner(root, catchAndLogError);
        } catch (Exception exc) {
            if ( !catchAndLogError) {
                throw exc;
            }
            logException("Error:", exc);
            return null;
        }

    }


    /**
     *  Create an object from the given dom subtree.
     *
     *  @param node The xml.
     *  @return the newly created object.
     */
    public Object toObject(Element node) {
        try {
            return toObjectInner(node, true);
        } catch (Exception exc) {
            logException("Error:", exc);
            return null;
        }
    }



    /**
     *  Create an object from the given dom subtree.
     *
     *  @param node The xml.
     * @param catchAndLogError _more_
     *  @return the newly created object.
     *
     * @throws Exception _more_
     */
    private Object toObjectInner(Element node, boolean catchAndLogError)
            throws Exception {
        synchronized (MUTEX) {
            Object object = null;
            init();

            try {
                object = createObject(node);
            } catch (Exception exc) {
                if ( !catchAndLogError) {
                    throw exc;
                }
                logException("Error:", exc);
            }

            if ((exceptions != null) && (exceptions.size() > 0)
                    && !catchAndLogError) {
                throw (Exception) exceptions.get(0);
            }
            LogUtil.printExceptions(errorMessages, exceptions);
            clear();
            return object;
        }
    }


    /**
     *  Clear the internal state this encoder keeps during the encoding/decoding process.
     */
    public void clear() {
        nextObjectId    = 0;
        idToObject      = null;
        objectToId      = null;
        objectToElement = null;
    }

    /**
     *  This resets the state of this encoder.
     */
    protected void init() {
        if (idToObject != null) {
            return;
        }
        nextObjectId    = 0;
        idToObject      = new Hashtable();
        objectToId      = new Hashtable();
        objectToElement = new Hashtable();
    }



    /**
     *  Is  the given method name one of the allowable methods to execute.
     *
     *  @param methodName The mehtod name.
     *  @return Ok to invoke the method.
     */
    public boolean methodOk(String methodName) {
        if (okMethods == null) {
            okMethods = new Hashtable();
            okMethods.put(METHOD_ADD, METHOD_ADD);
            okMethods.put(METHOD_PUT, METHOD_PUT);
        }
        return (okMethods.get(methodName) != null);
    }


    /**
     *  Add  an {@link XmlDelegate} for the given class.
     *  The delegate is the object responsible for the persistence
     *  of all objects  of the type of the given class or are a derived class.
     *
     *  @param theClass The class to handle.
     *  @param delegate The delegate that handles the class.
     */
    public void addDelegateForClass(Class theClass, XmlDelegate delegate) {
        if (delegates == null) {
            delegates = new ArrayList();
        }
        delegateClasses.add(theClass);
        delegates.add(delegate);
    }


    /**
     *  Add  an {@link XmlDelegate} for the given class. The given delegate
     * will get added to the beginning of the list so that it takes
     * priority over ant existing delegates
     *  The delegate is the object responsible for the persistence
     *  of all objects  of the type of the given class or are a derived class.
     *
     *  @param theClass The class to handle.
     *  @param delegate The delegate that handles the class.
     */
    public void addHighPriorityDelegateForClass(Class theClass,
            XmlDelegate delegate) {
        if (delegates == null) {
            delegates = new ArrayList();
        }
        delegateClasses.add(0, theClass);
        delegates.add(0, delegate);
    }




    /**
     *  Find the {@link XmlDelegate} that handles the given class.
     *
     *  @param theClass The class.
     *  @return The delegate that handles the class.
     */

    protected XmlDelegate getDelegate(Class theClass) {
        if (delegates == null) {
            return null;
        }
        //First try an exact match.
        //      final int dCnt = delegates.size ();
        for (int i = 0; i < delegates.size(); i++) {
            Class c = (Class) delegateClasses.get(i);
            if (c.equals(theClass)) {
                return (XmlDelegate) delegates.get(i);
            }
        }

        //Now try instanceof
        for (int i = 0; i < delegates.size(); i++) {
            Class c = (Class) delegateClasses.get(i);
            if (c.isAssignableFrom(theClass)) {
                return (XmlDelegate) delegates.get(i);
            }
        }
        return null;
    }



    /**
     *  Create (if needed) and return the xml Document for this encoder.
     *
     *  @return The xml document to write to.
     */
    public Document getDocument() {
        if (document == null) {
            document = XmlUtil.makeDocument();
        }
        return document;
    }

    /**
     *  Set the xml Document used by this encoder. This allows client code to override which Document
     *  is to be used.
     *
     *  @param document The new xml document for this encoder to use.
     */
    public void setDocument(Document document) {
        this.document = document;
    }


    /**
     *  Utility method to return a unique (during a particular encoding) String id.
     *
     *  @return A unique (for this encoder) id.
     */
    protected String getNextId() {
        return "id" + (++nextObjectId);
    }

    /**
     *  Returns the object (maybe null) that has the given id.
     *
     *  @param id The id to lookup.
     *  @return The object defined by the id or null if not found.
     */
    protected Object getObjectFromId(String id) {
        if (id == null) {
            return null;
        }
        return idToObject.get(id);
    }

    /**
     *  Add an existing object from the environement that is to be used in place
     *  of a newly created object.
     *  You can "seed" an encoder so that when a newly created object is ".equals" with
     *  the seedObject then we actually return and use  the seedObject. This way we maintain
     *  Object pointer equality.
     *
     *  @param seedObject The object that exists in the environment that we really want to use.
     */
    public void addSeedObject(Object seedObject) {
        if (seedTable == null) {
            seedTable = new Hashtable();
        }
        seedTable.put(seedObject, seedObject);
    }

    /**
     *  Lookup to see if we have a previously existing object that is in the seedTable
     *  that the newlyCreatedObject is .equals to.
     *
     *  @param newlyCreatedObject The newly created object that we want to see if it has a previously created
     *  object to actually use.
     *  @return  The previously created object or null.
     */
    private Object getSeedObject(Object newlyCreatedObject) {
        if (seedTable == null) {
            return null;
        }
        return seedTable.get(newlyCreatedObject);
    }


    /**
     *  A utility method that provides for pointer == based  hashtable inserts and lookups.
     *
     *  @param ht The hasthable to look up the key on.
     *  @param key The key.
     *  @return The value that the key object is mapped to.
     */
    private Object getObject(Hashtable ht, Object key) {
        return ht.get(new KeyWrapper(key));
    }

    /**
     *  Wrap the given key in a {@link KeyWrapper} and place it in the given Hashtable.
     *  This provides for pointer based equality checks in the Hashtable lookup, not .equals
     *  based check.
     *
     * @param ht
     * @param key
     * @param value
     */
    private void putObject(Hashtable ht, Object key, Object value) {
        ht.put(new KeyWrapper(key), value);
    }


    /**
     *  This class allows us to put objects into a hashtable and overwrite the
     *  equals method, doing  pointer equals instead of Object.equals.
     */

    private static class KeyWrapper {

        /** The hashtable key */
        private Object key;

        /**
         * Create one
         *
         * @param key The hashtable key
         *
         */
        public KeyWrapper(Object key) {
            this.key = key;
        }

        /**
         * override hashcode
         *
         * @return The hashcode
         */
        public int hashCode() {
            return key.hashCode();
        }

        /**
         * Override equals
         *
         * @param o Object to compare to
         * @return is equals
         */
        public boolean equals(Object o) {
            if (o instanceof KeyWrapper) {
                o = ((KeyWrapper) o).key;
            }
            return (key == o);
        }
    }


    /**
     *  Define a mapping between the given id and the object.
     *
     *  @param id The id of the object.
     *  @param  theObject The object.
     */
    protected void setObject(String id, Object theObject) {
        if ((id == null) || (theObject == null)) {
            return;
        }
        init();
        idToObject.put(id, theObject);
    }


    /**
     *  Return the encoding id defined for the given object.
     *
     *  @param theObject The object to lookup an id for.
     *  @return The string id of the object or null.
     */
    protected String getObjectId(Object theObject) {
        if (theObject == null) {
            return null;
        }
        init();
        return (String) getObject(objectToId, theObject);
    }

    /**
     *  Return the DOM element that represents the given object.
     *
     *  @param theObject The object.
     *  @return The dom Element that represented the object or null if not found.
     */
    protected Element getElementForObject(Object theObject) {
        if (theObject == null) {
            return null;
        }
        return (Element) getObject(objectToElement, theObject);
    }


    /**
     *  Allow client code to predefine an object to id mapping. This allows client code
     *  to predefine that certain objects (say, perhaps a fixed global singleton) are defined
     *  with the given id. The object itself won't get written out in the encoding but any other
     *  object that has a reference to theObject will write out an &lt;object idref=theId&gt; tag.
     *  When the encoded object is read back in it will point to the predefined object. e.g.:
     *  <pre>
     *  encoder.defineObjectId (someSingletonObject, "idOfSingletonObject");
     *  </pre>
     *  Will result in xml that looks like:
     *  <pre>
     *  &lt;object class="SomeOtherObject"&gt;
     *  ...
     *  &lt property name="referenceToSomeSingleton" idref="idOfSingletonObject"/&gt;
     *  ...
     *  </pre>
     *  Now when we decode the above xml the object defined with "idOfSingletonObject"
     *  will be pre-loaded here.
     *
     *  @param theObject The initial object.
     *  @param theId The id.
     */
    public void defineObjectId(Object theObject, String theId) {
        init();
        putObject(objectToId, theObject, theId);
        idToObject.put(theId, theObject);
    }


    /**
     *  Create a new id and define mappings between the id, object and DOM element.
     *
     *  @param theObject The object to define an id for.
     *  @return The new id.
     */
    protected String setObjectId(Object theObject) {
        String nextId = getNextId();
        defineObjectId(theObject, nextId);
        return nextId;
    }


    /**
     *  Create a new id and define mappings between the id, object and DOM element.
     *
     *  @param theObject The object to define a new id for.
     *  @param element  The Xml DOM node that defines theObject.
     *  @return The new id of the object.
     */
    public String setObjectId(Object theObject, Element element) {
        if ((element == null) || (theObject == null)) {
            return null;
        }
        putObject(objectToElement, theObject, element);
        return setObjectId(theObject);
    }


    /**
     *  Initialize the mapping between names for primitives and their classes.
     */
    protected void initPrimitiveName() {
        //Have we done this already?
        if (primitiveClassToName != null) {
            return;
        }

        primitiveClassToName = new Hashtable();
        nameToPrimitiveClass = new Hashtable();
        primitiveClassToCtor = new Hashtable();
        primitiveClasses     = new Hashtable();
        Class[] bclasses = {
            Boolean.TYPE, Byte.TYPE, Character.TYPE, Short.TYPE, Integer.TYPE,
            Long.TYPE, Float.TYPE, Double.TYPE
        };
        Class[] wclasses = {
            Boolean.class, Byte.class, Character.class, Short.class,
            Integer.class, Long.class, Float.class, Double.class
        };
        for (int i = 0; i < bclasses.length; i++) {
            primitiveClassToName.put(bclasses[i], bclasses[i].getName());
            nameToPrimitiveClass.put(bclasses[i].getName(), bclasses[i]);
            primitiveClasses.put(bclasses[i], bclasses[i]);
            Constructor ctor = Misc.findConstructor(wclasses[i],
                                   new Class[] { String.class });
            if (ctor != null) {
                primitiveClassToCtor.put(bclasses[i], ctor);
            }
        }
        for (int i = 0; i < wclasses.length; i++) {
            primitiveClassToName.put(wclasses[i], wclasses[i].getName());
            nameToPrimitiveClass.put(wclasses[i].getName(), wclasses[i]);
            Constructor ctor = Misc.findConstructor(wclasses[i],
                                   new Class[] { String.class });
            if (ctor != null) {
                primitiveClassToCtor.put(wclasses[i], ctor);
            }
        }

        //Special case for String
        primitiveClassToName.put(String.class, NAME_STRING);
        nameToPrimitiveClass.put(NAME_STRING, String.class);
    }

    /**
     *  Is the given Class a primitive (e.g., an int, float, Integer, Double, etc.).
     *
     *  @param theClass The class to check.
     *  @return Is theClass a primitive Class.
     */
    public boolean isPrimitive(Class theClass) {
        initPrimitiveName();
        return (primitiveClasses.get(theClass) != null);
    }


    /**
     *  Return the name of the class  to be used for the given class.
     *
     *  @param primitiveClass The class.
     *  @return The class name to use for the class.
     */
    public String getPrimitiveName(Class primitiveClass) {
        initPrimitiveName();
        return (String) primitiveClassToName.get(primitiveClass);
    }

    /**
     *  For the given name (e.g., "int", "boolean") return the class (e.g., Integer.TYPE, Boolean.TYPE).
     *
     *  @param name Primitive class name.
     *  @return The Class.
     */
    public Class getPrimitiveClass(String name) {
        initPrimitiveName();
        return (Class) nameToPrimitiveClass.get(name);
    }



    /**
     *  Find the Constructor   that creates objects of the given primitive class.
     *
     *  @param primitiveClass The class of the primitive (e.g., Integer, int, etc.)
     *  @return The constructor to use.
     */
    public Constructor getPrimitiveCtor(Class primitiveClass) {
        initPrimitiveName();
        return (Constructor) primitiveClassToCtor.get(primitiveClass);
    }



    /**
     *  Define a mapping from some name (perhaps an old class path) to the new
     *  Class that actually is used to create an object.
     *  For example, say you have encoded and saved as xml some object with
     *  class: old.path.SomeObject. Now later you restructured your code (like good programmers do)
     *  so now the SomeObject class is really: new.path.SomeObject but you want to be able
     *  to read in the old xml. So you do:
     *  <pre>
     *  encoder.registerClassName ("old.path.SomeObject",  new.path.SomeObject.class);
     *  </pre>
     *
     *
     *  @param theName The old class name.
     *  @param theClass The new Class to use.
     *
     */
    public void registerClassName(String theName, Class theClass) {
        classNameToClass.put(theName, theClass);
    }


    /**
     * _more_
     *
     * @param oldName _more_
     * @param newName _more_
     */
    public void registerNewClassName(String oldName, String newName) {
        newClassNames.put(oldName, newName);
    }





    /**
     * This allows on to change package  paths of classes that are in bundles
     * with a new path
     */

    public void addClassPatternReplacement(String pattern, String replace) {
        patterns.add(new String[]{pattern, replace});
    }


    /**
     *  Find the Class that corresponds to the given className. Lookup in the classNameToClass table
     *  to see if we have a different Class. If not then just used Class.forName (className);
     *
     *  @param  className The name of the Class.
     *  @return The class found for the className.
     *  @throws ClassNotFoundException When we cannot find the class.
     */
    public Class getClass(String className) throws ClassNotFoundException {
        Class type = getPrimitiveClass(className);
        if (type != null) {
            return type;
        }

        String newClassName = newClassNames.get(className);
        if (newClassName != null) {
            //            System.err.println("new class name: " + newClassName +" for:" + className);
            className = newClassName;
        }

        for(String[] patternTuple: patterns) {
            className = className.replace(patternTuple[0], patternTuple[1]);
        }


        type = (Class) classNameToClass.get(className);
        if (type != null) {
            return type;
        }
        type = Misc.findClass(className);
        classNameToClass.put(className, type);
        return type;
    }

    /**
     *  Get the String name of the given class. We lookup to see if the given Class
     *  is for one of the primitive types. Else we lookup in the mapping defined with
     *  registerClassName. Finally we simply use theClass.getName ().
     *
     *  @param theClass The Class.
     *  @return The name to use for the given Class.
     */
    public String getClassName(Class theClass) {
        String name = getPrimitiveName(theClass);
        if (name != null) {
            return name;
        }
        return theClass.getName();
    }


    /**
     *  Create a new Element of the given tag name using the current document.
     *
     *  @param tagName The tag name.
     *  @return The new Element.
     */
    public Element newElement(String tagName) {
        return getDocument().createElement(tagName);
    }


    /**
     *  Create an Xml Element that represents the given object (which should be an array).
     *  For example, a String array:
     *  <pre>
     *  String[]array ={"foo", "bar"};
     *  </pre>
     *  would result in:
     *         String[]array ={"foo", "bar"};<pre>
     * &lt;array class="string"  length="2" &gt;
     * &lt;string&gt;&lt;![CDATA[foo]]&gt;&lt;/string&gt;
     * &lt;string&gt;&lt;![CDATA[bar]]&gt;&lt;/string&gt;
     * &lt;/array&gt;
     *  </pre>
     *  Primitive arrays are handled differently (for size reasons):
     *  <pre>
     *  int[]array ={5,4};
     *  </pre>
     *  Would result in:
     *  <pre>
     * &lt;parray class="int"  length="2" &gt;5,4&lt;/parray&gt;
     *  </pre>
     *
     *  @param arrayObject The array object.
     *  @return The new Xml Element that represents the arrayObject.
     */
    public Element createArrayElement(Object arrayObject) {
        Class theClass = arrayObject.getClass();
        int   length   = Array.getLength(arrayObject);
        if (isPrimitive(theClass.getComponentType())) {
            return createPrimitiveArrayElement(arrayObject);
        }
        Element arrayElement = newElement(TAG_ARRAY);

        arrayElement.setAttribute(ATTR_CLASS,
                                  getClassName(theClass.getComponentType()));
        arrayElement.setAttribute(ATTR_LENGTH, "" + length);
        for (int i = 0; i < length; i++) {
            arrayElement.appendChild(createElement(Array.get(arrayObject,
                    i)));
        }
        return arrayElement;
    }

    /**
     *  Create an Xml Element that represents the given array of primitives.
     *
     *  @param primitiveArray The array of primitives.
     *  @return The xml representation of the primitive array.
     */

    public Element createPrimitiveArrayElement(Object primitiveArray) {
        Class   theClass     = primitiveArray.getClass();
        int     length       = Array.getLength(primitiveArray);
        Element arrayElement = newElement(TAG_PARRAY);
        String  contents;
        arrayElement.setAttribute(ATTR_CLASS,
                                  getClassName(theClass.getComponentType()));
        if (length < 20) {
            StringBuffer buff = new StringBuffer();
            arrayElement.setAttribute(ATTR_LENGTH, "" + length);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    buff.append(",");
                }
                buff.append(Array.get(primitiveArray, i).toString());
            }
            contents = buff.toString();
        } else {
            try {
                contents = new String(
                    XmlUtil.encodeBase64(
                        serialize((Serializable) primitiveArray)));
            } catch (Exception exc) {
                logException("Error primitive creating array", exc);
                contents = null;
            }
        }
        arrayElement.appendChild(getDocument().createTextNode(contents));
        return arrayElement;
    }




    /**
     * A utility to create a text node
     *
     * @param contents The text
     *
     * @return The text node
     */
    public Node createTextNode(String contents) {
        return getDocument().createTextNode(contents);
    }


    /**
     * A utility to deserialize the given bytes
     *
     * @param bytes The bytes
     *
     * @return The deserialized object
     *
     * @throws Exception When something bad happens
     */
    public static Object deserialize(byte[] bytes) throws Exception {
        ByteArrayInputStream istream = new ByteArrayInputStream(bytes);
        ObjectInputStream    p       = new ObjectInputStream(istream);
        return p.readObject();
    }

    /**
     * A utility to serialize the given object
     *
     * @param object The object
     *
     * @return The serialized representation of the object
     *
     * @throws Exception When something bad happens
     */
    public static byte[] serialize(Serializable object) throws Exception {
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        ObjectOutputStream    p       = new ObjectOutputStream(ostream);
        p.writeObject(object);
        p.flush();
        ostream.close();
        return ostream.toByteArray();
    }


    /**
     *  Create a "method" xml element, one that represents a method call. The  argumentElements
     *  is a List of Xml Elements that represent the arguments to the method.
     *
     *  @param methodName The name of the method.
     *  @param argumentElements List of method arguments.
     *  @return The method xml Element.
     */
    public Element createMethodElement(String methodName,
                                       List argumentElements) {
        Element n = createMethodElement(methodName);
        XmlUtil.addChildren(n, argumentElements);
        return n;
    }

    /**
     *  Create a "method" xml element, one that represents a method call.
     *
     *  @param methodName The name of the method.
     *  @param contents Xml represention of the method arguments.
     *  @return The method xml Element.
     */
    public Element createMethodElement(String methodName, Element contents) {
        Element element = createMethodElement(methodName);
        element.appendChild(contents);
        return element;
    }

    /**
     *  Create a "method" xml element, one that represents a method call.  This method has no
     *  arguments.
     *
     *  @param methodName The name of the method.
     *  @return The method xml Element.
     */
    public Element createMethodElement(String methodName) {
        Element n = newElement(TAG_METHOD);
        n.setAttribute(ATTR_NAME, methodName);
        return n;
    }



    /**
     *  Create a "serial" xml element, one that represents a serialized Object.
     *
     *  @param theClass The class of the original serialized object.
     *  @param serialRepresentation Go figure.
     *  @return The xml element that represents the serialized object.
     */
    public Element createSerialElement(Class theClass,
                                       String serialRepresentation) {
        Element element = newElement(TAG_SERIAL);
        element.setAttribute(ATTR_CLASS, theClass.getName());
        element.appendChild(
            getDocument().createCDATASection(serialRepresentation));
        return element;
    }




    /**
     *  Create a "property" xml element, one that represents a bean property on an object.
     *
     *  @param propertyName The bean name.
     *  @param value The xml represention of the property value.
     *  @return The xml element that represents the property.
     */
    public Element createPropertyElement(String propertyName, Element value) {
        Element element = newElement(TAG_PROPERTY);
        element.setAttribute(ATTR_NAME, propertyName);
        element.appendChild(value);
        return element;
    }



    /**
     *  Create an element that represents a reference to an already encoded object.
     *
     *  @param id The id of the object.
     *  @return The xml element that represents holds the object reference.
     */
    public Element createReferenceElement(String id) {
        Element element = newElement(TAG_OBJECT);
        element.setAttribute(ATTR_IDREF, id);
        return element;
    }


    /**
     *  Create the xml element for the given primitive value and name.
     *
     *  @param primitiveName The name of the primitive (e.g., string, int, etc.)
     *  @param value The value.
     *  @return The xml representation of the primitive.
     */
    public Element createPrimitiveElement(String primitiveName,
                                          Object value) {
        Element element = newElement(primitiveName);
        if (value != null) {
            Node   child  = null;
            String svalue = value.toString();
            if (primitiveName.equals(NAME_STRING)) {
                if (svalue.length() == 0) {
                    //If this is a string and the string length == 0 then  write out an
                    //attribute stringvalue=""
                    element.setAttribute(ATTR_STRINGVALUE, "");
                } else {
                    //If there is xml in the string then base64 encode it
                    if (svalue.indexOf("<") >= 0) {
                        svalue = XmlUtil.encodeBase64(svalue.getBytes());
                        element.setAttribute(ATTR_ENCODING, VALUE_BASE64);
                    }
                    child = getDocument().createCDATASection(svalue);
                }
            } else {
                child = getDocument().createTextNode(svalue);
            }
            if (child != null) {
                element.appendChild(child);
            }
        } else {
            //If the value is null then write out the null="true" attribute
            element.setAttribute(ATTR_NULL, "true");
        }
        return element;
    }


    /**
     *  Create a null element with the given class (if non-null).
     *
     *  @param type The class of the null object.
     *  @return The null element.
     */
    public Element createNullElement(Class type) {
        Element element = newElement(TAG_NULL);
        if (type != null) {
            element.setAttribute(ATTR_CLASS, getClassName(type));
        }
        return element;
    }

    /**
     *  Create a null element with no class.
     *
     *  @return The null element.
     */
    public Element createNullElement() {
        return createNullElement(null);
    }

    /**
     *  Create a "object" tag with the given objectClass.
     *
     *  @param objectClass The class of the object.
     *  @return The xml object tag.
     */
    public Element createObjectElement(Class objectClass) {
        Element element = newElement(TAG_OBJECT);
        element.setAttribute(ATTR_CLASS, getClassName(objectClass));
        return element;
    }


    /**
     *  Create a "factory" tag with the given factoryClass.
     *
     *  @param factoryClass The class of the object that implements {@link XmlObjectFactory}.
     *  @return The xml factory tag.
     */
    public Element createFactoryElement(Class factoryClass) {
        Element element = newElement(TAG_FACTORY);
        element.setAttribute(ATTR_CLASS, getClassName(factoryClass));
        return element;
    }

    /**
     *  Create an object tag that holds a constructor tag.
     *
     *  @param object The object to encode.
     *  @param arguments The argument values to the constructor.
     *  @return The xml object tag containing a constructor tag.
     */
    public Element createObjectConstructorElement(Object object,
            List arguments) {
        return createObjectConstructorElement(object, arguments, null);
    }

    /**
     *  Create an object tag that holds a constructor tag.
     *
     *  @param object The object to encode.
     *  @param arguments The argument values to the constructor.
     *  @param types The types  of the arguments
     *  @return The xml object tag containing a constructor tag.
     */

    public Element createObjectConstructorElement(Object object,
            List arguments, List types) {
        Element result      = createObjectElement(object.getClass());
        Element ctorElement = createConstructorElement(arguments, types);
        result.appendChild(ctorElement);
        return result;
    }

    /**
     *  Construct a "constructor" tag that holds the set of argument objects.
     *
     *  @param arguments The argument values.
     *  @return The xml constructor tag.
     */

    public Element createConstructorElement(List arguments) {
        return createConstructorElement(arguments, null);
    }


    /**
     *  Construct a "constructor" tag that holds the set of argument objects with respective types.
     *
     *  @param arguments The argument values.
     *  @param types The types of the arguments.
     *  @return The xml constructor tag.
     */
    public Element createConstructorElement(List arguments, List types) {
        Element element = newElement(TAG_CONSTRUCTOR);
        if (arguments != null) {
            for (int i = 0; i < arguments.size(); i++) {
                if (types != null) {
                    Class  type = (Class) types.get(i);
                    Object arg  = arguments.get(i);
                    element.appendChild(createElement(arg, ((type != null)
                            ? type
                            : ((arg != null)
                               ? arg.getClass()
                               : Object.class))));
                } else {
                    element.appendChild(createElement(arguments.get(i)));
                }
            }
        }
        return element;
    }



    /**
     *  For the given class find the set of public set/get property methods.
     *
     *  @param c The class to look at.
     *  @return The list of property Method-s.
     */
    public static List findPropertyMethods(Class c) {
        return findPropertyMethods(c, true);
    }

    /**
     * _more_
     *
     * @param c _more_
     * @param returnGetters _more_
     *
     * @return _more_
     */
    public static List findPropertyMethods(Class c, boolean returnGetters) {
        Method[]  methods = c.getMethods();
        ArrayList v       = new ArrayList();
        for (int i = 0; i < methods.length; i++) {
            Method getter = methods[i];
            String name   = getter.getName();
            if ( !name.startsWith("get")) {
                continue;
            }
            String  propertyName = name.substring(3);
            Class[] getterParams = getter.getParameterTypes();
            if (getterParams.length != 0) {
                continue;
            }
            Class[] setterParams = { getter.getReturnType() };
            Method  setter       = null;
            try {
                setter = c.getMethod("set" + propertyName, setterParams);
                if (returnGetters) {
                    v.add(getter);
                } else {
                    v.add(setter);
                }
            } catch (NoSuchMethodException nsme) {
                continue;
            }

        }
        return v;
    }


    /**
     *  Create an object defined by the given XML.
     *
     *  @param element The xml representation of the  object.
     *  @return The  object.
     */

    public Object createObject(Element element) {
        ObjectClass oc = createObjectInner(element);
        return ((oc == null)
                ? null
                : oc.object);
    }

    /**
     *  Create an object defined by the given XML. Don't check if the object has a delegate.
     *
     *  @param element The xml representation of the  object.
     *  @return The  object.
     */
    public Object createObjectDontCheckDelegate(Element element) {
        ObjectClass oc = createObjectInner(element, false);
        return ((oc == null)
                ? null
                : oc.object);
    }


    /**
     *  Create an array that contains a set of  objects.
     *
     *  @param element The xml representation of the  array.
     *  @return The  array object and its class.
     */

    public ObjectClass createArrayObject(Element element) {
        try {
            Class arrayType = getClass(element.getAttribute(ATTR_CLASS));
            int length =
                new Integer(element.getAttribute(ATTR_LENGTH)).intValue();
            Object   array    = Array.newInstance(arrayType, length);
            NodeList children = XmlUtil.getElements(element);
            for (int i = 0; i < children.getLength(); i++) {
                Object      o     = children.item(i);
                Element     child = (Element) children.item(i);
                ObjectClass oc    = createObjectInner(child);
                if (oc != null) {
                    Array.set(array, i, oc.object);
                }
            }
            return new ObjectClass(array);
        } catch (Exception exc) {
            logException("Error creating array", exc);
        }
        return null;
    }




    /**
     * A utility to find the text node child of the given parent
     *
     * @param parent The parent node
     *
     * @return The contents of the text node
     */
    public String getTextFromChild(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.TEXT_NODE) {
                return n.getNodeValue();
            }
        }
        return null;
    }


    /**
     *  Create an array that contains a set of primitive objects.
     *
     *  @param element The xml representation of the primitive array.
     *  @return The primitive array object and its class.
     */

    public ObjectClass createPrimitiveArrayObject(Element element) {
        try {
            Class  arrayType = getClass(element.getAttribute(ATTR_CLASS));
            String lengthStr = element.getAttribute(ATTR_LENGTH);
            Object array;
            String value = getTextFromChild(element);

            if ((lengthStr == null) || (lengthStr.length() == 0)) {
                return new ObjectClass(
                    deserialize(XmlUtil.decodeBase64(value)));
            } else {
                int length = new Integer(lengthStr).intValue();
                array = Array.newInstance(arrayType, length);

                /**
                 * boolean isString =  (arrayType.equals (String.class));
                 * Constructor ctor  = getPrimitiveCtor (arrayType);
                 * String className = arrayType.getName ();
                 * boolean isChar =  (className.equals ("char") || className.equals ("java.lang.Character"));
                 * Class componentType = arrayType.getComponentType();
                 */
                List   strings = StringUtil.split(value, ",");
                Object object  = null;
                String arrayValue;
                for (int i = 0; i < length; i++) {
                    arrayValue = (String) strings.get(i);
                    object     = createPrimitiveObject(arrayType, arrayValue);

                    /**
                     * if (isString)
                     *   object = arrayValue;
                     * else if (isChar)
                     *   object =     new Character (arrayValue.charAt (0));
                     * else  if (ctor !=null)
                     *   object = ctor.newInstance (new Object[]{arrayValue});
                     *   else continue;
                     */
                    Array.set(array, i, object);
                }
                return new ObjectClass(array);
            }
        } catch (Exception exc) {
            logException("Error creating primitive array.", exc);
        }
        return null;
    }


    /**
     *  Create the primitive object defined by the given class and xml element.
     *
     *  @param primitiveClass The class of the primitive.
     *  @param element The xml representation of the primitive.
     *  @return The primitive object and its class.
     */

    public ObjectClass createPrimitiveObject(Class primitiveClass,
                                             Element element) {
        String   tagName  = element.getTagName();
        String   value    = null;
        NodeList children = element.getChildNodes();

        if (getAttribute(element, ATTR_NULL, false)) {
            return new ObjectClass(null, primitiveClass);
        }

        boolean isString = primitiveClass.equals(String.class);

        //If this is a String then check if there is a stringvalue attribute
        //If there is then it means that the string length was 0
        //we do this because a 0 length string in a CDATA section is dropped
        //by the parser.
        if (isString) {
            String stringValue = getAttribute(element, ATTR_STRINGVALUE,
                                     NULL_STRING);
            if (stringValue != null) {
                return new ObjectClass(stringValue, primitiveClass);
            }
        }

        //First look for cdata
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.CDATA_SECTION_NODE) {
                value = n.getNodeValue();
                break;
            }
        }

        //If no cdata then look for a text node
        if (value == null) {
            for (int i = 0; i < children.getLength(); i++) {
                Node n = children.item(i);
                if (n.getNodeType() == Node.TEXT_NODE) {
                    value = n.getNodeValue();
                    break;
                }
            }
        }


        if (value == null) {
            //This check is here to fix any problems with xml files that
            //were saved before the fix about zero lenght strings was added.
            //It seems as though if we write out a CDATA section with a blank
            //string then when we parse the xml we don't get a CDATA section.
            //So we are assuming here that if this is a string and there was no value
            //found then we return an empty string. This of course will screw up
            //any thing where a null string was saved (but only for those legacy files
            //created before the fix).
            if (isString) {
                return new ObjectClass("", primitiveClass);
            }
            return new ObjectClass(null, primitiveClass);
        }

        String encodingValue = getAttribute(element, ATTR_ENCODING,
                                            NULL_STRING);
        if (encodingValue != null) {
            if (encodingValue.equals(VALUE_BASE64)) {
                value = new String(XmlUtil.decodeBase64(value));
            }
        }

        Object object = createPrimitiveObject(primitiveClass, value);
        if (object == null) {
            System.err.println("NULL: " + primitiveClass.getName() + " "
                               + value);
            return null;
        }
        return new ObjectClass(object, primitiveClass);
    }

    /**
     *  Create the primitive object defined by the given class and value.
     *
     *  @param primitiveClass The class of the primitive.
     *  @param value The String representation of the value.
     *  @return The primitive object.
     */
    public Object createPrimitiveObject(Class primitiveClass, String value) {
        try {
            if (primitiveClass.equals(String.class)) {
                return value;
            }
            Constructor ctor = getPrimitiveCtor(primitiveClass);
            if (ctor != null) {
                return ctor.newInstance(new Object[] { value });
            }
            String className = primitiveClass.getName();

            //Character is special handling because it does not take a String
            if (className.equals("char")
                    || className.equals("java.lang.Character")) {
                return new Character(value.charAt(0));
            }
        } catch (Exception exc) {
            //Check for Double or Long objects with NaN, Infinity or -Infinity
            //This fails on jdk1.3
            if (exc instanceof java.lang.reflect.InvocationTargetException) {
                if (value.equals("NaN")) {
                    if (primitiveClass.equals(Double.class)) {
                        return new Double(Double.NaN);
                    }
                    if (primitiveClass.equals(Float.class)) {
                        return new Float(Float.NaN);
                    }
                } else if (value.equals("Infinity")) {
                    if (primitiveClass.equals(Double.class)) {
                        return new Double(Double.POSITIVE_INFINITY);
                    }
                    if (primitiveClass.equals(Float.class)) {
                        return new Float(Float.POSITIVE_INFINITY);
                    }
                } else if (value.equals("-Infinity")) {
                    if (primitiveClass.equals(Double.class)) {
                        return new Double(Double.NEGATIVE_INFINITY);
                    }
                    if (primitiveClass.equals(Float.class)) {
                        return new Float(Float.NEGATIVE_INFINITY);
                    }
                }
            }



            logException("Error creating primitive type: "
                         + primitiveClass.getName(), exc);
        }
        return null;
    }


    /**
     *  Deserialize the serialized object defined by the given parent xml element.
     *
     *  @param parent Contains the serialization xml.
     *  @return The new object and its class.
     */
    public ObjectClass createSerializedObject(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                byte[] bytes = XmlUtil.decodeBase64(child.getNodeValue());
                try {
                    ByteArrayInputStream istream =
                        new ByteArrayInputStream(bytes);
                    ObjectInputStream p = new ObjectInputStream(istream);
                    Object            theObject = p.readObject();

                    //If there was an id attribute then map the id to the new object
                    String id = getAttribute(parent, ATTR_ID, NULL_STRING);
                    if (id != null) {
                        setObject(id, theObject);
                    }
                    return new ObjectClass(theObject);
                } catch (Exception exc) {
                    logException("Error deserializing object.", exc);
                    return null;
                }
            }
        }
        System.err.println("No CDATA node found in serial tag");
        return null;
    }

    /**
     *  Create the object defined by the given xml.
     *
     *  @param element Defines the object.
     *  @return The object with its class.
     */
    public ObjectClass createObjectInner(Element element) {
        return createObjectInner(element, true);
    }


    /**
     *  This method creates both the Object that the Element element
     *  represents and its Class.  We keep track of the Class separately to handle
     *  primitive types (e.g., int, boolean, double).
     *
     *  @param element The xml element that represents the encoded object.
     *  @param checkDelegate Should we check if there is an {@link XmlDelegate} for this object.
     *  @return The pair (object, Class).
     */
    public ObjectClass createObjectInner(Element element,
                                         boolean checkDelegate) {

        String tagName = element.getTagName();

        //Check for null tag (i.e., <null> or <null class=...>
        if (tagName.equals(TAG_NULL)) {
            try {
                String nullClassName = getAttribute(element, ATTR_CLASS,
                                           NULL_STRING);
                if (nullClassName != null) {
                    return new ObjectClass(null, getClass(nullClassName));
                }
            } catch (Exception exc) {}
            return new ObjectClass(null, null);
        }


        //Check if the tag name is one of the primitive classes, e.g., <int ...>,
        //<double ...>, etc. 
        Class primitiveClass = getPrimitiveClass(tagName);
        if (primitiveClass != null) {
            //      pr ("createPrimitiveObject:" + XmlUtil.toString (element));
            return createPrimitiveObject(primitiveClass, element);
        }

        //Have we seen this object before? (i.e., <object idref=...>)
        String idref = getAttribute(element, ATTR_IDREF, NULL_STRING);
        if (idref != null) {
            Object object = getObjectFromId(idref);
            if (object == null) {
                //TODO: should we throw an exception here?
                //System.err.println ("Got null idref:" + idref);
            }
            return new ObjectClass(object);
        }

        //Check if it's an array.
        if (tagName.equals(TAG_ARRAY)) {
            return createArrayObject(element);
        }

        //Check if it's an primitive array.
        if (tagName.equals(TAG_PARRAY)) {
            return createPrimitiveArrayObject(element);
        }

        if (tagName.equals(TAG_SERIAL)) {
            return createSerializedObject(element);
        }

        if ( !(tagName.equals(TAG_OBJECT) || tagName.equals("o") || tagName.equals(TAG_FACTORY))) {
            logException("Unknown tag: " + tagName,
                         new IllegalArgumentException(""));
            return null;
        }

        Object newObject = null;
        String className = element.getAttribute(ATTR_CLASS);

        //If the tag is a factory tag (i.e., <factory class=...>) then
        //instantiate the factory object and ask it to create the real object
        try {
            if (tagName.equals(TAG_FACTORY)) {
                XmlObjectFactory factory =
                    (XmlObjectFactory) getClass(className).newInstance();
                newObject = factory.getObject(this, element);
            } else {
                //Else it is an "object" tag (i.e., <object class=...>)
                Element constructorNode = XmlUtil.findChild(element,
                                              TAG_CONSTRUCTOR);
                if (constructorNode != null) {
                    NodeList children = XmlUtil.getElements(constructorNode);
                    Object[] args     = new Object[children.getLength()];
                    Class[]  types    = new Class[children.getLength()];
                    for (int i = 0; i < children.getLength(); i++) {
                        ObjectClass oc =
                            createObjectInner((Element) children.item(i));
                        if (oc != null) {
                            args[i]  = oc.object;
                            types[i] = oc.type;
                        } else {
                            args[i]  = null;
                            types[i] = Object.class;
                        }
                    }
                    Class theClass = getClass(className);
                    Constructor ctor =
                        Misc.findConstructor(getClass(className), types);
                    if (ctor == null) {
                        System.err.println(
                            "Error: Unable to find constructor for class: "
                            + className + " with types:"
                            + StringUtil.toString(types) + "\n"
                            + XmlUtil.toString(element));
                        return null;
                    }
                    newObject = ctor.newInstance(args);

                } else {
                    Class       theClass = getClass(className);
                    XmlDelegate delegate = (checkDelegate
                                            ? getDelegate(theClass)
                                            : null);
                    if (delegate != null) {
                        newObject = delegate.createObject(this, element);
                        //If the delegate returns null then simply return  the ObjectClass here.
                        if (newObject == null) {
                            return new ObjectClass(newObject, theClass);
                        }
                    } else {
                        newObject = getClass(className).newInstance();
                    }
                }
            }
        } catch (Exception exc) {
            //            System.err.println("Error creating object:" + className + "\n"
            //                               + XmlUtil.toString(element) + "\n" + exc);
            logException("Error creating object: " + className, exc);
            return null;
        }


        //Make sure we have an object
        if (newObject == null) {
            return null;
        }


        //If there was an id attribute then map the id to the new object
        String id = getAttribute(element, ATTR_ID, NULL_STRING);
        if (id != null) {
            setObject(id, newObject);
        }

        //If the object is an XmlPersistable then have it initialize itself
        if (newObject instanceof XmlPersistable) {
            //If the init call returns false then return here and do not do the
            //method and property  initialization
            boolean okToProceed =
                ((XmlPersistable) newObject).initFromXml(this, element);
            if ( !okToProceed) {
                return new ObjectClass(newObject);
            }
        }

        //Evaluate any  method and property tags
        NodeList children = XmlUtil.getElements(element);
        for (int i = 0; i < children.getLength(); i++) {
            Element child     = (Element) children.item(i);
            String  childName = child.getTagName();
            if (childName.equals(TAG_METHOD)) {
                String methodName = child.getAttribute(ATTR_NAME);
                if ( !methodOk(methodName)) {
                    logException("Unknown method: " + methodName,
                                 new IllegalArgumentException());
                    return null;
                }
                invokeMethod(newObject, methodName, child);
            } else if (childName.equals(TAG_FIELD)) {
                invokeField(newObject, child.getAttribute(ATTR_NAME), child);
            } else if (childName.equals(TAG_PROPERTY)) {
                invokeMethod(newObject,
                             "set" + child.getAttribute(ATTR_NAME), child);
            } else if (childName.equals(TAG_CONSTRUCTOR)) {}
            else {
                if (child.getAttribute(TAG_IGNORE) == null) {
                    //              System.err.println ("Unknown tag: " + childName);
                }
            }
        }


        try {
            Method theMethod = newObject.getClass().getMethod(METHOD_INIT,
                                   ENCODER_ARRAY);
            if (theMethod != null) {
                theMethod.invoke(newObject, new Object[] { this });
            }
        } catch (NoSuchMethodException nsme) {}
        catch (Exception exc) {
            logException("Error calling method: " + METHOD_INIT, exc);
        }


        //Do we swap this new object for a seed object?
        Object seedObject = getSeedObject(newObject);
        if (seedObject != null) {
            newObject = seedObject;
            //If we do, then  make sure we replace the id->object mapping
            if (id != null) {
                setObject(id, newObject);
            }
        }
        return new ObjectClass(newObject);
    }




    /**
     *  Invoke the  the method, identified by the given method name, on the given object.
     *  Use the children of the given parent xml node as the arguments to the method.
     *
     *  @param object The object to set the field value of.
     *  @param methodName The name of the method.
     *  @param element The parent xml node that contains the argument object values.
     */
    protected void invokeMethod(Object object, String methodName,
                                Element element) {
        NodeList children   = getElements(element);
        Class[]  paramTypes = new Class[children.getLength()];
        Object[] params     = new Object[children.getLength()];


        pr("invokeMethod:" + methodName);
        //Assemble the list of parameter types and parameter objects
        //We separate the types from the objects to handle primitive types, e.g.,
        //int, double, etc.
        for (int i = 0; i < params.length; i++) {
            Element     child = (Element) children.item(i);
            ObjectClass oc    = createObjectInner(child);
            if (oc == null) {
                pr("InvokeMethod - child is null");
                return;
            }
            paramTypes[i] = oc.type;
            params[i]     = oc.object;
        }

        try {
            //TODO: Cache the results of method lookup.
            Method theMethod = Misc.findMethod(object.getClass(), methodName,
                                   paramTypes);
            if (theMethod == null) {
                /*
                for (int i = 0; i < paramTypes.length; i++) {
                    System.err.println("type:" + paramTypes[i]);
                }
                Method[]  methods = object.getClass().getMethods();
                for (int i = 0; i < methods.length; i++) {
                    if(methodName.equals(methods[i].getName())) {
                        Class[]types = methods[i].getParameterTypes();
                        for (int j = 0; j < types.length; j++) {
                        }
                    }
                }
                */

                throw new IllegalArgumentException("Unable to find method: "
                        + object.getClass().getName() + "." + methodName);
            } else {
                theMethod.invoke(object, params);
            }
        } catch (Exception exc) {
            String paramString = "";
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0) {
                    paramString += ", ";
                }
                if (paramTypes[i] == null) {
                    paramString += "null";
                } else {
                    paramString += paramTypes[i].getName();
                }
            }
            logException("Error invoking method: "
                         + getClassName(object.getClass()) + "." + methodName
                         + "(" + paramString + ")", exc);

        }
    }


    /**
     *  Set the field, identified by the given field name, on the given object,
     *  to the value  defined by the first xml child of the given parent.
     *
     *  @param object The object to set the field value of.
     *  @param fieldName The name of the field.
     *  @param parent The parent xml node that contains the object value.
     */
    protected void invokeField(Object object, String fieldName,
                               Element parent) {
        try {
            Element child = getFirstChild(parent);
            Field   field = object.getClass().getField(fieldName);
            if (field == null) {
                return;
            }
            ObjectClass oc = createObjectInner(child);
            if (oc == null) {
                return;
            }
            field.set(object, oc.object);
        } catch (NoSuchFieldException nsfe) {}
        catch (IllegalAccessException iae) {}
        catch (Exception exc) {
            logException("Error invoking field: " + fieldName, exc);
        }
    }


    /**
     *  This acts as a wrapper around   {@link  #createElement (Object,  boolean) },
     *  passing in true for the  checkPersistable flag.
     *
     *  @param object The object to create the xml element for.
     *  @return The xml representation of the object.
     */
    public Element createElement(Object object) {
        return createElement(object, true);
    }


    /**
     *  This acts as a wrapper around {@link  #createElement(Object,Class,boolean,boolean)},
     *  passing in false for the  checkPersistable flag and true for the checkDelegate flag.
     *
     *  @param object The object to create the xml element for.
     *  @return The xml representation of the object.
     */
    public Element createElementDontCheckPersistable(Object object) {
        return createElement(object, object.getClass(), false, true);
    }

    /**
     *  This acts as a wrapper around {@link  #createElement(Object,boolean)},
     *  passing in false for the checkDelegate flag.
     *
     *  @param object The object to create the xml element for.
     *  @return The xml representation of the object.
     */
    public Element createElementDontCheckDelegate(Object object) {
        return createElement(object, false);
    }



    /**
     *  This acts as a wrapper around {@link  #createElement(Object,Class,boolean,boolean)},
     *  passing in true for the  checkPersistable flag.
     *
     *  @param object The object to create the xml element for.
     *  @param checkDelegate Should we check if the object has an {@link XmlDelegate} that handles it.
     *  @return The xml representation of the object.
     */
    public Element createElement(Object object, boolean checkDelegate) {
        if (object == null) {
            return createNullElement();
        }
        return createElement(object, object.getClass(), true, checkDelegate);
    }


    /**
     *  This acts as a wrapper around {@link  #createElement(Object,Class,boolean,boolean)},
     *  passing in true and true for the  checkPersistable and  checkDelegate flags.
     *
     *  @param object The object to create the xml element for.
     *  @param theClass Its class.
     *  @return The xml representation of the object.
     */
    protected Element createElement(Object object, Class theClass) {
        return createElement(object, theClass, true, true);
    }

    /**
     *  Create a xml representation of the object.  The object may be anything, null,
     *  a primitive, an array or a regular object. We pass the class of the object in in case
     *  it is null. The flags tell  whether to check if the object is a XmlPersistable
     *  or has a delegate (the default is to check).
     *
     *  @param object The object  to encode.
     *  @param theClass The Class of the object.
     *  @param checkPersistable Do we defer to the object if is is an {@link XmlPersistable}
     *  @param checkDelegate Do we defer to the delegate if the object  has an {@link XmlDelegate}
     *  @return The xml representation of the object.
     */
    protected Element createElement(Object object, Class theClass,
                                    boolean checkPersistable,
                                    boolean checkDelegate) {

        if (object == null) {
            return createNullElement(theClass);
        }

        String primitiveName = getPrimitiveName(theClass);
        //If this is a primitive (e.g., Integer, int, double, etc.)
        //then output a special Element  (e.g., <c_int value=...>, <int value=...>, ...)
        if (primitiveName != null) {
            return createPrimitiveElement(primitiveName, object);
        }

        //Now check if this object is an array
        if (theClass.isArray()) {
            return createArrayElement(object);
        }


        //Have we seen this object before?
        String id = (String) getObjectId(object);
        if (id != null) {
            //If we have seen it  then output an idref Element
            Element oldElement = getElementForObject(object);
            //Make sure the original element has an id attribute
            if ((oldElement != null) && !oldElement.hasAttribute(ATTR_ID)) {
                oldElement.setAttribute(ATTR_ID, id);
            }
            return createReferenceElement(id);
        }

        Element newElement = null;


        //If the object we are persisting is a XmlPersistable
        //Then it handles the creation of the DOM    
        if (checkPersistable && (object instanceof XmlPersistable)) {
            newElement = ((XmlPersistable) object).createElement(this);
            setObjectId(object, newElement);
        } else {
            //Do we have a delegate?
            XmlDelegate delegate = (checkDelegate
                                    ? getDelegate(theClass)
                                    : null);
            if (delegate != null) {
                newElement = delegate.createElement(this, object);
                if (newElement == null) {
                    return null;
                }
                setObjectId(object, newElement);
            } else {
                newElement = createElementForObject(object, theClass);
            }
        }


        return newElement;
    }


    /**
     *  Check if the given class has a parameterless ctor.
     *
     * @param theClass
     * @return
     */
    private boolean isClassCtorOk(Class theClass) {
        Boolean ok = (Boolean) classCtorsOk.get(theClass);
        if (ok != null) {
            return ok.booleanValue();
        }
        boolean ctorOk = true;
        try {
            theClass.newInstance();
        } catch (InstantiationException ie) {
            ctorOk = false;
        } catch (IllegalAccessException iae) {
            ctorOk = false;
        }
        classCtorsOk.put(theClass, new Boolean(ctorOk));
        return ctorOk;
    }



    /**
     *  Construct the Xml representation of the given object. This object is just a regular
     *  Object, not a primitive, array, etc.
     *
     *  @param object The regular object.
     *  @param theClass The class of the object. We have this here in case the object is null.
     *  @return The xml representation of the object.
     */
    public Element createElementForObject(Object object, Class theClass) {
        Element newElement = null;
        //Make sure this object is a bean, i.e.,  has an argument-less ctor
        boolean ctorOk = isClassCtorOk(theClass);
        if ( !ctorOk) {
            if (object instanceof Serializable) {
                try {
                    System.err.println("Serializing: " + theClass.getName());
                    ByteArrayOutputStream ostream =
                        new ByteArrayOutputStream();
                    ObjectOutputStream p = new ObjectOutputStream(ostream);
                    p.writeObject(object);
                    p.flush();
                    ostream.close();
                    byte[] bytes = ostream.toByteArray();
                    newElement = createSerialElement(theClass,
                            new String(XmlUtil.encodeBase64(bytes)));
                    ctorOk = true;
                } catch (Exception exc) {
                    logException("Error serializing class: "
                                 + theClass.getName(), exc);
                }
            }
        }


        if ( !ctorOk) {
            Misc.printStack("Invalid constructor for: "
                            + getClassName(theClass), 10, null);
            return createElementForObject("INVALID", String.class);
            //      return null;
        }

        if (newElement == null) {
            newElement = createObjectElement(theClass);
        }

        setObjectId(object, newElement);
        Hashtable propertyNames = new Hashtable();
        XmlUtil.addChildren(newElement,
                            getPropertyElements(object, propertyNames));
        XmlUtil.addChildren(newElement,
                            getFieldElements(object, propertyNames));
        List specialCaseElements = getSpecialCaseElements(object);
        if (specialCaseElements != null) {
            XmlUtil.addChildren(newElement, specialCaseElements);
        }
        return newElement;
    }


    /**
     *  Return a List of Xml Elements that represent the set of public fields in the given object.
     *
     *  @param object The object to look for fields in.
     *  @param propertyNames These represent properties that are already being persisted.
     *  @return A List of xml "field" elements.
     */
    public List getFieldElements(Object object, Hashtable propertyNames) {
        List    elements = new ArrayList();
        Field[] fields   = object.getClass().getFields();
        for (int i = 0; i < fields.length; i++) {
            int modifiers = fields[i].getModifiers();
            if ( !Modifier.isPublic(modifiers)
                    || Modifier.isStatic(modifiers)
                    || Modifier.isFinal(modifiers)
                    || Modifier.isStatic(modifiers)) {
                continue;
            }
            if (propertyNames.get(fields[i].getName().toLowerCase())
                    != null) {
                continue;
            }
            try {
                Element element = newElement(TAG_FIELD);
                element.setAttribute(ATTR_NAME, fields[i].getName());
                element.appendChild(createElement(fields[i].get(object)));
                elements.add(element);
            } catch (IllegalAccessException iae) {}
        }
        return elements;
    }




    /**
     *  Return a List of Xml Elements that represent the set of public properties in the given object.
     *  A property is denoted with public  set/get  methods of the same type.
     *
     *  @param object The object to look for properties in.
     *  @param propertyNames Put the property names in here so we can keep them unique.
     *  @return A List of xml "property" elements.
     */
    public List getPropertyElements(Object object, Hashtable propertyNames) {
        //      System.err.println ("getProperty - " + object.getClass ().getName ());
        List  elements = new ArrayList();
        Class theClass = object.getClass();
        //Find the prototype object  for this class
        Object prototype = prototypes.get(theClass);
        if (prototype == null) {
            //Don't do prototypes for classes with delegates
            if (getDelegate(theClass) != null) {
                prototype = NO_PROTOTYPE;
            } else {
                try {
                    prototype = theClass.newInstance();
                } catch (Exception exc) {
                    prototype = NO_PROTOTYPE;
                }
            }
            prototypes.put(theClass, prototype);
        }
        if (prototype == NO_PROTOTYPE) {
            prototype = null;
        }
        //      prototype = null;
        List methods = findPropertyMethods(theClass);
        for (int i = 0; i < methods.size(); i++) {
            Method getter = (Method) methods.get(i);
            try {
                Object value = getter.invoke(object, new Object[] {});
                //See if we have a prototype
                if (prototype != null) {
                    try {
                        //If the value is the same as the prototype's then skip this
                        Object protoTypeValue = getter.invoke(prototype,
                                                    new Object[] {});
                        if (Misc.equals(value, protoTypeValue)) {
                            //System.err.println ("skipping: " +getter);
                            continue;
                        }
                    } catch (Exception protoExc) {}
                }
                Class  returnType    = getter.getReturnType();
                String primitiveName = getPrimitiveName(returnType);
                String methodName    = getter.getName();
                //              System.err.println ("   " + methodName);
                Element valueElement;
                if (primitiveName != null) {
                    valueElement = createPrimitiveElement(primitiveName,
                            value);
                } else {
                    if (value == null) {
                        valueElement = createNullElement(returnType);
                    } else {
                        valueElement = createElement(value);
                    }
                }
                if (valueElement != null) {
                    String propertyName = methodName.substring(3);
                    if (propertyNames != null) {
                        propertyNames.put(propertyName.toLowerCase(),
                                          propertyName);
                    }
                    elements.add(createPropertyElement(propertyName,
                            valueElement));
                }
            } catch (Exception exc) {
                logException("Error evaluating method: " + getter.getName()
                             + "\n", exc);

            }
        }
        return elements;
    }


    /**
     *  Handle certain objects in a special way. For List-s return a set of "add" method
     *  Elements. For a Hashtable return a set of "put" method elements.
     *
     *  @param object The object to check for special case handling.
     *  @return A list of xml Elements or null if the object is not special.
     */
    public List getSpecialCaseElements(Object object) {
        if (object instanceof List) {
            List elements = new ArrayList();
            List v        = (List) object;
            for (int i = 0; i < v.size(); i++) {
                Element argumentElement = createElement(v.get(i));
                if (argumentElement != null) {
                    elements.add(createMethodElement(METHOD_ADD,
                            argumentElement));
                }
            }
            return elements;
        }

        if (object instanceof Hashtable) {
            List      elements = new ArrayList();
            Hashtable ht       = (Hashtable) object;
            for (Enumeration keys = ht.keys(); keys.hasMoreElements(); ) {
                Object  key           = keys.nextElement();
                Element methodElement = createMethodElement("put");
                methodElement.appendChild(createElement(key));
                methodElement.appendChild(createElement(ht.get(key)));
                elements.add(methodElement);
            }
            return elements;
        }



        /*
        if (object instanceof HashSet) {
            List      elements = new ArrayList();
            HashSet ht       = (HashSet) object;
            for (Enumeration keys = ht.keys(); keys.hasMoreElements(); ) {
                Object  key           = keys.nextElement();
                Element methodElement = createMethodElement(METHOD_ADD);
                methodElement.appendChild(createElement(key));
                elements.add(methodElement);
            }
            return elements;
            }*/
        return null;
    }





    /**
     * Just some test code
     *
     * @param r node to recurse
     * @param tab  pretty print
     */
    private static void recurse(Node r, String tab) {
        System.err.println(tab + "Node:" + r);

        NodeList children    = r.getChildNodes();
        int      numChildren = children.getLength();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            recurse(child, tab + "  ");
        }
    }

    /**
     *  Add the given error message and exception to the list of errors.
     *
     *  @param message The error message.
     *  @param exc The exception.
     */

    private void logException(String message, Exception exc) {
        if (exceptions == null) {
            exceptions    = new ArrayList();
            errorMessages = new ArrayList();
        }
        errorMessages.add(message);
        exceptions.add(exc);
    }



    /**
     *  A utility method to print out an error message if we are in debug mode.
     *
     *  @param msg The message.
     */
    public static void pr(String msg) {
        if (debug) {
            System.err.println(msg);
        }
    }



    /**
     *  A simple test method.
     *
     *  @param o The object to encode.
     *  @param printXml Do we println the result xml.
     *  @return Did we pass the test.
     *  @throws Exception When something goes bad.
     */
    public boolean test(Object o, boolean printXml) throws Exception {

        String xml1 = toXml(o);
        if (printXml) {
            System.out.println(xml1);
            return true;
        }

        String  xml2 = toXml(toObject(xml1));

        boolean ok   = xml1.equals(xml2);
        if ( !ok) {
            System.out.println("Not ok");
            System.out.println("First xml:");
            System.out.println(xml1);
            System.out.println("******************\nSecond xml:");
            System.out.println(xml2);
        } else if (printXml) {
            System.out.println(xml2);
        } else {
            System.out.println("Ok");
        }
        return ok;
    }



    /**
     *  Runs some tests.
     *
     *  @param args The command line arguments.
     */
    public static void main(String[] args) {

        try {
            XmlEncoder enc1 = new XmlEncoder();
            String xml = enc1.toXml(new Date());
            Date date = (Date) enc1.toObject(xml);
            System.err.println ("Date:" + date);

            /*            String xmltest = IOUtil.readContents("test.xml",
                                 XmlEncoder.class, (String) null);
            XmlEncoder enc1 = new XmlEncoder();
            HashSet hs = new HashSet();
            hs.add("foo");
            HashSet h2   = (HashSet) enc1.toObject(hs);
            System.err.println("o1:" + h2);
            */
        } catch (Exception exc) {
            System.err.println("OOps:" + exc);
        }


        /*
        Test testit = new Test (new Double (Double.NaN),
                                new Double (Double.POSITIVE_INFINITY),
                                new Double (Double.NEGATIVE_INFINITY));

        try {
            XmlEncoder testEncoder = new XmlEncoder ();
            String xml =testEncoder.toXml (testit);
            System.err.println (xml);
            Test newTest = (Test) testEncoder.toObject (xml);
            System.err.println ("newTest = " + newTest);
        } catch (Exception exc) {
            System.err.println ("OOps:" + exc);
        }


        if (true) return;

        try {
            String xml = "<foo><string><![CDATA[x]]></string></foo>";
            Element root = getRoot (xml);
            System.err.println (XmlUtil.toString (root));
            recurse (root, "");

        } catch (Exception exc) {
            System.err.println ("OOps:" + exc);
        }
        if (true) return;


        if (args.length != 0) {
            try {
                XmlEncoder enc1 = new XmlEncoder ();
                enc1.toObject ( IOUtil.readContents (new File (args[0])));
            } catch (Exception exc) {
                System.err.println ("Error:" + exc);
            }
            System.exit (0);
        }

        try {
            XmlEncoder enc1 = new XmlEncoder ();
            String xml1 = enc1.toXml (new TestSerial ("hello there jeff"));
            System.err.println (xml1);
            Object o1 = enc1.toObject (xml1);
            System.err.println ("Object:"+ o1);
            System.exit (0);
        } catch (Exception exc) {
            System.err.println ("Error:" + exc);
        }


        Test1 t = new Test1 ();
        Hashtable ht = new Hashtable ();
        ht.put ("Hello there jeff", t);
        ht.put ("I am fine", t);
        List v = new ArrayList ();
        List v2 = new ArrayList ();
        v.add (t);
        v.add (v2);
        v2.add ("String 2");
        v2.add (ht);
        v.add ("String 2");
        v.add (new Test2 (5));
        v.add (new Test2 (77));
        v.add (t);
        v.add (new Integer (33));
        v.add (ht);
        v.add (ht);
        v.add (new Test3 ());


        int[][] twodarray ={{1,2,3},{4,5,6}};
        Object[] oa = {new Test1 ()};
        int [] intArray = {1,2,3,4,5};
        v.add (oa);
        v.add (intArray);
        v.add (twodarray);

        */

    }


}
