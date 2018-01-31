%{!?version:%define version 1.5.93}
%{!?releasenumber:%define releasenumber 0}
%{!?instprefix:%define instprefix /opt/opennms}
%{!?jdk:%define jdk jdk >= 1:1.5}

%{!?extrainfo:%define extrainfo %{nil}}
%{!?extrainfo2:%define extrainfo2 %{nil}}
%{!?enable_snapshots:%define enable_snapshots 1}

# keep RPM from making an empty debug package
%define debug_package %{nil}
# don't do a bunch of weird redhat post-stuff  :)
%define __os_install_post %{nil}

Name:          opennms-plugin-ticketer-centric
Summary:       OpenNMS Trouble-Ticketer Plugin: CentricCRM
Release:       %releasenumber
Version:       %version
License:       LGPL/AGPL
Group:         Applications/System
BuildArch:     noarch

Source:        centric-troubleticketer-%{pomversion}.tar.gz
URL:           http://www.opennms.org/index.php/CentricCRM_Trouble_Ticket_Plugin
BuildRoot:     %{_tmppath}/%{name}-%{version}-root

Requires(pre): opennms-core >= %{version}-%{release}
Requires:      opennms-core >= %{version}-%{release}
#BuildRequires: %{jdk}

Prefix:        %{instprefix}

%description
OpenNMS is an enterprise-grade network management platform.

This package contains the CentricCRM trouble-ticketer plugin
for OpenNMS.  For details, see the wiki page at:

http://www.opennms.org/index.php/CentricCRM_Trouble_Ticket_Plugin

%{extrainfo}
%{extrainfo2}

%prep

# %setup -n centric-troubleticketer-%{pomversion}

%build

%install
install -d -m 755 $RPM_BUILD_ROOT%{instprefix}
tar -xvzf %{_sourcedir}/centric-troubleticketer-%{pomversion}.tar.gz  -C $RPM_BUILD_ROOT%{instprefix}
chmod 644 $RPM_BUILD_ROOT%{instprefix}/lib/*.jar
# HACK ALERT!
rm -rf $RPM_BUILD_ROOT%{instprefix}/lib/commons-io-*.jar

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(664 root root 775)
%{instprefix}/etc/*.properties
%{instprefix}/lib/*.jar
