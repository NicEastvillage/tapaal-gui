package pipe.gui.undo;

import pipe.dataLayer.Template;
import dk.aau.cs.gui.TemplateExplorer;
import dk.aau.cs.gui.undo.Command;
import dk.aau.cs.model.tapn.TimedArcPetriNet;

public class AddTemplateCommand extends Command {
	private Template<TimedArcPetriNet> template;
	private final TemplateExplorer templateExplorer;
	private int listIndex;

	public AddTemplateCommand(TemplateExplorer templateExplorer,
			Template<TimedArcPetriNet> template, int listIndex) {
		this.templateExplorer = templateExplorer;
		this.template = template;
		this.listIndex = listIndex;
	}

	@Override
	public void redo() {
		templateExplorer.addTemplate(listIndex, template);
	}

	@Override
	public void undo() {
		templateExplorer.removeTemplate(listIndex, template);
	}
}
