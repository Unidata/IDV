/*
 * Copyright 1997-2025 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.mac;


import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;

import ucar.unidata.idv.IdvManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.GuiUtils;

import java.awt.Image;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.ImageIcon;
import javax.swing.UIManager;


/**
 * The MacBridge class for OS X specific stuff.
 */
public class MacBridge extends IdvManager  implements ApplicationListener  {
    static final String ADD_APPLICATION_LISTENER  = "addApplicationListener";

    static final String SET_ABOUT_HANDLER          = "setAboutHandler";

    static final String SET_DOCK_ICON_IMAGE = "setDockIconImage";

    static final String GET_APPLICATION = "getApplication";

    static final String SET_ENABLED_PREFERENCES_MENU = "setEnabledPreferencesMenu";



    Object application;
    /**
     * Create this manager.
     *
     * @param idv The IDV
     */
    public MacBridge(IntegratedDataViewer idv) {
        super(idv);
        init();
    }

    /**
     * Init
     */
    private void init() {
        String icon =
            getIdv().getProperty("idv.ui.logo",
                                 "/ucar/unidata/idv/images/logo.gif");
        Class<?>            eaAppClass     = null;

        Class<?>            appEventClass  = null;
        try {
            // Load com.apple.eawt.Application class
            Class<?> applicationClass = Class.forName("com.apple.eawt.Application");

            // getApplication
            Method getApplicationMethod = applicationClass.getMethod(GET_APPLICATION);
            // run it and obtain the result
            application = getApplicationMethod.invoke(null);
            Object image = GuiUtils.getImage(icon, getClass());
            if (application!= null) {
                addApplicationListener(application);
                setDockIcon(application, image);
                setEnabledPreferencesMenu(application);
            }
        } catch (Throwable exc) {
            System.out.println("error" + exc);
        }
        //Application application = Application.getApplication();
        //application.addApplicationListener(this);
        //addApplicationListener(this);
        //application.setDockIconImage(logo);
        //application.setEnabledPreferencesMenu(true);
    }

    private Method getMethod(Class<?> targetClass, String methodName, Class<?>...parameterTypes) throws NoSuchMethodException {
        Method method = targetClass.getMethod(methodName, parameterTypes);
        setAccessible(method);
        return method;
    }

    private void setAccessible(Method method) {
        try {
            method.setAccessible(true);
        } catch (Exception e) {
            System.err.println("trouble w/setAccessible " + e);
        }
    }

    /**
     * Set the application.
     *
     * @param application   The applicaton
     * @param logo          The image
     */
    private void setDockIcon(Object application,Object logo) {
        if (application == null) return;
        try {
            Method setDockIconImageMethod = getMethod(application.getClass(), SET_DOCK_ICON_IMAGE, java.awt.Image.class);
            System.err.println("got iconImage  method" + logo);
            setAccessible(setDockIconImageMethod);
            setDockIconImageMethod.invoke(application,  new Object[]{logo});
            System.err.println("success");

        }
        catch (Exception e) {
            System.err.println(" no go for dock icon image  " + e);
            e.printStackTrace();
        }
    }


    /**
     * Use reflection to call com.apple.eawt.Application.setEnabledPreferencesMenu
     */
    private void setEnabledPreferencesMenu(Object application) {
        try {
            Method setEnabledPreferencesMenuMethod = getMethod(application.getClass(), SET_ENABLED_PREFERENCES_MENU, boolean.class);
            setEnabledPreferencesMenuMethod.setAccessible(true);
            invokeMethod(application, SET_ENABLED_PREFERENCES_MENU, Boolean.TRUE);
        } catch (Exception e) {
            System.err.println("MacBridge: Error setting preferences menu" + e);
        }
    }


    /**
     *Added more reflection to have a more robust execution
     */
    public void handleOpenFile(Object event) {

        try{
            Method setHandledMethod = event.getClass().getMethod("setHandled", boolean.class);
            setHandledMethod.setAccessible(true);
            setHandledMethod.invoke(event,true);
        }catch(Exception e) {
            System.err.println("MacBridge: com.apple.eawt.Application.handleOpenFile not worked . " + e);
        }
    }

