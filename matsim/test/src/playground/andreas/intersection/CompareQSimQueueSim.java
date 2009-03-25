package playground.andreas.intersection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.ActEndEvent;
import org.matsim.core.events.ActStartEvent;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.AgentWait2LinkEvent;
import org.matsim.core.events.Events;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.LinkLeaveEvent;
import org.matsim.core.events.handler.ActEndEventHandler;
import org.matsim.core.events.handler.ActStartEventHandler;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

import playground.andreas.intersection.sim.QSim;

/**
 * @author aneumann
 *
 */
public class CompareQSimQueueSim extends MatsimTestCase implements	LinkLeaveEventHandler, LinkEnterEventHandler, ActEndEventHandler, ActStartEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler, AgentWait2LinkEventHandler{
	
	BufferedWriter writer = null;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		QueueNetwork.setSimulateAllLinks(true);
		QueueNetwork.setSimulateAllNodes(true);
	}
	
	public void testCompareBothSims_wo_trafficLight(){
  		  		
		System.setProperty("line.separator", "\n"); // Unix
//		System.setProperty("line.separator", "\r\n"); // Win
		
		Config conf = loadConfig("test/input/playground/andreas/intersection/bottleneck/config.xml");
		
		String newLSADef = null;
		String newLSADefCfg = null;
		
		ScenarioImpl data = new ScenarioImpl(conf);
		Events events = new Events();
		events.addHandler(this);
		
		
		try {		
			this.writer = new BufferedWriter(new FileWriter(new File("qsim_events.txt")));
			new QSim(events, data.getPopulation(), data.getNetwork(), false, newLSADef, newLSADefCfg).run();
//			new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
			this.writer.flush();
			this.writer.close();

			MatsimRandom.reset();

			this.writer = new BufferedWriter(new FileWriter(new File("queuesim_events.txt")));

			new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
			this.writer.flush();
			this.writer.close();
			
			assertEquals(CRCChecksum.getCRCFromFile("qsim_events.txt"),	CRCChecksum.getCRCFromFile("queuesim_events.txt"));
			
			new File("qsim_events.txt").delete();
			new File("queuesim_events.txt").delete();

		} catch (IOException e) {
			e.printStackTrace();
		}		
		
  	}
	
	public void testCompareBothSims_w_trafficLight(){
	  		
		System.setProperty("line.separator", "\n"); // Unix
//		System.setProperty("line.separator", "\r\n"); // Win
		
		Config conf = loadConfig("test/input/playground/andreas/intersection/oneways/config.xml");
		
		String newLSADef = "./test/input/playground/andreas/intersection/oneways/lsa.xml";
		String newLSADefCfg = "./test/input/playground/andreas/intersection/oneways/lsa_config.xml";
		
		ScenarioImpl data = new ScenarioImpl(conf);
		Events events = new Events();
		events.addHandler(this);
		
		
		try {		
			this.writer = new BufferedWriter(new FileWriter(new File("qsim_events.txt")));
			new QSim(events, data.getPopulation(), data.getNetwork(), false, newLSADef, newLSADefCfg).run();
//			new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
			this.writer.flush();
			this.writer.close();

			MatsimRandom.reset();

			this.writer = new BufferedWriter(new FileWriter(new File("queuesim_events.txt")));

			new QueueSimulation(data.getNetwork(), data.getPopulation(), events).run();
			this.writer.flush();
			this.writer.close();
			
			assertEquals(CRCChecksum.getCRCFromFile("qsim_events.txt"),	CRCChecksum.getCRCFromFile("queuesim_events.txt"));
			
			new File("qsim_events.txt").delete();
			new File("queuesim_events.txt").delete();

		} catch (IOException e) {
			e.printStackTrace();
		}		
		
  	}  	

	public void handleEvent(LinkEnterEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	public void handleEvent(LinkLeaveEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	
	public void reset(@SuppressWarnings("unused") int iteration) {
		// Not used in that TestCase
	}

	public void handleEvent(ActEndEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(ActStartEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(AgentArrivalEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(AgentDepartureEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void handleEvent(AgentWait2LinkEvent event) {
		try {
			this.writer.write(event.toString());
			this.writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}