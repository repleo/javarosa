/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.javarosa.core.util;

import java.util.Random;
import java.util.Vector;

import org.javarosa.core.services.PropertyManager;

public class PropertyUtils {

	//need 'addpropery' too.
	public static String initializeProperty(String propName, String defaultValue) {
		Vector propVal = PropertyManager._().getProperty(propName);
		if (propVal == null || propVal.size() == 0) {
			propVal = new Vector();
			propVal.addElement(defaultValue);
			PropertyManager._().setProperty(propName, propVal);
			//#if debug.output==verbose
			System.out.println("No default value for [" + propName
					+ "]; setting to [" + defaultValue + "]"); // debug
			//#endif
			return defaultValue;
		}
		return (String) propVal.elementAt(0);
	}
	
	
	/**
	 * Generate an RFC 1422 Version 4 UUID.
	 * 
	 * @return a uuid
	 */
	public static String genUUID() {
		return randHex(8) + "-" + randHex(4) + "-4"  + randHex(3) + "-" + Integer.toString(8 + MathUtils.getRand().nextInt(4), 16) + randHex(3) + "-"  + randHex(12);
	}
	
	/**
	 * Create a globally unique identifier string in no particular format
	 * with len characters of randomness.  
	 * 
	 * @param len The length of the string identifier requested.
	 * @return A string containing len characters of random data.
	 */
	public static String genGUID(int len) {
		String guid = "";
		for (int i = 0; i < len; i++) { // 25 == 128 bits of entropy
			guid += Integer.toString(MathUtils.getRand().nextInt(36), 36);
		}
		return guid.toUpperCase();
	}
	
	public static String randHex(int len) {
		String ret = "";
		Random r = MathUtils.getRand();
		for(int i = 0 ; i < len; ++i) {
			ret += Integer.toString(r.nextInt(16), 16);
		}
		return ret;
	}
	
	public static String trim (String guid, int len) {
		return guid.substring(0, Math.min(len, guid.length()));
	}
}
