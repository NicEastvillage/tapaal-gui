package pipe.gui.widgets;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pipe.gui.*;
import pipe.gui.GuiFrame.GUIMode;
import pipe.dataLayer.*;
import pipe.dataLayer.TAPNQuery.ExtrapolationOption;
import pipe.dataLayer.TAPNQuery.SearchOption;
import pipe.dataLayer.TAPNQuery.TraceOption;
import pipe.dataLayer.TAPNQuery.WorkflowMode;
import dk.aau.cs.TCTL.TCTLAtomicPropositionNode;
import dk.aau.cs.TCTL.TCTLEFNode;
import dk.aau.cs.TCTL.TCTLTrueNode;
import dk.aau.cs.model.tapn.Bound;
import dk.aau.cs.model.tapn.Constant;
import dk.aau.cs.model.tapn.ConstantBound;
import dk.aau.cs.model.tapn.IntBound;
import dk.aau.cs.model.tapn.LocalTimedPlace;
import dk.aau.cs.model.tapn.NetworkMarking;
import dk.aau.cs.model.tapn.SharedPlace;
import dk.aau.cs.model.tapn.SharedTransition;
import dk.aau.cs.model.tapn.TimeInterval;
import dk.aau.cs.model.tapn.TimeInvariant;
import dk.aau.cs.model.tapn.TimedArcPetriNet;
import dk.aau.cs.model.tapn.TimedArcPetriNetNetwork;
import dk.aau.cs.model.tapn.TimedInhibitorArc;
import dk.aau.cs.model.tapn.TimedInputArc;
import dk.aau.cs.model.tapn.TimedOutputArc;
import dk.aau.cs.model.tapn.TimedPlace;
import dk.aau.cs.model.tapn.TimedToken;
import dk.aau.cs.model.tapn.TimedTransition;
import dk.aau.cs.model.tapn.TransportArc;
import dk.aau.cs.model.tapn.simulation.TAPNNetworkTimeDelayStep;
import dk.aau.cs.model.tapn.simulation.TAPNNetworkTrace;
import dk.aau.cs.model.tapn.simulation.TAPNNetworkTraceStep;
import dk.aau.cs.model.tapn.simulation.TimedTAPNNetworkTrace;
import dk.aau.cs.translations.ReductionOption;
import dk.aau.cs.util.MemoryMonitor;
import dk.aau.cs.util.Tuple;
import dk.aau.cs.util.VerificationCallback;
import dk.aau.cs.verification.TraceConverter;
import dk.aau.cs.verification.VerificationResult;
import dk.aau.cs.verification.VerifyTAPN.TraceType;

public class WorkflowDialog extends JDialog {

	private String getHelpMessage(){ 
		// There is automatic word wrapping in the control that displays the text, so you don't need line breaks in paragraphs.
		StringBuffer buffer = new StringBuffer();
		buffer.append("<html>");
		buffer.append("<b>Headline</b><br/>");
		buffer.append("Some text. Bla bla.");
		buffer.append("<br/><br/>");
		buffer.append("<b>New Headlone</b><br/>");
		buffer.append("Any HTML tag can be used.");
		buffer.append("</html>");
		return buffer.toString(); 
	}
	
	private static final String TOOLTIP_SOUNDNESS = "Soundness";
	private static final String TOOLTIP_MIN = "Minimum duration";
	private static final String TOOLTIP_STRONGSOUNDNESS = "Strong soundness";
	private static final String TOOLTIP_MAX = "Maximum duration";
	
	private static final String DISCRETE_SEMANTICS_WARNING = "<html>Because the workflow contains age intervals and/or urgent transitions,<br /> this result is only valid with the discrete semantics.</html>";
	
	private static final String LABEL_TYPE_OF_WORKFLOW = "Type of workflow:";
	private static final String LABEL_INPUT_PLACE = "Input place of workflow:";
	private static final String LABEL_OUTPUT_PLACE = "Output place of workflow:";
	private static final String LABEL_INHIBITOR_ARCS = "Inhibitor arcs:";
	private static final String LABEL_URGENT_TRANSITIONS = "Urgent transitions:";
	private static final String LABEL_INVARIANTS = "Invariants:";

	private static final String LABEL_RESULT_SOUND = "Soundness:";
	private static final String LABEL_RESULT_MIN = "Minimum execution time:";
	private static final String LABEL_RESULT_STRONG_SOUND = "Strong soundness:";
	private static final String LABEL_RESULT_MAX = "Maximum execution time:";
	
	private static final String RESULT_STRING_SATISFIED = "Satisfied";
	private static final String RESULT_STRING_NOT_SATISFIED = "Not satisfied";
	private static final String RESULT_STRING_INCONCLUSIVE = "Inconclusive";
	private static final String RESULT_NOT_DEFINED = "Not defined";
	
	private static final String ERROR_INCREASE_BOUND = "Try to increase the number of extra tokens.";

	/* Soundness */
	
	private static final String RESULT_ERROR_NONFINAL_REACHED = "Non-final marking with token in output place reached.";
	private static final String RESULT_ERROR_NO_TRACE_TO_FINAL = "Marking reached with no trace to a final marking.";
	
	/* Strong Soundness */
	
	private static final String RESULT_ERROR_CYCLE = "Infinite trace found.";
	private static final String RESULT_ERROR_TIME = "Time divergent marking found.";
	/* Syntax */
	
	private static final String ERROR_MULTIPLE_IN = "Multiple input places found";
	private static final String ERROR_MULTIPLE_OUT = "Multiple output places found";

	private static final long serialVersionUID = 5613743579411748200L;

	static WorkflowDialog dialog;

	private JPanel panel;

	private static JCheckBox soundness = null;
	private static JCheckBox strongSoundness = null;
	private static JCheckBox min = null;
	private static JCheckBox max = null;

