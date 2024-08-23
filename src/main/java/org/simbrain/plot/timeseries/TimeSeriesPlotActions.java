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

import org.simbrain.util.ResourceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Contains actions for use in Time Series Component.
 *
 * @author jyoshimi
 */
public class TimeSeriesPlotActions {

    /**
     * Shows a properties dialog for the trainer.
     *
     * @param timeSeriesPanel reference to time series plot panel
     * @return the action
     */
    public static Action getPropertiesDialogAction(TimeSeriesPlotPanel timeSeriesPanel) {
        return new AbstractAction() {
            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Prefs.png"));
                putValue(NAME, "Plot properties...");
                putValue(SHORT_DESCRIPTION, "Show time series graph properties");
            }

            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.showPropertiesDialog();
            }
        };
    }

    /**
     * Clear the graph.
     *
     * @param timeSeriesPanel reference to time series plot panel
     * @return the action
     */
    public static Action getClearGraphAction(TimeSeriesPlotPanel timeSeriesPanel) {
        return new AbstractAction() {
            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("menu_icons/Eraser.png"));
                putValue(SHORT_DESCRIPTION, "Clear graph data");
            }

            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.getTimeSeriesModel().clearData();
            }
        };
    }

    /**
     * Add a data source.
     *
     * @param timeSeriesPanel reference to time series plot panel
     * @return the action
     */
    public static Action getAddSourceAction(TimeSeriesPlotPanel timeSeriesPanel) {
        return new AbstractAction() {
            // Initialize
            {
                putValue(NAME, "Add");
                putValue(SHORT_DESCRIPTION, "Add a data source");
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.getTimeSeriesModel().addTimeSeries();
            }
        };
    }

    /**
     * Add a data source.
     *
     * @param timeSeriesPanel reference to time series plot panel
     * @return the action
     */
    public static Action getRemoveSourceAction(
        final TimeSeriesPlotPanel timeSeriesPanel) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(NAME, "Remove");
                putValue(SHORT_DESCRIPTION, "Remove a data source");
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                timeSeriesPanel.getTimeSeriesModel().removeLastTimeSeries();
            }

        };
    }


}
