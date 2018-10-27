package org.metadatacenter.model.folderserver.extract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.metadatacenter.model.CedarNodeType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FolderServerTemplateExtract extends FolderServerResourceExtract {

  public FolderServerTemplateExtract() {
    super(CedarNodeType.TEMPLATE);
  }

}