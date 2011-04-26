package dk.aau.cs.io;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pipe.dataLayer.AnnotationNote;
import pipe.dataLayer.Arc;
import pipe.dataLayer.DataLayer;
import pipe.dataLayer.Note;
import pipe.dataLayer.PetriNetObject;
import pipe.dataLayer.Place;
import pipe.dataLayer.PlaceTransitionObject;
import pipe.dataLayer.TAPNQuery;
import pipe.dataLayer.Template;
import pipe.dataLayer.TimedInhibitorArcComponent;
import pipe.dataLayer.TimedInputArcComponent;
import pipe.dataLayer.TimedOutputArcComponent;
import pipe.dataLayer.TimedPlaceComponent;
import pipe.dataLayer.TimedTransitionComponent;
import pipe.dataLayer.Transition;
import pipe.dataLayer.TransportArcComponent;
import pipe.dataLayer.TAPNQuery.ExtrapolationOption;
import pipe.dataLayer.TAPNQuery.HashTableSize;
import pipe.dataLayer.TAPNQuery.SearchOption;
import pipe.dataLayer.TAPNQuery.TraceOption;
import pipe.gui.CreateGui;
import pipe.gui.DrawingSurfaceImpl;
import pipe.gui.Grid;
import pipe.gui.Pipe;
import pipe.gui.Zoomable;
import pipe.gui.handler.AnimationHandler;
import pipe.gui.handler.AnnotationNoteHandler;
import pipe.gui.handler.ArcHandler;
import pipe.gui.handler.LabelHandler;
import pipe.gui.handler.PlaceHandler;
import pipe.gui.handler.TAPNTransitionHandler;
import pipe.gui.handler.TimedArcHandler;
import pipe.gui.handler.TransitionHandler;
import pipe.gui.handler.TransportArcHandler;
import dk.aau.cs.TCTL.TCTLAbstractProperty;
import dk.aau.cs.TCTL.Parsing.TAPAALQueryParser;
import dk.aau.cs.TCTL.visitors.AddTemplateVisitor;
import dk.aau.cs.TCTL.visitors.VerifyPlaceNamesVisitor;
import dk.aau.cs.gui.NameGenerator;
import dk.aau.cs.model.tapn.Constant;
import dk.aau.cs.model.tapn.ConstantStore;
import dk.aau.cs.model.tapn.LocalTimedPlace;
import dk.aau.cs.model.tapn.TimeInterval;
import dk.aau.cs.model.tapn.TimeInvariant;
import dk.aau.cs.model.tapn.TimedArcPetriNet;
import dk.aau.cs.model.tapn.TimedArcPetriNetNetwork;
import dk.aau.cs.model.tapn.TimedInhibitorArc;
import dk.aau.cs.model.tapn.TimedInputArc;
import dk.aau.cs.model.tapn.TimedMarking;
import dk.aau.cs.model.tapn.TimedOutputArc;
import dk.aau.cs.model.tapn.TimedPlace;
import dk.aau.cs.model.tapn.TimedToken;
import dk.aau.cs.model.tapn.TimedTransition;
import dk.aau.cs.model.tapn.TransportArc;
import dk.aau.cs.translations.ReductionOption;
import dk.aau.cs.util.FormatException;
import dk.aau.cs.util.Require;
import dk.aau.cs.util.Tuple;

public class TapnLegacyXmlLoader {

	private static final String ERROR_PARSING_QUERY_MESSAGE = "TAPAAL encountered an error trying to parse one or more of the queries in the model.\n\nThe queries that could not be parsed will not show up in the query list.";
	private HashMap<TimedTransitionComponent, TransportArcComponent> presetArcs;
	private HashMap<TimedTransitionComponent, TransportArcComponent> postsetArcs;
	private HashMap<TransportArcComponent, TimeInterval> transportArcsTimeIntervals;
	private TimedArcPetriNet tapn;
	private DataLayer guiModel;
	private ArrayList<TAPNQuery> queries;
	private TreeMap<String, Constant> constants;
	private DrawingSurfaceImpl drawingSurface;
	private NameGenerator nameGenerator = new NameGenerator();
	private boolean firstQueryParsingWarning = true;
	private boolean firstInhibitorIntervalWarning = true;

	public TapnLegacyXmlLoader(DrawingSurfaceImpl drawingSurfaceImpl) {
		presetArcs = new HashMap<TimedTransitionComponent, TransportArcComponent>();
		postsetArcs = new HashMap<TimedTransitionComponent, TransportArcComponent>();
		transportArcsTimeIntervals = new HashMap<TransportArcComponent, TimeInterval>();
		queries = new ArrayList<TAPNQuery>();
		constants = new TreeMap<String, Constant>();
		this.drawingSurface = drawingSurfaceImpl;
	}
	
