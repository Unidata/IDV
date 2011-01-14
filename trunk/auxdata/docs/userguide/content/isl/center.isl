<isl>
    <bundle file="test.xidv"/>
    <pause/>

    <echo message="Set the projection to be the projection of the first display"/>
    <center/>
    <pause seconds="10"/>

    <echo message="Center at a point"/>
    <center lat="15" lon="-65"/>
    <pause seconds="10"/>

    <echo message="Set the projection to be the lat/lon box"/>
    <center north="40.0" south="30" east="-90" west="-100"/>
    <pause seconds="10"/>

    <echo message="Set the projection from the specified display"/>
    <center display="display1" useprojection="true"/>
    <pause seconds="10"/>

    <echo message="Center at the center of the given displays projection"/>
    <center display="display1" useprojection="false"/>
    <pause seconds="10"/>

</isl>
