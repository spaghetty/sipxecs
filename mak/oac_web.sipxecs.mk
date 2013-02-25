lib += oacd_web

oacd_web_VER = 2.0.0

# we'll have to generated this automatically based on the number
# of commits or something similar
oacd_web_PACKAGE_REVISION = $(shell cd $(SRC)/oac_freeswitch; git describe --long --always | cut -d- -f2-3 | sed 's/-/./g')

oacd_web_SRPM = erlang-oacd_web-$(oacd_web_VER)-$(oacd_web_PACKAGE_REVISION).src.rpm
oacd_web_TAR = $(SRC)/$(PROJ)/erlang-oacd_web-$(oacd_web_VER).tar.gz
oacd_web_SOURCES = $(oacd_web_TAR)

oacd_web_SRPM_DEFS = --define "buildno $(oacd_web_PACKAGE_REVISION)"
oacd_web_RPM_DEFS = --define="buildno $(oacd_web_PACKAGE_REVISION)"

oacd_web.autoreconf oacd_web.configure: ;

oacd_web.dist :
	cd $(SRC)/$(PROJ); \
	  make dist
