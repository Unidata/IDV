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


import ucar.unidata.beans.*;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.visad.display.AnimationInfo;



import visad.*;

import visad.util.DataUtility;

import java.rmi.RemoteException;

import java.util.Arrays;


/**
 * Provides support for a {@link Displayable} that needs a map to
 * Display.Animation
 *
 * @author  IDV development Team
 * @version $Revision: 1.75 $
 */
public class Animation extends Displayable {

    /**
     * The name of the animation real-type property.
     */
    public static final String ANI_REAL_TYPE = "aniRealType";

    /**
     * The name of the current time property.
     */
    public static final String ANI_VALUE = "aniValue";

    /**
     * The name of the set-of-times property.
     */
    public static final String ANI_SET = "animationSet";

    /**
     * The "forward" animation value.
     */
    public static final int FORWARD = 0;

    /**
     * The "reverse" animation value.
     */
    public static final int REVERSE = 1;

    /** are we enabled? */
    private boolean enabled = true;

    /** ScalarMap for animation */
    private ScalarMap animationMap;

    /** What steps are ok */
    private boolean[] stepsOk;

    /** Control for animationMap */
    private AnimationControl animationControl;

    /** RealType for the ScalarMap */
    private RealType aniRealType;

    /** Animation set */
    private Set animationSet;

    /** The current animation index */
    private int currentIndex = 0;

    /** Value for the current animation step */
    private Real aniValue = null;

    /** the last time */
    private Real lastTime;

    /** a Real that represents the missing value for aniRealType */
    private Real missingValue = null;

    /** Object for locking */
    private final Object MUTEX = new Object();

    /** AnimationInfo used to represent the configuration */
    private AnimationInfo animationInfo;  // won't be null

    /** boolean for whether rocking is on or not */
    private boolean rocking = false;

    /** forward or back */
    private boolean direction = true;

    /** The current animation thread */
    private Thread animationThread;

    /** forward dwell times */
    private long[] fwdTimeSteps;

    /** backward dwell times */
    private long[] backTimeSteps;


    /** listener for the AnimationMap */
    private final AnimationMapListener mapListener =
        new AnimationMapListener();

    /** listener for the AnimationControl */
    private final AnimationControlListener controlListener =
        new AnimationControlListener();

    /**
     * Constructs from nothing. Uses {@link visad.RealType#Time} for the
     * animation {@link visad.RealType}.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Animation() throws VisADException, RemoteException {
        this(RealType.Time);
    }

    /**
     * Constructs from the type of the VisAD parameter to animate over,
     * typically {@link visad.RealType#Time}.
     *
     * @param aniRealType       The type of the animation parameter or
     *                          <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Animation(RealType aniRealType)
            throws VisADException, RemoteException {

        this.aniRealType = aniRealType;

        if (aniRealType != null) {
            setAnimationMap();
        }

        animationInfo = new AnimationInfo();
    }

    /**
     * Construct a new Animation from another one
     *
     * @param that  the other Animation object
     *
     * @throws RemoteException  Java RMI failure
     * @throws VisADException   VisAD failure
     */
    protected Animation(Animation that)
            throws VisADException, RemoteException {

        super(that);

        this.aniRealType   = that.aniRealType;
        this.animationInfo = that.animationInfo;

        if (this.aniRealType != null) {
            setAnimationMap();
        }
    }



    /**
     * Sets the {@link visad.RealType} of the VisAD Animation parameter,
     * such as {@link visad.RealType#Time}.
     *
     * @param realType              The {@link visad.RealType} of the animation
     *                              parameter.
     * @throws VisADException       VisAD failure.
     * @throws RemoteException      Java RMI failure.
     */
    public void setAnimationRealType(RealType realType)
            throws RemoteException, VisADException {

        RealType oldType = null;
        boolean  isNew   = false;

        synchronized (MUTEX) {
            if ( !realType.equals(aniRealType)) {
                oldType     = aniRealType;
                aniRealType = realType;
                isNew       = true;
            }
        }

        if (isNew) {
            setAnimationMap();
            firePropertyChange(ANI_REAL_TYPE, oldType, realType);
        }
    }

