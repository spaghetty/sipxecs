/*
 *
 *
 * Copyright (C) 2007 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 * $
 */
package org.sipfoundry.sipxconfig.dialplan.config;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sipfoundry.sipxconfig.address.AddressManager;
import org.sipfoundry.sipxconfig.address.Address;
import org.sipfoundry.sipxconfig.bridge.BridgeSbc;
import org.sipfoundry.sipxconfig.dialplan.IDialingRule;
import org.sipfoundry.sipxconfig.mwi.Mwi;
import org.sipfoundry.sipxconfig.proxy.ProxyManager;
import org.sipfoundry.sipxconfig.registrar.Registrar;
import org.sipfoundry.sipxconfig.sbc.DefaultSbc;
import org.sipfoundry.sipxconfig.sbc.SbcDevice;
import org.sipfoundry.sipxconfig.sbc.SbcDeviceManager;
import org.sipfoundry.sipxconfig.sbc.SbcManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


/**
 * Controls very initial SIP message routing from proxy based on SIP method and potentialy message
 * content.
 */
public class ForwardingRules extends RulesFile implements ApplicationContextAware {
    private static final String LOWER = "&lt;";
    private static final String NAME_FIELD = "name";
    private static final String PATTERN_FIELD = "pattern";
    private static final String ROUTE_FIELD = "routeTo";

    private SbcManager m_sbcManager;
    private List<String> m_routes;
    private SbcDeviceManager m_sbcDeviceManager;
    private AddressManager m_addressManager;
    private VelocityEngine m_velocityEngine;
    private ApplicationContext m_context;


    public void setSbcManager(SbcManager sbcManager) {
        m_sbcManager = sbcManager;
    }

    public void setSbcDeviceManager(SbcDeviceManager sbcDeviceManager) {
        m_sbcDeviceManager = sbcDeviceManager;
    }

    public SbcDeviceManager getSbcDeviceManager() {
        return m_sbcDeviceManager;
    }

    public void begin() {
        m_routes = new ArrayList<String>();
    }

    public void generate(IDialingRule rule) {
        m_routes.addAll(Arrays.asList(rule.getHostPatterns()));
    }

    public void end() {
    }

    public class ForwardingMethod {
        private String m_name;
        private List<Map> m_fields;
        private String m_defaultRoute;

        public ForwardingMethod(String name, String defRoute) {
            if (name != null) {
                m_name = name;
            }
            m_fields = new ArrayList<Map>();
            if (defRoute != null) {
                m_defaultRoute = defRoute;
            } else {
                m_defaultRoute = "";
            }
        }

        public Map getMethodMap() {
            Map methodMap = new HashMap<String, List>();
            methodMap.put(m_name, m_fields);
            return methodMap;
        }

        public String getName() {
            return m_name;
        }

        public List<Map> getFields() {
            return m_fields;
        }

        public void addField(Map<String, String> field) {
            Map<String, String> newField = new HashMap<String, String>();
            for (String key : field.keySet()) {
                newField.put(key, field.get(key));
            }
            m_fields.add(newField);
        }

        public String getDefRoute() {
            return m_defaultRoute;
        }
    }

    @Override
    public void write(Writer writer) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("routes", m_routes);

        DefaultSbc sbc = m_sbcManager.loadDefaultSbc();
        context.put("sbc", sbc);

        if (sbc != null) {
            context.put("exportLocalIpAddress", !sbc.getRoutes().isEmpty());
        }

        context.put("auxSbcs", m_sbcManager.loadAuxSbcs());
        context.put("dollar", "$");

        // set required sipx services in context
        context.put("domainName", getDomainName());
        context.put("proxyAddress", m_addressManager.getSingleAddress(ProxyManager.TCP_ADDRESS, getLocation()));
        context.put("regAddress", m_addressManager.getSingleAddress(Registrar.TCP_ADDRESS, getLocation()));
        context.put("location", getLocation());


        // Defineng the subscribe formarding methods to be place in forwarding rules
        ForwardingMethod subscribe = new ForwardingMethod("SUBSCRIBE",
            LOWER + m_addressManager.getSingleAddress(Registrar.TCP_ADDRESS, getLocation()).stripProtocol()
            + ";transport=tcp;x-sipx-routetoreg&gt;");

        Address mwiSingleAddress = m_addressManager.getSingleAddress(Mwi.SIP_TCP, getLocation());
        if (null != mwiSingleAddress) {
            Map<String, String> msgSummary = new HashMap<String, String>();
            msgSummary.put(NAME_FIELD, "\"Event\"");
            msgSummary.put(PATTERN_FIELD, "message-summary.*");
            msgSummary.put(ROUTE_FIELD, LOWER + mwiSingleAddress.stripProtocol() + ";transport=tcp&gt;");
            subscribe.addField(msgSummary);
        }

        Address registrarAddress = m_addressManager.getSingleAddress(Registrar.EVENT_ADDRESS, getLocation());
        if (null != registrarAddress) {
            Map<String, String> reg = new HashMap<String, String>();
            reg.put(NAME_FIELD, "\"Event\"");
            reg.put(PATTERN_FIELD, "reg");
            reg.put(ROUTE_FIELD, LOWER + registrarAddress.stripProtocol() + ";transport=tcp&gt;");
            subscribe.addField(reg);
        }

        // List of all method
        Map<String, ForwardingMethod> methodsLocalFwd = new HashMap<String, ForwardingMethod>();
        methodsLocalFwd.put("subscribe", subscribe);


        // write local forwarding rules providers
        Map<String, ForwardLocalRuleProvider> beansLocal = m_context.getBeansOfType(ForwardLocalRuleProvider.class);
        if (beansLocal != null) {
            for (ForwardLocalRuleProvider beanLocal : beansLocal.values()) {
                if (beanLocal.isEnabled()) {
                    Map newfield = beanLocal.getMethodField();
                    String newfieldMethod = ((String) newfield.get("method")).toLowerCase();
                    if (methodsLocalFwd.get(newfieldMethod) != null) {
                        methodsLocalFwd.get(newfieldMethod).addField(newfield);
                    }
                }
            }
        }

        // Adding methods for local forward to the context
        context.put("locals", methodsLocalFwd);


        List<BridgeSbc> bridgeSbcs = new ArrayList<BridgeSbc>();
        List<SbcDevice> sbcDevices = m_sbcDeviceManager.getSbcDevices();
        for (SbcDevice device : sbcDevices) {
            if (device.getModel().isInternalSbc()) {
                bridgeSbcs.add((BridgeSbc) device);
            }
        }
        context.put("bridgeSbcs", bridgeSbcs);
        try {
            m_velocityEngine.mergeTemplate("commserver/forwardingrules.vm", context, writer);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void setAddressManager(AddressManager addressManager) {
        m_addressManager = addressManager;
    }

    public void setVelocityEngine(VelocityEngine velocityEngine) {
        m_velocityEngine = velocityEngine;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        m_context = context;
    }
}
