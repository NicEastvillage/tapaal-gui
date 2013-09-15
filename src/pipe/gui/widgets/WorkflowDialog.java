package pipe.gui.widgets;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import pipe.gui.*;
import pipe.dataLayer.*;
import pipe.dataLayer.TAPNQuery.ExtrapolationOption;
import pipe.dataLayer.TAPNQuery.ModelType;
import pipe.dataLayer.TAPNQuery.SearchOption;
import pipe.dataLayer.TAPNQuery.TraceOption;
import dk.aau.cs.TCTL.TCTLEFNode;
import dk.aau.cs.TCTL.TCTLTrueNode;
import dk.aau.cs.model.tapn.TimeInterval;
import dk.aau.cs.model.tapn.TimedArcPetriNet;
import dk.aau.cs.model.tapn.TimedInputArc;
import dk.aau.cs.model.tapn.TimedOutputArc;
import dk.aau.cs.model.tapn.TimedPlace;
import dk.aau.cs.model.tapn.TimedTransition;
import dk.aau.cs.model.tapn.TransportArc;
import dk.aau.cs.translations.ReductionOption;
import dk.aau.cs.verification.QueryType;

public class WorkflowDialog extends JDialog{

	private static final long serialVersionUID = 5613743579411748200L;

	static WorkflowDialog dialog;

	private JPanel panel;

	private JCheckBox soundness;
	private JCheckBox strongSoundness;
	private JCheckBox min;
	private JCheckBox max;

	private TimedPlace in;
	private TimedPlace out;

	private enum TAWFNTypes{
		ETAWFN, MTAWFN, NOTTAWFN
	}

	private String msg = "";
	TAWFNTypes netType;

	public static void showDialog(){
		dialog = new WorkflowDialog(CreateGui.getApp(), "Workflow Analysis", true);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setResizable(true);
		dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		dialog.setVisible(true);
	}

	private WorkflowDialog(Frame frame, String title, boolean modal) {
		super(frame, title, modal);

		initComponents();
		setContentPane(panel);
	}

	private void initComponents() {
		panel = new JPanel(new GridBagLayout());

		/* Check if workflow net */
		netType = checkIfTAWFN();

		JPanel informationPanel = new JPanel();
		informationPanel.setBorder(BorderFactory.createTitledBorder("Properties"));
		informationPanel.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(5, 5, 5, 0);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(informationPanel, gbc);

		gbc.insets = new Insets(5, 5, 5, 5);

		informationPanel.add(new JLabel("TODO print properties"), gbc);

		JLabel workflowTypeLabel = new JLabel("The net is a TAWFN!");
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(workflowTypeLabel, gbc);

		switch(netType){
		case MTAWFN:
			workflowTypeLabel.setText("This net is a MTAWFN.");
			break;
		case ETAWFN:
			workflowTypeLabel.setText("This net is a ETAWFN.");
			break;
		case NOTTAWFN:
			workflowTypeLabel.setText("<html>This net is not a TAWFN for the following reason:<br>"+msg+"</html>");
			break;
		}

		if(netType != TAWFNTypes.NOTTAWFN){
			initValidationPanel();
		}
	}

