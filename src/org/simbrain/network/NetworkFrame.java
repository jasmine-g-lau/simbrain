/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network;

import org.simbrain.gauge.GaugeFrame;

import org.simbrain.workspace.Workspace;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.util.ArrayList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;


/**
 * <b>NetworkFrame</b> contains a neural network
 */
public class NetworkFrame extends JInternalFrame implements ActionListener, MenuListener, InternalFrameListener {
    private Workspace workspace;
    private NetworkPanel netPanel = new NetworkPanel(this);

    // For workspace persistence 
    private String path = null;
    private int xpos;
    private int ypos;
    private int the_width;
    private int the_height;
    private String name;
    private boolean changedSinceLastSave = false;
    JMenuBar mb = new JMenuBar();
    JMenu fileMenu = new JMenu("File  ");
    JMenuItem newNetSubmenu = new JMenu("New");
    JMenuItem newWTAItem = new JMenuItem("Winner take all network");
    JMenuItem newHopfieldItem = new JMenuItem("Hopfield network");
    JMenuItem newBackpropItem = new JMenuItem("Backprop network");
    JMenuItem openNetItem = new JMenuItem("Open");
    JMenuItem saveNetItem = new JMenuItem("Save");
    JMenuItem saveAsItem = new JMenuItem("Save As");
    JMenuItem close = new JMenuItem("Close");
    JMenu editMenu = new JMenu("Edit  ");
    JMenuItem copyItem = new JMenuItem("Copy Selection");
    JMenuItem cutItem = new JMenuItem("Cut Selection");
    JMenuItem pasteItem = new JMenuItem("Paste Selection");
    JMenuItem setNeuronItem = new JMenuItem("Set Neuron(s)");
    JMenuItem setWeightItem = new JMenuItem("Set Weight(s)");
    JMenuItem selectAll = new JMenuItem("Select All");
    JMenuItem alignSubmenu = new JMenu("Align");
    JMenuItem alignHorizontal = new JMenuItem("Horizontal");
    JMenuItem alignVertical = new JMenuItem("Vertical");
    JMenuItem spacingSubmenu = new JMenu("Spacing");
    JMenuItem spacingHorizontal = new JMenuItem("Horizontal");
    JMenuItem spacingVertical = new JMenuItem("Vertical");
    JMenuItem clampWeights = new JCheckBoxMenuItem("Clamp weights", false);
    JMenuItem setInOutItem = new JCheckBoxMenuItem("Show I/O Info", false);
    JMenuItem subnetworkOutline = new JCheckBoxMenuItem("Show Subnetwork Outline", true);
    JMenuItem setAutozoom = new JCheckBoxMenuItem("Autozoom", true);
    JMenuItem prefsItem = new JMenuItem("Preferences");
    JMenu gaugeMenu = new JMenu("Gauges  ");
    JMenuItem addGaugeItem = new JMenuItem("Add Gauge");
    JMenu helpMenu = new JMenu("Help");
    JMenuItem quickRefItem = new JMenuItem("Network Help");

    public NetworkFrame() {
    }

    public NetworkFrame(Workspace ws) {
        workspace = ws;
        init();
    }

    public void init() {
        this.setResizable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setClosable(true);
        setUpMenus();
        this.getContentPane().add("Center", netPanel);
        this.addInternalFrameListener(this);
        this.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
    }

