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
public class NetConsumptionModel extends PowerState.ConsumptionModel {
  
   final double alphaConst = 3.66;
   final double hwConst = 10;
    @Override
    protected double evaluateConsumption(double load) {
        return Math.min(alphaConst * Math.log((load+0.01))  + hwConst,myPowerState.getConsumptionRange() + myPowerState.getMinConsumption());
    }
    
}