    /**
     * Returns the RealType of the Animation parameter.
     *
     * @return                  The RealType of the Animation parameter.  May
     *                          be <code>null</code>.
     */
    public RealType getAnimationRealType() {
        return aniRealType;
    }


    /**
     * Returns the value of the "aniValue" property.  Returns <code>null</code>
     * if no such value exists.
     *
     * @return                  The value of the property or <code>null</code>.
     */
    public final Real getAniValue() {
        return aniValue;
    }

    /**
     * This method should not be invoked while synchronized.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private void setAnimationMap() throws RemoteException, VisADException {

        ScalarMap oldMap   = null;
        ScalarMap newMap   = null;
        boolean   isNewMap = false;
        missingValue = new Real(aniRealType);

        synchronized (MUTEX) {
            oldMap       = animationMap;
            newMap       = new ScalarMap(aniRealType, Display.Animation);
            animationMap = newMap;
            isNewMap     = true;
        }

        if (isNewMap) {
            if (oldMap != null) {
                oldMap.removeScalarMapListener(mapListener);
            }
            newMap.addScalarMapListener(mapListener);
            replaceScalarMap(oldMap, newMap);
            fireScalarMapSetChange();
        }
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     *
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new Animation(this);
    }

    /**
     * <p>Adds the {@link visad.DataReference}s associated with this instance
     * to the display.</p>
     *
     * <p>This implementation does nothing.</p>
     */
    public final void myAddDataReferences() {}

    /**
     * <p>Removes the {@link visad.DataReference}s associated with this
     * instance from the display.</p>
     *
     * <p>This implementation does nothing.</p>
     */
    public final void myRemoveDataReferences() {}

    /**
     * Explicitly sets the animation parameters of this instance.  The
     * parameters are copied.
     *
     * @param ai                    The animation parameters.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisADError
     */
    public void setAnimationInfo(AnimationInfo ai)
            throws RemoteException, VisADException {
        if (ai == null) {
            throw new NullPointerException();
        }

        synchronized (MUTEX) {
            animationInfo.set(ai);
        }
        resetAnimationInfo();
    }

    /**
     * Get the {@link AnimationInfo} associated with this Animation.
     * @return the AnimationInfo
     */
    public AnimationInfo getAnimationInfo() {
        return animationInfo;
    }

    /**
     * Sets the animation properties in the AnimationControl from the
     * animationInfo member data.  Fires a PropertyChangeEvent for the
     * animation timeset.
     * This method should not be invoked while synchronized.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisADError
     */
    private void resetAnimationInfo() throws RemoteException, VisADException {

        AnimationInfo info = animationInfo;
        rocking   = info.rocking;
        direction = info.direction;
        int    SKIPTIME     = 0;
        int    numTimes     = getNumSteps();
        long[] newFwdTimes  = null;
        long[] newBackTimes = null;

        if (numTimes > 0) {
            // Adjust dwell times
            newFwdTimes  = new long[numTimes];
            newBackTimes = new long[numTimes];
            float fwdSpeed  = info.getFwdSpeed();
            float backSpeed = info.getBackSpeed();

            for (int nn = 0; nn < numTimes; nn++) {
                if ((stepsOk != null) && (nn < stepsOk.length)
                        && !stepsOk[nn]) {
                    newFwdTimes[nn]  = SKIPTIME;
                    newBackTimes[nn] = SKIPTIME;
                } else {
                    newFwdTimes[nn]  = (int) (fwdSpeed * 1000.0);
                    newBackTimes[nn] = (int) (backSpeed * 1000.0);
                }
            }

            //Set the start dwell
            for (int nn = 0; nn < numTimes; nn++) {
                if (newFwdTimes[nn] != SKIPTIME) {
                    newFwdTimes[nn]  = (int) (info.getStartDwell() * 1000.0);
                    newBackTimes[nn] = (int) (info.getStartDwell() * 1000.0);
                    break;
                }
            }

            //Set the end dwell
            for (int nn = numTimes - 1; nn >= 0; nn--) {
                if (newFwdTimes[nn] != SKIPTIME) {
                    newFwdTimes[nn]  = (int) (info.getEndDwell() * 1000.0);
                    newBackTimes[nn] = (int) (info.getEndDwell() * 1000.0);
                    break;
                }
            }
        }
        setFwdSteps(newFwdTimes);
        setBackSteps(newBackTimes);
        setAnimating(animationInfo.getRunning());
    }


