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
 *
 * History
 *   Jan 21, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class TreeNumericColumnMetaData extends TreeAttributeColumnMetaData {

    /**
     * Provides names in the format "Byte i" for artificial columns created from a byte vector column
     *
     * @param bytePosition
     * @return name for artificial byte column
     */
    public static final String getAttributeNameByte(final int bytePosition) {
        return "Byte " + bytePosition;
    }

    public static final String getAttributeNameDouble(final int doublePosition) {
        return "Double " + doublePosition;
    }

    /**
     * @param attrPosition
     * @param prefix for example Byte or Double
     * @return name for artificial column created from a numeric vector
     */
    public static final String getAttributeName(final int attrPosition, final String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        sb.append(" ").append(attrPosition);
        return sb.toString();
    }

    /**
     * @param attributeName
     */
    public TreeNumericColumnMetaData(final String attributeName) {
        super(attributeName);
    }

    /**
     * @param input
     * @throws IOException
     */
    TreeNumericColumnMetaData(final DataInputStream input) throws IOException {
        super(input);
    }

    /** {@inheritDoc} */
    @Override
        void saveContent(final DataOutputStream output) throws IOException {
        // no internals
    }

}
