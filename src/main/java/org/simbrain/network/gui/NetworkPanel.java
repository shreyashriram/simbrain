/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui;

import org.piccolo2d.PCamera;
import org.piccolo2d.PCanvas;
import org.piccolo2d.PNode;
import org.piccolo2d.event.PInputEventListener;
import org.piccolo2d.event.PMouseWheelZoomEventHandler;
import org.piccolo2d.util.PBounds;
import org.piccolo2d.util.PPaintContext;
import org.simbrain.network.dl4j.ArrayConnectable;
import org.simbrain.network.dl4j.MultiLayerNet;
import org.simbrain.network.dl4j.NeuronArray;
import org.simbrain.network.dl4j.WeightMatrix;
import org.simbrain.network.gui.dialogs.dl4j.MultiLayerNetCreationDialog;
import org.simbrain.network.gui.nodes.MultiLayerNetworkNode;
import org.simbrain.network.connections.QuickConnectionManager;
import org.simbrain.network.core.*;
import org.simbrain.network.groups.*;
import org.simbrain.network.gui.UndoManager.UndoableAction;
import org.simbrain.network.gui.actions.edit.CopyAction;
import org.simbrain.network.gui.actions.edit.CutAction;
import org.simbrain.network.gui.actions.edit.DeleteAction;
import org.simbrain.network.gui.actions.edit.PasteAction;
import org.simbrain.network.gui.actions.neuronarray.AddNeuronArrayAction;
import org.simbrain.network.gui.actions.neuron.AddNeuronsAction;
import org.simbrain.network.gui.actions.neuron.SetNeuronPropertiesAction;
import org.simbrain.network.gui.actions.synapse.SetSynapsePropertiesAction;
import org.simbrain.network.gui.dialogs.NetworkDialog;
import org.simbrain.network.gui.dialogs.group.NeuronGroupDialog;
import org.simbrain.network.gui.dialogs.group.SynapseGroupDialog;
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog;
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog;
import org.simbrain.network.gui.dialogs.text.TextDialog;
import org.simbrain.network.gui.nodes.*;
import org.simbrain.network.gui.nodes.neuronGroupNodes.CompetitiveGroupNode;
import org.simbrain.network.gui.nodes.neuronGroupNodes.SOMGroupNode;
import org.simbrain.network.gui.nodes.subnetworkNodes.*;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.subnetworks.*;
import org.simbrain.network.util.CopyPaste;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.JMultiLineToolTip;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.genericframe.GenericJDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.EditablePanel;
import org.simbrain.util.widgets.ToggleButton;
import org.simbrain.workspace.AttributeContainer;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Contains a piccolo PCanvas that maintains a visual representation of the network. Creation, deletion, and state
 * update events are received here and piccolo nodes created, deleted, and updated accordingly. Various selection events
 * and and other graphics processing are also handled here.
 * <p>
 * Here are some more details on this class. These are not API notes but rather an overview of the class, which, given
 * its size, is useful to have. Here are some of the main things handled here:
 * <ul>
 * <li>Set up network listeners, for responding to events where neurons,
 * synapses, groups, etc. are added, deleted, updated, or changed.</li>
 * <li>Methods for updating the visible states of GUI elements. Mostly private
 * and called by listeners. See methods beginning "update"...</li>
 * <li>Methods for adding and removing model objects of various kinds to the
 * canvas. This is where the most complex stuff in this class occurs, given the various types of compound objects that
 * must be represented in the Piccolo canvas. See methods beginning "add..".</li>
 * <li>Creation of relevant piccolo objects</li>
 * <li>Methods for managing menus, dealing with selections, copy paste,
 * aligning, spacing, etc., centering the camera, incrementing objects, nudging objects, etc.</li>
 * <li>Convenience methods for returning different collections of objects
 * (selected neurons, synapses, etc.).</li>
 * </ul>
 * <p>
 * <br>
 * <p>
 * Also note that NetworkPanel can be used separately from the Simbrain workspace, e.g. in an applet. Thus all
 * dependencies on workspace classes (e.g. handling coupling menus) are in {@link org.simbrain.network.desktop.NetworkPanelDesktop},
 * which explains some of the methods here that are stubs overridden in that class.
 */
public class NetworkPanel extends JPanel {

    /**
     * The Piccolo PCanvas.
     */
    private final PCanvas canvas;

    /**
     * The model neural-Network object.
     */
    private Network network;

    /**
     * Main splitter pane: network in middle, hierarchy on left.
     */
    private JSplitPane splitPane;

    /**
     * Default edit mode.
     */
    private static final EditMode DEFAULT_BUILD_MODE = EditMode.SELECTION;

    /**
     * Build mode.
     */
    private EditMode editMode;

    /**
     * Selection model.
     */
    private final NetworkSelectionModel selectionModel;

    /**
     * Action manager.
     */
    protected NetworkActionManager actionManager;

    /**
     * Undo manager.
     */
    protected final UndoManager undoManager;

    /**
     * Cached context menu.
     */
    private JPopupMenu contextMenu;

    /**
     * Cached alternate context menu.
     */
    private JPopupMenu contextMenuAlt;

    /**
     * Last selected Neuron.
     */
    private NeuronNode lastSelectedNeuron;

    /**
     * Label which displays current time.
     */
    private TimeLabel timeLabel;

    /**
     * Reference to bottom NetworkPanelToolBar.
     */
    private CustomToolBar southBar;

    /**
     * Show input labels.
     */
    private boolean inOutMode = true;

    /**
     * Use auto zoom.
     */
    private boolean autoZoomMode = true;

    /**
     * Show subnet outline.
     */
    private boolean showSubnetOutline = false;

    /**
     * Show time.
     */
    private boolean showTime = true;

    /**
     * Main tool bar.
     */
    private CustomToolBar mainToolBar;

    /**
     * Run tool bar.
     */
    private CustomToolBar runToolBar;

    /**
     * Edit tool bar.
     */
    private CustomToolBar editToolBar;

    /**
     * Clamp tool bar.
     */
    private CustomToolBar clampToolBar;

    /**
     * Color of background.
     */
    private static Color backgroundColor = Color.white;

    /**
     * How much to nudge objects per key click.
     */
    private static double nudgeAmount = 2;

    /**
     * Source elements (when setting a source node or group and then connecting to a target).
     */
    private List<ScreenElement> sourceElements = new ArrayList<>();

    /**
     * Toggle button for neuron clamping.
     */
    protected JToggleButton neuronClampButton = new JToggleButton();

    /**
     * Toggle button for weight clamping.
     */
    protected JToggleButton synapseClampButton = new JToggleButton();

    /**
     * Menu item for neuron clamping.
     */
    protected JCheckBoxMenuItem neuronClampMenuItem = new JCheckBoxMenuItem();

    /**
     * Menu item for weight clamping.
     */
    protected JCheckBoxMenuItem synapseClampMenuItem = new JCheckBoxMenuItem();

    /**
     * Turn GUI on or off.
     */
    private boolean guiOn = true;

    /**
     * Whether loose synapses are visible or not.
     */
    private boolean looseWeightsVisible = true;

    /**
     * Whether to display update priorities.
     */
    private boolean prioritiesVisible = false;

    /**
     * Whether to display the network hierarchy panel.
     */
    private boolean showNetworkHierarchyPanel;

    /**
     * Text object event handler.
     */
    private TextEventHandler textHandle;

    /**
     * Groups nodes together for ease of use.
     */
    private ViewGroupNode vgn;

    /**
     * Toolbar panel.
     */
    private JPanel toolbars;

    /**
     * Manages keyboard-based connections.
     */
    private final QuickConnectionManager quickConnector = new QuickConnectionManager();

    /**
     * Map associating network model objects with Piccolo Pnodes.
     */
    private final Map<Object, PNode> objectNodeMap = Collections.synchronizedMap(new HashMap<Object, PNode>());

    /**
     * Set to 3 since update neurons, synapses, and groups each decrement it by 1. If 0, update is complete.
     */
    private AtomicInteger updateComplete = new AtomicInteger(0);

    /**
     * Manages placement of new nodes, groups, etc.
     */
    private PlacementManager placementManager = new PlacementManager();;

    //TODO: Make constructor private and just use static creation method?

    /**
     * Create a new Network panel.
     *
     * @param Network the network panel being created.
     */
    public NetworkPanel(final Network Network) {
        super();

        this.network = Network;
        canvas = new PCanvas();

        // Always render in high quality
        canvas.setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        canvas.setAnimatingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
        canvas.setInteractingRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);

        editMode = DEFAULT_BUILD_MODE;
        selectionModel = new NetworkSelectionModel(this);
        actionManager = new NetworkActionManager(this);
        undoManager = new UndoManager();

        createNetworkContextMenu();
        // createContextMenuAlt();

        // Toolbars
        toolbars = new JPanel(new BorderLayout());
        toolbars.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        mainToolBar = this.createMainToolBar();
        runToolBar = this.createRunToolBar();
        editToolBar = this.createEditToolBar();
        FlowLayout flow = new FlowLayout(FlowLayout.LEFT);
        flow.setHgap(0);
        flow.setVgap(0);
        JPanel internalToolbar = new JPanel(flow);
        internalToolbar.add(getMainToolBar());
        internalToolbar.add(getRunToolBar());
        internalToolbar.add(getEditToolBar());
        toolbars.add("Center", internalToolbar);
        super.setLayout(new BorderLayout());
        this.add("North", toolbars);

        // Add the main Canvas
        this.add("Center", canvas);

        // Initialize priority text
        setPrioritiesVisible(prioritiesVisible);

        // Event listeners
        removeDefaultEventListeners();
        canvas.addInputEventListener(new MouseEventHandler(this));
        canvas.addInputEventListener(new ContextMenuEventHandler(this));
        PMouseWheelZoomEventHandler zoomHandler = new PMouseWheelZoomEventHandler();
        zoomHandler.zoomAboutMouse();
        canvas.addInputEventListener(zoomHandler);
        canvas.addInputEventListener(new WandEventHandler(this));
        textHandle = new TextEventHandler(this);
        canvas.addInputEventListener(textHandle);

        // Init network change listeners
        addNetworkListeners();

