/*
 * $Id: WrapperException.java,v 1.9 2006/12/13 13:34:38 jeffmc Exp $
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
 * Wraps an exception as a RuntimeException
 *
 * @author Unidata Development Team
 * @version $Revision: 1.9 $
 */
public class WrapperException extends RuntimeException {

    /** inner exception */
    private Throwable inner;

    /**
     * Wrap a throwable in this Exception
     *
     * @param msg    message to display
     * @param inner  wrapped Throwable
     *
     */
    public WrapperException(String msg, Throwable inner) {
        super(msg);
        this.inner = inner;
    }

    /**
     * Wrap a throwable in this Exception
     *
     * @param inner  wrapped Throwable
     *
     */
    public WrapperException(Throwable inner) {
        this.inner = inner;
    }

    /**
     * Print the stack trace for the wrapped exception
     */
    public void printStackTrace() {
        if (inner != null) {
            inner.printStackTrace();
        }
    }

    /**
     * Get the wrapped exception
     * @return wrapped exception
     */
    public Throwable getException() {
        return inner;
    }


    /**
     * Get a String representation of this.
     * @return a String representation of this.
     */
    public String toString() {
        return super.toString() + ((inner != null)
                                   ? inner.toString()
                                   : "");
    }



}

