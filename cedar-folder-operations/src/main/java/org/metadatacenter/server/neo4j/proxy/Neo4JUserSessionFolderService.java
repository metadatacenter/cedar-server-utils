package org.metadatacenter.server.neo4j.proxy;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.model.folderserver.FolderServerFolder;
import org.metadatacenter.model.folderserver.FolderServerGroup;
import org.metadatacenter.model.folderserver.FolderServerNode;
import org.metadatacenter.model.folderserver.FolderServerResource;
import org.metadatacenter.server.FolderServiceSession;
import org.metadatacenter.server.neo4j.*;
import org.metadatacenter.server.security.model.auth.NodePermission;
import org.metadatacenter.server.security.model.user.CedarUser;
import org.metadatacenter.util.CedarNodeTypeUtil;
import org.metadatacenter.util.CedarUserNameUtil;

import java.util.*;

public class Neo4JUserSessionFolderService extends AbstractNeo4JUserSession implements FolderServiceSession {

  public Neo4JUserSessionFolderService(CedarConfig cedarConfig, Neo4JProxies proxies, CedarUser cu) {
    super(cedarConfig, proxies, cu);
  }

  public static FolderServiceSession get(CedarConfig cedarConfig, Neo4JProxies proxies, CedarUser cedarUser) {
    return new Neo4JUserSessionFolderService(cedarConfig, proxies, cedarUser);
  }

  @Override
  public FolderServerResource createResourceAsChildOfId(String parentFolderURL, String childURL, CedarNodeType
      nodeType, String name, String description, NodeLabel label) {
    return createResourceAsChildOfId(parentFolderURL, childURL, nodeType, name, description, label, null);
  }

  @Override
  public FolderServerResource createResourceAsChildOfId(String parentFolderURL, String childURL, CedarNodeType
      nodeType, String name, String description, NodeLabel label, Map<String, Object> extraProperties) {
    return proxies.resource().createResourceAsChildOfId(parentFolderURL, childURL, nodeType, name,
        description, cu.getId(), label, extraProperties);
  }

  @Override
  public FolderServerFolder updateFolderById(String folderURL, Map<String, String> updateFields) {
    return proxies.folder().updateFolderById(folderURL, updateFields, cu.getId());
  }

  @Override
  public FolderServerResource updateResourceById(String resourceURL, CedarNodeType nodeType, Map<String,
      String> updateFields) {
    return proxies.resource().updateResourceById(resourceURL, updateFields, cu.getId());
  }

  @Override
  public boolean deleteFolderById(String folderURL) {
    return proxies.folder().deleteFolderById(folderURL);
  }

  @Override
  public boolean deleteResourceById(String resourceURL, CedarNodeType nodeType) {
    return proxies.resource().deleteResourceById(resourceURL);
  }

  @Override
  public void addPathAndParentId(FolderServerFolder folder) {
    if (folder != null) {
      List<FolderServerFolder> path = findFolderPath(folder);
      if (path != null) {
        folder.setPath(getPathString(path));
        folder.setParentPath(getParentPathString(path));
        folder.setDisplayPath(getDisplayPathString(path));
        folder.setDisplayParentPath(getDisplayParentPathString(path));
      }
    }
  }

  @Override
  public void addPathAndParentId(FolderServerResource resource) {
    if (resource != null) {
      List<FolderServerNode> path = proxies.node().findNodePathById(resource.getId());
      if (path != null) {
        resource.setPath(getPathString(path));
        resource.setParentPath(getParentPathString(path));
        resource.setDisplayPath(getDisplayPathString(path));
        resource.setDisplayParentPath(getDisplayParentPathString(path));
      }
    }
  }

  private String getParentPathString(List<? extends FolderServerNode> path) {
    List<FolderServerNode> p = new ArrayList<>();
    p.addAll(path);
    if (path.size() > 0) {
      p.remove(p.size() - 1);
    } else {
      return null;
    }
    return getPathString(p);
  }

