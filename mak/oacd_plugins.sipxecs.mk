oacd = \
 oacd_freeswitch \
 oacd_dialplan \
 oacd_web

oacd_all = \
  $(oacd) \
  sipxopenacd

lib += $(oacd_all)

$(foreach P,$(oacd_all), \
  $(eval $(P)_PACKAGE_REVISION = $(shell cd $(SRC)/$(P); git describe --long --always | cut -d- -f2-3 | sed 's/-/./g')) \
  $(eval $(P)_SOURCES = $($(P)_TAR)) \
  $(eval $(P)_SRPM_DEFS = --define "buildno $($(P)_PACKAGE_REVISION)") \
  $(eval $(P)_RPM_DEFS = --define="buildno $($(P)_PACKAGE_REVISION)") \
)

$(foreach P,$(oacd), \
  $(eval $(P)_VER = 2.0.0) \
  $(eval $(P)_SRPM = erlang-$(P)-$($(P)_VER)-$($(P)_PACKAGE_REVISION).src.rpm) \
  $(eval $(P)_TAR = $(SRC)/$(P)/erlang-$(P)-$($(P)_VER).tar.gz) \
)

# Doesn't have 'erlang-' in tarball/src rpm name for no particular reason other 
# than they started that way.  Hopefully will change to be consistent someday.
sipxopenacd_VER = $(PACKAGE_VERSION)
sipxopenacd_SRPM = sipxopenacd-$(sipxopenacd_VER)-$(sipxopenacd_PACKAGE_REVISION).src.rpm
sipxopenacd_TAR = $(SRC)/sipxopenacd/sipxopenacd-$(sipxopenacd_VER).tar.gz

# n/a - not autoconf projects
$(oacd_all:=.autoreconf) $(oacd_all:=.configure):;

$(oacd_all:=.dist): %.dist : $(SRC)/%
	cd $(SRC)/$(PROJ); \
	  make dist
