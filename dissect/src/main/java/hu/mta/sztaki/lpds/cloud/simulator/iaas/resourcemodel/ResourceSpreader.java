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
 *  (C) Copyright 2014, Gabor Kecskemeti (gkecskem@dps.uibk.ac.at,
 *   									  kecskemeti.gabor@sztaki.mta.hu)
 */

package hu.mta.sztaki.lpds.cloud.simulator.iaas.resourcemodel;

import hu.mta.sztaki.lpds.cloud.simulator.Timed;
import hu.mta.sztaki.lpds.cloud.simulator.energy.powermodelling.PowerState;
import hu.mta.sztaki.lpds.cloud.simulator.util.ArrayHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class ensures equal access to resource limited devices (such as network
 * interfaces, cpus or disks) for all ongoing resource consumptions.
 * 
 * Resource processing is actually handled by the processSingleConsumption
 * function which must be implemented externally for performance. This allows
 * the resource consumption simulation code to run with less conditional
 * statements in its core. While it also allows to efficiently add new features
 * later on.
 * 
 * @author 
 *         "Gabor Kecskemeti, Distributed and Parallel Systems Group, University of Innsbruck (c) 2013"
 *         "Gabor Kecskemeti, Laboratory of Parallel and Distributed Systems, MTA SZTAKI (c) 2012"
 * 
 */
public abstract class ResourceSpreader {

	// These final variables define the base behavior of the class:
	/**
	 * Maximum amount of resources to be shared among the consumption objects
	 * during a single tick.
	 */
	protected double perTickProcessingPower;
	protected double negligableProcessing;
	/**
	 * The array of consumption objects that will share the processing power of
	 * this spreader. The order is not guaranteed!
	 */
	private final ArrayList<ResourceConsumption> toProcess = new ArrayList<ResourceConsumption>();
	public final List<ResourceConsumption> underProcessing = Collections
			.unmodifiableList(toProcess);
	int underProcessingLen = 0;
	private FreqSyncer mySyncer = null;
	private ArrayList<ResourceConsumption> underAddition = new ArrayList<ResourceConsumption>();
	private ArrayList<ResourceConsumption> underRemoval = new ArrayList<ResourceConsumption>();
	public final List<ResourceConsumption> toBeAdded = Collections
			.unmodifiableList(underAddition);

	public interface PowerBehaviorChangeListener {
		void behaviorChanged(ResourceSpreader onSpreader, PowerState newState);
	}

	private PowerState currentPowerBehavior;
	private CopyOnWriteArrayList<PowerBehaviorChangeListener> listeners = new CopyOnWriteArrayList<PowerBehaviorChangeListener>();

	protected long lastNotifTime = 0;
	private double totalProcessed = 0;
	private boolean stillInDepGroup;

	public static class FreqSyncer extends Timed {
		/**
		 * myDepGroup is always kept in order: first all the providers are
		 * listed, then all the consumers, when dealing with this data member
		 * please keep in mind this expected behavior.
		 */
		private ResourceSpreader[] myDepGroup;
		private int depgrouplen;
		private int firstConsumerId;
		private final ArrayList<ResourceSpreader> depGroupExtension = new ArrayList<ResourceSpreader>();
		private boolean nudged = false;
		private boolean regularFreqMode = true;

		private FreqSyncer(final ResourceSpreader provider,
				final ResourceSpreader consumer) {
			myDepGroup = new ResourceSpreader[2];
			myDepGroup[0] = provider;
			myDepGroup[1] = consumer;
			firstConsumerId = 1;
			depgrouplen = 2;
			provider.mySyncer = consumer.mySyncer = this;
			setBackPreference(true);
		}

		private FreqSyncer(ResourceSpreader[] myDepGroup, final int provcount,
				final int dglen) {
			this.myDepGroup = myDepGroup;
			firstConsumerId = provcount;
			depgrouplen = dglen;
			for (int i = 0; i < dglen; i++) {
				myDepGroup[i].mySyncer = this;
			}
			setBackPreference(true);
		}

