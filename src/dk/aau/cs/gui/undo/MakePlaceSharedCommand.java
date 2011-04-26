package dk.aau.cs.gui.undo;

import java.util.List;

import pipe.dataLayer.TimedPlaceComponent;
import dk.aau.cs.model.tapn.SharedPlace;
import dk.aau.cs.model.tapn.TimedArcPetriNet;
import dk.aau.cs.model.tapn.TimedInhibitorArc;
import dk.aau.cs.model.tapn.TimedInputArc;
import dk.aau.cs.model.tapn.TimedOutputArc;
import dk.aau.cs.model.tapn.TimedPlace;
import dk.aau.cs.model.tapn.TimedToken;
import dk.aau.cs.model.tapn.TransportArc;
import dk.aau.cs.util.Require;

public class MakePlaceSharedCommand extends Command {
	private final SharedPlace sharedPlace;
	private final TimedPlace place;
	private final TimedArcPetriNet tapn;
	private final TimedPlaceComponent placeComponent;
	
	private final List<TimedToken> oldTokens;
	
	public MakePlaceSharedCommand(TimedArcPetriNet tapn, SharedPlace sharedPlace, TimedPlace place, TimedPlaceComponent placeComponent){
		Require.that(tapn != null, "tapn cannot be null");
		Require.that(sharedPlace != null, "sharedPlace cannot be null");
		Require.that(place != null, "timedPlace cannot be null");
		Require.that(placeComponent != null, "placeComponent cannot be null");
		
		this.tapn = tapn;
		this.sharedPlace = sharedPlace;
		this.place = place;
		this.placeComponent = placeComponent;
		this.oldTokens = place.tokens();
	}
	
	@Override
	public void redo() {
		updateArcs(place, sharedPlace);
		
		tapn.remove(place);
		tapn.add(sharedPlace);
		placeComponent.setUnderlyingPlace(sharedPlace);
	}

	@Override
	public void undo() {
		updateArcs(sharedPlace, place);
		tapn.remove(sharedPlace);
		tapn.add(place);
		place.addTokens(oldTokens);
		placeComponent.setUnderlyingPlace(place);
		
	}
	
	private void updateArcs(TimedPlace toReplace, TimedPlace replacement) {
		for(TimedInputArc arc : tapn.inputArcs()){
			if(arc.source().equals(toReplace)){
				arc.setSource(replacement);
			}
		}
		
		for(TimedInhibitorArc arc : tapn.inhibitorArcs()){
			if(arc.source().equals(toReplace)){
				arc.setSource(replacement);
			}
		}
		
		for(TransportArc arc : tapn.transportArcs()){
			if(arc.source().equals(toReplace)){
				arc.setSource(replacement);
			}
			
			if(arc.destination().equals(toReplace)){
				arc.setDestination(replacement);
			}
		}
		
		for(TimedOutputArc arc : tapn.outputArcs()){
			if(arc.destination().equals(toReplace)){
				arc.setDestination(replacement);
			}
		}
	}
}