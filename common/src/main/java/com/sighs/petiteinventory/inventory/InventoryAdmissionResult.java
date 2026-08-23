package com.sighs.petiteinventory.inventory;

/** Result of applying PetiteInventory's admission rules to an incoming stack. */
public enum InventoryAdmissionResult {
    /** The defense module stored the stack completely. */
    ACCEPTED,
    /** The defense module rejected the stack because no valid placement exists. */
    REJECTED,
    /** A normal 1x1 stack may continue through vanilla inventory handling. */
    DEFER_TO_VANILLA
}
