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
package org.simbrain.network.gui.dialogs.network;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.subnetworks.SOMNetwork;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;

/**
 * <b>SOMDialog</b> is used as an assistant to create SOM networks.
 */
public class SOMCreationDialog extends StandardDialog {

    private AnnotatedPropertyEditor somPanel;

    private SOMNetwork.SOMCreator sc = new  SOMNetwork.SOMCreator();

    private NetworkPanel networkPanel;

    public SOMCreationDialog(final NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        setTitle("New SOM Network");
        somPanel = new AnnotatedPropertyEditor(sc);
        setContentPane(somPanel);

        Action helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/network/subnetworks/selfOrganizingMap.html");
        addButton(new JButton(helpAction));
    }

    @Override
    protected void closeDialogOk() {
        somPanel.commitChanges();
        SOMNetwork som = sc.create();
        networkPanel.getNetwork().addNetworkModel(som);
        super.closeDialogOk();
    }

}
