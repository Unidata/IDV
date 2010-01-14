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

package ucar.visad.display;



import java.*;



/**
 * A class to hold and transfer animation properties settings, as to and
 * from the AnimationPropertiesDialog.
 *
 * <ul>
 * <li>fwdspeed controls speed of looping forward (seconds/frame) =
 *   how long each frame is seen when looping forwards, except first
 *   and last frame.
 * <li>backspeed controls speed of looping backwards (seconds/frame)=
 *   how long each frame is seen when looping reverse,
 *   except first and last frame.
 * <li>startdwell = how long first frame is seen;
 * <li>enddwell = how long last frame is seen;
 * <li>direction is the loop going forwards or backwards,
 *     true = forwards in time;
 * <li>rocking is true if rocking on - looping in both directions,
 *             back and forth.
 * <li>shared  is true if state should be shared with other animation widgets
 * @version $Revision: 1.34 $
 */
public class AnimationInfo {




    /** Enum for reset property */
    public static final String RESET_BEGINNING = "reset.beginning";

    /** Enum for reset property */
    public static final String RESET_CURRENT = "reset.current";

    /** Enum for reset property */
    public static final String RESET_END = "reset.end";

    /** The synthetic animation set info */
    private AnimationSetInfo animationSetInfo = new AnimationSetInfo();

    /** When getting a new animation this determines the reset policy */
    protected String resetPolicy = RESET_BEGINNING;

    /** Are boxes visible */
    protected boolean boxesVisible = true;

    /** forward speed */
    protected float fwdSpeed;

    /** backward speed */
    protected float backSpeed;

    /** starting dwell rate */
    protected float startDwell;

    /** ending dwell rate */
    protected float endDwell;

    /** direction of looping */
    protected boolean direction;

    /** flag for rocking */
    protected boolean rocking;

    /** flag for sharing */
    protected boolean shared = true;

    /** animation group */
    protected String animationGroup;

    /** Are we running */
    private boolean running = false;

    /** When we share do share the index of the time step */
    private boolean shareIndex = false;


    /**
     * Construct an object to hold and transfer animation properties settings.
     * Set initial values in seconds which user can change later.
     */
    public AnimationInfo() {
        this.fwdSpeed     = 0.5f;
        this.backSpeed    = 0.5f;
        this.startDwell   = 0.5f;
        this.endDwell     = 1.2f;
        this.direction    = true;
        this.rocking      = false;
        this.shared       = true;
        this.shareIndex   = false;
        this.boxesVisible = true;
    }

    /**
     * Construct an object to hold and transfer animation properties settings,
     * with values supplied.
     *
     * @param fwdSpeed controls speed of looping forward (seconds/frame)
     * @param backSpeed controls speed of looping backwards (seconds/frame)
     * @param startDwell duration of pause at start of loop (seconds)
     * @param endDwell duration of pause at end of loop (seconds)
     * @param direction is the loop going forwards or backwards
     * @param rocking is rocking on - looping in both directions
     */
    public AnimationInfo(float fwdSpeed, float backSpeed, float startDwell,
                         float endDwell, boolean direction, boolean rocking) {

        this(fwdSpeed, backSpeed, startDwell, endDwell, direction, rocking,
             true);
    }

    /**
     * Construct an object to hold and transfer animation properties settings,
     * with values supplied.
     *
     * @param fwdSpeed    controls speed of looping forward (seconds/frame)
     * @param backSpeed   controls speed of looping backwards (seconds/frame)
     * @param startDwell  duration of pause at start of loop (seconds)
     * @param endDwell    duration of pause at end of loop (seconds)
     * @param direction   is the loop going forwards or backwards
     * @param rocking     true if rocking on - looping in both directions
     * @param shared      true if state is to be shared
     */
    public AnimationInfo(float fwdSpeed, float backSpeed, float startDwell,
                         float endDwell, boolean direction, boolean rocking,
                         boolean shared) {

        this.fwdSpeed   = fwdSpeed;
        this.backSpeed  = backSpeed;
        this.startDwell = startDwell;
        this.endDwell   = endDwell;
        this.direction  = direction;
        this.rocking    = rocking;
        this.shared     = shared;
        this.shareIndex = false;
    }

    /**
     * Set the state of thie AnimationInfo from the state of
     * another.
     * @param that  other AnimationInfo
     */
    public void set(AnimationInfo that) {
        this.running        = that.running;
        this.resetPolicy    = that.resetPolicy;
        this.fwdSpeed       = that.fwdSpeed;
        this.backSpeed      = that.backSpeed;
        this.startDwell     = that.startDwell;
        this.endDwell       = that.endDwell;
        this.direction      = that.direction;
        this.rocking        = that.rocking;
        this.shared         = that.shared;
        this.shareIndex     = that.shareIndex;
        this.boxesVisible   = that.boxesVisible;
        this.animationGroup = that.animationGroup;
        if (that.animationSetInfo != null) {
            this.animationSetInfo =
                new AnimationSetInfo(that.animationSetInfo);
        }
    }

