#include <asm/types.h>          /* for videodev2.h */
#include <linux/videodev2.h>
//#include "dynctrl-logitech.h"
#include <stdio.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <sys/time.h>
#include <fcntl.h>

int fd;
int controltime;
#define PAN_REL 200
#define TILT_REL 100

enum { PANLEFT, PANRIGHT };
enum { TILTUP, TILTDOWN };

int xioctl(int fd, int request, void* arg){
	int r;
	do r = ioctl (fd, request, arg);
	while (-1 == r && EINTR == errno);
	return r;
}

int camera_pan(int direction, int fd){
	struct v4l2_control ctrl;
	ctrl.id = V4L2_CID_PAN_RELATIVE;
	
	if (direction == PANLEFT)
		ctrl.value = PAN_REL;
	else if (direction == PANRIGHT)
		ctrl.value = -PAN_REL;
	else {
		fprintf(stderr, "Invalid pan direction\n");
		return 0;
	}
	if(-1 == xioctl(fd,VIDIOC_S_CTRL,&ctrl)){
		fprintf(stderr,"Unable to pan\n");
		return 0;
	}
	return 1;
}

int camera_tilt(int direction, int fd){
	struct v4l2_control ctrl;
	ctrl.id = V4L2_CID_TILT_RELATIVE;
	
	if (direction == TILTDOWN)
		ctrl.value = TILT_REL;
	else if (direction == TILTUP)
		ctrl.value = -TILT_REL;
	else {
		fprintf(stderr, "Invalid tilt diirection\n");
		return 0;
	}
	if(-1 == xioctl(fd,VIDIOC_S_CTRL,&ctrl)){
		fprintf(stderr,"Unable to tilt\n");
		return 0;
	}
	return 1;
}

int main(int argc, char* argv[]) {
	int camerafound = 0;
	if(argc != 2)
	{
		printf("usage: camctl <u,d,l,r>\n");
		return 0;
	}

	if ((fd = open("/dev/video1",O_RDWR, 0)) != -1)
		camerafound = 1;
	else if ((fd = open("/dev/video0",O_RDWR, 0)) != -1)
		camerafound = 1;
	else
		return 0;


	if (argv[1][0] == 'u'){
		camera_tilt(TILTUP, fd);
		printf("up\n");
	}
	if (argv[1][0] == 'd') {
		camera_tilt(TILTDOWN, fd);
		printf("down\n");
	}
	if (argv[1][0] == 'l') {
		camera_pan(PANLEFT, fd);
		printf("left\n");
	}
	if (argv[1][0] == 'r') {
		camera_pan(PANRIGHT, fd);
		printf("right\n");
	}

	return 0;
}
