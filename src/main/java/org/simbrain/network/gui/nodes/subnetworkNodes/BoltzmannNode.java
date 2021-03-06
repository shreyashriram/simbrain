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
package org.simbrain.network.gui.nodes.subnetworkNodes;

import org.piccolo2d.PNode;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.BoltzmannTrainingDialog;
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.subnetworks.BoltzmannMachine;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * PNode representation of SOM Network.
 *
 * @author jyoshimi
 */
public class BoltzmannNode extends SubnetworkNode {

    /**
     * Create a Boltzmann Network PNode.
     *
     * @param networkPanel parent panel
     * @param subnet       the Boltzmann network
     */
    public BoltzmannNode(NetworkPanel networkPanel, BoltzmannMachine subnet) {
        super(networkPanel, subnet);
        setInteractionBox(new BoltzmannInteractionBox(networkPanel));
        setContextMenu();
    }

    @Override
    public StandardDialog getPropertyDialog() {
        return new BoltzmannTrainingDialog(getNetworkPanel(), (BoltzmannMachine) getSubnetwork());
    }

    /**
     * Sets custom menu for SOM Network node.
     */
    private void setContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        editAction.putValue("Name", "Edit / Train Boltzmann Machine...");
        menu.add(editAction);
        menu.add(renameAction);
        menu.add(removeAction);
        menu.addSeparator();
        menu.add(addInputRowAction);
        Action trainNet = new AbstractAction("Train on current pattern") {
            public void actionPerformed(final ActionEvent event) {
                //                SOMNetwork net = ((SOMNetwork) getSubnetwork());
                //                net.update();
                //                net.getParentNetwork().fireGroupUpdated(net);
            }
        };
        menu.add(trainNet);
        menu.addSeparator();
        Action randomizeNet = new AbstractAction("Randomize synapses") {
            public void actionPerformed(final ActionEvent event) {
                //                SOMNetwork net = ((SOMNetwork) getSubnetwork());
                //                net.getSom().randomizeIncomingWeights();
                //                net.getParentNetwork().fireGroupUpdated(net);
            }
        };
        menu.add(randomizeNet);
        setContextMenu(menu);
    }

    /**
     * Custom interaction box for SOM group node.
     */
    private class BoltzmannInteractionBox extends SubnetworkNodeInteractionBox {
        public BoltzmannInteractionBox(NetworkPanel net) {
            super(net);
        }

        @Override
        public String getToolTipText() {
            return "Temperature: " + Utils.round(((BoltzmannMachine) getSubnetwork()).getTemperature(), 2);
        }

        @Override
        public BoltzmannNode getNode() {
            return BoltzmannNode.this;
        }

        @Override
        public BoltzmannMachine getModel() {
            return (BoltzmannMachine) BoltzmannNode.this.getSubnetwork();
        }
    }

}
