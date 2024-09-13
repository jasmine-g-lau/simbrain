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
package org.simbrain.plot.rasterchart;

import kotlin.Unit;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.simbrain.util.SwingUtilsKt;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * Display a raster plot. This component can be used independently of the raster
 * plot workspace component.
 */
public class RasterPlotPanel extends JPanel {

    /**
     * Chart un-initialized instance.
     */
    private JFreeChart chart;

    /**
     * Initial size.
     */
    private static final Dimension PREFERRED_SIZE = new Dimension(500, 400);

    /**
     * Panel for chart.
     */
    private ChartPanel chartPanel = new ChartPanel(null);

    /**
     * Data model.
     */
    private RasterModel model;

    /**
     * Button panel.
     */
    private JPanel buttonPanel = new JPanel();

    /**
     * Renderer object where things like dot color and size are set.
     */
    private XYItemRenderer renderer;


    /**
     * Construct a raster panel.
     *
     * @param rasterModel model underlying model
     */
    public RasterPlotPanel(final RasterModel rasterModel) {

        model = rasterModel;
        setPreferredSize(PREFERRED_SIZE);
        setLayout(new BorderLayout());

        addAddDeleteButtons();
        addClearGraphDataButton();
        addPreferencesButton();

        add("Center", chartPanel);
        add("South", buttonPanel);

        init();
    }

    /**
     * Initialize Chart Panel.
     */
    public void init() {

        // Generate the graph
        chart = ChartFactory.createScatterPlot("", // Title
            "Iterations", // x-axis Label
            "Value(s)", // y-axis Label
            model.getDataset(), // Dataset
            PlotOrientation.VERTICAL, // Plot Orientation
            true, // Show Legend
            true, // Use tooltips
            false // Configure chart to generate URLs?
        );
        renderer = ((XYPlot) chart.getPlot()).getRenderer();
        updateChartSettings();
        chartPanel.setChart(chart);
        chart.setBackgroundPaint(null);

    }

    private void updateChartSettings() {

        // Renderer properties
        double size = model.getDotSize();
        double delta = size / 2.0;
        Shape shape1 = new Rectangle2D.Double(-delta, -delta, size, size);
        Shape shape2 = new Ellipse2D.Double(-delta, -delta, size, size);
        renderer.setSeriesShape(0, shape1);
        renderer.setSeriesShape(1, shape2);
        renderer.setSeriesShape(2, shape1);
        renderer.setSeriesShape(3, shape2);

        // Handle range properties
        chart.getXYPlot().getRangeAxis().setAutoRange(model.isAutoRange());
        if (!model.isAutoRange()) {
            chart.getXYPlot().getRangeAxis().setRange(model.getRangeLowerBound(), model.getRangeUpperBound());
        }

        // Handle domain properties
        if (model.isFixedWidth()) {
            chart.getXYPlot().getDomainAxis().setFixedAutoRange(model.getWindowSize());
        } else {
            chart.getXYPlot().getDomainAxis().setFixedAutoRange(-1);
            chart.getXYPlot().getDomainAxis().setAutoRange(true);
        }
    }

    /**
     * Remove all buttons from the button panel; used when customzing the
     * buttons on this panel.
     */
    public void removeAllButtonsFromToolBar() {
        buttonPanel.removeAll();
    }

    /**
     * Return button panel in case user would like to add custom buttons.
     */
    public JPanel getButtonPanel() {
        return buttonPanel;
    }

    /**
     * Add button for clearing graph data.
     */
    public void addClearGraphDataButton() {
        JButton clearButton = new JButton("Clear");
        clearButton.setAction(RasterPlotActions.getClearGraphAction(this));
        buttonPanel.add(clearButton);
    }

    /**
     * Add button for showing preferences.
     */
    public void addPreferencesButton() {
        JButton prefsButton = new JButton("Prefs");
        prefsButton.setHideActionText(true);
        prefsButton.setAction(RasterPlotActions.getPropertiesDialogAction(this));
        buttonPanel.add(prefsButton);
    }

    /**
     * Add buttons for adding and deleting sources.
     */
    public void addAddDeleteButtons() {
        JButton deleteButton = new JButton("Delete");
        deleteButton.setAction(RasterPlotActions.getRemoveSourceAction(this));
        JButton addButton = new JButton("Add");
        addButton.setAction(RasterPlotActions.getAddSourceAction(this));
        buttonPanel.add(deleteButton);
        buttonPanel.add(addButton);
    }

    /**
     * Show properties dialog.
     */
    public void showPropertiesDialog() {
        var dialog = SwingUtilsKt.createEditorDialog(model, model.getName(), (e) -> {
            updateChartSettings();
            return Unit.INSTANCE;
        });
        SwingUtilsKt.display(dialog);
    }

    public ChartPanel getChartPanel() {
        return chartPanel;
    }

    public RasterModel getRasterModel() {
        return model;
    }
}
