package be.nabu.eai.module.services.profile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.services.profile.RunProfileConfiguration.ServiceConfiguration;
import be.nabu.eai.module.services.profile.RunProfileConfiguration.ServiceProfile;
import be.nabu.eai.repository.api.Feature;
import be.nabu.eai.repository.api.FeaturedArtifact;
import be.nabu.eai.repository.api.FeaturedExecutionContext;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.impl.FeatureImpl;
import be.nabu.libs.cache.api.Cache;
import be.nabu.libs.cache.api.CacheProvider;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.evaluator.EvaluationException;
import be.nabu.libs.evaluator.PathAnalyzer;
import be.nabu.libs.evaluator.QueryParser;
import be.nabu.libs.evaluator.impl.VariableOperation;
import be.nabu.libs.evaluator.types.api.TypeOperation;
import be.nabu.libs.evaluator.types.operations.TypesOperationProvider;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.xml.XMLBinding;

public class RunProfile extends JAXBArtifact<RunProfileConfiguration> implements CacheProvider, FeaturedArtifact {

	private Map<String, TypeOperation> analyzedOperations = new HashMap<String, TypeOperation>();
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public RunProfile(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "run-profiles.xml", RunProfileConfiguration.class);
	}
	
	protected TypeOperation getOperation(String query) throws ParseException {
		if (!analyzedOperations.containsKey(query)) {
			synchronized(analyzedOperations) {
				if (!analyzedOperations.containsKey(query))
					analyzedOperations.put(query, (TypeOperation) new PathAnalyzer<ComplexContent>(new TypesOperationProvider()).analyze(QueryParser.getInstance().parse(query)));
			}
		}
		return analyzedOperations.get(query);
	}
	
	protected Object getVariable(ComplexContent pipeline, String query) throws ServiceException {
		VariableOperation.registerRoot();
		try {
			return getOperation(query).evaluate(pipeline);
		}
		catch (EvaluationException e) {
			throw new RuntimeException(e);
		}
		catch (ParseException e) {
			throw new RuntimeException(e);
		}
		finally {
			VariableOperation.unregisterRoot();
		}
	}

	@Override
	public Cache get(String name) throws IOException {
		List<String> features = ServiceRuntime.getRuntime() != null && ServiceRuntime.getRuntime().getExecutionContext() instanceof FeaturedExecutionContext 
			? ((FeaturedExecutionContext) ServiceRuntime.getRuntime().getExecutionContext()).getEnabledFeatures() 
			: null;
		if (features != null && features.contains(getId())) {
			List<ServiceProfile> profiles = getConfig().getProfiles();
			if (profiles != null) {
				for (ServiceProfile profile : profiles) {
					// if we have a profile for this service and at least one configuration, return it
					if (profile.getService() != null && profile.getService().getId().equals(name) && profile.getConfigurations() != null) {
						return new Cache() {
							@Override
							public boolean put(Object key, Object value) throws IOException {
								// no can do
								return true;
							}
							@Override
							public Object get(Object key) throws IOException {
								try {
									// we expect the service input here
									if (key instanceof ComplexContent || key == null) {
										for (ServiceConfiguration configuration : profile.getConfigurations()) {
											boolean matches = true;
											// a null key can only match with no input queries
											if (key == null) {
												matches = configuration.getInputQueries() == null || configuration.getInputQueries().isEmpty();
											}
											else if (configuration.getInputQueries() != null) {
												for (String inputQuery : configuration.getInputQueries()) {
													Object result = getVariable((ComplexContent) key, inputQuery);
													if (result != null) {
														Boolean booleanResult = result instanceof Boolean
															? (Boolean) result
															: ConverterFactory.getInstance().getConverter().convert(result, Boolean.class);
														// if we can't convert it to a boolean directly, we assume any non-null value is true and null itself is false
														// this should be consistent with blox step conditions
														if (booleanResult == null) {
															booleanResult = result != null;
														}
														matches &= booleanResult;
													}
												}
											}
											// if we have a match, return the stored output
											if (matches) {
												if (configuration.getErrorCode() != null) {
													throw new ServiceException(configuration.getErrorCode(), configuration.getErrorMessage());
												}
												else if (configuration.getOutput() == null || configuration.getOutput().trim().isEmpty()) {
													return null;
												}
												XMLBinding binding = new XMLBinding(profile.getService().getServiceInterface().getOutputDefinition(), Charset.forName("UTF-8"));
												binding.setIgnoreUndefined(true);
												return binding.unmarshal(new ByteArrayInputStream(configuration.getOutput().getBytes(Charset.forName("UTF-8"))), new Window[0]);
											}
										}
									}
								}
								catch (ServiceException e) {
									throw new IOException(e);
								}
								catch (Exception e) {
									logger.error("Could not establish runtime profile for " + getId() + " service " + profile.getService().getId(), e);
								}
								return null;
							}
							@Override
							public void clear(Object key) throws IOException {
								// no can do
							}
							@Override
							public void clear() throws IOException {
								// no can do
							}
							@Override
							public void prune() throws IOException {
								// no can do
							}
							@Override
							public void refresh() throws IOException {
								// no can do
							}
							@Override
							public void refresh(Object key) throws IOException {
								// no can do
							}
						};
					}
					break;
				}
			}
		}
		return null;
	}

	@Override
	public void remove(String name) throws IOException {
		// no can do
	}

	@Override
	public List<Feature> getAvailableFeatures() {
		return Arrays.asList(new FeatureImpl(getId(), getConfig().getDescription()));
	}
}
