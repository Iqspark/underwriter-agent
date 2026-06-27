package com.iqspark.underwriter.domain.model;

/**
 * The lines of business the agent can underwrite.
 *
 * <p>Vacant home is the first line built and the worked reference example; the agent is
 * line-agnostic by design (see the Line-of-Business plug-in model, doc 9). {@code VACANT_HOME},
 * {@code RENTAL} and {@code CONTENTS} are underwritten today; {@code FARM} is a placeholder.
 */
public enum LineOfBusiness {
    VACANT_HOME,
    RENTAL,
    CONTENTS,
    FARM;

    /** The default line when a submission does not declare one. */
    public static final LineOfBusiness DEFAULT = VACANT_HOME;
}
