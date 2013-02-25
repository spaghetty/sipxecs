lib += oacd_freeswitch

oacd_freeswitch_VER = 2.0.0

# we'll have to generated this automatically based on the number
# of commits or something similar
oacd_freeswitch_PACKAGE_REVISION = $(shell cd $(SRC)/oac_freeswitch; git describe --long --always | cut -d- -f2-3 | sed 's/-/./g')

oacd_freeswitch_SRPM = erlang-oacd_freeswitch-$(oacd_freeswitch_VER)-$(oacd_freeswitch_PACKAGE_REVISION).src.rpm
oacd_freeswitch_TAR = $(SRC)/$(PROJ)/erlang-oacd_freeswitch-$(oacd_freeswitch_VER).tar.gz
oacd_freeswitch_SOURCES = $(oacd_freeswitch_TAR)

oacd_freeswitch_SRPM_DEFS = --define "buildno $(oacd_freeswitch_PACKAGE_REVISION)"
oacd_freeswitch_RPM_DEFS = --define="buildno $(oacd_freeswitch_PACKAGE_REVISION)"

oacd_freeswitch.autoreconf oacd_freeswitch.configure: ;

oacd_freeswitch.dist :
	cd $(SRC)/$(PROJ); \
	  make dist
