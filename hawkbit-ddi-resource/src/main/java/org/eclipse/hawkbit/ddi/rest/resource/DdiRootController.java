/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.artifact.repository.model.DbArtifact;
import org.eclipse.hawkbit.ddi.json.model.DdiActionFeedback;
import org.eclipse.hawkbit.ddi.json.model.DdiCancel;
import org.eclipse.hawkbit.ddi.json.model.DdiCancelActionToStop;
import org.eclipse.hawkbit.ddi.json.model.DdiChunk;
import org.eclipse.hawkbit.ddi.json.model.DdiConfigData;
import org.eclipse.hawkbit.ddi.json.model.DdiControllerBase;
import org.eclipse.hawkbit.ddi.json.model.DdiDeployment;
import org.eclipse.hawkbit.ddi.json.model.DdiDeployment.HandlingType;
import org.eclipse.hawkbit.ddi.json.model.DdiDeploymentBase;
import org.eclipse.hawkbit.ddi.json.model.DdiResult.FinalResult;
import org.eclipse.hawkbit.ddi.json.model.DdiSoftwareConfiguration;
import org.eclipse.hawkbit.ddi.rest.api.DdiRootControllerRestApi;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.Action.UpdateType;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.eclipse.hawkbit.rest.util.RestResourceConversionHelper;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.util.IpUtil;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

/**
 * The {@link DdiRootController} of the hawkBit server DDI API that is queried
 * by the hawkBit controller in order to pull {@link Action}s that have to be
 * fulfilled and report status updates concerning the {@link Action} processing.
 *
 * Transactional (read-write) as all queries at least update the last poll time.
 */
