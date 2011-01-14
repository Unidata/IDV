<isl>
    <output 
        file="output1.txt" 
        template="Here is the text: ${text}"
        template:text="${thetext} ">

        <output template="text" thetext="The text I am writing"/>

        <output template="text" thetext="More text I am writing"/>

   </output>

</isl>