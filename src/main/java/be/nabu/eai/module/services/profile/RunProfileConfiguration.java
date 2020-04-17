package be.nabu.eai.module.services.profile;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "runProfile")
public class RunProfileConfiguration {
	public static class ServiceConfiguration {
		private List<String> inputQueries;
		private String output;
		private String errorMessage, errorCode;
		public List<String> getInputQueries() {
			return inputQueries;
		}
		public void setInputQueries(List<String> inputQueries) {
			this.inputQueries = inputQueries;
		}
		public String getOutput() {
			return output;
		}
		public void setOutput(String output) {
			this.output = output;
		}
		public String getErrorMessage() {
			return errorMessage;
		}
		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}
		public String getErrorCode() {
			return errorCode;
		}
		public void setErrorCode(String errorCode) {
			this.errorCode = errorCode;
		}
		
	}
	
	public static class ServiceProfile {
		private DefinedService service;
		private List<ServiceConfiguration> configurations;
		public List<ServiceConfiguration> getConfigurations() {
			return configurations;
		}
		public void setConfigurations(List<ServiceConfiguration> configurations) {
			this.configurations = configurations;
		}
		@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
		public DefinedService getService() {
			return service;
		}
		public void setService(DefinedService service) {
			this.service = service;
		}
	}
	
	private List<RunProfile> imports;
	private List<ServiceProfile> profiles;
	private String description;

	public List<ServiceProfile> getProfiles() {
		return profiles;
	}
	public void setProfiles(List<ServiceProfile> profiles) {
		this.profiles = profiles;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public List<RunProfile> getImports() {
		return imports;
	}
	public void setImports(List<RunProfile> imports) {
		this.imports = imports;
	}
	
}