		/**
		 * Should only be used from addToGroup!
		 * 
		 * @param rs
		 */
		private void addSingleToDG(final ResourceSpreader rs) {
			try {
				myDepGroup[depgrouplen] = rs;
				depgrouplen++;
				rs.mySyncer = this;
			} catch (ArrayIndexOutOfBoundsException e) {
				ResourceSpreader[] newdg = new ResourceSpreader[myDepGroup.length * 7];
				System.arraycopy(myDepGroup, 0, newdg, 0, depgrouplen);
				myDepGroup = newdg;
				addSingleToDG(rs);
			}
		}

		private void addToGroup() {
			int size = depGroupExtension.size();
			for (int i = 0; i < size; i++) {
				final ResourceSpreader rs = depGroupExtension.get(i);
				if (isInDepGroup(rs))
					continue;
				if (rs.isConsumer()) {
					addSingleToDG(rs);
				} else {
					if (firstConsumerId >= depgrouplen) {
						addSingleToDG(rs);
					} else {
						addSingleToDG(myDepGroup[firstConsumerId]);
						myDepGroup[firstConsumerId] = rs;
					}
					firstConsumerId++;
				}
				rs.mySyncer = this;
			}
		}

		private boolean isInDepGroup(final ResourceSpreader lookfor) {
			final int start = lookfor.isConsumer() ? firstConsumerId : 0;
			final int stop = start == 0 ? firstConsumerId : depgrouplen;
			// We will just check the part of the depgroup where
			// consumers or providers are located
			int i = start;
			for (; i < stop && myDepGroup[i] != lookfor; i++)
				;
			return i != stop;
		}

		void nudge() {
			if (nudged)
				return;
			nudged = true;
			updateFrequency(0);
		}

		/**
		 * Only those should get the depgroup with this function who are not
		 * planning to change it's contents
		 * 
		 * WARNING: If, for some reason, the contents of the returned array are
		 * changed then the proper operation of FreqSyncer cannot be guaranteed
		 * anymore.
		 * 
		 * @return
		 */
		ResourceSpreader[] getDepGroup() {
			return myDepGroup;
		}

		public int getDGLen() {
			return depgrouplen;
		}

		/**
		 * This will always give a fresh copy of the depgroup which can be
		 * changed as the user desires. Because of the always copying behavior
		 * it will reduce the performance a little.
		 * 
		 * @return
		 */
		public ResourceSpreader[] getClonedDepGroup() {
			return Arrays.copyOfRange(myDepGroup, 0, depgrouplen);
		}

		@Override
		public String toString() {
			return "FreqSyncer(" + super.toString() + " depGroup: "
					+ Arrays.toString(myDepGroup) + ")";
		}

		public int getFirstConsumerId() {
			return firstConsumerId;
		}

		protected final void outOfOrderProcessing(final long currentTime) {
			for (int i = 0; i < depgrouplen; i++) {
				myDepGroup[i].doProcessing(currentTime);
			}
		}

