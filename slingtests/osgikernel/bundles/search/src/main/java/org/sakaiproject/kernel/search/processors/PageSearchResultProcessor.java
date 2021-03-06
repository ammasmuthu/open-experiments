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
package org.sakaiproject.kernel.search.processors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Row;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.kernel.api.search.Aggregator;
import org.sakaiproject.kernel.api.search.SearchResultProcessor;
import org.sakaiproject.kernel.util.ExtendedJSONWriter;
import org.sakaiproject.kernel.util.RowUtils;

/**
 * Formats sakai/page nodes. Options are: - path: The path where you want to search under.
 * - properties: list of properties you want to filter on. - values: list of values for
 * the properties - operators: list of operators: > = <
 * 
 * @scr.component immediate="true" label="PageSearchResultProcessor"
 *                description="Formatter for pages search results."
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sakai.search.processor" value="Page"
 * @scr.property name="sakai.seach.resourcetype" value="sakai/page"
 * @scr.service interface="org.sakaiproject.kernel.api.search.SearchResultProcessor"
 */
public class PageSearchResultProcessor implements SearchResultProcessor {

  public void writeNode(SlingHttpServletRequest request, JSONWriter write, Aggregator aggregator, Row row)
      throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node node = RowUtils.getNode(row, session);
    write.object();
    ExtendedJSONWriter.writeNodeContentsToWriter(write, node);
    write.key("path");
    write.value(node.getPath());
    write.endObject();
    if ( aggregator != null ) {
      aggregator.add(node);
    }
  }
}
