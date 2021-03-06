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

package org.openecomp.aai.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest({PhaseInterceptorChain.class, AAIConfig.class})
@PowerMockIgnore("javax.management.*")
public class AAIApiServerURLBaseTest {

	@BeforeClass
	public static void configure() {
		System.setProperty("AJSC_HOME", ".");
		System.setProperty("BUNDLECONFIG_DIR", "bundleconfig-local");
	}
	
	 /**
 	 * Test get hostname.
 	 *
 	 * @throws Exception the exception
 	 */
	@Test
	  public void testGetHostname() throws Exception {
	    PowerMockito.mockStatic(PhaseInterceptorChain.class);	    
	    Map <String, List<String>> hm = new HashMap<String, List<String>>();
	    List<String> host = new ArrayList<String>();
	    host.add("my-localhost");
	    hm.put("host", host);
	    
	    Message outMessage = new MessageImpl();
	    outMessage.put(Message.PROTOCOL_HEADERS, hm);
	    
	    when(PhaseInterceptorChain.getCurrentMessage()).thenReturn(outMessage);
	    assertEquals("https://my-localhost/aai/", AAIApiServerURLBase.get());
	  }
	 
	 /**
 	 * Test get with null hostname.
 	 *
 	 * @throws Exception the exception
 	 */
 	@Test
	  public void testGetWithNullHostname() throws Exception {
		PowerMockito.mockStatic(AAIConfig.class);
	    String defaultHostname = "default-name";
	    when(AAIConfig.get(AAIConstants.AAI_SERVER_URL_BASE)).thenReturn(defaultHostname);
	    assertEquals(defaultHostname, AAIApiServerURLBase.get());
	  }	 
}
