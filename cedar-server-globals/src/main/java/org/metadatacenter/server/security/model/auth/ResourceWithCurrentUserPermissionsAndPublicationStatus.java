package org.metadatacenter.server.security.model.auth;

import org.metadatacenter.server.security.model.NodeWithPublicationStatus;
import org.metadatacenter.server.security.model.permission.resource.ResourceWithCurrentUserPermissions;

public interface ResourceWithCurrentUserPermissionsAndPublicationStatus
    extends ResourceWithCurrentUserPermissions, NodeWithPublicationStatus {
}