    /**
     * Begin animating or looping.
     *
     * @param on  Whether or not looping should be on.
     */
    public void setAnimating(boolean on) {
        animationInfo.setRunning(on);
        if ( !on) {
            animationThread = null;
            return;
        }
        animationThread = new Thread(new Runnable() {
            public void run() {
                runAnimation();
            }
        });
        animationThread.start();
    }

    /**
     * Run the animation
     */
    private void runAnimation() {
        try {
            Thread myThread = Thread.currentThread();
            while ((myThread == animationThread)
                    && animationInfo.getRunning()) {
                long sleepTime = 500;
                int  nextIndex = getCurrent();
                if (direction) {
                    if ( !anyStepsForward()) {
                        if (rocking) {
                            direction = false;
                        } else {
                            nextIndex = 0;
                        }
                    } else {
                        nextIndex++;
                    }
                } else {
                    if ( !anyStepsBack()) {
                        if (rocking) {
                            direction = true;
                        } else {
                            nextIndex = getNumSteps() - 1;
                        }
                    } else {
                        nextIndex--;
                    }
                }
                nextIndex = clipIndex(nextIndex, direction);
                setCurrent(nextIndex);
                long[] localTimeSteps;
                if (direction) {
                    localTimeSteps = fwdTimeSteps;
                } else {
                    localTimeSteps = backTimeSteps;
                }
                if ((localTimeSteps != null) && (nextIndex >= 0)
                        && (nextIndex < localTimeSteps.length)) {
                    sleepTime = localTimeSteps[nextIndex];
                }
                //Make sure we're not sleeping for 0 seconds
                if (sleepTime == 0) {
                    sleepTime = 500;
                }
                Misc.sleep(sleepTime);
            }
        } catch (Exception excp) {
            LogUtil.logException("Running animation", excp);
        }
    }


    /**
     * Check if  the animationSet.valueToIndex of the given time value
     * is equals to the current time step.
     *
     * @param timeValue The time value to check
     * @return Is the time value the current one being displayed
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   VisAD Exception
     */
    public boolean shouldShow(Real timeValue)
            throws VisADException, RemoteException {
        if ((animationSet == null) || (animationSet.getLength() == 0)) {
            return true;
        }
        float timeValueFloat =
            (float) timeValue.getValue(animationSet.getSetUnits()[0]);
        float[][] value = {
            { timeValueFloat }
        };
        int[]     index = animationSet.valueToIndex(value);
        return (index[0] == getCurrent());
    }


    /**
     * Indicates if looping is on.
     *
     * @return  true if looping; false if not looping.
     */
    public boolean isAnimating() {
        return animationInfo.getRunning() && (animationThread != null);
    }

    /**
     * Indicates if looping is on.
     *
     * @return  true if looping; false if not looping.
     */
    public boolean shouldBeAnimating() {
        return animationInfo.getRunning();
    }


    /**
     * Returns the set of animation time-values.
     *
     * @return                  The set if animation times.
     */
    public Set getSet() {
        return animationSet;
    }

    /**
     * Sets the animation times for this instance to use.
     *
     * @param newSet           New animation times.
     * @throws VisADException  if the set isn't 1-D or doesn't have the same
     *                         {@link visad.RealType} as the animation {@link
     *                         visad.ScalarMap}.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setSet(Set newSet) throws VisADException, RemoteException {
        setSet(newSet, true);
    }



    /**
     * Get the current animation value as a Real
     *
     * @return  the value
     */
    public Real getCurrentAnimationValue() {
        DateTime[] times   = getTimes();
        int        current = getCurrent();
        if ((times != null) && (current >= 0) && (current < times.length)) {
            return times[current];
        }
        return null;
    }


