# Apply model configuration updates immediately

Model configurations remain mutable, and agent configurations reference their current content by identity instead of binding an immutable model revision. A saved model configuration update applies to subsequent model calls, including calls from existing sessions; the system does not interrupt, retry, or otherwise control calls that were already issued. This avoids model-version management for now, at the cost of historical agent configurations not fully reproducing prior model behavior.
