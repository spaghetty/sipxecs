--inserts all default values as it would have been inserted when resetting the db
insert into openacd_agent_group (openacd_agent_group_id, name, description)
  values (nextval('openacd_agent_group_seq'), 'Default', 'Default agent group');
insert into openacd_queue_group (openacd_queue_group_id, name, description)
  values (nextval('openacd_queue_group_seq'), 'Default', 'Default queue group');
insert into openacd_queue (openacd_queue_id, name, description, openacd_queue_group_id, weight)
  values (nextval('openacd_queue_seq'), 'default_queue', 'Default queue', (select openacd_queue_group_id from openacd_queue_group where name = 'Default'), 1);

insert into openacd_skill_group(openacd_skill_group_id, name) values (1, 'Language');
insert into openacd_skill_group(openacd_skill_group_id, name) values (2, 'Magic');

insert into openacd_skill (openacd_skill_id, name, atom, description, default_skill, openacd_skill_group_id)
  values (nextval('openacd_skill_seq'), 'English', 'english', 'English', false, 1);
insert into openacd_skill (openacd_skill_id, name, atom, description, default_skill, openacd_skill_group_id)
  values (nextval('openacd_skill_seq'), 'German', 'german', 'German', false, 1);
insert into openacd_skill (openacd_skill_id, name, atom, description, default_skill, openacd_skill_group_id)
  values (nextval('openacd_skill_seq'), 'Brand', '_brand', 'Magic skill to expand to a client label (brand)', true, 2);
insert into openacd_skill (openacd_skill_id, name, atom, description, default_skill, openacd_skill_group_id)
  values (nextval('openacd_skill_seq'), 'Agent Name', '_agent', 'Magic skill that is replaced by the agent name', true, 2);
insert into openacd_skill (openacd_skill_id, name, atom, description, default_skill, openacd_skill_group_id)
  values (nextval('openacd_skill_seq'), 'Agent Profile', '_profile', 'Magic skill that is replaced by the agent profile name', true, 2);
insert into openacd_skill (openacd_skill_id, name, atom, description, default_skill, openacd_skill_group_id)
  values (nextval('openacd_skill_seq'), 'Node', '_node', 'Magic skill that is replaced by the node identifier', true, 2);
insert into openacd_skill (openacd_skill_id, name, atom, description, default_skill, openacd_skill_group_id)
  values (nextval('openacd_skill_seq'), 'Queue', '_queue', 'Magic skill replaced by a queue name', true, 2);
insert into openacd_skill (openacd_skill_id, name, atom, description, default_skill, openacd_skill_group_id)
  values (nextval('openacd_skill_seq'), 'All', '_all', 'Magic skill to denote an agent that can answer any call regardless of the other skills', true, 2);

insert into openacd_skill_queue (openacd_queue_id, openacd_skill_id)
  values ((select openacd_queue_id from openacd_queue where name = 'default_queue'), (select openacd_skill_id from openacd_skill where name = 'English'));
insert into openacd_skill_queue (openacd_queue_id, openacd_skill_id)
  values ((select openacd_queue_id from openacd_queue where name = 'default_queue'), (select openacd_skill_id from openacd_skill where name = 'Node'));

-- log in
insert into freeswitch_extension (freeswitch_ext_id, name, description, freeswitch_ext_type, did, alias, enabled)
  values (nextval('freeswitch_ext_seq'), 'login', 'Default login dial string', 'C', NULL, NULL, false);
insert into freeswitch_condition (freeswitch_condition_id, field, expression, freeswitch_ext_id)
  values (nextval('freeswitch_condition_seq'), 'destination_number', '^*80$', (select currval('freeswitch_ext_seq')));
insert into freeswitch_action (freeswitch_action_id, application, data, freeswitch_condition_id)
  values (nextval('freeswitch_action_seq'), 'answer', NULL, (select currval('freeswitch_condition_seq')));
insert into freeswitch_action (freeswitch_action_id, application, data, freeswitch_condition_id)
  values (nextval('freeswitch_action_seq'), 'erlang_sendmsg',
  'oacd_dialplan_listener openacd@' || (select fqdn from location where primary_location = true) || ' agent_login ${sip_from_user}',
  (select currval('freeswitch_condition_seq')));
insert into freeswitch_action (freeswitch_action_id, application, data, freeswitch_condition_id)
  values (nextval('freeswitch_action_seq'), 'sleep', '2000', (select currval('freeswitch_condition_seq')));
insert into freeswitch_action (freeswitch_action_id, application, data, freeswitch_condition_id)
  values (nextval('freeswitch_action_seq'), 'hangup', 'NORMAL_CLEARING', (select currval('freeswitch_condition_seq')));

-- log out
insert into freeswitch_extension (freeswitch_ext_id, name, description, freeswitch_ext_type, did, alias, enabled)
  values (nextval('freeswitch_ext_seq'), 'logout', 'Default logoout dial string', 'C', NULL, NULL, false);
insert into freeswitch_condition (freeswitch_condition_id, field, expression, freeswitch_ext_id)
  values (nextval('freeswitch_condition_seq'), 'destination_number', '^*81$', (select currval('freeswitch_ext_seq')));
insert into freeswitch_action (freeswitch_action_id, application, data, freeswitch_condition_id)
  values (nextval('freeswitch_action_seq'), 'answer', NULL, (select currval('freeswitch_condition_seq')));
insert into freeswitch_action (freeswitch_action_id, application, data, freeswitch_condition_id)
  values (nextval('freeswitch_action_seq'), 'erlang_sendmsg',
  'oacd_dialplan_listener openacd@' || (select fqdn from location where primary_location = true) || ' agent_logout ${sip_from_user}',
  (select currval('freeswitch_condition_seq')));
insert into freeswitch_action (freeswitch_action_id, application, data, freeswitch_condition_id)
  values (nextval('freeswitch_action_seq'), 'sleep', '2000', (select currval('freeswitch_condition_seq')));
insert into freeswitch_action (freeswitch_action_id, application, data, freeswitch_condition_id)
  values (nextval('freeswitch_action_seq'), 'hangup', 'NORMAL_CLEARING', (select currval('freeswitch_condition_seq')));
