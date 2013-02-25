lib += oacd_dialplan

oacd_dialplan_VER = 2.0.0

# we'll have to generated this automatically based on the number
# of commits or something similar
oacd_dialplan_PACKAGE_REVISION = $(shell cd $(SRC)/oac_freeswitch; git describe --long --always | cut -d- -f2-3 | sed 's/-/./g')

oacd_dialplan_SRPM = erlang-oacd_dialplan-$(oacd_dialplan_VER)-$(oacd_dialplan_PACKAGE_REVISION).src.rpm
oacd_dialplan_TAR = $(SRC)/$(PROJ)/erlang-oacd_dialplan-$(oacd_dialplan_VER).tar.gz
oacd_dialplan_SOURCES = $(oacd_dialplan_TAR)

oacd_dialplan_SRPM_DEFS = --define "buildno $(oacd_dialplan_PACKAGE_REVISION)"
oacd_dialplan_RPM_DEFS = --define="buildno $(oacd_dialplan_PACKAGE_REVISION)"

oacd_dialplan.autoreconf oacd_dialplan.configure: ;

$(SRC)/oacd_dialplan :
	! test -d $@.git || rm -rf $@.git
	git clone git@github.com:sipxopenacd/oacd_dialplan.git $@.git
	mv $@.git $@

oacd_dialplan.dist : $(SRC)/oacd_dialplan
	cd $(SRC)/$(PROJ); \
	  make dist
