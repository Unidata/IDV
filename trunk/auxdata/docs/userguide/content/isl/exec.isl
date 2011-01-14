<isl>
    <property name="imagefile" value="test.png"/>
    <group loop="1000" sleep="3600">
        <bundle file="test.xidv"/>
        <pause/>
        <image file="test.png"/>
        <exec command="scp ${imagefile} yourwebserver:/imagedir/${imagefile}"/>
    </group
</isl>
