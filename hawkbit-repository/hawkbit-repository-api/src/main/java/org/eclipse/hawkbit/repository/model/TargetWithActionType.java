/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.UpdateType;

/**
 * A custom view on {@link Target} with {@link ActionType}.
 * 
 *
 *
 */
public class TargetWithActionType {

    private final String targetId;
    private final ActionType actionType;
    private final UpdateType updateType;
    private final long forceTime;

    /**
     * @param targetId
     * @param actionType
     * @param forceTime
     */
//    public TargetWithActionType(final String targetId, final ActionType actionType, final long forceTime) {
//        this.targetId = targetId;
//        this.actionType = actionType;
//        this.forceTime = forceTime;
//    }
    
    /**
     * @param targetId
     * @param actionType
     * @param forceTime
     */
    public TargetWithActionType(final String targetId, final ActionType actionType, final long forceTime, final UpdateType updateType) {
        this.targetId = targetId;
        this.actionType = actionType;
        this.forceTime = forceTime;
        this.updateType = updateType;
    }

    /**
     * @return the actionType
     */
    public ActionType getActionType() {
        if (actionType != null) {
            return actionType;
        }
        // default value
        return ActionType.FORCED;
    }

    /**
     * @return the forceTime
     */
    public long getForceTime() {
        if (actionType == ActionType.TIMEFORCED) {
            return forceTime;
        }
        return RepositoryModelConstants.NO_FORCE_TIME;
    }

    /**
     * @return the targetId
     */
    public String getTargetId() {
        return targetId;
    }

	public UpdateType getUpdateType() {
		return updateType;
	}
}
