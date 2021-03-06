/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.event;

import org.eclipse.hawkbit.ui.common.DistributionSetIdName;

/**
 * Event fired on discard of software module assignment.
 * 
 *
 *
 *
 */
public class SoftwareModuleAssignmentDiscardEvent {

    private DistributionSetIdName distributionSetIdName;

    /**
     * 
     * @param distributionSetIdName.
     */
    public SoftwareModuleAssignmentDiscardEvent(final DistributionSetIdName distributionSetIdName) {
        this.distributionSetIdName = distributionSetIdName;
    }

    public DistributionSetIdName getDistributionSetIdName() {
        return distributionSetIdName;
    }

    public void setDistributionSetIdName(final DistributionSetIdName distributionSetIdName) {
        this.distributionSetIdName = distributionSetIdName;
    }

}
