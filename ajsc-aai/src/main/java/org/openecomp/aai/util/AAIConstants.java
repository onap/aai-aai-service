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

public final class AAIConstants {
	
	/** Default to unix file separator if system property file.separator is null */
	public static final String AAI_FILESEP = (System.getProperty("file.separator") == null) ? "/" : System.getProperty("file.separator");
	
	/** Default to opt aai if system property aai.home is null, using file.separator */
	public static final String AAI_HOME = (System.getProperty("AJSC_HOME") == null) ? AAI_FILESEP + "opt" + AAI_FILESEP + "app" + AAI_FILESEP +"aai" : System.getProperty("AJSC_HOME"); 
	public static final String AAI_BUNDLECONFIG_NAME = (System.getProperty("BUNDLECONFIG_DIR") == null) ? "bundleconfig" : System.getProperty("BUNDLECONFIG_DIR");
	public static final String AAI_HOME_BUNDLECONFIG = (System.getProperty("AJSC_HOME") == null) ? AAI_FILESEP + "opt" + AAI_FILESEP + "app" + AAI_FILESEP + "aai" + AAI_FILESEP + AAI_BUNDLECONFIG_NAME : System.getProperty("AJSC_HOME")+ AAI_FILESEP + AAI_BUNDLECONFIG_NAME; 

	/** etc directory, relative to AAI_HOME */
	public static final String AAI_HOME_ETC = AAI_HOME_BUNDLECONFIG + AAI_FILESEP + "etc" + AAI_FILESEP;
	public static final String AAI_HOME_ETC_APP_PROPERTIES = AAI_HOME_ETC + "appprops" + AAI_FILESEP;
	public static final String AAI_HOME_ETC_AUTH = AAI_HOME_ETC + "auth" + AAI_FILESEP;
	public static final String AAI_CONFIG_FILENAME = AAI_HOME_ETC_APP_PROPERTIES + "aaiconfig.properties";
	
	public static final String AAI_HOME_ETC_OXM = AAI_HOME_ETC + "oxm" + AAI_FILESEP;
	
	public static final String AAI_GETRES_LOGBACK_PROPS = "getres-logback.xml";
	public static final String AAI_DELTOOL_LOGBACK_PROPS = "deltool-logback.xml";
	public static final String AAI_UPDTOOL_LOGBACK_PROPS = "updtool-logback.xml";
	public static final String AAI_PUTTOOL_LOGBACK_PROPS = "puttool-logback.xml";
	public static final String AAI_POSTTOOL_LOGBACK_PROPS = "posttool-logback.xml";
	public static final String AAI_RSHIPTOOL_LOGBACK_PROPS = "rshiptool-logback.xml";
	public static final String AAI_LOGBACK_PROPS = "logback.xml";
	

	public static final String AAI_CREATE_DB_SCHEMA_LOGBACK_PROPS = "createDBSchema-logback.xml";
	
	public static final String AAI_DATA_GROOMING_LOGBACK_PROPS = "dataGrooming-logback.xml";
	public static final String AAI_DATA_SNAPSHOT_LOGBACK_PROPS = "dataSnapshot-logback.xml";
	public static final String AAI_SCHEMA_MOD_LOGBACK_PROPS = "schemaMod-logback.xml";

	public static final String AAI_TRUSTSTORE_FILENAME = "aai.truststore.filename";
	public static final String AAI_TRUSTSTORE_PASSWD = "aai.truststore.passwd";
	public static final String AAI_KEYSTORE_FILENAME = "aai.keystore.filename";
	public static final String AAI_KEYSTORE_PASSWD = "aai.keystore.passwd";
	
	public static final String AAI_SERVER_URL_BASE = "aai.server.url.base";
	public static final String AAI_SERVER_URL = "aai.server.url";
	public static final String AAI_GLOBAL_CALLBACK_URL = "aai.global.callback.url";
	
	public static final String AAI_DEFAULT_API_VERSION = "v8";
	public static final String AAI_DEFAULT_API_VERSION_PROP = "aai.default.api.version";
	public static final String AAI_NOTIFICATION_CURRENT_VERSION = "aai.notification.current.version";
	
    public static final String AAI_NODENAME = "aai.config.nodename";

    public static final String AAI_LOGGING_HBASE_INTERCEPTOR = "aai.logging.hbase.interceptor";
    public static final String AAI_LOGGING_HBASE_ENABLED = "aai.logging.hbase.enabled";
    public static final String AAI_LOGGING_HBASE_LOGREQUEST = "aai.logging.hbase.logrequest";
    public static final String AAI_LOGGING_HBASE_LOGRESPONSE = "aai.logging.hbase.logresponse";
    
    public static final String AAI_LOGGING_TRACE_ENABLED = "aai.logging.trace.enabled";
    public static final String AAI_LOGGING_TRACE_LOGREQUEST = "aai.logging.trace.logrequest";
    public static final String AAI_LOGGING_TRACE_LOGRESPONSE = "aai.logging.trace.logresponse";
    
    public static final String AAI_CONFIG_CHECKINGTIME = "aai.config.checktime";
    public static final String AAI_DBMODEL_FILENAME = "aai.dbmodel.filename";
    public static final String AAI_RESVERSION_ENABLEFLAG = "aai.resourceversion.enableflag";

	public static final String HBASE_TABLE_NAME = "hbase.table.name";
	public static final String HBASE_TABLE_TIMESTAMP_FORMAT = "hbase.table.timestamp.format";
	public static final String HBASE_CONFIGURATION_ZOOKEEPER_QUORUM = "hbase.zookeeper.quorum";
	public static final String HBASE_CONFIGURATION_ZOOKEEPER_CLIENTPORT = "hbase.zookeeper.property.clientPort";
	public static final String HBASE_ZOOKEEPER_ZNODE_PARENT = "hbase.zookeeper.znode.parent";
	public static final String ZOOKEEPER_ZNODE_PARENT = "zookeeper.znode.parent";
	
	public static final int AAI_MAX_TRANS_RETRIES = 5;
	public static final long AAI_TRANS_RETRY_SLEEP_MSEC = 500;
	
	public static final int AAI_GROOMING_DEFAULT_MAX_FIX = 150;
	public static final int AAI_GROOMING_DEFAULT_SLEEP_MINUTES = 7;
	
	public static final String LOGGING_MAX_STACK_TRACE_ENTRIES = "aai.logging.maxStackTraceEntries";
	
	/** Default to skipping real-time grooming unless system property aai.skiprealtime.grooming is set to "false" */
	public static final String AAI_SKIPREALTIME_GROOMING = (System.getProperty("aai.skiprealtime.grooming") == null) ? "true" : System.getProperty("aai.skiprealtime.grooming");
	
	/*** UEB ***/
	public static final String UEB_PUB_PARTITION_AAI = "AAI";	
	
	
	/**
	 * Instantiates a new AAI constants.
	 */
	private AAIConstants() {
		// prevent instantiation
	}

}
