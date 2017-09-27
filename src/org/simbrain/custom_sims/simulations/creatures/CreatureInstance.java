package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * Each CreatureInstance represents one particular creature in the simulation,
 * and wraps around various other classes and has helper methods for dealing
 * with particular creatures.
 *
 * @author Sharai
 *
 */
public class CreatureInstance {
	
	/**
	 * The name of the creature.
	 */
	private String name;

	/**
	 * The brain belonging to this creature.
	 */
	private CreaturesBrain brain;
	
	/**
	 * The odor world agent belonging to this creature.
	 */
	private RotatingEntity agent;

	public CreatureInstance(String name, NetBuilder net, RotatingEntity agent) {
		this.name = name;
		
		this.brain = new CreaturesBrain(net);
		initDefaultBrain();
		
		this.agent = agent;
		initAgent();
	}

	/**
	 * Update the various components of the creature.
	 */
	public void update() {
		brain.getNetwork().update();
	}
	
	/**
	 * Configure the agent's name, smells, sensors, and effectors.
	 */
	private void initAgent() {
		agent.setName(name);
		agent.setSmellSource(new SmellSource(new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 255.0}));
	}

	/**
	 * Build a brain network from a template.
	 */
	private void initDefaultBrain() {

		// Init non-mutable lobes #1-5
		NeuronGroup drives = brain.buildDriveLobe();
//		NeuronGroup stimulus = brain.buildStimulusLobe();
//		NeuronGroup verbs = brain.buildVerbLobe();
//		brain.buildNounLobe();
//		NeuronGroup senses = brain.buildSensesLobe();
//
//		// Init Lobe #6: Decisions
//		// TODO: Make this a WTA lobe.
//		NeuronGroup decisions = brain.createLobe(2510, 440, verbs.size(), "vertical line", "Lobe #6: Decisions");
//		brain.copyLabels(verbs, decisions);
//
//		// Init Lobe #7: Attention
//		// TODO: Make this a WTA lobe.
//		NeuronGroup attention = brain.createLobe(2090, 1260, stimulus.size(), "vertical line", "Lobe #7: Attention");
//		brain.copyLabels(stimulus, attention);
//
//		// Init Lobe #8: Concepts
//		NeuronGroup concepts = brain.createLobe(460, 95, 640, "grid", "Lobe #8: Concepts");
//		brain.setLobeColumns(concepts, 40);
//
//		// Init Lobe #0: Perception
//		brain.buildPerceptionLobe(new NeuronGroup[] {drives, verbs, senses, attention});

	}

}
