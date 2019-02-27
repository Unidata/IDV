#These are example jython procedures that get called by the JythonControl
from javax.swing import *
from javax.swing.JLabel import *
from javax.swing import JLabel
from javax.swing import JTextArea
from javax.swing import JScrollPane
from java.awt import Color;
from java.awt import Font;
from org.jfree.chart.title import PaintScaleLegend
from javax.swing import JPanel;
from javax.swing import JFrame
from org.jfree.chart import ChartPanel;
from org.jfree.chart import ChartUtilities;
from org.jfree.chart import JFreeChart;
from org.jfree.chart.axis import AxisLocation;
from org.jfree.chart.axis import NumberAxis;
from org.jfree.chart.block import BlockBorder;
from org.jfree.chart.plot import XYPlot;
from org.jfree.chart.renderer import GrayPaintScale;
from org.jfree.chart.renderer import PaintScale;
from org.jfree.chart.renderer.xy import XYBlockRenderer;
from org.jfree.chart.title import PaintScaleLegend;
from org.jfree.data import DomainOrder;
from org.jfree.data.general import DatasetChangeListener;
from org.jfree.data.general import DatasetGroup;
from org.jfree.data.xy import XYZDataset;
from org.jfree.ui import ApplicationFrame;
from org.jfree.ui import RectangleEdge;
from org.jfree.ui import RectangleInsets;
from org.jfree.ui import RefineryUtilities;
from org.jfree.data.xy import XYZDataset;
from org.jfree.data.xy import DefaultXYZDataset
from org.jfree.data.general import DefaultHeatMapDataset;
from org.jfree.data.general import HeatMapDataset;
from org.jfree.data.general import HeatMapUtilities;
from org.jfree.chart.annotations import XYDataImageAnnotation
from org.jfree.chart import ChartFactory;
from org.jfree.chart.plot import PlotOrientation
from org.jfree.data.xy import XYDataset;
from org.jfree.data.xy import XYSeriesCollection;
from org.jfree.ui import  Layer
from org.jfree.chart.renderer import LookupPaintScale
from java.lang import Math
#This gets called when the JythonControl is first created.
def handleInit (displayControl):
	exampleInit(displayControl);
#This gets called when there is data that has been loaded
def handleData (displayControl):
	exampleData(displayControl);
#This gets called when the probe position has changed.
def handleProbe (displayControl, probeLocation):
	exampleProbe(displayControl,probeLocation);
#This gets called when the animation time changes
def handleTime (displayControl, probeLocation):
	exampleTime(displayControl,probeLocation);
#This gets called when the JythonControl is first created.
def exampleInit (displayControl):
	#You can add any number of menuitems to the different menus
	#displayControl.addSaveMenuItem('save 2','menuCallback');
	#displayControl.addFileMenuItem('file 1','menuCallback');
	#displayControl.addEditMenuItem('edit 1','menuCallback');
	#displayControl.addViewMenuItem('view 1','menuCallback');
        #Create a JTextArea and put it into the display's window
	comp = JTextArea  ("",8,30)
        comp.setEditable(0)
	#setJythonComponent sets the GUI

	panl=ChartPanel(None)
        panl.setPreferredSize(java.awt.Dimension(600, 400))
        panl.setMouseWheelEnabled(True);
        panl.setVisible(True)
        displayControl.setVar('panl',panl)
	jp=JPanel()
	jp.add(panl)
	displayControl.setJythonComponent (jp)
        #Save state with the setVar/getVar methods on the display
	#displayControl.setVar ('comp', comp)
	#You can also set a component in the legend
	legendComp = JLabel("vertical Crosssection");
	displayControl.setLegendComponent (legendComp);
        displayControl.setVar("ctw","None")
#This gets called when there is data that has been loaded
def exampleData (displayControl):
	#The getDataList method returns a list of the data, one per data choice
	return
