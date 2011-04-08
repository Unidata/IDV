package ucar.unidata.idv;

//~--- non-JDK imports --------------------------------------------------------

import ucar.unidata.util.GuiUtils;
import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;

//~--- JDK imports ------------------------------------------------------------

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

/**
 * Preferences for the "System" tab. Thanks to McV team for providing hints
 * here. Note the slider is not always displayed because system memory is not 
 * always available.
 *
 */
public class SystemPreference {

    /** Max value for the slider. */
    private static final int MAX_SLIDER_VALUE = 80 + 1;

    /** Min value for the slider. */
    private static final int MIN_SLIDER_VALUE = 5;

    /** Total system memory. */
    private static final long TOTAL_SYSTEM_MEMORY_MB = SystemMemory.getMemory() / (1024 * 1024);

    /** If system memory is available display slider. */
    private final boolean displaySlider = SystemMemory.isMemoryAvailable();

    /** The jtb bg. */
    private ButtonGroup jtbBg;

    /** The memory. */
    private final AtomicLong memory;

    /** The slider. */
    private JSlider slider;

    /** The slider button. */
    private JRadioButton sliderButton;

    /** The slider comp. */
    private JComponent sliderComp;

    /** The slider label. */
    private JLabel sliderLabel;

    /** The slider sub comp. */
    private JComponent sliderSubComp;

    /** The text. */
    private JTextField text;

    /** The text button. */
    private JRadioButton textButton;

    /** The text comp. */
    private JComponent textComp;

    /** The text sub comp. */
    private JComponent textSubComp;

    /**
     * Instantiates a new system preference.
     *
     * @param memory
     *            the memory
     */
    SystemPreference(final AtomicLong memory) {
        this.memory = memory;

        if (displaySlider) {
            createSlider();
        }

        createText();

        if (displaySlider) {
            containerXable(textSubComp, false);
            containerXable(sliderSubComp, true);
            sliderButton.setSelected(true);
        }

        jtbBg = GuiUtils.buttonGroup(sliderButton, textButton);
    }

    /**
     * Recursively enable or disable the container, and whatever is in the
     * container.
     *
     * @param container
     *            the container
     * @param enable
     *            the enable
     */
    private void containerXable(final Container container, final boolean enable) {
        if (container == null) {
            return;
        } else {
            for (java.awt.Component c : container.getComponents()) {
                c.setEnabled(enable);
                if (c instanceof Container) {
                    containerXable((Container) c, enable);
                }
            }
        }
    }

    /**
     * Creates the memory slider UI.
     */
    private void createSlider() {
        sliderLabel = new JLabel("Use " + convertToPercent(memory.get()) + "% ");

        final JLabel         postLabel       = new JLabel(" of available memory (" + TOTAL_SYSTEM_MEMORY_MB + "mb"
                                                   + ")");
        final ChangeListener percentListener = new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                if ((sliderComp == null) ||!sliderComp.isEnabled()) {
                    return;
                }

                final int sliderValue = ((JSlider) evt.getSource()).getValue();

                sliderLabel.setText("Use " + sliderValue + "% ");
                memory.getAndSet(convertToNumber(sliderValue));
            }
        };
        final JComponent[] sliderComps = GuiUtils.makeSliderPopup(MIN_SLIDER_VALUE, MAX_SLIDER_VALUE,
                                             convertToPercent(memory.get()), percentListener);

        slider = (JSlider) sliderComps[1];
        slider.setMinorTickSpacing(5);
        slider.setMajorTickSpacing(10);
        slider.setSnapToTicks(true);
        slider.setExtent(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        sliderComps[0].setToolTipText("Set maximum memory by percent");
        sliderButton = new JRadioButton();
        sliderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                containerXable(textSubComp, false);
                containerXable(sliderSubComp, true);
                slider.setValue(convertToPercent(memory.get()));
                textButton.setSelected(false);
            }
        });
        sliderSubComp = GuiUtils.center(GuiUtils.hbox(sliderLabel, sliderComps[0], postLabel));
        sliderComp    = GuiUtils.hbox(sliderButton, sliderSubComp);
    }

    /**
     * Creates the memory text UI.
     */
    private void createText() {
        text = new JTextField(10);
        text.setText(memory.get() + "");
        text.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();

                if (((c < '0') || (c > '9')) && (c != KeyEvent.VK_BACK_SPACE)) {
                    e.consume();    // ignore event
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                final String t = ((JTextField) e.getSource()).getText();

                if ((t != null) && (t.length() > 0)) {
                    memory.getAndSet(Integer.valueOf(t));
                }
            }
            @Override
            public void keyPressed(KeyEvent e) {}
        });
        text.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {}
        });
        textButton = new JRadioButton();
        textButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                containerXable(sliderSubComp, false);
                containerXable(textSubComp, true);
                text.setText(memory.get() + "");
                sliderButton.setSelected(false);
            }
        });
        textSubComp = GuiUtils.hbox(text, new JLabel(" megabytes"));
        textComp    = displaySlider
                      ? GuiUtils.hbox(textButton, textSubComp)
                      : textSubComp;
    }

    /**
     * Get the JComponent that contains the system preferences.
     *
     * @return the system preferences UI.
     */
    JComponent getJComponent() {
        final List<JComponent> formatComps = new ArrayList<JComponent>();
        formatComps.add(GuiUtils.topBottom(GuiUtils.rLabel("Memory:   "), new JPanel()));
        formatComps.add(GuiUtils.left(GuiUtils.topBottom(sliderComp, textComp)));
        return GuiUtils.inset(GuiUtils.topLeft(GuiUtils.doLayout(formatComps, 2, GuiUtils.WT_N, GuiUtils.WT_N)), 5);
    }

    /**
     * Gets the system manager. Memory preferences will be applied.
     *
     * @return the system manager
     */
    PreferenceManager getSystemManager() {
        return new PreferenceManager() {
            @Override
            public void applyPreference(final XmlObjectStore store, final Object data) {
                store.put(IdvConstants.PREF_MEMORY, ((AtomicLong) data).get());
            }
        };
    }

	/**
	 * Convert memory to percent.
	 *
	 * @param number
	 *            the number
	 * @return the int
	 */
	private static int convertToPercent(final long number) {
	    return Math.round(((float) number / TOTAL_SYSTEM_MEMORY_MB) * 100);
	}

	/**
	 * Convert memory to number.
	 *
	 * @param percent
	 *            the percent
	 * @return the long
	 */
	private static long convertToNumber(final int percent) {
	    return (long) ((percent / 100.0) * TOTAL_SYSTEM_MEMORY_MB);
	}
}
