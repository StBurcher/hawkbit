/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.DistributionSetIdName;

/**
 * Proxy for {@link DistributionSet}.
 *
 *
 */
public class ProxyDistribution {

    private static final long serialVersionUID = -8891449133620645310L;

    private Long distId;

    private String createdDate;

    private String lastModifiedDate;

    private String createdByUser;

    private String modifiedByUser;

    private Boolean isComplete;

    private String nameVersion;

    private Long id;
    private String name;
    private String version;
    private String description;

    /**
     * @return the nameVersion
     */
    public String getNameVersion() {
        return nameVersion;
    }

    public DistributionSetIdName getDistributionSetIdName() {
        return DistributionSetIdName.generate(id, name, version);
    }

    /**
     * @param nameVersion
     *            the nameVersion to set
     */
    public void setNameVersion(final String nameVersion) {
        this.nameVersion = nameVersion;
    }

    public Boolean getIsComplete() {
        return isComplete;
    }

    public void setIsComplete(final Boolean isComplete) {
        this.isComplete = isComplete;
    }

    public Long getDistId() {
        return distId;
    }

    public void setDistId(final Long distId) {
        this.distId = distId;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(final String createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(final String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(final String createdByUser) {
        this.createdByUser = createdByUser;
    }

    public String getModifiedByUser() {
        return modifiedByUser;
    }

    public void setModifiedByUser(final String modifiedByUser) {
        this.modifiedByUser = modifiedByUser;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

}
