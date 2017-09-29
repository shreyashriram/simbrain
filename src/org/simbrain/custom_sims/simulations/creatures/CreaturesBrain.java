package org.simbrain.custom_sims.simulations.creatures;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.subnetworks.WinnerTakeAll;

/**
 * A helper class of Creatures for filling in networks, from either a base
 * template or from genetic code.
 *
 * @author Sharai
 *
 */
public class CreaturesBrain {

	/**
	 * List of lobes.
	 */
	private List<NeuronGroup> lobes = new ArrayList();
	
	/**
	 * The NetBuilder object this wraps around.
	 */
	private NetBuilder builder;
	
	/**
	 * The amount of space to spread between neurons.
	 */
	private double gridSpace = 50;

	/**
	 * Constructor.
	 *
	 * @param component
	 */
	public CreaturesBrain(NetworkComponent component) {
		this.builder = new NetBuilder(component);
	}

	/**
	 * Constructor.
	 *
	 * @param netBuilder
	 */
	public CreaturesBrain(NetBuilder netBuilder) {
		this.builder = netBuilder;
	}

	// Helper methods

	/**
	 * Creates a new lobe.
	 *
	 * @param x
	 * @param y
	 * @param numNeurons
	 * @param layoutName
	 *            Valid input includes "line", "vertical line", and "grid".
	 * @param lobeName
	 * @param neuronRule
	 *            (optional)
	 * @return
	 */
	public NeuronGroup createLobe(double x, double y, int numNeurons, String layoutName, String lobeName,
			CreaturesNeuronRule neuronRule) {
		NeuronGroup lobe = builder.addNeuronGroup(x, y, numNeurons, layoutName, neuronRule);
		lobe.setLabel(lobeName);
		lobes.add(lobe);
		return lobe;
	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param numNeurons
	 * @param layoutName
	 * @param lobeName
	 * @return
	 */
	public NeuronGroup createLobe(double x, double y, int numNeurons, String layoutName, String lobeName) {
		return createLobe(x, y, numNeurons, layoutName, lobeName, new CreaturesNeuronRule());
	}
	
	/**
	 * Names a neuron.
	 *
	 * @param lobe
	 * @param neuronIndex
	 * @param name
	 */
	public void nameNeuron(NeuronGroup lobe, int neuronIndex, String name) {
		lobe.getNeuronList().get(neuronIndex).setLabel(name);
	}

	/**
	 * Copies neuron labels from one neuron group to another if they are the same
	 * size.
	 *
	 * @param lobeToCopy
	 * @param lobeToPasteTo
	 * @param startPasteIndex
	 */
	public void copyLabels(NeuronGroup lobeToCopy, NeuronGroup lobeToPasteTo, int startPasteIndex) {
		if (lobeToCopy.size() <= lobeToPasteTo.size() - startPasteIndex) {
			for (int i = startPasteIndex; i < lobeToCopy.size(); i++) {
				lobeToPasteTo.getNeuronList().get(i).setLabel(lobeToCopy.getNeuronList().get(i).getLabel());
			}
		} else {
			System.out.print("copyToLabels error: Lobe " + lobeToCopy.getLabel() + " is too big to copy to Lobe "
					+ lobeToPasteTo.getLabel() + " starting at index " + startPasteIndex);
		}
	}

	public void copyLabels(NeuronGroup lobeToCopy, NeuronGroup lobeToPasteTo) {
		copyLabels(lobeToCopy, lobeToPasteTo, 0);
	}

	/**
	 * Manually gives a grid layout with a set number of columns to a certain lobe.
	 *
	 * @param lobe
	 * @param numColumns
	 */
	public void setLobeColumns(NeuronGroup lobe, int numColumns) {
		GridLayout gridLayout = new GridLayout(gridSpace, gridSpace, numColumns);
		lobe.setLayout(gridLayout);
		lobe.applyLayout();
	}

	// Methods for building specific pre-fabricated non-mutable lobes

	public NeuronGroup buildDriveLobe() {
		NeuronGroup lobe = createLobe(1580, 1300, 12, "grid", "Lobe #1: Drives");
		setLobeColumns(lobe, 6);

		nameNeuron(lobe, 0, "Pain");
		nameNeuron(lobe, 1, "Comfort");
		nameNeuron(lobe, 2, "Hunger");
		nameNeuron(lobe, 3, "Temperature");
		nameNeuron(lobe, 4, "Fatigue");
		nameNeuron(lobe, 5, "Drowsiness");
		nameNeuron(lobe, 6, "Lonliness");
		nameNeuron(lobe, 7, "Crowdedness");
		nameNeuron(lobe, 8, "Fear");
		nameNeuron(lobe, 9, "Boredom");
		nameNeuron(lobe, 10, "Anger");
		nameNeuron(lobe, 11, "Arousal");

		return lobe;
	}

	// TODO: Make this a WTA lobe. (Should we use the default WTA subnetwork
	// or make our own?)
	public NeuronGroup buildStimulusLobe() {
		NeuronGroup lobe = createLobe(610, 995, 7, "line", "Lobe #2: Stimulus Source");

		nameNeuron(lobe, 0, "Toy");
		nameNeuron(lobe, 1, "Fish");
		nameNeuron(lobe, 2, "Cheese");
		nameNeuron(lobe, 3, "Poison");
		nameNeuron(lobe, 4, "Hazard");
		nameNeuron(lobe, 5, "Flower");
		nameNeuron(lobe, 6, "Mouse");

		return lobe;
	}

	// TODO: Make this a WTA lobe.
	public NeuronGroup buildVerbLobe() {
		NeuronGroup lobe = createLobe(1715, 1000, 13, "grid", "Lobe #3: Verbs");
		setLobeColumns(lobe, 7);

		nameNeuron(lobe, 0, "Wait");
		nameNeuron(lobe, 1, "Left");
		nameNeuron(lobe, 2, "Right");
		nameNeuron(lobe, 3, "Forward");
		nameNeuron(lobe, 4, "Backward");
		nameNeuron(lobe, 5, "Sleep");
		nameNeuron(lobe, 6, "Approach");
		nameNeuron(lobe, 7, "Ingest");
		nameNeuron(lobe, 8, "Look");
		nameNeuron(lobe, 9, "Smell");
		nameNeuron(lobe, 10, "Attack");
		nameNeuron(lobe, 11, "Play");
		nameNeuron(lobe, 12, "Mate");

		return lobe;
	}

	// TODO: Make this a WTA lobe.
	public NeuronGroup buildNounLobe() {
		NeuronGroup lobe = createLobe(910, -40, 7, "line", "Lobe #4: Nouns");

		nameNeuron(lobe, 0, "Toy");
		nameNeuron(lobe, 1, "Fish");
		nameNeuron(lobe, 2, "Cheese");
		nameNeuron(lobe, 3, "Poison");
		nameNeuron(lobe, 4, "Hazard");
		nameNeuron(lobe, 5, "Flower");
		nameNeuron(lobe, 6, "Mouse");

		return lobe;
	}

	public NeuronGroup buildSensesLobe() {
		NeuronGroup lobe = createLobe(1490, 1550, 14, "grid", "Lobe #5: General Senses");
		setLobeColumns(lobe, 7);

		nameNeuron(lobe, 0, "Attacked");
		nameNeuron(lobe, 1, "Playing");
		nameNeuron(lobe, 2, "User Talked");
		nameNeuron(lobe, 3, "Mouse Talked");
		nameNeuron(lobe, 4, "It Approaches");
		nameNeuron(lobe, 5, "It is Near");
		nameNeuron(lobe, 6, "It Retreats");
		nameNeuron(lobe, 7, "Is Object");
		nameNeuron(lobe, 8, "Is Mouse");
		nameNeuron(lobe, 9, "Is Parent");
		nameNeuron(lobe, 10, "Is Sibling");
		nameNeuron(lobe, 11, "Is Child");
		nameNeuron(lobe, 12, "Opposite Sex");
		nameNeuron(lobe, 13, "Audible Event");

		return lobe;
	}

	public NeuronGroup buildPerceptionLobe(NeuronGroup[] lobes) {
		// Get the sum of all neurons in all incoming lobes.
		int totalSize = 0;
		for (NeuronGroup l : lobes) {
			totalSize += l.size();
		}

		// Build that lobe!
		NeuronGroup perception = createLobe(65, 440, totalSize, "grid", "Lobe #0: Perception");
		setLobeColumns(perception, 7);

		// Label neurons
		int indexPointer = 0;
		for (NeuronGroup l : lobes) {
			copyLabels(l, perception, indexPointer);
			indexPointer += l.size();
		}

		return perception;
	}

	// Accessor methods below this point

	public Network getNetwork() {
		return builder.getNetwork();
	}

	public List<NeuronGroup> getLobeList() {
		return lobes;
	}

	public NetBuilder getBuilder() {
		return builder;
	}

	/**
	 * Returns the label of a neuron of a given lobe.
	 *
	 * @param lobe
	 * @param neuronIndex
	 * @return String
	 */
	public String getNeuronLabel(NeuronGroup lobe, int neuronIndex) {
		return lobe.getNeuronList().get(neuronIndex).getLabel();
	}

}