/*
 * $Id: HashCodeUtils.java,v 1.3 2006/05/05 19:19:34 jeffmc Exp $
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




package ucar.unidata.util;






/**
 * Taken from http://www.javapractices.com/Topic28.cjp.
 * <p>
 * Collected methods which allow easy implementation of <code>hashCode</code>.
 * <p>
 * Example use case.
 * <pre>
 *  public int hashCode(){
 *    int result = HashCodeUtil.SEED;
 *    //collect the contributions of various fields
 *    result = HashCodeUtil.hash(result, fPrimitive);
 *    result = HashCodeUtil.hash(result, fObject);
 *    result = HashCodeUtil.hash(result, fArray);
 *    return result;
 *  }
 * </pre>
 */
public class HashCodeUtils {

    /**
     * An initial value for a <code>hashCode</code>, to which is added contributions
     * from fields. Using a non-zero value decreases collisons of <code>hashCode</code>
     * values.
     */
    public static final int SEED = 23;

    /**
     * booleans.
     *
     * @param aSeed _more_
     * @param aBoolean _more_
     *
     * @return _more_
     */
    public static int hash(int aSeed, boolean aBoolean) {
        return firstTerm(aSeed) + (aBoolean
                                   ? 1
                                   : 0);
    }

    /**
     * chars.
     *
     * @param aSeed _more_
     * @param aChar _more_
     *
     * @return _more_
     */
    public static int hash(int aSeed, char aChar) {
        return firstTerm(aSeed) + (int) aChar;
    }

    /**
     * ints.
     *
     * @param aSeed _more_
     * @param aInt _more_
     *
     * @return _more_
     */
    public static int hash(int aSeed, int aInt) {
        /*
        * Implementation Note
        * Note that byte and short are handled by this method, through
        * implicit conversion.
        */
        return firstTerm(aSeed) + aInt;
    }

    /**
     * longs.
     *
     * @param aSeed _more_
     * @param aLong _more_
     *
     * @return _more_
     */
    public static int hash(int aSeed, long aLong) {
        return firstTerm(aSeed) + (int) (aLong ^ (aLong >>> 32));
    }

    /**
     * floats.
     *
     * @param aSeed _more_
     * @param aFloat _more_
     *
     * @return _more_
     */
    public static int hash(int aSeed, float aFloat) {
        return hash(aSeed, Float.floatToIntBits(aFloat));
    }

    /**
     * doubles.
     *
     * @param aSeed _more_
     * @param aDouble _more_
     *
     * @return _more_
     */
    public static int hash(int aSeed, double aDouble) {
        return hash(aSeed, Double.doubleToLongBits(aDouble));
    }


    /// PRIVATE ///

    /** _more_ */
    private static final int fODD_PRIME_NUMBER = 37;

    /**
     * _more_
     *
     * @param aSeed _more_
     *
     * @return _more_
     */
    private static int firstTerm(int aSeed) {
        return fODD_PRIME_NUMBER * aSeed;
    }

    /**
     * _more_
     *
     * @param aObject _more_
     *
     * @return _more_
     */
    private static boolean isArray(Object aObject) {
        return aObject.getClass().isArray();
    }
}

