package org.eclipse.hawkbit.repository.jpa.eventbus;

import java.util.Map;

import org.eclipse.hawkbit.eventbus.EventSubscriber;
import org.eclipse.hawkbit.eventbus.event.Event;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetUpdateEvent;
import org.eclipse.hawkbit.repository.eventbus.event.TargetInfoUpdateEvent;
import org.eclipse.hawkbit.repository.jpa.TargetInfoRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInfo;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

@EventSubscriber
@Service
public class TargetEventHandler {

	@Autowired
	private DistributionSetManagement distributionSetManagement;
	@Autowired
	private SystemSecurityContext systemSecurityContext;
	@Autowired
	private TargetInfoRepository targetInfoRepository;

	@Subscribe
	@AllowConcurrentEvents
	public void onEvent(final Event event) {
		if (event instanceof TargetInfoUpdateEvent) {
			JpaTargetInfo info = (JpaTargetInfo) ((TargetInfoUpdateEvent) event).getEntity();
			Map<String, String> attributesMap = info.getControllerAttributes();
			if (attributesMap.containsKey("DSInformation") == true) {
				
				DistributionSet set = systemSecurityContext.runAsSystem(() -> distributionSetManagement.findDistributionSetByNameAndVersion(
                        attributesMap.get("installedName"), attributesMap.get("installedVersion")));
				if (set != null) {
					info.setInstalledDistributionSet((JpaDistributionSet) set);
					attributesMap.remove("installedName");
					attributesMap.remove("installedVersion");
					attributesMap.remove("DSInformation");
					targetInfoRepository.save(info);
				} else {
					
				}
			}
		} else if (event instanceof DistributionSetUpdateEvent) {
			System.out.println("Update DS");
		}
	}
}
