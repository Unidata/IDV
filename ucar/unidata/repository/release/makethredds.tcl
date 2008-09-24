

file delete -force thredds
file mkdir thredds
cd thredds
puts "Unjarring thredds.war"
exec jar -xvf ../thredds.war
cd WEB-INF/classes
foreach jar [glob ../lib/*.jar] {
    puts "Unjarring $jar"
    exec jar -xvf $jar
}

puts "Making tdsrepository.jar"
exec jar -cvf ../../../tdsrepository.jar *


