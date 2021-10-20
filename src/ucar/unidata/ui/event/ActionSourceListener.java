/*
 * $Id: ActionSourceListener.java,v 1.8 2005/05/13 18:32:14 jeffmc Exp $
 *
 * Copyright  1997-2022 Unidata Program Center/University Corporation for
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

package ucar.unidata.ui.event;



/**
 * ActionSourceListeners are used by objects that are both source and listener for
 *  a particular type of ActionValue events. They register themselves with the
 *  ActionCoordinator of that type of event. They send events
 *  by calling fireActionValueEvent().
 *  They recieve others' events through their actionPerformed() method.
 *
 * @see ActionCoordinator
 * @author John Caron
 * @version $Id: ActionSourceListener.java,v 1.8 2005/05/13 18:32:14 jeffmc Exp $
 */

public abstract class ActionSourceListener implements ActionValueListener {

    /** _more_ */
    static public final String SELECTED = "selected";

    /** _more_ */
    private ucar.unidata.util.ListenerManager lm;

    /** _more_ */
    private String eventType;

    /**
     * _more_
     *
     * @param eventType
     *
     */
    public ActionSourceListener(String eventType) {
        this.eventType = eventType;

        // manage ActionValueEvent Listeners
        lm = new ucar.unidata.util.ListenerManager(
            "ucar.unidata.ui.event.ActionValueListener",
            "ucar.unidata.ui.event.ActionValueEvent", "actionPerformed");
    }

    /**
     * _more_
     * @return _more_
     */
    public String getEventTypeName() {
        return eventType;
    }

    /**
     * _more_
     *
     * @param command
     * @param value
     */
    public void fireActionValueEvent(String command, Object value) {
        lm.sendEvent(new ActionValueEvent(this, command, value));
    }

    /**
     * _more_
     *
     * @param l
     */
    public void addActionValueListener(ActionValueListener l) {
        lm.addListener(l);
    }

    /**
     * _more_
     *
     * @param l
     */
    public void removeActionValueListener(ActionValueListener l) {
        lm.removeListener(l);
    }

    /**
     * _more_
     *
     * @param event
     */
    public abstract void actionPerformed(ActionValueEvent event);
}





