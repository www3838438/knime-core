/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.viz.property.color;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.colorchooser.AbstractColorChooserPanel;

/**
 * A default panel to show two color palettes.
 *
 * @author Johannes Schweig, KNIME AG
 * @since 3.5
 */
public class DefaultPalettesColorPanel extends AbstractColorChooserPanel implements ActionListener{
    /**
     *
     */
    private static final String[] set1Colors = {"#8dd3c7","#ffffb3","#bebada","#fb8072","#80b1d3","#fdb462","#b3de69","#fccde5","#d9d9d9","#bc80bd","#ccebc5","#ffed6f"};
    private static final String[] set2Colors = {"#a6cee3","#1f78b4","#b2df8a","#33a02c","#fb9a99","#e31a1c","#fdbf6f","#ff7f00","#cab2d6","#6a3d9a","#ffff99","#b15928"};
    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildChooser() {
        super.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        //JPanels
        JPanel set1Panel = new JPanel(new GridLayout(1,12));
        set1Panel.setAlignmentX(LEFT_ALIGNMENT);
        JPanel set2Panel = new JPanel(new GridLayout(1, 12));
        set2Panel.setAlignmentX(LEFT_ALIGNMENT);
        /**
         * Overwrites the default JButton to notify the ColorSelectionModel of changes.
         *
         * @author Johannes Schweig
         */
        class PaletteButton extends JButton{
            private Color color;
            private static final int SIZE = 30;

            PaletteButton(final String c){
                color = Color.decode(c);
                setPreferredSize(new Dimension(SIZE,SIZE));
                setBackground(getColor());
                setForeground(getColor());
                setBorderPainted(false);
                setFocusPainted(false);
                addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        getColorSelectionModel().setSelectedColor(getColor());
                    }
                });
            }

            private Color getColor(){
                return color;
            }
        }

        for(int i=0;i<12;i++){
            set1Panel.add(new PaletteButton(set1Colors[i]));
            set2Panel.add(new PaletteButton(set2Colors[i]));
        }


        //JLabels
        JLabel set1Label = new JLabel("Set 1");
        set1Label.setFont(new Font(set1Label.getFont().getName(), Font.PLAIN, set1Label.getFont().getSize()+2));
        JLabel set2Label = new JLabel("Set 2");
        set2Label.setFont(new Font(set2Label.getFont().getName(), Font.PLAIN, set2Label.getFont().getSize()+2));


        //add panels to layout
        super.add(set1Label);
        super.add(Box.createVerticalStrut(5));
        super.add(set1Panel);
        super.add(Box.createVerticalStrut(20));
        super.add(set2Label);
        super.add(Box.createVerticalStrut(5));
        super.add(set2Panel);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return "Palettes";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMnemonic() {
        return KeyEvent.VK_P;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDisplayedMnemonicIndex() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Icon getLargeDisplayIcon() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Icon getSmallDisplayIcon() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateChooser() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        // TODO Auto-generated method stub

    }
}
