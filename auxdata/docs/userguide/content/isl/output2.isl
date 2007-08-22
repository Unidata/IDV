<isl>
    <output 
        file="output2.txt" 
        template="Place 1 text: ${place1} Place 2 text: ${place2}"
        template:place1="${value1} -- ${value2} "
        template:place2="  ${someothervalue}  "
        >
        <output template="place1" value1="the value 1" value2="the value 2"/>
        <output template="place2" someothervalue="some value"/>
        <output template="place2" someothervalue="some other value"/>
   </output>

</isl>