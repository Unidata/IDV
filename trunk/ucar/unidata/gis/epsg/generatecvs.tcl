
proc camel {s} {
    return [string toupper [string range $s 0 0]][string tolower [string range $s  1 end]]
}

proc up1 {s} {
    return [string toupper [string range $s 0 0]][string range $s  1 end]
}

proc getName {col} {
    set didSyllable 0
    set name ""
    foreach syllable [split $col _] {
        if {!$didSyllable} {
            set didSyllable 1
            set name [string tolower $syllable]
        } else {
            append name [camel $syllable]
        }
    }
    return $name

}

proc getNames {line} {
    set didone 1
    set names [list]
    set nameIsIndex [list]

    foreach col [split  $line ,] {
        regsub -all \" $col {} col
        regsub -all { } $col _ col
        lappend nameIsIndex  [regexp {^\*(.*)} $col match col]
        lappend names $col
    }
    return [list $names  $nameIsIndex]
}

proc csv {s} {
    set r [list]
    set inQuote 0
    set word ""
    regsub -all {,"""} $s {,"_QUOTE_} s
    regsub -all {""",} $s {_QUOTE_",} s
    regsub -all {""} $s _QUOTE_ s
    foreach c  [split $s {}] {
        if {$inQuote} {
            if {$c == "\""} {
                set inQuote 0
            } else {
                append word $c
            }
        } else {
            if {$c == "\""} {
                set inQuote 1
            } elseif {$c == ","} {
                lappend r $word
                set word ""
            } else {
                append word $c
            }
        }
    }
    lappend r $word
    set results [list]
    foreach w $r {
        regsub -all _QUOTE_ $w \"  w
        lappend results $w
        
    }
    return $results
}


proc isDouble {s} {
    if {[string first " " $s] >=0} {return 0}
    string is double $s
}

proc isInt {s} {
    if {[string first " " $s] >=0} {return 0}
    string is int $s
}

proc jd {args} {
    set jd "\n\t/**\n"
    foreach line $args {
        append jd "\t* $line\n"
    }
    append jd "\t*/\n"
}
 
proc process {package file} {
    set fp [open $file r]
    set contents [read $fp]
    close $fp
    set contents [string trim $contents]
    append contents "\n"

    set fName [file root [file tail $file]]
    set className [up1 [getName $fName]]
    set names [list]
    set nameIsIndex [list]

    set data [list]
    set didone 0
    set inQuote 0
    set line ""
    set doProperties  0

    foreach c [split $contents {}] {
        if {$c == "\""} {
            if {$inQuote} {
                set inQuote 0
            } else {
                set inQuote 1
            }
            continue;
        }
        if {$inQuote} {
            append line $c
            continue;
        } 
        if {$c !="\n"} {
            append line $c
            continue;
        }

        if {$line == ""} { 
            continue;
        }
        if {$didone==0} {
            #The first line is the names
            foreach {names  nameIsIndex} [getNames $line] break;
        } else {
            if {[string last "\#" $line 0] == 0} {
                ##A comment perhaps
                if {[string first  doproperties $line] >=0} {
                    set doProperties  1
                }
            } else {
                lappend data  [csv $line]
            }
        }


        set line ""
        incr didone

    }

    set references [list]
    set tmpNames $names
    set names [list]
    foreach name $tmpNames {
        if {[regexp {(.*)->(.*)\.(.*)} $name match name class param]} {
            lappend references [list  $name  $class $param]
        }
        lappend names [getName $name]
    }



    set cnt [llength $names]
    foreach tuple $data {
        set idx 0
        foreach value $tuple {
#            puts -nonewline [string trim [string range $value 0 5]]
            if {$value == ""} {
                incr idx
                continue
            }
            if {![info exists typeA($idx)]} {set typeA($idx) int}
            set type $typeA($idx)
            if {[isInt $value]} {
                if {$type != "String" && $type != "double"} {
                    set typeA($idx) int
                }
            } elseif {[isDouble $value]} {
                if {$type != "String"} {
                    set typeA($idx) double
                }
            } else {
                set typeA($idx) String
            }
            incr idx
        }
       
    }

    set types [list]
    for {set i 0} {$i < $cnt} {incr i} {
        if {![info exists typeA($i)]} {
            set typeA($i) String
        }
        lappend types $typeA($i)
    }


#    puts "names: $names"
#    puts "types: $types"
    set java $::prefix1
    append java "package $package;\n\n"
    append java $::prefix2
    append java "public class $className extends ucar.unidata.util.CsvDb \{\n"
    append java "[jd {My csv file}]\tprivate static String csvFileName = \"$file\";\n\n"

    append java "[jd {Have we initialized}]\tprivate static boolean haveInitialized = false;\n\n"
    append java "[jd {The members list}]\tprivate static List members = new ArrayList ();\n\n"



    set docArgs ""
    set args [list]
    set ctor ""
    set ctor2 ""
    set idx 0
    set getters ""
    set finders ""
    set getIntBy ""
    set getDoubleBy ""
    set getStringBy ""
    set toString ""
    

    if {$doProperties} {
        ##For now let's assume the second one is the name the first one is the value.
        append java [jd {Properties generated from the csv file}]
        foreach tuple $data {
            set name [string toupper [getName [lindex $tuple 1]]]
            regsub -all { +} $name _ name
            regsub -all -- {-} $name _ name
            regsub -all -- {\.} $name {} name
            set value [lindex $tuple 0]
            append java "\tpublic static final int $name = $value;\n"
        }
        append java "\n\n"
    }





    foreach name $names type $types isIndex $nameIsIndex {
        append java [jd "The $name property"]
        append java "\tprivate $type $name;\n\n"
        append docArgs "\t* @param arg_$name The $name argument\n"
        lappend args "$type arg_$name"
        append ctor "\t\tthis.$name = arg_$name;\n"
        append ctor2 "\t\tthis.$name = "
        append getters [jd "Return the $name property." "" "@return The $name property"]
        append getters "\tpublic $type get[up1 $name] () \{return $name;\}\n\n"
        append toString "+\"   $name=\" + $name + \"\\n\""

        if {$isIndex} {
            append finders [jd "Find all of the $className objects with" "the $name value == the given value" "" "@return The found objects"]
            append finders "\tpublic static List findAll[up1 $name] ($type value) \{\n"
            append finders "\t\tList results = new ArrayList ();\n"
            append finders "\t\tfor (int i=0;i<members.size();i++) \{\n"
            append finders "\t\t\t$className obj = ($className) members.get (i);\n"
            
            switch $type {
                String {
                    append finders "\t\t\tif (obj.$name.equals (value)) results.add (obj);\n"
                }
                default {
                    append finders "\t\t\tif (obj.$name==value) results.add (obj);\n"
                }
            }
            append finders "\t\t\}\n\t\treturn results;\n\t\}\n"
        }


        switch $type {
            String {
                append getStringBy "\t\tif (varname.equals (\"$name\")) return $name;\n"
            }
            default {
                switch $type {
                    int {
                        append getIntBy "\t\tif (varname.equals (\"$name\")) return $name;\n"
                    }
                    double {
                        append getDoubleBy "\t\tif (varname.equals (\"$name\")) return $name;\n"
                    }
                }
            }
        }





        #Assume the first attribute is the key
        if {$idx == 0} {
            append finders [jd "Find the $className object with the $name value == the given value" "" "@return The object"]
            append finders "\tpublic static $className find[up1 $name] ($type value) \{\n"
            append finders "\t\tfor (int i=0;i<members.size();i++) \{\n"
            append finders "\t\t\t$className obj = ($className) members.get (i);\n"
            switch $type {
                String {
                    append finders "\t\t\tif (obj.$name.equals (value)) return obj;\n"
                }
                default {
                    append finders "\t\t\tif (obj.$name==value) return obj;\n"
                }
            }
            append finders "\t\t\}\n\t\treturn null;\n\t\}\n"
        }




        switch $type {
            int {
                append ctor2 "getInt ((String)tuple.get($idx));\n"
            }
            double {
                append ctor2 "getDouble ((String)tuple.get($idx));\n"                
            }
            default {
                append ctor2 "(String)tuple.get($idx);\n"
            }
        }
        incr idx

    }


    append java [jd "The constructor" $docArgs]

    append java "\tpublic $className ("
    append java [join $args ,]
    append java ") \{\n"
    append java $ctor
    append java "\t\}\n\n"

    append java [jd "The list based constructor" "" "@param tuple The list"]
    append java "\tpublic $className (List tuple) \{\n"
    append java $ctor2
    append java "\t\tmembers.add (this);\n"
    append java "\t\}\n\n"





    set static ""
    foreach tuple $data {
        set args [list]
        foreach v $tuple type $types {
            if {$v == ""} {
                switch $type {
                    double {
                        set v Double.NaN
                    }
                    int {
                        set v Integer.NaN
                    }
                }
            }
            if {$type == "String"} {
                regsub -all \" $v \\\" v
                set v \"$v\"
            }
            lappend args $v
        }
    }

    set numMembers [llength $names]

    append java "\tstatic \{doInit ();\}\n"
    append java [jd "The static initialization"]
    append java "\tprivate static void doInit () \{\n"
    append java "\t\tString csv = ucar.unidata.util.IOUtil.readContents (csvFileName, $className.class, (String) null);\n"
    append java "\t\tif (csv == null) \{System.err.println (\"Failed to read:\" + csvFileName); return;\}\n"
    append java "\t\tList lines = Misc.parseCsv (csv, true);\n"
    append java "\t\tfor (int i=0;i<lines.size();i++) \{\n"
    append java "\t\t\tList line = (List) lines.get (i);\n"
    append java "\t\t\tif  (line.size() < $numMembers) \{System.err.println (\"$file: line \#\" + i +\" \" + line ); continue;\}\n"
    append java "\t\t\ttry {new $className \(line);} catch (Exception exc) {System.err.println (\"Error creating $className \"+exc); exc.printStackTrace();return;}\n"
    append java "\t\t\}\n\t\}\n\n"

    append java "$getters"
    append java "$finders"



    if {$getIntBy != ""} {
        append  java [jd "Return the integer value by name" "" "@param varname The name" "@return The integer value"]
        append  java "\tpublic int findIntByName (String varname) \{\n$getIntBy \t\tthrow new IllegalArgumentException (\"Unknown name:\" + varname);\n\t\}\n"
    }

    if {$getDoubleBy != ""} {
        append  java [jd "Return the double value by name" "" "@param varname The name" "@return The double value"]
        append  java "\tpublic double findDoubleByName (String varname) \{\n$getDoubleBy \t\tthrow new IllegalArgumentException (\"Unknown name:\" + varname);\n\t\}\n"
    }

    if {$getStringBy != ""} {
        append  java [jd "Return the String value by name" "" "@param varname The name" "@return The String value"]
        append  java "\tpublic String findStringByName (String varname) \{\n$getStringBy \t\tthrow new IllegalArgumentException (\"Unknown name:\" + varname);\n\t\}\n"
    }


    foreach ref $references {
        foreach {thisParam otherClass otherParam} $ref break;
        set ThisParam [up1 [getName $thisParam]]
        set otherParam [up1 [getName $otherParam]]
        set otherClass [up1 [getName $otherClass]]
        append java "\tpublic $otherClass find$otherClass () \{\n"
        append java "\t\treturn $otherClass.find$otherParam (get$ThisParam\());\n"
        append java "\t\}\n"

    }



    append java [jd "Override toString" "" "@return String"]
    append java "\tpublic String toString () \{return \"\"$toString;\}\n"


    append java [jd "Implement main" "" "@param args The args"]
    append java "\tpublic static void main (String \[\]args) \{\}\n"


    append java "\n\n\n\t\} //End of $className\n\n"






    set fp [open $className.java w]
    puts $fp $java
    close $fp
    puts "java $package.$className"

}


set prefix1 {
// $Id: generatecvs.tcl,v 1.6 2005/02/16 17:01:04 jeffmc Exp $

/*
 * Copyright 1997-2001 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA */

}




set prefix2 {
import java.util.List;
import java.util.ArrayList;
import ucar.unidata.util.Misc;
import ucar.unidata.util.CsvDb;


/**
*  This class has been generated from the different csv files from the libgeotiff package
*
* @author IDV development team
**/

}



set package [lindex $argv 0]
set argv [lrange $argv 1 end]
foreach f $argv {
    process $package $f
}

