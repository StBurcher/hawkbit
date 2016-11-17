/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.distributionset;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Definition of the Action type for the REST management API.
 *
 */
public enum MgmtUpdateType {
    /**
     * The soft action type.
     */
	COMBINED("combined"),

    /**
     * The forced action type.
     */
    SEPARATED("separated");

    private final String name;

    private MgmtUpdateType(final String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

}
