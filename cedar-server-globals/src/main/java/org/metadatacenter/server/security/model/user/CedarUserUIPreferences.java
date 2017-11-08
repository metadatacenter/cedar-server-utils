package org.metadatacenter.server.security.model.user;

public class CedarUserUIPreferences {

  private CedarUserUIFolderView folderView;

  private CedarUserUIResourceTypeFilters resourceTypeFilters;

  private CedarUserUIInfoPanel infoPanel;

  private CedarUserUITemplateEditor templateEditor;

  private CedarUserUIMetadataEditor metadataEditor;

  private String stylesheet;

  public CedarUserUIPreferences() {
    folderView = new CedarUserUIFolderView();
    resourceTypeFilters = new CedarUserUIResourceTypeFilters();
    infoPanel = new CedarUserUIInfoPanel();
    templateEditor = new CedarUserUITemplateEditor();
    metadataEditor = new CedarUserUIMetadataEditor();
  }

  public CedarUserUIFolderView getFolderView() {
    return folderView;
  }

  public void setFolderView(CedarUserUIFolderView folderView) {
    this.folderView = folderView;
  }

  public CedarUserUIResourceTypeFilters getResourceTypeFilters() {
    return resourceTypeFilters;
  }

  public void setResourceTypeFilters(CedarUserUIResourceTypeFilters resourceTypeFilters) {
    this.resourceTypeFilters = resourceTypeFilters;
  }

  public CedarUserUIInfoPanel getInfoPanel() {
    return infoPanel;
  }

  public void setInfoPanel(CedarUserUIInfoPanel infoPanel) {
    this.infoPanel = infoPanel;
  }

  public CedarUserUITemplateEditor getTemplateEditor() {
    return templateEditor;
  }

  public void setTemplateEditor(CedarUserUITemplateEditor templateEditor) {
    this.templateEditor = templateEditor;
  }

  public CedarUserUIMetadataEditor getMetadataEditor() {
    return metadataEditor;
  }

  public void setMetadataEditor(CedarUserUIMetadataEditor metadataEditor) {
    this.metadataEditor = metadataEditor;
  }

  public String getStylesheet() {
    return stylesheet;
  }

  public void setStylesheet(String stylesheet) {
    this.stylesheet = stylesheet;
  }
}
