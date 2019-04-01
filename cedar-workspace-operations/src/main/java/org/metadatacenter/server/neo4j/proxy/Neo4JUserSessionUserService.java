package org.metadatacenter.server.neo4j.proxy;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.folderserver.basic.FolderServerGroup;
import org.metadatacenter.model.folderserver.basic.FolderServerUser;
import org.metadatacenter.server.UserServiceSession;
import org.metadatacenter.server.neo4j.AbstractNeo4JUserSession;
import org.metadatacenter.server.neo4j.Neo4JFieldValues;
import org.metadatacenter.server.security.model.user.CedarUser;

import java.io.IOException;
import java.util.List;

public class Neo4JUserSessionUserService extends AbstractNeo4JUserSession implements UserServiceSession {

  private Neo4JUserSessionUserService(CedarConfig cedarConfig, Neo4JProxies proxies, CedarUser cu,
                                      String globalRequestId, String localRequestId) {
    super(cedarConfig, proxies, cu, globalRequestId, localRequestId);
  }

  public static UserServiceSession get(CedarConfig cedarConfig, Neo4JProxies proxies, CedarUser cedarUser,
                                       String globalRequestId, String localRequestId) {
    return new Neo4JUserSessionUserService(cedarConfig, proxies, cedarUser, globalRequestId, localRequestId);
  }

  @Override
  public List<FolderServerUser> findUsers() {
    return proxies.user().findUsers();
  }

/*  @Override
  public FolderServerUser ensureUserExists() {
    FolderServerUser currentUser = proxies.user().findUserById(cu.getId());
    if (currentUser == null) {
      currentUser = proxies.user().createUser(cu);
      FolderServerGroup everybody = proxies.group().findGroupBySpecialValue(Neo4JFieldValues.SPECIAL_GROUP_EVERYBODY);
      proxies.user().addUserToGroup(currentUser, everybody);
    }
    return currentUser;
  }*/

  @Override
  public FolderServerUser getUser(String userId) {
    return proxies.user().findUserById(userId);
  }

  @Override
  public FolderServerUser addUserToEverybodyGroup(CedarUser user) throws IOException {
    FolderServerGroup everybody = proxies.group().findGroupBySpecialValue(Neo4JFieldValues.SPECIAL_GROUP_EVERYBODY);
    FolderServerUser fsUser = proxies.user().findUserById(user.getId());
    proxies.user().addUserToGroup(fsUser, everybody);
    return fsUser;
  }
}