    /**
     * Get the times as an array
     *
     * @return  the times or null
     */
    public DateTime[] getTimes() {
        if (animationSet != null) {
            return getDateTimeArray(animationSet);
        }
        return null;
    }


    /**
     * Sets the animation times for this instance to use.
     *
     * @param newSet           New animation times.
     * @param transform        true to transform
     * @throws VisADException  if the set isn't 1-D or doesn't have the same
     *                         {@link visad.RealType} as the animation {@link
     *                         visad.ScalarMap}.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setSet(Set newSet, boolean transform)
            throws VisADException, RemoteException {
        if (Misc.equals(newSet, animationSet)) {
            return;
        }
        animationSet = newSet;
        Real oldTime = aniValue;
        firePropertyChange(ANI_SET, "old value", animationSet);
        AnimationControl control = animationControl;
        if (control == null) {
            return;
        }
        control.setSet(newSet, transform);

        Real timeIWantToBe = null;
        if (animationSet != null) {
            DateTime[] timeArray = getDateTimeArray(animationSet);
            if ((timeArray != null) && (timeArray.length > 0)) {
                int index = 0;
                if (animationInfo.resetToEnd()) {
                    index = timeArray.length - 1;
                } else if (animationInfo.resetToBeginning()) {
                    index = 0;
                } else if (animationInfo.resetToCurrent()) {
                    index = Misc.indexOf(oldTime, timeArray);
                    if (index < 0) {
                        index = 0;
                    }
                }
                if ((index >= 0) && (index < timeArray.length)) {
                    timeIWantToBe = timeArray[index];
                }
            }
        }


        //For now always fire the event
        if (true || !Misc.equals(timeIWantToBe, getAniValue())) {
            aniValue = null;
            setAniValue(timeIWantToBe);
            if (timeIWantToBe != null) {
                //                System.err.println ("setSet.fire ANI_VALUE");
                //                firePropertyChange(ANI_VALUE, oldTime, timeIWantToBe);
            }
        }

    }




    /**
     * Returns an array of the dwell times for all the steps in integer
     * milliseconds. Returns <code>null</code> if no such times exist;
     * otherwise, each time value is the delay AFTER each loop step: the first
     * time in list is time delay AFTER the display of the first data.
     *
     * @deprecated Use getFwdSteps
     * @return Dwell delays in milliseconds or <code>null</code>.
     */
    public long[] getSteps() {
        return getFwdSteps();
    }

    /**
     * Sets an integer array in milliseconds of time delay AFTER
     * display each loop step.
     *
     * @deprecated Use setFwdSteps
     * @param newTimes          Dwell delays in milliseconds.
     *
     */
    public void setSteps(long[] newTimes) {
        setFwdSteps(newTimes);
    }



    /**
     * Returns an array of the dwell times for all the steps in integer
     * milliseconds. Returns <code>null</code> if no such times exist;
     * otherwise, each time value is the delay AFTER each loop step: the first
     * time in list is time delay AFTER the display of the first data.
     *
     * @return Dwell delays in milliseconds or <code>null</code>.
     */
    public long[] getFwdSteps() {
        return fwdTimeSteps;
    }

    /**
     * Sets an integer array in milliseconds of time delay AFTER
     * display each loop step.
     *
     * @param newTimes          Dwell delays in milliseconds.
     *
     */
    public void setFwdSteps(long[] newTimes) {
        fwdTimeSteps = newTimes;
    }




    /**
     * Returns an array of the backward dwell times for all the steps in integer
     * milliseconds. Returns <code>null</code> if no such times exist;
     * otherwise, each time value is the delay AFTER each loop step: the first
     * time in list is time delay AFTER the display of the first data.
     *
     * @return Dwell delays in milliseconds or <code>null</code>.
     */
    public long[] getBackSteps() {
        return backTimeSteps;
    }

