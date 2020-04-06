package be.nabu.eai.module.services.profile;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class RunProfileArtifactManager extends JAXBArtifactManager<RunProfileConfiguration, RunProfile> {
	
	public RunProfileArtifactManager() {
		super(RunProfile.class);
	}

	@Override
	protected RunProfile newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new RunProfile(id, container, repository);
	}

}