	private void initValidationPanel(){

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;

		soundness = new JCheckBox("Check soundness.");
		soundness.setSelected(true);
		soundness.setEnabled(false);
		gbc.gridx = 0;
		gbc.gridy = 1;	
		panel.add(soundness, gbc);

		strongSoundness = new JCheckBox("Check strong soundness.");
		gbc.gridx = 0;
		gbc.gridy = 2;	
		panel.add(strongSoundness, gbc);

		min = new JCheckBox("Calculate minimum duration.");
		gbc.gridx = 0;
		gbc.gridy = 3;	
		panel.add(min, gbc);

		max = new JCheckBox("Calculate maximum duration.");
		gbc.gridx = 0;
		gbc.gridy = 4;	
		gbc.insets = new Insets(0, 0, 5, 0);
		panel.add(max, gbc);
		
		min.setEnabled(strongSoundness.isSelected());
		max.setEnabled(strongSoundness.isSelected());
		
		strongSoundness.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				min.setEnabled(strongSoundness.isSelected());
				max.setEnabled(strongSoundness.isSelected());
				if(!strongSoundness.isSelected()){
					min.setSelected(false);
					max.setSelected(false);
				}
			}
		});

		JButton checkIfSound = new JButton("Check if model is sound");
		checkIfSound.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				checkTAWFNSoundness(in, out);
			}
		});
		gbc.gridx = 1;
		gbc.gridy = 2;
		panel.add(checkIfSound, gbc);
	}

	private TAWFNTypes checkIfTAWFN(){
		List<TimedArcPetriNet> tapns = CreateGui.getCurrentTab().network().activeTemplates();
		ArrayList<TimedPlace> sharedInPlaces = new ArrayList<TimedPlace>();	
		ArrayList<TimedPlace> sharedOutPlaces = new ArrayList<TimedPlace>();
		in = null;
		out = null;
		msg = "";

		boolean isin;
		boolean isout;
		boolean isMonotonic = true;
		int numberOfTokensInNet = 0;
		ArrayList<TimedPlace> countedSharedPlaces = new ArrayList<TimedPlace>();

		for(TimedArcPetriNet tapn: tapns){ 		
			for(TimedPlace p : tapn.places()){
				isin = true;
				isout = true;

				p.invariant().asIterval();
				if(isMonotonic && !p.invariant().asIterval().equals(TimeInterval.ZERO_INF)){
					isMonotonic = false;
				}

				// Test for arcs going in to place
				for(TimedOutputArc arc: tapn.outputArcs()){
					if(arc.destination().equals(p)){
						isin = false;
						break;
					}
				}

				//Test for arcs going out from place
				for(TimedInputArc arc: tapn.inputArcs()){
					if(arc.source().equals(p)){
						isout = false;
						break;
					}
				}

				// Transport arcs
				for(TransportArc arc: tapn.transportArcs()){
					if(arc.destination().equals(p)){
						isin = false;
					}
					if(arc.source().equals(p)){
						isout = false;
					}
					if(!isin && !isout)	break;
				}



				if(p.isShared()){
					if(isin){
						sharedInPlaces.add(p);
					}

					if(isout){
						sharedOutPlaces.add(p);
					}
				}else if(isin && isout){
					msg += "Model contains place with no in- or out-going arcs.";
					return TAWFNTypes.NOTTAWFN;
				}else if(isin){
					if(in == null){
						in = p;
					}else{
						msg += "Multiple in-places found.";
						return TAWFNTypes.NOTTAWFN;
					}
				}else if(isout){
					if(out == null){
						out = p;
					}else{
						msg += "Multiple out-places found.";
						return TAWFNTypes.NOTTAWFN;
					}
				}

				if(p.isShared() && !countedSharedPlaces.contains(p)){
					numberOfTokensInNet += p.numberOfTokens();
					countedSharedPlaces.add(p);
				}else if(!p.isShared()){
					numberOfTokensInNet += p.numberOfTokens();
				}
			}

			for(TimedTransition t : tapn.transitions()){
				if(t.getInputArcs().isEmpty() && t.getTransportArcsGoingThrough().isEmpty()){
					msg += "Transition "+t.name()+" has empty preset.";
					return TAWFNTypes.NOTTAWFN;
				}

				if(isMonotonic && (t.isUrgent() || !t.getInhibitorArcs().isEmpty())){
					isMonotonic = false;
				}
			}
		}

		while(sharedInPlaces.size()!=0){
			TimedPlace p = sharedInPlaces.get(0);
			while(sharedInPlaces.remove(p)){}
			if(!sharedOutPlaces.remove(p)){
				if(in == null){
					in = p;
				}else{
					msg += "Multiple in-places found.";
					return TAWFNTypes.NOTTAWFN;
				}
			}
			while(sharedOutPlaces.remove(p)){}
		}

		if(in == null){
			msg += "No in-place found.";
			return TAWFNTypes.NOTTAWFN;
		}

		while(sharedOutPlaces.size() > 0){
			if(out == null){
				TimedPlace p = sharedOutPlaces.get(0);
				out = p;
				while(sharedOutPlaces.remove(p)){}
			}else{
				msg += "Multiple out-places found.";
				return TAWFNTypes.NOTTAWFN;
			}
		}

		if(out == null){
			msg += "No in-place found.";
			return TAWFNTypes.NOTTAWFN;
		}

		if(numberOfTokensInNet != 1 || in.tokens().size() != 1){
			msg += "The current marking is not a valid initial marking.";
			return TAWFNTypes.NOTTAWFN;
		}


		return isMonotonic ? TAWFNTypes.MTAWFN : TAWFNTypes.ETAWFN;
	}

	private void checkTAWFNSoundness(TimedPlace in, TimedPlace out){
		TAPNQuery q = new TAPNQuery("Workflow soundness checking", 10, new TCTLEFNode(new TCTLTrueNode()), TraceOption.NONE, SearchOption.HEURISTIC, ReductionOption.VerifyTAPNdiscreteVerification, true, false, false, null, ExtrapolationOption.AUTOMATIC, ModelType.TAWFN);
		Verifier.runVerifyTAPNVerification(CreateGui.getCurrentTab().network(), q);
	}
}