/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.sample.videoframes;

import org.apache.commons.ssl.util.Hex;
import org.apache.log4j.Logger;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.wso2.carbon.databridge.agent.thrift.DataPublisher;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.exception.AuthenticationException;
import org.wso2.carbon.databridge.commons.exception.DifferentStreamDefinitionAlreadyDefinedException;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.databridge.commons.exception.StreamDefinitionException;
import org.wso2.carbon.databridge.commons.exception.TransportException;

import java.net.MalformedURLException;
import java.nio.ByteBuffer;

import static org.bytedeco.javacpp.opencv_core.IplImage;

/**
 * Object detecting client using JavaCV
 */
public class VideoFrameProducer {

    /**
     * The logger.
     */
    private static Logger log = Logger.getLogger(VideoFrameProducer.class);

    /**
     * The stream name in which the data will be published to
     */
    public static final String STREAM_NAME = "org.wso2.sample.video.frames";

    /**
     * The stream version in which the data will be published to
     */
    public static final String VERSION = "1.0.0";


    /**
     * The main method.
     *
     * @param args the arguments
     * @throws MalformedURLException                            the malformed url exception
     * @throws AgentException                                   the agent exception
     * @throws AuthenticationException                          the authentication exception
     * @throws TransportException                               the transport exception
     * @throws MalformedStreamDefinitionException               the malformed stream definition exception
     * @throws StreamDefinitionException                        the stream definition exception
     * @throws DifferentStreamDefinitionAlreadyDefinedException the different stream definition already defined exception
     * @throws InterruptedException                             the interrupted exception
     */
    public static void main(String[] args) throws MalformedURLException, AgentException,
                                                  AuthenticationException, TransportException,
                                                  MalformedStreamDefinitionException,
                                                  StreamDefinitionException,
                                                  DifferentStreamDefinitionAlreadyDefinedException,
                                                  InterruptedException {

        // Setting trust store system properties
        KeyStoreUtil.setTrustStoreParams();
        String source = args[0];
        String cascadeFilePath = args[1];
        int maxFrameCount = Integer.parseInt(args[2]);
        String host = args[3];
        String port = args[4];
        String username = args[5];
        String password = args[6];

        // new data publisher
        DataPublisher dataPublisher = new DataPublisher("tcp://" + host + ":" + port, username,
                                                        password);
        // Gets the stream ID to which the data should be published.
        String streamID = getStreamID(dataPublisher);

        FrameGrabber grabber = new OpenCVFrameGrabber(source);
        // Start grabber to capture video
        grabber.start();

        //Declare frame as IplImage
        IplImage frame;
        int tempFrameCount = 0;
        // Loop till maximum amount of frames are reached. Else keep looping till user exits.
        while (maxFrameCount == -1 || tempFrameCount < maxFrameCount) {
            // Grabs a frame from the video
            frame = grabber.grab();
            if (null != frame) {
                tempFrameCount++;
                long currentTime = System.currentTimeMillis();
                String croppedImageHex = imageToHex(frame);
                // The payload data to be published
                Object[] payloadData = new Object[]{currentTime, tempFrameCount, source,
                                                    croppedImageHex, cascadeFilePath};

                // Logging collected information
                log.info("Sending frame " + Integer.toString(tempFrameCount) +
                         " : Found frame at " + Long.toString(currentTime));

                // Creating an event and publishing
                Event eventOne = new Event(streamID, System.currentTimeMillis(), null, null,
                                           payloadData);
                dataPublisher.publish(eventOne);
            }
        }

        // Stops publishing data
        dataPublisher.stop();
    }

    /**
     * Converting a javacv image to hex.
     *
     * @param capturedImage the captured image from the input video stream
     * @return the hex string
     */
    private static String imageToHex(IplImage capturedImage) {
        ByteBuffer byteBuffer = capturedImage.getByteBuffer();
        byte[] imageArray = new byte[byteBuffer.remaining()];
        byteBuffer.get(imageArray);
        return Hex.encode(imageArray);
    }

    /**
     * Gets the stream ID to publish data. If stream ID does not exist, create a new stream.
     *
     * @param dataPublisher the data publisher
     * @return the stream id
     * @throws AgentException                                   the agent exception
     * @throws MalformedStreamDefinitionException               the malformed stream definition exception
     * @throws StreamDefinitionException                        the stream definition exception
     * @throws DifferentStreamDefinitionAlreadyDefinedException the different stream definition already defined exception
     */
    private static String getStreamID(DataPublisher dataPublisher)
            throws AgentException,
                   MalformedStreamDefinitionException,
                   StreamDefinitionException,
                   DifferentStreamDefinitionAlreadyDefinedException {
        // Stream definition
        // // timestamp - time which the object was identified
        // // frame_id - unique id for the frame
        // // camera_id - source
        // // image - cropped image of the detected object sent as a hex
        // // cascade - the file path to the cascade file
        log.info("Creating stream " + STREAM_NAME + ":" + VERSION);
        String streamId =
                dataPublisher.defineStream("{" +
                                           "  'name':'" +
                                           STREAM_NAME +
                                           "'," +
                                           "  'version':'" +
                                           VERSION +
                                           "'," +
                                           "  'nickName': 'Video_Frame_Feeding'," +
                                           "  'description': 'A sample for Video Frame Feeding'," +
                                           "  'payloadData':[" +
                                           "          {'name':'timestamp','type':'LONG'}," +
                                           "          {'name':'frame_id','type':'INT'}," +
                                           "          {'name':'camera_id','type':'STRING'}," +
                                           "          {'name':'image','type':'STRING'}," +
                                           "          {'name':'cascade','type':'STRING'}" +
                                           "  ]" + "}");

        return streamId;
    }
}