		@Override
		public void tick(final long fires) {
			boolean didRemovals = false;
			boolean didExtension;
			do {
				outOfOrderProcessing(fires);
				depGroupExtension.clear();
				nudged = false;
				didExtension = false;
				for (int rsi = 0; rsi < depgrouplen; rsi++) {
					final ResourceSpreader rs = myDepGroup[rsi];
					// managing removals
					if (!rs.underRemoval.isEmpty()) {
						didRemovals = true;
						int rsuLen = rs.toProcess.size();
						int urLen = rs.underRemoval.size();
						boolean isConsumer = rs.isConsumer();
						for (int urIndex = 0; urIndex < urLen; urIndex++) {
							final ResourceConsumption con = rs.underRemoval
									.get(urIndex);
							if (ArrayHandler.removeAndReplaceWithLast(
									rs.toProcess, con)) {
								rsuLen--;
							}
							if (isConsumer) {
								if (con.getUnProcessed() == 0) {
									con.ev.conComplete();
								} else if (!con.isResumable()) {
									con.ev.conCancelled(con);
								}
							}
						}
						rs.underProcessingLen = rsuLen;
						rs.underRemoval.clear();
					}
					// managing additions
					if (!rs.underAddition.isEmpty()) {
						if (rs.underProcessingLen == 0) {
							rs.lastNotifTime = fires;
						}
						final int uaLen = rs.underAddition.size();
						for (int i = 0; i < uaLen; i++) {
							final ResourceConsumption con = rs.underAddition
									.get(i);
							rs.toProcess.add(con);
							final ResourceSpreader cp = rs.getCounterPart(con);
							// Check if counterpart is in the dependency group
							if (!isInDepGroup(cp)) {
								// No it is not, we need an extension
								didExtension = true;
								if (cp.mySyncer == null || cp.mySyncer == this) {
									// Just this single item is missing
									if (!depGroupExtension.contains(cp)) {
										depGroupExtension.add(cp);
									}
								} else {
									// There are further items missing
									cp.mySyncer.unsubscribe();
									for (int j = 0; j < cp.mySyncer.depgrouplen; j++) {
										final ResourceSpreader todepgroupextension = cp.mySyncer.myDepGroup[j];
										if (!depGroupExtension
												.contains(todepgroupextension)) {
											depGroupExtension
													.add(todepgroupextension);
										}
									}
									// Make sure, that if we encounter this cp
									// next time we will not try to add all its
									// dep group
									cp.mySyncer = null;
								}
							}
						}
						rs.underProcessingLen += uaLen;
						rs.underAddition.clear();
					}
				}
				if (didExtension) {
					addToGroup();
				}
			} while (didExtension || nudged);

			if (didRemovals) {
				// Marking all current members of the depgroup as non members
				for (int i = 0; i < depgrouplen; i++) {
					myDepGroup[i].stillInDepGroup = false;
				}
				ResourceSpreader[] notClassified = myDepGroup;
				int providerCount = firstConsumerId;
				int notClassifiedLen = depgrouplen;
				do {
					int classifiableindex = 0;
					// finding the first dependency group
					for (; classifiableindex < notClassifiedLen; classifiableindex++) {
						final ResourceSpreader rs = notClassified[classifiableindex];
						buildDepGroup(rs);
						if (rs.stillInDepGroup) {
							break;
						}
						rs.mySyncer = null;
					}
					if (classifiableindex < notClassifiedLen) {
						notClassifiedLen -= classifiableindex;
						providerCount -= classifiableindex;
						// Remove the unused front
						System.arraycopy(notClassified, classifiableindex,
								notClassified, 0, notClassifiedLen);
						// Remove the not classified items
						ResourceSpreader[] stillNotClassified = null;
						int newpc = 0;
						int newlen = 0;
						for (int i = 0; i < notClassifiedLen; i++) {
							final ResourceSpreader rs = notClassified[i];
							if (!rs.stillInDepGroup) {
								notClassifiedLen--;
								// Management of the new group
								if (stillNotClassified == null) {
									stillNotClassified = new ResourceSpreader[notClassifiedLen];
								}
								stillNotClassified[newlen++] = rs;
								// Removals from the old group
								if (rs.isConsumer()) {
									notClassified[i] = notClassified[notClassifiedLen];
								} else {
									providerCount--;
									notClassified[i] = notClassified[providerCount];
									notClassified[providerCount] = notClassified[notClassifiedLen];
									newpc++;
								}
							}
						}
						// We now have the new groups so we can start
						// subscribing
						FreqSyncer subscribeMe;
						if (notClassified == myDepGroup) {
							depgrouplen = notClassifiedLen;
							firstConsumerId = providerCount;
							subscribeMe = this;
						} else {
							subscribeMe = new FreqSyncer(notClassified,
									providerCount, notClassifiedLen);
						}
						// Ensuring freq updates for every newly created group
						subscribeMe.updateMyFreqNow();
						if (stillNotClassified == null) {
							// No further spreaders to process
							break;
						} else {
							// let's work on the new spreaders
							notClassified = stillNotClassified;
							providerCount = newpc;
							notClassifiedLen = newlen;
						}
					} else {
						// nothing left in notclassified that can be use in
						// dependency groups
						notClassifiedLen = 0;
						if (notClassified == myDepGroup) {
							depgrouplen = 0;
						}
					}
				} while (notClassifiedLen != 0);
				if (notClassified == myDepGroup && depgrouplen == 0) {
					// No group was created we have to unsubscribe
					unsubscribe();
				}
			} else {
				updateMyFreqNow();
			}
		}

