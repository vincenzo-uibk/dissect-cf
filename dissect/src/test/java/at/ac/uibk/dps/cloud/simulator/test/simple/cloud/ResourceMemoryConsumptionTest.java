package at.ac.uibk.dps.cloud.simulator.test.simple.cloud;

import static org.junit.Assert.*;

import java.util.HashMap;

import hu.mta.sztaki.lpds.cloud.simulator.iaas.IaaSService;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.VirtualMachine.ResourceMemoryConsumption;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.pmscheduling.SchedulingDependentMachines;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.MaxMinConsumer;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.MaxMinProvider;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel.ResourceConsumption.ConsumptionEvent;
import hu.mta.sztaki.lpds.cloud.simulator.iaas.vmscheduling.FirstFitScheduler;
import hu.mta.sztaki.lpds.cloud.simulator.io.Repository;
import hu.mta.sztaki.lpds.cloud.simulator.io.VirtualAppliance;
import hu.mta.sztaki.lpds.cloud.simulator.util.SeedSyncer;

import org.junit.*;

import at.ac.uibk.dps.cloud.simulator.test.ConsumptionEventAssert;
import hu.mta.sztaki.lpds.cloud.simulator.Timed;

public class ResourceMemoryConsumptionTest extends ResourceConsumptionTest
{

        @Before
	public void setupConsumption() 
        {
		offer = new MaxMinProvider(permsProcessing);
		utilize = new MaxMinConsumer(permsProcessing);
		con = (ResourceMemoryConsumption) createAUnitConsumption(null, 10000, 0.5);
                Assert.assertTrue(true);
	}
        
        public ResourceMemoryConsumption createAUnitConsumption(final ConsumptionEvent ce,final int pageNum,final double dirtRate) 
        {
		return new ResourceMemoryConsumption(processingTasklen,
				ResourceConsumption.unlimitedProcessing, utilize, offer,
				ce == null ? new ConsumptionEventAssert() : ce,pageNum,dirtRate);
	}
        
        @Override
        public void testConsumption()
        {
            super.testConsumption();
        }
        
        @Test(expected = IllegalStateException.class, timeout = 100)
	public void testNullEvent() 
        {
		// Below we should not receive a null pointer
		con = new ResourceMemoryConsumption(processingTasklen,
				ResourceConsumption.unlimitedProcessing, utilize, offer, null);
		Assert.fail("Should not reach tis point because we asked for a consumption with a null event");
	}
        
        public void suspendResumeConsumption()
        {
            con.suspend();
            double before = preSuspendPhase();
            con.suspend();
            Timed.fire();
            pastSuspendChecks(before);
            Assert.assertTrue(((ResourceMemoryConsumption)con).getMemDirtyingRate() == 0.0);
            con.registerConsumption();
            Assert.assertTrue(((ResourceMemoryConsumption)con).getMemDirtyingRate() > 0.0);
            Timed.simulateUntilLastEvent();
            Assert.assertEquals("Not consumed the necessary amount", 0,
                		con.getUnProcessed(), 0);
        }
        
        
        
}
