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
 * 
 */

package ucar.unidata.collab;


import ucar.unidata.util.LogUtil;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;



/**
 * @author Jeff McWhirter
 * @version $Revision: 1.12 $Date: 2005/09/21 17:13:21 $
 */

public class SharableManager {

    /** Because these methods are static we use this to do synchronization */

    private static final Object MUTEX = new Object();

    /** 
        When a sharable receives a share we set the time when. When a sharable
        does a doShare we check the last time it rcvd the share event. If its
        less than this threshold (milliseconds) then we don't propagate the 
        doShare
    */
    private static final long SHARE_TIME_THRESHOLD = 1000;

    /** _more_ */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            SharableManager.class.getName());

    /** _more_ */
    public static final LogUtil LU = null;

    /** _more_ */
    private static List listeners;

    /** _more_ */
    public static List shareGroupNames;

    /** _more_ */
    private static boolean activelySharing = false;

    /** _more_ */
    public static final String GROUP_ALL = "All";


    /** _more_ */
    private static Hashtable groupToList = new Hashtable();

    /** _more_ */
    private static List shareGroups = null;

    /**
     * _more_
     * @return _more_
     */
    public static List getShareGroupNames() {
        synchronized (MUTEX) {
            if (shareGroupNames == null) {
                initShareGroupNames();
            }
            return new ArrayList(shareGroupNames);
        }
    }

    /**
     * _more_
     *
     * @param name _more_
     */
    public static void addShareGroupName(String name) {
        getShareGroupNames();
        if ( !shareGroupNames.contains(name)) {
            shareGroupNames.add(0, name);
        }

    }

    /**
     * _more_
     */
    private static void initShareGroupNames() {
        shareGroupNames = new ArrayList();
        shareGroupNames.add("Group 1");
        shareGroupNames.add("Group 2");
        shareGroupNames.add("Group 3");
        shareGroupNames.add("Group 4");
    }



    /**
     * _more_
     *
     * @param group
     * @return _more_
     */
    private static List getGroupList(Object group) {
        synchronized (MUTEX) {
            if (group == null) {
                return null;
            }
            List l = (List) groupToList.get(group);
            if (l == null) {
                groupToList.put(group, l = new ArrayList());
            }
            return l;
        }
    }

    /**
     * _more_
     *
     * @param s
     */
    public static void addSharable(Sharable s) {
        synchronized (MUTEX) {
            List groupList = getGroupList(s.getShareGroup());
            if (groupList == null) {
                return;
            }
            //            ucar.unidata.util.Misc.printStack ("Adding sharable: " + s.getClass ().getName (),15,null);
            if ( !groupList.contains(s)) {
                groupList.add(s);
            }
        }
    }

    /**
     * _more_
     *
     * @param s
     */
    public static void removeSharable(Sharable s) {
        synchronized (MUTEX) {
            //            System.err.println ("Removing: " + s.getClass ().getName ());
            List groupList = getGroupList(s.getShareGroup());
            if (groupList == null) {
                //                System.err.println ("Couldn't find group list");
                return;
            }
            groupList.remove(s);
        }
    }



    /**
     *  Add a listener to the list of listeners.
     *
     * @param listener
     */
    public static void addSharableListener(SharableListener listener) {
        if (listeners == null) {
            listeners = new ArrayList();
        }
        listeners.add(listener);
    }



    /**
     *  Remove a listener from the list of listeners.
     *
     * @param listener
     */
    public static void removeSharableListener(SharableListener listener) {
        if (listeners == null) {
            return;
        }
        listeners.remove(listener);
    }


    /**
     * _more_
     *
     * @param from
     * @param dataId
     * @param data
     * @param internal
     * @param external
     */
    protected static void checkShareData(Sharable from, Object dataId,
                                         Object[] data, boolean internal,
                                         boolean external) {
        //Check if the from object had rcvd a doShare in the last SHARE_TIME_THRESHOLD milliseconds
        Long lastTime = from.getReceiveShareTime(dataId);
        if (lastTime != null) {
            if ((System.currentTimeMillis() - lastTime.longValue())
                    < SHARE_TIME_THRESHOLD) {
                return;
            }
        }


        if (internal) {
            if (from.getSharing()) {
                sendShareData(from, dataId, data);
            }
        }
        if (external) {
            if ((from != null) && (listeners != null)) {
                for (int i = 0; i < listeners.size(); i++) {
                    ((SharableListener) listeners.get(i)).checkShareData(
                        from, dataId, data);
                }
            }
        }
    }


    /**
     * _more_
     *
     * @param from
     * @param dataId
     * @param data
     */
    private static void sendShareData(Sharable from, Object dataId,
                                      Object[] data) {
        sendShareData(from, from.getShareGroup(), dataId, data);
    }



    /**
     * _more_
     *
     * @param from
     * @param shareGroup
     * @param dataId
     * @param data
     */
    private static void sendShareData(Sharable from, Object shareGroup,
                                      Object dataId, Object[] data) {
        synchronized (MUTEX) {
            //TODO: Make the blocking of loop detection a bit more fine grained
            if (activelySharing) {
                return;
            }
            activelySharing = true;
        }
        try {
            List groupList = getGroupList(shareGroup);
            if (groupList == null) {
                return;
            }
            for (int i = 0; i < groupList.size(); i++) {
                Sharable to = (Sharable) groupList.get(i);
                if (to == from) {
                    continue;
                }
                if ( !to.getSharing()) {
                    continue;
                }
                to.setReceiveShareTime(dataId,
                                       new Long(System.currentTimeMillis()));
                to.receiveShareData(from, dataId, data);
            }
        } catch (Exception e) {
            LU.printException(log_, "sendShareData:" + dataId, e);
        }
        activelySharing = false;
    }



    /**
     * _more_
     * @return _more_
     */
    public static List getDefaultShareGroups() {
        synchronized (MUTEX) {
            if (shareGroups == null) {
                shareGroups = new ArrayList();
                shareGroups.add(GROUP_ALL);
            }
            return shareGroups;
        }
    }

    /**
     * _more_
     *
     * @param group
     */
    public static void addShareGroup(Object group) {
        synchronized (MUTEX) {
            List groups = getDefaultShareGroups();
            groups.add(group);
        }
    }

}