	public LoadedModel load(File file) throws FormatException {
		Require.that(file != null && file.exists(), "file must be non-null and exist");

		Document doc = loadDocument(file);
		if(doc == null) return null;
		return parse(doc);
	}

	private Document loadDocument(File file) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			return builder.parse(file);
		} catch (ParserConfigurationException e) {
			return null;
		} catch (SAXException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	private LoadedModel parse(Document tapnDoc) throws FormatException { 
		ArrayList<Template> templates = new ArrayList<Template>();

		NodeList constantNodes = tapnDoc.getElementsByTagName("constant");
		for (int i = 0; i < constantNodes.getLength(); i++) {
			Node c = constantNodes.item(i);

			if (c instanceof Element) {
				parseAndAddConstant((Element) c);
			}
		}

		TimedArcPetriNetNetwork network = new TimedArcPetriNetNetwork(new ConstantStore(constants.values()));
		NodeList nets = tapnDoc.getElementsByTagName("net");
		
		if(nets.getLength() <= 0)
			throw new FormatException("File did not contain any TAPN components.");
		
		templates.add(parseTimedArcPetriNetAsOldFormat(nets.item(0), network));
		
		checkThatQueriesUseExistingPlaces(network);
		
		return new LoadedModel(network, templates, queries);
	}

	private void checkThatQueriesUseExistingPlaces(TimedArcPetriNetNetwork network) {
		ArrayList<TAPNQuery> okQueries = new ArrayList<TAPNQuery>();
		ArrayList<Tuple<String,String>> templatePlaceNames = getTemplatePlaceNames(network);
		for(TAPNQuery query : queries) {
			if(!doesPlacesUsedInQueryExist(query, templatePlaceNames)) {
				if(firstQueryParsingWarning) {
					JOptionPane.showMessageDialog(CreateGui.getApp(), ERROR_PARSING_QUERY_MESSAGE, "Error Parsing Query", JOptionPane.ERROR_MESSAGE);
					firstQueryParsingWarning = false;
				}
				continue;
			}
			
			okQueries.add(query);
		}
		
		queries = okQueries;
	}

	private ArrayList<Tuple<String, String>> getTemplatePlaceNames(TimedArcPetriNetNetwork network) {
		ArrayList<Tuple<String,String>> templatePlaceNames = new ArrayList<Tuple<String,String>>();
		for(TimedArcPetriNet tapn : network.templates()) {
			for(TimedPlace p : tapn.places()) {
				templatePlaceNames.add(new Tuple<String, String>(tapn.name(), p.name()));
			}
		}
		
		for(TimedPlace p : network.sharedPlaces()) {
			templatePlaceNames.add(new Tuple<String, String>("", p.name()));
		}
		return templatePlaceNames;
	}

	private Arc parseAndAddTimedOutputArc(String idInput, boolean taggedArc,
			String inscriptionTempStorage, PlaceTransitionObject sourceIn,
			PlaceTransitionObject targetIn, double _startx, double _starty,
			double _endx, double _endy) throws FormatException {
		
		Arc tempArc;
		tempArc = new TimedOutputArcComponent(_startx, _starty, _endx, _endy, 
				sourceIn, targetIn,	Integer.valueOf(inscriptionTempStorage), idInput, taggedArc);

		TimedPlace place = tapn.getPlaceByName(targetIn.getName());
		TimedTransition transition = tapn.getTransitionByName(sourceIn.getName());

		TimedOutputArc outputArc = new TimedOutputArc(transition, place);
		((TimedOutputArcComponent) tempArc).setUnderlyingArc(outputArc);
		
		if(tapn.hasArcFromTransitionToPlace(outputArc.source(),outputArc.destination())) {
			throw new FormatException("Multiple arcs between a place and a transition is not allowed");
		}
		
		guiModel.addPetriNetObject(tempArc);
		addListeners(tempArc);
		tapn.add(outputArc);

		sourceIn.addConnectFrom(tempArc);
		targetIn.addConnectTo(tempArc);
		return tempArc;
	}

	private Arc parseAndAddTransportArc(String idInput, boolean taggedArc,
			String inscriptionTempStorage, PlaceTransitionObject sourceIn,
			PlaceTransitionObject targetIn, double _startx, double _starty,
			double _endx, double _endy) {
		
		Arc tempArc;
		String[] inscriptionSplit = {};
		if (inscriptionTempStorage.contains(":")) {
			inscriptionSplit = inscriptionTempStorage.split(":");
		}
		boolean isInPreSet = false;
		if (sourceIn instanceof Place) {
			isInPreSet = true;
		}
		tempArc = new TransportArcComponent(new TimedInputArcComponent(
						new TimedOutputArcComponent(_startx, _starty, _endx, _endy,	sourceIn, targetIn, 1, idInput, taggedArc),
						inscriptionSplit[0]), Integer.parseInt(inscriptionSplit[1]), isInPreSet);

		sourceIn.addConnectFrom(tempArc);
		targetIn.addConnectTo(tempArc);

		if (isInPreSet) {
			if (postsetArcs.containsKey((TimedTransitionComponent) targetIn)) {
				TransportArcComponent postsetTransportArc = postsetArcs.get((TimedTransitionComponent) targetIn);
				TimedPlace sourcePlace = tapn.getPlaceByName(sourceIn.getName());
				TimedTransition trans = tapn.getTransitionByName(targetIn.getName());
				TimedPlace destPlace = tapn.getPlaceByName(postsetTransportArc.getTarget().getName());
				TimeInterval interval = TimeInterval.parse(inscriptionSplit[0],	constants);

				assert (sourcePlace != null);
				assert (trans != null);
				assert (destPlace != null);

				TransportArc transArc = new TransportArc(sourcePlace, trans, destPlace, interval);

				((TransportArcComponent) tempArc).setUnderlyingArc(transArc);
				postsetTransportArc.setUnderlyingArc(transArc);
				guiModel.addPetriNetObject(tempArc);
				addListeners(tempArc);
				guiModel.addPetriNetObject(postsetTransportArc);
				addListeners(postsetTransportArc);
				tapn.add(transArc);

				postsetArcs.remove((TimedTransitionComponent) targetIn);
			} else {
				presetArcs.put((TimedTransitionComponent) targetIn,	(TransportArcComponent) tempArc);
				transportArcsTimeIntervals.put((TransportArcComponent) tempArc, TimeInterval.parse(inscriptionSplit[0], constants));
			}
		} else {
			if (presetArcs.containsKey((TimedTransitionComponent) sourceIn)) {
				TransportArcComponent presetTransportArc = presetArcs.get((TimedTransitionComponent) sourceIn);
				TimedPlace sourcePlace = tapn.getPlaceByName(presetTransportArc.getSource().getName());
				TimedTransition trans = tapn.getTransitionByName(sourceIn.getName());
				TimedPlace destPlace = tapn.getPlaceByName(targetIn.getName());
				TimeInterval interval = transportArcsTimeIntervals.get((TransportArcComponent) presetTransportArc);

				assert (sourcePlace != null);
				assert (trans != null);
				assert (destPlace != null);

				TransportArc transArc = new TransportArc(sourcePlace, trans,
						destPlace, interval);

				((TransportArcComponent) tempArc).setUnderlyingArc(transArc);
				presetTransportArc.setUnderlyingArc(transArc);
				guiModel.addPetriNetObject(presetTransportArc);
				addListeners(presetTransportArc);
				guiModel.addPetriNetObject(tempArc);
				addListeners(tempArc);
				tapn.add(transArc);

				presetArcs.remove((TimedTransitionComponent) sourceIn);
				transportArcsTimeIntervals.remove((TransportArcComponent) presetTransportArc);
			} else {
				postsetArcs.put((TimedTransitionComponent) sourceIn, (TransportArcComponent) tempArc);
			}
		}
		return tempArc;
	}

	private Arc parseAndAddTimedInputArc(String idInput, boolean taggedArc,
			String inscriptionTempStorage, PlaceTransitionObject sourceIn,
			PlaceTransitionObject targetIn, double _startx, double _starty,
			double _endx, double _endy) throws FormatException {
		Arc tempArc;
		tempArc = new TimedInputArcComponent(new TimedOutputArcComponent(
				_startx, _starty, _endx, _endy, sourceIn, targetIn, 1, idInput,
				taggedArc),
				(inscriptionTempStorage != null ? inscriptionTempStorage : ""));

		TimedPlace place = tapn.getPlaceByName(sourceIn.getName());
		TimedTransition transition = tapn.getTransitionByName(targetIn.getName());
		TimeInterval interval = TimeInterval.parse(inscriptionTempStorage, constants);

		TimedInputArc inputArc = new TimedInputArc(place, transition, interval);
		((TimedInputArcComponent) tempArc).setUnderlyingArc(inputArc);
		
		if(tapn.hasArcFromPlaceToTransition(inputArc.source(), inputArc.destination())) {
			throw new FormatException("Multiple arcs between a place and a transition is not allowed");
		}
		
		guiModel.addPetriNetObject(tempArc);
		addListeners(tempArc);
		tapn.add(inputArc);

		sourceIn.addConnectFrom(tempArc);
		targetIn.addConnectTo(tempArc);
		return tempArc;
	}

	private Arc parseAndAddTimedInhibitorArc(String idInput, boolean taggedArc,
			String inscriptionTempStorage, PlaceTransitionObject sourceIn,
			PlaceTransitionObject targetIn, double _startx, double _starty,
			double _endx, double _endy) {
		Arc tempArc;
		tempArc = new TimedInhibitorArcComponent(
					new TimedInputArcComponent(
						new TimedOutputArcComponent(_startx, _starty, _endx, _endy,	sourceIn, targetIn, 1, idInput, taggedArc)
					),
				(inscriptionTempStorage != null ? inscriptionTempStorage : ""));
		TimedPlace place = tapn.getPlaceByName(sourceIn.getName());
		TimedTransition transition = tapn.getTransitionByName(targetIn.getName());
		TimeInterval interval = TimeInterval.parse(inscriptionTempStorage, constants);
		
		if(!interval.equals(TimeInterval.ZERO_INF) && firstInhibitorIntervalWarning) {
			JOptionPane.showMessageDialog(CreateGui.getApp(), "The chosen model contained inhibitor arcs with unsupported intervals.\n\nTAPAAL only supports inhibitor arcs with intervals [0,inf).\n\nAny other interval on inhibitor arcs will be replaced with [0,inf).", "Unsupported Interval Detected on Inhibitor Arc", JOptionPane.INFORMATION_MESSAGE);
			firstInhibitorIntervalWarning = false;
		}
		
		TimedInhibitorArc inhibArc = new TimedInhibitorArc(place, transition, interval);

		((TimedInhibitorArcComponent) tempArc).setUnderlyingArc(inhibArc);
		guiModel.addPetriNetObject(tempArc);
		addListeners(tempArc);
		tapn.add(inhibArc);

		sourceIn.addConnectFrom(tempArc);
		targetIn.addConnectTo(tempArc);
		return tempArc;
	}

	private ReductionOption getQueryReductionOption(Element queryElement) {
		ReductionOption reductionOption;
		try {
			reductionOption = ReductionOption.valueOf(queryElement.getAttribute("reductionOption"));
		} catch (Exception e) {
			reductionOption = ReductionOption.STANDARD;
		}
		return reductionOption;
	}

	private ExtrapolationOption getQueryExtrapolationOption(Element queryElement) {
		ExtrapolationOption extrapolationOption;
		try {
			extrapolationOption = ExtrapolationOption.valueOf(queryElement.getAttribute("extrapolationOption"));
		} catch (Exception e) {
			extrapolationOption = ExtrapolationOption.AUTOMATIC;
		}
		return extrapolationOption;
	}

	private HashTableSize getQueryHashTableSize(Element queryElement) {
		HashTableSize hashTableSize;
		try {
			hashTableSize = HashTableSize.valueOf(queryElement.getAttribute("hashTableSize"));
		} catch (Exception e) {
			hashTableSize = HashTableSize.MB_16;
		}
		return hashTableSize;
	}

	private SearchOption getQuerySearchOption(Element queryElement) {
		SearchOption searchOption;
		try {
			searchOption = SearchOption.valueOf(queryElement.getAttribute("searchOption"));
		} catch (Exception e) {
			searchOption = SearchOption.BFS;
		}
		return searchOption;
	}

	private TraceOption getQueryTraceOption(Element queryElement) {
		TraceOption traceOption;
		try {
			traceOption = TraceOption.valueOf(queryElement.getAttribute("traceOption"));
		} catch (Exception e) {
			traceOption = TraceOption.NONE;
		}
		return traceOption;
	}

	private String getQueryComment(Element queryElement) {
		String comment;
		try {
			comment = queryElement.getAttribute("name");
		} catch (Exception e) {
			comment = "No comment specified";
		}
		return comment;
	}

	private void parseAndAddConstant(Element constantElement) {
		String name = constantElement.getAttribute("name");
		int value = Integer.parseInt(constantElement.getAttribute("value"));

		if (!name.isEmpty() && !name.equals(""))
			constants.put(name, new Constant(name, value));
	}

	// //////////////////////////////////////////////////////////
	// Legacy support for old format
	// //////////////////////////////////////////////////////////
	private Template parseTimedArcPetriNetAsOldFormat(Node tapnNode, TimedArcPetriNetNetwork network) throws FormatException {
		tapn = new TimedArcPetriNet(nameGenerator .getNewTemplateName());
		network.add(tapn);

		guiModel = new DataLayer();

		Node node = null;
		NodeList nodeList = null;

		nodeList = tapnNode.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			node = nodeList.item(i);
			parseElementAsOldFormat(node, tapn.name(), network.marking());
		}

		return new Template(tapn, guiModel);
	}

	private void parseElementAsOldFormat(Node node, String templateName, TimedMarking marking) throws FormatException {
		Element element;
		if (node instanceof Element) {
			element = (Element) node;
			if ("labels".equals(element.getNodeName())) {
				parseAndAddAnnotationAsOldFormat(element);
			} else if ("place".equals(element.getNodeName())) {
				parseAndAddPlaceAsOldFormat(element, marking);
			} else if ("transition".equals(element.getNodeName())) {
				parseAndAddTransitionAsOldFormat(element);
			} else if ("arc".equals(element.getNodeName())) {
				parseAndAddArcAsOldFormat(element);
			} else if ("queries".equals(element.getNodeName())) {
				TAPNQuery query = parseQueryAsOldFormat(element);
				
				
				if (query != null) {
					query.getProperty().accept(new AddTemplateVisitor(templateName), null);
					queries.add(query);
				}
			}
		}
	}

	private boolean doesPlacesUsedInQueryExist(TAPNQuery query, ArrayList<Tuple<String, String>> templatePlaceNames) {
		VerifyPlaceNamesVisitor nameChecker = new VerifyPlaceNamesVisitor(templatePlaceNames);

		VerifyPlaceNamesVisitor.Context c = nameChecker.VerifyPlaceNames(query.getProperty());
		
		return c.getResult();
	}

	private void parseAndAddAnnotationAsOldFormat(Element inputLabelElement) throws FormatException {
		int positionXInput = 0;
		int positionYInput = 0;
		int widthInput = 0;
		int heightInput = 0;
		boolean borderInput = true;

		String positionXTempStorage = inputLabelElement.getAttribute("x");
		String positionYTempStorage = inputLabelElement.getAttribute("y");
		String widthTemp = inputLabelElement.getAttribute("width");
		String heightTemp = inputLabelElement.getAttribute("height");
		String borderTemp = inputLabelElement.getAttribute("border");

		String text = getFirstChildNodeByName(inputLabelElement, "text").getTextContent();

		if (positionXTempStorage.length() > 0) {
			positionXInput = Integer.valueOf(positionXTempStorage).intValue() + 1;
		}

		if (positionYTempStorage.length() > 0) {
			positionYInput = Integer.valueOf(positionYTempStorage).intValue() + 1;
		}

		if (widthTemp.length() > 0) {
			widthInput = Integer.valueOf(widthTemp).intValue() + 1;
		}

		if (heightTemp.length() > 0) {
			heightInput = Integer.valueOf(heightTemp).intValue() + 1;
		}

		if (borderTemp.length() > 0) {
			borderInput = Boolean.valueOf(borderTemp).booleanValue();
		} else {
			borderInput = true;
		}
		AnnotationNote an = new AnnotationNote(text, positionXInput,
				positionYInput, widthInput, heightInput, borderInput);
		guiModel.addPetriNetObject(an);
		addListeners(an);
	}

	private void parseAndAddTransitionAsOldFormat(Element element) throws FormatException {
		double positionXInput = getPositionAttribute(element, "x");
		double positionYInput = getPositionAttribute(element, "y");
		String idInput = element.getAttribute("id");
		String nameInput = getChildNodesContentOfValueChildNodeAsString(element, "name");
		double nameOffsetXInput = getNameOffsetAttribute(element, "x");
		double nameOffsetYInput = getNameOffsetAttribute(element, "y");
		boolean timedTransition = getContentOfFirstSpecificChildNodesValueChildNodeAsBoolean(element, "timed");
		boolean infiniteServer = getContentOfFirstSpecificChildNodesValueChildNodeAsBoolean(element, "infiniteServer");
		int angle = getContentOfFirstSpecificChildNodesValueChildNodeAsInt(element,"orientation");
		int priority = getContentOfFirstSpecificChildNodesValueChildNodeAsInt(element,"priority");

		positionXInput = Grid.getModifiedX(positionXInput);
		positionYInput = Grid.getModifiedY(positionYInput);

		if (idInput.length() == 0 && nameInput.length() > 0) {
			idInput = nameInput;
		}

		if (nameInput.length() == 0 && idInput.length() > 0) {
			nameInput = idInput;
		}

		TimedTransition t = new TimedTransition(nameInput);

		TimedTransitionComponent transition = new TimedTransitionComponent(
				positionXInput, positionYInput, idInput, nameInput,
				nameOffsetXInput, nameOffsetYInput, timedTransition,
				infiniteServer, angle, priority);
		transition.setUnderlyingTransition(t);
		transition.setTimed(true);
		guiModel.addPetriNetObject(transition);
		addListeners(transition);
		tapn.add(t);
	}

	private void parseAndAddPlaceAsOldFormat(Element element, TimedMarking marking) throws FormatException {
		double positionXInput = getPositionAttribute(element, "x");
		double positionYInput = getPositionAttribute(element, "y");
		String idInput = element.getAttribute("id");
		String nameInput = getChildNodesContentOfValueChildNodeAsString(element, "name");
		double nameOffsetXInput = getNameOffsetAttribute(element, "x");
		double nameOffsetYInput = getNameOffsetAttribute(element, "y");
		int initialMarkingInput = getContentOfFirstSpecificChildNodesValueChildNodeAsInt(element, "initialMarking");
		double markingOffsetXInput = getMarkingOffsetAttribute(element, "x");
		double markingOffsetYInput = getMarkingOffsetAttribute(element, "y");
		int capacityInput = getContentOfFirstSpecificChildNodesValueChildNodeAsInt(element,	"capacity");
		String invariant = getChildNodesContentOfValueChildNodeAsString(element, "invariant");

		positionXInput = Grid.getModifiedX(positionXInput);
		positionYInput = Grid.getModifiedY(positionYInput);

		if (idInput.length() == 0 && nameInput.length() > 0) {
			idInput = nameInput;
		}

		if (nameInput.length() == 0 && idInput.length() > 0) {
			nameInput = idInput;
		}

		Place place = null;

		if (invariant == null || invariant == "") {
			place = new Place(positionXInput, positionYInput, idInput,
					nameInput, nameOffsetXInput, nameOffsetYInput,
					initialMarkingInput, markingOffsetXInput,
					markingOffsetYInput, capacityInput);

		} else {
			place = new TimedPlaceComponent(positionXInput, positionYInput,
					idInput, nameInput, nameOffsetXInput, nameOffsetYInput,
					initialMarkingInput, markingOffsetXInput,
					markingOffsetYInput, capacityInput);

			LocalTimedPlace p = new LocalTimedPlace(nameInput, TimeInvariant.parse(invariant, constants));
			tapn.add(p);
			
			((TimedPlaceComponent) place).setUnderlyingPlace(p);
			guiModel.addPetriNetObject(place);
			addListeners(place);

			for (int i = 0; i < initialMarkingInput; i++) {
				marking.add(new TimedToken(p, new BigDecimal(0.0)));
			}
		}
	}

	private void parseAndAddArcAsOldFormat(Element inputArcElement) throws FormatException {
		String idInput = inputArcElement.getAttribute("id");
		String sourceInput = inputArcElement.getAttribute("source");
		String targetInput = inputArcElement.getAttribute("target");
		boolean taggedArc = getContentOfFirstSpecificChildNodesValueChildNodeAsBoolean(inputArcElement, "tagged");
		String inscriptionTempStorage = getChildNodesContentOfValueChildNodeAsString(inputArcElement, "inscription");

		PlaceTransitionObject sourceIn = guiModel.getPlaceTransitionObject(sourceInput);
		PlaceTransitionObject targetIn = guiModel.getPlaceTransitionObject(targetInput);

		// add the insets and offset
		int aStartx = sourceIn.getX() + sourceIn.centreOffsetLeft();
		int aStarty = sourceIn.getY() + sourceIn.centreOffsetTop();

		int aEndx = targetIn.getX() + targetIn.centreOffsetLeft();
		int aEndy = targetIn.getY() + targetIn.centreOffsetTop();

		double _startx = aStartx;
		double _starty = aStarty;
		double _endx = aEndx;
		double _endy = aEndy;

		Arc tempArc;

		String type = "normal";
		type = ((Element) getFirstChildNodeByName(inputArcElement, "type")).getAttribute("value");

		if (type.equals("tapnInhibitor")) {

			tempArc = parseAndAddTimedInhibitorArc(idInput, taggedArc,
					inscriptionTempStorage, sourceIn, targetIn, _startx,
					_starty, _endx, _endy);

		} else {
			if (type.equals("timed")) {
				tempArc = parseAndAddTimedInputArc(idInput, taggedArc,
						inscriptionTempStorage, sourceIn, targetIn, _startx,
						_starty, _endx, _endy);

			} else if (type.equals("transport")) {
				tempArc = parseAndAddTransportArc(idInput, taggedArc,
						inscriptionTempStorage, sourceIn, targetIn, _startx,
						_starty, _endx, _endy);

			} else {
				tempArc = parseAndAddTimedOutputArc(idInput, taggedArc,
						inscriptionTempStorage, sourceIn, targetIn, _startx,
						_starty, _endx, _endy);
			}

		}

		parseArcPathAsOldFormat(inputArcElement, tempArc);
	}

	private void parseArcPathAsOldFormat(Element inputArcElement, Arc tempArc) {
		NodeList nodelist = inputArcElement.getElementsByTagName("arcpath");
		if (nodelist.getLength() > 0) {
			tempArc.getArcPath().purgePathPoints();
			for (int i = 0; i < nodelist.getLength(); i++) {
				Node node = nodelist.item(i);
				if (node instanceof Element) {
					Element element = (Element) node;
					if ("arcpath".equals(element.getNodeName())) {
						String arcTempX = element.getAttribute("x");
						String arcTempY = element.getAttribute("y");
						String arcTempType = element.getAttribute("curvePoint");
						float arcPointX = Float.valueOf(arcTempX).floatValue();
						float arcPointY = Float.valueOf(arcTempY).floatValue();
						arcPointX += Pipe.ARC_CONTROL_POINT_CONSTANT + 1;
						arcPointY += Pipe.ARC_CONTROL_POINT_CONSTANT + 1;
						boolean arcPointType = Boolean.valueOf(arcTempType).booleanValue();
						tempArc.getArcPath().addPoint(arcPointX, arcPointY,	arcPointType);
					}
				}
			}
		}
	}

	private TAPNQuery parseQueryAsOldFormat(Element queryElement) throws FormatException {
		String comment = getQueryComment(queryElement);
		TraceOption traceOption = getQueryTraceOption(queryElement);
		SearchOption searchOption = getQuerySearchOption(queryElement);
		HashTableSize hashTableSize = getQueryHashTableSize(queryElement);
		ExtrapolationOption extrapolationOption = getQueryExtrapolationOption(queryElement);
		ReductionOption reductionOption = getQueryReductionOption(queryElement);
		int capacity = getQueryCapacityAsOldFormat(queryElement);

		TCTLAbstractProperty query;
		query = parseQueryPropertyAsOldFormat(queryElement);
		
		if (query != null)
			return new TAPNQuery(comment, capacity, query, traceOption,
					searchOption, reductionOption, hashTableSize,
					extrapolationOption);
		else
			return null;
	}

	private TCTLAbstractProperty parseQueryPropertyAsOldFormat(Element queryElement) throws FormatException {
		TCTLAbstractProperty query = null;
		TAPAALQueryParser queryParser = new TAPAALQueryParser();

		String queryToParse = getChildNodesContentOfValueChildNodeAsString(queryElement, "query");

		try {
			query = queryParser.parse(queryToParse);
		} catch (Exception e) {
			if(firstQueryParsingWarning ) {
				JOptionPane.showMessageDialog(CreateGui.getApp(), ERROR_PARSING_QUERY_MESSAGE, "Error Parsing Query", JOptionPane.ERROR_MESSAGE);
				firstQueryParsingWarning = false;
			}
			System.err.println("No query was specified: " + e.getStackTrace());
		}
		return query;
	}

	private int getQueryCapacityAsOldFormat(Element queryElement) throws FormatException {
		return getContentOfFirstSpecificChildNodesValueChildNodeAsInt(queryElement, "capacity");
	}

	private boolean getContentOfFirstSpecificChildNodesValueChildNodeAsBoolean(Element element, String childNodeName) throws FormatException {
		Node node = getFirstChildNodeByName(element, childNodeName);

		if (node instanceof Element) {
			Element e = (Element) node;

			String value = getContentOfValueChildNode(e);

			return Boolean.parseBoolean(value);
		}

		return false;
	}

	private double getNameOffsetAttribute(Element element, String coordinateName) throws FormatException {
		Node node = getFirstChildNodeByName(element, "name");

		if (node instanceof Element) {
			Element e = (Element) node;

			Element graphics = ((Element) getFirstChildNodeByName(e, "graphics"));
			String offsetCoordinate = ((Element) getFirstChildNodeByName(graphics, "offset")).getAttribute(coordinateName);
			if (offsetCoordinate.length() > 0) {
				return Double.valueOf(offsetCoordinate).doubleValue();
			}
		}

		return 0.0;
	}

	private Node getFirstChildNodeByName(Element element, String childNodeName) throws FormatException {
		Node node = element.getElementsByTagName(childNodeName).item(0);

		if (node == null)
			throw new FormatException("TAPAAL could not recognize save format.");

		return node;
	}

	private String getContentOfValueChildNode(Element element) throws FormatException {
		return ((Element) getFirstChildNodeByName(element, "value")).getTextContent();
	}

	private String getChildNodesContentOfValueChildNodeAsString(Element element, String childNodeName) throws FormatException {
		Node node = getFirstChildNodeByName(element, childNodeName);

		if (node instanceof Element) {
			Element e = (Element) node;

			return getContentOfValueChildNode(e);
		}

		return "";
	}

	private double getPositionAttribute(Element element, String coordinateName) throws FormatException {
		Node node = getFirstChildNodeByName(element, "graphics");

		if (node instanceof Element) {
			Element e = (Element) node;

			String posCoordinate = ((Element) getFirstChildNodeByName(e, "position")).getAttribute(coordinateName);
			if (posCoordinate.length() > 0) {
				return Double.valueOf(posCoordinate).doubleValue();
			}
		}

		return 0.0;
	}

	private int getContentOfFirstSpecificChildNodesValueChildNodeAsInt(Element element, String childNodeName) throws FormatException {
		Node node = getFirstChildNodeByName(element, childNodeName);

		if (node instanceof Element) {
			Element e = (Element) node;

			String value = getContentOfValueChildNode(e);

			if (value.length() > 0)
				return Integer.parseInt(value);
		}

		return 0;
	}

	private double getMarkingOffsetAttribute(Element element, String coordinateName) throws FormatException {
		Node node = getFirstChildNodeByName(element, "initialMarking");

		if (node instanceof Element) {
			Element e = (Element) node;

			Element graphics = ((Element) getFirstChildNodeByName(e, "graphics"));
			String offsetCoordinate = ((Element) getFirstChildNodeByName(graphics, "offset")).getAttribute(coordinateName);
			if (offsetCoordinate.length() > 0)
				return Double.parseDouble(offsetCoordinate);
		}

		return 0.0;
	}

	private void addListeners(PetriNetObject newObject) {
		if (newObject != null) {
			if (newObject.getMouseListeners().length == 0) {
				if (newObject instanceof Place) {
					// XXX - kyrke
					if (newObject instanceof TimedPlaceComponent) {

						LabelHandler labelHandler = new LabelHandler(((Place) newObject).getNameLabel(), (Place) newObject);
						((Place) newObject).getNameLabel().addMouseListener(labelHandler);
						((Place) newObject).getNameLabel().addMouseMotionListener(labelHandler);
						((Place) newObject).getNameLabel().addMouseWheelListener(labelHandler);

						PlaceHandler placeHandler = new PlaceHandler(drawingSurface, (Place) newObject, guiModel, tapn);
						newObject.addMouseListener(placeHandler);
						newObject.addMouseWheelListener(placeHandler);
						newObject.addMouseMotionListener(placeHandler);
					} else {

						LabelHandler labelHandler = new LabelHandler(((Place) newObject).getNameLabel(), (Place) newObject);
						((Place) newObject).getNameLabel().addMouseListener(labelHandler);
						((Place) newObject).getNameLabel().addMouseMotionListener(labelHandler);
						((Place) newObject).getNameLabel().addMouseWheelListener(labelHandler);

						PlaceHandler placeHandler = new PlaceHandler(drawingSurface, (Place) newObject);
						newObject.addMouseListener(placeHandler);
						newObject.addMouseWheelListener(placeHandler);
						newObject.addMouseMotionListener(placeHandler);

					}
				} else if (newObject instanceof Transition) {
					TransitionHandler transitionHandler;
					if (newObject instanceof TimedTransitionComponent) {
						transitionHandler = new TAPNTransitionHandler(drawingSurface, (Transition) newObject, guiModel, tapn);
					} else {
						transitionHandler = new TransitionHandler(drawingSurface, (Transition) newObject);
					}

					LabelHandler labelHandler = new LabelHandler(((Transition) newObject).getNameLabel(), (Transition) newObject);
					((Transition) newObject).getNameLabel().addMouseListener(labelHandler);
					((Transition) newObject).getNameLabel().addMouseMotionListener(labelHandler);
					((Transition) newObject).getNameLabel().addMouseWheelListener(labelHandler);

					newObject.addMouseListener(transitionHandler);
					newObject.addMouseMotionListener(transitionHandler);
					newObject.addMouseWheelListener(transitionHandler);

					newObject.addMouseListener(new AnimationHandler());

				} else if (newObject instanceof Arc) {
					/* CB - Joakim Byg add timed arcs */
					if (newObject instanceof TimedInputArcComponent) {
						if (newObject instanceof TransportArcComponent) {
							TransportArcHandler transportArcHandler = new TransportArcHandler(drawingSurface, (Arc) newObject);
							newObject.addMouseListener(transportArcHandler);
							newObject.addMouseWheelListener(transportArcHandler);
							newObject.addMouseMotionListener(transportArcHandler);
						} else {
							TimedArcHandler timedArcHandler = new TimedArcHandler(drawingSurface, (Arc) newObject);
							newObject.addMouseListener(timedArcHandler);
							newObject.addMouseWheelListener(timedArcHandler);
							newObject.addMouseMotionListener(timedArcHandler);
						}
					} else {
						/* EOC */
						ArcHandler arcHandler = new ArcHandler(drawingSurface,(Arc) newObject);
						newObject.addMouseListener(arcHandler);
						newObject.addMouseWheelListener(arcHandler);
						newObject.addMouseMotionListener(arcHandler);
					}
				} else if (newObject instanceof AnnotationNote) {
					AnnotationNoteHandler noteHandler = new AnnotationNoteHandler(drawingSurface, (AnnotationNote) newObject);
					newObject.addMouseListener(noteHandler);
					newObject.addMouseMotionListener(noteHandler);
					((Note) newObject).getNote().addMouseListener(noteHandler);
					((Note) newObject).getNote().addMouseMotionListener(noteHandler);
				}
				if (newObject instanceof Zoomable) {
					newObject.zoomUpdate(drawingSurface.getZoom());
				}
			}
			newObject.setGuiModel(guiModel);
		}
	}
}