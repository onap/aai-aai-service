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

package org.openecomp.aai.workarounds;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LegacyURLTransformer {

    /**
     * Instantiates a new legacy URL transformer.
     */
    private LegacyURLTransformer() {

    }

    private static class Helper {

        private static final LegacyURLTransformer INSTANCE = new LegacyURLTransformer();
    }

    /**
     * Gets the single instance of LegacyURLTransformer.
     *
     * @return single instance of LegacyURLTransformer
     */
    public static LegacyURLTransformer getInstance() {
        return Helper.INSTANCE;
    }

    /**
     * Gets the legacy URL.
     *
     * @param url the url
     * @return the legacy URL
     * @throws MalformedURLException the malformed URL exception
     */
    public URL getLegacyURL(URL url) throws MalformedURLException {
        String substring = "/aai/(?<version>v\\d+)/cloud-infrastructure/tenants/tenant/(?<tenantKey>.*?)/vservers/"
            + "vserver/(?<vserverKey>[^/]*?$)";
        String replacement = "/aai/servers/${version}/${tenantKey}/vservers/${vserverKey}";
        Pattern p = Pattern.compile(substring);
        String result = url.toString();
        Matcher m = p.matcher(result);
        if (m.find()) {
            result = m.replaceFirst(replacement);
        }
        return new URL(result);
    }

    /**
     * Gets the current URL.
     *
     * @param url the url
     * @return the current URL
     * @throws MalformedURLException the malformed URL exception
     */
    public URL getCurrentURL(URL url) throws MalformedURLException {
        String substring = "/aai/servers/(?<version>v[23])/(?<tenantKey>.*?)/vservers/(?<vserverKey>[^/]*?$)";
        String replacement = "/aai/${version}/cloud-infrastructure/tenants/tenant/${tenantKey}/vservers/vserver/${vserverKey}";
        Pattern p = Pattern.compile(substring);
        String result = url.toString();
        Matcher m = p.matcher(result);
        if (m.find()) {
            result = m.replaceFirst(replacement);
        }

        return new URL(result);
    }
}
