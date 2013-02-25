oacd = \
 oacd_freeswitch \
 oacd_dialplan \
 oacd_web

lib += $(oacd)

$(foreach P,$(oacd), \
  $(eval $(P)_VER = 2.0.0) \
  $(eval $(P)_PACKAGE_REVISION = $(shell cd $(SRC)/$(P); git describe --long --always | cut -d- -f2-3 | sed 's/-/./g')) \
  $(eval $(P)_SRPM = erlang-$(P)-$($(P)_VER)-$($(P)_PACKAGE_REVISION).src.rpm) \
  $(eval $(P)_TAR = $(SRC)/$(P)/erlang-$(P)-$($(P)_VER).tar.gz) \
  $(eval $(P)_SOURCES = $($(P)_TAR)) \
  $(eval $(P)_SRPM_DEFS = --define "buildno $($(P)_PACKAGE_REVISION)") \
  $(eval $(P)_RPM_DEFS = --define="buildno $($(P)_PACKAGE_REVISION)") \
)

# n/a - not autoconf projects
$(oacd:=.autoreconf) $(oacd:=.configure):;

$(oacd:=.dist) : %.dist : $(SRC)/%
	cd $(SRC)/$(PROJ); \
	  make dist

$(addprefix $(SRC)/,$(oacd)) :
	! test -d $@.git || rm -rf $@.git
	git clone git@github.com:sipxopenacd/oacd_dialplan.git $@.git
	mv $@.git $@
