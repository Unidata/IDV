echo "Generating user guide and pdf"
echo "tclsh ../../build/generate.tcl -clean -doall"
tclsh ../../build/generate.tcl -clean -doall
echo "cd ../processed"
cd ../processed
echo "Generating userguide.pdf"
source ../content/genpdf.sh