    /**
     * Sets an integer array in milliseconds of time delay AFTER
     * display each loop step when animating backward.
     *
     * @param newTimes          Dwell delays in milliseconds.
     */
    public void setBackSteps(long[] newTimes) {
        backTimeSteps = newTimes;
    }




    /**
     * Returns the number of time steps
     *
     * @return    The number of time steps
     */
    public int getNumSteps() {
        try {
            if (animationSet != null) {
                return animationSet.getLength();
            }
        } catch (Exception excp) {
            LogUtil.logException("getNumSteps", excp);
        }
        return 0;
    }

    /**
     * Returns the origin-0 index of the current time being shown.  Returns
     * 0 if there is no such time.
     *
     * @return           The index of current time or 0.
     */

    public int getCurrent() {
        AnimationControl control = animationControl;
        if (control != null) {
            return currentIndex = control.getCurrent();
        }
        return currentIndex;
    }


    /**
     * Sets the current time being shown to the value of the
     * Real.
     * @param newTime  Real of the newTime.
     */
    public void setAniValue(Real newTime) {
        if ((newTime == null) || Misc.equals(aniValue, newTime)
                || !Misc.equals(aniRealType, newTime.getType())
                || (animationSet == null)) {
            return;
        }
        try {
            //Maybe try to set this here for now
            aniValue = newTime;
            int index = animationSet.doubleToIndex(new double[][] {
                { newTime.getValue(animationSet.getSetUnits()[0]) }
            })[0];
            // if animation value is not in the time set, set it to one end or the other
            if (index == -1) {
                double[] aniValues = animationSet.getDoubles(false)[0];
                double anitime =
                    newTime.getValue(animationSet.getSetUnits()[0]);
                int lastIndex = aniValues.length - 1;
                if (anitime >= aniValues[lastIndex]) {
                    index = lastIndex;
                } else {
                    index = 0;
                }
            }
            if (index != getCurrent()) {
                setCurrent(index, false);
            }
        } catch (Exception ve) {
            LogUtil.logException("setting animation value", ve);
        }
    }

    /**
     * Sets the current time being shown.
     *
     * @param newIndex           The origin-0 index of the desired time.
     */
    public void setCurrent(int newIndex) {
        setCurrent(newIndex, true);
    }

    /**
     * Sets the current time being shown.
     *
     * @param newIndex           The origin-0 index of the desired time.
     * @param checkUserFrames If true then go to the next "on" frame.
     */
    public void setCurrent(int newIndex, boolean checkUserFrames) {
        try {
            AnimationControl control = animationControl;
            currentIndex = newIndex;
            boolean dir = (newIndex > getCurrent());
            //Do we want to do this?
            if (checkUserFrames) {
                newIndex = clipIndex(newIndex, dir);
            }
            if (enabled) {
                if (control == null) {
                    if ((animationSet != null) && (newIndex >= 0)
                            && (newIndex < animationSet.getLength())) {
                        RealTuple tuple =
                            visad.util.DataUtility.getSample(animationSet,
                                newIndex);
                        aniValue = (Real) tuple.getComponent(0);
                    }
                } else {
                    control.setCurrent(newIndex);
                }
            }
            //            System.err.println ("setCurrent.fire ANI_VALUE");
            if ( !Misc.equals(lastTime, aniValue)) {
                lastTime = aniValue;
                firePropertyChange(ANI_VALUE, "old value", aniValue);
            }
        } catch (Exception ve) {
            LogUtil.logException("setCurrent", ve);
        }
    }

    /**
     * Step one time in the current direction (foward or reverse).
     */
    public void takeStep() {
        takeStep(direction
                 ? FORWARD
                 : REVERSE);
    }

