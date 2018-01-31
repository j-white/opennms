#!/bin/bash
echo "CPU Info"
cat /proc/cpuinfo
echo ""

echo "Mem Info"
cat /proc/meminfo
echo ""

echo "System Info"
uname -a
echo ""

echo "ULimits"
ulimit -a
echo ""

echo "Executing user"
whoami
echo ""

echo "Disk space"
df -h
echo ""

echo "Java Info"
java -version
echo ""

echo "Maven Info"
mvn -version