    /**
     * String representation of this object.
     * @return String representation of this object
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("AnimationInfo:\n");
        buf.append("fwdspd=");
        buf.append(fwdSpeed);
        buf.append(" backspd=");
        buf.append(backSpeed);
        buf.append(" startDwell=");
        buf.append(startDwell);
        buf.append(" endDwell=");
        buf.append(endDwell);
        buf.append(" rocking=");
        buf.append(rocking);
        buf.append(" shared=");
        buf.append(shared);
        buf.append(" shareIndex=");
        buf.append(shareIndex);
        buf.append(" reset=");
        buf.append(resetPolicy);
        buf.append(" boxesVisible=");
        buf.append(boxesVisible);
        buf.append(" animationGroup=");
        buf.append(animationGroup);
        return buf.toString();
    }

    /**
     *  Set the Shared property.
     *
     *  @param value The new value for Shared
     */
    public void setShared(boolean value) {
        shared = value;
    }

    /**
     *  Get the Shared property.
     *
     *  @return The Shared
     */
    public boolean getShared() {
        return shared;
    }

    /**
     *  Set the Rocking property.
     *
     *  @param value The new value for Rocking
     */
    public void setRocking(boolean value) {
        rocking = value;
    }

    /**
     *  Get the Rocking property.
     *
     *  @return The Rocking
     */
    public boolean getRocking() {
        return rocking;
    }

    /**
     *  Set the Direction property.
     *
     *  @param value The new value for Direction
     */
    public void setDirection(boolean value) {
        direction = value;
    }

    /**
     *  Get the Direction property.
     *
     *  @return The Direction
     */
    public boolean getDirection() {
        return direction;
    }

    /**
     *  Set the FwdSpeed property.
     *
     *  @param value The new value for FwdSpeed
     */
    public void setFwdSpeed(float value) {
        fwdSpeed = value;
    }

    /**
     *  Get the FwdSpeed property.
     *
     *  @return The FwdSpeed
     */
    public float getFwdSpeed() {
        return fwdSpeed;
    }

    /**
     *  Set the BackSpeed property.
     *
     *  @param value The new value for BackSpeed
     */
    public void setBackSpeed(float value) {
        backSpeed = value;
    }

    /**
     *  Get the BackSpeed property.
     *
     *  @return The BackSpeed
     */
    public float getBackSpeed() {
        return backSpeed;
    }

    /**
     *  Set the StartDwell property.
     *
     *  @param value The new value for StartDwell
     */
    public void setStartDwell(float value) {
        startDwell = value;
    }

    /**
     *  Get the StartDwell property.
     *
     *  @return The StartDwell
     */
    public float getStartDwell() {
        return startDwell;
    }


    /**
     *  Set the EndDwell property.
     *
     *  @param value The new value for EndDwell
     */
    public void setEndDwell(float value) {
        endDwell = value;
    }

    /**
     *  Get the EndDwell property.
     *
     *  @return The EndDwell
     */
    public float getEndDwell() {
        return endDwell;
    }


    /**
     *  Set the ResetPolicy property.
     *
     *  @param value The new value for ResetPolicy
     */
    public void setResetPolicy(String value) {
        resetPolicy = value;
    }

    /**
     *  Get the ResetPolicy property.
     *
     *  @return The ResetPolicy
     */
    public String getResetPolicy() {
        return resetPolicy;
    }



    /**
     * Should we reset to the end
     *
     * @return Reset to the end
     */
    public boolean resetToEnd() {
        return resetPolicy.equals(RESET_END);
    }

    /**
     * No reset
     *
     * @return No reset
     */
    public boolean resetToCurrent() {
        return resetPolicy.equals(RESET_CURRENT);
    }

    /**
     * Should we reset to the start
     *
     * @return Reset to the start
     */
    public boolean resetToBeginning() {
        return resetPolicy.equals(RESET_BEGINNING);
    }


    /**
     * Set the BoxesVisible property.
     *
     * @param value The new value for BoxesVisible
     */
    public void setBoxesVisible(boolean value) {
        boxesVisible = value;
    }

    /**
     * Get the BoxesVisible property.
     *
     * @return The BoxesVisible
     */
    public boolean getBoxesVisible() {
        return boxesVisible;
    }


    /**
     * Set the AnimationGroup property.
     *
     * @param value The new value for AnimationGroup
     */
    public void setAnimationGroup(String value) {
        animationGroup = value;
    }

    /**
     * Get the AnimationGroup property.
     *
     * @return The AnimationGroup
     */
    public String getAnimationGroup() {
        return animationGroup;
    }


    /**
     * Set the AnimationSetInfo property.
     *
     * @param value The new value for AnimationSet
     */
    public void setAnimationSetInfo(AnimationSetInfo value) {
        animationSetInfo = value;
    }

    /**
     * Get the AnimationSetInfo property.
     *
     * @return The AnimationSetInfo
     */
    public AnimationSetInfo getAnimationSetInfo() {
        return animationSetInfo;
    }


    /**
     * Set the Running property.
     *
     * @param value The new value for Running
     */
    public void setRunning(boolean value) {
        running = value;
    }

    /**
     * Get the Running property.
     *
     * @return The Running
     */
    public boolean getRunning() {
        return running;
    }


    /**
     *  Set the ShareIndex property.
     *
     *  @param value The new value for ShareIndex
     */
    public void setShareIndex(boolean value) {
        shareIndex = value;
    }

    /**
     *  Get the ShareIndex property.
     *
     *  @return The ShareIndex
     */
    public boolean getShareIndex() {
        return shareIndex;
    }




}
