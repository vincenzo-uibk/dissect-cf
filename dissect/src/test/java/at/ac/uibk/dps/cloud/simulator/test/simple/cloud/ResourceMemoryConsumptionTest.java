package at.ac.uibk.dps.cloud.simulator.test.simple.cloud;

import static org.junit.Assert.*;

import java.util.HashMap;

import hu.mta.sztaki.lpds.cloud.simulator.iaas.IaaSService;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine.MemoryIntensiveTask;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.pmscheduling.SchedulingDependentMachines;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.MaxMinConsumer;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.MaxMinProvider;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption.ConsumptionEvent;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmscheduling.FirstFitScheduler;
import hu.mta.sztaki.lpds.cloud.simulator.io.Repository;
import hu.mta.sztaki.lpds.cloud.simulator.io.VirtualAppliance;
import hu.mta.sztaki.lpds.cloud.simulator.util.SeedSyncer;

import org.junit.Before;
import org.junit.Test;

import at.ac.uibk.dps.cloud.simulator.test.ConsumptionEventAssert;

public class MemoryIntensiveTaskTest extends ResourceConsumptionTest{

	@Before
	public void setupConsumption() {
		offer = new MaxMinProvider(processingTasklen);
		utilize = new MaxMinConsumer(processingTasklen);
		con = new MemoryIntensiveTask(processingTasklen,
				ResourceConsumption.unlimitedProcessing, utilize, offer,
				new ConsumptionEventAssert(),0.5,1000);
		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void checkDirtyingRateGreaterThanOne(){
		offer = new MaxMinProvider(processingTasklen);
		utilize = new MaxMinConsumer(processingTasklen);
		con = new MemoryIntensiveTask(processingTasklen,
				ResourceConsumption.unlimitedProcessing, utilize, offer,
				new ConsumptionEventAssert(),1.1,1000);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void checkDirtyingRateLesserThanZero(){
		offer = new MaxMinProvider(processingTasklen);
		utilize = new MaxMinConsumer(processingTasklen);
		con = new MemoryIntensiveTask(processingTasklen,
				ResourceConsumption.unlimitedProcessing, utilize, offer,
				new ConsumptionEventAssert(),-0.1,1000);
	}
	
	/*@Test(timeout = 100)
	public void suspendResumeConsumption() {
		con.suspend();
		assertTrue(con.getMemDirtyingRate() == 0.0);
		con.registerConsumption();
		assertTrue(con.getMemDirtyingRate() == 0.5);
		//super.suspendConsumption();
		
	}*/

	
	
	
}
