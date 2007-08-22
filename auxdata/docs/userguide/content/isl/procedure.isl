<isl>
    <procedure name="makeImage">
        <bundle file="${bundlefile}"/>
        <pause/>
        <image file="${imagefile}">
            <thumbnail width="25%"/>
        </image>
    </procedure>
    <call name="makeImage" bundlefile="test1.xidv" imagefile="test1.png"/>
    <call name="makeImage" bundlefile="test2.xidv" imagefile="test2.png"/>

<!-- Note: you can also call the procedure directly with: -->
    <makeImage bundlefile="test2.xidv" imagefile="test2.png"/>

</isl>
