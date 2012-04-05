<isl>
  <foreach foo="file:file:${islpath}/test.txt" bar="apples,oranges,bananas,peach,grape">
       <echo message="value=${foo} fruit=${bar}"/>
  </foreach>
</isl>
