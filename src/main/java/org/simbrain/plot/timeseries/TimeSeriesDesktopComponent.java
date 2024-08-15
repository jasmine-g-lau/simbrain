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
package org.simbrain.plot.timeseries;

import org.simbrain.plot.actions.PlotActionManager;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;

import javax.swing.*;
import java.awt.*;

/**
 * Display a TimeSeriesPlot.
 */
public class TimeSeriesDesktopComponent extends DesktopComponent<TimeSeriesPlotComponent> {

    /**
     * Plot action manager.
     */
    private PlotActionManager actionManager;

    /**
     * Panel for chart.
     */
    private TimeSeriesPlotPanel timeSeriesPanel;

    /**
     * Construct a time series plot gui.
     *
     * @param frame     parent frame
     * @param component the underlying component
     */
    public TimeSeriesDesktopComponent(GenericFrame frame, TimeSeriesPlotComponent component) {
        super(frame, component);
        actionManager = new PlotActionManager(this);
        timeSeriesPanel = new TimeSeriesPlotPanel(component.getModel());
        createAttachMenuBar();
        this.setLayout(new BorderLayout());
        add("Center", timeSeriesPanel);
    }

    /**
     * Creates the menu bar.
     */
    private void createAttachMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        for (Action action : actionManager.getOpenSavePlotActions()) {
            fileMenu.add(action);
        }
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createRenameAction(this));
        fileMenu.addSeparator();
        fileMenu.add(SimbrainDesktop.INSTANCE.getActionManager().createCloseAction(this));

        JMenu editMenu = new JMenu("Edit");
        editMenu.add(new JMenuItem(TimeSeriesPlotActions.getPropertiesDialogAction(timeSeriesPanel)));

        JMenu helpMenu = new JMenu("Help");
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Plot/time_series.html");
        JMenuItem helpItem = new JMenuItem(helpAction);
        helpMenu.add(helpItem);

        bar.add(fileMenu);
        bar.add(editMenu);
        bar.add(helpMenu);

        getParentFrame().setJMenuBar(bar);
    }

}
