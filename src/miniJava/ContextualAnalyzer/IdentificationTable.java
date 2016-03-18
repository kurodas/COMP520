package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.Declaration;

public class IdentificationTable {

	private int level;
	private IdEntry latest;

	public IdentificationTable() {
		level = 0;
		latest = null;
	}

	// Opens a new level in the identification table, 1 higher than the
	// current topmost level.

	public void openScope() {
		level++;
	}

	// Closes the topmost level in the identification table, discarding
	// all entries belonging to that level.

	public void closeScope() {

		IdEntry entry, local;

		// Presumably, idTable.level > 0.
		entry = this.latest;
		while (entry.level == this.level) {
			local = entry;
			entry = local.previous;
		}
		this.level--;
		this.latest = entry;
	}

	// Makes a new entry in the identification table for the given identifier
	// and attribute. The new entry belongs to the current level.
	// duplicated is set to to true iff there is already an entry for the
	// same identifier at the current level.

	public void enter(String id, Declaration attr) {

		IdEntry entry = this.latest;
		boolean present = false, searching = true;

		// Check for duplicate entry ...
		while (searching) {
			if (entry == null || entry.level < this.level)
				searching = false;
			else if (entry.id.equals(id)) {
				present = true;
				searching = false;
			} else
				entry = entry.previous;
		}

		attr.duplicated = present;
		// Add new entry ...
		entry = new IdEntry(id, attr, this.level, this.latest);
		this.latest = entry;
	}

	// Finds an entry for the given identifier in the identification table,
	// if any. If there are several entries for that identifier, finds the
	// entry at the highest level, in accordance with the scope rules.
	// Returns null iff no entry is found.
	// otherwise returns the attribute field of the entry found.

	public Declaration retrieve(String id) {

		IdEntry entry;
		Declaration attr = null;
		boolean present = false, searching = true;

		entry = this.latest;
		while (searching) {
			if (entry == null)
				searching = false;
			else if (entry.id.equals(id)) {
				present = true;
				searching = false;
				attr = entry.attr;
			} else
				entry = entry.previous;
		}
		if(present)
			return attr;
		else
			return null;
	}

}