	private static JLabel soundnessResult;
	private static JLabel strongSoundnessResult;
	private static JLabel soundnessResultExplanation;
	private static JLabel strongSoundnessResultExplanation;
	private static JButton soundnessResultTraceButton;
	private static TAPNNetworkTrace soundnessResultTrace = null;
	private static JLabel minResult;
	private static JButton minResultTraceButton;
	private static TAPNNetworkTrace minResultTrace = null;
	private static JLabel maxResult;
	private static TAPNNetworkTrace maxResultTrace = null;
	private static JButton maxResultTraceButton;
	private static TAPNNetworkTrace strongSoundnessResultTrace = null;
	private static JButton strongSoundnessResultTraceButton;
	private static JLabel soundnessVerificationStats;
	private static JLabel strongSoundnessVerificationStats;

	private static JLabel soundnessResultLabel;
	private static JLabel minResultLabel;
	private static JLabel strongSoundnessResultLabel;
	private static JLabel maxResultLabel;

	private static JPanel resultPanel = null;

	private static CustomJSpinner numberOfExtraTokensInNet = null;

	private ArrayList<String> errorMsgs = new ArrayList<String>();
	private int errors = 0;
	private LinkedList<Runnable> verificationQueue = new LinkedList<Runnable>();
	private static ArrayList<SharedPlace> unusedSharedPlaces = new ArrayList<SharedPlace>();

	private TimedPlace in;
	private TimedPlace out;
	private TimedArcPetriNet out_template;

	private static boolean isSound = false;
	private static boolean isConclusive = true;
	private static long m;
	private static int B;
	private static Constant c = null;
	private static TimedPlace done = null;

	private static TimedArcPetriNetNetwork model = null;

	private static boolean restoreDialog = false;

	public static boolean restoreDialog(){
		return restoreDialog;
	}

	private enum TAWFNTypes {
		ETAWFN, MTAWFN, NOTTAWFN
	}

	TAWFNTypes netType;

	public static void showDialog(boolean clearResults) {

		/* Copy model */

		model = CreateGui.getCurrentTab().network().copy();
		// Fix - remove unused shared places
		unusedSharedPlaces.clear();
		for(SharedPlace p : model.sharedPlaces()){
			if(p.getComponentsUsingThisPlace().isEmpty()){
				unusedSharedPlaces.add(p);
			}
		}
		for(SharedPlace p : unusedSharedPlaces){
			model.remove(p);
		}

		/* Make dialog */

		dialog = new WorkflowDialog(clearResults, CreateGui.getApp(), "Workflow Analysis", false);
		dialog.setModal(false);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setResizable(true);
		dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		dialog.setVisible(true);
		restoreDialog = false;
	}

	private void switchToTrace(TAPNNetworkTrace trace){
		restoreDialog = true;
		dialog.setVisible(false);
		CreateGui.getApp().setGUIMode(GUIMode.animation);
		CreateGui.getAnimator().SetTrace(trace);
	}

	private WorkflowDialog(boolean clearResults, Frame frame, String title, boolean modal) {
		super(frame, title, modal);

		initComponents();
		if(clearResults && resultPanel != null)	resultPanel.setVisible(false);
		setContentPane(panel);
	}

