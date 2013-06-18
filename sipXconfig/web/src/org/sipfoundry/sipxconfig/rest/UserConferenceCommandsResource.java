/**
 *
 *
 * Copyright (c) 2010 / 2011 eZuce, Inc. All rights reserved.
 * Contributed to SIPfoundry under a Contributor Agreement
 *
 * This software is free software; you can redistribute it and/or modify it under
 * the terms of the Affero General Public License (AGPL) as published by the
 * Free Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 */
package org.sipfoundry.sipxconfig.rest;

import java.io.Serializable;
import com.thoughtworks.xstream.XStream;
import org.sipfoundry.sipxconfig.common.BeanWithId;

import org.apache.commons.lang.StringUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.sipfoundry.sipxconfig.conference.ActiveConferenceContext;
import org.sipfoundry.sipxconfig.conference.Conference;
import org.sipfoundry.sipxconfig.conference.ConferenceBridgeContext;
import org.springframework.beans.factory.annotation.Required;

public class UserConferenceCommandsResource extends UserResource {
    private static final String INVITATION_SENT = "<command-response>Invitation sent</command-response>";
    private static final String INCORECT_INVITE_COMMAND = "Incorect invite command";

    private ConferenceBridgeContext m_conferenceBridgeContext;
    private ActiveConferenceContext m_activeConferenceContext;
    private String m_confName;
    private String[] m_arguments;

    @Override
    public void init(Context context, Request request, Response response) {
        super.init(context, request, response);
        m_confName = (String) getRequest().getAttributes().get("confName");
        String argumentList = (String) getRequest().getAttributes().get("command");
        m_arguments = StringUtils.split(argumentList, '&');
        getVariants().add(new Variant(MediaType.TEXT_ALL));
	getVariants().add(new Variant(MediaType.TEXT_XML));
    }

