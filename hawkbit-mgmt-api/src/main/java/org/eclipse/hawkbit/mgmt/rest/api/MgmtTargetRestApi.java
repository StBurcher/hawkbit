/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtActionStatus;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtDistributionSetAssigment;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetAttributes;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetRequestBody;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Api for handling target operations.
 */
@RequestMapping(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING)
public interface MgmtTargetRestApi {

    /**
     * Handles the GET request of retrieving a single target.
     *
     * @param controllerId
     *            the ID of the target to retrieve
     * @return a single target with status OK.
     */

    @RequestMapping(method = RequestMethod.GET, value = "/{controllerId}", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTarget> getTarget(@PathVariable("controllerId") final String controllerId);

    /**
     * Handles the GET request of retrieving all targets.
     *
     * @param pagingOffsetParam
     *            the offset of list of targets for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=name==abc}
     * @return a list of all targets for a defined or default page request with
     *         status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */

    @RequestMapping(method = RequestMethod.GET, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getTargets(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam);

    
    
    /**
     * Handles the POST request of creating new targets. The request body must
     * always be a list of targets.
     *
     * @param targets
     *            the targets to be created.
     * @return In case all targets could successful created the ResponseEntity
     *         with status code 201 with a list of successfully created
     *         entities. In any failure the JsonResponseExceptionHandler is
     *         handling the response.
     */
    @RequestMapping(method = RequestMethod.POST, consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTarget>> createTargets(final List<MgmtTargetRequestBody> targets);

    /**
     * Handles the PUT request of updating a target. The ID is within the URL
     * path of the request. A given ID in the request body is ignored. It's not
     * possible to set fields to {@code null} values.
     *
     * @param controllerId
     *            the path parameter which contains the ID of the target
     * @param targetRest
     *            the request body which contains the fields which should be
     *            updated, fields which are not given are ignored for the
     *            udpate.
     * @return the updated target response which contains all fields also fields
     *         which have not updated
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{controllerId}", consumes = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTarget> updateTarget(@PathVariable("controllerId") final String controllerId,
            final MgmtTargetRequestBody targetRest);

    /**
     * Handles the DELETE request of deleting a target.
     *
     * @param controllerId
     *            the ID of the target to be deleted
     * @return If the given controllerId could exists and could be deleted Http
     *         OK. In any failure the JsonResponseExceptionHandler is handling
     *         the response.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{controllerId}", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> deleteTarget(@PathVariable("controllerId") final String controllerId);

    /**
     * Handles the GET request of retrieving the attributes of a specific
     * target.
     *
     * @param controllerId
     *            the ID of the target to retrieve the attributes.
     * @return the target attributes as map response with status OK
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{controllerId}/attributes", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetAttributes> getAttributes(@PathVariable("controllerId") final String controllerId);

    /**
     * Handles the GET request of retrieving the Actions of a specific target.
     *
     * @param controllerId
     *            to load actions for
     * @param pagingOffsetParam
     *            the offset of list of targets for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=status==pending}
     * @return a list of all Actions for a defined or default page request with
     *         status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{controllerId}/actions", produces = { "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtAction>> getActionHistory(@PathVariable("controllerId") final String controllerId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam);

    /**
     * Handles the GET request of retrieving a specific Actions of a specific
     * Target.
     *
     * @param controllerId
     *            to load the action for
     * @param actionId
     *            to load
     * @return the action
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{controllerId}/actions/{actionId}", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtAction> getAction(@PathVariable("controllerId") final String controllerId,
            @PathVariable("actionId") final Long actionId);

    /**
     * Handles the DELETE request of canceling an specific Actions of a specific
     * Target.
     *
     * @param controllerId
     *            the ID of the target in the URL path parameter
     * @param actionId
     *            the ID of the action in the URL path parameter
     * @param force
     *            optional parameter, which indicates a force cancel
     * @return status no content in case cancellation was successful
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{controllerId}/actions/{actionId}")
    ResponseEntity<Void> cancelAction(@PathVariable("controllerId") final String controllerId,
            @PathVariable("actionId") final Long actionId,
            @RequestParam(value = "force", required = false, defaultValue = "false") final boolean force);

    /**
     * Handles the GET request of retrieving the ActionStatus of a specific
     * target and action.
     *
     * @param controllerId
     *            of the the action
     * @param actionId
     *            of the status we are intend to load
     * @param pagingOffsetParam
     *            the offset of list of targets for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @return a list of all ActionStatus for a defined or default page request
     *         with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{controllerId}/actions/{actionId}/status", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtActionStatus>> getActionStatusList(
            @PathVariable("controllerId") final String controllerId, @PathVariable("actionId") final Long actionId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam);

    /**
     * Handles the GET request of retrieving the assigned distribution set of an
     * specific target.
     *
     * @param controllerId
     *            the ID of the target to retrieve the assigned distribution
     * @return the assigned distribution set with status OK, if none is assigned
     *         than {@code null} content (e.g. "{}")
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{controllerId}/assignedDS", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSet> getAssignedDistributionSet(
            @PathVariable("controllerId") final String controllerId);

    /**
     * Changes the assigned distribution set of a target.
     *
     * @param controllerId
     *            of the target to change
     * @param dsId
     *            of the distributionset that is to be assigned
     * @return http status
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{controllerId}/assignedDS", consumes = {
            "application/hal+json",
            MediaType.APPLICATION_JSON_VALUE }, produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> postAssignedDistributionSet(@PathVariable("controllerId") final String controllerId,
            final MgmtDistributionSetAssigment dsId);

    /**
     * Handles the GET request of retrieving the installed distribution set of
     * an specific target.
     *
     * @param controllerId
     *            the ID of the target to retrieve
     * @return the assigned installed set with status OK, if none is installed
     *         than {@code null} content (e.g. "{}")
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{controllerId}/installedDS", produces = {
            "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSet> getInstalledDistributionSet(
            @PathVariable("controllerId") final String controllerId);
    
    /**
     * Changes the assigned distribution set of a target.
     *
     * @param controllerId
     *            of the target to change
     * @param dsId
     *            of the distributionset that is to be assigned
     * @return http status
     */
    @RequestMapping(method = RequestMethod.POST, value = "/{controllerId}/actions/{actionId}", produces = { "application/hal+json", MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> postStartUpdate(@PathVariable("controllerId") final String controllerId,
    		@PathVariable("actionId") final Long actionId, @RequestParam(value = "force", required = false, defaultValue = "false") final boolean force);

}
