package org.metadatacenter.bridge;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.model.WorkspaceObjectBuilder;
import org.metadatacenter.model.folderserver.basic.FolderServerFolder;
import org.metadatacenter.model.folderserver.basic.FolderServerResource;
import org.metadatacenter.model.folderserver.currentuserpermissions.FolderServerFolderCurrentUserReport;
import org.metadatacenter.model.folderserver.currentuserpermissions.FolderServerResourceCurrentUserReport;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.util.http.CedarUrlUtil;
import org.metadatacenter.util.http.ProxyUtil;
import org.metadatacenter.util.json.JsonMapper;

public class FolderServerProxy {

  private FolderServerProxy() {
  }

  public static FolderServerResource getResource(String folderBaseResource, String resourceId, CedarRequestContext
      context) throws CedarProcessingException {
    if (resourceId != null) {
      try {
        String url = folderBaseResource + "/" + CedarUrlUtil.urlEncode(resourceId);
        HttpResponse proxyResponse = ProxyUtil.proxyGet(url, context);
        int statusCode = proxyResponse.getStatusLine().getStatusCode();
        HttpEntity entity = proxyResponse.getEntity();
        if (entity != null) {
          if (HttpStatus.SC_OK == statusCode) {
            FolderServerResource node = WorkspaceObjectBuilder.artifact(proxyResponse.getEntity().getContent());
            return node;
          }
        }
      } catch (Exception e) {
        throw new CedarProcessingException(e);
      }
    }
    return null;
  }

  public static FolderServerResourceCurrentUserReport getResourceCurrentUserReport(String folderBaseResource,
                                                                                   String resourceId,
                                                                                   CedarRequestContext context)
      throws CedarProcessingException {
    if (resourceId != null) {
      try {
        String url = folderBaseResource + "/" + CedarUrlUtil.urlEncode(resourceId) + "/current-user-report";
        HttpResponse proxyResponse = ProxyUtil.proxyGet(url, context);
        int statusCode = proxyResponse.getStatusLine().getStatusCode();
        HttpEntity entity = proxyResponse.getEntity();
        if (entity != null) {
          if (HttpStatus.SC_OK == statusCode) {
            FolderServerResourceCurrentUserReport node = null;
            String responseString = EntityUtils.toString(proxyResponse.getEntity());
            node = JsonMapper.MAPPER.readValue(responseString, FolderServerResourceCurrentUserReport.class);
            return node;
          }
        }
      } catch (Exception e) {
        throw new CedarProcessingException(e);
      }
    }
    return null;
  }

  public static FolderServerFolder getFolder(String folderBaseFolders, String folderId, CedarRequestContext context)
      throws CedarProcessingException {
    if (folderId != null) {
      try {
        String url = folderBaseFolders + "/" + CedarUrlUtil.urlEncode(folderId);
        HttpResponse proxyResponse = ProxyUtil.proxyGet(url, context);
        int statusCode = proxyResponse.getStatusLine().getStatusCode();
        HttpEntity entity = proxyResponse.getEntity();
        if (entity != null) {
          if (HttpStatus.SC_OK == statusCode) {
            FolderServerFolder folder = null;
            String responseString = EntityUtils.toString(proxyResponse.getEntity());
            folder = JsonMapper.MAPPER.readValue(responseString, FolderServerFolder.class);
            return folder;
          }
        }
      } catch (Exception e) {
        throw new CedarProcessingException(e);
      }
    }
    return null;
  }

  public static FolderServerFolderCurrentUserReport getFolderCurrentUserReport(String folderBaseFolders,
                                                                               String folderId,
                                                                               CedarRequestContext context)
      throws CedarProcessingException {
    if (folderId != null) {
      try {
        String url = folderBaseFolders + "/" + CedarUrlUtil.urlEncode(folderId) + "/current-user-report";
        HttpResponse proxyResponse = ProxyUtil.proxyGet(url, context);
        int statusCode = proxyResponse.getStatusLine().getStatusCode();
        HttpEntity entity = proxyResponse.getEntity();
        if (entity != null) {
          if (HttpStatus.SC_OK == statusCode) {
            FolderServerFolderCurrentUserReport folderCurrentUserReport = null;
            String responseString = EntityUtils.toString(proxyResponse.getEntity());
            folderCurrentUserReport =
                JsonMapper.MAPPER.readValue(responseString, FolderServerFolderCurrentUserReport.class);
            return folderCurrentUserReport;
          }
        }
      } catch (Exception e) {
        throw new CedarProcessingException(e);
      }
    }
    return null;
  }

}
