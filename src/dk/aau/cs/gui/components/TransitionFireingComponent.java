package dk.aau.cs.gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import pipe.dataLayer.Template;
import pipe.gui.AnimationSettings;
import pipe.gui.BlueTransitionControl;
import pipe.gui.CreateGui;
import pipe.gui.SimulationControl;
import pipe.gui.graphicElements.Transition;

public class TransitionFireingComponent extends JPanel {
	private static final long serialVersionUID = -1208007964368671066L;

	private EnabledTransitionsList enabledTransitionsList;
	private JButton fireButton;
	private JButton settingsButton;
	
	public TransitionFireingComponent(boolean showBlueTransitions) {
		super(new GridBagLayout());
		
		enabledTransitionsList = new EnabledTransitionsList();
		
		this.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Enabled Transitions"),
				BorderFactory.createEmptyBorder(3, 3, 3, 3)));
		this
		.setToolTipText("List of currently enabled transitions (double click a transition to fire it)");
		enabledTransitionsList.setPreferredSize(new Dimension(
				enabledTransitionsList.getPreferredSize().width,
				enabledTransitionsList.getMinimumSize().height));
		
		settingsButton = new JButton("Settings");
		settingsButton.setPreferredSize(new Dimension(0, settingsButton.getPreferredSize().height)); //Make the two buttons equal in size
		settingsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AnimationSettings.showAnimationSettings();
			}
		});
		
		fireButton = new JButton("Delay & Fire");
		fireButton.setPreferredSize(new Dimension(0, fireButton.getPreferredSize().height)); //Make the two buttons equal in size
		fireButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(SimulationControl.getInstance().randomSimulation()){
					SimulationControl.startSimulation();
				} else {
					fireSelectedTransition();
				}
			}
		});
		fireButton.addKeyListener(new KeyAdapter() {			
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER){
					if(SimulationControl.getInstance().randomSimulation()){
						SimulationControl.startSimulation();
					} else {
						fireSelectedTransition();
					}
				}
			}
		});
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		this.add(enabledTransitionsList, gbc);
		
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		this.add(settingsButton, gbc);
		
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.SOUTHWEST;
		this.add(fireButton, gbc);
		
		showBlueTransitions(showBlueTransitions);
	}
	
	public static final String FIRE_BUTTON_DEACTIVATED_TOOL_TIP = "No transitions are enabled";
	public static final String FIRE_BUTTON_ENABLED_TOOL_TIP = "Press to fire the selected transition";
	public void updateFireButton(){
		if(enabledTransitionsList.getNumberOfTransitions() == 0){
			fireButton.setEnabled(false);
			fireButton.setToolTipText(FIRE_BUTTON_DEACTIVATED_TOOL_TIP);
		} else {
			fireButton.setEnabled(true);
			fireButton.setToolTipText(FIRE_BUTTON_ENABLED_TOOL_TIP);
		}
	}
	
	public void addTransition(Template template, Transition transition){
		enabledTransitionsList.addTransition(template, transition);
	}
	
	public void startReInit(){
		enabledTransitionsList.startReInit();
	}
	
	public void reInitDone(){
		updateFireButton();
		enabledTransitionsList.reInitDone();
	}
	
	public void fireSelectedTransition(){
		enabledTransitionsList.fireSelectedTransition();
		CreateGui.getApp().setRandomAnimationMode(false);
	}
	
	public BlueTransitionControl getBlueTransitionControl() {
		return BlueTransitionControl.getInstance();
	}

	public void showBlueTransitions(boolean enable) {
		settingsButton.setVisible(enable);
		fireButton.setText(enable ? "Delay & Fire" : "Fire");
	}
}
