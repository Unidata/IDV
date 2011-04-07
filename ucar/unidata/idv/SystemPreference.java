package ucar.unidata.idv;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;

/**
 * Thanks to McV team for providing hints here.
 *
 */
public class SystemPreference {
	
	//Constants
	private static long TOTAL_SYSTEM_MEMORY_MB = SystemMemory.getMemory()/(1024*1024);
	
	private static int MIN_SLIDER_VALUE = 5;

	private static int MAX_SLIDER_VALUE = 80 + 1;
	
	//Instance vars
	
	private final AtomicLong memory;

	private JComponent textComp;
	
	private JTextField text;

	private JComponent sliderComp;

	private  JSlider slider;
	
	private JLabel sliderLabel;
	
	private JRadioButton percentButton = new JRadioButton();

	private JRadioButton numberButton = new JRadioButton();
	
	private ButtonGroup jtbBg = GuiUtils.buttonGroup(percentButton, numberButton);
	
	SystemPreference(final AtomicLong memory) {
		this.memory = memory;
		createSlider(); 
		createText();
		
		percentButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				containerXable(textComp, false);
				containerXable(sliderComp, true);
				slider.setValue(convertToPercent(memory.get()));
			}
		});

		numberButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				containerXable(sliderComp, false);
				containerXable(textComp, true);
                text.setText(memory.get() + "");                                                                                                                                                                    
			}
		});
		percentButton.setSelected(true);
		containerXable(textComp, false);
		containerXable(sliderComp, true);
	}
	
	private static int convertToPercent(final long number) {
		return Math.round(((float)number/TOTAL_SYSTEM_MEMORY_MB) * 100);
	}
	
	private static long convertToNumber(final int percent) {
		return (long)((percent/100.0)*TOTAL_SYSTEM_MEMORY_MB);
	}
	
	/**
	 * Recursively enable or disable the container, and whatever is in the container.
	 * @param container
	 * @param enable
	 */
	private void containerXable(final Container container, final boolean enable) {
		if (container == null) {
			return;
		} else {
			for (java.awt.Component c : container.getComponents()) {
				c.setEnabled(enable);
				if (c instanceof Container) {
					containerXable((Container)c, enable);
				}
			}
		}
	}
	
	private void createSlider() {
    	    	    	    	
    	sliderLabel = new JLabel("Use " + convertToPercent(memory.get()) + "% ");
    	        
        final JLabel postLabel = new JLabel(" of available memory (" + TOTAL_SYSTEM_MEMORY_MB + "mb" + ")");
        
        final ChangeListener percentListener = new ChangeListener() {                                                                                                                                                                          
            public void stateChanged(ChangeEvent evt) {                                                                                                                                                                                         
                if (sliderComp == null || !sliderComp.isEnabled()) {
                	return;                                                                                                                                                                                           
                }
            	
            	final int sliderValue = ((JSlider)evt.getSource()).getValue();
                sliderLabel.setText("Use " + sliderValue + "% ");
                memory.getAndSet(convertToNumber(sliderValue));
            }                                                                                                                                                                                                                                   
        };
        	
        final JComponent[] sliderComps = GuiUtils.makeSliderPopup(MIN_SLIDER_VALUE, MAX_SLIDER_VALUE, convertToPercent(memory.get()) , percentListener);                                                                                                            
        slider = (JSlider) sliderComps[1];                                                                                                                                                                                                  
        slider.setMinorTickSpacing(5);                                                                                                                                                                                                      
        slider.setMajorTickSpacing(10);                                                                                                                                                                                                     
        slider.setSnapToTicks(true);                                                                                                                                                                                                        
        slider.setExtent(1);                                                                                                                                                                                                                
        slider.setPaintTicks(true);                                                                                                                                                                                                         
        slider.setPaintLabels(true);                                                                                                                                                                                                        
        sliderComps[0].setToolTipText("Set maximum memory by percent");
        
        sliderComp = GuiUtils.center(GuiUtils.hbox(sliderLabel, sliderComps[0], postLabel));
	}
	
	private void createText() {		
    	text = new JTextField(10);
        text.setText(memory.get() + "");
        text.addKeyListener( new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			      char c = e.getKeyChar();
			      if ( ((c < '0') || (c > '9')) && (c != KeyEvent.VK_BACK_SPACE)) {
			         e.consume();  // ignore event
			      }
			   }
			@Override
			public void keyReleased(KeyEvent e) {
				int mem = Integer.valueOf(((JTextField)e.getSource()).getText());
				memory.getAndSet(mem);
			}
			@Override
			public void keyPressed(KeyEvent e) {}
		});
        
        text.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
			}
		});
        
        textComp = GuiUtils.hbox(text, new JLabel(" megabytes"));
    }

	
	JComponent getJComponent() {
        List<JComponent> formatComps = new ArrayList<JComponent>();
        formatComps.add(GuiUtils.topBottom(GuiUtils.rLabel("Memory:   "),new JPanel()));
        formatComps.add(GuiUtils.left(GuiUtils.topBottom(GuiUtils.hbox(percentButton, sliderComp), GuiUtils.hbox(numberButton, textComp))));
        
        return GuiUtils.inset(GuiUtils.topLeft(GuiUtils.doLayout(formatComps, 2,
                GuiUtils.WT_N, GuiUtils.WT_N)), 5);
	}
    
    PreferenceManager getSystemManager() {
    	return new PreferenceManager() {
			@Override
			public void applyPreference(final XmlObjectStore store, final Object data) {
				store.put(IdvConstants.PREF_MEMORY, ((AtomicLong)data).get());
			}
		};
    }        
}