#This gets called when the probe position has changed.
#The probeLocation is an array of EarthLocationTuple-s
#For point, level and  vertical probes the length is 1
#For the horizonal probe the length is 2 (the end points of the probe line)
#For the area probe the  length is 4 (upper-left, upper-right, lower-right, lower-left)
def exampleProbe (displayControl, probeLocation):
        #You can get the current animation time with:
	animationTime = displayControl.getAnimationTime();
	#You can get a list of all animation times with:
	#times = displayControl.getAnimationTimes();
	#You can get a list of all times from the data with:
	#times = displayControl.getTimesFromData();
        #This samples the data at the current probe location
        sample = displayControl.sampleAtTime();
	displayControl.setVar('sample',sample)
        comp = displayControl.getVar ('comp');
	#comp.setText ('sample: ' +str(sample) + ' at:' + str(animationTime))
        #You can also just call sample to get all values across time:
        #sample = displayControl.sample();
        ctw=displayControl.getVar('ctw')
	if ctw=="None":
		ctw=displayControl.getColorTableWidget(displayControl.getRangeForColorTable())
		ct=ctw.getColorTable()
		try:
		     ct.setRange(displayControl.getColorRangeFromData())
		except:
		     ct.setRange(displayControl.getRangeForColorTable())
	displayControl.setVar("ct",ct)
        if animationTime==None:
		sample=sample.getSample(0)
	nlola=sample.getDomainSet().getLengths()[0]
	nlev=sample.getDomainSet().getLengths()[1]
	levs=getLevels(displayControl.getGridDataInstance().getData())
	displayControl.setVar('levs',levs)

	dataset=createMapDataset2(sample,nlola,levs)
	chart=createMapChart(dataset,displayControl,nlola,levs)
	panl=displayControl.getVar('panl')
	panl.setChart(chart)
#This gets called when the animation time changes
def exampleTime (displayControl, probeLocation):
        #Just turn around and call handleProbe
	exampleProbe(displayControl, probeLocation);
def menuCallback(displayControl):
	comp = displayControl.getVar ('comp');
	comp.setText ('menuCallback called')
def createMapDataset2(sample,nlola,levs):
	d = DefaultHeatMapDataset(nlola, len(levs), 1, nlola, min(levs), max(levs));
	for j in range(len(levs)): #-1,-1,-1):
		for i in range(nlola):
			d.setZValue(i, len(levs)-1-j, sample.getValues()[0][j*nlola+i]);

	return d;

def createMapChart(dataset,displayControl,nlola,levs):
	chart = ChartFactory.createScatterPlot(displayControl.getDataChoice().getName()+" Vertical Crossection",
          "X", "Y", XYSeriesCollection(), PlotOrientation.VERTICAL, True, False,
          False);

	ct=displayControl.getVar('ct')
	ps=LookupPaintScale(ct.getRange().getMin(),ct.getRange().getMax(),ct.getColorList()[0])
	fr=frange(ct.getRange().getMin(),ct.getRange().getMax(),len(ct.getColorList()))
	for i in range(len(ct.getColorList())):
		ps.add(fr.next(),ct.getColorList()[i])
		image = HeatMapUtilities.createHeatMapImage(dataset, ps);
		ann = XYDataImageAnnotation(image, 1, min(levs), nlola, max(levs), True);
	displayControl.setVar('ann',ann)
	plot =chart.getPlot();
	plot.setDomainPannable(True);
	plot.setRangePannable(True);
	plot.getRenderer().addAnnotation(ann, Layer.BACKGROUND);
	xAxis = plot.getDomainAxis();
	xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	xAxis.setLowerMargin(0.0);
	xAxis.setUpperMargin(0.0);
	displayControl.setVar('xAxis',xAxis)
	yAxis = plot.getRangeAxis();
	yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	yAxis.setLowerMargin(0.0);
	yAxis.setUpperMargin(0.0);
	yAxis.setRange(min(levs),max(levs))

	yAxis.setInverted(True)
	scaleAxis = NumberAxis(displayControl.getDataChoice().getName());
	scaleAxis.setAxisLinePaint(Color.white);
	scaleAxis.setTickMarkPaint(Color.white);
	scaleAxis.setTickLabelFont(Font("Dialog", Font.PLAIN, 7));
    #scaleAxis.setInverted(True)
	sample=displayControl.getVar('sample')

	xAxis.setLabel("N")
	try:
		yAxis.setLabel(str(GridUtil.getVerticalUnit(sample)))
		scaleAxis.setLabel(displayControl.getDataChoice().getName()+" ["+str(GridUtil.getParamUnits(sample)[0])+"]")
	except:
		pass
	legend=PaintScaleLegend(ps,scaleAxis);
	legend.setStripOutlineVisible(False);
	legend.setSubdivisionCount(20);
	legend.setAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
	legend.setAxisOffset(5.0);
	legend.setMargin(RectangleInsets(5, 5, 5, 5));
	legend.setFrame(BlockBorder(Color.white));
	legend.setPadding(RectangleInsets(10, 10, 10, 10));
	legend.setStripWidth(10);
	legend.setPosition(RectangleEdge.LEFT);
	chart.addSubtitle(legend);
	return chart;
def frange(start, stop, N):
	i = start
	while i < stop:
		yield i
		i += float(stop-start)/N