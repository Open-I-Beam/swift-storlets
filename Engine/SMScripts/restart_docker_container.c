/*----------------------------------------------------------------------------
 * Copyright IBM Corp. 2015, 2015 All Rights Reserved
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * Limitations under the License.
 * ---------------------------------------------------------------------------
*/


#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/types.h>

 /*
  * Stop and Run a docker container using:
  * docker stop <container name>
  * docker run --name <container name> -d -v /dev/log:/dev/log -v <mount dir 1> -v <mount dir 2> -i -t <image> --net='none'
  * <container name> - The name of the container to stop / to start
  * <image name> - the name of the image from which to start the container
  * <mount dir 1> - The directory where the named pipes are placed. Typically mounted to /mnt/channels in the container
  * <mount dir 2> - The directory where the storlets are placed. Typically mounted to /home/swift in the container
  */

int main(int argc, char **argv) {
	char command[4096];
	char container_name[256];
	char container_image[256];
	char mount_dir1[1024];
	char mount_dir2[1024];

	snprintf(container_name,(size_t)256,"%s",argv[1]);
	snprintf(container_image,(size_t)256,"%s",argv[2]);
	snprintf(mount_dir1,(size_t)1024, "%s", argv[3]);
	snprintf(mount_dir2,(size_t)1024, "%s", argv[4]);

	int ret;
	setresuid(0,0,0);
	setresgid(0,0,0);
	sprintf(command,"/usr/bin/docker stop -t 1 %s",container_name);
	ret = system(command);

	sprintf(command,"/usr/bin/docker rm %s",container_name);
	ret = system(command);

	sprintf(command,
			"/usr/bin/docker run --net=none --name %s -d -v /dev/log:/dev/log -v %s -v %s -i -t %s",
			container_name,
			mount_dir1,
			mount_dir2,
			container_image);
	ret = system(command);
	return ret;
}
