/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.cache;

import java.math.RoundingMode;

import org.eclipse.hawkbit.repository.eventbus.event.DownloadProgressEvent;
import org.eclipse.hawkbit.repository.eventbus.event.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.EventBus;
import com.google.common.math.DoubleMath;

/**
 * An service which combines the functionality for functional use cases to write
 * into the cache an notify the writing to the cache to the {@link EventBus}.
 *
 *
 *
 */
@Service
public class CacheWriteNotify {
    private static final int DOWNLOAD_PROGRESS_MAX = 100;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private TenantAware tenantAware;

    /**
     * writes the download progress into the cache
     * {@link CacheKeys#DOWNLOAD_PROGRESS_PERCENT} and notifies the
     * {@link EventBus} with a {@link DownloadProgressEvent}.
     * 
     * @param statusId
     *            the ID of the {@link ActionStatus}
     * @param requestedBytes
     *            requested bytes of the request
     * @param shippedBytesSinceLast
     *            since last event
     * @param shippedBytesOverall
     *            for the download request
     */
    public void downloadProgress(final Long statusId, final Long requestedBytes, final Long shippedBytesSinceLast,
            final Long shippedBytesOverall) {

        final Cache cache = cacheManager.getCache(ActionStatus.class.getName());
        final String cacheKey = CacheKeys.entitySpecificCacheKey(String.valueOf(statusId),
                CacheKeys.DOWNLOAD_PROGRESS_PERCENT);

        final int progressPercent = DoubleMath.roundToInt(shippedBytesOverall * 100.0 / requestedBytes,
                RoundingMode.DOWN);

        if (progressPercent < DOWNLOAD_PROGRESS_MAX) {
            cache.put(cacheKey, progressPercent);
        } else {
            // in case we reached progress 100 delete the cache value again
            // because otherwise he will
            // keep there forever
            cache.evict(cacheKey);
        }

        eventBus.post(new DownloadProgressEvent(tenantAware.getCurrentTenant(), statusId, requestedBytes,
                shippedBytesSinceLast, shippedBytesOverall));
    }

    /**
     * Writes the {@link CacheKeys#ROLLOUT_GROUP_CREATED} and
     * {@link CacheKeys#ROLLOUT_GROUP_TOTAL} into the cache and notfies the
     * {@link EventBus} with a {@link RolloutGroupCreatedEvent}.
     * 
     * @param revision
     *            the revision of the event
     * @param rolloutId
     *            the ID of the rollout the group has been created
     * @param rolloutGroupId
     *            the ID of the rollout group which has been created
     * @param totalRolloutGroup
     *            the total number of rollout groups for this rollout
     * @param createdRolloutGroup
     *            the number of already created groups of the rollout
     */
    public void rolloutGroupCreated(final long revision, final Long rolloutId, final Long rolloutGroupId,
            final int totalRolloutGroup, final int createdRolloutGroup) {

        final Cache cache = cacheManager.getCache(Rollout.class.getName());
        final String cacheKeyGroupTotal = CacheKeys.entitySpecificCacheKey(String.valueOf(rolloutId),
                CacheKeys.ROLLOUT_GROUP_TOTAL);
        final String cacheKeyGroupCreated = CacheKeys.entitySpecificCacheKey(String.valueOf(rolloutId),
                CacheKeys.ROLLOUT_GROUP_CREATED);
        if (createdRolloutGroup < totalRolloutGroup) {
            cache.put(cacheKeyGroupTotal, totalRolloutGroup);
            cache.put(cacheKeyGroupCreated, createdRolloutGroup);
        } else {
            // in case we reached progress 100 delete the cache value again
            // because otherwise he will keep there forever
            cache.evict(cacheKeyGroupTotal);
            cache.evict(cacheKeyGroupCreated);
        }
        eventBus.post(new RolloutGroupCreatedEvent(tenantAware.getCurrentTenant(), revision, rolloutId, rolloutGroupId,
                totalRolloutGroup, createdRolloutGroup));
    }

    /**
     * @param cacheManager
     *            the cacheManager to set
     */
    void setCacheManager(final CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * @param eventBus
     *            the eventBus to set
     */
    void setEventBus(final EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * @param tenantAware
     *            the tenantAware to set
     */
    void setTenantAware(final TenantAware tenantAware) {
        this.tenantAware = tenantAware;
    }
}
