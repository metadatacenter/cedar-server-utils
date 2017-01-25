package org.metadatacenter.util.http;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URLEncoder;

public abstract class CedarUrlUtil {

  public static String urlEncode(String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (Exception e) {
      // Do nothing
    }
    return null;
  }

  public static URI getIdURI(UriInfo uriInfo, String id) {
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    return builder.path(CedarUrlUtil.urlEncode(id)).build();
  }

  public static URI getLocationURI(HttpResponse httpResponse) {
    URI location = null;
    if (httpResponse != null) {
      Header locationHeader = httpResponse.getFirstHeader(HttpHeaders.LOCATION);
      if (locationHeader != null) {
        location = URI.create(locationHeader.getValue());
      }
    }
    return location;
  }
}