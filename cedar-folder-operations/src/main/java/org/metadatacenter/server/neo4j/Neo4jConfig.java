package org.metadatacenter.server.neo4j;

public class Neo4jConfig {

  private String transactionUrl;
  private String authString;
  private String rootFolderPath;
  private String rootFolderDescription;
  private String usersFolderPath;
  private String usersFolderDescription;
  private String lostAndFoundFolderPath;
  private String lostAndFoundFolderDescription;

  public Neo4jConfig() {
  }

  public String getTransactionUrl() {
    return transactionUrl;
  }

  public void setTransactionUrl(String transactionUrl) {
    this.transactionUrl = transactionUrl;
  }

  public String getAuthString() {
    return authString;
  }

  public void setAuthString(String authString) {
    this.authString = authString;
  }

  public String getRootFolderPath() {
    return rootFolderPath;
  }

  public void setRootFolderPath(String rootFolderPath) {
    this.rootFolderPath = rootFolderPath;
  }

  public String getRootFolderDescription() {
    return rootFolderDescription;
  }

  public void setRootFolderDescription(String rootFolderDescription) {
    this.rootFolderDescription = rootFolderDescription;
  }

  public String getUsersFolderPath() {
    return usersFolderPath;
  }

  public void setUsersFolderPath(String usersFolderPath) {
    this.usersFolderPath = usersFolderPath;
  }

  public String getUsersFolderDescription() {
    return usersFolderDescription;
  }

  public void setUsersFolderDescription(String usersFolderDescription) {
    this.usersFolderDescription = usersFolderDescription;
  }

  public String getLostAndFoundFolderPath() {
    return lostAndFoundFolderPath;
  }

  public void setLostAndFoundFolderPath(String lostAndFoundFolderPath) {
    this.lostAndFoundFolderPath = lostAndFoundFolderPath;
  }

  public String getLostAndFoundFolderDescription() {
    return lostAndFoundFolderDescription;
  }

  public void setLostAndFoundFolderDescription(String lostAndFoundFolderDescription) {
    this.lostAndFoundFolderDescription = lostAndFoundFolderDescription;
  }
}