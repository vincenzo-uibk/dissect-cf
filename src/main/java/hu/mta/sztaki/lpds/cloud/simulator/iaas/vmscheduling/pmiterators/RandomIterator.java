/*
 *  ========================================================================
 *  DIScrete event baSed Energy Consumption simulaTor 
 *    					             for Clouds and Federations (DISSECT-CF)
 *  ========================================================================
 *  
 *  This file is part of DISSECT-CF.
 *  
 *  DISSECT-CF is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at
 *  your option) any later version.
 *  
 *  DISSECT-CF is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 *  General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with DISSECT-CF.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  (C) Copyright 2015, Gabor Kecskemeti (kecskemeti.gabor@sztaki.mta.hu)
 */
package hu.mta.sztaki.lpds.cloud.simulator.iaas.vmscheduling.pmiterators;

import hu.mta.sztaki.lpds.cloud.simulator.iaas.PhysicalMachine;
import hu.mta.sztaki.lpds.cloud.simulator.util.SeedSyncer;

import java.util.List;

public class RandomIterator extends PMIterator {

    private long resetCounter=0;
    private int[] randomIndexes = new int[0];

    public RandomIterator(List<PhysicalMachine> pmlist) {
        super(pmlist);
    }

    @Override
    public void reset() {
        super.reset();
        resetCounter++;
        if (maxIndex != randomIndexes.length||resetCounter%10000==0) {
            randomIndexes = new int[maxIndex];
            for (int i = 0; i < maxIndex; i++) {
                boolean regen;
                int proposedIndex = -1;
                do {
                    regen = false;
                    proposedIndex = SeedSyncer.centralRnd.nextInt(maxIndex);
                    for (int j = 0; j < i && !regen; j++) {
                        regen |= proposedIndex == randomIndexes[j];
                    }
                } while (regen);
                randomIndexes[i] = proposedIndex;
            }
        }
    }

    @Override
    public PhysicalMachine next() {
        return pmList.get(randomIndexes[index++]);
    }
}