		private void updateMyFreqNow() {
			final long newFreq = myDepGroup[0].singleGroupwiseFreqUpdater();
			regularFreqMode = newFreq != 0;
			updateFrequency(newFreq);
		}

		public boolean isRegularFreqMode() {
			return regularFreqMode;
		}

		private void buildDepGroup(final ResourceSpreader startingItem) {
			final int upLen;
			if ((upLen = startingItem.toProcess.size()) == 0
					|| startingItem.stillInDepGroup) {
				return;
			}
			startingItem.stillInDepGroup = true;
			for (int i = 0; i < upLen; i++) {
				buildDepGroup(startingItem
						.getCounterPart(startingItem.toProcess.get(i)));
			}
		}
	}

	/**
	 * This constructor just saves the processing power that can be spread in
	 * every tick by the newly instantiated spreader.
	 * 
	 * @param initialProcessingPower
	 *            Maximum usable bandwidth in a during a single timing event
	 */
	public ResourceSpreader(final double initialProcessingPower) {
		setPerTickProcessingPower(initialProcessingPower);
	}

	public final FreqSyncer getSyncer() {
		return mySyncer;
	}

	protected abstract long singleGroupwiseFreqUpdater();

	protected final void removeTheseConsumptions(
			final ResourceConsumption[] conList, final int len) {
		for (int i = 0; i < len; i++) {
			underRemoval.add(conList[i]);
			ArrayHandler.removeAndReplaceWithLast(underAddition, conList[i]);
		}
		if (mySyncer != null) {
			mySyncer.nudge();
		}
	}

	/**
	 * When a new consumption is initiated it must be registered to the
	 * corresponding spreader with this function.
	 * 
	 * The consumption object is added to the array of current consumptions.
	 * This function also makes sure that the timing events arrive if this is
	 * the first object in the array.
	 * 
	 * WARNING: This function should not be called by anyone else but the
	 * registration function of the resource consumption! (Otherwise duplicate
	 * registrations could happen!)
	 * 
	 * @param con
	 *            The consumption object to be registered
	 */
	static boolean registerConsumption(final ResourceConsumption con) {
		final ResourceSpreader provider = con.getProvider();
		final ResourceSpreader consumer = con.getConsumer();
		if (con.isRegistered()
				|| !(provider.isAcceptableConsumption(con) && consumer
						.isAcceptableConsumption(con))) {
			return false;
		}
		// ResourceConsumption synchronization
		ArrayHandler.removeAndReplaceWithLast(provider.underRemoval, con);
		ArrayHandler.removeAndReplaceWithLast(consumer.underRemoval, con);

		provider.underAddition.add(con);
		consumer.underAddition.add(con);

		boolean notnudged = true;
		if (provider.mySyncer != null) {
			provider.mySyncer.nudge();
			notnudged = false;
		}

		if (consumer.mySyncer != null) {
			consumer.mySyncer.nudge();
			notnudged = false;
		}
		if (notnudged) {
			new FreqSyncer(provider, consumer).nudge();
		}

		return true;
	}

	protected boolean isAcceptableConsumption(final ResourceConsumption con) {
		return getSamePart(con).equals(this) && perTickProcessingPower > 0
				&& con.getHardLimit() > 0;
	}