  private String getPathString(List<? extends FolderServerNode> path) {
    StringBuilder sb = new StringBuilder();
    boolean addSeparator = false;
    for (FolderServerNode node : path) {
      if (addSeparator) {
        sb.append(proxies.pathUtil.getSeparator());
      }
      if (node instanceof FolderServerFolder) {
        if (!((FolderServerFolder) node).isRoot()) {
          addSeparator = true;
        }
      }
      sb.append(node.getName());
    }
    return sb.length() == 0 ? null : sb.toString();
  }


  private String getDisplayParentPathString(List<? extends FolderServerNode> path) {
    List<FolderServerNode> p = new ArrayList<>();
    p.addAll(path);
    if (path.size() > 0) {
      p.remove(p.size() - 1);
    } else {
      return null;
    }
    return getDisplayPathString(p);
  }

  private String getDisplayPathString(List<? extends FolderServerNode> path) {
    StringBuilder sb = new StringBuilder();
    boolean addSeparator = false;
    for (FolderServerNode node : path) {
      if (addSeparator) {
        sb.append(proxies.pathUtil.getSeparator());
      }
      if (node instanceof FolderServerFolder) {
        if (!((FolderServerFolder) node).isRoot()) {
          addSeparator = true;
        }
      }
      sb.append(node.getDisplayName());
    }
    return sb.length() == 0 ? null : sb.toString();
  }

  @Override
  public String sanitizeName(String name) {
    return proxies.pathUtil.sanitizeName(name);
  }

  @Override
  public long findFolderContentsCount(String folderURL, List<CedarNodeType> nodeTypeList) {
    return proxies.node().findFolderContentsFilteredCount(folderURL, nodeTypeList);
  }

  @Override
  public long findFolderContentsCount(String folderURL) {
    return proxies.node().findFolderContentsFilteredCount(folderURL, CedarNodeTypeUtil.getValidNodeTypesForRestCalls());
  }

  @Override
  public FolderServerResource findResourceById(String resourceURL) {
    return proxies.resource().findResourceById(resourceURL);
  }

  @Override
  public List<FolderServerNode> findAllNodes(int limit, int offset, List<String> sortList) {
    return proxies.node().findAllNodes(limit, offset, sortList);
  }

  @Override
  public long findAllNodesCount() {
    return proxies.node().findAllNodesCount();
  }

  @Override
  public String getChildPath(String path, String name) {
    return proxies.pathUtil.getChildPath(path, name);
  }

  @Override
  public List<FolderServerNode> findFolderContents(String folderURL, List<CedarNodeType> nodeTypeList, int
      limit, int offset, List<String> sortList) {
    return proxies.node().findFolderContents(folderURL, nodeTypeList, limit, offset, sortList, cu);
  }

  @Override
  public String getRootPath() {
    return proxies.pathUtil.getRootPath();
  }

  @Override
  public List<FolderServerFolder> findFolderPathByPath(String path) {
    return proxies.folder().findFolderPathByPath(path);
  }

  @Override
  public FolderServerFolder findFolderById(String folderURL) {
    return proxies.folder().findFolderById(folderURL);
  }

  @Override
  public FolderServerFolder findFolderByPath(String path) {
    return proxies.folder().findFolderByPath(path);
  }

  @Override
  public FolderServerNode findNodeByParentIdAndName(FolderServerFolder parentFolder, String name) {
    return proxies.node().findNodeByParentIdAndName(parentFolder.getId(), name);
  }

  @Override
  public FolderServerFolder createFolderAsChildOfId(String parentFolderURL, String name, String displayName, String
      description, NodeLabel label) {
    return createFolderAsChildOfId(parentFolderURL, name, displayName, description, label, null);
  }

  @Override
  public FolderServerFolder createFolderAsChildOfId(String parentFolderURL, String name, String displayName, String
      description, NodeLabel label, Map<String, Object> extraProperties) {
    return proxies.folder().createFolderAsChildOfId(parentFolderURL, name, displayName, description, cu.getId(),
        label, extraProperties);
  }

