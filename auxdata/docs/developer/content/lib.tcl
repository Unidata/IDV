##
##To process the .tml files into .html:
##tclsh tml.tcl *.tml
##

#Turn on tcl evaluation
gen::setDoTclEvaluation 1


namespace eval dg {}



proc header {title} {
return "
<html><head>
<title>$title</title>
</head>
<body>
"
}


proc footer {} {
    return "</body>"
}

proc otag {name {attrs ""}} {
    return "&lt;$name $attrs&gt;\n"
}
proc ctag {name} {
    return "&lt;/$name&gt;\n"
}
proc tag {name attrs body} {
    return "&lt;$name $attrs&gt;\n$body\n&lt;/$name&gt;\n"
}
proc dg::attr {n} {return "<i>$n</i>"}

proc pre {args} {
set v [string trim [join $args " "]]
regsub -all "<" $v {\&lt;} v 
regsub -all ">" $v {\&gt;} v
return "<pre>$v</pre>"
}




set ::javadocPrefix "http://www.unidata.ucar.edu/content/software/IDV/docs/javadoc"


proc dg::class {class {method ""}} {
    set name $class
    if {![regexp {^(.+)\.([^.]+)$} $class match path file]} {
	set path ""
    }
    regsub -all {\.} $path / path
    set html "$::javadocPrefix/$path/$file.html"
    if {$method!=""} {append html "\#$method"}
    return "<span class=class><a href=$html>$name</a></span>"
}




proc dg::class2 {class {method {}} } {
    return [dg::className $class]
}

proc dg::className {class {method {}}} {
    set ext [file extension $class]
    set file $class
    if {![regexp {^(.+)\.([^.]+)$} $class match path file ext]} {
	set path ""
	regexp {^([^.]+)\.([^.]+)$} $class match  file ext
    }
    regsub -all {\.} $path / path
    set html "$::javadocPrefix/$path/${file}.html"
    if {$method!=""} {
        return "<span class=class><a href=$html#$method>$file.$method</a></span>"
    } else {
        return "<span class=class><a href=$html>$file</a></span>"
    }

}

proc dg::package {class} {
    regsub -all {\.} $class / path
    set url "$::javadocPrefix/$path/package-summary.html"
    return "<span class=package><a href=\"$url\">$class</a></span>"
}



proc method {args} {
    return "<code>[join $args { }]</code>"
}




proc doit {} {
foreach f $::argv {

    set destf  ../[file rootname $f].html
    puts "$f -> $destf"

    set fp [open $f r]
    set contents [read $fp]
    close $fp


    set contents [subst $contents]
    set fp [open $destf w]
    puts $fp $contents
    close $fp

}
}