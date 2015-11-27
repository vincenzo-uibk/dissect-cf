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
public class PiecewiseLinearCPUModel extends PowerState.ConsumptionModel{

    double lowLoadMult,highLoadMult;
    //m01-m02
    //final double loadSplit = 0.12;
    //final double lowLoadCoeff1 = 495.5966,lowLoadCoeff2 = 1864;
    //final double highLoadCoeff1 = 709.0335,highLoadCoeff2 = 174;
    //o1-o2
    final double loadSplit = 0.12;
    final double lowLoadCoeff1 = 166.8518,lowLoadCoeff2 = 920;
    final double highLoadCoeff1 = 254.1,highLoadCoeff2 = 118;

    
    @Override
    protected double evaluateConsumption(double load) {
        lowLoadMult = extractGeneralCoefficient(lowLoadCoeff1, lowLoadCoeff2, loadSplit);
        highLoadMult = (highLoadCoeff1 + highLoadCoeff2*loadSplit - myPowerState.getMinConsumption())/
                (myPowerState.getConsumptionRange());
        //highLoadSep = (highLoadMult + myPowerState.getMinConsumption())/myPowerState.getConsumptionRange();
                
        double pow = (load < loadSplit)? myPowerState.getMinConsumption() + lowLoadMult * myPowerState.getConsumptionRange() * load :
            myPowerState.getMinConsumption() + highLoadMult * myPowerState.getConsumptionRange() 
                + (1 - highLoadMult) * myPowerState.getConsumptionRange() * load;
        
        return Math.min(pow,myPowerState.getMinConsumption() + myPowerState.getConsumptionRange());
    }
    
    private double extractGeneralCoefficient(double coeff1,double coeff2,double loadSplit){
        return (coeff1 + coeff2 * loadSplit - myPowerState.getMinConsumption())/
                (myPowerState.getConsumptionRange()*loadSplit);
    }
    
}
