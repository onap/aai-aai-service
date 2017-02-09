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

package org.openecomp.aai.rest;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.persistence.dynamic.DynamicEntity;
import org.eclipse.persistence.jaxb.JAXBUnmarshaller;
import org.eclipse.persistence.jaxb.dynamic.DynamicJAXBContext;
import org.openecomp.aai.domain.model.AAIResource;
import org.openecomp.aai.exceptions.AAIException;
import org.openecomp.aai.extensions.AAIExtensionMap;
import org.openecomp.aai.introspection.Introspector;
import org.openecomp.aai.introspection.Loader;
import org.openecomp.aai.introspection.tools.CreateUUID;
import org.openecomp.aai.introspection.tools.DefaultFields;
import org.openecomp.aai.introspection.tools.InjectKeysFromURI;
import org.openecomp.aai.introspection.tools.IntrospectorValidator;
import org.openecomp.aai.introspection.tools.Issue;
import org.openecomp.aai.logging.AAILogger;
import org.openecomp.aai.logging.ErrorLogHelper;
import org.openecomp.aai.logging.LogLine;
import org.openecomp.aai.logging.LogLineBuilder;
import org.openecomp.aai.util.AAIConfig;
import org.openecomp.aai.util.AAIConstants;
import org.openecomp.aai.util.AAITxnLog;

import com.google.common.base.Joiner;


/**
 * Base class for AAI REST API classes.
 * Provides method to validate header information
 * TODO should authenticate caller and authorize them for the API they are calling
 * TODO should store the transaction
 
 *
 */
public class RESTAPI {
	
	protected static AAILogger aaiLogger = new AAILogger(RESTAPI.class.getName());
	
//	protected LogLine logline = new LogLine();
//
//	public String transId = null;
//	public String fromAppId = null;

protected final String COMPONENT = "aairest";

	/**
	 * The Enum Action.
	 */
	public enum Action {
		GET, PUT, POST, DELETE
	};

	
	public AAITxnLog txn = null;

	/**
	 * Gets the from app id.
	 *
	 * @param headers the headers
	 * @param logline the logline
	 * @return the from app id
	 * @throws AAIException the AAI exception
	 */
	protected String getFromAppId(HttpHeaders headers, LogLine logline) throws AAIException { 
		String fromAppId = null;
		if (headers != null) {
			List<String> fromAppIdHeader = headers.getRequestHeader("X-FromAppId");
			if (fromAppIdHeader != null) {
				for (String fromAppIdValue : fromAppIdHeader) {
					fromAppId = fromAppIdValue;
				}
			} 
		}

		if (fromAppId == null) {
			throw new AAIException("AAI_4009");
		}
		
		return fromAppId;
	}
	
	/**
	 * Gets the trans id.
	 *
	 * @param headers the headers
	 * @param logline the logline
	 * @return the trans id
	 * @throws AAIException the AAI exception
	 */
	protected String getTransId(HttpHeaders headers, LogLine logline) throws AAIException { 
		String transId = null;
		if (headers != null) {
			List<String> transIdHeader = headers.getRequestHeader("X-TransactionId");
			if (transIdHeader != null) {
				for (String transIdValue : transIdHeader) {
					transId = transIdValue;
				}
			}
		}

		if (transId == null) {
			throw new AAIException("AAI_4010");
		}
		
		return transId;
	}
	
	
	/**
	 * Gen date.
	 *
	 * @return the string
	 */
	protected String genDate() {
		LogLine logline = new LogLine();
		return genDate(logline);
	}
	
	/**
	 * Gen date.
	 *
	 * @param logline the logline
	 * @return the string
	 */
	protected String genDate(LogLine logline) {
		Date date = new Date();
		DateFormat formatter = null;
		try {
			formatter = new SimpleDateFormat(AAIConfig.get(AAIConstants.HBASE_TABLE_TIMESTAMP_FORMAT));
		} catch (AAIException ex) {
			logline.add("Property", AAIConstants.HBASE_TABLE_TIMESTAMP_FORMAT);
			aaiLogger.error(ex.getErrorObject(), logline, ex);
		} finally {
			if (formatter == null) {
				formatter = new SimpleDateFormat("YYMMdd-HH:mm:ss:SSS");
			}
		}

		return formatter.format(date);
	}

