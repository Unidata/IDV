
set fp [open makedb.sql r]
set c [read 	$fp]
close $fp

set newc ""
foreach line [split $c "\n"] {
    if {![regexp {^-} $line]} {
	append newc $line "\n"
    }

}

if {0} {
public static final String TABLE_ASSOCIATIONS  = "associations";
public static final String COL_ASSOCIATIONS_NAME = TABLE_ASSOCIATIONS+".name";
public static final String COL_ASSOCIATIONS_FROM_ENTRY_ID = TABLE_ASSOCIATIONS+".from_entry_id";
public static final String COL_ASSOCIATIONS_TO_ENTRY_ID = TABLE_ASSOCIATIONS+".to_entry_id";
public static final String COL_ASSOCIATIONS_TYPE = TABLE_ASSOCIATIONS+".type";
public static final String COL_ASSOCIATIONS_ID = TABLE_ASSOCIATIONS+".id";

public static final String []ARRAY_ASSOCIATIONS= new String[]{
COL_ASSOCIATIONS_NAME,
COL_ASSOCIATIONS_FROM_ENTRY_ID,
COL_ASSOCIATIONS_TO_ENTRY_ID,
COL_ASSOCIATIONS_TYPE,
COL_ASSOCIATIONS_ID};

public static final String COLUMNS_ASSOCIATIONS = SqlUtil.comma(ARRAY_ASSOCIATIONS);
public static final String NODOT_COLUMNS_ASSOCIATIONS = SqlUtil.commaNoDot(ARRAY_ASSOCIATIONS);
public static final String INSERT_ASSOCIATIONS=
SqlUtil.makeInsert(
TABLE_ASSOCIATIONS,
NODOT_COLUMNS_ASSOCIATIONS,
SqlUtil.getQuestionMarks(ARRAY_ASSOCIATIONS.length));

}


set template {
public static final String COLUMNS = SqlUtil.comma(ARRAY);
public static final String NODOT_COLUMNS = SqlUtil.commaNoDot(ARRAY);
public static final String INSERT=
SqlUtil.makeInsert(
NAME,
NODOT_COLUMNS,
SqlUtil.getQuestionMarks(ARRAY.length));
}

set  patterns [list]



while {[regexp -nocase {CREATE\s*TABLE([^\(]+)\(([^;]+)(.*)} $newc match table params newc]} {

    set table [string trim $table]
    set TABLE [string toupper $table]
    set params [string trim $params]
    set cols [list]
    set COLS [list]
    foreach line [split $params ,] {
	set line [string trim $line]
	regexp {([^\s]+)\s} $line match col
	lappend cols $col
	lappend COLS "COL_${TABLE}_[string toupper $col]"
    }

    puts "public static class ${TABLE} \{"
    puts "public static final String NAME = \"$table\";"
    set COLS [list]
    foreach col $cols {
	set COL COL_[string toupper $col]
	lappend COLS $COL
	puts "public static final String $COL = NAME + \".$col\";"
    }


    puts "public static final String \[\]ARRAY= new String\[\] \{"
    puts [join $COLS ","]
    puts "\};\n"

    if {$TABLE == "ENTRIES"} {
	puts "public static final String UPDATE =SqlUtil.makeUpdate(NAME,COL_ID,ARRAY);"
    }

    puts $template


    foreach col $cols COL $COLS {
	lappend patterns "Tables\\.$COL"
	lappend patterns "Tables.${TABLE}.COL_[string toupper $col]"
#	puts "public static final String ${COL} = NAME + \".$col\";"
    }
    puts "\};\n"

}



