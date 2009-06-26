

file delete -force thredds
file mkdir thredds
cd thredds
puts "Unjarring thredds.war"
exec jar -xvf ../thredds.war


cd WEB-INF/classes
if {1} {
##exec rm -r org/apache/log4j
foreach jar [glob ../lib/*.jar] {
    if {[regexp jfree $jar]} {
        puts "skipping $jar"
        continue
    }
    if {[regexp slf4j $jar]} {
#        puts "***** skipping $jar"
#        continue
    }
    puts "Unjarring $jar"
    exec jar -xvf $jar
}

puts "Making repositorytds.jar"
puts "pwd: [pwd]"
puts "exec: [exec pwd]"
set files ""
foreach file [glob *] {
    if {$file=="visad"} continue
    append files " "
    append files "\{[file tail $file]\}"
    puts "$file"
}
}




puts [exec pwd]
foreach f [glob -nocomplain  ../../../../ucar/unidata/util/DateUtil*class] {
    puts "File:$f"
    file copy -force $f ucar/unidata/util
}


set execLine "jar -cvf ../../../repositorytds.jar $files"
eval exec $execLine


