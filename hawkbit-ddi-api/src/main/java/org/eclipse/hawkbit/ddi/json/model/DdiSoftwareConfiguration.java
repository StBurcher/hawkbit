package org.eclipse.hawkbit.ddi.json.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DdiSoftwareConfiguration{
    
	@JsonProperty
    private String assignedName;
    @JsonProperty
    private String assignedVersion;
    @JsonProperty
    private String assignedTypeKey;
	
    @JsonProperty
    private String installedTypeColour;
    @JsonProperty
    private String installedTypeDescription;
    @JsonProperty
    private String installedTypeName;
    @JsonProperty
    private String installedTypeKey;
	@JsonProperty
    private String installedName;
    @JsonProperty
    private String installedVersion;
    @JsonProperty
    private String installedDescription;
    
	public String getAssignedName() {
		return assignedName;
	}

	public void setAssignedName(String assignedName) {
		this.assignedName = assignedName;
	}

	public String getAssignedVersion() {
		return assignedVersion;
	}

	public void setAssignedVersion(String assignedVersion) {
		this.assignedVersion = assignedVersion;
	}

	public String getAssignedTypeKey() {
		return assignedTypeKey;
	}

	public void setAssignedTypeKey(String assignedTypeKey) {
		this.assignedTypeKey = assignedTypeKey;
	}

	public String getInstalledName() {
		return installedName;
	}

	public void setInstalledName(String installedName) {
		this.installedName = installedName;
	}

	public String getInstalledVersion() {
		return installedVersion;
	}

	public void setInstalledVersion(String installedVersion) {
		this.installedVersion = installedVersion;
	}

	public String getInstalledTypeColour() {
		return installedTypeColour;
	}

	public void setInstalledTypeColour(String installedTypeColour) {
		this.installedTypeColour = installedTypeColour;
	}

	public String getInstalledTypeDescription() {
		return installedTypeDescription;
	}

	public void setInstalledTypeDescription(String installedTypeDescription) {
		this.installedTypeDescription = installedTypeDescription;
	}

	public String getInstalledTypeName() {
		return installedTypeName;
	}

	public void setInstalledTypeName(String installedTypeName) {
		this.installedTypeName = installedTypeName;
	}

	public String getInstalledTypeKey() {
		return installedTypeKey;
	}

	public void setInstalledTypeKey(String installedTypeKey) {
		this.installedTypeKey = installedTypeKey;
	}

	public String getInstalledDescription() {
		return installedDescription;
	}

	public void setInstalledDescription(String installedDescription) {
		this.installedDescription = installedDescription;
	}    
}
