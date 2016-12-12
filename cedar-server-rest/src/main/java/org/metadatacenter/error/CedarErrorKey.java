package org.metadatacenter.error;

public enum CedarErrorKey {

  TEMPLATE_ELEMENT_NOT_CREATED("templateElementNotCreated"),
  TEMPLATE_ELEMENT_NOT_FOUND("templateElementNotFound"),
  TEMPLATE_ELEMENT_NOT_DELETED("templateElementNotDeleted"),
  TEMPLATE_ELEMENT_NOT_UPDATED("templateElementNotUpdated"),
  TEMPLATE_ELEMENTS_NOT_LISTED("templateElementsNotListed"),

  TEMPLATE_FIELD_NOT_CREATED("templateFieldNotCreated"),
  TEMPLATE_FIELD_NOT_FOUND("templateFieldNotFound"),
  TEMPLATE_FIELD_NOT_DELETED("templateFieldNotDeleted"),
  TEMPLATE_FIELD_NOT_UPDATED("templateFieldNotUpdated"),
  TEMPLATE_FIELDS_NOT_LISTED("templateFieldsNotListed"),

  TEMPLATE_NOT_CREATED("templateNotCreated"),
  TEMPLATE_NOT_FOUND("templateNotFound"),
  TEMPLATE_NOT_DELETED("templateNotDeleted"),
  TEMPLATE_NOT_UPDATED("templateNotUpdated"),
  TEMPLATES_NOT_LISTED("templatesNotListed"),

  TEMPLATE_INSTANCE_NOT_CREATED("templateInstanceNotCreated"),
  TEMPLATE_INSTANCE_NOT_FOUND("templateInstanceNotFound"),
  TEMPLATE_INSTANCE_NOT_DELETED("templateInstanceNotDeleted"),
  TEMPLATE_INSTANCE_NOT_UPDATED("templateInstanceNotUpdated"),
  TEMPLATE_INSTANCES_NOT_LISTED("templateInstancesNotListed"),

  NO_READ_ACCESS_TO_FOLDER("noReadAccessToFolder"),
  NO_WRITE_ACCESS_TO_FOLDER("noWriteAccessToFolder"),

  NO_READ_ACCESS_TO_TEMPLATE("noReadAccessToTemplate"),
  NO_WRITE_ACCESS_TO_TEMPLATE("noWriteAccessToTemplate"),

  NO_READ_ACCESS_TO_TEMPLATE_ELEMENT("noReadAccessToTemplateElement"),
  NO_WRITE_ACCESS_TO_TEMPLATE_ELEMENT("noWriteAccessToTemplateElement"),

  NO_READ_ACCESS_TO_TEMPLATE_FIELD("noReadAccessToTemplateField"),
  NO_WRITE_ACCESS_TO_TEMPLATE_FIELD("noWriteAccessToTemplateField"),

  NO_READ_ACCESS_TO_TEMPLATE_INSTANCE("noReadAccessToTemplateInstance"),
  NO_WRITE_ACCESS_TO_TEMPLATE_INSTANCE("noWriteAccessToTemplateInstance"),
  ;

  private final String errorKey;

  CedarErrorKey(String errorKey) {
    this.errorKey = errorKey;
  }
}
