package org.sakaiproject.kernel.doc.proxy;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.DocumentationConstants;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceParameter;
import org.sakaiproject.kernel.api.doc.ServiceResponse;
import org.sakaiproject.kernel.doc.DocumentationWriter;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(paths = { "/system/doc/proxy" }, methods = { "GET" })
@ServiceDocumentation(name = "Proxy documentation", description = "Provides auto documentation of proxy nodes currently in the repository. Documentation will use the "
    + "node properties."
    + " Requests to this servlet take the form /system/doc/proxy?p=&lt;proxynodepath&gt where <em>proxynodepath</em>"
    + " is the absolute path of the proxynode deployed into the JCR repository. If the node is "
    + "not present a 404 will be retruned, if the node is present, it will be interogated to extract "
    + "documentation from the node. All documentation is assumed to be HTML encoded. If the browser is "
    + "directed to <a href=\"/system/doc/proxy\" >/system/doc/proxy</a> a list of all the proxy nodes in the system will be displayed ", 
    shortDescription = "Documentation for all the proxynodes in the repository. ", 
    url = "/system/doc/proxy", bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/system/doc/proxy"), methods = { @ServiceMethod(name = "GET", description = "GETs to this servlet will produce documentation for the proxynode, or an index of all searchnodes.", parameters = @ServiceParameter(name = "p", description = "The absolute path to a searchnode to display the documentation for"), response = {
    @ServiceResponse(code = 200, description = "html page for the requested resource"),
    @ServiceResponse(code = 404, description = "Search node not found") }) })
public class ProxyDocumentationServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -5820041368602931242L;
  private static final String PATH_PARAM = "p";

  @Override
  protected void doGet(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {

    RequestParameter path = request.getRequestParameter(PATH_PARAM);
    Session session = request.getResourceResolver().adaptTo(Session.class);
    PrintWriter writer = response.getWriter();
    try {
      if (path != null) {
        DocumentationWriter.writeSearchInfo(path.getString(), session, writer);
      } else {
        String query = "//*[@sling:resourceType='sakai/proxy']";
        DocumentationWriter.writeNodes(session, writer, query,
            DocumentationConstants.PREFIX + "/proxy");
      }
    } catch (ItemNotFoundException e) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
