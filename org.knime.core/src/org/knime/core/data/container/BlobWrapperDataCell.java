/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Dec 15, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.IOException;
import java.lang.ref.SoftReference;

import org.knime.core.data.DataCell;
import org.knime.core.data.container.BlobDataCell.BlobAddress;

/**
 * Wrapper for {@link BlobDataCell}. We explicitly wrap those cells in this
 * package to delay the access to the latest time possible (when someone
 * calls getCell() on the row). If such a cell has been added to an table
 * (by calling e.g. 
 * {@link org.knime.core.node.BufferedDataContainer#addRowToTable(
 * org.knime.core.data.DataRow) addRowToTable in a DataContainer}, 
 * the framework will write the underlying blob to a dedicated file and 
 * internally link to this blob file. The blob object itself can be garbage
 * collected and will silently re-instantiated if accessed.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class BlobWrapperDataCell extends DataCell {
    
    private Buffer m_buffer;
    private BlobAddress m_blobAddress;
    private final Class<? extends BlobDataCell> m_blobClass;
    private SoftReference<BlobDataCell> m_cellRef;
    private BlobDataCell m_hardCellRef;
    
    /**
     * Keeps references.
     * @param b The buffer that owns the cell.
     * @param ba Its address.
     * @param cl The class of the blob.
     */
    BlobWrapperDataCell(final Buffer b, final BlobAddress ba,
            final Class<? extends BlobDataCell> cl) {
        assert b.getBufferID() == ba.getBufferID();
        m_buffer = b;
        m_blobAddress = ba;
        m_blobClass = cl;
    }
    
    /**
     * Create a new wrapper cell for a blob object. The wrapper will keep a
     * hard reference to the cell unless the cell is added to data container,
     * in which case the reference is softened. 
     * @param cell The cell to wrap.
     */
    public BlobWrapperDataCell(final BlobDataCell cell) {
        m_blobClass = cell.getClass(); 
        m_blobAddress = cell.getBlobAddress();
        m_cellRef = new SoftReference<BlobDataCell>(cell);
        m_hardCellRef = cell;
    }
    
    /**
     * Fetches the content of the blob cell. May last long.
     * @return The blob Data cell being read.
     */
    public BlobDataCell getCell() {
        if (m_hardCellRef != null) {
            return m_hardCellRef;
        }
        BlobDataCell cell = m_cellRef != null ? m_cellRef.get() : null;
        if (cell == null) {
            try {
                cell = m_buffer.readBlobDataCell(m_blobAddress, m_blobClass);
                m_cellRef = new SoftReference<BlobDataCell>(cell);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
        return cell;
    }
    
    /** Framework method to set buffer and address.
     * @param address Address to set.
     * @param buffer Owner buffer to set.
     */
    void setAddressAndBuffer(final BlobAddress address, final Buffer buffer) {
        if (address == null || buffer == null) {
            throw new NullPointerException("Args must not be null");
        }
        if (m_blobAddress != null && !m_blobAddress.equals(address)) {
            throw new IllegalStateException("Blob wrapper cell has already "
                    + "an assigned blob address (" + m_blobAddress 
                    + ", new one is " + address + ")");
        }
        if (m_buffer == null) {
            assert m_hardCellRef != null 
            : "Should not keep hard reference when buffer is not available.";
            m_hardCellRef.setBlobAddress(address);
            m_blobAddress = address;
            m_buffer = buffer;
            m_hardCellRef = null;
        }
    }
    
    /** @return The blob address. */
    BlobAddress getAddress() {
        return m_blobAddress;
    }
    
    /** @return Blob class. */
    public Class<? extends BlobDataCell> getBlobClass() {
        return m_blobClass;
    }
    
    /** @return owning buffer or null if not set yet. */
    Buffer getBuffer() {
        return m_buffer;
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return getCell().equals(dc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getCell().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getCell().toString();
    }

}