	/**
	 * Gets the media type.
	 *
	 * @param mediaTypeList the media type list
	 * @return the media type
	 */
	protected String getMediaType(List <MediaType> mediaTypeList) {
		String mediaType = MediaType.APPLICATION_JSON;  // json is the default    
		for (MediaType mt : mediaTypeList) {
			if (MediaType.APPLICATION_XML_TYPE.isCompatible(mt)) {
				mediaType = MediaType.APPLICATION_XML;
			} 
		}
		return mediaType;
	}
		
	/**
	 * Gets the dynamic entity for request.
	 *
	 * @param jaxbContext the jaxb context
	 * @param aaiRes the aai res
	 * @param objectFromRequest the object from request
	 * @param aaiExtMap the aai ext map
	 * @return the dynamic entity for request
	 * @throws JAXBException the JAXB exception
	 */
	protected DynamicEntity getDynamicEntityForRequest(DynamicJAXBContext jaxbContext,
														AAIResource aaiRes,
														String objectFromRequest, 
														AAIExtensionMap aaiExtMap) throws JAXBException {
		DynamicEntity request = null;
		if (objectFromRequest != null) {
		   JAXBUnmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		   String dynamicClass = aaiRes.getResourceClassName();

		   if (aaiExtMap.getHttpServletRequest().getContentType() == null || 
				   aaiExtMap.getHttpServletRequest().getContentType().contains("application/json")) {
				unmarshaller.setProperty("eclipselink.media-type", "application/json");
				unmarshaller.setProperty("eclipselink.json.include-root", false);
		   }

		   Class<? extends DynamicEntity> resultClass = jaxbContext.newDynamicEntity(dynamicClass).getClass();
		   StringReader reader = new StringReader(objectFromRequest);
		   request = (DynamicEntity) unmarshaller.unmarshal(new StreamSource(reader), resultClass).getValue();
		}
		return request;
	}

	 /**
 	 * Log transaction.
 	 *
 	 * @param appId the app id
 	 * @param tId the t id
 	 * @param action the action
 	 * @param input the input
 	 * @param rqstTm the rqst tm
 	 * @param respTm the resp tm
 	 * @param request the request
 	 * @param respBuf the resp buf
 	 * @param status the status
 	 */
 	public void logTransaction(	String appId, String tId, String action, 
				String input, String rqstTm, String respTm, String request, String respBuf, String status) {	
		 logTransaction(appId, tId, action, input, rqstTm, respTm, request, respBuf, String.valueOf(status), new LogLine());
	 }
	
	/**
	 * Log transaction.
	 *
	 * @param appId the app id
	 * @param tId the t id
	 * @param action the action
	 * @param input the input
	 * @param rqstTm the rqst tm
	 * @param respTm the resp tm
	 * @param request the request
	 * @param response the response
	 * @param logline the logline
	 */
	/*  ---------------- Log Transaction into HBase --------------------- */
	public void logTransaction(	String appId, String tId, String action, 
			String input, String rqstTm, String respTm, String request, Response response, LogLine logline) {	
		String respBuf = "";
		int status = 0;

		if (response != null && response.getEntity() != null) {		
			respBuf = response.getEntity().toString();
			status = response.getStatus();
		}
		logTransaction(appId, tId, action, input, rqstTm, respTm, request, respBuf, String.valueOf(status), logline);
	}
	