	private void initComponents() {
		panel = new JPanel(new GridBagLayout());

		/* Check if workflow net */
		netType = checkIfTAWFN();

		JPanel informationPanel = new JPanel();
		informationPanel.setBorder(BorderFactory
				.createTitledBorder("About Workflow"));
		informationPanel.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = 3;
		gbc.weightx = 1;
		panel.add(informationPanel, gbc);

		gbc.fill = GridBagConstraints.NONE;
		gbc.gridwidth = 1;
		gbc.anchor = (gbc.gridx == 0 && gbc.gridwidth == 1) ? GridBagConstraints.EAST : GridBagConstraints.WEST;
		JLabel workflowType = new JLabel(LABEL_TYPE_OF_WORKFLOW);
		informationPanel.add(workflowType, gbc);

		gbc.gridx = 1;

		JLabel workflowTypeLabel = new JLabel("");
		informationPanel.add(workflowTypeLabel, gbc);

		JLabel workflowTypeError = new JLabel("");
		workflowTypeError.setVisible(false);
		gbc.gridy = 1;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		informationPanel.add(workflowTypeError, gbc);
		gbc.gridwidth = 1;

		switch (netType) {
		case MTAWFN:
			workflowTypeLabel.setText("Monotonic workflow net");
			break;
		case ETAWFN:
			workflowTypeLabel.setText("Extended workflow net");
			break;
		case NOTTAWFN:
			workflowType.setVisible(false);
			workflowTypeLabel.setVisible(false);
			StringBuilder sb = new StringBuilder();
			String sep = "<br>";
			for (String e : errorMsgs)
				sb.append(sep).append("- ").append(e);
			workflowTypeError
			.setText("<html>This net is not a workflow net for the following reason(s):"
					+ sb.toString() + "</html>");
			workflowTypeError.setVisible(true);
			break;
		}

		if (netType != TAWFNTypes.NOTTAWFN) {
			gbc.gridy = 1;
			gbc.gridx = 0;
			informationPanel.add(new JLabel(LABEL_INPUT_PLACE), gbc);

			JLabel inPlaceLabel = new JLabel(in.name());
			gbc.gridx = 1;
			informationPanel.add(inPlaceLabel, gbc);

			gbc.gridy = 2;
			gbc.gridx = 0;
			informationPanel.add(new JLabel(LABEL_OUTPUT_PLACE), gbc);
			JLabel outPlaceLabel = new JLabel(out.name());
			gbc.gridx = 1;
			informationPanel.add(outPlaceLabel, gbc);

			gbc.gridy = 0;
			gbc.gridx = 3;
			informationPanel.add(new JLabel(LABEL_INHIBITOR_ARCS, SwingConstants.RIGHT), gbc);
			gbc.gridx = 4;
			informationPanel.add(new JLabel(model.hasInhibitorArcs()? "Yes":"No"), gbc);

			gbc.gridy = 1;
			gbc.gridx = 3;
			informationPanel.add(new JLabel(LABEL_URGENT_TRANSITIONS, SwingConstants.RIGHT), gbc);
			gbc.gridx = 4;
			informationPanel.add(new JLabel(model.hasUrgentTransitions()? "Yes":"No"), gbc);

			gbc.gridy = 2;
			gbc.gridx = 3;
			informationPanel.add(new JLabel(LABEL_INVARIANTS, SwingConstants.RIGHT), gbc);
			gbc.gridx = 4;
			informationPanel.add(new JLabel(model.hasInvariants()? "Yes":"No"), gbc);


			initValidationPanel();
		}
		
		gbc.gridx = 0;
		gbc.gridy = 7;
		gbc.anchor = GridBagConstraints.WEST;
		JButton help_button = new JButton("Help");
		help_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(CreateGui.appGui, getMessageComponent(), "Help", JOptionPane.INFORMATION_MESSAGE);
			}
		});

		panel.add(help_button, gbc);

		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.EAST;
		JButton close_button = new JButton("Close");
		close_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});

		panel.add(close_button, gbc);
	}
	
	private Object getMessageComponent(){
		JTextPane pane = new JTextPane();
		pane.setContentType("text/html");
		pane.setText(getHelpMessage());
		pane.setEditable(false);
		pane.setCaretPosition(0);
		for(MouseListener listener : pane.getMouseListeners()){
			pane.removeMouseListener(listener);
		}
		Dimension dim = new Dimension(500,400);
		pane.setPreferredSize(dim);  
		pane.setMargin(new Insets(5,5,5,5));  
		JScrollPane scrollPane = new JScrollPane(pane);  
		scrollPane.setPreferredSize(dim);  
		return scrollPane;  
	}

	private void initValidationPanel() {

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.insets = new Insets(5, 5, 5, 5);

		JPanel soundnessPanel = new JPanel();
		soundnessPanel.setBorder(BorderFactory.createTitledBorder("Check Properties"));
		soundnessPanel.setLayout(new GridBagLayout());
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		panel.add(soundnessPanel, gbc);

		gbc.gridwidth = 1;

		if (soundness == null)
			soundness = new JCheckBox("Check soundness.");
		soundness.setToolTipText(TOOLTIP_SOUNDNESS);
		soundness.setSelected(true);
		soundness.setEnabled(false);
		gbc.gridx = 0;
		gbc.gridy = 1;
		soundnessPanel.add(soundness, gbc);

		if (min == null){
			min = new JCheckBox("Calculate minimum duration.");
			min.setSelected(true);
			min.setToolTipText(TOOLTIP_MIN);
		}
		gbc.gridx = 1;
		gbc.gridy = 1;
		soundnessPanel.add(min, gbc);

		if (strongSoundness == null)
			strongSoundness = new JCheckBox("Check strong soundness.");
		strongSoundness.setToolTipText(TOOLTIP_STRONGSOUNDNESS);
		gbc.gridx = 0;
		gbc.gridy = 2;
		soundnessPanel.add(strongSoundness, gbc);

		if (max == null)
			max = new JCheckBox("Calculate maximum duration.");
		max.setToolTipText(TOOLTIP_MAX);
		gbc.gridx = 1;
		gbc.gridy = 2;
		soundnessPanel.add(max, gbc);

		strongSoundness.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				max.setEnabled(strongSoundness.isSelected());
				if (!strongSoundness.isSelected()) {
					max.setSelected(false);
				}else{
					max.setSelected(true);
				}
			}
		});

		// Initialize correct state
		max.setEnabled(strongSoundness.isSelected());
		if (!strongSoundness.isSelected()) {
			max.setSelected(false);
		}

		/* Result panel */

		if(resultPanel == null){
			resultPanel = new JPanel();
			resultPanel.setBorder(BorderFactory
					.createTitledBorder("Results"));
			resultPanel.setLayout(new GridBagLayout());
			resultPanel.setVisible(false);
		}
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		panel.add(resultPanel, gbc);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(5, 5, 0, 5);
		
		/* Initialize results panel on first invokation */
		if(soundnessResultLabel == null){
			soundnessResultLabel = new JLabel(LABEL_RESULT_SOUND);
			soundnessResultLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			resultPanel.add(soundnessResultLabel, gbc);
			soundnessResultLabel.setVisible(false);

			soundnessResult = new JLabel();
			gbc.gridx = 1;
			resultPanel.add(soundnessResult, gbc);
			soundnessResult.setVisible(false);

			gbc.gridx = 2;
			soundnessVerificationStats = new JLabel();
			soundnessVerificationStats.setVisible(false);
			resultPanel.add(soundnessVerificationStats, gbc);

			soundnessResultExplanation = new JLabel();
			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.gridwidth = 2;
			gbc.insets = new Insets(0, 5, 0, 5);
			soundnessResultExplanation.setVisible(false);
			soundnessResultExplanation.setEnabled(false);
			resultPanel.add(soundnessResultExplanation, gbc);
			
			gbc.gridwidth = 1;
			
			soundnessResultTraceButton = new JButton("Show trace");
			gbc.gridx = 2;
			soundnessResultTraceButton.setVisible(false);
			soundnessResultTraceButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					switchToTrace(soundnessResultTrace);
				}
			});
			resultPanel.add(soundnessResultTraceButton, gbc);

			// Min 
			gbc.gridy = 2;
			gbc.gridx = 0;
			gbc.insets = new Insets(10, 5, 5, 5);
			minResultLabel = new JLabel(LABEL_RESULT_MIN);
			minResultLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			resultPanel.add(minResultLabel, gbc);
			minResultLabel.setVisible(false);

			minResult = new JLabel();
			gbc.gridx = 1;
			resultPanel.add(minResult, gbc);
			minResult.setVisible(false);

			minResultTraceButton = new JButton("Show trace");
			gbc.gridx = 2;
			minResultTraceButton.setVisible(false);
			minResultTraceButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					switchToTrace(minResultTrace);
				}
			});
			resultPanel.add(minResultTraceButton, gbc);

			// Strong soundness

			gbc.gridwidth = 1;
			gbc.gridy = 3;
			gbc.gridx = 0;
			gbc.insets = new Insets(5, 5, 0, 5);
			strongSoundnessResultLabel = new JLabel(LABEL_RESULT_STRONG_SOUND);
			strongSoundnessResultLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			resultPanel.add(strongSoundnessResultLabel, gbc);
			strongSoundnessResultLabel.setVisible(false);

			strongSoundnessResult = new JLabel();
			gbc.gridx = 1;
			resultPanel.add(strongSoundnessResult, gbc);
			strongSoundnessResult.setVisible(false);

			strongSoundnessVerificationStats = new JLabel();
			gbc.gridx = 2;
			strongSoundnessVerificationStats.setVisible(false);
			resultPanel.add(strongSoundnessVerificationStats, gbc);
			
			strongSoundnessResultExplanation = new JLabel();
			gbc.gridx = 0;
			gbc.gridy = 4;
			gbc.gridwidth = 2;
			gbc.insets = new Insets(0, 5, 0, 5);
			strongSoundnessResultExplanation.setVisible(false);
			strongSoundnessResultExplanation.setEnabled(false);
			resultPanel.add(strongSoundnessResultExplanation, gbc);
			
			gbc.gridwidth = 1;
			
			strongSoundnessResultTraceButton = new JButton("Show trace");
			gbc.gridx = 2;
			strongSoundnessResultTraceButton.setVisible(false);
			strongSoundnessResultTraceButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					switchToTrace(strongSoundnessResultTrace);
				}
			});
			resultPanel.add(strongSoundnessResultTraceButton, gbc);

			// Max 

			gbc.gridy = 5;
			gbc.gridx = 0;
			gbc.insets = new Insets(10, 5, 5, 5);
			maxResultLabel = new JLabel(LABEL_RESULT_MAX);
			maxResultLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			resultPanel.add(maxResultLabel, gbc);
			maxResultLabel.setVisible(false);

			maxResult = new JLabel();
			gbc.gridx = 1;
			resultPanel.add(maxResult, gbc);
			maxResult.setVisible(false);

			maxResultTraceButton = new JButton("Show trace");
			gbc.gridx = 2;
			maxResultTraceButton.setVisible(false);
			maxResultTraceButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					switchToTrace(maxResultTrace);
				}
			});
			resultPanel.add(maxResultTraceButton, gbc);
		}

		gbc.insets = new Insets(5, 5, 5, 5);

		/* K-bound panel */

		gbc.gridx = 0;
		gbc.gridy = 6;
		panel.add(new JLabel(" Number of extra tokens:  "), gbc);

		if (numberOfExtraTokensInNet == null)
			numberOfExtraTokensInNet = new CustomJSpinner(model.getDefaultBound(), 0,
					100000);	// Allow at most 100.000 extra tokens.
		else
			numberOfExtraTokensInNet.setValue(model.getDefaultBound());
		numberOfExtraTokensInNet.setMaximumSize(new Dimension(55, 30));
		numberOfExtraTokensInNet.setMinimumSize(new Dimension(55, 30));
		numberOfExtraTokensInNet.setPreferredSize(new Dimension(55, 30));
		gbc.gridx = 1;
		gbc.gridy = 6;
		panel.add(numberOfExtraTokensInNet, gbc);

		numberOfExtraTokensInNet.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				model.setDefaultBound((Integer) numberOfExtraTokensInNet.getValue());
				CreateGui.getCurrentTab().network().setDefaultBound((Integer) numberOfExtraTokensInNet.getValue());
				CreateGui.getDrawingSurface().setNetChanged(true);
			}
		});

		gbc.gridwidth = 1;
		
		JButton checkBound = new JButton("Check boundedness");
		gbc.gridx = 2;
		panel.add(checkBound, gbc);

		checkBound.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				checkBound();
			}
		});

		JButton checkIfSound = new JButton("Verify workflow");
		getRootPane().setDefaultButton(checkIfSound);
		checkIfSound.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				checkTAWFNSoundness();
			}
		});
		gbc.gridx = 2;
		gbc.gridy = 7;
		gbc.anchor = GridBagConstraints.EAST;
		panel.add(checkIfSound, gbc);
	}

	private TAWFNTypes checkIfTAWFN() {
		List<TimedArcPetriNet> tapns = model.activeTemplates();
		ArrayList<TimedPlace> sharedInPlaces = new ArrayList<TimedPlace>();
		ArrayList<TimedPlace> sharedOutPlaces = new ArrayList<TimedPlace>();
		ArrayList<TimedPlace> sharedAcceptedPlaces = new ArrayList<TimedPlace>();
		ArrayList<SharedTransition> sharedTransitions = new ArrayList<SharedTransition>();
		in = null;
		out = null;
		errorMsgs = new ArrayList<String>();
		errors = 0;

		boolean isin;
		boolean isout;
		boolean isMonotonic = true;
		int numberOfTokensInNet = 0;
		ArrayList<TimedPlace> countedSharedPlaces = new ArrayList<TimedPlace>();

		for (TimedArcPetriNet tapn : tapns) {
			for (TimedPlace p : tapn.places()) {
				isin = true;
				isout = true;

				p.invariant().asIterval();
				if (isMonotonic
						&& !p.invariant().asIterval()
						.equals(TimeInterval.ZERO_INF)) {
					isMonotonic = false;
				}

				// Test for arcs going in to place
				for (TimedOutputArc arc : tapn.outputArcs()) {
					if (arc.destination().equals(p)) {
						isin = false;
						break;
					}
				}

				// Test for arcs going out from place
				for (TimedInputArc arc : tapn.inputArcs()) {
					if (arc.source().equals(p)) {
						isout = false;
						break;
					}
				}

				// Transport arcs
				for (TransportArc arc : tapn.transportArcs()) {
					if (arc.destination().equals(p)) {
						isin = false;
					}
					if (arc.source().equals(p)) {
						isout = false;
					}
					if (!isin && !isout)
						break;
				}

				if (p.isShared()) {
					if (isin) {
						sharedInPlaces.add(p);
					}

					if (isout) {
						sharedOutPlaces.add(p);
					}

					if(!isin && !isout){
						sharedAcceptedPlaces.add(p);
					}

				} else if (isin && isout) {
					if(errorMsgs.size() > 5)	errors++;
					else
						errorMsgs.add("Place " + p
								+ " has no in- or out-going arcs.");
				} else if (isin) {
					if (in == null) {
						in = p;
					} else {
						if(errorMsgs.size() > 5)	errors++;
						else
							errorMsgs.add(ERROR_MULTIPLE_IN+" (" + in
									+ " and " + p + ").");
					}
				} else if (isout) {
					if (out == null) {
						out = p;
					} else {
						if(errorMsgs.size() > 5)	errors++;
						else
							errorMsgs.add(ERROR_MULTIPLE_OUT + " (" + out
									+ " and " + p + ").");
					}
				}

				if (p.isShared() && !countedSharedPlaces.contains(p)) {
					numberOfTokensInNet += p.numberOfTokens();
					countedSharedPlaces.add(p);
				} else if (!p.isShared()) {
					numberOfTokensInNet += p.numberOfTokens();
				}
			}

			for (TimedTransition t : tapn.transitions()) {
				if (t.isShared()) {
					sharedTransitions.add(t.sharedTransition());
				} else if (t.getInputArcs().isEmpty()
						&& t.getTransportArcsGoingThrough().isEmpty()) {
					if(errorMsgs.size() > 5)	errors++;
					else
						errorMsgs.add("Transition " + t.name()
								+ " has empty preset.");
				}

				if (isMonotonic
						&& (t.isUrgent() || !t.getInhibitorArcs().isEmpty())) {
					isMonotonic = false;
				}
			}
		}

		outer: while (sharedTransitions.size() > 0) {
			SharedTransition st = sharedTransitions.get(0);
			for (TimedTransition t : st.transitions()) {
				if (!t.getTransportArcsGoingThrough().isEmpty()
						|| !t.getInputArcs().isEmpty()) {
					while (sharedTransitions.remove(st)) {
					}
					continue outer;
				}
			}
			if(errorMsgs.size() > 5)	errors++;
			else
				errorMsgs.add("Transition " + st.name() + " has empty preset.");
			while (sharedTransitions.remove(st)) {
			}
		}

		for(TimedPlace p : sharedAcceptedPlaces){
			while (sharedInPlaces.remove(p)) {
			}
			while (sharedOutPlaces.remove(p)) {
			}
		}

		while (sharedInPlaces.size() != 0) {
			TimedPlace p = sharedInPlaces.get(0);
			while (sharedInPlaces.remove(p)) {
			}
			if (!sharedOutPlaces.remove(p)) {
				if (in == null) {
					in = p;
				} else {
					if(errorMsgs.size() > 5)	errors++;
					else
						errorMsgs.add(ERROR_MULTIPLE_IN+" (" + in + " and "
								+ p + ").");
				}
			}
			while (sharedOutPlaces.remove(p)) {
			}
		}

		if (in == null) {
			if(errorMsgs.size() > 5)	errors++;
			else
				errorMsgs.add("No input place found.");
		}

		while (sharedOutPlaces.size() > 0) {
			TimedPlace p = sharedOutPlaces.get(0);
			if (out == null) {
				out = p;
			} else {
				if(errorMsgs.size() > 5)	errors++;
				else
					errorMsgs.add(ERROR_MULTIPLE_OUT + " (" + out + " and " + p
							+ ").");
			}
			while (sharedOutPlaces.remove(p)) {
			}
		}

		if (out == null) {
			if(errorMsgs.size() > 5)	errors++;
			else
				errorMsgs.add("No output place found.");
		}

		if (numberOfTokensInNet > 1 || in.tokens().size() != 1) {
			if(errorMsgs.size() > 5)	errors++;
			else
				errorMsgs
				.add("The current marking is not a valid initial marking.");
		}

		if (!errorMsgs.isEmpty()) {
			if(errors > 0)	errorMsgs.add("and "+errors+" other problems.");
			return TAWFNTypes.NOTTAWFN;
		}
		
		int i = 0;
		outer: for(TimedArcPetriNet t : model.activeTemplates()){
			for(TimedPlace p : t.places()){
				if(p.equals(out)){
					break outer;
				}
			}
			i++;
		}

		out_template = model.activeTemplates().get(i);

		return isMonotonic ? TAWFNTypes.MTAWFN : TAWFNTypes.ETAWFN;
	}

	private void checkTAWFNSoundness() {
		// Clear old results
		soundnessResult.setText("");
		soundnessResult.setVisible(false);
		soundnessResultLabel.setVisible(false);
		soundnessResultTraceButton.setVisible(false);
		minResult.setText("");
		minResult.setVisible(false);
		minResultLabel.setVisible(false);
		minResultTraceButton.setVisible(false);
		strongSoundnessResult.setText("");
		strongSoundnessResult.setVisible(false);
		strongSoundnessResultLabel.setVisible(false);
		strongSoundnessResultTraceButton.setVisible(false);
		maxResult.setText("");
		maxResult.setVisible(false);
		maxResultLabel.setVisible(false);
		maxResultTraceButton.setVisible(false);
		soundnessResultExplanation.setVisible(false);
		soundnessVerificationStats.setVisible(false);
		strongSoundnessResultExplanation.setVisible(false);
		strongSoundnessVerificationStats.setVisible(false);
		resultPanel.setVisible(true);

		dialog.pack();

		verificationQueue.clear();
		isSound = false;
		isConclusive = true;

		verificationQueue.add(getSoundnessRunnable());

		if (strongSoundness.isSelected()) {
			verificationQueue.add(getStrongSoundnessRunnable());
		}

		// Run steps
		while (!verificationQueue.isEmpty()) {
			verificationQueue.get(0).run();
			verificationQueue.remove(0);
		}
	}

	private TimedArcPetriNetNetwork composeStrongSoundnessModel() {
		TimedArcPetriNetNetwork network = model.copy();
		TimedArcPetriNet out_template = network.getTAPNByName(this.out_template.name());
		TimedPlace out_hook = null;

		for(TimedPlace p : out_template.places()){
			if(p.name().equals(out.name())){
				out_hook = p;
				break;
			}
		}

		// Add new components
		String name = "C";
		while(network.getConstant(name) != null){
			name += "x";
		}
		c = new Constant(name, (int) m*B+1); 
		network.constants().add(c);

		/* Create transitions */

		name = "nok";
		while(out_template.getTransitionByName(name) != null){
			name += "x";
		}
		TimedTransition nok_t = new TimedTransition(name, true);
		nok_t.setUrgent(true);
		out_template.add(nok_t);

		name = "tick";
		while(out_template.getTransitionByName(name) != null){
			name += "x";
		}
		TimedTransition tick_t = new TimedTransition(name, false);
		out_template.add(tick_t);

		name = "ok";
		while(out_template.getTransitionByName(name) != null){
			name += "x";
		}
		TimedTransition ok_t = new TimedTransition(name, true);
		ok_t.setUrgent(true);
		out_template.add(ok_t);

		/* Create places */

		name = "timer";
		while(out_template.getPlaceByName(name) != null){
			name += "x";
		}
		TimedPlace timer_p = new LocalTimedPlace(name, new TimeInvariant(true, new ConstantBound(c)));
		out_template.add(timer_p);

		name = "ready";
		while(out_template.getPlaceByName(name) != null){
			name += "x";
		}
		TimedPlace ready_p = new LocalTimedPlace(name);
		out_template.add(ready_p);

		name = "finished";
		while(out_template.getPlaceByName(name) != null){
			name += "x";
		}
		TimedPlace finished_p = new LocalTimedPlace(name, new TimeInvariant(true, new IntBound(0)));
		out_template.add(finished_p);

		name = "done";
		while(out_template.getPlaceByName(name) != null){
			name += "x";
		}

		done = new LocalTimedPlace(name, new TimeInvariant(true, new IntBound(0)));
		out_template.add(done);

		/* Create arcs */

		out_template.add(new TimedInputArc(out_hook, nok_t, TimeInterval.ZERO_INF));
		out_template.add(new TimedInputArc(out_hook, ok_t, TimeInterval.ZERO_INF));
		out_template.add(new TimedInputArc(timer_p, tick_t, new TimeInterval(true, new ConstantBound(c), new ConstantBound(c), true)));
		out_template.add(new TimedOutputArc(tick_t, ready_p));
		out_template.add(new TimedInputArc(ready_p, ok_t, TimeInterval.ZERO_INF));
		out_template.add(new TimedOutputArc(ok_t, done));
		out_template.add(new TimedOutputArc(nok_t, finished_p));
		out_template.add(new TimedInhibitorArc(ready_p, nok_t, TimeInterval.ZERO_INF));

		out_template.addToken(new TimedToken(timer_p));

		return network;
	}

	private Runnable getStrongSoundnessRunnable() {
		return new Runnable() {

			@Override
			public void run() {

				// Check preliminary conditions
				if(!isSound){
					setStrongSoundnessResult(false,isConclusive?"Workflow is not sound.":"Workflow soundness check was inconclusive.", isConclusive);
					return;
				}

				// Compute B
				B = 0;
				for (TimedArcPetriNet t : model
						.activeTemplates()) {
					for (TimedPlace p : t.places()) {
						if (p.invariant().upperBound().equals(Bound.Infinity)) {
							continue;
						}
						B = Math.max(B, p.invariant().upperBound().value());
					}
				}

				final TimedArcPetriNetNetwork model = composeStrongSoundnessModel();		

				/* Call engine */

				String template = done.isShared()? ((SharedPlace) done).getComponentsUsingThisPlace().get(0):((LocalTimedPlace) done).model().name();
				final TAPNQuery q = new TAPNQuery(
						"Workflow strong soundness checking",
						numberOfExtraTokensInNet == null ? 0
								: (Integer) numberOfExtraTokensInNet.getValue(),
								new TCTLEFNode(new TCTLAtomicPropositionNode(template, done.name(), "=", 1)), TraceOption.SOME,
								SearchOption.HEURISTIC,
								ReductionOption.VerifyTAPNdiscreteVerification, true,
								false, false, null, ExtrapolationOption.AUTOMATIC, WorkflowMode.WORKFLOW_STRONG_SOUNDNESS);
				Verifier.runVerifyTAPNVerification(model, q, new VerificationCallback() {

					@Override
					public void run() {
					}

					@Override
					public void run(VerificationResult<TAPNNetworkTrace> result) {
						if(result.isQuerySatisfied()){

							strongSoundnessResultTrace = determineError(mapTraceToRealModel(result.getTrace()));
							strongSoundnessResultTraceButton.setVisible(true);

							if(max.isSelected()){
								maxResult.setText(RESULT_NOT_DEFINED);
								maxResult.setForeground(Pipe.QUERY_NOT_SATISFIED_COLOR);
								maxResult.setVisible(true);
							}
						}else{
							setStrongSoundnessResult(true, null);
							if(max.isSelected()){
								setMaxResult(model, result.stats().maximumExecutionTime());
								maxResultTrace = mapTraceToRealModel(result.getTrace());
								maxResultTraceButton.setVisible(true);
							}
						}
						
						
						strongSoundnessVerificationStats.setText(result
								.getVerificationTimeString().replace("Estimated verification time", "Est. time")
								+ ", memory: "
								+ MemoryMonitor.getPeakMemory());
						strongSoundnessVerificationStats.setVisible(true);
						
						dialog.pack();
					}

					private TimedTAPNNetworkTrace determineError(TAPNNetworkTrace trace){
						TimedTAPNNetworkTrace tmpTrace = new TimedTAPNNetworkTrace(trace.length());
						TraceType type = null;
						ArrayList<Tuple<TAPNNetworkTraceStep, Tuple<Integer, Integer>>> detectLoops = new ArrayList<Tuple<TAPNNetworkTraceStep,Tuple<Integer,Integer>>>();
						int loopIndex = -1;
						int delay = 0;
						int maxDelay = 0;
						int divergentIndex = -1;
						// Get trace until bound violated						
						outer: for(TAPNNetworkTraceStep step : trace){
							if(step instanceof TAPNNetworkTimeDelayStep){
								delay += ((TAPNNetworkTimeDelayStep) step).getDelay().intValue();
								if(((TAPNNetworkTimeDelayStep) step).getDelay().intValue() > maxDelay){
									maxDelay = ((TAPNNetworkTimeDelayStep) step).getDelay().intValue();
									divergentIndex = tmpTrace.length();
								}
							}else{
								for(Tuple<TAPNNetworkTraceStep, Tuple<Integer, Integer>> checkStep : detectLoops){
									if(checkStep.value1().equals(step) && checkStep.value2().value1() < delay){
										loopIndex = checkStep.value2().value2();
										setStrongSoundnessResult(false, RESULT_ERROR_CYCLE);
										type = TraceType.EG_LOOP;
										break outer;
									}
								}
								detectLoops.add(new Tuple<TAPNNetworkTraceStep, Tuple<Integer,Integer>>(step, new Tuple<Integer, Integer>(delay, tmpTrace.length())));
							}
							tmpTrace.add(step);
						}

						if(type == null){
							type = TraceType.EG_DELAY_FOREVER;
							setStrongSoundnessResult(false, RESULT_ERROR_TIME);
							loopIndex = divergentIndex;
						}

						TimedTAPNNetworkTrace realTrace = new TimedTAPNNetworkTrace(loopIndex);
						for(TAPNNetworkTraceStep step : tmpTrace){
							if(type == TraceType.EG_DELAY_FOREVER && realTrace.length() == loopIndex){
								break;
							}
							realTrace.add(step);
						}

						realTrace.setTraceType(type);

						return realTrace;
					}
				});
			}
		};
	}

	private void setMaxResult(TimedArcPetriNetNetwork model, int value){
		maxResult.setText(value + " time units.");
		maxResult.setForeground(Pipe.QUERY_SATISFIED_COLOR);
		maxResult.setVisible(true);
		maxResultLabel.setVisible(true);
		dialog.pack();
	}
	
	private void setStrongSoundnessResult(boolean satisfied, String explanation) {
		setStrongSoundnessResult(satisfied, explanation, true);
	}

	private void setStrongSoundnessResult(boolean satisfied, String explanation, boolean conclusive) {
		if (satisfied) {
			strongSoundnessResult.setText(RESULT_STRING_SATISFIED);
			strongSoundnessResult.setForeground(Pipe.QUERY_SATISFIED_COLOR);
		} else {
			if(conclusive){
				strongSoundnessResult.setText(RESULT_STRING_NOT_SATISFIED);
				strongSoundnessResult.setForeground(Pipe.QUERY_NOT_SATISFIED_COLOR);
			}else{
				strongSoundnessResult.setText(RESULT_STRING_INCONCLUSIVE);
				strongSoundnessResult.setForeground(Pipe.QUERY_INCONCLUSIVE_COLOR);
			}
			if(max.isSelected()){
				maxResult.setText(RESULT_NOT_DEFINED);
				maxResult.setForeground(Pipe.QUERY_NOT_SATISFIED_COLOR);
				maxResultLabel.setVisible(true);
				maxResult.setVisible(true);
			}
		}

		if (explanation != null) {
			strongSoundnessResultExplanation.setText(explanation);
			strongSoundnessResultExplanation.setVisible(true);
		}
		strongSoundnessResultLabel.setVisible(true);
		strongSoundnessResult.setVisible(true);
		
		dialog.pack();
	}

	private Runnable getSoundnessRunnable() {
		Runnable r = new Runnable() {

			@Override
			public void run() {
				final TAPNQuery q = new TAPNQuery(
						"Workflow soundness checking",
						numberOfExtraTokensInNet == null ? 0
								: (Integer) numberOfExtraTokensInNet.getValue(),
								new TCTLEFNode(new TCTLTrueNode()), TraceOption.SOME,
								SearchOption.HEURISTIC,
								ReductionOption.VerifyTAPNdiscreteVerification, true,
								false, false, null, ExtrapolationOption.AUTOMATIC,
								WorkflowMode.WORKFLOW_SOUNDNESS);
				Verifier.runVerifyTAPNVerification(model, q, new VerificationCallback() {

					@Override
					public void run() {
					}

					@Override
					public void run(VerificationResult<TAPNNetworkTrace> result) {
						if (result.isQuerySatisfied()) {
							soundnessResult
							.setText(RESULT_STRING_SATISFIED);
							soundnessResult
							.setForeground(Pipe.QUERY_SATISFIED_COLOR);
							if(model.hasUrgentTransitions() || model.hasInvariants()){
								soundnessResultExplanation.setText(DISCRETE_SEMANTICS_WARNING);
								soundnessResultExplanation.setVisible(true);
							}
							isSound = true;
						} else if (netType == TAWFNTypes.ETAWFN && !result.isBounded()) {
							soundnessResult
							.setText(RESULT_STRING_INCONCLUSIVE);
							soundnessResult
							.setForeground(Pipe.QUERY_INCONCLUSIVE_COLOR);
							isConclusive = false;
							soundnessResultExplanation.setText(ERROR_INCREASE_BOUND);
							soundnessResultExplanation.setVisible(true);
						} else {
							soundnessResult
							.setText(RESULT_STRING_NOT_SATISFIED);
							soundnessResult
							.setForeground(Pipe.QUERY_NOT_SATISFIED_COLOR);
							soundnessResultTrace = mapTraceToRealModel(result.getTrace());
							soundnessResultTraceButton.setVisible(true);
							soundnessResultExplanation.setText(calculateSoundnessError(result.getTrace()));
							soundnessResultExplanation.setVisible(true);
							
							NetworkMarking coveredMarking = result.getCoveredMarking(model);
							if(coveredMarking != null){
								completeSoundnessTrace(result, coveredMarking);
							}
						}
						soundnessResult.setVisible(true);
						soundnessResultLabel.setVisible(true);

						if (min.isSelected()) {
							if(result.isQuerySatisfied()){
								minResult.setText(result.stats()
										.minimumExecutionTime()
										+ " time units.");
								minResult.setForeground(Pipe.QUERY_SATISFIED_COLOR);
								minResultTrace = mapTraceToRealModel(result.getTrace());
								minResultTraceButton.setVisible(true);
							}else{
								minResult.setText(RESULT_NOT_DEFINED);
								minResult.setForeground(Pipe.QUERY_NOT_SATISFIED_COLOR);
							}
							minResultLabel.setVisible(true);
							minResult.setVisible(true);
						}

						soundnessVerificationStats.setText(result
								.getVerificationTimeString().replace("Estimated verification time", "Est. time")
								+ ", memory: "
								+ MemoryMonitor.getPeakMemory());
						soundnessVerificationStats.setVisible(true);

						m = result.stats().exploredStates();

						dialog.pack();
					}

					private void completeSoundnessTrace(final VerificationResult<TAPNNetworkTrace> soundnessResult, final NetworkMarking coveredMarking) {
						final String explanationText = soundnessResultExplanation.getText();
						soundnessResultExplanation.setText(explanationText + " Computing trace.");
						soundnessResultTraceButton.setVisible(false);
						
						final NetworkMarking oldMarking = model.marking();
						model.setMarking(coveredMarking);

						final TAPNQuery q = new TAPNQuery(
								"Workflow computing trace",
								numberOfExtraTokensInNet == null ? 0
										: (Integer) numberOfExtraTokensInNet.getValue(),
										new TCTLEFNode(new TCTLAtomicPropositionNode(out.isShared()?"":out_template.name(), out.name(), ">=",1)), TraceOption.SOME,
										SearchOption.HEURISTIC,
										ReductionOption.VerifyTAPNdiscreteVerification, true,
										true, true, null, ExtrapolationOption.AUTOMATIC);
						Verifier.runVerifyTAPNVerification(model, q, new VerificationCallback() {

							@Override
							public void run() {
							}

							@Override
							public void run(VerificationResult<TAPNNetworkTrace> result) {
								model.setMarking(oldMarking);
								soundnessResultExplanation.setText(explanationText);
								if(result.isQuerySatisfied()){
									appendTrace(mapTraceToRealModel(result.getTrace()));
									soundnessResultExplanation.setText(RESULT_ERROR_NONFINAL_REACHED);
									soundnessResultTraceButton.setVisible(true);
								}else{
									if(result.isBounded()){
										soundnessResultTrace = mapTraceToRealModel(result.getTrace());
										soundnessResultTraceButton.setVisible(true);
									}else{
										soundnessResultExplanation.setText("<html>"+ explanationText + "<br />Could not generate trace with "+numberOfExtraTokensInNet.getValue().toString() + "extra tokens.</html>");
									}
								}
							}

							private void appendTrace(TAPNNetworkTrace trace) {
								for(TAPNNetworkTraceStep step : trace){
									((TimedTAPNNetworkTrace) soundnessResultTrace).add(step);
								}
							}
						});

					}

					private String calculateSoundnessError(
							TAPNNetworkTrace trace) {

						// TODO detect subset result
						
						Iterator<TAPNNetworkTraceStep> iter = trace.iterator();
						NetworkMarking final_marking = model.marking().clone(); 
						while(iter.hasNext()) final_marking = iter.next().performStepFrom(final_marking);

						int out_size = final_marking.getTokensFor(out).size();
						if(out_size > 0 && final_marking.size() > out_size){
							return RESULT_ERROR_NONFINAL_REACHED;
						}

						// Detect if any transition is dEnabled from last marking (not deadlock)
						String output = "Deadlock reached.";
						NetworkMarking oldMarking = model.marking();
						model.setMarking(final_marking);
						outer: for( TimedArcPetriNet temp : model.activeTemplates()){
							Iterator<TimedTransition> transitionIterator = temp.transitions().iterator();
							while (transitionIterator.hasNext()) {
								TimedTransition tempTransition = transitionIterator.next();
								if (tempTransition.isDEnabled()){
									output = RESULT_ERROR_NO_TRACE_TO_FINAL;
									break outer;
								}
							}
						}
						model.setMarking(oldMarking);
						
						return output;
					}
				});
			}
		};
		return r;
	}

	private void checkBound() {
		Verifier.analyzeKBound(model,
				(Integer) numberOfExtraTokensInNet.getValue(),
				numberOfExtraTokensInNet);
	}

	private TAPNNetworkTrace mapTraceToRealModel(TAPNNetworkTrace tapnNetworkTrace){
		TraceConverter converter = new TraceConverter(tapnNetworkTrace, CreateGui.getCurrentTab().network());
		return converter.convert();
	}
}