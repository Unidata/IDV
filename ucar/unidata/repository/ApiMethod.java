/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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




package ucar.unidata.repository;


import java.lang.reflect.Method;

import java.util.Hashtable;


/**
 */

public class ApiMethod {

    /** _more_          */
    private String request;

    /** _more_          */
    private Permission permission;

    /** _more_          */
    private Method method;

    /** _more_          */
    private boolean canCache = false;

    /**
     * _more_
     *
     * @param request _more_
     * @param permission _more_
     * @param method _more_
     * @param canCache _more_
     */
    public ApiMethod(String request, Permission permission, Method method,
                     boolean canCache) {
        this.request    = request;
        this.permission = permission;
        this.method     = method;
        this.canCache   = canCache;
    }

    /**
     * Set the Request property.
     *
     * @param value The new value for Request
     */
    public void setRequest(String value) {
        request = value;
    }

    /**
     * Get the Request property.
     *
     * @return The Request
     */
    public String getRequest() {
        return request;
    }

    /**
     * Set the Permission property.
     *
     * @param value The new value for Permission
     */
    public void setPermission(Permission value) {
        permission = value;
    }

    /**
     * Get the Permission property.
     *
     * @return The Permission
     */
    public Permission getPermission() {
        return permission;
    }

    /**
     * Set the Method property.
     *
     * @param value The new value for Method
     */
    public void setMethod(Method value) {
        method = value;
    }

    /**
     * Get the Method property.
     *
     * @return The Method
     */
    public Method getMethod() {
        return method;
    }




    /**
     * Set the CanCache property.
     *
     * @param value The new value for CanCache
     */
    public void setCanCache(boolean value) {
        canCache = value;
    }

    /**
     * Get the CanCache property.
     *
     * @return The CanCache
     */
    public boolean getCanCache() {
        return canCache;
    }




}

