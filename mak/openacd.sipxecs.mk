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
  $(eval $(P)_SOURCES = $($(P)_TAR)) \
  $(eval $(P)_SRPM_DEFS = --define "buildno $($(P)_PACKAGE_REVISION)") \
  $(eval $(P)_RPM_DEFS = --define="buildno $($(P)_PACKAGE_REVISION)") \
)

openacd_VER = 2.0.0
sipxopenacd_VER = $(PACKAGE_VERSION)
$(foreach P,$(oacd_class_1), \
  $(eval $(P)_SRPM = $(P)-$($(P)_VER)-$($(P)_PACKAGE_REVISION).src.rpm) \
  $(eval $(P)_TAR = $(SRC)/$(P)/$(P)-$($(P)_VER).tar.gz) \
)

$(foreach P,$(oacd_class_2), \
  $(eval $(P)_VER = 2.0.0) \
  $(eval $(P)_SRPM = erlang-$(P)-$($(P)_VER)-$($(P)_PACKAGE_REVISION).src.rpm) \
  $(eval $(P)_TAR = $(SRC)/$(P)/erlang-$(P)-$($(P)_VER).tar.gz) \
)

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
