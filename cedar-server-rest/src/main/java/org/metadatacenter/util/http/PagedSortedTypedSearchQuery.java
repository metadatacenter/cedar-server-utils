package org.metadatacenter.util.http;

import org.metadatacenter.config.PaginationConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.rest.exception.CedarAssertionException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;

public class PagedSortedTypedSearchQuery extends PagedSortedTypedQuery {

  private Optional<String> qInput;
  private Optional<String> derivedFromIdInput;
  private String derivedFromId;

  public PagedSortedTypedSearchQuery(PaginationConfig config) {
    super(config);
  }

  public PagedSortedTypedSearchQuery derivedFromId(Optional<String> derivedFromIdInput) {
    this.derivedFromIdInput = derivedFromIdInput;
    return this;
  }

  public PagedSortedTypedSearchQuery q(Optional<String> qInput) {
    this.qInput = qInput;
    return this;
  }

  @Override
  public PagedSortedTypedSearchQuery resourceTypes(Optional<String> resourceTypesInput) {
    super.resourceTypes(resourceTypesInput);
    return this;
  }

  @Override
  public PagedSortedTypedSearchQuery sort(Optional<String> sortInput) {
    super.sort(sortInput);
    return this;
  }

  @Override
  public PagedSortedTypedSearchQuery limit(Optional<Integer> limitInput) {
    super.limit(limitInput);
    return this;
  }

  @Override
  public PagedSortedTypedSearchQuery offset(Optional<Integer> offsetInput) {
    super.offset(offsetInput);
    return this;
  }

  @Override
  public void validate() throws CedarException {
    validateLimit();
    validateOffset();
    validateSorting();
    validateSearchTerm();
    validateResourceTypesWithTemplateId();
  }

  public String getDerivedFromId() {
    return derivedFromId;
  }

  public String getQ() {
    return qInput.get();
  }

  private static boolean isValidURL(String urlStr) {
    try {
      URL url = new URL(urlStr);
      return true;
    } catch (MalformedURLException e) {
      return false;
    }
  }

  private void validateTemplateId() throws CedarException {
    if (derivedFromIdInput.isPresent()) {
      if (derivedFromIdInput.get() != null
          && !derivedFromIdInput.get().isEmpty()
          && isValidURL(derivedFromIdInput.get())) {
        derivedFromId = derivedFromIdInput.get();
      } else {
        throw new CedarAssertionException("You must pass in 'derived_from_id' as a valid template identifier!")
            .parameter("template_id", derivedFromIdInput.get());
      }
    }
  }

  protected void validateResourceTypesWithTemplateId() throws CedarException {
    validateTemplateId();
    if (derivedFromId != null) {
      if (resourceTypesInput.isPresent()) {
        throw new CedarAssertionException(
            "You must pass not specify 'resource_types' if the 'derived_from_id' is specified!")
            .parameter("derived_from_id", derivedFromId)
            .parameter("resource_types", resourceTypesInput.get());
      } else {
        nodeTypeList = new ArrayList<>();
        nodeTypeList.add(CedarNodeType.INSTANCE);
      }
    } else {
      validateResourceTypes();
    }
  }

  public void validateSearchTerm() throws CedarException {
    if (qInput.isPresent()) {
      if (qInput.get() == null || qInput.get().trim().isEmpty()) {
        throw new CedarAssertionException("You must pass in a valid search query as 'q'!")
            .parameter("q", qInput.get());
      }
    }
  }

}