#!/bin/bash

###
# ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
# The Apache License, Version 2.0
# ——————————————————————————————————————————————————————————————————————————————
# Copyright (C) 2019 Autonomic, LLC - All rights reserved
# ——————————————————————————————————————————————————————————————————————————————
# Proprietary and confidential.
# 
# NOTICE:  All information contained herein is, and remains the property of
# Autonomic, LLC and its suppliers, if any.  The intellectual and technical
# concepts contained herein are proprietary to Autonomic, LLC and its suppliers
# and may be covered by U.S. and Foreign Patents, patents in process, and are
# protected by trade secret or copyright law. Dissemination of this information
# or reproduction of this material is strictly forbidden unless prior written
# permission is obtained from Autonomic, LLC.
# 
# Unauthorized copy of this file, via any medium is strictly prohibited.
# ______________________________________________________________________________
###

# Fail on any error.
set -e
# Display commands to stderr.
set -x

cd github/jib/jib-maven-plugin
./mvnw -Prelease -B -U verify

# copy pom with the name expected in the Maven repository
ARTIFACT_ID=$(mvn -B help:evaluate -Dexpression=project.artifactId 2>/dev/null | grep -v "^\[")
PROJECT_VERSION=$(mvn -B help:evaluate -Dexpression=project.version 2>/dev/null| grep -v "^\[")
cp pom.xml target/${ARTIFACT_ID}-${PROJECT_VERSION}.pom
