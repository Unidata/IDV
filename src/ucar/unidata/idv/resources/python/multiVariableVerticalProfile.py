#These are example jython procedures that get called by the JythonControl
from javax.swing import *
from javax.swing.JLabel import *
from javax.swing import JLabel
from javax.swing import JTextArea
from javax.swing import JScrollPane
from javax.swing import JFrame
from javax.swing import JLabel
from javax.swing import ImageIcon
from java.awt import Image
from ucar.unidata.ui import ImageUtils
from java.awt.image import BufferedImage;
from javax.swing import JTextArea
from javax.swing import JScrollPane
from javax.swing import JPanel
from javax.swing import JButton
from java.awt.event import ActionListener
import java.awt.Dimension
from ucar.unidata.util import GuiUtils
from ucar.unidata.ui import LatLonWidget
import org.jfree.chart.ChartFactory
from org.jfree.data.xy import XYSeriesCollection
from org.jfree.data.xy import XYSeries
from org.jfree.chart.plot import XYPlot
from org.jfree.chart import ChartPanel;
from org.jfree.chart import ChartUtilities;
from org.jfree.chart import ChartFactory;
from org.jfree.chart import JFreeChart;
from org.jfree.chart.axis import NumberAxis;
from org.jfree.chart.renderer.xy import XYSplineRenderer;
from org.jfree.chart.plot.PlotOrientation import VERTICAL
from org.jfree.chart.plot.PlotOrientation import HORIZONTAL
from org.jfree.chart.renderer.xy import DeviationRenderer
from java.awt import BasicStroke;
from java.awt import Color
from javax.swing import JFrame
from ucar.unidata.ui import ImageUtils
from ucar.unidata.util import FileManager
class dataListen(ActionListener):
	def __init__(self,dc):
		self.displayControl=dc
	def actionPerformed(self,event):
        	displayControl.addData("")
                handleProbe(self.displayControl,self.displayControl.probeLocation)
def handleInit (displayControl):
        displayControl.addSaveMenuItem('Save Chart Image...','menuCallback');
	displayControl.addEditMenuItem('Fix Axis Range to View','setChartRanges');
	displayControl.setVar('XRange',"None")
	displayControl.setVar('YRange',"None")

	panl=ChartPanel(None)
	panl.setPreferredSize(java.awt.Dimension(600, 400))
        panl.setMouseWheelEnabled(True);
        panl.setVisible(True)
        displayControl.setVar('panl',panl)

	jp=JPanel()
        lllisten=llListener(displayControl)
	latLonWidget = LatLonWidget("Lat: ", "Lon: ",lllisten);
        displayControl.setVar('latLonWidget',latLonWidget)
	aniWidget    = displayControl.getAnimationWidget().getContents(False);

	button1 = JButton("Add Dataset");
        button1.setActionCommand("ADD_DATASET");
	dal=dataListen(displayControl)
        button1.addActionListener(dal);


	if len(displayControl.getAnimationTimes())==0:
		aniWidget    = displayControl.getAnimationWidget().getContents(False);
	        bottomPanel = GuiUtils.leftCenterRight(aniWidget,button1,latLonWidget);
                bottomPanel = GuiUtils.inset(bottomPanel, 5)
       		displayControl.setVar('bottomPanel',bottomPanel)
	else:

		bottomPanel = GuiUtils.centerRight(button1,latLonWidget);
        	bottomPanel = GuiUtils.inset(bottomPanel, 5)
        	displayControl.setVar('bottomPanel',bottomPanel)
	panl=GuiUtils.centerBottom(panl,bottomPanel)
        jp.add(panl)
        displayControl.setJythonComponent(jp)
#      	 thumb=chart.createBufferedImage(120, 80, 360, 240,None)
#        myTimes=GridUtil.getTimeSet(di.getGrid())
#   	 displayControl.getAnimationWidget().setBaseTimes(myTimes)

	legend=JPanel()
	legend.add(JLabel("Vertical Profile"));
	#legend.add(thumb)
	displayControl.setLegendComponent (legend);
#This gets called when there is data that has been loaded
def handleData (displayControl):
	return
#This gets called when the animation time changes
def handleTime (displayControl, probeLocation):
	handleProbe (displayControl, probeLocation)