    /**
     *Added more reflection to have a more robust execution
     */
    public void handlePreferences(Object event) {
        getIdv().showPreferenceManager();
        try{
            Method setHandledMethod = event.getClass().getMethod("setHandled", boolean.class);
            setHandledMethod.setAccessible(true);
            setHandledMethod.invoke(event,true);
        }catch(Exception e) {
            System.err.println("MacBridge: com.apple.eawt.Application.handlePreferences not worked . " + e);
        }
    }

    /**
     *Added more reflection to have a more robust execution
     */
    public void handlePrintFile(Object event) {
        try{
            Method setHandledMethod = event.getClass().getMethod("setHandled", boolean.class);
            setHandledMethod.setAccessible(true);
            setHandledMethod.invoke(event,true);
        }catch(Exception e) {
            System.err.println("MacBridge: com.apple.eawt.Application.handlePrintFile not worked . " + e);
        }
    }

    /**
     *Added more reflection to have a more robust execution
     */
    public void handleQuit(Object event) {
        getIdv().quit();
        try{
            Method setHandledMethod = event.getClass().getMethod("setHandled", boolean.class);
            setHandledMethod.setAccessible(true);
            setHandledMethod.invoke(event,true);
        }catch(Exception e) {
            System.err.println("MacBridge: com.apple.eawt.Application.handleQuit not worked . " + e);
        }
    }

    /**
     *Added more reflection to have a more robust execution
     */
    public void handleReOpenApplication(Object event) {
        try{
            Method setHandledMethod = event.getClass().getMethod("setHandled", boolean.class);
            setHandledMethod.setAccessible(true);
            setHandledMethod.invoke(event,true);
        }catch(Exception e) {
            System.err.println("MacBridge: com.apple.eawt.Application.handleReOpenApplication not worked . " + e);
        }
    }

    /**
     * Helper method to invoke a method reflectively.
     * @param object The object to invoke the method on.
     * @param methodName The name of the method.
     * @param params The parameters to pass to the method.
     * @throws Exception If any error occurs during reflection.
     */
    private void invokeMethod(Object object, String methodName, Object... params) throws Exception {
        Class<?>[] paramTypes = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }
        Method method = object.getClass().getMethod(methodName, paramTypes);
        method.setAccessible(true);
        method.invoke(object, params);
    }

    private void addApplicationListener(Object application) {
        try {
            Class<?> applicationListenerClass = Class.forName("com.apple.eawt.ApplicationListener");
            Method addAppListenerMethod = application.getClass().getMethod("addApplicationListener", applicationListenerClass);
            addAppListenerMethod.setAccessible(true);
            addAppListenerMethod.invoke(application, this);
        } catch (Exception e) {
            //Logger.getLogger(MacBridge.class.getName()).log(Level.SEVERE, "Error adding Mac App listener", e);
        }
    }
    /**
     * {@inheritDoc}
     */
    public void handleAbout(ApplicationEvent event) {
        getIdv().getIdvUIManager().about();
        event.setHandled(true);
    }

    /**
     * {@inheritDoc}
     */
    public void handleOpenApplication(ApplicationEvent event) {}

    /**
     * {@inheritDoc}
     */
    public void handleOpenFile(ApplicationEvent event) {}

    /**
     * {@inheritDoc}
     */
    public void handlePreferences(ApplicationEvent event) {
        getIdv().showPreferenceManager();
        event.setHandled(true);
    }

    /**
     * {@inheritDoc}
     */
    public void handlePrintFile(ApplicationEvent event) {}

    /**
     * {@inheritDoc}
     */
    public void handleQuit(ApplicationEvent event) {
        getIdv().quit();
        event.setHandled(true);
    }

    /**
     * {@inheritDoc}
     */
    public void handleReOpenApplication(ApplicationEvent event) {}
}
