/*
 *
 *
 * Copyright (C) 2007 Sip2ser Srl., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 */
package org.sipfoundry.sipxconfig.phone.snom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sipfoundry.sipxconfig.common.event.DaoEventListener;
import org.sipfoundry.sipxconfig.phone.PhoneContext;


public class LangUploadListener implements DaoEventListener {
    private static final Log LOG = LogFactory.getLog(LangUploadListener.class);
    private static final String GROUP_LANG_VERSION = "group.version/localization.version";
    private PhoneContext m_phoneContext;

    public void setPhoneContext(PhoneContext phoneContext) {
        m_phoneContext = phoneContext;
    }

    @Override
    public void onDelete(Object entity) {
    }

    @Override
    public void onSave(Object entity) {
    }
}
