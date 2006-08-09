/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.cluster.kmeans;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.DefaultHiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteTranslator;

import org.knime.base.data.append.column.AppendedColumnRow;

/**
 * Generate a clustering using a fixed number of cluster centers and the k-means
 * algorithm. Right now this works only on {@link DataTable}s holding
 * {@link org.knime.core.data.def.DoubleCell}s (or derivatives thereof).
 * 
 * @author Michael Berthold, University of Konstanz
 */
public class ClusterNodeModel extends NodeModel implements HiLiteMapper {
    /** Constant for the RowKey generation and identification in the view. */
    public static final String CLUSTER = "cluster_";

    private static final int INITIAL_NR_CLUSTERS = 3;

    private static final int INITIAL_MAX_ITERATIONS = 99;

    private static final String SETTINGS_FILE_NAME = "kMeansInternalSettings";

    private static final String CFG_NR_OF_CLUSTERS = "nrOfClusters";

    private static final String CFG_COVERAGE = "clusterCoverage";

    private static final String CFG_DIMENSION = "dimensions";

    private static final String CFG_IGNORED_COLS = "ignoredColumns";

    private static final String CFG_CLUSTER = "kMeansCluster";

    // information about the clusters
    private int m_nrClusters; // number of clusters to be used

    private int m_dimension; // dimension of input space

    private int m_nrIgnoredColumns;

    private boolean[] m_ignoreColumn;

    private double[][] m_clusters; // clusters generated by the algorithm

    private int[] m_clusterCoverage; // #patterns covered by each cluster

    // information controlling the algorithm's operation
    private int m_maxNrIterations; // max number of iterations

    // for the hiliting
    private Map<DataCell, Set<DataCell>> m_mapping;

    // mapping from cluster to covering data point

    private HiLiteHandler m_handler;

    private HiLiteTranslator m_translator;

    // predictor params constants

    private DataTableSpec m_spec;

    private DataTableSpec m_appendedSpec;

    private static final String CFG_PROTOTYPES = "prototypes";

    private static final String CFG_PROTOTYPE = "prototype";

    private static final String CFG_USED_COLS = "usedColumns";

    /**
     * Constructor, remember parent and initialize status.
     */
    ClusterNodeModel() {
        super(1, 1, 0, 1); // specify one input, one output and one model
        // output
        m_nrClusters = INITIAL_NR_CLUSTERS;
        m_maxNrIterations = INITIAL_MAX_ITERATIONS;
        m_mapping = new HashMap<DataCell, Set<DataCell>>();
        m_handler = new DefaultHiLiteHandler();
    }

    /**
     * @return the internal hilite handler
     */
    public HiLiteHandler getHiLiteHandler() {
        return m_handler;
    }

