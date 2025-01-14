/*
 *
 * Copyright (C) 2015-2020 Anarchy Engine Open Source Contributors (see CONTRIBUTORS.md)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

package engine.lua.history;

import java.util.ArrayList;
import java.util.HashMap;

import engine.lua.type.object.Instance;

public class HistoryStack {
	private HashMap<Instance, HistoryObjectReference> objectReferences;
	private HashMap<HistoryObjectReference, Instance> historyToInstanceMap;
	private boolean busy;
	
	/**
	 * This indicates where we are in the stack.
	 * We use this so that we can still keep track of REDO snapshots.
	 */
	private int latestIndex;
	
	/**
	 * List of snapshots.
	 */
	private ArrayList<HistorySnapshot> snapshots = new ArrayList<HistorySnapshot>();
	
	public HistoryStack() {
		this.objectReferences = new HashMap<>();
		this.historyToInstanceMap = new HashMap<>();
	}
	
	public void clear() {
		snapshots.clear();
		latestIndex = 0;
	}
	
	/**
	 * Push a snapshot to the history stack.
	 * @param snapshot
	 */
	public void push(HistorySnapshot snapshot) {
		// Cannot make history changes if we're already processing history changes
		if ( busy )
			return;
		
		// Delete all snapshots that exist infront of the latest snapshot.
		while ( snapshots.size() > latestIndex )
			snapshots.remove(snapshots.size()-1);
		
		// Push onto stack
		snapshots.add(snapshot);
		latestIndex = snapshots.size();
	}
	
	/**
	 * Undo a change from the history stack.
	 */
	public void undo() {
		if ( !canUndo() ) {
			System.out.println("Cant undo lol nerd.");
			return;
		}
		
		busy = true;
		
		// Get the snapshot at this index
		HistorySnapshot snapshot = snapshots.get(latestIndex-1);
		
		// Undo ALL changes in the snapshot.
		for (int i = 0; i < snapshot.changes.size(); i++) {
			HistoryChange change = snapshot.changes.get(i);
			if ( change == null )
				continue;
			
			Instance object = change.getHistoryInstance().getInstance();
			if ( object == null )
				continue;
			
			object.forceset(change.getFieldChanged(), change.getValueOld());
			object.set(change.getFieldChanged(), change.getValueOld());
			
			System.out.println("UNDO: " + object.getName() + "." + change.getFieldChanged() + " --> " + change.getValueOld());
			change.getHistoryInstance().update();
		}
		
		// Decrease snapshot index
		latestIndex--;
		busy = false;
	}
	
	/**
	 * Redo a change from the history stack.
	 */
	public void redo() {
		if ( !canRedo() )
			return;
		
		busy = true;
		// increase snapshot index
		latestIndex++;
		
		// Get the snapshot at this index
		HistorySnapshot snapshot = snapshots.get(latestIndex-1);
		
		// Undo ALL changes in the snapshot.
		for (int i = 0; i < snapshot.changes.size(); i++) {
			HistoryChange change = snapshot.changes.get(i);
			
			Instance object = change.getHistoryInstance().getInstance();
			object.forceset(change.getFieldChanged(), change.getValueNew());
		}
		busy = false;
	}
	
	/**
	 * Returns whether you are able to make an undo change.
	 * @return
	 */
	public boolean canUndo() {
		return latestIndex > 0;
	}
	
	/**
	 * Returns whether you are able to make a redo change.
	 * @return
	 */
	public boolean canRedo() {
		return latestIndex < snapshots.size();
	}
	
	public HistoryObjectReference getObjectReference(Instance instance) {
		if ( objectReferences.containsKey(instance) )
			return objectReferences.get(instance);
		
		HistoryObjectReference ref = new HistoryObjectReference(this, instance);
		objectReferences.put(instance, ref);
		historyToInstanceMap.put(ref, instance);
		return ref;
	}

	protected void updateReference(Instance object, HistoryObjectReference historyObjectReference) {
		Instance olf = historyToInstanceMap.get(historyObjectReference);
		if ( olf != null ) {
			objectReferences.remove(olf);
			historyToInstanceMap.put(historyObjectReference, object);
		}
		
		objectReferences.put(object, historyObjectReference);
	}
}