	static void cancelConsumption(final ResourceConsumption con) {
		final ResourceSpreader provider = con.getProvider();
		final ResourceSpreader consumer = con.getConsumer();
		ResourceConsumption[] sinlgeConsumption = new ResourceConsumption[] { con };
		provider.removeTheseConsumptions(sinlgeConsumption, 1);
		consumer.removeTheseConsumptions(sinlgeConsumption, 1);
	}

	private void doProcessing(final long currentFireCount) {
		if (currentFireCount == lastNotifTime && mySyncer.isRegularFreqMode()) {
			return;
		}
		ResourceConsumption[] toRemove = null;
		boolean firsthit = true;
		int remIdx = 0;
		final long ticksPassed = currentFireCount - lastNotifTime;
		for (int i = 0; i < underProcessingLen; i++) {
			final ResourceConsumption con = underProcessing.get(i);
			final double processed = processSingleConsumption(con, ticksPassed);
			if (processed < 0) {
				totalProcessed -= processed;
				if (firsthit) {
					toRemove = new ResourceConsumption[underProcessingLen - i];
					firsthit = false;
				}
				toRemove[remIdx++] = con;
			} else {
				totalProcessed += processed;
			}
		}
		if (remIdx > 0) {
			removeTheseConsumptions(toRemove, remIdx);
		}
		lastNotifTime = currentFireCount;
	}

	protected abstract double processSingleConsumption(
			final ResourceConsumption con, final long ticksPassed);

	protected abstract ResourceSpreader getCounterPart(
			final ResourceConsumption con);

	protected abstract ResourceSpreader getSamePart(
			final ResourceConsumption con);

	protected abstract boolean isConsumer();

	public double getTotalProcessed() {
		if (mySyncer != null) {
			final long currTime = Timed.getFireCount();
			if (isConsumer()) {
				// We first have to make sure the providers provide the
				// stuff that this consumer might need
				final int len = mySyncer.getFirstConsumerId();
				final ResourceSpreader[] dg = mySyncer.myDepGroup;
				for (int i = 0; i < len; i++) {
					dg[i].doProcessing(currTime);
				}
			}
			doProcessing(currTime);
		}
		return totalProcessed;
	}

	public double getPerTickProcessingPower() {
		return perTickProcessingPower;
	}

	protected void setPerTickProcessingPower(double perTickProcessingPower) {
		// if (isSubscribed()) {
		// // TODO: this case might be interesting to support.
		// throw new IllegalStateException(
		// "It is not possible to change the processing power of a spreader while it is subscribed!");
		// }
		this.perTickProcessingPower = perTickProcessingPower;
		this.negligableProcessing = this.perTickProcessingPower / 1000000000;
	}

	public PowerState getCurrentPowerBehavior() {
		return currentPowerBehavior;
	}

	// FIXME: this might be protected later on.
	public void setCurrentPowerBehavior(final PowerState newPowerBehavior) {
		if (newPowerBehavior == null) {
			throw new IllegalStateException(
					"Trying to set an unknown power behavior");
		}
		if (currentPowerBehavior != newPowerBehavior) {
			currentPowerBehavior = newPowerBehavior;
			final int size = listeners.size();
			for (int i = 0; i < size; i++) {
				listeners.get(i).behaviorChanged(this, newPowerBehavior);
			}
		}
	}

	public void subscribePowerBehaviorChangeEvents(
			final PowerBehaviorChangeListener pbcl) {
		listeners.add(pbcl);
	}

	public void unsubscribePowerBehaviorChangeEvents(
			final PowerBehaviorChangeListener pbcl) {
		listeners.remove(pbcl);
	}

	@Override
	public String toString() {
		return "RS(processing: "
				+ toProcess.toString()
				+ "in power state: "
				+ (currentPowerBehavior == null ? "-" : currentPowerBehavior
						.toString()) + ")";
	}

	static int hashCounter = 0;
	private int myHashCode = getHashandIncCounter();

	static int getHashandIncCounter() {
		// FIXME
		// WARNING: some possible hash collisions!
		return hashCounter++;
	}

	@Override
	public final int hashCode() {
		return myHashCode;
	}
}
