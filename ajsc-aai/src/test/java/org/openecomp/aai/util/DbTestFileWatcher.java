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

import java.util.*;
import java.io.*;

public abstract class DbTestFileWatcher extends TimerTask {
  private long timeStamp;
  private File file;

  /**
   * Instantiates a new db test file watcher.
   *
   * @param file the file
   */
  public DbTestFileWatcher( File file ) {
    this.file = file;
    this.timeStamp = file.lastModified();
  }

  /**
   * {@inheritDoc}
   */
  public final void run() {
    long timeStamp = file.lastModified();

    if( (timeStamp - this.timeStamp) > 500 ) {
      this.timeStamp = timeStamp;
      onChange(file);
    }
    try {
		Thread.sleep(1000);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }
  
  /**
   * On change.
   *
   * @param file the file
   */
  protected abstract void onChange( File file );
}
