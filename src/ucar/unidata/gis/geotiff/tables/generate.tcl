





proc process {file} {
    set nots [list Ellipse XXDatum XXDatumE GCSE PCS PM Proj Linear Angular VertCS CT]

    set fp [open $file r]
    set contents [read $fp]
    close $fp
    set fName [file root [file tail $file]]
    if {[regsub -all {_(.)} $fName {[string toupper \1]} fName]} {
        set fName [subst $fName]
    }
         

    set iName "[string toupper [string range $fName 0 0]][string range $fName 1 end]"
    set code "    \n\n/**\n*  @author IDV development team\n*/\n\npublic static class $iName \{\n"
    


    set space "        "
    set list ""
    set anames [list]
    set avalues [list]
    foreach line [split $contents "\n"] {
        if {[regexp {^\#} $line]} {continue;}
        set line [string trim $line]
        foreach {name value} [split $line =] break
        set name [string trim $name]
        set value [string trim $value]
        if {$name == ""} {continue;}
        if {[info exists  values($name)]} {continue;}
        set cleanName $name
        foreach not $nots {
            regsub "^${not}_" $cleanName {} cleanName
        }

        if {[regexp {^\$} $value]} {
            set value $values([string range $value 1 end])
        }
        set values($name) $value
        append code "${space}public static final int $cleanName = $value;\n"
        lappend anames \"$name\"
        lappend avalues $cleanName
    }        

    set didone 0
    append code "\n\n"
    append code "${space}public static final int values\[\] = \{[join $avalues ,]\};\n\n"
    append code "${space}public static final String names\[\] = \{[join $anames ,]\};\n\n"
    append code "${space}public static String getName (int value) \{\n"
    append code "${space}    for (int i=0;i<values.length;i++) if (values\[i\] == value) return names\[i\];\n"  
    append code "${space}    return null;\n${space}\}\n"

    append code "    \}\n\n"

    set code
}



set code {
// $Id: generate.tcl,v 1.2 2004/02/27 21:22:36 jeffmc Exp $

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

package ucar.unidata.gis.geotiff;


/**
 * This class has been generated from the different properties files in the geotiff package (Of Niles Ritter).
 *  It holds the different constant values for geotiffs.
 * 
 * @author Unidata development team
*/


}

append code "public class GeneratedKeys \{"



foreach f $argv {
    append code [process $f]

}

append code "\n\n\}\n\n"

puts $code