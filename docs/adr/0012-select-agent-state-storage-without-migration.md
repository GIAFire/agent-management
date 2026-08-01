# Select agent state storage without migration

Each agent configuration selects either `local_file` or `redis` for conversation state storage; MySQL is not exposed. Updating the selection takes effect directly and does not migrate existing conversation state, accepting loss of state continuity for existing sessions in exchange for avoiding a cross-store migration workflow in the current version.