    /**
     * @see org.knime.core.node.property.hilite.HiLiteMapper#getKeys(
     *      org.knime.core.data.DataCell)
     */
    public Set<DataCell> getKeys(final DataCell key) {
        if (m_mapping == null) {
            return new HashSet<DataCell>();
        }
        return m_mapping.get(key);
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    public Set<DataCell> keySet() {
        return Collections.unmodifiableSet(m_mapping.keySet());
    }

    /**
     * Appends to the given node settings the model specific configuration, that
     * are, the current settings (e.g. from the
     * {@link org.knime.core.node.NodeDialogPane}), as wells, the
     * {@link NodeModel} itself if applicable.
     * <p>
     * Method is called by the {@link org.knime.core.node.Node} if the
     * current configuration needs to be saved.
     * 
     * @param settings to write into
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        assert (settings != null);
        settings.addInt("nrClusters", m_nrClusters);
        settings.addInt("maxNrIterations", m_maxNrIterations);
    }

    /**
     * Method is called when the {@link NodeModel} before the model has to
     * change it's configuration using the given one. This method is also called
     * by the {@link org.knime.core.node.Node}.
     * 
     * @param settings to validate
     * @throws InvalidSettingsException if a property is not available or
     *             doesn't fit
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        // check values for parameters - simply throw any exceptions
        // from the config object to next caller of this function.
        // number of clusters:
        int newNrClusters = settings.getInt("nrClusters");
        if (!((1 < newNrClusters) && (newNrClusters < 9999))) {
            throw new InvalidSettingsException(
                    "Value out of range for number of"
                            + " clusters, must be in [1,9999]");
        }
        // maximum number of iterations:
        int newMaxNrIterations = settings.getInt("maxNrIterations");
        if (!((1 <= newMaxNrIterations) && (newMaxNrIterations < 9999))) {
            throw new InvalidSettingsException("Value out of range for maximum"
                    + " number of iterations, must be in [1,9999]");
        }
    }

    /**
     * Method is called when the {@link NodeModel} has to set its configuration
     * using the given one. This method is also called by the
     * {@link org.knime.core.node.Node}. Note that the settings should
     * have been validated before this method is called.
     * 
     * @param settings to read from
     * @throws InvalidSettingsException if a property is not available - which
     *             shouldn't happen...
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        // set values for all (previously validated) parameters
        // number of clusters:
        m_nrClusters = settings.getInt("nrClusters");
        // maximum number of iterations:
        m_maxNrIterations = settings.getInt("maxNrIterations");
    }

    /**
     * Get number of clusters.
     * 
     * @return number of clusters
     */
    int getNumClusters() {
        return m_nrClusters;
    }

    /**
     * Set number of clusters.
     * 
     * @param n number of clusters
     */
    void setNumClusters(final int n) {
        m_nrClusters = n;
    }

    /**
     * Get maximum number of iterations for batch mode.
     * 
     * @return maximum number of iterations currently chosen
     */
    int getMaxNumIterations() {
        return m_maxNrIterations;
    }

    /**
     * Set maximum number of iterations for batch mode.
     * 
     * @param i maximum number of iterations
     */
    void setMaxNumIterations(final int i) {
        m_maxNrIterations = i;
    }

    /**
     * Return dimension of feature space (and hence also clusters).
     * 
     * @return dimension of feature space
     */
    int getDimension() {
        return m_dimension;
    }

    /**
     * @return the number of used columns
     */
    int getNrUsedColumns() {
        return m_dimension - m_nrIgnoredColumns;
    }

    /**
     * @return true if the model is executed (and not reset) and cluster centers
     *         are available
     */
    boolean hasModel() {
        return m_clusters != null;
    }

    /**
     * Return prototype vector of cluster c. Do not call if model is not
     * executed or reset.
     * 
     * @param c index of cluster
     * @return array of doubles holding prototype vector
     */
    double[] getClusterCenter(final int c) {
        return m_clusters[c];
    }

    /**
     * Return coverage of a cluster.
     * 
     * @param c index of cluster
     * @return number of patterns covered by a cluster
     */
    int getClusterCoverage(final int c) {
        return m_clusterCoverage[c];
    }

    /**
     * Generate new clustering based on InputDataTable and specified number of
     * clusters. Currently the objective function only looks for cluster centers
     * that are extremely similar to the first n patterns...
     * 
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws Exception {
        assert (data.length == 1);
        // get dimension of feature space
        m_dimension = data[0].getDataTableSpec().getNumColumns();

        initialize(data[0]);
        // --------- create clusters --------------
        // reserve space for cluster center updates (do batch update!)
        double[][] delta = new double[m_nrClusters][];
        for (int c = 0; c < m_nrClusters; c++) {
            delta[c] = new double[m_dimension - m_nrIgnoredColumns];
        }
        // also keep counts of how many patterns fall in a specific cluster
        m_clusterCoverage = new int[m_nrClusters];
        // main loop - until clusters stop changing or maxNrIterations reached
        int currentIteration = 0;
        boolean finished = false;
        while ((!finished) && (currentIteration < m_maxNrIterations)) {
            if (exec != null) {
                exec.checkCanceled();
                exec.setProgress((double)currentIteration
                        / (double)m_maxNrIterations, "Iteration "
                        + currentIteration);
            }
            // initialize counts and cluster-deltas
            for (int c = 0; c < m_nrClusters; c++) {
                m_clusterCoverage[c] = 0;
                delta[c] = new double[m_dimension - m_nrIgnoredColumns];
                int deltaPos = 0;
                for (int i = 0; i < m_dimension; i++) {
                    if (!m_ignoreColumn[i]) {
                        delta[c][deltaPos++] = 0.0;
                    }
                }
            }
            // assume that we are done (i.e. clusters have stopped changing)
            finished = true;
            RowIterator rowIt = data[0].iterator(); // first training example
            int nrOverallPatterns = 0;
            while (rowIt.hasNext()) {
                DataRow currentRow = rowIt.next();
                int winner = findClosestPrototypeFor(currentRow);
                if (winner >= 0) {
                    // update winning cluster centers delta
                    int deltaPos = 0;
                    for (int i = 0; i < m_dimension; i++) {
                        DataCell currentCell = currentRow.getCell(i);
                        if ((!m_ignoreColumn[i])
                                && (!(currentCell.isMissing()))) {
                            delta[winner][deltaPos++] 
                                          += ((DoubleValue)(currentCell))
                                    .getDoubleValue();
                        }
                    }
                    m_clusterCoverage[winner]++;
                } else {
                    // we didn't find any winner - very odd
                    assert (winner >= 0); // let's report this during
                    // debugging!
                    // otherwise just don't reproduce result
                    throw new IllegalStateException("No winner found: "
                            + winner);
                }
                nrOverallPatterns++;
            }
            // update cluster centers
            for (int c = 0; c < m_nrClusters; c++) {
                if (m_clusterCoverage[c] > 0) {
                    // only update clusters who do cover some pattern:
                    int pos = 0;
                    for (int i = 0; i < m_dimension; i++) {
                        if (m_ignoreColumn[i]) {
                            continue;
                        }
                        // normalize delta by nr of covered patterns
                        double newValue = delta[c][pos] / m_clusterCoverage[c];
                        // compare before assigning the value to make sure we
                        // don't stop if things have changed substantially
                        if (Math.abs(m_clusters[c][pos] - newValue) > 1e-10) {
                            finished = false;
                        }
                        m_clusters[c][pos] = newValue;
                        pos++;
                    }
                }
            }
            currentIteration++;
        } // while(!finished & nrIt<maxNrIt)
        DataTable labeledInput = createMappings(data[0]);
        m_translator = new HiLiteTranslator(getHiLiteHandler(), this);
        m_translator.addToHiLiteHandler(getInHiLiteHandler(0));
        return new BufferedDataTable[]{exec.createBufferedDataTable(
                labeledInput, exec)};
    }

    private void initialize(final DataTable input) {
        // Find out which columns we can use (must be Double compatible)
        // Note that, for simplicity, we still use the entire dimensionality
        // for cluster prototypes below and simply ignore useless columns.
        m_ignoreColumn = new boolean[m_dimension];
        m_nrIgnoredColumns = 0;
        for (int i = 0; i < m_dimension; i++) {
            m_ignoreColumn[i] = !(input.getDataTableSpec().getColumnSpec(i)
                    .getType().isCompatible(DoubleValue.class));
            if (m_ignoreColumn[i]) {
                m_nrIgnoredColumns++;
            }
        }
        // initialize matrix of double (nr clusters * input dimension)
        m_clusters = new double[m_nrClusters][];
        for (int c = 0; c < m_nrClusters; c++) {
            m_clusters[c] = new double[m_dimension - m_nrIgnoredColumns];
        }
        // initialize cluster centers with values of first rows in table
        RowIterator rowIt = input.iterator();
        int c = 0;
        while (rowIt.hasNext() && c < m_nrClusters) {
            DataRow currentRow = rowIt.next();
            int pos = 0;
            for (int i = 0; i < currentRow.getNumCells(); i++) {
                if (!m_ignoreColumn[i]) {
                    if (currentRow.getCell(i).isMissing()) {
                        m_clusters[c][pos] = 0;
                        // missing value: replace with zero
                    } else {
                        assert currentRow.getCell(i).getType().isCompatible(
                                DoubleValue.class);
                        DoubleValue currentValue = (DoubleValue)currentRow
                                .getCell(i);
                        m_clusters[c][pos] = currentValue.getDoubleValue();
                    }
                    pos++;
                }
            }
            c++;
        }
    }

    private int findClosestPrototypeFor(final DataRow row) {
        // find closest cluster center
        int winner = -1; // closest cluster so far
        double winnerDistance = Double.MAX_VALUE; // best distance
        for (int c = 0; c < m_nrClusters; c++) {
            double distance = 0.0;
            int pos = 0;
            for (int i = 0; i < m_dimension; i++) {
                DataCell currentCell = row.getCell(i);
                if (!m_ignoreColumn[i]) {
                    if (!currentCell.isMissing()) {
                        assert currentCell.getType().isCompatible(
                                DoubleValue.class);
                        double d = (m_clusters[c][pos] 
                                - ((DoubleValue)(currentCell))
                                .getDoubleValue());
                        if (!Double.isNaN(d)) {
                            distance += d * d;
                        }
                    } else {
                        distance += 0.0; // missing
                    }
                    pos++;
                }
            }
            if (distance < winnerDistance) { // found closer cluster
                winner = c; // make it new winner
                winnerDistance = distance;
            }
        } // for all clusters (find closest one)
        return winner;
    }

    private DataTable createMappings(final DataTable input) {
        m_mapping.clear();

        DataContainer cont = new DataContainer(m_appendedSpec);
        for (DataRow row : input) {
            int winner = findClosestPrototypeFor(row);
            DataCell key = new StringCell(CLUSTER + winner);
            cont.addRowToTable(new AppendedColumnRow(row, key));
            if (m_mapping.get(key) == null) {
                Set<DataCell> set = new HashSet<DataCell>();
                set.add(row.getKey().getId());
                m_mapping.put(key, set);
            } else {
                m_mapping.get(key).add(row.getKey().getId());
            }
        }
        cont.close();
        return cont.getTable();
    }

    /**
     * @see org.knime.core.node.NodeModel#saveModelContent(int,
     *      ModelContentWO)
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        ModelContentWO clusterConfig = predParams
                .addModelContent(CFG_PROTOTYPES);
        String[] colsUsed = new String[m_dimension - m_nrIgnoredColumns];
        int pos = 0;
        for (int i = 0; i < m_spec.getNumColumns(); i++) {
            if (!m_ignoreColumn[i]) {
                colsUsed[pos++] = m_spec.getColumnSpec(i).getName();
            }
        }
        clusterConfig.addStringArray(CFG_USED_COLS, colsUsed);
        for (int c = 0; c < m_nrClusters; c++) {
            clusterConfig.addDoubleArray(CFG_PROTOTYPE + c, m_clusters[c]);
        }
    }

    /**
     * Clears the model.
     * 
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
        // remove the clusters
        m_clusters = null;
        m_mapping = new HashMap<DataCell, Set<DataCell>>();
        m_translator = null;
    }

    /**
     * Returns <code>true</code> always and passes the current input spec to
     * the output spec which is identical to the input specification - after
     * all, we are building cluster centers in the original feature space.
     * 
     * @param inSpecs the specifications of the input port(s) - should be one
     * @return the copied input spec
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) {
        // make sure we are a 1-input
        assert (inSpecs.length == 1);
        // input is output spec with all double compatible values set to
        // Double.

        m_dimension = inSpecs[0].getNumColumns();
        m_spec = inSpecs[0];
        // Find out which columns we can use (must be Double compatible)
        // Note that, for simplicity, we still use the entire dimensionality
        // for cluster prototypes below and simply ignore useless columns.
        boolean[] ignoreColumn = new boolean[m_dimension];
        int nrIgnoredColumns = 0;
        for (int i = 0; i < m_dimension; i++) {
            ignoreColumn[i] = !(inSpecs[0].getColumnSpec(i).getType()
                    .isCompatible(DoubleValue.class));
            if (ignoreColumn[i]) {
                nrIgnoredColumns++;
            }
        }
        // determine the possible values of the appended column
        DataCell[] possibleValues = new DataCell[m_nrClusters];
        for (int i = 0; i < m_nrClusters; i++) {
            DataCell key = new StringCell(CLUSTER + i);
            possibleValues[i] = key;
        }
        // create the domain
        DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(
                possibleValues);
        DataColumnSpecCreator creator = new DataColumnSpecCreator("Cluster",
                StringCell.TYPE);
        creator.setDomain(domainCreator.createDomain());
        // create the appended column spec
        DataColumnSpec labelColSpec = creator.createSpec();
        DataTableSpec appendedSpec = new DataTableSpec(labelColSpec);
        m_appendedSpec = new DataTableSpec(m_spec, appendedSpec);
        return new DataTableSpec[]{m_appendedSpec};
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException {
        File settingsFile = new File(internDir, SETTINGS_FILE_NAME);
        FileInputStream in = new FileInputStream(settingsFile);
        NodeSettingsRO settings = NodeSettings.loadFromXML(in);
        try {
            m_nrClusters = settings.getInt(CFG_NR_OF_CLUSTERS);
            m_dimension = settings.getInt(CFG_DIMENSION);
            m_nrIgnoredColumns = settings.getInt(CFG_IGNORED_COLS);
            m_clusterCoverage = settings.getIntArray(CFG_COVERAGE);
            m_clusters = new double[m_nrClusters][m_dimension];
            for (int i = 0; i < m_nrClusters; i++) {
                m_clusters[i] = settings.getDoubleArray(CFG_CLUSTER + i);
            }
        } catch (InvalidSettingsException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        NodeSettings internalSettings = new NodeSettings("kMeans");
        internalSettings.addInt(CFG_NR_OF_CLUSTERS, m_nrClusters);
        internalSettings.addInt(CFG_DIMENSION, m_dimension);
        internalSettings.addInt(CFG_IGNORED_COLS, m_nrIgnoredColumns);
        internalSettings.addIntArray(CFG_COVERAGE, m_clusterCoverage);
        for (int i = 0; i < m_nrClusters; i++) {
            internalSettings.addDoubleArray(CFG_CLUSTER + i, m_clusters[i]);
        }
        File f = new File(internDir, SETTINGS_FILE_NAME);
        FileOutputStream out = new FileOutputStream(f);
        internalSettings.saveToXML(out);
    }
}
