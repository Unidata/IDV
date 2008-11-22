/*
 * $Id: ViewManager.java,v 1.401 2007/08/16 14:05:04 jeffmc Exp $
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
 * This library is distributed in the hope that it will be2 useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */



package ucar.unidata.java3d;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;

import java.util.ArrayList;
import java.util.List;


import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


import java.awt.*;
import javax.swing.*;
import javax.media.j3d.*;
import javax.vecmath.*;






/**
 *
 * @author IDV development team
 */

public class LightInfo {
    private DirectionalLight light;
    private String name;
    private Vector3f direction;
    private Color3f color; 
    private boolean visible = true;
    private Point3d location;

    private JCheckBox visibleCbx;
    private JSlider slider;
    private JTextField directionXFld;
    private JTextField directionYFld;
    private JTextField directionZFld;
    private JTextField locationXFld;
    private JTextField locationYFld;
    private JTextField locationZFld;


    public LightInfo() {
    }

    public LightInfo(LightInfo that) {
        this.name = that.name;
        this.direction = that.direction;
        this.location = that.location;
        this.color = that.color;
        if(direction==null)
            direction = new Vector3f(0.0f, 0.0f, 1.0f);
        if(location==null) 
            location = new Point3d(0.0,0.0,0.0);
        getLight();
    }

    public LightInfo(String name,     Point3d location, Vector3f direction) {
        this.name =name;
        if(direction==null)
            direction = new Vector3f(0.0f, 0.0f, 1.0f);
        if(location==null) 
            location = new Point3d(0.0,0.0,0.0);
        this.direction = direction;
        this.location = location;
        this.color = new Color3f(0.0f,0.0f,0.0f);
        getLight();
    }

    public void initWith(LightInfo that) {
        this.color = that.color;
        this.visible = that.visible;
        updateLight();
    }

    public void getPropertyComponents(List comps,final ObjectListener listener) {
        visibleCbx= new JCheckBox(getName(), visible);
        float[]rgb = color.get().getRGBComponents(null);
        slider = new JSlider(0,100,(int)(rgb[0]*100));


        
        float[]xyz = new float[3];
        direction.get(xyz);
        directionXFld = makeField(""+xyz[0],listener,"X Direction");
        directionYFld = makeField(""+xyz[1],listener,"Y Direction");
        directionZFld = makeField(""+xyz[2],listener,"Z Direction");

        double[]pxyz = new double[3];        
        location.get(pxyz);
        locationXFld = makeField(""+pxyz[0],listener,"");
        locationYFld = makeField(""+pxyz[1],listener,"");
        locationZFld = makeField(""+pxyz[2],listener,"");

        List fldComps = Misc.newList(GuiUtils.rLabel("Direction:"),directionXFld,directionYFld,directionZFld);
        //        fldComps.addAll(Misc.newList(GuiUtils.rLabel("Location:"),locationXFld,locationYFld,locationZFld));

        comps.add(visibleCbx);
        GuiUtils.tmpInsets = new Insets(0,2,0,0);
        comps.add(GuiUtils.vbox(slider, GuiUtils.left(GuiUtils.doLayout(fldComps,4,GuiUtils.WT_N,GuiUtils.WT_N))));

        if(listener!=null) {
            visibleCbx.addActionListener(listener);
            slider.addChangeListener(listener);
        }

    }

    private JTextField makeField(String s, ObjectListener listener, String tip) {
        JTextField fld =  new JTextField(s,3);
        if(listener!=null)
            fld.addActionListener(listener);
        fld.setToolTipText(tip);
        return fld;
    }


    public void applyProperties() {
        visible = visibleCbx.isSelected();
        float f = (float)(slider.getValue()/100.0f);
        color = new Color3f(f,f,f);
        location = new Point3d(new Double(locationXFld.getText()).doubleValue(),
                               new Double(locationYFld.getText()).doubleValue(),
                               new Double(locationZFld.getText()).doubleValue());

        direction = new Vector3f(new Float(directionXFld.getText()).floatValue(),
                               new Float(directionYFld.getText()).floatValue(),
                               new Float(directionZFld.getText()).floatValue());


        updateLight();
    }

    public void brighter() {
        float[]rgb = color.get().getRGBComponents(null);
        rgb[0] = (float)Math.min(rgb[0]+0.1, 1.0);
        rgb[1] = (float)Math.min(rgb[1]+0.1, 1.0);
        rgb[2] = (float)Math.min(rgb[2]+0.1, 1.0);
        color = new Color3f(rgb);
        updateLight();
    }

    public DirectionalLight getLight() {
        if(light==null) {
            light =new DirectionalLight(visible, color, direction);
            BoundingSphere bounds =
                new BoundingSphere(location, Double.POSITIVE_INFINITY);
            light.setCapability(DirectionalLight.ALLOW_DIRECTION_READ);
            light.setCapability(DirectionalLight.ALLOW_DIRECTION_WRITE);
            light.setCapability(Light.ALLOW_COLOR_READ);
            light.setCapability(Light.ALLOW_COLOR_WRITE);
            light.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_READ);
            light.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
            light.setCapability(Light.ALLOW_SCOPE_READ);
            light.setCapability(Light.ALLOW_SCOPE_WRITE);
            light.setCapability(Light.ALLOW_STATE_READ);
            light.setCapability(Light.ALLOW_STATE_WRITE);
            light.setInfluencingBounds(bounds);
        }
        return light;
    }

    public void updateLight() {
        getLight().setEnable(visible);
        getLight().setColor(color);
        getLight().setDirection(direction);
        getLight().setInfluencingBounds(new BoundingSphere(location, Double.POSITIVE_INFINITY));
    }


    /**
       Set the Name property.

       @param value The new value for Name
    **/
    public void setName (String value) {
	name = value;
    }

    /**
       Get the Name property.

       @return The Name
    **/
    public String getName () {
	return name;
    }

    /**
       Set the Direction property.

       @param value The new value for Direction
    **/
    public void setDirection (Vector3f value) {
	direction = value;
    }

    /**
       Get the Direction property.

       @return The Direction
    **/
    public Vector3f getDirection () {
	return direction;
    }

    public void setColor (Color3f value, boolean updateGui) {
        setColor(value);
        if(updateGui && slider!=null) {
            float[]rgb = getColor().get().getRGBComponents(null);
            int sliderValue = (int)(100*rgb[0]);
            slider.setValue(sliderValue);
        }
    }


    /**
       Set the Color property.

       @param value The new value for Color
    **/
    public void setColor (Color3f value) {
	color = value;
    }

    /**
       Get the Color property.

       @return The Color
    **/
    public Color3f getColor () {
	return color;
    }

    public void setVisible (boolean value, boolean updateGui) {
        setVisible(value);
        if(updateGui && visibleCbx!=null) {
            visibleCbx.setSelected(visible);
        }
    }



    /**
       Set the Visible property.

       @param value The new value for Visible
    **/
    public void setVisible (boolean value) {
	visible = value;
    }

    /**
       Get the Visible property.

       @return The Visible
    **/
    public boolean getVisible () {
	return visible;
    }



    public String toString() {
        return name;
    }    

/**
Set the Location property.

@param value The new value for Location
**/
public void setLocation (Point3d value) {
	location = value;
}

/**
Get the Location property.

@return The Location
**/
public Point3d getLocation () {
	return location;
}


}