    @Override
    public Representation represent(Variant variant) throws ResourceException {
        Conference conference = m_conferenceBridgeContext.findConferenceByName(m_confName);
        if (conference == null) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Conference not found");
        }
        if (!conference.hasOwner() || !StringUtils.equals(conference.getOwner().getName(), getUser().getName())) {
            throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, "User is not owner of this conference");
        }
        if (m_arguments == null || m_arguments.length == 0) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "No conference command specified");
        }
        if (StringUtils.equals(m_arguments[0], "invite")) {
            if (m_arguments.length == 2) {
                m_activeConferenceContext.inviteParticipant(getUser(), conference, m_arguments[1]);
                return new StringRepresentation(INVITATION_SENT);
            } else {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, INCORECT_INVITE_COMMAND);
            }
        } else if (StringUtils.equals(m_arguments[0], "inviteim")) {
            if (m_arguments.length == 2) {
                m_activeConferenceContext.inviteImParticipant(getUser(), conference, m_arguments[1]);
                return new StringRepresentation(INVITATION_SENT);
            } else {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, INCORECT_INVITE_COMMAND);
            }
        }
        String response = m_activeConferenceContext.executeCommand(conference, m_arguments);
        return new StringRepresentation(response);
    }


    @Override
    public void storeRepresentation( Representation entity) throws ResourceException {
	Conference conference = m_conferenceBridgeContext.findConferenceByName(m_confName);
	if (conference == null) {
	    throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "conference not found");
	}
	if (!(conference.hasOwner() && StringUtils.equals(conference.getOwner().getName(), getUser().getName())) &&
	    !getUser().isAdmin()) {
	    throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, "User is not owner of this conference ");
	}
	if (m_arguments == null || m_arguments.length == 0) {
	    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "No conference command specified");
	}
	if (entity == null) {
	    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "No data defined");
	}
	if (StringUtils.equals(m_arguments[0], "change")) {
	    ConferenceRepresentation conferenceRep = new ConferenceRepresentation(entity);
	    Representable confValue = conferenceRep.getObject();
	    
	    conference.setEnabled(confValue.isEnabled());

	    if (StringUtils.isNotBlank(confValue.getName())) {
		conference.setName(confValue.getName());
	    }
	    if (StringUtils.isNotBlank(confValue.getDescription())) {
		conference.setDescription(confValue.getDescription());
	    }
	    if (StringUtils.isNotBlank(confValue.getExtension())) {
		conference.setExtension(confValue.getExtension());
	    }
	    if (StringUtils.isNotBlank(confValue.getParticipantAccessCode())) {
		conference.setSettingValue(Conference.PARTICIPANT_CODE,
					   confValue.getParticipantAccessCode());
	    }
	    if (StringUtils.isNotBlank(confValue.getOrganizerAccessCode())) {
		conference.setSettingValue(Conference.ORGANIZER_CODE,
					   confValue.getOrganizerAccessCode());
	    }

	    if (StringUtils.isNotBlank(confValue.getMaxLegs())) {
		conference.setSettingValue(Conference.MAX_LEGS,
					   confValue.getMaxLegs());
	    }
	    m_conferenceBridgeContext.saveConference(conference);
	    getResponse().setStatus(Status.SUCCESS_OK, "changed done");
	}
    }
    
    @SuppressWarnings("serial")
    static class Representable implements Serializable {
	@SuppressWarnings("unused")
	private boolean m_enabled;
	@SuppressWarnings("unused")
	private String m_name;
	@SuppressWarnings("unused")
	private String m_description;
	@SuppressWarnings("unused")
	private String m_extension;
	@SuppressWarnings("unused")
	private String m_participantAccessCode;
	@SuppressWarnings("unused")
	private String m_organizerAccessCode;
	@SuppressWarnings("unused")
	private String m_maxLegs;
	
	public Representable(Conference conference) {
	    m_enabled = conference.isEnabled();
	    m_name = conference.getName();
	    m_description = conference.getDescription();
	    m_extension = conference.getExtension();
	    m_participantAccessCode = conference.getParticipantAccessCode();
	    m_organizerAccessCode = conference.getOrganizerAccessCode();
	    m_maxLegs = conference.getSettingValue(Conference.MAX_LEGS);
	}
	
	public Boolean isEnabled() {
	    return m_enabled;
	}
	
	public String getName() {
	    return m_name;
	}
	
	public String getDescription() {
	    return m_description;
	}
	
	public String getExtension() {
	    return m_extension;
	}
	
	public String getParticipantAccessCode() {
	    return m_participantAccessCode;
	}
	
	public String getOrganizerAccessCode() {
	    return m_organizerAccessCode;
	}
		
	public String getMaxLegs() {
	    return m_maxLegs;
	}
    }
    
    
    static class ConferenceRepresentation extends XStreamRepresentation<Representable> {
	private static final String ID = "m_id";
	private static final String ENABLED = "enabled";
	private static final String NAME = "name";
	private static final String DESCRIPTION = "description";
	private static final String EXTENSION = "extension";
	private static final String PARTICIPANT_AC = "participantAccessCode";
	private static final String ORGANIZER_AC = "organizerAccessCode";
	private static final String MAX_LEGS = "maxLegs";
	
	public ConferenceRepresentation(Representation representation) {
	    super(representation);
	}
	
        @Override
        protected void configureXStream(XStream xstream) {
	    xstream.omitField(BeanWithId.class, ID);
	    xstream.alias("conference", Representable.class);
	    xstream.aliasField(ENABLED, Representable.class, ENABLED);
	    xstream.aliasField(NAME, Representable.class, NAME);
	    xstream.aliasField(DESCRIPTION, Representable.class, DESCRIPTION);
	    xstream.aliasField(EXTENSION, Representable.class, EXTENSION);
	    xstream.aliasField(PARTICIPANT_AC, Representable.class, PARTICIPANT_AC);
	    xstream.aliasField(ORGANIZER_AC, Representable.class, ORGANIZER_AC);
	    xstream.aliasField(MAX_LEGS, Representable.class, MAX_LEGS);
	    xstream.omitField(Representable.class, ID);
	}
    }
    
    @Required
    public void setConferenceBridgeContext(ConferenceBridgeContext conferenceBridgeContext) {
        m_conferenceBridgeContext = conferenceBridgeContext;
    }

    @Required
    public void setActiveConferenceContext(ActiveConferenceContext activeConferenceContext) {
        m_activeConferenceContext = activeConferenceContext;
    }

    public String[] getArguments() {
        return m_arguments;
    }

}
