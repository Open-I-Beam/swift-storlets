#!/bin/bash

#---------------------------------------------------------------------------
# Copyright IBM Corp. 2015, 2015 All Rights Reserved
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# Limitations under the License.
#---------------------------------------------------------------------------

# This script is the our docker ENTRYPOINT
# $1 determines between debug mode running mode
# For debug mode $2 is a command to execute
# Otherwise $2,$3 are as follows
# $2 is the path to the factory pipe , e.g. /channels/factory_pipe
#    This path must be mapped by docker run -v option
# $3 is assumed to be the factory debug level, e.g. DEBUG

if [ $1 = "debug" ]; then
    $2
else
    #service rsyslog start
    python /usr/local/lib/python2.7/dist-packages/storlet_daemon_factory/daemon_factory.py $2 $3 $HOSTNAME
fi
