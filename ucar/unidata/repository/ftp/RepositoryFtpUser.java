/*
 *
 * Copyright 1997-2009 Unidata Program Center/University Corporation for
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

package ucar.unidata.repository.ftp;


import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 */

public class RepositoryFtpUser implements User {

    /** _more_ */
    private String name = null;

    /** _more_ */
    private String password = null;

    /** _more_ */
    private int maxIdleTimeSec = 0;  // no limit

    /** _more_ */
    private String homeDir = null;

    /** _more_ */
    private boolean isEnabled = true;

    /** _more_ */
    private List<? extends Authority> authorities =
        new ArrayList<Authority>();

    /**
     * Default constructor.
     *
     * @param name _more_
     * @param password _more_
     */
    public RepositoryFtpUser(String name, String password) {
        this.name     = name;
        this.password = password;
    }

    /**
     * Copy constructor.
     *
     * @param user _more_
     */
    public RepositoryFtpUser(User user) {
        name           = user.getName();
        password       = user.getPassword();
        authorities    = user.getAuthorities();
        maxIdleTimeSec = user.getMaxIdleTime();
        homeDir        = user.getHomeDirectory();
        isEnabled      = user.getEnabled();
    }

    /**
     * Get the user name.
     *
     * @return _more_
     */
    public String getName() {
        return name;
    }

    /**
     * Set user name.
     *
     * @param name _more_
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the user password.
     *
     * @return _more_
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set user password.
     *
     * @param pass _more_
     */
    public void setPassword(String pass) {
        password = pass;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Authority> getAuthorities() {
        if (authorities != null) {
            return Collections.unmodifiableList(authorities);
        } else {
            return null;
        }
    }

    /**
     * _more_
     *
     * @param authorities _more_
     */
    public void setAuthorities(List<Authority> authorities) {
        if (authorities != null) {
            this.authorities = Collections.unmodifiableList(authorities);
        } else {
            this.authorities = null;
        }
    }

    /**
     * Get the maximum idle time in second.
     *
     * @return _more_
     */
    public int getMaxIdleTime() {
        return maxIdleTimeSec;
    }

    /**
     * Set the maximum idle time in second.
     *
     * @param idleSec _more_
     */
    public void setMaxIdleTime(int idleSec) {
        maxIdleTimeSec = idleSec;
        if (maxIdleTimeSec < 0) {
            maxIdleTimeSec = 0;
        }
    }

    /**
     * Get the user enable status.
     *
     * @return _more_
     */
    public boolean getEnabled() {
        return isEnabled;
    }

    /**
     * Set the user enable status.
     *
     * @param enb _more_
     */
    public void setEnabled(boolean enb) {
        isEnabled = enb;
    }

    /**
     * Get the user home directory.
     *
     * @return _more_
     */
    public String getHomeDirectory() {
        return homeDir;
    }

    /**
     * Set the user home directory.
     *
     * @param home _more_
     */
    public void setHomeDirectory(String home) {
        homeDir = home;
    }

    /**
     * String representation.
     *
     * @return _more_
     */
    public String toString() {
        return name;
    }

    /**
     * {@inheritDoc}
     *
     * @param request _more_
     *
     * @return _more_
     */
    public AuthorizationRequest authorize(AuthorizationRequest request) {
        System.err.println("Authorize:" + request);
        if (true) {
            return request;
        }
        // check for no authorities at all
        if (authorities == null) {
            return null;
        }

        boolean someoneCouldAuthorize = false;
        for (Authority authority : authorities) {
            if (authority.canAuthorize(request)) {
                someoneCouldAuthorize = true;
                request               = authority.authorize(request);
                // authorization failed, return null
                if (request == null) {
                    return null;
                }
            }

        }

        if (someoneCouldAuthorize) {
            return request;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param clazz _more_
     *
     * @return _more_
     */
    public List<Authority> getAuthorities(Class<? extends Authority> clazz) {
        List<Authority> selected = new ArrayList<Authority>();

        for (Authority authority : authorities) {
            if (authority.getClass().equals(clazz)) {
                selected.add(authority);
            }
        }

        return selected;
    }
}

