

file delete -force thredds
file mkdir thredds
cd thredds
puts "Unjarring thredds.war"
exec jar -xvf ../thredds.war
cd WEB-INF/classes
##exec rm -r org/apache/log4j
foreach jar [glob ../lib/*.jar] {
    if {[regexp log4 $jar]} {
#        puts "skipping $jar"
#        continue
    }
    if {[regexp slf4j $jar]} {
        puts "***** skipping $jar"
        continue
    }
    puts "Unjarring $jar"
    exec jar -xvf $jar
}

puts "Making repositorytds.jar"
puts "pwd: [pwd]"
puts "exec: [exec pwd]"
set files ""
foreach file [glob *] {
    append files " "
    append files "\{[file tail $file]\}"
}

set execLine "jar -cvf ../../../repositorytds.jar $files"
eval exec $execLine