    /**
     * Step one time in whatever direction is specified.
     * Remeber to keep direction same as before step was made; taking step does
     * not mean to change direction of looping; that is done through properties.
     * @param direction an int (0=FORWARD, 1=REVERSE)
     */
    public void takeStep(int direction) {
        try {
            int nextIndex = getCurrent();
            if (direction == FORWARD) {
                if (anyStepsForward()) {
                    nextIndex++;
                } else {
                    nextIndex = 0;
                }
            } else {
                if (anyStepsBack()) {
                    nextIndex--;
                } else {
                    nextIndex = getNumSteps() - 1;
                }
            }
            int newIndex = clipIndex(nextIndex, direction == FORWARD);
            setCurrent(newIndex);
        } catch (Exception ve) {
            LogUtil.logException("takeStep", ve);
        }
    }


    /**
     * Set the steps ok. Reset the animation.
     *
     * @param stepsOk What steps are ok
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setStepsOk(boolean[] stepsOk)
            throws RemoteException, VisADException {
        this.stepsOk = stepsOk;
        resetAnimationInfo();
    }

    /**
     * Get the next index to use
     *
     * @param nextIndex Where we are at
     * @param forward Going forward
     *
     * @return The next frame index to use
     */
    private int clipIndex(int nextIndex, boolean forward) {
        int numSteps = getNumSteps();
        if (stepsOk != null) {
            int nextGoodIndex = nextIndex;
            int delta         = (forward
                                 ? 1
                                 : -1);
            //            nextGoodIndex += delta;
            while ((nextGoodIndex >= 0) && (nextGoodIndex < stepsOk.length)) {
                if (stepsOk[nextGoodIndex]) {
                    break;
                }
                nextGoodIndex += delta;
            }
            nextGoodIndex = Math.max(Math.min(nextGoodIndex,
                    stepsOk.length - 1), 0);
            while ((nextGoodIndex >= 0) && (nextGoodIndex < stepsOk.length)
                    && !stepsOk[nextGoodIndex]) {
                nextGoodIndex -= delta;
            }
            if ((nextGoodIndex >= 0) && (nextGoodIndex < numSteps)) {
                nextIndex = nextGoodIndex;
            }
        }
        return Math.max(Math.min(nextIndex, numSteps - 1), 0);
    }

    /**
     * Set looping direction.
     * @param direction an int, 0 = FORWARD, 1 = REVERSE
     */
    public void setDirection(int direction) {
        this.direction = (direction == FORWARD);
    }


    /**
     * Step one time forward.
     */
    public void takeStepForward() {
        takeStep(FORWARD);
    }

    /**
     * Step one time backward.
     */
    public void takeStepBackward() {
        takeStep(REVERSE);
    }

    /**
     * Force recalculation of animation time steps based on all
     * data with domains mapped to this Animation's {@link visad.RealType}.
     */
    public void reCalculateAnimationSet() {

        DisplayImpl display = (DisplayImpl) getDisplay();

        if (display != null) {
            display.reAutoScale();
        }
    }

    /**
     * Checks the equality of this Animation with the object in
     * question.
     *
     * @param obj             The object to check.  Animations are equal if
     *                        their AnimationControls and RealTypes are equal.
     * @return true if they are equal
     */
    public boolean equals(Object obj) {

        if ( !(obj instanceof Animation)) {
            return false;
        }
        Animation that = (Animation) obj;
        return Misc.equals(animationControl, that.animationControl)
               && Misc.equals(aniRealType, that.aniRealType);
    }

    /**
     * Class AnimationMapListener
     *
     *
     * @author
     * @version $Revision: 1.75 $
     */
    private class AnimationMapListener implements ScalarMapListener {

