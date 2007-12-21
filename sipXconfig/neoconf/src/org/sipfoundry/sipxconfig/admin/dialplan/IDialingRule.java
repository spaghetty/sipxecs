/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 * $
 */
package org.sipfoundry.sipxconfig.admin.dialplan;

import java.util.List;

import org.sipfoundry.sipxconfig.admin.dialplan.config.Transform;
import org.sipfoundry.sipxconfig.gateway.Gateway;

/**
 * IDialingRule
 */
public interface IDialingRule {
    public abstract String getDescription();

    public abstract void setDescription(String description);

    public abstract boolean isEnabled();

    public abstract void setEnabled(boolean enabled);

    public abstract String getName();

    public abstract void setName(String name);

    public abstract List<Gateway> getGateways();

    public abstract String[] getPatterns();

    public abstract Transform[] getTransforms();

    public abstract List<String> getPermissionNames();

    public abstract Integer getId();

    public abstract boolean isInternal();

    /**
     * Whether or not the dialing rule can be used with a gateway.
     */
    public abstract boolean isGatewayAware();

    public abstract String[] getTransformedPatterns(Gateway gateway);

    /**
     * List of host patterns for this rule, if empty rule will be appended to default host match
     *
     * @return ip addresses, host names, or variables defined in config.defs
     */
    public abstract String[] getHostPatterns();
}
