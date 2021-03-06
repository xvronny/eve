/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentConfig;
import com.almende.eve.agent.ExampleAgent;
import com.almende.eve.capabilities.wake.WakeService;
import com.almende.eve.capabilities.wake.WakeServiceConfig;
import com.almende.eve.state.file.FileStateConfig;
import com.almende.eve.transport.http.HttpTransportConfig;
import com.almende.util.callback.AsyncCallback;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class TestAgents.
 */
public class TestAgents extends TestCase {
	private static final Logger	LOG	= Logger.getLogger(TestAgents.class
											.getName());
	
	/**
	 * Test agents.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws URISyntaxException
	 *             the URI syntax exception
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	@Test
	public void testAgent() throws IOException, URISyntaxException,
			InterruptedException {
		
		final HttpTransportConfig transportConfig = new HttpTransportConfig();
		transportConfig.setServletUrl("http://localhost:8080/agents/");
		
		transportConfig.setServletLauncher("JettyLauncher");
		final ObjectNode jettyParms = JOM.createObjectNode();
		jettyParms.put("port", 8080);
		transportConfig.put("jetty", jettyParms);
		
		final AgentConfig config = new AgentConfig("example");
		config.setTransport(transportConfig);
		
		ExampleAgent agent = new ExampleAgent();
		agent.setConfig(config);
		
		final ObjectNode callParams = JOM.createObjectNode();
		callParams.put("message", "Hello world!");
		
		agent.pubSend(new URI("http://localhost:8080/agents/example"),
				"helloWorld", callParams, new AsyncCallback<String>() {
					
					@Override
					public void onSuccess(final String result) {
						LOG.warning("Received:'" + result + "'");
					}
					
					@Override
					public void onFailure(final Exception exception) {
						LOG.log(Level.SEVERE, "", exception);
						fail();
					}
					
				});
		
		LOG.warning("Sync received:'"
				+ agent.pubSendSync(new URI(
						"http://localhost:8080/agents/example"), "helloWorld",
						callParams) + "'");
		
		final WakeServiceConfig wsconfig = new WakeServiceConfig();
		final FileStateConfig state = new FileStateConfig();
		state.setPath(".wakeservices");
		state.setId("testAgents");
		wsconfig.setState(state);
		
		agent.registerAt(new WakeService(wsconfig));
		
		// Try to get rid of the agent instance from memory
		agent = null;
		System.gc();
		System.gc();
		
		final AgentConfig ac = new AgentConfig("tester");
		ac.setTransport(transportConfig);
		final ExampleAgent tester = new ExampleAgent() {
		};
		tester.setConfig(ac);
		LOG.warning("Sync received:'"
				+ tester.pubSendSync(
						new URI("http://localhost:8080/agents/example"),
						"helloWorld",
						callParams.deepCopy().put("message",
								"Hello world after sleep!")) + "'");
	}
	
}