        /**
         * Method called when a change occurs in the control
         *
         * @param event   event that happened
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisADError
         */
        public void controlChanged(ScalarMapControlEvent event)
                throws RemoteException, VisADException {

            int id = event.getId();

            if ((id == event.CONTROL_ADDED)
                    || (id == event.CONTROL_REPLACED)) {

                AnimationControl oldControl;
                AnimationControl newControl =
                    (AnimationControl) animationMap.getControl();

                synchronized (MUTEX) {
                    oldControl       = animationControl;
                    animationControl = newControl;
                }

                if (oldControl != null) {
                    oldControl.removeControlListener(controlListener);
                }
                if (newControl != null) {
                    /*
                     * The order of the following is switched.
                     */
                    newControl.setComputeSet(false);
                    newControl.addControlListener(controlListener);
                    resetAnimationInfo();
                }
            }
        }

        /**
         * Method called when a change occurs to a ScalarMap
         *
         * @param event  event that happened
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisADError
         */
        public void mapChanged(ScalarMapEvent event)
                throws RemoteException, VisADException {

            int id = event.getId();

            if (id == event.AUTO_SCALE) {
                resetAnimationInfo();
            }
        }
    }

    /**
     * Class AnimationControlListener
     *
     *
     * @author  IDV Development team
     */
    private class AnimationControlListener implements ControlListener {

        /**
         * Method called when a change occurs in a control
         *
         * @param ce   event that occured
         */
        public void controlChanged(ControlEvent ce) {
            try {
                boolean doFireChange = false;
                Real    oldTime      = null;
                Real    newTime      = null;
                synchronized (MUTEX) {
                    oldTime  = aniValue;
                    aniValue = missingValue;
                    int index = getCurrent();
                    if (animationSet != null) {
                        DateTime[] newTimeArray =
                            getDateTimeArray(animationSet);
                        if ((index >= 0) && (index < newTimeArray.length)) {
                            aniValue = newTimeArray[index];
                        }
                    }

                    newTime = aniValue;
                    /* For now lets not fire the event. We handle this all ourselves now */
                    if ( !Misc.equals(oldTime, aniValue)) {
                        lastTime     = aniValue;
                        doFireChange = true;
                    }
                }
                //Do this here out of the synchronized block to keep from deadlocking
                if (doFireChange) {
                    firePropertyChange(ANI_VALUE, oldTime, aniValue);
                }
            } catch (Exception excp) {
                LogUtil.logException("Error", excp);
            }
        }
    }

    /**
     * Are there any ok time steps forward of the current index
     *
     * @return valid time steps forward
     */
    private boolean anyStepsForward() {
        int current = getCurrent();
        if (stepsOk != null) {
            for (int i = current + 1; i < stepsOk.length; i++) {
                if (stepsOk[i]) {
                    return true;
                }
            }
            return false;
        }
        return (current < getNumSteps() - 1);
    }

    /**
     * Are there any ok time steps back of the current index
     *
     * @return valid time steps back
     */
    private boolean anyStepsBack() {
        int current = getCurrent();
        if (stepsOk != null) {
            for (int i = current - 1; i >= 0; i--) {
                if (stepsOk[i]) {
                    return true;
                }
            }
            return false;
        }
        return !(current == 0);
    }

    /**
     * Utility to create an array of DateTime from the given time set
     *
     * @param timeSet The time set
     *
     * @return The array
     */
    public static DateTime[] getDateTimeArray(Set timeSet) {
        if (timeSet == null) {
            return new DateTime[] {};
        }
        try {
            if (timeSet instanceof Gridded1DSet) {
                return DateTime.timeSetToArray((Gridded1DSet) timeSet);
            } else if (timeSet instanceof SingletonSet) {
                return new DateTime[] { new DateTime(
                    (Real) ((SingletonSet) timeSet).getData().getComponent(
                        0)) };
            }
        } catch (VisADException excp) {
            ;
        } catch (RemoteException re) {
            ;
        }
        return new DateTime[] {};

    }

    /**
     *  Set the Enabled property.
     *
     *  @param value The new value for Enabled
     */
    public void setEnabled(boolean value) {
        enabled = value;
        if ((enabled != value) && value) {
            setCurrent(currentIndex, false);
        }
    }

    /**
     *  Get the Enabled property.
     *
     *  @return The Enabled
     */
    public boolean getEnabled() {
        return enabled;
    }



}
