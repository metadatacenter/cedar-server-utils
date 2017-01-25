package org.metadatacenter.server.security;

import org.metadatacenter.server.security.model.AuthRequest;

import javax.servlet.http.HttpServletRequest;

import static org.metadatacenter.constant.HttpConstants.*;

public abstract class CedarAuthFromRequestFactory {

  public static AuthRequest fromRequest(HttpServletRequest request) {
    if (request != null) {
      String auth = request.getHeader(HTTP_HEADER_AUTHORIZATION);
      if (auth != null) {
        if (auth.startsWith(HTTP_AUTH_HEADER_BEARER_PREFIX)) {
          return new CedarBearerAuthRequest(request);
        } else if (auth.startsWith(HTTP_AUTH_HEADER_APIKEY_PREFIX)) {
          return new CedarApiKeyAuthRequest(request);
        }
      }
    }
    return new CedarNoAuthRequest();
  }

}
