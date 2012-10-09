#include <jni.h>
#include "speex/speex.h"

#define FRAME_SIZE 320

int nbBytes;
/*Holds the state of the encoder*/
void *state;
/*Holds bits so they can be read and written to by the Speex routines*/
SpeexBits bits;
int i, tmp;

void Java_fusao_awesome_TestAppActivity_init(JNIEnv * env, jobject jobj) {
   /*Create a new encoder state in narrowband mode*/
   state = speex_encoder_init(&speex_wb_mode);

   /*Set the quality to 8*/
   tmp=8;
   speex_encoder_ctl(state, SPEEX_SET_QUALITY, &tmp);

   /*Initialization of the structure that holds the bits*/
   speex_bits_init(&bits);
}

jbyteArray Java_fusao_awesome_TestAppActivity_encode(JNIEnv * env, jobject jobj, jshortArray inputData) {
		jbyteArray ret;

		jshort * inputArrayElements = (*env)->GetShortArrayElements(env, inputData, 0);

		/*Flush all the bits in the struct so we can encode a new frame*/
		speex_bits_reset(&bits);

		/*Encode the frame*/
		speex_encode_int(state, inputArrayElements, &bits);
		/*Copy the bits to an array of char that can be written*/
		nbBytes = speex_bits_nbytes(&bits);

		ret = (jbyteArray) ((*env)->NewByteArray(env, nbBytes));
		jbyte * arrayElements = (*env)->GetByteArrayElements(env, ret, 0);

		speex_bits_write(&bits, arrayElements, nbBytes);

		(*env)->ReleaseShortArrayElements(env, inputData, inputArrayElements, JNI_ABORT);
		(*env)->ReleaseByteArrayElements(env, ret, arrayElements, 0);
		return ret;
}
