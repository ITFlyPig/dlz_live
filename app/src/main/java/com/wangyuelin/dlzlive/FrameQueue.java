package com.wangyuelin.dlzlive;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class FrameQueue {

    private BlockingDeque<byte[]> queue = new LinkedBlockingDeque<>();

    /**
     * 获取一帧的数据
     * @return
     */
    public byte[] getFrame() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 添加一帧的数据
     * @param frame
     */
    public void addFrame(byte[] frame) {
        if (frame == null) {
            return;
        }
        try {
            queue.put(frame);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
