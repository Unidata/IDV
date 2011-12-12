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


import ucar.unidata.util.GuiUtils;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;




/**
 * @author Jeff McWhirter
 * @version $Revision: 1.12 $Date: 2005/10/06 15:19:37 $
 */

public class SharableImpl implements Sharable {

    /** _more_ */
    private boolean sharing;

    /** _more_ */
    private Object shareGroup;

    /**
     *  Each sharable object has a (hopefully) globally unique id.
     */
    private String uniqueId;

    /** _more_ */
    private boolean hasBeenInitialized = false;

    /** _more_ */
    private JComboBox shareGroupBox;


    /** Keeps track of the last time this object rcvd  shared data  */
    private Hashtable<Object, Long> lastReceiveShareTime;


    /**
     * _more_
     *
     */
    public SharableImpl() {
        this(SharableManager.GROUP_ALL, false);
    }

    /**
     * _more_
     *
     * @param sharing
     *
     */
    public SharableImpl(boolean sharing) {
        this(SharableManager.GROUP_ALL, sharing);
    }

    /**
     * _more_
     *
     * @param group
     * @param sharing
     *
     */
    public SharableImpl(Object group, boolean sharing) {
        this.sharing    = sharing;
        this.shareGroup = group;
    }



    /**
     * _more_
     */
    public void showSharableDialog() {
        JComponent contents = getSharablePropertiesComponent();
        contents = GuiUtils.inset(contents, 5);
        if ( !GuiUtils.showOkCancelDialog(null, "Sharable Properties",
                                          contents, null)) {
            return;
        }
        applySharableProperties();
    }

    /**
     * _more_
     */
    private void initGroupBox() {
        if (shareGroupBox == null) {
            shareGroupBox = new JComboBox();
            shareGroupBox.setEditable(true);
        }
        String myName = shareGroup.toString();
        List   names  = SharableManager.getShareGroupNames();
        if ( !names.contains(myName)) {
            names.add(0, myName);
            SharableManager.addShareGroupName(myName);
        }
        GuiUtils.setListData(shareGroupBox, names);
        shareGroupBox.setSelectedItem(myName);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent getSharablePropertiesComponent() {
        initGroupBox();
        return GuiUtils.left(GuiUtils.label("Group: ", shareGroupBox));
    }


    /**
     * _more_
     */
    public void applySharableProperties() {
        if (shareGroupBox == null) {
            return;
        }
        setShareGroup(shareGroupBox.getSelectedItem());
        SharableManager.addShareGroupName(
            shareGroupBox.getSelectedItem().toString());
        initGroupBox();
    }


    /**
     * Initialize this sharable. Add it into the SharableManager.
     */
    protected void initSharable() {
        if (hasBeenInitialized) {
            return;
        }
        hasBeenInitialized = true;
        SharableManager.addSharable(this);
    }


    /**
     * _more_
     * @return _more_
     */
    public boolean getSharing() {
        return sharing;
    }

    /**
     * _more_
     *
     * @param sharing
     */
    public void setSharing(boolean sharing) {
        this.sharing = sharing;
    }

    /**
     * _more_
     * @return _more_
     */
    public Object getShareGroup() {
        return shareGroup;
    }


    /**
     * _more_
     */
    public void removeSharable() {
        SharableManager.removeSharable(this);
    }

    /**
     * _more_
     *
     * @param shareGroup
     */
    public void setShareGroup(Object shareGroup) {
        if (hasBeenInitialized) {
            SharableManager.removeSharable(this);
        }
        this.shareGroup = shareGroup;
        if (hasBeenInitialized) {
            SharableManager.addSharable(this);
        }
    }

    /**
     * _more_
     *
     * @param from
     * @param dataId
     * @param data
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        //noop
    }


    /**
     * Get the last time this object rcvd the share
     *
     * @param dataId data id
     *
     * @return last time or null
     */
    public Long getReceiveShareTime(Object dataId) {
        if (lastReceiveShareTime == null) {
            return null;
        }
        return lastReceiveShareTime.get(dataId);
    }


    /**
     * Record the time when this object rcvd the share
     *
     * @param dataId data id
     * @param time time
     */
    public void setReceiveShareTime(Object dataId, Long time) {
        if (lastReceiveShareTime == null) {
            lastReceiveShareTime = new Hashtable<Object, Long>();
        }
        lastReceiveShareTime.put(dataId, time);
    }

    /**
     *  Share the data identified by dataId.
     *
     *  @param dataId Identifies the attribute that is being shared
     *  @param data   The data that is being shared
     *  @param internal Share with internal objects
     *  @param external Share with external objects
     */
    private void doShare(Object dataId, Object[] data, boolean internal,
                         boolean external) {
        SharableManager.checkShareData(this, dataId, data, internal,
                                       external);
    }


    /**
     *  Share the data identified by dataId with both internal and external objects.
     *
     *  @param dataId Identifies the attribute that is being shared
     *  @param data   The array of data that is being shared
     */
    public void doShare(Object dataId, Object[] data) {
        doShare(dataId, data, true, true);
    }

    /**
     *  Share the data identified by dataId with both internal and external objects.
     *
     *  @param dataId Identifies the attribute that is being shared
     *  @param data   The data that is being shared
     */
    public void doShare(Object dataId, Object data) {
        doShare(dataId, new Object[] { data });
    }


    /**
     *  Share the data identified by dataId with only external (to the jvm) objects.
     *  i.e., only share with other objects on other jvms through the collaboration mechanism.
     *
     *  @param dataId Identifies the attribute that is being shared
     *  @param data   The data that is being shared
     */
    public void doShareExternal(Object dataId, Object data) {
        doShare(dataId, new Object[] { data }, false, true);
    }

    /**
     *  Share the data identified by dataId with only internal (to the jvm) objects.
     *
     * @param dataId
     * @param data
     */
    public void doShareInternal(Object dataId, Object data) {
        doShare(dataId, new Object[] { data }, true, false);
    }


    /**
     * _more_
     * @return _more_
     */
    public String getUniqueId() {
        if (uniqueId == null) {
            uniqueId = "sharable_" + ucar.unidata.util.Misc.getUniqueId();
        }
        return uniqueId;
    }

    /**
     * _more_
     *
     * @param id
     */
    public void setUniqueId(String id) {
        uniqueId = id;
    }


}
