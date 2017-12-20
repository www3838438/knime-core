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
import java.awt.FlowLayout;
import java.awt.Font;
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

    private static final String[] set1Colors = {"#fb8072", "#bc80bd", "#b3de69", "#80b1d3", "#fdb462", "#8dd3c7", "#bebada", "#ffed6f", "#ccebc5", "#d9d9d9", "#fccde5", "#ffffb3"};
    private static final String[] set2Colors = {"#33a02c", "#e31a1c", "#b15928", "#6a3d9a", "#1f78b4", "#ff7f00", "#b2df8a", "#fdbf6f", "#fb9a99", "#cab2d6", "#a6cee3", "#ffff99"};
    private JButton set1Button = new JButton("Apply to columns");
    private JButton set2Button = new JButton("Apply to columns");
    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildChooser() {
        super.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        //JPanels
        JPanel set1Panel = new JPanel(new FlowLayout());
        set1Panel.setAlignmentX(LEFT_ALIGNMENT);
        JPanel set2Panel = new JPanel(new FlowLayout());
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
        //JButtons Apply
        set1Panel.add(new JPanel());
        set1Panel.add(set1Button);
        set2Panel.add(new JPanel());
        set2Panel.add(set2Button);

        //JLabels
        JLabel set1Label = new JLabel("Default");
        set1Label.setFont(new Font(set1Label.getFont().getName(), Font.PLAIN, set1Label.getFont().getSize()+2));
        JLabel set2Label = new JLabel("Paired");
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
     * @param al1 the action listener for the first button
     * @param al2 the action listener for the second button
     */
    public void addActionListeners(final ActionListener al1, final ActionListener al2){
        if(al1==null || al2==null || set1Button==null || set2Button==null){
            return;
        }
        set1Button.addActionListener(al1);
        set2Button.addActionListener(al2);
    }

    /**
     * @param index
     * @return string array with color values for given index
     */
    public String[] getPalette(final int index){
        if(index==1){
            return set1Colors;
        }else if(index==2){
            return set2Colors;
        }
        return null;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        if(set1Button!=null && set2Button!=null){
            set1Button.setVisible(enabled);
            set2Button.setVisible(enabled);
        }
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
