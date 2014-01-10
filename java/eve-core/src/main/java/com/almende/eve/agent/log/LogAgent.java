/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent.log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class LogAgent.
 */
@Access(AccessType.PUBLIC)
public class LogAgent extends Agent {
	private static final long	TIMETOLIVE	= 20 * 60 * 1000;	// milliseconds
																
	/**
	 * Config.
	 *
	 * @param agentUrl the agent url
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JSONRPCException the jSONRPC exception
	 */
	public void config(final URI agentUrl) throws IOException, JSONRPCException {
		getEventsFactory().subscribe(agentUrl, "*", "eventLog");
	}
	
	/**
	 * Event log.
	 *
	 * @param event the event
	 * @param params the params
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void eventLog(@Name("event") final String event,
			@Name("params") final ObjectNode params) throws IOException {
		final String agentId = getId().replaceFirst("_logagent_", "");
		log(new Log(agentId, event, params));
	}
	
	/**
	 * Log.
	 *
	 * @param log the log
	 */
	public void log(final Log log) {
		// TODO: use a database instead of the state - when you register
		// more and more logs this will be very unreliable.
		ArrayList<Log> logs = getState().get("logs",
				new TypeUtil<ArrayList<Log>>() {
				});
		if (logs == null) {
			logs = new ArrayList<Log>();
		}
		logs.add(log);
		
		// TODO: limit to a maximum number and age of the logs?
		
		getState().put("logs", logs);
	}
	
	/**
	 * Gets the logs.
	 *
	 * @param since the since
	 * @return the logs
	 */
	public List<Log> getLogs(final Long since) {
		final ArrayList<Log> logs = getState().get("logs",
				new TypeUtil<ArrayList<Log>>() {
				});
		
		// TODO: use a database for the logs. It is very inefficient to
		// retrieve them all and then filter them.
		final List<Log> output = new ArrayList<Log>();
		if (logs != null) {
			for (final Log log : logs) {
				if (((since == null) || (log.getTimestamp() > since))) {
					output.add(log);
				}
			}
		}
		
		// reset the time to live for the agent. It will stay alive when
		// regularly requested for logs
		setTimeToLive(TIMETOLIVE);
		
		return output;
	}
	
	/**
	 * Remove existing time to live.
	 */
	public void cancelTimeToLive() {
		final String timeoutId = getState().get("timeoutId", String.class);
		if (timeoutId != null) {
			getScheduler().cancelTask(timeoutId);
		}
		getState().remove("timeoutId");
	}
	
	/**
	 * Set a time-to-live for the LogAgent. After this timeout, it will
	 * delete itself.
	 * This is useful for a temporary LogAgent used for a single session in a
	 * browser.
	 *
	 * @param interval interval in milliseconds
	 */
	public void setTimeToLive(final long interval) {
		// remove existing timeout
		cancelTimeToLive();
		
		// create a new timeout
		final JSONRequest request = new JSONRequest("killMe", null);
		final String timeoutId = getScheduler().createTask(request, interval);
		getState().put("timeoutId", timeoutId);
	}
	
	/**
	 * Delete the log agent.
	 *
	 * @throws JSONRPCException the jSONRPC exception
	 * @throws ClassNotFoundException the class not found exception
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws NoSuchMethodException the no such method exception
	 */
	public void killMe() throws JSONRPCException, ClassNotFoundException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		getAgentHost().deleteAgent(getId());
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#getDescription()
	 */
	@Override
	public String getDescription() {
		return "The LogAgent can temporarily log events of an agent. "
				+ "The agent is meant for internal use by the AgentHost.";
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#getVersion()
	 */
	@Override
	public String getVersion() {
		return "0.1";
	}
	
}
