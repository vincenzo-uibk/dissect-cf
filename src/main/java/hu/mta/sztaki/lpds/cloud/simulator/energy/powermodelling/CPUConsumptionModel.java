/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hu.mta.sztaki.lpds.cloud.simulator.energy.powermodelling;

/**
 *
 * @author vincenzo
 */
public class CPUConsumptionModel extends PowerState.ConsumptionModel {

    final double alphaConst = 73.13;
    final double hwConst = 371.0;
    @Override
    protected double evaluateConsumption(double load) {
        return Math.min(alphaConst * Math.log((load+0.01))  + hwConst,myPowerState.getConsumptionRange() + myPowerState.getMinConsumption());
    }
    
}
