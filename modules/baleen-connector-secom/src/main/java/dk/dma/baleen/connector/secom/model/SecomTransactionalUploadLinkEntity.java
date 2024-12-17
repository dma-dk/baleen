/*
 * Copyright (c) 2008 Kasper Nielsen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dma.baleen.connector.secom.model;

import java.time.Instant;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Represents a link that has been uploaded to a consumer. And retrieved via getLink at a later point in time.
 * <p>
 * We don't maintain any information about whether a link has actually been accessed. A client may do so multiple times.
 *
 * @see
 */
@Entity
@Table(name = "secom_transactional_upload_link_entity")
public class SecomTransactionalUploadLinkEntity extends SecomTransactionalEntity {

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(length = Integer.MAX_VALUE, columnDefinition = "BINARY")
    private byte[] data;

    @Column
    private Instant expiresAt;

    @Column
    private int linkSize;

    /**
     * @return the data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * @return the expiresAt
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * @param expiresAt the expiresAt to set
     */
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * @return the linkSize
     */
    public int getLinkSize() {
        return linkSize;
    }

    /**
     * @param linkSize the linkSize to set
     */
    public void setLinkSize(int linkSize) {
        this.linkSize = linkSize;
    }


}
