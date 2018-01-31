#!/usr/bin/env bash

# Prepare environment

# Add OpenNMS repository to allow installation of some dependencies, e.g. jicmp
echo "deb http://debian.opennms.org stable main" > /etc/apt/sources.list.d/opennms.list
echo "deb-src http://debian.opennms.org stable main" >> /etc/apt/sources.list.d/opennms.list

# Add pgp key
wget -O - http://debian.opennms.org/OPENNMS-GPG-KEY | apt-key add -

# initialize package manager (otherwise apt install may not work)
apt update

# install and configure PostgreSQL
apt install -y postgresql-9.3
echo "local   all             postgres                                peer" > /etc/postgresql/9.3/main/pg_hba.conf
echo "local   all             all                                     peer" >> /etc/postgresql/9.3/main/pg_hba.conf
echo "host    all             all             127.0.0.1/32            trust" >> /etc/postgresql/9.3/main/pg_hba.conf
echo "host    all             all             ::1/128                 trust" >> /etc/postgresql/9.3/main/pg_hba.conf
service postgresql restart || exit

# Create opennnms user and database
psql -U postgres -h localhost -c 'create user opennms' || exit
psql -U postgres -h localhost -c 'create database opennms' || exit



apt install -y expect rpm rsync jicmp jicmp6 jrrd nsis r-base || exit
