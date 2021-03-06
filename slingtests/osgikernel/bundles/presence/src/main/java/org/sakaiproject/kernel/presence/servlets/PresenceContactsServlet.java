/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.sakaiproject.kernel.presence.servlets;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.kernel.api.connections.ConnectionManager;
import org.sakaiproject.kernel.api.connections.ConnectionState;
import org.sakaiproject.kernel.api.doc.BindingType;
import org.sakaiproject.kernel.api.doc.ServiceBinding;
import org.sakaiproject.kernel.api.doc.ServiceDocumentation;
import org.sakaiproject.kernel.api.doc.ServiceExtension;
import org.sakaiproject.kernel.api.doc.ServiceMethod;
import org.sakaiproject.kernel.api.doc.ServiceResponse;
import org.sakaiproject.kernel.api.doc.ServiceSelector;
import org.sakaiproject.kernel.api.personal.PersonalUtils;
import org.sakaiproject.kernel.api.presence.PresenceService;
import org.sakaiproject.kernel.presence.PresenceUtils;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet deals with GET and JSON only and outputs the contacts listing presence
 * related to the current user, only includes accepted contacts
 * 
 * @scr.component metatype="no" immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sakai/presence"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.selectors" value="contacts"
 * @scr.property name="sling.servlet.extensions" value="json"
 * 
 * @scr.reference name="ConnectionManager"
 *                interface="org.sakaiproject.kernel.api.connections.ConnectionManager"
 * @scr.reference name="PresenceService"
 *                interface="org.sakaiproject.kernel.api.presence.PresenceService"
 */
@ServiceDocumentation(name = "Presence Contacts Servlet", 
    description = "Gets presence for the current users contact including the public profile of each accepted contact.",
    shortDescription="Gets the presence for the current user.",
    bindings = @ServiceBinding(type = BindingType.TYPE, 
        bindings = "sakai/presence",
        selectors = @ServiceSelector(name="contacts", description=" requires a selector of resource.contacts.json to get the presence for the user and their contacts."),
        extensions = @ServiceExtension(name="json", description={
            "the presence information is returned as a json tree."
        })
    ), 
    methods = { 
         @ServiceMethod(name = "GET", 
             description = {
                 "Gets the presence for the current user, a list of contacts, their presence and their profile. The servlet is bound " +
                 "to a node of type sakai/presence although at the moment, there does not appear to be any information used from that " +
                 "path.",
                 "<pre>" +
                 "curl http://ieb:password@localhost:8080/_user/presence.contacts.json\n" +
                 "{\n" +
                 "  \"user\": \"ieb\"  \n" +
                 "  \"sakai:status\": \"online\",\n" +
                 "  \"sakai:location\": \"At Home\",\n" +
                 "  \"contacts\": [\n" +
                 "     {\n" +
                 "       \"user\": \"mark\"  \n" +
                 "       \"sakai:status\": \"online\",\n" +
                 "       \"sakai:location\": \"At Work\",\n" +
                 "       \"profile\": {\n" +
                 "         ... \n" +
                 "         <em>profile json tree</em>\n" +
                 "         ... \n" +
                 "       },\n" +
                 "       \"user\": \"luke\"  \n" +
                 "       \"sakai:status\": \"offline\",\n" +
                 "       \"sakai:location\": \"away\",\n" +
                 "       \"profile\": {\n" +
                 "         ... \n" +
                 "         <em>profile json tree</em>\n" +
                 "         ... \n" +
                 "       }\n" +
                 "     }\n" +
                 "  ]\n" +
                 "}\n" +
                 "</pre>"
         },
        response = {
             @ServiceResponse(code=200,description="On sucess a a json tree of the presence for contacts."),
             @ServiceResponse(code=401,description="The user is not logged in and the resource is protected"),
             @ServiceResponse(code=403,description="The user does not have permission to access the resource"),
           @ServiceResponse(code=404,description="The resource does not exist, or the target is not found"),
           @ServiceResponse(code=0,description="Any other status codes emmitted with have the meaning prescribed in the RFC")
         })
        })
public class PresenceContactsServlet extends SlingAllMethodsServlet {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PresenceContactsServlet.class);

  private static final long serialVersionUID = 11111111L;

  protected PresenceService presenceService;

  protected void bindPresenceService(PresenceService presenceService) {
    this.presenceService = presenceService;
  }

  protected void unbindPresenceService(PresenceService presenceService) {
    this.presenceService = null;
  }

  protected ConnectionManager connectionManager;

  protected void bindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = connectionManager;
  }

  protected void unbindConnectionManager(ConnectionManager connectionManager) {
    this.connectionManager = null;
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    // get current user
    String user = request.getRemoteUser();
    if (user == null) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "User must be logged in to check their status");
    }
    LOGGER.info("GET to PresenceContactsServlet (" + user + ")");

    try {
      Writer writer = response.getWriter();
      ExtendedJSONWriter output = new ExtendedJSONWriter(writer);
      // start JSON object
      output.object();
      PresenceUtils.makePresenceJSON(output, user, presenceService, true);
      // add in the list of contacts info
      Session session = request.getResource().adaptTo(Node.class).getSession();
      List<String> userIds = connectionManager.getConnectedUsers(user,
          ConnectionState.ACCEPTED);
      output.key("contacts");
      output.array();
      for (String userId : userIds) {
        output.object();
        // put in the basics
        PresenceUtils.makePresenceJSON(output, userId, presenceService, true);
        // add in the profile
        output.key("profile");
        Node profileNode = (Node) session.getItem(PersonalUtils.getProfilePath(userId));
        ExtendedJSONWriter.writeNodeToWriter(output, profileNode);
        output.endObject();
      }
      output.endArray();
      // finish it
      output.endObject();
    } catch (JSONException e) {
      LOGGER.error(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }

    return;
  }

}