    /**
     * Sets up the main menu bar
     */
    private void setUpMenus() {
        this.setJMenuBar(mb);

        mb.add(fileMenu);
        fileMenu.add(newNetSubmenu);
        newNetSubmenu.add(newWTAItem);
        newWTAItem.addActionListener(this);
        newNetSubmenu.add(newHopfieldItem);
        newHopfieldItem.addActionListener(this);
        newNetSubmenu.add(newBackpropItem);
        newBackpropItem.addActionListener(this);
        fileMenu.addSeparator();
        openNetItem.setAccelerator(KeyStroke.getKeyStroke(
                                                          KeyEvent.VK_O,
                                                          Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileMenu.add(openNetItem);
        openNetItem.addActionListener(this);
        saveNetItem.setAccelerator(KeyStroke.getKeyStroke(
                                                          KeyEvent.VK_S,
                                                          Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileMenu.add(saveNetItem);
        saveNetItem.addActionListener(this);
        fileMenu.add(saveAsItem);
        saveAsItem.addActionListener(this);
        fileMenu.add(close);
        close.addActionListener(this);
        close.setAccelerator(KeyStroke.getKeyStroke(
                                                    KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        fileMenu.addMenuListener(this);

        mb.add(editMenu);
        copyItem.setAccelerator(KeyStroke.getKeyStroke(
                                                       KeyEvent.VK_C,
                                                       Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        editMenu.add(copyItem);
        copyItem.addActionListener(this);
        cutItem.setAccelerator(KeyStroke.getKeyStroke(
                                                      KeyEvent.VK_X,
                                                      Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        editMenu.add(cutItem);
        cutItem.addActionListener(this);
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(
                                                        KeyEvent.VK_V,
                                                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        editMenu.add(pasteItem);
        pasteItem.addActionListener(this);
        editMenu.addSeparator();
        selectAll.setAccelerator(KeyStroke.getKeyStroke(
                                                        KeyEvent.VK_A,
                                                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        editMenu.add(selectAll);
        selectAll.addActionListener(this);
        editMenu.addSeparator();
        editMenu.add(alignSubmenu);
        alignSubmenu.add(alignHorizontal);
        alignHorizontal.addActionListener(this);
        alignSubmenu.add(alignVertical);
        alignVertical.addActionListener(this);
        editMenu.add(spacingSubmenu);
        spacingSubmenu.add(spacingHorizontal);
        spacingHorizontal.addActionListener(this);
        spacingSubmenu.add(spacingVertical);
        spacingVertical.addActionListener(this);
        editMenu.addSeparator();
        editMenu.add(setNeuronItem);
        setNeuronItem.addActionListener(this);
        editMenu.add(setWeightItem);
        setWeightItem.addActionListener(this);
        editMenu.addSeparator();
        editMenu.add(clampWeights);
        clampWeights.addActionListener(this);
        editMenu.addSeparator();
        editMenu.add(setInOutItem);
        setInOutItem.addActionListener(this);
        editMenu.add(setAutozoom);
        setAutozoom.addActionListener(this);
        editMenu.add(subnetworkOutline);
        subnetworkOutline.addActionListener(this);
        editMenu.addSeparator();
        editMenu.add(prefsItem);
        prefsItem.addActionListener(this);
        editMenu.addMenuListener(this);

        mb.add(gaugeMenu);
        gaugeMenu.add(addGaugeItem);
        addGaugeItem.addActionListener(this);
        gaugeMenu.addMenuListener(this);

        mb.add(helpMenu);
        quickRefItem.setAccelerator(KeyStroke.getKeyStroke(
                                                           KeyEvent.VK_H,
                                                           Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        helpMenu.add(quickRefItem);
        quickRefItem.addActionListener(this);
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if ((e.getSource().getClass() == JMenuItem.class) || (e.getSource().getClass() == JCheckBoxMenuItem.class)) {
            JMenuItem jmi = (JMenuItem) e.getSource();

            if (jmi == openNetItem) {
                netPanel.open();
                changedSinceLastSave = false;
            } else if (jmi == saveAsItem) {
                netPanel.saveAs();
            } else if (jmi == saveNetItem) {
                netPanel.save();
            } else if (jmi == selectAll) {
                netPanel.selectAll();
            } else if (jmi == prefsItem) {
                netPanel.showNetworkPrefs();
                changedSinceLastSave = true;
            } else if (jmi == setNeuronItem) {
                netPanel.showNeuronPrefs();
                changedSinceLastSave = true;
            } else if (jmi == setWeightItem) {
                netPanel.showWeightPrefs();
                changedSinceLastSave = true;
            } else if (jmi == setInOutItem) {
                netPanel.setInOutMode(setInOutItem.isSelected());
                netPanel.renderObjects();
                netPanel.repaint();
            } else if (jmi == clampWeights) {
                netPanel.getNetwork().setClampWeights(clampWeights.isSelected());
            } else if (jmi == setAutozoom) {
                netPanel.setAutoZoom(setAutozoom.isSelected());
                netPanel.repaint();
            } else if (jmi == subnetworkOutline) {
                netPanel.setSubnetworkOutline(subnetworkOutline.isSelected());
                netPanel.repaint();
            } else if (jmi == prefsItem) {
                netPanel.showNetworkPrefs();
            } else if (jmi == addGaugeItem) {
                netPanel.addGauge();
            } else if (jmi == newWTAItem) {
                netPanel.showWTADialog();
            } else if (jmi == newHopfieldItem) {
                netPanel.showHopfieldDialog();
            } else if (jmi == newBackpropItem) {
                netPanel.showBackpropDialog();
            } else if (jmi == cutItem) {
                netPanel.getHandle().cutToClipboard();
            } else if (jmi == copyItem) {
                netPanel.getHandle().copyToClipboard();
            } else if (jmi == pasteItem) {
                netPanel.paste();
                changedSinceLastSave = true;
            } else if (jmi == alignHorizontal) {
                netPanel.alignHorizontal();
                changedSinceLastSave = true;
            } else if (jmi == alignVertical) {
                netPanel.alignVertical();
                changedSinceLastSave = true;
            } else if (jmi == spacingHorizontal) {
                netPanel.spacingHorizontal();
                changedSinceLastSave = true;
            } else if (jmi == spacingVertical) {
                netPanel.spacingVertical();
                changedSinceLastSave = true;
            } else if (jmi == quickRefItem) {
                org.simbrain.util.Utils.showQuickRef(this);
            } else if (jmi == close) {
                if (isChangedSinceLastSave()) {
                    hasChanged();
                } else {
                    dispose();
                }
            }
        }
    }

    public void internalFrameOpened(InternalFrameEvent e) {
    }

    public void internalFrameClosing(InternalFrameEvent e) {
        if (isChangedSinceLastSave()) {
            hasChanged();
        } else {
            dispose();
        }
    }

    public void internalFrameClosed(InternalFrameEvent e) {
        this.getNetPanel().resetNetwork();
        this.getWorkspace().getNetworkList().remove(this);

        // To prevent currently linked gauges from being updated
        ArrayList gauges = this.getWorkspace().getGauges(this);

        for (int i = 0; i < gauges.size(); i++) {
            ((GaugeFrame) gauges.get(i)).getGaugedVars().clear();
        }

        //resentCommandTargets
        NetworkFrame net = workspace.getLastNetwork();

        if (net != null) {
            net.grabFocus();
            workspace.repaint();
        }

        NetworkPreferences.setCurrentDirectory(netPanel.getSerializer().getCurrentDirectory());
    }

    public void internalFrameIconified(InternalFrameEvent e) {
    }

    public void internalFrameDeiconified(InternalFrameEvent e) {
    }

    public void internalFrameActivated(InternalFrameEvent e) {
    }

    public void internalFrameDeactivated(InternalFrameEvent e) {
    }

    ////////////////////////////
    // Menu Even      //
    ////////////////////////////
    public void menuCanceled(MenuEvent e) {
    }

    public void menuDeselected(MenuEvent e) {
    }

    /* (non-Javadoc)
     * @see javax.swing.event.MenuListener#menuSelected(javax.swing.event.MenuEvent)
     */
    public void menuSelected(MenuEvent e) {
        // Handle gauge submenu
        // TODO: Note! This will break if more menuitems are added
        JMenu gaugeSubMenu = getWorkspace().getGaugeMenu(netPanel);

        if (gaugeSubMenu != null) {
            if (gaugeMenu.getItemCount() == 1) {
                gaugeMenu.add(gaugeSubMenu);
            } else {
                gaugeMenu.remove(1);
                gaugeMenu.add(gaugeSubMenu);
            }
        }

        if (e.getSource().equals(fileMenu)) {
            if (isChangedSinceLastSave()) {
                saveNetItem.setEnabled(true);
            } else if (!isChangedSinceLastSave()) {
                saveNetItem.setEnabled(false);
            }
        }

        // Handle set-neuron and set-weight menu-items.
        int num_neurons = netPanel.getSelectedNeurons().size();

        if (num_neurons > 0) {
            setNeuronItem.setText("Set " + num_neurons + ((num_neurons > 1) ? " Selected Neurons" : " Selected Neuron"));
            setNeuronItem.setEnabled(true);
        } else {
            setNeuronItem.setText("Set Selected Neuron(s)");
            setNeuronItem.setEnabled(false);
        }

        int num_weights = netPanel.getSelectedWeights().size();

        if (num_weights > 0) {
            setWeightItem.setText("Set " + num_weights + ((num_weights > 1) ? " Selected Weights" : " Selected Weight"));
            setWeightItem.setEnabled(true);
        } else {
            setWeightItem.setText("Set Selected Weight(s)");
            setWeightItem.setEnabled(false);
        }
    }

    ////////////////////////////
    // Main method		      //
    ///////////////////////////

    /**
     * @return Returns the netPanel.
     */
    public NetworkPanel getNetPanel() {
        return netPanel;
    }

    /**
     * @param netPanel The netPanel to set.
     */
    public void setNetPanel(NetworkPanel netPanel) {
        this.netPanel = netPanel;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        setTitle(name);
        this.name = name;
    }

    /**
     * @return Returns the path.  Used in persistence.
     */
    public String getPath() {
        return path;
    }

    /**
     * @return platform-specific path.  Used in persistence.
     */
    public String getGenericPath() {
        String ret = path;

        if (path == null) {
            return null;
        }

        ret.replace('/', System.getProperty("file.separator").charAt(0));

        return ret;
    }

    /**
     * @param path The path to set.  Used in persistence.
     */
    public void setPath(String path) {
        String thePath = path;

        if (thePath.charAt(2) == '.') {
            thePath = path.substring(2, path.length());
        }

        thePath = thePath.replace(System.getProperty("file.separator").charAt(0), '/');
        this.path = thePath;
    }

    /**
     * @return Returns the parent.
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * @param parent The parent to set.
     */
    public void setWorkspace(Workspace parent) {
        this.workspace = parent;
    }

    /**
     * For Castor.  Turn Component bounds into separate variables.
     */
    public void initBounds() {
        xpos = this.getX();
        ypos = this.getY();
        the_width = this.getBounds().width;
        the_height = this.getBounds().height;
    }

    /**
     * @return Returns the xpos.
     */
    public int getXpos() {
        return xpos;
    }

    /**
     * @param xpos The xpos to set.
     */
    public void setXpos(int xpos) {
        this.xpos = xpos;
    }

    /**
     * @return Returns the ypos.
     */
    public int getYpos() {
        return ypos;
    }

    /**
     * @param ypos The ypos to set.
     */
    public void setYpos(int ypos) {
        this.ypos = ypos;
    }

    /**
     * @return Returns the the_height.
     */
    public int getThe_height() {
        return the_height;
    }

    /**
     * @param the_height The the_height to set.
     */
    public void setThe_height(int the_height) {
        this.the_height = the_height;
    }

    /**
     * @return Returns the the_width.
     */
    public int getThe_width() {
        return the_width;
    }

    /**
     * @param the_width The the_width to set.
     */
    public void setThe_width(int the_width) {
        this.the_width = the_width;
    }

    /**
     * Display dialog asking user whether he/she wants to save the network Called when closing network after changes
     * have been made.
     */
    private void hasChanged() {
        Object[] options = { "Save", "Don't Save", "Cancel" };
        int s = JOptionPane.showInternalOptionDialog(
                                                     this,
                                                     "Network " + this.getName()
                                                     + " has changed since last save,\nwould you like to save these changes?",
                                                     "Network Has Changed", JOptionPane.YES_NO_OPTION,
                                                     JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        if (s == 0) {
            netPanel.save();
            dispose();
        } else if (s == 1) {
            dispose();
        } else {
            return;
        }
    }

    /**
     * @return Returns the changedSinceLastSave.
     */
    public boolean isChangedSinceLastSave() {
        return changedSinceLastSave;
    }

    /**
     * @param changedSinceLastSave The changedSinceLastSave to set.
     */
    public void setChangedSinceLastSave(boolean hasChangedSinceLastSave) {
        this.changedSinceLastSave = hasChangedSinceLastSave;
    }
}