#This gets called when the probe position has changed.
def handleProbe (displayControl, probeLocation):
	animationTime = displayControl.getAnimationTime();
	#jcdi=displayControl.getDataInstance()
	#if not animationTime==None:
	#    sample=jcdi.getData().evaluate(animationTime)
        #else:
        #    sample=jcdi.getData().getSample(0)
	data=[]
	llp=probeLocation[0].getLatLonPoint()
	dsel=displayControl.getDataSelection()
	for dataC in displayControl.getDataChoices():
		dsel=dataC.getDataSelection() #this one or from displayControl
		try:
	        	if not animationTime==None:
		        	datatemp=GridUtil.getProfileAtLatLonPoint(dataC.getData(dsel).evaluate(animationTime),llp)
	        	else:
		        	datatemp=GridUtil.getProfileAtLatLonPoint(dataC.getData(dsel).getSample(0),llp)
			dataname=dataC.getName()
			data.append({'name':dataname,'data':datatemp})
		except:
			pass
	sample=displayControl.getDataChoices()[0].getData(dsel).getSample(0)
	displayControl.setVar('sample',sample)
  	displayControl.setVar('data',data)

	panl=displayControl.getVar('panl')
    	panl.setChart(makeVerticalPlot(makeChartData(data),displayControl))
    	latLonWidget=displayControl.getVar('latLonWidget')
	latLonWidget.setLat(displayControl.getDisplayConventions().formatLatLon(
                        llp.getLatitude().getValue()));
    	latLonWidget.setLon(displayControl.getDisplayConventions().formatLatLon(llp.getLongitude().getValue()));

	#Disabled Legend for now
	#chart=displayControl.getVar("chart")
    	#thumb=chart.createBufferedImage(270, 180)
	#displayControl.setLegendComponent (JLabel(ImageIcon(thumb)));
def makeChartData(data):
  	dataset=XYSeriesCollection()
        for da in data:
    		series=XYSeries(da['name'])
		ods=da['data'].getDomainSet()
    		levs=ods.getSamples(False)[2]
    		values=da['data'].getValues()[0]
    		for lev,val in zip(levs,values):
        		series.add(lev,val)
		try:
    			dataset.addSeries(series)
		except:
			if da['name'].upper()==da['name']:
				series.setKey(da['name'].lower())
			else:
				series.setKey(da['name'].upper())
    		        dataset.addSeries(series)
		finally:
			pass
    	return dataset
def makeVerticalPlot(dataset,displayControl):
    chart = ChartFactory.createXYLineChart("Vertical Profile", "Level","W", dataset,HORIZONTAL, True, True, False)

    DCV=displayControl.getDisplayConventions()
    dcolor=DCV.getColorName(displayControl.getColor())
    colors=(n for n in DCV.getColorNameList() if not n=='white' or not n==dcolor)
    displayControl.setVar('colors',colors)
    renderer=chart.getPlot().getRenderer()
    renderer.setBaseStroke(BasicStroke(2.0));
    renderer.setAutoPopulateSeriesStroke(False);
    renderer.setSeriesPaint(0,displayControl.getColor())
    for i in range(dataset.getSeriesCount()):
	if i==0:
        	renderer.setSeriesPaint(i,displayControl.getColor())
	else:
    		renderer.setSeriesPaint(i,DCV.getColor(colors.next()))

    sample=displayControl.getVar("sample")
    xName = str(GridUtil.getParamUnits(sample)[0])
    yName = str(GridUtil.getVerticalUnit(sample))
    xAxis = NumberAxis(xName);

    yAxis = NumberAxis(yName);

    #XRange=displayControl.getVar('XRange')
    #YRange=displayControl.getVar('YRange')
    #if not XRange=="None":
    #	yAxis.setRange(XRange)
    #if not YRange=="None":
    # 	xAxis.setRange(YRange)
    yAxis.setInverted(True)

    chart.getPlot().setRangeAxis(xAxis)
    chart.getPlot().setDomainAxis(yAxis)
    chart.setBackgroundPaint(Color.white)
    chart.getPlot().setBackgroundPaint(Color.white)
    chart.getPlot().setDomainGridlinesVisible(True)
    chart.getPlot().setRangeGridlinesVisible(True)
    chart.getPlot().setDomainGridlinePaint(Color.black)
    chart.getPlot().setRangeGridlinePaint(Color.black)
    displayControl.setVar('chart',chart)
    return chart
class llListener(ActionListener):
	def __init__(self,dc):
		self.displayControl=dc
	def actionPerformed( self,event):
        	handleLatLonChange(self.displayControl)
def handleLatLonChange(displayControl):
    latLonWidget=displayControl.getVar('latLonWidget')
    try:
    	lat = latLonWidget.getLat();
    	lon = latLonWidget.getLon();
    	xyz = displayControl.earthToBox(makeEarthLocation(lat, lon, 0));
    	displayControl.setProbePosition(xyz[0], xyz[1]);
    except:
        print('error')
def menuCallback(displayControl):
        filename=FileManager.getWriteFile()
	ImageUtils.writeImageToFile(displayControl.getImage(),filename)
def setChartRanges(displayControl):
        panl = displayControl.getVar('panl')
        plt=panl.getChart().getPlot()
        displayControl.setVar('XRange',plt.getDomainAxis().getRange())
	displayControl.setVar('YRange',plt.getRangeAxis().getRange())