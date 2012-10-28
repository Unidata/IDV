<isl debug="true" offscreen="false">
    <property name="dataTime" value="2001111217"/>
    <bundle file="${islpath}/isl.xidv" clear="true" >
       <!--setfiles datasource=".*" file="${islpath}/test.nc" /-->
    </bundle>

    <procedure name="mkContourNColorfilledImage">
        <!--center useprojection="false"/-->
        <display type="planviewcontourfilled"  param="${ele_name}" >
            <property name="id" value="display1" />
            <property name="displayCategory"  value="Basic"/>
            <property name="contourInfo"  value="interval=4;min=-40;max=140;base=-28;dashed=false;labels=false"/>
            <property name="SmoothingType" value="SM5S"/>
        </display>
        <pause/>
        <image file="${islpath}/${ele_name}${dataTime}.png"/>
    </procedure>
    <mkContourNColorfilledImage  ele_name="T"  />
</isl>
