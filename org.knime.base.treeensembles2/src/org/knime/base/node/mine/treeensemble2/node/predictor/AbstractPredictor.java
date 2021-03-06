/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   25.10.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.predictor;

import org.knime.base.node.mine.treeensemble2.model.AbstractTreeEnsembleModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Abstract implementation of the {@link Predictor} interface.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <M> the type of model
 * @param <S> the type of model spec
 * @param <C> the type of configuration
 */
public abstract class AbstractPredictor
<M extends AbstractTreeEnsembleModel, S extends PortObjectSpec, C extends PredictorConfiguration>
implements Predictor<M, S, C> {

    private final M m_model;
    private final C m_config;
    private final S m_spec;
    private final ColumnRearranger m_predictionRearranger;
    private final DataTableSpec m_dataSpec;

    /**
     * @param model  to use for prediction
     * @param modelSpec corresponding to <b>model</b>
     * @param dataTableSpec of the input table
     * @param configuration of the predictor node
     * @throws InvalidSettingsException if the settings are invalid
     *
     */
    public AbstractPredictor(final M model, final S modelSpec, final DataTableSpec dataTableSpec,
        final C configuration) throws InvalidSettingsException {
        m_model = model;
        m_spec = modelSpec;
        m_dataSpec = dataTableSpec;
        m_config = configuration;
        m_predictionRearranger = createPredictionRearranger();
    }

    /**
     * @return the {@link ColumnRearranger} to use for prediction
     * @throws InvalidSettingsException if the settings are invalid
     */
    protected abstract ColumnRearranger createPredictionRearranger() throws InvalidSettingsException;

    @Override
    public ColumnRearranger getPredictionRearranger() {
        return m_predictionRearranger;
    }

    @Override
    public DataTableSpec getDataSpec() {
        return m_dataSpec;
    }

    @Override
    public M getModel() {
        return m_model;
    }

    @Override
    public S getModelSpec() {
        return m_spec;
    }

    @Override
    public C getConfiguration() {
        return m_config;
    }

}
