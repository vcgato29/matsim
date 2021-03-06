/* *********************************************************************** *
 * project: org.matsim.*
 * DescriptivePiStatisticsFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.socnetgen.sna.math;


import org.matsim.contrib.common.stats.DescriptivePiStatistics;

/**
 * A factory for creating pre-configured instance of
 * {@link DescriptivePiStatistics}.
 * 
 * @author illenberger
 * 
 */
public interface DescriptivePiStatisticsFactory {

	/**
	 * Creates and returns a new instance of {@link DescriptivePiStatistics}.
	 * 
	 * @return a new instance of {@link DescriptivePiStatistics}.
	 */
	public DescriptivePiStatistics newInstance();

}