@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class DdiRootController implements DdiRootControllerRestApi {

	private static final Logger LOG = LoggerFactory.getLogger(DdiRootController.class);
	private static final String GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET = "given action ({}) is not assigned to given target ({}).";

	@Autowired
	private ControllerManagement controllerManagement;

	@Autowired
	private SoftwareManagement softwareManagement;

	@Autowired
	private ArtifactManagement artifactManagement;

	@Autowired
	private HawkbitSecurityProperties securityProperties;

	@Autowired
	private TenantAware tenantAware;

	@Autowired
	private SystemManagement systemManagement;

	@Autowired
	private ArtifactUrlHandler artifactUrlHandler;

	@Autowired
	private RequestResponseContextHolder requestResponseContextHolder;

	@Autowired
	private EntityFactory entityFactory;

	@Override
	public ResponseEntity<List<org.eclipse.hawkbit.ddi.json.model.DdiArtifact>> getSoftwareModulesArtifacts(
			@PathVariable("tenant") final String tenant, @PathVariable("controllerId") final String controllerId,
			@PathVariable("softwareModuleId") final Long softwareModuleId) {
		LOG.debug("getSoftwareModulesArtifacts({})", controllerId);

		final Target target = controllerManagement.updateLastTargetQuery(controllerId, IpUtil
				.getClientIpFromRequest(requestResponseContextHolder.getHttpServletRequest(), securityProperties));

		final SoftwareModule softwareModule = softwareManagement.findSoftwareModuleById(softwareModuleId);

		if (softwareModule == null) {
			LOG.warn("Software module with id {} could not be found.", softwareModuleId);
			throw new EntityNotFoundException("Software module does not exist");

		}

		return new ResponseEntity<>(
				DataConversionHelper.createArtifacts(target, softwareModule, artifactUrlHandler, systemManagement),
				HttpStatus.OK);
	}

	@Override
	public ResponseEntity<DdiControllerBase> getControllerBase(@PathVariable("tenant") final String tenant,
			@PathVariable("controllerId") final String controllerId) {
		LOG.debug("getControllerBase({})", controllerId);

		final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotexist(controllerId, IpUtil
				.getClientIpFromRequest(requestResponseContextHolder.getHttpServletRequest(), securityProperties));

		if (target.getTargetInfo().getUpdateStatus() == TargetUpdateStatus.UNKNOWN) {
			LOG.debug("target with {} extsisted but was in status UNKNOWN -> REGISTERED)", controllerId);
			controllerManagement.updateTargetStatus(target.getTargetInfo(), TargetUpdateStatus.REGISTERED,
					System.currentTimeMillis(), IpUtil.getClientIpFromRequest(
							requestResponseContextHolder.getHttpServletRequest(), securityProperties));
		}

		return new ResponseEntity<>(
				DataConversionHelper.fromTarget(target, controllerManagement.findOldestActiveActionByTarget(target),
						controllerManagement.getPollingTime(), tenantAware),
				HttpStatus.OK);
	}

	@Override
	public ResponseEntity<DdiSoftwareConfiguration> getControllerSoftwareConfig(
			@PathVariable("tenant") final String tenant, @PathVariable("controllerId") final String controllerId) {
		LOG.debug("getControllerSoftwareConfig({})", controllerId);
		final Target target = controllerManagement.findByControllerId(controllerId);
		if (target != null) {
			DdiSoftwareConfiguration setConfig = new DdiSoftwareConfiguration();
			if (target.getTargetInfo().getInstalledDistributionSet() != null) {
				DistributionSet set = target.getTargetInfo().getInstalledDistributionSet();
				setConfig.setInstalledName(set.getName());
				setConfig.setInstalledVersion(set.getVersion());
				setConfig.setInstalledDescription(set.getDescription());
				setConfig.setInstalledTypeDescription(set.getType().getDescription());
				setConfig.setInstalledTypeName(set.getType().getName());
				setConfig.setInstalledTypeKey(set.getType().getKey());
			} else {
				LOG.warn("Installed Software Configuration for controller id {} could not be found.", controllerId);
			}
			if (target.getAssignedDistributionSet() != null) {
				setConfig.setAssignedVersion(target.getAssignedDistributionSet().getVersion());
				setConfig.setAssignedName(target.getAssignedDistributionSet().getName());
				setConfig.setAssignedTypeKey(target.getAssignedDistributionSet().getType().getKey());
			} else {
				LOG.warn("Assigned Software Configuration for controller id {} could not be found.", controllerId);
			}
			// Both Sets not present then error
			if (target.getTargetInfo().getInstalledDistributionSet() == null
					&& target.getAssignedDistributionSet() == null) {
				LOG.warn("Software Configuration for controller id {} could not be found.", controllerId);
				throw new EntityNotFoundException("Software Configuration does not exist");
			} else {
				// Otherwise ok
				return new ResponseEntity<>(setConfig, HttpStatus.OK);
			}
		} else {
			LOG.warn("Controller with id {} could not be found.", controllerId);
			throw new EntityNotFoundException("Controller does not exist");
		}
	}

	@Override
	public ResponseEntity<InputStream> downloadArtifact(@PathVariable("tenant") final String tenant,
			@PathVariable("controllerId") final String controllerId,
			@PathVariable("softwareModuleId") final Long softwareModuleId,
			@PathVariable("fileName") final String fileName) {
		ResponseEntity<InputStream> result;

		final Target target = controllerManagement.updateLastTargetQuery(controllerId, IpUtil
				.getClientIpFromRequest(requestResponseContextHolder.getHttpServletRequest(), securityProperties));
		final SoftwareModule module = softwareManagement.findSoftwareModuleById(softwareModuleId);

		if (checkModule(fileName, module)) {
			LOG.warn("Softare module with id {} could not be found.", softwareModuleId);
			result = new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {

			// Exception squid:S3655 - Optional access is checked in checkModule
			// subroutine
			@SuppressWarnings("squid:S3655")
			final LocalArtifact artifact = module.getLocalArtifactByFilename(fileName).get();

			final DbArtifact file = artifactManagement.loadLocalArtifactBinary(artifact);

			final String ifMatch = requestResponseContextHolder.getHttpServletRequest().getHeader("If-Match");
			if (ifMatch != null && !RestResourceConversionHelper.matchesHttpHeader(ifMatch, artifact.getSha1Hash())) {
				result = new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);
			} else {
				final ActionStatus action = checkAndLogDownload(requestResponseContextHolder.getHttpServletRequest(),
						target, module);
				result = RestResourceConversionHelper.writeFileResponse(artifact,
						requestResponseContextHolder.getHttpServletResponse(),
						requestResponseContextHolder.getHttpServletRequest(), file, controllerManagement,
						action.getId());
			}
		}
		return result;
	}

	private ActionStatus checkAndLogDownload(final HttpServletRequest request, final Target target,
			final SoftwareModule module) {
		final Action action = controllerManagement
				.getActionForDownloadByTargetAndSoftwareModule(target.getControllerId(), module);
		final String range = request.getHeader("Range");

		final ActionStatus statusMessage = entityFactory.generateActionStatus();
		statusMessage.setAction(action);
		statusMessage.setOccurredAt(System.currentTimeMillis());
		statusMessage.setStatus(Status.DOWNLOAD);

		if (range != null) {
			statusMessage.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target downloads range " + range
					+ " of: " + request.getRequestURI());
		} else {
			statusMessage.addMessage(
					RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target downloads " + request.getRequestURI());
		}

		return controllerManagement.addInformationalActionStatus(statusMessage);
	}

	private static boolean checkModule(final String fileName, final SoftwareModule module) {
		return null == module || !module.getLocalArtifactByFilename(fileName).isPresent();
	}

	@Override
	// Exception squid:S3655 - Optional access is checked in checkModule
	// subroutine
	@SuppressWarnings("squid:S3655")
	public ResponseEntity<Void> downloadArtifactMd5(@PathVariable("tenant") final String tenant,
			@PathVariable("controllerId") final String controllerId,
			@PathVariable("softwareModuleId") final Long softwareModuleId,
			@PathVariable("fileName") final String fileName) {
		controllerManagement.updateLastTargetQuery(controllerId, IpUtil
				.getClientIpFromRequest(requestResponseContextHolder.getHttpServletRequest(), securityProperties));

		final SoftwareModule module = softwareManagement.findSoftwareModuleById(softwareModuleId);

		if (checkModule(fileName, module)) {
			LOG.warn("Software module with id {} could not be found.", softwareModuleId);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		try {
			DataConversionHelper.writeMD5FileResponse(fileName, requestResponseContextHolder.getHttpServletResponse(),
					module.getLocalArtifactByFilename(fileName).get());
		} catch (final IOException e) {
			LOG.error("Failed to stream MD5 File", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Override
	public ResponseEntity<DdiDeploymentBase> getControllerBasedeploymentAction(
			@PathVariable("tenant") final String tenant, @PathVariable("controllerId") final String controllerId,
			@PathVariable("actionId") final Long actionId,
			@RequestParam(value = "c", required = false, defaultValue = "-1") final int resource) {
		LOG.debug("getControllerBasedeploymentAction({},{})", controllerId, resource);

		final Target target = controllerManagement.updateLastTargetQuery(controllerId, IpUtil
				.getClientIpFromRequest(requestResponseContextHolder.getHttpServletRequest(), securityProperties));

		final Action action = findActionWithExceptionIfNotFound(actionId);
		if (!action.getTarget().getId().equals(target.getId())) {
			LOG.warn(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (!action.isCancelingOrCanceled()) {

			final List<DdiChunk> chunks = DataConversionHelper.createChunks(target, action, artifactUrlHandler,
					systemManagement);

			final HandlingType handlingType = action.isForce() ? HandlingType.FORCED : HandlingType.ATTEMPT;

			final HandlingType updateHandlingType = action.getStatus() == Status.START
					&& UpdateType.SEPARATED == action.getUpdateType() ? handlingType : HandlingType.SKIP;

			final String updateStatus = action.getUpdateType() == null ? "" : action.getUpdateType().toString();

			final DdiDeploymentBase base = new DdiDeploymentBase(Long.toString(action.getId()),
					new DdiDeployment(handlingType, updateHandlingType, chunks, updateStatus));

			LOG.debug("Found an active UpdateAction for target {}. returning deyploment: {}", controllerId, base);

			controllerManagement.registerRetrieved(action, RepositoryConstants.SERVER_MESSAGE_PREFIX
					+ "Target retrieved update action and should start now the download.");

			return new ResponseEntity<>(base, HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<Void> postBasedeploymentActionFeedback(@Valid @RequestBody final DdiActionFeedback feedback,
			@PathVariable("tenant") final String tenant, @PathVariable("controllerId") final String controllerId,
			@PathVariable("actionId") @NotEmpty final Long actionId) {
		LOG.debug("provideBasedeploymentActionFeedback for target [{},{}]: {}", controllerId, actionId, feedback);

		final Target target = controllerManagement.updateLastTargetQuery(controllerId, IpUtil
				.getClientIpFromRequest(requestResponseContextHolder.getHttpServletRequest(), securityProperties));

		if (!actionId.equals(feedback.getId())) {
			LOG.warn(
					"provideBasedeploymentActionFeedback: action in payload ({}) was not identical to action in path ({}).",
					feedback.getId(), actionId);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		final Action action = findActionWithExceptionIfNotFound(actionId);
		if (!action.getTarget().getId().equals(target.getId())) {
			LOG.warn(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (!action.isActive()) {
			LOG.warn("Updating action {} with feedback {} not possible since action not active anymore.",
					action.getId(), feedback.getId());
			return new ResponseEntity<>(HttpStatus.GONE);
		}

		controllerManagement
				.addUpdateActionStatus(generateUpdateStatus(feedback, controllerId, feedback.getId(), action));

		return new ResponseEntity<>(HttpStatus.OK);

	}

	private ActionStatus generateUpdateStatus(final DdiActionFeedback feedback, final String controllerId,
			final Long actionid, final Action action) {

		final ActionStatus actionStatus = entityFactory.generateActionStatus();
		actionStatus.setAction(action);
		actionStatus.setOccurredAt(System.currentTimeMillis());

		switch (feedback.getStatus().getExecution()) {
		case CANCELED:
			LOG.debug("Controller confirmed cancel (actionid: {}, controllerId: {}) as we got {} report.", actionid,
					controllerId, feedback.getStatus().getExecution());
			actionStatus.setStatus(Status.CANCELED);
			actionStatus.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target confirmed cancelation.");
			break;
		case REJECTED:
			LOG.info("Controller reported internal error (actionid: {}, controllerId: {}) as we got {} report.",
					actionid, controllerId, feedback.getStatus().getExecution());
			actionStatus.setStatus(Status.WARNING);
			actionStatus.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target REJECTED update.");
			break;
		case CLOSED:
			handleClosedUpdateStatus(feedback, controllerId, actionid, actionStatus);
			break;
		case DOWNLOADED:
			LOG.debug("Controller reported intermediate status (actionid: {}, controllerId: {}) as we got {} report.",
					actionid, controllerId, feedback.getStatus().getExecution());
			if(action.getStatus() != Status.START || action.getUpdateType() != UpdateType.SEPARATED) {
				actionStatus.setStatus(Status.DOWNLOADED);	
			}
			actionStatus.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target reported "
					+ feedback.getStatus().getExecution());
			break;
		default:
			handleDefaultUpdateStatus(feedback, controllerId, actionid, actionStatus);
			break;
		}

		action.setStatus(actionStatus.getStatus());

		if (feedback.getStatus().getDetails() != null && !feedback.getStatus().getDetails().isEmpty()) {
			final List<String> details = feedback.getStatus().getDetails();
			for (final String detailMsg : details) {
				actionStatus.addMessage(detailMsg);
			}
		}

		return actionStatus;
	}

	private static void handleDefaultUpdateStatus(final DdiActionFeedback feedback, final String controllerId,
			final Long actionid, final ActionStatus actionStatus) {
		LOG.debug("Controller reported intermediate status (actionid: {}, controllerId: {}) as we got {} report.",
				actionid, controllerId, feedback.getStatus().getExecution());
		actionStatus.setStatus(Status.RUNNING);
		actionStatus.addMessage(
				RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target reported " + feedback.getStatus().getExecution());
	}

	private static void handleClosedUpdateStatus(final DdiActionFeedback feedback, final String controllerId,
			final Long actionid, final ActionStatus actionStatus) {
		LOG.debug("Controller reported closed (actionid: {}, controllerId: {}) as we got {} report.", actionid,
				controllerId, feedback.getStatus().getExecution());
		if (feedback.getStatus().getResult().getFinished() == FinalResult.FAILURE) {
			actionStatus.setStatus(Status.ERROR);
			actionStatus.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target reported CLOSED with ERROR!");
		} else {
			actionStatus.setStatus(Status.FINISHED);
			actionStatus.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target reported CLOSED with OK!");
		}
	}

	@Override
	public ResponseEntity<Void> putConfigData(@Valid @RequestBody final DdiConfigData configData,
			@PathVariable("tenant") final String tenant, @PathVariable("controllerId") final String controllerId) {
		controllerManagement.updateLastTargetQuery(controllerId, IpUtil
				.getClientIpFromRequest(requestResponseContextHolder.getHttpServletRequest(), securityProperties));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@Override
	public ResponseEntity<DdiCancel> getControllerCancelAction(@PathVariable("tenant") final String tenant,
			@PathVariable("controllerId") @NotEmpty final String controllerId,
			@PathVariable("actionId") @NotEmpty final Long actionId) {
		LOG.debug("getControllerCancelAction({})", controllerId);

		final Target target = controllerManagement.updateLastTargetQuery(controllerId, IpUtil
				.getClientIpFromRequest(requestResponseContextHolder.getHttpServletRequest(), securityProperties));

		final Action action = findActionWithExceptionIfNotFound(actionId);
		if (!action.getTarget().getId().equals(target.getId())) {
			LOG.warn(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (action.isCancelingOrCanceled()) {
			final DdiCancel cancel = new DdiCancel(String.valueOf(action.getId()),
					new DdiCancelActionToStop(String.valueOf(action.getId())));

			LOG.debug("Found an active CancelAction for target {}. returning cancel: {}", controllerId, cancel);

			controllerManagement.registerRetrieved(action, RepositoryConstants.SERVER_MESSAGE_PREFIX
					+ "Target retrieved cancel action and should start now the cancelation.");

			return new ResponseEntity<>(cancel, HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	@Override
	public ResponseEntity<Void> postCancelActionFeedback(@Valid @RequestBody final DdiActionFeedback feedback,
			@PathVariable("tenant") final String tenant,
			@PathVariable("controllerId") @NotEmpty final String controllerId,
			@PathVariable("actionId") @NotEmpty final Long actionId) {
		LOG.debug("provideCancelActionFeedback for target [{}]: {}", controllerId, feedback);

		final Target target = controllerManagement.updateLastTargetQuery(controllerId, IpUtil
				.getClientIpFromRequest(requestResponseContextHolder.getHttpServletRequest(), securityProperties));

		if (!actionId.equals(feedback.getId())) {
			LOG.warn(
					"provideBasedeploymentActionFeedback: action in payload ({}) was not identical to action in path ({}).",
					feedback.getId(), actionId);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		final Action action = findActionWithExceptionIfNotFound(actionId);
		if (!action.getTarget().getId().equals(target.getId())) {
			LOG.warn(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		controllerManagement.addCancelActionStatus(
				generateActionCancelStatus(feedback, target, feedback.getId(), action, entityFactory));
		return new ResponseEntity<>(HttpStatus.OK);
	}

	private static ActionStatus generateActionCancelStatus(final DdiActionFeedback feedback, final Target target,
			final Long actionid, final Action action, final EntityFactory entityFactory) {

		final ActionStatus actionStatus = entityFactory.generateActionStatus();
		actionStatus.setAction(action);
		actionStatus.setOccurredAt(System.currentTimeMillis());

		switch (feedback.getStatus().getExecution()) {
		case CANCELED:
			LOG.error(
					"Controller reported cancel for a cancel which is not supported by the server (actionid: {}, controllerId: {}) as we got {} report.",
					actionid, target.getControllerId(), feedback.getStatus().getExecution());
			actionStatus.setStatus(Status.WARNING);
			break;
		case REJECTED:
			LOG.info("Controller rejected the cancelation request (too late) (actionid: {}, controllerId: {}).",
					actionid, target.getControllerId());
			actionStatus.setStatus(Status.WARNING);
			break;
		case CLOSED:
			handleClosedCancelStatus(feedback, actionStatus);
			break;
		default:
			actionStatus.setStatus(Status.RUNNING);
			break;
		}

		action.setStatus(actionStatus.getStatus());

		if (feedback.getStatus().getDetails() != null && !feedback.getStatus().getDetails().isEmpty()) {
			final List<String> details = feedback.getStatus().getDetails();
			for (final String detailMsg : details) {
				actionStatus.addMessage(detailMsg);
			}
		}

		return actionStatus;

	}

	private static void handleClosedCancelStatus(final DdiActionFeedback feedback, final ActionStatus actionStatus) {
		if (feedback.getStatus().getResult().getFinished() == FinalResult.FAILURE) {
			actionStatus.setStatus(Status.ERROR);
		} else {
			actionStatus.setStatus(Status.CANCELED);
		}
	}

	private Action findActionWithExceptionIfNotFound(final Long actionId) {
		final Action findAction = controllerManagement.findActionWithDetails(actionId);
		if (findAction == null) {
			throw new EntityNotFoundException("Action with Id {" + actionId + "} does not exist");
		}
		return findAction;
	}
}