        // Don't show text when the canvas is sufficiently zoomed in
        PropertyChangeListener zoomListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                for (NeuronNode node : getNodes(NeuronNode.class)) {
                    node.updateTextVisibility();
                }

            }
        };
        canvas.getCamera().addPropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM, zoomListener);

        selectionModel.addSelectionListener(new NetworkSelectionListener() {
            /** @see NetworkSelectionListener */
            public void selectionChanged(final NetworkSelectionEvent e) {
                updateNodeHandles(e);
            }
        });

        // Format the time Label
        timeLabel = new TimeLabel(this);
        // timeLabel.offset(TIME_LABEL_H_OFFSET, canvas.getCamera().getHeight()
        // - TIME_LABEL_V_OFFSET);
        // canvas.getCamera().addChild(timeLabel);
        timeLabel.update();

        JToolBar statusBar = new JToolBar();
        statusBar.add(timeLabel);
        this.add("South", statusBar);

        // Register support for tool tips
        // TODO: might be a memory leak, if not unregistered when the parent
        // frame is removed
        ToolTipManager.sharedInstance().registerComponent(this);

        // Register key support
        KeyBindings.addBindings(this);
        // Must pass action map to splitpane or some key bindings lost
        // splitPane.setActionMap(this.getActionMap());

        // Repaint whenever window is opened or changed.
        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent arg0) {
                repaint();
            }
        });


    }

    /**
     * Register and define all network listeners.
     */
    private void addNetworkListeners() {

        network.addPropertyChangeListener(
                evt -> {
                    if ("neuronAdded".equals(evt.getPropertyName())) {
                        addNeuron((Neuron) evt.getNewValue());
                    } else if ("neuronRemoved".equals(evt.getPropertyName())) {
                        ((Neuron) evt.getOldValue()).fireDeleted();
                    } else if ("neuronsUpdated".equals(evt.getPropertyName())) {
                        if (!isGuiOn()) {
                            return;
                        }
                        NetworkPanel.this.updateNeuronNodes((List<Neuron>) evt.getNewValue());
                    } else if ("synapseAdded".equals(evt.getPropertyName())) {
                        addSynapse((Synapse) evt.getNewValue());
                    } else if ("synapseRemoved".equals(evt.getPropertyName())) {
                        ((Synapse) evt.getOldValue()).fireDeleted();
                    } else if ("textAdded".equals(evt.getPropertyName())) {
                        addTextObject((NetworkTextObject) evt.getNewValue());
                    } else if ("textRemoved".equals(evt.getPropertyName())) {
                        ((NetworkTextObject) evt.getOldValue()).fireDeleted();
                    } else if ("groupAdded".equals(evt.getPropertyName())) {
                        addGroup((Group) evt.getNewValue());
                    } else if ("neuronArrayAdded".equals(evt.getPropertyName())) {
                        addNeuronArray((NeuronArray) evt.getNewValue());
                    } else if ("multiLayerNetworkAdded".equals(evt.getPropertyName())) {
                        addMultiLayerNetwork((MultiLayerNet) evt.getNewValue());
                    } else if ("naRemoved".equals(evt.getPropertyName())) {
                        ((NeuronArray) evt.getOldValue()).fireDeleted();
                    } else if ("wmRemoved".equals(evt.getPropertyName())) {
                        ((WeightMatrix) evt.getOldValue()).fireDeleted();
                    } else if ("ncAdded".equals(evt.getPropertyName())) {
                        addNeuronCollection((NeuronCollection) evt.getNewValue());
                    } else if ("updateTimeDisplay".equals(evt.getPropertyName())) {
                        updateTime();
                    } else if ("updateCompleted".equals(evt.getPropertyName())) {
                        NetworkPanel.this.setUpdateComplete((Boolean) evt.getNewValue());
                        repaint();
                    }
                }
        );

    }

    /**
     * Update visible state of all neurons nodes. This is not used much internally, because it is preferred to updated
     * the specific nodes that need to be updated. It is here mainly for convenience (e.g. for use in scripts).
     */
    private void updateNeuronNodes() {
        // System.out.println("In update neuron nodes");
        for (NeuronNode node : getNodes(NeuronNode.class)) {
            node.update();
        }
        timeLabel.update();
        updateComplete.decrementAndGet();
    }

    /**
     * Update the time label.
     */
    public void updateTime() {
        timeLabel.update();
    }

    /**
     * Update visible state of nodes corresponding to specified neurons.
     *
     * @param neurons the neurons whose corresponding pnode should be updated.
     */
    private void updateNeuronNodes(Collection<Neuron> neurons) {
        // TODO: Refactor or remove this
        for (Neuron neuron : neurons) {
            NeuronNode neuronNode = ((NeuronNode) objectNodeMap.get(neuron));
            if (neuronNode != null) {
                // TODO: Possibly separate color update from position update
                neuronNode.pullViewPositionFromModel();
                neuronNode.update();
            }
        }
        timeLabel.update();
        updateComplete.decrementAndGet();
    }

    /**
     * Update visible state of group nodes.
     *
     * @param groups the group to update
     */
    private void updateGroupNodes(Collection<Group> groups) {
        // System.out.println("In update group node. Updating group " + group);
        for (Group group : groups) {
            PNode groupNode = objectNodeMap.get(group);
            if (groupNode != null) {
                ((GroupNode) groupNode).updateConstituentNodes();
            }
        }
        updateComplete.decrementAndGet();
    }

    /**
     * Update visible state of all synapse nodes. This is not used much internally, because it is preferred to updated
     * the specific nodes that need to be updated. It is here mainly for convenience (e.g. for use in scripts).
     */
    private void updateSynapseNodes() {

        // System.out.println("In update synapse nodes");
        for (SynapseNode node : this.getNodes(SynapseNode.class)) {
            if (node.getVisible()) {
                node.updateColor();
                node.updateDiameter();
            }
        }
        timeLabel.update();
        updateComplete.decrementAndGet();
    }

    /**
     * Update visible state of nodes corresponding to specified synapses.
     *
     * @param synapses the synapses whose corresponding pnodes should be updated.
     */
    private void updateSynapseNodes(Collection<Synapse> synapses) {
        // System.out.println("In update synapse nodes. Updating " +
        // synapses.size() + " synapses");
        for (Synapse synapse : synapses) {
            SynapseNode node = ((SynapseNode) objectNodeMap.get(synapse));
            if (node != null) {
                node.updateColor();
                node.updateDiameter();
            }
        }
        timeLabel.update();
        updateComplete.decrementAndGet();
    }

    /**
     * Use the GUI to add a new neuron to the underlying network model.
     *
     * @param baseRule the neuron update rule to use for the new neuron
     */
    public void addNeuron(final NeuronUpdateRule baseRule) {

        final Neuron neuron = new Neuron(getNetwork(), baseRule);
        Point2D p = placementManager.getLocation();
        neuron.setX(p.getX());
        neuron.setY(p.getY());
        neuron.forceSetActivation(0);
        getNetwork().addLooseNeuron(neuron);

        undoManager.addUndoableAction(new UndoableAction() {

            @Override
            public void undo() {
                getNetwork().removeNeuron(neuron);
                // System.out.println("AddNeuron:undo. Remove "
                // + neuron.getId());
            }

            @Override
            public void redo() {
                getNetwork().addLooseNeuron(neuron);
                // System.out.println("AddNeuron:redo. Add" + neuron.getId());
            }

        });
        repaint();
    }

    /**
     * Remove the indicated neuron from the GUI.
     *
     * @param neuron the model neuron to remove
     */
    private void removeNeuron(Neuron neuron) {
        NeuronNode node = (NeuronNode) objectNodeMap.get(neuron);
        if (node != null) {
            selectionModel.remove(node);
            node.removeFromParent();
            objectNodeMap.remove(neuron);
            // Clean up parent neuron group, if any
            if (neuron.getParentGroup() != null) {
                NeuronGroupNode groupNode = (NeuronGroupNode) objectNodeMap.get(neuron.getParentGroup());
                if (groupNode != null) {
                    groupNode.removeNeuronNode(node);
                }
            }
            zoomToFitPage(false);
        }
    }

    /**
     * Using the GUI to add a set of neurons to the underlying network panel.
     *
     * @param neurons the set of neurons
     * @param layout  the layout to use in adding them
     */
    public void addNeuronsToPanel(final List<Neuron> neurons, final Layout layout) {
        Network net = getNetwork();
        ArrayList<NeuronNode> nodes = new ArrayList<NeuronNode>();
        for (Neuron neuron : neurons) {
            nodes.add(new NeuronNode(this, neuron));
            net.addLooseNeuron(neuron);
        }

        setSelection(nodes);

        layout.setInitialLocation(placementManager.getLocation());

        layout.layoutNeurons(getSelectedModels(Neuron.class));

        network.fireNeuronsUpdated(getSelectedModels(Neuron.class));
        repaint();

    }

    /**
     * Add representation of specified neuron to network panel. Invoked when linking to a neuron that exists in the
     * model.
     */
    private void addNeuron(final Neuron neuron) {
        if (objectNodeMap.get(neuron) != null) {
            return;
        }
        NeuronNode node = new NeuronNode(this, neuron);
        canvas.getLayer().addChild(node);
        objectNodeMap.put(neuron, node);
        selectionModel.setSelection(Collections.singleton(node));
    }

    /**
     * Add representation of specified text to network panel.
     */
    private void addTextObject(final NetworkTextObject text) {
        if (objectNodeMap.get(text) != null) {
            return;
        }
        TextNode node = new TextNode(NetworkPanel.this, text);
        // node.getPStyledText().syncWithDocument();
        canvas.getLayer().addChild(node);
        objectNodeMap.put(text, node);
        // node.update();
        undoManager.addUndoableAction(new UndoableAction() {

            @Override
            public void undo() {
                getNetwork().deleteText(text);
                // System.out.println("AddText:undo. Remove "
                // + text);
            }

            @Override
            public void redo() {
                // getNetwork().addText(text);
                // System.out.println("AddText:redo. Add" + text);
            }

        });
    }

    /**
     * Add GUI representation of specified model synapse to network panel.
     *
     * @param synapse the synapse to add
     */
    public void addSynapse(final Synapse synapse) {
        if (objectNodeMap.get(synapse) != null) {
            return;
        }
        NeuronNode source = (NeuronNode) objectNodeMap.get(synapse.getSource());
        NeuronNode target = (NeuronNode) objectNodeMap.get(synapse.getTarget());
        if ((source == null) || (target == null)) {
            return;
        }

        SynapseNode node = new SynapseNode(NetworkPanel.this, source, target, synapse);

        // TODO: Figure out why code below is needed at all.
        //  NEAT.java is an example of code that creates nodes in this bad state
        // Don't add synpase nodes when they are created in a bad state.
        if (Double.isNaN(node.getGlobalFullBounds().x) || Double.isNaN(node.getGlobalFullBounds().y)) {
            return;
        }
        canvas.getLayer().addChild(node);
        objectNodeMap.put(synapse, node);
        // System.out.println(objectNodeMap.size());

        // TODO: Call below is expensive
        node.lowerToBottom();
    }

    /**
     * Remove any gui representation of the indicated synapse from the canvas.
     *
     * @param synapse the model synapse to remove.
     */
    public void removeSynapse(final Synapse synapse) {
        SynapseNode synapseNode = (SynapseNode) objectNodeMap.get(synapse);
        if (synapseNode != null) {
            selectionModel.remove(synapseNode);
            synapseNode.getTarget().getConnectedSynapses().remove(synapseNode);
            synapseNode.getSource().getConnectedSynapses().remove(synapseNode);
            synapseNode.removeFromParent();
            objectNodeMap.remove(synapse);
            // If synapsenode exists in a visible synapse group node, remove it
            if (synapse.getParentGroup() != null) {
                SynapseGroupNode parentGroupNode = (SynapseGroupNode) objectNodeMap.get(synapse.getParentGroup());
                if (parentGroupNode != null) {
                    if (parentGroupNode instanceof SynapseGroupNodeVisible) {
                        ((SynapseGroupNodeVisible) parentGroupNode).removeSynapseNode(synapseNode);

                    }
                }
            }
        }
    }

    /**
     * Add a model group node representation to the piccolo canvas.
     * <p>
     * Be aware that creation of groups is complex. Parts of the groups can be added in different orders (e.g. a
     * neurongroup inside a subnetwork, or neurons inside a neuron group, etc). These may or may not fire listeners So
     * it is hard to make assumptions about which parts of a group have been added when this method is called. As an
     * example, feedforward networks are created using all-to-all connection objects, which can call addSynpase as they
     * are invoked, so that synapse nodes may are already here when the feed-forward node is created and added.
     *
     * @param group the group to add
     */
    private void addGroup(Group group) {

        // If the object has already been added don't keep going.
        if (objectNodeMap.get(group) != null) {
            return;
        }

        // Add an appropriate piccolo representation based on what type of group
        // it is.
        if (group instanceof NeuronGroup) {
            NeuronGroup ng = (NeuronGroup) group;
            addNeuronGroup(ng);
            //if (ng.isTopLevelGroup()) {
            //    placementManager.update();
            //}
        } else if (group instanceof SynapseGroup) {
            addSynapseGroup((SynapseGroup) group);
        } else if (group instanceof Subnetwork) {
            addSubnetwork((Subnetwork) group);
        }
        clearSelection();
    }

    /**
     * Add a neuron group representation to the canvas.
     *
     * @param neuronGroup the group to add.
     */
    private void addNeuronGroup(NeuronGroup neuronGroup) {

        List<NeuronNode> neuronNodes = new ArrayList<NeuronNode>();

        // Create neuron nodes and add them to the canvas. This is done
        // since the neuron nodes can be interacted with separately from the
        // neuron group they are part of.
        for (Neuron neuron : neuronGroup.getNeuronList()) {
            addNeuron(neuron);
            neuronNodes.add((NeuronNode) objectNodeMap.get(neuron));
        }
        // Create the neuron group node.
        NeuronGroupNode neuronGroupNode = createNeuronGroupNode(neuronGroup);

        // Add the pnodes to the neuron group
        for (NeuronNode node : neuronNodes) {
            neuronGroupNode.addNeuronNode(node);
        }

        // Add neuron group to canvas
        canvas.getLayer().addChild(neuronGroupNode);
        objectNodeMap.put(neuronGroup, neuronGroupNode);

        repaint();
    }

    /**
     * Add a graphical representation of a neuron array to the network
     *
     * @param neuronArray the neuron array
     */
    private void addNeuronArray(NeuronArray neuronArray) {
        NeuronArrayNode nad = new NeuronArrayNode(this, neuronArray);
        canvas.getLayer().addChild(nad);
        objectNodeMap.put(neuronArray, nad);
        repaint();
    }

    private void addMultiLayerNetwork(MultiLayerNet multiLayerNetwork) {
        MultiLayerNetworkNode node = new MultiLayerNetworkNode(this, multiLayerNetwork);
        canvas.getLayer().addChild(node);
        objectNodeMap.put(multiLayerNetwork, node);
        repaint();
    }

    /**
     * Add a representation of a neuron collection to this panel.
     *
     * @param nc the {@link NeuronCollection} to add
     */
    private void addNeuronCollection(NeuronCollection nc) {

        NeuronCollectionNode ncn = new NeuronCollectionNode(this, nc);

        List<NeuronNode> neuronNodes = new ArrayList<NeuronNode>();
        for (Neuron neuron : nc.getNeuronList()) {
            neuronNodes.add((NeuronNode) objectNodeMap.get(neuron));
        }
        for (NeuronNode node : neuronNodes) {
            ncn.addNeuronNode(node);
        }
        canvas.getLayer().addChild(ncn);
        objectNodeMap.put(nc, ncn);
        repaint();
    }

    /**
     * Add a SynapseGroup representation to the canvas. Depending on the whether visibility is turned on, and if not,
     * whether we are dealing with recurrent or bidirectional cases, different types of PNodes are created.
     *
     * @param synapseGroup the synapse group to add
     */
    private void addSynapseGroup(final SynapseGroup synapseGroup) {
        if (synapseGroup.isDisplaySynapses()) {
            addSynapseGroupVisible(synapseGroup);
        } else {
            if (synapseGroup.getTargetNeuronGroup().equals(synapseGroup.getSourceNeuronGroup())) {
                addSynapseGroupRecurrent(synapseGroup);
            } else {

                // Test if there isn't already a synapse group going in
                // the opposite direction. The synapse groups which _originate_
                // from this synapse groups's target neuron group
                Set<SynapseGroup> targetGroupOutgoing = synapseGroup.getTargetNeuronGroup().getOutgoingSg();
                // The synapse groups which _terminate_ at this synapse group's
                // source neuron group
                Set<SynapseGroup> sourceGroupIncoming = synapseGroup.getSourceNeuronGroup().getIncomingSgs();

                // If there exists a synapse group that _originates_ in this
                // synapse group's target neuron group and _terminates_ in this
                // synapse group's source neuron group, then .retainAll between
                // the two above sets will contain that group
                targetGroupOutgoing.retainAll(sourceGroupIncoming);

                if (targetGroupOutgoing.size() != 0) { // There _is_ a synapse
                    // group going in the opposite direction between the same
                    // two neuron groups.
                    final SynapseGroup reverse = (SynapseGroup) targetGroupOutgoing.toArray()[0];
                    if (objectNodeMap.get(reverse) != null) {
                        removeGroup(reverse);
                        addSynapseGroupBidirectional(synapseGroup, reverse);
                    } else {
                        addSynapseGroupSimple(synapseGroup);
                    }
                    return;
                } else {
                    // Not recurrent, no synapse group going in the
                    // opposite direction, and is not displaying individual
                    // synapses...
                    // add group normally
                    addSynapseGroupSimple(synapseGroup);
                }
            }
        }
        SynapseGroupNode synapseGroupNode = (SynapseGroupNode) objectNodeMap.get(synapseGroup);

        // TODO: Clean up listeners if the synapsegroup is removed.
        NeuronGroupNode srcNode = (NeuronGroupNode) objectNodeMap.get(synapseGroup.getSourceNeuronGroup());
        // System.out.println("Source" + srcNode);
        if (srcNode != null) {
            srcNode.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS, synapseGroupNode);
        }
        NeuronGroupNode tarNode = (NeuronGroupNode) objectNodeMap.get(synapseGroup.getTargetNeuronGroup());
        // System.out.println("Target" + tarNode);
        if (tarNode != null) {
            tarNode.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS, synapseGroupNode);
        }

    }

    /**
     * Add a synapse group representation for case where all constituent synapses are visible.
     *
     * @param synapseGroup the model synapse group being represented
     */
    private void addSynapseGroupVisible(SynapseGroup synapseGroup) {
        // List of neuron and synapse nodes
        List<SynapseNode> nodes = new ArrayList<SynapseNode>();
        // Add excitatory synapse nodes to canvas
        for (Synapse synapse : synapseGroup.getExcitatorySynapses()) {
            addSynapse(synapse);
            SynapseNode node = (SynapseNode) objectNodeMap.get(synapse);
            if (node != null) {
                canvas.getLayer().addChild(node);
                nodes.add(node);
            }
        }
        // Add inhibitory synapse nodes to canvas
        for (Synapse synapse : synapseGroup.getInhibitorySynapses()) {
            addSynapse(synapse);
            SynapseNode node = (SynapseNode) objectNodeMap.get(synapse);
            if (node != null) {
                canvas.getLayer().addChild(node);
                nodes.add(node);
            }
        }
        // Add synapse nodes to group node
        SynapseGroupNodeVisible synapseGroupNode = new SynapseGroupNodeVisible(this, synapseGroup);
        canvas.getLayer().addChild(synapseGroupNode);
        objectNodeMap.put(synapseGroup, synapseGroupNode);

        // Make this a child node of parent, if any
        if (synapseGroup.hasParentGroup()) {
            SubnetworkNode parentNode = (SubnetworkNode) objectNodeMap.get(synapseGroup.getParentGroup());
            if (parentNode != null) {
                parentNode.addNode(synapseGroupNode);
            }
        }

        // Add the synapse nodes to the synapse group node
        for (SynapseNode node : nodes) {
            synapseGroupNode.addSynapseNode(node);
        }
        synapseGroupNode.lowerToBottom();
    }

    /**
     * Add a "simple" synapse group representation, in which the constituent synapses are not visible, and are
     * non-recurrent.
     *
     * @param synapseGroup the model synapse group being represented
     */
    private void addSynapseGroupSimple(SynapseGroup synapseGroup) {
        // System.out.println("Add invisible synapse group");
        SynapseGroupNodeSimple synapseGroupNode = new SynapseGroupNodeSimple(this, synapseGroup);
        canvas.getLayer().addChild(synapseGroupNode);
        objectNodeMap.put(synapseGroup, synapseGroupNode);
        // Make this a child node of parent, if any
        if (synapseGroup.hasParentGroup()) {
            SubnetworkNode parentNode = (SubnetworkNode) objectNodeMap.get(synapseGroup.getParentGroup());
            if (parentNode != null) {
                parentNode.addNode(synapseGroupNode);
            }
        }
    }

    /**
     * Add a bidirectional synapse group representation. This is not logically different than two simple synapse groups,
     * but the case is represented by a different PNode object.
     *
     * @param sg1 synapse group 1
     * @param sg2 synapse group 2
     */
    private void addSynapseGroupBidirectional(SynapseGroup sg1, SynapseGroup sg2) {
        SynapseGroupNodeBidirectional synGBD = SynapseGroupNodeBidirectional.createBidirectionalSynapseGN(this, sg1, sg2);
        canvas.getLayer().addChild(synGBD);
        objectNodeMap.put(sg1, synGBD);
        objectNodeMap.put(sg2, synGBD);
        NeuronGroupNode srcNode = (NeuronGroupNode) objectNodeMap.get(sg1.getSourceNeuronGroup());
        if (srcNode != null) {
            srcNode.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS, synGBD);
        }
        NeuronGroupNode tarNode = (NeuronGroupNode) objectNodeMap.get(sg1.getTargetNeuronGroup());
        // System.out.println("Target" + tarNode);
        if (tarNode != null) {
            tarNode.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS, synGBD);
        }

        // Bidirectional groups do not currently exist in any subnetworks
        // so the "parent check" is not done
    }

    /**
     * Add a recurrent synapse group representation to the canvas
     *
     * @param sg the recurrent synapse group
     */
    private void addSynapseGroupRecurrent(SynapseGroup sg) {
        SynapseGroupNodeRecurrent synGNR = SynapseGroupNodeRecurrent.createRecurrentSynapseGN(this, sg);
        objectNodeMap.put(sg, synGNR);
        canvas.getLayer().addChild(synGNR);
        NeuronGroupNode srcNode = (NeuronGroupNode) objectNodeMap.get(sg.getSourceNeuronGroup());
        if (srcNode != null) {
            srcNode.addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS, synGNR);
        }
        // Make this a child node of parent, if any
        if (sg.hasParentGroup()) {
            SubnetworkNode parentNode = (SubnetworkNode) objectNodeMap.get(sg.getParentGroup());
            if (parentNode != null) {
                parentNode.addNode(synGNR);
            }
        }
    }

    /**
     * Add a subnetwork representation to the canvas.
     *
     * @param subnet the group to add.
     */
    private void addSubnetwork(Subnetwork subnet) {

        Set<PNode> nodes = new HashSet<PNode>();
        // Add neuron groups
        for (NeuronGroup neuronGroup : ((Subnetwork) subnet).getNeuronGroupList()) {
            addGroup(neuronGroup);
            NeuronGroupNode neuronGroupNode = (NeuronGroupNode) objectNodeMap.get(neuronGroup);
            nodes.add(neuronGroupNode);
        }

        // Update neuron positions. Must do this before synapse groups so neurons
        // are properly positioned
        network.fireNeuronsUpdated(subnet.getFlatNeuronList());

        // Add synapse groups
        for (SynapseGroup synapseGroup : ((Subnetwork) subnet).getSynapseGroupList()) {
            addSynapseGroup(synapseGroup);
        }
        for (SynapseGroup synapseGroup : ((Subnetwork) subnet).getSynapseGroupList()) {
            PNode synapseGroupNode = objectNodeMap.get(synapseGroup);
            nodes.add(synapseGroupNode);
        }

        // Add neuron and synapse group nodes to subnetwork node
        SubnetworkNode subnetNode = createSubnetworkNode(subnet);
        for (PNode node : nodes) {
            subnetNode.addNode(node);
        }

        // Add subnetwork node to canvas
        canvas.getLayer().addChild(subnetNode);
        objectNodeMap.put(subnet, subnetNode);

        subnet.fireLabelUpdated();

        // Update canvas
        repaint();
    }

    /**
     * Removes a group from the network panel, but not necessarily the network model.
     *
     * @param group the group to remove
     */
    private void removeGroup(Group group) {
        PNode node = objectNodeMap.get(group);
        if (node != null) {
            if (node instanceof GroupNode) {
                for (InteractionBox box : ((GroupNode) node).getInteractionBoxes()) {
                    // TODO: property listener list is not visible in pnode,
                    // so have not tested to make sure this cleanup is working
                    canvas.getCamera().removePropertyChangeListener(box.getZoomListener());
                }
            }
            node.removeFromParent();
            objectNodeMap.remove(group);
            // If this node is a child of a parent group, remove it from the
            // parent group
            if (!group.isTopLevelGroup()) {
                PNode parentGroupNode = objectNodeMap.get(group.getParentGroup());
                if (parentGroupNode != null) {
                    if (parentGroupNode instanceof SubnetworkNode) {
                        ((SubnetworkNode) parentGroupNode).getOutlinedObjects().removeChild(node);
                    }
                }
            }
        }
        zoomToFitPage(false);
    }

    /**
     * Remove all synapse group nodes associated with a synapse group. Used when toggling the visibility of synapses in
     * a synapse group node.
     *
     * @param group the synapse group whose synapses should be removed
     */
    private void removeSynapseGroupNodes(SynapseGroup group) {
        SynapseNode node;
        for (Synapse synapse : group.getExcitatorySynapses()) {
            node = (SynapseNode) objectNodeMap.get(synapse);
            if (node != null) {
                selectionModel.remove(node);
                objectNodeMap.remove(synapse);
                node.removeFromParent();
            }
        }
        for (Synapse synapse : group.getInhibitorySynapses()) {
            node = (SynapseNode) objectNodeMap.get(synapse);
            if (node != null) {
                selectionModel.remove(node);
                objectNodeMap.remove(synapse);
                node.removeFromParent();
            }
        }
        repaint();
    }

    /**
     * Toggle the visibility of synapses in a synapsegroup. Based the status of the displaySynpases flag in the model
     * synapseGroup, either a visible or invisible synapse group node is created and added to the canvas.
     *
     * @param synapseGroup the synapse group whose visibility should be toggled.
     */
    public void toggleSynapseVisibility(SynapseGroup synapseGroup) {

        // Remove existing synapsegroup nodes and synapsenodes
        removeGroup(synapseGroup);
        removeSynapseGroupNodes(synapseGroup);

        addSynapseGroup(synapseGroup);

        // System.out.println("Number of pnodes:"
        // + this.getCanvas().getLayer().getChildrenCount());
        // `System.out.println("Size of objectNodeMap:" + objectNodeMap.size());
    }

    /**
     * Create the type of NeuronGroupNode associated with the type of the group.
     *
     * @param neuronGroup the neuron group to create a piccolo node for
     * @return the node
     */
    private NeuronGroupNode createNeuronGroupNode(NeuronGroup neuronGroup) {
        NeuronGroupNode ret;
        if (neuronGroup instanceof SOMGroup) {
            ret = new SOMGroupNode(NetworkPanel.this, (SOMGroup) neuronGroup);
        } else if (neuronGroup instanceof CompetitiveGroup) {
            ret = new CompetitiveGroupNode(NetworkPanel.this, (CompetitiveGroup) neuronGroup);
        } else {
            ret = new NeuronGroupNode(this, neuronGroup);
        }
        return ret;
    }

    /**
     * Create an instance of a subnetwork node.
     *
     * @param subnet the model subnetwork
     * @return the PNode represention of the subnetwork.
     */
    private SubnetworkNode createSubnetworkNode(Subnetwork subnet) {
        SubnetworkNode subnetNode = null;
        if (subnet instanceof Hopfield) {
            subnetNode = new HopfieldNode(NetworkPanel.this, (Hopfield) subnet);
        } else if (subnet instanceof CompetitiveNetwork) {
            subnetNode = new CompetitiveNetworkNode(NetworkPanel.this, (CompetitiveNetwork) subnet);
        } else if (subnet instanceof SOMNetwork) {
            subnetNode = new SOMNetworkNode(NetworkPanel.this, (SOMNetwork) subnet);
        } else if (subnet instanceof EchoStateNetwork) {
            subnetNode = new ESNNetworkNode(NetworkPanel.this, (EchoStateNetwork) subnet);
        } else if (subnet instanceof SimpleRecurrentNetwork) {
            subnetNode = new SRNNetworkNode(NetworkPanel.this, (SimpleRecurrentNetwork) subnet);
        } else if (subnet instanceof FeedForward) {
            if (subnet instanceof BackpropNetwork) {
                subnetNode = new BackpropNetworkNode(NetworkPanel.this, (BackpropNetwork) subnet);
            } else if (subnet instanceof LMSNetwork) {
                subnetNode = new LMSNetworkNode(NetworkPanel.this, (LMSNetwork) subnet);
            } else if (subnet instanceof BPTTNetwork) {
                subnetNode = new BPTTNode(NetworkPanel.this, (BPTTNetwork) subnet);
            } else {
                subnetNode = new SubnetworkNode(NetworkPanel.this, subnet);
            }
        } else {
            subnetNode = new SubnetworkNode(NetworkPanel.this, subnet);
        }

        return subnetNode;
    }

    /**
     * Create a new context menu for this Network panel.
     *
     * @return the newly constructed context menu
     */
    public JPopupMenu createNetworkContextMenu() {

        contextMenu = new JPopupMenu();

        // Insert actions
        contextMenu.add(actionManager.getNewNeuronAction());
        contextMenu.add(new AddNeuronsAction(this));
        contextMenu.add(new AddNeuronArrayAction(this));
        contextMenu.add(actionManager.getNewGroupMenu());
        contextMenu.add(actionManager.getNewNetworkMenu());

        // Clipboard actions
        contextMenu.addSeparator();
        for (Action action : actionManager.getClipboardActions()) {
            contextMenu.add(action);
        }
        contextMenu.addSeparator();

        // Connection actions
        contextMenu.add(actionManager.getClearSourceNeuronsAction());
        contextMenu.add(actionManager.getSetSourceNeuronsAction());
        contextMenu.addSeparator();

        // Preferences
        contextMenu.add(actionManager.getShowNetworkPreferencesAction());

        return contextMenu;
    }

    /**
     * Return the context menu for this Network panel.
     * <p>
     * This context menu should return actions that are appropriate for the Network panel as a whole, e.g. actions that
     * change modes, actions that operate on the selection, actions that add new components, etc. Actions specific to a
     * node of interest should be built into a node-specific context menu.
     * </p>
     *
     * @return the context menu for this Network panel
     */
    JPopupMenu getContextMenu() {
        return contextMenu;
    }

    /**
     * Create the iteration tool bar.
     *
     * @return the toolbar.
     */
    protected CustomToolBar createRunToolBar() {

        CustomToolBar runTools = new CustomToolBar();

        runTools.add(actionManager.getIterateNetworkAction());
        runTools.add(new ToggleButton(actionManager.getNetworkControlActions()));

        return runTools;
    }

    /**
     * Create the main tool bar.
     *
     * @return the toolbar.
     */
    protected CustomToolBar createMainToolBar() {

        CustomToolBar mainTools = new CustomToolBar();

        for (Action action : actionManager.getNetworkModeActions()) {
            mainTools.add(action);
        }

        mainTools.addSeparator();
        mainTools.add(actionManager.getSetAutoZoomToggleButton());

        return mainTools;
    }

    /**
     * Create the edit tool bar.
     *
     * @return the toolbar.
     */
    protected CustomToolBar createEditToolBar() {

        CustomToolBar editTools = new CustomToolBar();

        for (Action action : actionManager.getNetworkEditingActions()) {
            editTools.add(action);
        }
        editTools.add(actionManager.getClearNodesAction());
        editTools.add(actionManager.getRandomizeObjectsAction());

        return editTools;
    }

    /**
     * Return the align sub menu.
     *
     * @return the align sub menu
     */
    public JMenu createAlignMenu() {

        JMenu alignSubMenu = new JMenu("Align");
        alignSubMenu.add(actionManager.getAlignHorizontalAction());
        alignSubMenu.add(actionManager.getAlignVerticalAction());
        return alignSubMenu;

    }

    /**
     * Return the space sub menu.
     *
     * @return the space sub menu
     */
    public JMenu createSpacingMenu() {

        JMenu spaceSubMenu = new JMenu("Space");
        spaceSubMenu.add(actionManager.getSpaceHorizontalAction());
        spaceSubMenu.add(actionManager.getSpaceVerticalAction());
        return spaceSubMenu;

    }

    /**
     * Remove the default event listeners.
     */
    private void removeDefaultEventListeners() {
        PInputEventListener panEventHandler = canvas.getPanEventHandler();
        PInputEventListener zoomEventHandler = canvas.getZoomEventHandler();
        canvas.removeInputEventListener(panEventHandler);
        canvas.removeInputEventListener(zoomEventHandler);
    }

    /**
     * Return the current edit mode for this Network panel.
     *
     * @return the current edit mode for this Network panel
     */
    public EditMode getEditMode() {
        return editMode;
    }

    /**
     * Set the current edit mode for this Network panel to <code>editMode</code> .
     * <p>
     * This is a bound property.
     * </p>
     *
     * @param newEditMode edit mode for this Network panel, must not be null
     */
    public void setEditMode(final EditMode newEditMode) {

        if (newEditMode == null) {
            throw new IllegalArgumentException("editMode must not be null");
        }

        EditMode oldEditMode = this.editMode;
        this.editMode = newEditMode;
        if (editMode == EditMode.WAND) {
            editMode.resetWandCursor();
        }
        firePropertyChange("editMode", oldEditMode, this.editMode);
        updateCursor();
        repaint();
    }

    /**
     * Update the cursor.
     */
    public void updateCursor() {
        setCursor(this.editMode.getCursor());
    }

    /**
     * Delete selected items.
     */
    public void deleteSelectedObjects() {
        for (PNode selectedNode : getSelectedNodes()) {
            if (selectedNode instanceof NeuronNode) {
                NeuronNode selectedNeuronNode = (NeuronNode) selectedNode;
                final Neuron neuron = selectedNeuronNode.getNeuron();
                // TODO: Refactor events.  Added the flag below but there is currently
                // no way to update the network after removing all the neurons.
                network.removeNeuron(neuron, true);
            } else if (selectedNode instanceof SynapseNode) {
                SynapseNode selectedSynapseNode = (SynapseNode) selectedNode;
                network.removeSynapse(selectedSynapseNode.getSynapse());
            } else if (selectedNode instanceof NeuronArrayNode) {
                NeuronArrayNode arrayNode = (NeuronArrayNode) selectedNode;
                network.removeNeuronArray(arrayNode.getNeuronArray());
            } else if (selectedNode instanceof WeightMatrixNode) {
                WeightMatrixNode wmNode = (WeightMatrixNode) selectedNode;
                network.removeWeightMatrix(wmNode.getWeightMatrix());
            } else if (selectedNode instanceof TextNode) {
                TextNode selectedTextNode = (TextNode) selectedNode;
                network.deleteText(selectedTextNode.getTextObject());
            } else if (selectedNode instanceof InteractionBox) {
                if (selectedNode.getParent() instanceof NeuronGroupNode) {
                    network.removeGroup(((NeuronGroupNode) selectedNode.getParent()).getNeuronGroup());
                } else if (selectedNode.getParent() instanceof SynapseGroupNode) {
                    network.removeGroup(((SynapseGroupNode) selectedNode.getParent()).getSynapseGroup());
                } else if (selectedNode.getParent() instanceof SubnetworkNode) {
                    network.removeGroup(((SubnetworkNode) selectedNode.getParent()).getSubnetwork());
                }
            }
        }
        // Zoom events are costly so only zoom after main deletion events
        zoomToFitPage(true);

        // undoManager.addUndoableAction(new UndoableAction() {
        //
        // @Override
        // public void undo() {
        // for (Object object : deletedObjects) {
        // if (object instanceof Neuron) {
        // network.addLooseNeuron((Neuron) object);
        // } else if (object instanceof NetworkTextObject) {
        // network.addText((NetworkTextObject) object);
        // }
        // }
        // //
        // System.out.println("Delete Selected Objects:undo - Add those
        // objects");
        // }
        //
        // @Override
        // public void redo() {
        // for (Object object : deletedObjects) {
        // if (object instanceof Neuron) {
        // network.removeNeuron((Neuron) object);
        // } else if (object instanceof NetworkTextObject) {
        // network.deleteText((NetworkTextObject) object);
        // }
        // }
        // //
        // System.out.println("Delete Selected Objects:redo - Re-Remove those
        // objects");
        // }
        //
        // });

    }

    /**
     * Copy to the clipboard.
     */
    public void copy() {
        if(getSelectedModels().isEmpty()) {
            return;
        }
        Clipboard.clear();
        placementManager.setAnchorPoint(SimnetUtils.getUpperLeft(getSelectedModels()));
        ArrayList deepCopy = CopyPaste.getCopy(this.getNetwork(), getSelectedModels());
        Clipboard.add(deepCopy);
    }

    /**
     * Cut to the clipboard.
     */
    public void cut() {
        copy();
        deleteSelectedObjects();
    }

    /**
     * Paste from the clipboard.
     */
    public void paste() {
        Clipboard.paste(this);
    }

    /**
     * Duplicates selected objects.
     */
    public void duplicate() {
        if(getSelectedModels().isEmpty()) {
            return;
        }
        copy();
        paste();
    }

    /**
     * Aligns neurons horizontally.
     */
    public void alignHorizontal() {
        double min = Double.MAX_VALUE;
        List<Neuron> selectedNeurons = getSelectedModels(Neuron.class);
        for (Neuron neuron : selectedNeurons) {
            if (neuron.getY() < min) {
                min = neuron.getY();
            }
        }
        for (Neuron neuron : selectedNeurons) {
            neuron.setY(min);
        }
        repaint();
    }

    /**
     * Aligns neurons vertically.
     */
    public void alignVertical() {

        double min = Double.MAX_VALUE;

        List<Neuron> selectedNeurons = getSelectedModels(Neuron.class);

        for (Neuron neuron : selectedNeurons) {
            if (neuron.getX() < min) {
                min = neuron.getX();
            }
        }
        for (Neuron neuron : selectedNeurons) {
            neuron.setX(min);
        }
        repaint();
    }

    /**
     * TODO: Push this and related methods to model? Spaces neurons horizontally.
     */
    public void spaceHorizontal() {
        if (getSelectedNodes(NeuronNode.class).size() <= 1) {
            return;
        }
        List<Neuron> sortedNeurons = getSelectedModels(Neuron.class);
        sortedNeurons.sort(new NeuronComparator(NeuronComparator.Type.COMPARE_X));

        double min = sortedNeurons.get(0).getX();
        double max = (sortedNeurons.get(sortedNeurons.size() - 1)).getX();
        double space = (max - min) / (sortedNeurons.size() - 1);

        int i = 0;
        for (Neuron neuron : sortedNeurons) {
            neuron.setX(min + space * i);
            i++;
        }

        repaint();
    }

    /**
     * Spaces neurons vertically.
     */
    public void spaceVertical() {
        if (getSelectedNodes(NeuronNode.class).size() <= 1) {
            return;
        }
        List<Neuron> sortedNeurons = getSelectedModels(Neuron.class);
        Collections.sort(sortedNeurons, new NeuronComparator(NeuronComparator.Type.COMPARE_Y));

        double min = sortedNeurons.get(0).getY();
        double max = (sortedNeurons.get(sortedNeurons.size() - 1)).getY();
        double space = (max - min) / (sortedNeurons.size() - 1);

        int i = 0;
        for (Neuron neuron : sortedNeurons) {
            neuron.setY(min + space * i);
            i++;
        }

        repaint();
    }

    /**
     * Creates and displays the neuron properties dialog.
     */
    public void showSelectedNeuronProperties() {
        NeuronDialog dialog = new NeuronDialog(getSelectedModels(Neuron.class));
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Creates and displays the neuron array properties dialog.
     */
    public void showSelectedNeuronArrayProperties() {
        StandardDialog dialog = new AnnotatedPropertyEditor(getSelectedModels(NeuronArray.class)).getDialog();
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Creates and displays the synapse properties dialog.
     */
    public void showSelectedSynapseProperties() {
        SynapseDialog dialog = SynapseDialog.createSynapseDialog(this.getSelectedModels(Synapse.class));
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

    }

    /**
     * Creates and displays the text properties dialog.
     */
    public void showTextPropertyDialog() {
        TextDialog dialog = new TextDialog(getSelectedNodes(TextNode.class));
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    /**
     * Clear the selection.
     */
    public void clearSelection() {
        selectionModel.clear();
    }

    /**
     * Select all elements.
     */
    public void selectAll() {
        setSelection(getSelectableNodes());
    }

    /**
     * Return true if the selection is empty.
     *
     * @return true if the selection is empty
     */
    public boolean isSelectionEmpty() {
        return selectionModel.isEmpty();
    }

    /**
     * Return true if the specified element is selected.
     *
     * @param element element
     * @return true if the specified element is selected
     */
    public boolean isSelected(final Object element) {
        return selectionModel.isSelected(element);
    }

    /**
     * Return the selection as a collection of selected screen elements.
     *
     * @return the selection as a collection of selected screen elements
     */
    @SuppressWarnings("unchecked")
    public List<ScreenElement> getSelectedNodes() {
        Collection<PNode> nodes = selectionModel.getSelection();
        return nodes.stream()
                .filter(ScreenElement.class::isInstance)
                .map(ScreenElement.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Set the selection to the specified collection of elements.
     *
     * @param elements elements
     */
    public void setSelection(final Collection elements) {
        selectionModel.setSelection(elements);
    }

    /**
     * Toggle the selected state of the specified element; if it is selected, remove it from the selection, if it is not
     * selected, add it to the selection.
     *
     * @param element element
     */
    public void toggleSelection(final Object element) {
        if (isSelected(element)) {
            selectionModel.remove(element);
        } else {
            selectionModel.add(element);
        }
    }

    /**
     * Add objects to the current selection.
     *
     * @param element the element to "select."
     */
    public void addSelection(final Object element) {
        selectionModel.add(element);
    }

    /**
     * Add the specified Network selection listener.
     *
     * @param l Network selection listener to add
     */
    public void addSelectionListener(final NetworkSelectionListener l) {
        selectionModel.addSelectionListener(l);
    }

    /**
     * Remove the specified Network selection listener.
     *
     * @param l Network selection listener to remove
     */
    public void removeSelectionListener(final NetworkSelectionListener l) {
        selectionModel.removeSelectionListener(l);
    }

    /**
     * Update selection handles.
     *
     * @param event the NetworkSelectionEvent
     */
    private void updateNodeHandles(final NetworkSelectionEvent event) {

        Set<PNode> selection = event.getSelection();
        Set<PNode> oldSelection = event.getOldSelection();

        Set<PNode> difference = new HashSet<>(oldSelection);
        difference.removeAll(selection);

        for (PNode node : difference) {
            SwingUtilities.invokeLater(() -> NodeHandle.removeSelectionHandleFrom(node));
        }
        for (PNode node : selection) {
            if (node instanceof ScreenElement) {
                ScreenElement screenElement = (ScreenElement) node;
                if (screenElement.showNodeHandle()) {
                    if (screenElement instanceof InteractionBox) {
                        SwingUtilities.invokeLater(
                                () -> NodeHandle.addSelectionHandleTo(node, NodeHandle.INTERACTION_BOX_SELECTION_STYLE)
                        );
                    } else {
                        SwingUtilities.invokeLater(() -> NodeHandle.addSelectionHandleTo(node));
                    }
                }
            }
        }
    }
    
    /**
     * Create a new weight matrix from a source and target.
     *
     * @param source the source of the connection
     * @param target the target of the connection
     */
    public void addWeightMatrix(ArrayConnectable source, ArrayConnectable target) {
        WeightMatrix matrix = network.addWeightMatrix(source, target);
        if (objectNodeMap.get(matrix) != null) {
            return;
        }
        WeightMatrixNode node = new WeightMatrixNode(this, matrix);
        canvas.getLayer().addChild(node);
        node.lowerToBottom();
        objectNodeMap.put(matrix, node);
    }

    /**
     * Add a weight matrix between neuron collections or arrays.
     */
    public void addWeightMatricesFromSelection() {
        List<ArrayConnectable> sources = getSourceModels(ArrayConnectable.class);
        List<ArrayConnectable> targets = getSelectedModels(ArrayConnectable.class);
        for (ArrayConnectable source : sources) {
            for (ArrayConnectable target : targets) {
                if (source instanceof NeuronCollection && target instanceof NeuronCollection) {
                    continue;
                }
                addWeightMatrix(source, target);
            }
        }
    }


    /**
     * Returns model Network elements corresponding to selected screen elements.
     *
     * @return list of selected model elements
     */
    public List<Object> getSelectedModels() {
        return getSelectedNodes().stream()
                .filter(Objects::nonNull)
                .map(ScreenElement::getModel)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of selected network element models.
     * @param cls the class of the model
     * @return the list of selected network element models
     */
    public <T> List<T> getSelectedModels(Class<T> cls) {
        return getSelectedModels().stream()
                .filter(cls::isInstance)
                .map(cls::cast)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of network element nodes.
     * @param cls the class of the node
     */
    public <T extends PNode> List<T> getSelectedNodes(Class<T> cls) {
        return getSelectedNodes().stream()
                .map(ScreenElement::getNode)
                .filter(Objects::nonNull)  // TODO: remove. here because of synapse interaction box
                .filter(cls::isInstance)
                .map(cls::cast)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of network element nodes of a specific type.
     * @param cls the class of the type of node to get
     * @return the list of network element nodes.
     */
    @SuppressWarnings("unchecked")
    public <T extends PNode> List<T> getNodes(Class<T> cls) {
        Collection<PNode> nodes = canvas.getLayer().getAllNodes();
        return nodes.stream()
                .filter(cls::isInstance)
                .map(cls::cast)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of all network elements that are marked as source
     */
    public List<Object> getSourceModels() {
        return sourceElements.stream()
                .map(ScreenElement::getModel)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of network elements model of a specific type.
     * @param cls the class of the type of node to get
     */
    public <T> List<T> getSourceModels(Class<T> cls) {
        return getSourceModels().stream()
                .filter(cls::isInstance)
                .map(cls::cast)
                .collect(Collectors.toList());
    }


    /**
     * Return a collection of all selectable {@link ScreenElement}
     */
    public List<ScreenElement> getSelectableNodes() {
        return getNodes(ScreenElement.class).stream()
                .filter(ScreenElement::isSelectable)
                .collect(Collectors.toList());
    }

    /**
     * Set selected elements to be source elements (with red rectangles around them).
     */
    public void setSourceElements() {
        clearSourceElements();
        for (NeuronNode node : this.getSelectedNodes(NeuronNode.class)) {
            sourceElements.add(node);
            NodeHandle.addSourceHandleTo(node);
        }
        for (NeuronGroupNode node : this.getSelectedNodes(NeuronGroupNode.class)) {
            sourceElements.add(node.getInteractionBox());
            NodeHandle.addSourceHandleTo(node.getInteractionBox(), NodeHandle.INTERACTION_BOX_SOURCE_STYLE);
        }
        for (NeuronCollectionNode node : getSelectedNodes(NeuronCollectionNode.class)) {
            sourceElements.add(node.getInteractionBox());
            NodeHandle.addSourceHandleTo(node.getInteractionBox(), NodeHandle.INTERACTION_BOX_SOURCE_STYLE);
        }
        for (NeuronArrayNode node : getSelectedNodes(NeuronArrayNode.class)) {
            sourceElements.add(node);
            NodeHandle.addSourceHandleTo(node);
        }
        for (MultiLayerNetworkNode node : getSelectedNodes(MultiLayerNetworkNode.class)) {
            sourceElements.add(node);
            NodeHandle.addSourceHandleTo(node);
        }
        selectionModel.fireSelectionChanged();
    }


    /**
     * Called by Network preferences as preferences are changed. Iterates through screen elements and resets relevant
     * colors.
     */
    public void resetColors() {
        canvas.setBackground(backgroundColor);
        for (Object obj : canvas.getLayer().getChildrenReference()) {
            if (obj instanceof ScreenElement) {
                ((ScreenElement) obj).resetColors();
            }
        }
        repaint();
    }

    /**
     * Called by Network preferences as preferences are changed. Iterates through screen elemenets and resets relevant
     * colors.
     */
    public void resetSynapseDiameters() {
        for (SynapseNode synapse : this.getNodes(SynapseNode.class)) {
            synapse.updateDiameter();
        }
        repaint();
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public String debugString() {
        String ret = "";
        Iterator it = objectNodeMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            if (pairs.getKey() instanceof Neuron) {
                ret = ret + ((Neuron) pairs.getKey()).getId() + " --> " + pairs.getValue();
            } else if (pairs.getKey() instanceof Synapse) {
                ret = ret + ((Synapse) pairs.getKey()).getId() + " --> " + pairs.getValue();
            } else if (pairs.getKey() instanceof Group) {
                ret = ret + ((Group) pairs.getKey()).getId() + " --> " + pairs.getValue();
            }
        }
        return ret;
    }

    /**
     * @return Returns the Network.
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * Set Root network.
     *
     * @param network The Network to set.
     */
    public void setNetwork(final Network network) {
        this.network = network;
    }



    /**
     * Rescales the camera so that all objects in the canvas can be seen. Compare "zoom to fit page" in draw programs.
     *
     * @param forceZoom if true force the zoom to happen
     */
    public void zoomToFitPage(boolean forceZoom) {
        PCamera camera = canvas.getCamera();

        // TODO: Add a check to see if network is running
        if ((autoZoomMode && editMode.isSelection()) || forceZoom) {
            PBounds filtered = canvas.getLayer().getUnionOfChildrenBounds(null);
            PBounds adjustedFiltered = new PBounds(filtered.getX() - 10, filtered.getY() - 10, filtered.getWidth() + 20, filtered.getHeight() + 20);
            camera.setViewBounds(adjustedFiltered);
        }
    }

    /**
     * Synchronize model and view.
     */
    public void syncToModel() {
        for (Neuron neuron : network.getNeuronList()) {
            addNeuron(neuron);
        }
        for (Group group : network.getGroupList()) {
            addGroup(group);
        }
        // Synapses must be added _after_ groups are added so that all neurons
        // in groups are
        // in place.
        for (Synapse synapse : network.getSynapseList()) {
            addSynapse(synapse);
        }
        for (NeuronCollection nc : network.getNeuronCollectionSet()) {
            addNeuronCollection(nc);
        }
        for (NetworkTextObject text : network.getTextList()) {
            addTextObject(text);
        }
    }

    /**
     * Find the upper left corner of the subnet nodes.
     *
     * @param neuronList the set of neurons to check
     * @return the upper left corner
     */
    private Point2D getUpperLeft(final ArrayList neuronList) {
        double x = Double.MAX_VALUE;
        double y = Double.MAX_VALUE;
        for (Iterator neurons = neuronList.iterator(); neurons.hasNext(); ) {
            NeuronNode neuronNode = (NeuronNode) neurons.next();
            if (neuronNode.getNeuron().getX() < x) {
                x = neuronNode.getNeuron().getX();
            }
            if (neuronNode.getNeuron().getY() < y) {
                y = neuronNode.getNeuron().getY();
            }
        }
        return new Point2D.Double(x, y);
    }

    /**
     * Ungroup specified object.
     *
     * @param vgn                the group to remove.
     * @param selectConstituents whether to select the grouped items or not.
     */
    public void unGroup(final ViewGroupNode vgn, final boolean selectConstituents) {
        for (ScreenElement element : vgn.getGroupedObjects()) {
            element.setPickable(true);
            if (selectConstituents) {
                selectionModel.add(element);
                element.setGrouped(false);
            }
        }
        vgn.removeFromParent();
    }

    /**
     * Create a group of GUI objects.
     */
    public void groupSelectedObjects() {

        ArrayList<ScreenElement> elements = new ArrayList<ScreenElement>();
        ArrayList<ScreenElement> toSearch = new ArrayList<ScreenElement>();

        // Ungroup selected groups
        for (PNode node : this.getSelectedNodes()) {
            if (node instanceof ViewGroupNode) {
                unGroup((ViewGroupNode) node, false);
                elements.addAll(((ViewGroupNode) node).getGroupedObjects());
            } else {
                if (node instanceof ScreenElement) {
                    toSearch.add((ScreenElement) node);
                }
            }
        }

        // Now group all elements.
        for (ScreenElement element : toSearch) {
            if (element.isDraggable()) {
                elements.add(element);
                element.setGrouped(true);
            }
        }

        vgn = new ViewGroupNode(this, elements);
        canvas.getLayer().addChild(vgn);
        this.setSelection(Collections.singleton(vgn));
    }

    public void setLastSelectedNeuron(final NeuronNode lastSelectedNeuron) {
        this.lastSelectedNeuron = lastSelectedNeuron;
    }

    public NeuronNode getLastSelectedNeuron() {
        return lastSelectedNeuron;
    }

    /**
     * Return height bottom toolbar is taking up.
     */
    private double getToolbarOffset() {
        if (southBar != null) {
            return southBar.getHeight();
        }
        return 0;
    }

    /**
     * @see PCanvas
     */
    public void repaint() {
        super.repaint();
        // if (timeLabel != null) {
        // timeLabel.setBounds(TIME_LABEL_H_OFFSET,
        // canvas.getCamera().getHeight() - getToolbarOffset(),
        // timeLabel.getHeight(), timeLabel.getWidth());
        // }
        //
        // if (updateStatusLabel != null) {
        // updateStatusLabel.setBounds(TIME_LABEL_H_OFFSET,
        // canvas.getCamera().getHeight() - getToolbarOffset(),
        // updateStatusLabel.getHeight(), updateStatusLabel.getWidth());
        // }

        if ((network != null) && (canvas.getLayer().getChildrenCount() > 0)) {
            zoomToFitPage(false);
        }
    }

    public boolean getAutoZoomMode() {
        return autoZoomMode;
    }

    public void setAutoZoomMode(final boolean autoZoomMode) {
        this.autoZoomMode = autoZoomMode;
        actionManager.getSetAutoZoomToggleButton().setSelected(autoZoomMode);
        repaint();
    }

    public boolean getInOutMode() {
        return inOutMode;
    }

    public boolean getShowSubnetOutline() {
        return showSubnetOutline;
    }

    public void setShowSubnetOutline(final boolean showSubnetOutline) {
        this.showSubnetOutline = showSubnetOutline;
    }

    public boolean getShowTime() {
        return showTime;
    }

    public void setShowTime(final boolean showTime) {
        this.showTime = showTime;
        timeLabel.setVisible(showTime);
    }

    // /**
    // * Update clamp toolbar buttons and menu items.
    // */
    // public void clampBarChanged() {
    // for (Iterator j = toggleButton.iterator(); j.hasNext(); ) {
    // JToggleButton box = (JToggleButton) j.next();
    // if (box.getAction() instanceof ClampWeightsAction) {
    // box.setSelected(Network.getClampWeights());
    // } else if (box.getAction() instanceof ClampNeuronsAction) {
    // box.setSelected(Network.getClampNeurons());
    // }
    // }
    // }
    //
    // /**
    // * Update clamp toolbar buttons and menu items.
    // */
    // public void clampMenuChanged() {
    // for (Iterator j = checkBoxes.iterator(); j.hasNext(); ) {
    // JCheckBoxMenuItem box = (JCheckBoxMenuItem) j.next();
    // if (box.getAction() instanceof ClampWeightsAction) {
    // box.setSelected(Network.getClampWeights());
    // } else if (box.getAction() instanceof ClampNeuronsAction) {
    // box.setSelected(Network.getClampNeurons());
    // }
    // }
    // }

    /**
     * Increases neuron and synapse activation levels.
     */
    public void incrementSelectedObjects() {
        for (Neuron neuron : getSelectedModels(Neuron.class)) {
            neuron.getUpdateRule().incrementActivation(neuron);
        }

        for (SynapseNode synapseNode : getSelectedNodes(SynapseNode.class)) {
            synapseNode.getSynapse().incrementWeight();
            synapseNode.updateColor();
            synapseNode.updateDiameter();
        }
    }

    /**
     * Decreases neuron and synapse activation levels.
     */
    public void decrementSelectedObjects() {

        for (NeuronNode neuronNode : getSelectedNodes(NeuronNode.class)) {
            neuronNode.getNeuron().getUpdateRule().decrementActivation(neuronNode.getNeuron());
            neuronNode.update();
        }

        for (SynapseNode synapseNode : getSelectedNodes(SynapseNode.class)) {
            synapseNode.getSynapse().decrementWeight();
            synapseNode.updateColor();
            synapseNode.updateDiameter();
        }
    }

    /**
     * Invoke contextual increment (which respects neuron specific rules) on selected objects.
     */
    public void contextualIncrementSelectedObjects() {
        for (Neuron neuron : getSelectedModels(Neuron.class)) {
            neuron.getUpdateRule().contextualIncrement(neuron);
        }
    }

    /**
     * Invoke contextual decrement (which respects neuron specific rules) on selected objects.
     */
    public void contextualDecrementSelectedObjects() {
        for (Neuron neuron : getSelectedModels(Neuron.class)) {
            neuron.getUpdateRule().contextualDecrement(neuron);
        }
    }

    /**
     * Nudge selected objects.
     *
     * @param offsetX amount to nudge in the x direction (multiplied by nudgeAmount)
     * @param offsetY amount to nudge in the y direction (multiplied by nudgeAmount)
     */
    protected void nudge(final int offsetX, final int offsetY) {
        for (Neuron neuron : getSelectedModels(Neuron.class)) {
            neuron.offset(offsetX * nudgeAmount, offsetY * nudgeAmount);
        }
    }

    /**
     * Clamped / unclamped or freeze / unfreeze selected nodes.
     */
    public void toggleClamping() {
        for (Neuron neuron : getSelectedModels(Neuron.class)) {
            neuron.setClamped(!neuron.isClamped());
        }

        for (SynapseNode synapseNode : getSelectedNodes(SynapseNode.class)) {
            Synapse synapse = synapseNode.getSynapse();
            synapse.setFrozen(!synapse.isFrozen());

            // TODO: this should happen via an event
            //   but firing events from setFrozen causes problems
            //   when opening saved networks
            synapseNode.updateClampStatus();
        }

        this.revalidate();
    }

    /**
     * Close model Network.
     */
    public void closeNetwork() {
    }

    public CustomToolBar getEditToolBar() {
        return editToolBar;
    }

    public CustomToolBar getRunToolBar() {
        return runToolBar;
    }

    public CustomToolBar getMainToolBar() {
        return mainToolBar;
    }

    /**
     * Clear all source elements.
     */
    public void clearSourceElements() {
        for (PNode node : sourceElements) {
            NodeHandle.removeSourceHandleFrom(node);
        }
        sourceElements.clear();
        selectionModel.fireSelectionChanged();
    }

    /**
     * Turns the displaying of loose synapses on and off (for performance increase or visual clarity).
     *
     * @param weightsVisible if true loose weights should be visible
     */
    public void setWeightsVisible(final boolean weightsVisible) {
        this.looseWeightsVisible = weightsVisible;
        actionManager.getShowWeightsAction().setState(looseWeightsVisible);
        for (SynapseNode node : getNodes(SynapseNode.class)) {
            if (node != null) {
                node.setVisible(weightsVisible);
            }
        }
    }

    /**
     * @return turn synapse nodes on.
     */
    public boolean getWeightsVisible() {
        return looseWeightsVisible;
    }

    /**
     * Turns the displaying of neuron priorities on or off.
     *
     * @param prioritiesOn whether to show priorities or not
     */
    public void setPrioritiesVisible(final boolean prioritiesOn) {
        this.prioritiesVisible = prioritiesOn;
        actionManager.getShowPrioritiesAction().setState(prioritiesOn);
        for (Iterator<NeuronNode> neuronNodes = this.getNodes(NeuronNode.class).iterator(); neuronNodes.hasNext(); ) {
            NeuronNode node = neuronNodes.next();
            node.setPriorityView(prioritiesVisible);
        }
    }

    public boolean getPrioritiesVisible() {
        return prioritiesVisible;
    }

    public boolean isGuiOn() {
        return guiOn && network.isRedrawTime();
    }

    public void setGuiOn(final boolean guiOn) {
        if (guiOn) {
            this.setUpdateComplete(false);
            this.updateNeuronNodes();
            this.updateSynapseNodes();
            updateComplete.decrementAndGet();
            network.setFireUpdates(true);
        } else {
            network.setFireUpdates(false);
        }
        this.guiOn = guiOn;
    }

    /**
     * Overridden so that multi-line tooltips can be used.
     */
    public JToolTip createToolTip() {
        return new JMultiLineToolTip();
    }

    public NetworkActionManager getActionManager() {
        return actionManager;
    }

    public JPopupMenu getContextMenuAlt() {
        return contextMenuAlt;
    }

    public TextEventHandler getTextHandle() {
        return textHandle;
    }

    public ViewGroupNode getViewGroupNode() {
        return vgn;
    }

    public void setTitle(String name) {
    }

    /**
     * Returns a NetworkDialog. Overriden by NetworkPanelDesktop, which returns a NetworkDialog with additional features
     * used in Desktop version of Simbrain.
     *
     * @param networkPanel network panel
     * @return subclass version of network dialog.
     */
    public NetworkDialog getNetworkDialog(final NetworkPanel networkPanel) {
        return new NetworkDialog(networkPanel);
    }

    /**
     * Overriden by NetworkPanelDesktop to adorn node with coupling menus.
     *
     * @param node the SynapseGroupNode.
     * @return the node. Does nothing here obviously.
     */
    public SynapseGroupNode addMenuToSynapseGroupNode(SynapseGroupNode node) {
        return node;
    }

    /**
     * Overridden by NetworkPanelDesktop to ensure modeless-ness relative to SimbrainDesktop.
     *
     * @param node the neuron group node to display the dialog for
     * @return the dialog representing the neuron group node.
     */
    public StandardDialog getNeuronGroupDialog(final NeuronGroupNode node) {

        NeuronGroupDialog ng = new NeuronGroupDialog(this, node.getNeuronGroup());
        ng.setTitle("Neuron Group Dialog");
        ng.setAsDoneDialog();
        ng.setModalityType(Dialog.ModalityType.MODELESS);
        return ng;
    }

    /**
     * Creates a synapse group dialog from a synapse group interaction box, overrriden in NetworkPanelDesktop to make it
     * modeless relative to the SimbrainDesktop.
     *
     * @param sgib must be a synapse group interaction box because SynapseGroupBidirectional does not inhereit from
     *             SynapseGroupNode and the interaction box is the only visual and interactable object with a 1:1
     *             correspondence to synapse groups.
     * @return
     */
    public StandardDialog getSynapseGroupDialog(SynapseGroupInteractionBox sgib) {
        SynapseGroupDialog sgd = SynapseGroupDialog.createSynapseGroupDialog(this, sgib.getSynapseGroup());
        sgd.setModalityType(Dialog.ModalityType.MODELESS);
        return sgd;
    }

    /**
     * Returns a dialog for editing the current selected neurons.
     *
     * @return the NeuronDialog
     */
    public NeuronDialog getNeuronDialog() {

        List<Neuron> neurons = getSelectedModels(Neuron.class);
        if (neurons == null || neurons.isEmpty()) {
            return null;
        }
        NeuronDialog dialog = new NeuronDialog(neurons);
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        return dialog;
    }

    public void showNeuronArrayCreationDialog() {
        NeuronArray.CreationTemplate creationTemplate =
                new NeuronArray.CreationTemplate(network.getArrayIdGenerator().getProposedId());
        StandardDialog and = new AnnotatedPropertyEditor(creationTemplate).getDialog();
        and.addClosingTask(() -> SwingUtilities.invokeLater(() -> {
            Network network = getNetwork();
            NeuronArray neuronArray = creationTemplate.create(network);
            neuronArray.setX(placementManager.getLocation().getX());
            neuronArray.setY(placementManager.getLocation().getY());
            network.addNeuronArray(neuronArray);
        }));
        and.pack();
        and.setLocationRelativeTo(null);
        and.setVisible(true);
    }

    public void showMultiLayerNetworkCreationDialog() {
        MultiLayerNetCreationDialog mlncd = new MultiLayerNetCreationDialog(this);
        mlncd.pack();
        mlncd.setLocationRelativeTo(null);
        mlncd.setVisible(true);
    }

    public StandardDialog getSynapseDialog(Collection<SynapseNode> sns) {
        SynapseDialog dialog = SynapseDialog.createSynapseDialog(sns);
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        return dialog;
    }

    /**
     * Remove all nodes from panel.
     */
    public void clearPanel() {
        canvas.getLayer().removeAllChildren();
    }

    /**
     * Adds an internal menu bar; used in applets.
     */
    public void addInternalMenuBar() {
        toolbars.add("North", NetworkMenuBar.getAppletMenuBar(this));
    }

    public boolean isRunning() {
        return network.isRunning();
    }

    public void setRunning(boolean value) {
        network.setRunning(value);
    }

    /**
     * Initialize the Gui. Intended to be called after the panel is loaded (in an applet's start() method, and by the
     * network components postAddInit() method).
     */
    public void initGui() {
        resetSynapseDiameters();
        resetColors();
        repaint();
        clearSelection();
    }

    public PCanvas getCanvas() {
        return canvas;
    }

    /**
     * Display a panel in a dialog. This is overridden by the desktop version to display the panel within the Simbrain
     * desktop.
     *
     * @param panel panel to display
     * @param title title for the frame
     * @return reference to frame the panel will be displayed in.
     */
    public GenericFrame displayPanel(final JPanel panel, String title) {
        GenericFrame frame = new GenericJDialog();
        if (frame instanceof JInternalFrame) {
            ((JInternalFrame) frame).addInternalFrameListener(new InternalFrameAdapter() {
                @Override
                public void internalFrameClosed(InternalFrameEvent e) {
                    if (panel instanceof EditablePanel) {
                        ((EditablePanel) panel).commitChanges();
                    }
                }
            });
        }
        frame.setContentPane(panel);
        frame.pack();
        frame.setTitle(title);
        frame.setVisible(true);
        return frame;
    }

    /**
     * A copy of displayPanel except returning a subclass of Window. Here to temporarily resolve ongoing conflict
     * between classes using generic frame and classes using window.
     *
     * @param panel the display panel
     * @param title the title of the panel
     * @return the new frame
     */
    public JDialog displayPanelInWindow(final JPanel panel, String title) {
        JDialog frame = new GenericJDialog();
        frame.setContentPane(panel);
        frame.pack();
        frame.setTitle(title);
        frame.setVisible(true);
        return frame;
    }

    public Map<Object, PNode> getObjectNodeMap() {
        return objectNodeMap;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public static Color getBackgroundColor() {
        return backgroundColor;
    }

    public static void setBackgroundColor(Color backgroundColor) {
        NetworkPanel.backgroundColor = backgroundColor;
    }

    public static double getNudgeAmount() {
        return nudgeAmount;
    }

    public static void setNudgeAmount(double nudgeAmount) {
        NetworkPanel.nudgeAmount = nudgeAmount;
    }

    /**
     * Creates the context menu for neurons. Overridden by {@link org.simbrain.network.desktop.NetworkPanelDesktop}
     * which adds a coupling menu.
     *
     * @param neuron neuron which needs a menu
     * @return the context menu
     */
    public JPopupMenu getNeuronContextMenu(Neuron neuron) {

        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.add(new CutAction(this));
        contextMenu.add(new CopyAction(this));
        contextMenu.add(new PasteAction(this));
        contextMenu.add(new DeleteAction(this));
        contextMenu.addSeparator();
        contextMenu.add(actionManager.getClearSourceNeuronsAction());
        contextMenu.add(actionManager.getSetSourceNeuronsAction());
        contextMenu.add(actionManager.getConnectionMenu());
        contextMenu.addSeparator();
        contextMenu.add(actionManager.getLayoutNeuronsAction());
        contextMenu.add(actionManager.getNeuronCollectionAction());
        contextMenu.addSeparator();
        // Add align and space menus if objects are selected
        if (this.getSelectedNodes(NeuronNode.class).size() > 1) {
            contextMenu.add(this.createAlignMenu());
            contextMenu.add(this.createSpacingMenu());
            contextMenu.addSeparator();
        }
        contextMenu.add(new SetNeuronPropertiesAction(this));
        contextMenu.addSeparator();
        JMenu nodeSelectionMenu = new JMenu("Select");
        nodeSelectionMenu.add(actionManager.getSelectIncomingWeightsAction());
        nodeSelectionMenu.add(actionManager.getSelectOutgoingWeightsAction());
        contextMenu.add(nodeSelectionMenu);
        contextMenu.addSeparator();
        contextMenu.add(actionManager.getTestInputAction());
        contextMenu.add(actionManager.getShowWeightMatrixAction());
        return contextMenu;
    }

    public JPopupMenu getSynapseContextMenu(Synapse synapse) {
        JPopupMenu contextMenu = new JPopupMenu();

        contextMenu.add(new CutAction(this));
        contextMenu.add(new CopyAction(this));
        contextMenu.add(new PasteAction(this));
        contextMenu.addSeparator();

        contextMenu.add(new DeleteAction(this));
        contextMenu.addSeparator();

        // contextMenu.add(this.getActionManager().getNeuronCollectionAction());
        // contextMenu.addSeparator();

        // Workspace workspace = getNetworkPanel().getWorkspace();
        // if (workspace.getGaugeList().size() > 0) {
        // contextMenu.add(workspace.getGaugeMenu(getNetworkPanel()));
        // contextMenu.addSeparator();
        // }

        contextMenu.add(new SetSynapsePropertiesAction(this));
        return contextMenu;
    }


    /**
     * Creates the coupling menu for the provided attribute container. Null if the network panel is not in a desktop
     * environment. Overridden by {@link org.simbrain.network.desktop.NetworkPanelDesktop} which has access to workspace
     * level coupling menus.
     *
     * @param container the neuron group whose producers and consumers will be used to produce a menu
     * @return the coupling menu
     */
    public JMenu getCouplingMenu(AttributeContainer container) {
        return null;
    }

    public QuickConnectionManager getQuickConnector() {
        return quickConnector;
    }

    public boolean getUpdateComplete() {
        return updateComplete.get() == 0;
    }

    public void setUpdateComplete(boolean updateComplete) {
        if (!updateComplete && this.updateComplete.get() != 0) {
            return;
        }
        this.updateComplete.set(updateComplete ? 0 : 3);
    }

    /**
     * Returns the neuron node corresponding to a neuron, or null if there is no match.
     *
     * @param neuron neuron to check for
     * @return corresponding neuron node
     */
    public NeuronNode getNode(Neuron neuron) {
        return ((NeuronNode) objectNodeMap.get(neuron));
    }

    /**
     * Unselect everything and clear source elements.
     */
    public void unselectAll() {
        selectionModel.clear();
        clearSourceElements();
    }

    /**
     * Set all node activations to 0.
     */
    public void clearNeurons() {
        for (NeuronNode node : getNodes(NeuronNode.class)) {
            node.getNeuron().clear();
        }
        this.setSelection(getNodes(NeuronNode.class));
    }

    /**
     * Set all selected items (nodes / weights) to 0. Dangerous for weights!
     */
    public void clearSelectedObjects() {
        for (NeuronGroupNode node : getSelectedNodes(NeuronGroupNode.class)) {
            node.getNeuronGroup().clearActivations();
        }
        for (NeuronNode node : getSelectedNodes(NeuronNode.class)) {
            node.getNeuron().clear();
        }
        for (SynapseNode node : getSelectedNodes(SynapseNode.class)) {
            node.getSynapse().forceSetStrength(0);
        }
        for (NeuronArray na : getSelectedModels(NeuronArray.class)) {
            na.clear();
        }
    }

    /**
     * Select nodes in selected neuron groups.
     */
    public void selectNeuronsInNeuronGroups() {
        for (NeuronGroupNode ng : getSelectedNodes(NeuronGroupNode.class)) {
            ng.selectNeurons();
        }
    }

    public PlacementManager getPlacementManager() {
        return placementManager;
    }


    /**
     * Display the provided network in a dialog
     *
     * @param network the model network to show
     */
    public static void showNetwork(Network network) {
        // TODO: Creation outside of desktop lacks menus
        NetworkPanel np = new NetworkPanel(network);
        np.syncToModel();
        JFrame frame = new JFrame();
        frame.setContentPane(np);
        frame.setPreferredSize(new Dimension(500, 500));
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });
        //System.out.println(np.debugString());
    }

}