  @Override
  public boolean moveResource(FolderServerResource sourceResource, FolderServerFolder targetFolder) {
    return proxies.resource().moveResource(sourceResource, targetFolder);
  }

  @Override
  public boolean moveFolder(FolderServerFolder sourceFolder, FolderServerFolder targetFolder) {
    return proxies.folder().moveFolder(sourceFolder, targetFolder);
  }

  @Override
  public String normalizePath(String path) {
    return proxies.pathUtil.normalizePath(path);
  }

  @Override
  public List<FolderServerFolder> findFolderPath(FolderServerFolder folder) {
    if (folder.isRoot()) {
      List<FolderServerFolder> pathInfo = new ArrayList<>();
      pathInfo.add(folder);
      return pathInfo;
    } else {
      return proxies.folder().findFolderPathById(folder.getId());
    }
  }

  @Override
  public FolderServerFolder ensureUserHomeExists() {
    FolderServerFolder currentUserHomeFolder = findHomeFolderOf();
    if (currentUserHomeFolder == null) {
      currentUserHomeFolder = createUserHomeFolder();
    }
    return currentUserHomeFolder;
  }

  @Override
  public List<FolderServerNode> viewSharedWithMe(List<CedarNodeType> nodeTypes, int limit, int offset, List<String>
      sortList) {
    return proxies.node().viewSharedWithMeFiltered(nodeTypes, limit, offset, sortList, cu);
  }

  @Override
  public long viewSharedWithMeCount(List<CedarNodeType> nodeTypes) {
    return proxies.node().viewSharedWithMeFilteredCount(nodeTypes, cu);
  }

  @Override
  public List<FolderServerNode> viewAll(List<CedarNodeType> nodeTypes, int limit, int offset, List<String>
      sortList) {
    return proxies.node().viewAllFiltered(nodeTypes, limit, offset, sortList, cu);
  }

  @Override
  public long viewAllCount(List<CedarNodeType> nodeTypes) {
    return proxies.node().viewAllFilteredCount(nodeTypes, cu);
  }

  @Override
  public List<FolderServerNode> findAllDescendantNodesById(String id) {
    return proxies.node().findAllDescendantNodesById(id);
  }

  @Override
  public List<FolderServerNode> findAllNodesVisibleByUserId(String id) {
    return proxies.node().findAllNodesVisibleByUserId(id);
  }

  @Override
  public List<FolderServerNode> findAllNodesVisibleByGroupId(String id) {
    return proxies.node().findAllNodesVisibleByGroupId(id);
  }


  @Override
  public FolderServerFolder findHomeFolderOf() {
    return proxies.folder().findHomeFolderOf(cu.getId());
  }

  @Override
  public FolderServerFolder createUserHomeFolder() {
    String userId = cu.getId();
    Neo4jConfig config = proxies.config;
    FolderServerFolder currentUserHomeFolder;
    FolderServerFolder usersFolder = findFolderByPath(config.getUsersFolderPath());
    // usersFolder should not be null at this point. If it is, we let the NPE to be thrown
    Map<String, Object> extraParams = new HashMap<>();
    extraParams.put(Neo4JFields.IS_USER_HOME, true);
    extraParams.put(Neo4JFields.HOME_OF, userId);
    String displayName = CedarUserNameUtil.getDisplayName(cedarConfig, cu);
    String description = CedarUserNameUtil.getHomeFolderDescription(cedarConfig, cu);
    currentUserHomeFolder = createFolderAsChildOfId(usersFolder.getId(), displayName, displayName, description,
        NodeLabel.USER_HOME_FOLDER, extraParams);
    if (currentUserHomeFolder != null) {
      FolderServerGroup everybody = proxies.group().findGroupBySpecialValue(Neo4JFieldValues.SPECIAL_GROUP_EVERYBODY);
      if (everybody != null) {
        proxies.permission().addPermission(currentUserHomeFolder, everybody, NodePermission.READTHIS);
      }
    }
    return currentUserHomeFolder;
  }


}
