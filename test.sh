#!/bin/bash

mvn clean;
mvn package;


# Define SSH parameters
remote_server="49.232.155.160"
username="ubuntu"
private_key_path="/home/chaowen/.ssh/common-key.pri"  # Replace with the path to your private key file
command_to_execute="./run_jar.sh"  # Replace with the command you want to execute

dist=server/target/server-0.0.1-SNAPSHOT.jar
remote_dist=server-0.0.1-SNAPSHOT.jar

# Define local output file
output_file="output.txt"

scp -i "$private_key_path" ${dist} "$username@$remote_server":${remote_dist}



# Use SSH to execute the command remotely, capture the output, and display it on the console
ssh -i "$private_key_path" "$username@$remote_server" "$command_to_execute" | tee "$output_file"

# Check the exit status of the SSH command
if [ $? -eq 0 ]; then
    echo "Command executed successfully. Output saved to $output_file"
else
    echo "Error executing the command on the remote server."
fi

