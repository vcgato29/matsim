/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**<p>
 * This is, I think, a configurable wrapper/adapter class that essentially uses
 * <tt>(new DijkstraFactory()).createPathCalculator( costCalculator, timeCalculator )</tt>
 * to bundle costCalculator and timeCalculator with the Dijkstra s.p. algorithm into a MATSim Person/PlanAlgo.
 * </p><p>
 * Dijkstra can be replaced by something else by using the <tt>factory</tt> parameter.
 * </p>
 */
public class PlansCalcRoute extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private static final Logger log = Logger.getLogger(PlansCalcRoute.class);

	private static final String NO_CONFIGGROUP_SET_WARNING = "No PlansCalcRouteConfigGroup"
		+ " is set in PlansCalcRoute, using the default values. Make sure that those values" +
				"fit your needs, otherwise set it expclicitly.";

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	/**
	 * The routing algorithm to be used for finding routes on the network with actual travel times.
	 */
	private final LeastCostPathCalculator routeAlgo;
	/**
	 * The routing algorithm to be used for finding pt routes, based on the empty network, with freeflow travel times.
	 */
	private final LeastCostPathCalculator routeAlgoPtFreeflow;

	/**
	 * if not set via constructor use the default values
	 */
	protected PlansCalcRouteConfigGroup configGroup = new PlansCalcRouteConfigGroup();

	private final NetworkFactoryImpl routeFactory;

	protected final Network network;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	/**Does the following (as far as I can see):<ul>
	 * <li> sets routeAlgo to the path calculator defined by <tt>factory</tt>, using <tt>costCalculator</tt> and <tt>timeCalculator</tt> as arguments </li>
	 * <li> sets routeAlgoPtFreeflow to "-1 utils/sec" (which is <it>enormous</it>--????) </li>
	 * <li> sets configGroup to <tt>group</tt> but it is not clear where this will be used.
	 * </ul>
	 * [[old javadoc: Uses the speed factors from the config group and the rerouting of the factory]]
	 */
	public PlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network,
			final TravelCost costCalculator,
			final TravelTime timeCalculator, LeastCostPathCalculatorFactory factory){
		this.routeAlgo = factory.createPathCalculator(network, costCalculator, timeCalculator);
		FreespeedTravelTimeCost ptTimeCostCalc = new FreespeedTravelTimeCost(-1.0, 0.0, 0.0);
		this.routeAlgoPtFreeflow = factory.createPathCalculator(network, ptTimeCostCalc, ptTimeCostCalc);
		this.network = network;
		this.routeFactory = (NetworkFactoryImpl) network.getFactory();
		if (group != null) {
			this.configGroup = group;
		}
		else {
			log.warn(NO_CONFIGGROUP_SET_WARNING);
		}
	}

	/**
	 * Creates a rerouting strategy module using dijkstra rerouting.  Does the following (as far as I can see):<ul>
	 * <li> sets routeAlgo to the path calculator defined by <tt>new DijkstraFactory()</tt>, using <tt>costCalculator</tt> and <tt>timeCalculator</tt> as arguments </li>
	 * <li> sets routeAlgoPtFreeflow to "-1 utils/sec" (which is <it>enormous</it>--????) </li>
	 * <li> sets configGroup to <tt>group</tt> but it is not clear where this will be used.
	 * </ul>
	 */
	public PlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network, final TravelCost costCalculator, final TravelTime timeCalculator) {
		this(group, network, costCalculator, timeCalculator, new DijkstraFactory());
	}

	/**
	 * @param router The routing algorithm to be used for finding routes on the network with actual travel times.
	 * @param routerFreeflow The routing algorithm to be used for finding routes in the empty network, with freeflow travel times.
	 * @deprecated use one of the other constructors of this class
	 */
	@Deprecated
	public PlansCalcRoute(final Network network, final LeastCostPathCalculator router, final LeastCostPathCalculator routerFreeflow) {
		super();
		this.network = network;
		this.routeAlgo = router;
		this.routeAlgoPtFreeflow = routerFreeflow;
		this.routeFactory = (NetworkFactoryImpl) network.getFactory();
		log.warn(NO_CONFIGGROUP_SET_WARNING);
	}

	public final LeastCostPathCalculator getLeastCostPathCalculator(){
		return this.routeAlgo;
	}

	public final LeastCostPathCalculator getPtFreeflowLeastCostPathCalculator(){
		return this.routeAlgoPtFreeflow;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		for (Plan plan : person.getPlans()) {
			handlePlan(plan);
		}
	}

	public void run(final Plan plan) {
		handlePlan(plan);
	}

	//////////////////////////////////////////////////////////////////////
	// helper methods
	//////////////////////////////////////////////////////////////////////

	protected void handlePlan(final Plan plan) {
		double now = 0;

		// loop over all <act>s
		ActivityImpl fromAct = null;
		ActivityImpl toAct = null;
		LegImpl leg = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof LegImpl) {
				leg = (LegImpl) pe;
			} else if (pe instanceof ActivityImpl) {
				if (fromAct == null) {
					fromAct = (ActivityImpl) pe;
				} else {
					toAct = (ActivityImpl) pe;

					double endTime = fromAct.getEndTime();
					double startTime = fromAct.getStartTime();
					double dur = fromAct.getDuration();
					if (endTime != Time.UNDEFINED_TIME) {
						// use fromAct.endTime as time for routing
						now = endTime;
					} else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
						// use fromAct.startTime + fromAct.duration as time for routing
						now = startTime + dur;
					} else if (dur != Time.UNDEFINED_TIME) {
						// use last used time + fromAct.duration as time for routing
						now += dur;
					} else {
						throw new RuntimeException("activity of plan of person " + plan.getPerson().getId().toString() + " has neither end-time nor duration." + fromAct.toString());
					}

					now += handleLeg(leg, fromAct, toAct, now);

					fromAct = toAct;
				}
			}
		}
	}

	/**
	 * @param leg the leg to calculate the route for.
	 * @param fromAct the Act the leg starts
	 * @param toAct the Act the leg ends
	 * @param depTime the time (seconds from midnight) the leg starts
	 * @return the estimated travel time for this leg
	 */
	public double handleLeg(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		TransportMode legmode = leg.getMode();

		if (legmode == TransportMode.car) {
			return handleCarLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == TransportMode.ride) {
			return handleRideLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == TransportMode.pt) {
			return handlePtLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == TransportMode.walk) {
			return handleWalkLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == TransportMode.bike) {
			return handleBikeLeg(leg, fromAct, toAct, depTime);
		} else if (legmode == TransportMode.undefined) {
			/* balmermi: No clue how to handle legs with 'undef' mode
			 *                Therefore, handle it similar like bike mode with 50 km/h
			 *                and no route assigned  */
			return handleUndefLeg(leg, fromAct, toAct, depTime);
		} else {
			throw new RuntimeException("cannot handle legmode '" + legmode + "'.");
		}
	}

	protected double handleCarLeg(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		double travTime = 0;
		Link fromLink = this.network.getLinks().get(fromAct.getLinkId());
		Link toLink = this.network.getLinks().get(toAct.getLinkId());
		if (fromLink == null) throw new RuntimeException("fromLink missing.");
		if (toLink == null) throw new RuntimeException("toLink missing.");

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

//		CarRoute route = null;
		Path path = null;
		if (toLink != fromLink) {
			// do not drive/walk around, if we stay on the same link
			path = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			NetworkRouteWRefs route = (NetworkRouteWRefs) this.routeFactory.createRoute(TransportMode.car, fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			route.setTravelTime((int) path.travelTime);
			route.setTravelCost(path.travelCost);
			leg.setRoute(route);
			travTime = (int) path.travelTime;
		} else {
			// create an empty route == staying on place if toLink == endLink
			NetworkRouteWRefs route = (NetworkRouteWRefs) this.routeFactory.createRoute(TransportMode.car, fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			leg.setRoute(route);
			travTime = 0;
		}

		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(depTime + travTime);
		return travTime;
	}

	private double handleRideLeg(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		// handle a ride exactly the same was as a car
		// the simulation has to take care that this leg is not really simulated as a stand-alone driver
		return handleCarLeg(leg, fromAct, toAct, depTime);
	}

	private double handlePtLeg(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {

		int travTime = 0;
		final Link fromLink = this.network.getLinks().get(fromAct.getLinkId());
		final Link toLink = this.network.getLinks().get(toAct.getLinkId());
		if (fromLink == null) throw new RuntimeException("fromLink missing.");
		if (toLink == null) throw new RuntimeException("toLink missing.");

		Path path = null;
//		CarRoute route = null;
		if (toLink != fromLink) {
			Node startNode = fromLink.getToNode();	// start at the end of the "current" link
			Node endNode = toLink.getFromNode(); // the target is the start of the link
			// do not drive/walk around, if we stay on the same link
			path = this.routeAlgoPtFreeflow.calcLeastCostPath(startNode, endNode, depTime);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			// we're still missing the time on the final link, which the agent has to drive on in the java mobsim
			// so let's calculate the final part.
			double travelTimeLastLink = ((LinkImpl) toLink).getFreespeedTravelTime(depTime + path.travelTime);
			travTime = (int) (((int) path.travelTime + travelTimeLastLink) * this.configGroup.getPtSpeedFactor());
			RouteWRefs route = this.routeFactory.createRoute(TransportMode.pt, fromLink.getId(), toLink.getId());
			route.setTravelTime(travTime);
			double dist = 0;
			if ((fromAct.getCoord() != null) && (toAct.getCoord() != null)) {
				dist = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
			} else {
				dist = CoordUtils.calcDistance(fromLink.getCoord(), toLink.getCoord());
			}
			route.setDistance(dist * 1.5);
//			route.setTravelCost(path.travelCost);
			leg.setRoute(route);
		} else {
			// create an empty route == staying on place if toLink == endLink
			RouteWRefs route = this.routeFactory.createRoute(TransportMode.pt, fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			leg.setRoute(route);
			travTime = 0;
		}
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(depTime + travTime);
		return travTime;
	}

	private double handleWalkLeg(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		// make simple assumption about distance and walking speed
		double dist = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
		// create an empty route, but with realistic traveltime
		RouteWRefs route = this.routeFactory.createRoute(TransportMode.walk, fromAct.getLinkId(), toAct.getLinkId());
		int travTime = (int)(dist / this.configGroup.getWalkSpeedFactor());
		route.setTravelTime(travTime);
		route.setDistance(dist * 1.5);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(depTime + travTime);
		return travTime;
	}

	private double handleBikeLeg(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		// make simple assumption about distance and cycling speed
		double dist = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
		// create an empty route, but with realistic traveltime
		RouteWRefs route = this.routeFactory.createRoute(TransportMode.bike, fromAct.getLinkId(), toAct.getLinkId());
		int travTime = (int)(dist / this.configGroup.getBikeSpeedFactor());
		route.setTravelTime(travTime);
		route.setDistance(dist * 1.5);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(depTime + travTime);
		return travTime;
	}

	private double handleUndefLeg(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		// make simple assumption about distance and a dummy speed (50 km/h)
		double dist = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
		// create an empty route, but with realistic traveltime
		RouteWRefs route = this.routeFactory.createRoute(TransportMode.undefined, fromAct.getLinkId(), toAct.getLinkId());
		int travTime = (int)(dist / this.configGroup.getUndefinedModeSpeedFactor());
		route.setTravelTime(travTime);
		route.setDistance(dist * 1.5);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(depTime + travTime);
		return travTime;
	}

	public NetworkFactoryImpl getRouteFactory() {
		return routeFactory;
	}

}
