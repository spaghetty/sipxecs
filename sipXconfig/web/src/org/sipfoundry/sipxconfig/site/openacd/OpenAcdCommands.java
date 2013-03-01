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
package org.sipfoundry.sipxconfig.site.openacd;

import static org.sipfoundry.sipxconfig.openacd.OpenAcdContext.OPEN_ACD_LOGOUT_EXTENSION_NAME;
import static org.sipfoundry.sipxconfig.openacd.OpenAcdContext.OPEN_ACD_LOGIN_EXTENSION_NAME;
import static org.sipfoundry.sipxconfig.openacd.OpenAcdContext.OPEN_ACD_PREFIX_EXTENSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.tapestry.BaseComponent;
import org.apache.tapestry.IPage;
import org.apache.tapestry.annotations.Bean;
import org.apache.tapestry.annotations.InjectObject;
import org.apache.tapestry.annotations.InjectPage;
import org.apache.tapestry.callback.PageCallback;
import org.apache.tapestry.event.PageBeginRenderListener;
import org.apache.tapestry.event.PageEvent;
import org.apache.tapestry.valid.IValidationDelegate;
import org.apache.tapestry.valid.ValidatorException;
import org.sipfoundry.sipxconfig.common.UserException;
import org.sipfoundry.sipxconfig.components.SelectMap;
import org.sipfoundry.sipxconfig.components.TapestryUtils;
import org.sipfoundry.sipxconfig.openacd.OpenAcdCommand;
import org.sipfoundry.sipxconfig.openacd.OpenAcdContext;
import org.sipfoundry.sipxconfig.openacd.OpenAcdContextImpl.DefaultExtensionException;
import org.sipfoundry.sipxconfig.openacd.OpenAcdExtension;

public abstract class OpenAcdCommands extends BaseComponent implements PageBeginRenderListener {
    @InjectObject("spring:openAcdContext")
    public abstract OpenAcdContext getOpenAcdContext();

    public abstract OpenAcdCommand getCurrentRow();

    public abstract void setCurrentRow(OpenAcdCommand e);

    public abstract Collection<Integer> getRowsToDelete();

    @InjectPage(EditOpenAcdCommand.PAGE)
    public abstract EditOpenAcdCommand getEditCommandPage();

    @Bean
    public abstract SelectMap getSelections();

    public abstract Set<OpenAcdCommand> getOpenAcdCommands();

    public abstract void setOpenAcdCommands(Set<OpenAcdCommand> l);

    public void pageBeginRender(PageEvent event) {
        Set<OpenAcdCommand> commands = new HashSet<OpenAcdCommand>();
        for (OpenAcdCommand command : getOpenAcdContext().getCommands()) {
            String commandName = command.getName();
            if (commandName.equals(OPEN_ACD_LOGIN_EXTENSION_NAME)
                    || commandName.equals(OPEN_ACD_LOGOUT_EXTENSION_NAME)) {
                String displayName = StringUtils.stripStart(commandName, OPEN_ACD_PREFIX_EXTENSION);
                command.setName(displayName);
            }
            commands.add(command);
        }
        setOpenAcdCommands(commands);
    }

    public IPage editCommand(int id) {
        OpenAcdCommand ext = (OpenAcdCommand) getOpenAcdContext().getExtensionById(id);
        EditOpenAcdCommand page = getEditCommandPage();
        page.setOpenAcdCommandId(ext.getId());
        page.setActions(null);
        page.setCallback(new PageCallback(this.getPage()));
        return page;
    }

    public IPage addCommand() {
        EditOpenAcdCommand page = getEditCommandPage();
        page.setOpenAcdCommandId(null);
        page.setActions(null);
        page.setCallback(new PageCallback(this.getPage()));
        return page;
    }

    public void deleteCommands() {
        Collection<Integer> ids = getSelections().getAllSelected();
        if (ids.isEmpty()) {
            return;
        }

        try {
            // delete extensions one by one since we want to trigger onDeleteEvent in order to
            // have delete and mongo write in the same transaction
            // deleting bulk and publishing delete event will make 2 separate transactions
            List<String> defaultCommands = new ArrayList<String>();
            for (Integer id : getRowsToDelete()) {
                OpenAcdExtension command = getOpenAcdContext().getExtensionById(id);
                try {
                    getOpenAcdContext().deleteExtension(command);
                } catch (DefaultExtensionException e) {
                    defaultCommands.add(command.getName());
                }
            }
            if (!defaultCommands.isEmpty()) {
                String defaultCommandNames = StringUtils.join(defaultCommands.iterator(), ", ");
                String errMessage = getMessages().format("msg.err.defaultCommandsDeletion", defaultCommandNames);
                IValidationDelegate validator = TapestryUtils.getValidator(getPage());
                validator.record(new ValidatorException(errMessage));
            }
        } catch (UserException ex) {
            IValidationDelegate validator = TapestryUtils.getValidator(getPage());
            validator.record(new ValidatorException(getMessages().getMessage("msg.cannot.connect")));
        }
    }
}
