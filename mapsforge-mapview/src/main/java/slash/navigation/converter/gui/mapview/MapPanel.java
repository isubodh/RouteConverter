/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.converter.gui.mapview;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER;
import static com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH;
import static com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW;
import static com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK;
import static java.awt.event.ItemEvent.SELECTED;
import static slash.common.io.Files.collectFiles;
import static slash.common.io.Files.getExtension;
import static slash.navigation.converter.gui.mapview.FileListCellRenderer.removePrefix;
import static slash.navigation.converter.gui.mapview.MapsforgeMapView.OPEN_STREET_MAP_MAPNIK_ONLINE;
import static slash.navigation.converter.gui.mapview.MapsforgeMapView.OSMARENDERER_INTERNAL;

/**
 * The map and theme chooser panel of the mapsforge map view.
 *
 * @author Christian Pesch
 */

public class MapPanel {
    private static final Preferences preferences = Preferences.userNodeForPackage(MapPanel.class);
    private static final String MAP_FILE_PREFERENCE = "mapFile";
    private static final String THEME_FILE_PREFERENCE = "themeFile";

    private JComboBox comboBoxMap;
    private JComboBox comboBoxTheme;
    private JPanel component;
    private JPanel mapViewPanel;

    private File mapsforgeDirectory;
    private DefaultComboBoxModel themeModel = new DefaultComboBoxModel(new File[]{OSMARENDERER_INTERNAL});
    private boolean ignoreThemeSelectionEvents = false;

    public MapPanel(final MapsforgeMapView mapsforgeMapView, File newMapsforgeDirectory, AwtGraphicMapView awtGraphicMapView) {
        this.mapsforgeDirectory = newMapsforgeDirectory;

        comboBoxMap.setRenderer(new FileListCellRenderer(mapsforgeDirectory));
        comboBoxMap.setModel(new DefaultComboBoxModel(collectFiles(mapsforgeDirectory, ".map").toArray()));
        comboBoxMap.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED)
                    return;
                File mapFile = (File) e.getItem();
                updateThemes(mapFile);
                mapsforgeMapView.setMapFile(mapFile, (File) comboBoxTheme.getSelectedItem());
                preferences.put(MAP_FILE_PREFERENCE, mapFile.getAbsolutePath());
            }
        });

        comboBoxTheme.setRenderer(new FileListCellRenderer(mapsforgeDirectory));
        comboBoxTheme.setModel(themeModel);
        comboBoxTheme.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() != SELECTED || ignoreThemeSelectionEvents)
                    return;
                File mapFile = (File) comboBoxMap.getSelectedItem();
                File themeFile = (File) e.getItem();
                mapsforgeMapView.setMapFile(mapFile, themeFile);
                preferences.put(THEME_FILE_PREFERENCE + removePrefix(mapsforgeDirectory, mapFile), themeFile.getAbsolutePath());
            }
        });

        File mapFile = new File(preferences.get(MAP_FILE_PREFERENCE, OPEN_STREET_MAP_MAPNIK_ONLINE.getName()));
        comboBoxMap.setSelectedItem(mapFile);
        updateThemes(mapFile);

        mapViewPanel.add(awtGraphicMapView, new GridConstraints(0, 0, 1, 1, ANCHOR_CENTER, FILL_BOTH,
                SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW, SIZEPOLICY_CAN_SHRINK | SIZEPOLICY_CAN_GROW,
                new Dimension(0, 0), new Dimension(0, 0), new Dimension(2000, 2640), 0, true));

        mapsforgeMapView.setMapFile((File) comboBoxMap.getSelectedItem(), (File) comboBoxTheme.getSelectedItem());
    }

    private void updateThemes(File mapFile) {
        File themeFile = new File(preferences.get(THEME_FILE_PREFERENCE + removePrefix(mapsforgeDirectory, mapFile), OSMARENDERER_INTERNAL.getName()));
        this.ignoreThemeSelectionEvents = true;
        try {
            for (int i = themeModel.getSize() - 1; i > 0; i--) {
                themeModel.removeElementAt(i);
            }
            if (mapFile.getParentFile() != null) {
                List<File> themeFiles = collectThemeFiles(mapFile.getParentFile(), mapsforgeDirectory);
                for (File file : themeFiles) {
                    themeModel.addElement(file);
                }
            }
        } finally {
            this.ignoreThemeSelectionEvents = false;
        }
        comboBoxTheme.setSelectedItem(themeFile);
    }

    private static List<File> collectThemeFiles(File path, File root) {
        List<File> result = new ArrayList<File>(1);
        collectThemeFiles(new File(path, "themes"), ".xml", result);
        recursiveCollect(path, root, ".xml", result);
        return result;
    }

    private static void recursiveCollect(final File path, final File root, final String extension, final List<File> result) {
        collectThemeFiles(path, extension, result);
        if (!path.equals(root))
            recursiveCollect(path.getParentFile(), root, extension, result);
    }

    private static void collectThemeFiles(File path, final String extension, final List<File> result) {
        //noinspection ResultOfMethodCallIgnored
        path.listFiles(new FileFilter() {
            public boolean accept(File file) {
                if (file.isFile() && (extension == null || getExtension(file).equals(extension)))
                    result.add(file);
                return true;
            }
        });
    }

    public Component getComponent() {
        return component;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        component = new JPanel();
        component.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 5, new Insets(2, 2, 0, 4), -1, -1));
        component.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Map:");
        panel1.add(label1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        comboBoxMap = new JComboBox();
        panel1.add(comboBoxMap, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Theme:");
        panel1.add(label2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 2, false));
        comboBoxTheme = new JComboBox();
        panel1.add(comboBoxTheme, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        mapViewPanel = new JPanel();
        mapViewPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        component.add(mapViewPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return component;
    }
}
