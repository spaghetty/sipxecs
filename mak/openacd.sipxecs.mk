oacd_class_1 = \
  openacd \
  sipxopenacd

# Class 2 have 'erlang-' in tarball/src rpm name for no particular reason other 
# than they started that way.  Hopefully will change to be consistent someday.
oacd_class_2 = \
 oacd_freeswitch \
 oacd_dialplan \
 oacd_web

oacd = \
  $(oacd_class_1) \
  $(oacd_class_2)

lib += $(oacd)

$(foreach P,$(oacd), \
  $(eval $(P)_PACKAGE_REVISION = $(shell cd $(SRC)/$(P); git describe --long --always | cut -d- -f2-3 | sed 's/-/./g')) \
  $(eval $(P)_SRPM_DEFS = --define "buildno $($(P)_PACKAGE_REVISION)") \
  $(eval $(P)_RPM_DEFS = --define="buildno $($(P)_PACKAGE_REVISION)") \
)

openacd_VER = 2.0.0
sipxopenacd_VER = $(PACKAGE_VERSION)
$(foreach P,$(oacd_class_1), \
  $(eval $(P)_SRPM = $(P)-$($(P)_VER)-$($(P)_PACKAGE_REVISION).src.rpm) \
  $(eval $(P)_TAR = $(SRC)/$(P)/$(P)-$($(P)_VER).tar.gz) \
  $(eval $(P)_SOURCES = $($(P)_TAR)) \
)

$(foreach P,$(oacd_class_2), \
  $(eval $(P)_VER = 2.0.0) \
  $(eval $(P)_SRPM = erlang-$(P)-$($(P)_VER)-$($(P)_PACKAGE_REVISION).src.rpm) \
  $(eval $(P)_TAR = $(SRC)/$(P)/erlang-$(P)-$($(P)_VER).tar.gz) \
  $(eval $(P)_SOURCES = $($(P)_TAR)) \
)

ifneq ($(filter erlang,$(lib)),)
erlang_DEPS_OPT = $(call deps,erlang)
endif
gen_server_mock_DEPS = $(erlang_DEPS_OPT)
erlmmongo_DEPS = $(erlang_DEP_OPT)
erlang-ej_DEPS = $(erlang_DEP_OPT)
erlang-cowboy_DEPS = $(erlang_DEP_OPT)
erlang-mimetypes_DEPS = $(erlang_DEP_OPT)
erlang-ejrpc2_DEPS = $(call deps,erlang-ej)
openacd_DEPS = $(call deps,erlang-ejrpc2) $(call deps,erlang-gen_server_mock) $(call deps,erlmongo)
oacd_web_DEPS = $(call deps,openacd) $(call deps,erlang-cowboy) $(call deps,erlang-mimetypes)
oacd_dialplan_DEPS = $(call deps,openacd)
oacd_freeswitch_DEPS = $(call deps,openacd)

# n/a - not autoconf projects
$(oacd:=.autoreconf) $(oacd:=.configure):;

openacd.dist:
	cd $(SRC)/$(PROJ); \
	  autoreconf -if; \
	  ./configure; \
	  make dist

sipxopenacd.dist $(oacd_class_2:=.dist):
	cd $(SRC)/$(PROJ); \
	  make dist
