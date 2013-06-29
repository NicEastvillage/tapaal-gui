package pipe.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pipe.gui.widgets.EscapableDialog;

import dk.aau.cs.gui.components.EnabledTransitionsList;

import sun.security.jca.GetInstance.Instance;

public class SimulationControl extends JPanel {

	JSlider simulationSpeed;
	JCheckBox randomSimulation;
	Timer timer;
	
	private static SimulationControl instance;
	
	public static SimulationControl getInstance(){
		if(instance == null){
			instance = new SimulationControl();
		}
		return instance;
	}
	
	private SimulationControl() {
		super(new GridBagLayout());
		
		simulationSpeed = new JSlider();
		simulationSpeed.setSnapToTicks(false);
		simulationSpeed.setMajorTickSpacing(10);
		simulationSpeed.setPaintLabels(false);
		simulationSpeed.setPaintTicks(true);
		simulationSpeed.setPaintTrack(false);
		simulationSpeed.setPreferredSize(new Dimension(340, simulationSpeed.getPreferredSize().height));
		
		simulationSpeed.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(timer != null){
					setDelay(simulationSpeed.getValue()*20);
				}
			}
		});
		
		randomSimulation = new JCheckBox("Enable automatic random simulation");
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		add(randomSimulation, gbc);
		
		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.gridx = 0;
		gbc.gridy = 1;
		add(new JLabel("Set simulation speed:"), gbc);
		
		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.gridx = 0;
		gbc.gridy = 2;
		add(simulationSpeed, gbc);
		
		setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("Simulation controller"), 
				BorderFactory.createEmptyBorder(3, 3, 3, 3)));
		initTimer();
	}
	
	private void setDelay(int delay) {
		timer.setInitialDelay(delay);
		timer.setDelay(delay);
	}
	
	private void initTimer(){
		timer = new Timer(simulationSpeed.getValue()*20, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CreateGui.getTransitionFireingComponent().fireSelectedTransition();				
			}
		});
		timer.setRepeats(true);
	}

	public JSlider getSimulationSpeedSlider(){
		return simulationSpeed;
	}
	
	public boolean randomSimulation(){
		return randomSimulation.isSelected();
	}
	
	public void start(){
		timer.start();
	}
	
	public void stop(){
		timer.stop();
	}
	
	private static EscapableDialog dialog;
	
	public static void startSimulation(){
		JPanel contentPane = new JPanel(new GridBagLayout());
		
		JButton stopSimulationButton = new JButton("Stop");
		stopSimulationButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				dialog.setVisible(false);
			}
		});
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(0, 3, 0, 3);
		gbc.fill = GridBagConstraints.BOTH;
		contentPane.add(getInstance(), gbc);
		
		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(3, 3, 0, 3);
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		contentPane.add(stopSimulationButton, gbc);
		
		dialog = new EscapableDialog(CreateGui.getApp(), "Simulation controls", false);
		
		dialog.addComponentListener(new ComponentListener() {
			
			public void componentShown(ComponentEvent e) {}
			public void componentResized(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {
				getInstance().stop();
			}
		});
		
		dialog.setContentPane(contentPane);
		dialog.pack();
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(CreateGui.getApp());
		dialog.setVisible(true);
		getInstance().start();
	}
}
