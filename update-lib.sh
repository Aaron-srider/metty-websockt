#!/bin/bash

rm -R server/target/lib

mvn package

remote_server="49.232.155.160"
username="ubuntu"
private_key_path="/home/chaowen/.ssh/common-key.pri"  # Replace with the path to your private key file
command_to_execute="./run_jar.sh"  # Replace with the command you want to execute

lib_path=server/target/lib
remote_lib_path=lib/



ssh -i "$private_key_path" "$username@$remote_server" "rm -R ${remote_lib_path} && mkdir -p ${remote_lib_path} && exit"
scp -i "$private_key_path" ${lib_path}/* "$username@$remote_server":${remote_lib_path}

