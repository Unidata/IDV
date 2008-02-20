/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.util.Date;
import java.util.List;


/**
 * Class FilesInfo _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class User {

    /** _more_ */
    private String id = "";

    /** _more_ */
    private String name = "";

    /** _more_ */
    private String email = "";

    /** _more_ */
    private String question = "";

    /** _more_ */
    private String answer = "";

    /** _more_ */
    private String password = "";

    /** _more_ */
    private boolean admin = false;

    /** _more_ */
    private boolean anonymous = false;

    /** _more_ */
    private List<String> roles;

    /**
     * _more_
     */
    public User() {
        this.anonymous = true;
        this.name      = "anonymous";
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param admin _more_
     */
    public User(String id, boolean admin) {
        this.id    = id;
        this.admin = admin;
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param name _more_
     * @param admin _more_
     */
    public User(String id, String name, boolean admin) {
        this(id, admin);
        this.name = name;
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param name _more_
     * @param email _more_
     * @param question _more_
     * @param answer _more_
     * @param password _more_
     * @param admin _more_
     */
    public User(String id, String name, String email, String question,
                String answer, String password, boolean admin) {
        this.id       = id;
        this.name     = name;
        this.email    = email;
        this.question = question;
        this.answer   = answer;
        this.password = password;
        this.admin    = admin;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return Misc.hashcode(id) ^ Misc.hashcode(name) ^ (admin
                ? 1
                : 2) ^ (anonymous
                        ? 1
                        : 2);
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !o.getClass().equals(getClass())) {
            return false;
        }
        User that = (User) o;
        return Misc.equals(this.id, that.id);
    }



    /**
     * Set the Id property.
     *
     * @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getId() {
        return id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getLabel() {
        if (name.trim().length() == 0) {
            return id;
        }
        return name;

    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
        if (name == null) {
            name = "";
        }
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the Admin property.
     *
     * @param value The new value for Admin
     */
    public void setAdmin(boolean value) {
        admin = value;
    }

    /**
     * Get the Admin property.
     *
     * @return The Admin
     */
    public boolean getAdmin() {
        return admin;
    }

    /**
     * Set the Anonymous property.
     *
     * @param value The new value for Anonymous
     */
    public void setAnonymous(boolean value) {
        anonymous = value;
    }

    /**
     * Get the Anonymous property.
     *
     * @return The Anonymous
     */
    public boolean getAnonymous() {
        return anonymous;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return id;
    }


    /**
     * Set the Email property.
     *
     * @param value The new value for Email
     */
    public void setEmail(String value) {
        email = value;
        if (email == null) {
            email = "";
        }
    }

    /**
     * Get the Email property.
     *
     * @return The Email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set the Question property.
     *
     * @param value The new value for Question
     */
    public void setQuestion(String value) {
        question = value;
        if (question == null) {
            question = "";
        }
    }

    /**
     * Get the Question property.
     *
     * @return The Question
     */
    public String getQuestion() {
        return question;
    }

    /**
     * Set the Answer property.
     *
     * @param value The new value for Answer
     */
    public void setAnswer(String value) {
        answer = value;
        if (answer == null) {
            answer = "";
        }
    }

    /**
     * Get the Answer property.
     *
     * @return The Answer
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * Set the Password property.
     *
     * @param value The new value for Password
     */
    public void setPassword(String value) {
        password = value;
        if (password == null) {
            password = "";
        }
    }

    /**
     * Get the Password property.
     *
     * @return The Password
     */
    public String getPassword() {
        return password;
    }

    /**
     *  Set the Roles property.
     *
     *  @param value The new value for Roles
     */
    public void setRoles(List<String> value) {
        roles = value;
    }

    /**
     *  Get the Roles property.
     *
     *  @return The Roles
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * _more_
     *
     * @param role _more_
     *
     * @return _more_
     */
    public boolean isRole(String role) {
        if (role.equals(UserManager.ROLE_ANY)) {
            return true;
        }
        if (role.equals("user:" + getName())) {
            return true;
        }
        if (roles == null) {
            return false;
        }
        return roles.contains(role);
    }

    /**
     * _more_
     *
     * @param delimiter _more_
     *
     * @return _more_
     */
    public String getRolesAsString(String delimiter) {
        if (roles == null) {
            return "";
        }
        return StringUtil.join(delimiter, roles);
    }


}

