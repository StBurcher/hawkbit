/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import org.eclipse.hawkbit.eventbus.event.AbstractDistributedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 * Defines the {@link AbstractDistributedEvent} for deletion of
 * {@link DistributionSet}.
 */
public class DistributionDeletedEvent extends AbstractDistributedEvent {
    private static final long serialVersionUID = -3308850381757843098L;
    private final Long distributionId;

    /**
     * @param tenant
     *            the tenant for this event
     * @param distributionId
     *            the ID of the distribution set which has been deleted
     */
    public DistributionDeletedEvent(final String tenant, final Long distributionId) {
        super(-1, tenant);
        this.distributionId = distributionId;
    }

    public Long getDistributionSetId() {
        return distributionId;
    }
}