	/**
	 * Log transaction.
	 *
	 * @param appId the app id
	 * @param tId the t id
	 * @param action the action
	 * @param input the input
	 * @param rqstTm the rqst tm
	 * @param respTm the resp tm
	 * @param request the request
	 * @param respBuf the resp buf
	 * @param status the status
	 * @param logline the logline
	 */
	public void logTransaction(	String appId, String tId, String action, 
			String input, String rqstTm, String respTm, String request, String respBuf, String status, LogLine logline) {	
		try {
			// we only run this way if we're not doing it in the CXF interceptor
			if (!AAIConfig.get(AAIConstants.AAI_LOGGING_HBASE_INTERCEPTOR).equalsIgnoreCase("true")) {
				if (AAIConfig.get(AAIConstants.AAI_LOGGING_HBASE_ENABLED).equalsIgnoreCase("true")) {
					txn = new AAITxnLog(tId, appId);
					// tid, status, rqstTm, respTm, srcId, rsrcId, rsrcType, rqstBuf, respBuf		
					String hbtid = txn.put(tId, status, 
							rqstTm, respTm, appId, input, action, request, respBuf);

					logline.add("HbTransId",hbtid);
					logline.add("action",  action);
					logline.add("urlin", input);
				}

			}
		} catch (AAIException e) {
			// i think we do nothing
		}
	}
	
	/* ----------helpers for common consumer actions ----------- */
	
	/**
	 * Sets the depth.
	 *
	 * @param depthParam the depth param
	 * @return the int
	 * @throws AAIException the AAI exception
	 */
	protected int setDepth(String depthParam) throws AAIException {
		int depth = Integer.MAX_VALUE; //default 
		if (depthParam != null && depthParam.length() > 0 && !depthParam.equals("all")){
			try {
				depth = Integer.valueOf(depthParam);
			} catch (Exception e) {
				throw new AAIException("AAI_4016");
			}
		}
		return depth;
	}

	/**
	 * Consumer exception response generator.
	 *
	 * @param headers the headers
	 * @param info the info
	 * @param templateAction the template action
	 * @param e the e
	 * @param logline the logline
	 * @return the response
	 */
	protected Response consumerExceptionResponseGenerator(HttpHeaders headers, UriInfo info, HttpMethod templateAction, AAIException e, LogLine logline) {
		ArrayList<String> templateVars = new ArrayList<String>();
		templateVars.add(templateAction.toString()); //GET, PUT, etc
		templateVars.add(info.getPath().toString());
		templateVars.addAll(e.getTemplateVars());
		return Response
				.status(e.getErrorObject().getHTTPResponseCode())
				.entity(ErrorLogHelper.getRESTAPIErrorResponseWithLogging(headers.getAcceptableMediaTypes(), e, templateVars, logline))
				.build();
	}
	
	/**
	 * Validate introspector.
	 *
	 * @param obj the obj
	 * @param loader the loader
	 * @param uri the uri
	 * @param validateRequired the validate required
	 * @throws AAIException the AAI exception
	 * @throws UnsupportedEncodingException the unsupported encoding exception
	 */
	protected void validateIntrospector(Introspector obj, Loader loader, URI uri, boolean validateRequired) throws AAIException, UnsupportedEncodingException {
		
		IntrospectorValidator validator = new IntrospectorValidator.Builder(loader.getLogLineBuilder()).
				validateRequired(validateRequired).
				addResolver(new CreateUUID()).
				addResolver(new DefaultFields()).
				addResolver(new InjectKeysFromURI(loader, uri)).
				build();
		boolean result = validator.validate(obj);
		if (!result) {
			result = validator.resolveIssues();
		}
		if (!result) {
			List<String> messages = new ArrayList<>();
			for (Issue issue : validator.getIssues()) {
				messages.add(issue.getDetail());
			}
			String errors = Joiner.on(",").join(messages);
			throw new AAIException("AAI_3000", errors);
		}
		//check that key in payload and key in request uri are the same
        String objURI = obj.getURI();
        //if requested object is a parent objURI will have a leading slash the input uri will lack
        //this adds that leading slash for the comparison
        String testURI = "/" + uri.getRawPath();
        if (!testURI.endsWith(objURI)) {
        	throw new AAIException("AAI_3000", "uri and payload keys don't match");
        }
	}
}
