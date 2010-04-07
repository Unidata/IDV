echo "Generating workshop and pdf"
echo "tclsh ../../build/generate.tcl -clean -doall"
tclsh ../../build/generate.tcl -clean -doall
echo "cd ../processed"
cd ../processed
echo "Generating workshop.pdf"
source ../content/genpdf.sh
