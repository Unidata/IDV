<isl debug="true">
     <property name="message1" value="This is  property message1"/>
     <property name="message2" value="This is  property message2"/>
     <echo message="Here is my loop:"/>
     <group loop="5" sleep="1">
         <echo message="Here is message 1: ${message1}"/>
         <echo message="Here is message 2: ${message2}"/>
         <echo message="There is a built-in loopindex property: ${loopindex}"/>
     </group>


     <echo message="\n\nThere are also built-in time properties"/>
     <echo message="The islpath points to where the isl is: ${islpath}"/>
     <echo message="There are date/time properties, e.g.: ${yyyy}_${MM}_${dd} ${H}:${mm}:${s}"/>


</isl>
