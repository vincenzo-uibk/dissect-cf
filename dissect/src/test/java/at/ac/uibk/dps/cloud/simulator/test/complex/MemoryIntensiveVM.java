package at.ac.uibk.dps.cloud.simulator.test.complex;

import static org.junit.Assert.*;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine.ResourceMemoryConsumption;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.MaxMinConsumer;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.MaxMinProvider;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption;

import org.junit.Test;

import at.ac.uibk.dps.cloud.simulator.test.ConsumptionEventAssert;
import at.ac.uibk.dps.cloud.simulator.test.simple.cloud.VMTest;
import org.junit.Before;

public class MemoryIntensiveVM extends VMTest{

	
	@Before
	public void setupVM(){
		
	}
	
	@Test
	public void testGetTotalDirtyingRate() {
		
	}

	@Test
	public void testGetTotalMemoryPages() {
		fail("Not yet implemented");
	}

}
