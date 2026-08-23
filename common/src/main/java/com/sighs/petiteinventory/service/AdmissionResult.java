package com.sighs.petiteinventory.service;

/** Outcome of a policy-controlled inventory insertion. */
public enum AdmissionResult {
    ACCEPTED,
    REJECTED,
    DEFER_TO_VANILLA
}
