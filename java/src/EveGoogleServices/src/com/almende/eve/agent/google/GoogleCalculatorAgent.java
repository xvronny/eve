/**
 * @file GoogleCalculatorAgent.java
 * 
 * @brief 
 * CalcAgent can evaluate mathematical expressions. 
 * It uses the Google calculator API.
 *
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Copyright © 2012 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date	  2011-04-02
 */

package com.almende.eve.agent.google;

import java.net.URLEncoder;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import com.almende.eve.agent.Agent;
import com.almende.eve.json.annotation.ParameterName;
import com.almende.eve.json.util.HttpUtil;

@SuppressWarnings("serial")
public class GoogleCalculatorAgent extends Agent {
	static private String CALC_API_URL = "http://www.google.com/ig/calculator";

	/**
	 * Evaluate given expression
	 * For example expr="2.5 + 3 / sqrt(16)" will return "3.25"
	 * @param expr
	 * @return result
	 * @throws Exception
	 */
	public String eval(@ParameterName("expr") String expr) throws Exception {
		String url = CALC_API_URL + "?q=" + URLEncoder.encode(expr, "UTF-8");
		String resp = HttpUtil.get(url);
		JSONObject json = (JSONObject) JSONSerializer.toJSON(resp);
		
		String error = json.getString("error");
		if (error != null && !error.isEmpty()) {
			throw new Exception(error);
		}
		
		String rhs = json.getString("rhs");
		return rhs;
	}
	
	@Override
	public String getVersion() {
		return "1.0";
	}
	
	@Override
	public String getDescription() {
		return 
			"GoogleCalculatorEvent can evaluate mathematical expressions. " + 
			"It uses the Google calculator API.";
	}
}