/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.scheduling;

import com.almende.eve.capabilities.Config;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class SimpleSchedulerConfig.
 */
public class SimpleSchedulerConfig extends Config {
	
	/**
	 * Instantiates a new simple scheduler config.
	 */
	public SimpleSchedulerConfig() {
		this(JOM.createObjectNode());
	}
	
	/**
	 * Instantiates a new simple scheduler config.
	 * 
	 * @param node
	 *            the node
	 */
	public SimpleSchedulerConfig(final ObjectNode node) {
		super(node);
		if (!node.has("class")) {
			setClassName(SimpleSchedulerBuilder.class.getName());
		}
	}
}
