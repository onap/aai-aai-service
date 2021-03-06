/*-
 * ============LICENSE_START=======================================================
 * org.openecomp.aai
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.openecomp.aai.introspection.tools;

import java.net.URI;

import org.openecomp.aai.introspection.Introspector;
import org.openecomp.aai.introspection.Loader;
import org.openecomp.aai.logging.LogLineBuilder;
import org.openecomp.aai.parsers.uri.URIToObject;

public class InjectKeysFromURI  implements IssueResolver {

	private URI uri = null;
	private Loader loader = null;
	
	/**
	 * Instantiates a new inject keys from URI.
	 *
	 * @param loader the loader
	 * @param uri the uri
	 */
	public InjectKeysFromURI(Loader loader, URI uri) {
		this.loader = loader;
		this.uri = uri;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean resolveIssue(Issue issue) {
		boolean result = false;
		Introspector obj = issue.getIntrospector();
		if (issue.getError().equals(Error.MISSING_KEY_PROP)) {
			try {
				URIToObject toObject = new URIToObject(loader, uri);
				Introspector minimumObj = toObject.getEntity();
				if (toObject.getEntityName().equals(obj.getDbName())) {
					obj.setValue(issue.getPropName(), minimumObj.getValue(issue.getPropName()));
					result = true;
				}
			} catch (Exception e) {
				//log something probably
				result = false;
			}
		}
		
		return result;
	}

}
