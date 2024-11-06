/*
* Copyright (C) 2020 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.services.profile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.nabu.eai.developer.ComplexContentEditor;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.developer.util.EAIDeveloperUtils.PropertiesHandler;
import be.nabu.eai.developer.util.EAIDeveloperUtils.PropertyUpdaterListener;
import be.nabu.eai.module.services.profile.RunProfileConfiguration.ServiceConfiguration;
import be.nabu.eai.module.services.profile.RunProfileConfiguration.ServiceProfile;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.ace.AceEditor;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.xml.XMLBinding;
import javafx.beans.property.BooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class RunProfileArtifactGUIManager extends BaseJAXBGUIManager<RunProfileConfiguration, RunProfile> {

	public RunProfileArtifactGUIManager() {
		super("Run Profile", RunProfile.class, new RunProfileArtifactManager(), RunProfileConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected RunProfile newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		return new RunProfile(entry.getId(), entry.getContainer(), entry.getRepository());
	}
	
	@Override
	public String getCategory() {
		return "Caching";
	}
	
	@Override
	protected void display(RunProfile instance, Pane pane) {
		VBox box = new VBox();
		
		BooleanProperty hasLock = MainController.getInstance().hasLock(instance.getId());
		
		// buttons at the top to add service configurations
		HBox buttons = new HBox();
		buttons.disableProperty().bind(hasLock.not());
		
		buttons.setPadding(new Insets(10));
		
		// then an anchor pane with one entry per service
		// within each anchor pane first a list of queries
		// then an error code + message
		// then a complex editor for the output
		// then finally buttons (to remove mostly)
		Accordion accordion = new Accordion();
		box.getChildren().addAll(buttons, accordion);
		
		// draw the existing profiles
		if (instance.getConfig().getProfiles() != null) {
			for (ServiceProfile profile : instance.getConfig().getProfiles()) {
				drawServiceProfile(instance, accordion, profile, false);
			}
		}
		
		Button create = new Button("Add Service");
		create.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				SimpleProperty<DefinedService> property = new SimpleProperty<DefinedService>("Service", DefinedService.class, true);
				
				EAIDeveloperUtils.buildPopup(MainController.getInstance(), "Add Service", Arrays.asList(property), new PropertiesHandler() {
					@Override
					public void handle(SimplePropertyUpdater updater) {
						DefinedService service = updater.getValue("Service");
						if (service != null) {
							// the same service can only be added once
							if (instance.getConfig().getProfiles() == null) {
								instance.getConfig().setProfiles(new ArrayList<ServiceProfile>());
							}
							boolean found = false;
							for (ServiceProfile profile : instance.getConfig().getProfiles()) {
								if (profile.getService().getId().equals(service.getId())) {
									found = true;
									break;
								}
							}
							if (!found) {
								ServiceProfile serviceProfile = new ServiceProfile();
								serviceProfile.setService(service);
								instance.getConfig().getProfiles().add(serviceProfile);
								drawServiceProfile(instance, accordion, serviceProfile, true);
								MainController.getInstance().setChanged();
							}
						}
					}
				}, false, MainController.getInstance().getActiveStage());
			}
		});
		buttons.getChildren().add(create);
		
		pane.getChildren().add(box);
		AnchorPane.setBottomAnchor(box, 0d);
		AnchorPane.setTopAnchor(box, 0d);
		AnchorPane.setRightAnchor(box, 0d);
		AnchorPane.setLeftAnchor(box, 0d);
	}
	
	
	protected void drawServiceProfile(RunProfile runProfile, Accordion accordion, ServiceProfile profile, boolean open) {
		BooleanProperty hasLock = MainController.getInstance().hasLock(runProfile.getId());
		
		AnchorPane pane = new AnchorPane();
		pane.getStyleClass().add("service-profile-pane");
		TitledPane titledPane = new TitledPane(profile.getService().getId(), pane);
		accordion.getPanes().add(titledPane);
		if (open) {
			accordion.setExpandedPane(titledPane);
		}
		VBox box = new VBox();
		
		pane.getChildren().add(box);
		AnchorPane.setBottomAnchor(box, 0d);
		AnchorPane.setTopAnchor(box, 0d);
		AnchorPane.setRightAnchor(box, 0d);
		AnchorPane.setLeftAnchor(box, 0d);
		
		HBox buttons = new HBox();
		buttons.disableProperty().bind(hasLock.not());
		
		buttons.setPadding(new Insets(10));
		box.getChildren().add(buttons);
		
		Button create = new Button("Add Configuration");
		create.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				if (profile.getConfigurations() == null) {
					profile.setConfigurations(new ArrayList<ServiceConfiguration>());
				}
				ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
				profile.getConfigurations().add(serviceConfiguration);
				drawServiceConfiguration(runProfile, profile, box, serviceConfiguration);
				MainController.getInstance().setChanged();
			}
		});
		buttons.getChildren().add(create);
		
		Button delete = new Button("Delete All");
		delete.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				Confirm.confirm(ConfirmType.QUESTION, "Delete All Configurations", "Are you sure you want to delete all configurations for this service?", new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						runProfile.getConfig().getProfiles().remove(profile);
						accordion.getPanes().remove(titledPane);
						MainController.getInstance().setChanged();
					}
				});
			}
		});
		buttons.getChildren().add(delete);
		
		if (profile.getConfigurations() != null) {
			for (ServiceConfiguration configuration : profile.getConfigurations()) {
				drawServiceConfiguration(runProfile, profile, box, configuration);
			}
		}
	}
	
	protected void drawServiceConfiguration(RunProfile runProfile, ServiceProfile profile, VBox parent, ServiceConfiguration configuration) {
		BooleanProperty hasLock = MainController.getInstance().hasLock(runProfile.getId());
		SimplePropertyUpdater updater = EAIDeveloperUtils.createUpdater(configuration, new PropertyUpdaterListener() {
			@Override
			public boolean updateProperty(Property<?> property, Object value) {
				MainController.getInstance().setChanged();
				return true;
			}
		}, "output");
		updater.setSourceId(runProfile.getId());
		VBox box = new VBox();
		box.setPadding(new Insets(10));
		MainController.getInstance().showProperties(updater, box, false);
		
		HBox buttons = new HBox();
		setComplexContent(runProfile, profile, configuration, box, null, buttons);
		
//		box.getChildren().add(formats);
		
		buttons.disableProperty().bind(hasLock.not());
		buttons.setPadding(new Insets(10));
		Button delete = new Button("Delete Configuration");
		delete.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				Confirm.confirm(ConfirmType.QUESTION, "Delete Configuration", "Are you sure you want to delete this configuration?", new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						profile.getConfigurations().remove(configuration);
						parent.getChildren().remove(box);
						MainController.getInstance().setChanged();
					}
				});
			}
		});
		buttons.getChildren().add(delete);
		
		parent.getChildren().addAll(box, buttons);
	}

	private void setComplexContent(RunProfile runProfile, ServiceProfile profile, ServiceConfiguration configuration, VBox box, String newContent, HBox buttons) {
		ComplexType outputDefinition = profile.getService().getServiceInterface().getOutputDefinition();
		XMLBinding binding = new XMLBinding(outputDefinition, Charset.forName("UTF-8"));
		binding.setIgnoreUndefined(true);
		
		ComplexContent content;
		if (configuration.getOutput() != null && !configuration.getOutput().trim().isEmpty()) {
			try {
				content = binding.unmarshal(new ByteArrayInputStream((newContent == null ? configuration.getOutput() : newContent).getBytes(Charset.forName("UTF-8"))), new Window[0]);
				if (newContent != null) {
					configuration.setOutput(newContent);
					MainController.getInstance().setChanged();
				}
			}
			catch (Exception e) {
				// if we received new content, we failed to parse it, it is invalid, back to xml with you!
				// this is not very clean code but it should work...
				if (newContent != null) {
					setXml(runProfile, profile, configuration, box, newContent, buttons);
					return;
				}
				content = outputDefinition.newInstance();
				MainController.getInstance().notify(e);
			}
		}
		else {
			content = outputDefinition.newInstance();
		}
		final ComplexContent finalContent = content;
		ComplexContentEditor complexContentEditor = new ComplexContentEditor(content, true, runProfile.getRepository()) {
			@Override
			public void update() {
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				try {
					binding.marshal(output, finalContent);
					configuration.setOutput(new String(output.toByteArray(), Charset.forName("UTF-8")));
					MainController.getInstance().setChanged();
				}
				catch (IOException e) {
					MainController.getInstance().notify(e);
				}
				super.update();
			}
		};
		box.getChildren().clear();
		box.getChildren().add(complexContentEditor.getTree());
		
		Button button = new Button("Edit as XML");
		button.setId("asXml");
		button.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				setXml(runProfile, profile, configuration, box, null, buttons);
			}
		});
		Node lookup = buttons.lookup("#asContent");
		if (lookup != null) {
			buttons.getChildren().remove(lookup);
		}
		buttons.getChildren().add(0, button);
		
		// need to manually subtract the padding
		complexContentEditor.getTree().prefWidthProperty().bind(box.widthProperty().subtract(20));
	}
	
	private void setXml(RunProfile runProfile, ServiceProfile profile, ServiceConfiguration configuration, VBox box, String content, HBox buttons) {
		AceEditor editor = new AceEditor();
		editor.setContent("application/xml", content == null ? configuration.getOutput() : content);
		editor.setKeyCombination("commit", new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN));
		editor.subscribe("commit", new EventHandler<Event>() {
			@Override
			public void handle(Event arg0) {
				setComplexContent(runProfile, profile, configuration, box, editor.getContent(), buttons);
			}
		});
		box.getChildren().clear();
		box.getChildren().addAll(editor.getWebView());

		Button button = new Button("Push to content");
		button.setId("asContent");
		button.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				setComplexContent(runProfile, profile, configuration, box, editor.getContent(), buttons);
			}
		});
		Node lookup = buttons.lookup("#asXml");
		if (lookup != null) {
			buttons.getChildren().remove(lookup);
		}
		// you can get redirect back here very fast, we don't want to the button added multiple times
		lookup = buttons.lookup("#asContent");
		if (lookup != null) {
			buttons.getChildren().remove(lookup);
		}
		buttons.getChildren().add(0, button);
	}
}
